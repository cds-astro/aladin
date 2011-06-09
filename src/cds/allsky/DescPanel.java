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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.tools.Util;

public class DescPanel extends JPanel implements ActionListener {

   private String REP_SOURCE;
   private String REP_DEST;
   private String REP_DEST_RESET;

   private JLabel infoLabel;
   private JLabel destLabel;
   private JLabel sourceLabel;
   private JLabel labelAllsky;
   private String LABELALLSKY;
   private String NEXT;
   private String INFOALLSKY;
   private String PARAMALLSKY;
   private String KEEPALLSKY,COADDALLSKY,OVERWRITEALLSKY;
   private String SPECIFALLSKY,BLANKALLSKY ;

   
   private JLabel paramLabel;
   private JRadioButton keepRadio,coaddRadio,overwriteRadio;
   private JCheckBox specifCheckbox;
   private JCheckBox blankCheckbox;
   private ButtonGroup tilesGroup;
   private JTextField specifTextField;
   protected JTextField blankTextField;

   private JCheckBox resetCheckbox = new JCheckBox();
   private JButton browse_S = new JButton();
   private JButton browse_D = new JButton();
   private JTextField dir_S = new JTextField(30); 
   private JTextField dir_D = new JTextField(30);
   private JTextField textFieldAllsky = new JTextField(30);
   private String defaultDirectory;
   private AllskyPanel parentPanel;
   private String BROWSE;
   private JButton b_next;
   private String help, titlehelp;

   public DescPanel(String defaultDir, AllskyPanel parent) {
      super(new BorderLayout());
      this.parentPanel = parent;
      createChaine();
      init();
      
      JPanel pCenter = new JPanel(new GridBagLayout());
      this.defaultDirectory = defaultDir;

      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(1, 5, 1, 5);
      c.anchor = GridBagConstraints.NORTHWEST;
      
      // Baratin explicatif
      c.gridy = 0;
      c.gridx = 0;
      c.gridwidth = 4;
      pCenter.add(infoLabel,c);

      // Répertoire source
      c.insets.top = 20;
      c.gridwidth = 1;
      c.gridy++;
      c.gridx = 0;
      pCenter.add(sourceLabel, c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPDIRSRCALLSKY")),c);
      c.gridx++;
      pCenter.add(dir_S, c);
      c.gridx++;
      pCenter.add(browse_S, c);

      // Répertoire destination
      c.insets.top = 1;
      c.gridy++;
      c.gridx = 0;
      pCenter.add(destLabel, c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPDIRTRGALLSKY")),c);
      c.gridx++;
      pCenter.add(dir_D, c);
      c.gridx++;
      pCenter.add(browse_D, c);
      
      // Label
      c.insets.bottom = 20;
      c.gridy++;
      c.gridx = 0;
      pCenter.add(labelAllsky, c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPLABELALLSKY")),c);
      c.gridx++;
      pCenter.add(textFieldAllsky, c);

      // Paramètres avancées
      c.insets.bottom=1;
      c.gridx=0;
      c.gridy++;
      pCenter.add(paramLabel,c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPPARAMALLSKY")),c);
      c.gridx++;
      c.gridwidth=2;
      pCenter.add(resetCheckbox, c);
      c.gridy++;
      JPanel pTiles = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      pTiles.add(keepRadio);      keepRadio.setEnabled(false);
      pTiles.add(overwriteRadio); overwriteRadio.setEnabled(false);
      pTiles.add(coaddRadio);     coaddRadio.setEnabled(false);
      pCenter.add(pTiles, c);
      
      c.gridx=2;
      c.gridy++;
      pCenter.add(specifCheckbox,c); 

      c.gridy++;
      c.insets.left=60;
      JPanel pSpecif = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      pSpecif.add(specifTextField);
      JButton gridButton = new JButton(getString("HPXGRID"));
      gridButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            parentPanel.aladin.switchHpxGrid();
         }
      });
      pSpecif.add(gridButton);
      specifTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            specifCheckbox.setSelected( specifTextField.getText().trim().length()>0 );
         }
      });
      pCenter.add(pSpecif, c);
      c.insets.left=1;

      c.gridy++;
      JPanel pBlank = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      blankTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            blankCheckbox.setSelected( blankTextField.getText().trim().length()>0 );
         }
      });
      pBlank.add(blankCheckbox);
      pBlank.add(blankTextField);
      pCenter.add(pBlank,c); 

      if (Aladin.PROTO) {
         final JCheckBox cb = new JCheckBox("DSS Schmidt plates", false);
         cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               DBBuilder.DSS = cb.isSelected();
            }
         });
         c.gridy++;
         pCenter.add(cb, c);
      }
     
      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      pBtn.add(b_next);
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pBtn, BorderLayout.CENTER);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }

   private void createChaine() {
      REP_SOURCE = getString("REPSALLSKY");
      BROWSE = getString("FILEBROWSE");
      REP_DEST = getString("REPDALLSKY");
      REP_DEST_RESET = getString("REPRESALLSKY");
      LABELALLSKY = getString("LABELALLSKY");
      NEXT = getString("NEXT");
      titlehelp = getString("HHELP");
      INFOALLSKY = getString("INFOALLSKY");
      PARAMALLSKY = getString("PARAMALLSKY");
      COADDALLSKY = getString("COADDALLSKY");
      KEEPALLSKY = getString("KEEPALLSKY");
      OVERWRITEALLSKY = getString("OVERWRITEALLSKY");
      SPECIFALLSKY  = getString("SPECIFALLSKY");
      BLANKALLSKY  = getString("BLANKALLSKY");
   }
   
   private String getString(String k) { return parentPanel.aladin.getChaine().getString(k); }

   public void init() {
      infoLabel = new JLabel(Util.fold(INFOALLSKY,100,true));
      infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
      sourceLabel = new JLabel(REP_SOURCE);
      sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.BOLD));
      destLabel = new JLabel(REP_DEST);
      destLabel.setFont(destLabel.getFont().deriveFont(Font.BOLD));
      dir_S.addActionListener(this);
      dir_S.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            super.keyTyped(e);
            if (!dir_S.getText().equals(""))
               actionPerformed(new ActionEvent(dir_S, -1, "dirBrowser Action"));
         }
      });

      browse_S.setText(BROWSE);
      browse_S.addActionListener(this);
      dir_D.addActionListener(this);
      dir_D.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            super.keyTyped(e);
            if (!dir_D.getText().equals(""))
               actionPerformed(new ActionEvent(dir_D, -1, "dirBrowser Action"));
         }
      });
      browse_D.setText(BROWSE);
      browse_D.addActionListener(this);
      
      labelAllsky = new JLabel(LABELALLSKY);
      labelAllsky.setFont(labelAllsky.getFont().deriveFont(Font.BOLD));
      
      paramLabel = new JLabel(PARAMALLSKY);
      paramLabel.setFont(paramLabel.getFont().deriveFont(Font.BOLD));
      
      tilesGroup = new ButtonGroup();
      keepRadio = new JRadioButton(KEEPALLSKY); tilesGroup.add(keepRadio);
      overwriteRadio = new JRadioButton(OVERWRITEALLSKY); tilesGroup.add(overwriteRadio);
      coaddRadio = new JRadioButton(COADDALLSKY); tilesGroup.add(coaddRadio);
      keepRadio.setSelected(true);
      specifCheckbox = new JCheckBox(SPECIFALLSKY); specifCheckbox.setSelected(false);
      specifTextField = new JTextField(30);
      blankCheckbox = new JCheckBox(BLANKALLSKY); blankCheckbox.setSelected(false);
      blankTextField = new JTextField(18);

      resetCheckbox.setText(REP_DEST_RESET);
      resetCheckbox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (resetCheckbox.isSelected()) {
               parentPanel.setRestart();
               coaddRadio.setSelected(false);
               coaddRadio.setEnabled(false);
            }
            else {
               // if (!resetHpx.isSelected())
               parentPanel.setResume();
               keepRadio.setEnabled(true);
               overwriteRadio.setEnabled(true);
               coaddRadio.setEnabled(true);
            }
         }
      });
      // resetHpx.setText(s_hpxfiles);
      // resetHpx.addActionListener(new ActionListener() {
      // public void actionPerformed(ActionEvent e) {
      // if (resetHpx.isSelected())
      // parentPanel.aladin.frameAllsky.setRestart();
      // else if (!resetIndex.isSelected())
      // parentPanel.aladin.frameAllsky.setResume();
      // }
      // });
      setResetSelected(false);
      setResetEnable(false);

      b_next = new JButton(NEXT);
      b_next.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            parentPanel.showBuild();
         }
      });
//      b_help = Util.getHelpButton(this, help);

   }

   public void clearForms() {
      dir_S.setText("");
      dir_D.setText("");
      parentPanel.actionPerformed(new ActionEvent("", -1, "dirBrowser Action"));
      textFieldAllsky.setText("");
      // desc.setText("");
      // descfull.setText("");
      // copyright.setText("");
      // origin.setText("");
      setResetSelected(false);
      setResetEnable(false);
   }

   private void dirBrowser(JTextField dir) {
      String currentDirectoryPath = dir.getText().trim();
      if( currentDirectoryPath.length()==0 ) currentDirectoryPath=defaultDirectory;
      String s = Util.dirBrowser(this, currentDirectoryPath);
      if( s==null ) return;
      dir.setText(s);
      parentPanel.actionPerformed(new ActionEvent(dir, -1, "dirBrowser Action"));
   }

   public String getInputPath() {
      return dir_S.getText();
   }

   public String getOutputPath() {
      return dir_D.getText();
   }
   
   static final int KEEP = 0;
   static final int OVERWRITE = 1;
   static final int AVERAGE = 2;
   static final String [] COADDMODE = { "keep","overwrite","average" };

   public int getCoaddMode() {
      return resetCheckbox.isSelected() ? OVERWRITE : 
            keepRadio.isSelected() ? KEEP 
            :overwriteRadio.isSelected() ? OVERWRITE : AVERAGE;
   }

   public JTextField getSourceDirField() {
      return dir_S;
   }

   public void setFieldEnabled(boolean enabled) {
      dir_S.setEnabled(enabled);
      dir_D.setEnabled(enabled);
   }

   public void actionPerformed(ActionEvent e) {

      if (e.getSource() == dir_S) {
         initTxt();
      } else if (e.getSource() == browse_S) {
         dirBrowser(dir_S);
         initTxt();
      }

      if (e.getSource() == dir_D) {
         newAllskyDir();
      } else if (e.getSource() == browse_D) {
         dirBrowser(dir_D);
         newAllskyDir();
      }
   }

   /**
    * Itialisation des variables textuelles en fonction du nouveau répertoire
    * SOURCE
    */
   private void initTxt() {
      String txt = dir_S.getText();
      int i = txt.lastIndexOf(Util.FS);
      if (i == -1) return;

      // ne traite pas le dernier séparateur
      while (i + 1 == txt.length()) txt = txt.substring(0, i);
      
      // cherche le dernier mot et le met dans le label
      String str = txt.substring(txt.lastIndexOf(Util.FS) + 1);
      textFieldAllsky.setText(str);
      // dir_A.setText(str+AllskyConst.SURVEY);

      // rééinitialise le répertoire de destination avec le chemin des données
      // d'entrée
      dir_D.setText("");
      newAllskyDir();
   }

   public String getLabel() {
      return textFieldAllsky.getText();
   }
   
   public String getSpecifNpix() {
      if( !specifCheckbox.isSelected() ) return "";
      return specifTextField.getText();
   }

   public String getBlank() {
      if( !blankCheckbox.isSelected() ) return "";
      return blankTextField.getText();
   }


   /**
    * Applique les modifications si le nom du répertoire DESTINATION change
    */
   private void newAllskyDir() {
      String str = dir_D.getText();
      // enlève les multiples FS à la fin
      while (str.endsWith(Util.FS))
         str = str.substring(0, str.lastIndexOf(Util.FS));

      // si l'entrée est vide, on remet le défaut
      if (str.equals("")) {
         // réinitalise le répertoire SURVEY et l'utilise
         initDirD();
         parentPanel.newAllskyDir();
         return;
      }
      // cherche le dernier mot
      AllskyConst.SURVEY = str.substring(str.lastIndexOf(Util.FS) + 1);

      parentPanel.newAllskyDir();
   }

   private void initDirD() {
      AllskyConst.SURVEY = getLabel() + AllskyConst.ALLSKY;
      String path = dir_S.getText();
      // enlève les multiples FS à la fin
      while (path.endsWith(Util.FS))
         path = path.substring(0, path.lastIndexOf(Util.FS));

      dir_D.setText(path + AllskyConst.ALLSKY + Util.FS);
      dir_D.repaint();
   }

   public void setResetSelected(boolean b) {
      resetCheckbox.setSelected(b);
      keepRadio.setEnabled(!b);
      coaddRadio.setEnabled(!b);
      overwriteRadio.setEnabled(!b);
      // resetHpx.setSelected(b);
   }

   public boolean toReset() {
      return resetCheckbox.isSelected();
   }

   // public boolean toResetHpx() {
   // return resetHpx.isSelected();
   // }

   public void setResetEnable(boolean enable) {
      // resetLabel.setEnabled(enable);
      resetCheckbox.setEnabled(enable);
      keepRadio.setEnabled(enable);
      coaddRadio.setEnabled(enable);
      overwriteRadio.setEnabled(enable);
      // resetHpx.setEnabled(enable);
   }

   public void help() {
      JOptionPane.showMessageDialog(this, help, titlehelp,
            JOptionPane.INFORMATION_MESSAGE);
   }
}
