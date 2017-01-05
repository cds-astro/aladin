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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.mocmulti.MocItem;

/**
 * Classe qui gère le formulaire de filtrage de l'arbre HiPS
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public final class HipsFilter extends JFrame implements ActionListener {
   
   private boolean first=true;
   private Aladin  aladin;  // référence externe
   
   // Préfixe des paramètres de filtrage des HiPS par le MocServer
   private static String MOCSERVER_FILTERING 
        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
         +"&hips_service_url=*&casesensitive=false";
   
   public HipsFilter(Aladin aladin) {
      super();
      this.aladin = aladin;
      
//      Aladin.setIcon(this);
//      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
//      Util.setCloseShortcut(this, false,aladin);
      
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      JPanel contentPane = (JPanel)getContentPane();
      contentPane.setLayout( new BorderLayout(5,5)) ;
      contentPane.setBackground( new Color(240,240,250));
      contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
      setUndecorated(true);
      setAlwaysOnTop(true);
   }

   /** Construction du panel des boutons de validation */
   protected JPanel getValidPanel() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
      p.add( b=new JButton("Apply")); 
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { submitLocal(); }
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
      
      Map<String, Integer> map1  = sortByValues(map, 1);
      
      int p=0;
      StringBuilder others=new StringBuilder();
      for( String k : map1.keySet() ) {
        int n = map.get(k);
        String lab = k.length()>11 ? k.substring(0, 8)+"..." : k;
        JCheckBox bx = new JCheckBox( lab,true);
        bx.setToolTipText(k);
        String vm = (addLogic ? "!":"")+ k + (delim==null?"":delim+"*");
        bx.setActionCommand((addLogic?"":"-")+key+"="+vm);
        bx.addActionListener(this);
        vBx.add( bx );
        if( others.length()>0 ) others.append(',');
        others.append(vm);
        
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
      
      JPanel panel = new JPanel( new GridLayout(0,4) );
      
      panel.setBorder( BorderFactory.createLineBorder(Color.lightGray));
      for( JCheckBox bx : vBx ) panel.add(bx);
      
      JButton b = new JButton("none");
      JPanel p1 = new JPanel( new BorderLayout(0,0) );
      p1.add(b,BorderLayout.CENTER);
      panel.add( p1 );
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
         if( key.startsWith("-") ) {
            v[v.length-1] = v[v.length-1]+","+val;
         } else {
            String [] v1 = new String[ v.length+1 ];
            System.arraycopy(v, 0, v1, 0, v.length);
            v1[v1.length-1]=val;
            params.put(key,v1);
         }
      }
   }
   
   private void addParam( HashMap<String, String []> in, HashMap<String, String []> out, String s) {
      int i = s.indexOf('=');
      if( i==-1 ) return;
      String val = s.substring(i+1);
      if( s.charAt(0)=='-' ) {
         addParam( out, s.substring(1,i), val );
      } else {
         addParam( in, s.substring(0,i), val );
      }
   }
      
   /** Excécution du filtrage (génération de la requête MocServer correspondante au formulaire
    * puis appel au MocServer */
   private void submitLocal() {
      HashMap<String, String []> in = new HashMap<String, String[]>();
      HashMap<String, String []> out = new HashMap<String, String[]>();
      
      StringBuilder special = new StringBuilder();
      
      if( !bxImage.isSelected() )      addParam(out,"client_category","Image*");
      if( !bxCube.isSelected() )       addParam(out,"client_category","Cube*");
      if( !bxCatalog.isSelected() )    addParam(out,"client_category","Catalog*");
      if( !bxJournal.isSelected() )    addParam(out,"client_category","Journal*");
      if( !bxMisc.isSelected() )       addParam(in,"client_category","Catalog*,Image*,Cube*,Journal*");
      
      if( !bxGammaRay.isSelected() )  addParam(out,"obs_regime","Gamma-ray");
      if( !bxXray.isSelected() )      addParam(out,"obs_regime","X-ray");
      if( !bxUV.isSelected() )        addParam(out,"obs_regime","UV");
      if( !bxOptical.isSelected() )   addParam(out,"obs_regime","Optical");
      if( !bxInfrared.isSelected() )  addParam(out,"obs_regime","Infrared");
      if( !bxRadio.isSelected() )     addParam(out,"obs_regime","Radio");
      if( !bxGasLines.isSelected() )  addParam(out,"client_category","Image/Gas-lines/*");

      if( bxPixFull.isSelected() )     special.append(" && (hips_tile_format=*fits* || dataproduct_type=!Image)");
      if( bxPixColor.isSelected() )    special.append(" && (dataproduct_subtype=color || dataproduct_type=!Image)");
      
      for( JCheckBox bx : authVbx )    if( !bx.isSelected() ) addParam( in,out, bx.getActionCommand() );
      for( JCheckBox bx : missionVbx ) if( !bx.isSelected() ) addParam( in,out, bx.getActionCommand() );
      for( JCheckBox bx : assdataVbx ) if( !bx.isSelected() ) addParam( in,out, bx.getActionCommand() );
      
      if( bxSmallCat.isSelected() )   special.append(" &! nb_rows=>999");
      if( bxBigCat.isSelected() )     special.append(" &! nb_rows=<999999");
      
      String s;      
      if( (s=getText(tfCoverage)).length()!=0 )  special.append(" && moc_sky_fraction="+s);
      if( (s=getText(tfDescr)).length()!=0 )     special.append(" && obs_title,obs_description,obs_collection,ID=*"+s+"*");
      if( (s=getText(tfHiPSorder)).length()!=0 ) special.append(" && (hips_order="+s+"|| hips_service_url=!*)");
      if( (s=getText(tfUCD)).length()!=0 )       addParam(out,"data_ucd",s);
      
//      if( (s=getText(tfFree)).length()!=0 )      addParam(params,s);
      
      String is = rebuildExpr(in);
      String es = rebuildExpr(out);
      
      String expr = is.length()==0 ? "*" : is;
      if( es.length()>0 ) expr = "("+expr+") &! ("+es+")";
      if( special.length()>0 ) expr="("+expr+")"+special;
      
      // Pour faire des tests
      if( (s=getText(tfFree)).length()!=0 ) expr=s;
      
      Aladin.trace(3,"Filtering: "+expr);
      
      repaint();
      aladin.hipsStore.resumeFilter(expr);
   }
   
   private String rebuildExpr(HashMap<String,String[]> exclusion) {
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

   
   /** Retourne le string d'un JtextField encodé en HTTP */
   private String getText(JTextField tf) { return tf.getText().trim(); }
   
   /** Affichage du panel pour permettre à l'utilisateur de modifier son filtre */
   public void showFilter() {
      if( first ) {
         setTitle("HiPS tree filter");
         Aladin.makeAdd(getContentPane(), createPanel1(), "Center");
         Aladin.makeAdd(getContentPane(), getValidPanel(), "South");
         pack();
         first = false;
      }
//      setBounds(200,100,400,300);
      Point p = aladin.getLocationOnScreen();
      p.x+=aladin.hipsStore.getWidth();
      p.y+=100;
      setLocation( p );
      setVisible(true);
   }

   /** Génération du Panel du formulaire */
//   private JPanel createPanel() {
//      JPanel p = new JPanel( new BorderLayout());
//      JScrollPane sc = new JScrollPane(createPanel1());
//      p.add(sc,BorderLayout.CENTER);
//      return p;
//   }
   
   private JCheckBox bxImage, bxCube, bxCatalog, bxJournal, bxMisc;
   private JCheckBox bxGammaRay, bxXray,bxUV,bxOptical,bxInfrared,bxRadio,bxGasLines;
   private JCheckBox bxBigCat,bxSmallCat;
   private JCheckBox bxPixFull,bxPixColor;
   private JTextField tfFree,tfCoverage,tfHiPSorder,tfUCD,tfDescr;
   
   private Vector<JCheckBox> authVbx,missionVbx,categoryVbx,assdataVbx;
   
   /** Reset du formulaire */
   private void reset() {
      bxBigCat.setSelected(false);
      bxSmallCat.setSelected(false);
      bxImage.setSelected(true);
      bxCube.setSelected(true);
      bxCatalog.setSelected(true);
      bxJournal.setSelected(true);
      bxMisc.setSelected(true);
      bxGammaRay.setSelected(true);
      bxUV.setSelected(true);
      bxOptical.setSelected(true);
      bxInfrared.setSelected(true);
      bxRadio.setSelected(true);
      bxGasLines.setSelected(true);
      bxPixFull.setSelected(false);
      bxPixColor.setSelected(false);
      tfFree.setText("");
      tfCoverage.setText("");
      tfHiPSorder.setText("");
      tfUCD.setText("");
      tfDescr.setText("");
      
      for( JCheckBox bx : authVbx ) bx.setSelected(true);
      for( JCheckBox bx : missionVbx ) bx.setSelected(true);
//      for( JCheckBox bx : categoryVbx ) bx.setSelected(true);
      for( JCheckBox bx : assdataVbx ) bx.setSelected(true);
      
      submitLocal();
   }
   

   /** Construction du panel du formulaire */
   private JPanel createPanel1() {
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
      tfDescr = new JTextField(30);
      (l = new JLabel("Keyword")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "keyword in title, description, collection...\n(ex: DENIS)", 
            tfDescr, g, c, GridBagConstraints.EAST);
      
      // Le type de données
      (l = new JLabel("Data types")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new GridLayout(0,3));
      subPanel.add( bx=bxImage       = new JCheckBox("Image"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxCube        = new JCheckBox("Cube"));    bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxCatalog     = new JCheckBox("Catalog")); bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxJournal     = new JCheckBox("Journal table"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( bx=bxMisc        = new JCheckBox("Miscellaneous"));   bx.setSelected(true); bx.addActionListener(this);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxImage.setSelected(false);
            bxCube.setSelected(false);
            bxCatalog.setSelected(false);
            bxJournal.setSelected(false);
            bxMisc.setSelected(false);
         }
      });
//      categoryVbx = new Vector<JCheckBox>();
//      subPanel = createFilter(categoryVbx, 6, "client_category", "/");
      PropPanel.addCouple(this, p, l, "Data type...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Couverture du ciel
      tfCoverage = new JTextField(15);
      (l = new JLabel("Coverage (ex:>0.5)")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "Pourcentage of sky coverage...\n(ex:<0.2", 
            tfCoverage, g, c, GridBagConstraints.EAST);
      
      // Les différents régimes
      (l = new JLabel("Regimes")).setFont(l.getFont().deriveFont(Font.BOLD));
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
      PropPanel.addCouple(this, p, l, "Wavelength...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Couverture du ciel
      tfHiPSorder = new JTextField(15);
      (l = new JLabel("HiPS order (ex:>12)")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "HiPS order...\n(ex:<5", 
            tfHiPSorder, g, c, GridBagConstraints.EAST);
      
      // Les différentes origines des HiPS
      (l = new JLabel("Authority")).setFont(l.getFont().deriveFont(Font.BOLD));
      authVbx = new Vector<JCheckBox>();
      subPanel = createFilter(authVbx, 6, "ID", "/");
      PropPanel.addCouple(this, p, l, "Authority creator...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Les différentes Missions
      (l = new JLabel("Mission")).setFont(l.getFont().deriveFont(Font.BOLD));
      missionVbx = new Vector<JCheckBox>();
      subPanel = createFilter(missionVbx, 10, "obs_astronomy_kw", null);
      PropPanel.addCouple(this, p, l, "Mission...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Les données associées
      (l = new JLabel("Ass.data")).setFont(l.getFont().deriveFont(Font.BOLD));
      assdataVbx = new Vector<JCheckBox>();
      subPanel = createFilter(assdataVbx, 10, "associated_dataproduct_type", null, true);
      PropPanel.addCouple(this, p, l, "Associated data to a catalog", subPanel, g, c, GridBagConstraints.EAST);
      
      // Types de tuiles
      (l = new JLabel("Pixel formats")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bx=bxPixFull     = new JCheckBox("full dynamic only")); bx.addActionListener(this);
      subPanel.add( bx=bxPixColor    = new JCheckBox("color only"));        bx.addActionListener(this);
      PropPanel.addCouple(this, p, l, "Image HiPS pixel formats...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Tailles des tables
      (l = new JLabel("Cat size")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bx=bxBigCat   = new JCheckBox("big only"));   bx.setSelected(false); bx.addActionListener(this);
      subPanel.add( bx=bxSmallCat = new JCheckBox("small only")); bx.setSelected(false); bx.addActionListener(this);
      PropPanel.addCouple(this, p, l, "Table/catalog number of rows (<1000, >1 000 000)", 
            subPanel, g, c, GridBagConstraints.EAST);
      
      // UCDs
      tfUCD = new JTextField(30);
      (l = new JLabel("UCD constraints")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "UCD constraints...\n(ex: pos.angDistance)", 
            tfUCD, g, c, GridBagConstraints.EAST);

     // Champ libre
      tfFree = new JTextField(30);
      (l = new JLabel("Other constraints")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "Additionnal MocServer constraints...\n(ex: ID=CDS*&obs_astronomy_kw=Seyfert*)", 
            tfFree, g, c, GridBagConstraints.EAST);
      
      return p;
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      aladin.hipsStore.setTreeReady(false);
      aladin.makeCursor(this, Aladin.WAITCURSOR);
      submitLocal();
   }
}
