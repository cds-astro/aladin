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

import java.util.Enumeration;
import java.util.Vector;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.*;


/** Un arbre hiérarchique 
 * @author Thomas Boch [CDS]
 * @version 0.5 ??
 */

public class BasicTree extends JComponent 
                       implements MouseMotionListener, MouseListener, ActionListener {
  
    // references to ScrollPane for the tree
    private JScrollPane scroll = null;

    boolean hasSpectraOrImages;
    
	// popupmenu pour actions associées à un noeud/feuille de l'arbre
    protected JPopupMenu popup;
    //popupmenu pour actions générales sur l'arbre (expand/collapse all)
    protected JPopupMenu popupPrefs;
    
    protected static final String DELETE = "Delete";
	protected static final String COLLAPSE_SUBTREE = "Collapse this subtree";
	protected static final String EXPAND_SUBTREE = "Expand this subtree";
    protected static final String COLLAPSE_ALL = "Collapse all";
    protected static final String EXPAND_ALL = "Expand all";
    protected static final String FLAT_VIEW = "Flat view";
    protected static final String HIER_VIEW = "Hierarchical view";
    protected static final String SIAP_EVOL_SORT = "Sort";
    protected static final String CREATE_CATPLANE = "Create a catalog plane with all images/spectra";

	// option pour affichage à plat
	boolean flatView = false;

    private int prefHeight=600;
    private int prefWidth=250;
    
    static final int XSPACE=22;
    static final int YSPACE=18;
    static final int LOGOSIZE = YSPACE-6;
    static final int LOGOPADDING = 4;

    static final Color LITBGCOLOR = new Color(189,222,237);
    static final Color LITFGCOLOR = Color.black;
    static final Color selectedColor = new Color(255,190,255);
    static final Color mouseOverColor = Color.blue;
//    static final Color underlineColor = Color.green;
    static final Color bkgdLeafColor = Aladin.MYBLUE; // couleur pour une feuille survolée
    static final Color bkgdLeafClickedColor = Aladin.COLOR_CONTROL_BACKGROUND; // couleur pour une feuille cliquée
    
    // couleur des lignes
    static final Color lineColor = Color.black;
    
    static Font nameFont = Aladin.COURIER;
    static Font boldNameFont = Aladin.BCOURIER;
   
    private BasicNode rootNode;  	    // racine de l'arborescence
  
    // dernier noeud dont on a montre les infos
    BasicNode lastInfoNode;

    private BasicNode ancienCurNode;

	boolean isHistoryTree = false;
	

	// couleur de fond
    protected Color bkgColor = Aladin.COLOR_CONTROL_BACKGROUND;

//    protected Color bkgColor = Color.white;
    
    // true si l'initialisation n'a pas encore ete faite
    private boolean mustInit = true; 

//    private int neededHeight=0;

	// buffer et contexte graphique
//    private Image buffer;
//    Graphics h;

    // peut on trier selon les champs ?
    private boolean allowSortByFields = true;
    
    private int hilightNode;
    int oHilightNode = -1;
    BasicNode selectedNode,oselectedNode;
    private Vector nodeFullList;    // contient une reference sur tous les noeuds
    BasicNode[] nodeFullTab;     // reference sur tous les noeuds
    private Vector nodeList;	    // contient l'ensemble des noeuds actuellement affiches
    BasicNode[] nodeTab;  // tableau des noeuds actuellement affiches
    private int yCurrent=0;

    // lastChild[i] vrai si le dernier noeud de niveau i est le dernier fils du noeud père
    private boolean[] lastChild = new boolean[100];
    
	// référence à l'objet aladin
	Aladin aladin;
    
    // Constructors
    private BasicTree(Aladin aladin, BasicNode rootNode) {
    	this.aladin = aladin;
    	this.rootNode=rootNode;
    	this.rootNode.hide = true;
    	setBackground(bkgColor);
        setOpaque(true);
        setDoubleBuffered(false);
		createPopup();
        traverseTree();
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    // REFLECHIR A LA TAILLE DU CANVAS CREE !!
    BasicTree(Aladin aladin, BasicNode rootNode, JScrollPane scroll) {
        this(aladin, rootNode);
        this.scroll = scroll;
        // pour la roulette
//        scroll.getVAdjustable().setUnitIncrement(YSPACE);
    }
    
	BasicTree(Aladin aladin, BasicNode rootNode, JScrollPane scroll, boolean isHistoryTree) {
		this.aladin = aladin;
		this.rootNode=rootNode;
		this.rootNode.hide = true;
		this.isHistoryTree = isHistoryTree;
		setBackground(bkgColor);
        setOpaque(true);
        setDoubleBuffered(false);
		createPopup();
		traverseTree();
		this.scroll = scroll;
		// pour la roulette
//		scroll.getVAdjustable().setUnitIncrement(YSPACE);
        addMouseMotionListener(this);
        addMouseListener(this);
	}
    
    //  End of constructors

    
    protected JMenuItem createItem(String s) {
        JMenuItem item = new JMenuItem(s);
        item.setFont(Aladin.PLAIN);
        item.addActionListener(this);
        return item;
    }

	private void createPopup() {
		if( popup==null ) {
            popup = new JPopupMenu();
        }
        else popup.removeAll();
        
		popup.add(createItem(DELETE));
		
		popup.addSeparator();
		popup.add(createItem(COLLAPSE_SUBTREE));
		popup.add(createItem(EXPAND_SUBTREE));
		
		
		
		
		popupPrefs = new JPopupMenu();
		popupPrefs.add(createItem(COLLAPSE_ALL));
        popupPrefs.add(createItem(EXPAND_ALL));
        
        if( !isHistoryTree && hasSpectraOrImages ) {
            // ligne de séparation
            popupPrefs.addSeparator();
            popupPrefs.add(createItem(CREATE_CATPLANE));
        }

        
        // ligne de séparation
        popupPrefs.addSeparator();
		popupPrefs.add(flatView?createItem(HIER_VIEW):createItem(FLAT_VIEW));
        
        if( allowSortByFields ) {
            popupPrefs.addSeparator();
            popupPrefs.add(createItem(SIAP_EVOL_SORT));
        } 
	}

	/** fixe la couleur de fond de l'arbre */
	public void setBackground(Color color) {
		this.setOpaque(true);
	    super.setBackground(color);
	    this.bkgColor = color;
	    

	    
	}

	/** Déselectionne tous les noeuds */
    protected void clearSelected() {
        ancienCurNode=null;
        if( oHilightNode<0 || oHilightNode>=nodeTab.length ) return;
        BasicNode node = nodeTab[oHilightNode];
        
        oHilightNode = -1;
        drawNodeName(getGraphics(),node,true);
        
        repaint();
        
    }

    protected void majSize() {
        // tres important : fixer ces valeurs
        // prefWidth doit etre calcule pour version finale
        //prefWidth=350;
       
       // PF Mars 08 - Ca règle le problème de la scrollbar verticale qui prenait la
       // taille de l'arbre totalement déplié
        if( nodeList!=null ) prefHeight=nodeList.size()*YSPACE+20;
//        prefHeight=nodeFullTab.length*YSPACE+20;
        
        // on fixe taille minimale, sinon probleme d'artefacts visuels !
        if( scroll!=null ) {
            JViewport vPort = scroll.getViewport();
            if( prefHeight<vPort.getHeight() ) prefHeight = vPort.getHeight();
            if( prefWidth<vPort.getWidth() ) prefWidth = vPort.getWidth();
        }
        
        setSize(prefWidth, prefHeight);
        //System.out.println("prefHeight: "+prefHeight);
    }
   
    // a appeler une fois que ressource tree est dans un conteneur
    // PRE : nodeFullTab doit contenir tous les noeuds de l'arbre
    protected boolean init(Graphics h) {
        majSize();

        // test pour prendre moins de mémoire !
//        if( h!=null ) h.dispose();
        
//      buffer = createImage(getWidth(),getHeight());
//		if( buffer==null ) return false;
//    	h = buffer.getGraphics();
    	doDisplay(h);
		return true;
    }
    
//    // calcule la hauteur necessaire a l'affichage
//    // set neededHeight    
//    // NE SERT PAS ACTUELLEMENT
//    private void computeHeight() {
//    	neededHeight=0;
//    	computeNodeHeight(rootNode);
//    }
//    
//    // utilisé par computeHeight
//    // NE SERT PAS ACTUELLEMENT
//    private void computeNodeHeight(BasicNode node) {
//    	neededHeight+=YSPACE;
//    	if(!node.isLeaf && node.isOpen) {
//    	    Enumeration e = node.getChildren();
//    	    while(e.hasMoreElements()) {
//    	    	computeNodeHeight((BasicNode)e.nextElement());
//    	    }
//    	}
//    }
    
    protected Color getMouseOverColor(BasicNode node) {
    	return node.isLeaf?Color.white:mouseOverColor;
    }
    
    boolean mustScroll=false;
    /** Réaffiche l'arbre entièrement */
    void doDisplay(Graphics h) {
    	 	
        // Ces 2 lignes faisaient planter l'arbre sur le TX : le garbage collector ne se mettait pas en route, et toute la memoire etait prise
        // un gc en ligne de commande reglait le pb
    	/*buffer = createImage(size().width,size().height);
    	h = buffer.getGraphics();*/
    	
    	yCurrent=0;
    	nodeList = new Vector();
    	if( h==null ) return;
    	h.setColor(bkgColor);
    	h.fillRect(0,0,getWidth(),getHeight());
    	
        fullDisplay(h);
    }
    
    void fullDisplay(Graphics h) {
        // reset de last (sert pour flatView)
        last = false;
        h.setColor(Color.black);
        displayTree(h,rootNode,0);
        
        hilightNode=-1;

        nodeTab = new BasicNode[nodeList.size()];
        nodeList.copyInto(nodeTab);
        nodeList = null;
        // TEST 04/06/2003
        prefHeight = nodeTab.length*YSPACE; // TEST 04/06/2003 (en conjonction avec paint() )a commenter si on ne veut pas du dolayout à chaque click
        if( scroll!=null ) {
            JViewport vPort = scroll.getViewport();
            if( prefHeight<vPort.getHeight() ) prefHeight = vPort.getHeight();
        }

        if( getWidth()!=prefWidth || getHeight()!=prefHeight ) setSize(prefWidth,prefHeight); // autre test 13/06/2003 --> ligne non nécessaire en 1.4, mais nécessaire en 1.3
        
        //System.out.println("prefHeight2: "+prefHeight);
//        if( !mustInit && scroll!=null ) {
//            scroll.doLayout(); // solution originelle
//            // pour empêcher un bug d'affichage de l'arbre en Java 1.4
//            int tx = scroll.getHAdjustable().getValue();
//            int ty = scroll.getVAdjustable().getValue();
//            setLocation(-tx,-ty);
//    }

    }

	// dessine une ligne verticale de hauteur YSPACE
    private void drawVertLine(Graphics h,int x, int y) {
    	int xInit = x+XSPACE/2-5;
        h.setColor(lineColor);
        h.drawLine(xInit,y-4,xInit,y+YSPACE+2);
    }

    // dessine une ligne verticale de hauteur YSPACE/2
    private void drawHalfVertLine(Graphics h,int x, int y) {
		int xInit = x+XSPACE/2-5;
        h.setColor(lineColor);
        h.drawLine(xInit,y-4,xInit,y+YSPACE/2-1);
    }

	boolean last = false;
    private void displayTree(Graphics h,BasicNode node, int level) {
        nodeList.addElement(node);
        lastChild[level] = node.isLastChild();
        node.level = level;

        int x = -XSPACE+1;
    	for(int i=0;i<level;i++) {
            if( ! lastChild[i+1] && !flatView ) {
                drawVertLine(h,x+XSPACE,yCurrent);
            }
            else if( !flatView && i==level-1 ) {
                drawHalfVertLine(h,x+XSPACE,yCurrent);
            }

    	    x+=XSPACE;
    	}
    	
        if( !node.hide && ( node!=rootNode || !flatView) ) {
    	    displayIconLabel(h,node,x);
        }
        else if( !flatView && node==rootNode && node.getNbOfChildren()>0 ) {
        	drawVertLine(h,x+XSPACE,yCurrent);
        }
        else {
    	    x+=XSPACE;
    	    x+=XSPACE;
            node.x = x;
            node.y = yCurrent;
        }

    	
    	yCurrent+=YSPACE;
    	
        // affichage à plat
    	if( flatView ) {
    		//Vector v = new Vector();
    		//getAllLeaves(node,v);
    		//int size = v.size();
            //BasicNode[] leaves = new BasicNode[size];
            BasicNode[] leaves = getLeavesForFlatView(node);
            int size = leaves.length;
            //v.copyInto(leaves);
            // tri éventuel
            //Enumeration e = leaves.elements();
    		//int i=0;
    		//while( e.hasMoreElements() ) {
            for( int i=0; i<leaves.length; i++ ) {
    		    //i++;
    		    if( i==size ) last = true;
    		    //displayTree((BasicNode)e.nextElement(), level+1);     
                displayTree(h,leaves[i], level+1);
    		}
    	}
        // affichage hiérarchique classique (par défaut)
    	else {
    		// si on doit afficher les fils
    		if(!node.isLeaf && node.isOpen) {
    	    	Enumeration e = node.getChildren();
    	    	while(e.hasMoreElements()) {
    	    		displayTree(h,(BasicNode)e.nextElement(),level+1);
    	    	}
    		}
    	}
    }
    
    BasicNode[] getLeavesForFlatView(BasicNode node) {
        Vector v = new Vector();
        getAllLeaves(node,v);
        int size = v.size();
        BasicNode[] leaves = new BasicNode[size];
        v.copyInto(leaves);
        v = null;
        return leaves;
    }
  
  	static Color grayCB = new Color(102,102,102);
  	static Color grayFill = new Color(192,192,192);
  	static Color tickColor = Color.red;
    // dessine une checkbox
    protected void drawCheckBox(Graphics h,int x, int y, BasicNode node, boolean efface) {
//        int cote = YSPACE-6; // dimension d'un cote du carre
        int xBegin = x;
        int yBegin = y+2;
        if( efface ) {
            h.setColor(grayFill);
            h.fillRect(xBegin,yBegin,COTE,COTE);
        }
        h.setColor(Color.white);
        h.drawRect(xBegin+1,yBegin+1,COTE,COTE);
        h.setColor(grayCB);
        h.drawRect(xBegin,yBegin,COTE,COTE);
        // si selectionnee, on dessine l'espece de "V"
	    if( node.isSelected() ) {
            h.setColor(tickColor);
            h.fillRect(xBegin+3,yBegin+COTE/2-1,2,COTE/2);
            int x1 = xBegin+5;
            int y1 = yBegin+COTE-3;
            int l = COTE/2-1;
            h.drawLine(x1,y1,x1+l,y1-l);
            h.drawLine(x1,y1-1,x1+l,y1-l-1);
        }
    }

    static final int COTE = YSPACE-6; // cote du carré de la checkbox
    

    
    /**
     * 
     * @param x
     * @param y
     * @param node
     * @param efface si true, on efface la checkbox, sinon on la dessine
     */
    protected void drawPreCheckBox(Graphics h,int x, int y, BasicNode node, boolean efface) {
        if( !node.isLeaf ) return;
        
        int xBegin = x;
        int yBegin = y+2;
        

        int[] coordx = {xBegin+2, xBegin+5, xBegin+5, xBegin+10, xBegin+11, xBegin+11, xBegin+4, xBegin+2 };
        int[] coordy = {yBegin+COTE/2-2, yBegin+COTE/2-2, yBegin+COTE/2-2+3, yBegin+COTE/2-4, yBegin+COTE/2-4, yBegin+COTE/2-2, yBegin+COTE/2-2+7, yBegin+COTE/2-2+7};
        
        if( efface ) {
            if( !node.isSelected) {
                h.setColor(grayFill);
                h.fillRect(xBegin,yBegin,COTE,COTE);
            }
            else {
                h.setColor(grayFill);
                h.drawPolygon(coordx, coordy, coordx.length);
            }
        }
        h.setColor(Color.white);
        h.drawRect(xBegin+1,yBegin+1,COTE,COTE);
        h.setColor(grayCB);
        h.drawRect(xBegin,yBegin,COTE,COTE);
        
        if( efface ) return;
        
        // si selectionnee, on dessine l'espece de "V" évidé
        h.setColor(Color.black);
        h.drawPolygon(coordx, coordy, coordx.length);
    }

	/** Dessine le plus ou le moins d'un folder */
    private void drawLogo(Graphics h,int x, int y, BasicNode node) {
    	//int xInit = x+2;
		int xInit = x;
		
		h.setColor(Color.white);
		h.fillRect(xInit,y+2,LOGOSIZE, LOGOSIZE);
        h.setColor(Color.black);
        h.drawRect(xInit,y+2,LOGOSIZE, LOGOSIZE);
        
        if( node.isOpen ) {
//			h.setColor(Color.white);
//            h.drawLine(xInit+1+LOGOPADDING, y+3+LOGOSIZE/2, xInit+1+LOGOSIZE-LOGOPADDING, y+3+LOGOSIZE/2);
            h.setColor(Color.black);
            h.drawLine(xInit+LOGOPADDING, y+2+LOGOSIZE/2, xInit+LOGOSIZE-LOGOPADDING, y+2+LOGOSIZE/2);
        }
        else {
//            h.setColor(Color.white);
//            h.drawLine(xInit+1+LOGOSIZE/2, y+3+LOGOPADDING, xInit+1+LOGOSIZE/2, y+3+LOGOSIZE-LOGOPADDING);
//            h.drawLine(xInit+1+LOGOPADDING, y+3+LOGOSIZE/2, xInit+1+LOGOSIZE-LOGOPADDING, y+3+LOGOSIZE/2);
            h.setColor(Color.black);
            h.drawLine(xInit+LOGOSIZE/2, y+2+LOGOPADDING, xInit+LOGOSIZE/2, y+2+LOGOSIZE-LOGOPADDING);
            h.drawLine(xInit+LOGOPADDING, y+2+LOGOSIZE/2, xInit+LOGOSIZE-LOGOPADDING, y+2+LOGOSIZE/2);
            
            
        }
    }

    private void drawHorizLine(Graphics h,int x, int y, BasicNode node) {
        h.setColor(lineColor);
        if( node.isLeaf ) {
            h.drawLine(LOGOSIZE+x-XSPACE-5,yCurrent+YSPACE/2-1,LOGOSIZE+4+x+11-XSPACE,yCurrent+YSPACE/2-1);
        }
        else {
            h.drawLine(x-LOGOSIZE-3,yCurrent+YSPACE/2-1,x,yCurrent+YSPACE/2-1); 
        }
    	
    }

	private void drawNodeName(Graphics h,BasicNode node) {
	    drawNodeName(h,node,false);
	}

	private static Color[] sortFieldColor = {Color.decode("0xb7d2ca"), Color.decode("0xd0c2e9"), Color.decode("0xd5e0c4"), Color.decode("0xbccdf0")};
    
    protected void drawNodeName(Graphics h,BasicNode node, boolean redrawAll) {
    	if( node==rootNode && flatView ) return;
    	
        // dessin d'un rectangle par dessus le label 
        // Pour eviter bug ds IE6 Win XP (fontes degueulasses)
        String name = getName(node);
        if( redrawAll ) {
            h.setColor(bkgColor);
            h.fillRect( node.x-1,node.y,h.getFontMetrics().stringWidth(name)+2,YSPACE); 
        }
        
        boolean mouseOver = false;
        if( oHilightNode>=0 && oHilightNode<nodeTab.length && nodeTab!=null ) {
            
            if( node.equals(nodeTab[oHilightNode]) ) {
        	    mouseOver = true;
        	    
                if(node.isLeaf) {
                	drawSelectedLeafNodeName(h,node, name);
                }
        	}
        }

        
        h.setFont(node.equals(lastInfoNode)?boldNameFont:nameFont);
        
        int idx;
        boolean withSortField = (idx=name.indexOf("::: "))>=0 && !node.isLeaf;
        boolean selectedLeaf = false;
        String sortField = "";
        int offset = 0;
        if( withSortField ) {
        	name = MetaDataTree.replace(name, "///", "", 1);
        	sortField = name.substring(0,idx+1);
        	name = name.substring(idx+3);
        	h.setColor(sortFieldColor[node.level%sortFieldColor.length]);
        	offset = h.getFontMetrics().stringWidth(sortField)+1;
        	h.fillRect( node.x,node.y+1,offset,YSPACE-2);
        }
        else if( node.isLeaf && node.equals(lastInfoNode) ) {
        	selectedLeaf = true;
        }
        
        h.setColor(mouseOver?getMouseOverColor(node):Color.black);
        if( withSortField ) {
        	h.drawString(sortField,node.x,node.y+YSPACE/2+4);
        }
		if( selectedLeaf ) {
			// dessin du cadre pour une feuille sélectionnée
			drawBorder(h,node, h.getFontMetrics().stringWidth(name));
		}

		// dessin du nom du noeud
		h.drawString(name,offset+node.x,node.y+YSPACE/2+4);
    	
    }

    protected void drawSelectedLeafNodeName(Graphics h,BasicNode node, String name) {
    	int lineLength = h.getFontMetrics().stringWidth(name);
        h.setColor(bkgdLeafColor);
        h.fillRect( node.x,node.y+1,lineLength,YSPACE-2);
        
        drawBorder(h,node, lineLength);
//        h.setColor(Color.black);
//        h.drawLine(node.x-1,node.y,node.x-1+lineLength,node.y);
//        h.drawLine(node.x-1,node.y,node.x-1,node.y+YSPACE-2);
//        h.setColor(Color.white);
//        h.drawLine(node.x-1,node.y+YSPACE-1,node.x-1+lineLength,node.y+YSPACE-1);
//        h.drawLine(node.x+lineLength,node.y,node.x+lineLength,node.y+YSPACE-1);
    }
    
    protected void drawBorder(Graphics h,BasicNode node, int lineLength) {
    	Color saveColor = h.getColor();
        h.setColor(Color.black);
        h.drawLine(node.x-1,node.y,node.x-1+lineLength,node.y);
        h.drawLine(node.x-1,node.y,node.x-1,node.y+YSPACE-2);
        h.setColor(Color.white);
        h.drawLine(node.x-1,node.y+YSPACE-1,node.x-1+lineLength,node.y+YSPACE-1);
        h.drawLine(node.x+lineLength,node.y,node.x+lineLength,node.y+YSPACE-1);
        h.setColor(saveColor);
    }
    
    protected String getName(BasicNode node) {
        return node.name;
    }

    private void displayIconLabel(Graphics h,BasicNode node,int x) {
    	x+=XSPACE;
        if( !node.isLeaf ) 
			drawLogo(h,x,yCurrent,node);

        if( node.level>0 && !flatView ) 
			drawHorizLine(h,x,yCurrent,node);

        // dessin des checkbox pour les feuilles
        if( node.isLeaf ) {
            drawCheckBox(h,x,yCurrent,node,true);
        }

    	x+=XSPACE;

        node.x = x;
        node.y = yCurrent;
        drawNodeName(h,node);
    }
    
    /** teste si la position x se trouve au dessus du label de node */
    private boolean inNodeName(Graphics h,int x, BasicNode node) {
        return inNodeName(h,x, node, false);
    }
    
    /**
     * Teste si la position x se trouve au dessus du label de node
     * @param x position en x dans le repère de l'arbre
     * @param node noeud sur lequel on teste
     * @param considerBox true si on considère que la boite +/- fait partie du label
     * @return boolean true si x est compris dans les limites du label
     */
    private boolean inNodeName(Graphics h,int x, BasicNode node, boolean considerBox) {
        int leftLim = considerBox?node.level*XSPACE:node.x;
        return (x>=leftLim && x<=node.x+h.getFontMetrics().stringWidth(getName(node)));
    }
    
    boolean lastPosInLogo;
    public void mouseMoved(MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();
        int ooHilightNode;
        hilightNode = y/YSPACE;
        Graphics h=getGraphics();
    	h.setFont(nameFont);
        BasicNode curNode;
        boolean inNodeOrBox, inLogo;
        
        
        try{
            curNode = nodeTab[hilightNode];
            if( curNode==rootNode ) {
                onMouseMoved(rootNode,false);
                BasicNode old = nodeTab[oHilightNode];
                oHilightNode = -1;
                ancienCurNode = null;
                drawNodeName(h,old,true);
                if( !old.isSelected() ) drawPreCheckBox(h,old.x-XSPACE, old.y, old, true);
//                repaint();
            }
            
            if( ( curNode.hide ) ) return;
            
            inNodeOrBox = inNodeName(h,x,curNode,true);
            inLogo = inLogo(curNode, x);
            
            // on souligne si on se trouve au dessus du nom OU si on se trouve au dessus de la box
            if( !inNodeOrBox) {
                onMouseMoved(curNode,inNodeOrBox);
                BasicNode n = nodeTab[oHilightNode];
                oHilightNode = -1;
                ancienCurNode = null;
            	drawNodeName(h,n,true);
            	if( !n.isSelected() && lastPosInLogo ) drawPreCheckBox(h,n.x-XSPACE, n.y, n, true);
//            	repaint();
            	lastPosInLogo = inLogo;
                return;
            }
            
            
            if( inLogo && ! curNode.isSelected() && (!lastPosInLogo || curNode!=ancienCurNode) ) {
                drawPreCheckBox(h,curNode.x-XSPACE, curNode.y, curNode, false);
//                repaint();
            }
            else if( !inLogo && ! curNode.isSelected() && lastPosInLogo) {
                drawPreCheckBox(h,curNode.x-XSPACE, curNode.y, curNode, true);
//                repaint();
            }
            
            if( ancienCurNode==curNode ) {
                lastPosInLogo = inLogo;
                return;
            }

            if( ancienCurNode!=null && !ancienCurNode.isSelected() && lastPosInLogo ) drawPreCheckBox(h,ancienCurNode.x-XSPACE, ancienCurNode.y, ancienCurNode, true);
            ancienCurNode = curNode;


			ooHilightNode = oHilightNode;
			oHilightNode = hilightNode;
			
			
			// draw du noeud couramment sous la souris
			drawNodeName(h,curNode,true);
			
			lastPosInLogo = inLogo;
			
    	}
    	catch (ArrayIndexOutOfBoundsException e) {return;}
    	
        // appel du callback
		onMouseMoved(curNode,true);

    	if(ooHilightNode!=hilightNode) {

    	    try{
                BasicNode oCurNode = nodeTab[ooHilightNode];
                // draw de l'ancien noeud mis en valeur
                drawNodeName(h,oCurNode,true);
    	    }
    	    
    	    catch (ArrayIndexOutOfBoundsException e) {}
    	}
    	
    	//oHilightNode = hilightNode;
//    	repaint();
    }
    
    private int maxWidth;
    private int tmpSize;
    static FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(boldNameFont);
    
    // parcourt tout l'arbre et remplit nodeFullList et nodeFullTab
    void traverseTree() {
    	hasSpectraOrImages = false;
    	nodeFullList = new Vector();
    	maxWidth=initMaxWidth();
    	recTraverseTree(this.rootNode,0);
    	nodeFullTab = new BasicNode[nodeFullList.size()];
    	nodeFullList.copyInto(nodeFullTab);
    	// màj de la largeur
    	prefWidth = maxWidth+5;
        if( scroll!=null ) {
            JViewport vPort = scroll.getViewport();
            JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
            
            int newWidth = vPort.getWidth()+(verticalScrollBar.isVisible()?0:verticalScrollBar.getWidth());
            
            if( prefWidth<newWidth ) prefWidth = newWidth; 
        }
    	//System.out.println(maxWidth);
    	createPopup();
    }
    
    protected int initMaxWidth() {
        return 0;
    }
    
    // fonction recursive appelee par traverseTree
    private void recTraverseTree(BasicNode node, int width) {
    	nodeFullList.addElement(node);
    	width+=XSPACE;
    	
        if( !hasSpectraOrImages &&  (((ResourceNode)node).type==ResourceNode.SPECTRUM || ((ResourceNode)node).type==ResourceNode.IMAGE)) {
            hasSpectraOrImages = true;
        }

    	
    	// la taille totale varie selon que l'on soit en vue hiérarchique ou plate
    	tmpSize = /*width+*/fm.stringWidth(getName(node));
    	if( !flatView ) 
    		tmpSize += width;
    	else tmpSize += 2*XSPACE;
    	
    	if( tmpSize>maxWidth )
    		maxWidth = tmpSize;
    	
    	Enumeration e = node.getChildren();
    	BasicNode child;
    	while(e.hasMoreElements()) {
    	    child = (BasicNode)e.nextElement();
    	    recTraverseTree(child,width);
    	}
    }
    
    /** recherche dans les noeuds par nom
	 * @param name nom du noeud recherché
	 * @return le premier BasicNode correspondant, null si non trouvé
	 */
    public BasicNode searchNodeByName(String name) {
        
    	for(int i=0;i<nodeFullTab.length;i++) {
            if(nodeFullTab[i].name.equals(name)) return nodeFullTab[i];
    	}
    	
    	return null;
    }

	public void setFlat(boolean b) {
        // on enlève le dernier élt du popup
        this.flatView = b;
        if( ! ((ResourceNode)rootNode).isSIAPEvol ) {
            popupPrefs.remove(popupPrefs.getComponentCount()-1);
            
            // màj de popupPrefs
            popupPrefs.add(flatView?HIER_VIEW:FLAT_VIEW);
        }
        else {
            popupPrefs.remove(popupPrefs.getComponentCount()-1);
            popupPrefs.remove(popupPrefs.getComponentCount()-1);
            popupPrefs.remove(popupPrefs.getComponentCount()-1);
            
            // màj de popupPrefs
            popupPrefs.add(flatView?createItem(HIER_VIEW):createItem(FLAT_VIEW));
            popupPrefs.addSeparator();
            popupPrefs.add(SIAP_EVOL_SORT);
        }
        
	    traverseTree();
//	    init();
	    repaint();
        if( scroll!=null ) scroll.validate();
	}

    public void actionPerformed(ActionEvent ae) {
        String o = ae.getActionCommand();
        
        if( o.equals(DELETE) ) {
            removeNode(selectedNode);
            // maj de l'affichage
            //doDisplay(); // TEST (car ligne non nécessaire apparemment)
//            if( scroll!=null ) scroll.doLayout();
            repaint();
        }
        // fermeture de tous les noeuds du sous-arbre
        else if( o.equals(COLLAPSE_SUBTREE) ) {
			setAllNodes(selectedNode,false);
//			doDisplay();
			repaint();
        }
        
		// ouverture de tous les noeuds du sous-arbre
		else if( o.equals(EXPAND_SUBTREE) ) {
			setAllNodes(selectedNode,true);
//			doDisplay();
			repaint();
		}
        
        // fermeture de tous les noeuds
        else if( o.equals(COLLAPSE_ALL) ) {
            setAllNodes(rootNode,false);
//            doDisplay();
            repaint();
        }
        // ouverture de tous les noeuds
        else if( o.equals(EXPAND_ALL) ) {
            setAllNodes(rootNode,true);
//            doDisplay();
            repaint();
        }
        else if( o.equals(FLAT_VIEW) || o.equals(HIER_VIEW) ) {
            setFlat(!flatView);
        }
        
    }

    /** Enlève node de l'arbre
     *  @param node le noeud a supprimer
     */
    private void removeNode(BasicNode node) {
        if( node==null ) return;

        //System.out.println("noeud a virer : "+node.name);
        //System.out.println("pere du noeud a virer : "+node.father.name);
        // on supprime node de l'arbre
        ///*
        boolean removed = node.getParent().removeChild(node);
        // si lastInfoNode pointe sur le noeud qu'on vient de supprimer
		onNodeRemoved(node);
        // maj de nodeFullTab et nodeTab
        if( removed ) {
            traverseTree();
        }
		
        // maj de la taille du canvas et redisplay
//        init();
        repaint();
    }

	protected boolean isEmpty() {
	    return (rootNode.nbChildren==0);
	}

    public Vector getSelectedLeaves() {
        Vector v = new Vector();
        recGetSelectedLeaves(rootNode, v);
        return v;
    }

    private void recGetSelectedLeaves(BasicNode start, Vector result) {
        if(start.isSelected()) result.addElement(start);
        // on ne considere que les noeuds ouverts, donc les feuilles visibles
        if( start.isOpen || flatView ) {
    	    Enumeration e = start.getChildren();
    	    while(e.hasMoreElements()) {
    	        recGetSelectedLeaves((BasicNode)e.nextElement(), result);
    	    }
        }
    }
    
    // ouvre tous les noeuds sous le noeud node
    public void openAllNodes(BasicNode node) {
        setAllNodes(node,true);
    }
    
    // met tous les sous-noeuds de node à l'état open
    public void setAllNodes(BasicNode node, boolean open) {
        recSetAllNodes(node,open);
    }

    private void recSetAllNodes(BasicNode node, boolean open) {
        // on ne change pas l'état du noeud root
        if( node!=rootNode) {
        	node.isOpen = open;
        }
        Enumeration e = node.getChildren();
    	while(e.hasMoreElements()) {
    		recSetAllNodes((BasicNode)e.nextElement(),open);
    	}
    }

	static protected void getAllLeaves(BasicNode start, Vector result) {
        Enumeration e = start.getChildren();
        BasicNode n;
        while( e.hasMoreElements() ) {
        	n = (BasicNode)e.nextElement();
            if( n.isLeaf ) {
                result.addElement(n);
            } 
            getAllLeaves(n, result);
        }
	}
    
    // pour SIAP evol
    static protected void getAllObs(BasicNode start, Vector result) {
        Enumeration e = start.getChildren();
        BasicNode n;
        while( e.hasMoreElements() ) {
            n = (BasicNode)e.nextElement();
            if( n.isObs ) {
                result.addElement(n);
            } 
            getAllObs(n, result);
        }
    }
	
    static protected void getAllNonLeaves(BasicNode start, Vector result) {
        Enumeration e = start.getChildren();
        BasicNode n;
        while( e.hasMoreElements() ) {
            n = (BasicNode)e.nextElement();
            if( !n.isLeaf ) {
                result.addElement(n);
            } 
            getAllNonLeaves(n, result);
        }
    }

	protected void getAllSubnodes(BasicNode start, Vector result) {
		Enumeration e = start.getChildren();
		BasicNode n;
		while( e.hasMoreElements() ) {
			n = (BasicNode)e.nextElement();
			result.addElement(n);
			getAllSubnodes(n, result);
		}
	}

    // deselectionne toutes les feuilles
    protected void resetCb() {
        for( int i=0; i<nodeFullTab.length; i++ ) {
            if( !nodeFullTab[i].isLeaf ) continue;
            nodeFullTab[i].isSelected = false;
        }
        oHilightNode=-1;
//        doDisplay();
        repaint();
    }

	boolean triggerOnNodeSelected=false;
	BasicNode nodeToTrigger=null;
	// les "callbacks" liés aux popupMenu sont appelés sur l'evt mouseUp
	// afin d'éviter de mauvaises surprises liées au WM
    public void mouseReleased(MouseEvent evt) {
        try {
         int x = evt.getX();
           int y = evt.getY();
           
           Graphics h = getGraphics();
           
         // cet événement est déclenché sur mouseUp pour que la fenêtre s'affiche en avant plan
         if( triggerOnNodeSelected ) {
         	onNodeSelected(nodeToTrigger);
         	triggerOnNodeSelected=false;
         }
           BasicNode node;
           try {node = nodeTab[y/YSPACE];}
           catch (ArrayIndexOutOfBoundsException e) {
               if( (evt.getModifiers() & InputEvent.BUTTON3_MASK) !=0 ) onRightClickOutNode(x,y);
               return;
           }
         
           // aucune action pour un noeud cache
           if(node.hide) {
              if( (evt.getModifiers() & InputEvent.BUTTON3_MASK) !=0 ) onRightClickOutNode(x,y);
               return;
           } 

           boolean inLogo = inLogo(node, x);
           boolean inNodeName = inNodeName(h,x,node);
           
           // click droit sur un noeud
           if( !inLogo && (evt.getModifiers() & InputEvent.BUTTON3_MASK) !=0 ) {
               if(inNodeName) {
                   onRightClickInNode(node, x, y);
               }
               else{
                   onRightClickOutNode(x,y);  
               }
           }
      } catch( Exception e ) {
         e.printStackTrace();
      }
	}
    
    public void mousePressed(MouseEvent evt) {
       int x = evt.getX();
       int y = evt.getY();
        oselectedNode = selectedNode;
    	try {selectedNode = nodeTab[y/YSPACE];}
    	catch (ArrayIndexOutOfBoundsException e) {return;}
    	
        // aucune action pour un noeud cache
        if(selectedNode.hide) return;

        if(selectedNode==rootNode && flatView) return;
        
        Graphics h = getGraphics();

        boolean inLogo = inLogo(selectedNode, x);
        boolean inNodeName = inNodeName(h,x,selectedNode);
        
        // click droit sur un noeud
        if( !inLogo && (evt.getModifiers() & InputEvent.BUTTON3_MASK) !=0 )  {
            // traité dans mouseUp dorénavant
            /*
            if(inNodeName) {
                onRightClickInNode(selectedNode, x, y);
            }
            else{
                onRightClickOutNode(x,y);  
            }
            */
            return;
        }

		
		if( !inLogo && inNodeName ) {
		    if( evt.getClickCount()>1 )
		    	onNodeSelectedDbleClick(selectedNode);
		    else {
		    	triggerOnNodeSelected = true;
		    	nodeToTrigger= selectedNode;
		    }
		}
        if( !selectedNode.equals(lastInfoNode) && !inLogo && inNodeName ) {
			BasicNode oInfoNode = lastInfoNode;
            lastInfoNode = selectedNode;

            
            try {
              	// mise en gras du noeud
                drawNodeName(h,selectedNode,true);
                
                // ancien noeud remis en fonte normale
                if( oInfoNode != null ) {
                    // on vérifie que oInfoNode est encore affiché
                    if( !oInfoNode.hide && nodeTab[oInfoNode.y/YSPACE].equals(oInfoNode) ) {
                        drawNodeName(h,oInfoNode,true);
                    }
                }
            }
            catch(ArrayIndexOutOfBoundsException outExc) {}

		}

		// on a clique sur une feuille	
    	if(selectedNode.isLeaf) {
            if( inLogo ) {
                selectedNode.isSelected = !selectedNode.isSelected;
                drawCheckBox(h,selectedNode.x-XSPACE,selectedNode.y,selectedNode,true);
            }
            repaint();
            
    	}
    	// on a clique sur un noeud	
    	else {
            if( inLogo ) {
    	        selectedNode.changeState();
				if( selectedNode.isOpen ) { 
					onNodeExpanded(selectedNode);
				} else {
					onNodeCollapsed(selectedNode);
				}
    	        
//    	        doDisplay();
            }
            repaint();
    	}
    }


    /** Teste si x se situe dans la boite +/- */
    private boolean inLogo(BasicNode node, int x) {
        int min = (node.level)*XSPACE;
        int max = (node.level)*XSPACE+2+LOGOSIZE;
        return x>=min && x<=max;
    }



	/** Retourne une référence sur le noeud racine de l'arbre */
	BasicNode getRootNode() {
		return rootNode;
	}
    
    void setRootNode(BasicNode node) {
		node.isLeaf = false;
        this.rootNode = node;
        createPopup();
    }

    public Dimension getSize() {
        //System.out.println("getSize");
        // bidouille
//        if(mustInit) {
//            if( init() ) 
//                mustInit = false;
//        }
        majSize();
       return new Dimension(prefWidth,prefHeight);  
    }

    public Dimension getPreferredSize() {
        return getSize();
    }
    
    public Dimension getMinimumSize() {
    	return getSize();
    }
   
//    // dispose si besoin est l'ancien contexte graphique
//    private void sanityCheck(Graphics g) {
//    	if( og==null || g==null ) return;
////    	if( og!=g ) System.out.println("on doit disposer og");
////    	else System.out.println("**");
//    }
   
//    public void update(Graphics g){
////    	sanityCheck(g);
////    	og = g;
//        // bidouille
////    	System.out.println(scroll);
//    	
//        if(mustInit) {
//        	if( init() ) 
//				mustInit = false;
//        }
//        //System.out.println("update");
//        mustSetLocation = false;
//        paintComponent(g);
//    }
    
    boolean mustSetLocation = false;
//    private Graphics og; // reference sur l'ancien contexte graphique
    public void paintComponent(Graphics g) {
        // histoire de faire le ménage
        super.paintComponent(g);
        
//    	sanityCheck(g);
//    	og = g;
        // bidouille
//        if(mustInit) {
//        	if( init() ) 
//				mustInit = false;
//        }
//        //System.out.println("paint\n");
//        if( buffer==null) {
//            init();
//            mustInit = false;
//        } 
        
        init(g);

        //System.out.println(scroll.getHAdjustable().getValue()+"  "+scroll.getVAdjustable().getValue());
        // pour empêcher un bug d'affichage de l'arbre en Java 1.4
        if( mustSetLocation ) {
            //System.out.println("mustSET");
//            int tx = scroll.getHAdjustable().getValue();
//            int ty = scroll.getVAdjustable().getValue();
//            setLocation(-tx,-ty); 
        }
        
        mustSetLocation=true;
//        g.drawImage(buffer,0,0,this); // solution originelle
    }

    /**
     * sets if sorting nodes by fields is allowed for this tree
     * @param b
     */
    protected void setAllowSortByFields(boolean b) {
    	this.allowSortByFields = b;
    }
    
    
	// méthodes appelées lors d'une action sur l'arbre
	protected void onNodeExpanded(BasicNode node) {}
	protected void onNodeCollapsed(BasicNode node) {}
	protected void onNodeSelected(BasicNode node) {}
	protected void onNodeSelectedDbleClick(BasicNode node) {}
	protected void onNodeRemoved(BasicNode node) {}
	protected void onMouseMoved(BasicNode node,boolean inNodeName) {}
    protected void onRightClickInNode(BasicNode node, int x, int y) {
        popup.show(this, x, y);
    }
    
    protected void onRightClickOutNode(int x, int y) {
        popupPrefs.show(this, x,y);
    }
    
   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
protected JScrollPane getScroll() {
    return scroll;
}
protected void setScroll(JScrollPane scroll) {
    this.scroll = scroll;
    this.scroll.getVerticalScrollBar().setUnitIncrement(YSPACE);
}

}
