// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.bookmark.FrameBookmarks;
import cds.aladin.prop.PropPanel;
import cds.allsky.Constante;
import cds.astro.Astrotime;
import cds.moc.HealpixMoc;
import cds.moc.SpaceMoc;
import cds.moc.SpaceTimeMoc;
import cds.moc.TimeMoc;
import cds.tools.Astrodate;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Gestion des fenetres des proprietes associees aux plans.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.4 : (novembre 2004) gestion des infos du plan blink
 * @version 1.3 : (7 septembre 2003) fenetres multiples de  Properties
 * @version 1.2 : (20 aout 2002) Edition de l'astrometrie
 * @version 1.1 : (11 mai 99) Correction bug xylock!=null
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Properties extends JFrame implements ActionListener, ChangeListener {

   String SEEFITS,SEEHIPSPROP,SEEPARSING,TABLEINFO,/* LOADURL, */NEWCALIB,MODCALIB,/*,TOPBOTTOM,RIGHTLEFT,NEWCOL*/SHOWFOVS,HIDEFOVS,
   TITLE,BANNER,APPLY,/* BOOKMARK, */CLOSE,NOFILTER,LABEL,COLOR,ERROR,STATE,UNDER,SHAPE,IMG,VIEWABLE,
   LEVEL,REFCOORD,REFROTATE,ANGLE,COMPONENT,SOURCE,INF,FMT,EPOCH,DATEOBS,WCSEQ,SIZE,PIXMODE,FRAME,DELAY,
   ORIGIN,FILTER,FILTERB,ASTRED,XYRED,PROJ,NONE,METHOD,CENTER,SELECTFIELD,DEFCATPROJ,FLIPFLOP,ASSFOV,
   LOCAL,GLOBAL,SCOPE,HSCOPE,OPACITY,OPACITYLEVEL,DENSITY,WHITE,BLACK,AUTO,COLORBG,POLA,DISPLAYPOLA,
   GENERATEPOLAAMP,GENERATEPOLAANG,CURRENTFIELD,POLAOPTIONS,SEGMENTLEN,SEGMENTTHICK,SEGMENTDENSITY,
   SCALINGFACTOR,POINTING,POINTINGLABEL,FULLDESCR;

   String ON="On",OFF="Off";

   // Les references aux objets
   Aladin aladin;
   Plan      plan;

   // Les composantes de l'objet
   JPanel panel;                  // Le panel de la frame
   JPanel propPanel; // panel principal
   PlanFree  pmemo=null;         // un plan (bidon) pgr memoriser les caracteristiques courantes
   int  hcmemo=0;                // Memorisation du hashcode du plan memorise
   boolean flagHide=true;        // Vrai si la fenetre est cache

   // Memorisation temporaire
   JTextField label;              // Le label du plan
   TextField blankField;
   Couleur couleur=null;         // La couleur
   JComboBox sourceType=null;       // Le type de representation graphique
   ButtonGroup longitude;
   JRadioButton longitudeDescending,longitudeAscending;
   ButtonGroup scope;
   JRadioButton scopeGlobal,scopeLocal;
   ButtonGroup xyLock=null;    // Pour le plan TOOl, mode de calcul des x,y
   JComboBox planRefChoice=null;    // Projections possibles pour le plan de reference
   JComboBox specificProj=null;    // Les projections possibles par défaut pour un catalogue sans image sous-jacente ou un planBG
   JComboBox specificFrame=null;     // Les frames possibles par défaut pour un planBG ou un catalogue sans image sous-jacente
   Plan planRef[]=null;		 // Tableau associe a projsChoice
   JComboBox projsChoice=null;      // Plan de projections possibles
   Projection projs[]=null;	 // Tableau associe a planRefChoice
   JButton modCalib=null;	 // JButton de modification de la Calib
   JButton toGenFilterButton=null; // Pour pouvoir éditer un filtre dédié
   JRadioButton nofilter=null;       // La checkbox concernant l'absence de filtre dédié
   JTextField centerField=null;  // Pour le plan FIELD, centre du champ
   String sField=null;       // Pour savoir si centerField a ete modifie
   JTextField rotateCenter=null;  // Pour le plan FIELD, décalage du centre de rotation
   String rotateCenterField=null;       // Pour savoir si rotOffsetRA a ete modifie
   JTextField url=null;      // Pour visualiser, voir éditer l'url d'origine
   JTextField rollField=null;	 // Pour le plan FIELD, rotation du FOV
   JTextField epField=null;  // Pour pouvoir modifier l'epoque par defaut
   JTextField eqField=null;  // Pour pouvoir modifier l'equinoxe par defaut
   String sEquinox=null;	 // Pour savoir si l'equinoxe par defaut a ete modifiée
   String sRoll=null;	 	 // Pour savoir si rollField a ete modifie
   JRadioButton cbT=null;		 // Pour plan ADD, controle Target
   JRadioButton cbS=null;		 // Pour plan ADD, controle Scale
   JRadioButton cbG=null;		 // Pour plan ADD, controle Grid
   JButton btnDisplayPola;       // Pour PlanHealpix, affichage de la polarisation
   JButton btnDisplayAmp;        // pour PlanHealpix, affichage de l'amplitude de la polarisation
   JButton btnDisplayAng;        // pour PlanHealpix, affichage de l'angle de la polarisation
   JCheckBox[] contoursCB;        // Pour PlanContour, pour pouvoir visualiser ou cacher les contours
   Couleur[] contoursCouleurs;   // Pour PlanContour, pour choisir la couleur de chaque contour
   Curseur curs = null;          // Pour PlanContour, permet de changer la valeur des niveaux
   JButton filterButtons[]=null;	 // Pour plan CATALOG, boutons pour filtres prédéfinis
   JPanel panelCont;
   JPanel panelScroll;
   JScrollPane scroll;
   JSlider opacityLevel;
   JSlider gapOrder;

   JSlider epochSlider;
   JSlider scalingFactor; // facteur d'échelle pour les filtres des plans CATALOG

   JSlider polaSegmentLen; // Pour plan POLARISATION, longueur max segments
   JSlider polaSegmentThickness; // Pour plan POLARISATION, epaisseur segments
   JSlider polaSegmentDensity; // Pour plan POLARISATION, nombre de segments (densité)

   private ButtonGroup cb;  // Exclusivité pour les JRadioButton de couleur de background (image RGB)

   // Liste des frame de Properties ouvertes
   static Vector frameProp = new Vector(10);

   protected void createChaine() {
      if( SEEFITS!=null ) return;       // Déjà fait

      SEEFITS = aladin.chaine.getString("PROPSEEFITS");
      SEEHIPSPROP = aladin.chaine.getString("PROPSEEHIPSPROP");
      SEEPARSING = aladin.chaine.getString("VWTABLEINFO");
      TABLEINFO = aladin.chaine.getString("PROPTABLEINFO");
      //      LOADURL = aladin.chaine.getString("PROPLOADURL");
      NEWCALIB = aladin.chaine.getString("PROPNEWCALIB");
      MODCALIB = aladin.chaine.getString("PROPMODCALIB");
      SHOWFOVS = aladin.chaine.getString("PROPSHOWFOVS");
      HIDEFOVS = aladin.chaine.getString("PROPHIDEFOVS");
      TITLE = aladin.chaine.getString("PROPTITLE");
      BANNER = aladin.chaine.getString("PROPBANNER");
      APPLY = aladin.chaine.getString("PROPAPPLY");
//      BOOKMARK = aladin.chaine.getString("PROPBOOKMARK");
      CLOSE = aladin.chaine.getString("PROPCLOSE");
      NOFILTER = aladin.chaine.getString("PROPNOFILTER");
      LABEL = aladin.chaine.getString("PROPLABEL");
      COLOR = aladin.chaine.getString("PROPCOLOR");
      ERROR = aladin.chaine.getString("PROPERROR");
      STATE = aladin.chaine.getString("PROPSTATE");
      UNDER = aladin.chaine.getString("PROPUNDER");
      SHAPE = aladin.chaine.getString("PROPSHAPE");
      IMG = aladin.chaine.getString("PROPIMG");
      VIEWABLE = aladin.chaine.getString("PROPVIEWABLE");
      LEVEL = aladin.chaine.getString("PROPLEVEL");
      REFCOORD = aladin.chaine.getString("PROPREFCOORD");
      REFROTATE = aladin.chaine.getString("PROPREFROTATE");
      ANGLE = aladin.chaine.getString("PROPANGLE");
      COMPONENT = aladin.chaine.getString("PROPCOMPONENT");
      SOURCE = aladin.chaine.getString("PROPSOURCE");
      INF = aladin.chaine.getString("PROPINF");
      FMT = aladin.chaine.getString("PROPFMT");
      EPOCH = aladin.chaine.getString("PROPEPOCH");
      DATEOBS = aladin.chaine.getString("PROPDATEOBS");
      WCSEQ = aladin.chaine.getString("PROPWCSEQ");
      PIXMODE = aladin.chaine.getString("PROPPIXMODE");
      SIZE = aladin.chaine.getString("PROPSIZE");
      FRAME = aladin.chaine.getString("PROPFRAME");
      DELAY = aladin.chaine.getString("PROPDELAY");
      ORIGIN = aladin.chaine.getString("PROPORIGIN");
      FILTER = aladin.chaine.getString("PROPFILTER");
      FILTERB = aladin.chaine.getString("PROPFILTERB");
      ASTRED = aladin.chaine.getString("PROPASTRED");
      XYRED = aladin.chaine.getString("PROPXYRED");
      PROJ = aladin.chaine.getString("PROPPROJ");
      NONE = aladin.chaine.getString("PROPNONE");
      METHOD = aladin.chaine.getString("PROPMETHOD");
      CENTER = aladin.chaine.getString("PROPCENTER");
      SELECTFIELD = aladin.chaine.getString("PROPSELECTFIELD");
      DEFCATPROJ = aladin.chaine.getString("PROPDEFCATPROJ");
      FLIPFLOP = aladin.chaine.getString("PROPFLIPFLOP");
      ASSFOV = aladin.chaine.getString("PROPASSFOV");
      LOCAL = aladin.chaine.getString("PROPLOCAL");
      GLOBAL = aladin.chaine.getString("PROPGLOBAL");
      SCOPE = aladin.chaine.getString("PROPSCOPE");
      HSCOPE = aladin.chaine.getString("PROPHSCOPE");
      OPACITY = aladin.chaine.getString("PROPOPACITY");
      OPACITYLEVEL = aladin.chaine.getString("PROPOPACITYLEVEL");
      DENSITY = aladin.chaine.getString("PROPDENSITY");
      AUTO = aladin.chaine.getString("PROPAUTO");
      WHITE = aladin.chaine.getString("PROPWHITE");
      BLACK = aladin.chaine.getString("PROPBLACK");
      COLORBG = "Background";
      POLA = aladin.chaine.getString("PROPPOLA");
      DISPLAYPOLA = aladin.chaine.getString("PROPDISPLAYPOLA");
      GENERATEPOLAAMP = aladin.chaine.getString("PROPGENERATEOPOLAAMP");
      GENERATEPOLAANG = aladin.chaine.getString("PROPGENERATEOPOLAANG");
      CURRENTFIELD = aladin.chaine.getString("PROPCURRENTFIELD");
      POLAOPTIONS = aladin.chaine.getString("PROPPOLAOPTIONS");
      SEGMENTLEN = aladin.chaine.getString("PROPSEGMENTLENGTH");
      SEGMENTTHICK = aladin.chaine.getString("PROPSEGMENTTHICK");
      SEGMENTDENSITY = aladin.chaine.getString("PROPSEGMENTDENSITY");
      SCALINGFACTOR = aladin.chaine.getString("PROPSCALINGFACTOR");
      POINTING = aladin.chaine.getString("PROPPOINTING");
      POINTINGLABEL = aladin.chaine.getString("PROPPOINTINGLABEL");
      FULLDESCR = aladin.chaine.getString("PROPFULLDESCR");
   }

   /** Creation du Frame donnant les proprietes du plan.
    * @param aladin Reference
    */
   protected Properties(Plan p) {
      super();
      this.aladin = p.aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(TITLE);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true, aladin);
      plan=p;

      addProperties(this);
      int i=frameProp.size();

      // On decale l'emplacement de l'affichage en fonction du nombre de
      // fenetres de Properties deja ouvertes
      Point t = Aladin.computeLocation(this);
      t.x+=i*20;
      t.y+=i*20;
      setLocation(t);
   }

   /** Initialisation d'un JPanel en fonction d'un plan
    * @param force true si on force la réinitiailisation
    */
   void showProp() { showProp(false); }
   void showProp(boolean force) {
      Plan p=plan;
      flagHide = false;

      // Que des modifs mineures = > juste re-affichage
      if( !force && pmemo != null && hcmemo == p.hashCode() && pmemo.equals(p) ) {
         if (isShowing()) return;
         setVisible(true);
         return;
      }

      // On compare le type du precedent plan pour eviter un pack()
      boolean noPack = (pmemo!=null && pmemo.type==p.type
            && pmemo.flagOk==p.flagOk && pmemo.projd==p.projd);


      // On memorise les caracteristiques du plan afin de pouvoir
      // effectuer des comparaisons ulterieurement
      pmemo=new PlanFree(aladin);
      pmemo.objet= (p.objet==null)?null:new String(p.objet);
      pmemo.type = p.type;
      pmemo.param = (p.param==null)?null:new String(p.param);
      pmemo.error = (p.error==null)?null:new String(p.error);
      pmemo.flagOk = p.flagOk;
      pmemo.projd = p.projd;
      hcmemo = p.hashCode();

      // On remet a null ce qui est necessaire
      sourceType=null;
      couleur=null;
      sField=sEquinox=null;
      epField=eqField=rollField=null;

      rotateCenter = null;

      // On reconstruit le panel
      if( panel!=null ) remove(panel);
      panel = new JPanel();
      panel.setLayout( new BorderLayout(5,5) );

      propPanel=getPanelProperties();
      JScrollPane scrollPane = new JScrollPane(propPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      Aladin.makeAdd(panel,scrollPane /*propPanel*/,"Center");
      // bordure avec titre
      setTitre(BANNER+" \""+plan.label+"\"");

      Aladin.makeAdd(panel,getPanelValid(),"South");

      Aladin.makeAdd(this,panel,"Center");
      //      ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

      if( !noPack ) pack();

      setVisible(true);
   }

   /** Construction du panel des boutons de validation
    * @return Le panel contenant les boutons Apply/Close
    */
   protected JPanel getPanelValid() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
      
      
      p.add( b=new JButton(APPLY)); b.addActionListener(this);
      b.setFont( b.getFont().deriveFont(Font.BOLD) );
//      if( plan.getBookmarkCode()!=null ) {
//         p.add( new JLabel(" ") );
//         p.add( b=new JButton(BOOKMARK)); b.addActionListener(this);
//      }
      p.add( b=new JButton(CLOSE)); b.addActionListener(this);
      
      // On ajoute l'icone permettant la mise en bookmark de la requête
      if( plan.getBookmarkCode()!=null ) {
         JPanel p1 = new JPanel( new BorderLayout(2,2) );
         p1.add( p, BorderLayout.CENTER );
         
         b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Bookmark.png")));
         Util.toolTip(b, "Bookmarks this server query");
         b.setMargin(new Insets(0, 0, 0, 0));
         b.setBorderPainted(false);
         b.setContentAreaFilled(false);
         p1.add(b, BorderLayout.EAST );
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               bookmark();
            }
         });
         p=p1;
      }
               
      return p;
   }

   private ButtonGroup filterCB;

   /**
    * Construit le panel des filtres prédéfinis
    */
   private JPanel getPanelFilter(Plan plan) {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setLayout(g);

      filterCB = new ButtonGroup();

      JRadioButton cb = nofilter = new JRadioButton(NOFILTER);
      cb.setActionCommand(NOFILTER);
      cb.addActionListener(this);
      filterCB.add(cb);
      cb.setSelected(true);
      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
      p1.add(cb);
      g.setConstraints(p1,c);
      p.add(p1);

      for( int i=0; i<plan.filters.length; i++ ) {
         String label = ServerGlu.getFilterDescription(plan.filters[i]);
         if( label==null ) label = ServerGlu.getFilterName(plan.filters[i]);
         if( label==null ) continue;
         cb = new JRadioButton(label);
         cb.setMargin(c.insets);
         cb.setActionCommand(label);
         cb.addActionListener(this);
         filterCB.add(cb);
         if( plan.filterIndex==i ) cb.setSelected(true);

         p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
         p1.add(cb);
         g.setConstraints(p1,c);
         p.add(p1);
      }

      return p;
   }

   //   private void showInfo() {
   //      aladin.info(this,plan.verboseDescr);
   //   }


//   /** Genère un Label, éventuellement sur plusieurs lignes, qui peut avoir à la fin un lien (more...) pour de l'info
//    * additionnel, ou une url complète associée. Les deux simultanément ne sont pas possibles.
//    */
//   class MyAnchor extends JLabel {
//      String url,more;
//      int width;
//
//      /**
//       * @param text Texte du baratin (ou null si début du texte supplémentaire à afficher)
//       * @param width nombre de caractères avant repli (-1 si pas de repli), ou césure si text==null
//       * @param more texte supplémentaire accessible par (more...), null sinon
//       * @param url url associée, null sinon
//       */
//      MyAnchor(String text,int width, String more,final String url) {
//         super();
//
//         if( text==null && more==null && url!=null ) text=url;
//
//         if( text==null && more!=null ) {
//            if( more.length()>width ) {
//               int n = more.lastIndexOf(' ',width);
//               if( n<=0 ) n=width;
//               text=more.substring(0,n)+"...";
//            }
//            else { text=more; more=null; }
//         }
//         if( text==null ) text="";
//         this.more = more;
//         this.url=url;
//         if( width>0 ) {
//            if( (text.startsWith("http://") || text.startsWith("ftp://")) && text.length()>width ) text=text.substring(0,width)+"...";
//            else {
//               if( url!=null ) text = Util.fold(text,width,true);
//               text = Util.fold(text,width);
//            }
//         }
//         if( url!=null ) {
//            text = "<html><A HREF=\"\">"+text+"</A></html>";
//            setToolTipText(url);
//         }
//         if( more!=null ) text = "<html>"+text+" <A HREF=\"\">(more...)</A></html>";
//         setText(text);
//         setFont(getFont().deriveFont(Font.ITALIC));
//         final String more1 = more;
//         if( url!=null || more!=null ) {
//            final Component c = this;
//            addMouseMotionListener(new MouseMotionListener() {
//               public void mouseMoved(MouseEvent e) { Aladin.makeCursor(c,Aladin.HANDCURSOR); }
//               public void mouseDragged(MouseEvent e) { }
//            });
//            addMouseListener(new MouseListener() {
//               public void mouseReleased(MouseEvent e) {
//                  if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) return;
//                  if( url!=null ) aladin.glu.showDocument(url);
//                  else aladin.info(c,more1.replace("\\n","\n"));
//               }
//               public void mousePressed(MouseEvent e)  { 
//                  if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) {
//                     showPopMenu(e.getX(),e.getY());
//                  }
//               }
//               public void mouseExited(MouseEvent e)   { Aladin.makeCursor(c,Aladin.DEFAULTCURSOR); }
//               public void mouseEntered(MouseEvent e)  { }
//               public void mouseClicked(MouseEvent e) { }
//            });
//         }
//      }
//      
//      // Affiche le popup
//      private void showPopMenu(int x,int y) {
//         JPopupMenu popMenu = new JPopupMenu();
//         popMenu.setLightWeightPopupEnabled(false);
//         JMenuItem j=new JMenuItem(aladin.chaine.getString("MFCOPYURL"));
//         popMenu.add(j);
//         j.addActionListener( new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//               aladin.copyToClipBoard(url);
//            }
//         });
//         popMenu.show(this,x,y);
//      }
//   }
//
   /** Construction du panel des proprietes du plan courant.
    * @return Le panel des proprietes du plan courant
    */
   protected JPanel getPanelProperties() {
      JButton b;
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
      c.insets = new Insets(2,2,0,5);

      JPanel p = new JPanel();
      p.setLayout(g);

      // le label associe au plan
      label = new JTextField(plan.label,15);
      label.setMinimumSize(label.getPreferredSize());
      PropPanel.addCouple(p, LABEL, label, g,c);

      if( plan.verboseDescr!=null || plan.description!=null ) {
         PropPanel.addCouple(p,"Description: ", new MyAnchor(aladin,plan.description,50,plan.verboseDescr,null), g,c);
      }

      if( plan.ack!=null ) {
         PropPanel.addCouple(p,"Acknowledgment: ", new MyAnchor(aladin,null,40,plan.ack,null), g,c);
      }

      // Origine
      if( !(plan instanceof PlanBG) ||
            (((PlanBG)plan).getProperty(Constante.KEY_OBS_COPYRIGHT)==null && ((PlanBG)plan).getProperty(Constante.KEY_OBS_COPYRIGHT_URL)==null) ) {
         String copyright = plan.copyright==null ? plan.copyrightUrl : plan.copyright;
         if( copyright!=null ) {
            PropPanel.addCouple(p,ORIGIN, new MyAnchor(aladin,copyright,40,null,plan.copyrightUrl), g,c);
         }
      }

      if( plan instanceof PlanBG ) {
         PlanBG pbg = (PlanBG)plan;
         String s,su;

         s = pbg.getProperty(Constante.KEY_BIB_REFERENCE);
         su = pbg.getProperty(Constante.KEY_BIB_REFERENCE_URL);
         if( s!=null || su!=null ) PropPanel.addCouple(p,"Bib. reference", new MyAnchor(aladin,s,40,null,su), g,c);

         if(((PlanBG)plan).id!=null ) {
            JLabel datasetId = new JLabel(((PlanBG)plan).id);
            PropPanel.addCouple(p,"Dataset ID: ", datasetId, g,c);
         }

         s = pbg.getProperty(Constante.KEY_CREATOR);
         if( s==null ) s = pbg.getProperty(Constante.OLD_HIPS_PUBLISHER);
         if( s==null ) s = pbg.getProperty(Constante.KEY_CREATOR_ID);
         if( s==null ) s = pbg.getProperty(Constante.KEY_PUBLISHER_ID);
         if( s==null ) s = pbg.getCreatorFromId();
         if( s!=null ) PropPanel.addCouple(p,"HiPS creator", new JLabel(s), g,c);


         s = pbg.getProperty(Constante.KEY_HIPS_RELEASE_DATE);
         if( s!=null ) PropPanel.addCouple(p,"Release date", new JLabel(s), g,c);

      }

      //La couleur
      if( !(plan.isImage() || plan.type==Plan.ALLSKYIMG)
            && plan.type!=Plan.FOLDER && !(plan instanceof PlanContour)) {
         couleur = new Couleur(plan.c);
         PropPanel.addCouple(p,COLOR, couleur, g,c);
         couleur.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { actionCouleur(); }
         });
      }

      // couleur du plan contour
      if( plan instanceof PlanContour) {
         couleur = new Couleur(PlanContour.couleursBase,plan.c,25,4);
         PropPanel.addCouple(p,COLOR, couleur, g,c);
      }

      // Affichage de l'etat
      if( !plan.flagOk ) {
         MyAnchor l;
         String titre;
         if( plan.error!=null ) { titre = ERROR; l = new MyAnchor(aladin,null,40,plan.error,null); }
         else { titre = STATE; l = new MyAnchor(aladin,null,40,UNDER,null); }
         l.setForeground( Color.red );
         l.setFont(l.getFont().deriveFont(Font.BOLD));
         PropPanel.addCouple(p,titre, l, g,c );
      }

      if( plan.flagOk ) {
         // Les sourceType du Plan
         if( plan.isCatalog() && !plan.hasNoPos ) {
            sourceType = new JComboBox();
            for( int i=0; i<Source.TYPENAME.length; i++ ) sourceType.addItem(Source.TYPENAME[i]);
            sourceType.setSelectedIndex(plan.sourceType);
            PropPanel.addCouple(p,SHAPE, sourceType, g,c );
            sourceType.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { actionSourceType(); }
            });
         }

         if( plan.type==Plan.FOLDER ) {
            JPanel pscope  = new JPanel();
            pscope.setLayout( new FlowLayout(FlowLayout.LEFT));
            scope = new ButtonGroup();
            boolean limitScope = ((PlanFolder)plan).localScope;
            scopeGlobal = new JRadioButton(GLOBAL);
            scope.add(scopeGlobal); scopeGlobal.setSelected(!limitScope);
            scopeLocal = new JRadioButton(LOCAL);
            scope.add(scopeLocal);
            pscope.add( scopeGlobal );
            pscope.add( scopeLocal );
            pscope.add( b=new JButton(" ? ")); b.addActionListener(this);
            PropPanel.addCouple(p,SCOPE, pscope, g,c);
         } else scope=null;

         // cas d'un PlanContour
         if (plan instanceof PlanContour) {
            PlanContour pcont = (PlanContour)plan;

            // ajout du label de l'image sur laquelle a ete faite le contour
            PropPanel.addCouple(p,IMG,new JLabel(pcont.p.label + " - " + pcont.p.objet),g,c);

            int[] contoursLevels = pcont.getIntLevels();
            contoursCB = new JCheckBox[contoursLevels.length];
            contoursCouleurs = new Couleur[contoursLevels.length];

            int scrollWidth = 330;
            int scrollHeight = 200;
            panelScroll = new JPanel();  // panel qui sera integre dans le JScrollPane
            panelScroll.setLayout(g);

            for (int i=0;i<contoursLevels.length;i++) {
               contoursCB[i] = new JCheckBox(VIEWABLE,pcont.isViewable(i));
               contoursCB[i].addActionListener(this);
               panelCont = new JPanel();
               panelCont.add(contoursCB[i]);
               contoursCouleurs[i] = new Couleur(pcont.couleursContours[i],15,3,Couleur.getBrighterColors(pcont.c,4));
               panelCont.add(contoursCouleurs[i]);

               String suffix = (i==0) ? "1st" : ((i==1)? "2nd" : ((i==2)?"3rd": (i+1)+"th"));
               PropPanel.addCouple(panelScroll,suffix + " level",panelCont,g,c);
            }
            scroll = new JScrollPane(panelScroll); // JScrollPane pour le choix des couleurs
            scroll.setSize(scrollWidth,scrollHeight);
            g.setConstraints(scroll, c);
            p.add(scroll);

            // affichage de l'histogramme des niveaux de gris
            Histogramme hist = new Histogramme((PlanImage)pcont.p);
            PropPanel.addCouple(p,LEVEL,hist,g,c);

            // affichage des differents niveaux
            curs = new Curseur(hist);
            curs.nbNiveaux = contoursLevels.length;
            curs.couleurTriangle = pcont.couleursContours;
            for (int i=0;i<contoursLevels.length;i++) {
               curs.niveaux[i] = contoursLevels[i];
            }
            PropPanel.addCouple(p,"",curs,g,c);
         }

         // Une ligne de separation
         if( plan.isCatalog() || plan.isImage() ) PropPanel.addFilet(p,g,c);
         
         if (plan.isCatalog() && plan.query != null) {
            JTextArea ta = new JTextArea(plan.query, 5, 40);
            JScrollPane sc = new JScrollPane( ta );
        	PropPanel.addCouple(p,"ADQL query ", sc, g,c);
		}
      }

      // Centre du champ de l'instrument
      if( plan.type==Plan.APERTURE ) {
         final PlanField pf = (PlanField)plan;
         sField = pf.getProjCenter();
         centerField = new JTextField(sField,25);
         if( !pf.isMovable () ) centerField.setEnabled(false);
         PropPanel.addCouple(p,REFCOORD, centerField, g,c );
         if( Aladin.ROTATEFOVCENTER ) {
            rotateCenterField = pf.getRotCenter();
            JPanel pr = new JPanel( new BorderLayout(0,0));
            rotateCenter = new JTextField(rotateCenterField,25);
            rotateCenter.setEnabled(pf.isRollable() & pf.isCenterRollable() );
            pr.add(rotateCenter,BorderLayout.CENTER);
            JCheckBox rotCheck = new JCheckBox();
            rotCheck.setSelected(pf.isCenterRollable() );
            rotCheck.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  boolean r = !pf.isCenterRollable();
                  pf.setCenterRollable(r);
                  if( !r ) {
                     pf.resetRotCenterObjet();
                     rotateCenter.setText(pf.getRotCenter());
                     rotateCenterField="";
                     apply();
                  }
                  rotateCenter.setEnabled(pf.isRollable() & pf.isCenterRollable() );
               }
            });
            pr.add(rotCheck,BorderLayout.EAST);
            PropPanel.addCouple(p,REFROTATE, pr, g,c );
         }
         sRoll = pf.getRoll();
         rollField = new JTextField(sRoll, 4);
         if( !pf.isRollable () ) rollField.setEnabled(false);
         PropPanel.addCouple(p,ANGLE, rollField, g,c );

         // button to retrieve pointing centers
         if (pf.isAlmaFP()) {
            JButton bPointing = new JButton(POINTINGLABEL);
            bPointing.addActionListener(this);
            PropPanel.addCouple(p, POINTING, bPointing, g, c);
         }

         // panel with sub-FoVs
         JPanel subFoV = pf.getPanelSubFov(this);
         if( subFoV!=null ) PropPanel.addCouple(p,COMPONENT,subFoV,g,c);
      } else centerField=null;

      // Dans le cas d'un catalogue nombre d'items
      if( plan.isSimpleCatalog() && !plan.hasNoPos) {
         PropPanel.addCouple(p,SOURCE+":",new JLabel(""+((PlanCatalog)plan).getCounts()), g,c);
      }

      // Plan MultiExtension
      // Bouton de recuperation de visualisation du header FITS
      if( plan instanceof PlanFolder  && ((PlanFolder)plan).headerFits!=null ||
            plan instanceof PlanCatalog && ((PlanCatalog)plan).headerFits!=null ) {
         PropPanel.addCouple(p,"Fits extension", b=new JButton(SEEFITS), g,c);
         b.addActionListener(this);
      }

      // Info sur les images
      if( plan.isImage() || plan instanceof PlanBG   ) {
         final PlanImage pimg = (PlanImage)plan;

         // Survey de l'image
         String survey=pimg.survey();
         if( survey!=null && survey.length()>0 )
            PropPanel.addCouple(p,INF, new JLabel(pimg.survey()), g,c);

         // Format d'image
         JLabel fmtl = new JLabel(
               plan instanceof PlanHealpix ? "HEALPix map" :
               plan instanceof PlanSTMoc ?   "Space Time Multi-Order Coverage map (STMOC)" :
               plan instanceof PlanTMoc ?    "Temporal Multi-Order Coverage map (TMOC)" :
               plan instanceof PlanMoc ?     "Spatial Multi-Order Coverage map (MOC)" :
               plan instanceof PlanBG ?      "Hierarchical Progressive Survey (HiPS)" :
                        PlanImage.describeFmtRes(pimg.dis,pimg.res));

         // Bouton de visualisation du header FITS
         if( pimg.headerFits!=null ) {
            JPanel fmtp = new JPanel();
            fmtp.setLayout(new FlowLayout(FlowLayout.LEFT));
            fmtp.add(fmtl);
            fmtp.add( b=new JButton(SEEFITS) );
            b.addActionListener(this);
            PropPanel.addCouple(p,FMT, fmtp, g,c);
            
            // Bouton de visualisation des properties HiPS
         } else if( pimg instanceof PlanBG && ((PlanBG)pimg).prop!=null ) {
            JPanel fmtp = new JPanel();
            fmtp.setLayout(new FlowLayout(FlowLayout.LEFT));
            fmtp.add(fmtl);
            fmtp.add( b=new JButton(SEEHIPSPROP) );
            b.addActionListener(this);
            PropPanel.addCouple(p,FMT, fmtp, g,c);
            
         } else PropPanel.addCouple(p,FMT, fmtl, g,c);


         if( pimg.isImage() ) {
            // Mode graphique
            PropPanel.addCouple(p,PIXMODE, new JLabel(pimg.getPixModeInfo()), g,c);

            // Info d'image
            if(  plan.flagOk && plan.projd!=null ) {
               String s = Coord.getUnit(plan.projd.c.GetResol()[0])+" x "+Coord.getUnit(plan.projd.c.GetResol()[1]);
               PropPanel.addCouple(p, "Pixel angular res.", new JLabel(s), g, c);

               double ep = plan.projd.c.GetEpoch();
               if( !Double.isNaN(ep) ) {
                  PropPanel.addCouple(p,EPOCH, new JLabel(Astrodate.JDToDate(Astrodate.YdToJD(ep))+" ("+ep+")"), g,c);
               } else {
                  String d = ((PlanImage)plan).getDateObs();
                  if( d!=null ) PropPanel.addCouple(p,DATEOBS, new JLabel(d), g,c);
               }
               double eq = plan.projd.c.GetEquinox();
               if( eq!=0.0 ) {
                  eq= (int)(eq*1000)/1000.0;
                  PropPanel.addCouple(p,WCSEQ, new JLabel(""+eq), g,c);

                  // On autorise la modification de l'equinoxe par defaut
               } else {
                  sEquinox="2000.0";	// IL FAUDRA FAIRE PLUS MALIN
                  eqField = new JTextField(sEquinox);
                  PropPanel.addCouple(p,WCSEQ, eqField, g,c);
               }
            }

            if( pimg.width!=0 && !(pimg instanceof PlanBG) ) PropPanel.addCouple(p,SIZE, new JLabel(pimg.getSizeInfo()), g,c);

         }
      }

      // Valeur BLANK alternative
      if( plan.hasAvailablePixels() ) {
         final PlanImage pimg = (PlanImage)plan;
         String vBlank = pimg.getBlankString();
         blankField = new TextField(vBlank);
         blankField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               actionBlank();
               blankField.setText( pimg.getBlankString() );
               aladin.calque.repaintAll();
            }
         });
         PropPanel.addCouple(p,"Transparency", blankField, g,c);
      }

      if( plan.isCube() ) {
         PropPanel.addCouple(p,FRAME,new JLabel(plan.getDepth()+""), g,c);
      }
      if( plan instanceof PlanImageBlink ) {
         final PlanImageBlink pb=(PlanImageBlink)plan;
         JPanel panel = new JPanel(new FlowLayout() );
         JButton perm = new JButton("XxY->Z");
         perm.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               ((JButton)e.getSource()).setEnabled(false);
               pb.permutation(PlanImageBlink.PERM0);
            }
         });
         perm.setEnabled(pb.getPermutation()!=PlanImageBlink.PERM0);
         panel.add(perm);
         perm = new JButton("XxZ->Y");
         perm.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               ((JButton)e.getSource()).setEnabled(false);
               pb.permutation(PlanImageBlink.PERM1);
            }
         });
         perm.setEnabled(pb.getPermutation()!=PlanImageBlink.PERM1);
         panel.add(perm);
         perm = new JButton("ZxY->X");
         perm.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               ((JButton)e.getSource()).setEnabled(false);
               pb.permutation(PlanImageBlink.PERM2);
            }
         });
         perm.setEnabled(pb.getPermutation()!=PlanImageBlink.PERM2);
         panel.add(perm);
         PropPanel.addCouple(p,"Permutations",panel, g,c);

      }


      // Url de déchargement
      String s1 = plan.getUrl();
      if( s1!=null && (s1.startsWith("http://") || s1.startsWith("https://") || s1.startsWith("ftp://") )) {
         JComponent a = new MyAnchor(aladin,s1,50,null,s1);
         if( plan.getMirrorsUrl()!=null ) {
            JButtonTriangle b1 = new JButtonTriangle();
            JPanel p1 = new JPanel( new FlowLayout(FlowLayout.LEFT, 4, 0));
            p1.add(a);
            p1.add(b1);
            a=p1;
         } 
         PropPanel.addCouple(p,"Url: ", a , g,c);
      }

      // Panel pour les informations techniques
      //      if( plan.hasRemoteUrl() || (plan.isSimpleCatalog() || plan.type==Plan.FOLDER) ) {
      //
      //         JPanel panelInfo = new JPanel();
      //         panelInfo.setBorder(BorderFactory.createEmptyBorder());
      //         panelInfo.setLayout(new FlowLayout(FlowLayout.CENTER));
      //         if( plan.getUrl()!=null ) {
      //            panelInfo.add(b=new JButton(LOADURL));
      //            b.addActionListener(this);
      //         }
      //         c.fill = GridBagConstraints.NONE;
      //         PropPanel.addCouple(p,"",panelInfo, g,c);
      //         c.fill = GridBagConstraints.BOTH;
      //      }

      if( plan.isCatalog() ) {
         Vector<Legende> legs = plan.getLegende();
         b=new JButton(SEEPARSING);
         b.addActionListener(this);
         int n = legs.size();
         if( n==1 ) PropPanel.addCouple(p,TABLEINFO, b, g,c);
         else {
            StringBuffer s = new StringBuffer("<html>");
            for( int i=0; i<legs.size(); i++ ) s.append((i>0?"<br>":"")+(i+1)+": "+legs.elementAt(i).name);
            s.append("</html>");
            PropPanel.addCouple(p,TABLEINFO, new JLabel(s.toString()), g,c);
            PropPanel.addCouple(p,"", b, g,c);
         }
      }

      // Gestion des filtres prédéfinis
      if( plan.filters!=null ) {
         PropPanel.addFilet(p,g,c);
         JLabel l = new JLabel(FILTER);
         l.setFont(l.getFont().deriveFont(Font.BOLD));
         c.fill = GridBagConstraints.NONE;
         PropPanel.addCouple(p,l,getPanelFilter(plan),g,c);
         c.fill = GridBagConstraints.BOTH;

         toGenFilterButton = b=new JButton("This filter on the stack for editing");
         b.addActionListener(this);
         /* if( !Aladin.OUTREACH ) */PropPanel.addCouple(p,"",toGenFilterButton,g,c);
         toGenFilterButton.setEnabled(plan.hasDedicatedFilter());

      }

      // champ à afficher (PlanHealpix)
      if( plan instanceof PlanHealpix ) {
         final PlanHealpix ph = (PlanHealpix)plan;

         if ( ph.type==PlanBG.ALLSKYPOL ) {
            PropPanel.addFilet(p,g,c);
            PropPanel.addSectionTitle(p,POLAOPTIONS,g,c);

            polaSegmentLen = new JSlider(0, 200);
            polaSegmentLen.setValue((int)ph.getSegmentLenFactor()*200);
            polaSegmentLen.setPaintTicks(true);
            polaSegmentLen.addChangeListener(this);
            PropPanel.addCouple(p, SEGMENTLEN, polaSegmentLen, g, c);

            polaSegmentThickness = new JSlider(1, 5);
            polaSegmentThickness.setValue(ph.getSegmentThickness());
            //              System.out.println(ph.getSegmentThickness());
            polaSegmentThickness.setMajorTickSpacing(1);
            polaSegmentThickness.setPaintTicks(true);
            polaSegmentThickness.setSnapToTicks(true);
            polaSegmentThickness.setPaintLabels(false);
            polaSegmentThickness.addChangeListener(this);
            PropPanel.addCouple(p, SEGMENTTHICK, polaSegmentThickness, g, c);

            polaSegmentDensity = new JSlider(0, 200);
            polaSegmentDensity.setValue(100);
            polaSegmentDensity.setMajorTickSpacing(25);
            polaSegmentDensity.setPaintTicks(true);
            polaSegmentDensity.setSnapToTicks(true);
            polaSegmentDensity.addChangeListener(this);
            PropPanel.addCouple(p, SEGMENTDENSITY, polaSegmentDensity, g, c);
         }
         else {
            if (ph.tfieldNames!=null && ph.idxTFormToRead>=0 ) {
               // nom du field affiché
               PropPanel.addFilet(p, g, c);
               PropPanel.addCouple(p, CURRENTFIELD, new JLabel(ph.tfieldNames[ph.idxTFormToRead]), g, c);
            }
            
            boolean hasPolarisationData = ph.hasPolarisationData();

            if( hasPolarisationData || ph.tfieldNames.length>1 ) {
               PropPanel.addFilet(p, g, c);
            }
            // bouton pour demander affichage de la polarisation
            if (hasPolarisationData) {
               JPanel pPola = new JPanel(new GridLayout(0, 1));
               btnDisplayPola = new JButton(DISPLAYPOLA);
               btnDisplayPola.addActionListener(this);
               pPola.add(btnDisplayPola);

               btnDisplayAmp = new JButton(GENERATEPOLAAMP);
               btnDisplayAmp.addActionListener(this);
               pPola.add(btnDisplayAmp);

               btnDisplayAng = new JButton(GENERATEPOLAANG);
               btnDisplayAng.addActionListener(this);
               pPola.add(btnDisplayAng);

               c.fill = GridBagConstraints.NONE;
               PropPanel.addCouple(null, p, POLA, null, pPola, g, c, GridBagConstraints.NORTHWEST);
            }

            // affichage autres champs disponibles
            if (ph.tfieldNames.length > 1) {
               JPanel pAvailableFields = new JPanel(new GridLayout(0, 1));
               JScrollPane scrollPane = new JScrollPane(pAvailableFields, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
               scrollPane.setPreferredSize(new Dimension(200, 300));

               for (int i = 0; i < ph.tfieldNames.length; i++) {
                  if (i == ph.idxTFormToRead) {
                     continue;
                  }
                  pAvailableFields
                  .add(b = new JButton(ph.tfieldNames[i]));
                  final int idx = i;
                  b.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                        ph.loadNewField(idx);
                     }
                  });
               }
               PropPanel.addCouple(null, p, SELECTFIELD, null, pAvailableFields, g, c, GridBagConstraints.NORTHWEST);
            }
         }
      }

      if( plan.type==Plan.ALLSKYIMG ) {
         final PlanBG pbg = (PlanBG) plan;
         PropPanel.addFilet(p, g, c);
         long res = pbg.getMaxHealpixOrder();
         long ord = pbg.getTileOrder();
         PropPanel.addSectionTitle(p, "HiPS properties", g, c);
         PropPanel.addCouple(p, "Best pixel resolution", new JLabel(pbg.getMaxResolution()), g, c);
         PropPanel.addCouple(p, "HEALPix NSide:",  new JLabel(CDSHealpix.pow2(res)+" (2^"+res+")"), g, c);
      }

      if( plan.isMoc() ) {
         final PlanMoc pmoc = (PlanMoc)plan;
//         final Frame frameProp = this;
         
         if( plan.type==Plan.ALLSKYMOC ) {
            double cov = ((SpaceMoc)pmoc.getMoc()).getCoverage();
            double degrad = Math.toDegrees(1.0);
            double skyArea = 4.*Math.PI*degrad*degrad;
            PropPanel.addCouple(p,"Coverage: ",new JLabel(Util.round(cov*100, 3)+"% of sky => "+Coord.getUnit(skyArea*cov, false, true)+"^2"),g,c);
            PropPanel.addCouple(p,"Best ang.res: ",new JLabel(Coord.getUnit(((SpaceMoc)pmoc.getMoc()).getAngularRes())
                  +" (order="+pmoc.getMoc().getMocOrder()+")"),g,c);
         }

         else if( plan.type==Plan.ALLSKYTMOC ) {
            TimeMoc moc = (TimeMoc) ((PlanTMoc)plan).moc;
            long nbSec = moc.getUsedArea();
            int order = moc.getMocOrder();
            PropPanel.addCouple(p,"Start", new JLabel( Astrodate.JDToDate( moc.getTimeMin()) ), g,c);
            PropPanel.addCouple(p,"End", new JLabel( Astrodate.JDToDate( moc.getTimeMax()) ), g,c);
            PropPanel.addCouple(p,"Accuracy", new JLabel( Util.getTemps(  1000*( 1<<(2*(HealpixMoc.MAXORDER-order))))+" (order="+order+")" ), g,c);
            PropPanel.addCouple(p,"Sum", new JLabel( Util.getTemps(nbSec*1000000, true) ), g,c);
         }

         else if( plan.type==Plan.ALLSKYSTMOC ) {
            try {
               SpaceMoc spaceMoc =  ((SpaceTimeMoc) ((PlanMoc)plan).moc).getSpaceMoc();
               double cov = spaceMoc.getCoverage();
               double degrad = Math.toDegrees(1.0);
               double skyArea = 4.*Math.PI*degrad*degrad;
               PropPanel.addCouple(p,"Coverage: ",new JLabel(Util.round(cov*100, 3)+"% of sky => "+Coord.getUnit(skyArea*cov, false, true)+"^2"),g,c);
               PropPanel.addCouple(p,"Best ang.res: ",new JLabel(Coord.getUnit(spaceMoc.getAngularRes())
                     +" (order="+spaceMoc.getMocOrder()+")"),g,c);

               TimeMoc timeMoc =  ((SpaceTimeMoc) ((PlanMoc)plan).moc).getTimeMoc();
               long nbSec = timeMoc.getUsedArea();
               int order = timeMoc.getMocOrder();
               PropPanel.addCouple(p,"Start", new JLabel( Astrodate.JDToDate( timeMoc.getTimeMin()) ), g,c);
               PropPanel.addCouple(p,"End", new JLabel( Astrodate.JDToDate( timeMoc.getTimeMax()) ), g,c);
               PropPanel.addCouple(p,"Time Accuracy", new JLabel( Util.getTemps(  1000*( 1<<(2*(HealpixMoc.MAXORDER-order))))+" (order="+order+")" ), g,c);
               
            } catch( Exception e1 ) {
               if( aladin.levelTrace>=3 ) e1.printStackTrace();
            }
         }


         final JCheckBox b1 = new JCheckBox("cell borders");
         final JCheckBox b3 = new JCheckBox("perimeter");
         
         b1.setSelected( pmoc.isDrawingBorder() );
         b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               boolean a = b1.isSelected();
               pmoc.setDrawingBorder(a);
               if( a ) {
                  pmoc.setDrawingPerimeter(!a);
                  b3.setSelected(!a);
                  aladin.console.printCommand("set drawing=+border,-perimeter");
               } else aladin.console.printCommand("set drawing=-border");
              aladin.calque.repaintAll();
            }
         });
         b3.setSelected( pmoc.isDrawingPerimeter() );
         b3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               boolean a = b3.isSelected();
               pmoc.setDrawingPerimeter(a);
               if( a ) {
                  pmoc.setDrawingBorder(!a);
                  b1.setSelected(!a);
                  aladin.console.printCommand("set drawing=+perimeter,-border");
               } else aladin.console.printCommand("set drawing=-perimeter");
               aladin.calque.repaintAll();
            }
         });
         
         final JCheckBox b2a = new JCheckBox("fill in");
         b2a.setSelected( pmoc.isDrawingFillIn() );
         b2a.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               boolean a=b2a.isSelected();
               pmoc.setDrawingFillIn(a);
               aladin.calque.repaintAll();
               aladin.console.printCommand("set drawing="+(a?"+":"-")+"fill");
            }
         });
         JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         p1.add(b1); p1.add(b3);
         p1.add(b1); p1.add(b2a);
         PropPanel.addCouple(p,"Drawing method: ",p1,g,c);

      }

      //      // Accès au MOC des catalogues VizieR
      //      if( Aladin.PROTO &&
      //            (plan.server instanceof ServerVizieR || plan.server instanceof ServerVizieRMission) ) {
      //         JPanel p1 = new JPanel();
      //         final String cat = ((PlanCatalog)plan).getFirstTableName();
      //         JButton bt = new JButton("MOC");
      //         bt.addActionListener(new ActionListener() {
      //            public void actionPerformed(ActionEvent e) {
      //               ((ServerVizieR)aladin.dialog.server[ServerDialog.VIZIER]).createMocPlane(cat);
      //            }
      //         });
      //         p1.add(bt);
      //         PropPanel.addCouple(p,cat+" coverage map",p1,g,c);
      //      }

      if( plan instanceof PlanBG && !(plan instanceof PlanTMoc) ) {
         final PlanBG pbg = (PlanBG) plan;
         PropPanel.addCouple(p, "Coord.sys.:", new JLabel(pbg.getHipsFrame()), g, c);

         if( plan.type==Plan.ALLSKYIMG ) {
            int level = pbg.getMaxFileOrder();
            PropPanel.addCouple(p, "Number of levels", new JLabel(level+""), g, c);
            
            long ord = pbg.getTileOrder();
            PropPanel.addCouple(p, "Tile format", new JLabel(pbg.getFormat()), g, c);
            if( ord>0 ) PropPanel.addCouple(p, "Tile width:",  new JLabel((int)CDSHealpix.pow2(ord)+" pix (2^"+ord+")"), g, c);
            if( pbg.inFits && (pbg.inJPEG || pbg.inPNG) ) {
               JButton bt = new JButton( pbg.isTruePixels() ? aladin.chaine.getString("ALLSKYSWJPEG") : aladin.chaine.getString("ALLSKYSWFITS") );
               bt.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     pbg.switchFormat();
                     showProp(true);
                     aladin.view.repaintAll();

                  }
               } );
               PropPanel.addCouple(p,"",bt, g, c);
            }
         }

         String s = Aladin.BETA ? pbg.getNetSpeed() : null;
         if( s!=null ) {
            JLabel lab = new JLabel(s);
            if( s.indexOf("error")>=0 ) lab.setForeground(Color.red);
            PropPanel.addCouple(p, "Avg net speed:", lab, g, c);
         }


         //         if( pbg.hasMoc() || pbg.hasHpxFinder() ) {
         //            JPanel p1 = new JPanel();
         //            if( pbg.hasMoc() ) {
         //               JButton bt = new JButton(aladin.MOC);
         //               bt.addActionListener(new ActionListener() {
         //                  public void actionPerformed(ActionEvent e) { pbg.loadMoc(); }
         //               });
         //               p1.add(bt);
         //            }
         //            if( pbg.hasHpxFinder() ) {
         //               JButton bt = new JButton(aladin.chaine.getString("PROGENITOR"));
         //               bt.addActionListener(new ActionListener() {
         //                  public void actionPerformed(ActionEvent e) {
         //                     pbg.loadProgen();
         //                  }
         //               });
         //               p1.add(bt);
         //            }
         //            PropPanel.addCouple(p,"More info",p1,g,c);
         //         }

         // Description de l'origine des données
         String sTmin = pbg.getProperty(Constante.KEY_T_MIN);
         String sTmax = pbg.getProperty(Constante.KEY_T_MAX);
         String sEMmin = pbg.getProperty(Constante.KEY_EM_MIN);
         String sEMmax = pbg.getProperty(Constante.KEY_EM_MAX);
         String sR = pbg.getProperty(Constante.KEY_OBS_REGIME);
         String sS  = pbg.getProperty(Constante.KEY_MOC_SKY_FRACTION);
         boolean hasMoc = pbg.hasMoc();
         if( sS!=null || sTmin!=null || sTmax!=null || sEMmin!=null || sEMmax!=null || sR!=null || sS!=null || hasMoc ) {
            PropPanel.addFilet(p, g, c);
            PropPanel.addSectionTitle(p,"Coverage",g,c);

            if( sTmin!=null  || sTmax!=null )  PropPanel.addCouple(p,"Time range", new JLabel( pbg.getCoverageTime(sTmin,sTmax)), g,c);
            if( sEMmin!=null || sEMmax!=null ) PropPanel.addCouple(p,"Energy range", new JLabel( pbg.getCoverageEnergy(sEMmin,sEMmax)), g,c);
            if( hasMoc || sS!=null ) {
               if( sS==null && pbg.moc!=null ) sS = pbg.moc.getCoverage()+"";
               JPanel p1 = new JPanel();
               if( sS!=null ) {
                  try { sS = Util.myRound( Double.parseDouble(sS)*100); } catch( Exception e) {}
                  p1.add( new JLabel(sS+" % of sky"));
               }
               if( hasMoc ) {
                  JButton bt = new JButton(aladin.MOC);
                  bt.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                        pbg.loadMoc();
                     }
                  });
                  p1.add(bt);
               }
               PropPanel.addCouple(p,"Space",p1,g,c);
            }
         }


         // Description de l'origine des données
         String sP  = pbg.getProperty(Constante.KEY_PROV_PROGENITOR);
         String sC  = pbg.getProperty(Constante.KEY_OBS_COPYRIGHT);
         String sCU = pbg.getProperty(Constante.KEY_OBS_COPYRIGHT_URL);
         boolean hasProgen = pbg.hasHpxFinder();
         if( sP!=null || sC!=null || sCU!=null || hasProgen ) {
            PropPanel.addFilet(p, g, c);
            PropPanel.addSectionTitle(p,"Original data",g,c);

            if( sP!=null ) PropPanel.addCouple(p,"Provenance", new MyAnchor(aladin,null,40,sP,null), g,c);
            if( sC!=null || sCU!=null ) PropPanel.addCouple(p,"Copyright", new MyAnchor(aladin,sC,40,null,sCU), g,c);
            if( hasProgen ) {
               JButton bt = new JButton(aladin.chaine.getString("PROGENITOR"));
               bt.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     pbg.loadProgen();
                  }
               });
               JPanel p1 = new JPanel();
               p1.add(bt);
               PropPanel.addCouple(p,"Access",p1,g,c);
            }
         }
      }

      if( plan.flagOk && (plan.isSimpleCatalog() || plan instanceof PlanBG) ) {

         JPanel p1 = aladin.view.getPlotControlPanelForPlan(plan);
         if( p1!=null ) {
            PropPanel.addFilet(p,g,c);
            PropPanel.addCouple(p,"Scatter plot", p1, g,c);
         }

         // Les projections possibles par défaut s'il n'y a pas d'images dessous ou si PlanBG
         else if( plan.ref && Projection.isOk(plan.projd) )  {
            specificProj = new JComboBox();
            int index=-1;
            
            // Dans le cas dans PlanBG, on ajoute l'item Default en position 0
            if( plan instanceof PlanBG ) {
               if( !plan.hasSpecificProj() ) index=0;
               else index= 1+Projection.getAlaProjIndex( Calib.getProjName(plan.projd.c.getProj()) );
               specificProj.addItem("Default");
            } else {
               index= Projection.getAlaProjIndex( Calib.getProjName(plan.projd.c.getProj()) );
            }
            for( String s: Projection.getAlaProj() ) specificProj.addItem( s );
            
            //            String pr[]=Projection.getAlaProj();
            //            for( int i=0; i<pr.length; i++ ) defCatProj.addItem(pr[i]);
            //            defCatProj.setSelectedIndex(plan.projd.c.getProjSys()-1);
            
            specificProj.setSelectedIndex( index );
            specificProj.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { actionSpecificProj(); }
            });

            PropPanel.addFilet(p,g,c);
            PropPanel.addSectionTitle(p,DEFCATPROJ,g,c);
            if( !(plan instanceof PlanBG) ) PropPanel.addCouple(p,CENTER, new JLabel(plan.projd.c.getProjCenter().getSexa()), g,c);
            PropPanel.addCouple(p,METHOD, specificProj, g,c);
            
            if( plan.ref && plan instanceof PlanBG ) {
               
               specificFrame = Localisation.createFrameCombo();
               specificFrame.setSelectedIndex( ((PlanBG)plan).getFrameDrawing() );
               specificFrame.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) { actionSpecificFrame(); }
               });
               PropPanel.addCouple(p,".frame", specificFrame, g,c);
               
               JPanel plong = new JPanel();
               plong.setBorder( BorderFactory.createEmptyBorder());
               longitude = new ButtonGroup();
               longitudeDescending = new JRadioButton("descending");
               longitudeDescending.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     ((PlanBG)plan).setSpecificProj(true);
                     plan.projd.setProjSym( !plan.projd.sym );
                     aladin.view.newView();
                     actionSpecificProj();
                     aladin.calque.repaintAll();
                  }
               });
               longitude.add(longitudeDescending);
               longitudeAscending = new JRadioButton("ascending");
               longitudeAscending.addActionListener( new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     ((PlanBG)plan).setSpecificProj(true);
                     plan.projd.setProjSym( !plan.projd.sym );
                     aladin.view.newView();
                     actionSpecificProj();
                     aladin.calque.repaintAll();
                  }
               });

               longitude.add(longitudeAscending); 
               if( plan.projd.sym ) longitudeAscending.setSelected(true);
               else  longitudeDescending.setSelected(true);
               plong.add( longitudeAscending );
               plong.add( longitudeDescending );
               if( Aladin.BETA ) PropPanel.addCouple(p,".longitude", plong, g,c);

            }
         }
      }


      boolean filet=false;

      if( plan.isCatalog() && !plan.hasNoPos ) {

         // Epoque pour un catalogue
         if( plan.flagOk && plan.projd!=null && plan.hasPM() ) {

            if( !filet ) PropPanel.addFilet(p, g, c); filet=false;

            String sEpoch = plan.getEpoch().toString("J");
            JPanel pEpoch = new JPanel();
            epField = new JTextField(sEpoch,10);
            epField.addKeyListener(new KeyListener() {
               public void keyTyped(KeyEvent arg0) { }
               public void keyReleased(KeyEvent arg0) {
                  if( arg0.getKeyCode()==KeyEvent.VK_ENTER ) apply();
               }
               public void keyPressed(KeyEvent arg0) { }
            });
            pEpoch.add(epField);

            b = new JButton("Img epoch");
            Plan pi = aladin.calque.getPlanBase();
            b.setEnabled( pi instanceof PlanImage && ((PlanImage)pi).getDateObs()!=null );

            Insets m = b.getMargin();
            b.setMargin(new Insets(m.top,3,m.bottom,3));
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent arg0) {
                  try {
                     Plan pi = aladin.calque.getPlanBase();
                     if( !(pi instanceof PlanImage) ) return;
                     Astrotime t = new Astrotime();
                     t.set( ((PlanImage)pi).getDateObs() );
                     System.out.println("Epoch="+t+" => "+t.toString("J"));
                     epField.setText(t.toString("J"));
                     apply();
                  } catch( ParseException e ) {
                     e.printStackTrace();
                  }
               }
            });
            pEpoch.add(b);

            b = new JButton("Reset");
            m = b.getMargin();
            b.setMargin(new Insets(m.top,3,m.bottom,3));
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent arg0) {
                  epField.setText("J2000");
                  apply();
               }
            });
            pEpoch.add(b);
            PropPanel.addCouple(p,EPOCH, pEpoch, g,c);

            double y=2000;
            try { y = Double.parseDouble(sEpoch.substring(1));
            } catch( NumberFormatException e ) { }
            epochSlider = new JSlider(1700, 2300);
            epochSlider.setMinimumSize(epochSlider.getPreferredSize());
            epochSlider.setValue((int)y);
            epochSlider.setMajorTickSpacing(100);
            epochSlider.setPaintLabels(true);
            epochSlider.setPaintTicks(true);
            epochSlider.setPaintTrack(true);
            epochSlider.addChangeListener(this);
            PropPanel.addCouple(p, "", epochSlider, g, c);
         }

         if( !filet ) PropPanel.addFilet(p, g, c); filet=true;
         scalingFactor = new JSlider(0, 300);
         scalingFactor.setMinimumSize(scalingFactor.getPreferredSize());
         scalingFactor.setValue((int)plan.getScalingFactor()*100);
         scalingFactor.setMajorTickSpacing(50);
         scalingFactor.setPaintLabels(true);
         scalingFactor.setPaintTicks(true);
         scalingFactor.setPaintTrack(true);
         scalingFactor.addChangeListener(this);
         PropPanel.addCouple(p, SCALINGFACTOR, scalingFactor, g, c);
      }


      // niveau d'opacité des images et des footprints
      if( aladin.calque.canBeTransparent(plan) ) {
         if( !filet ) PropPanel.addFilet(p, g, c); filet=true;
         JPanel pTransp = new JPanel(new FlowLayout());
         opacityLevel = new JSlider(0, 100);
         opacityLevel.setValue((int)(100*plan.getOpacityLevel()));
         opacityLevel.setMajorTickSpacing(20);
         opacityLevel.setPaintLabels(true);
         opacityLevel.setPaintTicks(true);
         opacityLevel.setPaintTrack(true);
         opacityLevel.setToolTipText(OPACITYLEVEL+" : "+opacityLevel.getValue());
         opacityLevel.addChangeListener(this);
         pTransp.add(opacityLevel);
         PropPanel.addCouple(p,OPACITY, pTransp, g,c);
      }

      // Ajustement de l'ordre max
      if( plan instanceof PlanBGCat && !(plan instanceof PlanMoc)) {
         if( !filet ) PropPanel.addFilet(p, g, c); filet=true;
         JPanel pGapOrder = new JPanel(new FlowLayout());
         gapOrder = new JSlider(-PlanBGCat.MAXGAPORDER, PlanBGCat.MAXGAPORDER);
         gapOrder.setValue(((PlanBGCat)plan).getGapOrder());
         gapOrder.setMajorTickSpacing(1);
         gapOrder.setPaintLabels(true);
         gapOrder.setPaintTicks(true);
         gapOrder.setPaintTrack(true);
         gapOrder.addChangeListener(this);
         pGapOrder.add(gapOrder);
         PropPanel.addCouple(p,DENSITY, pGapOrder, g,c);
      }

      // Propriété de déplacement des objets du plan
      if( plan instanceof PlanTool && !(plan instanceof PlanContour) ) {
         JRadioButton r;
         cb = new ButtonGroup();
         JPanel bg = new JPanel();
         r = new JRadioButton(ON); r.setActionCommand(ON); cb.add(r);
         r.setSelected(plan.isMovable());
         r.addActionListener(this);
         bg.add(r);
         r = new JRadioButton(OFF); r.setActionCommand(OFF); cb.add(r);
         r.setSelected(!plan.isMovable());
         r.addActionListener(this);
         bg.add(r);
         PropPanel.addCouple(p,"Movable:",bg, g,c);
      }

      // Couleur de fond pour une image couleur
      if( plan.ref && !(plan instanceof PlanBG) ) {
         JRadioButton r;
         cb = new ButtonGroup();
         JPanel bg = new JPanel();
         r = new JRadioButton(WHITE); r.setActionCommand(WHITE); cb.add(r);
         r.setSelected(plan.colorBackground==Color.white ||
               (plan instanceof PlanImageRGB && plan.colorBackground==null) );
         r.addActionListener(this);
         bg.add(r);
         r = new JRadioButton(BLACK); r.setActionCommand(BLACK); cb.add(r);
         r.setSelected(plan.colorBackground==Color.black );
         r.addActionListener(this);
         bg.add(r);
         if( plan.type!=Plan.IMAGERGB ) {
            r = new JRadioButton(AUTO); r.setActionCommand(AUTO); cb.add(r);
            r.setSelected(plan.colorBackground==null );
            r.addActionListener(this);
            bg.add(r);
         }
         PropPanel.addCouple(p,COLORBG,bg, g,c);
      }

      // Gestion du plan de reference et de la projection associee
      // DANS LE CAS D'UN CATALOGUE, JE N'AFFICHE LE CHOICE QUE DANS LE CAS D'UNE
      // RECALIBRATION XY OU S'IL N'Y A PAS D'IMAGE EN DESSOUS
      if( plan.flagOk &&
            (plan.isImage() || (plan.isSimpleCatalog() && plan.hasXYorig)) ) {

         PropPanel.addFilet(p,g,c);
         String s = plan.isImage()?ASTRED:
            plan.hasNoPos?"No position":plan.hasXYorig?XYRED:PROJ;
         PropPanel.addSectionTitle(p,s,g,c);

         // Les plans de reference possibles pour le plan
         planRefChoice=null;
         majPlanRef();

         // Les projections possibles pour le plan de reference courant
         projsChoice=null;
         JButton n = b=new JButton(NEWCALIB);  b.addActionListener(this);
         modCalib = b=new JButton(MODCALIB);   b.addActionListener(this);
         majProjs();
         if( projsChoice.getItemCount()==0 ) {
            projsChoice.addItem(NONE);
         }
         JPanel projsP = new JPanel();
         projsP.add(projsChoice);
         projsP.add(n);
         projsP.add(modCalib);
         PropPanel.addCouple(p,"   "+METHOD, projsP, g,c);
      }

      // boutons pour montrer cacher les FoVs associés à un Plan.CATALOG
      if( plan.flagOk && plan.isCatalog() && plan.hasAssociatedFootprints() ) {
         PropPanel.addFilet(p,g,c);
         PropPanel.addSectionTitle(p,ASSFOV,g,c);

         JPanel p1 = new JPanel(new GridLayout(0,1));

         JButton b1 = b=new JButton(SHOWFOVS);
         b.addActionListener(this);
         p1.add(b1);

         JButton b2 = b=new JButton(HIDEFOVS);
         b.addActionListener(this);
         p1.add(b2);

         c.gridwidth = GridBagConstraints.REMAINDER;
         c.weightx = 0.0;
         c.fill = GridBagConstraints.NONE;
         c.anchor = GridBagConstraints.CENTER;
         g.setConstraints(p1,c);
         p.add(p1);

         c.anchor = GridBagConstraints.WEST;
         c.weightx = 1.0;
         c.fill = GridBagConstraints.BOTH;

      }

      return p;
   }
   
   // Création et affichage d'un popup présentant les URLs alternatives pour les mirroirs */
   private void showPopupMirrors( MouseEvent e, final PlanBG plan ) {
      ArrayList<String> a = plan.getMirrorsUrl();
      if( a==null ) return;
      JPopupMenu popup = new JPopupMenu();
      for( String s: a ) {
         JMenuItem mi = null;
         mi = new JMenuItem( Util.getUrlHost(s)+"/..." );
         mi.setActionCommand(s);
         mi.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               String s = ((JMenuItem)e.getSource()).getActionCommand();
               if( aladin.glu.setIndirectionByUrl(plan.gluTag, s) ) {
                  plan.url = s;
                  plan.resetStats();
                  showProp(true);
               }
            }
         });
         popup.add(mi);
      }
      ((JComponent)e.getSource()).setComponentPopupMenu(popup);
      popup.show((JComponent)e.getSource(), e.getX(), e.getY());
   }
   
   // Bouton en forme de triangle pour afficher les mirroirs alternatifs
   class JButtonTriangle extends JPanel implements MouseListener {
      boolean in=false;
      JButtonTriangle() { super(); addMouseListener(this);}
      public Dimension getPreferredSize() { return new Dimension(15,15); }
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         g.setColor( in ? Color.black : Color.darkGray.brighter() );
         Util.fillTriangle7(g, 5, 5);
      }
      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { }
      public void mouseReleased(MouseEvent e) { showPopupMirrors(e, (PlanBG)plan); in=false; }
      public void mouseEntered(MouseEvent e) { in=true; repaint(); }
      public void mouseExited(MouseEvent e) { in=false; repaint(); }
   }


//   private String getCoverageTime(String s1,String s2) {
//      return "[ "+ (s1==null ? " " : Util.getDateFromMJD(s1)) + " .. "+ (s2==null ? " " : Util.getDateFromMJD(s2)) + " ]";
//   }
//
//   private String getCoverageEnergy(String s1,String s2) {
//      return "[ "+ (s1==null ? " " : Util.getWaveFromMeter(s1)) + " .. "+ (s2==null ? " " : Util.getWaveFromMeter(s2)) + " ]";
//   }

   // implementation d'EventListener pour les differents sliders
   public void stateChanged(ChangeEvent e) {
      Object src = e.getSource();

      // modification du niveau de transparence
      if (src == opacityLevel) {
         float level = (float) (opacityLevel.getValue() / 100.0);
         plan.setOpacityLevel(level);
         opacityLevel.setToolTipText(OPACITYLEVEL + " : "  + (int) (level * 100));
         aladin.calque.repaintAll();
      }
      // Modification de la correction de densité pour un catalogue progressif
      else if( src == gapOrder ) {
         int v = gapOrder.getValue();
         ((PlanBGCat)plan).setGapOrder(v);
         aladin.calque.repaintAll();
      }
      // modification de la taille des segments de polarisation
      else if (src==polaSegmentLen) {
         float factor = (float)(polaSegmentLen.getValue()/100.0);
         ((PlanBG)plan).setSegmentLenFactor(factor);
         aladin.calque.repaintAll();
      }
      // modification de l'épaisseur des segments de polarisation
      else if (src==polaSegmentThickness) {
         int thickness = polaSegmentThickness.getValue();
         int othickness = ((PlanBG)plan).getSegmentThickness();
         if (thickness==othickness) {
            return;
         }

         ((PlanBG)plan).setSegmentThickness(thickness);
         aladin.calque.repaintAll();
      }
      // modification du nombre de segments de polarisation
      else if (src==polaSegmentDensity) {
         float factor = (float) ((200-polaSegmentDensity.getValue()) / 100.0);
         ((PlanBG)plan).setSegmentDensityFactor(factor);
         aladin.calque.repaintAll();
      }
      // modification du scaling factor
      else if (src==scalingFactor) {
         float factor = (float)(scalingFactor.getValue()/100.0);
         plan.setScalingFactor(factor);
         aladin.calque.repaintAll();

         // modification de l'époque
      } else if (src==epochSlider) {
         try {
            plan.setEpoch(epochSlider.getValue()+"");
            epField.setText( plan.getEpoch().toString("J"));
            aladin.calque.repaintAll();
         } catch( Exception e1 ) { e1.printStackTrace(); }
      }
   }

   // Mise-a-jour des plans de reference possibles pour le plan
   // maj de planRef[] et planRefChoice
   private void majPlanRef() {
      if( !plan.isCatalog() && !plan.isImage() ) return;

      int j=0;
      planRef = plan.getAvailablePlanRef();
      if( planRefChoice==null ) planRefChoice = new JComboBox();
      planRefChoice.removeAll();
      if( planRef.length>0 ) {
         for( int i=0; i<planRef.length; i++ ) {
            String s = Plan.Tp[planRef[i].type]+" \""+planRef[i].label+"\"";
            planRefChoice.addItem(s);
            if( planRef[i].ref ) j=i;
         }
         planRefChoice.setSelectedIndex(j);
      }
   }

   protected void majProjInitCat() {
      if( !plan.isCatalog() ) return;

   }

   // Mise-a-jour des projections possibles pour le plan de reference courant
   // maj de projs[] et projsChoice
   //
   // ATTENTION: dans le cas d'un plan catalogue n'ayant que des XY,
   // les elements projs[] et son alterego projsChoice concernent les projections
   // qui permettent de calculer les alpha,delta des objets en fonctions des
   // XY originaux du catalogue. En revanche, dans le cas d'un plan catalogue
   // normal, projs[] et projsChoice memorise les projections possibles du
   // plan de reference courant.
   protected void majProjs() {
      if( !plan.isCatalog() && !plan.isImage() || planRef==null ) return;

      int j = 0;             // Index de la projection courante
      Plan pref = null;

      // Cas d'un catalogue n'ayant que des XY
      if( plan.hasXYorig ) projs = plan.getAvailableProj();
      else {
         if( planRefChoice.getSelectedIndex() >= 0 ) {
            pref = planRef[planRefChoice.getSelectedIndex()];
            projs = pref.getAvailableProj();
         }
      }
      if( projsChoice == null ) projsChoice = new JComboBox();
      projsChoice.removeAllItems();

      if( projs != null && projs.length > 0 ) {
         for( int i = 0; i < projs.length; i++ ) {
            projsChoice.addItem(projs[i].label);
            if( plan.hasXYorig ) {
               if( plan.projd == projs[i] ) j = i;
            } else {
               if( pref != null && pref.projd == projs[i] ) j = i;
            }
         }
         projsChoice.setSelectedIndex(j);
         if( modCalib!=null ) modCalib.setEnabled(j >= 0 && projs[j].isModifiable());
         projsChoice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { actionSpecificProj(); }
         });

      }
      else {
         if( modCalib!=null ) modCalib.setEnabled(false);
      }
   }

   private void actionBlank() {
      ((PlanImage)plan).setBlankString(blankField.getText());
      showProp(true);
   }

   // Changement de projection spécifique d'un catalogue ou d'un planBG
   private void actionSpecificProj() {
      if( specificProj==null ) return;
      String sproj = (String) specificProj.getSelectedItem();
      int  index = specificProj.getSelectedIndex();
      
      // Est-ce qu'on a changé quelque chose ?
      if( plan instanceof PlanBG ) {
         if( index==0 && !plan.hasSpecificProj() ) return;
      }
      if( plan.projd.c.getProj()==Calib.getProjType( sproj ) ) return;
      
      if( plan instanceof PlanBG ) {
         
         // Dans le cas du "defaut", on récupère la projection "générale"
         if( index==0 ) {
            ((PlanBG)plan).setSpecificProj(false);
            sproj = aladin.projSelector.getProjItem();
         } else {
            ((PlanBG)plan).setSpecificProj(true);
         }
      }
      plan.modifyProj( sproj );
      
      // Maj éventuelle du sélecteur global
      if( aladin.view.getCurrentView().pref==plan ) {
         aladin.projSelector.setProjectionSilently( sproj );
      }
   }

   // Changement de la frame de traçage d'un planBG
   private void actionSpecificFrame() {
      if( !(plan instanceof PlanBG ) || specificFrame==null
            || ((PlanBG)plan).getFrameDrawing()==specificFrame.getSelectedIndex() ) return;
      ((PlanBG)plan).setFrameDrawing( specificFrame.getSelectedIndex() );
      if( specificFrame.getSelectedIndex()!=0 ) aladin.info(this,aladin.chaine.getString("PROPFRAMEINFO"));
   }

   // Util.indexInArrayOf(Localisation.REPERE[plan.projd.frame], Localisation.FRAMEBIS)

   // Changement de la frame de traçage d'un PlanCatalog (tout seul)
   // NE FONCTIONNE PAS POUR LE MOMENT
   //   private void actionFrameProjCat() {
   //      if( !(plan instanceof PlanCatalog) || defFrame==null ) return;
   //      int i = defFrame.getSelectedIndex();
   //      int f = Localisation.FRAMEVAL[i];
   //      if( f==plan.projd.frame ) return;
   //      plan.projd.modifyFrame(f);
   //      aladin.view.newView(1);
   //      aladin.calque.repaintAll();
   //   }

   // Changement de couleur
   private void actionCouleur() {
      if( couleur==null ) return;
      if( !(plan instanceof PlanContour) ) {
         Color c = couleur.getCouleur();
         if( plan.c!=c ) {
            plan.c = c;
            aladin.calque.repaintAll();
         }
      }
   }

   // Changement de motif pour les sources
   private boolean actionSourceType() {
      if( sourceType==null ) return false;
      plan.setSourceType(sourceType.getSelectedIndex());
      aladin.calque.repaintAll();
      return true;
   }

   // Changement de projection pour le calcul des alpha,delta
   // pour un plan n'ayant que des XY
   private void actionPlanXYProjs() {
      if (projsChoice==null ||
            projsChoice.getSelectedIndex() < 0)  return;
      Projection p = projs[projsChoice.getSelectedIndex()];
      plan.pcat.setCoord(p);
      plan.setNewProjD(p);
      plan.setHasSpecificCalib();
      aladin.view.newView(1);
      aladin.calque.repaintAll();
   }

   // Changement de plan de ref et/ou de projection en fonction
   // des choix dans planRefChoice et projsChoice
   private void actionPlanRefProjs() {
      // thomas : correction d'un bug :
      // ArrayIndexOutOfBounds -1 dans actionPlanRefProjs
      // quand on apply un filtre au bout d'un moment
      // je ne suis pas sur que ce soit la bonne solution, à voir avec pierre
      if( planRefChoice.getSelectedIndex() < 0 ||
            projsChoice==null ||
            projsChoice.getSelectedIndex() < 0 ) {
         //System.out.println("The bug was catched");
         return;
      }
      Plan pref = planRef[planRefChoice.getSelectedIndex()];
      boolean flagAction = false;

      // JE PENSE QUE CELA NE SERT PLUS A RIEN PUISQUE
      // JE NE PEUX PLUS ASSOCIER MANUELLEMENT LE PLAN DE REF (PF-7 oct 09)
      //     if( !pref.ref ) {
      //       Aladin.trace(2, "New reference plane => " + pref.label);
      //       aladin.calque.setPlanRef(pref);
      //       majProjs();
      //       flagAction = true;
      //     }
      // pour éviter un ArrayIndexOutofBoundsException
      // je ne suis pas sur que ce soit la bonne solution
      // 2e condition : du à un pb que j'avais avec export (démo avo)
      if( projsChoice.getSelectedIndex() < 0 || projsChoice.getSelectedIndex()>=projs.length ) return;

      Projection p = projs[projsChoice.getSelectedIndex()];

      if( pref.projd != p ) {
         Aladin.trace(2, "New proj. method for plane "+
               pref.label+" => "+p.label);
         pref.projd = p;
         flagAction = true;
      }

      if (!flagAction) return;

      aladin.view.newView(1);
      aladin.calque.repaintAll();
      majPlanRef();
   }

   private void bookmark(){
      FrameBookmarks fb = aladin.bookmarks.getFrameBookmarks();
      fb.setVisibleEdit();
      String name = aladin.bookmarks.getUniqueName(plan.label);
      String code =  plan.getBookmarkCode();
      StringBuffer param = new StringBuffer();
      if( code!=null && code.indexOf("$TARGET")>=0 ) param.append("$TARGET");
      if( code!=null && code.indexOf("$RADIUS")>=0 ) {
         if( param.length()>0 ) param.append(',');
         param.append("$RADIUS");
      }
      String param1 = param.length()>0 ? param.toString() : null;
      fb.createNewBookmark(name,param1,"Load "+plan.label+(param1!=null?"":" on the view"), code);
   }

   private void apply() {
      setFlagFullMaj(true);// Pour que toutes les fenetres soient maj par la suite
      if( label!=null && !label.getText().equals(plan.label) ) {
         plan.setLabel(label.getText());
         label.setText(plan.label);
         setTitre(BANNER+" \""+plan.label+"\"");
      }

      if( blankField!=null ) actionBlank();
      
      // Changement de projection catalogue par défaut ?
      actionSpecificProj();

      // Changement du frame par défaut ?
      actionSpecificFrame();
      //      actionFrameProjCat();

      if( planRefChoice != null ) {
         if( plan.hasXYorig ) actionPlanXYProjs();
         else actionPlanRefProjs();
      }

      actionCouleur();

      // Modification du scope du folder
      if( scope!=null ) {
         boolean flag = scopeLocal.isSelected();
         if( ((PlanFolder)plan).localScope!=flag ) {
            ((PlanFolder)plan).localScope=flag;
            aladin.calque.repaintAll();
         }
      }
      
      if( plan instanceof PlanContour) {
         int i;
         PlanContour pCont = (PlanContour)plan;
         int nbLevels = pCont.getIntLevels().length;

         // a remplacer par un appel ou on passe le tableau des niveaux et basta
         // vérifier la cohérence des couleurs
         for (i=0;i<nbLevels;i++) {
            pCont.setViewable(i,contoursCB[i].isSelected());
            pCont.adjustColor(contoursCouleurs[i].getCouleur(),i);
         }
         double[] levelTab = new double[curs.niveaux.length];
         for( i=0;i<levelTab.length;i++ ) {
            levelTab[i] = curs.niveaux[i];
         }
         pCont.adjustContour(levelTab);

         curs.repaint();

         Color couleurBase = couleur.getCouleur();

         pCont.updateColorIfNeeded(couleurBase);

         // bidouille pour forcer le reaffichage des objets Couleur
         pmemo = null;
         showProp();
      }

      // Changement du centre ou de l'angle de rotation de l'instrument (plan FIELD)
      if( centerField!=null ) {
         String s=centerField.getText();
         String s1 = (rollField!=null)?rollField.getText():null;
         String s2 = (rotateCenter!=null)?rotateCenter.getText():null;
         if( !s.equals(sField) || (s1!=null && !s1.equals(sRoll))
               || (s2!=null && !s2.equals(rotateCenterField))
               ) {
            try {
               Coord projCenter = new Coord(aladin.localisation.getICRSCoord(s));
               double roll = (s1==null)?0. :Double.valueOf(s1).doubleValue();

               // Déplacement du target (et de tout le FoV)
               if( (s2==null || s2.equals(rotateCenterField)) && s1!=null && s1.equals(sRoll)) {
                  ((PlanField)plan).changeTarget(projCenter.al,projCenter.del);

                  // Réajustement des paramètres individuellement
               } else {
                  Coord rotCenter = new Coord(aladin.localisation.getICRSCoord(s2!=null?s2:s));
                  ((PlanField)plan).setParameters(projCenter.al,projCenter.del, rotCenter.al,rotCenter.del,roll);
               }
               aladin.view.newView();
            } catch( Exception e ) { e.printStackTrace(); Aladin.error(this,aladin.chaine.getString("COORDERR"));}
         }
      }

      if( eqField!=null ) {
         String s=eqField.getText();
         if( !s.equals(sEquinox) ) {
            //System.out.println("Je positionne l'equinoxe par defaut :"+s);
            ((PlanImage)plan).projd.c.SetEquinox(Double.valueOf(s).doubleValue());
            aladin.view.newView(1);
         }
      }

      if( epField!=null ) {
         String s=epField.getText();
         if( !s.equals(plan.getEpoch().getJyr()) ) {
            //System.out.println("Je positionne la nouvelle époque :"+s);
            try {
               plan.setEpoch(s) ;
               epField.setText(plan.getEpoch().toString("J"));
               epField.setForeground(Color.black);
               aladin.view.newView(1);

            } catch(Exception e ) {
               Aladin.error(this,"Proper motion adjustement error\n=>"+e.getMessage());
               epField.setForeground(Color.red);
            }

         }
      }


      if( ! actionSourceType() ) aladin.calque.repaintAll();

   }

   public void actionPerformed(ActionEvent e) {

      Object src = e.getSource();
      String what = src instanceof JButton ? ((JButton)src).getActionCommand() : "";
      
      // Bouton d'édition d'un filtre dédié
      if( src==toGenFilterButton ) {
         plan.toGenericalFilter();
         nofilter.setSelected(true);
         return;
      }

      // Affichage ou non de la polarisation dédiée à un PlanHealpix
      else if( src==btnDisplayPola || src==btnDisplayAmp || src==btnDisplayAng) {
         int mode = src==btnDisplayPola ? PlanHealpix.POLA_SEGMENT_MAGIC_CODE :
            src==btnDisplayAmp ? PlanHealpix.POLA_AMPLITUDE_MAGIC_CODE
                  : PlanHealpix.POLA_ANGLE_MAGIC_CODE;
         ((PlanHealpix)plan).displayPolarisation(mode);
      }

      // Peut être un checkbox concernant un sub FoV
      if( src instanceof JCheckBox && plan instanceof PlanField ) {
         if( ((PlanField)plan).switchCheckbox((JCheckBox)src) ) return;
      }

      // Peut être un checkbox concernant un contour
      if( src instanceof JCheckBox && plan instanceof PlanContour ) {
         apply();
         return;
      }


      // filtres prédéfinis à positionner
      if( src instanceof JRadioButton && plan.isCatalog()&& filterCB!=null  ) {
         String s = filterCB.getSelection().getActionCommand();
         int i= ServerGlu.getFilterIndex(plan.filters,s);
         toGenFilterButton.setEnabled(i>=0);
         plan.setFilter(i);
      }

      // Peut être le sélecteur de déplacement des objets dans le cas d'un plan tool
      else if( src instanceof JRadioButton && plan instanceof PlanTool ) {
         String s = ((JRadioButton)src).getActionCommand();
         try { ((PlanTool)plan).setMovable(s); } catch( Exception e1 ) { }
      }
      
      // Peut être le sélecteur de couleur de fond puor une image couleur
      else if( src instanceof JRadioButton && plan.ref ) {
         String s = cb.getSelection().getActionCommand();
         Color c = s==WHITE ? Color.white : s==BLACK ? Color.black : null;
         if( plan.colorBackground==c ) return;
         plan.colorBackground=c;
         aladin.view.repaintAll();
         return;
      }

      // Cancel
      if( CLOSE.equals(what) ) dispose();

      // Help pour le scope
      else if( " ? ".equals(what) ) {
         aladin.info(this,HSCOPE);
      }

      // Creation d'une nouvelle calib
      else if( NEWCALIB.equals(what) ) {
         //         Plan p = plan.hasXYorig?plan:aladin.calque.getPlanRef();
         Plan p= plan;
         aladin.launchRecalibImg(p);

         // Modification d'une calib
      }else if( MODCALIB.equals(what) ) {
         //         Plan p = plan.hasXYorig?plan:aladin.calque.getPlanRef();
         Plan p = plan;
         if( aladin.frameNewCalib==null ) {
            aladin.frameNewCalib = new FrameNewCalib(aladin,
                  p,projs[projsChoice.getSelectedIndex()]);
         } else aladin.frameNewCalib.majFrameNewCalib(p,
               projs[projsChoice.getSelectedIndex()]);

         // Submit PLAN
      } else if( APPLY.equals(what) ) apply();

      // Creation d'un bookmark
//      else if( BOOKMARK.equals(what) ) bookmark();

      // Visualisation du header fits
      else if( SEEFITS.equals(what) ) aladin.header(plan);

      // Visualisation du header fits
      else if( SEEHIPSPROP.equals(what) ) aladin.header(plan);

      // Visualisation des informations de parsing
      else if( SEEPARSING.equals(what) ) aladin.tableInfo(plan);

      // show/hide FoVs associés
      else if( SHOWFOVS.equals(what) || HIDEFOVS.equals(what) ) {
         boolean flagShow = SHOWFOVS.equals(what)?true:false;
         plan.showFootprints(flagShow);
         if( plan instanceof PlanBGProgen ) ((PlanBGProgen)plan).setShowFootprint(flagShow);
      }

      // export pointing centers
      else if( POINTINGLABEL.equals(what)) {
         ((PlanField)plan).exportAlmaPointings();
      }
   }


   private Border border = null;

   // mise à jour du titre de la frame
   void setTitre(String s) {
      if( border==null ) border=BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, s, TitledBorder.CENTER, TitledBorder.TOP, Aladin.LBOLD),
            BorderFactory.createEmptyBorder(5,5,1,5)));
      propPanel.setBorder(border);
   }

   // Supprime la fenetre des Properties
   public void dispose() {
      frameProp.removeElement(this);
      super.dispose();
   }

   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) dispose();
      super.processWindowEvent(e);
   }

   /******* Les methodes gerant globalement les fenetres de Properties ******/

   static protected void addProperties(Properties p) {
      frameProp.addElement(p);
   }

   /**
    * Crée la fenêtre de Properties pour le plan p
    * Si elle existe déja, la retrouve et l'affiche
    * @param p le plan en question
    */
   static protected void createProperties(Plan p) {
      // Si la fenetre des properties existe deja, on la reaffiche simplement
      Properties pc = getProperties(p);
      if( pc!=null ) { pc.toFront(); return; }
      if( p.type==Plan.FILTER ) pc = new FilterProperties(p);
      else pc = new Properties(p);


      pc.showProp();
   }

   /**
    * Supprime l'objet Properties en fonction d'un plan. Peut ne pas exister
    * eventuellement
    * @param p le plan en question
    */
   static protected void disposeProperties(Plan p) {
      Properties pc = getProperties(p);
      if( pc!=null ) pc.dispose();
   }


   /**
    * Retourne l'objet properties en fonction d'un plan
    * null sinon
    */
   static protected Properties getProperties(Plan p) {
      Enumeration e = frameProp.elements();
      while( e.hasMoreElements() ) {
         Properties pc = (Properties)e.nextElement();
         if( pc.plan==p ) return pc;
      }
      return null;
   }

   /**
    * Si flagFullMaj est faux, la prochaine maj de toutes les fenetres des
    * Properties ne concernera que les fenetres en cours de construction,
    * cad celles dont les donnees sont encore en train d'arriver
    */
   static boolean flagFullMaj=false;
   static synchronized protected void setFlagFullMaj(boolean flag) {
      flagFullMaj=flag;
   }

   /** Maj de la fenêtre des propriétés du plan indiqué en paramètre
    * (si elle existe)
    */
   static protected void majProp(Plan plan) {
      Enumeration e = frameProp.elements();
      while( e.hasMoreElements() ) {
         Properties propc = (Properties) e.nextElement();
         if( propc.plan!=plan ) continue;
         propc.showProp(true);
      }
   }

   /** Maj des fenetres de Properties (mais pas les FilterProperties)
    * @param methode 0 toutes les fenetres
    *                1 uniquement les fenetres en attente de donnees
    *                2 toute les fenetres en forçant les réaffichages
    */
   static protected void majProp() { majProp(0); }
   static protected void majProp(int methode) {
      Enumeration e = frameProp.elements();
      while( e.hasMoreElements() ) {
         Properties propc = (Properties) e.nextElement();
         if( propc.plan.type==Plan.FILTER ) continue;
         if( !flagFullMaj && methode==1 ) {
            if( propc.plan.flagOk ) continue;
         }
         propc.showProp(methode==2 ? true : false);
         propc.majPlanRef();
         propc.majProjs();
      }
      setFlagFullMaj(false);
   }
}
