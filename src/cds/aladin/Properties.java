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

import cds.tools.*;
import cds.tools.pixtools.CDSHealpix;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

   String SEEFITS,SEEPARSING,TABLEINFO,LOADURL,NEWCALIB,MODCALIB,/*,TOPBOTTOM,RIGHTLEFT,NEWCOL*/SHOWFOVS,HIDEFOVS,
          TITLE,BANNER,APPLY,CLOSE,NOFILTER,LABEL,COLOR,ERROR,STATE,UNDER,SHAPE,IMG,VIEWABLE,
          LEVEL,REFCOORD,REFROTATE,ANGLE,COMPONENT,SOURCE,INF,FMT,EPOCH,DATEOBS,WCSEQ,SIZE,FRAME,DELAY,
          ORIGIN,FILTER,FILTERB,ASTRED,XYRED,PROJ,NONE,METHOD,CENTER,SELECTFIELD,DEFCATPROJ,FLIPFLOP,ASSFOV,
          LOCAL,GLOBAL,SCOPE,HSCOPE,OPACITY,OPACITYLEVEL,DENSITY,WHITE,BLACK,AUTO,COLORBG,POLA,DISPLAYPOLA,
          GENERATEPOLAAMP,GENERATEPOLAANG,CURRENTFIELD,POLAOPTIONS,SEGMENTLEN,SEGMENTTHICK,SEGMENTDENSITY,
          SCALINGFACTOR;

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
   Couleur couleur=null;         // La couleur
   JComboBox sourceType=null;       // Le type de representation graphique
   ButtonGroup scope;
   JRadioButton scopeGlobal,scopeLocal;
   ButtonGroup xyLock=null;    // Pour le plan TOOl, mode de calcul des x,y
   JComboBox planRefChoice=null;    // Projections possibles pour le plan de reference
   JComboBox defCatProj=null;    // Les projections possibles par défaut pour un catalogue sans image sous-jacente
   JComboBox defFrame=null;     // Les frames possibles par défaut pour un catalogue sans image sous-jacente
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
   JTextField eqField=null;	 // Pour pouvoir modifier l'equinox par defaut
   String sEquinox=null;	 // Pour savoir si l'equinox par defaut a ete modifie
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
      SEEPARSING = aladin.chaine.getString("VWTABLEINFO");
      TABLEINFO = aladin.chaine.getString("PROPTABLEINFO");
      LOADURL = aladin.chaine.getString("PROPLOADURL");
      NEWCALIB = aladin.chaine.getString("PROPNEWCALIB");
      MODCALIB = aladin.chaine.getString("PROPMODCALIB");
      SHOWFOVS = aladin.chaine.getString("PROPSHOWFOVS");
      HIDEFOVS = aladin.chaine.getString("PROPHIDEFOVS");
      TITLE = aladin.chaine.getString("PROPTITLE");
      BANNER = aladin.chaine.getString("PROPBANNER");
      APPLY = aladin.chaine.getString("PROPAPPLY");
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
      FRAME = aladin.chaine.getString("PROPFRAME");
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
      eqField=rollField=null;
      rotateCenter = null;

      // On reconstruit le panel
      if( panel!=null ) remove(panel);
      panel = new JPanel();
      panel.setLayout( new BorderLayout(5,5) );

      Aladin.makeAdd(panel,propPanel=getPanelProperties(),"Center");
      // bordure avec titre
      setTitre(BANNER+" \""+plan.label+"\"");

      Aladin.makeAdd(panel,getPanelValid(),"South");

      Aladin.makeAdd(this,panel,"Center");
//      ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

      if( !noPack ) pack();
      
      setVisible(true);
   }

  /** Ajoute un filet et passe a la ligne
   * @param p Le panel sur lequel on travaille
   * @param g Le gestionnaire d'affichage
   * @param c les contraintes courantes sur le gestionnaire d'affichage
   */
   protected static void addFilet(JPanel p, GridBagLayout g, GridBagConstraints c) { addFilet(p,g,c,5,1); }
   protected static void addFilet(JPanel p, GridBagLayout g, GridBagConstraints c,int h,int type) {
      Filet f = new Filet(h,type);
      GridBagConstraints c1 = (GridBagConstraints) c.clone();
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.BOTH;
      g.setConstraints(f,c);
      p.add(f);
      c = c1;
   }

   /** Ajoute d'un titre de section et passe a la ligne
    * @param p Le panel sur lequel on travaille
    * @param title Le titre de la nouvelle section
    * @param g Le gestionnaire d'affichage
    * @param c les contraintes courantes sur le gestionnaire d'affichage
    */
  protected static void addSectionTitle(JPanel p, String title,GridBagLayout g, GridBagConstraints c) {
   	  JLabel l = new JLabel(title);
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.weightx = 1.0;
      g.setConstraints(l,c);
      p.add(l);
   }

  /** Ajoute d'un paragraphe centré d'explications et passe à la ligne
   * @param p Le panel sur lequel on travaille
   * @param info Le texte d'explication (peut contenir des \n
   * @param g Le gestionnaire d'affichage
   * @param c les contraintes courantes sur le gestionnaire d'affichage
   */
   protected static void addInfo(JPanel p, String info,GridBagLayout g, GridBagConstraints c) {
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.CENTER;
      c.fill = GridBagConstraints.HORIZONTAL;
//      c.weightx = 1.0;

//      MyLabel l = new MyLabel(info,JLabel.CENTER,Aladin.SITALIC);
      JLabel l = new JLabel(Util.fold("<center>"+info+"</center>",80,true),JLabel.CENTER);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      g.setConstraints(l,c);
      p.add(l);
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
      p.add( b=new JButton(CLOSE)); b.addActionListener(this);
      return p;
   }
   
   protected static void addFull(JPanel p, Component valeur, GridBagLayout g, GridBagConstraints c) {
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.fill = GridBagConstraints.BOTH;
      c.weightx = 1.0;
      c.anchor = GridBagConstraints.CENTER;
      g.setConstraints(valeur,c);
      p.add(valeur);
   }

  /** Ajoute dans le JPanel un couple d'elements titre: valeur
   * @param frame  Le frame de référence (pour savoir où afficher le help, null sinon)
   * @param p      Le panel sur lequel on travaille
   * @param titre  Le titre de l'element que l'on va ajouter
   * @param valeur L'element (Component) a ajouter
   * @param g      Le gestionnaire d'affichage
   * @param c      Les contraintes courantes sur le gestionnaire d'affichage
   */
   public static void addCouple(final JFrame frame, JPanel p, Object titre, final String help, Component valeur,
                GridBagLayout g, GridBagConstraints c, int titleAnchor) {

      Component t;

      if( titre instanceof String ) {
         JLabel l = new JLabel((String)titre);
         l.setFont(l.getFont().deriveFont(Font.ITALIC));
         t=l;
      } else t=(Component)titre;
      
      if( help!=null ) {
         JPanel p2 = new JPanel();
         p2.add(t);
         JButton h = Util.getHelpButton(frame,help);
         p2.add(h);
         t=p2;
      }

      c.anchor = titleAnchor;
      c.gridwidth = GridBagConstraints.RELATIVE;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 0.0;
      g.setConstraints(t,c);
      p.add(t);
      
      if( valeur instanceof JButton ) {
         JPanel p1 = new JPanel();
         p1.add(valeur);
         t=p1;
      } else t=valeur;

      c.gridwidth = GridBagConstraints.REMAINDER;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 1.0;
      c.anchor = GridBagConstraints.WEST;
      g.setConstraints(t,c);
      p.add(t);

   }

   public static void addCouple(JPanel p, Object titre, Component valeur,
         GridBagLayout g, GridBagConstraints c) {
     addCouple(null, p, titre, null, valeur, g, c, GridBagConstraints.WEST);
 }

   protected static void addCouple(JFrame frame, JPanel p, Object titre, String help, Component valeur,
         GridBagLayout g, GridBagConstraints c) {
     addCouple(frame, p, titre, help, valeur, g, c, GridBagConstraints.WEST);
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

   private void showInfo() {
      aladin.info(this,plan.info);
   }

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
      if( plan.info!=null ) {
         JPanel p1 = new JPanel();
         p1.add(label);
         b = new JButton(FrameServer.INFO);
         Insets m = b.getMargin();
         b.setMargin(new Insets(m.top,3,m.bottom,3));
         b.setOpaque(false);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showInfo(); }
         });
         p1.add(b);
         addCouple(p, LABEL, p1, g,c);
      } else addCouple(p, LABEL, label, g,c);

      //La couleur
      if( !(plan.isImage() || plan.type==Plan.ALLSKYIMG)
          && plan.type!=Plan.FOLDER && !(plan instanceof PlanContour)) {
         couleur = new Couleur(plan.c);
         addCouple(p,COLOR, couleur, g,c);
         couleur.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { actionCouleur(); }
         });
      }

      // couleur du plan contour
      if( plan instanceof PlanContour) {
         couleur = new Couleur(PlanContour.couleursBase,plan.c,25,4);
         addCouple(p,COLOR, couleur, g,c);
      }

      // Affichage de l'etat
      if( !plan.flagOk ) {
         JLabel l = new JLabel();
         String titre;
         l.setForeground( Color.red );
         l.setFont(l.getFont().deriveFont(Font.BOLD));
         if( plan.error!=null ) { titre = ERROR; l.setText(plan.error); }
         else { titre = STATE; l.setText(UNDER); }
         addCouple(p,titre, l, g,c );
      }

      if( plan.flagOk ) {
         // Les sourceType du Plan
         if( plan.isCatalog() ) {
            sourceType = new JComboBox();
            for( int i=0; i<Source.TYPENAME.length; i++ ) sourceType.addItem(Source.TYPENAME[i]);
            sourceType.setSelectedIndex(plan.sourceType);
            addCouple(p,SHAPE, sourceType, g,c );
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
            addCouple(p,SCOPE, pscope, g,c);
         } else scope=null;

         // cas d'un PlanContour
         if (plan instanceof PlanContour) {
           PlanContour pcont = (PlanContour)plan;

           // ajout du label de l'image sur laquelle a ete faite le contour
           addCouple(p,IMG,new JLabel(pcont.p.label + " - " + pcont.p.objet),g,c);

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
             addCouple(panelScroll,suffix + " level",panelCont,g,c);
           }
           scroll = new JScrollPane(panelScroll); // JScrollPane pour le choix des couleurs
           scroll.setSize(scrollWidth,scrollHeight);
           g.setConstraints(scroll, c);
           p.add(scroll);

           // affichage de l'histogramme des niveaux de gris
           Histogramme hist = new Histogramme((PlanImage)pcont.p);
           addCouple(p,LEVEL,hist,g,c);

           // affichage des differents niveaux
           curs = new Curseur(hist);
           curs.nbNiveaux = contoursLevels.length;
           curs.couleurTriangle = pcont.couleursContours;
           for (int i=0;i<contoursLevels.length;i++) {
             curs.niveaux[i] = contoursLevels[i];
           }
           addCouple(p,"",curs,g,c);
         }

         // Une ligne de separation
         if( plan.isCatalog() || plan.isImage() ) addFilet(p,g,c);
      }

      // Centre du champ de l'instrument
      if( plan.type==Plan.APERTURE ) {
         PlanField pf = (PlanField)plan;
         sField = pf.getProjCenter();
         centerField = new JTextField(sField,20);
         if( !pf.isMovable () ) centerField.setEnabled(false);
         addCouple(p,REFCOORD, centerField, g,c );
         if( Aladin.ROTATEFOVCENTER ) {
            rotateCenterField = pf.getRotCenter();
            rotateCenter = new JTextField(rotateCenterField,20);
            if( !pf.isRollable () ) {
               rotateCenter.setEnabled(false);
            }
            addCouple(p,REFROTATE, rotateCenter, g,c );
         }
         sRoll = pf.getRoll();
         rollField = new JTextField(sRoll, 4);
         if( !pf.isRollable () ) rollField.setEnabled(false);
         addCouple(p,ANGLE, rollField, g,c );
         JPanel subFoV = pf.getPanelSubFov(this);
         if( subFoV!=null ) addCouple(p,COMPONENT,subFoV,g,c);
      } else centerField=null;

      // Dans le cas d'un catalogue nombre d'items
      if( plan.isSimpleCatalog() ) {
         addCouple(p,SOURCE+":",new JLabel(""+((PlanCatalog)plan).getCounts()), g,c);
      }

      // Plan MultiExtension
      // Bouton de recuperation de visualisation du header FITS
      if( plan instanceof PlanFolder  && ((PlanFolder)plan).headerFits!=null ||
          plan instanceof PlanCatalog && ((PlanCatalog)plan).headerFits!=null ) {
         addCouple(p,"Fits extension", b=new JButton(SEEFITS), g,c);
         b.addActionListener(this);
      }

      // Info sur les images
//      if( plan.type==Plan.IMAGE || plan.type==Plan.IMAGERGB && ((PlanImage)plan).headerFits!=null ) {
      if( plan.isImage() || plan instanceof PlanBG /*plan.type==Plan.ALLSKYIMG*/ ) {
        PlanImage pimg = (PlanImage)plan;

      	// Survey de l'image
        String survey=pimg.survey();
      	if( survey!=null && survey.length()>0 )
             addCouple(p,INF, new JLabel(pimg.survey()), g,c);

      	// Format d'image
        JLabel fmtl = new JLabel(
              plan instanceof PlanHealpix ? "HEALPix Fits"+(((PlanHealpix)plan).isPartial()?" (partial mode)":"") :
              plan instanceof PlanMoc ? "HEALPix coverage map (MOC)" :
              plan instanceof PlanBG ? "HEALPix CDS tesselation" :
                 PlanImage.describeFmtRes(pimg.dis,pimg.res));

        // Bouton de recuperation de visualisation du header FITS
        if( pimg.headerFits!=null ) {
           JPanel fmtp = new JPanel();
           fmtp.setLayout(new FlowLayout(FlowLayout.LEFT));
           fmtp.add(fmtl);
           fmtp.add( b=new JButton(SEEFITS) );
           b.addActionListener(this);
           addCouple(p,FMT, fmtp, g,c);
        } else addCouple(p,FMT, fmtl, g,c);

      	// Info d'image
      	if( plan.isImage() && plan.flagOk && plan.projd!=null ) {
           double ep = plan.projd.c.GetEpoch();
           if( !Double.isNaN(ep) ) {
              addCouple(p,EPOCH, new JLabel(Astrodate.JDToDate(Astrodate.YdToJD(ep))+" ("+ep+")"), g,c);
           } else {
              String d = ((PlanImage)plan).getDateObs();
              if( d!=null ) addCouple(p,DATEOBS, new JLabel(d), g,c);
           }
           double eq = plan.projd.c.GetEquinox();
           if( eq!=0.0 ) {
              eq= (int)(eq*1000)/1000.0;
              addCouple(p,WCSEQ, new JLabel(""+eq), g,c);

           // On autorise la modification de l'equinoxe par defaut
           } else {
              sEquinox="2000.0";	// IL FAUDRA FAIRE PLUS MALIN
              eqField = new JTextField(sEquinox);
              addCouple(p,WCSEQ, eqField, g,c);
           }
      	}
        if( pimg.width!=0 && !(pimg instanceof PlanBG) ) addCouple(p,SIZE, new JLabel(pimg.getSizeInfo()), g,c);
      }

      if( plan instanceof PlanImageBlink ) {
         PlanImageBlink pb=(PlanImageBlink)plan;
         addCouple(p,FRAME,new JLabel(pb.getNbFrame()+""), g,c);
      }

      // Origine
      if( plan.from!=null ) addCouple(p,ORIGIN, new JLabel(Util.fold(plan.from,50,true)), g,c);
      
      // Accès à la description complète
      if( plan instanceof PlanBG ) {
         final PlanBG pbg = (PlanBG)plan;
         if( pbg.verboseDescr!=null ) {
            b = new JButton("Full description...");
            final JFrame frame = this;
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  aladin.info(frame,pbg.verboseDescr);
               }
            });
            addCouple(p,"", b, g,c);
         }
      }

      // Url de déchargement
      if( plan.getUrl()!=null ) {
         url = new JTextField(plan.getUrl(),40);
         if( plan.hasRemoteUrl() ) {
            url.setBorder(BorderFactory.createEmptyBorder());
            url.setForeground(Color.blue);
      }
         url.setCaretPosition(0);
         addCouple(p,"", url, g,c);
      }

      // Panel pour les informations techniques
      if( plan.hasRemoteUrl() || (plan.isSimpleCatalog() || plan.type==Plan.FOLDER) ) {

         JPanel panelInfo = new JPanel();
         panelInfo.setBorder(BorderFactory.createEmptyBorder());
         panelInfo.setLayout(new FlowLayout(FlowLayout.CENTER));
         if( plan.getUrl()!=null ) {
            panelInfo.add(b=new JButton(LOADURL));
            b.addActionListener(this);
         }
         c.fill = GridBagConstraints.NONE;
         addCouple(p,"",panelInfo, g,c);
         c.fill = GridBagConstraints.BOTH;
      }

      if( plan.isSimpleCatalog() ) {
         PlanCatalog pc = (PlanCatalog)plan;
         b=new JButton(SEEPARSING);
         b.addActionListener(this);
         int n = pc.getNbTable();
         if( n==1 ) addCouple(p,TABLEINFO, b, g,c);
         else {
            Vector leg = pc.getLegende();
            StringBuffer s = new StringBuffer("<html>");
            for( int i=0; i<leg.size(); i++ ) s.append((i>0?"<br>":"")+(i+1)+": "+((Legende)leg.elementAt(i)).name);
            s.append("</html>");
            addCouple(p,TABLEINFO, new JLabel(s.toString()), g,c);
            addCouple(p,"", b, g,c);
         }
      }

      // Gestion des filtres prédéfinis
      if( plan.filters!=null ) {
         addFilet(p,g,c);
         JLabel l = new JLabel(FILTER);
         l.setFont(l.getFont().deriveFont(Font.BOLD));
         c.fill = GridBagConstraints.NONE;
         addCouple(p,l,getPanelFilter(plan),g,c);
         c.fill = GridBagConstraints.BOTH;

         toGenFilterButton = b=new JButton("This filter on the stack for editing");
         b.addActionListener(this);
         if( !Aladin.OUTREACH )addCouple(p,"",toGenFilterButton,g,c);
         toGenFilterButton.setEnabled(plan.hasDedicatedFilter());

      }

      // champ à afficher (PlanHealpix)
      if( plan instanceof PlanHealpix ) {
          final PlanHealpix ph = (PlanHealpix)plan;

          if ( ph.type==PlanBG.ALLSKYPOL ) {
              addFilet(p,g,c);
              addSectionTitle(p,POLAOPTIONS,g,c);

              polaSegmentLen = new JSlider(0, 200);
              polaSegmentLen.setValue((int)ph.getSegmentLenFactor()*200);
              polaSegmentLen.setPaintTicks(true);
              polaSegmentLen.addChangeListener(this);
              addCouple(p, SEGMENTLEN, polaSegmentLen, g, c);

              polaSegmentThickness = new JSlider(1, 5);
              polaSegmentThickness.setValue(ph.getSegmentThickness());
              System.out.println(ph.getSegmentThickness());
              polaSegmentThickness.setMajorTickSpacing(1);
              polaSegmentThickness.setPaintTicks(true);
              polaSegmentThickness.setSnapToTicks(true);
              polaSegmentThickness.setPaintLabels(false);
              polaSegmentThickness.addChangeListener(this);
              addCouple(p, SEGMENTTHICK, polaSegmentThickness, g, c);

              polaSegmentDensity = new JSlider(0, 200);
              polaSegmentDensity.setValue(100);
              polaSegmentDensity.setMajorTickSpacing(25);
              polaSegmentDensity.setPaintTicks(true);
              polaSegmentDensity.setSnapToTicks(true);
              polaSegmentDensity.addChangeListener(this);
              addCouple(p, SEGMENTDENSITY, polaSegmentDensity, g, c);
          }
          else {
              if (ph.tfieldNames!=null && ph.idxTFormToRead>=0 ) {
                 // nom du field affiché
                 addFilet(p, g, c);
                 addCouple(p, CURRENTFIELD, new JLabel(ph.tfieldNames[ph.idxTFormToRead]), g, c);
              }

              if( ph.hasPolarisationData() || ph.tfieldNames.length>1 ) {
                  addFilet(p, g, c);
              }
                // bouton pour demander affichage de la polarisation
                if (ph.hasPolarisationData()) {
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
                    addCouple(null, p, POLA, null, pPola, g, c, GridBagConstraints.NORTHWEST);
                }

                // affichage autres champs disponibles
                if (ph.tfieldNames.length > 1) {
                    JPanel pAvailableFields = new JPanel(new GridLayout(0, 1));
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
                    addCouple(null, p, SELECTFIELD, null, pAvailableFields, g, c, GridBagConstraints.NORTHWEST);
                }
            }
      }
      
      if( plan.type==Plan.ALLSKYIMG ) {
         final PlanBG pbg = (PlanBG) plan;
         addFilet(p, g, c);
         long res = pbg.getMaxHealpixOrder();
         long ord = pbg.getLosangeOrder();
         addSectionTitle(p, "HEALPix tesselation properties", g, c);
         addCouple(p, "Best pixel resolution", new JLabel(pbg.getMaxResolution()), g, c);
         addCouple(p, "Tile format", new JLabel(pbg.getFormat()), g, c);
         if( ord>0 ) addCouple(p, "Tile width:",  new JLabel((int)CDSHealpix.pow2(ord)+" pix (2^"+ord+")"), g, c);
         addCouple(p, "HEALPix NSide:",  new JLabel((long)CDSHealpix.pow2(res)+" (2^"+res+")"), g, c);
         if( pbg.inFits && pbg.inJPEG ) {
            JButton bt = new JButton( pbg.truePixels ? "Switch to fast 8 bit pixel mode" : "Switch to (slow) true pixel mode");
            bt.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  pbg.switchFormat();
                  showProp(true);
                  aladin.view.repaintAll();
                  
               }
            } );
            addCouple(p,"",bt, g, c);
         }
      }
      
      if( plan.type==Plan.ALLSKYMOC ) {
         final PlanMoc pmoc = (PlanMoc)plan;
         boolean wireFrame = pmoc.getWireFrame();
         ButtonGroup bg = new ButtonGroup();
         final JCheckBox b1 = new JCheckBox("wire frame");
         b1.setSelected(wireFrame);
         b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pmoc.setWireFrame(b1.isSelected()); aladin.calque.repaintAll(); }
         });
         JCheckBox b2 = new JCheckBox("solid frame");
         b2.setSelected(!wireFrame);
         b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pmoc.setWireFrame(b1.isSelected()); aladin.calque.repaintAll(); }
         });
         JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         bg.add(b1); bg.add(b2);
         p1.add(b1); p1.add(b2);
         addCouple(p,"Drawing method",p1,g,c);
      }
      
      if( plan instanceof PlanBG ) {
         final PlanBG pbg = (PlanBG) plan;
         addCouple(p, "HEALPix Coordsys:", new JLabel(Localisation.getFrameName(pbg.frameOrigin)), g, c);
         if( pbg.hasMoc() ) {
            JButton bt = new JButton("Load it");
            bt.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { pbg.loadMoc(); }
            });
            addCouple(p,"Coverage map:",bt,g,c);
         }
      }
      
      if( plan.flagOk && (plan.isSimpleCatalog() || plan instanceof PlanBG) ) {
         
         JPanel p1 = aladin.view.getPlotControlPanelForPlan(plan);
         if( p1!=null ) {
            addFilet(p,g,c);
            addCouple(p,"Scatter plot", p1, g,c);
         }

         // Les projections possibles par défaut s'il n'y a pas d'images dessous ou si PlanBG
         else if( plan.ref && Projection.isOk(plan.projd) )  {
            defCatProj = new JComboBox( Projection.getAlaProj() );
//            String pr[]=Projection.getAlaProj();
//            for( int i=0; i<pr.length; i++ ) defCatProj.addItem(pr[i]);
//            defCatProj.setSelectedIndex(plan.projd.c.getProjSys()-1);
            defCatProj.setSelectedIndex( Projection.getAlaProjIndex( Calib.getProjName(plan.projd.c.getProj()) ) );
            defCatProj.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { actionDefCatProj(); }
            });

            addFilet(p,g,c);
            addSectionTitle(p,DEFCATPROJ,g,c);
            addCouple(p,CENTER, new JLabel(plan.projd.c.getProjCenter().getSexa()), g,c);
            addCouple(p,METHOD, defCatProj, g,c);
            
            if( plan.ref && plan instanceof PlanBG ) {
               defFrame = Localisation.createFrameCombo();
               defFrame.setSelectedIndex( ((PlanBG)plan).getFrameDrawing() );
               defFrame.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) { actionFrameProj(); }
               });
               addCouple(p,".frame", defFrame, g,c);
            }
         }
      }
      
      
      boolean filet=false; 
      
      if( plan.isCatalog() ) {
          if( !filet ) addFilet(p, g, c); filet=true;
          scalingFactor = new JSlider(0, 300);
          scalingFactor.setMinimumSize(scalingFactor.getPreferredSize());
          scalingFactor.setValue((int)plan.getScalingFactor()*100);
          scalingFactor.setMajorTickSpacing(50);
          scalingFactor.setPaintLabels(true);
          scalingFactor.setPaintTicks(true);
          scalingFactor.setPaintTrack(true);
          scalingFactor.addChangeListener(this);
          addCouple(p, SCALINGFACTOR, scalingFactor, g, c);
      }
      
      
      // niveau d'opacité des images et des footprints
      if( aladin.calque.canBeTransparent(plan) ) {
         if( !filet ) addFilet(p, g, c); filet=true;
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
          addCouple(p,OPACITY, pTransp, g,c);
      }
      
      // Ajustement de l'ordre max
      if( plan instanceof PlanBGCat && !(plan instanceof PlanMoc)) {
         if( !filet ) addFilet(p, g, c); filet=true;
          JPanel pGapOrder = new JPanel(new FlowLayout());
          gapOrder = new JSlider(-PlanBGCat.MAXGAPORDER, PlanBGCat.MAXGAPORDER);
          gapOrder.setValue(((PlanBGCat)plan).getGapOrder());
          gapOrder.setMajorTickSpacing(1);
          gapOrder.setPaintLabels(true);
          gapOrder.setPaintTicks(true);
          gapOrder.setPaintTrack(true);
          gapOrder.addChangeListener(this);
          pGapOrder.add(gapOrder);
          addCouple(p,DENSITY, pGapOrder, g,c);
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
         addCouple(p,COLORBG,bg, g,c);
      }

      // Gestion du plan de reference et de la projection associee
      // DANS LE CAS D'UN CATALOGUE, JE N'AFFICHE LE CHOICE QUE DANS LE CAS D'UNE
      // RECALIBRATION XY OU S'IL N'Y A PAS D'IMAGE EN DESSOUS
      if( plan.flagOk &&
          (plan.isImage()
                || (plan.isSimpleCatalog() && plan.hasXYorig)) ) {

         addFilet(p,g,c);
         String s = plan.isImage()?ASTRED:
                    plan.hasXYorig?XYRED:PROJ;
         addSectionTitle(p,s,g,c);

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
         addCouple(p,"   "+METHOD, projsP, g,c);
      }

      // boutons pour montrer cacher les FoVs associés à un Plan.CATALOG
      if( plan.flagOk && plan.isSimpleCatalog() && ((PlanCatalog)plan).hasAssociatedFootprints() ) {
         addFilet(p,g,c);
         addSectionTitle(p,ASSFOV,g,c);

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
         public void actionPerformed(ActionEvent e) { actionDefCatProj(); }
      });

    }
    else {
      if( modCalib!=null ) modCalib.setEnabled(false);
    }
   }
   
   // Changement de projection par défaut d'un catalogue
   private void actionDefCatProj() {
      if( defCatProj==null || plan.projd.c.getProj()==Calib.getProjType( (String)defCatProj.getSelectedItem() ) ) return;
      plan.modifyProj((String) defCatProj.getSelectedItem());
   }
   
   // Changement de la frame de traçage d'un planBG
   private void actionFrameProj() {
      if( defFrame==null
            || ((PlanBG)plan).getFrameDrawing()==defFrame.getSelectedIndex() ) return;
      ((PlanBG)plan).setFrameDrawing( defFrame.getSelectedIndex() );
   }

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
   private void actionSourceType() {
      if( sourceType==null ) return;
      plan.setSourceType(sourceType.getSelectedIndex());
      aladin.calque.repaintAll();

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

   private void apply() {
      setFlagFullMaj(true);// Pour que toutes les fenetres soient maj par la suite
      if( label!=null ) {
         plan.setLabel(label.getText());
         label.setText(plan.label);
         setTitre(BANNER+" \""+plan.label+"\"");
      }

      // Changement de projection catalogue par défaut ?
      actionDefCatProj();

      // Changement du frame par défaut ?
      actionFrameProj();

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
               double roll = (s1==null)?0.
                            :Double.valueOf(s1).doubleValue();

               // Déplacement du target (et de tout le FoV)
               if( (s2==null || s2.equals(rotateCenterField)) && s1!=null && s1.equals(sRoll)) {
                  ((PlanField)plan).changeTarget(projCenter.al,projCenter.del);

               // Réajustement des paramètres individuellement
               } else {
                  Coord rotCenter = new Coord(aladin.localisation.getICRSCoord(s2!=null?s2:s));
                  ((PlanField)plan).setParameters(projCenter.al,projCenter.del, rotCenter.al,rotCenter.del,roll);
               }
               aladin.view.newView();
            } catch( Exception e ) { e.printStackTrace(); Aladin.warning(this,aladin.chaine.getString("COORDERR"));}
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

      actionSourceType();
      aladin.calque.repaintAll();

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
      if( src instanceof JRadioButton && plan.isCatalog() ) {
         String s = filterCB.getSelection().getActionCommand();
         int i= ServerGlu.getFilterIndex(plan.filters,s);
         toGenFilterButton.setEnabled(i>=0);
         plan.setFilter(i);
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

//      // Flip Top/Bottom
//      else if( TOPBOTTOM.equals(what) ) {
//         aladin.flip((PlanImage)plan,0);
//      }
//
//      // Flip Right/Left
//      else if( RIGHTLEFT.equals(what) ) {
//         aladin.flip((PlanImage)plan,1);
//      }

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

      // Visualisation du header fits
      else if( SEEFITS.equals(what) ) aladin.header(plan);

      // Visualisation des informations de parsing
      else if( SEEPARSING.equals(what) ) aladin.tableInfo((PlanCatalog)plan);

      // Rechargement de l'URL dans le navigateur
      else if( LOADURL.equals(what) ) {
         String u = url.getText().trim();
         if( u.length()==0 ) u=plan.getUrl();  // Si le champ est vide, je prends l'URL d'origine
         aladin.glu.showDocument("Http",u,true);
      }
//      // Ajout d'une nouvelle colonne
//      else if( NEWCOL.equals(what) ) aladin.addCol((PlanCatalog)plan);

      // show/hide FoVs associés
      else if( SHOWFOVS.equals(what) || HIDEFOVS.equals(what) ) {
         boolean flagShow = SHOWFOVS.equals(what)?true:false;
         ((PlanCatalog)plan).showFootprints(flagShow);
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
