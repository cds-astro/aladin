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
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;

import cds.tools.Util;

/**
 * Gestion de la pile des plans
 *
 * @author Pierre Fernique [CDS]
 *               (mai 2006) Ajout des items PLASTIC dans le PopupMenu et des actions associ�es (thomas)
 * @version 1.3: (oct 2004) Passage en multivue
 * @version 1.2: (11 fev 2003) autre methode de blink (JVM 1.4)
 * @version 1.1: (29 octobre 2002) scrollbar + folder + filter
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.91 : revisite le 25 nov 1998
 * @version 0.9 : (??) creation
 */
public final class Select extends JComponent  implements
             AdjustmentListener,ActionListener,
             MouseMotionListener, MouseListener, MouseWheelListener, 
             Runnable, WidgetFinder {

   String HSTACK0,HSTACK,HEYE,WAITMIN,NOPROJ,MSELECT,MBROADCASTALL,MALLAPPS,MBROADCASTTABLE,MBROADCASTIMAGE,
          MDEL,MDELALL,MDELEMPTY,MCREATFOLD,HSTACK2,
          MINSFOLD,MCOL,MEXP,MPROP,SHOW,GOTO,HIDE,WARNING,WARNINGSLIDER;
   String [] BEGIN;

   // Les references aux autres objets
   Aladin a;

   // Le menu Popup
   JPopupMenu popMenu;

   // Les parametres a memoriser
   Vector slides=null;		      // la liste des slides courants
   static boolean firstEye=true;  // Vrai si l'oeil a deja ete calcule
   int flagDrag=0;                // Vrai si on drag
   Plan currentPlan=null;	      // Plan en cours de manipulation
   int x,y;                       // Derniere position de la souris
   int oldx,oldy;                 // Pour pouvoir tester la distance minimal de 4 pixels
   Polygon logo;                  // Logo en cours de deplacement
   Rectangle r;                   // Clip en cas de repaint (eye, blink)
   boolean flagRClip;             // Vrai si le clip "r" doit etre utilise
//   boolean clinDoeil=false;       // Vrai si je dois faire un clin d'oeil
//   Thread clin=null;              // Thread d'attente pour le clin d'oeil
   Plan newRef=null;              // !=null si on est entrain de changer de ref.
   Plan planIn=null;		      // si !=null, dernier plan sous la souris

   Thread thread = null;   	  // thread pour pour le clignotement

   // Constantes dans le cas d'un clic&drag de plan
   static final int VERTICAL   = 1;
   static final int HORIZONTAL = 2;

   // Les valeurs accociees aux differents elements graphiques
   static final int sizeLabel = 100;//89/*112-MyScrollbar.LARGEUR*/;   // Nbre de pixels pour les labels
   // test AVO
   //static final int sizeLabel = 156-MyScrollbar.LARGEUR;   // Nbre de pixels pour les labels (test AVO)
   static final int gapL      =   16;   // Marge de gauche (reserve pour les controles)
//   static final int gapLL      =  14;   // Marge de gauche (reserve pour le radio)
   static final int gapB      =   5;   // Marge du bas
   static final int DX	      =  33;   // Largeur du logo

   // L'icone du calque
   static final int [] frX =  { 6+gapL, 0+gapL, DX+gapL, DX-6+gapL,6+gapL  };
   static final int [] frY =  {      1,      14,      14,       1,      1  };
   static final int frMin = frX[1];              // Limite inf. de l'icone (en abs)
   static final int frMax = frX[2];              // Limite sup. de l'icone (en abs)
   static final int MILIEU = (frX[2]+frX[1])/2;       // Le centre de la pile des calques

   // L'oeil (sourcil - ext - int - pupille)
//   static int [] o1x = { 0,11,22,36,36,22,10, 0,0 };      // Sourcil
//   static int [] o1y = { 10, 6, 6, 13,15, 9, 9,13,10 };
//   static int [] o2x = {  2, 3,12,18,24,32,36,36,32,18, 6, 2, 2 }; // Exterieur de l'oeil
//   static int [] o2y = { 18,18,12,10,12,18,20,23,22,24,21,21,18 };
//   static int [] o3x = {  6,16,22,28,30,18, 6 };         // Interieur de l'oeil
//   static int [] o3y = { 18,12,14,19,19,21,18 };
//   static int o4d = o3y[5]-o2y[3];                        // Taille (hors tout) de la pupille
//   static int o4xc = o2x[3]-o4d/2;                        // Abscisse du centre de la pupille
//   static int o4yc = (o2y[3]+o3y[5])/2-o4d/2;            // Ordonnee du centre de la pupille
//   static int eyeWidth=o1x[3];                           // Largeur
//   static int eyeHeight=o2y[7];                           // Hauteur

   // Les variables de gestion du graphisme
   static int ws=frMax+sizeLabel;        // Largeur du canvas (doit etre divisible par ViewZoom.WENZOOM)
   int hs;                        // Hauteur du canvas
   int hsp;                       // Hauteur de la portion pour les plans
   Image img;                     // Image du buffer du paint
   Graphics g;                    // GC du buffer du paint
//   private boolean mouseIn=false; // true si la souris est sur l'oeil

   protected void createChaine() {
      String appMsgProtocolName = a.getMessagingMgr().getProtocolName();
      HSTACK0 = a.chaine.getString("SLHSTACK0");
      HSTACK = a.chaine.getString("SLHSTACK");
      HSTACK2 = a.chaine.getString("SLHSTACK2");
      HEYE = a.chaine.getString("SLHEYE");
      WAITMIN = a.chaine.getString("SLWAITMIN");
      NOPROJ = a.chaine.getString("SLNOPROJ");
      MALLAPPS = a.chaine.getString("SLMALLAPPS").replaceAll("SAMP", appMsgProtocolName);
      MBROADCASTALL = a.chaine.getString("SLMBDCASTPLANES").replaceAll("SAMP", appMsgProtocolName);
      MBROADCASTTABLE = a.chaine.getString("SLMBDCASTTABLES");
      MBROADCASTIMAGE = a.chaine.getString("SLMBDCASTIMAGES");
      MDEL = a.chaine.getString("SLMDEL");
      MDELALL = a.chaine.getString("SLMDELALL");
      MDELEMPTY = a.chaine.getString("SLMDELEMPTY");
      MCREATFOLD = a.chaine.getString("SLMCREATFOLD");
      MINSFOLD = a.chaine.getString("SLMINSFOLD");
      MCOL = a.chaine.getString("SLMCOL");
      MEXP = a.chaine.getString("SLMEXP");
      MPROP = a.chaine.getString("MPROP");
      GOTO = a.chaine.getString("GOTO");
      SHOW = a.chaine.getString("SHOW");
      HIDE = a.chaine.getString("HIDE");
      WARNING = a.chaine.getString("SWARNING");
      WARNINGSLIDER = a.chaine.getString("SWARNINGSLIDER");
   }

  /** Creation de l'interface de la pile des plans.
   * @param calque,aladin References
   */
   protected Select(Aladin aladin) {
      this.a = aladin;
      createChaine();
      addMouseMotionListener(this);
      addMouseListener(this);
      addMouseWheelListener(this);
      
      setBackground(Aladin.NEWLOOK_V7 ? a.toolBox.getBackground() : Aladin.LBLUE );
//      if( Aladin.NEWLOOK_V7 ) eyeHeight=0;

      // Calcule des tailles
      hs=Aladin.LSCREEN?291:200;   // Hauteur du canvas
      hsp= hs-/*eyeHeight-*/gapB;      // Hauteur de la portion pour les plans
      createPopupMenu();
   }
   
   
   private long lastMouseWheelEventTime = -1;
   
   /** Modification de la transparence du plan sous la souris par action sur la molette */
   public void mouseWheelMoved(MouseWheelEvent e) {
      if( e.getClickCount()==2 ) return;    // SOUS LINUX, J'ai un double �v�nement � chaque fois !!!
      int sens = -e.getWheelRotation();
      Plan p = getPlan(e.getY());
      if( p==null || !a.calque.canBeTransparent(p) ) return;
      float opacity = p.getOpacityLevel();
      float oOpacity=opacity;
      long delta = e.getWhen()-lastMouseWheelEventTime;
      lastMouseWheelEventTime = e.getWhen();
      float acc = delta<50 ? 0.4f : delta<100 ? 0.2f : delta<200 ? 0.1f : 0.05f;
      opacity += sens*acc;
      if( opacity<0 ) opacity=0f;
      else if( opacity>1 ) opacity=1f;
      if( opacity==oOpacity ) return;
      setOpacityLevel( p, opacity );
      Properties.majProp(p);
      a.calque.repaintAll();
   }
   
   // Ajustement de la transparence avec r�activation automatique du plan si n�cessaire */
   private void setOpacityLevel(Plan p,float opacity) {
      p.setOpacityLevel( opacity );
      if( !p.active && opacity>0.1 ) p.setActivated(true);
   }

//   public Dimension getPreferredSize() { return new Dimension(ws+5,hs); }

   JMenuItem menuBroadcast,menuDel,menuDelEmpty,menuDelAll,menuShow,menuGoto,
            menuColl,menuCreatFold,menuInsertFold,menuProp,menuSelect,menuUnselect,
            menuConcat1,menuConcat2,menuTableInfo,menuPlot,menuCreateMulti,menuCreateUniq;

   JMenu menuBroadcastTable,menuBroadcastImg,menuConcat,menuExport;

   // Cree le popup menu associe au select
   private void createPopupMenu() {
      popMenu = new JPopupMenu();
      JMenuItem j;
      popMenu.add( menuShow=j=new JMenuItem(SHOW));
      j.addActionListener(this);
      popMenu.add( menuGoto=j=new JMenuItem(GOTO));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuSelect=j=new JMenuItem(a.SELECT));
      j.addActionListener(this);
      popMenu.add( menuUnselect=j=new JMenuItem(a.UNSELECT));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuPlot=j=new JMenuItem(a.PLOTCAT));
      j.addActionListener(this);
      popMenu.add( menuExport=new JMenu(a.chaine.getString("VWCPLANE")));
      menuExport.add( menuCreateMulti=j=new JMenuItem(a.chaine.getString("VWCPLANEUNIQ")));
      j.addActionListener(this);
      menuExport.add( menuCreateUniq=j=new JMenuItem(a.chaine.getString("VWCPLANEMULTI")));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuDel=j=new JMenuItem(MDEL));
      j.addActionListener(this);
      popMenu.add( menuDelEmpty=j=new JMenuItem(MDELEMPTY));
      j.addActionListener(this);
      popMenu.add( menuDelAll=j=new JMenuItem(MDELALL));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuCreatFold=j=new JMenuItem(MCREATFOLD));
      j.addActionListener(this);
      popMenu.add( menuInsertFold=j=new JMenuItem(MINSFOLD));
      j.addActionListener(this);
      popMenu.add( menuColl=j=new JMenuItem(MCOL));
      j.addActionListener(this);
      if( Aladin.PLASTIC_SUPPORT ) {
         popMenu.addSeparator();
         popMenu.add( menuBroadcast = j= new JMenuItem(MBROADCASTALL));
         j.addActionListener(this);
         popMenu.add( menuBroadcastTable = new JMenu(MBROADCASTTABLE) );
         popMenu.add( menuBroadcastImg = new JMenu(MBROADCASTIMAGE) );
      }
      popMenu.addSeparator();
      popMenu.add( menuConcat=new JMenu(a.CONCAT));
      menuConcat.add( menuConcat1=j=new JMenuItem(a.CONCAT1));
      j.addActionListener(this);
      menuConcat.add( menuConcat2=j=new JMenuItem(a.CONCAT2));
      j.addActionListener(this);
      popMenu.add( menuTableInfo=j=new JMenuItem(a.TABLEINFO));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuProp=j=new JMenuItem(MPROP));
      j.addActionListener(this);
      add(popMenu);
   }

   /** Reactions aux differents boutons du menu */
    public void actionPerformed(ActionEvent e) {
       Object src = e.getSource();
       String o="";

       if( src instanceof JMenuItem && ((JMenuItem)src).getParent() instanceof JMenu ) {
          o=((JMenuItem)src).getActionCommand();
       }

            if( src==menuCreatFold )  a.fold();
       else if( src==menuTableInfo )  a.tableInfo(null);
       else if( src==menuPlot )       a.createPlotCat();
       else if( src==menuConcat1 )    a.concat(true);
       else if( src==menuConcat2 )    a.concat(false);
       else if( src==menuInsertFold ) insertFolder();
       else if( src==menuSelect )     a.select();
       else if( src==menuUnselect )   a.unSelect();
       else if( src==menuCreateMulti )a.cloneObj(false);
       else if( src==menuCreateUniq ) a.cloneObj(true);
       else if( src==menuShow )       a.calque.setActivatedSet(((JMenuItem)src).getActionCommand().equals(SHOW));
       else if( src==menuGoto )       a.view.syncPlan(a.calque.getFirstSelectedPlan());
       else if( src==menuDel )        a.calque.FreeSet(true);
       else if( src==menuDelEmpty )   a.calque.FreeEmpty();
       else if( src==menuDelAll )     a.calque.FreeAll();
       else if( src==menuBroadcast )  a.broadcastSelectedPlanes(null);
       else if( src==menuProp )       propertiesOfSelectedPlanes();
       else if( src==menuColl ) {
          switchCollapseFolder( a.calque.getFirstSelectedPlan() );
//          for( int i=menuPlan.size()-1; i>=0; i-- ) {
//             switchCollapseFolder((Plan)menuPlan.elementAt(i));
//          }
       // envoi de plans catalogues via PLASTIC
       } else if( src instanceof JMenuItem && ((JMenuItem)src).getActionCommand().equals(MBROADCASTTABLE )) {
          o = ((JMenuItem)src).getText();
          // broadcast � toutes les applis
          if( o.equals(MALLAPPS) ) {
             a.broadcastSelectedTables(null);
          }
          else {
             a.broadcastSelectedTables(new String[]{o.toString()});
          }
       // envoi de plans images via PLASTIC
       } else if( src instanceof JMenuItem
             && ((JMenuItem)src).getActionCommand().equals(MBROADCASTIMAGE)  ) {
           o = ((JMenuItem)src).getText();
           // broadcast � toutes les applis
          if( o.equals(MALLAPPS) ) {
             a.broadcastSelectedImages(null);
          }
          // envoi � une appli particuli�re
          else {
             a.broadcastSelectedImages(new String[]{o.toString()});
          }

       } else return;

       a.calque.repaintAll();
   }

    /** Affichage des propri�t�s des plans s�lectionn�s */
    protected void propertiesOfSelectedPlanes() {
       Plan [] allPlan = a.calque.getPlans();
       for( int i=0; i<allPlan.length; i++ ) {
          Plan p = allPlan[i];
          if( p.type!=Plan.NO && p.selected ) Properties.createProperties(p);
       }
    }

   // demo AVO
//   private void exportAsVOT(PlanCatalog pc) {
//      FileDialog fd = new FileDialog(a.f,"Choose file",FileDialog.SAVE);
//      fd.setFile(pc.label.replace(' ','_')+"-"+pc.objet.replace(' ','_')+".xml");
//      fd.show();
//      String dir = fd.getDirectory();
//      String name =  fd.getFile();
//      String s = (dir==null?"":dir)+(name==null?"":name);
//      if( name==null ) return;
//      File file = new File(s);
//
//      try {
//         DataOutputStream out = new DataOutputStream(
//            new BufferedOutputStream(new FileOutputStream(file)));
//         a.writePlaneInVOTable(pc, out);
//         out.close();
//      }
//      catch(IOException ioe) {Aladin.warning(this,"I/O error, could not export plane : "+ioe);}
//
//   }


   Vector menuPlan=null;

   // Affiche le popup
   private void showPopMenu(int x,int y) {
      boolean allShow = true;
      boolean allCollapse = true;
      boolean canBeRef = true;
      boolean canBeColl = true;
      boolean hasEmpty = false;
      int nbCatalog=0;
      int nbImg=0;
      int nbTool=0;
      menuPlan = new Vector(10);

      Plan [] plan = a.calque.getPlans();
      for( int i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];
         hasEmpty = hasEmpty || pc.isEmpty();
         if( !pc.selected ) continue;
         if( pc.isCatalog() && pc.flagOk ) nbCatalog++;
         if( pc.type==Plan.TOOL && pc.flagOk ) nbTool++;
         if( pc.type==Plan.IMAGE && pc.flagOk ) nbImg++;
         if( pc.type==Plan.IMAGEHUGE && pc.flagOk ) nbImg++;
         menuPlan.addElement(pc);
         allShow = allShow && pc.active;
         canBeColl = canBeColl && pc.type==Plan.FOLDER;
         allCollapse = allCollapse && a.calque.isCollapsed(pc);
         canBeRef = a.calque.canBeRef(pc) && pc.ref==false
                              && pc.flagOk;
      }
      canBeRef = canBeRef && menuPlan.size()==1;

      if( !allCollapse ) menuColl.setText(MCOL);
      else menuColl.setText(MEXP);
      if( allShow ) menuShow.setText(HIDE);
      else menuShow.setText(SHOW);
      menuShow.setEnabled(!a.calque.isFree());
      menuColl.setEnabled(canBeColl && menuPlan.size()>0);
      menuInsertFold.setEnabled(menuPlan.size()>0);
      menuProp.setEnabled(menuPlan.size()>0);
      menuDel.setEnabled(menuPlan.size()>0);
      menuDelAll.setEnabled(menuPlan.size()>0);
      menuDelEmpty.setEnabled(hasEmpty);
      menuShow.setEnabled(menuPlan.size()>0);
      menuSelect.setEnabled(nbCatalog>0 || nbTool>0);
      menuTableInfo.setEnabled(nbCatalog>0);
      menuPlot.setEnabled(nbCatalog>0);
      menuConcat.setEnabled(nbCatalog>1);
      menuUnselect.setEnabled(a.view.hasSelectedObj());
      menuExport.setEnabled(a.view.hasSelectedSource());

      // Activation des items relatifs � PLASTIC
      if( Aladin.PLASTIC_SUPPORT ) {
         AppMessagingInterface mMgr = a.getMessagingMgr();

         boolean canBroadcast = mMgr.isRegistered()
                                 && (nbCatalog>0 || nbImg>0);;

         ArrayList<String> imgApps = mMgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS);
         ArrayList<String> tabApps = mMgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_VOT_FROM_URL);
         menuBroadcast.setEnabled(canBroadcast && (tabApps.size()>0 || imgApps.size()>0));
         menuBroadcastTable.setEnabled(canBroadcast&&nbCatalog>0 && tabApps.size()>0);
         menuBroadcastImg.setEnabled(canBroadcast&&nbImg>0 && imgApps.size()>0);

         JMenuItem item;

         // pour envoi des catalogues s�lectionn�s
         menuBroadcastTable.removeAll();
         menuBroadcastTable.add(item = new JMenuItem(MALLAPPS));
         item.setActionCommand(MBROADCASTTABLE);
         item.addActionListener(this);
         menuBroadcastTable.addSeparator();
         if( tabApps!=null ) {
         	for (String appName: tabApps) {
         		menuBroadcastTable.add(item = new JMenuItem(appName));
         		item.setActionCommand(MBROADCASTTABLE);
                item.addActionListener(this);
         	}
         }

         // pour envoi des images s�lectionn�es
         menuBroadcastImg.removeAll();
         menuBroadcastImg.add(item = new JMenuItem(MALLAPPS));
         item.setActionCommand(MBROADCASTIMAGE);
         item.addActionListener(this);
         menuBroadcastImg.addSeparator();
         if( imgApps!=null ) {
         	for (String appName: imgApps) {
         		menuBroadcastImg.add(item = new JMenuItem(appName));
         		item.setActionCommand(MBROADCASTIMAGE);
                item.addActionListener(this);
         	}
         }

      }
      popMenu.show(this,x-15,y);
   }

//  /** Retourne vrai si on est dans l'oeil.
//   * @param x,y Position de la souris
//   * @return <I>true</I> si on est dans l'oeil <I>false</I> sinon
//   */
//   protected boolean inEye(int x, int y) { return(x-gapL<eyeWidth && y<eyeHeight); }


  /** Generation si necessaire du message "Image in progress...".
   * @param p le plan a attendre
   * @return <I>true</I> si c'est pret <I>false</I> sinon
   */
   protected boolean planOk(Plan p) {
      String s;
      if( p.type!=Plan.NO && p.type!=Plan.FILTER && !p.flagOk ) {
         if( p.error==null ) {
//            if( p.isImage() ) s=WAITMIN+"\n \n"+((PlanImage)p).getStatus();
//            else s=WAITMIN;
//            Aladin.warning(s);

            a.status.setText("*** "+WAITMIN+" ***");
         }
         return false;
      }
      return true;
   }

   boolean memoBoutonDroit = false;
  /** Gestion de la souris */
   public void mousePressed(MouseEvent e) {
       if( a.inHelp ) return;

       int x = e.getX();
       int y = e.getY();

      // On va ouvrir le Popup sur le mouseUp()
      if( e.isPopupTrigger() ) {
          // indispensable car sous MacOS, isPopupTrigger ne r�pond true que dans mousePressed, pas dans mouseReleased
          memoBoutonDroit = true;
          return;
      }

      // Fin du message d'accueil
      a.endMsg();

      // Juste pour faire plaisir � Seb
      if( a.calque.isFree() ) { a.dialog.show(); return; }
      
//      // Gestion de l'oeil (selection de tous les plans simultanement, ou avec
//      if( inEye(x,y) ) {
//         a.calque.clinDoeil();
//         a.view.repaintAll();
//         clinDoeil();
//      }

      // Recherche du plan clique
      if( (currentPlan = getPlan(y))==null ) return;

      // Par defaut, memorisation de la position
      this.x=x; this.y=y;
      oldy=y; oldx=x;
   }

//   /** Pour faire un clin d'oeil */
//   protected void clinDoeil() {
//      clinDoeil=true;
//      repaint();
//   }

   /** Permet de d�terminer si le plan peut devenir de r�f�rence
    * comme si l'utilisateur avait cliqu� sur le petit triangle */
   protected boolean canBeNewRef(Plan p) {
      return canBeNewRef(null,frMin+3,p);
   }

   long lastClick = -1;

   /** D�termine s'il s'agit d'un clic souris destin� � changer le plan de r�f�rence */
   protected boolean canBeNewRef(MouseEvent e, int x,Plan currentPlan) {
//      if( e==null ) return false;
      boolean shiftDown = e!=null && e.isShiftDown();

//      System.out.println("Double clic: "+(e.getWhen()-lastClick)+"ms");

      if( e!=null && e.getClickCount()==1 ) lastClick=e.getWhen();


      // AJOUT POUR LA VERSION 7.5 - NE MARCHE QUE SUR LA COCHE
      if( x>gapL ) return false;

      // Un plan non valide ne peut �tre pris comme r�f�rence
      if( !currentPlan.flagOk ) return false;

      // les plans FILTER et CONTOUR ne peuvent PAS etre pris comme r�f�rence
      if( currentPlan.type==Plan.FILTER
            || currentPlan instanceof PlanContour
            || currentPlan.type==Plan.FOLDER
//            || currentPlan.type==Plan.CATALOG
            || currentPlan.type==Plan.TOOL) return false;

      // Image sans astrom�trie non encore pris comme r�f�rence
      if( !Projection.isOk(currentPlan.projd) && !currentPlan.hasXYorig && !currentPlan.ref ) return x<frMax;

      // Un double clic sur le logo, sur ca marche
//      if( x>gapL && x<gapL+DX && e.getClickCount()>1 && e.getWhen()-lastClick<250) return true;

      // Un plan d�j� pris en r�f�rence visible ne pourra �tre repris en r�f�rence
      if( currentPlan.isRefForVisibleView() ) return false;

      // Sur le logo une image visible dans une vue ne pourra �tre pris en r�f�rence
      // => Simple activation
      if( x>gapL && (currentPlan.isImage() || currentPlan instanceof PlanBG)
            && (currentPlan.isViewable() && currentPlan.getOpacityLevel()>0 || a.calque.isBackGround())) return false;

      // Pour un plan Background */
      if( x<gapL && currentPlan.type==Plan.ALLSKYIMG ) return true;

      // Cas d'un plan catalogue qui ne peut �tre projet� sur une image
//      if( !currentPlan.isImage() && !currentPlan.isViewable() ) return x<frMax;
      if( currentPlan.isCatalog() && !currentPlan.isViewable() ) return x<frMax;

      // Cas d'un plan catalogue o� l'on force la projection
      if( !currentPlan.isImage() ) return x<frMin && shiftDown;

      // L'utilisateur veut s�lectionner plusieurs plans
      if( shiftDown && x>frMin ) return false;

      return x<frMax;
   }

  /** Deplacement (mode drag) de la souris.
   * Uniquement utilise pour le deplacement d'un plan
   * par rapport a un autre
   */
   public void mouseDragged(MouseEvent e) {
      if( a.inHelp ) return;

      // Peut etre un d�but de MegaDrag
      if( !a.view.isMegaDrag() ) a.view.startMegaDrag(currentPlan);

      int x = e.getX();
      int y = e.getY();

      // Pour �viter un changement intempestif de plan de r�f�rence par la suite
      newRef=null;

      // Pour ne pas s'emmeler avec les references
      if( /*x<gapL || */ currentPlan==null ) return;

      int DRAGLIMIT=1;

      // Gestion du d�placement soit vertical pour la permutation de plans
      // soit horizontal pour l'opacit� d'un plan image.
      Slide cSlide = getSlide(oldy);
      int dx = Math.abs(x-oldx);
      int dy = Math.abs(y-oldy);
      if( cSlide!=null && (flagDrag>0 || dx>DRAGLIMIT || dy>DRAGLIMIT) ) {
         // Pour changer de sens il faut que la diff�rence soit plus grande
         // et dans le cas initial o� l'on serait horizontal et qu'on veut passer en vertical
         // il faut que dy soit sup� � 10 pixels (sinon probl�me lorsqu'on remet � z�ro
         // le slider de transparence)
         if( x>cSlide.x1-8 && dx>dy && a.calque.canBeTransparent(cSlide.p) ) flagDrag=HORIZONTAL;
         else {
            if( flagDrag==HORIZONTAL && dy<10 ) flagDrag=HORIZONTAL;
            else flagDrag=VERTICAL;
         }

         if( flagDrag==VERTICAL ) {
//System.out.println("D�placement verticale "+dy);
            if( oldSlide!=null  ) {
//System.out.println("Restitution opacity du plan "+oldSlide.p.label);
               oldSlide.p.setOpacityLevel(oldTransp);

               if( oldSlide.p!=null ) Properties.majProp(oldSlide.p);

               oldSlide=null;
               a.view.repaintAll();
            }
            this.x=x; this.y=y;
            repaint();

         } else if( flagDrag==HORIZONTAL ) {
//System.out.println("D�placement horizontal "+dx);
            if( oldSlide==null ) {
               oldSlide=cSlide;
               oldTransp = oldSlide.p.getOpacityLevel();
//System.out.println("S�lection opacity "+oldTransp+" du plan "+oldSlide.p.label);
            }
            float t = (x-oldSlide.x1)/(float)(oldSlide.x2-oldSlide.x1);

            // Changement de transparence pour le plan sous la souris s'il n'est pas s�lectionn�
            if( !oldSlide.p.selected  ) setOpacityLevel( oldSlide.p, t);

            // sinon changement de transparence pour tous les plans s�lectionn�s
            else setOpacityLevel(t);

//System.out.println("Positionnement opacity "+t+" du plan "+oldSlide.p.label);
            a.view.repaintAll();
            repaint();
            if( oldSlide.p!=null ) Properties.majProp(oldSlide.p);

         }
      }
   }

   // Changement de la transparence pour tous les plans s�lectionn�s
   private void setOpacityLevel(float t) {
      Plan p[] = a.calque.getPlans();
      for( int i=0; i<p.length; i++ ) {
         if( p[i].selected && a.calque.canBeTransparent(p[i])) setOpacityLevel( p[i], t);
      }
   }

   /** Indique que ViewSimple peut dessiner les bords des autres images
    * en superposition car la souris est sur le label et non sur le logo
    * et que l'utilisateur n'est pas entrain de modifier la transparence
    * d'une image */
   //   protected boolean canDrawFoVImg() { return x>frMax && oldSlide==null; }
   protected boolean canDrawFoVImg() { return x>gapL  && oldSlide==null;  }

   private Slide oldSlide=null;
   private float oldTransp;

   // Creation d'un folder qui va contenir tous les plans qui sont dans
  // menuPlan
   protected void insertFolder() {
      Enumeration e = a.calque.getSelectedPlanes().elements();
      Plan op = (Plan)e.nextElement();
      Plan p = createFolder(op);
      while( true ) {
         a.calque.permute(op,p);
         p=op;
         if( !e.hasMoreElements() ) return;
         op=(Plan)e.nextElement();
      }
   }

  /** Creation d'un plan Folder juste au-dessus du plan p dans la pile */
   protected Plan createFolder(Plan p) {

      Plan fp = a.calque.createFolder(p.objet,p.folder,false);
      a.calque.permute(fp,p);
      return fp;

//      int i;
//      for( i=0; i<a.calque.plan.length && a.calque.plan[i]!=p; i++);
//      int n;
//
//      n=a.calque.newFolder(p.objet,p.folder,false);
//      Plan fp = a.calque.plan[n];
//      permute(n,i);
//      return fp;
   }

  /** Mise a jour ou creation d'un folder avec les plans p1 et p2
   * p2 peut etre doit etre du type folder
   */
   protected void folder(Plan p1, Plan p2) {
      int i;

      if( p2.type!=Plan.FOLDER ) return;
      for(i=0; i<a.calque.plan.length&& a.calque.plan[i]!=p2; i++ );
      if( i>=a.calque.plan.length-1 ) return;
      p2=a.calque.plan[i+1];
      a.calque.permute(p1,p2);
   }


   // Gestion des curseurs
   private int oc=Aladin.DEFAULT;
   private void handCursor()    { makeCursor(Aladin.HANDCURSOR); }
   private void waitCursor()    { makeCursor(Aladin.WAITCURSOR); }
   private void moveCursor()    { makeCursor(Aladin.MOVECURSOR); }
   private void defaultCursor() { makeCursor(Aladin.DEFAULT); }
   private void makeCursor(int c) {
      if( oc==c ) return;
      Aladin.makeCursor(this,c);
      oc=c;
   }


   /** Tente de montrer/cacher le plan passer en param�tre (par exemple
    * lorsque l'utilisateur clique sur le logo */
   protected boolean switchShow(Plan p,boolean inCheckBox) {
      if( p.type==Plan.FILTER ) {
         p.setActivated(!p.active);
         ((PlanFilter)p).updateState();
      } else {
         if( !planOk(p) ) return false;
         if(  !inCheckBox || !a.view.tryToShow(p) )  {
            
            boolean activeBefore = p.active;
            boolean isRefForVisibleView = p.isRefForVisibleView();
         
            if( a.calque.isBackGround() && !p.isViewable() && !(p instanceof PlanBG) && a.view.syncPlan(p) ) {
//               System.out.println("switchShow: Je synchronise sur l'image et je l'active");
               boolean ok = p.setActivated(true);
               if( !activeBefore && !ok ) {
                  setCheckBoxBlinkPlan(p);
//                  System.out.println("Impossible !");
               } else if( ok && p.getOpacityLevel()<0.1f ) p.setOpacityLevel(1f);
            } else {
               if( p.getOpacityLevel()<0.1f && p.active && !isRefForVisibleView ) {
                  if( a.calque.isBackGround() && p.type==Plan.ALLSKYIMG ) {
//                     System.out.println("switchShow: d�j� activ� mais transparence max => on indique que le slider est une meilleur id�e");
                     p.startCheckBoxBlink();
                     a.calque.unSelectAllPlan();
                     p.selected=true;
                     setLastMessage(WARNINGSLIDER);
                     return true;
                  }
//                  System.out.println("switchShow: d�j� activ� mais transparence max => je rends opaque");
                  p.setOpacityLevel(1f);
               } else {
//                  System.out.println("switchShow: j'inverse l'activation "+p.active+" => "+!p.active);
                  boolean ok = p.setActivated(!p.active);
                  if( p.active && p.getOpacityLevel()<0.1f && !isRefForVisibleView ) p.setOpacityLevel(1f);
                  if( !activeBefore && !ok ) {
                     setCheckBoxBlinkPlan(p);
//                     System.out.println("Impossible !");
                  }
               }
            }
         
//            p.setActivated(!p.active);
            if( !p.active ) a.console.setCommand("hide "+Tok.quote(p.label));
            else a.console.setCommand("show "+Tok.quote(p.label));
         }

         // thomas
         if( p.isCatalog() && p.active)  PlanFilter.updatePlan(p);
      }
      return true;
   }
   
   // Positionne toutes les checkboxes en mode blink des plans qui peuvent �tre utilis�es pour afficher
   // en overlay le plan pass� en param�tre, lui compris (=> changement de ref)
   private void setCheckBoxBlinkPlan(Plan p1) {
      int n = a.calque.getIndex(p1);
      Plan [] p = a.calque.getPlans();
      for( int i=n; i<p.length; i++ ) {
         if( !p[i].isReady() ) continue;
         if( p[i] instanceof PlanBG || p1.isCompatibleWith(p[i]) ) p[i].startCheckBoxBlink();
      }
      setLastMessage(WARNING);
   }
   
   
   private String lastMessage="";
   protected void setLastMessage(String s) { lastMessage=s; }
   protected String getLastMessage() {
      return lastMessage;
   }
   
   // Postionne le plan courant et met le rep�re en son centre si hors champs
   // => sinon probl�me par la suite en cas de zoom via le slider
   private boolean setPlanRef(Plan p) {
      boolean rep = a.calque.setPlanRef(p);
      if( !p.contains(new Coord(a.view.repere.raj,a.view.repere.dej)) ) {
         a.view.setRepere(p);
      }
      return rep;
   }

  /** Gestion de la souris */
   public void mouseReleased(MouseEvent e) {
      Plan newPlan;
      this.x=e.getX(); this.y=e.getY();

      if( a.inHelp ) { a.helpOff(); return; }

      // Peut etre la fin d'un megaDrag
      if( a.view.stopMegaDrag(e.getSource(),x,y,e.isControlDown()) ) {
         flagDrag=0;
         return;
      }

      // Peut �tre la fin d'un changement de transparence
      if( oldSlide!=null ) { oldSlide=null; flagDrag=0; return; }

      // Memorise l'etat du Shift ou du Control
      boolean flagCtrl = Aladin.macPlateform?e.isMetaDown():e.isControlDown();
      boolean flagShift = e.isShiftDown();

      boolean boutonDroit = e.isPopupTrigger() || memoBoutonDroit;
      memoBoutonDroit = false;
      if( boutonDroit ) {
         flagDrag=0;
         if( x<=frMax ) x=frMax+10;  // Pour �viter de switcher le logo
      }

      //Apres un deplacement de plan (par drag)
      if( flagDrag==VERTICAL ) {
         flagDrag=0;
         Slide s = getSousSlide(y);
         
         if( s!=null ) {
            newPlan = s.getPlan();
//       System.out.println("Je suis SOUS le slide du plan "+(newPlan!=null?newPlan.label:"null"));
         }
         
         // On est sans doute au-dessus de la pile
         else if( x>0 && x<getWidth() && y>0 && y<oldy ) {
            int n;
            for( n=0; a.calque.plan[n].type==Plan.NO; n++);
            if( n>0 ) n--;
            newPlan = a.calque.plan[n];
//          System.out.println("Je suis sur le dessus de la pile");
         } else return;

         //Permutation des plans
         if( currentPlan!=newPlan /* && Math.abs(oldy-y)>=4 */ ) {
            //System.out.println("currentPlan : "+(currentPlan!=null?currentPlan.label:"null"));
            //System.out.println("newPlan : "+(newPlan!=null?newPlan.label:"null"));
            a.calque.permute(currentPlan,newPlan);
            a.view.repaintAll();
            a.calque.zoom.zoomView.repaint();
         }
         repaint();
         return;
      }

      // Determination du plan et du slide clique
      Slide s = getSlide(y);
      if( s==null ) return;
      Plan p = s.getPlan();

      // Recherche du plan clique
      currentPlan = p;

      Plan [] allPlan = a.calque.getPlans();
      
      boolean itsDone=false;

      if( !canBeNewRef(e,x,p) 
            ||  (!a.view.isMultiView() && p.ref)  ) newRef=null;
      else newRef = p;

      if( !itsDone && p.type!=Plan.NO ) {
         if( newRef!=null ) {
//            boolean recenter= a.calque.isBackGround() && p instanceof PlanBG;
            boolean recenter= p instanceof PlanBG;
            if( recenter && a.calque.setPlanRefOnSameTarget((PlanBG)p) 
            || !recenter && setPlanRef(p) ) {
               a.view.newView();
               a.console.setCommand("cview "+Tok.quote(p.label));
            }
            newRef=null;

            // activation / desactivation du plan clique
         } else if( /* s.inLogo(x) */ s.inLogoCheck(x) ) {        // Dans les checkboxes
            a.calque.resetClinDoeil();	// Au cas o� on venait de faire un clin d'oeil
            if( p.type==Plan.FOLDER ) {
               if( e.getClickCount()>1 && s.inLogo(x) ) switchCollapseFolder(p);
               switchActiveFolder(p);         // Le double clic est tjrs pr�c�d� d'un simple clic
            } else {
               if( !switchShow(p, s.inCheck(x) ) ) {
                  System.out.println("switchShow returns false");
                  return;
               }
            }
            a.calque.repaintAll();
            return;
         }

         if( e.getClickCount()>1 && p.type!=Plan.ALLSKYCAT /* && a.calque.getPlanBase().type==Plan.IMAGEBKGD */ ) {
            a.view.syncPlan(p);
         }

         // Selection / deselection  du plan clique
         int nbc = 0;

         planOk(p);

         // Cas particulier d'un plan d'une image d'archive
         // on va automatiquement selectionner la source associee
         if( p.type==Plan.IMAGE && ((PlanImage)p).o!=null ) {
            Source src =(Source)((PlanImage)p).o;
            if( a.mesure.findSrc(src)==-1 ) a.view.setSelected(src,true);
            a.mesure.mcanvas.show(src,2);
         }

         // Si on active le popup menu
         if( boutonDroit ) {
            for( int i=0; i<allPlan.length; i++ ) {
               if( allPlan[i].selected && !allPlan[i].collapse ) nbc++;
            }

            if( nbc==1 && p.type!=Plan.NO ) {
               for( int i=0; i<allPlan.length; i++ ) allPlan[i].selected=false;
               p.selected=true;
            }

         } else {

            // S�lection de plans contigus
            if( flagShift ) {
               int first=0,last=a.calque.getIndex(allPlan,currentPlan);
               for( int i=0; i<allPlan.length; i++ ) {
                  if( allPlan[i].selected ) { first=i; break; }
               }
               if( first>last ) { int j=first; first=last; last=j; }
               for( int i=first; i<=last; i++ ) allPlan[i].selected=true;

               // S�lection/d�selection de plans discontigus
            } else {

               // On deselectionne le precedent plan courant
               // et les groupes de plans si necessaire
               for( int j=0; j<allPlan.length; j++ ) {
                  if( allPlan[j].selected ) {
                     nbc++;
                     if( allPlan[j].type==Plan.NO ) {
                        allPlan[j].setActivated(false);
                        if( flagCtrl ) allPlan[j].selected = false;
                     }
                     if( !flagCtrl ) allPlan[j].selected = false;
                  }
               }

               if( flagCtrl ) {
                  if( nbc>1 && p.selected ) p.selected=false;
                  else p.selected=true;
               } else p.selected = true;
            }
         }

          if( newRef!=null ) {
            if( a.calque.setPlanRef(p) ) {
               a.view.newView();
               a.console.setCommand("cview "+Tok.quote(p.label));
            }
            newRef=null;
         }

         // Selections des vues correspondantes aux plans s�lectionn�s
         if( p.isImage() ) a.view.selectViewFromStack(p);
         a.view.setSelectFromView(false);

         // S�lection de tous les objets du plan par double-clic
         if( x>gapL && !boutonDroit && e.getClickCount()==2 && (p.isCatalog() ||
               p.type==Plan.TOOL && !(p instanceof PlanContour) ) && p.active ) {
            a.view.calque.selectAllObjectInPlans();

            // On repasse en mode SELECT si n�cessaire
            int i=a.toolBox.getTool();
            if( i!=ToolBox.SELECT ) {
               a.toolBox.setMode(i,Tool.UP);
               a.toolBox.setMode(ToolBox.SELECT, Tool.DOWN);
            }
         }
      }

      a.calque.repaintAll();
      if( boutonDroit ) showPopMenu(x, y);
   }

   // Collapse ou uncollapse de tous les plans d'un folder
   protected void switchCollapseFolder(Plan f) {
      int i,n,m;
      boolean flag;
      Plan p[] = a.calque.getFolderPlan(f);

      if( p==null || p.length==0 ) return;

      // Determine le nombre de plans collapses de meme niveau
      for( m=n=i=0; i<p.length; i++) {
          if( p[i].folder==f.folder+1 ) {
             m++;
             if( p[i].collapse ) n++;
          }
       }

      // Activation ou desactivation ?
      if( n==m ) flag=false;
      else flag=true;

      // on active/desactive les plans du folder
      for( i=0; i<p.length; i++ ) {
         if( !flag ) {
             if( p[i].folder==f.folder+1 ) p[i].collapse=p[i].selected=false;
         } else  {
            p[i].collapse=true;
         }
      }

   }

   // Activation ou desactivation de tous les plans d'un folder
   private void switchActiveFolder(Plan f) {
      int i,n;
      boolean flag = !f.active;
      Plan p[] = a.calque.getFolderPlan(f);

      if( p==null  ) return;

//      // Determine le nombre de plans actifs
//      for( n=i=0; i<p.length; i++) if( p[i].active ) n++;
//
//      // Activation ou desactivation ?
//      if( p.length==0 ) flag = !f.active;
//      else if( n==p.length ) flag=false;
//      else flag=true;

      // on active/desactive les plans du folder
      for( i=0; i<p.length; i++ ) p[i].setActivated(flag);

      f.setActivated(flag);
   }

   /** Selection d'un plan particulier */
   protected void selectPlan(int i) {
      for( int j=0; j<a.calque.plan.length; j++ ) a.calque.plan[j].selected=false;
      a.calque.plan[i].selected=true;

   }

   /** Indication du plan sous la souris et
    *  reaffichage des bordures des vues si n�cessaire */
   private Plan lastPlanUnderMouse=null;
   protected void underMouse(Plan p) {
      if( lastPlanUnderMouse==p || a.menuActivated() ) return;
      boolean flagTrans = a.calque.canBeTransparent(p);
      a.calque.selectPlanUnderMouse(p);
      if( lastPlanUnderMouse!=null && lastPlanUnderMouse.isImage()
            /* || flagTrans */ ) a.view.repaintAll();
      else a.view.paintBordure();
      if( /*!flagTrans || */ canDrawFoVImg() ) lastPlanUnderMouse=p;
      else lastPlanUnderMouse=null;
   }


//   private Slide oSlide=null;

  /** Gestion de la souris */
   public void mouseMoved(MouseEvent e) {
      if( a.inHelp ) return;

      int x = e.getX();
      int y = e.getY();
      
      Slide s = getSlide(y);
      Plan p = s==null?null:s.getPlan();
      
      // Specification du plan sous la souris
      underMouse(p);

      planIn=null;

//      if( inEye(x,y) )  {
//         mouseIn=a.calque.getNbUsedPlans()>0; // implique l'affichage en vert
//         handCursor();
//         Util.toolTip(this,HEYE);
//      } else mouseIn=false;

      // Necessaire pour afficher les surlignages vert
      this.x=x;
      this.y=y;

//      // Pour �viter de tout redessinner
//      Graphics g = getGraphics();
//      if( oSlide!=null && oSlide!=s ) oSlide.redraw(g, x, y);
//      if( s!=null ) s.redraw(g,x,y);
//      else repaint();
//      oSlide=s;
      repaint();

      // Gestion des helps
      if( s!=null && s.inLogoCheck(x) && s.p!=null ) {
         handCursor();
         if( s.inCheck(x) && s.p.hasCheckBox() && !p.ref ) { Util.toolTip(this,Util.fold(HSTACK0,25,true)); return; }
         else if( s.inLogo(x) ) { Util.toolTip(this,Util.fold(HSTACK,25,true)); return; }
      }
      Util.toolTip(this,p==null ? null : Util.fold(p.getInfo(),30,true));

      // Infos sur les plans
      if( p==null ) { defaultCursor(); return; }

      // Cas particulier d'un plan d'une image d'archive
      // on va automatiquement montrer la source associee si elle est dans les mesures
      if( p.type==Plan.IMAGE && ((PlanImage)p).o!=null ) {
         Source src =(Source)((PlanImage)p).o;
         if( a.mesure.findSrc(src)>-1 ) a.mesure.mcanvas.show((Source)((PlanImage)p).o,1);
      }

      if( s!=null && s.inCheck(x) ) {
         if( p.ref || p.isImage() ) {
            handCursor();
            return;
         } else { defaultCursor(); return; }
      }
      planIn=p;
      if( p.type!=Plan.NO ) {
         if( !p.flagOk &&p.error==null ) waitCursor();
         else handCursor();
         setInfo(p);
      } else { defaultCursor(); a.status.setText(""); }
   }

   /** Affichage des infos (tooltip + status) concernant le plan */
   private void setInfo(Plan p) {
      String s = p.getInfo();
      a.status.setText(s);
//      Util.toolTip(this,s);
   }

  /** Gestion de la souris */
   public void mouseExited(MouseEvent e) {
      a.calque.unSelectUnderMouse();
      a.view.resetBorder();
      defaultCursor();
      planIn=null;

      // Peut etre s'agit-il d'un MegaDrag ?
      if( a.view.isMegaDrag() ) { flagDrag=0;
          Aladin.makeCursor(a, Aladin.PLANCURSOR);
      }

      // Effacement des surcharges vertes eventuelles
//      mouseIn=false;
      x=y=-1;
      a.view.repaintAll();
      repaint();
   }
   
  /** Retourne le slide juste au-dessus en fonction de l'ordonnee y
   * @param y Ordonnee de la souris
   * @return le slide concerne, null si aucun
   */
   protected Slide getSousSlide(int y) {
      if( slides==null ) return null;
      Enumeration e = slides.elements();
      while( e.hasMoreElements() ) {
         Slide s = (Slide)e.nextElement();
         if( s.sous(y) ) return s;
      }
      return null;
   }

  /** Retourne le slide en fonction de l'ordonnee y
   * @param y Ordonnee de la souris
   * @return le slide concerne, null si aucun
   */
   protected Slide getSlide(int y) {
      if( slides==null ) return null;
      Enumeration e = slides.elements();
      while( e.hasMoreElements() ) {
         Slide s = (Slide)e.nextElement();
         if( s.in(y) ) return s;
      }
      return null;
   }

  /** Retourne le plan en fonction de l'ordonnee y
   * @param y Ordonnee de la souris
   * @return le slide concerne, null si aucun
   */
   protected Plan getPlan(int y) {
      Slide s = getSlide(y);
      return s==null?null:s.getPlan();
   }   
 
   boolean beginnerHelp=true;
   
   protected void setBeginnerHelp(boolean flag) { beginnerHelp=flag; }
   
   int lastBegin=-1;
   int lastYMax;  // Derni�re ordonn�e mesur�e de la fin de la pile
   
   /** Affichage d'un message au-dessus de la pile des plans 
    * => arr�te automatique les messages pour les d�butants */
   protected void drawMessage(Graphics g,String s,Color c) {
      setBeginnerHelp(false);
      drawBeginnerHelp1(g,s,c,lastYMax);
   }

   /** Affiche un message pour les d�butants en fonction du nombre de plans en cours d'utilisation */
   private void drawBeginnerHelp(Graphics g,int nbVisiblePlan,int yMax) {
      if( BEGIN==null ) {
         BEGIN = new String[6];
         for( int i=1; i<BEGIN.length; i++ ) BEGIN[i] = a.chaine.getString("BEGIN"+i); 
      }
      int begin = nbVisiblePlan+1;
      
      // Pour n'afficher qu'une fois le m�me message
      if( lastBegin!=-1 && lastBegin!=begin ) BEGIN[lastBegin]=null;
      String s = BEGIN[begin];
      lastBegin=begin;
      if( s==null ) return;
      int y= drawBeginnerHelp1(g,s,Aladin.MYBLUE,yMax);
//      drawControlHelp(g,y);
   }
   
//   private void drawControlHelp(Graphics g,int y) {
//      drawCross(g,Color.red.darker(),145,20);
//      drawArrow(g,Color.lightGray,130,y-10,-1);
//      drawArrow(g,Color.gray,142,y-10,1);
//   }
//   
//   static private final int X = 6;
//   private void drawCross(Graphics g, Color c, int x, int y) {
//    g.setColor(c);
//      g.setColor( Color.red.darker() );
//      g.drawLine(x,y,x+X,y+X);
//      g.drawLine(x+1,y,x+X+1,y+X);
//      g.drawLine(x+2,y,x+X+2,y+X);
//      g.drawLine(x+X,y,x,y+X);
//      g.drawLine(x+X+1,y,x+1,y+X);
//      g.drawLine(x+X+2,y,x+2,y+X);
////      cross = new Rectangle(x,y,X,X);
//   }
//   
//   private void drawArrow(Graphics g, Color c, int x, int y,int sens) {
//      int h=4;
//      int w=8;
//      g.setColor(c);
//      g.fillRect(x, y, w, h);
//      if( sens==1 ) {
//         g.drawLine(x+w-3,y-2,x+w-3,y+h+2);
//         g.drawLine(x+w-2,y-1,x+w-2,y+h+1);
//         g.drawLine(x+w,y+h/2,x+w,y+h/2);
//      } else {
//         g.drawLine(x+2,y-2,x+2,y+h+2);
//         g.drawLine(x+1,y-1,x+1,y+h+1);
//         g.drawLine(x-1,y+h/2,x-1,y+h/2);
//      }
//   }
                                             
      
   private int drawBeginnerHelp1(Graphics g,String s,Color c,int yMax) {
      int xMax=getWidth();
      g.setColor(c);
      g.setFont(Aladin.BOLD);
      FontMetrics fm = g.getFontMetrics();
      int h=fm.getHeight();
      boolean first=true;
      StringBuffer line = new StringBuffer();
      int w=0;
      StringTokenizer st = new StringTokenizer(s,"\n");
      int y,y0 = 30;
      for( y=y0 ; y+3*h<yMax && st.hasMoreTokens(); y+=h ) {
         StringTokenizer st1 = new StringTokenizer(st.nextToken()," ");
         for( ; y<yMax && st1.hasMoreTokens(); ) {
            String s1 = st1.nextToken().trim();
            int w1 = fm.stringWidth(" "+s1);
            if( w1+w>xMax ) {
               drawString(g,line.toString(),5,y);
               y+=h;
               line = new StringBuffer(s1);
               w=0;
               if( first ) { first=false; g.setFont(Aladin.PLAIN); }
            } else line.append( (line.length()>0 ? " ":"")+s1);
            w+=w1;
         }
         if( y<yMax && line.length()>0 ) {
            drawString(g,line.toString(),5,y);
            line = new StringBuffer();
            w=0;
         }
         if( first ) { first=false; g.setFont(Aladin.PLAIN); }
      }
      
      // Bordure en marge gauche si on a �crit au-moins une ligne
      if( !first ) {
         g.setColor( Color.lightGray );
         g.drawLine(2,y0-10,2,y);
      }
      
      return y;
   }
   
   private void drawString(Graphics g,String s,int x, int y) {
      if( s.length()==0 ) return;
      if( s.charAt(0)=='*' ) { Util.drawCircle5(g, x+2, y-4); g.drawString(s.substring(1),x+7,y); }
      else g.drawString(s,x,y);
   }
   
//   long timeTips=0L;
//   String tip=null;
//   void drawTipHelp(Graphics g) {
//      long t = System.currentTimeMillis();
//      if( t-timeTips>5000 ) {
//         tip = ((Tips)a.urlStatus).getNextTips();
//         timeTips=t;
//      }
//      drawBeginnerHelp(g,tip);
//   }

//   // Dessin de l'oeil
//   void drawEye(Graphics g,boolean open) {
//      if( Aladin.NEWLOOK_V7 ) return;
////      Color c = eyeInGreen?Aladin.GREEN:Color.black;
//
//      Color c = a.calque.isFree() ? Color.gray : mouseIn ? Aladin.MYBLUE : Color.black;
//      if( firstEye ) {
//         int i;
//         int gap=gapL-2;
//         for( i=0; i<o1x.length; i++ ) o1x[i]+=gap;
//         for( i=0; i<o2x.length; i++ ) o2x[i]+=gap;
//         for( i=0; i<o3x.length; i++ ) o3x[i]+=gap;
//         o4xc+=gap;
//         firstEye=false;
//      }
//      g.setColor(c);
//      g.fillPolygon(o1x,o1y,o1x.length);
//      g.fillPolygon(o2x,o2y,o2x.length);
//      if( open ) {
//         g.setColor( a.calque.isFree() ? Aladin.LBLUE : !Aladin.NETWORK ? Color.red : Color.white );
//         g.fillPolygon(o3x,o3y,o3x.length);
//         g.setColor( c);
//         g.fillOval(o4xc,o4yc,o4d,o4d);
//         g.setColor( Color.white );
//         int x = o4xc+o4d/2+1;
//         int y = o4yc+o4d/2+1;
//         g.drawLine(x+1,y-1,x+2,y-1);
//         g.drawLine(x+2,y-2,x+2,y-2);
////         g.fillOval(o4xc+(2*o4d)/3,o4yc+o4d/3,2,3);
//      }
//   }

//   static final int eyeO [][]
//       = { {0, 13,23 },
//           {1, 9,26},
//           {2, 7,28},
//           {3, 5,11}, {3, 16,21}, {3,27,30},
//           {4, 3,8}, {4, 17,22}, {4,29,32},
//           {5, 2,6}, {5, 18,23}, {5,31,33},
//           {6, 1,4}, {6, 13,14}, {6, 18,23}, {6, 32,35},
//           {7, 0,3}, {7, 13,23}, {7, 33,35},
//           {8, 1,2}, {8, 13,23}, {8, 33,34},
//           {9, 2,2}, {9, 13,23}, {9, 33,33},
//           {10, 3,4}, {10, 14,22}, {10,31,32},
//           {11, 5,6}, {11, 15,21}, {11,29,30},
//           {12, 7,8}, {12, 16,20}, {12,27,28},
//           {13, 9,11}, {13, 24,26},
//           {14, 12,23}
//   };
//
//   static final int eyeC [][]
//                            = { {0, 13,23 },
//                                {1, 9,26},
//                                {2, 7,28},
//                                {3, 5,30},
//                                {4, 3,32},
//                                {5, 2,33},
//                                {6, 1,35},
//                                {7, 0,35},
//                                {8, 1,34},
//                                {9, 2,33},
//                                {10, 3,32},
//                                {11, 5,30},
//                                {12, 7,28},
//                                {13, 9,26},
//                                {14, 12,23}
//                        };
//   // Dessin de l'oeil
//   void drawEye(Graphics g,boolean open) {
//      int x=gapL-2, y=10;
//      int [][] e ;
//
//      if( !this.a.calque.isFree() ) {
//         g.setColor(!Aladin.NETWORK ? Color.red : Color.white);
//         e = eyeC;
//         for( int i=0; i<e.length; i++ ) {
//            int [] a = e[i];
//            int y1 = y+a[0];
//            g.drawLine(x+a[1],y1,x+a[2],y1);
//         }
//      }
//
//      g.setColor(this.a.calque.isFree() ? Color.gray : mouseIn ? Aladin.MYBLUE : Color.black);
//      e = open ? eyeO : eyeC;
//      for( int i=0; i<e.length; i++ ) {
//         int [] a = e[i];
//         int y1 = y+a[0];
//         g.drawLine(x+a[1],y1,x+a[2],y1);
//      }
//   }


   // Dessin du logo entrain d'etre deplace
   void moveLogo(Graphics g) {
      Slide cSlide=getSlide(oldy);
      Slide s = new Slide(a,cSlide.p);
      s.mode=cSlide.mode;
      if( x>=0 ) s.dragDraw(g,x,y);
      }

//   // Ecrit le nom de l'objet central du plan de reference
//   // a cote de l'oeil
//   void writeTitle(Graphics g) {
//      if( Aladin.NEWLOOK_V7 ) return;
//      Plan p =a.calque.getPlanRef();
//      if( p==null || p.objet==null || p.flagOk==false ) return;
//      g.setFont( Aladin.LBOLD );
//      g.setColor( Color.black );
//      FontMetrics m = g.getFontMetrics(Aladin.LBOLD);
//      int largeur = ws-(gapL+eyeWidth);
//      int stext = m.stringWidth(p.objet);
//      if( stext>largeur ) {
//         g.setFont( Aladin.ITALIC );
//         m = g.getFontMetrics(Aladin.BOLD);
//         stext = m.stringWidth(p.objet);
//      }
//      if( stext>sizeLabel) return;
//      int xo = (gapL+eyeWidth+ws)/2-stext/2;
//      g.drawString( p.objet, xo , eyeHeight-5);
//   }

//   protected int getFirstVisible() {
//      int j;
//      int y=hs-22;
//      for( j = a.calque.scroll.getLastVisiblePlan(); j>=0 && y>eyeHeight; j-- ) {
//         if( a.calque.plan[j].collapse ) continue;
//         y -= Slide.DY;
//      }
//      return(j+1);
//   }

   /** Si n�cessaire d�cale la scrollbar du select pour montrer le "slide"
    * correspondant au plan de base */
   protected void showSelectedPlan() {
      int n = a.calque.getFirstSelected();
//      int n = a.calque.getIndexPlanBase();
      int lastPlan = a.calque.scroll.getLastVisiblePlan();
      int firstPlan = a.calque.scroll.getFirstVisiblePlan();
      int nb = a.calque.scroll.getNbVisiblePlan();
      if( lastPlan<0 || firstPlan<0 || n<0 ) return;
//      int nb = lastPlan-firstPlan;
      if( n<firstPlan ) a.calque.scroll.setValue(n+nb);
      else if( n>lastPlan ) a.calque.scroll.setValue(n);
   }

   private boolean firstUpdate=true;

   public void paintComponent(Graphics g) {
      
      if( a.calque.scrollAdjustement() ) {
         repaint();
         return;
      }
      
      // Pas tr�s joli
      if( a.calque.zoom.opacitySlider!=null ) {
         if( a.calque.zoom.sizeSlider!=null ) a.calque.zoom.sizeSlider.repaint();
         a.calque.zoom.opacitySlider.repaint();
         a.calque.zoom.zoomSlider.repaint();
      }
      
      // Positionnement du curseur apres le demarrage d'Aladin
      if( firstUpdate ) {
         Aladin.makeCursor(a,Aladin.DEFAULT);
         a.localisation.setInitialFocus();
         firstUpdate=false;
      }

      ws = getSize().width;
      hs = getSize().height;
      hsp= hs-/*eyeHeight-*/gapB;        // Hauteur de la portion pour les plans

      // On prepare le fond
      g.setColor( getBackground() );
      g.fillRect(0,0,ws,hs);

      // Le pourtour
//      Util.drawEdge(g,ws,hs);
      

      // Le clip Rect pour ne pas depasser
      g.clipRect(2,2,ws-3,hs-3);

      // AntiAliasing
      if( Aladin.ANTIALIAS ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
      }

      // Dessin de l'oeil de l'observateur et de l'objet central regarde
//      drawEye(g,true);
//      writeTitle(g);

      Plan [] plan = a.calque.getPlans();
      // Determination du premier plan image (opaque)
      int nOpaque = a.calque.getIndexPlanBase();

      /*anais*/
      //      int planRGB =(nOpaque>=0 && plan[nOpaque].type==Plan.IMAGERGB)?nOpaque:-1;
      Plan planRGB = (nOpaque>=0 && plan[nOpaque].type==Plan.IMAGERGB) ? a.calque.getPlan(nOpaque) : null;
      if( nOpaque<0 ) nOpaque=plan.length;

      setSlideBlink(false);

      if( slides==null ) slides = new Vector(plan.length+5);
      else slides.clear();

      ViewSimple v = a.view.getCurrentView();
      int y=hs-22;
      int j,n;
      int nbPlanVisible=0;
      for( n=0; n<plan.length && plan[n].type==Plan.NO; n++);
      for( j=a.calque.scroll.getLastVisiblePlan(); j>=n && y>0/*eyeHeight*/; j-- ) {
         if( plan[j].slide==null ) plan[j].slide=new Slide(a,plan[j]);
         Slide s = plan[j].slide;
         slides.addElement(s);
         int mode = newRef==plan[j] || (v!=null && plan[j]==v.pref)?Slide.NOIR:
            plan[j].ref?Slide.GRIS:Slide.VIDE;
         try {
            int y1=s.draw(g ,y,this.x,flagDrag==VERTICAL?-1:this.y,planRGB,mode);
            if( y1!=y ) nbPlanVisible++;   // Si on n'avance pas, c'est que le plan est "dans" un folder ferm�
            y=y1;
         } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      }
      a.calque.scroll.setFirstVisiblePlan(j+1);
      a.calque.scroll.setNbVisiblePlan(nbPlanVisible);
      a.calque.scroll.setRequired(y<0/*eyeHeight*/ || a.calque.scroll.getLastVisiblePlan()!=plan.length-1);
      
      // Dans le cas d'un deplacement de plan
      if( flagDrag==VERTICAL ) moveLogo(g);

      lastYMax = y;
      if( a.configuration.isHelp() && beginnerHelp && nbPlanVisible<=4 ) drawBeginnerHelp( g, nbPlanVisible, y);
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            // On met a jour la fenetre des proprietes en indiquant
            // s'il y a ou non des plans en train d'etre charge
            // afin d'eviter les clignotement de Properties
            // intempestifs
            Properties.majProp(slideBlink?1:0);

            // On met a jour la fenetre de la table des couleurs
            if( a.frameCM!=null ) a.frameCM.majCM();

            // On met a jour la fenetre des contours
            if( a.frameContour!=null ) a.frameContour.majContour();

            // On met a jour la fenetre des RGB et des Blinks
            if( a.frameRGB!=null )   a.frameRGB.maj();
            if( a.frameBlink!=null ) a.frameBlink.maj();
            if( a.frameArithm!=null && a.frameArithm.isVisible() ) a.frameArithm.maj();

            // Activation ou desactivation des boutons du menu principal
            // associes a la presence d'au moins un plan
            a.setButtonMode();

        }
      });


      // Reaffichage du status du plan sous la souris
      if( planIn!=null ) setInfo(planIn);

      // En cas de clin d'oeil
//      if( clinDoeil ) {
//         drawEye(g,false);
//         flagRClip = false;
//         startBlink();
//      }

      //Clignotement des voyants si besoin
      if( slideBlink ) startBlink();
   }

   private boolean slideBlink=false;

   /** Sp�cifie si au dernier retra�age de la pile au-moins un plan est en clignotement */
   protected void setSlideBlink(boolean flag) { slideBlink=flag; }

   private boolean flagThreadBlink=false;
   synchronized void setFlagThreadBlink(boolean a) { flagThreadBlink = a; }

  // Gestion du blinking d'une source par thread (pour supporter JVM 1.4)
   private void startBlink() {
      if( !slideBlink /*&& !clinDoeil*/ ) return;
      if( flagThreadBlink ) {
//System.out.println("blink thread already running ");
         return;
      }
      thread = new Thread(this,"AladinSelectBlink");
//System.out.println("launch blink thread "+thread);
      Util.decreasePriority(Thread.currentThread(), thread);
      setFlagThreadBlink(true);
      thread.start();
   }

/* Inutile car s'arret tout seul lorsque Slide.flagBlink passe a false
   private void stopBlinking() {
      setFlagBlink(false);
   }
*/
  // Gestion du Blinking 0.5 secondes
   public void run() {
      while( flagThreadBlink && (slideBlink /*|| clinDoeil*/) ) {
//System.out.println("blink thread (slideBlink="+slideBlink+") j'attends 0.5 sec "+thread);
         Util.pause(500);
         /* if( clinDoeil ) clinDoeil=false;
         else */ Slide.blinkState=!Slide.blinkState;
         repaint();
      }
      setFlagThreadBlink(false);
//System.out.println("fin du thread du blink "+thread);
      repaint();
   }

   /** Gestion du Help */
    protected String Help() { return a.chaine.getString("Select.HELP"); }

   // impl�mentation de WidgetFinder

   /** Cherche un plan par son nom
    *
    */
   public boolean findWidget(String name) {
       if( name.startsWith("ref") ) name = name.substring(3);
       return a.command.getNumber(name,1,false,false)!=null;
   }

   public Point getWidgetLocation(String name) {
       boolean ref = false;
       if( name.startsWith("ref") ) {
           ref = true;
           name = name.substring(3);
       }

       Plan plan = a.command.getNumber(name,1,false,false);
       int idx = plan==null ? 0 : a.calque.getIndex(plan);
       int oyShift = hs-(a.calque.plan.length-idx)*Slide.DY;
       // pour tenir compte de la scrollbar
       int yShift = oyShift+(a.calque.maxPlan-a.calque.scroll.getValue()-1)*Slide.DY;

       // dans ce cas, on va agir sur la scrollbar
       if( yShift<0/*eyeHeight*/ || yShift>hs ) {
          a.calque.scroll.setValue(idx);
          yShift = oyShift+(a.calque.maxPlan-idx-1)*Slide.DY;

          validate();
          repaint();
       }
       return new Point(ref?2:gapL+DX/2,yShift);
   }

   public void mouseClicked(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) {
      if( a.inHelp )  a.help.setText(Help());
      a.makeCursor(this, Aladin.DEFAULT);
   }

   public void adjustmentValueChanged(AdjustmentEvent e) {
      repaint();
   }

}

