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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import cds.tools.TwoColorJTable;
import cds.tools.Util;


/**
 * Fenetre de gestion des macros
 * (utilisation des scripts avec liste de paramètres)
 *
 * @author Thomas Boch [CDS]
 *
 * @version 1.1 : ajout 'Load an example' --> exemple de script et de parametres
 * @version 0.9 : sept. 2006 - création
 */
public class FrameMacro extends JFrame {

	// Des labels
	static protected String SCRIPT, PARAMS, EXECUTE, LOAD_SCRIPT, SAVE_SCRIPT,
	                        LOAD_PARAMS, SAVE_PARAMS, IMPORT_PARAMS, ADD_COL, CLEAR_PARAMS,
							EXEC_ALL, EXEC_ALL_FROM_CURRENT, EXEC_CURRENT, EXEC_NEXT,
							FILE, HELP, LOADEX, SEEHELP, CLOSE, STOP, DELETE;

	// controleur faisant le lien entre la vue (cette classe) et le modèle de données
	private MacroController macroController;

	// boite de texte contenant les commandes scripts à exécuter
	private JTextPane scriptTP;

	// tableau comportant les valeurs des différents paramètres
	private JTable paramTable;

	// pour pouvoir effacer une ligne de params
	private JPopupMenu popup;

	// bouton permettant l'arret de l'execution en cours
	private JButton stopBtn;

	// référence à Aladin
	private Aladin a;

	// position de la souris dans le JTextPane
	int mousePosInScript = -1;

	Vector cmdWordsPos;

	private ScriptSyntaxRenderer scriptRenderer;

	// constructeur
	protected FrameMacro(Aladin a) {
		this.a = a;
        setIconImage(a.getImagette("AladinIconSS.gif"));

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Util.setCloseShortcut(this, false, a);

		macroController = new MacroController(this, a);

		createChaine();
		buildFrame();
	}

	/**
	 * Création des chaines de caractère nécessaires
	 *
	 */
	private void createChaine() {
		SCRIPT =                a.chaine.getString("FMSCRIPT");
		PARAMS =                a.chaine.getString("FMPARAMS");
		EXECUTE =               a.chaine.getString("FMEXEC");
		LOAD_SCRIPT =           a.chaine.getString("FMLOADSCRIPT");
		SAVE_SCRIPT =           a.chaine.getString("FMSAVESCRIPT");
		LOAD_PARAMS =           a.chaine.getString("FMLOADPARAMS");
		SAVE_PARAMS =           a.chaine.getString("FMSAVEPARAMS");
        IMPORT_PARAMS =         a.chaine.getString("FMIMPORTPARAMS");
		ADD_COL =               a.chaine.getString("FMADDCOL");
		CLEAR_PARAMS =          a.chaine.getString("FMCLEARPARAMS");
		EXEC_ALL =              a.chaine.getString("FMEXECALL");
		EXEC_ALL_FROM_CURRENT = a.chaine.getString("FMEXECALLFROMCURRENT");
		EXEC_CURRENT =          a.chaine.getString("FMEXECCURRENT");
		EXEC_NEXT =             a.chaine.getString("FMEXECNEXT");
		FILE =         a.chaine.getString("FMFILE");
		HELP =         a.chaine.getString("FMHELP");
        LOADEX =       a.chaine.getString("FMLOADEX");
		SEEHELP =      a.chaine.getString("FMSEEHELP");
		CLOSE =        a.chaine.getString("FMCLOSE");
		STOP =         a.chaine.getString("FMSTOP");
		DELETE =       a.chaine.getString("FMDELETE");
	}

	/**
	 * Construit la fenetre en entier
	 *
	 */
	private void buildFrame() {
		setTitle("Macros");
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout(5,5));
		((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
									buildScriptPanel(), buildParamPanel());
		splitPane.setResizeWeight(0.5);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		splitPane.setPreferredSize(new Dimension(300, 400));

		getContentPane().add(buildCommandPanel(), BorderLayout.SOUTH);

		// ajout de la barre de menu
		setJMenuBar(buildMenuBar());

		pack();

		splitPane.setDividerLocation(0.5);
	}

	private JPanel buildScriptPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(5,5));

		// zone de texte pour saisir les commande scripts
		scriptTP = new JTextPane();
        // très important : on force le séparateur de ligne à être "\n"
        // pour le document associé au textpane (sinon, pb en perspective entre Windows, Linux, Mac)
        scriptTP.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        Aladin.makeCursor(scriptTP, Aladin.TEXTCURSOR);
		scriptTP.addMouseMotionListener(macroController);
		scriptTP.addMouseListener(macroController);

		Dimension dim = new Dimension(300,100);
		scriptTP.setPreferredSize(dim);
		scriptTP.getDocument().addDocumentListener(scriptRenderer = new ScriptSyntaxRenderer());
		JScrollPane scroll = new JScrollPane(scriptTP);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(dim);
		panel.add(scroll, BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(SCRIPT),
                BorderFactory.createEmptyBorder(5,5,5,5))));


		return panel;
	}

	private JPanel buildParamPanel() {
		JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5,5));

		// ajout d'un bouton pour ajouter une colonne
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addCol = new JButton(ADD_COL);
		addCol.addActionListener(macroController);
		buttons.add(addCol);

		// ajout d'un bouton pour réinitialiser (on vide tout, et on met une colonne vide)
		JButton resetCol = new JButton(CLEAR_PARAMS);
		resetCol.addActionListener(macroController);
		buttons.add(resetCol);
		panel.add(buttons, BorderLayout.NORTH);

		// ajout de la JTable représentant la liste des paramètres
		paramTable = new TwoColorJTable();
		// couleur des lignes séparatrices
		paramTable.setGridColor(Color.lightGray);
		// on n'autorise la sélection que d'une ligne à la fois
		paramTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		paramTable.setModel(macroController.getMacroModel().getParamTableModel());
		paramTable.setDefaultEditor(String.class, macroController.getMacroModel().getTableCellEditor());
		// pour le popup sur click droit
		paramTable.addMouseListener(macroController);

		macroController.getMacroModel().getParamTableModel().initTable();
		macroController.getMacroModel().getParamTableModel().addEmptyCol();
		JScrollPane scrollPane = new JScrollPane(paramTable);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		panel.add(scrollPane, BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(PARAMS),
                BorderFactory.createEmptyBorder(5,5,5,5))));

		return panel;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		int modifier = Aladin.macPlateform?ActionEvent.META_MASK:ActionEvent.CTRL_MASK;
		// 'File'
		JMenu fileMenu = new JMenu(FILE);
		menuBar.add(fileMenu);
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenuItem item;
		fileMenu.add(item=new JMenuItem(LOAD_SCRIPT));
		item.addActionListener(macroController);
		item.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_S, modifier));
		fileMenu.add(item=new JMenuItem(SAVE_SCRIPT));
		item.addActionListener(macroController);
		fileMenu.add(new JSeparator());
		fileMenu.add(item=new JMenuItem(LOAD_PARAMS));
		item.addActionListener(macroController);
		item.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_P, modifier));
        fileMenu.add(item=new JMenuItem(IMPORT_PARAMS));
        item.addActionListener(macroController);
        item.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_I, modifier));
		fileMenu.add(item=new JMenuItem(SAVE_PARAMS));
		item.addActionListener(macroController);
		fileMenu.add(new JSeparator());
		fileMenu.add(item=new JMenuItem(CLOSE));
		item.addActionListener(macroController);

		// 'Help'
		JMenu helpMenu = new JMenu(HELP);
		menuBar.add(helpMenu);
        helpMenu.add(item = new JMenuItem(LOADEX));
        item.addActionListener(macroController);
        helpMenu.addSeparator();
		helpMenu.add(item = new JMenuItem(SEEHELP));
		item.addActionListener(macroController);


		return menuBar;
	}

	// panel de commande (exécution du script, etc)
	private JPanel buildCommandPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());

		// boutons d'exécution du script

		// première ligne
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton executeCurrent = new JButton(EXEC_CURRENT);
		executeCurrent.addActionListener(macroController);
		p1.add(executeCurrent);
		JButton executeNext = new JButton(EXEC_NEXT);
		executeNext.addActionListener(macroController);
		p1.add(executeNext);

		panel.add(p1, BorderLayout.NORTH);

		// seconde ligne
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton executeAll = new JButton(EXEC_ALL);
		executeAll.addActionListener(macroController);
		p2.add(executeAll);
		JButton executeAllFromCur = new JButton(EXEC_ALL_FROM_CURRENT);
		executeAllFromCur.addActionListener(macroController);
		p2.add(executeAllFromCur);
		stopBtn = new JButton(STOP);
		stopBtn.addActionListener(macroController);
		stopBtn.setEnabled(false);
		p2.add(stopBtn);


		panel.add(p2, BorderLayout.SOUTH);



		// TODO : possibilité de changer libelle des colonnes

		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(EXECUTE),
	            BorderFactory.createEmptyBorder(5,5,5,5)));

		return panel;
	}

	protected void setScript(String s) {
		scriptTP.setText(s);
		scriptTP.setCaretPosition(0);
		scriptTP.requestFocus();
	}

	protected String getScriptText() {
		return scriptTP.getText();
	}


	/** active/désactive le bouton stop */
	protected void setEnabledStopBtn(boolean b) {
		stopBtn.setEnabled(b);
	}

	private String cmdWordUnderMouse, ocmdWordUnderMouse;
	/** teste si pos correspond à une commande script, et le souligne si nécessaire
	 *  Mémorise le mot couramm
	 */
	protected void testIfColorLink(int pos) {
		mousePosInScript = pos;

		ocmdWordUnderMouse = cmdWordUnderMouse;
		if( cmdWordsPos==null ) return;
		Enumeration e = cmdWordsPos.elements();
		String w = null;
		int[] bounds;
		while( e.hasMoreElements() ) {
			bounds = (int[])e.nextElement();
			if( pos>=bounds[0] && mousePosInScript<bounds[1] ) {
				w = scriptTP.getText().substring(bounds[0], bounds[1]);
				break;
			}
		}
		cmdWordUnderMouse = w;
		// on force la coloration si on a un nouveau lien à dessiner ou à enlever (un peu bourrin,je sais)
		if( cmdWordUnderMouse!=ocmdWordUnderMouse ) {
			// passage au curseur de pointage
			if( cmdWordUnderMouse!=null) Aladin.makeCursor(scriptTP, Aladin.HANDCURSOR);
			// curseur par défaut
			else Aladin.makeCursor(scriptTP, Aladin.TEXTCURSOR);

			scriptRenderer.doColoringLater();
		}
//		System.out.println(mustColore);
//		System.out.println(cmdWordUnderMouse);
	}

	// indice de la ligne à mettre en valeur
	private int hlScriptRow = -1;
	private int ohlScriptRow = -1;
	/**
	 * Highlight a given script line
	 * @param row index of the row to highlight, -1 or negative value to stop highlighting a row
	 */
	protected void hilightScriptLine(int row) {
		// on force la coloration si on a une nouvelle ligne à mettre en valeur
		hlScriptRow = row;
		if( hlScriptRow!=ohlScriptRow ) {
			scriptRenderer.doColoringLater();
            // TODO : je ne sais pas comment forcer la position du scroll,
            // car je ne sais pas récupérer l'indice correspondant à une ligne
            try {
               if( row>=0 ) {
                   Element root = scriptTP.getDocument().getDefaultRootElement();
                   int idx = root.getElement(row).getStartOffset();
                   scriptTP.setCaretPosition(idx);
               }
            } catch( Exception e ) {
               e.printStackTrace();
            }
		}

		ohlScriptRow = hlScriptRow;
	}

	HelpFrame helpFrame;
	/**
	 * Montre la fenetre d'aide des macros
	 *
	 */
	protected void showHelp() {
		if( helpFrame==null ) helpFrame = new HelpFrame();

		helpFrame.setVisible(true);
		helpFrame.toFront();
	}

	/**
	 * Classe interne : fenetre d'aide pour les macros
	 */
	class HelpFrame extends JFrame {
		HelpFrame() {
			setIconImage(a.getImagette("AladinIconSS.gif"));

			setTitle(a.chaine.getString("FMHELPTITLE"));
			setDefaultCloseOperation(HIDE_ON_CLOSE);

			getContentPane().setLayout(new BorderLayout());
			((JPanel)getContentPane()).setBorder(new EmptyBorder(5,5,5,5));

			JPanel panel = new JPanel(new BorderLayout());
			panel.setOpaque(true);
			panel.setBorder(BorderFactory.createCompoundBorder(
	                BorderFactory.createTitledBorder(a.chaine.getString("FMHOWTO")),
	                BorderFactory.createEmptyBorder(10,10,10,10)));

			JEditorPane helpPane = new JEditorPane();
            helpPane.setContentType("text/html");
            helpPane.setText(a.chaine.getString("FMHELPCONTENT"));
            helpPane.setEditable(false);
//			JLabel helpLabel = new JLabel(a.chaine.getString("FMHELPCONTENT"));
			helpPane.setFont(Aladin.PLAIN);
			helpPane.setOpaque(true);
			helpPane.setBackground(Color.white);
			helpPane.setBorder(new EmptyBorder(5,5,5,5));
			final JScrollPane scrollPane = new JScrollPane(helpPane);
			scrollPane.setPreferredSize(new Dimension(530, 250));
			panel.add(scrollPane, BorderLayout.CENTER);
			getContentPane().add(panel, BorderLayout.CENTER);

            // on met la scrollbar verticale au début du texte
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    scrollPane.getVerticalScrollBar().setValue(0);
                }
            });

			pack();
		}
	}

	/**
	 * Classe interne gérant la coloration syntaxique du script
	 */
	class ScriptSyntaxRenderer implements DocumentListener, Runnable {
		StyledDocument TEMPLATE;
		Style COMMENT_STYLE, DEFAULT_STYLE, PARAM_STYLE,
		      KEYWORD_STYLE, LINK_STYLE, HILIGHT_STYLE;

		Color COMMENT_COLOR = new Color(63,  180,  95);
		Color PARAM_COLOR   = new Color(182, 0,    85);
		Color KEYWORD_COLOR = new Color(100, 163, 224);
		Color HILIGHT_COLOR = new Color(249, 237, 130);

		// flag de synchro pour la coloration
		boolean isColoring = false;
		// référence au document à colorer
		private Document doc;

		public ScriptSyntaxRenderer() {
			initStyles();
		}

		private void initStyles() {
			// création des styles nécessaires
			TEMPLATE = new DefaultStyledDocument();

			COMMENT_STYLE = TEMPLATE.addStyle("comment", null);
			StyleConstants.setItalic(COMMENT_STYLE, true);
			StyleConstants.setForeground(COMMENT_STYLE , COMMENT_COLOR);

			DEFAULT_STYLE = TEMPLATE.addStyle("default", null);

			PARAM_STYLE = TEMPLATE.addStyle("param", null);
			StyleConstants.setBold(PARAM_STYLE, true);
			StyleConstants.setForeground(PARAM_STYLE, PARAM_COLOR);

			KEYWORD_STYLE = TEMPLATE.addStyle("kw", null);
			StyleConstants.setBold(KEYWORD_STYLE, true);
			StyleConstants.setForeground(KEYWORD_STYLE, KEYWORD_COLOR);

			LINK_STYLE = TEMPLATE.addStyle("link", null);
			StyleConstants.setBold(LINK_STYLE, true);
			StyleConstants.setForeground(LINK_STYLE, KEYWORD_COLOR);
			StyleConstants.setUnderline(LINK_STYLE, true);

			HILIGHT_STYLE = TEMPLATE.addStyle("hl", null);
			StyleConstants.setBackground(HILIGHT_STYLE, HILIGHT_COLOR);
		}

		// implémentation interface DocumentListener
		public void insertUpdate(DocumentEvent ev) {
			doc = ev.getDocument();
			doColoringLater();
		}

		public void removeUpdate(DocumentEvent ev) {
			doc = ev.getDocument();
			doColoringLater();
		}

		public void changedUpdate(DocumentEvent ev) {
			doc = ev.getDocument();
			doColoringLater();
		}

		/** Color the document later at Event Dispatch.
		 * Does nothing if current edition event is related to coloring modifications within the document.
		 */
		private void  doColoringLater() {
			if (!isColoring) SwingUtilities.invokeLater(this);
		}

		// thread s'occupant de la coloration
		public void run() {
			isColoring = true;
			try {
				coloration();
			}
			finally {
				isColoring = false;
			}
		}

		private void coloration() {
			if( doc==null ) return;
		  	Position startPos = doc.getStartPosition();
		  	Position endPos = doc.getEndPosition();

		  	String txt;
		  	try {
		  		txt = doc.getText(startPos.getOffset(), endPos.getOffset()-startPos.getOffset());
		  	}
		  	catch(BadLocationException ble) {
		  		ble.printStackTrace();
		  		return;
		  	}

		  	// vecteur temporaire contenant les limites des commandes script trouvées dans le JTextPane
		  	Vector vec = new Vector();

		  	int posIdx = 0;
		  	int rowIdx = -1;
		  	String curLine, ocurLine;
		  	StyledDocument styledDoc = (StyledDocument)doc;
            String sep = "\n";
            ocurLine = sep;
		  	StringTokenizer st = new StringTokenizer(txt, sep, true);
		  	while( st.hasMoreTokens() ) {
		  		curLine = st.nextToken();

		  		if( curLine.equals(sep) ) {
		  		    if( !sep.equals(ocurLine) ) {
                        ocurLine = curLine;
                    }
                    else {
                        ocurLine = curLine;
                        rowIdx++;
                    }
                    posIdx += curLine.length();
                    continue;
		  		}
                else ocurLine = curLine;

		  		rowIdx++;

		  		// s'agit-il d'une ligne de commentaire ?
		  		if( curLine.trim().startsWith("#") ) {
		  			styledDoc.setCharacterAttributes(posIdx, curLine.length(), COMMENT_STYLE, true);
		  		}
		  		// si non, on force le style normal
		  		else {
		  			styledDoc.setCharacterAttributes(posIdx, curLine.length(), DEFAULT_STYLE, true);
		  			int idx = -1;

		  			// recherche de commandes script ("get", "load", "contour", ...)
		  			try {
		  				StreamTokenizer streamTk = new StreamTokenizer(new ByteArrayInputStream(curLine.getBytes()));
		  				int type = streamTk.nextToken();
		  				String word;
		  				if( type==StreamTokenizer.TT_WORD ) {
		  					word = streamTk.sval;
		  					if( Util.indexInArrayOf(word.toLowerCase(), Command.CMD)>=0 ) {
		  						idx = curLine.indexOf(word);
		  						int cmdIdx = posIdx+idx;
		  						vec.add(new int[] {cmdIdx, cmdIdx+word.length()});
//		  						System.out.println("cmdIdx : "+cmdIdx);
//			  					System.out.println("mousePos : "+mousePosInScript);

			  					if( mousePosInScript>=cmdIdx && mousePosInScript<cmdIdx+word.length() ) {
			  						styledDoc.setCharacterAttributes(cmdIdx, word.length(), LINK_STYLE, true);
			  					}
		  						else {
		  							styledDoc.setCharacterAttributes(cmdIdx, word.length(), KEYWORD_STYLE, true);
		  						}
		  						// cas spécial pour get : on met en valeur certains noms de serveurs
		  						if( word.equalsIgnoreCase("get") ) {
		  							type = streamTk.nextToken();
		  							if( type==StreamTokenizer.TT_WORD ) {
		  			  					word = streamTk.sval;
		  			  					String[] array = {"aladin", "simbad", "vizier"};
		  			  					if( Util.indexInArrayOf(word.toLowerCase(), array)>=0 ) {
		  			  						idx = curLine.indexOf(word);
		  			  						styledDoc.setCharacterAttributes(posIdx+idx, word.length(), KEYWORD_STYLE, true);
		  			  					}
		  							}
		  						}
		  					}
		  				}
		  			}
		  			catch(Exception e) {e.printStackTrace();}

		  			// recherche de paramètres
		  			while( (idx=curLine.indexOf('$', idx+1))>=0 ) {
		  				String tmp;
		  				int end = idx+2;
		  				// on va chercher la chaine la plus grande répondant à $[0-9]*
		  				while( end<=curLine.length() ) {
		  					tmp = curLine.substring(idx+1, end);
		  					try {
		  						Integer.parseInt(tmp);
		  					}
		  					catch(NumberFormatException nfe) {
		  						break;
		  					}

		  					end++;
		  				}
		  				if( end>idx+2 ) styledDoc.setCharacterAttributes(posIdx+idx, end-idx-1, PARAM_STYLE, true);
		  			}
		  		}

		  		// s'agit-il d'une ligne à highlighter ?
		  		if( rowIdx == hlScriptRow) {
		  			styledDoc.setCharacterAttributes(posIdx, curLine.length(), HILIGHT_STYLE, false);
		  		}

		  		posIdx += curLine.length();
		  	}

		  	cmdWordsPos = vec;
		}
	} // end of inner class ScriptSyntaxRenderer

	private void createPopup() {
		popup = new JPopupMenu();

		JMenuItem item = new JMenuItem(DELETE);
		item.addActionListener(macroController);
		popup.add(item);
	}

	/**
	 *
	 * @return Returns the popup
	 */
	protected JPopupMenu getPopup() {
		if( popup==null ) createPopup();

		return popup;
	}

	/**
	 * @return Returns the paramTable.
	 */
	protected JTable getParamTable() {
		return paramTable;
	}
	/**
	 * @return Returns the cmdWordUnderMouse.
	 */
	protected String getCmdWordUnderMouse() {
		return cmdWordUnderMouse;
	}
}
