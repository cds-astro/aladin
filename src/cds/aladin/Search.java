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


package cds.aladin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.tools.Util;

/**
 * Panel de gestion du search dans les mesures
 * @author Pierre Fernique [CDS]
 * @version 1.0 : Mars 2007 creation
 */
public final class Search extends JPanel implements MouseListener {
   
   Aladin aladin;
   SearchText text;
   JLabel label;
   JButton left,right,go,out,reduce;
   protected JPanel panelSearch;
   
   static String MFSEARCHIN=null,MFSEARCHOUT,MFSEARCHNO,YOURSEARCH;

   protected Search(Aladin aladin,boolean withReduceButton) {
      this.aladin = aladin;
      if( MFSEARCHIN==null ) {
         MFSEARCHIN = aladin.chaine.getString("MFSEARCHIN");
         MFSEARCHOUT = aladin.chaine.getString("VWNIF");
         MFSEARCHNO = aladin.chaine.getString("MFSEARCHNO");
         YOURSEARCH = aladin.chaine.getString("MFSEARCHFOCUS");
      }
      JButton b;
      left = b = new JButton(new ImageIcon(aladin.getImagette("Left.gif")));
      b.setMargin(new Insets(0,0,0,0));
      b.setBorderPainted(false);
      b.setContentAreaFilled(false);
      b.setToolTipText(aladin.chaine.getString("MFSEARCHLEFT"));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { down(); }
      });
      
      right = b = new JButton(new ImageIcon(aladin.getImagette("Right.gif")));
      b.setMargin(new Insets(0,0,0,0));
      b.setBorderPainted(false);
      b.setContentAreaFilled(false);
      b.setToolTipText(aladin.chaine.getString("MFSEARCHRIGHT"));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { up(); }
      });
      
      go = b = new JButton(new ImageIcon(aladin.getImagette("Go.gif")));
      b.setMargin(new Insets(0,0,0,0));
      b.setBorderPainted(false);
      b.setContentAreaFilled(false);
      b.setToolTipText(aladin.chaine.getString("MFSEARCHGO"));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { go(); }
      });
      
      if( withReduceButton ) {
         reduce = b = new JButton(new ImageIcon(aladin.getImagette("Agrandir.gif")));
         b.setMargin(new Insets(0,0,0,0));
         b.setBorderPainted(false);
         b.setContentAreaFilled(false);
         b.setToolTipText(aladin.chaine.getString("REDUCEH"));
         b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { reduce(); }
         });
      }
      
      out = b = new JButton(new ImageIcon(aladin.getImagette("Cross.gif")));
      b.setMargin(new Insets(0,0,0,0));
      b.setBorderPainted(false);
      b.setContentAreaFilled(false);
      b.setToolTipText(aladin.chaine.getString("SPLITH"));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { split(); }
      });
      
      text = new SearchText();
      text.setFont(Aladin.BOLD);
      text.setToolTipText(aladin.chaine.getString("MFSEARCHEX"));
      text.addMouseListener(this);
      
      setLayout( new BorderLayout(0,0) );
      JPanel searchPanel = new JPanel( new BorderLayout(0,0) );
      searchPanel.add(label=Aladin.createLabel(aladin.chaine.getString("MFSEARCHL")),"West");
      label.setToolTipText(aladin.chaine.getString("MFSEARCHHELP"));
      searchPanel.add(text,"Center");
      
      panelSearch = new JPanel( new BorderLayout(0,0) );
      
      JPanel searchControlPanel = new JPanel( new BorderLayout(0,0) );
      if( !Aladin.OUTREACH ) searchControlPanel.add(go,"West");
      searchControlPanel.add(left,"Center");
      searchControlPanel.add(right,"East");
      
      JPanel genericSearchPanel = new JPanel( new BorderLayout(0,0) );
      if( !Aladin.OUTREACH ) genericSearchPanel.add(searchPanel,"West");
      genericSearchPanel.add(searchControlPanel,"Center");
      
      panelSearch.add(genericSearchPanel,"West");
      if( !Aladin.OUTREACH ) panelSearch.add(new JLabel("    "),"Center");
      
      JPanel buttonPanel = new JPanel( new BorderLayout(0,0) );
      if( reduce!=null ) buttonPanel.add(reduce,"West");
      if( !Aladin.OUTREACH ) buttonPanel.add(out,"East");
      
      add(panelSearch,"Center");
      add(buttonPanel,"East");
      
      setEnabled(false);
      addMouseListener(this);
   }
   
   private boolean flagHideSearch=false;
   
   /** Permet de cacher/montrer les widgets propre à la recherche en ne laissant que les boutons "reduce" et "out" */
   protected void hideSearch(boolean flag) {
      if( flag==flagHideSearch ) return;
      flagHideSearch=flag;
      if( flagHideSearch ) remove(panelSearch);
      else add(panelSearch,"Center");
      validate();
   }
   
   private void down() {
//      if( text.getText().trim().length()==0 ) return;
      if( aladin.mesure.nbSrc==0 ) text.execute(KeyEvent.VK_ENTER,null,0);
      else text.execute(KeyEvent.VK_DOWN,null,0);
   }
   
   private void up() {
//      if( text.getText().trim().length()==0 ) return;
      if( aladin.mesure.nbSrc==0 ) text.execute(KeyEvent.VK_ENTER,null,0);
      else text.execute(KeyEvent.VK_UP,null,0);
   }

   private void go() {
      text.execute(KeyEvent.VK_ENTER,null,0);
   }
   
   private void split() {
      aladin.mesure.split();
      setIcon();
   }
   
   protected void reduce() {
      aladin.mesure.switchReduced();
      setIcon();
   }
   
   protected void setIcon() {
      if( reduce==null ) return;
      if( aladin.mesure.flagReduced || aladin.mesure.f!=null ) reduce.setIcon(new ImageIcon(aladin.getImagette("Agrandir.gif")));
      else reduce.setIcon(new ImageIcon(aladin.getImagette("Reduire.gif")));
   }

   
   static String SELECT,UNSELECT,APPEND,SHOW;
   
   private int getDefaultMode() {
//      int i=methodChoice.getSelectedIndex();
//      return i==0 ? 0 : i==1 ? -1 : i==2 ? 1 : 2;
      return 0;
   }
   
   /** Fait clignoter le search pour attirer l'attention
    * de l'utilisateur et demande le focus sur le champ de saisie */
   protected void focus() {
      text.setText(YOURSEARCH);
      (new Thread() {
         Color def = text.getBackground();
         Color deff = text.getForeground();
         public void run() {
            for( int i=0; i<3; i++ ) {
               text.setBackground(Color.green);
               text.setForeground(Color.black);
               Util.pause(200);
               text.setBackground(def);
               text.setForeground(deff);
               Util.pause(200);
            }
            text.setText("");
            text.requestFocusInWindow();
         }
      }).start();
   }
   
   private Insets INSETS = new Insets(1,0,3,0);
   public Insets getInsets() { return INSETS; }
   
   void setText(String s) { text.setText(s);}
   public String getText() { return text.getText(); }
   
   boolean oEnable=true;
   public void setEnabled(boolean flag) { 
      boolean x=flag;
      if(aladin.mesure!=null ) x=aladin.mesure.nbSrc>0;
      right.setEnabled(x);
      left.setEnabled(x);
      go.setEnabled(text.searchChanged());
      
      if( flag==oEnable ) return;
      oEnable=flag;
      text.setEnabled(flag);
      setColor( flag ? DEFAULT : DISABLE);
      label.setForeground(flag?Aladin.DARKBLUE:Color.lightGray);
      if(aladin.mesure!=null ) flag=aladin.mesure.nbSrc>0;
      right.setEnabled(flag);
      left.setEnabled(flag);
   }
   
   
   static final int DEFAULT = 0;
   static final int IN  = 1;
   static final int OUT = 2;
   static final int NO  = 3;
   static final int DISABLE = 4;
   
   protected void setColor(int mode) {
      switch(mode) {
         case IN:  text.setBackground( Color.white);
                   text.setForeground( Color.magenta);
                   break;
         case NO: text.setBackground( Color.red);
                   text.setForeground( Color.white);
                   break;
         case OUT: text.setBackground( Color.orange);
                   text.setForeground( Color.magenta);
                   break;
         case DISABLE: text.setBackground( getBackground() );
                       text.setForeground( getBackground() );
                   break;
        default: text.setBackground( Color.white);
                 text.setForeground( Color.magenta);         
      }
   }
   
   protected String Help() { return aladin.chaine.getString("Search.HELP"); }
   
   public void mouseEntered(MouseEvent e) {
      if( aladin.inHelp ) { aladin.help.setText(Help()); return; }
   }
      
   /** Execution d'une recherche générique */
   void execute(String s) {
      if( s==null ) s=text.getText();
      int mode = s.equals("+") ? 1 : s.equals("-") ? -1 : 0;
      switch(mode) {
         case -1: text.execute(KeyEvent.VK_UP,s,0);    break;
         case  1: text.execute(KeyEvent.VK_DOWN,s,0);  break;
         case  0: text.execute(KeyEvent.VK_ENTER,s,0); break;
      }
   }
   
   public void mouseClicked(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) {
      if( !oEnable ) return;
      setColor(DEFAULT);
   }
   public void mouseReleased(MouseEvent e) { }

   
   /** Juste pour pouvoir redéfinir qq trucs */
   class SearchText extends JTextField implements KeyListener,MouseWheelListener {
      private Dimension DIM = new Dimension(80,10);
      protected String previousSearch="";
      protected int oNbSrc=0;
      
      SearchText() {
         super();
         addKeyListener(this);
         addMouseWheelListener(this);
      }
                  
      public Dimension getPreferredSize() { return DIM; }

      public void mouseWheelMoved(MouseWheelEvent e) {
         int mode = e.getWheelRotation();
         setColorAndStatus( aladin.mesure.searchString(getText(),mode) );
      }
      
      public void keyReleased(KeyEvent e) {
         execute(e.getKeyCode(),getText(),getDefaultMode());
      }
      
      public boolean searchChanged() {
         try {
            return aladin.mesure.nbSrc!=oNbSrc || !previousSearch.equals(getText());
         }catch( Exception e ) { return false; }
      }
               
      public void execute(int keyCode,String s,int flagAdd) {
         if( s==null ) s=getText();
         go.setEnabled(searchChanged());
         if( keyCode==KeyEvent.VK_ENTER && flagAdd!=2 ) {
            previousSearch=s;
            // L'expression commence par "-", il s'agit d'une déselection de sources
            // L'expression commence par "+", il s'agit d'un ajout
            if( s.length()>0 && (s.charAt(0)=='-' || s.charAt(0)=='+') ) {
               flagAdd=s.charAt(0)=='-' ? -1 : 1;
               s=s.substring(1);
               text.setText(s);
            }
            boolean rep = aladin.mesure.selectByString(s,flagAdd);
            setColor( rep ? DEFAULT : NO);
            go.setEnabled(false);
            right.setEnabled(rep);
            left.setEnabled(rep);
            text.selectAll();
            oNbSrc = aladin.mesure.nbSrc;
            return;
         }
         int mode =  keyCode==KeyEvent.VK_UP ? -1
                   : keyCode==KeyEvent.VK_DOWN || keyCode==KeyEvent.VK_ENTER? 1
                   : 0;
         if( mode!=0 ) {
            text.setText(s);
            setColorAndStatus( aladin.mesure.searchString(s,mode) );
            text.selectAll();
         }
      }

      public void keyPressed(KeyEvent e) { setColor(DEFAULT); }
      public void keyTyped(KeyEvent e) { }
   }
   
   void setColorAndStatus(int rep ) {
      setColor( rep==0 ? NO : rep==1 ? IN : OUT );
      if( rep==0 ) aladin.mesure.setStatus(MFSEARCHNO);
      else if( rep==-1 ) aladin.mesure.setStatus(MFSEARCHOUT);
      else aladin.mesure.setStatus(MFSEARCHIN);
   }
   
}
