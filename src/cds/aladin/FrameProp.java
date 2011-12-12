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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cds.aladin.prop.PropAction;
import cds.aladin.prop.PropPanel;
import cds.aladin.prop.Propable;
import cds.tools.Util;

/** Gère le frame associé aux propriétés d'un objet
 * @date déc 2011 - création
 * @author Pierre Fernique [CDS]
 */
public class FrameProp extends JFrame implements ActionListener {
   private String apply,close;
   private Aladin aladin;
   private Propable obj=null;
   private PropPanel propPanel;
   
   public FrameProp(Aladin aladin, Propable obj) {
      super("Individual object properties");
      this.aladin = aladin;
      Aladin.setIcon(this);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false,aladin);

      apply = aladin.chaine.getString("UPAPPLY");
      close = aladin.chaine.getString("UPCLOSE");
      
      JPanel container = (JPanel)getContentPane();
      container.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
      container.add( getValidPanel(), BorderLayout.SOUTH);
      setLocation(Aladin.computeLocation(this));
      
      updateAndShow(obj);
   }
   
   public void updateAndShow(Propable o) {
      if( o!=obj ) {
         obj=o;
         JPanel container = (JPanel)getContentPane();
         if( propPanel!=null ) container.remove(propPanel);
         propPanel = new PropPanel(this, obj);
         container.add( propPanel , BorderLayout.CENTER);
         pack();
      }
      resume();
      setVisible(true);
   }
   
   public void resume() {
      if( propPanel==null ) return;
      propPanel.resume();
   }

   /** Construction du panel des boutons de validation
    * @return Le panel contenant les boutons Apply/Close */
    private JPanel getValidPanel() {
       JPanel p = new JPanel();
       p.setLayout( new FlowLayout(FlowLayout.CENTER));
       JButton b;
       p.add( b=new JButton(apply));
       b.addActionListener(this);
       b.setFont(b.getFont().deriveFont(Font.BOLD));
       p.add( b=new JButton(close));
       b.addActionListener(this);
       return p;
    }
    
    public void actionPerformed(ActionEvent evt) {
       Object src = evt.getSource();
       String what = src instanceof JButton ? ((JButton)src).getActionCommand() : "";

       if( close.equals(what) ) dispose();
       else if( apply.equals(what) ) {
          try { 
             System.out.println("actionPerformed apply on "+obj);
             if( propPanel.apply()==PropAction.SUCCESS ) {
                aladin.view.newView(1);
                aladin.calque.repaintAll();
             }
          } catch( Exception e ) { Aladin.warning(this," "+e.getMessage(),1); }
       }
    }
}


