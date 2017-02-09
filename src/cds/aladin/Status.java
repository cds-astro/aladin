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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

/**
 * Gestion de la fenetre d'affichage du statut (en bas).
 * Le texte est par defaut centre, a moins qu'il ne commence par le
 * caractere '<', dans ce cas il est aligne a gauche
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : (29 oct 04) Nettoyage systématique du cadre
 * @version 1.1 : (28 mars 00) ReToilettage du code
 * @version 1.0 : (10 mai 99)  Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Status extends JComponent implements MouseListener {

   // Les constantes
   protected int W = 50;	      // Largeur
   protected int H = 26;	      // Hauteur par defaut
   static protected int y=-2;	      // Ordonnee pour les messages
   static protected final Color BG = Aladin.BLUE;

   // Les elements de l'objet
   protected String text;               // Le texte du status
   private   String lastText=null;      // Le dernier texte affiché

   // Les references aux autres objets
   protected Aladin aladin;

  /** Creation du statut.
   * @param aladin Reference
   * @param text   Le premier texte a afficher
   */
   protected Status(Aladin a,String s) {
      text = s;
      aladin = a;
   }

   /** Recuperation du texte */
   protected String getText() { return text; }

  /** Modification du texte de status
   * @param text Le nouveau texte du statut
   */
   protected void setText(String s) {
      if( s==null ) s="";
      
      if( s.length()>0 && s.charAt(0)=='!' ) {
         foreGround=Color.red;
         s = s.substring(1);
      } else foreGround=aladin.COLOR_BLUE;
      
      if( lastText!=null && lastText.equals(s) ) return;
      if( s.length()==0 &&  aladin.dialog!=null && !aladin.command.isSync() ) s=aladin.chaine.getString("SEESTACK"); 
      text = s;
      repaint();
   }
   
   public Dimension getPreferredSize() { return new Dimension(W,H); }
   
   private Color foreGround = Aladin.COLOR_BLUE;
   
   public void paintComponent(Graphics g) {
      
      super.paintComponent(g);
      
      aladin.setAliasing(g);
      g.setColor( getBackground() );
      g.fillRect(0,0,W,H);
      g.setColor( foreGround );
      FontMetrics m = g.getFontMetrics();

      // Le status
      if( text!=null && !text.equals("") ) {
         if( y<0 ) y = H/2+(m.getDescent()+m.getAscent())/2-4;
         int x = getSize().width/2-m.stringWidth(text)/2;
         if( x<1 ) x=5;			// Decale a gauche force
         if( text.charAt(0)=='<') {   	// Decale a gauche
            text=text.substring(1);
            x=5;
         }
         g.drawString(text,x,y);
      }
      lastText=text;
   }

  /** Gestion du Help */
   protected String Help() { return aladin.chaine.getString("Status.HELP"); }

   public void mouseClicked(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) {
      if( aladin.inHelp ) aladin.help.setText(Help());
   }
   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) aladin.helpOff();
   }


}
