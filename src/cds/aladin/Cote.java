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
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JComboBox;

import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.astro.AstroMath;
import cds.tools.Util;

/**
 * Objet graphique pour une Cote
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : (oct 04) peaufinage de la "Coupe"
 * @version 1.1 : (fev 04) Ajout de la manipulation d'une "Coupe"
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Cote extends Ligne {

   protected double dist=-1;    // Dist angulaire de la cote (ou -1 si non encore calcul�e)
   protected double distRA=-1;    // Dist RA de la cote (ou -1 si non encore calcul�e)
   protected double distDE=-1;    // Dist DE de la cote (ou -1 si non encore calcul�e)
   private double distXY=-1;    // Dist de la cote (ou -1 si non encore calcul�e)
   private boolean withTriangle=false; // True si affichage de la base et de la hauteur conjointe avec affichage label

  /** Constructeurs du premier bout.
   * @param plan plan d'appartenance de la cote
   * @param x,y  position du premier bout
   */
   protected Cote(Plan plan,ViewSimple v, double x, double y) {
      super(plan,v,x,y,(String)null);
      bout=2;
   }

  /** Creation d'une Cote pour les bakcups */
   protected Cote(Plan plan) { super(plan); bout=2; }

  /** Constructeurs du premier bout.
   * @param plan plan d'appartenance de la cote
   * @param x,y  position du premier bout
   * @param id   identificateur associe a la cote
   */
   protected Cote(Plan plan,ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,id);
      bout=2;
   }

  /** Constructeurs du deuxieme bout.
   * @param plan     plan d'appartenance de la cote
   * @param x,y      position du premier bout
   * @param debcote  premier bout de la cote
   */
   protected Cote(Plan plan,ViewSimple v, double x, double y,Cote debcote) {
      this(plan,v,x,y,"",debcote);
   }

  /** Constructeurs du deuxieme bout.
   * @param plan     plan d'appartenance de la cote
   * @param x,y      position du premier bout
   * @param id       identificateur associe a la cote
   * @param debcote  premier bout de la cote
   */
   protected Cote(Plan plan,ViewSimple v, double x, double y, String id,Cote debcote) {
      super(plan,v,x,y,id,debcote);
      bout=2;
   }
   
   protected Cote(double ra, double dec, Plan plan, ViewSimple v,Cote debcote) {
      super(ra,dec,plan,v,debcote);
      bout=2;
   }
   
   protected Cote(double ra, double dec, Plan plan, ViewSimple v,Ligne debcote) {
      super(ra,dec,plan,v,debcote);
      bout=2;
   }
   
   public Vector getProp() {
      Cote deb = (Cote)getFirstBout();
      Cote fin = (Cote)getLastBout();
      Vector propList= deb.getProp1();
      propList.addAll(fin.getProp1());
      
      final Couleur col = new Couleur(couleur,true);
      final PropAction changeCouleur = new PropAction() {
         public int action() { 
            Color c= col.getCouleur();
            if( c==couleur ) return PropAction.NOTHING;
            setColor(c);
            return PropAction.SUCCESS;
         }
      };
      col.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeCouleur.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("color","Color","Alternative color",col,null,changeCouleur) );
      
      
      final JComboBox pole =  new JComboBox(LABELMODES);
      final PropAction updatePole = new PropAction() {
         public int action() {
            labelMode = isWithLabel() ? (isWithTriangle() ? 2 : 1) : 0;
            pole.setSelectedIndex(labelMode);
            return PropAction.SUCCESS;
         }
      };
      final PropAction changeLabel = new PropAction() {
         public int action() {
            int mode = pole.getSelectedIndex();
            if( labelMode==mode ) return PropAction.NOTHING;
            setWithLabel( mode>0 );
            setWithTriangle(mode==2);
            return PropAction.SUCCESS;
         }
      };
      pole.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeLabel.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("Labels","Labels","Distance labels control",pole,updatePole,changeLabel) );

      
      return propList;
   }
   
   private int labelMode=-1;
   static private final String [] LABELMODES = { "No label when unselected","Only distance label when unselected","Triangle distance lines and labels" };
   
   public Vector getProp1() {
      Vector propList = super.getProp();
      Prop.remove(propList, "color");
      return propList;
  }
   

   /** Retourne le type d'objet */
   public String getObjType() { return finligne==null ? "Arrow" : "Arrow+"; }

   /** Modification de id pour prendre en compte la taille */
   protected void status(Aladin aladin) {
      setId();
      super.status(aladin);
   }

  /** Test d'appartenance a la cote.
   * Retourne vrai si le point (x,y) de la vue se
   * trouve sur la cote
   * Met egalement a jour l'id avec la taille courante de la Cote
   * afin de pouvoir la visualiser pendant le trace
   * @param x,y le point a tester
   * @param z le zoom
   */
   protected boolean in(ViewSimple v,double x, double y) {
      if( !super.in(v,x,y) ) return false;
      setId(v);
      return true;
   }

   /** Retourne la derni�re distance angulaire calcul�e
    * ou -1 si non encore calcul�e */
   protected double getDist() { return dist; }

   /** Retourne la derni�re distance angulaire calcul�e
    * ou -1 si non encore calcul�e */
   protected double getDistXY() { return distXY; }

   protected void deltaPosition(ViewSimple v,double x, double y) {
      super.deltaPosition(v,x,y);
      if( finligne!=null && finligne instanceof Cote) ((Cote)finligne).setId(v);
      else setId(v);
   }

   protected void deltaRaDec(double dra, double dde) {
      super.deltaRaDec(dra,dde);
      if( finligne!=null && finligne instanceof Cote ) ((Cote)finligne).setId();
      else setId();
   }

   protected void setPosition(ViewSimple v,double x, double y) {
      super.setPosition(v,x,y);
      if( finligne!=null && finligne instanceof Cote ) ((Cote)finligne).setId();
      setId();
   }

   // Juste pour �viter de les r�-allouer tout le temps.

   /** Mise en place de l'ID.
    * En fonction du plan de reference courant, positionne id avec la
    * longueur courante de la cote
    */
   protected void setId() { setId(null); }
   protected void setId(ViewSimple v) {
      Position p1,p2;
      double dx,dy;
      if( debligne!=null ) {
         p2 = this;
         p1 = debligne;
      } else {
         p2 = finligne;
         p1 = this;
      }
      if( v==null ) v = plan.aladin.view.getCurrentView();
      if( v==null ) return;
      
      int frame = plan.aladin.localisation.getFrame();
      if( frame!=Localisation.XY && frame!=Localisation.XYNAT && frame!=Localisation.XYLINEAR ) {
         try {
            if( Projection.isOk( v.getProj() ) ) {
               Coord  c1 = new Coord(p1.raj,p1.dej);
               Coord  c2 = new Coord(p2.raj,p2.dej);
               if( !Double.isNaN(c1.al) && !Double.isNaN(c2.al) ) {
                  double dra = c2.al-c1.al;
                  double dde = Math.abs(c1.del-c2.del);
                  double cosc2 = AstroMath.cosd(c2.del);
                  double num = cosc2 * AstroMath.sind(dra);
                  double den = AstroMath.sind(c2.del) * AstroMath.cosd(c1.del)
                     - cosc2 * AstroMath.sind(c1.del) * AstroMath.cosd(dra);
                  double angle = (dra==0.0 && dde==0.0)?-1000:(den==0.0)?90.0:Math.atan2(num,den)*180/Math.PI;
                  if( angle<0.0 ) angle+=360.0;
                  dra = Math.abs(dra);
                  if( dra>180 ) dra-=360;
                  double drac = dra*AstroMath.cosd(c1.del);
                  distRA=drac;
                  distDE=dde;
                  dist = Coord.getDist(c1,c2);
                  if( dist==0 ) id="Null distance";
                  
                  // On sort de la projection ?
                  else if( dist>180 ) { id=""; return; }
                  
                  else id= "Dist = "+ Coord.getUnit(dist) +
                     " (RA="+Coord.getUnit(drac)+
                     "/"+Coord.getUnitTime(dra/15)+
                     ", DE="+Coord.getUnit(dde)+")"+
                     ((angle==-1000)?"":" PA = "+(Math.round(angle*10)/10.0)+" deg");
                  v.getProj().getXY(c1);
                  v.getProj().getXY(c2);
                  dx = c1.x-c2.x;
                  dy = c1.y-c2.y;
                  distXY = (int)(Math.sqrt(dx*dx+dy*dy)+0.5);
                  return;
               }
            }
         } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      }

      // Si on ne peut pas calculer les coordonnees, on donne juste
      // le dx et le dy
      if( !id.equals("-") ) {
         distRA=dx = v.HItoI(p1.xv[v.n]) - v.HItoI(p2.xv[v.n]);
         distDE=dy = v.HItoI(p1.yv[v.n]) - v.HItoI(p2.yv[v.n]);
         dist=distXY = Math.sqrt(dx*dx+dy*dy);
         id= Util.myRound(distXY+"",1)+ " (delta x="+Util.myRound(dx+"",2)+", delta y="+Util.myRound(dy+"",2)+")";
      }
   }
   
   /** Il faut faire 2 polylignes disjointes */
   protected void remove() {
      plan.aladin.calque.zoom.zoomView.cutOff(this);
   }

  /** Test d'appartenance.
   * Retourne vrai si le point (x,y) de l'image se trouve
   * - soit a proximite de l'extremite courante du segment
   * - soit sur le segment mais eloigne des deux extremites
   * @param x,y le point a tester
   * @param z le zoom
   */
   protected boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      Ligne bout, autreCote;
      if( nearArrow(v,x,y) ) return true;
      if( debligne==null ) { autreCote=bout=finligne; }
      else { autreCote=debligne; bout=this; }
      return bout.in(v,x,y) && !autreCote.nearArrow(v,x,y);
   }
   
   /** Positionne le flag LABEL */
   protected final void setWithLabel(boolean withLabel) {
      Ligne bout, autreCote;
      if( debligne==null ) { autreCote=bout=finligne; }
      else { autreCote=debligne; bout=this; }
      
      if( withLabel ) {
         bout.flags |= WITHLABEL;
         autreCote.flags |= WITHLABEL;
      } else {
         bout.flags &= ~WITHLABEL;
         autreCote.flags &= ~WITHLABEL;
      }
   }
   
   /** Retourne true si la source a le flag WITHLABEL positionn� */
   protected boolean isWithLabel() {
      Ligne bout, autreCote;
      if( debligne==null ) { autreCote=bout=finligne; }
      else { autreCote=debligne; bout=this; }
      
      return (bout.flags & WITHLABEL) !=0 || (autreCote.flags & WITHLABEL) !=0;
   }
   
   /** Retourne true si on doit dessiner la hauteur et la base du triangle */
   protected boolean isWithTriangle() { return withTriangle; }
   
   
   /** Positionne le flag d'affichage de la hauteur et de la base du triangle */
   protected void setWithTriangle(boolean withTriangle ) { this.withTriangle = withTriangle; }

   protected void drawID(Graphics g, ViewSimple v,Point p1,Point p2) {
         if( !(isCoteSelected() || isWithLabel())  ) return;
         drawID1(g,v,p1,p2);
   }
   
   protected void drawID1(Graphics g, ViewSimple v,Point p1,Point p2) {
      int frame = plan.aladin.localisation.getFrame();
      String s = raj==Double.NaN || (frame==Localisation.XY 
            || frame==Localisation.XYNAT || frame==Localisation.XYLINEAR )?  Util.myRound(dist+"", 2) : dist>180 ? "" : Coord.getUnit(dist);
      drawLabel(g,v,p1,p2,s,Aladin.BOLD);            
   }

   private boolean isCoteSelected() {
      if( debligne!=null ) return isSelected() || debligne.isSelected();
      return isSelected() || finligne.isSelected();
   }

   protected int clipXId() { return !isCoteSelected()?0 : 12*Coord.getUnit(dist).length(); }
   protected int clipYId() { return !isCoteSelected()?0 : 15; }
   
   protected boolean tooLarge(ViewSimple v,Point p1, Point p2) {
      return false;
   }

   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) {
      if( this instanceof CoteDist ) return super.draw(g,v,dx,dy);
      
      if( !isVisible() ) return false;
      if( !super.draw(g,v,dx,dy) ) { cutOff(); return false; }
      
      drawCoteBase(g,v,dx,dy);

      if( (isSelected() || debligne!=null && debligne.isSelected() || finligne!=null && finligne.isSelected())
            && plan.aladin.view.hasOneCoteSelected() ) cutOn();
      else cutOff();
      
      return true;
   }
   
   private boolean isDrawingTriangle() { return withTriangle; }
   
   /** Dessin de la hauteur et de la base du triangle de la cote */
   protected void drawCoteBase(Graphics g, ViewSimple v,int dx,int dy) {
      if( !isDrawingTriangle() ) return;
      
      if( debligne==null ) return;
      
      double lon = raj;
      double lat = debligne.dej;
      // IL FAUDRAIT PASSER PAR LE FRAME COURANT
//      Localisation.frameToFrame(c, frameSrc, frameDst)
      
      CoteBase base = new CoteBase(lon, lat, plan, v, new CoteBase(debligne,v), Coord.getUnit(distRA));
      base.draw(g, v, dx, dy);
      
      CoteBase hauteur = new CoteBase(lon, lat, plan, v, new CoteBase(this,v), Coord.getUnit(distDE));
      hauteur.draw(g, v, dx, dy);
   }
   
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      clip = super.extendClip(v,clip);
      if( !isDrawingTriangle() ) return clip;
      
      // Si on dessine la base et la hauteur du triangle, il faut �tendre le clip avec ce point
      double lon = debligne==null ? finligne.raj : raj;
      double lat = debligne==null ? dej : debligne.dej;
      // IL FAUDRAIT PASSER PAR LE FRAME COURANT
//      Localisation.frameToFrame(c, frameSrc, frameDst)
      Position a = new Position(plan, v, 0, 0, lon, lat, RADE, null);
      a.projection(v);
      return a.extendClip(v, clip);
   }

   /** Suppression de la coupe memorise dans le zoomView
    * => arret de son affichage
    */
   protected void cutOff() { plan.aladin.calque.zoom.zoomView.cutOff(this); }
   
   /** Passage d'une coupe du segment au zoomView
    * => affichage d'un histogramme dans le zoomView en surimpression
    * de la vignette courante.
    * @return true si le CutGraph a pu �tre fait
    */
   protected boolean cutOn() {
      boolean rep=cutOn1();
      if( !rep ) plan.aladin.calque.zoom.zoomView.setCut(null);
      return rep;
   }

   protected boolean cutOn1() {
      ViewSimple v=plan.aladin.view.getCurrentView();
//      if( Aladin.OUTREACH ) return false;
      if( v==null || plan.aladin.toolBox.getTool()==ToolBox.PAN ) return false;
      Plan p=v.pref;
      if( p==null || !p.hasAvailablePixels() && !(p instanceof PlanImageRGB) ) return false;
      PlanImage pi = (PlanImage)p;

      int x1,y1,x2,y2;
      if( debligne==null ) {
         x1=(int)xv[v.n]; y1=(int)yv[v.n];
         x2=(int)finligne.xv[v.n]; y2=(int)finligne.yv[v.n];
      } else {
         x1=(int)debligne.xv[v.n]; y1=(int)debligne.yv[v.n];
         x2=(int)xv[v.n]; y2=(int)yv[v.n];
      }

      if( x1!=x2 || y1!=y2 ) {
         if( v.northUp || v.isProjSync() ) {
            PointD p1 = new PointD(x1,y1);
            p1 = v.northOrSyncToRealPosition(p1);
            x1=(int)p1.x; y1=(int)p1.y;
            p1 = new PointD(x2,y2);
            p1 = v.northOrSyncToRealPosition(p1);
            x2=(int)p1.x; y2=(int)p1.y;
         }

         int[] res = bresenham(pi,x1,y1,x2,y2);
         int mode = pi.type==Plan.IMAGERGB || pi.type==Plan.IMAGECUBERGB ? ZoomView.CUTRGB : ZoomView.CUTNORMAL;
         plan.aladin.calque.zoom.zoomView.setCut(this,res,mode);
      }
      return true;
   }


   private int [] vBresenham=null;    //Tableau temporaire des valeurs qui vont etre retournees
   private int [] repBresenham=null;

   /**
    * Parcours d'un segment de droite via l'algo de Bresenham
    * et creation d'un tableau de valeurs de pixels pour les pixels
    * qui se trouvent sur ce segment. Si le plan est RGB, les 3 derniers
    * octets de chaque valeur du tableau seront utilis�s pour stocker
    * chaque composante R, G et B
    * -1 indique un pixel non utilis� (hors champ)
    * @param pi Le plan image sous-jacent
    * @param x1 abs du premier point
    * @param y1 ord du premier point
    * @param x2 abs du deuxieme point
    * @param y2 ord du deuxieme point
    * @return un tableau des valeurs des pixels, soit en niveaux de gris,
    *         soit en RGB
    */
   protected int[] bresenham(PlanImage pi,int x1,int y1,int x2,int y2 ) {

      // Variables pour l'algo de Bresenham
      int dx = Math.abs(x2-x1);
      int dy = Math.abs(y2-y1);
      int e,x,y,xend,yend,horiz,diago,verti;

      // Acces aux pixels de l'image sous-jacente
//      byte [] pixels = pi.pixels;
      int width = pi.width;
      int height = pi.height;

      int n=0;  // nombre d'elements du tableau v[]

      // Allocation d'un tableau assez grand pour recevoir les valeurs des pixels
      if( vBresenham==null || vBresenham.length!=dx+dy+2 )  vBresenham = new int[dx+dy+2];
      n=0;

      boolean inverse=false;

      if( dx>dy ) {
         if( x1<x2 ) {
            x=x1; y=y1;
            xend=x2; yend=y2;
         } else {
            x=x2; y=y2;
            xend=x1; yend=y1;
            inverse=true;
         }
         e = 2*dy -dx;
         horiz = 2*dy;
         diago = 2*(dy-dx);
         boolean croissant = y<yend;

         for( int i=0; i<dx; i++ ) {
            vBresenham[n++] = (x<0 || x>=width || y<0 || y>=height)?-1: pi.getPixel8(x,y);
            if( e>0 ) {
               if( croissant ) y++;
               else y--;
               e += diago;
            } else e+=horiz;
            x++;
         }
      } else {
         if( y1<y2 ) {
            x=x1; y=y1;
            xend=x2; yend=y2;
         } else {
            x=x2; y=y2;
            xend=x1; yend=y1;
            inverse=true;
         }
         e = 2*dx -dy;
         verti = 2*dx;
         diago = 2 * (dx-dy);
         boolean croissant = x<xend;

         for( int i=0; i<dy; i++ ) {
            vBresenham[n++] = (x<0 || x>=width || y<0 || y>=height)?-1: pi.getPixel8(x,y);
            if( e>0 ) {
               if( croissant ) x++;
               else x--;
               e +=diago;
            } else e+= verti;
            y++;
         }
      }

      vBresenham[n++] = (xend<0 || xend>=width || yend<0 || yend>=height)?-1: pi.getPixel8(xend,yend);

      // Generation du tableau final
      if( repBresenham==null || repBresenham.length!=n ) repBresenham = new int[n];
      if( inverse ) for( int i=0; i<n; i++ ) repBresenham[n-i-1]=vBresenham[i];
      else System.arraycopy(vBresenham,0,repBresenham,0,n);
      vBresenham=null;
      return repBresenham;

   }

//   private double deb=-1,fin=-1;
//
//   /** Positionnement du segment � colorier en vert pour rep�rer sur la cote
//    * la portion rep�r� dans le Cut
//    * @param deb debut du segment color� (en nombre de pixels images)
//    * @param fin fin du segment color� (en nombre de pixels images)
//    */
//   protected void drawFWHM(double deb, double fin) {
//      this.deb=deb;
//      this.fin=fin;
//   }

   /**
    * Trac� d'un segment de droite via l'algo de Bresenham avec une couleur
    * particuli�re pour une portion d�finie par deb et fin
    * @param g le contexte graphique
    * @param x1 abs du premier point
    * @param y1 ord du premier point
    * @param x2 abs du deuxieme point
    * @param y2 ord du deuxieme point
    */
//   protected void droite(Graphics g,ViewSimple v,int x1,int y1,int x2,int y2) {
//
//      if( g instanceof EPSGraphics ) { g.drawLine(x1,y1,x2,y2); return; }
//
//      // Variables pour l'algo de Bresenham
//      int dx = Math.abs(x2-x1);
//      int dy = Math.abs(y2-y1);
//      int e,x,y,xend,yend,horiz,diago,verti;
//
//      int deb = (int)Math.round(this.deb*v.zoom);
//      int fin = (int)Math.round(this.fin*v.zoom);
//
//      if( dx>dy ) {
//         if( x1<x2 ) {
//            x=x1; y=y1;
//            xend=x2; yend=y2;
//         } else {
//            x=x2; y=y2;
//            xend=x1; yend=y1;
//         }
//         e = 2*dy -dx;
//         horiz = 2*dy;
//         diago = 2*(dy-dx);
//         boolean croissant = y<yend;
//
//         for( int i=0; i<dx; i++ ) {
//            g.setColor(i>=deb && i<=fin ? Color.green: plan.c );
//            g.drawLine(x,y,x,y);
//            if( e>0 ) {
//               if( croissant ) y++;
//               else y--;
//               e += diago;
//            } else e+=horiz;
//            x++;
//         }
//      } else {
//         if( y1<y2 ) {
//            x=x1; y=y1;
//            xend=x2; yend=y2;
//         } else {
//            x=x2; y=y2;
//            xend=x1; yend=y1;
//         }
//         e = 2*dx -dy;
//         verti = 2*dx;
//         diago = 2 * (dx-dy);
//         boolean croissant = x<xend;
//
//         for( int i=0; i<dy; i++ ) {
//            g.setColor(i>=deb && i<=fin ? Color.green : plan.c );
//            g.drawLine(x,y,x,y);
//            if( e>0 ) {
//               if( croissant ) x++;
//               else x--;
//               e +=diago;
//            } else e+= verti;
//            y++;
//         }
//      }
//
//      g.drawLine(xend,yend,xend,yend);
//   }

}
