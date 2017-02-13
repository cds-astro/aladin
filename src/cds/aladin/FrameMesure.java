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

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Gestion de la fenetre "externe" associee aux mesures
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (22 fevrier 2002) Creation
 */
public final class FrameMesure extends JFrame  {

   // Les references aux objets
   Aladin a;

  /** Creation du Frame gerant les mesures lorsqu'elles sont dans une fenêtre externe
   * @param aladin Reference
   */
   protected FrameMesure(Aladin aladin) {
      super("Aladin Java measurements frame");
      this.a = aladin;
      Aladin.setIcon(this);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { close(); }
         }, 
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
      );
	  // pour le bug sous KDE
//	  super.show();
	  // test (bug KDE) , le move est effectué entre un show() et un hide()
      if( !Aladin.LSCREEN ) setLocation(200,300);
      else setLocation(200,400);
//	  super.hide();

//      a.mesurePanel.remove(a.mesure);
      if( a.splitMesureHeight.getBottomComponent()!=null ) aladin.splitMesureHeight.remove(a.mesure);
      a.mesure.scrollV.setValue(0);
      a.validate();
      a.f.validate(); // pour maj frame principale sous Mac
      a.repaint();
      
      JPanel p = (JPanel)getContentPane();
      p.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
      p.setBackground( a.getBackground() );
      p.add(a.mesure,"Center");
      a.mesure.split(true);
      pack();
      setVisible(true);
   }
   
   public Dimension getPreferredSize() {
      return new Dimension(800,512);
   }
   
   protected void close() {
      remove(a.mesure);
      dispose();
      a.mesure.split(false);
//      Aladin.makeAdd(a.mesurePanel,a.mesure,"Center");
      if( a.splitMesureHeight.getBottomComponent()==null ) a.splitMesureHeight.setBottomComponent(a.mesure);
      a.mesure.setPreferredSize(new Dimension(100,150));
      a.mesure.setMinimumSize(new Dimension(100,0));
      a.mesure.setReduced(false);
      if( !Aladin.OUTREACH ) a.search.hideSearch(false);
      a.getContentPane().validate();
//      a.search.setIcon();
      a.f.validate(); // pour maj frame principale sous Mac
      a.getContentPane().repaint();
//      a.split.in();
   }
   
   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) close();
      super.processWindowEvent(e);
   }
}
