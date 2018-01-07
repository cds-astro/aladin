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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/**
 * Gestion de la fenetre associee à la conversion BITPIX d'une image
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (sept 2011) Creation
 */
public final class FrameBitpix extends FrameRGBBlink {

   private String TITLE,INFO,HELP1,CUT,CUTLABEL,CAST,PLANE;

   private static int [] BITPIX = { -64,-32,64,32,16,8};
   private static String [] CODE = { "double (-64)","float (-32)","long (64)",
                                     "integer (32)","short (16)","byte (8)" };
   private JRadioButton [] rb;
   private JRadioButton cut;
   private JLabel srcSize,trgSize;
   private ButtonGroup cbBitpix; 

   @Override
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("BITPIXTITLE");
      INFO  = a.chaine.getString("BITPIXINFO");
      HELP1  = a.chaine.getString("BITPIXHELP");
      CUTLABEL    = a.chaine.getString("BITPIXCUTLABEL");
      CUT    = a.chaine.getString("BITPIXCUT");
      CAST    = a.chaine.getString("BITPIXCAST");
   }

   /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameBitpix(Aladin aladin) {
      super(aladin);
      Aladin.setIcon(this);
   }

   @Override
   protected String getTitre() { return TITLE; }

   @Override
   protected String getInformation() { return INFO; }

   @Override
   protected String getHelp() { return HELP1; }

   @Override
   protected int getToolNumber() { return -2; }
   @Override
   protected int getNb() { return 1; }

   @Override
   protected String getLabelSelector(int i) {
      return PLANE;
   }

   /** Recupere la liste des plans images valides */
   @Override
   protected PlanImage[] getPlan() {
      Vector<Plan> v  =a.calque.getPlans(Plan.IMAGE);
      if( v==null ) return new PlanImage[0];
      PlanImage pi [] = new PlanImage[v.size()];
      v.copyInto(pi);
      return pi;
   }

   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }
   
   @Override
   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;
      c.gridwidth=GridBagConstraints.REMAINDER;

      JPanel p=new JPanel();
      p.setLayout(g);
      
      cbBitpix=new ButtonGroup();
      JPanel pp1=new JPanel();
      JLabel l = new JLabel("Bitpix:"); l.setFont(l.getFont().deriveFont(Font.BOLD));
      pp1.add(l);
      JRadioButton cb;
      rb = new JRadioButton[ BITPIX.length ];
      JPanel pbitpix = new JPanel( new GridLayout(2,3));
      for( int i=0; i<rb.length; i++ ) {
         rb[i]=cb=new JRadioButton(CODE[i]); cb.setActionCommand(CODE[i]);
         pbitpix.add(cb); cbBitpix.add(cb);
         cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateTrg(); }
         });
      }
      pp1.add(pbitpix);
      g.setConstraints(pp1,c);
      p.add(pp1);
      
      ButtonGroup cbg=new ButtonGroup();
      JPanel pp=new JPanel();
      l = new JLabel(CUTLABEL); l.setFont(l.getFont().deriveFont(Font.BOLD));
      pp.add(l);
      cut=cb=new JRadioButton(CUT); cb.setActionCommand(CUT);
      cbg.add(cb); pp.add(cb);  cb.setSelected(true);
      cb=new JRadioButton(CAST); cb.setActionCommand(CAST);
      cbg.add(cb); pp.add(cb);
      g.setConstraints(pp,c);
      p.add(pp);
      
      pp=new JPanel();
      srcSize = l= new JLabel("."); l.setFont(l.getFont().deriveFont(Font.ITALIC));
      pp.add(l);
      g.setConstraints(pp,c);
      p.add(pp);
      pp=new JPanel();
      trgSize = l= new JLabel("."); l.setFont(l.getFont().deriveFont(Font.ITALIC));
      pp.add(l);
      g.setConstraints(pp,c);
      p.add(pp);
      

      return p;
   }

   @Override
   protected void submit() {
      try {
         PlanImage p=(PlanImage)getPlan(ch[0]);
         String code = cbBitpix.getSelection().getActionCommand();
         int i = Util.indexInArrayOf(code, CODE);
         int fct = cut.isSelected() ? PlanImageAlgo.BITPIXCUT : PlanImageAlgo.BITPIX;
         a.calque.newPlanImageAlgo(null,p,null,fct,0,""+BITPIX[i],0);

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("Bitpix convertion failed !");
      }
   }
   
   private void updateTrg() {
      PlanImage p =(PlanImage) getPlan(ch[0]);
      if( p==null ) return;
      String code = cbBitpix.getSelection().getActionCommand();
      int i = Util.indexInArrayOf(code, CODE);
      int bitpix=BITPIX[i];
      int width=p.naxis1;
      int height=p.naxis2;
      int n = Math.abs(bitpix)/8;
      long size = (long)width*(long)height*(long)n;
      trgSize.setText("Target size: "+width+"x"+height+" x "+n+" => "+Util.getUnitDisk(size));
   }

   
   private void updateSrc(int bitpix,int width,int height) {
      for( int i=0; i<BITPIX.length; i++ ){
         rb[i].setSelected( BITPIX[i]==bitpix );
         rb[i].setEnabled( BITPIX[i]!=bitpix );
      }
      int n = Math.abs(bitpix)/8;
      long size = (long)width*(long)height*(long)n;
      srcSize.setText("Current size: "+width+"x"+height+" x "+n+" => "+Util.getUnitDisk(size));
   }

   @Override
   protected void adjustWidgets() {
      srcSize.setText("");
      trgSize.setText("");
      PlanImage p = (PlanImage)getPlan(ch[0]);
      if( p==null ) return;
      updateSrc(p.bitpix,p.naxis1,p.naxis2);
   }
}
