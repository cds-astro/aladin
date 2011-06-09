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

import cds.tools.Util;
import cds.vizier.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.util.*;

import javax.swing.*;

/**
 * Le formulaire d'interrogation des Missions
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : jan 03 - Suppression du Layout Manager et toilettage
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class ServerVizieRMission extends Server  {

   // Les references
   Vector vArchives;

   // les composantes de l'objet
   VizieRTable missionlist;        // La liste des missions proposees
   JTextField mission;            // le champ de saisie d'une mission
   MyLabel currentlist;           // legende courante de la liste
   JButton getReadMe;             // Le bouton pour demander des infos
   protected String default_methode;
   protected String help_list;
   protected String nomTextfield;
   JCheckBox cbGetAll,cbGetAllCat;
   protected String CATDESC,TAGGLU= "VizieRXML++",GETALL,GETALL1,ALLCAT;

   // Pour pouvoir overider les variables pour les classes derivees
   protected void init() {
      aladinLogo = "VizieRMLogo.gif";
      docUser="http://vizier.u-strasbg.fr";
  }

   protected void createChaine() {
      super.createChaine();
      aladinLabel       = aladin.chaine.getString("ARNAME");
      nomTextfield= aladin.chaine.getString("ARCAT");
      title     = aladin.chaine.getString("ARTITLE");
      description      = aladin.chaine.getString("ARINFO");
      default_methode = aladin.chaine.getString("ARINFO1");
      help_list = aladin.chaine.getString("ARINFO2");
      CATDESC   = aladin.chaine.getString("ARCATDESC");
      verboseDescr      = aladin.chaine.getString("ARDESC");
      GETALL    = aladin.chaine.getString("VZGETALL2");
      GETALL1   = aladin.chaine.getString("VZGETALL3");
      ALLCAT    = aladin.chaine.getString("VZALLCAT");
   }

 /** Creation du formulaire d'interrogation des missions
   * @param aladin reference
   * @param status le label qui affichera l'etat courant
   * @param vArchives La liste des archives
   */
   protected ServerVizieRMission(Aladin aladin,Vector vArchives) {
      this.aladin = aladin;
      createChaine();
      init();
      type = CATALOG;

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=0;

      // Le titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,aladinLabel);
      tp.setBounds(XWIDTH/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);

      // Premiere indication
      StringTokenizer st = new StringTokenizer(default_methode,"\n");
      while( st.hasMoreTokens() ) {
         JLabel info = new JLabel(st.nextToken());
         info.setBounds(86,y,400, 20); y+=15;
         add(info);
      }
      y+=5;

      int yGetAll = y+15;
      int xGetAll = XWIDTH-90-15;

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
      if( !Aladin.OUTREACH ) {
         cbGetAll.setBackground(Aladin.BLUE);
         cbGetAll.setBounds(xGetAll,yGetAll,120,20); yGetAll+=20;
         add(cbGetAll);
      }

      // La checkbox du getWholeCat
      cbGetAllCat=new JCheckBox(GETALL1,false);
      cbGetAllCat.setBackground(Aladin.BLUE);
      cbGetAllCat.setBounds(xGetAll,yGetAll,120,20); yGetAll+=20;
      cbGetAllCat.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean flag=!cbGetAllCat.isSelected();
            target.setEnabled(flag);
            radius.setEnabled(flag);
         }
      });
      if( !Aladin.OUTREACH && !(this instanceof ServerVizieRSurvey) ) add(cbGetAllCat);

      // Catalog + Radius
      JLabel label1 = new JLabel(addDot(nomTextfield));
      label1.setBackground(Aladin.BLUE);
      label1.setFont(Aladin.BOLD);
      int l= 55+30;
      label1.setBounds(XTAB1,y,l,HAUT);
      add(label1);
      mission = new JTextField(28);
      mission.setBounds(l+15,y,150-30,HAUT);
      mission.addKeyListener(this);
      add(mission);
      JLabel label2 = new JLabel(addDot(RAD));
      label2.setBackground(Aladin.BLUE);
      label2.setFont(Aladin.BOLD);
      label2.setBounds(246-15,y,l,HAUT);
      add(label2);
      radius = new JTextField("10 arcmin");
      radius.addKeyListener(this);
      radius.setBounds(294-15,y,Aladin.OUTREACH?60:105,HAUT); y+=HAUT+MARGE;
      add(radius);
      modeRad=RADIUS;

      // Deuxieme indication
      JLabel info2 = new JLabel(help_list);
      info2.setBounds(56,y,400, 20); y+=20;
      add(info2);

      // Bouton Get info.
      JButton getReadMe = new JButton(CATDESC);
      getReadMe.setOpaque(false);
      getReadMe.addActionListener(this);

      // La liste des Missions
      missionlist = new VizieRTable(mission,getReadMe,vArchives,12,VizieRTable.SURVEY_MODE);
      missionlist.setFont( Aladin.PLAIN );
      JScrollPane jc = new JScrollPane(missionlist);
      jc.setBounds(XTAB1,y,XWIDTH-2*XTAB1,180); y+=190;
      add(jc);

      // La suite du bouton Get info.
//      getReadMe.setFont( Aladin.BOLD);
      if( !Aladin.OUTREACH ) {
         getReadMe.setEnabled(false);
         getReadMe.setBounds(300,y,100,25);
         add(getReadMe);
      }

      // Indication du component à maximiser
      setMaxComp(jc);

   }

   /** Positionne si possible le paramètre principal dans le formulaire,
    * dans ce cas le nom d'un catalog */
   protected boolean setParam(String param) {
      Vector v = missionlist.getList();
      Enumeration e = v.elements();
      StringTokenizer st = new StringTokenizer(param);
      String id = st.nextToken();

      while( e.hasMoreElements() ) {
         String s = (String)e.nextElement();
         st = new StringTokenizer(s, "\t");
         String sId = st.nextToken();

         if( sId.equals(id) ) {
            mission.setText(id);
            missionlist.selectCatalog(id);
            return true;
         }
      }
      return false;
   }

   protected String [] getNomPaths() {
      String n = super.getNomPaths()[0];
      Vector v = missionlist.getList();
      String res[] = new String[v.size()];
      Enumeration e = v.elements();
      StringTokenizer st;
      for( int i=0; e.hasMoreElements(); i++ ) {
         st = new StringTokenizer((String)e.nextElement(), "\t");
         res[i] = n+"/"+Util.slash(st.nextToken()+" - "+st.nextToken());
      }
      return res;
   }

   /** Creation d'un plan de maniere generique */
   protected int createPlane(String target,String radius,String criteria,
   				 String label, String origin) {

      // Pour enlever des quotes intempestives
      criteria = specialUnQuoteCriteria(criteria);

      String catalogs=criteria;	// EN PREMIERE APPOCHE...
      if( label==null ) label=catalogs;
      return creatArchivePlane(target,radius,catalogs,label,origin,cbGetAll.isSelected());
   }

   /** Creation d'un plan de maniere specifique */
   protected int creatArchivePlane(String target,String radius,String catalogs,
   				 String label, String origin,boolean allColumns ) {
      URL u;

      // On enlève l'éventuel unité
      radius = getRM(radius)+"";

      String s = Glu.quote(catalogs)+" "+Glu.quote(target)+" "+Glu.quote(radius);
      if( allColumns ) s = s+" -out.all";

      if( (u=aladin.glu.getURL(TAGGLU,s))==null ) {
         Aladin.warning(this,WERROR);
         return -1;
      }
      if( !verif(Plan.CATALOG,target,label+" "+radius) ) return -1;
      return aladin.calque.newPlanCatalog(u,label,target,label+" "+radius,origin,this);
   }

  /** Interrogation des missions, soit par Vizir soit directement aux archives */
   public void submit() {
      URL u;
      String objet="",cata,r;
      double rm=0;

      boolean allcat = cbGetAllCat.isSelected();

      if( !allcat && (objet=getTarget())==null ) return;
      cata = mission.getText().trim();
      if( cata.equals("") ) { Aladin.warning(this,WNEEDCAT); return; }


      if( allcat || objet==null || objet.length()==0) {
         if( cata.length()==0 ) { Aladin.warning(this,WNEEDCAT); return; }
         if( !Aladin.confirmation(this,ALLCAT+" \""+cata+"\" ?") ) return;
         objet="";
         rm=0;
      } else if( !allcat ) {
         if( (r=getRadius())==null ) return;
         rm = getRM(r);
      }

      if( allcat ) objet="";

//      String t= new String(objet);
      waitCursor();

      resetList();

      String s="";
      if( cbGetAll.isSelected() ) s=",allcolumns";
      if( !cbGetAllCat.isSelected() && objet!=null && objet.length()>0 ) aladin.console.setCommand("get VizieR("+cata+s+") "+objet+" "+Coord.getUnit(rm/60.));
      else aladin.console.setCommand("get VizieR("+cata+s+")");

      createPlane(objet,rm+"",cata,null,institute);
      
      resetFlagBoxes();
      defaultCursor();
   }


   protected void resetList() {
      missionlist.resetList();
//      for( int i=missionlist.countItems()-1; i>=0; i-- ) missionlist.deselect(i);
   }

  /** Clear du formulaire */
   protected void clear() {
      super.clear();
      mission.setText("");
      resetList();
   }

   /** Reset du formulaire */
   protected void reset() {
      super.reset();
      mission.setText("");
      resetList();
      resetFlagBoxes();
   }
   
   protected void resetFlagBoxes() {
      cbGetAll.setSelected(false);
      cbGetAllCat.setSelected(false);
      target.setEnabled(true);
      radius.setEnabled(true);
   }


  /** Re-affichage avec regeneration du panel du formulaire.
   * Rq : Eh oui, il faut bien ruser pour supporter Netscape 3.0
   */
   protected void reaffiche() {
//      try{ invalidate(); validate(); } catch( Exception e ) {}
      hide();show();
   }

  /** Gestion des evenements. */
   public void actionPerformed(ActionEvent e) {
      Object s = e.getSource();

      // Affichage via Netscape du Readme du missionue courant
      if( s instanceof JButton
            && ((JButton)s).getActionCommand().equals(CATDESC)) {
         String cata = mission.getText().trim();
         if( cata.equals("") ) { Aladin.warning(this,WNEEDCAT); return; }

         cata = Glu.quote(cata);
         aladin.glu.showDocument("getReadMe",cata);
         return;
      }

      super.actionPerformed(e);
   }

}
