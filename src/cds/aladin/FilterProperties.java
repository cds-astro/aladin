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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultEditorKit;

import cds.tools.Util;
import cds.tools.parser.Parser;


/** FilterProperties
 * Frame de Propriétés des filtres (PlanFilter)
 * Cette classe est découplée de la fenêtre des propriétés des autres plans
 * @author Thomas Boch [CDS] - Pierre Fernique [CDS]
 * @version 1.1 : (dec 2007) Passage à Swing
 * @version 0.7 : (avril 2005) Amélioration de l'ergonomie, introduction d'un mode Beginner
 * @version 0.6 : (mars 2004) Correction du bug lié au TextArea sous Windows + modifs pour robot
 * @version 0.5 : (3 mars 2003) Création
 */
public final class FilterProperties extends Properties implements MouseListener, ActionListener {


   protected String SELECT,EXPORT,UCD,FILTER_EXAMPLES,FILTERMANUAL,SAVEFILTER,
	       LOADFILTER,PICKCOLUMN,PICKUCD,SHAPEFUNC,COLORFUNC,IOERR,CAVEAT,
	       MATHS,OPEN_COLUMNS,LABEL,CHOOSE,CHOOSE1,PREDEF,YOUROWN,PICK,
	       COL,ACTION,UNIT,CPLANE,FREQUCD,CATUCD,SHAPEFCT,COLORFCT,CAVEATMSG,
	       FILTER_EXPLAIN,RAINBOWCM;

    protected static String BEGINNER,ADVANCED;

    // onglets pour choisir le mode
    JTabbedPane modeTabbedPane;

	static private String currentMode = BEGINNER; // on commence en mode Débutant

    // utilise-t-on les explications d'UCD
    static boolean useUcdExplain = true;

	// URL d'accès au browser d'UCD
	//private static final String URLUCD = "http://vizier.u-strasbg.fr/UCD/UCD/java/";

	private static final String[] UCDBASE = {"phot*", "phot.mag*", "phot.mag;em.opt*", "pos.eq.ra;meta.main",
			"pos.eq.dec;meta.main", "src.class*", "meta.code*", "meta.id;meta.main", "meta.number",};

    private static final String[] UNITBASE = {"arcmin", "arcsec", "deg", "eV", "Jy", "mag",
        "pc", "rad", "yr", "W"};

    private static String[] UNITEXPLAIN;

	FileDialog fd = null;


	// description des filtres pour mode débutant
	protected static String[] BEGINNER_FILTER;

	// definitions correspondants aux filtres pour mode débutant
	protected static final String[] BEGINNER_FILTERDEF = {
			"{ draw circle(-$[phot.mag*]) }",
			"$[phot.mag*]<12 { draw }",
			"$[phot.mag*]>17 { draw }",
			"{ draw $[src.class] }",
			"{ draw pm($[pos.pm;pos.eq.ra], $[pos.pm;pos.eq.dec]) }",
			"{\ndraw\ndraw ellipse(0.5*$[phys.angSize.smajAxis],0.5*$[phys.angSize.sminAxis],$[pos.posAng])\n}"
	};

	private ButtonGroup beginnerCbg; // pour choix filtre débutant

	// Les references aux objets

	PlanFilter pf;

	// mémoire du label et du script
	String oscript, olabel;

	// Les composantes de l'objet
	private JPanel panel;                  // Le panel de la frame
	private int  hcmemo=0;                // Memorisation du hashcode du plan memorise
	//private boolean flagHide=true;        // Vrai si la fenetre est cache

	private boolean computeCol=true;	  // flag indiquant si on doit recalculer l'ensemble des colonnes et UCD dispos

	private static boolean pickUCDMode = false;  // vrai si on est en mode "pick UCD"
	private static boolean pickColumnMode = false; // vrai si on est en mode "pick column"

	private static FilterProperties curWindow; // derniere fenetre FilterProperties utilisé

    JButton applyBtn;
    JButton closeBtn;

    JButton showRainbowBtn;

	// Widgets
	JTextField label;              // Le label du plan

	JTextArea filterDef;  // Pour entrer/modifier la definition du filtre
    JScrollPane filterDefSp;
	private String saveDef;      // Sauvegarde de la definition du filtre
    private String saveName;     // Sauvegarde du nom du filtre
	private JComboBox predefFilters; // Pour choisir un filtre predefini
//	private PopupMenu popup;           // PopupMenu d'aide à l'écriture (UCD,colonnes, actions)
	private JPopupMenu columnMenu;           // Pour choisir une colonne directement
	private JPopupMenu ucdMenu;
	private JPopupMenu actionMenu;
	private JPopupMenu unitMenu;
	private JMenu currentUcdMenu;       // contient les UCDs correspondant aux cats chargés

	// frames d'aide à la rédaction
	static private ColumnHelperFrame columnHelper;
	static private MathHelperFrame mathHelper;

	static private PickupHelperFrame pickupHelper;

	private int oldIndex = 0;         // index sauvegardant la derniere position dans predefFilters

//	static Color LIGHTER_GRAY = new Color(215, 215, 215);


	// initialisation statique
	static {
		// ATTENTION : ne peut PAS être intégré à createChaine(),
		// on peut avoir besoin de ces valeurs avant création d'un FilterProperties
		BEGINNER = Aladin.aladin.chaine.getString("FTBEGINNER");
		ADVANCED = Aladin.aladin.chaine.getString("FTADVANCED");
		BEGINNER_FILTER = new String[]{
		      Aladin.aladin.chaine.getString("FTDEMO1"),
		      Aladin.aladin.chaine.getString("FTDEMO2"),
		      Aladin.aladin.chaine.getString("FTDEMO3"),
		      Aladin.aladin.chaine.getString("FTDEMO4"),
		      Aladin.aladin.chaine.getString("FTDEMO5"),
		      Aladin.aladin.chaine.getString("FTDEMO6")
		};
	}

	@Override
    protected void createChaine() {
	   super.createChaine();
	   SELECT = aladin.chaine.getString("FTSELECT");
	   EXPORT = aladin.chaine.getString("FTEXPORT");
	   UCD = aladin.chaine.getString("FTUCD");
	   FILTER_EXAMPLES = aladin.chaine.getString("FTFILTER_EXAMPLES");
	   FILTERMANUAL = aladin.chaine.getString("FTFILTERMANUAL");
	   SAVEFILTER = aladin.chaine.getString("FTSAVEFILTER");
	   LOADFILTER = aladin.chaine.getString("FTLOADFILTER");
	   PICKCOLUMN = aladin.chaine.getString("FTPICKCOLUMN");
	   PICKUCD = aladin.chaine.getString("FTPICKUCD");
	   SHAPEFUNC = aladin.chaine.getString("FTSHAPEFUNC");
	   COLORFUNC = aladin.chaine.getString("FTCOLORFUNC");
	   IOERR = aladin.chaine.getString("FTIOERR");
	   CAVEAT = aladin.chaine.getString("FTCAVEAT");
	   MATHS = aladin.chaine.getString("FTMATHS");
	   OPEN_COLUMNS = aladin.chaine.getString("FTOPEN_COLUMNS");
	   LABEL = aladin.chaine.getString("FTLABEL");
	   CHOOSE = aladin.chaine.getString("FTCHOOSE");
	   CHOOSE1 = aladin.chaine.getString("FTCHOOSE1");
	   PREDEF = aladin.chaine.getString("FTPREDEF");
	   YOUROWN = aladin.chaine.getString("FTYOUROWN");
	   PICK = aladin.chaine.getString("FTPICK");
	   COL = aladin.chaine.getString("FTCOL");
	   ACTION = aladin.chaine.getString("FTACTION");
	   UNIT = aladin.chaine.getString("FTUNIT");
	   CPLANE = aladin.chaine.getString("FTCPLANE");
	   FREQUCD = aladin.chaine.getString("FTFREQUCD");
	   CATUCD = aladin.chaine.getString("FTCATUCD");
	   SHAPEFCT = aladin.chaine.getString("FTSHAPEFCT");
	   COLORFCT = aladin.chaine.getString("FTCOLORFCT");
	   CAVEATMSG = aladin.chaine.getString("FTCAVEATMSG");
	   FILTER_EXPLAIN = aladin.chaine.getString("FTFILTER_EXPLAIN");
	   RAINBOWCM = aladin.chaine.getString("FTRAINBOWCM");

	   UNITEXPLAIN= new String[]{
		      aladin.chaine.getString("FTMIN"),
		      aladin.chaine.getString("FTSEC"),
		      aladin.chaine.getString("FTDEG"),
		      aladin.chaine.getString("FTEV"),
		      aladin.chaine.getString("FTJY"),
		      aladin.chaine.getString("FTMAG"),
		      aladin.chaine.getString("FTPSC"),
		      aladin.chaine.getString("FTRAD"),
		      aladin.chaine.getString("FTYEAR"),
		      aladin.chaine.getString("FTWATT"),
	   };
	}

	/** Creation du Frame donnant les proprietes d'un PlanFilter
	 * @param p le plan concerné
	 */
	 protected FilterProperties(Plan p) {
        super(p);
        pf = (PlanFilter)p;
        majFilterProp(true,false);
        aladin.lastFilterCreated = this;

	 }


	  // partie supprimée
	  /*
	  // met à jour l'état du plan lorsqu'on n'a pas besoin de reconstruire tout le panel
	  private void majState(PlanFilter p) {
		   // maj de viewable/hidden
		   hiddenCb.setState(!p.active);
		   viewableCb.setState(p.active);
	  }
	  */

	// Initialisation d'un JPanel en fonction d'un plan
	@Override
    void showProp() {
	   curWindow = this;
	   //flagHide=false;

	   if( panel!=null ) {
	   	  if( !oscript.equals(pf.script) ) filterDef.setText(pf.script);
	   	  if( !olabel.equals(pf.label) ) label.setText(pf.label);
	   }

	   oscript = pf.script;
	   olabel = pf.label;

	   if( hcmemo==pf.hashCode() ) {
		  if( isShowing() ) return;
		  setVisible(true);
		  return;
	   }

	   hcmemo = pf.hashCode();

       // On remet a null ce qui est necessaire
       // partie supprimée
	   //state=null;
	   saveDef = "";
       saveName = "";

       if( panel!=null ) getContentPane().remove(panel);

	   panel = new JPanel();
	   panel.setLayout( new BorderLayout(5,5) );

       propPanel = getPanelProperties();
       
	   Aladin.makeAdd(panel,propPanel,"Center");
	   Aladin.makeAdd(panel,getPanelValid(),"South");

       // ajout d'une bordure
      setTitre(LABEL+" \""+pf.label+"\"");

	   Aladin.makeAdd(this.getContentPane(),panel,"Center");

	   pack();
	   setVisible(true);
	}


	/** Construction du panel des boutons de validation
	 * @return Le panel contenant les boutons Apply/Close
	 */
	 @Override
    protected JPanel getPanelValid() {
		JPanel p = new JPanel();
		p.setLayout( new FlowLayout(FlowLayout.CENTER) );
		p.setFont( Aladin.LBOLD );
        applyBtn = new JButton(APPLY);
        applyBtn.addActionListener(this);
		p.add( applyBtn );
        closeBtn = new JButton(CLOSE);
        closeBtn.setFont(Aladin.PLAIN);
        closeBtn.addActionListener(this);
		p.add( closeBtn );
		return p;
	 }

	/** Construction du panel des proprietes du plan courant.
	 * @return Le panel des proprietes du plan courant
	 */
	 @Override
    protected JPanel getPanelProperties() {
		GridBagConstraints c = new GridBagConstraints();
		GridBagLayout g =  new GridBagLayout();
		c.fill = GridBagConstraints.BOTH; // J'agrandirai les composantes
        c.insets = new Insets(2,2,3,5);

		JPanel p = new JPanel();
		p.setLayout(g);

		// petit texte d'explication sur les filtres
		JLabel explain = new JLabel(FILTER_EXPLAIN, JLabel.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		g.setConstraints(explain,c);
		p.add(explain);

		// le label associe au plan
		label = new JTextField(pf.label,15);
		addCouple(p, LABEL, label, g,c);

		 // si le plan est un PlanFilter et que la def est vide, il est actif par defaut
		if( pf.script.length()==0 ) pf.active = true;

		// si la définition du script n'est pas vide, ou que l'on est en mode robot, on force le mode Advanced
		if( pf.script!=null && pf.script.length()>0 || (Aladin.ROBOTSUPPORT && aladin.command.robotMode) ) currentMode = ADVANCED;
		else currentMode = BEGINNER;

        // panel du choix du mode (débutant ou avancé)
		modeTabbedPane = new JTabbedPane();
		modeTabbedPane.addTab(BEGINNER, getBeginnerPanel());
		modeTabbedPane.addTab(ADVANCED, getAdvancedPanel());

		// on montre le bon panel
		modeTabbedPane.setSelectedIndex(currentMode==BEGINNER?0:1);

		c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = c.weighty = 1.0;
        c.insets = new Insets(0,0,0,0);
        c.fill = GridBagConstraints.BOTH;
		g.setConstraints(modeTabbedPane,c);
		p.add(modeTabbedPane);
		
        return p;
	 }

     /**
      * Retourne la liste des descriptions des filtres pour débutants
      * en préfixant d'éventuels / par \ (pour la construction de la JBar)
      * PF 24/8/2007
      */
     static protected String[] getBeginnerFilters() {
        String res[] = new String[BEGINNER_FILTER.length];
        for( int i=0; i<res.length; i++ ) res[i]=Util.slash(BEGINNER_FILTER[i]);
        return res;
     }

     /**
      * Construit le panel du mode débutant
      * @return le panel mode débutant
      */
     private JPanel getBeginnerPanel() {
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g =  new GridBagLayout();
         c.fill = GridBagConstraints.NONE;

         JPanel p = new JPanel();
         p.setLayout(g);

         p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder(null, CHOOSE, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                 BorderFactory.createEmptyBorder(5,5,5,5))));

         // Possibilite de choisir dans la liste deroulante un filtre predefini
         c.gridwidth = GridBagConstraints.REMAINDER;
         c.anchor = GridBagConstraints.WEST;

         beginnerCbg = new ButtonGroup();
         // création des checkbox pour choisir un des filtres "débutant"
         JRadioButton cb;
         String label;
         for( int i=0; i<BEGINNER_FILTER.length; i++ ) {
             label = " "+BEGINNER_FILTER[i];
             cb = new JRadioButton(label, false);
             beginnerCbg.add(cb);
             cb.addActionListener(this);
             g.setConstraints(cb, c);
             p.add(cb);
         }

         c.insets = new Insets(30,0,0,0);
         c.anchor = GridBagConstraints.CENTER;
         JButton caveatBtn = new JButton(CAVEAT);
         caveatBtn.addActionListener(this);
         caveatBtn.setFont(Aladin.BOLD);
         g.setConstraints(caveatBtn, c);
         p.add(caveatBtn);

         return p;
     }

     public static void addCouple(JPanel p, Object titre, Component valeur,
            GridBagLayout g, GridBagConstraints c) {

        Component t;

        if (titre instanceof String) {
            JLabel l = new JLabel((String) titre);
            l.setFont(l.getFont().deriveFont(Font.ITALIC));
            t = l;
        } else
            t = (Component) titre;

        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 0.0;
        g.setConstraints(t, c);
        p.add(t);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        g.setConstraints(valeur, c);
        p.add(valeur);

    }

	 /**
	  * Construit le panel du mode avancé
	  * @return le panel mode expert
*/
	 private JPanel getAdvancedPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));


		// Possibilite de choisir dans la liste deroulante un filtre predefini
		// création et ajout d'éléments à predefFilters
		majPredefFilters();
        JLabel l;
        JPanel predefPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
        predefPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, CHOOSE1, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                BorderFactory.createEmptyBorder(5,5,5,5))));

        predefPanel.add(l = new JLabel(PREDEF));
        l.setFont(l.getFont().deriveFont(Font.ITALIC));
        predefPanel.add(predefFilters);
        p.add(predefPanel);


        GridBagConstraints c = new GridBagConstraints();
        GridBagLayout g =  new GridBagLayout();
        c.fill = GridBagConstraints.BOTH; // J'agrandirai les composantes
        JPanel defPanel = new JPanel();
        defPanel.setLayout(g);

        defPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, YOUROWN, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                BorderFactory.createEmptyBorder(5,5,5,5))));
        p.add(defPanel);

        // exemple de filtre
        JLabel lEg = new JLabel("eg: ${Bmag}<10 {draw red square}");
		lEg.setFont(Aladin.ITALIC);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		g.setConstraints(lEg,c);
		defPanel.add(lEg);


        JLabel pickLabel = new JLabel(PICK);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		pickLabel.setFont(Aladin.BOLD);
		g.setConstraints(pickLabel,c);
		defPanel.add(pickLabel);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;


		// JPanel avec les 4 boutons permettant d'afficher les popup d'aide
		JPanel popupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));

		if( columnMenu==null ) createPopup();
        Insets insets = new Insets(2,5,2,5);
        ImageIcon icon = new ImageIcon(aladin.getImagette("arrow.gif"));
        // accès aux colonnes
        final JButton colBtn = new JButton(COL);
        popupPanel.add(colBtn);
        colBtn.setIcon(icon);
        colBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        colBtn.setMargin(insets);
        colBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent mouseEvent)  {
                columnMenu.show(colBtn,mouseEvent.getX(),mouseEvent.getY());
            }
        });
        // accès aux UCD
        final JButton ucdBtn = new JButton("UCDs");
        popupPanel.add(ucdBtn);
        ucdBtn.setIcon(icon);
        ucdBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        ucdBtn.setMargin(insets);
        ucdBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent mouseEvent)  {
                ucdMenu.show(ucdBtn,mouseEvent.getX(),mouseEvent.getY());
            }
        });
        // accès aux différentes 'actions'
        final JButton actionBtn = new JButton(ACTION);
        popupPanel.add(actionBtn);
        actionBtn.setIcon(icon);
        actionBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        actionBtn.setMargin(insets);
        actionBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent mouseEvent)  {
                actionMenu.show(actionBtn,mouseEvent.getX(),mouseEvent.getY());
            }
        });
        // accès aux fonctions mathématiques
        JButton mathBtn = new JButton(MATHS);
        popupPanel.add(mathBtn);
        mathBtn.addActionListener(this);
        mathBtn.setMargin(insets);
        mathBtn.setIcon(null);
        // accès aux unités
        final JButton unitBtn = new JButton(UNIT);
        popupPanel.add(unitBtn);
        unitBtn.setIcon(icon);
        unitBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        unitBtn.setMargin(insets);
        unitBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent mouseEvent)  {
                unitMenu.show(unitBtn,mouseEvent.getX(),mouseEvent.getY());
            }
        });

		c.gridx = 1;
		g.setConstraints(popupPanel,c);
		defPanel.add(popupPanel);
		c.gridx = 0;

		JPanel bigPanel = new JPanel();
		bigPanel.setLayout(new BorderLayout(0,0));

		// JPanel avec les 2 boutons d'aide
		JPanel helpPanel = new JPanel();
		GridBagLayout gHelp = new GridBagLayout();
		GridBagConstraints cHelp = new GridBagConstraints();

		helpPanel.setLayout(gHelp);

		// Bouton pour obtenir le manuel d'aide
		JButton manualFilterBtn = createButton(FILTERMANUAL);
		manualFilterBtn.setFont(Aladin.LBOLD);

		// Bouton ouvrant la frame d'aide avec les exemples
		JButton helpFilterBtn = createButton(FILTER_EXAMPLES);
		helpFilterBtn.setFont(Aladin.LBOLD);

		cHelp.gridwidth = GridBagConstraints.REMAINDER;
		cHelp.insets = new Insets(5,0,5,3);
		cHelp.fill = GridBagConstraints.HORIZONTAL;
		gHelp.setConstraints(manualFilterBtn, cHelp);
		helpPanel.add(manualFilterBtn);
		gHelp.setConstraints(helpFilterBtn, cHelp);
		helpPanel.add(helpFilterBtn);


		bigPanel.add(helpPanel, BorderLayout.WEST);

		// TextArea pour entrer sa propre definition de filtre
		// reduction a la demande de FOX (60 a 50)
		filterDef = new JTextArea(9,50);

		// ajout popupmenu avec Cut, Copy, Paste
		JPopupMenu popup = new JPopupMenu();
		JMenuItem item = new JMenuItem(new DefaultEditorKit.CutAction());
		item.setText("Cut");
		popup.add(item);
		item = new JMenuItem(new DefaultEditorKit.CopyAction());
        item.setText("Copy");
        popup.add(item);
        item = new JMenuItem(new DefaultEditorKit.PasteAction());
        item.setText("Paste");
        popup.add(item);
//        filterDef.setComponentPopupMenu(popup);   // JVM 1.5 - en attendant que Thomas corrige, ou que l'on passe à 1.5

        filterDef.setLineWrap(true);
		// ajout listener pour permettre de modifier la valeur de curWindow
		filterDef.addMouseListener(this);
        // fonte plus lisible que celle par defaut
		filterDef.setFont(Aladin.COURIER);
		filterDef.setText(pf.script);
        filterDefSp = new JScrollPane(filterDef,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		bigPanel.add(filterDefSp, BorderLayout.CENTER);
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		g.setConstraints(bigPanel,c);
		defPanel.add(bigPanel);

		// raz
		c.weighty = c.weightx = 0.0;

		// boutons permettant de sauvegarder/charger un filtre (mode STANDALONE ou SIGNEDAPPLET only)
		if( Aladin.hasNoResctriction() ) {
		   	JButton saveFilter = createButton(SAVEFILTER);
            saveFilter.setFont(Aladin.PLAIN);
		   	JButton loadFilter = createButton(LOADFILTER);
            loadFilter.setFont(Aladin.PLAIN);
		   	JPanel loadSavePanel = new JPanel();
		   	loadSavePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		   	loadSavePanel.add(saveFilter);
		   	loadSavePanel.add(loadFilter);

			JLabel bidon = new JLabel("");
			c.gridwidth = GridBagConstraints.RELATIVE;
			g.setConstraints(bidon,c);
			defPanel.add(bidon);

		   	c.gridx=1;
		   	c.gridwidth = GridBagConstraints.REMAINDER;
		   	g.setConstraints(loadSavePanel,c);
		   	defPanel.add(loadSavePanel);
		}



		if( computeCol ) updateUCDAndColumn();

		c.gridx=0;

		// Avril 2005 : le bouton EXPORT est de retour
		JPanel pBtns = new JPanel();
		GridBagConstraints cc = new GridBagConstraints();
		GridBagLayout gg =  new GridBagLayout();
		cc.fill = GridBagConstraints.BOTH;
		cc.insets = new Insets(1,3,1,3);
		pBtns.setLayout(gg);
		pBtns.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));

		JButton bExport = new JButton(EXPORT);
        bExport.addActionListener(this);
		JLabel lExport = new JLabel(CPLANE);

		cc.gridwidth = GridBagConstraints.RELATIVE;
        cc.weightx = 0.0;
        gg.setConstraints(bExport,cc);
        pBtns.add(bExport);

        cc.gridwidth = GridBagConstraints.REMAINDER;
        cc.weightx = 1.0;
        gg.setConstraints(lExport,cc);
        pBtns.add(lExport);


        cc.gridwidth = GridBagConstraints.REMAINDER;
        cc.anchor = GridBagConstraints.WEST;
        cc.weightx = 0.0;
        cc.fill = GridBagConstraints.NONE;
        showRainbowBtn = new JButton(RAINBOWCM);
        showRainbowBtn.addActionListener(this);
        showRainbowBtn.setEnabled(pf.getUCDFilter().hasRainbowFunction());
        gg.setConstraints(showRainbowBtn, cc);
        pBtns.add(showRainbowBtn);

		p.add(pBtns);

		return p;
	 }

     static private final Insets BUTTON_INSETS = new Insets(0,1,0,1);
     private JButton createButton(String s) {
         JButton b = new JButton(s);
         b.setMargin(BUTTON_INSETS);
         b.addActionListener(this);

         return b;
     }

	// Méthodes implémentant l'interface MouseListener
	public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
    	curWindow = this;
    }
    // en affichant le popup sur l'evt mouseReleased, on évite de mauvaises surprises liées au WM
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}


	 // crée et initialise le popup
	 private void createPopup() {
	     columnMenu = new JPopupMenu();
	     ucdMenu = new JPopupMenu();
	     actionMenu = new JPopupMenu();
	     unitMenu = new JPopupMenu();


	     // le remplissage de columnMenu se fait ailleurs
	     // remplissage partiel de ucdMenu
	     JMenu freqUcdMenu = new JMenu(FREQUCD);
	     JMenuItem mi;
         String label, key;
	     for( int i=0;i<UCDBASE.length;i++ ) {
             label = UCDBASE[i];
             if( useUcdExplain ) {
                 if( label.endsWith("*") ) key = label.substring(0,label.length()-1);
                 else key = label;
                 String explain = getUCDExplain(key, true);
                 if( explain.length()>0 ) label += " - "+explain;
             }
	         mi = new JMenuItem(label);
             mi.addActionListener(this);
	         mi.setActionCommand("$["+UCDBASE[i]+"]");
	         freqUcdMenu.add(mi);
	     }
	     currentUcdMenu = new JMenu(CATUCD);

	     ucdMenu.add(freqUcdMenu);
	     ucdMenu.add(currentUcdMenu);
	     ucdMenu.addSeparator();
	     ucdMenu.add(mi = new JMenuItem(PICKUCD));
         mi.addActionListener(this);

	     // remplissage de actionMenu
	     JMenu shapeMenu = new JMenu(SHAPEFCT);
	     JMenu colorMenu = new JMenu(COLORFCT);
         // les fonctions "de forme" disponibles
         // d'abord les fonctions à paramètre
         mi = new JMenuItem(Action.ELLIPSE.toUpperCase()+"(semi-maj. axis, semi-minor axis, pos. angle)");
         mi.setActionCommand(Action.DRAW+" "+Action.ELLIPSE+"(,,)");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.FILLELLIPSE.toUpperCase()+"(semi-maj. axis, semi-minor axis, pos. angle)");
         mi.setActionCommand(Action.DRAW+" "+Action.FILLELLIPSE+"(,,)");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.SIZE.toUpperCase()+"(parameter)");
         mi.setActionCommand(Action.DRAW+" "+Action.SIZE+"()");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.FILLSIZE.toUpperCase()+"(parameter)");
         mi.setActionCommand(Action.DRAW+" "+Action.FILLSIZE+"()");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.FIXEDCIRCLE.toUpperCase()+"(parameter)");
         mi.setActionCommand(Action.DRAW+" "+Action.FIXEDCIRCLE+"()");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.PM.toUpperCase()+"(proper motion RA, proper motion Dec)");
         mi.setActionCommand(Action.DRAW+" "+Action.PM+"(,)");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.RECTANGLE.toUpperCase()+"(width, height, pos. angle)");
         mi.setActionCommand(Action.DRAW+" "+Action.RECTANGLE+"(,,)");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         mi = new JMenuItem(Action.LINE.toUpperCase()+"(ra1, dec1, ra2, dec2)");
         mi.setActionCommand(Action.DRAW+" "+Action.LINE+"(,,,)");
         mi.addActionListener(this);
         shapeMenu.add(mi);

         shapeMenu.addSeparator();
         // puis les fonctions sans paramètre (faut-il vraiment les mettre ??)
         for(int i=0;i<Action.NOPARAMSHAPE.length;i++ ) {
             mi = new JMenuItem(Action.NOPARAMSHAPE[i]);
             mi.addActionListener(this);
             mi.setActionCommand(Action.DRAW+" "+Action.NOPARAMSHAPE[i]);
             shapeMenu.add(mi);
         }

		 // les fonctions de couleur disponibles

         // d'abord les fonctions à paramètre
         mi = new JMenuItem(Action.RGB.toUpperCase()+"(red param., green param., blue param.)");
         mi.addActionListener(this);
         mi.setActionCommand(Action.DRAW+" "+Action.RGB+"(,,)");
         colorMenu.add(mi);

         mi = new JMenuItem(Action.RAINBOW.toUpperCase()+"(parameter)");
         mi.addActionListener(this);
         mi.setActionCommand(Action.DRAW+" "+Action.RAINBOW+"()");
         colorMenu.add(mi);

         mi = new JMenuItem(Action.SATURATION.toUpperCase()+"(parameter)");
         mi.addActionListener(this);
         mi.setActionCommand(Action.DRAW+" "+Action.SATURATION+"()");
         colorMenu.add(mi);

         colorMenu.addSeparator();
         // puis les fonctions sans paramètre (faut-il vraiment les mettre ??)
         for(int i=0;i<Action.COLORNAME.length;i++ ) {
             mi = new JMenuItem(Action.COLORNAME[i]);
             mi.addActionListener(this);
             mi.setActionCommand(Action.DRAW+" "+Action.COLORNAME[i]);
             colorMenu.add(mi);
         }

         actionMenu.add(shapeMenu);
         actionMenu.add(colorMenu);

         // Unités

         for( int i=0; i<UNITBASE.length; i++ ) {
             mi = new JMenuItem(UNITBASE[i]+" - "+UNITEXPLAIN[i]);
             mi.addActionListener(this);
             mi.setActionCommand(UNITBASE[i]);
             unitMenu.add(mi);
         }
	 }

	 // reconstruit la liste des filtres
	 private void majPredefFilters() {
        if( predefFilters==null ) {
            predefFilters = new JComboBox();
            predefFilters.addActionListener(this);
        }
		createPredefFilters();
        predefFilters.setSelectedIndex(0);
	 }

	 // Mise-a-jour des filtres prédéfinis
	 private void createPredefFilters() {
		predefFilters.removeAllItems();
		predefFilters.addItem("----");
		// Remplissage du choice avec les labels des filtres predef.
		for(int i=0;i<PlanFilter.PREDEFLABELS.length;i++) {
		   predefFilters.addItem("** "+PlanFilter.PREDEFLABELS[i]+" **");
		}
		// Remplissage du choice avec les labels des filtres deja crees
		for(int i=0;i<=PlanFilter.num;i++) {
		   predefFilters.addItem(PlanFilter.saveLabels[i]);
		}
	 }

     /**
      * Applique le filtre débutant correspondant au checkbox sélectionné
      * @param cb le checkbox sélectionné
      */
	 private void applyBeginnerFilter(JRadioButton cb) {
    	if( cb==null ) {
    		pf.updateDefinition("",label.getText(),this);
    		filterDef.setText("");
    		return;
    	}
    	int idx = Util.indexInArrayOf(cb.getText().substring(1), BEGINNER_FILTER);
    	if( idx<0 ) return;

		pf.updateDefinition(BEGINNER_FILTERDEF[idx],label.getText(),this);
		filterDef.setText(pf.script);

        // affichage dans la console de la commande script équivalente
        aladin.console.printCommand("filter "+pf.label+" {\n"+pf.script+"\n}");
	 }

	 private void apply() {
         pf.label = label.getText();

         currentMode = modeTabbedPane.getSelectedIndex()==0?BEGINNER:ADVANCED;

		// thomas (filtres)
		// pour un PlanFilter, maj de la definition du filtre
        if( currentMode.equals(ADVANCED) ) {
            boolean wasValid = pf.isValid();

	        pf.updateDefinition(filterDef.getText(),label.getText(),this);

            // si on vient de corriger un filtre invalide, on l'active !
            if( !wasValid && pf.isValid() ) {
                pf.setActivated(true);
                pf.updateState();
                aladin.calque.select.repaint();
            }

			// au cas ou on a changé la déf. pour la rendre correcte
			filterDef.setText(pf.script);
			// on déselectionne dans le mode débutant
            // (c'est la seule façon que j'ai trouvée, les ButtonGroup ne permettant pas de déselection)
            beginnerCbg.setSelected(new JButton().getModel(), true);

	        // affichage dans la console de la commande script équivalente
	        aladin.console.printCommand("filter "+pf.label+" {\n"+pf.script+"\n}");
        }
        else {
        	// NB : pour le mode beginner, l'affichage dans la console
        	// de la commande script se fait dans applyBeginnerFilter
        	// (car on peut avoir choisi un filtre sans passer par apply)
            Enumeration e = beginnerCbg.getElements();
            JRadioButton cb = null;
            while( e.hasMoreElements() ) {
                cb = (JRadioButton)e.nextElement();
                if( cb.isSelected() ) break;
            }
        	applyBeginnerFilter(cb);
        }



		// on met a jour predefFilters (quand on a changé le label du filtre)
		//majPredefFilters(pf);
        majFilterProp(true,false);
		// mise a jour du textfield label (peut etre different de celui rentré, car il doit etre unique)
		label.setText(pf.label);
        setTitre(LABEL+" \""+pf.label+"\"");
		saveDef = filterDef.getText();
        saveName = label.getText();

        // rainbow color map
        showRainbowBtn.setEnabled(pf.getUCDFilter().hasRainbowFunction());

		aladin.view.repaintAll();
		aladin.calque.select.repaint();

	 }


	   protected String getCaveat() { return CAVEATMSG;}

	 // Gestion des evenements
	  public void actionPerformed(ActionEvent ae) {
          String what = ae.getActionCommand();
          Object target = ae.getSource();

         // Submit PLAN
		 if( APPLY.equals(what) ) apply();
         else if( CLOSE.equals(what) ) dispose();
		 else if (CAVEAT.equals(what)) Aladin.info(this, getCaveat());
		 else if( MATHS.equals(what) ) showMathHelper();

		 // action sur un des CheckBox pour choisir un filtre en mode Beginner
		 else if( target instanceof JRadioButton) {
		 	applyBeginnerFilter(((JRadioButton)target));
		 }

         else if( what.equals(RAINBOWCM)) {
             double[] minmax = pf.getUCDFilter().getRainbowMinMax();
             aladin.view.showRainbowFilter(CanvasColorMap.getRainbowCM(false), minmax[0], minmax[1]);
             aladin.view.getCurrentView().rainbowF.setTitle(pf.label);

             aladin.view.getCurrentView().repaint();
         }

		 // action sur un MenuItem
		 else if( target instanceof JMenuItem ) {

		 	if( what.equals(PICKCOLUMN) || what.equals(PICKUCD) ) {
		 		if( pickupHelper==null ) {
		 			pickupHelper = new PickupHelperFrame();
		 		}
		 		pickupHelper.setVisible(true);
		 		pickupHelper.toFront();
		 	}

             if( what.equals(PICKCOLUMN) ) {
             	// un seul mode à la fois
                pickColumnMode = true;
                pickUCDMode = false;
                pickupFP = this;
             }
             else if( what.equals(PICKUCD) ) {
                 // un seul mode à la fois
                 pickUCDMode = true;
                 pickColumnMode = false;
                 pickupFP = this;
             }
             else if( what.equals(OPEN_COLUMNS) ) {
             	showColumnHelper();
             }
             else {
                 JMenuItem mi = (JMenuItem)target;
                 String toInsert = mi.getActionCommand();
                 toInsert = adjustText(toInsert);
                 int pos = insertInTA(filterDef, toInsert, filterDef.getCaretPosition());
                 int newPos = pos;
                 int idx;
                 // ajustement fin de la position du curseur pour aider l'utilisateur
                 if ((idx = toInsert.indexOf('('))>0 ) {
                     newPos = pos-toInsert.length()+idx+1;
                 }
                 filterDef.setCaretPosition(newPos);
                 if (toInsert.startsWith("${") || toInsert.startsWith("$[")) {
                     adjustCaretPos();
                 }
                 filterDef.requestFocus();
             }

		 }

		 // Choix d'un filtre predefini
		 else if( predefFilters!=null && predefFilters.equals(target) ) {
             if( filterDef==null ) return;

			int index = predefFilters.getSelectedIndex();
			if( oldIndex==0 ) {
			   saveDef = filterDef.getText();
               saveName = label.getText();
			}
			oldIndex=index;
            if( index==-1 ) {
                return;
            }
			if( index==0 ) {
			   filterDef.setText(saveDef);
               label.setText(saveName);
			} else {
			   // dans ce cas, on tape dans les filtres exemple
			   if( index-1 < PlanFilter.PREDEFFILTERS.length ) {
				   // le premier item du choice est "---", il faut prendre index-1
				   filterDef.setText(PlanFilter.PREDEFFILTERS[index-1]);
                   String predefName = getPredefName(PlanFilter.PREDEFFILTERS[index-1]);
                   if( predefName!=null ) label.setText(predefName);
			   }
			   // sinon on tape dans les filtres crees par l'utilisateur
			   else {
				   filterDef.setText(PlanFilter.saveFilters[index-1-PlanFilter.PREDEFFILTERS.length]);
                   label.setText(saveName);
			   }
			}
            // on met la scrollbar verticale au début du texte
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    filterDefSp.getVerticalScrollBar().setValue(0);
                }
            });
		 }

		 // Ouverture du manuel des filtres
		 else if( FILTERMANUAL.equals(what) ) {
			aladin.glu.showDocument("Aladin.filterManual","");
		 }

/* supprimé pour le moment

         // Ouverture du browser d'UCD
         else if( UCD.equals(what) ) {
            aladin.glu.showDocument("Http",URLUCD,true);
         }

		 // Selection des sources par le PlanFilter
		 else if( SELECT.equals(what) ) {
			((PlanFilter)plan).select();
		 }

*/
		 // Avril 2005 : cette fonctionnalité fait son retour
		 // Export des sources selectionnees vers un nouveau plan
		 else if( EXPORT.equals(what) ) {
			pf.export();
		 }

        // affichage de la fenetre d'aide
		 else if( FILTER_EXAMPLES.equals(what) ) {
			FilterHelp fh = FilterHelp.getInstance();
			if( !fh.isShowing() ) {
			   fh.setVisible(true);
			}
			fh.toFront();
		 }

		 // sauvegarde d'un filtre
		 else if( SAVEFILTER.equals(what) ) {
		 	showFileDialog(FileDialog.SAVE);
		 	saveFilter();
		 }
		 // chargement d'un filtre
		 else if( LOADFILTER.equals(what) ) {
		 	showFileDialog(FileDialog.LOAD);
		 	loadFilter();
		 }
	  }

	  /**
	   * ajustement fin de la position du curseur
	   * si on a inséré un nom de colonne/UCD dans une fonction (comme rgb(,,) par exemple)
	   */
	  private void adjustCaretPos() {
	      int pos = filterDef.getCaretPosition();
	      String text = filterDef.getText();
	      if (text.length()>pos && text.charAt(pos)==',') {
	          filterDef.setCaretPosition(pos+1);
	      }
	  }

	  /**
	   * ajuste le texte à insérer au contexte
	   * eg : si on a déja draw blue, et qu'on clique sur 'rhomb', on
	   * ajoutera seulement rhomb et non 'draw rhomb'
	   * @param s
	   * @return
	   */
	  private String adjustText(String s) {
	      if (! s.startsWith("draw ")) {
	          return s;
	      }
	      String text = filterDef.getText().substring(0, filterDef.getCaretPosition());
	      int pos1 = -1;
	      for (int i=text.length()-1; i>=0; i--) {
	          if (text.charAt(i)=='\n') {
	              pos1 = i;
	              break;
	          }
	          if (text.charAt(i)=='{') {
	              if (i==0) {
	                  pos1 = i;
	              }
	              if( text.charAt(i-1)!='$') {
	                  pos1 = i;
	                  break;
	              }
	          }
	      }
	      int pos2 = text.lastIndexOf("draw");
	      if(pos2<0) {
	          return s;
	      }
	      if (pos1<pos2 ) {
	          return s.substring(5)+" ";
	      }
	      return s;
	  }

		/** Affiche la fenetre d'aide à la syntaxe sur les colonnes */
		private void showColumnHelper() {
			curWindow = this;
			// lazy creation
			if( columnHelper==null ) {
				columnHelper = new ColumnHelperFrame();
				Point p = getRightTopPos();
				columnHelper.setLocation(p.x,p.y);
			}
			else {
				// update the frame
				columnHelper.updateFrame();
			}

			columnHelper.show();
			columnHelper.toFront();
		}

		/** Affiche la fenetre d'aide à la syntaxe pour les fonctions mathematiques */
		private void showMathHelper() {
			curWindow = this;
			// lazy creation
			if( mathHelper==null ) {
				mathHelper = new MathHelperFrame();
				Point p = getRightTopPos();
				int shift = Aladin.SCREENSIZE.height-p.y;
			   	if( shift<60 ) p.y -= 60-shift;
				mathHelper.setLocation(p.x,p.y+350);
			}

			mathHelper.show();
			mathHelper.toFront();
		}

    /** Return the default name of a predefined filter
     * It is the first word of the first comment line
     * @param def
     * @return String
     */
    private String getPredefName(String def) {
        StringTokenizer st = new StringTokenizer(def,"\n");
        String curLine="";
        while( st.hasMoreTokens() && (curLine=st.nextToken().trim()).charAt(0)!='#' ) ;
        if( curLine.charAt(0)=='#' ) return curLine.substring(1).trim();
        else return null;
    }

    /** trie le tableau t dans l'ordre lexicographique (ne tient pas compte de majuscules/minuscules */
    static void sortLexico(String[] t) {
        MetaDataTree.sort(t, null, true);
    }

	  /** Méthode appelée lorsqu'un nouveau PlanCatalog est prêt */
	  static protected void notifyNewPlan() {
          majFilterProp(false,true);
	  }

      // FilterProperties sur lequel on a demandé un "pickup"
      private static FilterProperties pickupFP;
	  /** fonction appelée lorsqu'on clique dans le canvas des mesures
	   *
	   * @param s la source correspondant à la ligne cliquée
	   * @param index l'indice de la colonne cliquée
	   * @return true si on était en mode pickColumn ou pickUCD et que la fenêtre est montrée
	   */
	  static protected boolean clickInMesure(Source s, int index) {
            if( pickupFP==null || !pickupFP.isShowing() ) return false;
            pickupHelper.setVisible(false);
	  		if( index>=s.leg.field.length ) return true;
	  		if( pickColumnMode ) {
	  			String col = s.leg.field[index].name;
	  			col = col!=null?col:"";

	  			int pos = insertInTA(pickupFP.filterDef, "${"+col+"}", pickupFP.filterDef.getCaretPosition());
                pickupFP.filterDef.setCaretPosition(pos);
                pickupFP.adjustCaretPos();
                pickupFP.toFront();

                pickupFP.filterDef.requestFocus();

				pickColumnMode = false;
				return true;
	  		}
			if( pickUCDMode ) {
				String ucd = s.leg.field[index].ucd;
				ucd = ucd!=null?ucd:"";

				int pos = insertInTA(pickupFP.filterDef, "$["+ucd+"]", pickupFP.filterDef.getCaretPosition());
                pickupFP.filterDef.setCaretPosition(pos);
                pickupFP.toFront();

                pickupFP.filterDef.requestFocus();

				pickUCDMode = false;
				return true;
			}

	  		return false;
	  }

      private static String[] ucds;
//      private static String[] columns;

      private boolean firstTime = true;

	  /** Remplissage des menus d'aide à la syntaxe
	   */
	  private void updateUCDAndColumn() {
        Aladin.trace(3,"Updating UCD and column names in FilterProperties");
        // la première fois, il faut récupérer les ucds et nom de colonnes
        if( ucds==null /*|| columns==null */ ) {
            getUcdsAndColumns(aladin);
        }

        JMenuItem mi;
        if( firstTime ) {
            columnMenu.add(mi = new JMenuItem(OPEN_COLUMNS));
            mi.addActionListener(this);
    		columnMenu.addSeparator();

    		// pour sélectionner une colonne dans la frame des mesures
    		columnMenu.add(mi = new JMenuItem(PICKCOLUMN));
            mi.addActionListener(this);
    		firstTime = false;
        }

//	  	columnMenu.removeAll();
		currentUcdMenu.removeAll();


		/*
		// ajout de tous les noms de colonnes
		for( int i=0;i<columns.length;i++ ) {
		    mi = new MenuItem(columns[i]);
		    mi.setActionCommand("${"+columns[i]+"}");
		    columnMenu.add(mi);
		}
		*/

        // ajout de tous les noms d'UCD
        String label;
        for( int i=0;i<ucds.length;i++ ) {
            label = ucds[i];
            //if( useUcdExplain && ucdExplain[i]!=null ) label += " - "+ucdExplain[i];
            if( useUcdExplain ) {
            	String explain = getUCDExplain(ucds[i], true);
                if( explain.length()>0 ) label += " - "+explain;
            }

            mi = new JMenuItem(label);
            mi.addActionListener(this);
            mi.setActionCommand("$["+ucds[i]+"]");
            currentUcdMenu.add(mi);
        }

        computeCol = false;
	  }

	  /**
	   * Retourne l'explication pour un UCD donné
	   *
	   * @param ucd
	   * @return
	   */
	static String getUCDExplain(String ucd, boolean truncate) {
		if( ucd==null ) return "";


		// à partir de combien de caractères tronque-t-on ?
		int limit = 70;

		String explain;
		if( isUCD1Plus(ucd) ) {
			// les UCD1+ ont été entrés en lowercase dans Aladin.string
			ucd = ucd.toLowerCase();
			StringTokenizer st = new StringTokenizer(ucd, ";");
			explain = "";
			int idx = 0;
			String partExplain;
			while( st.hasMoreTokens() ) {
				partExplain = Aladin.aladin.chaine.getString(st.nextToken());
//				//  si on n'a pas trouvé la chaine correspondante
				if( partExplain.length()>0 && partExplain.charAt(0)=='[' ) partExplain = "?";
				if( idx>0 ) explain += "/";
				explain += partExplain;
				idx++;
			}
//			String s;
//			int idx = ucd.indexOf(';');
//			if( idx>0 ) s = ucd.substring(0, idx);
//			else s = ucd;

//			explain =  Aladin.aladin.chaine.getString(s);


		}
		else {
			explain = Aladin.aladin.chaine.getString(ucd);
			// si on n'a pas trouvé la chaine correspondante
			if( explain.length()>0 && explain.charAt(0)=='[' ) {
				explain = "?";
			}
		}

		// doit-on couper la chaine ?
		if( truncate && explain.length()>limit ) {
			explain = explain.substring(0, limit)+"...";
		}

		return explain;
	}

	  /**
	   *
	   * @param ucd
	   * @return true si il s'agit d'un UCD1+, false sinon
	   */
	static boolean isUCD1Plus(String ucd) {
		// méthode empirique : si ça contient des '_' --> UCD1
		// si ça contient des ';' --> UCD1+
		// sinon, si c'est en majuscule --> UCD1
		// sinon --> UCD1+

		if( ucd.indexOf('_')>0 ) return false;
		if( ucd.indexOf(';')>0 ) return true;

		if( ucd.toUpperCase().equals(ucd) ) return false;

		return true;

	}

      static void getUcdsAndColumns(Aladin aladin) {
         
          Aladin.trace(3,"Recompute all available columns and UCD");
//          Vector vCol = new Vector(); // pour mémoriser les noms de colonnes
          Vector vUCD = new Vector(); // pour mémoriser les noms des UCD

          Plan p;
          Obj[] o;
          Source s;
          String str;
          // boucle sur les plans : on récupères tous les noms de colonnes existant
          for( int i=aladin.calque.plan.length-1;i>=0;i-- ) {
            p = aladin.calque.plan[i];
            if( p.isCatalog() ) {
               Iterator<Obj> it = p.iterator();
               while( it!=null && it.hasNext() ) {
                  Obj o1 = it.next();
                  if( !(o1 instanceof Source) ) continue;
                  s = (Source)o1;
                  if( s.leg==null || s.leg.field==null ) {
                     System.out.println("Bizarre");
                  }
                  for( int k=s.leg.field.length-1;k>=0;k-- ) {
                     str = s.leg.field[k].name;
//                            if( vCol.indexOf(str)<0 ) vCol.addElement(str);
                     str = s.leg.field[k].ucd;
                     if( str!=null && str.length()>0 && vUCD.indexOf(str)<0 ) vUCD.addElement(str);
                  }
               }
            }
         }
//        columns = new String[vCol.size()];
//        vCol.copyInto(columns);
//        vCol = null;
        ucds = new String[vUCD.size()];
        vUCD.copyInto(ucds);
        vUCD = null;

        // on trie les noms de colonnes dans l'ordre alphabétique
//        sortLexico(columns);
        sortLexico(ucds);
      }

	  /** Sauvegarde le filtre dans un fichier */
	  private void saveFilter() {
		String dir = fd.getDirectory();
		String name =  fd.getFile();
		String s = (dir==null?"":dir)+(name==null?"":name);
		if( name==null ) return;

        // ajout suffixe ".ajs" si nécessaire
        if( ! s.endsWith(".ajs") ) s=s+".ajs";

		File file = new File(s);

		try {
			DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(file)));
				// on utilise writeBytespour éviter d'avoir un encodage 16-bit Unicode
                out.writeBytes("#AJS"+Util.CR);
				out.writeBytes("filter "+pf.label+" {"+Util.CR);
				out.writeBytes(pf.script);
				out.writeBytes(Util.CR+"}"+Util.CR);
				out.close();
		}
		catch(IOException e) {Aladin.error(this,IOERR+" : "+e);}

	  }

	  /** Charge une définition de filtre à partir d'un fichier */
	  private void loadFilter() {
		String dir = fd.getDirectory();
		String name =  fd.getFile();
		String s = (dir==null?"":dir)+(name==null?"":name);
		if( name==null ) return;
		File file = new File(s);

		String def = new String();
		String line;

		try {
			DataInputStream dis = new DataInputStream(
				new FileInputStream(file));
			while( (line = dis.readLine()) != null ) {
				if( def.length()>0 ) def += Util.CR;
				def += line;
			}
			dis.close();
	  	}
	  	catch(IOException e) {Aladin.error(this,IOERR+" : "+e,1);return;}

//        if( showHelp ) removeHelp();

        pf.updateDefinition(def,null,this);

		showProp();
        setTitre(LABEL+" \""+pf.label+"\"");
        majFilterProp(true,false);
		aladin.calque.select.repaint();
		aladin.view.setMesure();

	  }

      /** Fonctions statique permettant de mettre à jour les filtres prédéfinis/le popup d'aide
       *
       * @param majPredef true si on veut mettre à jour la liste de filtres prédéfinis
       * @param majPopup true si on veut mettre à jour le popupmenu
       */
      static protected void majFilterProp(boolean majPredef, boolean majPopup) {
          // màj UCDs+noms de colonnes
          if( majPopup ) getUcdsAndColumns(Aladin.aladin);

          Enumeration e = frameProp.elements();
          FilterProperties fp;

          while( e.hasMoreElements() ) {
            Properties propc = (Properties) e.nextElement();

            if( propc.plan.type==Plan.FILTER ) {
                fp = (FilterProperties)propc;
                if( majPredef ) fp.majPredefFilters();
                if( majPopup )  fp.updateUCDAndColumn();
            }
          }
      }

    /** la fonction insert de TextArea est bugguée sous Windows
     *  j'utilise donc cette fonction (c'est un peu lourd, mais ça marche partout)
     *
     *  @param t JTextArea dans lequel on insère
     *  @param text le texte à insérer
     *  @param pos la position d'insertion
     */
    static int insertInTA(JTextArea t, String text, int pos) {
        StringBuffer sb = new StringBuffer(t.getText());
        sb.insert(pos, text);
        t.setText(sb.toString());
        t.repaint();
        int nb = Action.countNbOcc('\r', t.getText().substring(0, pos));
        int newPos = pos+text.length()-nb;
        t.setCaretPosition(newPos);
        return newPos;
    }

	  /** Montre le sélecteur de fichiers */
	  private void showFileDialog(int mode) {
	  	String dir = null;
	  	if( fd!=null ) dir = fd.getDirectory();

	  	fd = new FileDialog(this,"",mode);
	  	if( dir!=null ) fd.setDirectory(dir);
	  	else aladin.setDefaultDirectory(fd);

	  	// Si on est en mode SAVE, le nom par défaut du fichier est le label du filtre
	  	if( mode==FileDialog.SAVE ) {
	  		fd.setFile(pf.label+".ajs");
	  		fd.setTitle(SAVEFILTER);
	  	}
	  	else {
	  		fd.setFile("");
	  		fd.setTitle(LOADFILTER);
	  	}
		fd.show();
	  }

	  /** Cache la fenetre des Properties et remonte le bouton
	   * des properties */
	   public void dispose() {
		  //flagHide=true;
		  //aladin.toolbox.tool[ToolBox.FILTER].mode=Tool.UP;
		  //aladin.toolbox.toolMode();
		  //aladin.toolbox.repaint();
          if( pickupFP == this ) {
          	pickupFP=null;
          	pickupHelper.hide();
          }
          closeHelperFrames();
		  super.dispose();
	   }

	   // ferme les frames d'aide si this==curWindow
	   private void closeHelperFrames() {
	   	if( curWindow!=this) return;
	   	if( columnHelper!=null ) columnHelper.hide();
	   	if( mathHelper!=null ) mathHelper.hide();
	   }

	   // Gestion des evenement
	   public boolean handleEvent(Event e) {
          // On cache le frame
		  if( e.id==Event.WINDOW_DESTROY ) dispose();
          return super.handleEvent(e);
	   }

	   private Point getRightTopPos() {
	   	Point p;
	   	p = this.getLocation();
	   	Dimension d = this.getSize();
	   	p.x += d.width;
	   	int shift = Aladin.SCREENSIZE.width-p.x;
	   	if( shift<100 ) p.x -= 100-shift;

	   	return p;
}
	   /** inner class pour aider à la rédaction des filtres (liste les colonnes)
	    *
	    * @author Thomas Boch [CDS]
	    */
	   class ColumnHelperFrame extends JFrame implements ActionListener {
	       static final String CLOSE = "Close";

	       /** Constructeur
	        */
	       ColumnHelperFrame() {
	           super("Available columns");
	           Aladin.setIcon(this);

//	           setBackground(Aladin.BKGD);
	           getContentPane().setLayout(new BorderLayout(0,0));


	           updateFrame();
	       }

	       /** Construit entièrement la Frame */
	       void updateFrame() {
	           getContentPane().removeAll();
	           JPanel p = new JPanel();
               p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	           GridBagLayout g =  new GridBagLayout();
	           GridBagConstraints c = new GridBagConstraints();
//	           c.insets = new Insets(20,5,0,5);
	           c.fill = GridBagConstraints.NONE;
	           c.gridwidth = GridBagConstraints.REMAINDER;
	           c.anchor = GridBagConstraints.WEST;
	           p.setLayout(g);

	           Plan plan;
	           Source s;

	           int nbCol = 4; // nb de colonnes pour les boutons

	           // loop over loaded catalogues
	           for (int i = 0; i < aladin.calque.plan.length; i++) {
	               plan = aladin.calque.plan[i];
	               if( !plan.isCatalog() || !plan.flagOk ) continue;

	               // loop over names of columns in current catalogue
//	               Vector v = new Vector();
	               Legende leg;
	               Legende oleg = null;

	               // panel avec l'ensemble des boutons
	               JPanel pBtns = new JPanel(new GridLayout(0,nbCol,2,2));
                   pBtns.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4),
                                   BorderFactory.createTitledBorder(null,plan.getLabel(),TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,Aladin.BOLD)),
                                   BorderFactory.createEmptyBorder(4,4,4,4)));
	               int idxBtn = 0;

	               Iterator<Obj> it = plan.iterator();
	               while( it.hasNext() ) {
	                  Obj o = it.next();
	                  if( !(o instanceof Source) ) continue;
	                  s = (Source)o;

	                  leg = s.leg;
	                  if( oleg!=null && leg==oleg ) continue;

	                  // on remplit si la ligne est incomplète
	                  while( idxBtn%nbCol!=0 ) {
	                     pBtns.add(new JLabel());
	                     idxBtn++;
	                  }

	                  // passer à la ligne ?
	                  JButton btn;
	                  for( int k=0;k<s.leg.field.length;k++ ) {

	                     btn = new JButton(s.leg.field[k].name);
	                     btn.setMargin(BUTTON_INSETS);
	                     btn.addActionListener(this);
	                     btn.setFont(Aladin.PLAIN);
	                     pBtns.add(btn);
	                     idxBtn++;
//	                           /*if( v.indexOf(str)<0 )*/??ajouter une ligne v.addElement(str);
	                     oleg = leg;
	                  }
	               }

	               g.setConstraints(pBtns, c);
	               p.add(pBtns);



//	               Enumeration e = v.elements();
//	               while( e.hasMoreElements() ) {
//	               System.out.println(e.nextElement());
//	               }

	           }

	           JScrollPane scroll = new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	           scroll.setSize(300,290);


//	           add(p, BorderLayout.CENTER);
	           getContentPane().add(scroll, BorderLayout.CENTER);

               JButton b;
	           JPanel p2 = new JPanel(new FlowLayout());
	           p2.add(b = new JButton(CLOSE));
               b.addActionListener(this);

	           getContentPane().add(p2, BorderLayout.SOUTH);

	           pack();
	       }

	       /** Gestion des evenements
	        */
	       public void actionPerformed(ActionEvent ae) {
	           String what = ae.getActionCommand();
               Object target = ae.getSource();

	           // on ferme !
	           if( CLOSE.equals(what) ) {
	               setVisible(false);
	           }
	           //
	           else {
	               String btnLabel = ((JButton)target).getText();
	               int pos = insertInTA(curWindow.filterDef, "${"+btnLabel+"}", curWindow.filterDef.getCaretPosition());
	               curWindow.filterDef.setCaretPosition(pos);
	               adjustCaretPos();
	               // les requestFocus trans-frames ne fonctionnent pas
	               curWindow.toFront();
	               curWindow.filterDef.requestFocus();
	           }
	       }

	       public boolean handleEvent(Event e) {
	           if( e.id==Event.WINDOW_DESTROY ) {
	               setVisible(false);
	           }
	           return super.handleEvent(e);
	       }

	       public void show() {
	           super.show();
	       }

	   } // end of inner class ColumnHelperFrame

	   class MathHelperFrame extends JFrame implements ActionListener {
	       static final String CLOSE = "Close";

	       /** Constructeur
	        */
	       MathHelperFrame() {
	           super("Available math operators/functions");
	           Aladin.setIcon(this);

//	           setBackground(Aladin.BKGD);
	           getContentPane().setLayout(new BorderLayout(0,0));

	           buildFrame();
	       }

	       /** Construit entièrement la Frame */
	       private void buildFrame() {
	           getContentPane().removeAll();
	           JPanel p = new JPanel();
	           GridBagLayout g =  new GridBagLayout();
	           GridBagConstraints c = new GridBagConstraints();
	           c.fill = GridBagConstraints.NONE;
	           c.gridwidth = GridBagConstraints.REMAINDER;
	           c.insets = new Insets(2,2,2,2);
	           c.anchor = GridBagConstraints.WEST;
	           p.setLayout(g);

	           // operators
	           JLabel l;
	           JButton b;
	           l = new JLabel("Operators");
	           l.setFont(Aladin.BOLD);
	           g.setConstraints(l, c);
	           p.add(l);
	           JPanel pOp = new JPanel(new FlowLayout(FlowLayout.LEFT));
	           for( int i=0; i<FrameColumnCalculator.OPERATORS.length; i++ ) {
	               b = new JButton(FrameColumnCalculator.OPERATORS[i]);
	               b.addActionListener(this);
	               b.setFont(Aladin.BOLD);
	               pOp.add(b);
	           }
	           g.setConstraints(pOp, c);
	           p.add(pOp);

	           // math functions
	           l = new JLabel("Functions");
	           l.setFont(Aladin.BOLD);
	           g.setConstraints(l, c);
	           p.add(l);
	           JPanel pFunc = new JPanel(new GridLayout(0,4,0,0));
	           String[] func = Parser.getAvailFunc();
	           for( int i=0; i<func.length; i++ ) {
	               b = new JButton(func[i]);
	               b.addActionListener(this);
	               b.setActionCommand(func[i]+"()");
	               pFunc.add(b);
	           }
	           g.setConstraints(pFunc, c);
	           p.add(pFunc);

	           // comparison operators
	           String[] compOp = {"=", "!=", "<", "<=", ">", ">=" };
	           l = new JLabel("Comparison operators");
	           l.setFont(Aladin.BOLD);
	           g.setConstraints(l, c);
	           p.add(l);
	           JPanel pCompOp = new JPanel(new FlowLayout(FlowLayout.LEFT));
	           for( int i=0; i<compOp.length; i++ ) {
	               b = new JButton(compOp[i]);
	               b.addActionListener(this);
	               b.setFont(Aladin.BOLD);
	               pCompOp.add(b);
	           }
	           g.setConstraints(pCompOp, c);
	           p.add(pCompOp);

	           // misc.
	           String[] misc = {"{", "}", "&&", "||", "\""};
	           l = new JLabel("Miscellaneous");
	           l.setFont(Aladin.BOLD);
	           g.setConstraints(l, c);
	           p.add(l);
	           JPanel pMisc = new JPanel(new FlowLayout(FlowLayout.LEFT));
	           for( int i=0; i<misc.length; i++ ) {
	               b = new JButton(misc[i]);
	               b.addActionListener(this);
	               b.setFont(Aladin.BOLD);
	               pMisc.add(b);
	           }
	           g.setConstraints(pMisc, c);
	           p.add(pMisc);

	           JScrollPane scroll = new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	           scroll.setSize(280,300);
	           scroll.setOpaque(false);

	           getContentPane().add(scroll, BorderLayout.CENTER);

	           scroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8,8,5,8),
	                   BorderFactory.createTitledBorder("")));

	           JPanel p2 = new JPanel(new FlowLayout());
	           p2.add(b = new JButton(CLOSE));
	           b.addActionListener(this);

	           getContentPane().add(p2, BorderLayout.SOUTH);

	           pack();
	       }



	       /** Gestion des evenements
	        */
	       public void actionPerformed(ActionEvent ae) {
	           String what = ae.getActionCommand();
	           Object target = ae.getSource();

	           // on ferme !
	           if( CLOSE.equals(what) ) {
	               setVisible(false);
	           }
	           //
	           else {
	               String btnLabel = ((JButton)target).getActionCommand();
	               int pos = insertInTA(curWindow.filterDef, btnLabel, curWindow.filterDef.getCaretPosition());
	               curWindow.filterDef.setCaretPosition(pos);
	               // les requestFocus trans-frames ne fonctionnent pas
	               curWindow.toFront();
	               curWindow.filterDef.requestFocus();
	           }
	       }

	       public boolean handleEvent(Event e) {
	           if( e.id==Event.WINDOW_DESTROY ) {
	               setVisible(false);
	           }
	           return super.handleEvent(e);
	       }

	       public void show() {
	           super.show();
	       }
	   } // end of inner class MathHelperFrame

	   /** inner class pour expliquer le fonctionnement de la feature "Pick a column/UCD"
	    *
	    * @author Thomas Boch [CDS]
	    */
	   class PickupHelperFrame extends JFrame implements ActionListener {
	       static final String HELP = "First, select some sources \n \n"+
	               "Then, click in the measurement panel on the given field \n"+
	               "you would like to use in your filter.";

	       static final String CLOSE = "Close";

	       /** Constructeur
	        */
	       PickupHelperFrame() {
	           super("Pick a column/a UCD");
	           Aladin.setIcon(this);
//	           setBackground(Aladin.BKGD);
	           getContentPane().setLayout(new BorderLayout(0,0));

	           updateFrame();
	       }

	       /** Construit entièrement la Frame */
	       void updateFrame() {
	           getContentPane().removeAll();

	           MyLabel l = new MyLabel(HELP);

	           getContentPane().add(l, BorderLayout.CENTER);
               l.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

               JButton b;
	           JPanel p2 = new JPanel(new FlowLayout());
	           p2.add(b = new JButton(CLOSE));
               b.addActionListener(this);

	           getContentPane().add(p2, BorderLayout.SOUTH);

	           pack();

	           // on positionne la frame qqpart de pas trop embetant
	           Point p = aladin.localisation.getLocation();
	           Component parent = aladin.localisation;

	           while( (parent = parent.getParent())!=null ) {
	               Point tmp = parent.getLocation();
	               p.x += tmp.x;
	               p.y += tmp.y;
	           }

	           setLocation(p.x, p.y+30);
	       }

	       /** Gestion des evenements
	        */
	       public void actionPerformed(ActionEvent ae) {
               String what = ae.getActionCommand();

	           if( what.equals(CLOSE) ) {
	               setVisible(false);
	           }
	       }

	       public boolean handleEvent(Event e) {
	           if( e.id==Event.WINDOW_DESTROY ) {
	               setVisible(false);
	           }
	           return super.handleEvent(e);
	       }

	   } // end of inner class PickupHelperFrame

	/**
	 * @return Returns the currentMode.
	 */
	protected static String getCurrentMode() {
		return currentMode;
	}
	/**
	 * @param currentMode The currentMode to set.
	 */
	protected static void setCurrentMode(String currentMode) {
		if( !currentMode.equals(BEGINNER) && !currentMode.equals(ADVANCED) ) return;
		FilterProperties.currentMode = currentMode;
	}
	/**
	 * @return Returns the curWindow.
	 */
	protected static FilterProperties getCurWindow() {
		return curWindow;
	}
}
