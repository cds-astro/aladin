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
public final class FrameMocOperation extends FrameRGBBlink {

   String TITLE,INFO,HELP1,UNION,INTER,SUB,COMP,PLANE;

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des opérations possibles

   @Override
protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCTITLE");
      INFO  = a.chaine.getString("MOCINFO");
      HELP1  = a.chaine.getString("MOCHELP");
      UNION   = a.chaine.getString("MOCUNION");
      INTER = a.chaine.getString("MOCINTER");
      SUB  = a.chaine.getString("MOCSUB");
      COMP  = a.chaine.getString("MOCCOMP");
      PLANE    = a.chaine.getString("MOCPLANE");
   }

  /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameMocOperation(Aladin aladin) {
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
      return PLANE;
   }

   /** Recupere la liste des plans images valides */
   @Override
protected Plan[] getPlan() {
      Vector<Plan> v  =a.calque.getPlans(Plan.ALLSKYMOC);
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
      cb=new JRadioButton(UNION); cb.setActionCommand(UNION);
      cbg.add(cb); pp.add(cb);  cb.setSelected(true);
      cb=new JRadioButton(INTER); cb.setActionCommand(INTER);
      cbg.add(cb); pp.add(cb);
      cb=new JRadioButton(SUB); cb.setActionCommand(SUB);
      cbg.add(cb); pp.add(cb);
      cb=new JRadioButton(COMP); cb.setActionCommand(COMP);
      cbg.add(cb); pp.add(cb);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);


      return p;
   }

   private int getOperation(String s) {
      if( s.equals(UNION) ) return PlanImageAlgo.ADD;
      if( s.equals(INTER) ) return PlanImageAlgo.SUB;
      if( s.equals(SUB) ) return PlanImageAlgo.MUL;
      return PlanImageAlgo.DIV;
   }

   @Override
protected void submit() {
      try {
         PlanMoc p1=(PlanMoc)getPlan(ch[0]), p2=(PlanMoc)getPlan(ch[1]);

         String s=cbg.getSelection().getActionCommand();
         int fct=getOperation(s);
         
         System.out.println("Il faudrait que j'opère sur "+p1+" et "+p2+" l'opération "+s+" ("+fct+")");
         
         a.calque.newPlanMoc(s.substring(0,3),p1,p2,fct);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning("MOC operation failed !");
      }

    }

   @Override
protected void adjustWidgets() {
      ch[1].setEditable(true);
   };
}
