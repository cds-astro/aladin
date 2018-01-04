// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import cds.aladin.bookmark.FrameBookmarks;
import cds.aladin.prop.PropPanel;
import cds.allsky.Constante;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.mocmulti.BinaryDump;
import cds.mocmulti.MocItem;
import cds.mocmulti.MocItem2;
import cds.mocmulti.MultiMoc;
import cds.mocmulti.MultiMoc2;
import cds.tools.MultiPartPostOutputStream;
import cds.tools.Util;

/**
 * Classe qui gère l'arbre du Directory des collections
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public class Directory extends JPanel implements Iterable<MocItem>, GrabItFrame {
   
   static protected final String MMOC = "Multiprop.bin";

   // Nombre de collections appelables individuellement en parallèle
   static private final int MAX_PARALLEL_QUERY = 30;

   static private String DIRECTORY, MULTICOL, HELP;
   static protected String AWCSLAB, AWCSTIP, AWMCSTIP, AWSIATIP, AWSSATIP, AWMOCQLAB, AWMOCQLABTIP, AWMOCTITLE, AWMOC1, AWMOC1TIP,
         AWMOC2, AWMOC2TIP, AWMOC3, AWMOC3TIP ;
   static protected String ALLCOLL, MYLIST, ALLCOLLHTML, MYLISTHTML, AWSKYCOV, AWMOCUNK, AWHIPSRES, AWNBROWS, AWREFPUB, AWPROGACC,
         AWPROGACCTIP, AWDATAACC, AWDATAACCTIP, AWDATAACCTIP1, AWINVIEWTIP, AWMOCQLABTIP2, AWXMATCH, AWXMATCHTIP, AWCRIT,
         AWCRITTIP, AWMOCX, AWMOCXTIP, AWDM, AWDMTIP, AWPROGEN, AWPROGENTIP, AWSCANONLY, AWSCANONLYTIP, AWLOAD, FPCLOSE,
         AWFRAMEINFOTITLE,AWCGRAPTIP,AWCONE,AWCONETIP,AWACCMODE,AWACCMODETIP,AwDERPROD,AwDERPRODTIP,AWINFOTIP,AWPROPTIP,
         AWBOOKMARKTIP,AWPARAMTIP,AWSTICKTIP,AWCUSTOM,AWCUSTOMTIP;
   static private final String UPDATING = "  updating...";
   static protected final String ROOT_LABEL = "Collections";

   private Aladin aladin; // Référence

   protected MultiMoc2 multiProp; // Le multimoc de stockage des properties des collections
   private Color cbg;             // La couleur du fond

   private DirectoryFilter directoryFilter = null; // Formulaire de filtrage de l'arbre des collections
   private DirectorySort directorySort = null;     // Gestion du tri de l'arbre des collections
   protected boolean mocServerLoading = false;     // true si on est en train de charger le directory initial
   protected boolean mocServerUpdating = false;    // true si on est en train de mettre à jour le directory
   private boolean flagScanLocal = false;          // true si on a des MOCs dans le multiprop local
   protected boolean flagError = false;            // true si l'expression de filtrage est buggée
   private DirectoryTree dirTree;                  // Le JTree du directory
   protected ArrayList<TreeObjDir> dirList;        // La liste des collections connues

   // Composantes de l'interface
   private QuickFilterField quickFilter;           // Champ de filtrage rapide
   protected FilterCombo comboFilter;              // Menu popup des filtres
   protected IconFilter iconFilter;                // L'icone d'activation du filtrage
   protected IconInside iconInside;                // L'icone d'activation du mode "inside"
   protected IconScan iconScan;                    // L'icone d'activation du scan
   private IconCollapse iconCollapse;              // L'icone pour développer/réduire l'arbre
   private IconSort iconSort;                      // L'icone pour trier l'arbre
   private Timer timer = null;                     // Timer pour le réaffichage lors du chargement
   private JLabel dir = null;                      // Le titre qui apparait au-dessus de l'arbre
   private FrameProp frameProp = null;             // Frame des paramètres pour les différents types d'interrogation

   // Paramètres d'appel initial du MocServer (construction de l'arbre)
   private static String MOCSERVER_INIT = "*&fields=!hipsgen*&get=record&fmt=asciic";

   // Paramètres de maj par le MocServer (update de l'arbre)
   private static String MOCSERVER_PARAM = "fmt=asciic";

   private JScrollPane scrollTree = null;

   private String S(String k) {
      return aladin.chaine.getString(k);
   }

   private void loadString() {
      DIRECTORY = S("DTLABEL");
      MULTICOL = S("AWMULTICOL");
      HELP = S("Datatree.HELP");
      MYLIST = "";
      MYLISTHTML = "-- " + S("DTWORKLIST") + " --";
      ALLCOLLHTML = "-- " + S("DTALLCOLL") + " --";
      ALLCOLL = ALLCOLLHTML;
      AWFRAMEINFOTITLE = S("AWFRAMEINFOTITLE");
      AWCUSTOM = S("AWCUSTOM");
      AWCUSTOMTIP = S("AWCUSTOMTIP");
      AWCSLAB = S("AWCSLAB");
      AWMCSTIP = S("AWMCSTIP");
      AWCGRAPTIP = S("AWCGRAPTIP");
      AWCONE = S("AWCONE");
      AWCONETIP = S("AWCONETIP");
      AWACCMODE = S("AWACCMODE");
      AWACCMODETIP = S("AWACCMODETIP");
      AwDERPROD = S("AwDERPROD");
      AwDERPRODTIP = S("AwDERPRODTIP");
      AWINFOTIP = S("AWINFOTIP");
      AWPROPTIP = S("AWPROPTIP");
      AWBOOKMARKTIP = S("AWBOOKMARKTIP");
      AWPARAMTIP = S("AWPARAMTIP");
      AWSTICKTIP = S("AWSTICKTIP");
      AWCONETIP = S("AWCONETIP");
      AWSIATIP = S("AWSIATIP");
      AWSSATIP = S("AWSSATIP");
      AWMOCQLAB = S("AWMOCQLAB");
      AWMOCQLABTIP = S("AWMOCQLABTIP");
      AWMOCTITLE = S("AWMOCTITLE");
      AWMOC1 = S("AWMOC1");
      AWMOC1TIP = S("AWMOC1TIP");
      AWMOC2 = S("AWMOC2");
      AWMOC2TIP = S("AWMOC2TIP");
      AWMOC3 = S("AWMOC3");
      AWMOC3TIP = S("AWMOC3TIP");
      AWSKYCOV = S("AWSKYCOV");
      AWMOCUNK = S("AWMOCUNK");
      AWHIPSRES = S("AWHIPSRES");
      AWNBROWS = S("AWNBROWS");
      AWREFPUB = S("AWREFPUB");
      AWPROGACC = S("AWPROGACC");
      AWPROGACCTIP = S("AWPROGACCTIP");
      AWDATAACC = S("AWDATAACC");
      AWDATAACCTIP = S("AWDATAACCTIP");
      AWDATAACCTIP1 = S("AWDATAACCTIP1");
      AWINVIEWTIP = S("AWINVIEWTIP");
      AWMOCQLABTIP2 = S("AWMOCQLABTIP2");
      AWXMATCH = S("AWXMATCH");
      AWXMATCHTIP = S("AWXMATCHTIP");
      AWCRIT = S("AWCRIT");
      AWCRITTIP = S("AWCRITTIP");
      AWMOCX = S("AWMOCX");
      AWMOCXTIP = S("AWMOCXTIP");
      AWDM = S("AWDM");
      AWDMTIP = S("AWDMTIP");
      AWPROGEN = S("AWPROGEN");
      AWPROGENTIP = S("AWPROGENTIP");
      AWSCANONLY = S("AWSCANONLY");
      AWSCANONLYTIP = S("AWSCANONLYTIP");
      AWLOAD = S("AWLOAD");
      FPCLOSE = S("FPCLOSE");
   }
   
   // Fournit une légende pour les couleurs de l'arbre
   class LegIn extends JPanel {
      
      public Dimension getPreferredSize() {
         return new Dimension( super.getPreferredSize().width, 18);
      }
      
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         g.setColor( cbg );
         g.fillRect(0, 0, getWidth(), getHeight());
         if( !isCheckIn ) return;
         g.setFont(g.getFont().deriveFont(Font.ITALIC));
         
         int x=40;
         int y=14;
         g.setColor(Aladin.COLOR_GREEN);
         Util.fillCircle7(g, x, y-4);
         g.setColor( Aladin.COLOR_LABEL );
         g.drawString("in view",x+7,y);
         
         x+=55;
         g.setColor(Aladin.ORANGE);
         Util.fillCircle7(g, x, y-4);
         g.setColor( Aladin.COLOR_LABEL );
         g.drawString("out view",x+7,y);
      }
   }

   public Directory(Aladin aladin, Color cbg) {
      this.aladin = aladin;
      loadString();
      multiProp = new MultiMoc2();
      this.cbg = cbg;
      
      // POUR LES TESTS => Surcharge de l'URL du MocServer
      // if( aladin.levelTrace>=3 ) {
      // aladin.glu.aladinDic.put("MocServer","http://localhost:8080/MocServer/query?$1");
      // aladin.trace(0,"WARNING: use local MocServer for test => http://localhost:8080/MocServer/query !!!");
      // }

      setBackground(cbg);
      setLayout(new BorderLayout(0, 0));
      setBorder(BorderFactory.createEmptyBorder(9, 3, 12, 0));
      
      JPanel pTitre1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 35, 0));
      pTitre1.setBackground(cbg);
      dir = new JLabel(DIRECTORY);
      Util.toolTip(dir, S("DTLABELTIP"), true);
      dir.setFont(dir.getFont().deriveFont(Font.BOLD));
      dir.setForeground(Aladin.COLOR_LABEL);
      pTitre1.add(dir);
      
      LegIn inLeg = new LegIn();
      
      JPanel pTitre = new JPanel( new BorderLayout(0,0) );
      pTitre.setBackground(cbg);
      pTitre.add( pTitre1, BorderLayout.NORTH );
      pTitre.add( inLeg, BorderLayout.CENTER );

      // L'arbre avec sa scrollbar
      dirTree = new DirectoryTree(aladin, cbg);
      scrollTree = new JScrollPane(dirTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
      scrollTree.setBackground(cbg);
      // scrollTree.getViewport().setOpaque(true);
      // scrollTree.getViewport().setBackground(cbg);
      scrollTree.setOpaque(false);
      scrollTree.setViewportBorder(null);

      if( Aladin.DARK_THEME ) {
         scrollTree.getVerticalScrollBar().setUI(new MyScrollBarUI());
         scrollTree.getHorizontalScrollBar().setUI(new MyScrollBarUI());
      }

      // Le controle des filtres
      String s = S("DTSELECTTIP");
      JLabel labelFilter = new JLabel(S("DTSELECT"));
      Util.toolTip(labelFilter, s, true);
      labelFilter.setFont(labelFilter.getFont().deriveFont(Font.BOLD));
      labelFilter.setForeground(Aladin.COLOR_LABEL);

      quickFilter = new QuickFilterField(10);
      Util.toolTip(quickFilter, s, true);

      s = S("DTCOMBOTIP");
      JLabel fromLabel = new JLabel(S("DTFROM"));
      Util.toolTip(fromLabel, s, true);
      fromLabel.setFont(fromLabel.getFont().deriveFont(Font.BOLD));
      fromLabel.setForeground(Aladin.COLOR_LABEL);

      comboFilter = new FilterCombo(s);

//      JLabel plus = new JLabel("  + ");
      JLabel plus = new JLabel(new ImageIcon(aladin.getImagette("editplus.png")));
      plus.setBorder( BorderFactory.createEmptyBorder(26, 2,0,0));
      Util.toolTip(plus, S("DTPLUSTIP"), true);
      plus.setFont(Aladin.LBOLD);
      plus.setForeground(Aladin.COLOR_LABEL);
      final Aladin a = aladin;
      plus.addMouseListener(new MouseListener() {
         public void mouseReleased(MouseEvent e) { 
            Aladin.makeCursor(a, Aladin.WAITCURSOR);
            openAdvancedFilterFrame();
            Aladin.makeCursor(a, Aladin.DEFAULTCURSOR);
         }
         public void mousePressed(MouseEvent e) { }
         public void mouseExited(MouseEvent e) { }
         public void mouseEntered(MouseEvent e) { }
         public void mouseClicked(MouseEvent e) { }
      });

      // Pour que le quickFilter et le popupFilter aient même taille et soient alignés, je les place
      // tous les deux dans un GridBagLayoutPanel qui sera CENTER dans BorderLayout, et à EAST de ce panel
      // le bouton du " + ". Ca permet également d'éviter de faire disparaitre ce "+" lorsque le bandeau
      // du Directory tree est trop étroit
      //
      // select XXXXXXXX |
      // from XXXXXXXX | +
      //
      JPanel plusFilter = new JPanel(new BorderLayout(0, 0));
      plusFilter.setBackground(cbg);
      plusFilter.add(plus, BorderLayout.EAST); //BorderLayout.SOUTH);

      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH; // J'agrandirai les composantes
      c.insets = new Insets(2, 3, 3, 2);
      JPanel panelFilter1 = new JPanel(g);
      panelFilter1.setBackground(cbg);
      PropPanel.addCouple(null, panelFilter1, labelFilter, null, quickFilter, g, c, GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL);
      PropPanel.addCouple(null, panelFilter1, fromLabel, null, comboFilter, g, c, GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL);

      JPanel panelFilter = new JPanel(new BorderLayout(0, 0));
      panelFilter.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      panelFilter.setBackground(cbg);
      panelFilter.add(panelFilter1, BorderLayout.CENTER);
      panelFilter.add(plusFilter, BorderLayout.EAST);

      // Les icones de controle tout en bas
      iconFilter = new IconFilter(aladin);
      iconCollapse = new IconCollapse(aladin);
      iconSort = new IconSort(aladin);
      iconInside = new IconInside(aladin);
      iconScan = new IconScan(aladin);

      JPanel pControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
      pControl.setBackground(cbg);
      pControl.add(iconCollapse);
      pControl.add(iconSort);
      pControl.add(iconInside);
      pControl.add(iconScan);
      pControl.add(iconFilter);

      JPanel panelControl = new JPanel(new BorderLayout(0, 0));
      panelControl.setBackground(cbg);
      panelControl.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
      panelControl.add(panelFilter, BorderLayout.NORTH);
      panelControl.add(pControl, BorderLayout.SOUTH);

      JPanel panelTree = new JPanel(new BorderLayout(0, 0));
      panelTree.setBackground(cbg);
      panelTree.add(pTitre, BorderLayout.NORTH);
      panelTree.add(scrollTree, BorderLayout.CENTER);
 
      add(panelTree, BorderLayout.CENTER);
      add(panelControl, BorderLayout.SOUTH);

      // Actions sur le clic d'un noeud de l'arbre
      dirTree.addMouseListener(new MouseAdapter() {
         public void mouseExited(MouseEvent e) {
            if( timerTip!=null ) { timerTip.stop(); timer=null; }
         }
         public void mousePressed(MouseEvent e) {
            if( timerTip!=null ) timerTip.stop();
            toHighLighted = null;
            TreePath tp = dirTree.getPathForLocation(e.getX(), e.getY());
            if( tp == null ) hideInfo();
            else {
               ArrayList<TreeObjDir> treeObjs = getSelectedTreeObjDir();

               // Double-clic, on effectue l'action par défaut du noeud
               if( e.getClickCount() == 2 ) loadMulti(treeObjs);

               // Simple clic => on montre les informations associées au noeud
               else {
                  if( treeObjs.size() == 1 ) selectInStack(treeObjs.get(0).internalId);
                  showInfo(treeObjs, e);
               }
            }

            iconCollapse.repaint();
            iconSort.repaint();
            resetWasExpanded();
            repaint();
         }
      });

      dirTree.addMouseMotionListener(new MouseMotionListener() {

         public void mouseMoved(MouseEvent e) {
            
            if( Aladin.aladin.inHelp  ) Aladin.aladin.help.setText(HELP);
            
            // Aide Tip sur ce bouton ?
            if( timerTip==null ) timerTip = new Timer(6000, new ActionListener() {
               public void actionPerformed(ActionEvent e) { showTip(); }
            }); 
            timerTip.restart();


            if( frameInfo == null || !frameInfo.isVisible() ) return;
            
            // La maj automatique de la fenêtre d'accès est invalidée si elle a été épinglée
            // il faut explicitement cliqué sur les noeuds
            if( decorated ) return;
            
            TreePath tp = dirTree.getPathForLocation(e.getX(), e.getY());
            if( tp == null ) return;
            if( getSelectedTreeObjDir().size() > 1 ) return;

            // Si on se déplace davantage vers la droite que vers le bas ou le haut
            // => on ne change pas
            Point p = e.getPoint();
            if( lastMove != null && 1.5 * (p.x - lastMove.x) > Math.abs(p.y - lastMove.y) ) return;
            lastMove = p;

            ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
            DefaultMutableTreeNode to = (DefaultMutableTreeNode) tp.getLastPathComponent();
            if( !to.isLeaf() ) return;
            treeObjs.add((TreeObjDir) to.getUserObject());
            if( treeObjs.size() == 1 ) {
               showInfo(treeObjs, e);
               toHighLighted = treeObjs.get(0);
               dirTree.repaint();
               // if( showInfo(treeObjs,e) ) dirTree.setSelectionPath(tp);
            }
         }

         public void mouseDragged(MouseEvent e) {
         }
      });

      // Chargement des paramètres initiaux
      initParams();

      // Chargement de l'arbre initial
      (new Thread() {
         public void run() {
            updateTree();
         }
      }).start();
   }
   
   // Affichage du tip associé au bouton courant
   private void showTip() {  aladin.configuration.showHelpIfOk("Datatree.HELP"); }
   
   private Timer timerTip = null;


   protected void selectInStack(String id) {
      aladin.calque.selectPlan(id);
   }

   /** Ouvre l'arbre en montrant le noeud associé au path spécifié */
   protected boolean showTreePath(String path) {
      if( !isVisible() || !hasCollections() || path == null ) return false;
      return dirTree.showBranch(path);
   }

   /** Ouvre l'arbre en montrant le noeud associé à l'id (venant du stack) spécifié */
   protected void showTreeObj(String id) {
      if( !isVisible() || !hasCollections() || id == null ) return;
      int i = id.indexOf('~');
      if( i < 0 ) i = id.indexOf(' ');
      if( i > 0 ) id = id.substring(0, i);
      dirTree.showTreeObj(id);
   }
   
   /** Retourne true si le directory contient quelque chose (avant filtrage éventuel) */
   protected boolean hasCollections() {
      return dirList != null && dirList.size() > 0;
   }

   /** Reset du filtre (combobox) possitionné */
   protected void reset() {
      directoryFilter.reset();
   }

   /** Reset complet du filtrage (quickFilter+combobox) */
   protected void fullReset() { fullReset(false); }
   protected void fullReset(boolean flagDefaultExpand ) {
      quickFilter.setText("");
      comboFilter.setSelectedIndex(0);
      reset();
      if( flagDefaultExpand ) dirTree.defaultExpand();

   }

   // Mémorisation de la dernière position de la souris en mouseMoved()
   private Point lastMove = null;

   // Mise à jour du formulaire des filtres s'il est visible
   protected void updateWidget() {
      if( directoryFilter != null && directoryFilter.isShowing() ) directoryFilter.updateWidget();
   }

   /** Met à jour la liste des filtres */
   protected void updateDirFilter() {
      if( comboFilter == null ) return;

      ActionListener[] a = comboFilter.getListeners(ActionListener.class);
      if( a != null ) for( ActionListener a1 : a )
         comboFilter.removeActionListener(a1);

      String current = (String) comboFilter.getSelectedItem();
      comboFilter.removeAllItems();
      comboFilter.addItem(ALLCOLLHTML);

      String mylist = aladin.configuration.filterExpr.get(MYLIST);
      if( mylist != null ) {
         comboFilter.addItem(Directory.MYLISTHTML);
      }

      for( String name : aladin.configuration.filterExpr.keySet() ) {
         if( name.equals(ALLCOLL) ) continue;
         if( name.equals(MYLIST) ) continue;
         comboFilter.addItem(name);
      }

      if( current != null ) comboFilter.setSelectedItem(current);

      if( a != null ) {
         for( ActionListener a1 : a ) comboFilter.addActionListener(a1);
      }
   }

   private TreeObjDir toHighLighted = null;

   /** retourne le noeud qu'il faut encadrer (passage de la souris dessus) */
   protected TreeObjDir getTreeObjDirHighLighted() {
      return toHighLighted;
   }

   class FilterCombo extends JComboBox<String> {
      FilterCombo(String tooltip) {
         super();
         setUI(new MyComboBoxUI());
         // setPrototypeDisplayValue("000000000000000");
         Util.toolTip(this, tooltip, true);
         setFont(Aladin.PLAIN);
         addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               filtre((String) getSelectedItem());
            }
         });
         updateWidgets();
      }

      void updateWidgets() {
         boolean actif = iconFilter != null && iconFilter.isActivated();
         setBackground(actif ? Aladin.COLOR_TEXT_BACKGROUND : Aladin.COLOR_TEXT_BACKGROUND.darker());
         // setForeground( actif ? Aladin.COLOR_TEXT_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
      }
   }

   protected void focusSearch() {
      quickFilter.focus(S("DTYRKW"));
   }

   /** Classe pour un JTextField avec reset en bout de champ (petite croix rouge) */
   class QuickFilterField extends JTextField implements KeyListener/* ,MouseListener,MouseMotionListener */ {
      private Rectangle cross = null;

      QuickFilterField(int nChar) {
         super(nChar);
         addKeyListener(this);
         setUI(new BasicTextFieldUI());
         updateWidgets();
      }

      public void keyTyped(KeyEvent e) {
      }

      public void keyPressed(KeyEvent e) {
      }

      public void keyReleased(KeyEvent e) {
         if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            if( timer != null ) timer.stop();
//            timer = null;
            doFiltre();
         } else filtre();
      }

      Timer timer = null;

      private void filtre() {
         if( timer == null ) {
            timer = new Timer(500, new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  doFiltre();
                  requestFocus();
                  timer = null;
               }
            });
            timer.setRepeats(false);
            timer.start();
         } else {
            timer.restart();
         }
         updateWidgets();
      }

      /**
       * Fait clignoter le champ pour attirer l'attention de l'utilisateur et demande le focus sur le champ de saisie
       */
      protected void focus(String s) {
         focus(s, null);
      }

      protected void focus(String s, final String initial) {
         setText(s);

         (new Thread() {
            Color def = getBackground();

            Color deff = getForeground();

            public void run() {
               for( int i = 0; i < 2; i++ ) {
                  setBackground(Color.green);
                  setForeground(Color.black);
                  Util.pause(800);
                  setBackground(def);
                  setForeground(deff);
                  Util.pause(200);
               }
               if( initial == null ) {
                  setText("");
                  requestFocusInWindow();
               } else {
                  setText(initial);
                  requestFocusInWindow();
                  setCaretPosition(getText().length());
               }
               updateWidgets();
            }
         }).start();
      }

      public Dimension getMaximumSize() {
         Dimension d = super.getMaximumSize();
         d.width = 150;
         return d;
      }

      void updateWidgets() {
         boolean actif = iconFilter != null && iconFilter.isActivated();
         setBackground(actif ? Aladin.COLOR_TEXT_BACKGROUND : Aladin.COLOR_CONTROL_BACKGROUND_UNAVAILABLE); // Aladin.COLOR_TEXT_BACKGROUND.darker()
                                                                                                            // );
         setForeground(actif ? Aladin.COLOR_TEXT_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE);
      }

      boolean in(int x) {
         if( cross == null ) return false;
         return x >= cross.x;
      }

      public void paintComponent(Graphics g) {
         try {
            super.paintComponent(g);
            int x = getWidth() - X - 8;
            int y = getHeight() / 2 - X / 2;
            if( mocServerLoading || mocServerUpdating ) {
               Slide.drawBall(g, x, y, blinkState ? Color.white : Color.green);
            }
         } catch( Exception e ) {
         }
      }

      static private final int X = 6;
   }

   /** Retourne true si on a sélectionné quelque chose de scannable */
   protected boolean isScannable() {
      return getSelectedTreeObjDirScannable().size() >= 1;
   }
   
   /** Retourne true si on a sélectionné quelque chose qui est triable */
   protected boolean isSortable() {
      TreePath tp = dirTree.getSelectionPath();
      if( tp==null ) return false;
      DefaultMutableTreeNode to = (DefaultMutableTreeNode) tp.getLastPathComponent();
      return to.getChildCount()>1;
   }

   /** Récupération de la liste des TreeObj sélectionnées qui n'ont pas de MOC à disposition */
   private ArrayList<TreeObjDir> getSelectedTreeObjDirScannable() {
      ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
      for( TreeObjDir to : getSelectedTreeObjDir() ) {
         if( !to.hasMoc() ) treeObjs.add(to);
      }
      return treeObjs;
   }

   /** Récupération de la liste des TreeObj sélectionnées */
   private ArrayList<TreeObjDir> getSelectedTreeObjDir() {
      TreePath[] tps = dirTree.getSelectionPaths();
      ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
      if( tps != null ) {
         for( int i = 0; i < tps.length; i++ ) {
            DefaultMutableTreeNode to = (DefaultMutableTreeNode) tps[i].getLastPathComponent();
            addObj(treeObjs, to);
         }
      }
      return treeObjs;
   }

   private void addObj(ArrayList<TreeObjDir> treeObjs, DefaultMutableTreeNode to) {
      if( to.isLeaf() ) {
         Object obj = to.getUserObject();
         if( obj instanceof TreeObjDir ) treeObjs.add((TreeObjDir) obj);

      } else {
         int n = to.getChildCount();
         for( int i = 0; i < n; i++ ) {
            addObj(treeObjs, (DefaultMutableTreeNode) to.getChildAt(i));
         }
      }
   }

   // La frame d'affichage des informations du HiPS cliqué à la souris
   private FrameInfo frameInfo = null;

   /** Cache la fenêtre des infos du HiPS */
   private void hideInfo() {
      showInfo(null, null);
      toHighLighted = null;
      repaint();
   }

   /** Chargements simultanés de toutes les collections sélectionnées */
   private void loadMulti(ArrayList<TreeObjDir> treeObjs) {
      if( treeObjs.size() == 0 ) return;
      hideInfo();
      if( tooMany(treeObjs.size()) ) return;
      for( TreeObjDir to : treeObjs )
         to.load();
   }

   private boolean tooMany(int n) {
      if( n > 20 ) {
         if( !aladin.confirmation(S("DTTOOMANY") + " (" + n + ")") ) return true;
      }
      return false;
   }

   /**
    * Affiche la fenetre des infos des Collections passées en paramètre
    * @param treeObjs les noeuds correspondant aux collections sélectionnées, ou null si effacement de la fenêtre
    * @param e évènement souris pour récupérer la position absolue où il faut afficher la fenêtre d'info
    */
   private boolean showInfo(ArrayList<TreeObjDir> treeObjs, MouseEvent e) {
      if( treeObjs == null || treeObjs.size() == 0 ) {
         if( frameInfo == null ) return false;
         frameInfo.setVisible(false);
         return false;
      }
      boolean created = false;
      if( frameInfo == null ) {
         created=true;
         frameInfo = new FrameInfo(decorated);
      }
      if( !frameInfo.setCollections(treeObjs) ) return false;

      Point p = e.getLocationOnScreen();
      int w = 350;
      int h = 120;
      //      int x = p.x + 50;
      int x = aladin.getLocationOnScreen().x+aladin.splitHiPSWidth.getDividerLocation();
      int y = p.y - 30;
      if( y < 0 ) y = 0;

      // Calcul de la position de la fenêtre si elle n'était pas "décorée"
      rectFrameInfo = new Rectangle( x, y, Math.max(w, frameInfo.getWidth()), Math.max(h, frameInfo.getHeight()) );
      
      // Positionnement de la fenêtre si elle n'est pas "décorée", ou si elle vient d'être créée
      if( !decorated || created )  frameInfo.setBounds( rectFrameInfo );

      frameInfo.setVisible(true);
      if( decorated ) frameInfo.toFront();
      
      this.decorated = decorated;
      treeObjsShown = treeObjs;

      return true;
   }
   
   private boolean decorated=false;
   private Rectangle rectFrameInfo = null;
   private ArrayList<TreeObjDir> treeObjsShown = null;
   
   private boolean reshowInfo(boolean decorated) {
      if( frameInfo==null || !frameInfo.isVisible() ) return false;
      Rectangle bounds = frameInfo.getBounds();
      frameInfo.dispose();
      frameInfo = new FrameInfo(decorated);
      this.decorated=decorated;
      if( !frameInfo.setCollections(treeObjsShown) ) return false;
      frameInfo.setBounds( !decorated ? rectFrameInfo : bounds );
      frameInfo.setVisible(true);
      return true;
      
   }

   // Gestion du scanning
   private boolean isScanning = false; // true si un scan est en cours

   synchronized private void setScanning(boolean flag) {
      isScanning = flag;
   }

   synchronized protected boolean isScanning() {
      return isScanning;
   }

   // Liste des collections en cours de scanning
   private ArrayList<TreeObjDir> scanTreeObjs = null;

   // Le gestionnaire du pool de threads du scan
   private ExecutorService scanService = null;

   // true lorsqu'on a demandé l'arrêt du scan courant
   private boolean abortScan = false;

   /**
    * Demande l'arrêt anticipé du scan en cours. Et force la sortie en exception des collections en cours de scanning en fermant
    * manu-militari leur flux de données vers le serveur. Cette technique n'est pas vraiment super, mais c'est mieux que rien
    */
   synchronized protected void abortScan() {
      if( !isScanning() ) return;
      abortScan = true;

      if( scanService != null ) scanService.shutdownNow();

      // On essaye de forcer la main aux collections en cours de scanning
      // en fermant le flux
      (new Thread() {
         public void run() {
            for( TreeObjDir to : scanTreeObjs ) {
               if( to.isScanning() ) to.abortScan();
            }
         }
      }).start();
   }

   /** true si le scan en cours doit être arrêté dès que possible */
   synchronized protected boolean isAbortingScan() {
      return abortScan;
   }

   /**
    * Lancement d'un scan sur les collections sélectionnées qui ne disposent pas d'un MOC. Lance une requête CS ou SIA ou SSA sur
    * le champ courant, et construit avec les positions retournées un MOC local dans le MultiProp. Celui-ci sera utilisé en ajout
    * du MocServer pour déterminer si la collection a ou n'a pas de résultat dans le champ courant; Afin de connaitre les champs
    * déjà scannés, un MOC de couverture sera également mémorisé dans multiProp (mocRef).
    */
   protected void scan() {

      // On va éviter de lancer plusieurs scans en parallèle.
      if( isScanning() ) {
         aladin.warning(S("DTSCANRUN"));
         return;
      }

      // Déployement de l'arbre pour montrer les collections concernées
      dirTree.allExpand(dirTree.getSelectionPath());

      // Liste des collections sélectionnées scannables
      final ArrayList<TreeObjDir> treeObjs = getSelectedTreeObjDirScannable();

      // Demande de confirmation du scan s'il concerne beaucoup de collections
      final int n = treeObjs.size();
      if( tooMany(n) ) return;

      // On va travaillé avec un pool de threads
      final ExecutorService service = Executors.newFixedThreadPool(10);

      // Mémorisation de la liste pour un éventuel abort (cf abortScan())
      scanTreeObjs = treeObjs;
      scanService = service;

      setScanning(true);
      (new Thread() {

         public void run() {

            // Le timer permet de réafficher l'arbre toutes les demi secondes et ainsi
            // de faire clignoter le logo "noMoc" des collections en cours de scanning
            startTimer();

            try {
               abortScan = false;
               flagScanLocal = true; // le MultiProp local doit être pris en compte

               Aladin.trace(4, "Directory.scan()... Launched on " + n + " data sets...");

               for( final TreeObjDir to : treeObjs ) {

                  // On scanne chaque collection (10 en parallèle)
                  service.execute(new Runnable() {
                     public void run() {
                        try {
                           // System.out.println("Scanning "+to.internalId+"...");
                           MocItem2 mo = multiProp.getItem(to.internalId);
                           to.scan(mo);
                           // System.out.println(mo.mocId+" => "+mo.getMocRef().todebug());

                        } catch( Exception e ) {
                           if( aladin.levelTrace >= 3 ) e.printStackTrace();
                        }
                     }
                  });
               }
               service.shutdown();

               // On ajoute au fur et à mesure les nouvelles collections qui ont été scannées
               while( !service.isTerminated() ) {
                  resumeIn(ResumeMode.LOCALADD);
                  Util.pause(500);
                  // System.out.println("Scan processing...");
               }
            } finally {
               setScanning(false);
               scanTreeObjs = null;
               scanService = null;
               stopTimer();
               Util.pause(200);
               resumeIn(ResumeMode.FORCE); // Un dernier resume complet de l'arbre
               Aladin.trace(4, "Directory.scan() finished");
               repaint();
            }
         }
      }).start();
   }

   /** Création/ouverture/fermeture du formulaire de filtrage de l'arbre des Collections */
   protected void openAdvancedFilterFrame() {
      if( directoryFilter == null ) directoryFilter = new DirectoryFilter(aladin);
//      if( directoryFilter.isVisible() ) directoryFilter.setVisible(false);
//      else {
         String name = (String) comboFilter.getSelectedItem();
         if( name.equals(ALLCOLLHTML) ) {
            name = MYLIST;
            aladin.configuration.setDirFilter(name, "", null);
            updateDirFilter();
            comboFilter.setSelectedItem(MYLISTHTML);
         }

         directoryFilter.showFilter();
//      }
   }

   /** Filtrage de l'arbre des Collections */
   protected void doFiltre() {
      if( directoryFilter == null ) directoryFilter = new DirectoryFilter(aladin);

      int i = aladin.directory.comboFilter.getSelectedIndex();
      if( iconFilter.isActivated() && i > 0 ) {
         aladin.directory.filtre((String) aladin.directory.comboFilter.getSelectedItem());

      } else {
         directoryFilter.submitAction(false);
      }

      if( getNbVisible() <1000 ) dirTree.allExpand();
   }

   /** Activation d'un filtre préalablement sauvegardé */
   protected void filtre(String name) {

      if( name.equals(ALLCOLLHTML) ) name = ALLCOLL;
      else if( name.equals(MYLISTHTML) ) name = MYLIST;

      // System.out.println("filter("+name+")");

      String expr = name.equals(ALLCOLL) ? "*" : aladin.configuration.filterExpr.get(name);
      HealpixMoc moc = name.equals(ALLCOLL) ? null : aladin.configuration.filterMoc.get(name);

      if( expr != null || moc != null ) {
         if( directoryFilter == null ) directoryFilter = new DirectoryFilter(aladin);

         int intersect = DirectoryFilter.getIntersect(moc);
         directoryFilter.setSpecificalFilter(name, expr, moc, intersect);
      }
      
      directoryFilter.toFront();
   }

   /** Positionne une contrainte, soit en texte libre, soit cle=valeur */
   protected String getQuickFilterExpr() {
      return getKeyWordExpr(quickFilter.getText().trim());
   }

   /** Génère une contrainte, soit en texte libre, soit cle=valeur */
   protected String getKeyWordExpr(String s) {
      String s1;
      if( s.length() == 0 ) return s;

      String expr;

      // si ça commence par ivo://xxx on le remplace par ID=xxx
      if( s.startsWith("ivo://") ) s = "ID=" + s.substring(6);

      int i = s.indexOf('=');
      if( i < 0 ) i = s.indexOf('>');
      if( i < 0 ) i = s.indexOf('<');

      // S'il s'agit directement d'une expression, rien à faire, ...
      if( i > 0 && (aladin.directory.isFieldName((s1 = s.substring(0, i).trim())) || s1.indexOf('*') >= 0) ) expr = s;

      // Sinon il faut générer l'expression en fonction d'une liste de mots clés
      else {
         // expr = "obs_title,obs_collection,ID="+DirectoryFilter.jokerize(s);

         StringBuilder s2 = null;
         Tok tok = new Tok(s);
         while( tok.hasMoreTokens() ) {
            expr = "obs_title,obs_collection,obs_collection_label,client_category,bib_reference,ID=" + DirectoryFilter.jokerize(tok.nextToken());
            if( s2 == null ) s2 = new StringBuilder(expr);
            else s2.append(" && " + expr);
         }
         expr = s2.toString();
      }

      return expr;
   }

   /** Retourne true si la chaine est une clé de propriété d'au moins un enregistrement */
   protected boolean isFieldName(String s) {
      return multiProp.isFieldName(s);
   }

   /**
    * Retourne le node correspondant à une identiciation
    * @param A l'identificateur de la collection à chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact 1 - substring sur label 2 - match exact puis substring sur l'IVOID (ex: Simbad ok pour
    *           CDS/Simbad) puis du menu (ex DssColored ok pour Optical/DSS/DssColored)
    * @return le Hips node trouvé, null sinon NECESSAIRE POUR get Hips(XXX) MAIS IL FAUDRA CHANGER CELA
    */
   protected TreeObjDir getTreeObjDir(String A) {
      return getTreeObjDir(A, 0);
   }

   protected TreeObjDir getTreeObjDir(String A, int mode) {
      for( TreeObjDir to : dirList ) {
         if( !to.isHiPS() ) continue;
         if( A.equals(to.id) || A.equals(to.label) || A.equals(to.internalId) ) return to;
         if( mode == 1 && Util.indexOfIgnoreCase(to.label, A) >= 0 ) return to;
         if( mode == 2 ) {
            if( to.internalId != null && to.internalId.endsWith(A) ) return to;

            int offset = to.label.lastIndexOf('/');
            if( A.equals(to.label.substring(offset + 1)) ) return to;
         }
      }

      if( mode == 2 ) {
         for( TreeObjDir to : dirList ) {
            if( !to.isHiPS() ) continue;
            int offset = to.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(to.label.substring(offset + 1), A) >= 0 ) return to;
         }
      }
      return null;
   }

   /**
    * Création du plan via script - méthode identique à celle de la série des classes ServerXXX (de fait, reprise de ServerHips de
    * la version Aladin v9)
    */
   protected int createPlane(String target, String radius, String criteria, String label, String origin) {
      String survey;
      int defaultMode = PlanBG.UNKNOWN;

      if( criteria == null || criteria.trim().length() == 0 ) survey = "CDS/P/DSS2/color";
      else {
         Tok tok = new Tok(criteria, ", ");
         survey = tok.nextToken();

         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode = PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("png") )
               defaultMode = PlanBG.JPEG;
         }
      }

      TreeObjDir to = getTreeObjDir(survey, 2);
      if( to == null ) {
         Aladin.warning(this, "Progressive survey (HiPS) unknown [" + survey + "]", 1);
         return -1;
      }

      // Pour éviter de charger 2x le même plan HiPS
      if( !Aladin.NOGUI && aladin.calque.isBGAlreadyLoaded(to.internalId) ) {
         if( !aladin.confirmation(aladin, aladin.chaine.getString("HIPSALREADYLOADED")) ) return -1;
      }

      try {
         if( defaultMode != PlanBG.UNKNOWN ) to.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! " + e.getMessage());
      }

      return aladin.hips(to, label, target, radius);
   }

   /**
    * Returne l'URL du service_url indiqué (cs,sia,ssa,tap...) pour l'enregistrement properties correspondant au mocid
    * @param service_url (cs,sia,ssa,tap...)
    * @param mocId
    * @return l'URL demandé, ou null si non trouvé
    */
   protected String resolveServiceUrl(String service_url, String mocId) {

      service_url = service_url.toLowerCase();
      
      // un éventuel préfixe ivo:// doit être ignoré
      if( mocId.startsWith("ivo://") ) mocId = mocId.substring(6);

      MocItem m = multiProp.getItem(mocId);
      if( m == null ) return null;

      // Recherche du champ correspondant au service_url indiqué
      String key = service_url + "_service_url";
      String s = m.prop.get(key);
      if( s == null ) return null;

      // Il peut y avoir plusieurs URLs séparées par des TAB. Il faut donc les nettoyer toutes
      StringBuilder url1 = null;
      Tok tok = new Tok(s, "\t");
      while( tok.hasMoreTokens() ) {
         String url = tok.nextToken();

         // Petit nettoyage de l'URL (le VO registry regorge d'imagination...)
         if( service_url.equals("tap") ) {
            if( url.endsWith("/") ) url = url.substring(0,url.length()-1);
         } else {
            if( !url.endsWith("?") && !url.endsWith("&") ) url += "?";
         }

         int pos;
         String fmt;

         // On enlève un éventuel &REQUEST=queryData redondant
         if( service_url.equalsIgnoreCase("ssa") ) {
            fmt = "&REQUEST=queryData";
            if( (pos = Util.indexOfIgnoreCase(url, fmt)) >= 0 ) {
               url = url.substring(0, pos) + url.substring(pos + fmt.length());
            }

         } else if( service_url.equalsIgnoreCase("sia") ) {
            fmt = "&FORMAT=image/fits";
            if( (pos = Util.indexOfIgnoreCase(url, fmt)) >= 0 ) {
               url = url.substring(0, pos) + url.substring(pos + fmt.length());
            }
         }

         if( url1 == null ) url1 = new StringBuilder(url);
         else url1.append("\t" + url);
      }

      return url1.toString();
   }

   @Override
   public Iterator<MocItem> iterator() {
      return multiProp.iterator();
   }

   // False lorsque la première initialisation de l'arbre est faite
   private boolean init = true;
   
// CA NE FONCTIONNE PAS CORRECTEMENT ACTUELLEMENT - PF DEC 2017
   protected void reload() {
//      init = true;
//      try {
//         initMultiProp(true);
//         buildTree();
//      } finally {
//         postTreeProcess(true);
//         init = false;
//      }
   }

   /** Maj initiale de l'arbre - et maj des menus Aladin correspondants */
   protected void updateTree() {
      try {
         initMultiProp(false);
         buildTree();
      } finally {
         postTreeProcess(true);
         init = false;
      }
   }

   /**
    * (Re)construction de l'arbre en fonction de l'état précédent de l'arbre et de la valeur des différents flags associés aux
    * noeuds
    * @param refCount true si on doit faire les décomptages des noeuds en tant que référence sinon état courant
    * @return true s'il y a eu au-moins une modif
    */
   private void buildTree() {
      DirectoryModel model = (DirectoryModel) dirTree.getModel();
      for( TreeObjDir to : dirList ) model.createTreeBranch(to);
      int n = initCounter(model);
      updateTitre(n);
   }
   
   private int nbVisible=-1;
   
   /** Retourne le nombre de feuilles de l'arbre visibles */
   private int getNbVisible() { return nbVisible==-1 ? dirList.size() : nbVisible; }

   // Initialisation du compteur de référence en fonction d'un TreeModel
   private int initCounter(DirectoryModel model) {
      // Initialisation du compteur de référence
      HashMap<String, Integer> hs = new HashMap<String, Integer>();
      int n = model.countDescendance(hs);
      counter = hs;
      return n;
   }

   /** Mise à jour du titre au-dessus de l'arbre en fonction des compteurs */
   private void updateTitre(int nb) {
      String t = DIRECTORY;
      if( nb != -1 && dirList != null && nb < dirList.size() ) {
         t = "<html>" + t + "<font color=\"#D0D0F0\"> &rarr; " + nb + " / " + dirList.size() + "</font></html>";
         nbVisible=nb;
      } else nbVisible=-1;
      
      dir.setText(t);
      if( directoryFilter != null && dirList != null )
         directoryFilter.setLabelResume(nb, dirList.size(), quickFilter.getText().trim().length() > 0);

   }

   // Compteurs des noeuds
   private HashMap<String, Integer> counter = null;

   private HashSet<String> wasExpanded = null;

   protected void resetWasExpanded() {
      wasExpanded = null;
   }
   
   /** Réaffichage de l'arbre avec un nouveau tri */
   protected void resumeSort() {
      wasExpanded = null;
      resumeTree(dirList, false, false);
   }

   /**
    * Reconstruction de l'arbre en utilisant à chaque fois un nouveau model pour éviter les conflits d'accès et éviter la lenteur
    * des évènements de maj des état de l'arbre (après de très nombreux tests, c'est de loin la meilleure méthode que j'ai trouvée
    * même si elle peut paraitre étonnante à première vue.)
    * @param tmpDirList
    */
   private void rebuildTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter) {
      boolean insideActivated = iconInside.isActivated();

      // Mémorisation temporaire des états expanded/collapsed
      if( wasExpanded == null ) {
         wasExpanded = new HashSet<String>();
         backupState(new TreePath(dirTree.root), wasExpanded, dirTree);
      }

      // Mémorisation temporaire du premier noeud sélectionné
      TreePath tp = dirTree.getSelectionPath();

      // Génération d'un nouveau model prenant en compte les filtres
      DirectoryModel model = new DirectoryModel(aladin);

      // S'il y a des noeuds à cacher, on va regénérer un tableau intermédiaire des noeuds restants,
      // mais en le post-triant pour que les branches de l'arbre restent dans le même ordre que précédemment
      ArrayList<TreeObjDir> tmpDirList1 = new ArrayList<TreeObjDir>(tmpDirList.size());
      for( TreeObjDir to : tmpDirList ) {
         boolean mustBeActivated = !to.isHidden() && (!insideActivated || insideActivated && to.getIsIn() != 0);
         if( mustBeActivated ) {
            directorySort.setInternalSortKey(to.id,to.prop);
            to.setTri();
            tmpDirList1.add(to);
         }
      }
      Collections.sort(tmpDirList1, TreeObj.getComparator());
      for( TreeObjDir to : tmpDirList1 ) model.createTreeBranch(to);

      if( initCounter ) initCounter(model);
      else updateTitre(model.countDescendance());

      // Répercussion des états des feuilles sur les branches
      model.populateFlagIn();

      // Remplacement du model dans l'arbre affiché
      dirTree.setModel(model);

      // Ouverture minimal des noeuds
      if( defaultExpand ) dirTree.defaultExpand();

      // Restauration des états expanded/collapses + compteurs de référence
      restoreState(new TreePath(model.root), defaultExpand ? null : wasExpanded, counter, dirTree);

      // Restauration des noeuds sélectionnées
      if( tp != null ) showTreePath(getPathString(tp));
   }

   /**
    * Retourne le path sous forme de chaine - sans le premier "/" et "" pour la racine ex => Image/Optical/DSS
    */
   private String getPathString(TreePath p) {
      boolean first = true;
      StringBuilder path = null;
      for( Object n : p.getPath() ) {
         if( first ) {
            first = false;
            continue;
         } // on ne prend pas la racine
         if( path == null ) path = new StringBuilder(n + "");
         else path.append("/" + n);
      }
      return path == null ? "" : path.toString();
   }

   /** Mémorise dans une HashSet les branches qui sont expanded */
   private void backupState(TreePath parent, HashSet<String> wasExpanded, DirectoryTree tree) {
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      TreeObj to = (TreeObj) lastNode.getUserObject();
      if( tree.isExpanded(parent) ) wasExpanded.add(to.path);

      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            backupState(parent.pathByAddingChild(node), wasExpanded, tree);
         }
      }
   }

//   private String parent(String path) {
//      int i = path.lastIndexOf('/');
//      if( i < 0 ) return "";
//      return path.substring(0, i);
//   }

   /**
    * Restaure l'état des branches en fonction d'une hashSet des branches qui étaient expanded, ainsi que d'une hashmap donnant
    * les décomptes de référence pour chaque branche.
    */
   private void restoreState(TreePath parent, HashSet<String> wasExpanded, HashMap<String, Integer> counter, JTree tree) {
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      String path = getPathString(parent);

      // Récupération de l'état expanded/collapsed
      if( wasExpanded != null ) {
         if( wasExpanded.contains(path) ) tree.expandPath(parent);
      }

      // Récupération du compteur de référence
      Integer nbRef = counter.get(path);
      if( nbRef != null ) {
         TreeObj to = (TreeObj) lastNode.getUserObject();
         to.nbRef = nbRef.intValue();
      }

      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            restoreState(parent.pathByAddingChild(node), wasExpanded, counter, tree);
         }
      }
   }
   
   /** Réaffichage de l'arbre en fonction des flags courants */
   protected void resumeTree() {
      resumeTree(dirList, false, false);
   }

   /** Remplacement et réaffichage de l'arbre avec une nouvelle liste de noeuds */
   protected void replaceTree(ArrayList<TreeObjDir> tmpDirList) {
      resumeTree(tmpDirList, true, true);
      dirList = tmpDirList;
   }

   /** Réaffichage de l'arbre */
   private void resumeTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter) {
      try {
         // long t0 = System.currentTimeMillis();
         rebuildTree(tmpDirList, defaultExpand, initCounter);
         validate();
         postTreeProcess(defaultExpand);
         // System.out.println("resumeTree done in "+(System.currentTimeMillis()-t0)+"ms");
      } finally {

         // Pour permettre le changement du curseur d'attente de la fenêtre de filtrage
         if( directoryFilter != null ) aladin.makeCursor(directoryFilter, Aladin.DEFAULTCURSOR);
      }

   }

   /** retourne true si un filtre est positionné */
   protected boolean hasFilter() {
      String s = quickFilter.getText();
      if( s == null || s.startsWith(UPDATING) ) s = "";
      return s.length() > 0 || comboFilter.getSelectedIndex() > 0;
   }

   // Mémorisation du dernier filtre demandé (pour éviter de l'appliquer 2x de suite)
   private String oExpr = null;

   private int oIntersect = -1;

   private HealpixMoc oMoc = null;

   /**
    * Filtrage et réaffichage de l'arbre en fonction des contraintes indiquées dans params
    * @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    * @param moc filtrage spatial, null si aucun
    * @param intersect pour le filtrage spatial: MultiMoc.OVERLAPS, ENCLOSED ou COVERS
    */
   protected void resumeFilter(String expr, HealpixMoc moc, int intersect) {
      try {

         // Ajout de la contrainte du filtre rapide à l'expression issue du filtre global
         String quick = getQuickFilterExpr();
         if( quick.length() > 0 ) {
            if( expr.length() == 0 || expr.equals("*") ) expr = quick;
            else expr = "(" + expr + ") && " + quick;
         }

         // On vient-ti pas de le faire ?
         if( expr.equals(oExpr) && (moc == oMoc || moc != null && moc.equals(oMoc) && intersect == oIntersect) ) { return; }
         oExpr = expr;
         oIntersect = intersect;
         oMoc = moc == null ? null : (HealpixMoc) moc.clone();

         // logs
         String smoc = moc == null ? "" : directoryFilter.getASCII(moc);
         if( smoc.length() > 0 || expr.length() > 0 && !expr.equals("*") ) {
            String log = expr + (smoc.length() > 0 ? " AND MOC:" + smoc : "");
            aladin.trace(4, "Directory.resumeFilter() => " + log);
            aladin.glu.log("DirectoryFilter", log);
         }

         // Filtrage
         try {
            checkFilter(expr, moc, intersect);
            flagError = false;
         } catch( Exception e ) {
            if( Aladin.levelTrace >= 3 ) e.printStackTrace();
            flagError = true;
         }

         // Regénération de l'arbre
         resumeTree();

         if( getNbVisible() <1000 ) dirTree.allExpand();

         updateWidgets();

      } catch( Exception e ) {
         if( Aladin.levelTrace >= 3 ) e.printStackTrace();
      }
   }

   private void updateWidgets() {
      iconFilter.repaint();

      quickFilter.updateWidgets();
      comboFilter.updateWidgets();
   }

   static protected enum ResumeMode {
      NORMAL, FORCE, LOCALADD
   };

   /** Réaffichage de l'arbre en fonction des Hips in/out de la vue courante */
   protected void resumeIn() {
      resumeIn(ResumeMode.NORMAL);
   }

   protected void resumeIn(ResumeMode mode) {
      if( !checkIn(mode) ) return;
      if( iconInside.isActivated() ) resumeTree();
      ((DirectoryModel) dirTree.getModel()).populateFlagIn();
      repaint();
   }

   /**
    * Positionnement des flags isHidden() de l'arbre en fonction des contraintes de filtrage
    * @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    * @param moc filtrage spatial, null si aucun
    * @param intersect pour le filtrage spatial, OVERLAPS, ENCLOSED ou COVERS
    */
   private void checkFilter(String expr, HealpixMoc moc, int intersect) throws Exception {

      // Filtrage par expression
      // long t0 = System.currentTimeMillis();
      ArrayList<String> ids = multiProp.scan((HealpixMoc) null, expr, false, -1, -1);
      // System.out.println("Filter: "+ids.size()+"/"+multiProp.size()+" in "+(System.currentTimeMillis()-t0)+"ms");

      // Filtrage spatial
      ArrayList<String> ids1 = filtrageSpatial(moc, intersect);

      // Positionnement des flags isHidden() en fonction du filtrage
      HashSet<String> set = new HashSet<String>(ids.size());
      for( String s : ids ) {
         if( ids1 != null && !ids1.contains(s) ) continue;
         set.add(s);
      }
      for( TreeObjDir to : dirList ) to.setHidden(!set.contains(to.internalId));
   }

   private HealpixMoc oldMocSpatial = null;

   private ArrayList<String> oldIds = null;

   private int oldIntersect = MultiMoc.OVERLAPS;

   /**
    * Filtrage spatial sur le MocServer distant. Utilise un cache pour éviter de faire plusieurs fois de suite la même requête
    * @param moc
    * @param intersect pour le filtrage spatial, OVERLAPS, ENCLOSED ou COVERS
    * @return la liste des IDs qui matchent
    */
   private ArrayList<String> filtrageSpatial(HealpixMoc moc, int intersect) {
      if( moc == null ) return null;
      if( oldMocSpatial != null && intersect == oldIntersect && oldMocSpatial.equals(moc) ) return oldIds;

      oldIntersect = intersect;
      oldMocSpatial = moc;
      try {
         oldIds = filtrageSpatial1(moc, intersect);
      } catch( Exception e ) {
         oldIds = null;
         if( aladin.levelTrace >= 3 ) e.printStackTrace();
      }
      return oldIds;
   }

   /**
    * Filtrage spatial sur le MocServer distant. Utilise un cache pour éviter de faire => voir filtrageSpatial(...)
    */
   private ArrayList<String> filtrageSpatial1(HealpixMoc moc, int intersect) throws Exception {
      String url = aladin.glu.getURL("MocServer").toString();
      int i = url.lastIndexOf('?');
      if( i > 0 ) url = url.substring(0, i);

      MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
      String boundary = MultiPartPostOutputStream.createBoundary();
      HttpURLConnection urlConn = (HttpURLConnection) MultiPartPostOutputStream.createConnection(new URL(url));
      urlConn.setRequestProperty("Accept", "*/*");
      urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
      urlConn.setRequestProperty("Connection", "Keep-Alive");
      urlConn.setRequestProperty("Cache-Control", "no-cache");
      MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);
      if( intersect != MultiMoc.OVERLAPS ) out.writeField("intersect", MultiMoc.INTERSECT[intersect]);
      File tmp = File.createTempFile("tmp", "fits");
      tmp.deleteOnExit();
      FileOutputStream fo = new FileOutputStream(tmp);
      try {
         moc.writeFits(fo);
      } finally {
         try {
            fo.close();
         } catch( Exception e ) {
         }
      }
      out.writeFile("moc", null, tmp, false);
      out.close();

      // récupération de chaque ID concernée (1 par ligne)
      BufferedReader in = null;
      ArrayList<String> ids = null;
      try {
         in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
         String s;
         ids = new ArrayList<String>();
         while( (s = in.readLine()) != null )
            ids.add(getId(s));
      } finally {
         in.close();
      }

      return ids;
   }

   /**
    * Détermination de l'ordre pour avoir 30 cellules dans la distance
    * @param size taille à couvrir (en degrés)
    * @return order HEALPix approprié
    */
   private int getAppropriateOrder(double size) {
      int order = 4;
      if( size == 0 ) order = HealpixMoc.MAXORDER;
      else {
         double pixRes = size / 30;
         double degrad = Math.toDegrees(1.0);
         double skyArea = 4. * Math.PI * degrad * degrad;
         double res = Math.sqrt(skyArea / (12 * 16 * 16));
         while( order < HealpixMoc.MAXORDER && res > pixRes ) {
            res /= 2;
            order++;
         }
      }
      return order;
   }

   /**
    * Retourne true si pour la collection identifiée par "id" on dispose d'un MOC local sur la zone décrite par mocQuery (càd que
    * leur intersection n'est pas nulle
    */
   private boolean hasLocalMoc(String id, HealpixMoc mocQuery) {
      if( mocQuery == null ) return false;
      MocItem2 mo = multiProp.getItem(id);
      if( mo.mocRef == null ) return false;
      return mo.mocRef.isIntersecting(mocQuery);
   }

   // Dernier champs interrogé sur le MocServer
   private Coord oc = null;

   private double osize = -1;

   private HashSet<String> previousSet = null;
   
   private boolean isCheckIn = false;

   /**
    * Interroge le MocServer pour connaître les Collections disponibles dans le champ. Met à jour l'arbre en conséquence
    * @param mode RESUME_IN_IF_NEEDED, RESUME_IN_FORCE, RESUME_IN_FORCE_LOCAL
    */
   private boolean checkIn(ResumeMode mode) {
      if( !dialogOk() ) return false;

      // Le champ est trop grand ou que la vue n'a pas de réf spatiale ?
      // => on suppose que tous les HiPS sont a priori visibles
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
         boolean modif = false;
         for( TreeObjDir to : dirList ) {
            if( !modif && to.getIsIn() != -1 ) modif = true;
            to.setIn(-1);
         }
         isCheckIn=false;
         return modif;
      }

      try {

         HashSet<String> set = mode == ResumeMode.LOCALADD ? previousSet : new HashSet<String>();

         // Pour éviter de faire 2x la même chose de suite
         Coord c = v.getCooCentre();
         double size = v.getTaille();
         boolean sameLocation = c.equals(oc) && size == osize;

         String params;
         if( mode == ResumeMode.NORMAL && sameLocation ) return false;
         oc = c;
         osize = size;

         // Interrogation du MocServer distant...
         BufferedReader in = null;

         // Interrogation par cercle
         if( v.getTaille() > 45 ) {
            params = MOCSERVER_PARAM + "&RA=" + c.al + "&DEC=" + c.del + "&SR=" + size * Math.sqrt(2);

            // Interrogation par rectangle
         } else {
            StringBuilder s1 = new StringBuilder("Polygon");
            for( Coord c1 : v.getCooCorners() )
               s1.append(" " + c1.al + " " + c1.del);
            params = MOCSERVER_PARAM + "&stc=" + URLEncoder.encode(s1.toString());
         }

         try {
            if( mode == ResumeMode.FORCE || !sameLocation ) {
               URL u = aladin.glu.getURL("MocServer", params, true);

               Aladin.trace(4, "HipsMarket.hipsUpdate: Contacting MocServer : " + u);
               in = new BufferedReader(new InputStreamReader(Util.openStream(u)));
               String s;

               // récupération de chaque ID concernée (1 par ligne)
               while( (s = in.readLine()) != null )
                  set.add(getId(s));
            }

         } catch( EOFException e ) {
         } finally {
            if( in != null ) in.close();
         }

         // Interrogation du Multimoc interne (uniquement par cercle)
         HealpixMoc mocQuery = null;
         if( flagScanLocal && (mode == ResumeMode.FORCE || mode == ResumeMode.LOCALADD || !sameLocation) ) {

            // Construction d'un MOC qui englobe le cercle couvrant le champ de vue courant
            Healpix hpx = new Healpix();
            int order = getAppropriateOrder(size);
            mocQuery = new HealpixMoc(order);
            int i = 0;
            mocQuery.setCheckConsistencyFlag(false);
            for( long n : hpx.queryDisc(order, c.al, c.del, size / 2) ) {
               mocQuery.add(order, n);
               if( (++i) % 1000 == 0 ) mocQuery.checkAndFix();
            }
            mocQuery.setCheckConsistencyFlag(true);
            // System.out.println("Moc d'interrogation => "+mocQuery.todebug());

            ArrayList<String> mocIds = multiProp.scan(mocQuery);
            if( mocIds != null ) for( String id : mocIds )
               set.add(id);
         }

         // Vérification si ça a changé depuis la dernière fois
         if( previousSet != null && previousSet != set && previousSet.equals(set) ) {
            // System.out.println("Pas de changement depuis la dernière fois");
            return false;
         }
         previousSet = set;

         // Positionnement des flags correspondants
         for( TreeObjDir to : dirList ) {
            if( !to.hasMoc() && !hasLocalMoc(to.internalId, mocQuery) ) to.setIn(-1);
            else to.setIn(set.contains(to.internalId) ? 1 : 0);
         }
         isCheckIn=true;

      } catch( Exception e1 ) {
         if( Aladin.levelTrace >= 3 ) e1.printStackTrace();
      }

      return true;
   }

   /** Retourne true si l'arbre est développé selon le défaut prévu */
   protected boolean isDefaultExpand() {
      return dirTree.isDefaultExpand();
   }
   
   protected void tri(Component parent, int x, int y) {
      TreePath tps = dirTree.getSelectionPath();
      if( tps==null ) return;
      DefaultMutableTreeNode to = (DefaultMutableTreeNode) tps.getLastPathComponent();
      TreeObj t = (TreeObj)to.getUserObject();
      JPopupMenu popup = directorySort.createPopup( t.path );
      popup.show(parent,x,y);
   }

   /** Collapse/Expande l'arbre en fonction du noeud courant et de l'état de l'arbre */
   protected void collapseOrNot() {

      // Si j'ai une ou plusieurs branches sélectionnées...
      TreePath[] tps = dirTree.getSelectionPaths();
      if( tps != null ) {

         // Sélection d'une feuille => on collapse tout l'arbre sauf la feuille
         if( tps.length == 1 && ((DefaultMutableTreeNode) tps[0].getLastPathComponent()).isLeaf() ) {
            dirTree.defaultExpand();
            dirTree.expandPath(tps[0].getParentPath());
            dirTree.setSelectionPath(tps[0]);

            // Selection de plusieurs noeud
         } else {

            int mode = 0; // -1 collapse, 0-pas encore initialisé, 1 expand
            for( TreePath tp : tps ) {

               // cas particulier de la racine => on ne referme pas tout
               if( tp.getPathCount() == 1 ) {
                  if( mode == 1 || isDefaultExpand() ) {
                     mode = 1;
                     dirTree.allExpand();
                  } else {
                     mode = -1;
                     dirTree.defaultExpand();
                  }

                  // Cas de la sélection d'une branche
               } else {

                  // S'il n'est pas totalement ouvert, je le fais
                  if( mode == 1 || dirTree.isCollapsed(tp) ) {
                     mode = 1;
                     dirTree.allExpand(tp);
                  } else {
                     mode = -1;
                     dirTree.collapseRec(tp);
                  }
               }
            }
         }

         // Rien n'est sélectionné => comme si on avait sélectionné la racine
      } else {
         if( isDefaultExpand() ) dirTree.allExpand();
         else dirTree.defaultExpand();
      }
      resetWasExpanded();
   }

   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return dirTree == null || dirTree.root == null;
   }

   /** Traitement à appliquer après la génération ou la régénération de l'arbre */
   private void postTreeProcess(boolean minimalExpand) {

      // filter.setEnabled( dialogOk() );
      if( minimalExpand ) dirTree.minimalExpand();

      // Mise en route ou arrêt du thread de coloration de l'arbre en fonction des Collections
      // présentes ou non dans la vue courante
      if( hasCollections() ) startInsideUpdater();
      else stopInsideUpdater();
   }

   /** Ajoute si nécessaire les propriétés d'un fichier "properties" distant */
   private void addRemoteProp(MyProperties prop) {
      String u = prop.getProperty("hips_service_url");
      if( u == null || prop.getProperty("hips_order") != null ) return;

      InputStreamReader in = null;
      try {
         in = new InputStreamReader(Util.openAnyStream(u + "/properties"), "UTF-8");
         MyProperties aProp = new MyProperties();
         aProp.load(in);

         for( String key : aProp.getKeys() ) {
            if( prop.get(key) == null ) prop.put(key, aProp.get(key));
         }
      } catch( Exception e ) {
      } finally {
         try {
            if( in != null ) in.close();
         } catch( Exception e ) {
         }
      }

   }

   private boolean interruptServerReading;

   /** (Re)génération du Multiprop en fonction d'un stream d'enregistrements properties */
   protected int loadMultiProp(InputStreamReader in, boolean addition, String path) throws Exception {
      MyProperties prop;

      boolean mocServerReading = true;
      interruptServerReading = false;
      int n = 0;
      int rm = 0;
      String memo = "";

      try {
         memo = quickFilter.getText();
         quickFilter.setEditable(false);
         quickFilter.setForeground(Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE);

         while( mocServerReading && !interruptServerReading ) {
            prop = new MyProperties();
            mocServerReading = prop.loadRecord(in);
            if( prop.size() == 0 || MultiMoc.getID(prop) == null ) continue;

            // Définition locale => PROP_ORIGIN = local
            if( addition ) {
               prop.setProperty("PROP_ORIGIN", "local");

               // si HiPS, peut être requière un complément d'information par accès au fichier properties distant ?
               addRemoteProp(prop);

               // Dans le cas d'un fichier local, on conserve on éventuel path pour le HiPS
               if( path != null ) prop.setProperty("hips_service_url", path);
            }

            try {

               if( prop.getProperty("MOCSERVER_REMOVE") != null ) {
                  multiProp.remove(MultiMoc.getID(prop));
                  rm++;
               } else multiProp.add(prop);

               quickFilter.setText(UPDATING + " (" + n + ")");
            } catch( Exception e ) {
               if( Aladin.levelTrace >= 3 ) e.printStackTrace();
            }

            n++;
            if( n % 1000 == 0 && n > 0 ) aladin.trace(4,
                  "Directory.loadMultiProp(..) " + (n - rm) + " prop loaded " + (rm > 0 ? " - " + rm + " removed" : "") + "...");
            // if( n%100==0 ) Util.pause(10);
         }
         if( interruptServerReading ) aladin.trace(3, "MocServer update interrupted !");
      } finally {
         try {
            in.close();
         } catch( Exception e ) {
         }
         quickFilter.setEditable(true);
         quickFilter.setForeground(Aladin.COLOR_TEXT_FOREGROUND);
         quickFilter.setText(memo);
      }

      return n;
   }

   /** Interruption du chargement des infos du MocServer */
   protected void interruptMocServerReading() {
      interruptServerReading = true;
   }
   
   /**
    * Génération de la liste des collections en fonction du contenu du MultiProp La liste est triée Les URLs HiPS seront
    * mémorisées dans le Glu afin de pouvoir gérer les sites miroirs
    */
   private ArrayList<TreeObjDir> populateMultiProp() {
      ArrayList<TreeObjDir> listReg = new ArrayList<TreeObjDir>(30000);
      multiple=null;  // Il faut reseter la liste des collections à plusieurs "feuilles"
      for( MocItem mi : this ) populateProp(listReg, mi.prop);
      Collections.sort(listReg, TreeObj.getComparator());
      return listReg;
   }

   private static final String AD = "AladinDesktop";

   private boolean hasValidProfile(MyProperties prop) {
      Iterator<String> it = prop.getIteratorValues("client_application");
      if( it == null ) return true;
      while( it.hasNext() ) {
         String profile = it.next();
         Tok tok = new Tok(profile);
         while( tok.hasMoreTokens() ) {
            profile = tok.nextToken();
            int offset = 0;
            if( (offset = profile.indexOf(AD)) < 0 ) continue;
            profile = profile.substring(offset + AD.length()).trim();

            // Si j'ai une mention de AladinDesktop, j'analyse le suffixe éventuel (ex: AladinDesktopBeta>9.6)
            if( profile.length() > 0 ) {
               String gluProfile = getGluProfile(profile);
//                System.out.println("Profile = "+gluProfile);
               return aladin.glu.hasValidProfile(gluProfile);

               // System.out.println("valide="+aladin.glu.hasValidProfile(gluProfile));
               // return true;
            }
         }
      }
      return true;
   }

   /** Retourne true si on a au moins une clé vers une url d'accès */
   private boolean hasURLkey(MyProperties prop) {
      for( String k : prop.getKeys() ) {
         if( k.indexOf("url") >= 0 ) return true;
      }
      return false;
   }

   // Conversion d'un profile à la "properties", en un profile à la "GLU"
   // Exemple: "Beta>9.6" sera converti en "beta >9.6"
   private String getGluProfile(String s) {
      StringBuilder rep = new StringBuilder();
      int mode = 0;
      for( char c : s.toCharArray() ) {
         switch( mode ) {
            case 0: // Avant un nouveau token ou dans un mot
               if( Character.isUpperCase(c) ) rep.append(" " + Character.toLowerCase(c));
               else if( isDigit(c) || c == '<' || c == '>' ) {
                  mode = 1;
                  rep.append(" " + c);
               } else rep.append(c);
               break;
            case 1: // Dans un numéro de version (token numérique)
               if( c == '<' || c == '>' ) rep.append(" " + c);
               else if( !isDigit(c) && c != '=' ) {
                  mode = 0;
                  rep.append(" " + Character.toLowerCase(c));
               } else rep.append(c);
               break;
         }
      }
      return rep.toString();
   }
   
   private boolean isDigit(char c) { return Character.isDigit(c) || c=='.'; }

   /**
    * Ajout d'une collection correspondant à un enregistrement prop, ainsi que des entrées GLU associées
    * @param prop Enregistrement des propriétés
    * @param localFile true si cet enregistrement a été chargé localement par l'utilisateur (ACTUELLEMENT NON UTILISE)
    */
   private void populateProp(ArrayList<TreeObjDir> listReg, MyProperties prop) {

      // Détermination de l'identificateur
      String id = MultiMoc.getID(prop);
      if( id == null ) {
         System.err.println(
               "Directory.populateProp error - getID returns null => ignored [" + prop.toString().replace("\n", " ") + "]");
         return;
      }

      // S'agit-il d'un hips privé ?
      String status = prop.getProperty("hips_status");
      if( status != null && status.indexOf("private") >= 0 ) return;

      // Est-ce qu'il y a un "profile", et si oui, est-il compatible avec cette version d'Aladin
      if( !hasValidProfile(prop) ) return;

      // Y a-t-il au moins un moyen d'accès ?
      if( !hasURLkey(prop) ) return;

      // Ajustement local des propriétés
      try {
         propAdjust(id, prop);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return;
      }

      String HIPSU = Constante.KEY_HIPS_SERVICE_URL;

      // Dans le cas où il n'y a pas de mirroir, mémorisation de l'URL directement
      if( prop.getProperty(HIPSU + "_1") == null ) {
         String url = prop.getProperty(HIPSU);
         if( url != null ) aladin.glu.aladinDic.put(id, url);

         // Dans le cas où il y a des mirroirs => mémorisation des indirections
      } else {

         // Enregistrement de tous les mirroirs comme des indirections
         StringBuilder indirection = null;
         for( int i = 0; true; i++ ) {
            String url = prop.getProperty(HIPSU + (i == 0 ? "" : "_" + i));
            if( url == null ) break;

            String subid = id + "_" + i;
            aladin.glu.aladinDic.put(subid, url);

            if( indirection == null ) indirection = new StringBuilder("%I " + subid);
            else indirection.append("\t" + subid);
         }

         // Mémorisation des indirections possibles sous la forme %I id0\tid1\t...
         if( indirection != null ) aladin.glu.aladinDic.put(id, indirection.toString());
      }

      // Ajout dans la liste des noeuds d'arbre
      listReg.add(new TreeObjDir(aladin, id, prop));
   }
   
   
   /**
    * Ajustement des propriétés, notamment pour ajouter le bon client_category s'il s'agit d'un catalogue
    */
   private void propAdjust(String id, MyProperties prop) {
      
      // Insertion de la date de publication de l'article de référence
      String bib = prop.get("bib_reference");
      if( bib != null ) {
         try {
            int year = Integer.parseInt(bib.substring(0, 4));
            prop.replaceValue("bib_year", "" + year);
         } catch( Exception e ) { }
      }

      // Traitement spécifique CDS
      propAdjust1(id, prop);
      
      String category = prop.getProperty(Constante.KEY_CLIENT_CATEGORY);
      boolean isHips = prop.getProperty("hips_service_url") != null;
      
      // Sans catégorie => dans la branche "Others" suivi du protocole puis de l'authority
      if( category == null ) {
         boolean isCS  = prop.getProperty("cs_service_url")   != null;
         boolean isSIA = prop.getProperty("sia_service_url")  != null 
                      || prop.getProperty("sia2_service_url") != null;
         boolean isSSA = prop.getProperty("ssa_service_url")  != null;
         boolean isTAP = prop.getProperty("tap_service_url")  != null;
         String subCat = isHips ? "HiPS"
               : isSIA ? "Image (by SIA)" : isSSA ? "Spectrum (by SSA)" : isCS ? "Catalog (by CS)" : isTAP ? "Table (by TAP)" : "Miscellaneous";

         category = DirectorySort.OTHERS+"/" + subCat + "/" + Util.getSubpath(id, 0, 1);
         prop.setProperty(Constante.KEY_CLIENT_CATEGORY, category);
      }

      boolean local = prop.getProperty("PROP_ORIGIN") != null;

      // Rangement dans la branche "Adds" si chargement local
      if( local && !category.startsWith(DirectorySort.ADDS+"/") ) {
         category = DirectorySort.ADDS+"/" + category;
         prop.replaceValue(Constante.KEY_CLIENT_CATEGORY, category);
      }
      
      // Génération de la clé de tri
      directorySort.setInternalSortKey(id,prop);
   }

   private void propAdjust1(String id, MyProperties prop) {
      
      String type = prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( type == null || type.indexOf("catalog") < 0 ) return;
      
      // Déjà fait en amont => on ne bidouille plus dans le client
      if( prop.get(Constante.KEY_CLIENT_CATEGORY)!=null ) return;

      String category = null;

      // Ne concerne pas VizieR => on ne bidouille pas dans le client
      if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) return;

      // Nettoyage des macros latex qui trainent
      cleanLatexMacro(prop);

      // Ajout de l'entrée TAP (si elle n'existe pas)
      if( prop.get("tap_service_url") == null ) {
         prop.replaceValue("tap_service_url", "http://tapvizier.u-strasbg.fr/TAPVizieR/tap/");
      }

      // Détermination de la catégorie
      String code = getCatCode(id);
      if( code == null ) return;

      String vizier = "/VizieR";

      boolean flagJournal = code.equals("J");
      if( flagJournal ) {
         String journal = getJournalCode(id);
         category = "Catalog" + vizier + "/Journal table/" + journal; 

      } else {
         int c = Util.indexInArrayOf(code, DirectorySort.CATCODE);
         if( c == -1 ) category = "Catalog" + vizier + "/" + code; // Catégorie inconnue
         else category = "Catalog" + vizier + "/" + DirectorySort.CATLIB[c];
      }

      // Détermination du suffixe (on ne va pas créer un folder pour un élément unique)
      String parent = getCatParent(id);
      boolean hasMultiple = hasMultiple(parent);
      if( !hasMultiple ) {
         parent = getCatParent(parent);

         if( !flagJournal ) {
            // Je vire le nom de la table a la fin du titre
            String titre = prop.get(Constante.KEY_OBS_TITLE);
            int i;
            boolean modif=false;
            if( titre != null && (i = titre.lastIndexOf('(')) > 0 ) {
               titre = titre.substring(0, i - 1);
               modif=true;
            }
            
            // J'ajoute le petit nom de la collection en suffixe si nécessaire
            String label = prop.getFirst("obs_collection_label");
            if( label!=null && titre!=null ) {
               titre = addLabelPrefix(label,titre);
               modif=true;
            }
            if( modif ) prop.replaceValue(Constante.KEY_OBS_TITLE, titre);
         }

      } else {
         String titre = prop.get(Constante.KEY_OBS_TITLE);
         if( titre != null ) {
            
            // Je remonte le nom du catalog sur la branche
            int i = titre.lastIndexOf('(');
            int j = titre.indexOf(')', i);
            if( i > 0 && j > i ) {
               int k = parent.lastIndexOf('/');
               parent = (k == -1 ? "" : parent.substring(0, k + 1)) + titre.substring(0, i - 1);

               String desc = prop.get(Constante.KEY_OBS_DESCRIPTION);
               if( desc != null ) {
                  String newTitre = desc + titre.substring(i - 1, j + 1);
                  prop.replaceValue(Constante.KEY_OBS_TITLE, newTitre);
               }
            }
         }
      }
      if( !flagJournal ) {
         String suffix = getCatSuffix(parent);
         
         // J'ajoute le petit nom de la collection en suffixe si
         // absent du titre du catalogue
         String label = prop.getFirst("obs_collection_label");
         if( label!=null && suffix!=null  ) {
            suffix=addLabelPrefix(label,suffix);
         }

         suffix = suffix == null ? "" : "/" +suffix;
         category += suffix;
      }

      prop.replaceValue(Constante.KEY_CLIENT_CATEGORY, category);
      prop.remove(Constante.KEY_OBS_DESCRIPTION);
   }
   
   static private boolean isSep(char c) { return c==' ' || c=='-' || c=='_'; }
   
   // Ajoute le label en préfixe si les premiers mots sont différents
   // Supprime éventuellement l'article The en début de titre
   private String addLabelPrefix(String label, String title) {
      if( label==null ) return title;
      
      String title1 = title;
      if( title.startsWith("The ") ) title = title.substring(4);
     
      char [] a1 = label.toCharArray();
      char [] a2 = title.toCharArray();
      
      for( int i=0, j=-1; i<a1.length; i++ ) {
         if( isSep( a1[i] ) ) continue;
         while( ++j<a2.length && isSep( a2[j] ) );
         if( j==a2.length || Character.toUpperCase( a1[i] ) != Character.toUpperCase( a2[j] ) ) {
            return label+" - "+title1;
         }
      }
      return title;
   }

   static private final String[] TEXTKEYS = { "obs_title", "obs_description", "obs_label", "obs_collection" };

   /** Nettoyage des macros Latex oublié par VizieR */
   private void cleanLatexMacro(MyProperties prop) {
      for( String key : TEXTKEYS ) {
         String s = prop.get(key);
         if( s == null ) continue;
         if( s.indexOf('\\') < 0 ) continue;
         s = cleanLatexMacro(s);
         prop.replaceValue(key, s);
      }
   }

   static final private int AVANT = 0, MACRO = 1, IN = 2, NEXT = 3, MACRO1 = 4, NAMEMACRO = 5, INMACRO = 6;

   /**
    * Suppression des macros Latex présent dans la chaine ex: texte avant \macro{contenu{ou plus}} texte avant...
    */
   private String cleanLatexMacro(String s) {
      StringBuilder s1 = new StringBuilder();
      StringBuilder macro = new StringBuilder();
      char[] a = s.toCharArray();
      int mode = 0;
      int level = 0;
      boolean keepIn = false;

      for( char c : a ) {
         switch( mode ) {
            case AVANT:
               if( c == '{' ) mode = MACRO1;
               else if( c == '\\' ) {
                  mode = MACRO;
                  macro.replace(0, macro.length(), "");
               } else s1.append(c);
               break;
            case MACRO:
               if( c == '\\' ) macro.replace(0, macro.length(), "");
               else if( c == '{' ) {
                  keepIn = macro.toString().equals("vFile") || macro.toString().equals("em") || macro.toString().equals("bf");
                  mode = IN;
               } else if( !Character.isJavaIdentifierPart(c) ) {
                  String s2 = resolveMacro(macro.toString());
                  if( s2.length() > 0 ) {
                     s1.append(s2);
                     s1.append(c);
                  } else {
                     int n = s1.length() - 1;
                     if( c == ')' && s1.charAt(n) == '(' ) s1.deleteCharAt(n);
                     else if( c == ']' && s1.charAt(n) == '[' ) s1.deleteCharAt(n);
                     else if( c == '}' && s1.charAt(n) == '{' ) s1.deleteCharAt(n);
                  }
                  mode = AVANT;
               } else macro.append(c);
               break;
            case IN:
               if( c == '}' && level == 0 ) mode = NEXT;
               else if( c == '{' ) level++;
               else if( c == '}' ) level--;
               else if( keepIn ) s1.append(c);
               break;
            case NEXT:
               if( c == '{' ) {
                  mode = IN;
                  keepIn = false;
               } else {
                  mode = AVANT;
                  int n = s1.length() - 1;
                  if( c == ')' && s1.charAt(n) == '(' ) s1.deleteCharAt(n);
                  else if( c == ']' && s1.charAt(n) == '[' ) s1.deleteCharAt(n);
                  else if( c == '}' && s1.charAt(n) == '{' ) s1.deleteCharAt(n);
                  else s1.append(c);
               }
               break;
            case MACRO1:
               if( c != '\\' ) {
                  s1.append('{');
                  s1.append(c);
                  mode = AVANT;
               } // Fausse alerte
               else mode = NAMEMACRO;
               break;
            case NAMEMACRO:
               if( c == '\\' ) break;
               if( !Character.isJavaIdentifierPart(c) ) mode = INMACRO;
               break;
            case INMACRO:
               if( c == '}' ) mode = AVANT;
               else s1.append(c);
               break;
         }
      }
      // System.out.println("\nAVANT: "+s+"\nAPRES: "+s1.toString());
      return s1.toString();
   }

   static final private String MACRO_LIST[] = { "deg", "originalcolumnnames" };

   static final private String MACRO_CONV[] = { "°", "" };

   private String resolveMacro(String macro) {
      int i = Util.indexInArrayOf(macro, MACRO_LIST);
      if( i < 0 ) return macro;
      return MACRO_CONV[i];
   }

   // Mémorise les paths de l'arbre qui ont de multiples feuilles
   private HashSet<String> multiple = null;

   /** Retourne true si le path contient plusieurs feuilles */
   private boolean hasMultiple(String path) {
      if( multiple == null ) {
         multiple = new HashSet<String>(multiProp.size());
         HashSet<String> un = new HashSet<String>(multiProp.size());
         for( MocItem mi : multiProp ) {
            String parent = getCatParent(mi.mocId);
            if( un.contains(parent) ) multiple.add(parent);
            else un.add(parent);
         }
      }
      return multiple.contains(path);
   }

   /**
    * Provides the properties java object describing the collection identified by id
    * @param id Identifier (ex: CDS/Simbad)
    * @return The properties (very similar to basic java Properties object)
    */
   protected MyProperties getProp(String id) {
      return multiProp.getProperties(id);
   }
   
   /** Retourne les properties associées à l'id (venant du stack) spécifié */
   protected MyProperties getProperties(String id) {
      if( id==null ) return null;
      int i = id.indexOf('~');
      if( i < 0 ) i = id.indexOf(' ');
      if( i > 0 ) id = id.substring(0, i);
      return getProp(id);
   }

   /**
    * Provides the list of pre-selected TAP servers Output syntax: ID url description...
    * @throws Exception
    */
   protected ArrayList<String> getPredefinedTAPServers() throws Exception {
      return getTAPServersByMocServer("client_tap_mainlist=*");
   }
   
   //returns multiPropId
   protected ArrayList<String> getPredefinedTAPServersMultiProp() throws Exception {
	      return getTAPServersMultiPropByMocServer("client_tap_mainlist=*");
   }

   /**
    * provides the list of Tap servers matching the keyword(s) - blank separated (AND logic, applied on ID, obs_title et
    * obs_collection Output syntax: ID url description...
    * @throws Exception
    */
   protected ArrayList<String> geTAPServers(String keyword) throws Exception {
      if( keyword == null || keyword.trim().length() == 0 ) return getPredefinedTAPServers();
      return getTAPServersByMocServer("(" + getKeyWordExpr(keyword) + ")");
   }

   // For tap server list, slight change
   protected ArrayList<String> getTAPServers(String keyword) throws Exception {
      if( keyword == null || keyword.trim().length() == 0 ) return getTAPServersByMocServer("(*)");
      return getTAPServersMultiPropByMocServer("(" + getKeyWordExpr(keyword) + ")");
   }

   /**
    * Provides the list of TAP server matching the MocServer query Output syntax: ID url description...
    * @param query
    * @return
    * @throws Exception
    */
   protected ArrayList<String> getTAPServersByMocServer(String query) throws Exception {
      ArrayList<String> b = new ArrayList<String>();
      ArrayList<String> a = multiProp.scan((HealpixMoc) null, "tap_service_url*=* && " + query, false, -1, -1);
      for( String id : a ) {
         // String auth = Util.getSubpath(id, 0);
         MocItem mi = multiProp.getItem(id);
         String url = mi.prop.get("tap_service_url");
         String desc = mi.prop.get("obs_title");
         if( desc == null ) desc = mi.prop.get("obs_collection");
         String s = id + "  " + url + " " + desc;
         b.add(s);
      }
      return b;
   }
   
 //returns multiPropId
	protected ArrayList<String> getTAPServersMultiPropByMocServer(String query) throws Exception {
		ArrayList<String> a = multiProp.scan((HealpixMoc) null, "tap_service_url*=* && " + query, false, -1, -1);
		return a;
	}

   // protected ArrayList<String> getBigTAPServers(int limitNbCat) throws Exception {
   //
   // ArrayList<String> a = multiProp.scan( (HealpixMoc)null, "tap_service_url*=*", false, -1, -1);
   //
   // Map<String, Integer> map = new HashMap<String, Integer>();
   // for( String id : a) {
   //
   // // Cas particulier à écarter pour ne pas poser souci avec les catalogues VizieR
   // if( id.startsWith("CDS/Simbad") ) continue;
   //
   // String auth = Util.getSubpath(id, 0);
   // int m;
   // Integer n = map.get(auth);
   // if( n==null ) m=0;
   // else m = n;
   // m++;
   // map.put(auth,m);
   // }
   //
   // // On trie
   // Map<String, Integer> map1 = DirectoryFilter.sortByValues(map, 1);
   //
   // // On prend uniquement les serveurs qui ont au moins limitNbCat collections
   // ArrayList<String> b = new ArrayList<String>();
   // for( String auth : map1.keySet() ) {
   //
   // Integer n = map.get(auth);
   // if( n<limitNbCat ) break;
   //
   // // On cherche la première entrée correspondante pour récupérer une URL TAP
   // for( String id : a ) {
   // String auth1 = Util.getSubpath(id, 0);
   // if( !auth1.equals(auth) ) continue;
   //
   // MocItem mi = multiProp.getItem(id);
   // String url = mi.prop.get("tap_service_url");
   //
   // // On mémorise pour le tableau résultat.
   // if( auth.equals("CDS") ) auth="cds.vizier";
   // String s = auth+" "+url+" "+n+" collections";
   // b.add(s);
   // break;
   // }
   // }
   //
   // return b;
   // }
   //

   /** Retourne le code de la catégorie des catalogues, null sinon (ex: CDS/I/246/out => I) */
   static protected String getCatCode(String id) {
      return Util.getSubpath(id, 1);
   }

   /** retourne l'abbréviation du journal (ex: CDS/J/A+A/171/261/table1 => A+A) */
   static protected String getJournalCode(String id) {
      return Util.getSubpath(id, 2);
   }

   /** retourne numéro du journal (ex: CDS/J/A+A/171/261/table1 => 171/261 ) */
   static protected String getJournalNum(String id) {
      return Util.getSubpath(id, 3,2);
   }

   /**
    * Retourne le suffixe de l'identificateur d'un catalogue => tous ce qui suit le code de catégorie (ex: CDS/I/246/out =>
    * 246/out)
    */
   static protected String getCatSuffix(String id) {
      return Util.getSubpath(id, 2, 2);
   }

   /**
    * Retourne le préfixe parent d'un identificateur de catalgoue => tout ce qui précède le dernier mot (ex: CDS/I/246/out =>
    * CDS/I/246)
    */
   static protected String getCatParent(String id) {
      int i = id.lastIndexOf('/');
      if( i > 0 ) return id.substring(0, i);
      return null;
   }
   
   // Sauvegarde dans le cache du MultiMoc sous forme binaire
   private boolean cacheWrite() {
      try {
         String s = aladin.cache.getCacheDir() + Util.FS + MMOC;
         (new BinaryDump()).save(multiProp, s);
         aladin.trace(3, "Multiprop stored in cache [" + s + "]");
      } catch( Exception e ) {
         if( aladin.levelTrace >= 3 ) e.printStackTrace();
         return false;
      }
      return true;
   }

   // Génération du MultiMoc depuis la sauvegarde binaire du cache
   private boolean cacheRead() {
      try {
         long t0 = System.currentTimeMillis();
         String s = aladin.cache.getCacheDir() + Util.FS + MMOC;
         MultiMoc multiProp = (new BinaryDump()).load(s);
         this.multiProp = new MultiMoc2(multiProp);
         aladin.trace(3, "Multiprop loaded (" + multiProp.size() + " rec.) from cache [" + s + "] in "
               + (System.currentTimeMillis() - t0) + "ms...");
      } catch( Exception e ) {
         return false;
      }
      return true;
   }

   /**
    * Retourne la date d'estampillage la plus tardive de l'ensemble des enregistrements du multiProp. 0 si aucun
    */
   private long getMultiPropTimeStamp() {
      long max = 0;
      for( MocItem mi : multiProp ) {
         long ts = mi.getPropTimeStamp();
         if( ts > max ) max = ts;
      }
      return max;
   }

   // Retourne la date du cache
   private long getCacheTimeStamp() {
      String s = aladin.cache.getCacheDir() + Util.FS + MMOC;
      return (new File(s)).lastModified();
   }

   /** Retourne true si le HipsStore est prêt */
   protected boolean dialogOk() {
      return !init /* !mocServerLoading && multiProp.size()>0 */;
   }

   /** Chargement des descriptions de l'arbre par le MocServer */
   private void initMultiProp(final boolean flagReload) {

      // Tentative de rechargement depuis le cache
      if( cacheRead() ) {
         startTimer();
         while( !init )
            Util.pause(100);
         (new Thread("updateFromMocServer") {
            public void run() {
               try {
                  quickFilter.setEditable(false);
                  quickFilter.setText(UPDATING);
                  if( updateFromMocServer(flagReload) > 0 ) {
                     cacheWrite();
                     final ArrayList<TreeObjDir> tmpListReg = populateMultiProp();
                     SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                           replaceTree(tmpListReg);
                           stopTimer();
                        }
                     });
                  } else stopTimer();
               } finally {
                  quickFilter.setText("");
                  quickFilter.setEditable(true);
               }
            }
         }).start();

         // Le cache est vide => il faut charger depuis le MocServer
      } else {

         // L'initialisation se fait en deux temps pour pouvoir laisser
         // l'utilisateur travailler plus rapidement
         String s = "client_application=AladinDesktop" + (!Aladin.PROTO ? "*" : "")
               + "&hips_service_url=*&fields=!obs_description,!hipsgen*&fmt=gzip-ascii&get=record";
         loadFromMocServer(s);

         startTimer();
         while( !init )
            Util.pause(100);
         (new Thread("updateFromMocServer") {
            public void run() {
               if( loadFromMocServer(MOCSERVER_INIT) > 0 ) {
                  cacheWrite();
                  final ArrayList<TreeObjDir> tmpListReg = populateMultiProp();
                  SwingUtilities.invokeLater(new Runnable() {
                     public void run() {
                        replaceTree(tmpListReg);
                        stopTimer();
                     }
                  });
               } else stopTimer();
            }
         }).start();
      }

      this.dirList = populateMultiProp();
   }

   /** Ajout de nouvelles collections */
   protected boolean addHipsProp(InputStreamReader in, boolean addition, String path) {
      try {
         // System.out.println("addHipsProp => "+path);
         loadMultiProp(in, addition, path);
         ArrayList<TreeObjDir> tmpDirList = populateMultiProp();
         rebuildTree(tmpDirList, false, true);
         dirList = tmpDirList;
      } catch( Exception e ) {
         if( aladin.levelTrace > 3 ) e.printStackTrace();
         return false;
      }
      return true;
   }

   /**
    * Demande de maj des enr. via le MocServer en passant en POST la liste des IDs des collections que l'on connait déjà + une
    * date d'estampillage pour chacune d'elles. Si ça ne fonctionne pas, on tentera une maj plus basique
    * @return le nombre de records chargés
    */
   private int updateFromMocServer(boolean flagReload) {
      long t0 = System.currentTimeMillis();
      URL url;
      try {
         url = aladin.glu.getURL("MocServer");
      } catch( Exception e ) {
         if( aladin.levelTrace >= 3 ) e.printStackTrace();
         return -1;
      }

      try {
         mocServerUpdating = true;
         MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
         String boundary = MultiPartPostOutputStream.createBoundary();
         URLConnection urlConn = MultiPartPostOutputStream.createConnection(url);
         urlConn.setRequestProperty("Accept", "*/*");
         urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
         // set some other request headers...
         urlConn.setRequestProperty("Connection", "Keep-Alive");
         urlConn.setRequestProperty("Cache-Control", "no-cache");
         MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

         File tmpMoc = File.createTempFile("moc", ".txt");
         tmpMoc.deleteOnExit();

         BufferedWriter fo = new BufferedWriter(new FileWriter(tmpMoc));
         try {
            for( MocItem mi : this ) {
               String s = mi.mocId + "=" + (flagReload ? "0":mi.getPropTimeStamp()) + "\n";
               fo.write(s.toCharArray());
            }
         } finally {
            try {
               fo.close();
            } catch( Exception e ) {
            }
         }

         out.writeField("fmt", "asciic");
         out.writeFile("maj", null, tmpMoc, true);
         out.close();
         aladin.trace(4, "ID list sent");

         int n = loadMultiProp(new InputStreamReader(urlConn.getInputStream()), false, null);
         Aladin.trace(3,
               "Multiprop updated in " + (System.currentTimeMillis() - t0) + "ms => " + n + " record" + (n > 1 ? "s" : ""));
         return n;

      } catch( Exception e ) {
         if( aladin.levelTrace >= 3 ) e.printStackTrace();
         System.err.println("An error occured while updating the MocServer service => try the basic mode...");
         return updateFromMocServerBasic();
      } finally {
         mocServerUpdating = false;
      }
   }

   // /** Gunzippe le flux si nécessaire */
   // private InputStream gunzipIfRequired(InputStream in) throws Exception {
   // PushbackInputStream pb = new PushbackInputStream( in, 2 );
   // byte[] signature = new byte[2];
   // pb.read( signature );
   // pb.unread( signature );
   // int head = ( signature[0] & 0xff ) | ( ( signature[1] << 8 ) & 0xff00 );
   // boolean isGziped = GZIPInputStream.GZIP_MAGIC == head;
   // if( !isGziped ) return pb;
   // return new GZIPInputStream(pb);
   // }

   /**
    * Demande de maj des enr. via le MocServer en fonction d'une seule date spécifique. La date d'estampillage de référence sera
    * la plus tardive de l'ensemble des enr. déjà copiés. Et si aucun, ce sera la date du fichier de cache (avec un risque de
    * décalage des horloges entre le MocServer et la machine locale. Cette méthode de maj ne permet pas la suppression des rec
    * obsolètes, ni le complément de maj si la précédente maj a été interrompue en cours.
    * @return le nombre de records chargés
    */
   private int updateFromMocServerBasic() {
      long ts = getMultiPropTimeStamp();
      if( ts == 0L ) ts = getCacheTimeStamp();
      return loadFromMocServer("TIMESTAMP=>" + ts + "&fmt=asciic&get=record");
   }

   private int loadFromMocServer(String params) {
      InputStreamReader in = null;
      boolean eof = false;
      int n = 0;

      String text = params.indexOf("TIMESTAMP") >= 0 ? "updat" : "load";

      // Recherche sur le site principal, et sinon sur un site miroir
      try {
         mocServerLoading = true;

         long t0 = System.currentTimeMillis();
         Aladin.trace(3, text + "ing Multiprop definitions from MocServer...");

         String u = aladin.glu.getURL("MocServer", params, true).toString();
         try {
            in = new InputStreamReader(Util.openStream(u, false, 3000));
            if( in == null ) throw new Exception("cache openStream error");

            // Peut être un site esclave actif ?
         } catch( EOFException e1 ) {
            eof = true;
         } catch( Exception e ) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            try {
               in = new InputStreamReader(Util.openStream(u, false, -1));
            } catch( EOFException e1 ) {
               eof = true;
            }
         }

         if( !eof ) n = loadMultiProp(in, false, null);
         Aladin.trace(3, "Multiprop " + text + "ed in " + (System.currentTimeMillis() - t0) + "ms => " + n + " record"
               + (n > 1 ? "s" : ""));

      } catch( Exception e1 ) {
         if( Aladin.levelTrace >= 3 ) e1.printStackTrace();
         return -1;

      } finally {
         mocServerLoading = false;
         if( in != null ) {
            try {
               in.close();
            } catch( Exception e ) {
            }
         }
      }
      return n;
   }
   
   private String getId(String ivoid) {
      if( ivoid.startsWith("ivo://") ) return ivoid.substring(6);
      return ivoid;
   }

   /** Retourne le nombre max du nombre de lignes des catalogues et tables */
   protected int getNbRowMax() {
      long max = 0L;
      for( MocItem mi : this ) {
         try {
            long n = Long.parseLong(mi.prop.get("nb_rows"));
            if( n > max ) max = n;
         } catch( Exception e ) {
         }
      }
      return (int) (max);
   }

   /** Retourne le nombre d'items matchant la contrainte */
   protected int getNumber(String contrainte) {
      try {
         return multiProp.scan(contrainte).size();
      } catch( Exception e ) {
      }
      return -1;
   }

   /** true si on peut raisonnablement faire un updating des HiPS visibles dans la vue */
   private boolean isReadyForUpdating() {
      return !mocServerLoading && isVisible() && getWidth() > 0;
   }

   protected boolean blinkState = true;

   /** Réaffichage du Panel tous les demi-secondes afin de faire clignoter le message d'attente */
   private void startTimer() {
      if( timer == null ) {
         timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               blinkState = !blinkState;
               repaint();
            }
         });
      }
      if( !timer.isRunning() ) {
         // System.out.println("Timer started");
         timer.start();
      }
   }

   private void stopTimer() {
      if( timer != null && timer.isRunning() ) {
         // System.out.println("Timer stopped");
         timer.stop();
      }
   }

   /************** Pour faire la maj en continue des HiPS visibles dans la vue *******************************/

   transient private Thread threadUpdater = null;

   private boolean encore = true;

   private void startInsideUpdater() {
      if( threadUpdater == null ) {
         threadUpdater = new Updater("RegUpdater");
         threadUpdater.start();
      } else encore = true;
   }

   private void stopInsideUpdater() {
      encore = false;
   }

   class Updater extends Thread {
      public Updater(String s) {
         super(s);
      }

      public void run() {
         encore = true;
         // System.out.println("Registry Tree updater running");
         while( encore ) {
            try {
               // System.out.println("Hips updater checking...");
               if( isReadyForUpdating() ) resumeIn();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) {
            }
         }
         // System.out.println("Registry Tree updater stopped");
         threadUpdater = null;
      }
   }

   /****************** Gestion des paramètres des différents modes d'interrogation *************************/

   private HashMap<String, String> params = null;

   private void initParams() {
      TreeObjDir.loadString(aladin.chaine);
      params = TreeObjDir.paramsFactory();
      directorySort = new DirectorySort(aladin);
   }

   protected void setParam(String key, String val) {
      params.put(key, val);
   }

   protected String getParam(String key) {
      return params.get(key);
   }

   /** Remet à jour les différents widgets du FrameInfo (s'il est visible) */
   protected void resumeFrameInfo() {
      if( frameInfo == null || !frameInfo.isVisible() ) return;
      frameInfo.resumePanel();
      frameInfo.updateWidget();
   }
   
   /********* Classe gérant une fenêtre d'information associée au HiPS cliqué dans l'arbre des HiPS ***************/

   private class FrameInfo extends JFrame implements ActionListener {

      ArrayList<TreeObjDir> treeObjs = null; // hips dont il faut afficher les informations

      JPanel panelInfo = null; // le panel qui contient les infos (sera remplacé à chaque nouveau hips)

      JCheckBox hipsBx = null, mocBx = null, mociBx = null, mocuBx, progBx = null, dmBx = null, siaBx = null, ssaBx = null,
            customBx = null, csBx = null, msBx = null, allBx = null, tapBx = null, xmatchBx = null, globalBx = null, liveBx = null;
      ConeField target = null;
      JPanel targetPanel = null;
      JButton load = null, grab = null;

      FrameInfo(boolean decorated) {
         
         Aladin.setIcon(this);
         setTitle(AWFRAMEINFOTITLE);
         enableEvents(AWTEvent.WINDOW_EVENT_MASK);
         Util.setCloseShortcut(this, false, aladin);
         
         JPanel contentPane = (JPanel) getContentPane();
         contentPane.setLayout(new BorderLayout(5, 5));
         contentPane.setBackground(new Color(240, 240, 250));
         contentPane.setBorder(BorderFactory.createLineBorder(Color.black));
         setUndecorated(!decorated);
         pack();
      }

      public void setVisible(boolean flag) {

         // Si on efface la fenêtre popup, on resete aussi la mémorisation
         // de la liste des TreeObj concerné par le dernier affichage
         if( !flag ) {
            treeObjs = null;
            setTarget(null);
         } else updateWidget();

         super.setVisible(flag);
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         updateWidget();
      }
      
      String sTarget=null;
      String sRadius=null;
      
      protected void setTarget(String s) { sTarget = s; }
      protected void setRadius(String s) { sRadius = s; }

      private void updateWidget() {
         if( load == null ) return;
         boolean activateLoad = false;
         boolean activateTarget = false;
         if( hipsBx != null )   activateLoad |= hipsBx.isSelected();
         if( mocBx != null )    activateLoad |= mocBx.isSelected();
         if( mociBx != null )   activateLoad |= mociBx.isSelected();
         if( mocuBx != null )   activateLoad |= mocuBx.isSelected();
         if( progBx != null )   activateLoad |= progBx.isSelected();
         if( dmBx != null )     activateLoad |= dmBx.isSelected();
         if( siaBx != null )  { activateLoad |= siaBx.isSelected();  activateTarget |= siaBx.isSelected(); }
         if( ssaBx != null )  { activateLoad |= ssaBx.isSelected();  activateTarget |= ssaBx.isSelected(); }
         if( csBx != null )   { activateLoad |= csBx.isSelected();   activateTarget |= csBx.isSelected(); }
         if( liveBx != null ) { activateLoad |= liveBx.isSelected(); activateTarget |= liveBx.isSelected(); }
         if( customBx != null ) activateLoad |= customBx.isSelected();
         if( msBx != null )     activateLoad |= msBx.isSelected();
         if( allBx != null )    activateLoad |= allBx.isSelected();
         if( tapBx != null )    activateLoad |= tapBx.isSelected();
         if( xmatchBx != null ) activateLoad |= xmatchBx.isSelected();
         if( globalBx != null ) activateLoad |= globalBx.isSelected();
         load.setEnabled(activateLoad);
         
         if( activateTarget ) {
            if( sTarget!=null && sTarget.trim().length()==0  ) sTarget=null;
            String s = sTarget==null?"":sTarget;
            if( s.length()>0 && sRadius!=null && sRadius.trim().length()>0 ) s=s+"   "+sRadius;
            target.setText(s);
            int n = targetPanel.getComponentCount();
            if( n<2 ) {
               targetPanel.removeAll();
               targetPanel.add( target );
               if( Projection.isOk( aladin.view.getCurrentView().getProj()) ) targetPanel.add( grab );
               validate();
            }
            String info = getDefaultCone();
            target.setInfo(info==null ? "Object or coordinates ?":info);
         } else {
            if( targetPanel!=null && targetPanel.getComponentCount()>0 ) {
               targetPanel.removeAll();
               validate();
            }
         }
         
         // Changement de nom et de tooltip sur les checkbox "in view"
         if( target!=null && targetPanel!=null ) {
            boolean inView = (targetPanel.getComponentCount()==0 || target.getText().trim().length()==0) && getDefaultCone()!=null;
            for( JCheckBox cb : new JCheckBox[]{ csBx, siaBx, ssaBx }) {
               if( cb==null ) continue;
               cb.setText( inView  ? AWCSLAB : AWCONE );
               Util.toolTip(cb, inView ? AWCSTIP : AWCONETIP);
            }
         }
      }
      
      protected String getDefaultCone() {
         Coord coo;
         if( aladin.view.isFree() || !Projection.isOk( aladin.view.getCurrentView().getProj()) ) {
            return null;
         }
         String target;
         try {
            coo = aladin.view.getCurrentView().getCooCentre();
            target = aladin.localisation.aladin.localisation.getFrameCoord( coo.getDeg() );
         } catch( Exception e ) {
            e.printStackTrace();
            return null;
         }
         
         double radius = aladin.view.getCurrentView().getTaille();
         return target+"   "+Coord.getUnit( radius );
      }
      
      boolean isSame(ArrayList<TreeObjDir> a1, ArrayList<TreeObjDir> a2) {
         if( a1 == null && a2 == null ) return true;
         if( a1 == null || a2 == null ) return false;
         if( a1.size() != a2.size() ) return false;
         for( TreeObjDir ti : a1 ) {
            if( !a2.contains(ti) ) return false;
         }
         return true;
      }

      /** Positionne les collections concernées, et regénère le panel en fonction */
      boolean setCollections(ArrayList<TreeObjDir> tos) {
         if( isSame(tos, treeObjs) ) {
            toFront();
            return false;
         }
         this.treeObjs = tos;
         resumePanel();
         validate();
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               pack();
               toFront();
               repaint();
            }
         });
         return true;
      }

      // Le Panel de la fenêtre info qui gère localement une croix de fermeture
      class MyInfoPanel extends JPanel implements MouseListener, MouseMotionListener {
         static private final int W = 6;

         Rectangle cross = null;

         boolean inCross;

         MyInfoPanel() {
            super();
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 7));
            addMouseListener(this);
            addMouseMotionListener(this);
         }

         public void paint(Graphics g) {
            super.paint(g);
            if( !decorated ) drawCross(g, getWidth() - W - 6, 4);
         }

         private void drawCross(Graphics g, int x, int y) {
            g.setColor(inCross ? Color.red : Color.gray);
            g.drawLine(x, y, x + W, y + W);
            g.drawLine(x + 1, y, x + W + 1, y + W);
            g.drawLine(x + 2, y, x + W + 2, y + W);
            g.drawLine(x + W, y, x, y + W);
            g.drawLine(x + W + 1, y, x + 1, y + W);
            g.drawLine(x + W + 2, y, x + 2, y + W);
            cross = new Rectangle(x, y - 2, W + 2, W + 2);
         }


         public void mousePressed(MouseEvent e) {
            if( inCross ) hideInfo();
         }

         public void mouseMoved(MouseEvent e) {
            if( cross!=null ) {
               boolean in1 = cross.contains(e.getPoint());
               if( in1 != inCross ) repaint();
               inCross = in1;
            }
         }
         
         public void mouseClicked(MouseEvent e) { }
         public void mouseReleased(MouseEvent e) { }
         public void mouseEntered(MouseEvent e) { }
         public void mouseExited(MouseEvent e) { }
         public void mouseDragged(MouseEvent e) { }
      }
      
      private void epingle() { reshowInfo( !decorated ); }

      /** Reconstruit le panel des informations en fonction des collections courantes */
      void resumePanel() {
         JPanel contentPane = (JPanel) getContentPane();
         if( panelInfo != null ) contentPane.remove(panelInfo);

         panelInfo = new MyInfoPanel();

         String s;
         boolean flagScan = false;
         long nbRows = -1;
         MyAnchor a;
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g = new GridBagLayout();
         c.fill = GridBagConstraints.BOTH;
         c.insets = new Insets(0, 1, 0, 5);
         JPanel p = new JPanel(g);

         TreeObjDir to = null;
         boolean hasView = !aladin.view.isFree();
//         boolean hasSmallView = hasView && aladin.view.getCurrentView().getTaille() < 30;
         boolean hasMocPol = aladin.view.hasMocPolSelected();
         boolean hasRegion = aladin.calque.getNbPlanMoc() > 0 || hasMocPol;
         boolean hasLoadedCat = aladin.calque.getNbPlanCat() > 0;

         NoneSelectedButtonGroup bg = new NoneSelectedButtonGroup();

         // En cas de sélection multiples de collections
         if( treeObjs.size() > 1 ) {
            StringBuilder list = null;
            String sList = null, more = null;
            boolean hasCS = false;
            boolean hasSIA = false;
            boolean hasSSA = false;
            boolean hasCDScat = false;
            boolean hasMoc = false;
            boolean hasHips = false;
            boolean hasGlobalAccess = false;
            int nbIn = 0;
            int nbInMayBe = 0;
            int nbInHips = 0;

            for( TreeObjDir to1 : treeObjs ) {
               if( list == null ) list = new StringBuilder(to1.internalId);
               else list.append(", " + to1.internalId);
               if( sList == null && list.length() > 80 ) sList = list + "...";
               if( !hasCS && to1.hasCS() ) hasCS = true;
               if( !hasSIA && to1.hasSIA() ) hasSIA = true;
               if( !hasSSA && to1.hasSSA() ) hasSSA = true;
               if( !hasMoc && to1.hasMoc() ) hasMoc = true;
               if( !hasHips && to1.hasHips() ) hasHips = true;
               if( !hasGlobalAccess && to1.hasGlobalAccess() ) hasGlobalAccess = true;
               if( !hasCDScat && to1.isCDSCatalog() ) hasCDScat = true;
               if( !flagScan && !to1.hasMoc() ) flagScan = true;
               int inFlag = to1.getIsIn();
               if( inFlag == 1 ) nbIn++;
               else if( inFlag == -1 ) nbInMayBe++;
               if( inFlag != 0 && to1.hasHips() ) nbInHips++;
            }

            a = new MyAnchor(aladin, MULTICOL + ": " + treeObjs.size(), 50, null, null);
            a.setFont(a.getFont().deriveFont(Font.BOLD));
            a.setFont(a.getFont().deriveFont(a.getFont().getSize2D() + 1));
            a.setForeground(Aladin.COLOR_GREEN);
            PropPanel.addCouple(p, null, a, g, c);

            if( sList != null ) more = list.toString();
            else sList = list.toString();

            JPanel mocAndMore = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            JCheckBox bx;
            mociBx = mocBx = csBx = liveBx = customBx = null;

            // S'il n'y a pas trop de collections, on pourra les appeler en parallèle
            boolean flagTooMany = false;
            if( treeObjs.size() > MAX_PARALLEL_QUERY ) flagTooMany = true;
            else {

               // On utilise le checkbox CS pour cumuler à la fois les accès CS,SIA et SSA
               if( hasCS || hasSIA || hasSSA || hasCDScat || hasGlobalAccess ) {
                  csBx = bx = new JCheckBox(AWCSLAB);
                  bx.addActionListener(this);
                  mocAndMore.add(bx);
                  String info = nbIn + nbInMayBe == 0 ? "(no data in the field)"
                        : "(" + (nbIn + nbInMayBe) + " collections " + (nbInMayBe > 0 ? " may " : "should")
                              + " have data in the field)";
                  Util.toolTip(bx, Util.fold(AWMCSTIP + "\n" + info, 100, true));
//                  bx.setEnabled(hasSmallView && Projection.isOk(aladin.view.getCurrentView().getProj()) && nbIn + nbInMayBe > 0);
//                  bx.setSelected(hasSmallView && Projection.isOk(aladin.view.getCurrentView().getProj()) && bx.isEnabled());
                  bx.setEnabled(true);
                  bx.setSelected(true);
                  bg.add(bx);
               }

               if( hasHips ) {
                  hipsBx = bx = new JCheckBox(AWPROGACC);
                  bx.addActionListener(this);
                  mocAndMore.add(bx);
                  if( csBx == null || !csBx.isSelected() ) bx.setSelected(true);
                  String info = "(" + nbInHips + " coll. have data in the field)";
                  Util.toolTip(bx, Util.fold(AWPROGACCTIP + "\n" + info, 100, true));
               }

               if( hasCDScat && hasRegion ) {
                  msBx = bx = new JCheckBox(AWMOCQLAB);
                  bx.addActionListener(this);
                  mocAndMore.add(bx);
                  Util.toolTip(bx, AWMOCQLABTIP);
                  bx.setEnabled(hasRegion);
                  bg.add(bx);

               }

               if( (csBx != null || hipsBx != null || msBx != null || customBx != null ) && (hasMoc) ) {
                  JLabel labelPlus = new JLabel(" + ");
                  mocAndMore.add(labelPlus);
               }
            }

            if( hasMoc ) {
               JLabel labelMoc = new JLabel(AWMOCTITLE);
               labelMoc.setFont(labelMoc.getFont().deriveFont(Font.ITALIC));
               labelMoc.setForeground(Color.gray);
               mocAndMore.add(labelMoc);

               if( !flagTooMany ) {
                  mocBx = bx = new JCheckBox(AWMOC1);
                  bx.addActionListener(this);
                  mocAndMore.add(bx);
                  Util.toolTip(bx, AWMOC1TIP);
               }

               mocuBx = bx = new JCheckBox(AWMOC2);
               bx.addActionListener(this);
               mocAndMore.add(bx);
               Util.toolTip(bx, AWMOC2TIP);

               mociBx = bx = new JCheckBox(AWMOC3);
               bx.addActionListener(this);
               mocAndMore.add(bx);
               Util.toolTip(bx, AWMOC3TIP);
            }

            c.insets.top = 1;
            a = new MyAnchor(aladin, sList, 100, more, null);
            a.setForeground(Aladin.COLOR_BLUE);
            PropPanel.addCouple(p, null, a, g, c);

            if( mocAndMore.getComponentCount() > 0 ) {

               JLabel label1 = new JLabel("Access mode");
               String tip = "Access protocols available for this collection. According to your current "
                     + "view and/or the selected stack planes, some access protocols may or may " + "not be activated.";
               Util.toolTip(label1, Util.fold(tip, 100, true));
               label1.setForeground(Color.gray.brighter());
               label1.setFont(label1.getFont().deriveFont(Font.ITALIC));

               GridBagConstraints c1 = new GridBagConstraints();
               GridBagLayout g1 = new GridBagLayout();
               c1.fill = GridBagConstraints.BOTH; // J'agrandirai les composantes
               c1.insets = new Insets(0, 0, 0, 0);
               JPanel loadPanel = new JPanel(g1);

               PropPanel.addFilet(p, g, c, 5, 2);
               PropPanel.addCouple(null, loadPanel, flagTooMany ? null : label1, null, mocAndMore, g1, c1,
                     GridBagConstraints.CENTER);
               PropPanel.addCouple(p, "", loadPanel, g, c);

            }

            // Une seule collection
         } else {

            to = treeObjs.get(0);

            if( to.verboseDescr != null || to.description != null ) {
               s = to.verboseDescr == null ? null : to.verboseDescr;
               String s1 = to.description != null && to.description.length() > 60 ? (to.description.substring(0, 58) + "...")
                     : to.description;
               a = new MyAnchor(aladin, s1, 50, s, null);
               a.setFont(a.getFont().deriveFont(Font.BOLD));
               a.setFont(a.getFont().deriveFont(a.getFont().getSize2D() + 1));
               a.setForeground(Aladin.COLOR_GREEN);
               PropPanel.addCouple(p, null, a, g, c);
            }

            String provenance = to.getProperty("prov_progenitor");
            String copyright = to.copyright == null ? to.copyrightUrl : to.copyright;
            if( provenance != null || copyright != null ) {
               String url=null;
               s = null;
               
               if( provenance!=null && provenance.startsWith("http") ) { url = provenance; provenance=null; }
               else url=to.copyrightUrl;
               if( url==null ) url = to.getProperty("prov_progenitor_url");
               
               s = provenance;
               if( provenance!=null ) s=provenance;
               if( s==null && copyright!=null ) s=copyright;
               if( s!=null && s.startsWith("http") ) {
                  if( url==null ) url=s;
                  s=null;
               }
               if( s==null ) s=Util.getSubpath(to.id, 0);
               s = "Provenance: "+s+" ";
               
               a = new MyAnchor(aladin, s, 50, url, null);
               a.setForeground(Color.gray);
               PropPanel.addCouple(p, null, a, g, c);
            }

            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
//            s = to.getProperty(Constante.KEY_MOC_SKY_FRACTION);
            s = to.getCoverage();
            if( s != null ) {
               boolean isIn = to.getIsIn() == 1;
//               try {
//                  s = Util.myRound(Double.parseDouble(s) * 100);
//               } catch( Exception e ) {
//               }
//               s = AWSKYCOV + " " + s + "% ";
               s = AWSKYCOV + " " + s + " ";
                             a = new MyAnchor(aladin, s, 50, null, null);
               a.setForeground(Color.gray);
               p1.add(a);

               if( hasView ) {
                  a.setToolTipText(isIn ? "Data available in the current view" : "No data in the current view");
               }

            } else {
               s = AWMOCUNK;
               a = new MyAnchor(aladin, s, 50, null, null);
               a.setForeground(Aladin.ORANGE);
               p1.add(a);

            }
            s = to.getEnergy();
            if( s != null ) {
               s = "    "  + s + " ";
               a = new MyAnchor(aladin, s, 50, null, null);
               a.setForeground(Color.gray);
               p1.add(a);
            }
            
            s = to.getPeriod();
            if( s!=null ) {
               s = "    "  + s + " ";
               a = new MyAnchor(aladin, s, 50, null, null);
               a.setForeground(Color.gray);
               p1.add(a);

            } else {
               s = to.getProperty("bib_year");
               if( s != null ) {
                  s = "    " + AWREFPUB + " " + s + " ";
                  a = new MyAnchor(aladin, s, 50, null, null);
                  a.setForeground(Color.gray);
                  p1.add(a);
               }
            }

          s = to.getProperty(Constante.KEY_HIPS_PIXEL_SCALE);
          if( s != null ) {
             try {
                s = Coord.getUnit(Double.parseDouble(s));
             } catch( Exception e ) {
             }
             s = "    " + AWHIPSRES + " " + s + " ";
             a = new MyAnchor(aladin, s, 50, null, null);
             a.setForeground(Color.gray);
             p1.add(a);
          }
          s = to.getProperty(Constante.KEY_NB_ROWS);
          if( s != null ) {
             try {
                nbRows = Long.parseLong(s);
             } catch( Exception e ) {
             }
             s = String.format("%,d", nbRows);
             s = "    " + AWNBROWS + " " + s + " ";
             a = new MyAnchor(aladin, s, 50, null, null);
             a.setForeground(Color.gray);
             p1.add(a);
          }
          
            if( p1.getComponentCount() > 0 ) PropPanel.addCouple(p, null, p1, g, c);

            JPanel accessPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
            JCheckBox bx;
            customBx = hipsBx = mocBx = mociBx = progBx = dmBx = csBx = liveBx = siaBx = ssaBx = allBx = globalBx = tapBx = xmatchBx = msBx = null;
            if( to.hasHips() ) {
               hipsBx = bx = new JCheckBox(AWPROGACC);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bg.add(bx);
               bx.setSelected(true);
               Util.toolTip(bx, AWPROGACCTIP);
            }

            if( to.hasGlobalAccess() ) {
               globalBx = bx = new JCheckBox(AWDATAACC);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setSelected(true);
               bg.add(bx);
               Util.toolTip(bx, AWDATAACCTIP);
            }

            if( to.hasSIA() ) {
               siaBx = bx = new JCheckBox(AWCSLAB);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bg.add(bx);
               bx.setSelected( !to.hasCustom() );
               String proto = to.hasSIAv2() ? "SIAv2" : "SIA";
               String s1 = AWSIATIP;
               s1 = s1.replace("SIA", proto);
               Util.toolTip(bx, s1, true);
            }

            if( to.hasSSA() ) {
               ssaBx = bx = new JCheckBox(AWCSLAB);
               bx.addActionListener(this);
               bg.add(bx);
               accessPanel.add(bx);
               bx.setSelected( to.hasCustom() );
               Util.toolTip(bx, AWSSATIP, true);
            }

            if( to.isCDSCatalog() ) {
               boolean allCat = nbRows < 2000;
               if( hipsBx != null ) bg.add(hipsBx);

               if( nbRows != -1 && nbRows < 100000 ) {
                  allBx = bx = new JCheckBox(AWDATAACC);
                  bx.addActionListener(this);
                  accessPanel.add(bx);
                  bg.add(bx);
                  bx.setSelected(!to.hasHips() && allCat);
                  Util.toolTip(bx, AWDATAACCTIP1);
               }

               csBx = bx = new JCheckBox(AWCSLAB);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setSelected(!to.hasHips() && !allCat);
               bx.setToolTipText(AWINVIEWTIP);
               bg.add(bx);

               if( to.isSimbadLive() ) {
                  liveBx = bx = new JCheckBox("live");
                  bx.addActionListener(this);
                  accessPanel.add(bx);
                  bg.add(bx);
                  bx.setToolTipText("Access to Simbad \"live\" (slower but with no cache)");
               }
               
               msBx = bx = new JCheckBox(AWMOCQLAB);
               bx.addActionListener(this);
               accessPanel.add(bx);
               Util.toolTip(bx, AWMOCQLABTIP);
               bx.setEnabled(hasRegion);
               bg.add(bx);

               xmatchBx = bx = new JCheckBox(AWXMATCH);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setEnabled(hasLoadedCat);
               Util.toolTip(bx, AWXMATCHTIP, true);
               bg.add(bx);

               // Positionnement de la sélection par défaut
               boolean onRegion = aladin.calque.getFirstSelectedPlan() instanceof PlanMoc || hasMocPol;
               if( onRegion && msBx != null ) msBx.setSelected(true);
               else if( allBx != null && nbRows < 10000 ) allBx.setSelected(true);
               else if( csBx.isEnabled() ) csBx.setSelected(true);

            } 
               
            if( to.hasCustom() ) {
               customBx = bx = new JCheckBox(AWCUSTOM);
               Util.toolTip(bx, AWCUSTOMTIP);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setSelected(true);
               bg.add(bx);
            }

            // Peut être y a-t-il tout de même un simple accès par cone
            if( csBx==null && to.getCSUrl() != null ) {
               csBx = bx = new JCheckBox(AWCSLAB);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setSelected( hipsBx==null && customBx==null);
               Util.toolTip(bx, AWINVIEWTIP);
               bg.add(bx);
            }

            if( to.hasTAP() ) {
               tapBx = bx = new JCheckBox(AWCRIT);
               bx.addActionListener(this);
               accessPanel.add(bx);
               bx.setSelected(csBx == null && siaBx == null && ssaBx == null && customBx == null );
               Util.toolTip(bx, AWCRITTIP, true);
               bg.add(bx);
            }

            // Juste pour ne pas sélectioner un truc inactivé
            if( csBx != null   && !csBx.isEnabled() )   csBx.setSelected(false);
            if( liveBx != null && !liveBx.isEnabled() ) liveBx.setSelected(false);
            if( siaBx != null  && !siaBx.isEnabled() )  siaBx.setSelected(false);
            if( ssaBx != null  && !ssaBx.isEnabled() )  ssaBx.setSelected(false);
            if( customBx != null && !customBx.isEnabled() ) customBx.setSelected(false);

            JPanel productPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));

            if( to.hasMoc() ) {
               mocBx = bx = new JCheckBox(AWMOCX);
               bx.addActionListener(this);
               productPanel.add(bx);
               Util.toolTip(bx, AWMOCXTIP, true);
            }

            if( to.isCDSCatalog() && nbRows != -1 && nbRows >= 10000 ) {
               dmBx = bx = new JCheckBox(AWDM);
               bx.addActionListener(this);
               productPanel.add(bx);
               Util.toolTip(bx, AWDMTIP, true);
            } else {
               if( to.getProgenitorsUrl() != null ) {
                  progBx = bx = new JCheckBox(AWPROGEN);
                  bx.addActionListener(this);
                  productPanel.add(bx);
                  Util.toolTip(bx, AWPROGENTIP, true);
               }
            }

            GridBagConstraints c1 = new GridBagConstraints();
            GridBagLayout g1 = new GridBagLayout();
            c1.fill = GridBagConstraints.BOTH;
            c1.insets = new Insets(0, 0, 0, 0);
            JPanel loadPanel = new JPanel(g1);

            JLabel label1 = new JLabel(AWACCMODE+" ");
            JLabel label2 = new JLabel(AwDERPROD+" ");
            String tip = AWACCMODETIP;
            Util.toolTip(label1, Util.fold(tip, 100, true));
            String tip2 = AwDERPRODTIP;
            Util.toolTip(label2, Util.fold(tip2, 100, true));
            label1.setForeground(Color.gray.brighter());
            label2.setForeground(Color.gray.brighter());
            label1.setFont(label1.getFont().deriveFont(Font.ITALIC));
            label2.setFont(label2.getFont().deriveFont(Font.ITALIC));

            PropPanel.addCouple(null, loadPanel, label1, null, accessPanel, g1, c1, GridBagConstraints.EAST);
            int nbAccess = accessPanel.getComponentCount();
            int nbProduct = productPanel.getComponentCount();
            if( nbProduct > 0 ) {

               // Ca va faire trop de checkboxes sur une ligne => on replie sur 2 lignes
               if( nbAccess + nbProduct > 5 )
                  PropPanel.addCouple(null, loadPanel, label2, null, productPanel, g1, c1, GridBagConstraints.EAST);

               // Sinon on ajoute au bout de la précédente ligne
               else {
                  accessPanel.add(new JLabel(" + "));
                  for( Component cp : productPanel.getComponents() )
                     accessPanel.add(cp);
                  label1.setText(label1.getText() + " & " + label2.getText());
               }
            }

            PropPanel.addFilet(p, g, c, 5, 2);
            PropPanel.addCouple(p, "", loadPanel, g, c);
         }

         panelInfo.add(p, BorderLayout.CENTER);

         JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
         JPanel precontrol = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
         control.setBackground(contentPane.getBackground());

         Preview preview = null;
         JButton b = null;
         JTextField t = null;
         JPanel submitPanel = new JPanel(new FlowLayout());

         if( treeObjs.size() == 1 ) {

            preview = new Preview(to);

            // Info
            if( to.hasInfo() ) {
               b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Info.png")));
               b.setToolTipText(AWINFOTIP);
               b.setMargin(new Insets(0, 0, 0, 0));
               b.setBorderPainted(false);
               b.setContentAreaFilled(false);
               control.add(b);
               b.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     info();
                  }
               });
            }

            // Properties
            b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Prop.png")));
            b.setToolTipText(AWPROPTIP);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            control.add(b);
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  prop();
               }
            });

            // Bookmark
            b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Bookmark.png")));
            Util.toolTip(b, AWBOOKMARKTIP);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            control.add(b);
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  bookmark();
               }
            });

            // Paramètres
            if( to.hasProp() ) {
               b = new JButton(new ImageIcon(Aladin.aladin.getImagette("settings.png")));
               b.setToolTipText(AWPARAMTIP);
               b.setMargin(new Insets(0, 0, 0, 0));
               b.setBorderPainted(false);
               b.setContentAreaFilled(false);
               final TreeObjDir to1 = to;
               b.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     parameters(to1);
                  }
               });
               control.add(b);
            }

         } else {
            
            b = new JButton(new ImageIcon( Aladin.aladin.getImagette("Expand.png")));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            Util.toolTip(b, aladin.chaine.getString("COLLAPSETIP"), true);
            b.setFont(b.getFont().deriveFont(Font.BOLD));

            precontrol.add(b);
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  iconCollapse.submit();
               }
            });
           
            b = new JButton(new ImageIcon( Aladin.aladin.getImagette("Sort.png")));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            Util.toolTip(b, aladin.chaine.getString("SORTTIP"), true);
            b.setFont(b.getFont().deriveFont(Font.BOLD));

            precontrol.add(b);
            final JButton bfinal = b;
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  aladin.directory.tri( bfinal, 5,5);
               }
            });
            
            if( flagScan ) {
               b = new JButton(new ImageIcon( Aladin.aladin.getImagette("icon_searchAitoff.png"))); // AWSCANONLY);
               b.setMargin(new Insets(0, 0, 0, 0));
               b.setBorderPainted(false);
               b.setContentAreaFilled(false);
               b.setEnabled(hasView);
               Util.toolTip(b, AWSCANONLYTIP, true);
               b.setFont(b.getFont().deriveFont(Font.BOLD));

               precontrol.add(b);
               b.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     scan();
                     hideInfo();
                  }
               });
            }
         }
         
         // Epinglette
         b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Pin.png")));
         b.setToolTipText(AWSTICKTIP);
         b.setMargin(new Insets(0, 0, 0, 0));
         b.setBorderPainted(false);
         b.setContentAreaFilled(false);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { epingle(); }
         });
         control.add(b);
         
         load = b = new JButton(AWLOAD);
         b.setFont(b.getFont().deriveFont(Font.BOLD));
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit();
               if( !decorated ) hideInfo();
               else aladin.f.toFront();
            }
         });
         submitPanel.add(b);

         if( siaBx!=null || ssaBx!=null || csBx!=null ) {
            
            targetPanel = new JPanel( new FlowLayout(FlowLayout.RIGHT,0,0) );
            t = target = new ConeField();
            Util.toolTip(t, AWCONETIP);
            t.addKeyListener( new KeyListener() {
               public void keyPressed(KeyEvent e) { }
               public void keyTyped(KeyEvent e) { }
               public void keyReleased(KeyEvent e) {
                  if( grabIt ) aladin.view.getCurrentView().stopGrabIt();
                  if( e.getKeyCode()==KeyEvent.VK_ENTER ) {
                    if( ((JTextField)e.getSource()).getText().trim().length()==0 ) {
                       setTarget(null);
                    } else {
                       submit();
                       setVisible(false);
                    }
                 }
               }
            });
            targetPanel.add( t );
           
            // Grab
            grab = b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Grab.png")));
            Util.toolTip(b, AWCGRAPTIP);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  startGrabIt();
               }
            });
            if( hasView ) targetPanel.add( b );
            
            submitPanel.add(targetPanel);
         }
         
         JPanel bas = new JPanel(new BorderLayout(0, 0));
         if( precontrol.getComponentCount()>0 ) bas.add(precontrol, BorderLayout.WEST);
         bas.add(submitPanel, BorderLayout.CENTER);
         bas.add(control, BorderLayout.EAST);

         if( to != null && to.internalId != null ) {
            JTextField x = new JTextField(to.internalId + " "); // JTextField pour un label "copiable"
            x.setEditable(false);
            x.setBackground(getBackground());
            x.setBorder(BorderFactory.createEmptyBorder());
            x.setForeground(Aladin.COLOR_BLUE);
            x.setFont(x.getFont().deriveFont(Font.BOLD + Font.ITALIC));
            bas.add(x, BorderLayout.WEST);
         }

         contentPane.add(panelInfo, BorderLayout.CENTER);
         panelInfo.add(bas, BorderLayout.SOUTH);

         if( preview != null ) panelInfo.add(preview, BorderLayout.WEST);
         contentPane.validate();
      }
      
      protected void startGrabIt() {
         grabIt=true;
         targetPanel.removeAll();
         targetPanel.add( target );
         targetPanel.add( grab );
         validate();
         aladin.getFrame(aladin).toFront();
         
         if( aladin.firstGrab && aladin.configuration.isHelp() && aladin.configuration.showHelpIfOk("GRABINFO") ) {
            aladin.firstGrab=false;
         }

      }

      class Preview extends JPanel {
         String url;

         TreeObjDir to;

         Preview(TreeObjDir to) {
            super();
            this.to = to;

            // Déjà fait ?
            if( to.previewError || to.imPreview != null ) return;

            // Pas de preview ?
            if( !to.hasPreview() ) {
               to.previewError = true;
               return;
            }

            this.url = to.getPreviewUrl();
            (new Thread() {
               public void run() {
                  load();
               }
            }).start();
         }

         void load() {

            MyInputStream is = null;
            try {
               is = new MyInputStream(Util.openStream(url));
               byte buf[] = is.readFully();
               if( buf.length == 0 ) throw new Exception();
               Image im = Toolkit.getDefaultToolkit().createImage(buf);

               MediaTracker mt = new MediaTracker(this);
               mt.addImage(im, 0);
               mt.waitForAll();

               to.imPreview = im;

            } catch( Exception e ) {
               to.previewError = true;
            } finally {
               if( is != null ) try {
                  is.close();
               } catch( Exception e1 ) {
               }
            }
            repaint();
         }

         public Dimension getPreferredSize() {
            return new Dimension(88, 88);
         }

         public void paintComponent(Graphics g) {
            paintComponent1(g);
            int w = getWidth();
            int h = getHeight();
            int x,y;
            String s;
            
            if( to.isNew() ) {
               g.setFont(Aladin.ITALIC);
               s = "New " + (to.isNewObsRelease() ? "release" : to.isNewHips() ? "HiPS" : "!");
               x = w / 2 - g.getFontMetrics().stringWidth(s) / 2 + 3;
               y = 12;
               Util.drawNew(g, x - 4, y - 6, Color.yellow);
               g.drawString(s, x, y);
            }

            boolean in = to.getIsIn() == 1;
            g.setFont(Aladin.SPLAIN);
            Color c = in ? Aladin.COLOR_GREEN.brighter().brighter() : Aladin.ORANGE.brighter();
            s = to.getDataType();
            x = w / 2 - g.getFontMetrics().stringWidth(s) / 2 + 3;
            y = h-16;
            g.setColor( c );
            g.drawString(s, x, y);

            if( to.getIsIn() != -1 ) {
               g.setFont(Aladin.SPLAIN);
               s = in ? "data in view" : "out of view";
               x = w / 2 - g.getFontMetrics().stringWidth(s) / 2 + (in ? 0 : 3);
               y = h - 4;
               if( !in ) Util.drawWarning(g, x - 10, y - 7, c, Color.black);
               g.setColor( c );
               g.drawString(s, x, y);
            }
         }

         private void paintComponent1(Graphics g) {
            super.paintComponent(g);
            int ws = getWidth();
            int hs = getHeight();
            g.setColor(Color.gray);
            g.fillRect(0, 0, ws, hs);

            if( to.previewError || to.imPreview == null ) {
               g.setColor(Color.lightGray);
               String s = to.previewError ? "no preview" : "loading...";
               g.setFont(Aladin.ITALIC);
               java.awt.FontMetrics fm = g.getFontMetrics();
               g.drawString(s, ws / 2 - fm.stringWidth(s) / 2, hs / 2 + 4);
               return;
            }

            if( to.imPreview == null ) return;

            Image img = to.imPreview;

            int wi = img.getWidth(this);
            int hi = img.getHeight(this);
            boolean vertical = Math.abs(1 - (double) hi / hs) < Math.abs(1 - (double) wi / ws);
            double sx2, sy2;
            if( vertical ) {
               sy2 = hi;
               sx2 = ws * ((double) hi / hs);
            } else {
               sx2 = wi;
               sy2 = hs * ((double) wi / ws);
            }

            if( hi < hs ) g.translate(0, (hs - hi) / 2);
            g.drawImage(img, 0, 0, ws, hs, 0, 0, (int) sx2, (int) sy2, this);
            if( hi < hs ) g.translate(0, -(hs - hi) / 2);
         }
      }
      
      class ConeField extends JTextField {
         String info=null;
         ConeField() { super(26); }
         
         void setInfo(String s) { info=s; }
         
         public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if( info!=null && getText().length()==0 ) {
               g.setColor( Color.lightGray );
               g.setFont( getFont().deriveFont(Font.ITALIC) );
               g.drawString(info,5,getHeight()-6);
            }
         }
         
      }
      
      private class NoneSelectedButtonGroup extends ButtonGroup {
         public void setSelected(ButtonModel model, boolean selected) {
            if( selected ) super.setSelected(model, selected);
            else clearSelection();
         }
      }

      // Ajout du bookmark qui correspond à la sélection des checkboxes
      void bookmark() {
         StringBuilder bkm = new StringBuilder();
         TreeObjDir to = treeObjs.get(0);
         if( allBx != null && allBx.isSelected() ) addBkm(bkm, to.getAllBkm());
         if( globalBx != null && globalBx.isSelected() ) addBkm(bkm, to.getGlobalAccessBkm());
         if( siaBx != null && siaBx.isSelected() ) addBkm(bkm, to.getSIABkm());
         if( ssaBx != null && ssaBx.isSelected() ) addBkm(bkm, to.getSSABkm());
         if( csBx != null && csBx.isSelected() ) addBkm(bkm, to.getCSBkm());
//         if( customBx != null && customBx.isSelected() ) addBkm(bkm, to.getCustomBkm());
         if( liveBx != null && liveBx.isSelected() ) addBkm(bkm, to.getLiveSimbadBkm());
         if( hipsBx != null && hipsBx.isSelected() ) addBkm(bkm, to.getHipsBkm());
         if( mocBx != null && mocBx.isSelected() ) addBkm(bkm, to.getMocBkm());
         if( progBx != null && progBx.isSelected() ) addBkm(bkm, to.getProgenitorsBkm());

         // LES 3 AUTRES ACCES NE SONT PAS ACTUELLEMENT ACCESSIBLES PAR COMMANDE SCRIPT
         // => PAS DE BOOKMARK POSSIBLE
         // if( msBx!=null && msBx.isSelected() ) to.queryByMoc();
         // if( dmBx!=null && dmBx.isSelected() ) to.loadDensityMap();
         // if( tapBx!=null && tapBx.isSelected() ) to.queryByTap();

         if( bkm.length() == 0 ) return;

         String name = to.internalId;
         FrameBookmarks fb = aladin.bookmarks.getFrameBookmarks();
         fb.setVisibleEdit();
         fb.createNewBookmark(name, "$TARGET,$RADIUS", "Load " + name + " on the view", bkm.toString());
      }

      // Ajout d'une commande au bookmark en cours de construction
      private void addBkm(StringBuilder bkm, String cmd) {
         if( bkm.length() > 0 ) bkm.append("\n");
         bkm.append("   " + cmd);
      }

      /** Affiche la fenêtre des paramètres */
      void parameters(TreeObjDir to) {
         if( !to.hasProp() ) {
            if( frameProp != null ) frameProp.setVisible(false);
            return;
         }
         if( frameProp == null ) frameProp = new FrameProp(aladin, "Query parameters", to);
         else frameProp.updateAndShow(to);
      }

      void prop() {
         try {
            TreeObjDir to1 = treeObjs.get(0);
            new FrameHipsProperties(to1.internalId + " properties", to1.prop.getRecord(null));
         } catch( Exception e2 ) {
         }
      }

      void info() {
         TreeObjDir to = treeObjs.get(0);
         String url = to.getInfoUrl();
         aladin.glu.showDocument(url);
      }

      void submit() {
         if( treeObjs.size() == 0 ) return;

         // Interrogation par region, mais comme c'est un cercle, on fait directement un CS
         // plutôt qu'un query by MOC
         if( msBx != null && msBx.isSelected() && aladin.view.vselobj.size() == 1 ) {
            Obj o = aladin.view.vselobj.get(0);
            if( o instanceof Cercle || o instanceof SourceStat ) {
               try {
                  double ra = o.getRa();
                  double de = o.getDec();
                  double radius = o.getRadius();
                  for( TreeObjDir to : treeObjs )
                     to.loadCS(new Coord(ra, de), radius);
                  return;
               } catch( Exception e ) {
               }
            }
         }

         PlanMoc planMoc = null;
         if( msBx != null && msBx.isSelected() ) {

            // Création d'un plan MOC à partir d'une région sélectionnée
            // dans un plan tool
            if( aladin.view.hasMocPolSelected() ) {

               HealpixMoc moc = null;
               try {
                  moc = aladin.createMocByRegions(-1);
               } catch( Exception e ) {
               }
               if( moc != null ) {
                  Coord c = aladin.calque.getTargetBG(null, null);
                  double rad = aladin.calque.getRadiusBG(null, null, null);
                  planMoc = new PlanMoc(aladin, moc, "moc", c, rad);
               } else {
                  aladin.warning("MOC creation failed !\nYour graphical region must be circles, "
                        + "and/or polygons counter-clock oriented");
                  return;
               }

               // Détermination du PlanMoc directement dans la pile
            } else {

               for( Object p : aladin.calque.getSelectedPlanes() ) {
                  if( p instanceof PlanMoc && ((PlanMoc) p).flagOk ) {
                     planMoc = (PlanMoc) p;
                     break;
                  }
               }
               if( planMoc == null ) {
                  for( Object p : aladin.calque.getPlans() ) {
                     if( p instanceof PlanMoc && ((PlanMoc) p).flagOk ) {
                        planMoc = (PlanMoc) p;
                        break;
                     }
                  }
               }
            }

            if( planMoc == null ) {
               aladin.warning("You need to select a graphical region or a MOC plane in the stack");
               return;
            }
         }
         
         // Y a-t-il un cone spécifique ?
         String cone = target==null ? null : target.getText().trim();
         if( cone!=null && cone.length()==0 ) cone=null;

         // Accès à une collection
         if( treeObjs.size() == 1 ) {
            TreeObjDir to = treeObjs.get(0);
            if( globalBx != null && globalBx.isSelected() ) to.loadGlobalAccess();
            if( allBx != null    && allBx.isSelected() )    to.loadAll();
            if( siaBx != null    && siaBx.isSelected() )    to.loadSIA(cone);
            if( ssaBx != null    && ssaBx.isSelected() )    to.loadSSA(cone);
            if( csBx != null     && csBx.isSelected() )     to.loadCS(cone);
            if( customBx!=null   && customBx.isSelected() ) to.loadCustom();
            if( liveBx != null   && liveBx.isSelected() )   to.loadLiveSimbad(cone);
            if( msBx != null     && msBx.isSelected() )     to.queryByMoc(planMoc);
            if( hipsBx != null   && hipsBx.isSelected() )   to.loadHips();
            if( mocBx != null    && mocBx.isSelected() )    to.loadMoc();
            if( progBx != null   && progBx.isSelected() )   to.loadProgenitors();
            if( dmBx != null     && dmBx.isSelected() )     to.loadDensityMap();
            if( tapBx != null    && tapBx.isSelected() )    to.queryByTap();
            if( xmatchBx != null && xmatchBx.isSelected() ) to.queryByXmatch();

            // Accès à plusieurs collections simultanément
         } else {

            // CS et assimilés
            if( csBx != null && csBx.isSelected() ) {
               if( tooMany(treeObjs.size()) ) return;
               for( TreeObjDir to : treeObjs ) {
                  if( to.getIsIn() == 0 ) continue; // Je n'interroge pas les collections hors champs
                  if( to.hasSIA() ) to.loadSIA(cone);
                  else if( to.hasSSA() ) to.loadSSA(cone);
                  else if( to.hasGlobalAccess() ) to.loadGlobalAccess();
                  else to.loadCS(cone);
               }
            }

            if( hipsBx != null && hipsBx.isSelected() ) {
               if( tooMany(treeObjs.size()) ) return;
               for( TreeObjDir to : treeObjs ) {
                  if( to.hasHips() ) to.loadHips();
               }
            }

            // MOC search
            if( msBx != null && msBx.isSelected() ) {
               for( TreeObjDir to : treeObjs ) {
                  if( to.isCDSCatalog() ) to.queryByMoc(planMoc);
               }
            }

            // Union des MOCs
            if( mocBx != null && mocBx.isSelected() ) {
               if( tooMany(treeObjs.size()) ) return;
               multiMocLoad(treeObjs, MultiMocMode.EACH);
            }
            if( mocuBx != null && mocuBx.isSelected() ) multiMocLoad(treeObjs, MultiMocMode.UNION);
            if( mociBx != null && mociBx.isSelected() ) multiMocLoad(treeObjs, MultiMocMode.INTER);
         }

      }

      /** Chargement de chaque MOC ou de l'union ou intersection des Mocs */
      void multiMocLoad(ArrayList<TreeObjDir> treeObjs, MultiMocMode mode) {

         // Chaque MOC individuellement
         if( mode == MultiMocMode.EACH ) {
            for( TreeObjDir to : treeObjs )
               to.loadMoc();
            return;
         }

         // Union ou Intersection
         StringBuilder params = null;
         for( TreeObjDir to : treeObjs ) {
            if( params == null ) params = new StringBuilder(to.internalId);
            else params.append("," + to.internalId);
         }
         String cmd = (mode == MultiMocMode.INTER ? "iMOCs" : "MOCs") + "=get Moc(" + Tok.quote(params.toString())
               + (mode == MultiMocMode.INTER ? ",imoc" : "") + ")";
         aladin.execAsyncCommand(cmd);

      }
   }
   static private enum MultiMocMode {
      EACH, UNION, INTER
   }
   
   protected boolean grabIt=false;
   public boolean isGrabIt() { return grabIt; }
   public void stopGrabIt() { grabIt=false; }
   public void setGrabItCoord(double x, double y) {
      String sexaCoord = GrabUtil.getGrabItCoord(aladin, x, y);
      frameInfo.setTarget( aladin.localisation.getFrameCoord(sexaCoord) );
      frameInfo.updateWidget();

   }
   public void setGrabItRadius(double x1, double y1, double x2, double y2) {
      String radius = GrabUtil.setGrabItRadius(aladin, null, x1,y1, x2,y2);
      frameInfo.setRadius( radius );
      frameInfo.updateWidget();
   }
   
   public void toFront() { frameInfo.toFront(); }
   
   /** Cache la fenêtre des infos si possible */
   public void hideInfoIfPossible() {
      System.out.println("Bip");
      if( frameInfo==null || !frameInfo.isVisible() ) return;
      if( decorated ) return;
      frameInfo.setVisible(false);

   }
}
