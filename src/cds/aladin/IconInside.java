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

import java.awt.Graphics;

import javax.swing.SwingUtilities;

/**
 * Bouton pour ne conserver que les collections visibles dans le champ de vue courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (d�c 2016) Creation
 */
public class IconInside extends Icon {
   static final int L = 12;      // Taille d'un logo
   static String INSIDE;
   
   private boolean activated=false;
   
   /** Creation */
   protected IconInside(Aladin aladin) { this(aladin,32,24); }
      
   protected IconInside(Aladin aladin, int w, int h) {
      super(aladin,w,h);
      INSIDE=  aladin.chaine.getString("INSIDE");
     
   }

   
   // Barres horizontales du dessin (cadre et une fl�che pointant � l'int�rieur)
   static final private int TX[][] = {
         {0,0,13},
         {1,0,0},             {1,13,13},
         {2,0,0},             {2,13,13},
         {3,0,0},             {3,13,13},
         {4,0,0},             {4,13,13},
         {5,0,0},  {5,6,6},   {5,13,13},
         {6,0,0},  {6,5,7},   {6,13,13},
         {7,0,0},  {7,4,8},   {7,13,13},
         {8,0,0},  {8,5,7},   {8,13,13},
         {9,0,2},  {9,5,7},   {9,11,13},
                  {10,5,7},  
                  {11,5,7},  
         };
   
  
   /** Dessine l'icone  */
   protected void drawElague(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   protected boolean isAvailable() {
      return !aladin.view.isFree() && !aladin.directory.isFree();
   }
   
   /** Retourne true si l'�lagage de l'arbre HiPS est activ� */
   protected boolean isActivated() { return activated; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 10+DX;
      int y = 1;
      boolean isAvailable = isAvailable();
      
      if( isAvailable ) {
         g.setColor( getFillInColor() );
         g.fillRect(x, y, 13, 10);
      }

      g.setColor( getLogoColor() );
      drawElague(g,x,y);
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(INSIDE,W/2-g.getFontMetrics().stringWidth(INSIDE)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      activated = !activated;
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            aladin.directory.resumeTree();
         }
      });
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("INSIDETIP"); }
   protected String getHelpKey() { return "Inside.HELP"; }
}
