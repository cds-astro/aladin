// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Gestion d'un bouton avec icone
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Mars 2007) Creation
 */
abstract public class MyIcon extends JComponent implements
         MouseMotionListener, MouseListener
         {
   
   protected String DESCRIPTION = null;

   // Gestion de l'icone Split
   protected boolean up=true;      // Vrai si l'icone est up
   protected boolean in=false;     // Vrai si la souris est dessus

   protected Aladin aladin;
   protected int W,H;

  /** Creation */
   protected MyIcon(Aladin aladin,int width,int height) {
      W=width;
      H=height;
      this.aladin=aladin;
      addMouseMotionListener(this);
      addMouseListener(this);
      setBackground( Aladin.COLOR_MAINPANEL_BACKGROUND);
   }
   
   public Dimension getPreferredSize() { return new Dimension(W,H); }
   
   /** Affichage de l'icon du split. */
   protected void drawLogo(Graphics g) {
      g.setColor( Aladin.COLOR_MAINPANEL_BACKGROUND );
      g.fillRect(0, 0, W, H);
   }
   
   protected boolean isAvailable() { return true; }
   protected boolean isActivated() { return false; }
   protected boolean isMouseIn() { return in; }
   
   protected Color getFillInColor() {
      return !isAvailable() ? Aladin.COLOR_MAINPANEL_BACKGROUND
            : Aladin.COLOR_CONTROL_FILL_IN;
   }
   protected Color getLogoColor() {
      boolean isAvailable = isAvailable();
      Color c =  !isAvailable ? Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE :
         isActivated() ? Aladin.COLOR_ICON_ACTIVATED : Aladin.COLOR_CONTROL_FOREGROUND;
      if( isMouseIn() && isAvailable ) c=c.brighter();
      return c;
   }
   protected Color getLabelColor() {
      return isAvailable() ? Aladin.COLOR_CONTROL_FOREGROUND 
            : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE;
   }
   
   /** Recuperation de la chaine de help (une page) */
   abstract protected String Help();

   /** Action a effectuer lorsque l'on clique dessus */
   abstract protected void submit();

   /** Recuperation de la chaine de help (Tooltip) */
   abstract protected String getHelpTip();
 
   protected void in() { up=true; repaint(); }

  /** On relache le bouton du split.
    * On split (resp. unsplit) l'interface
    */
   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) { aladin.helpOff(); return; }
      up=!up;
      repaint();
      submit();
   }
   
  /** On se deplace sur le bouton du split */
   public void mouseMoved(MouseEvent e) {
      if( aladin.inHelp ) return;
      if( DESCRIPTION==null ) DESCRIPTION = getHelpTip();
      Util.toolTip(this,DESCRIPTION,true);
   }

  /** On quitte le bouton du split*/
   public void mouseExited(MouseEvent e) {
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      aladin.status.setText("");
      in=false;
      repaint();
   }

  /** On quitte le bouton du split*/
   public void mouseEntered(MouseEvent e) {
      if( aladin.inHelp ) { aladin.help.setText(Help()); return; }
      Aladin.makeCursor(this, getMyCursor() );
      in = true;
      repaint();
   }
   
   public int getMyCursor() { return Aladin.HANDCURSOR ; }
   
   public void paintComponent(Graphics gr) {
      drawLogo(gr);
   }
   

   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }

}
