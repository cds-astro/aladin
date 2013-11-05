// Copyright 2012 - UDS/CNRS
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
package cds.allsky;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.prop.PropPanel;
import cds.allsky.Context.JpegMethod;
import cds.tools.Util;

public class TabRgb extends JPanel implements ActionListener {

   private static String B;
   private static String G;
   private static String R;

   private String REP_DEST;
   private String RGBALLSKY;
   private String BROWSE;
   private String HELP;
   private String CHOOSE="-- select a plane --";

   private JLabel		dirLabel = new JLabel();
   private JButton 	    browse = new JButton();
   private JTextField 	outputField = new JTextField(30);

   private JRadioButton radioMediane;               // selected si on est en calcul selon la médiane
   private JRadioButton formatJpeg;                 // selected si JPG sinon PNG

   JProgressBar progressBar = new JProgressBar(0,100);

   private JComboBox[] ch;
   private JButton start,abort,pause;
   private JButton help = new JButton();
   private final Aladin aladin;
   private MainPanel mainPanel;
   private ContextGui context;
   private String titlehelp;

   public TabRgb(Aladin aladin,MainPanel mainPanel) {
      super(new BorderLayout());
      this.aladin = aladin;
      this.mainPanel = mainPanel;
      context = mainPanel.context;
      createChaine(Aladin.getChaine());
      help = Util.getHelpButton(this,HELP);

      GridBagConstraints c = new GridBagConstraints();
      c.fill=GridBagConstraints.BOTH;
      c.insets = new Insets(2,2,2,2);

      JPanel pCenter = new JPanel();
      pCenter.setLayout(new GridBagLayout());
      pCenter.setBorder(BorderFactory.createEmptyBorder(5, 55, 5,55));

      JLabel info = new JLabel(Util.fold(RGBALLSKY,80,true));
      info.setFont(info.getFont().deriveFont(Font.ITALIC));
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.insets.bottom=20;
      pCenter.add(info,c);

      // Création des lignes pour choisir les plans
      c.insets.bottom=2;
      int n=3;
      ch=new JComboBox[n];
      for (int i=0; i<n; i++) {
         ch[i]=new JComboBox();
         ch[i].addActionListener(this);
         ch[i].setPreferredSize(new Dimension(200,20));

         JLabel ll=new JLabel(getLabelSelector(i));
         ll.setForeground(getColorLabel(i));

         c.gridwidth=GridBagConstraints.RELATIVE;
         c.weightx=0.0;
         pCenter.add(ll,c);
         c.gridwidth=GridBagConstraints.REMAINDER;
         //			c.weightx=10.0;
         pCenter.add(ch[i],c);

         ch[i].addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
               init();
            }
         });
      }

      init();
      c.gridwidth=GridBagConstraints.RELATIVE;
      c.weightx=0;

      // Sélection du répertoire destination
      JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      dirLabel = new JLabel(REP_DEST);
      pCenter.add(dirLabel,c);
      browse.setText(BROWSE);
      browse.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { dirBrowser(outputField); }
      });
      dirPanel.add(outputField);
      dirPanel.add(browse);
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(dirPanel,c);

      //		c.gridx=GridBagConstraints.RELATIVE; c.gridy=GridBagConstraints.RELATIVE;
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.fill=GridBagConstraints.HORIZONTAL;

      int m=c.insets.top;
      c.insets.top=20;
      JPanel p = new JPanel();
      JLabel l;
      JRadioButton rb;
      l = new JLabel(getString("METHODJPG"));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      p.add(l);
      ButtonGroup bg1 = new ButtonGroup();
      radioMediane = rb = new JRadioButton(getString("MEDIANJPG"));
      rb.setSelected(true);
      bg1.add(rb);
      p.add(rb);
      rb = new JRadioButton(getString("AVERAGEJPG"));
      bg1.add(rb);
      p.add(rb);
      pCenter.add(p,c);
      c.insets.top=m;
      
      m=c.insets.top;
      c.insets.top=0;
      p = new JPanel();
      l = new JLabel("Output tile format:");
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      bg1 = new ButtonGroup();
      p.add(l);
      formatJpeg = rb = new JRadioButton("JPEG (faster)");
      rb.setSelected(true);
      bg1.add(rb);
      p.add(rb);
      rb = new JRadioButton("PNG (better)");
      bg1.add(rb);
      p.add(rb);
      pCenter.add(p,c);
      c.insets.top=m;

      // barre de progression
      progressBar.setStringPainted(true);
      //		c.insets.top=70;
      //		c.fill = GridBagConstraints.HORIZONTAL;
      //		c.gridwidth = GridBagConstraints.REMAINDER;
      //		c.gridy++;c.gridx=0;
      JPanel pProgress = new JPanel(new BorderLayout());
      pProgress.setBorder(new EmptyBorder(50, 0, 15, 0));
      pProgress.add(progressBar,BorderLayout.CENTER);
      pProgress.add(createStatPanel(),BorderLayout.SOUTH);
      pCenter.add(pProgress,c);

      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      start = new JButton(getString("START"));
      start.addActionListener(this);
      pBtn.add(start);
      pause = new JButton(getString("PAUSE"));
      pause.addActionListener(this);
      pBtn.add(pause);
      abort = new JButton(getString("ABORT"));
      abort.addActionListener(this);
      pBtn.add(abort);
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pBtn, BorderLayout.CENTER);
      fin.add(help, BorderLayout.EAST);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }
   protected String getLabelSelector(int i) {
      return i == 0?R:i == 1?G:i==2?B:"";
   }

   protected Color getColorLabel(int i) {
      return i == 0?Color.red:i == 1?Color.green:i==2?Color.blue:Color.black;
   }

   private void createChaine(Chaine chaine) {
      BROWSE = getString("FILEBROWSE");
      REP_DEST = getString("REPDALLSKY");
      RGBALLSKY = getString("RGBALLSKY");
      HELP = getString("HELPRGBALLSKY");
      titlehelp = getString("HHELP");
      R = getString("RGBRED");
      G = getString("RGBGREEN");
      B = getString("RGBBLUE");
   }

   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   /**   retourne la méthode qu'il faudra utiliser pour construire les JPG couleur */
   public JpegMethod getMethod() {
      if( radioMediane.isSelected() ) return Context.JpegMethod.MEDIAN;
      return Context.JpegMethod.MEAN;
   }

   /**   retourne le format de codage pour les tuiles couleurs (JPEG ou PNG) */
   public int getFormat() {
      if( formatJpeg.isSelected() ) return Context.JPEG;
      return Context.PNG;
   }

   /** Recupere la liste des plans Allsky valides */
   protected PlanBG[] getPlan() {
      try {
         Vector<Plan> v = aladin.calque.getPlanBG();
         if( v==null ) return new PlanBG[0];
         // enlève les plans déjà couleur
         for (Iterator<Plan> iterator = v.iterator(); iterator.hasNext();) {
            PlanBG plan = (PlanBG) iterator.next();

            if (plan.isColored())
               v.remove(plan);
         }
         PlanBG pi [] = new PlanBG[v.size()];
         v.copyInto(pi);
         return pi;
      } catch( Exception e ) {
         return new PlanBG[]{};
      }
   }

   private JLabel tileStat,timeStat;

   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      tileStat = new JLabel("--");
      PropPanel.addCouple(p, ".RGB tiles: ", tileStat, g, c);           

      timeStat = new JLabel("--");
      PropPanel.addCouple(p, ".Time: ", timeStat, g, c);           

      return p;
   }

   protected void setStat(int nbTile,long sizeTile,long time) {
      String s;
      if( nbTile==-1 ) s="";
      else s= nbTile+" tile"+(nbTile>1?"s":"") + " for "+Util.getUnitDisk(sizeTile);
      tileStat.setText(s);
      if( time==-1 ) s="";
      else s= Util.getTemps(time,true);
      timeStat.setText(s);
   }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == start ) {
         // récupère les 2 ou 3 plans sélectionnés
         Object[] plans = new Object[3];
         for (int i=0; i<3; i++) {
            plans[i] = ch[i].getSelectedItem();
         }
         // verifie si le champ du répertoire de sorie n'est pas vide ou invalide
         if ("".equals(outputField.getText()) || !(new File(outputField.getText())).isDirectory() ) {
            Aladin.warning("Choose an output directory");
            return;
         }

         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         context.setRgbPlans(plans);
         context.setRgbOutput(outputField.getText());
         context.setRgbMethod(getMethod());
         context.setRgbFormat(getFormat());
         context.setProgressBar(progressBar);
         try {
            new Task(context, Action.RGB, false);
         } catch( Exception e1 ) {
            e1.printStackTrace();
         }
      }
      else if (e.getSource() == abort)      abort();
      else if (e.getSource() == pause)      pause();
      resumeWidgets();
   }
   
   private void pause() {
      if( context.isTaskPause() ) {
         context.setTaskPause(false);
         pause.setText(getString("PAUSE"));
      } else {
         context.setTaskPause(true);
         pause.setText(getString("RESUME"));
      }
   }
   
   private void abort() {
      if( !Aladin.confirmation(mainPanel, "Do you really want to abort the RGB computation ?") ) return;
      context.taskAbort();
   }
   


   /** Ouverture de la fenêtre de sélection d'un fichier */
   private void dirBrowser(JTextField dir) {
      String path = dir.getText().trim();
      if( path.length()==0 ) path = aladin.getDefaultDirectory();
      String t = Util.dirBrowser(this,path);
      if( t==null ) return;
      dir.setText(t);
      actionPerformed(new ActionEvent(dir,-1, "dirBrowser Action"));
   }

   protected void init() {
      // sauvegarde les anciennes selections
      Object[] save = new Object[]{ch[0].getSelectedItem(),
            ch[1].getSelectedItem(),
            ch[2].getSelectedItem()};
      
      // rachaichit les combo box avec la liste des plans allsky
      PlanBG[] plans = getPlan();
      for (int i=0; i<3; i++) {
         ch[i].removeAllItems();
         ch[i].addItem(CHOOSE);
      }
      for (PlanBG planBG : plans) {
         for (int i=0; i<3; i++) {
            ch[i].addItem(planBG);
            // remet l'ancienne selection
            if (save[i]!=null && planBG==save[i]) ch[i].setSelectedItem(planBG);

         }
      }
   }
   
   public void show() {
      super.show();
      init();
   }

   public void help() {
      JOptionPane.showMessageDialog(this, HELP, titlehelp,
            JOptionPane.INFORMATION_MESSAGE);
   }
   
   private boolean hasPlanSelection() {
      int nb=0;
      for( int i=0; i<ch.length; i++ ) {
         Object o = ch[i].getSelectedItem();
         if( o instanceof PlanBG ) nb++;
      }
      return nb>1;
   }
   
   protected void resumeWidgets() {
      try {
         boolean readyToDo = hasPlanSelection() && outputField.getText().trim().length()>0;
         boolean isRunning = context.isTaskRunning();
         start.setEnabled(readyToDo && !isRunning);
         pause.setEnabled(isRunning);
         abort.setEnabled(isRunning);
         setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
               : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
      } catch( Exception e ) { } 
   }


}