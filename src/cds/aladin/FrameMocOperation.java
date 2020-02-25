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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cds.aladin.prop.PropPanel;
import cds.moc.Moc;

/**
 * Gestion de la fenetre associee a la creation d'un plan arithmétic
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2008) Creation
 */
public final class FrameMocOperation extends FrameRGBBlink {

   String TITLE,INFO,HELP1,SUNION,INTER,SUB,DIFF,COMP,PLANE;

   // Les composantes de l'objet
   private ButtonGroup cbg;	         // Les checkBox des opérations possibles
   private JRadioButton rUnion,rInter,rSub,rDiff,rComp;
   private JComboBox mocTimeOrder,mocSpaceOrder;

   @Override
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCTITLE");
      INFO  = a.chaine.getString("MOCINFO");
      HELP1  = a.chaine.getString("MOCHELP");
      SUNION   = a.chaine.getString("MOCUNION");
      INTER = a.chaine.getString("MOCINTER");
      SUB  = a.chaine.getString("MOCSUB");
      DIFF  = a.chaine.getString("MOCDIFF");
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
   protected int getToolNumber() { return ToolBox.MOC; }
   @Override
   protected int getNb() { return 10; }

   @Override
   protected String getLabelSelector(int i) {
      return PLANE;
   }

   /** Recupere la liste des plans images valides */
   @Override
   protected Plan[] getPlan() {
      Vector<Plan> v  = a.calque.getPlans( PlanMoc.class );
      if( v==null ) return new PlanImage[0];
      Plan pi [] = new PlanImage[v.size()];
      v.copyInto(pi);
      return pi;
   }


   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }
   
   protected JComboBox getComboSpaceRes() { return FrameMocGenImg.makeComboSpaceRes(); }
   protected JComboBox getComboTimeRes()  { return FrameMocGenImg.makeComboTimeRes(); }

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
      rUnion=cb=new JRadioButton(SUNION); cb.setActionCommand(SUNION);
      cbg.add(cb); pp.add(cb);
      rInter=cb=new JRadioButton(INTER); cb.setActionCommand(INTER);
      cbg.add(cb); pp.add(cb); cb.setSelected(true);
      rSub=cb=new JRadioButton(SUB); cb.setActionCommand(SUB);
      cbg.add(cb); pp.add(cb);
      rDiff=cb=new JRadioButton(DIFF); cb.setActionCommand(DIFF);
      cbg.add(cb); pp.add(cb);
      rComp=cb=new JRadioButton(COMP); cb.setActionCommand(COMP);
      cbg.add(cb); pp.add(cb);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);
      
      PropPanel.addSectionTitle(p, new JLabel("Target resolution"), g, c);
      c.gridwidth=GridBagConstraints.REMAINDER;
      pp=new JPanel();
      pp.add( new JLabel("Space "));
      mocSpaceOrder = getComboSpaceRes();
      pp.add(mocSpaceOrder);
      pp.add( new JLabel("    Time "));
      mocTimeOrder = getComboTimeRes();
      pp.add(mocTimeOrder);
      g.setConstraints(pp,c);
      p.add(pp);
      
      return p;
   }


   private int getOperation(String s) {
      if( s.equals(SUNION) ) return PlanMocAlgo.UNION;
      if( s.equals(INTER) )  return PlanMocAlgo.INTERSECTION;
      if( s.equals(SUB) )    return PlanMocAlgo.SUBTRACTION;
      if( s.equals(DIFF) )   return PlanMocAlgo.DIFFERENCE;
      return PlanMocAlgo.COMPLEMENT;
   }

   protected PlanMoc [] getPlans() {
      ArrayList<PlanMoc> pListA = new ArrayList<>();
      for( JComboBox c : ch ) {
         int i=c.getSelectedIndex()-1;
         if (i<0) continue;
         pListA.add((PlanMoc)choicePlan[i]);
      }

      PlanMoc [] pList = new PlanMoc[pListA.size()];
      pListA.toArray(pList);
      return pList;
   }

   protected int getTimeOrder() {
      return mocTimeOrder.isEnabled() ? 
            mocTimeOrder.getSelectedIndex()+FrameMocGenImg.FIRSTORDER : -1;
   }

   protected int getSpaceOrder() { 
      return mocSpaceOrder.isEnabled() ? 
            mocSpaceOrder.getSelectedIndex()+FrameMocGenImg.FIRSTORDER : -1; 
   }

   @Override
   protected void submit() {
      try {
         PlanMoc [] pList = getPlans();

         String s=cbg.getSelection().getActionCommand();
         int fct=getOperation(s);
         String label = s.substring(0,3)+" "+pList[0].label+(pList.length==1?""
               :pList[1].label+(pList.length==2?"":"..."));
         
         int spaceOrder = getSpaceOrder();
         int timeOrder = getTimeOrder();

         Plan [] ps = new Plan[ pList.length ];
         for( int i=0; i<ps.length; i++ ) ps[i] = pList[i];
         a.console.printCommand("cmoc -"+PlanMocAlgo.getOpName(fct)+" "+FrameMocGenImg.labelList(ps));
         a.calque.newPlanMoc(label,pList,fct,spaceOrder,timeOrder);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC operation failed !");
      }

   }
   
   private int initSpaceOrder=-1;
   private int initTimeOrder=-1;
   
   @Override
   protected void adjustWidgets() { 
      PlanMoc [] pList = getPlans();
      
      boolean un = pList.length==1;
      boolean deux = pList.length==2;
      boolean plus = pList.length>1;
      boolean time=false;
      boolean space=false;

      boolean setTimeOrder=false;
      boolean setSpaceOrder=false;

      for( PlanMoc p : pList ) {
         Moc moc = p.getMoc();
         
         if( moc.isTime() ) {
            time=true;
            if( initTimeOrder==-1 ) {
               initTimeOrder = p.moc.getTimeOrder();
               setTimeOrder=true;
            }
         }
         
         if( moc.isSpace() ) {
            space=true;
            initSpaceOrder = p.moc.getSpaceOrder();
            setSpaceOrder=true;
         }
         
//         if( p.isTimeMoc() ) {
//            time=true;
//            if( initTimeOrder==-1 ) {
//               initTimeOrder = p.moc.getTimeOrder();
//               setTimeOrder=true;
//            }
//         } else {
//            space=true;
//            initSpaceOrder = p.moc.getSpaceOrder();
//            setSpaceOrder=true;
//         }
      }

      if( !time ) initTimeOrder=-1;
      if( !space ) initSpaceOrder=-1;

      rUnion.setEnabled(plus);
      rInter.setEnabled(plus);
      rDiff.setEnabled(deux);
      rSub.setEnabled(deux);
      rComp.setEnabled(un);
      
      if( setSpaceOrder ) mocSpaceOrder.setSelectedIndex( initSpaceOrder-FrameMocGenImg.FIRSTORDER);
      if( setTimeOrder ) mocTimeOrder.setSelectedIndex( initTimeOrder-FrameMocGenImg.FIRSTORDER);
      
      mocSpaceOrder.setEnabled( space );
      mocTimeOrder.setEnabled( time );
   };
}
