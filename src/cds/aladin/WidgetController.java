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

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

/**
 * Gestionnaire de "Widgets" (JComponent implantant l'interface Widget).
 * Par exemple, permet l'utilisation de la boite de boutons d'Aladin (ToolBox) en superposition d'une vue (ViewSimple).
 *
 * Ce gestionnaire est typiquement utilisé par le "Container" des widgets (un JPanel par exemple) qui doit
 * l'utiliser dans son proper paintComponent, mousePressed, ...
 *
 * @see aladin.view.ViewSimple
 * @see aladin.view.ToolBox
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : jan 2015 - création
 */
public class WidgetController {
   private ArrayList<Widget> widgets;    // Liste des widgets
   private Cursor cursor=null;           // dernier cursor utilisé par le widget courant

   /** Création du controleur de Widgets */
   public WidgetController() {
      widgets = new ArrayList<Widget>();
   }

   /** Ajout d'un widget à gérer */
   public void addWidget(Widget vo) {
      if( widgets.contains(vo)) return;
      widgets.add(vo);
   }

   /** Suppression d'un widget */
   public void removeWidget(Widget vo) {
      widgets.remove(vo);
   }

   /** Retourne le dernier Cursor utilisé par le widget courant */
   public Cursor getCursor() { return cursor; }

   /** Dessins de tous les widgets gérés */
   public void paint(Graphics g) {
      for( Widget vo : widgets) vo.getWidgetControl().paint(g);
   }

   /** Transmission des évenements souris aux widgets concernés
    * @return true si au-moins un widget à utiliser l'évènement
    */
   public boolean mouseMoved(MouseEvent e)    { return mouseAction(e,0); }
   public boolean mouseDragged(MouseEvent e)  { return mouseAction(e,1); }
   public boolean mousePressed(MouseEvent e)  { return mouseAction(e,2); }
   public boolean mouseReleased(MouseEvent e) { return mouseAction(e,3); }
   public boolean mouseWheel(MouseEvent e)    { return mouseAction(e,4); }

   // Gestion des différents évènements souris pour l'ensemble des widgets
   private  boolean mouseAction(MouseEvent e,int action) {
      boolean rep=false;
      WidgetControl voc=null;

      // Y a-t-il un Widget déjà activé ?
      for( Widget vo : widgets ) {
         voc = vo.getWidgetControl();
         if( voc.isActivated(e) ) { rep = mouseActionOne(e,vo,action); break; }
      }

      // Bon alors on va chercher le premier qui est concerné (en partant du dernier)
      if( !rep ) {
         for( int i=widgets.size()-1; i>=0; i-- ) {
            Widget vo = widgets.get(i);
            voc = vo.getWidgetControl();
            if( voc.isMouseIn(e) ) { rep = mouseActionOne(e,vo,action); break; }
         }
      }

      if( rep ) cursor=voc.getCursor();
      return rep;
   }

   // Gestion des différents évènements souris pour un widget particulier
   private boolean mouseActionOne(MouseEvent e,Widget vo, int action) {
      WidgetControl voc = vo.getWidgetControl();
      switch( action ) {
         case 0: voc.mouseMoved(e);     return true;
         case 1: voc.mouseDragged(e); return true;
         case 2: voc.mousePressed(e);   return true;
         case 3:
            int code = voc.mouseReleased(e);
            if( code==WidgetControl.DISPOSE ) widgets.remove(vo);
            else if( code==WidgetControl.UP ) { widgets.remove(vo); widgets.add(vo); }
            return true;
         case 4: voc.mouseWheel((MouseWheelEvent)e); return true;
      }
      return false;
   }
}
