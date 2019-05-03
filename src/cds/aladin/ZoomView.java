// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cds.astro.Astrocoo;
import cds.tools.Util;

/**
 * Le Zoom montre l'image de base de la ``View frame'' (en vignette) et
 * surcharge cette image d'un rectangle representant la partie visible
 * de l'image dans la ``View frame'' suivant l'echelle courante.
 * Si un objet "Cote" a ete selectionne dans la View avec une image
 * sous-jacente, le Zoom doit afficher une "Coupe" des pixels sous le segment
 * en surcharge de l'image.
 * <P>
 * Une deuxieme fonction permet d'utiliser cet objet pour visualiser
 * en loupe les pixels proches de la souris lorsqu'elle se deplace dans
 * la ``View frame''
 *
 * @see aladin.Zoom()
 * @see aladin.View()
 * @author Pierre Fernique [CDS]
 * @author Anais Oberto [CDS]
 * @version 1.5 : (oct 2017) Incorporation de l'historique des targets
 * @version 1.4 : (nov 2004) Incorporation du cut Graph
 * @version 1.4 : (mars 2004) utilisation du setPixels() pour la loupe + les flêches
 * @version 1.3 : (janv 2004) Incorporation du trace des coupes
 * @version 1.2 : (dec 01) Incorporation image RGB
 * @version 1.1 : (27 mai 99) Recentrage du zoom corrige
 * @version 1.0 : (11 mai 99) Toilettage du code
 * @version 0.91 - 30 novembre 1998 (relecture du code)
 * @version 0.9 - 30 mars 1998
 */
public class ZoomView extends JComponent
implements  MouseWheelListener, MouseListener,MouseMotionListener,Widget {

   // Les valeurs generiques
   protected int WENZOOM     =  8; // Agrandissement pour la loupe (puissance de 2)

   // Les references aux autres objets
   Aladin aladin;

   // les composantes de l'objet
   Image img;                  // Image du buffer du canvas
   Image imgwen;               // Image courante de la loupe
   MemoryImageSource mImgWen;  // MemoryImageSource de la loupe
   Graphics gwen;              // Contexte graphique de la loupe
   Image imgbuf;               // Image reduite a WxW de l'image en cours
   Graphics gbuf;              // Contexte graphique de l'image reduite
   int lastImgID=-1;           // ImgID de l'imagette du zoom courant

   // Les parametres a memoriser pour le zoom
   RectangleD rectzoom;         // Rectangle de zoom
   RectangleD or;               // Precedente position du rectangle de zoom
   boolean flagdrag;           // Vrai si je suis en train de draguer
   boolean imgok;              // vrai si j'ai une image dans le buffer
   Rectangle clip;             // Rectangle du clip
   int W,H;                    // Taille de l'image reduite dans le zoom

   // Les parametres a memoriser pour la loupe
   double xwen,ywen;           // Position de la loupe dans la vue courante
   private PointD oc;           // Precedente position de la loupe dans les coord images
   private int oframe;         // Précédente frame de la loupe si image blink
   boolean zoomok;             // Vrai si on a fini de calculer le zoom
   int xmwen,ymwen;            // Position de la loupe sous la souris
   int xWenrect,yWenrect;      // Coin haut gauche du repère central de la table des pixels
   boolean memInv;             // Memorisation du cas INVERSE pour un plan RGB
   boolean flagRGB;            // true s'il s'agit d'un plan RGB
   boolean flagwen;            // Vrai si c'est la loupe qu'il faut afficher
   // a la place du zoom
   // Les parametres a memoriser pour la coup
   private int[] cut;                // le graphe de coupe du dernier cut en niveau de gris
   private int[] cutR,cutG,cutB;     // le graphe de coupe du dernier cut en RGB
   protected boolean flagCut=false;  // true si une coupe est active
   private Obj objCut;               // L'Objet graphique correspondant au cut que l'on trace

   ZoomTimeControl zoomTimeControl=null;  // Le controle du temps
   ZoomHist hist=null;                  // L'histogramme courant
   SED sed=null;                      // SED courant
   protected boolean flagHist=false;  // true si l'histogramme est actif
   protected boolean flagSED=false;   // true s'il faut afficher le SED courant
   private boolean flagTargetControl=false; // true si on affiche le controle des targets
   
   // Gestion de la synchronization des vues compatibles
   private boolean flagSynchronized=false;  // true - indique que l'on a déjà fait un synchronize des vues (SHIFT)

   static private Color BGD;

   /** creation de la fenetre du zoom.
    * @param aladin,calque References
    */
   protected ZoomView(Aladin aladin) {
      this.aladin = aladin;
      setOpaque(true);
      setBackground(Aladin.BLUE);
      
      zoomTimeControl = new ZoomTimeControl(this);
      
      BGD = Aladin.COLOR_BACKGROUND;

      addMouseWheelListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);

      WENZOOM=8;
   }
   
   protected ZoomView() { super(); }
   
   private void resumeSize() {
      int w = getWidth();
      if( cut!=null && cut.length==w ) return;
      cut = new int[w];
      cutR = new int[w];
      cutG = new int[w];
      cutB = new int[w];
   }

//   static Dimension DIM = new Dimension(Select.ws+MyScrollbar.LARGEUR,Select.ws+MyScrollbar.LARGEUR);
//   public Dimension getPreferredSize() { return DIM; }
   
   public void mouseWheelMoved(MouseWheelEvent e) {
      if( aladin.inHelp ) return;
      if( e.getClickCount()==2 ) return;    // SOUS LINUX, J'ai un double évènement à chaque fois !!!
      if( flagSED )  { if( sed.mouseWheel(e) ) repaint(); return; };
      if( flagHist ) { if( hist.mouseWheelMoved(e) ) repaint(); return; }
      if( flagCut && objCut instanceof Repere ) { 
         ViewSimple vc = aladin.view.getCurrentView();
         vc.cubeControl.incFrame(-e.getWheelRotation());
         aladin.calque.repaintAll();
         return;
      }
      synchronize(e);
      aladin.calque.zoom.incZoom(-e.getWheelRotation());
   }

   protected void free() {
      hist=null;
      sed=null;
      flagHist=flagSED=false;
   }

   public void mouseClicked(MouseEvent e) {};
   
   class JMenuItemExt extends JMenuItem {
      JMenuItemExt(String s) {
         super(s);
         addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
               String s = ((JMenuItem)e.getSource()).getActionCommand(); 
//               System.out.println("Je suis sur l'item "+s);
            }
         });
      }
   }
   
   /** Mémorisation de la position courante du réticule dans l'historique des targets */
   private void memoLoc() {
      aladin.targetHistory.add( aladin.localisation.getLocalisation(aladin.view.repere) );
   }
   
   /** Action à opérer lorsque l'on clique sur le triangle */
   protected void triangleAction(int x,int y) { triangleAction(x,y,0); }
   protected void triangleAction( final int x, final int y, int initIndex ) {
      int max=20;
      ArrayList<String> v =  aladin.targetHistory.getTargets( initIndex, max );
      if( v.size()==0 ) return;
      
      // On crée un JPopupmenu contenant les 10 dernières targets, et s'il y en a encore,
      // ajoute à la fin de la liste une entrée "..." qui permet d'avoir les suivantes
      JPopupMenu popup = new JPopupMenu();
      for( String s: v ) {
         JMenuItem mi = null;
         if( s.equals("...") ) {
            mi = new JMenuItem(s);
            mi.setActionCommand(""+(initIndex+max));
            mi.addActionListener( new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  try {
                     int index = Integer.parseInt( ((JMenuItem)e.getSource()).getActionCommand() );
                     triangleAction(x,y,index);
                  }catch( Exception e1) {}
               }
            });

         } else {
            mi = new JMenuItemExt( s.length()>40 ? s.substring(0,38)+" ..." : s);
            mi.setActionCommand( TargetHistory.getTarget(s) );
            mi.addActionListener( new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  String s = ((JMenuItem)e.getSource()).getActionCommand();
                  aladin.command.execNow(s);
               }
            });
         }
         popup.add(mi);
      }
      
      if( aladin.isFullScreen() ) {
         JComponent c = ((JComponent)aladin.fullScreen.getContentPane());
         c.setComponentPopupMenu(popup);
         WidgetControl wc = getWidgetControl();
         popup.show(c, wc.getX()+x , wc.getY()+y);
        
      } else {
         setComponentPopupMenu(popup);
         popup.show(this, x ,y);
      }
   }


   public void mousePressed(MouseEvent e) {
      if( aladin.inHelp ) return;
      if( flagCut || flagHist || flagSED ) return;
      
      // Actions liées à la liste des targets
      if( rectHistory!=null && flagTargetControl ) {
         if( rectTarget.contains(e.getPoint() ) ) {
            aladin.command.execNow( aladin.targetHistory.getLast() );

         } else if( rectHistory.contains(e.getPoint()) ) {
            triangleAction(e.getX(), e.getY());
            
         } else if( rectLoc.contains(e.getPoint()) ) {
            memoLoc();
            repaint();
         }
         return;
      }

      
      ViewSimple v = aladin.view.getCurrentView();
      v.stopAutomaticScroll();
      
      or = null;
      if( !v.isFree() && v.pref instanceof PlanBG ) setAllSkyCenter(v, e.getX(), e.getY()+DELTAY);
      else {
         flagdrag = true;
         newZoom(e.getX(),e.getY()+DELTAY);
         synchronize(e);
      }
   }

   /** Sélectionne et synchronize toutes les vues compatibles si demandé par SHIFT
    * sinon affiche un texte expliquant que c'est possible le cas échéant */
   protected void synchronize(MouseEvent e) {
      if( e.isShiftDown() && flagSynchronized || !aladin.view.isMultiView() ) return;
      ViewSimple vc=aladin.view.getCurrentView();
      if( vc==null || vc.isFree() || !Projection.isOk(vc.pref.projd) ) return;

      if( e.isShiftDown() ) {
         aladin.view.selectCompatibleViews();
         aladin.view.setZoomRaDecForSelectedViews(aladin.calque.zoom.getValue(),null);
         flagSynchronized=true;
      } else flagSynchronized=false;
   }


   //   public void keyTyped(KeyEvent e) {}
   //   public void keyReleased(KeyEvent e) {}
   //   public void keyPressed(KeyEvent e) {
   //      // Resynchronisation de toutes les vues compatibles
   //      if( e.isShiftDown() ) {
   ////         aladin.view.selectCompatibleViews();
   //         if( aladin.view.switchSelectCompatibleViews() )
   //             aladin.view.setZoomRaDecForSelectedViews(aladin.calque.zoom.getValue(),null);
   //         return;
   //      }
   //   }

   /** Deplacement du rectangle de zoom */
   public void mouseDragged(MouseEvent e) {
      if( aladin.inHelp ) return;
      if( flagSED ) return;
      if( flagHist ) { if( hist.mouseDragged(e) ) repaint(); return; }
      if( flagCut ) {
         if( objCut instanceof RepereSpectrum ) setFrameCube(e.getX());
         return;
      }
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isPlot() ) return;
      flagdrag = true;
      if( !v.isFree() && v.pref instanceof PlanBG ) setAllSkyCenter(v, e.getX(), e.getY()+DELTAY);
      else {
         synchronize(e);
         newZoom(e.getX(),e.getY()+DELTAY);
         drawInViewNow(e.getX(),e.getY()+DELTAY);
      }
   }

   private int cutX=-1;
   private int cutY=-1;

   private String INFO=null,TIPTARGET=null,TIPLISTTARGET=null,TIPSTORETARGET=null;

   /** Dans le cas de l'affichage d'un cut Graph, affichage du de FWHM */
   public void mouseMoved(MouseEvent e) {
      if( aladin.inHelp ) return;
      
      if( INFO==null ) {
         INFO = aladin.chaine.getString("ZINFO");
         TIPTARGET = aladin.chaine.getString("TIPTARGET");
         TIPLISTTARGET = aladin.chaine.getString("TIPLISTTARGET");
         TIPSTORETARGET = aladin.chaine.getString("TIPSTORETARGET");
      }
      
      // S'il y a affichage du controle de l'historique des targets, mise à jour
      // des flags si la souris est dessus
      if( rectHistory!=null ) {
         boolean in1,in2,in3;
         boolean repaint=false;
         in1=rectHistory.contains( e.getPoint() );
         if( in1!=flagHistoryIn ) { flagHistoryIn=in1; repaint=true; }
         if( in1 ) Util.toolTip(this, TIPTARGET);

         in2=rectTarget.contains( e.getPoint());
         if( in2!=flagTargetHistoryIn ) { flagTargetHistoryIn=in2; repaint=true; }
         if( in2 ) Util.toolTip(this, TIPLISTTARGET);

         in3=rectLoc.contains( e.getPoint());
         if( in3!=flagLocHistoryIn ) { flagLocHistoryIn=in3; repaint=true; }
         if( in3 ) Util.toolTip(this, TIPSTORETARGET);

         flagTargetControl = in1 || in2 || in3;
         if( !flagTargetControl ) Util.toolTip(this, "");
         if( repaint ) repaint();
      }

      if( flagSED ) {
         sed.mouseMove(e.getX(),e.getY());
         repaint();
         return;
      }

      aladin.status.setText(INFO);
      cutX=e.getX();cutY=e.getY();
      if( flagHist && hist.setOnMouse(e.getX(),e.getY()) ) {
         repaint();
         if( hist.flagHistPixel ) aladin.view.getCurrentView().repaint();
      }
      else if( flagCut ) repaint();
      
      else zoomTimeControl.mouseMove(e.getX(),e.getY());
   }

   /** Dans le cas de l'affichage d'un cut Graph,
    * fin de l'affichage du de FWHM */
   public void mouseExited(MouseEvent e) {
      if( aladin.inHelp ) return;

      if( flagSED ) { sed.mouseExit(); repaint(); return; }

      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      if( aladin.view.flagHighlight ) {
         hist.resetHighlightSource();
         aladin.view.flagHighlight=false;
         aladin.view.repaint();
      }
      if( flagCut || flagHist ) {
         if( flagHist ) hist.setOnMouse(-1,-1);
         cutX=cutY=-1;
         repaint();
         if( hist!=null &&  hist.flagHistPixel ) aladin.view.getCurrentView().repaint();
      }
   }

   public void mouseEntered(MouseEvent e) {
      if( aladin.inHelp ) { aladin.help.setText(Help()); return; }

//      if( flagSED ) { sed.mouseEnter(); return; }

      boolean resize = flagCut;
      if( objCut instanceof SourceStat ) resize=false;
      if( Ligne.isLigne(objCut) ) resize=false;
      Aladin.makeCursor(this,resize ?Aladin.RESIZECURSOR:Aladin.HANDCURSOR);
   }

   /** Mise à jour de la vue courante en fonction de la position de la souris de le ZoomView */
   protected Coord drawInViewNow(double xmouse,double ymouse) {
      ViewSimple vc = aladin.view.getCurrentView();
      if( vc==null || vc.isFree() ) return null;

      // Calcul de la coordonnée dans la vue courante
      Coord coo = null;
      if( !vc.isPlot() ) {
         try {
            Projection proj;
            if( Projection.isOk(proj=vc.getProj()) ) {
               PointD p = vc.zoomViewToImage(xmouse,ymouse);
               coo = new Coord();
               coo.x=p.x; coo.y=p.y;
               proj.getCoord(coo);
               if( Double.isNaN(coo.al) ) coo=null;
            }
         } catch( Exception e ) { coo=null; }
      }

      // Pas de calib ou une seule vue pour aller au plus vite
      if( /* aladin.view.getNbSelectedView()==1 || */ coo==null ) {
         calculZoom(vc,xmouse,ymouse);
         repaint();
         vc.repaint();

         // Multi-vue
      } else {
         aladin.view.setZoomRaDecForSelectedViews(0,coo,vc,false,true);
         aladin.view.syncView(1,coo,null);
      }

      return coo;
   }


   /** Changement du frame d'un cube via le spectre affiché */
   private void setFrameCube(int xc) {
      ViewSimple vc = aladin.view.getCurrentView();
      int nbFrame = vc.cubeControl.nbFrame;
      int frame = (int)( nbFrame* ( (double)xc/getWidth()));
      if( frame>=nbFrame ) frame=nbFrame-1;
      if( frame<0 ) frame=0;
      vc.cubeControl.setFrameLevel(frame);
      vc.repaint();
   }

   /** Deplacement du rectangle de zoom */
   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) { aladin.helpOff(); return; }
      
      if( flagTargetControl && rectHistory!=null ) return;
      
      if( flagSED ) { sed.mouseRelease(e.getX(), e.getY() ); return; }

      if( flagCut ) {
         if( objCut instanceof SourceStat ) setFrameCube(e.getX());
         flagdrag=false;
         return;
      }

      // Ajustement de la résolution de l'histogramme ou
      // Sélection de toutes les sources highlightées dans l'histogramme courant
      if( flagHist ) {
         if( e.getX()<10 ) {
            if( hist.mouseDragged(e) ) repaint();
         } else {
            if( hist.inCroix(e.getX(), e.getY() ) ) {
               stopHist();
            } else hist.selectHighlightSource();
            aladin.calque.repaintAll();
         }
         return;
      }

      //      if( flagSynchronized ) aladin.view.unselectCompatibleViews();
      flagSynchronized=false;

      ViewSimple v = aladin.view.getCurrentView();

      // Pour voir tout le champ initial
      if( e.isControlDown() || v.isPlot() ) {
         v.reInitZoom(true);
         return;
      }
      //aladin.view.getCurrentView().stopAutomaticScroll();
      Coord coo = drawInViewNow(e.getX(),e.getY()+DELTAY);
      if( coo!=null ) {
         aladin.view.setRepere(coo);
         //         aladin.view.memoUndo(v,coo,null);
      }
      flagdrag=false;
   }

   /** Active/désactive le flag du drag pour le zoom view pour accélérer l'affichage */
   protected void startDrag() { flagdrag=true; }
   protected void stopDrag() { flagdrag=false; or=null; repaint(); }

   /** Calcul et affichage du rectangle du zoom.
    * @param xc,yc    Nouveau centre du zoom
    */
   protected boolean newZoom(double xc, double yc) {
      return newZoom(aladin.view.getCurrentView(),xc,yc);
   }
   protected boolean newZoom(ViewSimple v,double xc, double yc) {
      if( v.locked ) return false;
      if( !calculZoom(v,xc,yc) ) return false;
      repaint();
      return true;
   }

   /** Calcul les dimensions du rectangle du zoom.
    * 1) Recherche le facteur du zoom (x1,x2,x4...)<BR>
    * 2) Determine si le plan de ref est une image ou un catalogue<BR>
    * 3) Calcul la taille du zoom ou fonction du facteur d'agrandissement
    *    et des proportions de la Vue (View Frame)<BR>
    * 4) Corrige un depassement eventuelle des limites
    *
    * @param xc,yc    Nouveau centre du zoom
    * @param centrage <I>true</I>avec centrage, <I>false</I> sinon
    */
   protected boolean calculZoom(ViewSimple v,double xc, double yc) {
      zoomok = false;
      if( v.isFree() ) return false;

      v.setZoomXY(v.zoom,xc,yc);

      // Teste si le nouveau rect zoom a change
      if( rectzoom!=null && v.rzoomView!=null && v.rzoomView.equals(rectzoom) ) {
         zoomok=true;
         return false; // pas modifie
      }
      rectzoom = v.rzoomView;

      // pour mettre à jour le choice Zoom
      aladin.calque.zoom.setValue(v.zoom);

      // Demande de reaffichage
      v.newView();
      zoomok=true;
      return true;
   }

   /** Reset l'imgID mémorisé par le ZoomView pour forcer une nouvelle
    *  extraction de l'imagette
    */
   protected void resetImgID() { lastImgID=-1; }

   /** Generation du zoom dans son buffer Image */
   protected boolean drawImgZoom(ViewSimple v) {
      mImgWen=null;

      int w = getWidth();
      int h = getHeight();

      // Creation du buffer du zoom si necessaire
      if( imgbuf==null || imgbuf.getWidth(aladin)!=w || imgbuf.getHeight(aladin)!=h) {
         if( gbuf!=null ) gbuf.dispose();
         imgbuf=aladin.createImage(w,h);
         gbuf=imgbuf.getGraphics();
      }

      // Recherche du plan de reference
      if( v==null || v.isFree() || !v.pref.isImage() || v.northUp  ) {

         // L'image associe au zoom sera simplement le zoom vide lui-meme
         if( lastImgID!=-2 ) {
            gbuf.setColor( BGD );
            gbuf.fillRect(0,0,w,h);
            drawBord(gbuf);
            lastImgID=-2;
         }
         if( v!=null && !v.isFree() ) calculZoom(v,v.xzoomView,v.yzoomView);
      } else {
         try {
            PlanImage pi = (PlanImage)v.pref;
            
            boolean flagRGB = pi.type==Plan.IMAGERGB || pi.type==Plan.IMAGECUBERGB;
            
            // Dessin de l'image zoomee si cela n'a pas deja ete fait
            if( lastImgID!=pi.getImgID() || 
                  !flagRGB && (pi.pixelsZoom==null || pi.pixelsZoom.length!=w*h) ||
                   flagRGB && (((PlanRGBInterface)pi).getPixelsZoomRGB()==null || ((PlanRGBInterface)pi).getPixelsZoomRGB().length!=w*h)
                   ) {

               // Affichage du zoom suivant l'echelle
               gbuf.setColor( BGD );
               gbuf.fillRect(0,0,w,h);

               Image pimg;
               if( flagRGB ) {
                  ((PlanRGBInterface)pi).calculPixelsZoomRGB();
                  pimg = createImage( new MemoryImageSource(w,h,((PlanRGBInterface)pi).getPixelsZoomRGB(), 0,w));
               } else {
                  pi.calculPixelsZoom();
                  pimg = createImage( new MemoryImageSource(w,h,pi.cm,pi.pixelsZoom,0,w));
               }
               gbuf.drawImage(pimg,0,0,this);
               drawBord(gbuf);

               lastImgID=pi.getImgID();
               calculZoom(v,v.xzoomView,v.yzoomView);
            }
         }
         catch( OutOfMemoryError eOut ) { aladin.gc(); return false; }
         catch( Exception ezoom ) { ezoom.printStackTrace(); System.out.println("Try ezoom error: "+ezoom); return false; }
      }

      // Le buffer du paint sera celui du zoom;
      img = imgbuf;

      return true;
   }

   protected void setAllSkyCenter(ViewSimple v,double xc, double yc) {
      Coord c = new Coord();
      c.x=xc; c.y=yc;
      proj.getCoord(c);
      if( Double.isNaN(c.al) ) return;
      //      aladin.view.gotoThere(c);
      v.goToAllSky(c);
   }

   private int ov=0;
   private short oiz=-2;
   private Projection proj = null;
   private int lastHipsWidth=0,lastHiPSHeight=0;
   int DELTAY=20;

   private void drawHipsControl(Graphics g,ViewSimple v) {
      try {

         int width = getWidth();
         int height = getHeight();
         
         if( lastHipsWidth!=width || lastHiPSHeight!=height ) {
            oiz=-1;
            lastHipsWidth=width;
            lastHiPSHeight=height;
         }

         //       Creation du buffer si necessaire
         if( imgbuf==null || imgbuf.getWidth(aladin)!=width || imgbuf.getHeight(aladin)!=height ) {
            if( gbuf!=null ) gbuf.dispose();
            imgbuf=aladin.createImage(width,height);
            gbuf=imgbuf.getGraphics();
         }
         
         if( oiz!=v.iz || ov!=v.hashCode() ) {
            //         long t = Util.getTime();
            int ga = (4*width)/10;
            int pa;

            gbuf.setColor( BGD );
            gbuf.fillRect(0, 0, width, height);

//            proj = new Projection(null,0,0,0,180*60,0,0,width*0.45, 0,false, Calib.AIT,Calib.FK5);
            Projection lastProj=proj;
            try {
               proj = new Projection(null,0,0,0,2*60,width/2,-height/2+10,width/190., 0,false, Calib.AIT,Calib.FK5);
               proj.frame = Localisation.ICRS;
            }catch( Exception e ) { proj=lastProj; }
            
            oiz=v.iz;
            ov=v.hashCode();
            Coord c = new Coord(0,0);
            proj.getXY(c);
            int y = (int)c.y;
            int x = (int)c.x;
            c.al=0;
            c.del=+90;
            proj.getXY(c);
            pa = (int)Math.abs(c.y-y);
            ga=pa*2;

//            gbuf.translate(width/2-xc,height/2-yc);
            gbuf.setColor(Aladin.COLOR_GREEN);
            gbuf.drawOval(x-ga,y-pa-DELTAY,ga*2,pa*2);
            gbuf.drawOval(x-(ga+pa)/2,y-pa-DELTAY,ga+pa,pa*2);
            gbuf.drawOval(x-pa,y-pa-DELTAY,pa*2,pa*2);
            gbuf.drawOval(x-pa/2,y-pa-DELTAY,pa,pa*2);
            gbuf.drawLine(x-ga,y-DELTAY,x+ga,y-DELTAY);
            gbuf.drawLine(x,y-pa-DELTAY,x,y+pa-DELTAY);

            //            Projection proj = v.getProj();
            
            Coord [] coin = v.getCouverture();
            if( coin!=null ) {
               proj.frame = aladin.localisation.getFrame();
               gbuf.setColor( Aladin.COLOR_BLUE );
               for( int i=0; i<coin.length; i++ ) {
                  if( Double.isNaN(coin[i].al) ) continue;
                  c.al = coin[i].al;
                  c.del = coin[i].del;
                  proj.getXY(c);
                  gbuf.drawLine((int)c.x,(int)c.y-DELTAY,(int)c.x,(int)c.y-DELTAY);
               }
               c = v.getCooCentre();
               if( c== null ) {
                  System.err.println("ZommView.drawHipsControl bug++ (v.getCooCenter() returns null ! => switch in SIN");
                  proj = v.pref.projd = new Projection("allsky",Projection.WCS,0,0,60*4,60*4,250,250,500,500,0,false,Calib.SIN,Calib.FK5);
                  v.pref.projd.frame = aladin.localisation.getFrame();
                  v.projLocal = v.pref.projd.copy();
                  drawHipsControl(g, v);
                  return;
               }
               proj.getXY(c);
               gbuf.setColor( Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT ); //Color.magenta );
//               int w=5;
//               gbuf.drawLine((int)c.x-w,(int)c.y,(int)c.x+w,(int)c.y);
//               gbuf.drawLine((int)c.x,(int)c.y-w,(int)c.x,(int)c.y+w);
               Util.fillCircle7(gbuf,(int)c.x,(int)c.y-DELTAY);
            }

            gbuf.setFont(Aladin.SPLAIN);
            gbuf.setColor(Aladin.COLOR_GREEN);
            gbuf.drawString("+90",x-5,y-pa-2-DELTAY);
            gbuf.drawString("-90",x-5,y+pa+10-DELTAY);
            gbuf.drawString("-180",x-ga+5-gbuf.getFontMetrics().stringWidth("-180"),y+15-DELTAY);
            gbuf.drawString("+180",x+ga-5,y-5-DELTAY);
//            gbuf.translate(xc-width/2,yc-height/2);

            String s;
            gbuf.setFont(Aladin.SBOLD);
            FontMetrics fm = gbuf.getFontMetrics();
//            s="Frame: "+Localisation.REPERE[aladin.localisation.getFrame()];
//            gbuf.drawString(s,getWidth()-fm.stringWidth(s)-2,10);
            
            
            // Info spaciale texttuelle
//            int yInfo = height-16;
            int xInfo,yInfo;
//            gbuf.setColor( Aladin.COLOR_BLUE );
//            gbuf.setColor( Color.magenta );
            gbuf.setColor( Aladin.COLOR_CONTROL_FOREGROUND ); //Color.magenta );
            s=aladin.localisation.J2000ToString(c.al, c.del, Astrocoo.ARCSEC+1,false);
            xInfo = (int)( c.x + fm.stringWidth(s) > width ? c.x - fm.stringWidth(s)-10 : c.x+10);
            yInfo = (int)c.y-5;
            gbuf.drawString(s,xInfo,yInfo-DELTAY);
            
            String s1=v.getTaille(0);
            xInfo = (int)( c.x + fm.stringWidth(s) > width ? c.x - fm.stringWidth(s1)-10 : c.x+10);
            yInfo+=12;
            gbuf.drawString(s1,xInfo,yInfo-DELTAY);
            
            drawBord(gbuf);

            //         t = Util.getTime()-t;
            //         System.out.println("Time to draw : "+t);
         }

         g.drawImage(imgbuf, 0,0, this);
         drawTargetHistoryControl(g);
         zoomTimeControl.draw(g);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }

   // Trace les bords avec un effet de relief
   private void drawBord(Graphics g) {
      Util.drawEdge(g,getWidth(),getHeight());
   }
   
   
   private Rectangle rectLoc=null;
   private Rectangle rectHistory=null;
   private Rectangle rectTarget=null;
   private boolean flagHistoryIn=false;
   private boolean flagTargetHistoryIn=false;
   private boolean flagLocHistoryIn=false;
   
   private void drawTargetHistoryControl(Graphics g) {
      Color c =  Aladin.COLOR_CONTROL_FOREGROUND;
      
      // Dessin du logo de localisation
      int x=5, y=10;
      g.setColor( flagLocHistoryIn ? c.brighter() : c);
      drawLoc(g,x,y-5);
      rectLoc = new Rectangle(x,y-5,15,20);
      x+=20;
      
      // Dessin du triangle
      g.setColor( flagHistoryIn ? c.brighter() : c);
      Util.fillTriangle7(g, x,y);
      rectHistory = new Rectangle(x,y-5,15,20);
      x+=15;
      
      // Affichage du dernier target
      String target = aladin.targetHistory.getLast();
      int w = g.getFontMetrics().stringWidth(target);
      g.setColor( flagTargetHistoryIn ? c.brighter() : c);
      g.drawString(target,x,y+6);
      rectTarget = new Rectangle(x,y-5,w,20);
   }
   
   // Barres horizontales du dessin 
   static final private int LOCX[][] = {
      {0, 5,9 },
      {1, 4,10},
      {2, 3,11},
      {3, 2,5}, {3, 9,12},
      {4, 2,4}, {4, 10,12},
      {5, 2,4}, {5, 10,12},
      {6, 2,4}, {6, 10,12},
      {7, 2,5}, {7, 9,12},
      {8, 3,11},
      {9, 4,10},
      {10, 4,10 },
      {11, 5,9 },
      {12, 6,8 },
      {13, 7,7 },
   };
   
   /** Dessine l'icone de mémorisation de la localisation  */
   protected void drawLoc(Graphics g, int x,int y) {
      for( int i=0; i<LOCX.length; i++ ) g.drawLine(LOCX[i][1]+x,LOCX[i][0]+y,LOCX[i][2]+x,LOCX[i][0]+y);
   }

   /** Changement de table des couleurs
    * POUR L'INSTANT JE FAIS UN SIMPLE REPAINT
    */
   protected void setCM(IndexColorModel ic) { repaint(); }

   /** Dessin de l'histogramme */
   protected void drawImgHist(Graphics g) {
      hist.draw(g);
      drawBord(g);
   }

   /** Dessin du SED courant */
   protected boolean drawImgSED(Graphics g) {
      if( sed.getCount()==0 ) return false;
      sed.draw(g);
      drawBord(g);
      return true;
   }

   /** Dessin du graphe de coupe */
   protected void drawImgCut(Graphics g) {

      int w = getWidth();
      int h = getHeight();

      // Nettoyage
      g.setColor( Aladin.COLOR_BACKGROUND );
      g.fillRect(1,1,w-2,h-2);
      drawBord(g);

      boolean trouve=false;
      for( int i=1; i<cut.length-1; i++ ) {
         if( cut[i]!=0 || cutR[i]!=0 || cutG[i]!=0 || cutB[i]!=0) { trouve=true; break; }
      }
      if( !trouve ) {
         aladin.status.setText("Nothing in cut graph !!");
         return;
      }

      // Affichage de la coupe (cut) courant. Si la premiere valeur de hist[] est -1
      // il s'agit d'une coupe en couleur, sinon d'une coupe en niveau de gris
      if( cut[0]!=-1 ) {
         g.setColor( Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
         for( int i=1; i<cut.length-1; i++ ) g.drawLine(i,h-cut[i],i,h);
         g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
         for( int i=1; i<cut.length-1; i++ ) g.drawLine(i,h-cut[i-1],i,h-cut[i]);

      } else {
         g.setColor(Color.red);
         for( int i=1; i<cutR.length-1; i++ ) g.drawLine(i-1,h-cutR[i-1],i,h-cutR[i]);
         g.setColor(Color.green);
         for( int i=1; i<cutG.length-1; i++ ) g.drawLine(i-1,h-cutG[i-1],i,h-cutG[i]);
         g.setColor(Color.blue);
         for( int i=1; i<cutB.length-1; i++ ) g.drawLine(i-1,h-cutB[i-1],i,h-cutB[i]);
      }
   }

   /** Trace le trait et les infos permettant le calcul de la valeur FWHM
    *  Utilise les variables cutX et cutY mémorisées dans mouseMove() */
   private void drawFWHM(Graphics g) {
      if( cutX<0 || cut[0]==-1 ) return;  // pas dans le zoomview ou RGB image
      int x1,x2;
      int width = getWidth();
      int height = getHeight();
      int y = height-cutY;

      // Récupération de la valeur du pixel correspondant à la position
      // en ordonnée de la souris dans le cut graph.
      PlanImage pimg = (PlanImage)aladin.calque.getPlanBase();
      String pixel = "pixel value: "+pimg.getPixelInfoFromGrey(y*256/height);
      g.setFont(Aladin.PLAIN);
      g.setColor( Aladin.COLOR_BLUE );
      g.drawLine(1,cutY,4,cutY);
      g.drawLine(width-4,cutY,width,cutY);
      g.drawString(pixel,25,cutY<20?height-2:18);
      //       Util.drawStringOutline(g, pixel,25,cutY<20?SIZE-2:10, Color.yellow, Color.black);

      // Tracage du trait repérant le FWHM en fonction de la position
      // courante de la souris dans le cut graph.
      // recherche en abscisse des points min et max où l'on "sort"
      // de l'histogramme
      for( x1=cutX; x1>0 && y<=cut[x1]; x1--);
      for( x2=cutX; x2<width && y<=cut[x2]; x2++);
      //       if( x2<=x1 )  ((Cote)objCut).drawFWHM(0,0);
      //       else ((Cote)objCut).drawFWHM((longCut*((x1+1.)/SIZE)),(longCut*((x2-1.)/SIZE)));
      //       aladin.view.repaintAll();
      if( x2<=x1 ) return;
      g.setColor(Aladin.COLOR_FOREGROUND );
      g.drawLine(x1+1,cutY,x2-1,cutY);
      g.drawLine(x1+1,cutY+1,x2-1,cutY+1);		// Pour mieux voir

      // Calcul des distances en pixel et angulaire (si astrométrie)
      // correspondantes au tracage du FWHM
      double distXY = ((Cote)objCut).getDistXY();
      if( distXY==-1 ) return;
      double f = (x2-x1)*distXY/width;
      String fwhm = Util.myRound( f+"" ,1 )+"pix";
      int x = width-cutX<40?cutX-50:cutX+7;
      g.drawString(fwhm,x,cutY-2);
      double dist = ((Cote)objCut).getDist();
      if( dist!=-1 ) g.drawString(Coord.getUnit(dist*f/distXY),x,cutY+15);
   }

   private void drawPixelCourant(Graphics g) {
      try {
         String s;

         PlanImage p = (PlanImage)aladin.calque.getPlanBase();
         if( p==null ) return;
         g.setColor( Aladin.COLOR_BLUE );
         g.setFont(Aladin.SPLAIN);

         // Affichage de la valeur min et max à droite et à gauche
         s=p.getPixelInfoFromGrey(0);
         g.drawString(s,2,10);
         s=p.getPixelInfoFromGrey(255);
         g.drawString(s,getWidth()-g.getFontMetrics().stringWidth(s)-2,10);

         // Affichage de la valeur du pixel courant (sous la souris)
         if( cutX>0 ) {
            s = p.getPixelInfoFromGrey((int)Math.floor(cutX* (256./getWidth())));
            int x = cutX;

            g.setColor(Color.red);

            // trait de repère du pixel courant
            g.drawLine(x,0, x,getHeight());

            int len = g.getFontMetrics().stringWidth(s);
            if( x+len>getWidth()-5 ) x = x-5-len;
            else x+=5;
            g.drawString(s,x,getHeight()-5);
         }

      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace() ; }
   }

   /** Trace le trait et les infos permettant le calcul de la valeur FWHM
    *  Utilise les variables cutX et cutY mémorisées dans mouseMove() */
   private void drawCubeFrame(Graphics g) {

      int width = getWidth();
      int height = getHeight();

      // Tracage du trait repérant le frame courant dans le cube
      ViewSimple vc = aladin.view.getCurrentView();
      int frame = vc.cubeControl.lastFrame;
      int nbFrame = vc.cubeControl.nbFrame;
      int x = (int)( width*((double)frame/nbFrame) );
      int w = Math.max(1,(int)( (double)width/nbFrame));
      g.setColor(Color.red);
      g.fillRect(x,0,w,height);


      PlanImageCube pic = null;
      if( vc.pref.type==Plan.IMAGECUBE ) pic = (PlanImageCube)vc.pref;
      else pic = null;

      // Affichage de l'info
      g.setFont(Aladin.SPLAIN);
      String s = (pic!=null && pic.fromCanal ? pic.getCanalValue(frame) : ""+ (frame+1));
      int l = g.getFontMetrics().stringWidth(s);
      g.drawString(s, x+l+w>width ? x-l-2 : x+w+2,height-2);


      // Récupération de la valeur du pixel correspondant à la position
      // en ordonnée de la souris dans le cut graph.
      int y = height-cutY;
      String pixel = ((PlanImage)vc.pref).getPixelInfoFromGrey(y*256/height);
      if( pixel.length()==0 ) return;
      g.setColor(Color.black);
      g.drawLine(1,cutY,width,cutY);
      g.drawString(pixel,10,cutY<20?cutY+10:cutY-2);
   }

   /** Suspension temporaire de l'affichage du cutGraph */
   protected void suspendCut() { flagCut=false; repaint(); }

   /** Retourne le Cote associée au cutGraph courant, null si aucune */
   protected Obj getObjCut() {
      return objCut;
   }

   protected void cutOff(Obj objCut) {
      if( objCut!=this.objCut ) return;
      setCut(null);
   }

   static final int CUTNORMAL = 0;
   static final int CUTRGB = 1;
   static final int CUTHIST = 2;

   /**
    * Passage d'un histogramme de valeurs que l'on va mapper dans
    * le carre du ZoomView (coupe)
    * L'arret de la visualisation de la coupe se fait en passant
    * une reference null.
    * Le repaint n'est assure que pour l'effacement, sinon c'est l'objet
    * View qui s'en charge via zoom.redraw();
    * @param v Liste des valeurs de l'histogramme.
    * @param flagRGB vrai si les valeurs de la liste sont en fait des triplet RGB
    */
   protected void setCut(int [] v ) { setCut(null, v,CUTNORMAL); }
   protected void setCut(Obj objCut, int [] v, int mode ) {
      cutX=-1;
      this.objCut=objCut;
      if( objCut==null || v==null || aladin.toolBox.tool[ToolBox.WEN].mode==Tool.DOWN ) {
         if( flagCut ) repaint();
         flagCut=false;
         return;
      }
      flagCut=true;

      int width = getWidth();
      int height = getHeight();

      if( cut==null || cut.length!=width) {
         cut = new int[width];
         cutR = new int[width];
         cutG = new int[width];
         cutB = new int[width];
      }

      double f = height/256.;
      double w = (double)v.length/width;
      int j=0;
      int n;
      for( int i=0; i<width; i++ ) {
         n=0;
         double sw=w*i;

         if( mode==CUTRGB ) {
            cut[0]=-1;
            double tR,tG,tB;
            boolean valid=true;
            tR=tB=tG=0.;
            while( j<sw && j<v.length ) {
               if( valid && v[j]==-1 ) valid=false;
               tR+= (v[j]>>16) & 0xFF;
               tG+= (v[j]>>8) & 0xFF;
               tB+= v[j] & 0xFF;
               j++;
               n++;
            }
            tR/=n; tG/=n; tB/=n;
            if( !valid ) cutR[i]=cutG[i]=cutB[i]=getWidth();
            else {
               cutR[i]=(int)(f*tR); cutG[i]=(int)(f*tG); cutB[i]=(int)(f*tB);
               if( cutR[i]==0 && i>0 ) cutR[i]=cutR[i-1];
               if( cutG[i]==0 && i>0 ) cutG[i]=cutG[i-1];
               if( cutB[i]==0 && i>0 ) cutB[i]=cutB[i-1];
            }
         } else  {
            double t=0.;
            boolean valid=true;
            while( j<sw && j<v.length ) {
               if( valid && v[j]==-1 ) valid=false;
               t+=v[j++];
               n++;
            }
            t/=n;
            if( !valid ) cut[i]=0;
            else {
               cut[i]=(int)(f*t);
               if( mode==CUTNORMAL && cut[i]==0 && i>0 ) cut[i]=cut[i-1];
            }
         }
      }
      repaint();
   }

   private String oSrcSed="";           // Source associé au dernier SED tracé

   protected void clearSED() {
      flagSED=false;
      oSrcSed="";
      if( sed!=null ) sed.clear();
      aladin.view.simRep=null;
      aladin.calque.repaintAll();
   }
   
   /** Chargement et affichage d'un SED à partir d'un nom d'objet
    * Si null, arrêt du SED précédent
    * @param position position de la source (résultat Sésame)
    * @param source identificateur de la source
    * @param simRep repere correspondant à l'objet dans la vue (si connu)
    */
   protected void setSED(String position, String source) { setSED(position, source,null); }
   protected void setSED(String position, String source, Repere simRep) {
      if( source == null ) source = "";
      if( source.length() == 0 ) flagSED = false;
      if( oSrcSed.equals(source) ) return;
      oSrcSed = source;
      
      // Arret du SED
      if( source.length() == 0 ) {
//         System.out.println("ZoomView.clearSED");
         clearSED();

         // Chargement du SED
      } else {
//         System.out.println("ZoomView.setSED pour "+source);
         if( sed == null ) sed = new SED(aladin);
         flagSED = true;
         flagHist = false;
         sed.loadFromSource(position,source);
         sed.setRepere(simRep);
      }
      repaint();
   }

   /** Chargement et affichage d'un SED à partir d'un objet étalon issu
    * de la table des mesures. Il faut que le plan associé à l'objet étalon
    * soit issu d'une table SED à la VizieR
    * Si null, arrêt du SED précédent
    * @param o
    */
   protected void setSED(Source o) {
      String source=o==null?null:o.id;
      if( source==null ) source="";
      //      if( oSrcSed.equals(source) ) return;
      oSrcSed=source;

      // Arret du SED
      if( source.length()==0 ) {
         clearSED();

         // Chargement du SED
      } else {
         if( sed==null ) sed = new SED(aladin);
         sed.clear();
         flagSED=true;
         flagHist=false;
         
         sed.addFromIterator( aladin.mesure.iterator() );
         //         sed.setSource(null);   // Pas possible d'ouvvrir l'outil d'Anne-Camille sinon
         sed.setHighLight(o);
      }
      repaint();

   }

   /** Met à jour le SED si nécessaire, sinon le fait disparaitre */
   protected void resumeSED() {
      Source s = aladin.mesure.getFirstSrc();
      if( (s==null || s.getLeg()==null || !s.getLeg().isSED()) && !flagSED ) return;
      setSED( s );
   }

   /** Arrêt de l'affichage de l'histogramme courant */
   protected void stopHist() {
      if( !flagHist ) return;   // Déjà fait
      flagHist=false;
      aladin.view.flagHighlight=false;
      hist.resetHighlightSource();
      repaint();
   }

   protected void initPixelHist() {
      int w = getWidth();
      int h = getHeight();
      if( hist==null ) hist = new ZoomHist(aladin);
      hist.startHistPixel();  // Reset/Création du tableau des pixels
   }

   protected void addPixelHist(double pix) { hist.addPixel(pix); }

   protected void createPixelHist(String titre) { hist.createHistPixel(titre); }

   protected void activeHistPixel(String texte) {
      //      if( hist.isOverFlow() ) { flagHist=true;  }
      flagHist=true;
      hist.setText(texte);
      repaint();
   }

   /** Génération (tentative) d'un histogramme et affichage
    * @return true si c'est ça concerne une nouvelle colonne
    */
   protected boolean setHist(Source o,int nField) {
      boolean rep=false;

      // pour annuler le précédent SED
      flagSED=false;
      oSrcSed="";

      int w = getWidth();
      int h = getHeight();

      if( hist==null ) { hist = new ZoomHist(aladin); rep=true; }
      rep |= hist.o!=o || hist.nField!=nField;
      flagHist=hist.init(o, nField);
      //      aladin.trace(4, "ZoomView.setHist("+o+","+nField+") done !");
      repaint();
      return rep;
   }

   /**
    * Passage d'un histogramme de valeurs que l'on va mapper dans
    * le carre du ZoomView (coupe)
    * L'arret de la visualisation de la coupe se fait en passant
    * une reference null.
    * Le repaint n'est assure que pour l'effacement, sinon c'est l'objet
    * View qui s'en charge via zoom.redraw();
    * @param v Liste des valeurs de l'histogramme.
    * @param flagRGB vrai si les valeurs de la liste sont en fait des triplet RGB
   protected void setCut(int [] v ) { setCut(v,false); }
   protected void setCut(int [] v, boolean flagRGB ) {
      if( v==null ) { if( flagCut ) repaint(); flagCut=false; return; }
      flagCut=true;

      double f = SIZE/256.;
      double w = (double)v.length/SIZE;
      int j=0;
      int n;
      for( int i=0; i<SIZE; i++ ) {
         n=0;
         double sw=w*i;

         if( flagRGB ) {
            hist[0]=-1;
            double tR,tG,tB;
            boolean valid=true;
            tR=tB=tG=0.;
            while( j<sw && j<v.length ) {
               if( valid && v[j]==-1 ) valid=false;
               tR+= (v[j]>>16) & 0xFF;
               tG+= (v[j]>>8) & 0xFF;
               tB+= v[j] & 0xFF;
               j++;
               n++;
            }
            tR/=n; tG/=n; tB/=n;
            if( !valid ) histR[i]=histG[i]=histB[i]=0;
            else {
               histR[i]=(int)(f*tR); histG[i]=(int)(f*tG); histB[i]=(int)(f*tB);
               if( histR[i]==0 && i>0 ) histR[i]=histR[i-1];
               if( histG[i]==0 && i>0 ) histG[i]=histG[i-1];
               if( histB[i]==0 && i>0 ) histB[i]=histB[i-1];
            }
         } else {
            double t=0.;
            boolean valid=true;
            while( j<sw && j<v.length ) {
               if( valid && v[j]==-1 ) valid=false;
               t+=v[j++];
               n++;
            }
            t/=n;
            if( !valid ) hist[i]=0;
            else {
               hist[i]=(int)(f*t);
               if( hist[i]==0 && i>0 ) hist[i]=hist[i-1];
            }
         }
      }
   }
    */

   protected void changeWen(int sens) {
      if( sens==-1 && WENZOOM>8 ) { WENZOOM/=2; oc=null; mImgWen=null; }
      else if( sens==1 && WENZOOM<64 ) { WENZOOM*=2; oc=null; mImgWen=null; }
      repaint();
   }

   /** Passage en zoom 32 (resp zoom 8) pour afficher (resp. ne pas afficher) les valeurs des pixels */
   protected void setPixelTable(boolean flag) { WENZOOM=flag?32:8; }

   /** Generation de la loupe courante dans son buffer
    * Utilise comme posiition centrale (xwen,ywen)
    */
   protected void drawImgWen() {
      PointD c;      // Position de la loupe ds les coord de l'image courante
      int frame;   // frame courante dans le cas d'une image blink

      // Recherche du plan sous la souris
      ViewSimple v = aladin.view.getMouseView();
      if( v==null || v.isFree() || !v.pref.isImage() || v.isProjSync() || v.northUp ) {
         img=null;
         imgok=false;
         return;
      }

      // Calcul de la correspondance du point de la vue
      // dans les coordonnees de l'image courante
      c = v.getPosition(xwen,ywen);

      // Ni bougé (ni même frame) ?
      if( v.pref.type==Plan.IMAGEBLINK || v.pref.type==Plan.IMAGECUBE ) {
         frame = v.cubeControl.lastFrame;
      } else frame=oframe;
      if( c.equals(oc) && frame==oframe ) return;
      oc = c;
      oframe = frame;

      // Calcul de la nouvelle loupe
      calculWen(v,(int)c.x,(int)c.y);
   }


   // Buffers de travail pour la loupe.
   private byte[] pixels=null;
   private byte[] scalepixels=null;
   private int[] pixelsRGB=null;
   private int[] scalepixelsRGB=null;


   /** Extraction des pixels et construction de l'image de la loupe.
    * @param val facteur d'agrandissement (puissance de 2 obligatoirement)
    * @param x,y Position de la loupe dans l'image de reference
    * @see aladin.ZoomView#run()
    */
   protected void calculWen(ViewSimple v, int x,int y) {
      xmwen = x;
      ymwen = y;
      int w = (int)Math.ceil((double)getWidth()/WENZOOM);
      /*anais*/
      int x1 = xmwen-w/2;
      int y1 = ymwen-w/2;
      
      // Mémorisation du coin haut gauche du repère rectangulaire central
      xWenrect = (xmwen-x1)*WENZOOM;
      yWenrect = (ymwen-y1)*WENZOOM;
      
      PlanImage p = (PlanImage)v.pref;
      boolean isPlanRGB = p.type==Plan.IMAGERGB;

      // Pour une sombre raison que je ne comprends pas, le changement de vue
      // ne permet pas d'utiliser le précédent MemoryImageSource pour la loupe,
      // Je le réinitialise donc à chaque changement de vue
      if( v.hashCode()!=ov ) mImgWen=null;
      ov = v.hashCode();

      // Allocations
      if( isPlanRGB ) {
         if( pixelsRGB==null || pixelsRGB.length!=w*w ) pixelsRGB = new int[w*w];
         if( scalepixelsRGB==null
               || scalepixelsRGB.length!=w*w*WENZOOM*WENZOOM ) scalepixelsRGB = new int [w*w*WENZOOM*WENZOOM];
      } else {
         if( pixels==null || pixels.length!=w*w ) pixels = new byte [w*w];
         if( scalepixels==null
               || scalepixels.length!=w*w*WENZOOM*WENZOOM ) scalepixels = new byte [w*w*WENZOOM*WENZOOM];
      }

      // Extraction de la portion de l'image
      if( x1>=0 && y1>=0 && x1+w<p.width && y1+w<p.height) {
         /*anais*/
         if( isPlanRGB ) {
            PlanImageRGB pi = (PlanImageRGB)p;
            pi.getPixels(pixelsRGB,x1,y1,w,w);
            int i,j,k,d;

            // Coloriage de 3 barres RGB en fonction du pixel central
            //            int pixel = pi.pixelsRGB[ymwen*pi.width+xmwen];
            //            i = ymwen*pi.width+xmwen;
            //            int rX = pi.red!=null?pi.red[i]:0;
            //            int gX = pi.green!=null?pi.green[i]:0;
            //            int bX = pi.blue!=null?pi.blue[i]:0;
            //            int pixel = 0xFF000000|(rX<<16)|(gX<<8)|bX;
            //
            //            memInv = pi.video==PlanImage.VIDEO_INVERSE;
            //            if( memInv ) pixel ^= 0x00FFFFFF;
            flagRGB=true;

            // Agrandissement
            d=0;
            for( i=0; i<w; i++ ) {
               int orig=d;
               for( k=0; k<w; k++) {
                  int c = pixelsRGB[i*w+k];
                  //                  ViewSimple.bytefill(scalepixelsRGB,d,WENZOOM,c);
                  for( int m=0; m<WENZOOM; m++ ) scalepixelsRGB[d+m]=c;

                  d+=WENZOOM;
               }
               for( j=1; j<WENZOOM; j+=j ) {
                  System.arraycopy(scalepixelsRGB,orig, scalepixelsRGB,orig+j*w*WENZOOM,j*WENZOOM*w);
               }
               d+=(WENZOOM-1)*WENZOOM*w;
            }
         }
         else {

            p.getPixels(pixels,x1,y1,w,w);
            flagRGB=false;

            // Agrandissement
            int i,j,k,d;
            d=0;
            for( i=0; i<w; i++ ) {
               int orig=d;
               for( k=0; k<w; k++) {
                  byte c = pixels[i*w+k];
                  //                  ViewSimple.bytefill(scalepixels,d,WENZOOM,c);
                  for( int m=0; m<WENZOOM; m++ ) scalepixels[d+m]=c;

                  d+=WENZOOM;
               }
               for( j=1; j<WENZOOM; j+=j ) {
                  System.arraycopy(scalepixels,orig, scalepixels,orig+j*w*WENZOOM,j*WENZOOM*w);
               }
               d+=(WENZOOM-1)*WENZOOM*w;
            }

         }
      }

      if( isPlanRGB ) {
         if( mImgWen==null ) {
            mImgWen = new MemoryImageSource(w*WENZOOM,w*WENZOOM,
                  scalepixelsRGB, 0, w*WENZOOM);
            mImgWen.setAnimated(true);
            imgwen = getToolkit().createImage(mImgWen);
         } else mImgWen.newPixels(scalepixelsRGB,ColorModel.getRGBdefault(),0,w*WENZOOM);
      } else {
         if( mImgWen==null ) {
            mImgWen = new MemoryImageSource(w*WENZOOM,w*WENZOOM,p.cm,
                  scalepixels, 0, w*WENZOOM);
            mImgWen.setAnimated(true);
            imgwen = getToolkit().createImage(mImgWen);
         } else mImgWen.newPixels(scalepixels,p.cm,0,w*WENZOOM);
         //            PlanImage.waitImage(this,imgwen);

      }

      if( flagwen ) {
         imgok=true;
         img=imgwen;
         //         repaint();
      }
   }

   private void drawInfo(Graphics g,int w,int h) {
      try {
         Projection proj = aladin.view.getCurrentView().pref.projd;
         if( proj.isXYLinear() ) return;
         Calib c = proj.c;
         String w1 = Coord.getUnit(c.getImgWidth());
         String h1 = Coord.getUnit(c.getImgHeight());
         g.setFont(Aladin.SPLAIN);
         g.setColor( Aladin.COLOR_BLUE );
         String s = w1+" x "+h1;
         g.drawString(s,w/2-g.getFontMetrics().stringWidth(s)/2,h-10);
      } catch( Exception e ) {}
   }

   /** Tracé d'une flêche orientée au Nord, Sud, Est ou Ouest
    *  @param g le contexte graphique
    *  @param x,y la position du bas de la flèche
    *  @param d la direction (N, S, E ou W)
    */
   static private int WF=12;
   static int [] XF = new int[3];
   static int [] YF = new int[3];
   static private void drawFleche(Graphics g,int x,int y,char d) {
      int dx=d=='W'?-WF:d=='E'?WF:0;
      int dy=d=='N'?-WF:d=='S'?WF:0;
      int xb=x+dx;
      int yb=y+dy;
      g.drawLine( x,y, xb,yb);
      dx/=3; dy/=3;
      XF[0]=xb;    YF[0]=yb;
      XF[1]=xb+dy-dx; YF[1]=yb-dy-dx;
      XF[2]=xb-dy-dx; YF[2]=yb-dy+dx;
      g.fillPolygon(XF,YF,3);
      g.drawPolygon(XF,YF,3);
   }

   private int owidth,oheight;

//   protected long startTaquinTime=0L;


//   String getTaquinTime() {
//      long time = (System.currentTimeMillis()-startTaquinTime)/1000;;
//      return Util.align2((int)time/60)+":"+Util.align2((int)time%60);
//   }

   protected boolean isPixelTable() {
      return WENZOOM>=32;
   }

   protected void copier() {
      String s;
      if( flagHist ) s = hist.getCopier();
      else return;
      aladin.copyToClipBoard(s);
   }
   
   private int lastWidth=0,lastHeight=0;

   public void paintComponent(Graphics gr) {
      if( Aladin.NOGUI ) return;
      
      rectHistory=null;
      
      // AntiAliasing
      aladin.setAliasing(gr);

      ViewSimple v = aladin.view.getCurrentView();

      int w = getWidth();
      int h = getHeight();
      
      if( w!=lastWidth || h!=lastHeight ) {
         aladin.view.adjustZoomView(lastWidth,w, lastHeight,h);
         lastWidth=w;
         lastHeight=h;
         resumeSize();
      }
      
//      if( v==null || v.isFree() ) {
         gr.setColor( Aladin.COLOR_BACKGROUND );
         gr.fillRect(0,0,w,h);
         drawBord(gr);
         if( v==null || v.isFree() ) return;
//      }
      

      Dimension d = v.getSize();
      
      // Faut-il afficher la loupe ou le zoom ?
      if( flagwen ) { imgok=true; drawImgWen(); }
      else if( !flagdrag )  imgok = drawImgZoom(v);

      // L'histogramme
      if( flagHist ) { drawImgHist(gr); return; }

      // Le SED
      if( flagSED ) { if( drawImgSED(gr) ) return; }

      // Le graphe de coupe
      if( flagCut ) {
         drawImgCut(gr);
         try {
            if( objCut instanceof Cote ) drawFWHM(gr);
            else {
               if( objCut instanceof SourceStat ) {
                  if( ((SourceStat)objCut).hasRayon() ) drawPixelCourant(gr);
               } else if( Ligne.isLigne(objCut) ) drawPixelCourant(gr);
               else if( objCut instanceof Repere ) drawCubeFrame(gr);
            }
         } catch( Exception e ) { setCut(null); }
         return;
      }

      if( v.isPlot()  ) {
         gr.setColor( BGD );
         gr.fillRect(0,0,w,h);
         drawBord(gr);
         return;
      }

      if( !Aladin.NOGUI && !v.isFree() 
            && (d.width!=owidth || d.height!=oheight) ) {
         owidth = d.width;
         oheight = d.height;
         newZoom(v.xzoomView,v.yzoomView);
         return;
      }
      
      // La vue AITOFF pour le mode allsky
      if( v.pref instanceof PlanBG ) {
         drawHipsControl(gr,v);
         return;
      }

      if( imgok ) gr.drawImage(img,0,0,this);
      else gr.clearRect(0,0,w,h);

      // Compteur pour le taquin
//      if( aladin.view.flagTaquin ) {
//         gr.setFont(new Font("Helvetica",Font.BOLD,30));
//         gr.setColor(Color.red);
//         if( startTaquinTime==0L ) startTaquinTime=System.currentTimeMillis();
//         gr.drawString(getTaquinTime(),38,w/2+15);
//         drawBord(gr);
//         return;
//      }

      // Affichage du rectangle du zoom et des infos sur le champ
      if( !v.isFree() && !flagwen && !flagCut && rectzoom!=null ) {
         drawArea(gr, (int)Math.round(rectzoom.x),(int)Math.round(rectzoom.y),
               (int)Math.round(rectzoom.width-1),(int)Math.round(rectzoom.height-1));
         gr.setColor( Aladin.COLOR_BLUE );
         drawInfo(gr,w,h);
      }

      // Repere dans la loupe et les flèches dans les 4 directions
      if( flagwen && imgok ) {
//         int n = (int)Math.ceil((double)getWidth()/WENZOOM);
//         int c = (n/2)*WENZOOM;
         int W2 = WENZOOM/2;
         gr.setColor( Aladin.COLOR_BLUE );
//         gr.drawRect(c,c,WENZOOM,WENZOOM);
         gr.drawRect(xWenrect,yWenrect,WENZOOM,WENZOOM);

         if( WENZOOM<=16 ) {
            drawFleche(gr,xWenrect+W2,yWenrect-WENZOOM,'N');
            drawFleche(gr,xWenrect+W2,yWenrect+2*WENZOOM,'S');
            drawFleche(gr,xWenrect-WENZOOM,yWenrect+W2,'W');
            drawFleche(gr,xWenrect+2*WENZOOM,yWenrect+W2,'E');
         }
         gr.setFont(Aladin.LBOLD);
         gr.setColor(Color.red);
//         if( WENZOOM>8  ) gr.drawString("-",w-11,h-15);
//         if( WENZOOM<64 )gr.drawString("+",w-12,h-5);

         // Affichage des valeurs individuelles des pixels
         if( isPixelTable() && v.pref.type!=Plan.IMAGERGB ) {
            v = aladin.view.getMouseView();
            int i,j,x1,y1;
            int step= WENZOOM==32 ? 4 : 1;
            gr.setFont( Aladin.BOLD );
            gr.setColor( Color.black );
            for( j=0, y1=ymwen-step-1; y1<=ymwen+step; y1++,j++) {
               for( i=0, x1=xmwen-step-1; x1<=xmwen+step; x1++,i++) {
                  int xx = (i-1)*WENZOOM+8;
                  int yy = (j-1)*WENZOOM+2*WENZOOM/3-7+(i%2)*WENZOOM/2;
                  
                  String pix = ((PlanImage)v.pref).getPixelInfo(x1, y1, View.REALX);
                  Util.drawStringOutline(gr, pix, xx, yy, 
                        y1==ymwen && x1==xmwen ? Color.cyan : Color.white,Color.black);
               }
            }
         }
      }

      drawBord(gr);
   }
   

   /** Tracé du rectangle de la zone visible */
   private void drawArea(Graphics g, int x, int y, int width, int height) {
      Util.drawArea(aladin,g,x,y,width,height,Color.blue,0.15f, true);

      int w = getWidth();
      int h = getHeight();

      // Complètement en dehors  ?
      g.setColor( Color.red );
      if( x+width<0 ) drawFleche(g,15,h/2,'W');
      if( y+height<0 ) drawFleche(g,w/2,15,'N');
      if( x>=getWidth() ) drawFleche(g,w-15,h/2,'E');
      if( y>=getHeight() ) drawFleche(g,w/2,h-15,'S');
   }

   /** Demande de reaffichage de la loupe.
    * @param x,y Nouveau centre de la loupe
    */
   protected void wen(double x,double y) {
      if( !flagwen ) return;
      if( xwen==x && ywen==y ) return;        // Cela n'as pas bouge
      xwen = x; ywen = y;
      repaint();
   }

   /** Activation de la loupe */
   protected void wenOn() { flagwen=true; }

   /** Desactivation de la loupe */
   void wenOff() { flagwen=false; repaint(); }

   /** Gestion du Help */
   protected String Help() { return aladin.chaine.getString("ZoomView.HELP"); }

   // Gestion du Help
   //   public boolean handleEvent(Event e) {
   //      if( Aladin.inHelp ) {
   //         if( e.id==Event.MOUSE_ENTER ) aladin.help.setText(Help());
   //         else if( e.id==Event.MOUSE_UP ) aladin.helpOff();
   //         return true;
   //      }
   //
   //      return super.handleEvent(e);
   //   }

   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity, JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
   }

   @Override
   public void paintCollapsed(Graphics g) {
      Tool.drawVOHand(g,3,2);
   }

}
