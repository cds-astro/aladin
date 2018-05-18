// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.aladin.prop.PropPanel;
import cds.moc.Healpix;
import cds.moc.TMoc;

/**
 * Gestion de la fenetre associee a la creation d'un MOC à partir d'un catalogue
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (nov 2011) Creation
 */
public class FrameTMocGenCat extends FrameMocGenImg {

   protected FrameTMocGenCat(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      INFO  = a.chaine.getString("TMOCGENCATINFO");
   }

   protected boolean isPlanOk(Plan p) {
      if( p.isCatalogTime() ) return true;
      return false;
   }
   
   protected JComboBox getComboRes() {
      JComboBox c = new JComboBox();
      for( int o=FIRSTORDER; o<=Healpix.MAXORDER; o++ ) {
         String s = "Order "+o+" => "+TMoc.getTemps( TMoc.getDuration(o) );
         c.addItem(s);
      }
      c.setSelectedIndex(7);
      return c;
   }
   
   private JTextField duration;
   private JCheckBox boxDuration;
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { 
      
      JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
      
      ButtonGroup bg = new ButtonGroup();
      JCheckBox box = new JCheckBox();
      bg.add( box);
      box.setSelected( true );
      PropPanel.addCouple(frame,p, " - No duration", "Only the epoch is used to populate the temporal MOC", box, g,c);

      JPanel p2 = new JPanel( new BorderLayout(0,0));
      duration=new JTextField("3", 5);
      box =boxDuration= new JCheckBox();
      bg.add( box);
      p2.add(box,BorderLayout.WEST);
      p2.add(duration,BorderLayout.CENTER);
      p2.add(new JLabel(" in secondes"),BorderLayout.EAST);
      PropPanel.addCouple(frame,p, " - Duration from starting time:","Duration of the event", p2, g,c);
      
   }
   
   private double getDuration() throws Exception {
      double x=0;
      try {
         String s = duration.getText().trim();
         if( s.length()>0 ) x=Double.parseDouble(s);
      } catch( Exception e ) {
         duration.setForeground(Color.red);
         throw e;
      }
      duration.setForeground(Color.black);
      return x;
   }
   
   @Override
   protected void submit() {
      try {
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
         int order=getOrder();
         double duration = boxDuration.isSelected() ? getDuration() : 0;
         String param = "";
         if( duration>0 ) param=" -duration="+duration;
         
         a.console.printCommand("cmoc -time -order="+order+param+" "+labelList(ps));
         a.calque.newPlanTMoc(ps[0].label+" TMOC",ps,order,duration);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("TMOC generation failed !");
      }
   }
   
   @Override
   protected void adjustWidgets() { };
}
