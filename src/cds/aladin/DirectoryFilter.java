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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import cds.aladin.prop.PropPanel;
import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;
import cds.moc.MocCell;
import cds.moc.SMoc;
import cds.mocmulti.MocItem;
import cds.mocmulti.MultiMoc;
import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Classe qui gère le formulaire de filtrage de l'arbre HiPS
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public final class DirectoryFilter extends JFrame implements ActionListener {
   
   private static String SINTERSECT[];

   
   private Aladin  aladin;  // référence externe
   
   private String S(String k) { return aladin.chaine.getString(k); }
   
   private void loadStrings() {
      SINTERSECT = new String[] { S("FPOVERLAPS"), S("FPISENCLOSED"), S("FPCOVERS") };
   }
  
   public DirectoryFilter(Aladin aladin) {
      super();
      this.aladin = aladin;
      
      loadStrings();
      
      Aladin.setIcon(this);
      setTitle(S("FPTITLE"));
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);
      
      JPanel contentPane = (JPanel)getContentPane();
      contentPane.setLayout( new BorderLayout(5,5)) ;
//      setAlwaysOnTop(true);
      
      contentPane.add( getHeaderPanel(), BorderLayout.NORTH );
      contentPane.add( getMainFilterPanel(), BorderLayout.CENTER );
      contentPane.add( getValidPanel(), BorderLayout.SOUTH );
      contentPane.setBackground( BGCOLOR );
      
      pack();
   }
   
   private JPanel getHeaderPanel() {
      JButton b;
      
      JPanel storePanel = new JPanel( new FlowLayout(FlowLayout.CENTER,7,7) );
      storePanel.setBackground( BGCOLOR );
      storePanel.setBorder( BorderFactory.createEmptyBorder(0,20,0,0));
      storeButton=b=new JButton(S("FPSTORE"));
      Util.toolTip(storeButton, S("FPSTORETIP"),true);
      deleteButton=b=new JButton(S("FPDELETE"));
      Util.toolTip(deleteButton, S("FPDELETETIP"),true);
           
      JLabel l = new JLabel(S("FPNAME"));
      l.setFont( l.getFont().deriveFont(Font.BOLD));
      storePanel.add( l );
      nameField = new JTextField(20);
      nameField.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { updateWidget(); }
         public void keyPressed(KeyEvent e) {
         }
      });
      storePanel.add(nameField);
      
      storePanel.add( b=storeButton ); b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { store(); }
      });
      storePanel.add( b=deleteButton ); b.setEnabled(false);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { delete(); }
      });
      
      storePanel.add(new JLabel("      "));
      storePanel.add(Util.getHelpButton(this,S("FPHELP")));


      return storePanel;
   }
   
   private JTextArea exprArea;
   private JTextField mocArea;
   private JButton btMocShow;
   private JLabel labelIntersect,labelCollection;
   private boolean flagFormEdit=false;
   private SMoc mocFiltreSpatial=null;
   
   
   /** Création du panel de l'expression correspondant au filtre courant */
   private JPanel createExpPanel() {
      JPanel areaPanel = new JPanel( new BorderLayout(2,2) );
      areaPanel.setBackground( BGCOLOR );
      setTitleBorder(areaPanel, " "+S("FPRULELABEL")+" ", FGCOLOR);
      exprArea = new JTextArea(3,50);
      exprArea.setLineWrap(true);
      exprArea.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyPressed(KeyEvent e) {
            flagFormEdit=true;
            aladin.directory.flagError=false;
            updateWidget();
         }
         public void keyReleased(KeyEvent e) {
            if( e.getKeyCode()==KeyEvent.VK_ENTER ) submit();
         }
      });
      exprArea.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            activateAreaText(true);
         }
      });
      areaPanel.add( exprArea, BorderLayout.CENTER );
      
      JPanel mocPanel = new JPanel( new BorderLayout( 5,5));
      mocPanel.setBackground( BGCOLOR );
      labelIntersect = new JLabel( "MOC " );
      Util.toolTip(labelIntersect, S("FPMOCLABELTIP"), true);
      mocPanel.add( labelIntersect, BorderLayout.WEST );
      
      mocArea = new JTextField(46);
      mocArea.setEditable( false );
      mocArea.setForeground( new Color(80,80,80) );
      mocArea.setBackground( BGCOLOR );
      mocArea.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { flagFormEdit=true; updateWidget(); }
         public void keyReleased(KeyEvent e) {
            if( e.getKeyCode()==KeyEvent.VK_ENTER ) submit();
         }
      });
      mocArea.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            activateAreaText(true);
         }
      });
      mocPanel.add(mocArea, BorderLayout.CENTER );
      
      
      JPanel p = new JPanel( new FlowLayout(FlowLayout.CENTER,0,0) );
      p.setBackground( BGCOLOR );
      JButton bt = btMocShow = new JButton(S("FPSHOWIT"));
      Util.toolTip(bt, S("FPSHOWITTIP"), true);
      bt.setMargin( new Insets(2,2,2,2) ); 
      bt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            if( mocFiltreSpatial!=null ) aladin.calque.newPlanMOC(mocFiltreSpatial, "Moc spatial filter"); }
      });
      p.add(bt);
      mocPanel.add(p, BorderLayout.EAST );
      
      p = new JPanel( new BorderLayout());
      p.setBackground( BGCOLOR );
      p.add( mocPanel, BorderLayout.CENTER);
      
      JPanel p1 = new JPanel( new FlowLayout() );
      p1.setBackground( BGCOLOR );
      labelCollection = new JLabel();
      labelCollection.setFont(labelCollection.getFont().deriveFont(Font.BOLD));
      p1.add( labelCollection);
      
      p.add( p1, BorderLayout.SOUTH );
      areaPanel.add( p, BorderLayout.SOUTH );
      
      p = new JPanel( new BorderLayout());
      p.setBackground( BGCOLOR );
      p.setBorder( BorderFactory.createEmptyBorder(0,5,5,5));
      p.add( areaPanel, BorderLayout.CENTER);
      
      activateAreaText(false);
      return p;
   }
   
   /** Maj du résumé des collections qui matchent */
   protected void setLabelResume(int nb, int total,boolean hasQuickFilter) {
      boolean isEmpty = isEmpty();
      if( isEmpty && !hasQuickFilter ) {
         labelCollection.setText("<html>&rarr; "+S("FPNOACTIVATED")+" &rarr; "+S("FPACTIVATED2")+" "+total+"<html>");
         labelCollection.setForeground(new Color(80,80,80));
      }
      else {
         labelCollection.setText("<html>&rarr; "+S("FPACTIVATED")+" &rarr; "+S("FPACTIVATED1")+" <font size=\"+1\">"+nb+"</font><html>");
         labelCollection.setForeground( Aladin.COLOR_GREEN.darker() );
      }
   }
   
   private JButton storeButton, deleteButton;
   private JTextField nameField;
   
   /** Activation/desactivation des boutons en fonction du contenu du formulaire */
   synchronized protected void updateWidget() {
      
      String name = nameField.getText().trim();
      boolean enabled = name.length()>0 && !isEmpty();
      storeButton.setEnabled( enabled && !name.equals(Directory.ALLCOLL) );
      deleteButton.setEnabled( enabled && aladin.configuration.filterExpr.containsKey(name) 
            && !name.equals(Directory.ALLCOLL) && !name.equals(Directory.MYLIST));
      
      String expr = aladin.configuration.filterExpr.get(name);
      boolean modif = expr==null ? false : expr.equals( exprArea.getText().trim() );
      storeButton.setText( modif ? S("FPUPDATE") : S("FPSTORE") );
      
      labelIntersect.setForeground( mocArea.getText().trim().length()>0 ? Color.black : FGCOLOR );
      
      // Maj du JComboBox de la liste des plans MOC
      ActionListener al = comboMocPlane.getActionListeners()[0];
      comboMocPlane.removeActionListener(al);
      Object o = comboMocPlane.getSelectedItem();
      comboMocPlane.removeAllItems();
      Vector<Plan> v = aladin.calque.getPlans( PlanMoc.class );
      if( v!=null ) {
         for( Plan p: v  ) comboMocPlane.addItem( p.label );
         if( o!=null ) comboMocPlane.setSelectedItem(o);
         comboMocPlane.setEnabled( true );
         cbMocPlane.setEnabled( true );
      } else {
         comboMocPlane.addItem(" ------ ");
         comboMocPlane.setEnabled( false );
         cbMocPlane.setEnabled( false );
         
      }
      comboMocPlane.addActionListener(al);
      
      // Y a-t-il un graphique sélectionnée
      cbSelectedGraph.setEnabled( aladin.view.hasMocPolSelected() );
      
      if( flagFormEdit && !exprArea.getText().trim().equals("") ) activateAreaText(true);
      else activateAreaText(false);
      
//      btMocShow.setEnabled( mocFiltreSpatial!=null ); 
      
      btReset.setEnabled( !isEmpty() );
      btApply.setEnabled( !hasBeenApplied() );
      btMocShow.setEnabled( mocArea.getText().length()>0 );
      
      if( isVisible() ) aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
   }
   
   private void activateAreaText(boolean flag) {
      if( flag ) {
         exprArea.setForeground( aladin.directory.flagError ? Color.red : Aladin.COLOR_GREEN.darker() );
         exprArea.setBackground( Color.white );
         exprArea.getFont().deriveFont(Font.BOLD);

     } else {
         exprArea.setForeground( aladin.directory.flagError ? Color.red : new Color(80,80,80) );
         exprArea.setBackground( BGCOLOR );
         exprArea.getFont().deriveFont(Font.ITALIC);
      }
   }
   
   /** Mémorisation + activation du filtre courant */
   private void store() {
      String name = nameField.getText().trim();
      if( name.equals(Directory.ALLCOLL) || name.equals(Directory.MYLIST) ) {
         aladin.error(this,S("FPSTOREWARNING"));
         return;
      }
      String expr = exprArea.getText().trim();
      
      aladin.configuration.setDirFilter(name, expr, mocFiltreSpatial );
      aladin.configuration.filterExpr.remove(Directory.MYLIST);
      aladin.directory.updateDirFilter();
      aladin.directory.comboFilter.setSelectedItem(name);
   }
   
   /** Suppression de la mémorisation du filtre courant */
   private void delete() {
      String name = nameField.getText().trim();
      if( name.equals(Directory.ALLCOLL) ) return;
      if( name.equals(Directory.MYLIST) ) return;
      aladin.configuration.filterExpr.remove(name);
      aladin.directory.updateDirFilter();
      reset();
      nameField.setText("");
   }
   
   
   private Color BGCOLOR = new Color(220,220,220);
   private Color FGCOLOR = new Color(150,150,150);
   
   /** Construction du panel qui contient les tabs des différents filtres + le panel de l'expression brute */
   private JSplitPane getMainFilterPanel() {
      JPanel p =  createExpPanel();
      MySplitPane pane = new MySplitPane(aladin, JSplitPane.VERTICAL_SPLIT, createFilterPanel(),p , 1);
      p.setPreferredSize(new Dimension(0,120));

      pane.setBackground( BGCOLOR );
      return pane;
   }

   /** Construction du panel des boutons de validation */
   private JPanel getValidPanel() {
      JPanel p = new JPanel();
      p.setBackground( BGCOLOR );
      p.setLayout( new BorderLayout(10,10) );
      JButton b;
      
      JPanel applyPanel = new JPanel( new FlowLayout( FlowLayout.CENTER,7,7 ) );
      applyPanel.setBackground( BGCOLOR );
      
      b= btApply = new JButton(S("FPAPPLY"));
      applyPanel.add( b ); 
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(); }
      });
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      b= btReset = new JButton(S("FPRESET"));
      applyPanel.add( b );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { reset(); }
      });
      
      JPanel closePanel = new JPanel( new FlowLayout( FlowLayout.CENTER,7,7 ) );
      closePanel.setBackground( BGCOLOR );
      closePanel.add( b=new JButton(S("FPCLOSE")));
      closePanel.setBorder( BorderFactory.createEmptyBorder(0,0,0,20));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { setVisible(false); }
      });
      
      p.add( applyPanel, BorderLayout.CENTER );
      p.add( closePanel, BorderLayout.EAST );
      return p;
   }
   
   
   public void setVisible(boolean visible) {
      // Si on ferme la fenêtre des filtres sans qu'il n'y ait aucune contrainte, on remet le selecteur
      // de filtres de la fenêtre principale au choix par défaut
      if( !visible && isEmpty() ) {
         aladin.directory.comboFilter.setSelectedIndex(0);
         aladin.directory.iconFilter.repaint();
      }
      super.setVisible(visible);
   }
   
   static protected <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map, int ascending) {
      Comparator<K> valueComparator =  new Comparator<K>() {         
         private int ascending;
         public int compare(K k1, K k2) {
            int compare = map.get(k2).compareTo(map.get(k1));
            if (compare == 0) return 1;
            else return ascending*compare;
         }
         public Comparator<K> setParam(int ascending) {
            this.ascending = ascending;
            return this;
         }
      }.setParam(ascending);

      Map<K, V> sortedByValues = new TreeMap<>(valueComparator);
      sortedByValues.putAll(map);
      return sortedByValues;
   }

   static protected <K extends Comparable<K>, V> Map<K, V> sortAlpha(final Map<K, V> map, int ascending) {
      Comparator<K> valueComparator =  new Comparator<K>() {         
         private int ascending;
         public int compare(K k1, K k2) {
            int compare = k2.compareTo(k1);
            if (compare == 0) return 1;
            else return ascending*compare;
         }
         public Comparator<K> setParam(int ascending) {
            this.ascending = ascending;
            return this;
         }
      }.setParam(ascending);

      Map<K, V> sortedByValues = new TreeMap<>(valueComparator);
      sortedByValues.putAll(map);
      return sortedByValues;
   }

   
   static protected <K extends Comparable<K>, V> Map<K, V> sortList(final Map<K, V> map, final String [] list) {
      Comparator<K> valueComparator =  new Comparator<K>() {         
         public int compare(K k1, K k2) {
            int i = Util.indexInArrayOf((String)k1, list,true);
            int j = Util.indexInArrayOf((String)k2, list,true);
            if( i==j ) return 0;
            if( i==-1 ) return 1;
            if( j==-1 ) return -1;
            return i-j;
         }
         public Comparator<K> setParam() {
            return this;
         }
      }.setParam();

      Map<K, V> sortedByValues = new TreeMap<>(valueComparator);
      sortedByValues.putAll(map);
      return sortedByValues;
   }
   
   static private final int SORT_NO     = 0;
   static private final int SORT_FREQ   = 1;
   static private final int SORT_ALPHA  = 2;
   static private final int SORT_LIST   = 3;
   
   static private final String [] REGIME = { "Radio" , "Millimeter" , "Infrared" , "Optical" , "UV" , "EUV" , "X-ray" , "Gamma-ray" };
   
   /**
    * 
    * @param vBx
    * @param max
    * @param split
    * @param key
    * @param delim
    * @param sort 0-non trié, 1-décroissant en fréquence, 2-alphabétique sur label
    * @return
    */
   private JPanel createFilterBis( final Vector<JCheckBox> vBx, int max, boolean split, String key, String delim, int sort) {
      Map<String, Integer> map = new HashMap<>();
      
      // On décompte les occurences
      int total=0;
      for( MocItem mi : aladin.directory ) {
         Iterator<String> it = mi.prop.getIteratorValues(key);
         if( it==null ) continue;
         while( it.hasNext() ) {
            String v = it.next();
            
            // Si un délimiteur est indiqué, seul le préfixe repéré
            // est pris en compte (jusqu'au délimiteur)
            if( delim!=null ) {
               int i = v.indexOf(delim);
               if( i>0 ) v = v.substring(0,i);
            }
            Integer ni = map.get(v);
            int n = ni==null ? 0 : ni.intValue();
            map.put(v,new Integer(n+1));
            total++;
         }
      }
      
      // On trie 
      Map<String, Integer> map1  = sort==SORT_FREQ ? sortByValues(map, 1) 
                  : sort==SORT_ALPHA ? sortAlpha(map,-1) 
                  : sort==SORT_LIST ? sortList(map, REGIME ) 
                  : map;
      
      int p=0, i=0;
      for( String k : map1.keySet() ) {
         int n = map.get(k);
         
         // Ne retient pas les catégorie à un seul élément
         if( max==-2 && n==1 ) continue;

         // On génère chaque label de checkbox
         String lab = k;
         lab=lab.replace('_',' ');

         // si le label est trop grand => on coupe (slit=true)
         if( split && lab.length()>11 ) lab = lab.substring(0, 8)+"...";

         JCheckBox bx = new JCheckBox( lab, false);
         bx.setToolTipText(k+": "+n+" item"+(n>1?"s":""));

         // Positionnement de l'action correspondante
         String vm =  k + (delim==null?"":"*");
         bx.setActionCommand(key+"="+vm);
         bx.addActionListener(this);

         // Mémorisation de cette checkbox
         vBx.add( bx );

         p+=n;
         
         // On a fini ?
         i++;
         if( max>0 && i>=max ) break;
      }
      
      // On met tout ça dans un panel
      JPanel panel= new JPanel( new BorderLayout(0,0) );
      
      // Peu de checkboxes => une simple grille 
      if( vBx.size()<12 ) {
         panel.setLayout( new GridLayout(0,5) );
         panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) panel.add(bx);
         
      // Beaucoup d'éléments => deux colonnes avec scroll
      } else {
         JPanel p1 = new JPanel( new GridLayout(0,3) );
         for( JCheckBox bx : vBx )  p1.add(bx);
        
         JScrollPane scrollPane = new JScrollPane(p1, 
               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setPreferredSize( new Dimension(320,92) );
         panel.add( scrollPane, BorderLayout.CENTER);
         panel.setBorder( BorderFactory.createEmptyBorder(3, 0, 3, 3));
      }
      
      return panel;
   }
   
//   private JPanel createFilter( final Vector<JCheckBox> vBx, int max, String key, String delim, boolean addLogic) {
//      Map<String, Integer> map = new HashMap<String, Integer>();
//      
//      int total=0;
//      for( MocItem mi : aladin.directory ) {
//         Iterator<String> it = mi.prop.getIteratorValues(key);
//         if( it==null ) continue;
//         while( it.hasNext() ) {
//            String v = it.next();
//            if( delim!=null ) {
//               int i = v.indexOf(delim);
//               if( i>0 ) v = v.substring(0,i);
//            }
//            Integer ni = map.get(v);
//            int n = ni==null ? 0 : ni.intValue();
//            map.put(v,new Integer(n+1));
//            total++;
//         }
//      }
//      
//      Map<String, Integer> map1  = max==-1 ? sortAlpha(map,-1) : sortByValues(map, 1);
//      
//      int p=0;
//      StringBuilder others=new StringBuilder();
//      for( String k : map1.keySet() ) {
//        int n = map.get(k);
//        String lab = k;
//        if( max<0 ) lab=lab.replace('_',' ');
//        else if( lab.length()>11 ) lab = lab.substring(0, 8)+"...";
//        
//        JCheckBox bx = new JCheckBox( lab, max>=0);
////        bx.setToolTipText(k);
//        String vm = (max>0 && addLogic ? "!":"")+ k + (delim==null?"":delim+"*");
//        bx.setActionCommand((addLogic?"":"-")+key+"="+vm);
//        bx.addActionListener(this);
//        setToolTip(bx);
//        vBx.add( bx );
//        if( max>0 ) {
//           if( others.length()>0 ) others.append(',');
//           others.append(vm);
//        }
//        
//        p+=n;
//        if( max>0 && vBx.size()>=max ) {
//           if( p<total ) {
//              bx = new JCheckBox("Others",true);
//              bx.setActionCommand((addLogic?"-":"")+key+"="+others);
//              bx.addActionListener(this);
//              vBx.add( bx );
//           }
//           break;
//        }
//      }
//      
//      JPanel panel= new JPanel( new BorderLayout(0,0) );
//      
//      if( vBx.size()<12 ) {
//         panel.setLayout( new GridLayout(0,4) );
////         panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
//         for( JCheckBox bx : vBx ) panel.add(bx);
//         
//      } else {
//         JPanel p1 = new JPanel( new GridLayout(0,2) );
////         p1.setBorder( BorderFactory.createLineBorder(Color.lightGray));
//         for( JCheckBox bx : vBx ) {
////            bx.setText( bx.getToolTipText() );
//            p1.add(bx);
//         }
//        
//         JScrollPane scrollPane = new JScrollPane(p1, 
//               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//         scrollPane.setPreferredSize( new Dimension(320,95) );
//         panel.add( scrollPane, BorderLayout.CENTER);
//         panel.setBorder( BorderFactory.createEmptyBorder(3, 0, 3, 3));
//      }
//      
////      JButton b = new JButton("none");
////      JPanel p1 = new JPanel( new BorderLayout(0,0) );
////      p1.add(b,BorderLayout.CENTER);
//      
//      JButton b;
//      JPanel px = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
//      px.add(b = new JButton("none"));
//      
//      if( max>0 ) panel.add( px );
//      b.addActionListener(new ActionListener() {
//         public void actionPerformed(ActionEvent e) {
//            boolean flag = ((JButton)e.getSource()).getText().equals("none");
//            ((JButton)e.getSource()).setText( flag? "all" : "none" );
//            for( JCheckBox bx : vBx ) bx.setSelected(!flag);
//         }
//      });
//      
//      return panel;
//   }
   
   private void addParam( HashMap<String, String []> params, String key, String val ) {
      String v [] = params.get(key);
      if( v==null || v.length==0 ) params.put(key,new String[]{val});
      else {
         String [] v1 = new String[ v.length+1 ];
         System.arraycopy(v, 0, v1, 0, v.length);
         v1[v1.length-1]=val;
         params.put(key,v1);
      }
   }
   
   private void addParam( HashMap<String, String []> inclu, HashMap<String, String []> exclu, String s) {
      int i = s.indexOf('=');
      if( i==-1 ) return;
      String val = s.substring(i+1);
      if( s.charAt(0)=='-' ) {
         addParam( exclu, s.substring(1,i), val );
      } else {
         addParam( inclu, s.substring(0,i), val );
      }
   }
   
      
   
   /** Excécution du filtrage, soit à partir du contenu du formulaire, soit directement à partir
    * de l'expression saisie directement dans le champ exprArea */
   private void submit() {
      
      // Faut-il mettre à jour l'expression de filtrage en fonction du formulaire ?
      if( !flagFormEdit ) {
         generateExpression();
         generateMoc();
      } else {
         
         // PAS D'EDITION DIRECTE POSSIBLE
//         String smoc = mocArea.getText().trim();
//         try {
//            if( smoc.length()==0 ) mocFiltreSpatial=null;
//            else mocFiltreSpatial = new SMoc(smoc);
//         } catch( Exception e ) {
//            mocArea.setText( MOCERROR );
//            mocFiltreSpatial=null;
//         }
      }
      
      // updateWidget();
      
      flagFormEdit=false;
      
      String expr = exprArea.getText();
      if( expr.trim().length()==0 ) expr="*";
      
      if( isVisible() ) aladin.makeCursor(this, Aladin.WAITCURSOR);

      aladin.directory.resumeFilter(expr, mocFiltreSpatial, getIntersect( mocFiltreSpatial) );
      updateWidget();
      
      // mémorisation de l'expression s'il s'agit du MYLIST
      if( aladin.directory.comboFilter.getSelectedItem().equals(Directory.MYLISTHTML) ) {
         aladin.configuration.setDirFilter(Directory.MYLIST, expr, mocFiltreSpatial);
      }
      
   }
   
   
   /** Génération du MOC de filtrage correspondante aux positionnements des checkboxes et autres
    * champs de saisie. Le résultat est stocké dans le champ mocArea */
   private void generateMoc() {
      mocFiltreSpatial=getMoc();
      mocArea.setText( getASCII(mocFiltreSpatial) );
   }
   
   /** Génération du MOC de filtrage correspondante aux positionnements des checkboxes et autres
    * champs de saisie. */
   private SMoc getMoc() {
      SMoc moc=null;
      
      try {
         if( cbMocInLine.isSelected() ) {
            String s = tMoc.getText().trim();
            moc = s.length()==0 ? null : new SMoc( s );
         }
         
         else if( cbStcInLine.isSelected() ) {
            List<STCObj> stcObjects = new STCStringParser().parse( tSTC.getText().trim() );
            moc = aladin.createMocRegion(stcObjects,-1,true);
         }
         
         else if( cbMocPlane.isSelected() ) {
            String  label = (String)comboMocPlane.getSelectedItem();
            PlanMoc p = (PlanMoc) aladin.calque.getPlan( label );
            moc = (SMoc)( p==null ? null : p.getMoc() );
         }
         
         else if( cbSelectedGraph.isSelected() ) {
            moc = aladin.createMocByRegions(-1,true);
         }
         
      } catch( Exception e ) {
         moc=null;
      }
      
      if( moc!=null ) setIntersect(moc, comboIntersecting.getSelectedIndex() );
      
      return moc;
   }
   
   static public void setIntersect( SMoc moc, int intersect ) {
      if( moc==null ) return;
      String value = intersect==MultiMoc.OVERLAPS ? null : MultiMoc.INTERSECT[ intersect ];
      try {
         moc.setProperty("intersect", value);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   static public int getIntersect( SMoc moc ) {
      if( moc==null ) return -1;
      String s = moc.getProperty("intersect");
      return s==null ? MultiMoc.OVERLAPS : Util.indexInArrayOf(s,MultiMoc.INTERSECT,true);
   }
   
   /** Return the ASCII basic representation of a MOC  */
   static public String getASCII(SMoc moc ) { return getASCII(moc,40); }
   static public String getASCII(SMoc moc, int nbChars) {
      if( moc==null ) return "";
      StringBuffer s = new StringBuffer();
      long oOrder=-1;
      Iterator<MocCell> it = moc.iterator();
      while( it.hasNext() ) {
         MocCell x = it.next();
         if( x.order!=oOrder ) s.append(" "+x.order+"/");
         else s.append(",");
         s.append(x.npix);
         oOrder=x.order;

         if( s.length()>nbChars-4 ) { s.append(" ..."); break; }
      }
      
      int intersect = getIntersect(moc);
      if( intersect>=0 ) s.append(" ("+SINTERSECT[intersect]+")");
      return s.toString();
   }
   
   /** Génération de l'expression de filtrage correspondante aux positionnements des checkboxes et autres
    * champs de saisie. Le résultat est stocké dans le champ exprArea */
   private void generateExpression() {
      exprArea.setText( getExpression() );
   }

   
   /** Génération de l'expression de filtrage correspondante aux positionnements des checkboxes et autres
    * champs de saisie. */
   private String getExpression() {
      
      HashMap<String, String []> inclu = new HashMap<>();
      HashMap<String, String []> exclu = new HashMap<>();
      
      StringBuilder special = new StringBuilder();
      
      if( bxHiPS.isSelected() )      special.append(" && hips_service_url=*"); 
      if( bxSIA.isSelected() )       special.append(" && sia*=*");  
      if( bxSIA2.isSelected() )      special.append(" && sia2*=*");  
      if( bxSSA.isSelected() )       special.append(" && ssa*=*");  
      if( bxTAP.isSelected() )       special.append(" && tap*=*");  
      if( bxCS.isSelected() )        special.append(" && cs*=*");   
      if( bxMOC.isSelected() )       special.append(" && moc*=*");   
      if( bxTMOC.isSelected() )      special.append(" && tmoc*=*");   
      if( bxProg.isSelected() )      special.append(" && hips_progenitor_url=*");   
      
      if( bxPixFull.isSelected() )    special.append(" && (hips_tile_format=*fits* || dataproduct_type=!Image)");
      if( bxPixColor.isSelected() )   special.append(" && (dataproduct_subtype=color || dataproduct_type=!Image)");
      
      if( bxSuperseded.isSelected() )   special.append(" &! obs_superseded_by=*");

      for( JCheckBox bx : catVbx )    if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      for( JCheckBox bx : regVbx )    if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      for( JCheckBox bx : authVbx )   if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      
      HashMap<String, String []> inclu1 = new HashMap<>();
      for( JCheckBox bx : catMisVbx ) if( bx.isSelected() )  addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<>();
      for( JCheckBox bx : catkeyVbx ) if( bx.isSelected() )  addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<>();
      for( JCheckBox bx : assdataVbx ) if( bx.isSelected() ) addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<>();
      for( JCheckBox bx : catUcdVbx ) if( bx.isSelected() ) addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      String s;      
      if( (s=getText(tfDescr)).length()!=0 )     special.append(" && obs_title,obs_description,obs_collection,ID="+jokerize(s));
      if( (s=getText(tfCoverage)).length()!=0 )  special.append(" && moc_sky_fraction="+getFromPourcent(s));
      
      if( (s=getText(tfHiPSorder)).length()!=0 ) special.append(" && (hips_order="+s+"|| hips_service_url=!*)");
      if( (s=getText(tfCatNbRow)).length()!=0 )  special.append(" && (dataproduct_type!=catalog || nb_rows="+s+")");
      
      if( (s=getMJD(tfMinDate)).length()!=0 )   special.append( " && (t_min>="+s+" || t_min!=*)");
      if( (s=getMJD(tfMaxDate)).length()!=0 )   special.append( " && (t_max<="+s+" || t_max!=*)");
      if( (s=getText(tfBibYear)).length()!=0 )  special.append( " && bib_year="+s);
      
      
//      int v;
//      if( (v=slRow.getValue())!=slRow.getMaximum() ) special.append(" && (dataproduct_type!=catalog || nb_rows<="+v*1000+")");
      
      String incluS = rebuildInclu(inclu);
      String excluS = rebuildExclu(exclu);
      
      String expr="";
      if( excluS.length()>0 ) {
         if( incluS.length()>0 ) expr = "("+incluS+") && "+excluS;
         else expr = excluS;
      } else {
         if( incluS.length()>0 ) expr = incluS;
      }
      if( special.length()>0 ) {
         if( expr.length()>0 ) expr = "("+expr+")"+special;
         else {
            if( special.toString().startsWith(" && ") ) expr = special.substring(4);
            else expr = "*"+special;
         }
      }
      
//      String expr = incluS.length()==0 ? "*" : incluS;
//      if( excluS.length()>0 ) expr = "("+expr+") && ("+excluS+")";
//      if( special.length()>0 ) expr="("+expr+")"+special;
            
      return expr;
   }
   
   /** Retourne un pourcentage nnn% en valeur entre 0 et 1, à moins que ce ne soit déjà le cas */
   private String getFromPourcent(String s) {
      int i = s.lastIndexOf('%');
      int j;
      for( j=0; j<s.length() && !Character.isDigit( s.charAt(j) ); j++);
      double n;
      if( i==-1 ) n = Double.parseDouble(s.substring(j).trim());
      else n = Double.parseDouble( s.substring(j,i).trim());
      n/=100.;
      return s.substring(0,j)+n;
   }
   
   /** Retourne la date MJD d'une date écrite en clair */
   private String getMJD( JTextField d) {
      String s1 = getText(d);
      if( s1.length()==0 ) return s1;
      try { Long.parseLong(s1); return s1; }
      catch( Exception e) {}
      String s;
      try {
         //         s = aladin.dialog.aladinServer.setDateInMJDFormat(false,s1,null);

         double Yd = Double.valueOf( s1 ).doubleValue();
         s=Astrodate.JDToDate(Astrodate.YdToJD(Yd));

         if( s==null ) return "";
      } catch( Exception e ) {
         return "";
      }
      return s;
   }
   
   /** Parcours une liste de mots séparés par des virgules (,) ou des pipes (|)
    * et insère de part et d'autre le joker '*'
    * Ajoute des quotes si nécessaire
    * ex: 2MASS|US(NO) => "*2MASS*,*US(NO)*"
    */
   static protected String jokerize(String s) {
      Tok tok = new Tok(s,",|");
      StringBuilder s1 = null;
      while( tok.hasMoreTokens() ) {
         if( s1==null ) s1 = new StringBuilder("*"+tok.nextToken().trim());
         else s1.append("*,*"+tok.nextToken().trim());
      }
      s=s1.toString()+"*";
      if( s.indexOf("(")>=0 || s.indexOf(")")>=0 
            || s.indexOf("&&")>=0 ||  s.indexOf("||")>=0 || s.indexOf("&!")>=0 ) s = Tok.quote(s); 
      return s;
   }
   
   private String rebuildInclu(HashMap<String,String[]> exclusion) {
      StringBuilder expr = new StringBuilder();
      for( String k : exclusion.keySet() ) {
         if( expr.length()>0 )  expr.append(" && ");
         expr.append(k+"=");
         boolean first=true;
         for( String v : exclusion.get(k)) {
            if( !first ) expr.append(",");
            first=false;
            expr.append(v);
         }
      }
      return expr.toString().trim();
   }

   private String rebuildExclu(HashMap<String,String[]> exclusion) {
      StringBuilder expr = new StringBuilder();
      for( String k : exclusion.keySet() ) {
         if( expr.length()>0 )  expr.append(" && ");
         expr.append(k+"!=");
         boolean first=true;
         for( String v : exclusion.get(k)) {
            if( !first ) expr.append(",");
            first=false;
            expr.append(v);
         }
      }
      return expr.toString().trim();
   }
   
   /** Retourne le string d'un JtextField encodé en HTTP */
   private String getText(JTextField tf) { return tf.getText().trim(); }
   
   /** Affichage du panel pour permettre à l'utilisateur de modifier son filtre */
   public void showFilter() {
      Point p = aladin.getLocationOnScreen();
      p.x+=aladin.directory.getWidth()+30;
      p.y+=20;
      setLocation( p );
      setVisible(true);
      toFront();
   }
   
   class JTextFieldX extends JTextField {
      JTextFieldX(int n) {
         super(n);
         addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) { }
            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) {
               if( e.getKeyCode()==KeyEvent.VK_ENTER ) submitAction(true);
            }
         });
      }
      public Dimension getMinimumSize() { return new Dimension( 200, super.getMinimumSize().height); }
   }
   
   class JLabelX extends JLabel {
      JLabelX(String s) { super(s); }
      public Dimension getMinimumSize() { return new Dimension( 400, super.getMinimumSize().height); }
   }
   
   private JCheckBox cbMocPlane,cbMocInLine,cbStcInLine,cbSelectedGraph;
   private JCheckBox bxPixFull,bxPixColor,bxHiPS,bxSIA,bxSIA2,bxSSA,bxTAP,bxCS,bxMOC,bxTMOC,bxProg,bxSuperseded;
   private JTextFieldX tfCatNbRow,tfCoverage,tfHiPSorder,tfDescr,tfMinDate,tfMaxDate,tfBibYear;
   private JTextArea tMoc,tSTC;
   private Vector<JCheckBox> catVbx,authVbx,regVbx,catkeyVbx,catMisVbx,assdataVbx,catUcdVbx;
   private JComboBox<String> comboMocPlane,comboIntersecting;
   private JButton btReset,btApply;
   
   private NoneSelectedButtonGroup spaceBG;
   
   /** Retourne true si aucune contrainte n'est active */
   protected boolean isEmpty() {
      return exprArea.getText().trim().length()==0 && mocFiltreSpatial==null;
   }
   
   /** Retourne true si le filtre a déjà été appliquée */
   protected boolean hasBeenApplied() {
      boolean rep = hasBeenApplied1();
      return rep;
   }
   protected boolean hasBeenApplied1() {
      if( flagFormEdit ) return false;

      SMoc moc = getMoc();
      if( moc==null && mocFiltreSpatial!=null || moc!=null && mocFiltreSpatial==null
            || moc!=null && !moc.equals(mocFiltreSpatial) ) return false;

      if( isEmpty() ) return true;

      String expr = getExpression();
      String oExpr = exprArea.getText();
      if( !expr.equals( oExpr ) ) return false;

      if( mocFiltreSpatial!=null ) {
         int inter = comboIntersecting.getSelectedIndex();
         int oInter = getIntersect(mocFiltreSpatial);
         if( inter!=oInter ) return false;
      }

      return true;
   }
   
   /** Reset du formulaire et application à l'arbre immédiatement */
   protected void reset() { 
      clean();
      submit();
   }
   
   /** Remise à l'état initial du formulaire - sans application à l'arbre */
   protected void clean() {
      
      nameField.setText("");
      
      bxSuperseded.setSelected(false);
      bxPixFull.setSelected(false);
      bxHiPS.setSelected(false);
      bxSIA.setSelected(false);
      bxSIA2.setSelected(false);
      bxSSA.setSelected(false);
      bxTAP.setSelected(false);
      bxCS.setSelected(false);
      bxMOC.setSelected(false);
      bxTMOC.setSelected(false);
      bxProg.setSelected(false);
      bxPixColor.setSelected(false);
      tfCatNbRow.setText("");
      tfCoverage.setText("");
      tfHiPSorder.setText("");
      tfDescr.setText("");
      tfMinDate.setText("");
      tfMaxDate.setText("");
      tfBibYear.setText("");
      tMoc.setText("");
      tSTC.setText("");
      
      for( JCheckBox bx : regVbx ) bx.setSelected(false);
      for( JCheckBox bx : catVbx ) bx.setSelected(false);
      for( JCheckBox bx : authVbx ) bx.setSelected(false);
      for( JCheckBox bx : catMisVbx ) bx.setSelected(false);
      for( JCheckBox bx : catkeyVbx ) bx.setSelected(false);
      for( JCheckBox bx : assdataVbx ) bx.setSelected(false);
      for( JCheckBox bx : catUcdVbx ) bx.setSelected(false);
      
      comboIntersecting.setSelectedIndex( MultiMoc.OVERLAPS );
      spaceBG.clearSelection();
      
      mocFiltreSpatial=null;
      mocArea.setText("");
      
      updateWidget();
   }
   
   /** Mise en place d'un filtre prédéfini.
    * POUR LE MOMENT, SEULE LA SYNTAXE AVANCEE EST PRISE EN COMPTE, LES CHECKBOXES NE SONT PAS UTILISEES */
   protected void setSpecificalFilter(String name, String expr, SMoc moc, int intersect) {
      clean();
      if( name.equals(Directory.ALLCOLL) ) name=Directory.MYLIST;
      nameField.setText(name);      // Positionnement du nom du filtre
      exprArea.setText(expr==null || expr.equals("*") ? "" : expr);       // Positionnement de l'expression du filtre
      if( moc!=null ) {
         mocFiltreSpatial=moc;
         mocArea.setText( getASCII(moc) );
      }
      flagFormEdit=true;                
      submit();
   }
   
   // Positionne un cadre de titre autour d'un panel
   static public void setTitleBorder(JPanel p, String title, Color foreground) {
      Border line = BorderFactory.createMatteBorder(1, 1, 1, 1, foreground);
      if( title==null ) p.setBorder( line );
      else p.setBorder( BorderFactory.createTitledBorder(line,title,
            TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION,
            Aladin.BOLD,foreground) );
   }

   /** Construction du panel du formulaire */
   protected JTabbedPane createFilterPanel() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
//      c.insets = new Insets(2,2,2,2);
      JCheckBox bx;
      JPanel subPanel;
      JPanel topLeftPanel,bottomLeftPanel,rightPanel,spacePanel;
      
      JPanel p = topLeftPanel = new JPanel( g );
//      setTitleBorder(p,"Global filters");
      
      // Description
      tfDescr = new JTextFieldX(30);
      PropPanel.addCouple(this, p, S("FPKEYWORD"), S("FPKEYWORDTIP"), tfDescr, g, c, GridBagConstraints.EAST);
      
      // Catégories des collections
      catVbx = new Vector<>();
      subPanel = createFilterBis(catVbx, -2, true, "client_category", "/",SORT_ALPHA);
      PropPanel.addCouple(this, p, S("FPDATATYPE"), S("FPDATATYPETIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Couverture du ciel
      tfCoverage = new JTextFieldX(15);
      PropPanel.addCouple(this, p, S("FPCOVERAGE"), S("FPCOVERAGETIP"), tfCoverage, g, c, GridBagConstraints.EAST);
      
      // Les différents régimes
      regVbx = new Vector<>();
      subPanel = createFilterBis(regVbx, -2, true, "obs_regime", null,SORT_LIST);
      PropPanel.addCouple(this, p, S("FPREGIME"), S("FPREGIMETIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Epoch of observations
      tfBibYear = new JTextFieldX(10);
      PropPanel.addCouple(this, p, S("FPBIB"), S("FPBIBTIP"), tfBibYear, g, c, GridBagConstraints.EAST);

      
      // Les différentes origines des HiPS
      authVbx = new Vector<>();
      subPanel = createFilterBis(authVbx, -1, true, "ID", "/",SORT_ALPHA);
      PropPanel.addCouple(this, p, S("FPAUTH"), S("FPAUTHTIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Epoch of observations
      subPanel = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0) );
      tfMinDate = new JTextFieldX(10);
      tfMaxDate = new JTextFieldX(10);
      subPanel.add(tfMinDate); subPanel.add( new JLabel(" .. ") ); subPanel.add(tfMaxDate);
      PropPanel.addCouple(this, p, S("FPEPOCH"), S("FPEPOCHTIP"), subPanel, g, c, GridBagConstraints.EAST);

      p = bottomLeftPanel = new JPanel(g);

      // Restriction suivant le mode d'accès
      PropPanel.addSectionTitle(p, " "+S("FPPROTOLAB"), g, c);
      
      subPanel = new JPanel( new GridLayout(0,4) );
      subPanel.setBorder(  BorderFactory.createLineBorder(Color.gray));
      NoneSelectedButtonGroup bg = new NoneSelectedButtonGroup();
      subPanel.add( bx=bxHiPS = new JCheckBox("HiPS")); bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOHIPS"));
      subPanel.add( bx=bxSIA  = new JCheckBox("SIA(1&2)"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOSIA"));
      subPanel.add( bx=bxSIA2  = new JCheckBox("SIA2"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOSIA"));
      subPanel.add( bx=bxSSA  = new JCheckBox("SSA"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOSSA"));
      subPanel.add( bx=bxTAP  = new JCheckBox("TAP"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOTAP"));
      subPanel.add( bx=bxCS   = new JCheckBox("Cone Search"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOCS"));
      subPanel.add( bx=bxMOC  = new JCheckBox("MOC"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOMOC"));
      subPanel.add( bx=bxTMOC  = new JCheckBox("TMOC"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOTMOC"));
      subPanel.add( bx=bxProg   = new JCheckBox("HiPS progenitors"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText(S("FPPROTOHIPSPRO"));
      PropPanel.addCouple(this, p, S("FPPROTO")+" ", S("FPPROTOTIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
     
      
      // Résolution HiPS
      PropPanel.addFilet(p, g, c, 30, 0);
      PropPanel.addSectionTitle(p, " "+S("FPHIPSLAB"), g, c);
      
      tfHiPSorder = new JTextFieldX(15);
      PropPanel.addCouple(this, p, S("FPHIPSORDER"), S("FPHIPSORDERTIP"), tfHiPSorder, g, c, GridBagConstraints.EAST);
      
//    System.out.println("max="+max);
//    slRow = new JSlider(JSlider.HORIZONTAL, 0, max, max); 
//    slRow.addChangeListener(new ChangeListener() {
//       public void stateChanged(ChangeEvent e) { submit(); }
//    });
//    PropPanel.addCouple(this, p, "Test", "Max nb of rows",
//          slRow, g, c, GridBagConstraints.EAST);
      
      // Types de tuiles
      bg = new NoneSelectedButtonGroup();
      subPanel = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
      subPanel.add( bx=bxPixFull     = new JCheckBox(S("FPHIPSPIXEL1"))); bx.addActionListener(this);  bg.add(bx);
      subPanel.add( bx=bxPixColor    = new JCheckBox(S("FPHIPSPIXEL2")));        bx.addActionListener(this);  bg.add(bx);
      PropPanel.addCouple(this, p, S("FPHIPSPIXEL"), S("FPHIPSPIXELTIP"), subPanel, g, c, GridBagConstraints.EAST);
      
      // Les filtres dédiés aux catalogues
      p = rightPanel = new JPanel( g );
//      setTitleBorder(p, "Dedicated catalog/table filters");
      
      // Les différents mots clés
      catkeyVbx = new Vector<>();
      subPanel = createFilterBis(catkeyVbx, -1, false, "obs_astronomy_kw", null, SORT_ALPHA);
      PropPanel.addCouple(this, p, S("FPCATKEYWORD"), S("FPCATKEYWORDTIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Les différents mots clés
      catMisVbx = new Vector<>();
      subPanel = createFilterBis(catMisVbx, -1, false, "obs_mission", null, SORT_ALPHA);
      PropPanel.addCouple(this, p, S("FPMISSION"), S("FPMISSIONTIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Tailles des tables
      long max = aladin.directory.getNbRowMax();
      tfCatNbRow = new JTextFieldX(30); 
      PropPanel.addCouple(this, p, S("FPNBROWS"), S("FPNBROWSTIP")+"\n \n(Note: max nb rows is "+max+")",
            tfCatNbRow, g, c, GridBagConstraints.EAST);
      
      // les UCDs
      catUcdVbx = new Vector<>();
      subPanel = new JPanel( new GridLayout(0,3) );
      subPanel.add( bx = new JCheckBox("Parallax"));       bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=pos.parallax*");       setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Radial vel."));   bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=spect.dopplerVeloc*,phys.veloc*");      setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Proper motion")); bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=pos.pm*");             setToolTip(bx);   catUcdVbx.add(bx);
      
      subPanel.add( bx  = new JCheckBox("Flux Radio"));    bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.flux*;em.radio*");setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Flux IR"));       bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.flux*;em.IR*,phot.flux*;em.mm*");setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Flux Opt"));       bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.flux*;em.opt*");setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Flux HE"));       bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.flux*;em.X-ray*,phot.flux*;em.gamma*");setToolTip(bx);   catUcdVbx.add(bx);
      
      subPanel.add( bx  = new JCheckBox("Mag IR"));     bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.mag*;em.IR.K*");           setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Mag Opt"));     bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.mag*;em.opt.B*");           setToolTip(bx);   catUcdVbx.add(bx);
      
      subPanel.add( bx  = new JCheckBox("Color"));     bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=phot.color*");           setToolTip(bx);   catUcdVbx.add(bx);
      subPanel.add( bx  = new JCheckBox("Redshift"));      bx.setSelected(false); bx.addActionListener(this);
      bx.setActionCommand("data_ucd=src.redshift*");       setToolTip(bx);   catUcdVbx.add(bx);

      JPanel p1 = new JPanel( new BorderLayout(0,0) );
      subPanel.setBorder( BorderFactory.createLineBorder(Color.gray));
      p1.add(subPanel);
      p1.setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 5));
      PropPanel.addCouple(this, p, S("FPCONTENT"), S("FPCONTENTTIP"), p1, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Les données associées
      assdataVbx = new Vector<>();
      subPanel = createFilterBis(assdataVbx, -1, false, "associated_dataproduct_type", null, SORT_FREQ );
      PropPanel.addCouple(this, p, S("FPASSDATA"), S("FPASSDATATIP"), subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Types de tuiles
      subPanel = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
      subPanel.add( bx=bxSuperseded     = new JCheckBox(S("FPSUPERSEDEDCB"))); bx.addActionListener(this); 
      PropPanel.addCouple(this, p, S("FPSUPERSEDED"), S("FPSUPERSEDEDTIP"), subPanel, g, c, GridBagConstraints.EAST);
      
      
      //      JScrollPane scrollPane = new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//      JPanel p1 = new JPanel(new BorderLayout());
//      p1.add(scrollPane);
      
      
//      JPanel left = new JPanel( new BorderLayout(5,5));
//      left.add( topLeftPanel, BorderLayout.NORTH );
//      left.add( bottomLeftPanel, BorderLayout.SOUTH );
//      
//      JPanel globalPanel = new JPanel( new BorderLayout(5,5));
//      globalPanel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
//      globalPanel.add( left, BorderLayout.WEST);
//      globalPanel.add( rightPanel, BorderLayout.EAST);

      p = spacePanel = new JPanel( g );

      JLabelX desc = new JLabelX("<html>"+S("FPSPACEDESC")+"</html>");
      PropPanel.addCouple(this, p, "  ", null, desc, g, c, GridBagConstraints.EAST);
      desc.setFont( desc.getFont().deriveFont(Font.ITALIC));
      
      PropPanel.addFilet(p, g, c, 20, 0);

      PropPanel.addSectionTitle(p, " "+S("FPSPACEDEFLAB"), g, c);
      
      JCheckBox cb = cbSelectedGraph = new JCheckBox(S("FPSPACECURRENT"));
      cb.addActionListener( this );
      spaceBG = new NoneSelectedButtonGroup();
      spaceBG.add(cb);
      PropPanel.addCouple(this, p, "   ", S("FPSPACECURRENTTIP"), cb, g, c, GridBagConstraints.EAST);
      
      p1 = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
      cb = cbMocPlane = new JCheckBox(S("FPSPACEMOC"));
      cb.addActionListener( this );
      cb.setSelected(false);
      spaceBG.add(cb);
      comboMocPlane = new JComboBox<>();
      comboMocPlane.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            cbMocPlane.setSelected(true);
         }
      });

      p1.add(cb); p1.add(comboMocPlane);
      PropPanel.addCouple(this, p, "  ", S("FPSPACEMOCTIP"), p1, g, c, GridBagConstraints.EAST);

      p1 = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
      cb = cbMocInLine = new JCheckBox(S("FPSPACEMOCINLINE"));
      cb.addActionListener( this );
      spaceBG.add(cb);
//      tMoc = new JTextFieldX(40);
      tMoc = new JTextArea(3,40);
      tMoc.setLineWrap(true);
      JScrollPane js = new JScrollPane(tMoc, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tMoc.addKeyListener( new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            cbMocInLine.setSelected( tMoc.getText().length()>0 );
            updateWidget();
         }
      });
      p1.add(cb); p1.add(js);
      c.insets.bottom+=2;
      PropPanel.addCouple(this, p, "   ", S("FPSPACEMOCINLINETIP"), p1, g, c, GridBagConstraints.EAST);
      c.insets.bottom-=2;
     
      p1 = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
      cb = cbStcInLine = new JCheckBox(S("FPSPACESTCINLINE"));
      cb.addActionListener( this );
      spaceBG.add(cb);
//      tSTC = new JTextFieldX(41);
      tSTC = new JTextArea(3,40);
      tSTC.setLineWrap(true);
      js = new JScrollPane(tSTC, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tSTC.addKeyListener( new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            cbStcInLine.setSelected( tSTC.getText().length()>0 );
            updateWidget();
         }
      });

      p1.add(cb); p1.add(js);
      PropPanel.addCouple(this, p, "   ", S("FPSPACESTCINLINETIP"), p1, g, c, GridBagConstraints.EAST);
      
      comboIntersecting = new JComboBox<>( SINTERSECT );
      comboIntersecting.addActionListener( this );
      PropPanel.addFilet(p, g, c, 20, 2);
      JLabel mode = new JLabel(S("FPSPACEINTER"));
      PropPanel.addCouple(this, p, mode, S("FPSPACEINTERTIP"),comboIntersecting, g, c, GridBagConstraints.EAST);
      mode.setFont( mode.getFont().deriveFont(Font.BOLD));
     
      JTabbedPane globalPanel = new JTabbedPane( );
      globalPanel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
      globalPanel.add( topLeftPanel,    " "+S("FPGLOBAL")+" ");
      globalPanel.add( rightPanel,      " "+S("FPCAT")+" ");
      globalPanel.add( spacePanel,      " "+S("FPREGION")+" ");
      globalPanel.add( bottomLeftPanel, " "+S("FPTECH")+" ");

      return globalPanel;
   }
   
   private class NoneSelectedButtonGroup extends ButtonGroup {
      public void setSelected(ButtonModel model, boolean selected) {
        if (selected)  super.setSelected(model, selected);
        else clearSelection();
      }
    }
   
   private void setToolTip(JCheckBox bx) {
//      String s = aladin.directory.getNumber( bx.getActionCommand() )+" collections";
//      bx.setToolTipText(s);
   }
   
   @Override
   public void actionPerformed(ActionEvent e) {
      flagFormEdit=false; 
      submitAction(true);
   }
   protected void submitAction(boolean forceActivation) {
      submit();
   }
}
