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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Gestion de la Tool bar
 *
 * @author Pierre Fernique [CDS]
 * @version 1.7 : (29 oct 04) Code totalement revisité
 * @version 1.6 : (27 mar 02) Ajout du bouton Contour
 * @version 1.5 : (31 jan 02) Ajout du bouton RGB
 * @version 1.4 : (12 jan 01) Plus de confirmation pour la suppression de tous les plans
 * @version 1.3 : (15 dec 00) Gestion du curseur
 * @version 1.2 : (1 dec 00) changement du logo pour les dimensions de l'ecran
 * @version 1.1 : (28 mars 00) Retoilettage du code
 * @version 1.0 : (10 mai 99)  Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class ToolBox extends JComponent implements
MouseMotionListener,MouseListener,
SwingWidgetFinder, Widget {
   static int NUMBERTOOL=0;            // Numero croissant des tools plans (label par defaut)

   // Les differents outils possibles
   static final int SELECT= 0;
   static final int DRAW  = 1;
   static final int TAG   = 2;
   static final int PHOT  = 3;
   static final int DIST  = 4;
   static final int DEL   = 5;
   static final int WEN   = 6;
   static final int PROP  = 7;
   static final int PAN   = 8;
   public static final int HIST  = 9;
   static final int BNOTE = 10;
   static final int RGB   = 11;
   static final int CONTOUR=12;
   static final int FILTER= 13;
   static final int SYNC  = 14;
   static final int BLINK = 15;
   static final int ZOOM  = 16;
   static final int XMATCH= 17;
   static final int RESAMP= 18;
   static final int CROP  = 19;
   static final int PLOT  = 20;
   static final int SPECT = 21;
   static final int MOC   = 22;

   static int NBTOOL = 23;        // Nombre d'outils existants

   // Ordre d'apparition des boutons
   private int [] drawn = {SELECT,PAN,/*ZOOM,*/DIST,PHOT,DRAW,TAG,MOC,SPECT,
         FILTER,XMATCH,PLOT,RGB,BLINK,/* RESAMP,*/CROP,CONTOUR,HIST,PROP,
         DEL };

   // Ordre d'apparition des boutons
   static int [] OUTREACHDRAWN = {SELECT,PAN,/*ZOOM,*/DIST,PHOT,DRAW,TAG,
      CONTOUR,HIST,PROP,
      DEL };


   // liste des boutons lies au plan tool
   static int [] to = { CROP,SELECT,DRAW,TAG,PHOT,DIST,PAN,PROP,ZOOM,SPECT };

   // liste des boutons exclusifs
   static int [] exc = { CROP,SELECT,DRAW,TAG,PHOT,DIST,PAN,ZOOM,SPECT };

   // liste des boutons permettant la creation automatique d'un plan TOOL
   static int [] forTool = { DRAW,TAG,PHOT,DIST,SPECT };

   // liste des boutons toujours up (simple clic)
   static int [] up = { BNOTE,DEL,PROP,FILTER,PLOT };

   // Liste des tools non-autorisees en fonction du type de plan
   static int [] imgmode  = { /*DRAW,TAG,PHOT,DIST*//*, LABEL */ PLOT};   // pour Image
   static int [] imghugemode  = { /*DRAW,TAG,PHOT,DIST,*/RGB,BLINK,/*RESAMP,*/WEN,PLOT};   // pour Image huge
   static int [] contourmode = { HIST,DRAW,TAG,PHOT,DIST,CROP/* ,LABEL */,PLOT }; // pour un PlanContour
   static int [] toolmode = { HIST,CROP,PLOT };                       // pour Tool
   static int [] catmode  = { HIST,CROP /*,DRAW,TAG,PHOT,DIST*/ };         // pour Catalogue
   static int [] fieldmode= { HIST,CROP,PLOT /*,DRAW,TAG,PHOT,DIST,HIST*/ };   // pour Field


   // Les parametres generaux
   static int W        = 34;      // Largeur d'un bouton
   static int HMIN     = W-5;     // Hauteur minimale d'un bouton
   static int HREC     = W+2;       // Hauteur recommandee d'un bouton
   static int L        = 3;       // Demi-taille du carre de changt de prop.
   static int ICONEGAP = 12;      // Nombre de pixels reserves pour le changement de proportions

   // Les chaines
   String ICONEBAR,DRAWING;

   // References aux autres objets
   Aladin aladin;                 // La reference a l'ensemble
   Calque calque;                 // La reference a l'objet calque

   // Les parametres a memoriser
   int ws=W;                      // Derniere largeur de la boite a boutons
   int hs=600;                    // Derniere hauteur de la boite a boutons
   int nc;                        // nombre de colonnes de boutons
   int nb;                        // nombre de boutons par colonne
   int H;                         // Hauteur courante d'un bouton

   // Les composantes de l'objet
   public Tool [] tool;                  // Les outils de la boite

   // Les variables de travail
   boolean flagDelAll;            // Vrai si on doit effacer ts les plans apres confirmation

   /** Creation de la Tool bar
    * @param aladin Reference
    */
   protected ToolBox(Aladin aladin) {
      this.aladin = aladin;
      addMouseListener(this);
      addMouseMotionListener(this);
      ICONEBAR = aladin.chaine.getString("TBBAR");
      DRAWING = aladin.chaine.getString("TBDRAW");

      // Quels sont les boutons à afficher ?
      if( Aladin.OUTREACH ) drawn = OUTREACHDRAWN;

      calcConf(500-ICONEGAP);   //Calcul de la conf initiale

      init();

   }

   /** Recupere la reference au Calque et cree les outils
    * @param calque Reference
    */
   protected void init() {
      // Fabrication de chaque outil
      tool = new Tool[NBTOOL];
      for( int i=0; i<NBTOOL; i++ ) tool[i] = new Tool(i,aladin);
   }


   /** Retourne le numero du tool utilise
    * (uniquement parmi les outils to[])
    * @return le numero du tool en cours d'utilisation,
    *         <I>-1</I> sinon
    */
   protected int getTool() {
      for( int i=0; i<to.length; i++ ) {
         if( tool[ to[i] ].mode==Tool.DOWN ) return to[i];
      }
      return -1;
   }


   private boolean firstTag=true;
   private boolean firstRepere=true;


   /** Creation d'un nouveau objet en fonction du bouton appuye.
    * Retourne un nouvel objet si un des tools est un cours d'utilisation
    * @return Le nouvel objet a inserer dans le PlanTool, sinon <I>null</I>
    */
   protected Obj newTool(Plan plan, ViewSimple v, double x, double y) {
      int tool = getTool();
      switch(tool) {
         case DRAW: Ligne ligne = new Ligne(plan,v,x,y); ligne.bout=4; return ligne;
         case TAG:
            if( firstTag && aladin.configuration.isHelp() &&
                  aladin.configuration.showHelpIfOk("TAGINFO") ) {
               firstTag=false;
               return null;
            }
            return new Tag(plan,v,x,y);
         case PHOT:
            if( firstRepere && aladin.configuration.isHelp() && aladin.calque.getPlanBase().hasAvailablePixels() &&
                  aladin.configuration.showHelpIfOk("REPEREINFO") ) {
               firstRepere=false;
               return null;
            }
            SourceStat r = new SourceStat(plan,v,x,y,null);
            return r;
         case SPECT:
            RepereSpectrum rep = new RepereSpectrum(plan,v,x,y);
            return rep;
         case DIST: return new Cote(plan,v,x,y);
         default: return null;
      }
   }

   /** Position du bouton i dans le mode indiqué, et réaffichage
    *  de la toolbar
    */
   protected void setMode(int i, int mode) {
      if( tool[i].mode==mode ) return;
      tool[i].mode=mode;
      repaint();
   }

   /** Positionne un des boutons d'ajout de graphiques, et remonte tous les autres */
   protected void setGraphicButton(int n) {
      tool[DRAW].mode = tool[TAG].mode = tool[PHOT].mode
            = tool[DIST].mode = tool[SELECT].mode = tool[SPECT].mode =Tool.UP;
      tool[n].mode=Tool.DOWN;
      repaint();
   }

   /** Mise en place de l'etat des boutons.
    * Positionne les tools en fonction des vues sélectionnées
    */
   protected void toolMode() { toolMode(true); }
   protected void toolMode(boolean withRepaint) {
      Plan [] allPlan = aladin.calque.getPlans();
      int [] omode = new int[NBTOOL];  // etats precedents des tools
      int [] mode = new int[NBTOOL];   // prochains etats des tools
      int [] ex = {};                  // tmp
      boolean aucun = true;           // true si aucun plan valide
      int i,j;
      boolean dorepaint = false;
      int nbSimpleImg=0;
      int nbBlinkImg=0;
      int nbSimpleCat=0;
      int nbMoc=0;
      int nbCat=0;

      aladin.setMemory();   // Met à jour le niveau d'usage de la mémoire

      // Mémorisation des états précédents des boutons et initialisation des
      // nouveaux états
      for( i=0; i<omode.length; i++ ) {
         omode[i] = tool[i].mode;
         mode[i] = Tool.UNAVAIL;
         //         if( i==PAN ) mode[i]=Tool.UP;
      }
      // Parcours tous les plans courants actifs et pour chacun d'eux
      // supprime les tools qui ne peuvent lui etre associes
      for( i=0; i<allPlan.length; i++ ) {
         if( allPlan[i].type==Plan.NO || !allPlan[i].flagOk ) continue;
         if( allPlan[i].isPixel() || allPlan[i] instanceof PlanImageRGB )  nbSimpleImg++;
         if( allPlan[i] instanceof PlanImageBlink )  nbBlinkImg++;
         if( allPlan[i].type==Plan.CATALOG ) nbSimpleCat++;
         if( allPlan[i].isCatalog() ) nbCat++;
         if( allPlan[i].type==Plan.ALLSKYMOC ) nbMoc++;
         aucun=false;
         if( !allPlan[i].selected ) continue;

         switch(allPlan[i].type) {        // Quelle est la liste d'exclusion
            case Plan.IMAGERGB:
            case Plan.IMAGERSP:
            case Plan.IMAGEALGO:
            case Plan.IMAGECUBE:
            case Plan.IMAGECUBERGB:
            case Plan.IMAGEBLINK:
            case Plan.IMAGEMOSAIC:
            case Plan.ALLSKYIMG:
            case Plan.IMAGE:     ex = imgmode;     break;
            case Plan.IMAGEHUGE: ex = imghugemode; break;
            case Plan.ALLSKYCAT:
            case Plan.CATALOG:   ex = catmode;     break;
            case Plan.TOOL:      ex = toolmode;    break;
            case Plan.APERTURE:  ex = fieldmode;   break;
            case Plan.FOV:       ex = fieldmode;   break;
            case Plan.FILTER:    ex = contourmode; break;
         }

         // thomas
         if( allPlan[i] instanceof PlanContour) ex = contourmode;

         // Je positionne le complément du tableau ex[]
         for( j=0; j<mode.length; j++ ) {
            boolean flagEx=false;
            for( int k=0; k<ex.length; k++ ) { if( j==ex[k] ) { flagEx=true; break; } }
            if( !flagEx ) mode[j] = Tool.UP;
         }
      }

      // S'il n'y a pas de catalogues on invalide FILTER
      if( nbCat==0 ) mode[ToolBox.FILTER]=Tool.UNAVAIL;

      // S'il n'y a pas deux catalogues on invalide XMATCH
      if( nbSimpleCat<1 ) mode[ToolBox.XMATCH]=Tool.UNAVAIL;


      // S'il n'y a pas au-moins deux images on invalide RGB, BLINK, RESAMP
      if( nbSimpleImg<2 ) {
         mode[ToolBox.RGB]=/*mode[ToolBox.RESAMP]=*/mode[ToolBox.BLINK]=Tool.UNAVAIL;
      }

      // S'il n'y a pas au-moins un MOC on invalide
      if( nbMoc<1 ) {
         mode[ToolBox.MOC]=Tool.UNAVAIL;
      }

      ViewSimple v = aladin.view.getCurrentView();

      // Si la vue courante a un plan de référence qui n'a pas de pixels accessibles
      // on invalide CONTOUR
      //      if( aladin.calque.getFirstSelectedSimpleImage()==null
      //            && !(v.pref instanceof PlanBG && v.pref.isPixel()) ) {
      if( v==null || v.isFree() || !v.pref.isPixel() ) {
         mode[ToolBox.CONTOUR]=Tool.UNAVAIL;
      }
      
      // Si la vue courante n'est pas sur un cube
      if( v==null || v.isFree() || v.pref.type!=Plan.IMAGECUBE ) {
         mode[ToolBox.SPECT]=Tool.UNAVAIL;
      }

      // Si le premier plan sélectionné est un MOC, on peut faire un crop
      if( aladin.calque.getFirstSelectedPlan() instanceof PlanMoc ) {
         mode[ToolBox.CROP]=Tool.UP;
      }

      // On invalide l'outil phot pour les plan BG
      //      if( v!=null && !v.isFree() && v.pref instanceof PlanBG ) mode[ToolBox.PHOT]=Tool.UNAVAIL;

      // Si la vue courante a un plan de référence qui n'est pas une image simple
      // ni RGB on invalide HIST et PHOT
      Plan p = aladin.calque.getFirstSelectedPlan();
      if( p==null || !p.isPixel() && p.type!=Plan.IMAGERGB && p.type!=Plan.ALLSKYIMG ) mode[ToolBox.HIST]=Tool.UNAVAIL;

      if( v!=null && !v.isFree() && (v.pref instanceof PlanBG || v.northUp) ) mode[ToolBox.WEN]=Tool.UNAVAIL;

      // Si aucun plan valide dans la pile, tous les boutons sont invalidés
      if( aucun ) for( i=0; i<mode.length; i++ ) mode[i]=Tool.UNAVAIL;

      // Si la pile n'est pas vide (plan en cours..) le PROP et DEL sont possibles
      if( !aladin.calque.isFree() ) mode[ToolBox.PROP]=mode[ToolBox.DEL]=Tool.UP;

      // Repositionnement des boutons en fonctions de leur possibilité
      // et de l'état antérieur
      for( i=0; i<mode.length; i++ ) {
         if( mode[i]==Tool.UP && omode[i]==Tool.DOWN ) mode[i]=Tool.DOWN;
         if( mode[i]!=omode[i] ) dorepaint=true;
         tool[i].setMode(mode[i]);
      }

      // Si aucun outil exclusif enfoncé, alors SELECT enfoncé
      if( !aucun ) {
         boolean trouve=false;
         for( i=0; i<exc.length; i++ ) if( mode[ exc[i] ]==Tool.DOWN ) trouve=true;
         if( !trouve ) { tool[ToolBox.SELECT].setMode(Tool.DOWN); dorepaint=true; }
      }

      // Repaint si necessaire
      if( withRepaint && dorepaint ) repaint();
   }

   /** Test d'un bouton exclusif.
    * @param n Numero du bouton a tester
    * @return <I>true</I> le bouton est exclusif, <I>false</I> sinon
    */
   protected boolean isExcTool(int n) {
      for( int i=0; i<exc.length; i++ ) {
         if( n==exc[i] ) return true;
      }
      return false;
   }

   /** Test d'un bouton a creation automatique d'un plan TOOL
    * @param n Numero du bouton a tester
    * @return <I>true</I> Ok, <I>false</I> sinon
    */
   protected static boolean isForTool(int n) {

      for( int i=0; i<forTool.length; i++ ) {
         if( n==forTool[i] ) return true;
      }
      return false;
   }

   public void mouseDragged(MouseEvent e) {
      //      if( !flagDim ) return;
      //      deltaX = e.getX();
      //      deltaY = e.getY();
   }

   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) { aladin.helpOff(); return; }
      int x = e.getX();
      int y = e.getY();
      int j,i;

      // Recherche du bouton
      i = getToolNumber(x,y);
      if( i<0 ) return;

      if( i==CROP ) {
         if( tool[i].mode!=Tool.DOWN ) {
            if( aladin.view.crop!=null ) aladin.view.crop.setVisible(false);
            else {
               if( aladin.view.crop!=null ) aladin.view.crop.reset();
            }
         }
         aladin.view.repaintAll();
      }

      // Bouton non actif
      if( i!=HIST && i!=SYNC && i!=XMATCH && tool[i].mode!=Tool.DOWN ) return;

      switch(i) {
         case PROP :
            // Propriétés sur un objet sélectionné
            if( aladin.view.isPropObjet() ) aladin.view.propSelectedObj();

            // sinon sur le ou les plans sélectionnés
            else aladin.calque.select.propertiesOfSelectedPlanes();
            break;
         case HIST :
            aladin.updatePixel();
            break;
         case RGB :
            aladin.updateRGB();
            break;
         case MOC :
            aladin.updateMocOperation();
            break;
         case BLINK :
            aladin.updateBlink(0);
            break;
         case XMATCH :
            if( tool[i].mode==Tool.DOWN ) aladin.xmatch();
            else if( aladin.frameCDSXMatch!=null ) aladin.frameCDSXMatch.setVisible(false);
            break;
         case PLOT :
            if( tool[i].mode==Tool.DOWN ) aladin.createPlotCat();
            break;
            //        case RESAMP :
            //           new FrameResample(aladin);
            //           break;
            //        case SYNC :
            //           aladin.viewControl.setSyncPanMode(tool[i].mode==Tool.DOWN);
            //           break;
         case CONTOUR :
            aladin.updateContour();
            break;
         case FILTER :
            aladin.filter();
            break;
            /*
         case BNOTE:
            if( e.shiftDown() ) aladin.pad.reset();
            if( view.hasSelectedObjet() ) view.selObjToPad();
            aladin.pad.setText(aladin.mesure.getText());
            aladin.pad.show();
            aladin.pad.toFront();
            break;
             */
         case DEL:
            // Suppression des objets selectionnes s'il y en a
            if( aladin.view.isDelSelObjet() ) { aladin.view.delSelObjet(); break; }

            // Reset complet
            else if( e.isShiftDown() ) {
               aladin.reset();
               tool[ToolBox.DEL].setMode(Tool.UNAVAIL);
               aladin.console.printCommand("reset");
            }
            // Suppression des vues sélectionnés
            else if( aladin.view.isViewSelected() && aladin.view.isMultiView() ) {
               aladin.view.freeSelected();
               aladin.dialog.resume();
               aladin.calque.repaintAll();
            }
            // Suppression de certaines vues ou plans
            else {
               // Il faut donc supprimer des plans
               //               if( Aladin.STANDALONE || Aladin.confirmation(WDEL) ) {
               aladin.calque.FreeSet(true);
               aladin.dialog.resume();	// Desactivation du GrabIt ?
               //               }
            }
//            aladin.gc();
            break;
      }

      // Je remonte le bouton si c'est un bouton toujours up
      for( j=0; j<up.length; j++ ) {
         if( up[j]==i ) { tool[i].Push(); break; }
      }

      handCursor();
      repaint();
   }

   public void mousePressed(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      int i,j;

      // Recherche du bouton
      i = getToolNumber(x,y);
      if( i<0 ) return;

      // Creation automatique d'un plan tool si on clique sur un des
      // boutons suivants et quel e plan sélectionné n'est pas déjà un plan tool
      //      try {
      //         if( isForTool(i)
      //               && (aladin.calque.getFirstSelectedPlan().type!=Plan.TOOL || e.isShiftDown() )
      //               && tool[i].mode==Tool.UP ) {
      //            newPlanTool();
      //         }
      //      } catch( Exception e1 ) { newPlanTool(); }

      // Bouton non actif ? on ne fait rien
      if( tool[i].mode == Tool.UNAVAIL ) return;

      waitCursor();

      // On remonte tous les boutons de mode exclusif
      if( isExcTool(i) ) {
         for( j=0; j<exc.length; j++ ) {
            if( exc[j]!=i && tool[ exc[j] ].mode==Tool.DOWN ) tool[ exc[j] ].setMode(Tool.UP);
         }
      }

      // On inverse le bouton
      tool[i].Push();

      repaint();
   }

//   protected void newPlanTool() {
//      aladin.calque.selectPlanTool();
//      toolMode(false);
//   }

   // Gestion des curseurs
   private int oc=Aladin.DEFAULTCURSOR;
   private void handCursor() { 	  makeCursor(Aladin.HANDCURSOR); }
   private void waitCursor() {    makeCursor(Aladin.WAITCURSOR); }
   private void defaultCursor() { makeCursor(Aladin.DEFAULTCURSOR); }
   private void makeCursor(int c) {
      if( oc==c ) return;
      if( Aladin.makeCursor(this,c) ) oc=c;
   }

   public void mouseEntered(MouseEvent e) {
      oc=-1;
      int i=getTool();
      showCurrentButton(-1);
      if( i==ZOOM  || i==TAG ) {
         setMode(i,Tool.UP);
         setMode(SELECT,Tool.DOWN);
      }
   }

   public void mouseExited(MouseEvent e) {
      //      if( flagDim ) return;
      oc=-1;
      //      inRedim=false;
      currentButton=-1;
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      repaint();
   }

   // Info sur le plan
   public void mouseMoved(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();

      // Gestion du Help individualise pour chaque bouton
      if( aladin.inHelp ) {
         int n;
         if( y>=hs-2*L ) n=-2;
         else n=getToolNumber(x,y);
         aladin.help.setText(Help(n));
         return;
      }

      // Info sur les boutons
      int i = getToolNumber(x,y);
      if( i<0 ) return;

      if( tool[i].mode!=Tool.UNAVAIL /* || isForTool(i)*/ ) handCursor();
      else defaultCursor();

      String s = tool[i].getInfo();
      Util.toolTip(this,Util.fold(s,20,true));

      // Montre le bouton courant
      showCurrentButton(i);

   }

   /** Retourne le numéro du bouton sous la position x,y, ou -1 si aucun */
   protected int getToolNumber(int x,int y) {
      for( int i=0; i<drawn.length; i++ ) {
         if( tool[drawn[i]].in(x,y) ) return drawn[i];
      }
      return -1;
   }

   /** Retourne le numéro d'ordre d'apparition de la barre du bouton numéro number */
   private int getToolOrder(int number) {
      for( int order=0; order<drawn.length; order++ ) if( drawn[order]==number ) return order;
      return -1;
   }


   /** Dessin d'un bouton (avec encadrement)
    * @param order Numero d'ordre du bouton
    * @param number Numero du bouton
    * @param g Contexte graphique
    * @param currentButton true si c'est le bouton courant (affichage vert)
    */
   protected void drawButton(int order,int number, Graphics g,boolean currentButton) {
      int x = (order/nb)*W;                // Position en abscisse du bouton
      int y = (order%nb)*H;                // Position en ordonnee du bouton
      tool[number].drawIcone(g,x,y,currentButton);
   }

   /** Adaptation de la taille de la Tool Bar.
    * Calcul de la configuration de la boite a boutons
    * en fonction de la hauteur indiquee
    * @param hs La nouvelle hauteur de la Toolbar
    * @return la largeur de la Toolbar (dependante du nombre de colonnes)
    */
   protected void calcConf(int hs) {
      int nbtoolParCol=0; // Nombre moyen de boutons par col
      int nbtool = drawn.length;

      // On determine le nombre de colonnes en fonction de la hauteur
      // minimale d'un bouton
      for( nc=1; nc<50; nc++ ) {
         nbtoolParCol=nbtool/nc;
         if( nbtool%nc!=0 ) nbtoolParCol++;      // on majore
         H = hs/nbtoolParCol;                     // hauteur qu'aurait un bouton
         if( H>=HMIN ) break;
      }

      // On ajuste la hauteur du bouton pour equilibrer les colonnes
      // en jouant sur la taille du bouton (entre minimal et recommandee)
      for( H=HREC; H>HMIN ; H--) {
         if( H*nbtoolParCol<hs ) break;
      }

      // Nombre de boutons par colonne pleine
      nb = hs/H;
      if( nb<1 ) { nb=1; hs=H; }

      // Largeur et hauteur de la boite a outils
      this.ws = ws = nc*W;
      this.hs = hs+ICONEGAP;
      super.setSize(ws,hs+ICONEGAP);
      //      aladin.validate();
      Tool.resize(W,H);      // Changement de taille des boutons
   }


   // Pour tracer les petites triangles du logo de redimensionnement
   //   static private int XA[] = new int[3];
   //   static private int YA[] = new int[3];

   /** Dessin de l'icone de changement de proportion.
    * entre la boite de mesure et le reste
    * Se situera toujours en dessous de la boite a outils
    * @param g Contexte graphique
    */
   //   protected void drawIconeProp(Graphics g) {
   //      int x=ws/2;
   //      int y=hs-L-2;
   //
   //      g.setColor(flagDim||inRedim?Color.blue:Color.black);
   //
   //      for( int i=0; i<4; i++ ) {
   //         switch(i) {
   //            case 0: XA[0] = x-2; XA[1] = x; XA[2] = x+2;
   //                    YA[0] = y-2; YA[1] = y-4; YA[2] = y-2;
   //                     break;
   //            case 1: YA[0] = y+2; YA[1] = y+4; YA[2] = y+2;
   //                    break;
   //            case 2: XA[0] = x+6; XA[1] = x+8; XA[2] = x+6;
   //                    YA[0] = y-2; YA[1] = y; YA[2] = y+2;
   //                    break;
   //            case 3: XA[0] = x-6; XA[1] = x-8; XA[2] = x-6;
   //                    break;
   //         }
   //         g.fillPolygon(XA,YA,XA.length);
   //         g.drawPolygon(XA,YA,XA.length);
   //      }
   //
   //      g.drawLine( x-6,y,x+6,y);
   //      g.drawLine( x, y-4,x,y+4);
   //
   //   }

   private int currentButton=-1;
   private int oldCurrentButton=-1;

   private void showCurrentButton(int i) {
      if( oldCurrentButton==i ) return;
      Graphics g = getGraphics();
      if( oldCurrentButton>=0 ) drawButton(getToolOrder(oldCurrentButton),oldCurrentButton,g,false);
      oldCurrentButton=currentButton=i;
      if( currentButton>=0 ) drawButton(getToolOrder(currentButton),currentButton,g,true);
   }

   public void setSize(int width, int height) {
      calcConf(height-ICONEGAP);
   }

   public void paintComponent(Graphics g) {

      if( getSize().width!=ws || getSize().height!=hs ) {
         calcConf(getSize().height-ICONEGAP);   //Calcul de la nouvelle conf
      }

      // AntiAliasing
      aladin.setAliasing(g);

      // Remplissage du fond
      //      g.setColor( getBackground() );
      //      g.fillRect(dx,dy,ws,hs);

      // Dessin de l'icone de changt de proportion
      //      drawIconeProp(g);

      // Dessin de chaque boutons
      for( int i=0; i<drawn.length; i++ ) drawButton(i,drawn[i],g,currentButton==i);
      showCurrentButton(currentButton);
   }


   public Dimension getPreferredSize() { return new Dimension(ws,hs); }


   /** Affiche les textes d'aide adaptes a chaque bouton */
   protected String Help(int n) {
      if( n==-1 ) return aladin.chaine.getString("ToolBox.HELP1");
      if( n==-2 ) return aladin.chaine.getString("ToolBox.HELP2");
      return aladin.chaine.getString("ToolBox.HELP1")+"\n!Tool: "+Tool.label[n]+
            "\n"+aladin.chaine.getString("Tool."+Tool.label[n])+"\n";
   }

   /** Implémentation des méthodes de WidgetFinder */

   public boolean findWidget(String name) {
      if( name.equalsIgnoreCase("contour")
            || name.equalsIgnoreCase("filter")
            || name.equalsIgnoreCase("rgb") ) {
         return true;
      }

      return false;
   }

   /** Retourne la position du widget repéré par son nom
    * ATTENTION : DOIT ETRE DONNE DANS LA LANGUE COURANTE
    */
   public Point getWidgetLocation(String name) {
      for( int i=0; i<drawn.length; i++ ) {
         if( name.equalsIgnoreCase(tool[drawn[i]].nom) ) return tool[drawn[i]].getWidgetLocation();
      }
      return new Point(0,0);

      //      int ntool, tool;
      //      tool = -1;
      //      if( name.equalsIgnoreCase("contour") ) tool = CONTOUR;
      //      else if( name.equalsIgnoreCase("filter") ) tool = FILTER;
      //      else if( name.equalsIgnoreCase("rgb") ) tool = RGB;
      //
      //      ntool = getNTool(tool);
      //      Point p = new Point(0, 0);
      //      if( ntool!=-1 ) {
      //         p = new Point((ntool/nb)*W+W/2, (ntool%nb)*H+H/2);
      //      }
      //
      //      return p;
   }

   //   private int getNTool(int tool) {
   //      for( int i=0; i<sort.length; i++ ) {
   //         if( tool==sort[i] ) return i;
   //      }
   //
   //      return -1;
   //   }

   public void mouseClicked(MouseEvent e) { }


   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity,JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
//      voc.setCollapsed(false);
   }

   @Override
   public void paintCollapsed(Graphics g) {
      Tool.drawVOPointer(g,10,5);
   }

}
