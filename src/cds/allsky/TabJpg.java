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

package cds.allsky;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import cds.aladin.Aladin;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.aladin.prop.PropPanel;
import cds.allsky.Context.JpegMethod;
import cds.tools.Util;

public class TabJpg extends JPanel implements ActionListener {


   private String CUT_MAX = "Max";
   private String CUT_MIN = "Min";
   private JTextField tCutMin = new JTextField(10);
   private JTextField tCutMax = new JTextField(10);
   private JLabel label;                                  // texte d'explications
   private JRadioButton radioManual;                      // selected si on est en mode manuel
   private JRadioButton radioAllsky;                      // selected si on est en mode allsky
   private JLabel labelMethod;                            // Texte décrivant la méthode à utiliser
   private JRadioButton radioMediane;                     // selected si on est en calcul selon la médiane
   private JRadioButton radioMoyenne;                     // selected si on est en calcul selon la moyenne
   private JLabel labelFormat;                            // Texte décrivant le format à utiliser
   private JRadioButton jpegFormat;                       // JPEG tiles
   private JRadioButton pngFormat;                        // PNG tiles
   private JLabel currentCM;                              // info détaillant le cut de la vue courante
   private JLabel warning;                                // indique s'il est nécessaire ou non d'effectuer ce post-traitement

   private JButton start,abort,pause;
   private JButton next,previous;
   private JButton moc;
   private JProgressBar progressJpg = new JProgressBar(0,100);

   private final MainPanel mainPanel;
   private Context context;

   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   public TabJpg(final MainPanel mainPanel) {
      super(new BorderLayout());
      this.mainPanel = mainPanel;
      context = mainPanel.context;

      JRadioButton rb;
      ButtonGroup bg = new ButtonGroup();

      JLabel lab;
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = GridBagConstraints.REMAINDER;

      JPanel pCenter = new JPanel();
      pCenter.setLayout(new GridBagLayout());
      pCenter.setBorder(BorderFactory.createEmptyBorder(5, 25, 5,25));
      
      // Warning
      warning = new JLabel(" ");
      warning.setFont(warning.getFont().deriveFont(Font.BOLD));
      warning.setForeground( Color.red);
      c.gridheight = 2;
      c.insets.bottom=20;
      pCenter.add(warning,c);
      c.insets.bottom=5;
      c.gridy++;c.gridy++;
      

      // Texte d'intro
      label = lab = new JLabel(Util.fold(getString("JPEGINFOALLSKY"),80,true));
      lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
      c.gridheight = 5;
      pCenter.add(lab,c);
      c.insets.bottom=0;
      c.gridy++;c.gridy++;c.gridy++;c.gridy++;c.gridy++;
      c.gridheight = 1;

      // détermine le mode par défaut (automatique si vue courante est un all-sky en fits, sinon manuel)
      currentCM = new JLabel();
      boolean manualSelected = !updateCurrentCM();

      c.gridx = 0;
      c.gridy++;
      radioManual = rb = new JRadioButton(getString("JPEGMCUTALLSKY"));
      rb.setSelected(manualSelected);
      tCutMin.setEnabled(manualSelected);
      tCutMax.setEnabled(manualSelected);
      rb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean enable = radioManual.isSelected();
            tCutMin.setEnabled(enable);
            tCutMax.setEnabled(enable);
         }
      });
      rb.setFont( rb.getFont().deriveFont(Font.BOLD));
      bg.add(rb);
      pCenter.add(rb,c);
      c.gridx++;
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(Util.getHelpButton(this,getString("HELPJPEGMCUTALLSKY")),c);

      c.gridwidth=2;
      c.gridx = 0;
      c.gridy++;
      JPanel minmax = new JPanel(new FlowLayout());
      lab = new JLabel(CUT_MIN);
      minmax.add(lab);
      minmax.add(tCutMin);
      lab = new JLabel(CUT_MAX);
      minmax.add(lab);
      minmax.add(tCutMax);
      pCenter.add(minmax, c);

      c.gridwidth=1;
      c.gridx = 0;
      c.gridy++;
      int m=c.insets.top;
      c.insets.top=20;
      radioAllsky = rb = new JRadioButton(getString("JPEGCUTALLSKY"));
      rb.setSelected(!manualSelected);
      rb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean enable = radioManual.isSelected();
            tCutMin.setEnabled(enable);
            tCutMax.setEnabled(enable);
         }
      });
      rb.setFont( rb.getFont().deriveFont(Font.BOLD));
      rb.setSelected(true);
      bg.add(rb);
      pCenter.add(rb,c);
      
      c.gridx++;
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(Util.getHelpButton(this,getString("HELPJPEGCUTALLSKY")),c);
      c.insets.top=m;
      
      c.gridx = 0;
      c.gridy++;
      pCenter.add(currentCM,c);
      
      c.gridx=0;
      c.gridy++;
      m=c.insets.top;
      c.insets.top=20;
      JPanel p = new JPanel();
      JLabel l;
      labelFormat = l = new JLabel(getString("FORMATTILES"));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      p.add(l);
      ButtonGroup bg1 = new ButtonGroup();
      jpegFormat = rb = new JRadioButton("JPEG");
      bg1.add(rb);
      p.add(rb);
      pngFormat = rb = new JRadioButton("PNG");
      rb.setSelected(true);
      bg1.add(rb);
      p.add(rb);
      pCenter.add(p,c);
      c.insets.top=m;


      c.gridx=0;
      c.gridy++;
//      m=c.insets.top;
//      c.insets.top=20;
      p = new JPanel();
      labelMethod = l = new JLabel(getString("METHODJPG"));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      p.add(l);
      bg1 = new ButtonGroup();
      radioMediane = rb = new JRadioButton(getString("MEDIANJPG"));
      rb.setSelected(true);
      bg1.add(rb);
      p.add(rb);
      radioMoyenne = rb = new JRadioButton(getString("AVERAGEJPG"));
      bg1.add(rb);
      p.add(rb);
      pCenter.add(p,c);
//      c.insets.top=m;

      // barre de progression
      progressJpg.setStringPainted(true);
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridy++;c.gridx=0;
      JPanel pProgress = new JPanel(new BorderLayout());
      pProgress.setBorder(new EmptyBorder(0, 55, 15, 55));
      pProgress.add(progressJpg,BorderLayout.CENTER);
      pProgress.add(createStatPanel(),BorderLayout.SOUTH);
      
      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      previous = new JButton(getString("PREVIOUS"));
      previous.addActionListener(this);
      previous.addActionListener(this);
      pBtn.add(previous);
      
      moc = new JButton(getString("LOADMOCP"));
      moc.addActionListener(this);
      pBtn.add(moc);

      start= new JButton(getString("JPEGBUILDALLSKY"));
      start.addActionListener(this);
      pBtn.add(start);
      
      pause=new JButton(getString("PAUSE"));
      pause.addActionListener(this);
      pBtn.add(pause);
      
      abort=new JButton(getString("ABORT"));
      abort.addActionListener(this);
      pBtn.add(abort);
      
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      next = new JButton(getString("NEXT"));
      next.addActionListener(this);
      next.addActionListener(this);
      pBtn.add(next);
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pProgress, BorderLayout.NORTH);
      fin.add(pBtn, BorderLayout.CENTER);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }
   
   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == start ) {

         // Juste pour vérifier qu'on a bien un plan all-sky valable en cours de visualisation
         if( !radioManual.isSelected() ) {
            try {
               PlanBG p = (PlanBG) mainPanel.aladin.calque.getPlanBase();
               if( !p.isTruePixels() ) throw new Exception();
            } catch( Exception e1 ) {
               mainPanel.aladin.warning(mainPanel,"<html>There is no current view,<br>or the current view is not an all-sky view in true pixel mode");
               return;
            }
         }
         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         context.setJpegMethod(getMethod());
         context.setProgressBar(progressJpg);
         Action action = getTileFormat()==Constante.TILE_PNG ? Action.PNG : Action.JPEG;
         context.setValidateCut(false);
         
         try {
            new Task(context, action, false);
         } catch( Exception e1 ) {
            e1.printStackTrace();
         }
      }
      else if (e.getSource() == abort)      abort();
      else if (e.getSource() == pause)      pause();

      else if (e.getSource() == next)      mainPanel.showPubTab();
      else if (e.getSource() == previous)  mainPanel.showBuildTab();
      else if (e.getSource() == moc)       loadMoc();
      
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
      if( !Aladin.confirmation(mainPanel, "Do you really want to abort the compressed tile computation ?") ) return;
      context.taskAbort();
   }
   
   private void loadMoc() {
      String mocFile = context.getOutputPath()+Util.FS+Constante.FILE_MOC;
      mainPanel.aladin.execAsyncCommand("load "+mocFile);
   }

   private JLabel tileStat,timeStat;
   
   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      tileStat = new JLabel("--");
      PropPanel.addCouple(p, ".Tiles: ", tileStat, g, c);           

      timeStat = new JLabel("--");
      PropPanel.addCouple(p, ".Time: ", timeStat, g, c);           

      return p;
   }
   
   protected void setStat(String sTile,String sTime) {
      tileStat.setText(sTile);
      timeStat.setText(sTime);
   }
   
   public void setCutMin(String s) { tCutMin.setText(s); }
   public void setCutMax(String s) { tCutMax.setText(s); }

   public String getCutMin() { return tCutMin.getText().trim(); }
   public String getCutMax() { return tCutMax.getText().trim(); }
   
   private boolean isExistingMoc() {
      String moc = context.getOutputPath()+Util.FS+Constante.FILE_MOC;
      return  moc!=null && (new File(moc)).exists();
   }
   
   public boolean isCutFromPlanBase() { return !radioManual.isSelected(); }

   /** Retourne la table des couleurs de la vue courante, ou null si le mode de cut est positionné manuellement */
   public ColorModel getCM() {
      if( radioManual.isSelected() ) return null;
      return ((PlanImage) mainPanel.aladin.calque.getPlanBase() ).getCM();
   }
   
   /**   retourne la méthode qu'il faudra utiliser pour construire les JPG */
   public JpegMethod getMethod() {
      if( radioMediane.isSelected() ) return Context.JpegMethod.MEDIAN;
      return Context.JpegMethod.MEAN;
   }
   
   /**   retourne le format pour les tuiles compressées (JPEG ou PNG) */
   public int getTileFormat() {
      if( pngFormat.isSelected() ) return Constante.TILE_PNG;
      return Constante.TILE_JPEG;
   }

   protected void resumeWidgets() {
      try {
         boolean readyToDo = context.isExistingDir() || context.isExistingAllskyDir();
         boolean isRunning = context.isTaskRunning();
         boolean isColor = context.isColor();
         moc.setEnabled(isExistingMoc());
         previous.setEnabled(readyToDo && !isRunning);
         next.setEnabled(readyToDo && !isRunning);
         tCutMin.setEnabled(readyToDo && !isColor);
         tCutMax.setEnabled(readyToDo && !isColor);
         radioManual.setEnabled(readyToDo && !isColor);
         labelMethod.setEnabled(readyToDo && !isColor);
         radioAllsky.setEnabled(readyToDo && !isColor);
         radioMediane.setEnabled(readyToDo && !isRunning && !isColor);
         radioMoyenne.setEnabled(readyToDo && !isRunning && !isColor);
         labelFormat.setEnabled(readyToDo && !isColor);
         jpegFormat.setEnabled(readyToDo && !isColor);
         pngFormat.setEnabled(readyToDo && !isColor);
         progressJpg.setEnabled(readyToDo && !isRunning && !isColor);
         start.setEnabled(readyToDo && !isRunning && !isColor);
         pause.setEnabled(isRunning);
         abort.setEnabled(isRunning);
         setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
               : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
         warning.setText( isColor ? Util.fold(getString("MNOPOST"),60,true) : "" );
         label.setEnabled( !isColor);

      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      } 
   }

   public void clearForms() {
      tCutMin.setText("");
      tCutMax.setText("");
      radioManual.setSelected(true);
      radioMediane.setSelected(true);
      progressJpg.setValue(0);
      jpegFormat.setSelected(true);
   }

   public void setStartEnabled(boolean enabled) {
      start.setEnabled(enabled);
      next.setEnabled(enabled);
   }
   
   public void show() {
      updateCurrentCM();
      resumeWidgets();
      super.show(); 
   }

   public boolean updateCurrentCM() {
      boolean rep=true;
      String s;
      try {
         PlanImage p = (PlanImage) mainPanel.aladin.calque.getPlanBase();
//         if( !p.isTruePixels() ) throw new Exception();
         if( !p.hasAvailablePixels() ) throw new Exception();
         s="<html><i>"+"Pixels:<b> "+p.getPixelMinInfo()+" .. "+p.getPixelMaxInfo()+"</b> from "+p.getDataMinInfo()+" .. "+p.getDataMaxInfo()+" - " +
         "Transfert function: <b>"+p.getTransfertFctInfo()+"</b></i>";
      } catch( Exception e1 ) {
         s="<html><i>No compatible image/survey presently displayed !</i>";
         rep=false;
      }
      currentCM.setText(s);
      return rep;
   }
}
