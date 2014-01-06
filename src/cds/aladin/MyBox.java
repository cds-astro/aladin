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
import java.awt.event.*;

import javax.swing.*;

/**
 * Classe gerant l'affichage conjoint d'un champ d'affichage et d'un champ
 * de saisie, précédé d'un Choice contraignant le mode d'affichage.
 * Provient d'un découplage de la classe Localisation
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : jan 2012 : Ajout de la classe Text pour la petite croix
 * @version 1.0 : 22 février 2004 : création
 */
public abstract class MyBox extends JPanel implements MouseListener,MouseMotionListener {
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
   protected Text pos;          // Champ en mode affichage
//   private LCoord pos;
   protected Text text;	// Champ en mode saisie
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
      pos = new Text("",30);
      pos.setFont(FONT);
      pos.setForeground( Color.gray );
      pos.addMouseListener(this);
 
      // Creation d'un champ de saisie
      text = new Text("",30);
      text.setFont(FONT);
      text.setForeground( Aladin.MYBLUE );
      text.addMouseListener(this);
      text.addMouseMotionListener(this);

      cardPanel = new JPanel(cl=new CardLayout(0,0));
      cardPanel.add(LABEL_AFFICHAGE,pos);
      cardPanel.add(LABEL_SAISIE,text);
      
      JPanel p2 = new JPanel(new BorderLayout(0,0) );
      p2.add( label=new Lab(titre),BorderLayout.WEST);
      p2.add( cardPanel,BorderLayout.CENTER);
      
      setLayout(new BorderLayout(3,3));
      add(p2,BorderLayout.CENTER);
      
      if( !Aladin.OUTREACH ) {
         JPanel p1 = new JPanel( new BorderLayout(0,0));
         p1.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
         p1.add( new Lab(aladin.chaine.getString("FRAME")),BorderLayout.WEST );
         p1.add( c,BorderLayout.EAST );
         add(p1,BorderLayout.EAST);
      }
      
      addMouseListener(this);
      setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
   }
   
   public void setEnabled(boolean flag) { 
      text.setEnabled(flag);
      text.setBackground(flag?Color.white : getBackground() );
      if( !flag) text.setText("");
      pos.setBackground(flag?Color.white : getBackground() );
      label.setForeground(flag?Aladin.DARKBLUE:Color.lightGray);
      c.setEnabled(flag);
   }
   
   public void mouseDragged(MouseEvent e) { }
   public void mouseMoved(MouseEvent e) {
      Cursor nc,c = text.getCursor();
      if( text.in(e.getX(),e.getY()) )  nc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      else nc = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
      if( nc.equals(c) ) return;
      text.setCursor(nc);
   }

   public void mouseClicked(MouseEvent e) {}
   public void mousePressed(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) { if( aladin.inHelp ) aladin.help.setText(aladin.chaine.getString("LCoord.HELP")); }
   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) aladin.helpOff();
      if( text.in(e.getX(),e.getY()) ) reset();
   }
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
   
   /** Classe pour un JTextField avec reset en bout de champ (petite croix rouge) */
   class Text extends JTextField {
      private Dimension dim=null;
      private Rectangle cross=null;
      
      Text(String t,int width) {
         super(t,width);
         dim = new Dimension(width,super.getPreferredSize().height);
      }
      
      public Dimension getPreferredSize() { return dim; }
      
      boolean in(int x,int y) {
         if( cross==null || text.getText().length()==0) return false;
//         return cross.contains(x,y);
         return x>=cross.x;
      }
      
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         drawCross(g,getWidth()-X-8,getHeight()/2-X/2);
      }
      
      static private final int X = 6;
      private void drawCross(Graphics g, int x, int y) {
         g.setColor(Color.white);
//         g.fillRect(x-3, y-6, dim.height, dim.height);
         g.fillOval(x-3, y-6, dim.height, dim.height);
         g.setColor( text.getText().length()>0 ? Color.red.darker() : Color.gray );
         g.drawLine(x,y,x+X,y+X);
         g.drawLine(x+1,y,x+X+1,y+X);
         g.drawLine(x+2,y,x+X+2,y+X);
         g.drawLine(x+X,y,x,y+X);
         g.drawLine(x+X+1,y,x+1,y+X);
         g.drawLine(x+X+2,y,x+2,y+X);
         cross = new Rectangle(x,y,X,X);
      }
      
      
   }
   
}
