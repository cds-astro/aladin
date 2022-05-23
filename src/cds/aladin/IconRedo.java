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

import java.awt.Color;
import java.awt.Graphics;

/**
 * Bouton "Redo" pour remettre à jour tous les plans ConeSearch/TAP en fonction du champ courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (mai 2022) Creation
 */
public class IconRedo extends Icon {
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
      {0, 6,6},
      {1, 6,7},
      {2, 3,8},
      {3, 2,2},  {3,6,7}, 
      {4, 1,1},  {4,6,6},
      {5, 1,1},
      {6, 1,1},
      {7, 1,1},
      {8, 2,2},  {8,9,9},
      {9, 3,3},  {9,8,8},
      {10,4,7},

   };
   
   protected String LABEL;
   
   protected IconRedo(Aladin aladin) {
      super(aladin,28,24);
      LABEL = aladin.chaine.getString("CSREDO");
   }
   
   /** Dessine l'icone */
   static protected void drawRedo(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   protected boolean isAvailable() {
      return aladin.calque.getNbRedo()>0;
   }
//   protected boolean isActivated()    { return aladin.calque.hasClinDoeil(); }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x=5+DX;
      int y=2;
      
      drawRedo(g,x,y, getLogoColor() );
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,W/2-g.getFontMetrics().stringWidth(LABEL)/2,H-2);
   }

   protected void submit() {
      if( !isAvailable() ) return;
      aladin.redo();
   }
   
   protected String getHelpTip() { return aladin.chaine.getString("CSREDOH"); }
   protected String getHelpKey() { return "CSREDOH"; }
}
