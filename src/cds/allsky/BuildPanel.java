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

package cds.allsky;

import static cds.allsky.AllskyConst.INDEX;
import static cds.allsky.AllskyConst.TESS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.fits.Fits;
import cds.tools.Util;

public class BuildPanel extends JPanel implements ActionListener {
   protected static final String BEST = "best";
   protected static final String FIRST = "first";

   protected JButton b_cancel;
   protected JButton b_pause;
   protected JButton b_ok;
//   protected JButton b_help;
   protected JButton b_next;

   private static JTable tab = null;

   private JLabel 	method_label = new JLabel();
   private ButtonGroup 	groupSampl = new ButtonGroup();
   private ButtonGroup 	groupOverlay = new ButtonGroup();
   private JRadioButton samplFast = new JRadioButton();
   private JRadioButton overlayFast = new JRadioButton();
   private JRadioButton samplBest = new JRadioButton();
   private JRadioButton overlayBest = new JRadioButton();
   private JCheckBox    fading = new JCheckBox();
   private ActionListener 	methodsListener = new MethodSelectListener(
         new JRadioButton[] {samplFast, overlayFast, samplBest, overlayBest},fading);

   final private JLabel labelSampl = new JLabel();
   final private JLabel labelOverlay = new JLabel();

   final private String BIT8 = "short (8bits)";
   final private String BIT16 = "int (16bits)";
   final private String BIT32 = "long int (32bits)";
   final private String BIT_32 = "float real (32bits)";
   final private String BIT64 = "double real (64bits)";

   private ButtonGroup 	groupBitpix = new ButtonGroup();
   private JCheckBox 	keepBB = new JCheckBox();

   private ActionListener 	bitpixListener = new BitpixListener(keepBB);
   private JRadioButton 	bit8 = new JRadioButton(BIT8, false);
   private JRadioButton 	bit16 = new JRadioButton(BIT16, false);
   private JRadioButton 	bit32 = new JRadioButton(BIT32, true);
   private JRadioButton 	bit_32 = new JRadioButton(BIT_32, false);
   private JRadioButton 	bit_64 = new JRadioButton(BIT64, false);

   private double bscale;
   private double bzero;
   private double blank;
   private int bitpixO = -1;
   private JLabel bitpixLabel = new JLabel();
   private JLabel resoLabel;

   private String OK,RESTART,RESUME,STOP,DONE, NEXT;
   private String canceltip;
   AllskyPanel allskyPanel;

   AllskyStepsPanel pSteps;

   private void createChaine() {
      STOP = getString("STOP");
      OK = getString("START");
      RESUME = getString("RESUME");
      DONE = getString("DONE");
      RESTART = getString("RE_START");
      canceltip = getString("TIPCANCALLSKY");
      NEXT = getString("NEXT");
   }

   private String getString(String k) { return allskyPanel.aladin.getChaine().getString(k); }

   public BuildPanel(AllskyPanel panel) {
      super(new BorderLayout());
      setMainPanel(panel);
      createChaine();

      JPanel pCenter = new JPanel();
      pCenter.setLayout(new GridBagLayout());
      init();
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      c.insets = new Insets(0,0,0,0);

      // Zone de sélection du bitpix
      c.gridy++;
      bitpixLabel.setText(getString("BITPIXALLSKY"));
      pCenter.add(bitpixLabel, c);
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridx++;
      pCenter.add(Util.getHelpButton(this,getString("HELPBITPIXALLSKY")),c);
      c.gridx=0;
      c.gridwidth = 1;
      c.gridy++;
      pCenter.add(bit8, c);
      c.gridx++;
      pCenter.add(bit16, c);
      c.gridx++;
      pCenter.add(bit32, c);
      c.gridy++;
      c.gridx = 0;
      pCenter.add(bit_32, c);
      c.gridx++;
      pCenter.add(bit_64, c);
      c.gridx++;
      keepBB.setText(getString("KEEPORIGALLSKY"));
      pCenter.add(keepBB, c);

      // Zone de sélection de la résolution
      //		c.gridx = 0;
      //		c.gridy++;
      //		c.anchor = GridBagConstraints.WEST;
      //		c.fill = GridBagConstraints.HORIZONTAL;
      //		c.gridwidth = GridBagConstraints.REMAINDER;
      //		pCenter.add(new JSeparator(JSeparator.HORIZONTAL), c);
      //		c.fill = GridBagConstraints.NONE;
      //		c.gridwidth = 1;

      c.insets.top=15;
      c.gridx=0;
      c.gridy++;
      c.gridwidth = 1;
      resoLabel.setText(getString("RESOALLSKY"));
      pCenter.add(resoLabel, c);
      c.gridx++;
      c.gridwidth = GridBagConstraints.REMAINDER;
      pCenter.add(Util.getHelpButton(this,getString("HELPRESOALLSKY")),c);
      c.insets.top=0;

      // Tableau des résolutions
      c.gridx = 0;
      c.gridy++;
      c.anchor = GridBagConstraints.CENTER;
      c.gridwidth = GridBagConstraints.REMAINDER;
      tab.setPreferredScrollableViewportSize(new Dimension(500,100));
      tab.setRowSelectionAllowed(false);
      JScrollPane pane = new JScrollPane(tab);
      pCenter.add(pane, c);

      // méthode fast=plus proche / best=bilinéaire
//      		c.gridx = 0;
//      		c.gridy++;
//      		c.anchor = GridBagConstraints.WEST;
//      		c.fill = GridBagConstraints.HORIZONTAL;
//      		c.gridwidth = GridBagConstraints.REMAINDER;
//      		pCenter.add(new JSeparator(JSeparator.HORIZONTAL), c);
//      		c.fill = GridBagConstraints.NONE;

      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.NONE;
      c.insets.top=15;
      c.gridx = 0;
      c.gridwidth = 1;
      c.gridy++;
      method_label.setText(getString("RESPARAMALLSKY"));
      pCenter.add(method_label, c);
      c.gridx++;
      c.gridwidth = GridBagConstraints.REMAINDER;;
      pCenter.add(Util.getHelpButton(this,getString("HELPESAMPALLSKY")),c);
      c.insets.top=0;

      c.gridwidth = 1;
      c.gridx = 0;
      c.gridy++;
      labelSampl.setText(getString("SAMPLINGALLSKY"));
      pCenter.add(labelSampl,c);
      c.gridx++;
      samplFast.setText(getString("FIRSTALLSKY"));
      pCenter.add(samplFast, c);
      c.gridwidth=2;
      c.gridx++;
      samplBest.setText(getString("BILIALLSKY"));
      pCenter.add(samplBest, c);

      c.gridwidth=1;
      c.gridx = 0;
      c.gridy++;
      labelOverlay.setText(getString("OVERLAYALLSKY"));
      pCenter.add(labelOverlay,c);
      c.gridx++;
      overlayFast.setText(getString("FIRSTALLSKY"));
      pCenter.add(overlayFast, c);
      c.gridx++;

      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
      overlayBest.setText(getString("COADDALLSKY"));
      p1.add(overlayBest);
      overlayBest.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            fading.setEnabled( overlayBest.isSelected() );
         }
      });
      fading.setText(getString("FADINGALLSKY"));
      p1.add(fading); fading.setSelected(true);		
      pCenter.add(p1, c);

      // barres de progression
      c.insets.top=15;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridy++;c.gridx=0;
      pSteps = new AllskyStepsPanel();
      pCenter.add(pSteps, c);

      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      initBtn();
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      pBtn.add(b_ok);
      pBtn.add(b_cancel);
      //		pBtn.add(b_close);
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      pBtn.add(b_next);
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pBtn, BorderLayout.CENTER);
//      fin.add(b_help, BorderLayout.EAST);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }

   public void init() {
      tab = new TableNside();
      resoLabel = new JLabel();
      resoLabel.setFont(resoLabel.getFont().deriveFont(Font.BOLD));

      // bitpix
      bitpixLabel.setFont(bitpixLabel.getFont().deriveFont(Font.BOLD));
      keepBB.setEnabled(false);
      initBitpix();

      // methodes
      method_label.setFont(resoLabel.getFont().deriveFont(Font.BOLD));
      samplBest.setSelected(true);
      fading.setSelected(true);
      overlayBest.setSelected(true);
      initMethods();
      initListenerBitpix();

      // boutons
      initBtn();
   }

   private void initBtn() {
      b_ok = new JButton(OK);
      b_ok.addActionListener(this);
      b_ok.setEnabled(false);
      b_cancel = new JButton(STOP);
      b_cancel.addActionListener(this);
      b_cancel.setToolTipText(canceltip);
      b_cancel.setEnabled(false);
//      b_help = Util.getHelpButton(this,help);
      b_next = new JButton(NEXT);
      b_next.addActionListener(this);
      b_next.setEnabled(false);
   }

   private void initMethods() {
      groupSampl.add(samplBest);
      groupSampl.add(samplFast);
      groupOverlay.add(overlayBest);
      groupOverlay.add(overlayFast);
      samplBest.addActionListener(methodsListener);
      samplFast.addActionListener(methodsListener);
      overlayBest.addActionListener(methodsListener);
      overlayFast.addActionListener(methodsListener);

      samplBest.setActionCommand(BEST);
      samplFast.setActionCommand(FIRST);
      overlayBest.setActionCommand(BEST);
      overlayFast.setActionCommand(FIRST);
   }

   private void initListenerBitpix() {
      bit8.addActionListener(bitpixListener);
      bit16.addActionListener(bitpixListener);
      bit32.addActionListener(bitpixListener);
      bit_32.addActionListener(bitpixListener);
      bit_64.addActionListener(bitpixListener);
   }

   private void initBitpix() {
      bit8.setActionCommand("8");
      bit16.setActionCommand("16");
      bit32.setActionCommand("32");
      bit_32.setActionCommand("-32");
      bit_64.setActionCommand("-64");

      bit8.addActionListener(this);
      bit16.addActionListener(this);
      bit32.addActionListener(this);
      bit_32.addActionListener(this);
      bit_64.addActionListener(this);

      groupBitpix.add(bit8);
      groupBitpix.add(bit16);
      groupBitpix.add(bit32);
      groupBitpix.add(bit_32);
      groupBitpix.add(bit_64);
   }

   void setMainPanel(AllskyPanel panel) {
      allskyPanel = panel;
   }

   public void clearForms() {
      bitpixO = -1;
      bit8.setSelected(false);
      bit16.setSelected(false);
      bit32.setSelected(true);
      bit_32.setSelected(false);
      bit_64.setSelected(false);
      blank = Fits.DEFAULT_BLANK;
      bscale = Fits.DEFAULT_BSCALE;
      bzero = Fits.DEFAULT_BZERO;
      samplBest.setSelected(true);
      overlayBest.setSelected(true);
      fading.setSelected(true);
      ((TableNside) tab).reset();
   }

   public int setSelectedOrder(int val) {
      int i = ((TableNside) tab).setSelectedOrder(val);
      ((TableNside) tab).setDefaultRow(i);
      tab.repaint();
      return i;
   }

   /**
    * 
    * @return order choisi ou -1 s'il doit etre calculé
    */
   public int getOrder() {
      return ((TableNside) tab).getOrder();
   }

   public void setBScaleBZero(double bscale, double bzero) {
      this.bscale = bscale;
      this.bzero = bzero;
   }

   public void setBlank(double blank) {
      this.blank = blank;
   }

   public double getBscale() {
      // si ce n'est pas le bitpix original
      // on renvoie une valeur par défaut
      if (this.bitpixO != getBitpix())
         return Fits.DEFAULT_BSCALE;
      return bscale;
   }

   public double getBzero() {
      // si ce n'est pas le bitpix original
      // on renvoie une valeur par défaut
      if (this.bitpixO != getBitpix())
         return Fits.DEFAULT_BZERO;
      return bzero;
   }

   public double getBlank() {
      // si ce n'est pas le bitpix original
      // on renvoie une valeur par défaut
      if (this.bitpixO != getBitpix())
         return Fits.DEFAULT_BLANK;
      return blank;
   }

   public boolean isKeepBB() {
      return keepBB.isSelected();
   }

   public boolean toFast() {
      return samplFast.isSelected();
   }

   public boolean toFading() {
      return samplBest.isSelected() && fading.isSelected();
   }

   public void setOriginalBitpix(int bitpix) {
      this.bitpixO = bitpix;
      ((BitpixListener) bitpixListener).setDefault(bitpix);
      switch (bitpix) {
         case 8:
            bit8.doClick();
            break;
         case 16:
            bit16.doClick();
            break;
         case 32:
            bit32.doClick();
            break;
         case -32:
            bit_32.doClick();
            break;
         case 64:
            bit_64.doClick();
            break;
      }
   }

   public int getOriginalBitpix() {
      return bitpixO;
   }


   /**
    * Renvoie le bitpix sélectionné dans le formulaire
    * 
    * @return
    */
   public int getBitpix() {
      ButtonModel b = groupBitpix.getSelection();
      int i = TableNside.DEFAULT_BITPIX;
      try {
         i = Integer.parseInt(b.getActionCommand());
      } catch (NumberFormatException e) {
         e.printStackTrace();
      }
      return i;
   }

   public void displayStart() {
      b_ok.setText(OK);
      b_ok.setEnabled(true);
      b_cancel.setEnabled(true);
      b_next.setEnabled(false);
      enableProgress(true,INDEX);
      enableProgress(true,TESS);
      setProgress(INDEX,0);
      setProgress(TESS,0);
   }
   public void displayReStart() {
      b_ok.setText(RESTART);
      b_ok.setEnabled(true);
      b_cancel.setEnabled(false);
      displayNext();
   }
   public void displayResume() {
      b_ok.setText(RESUME);
      b_ok.setEnabled(true);
      b_cancel.setEnabled(false);
      displayNext();
   }
   public void displayDone() {
      b_ok.setText(DONE);
      b_ok.setEnabled(false);
      b_cancel.setEnabled(false);
      displayNext();
      setCursor(null);
      allskyPanel.pDesc.setFieldEnabled(true);
   }
   public void displayNext() {
      b_next.setEnabled(true);
   }

   protected void setProgress(int mode, int value) {
      switch (mode) {
         case INDEX:
            pSteps.setProgressIndex(value);
            break;
         case TESS:
            pSteps.setProgressTess(value);
            break;
      }
   }

   protected void enableProgress(boolean selected, int mode) {
      pSteps.select(selected, mode);
   }

   protected void setInitDir(String txt) {
      pSteps.setProgressIndexTxt(txt);
   }

   AllskyTask task;
   public void actionPerformed(ActionEvent e) {
      // on applique aussi la modification dans le tableau (calcul des volumes
      // disques)
      ((TableNside) tab).setBitpix(getBitpix());

      // START / RESTART / RESUME 
      if (e.getSource() == b_ok) {
         // PAUSE -> b_cancel
         //			if (e.getActionCommand() == PAUSE) {
         //				if (task != null)
         //					task.stopThread();
         //				allskyPanel.setResume();
         //				stop();
         //				return;
         //			}

         // initialisation correcte des barres de progression et boutons
         allskyPanel.resetProgress();

         b_ok.setEnabled(false);
         b_cancel.setEnabled(true);
         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

         // bloque les champs texte
         allskyPanel.pDesc.setFieldEnabled(false);

         if (getBitpix() == -1) {
            allskyPanel.init();
         }

         // effectue le nettoyage selon les "reset" cochés
         allskyPanel.toReset();

         //lance les taches en arrière plan
         task = new AllskyTask(allskyPanel);
         if (task.isDone()) {
            try {
               task.doInBackground();
            } catch (Exception e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
               return;
            }

            // Thread qui attend de savoir que les calculs sont terminés
            new Thread(new Runnable() {
               public void run() {
                  boolean done = false;
                  while(!done) {
                     try {
                        Thread.sleep(1000);
                     } catch (InterruptedException e) {
                     }
                     if (task == null || task.isDone()) {
                        Aladin.trace(3,"Task END");
                        done=true;
                        if (task == null) stop();
                     }
                  }
               }
            }).start();
         }
         // else rien, il faut attendre que la tache soit terminee ou interrompue
         //		    showBuild();
      } else if (e.getSource() == b_cancel) {
         // arrete les process en cours
         if (task != null)
            task.stopThread();
         stop();
         // se prépare à relancer le process
         allskyPanel.setResume();

         //		} else if (e.getSource() == b_close) {
         //			// ferme juste la frame
         //			allskyPanel.close();
      } else if (e.getSource() == b_next) {
         allskyPanel.showDisplay();
      }

   }

   public void stop() {
      allskyPanel.setResume();
      b_cancel.setEnabled(false);
      setCursor(null);
      allskyPanel.pDesc.setFieldEnabled(true);
   }

   class BitpixListener implements ActionListener {
      int defaultBitpix = TableNside.DEFAULT_BITPIX;

      public BitpixListener(JCheckBox keepCheckBox) {
         check = keepCheckBox;
      }
      JCheckBox check = null;
      public void setDefault(int bitpix) {
         defaultBitpix = bitpix;
      }

      public void actionPerformed(ActionEvent arg0) {
         int i = Integer.parseInt(arg0.getActionCommand());
         if (i == defaultBitpix) setKeepOn();
         else setKeepOff();

      }
      private void setKeepOn() {
         check.setEnabled(true);
         check.setSelected(true);
      }
      private void setKeepOff() {
         check.setSelected(false);
         check.setEnabled(false);
      }
   }

   class MethodSelectListener implements ActionListener {
      private JRadioButton samplFast = null;
      private JRadioButton overlayFast = null;
      private JRadioButton samplBest = null;
      private JRadioButton overlayBest = null;
      private JCheckBox fading=null;

      public MethodSelectListener(JRadioButton[] buttons,JCheckBox fading) {
         super();
         samplFast=buttons[0];
         overlayFast = buttons[1];
         samplBest=buttons[2];
         overlayBest = buttons[3];
         this.fading=fading;
      }

      public void actionPerformed(ActionEvent e) {
         Object source = e.getSource();

         if (source == samplFast) {
            if (!overlayFast.isSelected())
               overlayFast.doClick();
         }
         if (source == overlayFast) {
            if (!samplFast.isSelected())
               samplFast.doClick();
         }
         if (source == samplBest) {
            if (!overlayBest.isSelected())
               overlayBest.doClick();
         }
         if (source == overlayBest) {
            if (!samplBest.isSelected())
               samplBest.doClick();
         }

         fading.setEnabled(samplBest.isSelected());
      }
   }
}

