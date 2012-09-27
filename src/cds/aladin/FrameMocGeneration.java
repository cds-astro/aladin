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
 * Gestion de la fenetre associee a la creation d'un plan arithmétic
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2008) Creation
 */
public final class FrameMocGeneration extends FrameRGBBlink {

   String TITLE,INFO,HELP1,PLANE;

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des opérations possibles

   @Override
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCGENTITLE");
      INFO  = a.chaine.getString("MOCGENINFO");
      HELP1  = a.chaine.getString("MOCHELP");
      PLANE    = a.chaine.getString("MOCPLANE");
   }

   /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameMocGeneration(Aladin aladin) {
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
   protected int getNb() { return 10; }

   @Override
   protected String getLabelSelector(int i) {
      return PLANE;
   }

   /** Recupere la liste des plans images et catalogues valides */
   @Override
   protected Plan[] getPlan() {
      Vector<Plan> v  = a.calque.getPlans(Plan.IMAGE);
      Vector<Plan> v2  =a.calque.getPlans(Plan.CATALOG);
      if( v==null ) v=v2;
      else if( v2!=null ) v.addAll(v2);
      if( v==null ) return new Plan[0];
      Plan pi [] = new Plan[v.size()];
      v.copyInto(pi);
      return pi;
   }


   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }

   
   JComboBox mocRes;
   
   @Override
   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;

      JPanel p=new JPanel();
      p.setLayout(g);

      cbg=new ButtonGroup();

      JPanel pp=new JPanel();
      pp.add( new JLabel("MOC resolution :"));
      mocRes = getComboRes();
      pp.add(mocRes);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);

      return p;
   }
   
   static final private int FIRSTORDER=8;
   
   private JComboBox getComboRes() {
      JComboBox c = new JComboBox();
      for( int o=FIRSTORDER; o<=Healpix.MAXORDER; o++ ) {
         String s = "Order "+o+" => "+Coord.getUnit( CDSHealpix.pixRes( CDSHealpix.pow2(o))/3600. );
         c.addItem(s);
      }
      return c;
   }
   
   private int getRes() { return mocRes.getSelectedIndex()+FIRSTORDER; }

   
   @Override
   protected void submit() {
      try {
         // Décompte
         int n=0;
         for( int i=0; i<ch.length; i++ ) {
            Plan p = getPlan(ch[i]);
            if( p==null ) continue;
            n++;
         }
         Plan [] ps = new Plan[n];
         n=0;
         for( int i=0; i<ch.length; i++ ) {
            Plan p = getPlan(ch[i]);
            if( p==null ) continue;
            ps[n++] = p;
         }
        
         int res=getRes();
         a.calque.newPlanMoc("MOC",ps,res);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning("MOC generation failed !");
      }

   }

   @Override
   protected void adjustWidgets() { };
}
