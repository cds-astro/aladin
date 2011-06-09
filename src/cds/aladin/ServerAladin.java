// Copyright 2010 - UDS/CNRS
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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import cds.tools.Util;

/**
 * Le formulaire d'interrogation d'Aladin
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : (11 fevrier 2002) Selection multiple possible
 * @version 1.1 : (10 octobre 2000) Bidouillage pour DENIS et 2MASS
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class ServerAladin extends Server implements Runnable, MyListener {
   Thread thread;
//   ButtonGroup listTree;
   JCheckBox byTree;
   ButtonGroup format;
   JRadioButton jpgCb, fitsCb;
   JLabel step1,step2;
   String STEP1,STEP2,BYTREE,DEFFMT,SERVERR;
   
   String GLUDEFQUAL = "DefQual1";
   String GLUIMAGE = "Image1";
   String GLUQUALIFIER = "Aladin.qualifierServer1";    
   
   // retourne le tagGLU à utiliser en fonction du choix du serveur test ou non
   // Supprimme le "1" à la fin
   private String getTagGlu(String tag) {
      if( TESTSERVER && !testServer.isSelected() ) return tag.substring(0,tag.length()-1);
      return tag;
   }   

 /** Creation du formulaire d'interrogation d'Aladin.
   * @param aladin reference
   */
   protected ServerAladin(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      type = IMAGE;
      aladinLogo="AladinLogo.gif";
      DISCOVERY=true;
      
      // Juste pour revenir au serveur Aladin normal si on n'a pas 
      // la surcharge GLU pour le nouveau serveur
      if( !Aladin.BETA || aladin.glu.getURL(GLUQUALIFIER,"",false,false)==null ) {
         GLUDEFQUAL = GLUDEFQUAL.substring(0,GLUDEFQUAL.length()-1);
         GLUIMAGE = GLUIMAGE.substring(0,GLUIMAGE.length()-1);
         GLUQUALIFIER = GLUQUALIFIER.substring(0,GLUQUALIFIER.length()-1);         
      } else TESTSERVER=true;

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      //int y=25;
      int y=2;

      // Le titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,title);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);
      if( TESTSERVER ) testServer.setText("(beta server)");
     
      // Premiere indication
      step1 = new JLabel(STEP1);
      step1.setForeground(Color.blue);
      step1.setBounds(80,y,400, 19); y+=20;
      add(step1);

      // Panel pour la memorisationdu target (+bouton DRAG)
      JPanel tPanel = new JPanel();
      tPanel.setBackground(Aladin.BLUE);
      int h = makeTargetPanel(tPanel,Aladin.OUTREACH? NORADIUS : FORALADIN);
      tPanel.setBounds(0,y,XWIDTH,h); y+=h;
      add(tPanel);
      
      
      modeCoo=COO|SIMBAD;
      modeRad=NOMODE;
      
      if( radius!=null ) radius.setText("0 arcmin");

     // Deuxième indication
      step2 = new JLabel(STEP2);
      step2.setBounds(15,y,310, 19); // y+=20;
      add(step2);

      // Boutons pour controler l'arbre
      byTree = new JCheckBox(BYTREE,false); byTree.setActionCommand(BYTREE);
      byTree.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeViewMode(); }
      });
      int wpos=150;
      int xpos=XWIDTH-XTAB1-wpos;
      byTree.setBounds(xpos,y+2,wpos, 18); y+=22;
      byTree.setBackground(Aladin.BLUE);
      add(byTree);
      
//      listTree = new ButtonGroup();
//      b1 = new JRadioButton(BYLIST,true);  b1.setActionCommand(BYLIST);
//      b1.addActionListener(this);
//      listTree.add(b1);
//      int xpos=XWIDTH-XTAB1-100-20;
//      int wpos=68+10;
//      b1.setBounds(xpos,y+2,wpos, 18);
//      b1.setBackground(Aladin.BLUE);
//      add(b1);
//      
//      b2 = new JRadioButton(BYTREE,false); b2.setActionCommand(BYTREE);
//      b2.addActionListener(this);
//      listTree.add(b2);
//      b2.setBounds(xpos+wpos+3,y+2,50+10, 18); y+=22;
//      b2.setBackground(Aladin.BLUE);
//      add(b2);

      // L'arbre de Thomas
      tree = new MetaDataTree(aladin,null);
      tree.setAllowSortByFields(false);
      tree.setFlat(true);
      tree.setSortable(true);
      tree.setStateChangedListener(this);
      JScrollPane scrollTree = new JScrollPane(tree);
      tree.setScroll(scrollTree);
      scrollTree.setBackground(tree.bkgColor);
      scrollTree.setBounds(XTAB1,y,XWIDTH-2*XTAB1,217); y+=215;
      add(scrollTree);

      // boutons radio pour choix JPEG/FITS
      JPanel formatPanel = new JPanel();
      formatPanel.setBackground(Aladin.BLUE);
      formatPanel.setBounds(110,y,XWIDTH,30); y+=35;
      formatPanel.setLayout( new FlowLayout(FlowLayout.LEFT));
      formatPanel.setFont(Aladin.PLAIN);
      format = new ButtonGroup();
      formatPanel.add( new JLabel(DEFFMT));
      formatPanel.add( jpgCb = new JRadioButton("JPEG",true));  jpgCb.setActionCommand("JPEG");
      jpgCb.setBackground(Aladin.BLUE);
      format.add(jpgCb);
      formatPanel.add( fitsCb = new JRadioButton("FITS",false));fitsCb.setActionCommand("FITS");
      fitsCb.setBackground(Aladin.BLUE);
      format.add(fitsCb);
      if( !Aladin.OUTREACH ) add(formatPanel);
      
      // positionnement de l'étape courante
      setStepColor(step1,step2);
      
      // Indique le component à maximiser
      setMaxComp(scrollTree);
   }
   
   protected void createChaine() {
      super.createChaine();
      title  = aladin.chaine.getString("ALADINTITLE");
      BYTREE = aladin.chaine.getString("ALADINBYTREE");
      DEFFMT = aladin.chaine.getString("ALADINDEFFMT");
      aladinLabel    = aladin.chaine.getString("ALADINNAME");
      description   = aladin.chaine.getString("ALADININFO");
      verboseDescr   = aladin.chaine.getString("ALADINDESC");
      STEP1  = aladin.chaine.getString("ALADINSTEP1");
      STEP2  = aladin.chaine.getString("ALADINSTEP2");
      SERVERR = aladin.chaine.getString("ALADINSERVERR");
  }
   
   
   protected boolean is(String s) { return s.equalsIgnoreCase("Aladin"); }
   
  /** Retourne la chaine avec des blancs pour completer sa taille *
   * @param s La chaine initiale
   * @parma l La longueur desiree
   * @return La chaine modifiee
   */
  static protected String fixChar(String s, int l) {
     char [] a = s.toCharArray();
     char [] b = new char[l];
     for( int i=0; i<l; i++ ) {
        if( i<a.length ) b[i]=a[i];
        else b[i]=' ';
     }
     return new String(b);
  }

 /** Subsitue la chaine du format "JPEG" par "FITS
  * dans une url ou une marque GLU
  * @param la chaine a modifier
  * @param la chaine modifiee
  */
  static String change2FITS(String u) {
     int i=u.indexOf("JPEG");
     if( i<0 ) return u;
     return u.substring(0,i)+"FITS"+u.substring(i+4,u.length());
  }



  static String [] ACOLOR = {"J","F","E","EJ","V","O","R",
                          "I","S","SR","ER",
                          "U","B","N" };
  static String [] COLOR =  {"Blue","Red","Red","Equ. blue","Visible","Blue","Red",
                          "InfraRed", "Short red","Short red","Equ. red",
                          "UV","Blue","Red/IR" };
  static String lastQual="";
  static boolean flagContWhichQual=false; // Pour gerer les " de repetitions de lignes

  /** Retourne une chaine en langage naturel decrivant le qualifier
   * passe en parametre.
   * La variable de classe flagContWhichQual doit etre mise a false
   * si on ne veut pas de guillemets de repetition de ligne.
   * sinon il faut utiliser whichQualifier()
   * @param qual Le qualifier sous la forme SURVEY COLOR ORIGIN
   * @return la chaine en clair
   */
   static String whichQualifier(String qual) {
      flagContWhichQual=false;
      return whichQual(qual);
   }
   static String whichQual(String qual) {
      boolean ts = flagContWhichQual && qual.equals(lastQual);
      flagContWhichQual=true;
      StringBuffer res = new StringBuffer();
      StringTokenizer st = new StringTokenizer(qual);
      if( st.countTokens()<3 ) return qual;
      String s = st.nextToken();
      String c = st.nextToken();
      String o = st.nextToken();

      // Survey
      res.append(fixChar(ts?"   \"":s,6));

      // Origine
      res.append(fixChar(ts?"    \"   \"":"  "+(o.equals("MAMA")?"MAMA/CAI":
                 o.equalsIgnoreCase("STSCI")?"DSS1/STScI":
                 o.equalsIgnoreCase("DSS2")?"DSS2/STScI":
                 s.equalsIgnoreCase("2MASS")?"UMass/IRSA":o),12));

      // Couleur
      int i;
      if( s.equalsIgnoreCase("2MASS") || s.equalsIgnoreCase("EROSI")) {
         res.append(fixChar("  "+c,12));
      } else {
         for( i=0; i<ACOLOR.length; i++) if( ACOLOR[i].equals(c) ) break;
         if( i<ACOLOR.length ) res.append(fixChar(ts?"  \"  \"":"  "+c+" ("+COLOR[i]+")",12));
         else res.append(fixChar(ts?"  \"":"  "+c,12));
      }

      lastQual=qual;	// Pour comparer avec le qualifier suivant
      return res.toString();
   }

  /** Gestion du ENTER.
   * Pour pouvoir gerer le ENTER comme si on appuyait sur le bouton SUBMIT
   * et en meme temps, desactiver les caracteristiques des images
   */
//   public boolean keyDown(Event e,int key) {
//      if( e.target instanceof TextField) {
//         if( tree!=null && !tree.isEmpty() ) tree.clear();
//      }
//      return super.keyDown(e,key);
//   }

   private String targetT;
   private String criteriaT;
   private String labelT;
   private String originT;

   private boolean flagIDHASIAcall = false;
   private boolean flagCreatPlane = false;


   private boolean launchLock=false;
   private void freeLaunchLock() { launchLock=false; }
   synchronized private boolean getLaunchLock() {
      if( launchLock ) return false;
      launchLock=true;
      return true;
   }



   /** Creation d'un plan de maniere generique */
   synchronized protected  int createPlane(String target,String radius,String criteria,
   				 String label, String origin) {
      String format=null,resol=null,qual=null;
      
      // Pour enlever des quotes intempestives
      criteria = specialUnQuoteCriteria(criteria);

//System.out.println("Je recherche les criteres ["+criteria+"]");
      // Test si les criteres ne seraient pas complets  (ex: POSSI/E/STScI)
      // cad directement assimilable par le serveur d'image. Si oui,
      // on ne cherche pas les criteres par defaut
      if( criteria!=null && !isAllSky(criteria) ) {
         int pos1 = criteria.indexOf('/');
         int pos2 = criteria.indexOf('/',pos1+1);
         if( pos1>0 && pos2>pos1 ) {
            qual=criteria.replace('/',' ');
            resol="FULL";
            format="JPEG";
            criteria=null;
         }
      }

      // Recherche des meilleurs parametres en fonction des criteres
      // Dans un thread separe pour rendre assez vite la main
      if( criteria!=null ) {
         while( !getLaunchLock() ) Util.pause(500);
         targetT=target;
         criteriaT=criteria;
         originT=origin;
         labelT=label;

         setSync(false);	// bloque la synchronisation Command.sync()
         thread= new Thread(this,"AladinCreatePlane");
         flagCreatPlane=true;
         thread.start();
         return 0;		// 0, c'est pas terrible, mais on ne connait
                                // pas encore le numero du plan !
      }

      // Creation du plan avec les parametres specifiques
      return creatAladinPlane(target,format,resol,qual,label,origin);
   }

   public void run() {
      if( flagIDHASIAcall ) { flagIDHASIAcall=false; submitThread(); }
      else if( flagCreatPlane ) { flagCreatPlane=false; creatPlaneThread(); }
      setStepColor(step1,step2);
   }
   
   /** Retourne true s'il s'agit du ciel complet */
   protected boolean isAllSky(String s) { return s.equalsIgnoreCase("allsky"); }

   // Recherche des meilleurs parametres en fonction des criteres
   // Dans un thread separe pour rendre assez vite la main
   private void creatPlaneThread() {
      String qual=null,resol=null,format=null;

      // Je recupere les parametres que j'avais mis de cote
      String target=targetT;
      String criteria=criteriaT;
      String origin=originT;
      String label=labelT;

      // Je libere le prochain
      freeLaunchLock();
      
      // Particularité pour allsky
      if( isAllSky(criteria) ) {
         aladin.calque.newPlan("http://aladin.u-strasbg.fr/java/AllSky.fits","AllSky","Aladin image server");

      // Le cas général
      } else {

         // Acces aux qualifiers disponibles
         try {
            URL u = aladin.glu.getURL(getTagGlu(GLUDEFQUAL),Glu.quote(target)+" "+Glu.quote(criteria));
//            DataInputStream cat = new DataInputStream(u.openStream());
            DataInputStream cat = new DataInputStream(Util.openStream(u));
            qual=cat.readLine();
            resol=cat.readLine();
            format=cat.readLine();
         } catch( Exception e ) {}

         if( qual==null || qual.length()==0 ) Aladin.warning(this,aladin.chaine.getString("NOSUCHIMG")
               +" ["+criteria+"] ["+target+"]",1);

         else creatAladinPlane(target,format,resol,qual,label,origin);
      }
      setSync(true);	// libere la synchronisation Command.sync()
   }

   /** Creation d'un plan Aladin, parametres specifiques */
   protected int creatAladinPlane(String target,
   				 String format,String resol,String qual,
                                 String label, String origin) {
      URL u;

      // Default
      if( format==null ) format="JPEG";
      if( resol==null ) resol="FULL";

      // Construction et appel de l'URL
      String s = Glu.quote(target)+" "+Glu.quote(format)
                        +" "+Glu.quote(resol);

      if( qual!=null && qual.length()>0 ) s = s+" "+Glu.quote(qual);

      if( (u=aladin.glu.getURL(getTagGlu(GLUIMAGE),s))==null ) {
         Aladin.warning(this,WERROR,1);
         return -1;
      }

      // Verification de non-redondance
      if( !verif(Plan.IMAGE,target,qual, PlanImage.getFmt(format)+"/"+PlanImage.getRes(resol) ) ) return -1;

      // Generation automatique du label du plan
      if( label==null) label=getPlanLabel(resol,qual);

      // Positionnement de l'origine si non mentionne
      if( origin==null ) {
          if( qual.indexOf("MAMA")>=0 ) origin="CAI/Paris - provided by CDS image server";
          else if(qual.indexOf("DSS2")>=0 ) origin="STScI -  provided by CDS image server";
          else if(qual.indexOf("2MASS")>=0 ) origin="UMass/IRSA - provided by CDS image server";
          else origin="STScI - provided by CDS";
      }

      return aladin.calque.newPlanImage(u,PlanImage.ALADIN,label,
                                   target,qual,origin,
                                   PlanImage.getFmt(format),
                                   PlanImage.getRes(resol),
                                   null);
   }

   /**
    * Mise a jour de l'arbre de Thomas par Thread independant
    * pour ne pas bloquer l'affichage
    */
   private void submitThread() {
      waitCursor();
      URL url=null;

      try {
         String objc=null;
         String obj=null;
         String r;
         try {
            obj = getTarget();
            if( (r=getRadius(false))==null ) r="0";
            else r = ""+(getRM(r)/60.);
            objc = sesameIfRequired(obj,":");
            if( objc==null ) throw new Exception(UNKNOWNOBJ+" ["+obj+"]");
         } catch( Exception e1 ) {
            Aladin.warning(this,e1.getMessage(),1);
            defaultCursor();
            ball.setMode(Ball.NOK);
            return;
         }
         url = aladin.glu.getURL(getTagGlu(GLUQUALIFIER),
               Glu.quote(TreeView.getDeciCoord(objc))+
               Glu.quote(r));

//         MyInputStream is = new MyInputStream(url.openStream());
         MyInputStream is = Util.openStream(url);
         if( (is.getType() & (MyInputStream.IDHA|MyInputStream.SIA_SSA))==0 ) {
            String err = is.readLine().trim();
            Aladin.warning(this,SERVERR+
                           "\n\""+err+"\"");
            defaultCursor();
            ball.setMode(Ball.HS);
            return;
         }
         updateMetaData(is,this,obj,null);
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning(this,SERVERR,1);
      }
      defaultCursor();
      if( tree.isEmpty() ) ball.setMode(Ball.NOK);
   }

   /** Retourne les metadata correspondants à une position/radius */
   protected MyInputStream getMetaData(String target,String radius,StringBuffer infoUrl) throws Exception {
      if( radius!="" ) radius = getRM(radius)/60.+"";
      String objc = sesameIfRequired(target,":");
      if( objc==null ) throw new Exception();
      URL url = aladin.glu.getURL("Aladin.qualifierServer",
                   Glu.quote(TreeView.getDeciCoord(objc))+
                   Glu.quote(radius));
      infoUrl.append(url+"");
//      return new MyInputStream(url.openStream());
      return Util.openStream(url);
   }

   private boolean olistView=true;

//   // Gestion des evenement
//   public void actionPerformed(ActionEvent e) {
//      if( e.getSource() instanceof JRadioButton ) {
//         JRadioButton cb = (JRadioButton)e.getSource();
//         if( cb.equals(b1) || cb.equals(b2) ) {
//            changeViewMode();
//         }
//         return;
//      }
//
//      super.actionPerformed(e);
//   }

   /** change le mode d evisualisation : arbre ou a plat */
   private void changeViewMode() {
//       int i;

//       boolean listView = listTree.getSelection().getActionCommand().indexOf("list")>=0;
       boolean listView = !byTree.isSelected();
       if( olistView!=listView ) {
          tree.setFlat(listView);
          olistView=listView;
       }
   }

   /** Retourne le format choisi par l'utilisateur (FITS/JPEG) */
   protected String getDefaultFormat() {
      return format.getSelection().getActionCommand();
   }

  /** Interrogation d'Aladin */
   public void submit() {
      
      // Cas particulier pour le ciel complet
      String s = target.getText().trim();
      if( s!=null && isAllSky(s) ) {
         while( !getLaunchLock() ) Util.pause(500);
         criteriaT=s;
         creatPlaneThread();
         return;
      }

      // Recuperation et memorisation du target
      String obj = getTarget();
      if( obj==null ) return;
      memoTarget(obj);

      // Traitement des images par lot
      if( tree!=null && !tree.isEmpty() ) {
         if( tree.nbSelected()>0 ) {
             String format = getDefaultFormat();
// verif() IL FAUDRAIT AJOUTER LA VERIFICATION DU PLAN.
            tree.loadSelected(format);
            tree.resetCb();
         } else {
            // Si aucune ligne n'a ete cochee et si la Frameinfo est ouverte, je charge
            // l'image de cette derniere
            FrameInfo fi = aladin.getFrameInfo();
            if(  fi.isVisible() ) fi.load();
            else Aladin.warning(this,WNEEDCHECK);
         }
         return;
      }

      // Je cache la FrameInfo pour ne pas la prendre a tort par defaut
      // si l'utilisateur clique sur SUBMIT sans avoir coche qq chose
      // (Tu me suis ?) --> (ca va !)
      FrameInfo fi = aladin.getFrameInfo();
      if( fi.isVisible() ) fi.hide();

      // Chargement des descriptions des images disponibles (threade)
      thread= new Thread(this,"AladinServerMetaData");
      flagIDHASIAcall=true;
      Util.decreasePriority(Thread.currentThread(),thread);
//      thread.setPriority( Thread.NORM_PRIORITY -1);
      thread.start();
   }

  /** Construit le label du plan en fonction du qualifier et de la resolution
   * Exemple: PLATE SERC J STScI => Pl-SERC.J.DSS1
   */
  static protected String getPlanLabel(String resol, String qual) {
     return getPlanLabel( PlanImage.getRes(resol),qual);
  }
  static protected String getPlanLabel(int resol, String qual) {
     StringBuffer s = new StringBuffer();
     String [] f = new String[3];
     
     // On decoupe les trois champs SURVEY COLOR SCAN
     StringTokenizer st = new StringTokenizer(qual);
     for( int i=0; i<3; i++ ) f[i] = st.nextToken();
     
     if( f[0].indexOf("2MASS")>=0 ) { f[2]=f[0]; f[0]=null; }
     
     // Et on les assemble dans le sesn inverse eventuellement
     // precedes par l'indication Pl- pour Plate
     if( resol==PlanImage.PLATE ) s.append("Pl-");
     else if( resol==PlanImage.LOW ) s.append("Lw-");
     if( f[2].equals("STScI") || f[2].equals("STSCI") ) s.append("DSS1.");
     else if( !f[2].startsWith("___") ) s.append(f[2]+".");
     s.append(f[1]);
     if( f[0]!=null ) s.append("."+f[0]);
     
     return s.toString();
  }


  static String otarget=null;
  boolean flagClear=true;	// Une horreur pour eviter un clear()
                                // intempestif lors d'un reset

  /** Memorisation du target de la derniere interrogation aladin */
  protected void memoTarget(String s) { otarget=s; }

  /** Pre-remplissage du champ target
   * Si il ne s'agit pas de la meme chaine que la derneire interro d'aladin,
   * le formulaire est resete (pour effacer les images disponibles qui ne
   * correspondront plus)
   * @param s La chaine a mettre dans le champ target
   */
   protected void setTarget(String s) {
      if( flagClear && otarget!=null && !otarget.equals(s) ) {
         clear();
      }
      flagClear=true;
      super.setTarget(s);
   }

  /** Remplace les blancs par des soulignes */
   protected static String blankToUnderline(String s) {
      char [] a = s.toCharArray();
      for( int i=0; i<a.length; i++ ) if( a[i]==' ') a[i]='_';
      return new String(a);
   }

  /** Reset du formulaire */
   protected void reset() {
      tree.resetCb();
      flagClear=false;
      super.reset();
      setStepColor(step1,step2);
   }

  /** Clear du formulaire */
   protected void clear() {
      tree.clear();
      super.clear();
      if( radius!=null ) radius.setText("0 arcmin");
      setStepColor(step1,step2);
   }

   /** Implémentation de l'interface MyListener */
   public void fireStateChange(String state) {
       if( state.equals(MetaDataTree.FLAT_VIEW) ) {
          byTree.setSelected(false);
//           listTree.setSelected(b1.getModel(),true);
           changeViewMode();
       }
       else if( state.equals(MetaDataTree.HIER_VIEW) ) {
          byTree.setSelected(true);
//           listTree.setSelected(b2.getModel(),true);
           changeViewMode();
       }
   }

   public void fireStateChange(int i) {}
   
}
