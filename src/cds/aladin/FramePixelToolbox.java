// Copyright 2014 - UDS/CNRS
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;

import javax.swing.*;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Gestion d'une fenêtre d'outils basiques sur les valeurs des pixels
 * - Indique les plages de codages en fonction du BITPIX
 * - Indique l'intervalle des valeurs présentes dans l'image [dataMin..dataMax]
 * - Indique l'intervalle des valeurs affichées [cutMin..cutMax]
 * - Donne la correspondance des valeurs de pixel :
 *      . la valeur physique => multiplié par BSCALE + BZERO
 *      . l'indice dans la color map
 *      . la valeur des composantes R G B associées
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (janvier 2014) creation
 */
public class FramePixelToolbox extends JFrame {

   protected Aladin aladin;
   
   // Taille de chaque colonne du tableau des pixels
   static final private int W [] = {100,100,50,75,25 };
   
   // Les lignes de la table des pixels
   private PixelLine pVal,pCutMin,pCutMax,pDataMin,pDataMax,pMin,pMax;

   // Les champs annexes
   private JTextField bzeroField,bscaleField,bitpixField,blankField;
   
   private double raw;                  // La valeur du pixel courant (en raw)
   private int bitpix=0;          
   private double bzero=0;
   private double bscale=1;
   private double blank=Double.NaN;
   private double cutMin,cutMax,dataMin,dataMax;
   private ColorModel cm;               // La table des couleurs courantes
   private boolean isTransparent;       // Indique si l'image associée gère la transparence
   
   protected FramePixelToolbox(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      setTitle("Pixel toolbox");
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true, aladin);
      setLocation( Aladin.computeLocation(this) );
      getContentPane().add( createPanelTop(), BorderLayout.NORTH);
      getContentPane().add( createPanelBottom(), BorderLayout.SOUTH);
      pack();
      setVisible(true);
   }
   
   // Création du tableau des différentes valeurs clés de pixel
   // => min/max encodable, min/max dans l'image, min/max du cut, et une valeur courante
   // avec leurs différentes représentations (physical, raw, indexCM, RGB, couleur)
   private JPanel createPanelTop() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      p.setLayout(g);
      
      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      p1.add( new MyLabel("Physical","Physical value associated to the raw pixel value",W[0]) );
      p1.add( new MyLabel("Raw","Encoded pixel value",W[1]) );
      p1.add( new MyLabel("IndexCM","Index in the color map (normalized in [cutMin..cutMax])",W[2]) );
      p1.add( new MyLabel("R G B","Color map Red,Green,Blue components",W[3]) );
      p1.add( new MyLabel("Color","Displayed color",W[4]) );
      PropPanel.addCouple(p,"", p1, g,c);
      
      PixelLine pl;
      pMin = pl = new PixelLine("Min enc.","Smallest encodable value",false,false);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pDataMin = pl = new PixelLine("Data min","Smallest value in the image",false,true);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pCutMin = pl = new PixelLine("Cut min","Smallest displayed value",false,true);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pVal = pl = new PixelLine("Pixel value","Pixel value",true,true);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pCutMax = pl = new PixelLine("Cut max","Largest displayed value",false,true);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pDataMax = pl = new PixelLine("Data max","Largest value in the image",false,true);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      pMax = pl = new PixelLine("Max enc.","Largest encodable value",false,false);
      PropPanel.addCouple(p,pl.label, pl.getPanel(), g,c);
      return p;
   }
   
   // Panel indiquant les paramètres annexes BSCALE,BZERO,BLANK et BITPIX
   private JPanel createPanelBottom() {
      JPanel p = new JPanel( new BorderLayout());
      p.add( createPanelBottomLeft(), BorderLayout.WEST );
      p.add( createPanelBottomRight(), BorderLayout.EAST );
      return p;
   }
   
   private JPanel createPanelBottomRight() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(2,2,2,2);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(10,5,5,20));
      p.setLayout(g);
      
      bitpixField = new JTextField(10);
      bitpixField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { modify(-1); }
      });
      blankField = new JTextField(10);
      blankField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { modify(-1); }
      });
     
      PropPanel.addCouple(p,new JLabel("BITPIX"), bitpixField, g,c);
      PropPanel.addCouple(p,new JLabel("BLANK"), blankField, g,c);
      
      return p;
   }
   
   private JPanel createPanelBottomLeft() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(10,20,5,5));
      p.setLayout(g);
      
      bzeroField = new JTextField(10);
      bzeroField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { modify(-1); }
      });
      bscaleField = new JTextField(10);
      bscaleField.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { modify(-1); }
      });
      
      PropPanel.addCouple(p,new JLabel("BZERO"), bzeroField, g,c);
      PropPanel.addCouple(p,new JLabel("BSCALE"), bscaleField, g,c);
      
      return p;
   }
   
   // Action lors d'une modification de la valeur d'un champ
   // from==0 si l'on vient du tableau des pixels, champ "Physical"
   // from==1 si l'on vient du tableau des pixels, champ "Raw"
   // from==-1 si l'on vient de n'importe quel autre champ
   private void modify(int from) {
      try {
         if( bzeroField.getText().length()>0 )
            bzero = Double.parseDouble( bzeroField.getText() );
         if( bscaleField.getText().length()>0 )
            bscale = Double.parseDouble( bscaleField.getText() );
         if( blankField.getText().length()>0 ) {
            if( blankField.getText().equalsIgnoreCase("NaN") ) blank=Double.NaN;
            else blank = Double.parseDouble( blankField.getText() );
         }
         
         if( from==1 ) {
            raw = Double.parseDouble(pVal.field[from].getText() );
            cutMin = Double.parseDouble(pCutMin.field[from].getText() );
            cutMax = Double.parseDouble(pCutMax.field[from].getText() );
            dataMin = Double.parseDouble(pDataMin.field[from].getText() );
            dataMax = Double.parseDouble(pDataMax.field[from].getText() );
         } else if( from==0 ) {
            raw = (Double.parseDouble(pVal.field[from].getText() )-bzero)/bscale;
            cutMin = (Double.parseDouble(pCutMin.field[from].getText() )-bzero)/bscale;
            cutMax = (Double.parseDouble(pCutMax.field[from].getText() )-bzero)/bscale;
            dataMin = (Double.parseDouble(pDataMin.field[from].getText() )-bzero)/bscale;
            dataMax = (Double.parseDouble(pDataMax.field[from].getText() )-bzero)/bscale;
         }
         
         if( bitpixField.getText().length()>0 ) {
            bitpix = (int)Double.parseDouble( bitpixField.getText() );
            bitpix = (int)(bitpix/8) * 8;
            if( Math.abs(bitpix)>64 ) throw new Exception();
         }
      } catch( Exception e ) { }
      resume();
   }

   /** Positionne la valeur du pixel courant ainsi que tous les paramètres
    * associés au traitement du pixel à partir de la position dans l'image courante
    * @param v La vue contenant l'image courante
    * @param x,y les coordonnées souris de la position courante
    */
   protected void setPixel(ViewSimple v,double x, double y) {
      PlanImage p = (PlanImage)v.pref;
      PointD po   = v.getPosition(x,y);
      double pixel;
      if( p instanceof PlanBG ) {
         p.projd = v.projLocal.copy();
         pixel = ((PlanBG)p).getPixelInDouble(po.x, po.y);
      }
      else {
         int yi = (int)Math.floor(po.y);
         int xi = (int)Math.floor(po.x);
         if( yi<0 || yi>=p.naxis2 || xi<0 || xi>p.naxis1 ) pixel=Double.NaN;
         else {
            if( p.pixelsOrigin!=null ) {
               pixel = p.getPixVal(p.pixelsOrigin,p.bitpix,(p.naxis2-yi-1)*p.naxis1+xi)*p.bScale+p.bZero;
            } else {
               if( !p.pixelsOriginFromDisk() ) pixel=Double.NaN;
               byte [] onePixelOrigin = new byte[p.npix];
               if( !p.getOnePixelFromCache(onePixelOrigin,p.npix,xi,yi) ) pixel = Double.NaN;
               else pixel = p.getPixVal(onePixelOrigin,bitpix,0)*p.bScale+p.bZero;
            }
         }
      }
      setParams(p,pixel);
   }
      
   protected void setParams(PlanImage p,double pixel) {
      bzero = p.bZero;
      bscale = p.bScale;
      blank = p.isBlank ? p.blank : Double.NaN;
      bitpix = p.bitpix;
      raw = pixel==blank ? blank : (pixel-bzero)/bscale;
      cutMin = p.pixelMin;
      cutMax = p.pixelMax;
      dataMin = p.dataMin;
      dataMax = p.dataMax;
      isTransparent = p.isTransparent();
      cm = p.getCM();
      resume();
   }
   
   // Regénération de l'ensemble des valeurs à partir des éléments connus
   private void resume() {
      if( pVal==null ) return;
      pVal.setValue(raw);
      pCutMin.setValue(cutMin);
      pCutMax.setValue(cutMax);
      pDataMin.setValue(dataMin);
      pDataMax.setValue(dataMax);
      bzeroField.setText(Util.myRound(bzero));
      bscaleField.setText(Util.myRound(bscale));
      bitpixField.setText(bitpix+"");
      blankField.setText(blank+"");
      
      pMin.setValue(dataMin);
      String min = bitpix==0 ? "" : bitpix==8 ? "0" : bitpix==16 ? Short.MIN_VALUE+"" 
            : bitpix==32 ? Integer.MIN_VALUE+"" : bitpix==64 ? "-2^63"
            : bitpix==-32 ? Util.myRound(-Float.MAX_VALUE+"",2) : Util.myRound(-Double.MAX_VALUE+"",2);
      pMin.setValue(min,1);
      if( bzero==0 && bscale==1 ) pMin.setValue(min,0);
      else {
         String minp =bitpix==0 ? "" : bitpix==8 ? ""+bzero : bitpix==16 ? Util.myRound(""+(Short.MIN_VALUE*bscale+bzero),2)
               : bitpix==32 ?  Util.myRound(""+(Long.MIN_VALUE*bscale+bzero),2) : bitpix==64 ? Util.myRound(""+(-Math.pow(2,63)*bscale+bzero),2)
               : bitpix==-32 ? Util.myRound(""+(-Float.MAX_VALUE*bscale+bzero),2) : Util.myRound(""+(-Double.MAX_VALUE*bscale+bzero),2);
         pMin.setValue(minp,0);
      }
      
      pMax.setValue(dataMax);
      String max = bitpix==0 ? "" : bitpix==8 ? "255" : bitpix==16 ? Short.MAX_VALUE+"" 
            : bitpix==32 ? Integer.MAX_VALUE+"" : bitpix==64 ? "2^63"
            : bitpix==-32 ? Util.myRound(Float.MAX_VALUE+"",2) : Util.myRound(Double.MAX_VALUE+"",2);
      pMax.setValue(max,1);
      if( bzero==0 && bscale==1 ) pMax.setValue(max,0);
      else {
         String maxp = bitpix==0 ? "" : bitpix==8 ? ""+(255*bscale+bzero) : bitpix==16 ? Util.myRound(""+(Short.MAX_VALUE*bscale+bzero),2)
               : bitpix==32 ?  Util.myRound(""+(Long.MAX_VALUE*bscale+bzero),2) : bitpix==64 ? Util.myRound(""+(Math.pow(2,63)*bscale+bzero),2)
               : bitpix==-32 ? Util.myRound(""+(Float.MAX_VALUE*bscale+bzero),2) : Util.myRound(""+(Double.MAX_VALUE*bscale+bzero),2);
         pMax.setValue(maxp,0);
      }
      
      aladin.glu.log("PixelToolbox","");
   }
   
   // Permet de bloquer la taille d'un Label
   class MyLabel extends JLabel {
      int width=75;
      MyLabel(String s,String tip, int w) {
         super(s,JLabel.CENTER);
         Util.toolTip(this, tip);
         width=w;
      }
      public Dimension getPreferredSize() { 
         return new Dimension(width,super.getPreferredSize().height); 
      }
   }

   // Permet de bloquer la taille d'un champ de saisie
   class MyField extends JTextField {
      int width=75;
      MyField(int w) { width=w; }
      public Dimension getPreferredSize() { 
         return new Dimension(width,super.getPreferredSize().height); 
      }
   }
   
   // Gestion d'une ligne du tableau des pixels
   class PixelLine { 
      JLabel label;                             // Un label...
      MyField [] field = new MyField[W.length]; // ...et 4 champs par ligne
      
      /**
       * Création d'une ligne pour le tableau des pixels
       * @param label Le label en début de ligne
       * @param tip Le tooltip associé à ce label
       * @param bold true s'il faut l'afficher en gras
       * @param editable true si la ligne autorise des modifications de ses valeurs
       */
      PixelLine(String label,String tip,boolean bold,boolean editable) { 
         this.label = new JLabel(label); 
         Util.toolTip(this.label, tip);
         if( bold ) this.label.setFont( this.label.getFont().deriveFont(Font.BOLD));
         
         for( int i=0; i<field.length; i++ ) {
            MyField f = field[i] = new MyField(W[i]);
            if( i==4 ) field[i].setOpaque(true);
            f.setEditable(editable && i<2);
            if( f.isEditable() ) {
               f.setActionCommand(i+"");
               f.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     modify( Integer.parseInt(e.getActionCommand()));
                  }
               });
            }
            if( bold ) f.setFont( f.getFont().deriveFont(Font.BOLD));
         }
      }
      
      // Construit et retourne le panel qui contient les 4 cases de la ligne
      JPanel getPanel() {
         JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
         for( int i=0; i<field.length; i++ ) p.add( field[i] );
         return p;
      }
      
      // Positionne les valeurs de la ligne à partir de la valeur RAW (colonne 1)
      void setValue(double raw) {
         field[0].setText( Double.isNaN(raw) || raw==blank ? "" : Util.myRound(raw*bscale+bzero) );
         field[1].setText( Double.isNaN(raw) ? "NaN" : Util.myRound(raw) );
         int index = getColormapIndex(raw);
         field[2].setText( index+"" );
         if( cm!=null ) {
            int r=cm.getRed(index);
            int g=cm.getGreen(index);
            int b=cm.getBlue(index);
            field[3].setText( String.format("%02X-%02X-%02X", r,g,b) );
            field[4].setBackground( new Color(r,g,b) );
            field[4].setText("");
         } else {
            field[3].setText("");
            field[4].setBackground( Color.white );
            field[4].setText(".");
         }
      }
      
      // Affiche s dans la colonne i, sans recalcul
      void setValue(String s,int i) {
         field[i].setText(s);
      }
      
      // Retourne l'indice pour la colormap correspondant à la valeur du pixel
      // en raw (même code que dans PixelImage.cut(...)
      // L'indice 0 peut être réservé à la transparence le cas échéant
      int getColormapIndex(double c) {
         int pos;
         int range  = isTransparent ? 255 : 256;
         int gapTransp = isTransparent ?   1 :   0;
         double r = range/(cutMax - cutMin);
         range--;
         if( Double.isNaN(c) || c==blank ) pos=0;
         else pos = (gapTransp+ (c<=cutMin?0:c>=cutMax?range:(int)( (c-cutMin)*r) ));
         return pos;
      }
   }
}
