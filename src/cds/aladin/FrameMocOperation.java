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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.moc.Moc;
import cds.moc.STMoc;

/**
 * Gestion de la fenetre associee a la creation d'un plan MOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2008) Creation
 */
public class FrameMocOperation extends FrameRGBBlink {

   protected String TITLE,INFO,HELP1,SUNION,INTER,SUB,DIFF,COMP,COPY,PLANE;

   // Les composantes de l'objet
   private ButtonGroup cbgOp;	         // Les checkBox des opérations possibles
   private JRadioButton rUnion,rInter,rSub,rDiff,rComp,rCopy;
   private JRadioButton rFree,rLessThan,rRedSpace,rRedTime,rRedBoth;
   protected JCheckBox mocCheckSpace,mocCheckTime;
   protected JComboBox mocTimeOrder,mocSpaceOrder;
   private JTextField maxMB;
   private JSlider sliderAcc;
   
   static String STIME = "Time";
   static String SSPACE = "Space";
   static String SBOTH = "Both";
   static String IFTOOBIG = "if too big, reduce the resolution in:";
   static String TARGETRES = "Target MOC parameters";
   static String TARGETSIZE = "Target size";
   static String SFREE = "unlimited";
   static String SLESSTHAN = "less than";

   
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
      COPY  = a.chaine.getString("MOCCOPY");
      PLANE    = a.chaine.getString("MOCPLANE");
   }

   /** Creation du Frame gerant la creation d'un plan MOC. */
   protected FrameMocOperation(Aladin aladin ) {
      super(aladin);
      Aladin.setIcon(this);
   }
   
   protected void maj() { maj(-1); }
   protected void maj(int op) {
      super.maj();
      if( op==PlanMocAlgo.UNION )             rUnion.setSelected(true);
      else if( op==PlanMocAlgo.INTERSECTION ) rInter.setSelected(true);
      else if( op==PlanMocAlgo.SUBTRACTION )  rSub.setSelected(true);
      else if( op==PlanMocAlgo.DIFFERENCE )   rDiff.setSelected(true);
      else if( op==PlanMocAlgo.COMPLEMENT )   rComp.setSelected(true);
      else if( op==PlanMocAlgo.COPY )         rCopy.setSelected(true);
      else {
         if( getPlan().length==1 ) rCopy.setSelected(true);
         else rInter.setSelected(true);
      }
      rFree.setSelected(true);
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

      ButtonGroup cbg;
      cbgOp=cbg=new ButtonGroup();

      JPanel pp=new JPanel();
      JRadioButton cb;
      rUnion=cb=new JRadioButton(SUNION); cb.setActionCommand(SUNION);
      cbg.add(cb); pp.add(cb); 
      rInter=cb=new JRadioButton(INTER); cb.setActionCommand(INTER);
      cbg.add(cb); pp.add(cb); 
      rSub=cb=new JRadioButton(SUB); cb.setActionCommand(SUB);
      cbg.add(cb); pp.add(cb); 
      rDiff=cb=new JRadioButton(DIFF); cb.setActionCommand(DIFF);
      cbg.add(cb); pp.add(cb); 
      rComp=cb=new JRadioButton(COMP); cb.setActionCommand(COMP);
      cbg.add(cb); pp.add(cb); 
      rCopy=cb=new JRadioButton(COPY); cb.setActionCommand(COPY);
      cbg.add(cb); pp.add(cb); cb.setSelected(true);

      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;
      g.setConstraints(pp,c);
      p.add(pp);
      
// EN COURS DE DEVELOPPEMENT
//      PropPanel.addSectionTitle(p, new JLabel("Accuracy"), g, c);
//      c.gridwidth=GridBagConstraints.REMAINDER;
//      pp=new JPanel();
//      pp.add( sliderAcc=new JSlider(0, 10) );
//      g.setConstraints(pp,c);
//      p.add(pp);
      
      
      PropPanel.addSectionTitle(p, new JLabel(TARGETRES), g, c);
      c.gridwidth=GridBagConstraints.REMAINDER;
      pp=new JPanel();
      JCheckBox cb1;
      cb1=mocCheckSpace = new JCheckBox(SSPACE+" ");
      cb1.setSelected(true);
      cb1.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { adjustWidgets(); }
      });
      cb1=mocCheckTime = new JCheckBox(STIME+" ");
      cb1.setSelected(true);
      cb1.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { adjustWidgets(); }
      });
      pp.add( mocCheckSpace );
      mocSpaceOrder = getComboSpaceRes();
      pp.add(mocSpaceOrder);
      pp.add( new JLabel("    ") );
      pp.add( mocCheckTime );
      mocTimeOrder = getComboTimeRes();
      pp.add(mocTimeOrder);
      g.setConstraints(pp,c);
      p.add(pp);
            
      
      pp=new JPanel();
      cbg=new ButtonGroup();
      pp.add( new JLabel(TARGETSIZE+" ") );
      rFree=cb=new JRadioButton(SFREE); cb.setActionCommand(SFREE);
      cb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { adjustWidgets(); }
      });
      cbg.add(cb); pp.add(cb); cb.setSelected(true);
      rLessThan=cb=new JRadioButton(SLESSTHAN); cb.setActionCommand(SLESSTHAN);
      cb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { adjustWidgets(); }
      });
      cbg.add(cb); pp.add(cb); 
      maxMB = new JTextField("10",5); pp.add(maxMB);
      pp.add( new JLabel(" MB") );
      g.setConstraints(pp,c);
      p.add(pp); 
      
      pp=new JPanel();
      cbg=new ButtonGroup();
      pp.add( new JLabel(IFTOOBIG) );
      rRedSpace=cb=new JRadioButton(SSPACE); cb.setActionCommand(SSPACE);
      cbg.add(cb); pp.add(cb); 
      rRedTime=cb=new JRadioButton(STIME); cb.setActionCommand(STIME);
      cbg.add(cb); pp.add(cb); 
      rRedBoth=cb=new JRadioButton(SBOTH); cb.setActionCommand(SBOTH);
      cbg.add(cb); pp.add(cb); cb.setSelected(true);
      g.setConstraints(pp,c);
      p.add(pp); 

      return p;
   }
   
   private int getOperation(String s) {
      if( s.equals(SUNION) ) return PlanMocAlgo.UNION;
      if( s.equals(INTER) )  return PlanMocAlgo.INTERSECTION;
      if( s.equals(SUB) )    return PlanMocAlgo.SUBTRACTION;
      if( s.equals(DIFF) )   return PlanMocAlgo.DIFFERENCE;
      if( s.equals(COMP) )   return PlanMocAlgo.COMPLEMENT;
      return PlanMocAlgo.COPY;
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
            mocTimeOrder.getSelectedIndex()+FrameMocGenImg.FIRSTORDER_T : -1;
   }

   protected int getSpaceOrder() { 
      return mocSpaceOrder.isEnabled() ? 
            mocSpaceOrder.getSelectedIndex()+FrameMocGenImg.FIRSTORDER_S : -1; 
   }
   
   protected boolean getTimeMoc() {
      return mocCheckTime.isEnabled() && mocCheckTime.isSelected();
   }
   
   protected boolean getSpaceMoc() {
      return mocCheckSpace.isEnabled() && mocCheckSpace.isSelected();
   }
   
   protected long getSizeMax() throws Exception { 
      if( !maxMB.isEnabled() || rFree.isSelected() ) return -1;
      try {
         long sizeMax = Long.parseLong( maxMB.getText() ) * 1024L * 1024L;
         if( sizeMax<=0 ) throw new Exception();
         maxMB.setForeground( Color.black );
         return sizeMax;
      } catch( Exception e ) {
         maxMB.setForeground( Color.red );
      }
      return -1;
   }
   
   protected String getMaxPriority() {
      if( !maxMB.isEnabled() || rFree.isSelected() ) return null;
      if( rRedTime.isSelected() ) return "t";
      if( rRedSpace.isSelected() ) return "s";
      return null;
   }
   

   @Override
   protected void submit() {
      try {
         PlanMoc [] pList = getPlans();

         String s=cbgOp.getSelection().getActionCommand();
         int op=getOperation(s);
         String label = s.substring(0,3)+" "+pList[0].label+(pList.length==1?""
               :pList[1].label+(pList.length==2?"":"..."));
         
         boolean space = getSpaceMoc();
         boolean time = getTimeMoc();
         int spaceOrder = space ? getSpaceOrder() : -1;
         int timeOrder = time ? getTimeOrder() : -1;
         long sizeMax = getSizeMax();
         String maxPriority = getMaxPriority();
         
         String paramOrd="";
         int oSpace = getMinSpaceOrder(pList);
         int oTime = getMinTimeOrder(pList);
         if( !hasSTMoc(pList) ) {
            if( oSpace!=-1 && oSpace!=spaceOrder ) paramOrd=" -order="+spaceOrder;
            if( oTime!=-1 && oTime!=timeOrder ) paramOrd=" -order="+timeOrder;
         } else {
            if( oTime!=timeOrder || oSpace!=spaceOrder ) paramOrd= " -order="+spaceOrder+"/"+timeOrder;
         }
         
         String paramSize="";
         if( sizeMax!=-1L ) {
            long maxMB = sizeMax/(1024L*1024L);
            paramSize = " -maxSize="+maxMB;
            if( maxPriority!=null ) paramSize=paramSize+"/"+maxPriority;
         }
         
         Plan [] ps = new Plan[ pList.length ];
         for( int i=0; i<ps.length; i++ ) ps[i] = pList[i];
         a.console.printCommand("cmoc -"+PlanMocAlgo.getOpName(op)+paramOrd+paramSize+" "+FrameMocGenImg.labelList(ps));
         a.calque.newPlanMoc(label,pList,op,spaceOrder,timeOrder,sizeMax,maxPriority);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC operation failed !");
      }

   }
   
   private boolean hasSTMoc(PlanMoc [] pList ) {
      for( PlanMoc p : pList ) { if( p.getMoc() instanceof STMoc ) return true; }
      return false;
   }

   
   private int getMinTimeOrder(PlanMoc [] pList ) {
      int min=-1;;
      for( PlanMoc p : pList ) {
         Moc moc = p.getMoc();
         if( !moc.isTime() ) continue;
         int o=p.moc.getTimeOrder();
         if( min==-1 || o<min ) min=o;
      }
      return min;
   }
   
   private int getMinSpaceOrder(PlanMoc [] pList ) {
      int min=-1;;
      for( PlanMoc p : pList ) {
         Moc moc = p.getMoc();
         if( !moc.isSpace() ) continue;
         int o=p.moc.getSpaceOrder();
         if( min==-1 || o<min ) min=o;
      }
      return min;
   }
   
   protected int initSpaceOrder=-1;
   protected int initTimeOrder=-1;
   
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
      
      int oSpace = getMinSpaceOrder(pList);
      mocCheckSpace.setEnabled(oSpace!=-1);
//      if(oSpace==-1) mocCheckSpace.setSelected(false);
      if( oSpace!=-1 && mocCheckSpace.isEnabled() && mocCheckSpace.isSelected() ) {
         space=true;
         if( initSpaceOrder==-1 ) {
            initSpaceOrder=oSpace;
            setSpaceOrder=true;
         }
      }
      int oTime = getMinTimeOrder(pList);
      mocCheckTime.setEnabled(oTime!=-1);
//      if(oTime==-1) mocCheckTime.setSelected(false);
      if( oTime!=-1 && mocCheckTime.isEnabled() && mocCheckTime.isSelected() ) {
         time=true;
         if( initTimeOrder==-1 ) {
            initTimeOrder=oTime;
            setTimeOrder=true;
         }
      }

//      for( PlanMoc p : pList ) {
//         Moc moc = p.getMoc();
//         
//         if( moc.isTime() ) {
//            time=true;
//            if( initTimeOrder==-1 ) {
//               initTimeOrder = p.moc.getTimeOrder();
//               setTimeOrder=true;
//            }
//         }
//         
//         if( moc.isSpace() ) {
//            space=true;
//            initSpaceOrder = p.moc.getSpaceOrder();
//            setSpaceOrder=true;
//         }
//         
//      }
      
      if( !time ) initTimeOrder=-1;
      if( !space ) initSpaceOrder=-1;

      rUnion.setEnabled(plus);
      rInter.setEnabled(plus);
      rDiff.setEnabled(deux);
      rSub.setEnabled(deux);
      rComp.setEnabled(un);
      rCopy.setEnabled(un);
      
      if( setSpaceOrder ) {
         int i = initSpaceOrder-FrameMocGenImg.FIRSTORDER_S;
         if( i<0 ) i=0;
         mocSpaceOrder.setSelectedIndex( i );
      }
      if( setTimeOrder ) {
         int i = initTimeOrder-FrameMocGenImg.FIRSTORDER_T;
         if( i<0 ) i=0;
         mocTimeOrder.setSelectedIndex( i );
      }
      
      mocSpaceOrder.setEnabled( space ); 
      mocTimeOrder.setEnabled( time );
      
      if( rFree!=null ) rFree.setEnabled( space || time );
      if( rLessThan!=null ) rLessThan.setEnabled( space || time );
      boolean reduce = rLessThan.isSelected();
      if( maxMB!=null ) maxMB.setEnabled( reduce && (space || time) );
      if( rRedSpace!=null ) rRedSpace.setEnabled( reduce && space );
      if( rRedTime!=null ) rRedTime.setEnabled( reduce && time );
      if( rRedBoth!=null ) rRedBoth.setEnabled( reduce && space && time );
      
      submitBtn.setEnabled( time || space );
   };
}
