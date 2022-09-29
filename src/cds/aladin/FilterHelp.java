// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


/**
 * Frame d'aide (exemples) pour la syntaxe des filtres
 *
 * @author Thomas Boch [CDS]
 * @version 1.0 : Avril 2005 - cette frame donne maintenant des exemples de filtres
 * @version 0.9 : 8 Novembre 2002 - Creation
 */

public class FilterHelp extends JFrame implements ActionListener {

	private static FilterHelp singleton=null;

	private static final String COPY_DEF = "Copy/Paste this definition";

	private static final String TITRE = "Examples of filters";
	private static final Color BKGD = Color.white;

	private FilterHelp() {
		super(TITRE);
		Aladin.setIcon(this);
//		setBackground(BKGD);

		setSize(550, (int)(Aladin.SCREENSIZE.height*0.7));
		JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        getContentPane().add(mainPanel);
        mainPanel.add(suite(), BorderLayout.CENTER);

		setLocation(350,200);
	}

	private JScrollPane suite() {
		JPanel panel = new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		GridBagLayout g =  new GridBagLayout();
		panel.setLayout(g);

		String[] def = PlanFilter.PREDEFFILTERS;
		String[] names = PlanFilter.PREDEFLABELS;
		String curName;
		String line = null;
		StringBuffer comments, curDef;
		StringTokenizer st;

		// boucle sur la liste des filtres à afficher
		for( int i=0; i<names.length; i++ ) {
			curName = names[i];
			// on écrit le nom du filtre
			writeTitle(panel, g, c, curName);

			// ** extraction des commentaires **
			comments = new StringBuffer();
			st = new StringTokenizer(def[i], "\n");
			// on skippe la première ligne
			st.nextToken();
			while( st.hasMoreTokens() && (line=st.nextToken().trim()).startsWith("#") ) {
				comments.append(line.substring(1).trim());
				if( st.hasMoreElements() ) comments.append('\n');
			}

			// on écrit les commentaires
			writeComment(panel, g, c, comments.toString());

			// ** extraction de la définition **
			curDef = new StringBuffer();
			if( line.length()!=0 ) curDef.append(line+"\n");
			while ( st.hasMoreTokens() && !(line=st.nextToken().trim()).startsWith("#") ) {
				if( line.length()==0 ) continue;
				curDef.append(line);
				if( st.hasMoreElements() ) curDef.append('\n');
			}
//			System.out.println(curDef.toString());
			// on écrit la définition
			writeDefinition(panel, g, c, curDef.toString());

			// ajout d'un bouton pour recopier le filtre dans la fenetre FilterProperties
			JButton b = new JButton(COPY_DEF);
			b.addActionListener(this);
			b.setActionCommand(i+"");
			Insets save = c.insets;
			c.insets = new Insets(10,save.left, save.right, 0);
			c.anchor = GridBagConstraints.CENTER;
			g.setConstraints(b,c);
			panel.add(b);
			c.insets = save;
		}

		JScrollPane scroll = new JScrollPane(panel);

		scroll.setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(50);



		return scroll;
	}

	private static final Font TITLE_FONT = Aladin.BOLD;
	private static final Color TITLE_COLOR = new Color(127,159,191);
	private void writeTitle(JPanel p, GridBagLayout g, GridBagConstraints c, String title) {
		JLabel l = new JLabel(title);
		l.setFont(TITLE_FONT);
		l.setForeground(TITLE_COLOR);

		Insets save = c.insets;
		c.insets = new Insets(10,save.left,10,save.right);

		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		g.setConstraints(l, c);
		p.add(l);

		c.insets = save;
	}

	private static final Font COMMENT_FONT = Aladin.PLAIN;
	private static final Color COMMENT_COLOR = new Color(63,127,95);
	private void writeComment(JPanel p, GridBagLayout g, GridBagConstraints c, String comment) {
		MyLabel l = new MyLabel(comment, Label.LEFT);
		l.setFont(COMMENT_FONT);
		l.setForeground(COMMENT_COLOR);

		Insets save = c.insets;
		c.insets = new Insets(0,45,10,0);
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		g.setConstraints(l, c);
		p.add(l);

		c.insets = save;
	}

	private static final Font DEF_FONT = Aladin.COURIER;
	private static final Color DEF_COLOR = Color.black;
	private static final Color CONDITION_COLOR = new Color(165,75,135);
	private void writeDefinition(JPanel p, GridBagLayout g, GridBagConstraints c, String def) {
		Insets save = c.insets;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(0,15,0,0);
		c.gridwidth = GridBagConstraints.REMAINDER;

		StringTokenizer st = new StringTokenizer(def, "\n");
		String line, cond, action;
		int idxAction;

		while( st.hasMoreTokens() ) {
			line = st.nextToken();
			idxAction = line.indexOf(" {");
			// du gros bricolage :o
			if( line.trim().endsWith("||") ) idxAction = line.length();

			if( idxAction>0 ) {
				JPanel pp = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));

				cond = line.substring(0,idxAction);
				action = line.substring(idxAction);

				JLabel lCond = new JLabel(cond);
				lCond.setFont(DEF_FONT);
				lCond.setForeground(CONDITION_COLOR);
				pp.add(lCond);

				JLabel lAction = new JLabel(action);
				lAction.setFont(DEF_FONT);
				lAction.setForeground(DEF_COLOR);
				pp.add(lAction);

				g.setConstraints(pp, c);
				p.add(pp);
			}
			else {
				JLabel l = new JLabel(line);
				l.setFont(DEF_FONT);
				l.setForeground(DEF_COLOR);
				c.gridwidth = GridBagConstraints.REMAINDER;
				g.setConstraints(l, c);
				p.add(l);
			}
		}

		c.insets = save;
	}

	private void makeAdd(Component comp, GridBagLayout g, GridBagConstraints c, int gridwidth, JPanel p) {
		c.gridwidth = gridwidth;
		g.setConstraints(comp,c);
		p.add(comp);
	}

	/** Retourne la reference de la Frame */
	static protected FilterHelp getInstance() {
		if( singleton == null) singleton = new FilterHelp();
		return singleton;
	}

	public void toFront() {
		move(350,200);
		super.toFront();
	}

	// Gestion des evenements
	public void actionPerformed(ActionEvent ae) {
		Object target = ae.getSource();

		if( ! (target instanceof JButton) ) return;

		// copie d'un filtre exemple dans le textarea de la fenetre FilterProperties courante
		int idx = Integer.parseInt(ae.getActionCommand());
		final FilterProperties window = FilterProperties.getCurWindow();
		if( window!=null && window.isShowing() ) {
			window.filterDef.setText(PlanFilter.PREDEFFILTERS[idx]);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.filterDefSp.getVerticalScrollBar().setValue(0);
			}
		});
	}

	/** Pour gerer la fermeture de la fenetre */
	public boolean handleEvent(Event e) {
		if( e.id==Event.WINDOW_DESTROY ) {
			hide();
		}
		return super.handleEvent(e);
	}


}

