// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLayeredPane;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Gestion de la fenetre associee à l'application d'une convolution
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (oct 2010) Creation
 */
public final class FrameConvolution extends FrameRGBBlink {

   String TITLE,INFO,HELP1,FWHM,SIGMA,RADIUS,PIXRES,PLANE,KERNEL,GAUSSIAN;

   // Les composantes de l'objet

   @Override
protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("CONVTITLE");
      INFO  = a.chaine.getString("CONVINFO");
      HELP1  = a.chaine.getString("CONVHELP");
      PIXRES  = a.chaine.getString("CONVPIXRES");
      FWHM   = "FWHM";
      SIGMA = "or   Sigma";
      RADIUS  = a.chaine.getString("CONVRADIUS");
      KERNEL    = a.chaine.getString("CONVKERNEL");
      GAUSSIAN    = a.chaine.getString("CONVGAUSSIAN");
      PLANE    = a.chaine.getString("ARITHPLANE");
   }

  /** Creation du Frame gerant la creation d'un plan RGB. */
   protected FrameConvolution(Aladin aladin) {
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
   protected String getLabelSelector(int i) { return PLANE; }

   @Override
   protected int getToolNumber() { return -2; }
   
   @Override
   protected int getNb() { return 1; }
   
   protected JButton [] getAddButtons() { 
      JButton [] tb = new JButton[1];
      tb[0] = show = new JButton("Show");
      show.setEnabled(false);
      tb[0].addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { showKernel( (PlanImage)getPlan(ch[0]) ); }
      });
      return tb;
   }


   /** Recupere la liste des plans images valides */
   @Override
   protected PlanImage[] getPlan() {
      Vector<Plan> v  =a.calque.getPlans(Plan.IMAGE);
      if( v==null ) return new PlanImage[0];
      PlanImage pi [] = new PlanImage[v.size()];
      v.copyInto(pi);
      return pi;
   }

   @Override
   protected Color getColorLabel(int i) {
      return Color.black;
   }
   
   JTextField pixres,fwhm,sigma,radius;
   JRadioButton gaussian,kernels;
   JComboBox comboKernel;
   JButton show;
   
   @Override
   protected JPanel getAddPanel() {
      final ButtonGroup bg =new ButtonGroup();

      
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.NONE;
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=10.0;

      JPanel p=new JPanel();
      p.setLayout(g);
      
      PropPanel.addFilet(p, g, c, 15,0);
      
      c.anchor=GridBagConstraints.EAST;
      JRadioButton r = gaussian = new JRadioButton(GAUSSIAN,true);
      r.setFont(r.getFont().deriveFont(Font.BOLD));
      g.setConstraints(r, c);
      p.add(r); bg.add(r);
      
      c.anchor=GridBagConstraints.CENTER;
      JPanel pp = new JPanel( new GridLayout(2,4,5,5));
      pp.add( new JLabel(FWHM,JLabel.RIGHT) );   pp.add( fwhm=new JTextField(5) );
      pp.add( new JLabel(SIGMA,JLabel.RIGHT) );  pp.add( sigma=new JTextField(5) );
      pp.add( new JLabel(PIXRES,JLabel.RIGHT) ); pp.add( pixres=new JTextField(5) ); pixres.setEditable(false);
      pp.add( new JLabel(RADIUS,JLabel.RIGHT) ); pp.add( radius=new JTextField(5) );
      fwhm.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            gaussian.setSelected(true); 
            lockRadius=false; 
            radius.setText(""); 
            sigma.setText(""); 
            show.setEnabled(true);
            adjustWidgets(); 
         }
      });
      fwhm.addKeyListener( new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) {
            radius.setText("");
            sigma.setText("");
         }
      });
      sigma.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            gaussian.setSelected(true); 
            lockRadius=false; 
            radius.setText(""); 
            show.setEnabled(true);
            fwhm.setText(""); 
            adjustWidgets(); 
         }
      });
      sigma.addKeyListener( new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { 
            radius.setText("");
            fwhm.setText("");
         }
      });
      radius.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            gaussian.setSelected(true); 
            lockRadius=true; 
            adjustWidgets(); 
         }
      });
      radius.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { lockRadius=true; }
      });
      g.setConstraints(pp,c);
      p.add(pp);
      
      PropPanel.addFilet(p, g, c, 10, 0);
      
      c.anchor=GridBagConstraints.EAST;
      r = kernels = new JRadioButton(KERNEL);
      r.setFont(r.getFont().deriveFont(Font.BOLD));
      g.setConstraints(r, c);
      p.add(r); bg.add(r);

      c.anchor=GridBagConstraints.CENTER;
      c.fill=GridBagConstraints.NONE;
      comboKernel = new JComboBox(a.kernelList.getKernelListAsVector());
      comboKernel.setMinimumSize( new Dimension(200,comboKernel.getPreferredSize().height));
      comboKernel.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            kernels.setSelected(true);
            show.setEnabled(true);
         }
      });
      g.setConstraints(comboKernel,c);
      p.add(comboKernel);
      
      PropPanel.addFilet(p, g, c, 15, 0);
      
     return p;
   }
   
   /** Rechargement de la liste des kernels disponibles, avec sélection d'un kernel particulier
    * ou conservation de la précédente sélection si null */
   public void reloadComboKernel(String item) {
      boolean g = gaussian.isSelected();
      String c = item==null ? (String) comboKernel.getSelectedItem() : item;
      comboKernel.removeAllItems();
      Enumeration<String> e = a.kernelList.getKernelListAsVector().elements();
      while( e.hasMoreElements() ) comboKernel.addItem(e.nextElement());
      if( c!=null ) comboKernel.setSelectedItem(c);
      gaussian.setSelected(g);
   }
   
   private void showKernel(PlanImage p) {
      try {
         String conv = getConvCmd();
         Kernel k = a.kernelList.getKernel(conv, res);
         AladinData ad = a.createAladinImage(k.name);
         String name = ad.getLabel();
         ad.setPixels(k.matrix, -64);
         if( !p.hasNoReduction() ) {
            double cd = p.projd.getPixResAlpha();
            Coord c = new Coord(a.view.repere.raj,a.view.repere.dej);
            int n = k.matrix.length;
            String s = 
               "SIMPLE  = T\n"+
               "BITPIX  = -64\n"+
               "NAXIS   = 2\n"+
               "NAXIS1  = "+n+"\n"+
               "NAXIS2  = "+n+"\n"+
               "CRPIX1  = "+(n/2+1)+"\n"+
               "CRPIX2  = "+(n/2+1)+"\n"+
               "CRVAL1  = "+c.al+"\n"+
               "CRVAL2  = "+c.del+"\n"+
               "CTYPE1  = RA---TAN\n"+
               "CTYPE2  = DEC--TAN\n"+
               "RADECSYS= FK5\n"+
               "CD1_1   = "+(-cd)+"\n"+
               "CD1_2   = 0\n"+
               "CD2_1   = 0\n"+
               "CD2_2   = "+cd+"\n";
            ad.setFitsHeader( s );
            a.command.execLater("set "+name+" opacity=75");
            a.command.execLater("set "+name+" shown");
            a.command.execLater("cm "+name+" asinh reverse autocut");
            
         } else a.command.execLater("show "+name);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   private String getConvCmd() throws Exception {
      String conv=null;
      String s;
      if( gaussian.isSelected() ) {
         StringBuffer param= new StringBuffer();
         s=fwhm.getText().trim();
         if( s.length()>0 ) {
            if( param.length()>0 ) param.append(',');
            param.append("fwhm="+s);
         } else {
            s=sigma.getText().trim();
            if( s.length()>0 ) {
               if( param.length()>0 ) param.append(',');
               param.append("sigma="+s);
            }
         }
         s=radius.getText().trim();
         if( s.length()>0 ) {
            if( param.length()>0 ) param.append(',');
            param.append("radius="+s);
         }

         conv = "gauss("+param+")";
         double [] p = KernelList.parseGaussCmd(conv);
         p = KernelList.computeGaussParam(p[0], p[1], res, 0);
         fwhm.setText( Coord.getUnit(p[0]) );
         sigma.setText(Coord.getUnit(p[1]) );
         if( !lockRadius ) radius.setText(""+(int)p[2]);
      } else {
         conv = (String)comboKernel.getSelectedItem();
      }
      return conv;
   }

   @Override
protected void submit() {
      try {
         PlanImage p1=(PlanImage)getPlan(ch[0]);
         String conv = getConvCmd(); 
         String label = conv.replace('=',':');
         a.console.printCommand(label+"=conv "+Tok.quote(p1.label)+" "+Tok.quote(conv));
         a.calque.newPlanImageAlgo(label,p1,null,PlanImageAlgo.CONV,0,conv,0);
//         hide();
      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning("Convolution failed !");
      }
    }
   
   /** Reset de la fenetre */
   protected void reset() {
      super.reset();
      fwhm.setText("");
      sigma.setText("");
      adjustWidgets();
      comboKernel.setSelectedIndex(0);
      gaussian.setSelected(true);
      show.setEnabled(false);
   }

   
   private boolean lockRadius=false;
   private double res=1/3600.;

   @Override
   protected void adjustWidgets() {
      
      PlanImage p1=(PlanImage)getPlan(ch[0]);
      String resolution="--";
      if( p1!=null ) {
         try {
            res = p1.projd.getPixResDelta();
            resolution = Coord.getUnit(res);
         } catch( Exception e ) { resolution="--"; }
      }
      
      String s;
      boolean fwhmFlag=false;
      boolean sigmaFlag=false;
      StringBuffer param= new StringBuffer();
      s=fwhm.getText().trim();
      if( s.length()>0 ) {
         if( param.length()>0 ) param.append(',');
         param.append("fwhm="+s);
         sigmaFlag=true;
      } else {
         s=sigma.getText().trim();
         if( s.length()>0 ) {
            if( param.length()>0 ) param.append(',');
            param.append("sigma="+s);
            fwhmFlag=true;
         }
      }
      s=radius.getText().trim();
      if( s.length()>0 ) {
         if( param.length()>0 ) param.append(',');
         param.append("radius="+s);
      } else lockRadius=false;
      
      if( param.length()>0 ) {
         double [] p = KernelList.parseGaussCmd("gauss("+param+")");
         p = KernelList.computeGaussParam(p[0], p[1], res, 0);
         if( !lockRadius ) radius.setText(""+(int)p[2]);
         if( sigmaFlag ) sigma.setText( Coord.getUnit(p[1]) );
         else if( fwhmFlag ) fwhm.setText( Coord.getUnit(p[0]) );
      } else radius.setText("");
      
      pixres.setText(resolution);
   }

}
