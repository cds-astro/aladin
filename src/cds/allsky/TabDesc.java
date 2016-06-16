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
import java.awt.Cursor;
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

import cds.aladin.Aladin;
import cds.aladin.Localisation;
import cds.tools.Util;

public class TabDesc extends JPanel implements ActionListener {

   private String REP_SOURCE;
   private String REP_DEST;
   private String REP_DEST_RESET;
   private String INDEX_RESET;

   private JLabel infoLabel;
   private JLabel destLabel;
   private JLabel sourceLabel;
   private JLabel labelAllsky;
   private String LABELALLSKY;
   private String NEXT;
   private String SEE;
   private String INFOALLSKY;
   private String PARAMALLSKY;
   private String KEEPALLSKY,COADDALLSKY,OVERWRITEALLSKY,KEEPCELLALLSKY;
   private String SPECIFALLSKY,BLANKALLSKY,BORDERALLSKY, SKYVALALLSKY,HDUALLSKY ;

   
   private JLabel paramLabel;
   private JRadioButton keepRadio,coaddRadio,overwriteRadio,keepCellRadio;
   private JCheckBox specifCheckbox;
   private JCheckBox blankCheckbox;
   private JCheckBox hduCheckbox;
   private JCheckBox borderCheckbox;
   private JRadioButton skyvalCheckbox;
   private JCheckBox frameCheckbox;
   
   private JTextField specifTextField;
   protected JTextField blankTextField;
   protected JTextField hduTextField;
   protected JTextField borderTextField;
   private JTextField skyvalTextField;

   protected JTextField inputField = new JTextField(35); 
   protected JTextField outputField = new JTextField(35);
   private JButton browseInput = new JButton();
   private JButton browseOutput = new JButton();
   private JCheckBox resetIndex = new JCheckBox();
   private JCheckBox resetTiles = new JCheckBox();
   private JTextField labelField = new JTextField(35);
   private String defaultDirectory;
   private String BROWSE;
   private JButton next,seeImg,reset;
   private String help, titlehelp;
   
   private MainPanel mainPanel;
   private Context context;

   public TabDesc(String defaultDir, final MainPanel mainPanel) {
      super(new BorderLayout());
      this.mainPanel = mainPanel;
      context = mainPanel.context;
      createChaine();
      init();
      
      JPanel px;
      JPanel pCenter = new JPanel(new GridBagLayout());
      this.defaultDirectory = defaultDir;

      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(1, 3, 1, 3);
      c.anchor = GridBagConstraints.NORTHWEST;
      c.fill = GridBagConstraints.HORIZONTAL;
      
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
      pCenter.add(inputField, c);
      c.gridx++;
      if( Aladin.aladin.configuration.isLookAndFeelJava() ) pCenter.add(browseInput, c);

      // Répertoire destination
      c.insets.top = 1;
      c.gridy++;
      c.gridx = 0;
      pCenter.add(destLabel, c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPDIRTRGALLSKY")),c);
      c.gridx++;
      pCenter.add(outputField, c);
      c.gridx++;
      if( Aladin.aladin.configuration.isLookAndFeelJava() ) pCenter.add(browseOutput, c);
      
      // Label
      c.insets.bottom = 20;
      c.gridy++;
      c.gridx = 0;
      pCenter.add(labelAllsky, c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPLABELALLSKY")),c);
      c.gridx++;
      pCenter.add(labelField, c);

      // Paramètres avancées
      c.insets.bottom=1;
      c.gridx=0;
      c.gridy++;
      pCenter.add(paramLabel,c);
      c.gridx++;
      pCenter.add(Util.getHelpButton(this, getString("HELPPARAMALLSKY")),c);
      c.gridx++;
      c.gridwidth=2;
      pCenter.add(resetIndex, c);
      c.gridy++;
      pCenter.add(resetTiles, c);
      c.gridy++;
      JPanel pTiles = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      pTiles.setBorder( BorderFactory.createEmptyBorder(0,10,0,0));
      pTiles.add(keepRadio);      //keepRadio.setEnabled(false);
      pTiles.add(overwriteRadio); //overwriteRadio.setEnabled(false);
      pTiles.add(coaddRadio);     //coaddRadio.setEnabled(false);
      pCenter.add(pTiles, c);
      
      if( Aladin.PROTO ) {
         c.gridy++;
         pTiles = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         pTiles.setBorder( BorderFactory.createEmptyBorder(0,10,0,0));
         pTiles.add(keepCellRadio);
         pCenter.add(pTiles, c);
      }
      
      c.gridx=2;
      
      c.gridy++;
      px = new JPanel( new BorderLayout(0,0));
      blankTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            blankCheckbox.setSelected( blankTextField.getText().trim().length()>0 );
         }
      });
      px.add(blankCheckbox,BorderLayout.WEST);
      px.add(blankTextField,BorderLayout.CENTER);
      pCenter.add(px,c); 

      c.gridy++;
      px = new JPanel( new BorderLayout(0,0));
      hduTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            hduCheckbox.setSelected( hduTextField.getText().trim().length()>0 );
         }
      });
      px.add(hduCheckbox,BorderLayout.WEST);
      px.add(hduTextField,BorderLayout.CENTER);
      pCenter.add(px,c); 

      c.gridy++;
      px = new JPanel( new BorderLayout(0,0));
      skyvalTextField.addKeyListener(new KeyAdapter() {
          public void keyReleased(KeyEvent e) {
              skyvalCheckbox.setSelected( skyvalTextField.getText().trim().length()>0 );
          }
      });
      px.add(skyvalCheckbox,BorderLayout.WEST);
      px.add(skyvalTextField,BorderLayout.CENTER);
      pCenter.add(px, c);

      c.gridy++;
      px = new JPanel( new BorderLayout(0,0));
      borderTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            borderCheckbox.setSelected( borderTextField.getText().trim().length()>0 );
         }
      });
      px.add(borderCheckbox,BorderLayout.WEST);
      px.add(borderTextField,BorderLayout.CENTER);
      pCenter.add(px,c); 
      
      c.gridy++;
      px = new JPanel( new BorderLayout(0,0));
      px.add(specifCheckbox,BorderLayout.WEST);
      px.add(specifTextField,BorderLayout.CENTER);
      specifTextField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            specifCheckbox.setSelected( specifTextField.getText().trim().length()>0 );
         }
      });
      pCenter.add(px,c);
      
      if (Aladin.PROTO) {
         c.gridy++;
         final JCheckBox cb1 = frameCheckbox = new JCheckBox("HEALPix in galactic (default is ICRS)", false);
         cb1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               mainPanel.context.setFrame(cb1.isSelected() ? Localisation.GAL : Localisation.ICRS);
            }
         });
         pCenter.add(cb1, c);
      }
     
      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      pBtn.add(seeImg);
      pBtn.add(reset);
      pBtn.add(next);
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
      INDEX_RESET = getString("INDEXRESETALLSKY");
      LABELALLSKY = getString("LABELALLSKY");
      SEE = getString("SEE");
      NEXT = getString("NEXT");
      titlehelp = getString("HHELP");
      INFOALLSKY = getString("INFOALLSKY");
      PARAMALLSKY = getString("PARAMALLSKY");
      COADDALLSKY = getString("COADDALLSKY");
      KEEPALLSKY = getString("KEEPALLSKY");
      KEEPCELLALLSKY = getString("KEEPCELLALLSKY");
      OVERWRITEALLSKY = getString("OVERWRITEALLSKY");
      SPECIFALLSKY  = getString("SPECIFALLSKY");
      BLANKALLSKY  = getString("BLANKALLSKY");
      HDUALLSKY  = getString("HDUALLSKY");
      BORDERALLSKY  = getString("BORDERALLSKY");
      SKYVALALLSKY  = getString("SKYVALALLSKY");
   }
   
   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   public void init() {
      infoLabel = new JLabel(Util.fold(INFOALLSKY,100,true));
      infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
      sourceLabel = new JLabel(REP_SOURCE);
      sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.BOLD));
      destLabel = new JLabel(REP_DEST);
      destLabel.setFont(destLabel.getFont().deriveFont(Font.BOLD));
      inputField.addActionListener(this);
      inputField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            super.keyTyped(e);
            if (!inputField.getText().equals(""))
               actionPerformed(new ActionEvent(inputField, -1, "dirBrowser Action"));
         }
      });

      browseInput.setText(BROWSE);
      browseInput.addActionListener(this);
      outputField.addActionListener(this);
      outputField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            super.keyTyped(e);
            if (!outputField.getText().equals(""))
               actionPerformed(new ActionEvent(outputField, -1, "dirBrowser Action"));
         }
      });
      browseOutput.setText(BROWSE);
      browseOutput.addActionListener(this);
      
      labelAllsky = new JLabel(LABELALLSKY);
      labelAllsky.setFont(labelAllsky.getFont().deriveFont(Font.BOLD));
      
      paramLabel = new JLabel(PARAMALLSKY);
      paramLabel.setFont(paramLabel.getFont().deriveFont(Font.BOLD));
      
      ButtonGroup tilesGroup = new ButtonGroup();
      keepRadio = new JRadioButton(KEEPALLSKY); tilesGroup.add(keepRadio);
      overwriteRadio = new JRadioButton(OVERWRITEALLSKY); tilesGroup.add(overwriteRadio);
      coaddRadio = new JRadioButton(COADDALLSKY); tilesGroup.add(coaddRadio);
      keepCellRadio = new JRadioButton(KEEPCELLALLSKY); tilesGroup.add(keepCellRadio);
      keepRadio.setSelected(true);
      
      specifCheckbox = new JCheckBox(SPECIFALLSKY); specifCheckbox.setSelected(false);
      specifTextField = new JTextField();
      blankCheckbox = new JCheckBox(BLANKALLSKY); blankCheckbox.setSelected(false);
      blankTextField = new JTextField();
      hduCheckbox = new JCheckBox(HDUALLSKY);hduCheckbox.setSelected(false);
      hduTextField = new JTextField();
      borderCheckbox = new JCheckBox(BORDERALLSKY); borderCheckbox.setSelected(false);
      borderTextField = new JTextField();
      skyvalCheckbox = new JRadioButton(SKYVALALLSKY); skyvalCheckbox.setSelected(false);
      skyvalTextField = new JTextField();
      skyvalCheckbox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { resumeWidgets(); }
      });

      resetTiles.setText(REP_DEST_RESET);
      resetTiles.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { resumeWidgets(); }
      });

      resetIndex.setText(INDEX_RESET);
      resetIndex.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { resumeWidgets(); }
      });

      seeImg = new JButton(SEE);
      seeImg.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { loadImgEtalon(); }
      });

      reset = new JButton(getString("RESET"));
      reset.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { mainPanel.clearForms(); }
      });

      next = new JButton(NEXT);
      next.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { mainPanel.showBuildTab(); }
      });

      resetTiles.setSelected(false);
      resetIndex.setSelected(true);
  }
   
   // Chargement dans Aladin de l'image "étalon"
   private void loadImgEtalon() {
      String fileName = context.getImgEtalon();
      if( fileName.endsWith(".hhh") ) fileName=fileName.substring(0,fileName.length()-4)+".jpg";
      mainPanel.aladin.execAsyncCommand("load "+fileName);
   }
   
   public void show() {
      super.show();
      resumeWidgets();
   }
   
   protected void resumeWidgets() {
      try {
         boolean color       = context.isColor();
         boolean dirExist    = context.isExistingDir();
         boolean allskyExist = context.isExistingAllskyDir();
         boolean indexExist  = context.isExistingIndexDir();
         boolean isRunning   = context.isTaskRunning();
         boolean isMap       = context.isMap();
         
         resetTiles.setEnabled(allskyExist && !isRunning);
         resetIndex.setEnabled(indexExist && !isRunning);

         boolean flag = !resetTiles.isSelected() && resetTiles.isEnabled();
         keepRadio.setEnabled(flag);
         keepCellRadio.setEnabled(flag);
         overwriteRadio.setEnabled(flag);
         coaddRadio.setEnabled(flag);
         
         boolean ready = (dirExist || isMap) && outputField.getText().trim().length()>0;
         seeImg.setEnabled(context.getImgEtalon()!=null && !isMap);
         next.setEnabled(ready);
         reset.setEnabled( getInputField().trim().length()>0 );
         blankCheckbox.setEnabled(ready && !isRunning && !color);
         blankTextField.setEnabled(ready && !isRunning && !color);
         hduCheckbox.setEnabled(ready && !isRunning && !color && !isMap);
         hduTextField.setEnabled(ready && !isRunning && !color && !isMap);
         borderCheckbox.setEnabled(ready && !isRunning && !isMap);
         borderTextField.setEnabled(ready && !isRunning && !isMap);
         skyvalCheckbox.setEnabled(ready && !isRunning && !color && !isMap);
         skyvalTextField.setEnabled(ready && !isRunning && !color && !isMap);
         specifCheckbox.setEnabled(ready && !isRunning && !isMap);
         specifTextField.setEnabled(ready && !isRunning && !isMap);
         if( frameCheckbox!=null ) frameCheckbox.setEnabled(ready && !isRunning && !isMap);
         inputField.setEnabled(!isRunning);
         outputField.setEnabled(!isRunning);
         labelField.setEnabled(!isRunning);
         
         if( skyvalCheckbox.isSelected() 
               && skyvalTextField.getText().trim().length()==0 ) skyvalTextField.setText("true");
         
         setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : 
                                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
      } catch( Exception e ) { 
         e.printStackTrace();
      } 
   }

   public void clearForms() {
      inputField.setText("");
      outputField.setText("");
      if( mainPanel!=null ) mainPanel.actionPerformed(new ActionEvent("", -1, "dirBrowser Action"));
      labelField.setText("");
      resetTiles.setSelected(false);
      resetIndex.setSelected(true);
      resumeWidgets();
   }
   
   private void dirBrowser(JTextField dir) {
      String currentDirectoryPath = dir.getText().trim();
      String path = Util.dirBrowser("",currentDirectoryPath,dir,1);
      if( path==null ) return;
      mainPanel.actionPerformed(new ActionEvent(dir, -1, "dirBrowser Action"));
   }
   

   public String getInputField() { return inputField.getText(); }

   public String getOutputField() { return outputField.getText(); }
   public void setOutputField(String output) { outputField.setText(output); }

   public Mode getCoaddModeField() {
      return resetTiles.isSelected() ? Mode.REPLACETILE : 
         keepRadio.isSelected() ? Mode.KEEP : keepCellRadio.isSelected() ? Mode.KEEPTILE
         :overwriteRadio.isSelected() ? Mode.OVERWRITE 
         : coaddRadio.isSelected() ? Mode.AVERAGE : Mode.REPLACETILE;

//      return resetTiles.isSelected() || !resetTiles.isEnabled()? CoAddMode.REPLACETILE : 
//         keepRadio.isSelected() ? CoAddMode.KEEP : keepCellRadio.isSelected() ? CoAddMode.KEEPTILE
//         :overwriteRadio.isSelected() ? CoAddMode.OVERWRITE : CoAddMode.AVERAGE;
   }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == inputField) {
         initTxt();
      } else if (e.getSource() == browseInput) {
         dirBrowser(inputField);
         initTxt();
      }

      if (e.getSource() == outputField) {
         newAllskyDir();
      } else if (e.getSource() == browseOutput) {
         dirBrowser(outputField);
         newAllskyDir();
      }
   }

   /**
    * Itialisation des variables textuelles en fonction du nouveau répertoire
    * SOURCE
    */
   private void initTxt() {
      String txt = inputField.getText();
//      if( !(new File(txt)).isDirectory() ) return;
      int i = txt.lastIndexOf(Util.FS);
      if (i == -1) return;

      // ne traite pas le dernier séparateur
      while (i + 1 == txt.length()) txt = txt.substring(0, i);
      
      // cherche le dernier mot et le met dans le label
      String str = txt.substring(txt.lastIndexOf(Util.FS) + 1);
      labelField.setText(str);

      // rééinitialise le répertoire de destination avec le chemin des données
      // d'entrée
      outputField.setText("");
      newAllskyDir();
   }

   public String getLabelField() {
      return labelField.getText();
   }
   
   public String getMocField() {
      if( !specifCheckbox.isSelected() ) return "";
      return specifTextField.getText();
   }

   public String getBlank() {
      if( !blankCheckbox.isSelected() ) return "";
      return blankTextField.getText();
   }
   
   public String getHDU() {
      if( !hduCheckbox.isSelected() ) return "";
      return hduTextField.getText();
   }
   
   public String getBorderSize() {
      if( !borderCheckbox.isSelected() ) return "0";
      return borderTextField.getText();
   }

   public String getSkyvalField() {
	   if( !skyvalCheckbox.isSelected() ) return null;
	   String s = skyvalTextField.getText().toUpperCase().trim();
	   if( s.length()==0 )  s="true";  // automatic skyval substraction
	   return s;
   }
   
   /**
    * Applique les modifications si le nom du répertoire DESTINATION change
    */
   private void newAllskyDir() {
      String str = outputField.getText();
      // enlève les multiples FS à la fin
      while (str.endsWith(Util.FS)) str = str.substring(0, str.lastIndexOf(Util.FS));

      // si l'entrée est vide, on remet le défaut
      if (str.equals("")) {
         // réinitalise le répertoire SURVEY et l'utilise
         initOutputField();
         mainPanel.newAllskyDir();
         return;
      } 
      // cherche le dernier mot
      Constante.SURVEY = str.substring(str.lastIndexOf(Util.FS) + 1);

      mainPanel.newAllskyDir();
      resumeWidgets();

   }

   private void initOutputField() {
      Constante.SURVEY = getLabelField() + Constante.HIPS;
      String path = inputField.getText();
      // enlève les multiples FS à la fin
      while (path.endsWith(Util.FS)) path = path.substring(0, path.lastIndexOf(Util.FS));

      outputField.setText(path + Constante.HIPS + Util.FS);
   }

   public boolean isResetTiles() {
      return resetTiles.isSelected() && resetTiles.isEnabled();
   }

   public boolean isResetIndex() {
      return resetIndex.isSelected() && resetIndex.isEnabled();
   }

//   public void setResetEnable(boolean enable) {
//      // resetLabel.setEnabled(enable);
//      resetHpx.setEnabled(enable);
//      resetIndex.setEnabled(enable);
//      enableUpdate();
//   }

   public void help() {
      JOptionPane.showMessageDialog(this, help, titlehelp,
            JOptionPane.INFORMATION_MESSAGE);
   }
}
