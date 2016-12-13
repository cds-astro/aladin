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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URLEncoder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Classe qui gère le formulaire de filtrage de l'arbre HiPS
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public final class HipsFilter extends JFrame {
   
   private boolean first=true;
   private Aladin  aladin;  // référence externe
   
   // Préfixe des paramètres de filtrage des HiPS par le MocServer
   private static String MOCSERVER_FILTERING 
        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
         +"&hips_service_url=*&casesensitive=false";
   
   public HipsFilter(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);
   }

   /** Construction du panel des boutons de validation */
   protected JPanel getValidPanel() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
      p.add( b=new JButton("Apply")); 
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(); }
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
   
   /** Excécution du filtrage (génération de la requête MocServer correspondante au formulaire
    * puis appel au MocServer */
   private void submit() {
      StringBuilder params = new StringBuilder(MOCSERVER_FILTERING);
      
      if( !bxImage.isSelected() || !bxCube.isSelected() || !bxCatalog.isSelected() ) {
         if( !bxImage.isSelected() )      params.append("&client_category=!Image*");
         if( !bxCube.isSelected() )       params.append("&client_category=!Cube*");
         if( !bxCatalog.isSelected() )    params.append("&client_category=!Catalog*");
         if( !bxTypeOthers.isSelected() ) params.append("&client_category=Catalog*,Image*,Cube*");
      }
      
      if( !bxGammaRay.isSelected() || !bxXray.isSelected() || !bxUV.isSelected() || !bxOptical.isSelected()
            || !bxInfrared.isSelected() || !bxRadio.isSelected() || !bxGasLines.isSelected() ) {
         if( !bxGammaRay.isSelected() )  params.append("&obs_regime=!Gamma-ray");
         if( !bxXray.isSelected() )      params.append("&obs_regime=!X-ray");
         if( !bxUV.isSelected() )        params.append("&obs_regime=!UV");
         if( !bxOptical.isSelected() )   params.append("&obs_regime=!Optical");
         if( !bxInfrared.isSelected() )  params.append("&obs_regime=!Infrared");
         if( !bxRadio.isSelected() )     params.append("&obs_regime=!Radio");
         if( !bxGasLines.isSelected() )  params.append("&client_category=!Image/Gas-lines/*");
       }
      
      if( bxPixFull.isSelected() || bxPixColor.isSelected() ) {
         if( bxPixFull.isSelected() )     params.append("&hips_tile_format=*fits*");
         if( bxPixColor.isSelected() )    params.append("&dataproduct_subtype=color");
      }
      
      if( !bxCDS.isSelected() || !bxESAVO.isSelected() || !bxJAXA.isSelected() || !bxOVGSO.isSelected()
            || !bxOriginOthers.isSelected() ) {
         if( !bxCDS.isSelected() )          params.append("&ID=!CDS/*");
         if( !bxESAVO.isSelected() )        params.append("&ID=!ESAVO/*");
         if( !bxJAXA.isSelected() )         params.append("&ID=!JAXA/*");
         if( !bxOVGSO.isSelected() )        params.append("&ID=!ov-gso/*");
         if( !bxOriginOthers.isSelected() ) params.append("&ID=CDS/*,ESAVO/*,JAXA/*,ov-gso/*");
       }
      
      String s;
      if( (s=getText(tfCoverage)).length()!=0 )  params.append("&moc_sky_fraction="+s);
      if( (s=getText(tfDescr)).length()!=0 )     params.append("&obs_title,obs_description,obs_collection,ID=*"+s+"*");
      if( (s=getText(tfHiPSorder)).length()!=0 ) params.append("&hips_order="+s);
      if( (s=getText(tfUCD)).length()!=0 )       params.append("&data_ucd="+s);
      if( (s=getText(tfFree)).length()!=0 )      params.append("&"+s);
      
      aladin.hipsMarket.resumeTree(params.toString());
   }
   
   /** Retourne le string d'un JtextField encodé en HTTP */
   private String getText(JTextField tf) { return URLEncoder.encode(tf.getText().trim()); }
   
   /** Affichage du panel pour permettre à l'utilisateur de modifier son filtre */
   public void show() {
      if( first ) {
         setTitle("HiPS tree filter");
         ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
         Aladin.makeAdd(getContentPane(), createPanel(), "Center");
         Aladin.makeAdd(getContentPane(), getValidPanel(), "South");
         pack();
         first = false;
         setLocation(Aladin.computeLocation(this));
      }
      super.show();
   }

   /** Génération du Panel du formulaire */
   private JPanel createPanel() {
      JPanel p = new JPanel( new BorderLayout());
      JScrollPane sc = new JScrollPane(createPanel1());
      p.add(sc,BorderLayout.CENTER);
      return p;
   }
   
   private JCheckBox bxImage, bxCube, bxCatalog,bxTypeOthers;
   private JCheckBox bxGammaRay, bxXray,bxUV,bxOptical,bxInfrared,bxRadio,bxGasLines;
   private JCheckBox bxCDS,bxESAVO,bxJAXA,bxOVGSO,bxOriginOthers;
   private JCheckBox bxPixFull,bxPixColor;
   private JTextField tfFree,tfCoverage,tfHiPSorder,tfUCD,tfDescr;
   
   /** Reset du formulaire */
   private void reset() {
      bxImage.setSelected(true);
      bxCube.setSelected(true);
      bxCatalog.setSelected(true);
      bxTypeOthers.setSelected(true);
      bxGammaRay.setSelected(true);
      bxUV.setSelected(true);
      bxOptical.setSelected(true);
      bxInfrared.setSelected(true);
      bxRadio.setSelected(true);
      bxGasLines.setSelected(true);
      bxPixFull.setSelected(false);
      bxPixColor.setSelected(false);
      bxCDS.setSelected(true);
      bxESAVO.setSelected(true);
      bxJAXA.setSelected(true);
      bxOVGSO.setSelected(true);
      bxOriginOthers.setSelected(true);
      tfFree.setText("");
      tfCoverage.setText("");
      tfHiPSorder.setText("");
      tfUCD.setText("");
      tfDescr.setText("");
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
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bx=bxImage       = new JCheckBox("Image"));     bx.setSelected(true);
      subPanel.add( bx=bxCube        = new JCheckBox("Cube"));      bx.setSelected(true);
      subPanel.add( bx=bxCatalog     = new JCheckBox("Catalog"));   bx.setSelected(true);
      subPanel.add( bx=bxTypeOthers  = new JCheckBox("Others"));    bx.setSelected(true);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxImage.setSelected(false);
            bxCube.setSelected(false);
            bxCatalog.setSelected(false);
            bxTypeOthers.setSelected(false);
         }
      });
      PropPanel.addCouple(this, p, l, "HiPS data type...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Couverture du ciel
      tfCoverage = new JTextField(15);
      (l = new JLabel("Sky coverage (ex:>0.5)")).setFont(l.getFont().deriveFont(Font.BOLD));
      PropPanel.addCouple(this, p, l, "Pourcentage of sky coverage...\n(ex:<0.2", 
            tfCoverage, g, c, GridBagConstraints.EAST);
      
      // Les différents régimes
      (l = new JLabel("Regimes")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new GridLayout(2,3) );
      subPanel.add( bx=bxGammaRay = new JCheckBox("Gamma-ray")); bx.setSelected(true);
      subPanel.add( bx=bxXray     = new JCheckBox("X-ray"));     bx.setSelected(true);
      subPanel.add( bx=bxUV       = new JCheckBox("UV"));        bx.setSelected(true);
      subPanel.add( bx=bxOptical  = new JCheckBox("Optical"));   bx.setSelected(true);
      subPanel.add( bx=bxInfrared = new JCheckBox("Infrared"));  bx.setSelected(true);
      subPanel.add( bx=bxRadio    = new JCheckBox("Radio"));     bx.setSelected(true);
      subPanel.add( bx=bxGasLines = new JCheckBox("Gas-lines")); bx.setSelected(true);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxGammaRay.setSelected(false);
            bxXray.setSelected(false);
            bxUV.setSelected(false);
            bxGammaRay.setSelected(false);
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
      (l = new JLabel("HiPS creators")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bx=bxCDS          = new JCheckBox("CDS"));     bx.setSelected(true);
      subPanel.add( bx=bxESAVO        = new JCheckBox("ESAVO"));   bx.setSelected(true);
      subPanel.add( bx=bxJAXA         = new JCheckBox("JAXA"));    bx.setSelected(true);
      subPanel.add( bx=bxOVGSO        = new JCheckBox("OVGSO"));   bx.setSelected(true);
      subPanel.add( bx=bxOriginOthers = new JCheckBox("Others"));  bx.setSelected(true);
      subPanel.add( b = new JButton("none"));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bxCDS.setSelected(false);
            bxESAVO.setSelected(false);
            bxJAXA.setSelected(false);
            bxOVGSO.setSelected(false);
            bxOriginOthers.setSelected(false);
         }
      });
      PropPanel.addCouple(this, p, l, "HiPS creator...", subPanel, g, c, GridBagConstraints.EAST);
      
      // Types de tuiles
      (l = new JLabel("Pixel formats")).setFont(l.getFont().deriveFont(Font.BOLD));
      subPanel = new JPanel( new FlowLayout());
      subPanel.add( bxPixFull     = new JCheckBox("full dynamic only"));
      subPanel.add( bxPixColor    = new JCheckBox("color only"));
      PropPanel.addCouple(this, p, l, "Image HiPS pixel formats...", subPanel, g, c, GridBagConstraints.EAST);
      
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


}
