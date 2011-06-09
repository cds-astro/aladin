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

/**
 * Un segment d'une grille de coordonnées. Celui-ci comporte les éléments suivants
 * . la position des 2 bouts à la fois sur la sphère céleste dans le système
 * de référence courant  et en XY dans la vue zoomée (al1,del1 - x1,y1) et
 * (al2,del2 - x2,y2)
 * . un éventuel label à afficher (label) et sa position (labelMode), soit
 * sur la marge de gauche, soit sur la marge du haut.
 * . un flag boolean "horsChamp" positionné à true si le segment est totalement
 * en dehors du champ de vue
 * . un clip, rectangle englobant le segment
 * @see Aladin.Grille
 * @see Aladin.ViewSimple
 * @author P. Fernique CDS
 * @version 1.0 : (fév 05) création
 */
public class Segment {
   static final int NOLABEL  = 0; // N'affiche pas de label
   static final int GAUCHE   = 1; // Affiche à la marche de gauche
   static final int HAUT     = 2; // Affiche à la marge du haut
   static final int MILIEURA = 3; // Affiche directement sur le segment en RA constant
   static final int MILIEUDE = 4; // Affiche directement sur le segment en DE constant
   
   static final int ISOUNKNOWN=0; // pas d'indication sur RA ou DE constant
   static final int ISORA    = 1; // Le segment est à RA constant
   static final int ISODE    = 2; // Le segment est à DE constant
   
   protected double al1,del1;	// 1er bout du segment  (coord sphériques)
   protected double al2,del2;   // 2ème bout du segment (coord sphériques)
   protected int iso=ISOUNKNOWN;// soit ISORA, soit ISODE, soit ISOUNKNOWN
   protected int x1,y1;         // 1er bout en coord projetées
   protected int x2,y2;         // 2ème bout en coord projetées
   protected String label=null; // Label du segment
   protected int labelMode=NOLABEL;// Mode d'affichage du label
   protected boolean horsChamp; // true si on est en-dehors du champ de vue
   private Rectangle clip=null; // Rectangle contenant le segment
   
   /** Génération d'un segment qui va prolonger le segment courant */
   protected Segment createNextSegment() {
      Segment seg = new Segment();
      seg.del1=seg.del2=del2;
      seg.al1=seg.al2=al2;
      seg.x1=seg.x2=x2; seg.y1=seg.y2=y2;
      return seg;
   }
   
   /** Génération d'un segment qui va prolonger le segment courant */
   protected Segment copy(boolean withLabel) {
      Segment seg = new Segment();
      seg.al1=al1; seg.al2=al2;
      seg.del1=del1; seg.del2=del2;
      seg.x1=x1; seg.x2=x2;
      seg.y1=y1; seg.y2=y2;
      seg.iso=iso;
      seg.clip=clip;
      if( withLabel ) {
         seg.labelMode = labelMode;
         seg.label = label;
      }
      return seg;
   }
      
   /** Subdivise le segment en 2 en fonction des coordonnées sphériques.
    * Effectue le calcul des projections correspondant au point médian
    * @param la vue utilisé pour la projection
    * @return les 2 segments calculés
    */
   protected Segment[] subdivise(ViewSimple v) throws Exception {
      Segment seg[] = new Segment[2];
      seg[0] = copy(true);
      seg[1] = copy(false);
      double al = (al2+al1 - (Math.abs(al2-al1)>180 ? 360:0))/2;
      double del = (del2+del1 -(Math.abs(del2-del1)>180 ? 360:0))/2;
      seg[0].al2=seg[1].al1 = al;
      seg[0].del2=seg[1].del1 = del;
      Point p = getXY(v,al,del);
      if(p ==null ) return null;
      seg[0].x2=seg[1].x1=p.x;
      seg[0].y2=seg[1].y1=p.y;
      return seg;
   }
   
   /** Fournit le point XY dans les coordonnées de la vue zoomée (v)
    * correspondant aux coordonnées al,del qui sont supposées être
    * dans le système de référence courant (Choice J2000,B1950,GAL...)
    */
   private Coord c = new Coord();
   private Point getXY(ViewSimple v,double al,double del) {
      c.al=al; c.del=del;
      c = v.aladin.localisation.frameToICRS(c);
      v.getProj().getXY(c);
      clip=null;
      if( Double.isNaN(c.x) ) return null;
      return v.getViewCoord(c.x,c.y);
   }
   
   /** Calcul de la projection du deuxième bout du segment */
   protected boolean projection(ViewSimple v) {
      Point p = getXY(v,al2,del2);
      if( p==null ) return false;
      x2=p.x; y2=p.y;
      return true;
   }
   
   /** Retourne vrai si l'angle entre les 2 segments est supérieur
    * à 5° */
//   static final double ALPHA = 5*Math.PI/180;
   static final int BETA = (int)(1./Math.tan(5*Math.PI/180));
   static protected boolean courbe(Segment s1,Segment s2) {
      double dx1 = s1.x2-s1.x1;
      double dx2 = s2.x2-s2.x1;
      double dy1 = s1.y2-s1.y1;
      double dy2 = s2.y2-s2.y1;
      return Math.abs(dx1*dy2-dx2*dy1) > 1+ Math.abs(dx1*dx2+dy1*dy2)/BETA;
   }
   
   /** Retourne le carré de la taille du segment projeté */
   protected double distXY() {
      return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
   }
   
   /** Retourne true si le segment passe dans le clip. On a pris un peu de marge */
   protected boolean inClip(Rectangle clip) {
      if( clip==null ) return true;
      return Obj.intersectRect(clip,Math.min(x1,x2)-2,Math.min(y1,y2)-2, 
            Math.abs(x2-x1)+4,Math.abs(y2-y1)+4);
   }
   
//   static private boolean col;
   
   /** Dessin du segment */   
   protected void draw(Graphics g,ViewSimple v,Rectangle clip,int i,int dx,int dy) {
      
      if( label==null && !inClip(clip) ) return;
      
//      if( Aladin.levelTrace>=3 ) { g.setColor(col?Color.magenta:Color.blue); col=!col; }
//      g.setColor(Color.magenta);
      
      
//      g.setColor( iso==ISORA ? Color.red : iso==ISODE ? Color.green : Color.blue );
//      if( labelMode==GAUCHE ) g.setColor(Color.white);
//      if( labelMode==HAUT ) g.setColor(Color.yellow);
      
      g.drawLine(x1+dx,y1+dy,x2+dx,y2+dy);
      
//    g.setColor(Color.black);
//    g.drawString(i+"",(int)(x1+x2)/2,(int)(y1+y2)/2);
      
//      int d = (int)(Math.round(del2*10)/10.);
//      if( del2!=del1 ) g.drawString(d+"",(int)(x1+x2)/2,(int)(y1+y2)/2);

      if( label==null ) return;
      if( labelMode==MILIEURA ) g.drawString(label,(int)(x1+x2)/2+dx,(int)y1+dy-5);
      else if( labelMode==MILIEUDE ) g.drawString(label,(int)(x1-30+dx),(int)(y1+y2)/2+dy-2);
      else if( labelMode==GAUCHE && y2>30 )g.drawString(label,5+dx,(int)(y1+y2)/2+dy-2);
      else if( labelMode==HAUT ) g.drawString(label,(int)(x1+x2)/2+dx,25+dy);
   }
}
