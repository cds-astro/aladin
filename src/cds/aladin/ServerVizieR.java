// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cds.tools.CDSConstants;
import cds.vizier.VizieRCatalogs;
import cds.vizier.VizieRList;
import cds.vizier.VizieRPanel;
import cds.vizier.VizieRQuery;
import cds.xml.XMLParser;

/**
 * Le formulaire d'interrogation de Vizir
 *
 * @author Pierre Fernique [CDS]
 * @version 3.0 : jan 03 - Suppression du Layout Manager et toilettage [PF]
 * @version 2.0 : 13 mai 2002 - Utilisation de l'API VizieR [Andre Schaaff]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class ServerVizieR extends Server implements CDSConstants,Runnable {
   String CATDESC,CATMOC,CATDMAP,INFO1,TAGGLU,GETALL,GETALL1,CAT;
   
   static final String  MOCGLU = "getMOC";
   static final String  MOCERROR = "Catalog unknown or MOC server error";

   // les composantes de l'objet
   VizieRList vizierlist;       // La liste des catalogues proposees
   JTextField catalog;          // le champ de saisie du catalogue
   JCheckBox cbGetAll;          // La checkbox du Get All column
   JCheckBox cbGetAllCat;       // La checkbox du Get All Cat
   MyLabel legende;             // legende courante de la liste
   JButton getReadMe;           // Le bouton pour demander des infos
   JButton getMoc;              // Le bouton pour demander le MOC
   JButton getDMap;             // Le bouton pour demander la carte de densit�
   boolean hasPreviousFocus = false; // this boolean is needed for the submit button management
   Thread thread;

   // Les references
   Vector vSurveys;		//La liste des surveys par defaut
   Vector vArchives;		//La liste des archives
   ServerDialog serverDialog;

   // Catalogs panel
   VizieRPanel vp = null;
   Vector catalogs = new Vector();
   VizieRCatalogs vcl = null;

   JPanel panelButtonRight = new JPanel();

    JPanel actions;

 /** Creation du formulaire d'interrogation de Vizir
   * @param aladin reference
   * @param status le label qui affichera l'etat courant
   * @param vSurveys La liste des surveys par defaut
   * @param actions les boutons d'action
   */
   protected ServerVizieR(final Aladin aladin,
                          ServerDialog serverDialog, JPanel actions) {
      this.aladin = aladin;
      createChaine();
      type = CATALOG;
      aladinLogo = "VizieRLogo.gif";
      docUser="http://vizier.u-strasbg.fr";
      TAGGLU = "VizieRXML++1";
      aladinLabel="VizieR";
      
      // Juste pour revenir au serveur Vizier normal si on n'a pas 
      // la surcharge GLU pour le nouveau serveur
      if( !aladin.CDS || aladin.glu.getURL(TAGGLU,"",false,false)==null ) {
         TAGGLU = TAGGLU.substring(0,TAGGLU.length()-1);
      } else TESTSERVER=true;

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=0;

      this.actions = actions;

      // Creation des objets propres a l'interrogation de VizieR
      this.serverDialog = serverDialog;

      // Le titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,title);
      if( TESTSERVER ) {
         testServer.setText("(beta server)");
         d.width+=100;
         testServer.setSelected(false);
      }
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);
      
      // Premiere indication
      JLabel info1 = new JLabel(INFO1);
      info1.setBounds(86,y,400, 20); y+=20;
      add(info1);

      int yGetAll = y+15; // + (Aladin.PROTO ? -15 : 0);;
      int xGetAll = XWIDTH-105;

      // JPanel pour la memorisation du target (+bouton DRAG)
      JPanel tPanel = new JPanel();
      tPanel.setBackground(Aladin.BLUE);
      int h = makeTargetPanel(tPanel,FORVIZIER);
      tPanel.setBounds(0,y,xGetAll-5,h);
      add(tPanel);

      modeCoo=COO|SIMBAD;
      modeRad=RADIUS;

      y+=h;

      // La checkbox du getAllColumns
      cbGetAll=new JCheckBox(GETALL,false);
      cbGetAll.setEnabled(false);
      cbGetAll.setBackground(Aladin.BLUE);
      cbGetAll.setBounds(xGetAll,yGetAll,120,20); yGetAll+=20;
      if( !Aladin.OUTREACH ) add(cbGetAll);

      // La checkbox du getAllCat
      cbGetAllCat=new JCheckBox(GETALL1,false);
      cbGetAllCat.setEnabled(false);
      cbGetAllCat.setBackground(Aladin.BLUE);
      cbGetAllCat.setBounds(xGetAll,yGetAll,140,20); yGetAll+=20;
      cbGetAllCat.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean flag=!cbGetAllCat.isSelected();
            target.setEnabled(flag);
            radius.setEnabled(flag);
         }
      });
      

      if( !Aladin.OUTREACH ) add(cbGetAllCat);

      // Catalog + Radius
      JLabel label1 = new JLabel(addDot(CAT));
      label1.setFont(Aladin.BOLD);
      int l= 55+30;
      label1.setBounds(XTAB1,y,l,30);
      add(label1);
      catalog = new JTextField(28);
      catalog.setBounds(l+15,y,150-30,HAUT);
      catalog.addKeyListener(this);
      catalog.getDocument().addDocumentListener(new DocumentListener() {
         public void removeUpdate(DocumentEvent e)  { updateWidgets(); }
         public void insertUpdate(DocumentEvent e)  { updateWidgets(); }
         public void changedUpdate(DocumentEvent e) { updateWidgets(); }
      });

      add(catalog);
      JLabel label2 = new JLabel(addDot(RAD));
      label2.setFont(Aladin.BOLD);
      label2.setBounds(246-15,y,l,HAUT);
      add(label2);
      radius = new JTextField("10 arcmin");
      radius.setBounds(294-15,y,105,HAUT); y+=HAUT+MARGE-2;
      radius.addKeyListener(this);
      radius.addActionListener(this);
      add(radius);

      // Bouton getReadMe
      Insets insets = new Insets(0,10,0,10);
      getReadMe = new JButton(CATDESC);
      getReadMe.setMargin(insets);
      getReadMe.addActionListener(this);
      getReadMe.setFont( Aladin.BOLD);
      getReadMe.setEnabled(false);
      
      // Bouton getMoc
      getMoc = new JButton(CATMOC);
      getMoc.setMargin(insets);
      getMoc.addActionListener(this);
      getMoc.setFont( Aladin.BOLD);
      getMoc.setEnabled(false);
      
      // Bouton getDMap      
      getDMap = new JButton(CATDMAP);
      getDMap.setMargin(insets);
      getDMap.addActionListener(this);
      getDMap.setFont( Aladin.BOLD);
      getDMap.setEnabled(false);
      
      JPanel catControl = new JPanel(new FlowLayout(FlowLayout.LEFT));
      catControl.setBounds(l+15,y,350,HAUT);
      catControl.setBackground(Aladin.BLUE);
      catControl.add(getReadMe);
      catControl.add(getMoc);
      if( aladin.BETA ) catControl.add(getDMap);
      if( !Aladin.OUTREACH ) add(catControl);
      y+=HAUT+MARGE+5;


//      getReadMe.addActionListener( new ActionListener() {
//         public void actionPerformed(ActionEvent e) {
//            String cata = catalog.getText().trim();
//            if( cata.equals("") ) {
//               //Aladin.warning(this,WNEEDCAT);
//            }
//            else {
//               cata = Glu.quote(cata);
//               aladin.glu.showDocument("getReadMe",cata);
//            }
//         }
//      });

//      vp = new VizieRPanel(null, true, false, null, null);
      //vp = new VizieRPanel(aladin.glu, true, false, null, null);


      vp = new VizieRPanel(this, FRAME, false, null, null, 10);
      vp.setBounds(XTAB1,y,XWIDTH-2*XTAB1,280); y+=280;
      add(vp);

      String[] tab1 = vp.getSelection("SURVEY");
      vSurveys = new Vector();		//La liste des surveys par defaut
      for (int i = 0 ; i < tab1.length; i++) {
        vSurveys.addElement(tab1[i]);
      }
      String[] tab2 = vp.getSelection("MISSION");
      vArchives  = new Vector();	//La liste des archives
      for (int i = 0 ; i < tab2.length; i++) {
        vArchives.addElement(tab2[i]);
      }

      setMaxComp(vp);

      // Positionnement des objets pour les resultats
 //     vizier.setRef(vizierlist,legende);
   }
   
   // retourne le tagGLU � utiliser en fonction du choix du serveur test ou non
   private String getTagGlu() {
      if( TESTSERVER && !testServer.isSelected() ) return TAGGLU.substring(0,TAGGLU.length()-1);
      return TAGGLU;
   }


   protected void createChaine() {
      super.createChaine();
      title  = aladin.chaine.getString("VZTITLE");
      aladinLabel    = aladin.chaine.getString("VZNAME");
      description   = aladin.chaine.getString("VZINFO");
      verboseDescr   = aladin.chaine.getString("VZDESC");
      CATDESC= aladin.chaine.getString("VZCATDESC");
      CATMOC = aladin.chaine.getString("VZCATMOC");
      CATDMAP= aladin.chaine.getString("VZCATDMAP");
      INFO1  = aladin.chaine.getString("VZINFO1");
      GETALL = aladin.chaine.getString("VZGETALL2");
      GETALL1= aladin.chaine.getString("VZGETALL3");
      CAT    = aladin.chaine.getString("VZCAT");
  }

   protected boolean is(String s) {
      return s.equalsIgnoreCase("vizier")
      || s.equalsIgnoreCase("vizir")
      || s.equalsIgnoreCase("vizierX");
   }

   /** Creation d'un plan de maniere generique */
   protected int createPlane(String target,String radius,String criteria,
   				 String label, String origin) {

      // Pour enlever des quotes intempestives
      criteria = specialUnQuoteCriteria(criteria);

      // Toutes les colonnes
      int i;
      boolean allColumn=false;
      if( (i=criteria.indexOf("allcolumns"))>=0 ) {
         criteria=criteria.substring(0,i-1) + criteria.substring(i+"allcolumns".length());
         criteria=criteria.trim();
         allColumn=true;
      }

      // Tout le catalogue
      if( cbGetAllCat.isSelected() ) target="";
      if( (i=criteria.indexOf("allsky"))>=0 ) {
         criteria=criteria.substring(0,i-1) + criteria.substring(i+"allsky".length());
         criteria=criteria.trim();
         target="";
      }
      
      // Toutes les colonnes
//      if( cbGetAll.isSelected() ) allColumn=true;

//System.out.println("criteria ["+criteria+"]");

      String catalogs= addCDSPrefix(criteria);	// EN PREMIERE APPROCHE...
//      if( label==null ) label=catalogs;
      label = getDefaultLabelIfRequired(label,catalogs);
      return creatVizieRPlane(target,radius,criteria,label,institute,allColumn);
   }
   
   // Ajoute le pr�fixe "CDS/" devant chaque nom de table si ce n'est d�j� fait
   private String addCDSPrefix( String s ) {
      StringBuilder res=null;
      Tok tok = new Tok(s,", ");
      while( tok.hasMoreTokens() ) {
         String s1 = tok.nextToken();
         if( !s1.startsWith("CDS/") ) s1="CDS/"+s1;
         if( res==null ) res = new StringBuilder(s1);
         else res.append(","+s1);
      }
      return res.toString();
   }

   /** Creation d'un plan de maniere specifique */
   protected int creatVizieRPlane(String target,String radius,String catalogs,
   				 String label, String origin,boolean allColumns) {
      URL u;

      // On enl�ve l'�ventuel unit�
      radius = getRM(radius)+"";

      String s = Glu.quote(catalogs)+" "+Glu.quote(target)+" "+Glu.quote(radius);
      if( allColumns ) s = s+" -out.all";

      if( (u=aladin.glu.getURL(getTagGlu(),s))==null ) {
         Aladin.warning(this,WERROR,1);
         return -1;
      }

      String param = label+" "+radius+(allColumns?" (all columns)":"");
      if( !verif(Plan.CATALOG,target,param) ) return -1;
      return aladin.calque.newPlanCatalog(u,label,target,param,origin,this);
   }
   
   
   // MODIF PIERRE F. POUR DESYNCHRONISER L'APPEL A LA LISTE DES CATALOGUES
   // DE VIZIER (sept 03)
   public void run() {
      ball.setMode(Ball.WAIT);
      String rad;
      try {
          rad=getRM(radius.getText())+"";
      }
      catch(Exception e) {
          rad = "";
      }
      vp.submit(target.getText(), rad);

      JList theTestList = vp.getResultList();

      // fill the catalog list
      catalogs.removeAllElements();

      for (int i = 0; i < theTestList.getModel().getSize(); i++)
         catalogs.addElement(theTestList.getModel().getElementAt(i));

      if (vcl == null) {
         JButton controlButton = new JButton("SUBMIT");
         controlButton.setFont(LBOLD);
         controlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit();
               vcl.resetCatList();
               catalog.setText("");
            }
         }
         );
//         vcl = new VizieRCatalogs(catalog, getReadMe, Aladin.PROTO ?getMoc:null, catalogs, controlButton);
         vcl = new VizieRCatalogs(catalog, null, null, catalogs, controlButton);
      }
      else
         vcl.show(catalogs);

      if (target.getText().compareTo("") == 0)
         vcl.setTitle(catalogs.size() + " catalog(s) found " + target.getText());
      else
         vcl.setTitle(catalogs.size() + " catalog(s) found around " +
                      target.getText());

      if( catalogs.size()==0 ) ball.setMode(Ball.NOK);
      else ball.setMode(ball.OK);

         // appel au treeview
//		aladin.treeView.addBranch(target.getText(),radius.getText(),this);
   }

   /** Interrogation de Vizir */
   public void submit() {
      String objet,cata,r;
      double rm=0;

      // if focus on catalogs panel then apply on this
      // component the submit action
      // MODIF PIERRE F. (SEPT 03)
      if (catalog.getText().compareTo("") == 0) {
         thread = new Thread(this,"AladinVizieRQuery");
         thread.start();
         return;
      }
      
      boolean allCat = cbGetAllCat.isSelected();

      objet=getTarget(false);
      cata = catalog.getText().trim();

      if( objet==null || objet.length()==0 ) {
         if( cata.length()==0 ) { Aladin.warning(this,WNEEDCAT); return; }
         if( !Aladin.confirmation(this,"Do you really want to download\n"+
               "all \""+cata+"\" ?") ) return;
         objet="";
         rm=0;
      } else if( !allCat ) {
         if( (r=getRadius())==null ) return;
         rm=getRM(r);
      }

      waitCursor();

      String bookmarkCode=null;
      if( cbGetAll.isSelected() ) cata = cata+",allcolumns";
      if( !allCat && objet!=null && objet.length()>0 ) {
         bookmarkCode = "get VizieR("+cata+")";
         aladin.console.printCommand("get VizieR("+cata+") "+objet+" "+Coord.getUnit(rm/60.));
      } else {
         bookmarkCode = "get VizieRX("+cata+")";
         aladin.console.printCommand(bookmarkCode);
      }

      int n = createPlane(objet,rm+"",cata,null,null);
      if( n!=-1 ) aladin.calque.getPlan(n).setBookmarkCode(bookmarkCode+" $TARGET $RADIUS");

      // Suggestion de FOX pour �viter d'avoir � r��crire le catalogue si on fait plusieurs interrogations
//      catalog.setText("");
//      resetFlagBoxes();
      defaultCursor();
   }

   protected boolean isDiscovery() { return true; }

   /** Methode TEMPORAIRE pour cr�er un SIA pour liste de catalogues qui en l'occurence
    * ne comportera qu'une entr�e d�crivant le nombre d'objets retourn�s par l'URL */
   protected MyInputStream getMetaDataForCat(Vector list,String target,String radius) {
      try {
         int n = list.size();
         if( n==0 ) return null;

         MyByteArrayStream bas = null;
         OutputStream out;
         out = bas = new MyByteArrayStream(10000);

         Aladin.writeBytes(out,
//               System.out.print(
               "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
               "<!DOCTYPE VOTABLE SYSTEM \"http://us-vo.org/xml/VOTable.dtd\">\n"+
               "<VOTABLE version=\"v1.0\">\n"+
               "<RESOURCE type=\"results\">\n"+
               "   <INFO name=\"QUERY_STATUS\" value=\"OK\"/>\n"+
               "   <TABLE>\n"+
               "<FIELD name=\"Resource\" datatype=\"char\" ucd=\"VOX:Image_Title\" arraysize=\"*\"/>\n"+
               "<FIELD name=\"url\" datatype=\"char\" ucd=\"VOX:Image_AccessReference\" arraysize=\"*\"/>\n"+
               "<FIELD ID=\"FORMAT\" datatype=\"char\" arraysize=\"*\"/>\n"+
               "<DATA>\n"+
               "<TABLEDATA>\n");
         for( int i=0; i<n; i++ ) {
            URL u;
            String item = XMLParser.XMLEncode((String)list.elementAt(i));
//            System.out.println(item);
            int j;
            if( (j=item.indexOf('\t'))<0 ) continue;
            String catalogs = item.substring(0,j);
            String s = Glu.quote(catalogs)+" "+Glu.quote(target)+" "+Glu.quote(radius);
            if( (u=aladin.glu.getURL(getTagGlu(),s))==null ) continue;
            item = item.replace('\t', ' ');

            Aladin.writeBytes(out,
//System.out.print(
               "   <TR>\n"+
               "      <TD>"+item+"</TD>\n"+
               "      <TD><![CDATA["+u+"]]></TD>\n"+
               "      <TD>CATALOG</TD>\n"+
               "   </TR>\n");
         }
         Aladin.writeBytes(out,
//         System.out.print(
               "</TABLEDATA>\n"+
               "</DATA>\n"+
               "</TABLE>\n"+
               "</RESOURCE>\n"+
               "</VOTABLE>\n");

         return new MyInputStream(bas.getInputStream());
      } catch( Exception e) { }
      return null;
   }

   protected MyInputStream getMetaData(String target, String radius,StringBuffer infoUrl) {
      double rm = getRM(radius);
      Vector v = new Vector();
      if( !vp.getVizieRQuery().submit(target,rm+"",null,null,null,null,VizieRQuery.FRAME,v) ) return null;
      return getMetaDataForCat(v,target,radius);
   }

  /** Reset du formulaire */
   protected void clear() {
      super.clear();
//      rad[0].setText("10 arcmin");
      catalog.setText("");
      vp.resetAll();
   }
   
   /** Surcharge de show() pour remettre � jour les Choice Input */
   public void setVisible(boolean flag) {
      resetCatalog();
      super.setVisible(flag);
   }
   
   public void resetCatalog() { catalog.setText(""); }

   protected void reset() {
      resetFlagBoxes();
      super.reset();
      vp.resetAll();
   }
   
   protected void resetFlagBoxes() {
      cbGetAll.setSelected(false);
      cbGetAllCat.setSelected(false);
      target.setEnabled(true);
      radius.setEnabled(true);
   }

//  /** Re-affichage avec regeneration du panel du formulaire.
//   * Rq : Eh oui, il faut bien ruser pour supporter Netscape 3.0
//   */
//   protected void reaffiche() {
//      hide();show();
//   }
   
   private String oCat=null; // Juste pour �viter de faire plusieurs fois la m�me chose
   
   protected boolean updateWidgets() {
      if( !super.updateWidgets() ) return false;
      if( catalog==null ) return false;
      String cat =  catalog.getText().trim();
      if( oCat!=null && oCat.equals(cat) ) return true;
      oCat=cat;
      boolean catOk =cat.length()!=0;
      getReadMe.setEnabled(catOk);
      getMoc.setEnabled(catOk);
      getDMap.setEnabled(catOk);
      cbGetAll.setEnabled(catOk);
      cbGetAllCat.setEnabled(catOk);
      return true;
   }


  /** Events management
   * @see aladin.VizieR
   */
   public void actionPerformed(ActionEvent e) {
      super.actionPerformed(e);
      Object s = e.getSource();
      
      if( s instanceof JButton ) {
         String action = ((JButton)s).getActionCommand();
         
         if( action.equals(CATDESC) || action.equals(CATMOC) || action.equals(CATDMAP) ) {
            
            try {
               String cata = catalog.getText().trim();
               if( cata.equals("") ) { Aladin.warning(this,WNEEDCAT); return; }
               
               // Affichage du README
               if( action.equals(CATDESC) ) aladin.glu.showDocument("getReadMe",Glu.quote(cata));
               
               // Chargement du MOC
               else if( action.equals(CATMOC) ) {
                  URL u = aladin.glu.getURL(MOCGLU,Glu.quote(cata)+" 512");
                  aladin.execAsyncCommand("'"+cata+" MOC'=get File("+u+")");
               }
               
               // Chargement de la carte de densit�
               else if( action.equals(CATDMAP) ) aladin.calque.newPlanDMap(cata,cata);
               defaultCursor();
               return;
            } catch( Exception e1 ) {
               aladin.warning("Error: cannot load this product\n"+e1.getMessage());
            }
         }
         
      }
   }

  /** Cache la sous-fenetre d'interrogation de VizieR */
   protected void hideSFrame() { }
}
