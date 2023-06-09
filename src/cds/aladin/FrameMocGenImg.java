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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Gestion de la fenetre associee a la creation d'un MOC � partir d'une image ou d'un planBG image
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (nov 2011) Creation
 */
public class FrameMocGenImg extends FrameRGBBlink {

   String TITLE,INFO,HELP1,PLANE;
   
   static final protected int FIRSTORDER_S=3;
   static final protected int FIRSTORDER_T=10;
   
   static protected JComboBox makeComboSpaceRes() {
      JComboBox c = new JComboBox();
      for( int o=FIRSTORDER_S; o<=SMoc.MAXORD_S; o++ ) {
         String s = "Order "+o+" => "+Coord.getUnit( CDSHealpix.pixRes( o)/3600. );
         c.addItem(s);
      }
      c.setSelectedIndex(9);   // about 7'
      return c;
   }

   static protected JComboBox makeComboTimeRes() {
      JComboBox c = new JComboBox();
      for( int o=FIRSTORDER_T; o<=TMoc.MAXORD_T; o++ ) {
         String s = "Order "+o+" => "+Util.getTemps( TMoc.getDuration(o) );
         c.addItem(s);
      }
      c.setSelectedIndex(34);   // ~1 day
      return c;
   }

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des op�rations possibles

   @Override
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCGENTITLE");
      INFO  = a.chaine.getString("MOCGENIMGINFO");
      HELP1  = a.chaine.getString("MOCHELP");
      PLANE    = a.chaine.getString("MOCPLANE");
   }

   /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameMocGenImg(Aladin aladin) {
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

   protected boolean isPlanOk(Plan p) {
//      if( p instanceof PlanBG && !((PlanBG)p).isLocalAllSky() ) return false;
      if( p instanceof PlanImage && ((PlanImage)p).isPixel() ) return true;
      if( p.isCatalog() ) return true;
      return false;
   }
                                    
   /** Recupere la liste des plans images et catalogues valides */
   @Override
   protected Plan[] getPlan() {
      Plan [] p = a.calque.getPlans();
      int n=0;
      for( int i=0; i<p.length; i++ ) {
         if( isPlanOk(p[i]) ) n++;
      }
      Plan [] pi = new Plan[n];
      for( int i=0,j=0; i<p.length; i++ ) {
         if( isPlanOk(p[i]) ) pi[j++]=p[i];
      }
  
      return pi;
   }


   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }

   
   JComboBox mocOrder;
   JTextField minRange,maxRange;
   JTextField threshold;
   JCheckBox rangeCheckBox;
   
   @Override
   protected JPanel getAddPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH;

      JPanel p=new JPanel();
      p.setLayout(g);
      
      addSpecifPanel(p,c,g);
      
      cbg=new ButtonGroup();

      JPanel pp=new JPanel();
      pp.add( new JLabel("MOC resolution :"));
      mocOrder = getComboRes();
      pp.add(mocOrder);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);
      
      return p;
   }
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) {
      JPanel pp = new JPanel();
      pp.add( new JLabel("Pixel range: [") );
      pp.add( minRange=new JTextField(3));
      pp.add( new JLabel("..") );
      pp.add( maxRange=new JTextField(3));
      pp.add( new JLabel("]") );
      c.gridwidth=GridBagConstraints.REMAINDER;
      g.setConstraints(pp,c);
      p.add(pp);
   }
   
   
   protected JComboBox getComboRes() { return makeComboSpaceRes(); }
   
   protected int getOrder() { return mocOrder.getSelectedIndex()+FIRSTORDER_S; }
   
   
   private double getMin() throws Exception {
      double min = Double.NaN;
      try {
         String s = minRange.getText().trim();
         if( s.length()>0 ) min = Double.parseDouble(s);
      } catch( Exception e ) {
         minRange.setForeground(Color.red);
         throw e;
      }
      minRange.setForeground(Color.black);
      return min;
   }

   private double getMax() throws Exception {
      double max = Double.NaN;
      try {
         String s = maxRange.getText().trim();
         if( s.length()>0 ) max = Double.parseDouble(s);
      } catch( Exception e ) {
         maxRange.setForeground(Color.red);
         throw e;
      }
      maxRange.setForeground(Color.black);
      return max;
   }

  
   @Override
   protected void submit() {
      try {
         Plan [] ps = new Plan[]{ getPlan(ch[0]) };
         int order=getOrder();
         if( order>12 ) {
            if( !a.confirmation("Do you really want to generate a so high MOC resolution ?" ) ) return;
         }
         double pixMin=getMin();
         double pixMax=getMax();
         
         String pixelCut="";
         if( !Double.isNaN(pixMin) || !Double.isNaN(pixMax) ) {
            pixelCut = " -pixelCut="+pixMin+"/"+pixMax+"";
         }
         a.console.printCommand("cmoc -order="+order+pixelCut+" "+labelList(ps));
         a.calque.newPlanMoc(ps[0].label+" MOC",ps,order,0,pixMin,pixMax,Double.NaN,false);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC generation failed !");
      }

   }
   
   /** Construit une liste de noms de plans, �venutellement quot�s afin
    * de pouvoir afficher la commande script qui va bien
    * @param ps liste des plans concern�s
    */
   static protected String labelList(Plan [] ps) {
      StringBuilder s = null;
      for( Plan p : ps ) {
         if( s==null ) s = new StringBuilder( Tok.quote(p.label) );
         else  s.append(" "+Tok.quote(p.label));
      }
      return s.toString();
   }


   @Override
   protected void adjustWidgets() { };
}
