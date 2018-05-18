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

/*
 * Created on 12 janv. 2004
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import cds.savot.model.SavotField;
import cds.tools.Util;
import cds.tools.parser.Parser;


/** GUI for the column calculator
 * @author Thomas Boch [CDS]
 * @version 1.1 Dec. 2007 : passage à Swing
 *          0.5 Apr. 2004 : refonte
 *          0.1 Jan. 2004 : kickoff
 */
public class FrameColumnCalculator extends JFrame implements ActionListener {
    
    private static String TITLE,ADDCOL,ADDNEW,NAME,UNIT,DECI,KEEP,EXPR,NOEXPR,COL,CREATIONOK,
                          PICKNAME,PICKOP,FUNCT,NEEDNAME,ALLREADYEXIST,ERROR,CLOSE;
    
	protected static String[] OPERATORS = {"+","-","*","/","^","(",")"};;
	
    static private final Insets BUTTON_INSETS = new Insets(1,2,1,2);
    
    // plan catalog pour lequel on ajoute une colonne
    private Plan pc;
    
    private JButton[] buttons;
    private JTextField nameTF, ucdTF, unitTF;
    private JTextArea expressionTA;
    private JComboBox nbDecChoice;
    
    // contient les noms des colonnes
    private Vector vCol;
    // liste des fonctions dispos
    private JComboBox funcChoice;
    
    private Aladin a;

    /** Constructeur
     * @param a la reference habituelle a Aladin
     */
    public FrameColumnCalculator(Aladin a) {
        super();
        this.a = a;
        Aladin.setIcon(this);
        getContentPane().setLayout(new BorderLayout());
        
        Util.setCloseShortcut(this, true,a);
        
        setLocation(Aladin.computeLocation(this));
        
    }
    
    protected void createChaine() {
       if( TITLE!=null ) return;	// déjà fait
       TITLE = a.chaine.getString("CCTITLE");
       ADDCOL = a.chaine.getString("CCADDCOL");
       ADDNEW = a.chaine.getString("CCADDNEW");
       NAME = a.chaine.getString("CCNAME");
       UNIT = a.chaine.getString("CCUNIT");
       DECI = a.chaine.getString("CCDECI");
       KEEP = a.chaine.getString("CCKEEP");
       EXPR = a.chaine.getString("CCEXPR");
       PICKNAME = a.chaine.getString("CCPICKNAME");
       PICKOP = a.chaine.getString("CCPICKOP");
       FUNCT = a.chaine.getString("CCFUNCT");
       NEEDNAME = a.chaine.getString("CCNEEDNAME");
       NOEXPR = a.chaine.getString("CCNOEXPR");
       ALLREADYEXIST = a.chaine.getString("CCALLREADYEXIST");
       ERROR = a.chaine.getString("ERROR");
       CLOSE = a.chaine.getString("CLOSE");
       COL = a.chaine.getString("CCCOL");
       CREATIONOK = a.chaine.getString("CCCREATIONOK");
       
       setTitle(TITLE);
    }

    
    /** Mise à jour de la frame
     * @param p PlanCatalog auquel on veut ajouter des colonnes
     */
    protected void update(Plan pc) {
        this.pc = pc;
        
        createChaine();
        
        vCol = null;
        getContentPane().removeAll();
        buildFrame();
    }
    
    /** Construit entièrement la Frame */
    private void buildFrame() {
        JPanel p = new JPanel();
//        GridBagLayout g =  new GridBagLayout(); 
//        GridBagConstraints c = new GridBagConstraints();
//        c.insets = new Insets(2,5,2,5);
//        c.fill = GridBagConstraints.NONE;
//        c.gridwidth = GridBagConstraints.REMAINDER;
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
//        p.setLayout(g);
        
       
        // panel du haut
        JPanel topPanel = topPanel();
//        c.fill = GridBagConstraints.BOTH;
//        c.weightx = 1.0;
//        c.weighty = 1.0;
//        g.setConstraints(topPanel, c);
        p.add(topPanel);

//        c.fill = GridBagConstraints.NONE;
//        c.weightx = 0.0;
//        c.weighty = 0.0;
        
        // panel avec les boutons des colonnes
        JPanel pButtons = createButtons();
//        g.setConstraints(pButtons, c);
        p.add(pButtons);
        
        // panel avec les operateurs et les fonctions dispos
        JPanel funcPanel = funcPanel();
//        g.setConstraints(funcPanel, c);
        p.add(funcPanel);
        
        // ajout d'une bordure
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(15,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, ADDNEW+" "+pc.label, TitledBorder.CENTER, TitledBorder.TOP),
                BorderFactory.createEmptyBorder(5,5,5,5))));
        
        getContentPane().add(p, BorderLayout.CENTER);
        
        // panel du bas avec bouton de validation
        getContentPane().add(bottomPanel(), BorderLayout.SOUTH);
        
        pack();
        
    }
    
    /** creates top panel with textfields to enter the name of the new column
     *  and the textarea to enter the expression
     * @return JPanel
     */
    private JPanel topPanel() {
        JPanel p = new JPanel();
        GridBagLayout g =  new GridBagLayout(); 
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.setLayout(g);
        
        c.anchor = GridBagConstraints.WEST;
        // name
        nameTF = new JTextField(20);
        JLabel lName = new JLabel(NAME);
        lName.setFont(Aladin.BOLD);
        FilterProperties.addCouple(p, lName, nameTF, g, c);
        
        // ucd
        ucdTF = new JTextField(20);
        FilterProperties.addCouple(p, "UCD", ucdTF, g, c);
        
        // unit
        unitTF = new JTextField(20);
        FilterProperties.addCouple(p, UNIT, unitTF, g, c);
        
        // nb of decimals to keep
        nbDecChoice = new JComboBox();
        nbDecChoice.setFont(Aladin.PLAIN);
        for( int i=1; i<13; i++ ) nbDecChoice.addItem(i+"");
        nbDecChoice.setSelectedIndex(3);
        JPanel pDec = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        pDec.add(nbDecChoice);
        JLabel decLabel = new JLabel(" "+DECI);
        decLabel.setFont(Aladin.ITALIC);
        pDec.add(decLabel);
        FilterProperties.addCouple(p, KEEP, pDec, g, c);
        
        // expression for new column
        expressionTA = new JTextArea("",5,35);
        expressionTA.setLineWrap(true);
        expressionTA.setWrapStyleWord(true);
        expressionTA.setFont(Aladin.COURIER);
        JLabel lExp = new JLabel(EXPR);
        lExp.setFont(Aladin.BOLD);
        c.gridwidth = GridBagConstraints.RELATIVE;
        g.setConstraints(lExp, c);
        p.add(lExp);
        JLabel lExample = new JLabel("eg: ${Bmag}-${Vmag}");
        lExample.setFont(Aladin.ITALIC);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        g.setConstraints(lExample,c);
        p.add(lExample);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane scrollExprTA = new JScrollPane(expressionTA, 
                                                   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        g.setConstraints(scrollExprTA, c);
        p.add(scrollExprTA);
        
        // reset
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        
        c.anchor = GridBagConstraints.CENTER;
        
        return p;
    }
    
    /** creates panel with basic operators and available functions
     * @return JPanel
     */
    private JPanel funcPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
        
        // ajout des operateurs
//        JPanel opPanel = new JPanel();
//        opPanel.setLayout(new FlowLayout());
        
        JButton b;
		for( int i=0; i<OPERATORS.length; i++ ) {
//            opPanel.add(b = createButton(OPERATORS[i]));
		    b = createButton(OPERATORS[i]);
		    b.setMaximumSize(new Dimension(30,35));
		    b.setPreferredSize(new Dimension(30,35));
            b.setFont(Aladin.COURIER);
            p.add(b);
            p.add(Box.createHorizontalGlue());
        }
//		p.add(opPanel, BorderLayout.WEST);
        
        // ajout des fonctions
        funcChoice = new JComboBox();
        funcChoice.setFont(Aladin.PLAIN);
        funcChoice.addActionListener(this);
        funcChoice.addItem(FUNCT);
        String[] func = Parser.getAvailFunc();
        FilterProperties.sortLexico(func);
        for( int i=0; i<func.length; i++) {
            funcChoice.addItem(func[i]);
        }
        p.add(funcChoice);
//        opP.add(funcChoice);
        
        
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, PICKOP, TitledBorder.CENTER, TitledBorder.TOP, Aladin.COURIER),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        
        return p;
    }
    
    /** creates bottom panel with buttons for creating new column and closing the frame */
    private JPanel bottomPanel() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton b;
        p.add(b = createButton(ADDCOL));
        b.setFont(Aladin.BOLD);
        b.setMargin(null);
        p.add(b = createButton(CLOSE));
        b.setFont(Aladin.PLAIN);
        b.setMargin(null);
        
        return p;
    }
    
    private JButton createButton(String s) {
        JButton b = new JButton(s);
        b.setMargin(BUTTON_INSETS);
        b.addActionListener(this);
        
        return b;
    }
    
    /** Crée les boutons permettant d'écrire l'expression */
    private JPanel createButtons() {
        String[] columns = getCol(pc);
        int nbCol = 4;
        int maxNbRow = 5;
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, nbCol, 3, 3));
        buttons = new JButton[columns.length];
        boolean needScroll = columns.length > nbCol*maxNbRow;
        int maxWidth = 0;
        int tmp;
        FontMetrics fm = null;
        if( needScroll ) fm = Toolkit.getDefaultToolkit().getFontMetrics(Aladin.PLAIN);
        
        for( int i=columns.length-1; i>=0; i-- ) {
            buttons[i] = createButton(columns[i]);
            buttons[i].setFont(Aladin.PLAIN);
            if( needScroll && (tmp=fm.stringWidth(columns[i]))>maxWidth )
                maxWidth = tmp;
            p.add(buttons[i]);
        }
        
        JPanel retPanel;
        // ajout d'un scrollpane s'il y a trop de lignes
        if(  needScroll ) {
            JScrollPane sp = new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setPreferredSize(new Dimension((maxWidth+20)*nbCol+40, 140));
            JPanel newPanel = new JPanel();
            newPanel.add(sp);
            retPanel = newPanel;
        }
        else retPanel = p;
        
        retPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, PICKNAME, TitledBorder.CENTER, TitledBorder.TOP, Aladin.COURIER),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        
        return retPanel;
    }
    
    private String[] getCol(Plan p) {
        if( vCol==null ) {
            Source s;
            String str;
            vCol = new Vector();
        
            Iterator<Obj> it = p.iterator();
            while( it.hasNext() ) {
               // pour chaque source, on récupère les noms de colonnes (oui, c'est un peu lourd)
               Obj o = it.next();
               if( !(o instanceof Source) ) continue;
               s = (Source)o;
               for( int k=s.getLeg().field.length-1;k>=0;k-- ) {
                  str = s.getLeg().field[k].name;
                  if( vCol.indexOf(str)<0 ) vCol.addElement(str);
               }
            }
        }
        
        String[] result = new String[vCol.size()];
        vCol.copyInto(result);
        return result;
    }
    
    static protected String[] getCol(Plan p, Vector v) {
		Source s;
		String str;
		v = new Vector();

		Iterator<Obj> it = p.iterator();
		while( it.hasNext() ) {
           Obj o = it.next();
           if( !(o instanceof Source) ) continue;
           s = (Source)o;
		   for (int k = s.getLeg().field.length - 1; k >= 0; k--) {
		      str = s.getLeg().field[k].name;
		      if (v.indexOf(str) < 0) v.addElement(str);
		   }
		}

		String[] result = new String[v.size()];
		v.copyInto(result);
		return result;
    }
    
    public void actionPerformed(ActionEvent ae) {
        Object src = ae.getSource();
        String what = ae.getActionCommand();
        
        if( what.equals(CLOSE) ) {
            setVisible(false);
        }
        else if( what.equals(ADDCOL) ) {
            addColumn();
        }
        else if( src.equals(funcChoice) ) {
            int index = funcChoice.getSelectedIndex();
            if( index>0 ) {
                int pos = FilterProperties.insertInTA(expressionTA, funcChoice.getItemAt(index).toString()+"()",
                                        expressionTA.getCaretPosition());
                expressionTA.setCaretPosition(pos-1);
                expressionTA.requestFocus();
                funcChoice.setSelectedIndex(0);
            }
        }
        else if( src instanceof JButton ) {
            String btnLabel = ((JButton)src).getText();
            // insertion du label d'une colonne
            if( vCol.contains(btnLabel) ) {
                FilterProperties.insertInTA(expressionTA, "${"+btnLabel+"}",
                                            expressionTA.getCaretPosition());
            }
            // insertion d'un opérateur
            else {
                FilterProperties.insertInTA(expressionTA, btnLabel,
                                            expressionTA.getCaretPosition());
            }
            expressionTA.requestFocus();
        }
    }
    
    /** performs the computation */
    private void addColumn() {
        String name, ucd, unit, expr;
        int nbDec;
        name = nameTF.getText();
        ucd = ucdTF.getText();
        unit = unitTF.getText();
        expr = MetaDataTree.replace(MetaDataTree.replace(expressionTA.getText(), "\n", "", -1), "\r", "", -1);
        try {
        	nbDec = Integer.parseInt(nbDecChoice.getSelectedItem().toString());
        }
        catch(NumberFormatException e) {nbDec = 4;}
       
        // le nom de la nouvelle colonne ne doit pas être vide
       if( name.length()==0 ) {
           Aladin.error(this, NEEDNAME, 1);
           return;
       }
       
       if( expr.length()==0 ) {
           Aladin.error(this, NOEXPR, 1);
           return;
       }
       
        // on vérifie que le nom de la colonne n'existe pas déja
        if( colExist(name) ) {
        	Aladin.error(this, ALLREADYEXIST+name+"]");
        	return;
        }
       
        Aladin.trace(3,"expr is : "+expr);
       
        SavotField f = new SavotField();
        f.setName(name);
        f.setUcd(ucd);
        f.setUnit(unit);
        
        // écriture sur la console de la commande script équivalente
        a.console.printCommand("addcol "+pc.label+","+name+","+expr+","+unit+","+ucd+","+nbDec);
        
        ColumnCalculator cc = new ColumnCalculator(new SavotField[] {f}, new String[] {expr}, pc, nbDec, a);
        if( !cc.createParser() ) {
            Aladin.error(this, ERROR+" : "+cc.getError(), 1);
            return;
        }
       
        cc.compute();
        Aladin.info(COL + " '" + name + "' " + CREATIONOK);
        this.setVisible(false);
    }

    /**
     * Teste si le nom s est déja utilisé comme label de colonne dans pc
     * @param s nom à tester
     * @return true si s est le label d'une colonne de pc
     */
    private boolean colExist(String s) {
    	String[] noms = getCol(pc);
    	return (Util.indexInArrayOf(s, noms)>=0);
    }
    
    /**
     * Teste si le nom s est déja utilisé comme label de colonne dans p
     * @param s nom à tester
     * @param p Plan catalogue où l'on cherche
     * @return true si s est le label d'une colonne de p
     */
    static protected boolean colExist(String s, Plan p) {
    	String[] noms = getCol(p, new Vector());
    	return (Util.indexInArrayOf(s, noms)>=0);
    }

}
