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
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.mocmulti.MocItem;
import cds.tools.Util;

/**
 * Classe qui gère le formulaire de filtrage de l'arbre HiPS
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public final class RegFilter extends JFrame implements ActionListener {
   
   private Aladin  aladin;  // référence externe
   
   // Préfixe des paramètres de filtrage des HiPS par le MocServer
   private static String MOCSERVER_FILTERING 
        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
         +"&hips_service_url=*&casesensitive=false";
   
   public RegFilter(Aladin aladin) {
      super();
      this.aladin = aladin;
      
      Aladin.setIcon(this);
      setTitle("Collection registry filter");
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);
      
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      JPanel contentPane = (JPanel)getContentPane();
      contentPane.setLayout( new BorderLayout(5,5)) ;
      contentPane.setBackground( new Color(240,240,250));
//      contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
//      setUndecorated(true);
      setAlwaysOnTop(true);
      
      JPanel exprPanel = createExpPanel();
      MySplitPane splitPanel = new MySplitPane(aladin, JSplitPane.VERTICAL_SPLIT, createPanel(), exprPanel, 1);
      splitPanel.setBorder( BorderFactory.createEmptyBorder());
//      exprPanel.setMinimumSize( new Dimension(400,200) );
      
      Aladin.makeAdd(getContentPane(), splitPanel, "Center");
      Aladin.makeAdd(getContentPane(), getValidPanel(), "South");
      pack();
   }
   
   private JTextArea exprArea;
   private boolean inArea=false;
   
   /** Création du panel de l'expression correspondant au filtre courant */
   private JPanel createExpPanel() {
      JPanel areaPanel = new JPanel( new BorderLayout() );
      areaPanel.setBackground( new Color(240,240,250));
      exprArea = new JTextArea(3,60);
      exprArea.setLineWrap(true);
      exprArea.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { inArea=true; }
         public void keyReleased(KeyEvent e) {
            if( e.getKeyCode()==KeyEvent.VK_ENTER ) submitArea();
         }
      });
      areaPanel.add( exprArea, BorderLayout.CENTER );
      return areaPanel;
   }

   /** Construction du panel des boutons de validation */
   private JPanel getValidPanel() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
      p.add( b=new JButton("Apply")); 
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            if( inArea ) submitArea();
            else submitLocal(); }
      });
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      p.add( b=new JButton("Reset"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { reset(); }
      });
      p.add( b=new JButton("Close"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { setVisible(false); }
      });
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

   private JPanel createFilter( final Vector<JCheckBox> vBx, int max, String key, String delim) {
      return createFilter(vBx,max,key,delim,false);
   }
   private JPanel createFilter( final Vector<JCheckBox> vBx, int max, String key, String delim, boolean addLogic) {
      Map<String, Integer> map = new HashMap<String, Integer>();
      
      int total=0;
      for( MocItem mi : aladin.hipsStore ) {
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
        String lab = k.length()>11 ? k.substring(0, 8)+"..." : k;
        JCheckBox bx = new JCheckBox( lab,max>=0);
        bx.setToolTipText(k);
        String vm = (max>0 && addLogic ? "!":"")+ k + (delim==null?"":delim+"*");
        bx.setActionCommand((addLogic?"":"-")+key+"="+vm);
        bx.addActionListener(this);
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
      
      JPanel panel= new JPanel();
      
      if( vBx.size()<12 ) {
         panel.setLayout( new GridLayout(0,4) );
         panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) panel.add(bx);
         
      } else {
         JPanel p1 = new JPanel( new GridLayout(0,2) );
         p1.setBorder( BorderFactory.createLineBorder(Color.lightGray));
         for( JCheckBox bx : vBx ) {
            bx.setText( bx.getToolTipText() );
            p1.add(bx);
         }
        
         JScrollPane scrollPane = new JScrollPane(p1, 
               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setPreferredSize( new Dimension(320,100) );
         panel.add( scrollPane);
      }
      
      JButton b = new JButton("none");
      JPanel p1 = new JPanel( new BorderLayout(0,0) );
      p1.add(b,BorderLayout.CENTER);
      if( max>0 ) panel.add( p1 );
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
   
   private void submitArea() {
      String expr = exprArea.getText();
      aladin.hipsStore.resumeFilter(expr);
   }
      
   /** Excécution du filtrage (génération de la requête MocServer correspondante au formulaire
    * puis appel au MocServer */
   private void submitLocal() {
      HashMap<String, String []> inclu = new HashMap<String, String[]>();
      HashMap<String, String []> exclu = new HashMap<String, String[]>();
      
      StringBuilder special = new StringBuilder();
      
      if( !bxImage.isSelected() )     addParam(exclu,"client_category","Image*");
      if( !bxCube.isSelected() )      addParam(exclu,"client_category","Cube*");
      if( !bxCatalog.isSelected() )   addParam(exclu,"client_category","Catalog*");
      if( !bxJournal.isSelected() )   addParam(exclu,"client_category","Journal*");
      if( !bxOtherType.isSelected() )      addParam(inclu,"client_category","Catalog*,Image*,Cube*,Journal*");
      
      if( !bxGammaRay.isSelected() )  addParam(exclu,"obs_regime","Gamma-ray");
      if( !bxXray.isSelected() )      addParam(exclu,"obs_regime","X-ray");
      if( !bxUV.isSelected() )        addParam(exclu,"obs_regime","UV");
      if( !bxOptical.isSelected() )   addParam(exclu,"obs_regime","Optical");
      if( !bxInfrared.isSelected() )  addParam(exclu,"obs_regime","Infrared");
      if( !bxRadio.isSelected() )     addParam(exclu,"obs_regime","Radio");
      if( !bxGasLines.isSelected() )  addParam(exclu,"client_category","Image/Gas-lines/*");

      if( bxPixFull.isSelected() )    special.append(" && (hips_tile_format=*fits* || dataproduct_type=!Image)");
      if( bxPixColor.isSelected() )   special.append(" && (dataproduct_subtype=color || dataproduct_type=!Image)");
      
      for( JCheckBox bx : authVbx )    if( !bx.isSelected() ) addParam( inclu,exclu, bx.getActionCommand() );
      
      HashMap<String, String []> inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : catkeyVbx ) if( bx.isSelected() )  addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      inclu1 = new HashMap<String, String[]>();
      for( JCheckBox bx : assdataVbx ) if( bx.isSelected() ) addParam( inclu1,exclu, bx.getActionCommand() );
      if( inclu1.size()>0 )  special.append(" && (dataproduct_type!=catalog || "+rebuildInclu(inclu1)+")");
      
      String s;      
      if( (s=getText(tfDescr)).length()!=0 )     special.append(" && obs_title,obs_description,obs_collection,ID=*"+s+"*");
      if( (s=getText(tfCoverage)).length()!=0 )  special.append(" && moc_sky_fraction="+s);
      if( (s=getText(tfHiPSorder)).length()!=0 ) special.append(" && (hips_order="+s+"|| hips_service_url=!*)");
      if( (s=getText(tfUCD)).length()!=0 )       addParam(exclu,"data_ucd",s);
      if( (s=getText(tfCatNbRow)).length()!=0 )  special.append(" && (dataproduct_type!=catalog || nb_rows="+s+")");
      
      if( (s=getText(tfMinDate)).length()!=0 )   special.append( bxDate.isSelected() ? " && t_min>="+s : " && (t_min>="+s+" || t_min!=*)");
      if( (s=getText(tfMaxDate)).length()!=0 )   special.append( bxDate.isSelected() ? " && t_max<="+s : " && (t_max<="+s+" || t_max!=*)");
      
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
      
      Aladin.trace(3,"Filtering: "+expr);
      
      exprArea.setText( expr );
      repaint();
      aladin.hipsStore.resumeFilter(expr.length()==0 ? "*" : expr);
   }
   
   private String rebuildInclu(HashMap<String,String[]> exclusion) {
      StringBuilder expr = new StringBuilder();
      for( String k : exclusion.keySet() ) {
         if( expr.length()>0 )  expr.append(" || ");
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
      p.x+=aladin.hipsStore.getWidth()+5;
      p.y+=50;
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
               if( e.getKeyCode()==KeyEvent.VK_ENTER ) submit();
            }
         });
      }
   }
   
   private JCheckBox bxImage, bxCube, bxCatalog, bxJournal, bxOtherType;
   private JCheckBox bxGammaRay, bxXray,bxUV,bxOptical,bxInfrared,bxRadio,bxGasLines;
   private JCheckBox bxPixFull,bxPixColor,bxDate;
   private JTextFieldX tfCatNbRow,tfCoverage,tfHiPSorder,tfUCD,tfDescr,tfMinDate,tfMaxDate;
   
   private Vector<JCheckBox> authVbx,catkeyVbx,assdataVbx;
   
   /** Reset du formulaire */
   private void reset() {
      bxDate.setSelected(true);
      bxImage.setSelected(true);
      bxCube.setSelected(true);
      bxCatalog.setSelected(true);
      bxJournal.setSelected(true);
      bxOtherType.setSelected(true);
      bxGammaRay.setSelected(true);
      bxUV.setSelected(true);
      bxOptical.setSelected(true);
      bxInfrared.setSelected(true);
      bxRadio.setSelected(true);
      bxGasLines.setSelected(true);
      bxPixFull.setSelected(false);
      bxPixColor.setSelected(false);
      tfCatNbRow.setText("");
      tfCoverage.setText("");
      tfHiPSorder.setText("");
      tfUCD.setText("");
      tfDescr.setText("");
      tfMinDate.setText("");
      tfMaxDate.setText("");
      
      for( JCheckBox bx : authVbx ) bx.setSelected(true);
      for( JCheckBox bx : catkeyVbx ) bx.setSelected(false);
      for( JCheckBox bx : assdataVbx ) bx.setSelected(false);
      
      submitLocal();
   }
   
   protected void setFreeText( String s ) {
      tfDescr.setText(s);
   }
   
   /** Construction du panel du formulaire */
   protected JPanel createPanel() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,2,2,2);
      JLabel l;
      JCheckBox bx;
      JPanel subPanel;
      JButton b;

      JPanel p = new JPanel();
      p.setLayout(g);
      
      // Description
      tfDescr = new JTextFieldX(30);
      PropPanel.addCouple(this, p, "Keyword", "keyword in title, description, collection...\n(ex: DENIS)", 
            tfDescr, g, c, GridBagConstraints.EAST);
      
      // Le type de données
      subPanel = new JPanel( new GridLayout(0,3));
      subPanel.add( bx=bxImage       = new JCheckBox("Image"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxCube        = new JCheckBox("Cube"));    bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxCatalog     = new JCheckBox("Catalog")); bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxJournal     = new JCheckBox("Journal table"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxOtherType   = new JCheckBox("Others"));  bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxImage.setSelected(false);
            bxCube.setSelected(false);
            bxCatalog.setSelected(false);
            bxJournal.setSelected(false);
            bxOtherType.setSelected(false);
         }
      });
//      categoryVbx = new Vector<JCheckBox>();
//      subPanel = createFilter(categoryVbx, 6, "client_category", "/");
      PropPanel.addCouple(this, p, "Data type", "Collection data type...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Couverture du ciel
      tfCoverage = new JTextFieldX(15);
      PropPanel.addCouple(this, p, "Sky fraction", "Fraction of the sky coverage...\n(ex:<0.1", 
            tfCoverage, g, c, GridBagConstraints.EAST);
      
      // Les différents régimes
      subPanel = new JPanel( new GridLayout(2,3) );
      subPanel.add( bx=bxGammaRay = new JCheckBox("Gamma-ray")); bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxXray     = new JCheckBox("X-ray"));     bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxUV       = new JCheckBox("UV"));        bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxOptical  = new JCheckBox("Optical"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxInfrared = new JCheckBox("Infrared"));  bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxRadio    = new JCheckBox("Radio"));     bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxGasLines = new JCheckBox("Gas-lines")); bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxGammaRay.setSelected(false);
            bxXray.setSelected(false);
            bxUV.setSelected(false);
            bxOptical.setSelected(false);
            bxInfrared.setSelected(false);
            bxRadio.setSelected(false);
            bxGasLines.setSelected(false);
         }
      });
      PropPanel.addCouple(this, p, "Regime", "Wavelength...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Date
      subPanel = new JPanel( new FlowLayout(FlowLayout.LEFT,5,0) );
      tfMinDate = new JTextFieldX(10);
      tfMaxDate = new JTextFieldX(10);
      bxDate = new JCheckBox("only if known",true); bxDate.setToolTipText("Also removed collections with undefined epoch");
      bxDate.addActionListener(this);
      subPanel.add(tfMinDate); subPanel.add( new JLabel(" .. ") ); subPanel.add(tfMaxDate); subPanel.add( bxDate );
      PropPanel.addCouple(this, p, "Epoch", "In this periode. Date in ISO format", subPanel, g, c, GridBagConstraints.EAST);
      
      // Les différentes origines des HiPS
      authVbx = new Vector<JCheckBox>();
      subPanel = createFilter(authVbx, 6, "ID", "/");
      PropPanel.addCouple(this, p, "Authority", "Authority creator...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Les filtres dédiés aux HiPS
      PropPanel.addFilet(p,g,c);
      PropPanel.addSectionTitle(p, "Dedicated HiPS filters", g, c);
      
      // Couverture du ciel
      tfHiPSorder = new JTextFieldX(15);
      PropPanel.addCouple(this, p, "HiPS order", "HiPS order...\n(ex:<5", 
            tfHiPSorder, g, c, GridBagConstraints.EAST);
      
      // Types de tuiles
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bx=bxPixFull     = new JCheckBox("full dynamic only")); bx.addActionListener(this);
      subPanel.add( bx=bxPixColor    = new JCheckBox("color only"));        bx.addActionListener(this);
      PropPanel.addCouple(this, p, "Pixel formats", "Image HiPS pixel formats...", subPanel, g, c, GridBagConstraints.EAST);
      
      
     
      // Les filtres dédiés aux catalogues
      PropPanel.addFilet(p,g,c);
      PropPanel.addSectionTitle(p, "Dedicated catalog/table filters", g, c);
      
      // Les différentes Missions
      catkeyVbx = new Vector<JCheckBox>();
      subPanel = createFilter(catkeyVbx, -1, "obs_astronomy_kw", null, true);
      PropPanel.addCouple(this, p, "Keywords", "Catalog astronomical keywords", subPanel, g, c, GridBagConstraints.EAST);
      
      // Tailles des tables
      tfCatNbRow = new JTextFieldX(30);
      PropPanel.addCouple(this, p, "Nb rows", "Number of rows (ex: >1000 or 10..1000)",
            tfCatNbRow, g, c, GridBagConstraints.EAST);
      
      // Les données associées
      assdataVbx = new Vector<JCheckBox>();
      subPanel = createFilter(assdataVbx, -1, "associated_dataproduct_type", null, true);
      PropPanel.addCouple(this, p, "Ass.data", "Associated data to a catalog", subPanel, g, c, GridBagConstraints.EAST);
      
     // UCDs
      tfUCD = new JTextFieldX(30);
      PropPanel.addCouple(this, p, "UCD", "UCD constraints...\n(ex: pos.angDistance)", 
            tfUCD, g, c, GridBagConstraints.EAST);

      JScrollPane scrollPane = new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      JPanel p1 = new JPanel(new BorderLayout());
      p1.add(scrollPane);
      return p1;
   }

   @Override
   public void actionPerformed(ActionEvent e) { inArea=false; submit(); }
   protected void submit() {
      aladin.makeCursor(this, Aladin.WAITCURSOR);
      aladin.hipsStore.setTreeReady(false);
      submitLocal();
   }
}
