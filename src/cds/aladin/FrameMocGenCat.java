// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.aladin.prop.PropPanel;

/**
 * Gestion de la fenetre associee a la creation d'un MOC à partir d'un catalogue
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (nov 2011) Creation
 */
public final class FrameMocGenCat extends FrameMocGenImg {

   protected FrameMocGenCat(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      INFO  = a.chaine.getString("MOCGENCATINFO");
   }

   protected boolean isPlanOk(Plan p) {
      if( p.isCatalog() ) return true;
      return false;
   }
   
   private JTextField radius;
   private JCheckBox boxRad, boxFov;
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { 
      JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
      
      ButtonGroup bg = new ButtonGroup();
      JCheckBox box = new JCheckBox();
      bg.add( box);
      box.setSelected( true );
      PropPanel.addCouple(frame,p, " - Only the central position:", "Only the source position (lon,lat) is used to populate the MOC", box, g,c);

      JPanel p2 = new JPanel( new BorderLayout(0,0));
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
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
         int order=getOrder();
         double radius = boxRad.isSelected() ? getRadius() : 0;
         boolean fov = boxFov.isSelected();
         String param = "";
         if( fov ) param=" -fov";
         else {
            if( radius>0 ) param=" -radius="+Coord.getUnit(radius);
         }
         
         a.console.printCommand("cmoc -order="+order+param+" "+labelList(ps));
         a.calque.newPlanMoc(ps[0].label+" MOC",ps,order,radius,0,0,Double.NaN,fov);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC generation failed !");
      }
   }
   
   @Override
   protected void adjustWidgets() { };
}
