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

import cds.astro.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

/**
 * Classe gerant l'affichage conjoint d'un champ d'affichage et d'un champ
 * de saisie, précédé d'un Choice contraignant le mode d'affichage.
 * Provient d'un découplage de la classe Localisation
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 22 février 2004 : création
 */
public abstract class MyBox extends JPanel implements MouseListener {
   static protected final int SAISIE    = 0;
   static protected final int AFFICHAGE = 1;

   static private final String LABEL_SAISIE    = "SAISIE";
   static private final String LABEL_AFFICHAGE = "AFFICHAGE";

   // Les fontes associees a la classe
   static protected final int DF = Aladin.SIZE;
   static protected final Font F = Aladin.PLAIN;

   // Le mot affiche lorsque la position n'est pas disponible
   static protected String UNDEF = "";


   protected Aladin aladin;     // Reference
   protected JTextField pos;          // Champ en mode affichage
//   private LCoord pos;
   protected JTextField text;	// Champ en mode saisie
   private int mode = AFFICHAGE;// Mode de l'affichage courant
   private CardLayout cl;
   private JPanel cardPanel;
   protected JComboBox c;  
   protected JLabel label;
   
   static final Font FONT = new Font("Sans serif",Font.BOLD,12);


   protected MyBox(Aladin aladin,String titre) {
      
      this.aladin = aladin;
      
       // Creation du selecteur du repere
      c = createChoice();
      c.setFont(c.getFont().deriveFont((float)c.getFont().getSize()-1));
      c.addMouseListener(this);

      // Creation du label contenant la valeur de la position courant
      pos = new JTextField(30);
      pos.setFont(FONT);
//      pos.setForeground( Color.blue );
      pos.setForeground( Color.gray );
      pos.addMouseListener(this);
 
      // Creation d'un champ de saisie
      text = new JTextField(30);
      text.setFont(FONT);
//      text.setForeground( Color.magenta );
      text.addMouseListener(this);

      cardPanel = new JPanel(cl=new CardLayout(0,0));
      cardPanel.add(LABEL_AFFICHAGE,pos);
      cardPanel.add(LABEL_SAISIE,text);
      
      JPanel p2 = new JPanel(new BorderLayout(0,0) );
      p2.add( label=new Lab("   "+titre),BorderLayout.WEST);
      p2.add( cardPanel,BorderLayout.CENTER);
      JButton b = new JButton(aladin.chaine.getString("CLEAR"));
      b.setMargin(new Insets(0,2,0,2));
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { text.setText(""); text.requestFocusInWindow(); }
      });
      p2.add(b,BorderLayout.EAST);
      
      setLayout(new BorderLayout(3,3));
      add(p2,BorderLayout.CENTER);

      if( !Aladin.OUTREACH ) {
         JPanel p1 = new JPanel( new BorderLayout(0,0));
         p1.add( new Lab("   "+aladin.chaine.getString("FRAME")),BorderLayout.WEST );
         p1.add( c,BorderLayout.EAST );
         add(p1,BorderLayout.EAST);
      }
      
      addMouseListener(this);
   }
   
   public void setEnabled(boolean flag) { 
      text.setEnabled(flag);
      text.setBackground(flag?Color.white : getBackground() );
      if( !flag) text.setText("");
      pos.setBackground(flag?Color.white : getBackground() );
      label.setForeground(flag?Aladin.DARKBLUE:Color.lightGray);
      c.setEnabled(flag);
   }

   public void mouseClicked(MouseEvent e) {}
   public void mousePressed(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) { if( aladin.inHelp ) aladin.help.setText(aladin.chaine.getString("LCoord.HELP")); }
   public void mouseReleased(MouseEvent e) { if( aladin.inHelp ) aladin.helpOff(); }
   public void mouseExited(MouseEvent e) {}
   
   /** Retourne true si le popup est déroulé */
   protected boolean isPopupVisible() { return c.isPopupVisible(); }

   /** Doit être surchargée par les classes filles */
   protected JComboBox createChoice() {
      JComboBox c = new JComboBox();
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent arg0) { actionChoice(); }
      });
      return c;
   }
   
   abstract protected void actionChoice();

   /** Positionnement du texte qui sera affiché en mode de saisie */
   protected void setTextSaisie(String s) {
      text.setText(s);
      if( s.length()>0 ) text.select(0,0);
   }

   /** Positionnement du texte qui sera affiché en mode affichage */
   protected void setTextAffichage(String s) {
      pos.setText(s);
   }
   
   /** Efface les champs de texte */
   protected void reset() {
      text.setText("");
      pos.setText("");
   }

   /** Selection de tout le champ de saisie */
   protected void readyToClear() {  text.selectAll(); }

   /** Recuperation du texte du mode affichage */
   protected String getTextAffichage() { return pos.getText().trim(); };

   /** Recuperation du texte du mode saisie */
   protected String getTextSaisie() { return text.getText().trim(); };

   /** Changement du mode d'affichage */
   protected void setMode(int m) {
      if( mode==m ) return;
      mode=m;
      cl.show(cardPanel,mode==SAISIE?LABEL_SAISIE:LABEL_AFFICHAGE);
    }

  /** Positionne la valeur indefinie en mode affichage*/
   protected void setUndef() { pos.setText(UNDEF); }

   /** Retourne la position du menu deroulant */
   protected int getChoiceIndex() { return c.getSelectedIndex(); }

   /** Positionne le menu deroulant */
   protected void setChoiceIndex(int m) { c.setSelectedIndex(m); }
   
   /** Classe pour un JLabel de taille fixe */
   class Lab extends JLabel {
      Lab(String t) {
         super(t,JLabel.RIGHT);
         setBorder(BorderFactory.createEmptyBorder(0,3,0,2));
         setFont(Aladin.BOLD);
         setForeground(Aladin.DARKBLUE);
      }
      
//      public Dimension getPreferredSize() {
//         return new Dimension(60,super.getPreferredSize().height);
//      }
   }
   
}
