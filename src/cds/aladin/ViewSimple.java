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

import healpix.essentials.FastMath;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cds.astro.AstroMath;
import cds.moc.Healpix;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Gestionnaire de la vue. Il s'agit d'afficher les plans acifs (voir
 * ToolBox) dans l'ordre de visibilite selon l'echelle indiquee par le zoom
 * (voir ZoomView)
 * <P>
 * La souris a differentes fonctions suivant l'outil (Tool bar) active.
 * Ce peut etre la simple selection d'une source, le dessin d'objet
 * graphique, le zoom pointe...
 *
 * @see aladin.ToolBox()
 * @see aladin.Zoomview()
 * @author P. Fernique CDS
 * @version 1.2 : (1 dec 00)  Meilleure gestion de la source montree + gestion du plan Add
 * @version 1.1 : (28 mars 00)  Modif debug -> trace + label pour les plans
 * @version 1.0 : (11 mai 99)   Toilettage du code
 * @version 0.91 - 3 dec 1998   Nettoyage du code
 * @version 0.9 - 31 mars 1998
 */
public class ViewSimple extends JComponent
implements MouseWheelListener, MouseListener,MouseMotionListener,
KeyListener,ActionListener,
DropTargetListener, DragSourceListener, DragGestureListener {

   static final boolean OVERLAYFORCEDISPLAY = true;
   static final String CREDIT = "Powered by Aladin";
   static final float CARTOUCHE = 0.7f;

   //   static final int CMSIZE = 100;      // Taille de la portion d'image pour la CM dynamique
   Font F = Aladin.SPLAIN;

   static final int MOVE       = 0x1;
   static final int ROLL       = 0x2;
   static final int MOVECENTER = 0x4;

   // Les references aux autres objets
   Aladin aladin;
   Calque calque;
   View view;
   ZoomView zoomview;
   Status status;

   // Les valeurs a memoriser
   int ordreTaquin;
   Sablier sablier = new Sablier();  // Sablier d'attente de la premi�re image en fullScreen
   Rectangle rv;                 // Taille courante du view
   MemoryImageSource memImgCM=null;// Pour eviter de recreer l'image de l'histograme
   Image imgprep;                // Image en cours de preparation
   Image oimg;                   // Precedente reference a l'image de base
   RectangleD orz;               // Precedent rectangle de zoom
   boolean flagBord;             // true s'il y a une marge autour de l'image
   double oz;                    // Precedente valeur du zoom
   int pHashCode=-1;             // Pr�c�dent plan image utilis� (son hashCode)
   Thread testagain;             // Dans le cas ou l'image n'est pas encore prete
   Image img;                    // Buffer du paint
   Graphics g;                   // Contexte graphique du buffer du paint
   boolean imgok=true;           // Vrai si l'image courante est prete
   Rectangle clip;               // Clip rect courant
   boolean flagLigneClic;        // true si on trace une ligne en mode clic, sinon false;
   boolean first=true;           // Pour afficher au demarrage du Help
   int lastImgID=-1;             // ID de la derniere image visualis�e dans la vue
   byte [] pixels;               // Pixels de la portion de l'image visible
   int [] pixelsRGB;             // Pixels de la portion de l'image visible
   int [] tmpRGB;                // Pixels de la portion de l'image visible
   protected RainbowPixel rainbow;    // Un Rainbow en superposition de la vue pour les pixels
   protected Rainbow rainbowF;    // Un Rainbow en superposition de la vue pour les filtres
   CubeControl cubeControl=null; // En cas d'image Blink, le controleur associ�
   protected boolean flagHuge;   // true: Cette vue affiche une image HUGE en pleine r�solution
   int xHuge,yHuge,wHuge,hHuge;  // Position de la zone Haute d�finition si image Huge (coord dans l'image sous-�chantillonn�e)
   int w,h;                      // largeur et hauteur de l'image dans pixels[]
   int dx,dy;                    // memo de la marge en blanc
   boolean northUpDown;          // true si le North est en haut/bas (pour l'affichage de la grille)
   //   boolean flagCM = false;       // True si on s'amuse avec la table des couleurs
   //   byte [] pixelsCM = new byte[CMSIZE*CMSIZE]; // pixels de la portion test de la table des couleurs
   //   int [] pixelsCMRGB = new int[CMSIZE*CMSIZE]; // pixels de la portion test de la table des couleurs
   int [] tmpCMRGB ;             // pixels de la portion test de la table des couleurs
   int cmX,cmY;                  // taille de pixelsCM[]
   ColorModel cm;                // Model de couleur courant
   //   int etatDynamicCM;          // idem que "etat" mais concernant la zone test de CM
   boolean modeGrabIt=false;     // True si on est en mode GrabIt
   double grabItX=0,grabItY=0;   // Memorisation du debut du GrabIt
   double pGrabItX=-1,pGrabItY=0;// Memorisation de la fin du GrabIt (position prec)

   double cGrabItX=-1,cGrabItY=0;    // Memorisation de la fin du GrabIt (position cour)
   int currentCursor=Aladin.DEFAULTCURSOR; // Le curseur courant
   private int scrollX,scrollY;      // pour un scrolling via la souris

   // Les valeurs a memoriser pour le rectangle de selection et de zoom
   Rectangle rselect;            // Rectangle de selection multiple (ou zoom)
   //   Rectangle orselect;           // Precedent rectangle de selection multiple (ou zoom)
   boolean flagDrag;             // Vrai si on drague la souris
   boolean quickInfo;             // Vrai si on doit rapidement r�afficher les infos pixels et pos
   private boolean flagOnMovableObj; // True si on est en fullScreen et le pointeur sur un �l�ment de FoV (voir mouseDrag)
   private boolean flagOnFirstLine; // True si on est sur un d�but de polyligne (lors de l'insertion d'une polyligne)
   boolean flagMoveRepere=true;  // � false si on ne doit pas d�placer le repere
   boolean flagClicAndDrag=false;
   private int flagDragField;    // MOVE | ROLL si on tourne et/ou d�place un PlanField
   PointD fixe;                  // Point d'origine d'un deplacement (coord Image)
   PointD fixebis;               // Point d'origine d'un ajustement d'une recalibration d'un catalogue
   PointD fixev;                 // Point d'origine d'une select multi (coord View)
   PointD lastView=null;         // Derniere position d'un mouseMove pour SimRep
   PointD lastMove=null;         // Derniere position d'un mouseMove pour les fleches
   // en coordonnees de l'image

   // Les valeurs a memoriser pour montrer les sources
   private final Vector showSource = new Vector();   // Liste des blinks de la vue
   boolean repereshow;           // Vrai si un repere d'une source est actif

   // Les param�tres du zoom sur la vue
   protected double zoom;         // Facteur de zoom
   protected double xzoomView,yzoomView; // Centre du zoom (coordonn�es imagette)
   protected RectangleD rzoomView;// Portion de l'image visible par le zoom (coord imagette)
   protected double HView,WView;  // Taille de l'imagette
   protected RectangleD rzoom;    // Portion de l'image visible par le zoom (coord image)
   protected short iz;

   // POUR LE MOMENT CE N'EST PAS UTILISE (PF FEV 2009)
   protected Projection projLocal;  // Projection propre � la vue (pour planBG) => voir getProj()

   // Les param�tres associ�es � la vue
   protected Plan   pref;      // Le plan de r�f�rence
   protected boolean locked;      // True s'il s'agit d'une vue lock�e (anciennement ROI)
   protected boolean selected; // True si la vue est s�lectionn�
   protected boolean sticked;  // True si la vue est fix�e (pas sujette au scrolling)
   protected boolean northUp;  // True si la vue est forc�e Nord en saut, Est � gauche
   protected int n;            // Num�ro de la vue (indice dans aladin.view.simpleView[])

   /*anais*/
   boolean   RGBDynInit=false;

   // Pour les plans blinks
   private MemoryImageSource memImg=null;
   private double previousFrameLevel;   // Derni�re frame affich�e
   private boolean flagBlinkControl;    // Vrai pour un r�affichage rapide du blinkControl
   private boolean flagCube=false;      // True si on a cliqu� sur le blink Control
   private boolean flagSimRepClic=false; // true si on a cliqu� sur une marque Simbad

   private Plan planRecalibrating=null; // Plan catalogue en cours de recalibration, null si aucun
   protected Plan oldPlanBord=null;       // Pr�c�dent plan dont on affiche le bord

   // D�di�s au planBG => voir PlanBG.getImage(...)
   protected int owidthBG=-1,oheightBG=-1;
   protected Graphics g2BG=null;
   protected int ovizBG=-3;
   protected BufferedImage imageBG=null;
   //   protected Image imageBG=null;
   protected int oImgIDBG=-2;

   /** Creation de l'objet View
    * @param aladin Reference
    */
   protected ViewSimple(Aladin aladin,View view,int w,int h,int nview) {
      this.aladin = aladin;
      this.status = aladin.status;
      this.calque = aladin.calque;
      this.view = view;
      this.zoomview = aladin.calque.zoom.zoomView;
      this.n=nview;
      iz=1;
      lastImgID=-1;

      createPopupMenu();
      setBackground(Color.white);
      setDimension(w,h);

      addMouseWheelListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
      addKeyListener(this);

      setOpaque(true);
      //      setDoubleBuffered(false);

      setFocusTraversalKeysEnabled(false);

      // Pour g�rer le DnD de fichiers externes
      new DropTarget (this, this);
      DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            this, DnDConstants.ACTION_COPY_OR_MOVE, this);
   }
   
   // pour les classes d�riv�es
   protected ViewSimple(Aladin aladin) { this.aladin=aladin; }
   
   public void dragGestureRecognized(DragGestureEvent dragGestureEvent) { }
   public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
      aladin.view.setCurrentView(this);
      dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
   }
   public void dragExit (DropTargetEvent dropTargetEvent) {}
   public void dragOver (DropTargetDragEvent dropTargetDragEvent) {}
   public void dropActionChanged (DropTargetDragEvent dropTargetDragEvent){}
   public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent){}
   public void dragEnter(DragSourceDragEvent DragSourceDragEvent){}
   public void dragExit(DragSourceEvent DragSourceEvent){}
   public void dragOver(DragSourceDragEvent DragSourceDragEvent){}
   public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent){}
   public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
      aladin.drop(dropTargetDropEvent);
      aladin.view.setMouseView(this);
   }

   private long lastTime = 0L;
   public void mouseWheelMoved(MouseWheelEvent e) {
      Coord coo=null;
      PointD p=null;
      if( e.getClickCount()==2 ) return;    // SOUS LINUX, J'ai un double �v�nement � chaque fois !!!
      int mult=1;

      if( isFullScreen() && widgetControl!=null && widgetControl.mouseWheel(e) ) {
         repaint(); return;
      }

      if( isPlanBlink() && cubeControl.mouseWheelMoved(e) ) return;

      // Synchronisation sur une autre vue ?
      ViewSimple vs = getProjSyncView();

      // Acc�l�ration
      if( vs.pref instanceof PlanBG ) {
         long time = System.currentTimeMillis();
         if( lastTime!=0L ) {
            long delta = time - lastTime;
            if( delta<30 ) mult=3;
            else if( delta<50 ) mult=1;
         }
         lastTime=time;
      }

      // Peut �tre un changement sur le tag courant
      try {
         p = vs.getPosition((double)e.getX(),(double)e.getY());
         Tag tag=null;
         // tag en cours d'�dition ?
         if( view.newobj!=null && view.newobj instanceof Tag ) {
            if( ((Tag)view.newobj).onViaWheel(vs, p.x, p.y) ) tag = (Tag)view.newobj;
         }
         // tag s�lectionn� ?
         if( tag==null )  tag = testWheelTag(p.x,p.y);
         if( tag!=null ) {
            tag.modifyViaWheel(e.getWheelRotation());
            aladin.view.repaintAll();
            return;
         }
      }catch( Exception e1) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

      try {

         // On positionne le xviewzoom.x et xviewzoom.y sur la position
         // de la souris dans le cas o� le zoom via la position c�leste
         // ne fonctionnerait pas
         PointD p1 = getZoomCoord(e.getX(), e.getY());

         // Dans le cas d'un nuage de points, on prend simplement le centre de la vue
         // sinon le d�placement est trop complexe
         if( isPlotView() ) {
            p1 = getZoomCoord(rv.width/2, rv.height/2);
            xzoomView=p1.x;
            yzoomView=p1.y;
         }

         // Si le repere n'existe pas ou qu'il n'est pas dans la vue, on zoom au centre
         // (ou qu'on est en train de faire un crop)
         else if( !isPlotView() &&  ( /* aladin.toolBox.getTool()==ToolBox.PHOT || */
               view.repere==null ||
               !(vs.pref instanceof PlanBG) && view.repere.getViewCoord(vs,0,0)==null
               || hasCrop() ) ) {
            if( hasCrop() ) p = view.crop.getFocusPos();
            //            else p = vs.getPosition((double)e.getX(),(double)e.getY());
            coo = new Coord();
            coo.x=p.x; coo.y=p.y;
            vs.getProj().getCoord(coo);
         }

      } catch( Exception e1 ) { coo=null; /* if( aladin.levelTrace>=3 ) e1.printStackTrace();*/ }
      if( aladin.toolBox.getTool()==ToolBox.ZOOM ) { flagDrag=false; rselect = null; }
      if( e.isShiftDown() ) aladin.view.selectCompatibleViews();

      vs.syncZoom(-e.getWheelRotation()*mult,coo,false);
   }

   /**
    * Zoom synchroniz�.
    * @param sens 1:augmentation, -1:diminution
    * @param coo centre du zoom, null si Repere courant
    */
   private void syncZoom(int sens,Coord coo,boolean flagPow2) {
      if( isFree() ) return;
      double nz = aladin.calque.zoom.getNextValue(zoom,sens,flagPow2);
      //      aladin.trace(4,"ViewSimple.syncZoom("+sens+","+(coo==null?null:aladin.localisation.frameToString(coo.al, coo.del))+") zoom="+zoom+" => nz="+nz);
      if( nz==-1 ) return;

      if( !selected ) {
         aladin.calque.unSelectAllPlan();
         aladin.view.unSelectAllView();
         aladin.view.setCurrentView(this);
         selected=true;
         aladin.view.setSelectFromView(true);
      }

      aladin.view.setZoomRaDecForSelectedViews(nz,coo,this,true,false);
      //      if( aladin.view.onUndoTop() ) aladin.view.memoUndo(this, coo, null);
   }

   JPopupMenu popMenu;
   JMenuItem menuLabel,menuClone,menuCopy,menuCopyImg,menuLock
   //             ,menuROI,menuDel,menuDelROI,menuStick,menuSel,
   //              menuMore,menuNext,menuScreen
   ;

   // Cree le popup menu associe au View
   private void createPopupMenu() {
      JMenuItem j;
      popMenu = new JPopupMenu();
      popMenu.setLightWeightPopupEnabled(false);
      //      popMenu.add( menuNext=j=new JMenuItem(view.NEXT));
      //      j.addActionListener(this);
      //      String s = aladin.isBetaProtoMenu(aladin.FULLSCREEN+"  (F9)");
      //      if( s!=null ) {
      //         popMenu.add( menuScreen=j=new JMenuItem(s));
      //         j.addActionListener(this);
      //      }
      //      popMenu.add( menuMore=j=new JMenuItem(view.MOREVIEWS));
      //      j.addActionListener(this);
      //      popMenu.addSeparator();
      popMenu.add( menuCopyImg=j=new JMenuItem(view.MCOPYIMG));
      j.addActionListener(this);
      popMenu.add( menuCopy=j=new JMenuItem(view.MCOPY));
      j.addActionListener(this);
      popMenu.add( menuClone=j=new JMenuItem(aladin.CLONE));
      j.addActionListener(this);
      popMenu.add( menuLabel=j=new JMenuItem(view.MLABELON));
      j.addActionListener(this);
      popMenu.addSeparator();
      popMenu.add( menuLock=j=new JMenuItem(view.MNEWROI));
      j.addActionListener(this);
      ////      popMenu.add( menuROI=j=new JMenuItem(view.MROI));
      ////      j.addActionListener(this);
      //      popMenu.add( menuDelROI=j=new JMenuItem(view.MDELROI));
      //      j.addActionListener(this);
      //      popMenu.addSeparator();
      //      popMenu.add( menuSel=j=new JMenuItem(view.MSEL));
      //      j.addActionListener(this);
      //      popMenu.add( menuDel=j=new JMenuItem(view.MDELV));
      //      j.addActionListener(this);
      //      popMenu.addSeparator();
      //      popMenu.add( menuStick=j=new JMenuItem(view.MSTICKON));
      //      j.addActionListener(this);

      super.add(popMenu);
   }

   /** Reactions aux differents boutons du menu */
   public void actionPerformed(ActionEvent e) {

      Object src = e.getSource();

      if( src==menuLabel )  view.setSourceLabel();
      else if( src==menuClone )  aladin.cloneObj(false);
      else if( src==menuCopy )   aladin.copyToClipBoard(aladin.localisation.J2000ToString(repCoord.al,repCoord.del));
      else if( src==menuCopyImg )copier();
      //      else if( src==menuROI )    aladin.view.createROI();
      else if( src==menuLock )   switchLock();
      //      else if( src==menuDel )    aladin.view.freeSelected();
      //      else if( src==menuDelROI ) aladin.view.freeLock();
      //      else if( src==menuSel )    aladin.view.selectAllViews();
      //      else if( src==menuMore )   aladin.view.autoViewGenerator();
      //      else if( src==menuNext )   aladin.view.next(1);
      //      else if( src==menuScreen )   aladin.fullScreen();
      //      else if( src==menuStick ) {
      //         aladin.view.stickSelectedView();
      //         return;
      //      }
      calque.repaintAll();
   }

   /** Copie la vue courante dans le Clipboard */
   protected void copier() {
      aladin.copyToClipBoard(getImage(-1,-1));
   }

   /** Permute l'�tat lock de la vue */
   protected boolean switchLock() {
      if( isFree() ) return false;
      locked=!locked;
      repaint();
      return true;
   }

   /** True si le mode Nord en haut est activ� pour cette vue */
   protected boolean isNorthUp() { return northUp; }

   /** En attente de d�veloppement de la rotation libre des images */
   //    protected boolean isRolled() { return true; }

   /** True si le plan de r�f�rence de cette vue peut �tre forc�e Nord en haut */
   protected boolean canBeNorthUp() {
      Projection proj = getProj();
      return !isFree() && Projection.isOk(proj) /* && pref.isImage() */
            && (!(pref instanceof PlanBG) || pref instanceof PlanBG && proj.rot!=0 )
            && !isProjSync();
   }

   /** Permute l'�tat northUp de la vue */
   protected boolean switchNorthUp() {
      if( !canBeNorthUp() ) return false;
      if( pref instanceof PlanBG ) {
//         pref.projd.setProjRot(0);
         getProj().setProjRot(0);
      } else {
         Coord coo = getCooCentre();
         northUp=!northUp;
         setZoomRaDec(0,coo.al,coo.del);
         if( pref.isImage() ) ((PlanImage)pref).changeImgID();
      }
      newView(1);
      aladin.view.repaintAll();
      return true;
   }

   // Affiche le popup
   private void showPopMenu(int x,int y) {
      //      menuDel.setEnabled(aladin.view.isMultiView());
      //      menuSel.setEnabled(aladin.view.isMultiView());
      //      menuStick.setEnabled(aladin.view.isMultiView());
      menuClone.setEnabled(aladin.view.hasSelectedSource());
      menuCopy.setEnabled(repCoord.al!=0 && repCoord.del!=0);
      //      menuStick.setText( sticked ? view.MSTICKOFF:view.MSTICKON);
      menuLabel.setText( view.labelOn() ? view.MLABELON:view.MLABELOFF);
      menuLock.setEnabled( !isProjSync() );
      menuLock.setSelected(locked);
      //      menuMore.setEnabled( !aladin.view.allImageWithView());
      //      menuNext.setEnabled( aladin.calque.getNbPlanImg()>1 );

      popMenu.show(this,x,y);
   }

   /** Retourne true si la vue n'est pas utilis�e */
   protected boolean isFree() { return pref==null || pref.type==Plan.NO || pref.type==Plan.X; }

   /** Retourne true si la vue est synchronis�e */
   //   protected boolean isSync() {
   //      if( isFree() || !(pref.type==Plan.ALLSKYIMG) ) return true;
   ////      System.out.println("ViewSimple="+this+" ovizBG="+ovizBG+" iz="+iz);
   //      return ovizBG==iz;
   //   }

   /** Attend que la vue soit synchronis�e - pour les PlanBG */
   //   protected void sync() {
   //      if( isSync() ) return;
   //      while( !isSync() ) {
   //         Util.pause(25);
   //         aladin.trace(4,Thread.currentThread().getName()+": ViewSimple.sync() waiting image PlanBG buffer...");
   //      }
   //   }

   /** Lib�re la vue  */
   protected void free() {
      //      unlockRepaint("free");
      sticked=selected=locked=northUp=false;
      pref=null;
      memImgCM=null;
      imgprep=oimg=img=null;
      memImg=null;
      pixels=null;
      pixelsRGB=null;
      cubeControl=null;
      previousFrameLevel=-1;
      lastImgID=-1;
      ordreTaquin=-1;
      oiz=oTailleRA=oTailleDE=-1;
      projLocal=null;
      imageBG=null;
      if( g2BG!=null ) { g2BG.dispose(); g2BG=null; }
      if( isPlotView() ) plot.free();
      plot=null;
   }

   /** Copy d'une vue dans une autre */
   protected void copyIn(ViewSimple v) {
      v.pref=pref;
      v.locked=locked;
      v.northUp=northUp;
      v.ordreTaquin=ordreTaquin;
      v.projLocal=projLocal==null ? null : projLocal.copy();
      v.plot = isPlotView() ? plot.copyIn(v) : null;

      if( pref.isCube() ) v.cubeControl = cubeControl.copy();
      v.setZoomXY(zoom,xzoomView,yzoomView);
   }

   //   /** G�n�ration d'un plan image correspondant � la portion visible */
   //   protected void copyAndCrop() {
   //      if( pref instanceof PlanImageBlink ) { pickAndCropFrame(); return; }
   //
   //      try {
   //         PlanImage pi = (PlanImage)aladin.calque.dupPlan((PlanImage)pref,
   //               "["+pref.label+"]",pref.type);
   //         aladin.pad.setCmd("copy "+Tok.quote(pref.label));
   //         pi.crop(rzoom.x,rzoom.y,rzoom.width,rzoom.height,false);
   //         pi.from="Cropped image from "+pref.label
   //            +" ("+rzoom.x+","+rzoom.y+","+rzoom.width+","+rzoom.height+")";
   //         aladin.view.createViewForPlan(pi);
   //      } catch( Exception e ) {}
   //   }
   //
   //   /** G�n�ration d'un plan correspondant � la frame courante d'un plan blink */
   //   protected void pickFrame() {
   //      try {
   //         int frame = blinkControl.lastFrame+1;
   //         PlanImage pi = (PlanImage)aladin.calque.dupPlan((PlanImage)pref,
   //                "#"+frame+"."+pref.label,pref.type);
   //         pi.type=Plan.IMAGE;
   //         pi.calculPixelsZoom(pi.pixels);
   //         pi.from="Cube picked frame #"+frame+" from "+pref.label;
   //         aladin.view.createViewForPlan(pi);
   //      } catch( Exception e ) {}
   //   }

   /** G�n�ration d'un plan � partir des pixels rep�r�s par le rectangle crop pour un plan allsky */
   protected PlanImage cropAreaBG(RectangleD rcrop,String label,double zoom,double resMult,boolean fullRes,boolean inStack) {
      PlanImage pi=null;
      PlanBG pref = (PlanBG)this.pref;
      pref.projd = projLocal.copy();

      try {
         if( label==null ) label = pref.label;
         if( inStack )  pi = (PlanImage)aladin.calque.dupPlan(pref, null,pref.type,false);
         else {
            if( pref.color ) pi = new PlanImageRGB(aladin,pref);
            else pi = new PlanImage(aladin,pref);
         }
         pi.flagOk=false;
         pi.setLabel(label);
         pi.pourcent=1;
         pi.type=Plan.IMAGE;
         boolean cropped;

         double zoomFct = zoom*resMult;

         pi.width = pi.naxis1 = (int)Math.round(rcrop.width*zoomFct);
         pi.height = pi.naxis2 = (int)Math.round(rcrop.height*zoomFct);
         pi.initZoom=1;

         // Conversion dans les coord. de la vue
         PointD p = getViewCoordDble(rcrop.x, rcrop.y);
         RectangleD rview = new RectangleD(p.x,p.y,pi.width,pi.height);

         if( pref.hasOriginalPixels() ) {
            pref.getCurrentBufPixels(pi,rcrop,zoomFct,resMult,fullRes);

         } else if( pref.color ) {
            pi.type=Plan.IMAGERGB;
            pi.bitpix=8;

            ((PlanImageRGB)pi).pixelsRGB = pref.getPixelsRGBArea(this,rview,true);
            pi.cm = IndexColorModel.getRGBdefault();
            ((PlanImageRGB)pi).initCMControl();
            ((PlanImageRGB)pi).flagRed=((PlanImageRGB)pi).flagGreen=((PlanImageRGB)pi).flagBlue=true;

         } else {
            pi.pixels = pref.getPixels8Area(this,rview,true);
            pi.bitpix=8;
            pi.video = pref.video;
            pi.setTransfertFct(PlanImage.LINEAR);
            pi.cmControl[0]=0; pi.cmControl[1]=128; pi.cmControl[2]=255;
            pi.restoreCM();
         }

         pi.projd.cropAndZoom(rcrop.x,rcrop.y,rcrop.width,rcrop.height, zoomFct);

         PointD pA = getPosition(getWidth()/2.,getHeight()/2);
         double deltaX = (pA.x-Math.floor(pA.x))*zoomFct;
         double deltaY = (pA.y-Math.floor(pA.y))*zoomFct;
         pi.projd.deltaProjXYCenter(-deltaX,-deltaY);

         // Beurk !! en attendant BOF
         try { pi.projd = Projection.getEquivalentProj(pi.projd); }
         catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

         pi.noCacheFromOriginalFile();
         cropped=true;

         pi.copyright = "Dumped from "+pref.label
               + (cropped?" cropped ("+rcrop+")":"");

         pi.setHasSpecificCalib();
         pi.pourcent=-1;
         pi.isOldPlan=false;
         pi.ref=false;
         pi.selected=false;
         pi.setOpacityLevel(1f);
         pi.changeImgID();
         if( inStack ) pi.resetProj();
         pi.colorBackground=null;
         pi.reverse();
         pi.objet = pi.projd.getProjCenter().getSexa();
         pi.flagOk=true;

      } catch( Exception e ) { if( pi!=null ) pi.error=e.getMessage(); e.printStackTrace(); }
      return pi;
   }


   /** G�n�ration d'un plan � partir des pixels rep�r�s par le rectangle crop (�ventuellement extraction de la frame
    * courante dans le cas d'un plan Blink et possibilit� de recadrage sur la portion visible
    */
   protected PlanImage cropArea(RectangleD rcrop,String label,double zoom,double resMult,boolean fullRes,boolean verbose) {
      PlanImage pi=null;

      if( pref.type==Plan.ALLSKYIMG ) return cropAreaBG(rcrop,label,zoom,resMult,fullRes,true);

      try {
         int frame=0;
         if( label==null ) label = pref.label;
         pi = (PlanImage)aladin.calque.dupPlan((PlanImage)pref, null,pref.type,false);
         pi.flagOk=false;
         pi.setLabel(label);
         pi.pourcent=1;
         if( !(pref instanceof PlanImageRGB) ) pi.type=Plan.IMAGE;
         boolean picked= pref instanceof PlanImageBlink;
         if( picked ) ((PlanImageBlink)pref).activePixelsOrigin(this,pi);
         boolean cropped;
         double x=0,y=0,w=0,h=0;

         x=(int)Math.floor(rcrop.x);     y=(int)Math.floor(rcrop.y);
         w=(int)Math.ceil(rcrop.width);   h=(int)Math.ceil(rcrop.height);
         if( verbose ) aladin.console.printCommand("crop "+D(x)+","+D( ((PlanImage)pref).naxis2-(y+h))+" "+D(w)+"x"+D(h));
         if( pref.type==Plan.IMAGEHUGE ) cropped = pi.cropHuge(x,y,w,h,false);
         else cropped = pi.crop(x,y,w,h,false);

         if( picked ) frame = cubeControl.lastFrame+1;

         pi.copyright = "Dumped from "+pref.label
               + (picked?"  frame #"+frame:"")
               + (cropped?" cropped ("+rcrop+")":"");

         pi.setHasSpecificCalib();
         pi.pourcent=-1;
         pi.flagOk=true;
         pi.isOldPlan=false;
         pi.ref=false;
         pi.selected=false;
         pi.setOpacityLevel(1f);
         if( !(pref instanceof PlanImageRGB) ) pi.reverse();
         pi.objet = pi.projd.getProjCenter().getSexa();


      } catch( Exception e ) { if( pi!=null ) pi.error=e.getMessage(); e.printStackTrace(); }
      return pi;
   }

   // Juste pour �diter sans le .0 les doubles de fait entiers
   private String D(double x) {
      if( x==(int)x ) return (int)x+"";
      else return x+"";
   }

   /** Convertit des coordonn�es de l'imagette du zoomview
    *  en coordonn�es de l'image
    */
   protected PointD zoomViewToImage(double xZoomView,double yZoomView) {
      Dimension d = getPRefDimension();
      double fct = d.width/WView;
      return new PointD( HItoI(xZoomView*fct) , HItoI(yZoomView*fct) );
   }

   /** Convertit des coordonn�es de l'image
    *  en coordonn�es de l'imagette du zoomview
    */
   protected PointD imageToZoomView(double x,double y) {
      Dimension d = getPRefDimension();
      double fct = WView/d.width;
      return new PointD( ItoHI(x)*fct , ItoHI(y)*fct );
   }

   /** Convertion de coordonnees.
    * @param xview,yview Point dans la Vue (View Frame)
    * @return  Retourne la position dans la vue dans les coordonnees
    * de l'image courante
    */
   protected Point getPosition(int xview, int yview) {
      PointD p = getPosition((double)xview,(double)yview);
      return new Point((int)Math.floor(p.x),(int)Math.floor(p.y));
   }

   /** Convertion de coordonnees.
    * @param xview,yview Point dans la Vue (View Frame)
    * @return  Retourne la position dans l'image originale
    */
   protected PointD getPosition(double xview, double yview) {

      if( rzoom==null ) return new PointD(xview+0.5,yview+0.5);
      double x= rzoom.x + xview/zoom;
      double y= rzoom.y + yview/zoom;

      return new PointD(HItoI(x),HItoI(y));
   }

   /** Convertion de coordonnees.
    * @param ximg,yimg Point dans l'image original
    * @return  Retourne la position dans la view
    */
   protected PointD getPositionInView(double ximg, double yimg) {
      if( rzoom==null ) return new PointD(ximg-0.5,ximg-0.5);
      ximg = ItoHI(ximg);
      yimg = ItoHI(yimg);
      return new PointD( (ximg-rzoom.x)*zoom, (yimg-rzoom.y)*zoom );
   }

   private Coord coo = new Coord();

   /** Convertit une position image orient�e manuellement au Nord, ou une image en synchronisation
    * de projection en coord images r�elles */
   protected PointD northOrSyncToRealPosition(PointD p) {
      coo.x=p.x;
      coo.y=p.y;
      getProj().getCoord(coo);
      pref.projd.getXY(coo);
      p.x=coo.x;
      p.y=coo.y;
      return p;
   }


   int testx1,testy1,testw,testh,testxview,testyview;

   //   protected void testConvertion(double xview, double yview) {
   //      if( !(pref instanceof PlanImage) ) return;
   //      PlanImage p = (PlanImage)pref;
   //      System.out.println("Taille de l'image : "+p.naxis1+"x"+p.naxis2);
   //      System.out.println("Taille de la vue  : "+getWidth()+"x"+getHeight());
   //      System.out.println("Facteur de zoom   : "+zoom);
   //      System.out.println("Champ visible     : "+rzoom);
   //      System.out.println("Pixels extraits   : "+testx1+","+testy1+" "+testw+"x"+testh);
   //      System.out.println("Test conversions:");
   //      PointD pview = new PointD(xview,yview);
   //      PointD pzoom = getZoomCoord(xview, yview);
   //      PointD pimg = getPosition(xview, yview);
   //      PointD pimg2 = zoomViewToImage(pzoom.x,pzoom.y);
   //      System.out.println("   .view="+pview);
   //      System.out.println("   .zoom="+pzoom+" => toimg="+pimg2+" => toviewd="+getViewCoordDble(pimg2.x, pimg2.y));
   //      System.out.println("   .img="+pimg+" => toviewd="+getViewCoordDble(pimg.x, pimg.y)+" => toviewd="+getViewCoord(pimg.x, pimg.y));
   //      try {
   //         Coord coo = new Coord();
   //         coo.x = pimg.x; coo.y = pimg.y;
   //         p.projd.getCoord(coo);
   //         p.projd.getXY(coo);
   //         PointD pimg3 = new PointD(coo.x,coo.y);
   //         System.out.println("   .img="+pimg+" => coo="+coo+" => img="+pimg3);
   //      } catch( Exception e ) { }
   //   }

   /** Convertion de coordonnees.
    * @param xview,yview Point dans la Vue (View Frame)
    * @return  Retourne la position dans les coordonnees de l'imagette du zoom
    */
   protected PointD getZoomCoord(double xview, double yview) {
      PointD c = getPosition(xview,yview);
      Dimension d = getPRefDimension();
      c.x = WView/d.width*c.x;
      c.y = HView/d.height*c.y;
      return c;
   }

   /** Conversion d'un vecteur exprim� dans les coordonn�es de la vue
    * vers les coordonn�es du zoomView;
    * @param dxView
    * @param dyView
    * @return
    */
   protected PointD getDeltaZoomCoord(double dxView, double dyView) {
      Dimension d = getPRefDimension();

      double dx = dxView * (WView/d.width) / zoom;
      double dy = dyView * (HView/d.height) / zoom;
      return new PointD(dx,dy);
   }

   /** Retourne la dimension du plan, c�d largeur et hauteur de l'image
    *  s'il s'agit d'un plan IMAGE, sinon un savant machin s'il s'agit
    *  d'un plan CATALOG
    */

   private final Dimension dimTmp = new Dimension();
   protected Dimension getPRefDimension() {

      // Si il n'y a pas d'image de ref, on prendra la
      // taille de la projection en pixels
      if( isFree() || !pref.isImage() ) {
         dimTmp.width=dimTmp.height=(int)( ( isFree() || !Projection.isOk(getProj()) )?1024:getProj().r );
      } else {
         try {
            dimTmp.width = ((PlanImage)pref).width;
            dimTmp.height = ((PlanImage)pref).height;
         }catch( Exception e ) {
            dimTmp.width=dimTmp.height=1024;
         }
      }
      return dimTmp;
   }



   /** Conversion de coordonnees.
    * @param p pour �viter une allocation, null si allocation � faire
    * @param x,y Point dans l'image courante EN DOUBLES
    * @return  Retourne la position dans les coordonnees de la vue (View Frame)
    */
   protected Point getViewCoord(double x, double y) { return getViewCoord(null,x,y); }
   protected Point getViewCoord(Point p, double x, double y) {
      int newx,newy;

      if( Double.isNaN(x) || pref==null ) return null;

      // Juste pour acc�l�rer (profiling)
      //     x = ItoHI(x);
      //     y = ItoHI(y);
      if( pref.type == Plan.IMAGEHUGE ) {
         int step = ((PlanImageHuge) pref).step;
         x /= step;
         y /= step;
      }

      if( rzoom==null ) {
         newx = (int)Math.round(x);
         newy = (int)Math.round(y);
      } else {
         newx = (int)Math.round( (x - rzoom.x)*zoom );
         newy = (int)Math.round( (y - rzoom.y)*zoom );
      }

      // Petite correction due � une �ventuelle marge sur le bord
      // de l'image (pixel partiel)
      if( this.dx>=0 ) newx-=ddx;
      else newx++;
      if( this.dy>=0 ) newy-=ddy;
      //       else newy++;

      if( p==null ) return new Point(newx,newy);
      p.x=newx;
      p.y=newy;
      return p;
   }


   // ajout thomas
   /** Convertion de coordonnees.
    * Ne teste pas si le point calcule est affichable.
    * @param x,y Point dans l'image courante EN DOUBLES
    * @return  Retourne la position dans les coordonnees de la vue en DOUBLES (View Frame)
    */
   public PointD getViewCoordDble(double x, double y) { return getViewCoordDble(null,x,y); }
   public PointD getViewCoordDble(PointD p, double x, double y) {
      double newx,newy;

      // Juste pour acc�l�rer (profiling)
      //     x = ItoHI(x);
      //     y = ItoHI(y);
      if( pref.type == Plan.IMAGEHUGE ) {
         int step = ((PlanImageHuge) pref).step;
         x /= step;
         y /= step;
      }

      if( p==null ) p=new PointD(x,y);
      if( rzoom==null ) return p;
      newx = (x - rzoom.x)*zoom;
      newy = (y - rzoom.y)*zoom;

      // Petite correction due � une �ventuelle marge sur le bord
      // de l'image (pixel partiel)
      if( this.dx>=0 ) newx-=ddx;
      else newx++;
      if( this.dy>=0 ) newy-=ddy;
      else newy++;

      p.x=newx; p.y=newy;
      return p;
   }

   /** retourne true si le point p en coordonn�es de la vue se trouve
    * effectivement dans la vue (affichable) */
   protected boolean inside(PointD p) {
      return p.x>=0 && p.y>=0 && p.x<rv.width && p.y<rv.height;
   }

   /** Convertion de coordonnees.
    * @param p null si allocation � faire, sinon on utilise p
    * @param x,y Point dans l'image courante
    * @param dw,dh Marges de tolerance pour les tests sur l'affichage
    * @return  Retourne la position dans les coordonnees de la vue (View Frame)
    *          ou <I>null</I> si le point calcule est en dehors de l'espace
    *          affichable en tenant compte des marge de tolerance
    */
   protected Point getViewCoordWithMarge(double x, double y, int dw, int dh) {
      return getViewCoordWithMarge(null,x,y,dw,dh);
   }
   protected Point getViewCoordWithMarge(Point p,double x, double y, int dw, int dh) {
      p = getViewCoord(p,x,y);
      if( p==null || p.x<-dw || p.x>rv.width+dw ||
            p.y<-dh || p.y>rv.height+dh ) return null;
      return p;
   }

   protected PointD getViewCoordDoubleWithMarge(PointD p,double x, double y, int dw, int dh) {
      p = getViewCoordDble(p,x,y);
      if( p.x<-dw || p.x>rv.width+dw ||
            p.y<-dh || p.y>rv.height+dh ) return null;
      return p;
   }

   /* Changement de taille */
   protected void setDimension(int w,int h) {
      resize(w,h);
      rv = new Rectangle(w,h);
   }

   //   public int getWidth() { return rv.width; }
   //   public int getHeight() { return rv.height; }

   /** Ajustement automatique du zoom en fonction d'une �ventuelle modification
    *  de la taille du panel englobant la vue
    *  @return true si le facteur de zoom a �t� modifi�
    */
   protected void adjustZoom() {
      if( this.rzoom==null || isFree() ) return;

      // M�morisation des param�tres courants du zoom
      double pwidth = rzoom.width;
      double pheight = rzoom.height;
      double z = zoom;
      double x = xzoomView;
      double y = yzoomView;

      // Calcul des param�tres du zoom (�ventuellement ajust� si le panel
      // de la vue a �t� modifi�
      RectangleD rzoom=setZoomXY(z,x,y,false);
      if( rzoom==null ) return;

      // Determination du facteur d'accroissement
      double fct = Math.max( rzoom.width/pwidth, rzoom.height/pheight );

      // D�termination du facteur de zoom le plus adapt� pour conserver
      // la m�me portion de l'image visible
      double nz = fct==1 ? z : aladin.calque.zoom.getNearestZoomFct(fct*z);

      // Recalcul du zoom avec le nouveau facteur
      // N�cessaire m�me si nz==z pour �ventuellement recentrer l'image
      setZoomXY(nz,x,y);

      // Postionnement du s�lecteur de Zoom � la valeur calcul�e
      if( this==aladin.view.getCurrentView() ) aladin.calque.zoom.setValue(nz);
   }

   /**
    * Changement du facteur du zoom afin de couvrir un champ de vue.
    * Ne fonctionne que pour les vues ayant un plan de r�f�rence avec une
    * calibration astrom�trique.
    * @param radius taille souhait� du champ de vue en degr�s
    * @param width largeur en pixel de la vue pr�vue
    */
   protected void setZoomByRadius(double radius,int width) {
      Projection proj = getProj();
      if( proj==null ) return;  // Pas possible, pas de calibration
      double pixelSize = proj.c.getImgWidth()/proj.c.getImgSize().width;
      double nbPixel = radius/pixelSize;
      double fctZoom = width/nbPixel;
      double fct = aladin.calque.zoom.getNearestZoomFct(fctZoom);
      if( fct==zoom ) return;       // Ca ne change pas
      setZoomXY(fct,this.xzoomView,this.yzoomView);
   }

   /** Scrolling ou Rotation par un clic and drag souris */
   protected void scroll(MouseEvent e) {
      if( locked ) return;
      int x = e.getX();
      int y = e.getY();

      Plan pref = getProjSyncView().pref;

      int dxView = x-scrollX;
      int dyView = y-scrollY;

      // Rotation libre soit en appuyant CTRL, soit en pivotant la rose des vents
      int W=getNESize();
      boolean inNE = inNE(x,y);
      if(  inNE || e.isControlDown() && pref instanceof PlanBG ) {
         int xc = inNE ? rv.width-W/2 : rv.width/2;;
         int yc = inNE ? rv.height-W/2 : rv.height/2;
         double a1 = Math.atan2(scrollY-yc,scrollX-xc);
         double a2 = Math.atan2(y-yc,x-xc);
         getProj().deltaProjRot( Math.toDegrees( a1-a2 ) );
         flagMoveRepere=false;
         aladin.view.newView(1);
         aladin.view.repaintAll();
         return;
      }

      // D�placement par changement de centre de projection
      if( pref instanceof PlanBG && !(e.isControlDown() || getTaille()<30)  ) {

         Point ps = getPosition(scrollX, scrollY);
         Point pt = getPosition(x, y);

         Projection proj = getProj();
         Projection proj1 = proj.copy();
         proj1.frame=0;
         proj1.setProjCenter(90, 0);
         Coord cs = new Coord(90,0);
         proj1.getXY(cs);
         if( Double.isNaN(cs.x) ) return;
         Coord ct = new Coord();
         ct.x = cs.x+(pt.x-ps.x);
         ct.y = cs.y+(pt.y-ps.y);
         proj1.getCoord(ct);
         if( Double.isNaN(ct.al) ) return;
         double deltaRa = cs.al-ct.al;
         double deltaDe = cs.del-ct.del;
         //System.out.println("Changement de centre delta="+Coord.getUnit(deltaRa)+","+Coord.getUnit(deltaDe)  );
         proj.deltaProjCenter(deltaRa,deltaDe);

         aladin.view.newView(1);
         aladin.view.setRepere(proj.getProjCenter());
      }

      if( pref instanceof PlanBG && aladin.view.crop!=null ) aladin.view.crop.deltaXY(dxView/zoom,dyView/zoom);

      if( e!=null ) {
         if( Math.abs(dxView)>Math.abs(dxScroll) || dxView*dxScroll<0 ) dxScroll = dxView;
         if( Math.abs(dyView)>Math.abs(dyScroll) || dyView*dyScroll<0 ) dyScroll = dyView;
      }

      PointD delta = getDeltaZoomCoord(dxView,dyView);
      aladin.calque.zoom.zoomView.drawInViewNow(xzoomView-delta.x,yzoomView-delta.y);
   }

   /** Scrolling par un clic and drag souris */
   protected void scroll(MouseEvent e,double dxView, double dyView) {
      if( e!=null ) {
         if( Math.abs(dxView)>Math.abs(dxScroll) || dxView*dxScroll<0 ) dxScroll = dxView;
         if( Math.abs(dyView)>Math.abs(dyScroll) || dyView*dyScroll<0 ) dyScroll = dyView;
      }

      if( pref instanceof PlanBG
            && !(e.isControlDown() || e.isShiftDown())) {
         double taille = getPixelSize()*1.5;
         double deltaRa=dxView*taille, deltaDec=dyView*taille;
         getProj().deltaProjCenter(deltaRa,deltaDec);
         aladin.view.newView(1);
         if( aladin.view.crop!=null ) aladin.view.crop.deltaXY(dxView/zoom,dyView/zoom);
         aladin.view.repaintAll();
         return;
      }

      PointD delta = getDeltaZoomCoord(dxView,dyView);
      aladin.calque.zoom.zoomView.drawInViewNow(xzoomView-delta.x,yzoomView-delta.y);
   }

   protected void scrollA(double dxView, double dyView) {
      if( Math.abs(dxView)>Math.abs(dxScroll) || dxView*dxScroll<0 ) dxScroll = dxView;
      if( Math.abs(dyView)>Math.abs(dyScroll) || dyView*dyScroll<0 ) dyScroll = dyView;
      PointD delta = getDeltaZoomCoord(dxView,dyView);
      aladin.calque.zoom.zoomView.drawInViewNow(xzoomView-delta.x,yzoomView-delta.y);
   }

   /** Active l'�x�cution du scrolling automatique si n�cessaire */
   synchronized private void startAutomaticScroll() {
      scrollCont=isScrolling();
   }

   /** Arr�te le scrolling automatique */
   protected  void stopAutomaticScroll() {
      aladin.calque.zoom.zoomView.stopDrag();
      dxScroll=dyScroll=0;
      scrollCont=false;
      xViewGoal=-1;
   }

   protected void goToAllSky(Coord c) {
      aladin.view.gotoThere(c,0,true);
      //      aladin.view.setRepere(c);
      //      aladin.view.showSource();
   }

   static int STEP = 2;
   protected void goTo(double xImg,double yImg) {
      PointD p = imageToZoomView(xImg, yImg);
      goTo(p);
   }
   protected void goTo(PointD p) {
      double cx = rzoomView.x;
      double cy = rzoomView.y;
      double dx = (cx-p.x)/STEP;
      double dy = (cy-p.y)/STEP;
      setScrollable(true);
      initScroll();
      xViewGoal = p.x;
      yViewGoal = p.y;
      scrollA(dx,dy);
      startAutomaticScroll();
   }

   private double dxScroll,dyScroll;   // Offset du scrolling automatique
   private boolean scrollCont=false; // Le scrolling automatique peut �tre activ�
   private double xViewScroll,yViewScroll;    // Pr�c�dente position du scrolling automatique
   private boolean wasScrolling=false; // Etait en scroll automatique
   private long lastWhenDrag=0L;   // Date du dernier mouseDrag
   double xViewGoal=-1,yViewGoal;     // But d'un scroll goto

   /** Retourne true si le scrolling automatique a un offset non nul */
   protected boolean isScrolling() {
      return dxScroll!=0 || dyScroll!=0;
   }

   /** Op�re une it�ration du scrolling automatique */
   synchronized protected void scrolling() {
      try {
         if( !scrollCont ) return;


         // Test d'arriv�e au but */
         if( xViewGoal!=-1 && Math.abs(xViewGoal-rzoomView.x)<1
               && Math.abs(yViewGoal-rzoomView.y)<1 ) {
            stopAutomaticScroll();
         }

         // Test de but�e sur un bord => arr�te le scrolling
         else if( xViewScroll==rzoomView.x && dxScroll!=0 || yViewScroll==rzoomView.y && dyScroll!=0 ) {
            stopAutomaticScroll();

            // Scrolling + m�morisation de la position courante
         } else {
            xViewScroll=rzoomView.x; yViewScroll=rzoomView.y;
            scroll(null,dxScroll,dyScroll);
         }
      } catch( Exception e ) {}
   }


   /** Calcul et m�morisation �ventuelle du zoom courant
    *  @param zoom facteur de zoom
    *  @param xc,yc position centrale (demand�e) en coordonn�es XY de l'imagette du zoom
    *  @param memorisation true si on doit m�moriser des infos (voir ci-dessous)
    *  xzoom et yzoom peuvent ne pas correspondre � ce qui est demand� s'il y
    *  d�bordement et donc recentrage. Si -1,-1, prendra le centre de l'imagette
    *  si memorisation==true met � jour:
    *             rzoom: rectangle du zoom dans les coordonn�es image
    *             rzoomView: rectangle du zoom dans les coordonn�es du ZoomView
    *             zoom: le facteur courant du zoom
    *             xzoomView: le centre de la vue dans les coordonn�es du ZoomView;
    *             yzoomView: idem pour l'ordonn�e;
    *             HView: la hauteur en pixel de la vue;
    *             WView: la largeur en pixel de la vue;
    * @return si memorisation==false, retourne simplement le rzoom calcul�
    */
   protected void setZoomXY(double zoom,double xc,double yc) { setZoomXY(zoom,xc,yc,true); }
   synchronized protected RectangleD setZoomXY(double zoom,double xc,double yc,
         boolean memorisation) {
      if( zoom==0 ) return null;
      double imgW,imgH;   // Taille de l'image de base

      // Recherche de la taille de l'image (ou de la projection si catalogue)
      Dimension d = getPRefDimension();
      imgW=d.width;
      imgH=d.height;
      
 
      // Calcul de la proportion (si l'image n'est pas carree)
      double W,H;
      W = aladin.calque.zoom.zoomView.getWidth();
      H = (W/imgW)*imgH;
      if( H>W ) {
         W = W*W /H;
         H = aladin.calque.zoom.zoomView.getHeight();
      }

      // On part sur la position centrale
      if( xc==-1 ) xc=W/2;
      if( yc==-1 ) yc=H/2;

      // Determination de la taille du rectangle de zoom
      double dW = ((W/imgW)*rv.width )/zoom;
      double dH = ((H/imgH)*rv.height )/zoom;

      // Memorisation du rectangle du zoom dans les coord. de l'image courante
      double xzImg = (imgW/W)*xc;
      double yzImg = (imgH/H)*yc;
      double wzImg = rv.width/zoom;
      double hzImg = rv.height/zoom;
      
      // Avec m�morisation des infos
      if( memorisation ) {

         // M�morisation diverses
         this.rzoom = new RectangleD(xzImg-wzImg/2,yzImg-hzImg/2, wzImg,hzImg);
         this.rzoomView = new RectangleD(xc-dW/2,yc-dH/2,dW,dH);
         this.zoom=zoom;
         this.xzoomView=xc;
         this.yzoomView=yc;
         this.HView = H;
         this.WView = W;

         return rzoom;
      }

      // Sans m�morisation
      return new RectangleD(xzImg-wzImg/2,yzImg-hzImg/2, wzImg,hzImg);
   }

   /** Positionne le plan de r�f�rence de la vue
    *  en ajustant les param�tres du zoom pour que la portion visible
    *  corresponde au facteur de zoom initial indiqu� dans le plan.
    */
   protected void setPlanRef(Plan p,boolean withZoomView) {
      //System.out.println("setPlanRef("+p.label+") pour "+this);
      boolean flagMemo = !isFree();

      // Sauvegarde des valeurs du zoom dans le plan afin de pouvoir y revenir
      if( flagMemo ) pref.memoInfoZoom(this);

      // Changement de plan de r�f�rence
      if( pref!=p ) {
         pref=p;

         // Affectation si n�cessaire d'une projection locale
         projLocal = pref instanceof PlanBG ? pref.projd.copy() : null;
      }

      if( pref instanceof PlanBG ) northUp=false;

      if( !p.isCatalog() && isPlotView() ) { plot.free(); plot=null; }

      // Cr�ation du controleur de blink s'il s'agit d'un cube
      if( pref.isCube() ) {
         cubeControl = new CubeControl(this,pref,pref.getInitDelay(),pref.isPause());
         cubeControl.setFrameLevel(pref.getZ(),false);
         cubeControl.resume();
      }
      else cubeControl=null;

      // Tentative de r�cup�ration de valeurs de zoom pr�c�demment sauvegard�es
      // pour ce plan dans le cas du mode MVIEW1
      if( flagMemo && pref.initInfoZoom(this) ) {
         setZoomXY(zoom,xzoomView,yzoomView);

         // On va prendre le zoom initial
      } else reInitZoom();

      // Test pour d�sactivation des plans images qui cacheraient
      //      unActivateCoveringPlan();

      if( withZoomView ) aladin.calque.zoom.reset();

      //      aladin.view.memoUndo(this, null, null);

      // Maintenant qu'on affiche syst�matiquement la valeur du pixel
      reloadPixelsOriginIfRequired();
   }

   /** Ajustement du zoom en fonction de la valeur du zoom
    * initial (pref.initZoom) et de la taille actuelle de la vue
    * afin de voir toute (initZoom=1) l'image
    * Met � jour zoom et rzoom en fonction
    */
   protected void reInitZoom() { reInitZoom(false); }
   protected void reInitZoom(boolean flagRepaint) {

      if( isPlotView() ) plot.adjustPlot(null);
      else {

         // Positionnement des valeurs du zoom initial
         zoom=pref.initZoom;
         setZoomXY(zoom,-1,-1);

         // Ajustement afin que l'on voit toute l'image (initzoom=1)
         // ou le 1/4 de l'image (initzoom=2), etc...
         Dimension d=getPRefDimension();
         double w = d.width/zoom;
         double h = d.height/zoom;
         double x = (d.width - w)/2;
         double y = (d.height - h)/2;
         rzoom=new RectangleD(x,y,w,h);

         adjustZoom();
      }
      if( flagRepaint ) {
         repaint();
         aladin.calque.zoom.reset();
      }
   }

   // Pour �viter des allocations inutiles
   private final Coord coo_x = new Coord();

   /** Positionne le champ sur la coordonn�e pass�e en param�tre
    *  @return true si c'est possible, false sinon
    */
   protected boolean setCenter(double ra,double dec) {
      if( pref instanceof PlanBG ) projLocal.setProjCenter(ra, dec);
      return setZoomRaDec(zoom,ra,dec);
   }

   /** Positionne le champ sur la source indiqu�e en tenant compte du nouveau
    * facteur de zoom
    *  @return true si c'est possible, false sinon
    */
   protected boolean setZoomSource(double nzoom,Source s) {
      if( !isPlotView() ) return setZoomRaDec(nzoom,s.raj,s.dej);

      double [] val = plot.getValues(s);
      return setZoomRaDec(nzoom,val[0],val[1]);
   }

   /** Positionne le champ sur la coordonn�e pass�e en param�tre
    *  en tenant compte du nouveau facteur de zoom
    *  @return true si c'est possible, false sinon
    */
   synchronized protected boolean setZoomRaDec(double nzoom,double ra,double dec) {
      if( isFree()  ) return false;
      Projection proj = getProj();
      if( !Projection.isOk(proj) ) return false;
      try {
         coo_x.al=ra; coo_x.del=dec;
         proj.getXY(coo_x);
         if( Double.isNaN(coo_x.x) ) return false;

         PointD p = imageToZoomView(coo_x.x,coo_x.y);
         setZoomXY(nzoom==0?zoom:nzoom,p.x,p.y);
         newView();
      } catch (Exception e) { return false; }
      return true;
   }

   /** Zoom la vue sur la source pass�e en param�tre */
   protected void zoomOnSource(Source o) {
      setZoomSource(isPlotView() ? 2 : 16,o);
      repaint();
   }

   /** Retourne true si on doit changer le champ de vue pour visualiser la coordonn�e,
    * c�d si la coordonn�e se trouve dans l'image de la vue mais non visible et que
    * le bouton SELECT est appuy� */
   boolean shouldMove(double ra, double dec) {
      if( pref!=null && pref instanceof PlanBG ) {
         return selected && !isInView(ra, dec);
         //         return selected || !aladin.view.isMultiView();
      }
      if(  isFree() ||
            (aladin.toolBox.getTool()!=ToolBox.SELECT && aladin.toolBox.getTool()!=ToolBox.PAN)
            || aladin.calque.zoom.zoomView.flagdrag) return false;
      return isInImage(ra,dec);
   }

   /** Retourne true si la coordonn�e est dans l'image (avec de la marge) */
   boolean isInImage(double ra, double dec) {
      try {
         coo_x.al=ra; coo_x.del=dec;
         getProj().getXY(coo_x);
         if( Double.isNaN(coo_x.x) ) return false;
         PointD p = imageToZoomView(coo_x.x,coo_x.y);
         return p.x>=-WView/2 && p.x<=3*WView/2 && p.y>=-HView/2 && p.y<=3*HView/2;
      } catch (Exception e) {}
      return false;
   }

   /** Retourne true si la coordonn�e est visible dans la vue */
   boolean isInView(double ra, double dec) { return isInView(ra,dec,0); }
   boolean isInView(double ra, double dec,int marge) {
      try {
         coo_x.al=ra; coo_x.del=dec;
         getProj().getXY(coo_x);
         if( Double.isNaN(coo_x.x) ) return false;
         PointD p = getViewCoordDble(coo_x.x,coo_x.y);
         return p.x>=-marge && p.x<rv.width+marge && p.y>=-marge && p.y<rv.height+marge;
      } catch (Exception e) {}
      return false;

   }

   /** Recuperation de l'image courante de la vue
    * @param w largeur de l'image � r�cup�rer (uniquement en mode nogui)
    * @param h hauteur de l'image � r�cup�rer (uniquement en mode nogui)
    */
   protected Image getImage(int w, int h) { return getImage(w,h,true); }
   protected Image getImage(int w, int h, boolean withOverlays) {

      // Si on est en mode script, il faut creer manuellement l'image
      // de la vue. La taille sera fonction de l'image de base
      //      if( Aladin.NOGUI ) {
      //         if( isFree() ) return null;
      //         if( w==-1 || h==-1 ) {
      //            setDimension(((PlanImage)pref).width,((PlanImage)pref).height);
      //            setZoomXY(1, -1, -1);
      //         } else setDimension(w,h);
      //      }
      BufferedImage img = new BufferedImage(rv.width, rv.height,
            pref.isImage() && ((PlanImage)pref).isTransparent() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
      Graphics2D g = (Graphics2D)img.getGraphics();
      if( aladin.NOGUI ) {
         PlanImage pi = (PlanImage)( (!isFree() && pref.isImage() ) ? pref : null );
         getImgView(g,pi);

      }
      drawBackground(g);

      // Tout ?
      if( withOverlays ) {
         paintOverlays(g,null,0,0,true);
         drawCredit(g, 0, 0);

         // ou uniquement les images
      } else paintOverlays(g,null,0,0,true,0x1);

      aladin.waitImage(img);
      //      System.out.println("ViewSimple.getImage("+w+","+h+") => paintOverlays done on "+img);
      //      if( aladin.NOGUI ) aladin.command.syncNeedRepaint=false;
      return img;
   }

   //   protected Image getImage(int w,int h) {
   //
   //      aladin.calque.flagCredit = true;
   //
   //      img=null;
   //
   //      // Si on est en mode script, il faut creer manuellement l'image
   //      // de la vue. La taille sera fonction de l'image de base
   //      if( Aladin.NOGUI ) {
   //         if( isFree() ) return null;
   //         if( w==-1 || h==-1 ) {
   //            setDimension(((PlanImage)pref).width,((PlanImage)pref).height);
   //            setZoomXY(1, -1, -1);
   //         } else setDimension(w,h);
   //      }
   //      paintComponent(null);
   //      aladin.calque.flagCredit = false;
   //
   //      aladin.waitImage(img);
   //      return img;
   //   }

   //   private boolean flagLockRepaint=false;   // voir waitLockReapaint() et unlockRepaint()
   //   private Object lockRepaint = new Object();
   //   private int threadIDlock=0;             // ID du thread qui dispose du lock
   //
   //   protected void unlockRepaint(String orig) {
   //      if( true ) return;
   //      aladin.trace(4,"ViewSimple.unlockRepaint("+orig+")...");
   //      synchronized( lockRepaint ) {
   //         flagLockRepaint=false;
   //         threadIDlock=0;
   //      }
   //   }
   //
   //   // Si l'attente du lock du repaint d�passe 8 secondes, on lib�re le lock
   //   static final int ITSTIME = 8000;
   //
   //   /** Attente du lock pour effectuer un paintComponent()
   //    * Si le thread dispose d�j� du lock, retourne imm�diatement (�vite un deadlock)
   //    * @param orig Origine de l'appel (pour debugging)
   //    */
   //   protected void waitLockRepaint(String orig) {
   //      long t1 = System.currentTimeMillis();
   //      while( true )  {
   //         try {
   //            synchronized( lockRepaint ) {
   //               int threadID = Thread.currentThread().hashCode();
   //               if( threadIDlock==threadID ) {
   //                  System.out.println("waitLockRepaint() m�me threadID="+threadID+" => "+Thread.currentThread().getName());
   //                  return;
   //               }
   //               if( !flagLockRepaint ) {
   //                  threadIDlock=threadID;
   //                  flagLockRepaint=true;
   //                  aladin.trace(4,Thread.currentThread().getName()+": ViewSimple.waitLockRepaint("+orig+") yes ! by "+threadID);
   //                  return;
   //               }
   //            }
   //            Util.pause(50);
   //            if( System.currentTimeMillis()-t1 > ITSTIME ) {
   //               System.err.println(Thread.currentThread().getName()+": ViewSimple.getLockRepaint("+orig+") too long... force unlock !");
   //               flagLockRepaint=false;
   //               threadIDlock=0;
   //               return;
   //            }
   //            aladin.trace(4,"ViewSimple.waitLockRepaint("+orig+") waiting...");
   //         } catch( Exception e ) { if( aladin.levelTrace>=3  ) e.printStackTrace(); }
   //      }
   //   }

   /** Force la regeneration de la vue (projections comprises)
    * si methode==1, force le recalcul des projections, -1 uniquement les projections
    */
   protected void newView() { newView(0); }
   protected void newView(int methode) {
      if( methode==1 ) {
         Plan [] allPlans = calque.getPlans();
         for (int i = 0; i < allPlans.length; i++) {
            if( !allPlans[i].flagOk ) continue;
            //System.out.println("J'invalide la projection du plan "+i+
            //                   " ("+allPlans[i]+") de la vue "+this);
            allPlans[i].resetProj(n);
         }
      }
      if( methode!=-1 ) iz++;
   }

   /** Retourne la couleur du premier plan selectionne */
   protected Color getColor() {
      Plan pc = calque.getFirstSelectedPlan();
      if( pc==null || pc.type==Plan.NO ) return Color.black;
      return pc.c;
   }

   /** Extension du clip courant � un rectangle */
   protected void extendClip(Rectangle r) {
      if( r!=null ) clip=Obj.unionRect(clip,r.x,r.y,r.width,r.height);
   }

   /** Extension du clip courant � un objet */
   protected void extendClip(Obj o) {
      clip = o.extendClip(getProjSyncView(),clip);
   }

   /** Extension du clip � une liste d'objets */
   protected void extendClip(Vector v) {
      if( v==null ) return;
      Enumeration e = v.elements();
      while( e.hasMoreElements()) {
         clip = ((Obj)e.nextElement()).extendClip(this,clip);
      }
   }

   /** Reset du clip (force � null) */
   protected void resetClip() {
      clip=null;
   }

   /** Positionnement du clip sur le contect graphics pass� en param�tre */
   private void setClip(Graphics g) { setClip(g,false); }
   private void setClip(Graphics g,boolean debug) {
      if( clip!=null ) {
         g.clipRect(clip.x-1,clip.y-1,clip.width+2,clip.height+2);

         if( !debug ) return;
         g.setColor(Color.magenta);
         g.drawRect(clip.x,clip.y,clip.width,clip.height);

      }
   }

   /** Fin de saisie d'un objet */
   protected void finNewObjet() {
      if( view.newobj==null ) return;
      calque.setObjet(view.newobj);
      if( view.newobj instanceof Ligne && !(view.newobj instanceof Cote) ) ((Ligne)view.newobj).getFirstBout().bout=0;
      if( view.newobj instanceof Tag ) {
      }
      view.extendClip(view.newobj);
      aladin.console.printCommand(view.newobj.getCommand());
      view.setSelectFromView(true);

      view.moveRepere( view.newobj.raj,view.newobj.dej );

      view.newobj=null;
      aladin.calque.repaintAll();
   }

   /** Ajoute l'objet en cours de saisie dans le plan selectionne */
   protected void setNewObjet() {
      //      setNewObjet(false);
      //      }
      //   protected void setNewObjet(boolean withForCDSTeam) {
      if( view.newobj==null ) return;
      calque.setObjet(view.newobj);
      view.extendClip(view.newobj);
      //      if( withForCDSTeam ) createCoteDist(aladin.view.vselobj);
   }

   // Calcul du nouveau selecteur multiple en fonction de la souris
   // et positionnement du clipRect en fct
   void extendSelect(int x, int y) {
      //      extendClip(rselect);
      rselect = new Rectangle(
            Math.min((int)Math.round(fixev.x),x),
            Math.min((int)Math.round(fixev.y),y),
            Math.abs((int)Math.round(fixev.x)+1-x),
            Math.abs((int)Math.round(fixev.y)+1-y) );
      //      extendClip(rselect);
   }

   /** Retourne l'etat du mode GrabIt */
   protected boolean isGrabIt() {
      return aladin.dialog==null?false:aladin.dialog.isGrabIt();
   }

   /**
    * Arrete le GrabIt Courant
    */
   protected void stopGrabIt() {
      if( !isGrabIt() ) return;
      pGrabItX=-1;
      aladin.dialog.stopGrabIt();
      aladin.view.repaintAll();
   }

   private Position poignee=null;   // poign�e d'une rotation en cours (apr�s un mouseDown)
   private Repere poigneePhot=null;  // poign�e d'une extension de rep�re circulaire en cours (apr�s un mouseDown)
   private Tag poigneeTag=null; // poign�e d'un changement d'ancrage pour un tag/label (apr�s un mouseDown)

   /** teste si dans a liste des objet qui ont �t� s�lectionn� par la souris, parmi ceux qui
    * sont pr�cis�ment sous la souris il n'y aurait peut �tre pas un objet d'un planField rollable.
    * Si c'est le cas, m�morise cet objet comme la "poign�e de rotation
    * et force la s�lection courante aux seuls objets de son Plan d'appartenance
    * @param v liste des objets � tester
    */
   private Position testFoVRollable(ViewSimple vs,Vector v,double x,double y) {
      Position poignee=null;
      Enumeration e = v.elements();
      if( e.hasMoreElements() ) e.nextElement();    // On saute le centre de rotation FoV (premier objet de type Repere)
      if( e.hasMoreElements() ) e.nextElement();    // On saute le centre de proj FoV (2eme objet de type Repere)
      while( e.hasMoreElements() ) {
         Position o = (Position)e.nextElement();
         if( o.plan instanceof PlanField
               && o.plan.active
               && ((PlanField)o.plan).isRollable()
               && o.inBout(vs,x,y) ) { poignee = o; break; }
      }
      if( poignee==null || poignee.plan==null ) return null;

      // On s�lectionne tous les objets du plan
      aladin.view.selectObjetPlanField(poignee.plan);

      return poignee;
   }

   /** Teste si l'objet sous la souris est un Repere circulaire, non seulement s�lectionn�
    * mais pour lequel la position de la souris se trouve sur une des 4 poign�es d'extensions (bas,haut,droite,gauche)
    * Si c'est le cas, m�morise cet objet dans poigneeRepere pour permettre sa manipulation via mouseDrag */
   private Repere testPhot(ViewSimple vs,double x,double y,boolean withPoignee) {
      Enumeration e = aladin.view.getSelectedObjet().elements();
      while( e.hasMoreElements() ) {
         Obj o = (Obj)e.nextElement();
         if( !(o instanceof Repere) ) continue;
         if( !o.plan.isMovable() ) continue;
         if( o instanceof Position && ((Position)o).plan.type == Plan.APERTURE ) continue;
         Repere t = (Repere)o;
         if( !t.isSelected() || !t.hasRayon() ) continue;
         if( withPoignee ) { if( !t.onPoignee(vs, x, y) ) continue; }
         else { if( !t.inside(vs,x,y) ) continue; }
         return t;
      }
      return null;
   }


   /** Teste si l'objet sous la souris est un Tag/label, non seulement s�lectionn� (ou en cours d'�dition)
    * mais pour lequel la position de la souris correspond � une action avec la molette de la souris */
   private Tag testWheelTag(double x,double y) {
      Enumeration e = aladin.view.getSelectedObjet().elements();
      while( e.hasMoreElements() ) {
         Obj o = (Obj)e.nextElement();
         if( !(o instanceof Tag) ) continue;
         if( o instanceof Position && ((Position)o).plan.type == Plan.APERTURE ) continue;
         Tag t = (Tag)o;
         if( !t.isSelected() ) continue;
         if( !t.onViaWheel(this, x, y) ) continue;
         return t;
      }
      return null;
   }

   /** Teste si l'objet sous la souris est un Tag/label, non seulement s�lectionn�
    * mais pour lequel la position de la souris se trouve sur un �lement extensible (poign�e d'ancrage, coin)
    * Si c'est le cas, m�morise cet objet dans poigneeTag pour permettre sa manipulation via mouseDrag */
   private Tag testPoigneeTag(double x,double y) {
      Enumeration e = aladin.view.getSelectedObjet().elements();
      while( e.hasMoreElements() ) {
         Obj o = (Obj)e.nextElement();
         if( !(o instanceof Tag) ) continue;
         if( !o.plan.isMovable() ) continue;
         if( o instanceof Position && ((Position)o).plan.type == Plan.APERTURE ) continue;
         Tag t = (Tag)o;
         if( !t.isSelected() ) continue;
         if( !t.onViaMouse(this, x, y) ) continue;
         return t;
      }
      return null;
   }

   /** Teste si l'objet sous la souris est un Tag/label, non seulement s�lectionn�
    * mais pour lequel la position de la souris se trouve sur le label
    * Si c'est le cas passe ce Tag en mode �dition */
   private boolean testEditTag(double x,double y) {
      Enumeration e = aladin.view.getSelectedObjet().elements();
      while( e.hasMoreElements() ) {
         Obj o = (Obj)e.nextElement();
         if( !(o instanceof Tag) ) continue;
         if( o instanceof Position && ((Position)o).plan.type == Plan.APERTURE ) continue;
         Tag t = (Tag)o;
         if( !t.isSelected() ) continue;
         if( !t.onLabel(this, x, y) && !t.onTag(this,x,y) ) continue;
         aladin.view.vselobj.remove(t);
         t.plan.pcat.delObjet(t);
         t.setEditing(true);
         view.newobj=t;
         return true;
      }
      return false;
   }

   // true si la vue est entrain d'�tre scroll� par clic-and-drag
   protected boolean flagScrolling=false;

   protected void setScrollable(boolean flag ) {
      // Il faut n�cessairement un repaint forcc� pour un planBG
      if( !flag && flagScrolling && pref!=null && pref.type==Plan.ALLSKYIMG) {
         newView(); repaint();
      }
      flagScrolling=flag;
   }

   /** Retourne le Tool courant avec la possibilit� de passer directement
    * en PAN avec la touche ALT enfonc�e    */
   private int getTool(MouseEvent e) {
      int tool = aladin.toolBox.getTool();
      if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 || e.isAltDown() ) tool=ToolBox.PAN;

      if( tool==ToolBox.SELECT && !Aladin.OUTREACH && !aladin.calque.hasSelectableObjects() ) {

         // En multiview, on garde la possibilit� de d�placer les vues en attrapant
         // leur bord
         if( aladin.view.isMultiView() ) {
            int x = e.getX();
            int y = e.getY();
            boolean closeBorder = x<10 || x>getWidth()-10 || y<10 || y>getHeight()-10;
            if( !closeBorder ) return ToolBox.PAN;
         } else return ToolBox.PAN;
      }
      return tool;
   }

   public void mouseClicked(MouseEvent e) {}

   public void mousePressed(MouseEvent e) {
      if( isFullScreen() && widgetControl!=null && widgetControl.mousePressed(e) ) {
         repaint(); return;
      }

      mousePressed1(e.getX(),e.getY(),e);
   }

   private int xDrag=-1,yDrag=-1;
   private boolean rainbowUsed=false;

   protected void mousePressed1(double x, double y,MouseEvent e) {

      // Au cas o�
      aladin.view.resetMegaDrag();

      // Synchronisation sur une autre vue ?
      ViewSimple vs = getProjSyncView();
      boolean isProjSync = isProjSync();
      boolean fullScreen = isFullScreen();

      // Pour prendre en compte un ajustement de l'histogramme directement par d�placement
      // de la souris dans la vue (� la DS9)
      if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) {
         xDrag=e.getX();
         yDrag=e.getY();
      }

      boolean boutonDroit = (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 && fullScreen;
      int tool = getTool(e);
      boolean flagshift = e.isShiftDown() ;

      if( tool==ToolBox.ZOOM ) {
         vs.flagDrag=false;
         /* orselect =*/ rselect = null;
      }

      // On va ouvrir le Popup sur le mouseUp()
      if( boutonDroit ) return;

      aladin.calque.zoom.zoomView.stopDrag();

      // Gestion de l'outil de rainbow pixel
      rainbowUsed = rainbowUsed();
      if( hasRainbow() && rainbow.isInside(x,y) ) {
         if( !rainbow.submit(vs) ) {
            rainbow.reset();
            rainbow.startDrag(x,y);
         }
         repaint();
      }

      // Gestion de l'outil de rainbow filter
      if( rainbowF!=null && rainbowF.isInside(x,y) ) {
         if( !rainbowF.submit(vs) ) {
            rainbowF.reset();
            rainbowF.startDrag(x,y);
         }
         repaint();
      }
      if( rainbowUsed ) return;

      // Gestion de l'outil de cropping
      if( tool==ToolBox.CROP ) {
         if( view.crop==null ) view.crop = new CropTool(aladin, vs, aladin.calque.getFirstSelectedPlan(),
               x, y, pref instanceof PlanBG && pref.hasAvailablePixels());
         else {
            if( view.crop.submit(vs,x,y) ) {
               view.crop=null;
               aladin.toolBox.setMode(ToolBox.CROP, Tool.UP);
            }
            else {
               view.crop.reset();
               view.crop.startDrag(vs,x,y);
            }
         }
         repaint();
         return;
      }

      if( tool==ToolBox.SELECT  || tool==-1  || tool==ToolBox.PAN  || tool==ToolBox.CROP ) {

         if( !isProjSync ) {
            if( !flagshift && (!selected || !view.isMultiView()) ) {
               aladin.calque.unSelectAllPlan();
               aladin.view.unSelectAllView();
            }

            selected=flagshift?!selected:true;
            if( !isFree() ) pref.selected=selected;

            // S'il n'y a plus aucune vue/plan s�lectionn�, je res�lectionne la derni�re
            if( aladin.calque.getNbSelectedPlans()==0 ) {
               selected = true;
               if( !isFree() ) pref.selected=true;
            }

            // Affectation de la vue courante, ou recherche de la prochaine vue
            // courante parmi celles s�lectionn�es
            if( selected ) aladin.view.setCurrentView(this);
            else aladin.view.setCurrentView();

            // Pour savoir dans le cas d'un DEL si on a s�lectionn� la vue depuis ViewSimple
            aladin.view.setSelectFromView(true);

            aladin.calque.repaintAll();
         }

         // Pour pouvoir changer la Colormap associ�e � cette vue m�me en mode synchronis�
         if( selected && aladin.match.isProjSync()  ) {
            aladin.view.setLastClickView(this);
            aladin.calque.repaintAll();
         }

         // Mise � jour du CutGraph courant si il existe
         Obj c = aladin.view.zoomview.getObjCut();
         if( c!=null && !c.cutOn() ) aladin.view.zoomview.suspendCut();
      }

      // Si on est sur le blinkControl..
      if( isPlanBlink() && cubeControl.mousePressed(e)>CubeControl.NOTHING ) {
         flagCube=true;
         return;
      }
      //      int r;
      //      if( isPlanBlink()
      //            && (r=cubeControl.mousePressed((int)Math.round(x),(int)Math.round(y)))>CubeControl.NOTHING ) {
      //         flagCube=true;
      //         if( r==CubeControl.IN ) return;
      //         if( r==CubeControl.SLIDE )
      //            aladin.view.setCubeFrame(this, cubeControl.getFrameLevel((int)Math.round(x)),e.isShiftDown());
      //         aladin.view.repaintAll();
      //         return;
      //      }

      if( fullScreen && aladin.fullScreen.mousePressed(e) ) {
         flagMoveRepere=false;
         return;
      }

      // Calcul des coordonnees dans le plan de projection de l'image courante
      PointD p=null;
      Coord cs=null;
      p = vs.getPosition(x,y);
      fixev = new PointD(x,y);       // Pt fixe pour le selecteur multiple
      fixebis = fixe  = new PointD(p.x,p.y);   // Dans le cas ou il faut qu'on connaisse l'origine
      Projection proj = getProj();
      if( Projection.isOk(proj)) {
         cs = new Coord();
         cs.x=fixe.x;
         cs.y=fixe.y;
         proj.getCoord(cs);
         if( Double.isNaN(cs.al ) ) cs=null;
      }

      // Mode GrabIt actif
      if( isGrabIt() && !isFree() ) {
         aladin.dialog.setGrabItCoord(x,y);
         cGrabItX=pGrabItX=-1;
         grabItX=x; grabItY=y;
         modeGrabIt=true;
         aladin.view.repaintAll();
         return;
      }

      // Juste pour tester...
      //      fullScreen = !view.isMultiView() && calque.getNbPlanCat()==0;

      if( tool==ToolBox.SELECT || tool==ToolBox.PAN ) {
         // thomas
         // click en mode SELECT --> on marque les images disponibles dans le treeView
         vs.markAvailableImages(x,y,flagshift);

         // Dans le cas d'un Repere temporaire Simbad
         if( view.simRep!=null && view.simRep.inLabel(this, x, y) ) {
            view.showSimRep();
            flagSimRepClic=true;
            return;
         }
      }

      // Initialisation d'un clic-and-drag de la vue
      if( tool==ToolBox.PAN
            //            || (fullScreen && !flagOnMovableObj && tool==ToolBox.SELECT) ) {
            ) {
         vs.scrollX=(int)Math.round(x); vs.scrollY=(int)Math.round(y);
         setScrollable(true);
         wasScrolling = vs.isScrolling();
         vs.initScroll();
         //         if( !fullScreen ) return;
      }

      // Cr�ation automatique d'une vue associ� au plan Draw ?
      if( isFree() ) {
         Plan pc = aladin.calque.getFirstSelectedPlan();
         if( !aladin.toolBox.isForTool(tool) || pc==null || pc.type!=Plan.TOOL ) return/* false*/;
         //         System.out.println("J'affecte � la vue "+this+" le plan TOOL "+pc);
         aladin.calque.setPlanRef(pc,vs.n);
      }

      Enumeration en;

      // Si on a Control, on d�place uniquement le rep�re
      if( e.isControlDown() ) return;

      // JE L'AI DEPLACE DANS mouseRelease()
      //    boolean recalib = !isProjSync && aladin.view.isRecalibrating();
      //      // Recalibration dynamique en cours ?
      //      if( recalib ) {
      //         Vector v=null;
      //
      //         if( aladin.frameNewCalib.isGettingOriginalXY() ) v=aladin.frameNewCalib.plan.getObjWith(this,p.x,p.y);
      //         else v=calque.getObjWith(this,p.x,p.y);
      //
      //         Obj o = v!=null && v.size()>0?(Obj)v.elementAt(0):null;
      //         flagMoveRepere=false;
      //
      //         if( o==null || o instanceof Position ) {
      //
      //            // D�placement globale
      //            int method=aladin.frameNewCalib.getModeCalib();
      //            if( method==FrameNewCalib.SIMPLE ) {
      //               if( o!=null && ((Position)o).plan==aladin.frameNewCalib.plan ) {
      //                  planRecalibrating=aladin.frameNewCalib.plan;
      //                }
      //
      //            // Mise � jour de la liste des quadruplets
      //            } else if( method==FrameNewCalib.QUADRUPLET ) {
      //               aladin.frameNewCalib.mouse(p.x,p.y,(PlanImage)pref,(Position)o);
      //            }
      //         }
      //      }

      if( tool== -1 ) return;

      // Edition, modification d'un tag ?
      if( tool==ToolBox.SELECT || tool==ToolBox.TAG ) {
         // S'agirait-il d'un changement d'ancrage d'un texte/Label ?
         if( (poigneeTag = testPoigneeTag(p.x,p.y))!=null ) return;
         // S'agirait-il d'une �dition d'un texte/Label ?
         else if( e.getClickCount()==2 && testEditTag(p.x,p.y) )  return;
      }

      // Juste pour �viter de cr�er un deuxi�me PHOT sur le premier en ayant
      // oubli� de rebasculer en SELECT
      if( tool==ToolBox.PHOT && testPhot(vs,p.x,p.y,false)!=null ) {
         tool = ToolBox.SELECT;
         aladin.toolBox.setMode(ToolBox.PHOT,Tool.UP);
         aladin.toolBox.setMode(ToolBox.SELECT,Tool.DOWN);
      }

      // Dans le cas de l'outil de selection
      if( tool==ToolBox.SELECT ) {

         Vector<Obj> v = calque.getObjWith(vs,p.x,p.y);

         // Pour indiquer � un �ventuel drag qui suivrait qu'il faut d�placer les objets
         // et non �tirer un rectangle de s�lection
         flagMoveNotSelect = view.hasMovableObj(v);

         // Synchronisation des vues sur l'objet - PREVU POUR LES VIEWS PLOT, MAIS NE MARCHE PAS
         if( e.getClickCount()==2 && v.size()==1 && v.elementAt(0) instanceof Source ) {
            view.setRepere((Source)view.vselobj.elementAt(0));
            //            System.out.println("Zoom on "+(Source)view.vselobj.elementAt(0));
         }

         // Dans le cas du shift on merge les deux vecteurs
         if( e.isShiftDown() ) {

            // On recommence une selection multiple
            if( v.size()==0 ) {
               rselect = new Rectangle((int)Math.round(x),(int)Math.round(y),1,1);
               return;
            }

            en = v.elements();
            while( en.hasMoreElements() ) {
               Position o = (Position) en.nextElement();

               // Faut-il l'ajouter ou l'enlever
               if( !o.isSelected() ) {
                  //System.out.println("faut il ajouter ou enlever");

                  if( !(o instanceof Source)
                        || ((Source)o).noFilterInfluence()
                        || ((Source)o).isSelectedInFilter() )  { // ajout thomas
                     if( !aladin.view.vselobj.contains(o) ) aladin.view.vselobj.addElement(o);
                     o.setSelect(true);
                  }
               } else {
                  if( o instanceof Source && !((Source)o).isTagged() ) {
                     o.setSelect(false);
                     aladin.view.vselobj.removeElement(o);
                  }
               }
            }

            // S'agirait-il d'une rotation d'un Plan Field
         } else if( (poignee=vs.testFoVRollable(vs,v,p.x,p.y))!=null ) {
            return;


            // S'agirait-il d'un changement de taille d'un Repere circulaire ?
         } else if( (poigneePhot = testPhot(vs,p.x,p.y,true))!=null ) {
            return;

            // Sinon on deselectionne les precedents, et on reselectionne
            // les nouveaux
         } else {

            // Si le premier element a deja ete selectionne precedemment
            // on suppose qu'il s'agit d'un clic-and-drag
            if( v.size()>0 && aladin.view.vselobj.contains(v.elementAt(0))  ) {
               flagMoveNotSelect=true;
               rselect=null;
               try {
                  Source o = (Source) v.elementAt(0);
                  repereshow=aladin.mesure.mcanvas.show(o,2);
               } catch( Exception ec ) {}
               return;
            }

            Vector vTag = new Vector();
            en = aladin.view.vselobj.elements();
            while( en.hasMoreElements() ) {
               Obj o = (Obj)en.nextElement();
               if( !(o instanceof Source) ) o.setSelect(false);
               else {
                  if( ((Source)o).isTagged() ) vTag.add(o);
                  else o.setSelect(false);
               }
            }
            en = v.elements();
            while( en.hasMoreElements() ) {
               Position pos = ((Position)en.nextElement());
               // on interdit la selection de points d'un PlanContour et d'un PlanFov
               if (pos.plan instanceof PlanContour || pos.plan.type==Plan.FOV ) continue;

               pos.setSelect(true);
            }

            aladin.view.vselobj = vTag;
            aladin.view.vselobj.addAll(v);
         }

         // Mise a jour des mesures
         aladin.view.setMesure();


         //         if( aladin.view.hasSelectedObj() ) {
         if( flagMoveNotSelect ) {

            // Positionnement du repere et mise a jour de la
            // fenetre des infos
            try {
               Source o = (Source) v.elementAt(0);
               repereshow=aladin.mesure.mcanvas.show(o,2);
            } catch( Exception ec ) { }

         } else rselect = new Rectangle((int)Math.round(x),(int)Math.round(y),1,1);

         return;
      }

      // S'il y a deja un objet en cours de creation, on le
      // termine avant d'en creer un nouveau
      if( view.newobj!=null ) {

         // Fin d'un texte
         if( view.newobj instanceof Tag ) {
            finNewObjet();

            // Traitement d'une polyligne en mode clic, un simple clic on cree
            // un nouveau sommet, un double clic (ou cote) =>  on a fini
         } else if( view.newobj instanceof Ligne && flagLigneClic ) {
            boolean finObj=false;
            Obj suivant=null;
            boolean cote = (view.newobj instanceof Cote);
            view.extendClip(view.newobj);
            view.newobj.setPosition(vs,p.x,p.y);
            if( e.getClickCount()<2 && !cote && !flagOnFirstLine) {
               suivant = new Ligne(view.newobj.getPlan(),vs,p.x,p.y,(Ligne)view.newobj);
            }

            setNewObjet();

            // Bouclage d'un polygone
            if( flagOnFirstLine ) {
               if( e.getClickCount()>1 )view.newobj = ((Ligne)view.newobj).debligne;  // On efface le sommet en doublon
               ((Ligne)view.newobj).makeLastLigneForPolygone(this,true);
               addObjSurfMove(((Ligne)view.newobj).getFirstBout());
               finObj=true;
            }
            if( e.getClickCount()<2 && !cote && !finObj) {
               view.newobj = suivant;
               view.extendClip(view.newobj);
            } else finObj=true;

            if( finObj ) finNewObjet();

            view.repaintAll();
            return;
         }
         view.newobj=null;
      }

      // Creation d'un nouvel objet
      if( ToolBox.isForTool(tool) ) {
         Plan plan = aladin.calque.selectPlanTool();
         view.newobj = aladin.toolBox.newTool( plan ,vs,p.x,p.y);
      } else view.newobj=null;

      // Le debut d'une cote est insere immediatement
      // et on travaille sur le deuxieme bout
      if( view.newobj instanceof Cote ) {
         calque.setObjet(view.newobj);
         view.newobj = new Cote(view.newobj.getPlan(),vs,p.x,p.y,(Cote)view.newobj);
         view.newobj.setSelect(true);
         if( !aladin.view.vselobj.contains(view.newobj) ) aladin.view.vselobj.addElement(view.newobj);
         flagLigneClic=false;        // Par defaut une polyligne s'insere un mode drag
      }

      // idem pour le debut d'une polyligne
      else if( view.newobj instanceof Ligne ) {
         calque.setObjet(view.newobj);
         view.newobj = new Ligne(view.newobj.getPlan(),vs,p.x,p.y,(Ligne)view.newobj);
         flagLigneClic=true;        // Par defaut une polyligne s'insere un mode clic
      }

      // Extension du clip Rect
      if( view.newobj!=null ) view.extendClip(view.newobj);

      aladin.view.repaintAll();
      return;
   }

   /** Positionne les mesures dans la fenetre des infos.
    * et reaffiche la fenetre des tools et de la vue
    * en fonctions des objets selectionnes
   protected void setMesure() {
      boolean flagExtApp = aladin.hasExtApp();
      String listOid[] = null;
      int nbOid=0;

      aladin.mesure.removeAllElements();

      Enumeration e = aladin.view.vselobj.elements();
      //long b = System.currentTimeMillis();

      if( flagExtApp ) listOid = new String[aladin.view.vselobj.size()];

      // vecteur des sources � supprimer de vselobj
      Vector v = new Vector();
      while( e.hasMoreElements() ) {
         Objet o = (Objet) e.nextElement();

         if( o instanceof Source ) {
            // pas de selection pour les objets ne "passant" pas le filtre
               if( ((Source)o).noFilterInfluence() || ((Source)o).isSelected() ) {
                  o.info(aladin);
               }
               else {
                   ((Source)o).select = false;
                   //vselobj.removeElement(o);
                   v.addElement(o);
               }

              // Test si cette source provient d'une application cooperative
              // type VOPlot, et si oui, fait le callBack adequat
             if( flagExtApp ) {
                String oid = ((Source)o).getOID();
                if( oid!=null ) listOid[nbOid++]=oid;
             }
         }
      }

      // on supprime de vselobj toutes les sources qui doivent l'etre
      Enumeration eObjTodel = v.elements();
      while( eObjTodel.hasMoreElements() ) aladin.view.vselobj.removeElement(eObjTodel.nextElement());

      //long end = System.currentTimeMillis();
      //System.out.println(end-b);
      aladin.toolbox.toolMode();
      aladin.mesure.mcanvas.fullRepaint();

      // Callback adequat pour la liste des sources que l'application cooperative
      // doit selectionner
      if( flagExtApp ) aladin.callbackSelectExtApp(listOid,nbOid);

      // Calculs pour CDS team
      aladin.view.forCDSteam();

      aladin.view.repaintAll();
   }
    */

   public void mouseReleased(MouseEvent e) {
      if( isFullScreen() && widgetControl!=null && widgetControl.mouseReleased(e) ) {
         repaint(); return;
      }
      mouseReleased1(e.getX(),e.getY(),e);
      if( createCoteDist() ) repaint();
   }
   public void mouseReleased1(double x, double y,MouseEvent e) {

      //      if( Aladin.levelTrace>=3 ) testConvertion(x,y);

      if( pref!=null && pref instanceof PlanBG ) ((PlanBG)pref).resetDrawFastDetection();

      if( hasCrop() ) {
         view.crop.endDrag(this);
         repaint();
      }

      if( hasRainbow() && rainbow.endDrag() ) repaint();
      if( rainbowF!=null && rainbowF.endDrag() ) repaint();
      if( rainbowUsed ) return;

      if( flagSimRepClic )  {
         flagSimRepClic=false;
         view.simRep.couleur = Color.red; //MCanvas.C2;
         aladin.view.repaintAll();
         return;
      }

      // Rien � faire
      if( flagCube ) {
         flagCube=false;
         if( e.isShiftDown() ) { aladin.view.syncCube(this); aladin.view.repaintAll(); }
         return;
      }

      int tool = getTool(e);
      boolean boutonDroit = (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0;

      boolean fullScreen = isFullScreen();

      //Menu Popup
      if( boutonDroit ) {
         setScrollable(false);

         // Fin d'ajustement du contraste par le bouton droit
         if( xDrag!=-1 && Math.abs(xDrag-x)>2 && Math.abs(yDrag-y)>2 ) { xDrag=yDrag=-1; repaint(); return; }
         xDrag=yDrag=-1;

         if( fullScreen ) {
            aladin.fullScreen.mousePressed(e);
            return;
         }
         flagDrag=false;
         rselect = /*orselect =*/ null;
         showPopMenu(e.getX(),e.getY());
         return;
      }

      // Synchronisation sur une autre vue ?
      ViewSimple vs = getProjSyncView();
      boolean isProjSync = isProjSync();

      // Dans le cas de l'outil Move, Zoom Avant/arri�re si aucun d�placement
      // mesur� (via scroll()) et que le scroll automatique est arr�t�
      if( tool==ToolBox.ZOOM ) {
         aladin.calque.zoom.zoomView.stopDrag();
         setScrollable(false);
         Coord coo=null;
         try {
            if( !e.isShiftDown() ) {
               PointD p = vs.getPosition(x,y);
               coo = new Coord();
               coo.x=p.x; coo.y=p.y;
               vs.getProj().getCoord(coo);
            }
         } catch( Exception e1 ) { coo=null; }
         vs.syncZoom(e.isShiftDown() ?-1:1,coo,true);
         return;
      }

      // L'outils Move pour tester s'il faut lancer un scrolling automatique
      // ou si simplement il faut finir un clic-and-drag simple du champ
      if( flagScrolling || vs.isScrolling() ) {
         setScrollable(false);
         boolean bougeRepere=false;

         // L'utilisateur a attendu plus de 50ms entre son dernier mouseDrag et son mouseUp
         // on suppose qu'il ne veut pas lancer le scrolling automatique
         if( e.getWhen()-lastWhenDrag>30 ) {
            bougeRepere = !wasScrolling && !vs.isScrolling();
            vs.stopAutomaticScroll();
            //            orselect=null;
            //System.out.println("Automatical scroll aborted");
         }

         vs.stopAutomaticScroll();

         // S'il y a un offset suffisant (de l'�lan dans la souris), on lance
         // le scrolling automatique
         if( vs.isScrolling() ) {
            rselect=null;
            vs.scroll(e,(int)Math.round(x-vs.scrollX),(int)Math.round(y-vs.scrollY));
            vs.startAutomaticScroll();
         }

         if( fullScreen && !bougeRepere  ) return;
      }

      // Juste pour que la valeur dans le champ de localisation corresponde au repere
      // et qu'on puisse faire un undo
      if( tool==ToolBox.PAN ) {
         Coord repCoord = new Coord(view.repere.raj,view.repere.dej);
         //         aladin.view.memoUndo(this,repCoord,null);
         String s = aladin.localisation.J2000ToString(repCoord.al,repCoord.del);
         aladin.localisation.setSesameResult(s);
         aladin.view.setRepereId(s);
      }

      // Fin d'une �ventuelle manipulation de rotation d'un FOV ou d'extension d'un repere circulaire
      poignee=null;
      poigneePhot=null;
      if( poigneeTag!=null ) { poigneeTag.resetOn(); poigneeTag=null; }

      // Gestion d'une recalibration de catalogue par glissement
      if( planRecalibrating!=null ) {
         view.newView(1);
         planRecalibrating=null;
         view.repaintAll();
         return;
      }

      //      boolean withForCDSTeam = true;    // Test d'usage de la m�thode forCDSTeam
      //      if( tool==ToolBox.SELECT ) view.coteDist=null;

      // Pour forcer la reg�n�ration du curseur
      resetDefaultCursor(tool,e.isShiftDown());

      // Pour supprimer le message d'accueil en mode fullScreen
      if( fullScreen ) aladin.endMsg();

      // Fin d'un megaDrag ?
      if( tool==ToolBox.SELECT && aladin.view.stopMegaDrag(this,(int)Math.round(x),(int)Math.round(y),e.isControlDown()) ) return;

      // Recalibration dynamique en cours ?
      boolean recalib = !isProjSync && aladin.view.isRecalibrating() && !flagClicAndDrag
            && (tool==ToolBox.SELECT || tool==ToolBox.PAN);
      if( recalib ) {
         Vector v=null;

         PointD p = vs.getPosition(x,y);
         if( aladin.frameNewCalib.isGettingOriginalXY() ) v=aladin.frameNewCalib.plan.getObjWith(this,p.x,p.y);
         else v=calque.getObjWith(this,p.x,p.y);

         Obj o = v!=null && v.size()>0?(Obj)v.elementAt(0):null;
         flagMoveRepere=false;

         if( o==null || o instanceof Position ) {

            // D�placement globale
            int method=aladin.frameNewCalib.getModeCalib();
            if( method==FrameNewCalib.SIMPLE ) {
               if( o!=null && ((Position)o).plan==aladin.frameNewCalib.plan ) {
                  planRecalibrating=aladin.frameNewCalib.plan;
               }

               // Mise � jour de la liste des quadruplets
            } else if( method==FrameNewCalib.QUADRUPLET ) {
               aladin.frameNewCalib.mouse(p.x,p.y,(PlanImage)pref,(Position)o);
            }
         }
      }
      
      // D�placement du rep�re
      if( (tool==ToolBox.SELECT || tool==ToolBox.PAN && (!flagClicAndDrag || e.getClickCount()>1) )
            && flagMoveRepere && !isGrabIt() && !e.isShiftDown() && !isPlotView() ) {
         PointD p = vs.getPosition(x,y);
         vs.moveRepere(p.x,p.y,e.getClickCount()>1);
      }
      
      flagMoveRepere=true;
      flagClicAndDrag=false;

      if( isFree() ) return;

      // Mode GrabIt actif
      if( isGrabIt() ) {
         modeGrabIt=false;
         aladin.dialog.setGrabItRadius(grabItX,grabItY,x,y);
         stopGrabIt();
         aladin.dialog.toFront();
         flagMoveRepere=false;
      }

      //  Recalcul des Field of View si necessaire
      if( flagDragField!=0 ) {
         calque.resetPlanField(flagDragField,aperture,nAperture);
         flagDragField=0;
         aladin.view.repaintAll();
         return;
      }

      // memorisation de la position et de la valeur du pixel
      // ou de l'objet sous la souris si il est unique
      if( tool==ToolBox.SELECT || tool==ToolBox.PAN ) {

         if( view.vselobj.size()==1 ) {
            aladin.localisation.seeCoord((Position)view.vselobj.elementAt(0),1);
         } else if( rselect==null || rselect.width==1 && rselect.height==1) {
            aladin.localisation.setPos(vs,x,y,1);
            updateInfo();

            if( aladin.framePixelTool!=null ) aladin.framePixelTool.setPixel(vs,x,y);
         }
      }

      // Cas du zoom => Rien a faire
      if( tool==ToolBox.ZOOM ) return;

      // Le repere est insere
      if( view.newobj instanceof Repere ) {

         flagDrag=false;
         Point p = getPosition((int)x,(int)y);
         PointD pp = getPosition(x,y);

         // Extraction d'une source par m�thode IQE
         if( !((Repere)view.newobj).hasRayon() && !(pref instanceof PlanImageBlink || pref instanceof PlanBG) ) {
            double [] iqe = ((PlanImage)pref).getPixelStats(p);
            if( iqe!=null ) {
               pp.x = iqe[0]; pp.y = iqe[2];
               view.extendClip(view.newobj);
               view.newobj.setPosition(this,pp.x,pp.y);
               iqe[2] = ((PlanImage)pref).height - iqe[2];
               aladin.calque.updatePhotometryPlane( (Repere)view.newobj,iqe);
            }
            view.newobj = null;
         }

         if( view.newobj!=null ) {

            // Juste pour le spectre localis� pour un cube via un repere
            if( pref instanceof PlanImageBlink && !view.hasSelectedObj() ) {
               aladin.toolBox.setMode(ToolBox.PHOT,Tool.UP);
               aladin.toolBox.setMode(ToolBox.SELECT,Tool.DOWN);
               view.selectCote(view.newobj);
               view.extendClip(view.newobj);
            }

            // Insertion d'un rep�re avec mesure de surface
            if( ((Repere)view.newobj).hasRayon() ) {
               view.newobj.setSelected(true);
               addObjSurfMove(view.newobj);

            }
            finNewObjet();
         }

         view.newobj=null;
      }

      // Traitement de la fin d'une selection multiple
      if( rselect!=null ) {
         if( rselect.width>1 && rselect.height>1 ) {
            flagDrag=false;
            extendSelect((int)Math.round(x),(int)Math.round(y));
            PointD p1 = vs.getPosition((double)rselect.x,(double)rselect.y);
            PointD p2 = vs.getPosition((double)rselect.x+rselect.width,
                  (double)rselect.y+rselect.height);
            Vector<Obj> res = calque.setMultiSelect(vs,new RectangleD(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y));

            // Dans le cas du shift on merge les deux vecteurs
            int nObjAdd=0;
            if( e.isShiftDown() ) {
               Enumeration<Obj> en = res.elements();
               while( en.hasMoreElements() ) {
                  Obj o = en.nextElement();
                  if( !aladin.view.vselobj.contains(o) ) {
                     aladin.view.vselobj.addElement(o);
                     o.info(aladin);
                     nObjAdd++;
                  }
               }
            } else {
               Vector vTag = new Vector();
               for( Obj o : aladin.view.vselobj ) {
                  if( o instanceof Source && ((Source)o).isTagged()
                        && !res.contains(o) ) vTag.add(o);
               }
               aladin.view.vselobj = vTag;
               aladin.view.vselobj.addAll(res);
            }

            extendClip(res);

            //            if( nObjAdd==2 ) createCoteDist(res);
            //            else { withForCDSTeam=false; createCoteDist(aladin.view.vselobj); }

            aladin.view.setMesure();
            rselect=null;
            resetClip();
            aladin.view.repaintAll();
            return;
         } else { rselect=null; resetClip(); }
      }

      // Traitement pour la Ligne et Cote
      if( view.newobj!=null && view.newobj instanceof Ligne ) {

         if( !(view.newobj instanceof Cote) ) flagLigneClic=true;

         // Calcul de la nouvelle position
         PointD p = vs.getPosition(x,y);

         // Je selectionne la cote
         if( view.newobj instanceof Cote ) {
            aladin.view.selectCote(view.newobj);

            // Pour �viter de faire 2 cotes de suite
            aladin.toolBox.setMode(ToolBox.DIST,Tool.UP);
            aladin.toolBox.setMode(ToolBox.SELECT,Tool.DOWN);

         }

         // Rien a faire si je suis une ligne en mode Clic, le mouseDown s'est charge
         // d'inserer le sommet
         if( !(view.newobj instanceof Cote) && flagLigneClic ) return;

         // Si on n'a pas bouge de la position du mouse Down, on va tracer
         // la polyligne en mode clique, plutot qu'en mode drag. il faudra
         // un double-clic pour terminer
         // Rq : on test si le point precedent est bien le debut de la ligne et que
         //      l'on a pas bouge
         if( !flagLigneClic ) {
            Ligne l = (Ligne) view.newobj;
            if( l.debligne!=null ) {
               l=l.debligne;
               if( p.x==l.xv[n] && p.y==l.yv[n] && l.debligne==null ) {
                  flagLigneClic=true;
                  return;
               }
            }
         }

         view.extendClip(view.newobj);
         view.newobj.setPosition(vs,p.x,p.y);
         view.extendClip(view.newobj);
         if( view.newobj instanceof Cote ) finNewObjet();
         else setNewObjet();
         //         else setNewObjet(withForCDSTeam);
         view.newobj=null;
      }

      if(  view.newobj!=null && view.newobj instanceof Tag ) {
         if( ((Tag)view.newobj).isReticle() ) {
            aladin.calque.updateTagPlane((Tag)view.newobj);
            view.newobj=null;
         } else ((Tag)view.newobj).setEditing(true);
      }

      /** Traitement d'�ventuellement d�placement (ou cr�ation) d'objet de surface
       * � transmettre � des observers */
      if( objSurfMove!=null ) {
         Enumeration e1=objSurfMove.elements();
         while( e1.hasMoreElements() ) {
            final Position o = (Position)e1.nextElement();
            if( o.plan.type!=Plan.TOOL ) continue;
            ((PlanTool)(o.plan)).sendMesureObserver(o, false);
            aladin.console.printInPad(o.getSexa()+" => "+o.getInfo()+"\n" );


            //            System.out.println("Ici c'est parti !");
            //            ((PlanTool)(o.plan)).updatePhotMan(o);
            //
            //            SwingUtilities.invokeLater(new Runnable() {
            //               public void run() {
            //                  Util.pause(100);
            //                  o.setSelected(true);
            //                  o.plan.updateDedicatedFilter();
            //                  aladin.calque.repaintAll();
            //               }
            //            });
         }
         objSurfMove=null;
      }

      aladin.view.repaintAll();
   }

   /** Traitement particulier pour le CDS */
   protected boolean createCoteDist() {
      if( !calque.flagAutoDist ) return false;
      Vector<Obj> v = view.vselobj;
      //      System.out.println("createCoteDist() => size="+v.size());
      CoteDist oc = view.coteDist;
      if( v.size()!=2 ) { view.coteDist=null; return oc!=view.coteDist; }
      Obj a = v.elementAt(0);
      Obj b = v.elementAt(1);
      if( !(a instanceof Repere || a instanceof Source
            || (a instanceof Tag && a.hasPhot())) ) return false;
      if( !(b instanceof Repere || b instanceof Source
            || (b instanceof Tag && b.hasPhot())) ) return false;
      view.coteDist = new CoteDist(a,b,getProjSyncView());
      aladin.status.setText(view.coteDist.id);
      aladin.console.printInPad(view.coteDist.id+"\n");
      return true;
   }

   /* REMARQUE SUR LE CLIP RECT
    *    L'extension en 2 temps du clip est indispensable car il est
    *    possible que la methode mouseDrag soit appelee plusieurs fois avant que
    *    le update() ait pu avoir lieu
    */

   private final Coord c1 = new Coord();
   private final Coord c2 = new Coord();

   /** Modification de la colormap par d�placement direct dans la vue - � la  DS9 */
   private void setCMByMouse(int x, int y) {

      Plan pref = view.getLastClickView().pref;
      //      Plan pref = aladin.calque.getFirstSelectedPlanImage();
      if(  !(pref.isImage() || pref.type==Plan.ALLSKYIMG )  ) return;
      //      if( pref.type==Plan.ALLSKYIMG && ((PlanBG)pref).color ) return;
      y=getHeight()-y;
      int x1 = (int)( (x*256.)/(getWidth()-5));
      int y1 = (int)( (y*256.)/(getHeight()-5));
      if( y1<10 ) y1=10;
      int tr1 = x1-y1/2;
      int tr3 = x1+y1/2;
      int gap=0;
      if( tr1<0 ) { gap=tr1/2; tr1=0; }
      if( tr3>255 ) { gap=(tr3-255)/2; tr3=255; }

      int tr2 = (tr1+tr3)/2+gap;
      if( tr2<tr1-5 ) tr2=tr1+5;
      if( tr2>tr3-5 ) tr2=tr3-5;

      PlanImage pimg = (PlanImage)pref;
      pimg.cmControl[0]=tr1;
      pimg.cmControl[1]=tr2;
      pimg.cmControl[2]=tr3;
      if( pimg instanceof PlanImageRGB ) {
         for( int i=0; i<3; i++ ) ((PlanImageRGB)pimg).filterRGB(pimg.cmControl, i);
         for( int i=0; i<9; i++ ) {
            ((PlanImageRGB)pimg).RGBControl[i]=(i%3)==0 ? tr1 : (i%3)==1 ? tr2 : tr3;
         }
      } else if( pimg instanceof PlanBG && ((PlanBG)pimg).isColored() ) {
         for( int i=0; i<9; i++ ) {
            ((PlanBG)pimg).RGBControl[i]=(i%3)==0 ? tr1 : (i%3)==1 ? tr2 : tr3;
         }
         pimg.changeImgID();

      } else {
         IndexColorModel  ic = CanvasColorMap.getCM(tr1,tr2,tr3,
               pimg.video==PlanImage.VIDEO_INVERSE,
               pimg.typeCM,pimg.transfertFct,pimg.isTransparent());
         pimg.setCM(ic);
         aladin.calque.zoom.zoomView.setCM(ic);
      }
      repaint();
      aladin.calque.zoom.zoomView.repaint();

      if( aladin.frameCM!=null ) {
         for( int i=0; i<3; i++ ) {
            aladin.frameCM.cm.triangle[i] = pimg.cmControl[i];
            if( pimg instanceof PlanImageRGB ||
                  pimg instanceof PlanBG && ((PlanBG)pimg).isColored() ) {
               aladin.frameCM.cm2.triangle[i] = pimg.cmControl[i];
               aladin.frameCM.cm3.triangle[i] = pimg.cmControl[i];
            }
         }
         aladin.frameCM.majCM();
      }
   }


   static int MAXAPERTURE = 100;

   // Pour la m�morisation d'apertures concern�s par une rotation ou un glissement
   // Sera utilis� par mouseDrag et par mouseUp
   PlanField aperture[] = new PlanField[MAXAPERTURE];
   int nAperture = 0;

   // Pour la m�morisation des objets de surface ayant �t� d�plac�s
   Vector objSurfMove = null;

   /** Ajout d'un �l�ment dans la liste des objets qui viennent d'�tre d�plac�s par la souris afin
    * de les conna�tre lors de l'�v�nement mouseReleased1() */
   private void addObjSurfMove(Obj o) {
      //      if( aladin.VOObsMes==null || aladin.VOObsMes.size()==0 ) return;
      if( objSurfMove==null ) objSurfMove=new Vector();
      if( !objSurfMove.contains(o) ) {
         objSurfMove.addElement(o);
      }
   }

   // Retourne true si cette vue a l'outil crop
   private boolean hasCrop() {
      return view.getCurrentView()==this && view.crop!=null && view.crop.isVisible();
   }

   // Retourne true si le rainbow associ�e � la table des couleurs existe et est visible
   protected boolean hasRainbow() { return rainbow!=null && rainbow.isVisible(); }

   // Retourne true si le rainbow associ�e au filtre existe et est visible
   protected boolean hasRainbowF() { return rainbowF!=null && rainbowF.isVisible(); }

   protected boolean rainbowAvailable() { return pref!=null && pref.hasAvailablePixels(); }

   protected boolean rainbowUsed() {
      return hasRainbow() && rainbow.isUsed()
            || hasRainbowF() && rainbowF.isUsed();
   }

   private boolean flagMoveNotSelect=false; // true si le porchain clic and drag sera un d�placement d'objets
   // et non une extension du rectangle de la s�lection

   public void mouseDragged(MouseEvent e) {

      if( isFullScreen() && widgetControl!=null && widgetControl.mouseDragged(e) ) {
         repaint(); return;
      }

      int x = e.getX();
      int y = e.getY();
      int tool = getTool(e);
      int i;

      // On cache le simbad tooltip
      aladin.view.suspendQuickSimbad();

      boolean boutonDroit = (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0;
      if( boutonDroit ) {
         if( Math.abs(xDrag-x)>1 || Math.abs(yDrag-y)>1 ) {
            try { setCMByMouse(x,y); } catch( Exception e1 ) {}
            return;
         }
      }

      // Synchronisation sur une autre vue ?
      ViewSimple vs = getProjSyncView();
      boolean isProjSync = isProjSync();

      flagClicAndDrag=true;

      if( !isProjSync ) {
         if( tool==ToolBox.PAN && e.isShiftDown() && selected ) aladin.view.selectCompatibleViews();
         if( tool==ToolBox.PAN ) {
            selected=true;
            if( aladin.view.getCurrentView()!=this ) aladin.view.setCurrentView(this);
         }
      }

      // Scrolling de la vue par la souris
      if( flagScrolling || inNE(x,y) ) {
         aladin.calque.zoom.zoomView.startDrag();
         lastWhenDrag = e.getWhen();
         vs.scroll(e);
         vs.scrollX=x; vs.scrollY=y;
         return;
      }

      // Si on est sur le blinkControl..
      if( flagCube  ) {
         cubeControl.mouseDragged(e);
         return;
      }
      //      if( flagCube  ) {
      //         aladin.view.setCubeFrame(this, cubeControl.getFrameLevel(x),e.isShiftDown());
      //         aladin.view.repaintAll();
      //         return;
      //      }


      // Pas de d�placement de rep�re si on fait un drag > � 4 pixels
      if( rselect!=null && Math.max(rselect.width,rselect.height)>4
            || !aladin.view.vselobj.isEmpty() ) flagMoveRepere=false;

      // Mode GrabIt actif
      if( isGrabIt() ) {
         aladin.dialog.setGrabItRadius(grabItX,grabItY,x,y);
         cGrabItX=x; cGrabItY=y;
         aladin.view.updateAll();
         return;
      }

      // Extension d'un rectangle de cropping
      if( hasCrop() ) {
         view.crop.mouseDrag( vs, x,y, e.isShiftDown() );
         repaint();
         return;
      }

      // Extension d'un rectangle de rainbow
      if( hasRainbow() && rainbow.mouseDrag( vs, x,y, e.isShiftDown() ) ) { repaint(); return; }
      if( rainbowF!=null && rainbowF.mouseDrag( vs, x,y, e.isShiftDown() ) ) { repaint(); return; }

      if( true /* planRecalibrating==null */ ) {

         // Peut etre un d�but de MegaDrag
         if( tool==ToolBox.SELECT && xDrag==-1 ) aladin.view.startMegaDrag(this);

         // On sort du panel, sans doute un MegaDrag
         if( x<0 || x>getSize().width || y<0 || y>getSize().height ) return;


         // Agrandissement du rectangle de selection multiple
         if( rselect!=null ) {
            extendSelect(x,y);
            flagDrag=true;
            repaint();
            return;
         }
      }

      // Deplacement d'objets s�lectionn�s
      if( tool==ToolBox.SELECT && flagMoveNotSelect && fixe!=null && poigneePhot==null && poigneeTag==null) {

         // Calcul du vecteur deplacement en RA,DEC ou en XY (suivant les flags)
         PointD p = vs.getPosition((double)x,(double)y);
         double dx=0,dy=0;
         double dra=0,dde=0;
         Projection proj=vs.getProj();
         boolean flagDepRaDec=e.isShiftDown() && Projection.isOk(proj);
         if( flagDepRaDec ) {
            c1.x=fixe.x; c1.y=fixe.y;
            c2.x=p.x;    c2.y=p.y;
            proj.getCoord(c1);
            proj.getCoord(c2);
            if( Double.isNaN(c1.al) || Double.isNaN(c2.al) ) flagDepRaDec=false;
            else {
               dra = c2.al-c1.al;
               dde = c2.del-c1.del;
            }
         }

         // Calcul de l'offset m�me si on utilise que flagDepRaDec
         dx = p.x-fixe.x;
         dy = p.y-fixe.y;
         fixe = p;                // pour l'etape suivante

         double centerX=0,centerY=0,angle=0;

         // S'agit-il d'une rotation => calcul de l'angle et du centre de la rotation
         // C'est assez tordu car dans le cas d'un Pickle ou d'un Arc, le centre de rotation
         // et le centre du cercle inscrit (voir Arc.rotatePosition() ), donc si on ne s�lectionne
         // qu'un arc ou un pickle, le centre de rotation est l'objet lui-m�me
         // En revanche pour un planField, tous les objets qui le composent auront �t� s�lectionn�s
         // via la m�thode testFoVRollable() appel�e par mouseDown() et ce sera le plan d'appartenance
         // qui donnera le centre de rotation via getRotCenterObjet()
         if( poignee!=null ) {

            Position center;
            if( poignee.plan instanceof PlanField ) center = ((PlanField)poignee.plan).getRotCenterObjet();
            else center = poignee;
            centerX = center.xv[vs.n];
            centerY = center.yv[vs.n];
            double y1 = p.y-centerY;
            double x1 = p.x-centerX;
            angle =  Math.atan2(y1+dy,x1+dx) - Math.atan2(y1,x1);
            double angleDeg = (angle*180/Math.PI);
            if( poignee.plan instanceof PlanField ) {
               ((PlanField)poignee.plan).deltaRoll(-angleDeg);
               aladin.status.setText("Rotation: angle="+((PlanField)poignee.plan).getRoll()+"�");
            }
         }

         nAperture=0;
         Enumeration<Obj> en = aladin.view.vselobj.elements();
         while( en.hasMoreElements() ) {
            Obj o = en.nextElement();
            if( !o.plan.recalibrating ) {
               if( o.plan.type!=Plan.TOOL && o.plan.type!=Plan.APERTURE
                     || !o.plan.isMovable()
                     || o.plan.type==Plan.FOV ) continue; // pas de deplacement pour un objet de PlanContour ou PlanFov
            }

            // Creation ou extension du clip
            extendClip(o);
            //            extendClip(o.getClip(this));

            // Attention il y aura un FoV a reprojeter � la fin du d�placement
            if( o.plan.type==Plan.APERTURE ) {
               flagDragField |= poignee!=null ? ROLL
                     : o.plan==aladin.calque.planRotCenter ?
                           MOVECENTER : MOVE;

               for( i=0; i<nAperture; i++ ) if( aperture[i]==o.plan ) break;
               if( i==nAperture ) aperture[nAperture++] = (PlanField)( o.plan );
            }

            // Rotation ?
            if( poignee!=null ) {
               ((Position)o).rotatePosition(vs,angle,centerX,centerY);

               // Deplacement soit en ra,dec si possible, soit en xy
            } else if( o.plan.type!=Plan.APERTURE || o.plan.isMovable()) {
               if( flagDepRaDec
                     && !((Position)o).plan.recalibrating
                     && flagDragField==0 ) o.deltaRaDec(dra,dde);
               else o.deltaPosition(vs,dx,dy);

               if( o instanceof Repere && ((Repere)o).hasRayon() && o.plan.isMovable()) {
                  addObjSurfMove(o);
               }

               createCoteDist();
            }

            //extension du clip et reaffichage
            extendClip(o);
            //            extendClip(o.getClip(this));
            //            resetClip();
         }

         // Dans le cas o� il y aurait des Observers pour les APERTURE en cours de manipulation
         if( nAperture>0 ) {
            for( i=0; i<nAperture; i++ ) {
               if(       (flagDragField&ROLL)!=0 ) aperture[i].sendRollObserver(true);
               if(       (flagDragField&MOVE)!=0 ) aperture[i].sendTargetObserver(true);
               if( (flagDragField&MOVECENTER)!=0 ) aperture[i].sendRotCenterObserver(true);
            }
         }

         if( isFullScreen() ) aladin.view.resetClip();
         aladin.view.quickRepaintAll();
      }

      // Extension d'un rep�re pour avoir une surface circulaire (d�j� trac� - poigneePhot!=null,
      // ou en cours de tra�age - view.newobj!=null )
      if( poigneePhot!=null || view.newobj!=null && view.newobj instanceof Repere ) {
         Repere t = poigneePhot!=null ? poigneePhot : (Repere)view.newobj;
         view.extendClip(t);
         PointD p = getPosition((double)x,(double)y);
         double rayonView = Math.sqrt( Math.pow(fixev.x-x,2) + Math.pow(fixev.y-y,2) );
         double deltaX = Math.abs(t.xv[n]-p.x);
         double deltaY = Math.abs(t.yv[n]-p.y);
         double rayon = Math.sqrt( deltaX*deltaX + deltaY*deltaY);
         if( rayonView>3 ) {
            t.setRayon(this,rayon);
            t.setSelected(true);
            if( poigneePhot==null ) view.setSelected(t,true);
            view.extendClip(t);
            if( view.newobj!=null ) {
               aladin.view.unSelectObjetPlanField(((Position)view.newobj).plan);
               view.newobj.setSelect(true);
            }
            aladin.view.repaintAll();
         }
         if( poigneePhot!=null ) addObjSurfMove(t);

         // Modification de l'ancrage d'un Texte/Label
      } else if( poigneeTag!=null ) {
         PointD p = vs.getPosition((double)x,(double)y);
         if( poigneeTag.modifyViaMouse(vs,p,fixe) ) aladin.view.repaintAll();

         // insertion d'un tag, le drag entraine la saisie d'un label dans la direction indiqu�e
      } else if( view.newobj instanceof Tag ) {
         Tag t = (Tag)view.newobj;
         PointD p = vs.getPosition((double)x,(double)y);
         t.setEditing(true);
         t.modifyPoignee(vs,p.x,p.y);
         aladin.view.repaintAll();
      }


      // Traitement des lignes (Cotes, polylignes...)
      if( view.newobj!=null && view.newobj instanceof Ligne ) {

         // Puisqu'on fait un drag !
         flagLigneClic=false;

         // Calcul de la nouvelle position
         PointD p = vs.getPosition((double)x,(double)y);

         // Positionnement du clip initial
         view.extendClip(view.newobj);

         // Memorisation de la nouvelle position
         view.newobj.setPosition(vs,p.x,p.y);

         // Dans le cas d'une polyligne, on commence une nouvelle arete
         if( !(view.newobj instanceof Cote) /* && !flagLigneClic */ ) {

            Obj suivant = new Ligne(view.newobj.getPlan(),vs,p.x,p.y,(Ligne)view.newobj);
            setNewObjet();
            view.newobj = suivant;
         } else {
            if( view.newobj!=null ) {
               view.unSelectObjetPlanField(((Position)view.newobj).plan);
               view.selectCote(view.newobj);
            }
         }

         // Extension du clip et reaffichage
         view.extendClip(view.newobj);
         view.quickRepaintAll();
      }

      //      reloadPixelsOriginIfRequired();
      aladin.localisation.setPos(vs,x,y);

      updateInfo();
      //      if( isProjSync ) aladin.pixel.setUndef();
      //      else aladin.pixel.setPixel(vs,x,y);

      // affichage de la loupe ou d'une coupe si necessaire
      if( !calque.zoom.redrawWen(x,y) ) calque.zoom.redrawCut();

      view.propResume();
      return;
   }

   /** Recharge pixelsOrigin si n�cessaire pour pouvoir afficher la vraie valeur
    * du pixel courant.
    * @return true si n�cessaire et possible
    */
   private boolean reloadPixelsOriginIfRequired() {
      if( isFree() || !pref.hasAvailablePixels() ) {
         return false;
      }
      if( pref.type==Plan.IMAGEBLINK || pref.type==Plan.IMAGECUBE ) {
         if( cubeControl.mode!=CubeControl.PAUSE ) return false;
      }
      if( aladin.view.getPixelMode()==View.LEVEL ) {
         return false;
      }
      if( ((PlanImage)pref).pixelsOriginFromDisk() ) {
         return false;
      }
      return ((PlanImage)pref).pixelsOriginFromCache();
   }

   private int oc=Aladin.DEFAULTCURSOR;
   int margeX, margeY;

   /** Retourne true si la souris n'est pas dans du fond d'image */
   //   protected boolean isMouseOnSomething() {
   //      if( lastView==null ) return false;
   //      int x = (int)( lastView.x - margeX);
   //      int y = (int)( lastView.y - margeY);
   //      //      System.out.println("lastView="+lastView.x+","+lastView.y+" marge="+margeX+","+margeY+" x,y="+x+","+y+" width="+w);
   //      if( x<0 || x>=w || y<0 || y>=h ) return false;
   //      int pix=0;
   //      if( pixelsRGB!=null ) {
   //         pix = pixelsRGB[ y*w+x ];
   //         pix =  ( (0xFF & pix) + ((pix>>>8) & 0xFF) + ((pix>>>16) & 0xFF) )/3;
   //      }
   //      else if( pixels!=null ) {
   //         pix = pixels[ y*w+x ] & 0xFF ;
   //      }
   //      if( ((PlanImage)pref).video == PlanImage.VIDEO_INVERSE ) pix = 255 - pix;
   //      aladin.trace(4,"ViewSimple.isMouseOnSomething() pix => "+pix );
   //      return pix>20;
   //   }

   public void mouseMoved(MouseEvent e) {
      if( isFullScreen() && widgetControl!=null && widgetControl.mouseMoved(e) ) {
         repaint(); return;
      }
      mouseMoved1(e.getX(),e.getY(),e);
   }

   protected void mouseMoved1(double x, double y,MouseEvent e) {
      boolean trouve = false;
      boolean flagRollable=false;
      flagOnFirstLine=false;
      boolean ok=false;

      // Synchronisation sur une autre vue ?
      ViewSimple vs = getProjSyncView();

      if( isFree() || aladin.view.isMegaDrag() ) {
         if( aladin.view.isMultiView() ) aladin.status.setText("["+view.VIEW+" "+getID()+"]");
         return;
      }

      // Memorisation de la position en cas de deplacement fin avec les fleches
      lastMove = vs.getPosition(x,y);
      lastView = new PointD(x,y);

      int tool=getTool(e);

      // Gestion du rectangle de crop
      if( hasCrop() ) view.crop.mouseMove(x,y,vs );

      // Gestion du rectangle de rainbow
      if( hasRainbow() && rainbow.mouseMove(x,y)
            || rainbowF!=null && rainbowF.mouseMove(x,y) ) { repaint(); return; }

      // rechargement des pixels d'origine si n�cessaire (obligatoire pour les cubes)
      reloadPixelsOriginIfRequired();

      boolean fullScreen = isFullScreen();

      if( fullScreen && aladin.fullScreen.mouseMoved((int)x,(int)y) ) return;

      // Affichage de la position et de la valeur du pixel
      aladin.localisation.setPos(vs,x,y);
      Projection proj = vs.getProj();
      if( aladin.calque.hasPixel() ) {
         if( Projection.isOk(proj) ) {
            Coord coo = new Coord();
            coo.x = lastMove.x; coo.y=lastMove.y;
            proj.getCoord(coo);
            aladin.view.setPixelInfo(coo);
         } else {
            if( pref instanceof PlanImage ) {
               String s = ((PlanImage)pref).getPixelInfo( (int)Math.floor(lastMove.x), (int)Math.floor(lastMove.y), view.getPixelMode());
               if( s==PlanImage.UNK ) s="";
               setPixelInfo(s);
            }
         }
      }


      // (thomas) affichage dans l'arbre des images disponibles
      if( tool==ToolBox.SELECT ) vs.showAvailableImages(x,y);

      // En cas de passage de la souris sur le blinkControl, il faut
      // le r�afficher imm�diatement pour que le REWIND/PLAY/FORWARD soit
      // trac� dans la bonne couleur
      if( isPlanBlink() && cubeControl.mouseMoved(e)>CubeControl.NOTHING ) {
         flagBlinkControl=true;
         update(getGraphics());
         //         return;
      }
      //      if( isPlanBlink() && cubeControl!=null ) {
      //         int m = cubeControl.setMouseMove(vs,(int)Math.round(x),(int)Math.round(y));
      //         if( m!=CubeControl.NOTHING ) {
      //           flagBlinkControl=true;
      //           update(getGraphics());
      //           return;
      //         } else Util.toolTip(this,"");
      //      }

      // Juste pour �viter de le faire 2x
      boolean isSelectOrTool = tool==ToolBox.SELECT || tool==ToolBox.PAN || ToolBox.isForTool(tool);

      // Affichage du rectangle du zoom
      if( tool==ToolBox.ZOOM && !isScrolling() ) {
         rselect = new Rectangle((int)Math.round(x)-rv.width/4,(int)Math.round(y)-rv.height/4,rv.width/2,rv.height/2);
         flagDrag=true;        // Pour un affichage rapide
         repaint();

         // Affichage des labels des sources et des vecteurs
      } else if( isSelectOrTool ) {
         PointD p = lastMove;   // deja calcule

         if( view.simRep!=null && view.simRep.inLabel(this, x, y) ) trouve=true;

         Plan [] allPlans = calque.getPlans();
         Plan folder = calque.getMyScopeFolder(allPlans,pref);

         // Affichage du baratin de l'objet pointe
         Plan cPlan=null;
         //         int nplan=-1;
         int n=0;
         flagOnMovableObj=false;
         Obj memoObj=null;
         for( int i=allPlans.length-1; i>=0; i-- ) {
            Plan plan =  allPlans[i];
            if( !plan.active ) continue;        // Pas visible
            if( plan.type==Plan.NO || plan.isImage()
                  || plan.type==Plan.FOV || plan.type==Plan.FOLDER
                  || plan instanceof PlanContour // on ne montre plus la valeur des niveaux pour les contours
                  || plan.type==Plan.FILTER  ) continue; // Pas d'objet

            // Plan objet simple pas projetable dans cette vue
            if( plan.pcat!=null && !plan.pcat.isDrawnInSimpleView(vs.n) ) continue;

            // Pas le m�me scope
            if( folder!=calque.getMyScopeFolder(allPlans,plan) ) continue;

            boolean testOnMovable = fullScreen && (plan.type==Plan.APERTURE
                  || plan.type==Plan.TOOL);


            // Determination du nombre d'objet sous la souris
            Iterator<Obj> it = plan.iterator(this);
            for( int j=0; it!=null && it.hasNext(); j++ ) {
               Obj o = it.next();
               if( o.in(vs,p.x,p.y) ) {

                  if( testOnMovable ) flagOnMovableObj=true;

                  // Objets rollables (APERTURE)
                  if( allPlans[i].type==Plan.APERTURE && j>1  /* Pour eviter de prendre le Repere central et le centre de rotation*/
                        && ((PlanField)allPlans[i]).isRollable() ) {
                     if( ((Position)o).inBout(vs,p.x,p.y) ) {
                        flagRollable=true;
                     }
                  }

                  // rep�re si on est entrain de faire une polyligne et que le prochain
                  // point se trouve sur le d�but de la polyligne pour faire un
                  // polygone => changement de curseur
                  else if( !flagOnFirstLine
                        //                        && ( !(pref instanceof PlanBG) || ( pref instanceof PlanBG && pref.hasAvailablePixels() ))
                        && Ligne.isLigne(view.newobj)
                        && Ligne.isDebLigne(o)
                        && ((Ligne)o).plan==((Ligne)view.newobj).plan) {
                     flagOnFirstLine=true;
                  }

                  if(  !( o instanceof Source )
                        || ((Source)o).noFilterInfluence()
                        || ( ((Source)o).isSelectedInFilter() ) ) {
                     n++;

                     if( memoObj==null ) {
                        memoObj=o;
                        cPlan = allPlans[i];
                        //                        nplan=i;
                     }
                  }
               }
            }
         }

         // Pour pouvoir afficher les infos d'une coteDist
         if( view.coteDist!=null && view.coteDist.inside(this, p.x, p.y) ) {
            view.coteDist.status(aladin);
            n=0;
         }

         if( n>0 ) {
            if( n==1 ) {
               memoObj.status(aladin);
               if( calque.flagTip ) {
                  String id = memoObj.id;
                  Util.toolTip(this,id!=null ? cPlan.label+": "+id : "");
               }
            } else {
               if( calque.flagTip ) Util.toolTip(this,n+" objects");
            }

            // Show info de la source dans le canvas des mesures
            if( cPlan.isCatalog() && memoObj instanceof Source ) {
               Source o = (Source) memoObj;
               if( o.isSelected() ) ok=aladin.mesure.mcanvas.show(o,1);

               //System.out.println("je vais p-e monter la source courante");
               //if(o instanceof Source) System.out.println(((Source)o).isSelected);
               // Je montre la source courante
               if(   o.noFilterInfluence() || o.isSelectedInFilter()   ) {
                  aladin.view.showSource(o);
               }
            }

            // Plus d'un objet
            if( n>1 && cPlan.isCatalog() ) aladin.status.setText(n+" "+View.HCLIC1);
         }

         nbSourceUnderMouse=n;

         trouve=trouve || (n>0);

         // Je cache la derniere source montree si necessaire
         if( !trouve ) {
            aladin.view.hideSource();
            if( calque.flagTip ) Util.toolTip(this,"");
         }

         // d�termine si la souris est sur le bord de la vue pour un �ventuel d�placement ou une copie de vue
         int marge = rv.width/40;
         boolean flagMarge = view.isMultiView() && (tool==ToolBox.SELECT || tool==ToolBox.PAN)
               && (x<=marge || x>=rv.width-marge || y<=marge || y>=rv.height-marge);

         // Je change le curseur si necessaire
         if( !aladin.lockCursor && isSelectOrTool ) {
            resetDefaultCursor(tool, e.isShiftDown() );
            if( flagOnFirstLine ) {
               if( oc!=Aladin.JOINDRECURSOR ) Aladin.makeCursor(this,(oc=Aladin.JOINDRECURSOR));
            } else if( flagRollable || inNE((int)x,(int)y) ) {
               if( oc!=Aladin.TURNCURSOR ) Aladin.makeCursor(this,(oc=Aladin.TURNCURSOR));
            } else if( trouve ) {
               if( oc!=Aladin.HANDCURSOR ) Aladin.makeCursor(this,(oc=Aladin.HANDCURSOR));
            } else if( flagMarge ) {
               if( oc!=Aladin.PLANCURSOR ) Aladin.makeCursor(this,(oc=Aladin.PLANCURSOR));
            } else {
               if( oc!=currentCursor ) Aladin.makeCursor(this,(oc=currentCursor));
            }
         }

         // Cache le repere de la source dans le canvas des mesures
         // si necessaire et memorise le fait qu'un repere est positionne
         if( repereshow && !ok ) aladin.mesure.mcanvas.show((Source)null,1);
         repereshow=ok;
      }

      // je suspends une �ventuelle r�solution en cours
      if( tool!=ToolBox.SELECT && tool!=ToolBox.PAN) aladin.view.suspendQuickSimbad();
      else {
         // Je d�marre le d�compte des 2 secondes en attendant une r�solution QuickSimbad
         if( aladin.calque.flagSimbad || aladin.calque.flagVizierSED ) {
            aladin.view.waitQuickSimbad(vs);
         }
      }

      boolean rep=false;
      if( tool==ToolBox.SELECT && !trouve
            && aladin.view.repere!=null
            && aladin.view.repere.in(vs,lastMove.x,lastMove.y) ) {
         aladin.view.repere.status(aladin);
         rep=true;
      }

      if( !trouve && !rep  ) {
         StringBuffer s = new StringBuffer();
         if( aladin.view.isMultiView() ) {
            s.append("["+view.VIEW+" "+getID()+"]");
            s.append( (s.length()>0 ? " - ":"")+pref.label);
            //         if( pref.from!=null && pref.isSimpleImage() )  s.append(" - "+pref.getFrom());
         }
         aladin.status.setText(s.toString());
      }

      // Affichage de la loupe ou d'une coupe si necessaire
      //      calque.zoom.redraw((int)(x),(int)(y+0.5));
      calque.zoom.redrawWen(x,y);

      // Pour desssiner le losange de controle Healpix sous la souris
      if( aladin.getOrder()>=0 ) repaint();

      // Pour afficher la position courante
      else if( fullScreen ) repaint();

      return;
   }

   /** Affiche les coordonn�es correspondantes � la position de la souris
    * directement dans la vue */
   private void updateInfo() {
      if( imgbuf==null || hasCrop() ) return;
      Graphics g = getGraphics();
      int w=260;
      int a=imgbuf.getWidth(this)-w;
      g.setClip(a,3,w-20,13);
      g.drawImage(imgbuf,0,0,this);
      drawPixelInfo(g);
      g.dispose();
   }

   private int nbSourceUnderMouse=-1;
   protected int getNbSourceUnderMouse() { return nbSourceUnderMouse; }

   public void mouseExited(MouseEvent e) {
      
      aladin.setMemory();

      if( aladin.menuActivated() ) return;
      if( hasRainbow() && rainbow.isDragging() ) return;
      if( rainbowF!=null && rainbowF.isDragging( ) ) return;

      // Arr�t de la proc�dure QuickSimbad si n�cessaire
      if( aladin.calque.flagSimbad || aladin.calque.flagVizierSED ) aladin.view.suspendQuickSimbad();

      // Peut etre s'agit-il d'un MegaDrag ?
      if( aladin.view.flagMegaDrag ) { rselect=null; resetClip(); }

      // Notification a view que ce n'est plus moi qui ait la souris
      aladin.view.setMouseView(null);
      repaint();  // pour mettre � jour le pourtour

      // R�affichage du select pour mettre � jour le bon triangle
      aladin.calque.select.repaint();

      if( aladin.view.flagMegaDrag ) return;

      // Fin du blinking si necessaire
      aladin.view.hideSource();

      // Fin de l'affichage rapide en cas de zoom
      if( getTool(e)==ToolBox.ZOOM ) {
         flagDrag=false;
         rselect=null;
         repaint();
      }

      // (thomas) Fin de l'affichage des images disponibles
      if( getTool(e)==ToolBox.SELECT) {
         if( aladin.treeView.mTree != null ) {
            aladin.treeView.mTree.turnOffAllNodes();
         }
         if( aladin.dialog.server[aladin.dialog.current].tree!=null ) {
            aladin.dialog.server[aladin.dialog.current].tree.turnOffAllNodes();
         }
      }

      // Modification du curseur
      Aladin.makeCursor(aladin,Aladin.DEFAULTCURSOR);
      resetClip();

      // On arrete l'insertion de l'objet en cours
      finNewObjet();

      // Modification du zoom si necessaire
      calque.zoom.wenOff();

      // Arret de la localisation
      aladin.localisation.setUndef();

      // Arr�t de l'affichage du pixel
      aladin.view.setPixelInfo(null);
   }

   public String toString() {
      return "View "+n+" pref="+(isFree()?"null":pref.label);
   }

   public void mouseEntered(MouseEvent e) {

      if( aladin.menuActivated() ) return;

      // Notification a view que c'est moi qui ait la souris
      aladin.view.setMouseView(this);
      paintBordure();

      // R�affichage du select pour mettre � jour le bon triangle
      aladin.calque.select.repaint();

      // Modification du zoom si necessaire
      calque.zoom.wenOn();

      // Recuperation du Focus dans le cas d'un WEN actif pour pouvoir
      // effectuer un ajustement fin
      if( aladin.toolBox.tool[ToolBox.WEN].mode==Tool.DOWN ) requestFocusInWindow();

      // Curseur pour le d�placement d'un plan
      if( aladin.view.isMegaDrag() ) Aladin.makeCursor(this,Aladin.PLANCURSOR);

      // Modification du curseur en fonction de l'outil
      else setDefaultCursor(aladin.toolBox.getTool(),e.isShiftDown());
   }

   /** Positionnement du curseur par d�faut en fonction de l'outil courant et
    * d'un �ventuel Megadrag en cours */
   protected void setDefaultCursor(int tool,boolean shift) {
      if( aladin.lockCursor ) return;
      currentCursor =
            tool==ToolBox.PAN ? Aladin.HANDCURSOR :
               tool==ToolBox.PHOT ? ( isTagCentered(shift) ? Aladin.TAGCURSOR : Aladin.CROSSHAIRCURSOR) :
                  aladin.view.isRecalibrating() && (tool==ToolBox.SELECT || tool==ToolBox.PAN)
                  || isGrabIt() || tool==ToolBox.ZOOM ? Aladin.CROSSHAIRCURSOR:
                     tool==ToolBox.TAG ? Aladin.TEXTCURSOR:Aladin.DEFAULTCURSOR;

               Aladin.makeCursor(this,currentCursor);
   }


   /** Positionnement du cureseur par d�faut en fonction de l'outil courant */
   private void resetDefaultCursor(int tool,boolean isShift) {
      if( hasRainbow() && rainbow.isSelected() ) return;
      if( rainbowF!=null && rainbowF.isSelected() ) return;
      if( aladin.lockCursor ) return;

      if( widgetControl!=null && widgetControl.getCursor()!=null ) { setCursor(  widgetControl.getCursor() ); return; }

      oc=-1;
      currentCursor = tool==ToolBox.PHOT ||
            aladin.view.isRecalibrating() && tool==ToolBox.SELECT? (isTagCentered(isShift) ? Aladin.TAGCURSOR : Aladin.CROSSHAIRCURSOR) :
               tool==ToolBox.ZOOM ?
                     Aladin.CROSSHAIRCURSOR:(tool==ToolBox.PAN )?
                           Aladin.HANDCURSOR:(tool==ToolBox.TAG)?
                                 Aladin.CROSSHAIRCURSOR:Aladin.DEFAULTCURSOR;

            Aladin.makeCursor(this,currentCursor);
   }

   /** Retourne le mode Centered ou non pour le tag */
   private boolean isTagCentered(boolean isShift) {
      return false;   // POUR LE MOMENT PAR DE RECENTRAGE
      //      return isNorthUp() ? false : isShift ? !aladin.CENTEREDTAG : aladin.CENTEREDTAG;
   }

   // Pour pouvoir faire du copier/coller
   private Coord repCoord = new Coord();
   
   /** D�placement du repere courant dans toutes les vues en fonction
    *  de la position ximg,yimg (position dans l'image) */
   protected void moveRepere(double ximg,double yimg,boolean flagSync) {
      if( isPlotView() ) return;
      try {
         repCoord = new Coord();
         repCoord.x=ximg; repCoord.y=yimg;
         
         getProj().getCoord(repCoord);
         if( Double.isNaN(repCoord.al) ) return;
         moveRepere(repCoord,flagSync);
      } catch( Exception e){ }
   }

   /** D�placement du repere courant dans toutes les vues en fonction
    *  de la coordonn�e pass�e en param�tre */
   protected void moveRepere(Coord repCoord,boolean flagSync) {
      if( isPlotView() ) return;
      
      // Si on change de CCD, le flagSync est forc� � true
      if( pref instanceof PlanMultiCCD ) {
         flagSync = ((PlanMultiCCD)pref).setRef(repCoord);
      }

      String s = aladin.localisation.J2000ToString(repCoord.al,repCoord.del);
      
      // Affichage dans la console de la position HEALPIX
      if( aladin.calque.gridMode==2 && aladin.calque.hasGrid() ) {
         try {
            Coord c = new Coord(repCoord.al,repCoord.del);
            c = aladin.localisation.frameToFrame(c,Localisation.ICRS,aladin.localisation.getFrameGeneric() );
            double [] d = new double[]{ c.al,c.del };
            d=CDSHealpix.radecToPolar(d);
            int order = getLastGridHpxOrder();
            long nside = CDSHealpix.pow2(order);
            long npix = CDSHealpix.ang2pix_nest(nside, d[0], d[1]);
            aladin.console.printCommand(order+"/"+npix);
         } catch( Exception e ) { }
         
      // Sinon affichage de la coordonn�e
      } else aladin.console.printCommand(s);
      
      aladin.view.setRepereId(s);
      //      aladin.view.memoUndo(this,repCoord,null);
      //      if( flagSync ) aladin.view.syncView(1,repCoord,this);
      if( flagSync ) aladin.view.syncView(1,repCoord,this,flagSync);
      else { aladin.view.moveRepere(repCoord); aladin.view.repaintAll(); }

      /* if( pref instanceof PlanBG ) */ aladin.dialog.adjustParameters();
      //      if( pref instanceof PlanBG ) {
      ////         aladin.dialog.setDefaultTarget(repCoord+"");
      //         aladin.dialog.setDefaultTarget(s);
      //         aladin.dialog.setDefaultParameters(Aladin.aladin.dialog.current,0);
      //      }
      aladin.sendObserver();
   }

   //   private boolean shiftSync=true; /* false pour invalider l'action de syncro sur SHIFT */

   public void keyTyped(KeyEvent e) {}

   /** Initialise le scrolling automatique, sans l'activer */
   synchronized private void initScroll() {
      stopAutomaticScroll();
      view.startTimer(50);
      xViewScroll=yViewScroll=-1;
   }

   public void keyReleased(KeyEvent e) {
      if( view.newobj!=null && view.newobj instanceof Tag ) {
         view.quickRepaintAll();
      }
   }

   public void keyPressed(KeyEvent e) {
      int key = e.getKeyCode();
      char k = e.getKeyChar();

      // Suppression de l'objet selectionne
      if(  key==KeyEvent.VK_DELETE && aladin.toolBox.getTool()==ToolBox.SELECT ) {
         //         aladin.view.delSelObjet();   // Ca sera effectu� par aladin.delete()
         return;
      }

      if( isPlanBlink() && cubeControl.isEditing() ) {
         if( cubeControl.keyPress(e) ) repaint();
         return;
      }

      if( hasCrop() && view.crop.isEditing() ) {
         if( view.crop.keyPress(this,e) ) repaint();
         return;
      }

      if( view.newobj!=null && key==KeyEvent.VK_ESCAPE ) finNewObjet();

      // Construction d'un objet graphique de texte
      else if( view.newobj!=null && view.newobj instanceof Tag ) {

         // On efface le dernier caractere
         if( key==KeyEvent.VK_BACK_SPACE || key==KeyEvent.VK_DELETE ) {
            Tag t = (Tag) view.newobj;
            if( t.id.length()==0 ) return;
            t.setText( t.id.substring(0,t.id.length()-1) );

            // On insere un nouveau caractere
         } else if( k>=31 && k<=255 || key==KeyEvent.VK_ENTER) {
            Tag t = (Tag) view.newobj;
            t.setText( t.id + k );
         }
      } else if( key!=KeyEvent.VK_UP && key!=KeyEvent.VK_DOWN && key!=KeyEvent.VK_PAGE_DOWN ) {
         if( isFullScreen() ) { flagDrag = !aladin.fullScreen.sendKey(e); repaint(); }
         else aladin.localisation.sendKey(e);
      }

      // Extension du ClipRect
      if( view.newobj!=null ) view.extendClip(view.newobj);

      else {
         // Peut etre un ajustement fin par les fleches (si on a la loupe active)
         if( aladin.toolBox.tool[ToolBox.WEN].mode==Tool.DOWN &&
               (key==KeyEvent.VK_UP || key==KeyEvent.VK_DOWN
               || key==KeyEvent.VK_LEFT || key==KeyEvent.VK_RIGHT
//               || k=='+' || k=='-'
               || key==KeyEvent.VK_ENTER) ) {
            if( k=='+' ) aladin.calque.zoom.zoomView.changeWen(1);
            else if( k=='-' ) aladin.calque.zoom.zoomView.changeWen(-1);
            else if( key!=KeyEvent.VK_ENTER ) {
               PointD p = focusByKey(key);
               MouseEvent me = new MouseEvent(this,e.getID(),e.getWhen(), e.getModifiers(),
                     (int)Math.round(p.x),(int)Math.round(p.y),1,false);
               mouseMoved1(p.x,p.y,me);
               return;
            } else {
               PointD p = getViewCoordDble(lastMove.x,lastMove.y);
               MouseEvent me = new MouseEvent(this,e.getID(),e.getWhen(),
                     e.getModifiers(),(int)Math.round(p.x),(int)Math.round(p.y),1,false);
               mousePressed1(p.x,p.y,me);
               mouseReleased1(p.x,p.y,me);
               requestFocusInWindow();
               return;
            }
         } else {
            if( key==KeyEvent.VK_UP || key==KeyEvent.VK_DOWN 
                  || key==KeyEvent.VK_PAGE_DOWN ) aladin.localisation.sendKey(e);
         }
      }
   }


   private void markAvailableImages(double x, double y, boolean shiftDown) {
      // on marque les images pour le serveur courant et pour l'history tree
      markAvailableImages(x,y,aladin.dialog.server[aladin.dialog.current].tree,shiftDown);
      markAvailableImages(x,y,aladin.treeView.mTree,shiftDown);

      showAvailableImages(x,y);
   }

   /**
    * Marque dans le treeView les images disponibles par un "V" rouge
    * @param x abscisse dans vue courante
    * @param y ordonn�e dans vue courante
    * @param tree arbre pour lequel on doit marquer les images
    * @param shiftDown si shift �tait appuy� --> les images d�ja marqu�es le restent (on compl�te la s�lection)
    */
   private void markAvailableImages(double x, double y, MetaDataTree tree, boolean shiftDown) {

      Projection proj;
      if( isFree() || !Projection.isOk(proj=getProj())) return;

      PointD p = getPosition(x,y);
      Coord c = new Coord();
      c.x=p.x; c.y=p.y;
      proj.getCoord(c);
      if( Double.isNaN(c.al) ) return;
      String targetSexa = c.getSexa();

      // si on est en grabMode, maj pour un seul noeud, et on ne marque pas les images dispos !!
      if( aladin.getFrameInfo().inGrabMode() ) {
         aladin.getFrameInfo().setTarget(targetSexa);
         aladin.getFrameInfo().toFront();
         return;
      }

      // si on est en mosaicGrabMode, maj pour un seul noeud, et on ne marque pas les images dispos !!
      if( aladin.getFrameInfo().inMosaicGrabMode() ) {
         aladin.getFrameInfo().setMosaicTarget(targetSexa);
         aladin.getFrameInfo().toFront();
         return;
      }

      // si on est en imagePosGrabMode, maj pour un seul noeud, et on ne marque pas les images dispos !!
      if( aladin.getFrameInfo().inImagePosGrabMode() ) {
         String xStr, yStr;
         if( pref==null || !pref.isImage() ) xStr = yStr = "";
         else {
            PointD p2   = getPosition(x,y);
            xStr = (int)(p2.x)+"";
            yStr = (int)(((PlanImage)pref).naxis2-p2.y)+"";
         }

         aladin.getFrameInfo().setImagePosTarget(xStr, yStr);
         aladin.getFrameInfo().toFront();
         return;
      }

      if( tree==null || tree.nodeFullTab==null ) return;
      BasicNode[] tab = tree.nodeFullTab;
      ResourceNode node;

      for( int i=0; i<tab.length; i++) {
         node = (ResourceNode)tab[i];

         // si ce n'est pas une feuille ou un noeud image, on passe au noeud suivant
         if( node==null || !node.isLeaf || node.type!=ResourceNode.IMAGE || node.getFov()==null ) continue;

         if( node.getFov().contains(x,y,pref,this) ) {
            node.isSelected=true;
            if( node.cutout )node.setCutoutTarget(targetSexa);
            if( node.modes!=null && node.modes.length>1 ) node.setMosaicTarget(targetSexa);
            // si shiftDown, les noeuds d�ja s�lectionn�s le restent
         } else if( !shiftDown ){
            node.isSelected=false;
         }
      }
      tree.turnOffAllNodes();
      //      tree.doDisplay();
      tree.repaint();
   }

   /** Montre dans le treeView et dans AladinServer les images disponibles */
   private void showAvailableImages(double x, double y) {
      if( aladin.dialog==null ) return;
      // on montre les images pour le serveur courant ET pour l'history tree
      // pour le serveur courant
      showAvailableImages(x,y,(aladin.dialog.server[aladin.dialog.current]).tree,aladin.dialog.server[aladin.dialog.current]);
      // pour l'arbre d'history
      showAvailableImages(x,y,aladin.treeView.mTree,aladin.treeView);
   }


   private void showAvailableImages(double x, double y,MetaDataTree tree,Component container) {
      if( !container.isShowing() ) return;
      if( tree==null ) return;
      //if( !tree.isShowing() ) return;

      if( isFree() || getProj() == null ) return;
      BasicNode[] tab = tree.nodeTab;
      if( tab==null ) return;
      ResourceNode node;

      for( int i=0; i<tab.length; i++) {

         node = (ResourceNode)tab[i];


         // si ce n'est pas une feuille ou un noeud image ou fov null,
         // on passe au noeud suivant
         if( !node.isLeaf || node.type!=ResourceNode.IMAGE || node.getFov()==null ) continue;

         if( node.getFov().contains(x,y,pref,this) ) tree.litUpNode(node);
         else tree.turnOffNode(node);
      }

      tree.repaint();
   }

   /** Deplacement d'un pixel par fleches */
   private PointD focusByKey(int key) {
      int dx = key==KeyEvent.VK_LEFT?-1:key==KeyEvent.VK_RIGHT?1:0;
      int dy = key==KeyEvent.VK_UP?-1:key==KeyEvent.VK_DOWN?1:0;
      return new PointD((lastMove.x+dx - rzoom.x)*zoom ,
            ((lastMove.y+dy - rzoom.y)*zoom));
   }

   /** Reset l'�tat de l'image extraite pour l'imagette du zoom
    *  afin d'un r�extraire une nouvelle
    */
   protected void resetImgID() { lastImgID=-1; previousFrameLevel=-2; }

   /** Cr�ation ou r�cup�ration de l'Image de background. Cr�ation
    *  de l'image et de son contexte graphique si n�cessaire
    *  @return true si l'image a �t� modifi�, false sinon
    */
   protected boolean getImageBG() {
      boolean flagchange=false;         // Vrai s'il faut reextraire une vue
      Dimension rv = getSize();
      if( rv.width==0 ) rv = new Dimension(this.rv.width,this.rv.height);   // bon on passe par le rectangle

      // Pas encore de buffer de base
      if( img==null || img.getWidth(this)!=rv.width
            || img.getHeight(this)!=rv.height ) {
         //         img = getGraphicsConfiguration().createCompatibleImage(rv.width,rv.height);
         img = aladin.createImage(rv.width,rv.height);
         flagchange=true;
         newView();

         // Creation du contexte graphique
         g = img.getGraphics();
      }
      adjustZoom();
      g.setClip(null);

      // Mise en place du clip
      //      setClip(g);

      // Du blanc en dessous (dans le cas des bords)
      drawBackground(g);

      return flagchange;
   }

   private int[] createZoomPixelsRGB(PlanImage p,int [] oldPixelsRGB) { 
      try {
         return createZoomPixelsRGB(p,oldPixelsRGB,-1);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return null;
      }
   }
   private int[] createZoomPixelsRGB(PlanImage p,int [] oldPixelsRGB, double frame) {


      // A tout hasard => NE MARCHE PAS APRES UN CROP ?? => voir COMMENTAIRE "CROP" ci-dessous
      int [] newPixelsRGB = null;
      //       int [] newPixelsRGB = frame==-1 ? ((PlanRGBInterface)p).getPixelsRGB()
      //             : ((PlanImageCubeRGB)p).getFrameRGB((int)frame) ;

      w = (int)Math.ceil(rzoom.width+1);
      h = (int)Math.ceil(rzoom.height+1);

      // Extraction de la portion visible avec reajustement de l'image si
      // necessaire (presence de bords partiels)
      int x1 = (int)Math.floor(rzoom.x);
      int y1 = (int)Math.floor(rzoom.y);
      if( x1<0 ) {w+=x1; x1=0; }
      if( y1<0 ) {h+=y1; y1=0; }
      if( x1+w>p.width ) w=p.width-x1;
      if( y1+h>p.height ) h=p.height-y1;

      if( w<0 || h<0 ) return new int[0];

      //CROP  if( !(x1==0 && y1==0 && w==p.width && h==p.height) ) {
      newPixelsRGB = new int[w*h];
      if( frame==-1) ((PlanRGBInterface)p).getPixels(newPixelsRGB,x1,y1,w,h);
      else ((PlanImageCubeRGB)p).getPixels(newPixelsRGB,x1,y1,w,h,(int)frame) ;
      //     }
      
      // Zoom en agrandissement
      if( zoom>1 ) {

         int [] scalePixelsRGB;
         int z1 = (int) zoom;   // Casting
         int ddx = rzoom.x<=0 ? 0 : (int)Math.floor( (rzoom.x-Math.floor(rzoom.x))*zoom );
         int ddy = rzoom.y<=0 ? 0 : (int)Math.floor( (rzoom.y-Math.floor(rzoom.y))*zoom );
         int wDst=Math.min(w*z1-ddx,getWidth());
         int hDst=Math.min(h*z1-ddy,getHeight());
         int taille = wDst*hDst;
         scalePixelsRGB = oldPixelsRGB!=null && oldPixelsRGB.length==taille ? oldPixelsRGB : new int [taille];

         int yDeb=ddy;         // On commence sur une fraction de pixel (ordonn�e)
         int yFin = z1;

         int lig=0;          // num�ro ligne target
         for( int y=0; y<h && lig<hDst; y++ ) {
            int col=0;       // numero de colonne target
            int xDeb = ddx;   // On commence sur une fraction de pixel (abcisse)
            int xFin = z1;

            for( int x=0; x<w; x++ ) {
               int c = newPixelsRGB[y*w+x];

               for( int i=xDeb; i<xFin && col<wDst; i++, col++ ) scalePixelsRGB[lig*wDst + col]=c;
               xDeb=xFin;
               xFin+=z1;
            }
            lig++;

            for( int i=yDeb+1; i<yFin && lig<hDst; i++, lig++) {
               System.arraycopy(scalePixelsRGB, (lig-1)*wDst, scalePixelsRGB, lig*wDst, wDst);
            }
            yDeb=yFin;
            yFin+=z1;

         }
         newPixelsRGB = scalePixelsRGB;
         w=wDst;
         h=hDst;


         //          int i,j,k,d;
         //          d=0;
         //          int taille = w*h*z1*z1;
         //          scalepixelsRGB = oldPixelsRGB!=null && oldPixelsRGB.length==taille ? oldPixelsRGB : new int [taille];
         //          for( i=0; i<h; i++ ) {
         //             int orig=d;
         //             for( k=0; k<w; k++) {
         //                int c = pixelsRGB[i*w+k];
         ////                bytefill(scalepixelsRGB,d,z1,c);   // N'est plus aussi rentable qu'avant - sans doute JIT)
         //                for( int m=0; m<z1; m++ ) scalepixelsRGB[d+m]=c;
         //
         //                d+=z1;
         //             }
         //             for( j=1; j<z1; j+=j ) {
         //                System.arraycopy(scalepixelsRGB,orig, scalepixelsRGB,orig+j*w*z1,j*z1*w);
         //             }
         //             d+=(z1-1)*z1*w;
         //          }
         //          pixelsRGB = scalepixelsRGB;
         //          w *= z1;
         //          h *= z1;

         // Zoom reduction
      } else if( zoom<1 ) {
         int [] reducePixelsRGB;
         int  src=0;      // Nombre de pixels sources
         int  dst=0;      // Nombre de pixels destinations
         int  i,j,k,l,m;   // increments de balayage de l'image source
         int  di,dj;      // increments de balayage de l'image destination
         int  dw,dh;      // width,height de l'image de destination
         int  srcX;      // src*src;
         int  c1,c2,c3;      // nouveau pixel calcule
         int pk1[] = new int[9];   // pour le 2/3
         int pk2[] = new int[9];   // pour le 2/3
         int pk3[] = new int[9];   // pour le 2/3
         int maxw,maxh;   // Bornes sup de balayage

         src = calque.zoom.getNbPixelSrc(zoom);
         dst = calque.zoom.getNbPixelDst(zoom);
         srcX=src*src;

         maxw = w - w%src;
         maxh = h - h%src;
         dw = Math.round(maxw*dst/src);
         dh = Math.round(maxh*dst/src);

         int taille = dw*dh;
         if( taille<0 ) return null;
         try {
            reducePixelsRGB = oldPixelsRGB!=null && oldPixelsRGB.length==taille ? oldPixelsRGB : new int[taille];
         } catch( Throwable e ) {
            return null;
         }

         // cas 1/x
         if( dst==1  ) {
            for( di=i=0; i<maxh; i+=src, di+=dw ) {
               for( dj=j=0; j<maxw; j+=src, dj++ ) {
                  for( c1=c2=c3=l=0; l<src; l++ ) {
                     for( k=0, m=(i+l)*w+j; k<src; k++, m++ ) {
                        c1+= ((newPixelsRGB[m] & 0x00FF0000)>>16);
                        c2+= ((newPixelsRGB[m] & 0x0000FF00)>>8);
                        c3+= ((newPixelsRGB[m] & 0x000000FF));
                     }
                  }
                  c1/=srcX; c2/=srcX; c3/=srcX;
                  reducePixelsRGB[di+dj] = ( ( 0xFF ) << 24 ) |
                        ( (c1 & 0xFF ) << 16 ) |
                        ( (c2 & 0xFF ) << 8 ) |
                        (c3 & 0xFF );
               }
            }

            // Cas 2/3
         } else {
            int c;
            for( di=i=0; i<maxh; i+=src, di+=dst ) {
               for( dj=j=0; j<maxw; j+=src, dj+=dst ) {
                  for( c=l=0; l<src; l++ ) {
                     for( k=0, m=(i+l)*w+j; k<src; k++, m++, c++ ) {
                        pk1[c]=((newPixelsRGB[m] & 0x00FF0000)>>17);
                        pk2[c]=((newPixelsRGB[m] & 0x0000FF00)>>9);
                        pk3[c]=((newPixelsRGB[m] & 0x000000FF)>>1);
                     }
                  }
                  c=di*dw;
                  reducePixelsRGB[c+=dj]  =
                        ( ( 0xFF ) << 24 ) |
                        ( ((pk1[0]+pk1[1]) & 0xFF ) << 16 ) |
                        ( ((pk2[0]+pk2[1]) & 0xFF ) << 8 ) |
                        ((pk3[0]+pk3[1]) & 0xFF ) ;

                  reducePixelsRGB[c+1]    =
                        ( ( 0xFF ) << 24 ) |
                        ( ((pk1[2]+pk1[5]) & 0xFF ) << 16 ) |
                        ( ((pk2[2]+pk2[5]) & 0xFF ) << 8 ) |
                        ((pk3[2]+pk3[5]) & 0xFF ) ;

                  c=(di+1)*dw;

                  reducePixelsRGB[c+=dj]  =
                        ( ( 0xFF ) << 24 ) |
                        ( ((pk1[3]+pk1[6]) & 0xFF ) << 16 ) |
                        ( ((pk2[3]+pk2[6]) & 0xFF ) << 8 ) |
                        ((pk3[3]+pk3[6]) & 0xFF ) ;

                  reducePixelsRGB[c+1]    =
                        ( ( 0xFF ) << 24 ) |
                        ( ((pk1[7]+pk1[8]) & 0xFF ) << 16 ) |
                        ( ((pk2[7]+pk2[8]) & 0xFF ) << 8 ) |
                        ((pk3[7]+pk3[8]) & 0xFF ) ;
               }
            }
         }

         newPixelsRGB = reducePixelsRGB;
         w=dw;
         h=dh;
      }

      return newPixelsRGB;
   }


   /**
    * Cr�ation d'un buffer des pixels visibles �ventuellement zoom�s.
    * @param p le plan d'o� sont pris les pixels
    * @param frameLevel num�ro de la tranche d'un cube, ou -1 si non relevant
    *         peut comporter une partie d�cimale qui repr�sente le niveau de transparence
    *         entre la frame courante et la suivante (ou la premi�re si fin de s�rie)
    */
   private byte[] createZoomPixels(PlanImage p,byte [] oldPixels) { return createZoomPixels(p,oldPixels,-1); }
   private byte[] createZoomPixels(PlanImage p,byte [] oldPixels, double frameLevel) {

      int frame =(int)frameLevel;
      double transparency = frameLevel - frame;   // La partie d�cimale repr�sente la transparence

      // A tout hasard => NE MARCHE PAS APRES UN CROP ?? => voir COMMENTAIRE "CROP" ci-dessous
      //      byte [] pixels = frame==-1 ? p.pixels : ((PlanImageBlink)p).getFrame(frame) ;
      byte [] pixels =null;

      w = (int)Math.ceil(rzoom.width+1);
      h = (int)Math.ceil(rzoom.height+1);

      double zoom = this.zoom;

      int x1 = (int)Math.floor(rzoom.x);
      int y1 = (int)Math.floor(rzoom.y);
      if( x1<0 ) {w+=x1; x1=0; }
      if( y1<0 ) {h+=y1; y1=0; }
      if( x1+w>p.width ) w=p.width-x1;
      if( y1+h>p.height ) h=p.height-y1;

      if( w<0 || h<0 ) return new byte[0];

      // Cas d'une image HUGE
      flagHuge=false;
      if( p.type==Plan.IMAGEHUGE &&
            (((PlanImageHuge)p).inSubImage(x1,y1,w,h)
                  || ((PlanImageHuge)p).fromSubImage(zoom,getWidth(),getHeight())) ) {
         PlanImageHuge ph = (PlanImageHuge)p;
         if( ph.loadSubImage(x1,y1,w,h) ) {
            xHuge=x1; yHuge=y1; wHuge=w; hHuge=h;
            pixels = ph.cropPixels(x1,y1,w,h);
            zoom /= ph.step;
            w *=ph.step;
            h *=ph.step;
            flagHuge=true;
         }
      }

      // Cas courant
      if( !flagHuge ) {
         // Extraction de la portion visible avec reajustement de l'image si
         // necessaire (presence de bords partiels)
         // CROP  if( !(x1==0 && y1==0 && w==p.width && h==p.height) || transparency!=0 ) {

         pixels = new byte[w*h];
         //System.out.println("* J'extrait les pixels ("+x1+","+y1+") sur "+w+"x"+h+" pour "+this);
         if( frame==-1 ) p.getPixels(pixels,x1,y1,w,h);
         else ((PlanImageBlink)p).getPixels(pixels,x1,y1,w,h,frame,transparency);
         //         }
      }

      // Pour les tests unitaires
      testx1=x1; testy1=y1; testw=w; testh=h;

      // Zoom en agrandissement
      if( zoom>1 ) {
         byte [] scalepixels;
         int z1 = (int) zoom;   // Casting

         // Voir commentaire au else
         if( !flagHuge ) {
            int ddx = rzoom.x<=0 ? 0 : (int)Math.floor( (rzoom.x-Math.floor(rzoom.x))*zoom );
            int ddy = rzoom.y<=0 ? 0 : (int)Math.floor( (rzoom.y-Math.floor(rzoom.y))*zoom );
            int wDst=Math.min(w*z1-ddx,getWidth());
            int hDst=Math.min(h*z1-ddy,getHeight());
            int taille = wDst*hDst;
            scalepixels = oldPixels!=null && oldPixels.length==taille ? oldPixels : new byte [taille];

            int yDeb=ddy;         // On commence sur une fraction de pixel (ordonn�e)
            int yFin = z1;

            int lig=0;          // num�ro ligne target
            for( int y=0; y<h && lig<hDst; y++ ) {
               int col=0;       // numero de colonne target
               int xDeb = ddx;   // On commence sur une fraction de pixel (abcisse)
               int xFin = z1;

               for( int x=0; x<w; x++ ) {
                  byte c = pixels[y*w+x];

                  for( int i=xDeb; i<xFin && col<wDst; i++, col++ ) scalepixels[lig*wDst + col]=c;
                  xDeb=xFin;
                  xFin+=z1;
               }
               lig++;

               for( int i=yDeb+1; i<yFin && lig<hDst; i++, lig++) {
                  System.arraycopy(scalepixels, (lig-1)*wDst, scalepixels, lig*wDst, wDst);
               }
               yDeb=yFin;
               yFin+=z1;

            }
            pixels = scalepixels;
            w=wDst;
            h=hDst;

            // ANCIENNE METHODE PLUS GOURMANDE EN MEMOIRE => PIXELS DEBORDANT SUR LES BORDS, AJUSTEMENT PAR ddx ET ddy
            // J'AI DU LA GARDER POUR LES IMAGES HUGE => TROP COMPLIQUE/RISQUE A VIRER voir PlanImageHuge.getSubImage()
         } else {
            int i,j,k,d;
            d=0;
            int taille = w*h*z1*z1;
            scalepixels = oldPixels!=null && oldPixels.length>=taille ? oldPixels : new byte [taille];
            for( i=0; i<h; i++ ) {
               int orig=d;
               for( k=0; k<w; k++) {
                  byte c = pixels[i*w+k];
                  //               bytefill(scalepixels,d,z1,c);
                  for( int m=0; m<z1; m++ ) scalepixels[d+m]=c;

                  d+=z1;
               }
               for( j=1; j<z1; j+=j ) {
                  System.arraycopy(scalepixels,orig, scalepixels,orig+j*w*z1,j*z1*w);
               }
               d+=(z1-1)*z1*w;
            }
            pixels = scalepixels;
            w *= z1;
            h *= z1;
         }

         // Zoom reduction
      } else if( zoom<1 ) {
         byte [] reducepixels;
         int  src=0;      // Nombre de pixels sources
         int  dst=0;      // Nombre de pixels destinations
         int  i,j,k,l,m;   // increments de balayage de l'image source
         int  di,dj;      // increments de balayage de l'image destination
         int  dw,dh;      // width,height de l'image de destination
         int  c;      // nouveau pixel calcule
         int pk[] = new int[9];   // pour le 2/3
         int maxw,maxh;   // Bornes sup de balayage

         src = calque.zoom.getNbPixelSrc(zoom);
         dst = calque.zoom.getNbPixelDst(zoom);

         //       w=imgW;
         maxw = w - w%src;
         //       h=imgH;
         maxh = h - h%src;
         dw = Math.round(maxw*dst/src);
         dh = Math.round(maxh*dst/src);
         //       System.out.println("image src = ("+w+","+h+") --> ("+maxw+","+maxh+")");
         //       System.out.println("image dst = ("+dw+","+dh+")");

         int taille=dw*dh;
         reducepixels = oldPixels!=null && oldPixels.length==taille ? oldPixels : new byte [taille];

         // Cas 1/x avec x>4 => Au plus proche
         if( src>4 ) {
            for( di=i=0; i<maxh; i+=src, di+=dw ) {
               int pos = i*w;
               for( dj=di,j=0; j<maxw; j+=src ) {
                  reducepixels[dj++]=pixels[pos+j];
               }
            }

            // cas 1/x avec x<=4 => Moyenne
         } else if( dst==1  ) {
            //          long t = System.nanoTime();
            for( di=i=0; i<maxh; i+=src, di+=dw ) {
               for( dj=di,j=0; j<maxw; j+=src ) {
                  for( c=l=0; l<src; l++ ) {
                     for( k=0, m=(i+l)*w+j; k<src; k++, m++ ) {
                        c+=(pixels[m] & 0xFF);
                     }
                  }
                  reducepixels[dj++]=(byte)(c>>src);
               }
            }
            //          t = System.nanoTime()-t;
            //          System.out.println("z="+zoom+" norm "+t);

            // Cas 2/3 => A la Pierre
         } else {
            for( di=i=0; i<maxh; i+=src, di+=dst ) {
               for( dj=j=0; j<maxw; j+=src, dj+=dst ) {
                  for( c=l=0; l<src; l++ ) {
                     for( k=0, m=(i+l)*w+j; k<src; k++, m++, c++ ) {
                        pk[c]=(pixels[m] & 0xFF);
                     }
                  }
                  c=di*dw;
                  reducepixels[c+=dj]  =(byte)( (pk[0]+pk[1])/2 );
                  reducepixels[c+1]    =(byte)( (pk[2]+pk[5])/2);
                  c=(di+1)*dw;
                  reducepixels[c+=dj]  =(byte)( (pk[3]+pk[6])/2);
                  reducepixels[c+1]    =(byte)( (pk[7]+pk[8])/2);
               }
            }
         }

         pixels = reducepixels;
         w=dw;
         h=dh;
      }

      return pixels;
   }

   int otype=-1;
   int oh=-1;
   int ow=-1;
   ColorModel ocm=null;

   /** Preparation de l'image.
    * Prepare l'image de la vue courante en fonction du plan
    * de base et du zoom.
    * @param gr Le contexte graphique servira a afficher
    *          un eventuel message d'attente, ou null sinon
    * @param p le plan concern�
    * @return <I>true</I> si l'[iamge est prete, <I>false</I> sinon
    */
   protected boolean getImgView(Graphics gr,PlanImage p) {
      try {
         int [] nPixelsRGB=null;
         byte [] nPixels=null;

         boolean flagchange;         // Vrai s'il faut reextraire une vue
         flagBord=false;   // true s'il y a une marge autour de l'image

         // Cr�ation ou r�cup�ration de l'objet Image pour le tracage du fond
         flagchange=getImageBG();

         // Aucune image de base
         if( p==null ) return true;

         // zoom pas pret
         if( rzoom==null  ) return false;

         // L'image de base n'est pas prete
         if( !p.flagOk ) return false;

         // Inutile dans ces 2 cas
         if( p instanceof PlanBG ) return true;
         if( isProjSync() || isNorthUp() /* || isRolled() */ ) return true;

         // Teste s'il faut recommencer a extraire une image
         int imgID = p.getImgID();
         flagchange = (flagchange || lastImgID!=imgID
               || orz==null || !rzoom.equals(orz)
               || oz!=zoom || pHashCode!=p.hashCode() );

         // Test s'il va y avoir des marges autour
         flagBord=( rzoom.x<0 || rzoom.y<0);
         //System.out.println("flagBord="+flagBord+" flagchange="+flagchange);

         if( flagchange ) {
            if( gr!=null && Aladin.isSlow ) waitImg(gr);      // message pour patienter

            // Zoom des pixels
            if( !(p instanceof PlanImageBlink)
                  && !(aladin.frameCM!=null && aladin.frameCM.isDragging()) ) {
               try {
                  if( p.type==Plan.IMAGERGB ) {
                     nPixelsRGB = createZoomPixelsRGB(p,pixelsRGB);
                     if( nPixelsRGB!=pixelsRGB ) { memImg=null; pixelsRGB=nPixelsRGB; }
                  } else {
                     nPixels = createZoomPixels(p,pixels);
                     if( nPixels!=pixels ) { memImg=null; pixels=nPixels; }
                  }
               } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); return true; }

               // Totalement en dehors de l'image
               if( pixels!=null && pixels.length==0 ) { imgFlagDraw=false;  return true;}
               else imgFlagDraw=true;
            }

            // Reg�n�ration du MemoryImageSource n�cessaire ?
            // ATTENTION: LE TEST SUR LE pHashCode SERT A CONTOURNER UN BUG SOUS LINUX POUR FORCER
            // LA RECREATION DU MEMORYIMAGESOURCE
            if( memImg==null || otype!=p.type || oh!=h || ow!=w || pHashCode!=p.hashCode() ) memImg=null;

            // Construction de l'image Blink (elle sera faite apr�s)
            if( p instanceof PlanImageBlink ) {
               memImg=null;
               previousFrameLevel=-1;

               // Construction de l'image
            } else {
               if( memImg!=null ) {
                  try {

                     if( ocm==p.cm ) memImg.newPixels();
                     else {
                        if( p.type==Plan.IMAGERGB ) memImg.newPixels(pixelsRGB,p.cm,0,w);
                        else memImg.newPixels(pixels,p.cm,0,w);
                        ocm=p.cm;
                     }
                  } catch(Exception e) { if( Aladin.levelTrace>+3 ) e.printStackTrace(); memImg=null; }
               }
               if( memImg==null  ) {
                  if( p.type==Plan.IMAGERGB ) memImg = new MemoryImageSource(w,h,p.cm, pixelsRGB, 0,w);
                  else memImg = new MemoryImageSource(w,h,p.cm, pixels, 0,w);

                  memImg.setAnimated(true);
                  try { memImg.setFullBufferUpdates(false); } catch( Exception e) { }
                  if( imgprep!=null ) imgprep.flush();
                  imgprep = getToolkit().createImage(memImg);
               }
            }

            // Memorisation de l'etat pour eviter de refaire le travail
            oz = zoom; otype = p.type; oh = h; ow = w;
            orz = new RectangleD(rzoom.x,rzoom.y,rzoom.width,rzoom.height);
            pHashCode = p.hashCode();
            lastImgID = imgID;
         }

         if( p instanceof PlanImageBlink ) {
            double currentFrameLevel = getCurrentFrameLevel();
            if( currentFrameLevel!=previousFrameLevel ) {

               // Extraction de la prochaine tranche
               previousFrameLevel=currentFrameLevel;

               if( p.type==Plan.IMAGECUBERGB ) {
                  nPixelsRGB=createZoomPixelsRGB(p,pixelsRGB,currentFrameLevel);
                  if( nPixelsRGB!=pixelsRGB ) { memImg=null; pixelsRGB=nPixelsRGB; }
               } else {
                  nPixels = createZoomPixels(p,pixels,currentFrameLevel);
                  if( nPixels!=pixels ) { memImg=null; pixels=nPixels; }
                  if( nPixels==null ) return false;  // Cas ImageHuge non encore pr�te
                  if( nPixels.length==0 ) return true; // En dehors de l'image

                  // PROBLEME DE SYNCHRONISATION, ON RETENTE UN COUP (Pas fantastique je sais !!)
                  if( w*h!=nPixels.length  ) {
                     if( Aladin.isSlow ) waitImg(gr);
                     resetImgID();
                     return false;
                  }
               }

               // G�n�ration de la tranche
               if( memImg==null  ) {
                  if( p.type==Plan.IMAGECUBERGB ) memImg = new MemoryImageSource(w,h,p.cm, pixelsRGB, 0,w);
                  else memImg = new MemoryImageSource(w,h,p.cm,pixels, 0, w);
                  memImg.setAnimated(true);
               } else {
                  //                  memImg.newPixels();
                  if( p.type==Plan.IMAGECUBERGB ) memImg.newPixels(pixelsRGB,p.cm,0,w);
                  else memImg.newPixels(pixels,p.cm,0,w);
               }
               if( imgprep!=null ) imgprep.flush();
               imgprep = getToolkit().createImage(memImg);
               // On va lib�rer l'histogramme des pixels
               //System.out.println("PlanImageBlink update memImg "+memImg+" mem="+aladin.getMem());
               if( aladin.frameCM!=null  ) aladin.frameCM.blinkUpdate(pref);
            }
         }

         if( imgprep==null ) return false;
         w=imgprep.getWidth(this);
         h=imgprep.getHeight(this);
         if( w<0 ) return false;

         // Centrage de l'image si plus petite que view
         if( flagBord  ) {
            imgDx= - rzoom.x*zoom;
            imgDy= - rzoom.y*zoom;

            // Necessaire dans le cas de bords que verticaux ou horizontaux
            if( imgDx<0 ) imgDx=0;
            if( imgDy<0 ) imgDy=0;

         } else imgDx=imgDy=0;

         if( pref.active ) {
            // Ajustements n�gatifs de la premi�re colonne et ligne de pixel (pixel partiel)
            // A DECOMMENTER SI UTILISATION ANCIENNE METHODE DANS createZoomView()
            //            if( zoom>=1 ) {
            //               if( rzoom.x>=0 ) ddx = floor( (rzoom.x - floor(rzoom.x))*zoom);
            //               else ddx=0;
            //
            //               if( rzoom.y>=0 ) ddy = floor( (rzoom.y - floor(rzoom.y))*zoom);
            //               else ddy=0;
            //
            //               imgDx-=ddx;
            //               imgDy-=ddy;
            //            } else ddx=ddy=0;

            ddx=ddy=0;
         }

         this.dx = (int)Math.floor(imgDx);
         this.dy = (int)Math.floor(imgDy);

         return true;

      } catch( OutOfMemoryError e ) {
         e.printStackTrace();
         if( System.currentTimeMillis()-OUTOFMEMTIME > 10000L ) {
            aladin.warning("Out of memory error !!\nPlease restart Aladin with more memory\n\n(ex: java -Xmx1024m -jar Aladin.jar", 1);
            OUTOFMEMTIME=System.currentTimeMillis();
         }
         return false;
      }
   }

   // Date du dernier affichage du warning de d�passement de m�moire
   static private long OUTOFMEMTIME = 0L;

   int ddx,ddy;
   double imgDx,imgDy;   // Position de l'image dans la vue
   boolean imgFlagDraw=true;  // Passe � false si l'image sort totalement de la vue

   /** Retourne true si la vue est plus grande que l'image (pr�sence de bords) */
   protected boolean hasBord() { return flagBord; }

   /** Remplissage du fond suivant la bonne couleur */
   protected void drawBackground(Graphics g) {
      if( g==null ) return;
      try {
         if( pref!=null && pref.colorBackground!=null) {
            g.setColor(pref.colorBackground);
         } else {
            g.setColor( pref!=null && (pref.type==Plan.IMAGE || pref.type==Plan.IMAGEHUGE)
                  && pref.active
                  && ((PlanImage)pref).video==PlanImage.VIDEO_NORMAL ? Color.black : Color.white );
         }
         g.fillRect(1,1,getWidth()-2,getHeight()-2);

         if( pref!=null && pref instanceof PlanBG && pref.active ) {
            ((PlanBG)pref).drawBackground(g, this);
         }
      } catch( Exception e ) {
         g.setColor(Color.white);
         g.fillRect(1,1,getWidth()-2,getHeight()-2);
      }
   }

   /** Dessin du foreground
    * mode 0x1 image, 0x2 overlay
    */
   protected void drawForeGround(Graphics g,int mode, boolean flagBordure) {
      Rectangle clip = g.getClipBounds();
      int m = isFullScreen()?0:2;
      g.setClip(m, m, rv.width-2*m, rv.height-2*m);
      if( flagBordure && pref!=null && pref instanceof PlanBG ) {
         if( (mode&0x1)!=0 && pref.isPixel() || (mode&0x2)!=0 && pref.isOverlay() ) {
            ((PlanBG)pref).drawForeground(g, this);
         }
      }

      if( (mode&0x2)!=0 && aladin.getOrder()>=0) drawHealpixMouse(g);
      g.setClip(clip);

   }


   /** Initialisation d'un tableau.
    * @param array Le tableau a initialiser
    * @param offset L'indice du premier element du tableau concerne
    * @param len    Longueur de l'initialisation
    *              (NECESSAIREMENT UNE PUISSANCE DE 2)
    * @param value  La valeur a y mettre
    */
   //   protected static void bytefill(byte[] array, int offset, int len, byte value) {
   //      int i;
   //
   //      for( i=0; i<len; i++ ) array[offset+i]=value;
   //
   //      // INUTILE AVEC LES JVM AVEC JIT
   ////      if( len<4 ) {
   ////         for( i=0; i<len; i++ ) array[offset+i]=value;
   ////      } else {
   ////         array[offset]=value;
   ////         for( i=1; i<len; i+=i ) System.arraycopy(array,offset, array,offset+i,i);
   ////      }
   //   }


   /** Definit le filtrage sur les pixels couleurs de toute l'image
    */
   protected void setFilter(int [] triangle, int color) {
      ((PlanImageRGB)pref).filterRGB (triangle, color);
      view.repaintAll();
      zoomview.repaint();
   }

   /** Sp�cification du cr�dit */
   protected void drawCredit(Graphics g, int dx, int dy ) {
      if( !aladin.CREDIT ) return;
      g.setColor(Aladin.GREEN);
      g.setFont(Aladin.SITALIC);
      g.drawString(CREDIT,dx+4, dy+rv.height-2);
   }

   /** Dessin du rep�re  */
   protected void drawRepere(Graphics g, int dx, int dy) {
      aladin.view.repere.reprojection(this);  // On reprojette syst�matiquement !!
      if( !aladin.calque.hasReticle() || !Projection.isOk(getProj()) ) return;
      if( aladin.calque.reticleMode==2
            && aladin.view.repere.type==Repere.TARGET ) aladin.view.repere.type=Repere.TARGETL;
      else if( aladin.calque.reticleMode==1
            && aladin.view.repere.type==Repere.TARGETL ) aladin.view.repere.type=Repere.TARGET;
      aladin.view.repere.draw(g,this,dx,dy);
   }

   /** Dessin des FOV  */
   protected void drawFovs(Graphics g, ViewSimple v, int dx, int dy) {
      Fov fov;
      Projection proj=getProj();

      // si pas de projection, on peut quand m�me dessiner les fovs, en prenant une projection par d�faut
      // on en profite pour afficher l'�chelle
      if( isFree() || proj==null ) {
         if( calque.curFov==null || calque.curFov.length==0 || calque.curFov[0]==null ) return;
         proj = calque.fovProj!=null ? calque.fovProj : PlanFov.getProjection(calque.curFov);
         if( calque.flagOverlay ) drawScale(g,v,dx,dy);
         // on remet le zoom en place pour que les Fov se trouvent au milieu
         aladin.calque.zoom.newZoom();
      }

      // champ courant
      if( calque.curFov != null) {
         Fov f;
         for( int i=0; i<calque.curFov.length; i++ ) {
            f = calque.curFov[i];
            if( f==null ) continue;
            f.draw(proj,this,g,dx,dy,Color.red);
         }
      }

      // champ du cutout
      if( calque.cutoutFov != null) {
         Fov f;
         for( int i=0; i<calque.cutoutFov.length; i++ ) {
            f = calque.cutoutFov[i];
            if( f==null ) continue;
            f.draw(proj,this,g,dx,dy,Color.blue);
         }
         //calque.cutoutFov.draw(proj,zoomview,g,dx,dy,Color.blue);
      }

   }

   /** Retourne la coordonnee du centre de la vue
    * sous la forme d'une chaine, "" si probl�me
    */
   protected String getCentre() {
      Coord coo = getCooCentre();
      if( coo==null ) return "";
      return aladin.localisation.J2000ToString(coo.al,coo.del);
   }

   /** Retourne les coordonn�es du centre de la vue
    * @return coordonn�es centrales, ou null
    */
   protected Coord getCooCentre() {
      if( isFree() ) return null;
      //      Projection proj=pref.projd;
      Projection proj = getProj();
      if( isFree() || !Projection.isOk(proj) ) return null;
      double x = rv.width/2.;
      double y = rv.height/2.;
      PointD p = getPosition(x,y);
      Coord coo = new Coord();
      coo.x = p.x;
      coo.y = p.y;
      proj.getCoord(coo);
      if( Double.isNaN(coo.del) ) return null;
      return coo;

   }

   /** Retourne les coordonn�es des 4 coins dans le sens HG,HD,BD,BG */
   protected Coord [] getCooCorners() { return getCooCorners(pref.projd); }
   protected Coord [] getCooCorners(Projection proj) {
      if( isFree() || !Projection.isOk(proj) ) return null;
      Coord coo[] = new Coord[4];
      for( int i=0; i<4;i++ ) {
         double x = i==0 || i==3 ? 0 : rv.width;
         double y = i<2 ? 0 : rv.height;
         PointD p = getPosition(x,y);
         coo[i] = new Coord();
         coo[i].x = p.x;
         coo[i].y = p.y;
         proj.getCoord(coo[i]);
      }
      return coo;

   }


   private Coord [] couverture = null;
   private short oCouverture = -1;

   /** Retourne une couverture r�guli�re de la zone visible dans la vue */
   protected Coord [] getCouverture() {
      if( aladin.view.mustDrawFast() )  return new Coord[0];
      if( oCouverture!=iz) {
         oCouverture=iz;
         int nseg = Math.max(4,(int)getTaille()/6);
         Projection proj=getProj();
         if( isFree() || !Projection.isOk(proj) ) couverture = null;
         else {
            couverture = new Coord[(nseg+1)*(nseg+1)];
            double segx = (double)rv.width/nseg;
            double segy = (double)rv.height/nseg;
            PointD p;
            for( int i=0; i<couverture.length; i++ ) {
               couverture[i] = new Coord();
               p = getPosition((i%(nseg+1))*segx,(i/(nseg+1))*segy);
               couverture[i].x = p.x;
               couverture[i].y = p.y;
               proj.getCoord(couverture[i]);
            }
         }
      }
      return couverture;
   }

   private final Coord coo1 = new Coord();
   private final Coord coo2 = new Coord();
   double tailleRA=0;
   double tailleDE=0;
   short oTailleRA=-1,oTailleDE=-1;

   static double ZOOMBGMIN = 1/64.;

   /** Retourne la taille en RA en degr�s de la vue courante */
   protected double getTailleRA() {
      if( oTailleRA!=iz ) {
         Projection proj=getProj();
         double max = proj.getRaMax();
         if( (zoom<=ZOOMBGMIN || proj.t==Calib.MOL && zoom<=ZOOMBGMIN*2) && pref instanceof PlanBG ) {
            tailleRA=max;
         } else {
            double w = rv.width;
            double h = rv.height;
            PointD p1,p2;

            p1 = getPosition(w/2.,h/2);
            p2 = getPosition(w,h/2);
            coo1.x = p1.x; coo1.y = p1.y;
            coo2.x = p2.x; coo2.y = p2.y;
            proj.getCoord(coo1);
            proj.getCoord(coo2);
            if( Double.isNaN(coo1.al) || Double.isNaN(coo2.al) ) tailleRA=max;
            else {
               tailleRA = Coord.getDist(coo1,coo2)*2;
               if( tailleRA>max ) tailleRA=max;
            }
         }
         oTailleRA=iz;
      }
      return tailleRA;
   }

   /** Retourne la taille en DE en degr�es de la vue courante */
   protected double getTailleDE() {
      if( oTailleDE!=iz ) {
         Projection proj=getProj();
         double max = proj.getDeMax();
         if( (zoom<=ZOOMBGMIN || proj.t==Calib.MOL && zoom<=ZOOMBGMIN*2) && pref instanceof PlanBG ) {
            tailleDE=max;
         } else {
            double w = rv.width;
            double h = rv.height;
            PointD p1,p2;

            p1 = getPosition(w/2,h/2);
            p2 = getPosition(w/2,h);
            coo1.x = p1.x; coo1.y = p1.y;
            coo2.x = p2.x; coo2.y = p2.y;
            proj.getCoord(coo1);
            proj.getCoord(coo2);
            if( Double.isNaN(coo1.del) || Double.isNaN(coo2.del) ) tailleDE=max;
            else {
               tailleDE=Coord.getDist(coo1,coo2)*2;
               if( tailleDE>max ) tailleDE=max;
            }
         }
         oTailleDE=iz;
      }
      return tailleDE;
   }

   //   boolean flagAllSky=false;

   /** Retourne true si on affiche la totalit� du ciel */
   public  boolean isAllSky() {
      Projection proj;
      if( pref==null || !Projection.isOk(proj=getProj()) ) return false;
      return getTailleDE()==proj.getDeMax()
            || getTailleRA()==proj.getRaMax();
   }

   /** Retourne la taille en degr�s de la vue courante */
   protected double getTaille() {
      return Math.max(getTailleRA(),getTailleDE());
   }

   /* Retourne la taille angulaire (en deg) d'un pixel de la vue */
   public double getPixelSize() {
      return getTailleDE()/rv.height;
   }

   /** Retourne la taille de la vue courante
    *  @param mode 0 - RA x DE (defaut)
    *              1 - max(RA,DE)
    *              2 - sqrt(RA*RA+DE*DE) major� par 180�
    */
   protected String getTaille(int mode) {
      if( isFree() || !Projection.isOk(getProj()) ) return null;
      double RAw = getTailleRA();
      double DEw = getTailleDE();
      if( RAw==0 || DEw==0 ) return "";

      switch(mode) {
         case 0: return Coord.getUnit(RAw) +" x "+Coord.getUnit(DEw);
         case 1: return Coord.getUnit( Math.max(RAw,DEw) );
         case 2: double t = Math.sqrt(RAw*RAw+DEw*DEw);
         return Coord.getUnit( Math.min(t, 180) );
      }
      return "";
   }

   /** Retourne l'epoque de la projection courante de la vue courante */
   protected String getEpoch() {
      Projection proj;
      if( pref!=null && Projection.isOk(proj=getProj()) && proj.c!=null ) {
         double epoch=proj.c.GetEpoch();
         if( !Double.isNaN(epoch)
               && epoch!=2000) return epoch+"";   // LE TEST 2000 DOIT ETRE VIRE DES QUE BOF AURA FAIT LE NECESSAIRE
         else return null;
      }
      return null;
   }

   /** Retourne l'ID A1,... identifiant la vue */
   protected String getID() { return aladin.view.getIDFromNView(n); }

   /** Retourne un paragraphe d�crivant l'�tat de la vue (voir commande status) */
   protected String getStatus() {
      return
            "ViewID  "+getID()+"\n"
            + "Centre  "+getCentre()+"\n"
            + "Size    "+getTaille(0)+"\n"
            + "Zoom    "+zoom+"\n"
            + (Projection.isOk(getProj()) ?"Proj    ["+pref+"] "+getProj().getName()+"\n":"")
            + "Status  "+ (selected ? "selected":"unselected")
            + (locked ? " locked":"")
            + (northUp ? " northUp":"")
            + "\n"
            ;
   }

   /** Affichage de la taille du champ courant. S'il s'agit d'une
    * impression (dx==0) on met aussi la position centrale
    */
   protected void drawSize(Graphics g,int dx,int dy) {
      if( !aladin.calque.hasSize() ) return;
      int mode=1;  // Par d�faut on l'affiche au milieu en bas

      // Si le cadre est trop petit, on n'affiche rien
      // sauf s'il s'agit d'un impression, alors on met uniquement
      // la position (� droite en bas)
      if( rv.width<200 ) {
         if( dx==0 ) return;
         mode=0;

         // si cadre moyen et impression, on met � gauche
      } else if ( dx>0 && rv.width<300 ) mode=0;

      String s1 = dx>0?getCentre():"";
      String s = s1+(dx>0?" / ":"")+getTaille(0);

      int marge = getMarge();
      int taille = g.getFontMetrics().stringWidth(s);
      int x = mode==0?marge:mode==1?rv.width/2-taille/2:
         rv.width- taille-marge;

      if( x+taille > rv.width ) s=s1;
      //      g.setColor( getGoodColor(x+dx,rv.height-marge+2 + dy-10,taille,15));
      int y=rv.height-marge+3;
      //      Util.drawCartouche(g, x+dx, y-11+dy, taille, 14, CARTOUCHE, null, Color.white);
      //      g.setColor( Aladin.BLACKBLUE);
      //      g.drawString(s, x+dx, y+dy);
      Util.drawStringOutline(g, s, x+dx, y+dy, Color.cyan,null);

   }

   /** Retourne la marge pour les infos en fonction de la taille
    * de la vue */
   private int getMarge() {
      if( rv.width<100 ) return 4;
      if( rv.width<200 ) return 7;
      return 10;
   }

   /** Retourne une couleur bleut�e qui se voit sur le rectangle indiqu� */
   protected Color getGoodColor(int x1,int y1, int w, int h ) {
      //      int x = x1-(int)Math.round(imgDx);
      //      int y = y1-(int)Math.round(imgDy);
      int x = x1-Math.round(margeX);
      int y = y1-Math.round(margeY);
      try {
         if( pref.type==Plan.ALLSKYIMG && pref.active && !isAllSky()
               && ((PlanBG)pref).color ) return Color.cyan;
         if( isFree()
               || (/* !(pref.isSimpleImage() */ pref.isOverlay() && pref.active)
               || w*h==0 || x>this.w || y>this.h || x+w<0 || y+h<0 ) return Color.blue;
         double pix=0;
         for( int j=y; j<y+h; j++ ) {
            for( int i=x; i<x+w; i++ ) {
               int offset = j*this.w + i;
               if( offset<0 || offset>=pixels.length ) continue;
               pix += (pixels[offset] & 0xFF);
            }
         }
         pix /=w*h;
         if( ((PlanImage)pref).video== (pref.type==Plan.ALLSKYIMG ? PlanImage.VIDEO_INVERSE : PlanImage.VIDEO_NORMAL) ) pix = 255-pix;
         return pix<128 ? Color.blue : Color.cyan;
      } catch( Exception e ) { return Color.blue; }
   }

   //   /** Dessine la chaine en blanc avec un bord de la couleur courante */
   //   protected void myDrawString(Graphics gr,String s, int x, int y) {
   //      Graphics2D g = (Graphics2D)gr;
   //      g.setFont(new Font("Times",Font.BOLD,14));
   //      Color c = g.getColor();
   //      TextLayout tl = new TextLayout(s,g.getFont(),g.getFontRenderContext());
   //      AffineTransform transform = new AffineTransform();
   //      transform.setToTranslation(x, y);
   //      Shape outline = tl.getOutline(transform);
   //      g.setColor(Color.white); g.drawString(s,x,y);
   //      g.setColor(Color.black); g.draw(outline);
   //      g.setColor(c);
   //   }

   // Barres verticales du dessin du cadenas
   static final private int TX[][] = {
      {4,6,0},  {3,7,1},  {2,4,2},  {6,8,2},
      {1,9,6},  {3,7,10}, {0,10,14}, {1,9,15}
   };

   // Barres horizontales du dessin du cadenas
   static final private int TY[][] = {
      {3,5,2},  {3,5,3},  {3,5,7},  {3,5,8},
      {8,12,5}, {7,14,0}, {7,14,10}
   };

   /** Dessine le cadenas */
   protected void drawLock(Graphics g, int x,int y,Color c) {
      if( !aladin.calque.flagOverlay ) return;
      g.setColor(c);
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][0]+x,TX[i][2]+y,TX[i][1]+x,TX[i][2]+y);
      for( int i=0; i<TY.length; i++ ) g.drawLine(TY[i][2]+x,TY[i][0]+y,TY[i][2]+x,TY[i][1]+y);
   }

   /** Dessine le signe de synchronisation des vues */
   protected int drawSync(Graphics g, int x,int y, Color c) {
      int r=3;
      int L=12;
      g.setColor(c);
      g.drawRect(x-r,y-r,2*r,2*r);
      g.drawLine(x-L/2,y,x-1,y);
      g.drawLine(x+1,y,x+L/2,y);
      g.drawLine(x,y-L/2,x,y-1);
      g.drawLine(x,y+1,x,y+L/2);
      return x+L;
   }


   /** Affichage du label de la vue */
   protected void drawLabel(Graphics g,int dx,int dy) {
      if( !aladin.calque.hasLabel() ) return;
      Color c = g.getColor();
      String s=isPlotView() ? plot.getPlotLabel() :
         pref.isCube() ? pref.getFrameLabel(getCurrentFrameIndex()) : pref.label;
         if( s==null ) return;

         int x=getMarge()+dx;
         int y=5+getMarge()+dy;

         //      int len;
         //      FontMetrics fm = g.getFontMetrics();
         //      for( len=fm.stringWidth(s); s.length()>4 && len>rv.width-100; s=s.substring(0,s.length()-2), len=fm.stringWidth(s) );
         //      Util.drawCartouche(g, x, y-11, len, 15, CARTOUCHE, null, Color.white);
         //      g.setColor( locked ? Color.red : Aladin.BLACKBLUE );
         //      g.drawString(s,x, y);
         //      g.setColor(c);

         Util.drawStringOutline(g, s, x, y, Color.yellow, locked ? Color.red : null);

   }

   /** Affichage du target de la vue */
   private void drawTarget(Graphics g,int dx,int dy) {
      if( !aladin.calque.hasTarget() ) return;

      // Tracage de la position demand�e � l'origine
      if( aladin.calque.hasTarget()
            && pref.isImage()
            && ((PlanImage)pref).orig!=PlanImage.LOCAL
            && ((PlanImage)pref).co!=null ) {
         Repere a = new Repere(pref,((PlanImage)pref).co);
         a.setType(Repere.ARROW);
         a.setSize(15);
         ViewSimple vs = getProjSyncView();
         a.projection(vs);
         a.draw(g,vs,dx,dy);
      }
   }

   /** Retourne l'indice de la frame courante dans le cas d'un plan blink de r�f�rence.
    * Si la valeur comporte une partie d�cimale, elle repr�sente l'opacit� [0..1] de la frame
    * courante par rapport � la prochaine frame. par exemple 5.33 indique que la frame courante
    * est la 5 et qu'elle repr�sente le 1/3 de la contribution avec la frame suivante pour
    * un fondu enchain� */
   protected double getCurrentFrameLevel() {
      if( cubeControl==null ) return 1;
      double frame = getCurrentFrameIndex();
      if( frame==-1 ) frame=0;
      double transparency = cubeControl.getTransparency();
      if( transparency==-1 || transparency==0 ) return frame;
      if( transparency==1 ) return frame<cubeControl.nbFrame ? frame+1 : 0;
      return frame+transparency;
   }

   /** Retourne l'indice de la frame courante dans le cas d'un plan blink de r�f�rence. */
   protected int getCurrentFrameIndex() {
      double f = cubeControl.getCurrentFrameIndex();
      if( f==-1 ) f=0;
      return (int)f;
   }


   /** Affiche si n�cessaire les logos de contr�les pour les images Blink */
   private void drawBlinkControl(Graphics g) {
      if( !isPlanBlink() ) return;
      int x,y;
      boolean fullScreen = isFullScreen();

      // Calcul de la taille et de la position en fonction de la taille
      // de la vue et du nombre de vues simultan�es
      int size=rv.width>=200?8:6;

      if( cubeControl.SIZE==-1 ) {
         cubeControl.init(size);
         getCurrentFrameIndex();
         return;
      }

      if( aladin.view.getModeView()<=ViewControl.MVIEW9 || fullScreen ) {
         //         x=rv.width-blinkControl.getWidth()-10;
         x=rv.width/2 - cubeControl.getWidth()/2;
         y =5 + ( fullScreen ? 13:0);
      } else {
         x=rv.width-cubeControl.getWidth()-10;
         y =rv.height-cubeControl.getHeight()-size-3;
      }

      // Mise � jour des pixels 8 bits
      if( cubeControl.mode!=cubeControl.SLIDE ) {
         if( selected ) pref.activeCubePixels(this);
      }

      // Affichage du blinkControl
      cubeControl.draw(g,x,y,size, getCurrentFrameIndex(), pref.getDepth());
   }

   /** Retourne les coordonn�es de la rose des vents */
   protected Coord getNECentre(Projection proj) {
      if( isFree() || !Projection.isOk(proj) ) return null;
      double x = rv.width-15;
      double y = rv.height-15;
      PointD p = getPosition(x,y);
      Coord coo = new Coord();
      coo.x = p.x;
      coo.y = p.y;
      proj.getCoord(coo);
      if( Double.isNaN(coo.del) ) return null;
      return coo;

   }

   // Retourne la taille de la rose des vents
   private int getNESize() {
      int L = rv.width/20;
      if( L>50 ) L=50;
      else if( L<16 ) L=16;
      return L*2;
   }

   // Retourne true si la coordonn�e se trouve dans la rose des vents
   private boolean inNE(int x,int y) {
      if( !(pref instanceof PlanBG) ) return false;
      if( !isFullScreen() && aladin.toolBox.getTool()!=ToolBox.PAN ) return false;
      int L = (int)(getNESize()*1.5);
      int lX = rv.width-L;
      int lY = rv.height-L;
      if( isFullScreen() ) {
         lX = aladin.fullScreen.getContentPane().getWidth()-L;
         lY = aladin.fullScreen.getContentPane().getHeight()-L;
      }
      return x>lX && y>lY;
   }

   /** Positionnement d'un repere Nord et Est */
   protected void drawNE(Graphics g,Projection proj,int dx,int dy) {
      //flagAllSky=false;
      if( !aladin.calque.hasNE() || isAllSky() ) return;

      if( rv.width<150 || rv.width<200 && dx>0) return;
      double x,y,x1,y1,x2,y2,alpha;
      Coord c1,c;
      double delta = 1/60.;

      double L = getNESize()/2.;

      //      boolean flagBG = pref instanceof PlanBG;
      boolean flagBG=false;

      try {
         c=getNECentre(proj);

         // On n'affiche pas le rep�re si on traverse le nord ou le sud
         //         if( c.del+height>90. || c.del-height<-90. ) return;

         if( flagBG ) {
            c1 = aladin.localisation.ICRSToFrame(c);
            x=c1.x; y=c1.y;
         } else {
            x=c.x; y=c.y;
            c = aladin.localisation.ICRSToFrame(c);
         }

         c1 = new Coord(c.al,c.del+delta);
         if( !flagBG ) c1 = aladin.localisation.frameToICRS(c1);
         //         proj.getXYNative(c1);
         proj.getXY(c1);
         if( Double.isNaN(c1.x) ) return;
         x1=c1.x; y1=c1.y;
         alpha = Math.atan2(y1-y,x1-x);
         x1 = x+L*Math.cos(alpha); y1 = y+L*FastMath.sin(alpha);

         c1 = new Coord(c.al+delta/FastMath.cos(c.del*Math.PI/180.),c.del);
         if( !flagBG ) c1 = aladin.localisation.frameToICRS(c1);
         //         proj.getXYNative(c1);
         proj.getXY(c1);
         if( Double.isNaN(c1.x) ) return;
         x2=c1.x; y2=c1.y;
         alpha = FastMath.atan2(y2-y,x2-x);
         x2 = x+L*FastMath.cos(alpha); y2 = y+L*FastMath.sin(alpha);

         // On force l'affichage orthogonal si on est proche du Nord pile-poil en haut
         if( northUp && Math.abs( 180-Math.toDegrees(alpha))<1 ) { x1=x; y2=y; }

         double maxX = Math.max(Math.max(x,x1),x2)+15;
         double maxY = Math.max(Math.max(y,y1),y2)+15;
         x  += rv.width-maxX;
         x1 += rv.width-maxX;
         x2 += rv.width-maxX;
         y  += rv.height-maxY;
         y1 += rv.height-maxY;
         y2 += rv.height-maxY;

         if( northUp ) {
            g.setColor(Color.red);
         } else {
            g.setColor( Color.cyan);
            //            g.setColor( getGoodColor((int)Math.min(dx+x,dx+x1)-(int)L,(int)Math.min(dy+y,dy+y1),(int)L,(int)L));
         }
         Util.drawFlecheOutLine(g,dx+x,dy+y,dx+x1,dy+y1,5,"N");
         Util.drawFlecheOutLine(g,dx+x,dy+y,dx+x2,dy+y2,5,"E");
      } catch( Exception e ) { return; }
   }

   /** Pas d'incr�ments en d�clinaison */
   static final double [] PASD = {
      1./(60*10),1./(60*5),1./(60*3),1./(60*2),
      1./60, 2./60, 5./60, 10.0/60, 15.0/60, 20.0/60, 30.0/60,
      1, 2, 3, 5, 10, 15, 20, 30,
      1*60, 2*60, 5*60, 10*60, 15*60, };

   /** Pas d'incr�ments en ascension droite */
   static final double [] PASA = {
      1./(60*10),1./(60*5),1./(60*3),1./(60*2),
      1./60, 2./60, 5./60, 10.0/60, 15.0/60, 30.0/60, 75.0/60, 150.0/60, 225.0/60,
      5, 7.5, 15, 20, 25, 30, 45, 75, 150, 225,
      5*60, 7.5*60, 15*60, 30*60, };

   /** Dans le calcul d'une grille de coordonn�es, cherche le meilleur
    * pas d'incr�ment
    * @param x la valeur a approch�e
    * @param PAS le tableau des incr�ments possibles
    * @return le la valeur du meilleur pas
    */
   private double goodPas(double x,double PAS[] ) {
      int cran=0;
      double min = Double.MAX_VALUE;
      for( int i=1; i<PAS.length; i++ ) {
         double diff = Math.abs(PAS[i]-x);
         if( min>diff ) { min=diff; cran=i; }
      }
      return PAS[cran];
   }

   /** D�tecte si un segment de grille de coordonn�es et en dehors du champ. */
   protected boolean horsChamp(Segment oseg,Segment seg) {
      boolean rep=true;
      if( seg.x1>=0-100 && seg.x1<rv.width+100 &&
            seg.y1>=0-100 && seg.y1<rv.height+100 ) rep=false;
      if( seg.x2>=0-100 && seg.x2<rv.width+100 &&
            seg.y2>=0-100 && seg.y2<rv.height+100 ) rep=false;
      seg.horsChamp=rep;
      if( oseg==null ) return rep;
      return rep && oseg.horsChamp;
   }

   /** Ajout d'un segment de tra�age dans la grille. Si le segment
    * mord sur la marge de gauche ou du haut, il lui associe un
    * label
    * @param seg
    */
   private void addGrilleSeg(Segment seg,boolean allsky) {
      int a1 = northUpDown ? seg.x1 : seg.y1;
      int a2 = northUpDown ? seg.x2 : seg.y2;
      int b1 = !northUpDown ? seg.x1 : seg.y1;
      int b2 = !northUpDown ? seg.x2 : seg.y2;
      if( !allsky ) {
         if(  seg.iso==Segment.ISODE && (a1*a2<0 || (a1*a2==0 && (a1>0 || a2>0))) ) {
            seg.labelMode=northUpDown?Segment.GAUCHE:Segment.HAUT;
//            seg.label = aladin.localisation.frameToString(seg.al1,seg.del1);
//            int i = seg.label.indexOf(' ');
//            seg.label = zeroSec(seg.label.substring(i+1));

            seg.label = aladin.localisation.getGridLabel(seg.al1,seg.del1,1,getProj().sym);

         } else if(  seg.iso==Segment.ISORA && (b1*b2<0 || (b1*b2==0 && (b1>0 || b2>0))) ) {
            seg.labelMode=northUpDown?Segment.HAUT:Segment.GAUCHE;
//            seg.label = aladin.localisation.frameToString(seg.al1,seg.del1);
//            int i = seg.label.indexOf(' ');
//            seg.label = zeroSec(seg.label.substring(0,i));
            
            seg.label = aladin.localisation.getGridLabel(seg.al1,seg.del1,0,getProj().sym);
         }
      }
      grille.addElement(seg);
   }

//   /** Tronque les centi�mes de secondes, voire les secondes afin que les labels
//    * de la grille ne soient pas trop longs */
//   private String zeroSec(String s) {
//      char a[]=s.toCharArray();
//      int flagDecimal=0;
//      int flagZero=0;
//
//      for( int i=a.length-1; i>0 && (a[i]<'1' || a[i]>'9'); i--) {
//         if( a[i]=='.') flagDecimal=i;
//         else if( a[i]=='0' ) flagZero=i;
//         else if( a[i]==':') return s.substring(0,i);
//      }
//      if( flagDecimal>0 ) return s.substring(0,flagDecimal);
//      if( flagZero>0 ) return s.substring(0,flagZero);
//      return s;
//   }


   private Hashtable memoSeg; // Table de hachage de m�morisation des noeuds de la grille
   private final Boolean ok = new Boolean(true);  // Valeur bidon pour memoSeg
   protected long oiz=-1;
   private Vector grille = null;  // Memorise les segments de la grille de coordonn�es


   /** Initialisation de la grille de coordonn�es et
    * de la Hashtable de m�morisation des noeuds */
   private void initMemoSegment() {
      grille = new Vector(4000);
      memoSeg=new Hashtable(4000);
   }

   /** Lib�ration de la m�morisation des noeuds de la grille de coord */
   private void freeMemoSegment() { memoSeg=null; }

   /** M�morisation des segments de la grille (sur les noeuds uniquement)
    * en utilisant une table de hachage dont la cl� correspond au
    * x1,y1,x2,y2 du segment. On les prends en ordre croissant afin
    * de ne pas compter le segment dans les 2 sens
    * @param seg le segment � m�moriser
    * @return true si le segment n'a pas d�j� �t� m�moris�
    */
   private boolean memoSegment(Segment seg) {
      int x1,y1,x2,y2;
      if( seg.x1>seg.x2 ) { x1=seg.x2; x2=seg.x1; }
      else { x1=seg.x1; x2=seg.x2; }
      if( seg.y1>seg.y2 ) { y1=seg.y2; y2=seg.y1; }
      else { y1=seg.y1; y2=seg.y2; }
      String cle  = x1+":"+y1+"/"+x2+":"+y2;
      if( memoSeg.get(cle)!=null ) return false;
      memoSeg.put(cle,ok);
      return true;
   }

   /** M�thode r�cursive de calcul des 3 voisins d'un segment
    * d'une grille de coordonn�es. Ajoute les segments retenus dans Vector grille
    * Ne retient que les segments ayant au moins un bout dans le champ
    * de vue, et qui n'ont pas encore �t� calcul�s.
    * Le Nord et le Sud ne peuvent �tre donn�s (pas 3 voisins)
    * Dans le cas o� la grille a plus de 2000 segments, le processus r�cursif
    * est interrompu.
    * @param oseg Le segment d'o� l'on vient
    * @param p La projection utilis�
    * @param sens le sens d'o� l'on vient (0 � 3 pour les 4 directions, -1 si pas de direction)
    * @param dra Le pas d'incr�ment en longitude/RA
    * @param dde Le pas d'incr�ment en latitude/DE
    * @param rajc,dejc point d'origine de la grille
    */
   private void calcul3Voisins(Segment oseg, int osens,double dra, double dde,
         double rajc,double dejc,boolean labelAllSky,int depth,double limiteSegmentSize) {
      if( depth>1000 ) return;
      for( int sens=0; sens<4; sens++ ) {
         Segment seg = oseg.createNextSegment();
         switch(sens) {
            case 0: seg.del2+=dde; seg.iso=Segment.ISORA; break;
            case 1: seg.del2-=dde; seg.iso=Segment.ISORA; break;
            case 2: seg.al2+=dra;  seg.iso=Segment.ISODE; if( seg.al2>=360. )  seg.al2-=360.;  break;
            case 3: seg.al2-=dra;  seg.iso=Segment.ISODE; if( seg.al2<=0   )    seg.al2+=360.;  break;
         }
         if( oseg.del2<= -91. || oseg.del2>= 91. ) return; // On ne traverse pas les p�les
         if( seg.del2==oseg.del1 && seg.al2==oseg.al1 ) continue; // Je reviens sur mes pas
         //         if( !seg.projection(this) ) continue;

         // On triche pour pouvoir continuer la r�cursivit� afin de s'approcher du bord
         if( !seg.projection(this) ){
            seg.x2=-1; seg.y2=-1;
         }
         if( horsChamp(oseg,seg) ) continue;
         if( !memoSegment(seg) ) continue;

         if( labelAllSky ) {
            if( seg.al2==rajc && seg.al1==rajc ) {
               seg.labelMode=Segment.MILIEURA;
//               seg.label = aladin.localisation.frameToString(seg.al1,seg.del1);
//               int i = seg.label.indexOf(' ');
//               seg.label = zeroSec(seg.label.substring(i+1));
               seg.label = aladin.localisation.getGridLabel(seg.al1,seg.del1,1,getProj().sym);
            } else if( seg.del2==dejc && seg.del1==dejc ) {
               seg.labelMode=Segment.MILIEUDE;
//               seg.label = aladin.localisation.frameToString(seg.al1,seg.del1);
//               int i = seg.label.indexOf(' ');
//               seg.label = zeroSec(seg.label.substring(0,i));
               seg.label = aladin.localisation.getGridLabel(seg.al1,seg.del1,0,getProj().sym);


            }
         }

         if( !seg.horsChamp && !subdivise(sens==osens?oseg:null,seg,0,labelAllSky,limiteSegmentSize) ) continue;

         calcul3Voisins(seg,sens,dra,dde,rajc,dejc,labelAllSky,depth+1,limiteSegmentSize);
      }
   }

   /** Ins�re un segment dans la grille en le subdivisant si besoin est
    * dans le cas o� le rayon de courbure serait trop grand.
    * Pour calculer ce rayon de courbure, soit on connait le segment pr�c�dent
    * orient� dans le m�me sens (c�d oseg!=null), soit on subdivise le segment
    * � ins�rer. L'angle form� doit �tre inf�rieur � 6�. Si ce n'est pas le cas
    * on va tenter d'ins�rer r�cursivement le segment en le coupant en 2.
    * @param oseg Le segment pr�c�dent si on le connait, sinon null
    * @param seg Le segment � ins�rer
    * @param p La profondeur de la r�cursivit�
    * @return false si le segment n'est pas tra�able
    */
   private boolean subdivise(Segment oseg,Segment seg,int p,boolean allsky,double limiteSegmentSize) {
      Segment s0,s1;

      try {
         // Le segment pr�c�dent est inconnu, on va subdiviser le segment � ins�rer
         // afin de connaitre le rayon de courbure
         if( oseg==null ) {
            Segment s[] = seg.subdivise(this);
            if( s==null ) return false;
            s0=s[0]; s1=s[1];
         }

         // sinon on compare le segment pr�c�dent et celui � ins�rer
         else { s0=oseg; s1=seg; }

         // On ins�rer le segment, si il n'est pas trop courb� et si
         // �a taille est inf�rieure � une certaine valeur en fonction du zoom courant.
         double taille = seg.distXY();

         if( zoom<1 ) limiteSegmentSize *= zoom;
         if( limiteSegmentSize<20 ) limiteSegmentSize=20;

         if( p>5 || !Segment.courbe(s0,s1) && taille<limiteSegmentSize ) {
            if( taille<limiteSegmentSize ) addGrilleSeg(seg,allsky);
            if( grille.size()>4000 ) return false; // Y a sans doute un probl�me
            return true;
         }

         // Cas o� on ne connait pas le segment pr�c�dent, on va lancer
         // r�cursivement la subdivision sur les 2 moiti�s du segment � ins�rer
         if( oseg==null ) {
            boolean rep=subdivise(null,s0,p+1,allsky,limiteSegmentSize);
            return subdivise(s0,s1,p+1,allsky,limiteSegmentSize) && rep;
         }

         // Idem mais dans le cas o� l'on connait le segment pr�c�dent
         Segment s[] = seg.subdivise(this);
         if( s==null ) return false;
         boolean rep=subdivise(oseg,s[0],p+1,allsky,limiteSegmentSize);
         return subdivise(s[0],s[1],p+1,allsky,limiteSegmentSize) && rep;
      } catch( Exception e ) { return false; }
   }

   /** Nombre de cellules de la grille de coordonn�es en fonction
    * du niveau de multivue */
   static final int NBCELL[] = {5,4,2,2,2};

   // True si je trace la grille en antialias (d�pend de la vitesse du dernier trac�)
   private boolean antiAliasGrid=true;
   private int gridHpxOrder=3;
   
   private int getLastGridHpxOrder() { return gridHpxOrder; }
   
   // Dessine les constellations
   private void drawConstellation(Graphics g,int dx,int dy) {
      if( !aladin.calque.hasConst() ) return;
      if( aladin.view.constellation==null ) aladin.view.constellation = new Constellation(aladin);
      
      // Affichage en semi transparence
      Stroke st = null;
      if( g instanceof Graphics2D ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         st = ((Graphics2D)g).getStroke();
         ((Graphics2D)g).setStroke(new BasicStroke(0.3f));
      }


      aladin.view.constellation.draw(g,this,dx,dy);
      
      if( st!=null ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
         ((Graphics2D)g).setStroke(st);
      }

   }
   
   /** Dessine la grille HEALPix */
   private void drawGridHpx(Graphics g,Rectangle clip,int dx,int dy) {
      
      // R�cup�ration de l'ordre le plus appropri�
      int order=3;
      long nside=8;
      double taille = getTaille();
      if( taille<30 ) {
         for( order=CDSHealpix.MAXORDER; order>=3; order-- ) {
            nside = Healpix.pow2(order);
            double resDeg = CDSHealpix.pixRes(nside)/3600;
            if( taille/resDeg<8 ) break;
         }
         if( order<3 ) order=3;
      }
      
      // m�morisation pour usage �ventuel dans moveRepere pour affichage dans la console
      gridHpxOrder=order;
      
      // R�cup�ration du frame courant
      int frame = aladin.localisation.getFrameGeneric();
      if( frame==-1 || frame==Localisation.SGAL ) return;   // Pas de syst�me de coord ou non support� par HEALPix
      
      CDSHealpix hpx = new CDSHealpix();
      
      // r�cup�ration de la liste des losanges HEALPix qui couvre le champ de vue
      // dans le syst�me de coordonn�es courant
      // Si le champ est <30�, on va utiliser une requ�te par rectangle
      long[] npix=null;
      int max=0;
      if( taille>125 ) max = (int)( nside*nside*12 );
      else {
         Coord [] coo = getCooCorners();
         ArrayList<double[]> a = new ArrayList<double[]>();
         for( Coord c : coo ) {
            c=Localisation.frameToFrame(c,Localisation.ICRS,frame);
            a.add(new double[]{c.al,c.del});
         }
         try { npix = hpx.query_polygon(nside, a); }
         catch( Exception e ) { return; }
      }
      
      // Affichage de la grille en semi transparence
      Stroke st = null;
      if( g instanceof Graphics2D ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         st = ((Graphics2D)g).getStroke();
         ((Graphics2D)g).setStroke(new BasicStroke(0.4f));
      }

      
      for( int i=0; i< (npix==null ? max : npix.length); i++ ) {
         long pix = npix==null ? i : npix[i];
         HealpixKey hpix = new HealpixKey(order, pix, frame);
         if( hpix.isOutView(this) ) continue;
         hpix.drawLosangeBorder(g, this);
//         hpix.drawRealBorders(g, this);
      }
      
      if( st!=null ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
         ((Graphics2D)g).setStroke(st);
      }

   }
   
   /** Dessine et calcule si besoin est la grille de coordonn�es */
   protected void drawGrid(Graphics g,Rectangle clip,int dx,int dy) {
      Projection proj;
      if( isFree() || (proj=getProj())==null ) return;

      Font f = g.getFont();
      g.setColor(view.gridColor);
      long t = Util.getTime();
      
      if( calque!=null && calque.gridMode==2 ) {
         g.setFont( new Font("SansSerif",Font.PLAIN,view.gridFontSize) );
         drawGridHpx(g, clip, dx, dy);
         g.setFont(f);
         return;
      }
      
      g.setFont( new Font("SansSerif",Font.ITALIC,view.gridFontSize) );

      // (Re)calcul de la grille
      if( oiz!=iz ) {
         initMemoSegment();
         Coord c = new Coord();

         oiz=iz;

         // D�termination du cosd dans le syst�me Equat/Gal/SGal
         double cosd;
         c.al = proj.raj;
         c.del = proj.dej;
         c = aladin.localisation.ICRSToFrame(c);
         try { cosd = Util.cosd(c.del); }
         catch(Exception e) { cosd=1.; }

         boolean fullScreen = isFullScreen();

         // D�termination du pas en RA et en DE en fonction du champ de vue
         int nb = fullScreen ? 6 : NBCELL[ViewControl.getLevel(aladin.view.getModeView())];
         double rd = getTailleDE()*60.;
         if( rd==0. || Double.isNaN(rd) ) rd=60*360.;
         double pasd = goodPas(rd/nb,PASD)/60.;
         double ra=getTailleRA()*60.;
         if( Math.abs(cosd)<0.000001 ) ra=360*60;
         else ra = rd/cosd;

         double pasa=0;
         // Est-ce que un pole est proche du  champ de vue ?
         c.al = 0; c.del = 90; c = aladin.localisation.frameToICRS(c);
         boolean in1 = isInView(c.al,c.del,500);
         c.al = 0; c.del = -90; c = aladin.localisation.frameToICRS(c);
         boolean in2 = isInView(c.al,c.del,500);
         if( in1 || in2 )  pasa=30;
         else {
            if( ra>360*60. ) ra=360*60.;
            pasa = goodPas(ra/nb,PASA)/60.;
         }

         // Est-ce que le Nord et sur le cot� ?
         try { c = proj.c.getImgCenter(); }
         catch( Exception e ) { e.printStackTrace(); return; }
         double ndel = c.del+pasd;
         if( ndel>90 ) ndel=c.del-pasd;
         Coord c1 = new Coord(c.al,ndel);
         proj.getXYNative(c1);
         if( Double.isNaN(c1.x) ) return;
         double x1 = c1.x-c.x;
         double y1 = c1.y-c.y;
         northUpDown= Math.abs(x1)<= 0.707*Math.sqrt(x1*x1+y1*y1);


         // D�termination du point de d�part
         // Allsky => le centre de projection sinon le point de la grille le plus proche du centre de l'image
         if( isAllSky() ) try { c=proj.getProjCenter(); } catch( Exception e ) { return; }
         else {
            Point p1 = getPosition(rv.width/2,rv.height/2);
            c.x=p1.x;
            c.y=p1.y;
            proj.getCoord(c);
         }
         // en dehors du ciel ! on prend alors le centre de la projection comme point de d�part
         // comme pour un allsky
         if( Double.isNaN(c.al) ) {
            try { c=proj.getProjCenter(); } catch( Exception e ) { return; }
         }
         c = aladin.localisation.ICRSToFrame(c);
         double rajc = c.al - c.al%pasa;
         double dejc = c.del - c.del%pasd;

         //System.out.println("Computing grid: center on "+c.getSexa()+" ("+c.getUnit(ra/60.)+"x"+c.getUnit(rd/60.)+" cells: "+
         //                c.getUnit(pasa)+" x "+c.getUnit(pasd)+") "+(northUpDown?"North left/right":"North up/down"));

         // On initialise avec un segment de taille nulle sur
         // le point d'origine de la grille
         Segment seg = new Segment();
         seg.al2 = rajc; seg.del2 = dejc;
         seg.projection(this);
         seg = seg.createNextSegment();

         // Calcul de tous les segments de la grille (m�thode r�cursive de proche
         // en proche).
         //         double limiteSegmentSize = proj.t==Calib.AIT || proj.t==Calib.MOL ? 50 : proj.t==Calib.TAN || proj.t==Calib.SIP ? 4000 : 2000;
         double limiteSegmentSize=50;
         calcul3Voisins(seg,-1,pasa,pasd,rajc,dejc,isAllSky() && getFullSkySize()>200,0,limiteSegmentSize);
         freeMemoSegment();
      }

      // Affichage de la grille en semi transparence
      //      g.setColor(view.gridColor);
      Stroke st = null;
      if( g instanceof Graphics2D ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
         st = ((Graphics2D)g).getStroke();
         ((Graphics2D)g).setStroke(new BasicStroke(0.5f));
      }

      if( aladin.view.opaciteGrid!=1f ) {
         Enumeration e = grille.elements();
         int j=0;
         Graphics2D g2d = null;
         Composite saveComposite = null;
         if(  (g instanceof Graphics2D) ) {
            g2d = (Graphics2D)g;
            saveComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, aladin.view.opaciteGrid));
         }
         while( e.hasMoreElements() ) {
            Segment seg = (Segment)e.nextElement();
            if( g2d!=null ) seg.draw(g2d,this,clip,j++,dx,dy);
            else seg.draw(g,this,clip,j++,dx,dy);
         }
         drawGridBord(g2d,dx,dy);

         // on restaure le composite
         if( g2d!=null ) g2d.setComposite(saveComposite);

         // Activation sans semi transparence
      } else {
         Enumeration e = grille.elements();
         int j=0;
         while( e.hasMoreElements() ) {
            Segment seg = (Segment)e.nextElement();
            seg.draw(g,this,clip,j++,dx,dy);
         }
         drawGridBord(g,dx,dy);
      }
      if( st!=null ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
         ((Graphics2D)g).setStroke(st);
      }
      g.setFont(f);
      long t1 = Util.getTime()-t;
      tGridMoy+=t1;
      nGridMoy++;
      //      System.out.println("draw grid in "+t1+"ms antialias="+antiAliasGrid);
      antiAliasGrid = (tGridMoy/nGridMoy)<20;
   }

   long tGridMoy=15*30;
   int nGridMoy=30;

   /** Retourne la taille (en pixels de la vue) du champ fullsky dans les projections non infinies */
   private int getFullSkySize() {
      Projection proj = getProj();
      if( proj.t!=Calib.SIN && proj.t!=Calib.ARC && proj.t!=Calib.AIT
            && proj.t!=Calib.MOL && proj.t!=Calib.ZEA ) return Integer.MAX_VALUE;
      Coord c = proj.c.getProjCenter();
      proj.getXYNative(c);
      PointD center = getViewCoordDble(c.x, c.y);
      double signe = c.del<0?1:-1;
      c.del = c.del + signe*( proj.t==Calib.SIN ? 89 : 179);
      proj.getXYNative(c);
      PointD haut = getViewCoordDble(c.x, c.y);
      double deltaY = haut.y-center.y;
      double deltaX = haut.x-center.x;
      return (int)(Math.abs(Math.sqrt(deltaX*deltaX+deltaY*deltaY))*2);
   }

   private void drawGridBord(Graphics g,int dx, int dy) {

      Projection proj = getProj();

      if( proj.t==Calib.SIN || proj.t==Calib.ARC || proj.t==Calib.ZEA ) {
         Coord c = proj.c.getProjCenter();
         proj.getXYNative(c);
         PointD center = getViewCoordDble(c.x, c.y);
         double signe = c.del<0?1:-1;
         c.del = c.del + signe*( proj.t==Calib.SIN ? 89.99 : 179.99);
         proj.getXYNative(c);
         PointD haut = getViewCoordDble(c.x, c.y);
         double deltaY = haut.y-center.y;
         double deltaX = haut.x-center.x;
         int rayon = (int)(Math.abs(Math.sqrt(deltaX*deltaX+deltaY*deltaY)));
         int x = (int)(center.x-rayon);
         int y = (int)(center.y-rayon);
         g.drawOval(x+dx,y+dy,rayon*2,rayon*2);
      } else if( proj.t==Calib.AIT || proj.t==Calib.MOL ) {
         Projection p =  proj.copy();
         double angle = -p.c.getProjRot();
         p.setProjRot(0);
         p.frame=Localisation.ICRS;
         p.setProjCenter(0,0.1);
         Coord c =p.c.getProjCenter();
         p.getXYNative(c);
         PointD center = getViewCoordDble(c.x, c.y);
         double signe = c.del<0?1:-1;
         double del=c.del;
         c.del += signe*89.99;
         p.getXYNative(c);
         PointD haut = getViewCoordDble(c.x, c.y);
         c.del = del;
         c.al-=179.99;
         p.getXYNative(c);
         PointD droit = getViewCoordDble(c.x, c.y);

         int petitAxe = (int)(haut.y-center.y)+1;
         int grandAxe = (int)(droit.x-center.x)+1;
         int x = (int)(center.x-grandAxe);
         int y = (int)(center.y-petitAxe);
         if( angle==0 ) g.drawOval(x+dx,y+dy,grandAxe*2,petitAxe*2);
         else Util.drawEllipse(g, x+dx+grandAxe, y+dy+petitAxe, grandAxe, petitAxe, angle);
      }
   }

   // Pas de l'echelle (en arcsec)
   static final double [] PASE = { 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.02, 0.05,
      0.1, 0.2, 0.5, 1.0, 5.0, 15.0, 30.0, 60.0, 900.0,
      1800.0, 3600.0, 18000.0, 36000.0, 54000.0 };

   /** Dessin de l'echelle de la Vue.
    * @param g Le contexte graphique
    */
   protected int drawScale(Graphics g,ViewSimple vs,int dx,int dy){
      double y;
      int i;

      Projection proj = vs.getProj();

      int X,Y;
      X=Y=getMarge();
      Y = rv.height-Y;

      if( isAllSky() ) return X;

      // Omis pour une impression dont le cadre est trop petit
      if( dx>0 && rv.width<300 ) return X;

      double w = rv.width/4.;

      if( aladin.calque.hasScale() ) {

         // Determination de la taille du repere (au max d'1/4 de la vue)

         Coord coo = vs.getCooCentre();
         if( coo!=null ) {
            Coord coo1 = new Coord();
            coo1.x = coo.x; coo1.y = coo.y+w/zoom;
            proj.getCoord(coo1);
            if( !Double.isNaN(coo1.al) ) {

               y = Coord.getDist(coo, coo1);

               // Determination de l'echelle la plus appropriee
               for( i=0; i<PASE.length; i++ ) if( PASE[i]>y*3600.0 ) break;
               if( i>0 ) i--;

               // On determine la taille exacte du repere
               y = PASE[i]/3600.0;
               coo1.al=coo.al;
               coo1.del=coo.del+y;
               proj.getXY(coo1);
               if( !Double.isNaN(coo1.x) ) {
                  w = Math.sqrt( (coo1.y - coo.y)*(coo1.y - coo.y) + (coo1.x - coo.x)*(coo1.x - coo.x)) *zoom;
                  w = ItoHI(w);

                  // Conversion dans une unite adequate
                  String s=Coord.getUnit(y,true,false);

                  // Affichage
                  int bord = aladin.view.getModeView()>4?2:4;
                  X+=dx; Y+=dy;

                  g.setColor( Color.black ) ;
                  g.fillRect(X-1,Y-bord-1,3,bord+2);
                  g.fillRect(X-1,Y-1,(int)w+2,3);
                  g.fillRect(X+(int)w-1,Y-bord-1,3,bord+2);

                  //                   g.setColor( getGoodColor(X,Y-bord,(int)w,bord)) ;
                  g.setColor( Color.cyan ) ;
                  g.drawLine(X,Y-bord,X,Y);
                  g.drawLine(X,Y,X+(int)w,Y);
                  g.drawLine(X+(int)w,Y,X+(int)w,Y-bord);


                  //                   g.drawString(s,X+3,Y-bord+1);
                  Util.drawStringOutline(g,s,X+3,Y-bord+1, null, Color.black);
               }
            }
         }
      }

      if( dx==0 ) X=drawViewState(g,X+(int)w+7,Y-15);
      return X;

   }


   protected int drawViewState(Graphics g,int x,int y) {
      Color c = g.getColor();

      // Logo pour les vues lock�es
      if( locked ) { drawLock(g,x,y,Color.red); x+=15; }

      // Logo pour les plans r�-�chantillon�s
      if( pref instanceof PlanImageResamp
            && ((PlanImageResamp)pref).isResample() ) {
         aladin.toolBox.tool[0].drawRsampIcon(g,Color.red,x,y,false);
         x+=15;
      }
      g.setColor(c);
      return x;

   }

   // Message d'attente de l'image dans la vue
   void waitImg(Graphics gr ) {
      if( gr==null ) return;
      gr.setFont( Aladin.LLITALIC );
      FontMetrics m = gr.getFontMetrics();
      gr.setColor( Color.red );
      String s = "New view";
      gr.drawString(s,rv.width/2-m.stringWidth(s)/2,rv.height/2 - m.getHeight()/2);
      s = "in progress...";
      gr.drawString(s,rv.width/2-m.stringWidth(s)/2,rv.height/2 + m.getHeight()/2);
   }

   /** Dessin de la surface couverte par le Grabit */
   private void drawGrabIt(Graphics g,double x1,double y1, double x2, double y2) {
      g.drawLine((int)Math.round(x1-5),(int)Math.round(y1),(int)Math.round(x1+5),(int)Math.round(y1));
      g.drawLine((int)Math.round(x1),(int)Math.round(y1-5),(int)Math.round(x1),(int)Math.round(y1+5));
      double dx = x1-x2;
      double dy = y1-y2;
      double r = Math.sqrt(dx*dx+dy*dy);
      g.drawOval((int)(x1-r),(int)(y1-r),(int)(r*2),(int)(r*2));
   }

   /** Retourne la couleur des infos de services en fonction
    * du plan de reference. S'il est normal, on prend le vert,
    * s'il est inverse ou blanc ou prend le noir
    */
   protected Color getInfoColor() {
      if( !isFree() && pref.isImage()
            && ((PlanImage)pref).video==PlanImage.VIDEO_NORMAL ) return Color.green;
      return Color.black;
   }

   /** Tracage en XOR du rectangle de s�lection */
   private void drawRectSelect(Graphics gr) {
      gr.setColor( Color.green );
      //      gr.setXORMode( Color.red );
      //      if( orselect!=null ) {
      //         gr.drawRect(orselect.x,orselect.y, orselect.width, orselect.height);
      //         orselect = null;
      //      }
      if( rselect!=null ) {
         Util.drawArea(aladin,gr,rselect,Color.green);
         //         orselect = new Rectangle( rselect.x,rselect.y,rselect.width,rselect.height);
      }
      //      gr.setPaintMode();
   }

   // Jamais d'effacement de la vue
   public void update(Graphics gr ) {
      if( aladin.view.getNbView()<=n ) return;

      //      if( isLockRepaint() ) return;   // Pas de repaint() pendant un saveView()

      //      waitLockRepaint("update");
      try {
         // Affichage rapide des bordures
         if( quickBordure ) { drawBordure(gr); quickBordure=false; }

         // Affichage rapide du blinkControl
         else if( flagBlinkControl ) { drawBlinkControl(gr); flagBlinkControl=false; }

         // Traitement pour le Grabit
         else if( modeGrabIt && cGrabItX!=-1) {
            gr.setXORMode( Color.green );
            if( pGrabItX>=0 ) drawGrabIt(gr,grabItX,grabItY,pGrabItX,pGrabItY);
            drawGrabIt(gr,grabItX,grabItY,cGrabItX,cGrabItY);
            pGrabItX=cGrabItX;
            pGrabItY=cGrabItY;
            gr.setPaintMode();
         }

         // trace du rectangle de selection multiple
         else if( flagDrag && (/*orselect!=null || */rselect!=null) ) {
            drawRectSelect(gr);
            flagDrag=false;
         }

         // Traitement pour montrer/cacher une source
         else if( !Aladin.NOGUI && quickBlink ) { paintBlinkSource(gr); quickBlink=false; }

      } catch( Exception e ) { }

      //      unlockRepaint("update");
      resetClip();
   }

   /** Retourne la vue utilis�e pour synchroniser cette vue (cf Aladin.sync),
    * la vue elle-m�me sinon */
   protected ViewSimple getProjSyncView() {
      if( isProjSync() ) {
         ViewSimple vs = aladin.view.getCurrentView();

         // Peut �tre �gale s'il s'agit d'un cube
         if( vs.pref!=pref ) return vs;
      }
      return this;
   }

   /** Retourne true si la vue est synchronis�e par projection sur une autre vue */
   protected boolean isProjSync() {
      ViewSimple v=aladin.view.getCurrentView();
      return !locked && !isPlotView()  && selected && (v==null || v!=this )
            && aladin.match.isProjSync();

   }

   protected boolean flagPhotometry=false;

   /** Tracage des overlays graphiques
    * @param g le contexte graphique concerne
    * @param clip le cliprect s'il existe, sinon null
    * @param dx,dy l'offset d'affichage uniquement utilise pour les impressions
    * @param now true pour avoir imm�diatement un affichage complet (mode allsky notamment)
    * @param mode 0x1 - image, 0x2 - overlays, 0x3 - both
    * @return true si au moins un plan a ete affiche
    */
   protected boolean paintOverlays(Graphics g,Rectangle clip,int dx,int dy,boolean now) {
      return paintOverlays(g,clip,dx,dy,now,0x3);
   }
   protected boolean paintOverlays(Graphics g,Rectangle clip,int dx,int dy,
         boolean now,int mode) {
      boolean fullScreen = isFullScreen();
      boolean flagBordure=false;   // true si on devra dessiner le cache elliptique pour cacher le feston des PlanBG

      if( isFree() ) {
         drawFovs(g,this,dx,dy);
         if( fullScreen ) { aladin.fullScreen.drawBlinkInfo(g); }
         return false;
      }
      boolean flagDisplay=false;

      // AntiAliasing
      aladin.setAliasing(g,-1);

      Projection proj;
      ViewSimple vs;        // La vue courante sauf s'il y a projSync sur une autre vue
      boolean isProjSync=isProjSync() && g instanceof Graphics2D;
      if( isProjSync ) {
         vs = getProjSyncView();
         proj = vs.getProj();
         vs.flagPhotometry=false;
      } else {
         proj =getProj();
         vs=this;
         vs.flagPhotometry=true;
      }

      if( dx==0 ) drawBlinkControl(g);      // Il ne s'agit pas d'une impression

      Plan [] allPlans = calque.getPlans();
      // Recherche d'un �ventuel Folder contenant le plan de ref
      Plan folder = calque.getMyScopeFolder(allPlans,pref);

      // Rep�rage d'un �ventuel plan sous la souris dans le stack
      PlanImage planUnderMouse = null;

      // Pour afficher des checkboxs associ�s aux plans directement dans la vue
      if( fullScreen ) aladin.fullScreen.startMemo();

      // Affichage en 2 passes, d'abord les images, puis tout le reste
      for( int passe=1; passe <= (OVERLAYFORCEDISPLAY ? 2 : 1); passe++ ) {

         // Dessin des plans les uns apr�s les autres
         for( int i=allPlans.length-1; i>=0; i--) {
            Plan p = allPlans[i];
            if( p.type==Plan.NO || !p.flagOk ) continue;

            // On affiche d'abord les images, puis tout le reste
            if( OVERLAYFORCEDISPLAY ) {
               if( passe==1 && !p.isPixel() ) continue;
               if( passe==2 && p.isPixel() ) continue;
            }

            if( p.isPixel()   && (mode & 0x1) == 0 ) continue;
            if( p.isOverlay() && (mode & 0x2) == 0 ) continue;


            // Seuls les catalogues (et �ventuellement les surcharges graphiques) sont tra�ables dans un plot
            if( isPlotView() && !p.isCatalog() && !p.isTool() ) continue;

            // Rep�rage d'un �ventuel plan sous la souris dans le stack
            if( p.underMouse && p.isImage() ) planUnderMouse = (PlanImage)p;

            margeX=margeY=0;

            // Le plan image de r�f�rence (le cas allsky est trait� apr�s)
            if( p==pref && p.isImage() ) {
               if( p.active ) {
                  
                  if( p instanceof PlanMultiCCD ) ((PlanImage)p).draw(g,vs,dx,dy,1);
                 
                  else if( northUp || isProjSync() /* || isRolled() */ ) {
                     ((PlanImage)p).draw(g,vs,dx,dy,1);

                  } else {
                     
                     
                     double offsetX = imgDx;
                     double offsetY = imgDy;
                     if( pref.type == Plan.IMAGEHUGE ) {
                        if( rzoom.x>=0 ) offsetX -= (int)Math.floor( (rzoom.x - (int)Math.floor(rzoom.x))*zoom);
                        if( rzoom.y>=0 ) offsetY -= (int)Math.floor( (rzoom.y - (int)Math.floor(rzoom.y))*zoom);
                     }
                     if( imgFlagDraw ) {
                        margeX = dx+(int)Math.round(offsetX);
                        margeY = dy+(int)Math.round(offsetY);
                        g.drawImage(imgprep,margeX,margeY,this);
                        
                     }
                  }
               }
               continue;
            }

            if( p==pref && p instanceof PlanBG ) {
               if( p.active ) {
                  ((PlanBG)p).draw(g,vs,dx,dy, 1,now);
                  if( p.isPixel() ) flagBordure=true;
               }
               continue;
            }

            // M�me scope que le plan de r�f�rence ?
            boolean flagDraw;
            // Dans le cas d'un plan de polarisation, il faut qu'il soit hors tout ou
            // dans le m�me folder que le plan de r�f�rence
            if( p.type!=Plan.ALLSKYPOL ) flagDraw = calque.getMyScopeFolder(allPlans,p)==folder;
            else {
               Plan foldpol = calque.getFolder(p);
               flagDraw = foldpol==null || calque.getFolder(pref) == calque.getFolder(p);
            }

            // Activ� ?
            boolean flagActive = flagDraw && p.active;

            // Cas d'un image ou d'un plan BG
            if( (p.isImage() || p instanceof PlanBG ) && Projection.isOk(p.projd) ) {
               if( p.isImage() && (mode & 0x1) == 0 ) continue;
               if( p.isOverlay() && (mode & 0x2) == 0 ) continue;
               if( flagActive && !p.isRefForVisibleView() ) {
                  ((PlanImage)p).draw(g,vs,dx,dy,-1);
                  if( p instanceof PlanBG && p.isPixel() && p.getOpacityLevel()>0.1) flagBordure=true;
               }
               if( fullScreen &&  p.hasObj() && p.isOverlay() ) aladin.fullScreen.setCheck(p);

               // Cas des plans TOOL et CATALOG
            } else {
               if( fullScreen ) {
                  if( p.hasObj() && p.pcat.computeAndTestDraw(this,flagDraw) ) {
                     aladin.fullScreen.setCheck(p);
                  }
               }

               if( !(p instanceof PlanMoc) && p.pcat==null || !p.active ) continue;
               float opacity = p.getOpacityLevel();
               if( opacity >0.05 ) {
                  if( p.getOpacityLevel()<0.9 && g instanceof Graphics2D
                        && !(p instanceof PlanField) ) {
                     Graphics2D g2d = (Graphics2D)g;
                     Composite saveComposite = g2d.getComposite();
                     Composite myComposite = Util.getImageComposite(opacity);
                     g2d.setComposite(myComposite);
                     p.pcat.draw(g2d,clip,vs,flagActive,dx,dy);
                     g2d.setComposite(saveComposite);
                  } else p.pcat.draw(g,clip,vs,flagActive,dx,dy);
               }
            }

            flagDisplay = true;

            // Pour eviter de memoriser dans chaque objet les positions
            // decalees par les offsets de marge (en cas d'impression), je
            // force le recalcul de la projection de tous les plans
            if( dx>0 || dy>0 ) newView(1);
         }
      } // Fin du for de l'affichage en 2 passes

      // Rien de plus si aucune projection
      if( !Projection.isOk(proj) ) {
         if( dx==0 ) drawBlinkControl(g);      // Eh oui !
         if( fullScreen ) { aladin.fullScreen.drawBlinkInfo(g); }
         return false;
      }

      // Le bord de l'image en couleur rouge (plan point� dans la pile par la souris)
      boolean drawBord=false;
      if( planUnderMouse!=null && aladin.view.getMouseView()==null
            && aladin.calque.select.canDrawFoVImg() && g instanceof Graphics2D ) {

         if( planUnderMouse!=oldPlanBord ) aladin.view.activeFoV();

         boolean aplat = planUnderMouse.getOpacityLevel()==0f; // On ne fait pas les aplats pour les images en transparence
         aplat=false;
         planUnderMouse.drawBord(g,vs,dx,dy,1f,aplat);
         drawBord=true;
      }
      if( !drawBord ) oldPlanBord=null;

      if( (mode & 0x2)==0 ) {
         drawForeGround(g,mode,flagBordure);
         return flagDisplay;
      }

      if( vs.isPlotView() ) vs.plot.drawPlotGrid(g,dx,dy);
      else if( calque.hasGrid() && !proj.isXYLinear() ) vs.drawGrid(g,clip,dx,dy);

      if( fullScreen ) g.setFont( Aladin.BOLD);
      else if( rv.width>200 ) g.setFont(Aladin.SBOLD);
      else  g.setFont(Aladin.SSBOLD);

      if( !vs.isPlotView() ) {
         
         // trac� des constellations
         drawConstellation(g,dx,dy);

         // Pourtour cache mis�re
         drawForeGround(g,mode,flagBordure);
      }

      if( calque.flagOverlay  ) {
         drawLabel(g,dx,dy);
         drawTarget(g,dx,dy);
      }

      if( !vs.isPlotView() )  {

        if( !proj.isXYLinear() && calque.flagOverlay ) {
            int x=vs.drawScale(g,vs,dx,dy);
            if( aladin.calque.flagOverlay ) {
               if( isProjSync ) drawSync(g,x+10,rv.height-getMarge()-5,Color.red);
               vs.drawSize(g,dx,dy);
               vs.drawNE(g,proj,dx,dy);
            }
         }

         // (thomas) dessins des fov des images
         // on le place ici pour que le cutout fov puisse se voir m�me
         // lorqu'il correspond parfaitement avec le drawBord
         drawFovs(g,vs,dx,dy);

         // Le repere courant
         vs.drawRepere(g,dx,dy);

         // Tracage de la distance entre 2 objets s�lectionn�s si elle existe
         if( aladin.view.coteDist!=null ) {
            aladin.view.coteDist.projection(vs);
            aladin.view.coteDist.draw(g,vs,dx,dy);
         }

         if( aladin.view.getMouseNumView()==n ) {
            // Tracage du quick Simbad s'il existe
            if( aladin.view.simRep!=null ) {
               aladin.view.simRep.projection(vs);
               aladin.view.simRep.draw(g,vs,dx,dy);
            }
         }
      }

      // Trac� du rainbow
      if( hasRainbow() ) rainbow.draw(g,this,dx,dy);
      if( rainbowF!=null ) rainbowF.draw(g,this,dx,dy);
      

      // Juste parce que le drawForeGround cache en partie
      if( dx==0 ) drawBlinkControl(g);

      return flagDisplay;
   }

   /** Affichage de la bordure rouge et des deux petits triangles
    * indiquant que la vue est stick�e */
   protected void drawStick(Graphics g) {
      Polygon p;
      g.setColor(Color.red);
      g.drawRect(1,1,rv.width-3,rv.height-3);
      int M=rv.width/12;
      if( M>10 )M=15;
      int x=rv.width-1;
      int y=rv.height-1;
      p = new Polygon(new int[]{x,x,x-M},new int[]{1,M,1},3);
      g.fillPolygon(p);
      p = new Polygon(new int[]{1,1,M},new int[]{y,y-M,y},3);
      g.fillPolygon(p);
   }

   static final Color REDC    = new Color(255,100,100);
   static final Color ORANGE = new Color(255,170,100);

   /** Dessin des bordures du cadre en fonction de l'�tat de la vue
    *  et du plan associ�
    */
   protected void drawBordure(Graphics g) {
      if( g==null || aladin.msgOn || aladin.isFullScreen() ) return;
      int w = getWidth();
      int h = getHeight();
      if( !aladin.view.isMultiView() ) {
         if( !Aladin.NOGUI ) Util.drawEdge(g,w,h);
         if( sticked ) drawStick(g);
         return;
      }

      boolean current =  aladin.view.getCurrentView()==this;
      boolean select =  current || selected;
      int syncMode = aladin.match.getMode();
      ViewSimple v = aladin.view.getMouseView();
      boolean showFromStack = v==null && !isFree() && pref.underMouse;
      boolean show = v==this || showFromStack;
      // Le test sur v==null permet de s'assurer que la souris n'est
      // pas actuellement sur une vue mais sur la pile. Sans ce test il y aurait conservation
      // de la bordure verte dans le cas ou la souris passe d'une vue � une autre pour un m�me
      // plan de r�f�rence.

      //System.out.println("current="+current+" select="+select+" show="+show+" showFromStack="+showFromStack+" "+this);

      g.setColor(Color.lightGray);
      g.drawRect(0,0,w-1,h-1);
      g.drawRect(1,1,w-3,h-3);
      g.drawRect(2,2,w-5,h-5);

      if( showFromStack  ) {
         g.setColor(Color.green);
         g.drawRect(0,0,w-1,h-1);
         g.drawRect(1,1,w-3,h-3);
         g.drawRect(2,2,w-5,h-5);
      }
      else if( select  ) {
         Color c;
         if( current ) c = syncMode==3 ? Color.red : Color.blue;
         else c= syncMode==3 ? REDC : Color.blue;
         g.setColor(c);
         g.drawRect(1,1,w-3,h-3);
         g.drawRect(2,2,w-5,h-5);
      }
      else if( show ) {
         g.setColor(Color.green);
         g.drawRect(0,0,w-1,h-1);
         g.drawRect(1,1,w-3,h-3);
         g.drawRect(2,2,w-5,h-5);
      }

      //        if( selected ) g.drawString("selected",5,40);
      //        if( current ) g.drawString("current",5,52);

      if( sticked ) drawStick(g);

      /*String s = current && selected ?"SC":current?"C":selected?"S":"";
g.setColor(Color.red);
g.drawString(s,10,100);
       */   }


   /** Affichage de la bordure uniquement */
   private boolean quickBordure=false;
   protected void paintBordure() {
      if( isFullScreen() ) return;
      quickBordure=true;
      update(getGraphics());
   }

   /** Changement d'�tat d'un plan Blink, retourne le d�lai */
   protected int paintPlanBlink() {
      if( !isPlanBlink() || !pref.active ) return 0;
      if( getCurrentFrameIndex() != previousFrameLevel ) repaint();
      return cubeControl.delay;
   }

   /** Affichage de la bordure uniquement */
   private boolean quickBlink=false;
   protected void paintSourceBlink() {
      quickBlink=true;
      //      repaint();
      update(getGraphics());
   }

   /** Retourne true si le plan de r�f�rence est un plan Blink */
   protected boolean isPlanBlink() {
      return pref!=null && pref.isCube();
      //     return pref!=null && (pref instanceof PlanImageBlink );
   }

   /** Retourne true si il y a au moins un objet blink � montrer
    *  ou � restituer, ou s'il s'agit d'un plan RGB en mode blink.
    */
   protected boolean isSourceBlink() {
      if( isFree() ) return false;
      Enumeration e = showSource.elements();
      while( e.hasMoreElements() ) {
         if( !((Blink)e.nextElement()).isFree() ) return true;
      }
      return false;
   }

   private int oFrame=-1;
   private double oAngle=-1;
   private long oizAngle=-1;


   /** Retourne un identificateur unique de la vue et de son �tat. Si celui-ci a chang�,
    * il est n�cessaire de recalculer les projections dans cette vue */
   public long getIZ() { return ( ((long)hashCode()<<31) | (long)iz<<16); }


   /** Retourne la projection courante pour la vue.
    * Par d�faut il s'agit de pref.projd, mais si on est en mode Nord vers le haut,
    * retourne la projection dont la rotation oriente le Nord du syst�me de r�f�rence vers le haut */
   public Projection getProj() {
      Projection proj;
      if( pref==null ) return null;

      if( isPlotView() ) return plot.getProj();

      proj = projLocal!=null ? projLocal : pref.projd;    // projLocal dans le cas d'un planBG
      //      proj = pref.projd;
      if( proj==null ) return null;
      
//      if( Command.longitude==-1 && !proj.sym ) {
//         System.out.println("proj.setProjSym(true)");
//         proj.setProjSym(true);
//      }
//      else if( Command.longitude==1 && proj.sym ) {
//         System.out.println("proj.setProjSym(false)");
//         proj.setProjSym(false);
//      }
      
      if( !northUp ) return proj;

      // Pour le cas NorthUP
      int frame=aladin.localisation.getFrame();
      if( frame!=oFrame || iz!=oizAngle ) {
         oizAngle=oiz;

         // Centre de l'image
         Coord c1;
         try { c1 = proj.c.getImgCenter(); } catch( Exception e ) { return proj; }

         // Juste un peu au-dessus de le syst�me de r�f�rence courant
         Coord c2 = aladin.localisation.ICRSToFrame(new Coord(c1.al,c1.del));
         c2.del+=1/3600.;
         c2 = aladin.localisation.frameToICRS(c2);

         // Calcul de l'angle au Nord �quatorial
         double dra = c2.al-c1.al;
         double cosc2 = AstroMath.cosd(c2.del);
         double num = cosc2 * AstroMath.sind(dra);
         double den = AstroMath.sind(c2.del) * AstroMath.cosd(c1.del)
               - cosc2 * AstroMath.sind(c1.del) * AstroMath.cosd(dra);
         double angle = (den==0.0)?90.0:Math.atan2(num,den)*180/Math.PI;
         //         if( angle<0.0 ) angle+=360.0;
         oFrame=frame;
         oAngle=angle;
         //               System.out.println("c1="+c1+" c2="+c2+" AngletoNorth = "+angle);
      }
      return proj.toNorth(-oAngle);
   }

   /** Blink.paint(g) tous les objets blinks n�cessaires */
   private void paintBlinkSource(Graphics g) {
      Enumeration e = showSource.elements();
      while( e.hasMoreElements() ) {
         Blink b = (Blink)e.nextElement();
         if( b.isFree() ) continue;
         b.paint(this,g);
      }
   }

   /** Blink.reset() tous les objets blinks n�cessaires */
   protected void resetBlinkSource() {
      Enumeration e = showSource.elements();
      while( e.hasMoreElements() ) {
         Blink b = (Blink)e.nextElement();
         if( b.isFree() ) continue;
         b.reset();
      }
   }

   /** Indique que la nouvelle source � montrer est o. Adapte sa liste
    *  de blinks en fonction
    */
   protected void changeBlinkSource(Source o) {
      if( isFree() ) return;
      boolean trouve=false;
      Enumeration e = showSource.elements();
      //int i=0;
      while( e.hasMoreElements() ) {
         Blink b = (Blink)e.nextElement();
         if( !b.isFree() ) {
            //System.out.println("Je stoppe showSource("+i+") dans "+this);
            b.stop();
         } else if( !trouve && o!=null) {
            //System.out.println("Je d�marre showSource("+i+") pour "+o.id+"  dans "+this);
            b.start(o);
            trouve=true;
         }
         //i++;
      }
      if( !trouve && o!=null ) {
         //       ViewSimple vs=getProjSyncView();
         //       if( vs==null ) vs=this;
         Blink b = new Blink(this);
         b.start(o);
         showSource.addElement(b);
         //System.out.println("J'ai ajout� "+o.id+" dans showSource dans "+this);
      }
   }

   /** Retourne true si cette vue est en fullScreen */
   private boolean isFullScreen() {
      return aladin.isFullScreen() && aladin.fullScreen.viewSimple==this;
   }

   /** D�marre le sablier d'attente de la premi�re image en fullscreen si
    * ce n'est d�j� fait */
   private void startSablier() {
      if( !sablier.isRunning() ) {
         sablier.start();
         view.startTimer(300);
      }
   }

   /** Arr�te le sablier d'attente de la premi�re image en fullscreen */
   private void stopSablier() { sablier.stop(); }

   /** Retourne true si le sablier d'attente de la premi�re image en fullscreen
    * est activ� */
   protected boolean isSablier() { return sablier.isRunning(); }

   /** Trace le sablier si n�cessaire au milieu de la vue */
   private void drawSablier(Graphics g) {
      if( !isSablier() ) return;
      sablier.setCenter(getWidth()/2,getHeight()/2);
      sablier.paintComponents(g);
   }

   static long timeForPaint = 0L;
   private Image imgbuf=null;
   private Graphics gbuf=null;

   protected void resetFlagForRepaint() {
      resetClip();
      quickInfo=flagDrag=quickBlink=flagBlinkControl=quickBordure=false;
   }

   // ATTENTION: gr peut �tre null dans le cas d'un print ou d'un NOGUI
   public void paintComponent(Graphics gr) {
      try { paintComponent1(gr); }
      catch( Exception e ) {
         if( aladin.levelTrace>3 ) e.printStackTrace();
         drawBackground(gr);
         drawBordure(gr);
         if( gr!=null ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            gr.setColor(Color.red);
            gr.drawString("Repaint error ("+e.getMessage()+")",10,15);
         }
      }
      finally {
         aladin.command.setSyncNeedRepaint(false);
         resetClip();
         aladin.view.setPaintTimer();
      }
   }

   // ATTENTION: gr peut �tre null dans le cas d'un print ou d'un NOGUI
   private void paintComponent1(Graphics gr) {
      long t = Util.getTime();

      // Pour forcer le r�affichage total (dans le cas d'un save ou d'un print)
      if( gr==null ) {
         rselect=null;
         resetFlagForRepaint();
         modeGrabIt=false;
         pref.flagProcessing=false;
      }

      boolean fullScreen = isFullScreen();

      // Sablier d'attente de la cas du fullScreen
      if( fullScreen ) {
         if( aladin.calque.waitingFirst() )  {
            startSablier();
            drawSablier(gr);
            return;
         } else stopSablier();
      }

      // Message pour patienter si on est entrain de modifier le plan de ref.
      if( !isFree() && pref.flagProcessing ) {
         drawBackground(gr);
         if( imgprep!=null && gr!=null ) gr.drawImage(imgprep,dx,dy,this);
         waitImg(gr);
         drawBordure(gr);
         if( !flagDrag ) return;
      }

      // Quelle est la taille actuelle du view
      if( !Aladin.NOGUI ) rv = new Rectangle(0,0,getSize().width,getSize().height);

      PlanImage pi = (PlanImage)( (!isFree() && pref.isImage() ) ? pref : null );

      if( !getImgView(gr,pi) || isFree() ) {
         drawBackground(gr);
         drawBordure(gr);

         // Ajout des infos clignotantes pour le mode fullScreen (curseur, voyant clignotant...)
         //         aladin.fullScreen.drawChecks(g);
         if( fullScreen && gr!=null) {
            aladin.fullScreen.drawBlinkInfo(gr);

            // Gestion d'un message d'accueil
            if( aladin.msgOn ) {
               aladin.help.setText( aladin.logo.Help());
               aladin.help.setSize(aladin.fullScreen.getSize());
               aladin.help.paintComponent(gr);
            }
         }

         return;
      }

      // L'affichage complet n�cessite de reseter tous les blinks
      if( !Aladin.NOGUI ) resetBlinkSource();

      // Si aucun clip, on prend au minimum la taille de la fen�tre
      if( clip==null ) clip=(Rectangle)rv.clone();

      //Positionnement des clips rect si necessaire
      if( gr!=null ) setClip(gr);
      if( g!=null ) setClip(g);

      // Buffer du fond
      if( imgbuf==null || imgbuf.getWidth(this)!=rv.width || imgbuf.getHeight(this)!=rv.height ) {
         if( gbuf!=null ) gbuf.dispose();
         imgbuf=aladin.createImage(rv.width,rv.height);
         gbuf=imgbuf.getGraphics();
      }

      if( !(flagDrag || quickInfo || quickBlink || modeGrabIt || flagBlinkControl || quickBordure ||
            view.newobj!=null && view.newobj instanceof Cote  ) ) {
         drawBackground(gbuf);
         paintOverlays(gbuf,clip,0,0,false);
         //System.out.println("paint");
      }

      if( g!=null ) g.drawImage(imgbuf,0,0,this);

      // Cas du fullScreen
      if( fullScreen && g!=null) {

         // Ajout des infos clignotantes pour le mode fullScreen (curseur, voyant clignotant...)
         //         aladin.fullScreen.drawChecks(g);
         aladin.fullScreen.drawBlinkInfo(g);

         // Gestion d'un message d'accueil
//         if( aladin.msgOn ) {
//            aladin.help.setText( aladin.logo.Help());
//            aladin.help.setSize(aladin.fullScreen.getSize());
//            aladin.help.paintComponent(g);
//            return ;
//         }
      }

      // Un objet en cours ?
      if( view.newobj!=null ) {
         g.setColor( getColor() );
         view.newobj.draw(g,getProjSyncView(),0,0);
         if( view.newobj instanceof Cote ) view.newobj.status(aladin);
      }

      // trace du rectangle de selection multiple
      if( flagDrag && rselect!=null ) {
         drawRectSelect(g);
         if( flagDrag && !isScrolling() ) { flagDrag=false;  }
      }

      // trac� du rectangle de crop
      if( view.crop!=null && isFree()
            || aladin.toolBox.tool[ToolBox.CROP].mode!=Tool.DOWN ) view.crop=null;
      else if( hasCrop() ) view.crop.draw(g,this);

      // Dessin des bordures
      drawBordure(g);

      // Suppression du clip
      resetClip();

      // Dessin de la colormap
      if( xDrag!=-1 && !hasRainbow() ) drawColorMap(g);

      if( !rainbowUsed()  ) {

         // Dessin en sur impression de la valeur de pixel, et de la position pour FullScreen
         if( pref!=null && aladin.calque.hasPixel() ) drawPixelInfo(g);
         quickInfo=false;
      }

      if( fullScreen )  {
         //         aladin.fullScreen.drawMesures(g);
         //         aladin.fullScreen.showMesures();
         aladin.fullScreen.drawIcons(g);

         //         drawOverlayControls(g);

         if( aladin.fullScreen.getMode()!=FrameFullScreen.CINEMA ) {
            widgetInit();
            if( widgetControl!=null ) widgetControl.paint(g);
         }
      }

      // Affichage du buffer
      if( gr!=null ) gr.drawImage(img,0,0,this);

      // Statistiques de temps de repaint
      timeForPaint  = (Util.getTime() - t);
      //      System.out.println("ViewSimple paint "+timeForPaint+"ms");

      frameNumber++;
   }

   protected int frameNumber=0;

   private void drawHealpixMouse(Graphics g) {
      if( !(pref instanceof PlanBG) ) return;
      ((PlanBG)pref).drawHealpixMouse(g,this);
   }

   private WidgetController widgetControl=null;
   private final int MG=3;

   private void widgetInit() {
      if( widgetControl!=null ) return;
      widgetControl = new WidgetController();

      // La toolbox
      int width = -1;
      int height = 200;
      aladin.toolBox.createWidgetControl(/*25*/MG,40,width,height,-1f,this);
      widgetControl.addWidget( aladin.toolBox );

      // La pile
      aladin.calque.select.createWidgetControl(getWidth()-aladin.calque.select.getWidth()-MG,50,-1,100,0.7f,this);
      widgetControl.addWidget( aladin.calque.select );

      // Le zoomView
      int x = getWidth()-aladin.calque.zoom.zoomView.SIZE-75;
      int y = getHeight()-aladin.calque.zoom.zoomView.SIZE-MG;
      aladin.calque.zoom.zoomView.createWidgetControl(x,y,aladin.calque.zoom.zoomView.SIZE,aladin.calque.zoom.zoomView.SIZE,0.7f,this);
      
      widgetControl.addWidget( aladin.calque.zoom.zoomView );

      //Et les mesures
      aladin.mesure.mcanvas.createWidgetControl(MG,getHeight()-125-MG,x-MG-10,100,0.7f,this);
      widgetControl.addWidget( aladin.mesure.mcanvas );

      //      aladin.bookmarks.createWidgetControl(50,MG,-1,-1,-1,this);
      //      widgetControl.addWidget( aladin.bookmarks );


      //      aladin.calque.zoom.zoomSlider.createVOC(200,getHeight()-aladin.calque.zoom.zoomSlider.getHeight()-MG,-1,-1,1.0f,this);
      //      voControl.addVOClient(aladin.calque.zoom.zoomSlider);

      //
      //      RainbowPixel r =  new RainbowPixel(aladin, this);
      //      r.createVOC(100,100,300,50, 1f);
      //      voControl.addVOClient( r);
   }


   protected String lastPixel="";

   /** Passage de la valeur du pixel qu'il faut afficher en surimpression de l'image */
   protected void setPixelInfo(String s) {
      if( lastPixel!=null && lastPixel.equals(s) ) return;  // pas de changement
      lastPixel=s;
      quickInfo=true;
      repaint();
   }

   /** Affichage en surimpression en haut � droite de la coordonn�e
    * et de la valeur du pixel sous la souris */
   protected void drawPixelInfo(Graphics g) {
      try {
         //      if( !isFullScreen() ) return;
         //         if( aladin.view.getMouseView()!=this || ) return;
         String pos = aladin.localisation.getLastPosition();
         String pixel = lastPixel;
         int y=15;
         int x;

         // Affichage de la valeur du pixel
         if( !Aladin.OUTREACH && pixel!=null && pixel.length()>0 ) {
            if( pixel.indexOf("unknown")>=0 ) return;
            g.setFont(Aladin.BOLD);
            int len = pixel.charAt(0)=='R' ? 150 : g.getFontMetrics().stringWidth(pixel);
            x = getWidth()-(28+len);

            // Dans le cas de trois composantes couleurs (ex: R:255 G:100 B:20)
            if( pixel.charAt(0)=='R' ) {
               StringTokenizer st = new StringTokenizer(pixel);
               for( int i=0; i<3; i++ ) {
                  String c = st.nextToken().substring(2);
                  g.setColor( i==0 ? Color.red : i==1 ? Aladin.GREEN : Color.blue );
                  Util.drawStringOutline(g, c,x+i*50,y, null,null);
               }
            } else {
               Util.drawStringOutline(g, pixel, x, y, Color.cyan,null);
            }

         }

         // Affichage de la position
         if( isFullScreen() && pos!=null && pos.length()>0 ) {
            g.setFont(Aladin.BOLD);
            x = getWidth()-260;
            Util.drawStringOutline(g, pos, x, y, Color.cyan,null);
         }
      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
   }

   /** Dessine la colormap en superposition de l'image (pendant un ajustement dynamique bouton de droite) */
   private void drawColorMap(Graphics g) {
      if( pref instanceof PlanImageRGB ) return;
      if( !( pref.hasAvailablePixels()
            || pref.type==Plan.ALLSKYIMG && !((PlanBG)pref).isColored() ) ) return;
      if( aladin.frameCM==null ) {
         aladin.frameCM = new FrameColorMap(aladin);
         aladin.frameCM.initCM((PlanImage)pref);
      }
      aladin.frameCM.cm.drawColorMap(g, 1, 1, getWidth(), 20);
   }

   /** Retourne le facteur de zoom courant.
    * Convertit le cas �ch�ant le facteur de zoom en facteur de zoom r�el dans le
    * cas de HUGE image */
   final protected double getZoom() {
      try {
         if( pref.type == Plan.IMAGEHUGE ) return zoom / ((PlanImageHuge) pref).step;
      } catch( Exception e ) { }
      return zoom;
   }

   /** Convertit le cas �ch�ant la coordonn�e image en coordonn�e HUGE image
    * en prenant en compte le pas de sous-�chantillonnage */
   final protected double ItoHI(double x) {
      try {
         if( pref.type == Plan.IMAGEHUGE ) return x / ((PlanImageHuge) pref).step;
      } catch( Exception e ) { }
      return x;
   }

   /** Convertit le cas �ch�ant la coordonn�e HUGE image en coordonn�e image
    * en prenant en compte le pas de sous-�chantillonnage */
   final protected double HItoI(double x) {
      try {
         if( pref.type==Plan.IMAGEHUGE ) return x * ((PlanImageHuge)pref).step;
      } catch( Exception e ) { }
      return x;
   }


   /************************  Gestion du plot de nuages de points ***********************/

   Plot plot = null;

   protected boolean isPlotView() { return plot!=null; }

   /** Cr�ation d'une table. Le nom des colonnes peut �tre mentionn� au moyen de jokers (*,?)
    * @param plan Nom du plan Catalog concern�
    * @param colX nom de la premi�re colonne. Si null ou vide => prend la premi�re colonne
    * @param colY nom de la deuxi�me colonne. Si null ou vide => prend la deuxi�me colone
    * @param openProp true si on va ouvrir les Properties du plan
    * @throws Exception
    */
   protected void addPlotTable(Plan plan, String colX, String colY,boolean openProp) throws Exception {
      Legende leg = plan.getFirstLegende();
      int x = (colX==null || colX.length()==0) ? 0 : leg.matchColIndex(colX);
      if( x<0 ) throw new Exception("Column name \""+colX+"\" not found in plane \""+plan.label+"\"");
      int y = (colY==null || colY.length()==0) ? 0 : leg.matchColIndex(colY);
      if( y<0 ) throw new Exception("Column name \""+colY+"\" not found in plane \""+plan.label+"\"");
      addPlotTable(plan,x,y,openProp);
   }

   protected void addPlotTable(Plan plan, int indexX, int indexY,boolean openProp) {
      if( plot==null ) {
         plot = new Plot(this);
         aladin.log("ScatterPlot","");
      }
      plot.addPlotTable(plan,indexX,indexY,openProp);
      if( openProp ) {
         Legende leg = plan.getFirstLegende();
         aladin.console.printCommand("cview -plot "+plan.label+"("+leg.getName(indexX)+","+leg.getName(indexY)+")");
      }

   }

}

