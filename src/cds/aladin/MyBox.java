// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextFieldUI;

import cds.tools.Util;


/**
 * Classe gerant l'affichage conjoint d'un champ d'affichage et d'un champ
 * de saisie, suivi d'un Choice contraignant le mode d'affichage.
 * Provient d'un découplage de la classe Localisation
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : sept 2017 : Remplacement de la croix par le JPopupmenu de l'historique
 * @version 1.1 : jan 2012 : Ajout de la classe Text pour la petite croix
 * @version 1.0 : 22 février 2004 : création
 */
public abstract class MyBox extends JPanel {
   static protected final int SAISIE    = 0;
   static protected final int AFFICHAGE = 1;

   static private final String LABEL_SAISIE    = "SAISIE";
   static private final String LABEL_AFFICHAGE = "AFFICHAGE";

   // Le mot affiche lorsque la position n'est pas disponible
   static protected String UNDEF = "";


   protected Aladin aladin;     // Reference
   protected Text pos;          // Champ en mode affichage
   protected Text text;	// Champ en mode saisie
   private int mode = AFFICHAGE;// Mode de l'affichage courant
   private CardLayout cl;
   private JPanel cardPanel;
   protected JComboBox c;
   protected JLabel label;

   protected MyBox() { super(); }

   protected MyBox(Aladin aladin,String titre) {

      this.aladin = aladin;
      
      // Creation du selecteur du repere
      c = createChoice();

      // Creation du label contenant la valeur de la position courant
      pos = new Text("",30);
      pos.setFont(Aladin.PLAIN);
      pos.setBackground( Aladin.COLOR_TEXT_BACKGROUND );
      pos.setForeground( Aladin.COLOR_TEXT_FOREGROUND_INFO);

      // Creation d'un champ de saisie
      text = new Text("",30);
      text.setFont(Aladin.PLAIN);
      text.setBackground( Aladin.COLOR_TEXT_BACKGROUND );
      text.setForeground( Aladin.COLOR_TEXT_FOREGROUND );

      cardPanel = new JPanel(cl=new CardLayout(0,0));
      cardPanel.add(LABEL_AFFICHAGE,pos);
      cardPanel.add(LABEL_SAISIE,text);
      cardPanel.setBackground( aladin.getBackground());

      setLayout(new BorderLayout(0,0));
      setBackground( aladin.getBackground() );
      
      label=new Lab(titre);
      add( label,BorderLayout.WEST );
      add( cardPanel, BorderLayout.CENTER );

//      if( !Aladin.OUTREACH ) {
         JPanel p1 = new JPanel( new BorderLayout(0,0));
         p1.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
         p1.add( new Lab(aladin.chaine.getString("FRAME")+" "),BorderLayout.WEST );
         
         GridBagLayout g;
         JPanel pCombo = new JPanel( g=new GridBagLayout() );
         pCombo.setBackground( aladin.getBackground() );
         GridBagConstraints gc = new GridBagConstraints();
         gc.fill = GridBagConstraints.HORIZONTAL;
         pCombo.add(c,gc);
         
         p1.add( pCombo,BorderLayout.CENTER );
         p1.setBackground( aladin.getBackground());
         add(p1,BorderLayout.EAST);
//      }

      setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
   }
   
   protected JComboBox getComboBox() { return c; }

   public void setEnabled(boolean flag) {
      text.setEnabled(flag);
      text.setBackground(flag?Color.white : getBackground() );
      if( !flag) text.setText("");
      pos.setBackground(flag?Color.white : getBackground() );
      label.setForeground(flag?Aladin.COLOR_LABEL:Color.lightGray);
      c.setEnabled(flag);
   }

   /** Retourne true si le popup est déroulé */
   protected boolean isPopupVisible() { return c.isPopupVisible(); }

   /** Doit être surchargée par les classes filles */
   protected JComboBox createChoice() {
      JComboBox c = new JComboBox();
      c.setUI( new MyComboBoxUI());
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
   
   /** Action à opérer lorsque l'on clique sur le triangle au bout du champ de saisie */
   protected void triangleAction(int x) {}
   
   /** Action à opérer lorsque l'on clique sur la croix => effacement du champ */
   protected void crossAction() { text.setText(""); }
   
   /** Retourne true s'il faut afficher un petit triangle au bout du champ de saisie */
   protected boolean hasTriangle() { return false; }
   
   /** Retourne true s'il faut afficher la petite croix permettant d'effacer le champ */
   protected boolean hasCross() { return text.getText().length()>0 ; }

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
   
   
   // Uniquement utilisé pour pouvoir transmettre à l'objet qui la surchage le fait que les flèches
   // UP ou DOWN ont été tapées.
   protected void sendKey(KeyEvent e) { };

   /** Classe pour un JLabel de taille fixe */
   class Lab extends JLabel {
      Lab(String t) {
         super(t,JLabel.RIGHT);
         setBorder(BorderFactory.createEmptyBorder(0,3,0,2));
         setFont(Aladin.BOLD);
         setForeground(Aladin.COLOR_LABEL);
      }
   }
   
   static private int POSCROSS;
   
   /** Classe pour un JTextField avec reset en bout de champ (petite croix rouge) */
   class Text extends JTextField implements MouseMotionListener, MouseListener {
//      private Dimension dim=null;
      private Rectangle region=null;
      private Rectangle regionCross=null;
      private Color colorTriangle = Color.darkGray;
      
      int w;

      Text(String t,int width) {
         super(t,width);
         setUI( new BasicTextFieldUI() );
         addMouseMotionListener(this);
         w = Math.round( 10*aladin.getUIScale());
         POSCROSS = 25+w;
         addMouseListener(this);
         
         // Pour ne pas passer sous la petite croix et le triangle
         // au bout du champ
         Insets i = getMargin();
         i.right=POSCROSS+1;
         setMargin(i);
      }

      boolean in(int x,int y) { return region!=null && x>=region.x;  }
      boolean inCross(int x,int y) { return regionCross!=null && x>=regionCross.x && x<=regionCross.x+regionCross.width;  }

      public void paintComponent(Graphics g) {
         
         try {
            super.paintComponent(g);
            if( hasCross() ) {
               g.setColor( getBackground() );
               g.fillRect(getWidth()-POSCROSS,0,getWidth()-16,getHeight());
               drawCross(g,getWidth()-(POSCROSS-2),getHeight()/2-4);
            }
            if( hasTriangle() ) {
               g.setColor( getBackground() );
               g.fillRect(getWidth()-POSCROSS+14,0,getWidth()-1,getHeight());
               drawTriangle(g,getWidth()-POSCROSS+15,getHeight()/2-2);
            }
         } catch( Exception e ) { }
      }

      private void drawCross(Graphics g, int x, int y) {
         g.setColor( colorTriangle );
         Util.drawCross(g,x,y,w-4);
         regionCross = new Rectangle(x-2,y-2,w,w);
      }

      private void drawTriangle(Graphics g, int x, int y) {
         g.setColor( colorTriangle );
         if( aladin.getUIScale()<1.25 ) Util.fillTriangle7(g, x, y);
         else Util.fillTriangle8(g, x, y);
         region = new Rectangle(x-2,y-2,w,w);
      }
      
      protected void processComponentKeyEvent(KeyEvent e) {
         int key = e.getKeyCode();
         if( e.getID()==KeyEvent.KEY_PRESSED && (key==KeyEvent.VK_UP || key==KeyEvent.VK_DOWN || key==KeyEvent.VK_PAGE_DOWN ) ) sendKey(e);
         else super.processComponentKeyEvent(e);
      }
      
      private Cursor cursor = null;
      public void mouseDragged(MouseEvent e) { }
      public void mouseMoved(MouseEvent e) {
         Cursor nc;
         if( text.in(e.getX(),e.getY()) ) nc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
         else if( text.inCross(e.getX(),e.getY()) ) nc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
         else nc = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
         if( nc.equals(cursor) ) return;
         setCursor(nc);
         cursor=nc;
      }

      public void mouseClicked(MouseEvent e) {}
      public void mousePressed(MouseEvent e) {}
      public void mouseEntered(MouseEvent e) {
         if( aladin.inHelp ) aladin.help.setText(aladin.chaine.getString("LCoord.HELP"));
      }
      public void mouseReleased(MouseEvent e) {
         if( aladin.inHelp ) aladin.helpOff();
         if( text.in(e.getX(),e.getY()) ) triangleAction( e.getX() );
         if( text.inCross(e.getX(),e.getY()) ) crossAction( );
      }
      public void mouseExited(MouseEvent e) {}




   }

}
