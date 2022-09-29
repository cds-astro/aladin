// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Label;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * Le label pour le Copyright et les TIPS
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (fév 2007) création
 */
public final class Tips extends MyLabel implements MouseListener {
      
//   static private final int TIPMAX=3;    // Nombre de fois où le message change avant d'afficher un TIPS

//   private Aladin aladin;
//   private long start;
//   private int tipCpt=0;
   private String cTips=null;            // Le tips courant
//   private Random random;   
//   private Vector<String> TIPS = null;    // Les TIPS
   
//  /** Creation du label pour le copyright et les TIPS */
   protected Tips(Aladin aladin) {
      super(aladin.COPYRIGHT,Label.LEFT,Aladin.SPLAIN);
//      this.aladin = aladin;
      addMouseListener(this);
      setMarge(5);
//      start=System.currentTimeMillis();
//      random = new Random(start);
   }
   
//   /** Chargement des TIPS */
//   private void loadTips() {
//      TIPS = new Vector<String>(100);
//      String s;
//      for( int i=0; true; i++ ) {
//         s = aladin.chaine.getString("TIP"+i);
//         if( s==null ) break;
//         TIPS.add(s);
//      }
//   }
   
   /** Retourne le tip courant, ou null si aucun */
   protected String getTips() { return cTips; }
   
//   /** Remplacement du Copyright par un TIPS de temps en temps */
//   private String pubNews(String text) {
//      if( text!=aladin.COPYRIGHT || random==null ) return text;
//      if( TIPS==null ) loadTips();
//      tipCpt++;
//      if( tipCpt>=1.5*TIPMAX ) { tipCpt=0; cTips=null; }
//      if( tipCpt<TIPMAX ) return text;
//      if( cTips==null )  cTips = getNextTips();
//      
//      return cTips;
//   }
   
   public void setText(String text) {
      super.setText(text);
   }

//   private String getNextTips() {
//      int n = random.nextInt(TIPS.size());
//      return "TIP: "+TIPS.elementAt(n);
//   }
   
   /** Changement de la couleur si c'est un TIPS */
   public Color getForeground() {
      if( cTips!=null ) return Color.green;
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
//      tipCpt=TIPMAX+1;
      cTips=null;
      setText(Aladin.COPYRIGHT);
   }
   
}
