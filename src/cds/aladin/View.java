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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.ColorModel;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import cds.aladin.prop.Propable;
import cds.tools.UrlLoader;
import cds.tools.Util;

/**
 * Gestionnaire des vues. Il s'agit d'afficher les plans actifs (voir
 * ToolBox) dans l'ordre de visibilite selon l'echelle indiquee par le zoom
 * (voir ZoomView) et ceci pour toutes les vues
 * <P>
 * La souris a differentes fonctions suivant l'outil (Tool bar) active.
 * Ce peut etre la simple selection d'une source, le dessin d'objet
 * graphique, le zoom pointe...
 *
 * INFO DE PROGRAMMATION SUR LA GESTION DU MULTIVIEW.
 *
 * A partir de la version 3, Aladin gère le multiview. Pour ce faire, l'ancienne
 * classe View a été scindée en deux :
 *    . View : gère tout ce qui concerne l'ensemble des vues
 *    . ViewSimple : gère l'affichage d'une vue particulière
 * Et plusieurs classes ont été ajoutées:
 *    . ViewMemo et ViewMemoItem : mémorisent les infos sur toutes les vues actives
 *    . ViewControl : gère le sélecteur définissant le nombre de vues simultanées.
 * D'autre part, certaines infos qui se trouvaient dans ZoomView ont été rapatriées
 * dans ViewSimple car propre à chaque vue (notamment la valeur courante du zoom,
 * son centre). Ainsi bon nombre de méthodes qui devaient connaître la valeur du zoom
 * courant et pour lesquelles il fallait passer l'objet ZoomView, ont désormais
 * comme paramètre directement ViewSimple à la place de ZoomView.
 *
 * Quelques détails qui peuvent servir:
 * 1) View compare les vues en cours d'affichage avec celles qui devraient
 *    être affichées (en fonction du scrollbar ou d'un changement du nombre de vues).
 *    S'il y a eu modif, View va sauvegarder les vues et regénérer les nouvelles
 *    via ViewMemo. Puis il va appeler en séquence le repaint de chacune de ces vues.
 *    Lors du réaffichage d'une vue, l'update de la vue ajuste automatiquement
 *    le zoom afin que la zone visible reste la même (zoomAdjust).
 *   (principales variables viewSimple[], modeView et currentView)
 * 2) Les overlays graphiques (sources ou objets graphiques) ont désormais
 *    des tableaux dimensionnés en fct du nombre de vues pour mémoriser les
 *    x,y pour chaque image de chaque vue, ainsi que pour les x,y de la portion
 *    zoomée. D'autre part, comme les plans objets ne se désactivent plus
 *    automatiquement, il faut désormais mémoriser le fait qu'un plan catalogue
 *    n'est pas projetable pour une vue particulière. Pour ce faire, on utilise
 *    un tableau d'état se trouvant dans plan.pcat.drawnInViewSimple[] et
 *    consultable par plan.pcat.isDrawnInViewSimple(int n).
 *    Cette méthode sera appelé pour filtrer les plans objets non concernés
 *    (détection des objets sous la souris...)
 * 3) L'affectation d'un plan dans une vue peut se faire désormais par glisser
 *    déposer depuis la pile dans une vue. Idem pour le déplacement d'une vue
 *    ou sa copie (Ctrl). La gestion de cette fonction se fait par les méthodes
 *    megaDrag... Il est a noter que la vue cible est déterminée par
 *    la méthode getTargetViewForEvent() en fonction de la configuration
 *    des panels graphiques d'Aladin.
 * 4) Pour conserver de bonnes performances, l'indice de chaque vue dans
 *    le tableau viewSimple[] est mémorisé dans chaque ViewSimple (variable n).
 *    En effet, pour tracer n'importe quel objet graphique, la procédure de
 *    drawing doit connaître le numéro de la vue concernée (indice dans les
 *    différents caches (x,y)) en plus du facteur de zoom. Il serait trop
 *    long de parcourir le tableau View.viewSimple[] à chaque fois.
 *    Il faut donc remettre à jour ces variables "n" à chaque
 *    regénération d'une vue récupérée de ViewMemo. (voir View.recharge())
 * 5) Le blinking (d'une source ou d'une image blink) est désormais géré
 *    par un thread particulier (voir startBlinking() et runC())
 *
 *
 * @see Aladin.ToolBox
 * @see Aladin.Zoomview
 * @see Aladin.ViewSimple
 * @author P. Fernique CDS
 * @version 2.0 : (nov 04) gestion du multiview
 * @version 1.2 : (1 dec 00)  Meilleure gestion de la source montree + gestion du plan Add
 * @version 1.1 : (28 mars 00)  Modif debug -> trace + label pour les plans
 * @version 1.0 : (11 mai 99)   Toilettage du code
 * @version 0.91 - 3 dec 1998   Nettoyage du code
 * @version 0.9 - 31 mars 1998
 */
public class View extends JPanel implements Runnable,AdjustmentListener {

   // Les valeurs generiques
   static final String WNOZOOM = "You have reached the zoom limit";
   static final int CMSIZE = 150;      // Taille de la portion d'image pour la CM dynamique
   Font F = Aladin.SPLAIN;
   static protected int INITW = 512;
   static protected int INITH = 512;
   protected Color gridColor = Aladin.GREEN;
   protected Color gridColorRA  = gridColor.brighter();
   protected Color gridColorDEC = gridColor.brighter().brighter();
   protected int gridFontSize = Aladin.SSIZE;

   // Les references aux autres objets
   Aladin aladin;
   Calque calque;
   ZoomView zoomview;
   Status status;

   // Différents mode d'affichage de la valeur du pixel
   static final int LEVEL   = 0;
   static final int REAL    = 1;
   static final int INFILE  = 2;
   static final int REALX   = 3;

   // Les composantes de l'objet
   Vector<Obj> vselobj = new Vector<Obj>(500); // Vecteur des objets selections
   String saisie ="";          // La saisie en cours (cf Localisation)
   boolean nextSaisie=true;    // Va remettre a zero saisie a la prochaine frappe
   protected Repere repere;    // Le repère de la position courante
   protected CropTool crop; // Le rectangle de sélection pour un crop
   protected Constellation constellation; // Tracé des constellations

   // Les valeurs a memoriser
   boolean first=true;           // Pour afficher au demarrage du Help

   // Les valeurs a memoriser pour montrer les sources
   boolean _flagTimer=false;      // pour l'aiguillage du run()
   boolean _flagSesameResolve=false; // pour l'aiguillage du run()

   // Gestion du mode blinking
   protected int blinkMode=0;    // Etat du blink

   protected Obj newobj;

   // Gestion de la configuratin d'affichage Multiview
   protected JPanel mviewPanel;        // JPanel du multiview (on n'utilise pas directement
   // celui de this car on veut pouvoir le bidouiller
   // facilement (remove...)
   protected ViewMemo viewMemo;       // Objet mémorisant les paramètres de toutes les vues
   // même celles non visibles (scrollBar)
   protected ViewMemo viewSticked;	  // Mémorisation des vues stickées
   private boolean memoStick[];       // Flag sur les vues stickées
   protected int mouseView=-1;        // La vue sous la souris
   protected ViewSimple viewSimple[]; // Le tableau des vues, 16 par défaut
   protected int currentView;         // L'indice de la vue courante dans viewSimple[]
   protected int modeView= ViewControl.DEFAULT; // Le nombre de vues simultanées
   protected MyScrollbar scrollV;     // La scrollbar Vertical
   private int previousScrollGetValue=0; // Indice de la première vue visible
   private Point memoPos=null;        // sert à mémoriser la précédente position
   // du scroll (x) et de la vue courante (y)
   // dans le cas où on reviendrait à un modeView
   // compatible avec cette mémorisation
   protected boolean selectFromView=false; // true si la dernière selection d'un plan a été
   // faite en cliquant dans un SimpleView

   // Gestion du MegaDrag (déplacement/affectation de vue)
   private ViewSimple megaDragViewSource=null;  // Vue source, ou null si aucune
   private Plan megaDragPlanSource=null;        // Plan de stack source, ou null si aucun
   private ViewSimple megaDragViewTarget=null;  // Vue destination ou null si non encore connue
   protected boolean flagMegaDrag=false;        // true si on a commencé un flagDrag dans une vue ou dans la pile
   protected boolean flagTaquin=false;          // true si on est en mode taquin

   protected boolean flagHighlight=false;       // true si on est en mode highlight des sources (voir hist[] dans ZommView)

   static protected String NOZOOM,MSTICKON,MSTICKOFF,MOREVIEWS,
   MLABELON,MCOPY,MCOPYIMG,MLABELOFF,/*MROI,*/MNEWROI,MDELROI,MSEL,MDELV,
   VIEW,ROIWNG,ROIINFO,HCLIC,HCLIC1,NIF,NEXT;

   protected void createChaine() {
      NOZOOM    = aladin.chaine.getString("VWNOZOOM");
      MSTICKON  = aladin.chaine.getString("VWMSTICKON");
      MSTICKOFF = aladin.chaine.getString("VWMSTICKOFF");
      MCOPYIMG  = aladin.chaine.getString("VWMCOPYIMG");
      MCOPY     = aladin.chaine.getString("VWMCOPY");
      MLABELON  = aladin.chaine.getString("VWMLABELON");
      MLABELOFF = aladin.chaine.getString("VWMLABELOFF");
      MOREVIEWS = aladin.chaine.getString("VWMOREVIEWS");
      NEXT      = aladin.chaine.getString("VWNEXT");
      //      MROI      = aladin.chaine.getString("VWMROI");
      MNEWROI   = aladin.chaine.getString("VWMNEWROI");
      MDELROI   = aladin.chaine.getString("VWMDELROI");
      MSEL      = aladin.chaine.getString("VWMSEL");
      MDELV     = aladin.chaine.getString("VWMDELV");
      VIEW      = aladin.chaine.getString("VWVIEW");
      ROIWNG    = aladin.chaine.getString("VWROIWNG");
      ROIINFO   = aladin.chaine.getString("VWROIINFO");
      HCLIC     = aladin.chaine.getString("VWHCLIC");
      HCLIC1    = aladin.chaine.getString("VWHCLIC1");
      NIF       = aladin.chaine.getString("VWNIF");
   }

   
   protected View(Aladin aladin) { this.aladin=aladin; aladin.view=this; }
   
   /** Creation de l'objet View
    * @param aladin Reference
    */
   protected View(Aladin aladin,Calque calque) {
      this.aladin = aladin;
      createChaine();
      this.status = aladin.status;
      this.calque = calque;
      this.zoomview = aladin.calque.zoom.zoomView;
      //      if( aladin.STANDALONE ) INITW=700;

      int nitem = aladin.viewControl.getNbCol(ViewControl.DEFAULT);
      scrollV = new MyScrollbar(Scrollbar.VERTICAL,0,nitem,0,ViewControl.MAXVIEW/nitem);
      scrollV.setUnitIncrement(nitem);
      scrollV.setBlockIncrement(nitem);
      scrollV.addAdjustmentListener(this);
      mviewPanel = new JPanel();
      mviewPanel.setBackground(Color.lightGray);

      setLayout( new BorderLayout(0,0) );
      add("Center",mviewPanel);
      //      add("West",scrollV);

      // Création du repère
      createRepere();

      // Création des éléments gérant le multiview
      viewMemo = new ViewMemo();
      viewSticked = new ViewMemo();
      viewSimple = new ViewSimple[ViewControl.MAXVIEW];
      memoStick = new boolean[ViewControl.MAXVIEW];
      int w = INITW/aladin.viewControl.getNbCol(modeView);
      int h = INITH/aladin.viewControl.getNbLig(modeView);;
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         viewSimple[i]=new ViewSimple(aladin,this,w,h,i);
         viewMemo.set(i,viewSimple[i]);
         viewSticked.set(i,(ViewSimple)null);
      }
      adjustPanel(modeView);
      setCurrentNumView(0);
      setBackground(Color.white);

      registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { next(1); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_TAB,InputEvent.SHIFT_MASK),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { next(-1); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
   }

   /** Pour déterminer par la suite si la dernière sélection d'un SimpleView
    *  a été faite en cliquant dans le SimpleView ou bien par conséquence
    *  d'un clic dans la pile
    *  On en profite pour éventuellement scroller la pile pour rendre visible
    *  le slide correspondant au plan de base
    */
   protected void setSelectFromView(boolean flag) {
      selectFromView=flag;
      if( flag ) aladin.calque.select.showSelectedPlan();
   }

   /** Retourne true si on peut/doit effacer au-moins une ou plusieurs vues */
   protected boolean isViewSelected() {
      if( !selectFromView ) return false;
      //       if( modeView==ViewControl.MVIEW1 ) return false;
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) if( viewSimple[i].selected ) return true;
      return false;
   }

   /**
    * Sélection de la vue indiquée
    */
   protected void setSelect(ViewSimple v) {
      for( int i=0; i<viewSimple.length; i++ ) {
         if( v==viewSimple[i] ) { setSelect(i); paintBordure(); return; }
      }
      return;
   }

   /**
    * Sélection de la vue indiquée afin que l'on puisse proprement
    * effacer le plan au cas où ! (comprenne qui pourra)
    */
   protected void setSelect(int nview) {
      setSelectFromView(false);
      unSelectAllView();
      viewSimple[nview].selected=true;
   }

   /** Sélection de toutes les vues (via le menu) */
   protected void selectAllViews() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( !viewSimple[i].isFree() ) selectView(i);
         viewSimple[i].paintBordure();
      }
   }

   /** Déselection de toutes les vues sauf la vue courante */
   protected void unselectViewsPartial() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( !viewSimple[i].isFree() && i!=currentView ) {
            viewSimple[i].selected = false;
            if( viewSimple[i].pref!=null  ) viewSimple[i].pref.selected=false;
            viewSimple[i].paintBordure();
         }
      }
      aladin.calque.select.repaint();
   }

   /**
    * Positionnement d'une frame donnée pour une vue blink. Possibilité de
    * synchroniser toutes les vues ayant le même plan de référence
    * @param v vue concernée
    * @param frameLevel numéro de la frame
    * @param sync true si on doit l'appliquer, non pas uniquement à "v" mais
    *             à toutes les vues.
    */
   protected void setCubeFrame(ViewSimple v,double frameLevel, boolean sync) {
      // Pas de synchronisation des frames, facile !
      if( !sync ) { v.cubeControl.setFrameLevel(frameLevel); return; }

      // Synchronisation de toutes les vues ayant le même plan blink de référence
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].pref==v.pref && viewSimple[i].pref.selected ) viewSimple[i].cubeControl.setFrameLevel(frameLevel);
      }
   }

   /**
    * Synchronisation de toutes les vues blinks sur le même plan de référence
    * @param v la vue de référence
    */
   protected void syncCube(ViewSimple v) {
      int m =getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].pref==v.pref && viewSimple[i].cubeControl!=v.cubeControl ) {
            viewSimple[i].cubeControl.syncBlink(v.cubeControl);
         }
      }
   }

   /** Active un Rainbow en superpostion de la vue courante
    * @param cm la table des couleurs à visualiser
    * @param min la valeur correspondante au 0 de la cm
    * @param max la valeur correspondante au 255 de la cm
    */
   public Rainbow showRainbowFilter(ColorModel cm,double min, double max) {
      getCurrentView().rainbowF = new Rainbow(aladin,cm,min,max);
      return getCurrentView().rainbowF;
   }

   public void showRainbow(boolean active) {
      ViewSimple v = getCurrentView();
      if( !active && v.rainbow==null ) return;
      if( active && v.rainbow==null ) v.rainbow = new RainbowPixel(aladin,v);
      else v.rainbow.setVisible(active);
   }

   public boolean hasRainbow() {
      return getCurrentView().hasRainbow();
   }

   public boolean rainbowAvailable() {
      return getCurrentView().rainbowAvailable();
   }

   // Valeurs des zoom des vues sélectionnées par selectCompatibleViews(), -1 sinon
   //    private ViewMemo cpView = new ViewMemo();

   //    /** Déselection des vues précédemment sélectionnées via selectCompatibleViews()
   //     *  (SHIFT dans le ZoomView) - utilise le talbeau initialZoomCompatibleViews[]
   //     * pour lequel les valeurs != -1 indiquent les vues concernées
   //     */
   //    protected void unselectCompatibleViews() {
   //       return;
   //       ViewSimple v ;
   //       for( int i=0; i<modeView; i++ ) {
   //          if( (v=cpView.get(i,viewSimple[i]))==null ) continue;
   //          v.setZoomXY(v.zoom,v.xzoomView,v.yzoomView);
   //          v.newView(1);
   //       }
   //       repaint();
   //    }

   /** Sélectionne toutes les vues compatibles si ce n'est déjà fait,
    * sinon déselectionne toutes les vues sauf la vue courante
    * @return true si on sélectionne, false si on déselectionne
    */
   protected boolean switchSelectCompatibleViews() {
      if( isSelectCompatibleViews() ) {
         unselectViewsPartial();
         return false;
      } else { selectCompatibleViews(); return true; }
   }

   /** Sélection de toutes les vues compatibles avec la vue courante (SHIFT zoomView) */
   protected void selectCompatibleViews() {
      ViewSimple cv = getCurrentView();
      if( !Projection.isOk(cv.pref.projd) ) return;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].isFree() ) continue;
         if( cv==viewSimple[i] ) continue;
         if( !cv.pref.projd.agree(viewSimple[i].pref.projd,viewSimple[i]) ) continue;
         //          cpView.set(i,viewSimple[i]);
         selectView(i);
         viewSimple[i].paintBordure();
      }
   }

   /** Retourne true si toutes les vues compatibles sont déjà sélectionnées */
   protected boolean isSelectCompatibleViews() {
      ViewSimple cv = getCurrentView();
      if( cv==null || cv.pref==null || !Projection.isOk(cv.pref.projd) ) return false;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].isFree() ) continue;
         if( cv==viewSimple[i] ) continue;
         if( !cv.pref.projd.agree(viewSimple[i].pref.projd,viewSimple[i]) ) continue;
         if( !viewSimple[i].selected ) return false;
      }
      return true;
   }

   /** Retourne true s'il y a au moins une vue compatible avec la vue courante (SHIFT zoomView)
    * non déjà sélectionnée, n'utilisant pas le même plan de référence
    */
   protected boolean hasCompatibleViews() {
      try {
         ViewSimple cv = getCurrentView();
         if( cv==null || cv.pref==null || !Projection.isOk(cv.pref.projd) ) return false;
         int m=getNbView();
         for( int i=0; i<m; i++ ) {
            if( viewSimple[i].isFree() || cv==viewSimple[i] ) continue;
            if( cv.pref==viewSimple[i].pref && cv.pref.getZ()==viewSimple[i].pref.getZ() ) continue;
            if( viewSimple[i].selected ) continue;
            if( cv.pref.projd.agree(viewSimple[i].pref.projd,viewSimple[i]) ) return true;
         }
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return false;
   }

   /** Retourne le nombre de vues sélectionnées */
   protected int nbSelectedViews() {
      int i=0;
      int m =getNbView();
      for( i=0; i<m; i++ ) if( viewSimple[i].selected ) i++;
      return i;
   }

   /** Selection de la vue */
   protected void selectView(int nview) {
      setSelectFromView(true);
      viewSimple[nview].selected=true;
   }

   /** Retourne la première vue sélectionnée, sinon retourne -1 */
   protected int getFirstSelectedView() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) if( viewSimple[i].selected ) return i;
      return -1;
   }

   /** Retourne la première vue sélectionnée, visible
    * dont le plan de référence est celui passé en paramètre, sinon null */
   protected ViewSimple getFirstSelectedView(Plan p) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].selected && viewSimple[i].pref==p ) return viewSimple[i];
      }
      return null;
   }

   /** Sélectionne toutes les vues qui ont p comme plan de référence
    *  mais ne déselectionne pas les vues déjà sélectionnées
    *  @return le numéro de la première vue concernée
    */
   protected int selectView(Plan p) {
      int rep=-1;
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         ViewSimple v = viewSimple[i];
         if( v.isFree() ) continue;
         if( v.pref==p ) {
            v.selected=true;
            if( rep==-1 ) rep=v.n;
         }
      }
      return rep;
   }

   /** Déselectionne toutes les vues */
   protected void unSelectAllView() {
      for( int i=0; i<viewSimple.length; i++ ) viewSimple[i].selected=false;
   }

   /** Reset du tracage des bords des autres images en cours de visualisation
    * (souris sort de la pile) */
   protected void resetBorder() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].oldPlanBord=null;
   }

   /** Force la recreation de tous les buffers MemoryImage qui affiche
    * une portion de l'image passée en paramètre. Sert à palier un bug sous Linux
    * On met simplement à 0 une variable d'état qui entrainera a regénération du buffer
    * voir ViewSimple.getImageView();
    * @param pimg L'image concernée
    */
   protected void recreateMemoryBufferFor(PlanImage pimg) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].pref==pimg ) viewSimple[i].pHashCode=0;
      }
   }

   // Reset d'un megaDrag
   protected void resetMegaDrag() {
      megaDragViewSource=null;
      megaDragPlanSource=null;
      flagMegaDrag = false;
   }

   /** Retourne true si le MegaDrag peut être réalisé a priori */
   protected boolean isMegaDrag() {
      return flagMegaDrag && !aladin.isFullScreen();
   }

   /** Démarre un MegaDrag potentiel sur une vue vSource */
   protected void startMegaDrag(ViewSimple vSource) {
      if( isMegaDrag() ) return;
      megaDragViewSource=vSource;
      flagMegaDrag=true;
   }

   /** Démarre un MegaDrag potentiel sur un plan de stack pSource */
   protected void startMegaDrag(Plan pSource) {
      if( isMegaDrag() ) return;
      megaDragPlanSource=pSource;
      flagMegaDrag=true;
   }

   /** Retourne true s'il s'agit d'indices de vues adjacentes */
   private boolean isAcote(int s, int t) {
      int n = (int)Math.sqrt(modeView);
      int s1 = s%n;
      if( t==s-1 && s1>0 ) return true;
      if( t==s+1 && s1<n-1 ) return true;
      if( t==s+n && s<modeView-n ) return true;
      if( t==s-n && s>n-1 ) return true;
      return false;
   }

   /** Retourne true si on a fini le taquin */
   private boolean isTaquinOk() {
      boolean rep=true;
      int m=getNbView();
      for( int i=0; i<m-1; i++ ) {
         System.out.print(" "+viewSimple[i].ordreTaquin);
         if( viewSimple[i].ordreTaquin!=i ) rep=false;
      }
      System.out.println(" => "+rep);
      compute();
      return rep;
   }

   private void compute() {
      int perm=0,n,on=0;
      for( int i=0; i<modeView; i++ ) {
         n=viewSimple[i].ordreTaquin;
         if( n==-1 ) n=15;
         if( i>1 ) {
            if( on>n ) perm++;
         }
         on=n;
      }
      //      System.out.println("Permutation : "+perm);
   }

   /** Effectue si possible un MegaDrag en fonction des paramètres positionnés
    *  au préalable (flagMegaDrag,megaDragViewSource,megaDragPlanSource)
    *  @return true si le MagaDrag est fait, false sinon
    */
   protected boolean stopMegaDrag(Object target,int x, int y,boolean ctrlPressed) {
      boolean rep=true;

      if( !isMegaDrag() ) return false;

      aladin.makeCursor(aladin.toolBox,Aladin.DEFAULTCURSOR);
      setMyCursor(Aladin.DEFAULTCURSOR);

      // Détermination de la vue de destination
      int i = getTargetViewForEvent(target,x,y);
      megaDragViewTarget = i<0?null:viewSimple[i];

      if( !flagMegaDrag
            || megaDragViewSource==null && megaDragPlanSource==null
            || megaDragViewTarget==null ) {
         rep=false;
      }

      //if( rep ) System.out.println("MegaDrag Source = "+ (megaDragViewSource!=null?(megaDragViewSource+""):(""+megaDragPlanSource))+" Target="+megaDragViewTarget);

      // La cible est soit-même => on ne fait rien
      if( rep && megaDragViewTarget==megaDragViewSource ) rep=false;

      // En mode Taquin la source doit être vide et juste à coté
      if( rep && flagTaquin &&
            (megaDragPlanSource!=null
            || !megaDragViewTarget.isFree()
            || !isAcote(megaDragViewSource.n,megaDragViewTarget.n)) ) rep=false;

      flagMegaDrag=false;

      // Ajout a une vue ayant un plan BLINK ou MOSAIC
      if( rep && !megaDragViewTarget.isFree()
            && (megaDragViewTarget.pref instanceof PlanImageBlink
                  || megaDragViewTarget.pref.type==Plan.IMAGEMOSAIC) ) {
         Plan p = megaDragPlanSource!=null?megaDragPlanSource:megaDragViewSource.pref;
         if( p==null || !p.isSimpleImage() || !Projection.isOk(p.projd)
               || !Projection.isOk(megaDragViewTarget.pref.projd) ) rep=false;
         else {
            if( !aladin.confirmation(aladin.chaine.getString("ADDFRAMECONF"))) rep=false;
            else if( megaDragViewTarget.pref instanceof PlanImageBlink)
               ((PlanImageBlink)megaDragViewTarget.pref).addPlan((PlanImage)p);
            else ((PlanImageMosaic)megaDragViewTarget.pref).addPlan((PlanImage)p);
            megaDragViewSource=null;
            megaDragPlanSource=null;
            megaDragViewTarget=null;
            return rep;
         }
      }

      // Création ou ajout d'un plan catalogue à une vue Plot
      else if( rep && (ctrlPressed && megaDragViewTarget.isFree() || megaDragViewTarget.isPlotView() )
            && megaDragPlanSource!=null && megaDragPlanSource.isSimpleCatalog() ) {
         Plan p = megaDragPlanSource;
         ViewSimple v = megaDragViewTarget;
         if( v.isFree() ) v.setPlanRef(p, false);
         v.addPlotTable(p, 0, 1,true);
      }

      //      // Creation d'un plan MOSAIC
      //      // TROP SURPRENANT, POUR L'INSTANT ON NE L'IMPLANTE PAS
      //      else if( rep && !megaDragViewTarget.isFree()
      //            && megaDragViewTarget.pref.isSimpleImage() ) {
      //         Plan p = megaDragPlanSource!=null?megaDragPlanSource:megaDragViewSource.pref;
      //         if( p==null || !p.isSimpleImage() ) rep=false;
      //         else {
      //            PlanImage pi[] = new PlanImage[2];
      //            pi[0] = (PlanImage) megaDragViewTarget.pref;
      //            pi[1] = (PlanImage) p;
      //            aladin.calque.newPlanImageMosaic(pi,megaDragViewTarget);
      //            megaDragViewSource=null;
      //            megaDragPlanSource=null;
      //            megaDragViewTarget=null;
      //            return rep;
      //         }
      //      }

      // Copie ou déplacement d'une vue
      else if( rep && megaDragViewSource!=null ) {
         boolean copy = ctrlPressed;
         aladin.console.printCommand( (copy ? "copy ":"mv ")+
               getIDFromNView(megaDragViewSource.n)+" "+
               getIDFromNView(megaDragViewTarget.n));
         moveOrCopyView(megaDragViewSource.n,megaDragViewTarget.n,copy);
      }

      // Affectation d'un plan de référence
      else if( rep ) {
         if( !aladin.calque.canBeRef( megaDragPlanSource ) ) rep=false;
         if( rep ) {
            aladin.console.printCommand("cview "+Tok.quote(megaDragPlanSource.label)+
                  " "+getIDFromNView(megaDragViewTarget.n));
            unSelectAllView();
            setPlanRef(megaDragViewTarget.n,megaDragPlanSource);
         }
      }

      // Ca a marché
      if( rep ) {
         unSelectAllView();
         megaDragViewTarget.selected=true;
         aladin.calque.selectPlan(megaDragViewTarget.pref);
         setCurrentView(megaDragViewTarget);
         repaintAll();
         aladin.calque.select.repaint();

         // Fin du taquin
         if( flagTaquin && isTaquinOk() ) taquinOk();
      }

      megaDragViewSource=null;
      megaDragPlanSource=null;
      megaDragViewTarget=null;
      return rep;
   }

   /** Déplacementd'une vue
    * @param nviewSrc numéro de la vue source
    * @param nviewTarget numéro de la vue destination
    */
   protected void moveView(int nviewSrc,int nviewTarget) {
      moveOrCopyView(nviewSrc,nviewTarget,false);
   }
   /** Copie d'une vue
    * @param nviewSrc numéro de la vue source
    * @param nviewTarget numéro de la vue destination
    */
   protected void copyView(int nviewSrc,int nviewTarget) {
      moveOrCopyView(nviewSrc,nviewTarget,true);
   }
   /** Déplacement ou copie d'une vue
    * @param nviewSrc numéro de la vue source
    * @param nviewTarget numéro de la vue destination
    * @param flagCopy true s'il faut faire une copie plutôt qu'un déplacement
    */
   synchronized private void moveOrCopyView(int nviewSrc,int nviewTarget,boolean flagCopy) {
      ViewSimple v = viewSimple[nviewTarget];
      ViewControl.moveViewOrder(viewSimple,nviewSrc,nviewTarget,flagCopy);
      v.newView(1);
      Properties.majProp(2);
      repaintAll();
   }

   /** Retourne l'indice de la vue en fonction d'un évènement
    *  dont l'origine peut être soit la pile, soit une autre vue
    *  Prend en compte la configuration de l'affichage d'Aladin.
    *  @param e l'évènement en question
    *  @return l'indice de la vue cible, -1 si problème
    */
   protected int getTargetViewForEvent(Object source, int origX, int origY) {
      Dimension vueDim = viewSimple[0].getSize();
      int vueInCol = aladin.viewControl.getNbCol(modeView);
      int x=0, y=0;   // Position de l'évènement par rapport à View
      if( source instanceof Select ) {
         if( origX>=0 ) return -1;  // On est resté dans la pile
         x = getSize().width + aladin.toolBox.getSize().width +10 + origX;
         y = origY;
      } else if( source instanceof ViewSimple ) {
         int currentView = ((ViewSimple)source).isProjSync() ?((ViewSimple)source).n
               : getCurrentNumView();
         x = (currentView%vueInCol)*vueDim.width + origX;
         y = (currentView/vueInCol)*vueDim.height + origY;
      } else return -1;

      int t = vueInCol*(y/vueDim.height) + (x/vueDim.width);
      if( t<0 || t>=getNbView() ) t=-1;
      return t;
   }

   /** Crée un tableau de Position en fonction d'un Vector d'Objet
    * en supprimant tous les doublons,les objets trop proche ou ceux qui sont
    * en dehors d'une des vues
    * @param v La liste des objets originaux
    * @param vc[] liste des views concernées
    * @param dist la distance limite de proximité
    * @return un tableau de Positions
    */
   protected Position[] getSourceList(Vector v,ViewSimple vc[], double dist) {
      // Mémorise les Positions dans un tableau temporaire
      Position a[] = new Position[v.size()];
      int n=0,m;
      Enumeration e = v.elements();
      while( e.hasMoreElements() ) {
         Obj o = (Obj)e.nextElement();

         // On ne garde que les Sources et les Reperes (tags)
         if( !(o instanceof Source) && !(o instanceof Repere) ) continue;
         Position s = (Position)o;

         // présent dans toutes les vues ?
         boolean flagOut=false;
         for( int i=0; i<vc.length; i++ ) {
            PlanImage p = (PlanImage)vc[i].pref;
            if( p instanceof PlanBG ) continue;
            int x=(int)s.xv[vc[i].n];
            int y=(int)s.yv[vc[i].n];
            if( x<=0 || y<=0 || x>p.naxis1 || y>p.naxis2 ) { flagOut=true; break; }
         }
         //if( flagOut ) System.out.println("*** "+s.id+" est en dehors");
         if( flagOut ) continue;

         a[n++]=s;
      }

      // Marques les sources en doublons ou trop proche via
      // un tableau de flag
      boolean tooClose[] = new boolean[n];
      Coord o1 = new Coord();
      Coord o2 = new Coord();
      m=n;		// Pour connaître le nombre de sources restantes
      for( int i=0; i<n; i++ ) {
         if( tooClose[i] ) continue;
         o1.al  = a[i].raj;
         o1.del = a[i].dej;
         for( int j=i+1; j<n; j++ ) {
            o2.al  = a[j].raj;
            o2.del = a[j].dej;
            if( Coord.getDist(o1,o2)<=dist ) { tooClose[j]=true; m--; }
         }
      }

      // Crée le tableau de sources final
      Position b[] = new Position[m];
      try {
         for( int i=0,j=0; i<n; i++ ) {
            if( !tooClose[i]) b[j++]=a[i];
         }
      } catch( Exception e1 ) {
         if( aladin.levelTrace>=3 ) e1.printStackTrace();
      }

      return b;
   }

   /** Création de ROI autour de la position indiquée pour toutes les
    * vues sélectionnées. */
   protected void createROI() {
      Aladin.makeCursor(aladin,Aladin.WAITCURSOR);
      double radius = createROIInternal(0,0,true);
      if( radius>0 ) aladin.console.printCommand("thumbnail "+Coord.getUnit(radius));
   }

   /** Crée des vues ROI de taillePixel de large */
   protected void createROI(int taillePixel) { createROIInternal(0,taillePixel,false); }

   /** Crée des vues ROI de tailleRadius de large */
   protected void createROI(double tailleRadius) { createROIInternal(tailleRadius,0,false); }

   /**
    * Méthode interne, normalement pas appelé directement
    * @param tailleRadius taille en arcmin
    * @param taillePixel taille en pixel (pris en compte ssi tailleRadius==0)
    * @param dialog true s'il y a dialogue avec l'utilisateur
    * @return la taille des champs ROI en degrés
    */
   private double createROIInternal(double tailleRadius,int taillePixel,boolean dialog) {

      int first = -1;

      // Détermination des views images concernées
      Vector<ViewSimple> vcVect = new Vector<ViewSimple>();

      // On commence par les plans sélectionnés
      // CA NE MARCHE PAS
      //      if( vcVect.size()==0 ) {
      //         Plan [] plan = aladin.calque.getPlans();
      //         for( Plan p : plan ) {
      //            if( !p.flagOk || !p.isPixel() || !p.selected ) continue;
      //            if( isUsed(p) ) continue;
      //            ViewSimple v = createViewForPlan(p);
      //            vcVect.add(v);
      //         }
      //      }

      int m = getNbView();
      
      // On prend en compte les vues correspondnantes
      for( int i=0; i<m; i++ ) {
         ViewSimple vc=viewSimple[i];
         if( vc.isFree() || !vc.pref.flagOk || !vc.pref.isPixel() ) continue;
         if( !vc.selected ) continue;
         if( !vcVect.contains(vc ) ) vcVect.add(vc);
      }

      // Toujours aucune vue => on prend la vue de base
      if( vcVect.size()==0 ) vcVect.add(getCurrentView());

      ViewSimple vca[] = new ViewSimple[vcVect.size()];
      Enumeration<ViewSimple> e = vcVect.elements();
      for( int i=0; e.hasMoreElements(); i++ ) vca[i]=e.nextElement();

      // Détermination des objets concernées... soit les objets sélectionnés
      // soit ceux du premier plan qui va bien
      if( !hasSelectedObj() )  {
         Plan [] allPlans = calque.getPlans();
         for( int i=0; i<allPlans.length; i++ ) {
            Plan p = allPlans[i];
            if( !(p instanceof PlanTool || p.isCatalog() ) || !p.flagOk ) continue;
            Iterator<Obj> it = p.iterator();
            while( it.hasNext() ) {
               Obj o = it.next();
               if( !(o instanceof Source) && !(o instanceof Repere) ) continue;
               setSelected(o,true);
            }
            if( hasSelectedObj() ) {
               repaintAll();
               aladin.mesure.mcanvas.repaint();
               break;
            }
         }
      }


      // Récupération des sources concernées dans l'ordre des mesures
      Vector<Obj> vsel = new Vector<Obj>();
      for( int i=0; i<aladin.mesure.nbSrc; i++ ) vsel.add(aladin.mesure.src[i]);

      // Détermination de la taille si non précisée
      if( tailleRadius==0 ) {
         ViewSimple vc = vca[0];

         double taille = Math.min( vc.getTailleRA(), vc.getTailleDE() );
         // Détermination de la taille du champ en se basant sur la première vue
         if( taille<1 ) tailleRadius=taille;

         // Sinon des valeurs par défaut
         else {
            Projection proj = vc.getProj();
            if( taillePixel==0 ) taillePixel= vc.pref instanceof PlanBG ? 20 : 40;  // 40 pixels par défaut
            double sizePixel = proj.c.getImgWidth()/proj.c.getImgSize().width;
            tailleRadius = sizePixel*taillePixel;
         }
      }


      // Détermination des sources en éliminant les doublons ou les objets
      // trop proches ou les objets hors champ
      Position src[] = getSourceList(vsel,vca,tailleRadius==0?3./3600:tailleRadius/2);
      if( src.length==0 ) {
         aladin.warning(ROIWNG);
         return 0;
      } else {
         if( !Aladin.NOGUI && dialog ) {
            Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);

            String pl = vca.length>1 ? "s" : "";
            StringBuffer stat = new StringBuffer();
            for( int i=0; i<vca.length; i++ ) {
               stat.append("\n   .Image: "+vca[i].pref.label);
            }
            pl = src.length>1 ? "s" : "";
            stat.append("\n   .Object"+pl+": "+src.length);
            //            if( !aladin.confirmation(ROIINFO+stat) ) return;

            Panel panel = new Panel();
            TextField radiusROI = new TextField(5);
            radiusROI.setText( Coord.getUnit(tailleRadius) );
            panel.add( new Label("Thumbnail size (arcmin):") );
            panel.add(radiusROI);
            if( Message.showFrame(this,ROIINFO+stat, panel, Message.QUESTION)!=Message.OUI ) return 0;
            tailleRadius = Server.getRM( radiusROI.getText() )/60;
         }
      }

      if( !isMultiView() ) setModeView(ViewControl.MVIEW9);
      
      m = getNbView();

      for( int j=0; j<src.length; j++ ) {
         Position o = src[j];
         //System.out.println("Source "+o.id);

         for( int i=0; i<vca.length; i++ ) {
            ViewSimple v;
            ViewSimple vc=vca[i];
            if( vc.isFree() || !vc.pref.flagOk || !vc.pref.isPixel() ) continue;
            if( !vc.selected ) continue;

            int n=getNextNumView();
            if( n==-1 ) {
               v= new ViewSimple(aladin,this,viewSimple[0].rv.width,viewSimple[0].rv.height,0);
            } else v = viewSimple[n];

            vc.copyIn(v);
            int width = isMultiView() ? v.rv.width : v.rv.width/3;
            v.setCenter(o.raj,o.dej);
            v.setZoomByRadius(tailleRadius,width);
            v.locked=true;
            //            if( !(v.pref instanceof PlanBG) ) v.locked= true;
            //            else {
            ////               v.setZoomXY(v.zoom,-1,-1);
            //               System.out.println("zoom="+v.zoom+" v.rzoom="+v.rzoom);
            //               v.pref=v.cropAreaBG(v.rzoom,Coord.getSexa(o.raj, o.dej),v.zoom,1,false,true);
            //            }

            if( n==-1 ) {
               n=viewMemo.setAfter(previousScrollGetValue+m-1 -getNbStickedView(),v);
            }
            if( first==-1 ) first=n;

         }
      }
      aladin.log("createROI","["+src.length+" position(s)]");
      scrollOn(0);
      repaintAll();
      //      if( first>=0 ) scrollOn(first);

      return tailleRadius;
   }

   /** Création d'une vue pour le Plan en paramètre */
   protected ViewSimple createViewForPlan(Plan p) {
      int n=getNextNumView();
      ViewSimple v;
      if( n==-1 ) {
         v= new ViewSimple(aladin,this,viewSimple[0].rv.width,viewSimple[0].rv.height,0);
      } else v = viewSimple[n];

      // Il faudrait tester s'il y a déjà une vue avec ce plan, et si c'est le
      // cas faire un lock (en tout cas en mode OUTREACH)
      // A FAIRE !!

      v.setPlanRef(p,false);
      p.setActivated(true);

      if( n== -1) viewMemo.setAfter(previousScrollGetValue+getNbView()-1
            -getNbStickedView(),v);
      return v;
   }


   private ViewSimple lastClickView=null;   // dernière vue cliqué

   /** Mémorise la dernière vue cliquée */
   protected void setLastClickView(ViewSimple v) {
      lastClickView=v;
   }

   /** Retourne la dernière vue cliquée, et à défaut la vue courante */
   protected ViewSimple getLastClickView() {
      return lastClickView!=null ? lastClickView : getCurrentView();
   }


   /** Positionnement de la vue courante et réaffichage
    *  @return true s'il y a eu effectivement changement de vue
    *          false si c'était déjà celle-là
    */
   synchronized protected boolean setCurrentView(ViewSimple v) {
      if( currentView>=0 && currentView<viewSimple.length
            && viewSimple[currentView]==v )  {
         if( !v.isFree() && aladin.dialog!=null && !(v.pref instanceof PlanBG) ) aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),4);
         return false;
      }

      setLastClickView(null);
      currentView = v.n;
      setMouseView(v);
      if( !v.isFree() ) v.pref.selected=true;
      aladin.calque.zoom.reset();
      aladin.calque.select.repaint();
      if( v.isFree() || aladin.toolBox.tool[ToolBox.SELECT].mode==Tool.UNAVAIL ) aladin.toolBox.toolMode();
      if( !v.isFree() && !(v.pref instanceof PlanBG) ) aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),4);
      return true;
   }

   /** Recherche et positionnement (si possible) de la vue courante parmi
    *  les vues déjà sélectionnées
    *  @return true si on a pu le faire, sinon false
    */
   protected boolean setCurrentView() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( !viewSimple[i].isFree() && viewSimple[i].selected ) {
            setCurrentView(viewSimple[i]);
            return true;
         }
      }
      return false;
   }

   /** Positionne le curseur en fonction de l'outil courant */
   protected void setDefaultCursor() {
      int tool=aladin.toolBox.getTool();
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].setDefaultCursor(tool,false);
   }

   /** Création du réticule par défaut, notamment lorsque je charge du AJ */
   protected void setDefaultRepere() {
      try {
         ViewSimple v = getCurrentView();
         Coord c = v.getCooCentre();
         if( c!=null ) setRepere(c);
      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

   }

   /** Dans le cas où on a remplacé un plan par un autre (cas d'un calcul
    * arithmétique par exemple), il faut réaffecter le "nouveau" plan aux
    * anciennes vues qui l'utilisaient. Ces vues seront celles dont le plans
    * de référence n'appartient plus à la pile
    * @param p
    */
   protected void adjustViews(Plan p) {
      for( int i=0; i<viewSimple.length; i++ ) {
         ViewSimple v = viewSimple[i];
         if( v.isFree() ) continue;
         if( aladin.calque.getIndex(v.pref)>=0 ) continue;
         //System.out.println("La vue "+getIDFromNView(i)+" change son pref="+p.label);
         v.pref = p;
         v.newView(1);
         v.repaint();
      }
   }

   /**
    * Positionne la vue et/ou le plan par défaut (en cas de suppression)
    */
   protected void findBestDefault() {
      int i,j;

      // pas encore pret
      if( currentView==-1 ) return;

      // Si la vue courante n'est pas vide...
      if( !viewSimple[currentView].isFree() ) {

         Plan p = viewSimple[currentView].pref;

         // Si le repère n'a jamais été initialisé, je vais
         // le faire au centre de la vue
         if( repere.raj==Double.NaN ) {
            try { moveRepere( p.projd.c.getImgCenter() ); }
            catch( Exception e ) {}
         }

         // plan correspondant déjà sélectionné alors rien à faire
         if( p.selected ) return;

         // On sélectionne le plan tout comme il faut
         aladin.calque.setPlanRef(p,currentView);

         return;
      }

      // Y aurait-il une autre vue déjà créée que je pourrais prendre
      // comme vue par défaut
      for( i=0, j=currentView+1; i<viewSimple.length; i++,j++ ) {
         if( j==viewSimple.length ) j=0;
         if( !viewSimple[j].isFree() ) {
            //System.out.println("Nouvelle vue par défaut "+viewSimple[j]);
            setCurrentView(viewSimple[j]);
            return;
         }
      }

      // Il n'y a plus aucune vue, je vais donc rechercher
      // un plan de référence parmi les images dans la piles
      Plan allPlans[] = aladin.calque.getPlans();
      for( j=-1, i=0; i<allPlans.length; i++ ) {
         Plan p=allPlans[i];
         if( !aladin.calque.canBeRef(p) ) continue;
         if( !(p.isImage() || p instanceof PlanBG) ) { j=i; continue; }
         //System.out.println("Le plan image "+p+" va être choisi comme réf");
         aladin.calque.setPlanRef(p,currentView);
         return;
      }

      // Il n'y a plus d'images dans la pile, peut être
      // y a-t-il tout de même un catalogue qui pourrait
      // servir de référence par défaut
      if( j!=-1 ) {
         //System.out.println("Le plan "+allPlans[j]+" va être choisi comme réf");
         aladin.calque.setPlanRef(allPlans[j]);
      }

      currentView=0;
      //System.out.println("Je n'ai rien à prendre comme défaut !");
      //       aladin.calque.zoom.reset();
      aladin.calque.zoom.zoomView.repaint();
   }

   /** Sélection des vues dont le plan de référence est sélectionné.
    *  Si la vue courante n'est plus sélectionné, on resélectionnera automatiquement
    *  la première vue qui a le plan p comme référence.
    *  (sans réaffichage du select - car appelé par lui)
    *  @return true si ça a marché, sinon false
    */
   protected boolean selectViewFromStack(Plan p) {
      setSelectFromView(false);
      boolean rep=false;
      int n=0,j=-1;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = viewSimple[i];
         v.selected=false;
         if( v.isFree() ) continue;
         if( !v.pref.selected ) continue;
         rep=true;
         v.selected=true;
         n++;
         if( j==-1 && v.pref==p ) j=i;
      }

      if( /* getCurrentView().selected && */ j!=-1 ) {
         setCurrentNumView(j);
         aladin.calque.zoom.reset();
         rep=true;
      }
      return rep;
   }

   /** Spécifie la vue sous la souris (afin d'être affiché avec une bordure
    *  verte et le plan de la pile qui correspond.
    *  @param v la vue sous la souris, ou null si aucune
    */
   public void setMouseView(ViewSimple v) {
      if( v==null ) { mouseView=-1; aladin.calque.selectPlanUnderMouse(null); return; }
      mouseView=v.n;
      v.requestFocusInWindow();
      aladin.calque.selectPlanUnderMouse(v.pref);
   }

   /** Recherche de la dernière cote
   protected Cote getCutGraph() {
      Enumeration e = vselobj.elements();
      Cote c=null;
      while( e.hasMoreElements() ) {
         Objet o = (Objet)e.nextElement();
         if( o instanceof Cote ) c=(Cote)o;
      }
      return c;
   }

   /** Retourne la vue sous la souris, null si aucune */
   protected ViewSimple getMouseView() { return mouseView==-1?null:viewSimple[mouseView]; }

   /** Retourne le numéro de la vue sous la souris, -1 si aucune */
   protected  int getMouseNumView() { return mouseView; }

   /** Retourne le nombre de vues allouées */
   protected  int getNbUsedView() {
      sauvegarde();
      return viewSticked.getNbUsed()+viewMemo.getNbUsed();
   }

   /** Retourne l'indice de dernière vue */
   protected  int getLastUsedView() {
      sauvegarde();
      return viewMemo.getLastUsed();
   }


   /** Retourne true s'il y a un fond de ciel actif */
   //   protected boolean hasBackGround() {
   //      return calque.planBG!=null && calque.planBG.active;
   //   }

   /** Retourne true s'il y a au-moins une vue stickée (un sauvegarde() doit avoir été
    * opéré au préalable) */
   protected boolean hasStickedView() { return viewSticked.getNbUsed()>0; }


   protected boolean hasLockedView() {
      return true;
   }
   
   /** retourne le nombre de vues */
   protected int getNbView() {
      return aladin.viewControl.getNbView(modeView);
   }

   /** Retourne le nombre de vues sélectionnées */
   protected  int getNbSelectedView() {
      int n=0;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i]!=null && viewSimple[i].selected ) n++;
      }
      return n;
   }

   /** Positionnement de la vue courante */
   synchronized protected void setCurrentNumView(int n) {
      currentView=n;
      if( aladin.dialog!=null ) aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),4);
   }


   /** Retourne la vue courante */
   protected ViewSimple getCurrentView() {
      int m =getNbView();
      if( currentView>=m ) {
         //         System.err.println("View.getCurrentView() error: currentView ("+currentView+") > modeView ("+modeView+")");
         setCurrentNumView(m-1);
      }
      return viewSimple[currentView];
   }

   /** Retourne un tableau des vues sélectionnées */
   protected ViewSimple[] getSelectedView() {
      int n,i;
      ViewSimple v[] = new ViewSimple[getNbSelectedView()];
      int m=getNbView();
      for( n=i=0; i<m; i++ ) {
         if( viewSimple[i].selected ) v[n++]=viewSimple[i];
      }
      return v;
   }

   /** Retourne le numéro de la vue courante */
   synchronized protected int getCurrentNumView() {

      return currentView;
   }

   /** Retourne l'indice de la première vue libre visible,
    *  si aucune, retourne -1 */
   protected int getNextNumView() {
      int m = getNbView();
      for( int i=0; i<m; i++) {
         if( viewSimple[i].isFree() ) return i;
      }
      return -1;
   }

   /** Retourne l'indice de la prochaine vue à utiliser pour un nouveau plan,
    * si plus aucune de libre, retourne la dernière vue visible et indique
    * qu'il va y avoir un écrasement (afin de pouvoir générer automatiquement les
    * vues manquantes si on scrolle ou on ajoute des vues ultérieurement
    * @param p le plan pour lequel on cherche une vue libre.
    */
   protected int getLastNumView(Plan p) {
      // Dans le cas où le plan pour lequel on cherche une vue est une image
      // et que la dernière vue sélectionnée a un plan CATALOG comme ref,
      // si tout deux couvrent la même région du ciel, on va écraser la vue
      // du catalogue par celle de l'image. Il y aura donc superposition
      // automatique
      ViewSimple v = getCurrentView();

      // Si la case est vide, on la donne
      if( v!=null && v.isFree() ) return getCurrentNumView();

      // S'il s'agit d'une case contenant un catalogue compatible
      // avec la nouvelle image, on remplace
      if( v!=null && !v.isFree() && p.isImage() && v.pref.isCatalog() /* !v.pref.isImage() */
            && (!Projection.isOk(p.projd) || p.projd.agree(v.pref.projd,null)) ) return getCurrentNumView();

      // Sinon on retourne la prochaine case libre
      int n = getNextNumView();
      
      int nbViews = getNbView();

      // Si ce n'est pas possible, on prend la dernière case qui n'est pas un scatter plot
      if( n==-1 ) {
         for( int i=nbViews-1; i>=0; i--) if( !viewSimple[i].isPlotView() ) { n=i; break; }
      }

      // Si c'est pas possible, on écrase la dernière case
      if( n==-1 ) return nbViews-1;

      return n;
   }

   /** true s'il y a une vue encore libre */
   public boolean hasFreeView() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].isFree() ) return true;
      }
      return false;
   }

   /** Retourne vrai si on est en mode multivues */
   protected boolean isMultiView() { return modeView!=ViewControl.MVIEW1; }

   /** Retourne le mode courant */
   protected int getModeView() { return modeView>viewSimple.length ? viewSimple.length : modeView; }

   /** Retourne le status des vues visibles (voir command status views) */
   protected StringBuffer getStatus() {
      StringBuffer res = new StringBuffer();
      int m =getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].isFree() ) continue;
         res.append( viewSimple[i].getStatus()+"\n");
      }
      return res;
   }

   /** Retourne sous la forme d'un double la valeur du pixel courant de la vue courante,
    * NaN si impossible. Cette méthode est utilisée pour l'interface VoObserver */
   protected double getPixelValue() {
      int m=getNbView();
      if( currentView<0 || currentView>=m ) return Double.NaN;
      Plan p = viewSimple[currentView].pref;
      if( p==null ) return Double.NaN;
      if( !p.hasAvailablePixels() ) return Double.NaN;
      repere.projection(viewSimple[currentView]);
      int x = (int)Math.round(repere.xv[currentView]-1);
      int y = (int)Math.round(repere.yv[currentView]);
      //      return ((PlanImage)p).getPixelInDouble(x,y);
      return ((PlanImage)p).getPixelOriginInDouble(x,((PlanImage)p).naxis2-y-1);
   }

   /** Ajustement de la taille des panels de chaque vue en fonction de la taille
    *  disponible pour le mode m
    *  @param int m mode du view parmi [ViewControl.MVIEW1, ViewControl.MVIEW4,
    *                                   ViewControl.MVIEW9, ViewControl.MVIEW16]
    */
   protected void adjustPanel() { adjustPanel(modeView); }
   protected void adjustPanel(int m) {
      mviewPanel.removeAll();
      int lig = aladin.viewControl.getNbLig(m);
      int col = aladin.viewControl.getNbCol(m);
      mviewPanel.setLayout( new GridLayout(lig,col,0,0));
      if( m==ViewControl.MVIEW2L) m=ViewControl.MVIEW2C;
      for( int i=0; i<m; i++ ) mviewPanel.add(viewSimple[i]);
   }

   /** Changement de taille de la zone des vues, et appel aux changements
    *  de taille de chaque vue individuelle.
    */
   protected void setDimension(int w,int h) {
      switch(modeView) {
         case ViewControl.MVIEW1: getCurrentView().setDimension(w,h); break;
         default:
            int lig = aladin.viewControl.getNbLig(modeView);
            int col = aladin.viewControl.getNbCol(modeView);
            int m=getNbView();
            for( int i=0; i<m; i++ ) viewSimple[i].setDimension(w/col,h/lig);
            break;
      }
   }


   /** Positionne un curseur pour toutes les vues */
   void setMyCursor(int type) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         aladin.makeCursor(viewSimple[i], type);
      }
   }
   /** Réallocation des buffers des plans objets (catalogues et objets
    *  graphiques) afin qu'ils s'ajustent au nombre courant de vues.
    */
   private void reallocObjetCache() {
      Plan [] allPlans = calque.getPlans();
      for( int i=0; i<allPlans.length; i++ ) {
         Plan p = allPlans[i];
         p.reallocObjetCache();

         if( p instanceof PlanCatalog ) ((PlanCatalog)p).reallocFootprintCache();
      }
      newView(1);
   }

   /** Retourne le plan de référence de la vue nview */
   protected Plan getPlanRef(int nview) { return viewSimple[nview].pref; }

   /** Positionne le plan de référence de la vue nview */
   protected void setPlanRef(int nview,Plan p) {
      viewSimple[nview].setPlanRef(p,true);
      viewMemo.set(previousScrollGetValue+nview,viewSimple[nview]);
      setCurrentView(viewSimple[nview]);

      // Juste pour faire un test
      //      if( p.type==Plan.CATALOG ) viewSimple[nview].addPlotTable(null, p, 0, 1);
   }

   /** Positionne le prochain plan image dans la vue courante */
   protected void next(int sens) {
      ViewSimple vc = getCurrentView();
      Plan p = aladin.calque.nextImage(vc.pref,sens);
      if( p==vc.pref ) return;
      aladin.calque.unSelectAllPlan();
      setPlanRef(vc.n, p);
      p.selected=true;
      aladin.calque.repaintAll();
   }

   /** retourne true si le plan est utilisé comme référence pour une vue
    *  effective ou mémorisée dans viewMemo
    */
   protected boolean isUsed(Plan p) { return find(p)!=-1; }
   protected int find(Plan p) {
      int m = getNbView();
      for( int i=0; i<m/*viewSimple.length BUG*/; i++ ) {
         if( viewSimple[i]!=null && viewSimple[i].pref==p ) return i;
      }
      return viewMemo.find(p,0);
   }

   /** Génèration automatiquement d'une vue pour chaque plan image qui n'est
    *  pas encore associé à une vue. */
   protected void autoViewGenerator() {
      int n=0;
      Plan [] allPlans = calque.getPlans();
      for( int i=allPlans.length-1; i>=0; i-- ) {
         Plan p = allPlans[i];
         if( !(p.isImage() || p.type==Plan.ALLSKYIMG) || !p.flagOk ) continue;
         if( isUsed(p) ) { n++; continue; }
         createViewForPlan(p);
         n++;
      }

      // Détermination du nombre de vues visibles
      int m=ViewControl.MVIEW16;
      for( int i=0; i<ViewControl.MODE.length; i++ ) {
         if( n<ViewControl.MODE[i] ) { m=ViewControl.MODE[i]; break; }
      }
      if( m!=getModeView() ) setModeView(m);
      aladin.calque.select.repaint();
      repaintAll();
   }

   /** Suppression de toutes les vues sauf la courante
    * et repassage en mode 1 vue */
   protected void oneView() {
      if( currentView!=0 ) moveView(currentView, 0);
      for( int i=1; i<ViewControl.MAXVIEW; i++ ) viewSimple[i].free();
      sauvegarde();
      viewMemo.freeAll();
      viewSticked.freeAll();
      setModeView(ViewControl.MVIEW1);
      currentView=0;
      scrollOn(0);
      repaintAll();
   }

   /** Teste si toutes les images de la pile ont au-moins une vue */
   protected boolean allImageWithView() {
      Plan [] allPlans = calque.getPlans();
      for( int i=0; i<allPlans.length; i++ ) {
         Plan p =allPlans[i];
         if( !(p.isImage() || p.type==Plan.ALLSKYIMG) || !p.flagOk ) continue;
         if( !isUsed(p) ) return false;
      }
      return true;
   }


   /** Libération des vues sélectionnées */
   protected void freeSelected() {
      StringBuffer cmd = new StringBuffer();
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         if( !viewSimple[i].selected ) continue;
         cmd.append(" "+getIDFromNView(i));
         viewSimple[i].free();
      }
      aladin.console.printCommand("rm "+cmd);
      sauvegarde();
      viewMemo.freeSelected();
   }

   /** Libération des vues spécifiées */
   protected void free(ViewSimple [] v) {
      StringBuffer cmd = new StringBuffer();
      for( int i=0; i<v.length; i++ ) {
         cmd.append(" "+getIDFromNView(i));
         v[i].free();
      }
      aladin.console.printCommand("rm "+cmd);
      sauvegarde();
      //      viewMemo.freeSelected();
   }


   /** Retourne true s'il y a au-moins une vue lockée (même parmi les vues sauvegardées  */
   protected boolean hasLock() {
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         if( viewSimple[i].locked ) return true;
      }
      sauvegarde();
      return viewMemo.hasLock();
   }

   /** Libération des vues lockées  */
   protected void freeLock() {
      aladin.console.printCommand("rm Lock");
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         if( viewSimple[i].locked ) viewSimple[i].free();
      }
      sauvegarde();
      viewMemo.freeLock();
      scrollOn(0,0,1);
      setCurrentNumView(0);
   }

   /** Libère les vues sélectionnées qui ont comme plan de référence
    *  celui passé en paramètre.  */
   protected void free(Plan p) {
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         if( viewSimple[i].pref==p ) viewSimple[i].free();
      }
      sauvegarde();
      viewMemo.freeRef(p);
      viewSticked.freeRef(p);
   }

   /** Retourne l'indice du memoView correspondant à la vue v
    *  ou -1 si non trouvé.
    *  POUR L'INSTANT INUTILISE, JE GARDE LE CODE A TOUT HASARD
   protected int getMemoViewIndice(ViewSimple v) {
      for( int i=0; i<viewSimple.length; i++ ){
         if( viewSimple[i]==v ) return previousScrollGetValue+i;
      }
      return -1;
   }
    */

   /** Libère la memoView correspondant à une vue
    * @param i l'indice de la vue
    */
   //   private void freeMemoView(int i) {
   //      viewMemo.free(previousScrollGetValue+i);
   //   }

   /** Libération de toutes les vues */
   protected void freeAll() {
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) viewSimple[i].free();
      sauvegarde();
      viewMemo.freeAll();
      viewSticked.freeAll();
      //      resetUndo();
      scrollOn(0);
   }

   /** Retourne true si aucune vue n'est utilisée */
   protected boolean isFree() { return getNbUsedView()==0; }

   /** Retourne le numero de la vue (en fonction du mode view courant)
    * associé à un identificateur de vue (ex : B2), ou -1 si pb */
   protected int getNViewFromID(String vID ) {
      int col,lig;
      int n =aladin.viewControl.getNbLig(modeView);
      try {
         col = "ABCD".indexOf(Character.toUpperCase(vID.charAt(0)));
         lig = Integer.parseInt(vID.substring(1))-1;
         if( col<0 || lig>=n ) throw new Exception();
      } catch( Exception e ) { return -1; }

      return lig*n + col;
   }

   /** Retourne l'ID Xn d'une vue */
   protected String getIDFromNView(int n) {
      int nlig = aladin.viewControl.getNbCol(modeView);
      char c = (char)('A'+n%nlig);
      return c+""+(1+n/nlig);
   }

   /** Retourne la première vue qui utilise le plan p ou null si non trouvé */
   protected ViewSimple getView(Plan p) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         if( viewSimple[i].pref==p ) return viewSimple[i];
      }
      return null;
   }

   /** Retourne les numéros des vues qui ont comme plan de référence le plan
    *  passé en paramètre, ou null si aucun
    */
   protected int[] getNumView(Plan p) {
      int nb=0;
      int i;

      // Nombre d'éléments (pour l'allocation)
      int m=getNbView();
      for( nb=i=0; i<m; i++ ) if( viewSimple[i].pref==p ) nb++;
      if( nb==0 ) return null;
      int [] num = new int[nb];

      for( nb=i=0; i<m; i++ ) if( viewSimple[i].pref==p ) num[nb++]=i;
      return num;
   }

   protected void endTaquin() {
      flagTaquin=false;
      freeAll();
      aladin.calque.Free(taquinP);
      setModeView(ViewControl.MVIEW1);
      getCurrentView().setPlanRef(taquinRef, true);
      aladin.calque.selectPlan(taquinRef);
      taquinRef=taquinP=null;
      aladin.calque.repaintAll();
      if( record!=null ) {
         aladin.log("Taquin",record);
         aladin.warning("Taquin's done in " + record);
      }
   }

   PlanImage taquinRef,taquinP;
   String record=null;

   protected void taquinOk() {
      new Thread("TaquinOk"){
         public void run() {
            record = aladin.calque.zoom.zoomView.getTaquinTime();
            unSelectAllView();
            int m=getNbView();
            for( int i=0; i<m; i++ ) {
               int j;
               do { j = (int)(Math.random()*m); }
               while( viewSimple[j].selected );
               mouseView=j;
               paintBordure();
               Util.pause(50);
               viewSimple[j].selected=true;
            }
            unSelectAllView();
            mouseView=-1;
            for( int i=0; i<6; i++ ) {
               viewSimple[0].pref.underMouse=(i%2==0);
               paintBordure();
               Util.pause(200);
            }
            endTaquin();
         }
      }.start();
   }

   /** Fabrication d'un taquin, juste pour rire
    * @param val niveau de difficulté
    */
   protected boolean taquin(String s) {
      if( flagTaquin ) { endTaquin(); return false; }
      ViewSimple vc = getCurrentView();
      if( isMultiView() || vc.sticked || vc.hasBord() || vc.northUp ||
            vc.isFree() || !vc.pref.isImage() || !vc.pref.flagOk ) return false;
      taquinRef=(PlanImage)vc.pref;
      try {
         taquinP = (PlanImage)aladin.command.execCropCmd("","Taquin");
         vc.setPlanRef(taquinP, true);
         aladin.calque.selectPlan(taquinP);
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }

      if( taquinP.hasNoReduction() ) {
         taquinP.projd = new Projection("taquin",Projection.SIMPLE,
               0,0,15,15,taquinP.width/2,taquinP.height/2,
               taquinP.width,taquinP.height,0,false,Calib.TAN,Calib.FK5);
      }
      int niveau=2;
      try { niveau=Integer.parseInt(s); } catch( Exception e) { niveau=3; }

      int m = niveau<=1?4:niveau==2?9:16;
      double z = vc.zoom;
      double W = aladin.calque.zoom.zoomView.SIZE;
      double delta = W/Math.sqrt(m);
      double debut = delta/2.;
      double x=debut,y=debut;
      setModeView(m);
      ViewSimple v;

      int ordre[] = new int[m];
      for( int i=0; i<m; i++ ) ordre[i]=m-i-1;

      // on mélange
      int n = (int)(Math.random()*1000)+100;
      int w = (int)Math.sqrt(m);
      int pos,npos;
      npos = pos=0;
      int sens,osens=-1;
      for( int i=0; i<n; i++ ) {
         while( (sens = (int)(Math.random()*4))==osens);
         switch(sens) {
            case 0 : if( pos>w ) { npos=pos-w; break; }
            case 3 : if( pos<m-w ) { npos=pos+w; break; }
            case 1 : if( pos%w!=0 ) { npos=pos-1; break; }
            case 2 : if( (pos+1)%w!=0 ) { npos=pos+1; break; }
         }
         osens = sens==0 ? 3 : sens==3 ? 0 : sens==1 ? 2 : 1;
         int t = ordre[npos]; ordre[npos]=ordre[pos]; ordre[pos]=t;
         pos=npos;
      }


      for( int i=0; i<m; i++, x+=delta ) {
         int rang=0;
         while( i!=ordre[rang] ) rang++;
         v=viewSimple[rang];
         if( x>W ) { x=debut; y+=delta; }
         vc.copyIn(v);
         v.setZoomXY(z,x,y);
         v.ordreTaquin=i;
         v.locked=true;
         if( i==m-1 ) v.free();
      }

      sauvegarde();
      calque.flagOverlay=false;
      flagTaquin=true;
      aladin.calque.zoom.zoomView.startTaquinTime=0L;
      record=null;
      startTimer();
      repaintAll();
      aladin.status.setText("*** For Your Pleasure - offered by the Aladin Team **** ");
      return true;
   }

   public boolean isOpaque() { return false; }

   /** Il y a une recalibration par Quadruplet en cours */
   public boolean isRecalibrating() {
      return aladin.frameNewCalib!=null
            && aladin.frameNewCalib.isShowing()
            && aladin.frameNewCalib.getModeCalib()==FrameNewCalib.QUADRUPLET
            ;
   }

   protected boolean syncSimple(Source o) {
      Coord c = new Coord(o.raj,o.dej);
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isPlotView() ) {
         double [] val = v.plot.getValues(o);
         c.al = val[0];
         c.del = val[1];
      }
      boolean rep=true;
      if( !v.isInView(c.al,c.del) ) rep = v.setZoomRaDec(0,c.al,c.del);
      if( rep ) {
         repaintAll();
         aladin.calque.zoom.newZoom();
         aladin.calque.zoom.zoomView.repaint();
      }
      //       memoUndo(v,c,o);
      moveRepere(c);
      showSource(o,false,false);
      resetBlinkSource();
      return rep;
   }

   protected void resetBlinkSource() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].resetBlinkSource();
   }

   /** Synchronisation sur le plan pour la vue courante
    *  @param p le plan à montrer
    */
   protected boolean  syncPlan(Plan p) {
      if( !Projection.isOk(p.projd) ) return false;
      //        return gotoThere(p.co,0,true);
      Coord c = null;
      if( p instanceof PlanBG ) c = p.co;
      else if( p instanceof PlanField ) c = ((PlanField)p).getCenter();
      else c = p.projd.getProjCenter();
      return gotoThere(Projection.isOk(p.projd)?c:p.co,0,true);
   }

   /** Va montrer la position repéree par son identificateur ou sa coordonnée J2000
    * @param target Identificateur valide, ou coordonnées J2000
    * @return true si ok
    */
   public  boolean gotoThere(String target) {
      try {
         Coord c1 = getCurrentView().getCooCentre();
         Coord c;
         if( !View.notCoord(target) ) c = new Coord(target);
         else c = sesame(target);
         gotoAnimation(c1, c);
      } catch( Exception e ) { }
      return false;
   }

   /** Va montrer la source
    * @param s la source à montrer
    * @return true si ok
    */
   public boolean gotoThere(Source s) {
      boolean rep = gotoThere(new Coord(s.raj,s.dej),0,false);
      setRepere(s);  // pour d'éventuels plots
      showSource(s,false,false); // pour blinker la source   CA FAIT TOUT DE MEME UN CALLBACK EN BOULCE AVEC SAMP
      return rep;
   }

   /** Va montrer la coordonnée indiquée.
    * @param c la coordonnée en J2000
    * @param zoom le facteur de zoom souhaitée, 0 si on ne le modifie pas
    * @return true si ok
    */
   public boolean gotoThere(Coord c) { return gotoThere(c,0,false); }
   public boolean gotoThere(Coord c, double zoom, boolean force) {

      ViewSimple v = getCurrentView();
      if( v.locked || c==null ) return false;
      setRepere(c);
      if( !force && !v.shouldMove(c.al,c.del) ) return false;

      if( v.pref instanceof PlanBG ) {
         v.getProj().setProjCenter(c.al,c.del);
         v.newView(1);
      }

      double z = zoom<=0 ? v.zoom : aladin.calque.zoom.getNearestZoomFct(zoom);
      v.setZoomRaDec(z,c.al,c.del);

      showSource();  // On force le réaffichage de la source blink en cas de vues bougées

      aladin.calque.zoom.newZoom();
      aladin.view.zoomview.repaint();
      return true;
   }

   public void gotoAnimation(final Coord from, final Coord to) {
      final ViewSimple v = getCurrentView();
      if( v.locked || to==null ) return;
      final double zoom = v.zoom;
      double z = v.zoom;

      double dist = Coord.getDist(from, to);
      int n=(int) (dist);
      int mode=0;
      int i=0;
      boolean encore=true;
      double fct=0;
      while( encore ) {

         switch(mode) {
            case 0:
               if( z<0.1 ) i++;
               if( z>0.05 ) z=z/1.05;
               else mode=1;
               break;
            case 1:
               i++;
               if( i>=n-3 ) mode=2;
               break;
            case 2:
               if( z<0.1 && i<n ) i++;
               if( z<zoom ) z=z*1.05;
               if( z>=zoom ) {z=zoom; encore=false; }
               break;

         }
         fct = i/(double)n;
         Coord c = new Coord( from.al + (to.al-from.al)*fct,
               from.del + (to.del-from.del)*fct);

         int frameNumber = v.frameNumber;
         gotoThere(c,z,true);
         while( frameNumber==v.frameNumber ) Util.pause(10);
         //                System.out.println("gotoAnimation(...) i="+i+" z="+z+" c="+c);
         //                Util.pause(75);
      }
      gotoThere(to,zoom,true);
   }
   //     }

   /** Ajustement de toutes les vues (non ROI )
    *  afin que leur centre corresponde à la coordonnée
    *  passée en paramètre ( et que leur zoom s'accroisse du
    *  facteur indiquée)
    *  @param fct facteur multiplicatif (ou 1 si inchangé)
    *  @param coo nouvelle position, (null si aligné sur repere)
    *  @param force : true => force le déplacement même pour les PlanBG et autres cas spéciaux
    */
   protected void syncView(double fct,Coord coo,ViewSimple vOrig) { syncView(fct,coo,vOrig,false); }
   protected void syncView(double fct,Coord coo,ViewSimple vOrig,boolean force) {
      ViewSimple v;
      Coord c = coo!=null ? coo : new Coord(repere.raj,repere.dej);

      // Ajustement des zooms des autres vues en fonction
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
         v = viewSimple[i];
         if( v.locked || v.isFree() )    continue;
         if( !force ) {
            if( v==vOrig && !(v.pref instanceof PlanBG) ) continue;
            if( !v.shouldMove(c.al,c.del) ) continue;
         }

         if( v.pref instanceof PlanBG ) {
            v.getProj().setProjCenter(c.al,c.del);
            v.newView(1);
         }

         double z = aladin.calque.zoom.getNearestZoomFct(v.zoom * fct);
         v.setZoomRaDec(z,c.al,c.del);
      }

      // Réaffichages nécessaires
      if( coo==null ) repaintAll();
      else setRepere(coo);

      showSource();  // On force le réaffichage de la source blink en cas de vues bougées

      aladin.calque.zoom.newZoom();
      aladin.view.zoomview.repaint();
   }

   /** Mémorise la valeur du pixel courante dans chaque ViewSimple pour la coordonnée indiquée
    * afin 'être affichée en surimpression de l'image */
   protected void setPixelInfo(Coord coo) {
      String s;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = viewSimple[i];
         if( v.isFree() || !v.pref.hasAvailablePixels() || v.isPlotView() ) continue;
         s=null;
         Projection proj = v.pref.projd;
         if( Projection.isOk(proj) && coo!=null ) {
            proj.getXY(coo);
            if( !Double.isNaN(coo.x) /* && v.isInView(coo.al, coo.del) */) {
               PointD p = new PointD(coo.x,coo.y);
               if( v.pref instanceof PlanBG ) s = ((PlanBG)v.pref).getPixelInfo(p.x, p.y,getPixelMode());
               else s = ((PlanImage)v.pref).getPixelInfo( (int)Math.floor(p.x), (int)Math.floor(p.y),getPixelMode());
               if( s==PlanImage.UNK ) s="";
            }
         }
         v.setPixelInfo(s);
      }
   }

   /** Retourne le mode courant d'affichage de la valeur du pixel */
   protected int getPixelMode() { return REAL; }

   /** Flip d'un plan et recentrage de toutes les vues utilisant ce plan en tant que
    * plan de référence afin d'afficher toujours les mêmes régions.
    * Le recentrage ne fonctionne que s'il y a calibration */
   protected void flip(PlanImage p,int methode) throws Exception {
      Coord centre[] =null;

      if( p.type==Plan.IMAGECUBE || p.type==Plan.ALLSKYIMG ) throw new Exception("Flip not available for this kind of plane");

      //      if( !Projection.isOk(p.projd) ) {
      //         aladin.warning("Cannot flip uncalibrated image !");
      //         return;
      //      }

      // Je mémorise le centre de chaque vue dont le plan de ref est "p"
      if( Projection.isOk(p.projd) ) {
         centre = new Coord[ViewControl.MAXVIEW];
         for( int i=0; i<ViewControl.MAXVIEW; i++ ) {
            centre[i]=null;
            ViewSimple v = viewSimple[i];
            if( v.locked || v.pref!=p ) continue;
            centre[i] = v.getCooCentre();
         }
      }

      // J'effectue le flip
      p.flip(methode);

      // Je recentre les vues concernées
      if( centre!=null ) {
         for( int i=0; i<centre.length; i++ ) {
            if( centre[i]!=null ) viewSimple[i].setCenter(centre[i].al,centre[i].del);
         }
      }

      newView(1);
      repaintAll();
   }

   /* Les méthodes setZoomRaDecForSelectedViews permettent de modifier la valeur du zoom
    *  ainsi que la position du centre de la portion visible pour toutes
    *  les vues sélectionnées (bordures bleu)
    *  @param z la nouvelle valeur de zoom (0 si inchangé)
    *  @param coo le nouveau centre (null si inchangé)
    *  @param v la vue par rapport à laquelle on va calculer la taille du pixel
    *           si null, on utilisera la vue courante.
    */
   protected void setZoomRaDecForSelectedViews(double z,Coord coo) {
      setZoomRaDecForSelectedViews(z,coo,null,true,false);
   }
   protected void setZoomRaDecForSelectedViews(double z,Coord coo, ViewSimple vc,boolean zoomRepaint,boolean flagNow) {
      double size=-1;
      double cSize;
      double nz;
      //
      //      System.out.println("setZoom("+coo+","+z+","+zoomRepaint+")");
      //      if( coo!=null ) {
      //         try {
      //            throw new Exception("ici");
      //         } catch( Exception e) {
      //            e.printStackTrace();
      //         }
      //      }

      suspendQuickSimbad();

      boolean flagMoveRepere=coo!=null;

      if( vc==null ) vc = getCurrentView();

      Projection proj = vc.pref.projd;

      // Récupération de la taille du pixel de la vue courante afin de déterminer
      // le rapport sur le zoom pour les autres vues
      try {
         size = proj.c.getImgWidth() / proj.c.getImgSize().width;
         if( vc.pref.type==Plan.IMAGEHUGE ) size *= ((PlanImageHuge)vc.pref).getStep();
      } catch( Exception e) {  };

      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = viewSimple[i];
         if( /* v.locked || */ !v.selected && v!=vc  || v.isPlotView()!=vc.isPlotView() ) continue;


         // Calcul du facteur de zoom pour les vues en fonction de la taille
         // du pixel
         if( size>0 && z!=0.) {
            try {
               cSize = proj.c.getImgWidth() / proj.c.getImgSize().width;
               if( v.pref.type==Plan.IMAGEHUGE ) cSize *= ((PlanImageHuge)v.pref).getStep();
               nz = aladin.calque.zoom.getNearestZoomFct(z/(size/cSize));
            } catch( Exception e ) { nz=z; if( aladin.levelTrace>=3) e.printStackTrace(); }
         } else nz=z;

         // En cas de recalibration on va éviter de déplacer la vue intempestivement
         // dans le cas où il y a déjà une mauvaise calibration
         // Et pour un plot, on n'a pas de repere
         if( isRecalibrating() && aladin.toolBox.getTool()==ToolBox.SELECT
               || v.isPlotView() || v.locked ) v.setZoomXY(nz,v.xzoomView,v.yzoomView);

         // Mode courant => Déplacement soit sur la coordonnée indiquée, soit sur le repere
         else {

            // Déplacement soit sur le repère, soit sur la coordonnée passée en paramètre
            boolean flag;

            //            if( coo==null ) coo = new Coord(repere.raj,repere.dej);
            // Sauf mention contraire, on va désomais pointer à la mi-distance entre la position centrale et le repere
            // pour faire un effet de glissement
            if( coo==null ) {
               coo = new Coord(repere.raj,repere.dej);
               if( !flagNow && v.pref instanceof PlanBG ) {
                  try {
                     coo = v.getProj().getXY(coo);
                     Coord c1 = v.getCooCentre();
                     coo = new Coord(c1.al+(coo.al-c1.al)/3,c1.del+(coo.del-c1.del)/3);

                     // Si ce point est en dehors de la vue, on va directement au repere
                     v.getProj().getXY(coo);
                     PointD p = v.getPositionInView(coo.x, coo.y);
                     if( p.x<0 || p.x>v.rv.width || p.y<0 || p.y>v.rv.height ) coo = new Coord(repere.raj,repere.dej);

                  } catch( Exception e ) { }
               }
            }
            if( v.pref instanceof PlanBG ) {
               v.getProj().setProjCenter(coo.al,coo.del);
               v.pref.projd.setProjCenter(coo.al,coo.del);
               v.newView(1);
            }
            flag=v.setZoomRaDec(nz,coo.al,coo.del);

            // Si impossible d'atteindre la position demandée,
            // on effectue un simple zoom si pas de calibration, sinon on déselectionne
            // la vue
            if( !flag && nz!=v.zoom && v.pref!=null ) {
               if( !Projection.isOk(proj) || v.isPlotView() || proj.isXYLinear() ) v.setZoomXY(nz,v.xzoomView,v.yzoomView);
               else v.selected=false;   // on déselectionne
            }
         }

         v.repaint();
      }
      if( crop!=null && crop.isVisible() ) coo=null;  // pour éviter le déplacement du repère

      // Réaffichages nécessaires
      if( flagMoveRepere ) moveRepere(coo);
      else repaintAll();

      aladin.dialog.adjustParameters();

      if( zoomRepaint ) {
         aladin.calque.zoom.newZoom();
         aladin.calque.repaintAll();
      }

   }

   /** Force le recalcul de la grille de coordonnées */
   protected void grilAgain() {
      if( !aladin.calque.hasGrid() ) return;
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].oiz=-2;
   }

   /** Demande de rafraichissement des buffers de calculs de position
    *  @param mode 0 (defaut) juste pour les images
    *              1 également pour les overlays
    */
   protected void newView() { newView(1); }
   protected void newView(int mode) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].newView(mode);
   }

   /** Desactivation du mode Dynamic Color Map de la vue courante */
   //   protected void endDynamicCM() { getCurrentView().endDynamicCM(); }

   /** Activation du mode Dynamic Color Map de la vue courante*/
   //   protected void dynamicCM(Object cm) { getCurrentView().dynamicCM(cm); }

   //   private boolean flagLockRepaint=false;
   //
   //   // Positionnement du flag du lockRepaint (pour le mode script)
   //   protected synchronized void setLockRepaint(boolean flag) {
   //      flagLockRepaint=flag;
   //   }
   //
   //   // Positionnement du flag du lockRepaint (pour le mode script)
   //   private synchronized boolean lockRepaint() {
   //      return flagLockRepaint;
   //   }

   /** Indique s'il y a des objets selectionnes.
    * @return <I>true</I> s'il y a des objets selectionnes, <I>false</I> sinon
    */
   protected boolean hasSelectedObj() {
      return( vselobj.size()>0 );
   }

   /** Retourne le nombre d'objets sélectionnés */
   protected int nbSelectedObjet() { return vselobj.size(); }
   
   /** retourne true si le premier objet sélectionné peut être utilisé pour extraire un MOC
    * (càd soit un polygone, soit un cercle) */
   protected boolean hasMocPolSelected() {
      if( !hasSelectedObj() ) return false;
      Obj o = vselobj.get(0);
      if( o instanceof Ligne && ((Ligne)o).isPolygone() ) return true;
      
      // POUR LE MOMENT, ON NE PREND PAS EN COMPTE LE CERCLE
      if( o instanceof Repere && ((Repere)o).hasRayon() )  return true;
      
      return false;
   }

   /** Retourne true s'il y a une et une seule Cote sélectionnée parmi les objets */
   protected boolean hasOneCoteSelected() {
      int n=0;
      Enumeration<Obj> e = vselobj.elements();
      while( e.hasMoreElements() ) {
         Obj o = e.nextElement();
         if( !(o instanceof Cote) ) continue;
         n++;
         if( n>2 ) return false;
      }
      return n==2;
   }


   /** Indique s'il y a au- moins une source sélectionnée */
   protected boolean hasSelectedSource() {
      Enumeration<Obj> e = vselobj.elements();
      while( e.hasMoreElements() ) {
         if( e.nextElement() instanceof Source ) return true;
      }
      return false;
   }

   /** Indique s'il y a au- moins un objet déplaçable */
   protected boolean hasMovableObj() { return hasMovableObj(vselobj); }
   protected boolean hasMovableObj(Vector v) {
      Enumeration<Obj> e = v.elements();
      while( e.hasMoreElements() ) {
         if( !(e.nextElement() instanceof Source) ) return true;
      }
      return false;
   }


   /** Retourne le Vector des objets sélectionnés */
   protected Vector<Obj> getSelectedObjet() { return vselobj; }

   /** Retourne le tableau des Source sélectionnées (uniquement les Source) */
   protected Source[] getSelectedSources() {
      if( vselobj==null ) return new Source[]{};
      Vector<Source> vSources = new Vector<Source>();
      Enumeration<Obj> eObj = vselobj.elements();
      Obj o;
      while( eObj.hasMoreElements() ) {
         o = eObj.nextElement();
         if( o instanceof Source ) vSources.addElement( (Source) o);
      }
      Source[] sources = new Source[vSources.size()];
      vSources.copyInto(sources);
      vSources = null;

      return sources;
   }

   private Propable getLastPropableObj() {
      Obj rep=null;
      Enumeration<Obj> e = vselobj.elements();
      while( e.hasMoreElements() ) {
         Obj obj = e.nextElement();
         if( obj.hasProp() ) rep=obj;
      }
      return rep;
   }

   /** Indique s'il y a au-moins à objet sélectionné susceptible d'avoir des propriétés */
   protected boolean isPropObjet() { return selectFromView && getLastPropableObj()!=null; }

   protected void propResume() {
      if( aladin.frameProp==null ) return;
      aladin.frameProp.resume();
   }
   protected void propSelectedObj() {
      Propable obj = getLastPropableObj();
      if( obj==null ) return;
      if( aladin.frameProp==null ) aladin.frameProp = new FrameProp(aladin,obj);
      else aladin.frameProp.updateAndShow(obj);
   }

   /** Indique s'il y a au-moins une suppression effective a faire
    * @return <I>true</I> s'il y a des objets a supprimer, <I>false</I> sinon
    */
   protected boolean isDelSelObjet() {
      if( !selectFromView ) return false;
      Enumeration<Obj> e = vselobj.elements();

      while( e.hasMoreElements() ) {
         Position p = (Position) e.nextElement();
         if( p.plan.type==Plan.TOOL || p.plan.isSourceRemovable() ) return true;        // Appartient a un plan Tool
      }
      return false;
   }

   /** Recopie les id des objets selectionnes dans le bloc note */
   protected void selObjToPad() {
      Enumeration<Obj> e = vselobj.elements();
      StringBuffer res = new StringBuffer("\n");

      while( e.hasMoreElements() ) {
         Position o = (Position) e.nextElement();
         if( o instanceof Source ) continue;
         if( o.id!=null && o.id.length()>0 ) res.append(o.id+"\n");
      }
      aladin.console.printInPad(res.toString());
   }

   /** Extension des clips de chaque vue pour contenir l'objet o */
   protected void extendClip(Obj o) {
      //      for( int i=0; i<modeView; i++ ) viewSimple[i].extendClip(o);
   }

   /** Supprime les objets selectionnes.
    * Effectue un reaffichage si necessaire
    * Dans le cas ou il y aurait un objet Cote, il faut demander l'arret
    * de l'affichage de la coupe dans le zoomView.
    * @return <I>true</I> s'il y en a eu au moins une suppression,
    *         <I>false</I> sinon
    */
   protected boolean delSelObjet() {

      Enumeration<Obj> e = vselobj.elements();

      // décompte des tags
      int tags = 0;
      for( Obj o : vselobj ) { if( o instanceof Source && o.plan!=null && o.plan instanceof PlanTool ) tags++; }

      e = vselobj.elements();

      boolean ok = !vselobj.isEmpty();
      boolean flagCote=false;

      while( e.hasMoreElements() ) {
         Obj o = e.nextElement();
         if( o instanceof Cote ) flagCote=true;
         extendClip(o);

         // On ne supprime pas s'il y a plus de 1 tags sélectionnés
         if( tags>1 && o instanceof Source && o.plan!=null && o.plan instanceof PlanTool )  continue;

         if( o instanceof Source && ((Source)o).plan.isSourceRemovable() ) {
            aladin.mesure.remove((Source)o);
         }

         calque.delObjet(o);
      }
      vselobj.removeAllElements();
      if( flagCote ) aladin.calque.zoom.zoomView.setCut(null);
      if( tags>1 ) aladin.warning(aladin.chaine.getString("REMOVETAG"));
      if( ok ) repaintAll();
      return ok;
   }

   /** Détermine si le menu qui permet d'afficher/cacher les labels des
    * objets sélectionnés doit être mis à "Afficher" ou "Cacher".
    * Il faut que tous les objets aient le label pour qu'ils soient
    * supprimes, sinon ils sont ajoutes.
    */
   protected boolean labelOn() {
      Enumeration<Obj> e = vselobj.elements();
      while( e.hasMoreElements() ) {
         Obj o = e.nextElement();
         if( !(o instanceof Source || o instanceof Repere) ) continue;
         if( !((Position)o).isWithLabel() ) return true;
      }
      return false;
   }

   /** Affichage de l'identificateur des objets selectionnes.
    * Positionne ou non le label (identificateur) des objets selectionnes
    * Il faut que tous les objets aient le label pour qu'ils soient
    * supprimes, sinon ils sont ajoutes.
    */
   protected void setSourceLabel() {
      int i/*,j,k*/;
      boolean enleve=false;  // true s'il faut enlever les labels, false sinon

      //      // Label sur les plans courants
      //      if( vselobj.size()==0 ) {
      //
      //         // deux tours : 1 pour le pre-traitement et l'autre pour le traitement
      //         for( k=0; k<2; k++ ) {
      //            encore:
      //            for( i=0; i<calque.plan.length; i++ ) {
      //               if( calque.plan[i].type!=Plan.CATALOG
      //                  && calque.plan[i].type!=Plan.TOOL
      //                  || !calque.plan[i].selected ) continue;
      //               PlanObjet pcat = calque.plan[i].pcat;
      //
      //               for( j=0; j<pcat.o.length; j++ ) {
      //                  Position o = (Position) pcat.o[j];
      //                  if( !(o instanceof Source || o instanceof Repere) ) continue;
      //
      //                  // Preparation du traitement
      //                  if( k==0 ) {
      //                     if( o.withlabel ) {
      //                        enleve=true;
      //                        break encore;
      //                     }
      //
      //                 // Traitement
      //                 } else o.withlabel=!enleve;
      //               }
      //            }
      //         }
      //
      //         repaintAll();
      //         return;
      //      }


      // label sur les objets selectionnees
      enleve=true;
      for( i=vselobj.size()-1; i>=0; i-- ) {
         Position o = (Position) vselobj.elementAt(i);
         if( !(o instanceof Source || o instanceof Repere) ) continue;
         if( !o.isWithLabel() ) enleve=false;
         o.setWithLabel(true);                // Par defaut on met le label
      }

      // Si jamais il fallait au contraire enlever les labels
      if( enleve ) {
         for( i=vselobj.size()-1; i>=0; i-- ) {
            Position o = (Position) vselobj.elementAt(i);
            if( !(o instanceof Source || o instanceof Repere) ) continue;
            o.setWithLabel(false);
         }
      }

      repaintAll();
   }

   /** Deselection des objets par rapport au vecteur vselobj.
    * Deselectionne tous les objets, demande la mise a jour
    * de la fenetre des infos
    * @return Retourne <I>true</I> si au-moins un objet a ete deselectionne
    */
   protected boolean deSelect() {
      if( vselobj.isEmpty() ) return false;
      Enumeration<Obj> e = vselobj.elements();

      while( e.hasMoreElements() ) {
         Obj o = e.nextElement();
         extendClip(o);
         o.setSelect(false);
      }
      vselobj.removeAllElements();
      aladin.mesure.removeAllElements();

      return true;
   }

   /** Déselection de tous les objets du plan p et réaffichage complet */
   protected void deSelect(Plan p) {
      if( p.isCatalog() ) aladin.mesure.rmPlanSrc(p);
      if( vselobj.isEmpty() ) return;
      synchronized(this) {
         int n=0,i,m=vselobj.size();
         for( i=0; i<m; i++ ){
            Obj o = vselobj.elementAt(i);

            if( o instanceof Position && ((Position)o).plan==p ) {
               o.setSelect(false);
               continue;
            }
            if( i!=n ) vselobj.setElementAt(o,n);
            n++;
         }
         if( n==m ) return;

         vselobj.setSize(n);
         //       System.out.println("J'ai déselectionné "+(m-n)+" objets");
      }
      resetClip();
      repaintAll();
   }

   /** Ajoute les sources taggées dans la sélection et dans la fenêtre des mesures */
   protected void addTaggedSource(Plan p) {
      if( p.getCounts()==0 ) return;
      Iterator<Obj> it = p.iterator();
      boolean trouve=false;
      synchronized(this) {
         while( it.hasNext() ) {
            Obj s = it.next();
            if( !(s instanceof Source) || !((Source)s).isTagged() || ((Source)s).isSelected() ) continue;
            ((Source)s).setSelect(true);
            vselobj.add(s);
            aladin.mesure.insertInfo((Source)s);
            trouve=true;
         }
      }
      if( trouve ) {
         aladin.mesure.adjustScroll();
         aladin.mesure.mcanvas.repaint();
      }
   }

   /** Ajoute toutes les sources SED du plan passé en paramètre dans la fenêtre des mesures
    * si ce n'est déjà fait
    * @return la première source SED trouvée, sinon null
    */
   protected Source addSEDSource(Plan p) {
      if( p.getCounts()==0 ) return null;
      Vector<Source> v = new Vector<Source>();
      Iterator<Obj> it = p.iterator();
      while( it.hasNext() ) {
         Obj s = it.next();
         if( !(s instanceof Source) || !((Source)s).leg.isSED() ) continue;
         //         if( ((Source)s).isSelected() ) return null;
         v.add( (Source) s);
      }
      if( v.size()==0 ) return null;

      synchronized(this) {
         for( Source s1 : v ) {
            s1.setSelect(true);
            vselobj.add(s1);
            aladin.mesure.insertInfo(s1);
         }
      }
      aladin.mesure.adjustScroll();
      aladin.mesure.mcanvas.repaint();
      return v.elementAt(0);
   }

   /** Deselection d'une source du vecteur vselobj.
    * Demande la mise a jour de la fenetre des infos
    * @return Retourne true si ok
    */
   protected boolean deSelect(Source src) {
      if( vselobj.isEmpty() ) return false;
      Enumeration<Obj> e = vselobj.elements();
      int i;
      boolean trouve=false;

      for( i=0; e.hasMoreElements(); i++ ) {
         Obj o = e.nextElement();
         if( o==src ) {
            extendClip(o);
            src.setSelect(false);
            hideSource();
            trouve=true;
            break;
         }
      }
      if( !trouve ) return false;
      vselobj.removeElementAt(i);
      aladin.mesure.remove(src);
      repaintAll();
      return true;
   }

   /** Ajustement de la selection des sources du vecteur vselobj.
    * en fonction du tableau des mesures (suites à une déselection des objets
    * tagués/non-tagués
    * On va simplement parcourir tous les objets sélectionnés dans vselobj
    * et supprimer ceux qui n'ont pas leur flag SELECT positionné
    */
   protected void majSelect() {
      int n=0,i,m=vselobj.size();
      for( i=0; i<m; i++ ){
         Obj o = vselobj.elementAt(i);

         if( o instanceof Source && !((Source)o).isSelected() ) continue;
         if( i!=n ) vselobj.setElementAt(o,n);
         n++;
      }
      if( n==m ) return;

      vselobj.setSize(n);
      //System.out.println("J'ai déselectionné "+(m-n)+" objets");
      resetClip();
      repaintAll();
   }

   /** Reset de tous les clips des vues */
   protected void resetClip() {
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].resetClip();
   }

   /** Sélection ou désélection d'un objet (Source ou simple Position) sans réaffichage
    * (Utilisé par les Plugins)
    * @param o l'objet
    * @param mode le flag de sélection ou non
    */
   protected void setSelected(Obj o,boolean mode) {
      boolean oMode = o.isSelected();
      if( oMode == mode ) return;
      if( !mode ) {
         vselobj.remove(o);
         if( o instanceof Source ) aladin.mesure.remove((Source)o);
      } else {
         if( !vselobj.contains(o) ) {
            vselobj.addElement(o);
            if( o instanceof Source ) aladin.mesure.insertInfo((Source)o);
         }
      }
      o.setSelect(mode);
   }

   /** highlight ou non d'un objet (Source uniquement)
    * (Utilisé par les plugins)
    * @param src la source
    * @param flag true pour blinker, false sinon
    */
   protected void setHighlighted(Source src, boolean flag) {
      if( !flag ) showSource();
      else showSource(src, false, false);
   }

   /** Selection d'un unique objet
    * @param Obj o
    */
   //   protected void selectOne(Obj o) {
   //      deSelect();
   //      o.setSelect(true);
   //      resetClip();
   //      vselobj.addElement(o);
   //      aladin.mesure.insertInfo((Source)o);
   //      repaintAll();
   //      aladin.toolbox.toolMode();
   //      aladin.mesure.mcanvas.repaint();
   //   }

   /** Selection d'une Cote dont on indique un des bouts */
   protected void selectCote(Obj c1) {
      c1.setSelect(true);
      if( !vselobj.contains(c1) ) vselobj.addElement(c1);
      if( c1 instanceof Cote ) {
         Ligne c2 = ((Cote)c1).debligne!=null?((Cote)c1).debligne:((Cote)c1).finligne;
         c2.setSelect(true);
         if( !vselobj.contains(c2) ) vselobj.addElement(c2);
      }
   }

   /** Selection d'une liste de Source par leur OID
    *  @param oid[]
    */
   protected void selectSourcesByOID(String oid[]) {
      deSelect();
      resetClip();
      Source o[] = aladin.calque.getSources(oid);
      for( int i=0; i<o.length; i++ ) {
         ((Obj)o[i]).setSelect(true);
         if( !o[i].plan.active ) o[i].plan.setActivated(true);
         vselobj.addElement(o[i]);
         if( i==o.length-1 ) aladin.mesure.setInfo(o[i]);
         else aladin.mesure.insertInfo(o[i]);
      }
      aladin.calque.repaintAll();
      aladin.mesure.mcanvas.repaint();
      repaint();
   }

   /** Selection d'une liste de Source par leur numéro d'ordre (pour PLASTIC)
    *  @param oid[]
    */
   protected void selectSourcesByRowNumber(PlanCatalog pc, int[] rowIdx) {
      deSelect();
      resetClip();
      Source curObj;
      Obj [] o = pc.pcat.getObj();
      for( int i=0; i<rowIdx.length; i++ ) {
         curObj = (Source)o[rowIdx[i]];
         ((Obj)curObj).setSelect(true);
         if( !curObj.plan.active ) curObj.plan.setActivated(true);
         vselobj.addElement(curObj);
         if( i==rowIdx.length-1 ) aladin.mesure.setInfo(curObj);
         else aladin.mesure.insertInfo(curObj);
      }
      aladin.calque.repaintAll();
      aladin.mesure.mcanvas.repaint();
      repaint();
   }

   /** Deselection de tous les objets d'un plan.
    * Puis selectionne tous les objets d'un plan demande la mise a jour
    * de la fenetre des infos et retourne Vrai si au-moins un objet
    * a ete deselectionne
    * @param plan Le plan pour lequel il faut deselectionner les objets
    * @return Retourne <I>true</I> si au-moins un objet a ete deselectionne
    */
   protected boolean selectAllInPlan(Plan plan) {

      // Deselection des precedents
      Enumeration<Obj> e = vselobj.elements();
      while( e.hasMoreElements() ) {
         Position p = (Position) e.nextElement();
         p.setSelect(false);
      }
      aladin.mesure.removeAllElements();
      vselobj.removeAllElements();

      // Reselection
      selectAllInPlanWithoutFree(plan,0);

      // et affichage
      repaintAll();
      aladin.toolBox.toolMode();
      aladin.mesure.mcanvas.repaint();
      return true;
   }

   /** Selection de tous les objets d'un plan CATALOG ou TOOLS
    * @param mode 0-tous les objets, 1-seuls les objets taggués (uniquement pour les sources)
    */
   protected void selectAllInPlanWithoutFree(Plan plan,int mode) {
      boolean flagSource=false;
      Iterator<Obj> it = plan.iterator();
      if( plan.isCatalog() ) {
         while( it.hasNext() ) {
            Obj o1 = it.next();
            if( o1 instanceof Source ) {
               Source o = (Source)o1;
               if( mode==1 && !o.isTagged() ) continue;
               if( !o.noFilterInfluence() && !o.isSelectedInFilter() ) continue;
               aladin.mesure.insertInfo(o);
               flagSource=true;
            } else if( mode==1 ) continue;
            o1.setSelect(true);
            vselobj.addElement(o1);
         }
         if( flagSource ) aladin.mesure.adjustScroll();
      }
   }

   static final int STR  = 0;
   static final int EGAL = 1;
   static final int DIFF = 2;
   static final int SUP  = 3;
   static final int INF  = 4;
   static final int SUPEG= 5;
   static final int INFEG= 6;
   static final int EGALL= -1;

   /**
    * Découpe une chaine de la forme NOMCOL = VALEUR dans les variables appropriées.
    * S'il n'y a pas de sous-chaine =,!=,>,>=,<,<=  toute la chaine est considérée,
    * comme valeur
    * @param colName Retourne le nom de la colonne si elle existe (trimée)
    * @param val Retourne la valeur de la recherche (trimée)
    * @param s Chaine initiale
    * @return type de recherche STR,EGAL,DIFF,SUP,INF,SUPEG ou INFEG
    */
   protected int getAdvancedSearch(StringBuffer colName, StringBuffer val, String s) {
      int i;
      int mode=STR;
      int n=s.length();

      // Teste l'égalité ou la différence
      i = s.indexOf('=');
      if( i>0 ) {
         if( s.charAt(i-1)=='!' ) { i--; mode=DIFF; }
         else if( s.charAt(i-1)=='>' ) { i--; mode=SUPEG; }
         else if( s.charAt(i-1)=='<' ) { i--; mode=INFEG; }
         else if( i<n-1 && s.charAt(i+1)=='=' ) mode=EGALL;
         else mode=EGAL;
      }

      // teste supérieur, supérieur ou égal
      if( mode==STR ) i=s.indexOf('>');
      if( mode==STR && i>0 && i<n-1 ) mode = SUP;

      // teste inférieur, inférieur ou égal
      if( mode==STR && i==-1 ) i=s.indexOf('<');
      if( mode==STR && i>0 && i<n-1 ) mode = INF;

      // Simple mode chaine
      if( mode==STR ) {
         val.append(s);
         return STR;
      }

      // Extraction nom de colonne et valeur
      colName.append( s.substring(0,i).trim() );
      n=colName.length();
      if( n>3 && colName.charAt(0)=='$'
            && colName.charAt(1)=='{' && colName.charAt(n-1)=='}' ) {
         colName.delete(n-1,n);
         colName.delete(0,2);
      }
      val.append( Tok.unQuote(s.substring(i+((mode==EGAL || mode==SUP || mode==INF)?1:2))).trim() );
      if( mode==EGALL ) mode=EGAL;

      return mode;
   }

   private String OP[] = { "","=","!=",">","<",">=","<=" };

   /** Teste si le nom de la colonne est demandée en valeur absolue, càd
    * qu'elle est écrite de la façons suivante "|nom|". Dans ce cas là
    * les barres seront enlevées.
    * @param colName le nom de la colonne, retour sans les |xx|
    * @return true si demandé en valeur absolue
    */
   protected boolean getAbsSearch(StringBuffer colName) {
      if( colName.charAt(0)=='|' && colName.charAt(colName.length()-1)=='|' ) {
         colName.deleteCharAt(0);
         colName.deleteCharAt(colName.length()-1);
         return true;
      }
      return false;
   }

   /** Effectue le test entre la valeur s (ou numS si numérique) et colVal.
    * Dans le cas d'un test nom numérique, la recherche se fait avec les jokers
    * La recherche sur simple chaine est case-insensitive
    * @param mode type de recherche STR,EGAL,DIFF,SUP,INF,SUPEG,INFEG
    * @param numeric true si le test est numérique
    * @param abs true si on fait le test en valeur absolue
    * @param colVal la valeur de colonne
    * @param s la valeur à tester sous forme d'une chaine
    * @param numS la valeur à tester sous forme numérique (uniquement si numeric==true)
    * @return true si le test est vrai
    */
   protected boolean advancedSearch(int mode,boolean numeric,boolean abs, String colVal, String s,double numS) {
      if( mode==STR || !numeric || s.length()==0 ) {
         boolean match;
         if( s.length()==0 ) return colVal.trim().length()==0 ? mode!=DIFF : mode==DIFF; // RQ !=DIFF => EGAL|STR
         else {
            if( s.indexOf('*')<0 && s.indexOf('?')<0 ) match = colVal.indexOf(s)>=0;
            else match = Util.matchMaskIgnoreCase(s,colVal);
         }
         return mode==EGAL || mode==STR ? match : mode==DIFF ? !match : false;
      }
      try {
         double v = Double.parseDouble(colVal);
         if( abs ) v=Math.abs(v);
         return mode==EGAL ? v==numS
               : mode==DIFF ? v!=numS
               : mode==SUP  ? v>numS
                     : mode==INF  ? v<numS
                           : mode==SUPEG? v>=numS
                           : v<=numS;
      } catch( Exception e) {}
      return false;

   }

   /** Selectionne ou déselectionne toutes les sources dont les mesures valident
    * la chaine passée en paramètre. Celle-ci peut être précédé d'un nom de colonne (avec éventuellement
    * des jokers) suivi du caractère =,> ou <... (voir advancedSearch())
    * @param s l'expression de recherche
    * @param flagAdd -1: on travaille sur les objets déjà sélectionné
    *               0: on efface au préalable la liste
    *               1: on ajoute à la liste précédente
    */
   protected void selectSrcByString(String s,int flagAdd) {
      if( flagAdd==0 ) deSelect();

      // Analyse de la recherche (ex: FLU*>10, OTYPE=X, Star, ...)
      StringBuffer col = new StringBuffer();    // pour récupérer un éventuel nom de colonne
      StringBuffer v = new StringBuffer();      // pour récupérer la valeur à chercher
      int mode = getAdvancedSearch(col,v,s);    // type de recherche EGAL,DIFF,SUP,INF...
      s = v.toString();
      boolean abs=false;                        // true si on travaille en valeur absolue
      if( col.length()>0 ) abs = getAbsSearch(col);
      int n=0;

      // Mode suppression de sources dans la liste
      if( flagAdd==-1 ) {
         ArrayList list = new ArrayList(100);
         Legende oLeg=null;
         int colIndex = -1;             // Index de la colonne dans le cas où un nom de champ aurait été spécifié
         double numS=Double.MAX_VALUE;  // Valeur numérique de la valeur à chercher si mode numérique
         boolean numeric=false;         // mode de recherche littéral ou numérique
         for( int i=0; i<aladin.mesure.nbSrc; i++ ) {
            Source o = aladin.mesure.src[i];
            if( o.leg!=oLeg && col.length()>0 ) {
               oLeg=o.leg;
               colIndex = o.leg.matchIgnoreCaseColIndex(col.toString());
               if( colIndex==-1 ) break;  // Pas dans ce plan
               numeric=o.leg.isNumField(colIndex);
               if( numeric ) {
                  try { numS = Double.parseDouble(s); }
                  catch(Exception e) {}
               }
               //             System.out.println("In selection mode="+mode+" col="+col+" val="+v+" colIndex="+colIndex+" dataType="+dataType+" numeric="+numeric+" numS="+numS);
            }

            if( !o.noFilterInfluence() && !o.isSelectedInFilter() ) continue;
            String val[] = o.getValues();
            boolean trouve=false;

            // Un nom de colonne précisée ?
            if( colIndex>=0 ) trouve = advancedSearch(mode,numeric,abs,val[colIndex],s,numS);

            // Sinon on cherche dans toutes les colonnes
            else {
               for( int k=0; k<val.length; k++ ){
                  if( Util.indexOfIgnoreCase(val[k],s)>=0 ) { trouve=true; break; }
               }
            }
            if( trouve )  {
               n++;
               o.setSelect(false);
               list.add(new Integer(i));
            }
         }
         //       System.out.println("Je vais supprimer "+list.size()+" sources de la liste");
         if( list.size()>0 ) {
            aladin.mesure.rmSrc(list);
            majSelect();
            hideSource();
         }

         // Mode génération d'une liste de source ou sélection du prochain
      } else {

         Plan [] allPlans = calque.getPlans();
         for( int j=0; j<allPlans.length; j++ ) {
            Plan p = allPlans[j];
            if( !p.isCatalog() || !p.active) continue;
            int colIndex = -1;             // Index de la colonne dans le cas où un nom de champ aurait été spécifié
            double numS=Double.MAX_VALUE;  // Valeur numérique de la valeur à chercher si mode numérique
            boolean numeric=false;         // mode de recherche littéral ou numérique
            Legende oLeg=null;
            Iterator<Obj> it = p.iterator();
            while( it.hasNext() ) {
               Source o = (Source)it.next();

               // Y a-t-il un nom de colonne précisée ?
               if( oLeg!=o.leg && col.length()>0 ) {
                  oLeg=o.leg;
                  colIndex = o.leg.matchIgnoreCaseColIndex(col.toString());
                  if( colIndex==-1 ) break;  // Pas dans ce plan
                  numeric=o.leg.isNumField(colIndex);
                  if( numeric ) {
                     try { numS = Double.parseDouble(s); }
                     catch(Exception e) {}
                  }
                  //                System.out.println(p.label+" mode="+mode+" col="+col+" val="+v+" colIndex="+colIndex+" dataType="+dataType+" numeric="+numeric+" numS="+numS);
               }

               if( !o.noFilterInfluence() && !(o).isSelectedInFilter() ) continue;
               String val[] = o.getValues();
               boolean trouve=false;

               // Un nom de colonne précisée ?
               if( colIndex>=0 ) trouve = advancedSearch(mode,numeric,abs,val[colIndex],s,numS);

               // Sinon on cherche dans toutes les colonnes
               else {
                  for( int k=0; k<val.length; k++ ){
                     if( Util.indexOfIgnoreCase(val[k],s)>=0 ) { trouve=true; break; }
                  }
               }
               if( !trouve ) continue;
               if( !o.isSelected() ) {
                  n++;
                  o.setSelect(true);
                  vselobj.addElement(o);
                  aladin.mesure.insertInfo(o);
               }
            }
         }
      }

      aladin.trace(2, (flagAdd==-1?"Search [Sub]:":flagAdd==1?"Search [Add]:":"Search:")
            + (col.length()>0 ? " column \""+(abs?"|":"")+col+(abs?"|":"")+"\""
                  : " string ")
                  + OP[mode] + "\""+s+"\""
                  + " => "+n+" matched sources");

      if( flagAdd==1 || n>0 ) {
         aladin.mesure.adjustScroll();
         aladin.mesure.display();
         repaintAll();
      }
   }

   /** Selection de tous les objets du plan uniquement (sans réaffichage ni mise à jour
    * du Frame measurements. */
   protected void selectObjetPlanField(Plan p) {
      //      deSelect();
      Iterator<Obj> it = p.iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         setSelected(o, true);
      }
   }

   /** Déselection de tous les objets du plan (sans réaffichage) */
   protected void unSelectObjetPlanField(Plan p) {
      if( p.pcat==null ) return;
      Iterator<Obj> it = p.iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         setSelected(o, false);
      }
   }

   /** Deselectionne tous les objets et demande un reaffichage si necessaire */
   protected void unSelect() {
      if( deSelect() ) {
         repaintAll();
         aladin.mesure.mcanvas.repaint();
      }
   }

   /** Positionne les mesures dans la fenetre des infos.
    * et reaffiche la fenetre des tools et des vues
    * en fonctions des objets selectionnes
    */
   protected void setMesure() {
      boolean flagExtApp = aladin.hasExtApp();
      String listOid[] = null;
      boolean plasticFlag;
      int nbOid=0;

      if( Aladin.PLASTIC_SUPPORT) {
         plasticFlag = aladin.getMessagingMgr().isRegistered();
      } else plasticFlag=false;


      aladin.mesure.removeAllElements();

      Enumeration<Obj> e = vselobj.elements();
      //long b = System.currentTimeMillis();

      if( flagExtApp /* || plasticFlag */ ) listOid = new String[vselobj.size()];


      // vecteur des sources à supprimer de vselobj
      Vector<Source> v = new Vector<Source>();
      while( e.hasMoreElements() ) {
         Obj o = e.nextElement();

         if( o instanceof Source ) {
            // pas de selection pour les objets ne "passant" pas le filtre
            if( ((Source)o).noFilterInfluence() || ((Source)o).isSelectedInFilter() ) {
               o.info(aladin);
            }
            else {
               ((Source)o).setSelect(false);
               v.addElement( (Source) o);
            }

            // Test si cette source provient d'une application cooperative
            // type VOPlot, et si oui, fera le callBack adequat
            if( flagExtApp /* || plasticFlag */ ) {
               String oid = ((Source)o).getOID();
               if( oid!=null ) listOid[nbOid++]=oid;
            }
         }
      }

      // on supprime de vselobj toutes les sources qui doivent l'etre
      Enumeration<Source> eObjTodel = v.elements();
      while( eObjTodel.hasMoreElements() ) vselobj.removeElement(eObjTodel.nextElement());

      //long end = System.currentTimeMillis();
      //System.out.println(end-b);
      aladin.toolBox.toolMode();
      aladin.mesure.mcanvas.repaint();

      // Callback adequat pour la liste des sources que les applications cooperatives
      // doivent selectionner
      if( flagExtApp ) aladin.callbackSelectVOApp(listOid,nbOid);
      // thomas, pour PLASTIC 08/12/2005
      if( Aladin.PLASTIC_SUPPORT && plasticFlag ) {
         try { aladin.getMessagingMgr().sendSelectObjectsMsg(); }
         catch( Throwable e1 ) { }
      }

      repaintAll();
   }

   /** Création du repere */
   private void createRepere() {
      repere = new Repere(null);
      repere.setType(Repere.TARGET);
      repere.dej=Double.NaN;       // Pour pouvoir repèrer qu'il n'a jamais été initialisé
   }

   /** Modification du texte associé au repère */
   protected void setRepereId(String s) {
      repere.setId("");
      //      repere.setId("Reticle location => "+s);
   }

   //   private Vector undoStack = new Vector();  // Mémorise les positions (repere,vue courante, zoom)
   //   private int nUndo=-1;   // indice de la position courante dans la liste undo

   /** Permet la mémorisation des position pour la liste undo */
   //   class Undo {
   //      double zoom;                // issu du viewSimple courant
   //      double xzoomView,yzoomView; // idem
   //      double ra,de;               // Position du repère
   //      Source source;              // Source courante s'il y a lieu
   //      int vn;                     // Numéro de la vue
   //      int nbView;                 // Nombre de vue
   //      int prefSignature;          // signature du plan de référence de la vue (pour comparaison)
   //
   //      Undo(ViewSimple v,Coord c,Source s) {
   //         zoom=v.zoom;
   //         xzoomView=v.xzoomView;
   //         yzoomView=v.yzoomView;
   //         ra=c.al;
   //         de=c.del;
   //         source=s;
   //         vn=v.n;
   //         nbView=modeView;
   //         prefSignature=v.pref.hashCode();
   //      }
   //
   //      /** retourne true si la postion */
   //      boolean samePos(Undo u) {
   //         if( ra!=u.ra || de!=u.de ) return false;
   //         if( vn!=u.vn ) return false;
   //         if( prefSignature!=u.prefSignature ) return false;
   //         return true;
   //      }
   //
   //      /** Mise à jour du zoom uniquement */
   //      void update(Undo u) {
   //         zoom=u.zoom;
   //         xzoomView = u.xzoomView;
   //         yzoomView=u.yzoomView;
   //      }
   //
   //      public String toString() {
   //         return "Stack zoom="+zoom+" x,y="+xzoomView+","+yzoomView+" ra,dec="+new Coord(ra,de)+
   //         " v.n="+vn+" prefSignature="+prefSignature;
   //      }
   //   }
   //
   //   /** reset de la liste undo */
   //   protected void resetUndo() {
   //      synchronized( undoStack ) {
   //         undoStack.clear();
   //         nUndo=-1;
   //      }
   //   }
   //
   //   /** Retourne true si on est au bout de la liste undo */
   //   protected boolean onUndoTop() {
   //      synchronized( undoStack ) { return undoStack.size()-1==nUndo; }
   //   }
   //
   //   /** Mémorise une position sur le haut de la liste undo, et repositionne le cursuer
   //    * de la liste en haut de la pile. Dans le cas ou le haut de la pile concerne
   //    * la même vue à la même position, se contente de mettre à jour les variables
   //    * associées au zoom */
   //   protected void memoUndo(ViewSimple v,Coord coo,Source source) {
   //      if( Aladin.NOGUI ) return;
   //      if( coo==null ) coo = new Coord(repere.raj,repere.dej);
   //      Undo u = new Undo(v,coo,source);
   //      synchronized( undoStack ) {
   //         int n = undoStack.size()-1;
   //         if( n>0 ) {
   //            Undo ou = (Undo)undoStack.elementAt(n);
   //            if( ou.samePos(u) ) {
   //               ou.update(u);
   //               return;
   //            }
   //         }
   //
   //         // On supprime toute la fin de la la file
   //         while( undoStack.size()>nUndo+1 ) undoStack.remove(nUndo+1);
   //
   //         undoStack.addElement(u);
   //         purgeUndo();
   //         nUndo=undoStack.size()-1;
   //      }
   //   }
   //
   //   /** Nettoyage de la file undo pour toutes les vues qui ont changé de plan de réf */
   //   private void purgeUndo() {
   //      synchronized( undoStack ) {
   //         for(int i=0; i<undoStack.size(); i++ ) {
   //            Undo u = (Undo)undoStack.elementAt(i);
   //            if( viewSimple[u.vn].pref==null
   //                  || u.prefSignature!=viewSimple[u.vn].pref.hashCode() ) {
   //               undoStack.remove(i);
   //               if( nUndo>i ) nUndo--;
   //               i--;
   //            }
   //         }
   //      }
   //   }
   //
   //   /** Retourne true si l'on peut revenir en arrière sur la liste undo */
   //   protected boolean canActivePrevUndo() {
   //      synchronized( undoStack ) { return nUndo>0; }
   //   }
   //
   //   /** Retourne true si l'on peut aller en avant sur la liste undo */
   //   protected boolean canActiveNextUndo() {
   //      synchronized( undoStack ) { return nUndo<undoStack.size()-1; }
   //   }
   //
   //   /** Effectue un undo */
   //   protected void undo(boolean flagFirst) {
   //      synchronized( undoStack ) {
   //         if( nUndo<=0 ) return;
   //         try {
   //            nUndo= flagFirst ? 0 : nUndo-1;
   //            setUndo((Undo)undoStack.elementAt(nUndo));
   //         } catch( Exception e) {}
   //      }
   //   }
   //
   //   /** Effectue un redo */
   //   protected void redo(boolean flagLast) {
   //      synchronized( undoStack ) {
   //         if( nUndo==undoStack.size()-1 ) return;
   //         try {
   //            nUndo= flagLast ? undoStack.size()-1 : nUndo+1;
   //            setUndo((Undo)undoStack.elementAt(nUndo));
   //         } catch( Exception e) {}
   //      }
   //   }
   //
   //   /** Exécute le undo passé en paramètre => repositionne la vue courante,
   //    * le zoom et le repère en conséquence */
   //   private void setUndo(Undo u) {
   //      if( Aladin.NOGUI ) return;
   //      setModeView(u.nbView);
   //      ViewSimple v = viewSimple[u.vn];
   //      if( v.pref.hashCode()!=u.prefSignature ) {
   ////System.out.println("Pref modifié => ignore");
   //         return;
   //      }
   //
   //      aladin.calque.unSelectAllPlan();
   //      setSelect(u.vn);
   //
   //      currentView=-1;   // Pour contourner le test dans setCurrentView()
   //      setCurrentView(viewSimple[u.vn]);
   //      moveRepere(u.ra,u.de);
   //      if( v.pref instanceof PlanBG ) setRepere(new Coord(u.ra,u.de));
   //      else v.setZoomXY(u.zoom,u.xzoomView,u.yzoomView);
   //
   //      if( u.source!=null ) aladin.mesure.mcanvas.show(u.source, 2);
   //
   //      aladin.calque.repaintAll();
   //      aladin.log("Undo","");
   //   }

   /** Déplacement du repere courant sur la source passée en paramètre et réaffichage
    *  @param coo on utilise les champs ra,dec uniquement
    *  @return true si le repère a pu être bougé au moins une fois
    */
   protected boolean setRepere(Source s) {
      Coord coo = new Coord(s.raj,s.dej);
      boolean rep = setRepere(coo);
      int m=getNbView();
      for( int i=0 ;i<m; i++ ) {
         ViewSimple v = viewSimple[i];
         if( !v.isPlotView() ) continue;
         rep |= v.setZoomSource(0,s);
         viewSimple[i].repaint();
      }
      return rep;
   }

   /** Déplacement du repere courant et réaffichage
    *  @param coo on utilise les champs ra,dec uniquement
    *  @return true si le repère a pu être bougé au moins une fois
    */
   protected boolean setRepere(Coord coo) { return setRepere(coo,false); }
   protected boolean setRepere(Coord coo,boolean force) {
      moveRepere(coo);

      syncView(1,null,null,force);              // <= POUR THOMAS
      boolean rep=false;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         viewSimple[i].repaint();
         rep |= viewSimple[i].isInImage(coo.al,coo.del);

         // Petite subtilité pour éviter tous les bugs liés à l'utilisation du ViewSimple.projLocal
         // à la place de pref.projd.
         if( currentView==i && !viewSimple[i].isFree()
               && viewSimple[i].pref instanceof PlanBG && viewSimple[i].projLocal!=null ) {
            //            viewSimple[i].pref.projd = viewSimple[i].projLocal.copy();
            viewSimple[i].pref.projd.setProjCenter(coo.al, coo.del);
         }
      }
      return rep;
   }

   /** Nouvelle position du repere courant (sans réaffichage) */
   protected void moveRepere(Coord coo) { {
      moveRepere(coo.al,coo.del); }
   }
   protected void moveRepere(double ra, double dec) {
      repere.raj=ra;
      repere.dej=dec;

      if( aladin.frameCooTool!=null ) aladin.frameCooTool.setReticle(ra,dec);

      aladin.localisation.setLastCoord(ra,dec);

      // TEST : mise à jour des champs des formulaires de saisie en fonction de la position du repère
      //      aladin.dialog.adjustParameters();
   }


   /** Indique que les vues doivent être tracées le plus vite possible */
   protected boolean mustDrawFast() {
      ViewSimple v = getCurrentView();
      if( v instanceof ViewSimpleStatic ) return true;
      
      //      System.out.println("mustDrawFast: v.flagScrolling="+v.flagScrolling+" zoomView.flagdrag="+aladin.calque.zoom.zoomView.flagdrag);
      return v.flagScrolling || aladin.calque.zoom.zoomView.flagdrag;
   }

   private long lastRepaint=0;

   /** Positionne la date du dernier repaint */
   protected void setPaintTimer() {
      lastRepaint = Util.getTime();
   }

   /** Indique qu'il est possible de prendre son temps pour tracer */
   protected boolean canDrawAll() {
      if( mustDrawFast() ) return false;
      long lastDelai = Util.getTime() - lastRepaint;
      return lastDelai > 500;
   }

   /** Action sur le ENTER dans la boite de localisation */
   protected void sesameResolve(String coord) { sesameResolve(coord,false); }
   protected String sesameResolve(String coord,boolean flagNow) {
      saisie=coord;
      setRepereId(coord);
      boolean result=true;
      if( coord.trim().length()>0 ) {

         coord = aladin.localisation.getICRSCoord(coord);

         if( notCoord(coord) ) {

            // resolution synchrone
            if( flagNow ) {
               result=(new SesameThread(saisie,null)).resolveSourceName();

               // resolution a-synchrone
            } else {
               String sesameSyncID = sesameSynchro.start("sesame/"+coord,7000);
               SesameThread sesameThread = new  SesameThread(saisie, sesameSyncID);
               Util.decreasePriority(Thread.currentThread(), sesameThread);
               sesameThread.start();
               return null;
            }
         }

         if( result ) setRepereByString();
      }
      return result ? saisie : null;
   }

   /** Thread de résolution Sésame */
   class SesameThread extends Thread {
      private Plan planObj=null;
      private String sourceName=null;
      private String sesameTaskId;

      /** Constructeur pour une résolution par nom de source
       * @param sourceName Nom de la source à résoudre
       * @param sesameTaskId ID du gestionnaire de task (voir synchroSesame)
       */
      SesameThread(String sourceName,String sesameTaskId){
         super("AladinSesameSourceName");
         this.sourceName=sourceName;
         this.sesameTaskId=sesameTaskId;
      }

      /** Constructeur pour une résolution d'un nom d'objet attaché à un plan
       * @param plan pour lequel il faut résoudre le nom d'objet
       * @param sesameTaskId ID du gestionnaire de task (voir synchroSesame)
       */
      SesameThread(Plan plan,String sesameTaskId){
         super("AladinSesamePlan");
         this.planObj=plan;
         this.sesameTaskId=sesameTaskId;
      }

      public void run() {
         if( sourceName!=null ) resolveSourceName();
         else if( planObj!=null ) resolvePlan();
         else System.err.println("SesameThread error, no plane, no planObj !");
      }

      /** Résolution Sésame de l'objet central du plan passé au constructeur du Thread (Sésame) */
      void resolvePlan() {
         try {
            Coord c=null;
            try { c = sesame(planObj.objet); }
            catch( Exception e) { System.err.println(e.getMessage()); }
            if( c!=null ) {
               planObj.co=c;
               suiteSetRepere(planObj.co);
               repaintAll();
            }
         } finally{ sesameSynchro.stop(sesameTaskId); }
      }

      /** résolution Sésame d'une source passée au constructeur du Thread (sourceName) */
      boolean resolveSourceName() {
         try {
            boolean rep=true;
            aladin.localisation.setTextSaisie(sourceName+" ...resolving...");

            Coord c=null;
            try { c = sesame(sourceName); }
            catch( Exception e) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
               System.err.println(e.getMessage());
            }
            if( c==null ) {
               if( sourceName.length()>0 ) {
                  String s = "Command or object identifier unknown ("+sourceName+") !";
                  //                  aladin.command.printConsole(s);
                  aladin.warning(s,1);
               }
               saisie=sourceName;
               rep=false;
            } else {
               saisie=aladin.localisation.J2000ToString(c.al,c.del);
               aladin.console.printInPad(sourceName+" => ("+aladin.localisation.getFrameName()+") "+saisie+"\n");
               if( !setRepereByString() && !aladin.NOGUI ) {
                  Vector<Plan> v = aladin.calque.getPlanBG();
                  if( v!=null && v.size()>0 ) {
                     for( Plan p : v ) p.startCheckBoxBlink();
                     aladin.calque.select.setLastMessage(aladin.getChaine().getString("TARGETNOTVISIBLE"));
                     aladin.calque.select.repaint();
                  }
               }
            }
            if( isFree() ) aladin.command.setSyncNeedRepaint(false);  // patch nécessaire dans le cas où la pile est vide - sinon blocage
            aladin.localisation.setSesameResult(saisie);
            return rep;
         } finally { sesameSynchro.stop(sesameTaskId); }
      }
   }


   //   private String _sesameTaskId=null;
   //   private String _saisie;
   //   private Plan _planWaitSimbad=null;
   static Coord oco=null;
   static String oobjet=null;

   /** resolution Simbad pour le repere d'un plan */
   protected boolean sesameResolveForPlan(Plan p) {

      // Mini-cache du dernier objet resolu
      if( oobjet!=null && oco!=null && p.objet.equals(oobjet) ) {
         p.co = oco;
         return true;
      }

      String sesameTaskId = sesameSynchro.start("sesameResolveForPlan/"+p);
      SesameThread sesameThread = new SesameThread(p, sesameTaskId);
      Util.decreasePriority(Thread.currentThread(), sesameThread);
      sesameThread.start();

      //      // Lancement du Thread de resolution Simbad pour le plan indique
      ////      setSesameInProgress(true);
      //      waitLockSesame();
      //      _planWaitSimbad = p;
      //      _sesameTaskId = sesameSynchro.start("sesameResolveForPlan/"+p);
      //      Thread sr = new Thread(this,"AladinSesameBis");
      //      Util.decreasePriority(Thread.currentThread(), sr);
      ////      sr.setPriority( Thread.NORM_PRIORITY -1);
      //      sr.start();
      return false;
   }

   //   private boolean lock=false;
   //
   //   /** Attente sur le lock pour passage de paramètre à Sesame */
   //   private void waitLockSesame() {
   //      while( !getLockSesame() ) {
   //         Util.pause(10);
   //         System.out.println("View.waitlockSesame...");
   //      }
   //   }
   //
   //   /** Tentative de récupération du lock */
   //   synchronized private boolean getLockSesame() {
   //      if( lock ) return false;
   //      lock=true;
   //      return true;
   //   }
   //
   //   /** Libération du lock */
   //   synchronized private void unlockSesame() { lock=false; }


   /**
    * Interprétation d'une chaine donnant des coordonnées XYLINEAR.
    * Rq: transformation : entrée "nnn[.nn]{ ,:}mmm[.mm]" et en sortie "nnn.nn mmm.mm"
    * ceci est indispensable pour que le parser de coordonnées de Fox
    * détecte que ce sont des coordonnées en décimales
    * Voir setRepereByString()
    */
   private Coord coordXYLinear(String s) throws Exception {
      if( !getCurrentView().pref.projd.isXYLinear() ) throw new Exception("No XYlinear conversion available");
      StringBuffer res = new StringBuffer();
      StringTokenizer st = new StringTokenizer(s," :,");
      for( int i=0; i<2; i++ ) {
         String p = st.nextToken();
         if( p.indexOf('.')<0 ) p = p+".0";
         if( i>0 ) res.append(' ');
         res.append(p);
      }
      return new Coord( res.toString() );
   }

   /**
    * Interprétation d'une chaine donnant des coordonnées XY.
    * Attention, on compte les Y depuis le bas.
    * Si la vue courante dispose d'une astrométrie, les coordonnées ra,dec
    * sont calculées en fonction, sinon elles sont laissées à 0
    */
   private Coord coordXY(String s) throws Exception { return coordXY1(s,true); }
   private Coord coordXYNat(String s) throws Exception {return coordXY1(s,false); }
   private Coord coordXY1(String s,boolean modeFits) throws Exception {
      StringTokenizer st = new StringTokenizer(s," :,");
      Coord c = new Coord();
      c.x = Double.parseDouble( st.nextToken() )- (modeFits?0.5:0);
      c.y = Double.parseDouble( st.nextToken() )- (modeFits?0.5:0);
      ViewSimple v = getCurrentView();
      if( v.pref.isImage() && modeFits ) c.y = ((PlanImage)v.pref).naxis2 - c.y;
      if( Projection.isOk(v.pref.projd) ) {
         v.pref.projd.getCoord(c);
      }

      //      if( Aladin.levelTrace>=3 ) {
      //         System.out.println("["+s+"] calibration : "+v.pref.projd.getName());
      //         System.out.println("   XY=>RADEC : c.x="+c.x+" c.y="+c.y+" => "+Coord.getSexa(c.al,c.del)+" ("+c.al+","+c.del+")");
      //         double x1=c.x,y1=c.y;
      //         v.pref.projd.getXY(c);
      //         System.out.println("   RADEC=>XY : "+Coord.getSexa(c.al,c.del)+" ("+c.al+","+c.del+") => c.x="+c.x+" c.y="+c.y);
      //         System.out.println("   décalage en X="+(c.x-x1)+" en Y="+(c.y-y1));
      //      }
      return c;
   }

   /** Positionnement du repere en fonction des coordonnees de la chaine "saisie" */
   protected boolean setRepereByString() {
      boolean rep=false;
      try {
         Coord c=null;
         switch( aladin.localisation.getFrame() ) {
            case Localisation.XYNAT:
               c = coordXYNat(saisie);
               break;
            case Localisation.XY:
               c = coordXY(saisie);
               break;
            case Localisation.XYLINEAR:
               c = coordXYLinear(saisie);
               break;
            default:
               c = new Coord(aladin.localisation.getICRSCoord(saisie));
         }
         rep = setRepere(c,true);
         aladin.sendObserver();
      } catch( Exception e) {
         aladin.warning("New reticle position error ("+saisie+")",1);
         if( aladin.levelTrace>=3 ) System.err.println(e.getMessage());
      }
      nextSaisie=true;
      return rep;
   }


   /** Positionnement du repere en fonction du target du plan de reference */
   protected void setRepere(Plan p) {
      if( p.objet==null || p.objet.length()==0 ) return;
      if( p.co==null ) {
         if( notCoord(p.objet) ) {
            if( !sesameResolveForPlan(p) ) return;
         } else {
            try { p.co=new Coord(p.objet); }
            catch( Exception e) { System.err.println(e); }
         }
      }
      setRepereId(p.objet);
      suiteSetRepere(p.co);
   }

   /** Suite de la procedure du positionnement du repere d'un plan */
   private void suiteSetRepere(Coord c) {
      setRepere(c);

      // Positionnement eventuel et a posteriori d'un centre
      // d'un FoV
      calque.setCenterForField(c.al,c.del);
   }


   /** Aiguillage pour les 2 differents types de resolution simbad
    * et un éventuel blinking
    */
   public void run() {
      //System.out.println("run: flagBlink="+flagBlink);
      /* if( _flagSesameResolve ) { _flagSesameResolve=false; runB(); }
         else */ if( _flagTimer ) runC();
         //         else if( _planWaitSimbad!=null ) runA();
   }

   /** Zoom sur la source passée en paramètre */
   protected void zoomOnSource(Source o) {
      getCurrentView().zoomOnSource(o);
   }

   private Source lastShowSource=null;

   /** Retourne la source montrée (qui clignote), ou null si aucune */
   protected Source getShowSource() { return lastShowSource; }

   /** Spécifie que la source o dans toutes les vues doit être "montrée"
    * @param o La source a montrer
    * @param force true si on recalcule la source même si c'est la même
    */
   protected void showSource() { if( lastShowSource!=null ) showSource(lastShowSource,true,false); }
   protected void showSource(Source o) { showSource(o,false,true); }

   protected void showSource(Source o,boolean force,boolean callback) {
      if( !force && lastShowSource==o ) return;   // déjà fait
      lastShowSource=o;

      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = viewSimple[i];

         // Pas projetable dans cette vue
         if( o!=null && o.plan!=null && o.plan.pcat!=null
               && !o.plan.pcat.isDrawnInSimpleView(v.getProjSyncView().n) ) continue;

         // C'est bon
         else v.changeBlinkSource(o);
      }
      blinkMode=0;
      startTimer();

      // Test si cette source provient d'une application cooperative
      // type VOPlot, et si oui, fait le callBack adequat
      if( callback && aladin.hasExtApp() ) {
         String oid = o==null ? null : (o).getOID();
         aladin.callbackShowVOApp(oid);
      }

      // envoi d'un message PLASTIC si nécessaire
      if( callback && Aladin.PLASTIC_SUPPORT ) {
         try { aladin.getMessagingMgr().sendHighlightObjectsMsg(o); }
         catch( Throwable e ) { }
      }
   }

   /** Ne montre plus la source precedemment montree */
   protected void hideSource() { showSource(null); }

   /** Positionne le mode Timer ou non des vues */
   synchronized private void setFlagTimer(boolean a) { _flagTimer = a; }

   private int defaultDelais=1000;
   synchronized private void setDefaultDelais(int d) { defaultDelais = d; }
   synchronized int getDefaultDelais() { return defaultDelais; }

   /** Lancement du mode Timer des vues afin de montrer une source
    *  (controle par le thread threadB)
    *  particulière.
    */
   protected void startTimer() { startTimer(1000); }
   protected void startTimer(int delais) {
      if( Aladin.NOGUI ) return;
      setDefaultDelais(delais);
      if( timer!=null ) {
         //System.out.println("Timer thread already running");
         try { timer.interrupt(); return; }
         catch( Exception e ) {}

      }
      setFlagTimer(true);
      timer = new Thread(this,"AladinTimer");
      //System.out.println("launch timer thread "+timer);
      timer.setPriority( Thread.NORM_PRIORITY -1);
      timer.start();
   }

   volatile Thread timer=null;

   /** Arrêt du mode Timer */
   protected void stopTimer() {
      //System.out.println("stopTimer");
      setFlagTimer(false);
   }

   /** Retourne true si on est en train d'éditer un tag/label */
   private boolean isTagEditing() {
      return newobj instanceof Tag && ((Tag)newobj).isEditing();
   }

   /** Boucle du thread threadB du mode Timer. Effectue un simple
    *  repaint des vues toutes les 150 msec
    */
   private void runC() {
      long debut = -1;
      long t,lastT=-1;
      boolean tagBlink,editBlink,planBlink,sourceBlink,simbadBlink,scrolling,sablierBlink,taquinBlink;
      int delais=getDefaultDelais();

      for( int tour=0; _flagTimer; tour++ ) {
         try {
            delais=getDefaultDelais();
            planBlink=sourceBlink=scrolling=sablierBlink=taquinBlink=editBlink=tagBlink=false;
            int t0,t1,t2,t3,t4,t5,t6;
            t0=t1=t2=t3=t4=t5=t6=0;

            ViewSimple cv= getCurrentView();

            int m=getNbView();
            for( int i=0; i<m; i++ ) {
               ViewSimple v = viewSimple[i];

               boolean plan = v.isPlanBlink() && v.cubeControl.mode!=CubeControl.PAUSE;
               boolean source = v.isSourceBlink();
               boolean scroll = v.isScrolling();
               boolean sablier = v.isSablier();
               boolean taquin = flagTaquin;
               boolean flagEdit = crop!=null && crop.isEditing()
                     || cv.isPlanBlink() && cv.cubeControl.isEditing();
               boolean flagtag = isTagEditing();

               if( flagtag && v==cv ) {
                  v.repaint();
                  t6=500;
                  if( t6<delais ) delais=t6;
               }
               if( flagEdit && v==cv ) {
                  v.repaint();
                  t0=500;
                  if( t0<delais ) delais=t0;
               }
               if( plan  ) {
                  t1 = v.paintPlanBlink();
                  if( t1<delais ) delais=t1;
               }
               if( source  ) {
                  v.paintSourceBlink();
                  t2=150;
                  if( t2<delais ) delais=t2;
               }
               if( scroll ) {
                  v.scrolling();
                  t3=25;
                  if( t3<delais ) delais=t3;
               }
               if( sablier ) {
                  v.repaint();
                  t4=300;
                  if( t4<delais ) delais=t4;
               }
               if( taquin ) {
                  aladin.calque.zoom.zoomView.repaint();
                  t5=1000;
                  if( t5<delais ) delais=t5;
               }

               tagBlink|=flagtag;
               editBlink|=flagEdit;
               planBlink|=plan;
               sourceBlink|=source;
               scrolling|=scroll;
               sablierBlink|=sablier;
               taquinBlink|=taquin;
            }

            simbadBlink = calque.flagSimbad || calque.flagVizierSED;

            if( t2>0 ) blinkMode++;

            // Peut être faut-il lancer une résolution quick Simbad ?
            // et/ou VizierSED ?
            if( simbadBlink && startQuickSimbad>0 ) {
               if( System.currentTimeMillis()-startQuickSimbad>500) {
                  startQuickSimbad=0L;
                  if( calque.flagSimbad ) quickSimbad();
                  if( calque.flagVizierSED && !calque.flagSimbad ) quickVizierSED();
               }
            }

            //t = System.currentTimeMillis();
            //int gap= lastT>=0? (int)(t-lastT) : -1;
            //lastT=t;
            //System.out.println("timer thread "+Thread.currentThread()+" gap=("+gap+") delais="+delais);

            // Arrêt au bout de 5 secondes sans blinking nécessaire
            if( tagBlink|editBlink|planBlink|sourceBlink
                  |simbadBlink|scrolling|sablierBlink|taquinBlink ) debut=-1;
            else {
               if( debut==-1 ) debut=System.currentTimeMillis();
               else if( System.currentTimeMillis()-debut>5000 ) stopTimer();
            }

         } catch( Exception e) {}
         try { Thread.currentThread().sleep(delais);} catch(Exception e) {
            //System.out.println("recu une interruption");
         }
      }
      timer=null;
      //System.out.println("stop blink thread");
   }
   //      long lastT=-1;

   //   private int sesameInProgress=0;  // Nombre de sésames en attente de résolution
   //   synchronized protected boolean isSesameInProgress() { return sesameInProgress>0; }
   //   synchronized private void setSesameInProgress(boolean flag) { if( flag ) sesameInProgress++; else sesameInProgress--; }

   // Synchro sur les résolutions de sésame
   protected Synchro sesameSynchro = new Synchro(10000);
   protected boolean isSesameInProgress() { return !sesameSynchro.isReady(); }

   /** Resolution Sesame a proprement parle
    * @param objet le nom d'objet
    * @return les coordonnées de l'objet (J2000) ou null si non trouvé
    */
   protected Coord sesame(String objet) throws Exception {
      URL url=null;
      String s=null;

      // Mini-cache du dernier objet resolu
      if( oobjet!=null && oco!=null && objet.equals(oobjet) ) {
         return oco;
      }
      oco=null;

      try {
         url = aladin.glu.getURL("openSesame",URLEncoder.encode(objet),true);

         UrlLoader urlLoader = new UrlLoader(url,3000);
         String res = urlLoader.getData();
         StringTokenizer st = new StringTokenizer(res,"\n");
         while( st.hasMoreTokens() ) {
            s = st.nextToken();
            //            System.out.println("Sesame read :["+s+"]");
            if( s.startsWith("%J ") ) {
               StringTokenizer st1 = new StringTokenizer(s);
               st1.nextToken();
               oco = new Coord(st1.nextToken()+" "+st1.nextToken());
               oobjet=objet;
               Aladin.trace(2,"Sesame: "+objet+" -> "+oco.getSexa());
               return oco;
            }
         }
      } catch( Exception e ) {
         System.err.println("View.sesame..."+e.getMessage());
         e.printStackTrace();
         // On va essayer un autre site Sesame...
         if( nbSesameCheck<MAXSESAMECHECK && aladin.glu.checkIndirection("openSesame","") ) {
            URL url1 = aladin.glu.getURL("Sesame",URLEncoder.encode(objet),true);
            if( !url.equals(url1) ) {
               nbSesameCheck++;
               aladin.command.console("!!! Automatic Sesame site switch => "+url1);
               return sesame(objet);
            }
         }

         if( aladin.levelTrace>=3 ) e.printStackTrace();
         throw new Exception(aladin.chaine.getString("NOSESAME"));
      }
      return oco;
   }

   private int nbSesameCheck=0;                     // Nombre de fois où l'on a changé de site Sesame
   private static final int MAXSESAMECHECK = 2;     // Nombre MAX de changement de site Sesame

   // Object pour afficher la distance entre 2 objets sélectionnés
   protected CoteDist coteDist = null;

   protected Repere simRep = null;
   private long startQuickSimbad=0L;
   private double ox=0, oy=0;		// Pour vérifier qu'on déplace suffisemment

   /** Ouverture de la page simbad pour l'objet indiqué par le repere SimRep */
   protected void showSimRep() {
      int offset = simRep.id.indexOf('(');
      if( offset<0 ) return;
      String obj = simRep.id.substring(0,offset).trim();
      aladin.glu.showDocument("smb.query", Tok.quote(obj));
   }

   /** Arrêt de la procédure Quick Simbad */
   synchronized protected void suspendQuickSimbad(){ startQuickSimbad=0L; simRep=null;  }

   static final int TAILLEARROW = 15;

   /** (Re)démarrage du compteur en attendant une requête quickSimbad */
   synchronized protected void waitQuickSimbad(ViewSimple v) {
      if( v.pref==null || !(v.pref.isImage() || v.pref instanceof PlanBG) || v.lastMove==null ) return;
      if( !Projection.isOk(v.pref.projd) || v.isAllSky() /* || v.getTaille()>1 */ ) return;
      if( Math.abs(ox-v.lastMove.x)<TAILLEARROW/v.zoom && Math.abs(oy-v.lastMove.y)<TAILLEARROW/v.zoom ) return;
      if( simRep!=null && simRep.inLabel(v, v.lastView.x, v.lastView.y) ) return;

      startQuickSimbad = System.currentTimeMillis();
      if( simRep!=null ) {
         extendClip(simRep);
         simRep=null;
         v.repaint();
      }
      startTimer();
   }


   /** Resolution d'une requête quickSimbad sur la position courante
    * afin de récupérer les informations de base sur l'objet sous
    * la souris */
   protected void quickSimbad() {
      ViewSimple v = getMouseView();
      Coord coo = new Coord();
      if( v==null || v.pref==null
            || v.pref.projd==null
            || v.lastMove==null  ) return;

      String s=null;
      ox = coo.x = v.lastMove.x;
      oy = coo.y = v.lastMove.y;
      v.getProj().getCoord(coo);
      if( Double.isNaN(coo.al) ) return;
      String target = coo.getSexa(":");

      // Est-on sur un objet avec pixel ?
      // CA NE MARCHE PAS BIEN, JE PREFERE LAISSER DE COTE
      //      if( !v.isMouseOnSomething() ) return;

      // Quelle est le rayon de l'interrogation (15 pixels écran au dessus) Max 1°
      double d = coo.del;
      PointD p = v.getViewCoordDble(coo.x, coo.y);
      p = v.getPosition(p.x, p.y-15);
      coo.x=p.x; coo.y=p.y;
      v.getProj().getCoord(coo);
      double radius = Util.round(Math.abs(coo.del-d),7);

      Aladin.makeCursor(v,Aladin.WAITCURSOR);

      // Faut-il également charger un SED ?
      // S'il y a déjà un SED affiché à partir d'un catalogue de la pile, on ne le fera pas.
      boolean flagSED=true;
      Source o;
      if( aladin.view.zoomview.flagSED && (o=aladin.mesure.getFirstSrc())!=null && o.leg.isSED() ) flagSED=false;

      InputStream is = null;
      DataInputStream cat = null;
      try {
         aladin.status.setText("Querying Simbad...");
         if( flagSED ) zoomview.setSED((String)null);
         URL url = aladin.glu.getURL("SimbadQuick","\""+target+"\" "+radius,false);
         is = url.openStream();
         if( is!=null ) {
            cat = new DataInputStream(is);
            s = cat.readLine();
         }
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return;
      }
      finally {
         try {
            if( cat!=null ) cat.close();
            else if( is!=null ) is.close();
         } catch( Exception e1 ) {}
      }

      // On affiche le résultat
      if( simRep!=null ) extendClip(simRep);
      if( s==null || s.trim().length()==0 ) {
         simRep=null;
         aladin.status.setText("No Simbad object here !");
         if( calque.flagVizierSED ) quickVizierSED();
      } else {
         StringTokenizer st = new StringTokenizer(s,"/");
         try {
            coo = new Coord(st.nextToken());
            simRep = new Repere(null,coo);
            simRep.setType(Repere.CARTOUCHE);
            simRep.setSize(TAILLEARROW);
            simRep.projection(v);
            String s1=s.substring(s.indexOf('/')+1);
            aladin.status.setText(s1+"    [by Simbad]");
            simRep.setId(s1);
            simRep.setWithLabel(true);
            aladin.console.printInPad(s1+"\n");

            // Et on cherche le SED correspondant
            if( flagSED && calque.flagVizierSED ) {
               String s2 = s.substring( s.indexOf('/')+1,s.indexOf('(')).trim();
               aladin.trace(2,"Loading VizieR phot. for \""+s2+"\"...");
               Repere sedRep = null;
               sedRep = new Repere(null,coo);
               sedRep.setType(Repere.CARTOUCHE);
               sedRep.setSize(TAILLEARROW);
               sedRep.projection(v);
               sedRep.setId("Phot: "+target);
               sedRep.setWithLabel(true);
               aladin.view.zoomview.setSED(s2,sedRep);
            }
         } catch( Exception e ) { return; }

      }
      Aladin.makeCursor(v,Cursor.DEFAULT_CURSOR);

      v.repaint();
   }

   /** Resolution d'une requête VizieRSED sur la position courante
    * afin de récupérer le SED sous la souris */
   protected void quickVizierSED() {
      ViewSimple v = getMouseView();
      Coord coo = new Coord();
      if( v==null || v.pref==null
            || v.pref.projd==null
            || v.lastMove==null  ) return;

      ox = coo.x = v.lastMove.x;
      oy = coo.y = v.lastMove.y;
      v.getProj().getCoord(coo);
      if( Double.isNaN(coo.al) ) return;
      String target = coo.getSexa();

      // Est-on sur un objet avec pixel ?
      //      if( !v.isMouseOnSomething() ) return;

      Aladin.makeCursor(v,Aladin.WAITCURSOR);

      // S'il y a déjà un SED affiché à partir d'un catalogue de la pile, on ne le fera pas.
      boolean flagSED=true;
      Source o;
      if( aladin.view.zoomview.flagSED && (o=aladin.mesure.getFirstSrc())!=null && o.leg.isSED() ) flagSED=false;

      try {
         if( flagSED ) zoomview.setSED((String)null);
         Repere sedRep = null; //new Repere(plan)
         coo = new Coord(target);
         sedRep = new Repere(null,coo);
         sedRep.setType(Repere.CARTOUCHE);
         sedRep.setSize(TAILLEARROW);
         sedRep.projection(v);
         sedRep.setId("Phot.: "+target);
         sedRep.setWithLabel(true);
         aladin.view.zoomview.setSED(target,sedRep);

      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return;
      }

      Aladin.makeCursor(v,Cursor.DEFAULT_CURSOR);
      v.repaint();
   }


   /** Retourne vrai si la chaine contient au moins une lettre */
   static protected boolean notCoord(String s) {
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         if( a[i]>='a' && a[i]<='z' ||  a[i]>='A' && a[i]<='Z' ) return true;
      }
      return false;
   }

   /** Affichage rapide des bordures des vues */
   public void paintBordure() {
      if( aladin.menuActivated() ) return;
      if( aladin.inScriptHelp || aladin.inHelp) return;
      int m=getNbView();
      for( int i=0; i<m; i++ ) viewSimple[i].paintBordure();
   }

   /** Retourne true si le plan est actuellement visible */
   protected boolean isVisible(Plan p) {
      int m=getNbView();
      for( int i=0; i<m; i++ ) if( viewSimple[i].pref==p ) return true;
      return false;
   }

   /** Dans le cas où il existe déjà une vue du plan mais non visible
    *  on va la rendre visible  */
   protected boolean tryToShow(Plan p) {
      int debut=previousScrollGetValue;
      int m=getNbView();
      int fin = debut+m;

      // Est-ce déjà visible ?
      if( isVisible(p) ) return false;
      int n = viewMemo.find(p,fin);
      if( n==-1 || n>=debut && n<fin ) {
         int pos = viewSticked.find(p,0);
         if( pos<0 ) return false;

         // La vue peut être rendue visible mais elle est dans une vue stickée
         // il va donc falloire changer de mode de vue pour la rendre visible
         //System.out.println("*** On va ripper sur la vue stickée en position "+pos);
         setModeView(ViewControl.MAXVIEW);
         p.setActivated(true);
         return true;
      }

      //System.out.println("*** On va ripper sur la vue "+n);
      scrollOn(n);
      p.setActivated(true);
      return true;
   }

   /*
    * Le positionnement des vues dépend de la scrollbar et du modeView
    * Toutes les infos sur les vues sont mémorisées via memoView
    * et peuvent être sauvegardées ou restituées via les méthodes
    * get et set de cette classe. La méthode synchro()  ci-dessous
    * sauvegarde toutes les vues actuellement visible. La méthode
    * recharge() ci-dessous permet de restituer une vue précédemment
    * sauvegardée.
    * La variable previousScrollGetValue indique l'indice de la première
    * vue visible dans memoView.
    * La variable memoPos permet de mémoriser le couple
    * (dernier previousScrollGetValue,currentView) afin de pouvoir y revenir
    * si l'utilisateur diminue le nombre de vues (modeView) puis revient
    * à l'affichage précédent. S'il n'est pas possible de revenir à la
    * configuration d'affichage mémorisée, la vue courante sera la première
    * vue.
    */

   /** Retourne le nombre de vues stickées avant la vue d'indice n
    * s'il est mentionné */
   protected int getNbStickedView() { return getNbStickedView(-1); }
   protected int getNbStickedView(int n) {
      int nbStick=0;
      if( n==-1 ) n=ViewControl.MAXVIEW;
      for( int i=0; i<n; i++ ) if( viewSimple[i].sticked ) nbStick++;
      return nbStick;
   }

   /**
    * Positionne le nombre de vues visiblement simultanément.
    * @param m nombre de vues (ViewControl.MVIEW1, MVIEW4, MVIEW9, VIEW16)
    */
   protected void setModeView(int m) {
      if( aladin.viewControl.modeView!=m ) aladin.viewControl.setModeView(m);
      if( modeView==m ) return;

      // Peut être faut-il générer quelques vues ??
      // AVEC LES TRASNPARENCE DES IMAGES, JE TROUVE QUE CA COMPLIQUE PLUS QUE CA NE SIMPLIFIE
      //      autoSimpleViewGenerator();
      Point pos=null;

      // Sauvegarde des vues de la configuration d'affichage actuelle
      sauvegarde();

      //      int currentNumView=getCurrentNumView();

      // Combien y a-t-il de vues fixées avant la vue courante

      // S'il n'existe pas de configuration d'affichage préalablement mémorisée
      // via memoPos ou qu'il n'est plus possible d'y revenir parce que la vue
      // courante ne peut s'y trouver, la nouvelle configuration d'affichage
      // aura comme première vue la vue courante
      if( memoPos!=null && memoPos.y+currentView<m ) {
         //System.out.println("memoPos à partir de "+memoPos.x+" en position "+memoPos.y);
         pos=new Point(memoPos.x,memoPos.y+currentView);
      } else {
         // Il faut que je soustraits le nombre de vues stickées
         // avant la vue courante pour retomber sur la bonne
         // dans viewMemo.
         pos = new Point(previousScrollGetValue+currentView
               -getNbStickedView(currentView),0);
      }

      //System.out.println("Je recharge "+m+" vues à partir de "+pos.x+" currentView="+pos.y);

      // Mémorisation de la nouvelle configuration d'affichage dans
      // le cas où l'utilisateur souhaite y revenir
      if( m<modeView ) memoPos=new Point(previousScrollGetValue,currentView);
      else memoPos=null;

      // INFO: Pour le mode MVIEW1, la gestion du stick est plus complexe
      // car on va devoir positionner temporairement le stick
      // sur la vue en case 0.
      if( m==ViewControl.MVIEW1 ) {
         int x=getStickPos(currentView);
         viewSimple[0].sticked = memoStick[x];
         if( viewSimple[0].sticked ) {
            //System.out.println("Je copie viewStick["+x+"] à l'emplacement 0");
            viewSticked.set(0,viewSticked.get(x,viewSimple[0]));
         }
      }

      // L'affectation du mode ne se fait qu'ici car la méthode
      // getStickPos() se base sur le mode courant
      modeView=m;

      // Ajustement des flags stick en fonction du nouveau mode Multiview
      if( m!=ViewControl.MVIEW1 ) {
         for( int i=0; i<m; i++ ) {
            //            int x = getStickPos(i);
            //if( memoStick[x] ) System.out.println("Je dois sticker la vue "+i);
            viewSimple[i].sticked = memoStick[ getStickPos(i) ];
         }
      }

      // Chargement des vues stickées dans le nouveau mode
      int nbStick=0;
      for( int i=0; i<m; i++) {
         if( viewSimple[i].sticked ) { nbStick++; rechargeFromStick(i); }
      }

      // Puis les vues normales
      int max = pos.x+m-nbStick;
      for( int i=pos.x,j=0; i<max;j++ ) {
         if( viewSimple[j].sticked ) continue;
         rechargeFromMemo(i++,j);
      }

      previousScrollGetValue=pos.x;
      setCurrentNumView(pos.y);

      // Nouvelle configuration de la scrollbar verticale
      scrollV.setValues(pos.x/m,m, 0  ,1+viewMemo.size()/m);
      int bloc = aladin.viewControl.getNbLig(m);
      scrollV.setBlockIncrement(bloc);

      // Modification du JPanel multiview en fonction de la nouvelle
      // configuration d'affiche
      setDimension(getSize().width,getSize().height);
      adjustPanel(m);
      reallocObjetCache();
      mviewPanel.doLayout();
      repaintAll();
      aladin.calque.zoom.zoomView.repaint();
   }

   /** Sauvegarde des ROIs
    * ATTENTION : LE SAVE NE MARCHE PAS ENCORE !!!! (JUILLET 2006)
    *
    * @param prefix préfixe utilisé pour les noms de fichiers ("ROI" par défaut)
    * @param mode 0 pour exportation en FITS, 1 pour sauvegarde des vues avec surcharges graphiques
    * @param w,h en mode 1, dimension des images à générer
    * @param fmt en mode 1, format des images (Save.JPEG ou Save.BMP)
    */
   protected void exportROI(String prefix) { exportSaveROI(prefix,0,0,0,0); }
   protected void saveROI(String prefix,int w,int h,int fmt) {
      System.out.println("No yet debugged !!!");
      //      exportSaveROI(prefix,1,w,h,fmt);
   }
   private void exportSaveROI(String prefix,int mode,int w,int h,int fmt) {
      if( prefix==null || prefix.trim().length()==0 ) prefix="ROI";
      ViewSimple v = new ViewSimple(aladin,aladin.view,0,0,0);
      sauvegarde();	// pour être sûr que tout est dans viewMemo
      Save save = ( aladin.save!=null ? aladin.save : new Save(aladin) );

      Aladin.trace(1,(mode==0?"Exporting locked images in FITS":
         "Saving locked views in "+(fmt==Save.BMP?"BMP":fmt==Save.PNG?"PNG":"JPEG")+" "+w+"x"+h)
         +" files prefixed by ["+prefix+"]");
      int n = viewMemo.size();
      int m=0;
      for( int i=0; i<n; i++ ) {
         if( viewMemo.get(i,v)==null ) continue;
         if( v.isFree() ) continue;

         if( !v.pref.isPixel() ) continue;
         //         if( !(v.pref instanceof PlanBG) ) continue;

         v.rv = new Rectangle(0,0,viewSimple[0].rv.width,viewSimple[0].rv.height);
         //         v.rv = new Rectangle(0,0,w,h);
         v.setSize(v.rv.width, v.rv.height);
         v.pref.projd = v.projLocal;

         v.newView(1);
         v.setZoomXY(v.zoom,v.xzoomView,v.yzoomView);

         //         v.n=ViewControl.MAXVIEW;
         //         v.n=0;
         //
         //
         //         // Duplication du plan de référence pour ne pas "l'abimer"
         //         PlanImage p = new PlanImage(aladin);
         //         v.pref.copy(p);
         //         v.pref = p;
         //
         //         ((PlanImage)v.pref).crop((int)Math.floor(v.rzoom.x),(int)Math.floor(v.rzoom.y),
         //               (int)Math.ceil(v.rzoom.width),(int)Math.ceil(v.rzoom.height),false);

         m++;
         String name = prefix+Util.align3(m);
         if( mode==0 ) save.saveImage(name+".fits",v.pref,0);
         else save.saveOneView(name+(fmt==Save.BMP?".bmp":fmt==Save.PNG?".png":".jpg"),w,h,fmt,-1,v);
      }

      v.free();
      newView(1);
      repaintAll();

   }

   /**
    * Sauvegarde dans viewMemo et dans viewSticked des vues de
    * la configuration d'affichage courante
    */
   protected void sauvegarde() {
      if( previousScrollGetValue==-1 ) return;
      //System.out.println("Je memorise "+modeView+" vues à partir de "+previousScrollGetValue);
      int m=getNbView();
      for( int i=0,j=previousScrollGetValue; i<m; i++) {
         if( viewSimple[i].sticked ) viewSticked.set(getStickPos(i),viewSimple[i]);
         else {
            viewSticked.set(getStickPos(i),(ViewSimple)null);  // Pour reseter au cas où
            viewMemo.set(j++,viewSimple[i]);
         }
      }
   }

   /**
    * Rechargement de la vue mémorisé dans viewMemo à l'indice i
    * dans la viewSimple de position j
    */
   protected void rechargeFromMemo(int i,int j) {
      ViewSimple v = viewMemo.get(i,viewSimple[j]);
      if( v==null ) if( viewSimple[j]!=null ) viewSimple[j].free();
      else viewSimple[j] = v;
      if( v==null ) return;
      v.n=j;
      if( v.isFree() ) return;
      v.setZoomXY(v.zoom,v.xzoomView,v.yzoomView);
      v.newView(1);
   }

   /** De-stickage de toutes les vues stickées */
   protected void unStickAll() {
      for( int i=0; i<viewSimple.length; i++ ) {
         if( viewSimple[i].sticked ) unsetStick(viewSimple[i]);
      }
   }

   /** INFO : Fonctionnement des vues "stickées".
    * Les vues stickées restent toujours à la même place, elles ne sont
    * donc pas sujettes au scrolling.
    * Supposons que la vue 2,2 en mode MVIEW4 est stickée et que l'on passe
    * en mode MVIEW9, la vue 2,2 gardera la même place.
    * La gestion du stickage passe par plusieurs structures:
    *    . boolean memoStick[MAXVIEW]: flag des vues stickées. L'order
    *            correspond à l'affichage MAXVIEW, il faudra passer
    *            par la methode getStickPos() pour retomber sur
    *            ses pattes pour les autres modes
    *    . ViewMemo viewSticked: Mémorise les vues stickées (elles ne
    *            sont plus gérées par viewMemo
    *    . boolean sticked (attribut de ViewSimple): permet d'afficher
    *            les bordures des ViewSimple en conséquence
    * Le "stickage" est géré par la méthode stickSelectedView()
    * Il doit être pris en compte par les méthodes suivantes :
    *     setModeView et scrollOn.
    */

   /** Stick toutes les vues sélectionnées. Si elles le sont
    * déjà, on les désticke. */
   protected void stickSelectedView() {
      if( !isMultiView() ) return;
      StringBuffer sID= new StringBuffer();
      ViewSimple v;
      boolean all=true;
      int m=getNbView();
      for( int i=0; i<m; i++ ) {
         v=viewSimple[i];
         if( v.selected && !v.sticked ) all=false;
      }
      boolean flag=!all;

      for( int i=0; i<m; i++ ) {
         v=viewSimple[i];
         if( v.selected ) {
            sID.append(" "+getIDFromNView(v.n));
            if( flag ) setStick(v);
            else unsetStick(v);
         }
      }
      aladin.console.printCommand((flag?"stick":"unstick")+sID);
      repaintAll();
   }

   /** Mémorisation de l'état du flag stick pour la vue i dans le
    * tableau memoStick. Attention, l'ordre de memoStick[] suit
    * l'ordre logique du mode=MAXVIEW. Il faut donc calculer
    * où se trouve chaque vue pour les autres modes */
   private void memoStick(int i,boolean flag) {
      int pos = getStickPos(i);
      //System.out.println("je "+(flag?"":"un")+"stick la vue "+i+" en position "+pos);
      memoStick[pos]=flag;
   }

   /** Mise à jour des structures de mémorisation des vues viewMemo et
    * viewSticked afin de prendre en compte une nouvelle vue stickée
    * @param v la vue à sticker */
   protected void setStick(ViewSimple v) {
      // Memorisation de l'état stick de la vue en cas de changement
      // de mode
      v.sticked=true;
      memoStick(v.n,true);
      // Je mémorise l'état courant
      sauvegarde();
      // Je décale les viewMemo qui suivent pour écraser la viewMemo
      // désormais inutile
      int m=getNbView();
      viewMemo.cale(previousScrollGetValue+m);

   }

   /** Mise à jour des structures de mémorisation des vues viewMemo et
    * viewSticked afin restituer une vue stickée
    * @param v la vue à dé-sticker */
   protected void unsetStick(ViewSimple v) {
      // Memorisation de l'état stick de la vue en cas de changement
      // de mode
      v.sticked=false;
      memoStick(v.n,false);
      // Je décale les viewMemo qui suivent pour libérer la place
      // pour une nouvelle viewMemo
      int m=getNbView();
      viewMemo.decale(previousScrollGetValue+m-1);

      // INUTILE, C'EST FAIT DANS SAUVEGARDE
      //      // Je libère la viewSticked correspondante à v
      //      viewSticked.set(previousScrollGetValue+v.n,(ViewSimple)null);

      // Je mémorise l'état courant
      sauvegarde();
   }

   /** Retourne la position dans viewSticked correspondant
    * à la vue d'indice i en fonction du mode courant.
    * Ceci permet de conserver les vues stickées à la même
    * place quelque soit le mode courant */
   protected int getStickPos(int i) {
      int ligne = aladin.viewControl.getNbCol(modeView);
      int maxLigne = (int)Math.sqrt(ViewControl.MAXVIEW);
      int ajoutParLigne=maxLigne-ligne;
      return i + (i/ligne)*ajoutParLigne;
   }

   /**
    * Rechargement de la vue stickée en position i
    */
   private void rechargeFromStick(int i) {
      ViewSimple v = viewSimple[i]
            = viewSticked.get(getStickPos(i),viewSimple[i]);
      if( v==null ) return;
      v.n=i;
      if( v.isFree() ) return;
      v.setZoomXY(v.zoom,v.xzoomView,v.yzoomView);
      v.newView(1);
   }

   /**
    * Position du scroll sur la vue n
    * @param n la nouvelle position initiale du scroll
    * @param current l'indice de la vue courante dans viewSimple[],
    *                0 si non indiqué
    * @param mode 0 avec sauvegarde dans ViewMemo au préalable,
    *             1, sans sauvegarde (pour rechargement AJ)
    */
   protected void scrollOn(int n) { scrollOn(n,0,0); }
   protected void scrollOn(int n,int current,int mode) {
      int m=getNbView();
      scrollV.setValue(n/aladin.viewControl.getNbCol(modeView));
      if( mode!=1 ) sauvegarde();
      //System.out.println("Je recharge "+modeView+" vues à partir de "+n+" current="+current);

      // D'abord les vues stickées
      int nbStick=0;
      for( int i=0; i<m; i++) {
         if( viewSimple[i].sticked ) { nbStick++; rechargeFromStick(i); }
      }

      // Puis les vues normales
      int k = n+m-nbStick;
      for( int i=n,j=0; i<k;j++ ) {
         if( viewSimple[j].sticked ) continue;
         rechargeFromMemo(i++,j);
      }

      previousScrollGetValue=n;
      setCurrentNumView(current);
      memoPos=null;   // Il ne sera plus possible de revenir à une configuration
      // d'affichage qui avait été au préalable sauvegardée.

      aladin.calque.majPlanFlag();
      aladin.calque.select.repaint();
      for( int i=0; i<m; i++ ) viewSimple[i].repaint();
   }

   protected int getScrollValue() {
      try { return scrollV.getValue()*aladin.viewControl.getNbCol(modeView); }
      catch( Exception e ) { return 1; }
   }

   public JPanel getPlotControlPanelForPlan(Plan plan) {
      JPanel p1 = null;
      int n=0;
      JTabbedPane tab = new JTabbedPane();
      int m=getNbView();
      for( int i =0; i<m; i++ ) {
         ViewSimple v = viewSimple[i];
         if( v.isFree() || !v.isPlotView() ) continue;
         p1 = v.plot.getPlotControlPanelForPlan(plan);
         if( p1==null ) continue;
         tab.add("View "+getIDFromNView(i),p1);
         n++;
      }
      if( n==0 ) return null;
      if( n==1 ) return p1;
      JPanel p = new JPanel();
      p.add(tab);
      return p;

   }

   /** Niveau d'opacité des FoV des plans*/
   protected float opaciteFoV=0f;
   boolean activeFoV=false;
   private Object lock1 = new Object();

   /** Activation progressive des FoV des plans */
   protected void activeFoV() {
      opaciteFoV=0.1f;
      if( activeFoV ) return;
      synchronized( lock1 ) { activeFoV=true; }
      (new Thread("ActiveFoV"){
         public void run() {
            Util.pause(100);
            while( opaciteFoV<1f ) { repaintAll(); Util.pause(50); opaciteFoV+=0.1f; }
            opaciteFoV=1f;
            repaintAll();
            synchronized(lock1) { activeFoV=false; }
         }
      }).start();
   }

   /** Niveau d'opacité de la grille de coordonnées */
   protected float opaciteGrid=1f;

   /** Activation progressive de la grille */
   protected void activeGrid() {
      if( aladin.NOGUI ) {
         opaciteGrid=1f;
         repaintAll();
         return;
      }
      opaciteGrid=0.1f;
      (new Thread("ActiveGrid"){
         public void run() {
            while( opaciteGrid<1f ) { repaintAll(); Util.pause(100); opaciteGrid+=0.1f; }
            opaciteGrid=1f;
            repaintAll();
         }
      }).start();
   }

   /** Désactivation progressive de la grille */
   protected void unactiveGrid() {
      if( aladin.NOGUI ) {
         opaciteGrid=0f;
         repaintAll();
         return;
      }
      opaciteGrid=0.9f;
      (new Thread("UnactiveGrid"){
         public void run() {
            while( opaciteGrid>0f ) { repaintAll(); Util.pause(100); opaciteGrid-=0.1f; }
            opaciteGrid=0f;
            aladin.calque.setOverlayFlag("grid", false);
            repaintAll();
         }
      }).start();
   }

   /**
    * Cette méthode d'affichage du multiview me semble un peu casse-gueule
    * dans le sens où je surcharge repaint() sans appelé super.repaint()
    * et c'est moi-même qui me charge d'appeler individuellement les repaint()
    * de chaque vue. Cela dit, c'est le seul moyen que j'ai trouvé qui semble
    * fonctionner correctement.
    */
   public void repaintAll()      {
      propResume();
      repaintAll1(0);
   }
   public void updateAll()       {
      if( aladin.isFullScreen() ) { aladin.fullScreen.repaint(); return; }
      repaintAll1(2);
   }
   public void quickRepaintAll() {
      if( aladin.isFullScreen() ) { aladin.fullScreen.repaint(); return; }
      repaintAll1(1);
   }

   /**
    *
    * @param mode 0 repaintAll, 1 - quickRepaintAll, 2- updateAll
    */
   private void repaintAll1(int mode) {

      if( aladin.NOGUI ) mode=1;        // Pour forcer le réaffichage car sinon pas d'appel à ViewSimple.paintComponent()

      // On réaffiche 2-3 petits trucs
      try{
         aladin.viewControl.repaint();
         aladin.grid.repaint();
         aladin.match.repaint();
         aladin.northup.repaint();
         aladin.oeil.repaint();

         // Ajustement de la configuration d'affichage en fonction de la position
         // de la scrollbar verticale si elle a changé.
         int n = getScrollValue();

         // Insertion ou suppression de la scrollbar verticale
         int m=getNbView();
         boolean hideScroll = getLastUsedView()< m && !hasStickedView();
         if( scrollV.isShowing() && hideScroll  ) { remove(scrollV); validate(); }
         else if( !scrollV.isShowing() && !hideScroll  ) { add(scrollV, "West" ); validate(); }
         //System.out.println("getLastUsedView="+getLastUsedView()+" hasSticked="+hasStickedView()+" => hideScroll="+hideScroll);


         // Repaint avec Scroll
         if( n!=previousScrollGetValue ) {
            int newCurrent = getCurrentNumView() - (n-previousScrollGetValue);
            if( newCurrent<0 ) {
               if( aladin.levelTrace>3 ) System.err.println("View.repaintAll1(): There is a problem with the scroll value ("+newCurrent+") => I assume 0 !");
               newCurrent=1;
            }
            scrollOn(n,newCurrent,0);

            // Simple repaint
         } else {
            aladin.calque.majPlanFlag();
            aladin.calque.select.repaint();
            for( int i=0; i<m; i++ ) {
               if( mode==2 ) viewSimple[i].update(viewSimple[i].getGraphics());
               else if( mode==1 ) viewSimple[i].paintComponent(viewSimple[i].getGraphics());
               else {
                  viewSimple[i].resetFlagForRepaint();
                  viewSimple[i].repaint();
               }
            }
         }

         // Libération des pixels d'origine inutiles
         aladin.calque.freeUnusedPixelsOrigin();

         // Ajustement de la taille du scrollV
         scrollV.setMaximum((viewMemo.size()/aladin.viewControl.getNbCol(modeView)));

         // repaint du gestionnaire de colormap si nécessaire
         if( aladin.frameCM!=null && aladin.frameCM.isVisible() ) aladin.frameCM.majCM();

      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
   }

   public void adjustmentValueChanged(AdjustmentEvent e) {
      repaintAll();
   }
}
