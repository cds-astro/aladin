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
import cds.moc.TimeMoc;

/**
 * Gestion de la fenetre associee a la creation d'un Space MOC à partir d'un catalogue
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (avril 2019) Creation
 */
public class FrameSTMocGenCat extends FrameMocGenImg {
   
   private JTextField radius;
   private JCheckBox boxRad, boxFov;

   protected FrameSTMocGenCat(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      INFO  = a.chaine.getString("STMOCGENCATINFO");
   }

   protected boolean isPlanOk(Plan p) {
      if( p.isCatalogTime() ) return true;
      return false;
   }
   
   protected JComboBox getComboTimeRes() {
      JComboBox c = new JComboBox();
      for( int o=FIRSTORDER; o<=Healpix.MAXORDER; o++ ) {
         String s = "Order "+o+" => "+TimeMoc.getTemps( TimeMoc.getDuration(o) );
         c.addItem(s);
      }
      c.setSelectedIndex(7);
      return c;
   }
   
   JComboBox mocTimeOrder;
   private JTextField duration;
   private JCheckBox boxDuration;
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { 
      
      JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
      
      
      // Paramètres temporels
      PropPanel.addFilet(p, g, c);
      PropPanel.addSectionTitle(p, "Temporal parameters", g, c);
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

      c.gridwidth=GridBagConstraints.REMAINDER;
      JPanel pp=new JPanel();
      pp.add( new JLabel("Time resolution :"));
      mocTimeOrder = getComboTimeRes();
      pp.add(mocTimeOrder);
      g.setConstraints(pp,c);
      p.add(pp);
      
      PropPanel.addFilet(p, g, c);
      PropPanel.addSectionTitle(p, "Spacial parameters", g, c);
      
      // Paramètres spaciaux
      bg = new ButtonGroup();
      box = new JCheckBox();
      bg.add( box);
      box.setSelected( true );
      PropPanel.addCouple(frame,p, " - Only the central position:", "Only the source position (lon,lat) is used to populate the MOC", box, g,c);

      p2 = new JPanel( new BorderLayout(0,0));
      radius=new JTextField("3", 5);
      box =boxRad= new JCheckBox();
      bg.add( box);
      p2.add(box,BorderLayout.WEST);
      p2.add(radius,BorderLayout.CENTER);
      p2.add(new JLabel(" in arcsec"),BorderLayout.EAST);
      PropPanel.addCouple(frame,p, " -Radius around position:","A circle of the disgnated radius centered on each source is used to populate the MOC", p2, g,c);
      
      box =boxFov= new JCheckBox();
      bg.add( box);
      PropPanel.addCouple(frame,p, " - FoV associated to each source:", "The Field of View (for instance s_region information) associated to each source is used to populate the MOC.", box, g,c);

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
   
   protected int getTimeOrder() { return mocTimeOrder.getSelectedIndex()+FIRSTORDER; }

   private double getRadius() throws Exception {
      double x=0;
      try {
         String s = radius.getText().trim();
         if( s.length()>0 ) x=Server.getAngleInArcmin(s,Server.RADIUSs)/60.;
      } catch( Exception e ) {
         radius.setForeground(Color.red);
         throw e;
      }
      radius.setForeground(Color.black);
      return x;
   }
   
   @Override
   protected void submit() {
      try {
         StringBuilder param = new StringBuilder();
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
         int spaceOrder=getOrder();
         int timeOrder=getTimeOrder();
         double duration = boxDuration.isSelected() ? getDuration() : 0;
         if( duration>0 ) param.append(" -duration="+duration);
         
         double radius = boxRad.isSelected() ? getRadius() : 0;
         boolean fov = boxFov.isSelected();
         if( fov ) param.append(" -fov");
         else {
            if( radius>0 ) param.append(" -radius="+Coord.getUnit(radius) );
         }
         
         
         a.console.printCommand("cmoc -spacetime -order="+spaceOrder+"/"+timeOrder+param+" "+labelList(ps));
         a.calque.newPlanSTMoc(ps[0].label+" STMOC",ps,spaceOrder,timeOrder,duration,radius,fov);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("STMOC generation failed !");
      }
   }
   
   @Override
   protected void adjustWidgets() { };
}
