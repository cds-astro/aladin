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

import java.awt.*;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.moc.Healpix;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

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
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { 
      JPanel pp = new JPanel();
      pp.add( new JLabel("Radius (in arcsec):") );
      pp.add( radius=new JTextField(5));
      c.gridwidth=GridBagConstraints.REMAINDER;
      g.setConstraints(pp,c);
      p.add(pp);
   }
   
   private double getRadius() throws Exception {
      double x=0;
      try {
         String s = radius.getText().trim();
         if( s.length()>0 ) x=Server.getAngle(s,Server.RADIUSs);
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
         int res=getOrder();
         double radius = getRadius();
         a.calque.newPlanMoc(ps[0].label+" MOC",ps,res,radius,0,0);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning("MOC generation failed !");
      }

   }

   @Override
   protected void adjustWidgets() { };
}
