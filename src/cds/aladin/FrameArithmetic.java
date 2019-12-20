// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.*;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/**
 * Gestion de la fenetre associee a la creation d'un plan arithmétic
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2008) Creation
 */
public final class FrameArithmetic extends FrameRGBBlink {

   String TITLE,INFO,HELP1,ADD,SUB,MUL,DIV,NORM,PPV,BIL,METHOD,PLANE,PLANEVALUE;

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des opérations possibles
   private JCheckBox cbNorm;         // Le checkbox pour indiquer une normalisation préalable
   private ButtonGroup cbMethod;         // Le Jradio pour indiquer la méthode de sampling

   @Override
protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("ARITHTITLE");
      INFO  = a.chaine.getString("ARITHINFO");
      HELP1  = a.chaine.getString("ARITHHELP");
      ADD   = a.chaine.getString("ARITHADD");
      SUB = a.chaine.getString("ARITHSUB");
      MUL  = a.chaine.getString("ARITHMUL");
      DIV  = a.chaine.getString("ARITHDIV");
      NORM  = a.chaine.getString("ARITHNORM");
      PPV= a.chaine.getString("RSPPPV");
      BIL= a.chaine.getString("RSPBIL");
      METHOD    = a.chaine.getString("ARITHMETHOD");
      PLANE    = a.chaine.getString("ARITHPLANE");
      PLANEVALUE    = a.chaine.getString("ARITHPLANEVALUE");
   }

  /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameArithmetic(Aladin aladin) {
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
protected int getNb() { return 2; }

   @Override
protected String getLabelSelector(int i) {
      return i==0 ? PLANE : PLANEVALUE;
   }

   /** Recupere la liste des plans images valides */
   @Override
protected Plan[] getPlan() {
      Vector<Plan> v  =a.calque.getPlans(Plan.IMAGE);
      if( v==null ) return new PlanImage[0];
      Plan pi [] = new PlanImage[v.size()];
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

      JPanel p=new JPanel();
      p.setLayout(g);

      cbg=new ButtonGroup();

      JPanel pp=new JPanel();
      JRadioButton cb;
      cb=new JRadioButton(ADD); cb.setActionCommand(ADD);
      cbg.add(cb); pp.add(cb);  cb.setSelected(true);
      cb=new JRadioButton(SUB); cb.setActionCommand(SUB);
      cbg.add(cb); pp.add(cb);
      cb=new JRadioButton(MUL); cb.setActionCommand(MUL);
      cbg.add(cb); pp.add(cb);
      cb=new JRadioButton(DIV); cb.setActionCommand(DIV);
      cbg.add(cb); pp.add(cb);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);

      // Normalisation ?
      cbNorm=new JCheckBox(NORM);
      c.fill=GridBagConstraints.NONE;
      g.setConstraints(cbNorm,c);
      p.add(cbNorm);
      c.fill=GridBagConstraints.BOTH;

      // Choix de la méthode
      JPanel p2=new JPanel();
      p2.add(new JLabel(METHOD));
      cbMethod=new ButtonGroup();
      cb=new JRadioButton(PPV); cb.setActionCommand(PPV); cbMethod.add(cb); cb.setSelected(true);
      p2.add(cb);
      cb=new JRadioButton(BIL); cb.setActionCommand(BIL); cbMethod.add(cb);
      p2.add(cb);
      c.fill=GridBagConstraints.NONE;
      g.setConstraints(p2,c);
      p.add(p2);
      c.fill=GridBagConstraints.BOTH;

      return p;
   }

   private int getOperation(String s) {
      if( s.equals(ADD) ) return PlanImageAlgo.ADD;
      if( s.equals(SUB) ) return PlanImageAlgo.SUB;
      if( s.equals(MUL) ) return PlanImageAlgo.MUL;
      return PlanImageAlgo.DIV;
   }

   @Override
protected void submit() {
      try {
         PlanImage p1=(PlanImage)getPlan(ch[0]), p2=(PlanImage)getPlan(ch[1]);

         double coef=0;
         if( p2==null ) {
            coef = Double.parseDouble(((String)ch[1].getSelectedItem()).trim());
         }

         int methode = cbMethod.getSelection().getActionCommand().equals(PPV)?
               PlanImageAlgo.PPV:PlanImageAlgo.BILINEAIRE;
         String s=cbg.getSelection().getActionCommand();
         int fct=getOperation(s);
         
         // Dans le cas où il faudrait normaliser avant le calcul
         if( cbNorm.isSelected() ) {
            if( p1!=null ) p1 = PlanImageAlgo.normalise(p1);
            if( p2!=null ) p2 = PlanImageAlgo.normalise(p2);
         }
         
         a.calque.newPlanImageAlgo(s.substring(0,3),p1,p2,fct,coef,null,methode);
//         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("Arithmetic operation failed !");
      }

    }

   @Override
protected void adjustWidgets() {
      ch[1].setEditable(true);
   };
}
