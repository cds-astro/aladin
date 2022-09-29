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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.allsky.Context;
import cds.allsky.ContextGui;
import cds.tools.Util;

/**
 * Gestion de la fenetre associee a la creation d'un MOC à partir d'une collection d'images
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (fev 2014) Creation
 */
public class FrameMocGenImgs extends FrameMocGenImg {

   protected FrameMocGenImgs(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCGENTITLE");
      INFO  = a.chaine.getString("MOCGENIMGSINFO");
   }
   
   @Override
   protected int getNb() { return 0; }

   private JTextField dirField;
   private JCheckBox strictBox;
   private JCheckBox recBox;
   private JTextField fieldBlank,fieldHDU;
   private JLabel labelBlank,labelHDU;
   private JButton browseButton;
   
   
   protected void reset() {
      dirField.setText("");
      recBox.setSelected(false);
      strictBox.setSelected(false);
      labelBlank.setEnabled(false);
      fieldBlank.setEnabled(false);
      fieldBlank.setText("");
      fieldHDU.setText("");
      super.reset();
   }
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { 
      c.gridwidth=GridBagConstraints.REMAINDER;
      
      JPanel pp = new JPanel();
      pp.add( new JLabel(a.chaine.getString("REPSALLSKY")) );
      pp.add( dirField=new JTextField(29));
      dirField.addKeyListener( new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) {
            JTextField t = (JTextField)e.getSource();
            if( t.getForeground()!=Color.black ) t.setForeground(Color.black);
         }
      });
      pp.add(browseButton=new JButton("Browse"));
      browseButton.setMargin( new Insets(2,4,2,4));
      browseButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { actionBrowse(); }
      });
      
      g.setConstraints(pp,c);
      p.add(pp);
      
      labelHDU = new JLabel("Specifical FITS HDU (e.g 1,3-5|all)");
      fieldHDU=new JTextField(10);
      fieldHDU.addKeyListener( new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) {
            JTextField t = (JTextField)e.getSource();
            if( t.getForeground()!=Color.black ) t.setForeground(Color.black);
         }
      });
      pp = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      pp.add( labelHDU );
      pp.add( fieldHDU );
      pp.add( new JLabel("(first one by default)"));
      g.setConstraints(pp,c);
      p.add(pp);

      
      recBox = new JCheckBox("Scanning sub-directories");
      g.setConstraints(recBox, c);
      p.add(recBox);

      labelBlank = new JLabel("=> Alternative BLANK value");
      fieldBlank=new JTextField(10);
      fieldBlank.addKeyListener( new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyReleased(KeyEvent e) { }
         public void keyPressed(KeyEvent e) {
            JTextField t = (JTextField)e.getSource();
            if( t.getForeground()!=Color.black ) t.setForeground(Color.black);
         }
      });
      strictBox = new JCheckBox("Scanning image pixel values (not just WCS)");
      strictBox.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean flag =  ((JCheckBox)e.getSource()).isSelected();
            fieldBlank.setEnabled(flag);
            labelBlank.setEnabled(flag);
         }
      });
      g.setConstraints(strictBox, c);
      p.add(strictBox);
      
      pp = new JPanel();
      pp.add( labelBlank );
      pp.add( fieldBlank );
      g.setConstraints(pp,c);
      p.add(pp);
      
      reset();
      
   }
   
   private static final String DEFAULT_FILENAME = "-";
   
   private void actionBrowse() {
      FileDialog fd = new FileDialog(a.dialog);

      // (thomas) astuce pour permettre la selection d'un repertoire
      // (c'est pas l'ideal, mais je n'ai pas trouve de moyen plus propre en AWT)
      fd.setFile(DEFAULT_FILENAME);

      fd.show();
      String directory = fd.getDirectory();
      String name =  fd.getFile();
      // si on n'a pas changé le nom, on a selectionne un repertoire
      boolean isDir = false;
      if( name!=null && name.equals(DEFAULT_FILENAME) ) {
         name = "";
         isDir = true;
      }
      if( (name!=null && name.length()>0) || isDir ) dirField.setText(directory);

   }
   
   private boolean getStrict()    { return strictBox.isSelected(); }
   private boolean getRecursive() { return recBox.isSelected(); }
   private double getBlank() throws Exception { 
      if( !getStrict() ) return Double.NaN;
      double x=Double.NaN;
      try {
         String s = fieldBlank.getText().trim();
         if( s.length()==0 || Util.indexOfIgnoreCase(s, "NaN", 0)>=0 ) x=Double.NaN;
         else x = Double.parseDouble( s );
      } catch( Exception e ) {
         fieldBlank.setForeground(Color.red);
         throw e;
      }
      fieldBlank.setForeground(Color.black);
      return x;
   }
   private int [] getHDU() throws Exception {
      int [] hdu = null;
      try {
         String s = fieldHDU.getText().trim();
         hdu = Context.parseHDU(s);
      } catch( Exception e ) {
         fieldHDU.setForeground(Color.red);
         throw e;
      }
      fieldBlank.setForeground(Color.black);
      return hdu;
   }
   
   private String getDir() throws Exception {
      String dir="";
      try {
         dir = dirField.getText().trim();
         if( dir.length()>0 ) if( !(new File(dir).isDirectory()) ) throw new Exception("Not a directory");
      } catch( Exception e ) {
         dirField.setForeground(Color.red);
         throw e;
      }
      dirField.setForeground(Color.black);
      return dir;
   }
   
   @Override
   protected void submit() {
      try {
         String dir = getDir();
         String label = (new File(dir)).getName()+" MOC";
         a.calque.newPlanMocColl(a, label, dir, getOrder() ,getStrict(),getRecursive(),getBlank(),getHDU());
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
      }

   }

   @Override
   protected void adjustWidgets() { };
}
