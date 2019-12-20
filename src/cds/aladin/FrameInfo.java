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

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import cds.tools.TwoColorJTable;
import cds.tools.Util;

/** Classe FrameInfo
 * Affiche des informations sur un ResourceNode
 * @author Thomas Boch [CDS]
 * @version 1.1 Nov. 2007 : Passage à Swing, ajout JTable pour visualisation des infos
 * @version 0.9 10 Juin 2003
 */
public class FrameInfo extends JFrame implements WindowListener, MyListener, ActionListener {
    // dimensions du ScrollPane contenant les informations
	static final int SCROLL_MAX_WIDTH = 300;
	static final int SCROLL_MAX_HEIGHT = 240;

    static String FOV_STACK,TITLE,SLICE,SUBIMG,CUTTARGET,SLICE1,
                  SLICE2,GETIN,MODE,TARGET,GRAB,FILTERCHAR,STICK,
                  CLOSE,LOAD,LOADIN,DOWNLOAD,DISPLAY,VALUE,FIELD,
                  DISTTOCENTER;

    static private final Insets BUTTON_INSETS = new Insets(1,1,1,1);

//    private Color BKGD_COLOR = Aladin.BLUE;

    // Taille de l'écran
    static private Dimension screenSize;

    // les differents boutons
    private JButton loadBtn; // chargement d'une ressource
    private JButton loadInBtn; // chargement d'une ressource dans une appli PLASTIC
    private JButton fovBtn;
    private JButton grabBtn;
    private JButton mosaicGrabBtn;


    private boolean grabMode = false;
    private boolean mosaicGrabMode = false;

    private boolean imagePosGrabMode = false;

    // widgets pour grab de la position sur l'image en x,y
    private JTextField xPosTF, yPosTF;
    private JButton imagePosGrabBtn;

    // slider pour choisir une image dans un cube selon sa velocite
	private Slider sliderVel;

	// panel avec tous les boutons
	private JPanel btnPanel;
	// panel avec informations sur ressource courante
	private JPanel infoPanel;
	// panel avec target+radius+grab pour les cutouts
	private JPanel cutoutPanel;
	// panel du choix du format de l'image
	private JPanel formatPanel;
    // panel du choix du mode de processing
    private JPanel modePanel;
    // panel du choix de la subimage (pour les slices des datacubes)
    private JPanel subImgPanel;
    // panel du grab de la position dans l'image (pour les spectres)
    private JPanel imagePosGrabPanel;

    private JPanel sub;

	JTextField target,size,mosaicTarget,subImg;

	// barre de menu
	JMenuBar menuBar;

	// JTable contenant les informations du noeud courant
	private JTable infoTable;
	// colonnes visibles par défaut : la 1ere (le nom) et la 4e (la valeur)
	static private boolean[] visibleCol = new boolean[] {true, false, false, true};
	static private int nbVisibleColumn = 2; // nombre de colonnes visibles

	static private Vector<String> colNames;

	// pour choix du format d'image
	private ButtonGroup fmt;

    // CheckBoxGrp de choix du mode de processing
    private ButtonGroup mode;

	private JPanel bottomPanel;

	private JButton lockBtn,closeBtn;

	// refrence à l'objet Aladin
	private Aladin aladin;



    // référence au noeud pour lequel on affiche les infos
    private ResourceNode node;
    private MetaDataTree tree;

//	private boolean isLocked = false;

	// Appelé par Chaine directement (pas possible par le constructeur)
	static protected void createChaine(Chaine chaine) {
	   if( TITLE!=null ) return;
	   FOV_STACK = chaine.getString("FIFOSTACK");
	   TITLE = chaine.getString("FITITLE");
	   SLICE = chaine.getString("FISLICE");
	   SUBIMG = chaine.getString("FISUBIMG");
	   CUTTARGET = chaine.getString("FICUTTARGET");
	   SLICE1 = chaine.getString("FISLICE1");
	   SLICE2 = chaine.getString("FISLICE2");
	   GETIN = chaine.getString("FIGETIN");
	   MODE = chaine.getString("FIMODE");
	   TARGET = chaine.getString("FITARGET");
	   GRAB = chaine.getString("FIGRAB");
	   FILTERCHAR = chaine.getString("FIFILTERCHAR");
	   STICK = chaine.getString("FISTICK");
	   CLOSE = chaine.getString("CLOSE");
	   LOAD = chaine.getString("FILOAD");
	   LOADIN = chaine.getString("FILOADIN");
	   DOWNLOAD = chaine.getString("FIDOWNLOAD");
	   DISPLAY = chaine.getString("FIDISPLAY");
	   VALUE = chaine.getString("FIVALUE");
	   FIELD = chaine.getString("FIFIELD");
	   DISTTOCENTER = chaine.getString("FIDISTTOCENTER");
	}

	/** Constructeur */
	protected FrameInfo(Aladin aladin) {
	    super();
        Aladin.setIcon(this);
	    setTitle(TITLE);

	    this.aladin = aladin;

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Util.setCloseShortcut(this, false, aladin);

	    createComponents();

//	    setBackground(BKGD_COLOR);
	    getContentPane().setLayout(new BorderLayout(5,5));

	    infoPanel = new JPanel();

        getContentPane().add(infoPanel, "Center");

	    // bottomPanel : contient cutoutPanel, formatPanel, modePanel et btnPanel
	    bottomPanel = new JPanel();
	    GridBagLayout g = new GridBagLayout();
	    GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
	    bottomPanel.setLayout(g);

	    // imagePosGrabPanel
	    imagePosGrabPanel = new JPanel();
	    imagePosGrabPanel.setLayout(new GridLayout(0,1));
	    imagePosGrabPanel.add(createLabel("Warning : position must be grabbed in the correct image !"));
	    JPanel imageSub = new JPanel(new FlowLayout());
	    imageSub.add(createLabel("X pos.:"));
	    imageSub.add(xPosTF = new JTextField(3));
        xPosTF.setFont(Aladin.PLAIN);
	    imageSub.add(createLabel("Y pos.:"));
	    imageSub.add(yPosTF = new JTextField(3));
        yPosTF.setFont(Aladin.PLAIN);
	    imageSub.add(imagePosGrabBtn = createButton("Grab image pos."));
	    imagePosGrabPanel.add(imageSub);

        // subImgPanel
        subImgPanel = new JPanel();
        //subImgPanel.setLayout(new FlowLayout());
        subImgPanel.setLayout(new GridLayout(0,1));

        subImgPanel.add(createLabel(SLICE));
        subImgPanel.add(sliderVel = new Slider());
        sub = new JPanel();
        sub.setLayout(new FlowLayout());
        sub.add(createLabel(SUBIMG));
        subImg = new JTextField(3);
        subImg.setFont(Aladin.PLAIN);
        sub.add(subImg);
        //subImg.addKeyListener(this);
        //subImgPanel.add(subImg);
        subImgPanel.add(sub);
        //subImgPanel.add(new Label());

	    // cutoutPanel
	    cutoutPanel = new JPanel();
	    cutoutPanel.setLayout(new FlowLayout(FlowLayout.CENTER,4,5));
	    cutoutPanel.add(createLabel(CUTTARGET));
	    target = new JTextField(15);
        target.setFont(Aladin.PLAIN);
	    // éditable uniquement via le grab ou click dans la vue
	    target.setEditable(false);
	    cutoutPanel.add(target);
	    //cutoutPanel.add(new Label("Size"));
	    //size = new TextField(5);
	    //cutoutPanel.add(size);
	    cutoutPanel.add(grabBtn);
	    g.setConstraints(cutoutPanel,c);
	    bottomPanel.add(cutoutPanel);

	    g.setConstraints(formatPanel,c);
	    bottomPanel.add(formatPanel);

        g.setConstraints(modePanel,c);
        bottomPanel.add(modePanel);

        g.setConstraints(imagePosGrabPanel,c);
        bottomPanel.add(imagePosGrabPanel);

        g.setConstraints(subImgPanel,c);
        bottomPanel.add(subImgPanel);

	    btnPanel = new JPanel();
	    btnPanel.setLayout(new FlowLayout());
	    g.setConstraints(btnPanel,c);
	    bottomPanel.add(btnPanel);

        getContentPane().add(bottomPanel,"South");

        addWindowListener(this);
	}

	/** Calcule la position à laquelle on placera la fenêtre lors de sa création */
	private Point computeAbsLoc() {
        if( screenSize==null ) screenSize=Aladin.SCREENSIZE;
        return Aladin.computeLocation(this);
/*  PIERRE mars 04
	    Point p = new Point(0,0);
	    Component c = tree;

	    while( (c = c.getParent()) != null ) {
	        Point loc = c.getLocation();
	        p.x += loc.x;
	        p.y += loc.y;
	    }

	    p.x += tree.getParent().getSize().width;

	    // pour éviter que l'InfoFrame n'apparaisse en dehors de l'écran !!
        if( screenSize==null ) screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if( p.x>screenSize.width-50 ) p.x = screenSize.width-50;
        if( p.y>screenSize.height-100 ) p.y = screenSize.height-100;

	    return p;
*/
	}

	/** Initialisation du target par défaut (soit au premier affichage, soit lors d'un
	 * changement de frame général - MODIF PF déc 2010 => juste extrait de update(...) */
	public void initTarget() {
	   if( node==null ) return;
       // maj cutout target
       if( node.isLeaf && (node.cutout || node.type==ResourceNode.CAT) ) {
           // si il n'y a pas encore de target pour le cutout, on prend celui du server
           if( node.getCutoutTarget()==null && node.server!=null ) {
               node.setCutoutTarget(node.server.getTarget(false),false);
           }
           String cTarget = node.getCutoutTarget();
           if( cTarget!=null ) target.setText(aladin.localisation.getFrameCoord(cTarget));
       }
	}

   	private boolean firstUpdate = true;
	/** MAJ de la frame avec les infos de node */
	public void update(ResourceNode node,MetaDataTree tree) {

	    grabMode = false;
	    imagePosGrabMode = false;
	    this.node = node;
	    this.tree = tree;

        memorySlice = node.curImgNumber;



        // à la première update, on positionne la frame
        if( firstUpdate ) {
            firstUpdate = false;
            Point pos = computeAbsLoc();
            setLocation(pos.x,pos.y);
        }

        infoPanel.removeAll();

//        GridBagLayout g = new GridBagLayout();
//        GridBagConstraints c = new GridBagConstraints();
        infoPanel.setLayout(new BorderLayout());

        // maj cutout target  - MODIF PF Déc 2010
        initTarget();
//        if( node.isLeaf && (node.cutout || node.type==ResourceNode.CAT) ) {
//            // si il n'y a pas encore de target pour le cutout, on prend celui du server
//            if( node.getCutoutTarget()==null && node.server!=null ) {
//                node.setCutoutTarget(node.server.getTarget(false),false);
//            }
//            String cTarget = node.getCutoutTarget();
//            if( cTarget!=null ) target.setText(aladin.localisation.getFrameCoord(cTarget));
//        }


        // maj de btnPanel
        btnPanel.removeAll();
        btnPanel.add(lockBtn);
        if( node.type==ResourceNode.IMAGE || (node.type==ResourceNode.SPECTRUM /*&& node.getFov()!=null*/) ) btnPanel.add(fovBtn);
        if( node.isLeaf || node.hasData ) {
        	boolean connectedToPlastic = aladin.getMessagingMgr().isRegistered();
        	ArrayList<String> plasticApps = null;

        	// on récupère les applis compatibles PLASTIC/SAMP
        	if( connectedToPlastic ) {
        	    AppMessagingInterface messagingIf = aladin.getMessagingMgr();
        		plasticApps = messagingIf.getAppsSupporting(node.getPlasticMsg());
        	}

        	if( node.type!=ResourceNode.SPECTRUM || (node.type==ResourceNode.SPECTRUM && node.location!=null) || (node.location!=null && node.location.indexOf("Xpix")>=0 ) ) {
        		btnPanel.add(loadBtn);
        		// ajout du bouton "load in" s'il existe une appli PLASTIC compatible avec le noeud traité
        		if( connectedToPlastic && plasticApps!=null && plasticApps.size()>0 ) {
        			btnPanel.add(loadInBtn);
        			loadInBtn.setEnabled(true);
        			fillPlasticPopupMenu(plasticApps);
        		}
        	}
        	// les spectres ne peuvent être chargés directement dans Aladin
        	else {
        		// on ajoute le bouton "Load in" quoi qu'il arrive
        		btnPanel.add(loadInBtn);
        		loadInBtn.setEnabled(plasticApps!=null && plasticApps.size()>0);
        		fillPlasticPopupMenu(plasticApps);
        	}
        }

        btnPanel.add(closeBtn);

        // MAJ label de btnLoad
        if( (node.type==ResourceNode.SPECTRUM && node.location!=null) || (node.indexing!=null && node.indexing.equals("HTML") ) ) {
        	loadBtn.setText(DOWNLOAD);
        }
		else {
		    loadBtn.setText(LOAD);
		}

        // affichage ou non de cutoutPanel
        if( node.isLeaf && node.cutout ) cutoutPanel.setVisible(true);
        else cutoutPanel.setVisible(false);

        // affichage ou non de imagePosGrabPanel
        // TODO : réfléchir à la condition, SPECTRUM est il indispensable ?
        if( node.dataOrga!=null && node.dataOrga.equals("SPECTRUM") &&
        	node.location!=null && node.location.indexOf("Xpix")>=0 ) {
        	imagePosGrabPanel.setVisible(true);
        	String[] xy = node.getImagePosTarget();
        	xPosTF.setText(xy[0]);
        	yPosTF.setText(xy[1]);
        }
        else imagePosGrabPanel.setVisible(false);

        // affichage ou non de subImgPanel
        if( !node.maxImgNumber.equals("1") && node.dataOrga!=null && node.dataOrga.equals("SLICES") ) {
            subImgPanel.setVisible(true);
            int nbSteps = Integer.parseInt(node.maxImgNumber)-1;
            int curImg = Integer.parseInt(node.curImgNumber)-1;
            //sliderVel.setNode(node);
            sliderVel.setListener(this);
            sliderVel.setPosition(curImg);
            sliderVel.setParams(node.beginVel, nbSteps, node.velStep);
            sliderVel.repaint();
            if( node.velStep==0.0 ) {
                sliderVel.setVisible(false);
                subImgPanel.remove(0);
                subImgPanel.add(createLabel(""), 0);
            }
            else {
                sliderVel.setVisible(true);
                subImgPanel.remove(0);
                subImgPanel.add(createLabel(SLICE), 0);
            }
        }
        else subImgPanel.setVisible(false);
        subImg.setText(this.node.curImgNumber);
        sub.remove(0);
        if( node.velStep==0.0 ) sub.add(createLabel(SLICE1+" "+node.maxImgNumber+"):"),0);
        else sub.add(createLabel(SLICE2+" "+node.maxImgNumber+"):"),0);
        //subImgPanel.add(new Label("(between 1 and "+this.node.maxImgNumber+")"));

        // affichage ou non de formatPanel
        // si il n'y a qu'un format, on ne va pas l'afficher !
        if( node.isLeaf && node.formats!=null && node.formats.length>1 ) {
            formatPanel.removeAll();
            fmt = new ButtonGroup();
            formatPanel.add( createLabel(GETIN));
            boolean selected;
            JRadioButton rb;
            for( int i=0; i<node.formats.length; i++ ) {
                // support de MRCOMP ou non
                if( node.formats[i].equals("MRCOMP") && !Aladin.MRDECOMP )
                	continue;

                if( node.formats[i].equals(node.curFormat) )
                	selected = true;
                else
                	selected = false;

                formatPanel.add(rb = new JRadioButton(node.formats[i],selected));
                rb.setFont(Aladin.PLAIN);
                rb.addActionListener(this);
                fmt.add(rb);
            }
            formatPanel.setVisible(true);
        }
        else formatPanel.setVisible(false);

        // FAIRE UN TRUC GENERIQUE POUR LES PANELS DE CHOIX

        // affichage ou non de modePanel
        // si il n'y a qu'un mode, on ne va pas l'afficher !
        if( node.isLeaf && node.modes!=null && node.modes.length>1 ) {
            modePanel.removeAll();
            mode = new ButtonGroup();
            modePanel.add( createLabel(MODE));
            boolean selected;
            JRadioButton rb;
            for( int i=0; i<node.modes.length; i++ ) {
                selected = node.modes[i].equals(node.curMode);

                modePanel.add(rb = new JRadioButton(node.modes[i],selected));
                rb.setFont(Aladin.PLAIN);
                rb.addActionListener(this);
                mode.add(rb);
            }
            modePanel.add(createLabel(TARGET));
            mosaicTarget = new JTextField(15);
            mosaicTarget.setFont(Aladin.PLAIN);
            // éditable uniquement via le grab ou click dans la vue
            mosaicTarget.setEditable(false);

            // maj mosaic target
            // si il n'y a pas encore de target pour la mosaic, on prend celui du server
            if( node.getMosaicTarget()==null && node.server!=null ) {
                    node.setMosaicTarget(node.server.getTarget(false),false);
            }
            String mTarget = node.getMosaicTarget();
            if( mTarget!=null ) mosaicTarget.setText(aladin.localisation.getFrameCoord(mTarget));


            modePanel.add(mosaicTarget);
            mosaicGrabBtn = createButton(GRAB);
            mosaicGrabBtn.setFont(Aladin.SBOLD);
            modePanel.add(mosaicGrabBtn);
            modePanel.setVisible(true);
        }
        else modePanel.setVisible(false);

        ArrayList<Object> names = new ArrayList<Object>();
        ArrayList<Object> values = new ArrayList<Object>();
        ArrayList<String> ucds = new ArrayList<String>();
        ArrayList<String> utypes = new ArrayList<String>();

//        c.fill = GridBagConstraints.BOTH;
        if( node.type==ResourceNode.IMAGE || node.type==ResourceNode.SPECTRUM  || node.type==ResourceNode.CUBE ||
        		node.type==ResourceNode.CHARAC || node.type==ResourceNode.CAT ) {

            // add distance to center information
            if( (node.type==ResourceNode.IMAGE || node.type==ResourceNode.SPECTRUM)
                    && ! Double.isNaN(node.getDistanceToCenter()) ) {

                names.add(DISTTOCENTER);
                values.add(formatDistance(node.getDistanceToCenter()));
                ucds.add("");
                utypes.add("");

                addEmptyRow(names, values, ucds, utypes);
            }

            if( node.criteria!=null && node.valueCriteria!=null ) {
            	names.add(node.criteria);
            	values.add(node.valueCriteria);
            	ucds.add("");
            	utypes.add("");
            }

            // affichage des infos dispos
            // pour toutes les descriptions
            displayInfo(node, names, ucds, utypes, values);

            // affichage de la description d'une sous-obs.
            if( node.desc!=null && node.desc.length()>0 ) {
            	names.add(MetaDataTree.replace(node.desc,"\\n","\n",-1));
            	values.add("");
            	ucds.add("");
            	utypes.add("");
            }

            // affichage des infos sur le filtre
            if( node.filterDesc!=null && node.filterExpla!=null ) {
            	names.add("");
            	values.add("");
            	ucds.add("");
            	utypes.add("");

            	names.add(FILTERCHAR);
            	values.add("");
            	ucds.add("");
            	utypes.add("");

                for( int i=0; i<node.filterDesc.length; i++ ) {
                	// on n'affiche ni filterDesc ni filterExpla si filterExpla est vide
                    if( node.filterExpla[i].length()==0 ) continue;

                    names.add(node.filterDesc[i]);
                    values.add(node.filterExpla[i]);
                    ucds.add("");
                	utypes.add("");
                }
            }
        } /* end of if BasicNode.IMAGE || SPECTRUM */



        // creation de la JTable avec l'ensemble des informations
        final FrameInfoTableModel myTableModel = new FrameInfoTableModel();

		myTableModel.setData( names.toArray(new Object[names.size()]),
								ucds.toArray(new Object[ucds.size()]),
								utypes.toArray(new Object[utypes.size()]),
								values.toArray(new Object[values.size()]) );

        infoTable = new TwoColorJTable() {
            @Override
            public boolean isCellEditable(int rowIndex, int vColIndex) {
                return false;
            }

        };
        infoTable.setGridColor(Color.lightGray);
        infoTable.setShowHorizontalLines(false);
        infoTable.setModel(myTableModel);
        infoTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(infoTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        infoPanel.add(scroll, BorderLayout.CENTER);

        int newScrollHeight, newScrollWidth;
        // on veut que la table ne soit pas plus haute que SCROLL_MAX_HEIGHT
        int allRowHeight = infoTable.getRowHeight()*infoTable.getRowCount();
        // TODO : prendre max entre SCROLL_MAX et hauteur existante
        if( allRowHeight>SCROLL_MAX_HEIGHT ) newScrollHeight = SCROLL_MAX_HEIGHT;
        else newScrollHeight = allRowHeight;

        infoTable.setPreferredScrollableViewportSize(
                new Dimension(SCROLL_MAX_WIDTH, newScrollHeight));

        // mise en italique de la première colonne
        infoTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                Component c = super.getTableCellRendererComponent(table, value,isSelected,hasFocus,row, column);

                if( row==0 && column==0 && value.equals(DISTTOCENTER) ) {
                    c.setFont(Aladin.BOLD);
                }
                else if( column==0 ) c.setFont(Aladin.ITALIC);

                return c;
            }
        });

        // la bordure qui va bien
        infoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(20,5,5,5), BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, node.name, TitledBorder.CENTER, TitledBorder.TOP),
                BorderFactory.createEmptyBorder(5,5,5,5))));

        // pour que le panel d'info se mette a jour
        infoPanel.invalidate();
        infoPanel.validate();
        infoPanel.setSize(0,0);

	    pack();
        // pour éviter que certains boutons ne soient cachés car en dehors de l'écran
        Point loc = getLocation();
        Dimension size = getSize();
        int offsetX, offsetY;
        int newX = loc.x;
        int newY = loc.y;
        boolean mustMove= false;
        offsetX = screenSize.width-(loc.x+size.width);
        offsetY = screenSize.height-(loc.y+size.height);
        if( offsetX<0 ) {
            mustMove = true;
            newX += offsetX;
        }
        // on prend la valeur de 25 pour prendre en compte la barre des taches sous Windows
        if( offsetY<25 ) {
            mustMove = true;
            if( offsetY<0 ) newY += offsetY-25;
            else newY += -(25-offsetY);
        }
        if( mustMove ) setLocation(newX, newY);
	}

	private void addEmptyRow(ArrayList<Object> names,ArrayList<Object> values,
	                         ArrayList<String> ucds,ArrayList<String> utypes) {
	    names.add("");
	    values.add("");
	    ucds.add("");
	    utypes.add("");
	}

    // affiche dans infoPanel toutes les infos (tableaux description et explanation) relatives au noeud node
    private void displayInfo(ResourceNode node, ArrayList<Object> names, ArrayList<String> ucds,
                             ArrayList<String> utypes, ArrayList<Object> values) {
        if( node.description == null || node.explanation == null ) return;
        // affichage des infos dispos
        // pour toutes les descriptions
        for( int i=0; i<node.description.length; i++ ) {
            // on n'affiche ni desc ni expla si expla est vide
            if( node.explanation[i].length()==0 ) continue;

            Object o1, o2;

            o1 = node.description[i];
            if( node.links!=null && node.links.get(node.description[i])!=null ) {
                MyLink l =  new MyLink(aladin, node.explanation[i]);

                l.setFont(Aladin.SBOLD);
                l.baratin = node.description[i];

                o2 = l;
            }
            else {
                o2 = node.explanation[i];
            }

            names.add(o1);
            values.add(o2);
            ucds.add(node.ucds==null?"":node.ucds[i]);
            utypes.add(node.utypes==null?"":node.utypes[i]);
        }

    }

    /** Pour vérifier les valeurs entrées pour une subImage
     * et setter node.curImgNumber
     * @see java.awt.Component#keyDown(Event, int)
     * @deprecated
     */
    @Deprecated
    private String memorySlice;

    @Override
    public boolean keyUp(Event e,int key) {
       if( e.target.equals(subImg) ) {

           // on n'autorise que la saisie de chiffres
           if( key>=0x30 && key<=0x39 ) {
           }
           else if( key>=0x7F || key<=31 ) {
           }
           // saisie d'autre chose que chiffres ou DEL ou BACKSPACE ...
           else {
               int caret = subImg.getCaretPosition();
               subImg.setText(memorySlice);
               subImg.setCaretPosition(caret==0?0:caret-1);
           }
       }
       memorySlice = this.node.curImgNumber = subImg.getText();
       try {
           if( sliderVel!=null ) {
               sliderVel.setPosition(Integer.parseInt(memorySlice)-1, false);
               sliderVel.repaint();
           }
       }
       catch(NumberFormatException nfe) {}
       return super.keyUp(e,key);
    }


    // cree les differents components necessaires
     private void createComponents() {
    	 // creation de la barre de menu
    	 menuBar = new JMenuBar();
    	 JMenu menu = new JMenu(DISPLAY);
    	 menuBar.add(menu);
    	 JMenuItem mi;
    	 colNames = new Vector<String>(Arrays.asList(new String[] {FIELD, "UCD", "UType", VALUE}));
    	 Enumeration<String> e = colNames.elements();
    	 String s;
    	 int k=0;
    	 while( e.hasMoreElements() ) {
    		 s = e.nextElement();
    		 menu.add( mi = new JCheckBoxMenuItem(s, visibleCol[k]) );
    		 mi.addActionListener(this);
    		 k++;
    	 }

    	 this.setJMenuBar(menuBar);

         // Les panels
         //Le choix du format de l'image
         formatPanel = new JPanel();
         formatPanel.setLayout( new FlowLayout(FlowLayout.LEFT));
         formatPanel.setFont(Aladin.PLAIN);

         // Le choix du mode de processing
         modePanel = new JPanel();
         modePanel.setLayout( new FlowLayout(FlowLayout.LEFT));
         modePanel.setFont(Aladin.PLAIN);


         grabBtn = createButton(GRAB);
         grabBtn.setFont(Aladin.SBOLD);

         fovBtn = createButton(FOV_STACK);
         fovBtn.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent mouseEvent) {}
             public void mouseEntered(MouseEvent mouseEvent) {
                 if( /*node.isLeaf && */node.type==ResourceNode.IMAGE || node.type==ResourceNode.SPECTRUM ) {
                     tree.showFov(node);
                     if(node.cutout) {
                         tree.showCutoutFov(node);
                     }
                 }
             }
             public void mouseExited(MouseEvent mouseEvent) {
                 tree.hideFov();
                 tree.deactivateCutoutFov();
             }
             public void mousePressed(MouseEvent mouseEvent) {}
             public void mouseReleased(MouseEvent mouseEvent)  {}
         });
//         fovBtn.setBackground(BKGD_COLOR);
         fovBtn.setFont(Aladin.PLAIN);

         // Les boutons //
         lockBtn = createButton(STICK);
//         lockBtn.setBackground(BKGD_COLOR);
         lockBtn.setFont(Aladin.PLAIN);

         closeBtn = createButton(CLOSE);
//         closeBtn.setBackground(BKGD_COLOR);
         closeBtn.setFont(Aladin.PLAIN);


         // bouton pour charger l'image/le catalogue directement
         loadBtn = createButton(LOAD);
//         loadBtn.setBackground(BKGD_COLOR);
         loadBtn.setFont(Aladin.BOLD);

         // bouton pour charger la ressource dans une appli PLASTIC
         loadInBtn = createButton(LOADIN);
         loadInBtn.setIcon(new ImageIcon(aladin.getImagette("arrow.gif")));
//         loadInBtn.setBackground(BKGD_COLOR);
         loadInBtn.setFont(Aladin.BOLD);
         loadInBtn.setHorizontalTextPosition(SwingConstants.LEFT);
         // quand on clique dessus --> affichage du popup PLASTIC
         loadInBtn.addMouseListener(new MouseAdapter() {
             @Override
            public void mouseReleased(MouseEvent mouseEvent)  {
                 plasticPopup.show(loadInBtn,mouseEvent.getX(),mouseEvent.getY()+5);
             }
         });
     }

	boolean inGrabMode() {
	    return grabMode;
	}

    boolean inMosaicGrabMode() {
        return mosaicGrabMode;
    }

    boolean inImagePosGrabMode() {
    	return imagePosGrabMode;
    }

	/** renvoie le noeud pour lequel on affiche les infos */
	ResourceNode getNode() {
	    return this.node;
	}

	/** fixe le target du cutout (ou du catalogue) de la ressource courante
	 * @param t le target
	 */
	void setTarget(String t) {
	    // remarque : la maj du TextField target se fait par effet de bord dans ResourceNode
	    node.setCutoutTarget(t);
	    toFront();
	    grabMode = false;
	}

	// met à jour le TextField target
	// Modif PF 20 jan 09 - pour pouvoir supporter des coordonnées non J2000
	void setTargetTF(String t) {
	    target.setText(aladin.localisation.getFrameCoord(t));
	}




    /** fixe le target d'une mosaic de la ressource courante
     * @param t le target
     */
    void setMosaicTarget(String t) {
        // remarque : la maj du TextField mosaicTarget se fait par effet de bord dans ResourceNode
        node.setMosaicTarget(t);
        toFront();
        mosaicGrabMode = false;
    }

    // met à jour le TextField target
    void setMosaicTargetTF(String t) {
        mosaicTarget.setText(aladin.localisation.getFrameCoord(t));
    }

	/** fixe le target du cutout (ou du catalogue) de la ressource courante
	 * @param t le target
	 */
	void setImagePosTarget(String x, String y) {
	    node.setImagePosTarget(x,y);
	    toFront();
	    imagePosGrabMode = false;
	}

	// met à jour le TextField target
	void setImagePosTargeTFt(String x, String y) {
		xPosTF.setText(x);
		yPosTF.setText(y);
	}

    private JPopupMenu plasticPopup;
	/**
	 * remplit le popup plasticPopup avec la liste des applis PLASTIC passés en paramètre
	 * @param plasticApps
	 *
	 */
	private void fillPlasticPopupMenu(ArrayList<String> plasticApps) {
		if( plasticApps==null ) return;

		plasticPopup = new JPopupMenu();
		JMenuItem mi;
		for(String app: plasticApps) {
			mi = new JMenuItem(app);
            mi.addActionListener(this);
			mi.setActionCommand(LOADIN);
            plasticPopup.add(mi);
		}
	}

    /** Gestion des evenements liés aux différents boutons */
    public void actionPerformed(ActionEvent ae) {
        Object src = ae.getSource();
        String o = ae.getActionCommand();

        // modif des colonnes à visualiser
        if( src instanceof JCheckBoxMenuItem ) {
        	toggleColumnVisibility(((JCheckBoxMenuItem)src).getText());
        }
        // grab du cutout target
        else if( src.equals(grabBtn) ) {
            grabMode = true;
            aladin.getFrame(aladin).toFront();
        }
        // grab du cutout target
        else if( src.equals(mosaicGrabBtn) ) {
            mosaicGrabMode = true;
            aladin.getFrame(aladin).toFront();
        }
        // grab de la position x,y de l'image ocurante
        else if( src.equals(imagePosGrabBtn) ) {
            imagePosGrabMode = true;
            aladin.getFrame(aladin).toFront();
        }
        // fermeture de la fenêtre
        else if( src.equals(closeBtn) ) {
            setVisible(false);
        }
        // "lock" de la fenêtre
        else if( src.equals(lockBtn) ) {
//            isLocked = true;
            lockBtn.setEnabled(false);
            // création d'une nouvelle FrameInfo
            aladin.frameInfo = new FrameInfo(aladin);
        }
        // Chargement de la ressource
        else if( src.equals(loadBtn) ) {
            if (loadBtn.getText().equals(DOWNLOAD) && node.type==ResourceNode.SPECTRUM) {
                aladin.glu.showDocument("Http",node.location,true);
                return;
            }

            node.setCutoutTarget(aladin.localisation.getICRSCoord(target.getText()),false);
            if( imagePosGrabPanel.isVisible() ) {
                node.setImagePosTarget(xPosTF.getText(), yPosTF.getText());
            }
            // màj du numéro de slice à charger
            if( !node.maxImgNumber.equals("1") ) {
                memorySlice = this.node.curImgNumber = subImg.getText();
            }
            tree.load(node, null, null, this);
        }
        // export du fov dans le stack
        else if( src.equals(fovBtn) ) {
            String planeName = node.name;
            if( node.server!=null && node.server instanceof ServerAladin ) {
                if( node.machine!=null && node.machine.length()!=0 )
                    planeName = node.machine+"."+planeName;
            }

            aladin.calque.newPlanFov("FoV_"+planeName,tree.getFovs(node,false,true));
            aladin.view.repaintAll();
            // pour activer le bouton grab
            if( aladin.dialog.server[aladin.dialog.current].grab!=null )
                aladin.dialog.server[aladin.dialog.current].grab.setEnabled(true);
            aladin.grabUtilInstance.setAllGrabItsEnabled(true);
        }
      // les JRadioButton
      else if( src instanceof JRadioButton ) {
          JRadioButton rb = (JRadioButton)src;
          // changement du format
          if( fmt!=null && rb.equals(getSelectedJRadioButton(fmt))  ) {
              node.curFormat = rb.getText();
          }
          // changement de mode
          else if( mode!=null && rb.equals(getSelectedJRadioButton(mode)) ) {
              node.curMode = rb.getText();
          }
      }
      // chargement du noeud courant dans une appli PLASTIC
      else if( src instanceof JMenuItem && o.equals(LOADIN) ) {
//        on pourrait passer par n'importe quel MetaDataTree
        aladin.dialog.server[ServerDialog.ALADIN].tree.loadNodeWithPlastic(node, ((JMenuItem)src).getText());
      }
    }

    private JRadioButton getSelectedJRadioButton(ButtonGroup bg) {
        Enumeration<AbstractButton> e = bg.getElements();
        JRadioButton rb;

        while( e.hasMoreElements()) {
            try {
                rb = (JRadioButton)e.nextElement();
            }
            catch(ClassCastException cce) {continue;}
            if( rb.isSelected() ) return rb;
        }
        return null;
    }


    private JButton createButton(String s) {
        JButton b = new JButton(s);
        b.setMargin(BUTTON_INSETS);
        b.addActionListener(this);

        return b;
    }

    /**
     * Formate la distance pour affichage dans l'info frame
     * @param dist
     * @return
     */
    private String formatDistance(double dist) {
        double value = dist;
        String unit = "deg";

        if( value<1.0 ) {
            value = dist*60.0;
            unit = "arcmin";
        }
        if( value<1.0 ) {
            value = dist*3600.0;
            unit = "arcsec";
        }

        return Util.myRound(value+"", 2)+" "+unit;
    }

    private JLabel createLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Aladin.PLAIN);

        return l;
    }

    private void toggleColumnVisibility(String colName) {
    	int idx = colNames.indexOf(colName);
    	if( idx<0 ) return;

    	boolean newVal = !visibleCol[idx];
    	visibleCol[idx] = newVal;
    	if( newVal ) nbVisibleColumn++;
    	else nbVisibleColumn--;

    	((AbstractTableModel)infoTable.getModel()).fireTableStructureChanged();
    }

    /** Chargement de la resource
     * Modif Pierre F. le 25/11/03 pour pouvoir appeler cette methode
     * Modif PF 20 jan 09 pour pouvoir lire des coordonnées non J2000
     * depuis l'exterieur
     */
    protected void load() {
       node.setCutoutTarget(aladin.localisation.getICRSCoord(target.getText()),false);
       tree.load(node, null, null, this);
    }


    /** Méthodes implémentant MyListener */
    public void fireStateChange(String s) {
        if( node!=null ) node.curImgNumber = s;
        if( subImg!=null ) subImg.setText(s);
    }

    public void fireStateChange(int i) {}

	/** Méthodes implémentant WindowListener */

    /** Windows closing
     *
     * @param e window event
     */
    public void windowClosing(WindowEvent e){
    	setVisible(false);
    }

    /** Window Closed
     *
     * @param e WindowEvent
     */
    public void windowClosed(WindowEvent e){
    	setVisible(false);
    }



    // Méthodes ne servant pas
    public void windowOpened(WindowEvent e){}
    public void windowDeactivated(WindowEvent e){}
    public void windowActivated(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowIconified(WindowEvent e){}

    class FrameInfoTableModel extends AbstractTableModel {
		Object[] names, values, ucds, utypes;

		public void setData(Object[] names, Object[] ucds, Object[] utypes,
				Object[] values) {
			this.names = names;
			this.ucds = ucds;
			this.utypes = utypes;
			this.values = values;
		}

		public int getRowCount() {
			return names == null ? -1 : names.length;
		}

		public int getColumnCount() {
			return nbVisibleColumn;
		}

		public Object getValueAt(int row, int col) {
			if (col == -1 || row == -1)
				return null;

			col = getRealColIdx(col);

			switch (col) {
			case 0:
				return names[row];
			case 1:
				return ucds[row];
			case 2:
				return utypes[row];
			case 3:
				return values[row];

			default:
				return null;
			}

		}

		private int getRealColIdx(int col) {
			int k = 0;
			for (int i = 0; i < visibleCol.length; i++) {
				if (!visibleCol[i]) {
					continue;
				}
				if (k == col) {
					return i;
				}

				k++;
			}

			return -1;
		}

		@Override
        public String getColumnName(int column) {
			int k = 0;
			for (int i = 0; i < visibleCol.length; i++) {
				if (!visibleCol[i]) {
					continue;
				}
				if (k == column) {
					return colNames.get(i);
				}
				k++;
			}
			return null;
		}

	}

}

