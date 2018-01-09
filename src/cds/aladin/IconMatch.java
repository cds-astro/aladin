// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Bouton pour la synchronisation des vues
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Mars 2007) Creation
 */
public class IconMatch extends Icon {
   static final int L = 12;      // Taille d'un logo
   static String MATCH;
   protected boolean megaMatch=false;

  /** Creation */
   protected IconMatch(Aladin aladin) {
      super(aladin,35,24);
      MATCH= aladin.chaine.getString("SYNC");
   }
   
   /** Retourne true si on est en mode sync */
   protected boolean isMatched() {
      return aladin.view.getNbSelectedView()>1 && !aladin.view.hasCompatibleViews();
   }

   protected boolean isAvailable() { return aladin.view.isMultiView() && getMode()>=1; }
   
   /** Retourne la couleur pour le mode de synchronisation actuelle
    * 0 - pas possible
    * 1 - possible 
    * 2 - activé 
    * 3 - activé + megaSync 
    */
   protected Color getFillColor() {
      int mode=getMode();
      Color c = mode==0 || mode==1 ? getBackground() :
         mode==2 ? Color.blue : Aladin.COLOR_RED;
      return c;
   }
   
   protected Color getLogoColor() {
      if( getMode()>1 ) return Aladin.COLOR_ICON_ACTIVATED;
     return super.getLogoColor();
   }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 20;
      int y = 7;
      int r = 3;
      
      g.setColor( getFillColor() );
      g.fillRect(x-r,y-r,2*r,2*r);
      
      // Les bords du carré
      g.setColor( getLogoColor() );
      g.drawRect(x-r,y-r,2*r,2*r);
      
      // La croix
      g.drawLine(x-L/2,y,x-1,y);
      g.drawLine(x+1,y,x+L/2,y);
      g.drawLine(x,y-L/2,x,y-1);
      g.drawLine(x,y+1,x,y+L/2);
      
      // Label
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(MATCH,W/2-g.getFontMetrics().stringWidth(MATCH)/2,H-2);
   }
   
   
   /** Retourne un code pour le mode de synchronisation actuelle
    * 0 - pas possible (gris)
    * 1 - possible (vert)
    * 2 - activé (orange)
    * 3 - activé + megaSync (rouge)
    */
   protected int getMode() {
      boolean active = aladin.view.hasCompatibleViews();
      boolean sync = aladin.view.getNbSelectedView()>1  && !active;
      return  sync && megaMatch ? 3 :
              sync ? 2 :
              active ? 1 : 0; 
   }
   
   protected boolean isProjSync() { return getMode()==3; }

   protected void submit() { aladin.cycleMatch(); }
      
   protected String getHelpTip() { return aladin.chaine.getString("MVIEWSYNC"); }
   protected String getHelpKey() { return "Sync.HELP"; }
}
