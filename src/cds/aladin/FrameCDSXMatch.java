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

/*
 * Created on 4 déc. 2003
 *
 */
package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import cds.tools.Util;
import cds.xml.Field;

/** GUI pour le cross-match du CDS
 * @author T. Boch [CDS]
 */
public class FrameCDSXMatch extends JFrame implements Runnable, ActionListener {
    static final String ADVANCED_OPTIONS = "Advanced options";

    static final String POS_XMATCH_ELLIPSES = "Ellipses"/*Pos. x-match with ellipses*/;

    static String TITRE,CLOSE,XMATCH,POS_XMATCH,CROSS_ID,ONLYPOS,
                  THRESHOLD,THRESHOLD1,CHOOSEMATCH,BESTMATCH,ALLMATCH,
                  NOMATCH,JOIN,XID,ERR1,ERR2;

    static final String[] LIST_UCD_MAJOR_AXIS = {"EXTENSION_DIAM", "phys.angSize.smajAxis;pos.errorEllipse", "phys.angSize.smajAxis"};
    static final String[] LIST_UCD_MINOR_AXIS = {"EXTENSION_MIN", "phys.angSize.sminAxis;pos.errorEllipse", "phys.angSize.sminAxis"};
    static final String[] LIST_UCD_PA = {"POS_POS-ANG", "pos.posAng;pos.errorEllipse", "pos.posAng"};
    // intitulé des algos de xmatch
    static final String[] xmatchNames = {POS_XMATCH, CROSS_ID};
    // algos de xmatch correspondant
    //static final XMatchAlgoInterface[] xmatchAlgos = {};

    Aladin aladin;

    // TODO : merger les checkbox entre x-match pos. et x-match pos.+ellipses dans un panel commun
    // les différentes méthodes pour x-match positionnel : all matches, best match, no match
    JCheckBox allMatch, bestMatch, noMatch;

    // les différentes méthodes pour x-match avec ellipses : all matches, best match, no match
    JCheckBox ellAllMatch, ellBestMatch, ellNoMatch;

    // système d'onglets pour choisir entre les differents x-match
    JTabbedPane tabbedPane;

    JComboBox RAChoiceA, RAChoiceB, DEChoiceA, DEChoiceB, XIDChoiceA, XIDChoiceB;
    JComboBox ellRAChoiceA, ellRAChoiceB, ellDEChoiceA, ellDEChoiceB,
	       ellMAChoiceA, ellMAChoiceB, ellMIChoiceA, ellMIChoiceB, ellPAChoiceA, ellPAChoiceB;

    // x-match positionnel
    JTextField minDist, maxDist;
    // ellipses (nombre de sigmas)
    JTextField nbSigmaMin, nbSigmaMax;

    JComboBox labelAXMatch, labelBXMatch, labelAXID, labelBXID, ellLabelA, ellLabelB;

    Thread runme;

    JPanel xIDOptionsP;

    private OptionFrame optionFrame;

    // ensemble des PlanCatalog couramment chargés
    Plan[] cats;

    protected void createChaine() {
       TITRE = aladin.chaine.getString("XMTITRE");
       CLOSE = aladin.chaine.getString("CLOSE");
       XMATCH = aladin.chaine.getString("XMXMATCH");
       POS_XMATCH = aladin.chaine.getString("XMPOS_XMATCH");
       CROSS_ID = aladin.chaine.getString("XMCROSS_ID");
       ONLYPOS = aladin.chaine.getString("XMONLYPOS");
       THRESHOLD = aladin.chaine.getString("XMTHRESHOLD");
       THRESHOLD1 = aladin.chaine.getString("XMTHRESHOLD1");
       CHOOSEMATCH = aladin.chaine.getString("XMCHOOSEMATCH");
       BESTMATCH = aladin.chaine.getString("XMBESTMATCH");
       ALLMATCH = aladin.chaine.getString("XMALLMATCH");
       NOMATCH = aladin.chaine.getString("XMNOMATCH");
       JOIN = aladin.chaine.getString("XMJOIN");
       XID = aladin.chaine.getString("XMXID");
       ERR1 = aladin.chaine.getString("XMERR1");
       ERR2 = aladin.chaine.getString("XMERR2");

       String[] tab = {POS_XMATCH, CROSS_ID, POS_XMATCH_ELLIPSES};
       tabbedPaneComponents = tab;
    }

    public FrameCDSXMatch(Aladin aladin) {
        super();
        Aladin.setIcon(this);
//        setBackground(Aladin.BKGD);
        this.aladin = aladin;
        createChaine();
        setTitle(TITRE);

        // raccourci pour fermeture rapide de la frame
        Util.setCloseShortcut(this, false, aladin);


        setLocation(Aladin.computeLocation(this));
    }

    /** Construit (ou reconstruit) entièrement la Frame */
    private void buildFrame() {
        getContentPane().removeAll();

        getContentPane().setLayout(new BorderLayout());
        memPlanA = memPlanB = null;


        GridBagLayout g =  new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JPanel p = new JPanel();
        p.setLayout(g);

        // Label de titre
        JLabel titre = new JLabel(TITRE,JLabel.CENTER);
        titre.setFont(Aladin.LBOLD);
        g.setConstraints(titre,c);

        // système d'onglets
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(POS_XMATCH, posXMatchPanel());
        tabbedPane.addTab(CROSS_ID, posXIDPanel());
        tabbedPane.addTab(POS_XMATCH_ELLIPSES, posXMatchEllipsesPanel());
        g.setConstraints(tabbedPane, c);
        p.add(tabbedPane);
        populateChoicesWithPlanes();


        // on affiche le panel parent (pour systeme d'onglets)
        g.setConstraints(tabbedPane,c);
        p.add(tabbedPane);

        // initialisation des objets Choice
        initComboBoxes();

        getContentPane().add(p, BorderLayout.CENTER);

        // JPanel du bas
        JPanel bottomPanel = bottomPanel();
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        pack();
    }

    /** Remet à jour la frame complètement */
    protected void update() {
            buildFrame();
            setVisible(true);
            toFront();
    }

    /** Retourne le panel du bas, contenant les boutons pour lancer le cross-match et pour annuler */
    private JPanel bottomPanel() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton b;

        p.add(b = new JButton(ADVANCED_OPTIONS));
        b.addActionListener(this);

        b = new JButton(XMATCH);
        b.addActionListener(this);
        b.setFont(Aladin.BOLD);
        p.add(b);

        p.add(b = new JButton(CLOSE));
        b.addActionListener(this);

        return p;
    }

    private void initComboBoxes() {
        if( cats.length>0 ) {
        	Plan pc;
        	pc = cats[0];
        	memPlanA = pc.label;

        	// remplissage des combo box pour le 2e catalogue
            populateRADECChoices(RAChoiceA, DEChoiceA, pc, true);
            labelAXMatch.setSelectedItem(pc.label);

            populateRADECChoices(XIDChoiceA, null, pc, false);
            labelAXID.setSelectedItem(pc.label);
//            XIDChoiceA.select(cats[0].label);

            populateRADECChoices(ellRAChoiceA, ellDEChoiceA, pc, true);
            populateChoice(ellMAChoiceA, pc, LIST_UCD_MAJOR_AXIS);
            populateChoice(ellMIChoiceA, pc, LIST_UCD_MINOR_AXIS);
            populateChoice(ellPAChoiceA, pc, LIST_UCD_PA);
            ellLabelA.setSelectedItem(pc.label);


            // remplissage des Choice pour le 2e catalogue
            if( cats.length>1 ) pc = cats[1];
            else pc = cats[0];
            memPlanB = pc.label;

            populateRADECChoices(RAChoiceB, DEChoiceB, pc, true);
            labelBXMatch.setSelectedItem(pc.label);

            populateRADECChoices(XIDChoiceB, null, pc, false);
            labelBXID.setSelectedItem(pc.label);
//            XIDChoiceB.select(pc.label);

            populateRADECChoices(ellRAChoiceB, ellDEChoiceB, pc, true);
            populateChoice(ellMAChoiceB, pc, LIST_UCD_MAJOR_AXIS);
            populateChoice(ellMIChoiceB, pc, LIST_UCD_MINOR_AXIS);
            populateChoice(ellPAChoiceB, pc, LIST_UCD_PA);
            ellLabelB.setSelectedItem(pc.label);

        }
    }

    /** Retourne le panel contenant toutes les options pour le positional cross-match */
    private JPanel posXMatchPanel() {
    	JPanel xMatchOptionsP, xMatchOptionsP2;

    	// quelques initialisations
		labelAXMatch = new JComboBox();
		labelAXMatch.addActionListener(this);
		labelAXMatch.setFont(Aladin.BOLD);
		labelBXMatch = new JComboBox();
		labelBXMatch.addActionListener(this);
		labelBXMatch.setFont(Aladin.BOLD);


        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, POS_XMATCH, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                BorderFactory.createEmptyBorder(5,5,5,5))));
        GridBagLayout g =  new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.setLayout(g);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(3,3,3,3);

        JLabel explain = new JLabel(ONLYPOS/*"Only positional offset is used to find the matches."*/,
                                              JLabel.CENTER);
        g.setConstraints(explain, c);
        p.add(explain);



        GridBagLayout gg = new GridBagLayout();
        GridBagConstraints d = new GridBagConstraints();
        d.gridwidth=1;d.gridheight=1;d.fill = GridBagConstraints.NONE;d.insets = new Insets(0,3,0,3);
        xMatchOptionsP = new JPanel();
        xMatchOptionsP.setLayout(gg);
        JComponent[][] components = new JComponent[2][6];


        components[0][0] = labelAXMatch;
        components[1][0] = labelBXMatch;

        components[0][1] = new JLabel("");
        components[1][1] = new JLabel("");

        components[0][2] = new JLabel("RA");
        components[1][2] = new JLabel("RA");

        RAChoiceA = new JComboBox();
        RAChoiceB = new JComboBox();

        components[0][3] = RAChoiceA;
        components[1][3] = RAChoiceB;

        components[0][4] = new JLabel("DEC");
        components[1][4] = new JLabel("DEC");

        DEChoiceA = new JComboBox();
        DEChoiceB = new JComboBox();


        components[0][5] = DEChoiceA;
        components[1][5] = DEChoiceB;

        for( int i=0; i<2; i++ ) {
            for( int j=0; j<6; j++ ) {
                d.gridx = j;
                d.gridy = i;
                if( j==0 ) d.anchor = GridBagConstraints.WEST;
                else d.anchor = GridBagConstraints.EAST;
                if( components[i][j] instanceof JComboBox ) d.fill = GridBagConstraints.HORIZONTAL;
                else d.fill = GridBagConstraints.NONE;
                //d.weightx = 1.0;
                //d.weighty = 1.0;
                gg.setConstraints(components[i][j],d);
                xMatchOptionsP.add(components[i][j]);
            }
        }
        // déconne sous MacOS
        //c.ipady = 5;
        c.anchor = GridBagConstraints.WEST;
        g.setConstraints(xMatchOptionsP, c);
        p.add(xMatchOptionsP);

        xMatchOptionsP2 = new JPanel();
        GridBagLayout g2 =  new GridBagLayout();
        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        xMatchOptionsP2.setLayout(g2);

        JLabel threshExplain = new JLabel(THRESHOLD, JLabel.CENTER);
		c2.gridwidth = GridBagConstraints.REMAINDER;
        c2.anchor = GridBagConstraints.CENTER;
		g2.setConstraints(threshExplain, c2);
		xMatchOptionsP2.add(threshExplain);

        JPanel distPanel = new JPanel();
        distPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        minDist = new JTextField("0",4);
        distPanel.add(minDist);
        distPanel.add(new JLabel(THRESHOLD1));
        maxDist = new JTextField("4",4);
        distPanel.add(maxDist);
        c2.gridwidth = GridBagConstraints.REMAINDER;
        g2.setConstraints(distPanel, c2);
        xMatchOptionsP2.add(distPanel);

        //c.ipady = 0;
        JLabel method = new JLabel(CHOOSEMATCH);
        method.setFont(Aladin.BOLD);
        g2.setConstraints(method, c2);
        xMatchOptionsP2.add(method);

        c2.anchor = GridBagConstraints.WEST;
        c2.insets = new Insets(0,40,0,0);

        bestMatch = new JCheckBox(BESTMATCH);
        bestMatch.addActionListener(this);
        bestMatch.setSelected(true);
        g2.setConstraints(bestMatch, c2);
        xMatchOptionsP2.add(bestMatch);

        allMatch = new JCheckBox(ALLMATCH);
        allMatch.addActionListener(this);
        g2.setConstraints(allMatch, c2);
        xMatchOptionsP2.add(allMatch);

        noMatch = new JCheckBox(NOMATCH);
        noMatch.addActionListener(this);
        g2.setConstraints(noMatch, c2);
        xMatchOptionsP2.add(noMatch);

        //c.insets = new Insets(0,0,0,0);

        c.anchor = GridBagConstraints.CENTER;
        g.setConstraints(xMatchOptionsP2, c);
        p.add(xMatchOptionsP2);

        return p;
    }

    private ColFilter getColFilter() {
    	if( optionFrame==null ) return null;
    	optionFrame.saveValues();
    	Field[] field1 = getSelectedFields(optionFrame.col1);
    	Field[] field2 = getSelectedFields(optionFrame.col2);
    	return new ColFilter(field1, field2, optionFrame.prefix1, optionFrame.prefix2, optionFrame.suffix1, optionFrame.suffix2);
    }

    private Field[] getSelectedFields(ColButton[] col) {
    	Vector v = new Vector();
    	for( int i=0; i<col.length; i++ ) {
    		if( col[i].mode==ColButton.DOWN ) v.addElement(col[i].field);
    	}
    	Field[] f = new Field[v.size()];
    	v.copyInto(f);
    	v = null;
    	return f;
    }

    /** Retourne le panel contenant toutes les options
     *  pour le positional cross-match with ellipses */
    private JPanel posXMatchEllipsesPanel() {
    	JPanel xMatchOptionsP, xMatchOptionsP2;

    	// quelques initialisations
		ellLabelA = new JComboBox();
		ellLabelA.addActionListener(this);
		ellLabelA.setFont(Aladin.BOLD);
		ellLabelB = new JComboBox();
		ellLabelB.addActionListener(this);
		ellLabelB.setFont(Aladin.BOLD);


        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, POS_XMATCH_ELLIPSES, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                BorderFactory.createEmptyBorder(5,5,5,5))));
        GridBagLayout g =  new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.setLayout(g);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(3,3,3,3);

        JLabel explain = new JLabel("Only positional offset is used to find the matches.",
                JLabel.CENTER);
        g.setConstraints(explain, c);
        p.add(explain);



        GridBagLayout gg = new GridBagLayout();
        GridBagConstraints d = new GridBagConstraints();
        d.gridwidth=1;d.gridheight=1;d.fill = GridBagConstraints.NONE;d.insets = new Insets(0,3,0,3);
        xMatchOptionsP = new JPanel();
        xMatchOptionsP.setLayout(gg);
        JComponent[][] components = new JComponent[4][6];

        JLabel l;

        l = new JLabel("Catalogue A");
        l.setFont(Aladin.BOLD);
        components[0][0] = l;
        l = new JLabel("Catalogue B");
        l.setFont(Aladin.BOLD);
        components[2][0] = l;

        components[0][1] = ellLabelA;
        components[2][1] = ellLabelB;

        components[0][2] = new JLabel("RA");
        components[2][2] = new JLabel("RA");

        ellRAChoiceA = new JComboBox();
        ellRAChoiceB = new JComboBox();

        components[0][3] = ellRAChoiceA;
        components[2][3] = ellRAChoiceB;

        components[0][4] = new JLabel("DEC");
        components[2][4] = new JLabel("DEC");

        ellDEChoiceA = new JComboBox();
        ellDEChoiceB = new JComboBox();


        components[0][5] = ellDEChoiceA;
        components[2][5] = ellDEChoiceB;

        components[1][0] = new JLabel("Maj. axis");
        components[3][0] = new JLabel("Maj. axis");

        ellMAChoiceA = new JComboBox();
        ellMAChoiceB = new JComboBox();

        components[1][1] = ellMAChoiceA;
        components[3][1] = ellMAChoiceB;

        components[1][2] = new JLabel("Min. axis");
        components[3][2] = new JLabel("Min. axis");

        ellMIChoiceA = new JComboBox();
        ellMIChoiceB = new JComboBox();

        components[1][3] = ellMIChoiceA;
        components[3][3] = ellMIChoiceB;

        components[1][4] = new JLabel("PA");
        components[3][4] = new JLabel("PA");

        ellPAChoiceA = new JComboBox();
        ellPAChoiceB = new JComboBox();

        components[1][5] = ellPAChoiceA;
        components[3][5] = ellPAChoiceB;

        for( int i=0; i<4; i++ ) {
            for( int j=0; j<6; j++ ) {
                d.gridx = j;
                d.gridy = i;
                if( j==0 ) d.anchor = GridBagConstraints.WEST;
                else d.anchor = GridBagConstraints.EAST;
                if( components[i][j] instanceof JComboBox ) d.fill = GridBagConstraints.HORIZONTAL;
                else d.fill = GridBagConstraints.NONE;
                //d.weightx = 1.0;
                //d.weighty = 1.0;
                gg.setConstraints(components[i][j],d);
                xMatchOptionsP.add(components[i][j]);
            }
        }
        //c.ipady = 5; // déconne sous MacOS
        c.anchor = GridBagConstraints.WEST;
        g.setConstraints(xMatchOptionsP, c);
        p.add(xMatchOptionsP);

        xMatchOptionsP2 = new JPanel();
        GridBagLayout g2 =  new GridBagLayout();
        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        xMatchOptionsP2.setLayout(g2);

        JLabel threshExplain = new JLabel("Number of sigmas threshold", JLabel.CENTER);
		c2.gridwidth = GridBagConstraints.REMAINDER;
        c2.anchor = GridBagConstraints.CENTER;
		g2.setConstraints(threshExplain, c2);
		xMatchOptionsP2.add(threshExplain);

        JPanel distPanel = new JPanel();
        distPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        nbSigmaMin = new JTextField("0", 4);
        distPanel.add(nbSigmaMin);
        distPanel.add(new JLabel("<= nbSigmas <="));
        nbSigmaMax = new JTextField("3",4);
        distPanel.add(nbSigmaMax);
        c2.gridwidth = GridBagConstraints.REMAINDER;
        g2.setConstraints(distPanel, c2);
        xMatchOptionsP2.add(distPanel);

        //c.ipady = 0;
        JLabel method = new JLabel("Choose match method");
        method.setFont(Aladin.BOLD);
        g2.setConstraints(method, c2);
        xMatchOptionsP2.add(method);

        c2.anchor = GridBagConstraints.WEST;
        c2.insets = new Insets(0,40,0,0);

        ellBestMatch = new JCheckBox("Best matches");
        ellBestMatch.addActionListener(this);
        ellBestMatch.setSelected(true);
        g2.setConstraints(ellBestMatch, c2);
        xMatchOptionsP2.add(ellBestMatch);

        ellAllMatch = new JCheckBox("All matches");
        ellAllMatch.addActionListener(this);
        g2.setConstraints(ellAllMatch, c2);
        xMatchOptionsP2.add(ellAllMatch);

        ellNoMatch = new JCheckBox("Sources without match");
        ellNoMatch.addActionListener(this);
        g2.setConstraints(ellNoMatch, c2);
        xMatchOptionsP2.add(ellNoMatch);

        //c.insets = new Insets(0,0,0,0);

        c.anchor = GridBagConstraints.CENTER;
        g.setConstraints(xMatchOptionsP2, c);
        p.add(xMatchOptionsP2);

        return p;
    }

    /** Retourne le panel contenant toutes les options pour le X-ID */
    private JPanel posXIDPanel() {
    	// quelques initialisations
		labelAXID = new JComboBox();
		labelAXID.addActionListener(this);
		labelAXID.setFont(Aladin.BOLD);
		labelBXID = new JComboBox();
		labelBXID.addActionListener(this);
		labelBXID.setFont(Aladin.BOLD);

        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, CROSS_ID, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
                BorderFactory.createEmptyBorder(5,5,5,5))));

        GridBagLayout g =  new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.setLayout(g);

        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3,3,3,3);

        JLabel explain = new JLabel(JOIN, JLabel.CENTER);
        g.setConstraints(explain, c);
        p.add(explain);


        GridBagLayout gg = new GridBagLayout();
        GridBagConstraints d = new GridBagConstraints();
        d.gridwidth=1;d.gridheight=1;d.fill = GridBagConstraints.NONE;d.insets = new Insets(0,3,0,3);
        xIDOptionsP = new JPanel();
        xIDOptionsP.setLayout(gg);
        JComponent[][] components = new JComponent[2][4];
        components[0][0] = labelAXID;
        components[1][0] = labelBXID;


        components[0][1] = new JLabel("");
        components[1][1] = new JLabel("");

        components[0][2] = new JLabel(XID);
        components[1][2] = new JLabel(XID);

        XIDChoiceA = new JComboBox();
        XIDChoiceB = new JComboBox();

        components[0][3] = XIDChoiceA;
        components[1][3] = XIDChoiceB;

        for( int i=0; i<2; i++ ) {
            for( int j=0; j<4; j++ ) {
                d.gridx = j;
                d.gridy = i;
                if( components[i][j] instanceof JComboBox ) d.fill = GridBagConstraints.HORIZONTAL;
                else d.fill = GridBagConstraints.NONE;
                if( j==0 ) d.anchor = GridBagConstraints.WEST;
                else d.anchor = GridBagConstraints.EAST;
                //d.weightx = 1.0;
                //d.weighty = 1.0;
                gg.setConstraints(components[i][j],d);
                xIDOptionsP.add(components[i][j]);
            }
        }
        // déconne sous MacOS
        //c.ipady = 5;
        c.anchor = GridBagConstraints.WEST;
        g.setConstraints(xIDOptionsP, c);
        p.add(xIDOptionsP);

        return p;
    }

    private void populateChoicesWithPlanes() {
        Vector v = new Vector();
        int nbSelected = 0;
        int nbActive = 0;
        // Parcours de tous les plans CATALOG
        for( int i=0; i<aladin.calque.plan.length; i++ ) {
           Plan p = aladin.calque.plan[i];
           if( !p.isSimpleCatalog() || !p.flagOk ) continue;
           // on veut d'abord les plans sélectionnés, puis les plans actifs, puis les autres
           if( p.selected && p.active ) v.insertElementAt(p, nbSelected++);
           else if( p.active ) v.insertElementAt(p, nbSelected+nbActive++);
           else v.addElement(p);

           labelAXMatch.addItem(p.label);
           labelAXID.addItem(p.label);
           ellLabelA.addItem(p.label);

           labelBXMatch.addItem(p.label);
           labelBXID.addItem(p.label);
           ellLabelB.addItem(p.label);
        }
        cats = new Plan[v.size()];
        v.copyInto(cats);
        v=null;
    }

    /**
     *
     * @param ra
     * @param dec
     * @param p
     * @param select si true, présélectionne les colonnes de coordonnées sur la base des UCD
     */
    private void populateRADECChoices(JComboBox ra, JComboBox dec, Plan p, boolean select) {
    	ra.removeAllItems();
        if( dec!=null ) {
        	if( dec.getItemCount()>0 ) dec.setSelectedIndex(0);
        	dec.removeAllItems();
        }
        Legende leg = p.getFirstLegende();
        if( leg==null ) return;
        for( int i=0; i<leg.field.length; i++ ) {
            ra.addItem(leg.field[i].name);
            if( dec!=null ) dec.addItem(leg.field[i].name);
        }
        if( select ) {
        	int[] idx = CDSXMatch.findCoord(p);
        	// colonnes de coord. introuvables
        	if( idx==null ) return;

//        	System.out.println("idx : "+idx[0]+" "+idx[1]);
        	if( idx[0]>=0 ) ra.setSelectedIndex(idx[0]);
        	if( idx[1]>=0 && dec!=null  ) dec.setSelectedIndex(idx[1]);
        }
    }

    /**
     * Populate a Choice widget
     * @param c choice to populate
     * @param pc
     * @param ucds list of ucds. If one of them is found, the choice will be pre-selected with
     * the correponding column. ucds must be sorted by order of preference
     */
    private void populateChoice(JComboBox c, Plan pc, String[] ucds) {
    	c.removeAllItems();

        Legende leg = pc.getFirstLegende();
        if( pc==null ) return;
        for( int i=0; i<leg.field.length; i++ ) {
            c.addItem(leg.field[i].name);
        }

        if( ucds!=null ) {
        	int idx = CDSXMatch.findIdx(pc, ucds);
//        	System.out.println("index found for plane "+pc.getLabel()+" : "+idx);
//        	System.out.println("nb of items :"+c.getItemCount());
        	if( idx>=0 ) c.setSelectedIndex(idx);

        }
    }

    public void setVisible(boolean flag) {
       aladin.toolBox.setMode(ToolBox.XMATCH, flag?Tool.DOWN:Tool.UP);
       super.setVisible(flag);
    }

    // Gestion des evenements
    String memPlanA, memPlanB;
    // implementation de ActionListener
    public void actionPerformed(ActionEvent ae) {
        String what = ae.getActionCommand();
        Object target = ae.getSource();

        if( what.equals(CLOSE) ) setVisible(false);
        else if( what.equals(ADVANCED_OPTIONS) ) {
            showOptionsFrame();
        }
        else if( what.equals(XMATCH) ) {
            launchXMatch();
        }
        else if( target==labelAXMatch || target==labelAXID || target==ellLabelA ) {
            String selectedPlane= ((JComboBox)target).getSelectedItem().toString();
            if( memPlanA!=null && memPlanA.equals(selectedPlane) ) return;
            memPlanA = selectedPlane;
            int indice = aladin.calque.getIndexPlan(selectedPlane);
            if( indice<0 ) return;
            Plan pc = aladin.calque.plan[indice];
            populateRADECChoices(RAChoiceA, DEChoiceA, pc, true);
            populateRADECChoices(XIDChoiceA, null, pc, false);
            populateRADECChoices(ellRAChoiceA, ellDEChoiceA, pc, true);
            populateChoice(ellMAChoiceA, pc, LIST_UCD_MAJOR_AXIS);
            populateChoice(ellMIChoiceA, pc, LIST_UCD_MINOR_AXIS);
            populateChoice(ellPAChoiceA, pc, LIST_UCD_PA);
            if( target!=labelAXMatch ) labelAXMatch.setSelectedItem(pc.label);
            if( target!=labelAXID ) labelAXID.setSelectedItem(pc.label);
            if( target!=ellLabelA ) ellLabelA.setSelectedItem(pc.label);

            if( optionFrame!=null ) optionFrame.update(pc, optionFrame.pc2);
        }
        else if( target==labelBXMatch || target==labelBXID || target==ellLabelB ) {
            String selectedPlane= ((JComboBox)target).getSelectedItem().toString();
            if( memPlanB!=null && memPlanB.equals(selectedPlane) ) return;
            memPlanB = selectedPlane;
            int indice = aladin.calque.getIndexPlan(selectedPlane);
            if( indice<0 ) return;
            Plan pc = aladin.calque.plan[indice];
            populateRADECChoices(RAChoiceB, DEChoiceB, pc, true);
            populateRADECChoices(XIDChoiceB, null, pc, false);
            populateRADECChoices(ellRAChoiceB, ellDEChoiceB, pc, true);
            populateChoice(ellMAChoiceB, pc, LIST_UCD_MAJOR_AXIS);
            populateChoice(ellMIChoiceB, pc, LIST_UCD_MINOR_AXIS);
            populateChoice(ellPAChoiceB, pc, LIST_UCD_PA);
            if( target!=labelBXMatch ) labelBXMatch.setSelectedItem(pc.label);
            if( target!=labelBXID ) labelBXID.setSelectedItem(pc.label);
            if( target!=ellLabelB ) ellLabelB.setSelectedItem(pc.label);

            if( optionFrame!=null ) optionFrame.update(optionFrame.pc1, pc);
        }
        else if( target.equals(allMatch) ) {
            if( allMatch.isSelected() && bestMatch.isSelected() ) bestMatch.setSelected(false);
            else if( ! allMatch.isSelected() && ! noMatch.isSelected() ) allMatch.setSelected(true);
        }
        else if( target.equals(bestMatch) ) {
            if( allMatch.isSelected() && bestMatch.isSelected() ) allMatch.setSelected(false);
            else if( ! bestMatch.isSelected() && ! noMatch.isSelected() ) bestMatch.setSelected(true);
        }
        else if( target.equals(ellAllMatch) ) {
            if( ellAllMatch.isSelected() && ellBestMatch.isSelected() ) ellBestMatch.setSelected(false);
            else if( ! ellAllMatch.isSelected() && ! ellNoMatch.isSelected() ) ellAllMatch.setSelected(true);
        }
        else if( target.equals(ellBestMatch) ) {
            if( ellAllMatch.isSelected() && ellBestMatch.isSelected() ) ellAllMatch.setSelected(false);
            else if( ! ellBestMatch.isSelected() && ! ellNoMatch.isSelected() ) ellBestMatch.setSelected(true);
        }


    }



    /** pour activer/desactiver un container ET les components qu'il contient
     * (faire un simple setEnable(false) sur un JPanel ne fonctionne pas sous windows)
     * @param cont container à activer/desactiver
     * @param e

    private void setEnabled(Container cont, boolean e) {
        cont.setEnabled(e);
        Component[] comps = cont.getComponents();
        for(int i = 0; i < comps.length; i++) {
            comps[i].setEnabled(e);
            if( comps[i] instanceof Container) setEnabled((Container)comps[i], e);
        }
    }
    */

    static private String[] tabbedPaneComponents;


    // paramètres pour le thread
    ColFilter colFilterT;
    Plan p1T,p2T;
    String labelT;
    double[] seuilsT;
    int index1T, index2T;
    int[] coordTab1T, coordTab2T;
    int[] ellipseParam1T, ellipseParam2T; // [0] : maj axis, [1] : min axis, [2] : pos. angle
    int methodeT;
    int typeT; // type de cross-match
    /** Lancement du xmatch selon la fonction choisie */
    private void launchXMatch() {
    	colFilterT = getColFilter();

    	String tab = tabbedPaneComponents[tabbedPane.getSelectedIndex()];

        if( tab.equals(POS_XMATCH) )
            launchPosXMatch();
        else if( tab.equals(CROSS_ID) )
            launchXID();
        else if( tab.equals(POS_XMATCH_ELLIPSES) )
        	launchPosXMatchEllipses();
    }

    /** lancement du xid */
    private void launchXID() {
        typeT = CDSXMatch.JOIN;

        index1T = XIDChoiceA.getSelectedIndex();
        index2T = XIDChoiceB.getSelectedIndex();

        p1T = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(labelAXID.getSelectedItem().toString())];
        p2T = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(labelBXID.getSelectedItem().toString())];

        runme = new Thread(this,"AladinXmatch");
        Util.decreasePriority(Thread.currentThread(), runme);
//        runme.setPriority( Thread.NORM_PRIORITY -1);
        runme.start();
    }

    /** lancement du xmatch positionnel avec vérification des paramètres ! */
    private void launchPosXMatch() {
        typeT = CDSXMatch.POSXMATCH;
        methodeT = 0;
        if( allMatch.isSelected() ) methodeT |= CDSXMatch.ALLMATCH;
        if( bestMatch.isSelected() ) methodeT |= CDSXMatch.BESTMATCH;
        if( noMatch.isSelected() ) methodeT |= CDSXMatch.NOMATCH;

        //System.out.println(methodeT);
        // vérification qu'une méthode a été choisie
        if( methodeT==0 ) {
            Aladin.error(this, ERR1, 1);
            return;
        }

        // vérification des seuils
        seuilsT = new double[2];
        try {
            //seuilsT[0] = Double.parseDouble(minDist.getText());
            seuilsT[0] = Double.valueOf(minDist.getText()).doubleValue();
            //seuilsT[1] = Double.parseDouble(maxDist.getText());
            seuilsT[1] = Double.valueOf(maxDist.getText()).doubleValue();
        }
        catch(NumberFormatException e) {
            Aladin.error(this,ERR2,1);
            return;
        }

        p1T = aladin.calque.plan[aladin.calque.getIndexPlan(labelAXMatch.getSelectedItem().toString())];
        p2T = aladin.calque.plan[aladin.calque.getIndexPlan(labelBXMatch.getSelectedItem().toString())];

        coordTab1T = new int[2];
        coordTab1T[0] = RAChoiceA.getSelectedIndex();
        coordTab1T[1] = DEChoiceA.getSelectedIndex();

        coordTab2T = new int[2];
        coordTab2T[0] = RAChoiceB.getSelectedIndex();
        coordTab2T[1] = DEChoiceB.getSelectedIndex();

        runme = new Thread(this,"AladinXmatchPos");
        Util.decreasePriority(Thread.currentThread(), runme);
//        runme.setPriority( Thread.NORM_PRIORITY -1);
        runme.start();
    }

    /** lancement du xmatch positionnel avec vérification des paramètres ! */
    private void launchPosXMatchEllipses() {
        typeT = CDSXMatch.POSXMATCH_ELLIPSES;
        // TODO : à merger avec la partie dans launchPosXMatch
        methodeT = 0;
        if( ellAllMatch.isSelected() ) methodeT |= CDSXMatch.ALLMATCH;
        if( ellBestMatch.isSelected() ) methodeT |= CDSXMatch.BESTMATCH;
        if( ellNoMatch.isSelected() ) methodeT |= CDSXMatch.NOMATCH;

        //System.out.println(methodeT);
        // vérification qu'une méthode a été choisie
        if( methodeT==0 ) {
            Aladin.error(this, "No cross-match method chosen !", 1);
            return;
        }

        // vérification des seuils
        seuilsT = new double[2];
        try {
        	seuilsT[0] = Double.valueOf(nbSigmaMin.getText()).doubleValue();
            seuilsT[1] = Double.valueOf(nbSigmaMax.getText()).doubleValue();
        }
        catch(NumberFormatException e) {
            Aladin.error(this,"Could not parse min or max distance !",1);
            return;
        }

        p1T = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(ellLabelA.getSelectedItem().toString())];
        p2T = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(ellLabelB.getSelectedItem().toString())];

        coordTab1T = new int[2];
        coordTab1T[0] = ellRAChoiceA.getSelectedIndex();
        coordTab1T[1] = ellDEChoiceA.getSelectedIndex();

        coordTab2T = new int[2];
        coordTab2T[0] = RAChoiceB.getSelectedIndex();
        coordTab2T[1] = DEChoiceB.getSelectedIndex();

        ellipseParam1T = new int[3];
        ellipseParam1T[0] = ellMAChoiceA.getSelectedIndex();
        ellipseParam1T[1] = ellMIChoiceA.getSelectedIndex();
        ellipseParam1T[2] = ellPAChoiceA.getSelectedIndex();

        ellipseParam2T = new int[3];
        ellipseParam2T[0] = ellMAChoiceB.getSelectedIndex();
        ellipseParam2T[1] = ellMIChoiceB.getSelectedIndex();
        ellipseParam2T[2] = ellPAChoiceB.getSelectedIndex();

        runme = new Thread(this,"AladinXmatchEllipse");
        Util.decreasePriority(Thread.currentThread(), runme);
//        runme.setPriority( Thread.NORM_PRIORITY -1);
        runme.start();
    }

    public void run() {
    	// plus nécessaire depuis que le plan résultat apparait immédiatement
//        Aladin.makeCursor(this, Aladin.WAIT);

        CDSXMatch xMatch = new CDSXMatch(aladin);

        xMatch.setColFilter(colFilterT);

        if( typeT==CDSXMatch.POSXMATCH)
            xMatch.posXMatch(p1T,p2T,null,coordTab1T,coordTab2T,seuilsT,methodeT,aladin);
        else if( typeT==CDSXMatch.JOIN )
            xMatch.xID(p1T, p2T, labelT, index1T, index2T, aladin);
        else if( typeT==CDSXMatch.POSXMATCH_ELLIPSES )
        	xMatch.posXMatchEllipses(p1T, p2T, null, coordTab1T, coordTab2T, ellipseParam1T, ellipseParam2T,
        			                 seuilsT[0],seuilsT[1], methodeT, aladin);
    }

    // Gestion des evenements
    public boolean handleEvent(Event e) {

       if( e.id==Event.WINDOW_DESTROY ) {hide();}
       return super.handleEvent(e);
    }

    /**
     * Montre la fenetre d'options de sortie
     * pour le résultat du cross-match
     * Crée d'abord l'objet si besoin est
     */
    private void showOptionsFrame() {
    	if( optionFrame==null ) optionFrame = new OptionFrame(aladin);
    	PlanCatalog pc1 = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(memPlanA)];
    	PlanCatalog pc2 = (PlanCatalog)aladin.calque.plan[aladin.calque.getIndexPlan(memPlanB)];
    	optionFrame.update(pc1, pc2);
    	optionFrame.setVisible(true);
    	optionFrame.toFront();
    }

    // classe interne représentant un bouton pour choisir ou non une colonne
    // à intégrer au résultat du cross-match
    class ColButton extends MyButton {
    	Field field;
    	Color hlColor = Color.black;;
    	boolean hl = false;

    	ColButton(Aladin aladin, String name, Field field) {
    		super(aladin, name);
    		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    		this.field = field;
    		setAlwaysUp(true);
    		// le bouton est sélectionné par défaut
    		push();
    	}

    	private void hilight(boolean b) {
    		hilight(b, Aladin.MYBLUE);

    	}

    	private void hilight(boolean b, Color c) {
    		hl = b;

    		Color toSet = b?c:Color.black;
    		if( ! (toSet.equals(hlColor) ) ) {
    			hlColor = toSet;
    			repaint();
    		}
    	}

    	   // Dessin du bouton
   void draw(Graphics g) {
      // Dessin du fond
      if( getColor()==Aladin.COLOR_GREEN && !hl ) {
        g.setColor(Aladin.MYBLUE);
        g.fillRect(1,1,W-2,H-2);
      } else {
      if( !hl ) g.setColor(getBackground());
      else g.setColor(hlColor);
      g.fillRect(0,0,W,H);

      }

      // Couleur du fond du bouton
      if( mode==DOWN ) {
         g.setColor(hl?hlColor:Color.gray);
         g.fillRect(0,1,W,H-1);
      }

      if( image!=null ) {
         int h = image.getHeight(this);
     	 if( h>H ) g.drawImage(image,2,2,W-4,H-4,this);
     	 else g.drawImage(image,2,2+(H-h)/2,this);
      }
      // Dessin des bords
      g.setColor( Color.black );
      if( type!=NORMAL && mode==DOWN ) {
//         g.drawRect(1,1,W-3,H-2);
         g.setColor( Color.black );
         g.drawLine(0,1,W-1,1);
         if( type!=RIGHT ) g.drawLine(0,1,0,H-1);
         g.setColor(Color.white);
         if( type!=TOP ) g.drawLine(1,H-1,W-1,H-1);
         if( type!=LEFT ) g.drawLine(W-1,1,W-1,H-1);

//         int x=(type==RIGHT)?0:W-2;
//         int SZ=(type==RIGHT)?1:2;
////         int y=H/3 - TRI;
//         int y=H/2-TRI;
//         g.setColor( Aladin.BLUE );
//         g.fillRect(x,y,2,TRI*2);
//         g.setColor( Color.black );
//         g.drawLine(x,y,x+SZ,y);
//         g.drawLine(x,y+TRI*2,x+SZ,y+TRI*2);

      } else if( !modeMenu || modeMenu && getColor()==Aladin.COLOR_GREEN ){
         g.setColor( (mode!=DOWN)?Color.white:Color.darkGray);
         g.drawLine(1,1,W-2,1);
         g.drawLine(1,1,1,  H-2);
         g.setColor( mode!=DOWN?Color.darkGray:Color.white );
         g.drawLine(W-2,1,  W-2,H-2);
         g.drawLine(W-2,H-2,1,  H-2);
      }

      // Dessin du triangle si nécessaire
      if( withTriangle ) {
      	g.setColor(mode==DOWN?LIGHT_GREY:GREY);
      	g.drawLine(W-16,H/2+5,W-9,H/2+1);

      	g.setColor(mode==DOWN?GREY:LIGHT_GREY);
      	g.drawLine(W-9,H/2,W-16,H/2-4);
      	g.drawLine(W-16,H/2-3,W-16,H/2+5);
      }
   }

    	/** switch le caractère "main" de l'UCD du field du bouton */
    	private void switchUcdMain() {
    		String ucd = field.ucd;
    		if( ucd==null ) return;
    		String newVal;
    		int k;
    		if( (k=ucd.toLowerCase().indexOf(";meta.main"))>=0 ) {
    			newVal = ucd.substring(0,k);
    		}
    		else if( (k=ucd.toLowerCase().indexOf("_main"))>=0 ) {
    			newVal = ucd.substring(0,k);
    		}
    		// ajout de "_MAIN"
    		else if( ucd.toUpperCase().equals(ucd) ) {
    			newVal = ucd+"_MAIN";
    		}
    		// ajout de ";meta.main"
    		else {
    			newVal = ucd+";meta.main";
    		}
    		field.ucd = newVal;
    		optionFrame.ucdLabel.setText(field.ucd==null?"":field.ucd);
    		optionFrame.hilightBtnsWithUcd(field.ucd);

    	}

    	   public void mousePressed(MouseEvent e) {}

    	   public void mouseReleased(MouseEvent e) {
    	      if( pm==null ) postEvent(new Event(new JButton(text),Event.ACTION_EVENT,text) );

    	      if( (e.getModifiers() & KeyEvent.CTRL_MASK) >0 ) {
    	      	switchUcdMain();
    	      	return;
    	      }
    	      if( mode==DOWN ) pop();
    	      else if( mode==UP ) push();

    	   }

    	   public void mouseEntered(MouseEvent e) {
    	       if( optionFrame!=null && optionFrame.ucdLabel!=null && field!=null ) {
    	           optionFrame.ucdLabel.setText(field.ucd==null?"":field.ucd);
    	           optionFrame.hilightBtnsWithUcd(field.ucd);
    	       }
    	       super.mouseEntered(e);
    	   }
    	   public void mouseExited(MouseEvent e) {
    	       if( optionFrame!=null && optionFrame.ucdLabel!=null ) {
    	           optionFrame.ucdLabel.setText("");
    	           optionFrame.hilightBtnsWithUcd(null);
    	       }

    	       super.mouseExited(e);
    	   }

    }

    // classe interne permettant le choix des colonnes de sortie,
    // du préfixe, du suffixe, et les UCDs meta.main
    class OptionFrame extends JFrame {
    	static final String SELECT_ALL = "Select all";
    	static final String SELECT_NONE = "Deselect all";

    	Aladin aladin;
    	String prefix1 = "";
    	String prefix2 = "";
    	String suffix1 = "_tab1";
    	String suffix2 = "_tab2";

    	JButton bAll1, bAll2;

    	Field[] fields1;
    	Field[] fields2;

    	JTextField prefix1TF, prefix2TF, suffix1TF, suffix2TF;
    	Plan pc1, pc2;
    	ColButton[] col1;
    	ColButton[] col2;

    	JLabel ucdLabel;

        OptionFrame(Aladin aladin) {
            super("Options for xmatch ouput");

            this.aladin = aladin;
            Aladin.setIcon(this);

            Util.setCloseShortcut(this, false, aladin);
//            setBackground(Aladin.BKGD);
            getContentPane().setLayout(new BorderLayout());

            setLocation(100,100);

        }

        Vector conflict = new Vector();

        void hilightBtnsWithUcd(String ucd) {
        	conflict.clear();
        	hilightBtnsWithUcd(ucd, col1);
        	hilightBtnsWithUcd(ucd, col2);
        	// on met en rouge les colonnes en conflit au niveau des UCDs
        	if( conflict.size()>=2 ) {
        		Enumeration e = conflict.elements();
        		while( e.hasMoreElements() ) {
        			((ColButton)e.nextElement()).hilight(true, Color.RED);
        		}
        	}
        }



        void hilightBtnsWithUcd(String ucd, ColButton[] btns) {
        	String tmp,otmp;
//        	int k;

        	if( ucd!=null ) {
        		ucd = getUcdRoot(ucd.toLowerCase());
        	}
        	for( int i=0; i<btns.length; i++ ) {
        		if( ucd==null ) {
        			btns[i].hilight(false);
        			continue;
        		}
        		//main = false;
        		if( btns[i].field.ucd==null || btns[i].field.ucd.length()==0 ) continue;
        		otmp = btns[i].field.ucd.toLowerCase();



        		tmp = getUcdRoot(otmp);
        		/*
        		k = tmp.indexOf(";meta.main");
        		if( k>=0 ) {
        			main = true;
        			tmp = tmp.substring(0, k);
        		}
        		*/
        		if( tmp.equals(ucd) ) {
        			btns[i].hilight(true);
        			if( otmp.indexOf(";meta.main")>=0 || otmp.indexOf("_main")>=0 )
        				conflict.addElement(btns[i]);
        		}
        		else if( btns[i].hl ) btns[i].hilight(false);
        	}
        }

        private String getUcdRoot(String s) {
        	String tmp = s.toLowerCase();
        	int k = tmp.indexOf(";meta.main");
        	if( k<0 ) k = tmp.indexOf("_main");
        	if( k>=0 ) {
        		tmp = tmp.substring(0, k);
        	}
        	return tmp;
        }

        void update(Plan pc1, Plan pc2) {
        	this.pc1 = pc1;
        	this.pc2 = pc2;

        	if( prefix1TF!=null ) saveValues();

            this.getContentPane().removeAll();

        	buildFrame();
        }

        /** Construit entièrement la Frame */
        private void buildFrame() {
            JPanel p = new JPanel();
            GridBagLayout g =  new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0,5,0,5);
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.weighty = 0.5;

            p.setLayout(g);
            p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(null, "Output options", TitledBorder.CENTER, TitledBorder.TOP, Aladin.BOLD),
                    BorderFactory.createEmptyBorder(5,5,5,5))));


            prefix1TF = new JTextField(prefix1, 5);
            suffix1TF = new JTextField(suffix1, 5);
            JPanel panelPc1 = getPanelForCat("Catalogue A : "+pc1.getLabel(), pc1, prefix1TF, suffix1TF, col1, fields1, true);
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(panelPc1,c);
            p.add(panelPc1);

//            addFilet(p,g,c,1);

            prefix2TF = new JTextField(prefix2, 5);
            suffix2TF = new JTextField(suffix2, 5);
            JPanel panelPc2 = getPanelForCat("Catalogue B : "+pc2.getLabel(), pc2, prefix2TF, suffix2TF, col2, fields2, false);
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(panelPc2,c);
            p.add(panelPc2);

            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.0;
            c.weighty = 0.0;

//            addFilet(p,g,c,1);

            JLabel l3 = new JLabel("UCD: ");
            l3.setFont(Aladin.ITALIC);
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.RELATIVE;
            g.setConstraints(l3, c);
            p.add(l3);
            ucdLabel = new JLabel("                                  ");
            ucdLabel.setFont(Aladin.BOLD);
            ucdLabel.setForeground(Aladin.MYBLUE);
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            g.setConstraints(ucdLabel, c);
            p.add(ucdLabel);

            // panel principal
            getContentPane().add(p, BorderLayout.CENTER);
            // panel du bas avec bouton de validation
            getContentPane().add(bottomPanel(), BorderLayout.SOUTH);
            pack();
        }

        JPanel getPanelForCat(String title, Plan pc, JTextField prefTF, JTextField sufTF, ColButton[] buttons, Field[] fields, boolean flag) {
        	JPanel p = new JPanel();

            p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
            // bordure avec titre
            p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2,2,2,2), BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, Aladin.LITALIC),
                    BorderFactory.createEmptyBorder(2,2,2,2))));

            JPanel prefixPanel = new JPanel(new FlowLayout());

            JLabel l1 = new JLabel("Column prefix");
            prefixPanel.add(l1);
            prefixPanel.add(prefTF);
            JLabel l2 = new JLabel("Column suffix");
            prefixPanel.add(l2);
            prefixPanel.add(sufTF);
            p.add(prefixPanel);

            p.add(Box.createRigidArea(new Dimension(0,10)));

            JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
            subPanel.setBorder(BorderFactory.createTitledBorder(null, "Choose columns to keep", TitledBorder.CENTER, TitledBorder.TOP));

            JButton bAll = new JButton(SELECT_NONE);
            bAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    switchState((JButton)ae.getSource());
                }
            });
            bAll.setFont(Aladin.ITALIC);
            bAll.setForeground(Color.blue);
            bAll.setAlignmentX(Component.CENTER_ALIGNMENT);
            subPanel.add(bAll);

            if( flag ) bAll1 = bAll;
            else bAll2 = bAll;

            // ensemble des colonnes
            JPanel colPanel = createButtons(pc, buttons, fields, flag);
            colPanel.setAlignmentY(0.5f);
            subPanel.add(colPanel);

            p.add(subPanel);
            return p;
        }

        private JPanel createButtons(Plan pc, ColButton[] b, Field[] f, boolean flag) {
//        	Vector v = new Vector();
//            String[] columns = FrameColumnCalculator.getCol(pc, v);
            f = pc.getFirstLegende().field;

            int nbCol = 4;
            int maxNbRow = 5;
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(0, nbCol, 0, 0));
            b = new ColButton[f.length];


            boolean needScroll = f.length > nbCol*maxNbRow;
            int maxWidth = 0;
            int tmp;
            FontMetrics fm = null;
            if( needScroll ) fm = Toolkit.getDefaultToolkit().getFontMetrics(Aladin.SPLAIN);


            for( int i=0; i<f.length; i++ ) {
                b[i] = new ColButton(aladin, f[i].name, f[i]);
                b[i].setFont(Aladin.SPLAIN);
                if( needScroll && (tmp=fm.stringWidth(f[i].name))>maxWidth )
                    maxWidth = tmp;
                p.add(b[i]);
            }

            if( flag ) {
            	col1 = b;
            	fields1 = f;
            }
            else {
            	col2 = b;
            	fields2 = f;
            }


            // ajout d'un scrollpane s'il y a trop de lignes
            if(  needScroll ) {
                ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
                sp.setSize((maxWidth+20)*nbCol+30, 140);
                sp.add(p);
                JPanel newPanel = new JPanel();
                newPanel.add(sp);
                return newPanel;
            }
            else return p;
        }

        private void saveValues() {
        	prefix1 = prefix1TF.getText();
        	prefix2 = prefix2TF.getText();
        	suffix1 = suffix1TF.getText();
        	suffix2 = suffix2TF.getText();
        }

        /** creates bottom panel with buttons for creating new column and closing the frame */
        private JPanel bottomPanel() {
            JPanel p = new JPanel();
            JButton b;
            p.setLayout(new FlowLayout(FlowLayout.CENTER));
            p.add(b = new JButton(CLOSE));
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setVisible(false);
                }
            });
            return p;
        }

        private void switchState(JButton b) {
        	boolean state = b.getText().equals(SELECT_ALL);
        	selectAll(state, b==bAll1?col1:col2);
			b.setText(state?SELECT_NONE:SELECT_ALL);
        }

        private void selectAll(boolean state, ColButton[] btns) {
        	for( int i=0; i<btns.length; i++ ) {
        		if( state ) btns[i].push();
        		else btns[i].pop();
        	}
        }

        // Gestion des evenements
        public boolean handleEvent(Event e) {

            if( e.id==Event.WINDOW_DESTROY ) {
                setVisible(false);
            }
            return super.handleEvent(e);
        }

    }
}
