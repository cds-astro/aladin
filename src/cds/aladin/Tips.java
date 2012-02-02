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
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;


/**
 * Le label pour le Copyright et les TIPS
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (fév 2007) création
 */
public final class Tips extends MyLabel implements MouseListener {
      
  /** Creation du label pour le copyright et les TIPS */
   protected Tips(String text) {
      super(text,Label.LEFT,Aladin.SPLAIN);
      addMouseListener(this);
   }
          
   private int tipCpt=0;
   private String cTips=null;            // Le tips courant
   static private final int TIPMAX=5;    // Nombre de fois où le message change avant d'afficher un TIPS
   static private Random random = new Random(System.currentTimeMillis());   
   static private Vector TIPS = null;    // Les TIPS
   
   /** Chargement des TIPS */
   private void loadTips() {
      TIPS = new Vector(100);
      String s;
      for( int i=0; true; i++ ) {
         s = Aladin.aladin.chaine.getString("TIP"+i);
         if( s==null ) break;
         TIPS.add(s);
      }
   }
   
   /** Remplacement du Copyright par un TIPS de temps en temps */
   protected String pubNews(String text) {
      if( text!=Aladin.COPYRIGHT ) return text;
//      synchronized ( this ) {
         if( TIPS==null ) loadTips();
         tipCpt++;
         if( tipCpt>=1.5*TIPMAX ) { tipCpt=0; cTips=null; }
         if( tipCpt<TIPMAX ) return text;
         if( cTips==null )  cTips = getNextTips();
//      }
      return cTips;
   }
   
   protected String getNextTips() {
      int n = random.nextInt(TIPS.size());
      return "TIP: "+TIPS.elementAt(n);
   }
   
   /** Changement de la couleur si c'est un TIPS */
   public Color getForeground() {
      if( cTips!=null ) return Aladin.GREEN;
      return super.getForeground();
   }
   
   public void mouseClicked(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }

   public void mouseEntered(MouseEvent e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   }

   public void mouseExited(MouseEvent e) {
      setCursor(Cursor.getDefaultCursor());
   }

   public void mousePressed(MouseEvent e) {
      tipCpt=TIPMAX+1;
      cTips=null;
      setText(Aladin.COPYRIGHT);
   }
   
}
