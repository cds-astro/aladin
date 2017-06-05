// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
 * Classe qui g�re l'arbre du Directory des collections
 * @version 1.0 d�cembre 2016 - cr�ation
 * @author Pierre Fernique [CDS]
 */
public class Directory extends JPanel implements Iterable<MocItem>{
   
   static private final String DIRECTORY = "Data access";
   static private final String UPDATING   = "  updating...";
   static protected final String ROOT_LABEL = "Collections";
   
   private Aladin aladin;                  // R�f�rence
   protected MultiMoc2 multiProp;             // Le multimoc de stockage des properties des collections
   private DirectoryFilter directoryFilter=null; // Formulaire de filtrage de l'arbre des collections
   protected boolean mocServerLoading=false;  // true si on est en train de charger le directory initial
   protected boolean mocServerUpdating=false; // true si on est en train de mettre � jour le directory
   private boolean flagScanLocal=false;       // true si on a des MOCs dans le multiprop local
   
   private DirectoryTree dirTree;          // Le JTree du directory
   protected ArrayList<TreeObjDir> dirList;  // La liste des collections connues
   
   // Composantes de l'interface
   private QuickFilterField quickFilter; // Champ de filtrage rapide
   protected FilterCombo comboFilter;         // Menu popup des filtres
   protected IconFilter iconFilter;      // L'icone d'activation du filtrage
   protected IconInside iconInside;      // L'icone d'activation du mode "inside"
   protected IconScan iconScan;          // L'icone d'activation du scan
   private IconCollapse iconCollapse;    // L'icone pour d�velopper/r�duire l'arbre
   private Timer timer = null;           // Timer pour le r�affichage lors du chargement
   private JLabel dir=null;              // Le titre qui apparait au-dessus de l'arbre
   
   // Param�tres d'appel initial du MocServer (construction de l'arbre)
   private static String  MOCSERVER_INIT = "*&fields=!hipsgen*&get=record&fmt=asciic";
   
   // Param�tres de maj par le MocServer (update de l'arbre)
   private static String MOCSERVER_PARAM = "fmt=asciic";

   private JScrollPane scrollTree = null;
   
   public Directory(Aladin aladin, Color cbg) {
      this.aladin = aladin;
      multiProp = new MultiMoc2();
      
      // POUR LES TESTS => Surcharge de l'URL du MocServer
//      if( aladin.levelTrace>=3 ) aladin.glu.aladinDic.put("MocServer","http://localhost:8080/MocServer/query?$1");

      setBackground(cbg);
      setLayout(new BorderLayout(0,0) );
      setBorder( BorderFactory.createEmptyBorder(8,3,10,0));
      
      JPanel pTitre = new JPanel( new FlowLayout(FlowLayout.LEFT,35,0));
      pTitre.setBackground( cbg );
      dir = new JLabel(DIRECTORY);
      Util.toolTip(dir, "Directory of available data set collections from CDS and other IVOA servers.\n"
            + "Browse, filter, select, and load the collections you want to display...",true);
      dir.setFont(dir.getFont().deriveFont(Font.BOLD));
      dir.setForeground( Aladin.COLOR_LABEL);
      pTitre.add( dir );
      
      // L'arbre avec sa scrollbar
      dirTree = new DirectoryTree(aladin, cbg);
      scrollTree = new JScrollPane(dirTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder( BorderFactory.createEmptyBorder(6,0,0,0));
      scrollTree.setBackground(cbg);
//      scrollTree.getViewport().setOpaque(true);
//      scrollTree.getViewport().setBackground(cbg);
      scrollTree.setOpaque(false);
      scrollTree.setViewportBorder(null);
      
      if( Aladin.DARK_THEME ) {
         scrollTree.getVerticalScrollBar().setUI(new MyScrollBarUI());
         scrollTree.getHorizontalScrollBar().setUI(new MyScrollBarUI());
      }
      
      
      // Le controle des filtres
      String s = "Filter by free keywords (comma separated) "
            + "or by any advanced filter expression (ex: nb_rows&lt;1000). Press ENTER to activate it.";
      JLabel labelFilter = new JLabel("select");
      Util.toolTip(labelFilter,s,true);
      labelFilter.setFont(labelFilter.getFont().deriveFont(Font.BOLD));
      labelFilter.setForeground( Aladin.COLOR_LABEL);
      
      quickFilter = new QuickFilterField(10);
      Util.toolTip(quickFilter,s,true);
      
      s = "List of predefined filters. Use '+' button to edit it, or create a new one";
      JLabel fromLabel = new JLabel("from");
      Util.toolTip(fromLabel,s,true);
      fromLabel.setFont(fromLabel.getFont().deriveFont(Font.BOLD));
      fromLabel.setForeground( Aladin.COLOR_LABEL);
      
      comboFilter = new FilterCombo(s);
      
      JLabel plus = new JLabel("  + ");
      Util.toolTip(plus,"Edit the current filter or create a new one",true);
      plus.setFont( Aladin.LBOLD );
      plus.setForeground(Aladin.COLOR_LABEL);
      plus.addMouseListener(new MouseListener() {
         public void mouseReleased(MouseEvent e) { }
         public void mousePressed(MouseEvent e) { }
         public void mouseExited(MouseEvent e) { }
         public void mouseEntered(MouseEvent e) { }
         public void mouseClicked(MouseEvent e) { openAdvancedFilterFrame(); }
      });

      // Pour que le quickFilter et le popupFilter aient m�me taille et soient align�s, je les place
      // tous les deux dans un GridBagLayoutPanel qui sera CENTER dans BorderLayout, et � EAST de ce panel
      // le bouton du " + ". Ca permet �galement d'�viter de faire disparaitre ce "+" lorsque le bandeau
      //  du Directory tree est trop �troit
      //
      //     select XXXXXXXX  |
      //     from   XXXXXXXX  |  +
      //
      JPanel plusFilter = new JPanel( new BorderLayout(0,0));
      plusFilter.setBackground(cbg);
      plusFilter.add(plus, BorderLayout.SOUTH);
      
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
      c.insets = new Insets(2,3,3,2);
      JPanel panelFilter1 = new JPanel( g );
      panelFilter1.setBackground( cbg );
      PropPanel.addCouple(null,panelFilter1, labelFilter, null, quickFilter, g,c,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL);
      PropPanel.addCouple(null,panelFilter1, fromLabel, null, comboFilter, g,c,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL);
      
      JPanel panelFilter = new JPanel( new BorderLayout(0,0));
      panelFilter.setBorder( BorderFactory.createEmptyBorder(5,5,10,5));
      panelFilter.setBackground(cbg);
      panelFilter.add(panelFilter1, BorderLayout.CENTER);
      panelFilter.add(plusFilter, BorderLayout.EAST);
      
      // Les icones de controle tout en bas
      iconFilter = new IconFilter(aladin);
      iconCollapse = new IconCollapse(aladin);
      iconInside = new IconInside(aladin);
      iconScan = new IconScan(aladin);
     
      JPanel pControl = new JPanel(new FlowLayout(FlowLayout.LEFT,1,1));
      pControl.setBackground(cbg);
      pControl.add(iconFilter);
      pControl.add(iconCollapse);
      pControl.add(iconInside);
      pControl.add(iconScan);
      
      JPanel panelControl = new JPanel(new BorderLayout(0,0));
      panelControl.setBackground(cbg);
      panelControl.setBorder( BorderFactory.createEmptyBorder(0,5,0,0));
      panelControl.add(panelFilter,BorderLayout.NORTH);
      panelControl.add(pControl,BorderLayout.SOUTH);
      
      JPanel panelTree = new JPanel( new BorderLayout(0,0));
      panelTree.setBackground(cbg);
      panelTree.add(pTitre,BorderLayout.NORTH);
      panelTree.add(scrollTree,BorderLayout.CENTER);

      add(panelTree, BorderLayout.CENTER);
      add(panelControl,BorderLayout.SOUTH);

      // Actions sur le clic d'un noeud de l'arbre
      dirTree.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            toHighLighted = null;
            TreePath tp = dirTree.getPathForLocation(e.getX(), e.getY());
            if( tp==null ) hideInfo();
            else {
               ArrayList<TreeObjDir> treeObjs = getSelectedTreeObjDir();

               // Double-clic, on effectue l'action par d�faut du noeud
               if (e.getClickCount() == 2) loadMulti( treeObjs );

               // Simple clic => on montre les informations associ�es au noeud
               else {
                  if( treeObjs.size()==1 ) selectInStack( treeObjs.get(0).internalId );
                  showInfo(treeObjs,e);
               }
            }
            
            iconCollapse.repaint();
            resetWasExpanded();
            repaint();
         }
      });
      
      dirTree.addMouseMotionListener(new MouseMotionListener() {
         public void mouseMoved(MouseEvent e) {
            if( frameInfo==null || !frameInfo.isVisible() ) return;
            TreePath tp = dirTree.getPathForLocation(e.getX(), e.getY());
            if( tp==null ) return;
            if( getSelectedTreeObjDir().size()>1 ) return;
            
            // Si on se d�place davantage vers la droite que vers le bas ou le haut
            // => on ne change pas
            Point p = e.getPoint();
            if( lastMove!=null && 1.5*(p.x-lastMove.x) > Math.abs(p.y-lastMove.y) ) return;
            lastMove=p;
            
            ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
            DefaultMutableTreeNode to = (DefaultMutableTreeNode)tp.getLastPathComponent();
            if( !to.isLeaf() ) return;
            treeObjs.add( (TreeObjDir)to.getUserObject() );
            if( treeObjs.size()==1 ) {
               showInfo(treeObjs,e);
               toHighLighted = treeObjs.get(0);
               dirTree.repaint();
//               if( showInfo(treeObjs,e) ) dirTree.setSelectionPath(tp);
            }
         }
         public void mouseDragged(MouseEvent e) { }
      });
      
//       Chargement de l'arbre initial
      (new Thread(){
         public void run() {  updateTree(); }
      }).start();
   }
   
   protected void selectInStack(String id) {
      aladin.calque.selectPlan( id );
   }
   
   /** Ouvre l'arbre en montrant le noeud associ� au path sp�cifi� */
   protected void showTreePath(String path) {
      if( !isVisible() || !hasCollections() || path==null ) return;
      dirTree.showBranch(path);
   }
   
   /** Ouvre l'arbre en montrant le noeud associ� � l'id sp�cifi� */
   protected void showTreeObj(String id) {
      if( !isVisible() || !hasCollections() || id==null ) return;
      int i = id.indexOf('~');
      if( i<0 ) i = id.indexOf(' ');
      if( i>0 ) id = id.substring(0,i);
      dirTree.showTreeObj(id);
   }
   
   /** Retourne true si le directory contient quelque chose (avant filtrage �ventuel) */
   protected boolean hasCollections() {
      return dirList!=null &&  dirList.size()>0;
   }
   
   protected void reset() {
      directoryFilter.reset();
      dirTree.defaultExpand();
   }
   
   // M�morisation de la derni�re position de la souris en mouseMoved()
   private Point lastMove = null;
   
   /** Met � jour la liste des filtres  */
   protected void updateDirFilter() {
      if( comboFilter==null ) return;
      
      ActionListener[] a = comboFilter.getListeners(ActionListener.class);
      if( a!=null ) for( ActionListener a1 : a ) comboFilter.removeActionListener( a1 );
      
      String current = (String)comboFilter.getSelectedItem();
      comboFilter.removeAllItems();
      comboFilter.addItem(directoryFilter.ALLCOLLHTML);
      
      String mylist = aladin.configuration.dirFilter.get(DirectoryFilter.MYLIST);
      if( mylist!=null ) {
         comboFilter.addItem(DirectoryFilter.MYLISTHTML);
      }
      
      for( String name : aladin.configuration.dirFilter.keySet() ) {
         if( name.equals(directoryFilter.ALLCOLL) ) continue;
         if( name.equals(directoryFilter.MYLIST) ) continue;
         comboFilter.addItem( name );
      }
      
      if( current!=null ) comboFilter.setSelectedItem( current );
      
      if( a!=null ) for( ActionListener a1 : a) comboFilter.addActionListener( a1 );
   }
   
   
   private TreeObjDir toHighLighted = null;
   
   /** retourne le noeud qu'il faut encadrer (passage de la souris dessus) */
   protected TreeObjDir getTreeObjDirHighLighted() { return toHighLighted; }
   
   class FilterCombo extends JComboBox<String> {
      FilterCombo(String tooltip) {
         super();
         setUI( new MyComboBoxUI());
         //      setPrototypeDisplayValue("000000000000000");
         Util.toolTip(this,tooltip,true);
         setFont(Aladin.PLAIN);
         addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { filtre( (String)getSelectedItem() ); }
         });
         updateWidgets();
      }

      void updateWidgets() {
         boolean actif = iconFilter!=null && iconFilter.isActivated();
         setBackground( actif ? Aladin.COLOR_TEXT_BACKGROUND : Aladin.COLOR_TEXT_BACKGROUND.darker() );
//         setForeground( actif ? Aladin.COLOR_TEXT_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
      }
   }
   
   protected void focusSearch() {
      quickFilter.focus("Your keyword ?");
   }
   
   /** Classe pour un JTextField avec reset en bout de champ (petite croix rouge) */
   class QuickFilterField extends JTextField implements MouseListener, KeyListener /*,MouseMotionListener*/ {
      private Rectangle cross=null;
      

      QuickFilterField(int nChar) {
         super(nChar);
         addMouseListener(this);
         addKeyListener(this);
         setUI( new BasicTextFieldUI() );
         updateWidgets();
      }
      
      public void keyTyped(KeyEvent e) { }
      public void keyPressed(KeyEvent e) { }
      public void keyReleased(KeyEvent e) {
         iconFilter.setActivated(true);
         if( e.getKeyCode()==KeyEvent.VK_ENTER ) {
            if( timer!=null ) timer.stop(); 
            timer=null;
            doFiltre(); 
         }
         else filtre();
      }
      
      Timer timer = null;
      private void filtre() {
         if( timer==null ) {
            timer = new Timer(500, new ActionListener() {
               public void actionPerformed(ActionEvent e) { 
                  doFiltre();
                  requestFocus();
                  timer=null;
               }
            });
            timer.setRepeats(false);
            timer.start();
         } else {
            timer.restart();
         }
         updateWidgets();
      }
      
      /** Fait clignoter le champ pour attirer l'attention
       * de l'utilisateur et demande le focus sur le champ de saisie */
      protected void focus(String s) { focus(s,null); }
      protected void focus(String s,final String initial) {
         setText(s);

         (new Thread() {
            Color def = getBackground();
            Color deff = getForeground();
            public void run() {
               for( int i=0; i<2; i++ ) {
                  setBackground(Color.green);
                  setForeground(Color.black);
                  Util.pause(800);
                  setBackground(def);
                  setForeground(deff);
                  Util.pause(200);
               }
               if( initial==null ) {
                  setText("");
                  requestFocusInWindow();
               } else {
                  setText(initial);
                  requestFocusInWindow();
                  setCaretPosition(getText().length());
               }
               if( iconFilter!=null ) iconFilter.setActivated(true);
               updateWidgets();
            }
         }).start();
      }

//      public Dimension getPreferredSize() {  Dimension d = super.getPreferredSize(); d.height-=4; return d; } 
      
      public Dimension getMaximumSize() { Dimension d = super.getMaximumSize(); d.width=150; return d; }

      void updateWidgets() {
         boolean actif = iconFilter!=null && iconFilter.isActivated();
         setBackground( actif ? Aladin.COLOR_TEXT_BACKGROUND : Aladin.COLOR_TEXT_BACKGROUND.darker() );
         setForeground( actif ? Aladin.COLOR_TEXT_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
      }

      boolean in(int x) {
         if( cross==null ) return false;
         return x>=cross.x;
      }

      public void paintComponent(Graphics g) {
         try {
            super.paintComponent(g);
            int x = getWidth()-X-8;
            int y = getHeight()/2-X/2;
            if( mocServerLoading || mocServerUpdating ) {
               Slide.drawBall(g, x, y, blinkState ? Color.white : Color.green );
            }
            else drawCross(g,x,y);
         } catch( Exception e ) { }
      }

      static private final int X = 6;
      private void drawCross(Graphics g, int x, int y) {
         cross = new Rectangle(x,y,X,X);
         String s = getText();
         if( s.length()==0 || s.startsWith(UPDATING) ) return;
         g.setColor( getBackground() );
         g.fillOval(x-3, y-3, X+7, X+7);
         g.setColor( Color.red.darker() );
         g.drawLine(x,y,x+X,y+X);
         g.drawLine(x+1,y,x+X+1,y+X);
         g.drawLine(x+2,y,x+X+2,y+X);
         g.drawLine(x+X,y,x,y+X);
         g.drawLine(x+X+1,y,x+1,y+X);
         g.drawLine(x+X+2,y,x+2,y+X);
      }

      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }

      void reset() {
         if( getText().length()==0 ) return;
         setText("");
         doFiltre();
      }

      public void mouseReleased(MouseEvent e) {
         if( in(e.getX())  ) reset();
         else { iconFilter.setActivated(true); updateWidgets(); }
      }
    }
   
   /** Retourne true si on a s�lectionn� quelque chose de scannable */
   protected boolean isScannable() {
      return getSelectedTreeObjDirScannable().size()>=1;
   }

   /** R�cup�ration de la liste des TreeObj s�lectionn�es qui n'ont pas de MOC � disposition */
   private  ArrayList<TreeObjDir> getSelectedTreeObjDirScannable() {
      ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
      for( TreeObjDir to : getSelectedTreeObjDir() ) {
         if( !to.hasMoc() ) treeObjs.add(to);
      }
      return treeObjs;
   }
   
   /** R�cup�ration de la liste des TreeObj s�lectionn�es */
   private ArrayList<TreeObjDir> getSelectedTreeObjDir() {
      TreePath [] tps = dirTree.getSelectionPaths();
      ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
      if( tps!=null ) {
         for( int i=0; i<tps.length; i++ ) {
            DefaultMutableTreeNode to = (DefaultMutableTreeNode) tps[i].getLastPathComponent();
            addObj(treeObjs,to);
         }
      }
      return treeObjs;
   }
   
   private void addObj( ArrayList<TreeObjDir> treeObjs, DefaultMutableTreeNode to ) {
      if( to.isLeaf() ) {
         Object obj = to.getUserObject();
         if( obj instanceof TreeObjDir ) treeObjs.add( (TreeObjDir)obj );

      } else {
         int n = to.getChildCount();
         for( int i=0; i<n; i++ ) {
            addObj( treeObjs, (DefaultMutableTreeNode) to.getChildAt(i) );
         }
      }
   }

   // La frame d'affichage des informations du HiPS cliqu� � la souris
   private FrameInfo frameInfo = null;
   
   /** Cache la fen�tre des infos du HiPS */
   private void hideInfo() { showInfo(null,null); toHighLighted=null;  repaint(); }
   
   /** Chargements simultan�s de toutes les collections s�lectionn�es */
   private void loadMulti(ArrayList<TreeObjDir> treeObjs) {
      if( treeObjs.size()==0 ) return;
      hideInfo();
      if( tooMany(treeObjs.size()) ) return;
      for( TreeObjDir to : treeObjs ) to.load();
   }
   
   private boolean tooMany( int n ) {
      if( n>100 ) {
         if( !aladin.confirmation("Do you really want to query "+n+" data sets\n"
               + "to check which of them have a result inside the current view ?") ) return true;
      }
      return false;
   }
   
   /** Affiche la fenetre des infos des Collections pass�es en param�tre
    * @param treeObjs les noeuds correspondant aux collections s�lectionn�es, ou null si effacement de la fen�tre
    * @param e �v�nement souris pour r�cup�rer la position absolue o� il faut afficher la fen�tre d'info
    */
   private boolean showInfo(ArrayList<TreeObjDir> treeObjs, MouseEvent e) {
      if( treeObjs==null || treeObjs.size()==0  ) {
         if( frameInfo==null ) return false;
         frameInfo.setVisible(false);
         return false;
      }
      if( frameInfo==null ) frameInfo = new FrameInfo();
      Point p = e.getLocationOnScreen();
      if( !frameInfo.setCollections( treeObjs ) ) return false;
      
      int w=350;
      int h=120;
      int x=p.x+50;
      int y=p.y-30;
      
      if( y<0 ) y=0;
      frameInfo.setBounds(x,y,Math.max(w,frameInfo.getWidth()),Math.max(h, frameInfo.getHeight()));
      frameInfo.setVisible(true);
      return true;
   }
   
   // Gestion du scanning
   private boolean isScanning=false; // true si un scan est en cours
   synchronized private void setScanning(boolean flag) { isScanning=flag; }
   synchronized protected boolean isScanning() { return isScanning; }
   
   // Liste des collections en cours de scanning
   private ArrayList<TreeObjDir> scanTreeObjs = null;
   
   // Le gestionnaire du pool de threads du scan
   private ExecutorService scanService=null;
   
   // true lorsqu'on a demand� l'arr�t du scan courant
   private boolean abortScan=false;
   
   /** Demande l'arr�t anticip� du scan en cours. Et force la sortie en exception des collections
    * en cours de scanning en fermant manu-militari leur flux de donn�es vers le serveur. Cette
    * technique n'est pas vraiment super, mais c'est mieux que rien
    */
   synchronized protected void abortScan() {
      if( !isScanning() ) return;
      abortScan = true;
      
      if( scanService!=null ) scanService.shutdownNow();
      
      // On essaye de forcer la main aux collections en cours de scanning
      // en fermant le flux
      (new Thread(){
         public void run() {
            for( TreeObjDir to : scanTreeObjs ) {
               if( to.isScanning() ) to.abortScan();
            }
         }
      }).start();
   }
   
   /** true si le scan en cours doit �tre arr�t� d�s que possible */
   synchronized protected boolean isAbortingScan() { return abortScan; }
   
   /** Lancement d'un scan sur les collections s�lectionn�es qui ne disposent pas d'un MOC.
    * Lance une requ�te CS ou SIA ou SSA sur le champ courant, et construit avec les positions
    * retourn�es un MOC local dans le MultiProp. Celui-ci sera utilis� en ajout du MocServer
    * pour d�terminer si la collection a ou n'a pas de r�sultat dans le champ courant;
    * Afin de connaitre les champs d�j� scann�s, un MOC de couverture sera �galement m�moris�
    * dans multiProp (mocRef).
    */
   protected void scan() {
      
      // On va �viter de lancer plusieurs scans en parall�le.
      if( isScanning() ) {
         aladin.warning("Another scan is still running.\nWait the end if this previous scan\n"
               + "or stop it before launching another one (button stop).");
         return;
      }
      
      // D�ployement de l'arbre pour montrer les collections concern�es
      dirTree.allExpand(  dirTree.getSelectionPath() );
      
      // Liste des collections s�lectionn�es scannables
      final ArrayList<TreeObjDir> treeObjs = getSelectedTreeObjDirScannable();
      
      // Demande de confirmation du scan s'il concerne beaucoup de collections
      final int n = treeObjs.size();
      if( tooMany(n) ) return;
      
      // On va travaill� avec un pool de threads
      final ExecutorService service = Executors.newFixedThreadPool(10);
      
      // M�morisation de la liste pour un �ventuel abort (cf abortScan())
      scanTreeObjs = treeObjs;
      scanService = service;
      
      setScanning(true);
      (new Thread(){
         
         public void run() {
            
            // Le timer permet de r�afficher l'arbre toutes les demi secondes et ainsi
            // de faire clignoter le logo "noMoc" des collections en cours de scanning
            startTimer();
            
            try {
               abortScan = false;
               flagScanLocal=true;  // le MultiProp local doit �tre pris en compte
               
               Aladin.trace(4,"Directory.scan()... Launched on "+n+" data sets...");
               
               for( final TreeObjDir to : treeObjs ) {
                  
                  // On scanne chaque collection (10 en parall�le)
                  service.execute( new Runnable() {
                     public void run() {
                        try {
//                           System.out.println("Scanning "+to.internalId+"...");
                           MocItem2 mo = multiProp.getItem(to.internalId);
                           to.scan( mo );
//                           System.out.println(mo.mocId+" => "+mo.getMocRef().todebug());
                           
                        } catch( Exception e ) {
                           if( aladin.levelTrace>=3 ) e.printStackTrace();
                        }
                     }
                  });
               }
               service.shutdown();
               
               // On ajoute au fur et � mesure les nouvelles collections qui ont �t� scann�es
               while( !service.isTerminated() ) {
                  resumeIn( ResumeMode.LOCALADD );
                  Util.pause(500);
//                  System.out.println("Scan processing...");
               }
            } finally {
               setScanning(false);
               scanTreeObjs=null;
               scanService=null;
               stopTimer();
               Util.pause( 200 );
               resumeIn( ResumeMode.FORCE );   // Un dernier resume complet de l'arbre
               Aladin.trace(4,"Directory.scan() finished");
               repaint();
            }
         }
      }).start();
   }

   /** Cr�ation/ouverture/fermeture du formulaire de filtrage de l'arbre des Collections */
   protected void openAdvancedFilterFrame() {
      if( directoryFilter==null ) directoryFilter = new DirectoryFilter(aladin);
      if( directoryFilter.isVisible() ) directoryFilter.setVisible( false );
      else {
         String name = (String)comboFilter.getSelectedItem();
         if( name.equals(directoryFilter.ALLCOLLHTML) ){
            name = directoryFilter.MYLIST;
            aladin.configuration.setDirFilter(name, "");
            updateDirFilter();
            comboFilter.setSelectedItem(directoryFilter.MYLISTHTML);
         }

         directoryFilter.showFilter();
      }
   }

   /** Filtrage de l'arbre des Collections */
   protected void doFiltre() {
      if( directoryFilter==null ) directoryFilter = new DirectoryFilter(aladin);
      
      int i = aladin.directory.comboFilter.getSelectedIndex();
      if( iconFilter.isActivated() && i>0 ) {
         aladin.directory.filtre( (String)aladin.directory.comboFilter.getSelectedItem() );
         
      } else {
         directoryFilter.submitAction(false);
      }
      
      if( dirList.size()<1000 ) dirTree.allExpand();
   }
   
   /** Activation d'un filtre pr�alablement sauvegard� */
   protected void filtre(String name) {
      
      if( name.equals(DirectoryFilter.ALLCOLLHTML) ) name=DirectoryFilter.ALLCOLL;
      else if( name.equals(DirectoryFilter.MYLISTHTML) ) name=DirectoryFilter.MYLIST;
      
      System.out.println("filter("+name+")");
      
      String expr = name.equals(DirectoryFilter.ALLCOLL) ? "*" : aladin.configuration.dirFilter.get(name);
      
      if( expr!=null ) {
         iconFilter.setActivated(true);
         if( directoryFilter==null ) directoryFilter = new DirectoryFilter(aladin);
         
         directoryFilter.setSpecificalFilter(name, expr );
      }
   }
   
   /** Positionne une contrainte, soit en texte libre, soit cle=valeur */
   protected String getQuickFilterExpr() {
      String s1;
      String s = quickFilter.getText().trim();
      if( s.length()==0 ) return s;
      
      String expr;
      
      // S'il s'agit directement d'une expression, rien � faire, sinon
      // il faut g�n�rer l'expression en fonction d'une liste de mots cl�s
      int i = s.indexOf('=');
      if( i<0 ) i=s.indexOf('>');
      if( i<0 ) i=s.indexOf('<');
      if( i>0 && (aladin.directory.isFieldName( (s1=s.substring(0,i).trim())) || s1.indexOf('*')>=0) ) expr = s;
//      else expr = "obs_title,obs_description,obs_collection,ID="+DirectoryFilter.jokerize(s);
      else expr = "obs_title,obs_collection,ID="+DirectoryFilter.jokerize(s);
      
      return expr;
   }
   
   /** Retourne true si la chaine est une cl� de propri�t� d'au moins un enregistrement */
   protected boolean isFieldName(String s) { return multiProp.isFieldName(s); }
   
   /**
    * Retourne le node correspondant � une identiciation
    * @param A l'identificateur de la collection � chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact
    *             1 - substring sur label
    *             2 - match exact puis substring sur l'IVOID (ex: Simbad ok pour CDS/Simbad)
    *                 puis du menu  (ex DssColored ok pour Optical/DSS/DssColored)
    * @return le Hips node trouv�, null sinon
    * 
    * NECESSAIRE POUR get Hips(XXX) MAIS IL FAUDRA CHANGER CELA
    */
   protected TreeObjDir getTreeObjDir(String A) { return getTreeObjDir(A,0); }
   protected TreeObjDir getTreeObjDir(String A,int mode) {
      for( TreeObjDir to : dirList ) {
         if( !to.isHiPS() ) continue;
         if( A.equals(to.id) || A.equals(to.label) || A.equals(to.internalId) ) return to;
         if( mode==1 && Util.indexOfIgnoreCase(to.label,A)>=0 ) return to;
         if( mode==2 ) {
            if( to.internalId!=null && to.internalId.endsWith(A) ) return to;

            int offset = to.label.lastIndexOf('/');
            if( A.equals(to.label.substring(offset+1)) ) return to;
         }
      }

      if( mode==2 ) {
         for( TreeObjDir to : dirList ) {
            if( !to.isHiPS() ) continue;
            int offset = to.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(to.label.substring(offset+1),A)>=0 ) return to;
         }
      }
      return null;
   }
   
   /** Cr�ation du plan via script - m�thode identique � celle de la s�rie des classes ServerXXX
    * (de fait, reprise de ServerHips de la version Aladin v9) */
   protected int createPlane(String target,String radius,String criteria, String label, String origin) {
      String survey;
      int defaultMode=PlanBG.UNKNOWN;

      if( criteria==null || criteria.trim().length()==0 ) survey="CDS/P/DSS2/color";
      else {
         Tok tok = new Tok(criteria,", ");
         survey = tok.nextToken();

         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode=PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("png") ) defaultMode=PlanBG.JPEG;
         }
      }
      
      TreeObjDir to = getTreeObjDir(survey,2);
      if( to==null ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) to.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(to,label,target,radius);
   }
   
   /** Returne l'URL du service_url indiqu� (cs,sia,ssa,tap...) pour
    * l'enregistrement properties correspondant au mocid
    * @param service_url (cs,sia,ssa,tap...)
    * @param mocId 
    * @return l'URL demand�, ou null si non trouv�
    */
   protected String resolveServiceUrl(String service_url, String mocId ) {
      
      // un �ventuel pr�fixe ivo:// doit �tre ignor�
      if( mocId.startsWith("ivo://") ) mocId = mocId.substring(6);
      
      MocItem m = multiProp.getItem( mocId );
      if( m==null ) return null;

      // Recherche du champ correspondant au service_url indiqu�
      String key = service_url.toLowerCase()+"_service_url";
      String url = m.prop.get( key );
      if( url==null ) return null;

      // Petit nettoyage de l'URL (le VO registry regorge d'imagination...)
      if( !url.endsWith("?") && !url.endsWith("&") ) url+="?";

      int pos;
      String fmt;

      // On enl�ve un �ventuel &REQUEST=queryData redondant
      if( service_url.equalsIgnoreCase("ssa") ) {
         fmt = "&REQUEST=queryData";
         if( (pos=Util.indexOfIgnoreCase(url, fmt))>=0 ) {
            url = url.substring(0,pos) + url.substring(pos+fmt.length() );
         }

      } else if( service_url.equalsIgnoreCase("sia") ) {
         fmt = "&FORMAT=image/fits";
         if( (pos=Util.indexOfIgnoreCase(url, fmt))>=0 ) {
            url = url.substring(0,pos) + url.substring(pos+fmt.length() );
         }
      }

      return url;

   }


   @Override
   public Iterator<MocItem> iterator() {
      return multiProp.iterator();
   }
   
   // False lorsque la premi�re initialisation de l'arbre est faite
   private boolean init=true;
   
   
   
   /** Maj initiale de l'arbre - et maj des menus Aladin correspondants */
   protected void updateTree() {
      try {
         initMultiProp();
         buildTree();
      } finally { postTreeProcess(true); init=false;}
   }
   
   /** (Re)construction de l'arbre en fonction de l'�tat pr�c�dent de l'arbre
    * et de la valeur des diff�rents flags associ�s aux noeuds
    * @param refCount  true si on doit faire les d�comptages des noeuds en tant que r�f�rence sinon �tat courant
    * @return true s'il y a eu au-moins une modif
    */
   private void buildTree() {
      DirectoryModel model = (DirectoryModel) dirTree.getModel();
      for( TreeObjDir to : dirList ) model.createTreeBranch( to ); 
      int n = initCounter( model );
      updateTitre(n);
   }
   
   // Initialisation du compteur de r�f�rence en fonction d'un TreeModel
   private int initCounter( DirectoryModel model ) {
      // Initialisation du compteur de r�f�rence
      HashMap<String,Integer> hs = new HashMap<String,Integer>();
      int n = model.countDescendance( hs );
      counter = hs;
      return n;
   }
   
   /** Mise � jour du titre au-dessus de l'arbre en fonction des compteurs */
   private void updateTitre(int nb) {
      String t = DIRECTORY;
      if( nb!=-1 && dirList!=null && nb<dirList.size() ) {
         t = "<html>"+t+"<font color=\"#D0D0F0\"> &rarr; "+nb+" / "+dirList.size()+"</font></html>";
      }
      dir.setText(t);
   }
   
   // Compteurs des noeuds
   private HashMap<String,Integer> counter = null;
   private HashSet<String> wasExpanded = null;
   
   protected void resetWasExpanded() { wasExpanded = null; }
   
   /** Reconstruction de l'arbre en utilisant � chaque fois un nouveau model
    * pour �viter les conflits d'acc�s et �viter la lenteur des �v�nements
    * de maj des �tat de l'arbre (apr�s de tr�s nombreux tests, c'est de loin
    * la meilleure m�thode que j'ai trouv�e m�me si elle peut paraitre �tonnante
    * � premi�re vue.)
    * 
    * @param tmpDirList
    */
   private void rebuildTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter ) {
      boolean insideActivated = iconInside.isActivated();
      
      // M�morisation temporaire des �tats expanded/collapsed
//      HashMap<String,Boolean> wasExpanded = new HashMap<String,Boolean>();
      if( wasExpanded==null ) {
         wasExpanded = new HashSet<String>();
         backupState(new TreePath(dirTree.root), wasExpanded, dirTree);
      }

      // G�n�ration d'un nouveau model prenant en compte les filtres
      DirectoryModel model = new DirectoryModel(aladin);
      for( TreeObjDir to : tmpDirList ) {
         boolean mustBeActivated = !to.isHidden() && (!insideActivated || insideActivated && to.getIsIn()!=0 );
         if( mustBeActivated ) model.createTreeBranch( to );
      }
      
      if( initCounter ) initCounter( model );
      else  updateTitre( model.countDescendance() );

      // R�percussion des �tats des feuilles sur les branches
      model.populateFlagIn();
      
      // Remplacement du model dans l'arbre affich�
      dirTree.setModel( model );
      
      // Ouverture minimal des noeuds
      if( defaultExpand ) dirTree.defaultExpand();
         
      // Restauration des �tats expanded/collapses + compteurs de r�f�rence
      restoreState( new TreePath(model.root), defaultExpand ? null : wasExpanded, counter, dirTree);
   }
   
   /** Retourne le path sous forme de chaine - sans le premier "/" et "" pour la racine
    * ex => Image/Optical/DSS
    */
   private String getPathString(TreePath p) {
      boolean first=true;
      StringBuilder path = null;
      for( Object n : p.getPath() ) {
         if( first ) { first=false; continue; }  // on ne prend pas la racine
         if( path==null ) path = new StringBuilder(n+"");
         else path.append("/"+n);
      }
      return path==null ? "" : path.toString();
   }
   
   /** M�morise dans une HashSet les branches qui sont expanded */
   private void backupState(TreePath parent, HashSet<String> wasExpanded, DirectoryTree tree) {
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      TreeObj to =  (TreeObj)lastNode.getUserObject();
      if( tree.isExpanded(parent) ) wasExpanded.add( to.path );
      
      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            backupState( parent.pathByAddingChild(node) , wasExpanded, tree);
         }
      }
   }
   
   private String parent( String path ) {
      int i = path.lastIndexOf('/');
      if( i<0 ) return "";
      return path.substring(0,i);
   }
   
   /** Restaure l'�tat des branches en fonction d'une hashSet des branches
    * qui �taient expanded, ainsi que d'une hashmap donnant les d�comptes
    * de r�f�rence pour chaque branche. */
   private void restoreState(TreePath parent, HashSet<String> wasExpanded,  HashMap<String,Integer> counter, JTree tree) {
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      String path = getPathString(parent);
      
      // R�cup�ration de l'�tat expanded/collapsed
      if( wasExpanded!=null ) {
         if( wasExpanded.contains( path ) ) tree.expandPath( parent );
      }
      
      // R�cup�ration du compteur de r�f�rence
      Integer nbRef = counter.get( path );
      if( nbRef!=null )  {
         TreeObj to =  (TreeObj)lastNode.getUserObject();
         to.nbRef = nbRef.intValue();
      }

      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            restoreState( parent.pathByAddingChild(node) , wasExpanded, counter, tree );
         }
      }
   }
   
   /** R�affichage de l'arbre en fonction des flags courants */
   protected void resumeTree() { resumeTree(dirList,false,false); }
   
   /** Remplacement et r�affichage de l'arbre avec une nouvelle liste de noeuds */
   protected void replaceTree(ArrayList<TreeObjDir> tmpDirList) {
      resumeTree(tmpDirList,true,true);
      dirList = tmpDirList;
   }
   
   /** R�affichage de l'arbre */
   private void resumeTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter) {
      try {
//         long t0 = System.currentTimeMillis();
         rebuildTree(tmpDirList,defaultExpand,initCounter);
         validate();
         postTreeProcess(defaultExpand);
//         System.out.println("resumeTree done in "+(System.currentTimeMillis()-t0)+"ms");
      } finally {

         // Pour permettre le changement du curseur d'attente de la fen�tre de filtrage
         if( directoryFilter!=null ) aladin.makeCursor(directoryFilter, Aladin.DEFAULTCURSOR);
      }

   }
   
   /** retourne true si un filtre est positionn� */
   protected boolean hasFilter() {
      String s = quickFilter.getText();
      if( s==null || s.startsWith(UPDATING) ) s="";
      return s.length()>0  || comboFilter.getSelectedIndex()>0;
   }
   
   /** Filtrage et r�affichage de l'arbre en fonction des contraintes indiqu�es dans params
    *  @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   protected void resumeFilter(String expr) {
      try {
         
         System.out.println("resumeFilter iconFilter.isActivated="+iconFilter.isActivated());
         
         // En cas de d�sactivation du filtrage, pas de contraintes
         if( !iconFilter.isActivated() ) expr="*";
         
         // sinon
         else {
            // Ajout de la contrainte du filtre rapide � l'expression issue du filtre global
            String quick = getQuickFilterExpr();
            if( quick.length()>0 ) {
               if( expr.length()==0 || expr.equals("*") ) expr=quick;
               else expr = "("+expr+") && "+quick;
            }
         }
         
         if( iconFilter.isActivated() && expr.equals("*") ) iconFilter.setActivated( false );
         
         System.out.println("resumeFilter("+expr+")");
         // Filtrage
         checkFilter(expr);

         // Reg�n�ration de l'arbre
         resumeTree();
         
         dirTree.allExpand();
         
         updateWidgets();
         
      } catch( Exception e ) {
        if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   private void updateWidgets() {
      iconFilter.repaint();
      
      quickFilter.updateWidgets();
      comboFilter.updateWidgets();
   }
   
   static protected enum ResumeMode { NORMAL, FORCE, LOCALADD };

   /** R�affichage de l'arbre en fonction des Hips in/out de la vue courante */
   protected void resumeIn() { resumeIn( ResumeMode.NORMAL ); }
   protected void resumeIn( ResumeMode mode) {
      if( !checkIn( mode ) ) return;
      if( iconInside.isActivated() ) resumeTree();
      ((DirectoryModel)dirTree.getModel()).populateFlagIn();
      repaint();
   }

   /** Positionnement des flags isHidden() de l'arbre en fonction des contraintes de filtrage
    * @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   private void checkFilter(String expr) throws Exception {
      
      // Filtrage
//      long t0 = System.currentTimeMillis();
      ArrayList<String> ids = multiProp.scan( (HealpixMoc)null, expr, false, -1);
//      System.out.println("Filter: "+ids.size()+"/"+multiProp.size()+" in "+(System.currentTimeMillis()-t0)+"ms");
      
      // Positionnement des flags isHidden() en fonction du filtrage
      HashSet<String> set = new HashSet<String>( ids.size() );
      for( String s : ids ) set.add(s);      
      for( TreeObjDir to : dirList ) to.setHidden( !set.contains(to.internalId) );
   }
   
   /**
    * D�termination de l'ordre pour avoir 30 cellules dans la distance
    * @param size taille � couvrir (en degr�s)
    * @return order HEALPix appropri�
    */
   private int getAppropriateOrder(double size) {
      int order = 4;
      if( size==0 ) order=HealpixMoc.MAXORDER;
      else {
         double pixRes = size/30;
         double degrad = Math.toDegrees(1.0);
         double skyArea = 4.*Math.PI*degrad*degrad;
         double res = Math.sqrt(skyArea/(12*16*16));
         while( order<HealpixMoc.MAXORDER && res>pixRes) { res/=2; order++; }
      }
      return order;
   }
   
   /** Retourne true si pour la collection identifi�e par "id" on dispose d'un MOC local 
    * sur la zone d�crite par mocQuery (c�d que leur intersection n'est pas nulle */
   private boolean hasLocalMoc(String id,HealpixMoc mocQuery) {
      if( mocQuery==null ) return false;
      MocItem2 mo = multiProp.getItem(id);
      if( mo.mocRef==null ) return false;
      return mo.mocRef.isIntersecting( mocQuery );
   }
   
   // Dernier champs interrog� sur le MocServer
   private Coord oc=null;
   private double osize=-1;
   private HashSet<String> previousSet = null;
   

   /** Interroge le MocServer pour conna�tre les Collections disponibles dans le champ.
    * Met � jour l'arbre en cons�quence
    * @param mode RESUME_IN_IF_NEEDED, RESUME_IN_FORCE, RESUME_IN_FORCE_LOCAL
    */
   private boolean checkIn(ResumeMode mode) {
      if( !dialogOk() ) return false; 

      // Le champ est trop grand ou que la vue n'a pas de r�f spatiale ?
      // => on suppose que tous les HiPS sont a priori visibles
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
         boolean modif=false;
         for( TreeObjDir to : dirList ) {
            if( !modif && to.getIsIn()!=-1 ) modif=true;
            to.setIn(-1);
         }
         return modif;
      }

      try {
         
         HashSet<String> set = mode==ResumeMode.LOCALADD ? previousSet : new HashSet<String>();
         
         // Pour �viter de faire 2x la m�me chose de suite
         Coord c = v.getCooCentre();
         double size = v.getTaille();
         boolean sameLocation = c.equals(oc) && size==osize;

         String params;
         if( mode==ResumeMode.NORMAL && sameLocation ) return false;
         oc=c;
         osize=size;

         // Interrogation du MocServer distant...
         BufferedReader in=null;
         
         // Interrogation par cercle
         if( v.getTaille()>45 ) {
            params = MOCSERVER_PARAM+"&RA="+c.al+"&DEC="+c.del+"&SR="+size*Math.sqrt(2);

            // Interrogation par rectangle
         } else {
            StringBuilder s1 = new StringBuilder("Polygon");
            for( Coord c1: v.getCooCorners())  s1.append(" "+c1.al+" "+c1.del);
            params = MOCSERVER_PARAM+"&stc="+URLEncoder.encode(s1.toString());
         }

         try {
            if( mode==ResumeMode.FORCE || !sameLocation ) {
               URL u = aladin.glu.getURL("MocServer", params, true);

               Aladin.trace(4,"HipsMarket.hipsUpdate: Contacting MocServer : "+u);
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               String s;

               // r�cup�ration de chaque ID concern�e (1 par ligne)
               while( (s=in.readLine())!=null ) set.add( getId(s) );
            }

         } catch( EOFException e ) {}
         finally{ if( in!=null ) in.close(); }
         
         // Interrogation du Multimoc interne (uniquement par cercle)
         HealpixMoc mocQuery=null;
         if( flagScanLocal && (mode==ResumeMode.FORCE || mode==ResumeMode.LOCALADD || !sameLocation) ) {
            
            // Construction d'un MOC qui englobe le cercle couvrant le champ de vue courant
            Healpix hpx = new Healpix();
            int order = getAppropriateOrder(size);
            mocQuery = new HealpixMoc(order);
            int i=0;
            mocQuery.setCheckConsistencyFlag(false);
            for( long n : hpx.queryDisc(order, c.al, c.del, size/2) ) {
               mocQuery.add(order, n);
               if( (++i)%1000==0 )  mocQuery.checkAndFix();
            }
            mocQuery.setCheckConsistencyFlag(true);
            //               System.out.println("Moc d'interrogation => "+mocQuery.todebug());

            ArrayList<String> mocIds = multiProp.scan(mocQuery);
            if( mocIds!=null ) for( String id : mocIds ) set.add( id );
         }

         // V�rification si �a a chang� depuis la derni�re fois
         if( previousSet!=null && previousSet!=set && previousSet.equals(set) ) {
            //               System.out.println("Pas de changement depuis la derni�re fois");
            return false;
         }
         previousSet = set;

         // Positionnement des flags correspondants
         for( TreeObjDir to : dirList ) {
            if( !to.hasMoc() && !hasLocalMoc(to.internalId,mocQuery)) to.setIn( -1 );
            else to.setIn( set.contains(to.internalId) ? 1 : 0 );
         }

      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

      return true;
   }
   
   /** Retourne true si l'arbre est d�velopp� selon le d�faut pr�vu */
   protected boolean isDefaultExpand() { return dirTree.isDefaultExpand(); }
   
   /** Collapse l'arbre sauf le noeud courant */
   protected void collapseAllExceptCurrent() {
      
      // Si j'ai une ou plusieurs branches s�lectionn�es
      TreePath [] tps = dirTree.getSelectionPaths();
      if( tps!=null ) {

         int mode=0;  // -1 collapse, 0-pas encore initialis�,  1 expand
         
         for( TreePath tp: tps ) {
            
            // cas particulier de la racine => on ne referme pas tout
            if( tp.getPathCount()==1 ) {
               if( mode==1 || isDefaultExpand() ) { mode=1; dirTree.allExpand(); }
               else { mode=-1; dirTree.defaultExpand(); }

               // Cas de la s�lection d'une branche
            } else {

               // S'il n'est pas totalement ouvert, je le fais
               if( mode==1 || dirTree.isCollapsed(tp) ) { mode=1; dirTree.allExpand(tp); }
               else  { mode=-1; dirTree.collapseRec(tp); }
            }
         }

         // Rien n'est s�lectionn� => comme si on avait s�lectionn� la racine
      } else {
         if( isDefaultExpand() ) dirTree.allExpand();
         else dirTree.defaultExpand();
      }
      resetWasExpanded();
   }
   
   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return dirTree==null || dirTree.root==null ;
   }

   /** Traitement � appliquer apr�s la g�n�ration ou la r�g�n�ration de l'arbre */
   private void postTreeProcess(boolean minimalExpand) {
      
//      filter.setEnabled( dialogOk() );
      if( minimalExpand ) dirTree.minimalExpand();
      
      // Mise en route ou arr�t du thread de coloration de l'arbre en fonction des Collections
      // pr�sentes ou non dans la vue courante
      if( hasCollections() ) startInsideUpdater();
      else stopInsideUpdater();
   }
   
   /** Ajoute si n�cessaire les propri�t�s d'un fichier "properties" distant */
   private void addRemoteProp(MyProperties prop) {
      String u = prop.getProperty("hips_service_url");
      if( u==null || prop.getProperty("hips_order")!=null ) return;
      
      InputStreamReader in=null;
      try {
         in = new InputStreamReader( Util.openAnyStream(u+"/properties"),"UTF-8" );
         MyProperties aProp = new MyProperties();
         aProp.load( in );
         
         for( String key : aProp.getKeys() ) {
            if( prop.get(key)==null ) prop.put(key,aProp.get(key));
         }
      } catch( Exception e) {}
      finally{ try { if( in!=null ) in.close(); } catch(Exception e) {} }

   }
   
   private boolean interruptServerReading;
   
   /** (Re)g�n�ration du Multiprop en fonction d'un stream d'enregistrements properties */
   protected int loadMultiProp(InputStreamReader in,boolean addition, String path) throws Exception {
      MyProperties prop;
      
      boolean mocServerReading=true;
      interruptServerReading=false;
      int n=0;
      int rm=0;
      String memo="";
      
      try {
         memo = quickFilter.getText();
         quickFilter.setEditable(false);
         quickFilter.setForeground( Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
         
         while( mocServerReading && !interruptServerReading ) {
            prop = new MyProperties();
            mocServerReading = prop.loadRecord(in);
            if( prop.size()==0 || MultiMoc.getID(prop)==null ) continue;
            
            // D�finition locale => PROP_ORIGIN = local
            if( addition ) {
               prop.setProperty("PROP_ORIGIN", "local");
               
               // si HiPS, peut �tre requi�re un compl�ment d'information par acc�s au fichier properties distant ?
               addRemoteProp(prop);
               
               // Dans le cas d'un fichier local, on conserve on �ventuel path pour le HiPS
               if( path!=null ) prop.setProperty("hips_service_url",path);
            }
            
            try {
               
               if( prop.getProperty("MOCSERVER_REMOVE")!=null ) {
                  multiProp.remove( MultiMoc.getID(prop));
                  rm++;
               }
               else multiProp.add( prop );
               
               quickFilter.setText(UPDATING+" ("+n+")");
            } catch( Exception e ) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
            }
            
            n++;
            if( n%1000==0 && n>0) aladin.trace(4,"Directory.loadMultiProp(..) "+(n-rm)+" prop loaded "+(rm>0?" - "+rm+" removed":"")+"...");
//if( n%100==0 ) Util.pause(10);
         }
         if( interruptServerReading ) aladin.trace(3,"MocServer update interrupted !");
      } finally{ 
         try { in.close(); } catch( Exception e) {}
         quickFilter.setEditable(true);
         quickFilter.setForeground( Aladin.COLOR_TEXT_FOREGROUND );
         quickFilter.setText(memo);
      }
      
      return n;
   }
   
   /** Interruption du chargement des infos du MocServer */
   protected void interruptMocServerReading() { interruptServerReading=true; }
   
   /** G�n�ration de la liste des collections en fonction du contenu du MultiProp
    * La liste est tri�e
    * Les URLs HiPS seront m�moris�es dans le Glu afin de pouvoir g�rer les sites miroirs
    */
   private ArrayList<TreeObjDir> populateMultiProp() {
      ArrayList<TreeObjDir> listReg = new ArrayList<TreeObjDir>(20000);
      for( MocItem mi : this ) populateProp(listReg, mi.prop);
      Collections.sort(listReg, TreeObj.getComparator() );
      return listReg;
   }
   
   /** Ajout d'une collection correspondant � un enregistrement prop, ainsi que des
    * entr�es GLU associ�es
    * @param prop       Enregistrement des propri�t�s
    * @param localFile  true si cet enregistrement a �t� charg� localement par l'utilisateur (ACTUELLEMENT NON UTILISE)
    */
   private void populateProp(ArrayList<TreeObjDir> listReg, MyProperties prop) {
      
      // D�termination de l'identificateur
      String id = MultiMoc.getID(prop);
      if( id==null ) {
         System.err.println("Directory.populateProp error - getID returns null => ignored ["+prop.toString().replace("\n"," ")+"]");
         return;
      }
      
      // Ajustement local des propri�t�s
      propAdjust( id, prop );
      
      String HIPSU = Constante.KEY_HIPS_SERVICE_URL;
      
      // Dans le cas o� il n'y a pas de mirroir, m�morisation de l'URL directement
      if( prop.getProperty(HIPSU+"_1")==null ) {
         String url =  prop.getProperty(HIPSU);
         if( url!=null ) aladin.glu.aladinDic.put(id,url);
         
      // Dans le cas o� il y a des mirroirs => m�morisation des indirections
      } else {

         // Enregistrement de tous les mirroirs comme des indirections
         StringBuilder indirection = null;
         for( int i=0; true; i++ ) {
            String url = prop.getProperty(HIPSU+(i==0?"":"_"+i));
            if( url==null ) break;
            
            String subid = id+"_"+i;
            aladin.glu.aladinDic.put(subid,url);

            if( indirection==null ) indirection = new StringBuilder("%I "+subid);
            else indirection.append("\t"+subid);
         }
         
         // M�morisation des indirections possibles sous la forme %I id0\tid1\t...
         aladin.glu.aladinDic.put(id,indirection.toString());
      }
      
      // Ajout dans la liste des noeuds d'arbre
      listReg.add( new TreeObjDir(aladin,id,prop) );
   }
   
//   static private final String [] CS_AUTH      = { "archive.stsci.edu","nasa.heasarc", "irsa.ipac","uk.ac.le.star.tmpledas","wfau.roe.ac.uk","org.gavo.dc" };
//   static private final String [] CS_AUTH_NAME = { "STScI","HEASARC","IRSA","LEDAS","WFAU","GAVO" };
   
   /** Ajustement des propri�t�s, notamment pour ajouter le bon client_category
    * s'il s'agit d'un catalogue */
   private void propAdjust(String id, MyProperties prop) {
      propAdjust1(id,prop);
      String category = prop.getProperty(Constante.KEY_CLIENT_CATEGORY);
      String key = prop.get(Constante.KEY_CLIENT_SORT_KEY);
      
      if( id.equals("CDS/Model.SED/sed") 
            || id.equals("CDS/METAobj")
            || id.equals("CDS/ReadMeObj") ) category=null;
      
      
      // Sans cat�gorie => dans la branche "Unsupervised" suivi du protocole puis de l'authority
      if( category==null ) {
         boolean isHips = prop.getProperty("hips_service_url")!=null;
         boolean isCS   = prop.getProperty("cs_service_url")!=null;
         boolean isSIA  = prop.getProperty("sia_service_url")!=null || prop.getProperty("sia2_service_url")!=null;
         boolean isSSA  = prop.getProperty("ssa_service_url")!=null;
         boolean isTAP  = prop.getProperty("tap_service_url")!=null;
         String subCat = isHips ? "HiPS": isCS || isTAP ? "Catalog by CS,TAP" : isSIA ? "Image by SIA" : isSSA ? "Spectrum by SSA" : "Miscellaneous";
         
//         if( isCS ) {
//            String auth = Util.getSubpath(id, 0, 1);
//            int i;
//            if( (i=Util.indexInArrayOf(auth, CS_AUTH, true))>=0 ) category = "Catalog/"+CS_AUTH_NAME[i];
//         }
         
         if( category==null ) category = "Unsupervised/"+subCat+"/"+Util.getSubpath(id, 0,1);
         prop.setProperty(Constante.KEY_CLIENT_CATEGORY,category);
         
         // On trie un peu les branches
         int k = isHips ? 4: isCS ? 1 :  isTAP ? 2 : isSIA ? 0 : isSSA ? 3 :5;
         key = key==null ? k+"" : k+"/"+key;
         prop.replaceValue( Constante.KEY_CLIENT_SORT_KEY, key);
      }
      
      boolean local = prop.getProperty("PROP_ORIGIN")!=null;
      
      // Rangement dans la branche "local" si chargement local
      if( local && !category.startsWith("Local/") ) {
         category = "Local/"+ category;
         prop.replaceValue(Constante.KEY_CLIENT_CATEGORY,category);
      }
      
      // Tri de la cat�gorie g�n�rale
      
      if( key==null ) key="Z";
      String cat = prop.get(Constante.KEY_CLIENT_CATEGORY);
      int c = Util.indexInArrayOf( Util.getSubpath(cat, 0), CAT);
      if( c==-1 ) c=CAT.length;
      key = String.format("%02d",c)+"/"+key;
      prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY, key);
      
      // Insertion de la date de publication de l'article de r�f�rence
      String bib = prop.get("bib_reference");
      if( bib!=null ) {
         try { 
            int year = Integer.parseInt( bib.substring(0,4) );
            prop.replaceValue("bib_year",""+year);
         } catch( Exception e) {}
      }
      
   }
   
   private void propAdjust1(String id, MyProperties prop) {

      String type = prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( type==null || type.indexOf("catalog")<0 ) return;
      
      String category=null;
      
      if( !id.startsWith("CDS/") ) {
         category = prop.get(Constante.KEY_CLIENT_CATEGORY);
         if( category.equals("Catalog") ) prop.replaceValue(Constante.KEY_CLIENT_CATEGORY,null);
         return;
      }
      
      
      if( id.equals("CDS/Simbad") ) category = "Data base";
      else {
         // Nettoyage des macros latex qui trainent         
         cleanLatexMacro(prop);
         
         // Ajout de l'entr�e TAP (si elle n'existe pas)
         prop.replaceValue("tap_service_url", "http://tapvizier.u-strasbg.fr/TAPVizieR/tap/");
         
         // D�termination de la cat�gorie
         String code = getCatCode(id);
         if( code==null ) return;
         
         String sortPrefix = "";
//         String vizier = "/CDS VizieR";
         String vizier = "";
         
         boolean flagJournal = code.equals("J");
         if( flagJournal ) {
            String journal = getJournalCode(id);
            category = "Catalog"+vizier+"/Journal table/"+journal;
            sortPrefix = journal;
            
         } else {
            int c = Util.indexInArrayOf(code, CAT_CODE);
            if( c==-1 ) category = "Catalog"+vizier+"/"+code;   // Cat�gorie inconnue
            
            else {
               category = "Catalog"+vizier+"/"+CAT_LIB[c];
               sortPrefix=String.format("%02d",c);
               
           }
         }
         
         // Tri par popularit� et cat�gorie
         String popularity = prop.get("vizier_popularity");
         if( popularity!=null ) {
            popularity = String.format("%04d", 1000 -Long.parseLong(popularity));
         } else popularity=getCatSuffix(id);
         String sortKey = sortPrefix+"/"+popularity;
         prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY,sortKey);
         
         // D�termination du suffixe (on ne va pas cr�er un folder pour un �l�ment unique)
         String parent = getCatParent(id);
         boolean hasMultiple = hasMultiple(parent);
         if( !hasMultiple ) {
            parent = getCatParent( parent );
            
            if( !flagJournal ) {
               //Je vire le nom de la table a la fin du titre
               String titre = prop.get(Constante.KEY_OBS_TITLE);
               int i;
               if( titre!=null && (i=titre.lastIndexOf('('))>0 ) {
                  titre = titre.substring(0,i-1);
                  prop.replaceValue(Constante.KEY_OBS_TITLE, titre);
               }
            }

         } else {
            String titre = prop.get(Constante.KEY_OBS_TITLE);
            if( titre!=null ) {

               // Je remonte le nom du catalog sur la branche
               int i= titre.lastIndexOf('(');
               int j = titre.indexOf(')',i);
               if( i>0 && j>i ) {
                  int k = parent.lastIndexOf('/');
                  parent = ( k==-1 ? "" : parent.substring(0,k+1) )+ titre.substring(0,i-1);
                  
                  String desc = prop.get(Constante.KEY_OBS_DESCRIPTION);
                  if( desc!=null ) {
                     String newTitre = desc + titre.substring(i-1,j+1);
                     prop.replaceValue(Constante.KEY_OBS_TITLE, newTitre);
                  }
               }
            }

         }
         if( !flagJournal ) {
            String suffix = getCatSuffix( parent );
            suffix = suffix==null ? "" : "/"+suffix;
            category += suffix;
         }
      }
      
      prop.replaceValue(Constante.KEY_CLIENT_CATEGORY, category );
   }
   
   static private final String [] TEXTKEYS = { "obs_title","obs_description","obs_label","obs_collection" };
   
   /** Nettoyage des macros Latex oubli� par VizieR */
   private void cleanLatexMacro( MyProperties prop ) {
      for( String key : TEXTKEYS ) {
         String s = prop.get(key);
         if( s==null ) continue;
         if( s.indexOf('\\')<0 ) continue;
         s = cleanLatexMacro( s );
         prop.replaceValue(key, s);
      }
   }
   
   static final private int AVANT = 0, MACRO = 1, IN = 2, NEXT = 3, MACRO1 = 4, NAMEMACRO = 5, INMACRO = 6;

   /** Suppression des macros Latex pr�sent dans la chaine
    * ex: texte avant \macro{contenu{ou plus}} texte avant... */
   private String cleanLatexMacro( String s ) {
      StringBuilder s1 = new StringBuilder();
      StringBuilder macro = new StringBuilder();
      char [] a = s.toCharArray();
      int mode=0;
      int level=0;
      boolean keepIn=false;
      
      for( char c : a ) {
         switch( mode ) {
            case AVANT:
               if( c=='{' ) mode=MACRO1;
               else if( c=='\\' ) { mode=MACRO; macro.replace(0, macro.length(), ""); }
               else s1.append(c);
               break;
            case MACRO:
               if( c=='\\' ) macro.replace(0, macro.length(), "");
               else if( c=='{' ) {
                  keepIn = macro.toString().equals("vFile") || macro.toString().equals("em") || macro.toString().equals("bf");
                  mode=IN;
               } else if( !Character.isJavaIdentifierPart(c) ) {
                  String s2 = resolveMacro( macro.toString() );
                  if( s2.length()>0 ) { s1.append( s2 ); s1.append(c); }
                  else {
                     int n = s1.length()-1;
                     if( c==')' && s1.charAt(n)=='(' ) s1.deleteCharAt( n );
                     else if( c==']' && s1.charAt(n)=='[' ) s1.deleteCharAt( n );
                     else if( c=='}' && s1.charAt(n)=='{' ) s1.deleteCharAt( n );
                  }
                  mode=AVANT;
               } else macro.append(c);
               break;
            case IN:
               if( c=='}' && level==0 ) mode=NEXT;
               else if( c=='{' ) level++;
               else if( c=='}' ) level--;
               else if( keepIn ) s1.append(c);
               break;
            case NEXT:
               if( c=='{' ) { mode=IN; keepIn=false; }
               else {
                  mode=AVANT;
                  int n = s1.length()-1;
                  if( c==')' && s1.charAt(n)=='(' ) s1.deleteCharAt( n );
                  else if( c==']' && s1.charAt(n)=='[' ) s1.deleteCharAt( n );
                  else if( c=='}' && s1.charAt(n)=='{' ) s1.deleteCharAt( n );
                  else s1.append(c);
               }
               break;
            case MACRO1:
               if( c!='\\' ) { s1.append('{'); s1.append(c); mode=AVANT; } // Fausse alerte
               else mode=NAMEMACRO;
               break;
            case NAMEMACRO:
               if( c=='\\' ) break;
               if( !Character.isJavaIdentifierPart(c) ) mode=INMACRO;
               break;
            case INMACRO:
               if( c=='}' ) mode=AVANT;
               else s1.append(c);
               break;
         }
      }
//      System.out.println("\nAVANT: "+s+"\nAPRES: "+s1.toString());
      return s1.toString();
   }
   
   static final private String MACRO_LIST [] = { "deg","originalcolumnnames" };
   static final private String MACRO_CONV []  = { "�", ""  };
   
   private String resolveMacro( String macro ) {
      int i = Util.indexInArrayOf(macro, MACRO_LIST);
      if( i<0 ) return macro;
      return MACRO_CONV[i];
   }
   
   // M�morise les paths de l'arbre qui ont de multiples feuilles
   private HashSet<String> multiple = null;
   
   /** Retourne true si le path contient plusieurs feuilles */
   private boolean hasMultiple(String path) {
      if( multiple==null ) {
         multiple = new HashSet<String>(multiProp.size());
         HashSet<String> un = new HashSet<String>(multiProp.size());
         for( MocItem mi : multiProp ) {
            String parent = getCatParent( mi.mocId );
            if( un.contains(parent) ) multiple.add(parent);
            else un.add(parent);
         }
      }
      return multiple.contains(path);
   }
   
   protected ArrayList<String> getBigTAPServers(int limitNbCat) throws Exception {
      
      ArrayList<String> a = multiProp.scan( (HealpixMoc)null, "tap_service_url*=*", false, -1);

      Map<String, Integer> map = new HashMap<String, Integer>();
      for( String id : a) {
         
         // Cas particulier � �carter pour ne pas poser souci avec les catalogues VizieR
         if( id.startsWith("CDS/Simbad") ) continue;

         String auth = Util.getSubpath(id, 0);
         int m;
         Integer n = map.get(auth);
         if( n==null ) m=0;
         else m = n;
         m++;
         map.put(auth,m);
      }
      
      // On trie 
      Map<String, Integer> map1  = DirectoryFilter.sortByValues(map, 1);
      
      // On prend uniquement les serveurs qui ont au moins limitNbCat collections
      ArrayList<String> b = new ArrayList<String>();
      for( String auth : map1.keySet() ) {
         
         Integer n = map.get(auth);
         if( n<limitNbCat ) break;
         
         // On cherche la premi�re entr�e correspondante pour r�cup�rer une URL TAP
         for( String id : a ) {
            String auth1 = Util.getSubpath(id, 0);
            if( !auth1.equals(auth) ) continue;
            
            MocItem mi = multiProp.getItem(id);
            String url = mi.prop.get("tap_service_url");
            
            // On m�morise pour le tableau r�sultat.
            if( auth.equals("CDS") ) auth="cds.vizier";
            String s = auth+"  "+url+" "+n+" collections";
            b.add(s);
            break;
         }
      }
      
      return b;
   }


   
   /** Retourne le code de la cat�gorie des catalogues, null sinon (ex: CDS/I/246/out => I) */
   private String getCatCode(String id) { return Util.getSubpath(id,1); }
   
   /** retourne l'abbr�viation du journal (ex: CDS/J/A+A/171/261/table1 => A+A) */
   private String getJournalCode(String id) { return Util.getSubpath(id,2); }
   
   /** Retourne le suffixe de l'identificateur d'un catalogue => tous ce qui suit le code
    * de cat�gorie (ex: CDS/I/246/out => 246/out) */
   private String getCatSuffix(String id) { return Util.getSubpath(id,2,2); }
   
   /** Retourne le pr�fixe parent d'un identificateur de catalgoue => tout ce qui pr�c�de
    * le dernier mot
    * (ex: CDS/I/246/out => CDS/I/246) */
   private String getCatParent(String id) {
      int i=id.lastIndexOf('/');
      if( i>0 ) return id.substring(0,i);
      return null;
   }
   
   private final String [] CAT = {"Image","Data base","Catalog","Cube","Outreach","Unsupervised" };
   
   private final String [] CAT_CODE = { "I","II","III","IV","V","VI","VII","VIII","IX","B" };
   private final String [] CAT_LIB = {
         "I-Astrometric Data",
         "II-Photometric Data",
         "III-Spectroscopic Data",
         "IV-Cross-Identifications",
         "V-Combined data",
         "VI-Miscellaneous",
         "VII-Non-stellar Objects",
         "VIII-Radio and Far-IR data",
         "IX-High-Energy data",
         "B-External databases, regularly updated"
   };
   
   static protected final String MMOC = "Multiprop.bin";
   
   // Sauvegarde dans le cache du MultiMoc sous forme binaire
   private boolean cacheWrite() {
      try {
         String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
         (new BinaryDump()).save(multiProp, s);
         aladin.trace(3,"Multiprop stored in cache ["+s+"]");
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   // G�n�ration du MultiMoc depuis la sauvegarde binaire du cache
   private boolean cacheRead() {
      try {
         long t0 = System.currentTimeMillis();
         String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
         MultiMoc multiProp = (new BinaryDump()).load(s);
         this.multiProp = new MultiMoc2( multiProp );
         aladin.trace(3,"Multiprop loaded ("+multiProp.size()+" rec.) from cache ["+s+"] in "+(System.currentTimeMillis()-t0)+"ms...");
      } catch( Exception e ) {
         return false;
      }
      return true;
   }
   
   /** Retourne la date d'estampillage la plus tardive de l'ensemble des enregistrements
    * du multiProp. 0 si aucun */
   private long getMultiPropTimeStamp() {
      long max=0;
      for( MocItem mi : multiProp ) {
         long ts = mi.getPropTimeStamp();
         if( ts>max ) max=ts;
      }
      return max;
   }
   
   // Retourne la date du cache
   private long getCacheTimeStamp() {
      String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
      return (new File(s)).lastModified();
   }
   
   /** Retourne true si le HipsStore est pr�t */
   protected boolean dialogOk() {
      return !init /* !mocServerLoading && multiProp.size()>0  */;
   }
   
   /** Chargement des descriptions de l'arbre par le MocServer */
   private void initMultiProp() {

      // Tentative de rechargement depuis le cache
      if( cacheRead()  ) {
         startTimer();
         while( !init ) Util.pause(100);
         (new Thread("updateFromMocServer"){
            public void run() {
               quickFilter.setText(UPDATING);
               if( updateFromMocServer()>0  ) {
                  cacheWrite();
                  final ArrayList<TreeObjDir> tmpListReg = populateMultiProp();
                  SwingUtilities.invokeLater(new Runnable() {
                     public void run() {
                        replaceTree(tmpListReg);
                        stopTimer();
                     }
                  });
               } else stopTimer();
               quickFilter.setText("");
            }
         }).start();


         // Le cache est vide => il faut charger depuis le MocServer
      } else {

         // L'initialisation se fait en deux temps pour pouvoir laisser
         // l'utilisateur travailler plus rapidement
         String s = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
               +"&hips_service_url=*&fields=!obs_description,!hipsgen*&fmt=gzip-ascii&get=record";
         loadFromMocServer(s);
         
         startTimer();
         while( !init ) Util.pause(100);
         (new Thread("updateFromMocServer"){
            public void run() { 
               if( loadFromMocServer(MOCSERVER_INIT)>0 ) {
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
         loadMultiProp(in,addition,path);
         ArrayList<TreeObjDir> tmpDirList = populateMultiProp();
         rebuildTree(tmpDirList,  false,  true);
         dirList=tmpDirList;
      } catch( Exception e ) {
         if( aladin.levelTrace>3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   /** Demande de maj des enr. via le MocServer en passant en POST la liste des IDs des collections
    * que l'on connait d�j� + une date d'estampillage pour chacune d'elles. Si �a ne fonctionne
    * pas, on tentera une maj plus basique
    * @return le nombre de records charg�s
    */
   private int updateFromMocServer() {
      long t0 = System.currentTimeMillis();
      URL url;
      try {
         url  = aladin.glu.getURL("MocServer");
      } catch(Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return -1;
      }

      try {
         mocServerUpdating=true;
         MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
         String boundary = MultiPartPostOutputStream.createBoundary();
         URLConnection urlConn = MultiPartPostOutputStream.createConnection(url);
         urlConn.setRequestProperty("Accept", "*/*");
         urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
         // set some other request headers...
         urlConn.setRequestProperty("Connection", "Keep-Alive");
         urlConn.setRequestProperty("Cache-Control", "no-cache");
         MultiPartPostOutputStream out =  new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

         File tmpMoc = File.createTempFile("moc", ".txt");
         tmpMoc.deleteOnExit();

         BufferedWriter fo = new BufferedWriter( new FileWriter(tmpMoc));
         try {
            for( MocItem mi : this ) {
               String s = mi.mocId+"="+mi.getPropTimeStamp()+"\n";
               fo.write( s.toCharArray() );
            }
         } finally { try { fo.close(); } catch(Exception e) {} }

         out.writeField("fmt", "asciic");
         out.writeFile("maj", null, tmpMoc, true);
         out.close();
         aladin.trace(4,"ID list sent");

         int n=loadMultiProp( new InputStreamReader( urlConn.getInputStream() ), false, null );
         Aladin.trace(3,"Multiprop updated in "+(System.currentTimeMillis()-t0)+"ms => "+n+" record"+(n>1?"s":""));
         return n;

      } catch(Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         System.err.println("An error occured while updating the MocServer service => try the basic mode...");
         return updateFromMocServerBasic();
      } finally { mocServerUpdating=false; }
   }
   
//   /** Gunzippe le flux si n�cessaire */
//   private InputStream gunzipIfRequired(InputStream in) throws Exception {
//      PushbackInputStream pb = new PushbackInputStream( in, 2 );
//      byte[] signature = new byte[2];
//      pb.read( signature );
//      pb.unread( signature );
//      int head = ( signature[0] & 0xff ) | ( ( signature[1] << 8 ) & 0xff00 );
//      boolean isGziped = GZIPInputStream.GZIP_MAGIC == head;
//      if( !isGziped ) return pb;
//      return new GZIPInputStream(pb);
//   }   

   /** Demande de maj des enr. via le MocServer en fonction d'une seule date sp�cifique.
    * La date d'estampillage de r�f�rence sera la plus tardive de l'ensemble des enr. d�j� copi�s.
    * Et si aucun, ce sera la date du fichier de cache (avec un risque de d�calage des horloges
    * entre le MocServer et la machine locale.
    * Cette m�thode de maj ne permet pas la suppression des rec obsol�tes, ni le compl�ment de maj
    * si la pr�c�dente maj a �t� interrompue en cours.
    * @return  le nombre de records charg�s
    */
   private int updateFromMocServerBasic() {
      long ts = getMultiPropTimeStamp();
      if( ts==0L ) ts = getCacheTimeStamp();
      return loadFromMocServer("TIMESTAMP=>"+ts+"&fmt=asciic&get=record");
   }
   
   private int loadFromMocServer(String params) {
      InputStreamReader in=null;
      boolean eof=false;
      int n=0;
      
      String text = params.indexOf("TIMESTAMP")>=0 ? "updat":"load";

      // Recherche sur le site principal, et sinon sur un site miroir
      try {
         mocServerLoading = true;

         long t0 = System.currentTimeMillis();
         Aladin.trace(3,text+"ing Multiprop definitions from MocServer...");

         String u = aladin.glu.getURL("MocServer", params, true).toString();
         try {
            in = new InputStreamReader( Util.openStream(u,false,3000) );
            if( in==null ) throw new Exception("cache openStream error");

            // Peut �tre un site esclave actif ?
         } catch( EOFException e1 ) {
            eof=true;
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            try {
               in = new InputStreamReader( Util.openStream(u,false,-1) );
            } catch( EOFException e1 ) { eof=true; }
         }
         
         if( !eof ) n=loadMultiProp(in, false, null);
         Aladin.trace(3,"Multiprop "+text+"ed in "+(System.currentTimeMillis()-t0)+"ms => "+n+" record"+(n>1?"s":""));
         
      } catch( Exception e1 ) {
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
         return -1;
         
      } finally {
         mocServerLoading=false;
         if( in!=null ) { try { in.close(); } catch( Exception e) {} }
      }
      return n;
   }
   
   private String getId(String ivoid) {
      if( ivoid.startsWith("ivo://") ) return ivoid.substring(6);
      return ivoid;
   }
   
   
   /** Retourne le nombre max  du nombre de lignes des catalogues et tables */
   protected int getNbRowMax() {
      long max = 0L;
      for( MocItem mi : this ) {
         try {
            long n = Long.parseLong( mi.prop.get("nb_rows") );
            if( n>max ) max=n;
         } catch( Exception e) {}
      }
      return (int)( max );
   }
   
   /** Retourne le nombre d'items matchant la contrainte */
   protected int getNumber(String contrainte) {
      try {
         return multiProp.scan( contrainte ).size();
      } catch( Exception e ) { } 
      return -1;
   }
   

   /** true si on peut raisonnablement faire un updating des HiPS visibles dans la vue */
   private boolean isReadyForUpdating() {
      return !mocServerLoading && isVisible() && getWidth()>0;
   }

   protected boolean blinkState=true;
   
   /** R�affichage du Panel tous les demi-secondes afin de faire clignoter le message d'attente */
   private void startTimer() {
      if( timer==null ) {
         timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) { blinkState=!blinkState; repaint(); }
         });
      }
      if( !timer.isRunning() ) {
//         System.out.println("Timer started");
         timer.start();
      }
   }
   
   private void stopTimer() {
      if( timer!=null && timer.isRunning() ) {
//         System.out.println("Timer stopped");
         timer.stop();
      }
   }
   
   
//   public void paintComponent(Graphics g) {
//      
//      super.paintComponent(g);
//      
//      g.setFont( Aladin.BOLD );
//      
//      // Petit message pour avertir l'utilisateur que l'on charge des d�finitions
//      if( mocServerLoading ) {
//         String dot="";
//         for( int i = 0; i<blinkDot; i++ ) dot+=".";
//         blinkDot++;
//         if( blinkDot>10 ) blinkDot=0;
//         g.setColor(Aladin.BKGD);
//         String s = "Updating directory ("+nbRecInProgress+")"+dot;
//         g.drawString(s, 16, 25);
//         Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.orange);
//         
//      } else {
//         
//         g.setColor( Aladin.DARKBLUE );
//         int n=multiProp.size();
//         g.drawString("Directory : "+n+" collection"+(n>1?"s":""),16,25);
//         
//         // On fait clignoter le voyant d'attente d'info in/out
//         if( flagCheckIn ) {
//            startTimer();
//            Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.green );
//            
//         } else {
//            stopTimer();
//            Slide.drawBall(g, 2, 16, Color.green );
//         }
//      }
//   }
   
   
   /************** Pour faire la maj en continue des HiPS visibles dans la vue *******************************/
   
   transient private Thread threadUpdater=null;
   private boolean encore=true;

   private void startInsideUpdater() {
      if( threadUpdater==null ) {
         threadUpdater = new Updater("RegUpdater");
         threadUpdater.start();
      } else encore=true;
   }

   private void stopInsideUpdater() { encore=false; }

   class Updater extends Thread {
      public Updater(String s) { super(s); }

      public void run() {
         encore=true;
         //         System.out.println("Registry Tree updater running");
         while( encore ) {
            try {
               //               System.out.println("Hips updater checking...");
               if( isReadyForUpdating() ) resumeIn();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) { }
         }
         //         System.out.println("Registry Tree updater stopped");
         threadUpdater=null;
      }
   }

   
   /********* Classe g�rant une fen�tre d'information associ�e au HiPS cliqu� dans l'arbre des HiPS ***************/
   
   private class FrameInfo extends JFrame {
      
      ArrayList<TreeObjDir> treeObjs=null;     // hips dont il faut afficher les informations
      JPanel panelInfo=null;                // le panel qui contient les infos (sera remplac� � chaque nouveau hips)
      JCheckBox hipsBx=null,mocBx=null,mociBx=null,mocuBx,progBx=null,
                dmBx=null, siaBx=null, ssaBx=null, csBx=null, msBx=null, allBx=null, tapBx=null,xmatchBx=null;
      
      FrameInfo() {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
//         setAlwaysOnTop(true);
         pack();
      }
      
      public void setVisible(boolean flag) {
         
         // Si on efface la fen�tre popup, on resete aussi la m�morisation
         // de la liste des TreeObj concern� par le dernier affichage
         if( flag==false ) treeObjs = null;
         
         super.setVisible(flag);
      }
      
      boolean isSame(ArrayList<TreeObjDir> a1, ArrayList<TreeObjDir> a2) {
         if( a1==null && a2==null ) return true;
         if( a1==null || a2==null ) return false;
         if( a1.size() != a2.size() ) return false;
         for( TreeObjDir ti : a1) {
            if( !a2.contains(ti) ) return false;
         }
         return true;
      }
      
      
      /** Positionne les collections concern�es, et reg�n�re le panel en fonction */
      boolean setCollections(ArrayList<TreeObjDir> tos) {
         if( isSame(tos,treeObjs) ) { toFront(); return false; }
         this.treeObjs = tos;
         resumePanel();
         validate();
         SwingUtilities.invokeLater( new Runnable() {
            public void run() { pack(); toFront(); repaint();}
         });
         return true;
      }
      
      /** Reconstruit le panel des informations en fonction des collections courantes */
      void resumePanel() {
         JPanel contentPane = (JPanel)getContentPane();
         if( panelInfo!=null ) contentPane.remove(panelInfo);
         
         panelInfo = new JPanel( new BorderLayout() );
         panelInfo.setBorder( BorderFactory.createEmptyBorder(5, 5, 2, 5));
         
         String s;
         boolean flagScan=false;
         long nbRows=-1;
         MyAnchor a;
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g =  new GridBagLayout();
         c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
         c.insets = new Insets(2,2,0,5);
         JPanel p = new JPanel(g);
         
         TreeObjDir to=null;
         boolean hasView = !aladin.view.isFree();
         boolean hasMocPol = aladin.view.hasMocPolSelected();
         boolean hasRegion = aladin.calque.getNbPlanMoc()>0 || hasMocPol;
         boolean hasLoadedCat = aladin.calque.getNbPlanCat()>0;
         
         NoneSelectedButtonGroup bg = new NoneSelectedButtonGroup();
         
         if( treeObjs.size()>1 )  {
            a = new MyAnchor(aladin,treeObjs.size()+" data sets selected",50,null,null);
            a.setFont(a.getFont().deriveFont(Font.BOLD));
            a.setFont(a.getFont().deriveFont(a.getFont().getSize2D()+1));
            a.setForeground( Aladin.COLOR_GREEN );
            PropPanel.addCouple(p,null, a, g,c);
            StringBuilder list = null;
            String sList=null,more=null;
            boolean hasCS = false;
            boolean hasSIA = false;
            boolean hasSSA = false;
            boolean hasCDScat = false;
            boolean hasMoc = false;
            
            for( TreeObjDir to1 : treeObjs ) {
               if( list==null ) list = new StringBuilder(to1.internalId);
               else list.append(", "+to1.internalId);
               if( sList==null && list.length()>80 ) sList = list+"...";
               if( !hasCS && to1.hasCS() ) hasCS=true;
               if( !hasSIA && to1.hasSIA() ) hasSIA=true;
               if( !hasSSA && to1.hasSSA() ) hasSSA=true;
               if( !hasMoc && to1.hasMoc() ) hasMoc=true;
               if( !hasCDScat && to1.isCDSCatalog() ) hasCDScat=true;
               if( !flagScan && !to1.hasMoc() ) flagScan=true;
            }
            if( sList!=null ) more = list.toString();
            else sList=list.toString();
            
            JPanel mocAndMore = new JPanel( new FlowLayout(FlowLayout.CENTER,5,0));
            JCheckBox bx;
            mociBx = mocBx = csBx = null;

            if( hasCS ) {
               csBx = bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected(hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
               Util.toolTip(bx,"Cone search (CS) on the current view\nfor the selected collections");
               bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
               bg.add(bx);
            }
            
            if( hasSIA ) {
               siaBx= bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected(hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
               Util.toolTip(bx,"Simple Image Access (SIA) query\non the current view for the selected collections");
               bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
            }

            if( hasSSA ) {
               ssaBx= bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected(hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
               Util.toolTip(bx,"Simple Spectra Access (SSA) query \non the current view for the selected collections");
               bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
            }
            
            if( hasCDScat && hasRegion ) {
               msBx = bx = new JCheckBox("in region or MOC");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Load all sources inside the selected region or MOC\nfor the selected collections supporting this access query");
               bx.setEnabled( hasRegion );
               bg.add(bx);
               
            }

            if( (hasCS || hasSIA || hasSSA) && (hasMoc) ) {
               JLabel labelPlus = new JLabel(" + ");
               mocAndMore.add(labelPlus);
            }
            
            if( hasMoc ) {
               JLabel labelMoc = new JLabel("Coverages: ");
               labelMoc.setFont( labelMoc.getFont().deriveFont(Font.ITALIC));
               labelMoc.setForeground( Color.gray );
               mocAndMore.add( labelMoc );
               
               mocBx = bx = new JCheckBox("All"); 
               mocAndMore.add(bx); 
               Util.toolTip(bx,"Load all MOCs of the selected collections");

               mocuBx = bx = new JCheckBox("Union"); 
               mocAndMore.add(bx); 
               Util.toolTip(bx,"Load the union of MOCs of the selected collections");

               mociBx = bx = new JCheckBox("Intersection"); 
               mocAndMore.add(bx); 
               Util.toolTip(bx,"Load the intersection of MOCs of the selected collections");
            }

            if( mocAndMore.getComponentCount()>0 ) {
               PropPanel.addCouple(p,"", mocAndMore, g,c);
            }

            a = new MyAnchor(aladin,sList,100,more,null);
            a.setForeground(Aladin.COLOR_BLUE);
            PropPanel.addCouple(p,null, a, g,c);
            
         // Une seule collection
         } else {
         
            to = treeObjs.get(0);

            if( to.verboseDescr!=null || to.description!=null ) {
               s = to.verboseDescr==null ? "":to.verboseDescr;
               a = new MyAnchor(aladin,to.description,200,s,null);
               a.setFont(a.getFont().deriveFont(Font.BOLD));
               a.setFont(a.getFont().deriveFont( a.getFont().getSize2D()+1) );
               a.setForeground( Aladin.COLOR_GREEN );
               PropPanel.addCouple(p,null, a, g,c);
            }
            String provenance = to.copyright==null ? to.copyrightUrl : to.copyright;
            if( provenance!=null ) {
               s = "Provenance: "+provenance+" ";
               a = new MyAnchor(aladin,null,50,s,null);
               a.setForeground(Color.gray);
               PropPanel.addCouple(p,null, a, g,c);
            }

            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
            s  = to.getProperty(Constante.KEY_MOC_SKY_FRACTION);
            if( s!=null ) {
               boolean isIn = to.getIsIn()==1;
               try { s = Util.myRound( Double.parseDouble(s)*100); } catch( Exception e) {}
               s = "Sky coverage: "+s+"% ";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground( Color.gray );
               p1.add(a);
               
               if( hasView ) {
                  a.setToolTipText( isIn ? "Data available in the current view"
                        : "No data in the current view");
               }
               
            } else {
               s = "Coverage unknown (no available MOC)";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Aladin.ORANGE);
               p1.add(a);
               
            }
            s  = to.getProperty(Constante.KEY_HIPS_PIXEL_SCALE);
            if( s!=null ) {
               try { s = Coord.getUnit( Double.parseDouble(s)); } catch( Exception e) {}
               s = "    HiPS pixel scale: "+s+" ";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a);
            }
            s  = to.getProperty(Constante.KEY_NB_ROWS);
            if( s!=null ) {
               try { nbRows = Long.parseLong(s); } catch( Exception e) {}
               s = String.format("%,d", nbRows);
               s = "    Nb rows: "+s+" ";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a);
            }
            s  = to.getProperty("bib_year");
            if( s!=null ) {
               s = "    Reference pub. year: "+s+" ";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a);
            }


            if( p1.getComponentCount()>0 ) PropPanel.addCouple(p,null, p1, g,c);

            JPanel mocAndMore = new JPanel( new FlowLayout(FlowLayout.CENTER,5,0));
            JCheckBox bx;
            hipsBx = mocBx = mociBx = progBx = dmBx = csBx = siaBx = ssaBx = allBx = tapBx = xmatchBx = null;
            if( to.hasHips() ) {
               hipsBx = bx = new JCheckBox("prog.access");
               mocAndMore.add(bx);
               bx.setSelected(true);
               Util.toolTip(bx,"Hierarchical Progressive Survey (HiPS)\n => Load the progressive view of the data");
            }
             
            if( to.hasSIA() ) {
               siaBx = bx = new JCheckBox( "in view" );
               mocAndMore.add(bx);
               bx.setSelected( !to.hasHips() );
               String proto = to.hasSIAv2() ? "SIAv2" : "SIA";
               Util.toolTip(bx,"Simple Image Access ("+proto+")\n => load the list of images available in the current view",true);
           }
            
            if( to.hasSSA() ) {
               ssaBx = bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected( !to.hasHips() );
               Util.toolTip(bx,"Simple Spectra Access (SSA)\n => load the list of spectra available in the current view",true);
           }
            
            if( to.isCDSCatalog() ) {
               boolean allCat = nbRows<2000;
               if( hipsBx!=null ) bg.add(hipsBx);
               
               if( nbRows!=-1 && nbRows<100000 ) {
                  allBx = bx = new JCheckBox("all");
                  mocAndMore.add(bx);
                  bx.setSelected( !to.hasHips() && allCat );
                  Util.toolTip(bx,"Load all sources\n(only available for small catalogs/tables <100000)");
                  bg.add(bx);
               } 

               csBx = bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected( !to.hasHips() && !allCat);
               bx.setToolTipText("Load all sources covering the current view");
               bg.add(bx);
               
               msBx = bx = new JCheckBox("in region or MOC");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Load all sources inside the selected region or MOC");
               bx.setEnabled( hasRegion );
               bg.add(bx);
               
               xmatchBx = bx = new JCheckBox("via Xmatch");
               mocAndMore.add(bx);
               bx.setEnabled( hasLoadedCat );
               Util.toolTip(bx,"Cross-correlate the remote catalog/table\nwith the table selected in the stack",true);
               bg.add(bx);

               // Positionnement de la s�lection par d�faut
               boolean onRegion = aladin.calque.getFirstSelectedPlan() instanceof PlanMoc || hasMocPol;
               if( onRegion && msBx!=null ) msBx.setSelected(true);
               else if( allBx!=null && nbRows<10000 ) allBx.setSelected(true);
               else if( csBx.isEnabled() ) csBx.setSelected(true); 
               
            } else if( to.getCSUrl()!=null ) {
               csBx = bx = new JCheckBox("in view");
               mocAndMore.add(bx);
               bx.setSelected( !to.hasHips() );
               Util.toolTip(bx,"Load all sources covering the current view");
               bg.add(bx);
           }
            
            if( to.hasTAP() ) {
               tapBx = bx = new JCheckBox("by criteria");
               mocAndMore.add(bx);
               bx.setSelected( csBx==null );
               Util.toolTip(bx,"Table Access Protocol (TAP)\n => Advanced query by criteria",true);
               bg.add(bx);
            }
            
            JLabel labelPlus = new JLabel(" + ");
            mocAndMore.add(labelPlus);
            
            if( to.hasMoc() ) {
               mocBx = bx = new JCheckBox("Coverage"); 
               mocAndMore.add(bx); 
               Util.toolTip(bx,"Load the MultiOrder Coverage map (MOC) associated to the collection",true);
            }
            
            if( to.isCDSCatalog() && nbRows!=-1 && nbRows>=10000 ) {
               dmBx = bx = new JCheckBox("Density map");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Progressive view (HiPS) of the density map associated to the catalog",true);
            } else {
               if( to.getProgenitorsUrl()!=null ) {
                  progBx = bx = new JCheckBox("Progenitors");
                  mocAndMore.add(bx);
                  Util.toolTip(bx,"Load meta data and links to the original data images/cubes\nwhich have been used for generating the progressive view (HiPS)",true);
               }
            }
            PropPanel.addCouple(p,"", mocAndMore, g,c);
            
            // On supprime le label car il n'y a aucun produit annexe
            if( mocBx==null && dmBx==null && progBx==null ) labelPlus.setText(" ");

         }
         
         panelInfo.add(p,BorderLayout.CENTER);

         JPanel control = new JPanel( new FlowLayout(FlowLayout.CENTER,6,2) );
         control.setBackground( contentPane.getBackground() );
         
         Preview preview = null;
         
         JButton b;
         
         if( treeObjs.size()==1 ) {
            
            /* if( to.hasPreview() ) */ preview = new Preview( to );
            // Info
            if( to.hasInfo() ) {
               b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Info.png")));
               b.setToolTipText("Information on this collection");
               b.setMargin(new Insets(0,0,0,0));
               b.setBorderPainted(false);
               b.setContentAreaFilled(false);
               control.add(b);
               b.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) { info(); }
               });
            }
            
            // Bookmark
            b = new JButton(new ImageIcon(Aladin.aladin.getImagette("Bookmark.png")));
            Util.toolTip(b,"Bookmarks this collection query");
            b.setMargin(new Insets(0,0,0,0));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            control.add(b);
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { bookmark(); }
            });
            
         } else {
            
//            b = new JButton("Coll/Exp"); b.setMargin( new Insets(2,4,2,4));
//            Util.toolTip(b,"Collapse/Expand the selected collections in the tree",true);
//            control.add(b);
//            b.addActionListener(new ActionListener() {
//               public void actionPerformed(ActionEvent e) { iconCollapse.submit(); }
//            });
            
            if( flagScan ) {
               b = new JButton("Scan only"); b.setMargin( new Insets(2,4,2,4));
               b.setEnabled( hasView );
               Util.toolTip(b,"Check if the collections contains data in the current view",true);
               b.setFont(b.getFont().deriveFont(Font.BOLD));
               control.add(b);
               b.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     scan();
                     hideInfo();
                  }
               });
            }
            control.add(new JLabel("    "));
         }

         b = new JButton("Load"); b.setMargin( new Insets(2,4,2,4));
         b.setFont(b.getFont().deriveFont(Font.BOLD));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit();
               hideInfo();
            }
         });
         
         
         b = new JButton("Close"); b.setMargin( new Insets(2,4,2,4));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { hideInfo(); }
         });
         
         JPanel bas = new JPanel( new BorderLayout(0,0));
         bas.add(control,BorderLayout.CENTER);
         
         if( to!=null && to.internalId!=null ) {
            MyAnchor x = new MyAnchor(aladin,to.internalId,100,to.prop.getRecord(null),null);
            x.setForeground(Aladin.COLOR_BLUE);
            x.setFont( x.getFont().deriveFont(Font.BOLD));
            bas.add(x,BorderLayout.WEST);
         }
         
         contentPane.add(panelInfo,BorderLayout.CENTER);
         panelInfo.add(bas,BorderLayout.SOUTH);
         
         if( preview!=null ) panelInfo.add(preview,BorderLayout.WEST);
         contentPane.validate();
      }
      
      class Preview extends JPanel {
         String url;
         TreeObjDir to;
         
         Preview( TreeObjDir to ) {
            super();
            this.to = to;
            
            // D�j� fait ?
            if( to.previewError || to.imPreview!=null ) return; 
            
            // Pas de preview ?
            if( !to.hasPreview() ) {
               to.previewError=true;
               return;
            }
            
            this.url = to.getPreviewUrl();
            (new Thread(){
               public void run() { load(); }
            }).start();
         }
         
         void load() {
            
            System.out.println("Loading preview => "+url);
            MyInputStream is=null;
            try {
               is = new MyInputStream( Util.openStream(url));
               byte buf[] = is.readFully();
               if( buf.length==0 ) throw new Exception();
               Image im = Toolkit.getDefaultToolkit().createImage(buf);
               
               MediaTracker mt = new MediaTracker(this);        
               mt.addImage(im,0);
               mt.waitForAll();
               
               to.imPreview = im;

            } catch( Exception e) { to.previewError=true; }
            finally{ if( is!=null ) try { is.close(); } catch( Exception e1 ) {}} 
            repaint();
         }
         
         public Dimension getPreferredSize() { return new Dimension(88,88); }
         
         public void paintComponent(Graphics g) {
            paintComponent1(g);
            int w = getWidth();
            int h = getHeight();
            
            if( to.isNew() ) {
               g.setFont( Aladin.ITALIC );
               String s = "New "+(to.isNewObsRelease()?"release":to.isNewHips()?"HiPS":"!");
               int x=w/2-g.getFontMetrics().stringWidth(s)/2+3; 
               int y=13;
               Util.drawStar(g, x-4, y-6, Color.yellow);
               g.drawString(s,x,y);
            }
            
            if( to.getIsIn()!=-1 ) {
               boolean in = to.getIsIn()==1;
               g.setFont( Aladin.SPLAIN );
               String s = in ? "data in view" : "out of view";
               int x=w/2-g.getFontMetrics().stringWidth(s)/2+( in ? 0 : 3 ); 
               int y=h-4;
               Color c = in ? Aladin.COLOR_GREEN.brighter().brighter() : Aladin.ORANGE.brighter();
               if( !in ) Util.drawWarning(g, x-10, y-7, c, Color.black);
               g.setColor(c);
               g.drawString(s,x,y);
            }
         }
         private void paintComponent1(Graphics g) {
            super.paintComponent(g);
            int ws=getWidth();
            int hs=getHeight();
            g.setColor( Color.gray);
            g.fillRect(0, 0, ws, hs );
            
            if( to.previewError || to.imPreview==null ) {
               g.setColor( Color.white );
               String s = to.previewError ? "no preview" : "loading...";
               g.setFont( Aladin.ITALIC);
               java.awt.FontMetrics fm = g.getFontMetrics();
               g.drawString(s, ws/2-fm.stringWidth(s)/2 ,hs/2+4);
               return;
            }
            
            if( to.imPreview==null ) return;
            
            Image img = to.imPreview;
            
            int wi = img.getWidth(this);
            int hi = img.getHeight(this);
            boolean vertical = Math.abs(1-(double)hi/hs) < Math.abs(1-(double)wi/ws);
            double sx2,sy2;
            if( vertical ) { sy2 = hi; sx2 = ws * ((double)hi/hs); }
            else { sx2 = wi; sy2 = hs * ((double)wi/ws); }
            
            if( hi<hs ) g.translate(0,(hs-hi)/2);
            g.drawImage(img,0,0,ws,hs, 0,0, (int)sx2,(int)sy2, this);
            if( hi<hs ) g.translate(0,-(hs-hi)/2);
         }
      }
      
      private class NoneSelectedButtonGroup extends ButtonGroup {
         public void setSelected(ButtonModel model, boolean selected) {
           if (selected)  super.setSelected(model, selected);
           else clearSelection();
         }
       }
      
      // Ajout du bookmark qui correspond � la s�lection des checkboxes
      void bookmark() {
         StringBuilder bkm=new StringBuilder();
         TreeObjDir to = treeObjs.get(0);
         if( allBx!=null  && allBx.isSelected() )   addBkm( bkm, to.getAllBkm() );
         if( siaBx!=null  && siaBx.isSelected() )   addBkm( bkm, to.getSIABkm() );
         if( ssaBx!=null  && ssaBx.isSelected() )   addBkm( bkm, to.getSSABkm() );
         if( csBx!=null   && csBx.isSelected() )    addBkm( bkm, to.getCSBkm() );
         if( hipsBx!=null && hipsBx.isSelected() )  addBkm( bkm, to.getHipsBkm() );
         if( mocBx!=null  && mocBx.isSelected() )   addBkm( bkm, to.getMocBkm() );
         if( progBx!=null && progBx.isSelected() )  addBkm( bkm, to.getProgenitorsBkm() );
         
         // LES 3 AUTRES ACCES NE SONT PAS ACTUELLEMENT ACCESSIBLES PAR COMMANDE SCRIPT
         // => PAS DE BOOKMARK POSSIBLE
//         if( msBx!=null   && msBx.isSelected() )    to.queryByMoc();
//         if( dmBx!=null   && dmBx.isSelected() )    to.loadDensityMap();
//         if( tapBx!=null  && tapBx.isSelected() )   to.queryByTap();
         
         if( bkm.length()==0 ) return;
         
         String name = to.internalId;
         FrameBookmarks fb = aladin.bookmarks.getFrameBookmarks();
         fb.setVisibleEdit();
         fb.createNewBookmark( name,"$TARGET,$RADIUS","Load "+name+" on the view", bkm.toString() );
      }
      
      // Ajout d'une commande au bookmark en cours de construction
      private void addBkm( StringBuilder bkm, String cmd) {
         if( bkm.length()>0 ) bkm.append("\n");
         bkm.append("   "+cmd );
      }
      
      void info() {
         TreeObjDir to = treeObjs.get(0);
         String url = to.getInfoUrl();
         aladin.glu.showDocument(url);
      }
      
      void submit() {
         if( treeObjs.size()==0 ) return;
         
         // Interrogation par region, mais comme c'est un cercle, on fait directement un CS
         // plut�t qu'un query by MOC
         if( msBx!=null && msBx.isSelected() && aladin.view.vselobj.size()==1  ) {
            Obj o = aladin.view.vselobj.get(0);
            if( o instanceof Cercle || o instanceof SourceStat ) {
               try {
                  double ra = o.getRa();
                  double de = o.getDec();
                  double radius = o.getRadius();
                  for( TreeObjDir to : treeObjs ) to.loadCS(new Coord(ra,de),radius);
                  return;
               } catch( Exception e ) {}
            }
         }

         
         PlanMoc planMoc=null;
         if( msBx!=null && msBx.isSelected() ) {

            // Cr�ation d'un plan MOC � partir d'une r�gion s�lectionn�e
            // dans un plan tool
            if( aladin.view.hasMocPolSelected() ) {
               
               HealpixMoc moc=null;
               try { moc = aladin.createMocByRegions(-1); } 
               catch( Exception e ) { }
               if( moc!=null ) {
                  Coord c=aladin.calque.getTargetBG(null,null);
                  double rad=aladin.calque.getRadiusBG(null,null,null);
                  planMoc = new PlanMoc(aladin, moc, "moc",c,rad);
               } else {
                  aladin.warning("MOC creation failed !\nYour graphical region must be circles, and/or polygons counter-clock oriented");
                  return;
               }

            // D�termination du PlanMoc directement dans la pile
            } else {

               for( Object p : aladin.calque.getSelectedPlanes() ) {
                  if( p instanceof PlanMoc && ((PlanMoc) p).flagOk) { planMoc=(PlanMoc)p; break; }
               }
               if( planMoc==null ) {
                  for( Object p : aladin.calque.getPlans() ) {
                     if( p instanceof PlanMoc && ((PlanMoc) p).flagOk) { planMoc=(PlanMoc)p; break; }
                  }
               }
            }
            
            if( planMoc==null ) {
               aladin.warning("You need to select a graphical region or a MOC plane in the stack");
               return;
            }
         }

         // Acc�s � une collection
         if( treeObjs.size()==1 ) {
            TreeObjDir to = treeObjs.get(0);
            if( allBx!=null  && allBx.isSelected() )   to.loadAll();
            if( siaBx!=null  && siaBx.isSelected() )   to.loadSIA();
            if( ssaBx!=null  && ssaBx.isSelected() )   to.loadSSA();
            if( csBx!=null   && csBx.isSelected() )    to.loadCS();
            if( msBx!=null   && msBx.isSelected() )    to.queryByMoc(planMoc);
            if( hipsBx!=null && hipsBx.isSelected() )  to.loadHips();
            if( mocBx!=null  && mocBx.isSelected() )   to.loadMoc();
            if( progBx!=null && progBx.isSelected() )  to.loadProgenitors();
            if( dmBx!=null   && dmBx.isSelected() )    to.loadDensityMap();
            if( tapBx!=null  && tapBx.isSelected() )   to.queryByTap();
            if( xmatchBx!=null && xmatchBx.isSelected() )   to.queryByXmatch();
            
         // Acc�s � plusieurs collections simultan�ment
         } else {
            
            // CS
            if( csBx!=null  && csBx.isSelected() ) {
               for( TreeObjDir to : treeObjs ) {
                       if( to.hasSIA() ) to.loadSIA();
                  else if( to.hasSSA() ) to.loadSSA();
                  else to.loadCS();
               }
            }
            
            // MOC search
            if( msBx!=null && msBx.isSelected() ) {
               for( TreeObjDir to : treeObjs ) {
                  if( to.isCDSCatalog() ) to.queryByMoc(planMoc);
               }
            }
            
            // Union des MOCs
            if( mocBx!=null   && mocBx.isSelected() )  multiMocLoad(treeObjs,MultiMocMode.EACH);
            if( mocuBx!=null  && mocuBx.isSelected() ) multiMocLoad(treeObjs,MultiMocMode.UNION);
            if( mociBx!=null  && mociBx.isSelected() ) multiMocLoad(treeObjs,MultiMocMode.INTER);
         }
      }
      
      /** Chargement de chaque MOC ou de l'union ou intersection des Mocs */
      void multiMocLoad(ArrayList<TreeObjDir> treeObjs, MultiMocMode mode ) {
         
         // Chaque MOC individuellement
         if( mode==MultiMocMode.EACH ) {
            for( TreeObjDir to : treeObjs ) to.loadMoc();
            return;
         }
         
         // Union ou Intersection
         StringBuilder params = null;
         for( TreeObjDir to : treeObjs ) {
            if( params==null ) params = new StringBuilder(to.internalId);
            else params.append(","+to.internalId);
         }
         String cmd = (mode==MultiMocMode.INTER?"iMOCs":"MOCs")+"=get Moc("+Tok.quote(params.toString())+(mode==MultiMocMode.INTER?",imoc":"")+")";
         aladin.execAsyncCommand(cmd);
         
//         String label;
//         if( mode==MultiMocMode.UNION ) { params.append("&get=moc"); label="MOCs"; }
//         else { params.append("&get=imoc"); label="iMOCs"; }
//         
//         String u = aladin.glu.getURL("MocServer", params.toString(),true).toString();
//         aladin.execAsyncCommand(label+"=load "+u);
      }
   }
   
   static private enum MultiMocMode { EACH, UNION, INTER };
}

