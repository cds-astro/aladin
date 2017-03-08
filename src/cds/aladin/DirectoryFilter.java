// Copyright 2010 - UDS/CNRS
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import cds.mocmulti.MocItem;
import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Classe qui gère le formulaire de filtrage de l'arbre HiPS
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public final class DirectoryFilter extends JFrame implements ActionListener {
   
   private Aladin  aladin;  // référence externe
   
   static protected String ALLCOLL = "All collections";
   static protected String MYLIST  = ""; //"My working list";
   
   static protected String ALLCOLLHTML = "<html><i> -- All collections --</i></html>";
   static protected String MYLISTHTML  = "<html><i> -- My working list --</i></html>";
   
//   // Préfixe des paramètres de filtrage des HiPS par le MocServer
//   private static String MOCSERVER_FILTERING 
//        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
//         +"&hips_service_url=*&casesensitive=false";
   
   public DirectoryFilter(Aladin aladin) {
      super();
      this.aladin = aladin;
      
      Aladin.setIcon(this);
      setTitle("Collection registry filter");
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);
      
      JPanel contentPane = (JPanel)getContentPane();
      contentPane.setLayout( new BorderLayout(5,5)) ;
      setAlwaysOnTop(true);
      
      contentPane.add( getHeaderPanel(), BorderLayout.NORTH );
      contentPane.add( getMainFilterPanel(), BorderLayout.CENTER );
      contentPane.add( getValidPanel(), BorderLayout.SOUTH );
      
      pack();
   }
   
   private JPanel getHeaderPanel() {
      JButton b;
      
      JPanel storePanel = new JPanel( new FlowLayout(FlowLayout.CENTER,7,7) );
      storePanel.setBorder( BorderFactory.createEmptyBorder(0,20,0,0));
      storeButton=b=new JButton("Store");
      Util.toolTip(storeButton, "Allows to create/update a permanent filter with its own specific label",true);
      deleteButton=b=new JButton("Delete");
      Util.toolTip(deleteButton, "Allows to remove a permanent filter",true);
           
      JLabel l = new JLabel("Filter name ");
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

      return storePanel;
   }
   
   private JTextArea exprArea;
   private boolean flagFormEdit=false;
   
   /** True si un filtre est en cours d'application */
   protected boolean hasFilter() { return exprArea.getText().trim().length()>0 && !exprArea.getText().equals("*"); }
   
   /** Création du panel de l'expression correspondant au filtre courant */
   private JPanel createExpPanel() {
      JPanel areaPanel = new JPanel( new BorderLayout(5,5) );
      setTitleBorder(areaPanel, "corresponding filter expression");
      exprArea = new JTextArea(3,60);
      exprArea.setLineWrap(true);
      exprArea.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { flagFormEdit=true; updateWidget(); }
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
      
      JPanel p = new JPanel( new BorderLayout());
      p.setBorder( BorderFactory.createEmptyBorder(0,5,5,5));
      p.add( areaPanel, BorderLayout.CENTER);
      return p;
   }
   
   private JButton storeButton, deleteButton;
   private JTextField nameField;
   
   /** Activation/desactivation des boutons en fonction du contenu du formulaire */
   private void updateWidget() {
      String name = nameField.getText().trim();
      boolean enabled = name.length()>0 && hasFilter();
      storeButton.setEnabled( enabled && !name.equals(ALLCOLL) );
      deleteButton.setEnabled( enabled && aladin.configuration.dirFilter.containsKey(name) 
            && !name.equals(ALLCOLL) && !name.equals(MYLIST));
      
      String expr = aladin.configuration.dirFilter.get(name);
      boolean modif = expr==null ? false : expr.equals( exprArea.getText().trim() );
      storeButton.setText( modif ? "update" : "store" );

      if( flagFormEdit && !exprArea.getText().trim().equals("") ) activateAreaText(true);
      else activateAreaText(false);
   }

   private void activateAreaText(boolean flag) {
      if( flag ) {
         exprArea.setForeground( Aladin.COLOR_GREEN.darker() );
         exprArea.setBackground( Color.white );
         exprArea.getFont().deriveFont(Font.BOLD);
      } else {
         exprArea.setForeground( Color.gray );
         exprArea.setBackground( getBackground() );
         exprArea.getFont().deriveFont(Font.ITALIC);
      }
   }
   
   /** Mémorisation + activation du filtre courant */
   private void store() {
      String name = nameField.getText().trim();
      if( name.equals(ALLCOLL) || name.equals(MYLIST) ) {
         aladin.warning(this,"You have to provide your own specific filter name\n"
               + "to save it as a permanent filter.");
         return;
      }
      String expr = exprArea.getText().trim();
      aladin.configuration.setDirFilter(name, expr);
      aladin.configuration.dirFilter.remove(MYLIST);
      aladin.directory.updateDirFilter();
      aladin.directory.comboFilter.setSelectedItem(name);
   }
   
   /** Suppression de la mémorisation du filtre courant */
   private void delete() {
      String name = nameField.getText().trim();
      if( name.equals(ALLCOLL) ) return;
      if( name.equals(MYLIST) ) return;
      aladin.configuration.dirFilter.remove(name);
      aladin.directory.updateDirFilter();
      reset();
      nameField.setText("");
   }
   
   /** Construction du panel qui contient les tabs des différents filtres + le panel de l'expression brute */
   private JSplitPane getMainFilterPanel() {
      MySplitPane pane = new MySplitPane(aladin, JSplitPane.VERTICAL_SPLIT, createFilterPanel(), createExpPanel() , 1);
      pane.setBackground( getBackground() );
      return pane;
   }

   /** Construction du panel des boutons de validation */
   private JPanel getValidPanel() {
      JPanel p = new JPanel();
      p.setLayout( new BorderLayout(10,10) );
      JButton b;
      
      
      JPanel applyPanel = new JPanel( new FlowLayout( FlowLayout.CENTER,7,7 ) );
      
      applyPanel.add( b=new JButton("Apply")); 
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(); }
      });
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      applyPanel.add( b=new JButton("Reset"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { reset(); }
      });
      
      JPanel closePanel = new JPanel( new FlowLayout( FlowLayout.CENTER,7,7 ) );
      
      closePanel.add( b=new JButton("Close"));
      closePanel.setBorder( BorderFactory.createEmptyBorder(0,0,0,20));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { setVisible(false); }
      });
      
      p.add( applyPanel, BorderLayout.CENTER );
      p.add( closePanel, BorderLayout.EAST );
      return p;
   }
   
   <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map, int ascending) {
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

      Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
      sortedByValues.putAll(map);
      return sortedByValues;
   }

   <K extends Comparable<K>, V> Map<K, V> sortAlpha(final Map<K, V> map, int ascending) {
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

      Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
      sortedByValues.putAll(map);
      return sortedByValues;
   }

   
   <K extends Comparable<K>, V> Map<K, V> sortList(final Map<K, V> map, final String [] list) {
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

      Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
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
      Map<String, Integer> map = new HashMap<String, Integer>();
      
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
         panel.setLayout( new GridLayout(0,4) );
         panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) panel.add(bx);
         
      // Beaucoup d'éléments => deux colonnes avec scroll
      } else {
         JPanel p1 = new JPanel( new GridLayout(0,2) );
         for( JCheckBox bx : vBx )  p1.add(bx);
        
         JScrollPane scrollPane = new JScrollPane(p1, 
               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setPreferredSize( new Dimension(320,120) );
         panel.add( scrollPane, BorderLayout.CENTER);
         panel.setBorder( BorderFactory.createEmptyBorder(3, 0, 3, 3));
      }
      
      return panel;
   }
   
   private JPanel createFilter( final Vector<JCheckBox> vBx, int max, String key, String delim, boolean addLogic) {
      Map<String, Integer> map = new HashMap<String, Integer>();
      
      int total=0;
      for( MocItem mi : aladin.directory ) {
         Iterator<String> it = mi.prop.getIteratorValues(key);
         if( it==null ) continue;
         while( it.hasNext() ) {
            String v = it.next();
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
      
      Map<String, Integer> map1  = max==-1 ? sortAlpha(map,-1) : sortByValues(map, 1);
      
      int p=0;
      StringBuilder others=new StringBuilder();
      for( String k : map1.keySet() ) {
        int n = map.get(k);
        String lab = k;
        if( max<0 ) lab=lab.replace('_',' ');
        else if( lab.length()>11 ) lab = lab.substring(0, 8)+"...";
        
        JCheckBox bx = new JCheckBox( lab, max>=0);
//        bx.setToolTipText(k);
        String vm = (max>0 && addLogic ? "!":"")+ k + (delim==null?"":delim+"*");
        bx.setActionCommand((addLogic?"":"-")+key+"="+vm);
        bx.addActionListener(this);
        setToolTip(bx);
        vBx.add( bx );
        if( max>0 ) {
           if( others.length()>0 ) others.append(',');
           others.append(vm);
        }
        
        p+=n;
        if( max>0 && vBx.size()>=max ) {
           if( p<total ) {
              bx = new JCheckBox("Others",true);
              bx.setActionCommand((addLogic?"-":"")+key+"="+others);
              bx.addActionListener(this);
              vBx.add( bx );
           }
           break;
        }
      }
      
      JPanel panel= new JPanel( new BorderLayout(0,0) );
      
      if( vBx.size()<12 ) {
         panel.setLayout( new GridLayout(0,4) );
//         panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) panel.add(bx);
         
      } else {
         JPanel p1 = new JPanel( new GridLayout(0,2) );
//         p1.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) {
//            bx.setText( bx.getToolTipText() );
            p1.add(bx);
         }
        
         JScrollPane scrollPane = new JScrollPane(p1, 
               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setPreferredSize( new Dimension(320,95) );
         panel.add( scrollPane, BorderLayout.CENTER);
         panel.setBorder( BorderFactory.createEmptyBorder(3, 0, 3, 3));
      }
      
//      JButton b = new JButton("none");
//      JPanel p1 = new JPanel( new BorderLayout(0,0) );
//      p1.add(b,BorderLayout.CENTER);
      
      JButton b;
      JPanel px = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      px.add(b = new JButton("none"));
      
      if( max>0 ) panel.add( px );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean flag = ((JButton)e.getSource()).getText().equals("none");
            ((JButton)e.getSource()).setText( flag? "all" : "none" );
            for( JCheckBox bx : vBx ) bx.setSelected(!flag);
         }
      });
      
      return panel;
   }
   
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
      if( !flagFormEdit ) generateExpression();
      
      updateWidget();
      
      flagFormEdit=false;
      
      String expr = exprArea.getText();
      if( expr.trim().length()==0 ) expr="*";
      
      aladin.directory.resumeFilter(expr);
      
      // mémorisation de l'expression s'il s'agit du MYLIST
      if( aladin.directory.comboFilter.getSelectedItem().equals(MYLISTHTML) ) {
         aladin.configuration.setDirFilter(MYLIST, expr);
      }
   }
   
   /** Génération de l'expression de filtrage correspondante aux positionnements des checkboxes et autres
    * champs de saisie. Le résultat est stocké dans le champ exprArea */
   private void generateExpression() {
      
      HashMap<String, String []> inclu = new HashMap<String, String[]>();
      HashMap<String, String []> exclu = new HashMap<String, String[]>();
      
      StringBuilder special = new StringBuilder();
      
      if( bxHiPS.isSelected() )      special.append(" && hips_service_url=*"); 
      if( bxSIA.isSelected() )       special.append(" && sia*=*");  
      if( bxSSA.isSelected() )       special.append(" && ssa*=*");  
      if( bxTAP.isSelected() )       special.append(" && tap*=*");  
      if( bxCS.isSelected() )        special.append(" && cs*=*");   
      if( bxProg.isSelected() )      special.append(" && hips_progenitor_url=*");   
      
      if( bxPixFull.isSelected() )    special.append(" && (hips_tile_format=*fits* || dataproduct_type=!Image)");
      if( bxPixColor.isSelected() )   special.append(" && (dataproduct_subtype=color || dataproduct_type=!Image)");
      
      for( JCheckBox bx : catVbx )    if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      for( JCheckBox bx : regVbx )    if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      for( JCheckBox bx : authVbx )   if( bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      
      HashMap<String, String []> inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : catMisVbx ) if( bx.isSelected() )  addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : catkeyVbx ) if( bx.isSelected() )  addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : assdataVbx ) if( bx.isSelected() ) addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : catUcdVbx ) if( bx.isSelected() ) addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      String s;      
      if( (s=getText(tfDescr)).length()!=0 )     special.append(" && obs_title,obs_description,obs_collection,ID="+jokerize(s));
      if( (s=getText(tfCoverage)).length()!=0 )  special.append(" && moc_sky_fraction="+s);
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
            
      exprArea.setText( expr );
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
   }
   
   class JTextFieldX extends JTextField {
      JTextFieldX(int n) {
         super(n);
         addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) { }
            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) {
               if( e.getKeyCode()==KeyEvent.VK_ENTER ) submitAction();
            }
         });
      }
   }
   
   private JCheckBox bxPixFull,bxPixColor,bxHiPS,bxSIA,bxSSA,bxTAP,bxCS,bxProg;
   private JTextFieldX tfCatNbRow,tfCoverage,tfHiPSorder,tfDescr,tfMinDate,tfMaxDate,tfBibYear;
   private Vector<JCheckBox> catVbx,authVbx,regVbx,catkeyVbx,catMisVbx,assdataVbx,catUcdVbx;
   
   /** Reset du formulaire et application à l'arbre immédiatement */
   protected void reset() { 
      clean();
      submit();
   }
   
   /** Remise à l'état initial du formulaire - sans application à l'arbre */
   protected void clean() {
      
      bxPixFull.setSelected(false);
      bxHiPS.setSelected(false);
      bxSIA.setSelected(false);
      bxSSA.setSelected(false);
      bxTAP.setSelected(false);
      bxCS.setSelected(false);
      bxProg.setSelected(false);
      bxPixColor.setSelected(false);
      tfCatNbRow.setText("");
      tfCoverage.setText("");
      tfHiPSorder.setText("");
      tfDescr.setText("");
      tfMinDate.setText("");
      tfMaxDate.setText("");
      tfBibYear.setText("");
      
      for( JCheckBox bx : regVbx ) bx.setSelected(false);
      for( JCheckBox bx : catVbx ) bx.setSelected(false);
      for( JCheckBox bx : authVbx ) bx.setSelected(false);
      for( JCheckBox bx : catMisVbx ) bx.setSelected(false);
      for( JCheckBox bx : catkeyVbx ) bx.setSelected(false);
      for( JCheckBox bx : assdataVbx ) bx.setSelected(false);
      for( JCheckBox bx : catUcdVbx ) bx.setSelected(false);
      
      updateWidget();
   }
   
   /** Mise en place d'un filtre prédéfini.
    * POUR LE MOMENT, SEULE LA SYNTAXE AVANCEE EST PRISE EN COMPTE, LES CHECKBOXES NE SONT PAS UTILISEES */
   protected void setSpecificalFilter(String name, String expr) {
      clean();
//      System.out.println("setSpecificalFilter("+name+")");
      if( name.equals(ALLCOLL) ) name=MYLIST;
      nameField.setText(name);      // Positionnement du nom du filtre
      exprArea.setText(expr.equals("*") ? "" : expr);       // Positionnement de l'expression du filtre
      flagFormEdit=true;                
      submit();
   }
   
   // Positionne un cadre de titre autour d'un panel
   private void setTitleBorder(JPanel p, String title) {
      Border line = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.gray);
      if( title==null ) p.setBorder( line );
      else p.setBorder( BorderFactory.createTitledBorder(line,title,
            TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION,
            Aladin.BOLD,new Color(140,140,140)) );
   }

   /** Construction du panel du formulaire */
   protected JTabbedPane createFilterPanel() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
//      c.insets = new Insets(2,2,2,2);
      JCheckBox bx;
      JPanel subPanel;
      JPanel topLeftPanel,bottomLeftPanel,rightPanel;
      
      JPanel p = topLeftPanel = new JPanel( g );
//      setTitleBorder(p,"Global filters");
      
      // Description
      tfDescr = new JTextFieldX(30);
      PropPanel.addCouple(this, p, "Keyword", "keyword or list of keyword (comma separated)\n"
            + "in title, description, collection, identifier...\nExamples: DENIS, CDS/P/DSS2/color", 
            tfDescr, g, c, GridBagConstraints.EAST);
      
      // Catégories des collections
      catVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(catVbx, -2, true, "client_category", "/",SORT_FREQ);
      PropPanel.addCouple(this, p, "Data type", "Collection data types", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Couverture du ciel
      tfCoverage = new JTextFieldX(15);
      PropPanel.addCouple(this, p, "Sky fraction", "Fraction of the sky coverage...\nExamples: <0.1, 0.2 .. 0.4, >=0.9)", 
            tfCoverage, g, c, GridBagConstraints.EAST);
      
      // Les différents régimes
      regVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(regVbx, -2, true, "obs_regime", null,SORT_LIST);
      PropPanel.addCouple(this, p, "Regime", "Wavelength regime of the collection", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Epoch of observations
      tfBibYear = new JTextFieldX(10);
      PropPanel.addCouple(this, p, "Bib. year", "Year of the biblographic reference paper\nExamples: 2008, >2015, 2012..2014", tfBibYear, g, c, GridBagConstraints.EAST);

      
      // Les différentes origines des HiPS
      authVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(authVbx, -1, true, "ID", "/",SORT_FREQ);
      PropPanel.addCouple(this, p, "Authority", "Filtering by the authority creator.", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Epoch of observations
      subPanel = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0) );
      tfMinDate = new JTextFieldX(10);
      tfMaxDate = new JTextFieldX(10);
      subPanel.add(tfMinDate); subPanel.add( new JLabel(" .. ") ); subPanel.add(tfMaxDate);
      PropPanel.addCouple(this, p, "Obs. epoch", "Epoch of observations (in MJD)\nExample: 46000 52000, >47000", subPanel, g, c, GridBagConstraints.EAST);

      // Restriction suivant le mode d'accès
      subPanel = new JPanel( new GridLayout(0,4) );
      NoneSelectedButtonGroup bg = new NoneSelectedButtonGroup();
      subPanel.add( bx=bxHiPS = new JCheckBox("HiPS")); bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("Hierarchical progressive survey compatible collections");
      subPanel.add( bx=bxSIA  = new JCheckBox("SIA"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("Simple Image Access (version 1 & 2) compatible collections");
      subPanel.add( bx=bxSSA  = new JCheckBox("SSA"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("Simple Spectra Access compatible collections");
      subPanel.add( bx=bxTAP  = new JCheckBox("TAP"));  bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("Table Access Protocol compatible collections");
      subPanel.add( bx=bxCS   = new JCheckBox("Cone search"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("Catalog/table cone searchable collections");
      subPanel.add( bx=bxProg   = new JCheckBox("Progenitors"));   bx.setSelected(false); bx.addActionListener(this); bg.add(bx);
      bx.setToolTipText("HiPS Progenitors access (details and links\nto original images used for building the HiPS)");
      PropPanel.addCouple(this, p, "Protocol ", "Keep the collections supporting the selected protocol", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
     
      // Les filtres dédiés aux HiPS
      p = bottomLeftPanel = new JPanel(g);
//      setTitleBorder(p, "Dedicated HiPS filters");
      
      // Couverture du ciel
      tfHiPSorder = new JTextFieldX(15);
      PropPanel.addCouple(this, p, "HiPS order", "HiPS order (WILL BE REPLACED BY ANGULAR RESOLUTION)\nExamples: <5, 6..11, >12", 
            tfHiPSorder, g, c, GridBagConstraints.EAST);
      
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
      subPanel.add( bx=bxPixFull     = new JCheckBox("full dynamic only")); bx.addActionListener(this);  bg.add(bx);
      subPanel.add( bx=bxPixColor    = new JCheckBox("color only"));        bx.addActionListener(this);  bg.add(bx);
      PropPanel.addCouple(this, p, "Pixel formats", "Filtering the HiPS images according to the constraint", subPanel, g, c, GridBagConstraints.EAST);
      
      // Les filtres dédiés aux catalogues
      p = rightPanel = new JPanel( g );
//      setTitleBorder(p, "Dedicated catalog/table filters");
      
      // Les différents mots clés
      catkeyVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(catkeyVbx, -1, false, "obs_astronomy_kw", null, SORT_ALPHA);
      PropPanel.addCouple(this, p, "Keyword", "Catalog astronomical keyword selection.\nHide not relevant tables.", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Les différents mots clés
      catMisVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(catMisVbx, -1, false, "obs_mission", null, SORT_ALPHA);
      PropPanel.addCouple(this, p, "Mission", "Catalog mission keyword selection.\nHide not relevant tables.", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Tailles des tables
      long max = aladin.directory.getNbRowMax();
      tfCatNbRow = new JTextFieldX(30); 
      PropPanel.addCouple(this, p, "Nb rows", "Filtering by the number of rows.\nHide not relevant tables."
            + "\nExamples: >1000, 10..1000.\n \n(Note: max nb rows is "+max+")",
            tfCatNbRow, g, c, GridBagConstraints.EAST);
      
      // les UCDs
      catUcdVbx = new Vector<JCheckBox>();
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
      PropPanel.addCouple(this, p, "Content", "Filtering by the content based on UCD tagging.\nHide not relevant tables.", p1, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
      // Les données associées
      assdataVbx = new Vector<JCheckBox>();
      subPanel = createFilterBis(assdataVbx, -1, false, "associated_dataproduct_type", null, SORT_FREQ );
      PropPanel.addCouple(this, p, "Ass.data", "Filtering by the associated data to a catalog.\nHide not relevant tables.", subPanel, g, c, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
      
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

      JTabbedPane globalPanel = new JTabbedPane( );
      globalPanel.setBackground( getBackground() );
      globalPanel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
      globalPanel.add( topLeftPanel,    " Global constraints ");
      globalPanel.add( rightPanel,      " Catalog constraints ");
      globalPanel.add( bottomLeftPanel, " HiPS constraints ");

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
      submitAction();
   }
   protected void submitAction() {
      aladin.makeCursor(this, Aladin.WAITCURSOR);
      aladin.directory.iconFilter.setActivated(true);
      submit();
   }
}
