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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import cds.tools.Util;

/**
 * Objet graphique representant Repère cartouche pour Simbad pointer
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 12 mai 2018) Creation (extraction de la classe mère Repere)
 */
public class RepereSimbad extends Repere {
   
   static final int ARROWLENGTH = 15;   // Taille de la hampe du cartouche (en pixels)
   static final int BORDER = 35;        // Marge droite+gauche du cartouche
   static private int SIZECROSS = 5;    // Taille de la croix de fermeture du cartouche (en pixels)

   private Aladin aladin;
   private boolean inCross;    // true si la souris est sur la croix de fermeture du cartouche
   private boolean inLabel;    // true si la souris est sur le label de l'objet (ID simbad)
   private boolean inBiblio;   // true si on est sur le mot "Biblio"
   private Rectangle cross;    // Dernière position de la croix de fermeture (dans la vue sous la souris, coord view)
   private Rectangle label;    // Dernière position du label (dans la vue sous la souris, coord view))
   private Rectangle biblio;   // Dernière position du lien biblio (dans la vue sous la souris, coord view))

   protected RepereSimbad(Aladin aladin,ViewSimple v,Coord c) {
      super(null,v,c);
      this.aladin=aladin;
      setSize(ARROWLENGTH);
      inCross=inLabel=inBiblio=false;
   }
   
   /** Détermine la taille du cartouche en fonction de son contenu */
   protected void setD() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw = m.stringWidth(id)+BORDER;
      dh=  HF;
      try {
         dw = m.stringWidth( getId(id) )+BORDER;
         int w = m.stringWidth( "Type : "+getType(id) )+BORDER;
         if( w>dw ) dw = w;
         w = m.stringWidth( "Mag : "+getMag(id) )+BORDER;
         if( w>dw ) dw = w;
         dh*=5.4; 
      } catch( Exception e ) { 
         int w = m.stringWidth("unknown by Simbad")+BORDER-5;
         if( w>dw ) dw = w;
         dh*=3; 
      }
   }
   
   /** La souris est-elle dans le cartouche (coordonnées view) */
   protected  boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      double l=L/v.getZoom();
      double xc = xv[v.n];
      double yc = yv[v.n];
      return(xc<=x+l+dw/2 && xc>=x-l-dw/2 && yc<=y+l+dh/2 && yc>=y-l-dh/2);
   }
   
   // Extraction de l'ID de l'objet astronomique à partir de la chaine retournée par QuickSimbad
   private String getId( String id ) {
      int i=id.lastIndexOf('(');
      return id.substring(0,i).trim();
   }

   // Extraction de la magnitude de l'objet astronomique à partir de la chaine retournée par QuickSimbad
   private String getMag( String id ) {
      int i=id.lastIndexOf('(');
      int j=id.indexOf(',',i+1);
      return id.substring(i+1,j).trim();
   }

   // Extraction du type de l'objet astronomique à partir de la chaine retournée par QuickSimbad
   private String getType( String id ) {
      int i=id.lastIndexOf('(');
      int j=id.indexOf(',',i+1);
      return id.substring(j+1,id.length()-1).trim();
   }
   
   /** Met à jour non seulement le flag de présence de la souris sur le label (ID simbad)
    * mais également le flag de la croix de fermeture et le lien vers la biblio
    * Si un des flags a été modifié depuis la dernière fois, demande un réaffichage de la vue */
   protected boolean inLabel(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      if( cross!=null ) {
         boolean inCross1 = cross.contains(x,y);
         if( inCross1!=inCross ) v.repaint();
         inCross=inCross1;
      }
      
      if( biblio!=null ) {
         boolean inBiblio1 = biblio.contains(x,y);
         if( inBiblio1!=inBiblio ) v.repaint();
         inBiblio=inBiblio1;
      }
      
      if( label==null ) return false;
      boolean inLabel1 = label.contains(x,y);
      if( inLabel1!=inLabel ) v.repaint();
      inLabel=inLabel1;
      
      return inLabel;
   }
   
   // Dessine la croix de fermeture, et s'il s'agit de la vue sous la souris
   // mémorise sa position
   private void drawCross(Graphics g, ViewSimple v, int x, int y) {
      if( v.n!=v.aladin.view.getMouseNumView() ) return;
      g.setColor(inCross ? Color.red : Color.gray);
      g.drawLine(x, y, x + SIZECROSS, y + SIZECROSS);
      g.drawLine(x + 1, y, x + SIZECROSS + 1, y + SIZECROSS);
      g.drawLine(x + 2, y, x + SIZECROSS + 2, y + SIZECROSS);
      g.drawLine(x + SIZECROSS, y, x, y + SIZECROSS);
      g.drawLine(x + SIZECROSS + 1, y, x + 1, y + SIZECROSS);
      g.drawLine(x + SIZECROSS + 2, y, x + 2, y + SIZECROSS);
      cross = new Rectangle(x, y - 2, SIZECROSS + 2, SIZECROSS + 2);
   }
   
   // Dessine le label (ID simbad), et s'il s'agit de la vue sous la souris
   // mémorise sa position. Si la souris est sur le label, souligne le label
   private void drawLabel(Graphics g, ViewSimple v, int x, int y, String s) {
      g.setColor( Aladin.COLOR_BLUE );
      g.setFont(Aladin.BOLD);
      g.drawString( s,x,y);
      if( v!=aladin.view.getMouseView() ) return;
      int w = g.getFontMetrics().stringWidth(s);
      label = new Rectangle(x-1,y-16,w+2,16);
      if( inLabel ) {
         g.drawLine(x-1,y+3,x+w+2,y+3);
         g.drawLine(x-1,y+4,x+w+2,y+4);
      }
   }
   
   // Dessine le lien vers la biblio, et s'il s'agit de la vue sous la souris
   // mémorise sa position. Si la souris est dessus, souligne le lien
   private void drawBiblio(Graphics g, ViewSimple v, int x, int y) {
      g.setColor( Aladin.COLOR_BLUE );
      String s="Biblio";
      int w = g.getFontMetrics().stringWidth(s);
      x -= w+5;
      g.drawString( s,x,y);
      if( v!=aladin.view.getMouseView() ) return;
      biblio = new Rectangle(x-1,y-16,w+2,16);
      if( inBiblio ) {
         g.drawLine(x-1,y+3,x+w+2,y+3);
         g.drawLine(x-1,y+4,x+w+2,y+4);
      }
   }
   
   /** Ouverture de la page Web simbad pour l'objet indiqué */
   protected void openSimbadID() {
      int offset = id.indexOf('(');
      if( offset<0 ) return;
      String obj = id.substring(0,offset).trim();
      aladin.glu.showDocument("smb.query", Tok.quote(obj));
   }
   
   /** Ouverture de la page Web simbad pour la littérature associée à l'objet indiqué */
   protected void openSimbadBiblio() {
      int offset = id.indexOf('(');
      if( offset<0 ) return;
      String obj = id.substring(0,offset).trim();
      aladin.glu.showDocument("simbad.score", Tok.quote(obj));
   }
   
   /** Suppression du cartouche */
   protected void dispose() {
      aladin.view.simRep=null;
      aladin.view.stopSED(false);
   }

   /** Action à faire suite à un clic dans le cartouche (dépend de la dernière position
    * de la souris)
    * @return true si une action a été opérée
    */
   protected boolean action() {
      if( inLabel ) openSimbadID();
      else if( inBiblio ) openSimbadBiblio();
      else if( inCross ) dispose();
      else return false;
      return true;
   }

   /** Dessin du cartouche */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      Point p = getViewCoord(v,L,L);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;

      g.setColor(JAUNEPALE);
      g.drawLine(p.x,   p.y-L, p.x,   p.y-3);
      g.setColor(Color.black);
      g.drawLine(p.x+1,   p.y-L, p.x+1,   p.y-3);
      g.setColor(JAUNEPALE);
      Util.drawCircle5(g, p.x, p.y);
      g.setColor(Color.black);
      Util.drawCircle7(g, p.x, p.y);

      g.setColor( CARTOUCHE_BACKGROUND );
      g.fillRoundRect(p.x-dw/2,p.y-L-dh-1, dw-2, dh+3, 10, 10);
      
      try {
         int x=p.x-dw/2+5;
         int y=p.y-4*L-6;
         String s = getId(id);
         drawLabel(g,v,x,y,s);

         g.setFont(Aladin.ITALIC);
         g.setColor( CARTOUCHE_FOREGROUND );
         g.drawString( "Type: "+getType(id),p.x-dw/2+15,p.y-3*L-4);
         g.drawString( "Mag : "+getMag(id),p.x-dw/2+15,p.y-2*L-4);

         g.setColor( CARTOUCHE_FOREGROUND.darker() );
         s = "by Simbad";
         g.setFont( g.getFont().deriveFont( g.getFont().getSize2D()-2));
         x = p.x+dw/2-5 - g.getFontMetrics().stringWidth(s);
         y = p.y-L-3;
         g.drawString( s,x,y);
         
         drawBiblio(g,v,x,y);

      } catch( Exception e ) {
         g.setColor( CARTOUCHE_FOREGROUND );
         g.drawString( id,p.x-dw/2+5,p.y-2*L-5);
         g.setColor( CARTOUCHE_FOREGROUND.darker() );
         g.setFont(Aladin.ITALIC);
         g.setFont( g.getFont().deriveFont( g.getFont().getSize2D()-2));
         String s = "unknown by Simbad";
         int x = p.x+dw/2-5 - g.getFontMetrics().stringWidth(s);
         int y = p.y-L-3;
         g.drawString( s,x,y );
      }
      
      drawCross(g,v,p.x+dw/2-SIZECROSS-7,p.y-L-dh+3);

      return true;
   }


}
