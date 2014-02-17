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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.moc.HealpixMoc;
import cds.tools.Util;

public class TabBuild extends JPanel implements ActionListener {
   protected static final String BEST = "best";
   protected static final String FIRST = "first";

   protected JButton abort;
   protected JButton start;
   protected JButton pause;
   protected JButton previous;
   protected JButton next;
   protected JButton moc;

   private static BuildTable tab = null;

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

   private int bitpixOrig = -1;
   private JLabel bitpixLabel = new JLabel();
   private JLabel resoLabel;
   private JLabel note;

   private String canceltip;
   MainPanel mainPanel;
   Context context;
   
   protected BuildProgressPanel buildProgressPanel;

   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   public TabBuild(MainPanel panel) {
      super(new BorderLayout());
      mainPanel = panel;
      context = mainPanel.context;

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
      tab.setPreferredScrollableViewportSize(new Dimension(500,120));
      tab.setRowSelectionAllowed(false);
      JScrollPane pane = new JScrollPane(tab);
      pCenter.add(pane, c);
      c.gridy++;
      c.anchor = GridBagConstraints.EAST;
      JLabel l = note = new JLabel("<html><i>(*) whole sky</i></html>");
      pCenter.add(l,c);

      // barres de progression
      c.insets.top=15;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridy++;c.gridx=0;
      buildProgressPanel = new BuildProgressPanel();
      pCenter.add(buildProgressPanel, c);
      buildProgressPanel.resetProgressBar();

      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      initBtn();
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      pBtn.add(previous);
      pBtn.add(moc);
      pBtn.add(start);
      pBtn.add(pause);
      pBtn.add(abort);
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      pBtn.add(next);
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pBtn, BorderLayout.CENTER);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }

   public void init() {
      tab = new BuildTable(context,this);
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
      JButton bt;
      previous = bt=new JButton(getString("PREVIOUS"));
      bt.addActionListener(this);
      
      moc = bt=new JButton(getString("LOADMOC"));
      bt.addActionListener(this);
      
      start = bt=new JButton(getString("START"));  
      bt.addActionListener(this);
      
      pause = bt=new JButton(getString("PAUSE"));  
      bt.addActionListener(this);
      
      abort = bt=new JButton(getString("ABORT"));
      bt.addActionListener(this); bt.setToolTipText(canceltip); 
      
      next = bt=new JButton(getString("NEXT"));
      bt.addActionListener(this); 
   }
   
   private void loadMoc() {
      String mocFile = mainPanel.context.getHpxFinderPath()+Util.FS+BuilderMoc.MOCNAME;
      mainPanel.aladin.execAsyncCommand("load "+mocFile);
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

   public void show() {
      super.show();
      mainPanel.init();
      mainPanel.resumeWidgets();
   }
   
   private String getCoverageString() {
      double cov = context.getIndexSkyArea();
      if( cov==1 ) return "whole sky ";
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      return Util.round(cov*100, 3)+"% of sky => "+Coord.getUnit(skyArea*cov, false, true)+"^2 ";
   }
   
   protected void resumeWidgets() {
      try {
         boolean readyToDo = context.isExistingDir() && mainPanel.tabDesc.outputField.getText().trim().length()>0;
         boolean isRunning = context.isTaskRunning();
         boolean isExistingMoc = context.getMocIndex()!=null;
         moc.setEnabled(isExistingMoc);
         note.setText("<html><i>(*) "+getCoverageString()+"</i></html>");
         previous.setEnabled(readyToDo && !isRunning);
         next.setEnabled(readyToDo && !isRunning && context.isExistingAllskyDir() );
         start.setEnabled(readyToDo && !isRunning && !(isRunning));
         pause.setEnabled(isRunning);
         abort.setEnabled(readyToDo && isRunning);
         
         bit8.setEnabled(readyToDo && !isRunning && bitpixOrig!=0 );
         bit16.setEnabled(readyToDo && !isRunning && bitpixOrig!=0 );
         bit32.setEnabled(readyToDo && !isRunning && bitpixOrig!=0 );
         bit_32.setEnabled(readyToDo && !isRunning && bitpixOrig!=0 );
         bit_64.setEnabled(readyToDo && !isRunning && bitpixOrig!=0 );
         samplFast.setEnabled(readyToDo && !isRunning);
         overlayFast.setEnabled(readyToDo && !isRunning);
         samplBest.setEnabled(readyToDo && !isRunning);
         overlayBest.setEnabled(readyToDo && !isRunning);
         fading.setEnabled(readyToDo && !isRunning);
         tab.setBackground( readyToDo && !isRunning ? Color.white : getBackground() );
         setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) 
                              : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
         tab.refresh();
      } catch( Exception e ) {
         e.printStackTrace();
      } 
   }
   
   public void clearForms() {
      bitpixOrig = -1;
      bit8.setSelected(false);
      bit16.setSelected(false);
      bit32.setSelected(true);
      bit_32.setSelected(false);
      bit_64.setSelected(false);
      samplBest.setSelected(true);
      overlayBest.setSelected(true);
      fading.setSelected(true);
      ((BuildTable) tab).reset();
      resumeWidgets();
   }

   public int setSelectedOrder(int val) {
      int i = ((BuildTable) tab).setSelectedOrder(val);
      ((BuildTable) tab).setDefaultRow(i);
      tab.repaint();
      return i;
   }

   /**
    * 
    * @return order choisi ou -1 s'il doit etre calculé
    */
   public int getOrder() {
      return ((BuildTable) tab).getOrder();
   }
   
   public void setOrder(int order) { setSelectedOrder(order); }

   public void setOriginalBitpixField(int bitpix) {
      this.bitpixOrig = bitpix;
      ((BitpixListener) bitpixListener).setDefault(bitpix);
      switch (bitpix) {
         case 0:
            keepBB.doClick();
            break;
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

   public int getOriginalBitpixField() {
      return bitpixOrig;
   }

   /**
    * Renvoie le bitpix sélectionné dans le formulaire
    * 
    * @return
    */
   public int getBitpixField() {
      if( bitpixOrig==0 ) return 0;
      ButtonModel b = groupBitpix.getSelection();
      int i = BuildTable.DEFAULT_BITPIX;
      try {
         i = Integer.parseInt(b.getActionCommand());
      } catch (NumberFormatException e) {
         e.printStackTrace();
      }
      return i;
   }
   
   /** Retourne le nombre de bytes qui correspond au bitpix sélectionné dans le formulaire */
   protected int getNpix() {
      return context.isColor() ? 4 : Math.abs(getBitpixField())/8;
   }

   public void actionPerformed(ActionEvent e) {
      // on applique aussi la modification dans le tableau (calcul des volumes disques)
      ((BuildTable) tab).setBitpix(getBitpixField());

           if (e.getSource() == start)    perform();
      else if (e.getSource() == abort)    abort();
      else if (e.getSource() == pause)    pause();
      else if (e.getSource() == moc)      loadMoc();
      else if (e.getSource() == next)     mainPanel.showJpgTab();
      else if (e.getSource() == previous) mainPanel.showDescTab();
      
      mainPanel.resumeWidgets();
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
      if( !Aladin.confirmation(mainPanel, "Do you really want to abort the HEALPix survey computation ?") ) return;
      context.taskAbort();
   }
   
   private void perform() {
      try {
         Vector<Action> actions = new Vector<Action>();
         
         if( mainPanel.tabDesc.isResetIndex() ) actions.add(Action.CLEANINDEX);
         actions.add(Action.INDEX);
         if( mainPanel.tabDesc.isResetTiles() ) actions.add(Action.CLEANTILES);
         actions.add(Action.TILES);
         if( !context.isColor() ) {
            actions.add(Action.GZIP);
            actions.add(Action.DETAILS);
         }

         new Task(context, actions, false);
      } catch( Exception e1 ) {
         Aladin.warning(e1.getMessage());
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
      }
   }

   class BitpixListener implements ActionListener {
      int defaultBitpix = BuildTable.DEFAULT_BITPIX;

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

