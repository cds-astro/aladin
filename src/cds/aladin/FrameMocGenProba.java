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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Gestion de la fenetre associee a la creation d'un MOC � partir d'une Map healpix de proba
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (mars 2016) Creation
 */
public final class FrameMocGenProba extends FrameMocGenImg {
   
   protected FrameMocGenProba(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCGENTITLE");
      INFO  = a.chaine.getString("MOCGENPROBAINFO");
      PLANE = a.chaine.getString("MOCFILTERINGPROBA");
   }

   protected boolean isPlanOk(Plan p) { return p.type==Plan.ALLSKYIMG; }
   
   @Override
   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;

      JPanel p=new JPanel();
      p.setLayout(g);
      
      addSpecifPanel(p,c,g);
      
      JPanel pp=new JPanel();
      pp.add( new JLabel("MOC resolution :"));
      mocOrder = getComboRes();
      pp.add(mocOrder);
      p.add(pp);

      return p;
   }
   
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) {
      
      JPanel pp = new JPanel();
      pp.add( new JLabel("Probability threshold : ") );
      pp.add( threshold=new JTextField(5));
      c.gridwidth=GridBagConstraints.REMAINDER;
      g.setConstraints(pp,c);
      p.add(pp);      
   }
   
   private double getThreshold() throws Exception {
      double x = Double.NaN;
      try {
         String s = threshold.getText().trim();
         if( s.length()>0 ) x = Double.parseDouble(s);
      } catch( Exception e ) {}
      return x;
   }
   
   @Override
   protected void submit() {
      try {
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
//         int res= ((PlanBG)ps[0]).getMaxHealpixOrder();
         int order =getOrder();
         if( order>12 ) {
            if( !a.confirmation("Do you really want to generate a so high MOC resolution ?" ) ) return;
         }

         double threshold=getThreshold();
         a.console.printCommand("cmoc -order="+order+" -threshold="+threshold+" "+labelList(ps));
         a.calque.newPlanMoc("MOC "+threshold+" "+ps[0].label,ps,order,0,Double.NaN,Double.NaN,threshold,false);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC generation failed !");
      }

   }

   @Override
   protected void adjustWidgets() { 
      try {
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
         int order = ((PlanBG)ps[0]).getMaxHealpixOrder();
         if( getOrder()>order ) {
            mocOrder.setSelectedIndex(order-FIRSTORDER_S);
         }
      } catch( Exception e ) { }
   };
}
