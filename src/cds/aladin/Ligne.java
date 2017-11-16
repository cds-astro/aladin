// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import cds.aladin.Hist.HistItem;
import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import healpix.essentials.FastMath;

/**
 * Objet graphique pour une Ligne.
 * En fait il s'agit du sommet de polyligne qui contient le sommet suivant
 * et le sommet precedent
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1? : (4 septembre 02) Fusion avec LigneCouleur (Thomas)
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Ligne extends Position {

   // Les constantes
   static final int L = 7;          // Longueur de la fleche
   static final int DL = L/2 +1;    // Decalage du a la fleche

   /** Type du bout de la ligne (0-aucun bout, 1-fleche, 2-cote, 3-fin polyligne circulaire,
    * 4 - début polyligne circulaire en cours de tracé ) */
   protected byte bout;

   /** <I>true</I> si la ligne doit etre affichee avec son id */
   //   protected boolean withlabel;

   // couleur de la ligne ou null si on prend la couleur du plan d'appartenance
   protected Color couleur = null;
   // la ligne ne doit pas etre affichee si hidden == true
   protected boolean hidden = false;

   /** Reference au point de debut de la ligne ou <I>null</I> sinon */
   protected Ligne debligne=null;
   /** Reference au point de fin de la ligne ou <I>null</I> sinon */
   protected Ligne finligne=null;

   /** Creation d'une ligne pour les backups */
   protected Ligne(Plan plan) { super(plan); }

   /** Constructeurs du premier bout.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position du premier bout
    */
   protected Ligne(Plan plan,ViewSimple v,double x, double y) {
      super(plan,v,x,y,0.,0.,XY|RADE_COMPUTE,null);
   }

   /** Constructeurs du premier bout.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position du premier bout
    * @param id   identificateur associe a la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,0.,0.,XY|RADE_COMPUTE,id);
   }

   /** Continuation d'une ligne.
    * @param plan     plan d'appartenance de la ligne
    * @param x,y      position du premier bout
    * @param debligne sommet precedent dans la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y,Ligne debligne) {
      this(plan,v,x,y,"",debligne);
   }

   protected Ligne(double ra, double dec, Plan plan, ViewSimple v) {
      super(plan,v,0.0,0.0,ra,dec,RADE,null);
   }

   protected Ligne(ViewSimple v,double ra, double dec) {
      super(null,v,0.0,0.0,ra,dec,RADE_COMPUTE,null);
   }
   
   protected Ligne(double ra, double dec) {
      this.raj=ra;
      this.dej=dec;
   }

   protected Ligne(double ra, double dec, Plan plan, ViewSimple v, Ligne debligne) {
      this(ra,dec,plan,v,null,debligne);
   }

   protected Ligne(double ra, double dec, Plan plan, ViewSimple v, String id,Ligne debligne) {
      super(plan,v,0.0,0.0,ra,dec,RADE,id);
      this.debligne = debligne;
      if( debligne!=null ) debligne.finligne = this;
   }

   /** Continuation d'une ligne.
    * @param plan     plan d'appartenance de la ligne
    * @param x,y      position du premier bout
    * @param id       identificateur associe a la ligne
    * @param debligne sommet precedent dans la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y, String id, Ligne debligne) {
      super(plan,v,x,y,0.,0.,XY|RADE_COMPUTE,id);
      this.debligne = debligne;
      if( debligne!=null ) debligne.finligne = this;
   }

   ///// Constructeurs permettant de preciser la couleur de la ligne /////
   /** Constructeurs du premier bout.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position du premier bout
    * @param c    couleur de la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y, Color c) {
      this(plan,v,x,y);
      this.couleur=new Color(c.getRGB());
   }

   /** Constructeurs du premier bout.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position du premier bout
    * @param id   identificateur associe a la ligne
    * @param c    couleur de la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y,String id, Color c) {
      this(plan,v,x,y,id);
      this.couleur=new Color(c.getRGB());
   }

   /** Continuation d'une ligne.
    * @param plan     plan d'appartenance de la ligne
    * @param x,y      position du premier bout
    * @param debligne sommet precedent dans la ligne
    * @param c        couleur de la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y,Ligne debligne, Color c) {
      this(plan,v,x,y,"",debligne,c);
      this.couleur=new Color(c.getRGB());
   }

   /** Continuation d'une ligne.
    * @param plan     plan d'appartenance de la ligne
    * @param x,y      position du premier bout
    * @param id       identificateur associe a la ligne
    * @param debligne sommet precedent dans la ligne
    * @param c        couleur de la ligne
    */
   protected Ligne(Plan plan,ViewSimple v, double x, double y, String id, Ligne debligne, Color c) {
      this(plan,v,x,y,id);
      this.debligne = debligne;
      debligne.finligne = this;
      this.couleur=new Color(c.getRGB());
   }

   public Vector getProp() {
      Vector propList = super.getProp();

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
      return propList;
   }
   
   ///// FIN des constructeurs /////

   /** Retourne le type d'objet */
   public String getObjType() { return finligne==null ? "Line" : "Line+"; }

   /**
    * Vérifie qu'un point est proche d'un segment
    */
   static protected boolean inLigne(double x1,double y1, double x2,double y2,
         double x, double y, double l) {

      l = Math.sqrt(l);
      
      // Verifie qu'on est pas en dehors de la boite contenant le segment
      if( (x2-x)*(x1-x)>l || (y2-y)*(y1-y)>l ) return false;

      // Verifie qu'on est a moins de qq pixels du segment
      double dx = x2-x1;
      double dy = y2-y1;
      double ddx = x-x1;
      double ddy = y-y1;
      double dd = ddx*ddx + ddy*ddy;
      if( dx!=0 || dy!=0 ) {
         double ddd = (dx*ddx + dy*ddy);
         dd -= ddd*ddd / (dx*dx + dy*dy);
      }

      return (dd<=l);
   }

   /** Retourne true si l'objet passé est un élément d'une ligne (pas d'une cote) */
   static boolean isLigne(Obj o) {
      return o!=null && o instanceof Ligne && !(o instanceof Cote);
   }

   /** Retourne true si l'objet passé est le premier élément d'une ligne (pas d'une cote) */
   static boolean isDebLigne(Obj o) {
      return isLigne(o) && ( ((Ligne)o).debligne==null );
   }

   /** Positionne les variables nécessaires au dernier segment d'un polygone */
   protected void makeLastLigneForPolygone(ViewSimple v,boolean select) {
      getFirstBout().bout=0;
      bout=3;
      setSelected(select);
      Ligne tmp = getFirstBout();
      raj=tmp.raj;
      dej=tmp.dej;
      tmp.setSelected(select);
      projection(v);
   }

   /** Positionne les variables nécessaires au dernier segment d'une polyligne fermée */
   protected void makeLastLigneForClose(ViewSimple v) {
      Ligne tmp = getFirstBout();
      raj=tmp.raj;
      dej=tmp.dej;
      bout=3;
      projection(v);
   }

   /** Il faut faire 2 polylignes disjointes */
   protected void remove() {
      Ligne avant = debligne;
      Ligne apres = finligne;
      if( avant!=null ) avant.finligne=apres;
      if( apres!=null ) apres.debligne=avant;
      if( bout==2 ) plan.aladin.calque.zoom.zoomView.cutOff(this);
      else if( bout==3 ) plan.aladin.calque.zoom.zoomView.stopHist();
   }

   /** Retourne true si la Ligne fait partie d'un polygone (dernier segment
    * avec un flag bout==3 */
   protected boolean isPolygone() {
      return getLastBout().bout==3;
   }

   public String getCommand() {
      StringBuffer s = new StringBuffer("draw");
      boolean isPolygon = isPolygone();
      if( isPolygon ) s.append(" polygon(");
      else if( this instanceof Cote) s.append(" dist(");
      else s.append(" line(");
      boolean first=true;
      Ligne lig = getFirstBout();
      while( isPolygon && lig!=null && lig.finligne!=null || !isPolygon && lig!=null ) {
         if( !first ) s.append(", ");
         s.append(lig.getLocalisation());
         first=false;
         lig=lig.finligne;
      }
      if( !(this instanceof Cote) && id!=null && id.trim().length()>0 ) s.append(","+Tok.quote(id));
      s.append(')');
      return s.toString();
   }
   
   public boolean hasPhot() { return isPolygone(); }
   public boolean hasPhot(Plan p) {
      if( !hasPhot() ) return false;
      //      if( p instanceof PlanBG ) return false;  // POUR LE MOMENT
      return p.hasAvailablePixels();
   }
   
   /** Redessine tout le polygone avec un aplat - ne fonctionne que pour le dernier segment */
   private void fillPolygon(Graphics g,ViewSimple v,int dx, int dy) {
      if( bout!=3 ) return;
      Ligne tmp = this;
      Polygon pol = new Polygon();
      while( tmp.debligne!=null ) {
         Point p2 = tmp.getViewCoord(v);
         pol.addPoint(dx+p2.x, dy+p2.y);
         tmp=tmp.debligne;
      }
      float opacity = plan==null ? 1f : plan.getOpacityLevel();
      Util.drawFillPolygon(g, pol, 0.1f * opacity, null);
   }

   /** Test d'appartenance a la Ligne
    * Retourne vrai si le point (x,y) de la vue se
    * trouve sur la cote
    * Met egalement a jour l'id avec la taille courante de la Cote
    * afin de pouvoir la visualiser pendant le trace
    * @param x,y le point a tester
    */
   protected boolean in(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      if( debligne==null ) return inBout(v,x,y);
      
      PointD p1 = v.getViewCoordDble(debligne.xv[v.n],debligne.yv[v.n]);
      PointD p2 = v.getViewCoordDble(xv[v.n],yv[v.n]);
      PointD p = v.getViewCoordDble(x,y);
      return inLigne(p1.x,p1.y,p2.x,p2.y,p.x,p.y,mouseDist(v)) || inBout(v,x,y);
      
//      return inLigne(debligne.xv[v.n],debligne.yv[v.n],xv[v.n],yv[v.n],x,y,mouseDist(v)) || inBout(v,x,y);
   }

   /** Test d'appartenance sur un bout
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
   protected boolean inBout(ViewSimple v, double x, double y) {
      return nearArrow(v,x,y);
   }


   /** Retourne vrai si le point (x,y) de l'image se trouve proche
    * de l'extremite d'un segment
    * @param x,y le point a tester
    * @param z le zoom
    */
   boolean nearArrow(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      PointD p1 = v.getViewCoordDble(x, y);
      PointD p2 = v.getViewCoordDble(xv[v.n], yv[v.n]);
      if( p1==null || p2==null ) return false;
      
      double ddx = p1.x-p2.x;
      double ddy = p1.y-p2.y;
      double dist = mouseDist(v);
      boolean rep = Math.sqrt(ddx*ddx + ddy*ddy)<=dist;
      return rep;

      
//      double ddx = x-xv[v.n];
//      double ddy = y-yv[v.n];
//      double dist = mouseDist(v);
//      if( Ligne.isDebLigne(this) ) dist*=2;
//      boolean rep = Math.sqrt(ddx*ddx + ddy*ddy)<=dist;
//            if( rep ) {
//               System.out.println("xyv=("+xv[v.n]+","+yv[v.n]+") dist="+Math.sqrt(ddx*ddx + ddy*ddy)+" mouseDist="+mouseDist(v));
//            }
//      return rep;
   }

   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve
    * - soit a proximite de l'extremite courante du segment
    * - soit sur le segment mais eloigne des deux extremites
    * @param x,y le point a tester
    * @param fz le zoom
    */
   protected boolean inside(ViewSimple v, double x,double y) {
      //      if( bout==3 ) return inPolygon(v,(int)x,(int)y);

      // Cas courant
      return nearArrow(v,x,y);
   }

   /** Redessine tout le polygone avec un aplat - ne fonctionne que pour le dernier segment */
   protected boolean inPolygon(ViewSimple v,int x, int y) {
      if( bout!=3 ) return false;
      Ligne tmp = this;
      Polygon pol = new Polygon();
      while( tmp.debligne!=null ) {
         pol.addPoint((int)tmp.xv[v.n],(int)tmp.yv[v.n]);
         tmp=tmp.debligne;
      }
      return pol.contains(x, y);
   }


   /** Coordonnees dans la vue courante.
    * Retourne la position dans les coord. de la vue courante
    * Memorise l'etat du zoom pour s'eviter du travail ulterieur
    * @param zoomview reference au zoom courant
    * @return         les coordonnees
    */
   protected Point getViewCoord(ViewSimple v) {
      /* if( !Aladin.VP ) */ return v.getViewCoord(xv[v.n],yv[v.n]);
      //      vp[v.n] = v.getViewCoord(xv[v.n],yv[v.n]);
      //      oiz[v.n]=v.iz;
      //      return vp[v.n];
   }

   /** Generation d'un clip englobant.
    * Retourne un rectangle qui englobe la ligne dans les coord de la vue
    * En fait on traite le cas general avec 3 points (debligne,this,finligne)
    * @param zoomview reference au zoom courant
    * @return         le rectangle enblobant
    */
   //   protected Rectangle getClip(ViewSimple v) {
   //      if( !visible ) return null;
   //      Point [] p = new Point[4];
   //      int x1=2048,y1=2048,x2=0,y2=0;
   //
   //      p[0] = (debligne!=null)?debligne.getViewCoord(v):null;
   //      p[1] = getViewCoord(v);
   //      p[2] = (finligne!=null)?finligne.getViewCoord(v):null;
   //
   //      // Recherche des min et max
   //      for( int i=0; i<4; i++ ) {
   //         if( p[i]==null ) continue;
   //         if( p[i].x<x1 ) x1=p[i].x;
   //         if( p[i].y<y1 ) y1=p[i].y;
   //         if( p[i].x>x2 ) x2=p[i].x;
   //         if( p[i].y>y2 ) y2=p[i].y;
   //      }
   //
   //      // Un peu de marge
   //      int gap = 1;
   //      if( bout>0 || select ) gap=DL;
   //      x1-=gap; y1-=gap; x2+=gap; y2+=gap;
   //
   //      return new Rectangle(x1,y1,x2-x1+1,y2-y1+1);
   //   }
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      Point p,p0,p1,p2;
      int x1=2048,y1=2048,x2=0,y2=0;

      p0 = (debligne!=null)?debligne.getViewCoord(v):null;
      p1 = (finligne!=null)?finligne.getViewCoord(v):null;
      p2 = getViewCoord(v);

      // Recherche des min et max
      for( int i=0; i<3; i++ ) {
         p = i==0 ? p0 : i==1 ? p1 : p2;
         if( p==null ) continue;
         if( p.x<x1 ) x1=p.x;
         if( p.y<y1 ) y1=p.y;
         if( p.x>x2 ) x2=p.x;
         if( p.y>y2 ) y2=p.y;
      }

      // Un peu de marge
      int gap = 1;
      if( bout>0 || isSelected() ) gap=DL;
      x1-=gap; y1-=gap+clipYId(); x2+=gap+clipXId(); y2+=gap+clipYId();

      // Peut être y a-t-il des stats affichées sur le côté
      if( bout==3 && hasOneSelected() ) clip = unionRect(clip,getStatPosition(v));

      return unionRect(clip, x1,y1,x2-x1+1,y2-y1+1);
   }

   /** Recopie du code de extendClip() mais permet d'accélérer considérablement
    * le tracé des contours en évitant de passer par la classe mère qui fait un getClip(),
    * et donc une création de Rectangle() à chaque fois. Je ne peux pas utiliser des
    * variables d'instances sans exploser la mémoire pour pas grand chose
    */
   protected boolean inClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return false;
      Point p,p0,p1,p2;
      int x1=2048,y1=2048,x2=0,y2=0;

      p0 = (debligne!=null)?debligne.getViewCoord(v):null;
      p1 = (finligne!=null)?finligne.getViewCoord(v):null;
      p2 = getViewCoord(v);

      // Recherche des min et max
      for( int i=0; i<3; i++ ) {
         p = i==0 ? p0 : i==1 ? p1 : p2;
         if( p==null ) continue;
         if( p.x<x1 ) x1=p.x;
         if( p.y<y1 ) y1=p.y;
         if( p.x>x2 ) x2=p.x;
         if( p.y>y2 ) y2=p.y;
      }

      // Un peu de marge
      int gap = 1;
      if( bout>0 || isSelected() ) gap=DL;
      x1-=gap; y1-=gap+clipYId(); x2+=gap+clipXId(); y2+=gap+clipYId();

      // Peut être y a-t-il des stats affichées sur le côté
      if( bout==3 && hasOneSelected() ) {
         clip = unionRect(clip,getStatPosition(v));
      }

      return intersectRect(clip, x1,y1,x2-x1+1,y2-y1+1);
   }

   protected int clipXId() { return 0; }
   protected int clipYId() { return 0; }

   /** Détermination de la couleur de l'objet */
   public Color getColor() {
      if( couleur!=null ) return couleur;
      if( plan!=null && plan.type==Plan.APERTURE ) {
         couleur = ((PlanField)plan).getColor(this);
         if( couleur==null ) return plan.c;
         return couleur;
      }
      if( plan!=null ) return plan.c;
      return Color.black;
   }

   class Segment implements Comparator {
      double x1,y1,x2,y2;
      double dx,dy,dxb;
      double d;
      boolean cut;
      boolean out;

      Segment(double a,double b,double a1, double b1) {
         x1=a; y1=b; x2=a1; y2=b1;
         out=false;
         dx=x2-x1;
         dy=y2-y1;
         dxb=dx*y1-dy*x1;
      }

      void init() {
         d=Double.NaN;
         cut=false;
      }

      /** détermine si on traverse le segment par changement de signe */
      boolean cut(double x, double y) {
         if( out ) return false;
         if( y<y1 ) return false;
         if( y>=y2 ) { out=true; return false; }
         if( cut ) return true;

         double d1 = y*dx - x*dy - dxb;
         if( Double.isNaN(d) ) d=d1;
         else cut = d<0 && d1>=0 || d>=0 && d1<0;
         d1=d;
         return cut;
      }

      public int compare(Object arg0, Object arg1) {
         Segment a = (Segment)arg0;
         Segment b = (Segment)arg1;
         return a.y1==b.y1 ? 0 : a.y1<b.y1? -1 :1;
      }

      //      public String toString() {
      //         return Util.myRound(""+x1,2)+","+Util.myRound(""+y1,2)+" - "
      //         +Util.myRound(""+x2,2)+","+Util.myRound(""+y2,2)+" dxb="+dxb+" out="+out+" cut="+cut;
      //      }
   }

   /** Set specifical color (dedicated for catalog sources) */
   public void setColor(Color c) {
      for( Ligne lig = getFirstBout(); lig!=null; lig = lig.finligne) lig.couleur=c;
   }



   //   private ArrayList<Vec3> verto = null;
   //
   //   private void computeMoc() {
   //      try {
   //         long t = System.currentTimeMillis();
   //
   //         ArrayList<Vec3> vert = new ArrayList<Vec3>();
   //         for( Ligne deb=getFirstBout(); deb!=null; deb=deb.finligne ) {
   //            if( deb.finligne!=null ) {
   //               double theta = Math.PI/2 - Math.toRadians( deb.dej );
   //               double phi = Math.toRadians( deb.raj );
   //               vert.add(new Vec3(new Pointing(theta,phi)));
   //            }
   //         }
   //         if( verto!=null /* && verto.equals(vert) */ ) return;
   //         verto=vert;
   //
   //         Moc moc=MocQuery.queryGeneralPolygon (vert,15);
   //
   //         for(  Ligne deb=getFirstBout(); deb!=null; deb=deb.finligne ) {
   //            if( deb.finligne!=null ) {
   //               double theta = Math.PI/2 - Math.toRadians( deb.dej );
   //               double phi = Math.toRadians( deb.raj );
   //               System.out.println("vert.add(new Vec3(new Pointing("+theta+","+phi+")))");
   //            }
   //         }
   //
   //         long t1 = System.currentTimeMillis();
   //         System.out.println(vert.size()+" vertex Moc compute in "+Util.getTemps(t1-t));
   //         String s = MocUtil.mocToStringJSON(moc);
   //         System.out.println(s);
   //         HealpixMoc m = new HealpixMoc(s);
   //         plan.aladin.calque.newPlanMOC(m, "MocRegion");
   //
   //      } catch( Exception e ) {
   //         e.printStackTrace();
   //      }
   //   }

   /**
    * Méthode:
    * 1) on trie les segments en ordonnée croissante
    * 2) on balaye la boite englobante ligne par ligne
    *    2.1) on compte le nombre d'intersection du x courant
    *         avec les segments susceptibles d'être concernés.
    *         S'il s'agit d'un nombre impair, le pixel est dedans
    * On en profite pour mettre à jour posx,posy qui va indiquer
    * le point d'accrochage de la légende afin d'éviter le cas genre
    * "banane", où le barycentre est en dehors du polygone
    */
   protected boolean statCompute(Graphics g,ViewSimple v) {

      if( bout!=3 ) return false;

      if( v!=null && !v.isFree() && v.pref.type==Plan.ALLSKYIMG ) {
         //         ((PlanBG)v.pref).setDebugIn(this);
         return false;
      }

      if( v==null || v.isFree() || !hasPhot(v.pref) ) return false;

      boolean isHiPS = (v.pref.type == Plan.ALLSKYIMG);


      statInit();

      //      int x,y,i;
      double x,y;
      int i;
      int nb;
      Ligne tmp,deb,a,b;
      boolean flagHist = v==v.aladin.view.getCurrentView();

      minx=miny=Integer.MAX_VALUE;
      maxx=maxy=Integer.MIN_VALUE;

      double minx1,maxx1,miny1,maxy1, mind,maxd, mina,maxa;
      mina=mind=minx1=miny1=Integer.MAX_VALUE;
      maxa=maxd=maxx1=maxy1=Integer.MIN_VALUE;


      tmp = deb=getFirstBout();
      for( nb=0; tmp.finligne!=null; tmp=tmp.finligne) nb++;
      Segment [] seg = new Segment[nb];

      for( i=0, tmp=deb; tmp.finligne!=null; tmp=tmp.finligne, i++ ) {
         int fx=(int)Math.floor(tmp.xv[v.n]-0.5);
         int tx=(int)Math.ceil(tmp.xv[v.n]-0.5);
         int fy=(int)Math.floor(tmp.yv[v.n]-0.5);
         int ty=(int)Math.ceil(tmp.yv[v.n]-0.5);
         if( tx>maxx ) maxx=tx;
         if( fx<minx ) minx=fx;
         if( ty>maxy ) maxy=ty;
         if( fy<miny ) miny=fy;

         if( tmp.xv[v.n]<minx1 ) { minx1=tmp.xv[v.n]; mina=tmp.raj; }
         if( tmp.xv[v.n]>maxx1 ) { maxx1=tmp.xv[v.n]; maxa=tmp.raj; }
         if( tmp.yv[v.n]<miny1 ) { miny1=tmp.yv[v.n]; mind=tmp.dej; }
         if( tmp.yv[v.n]>maxy1 ) { maxy1=tmp.yv[v.n]; maxd=tmp.dej; }

         a=tmp; b=tmp.finligne;
         if( tmp.yv[v.n]> tmp.finligne.yv[v.n] ) { b=tmp ; a=tmp.finligne; }

         if( isHiPS ) seg[i] = new Segment(a.raj,a.dej, b.raj,b.dej);
         else seg[i] = new Segment(a.xv[v.n]-0.5,a.yv[v.n]-0.5, b.xv[v.n]-0.5,b.yv[v.n]-0.5);
      }

      Arrays.sort(seg, seg[0]);

      HistItem onMouse=null;
      if( flagHist ) {
         onMouse =v.aladin.view.zoomview.hist==null ? null :  v.aladin.view.zoomview.hist.onMouse;

         // Si le est simplement dû au passage de la souris sur un précédent histogramme,
         // il ne faut pas regénérer cet histogramme
         if( onMouse==null ) v.aladin.view.zoomview.initPixelHist();
         else flagHist=false;
      }

      // Position prévue pour l'accrochage de la légende
      double deuxTiersX = minx1+2*(maxx1-minx1)/3.;
      double unTierY    = miny1+(maxy1-miny1)/3.;

      if( isHiPS ) {


         PlanBG pbg = (PlanBG) v.pref;
         double d = CDSHealpix.pixRes( CDSHealpix.pow2( pbg.getOrder() + pbg.getTileOrder() ) ) / 3600;
         d /= 2;   // Pour être sur de ne pas sauter une ligne
         System.out.println("From "+Coord.getUnit(mind)+" to "+Coord.getUnit(maxd)+" Delta = " + Coord.getUnit(d) );
         Coord haut = new Coord( (maxa+mina)/2, mind);
         Coord bas  = new Coord( (maxa+mina)/2, maxd);
         double dist = Coord.getDist(haut, bas);
         int n = (int) (dist/d);
         System.out.println("Distance entre "+haut+"  et "+bas+" => "+Coord.getUnit(dist)+" soit "+n+" itérations");

         return false;

      } else {

         int n=0;   // Premier segment à tester
         for( y=miny; y<=maxy; y++ )  {
            for( ; n<nb && seg[n].out; n++ );
            for( i=n; i<nb; i++ ) seg[i].init();
            if( posy==-1 && (int)y==(int)unTierY ) posy=unTierY;
            for( x=maxx+1; x>=minx-1; x-- ) {
               int inter=0;  // Nombre de segments intersectés
               for( i=n; i<nb; i++ ) {
                  if( seg[i].cut(x,y) ) inter++;
               }
               if( inter%2==1 ) {
                  double pix = statPixel(g, (int)x, (int)y, v, onMouse);
                  if( flagHist ) plan.aladin.view.zoomview.addPixelHist(pix);
                  if( posy!=-1 && (int)x==(int)deuxTiersX && posx==-1 ) posx=deuxTiersX;
               } else {
                  if( posy!=-1 && posx==-1 && inter>0) posx=x+1;
               }
            }
         }

         minx=minx1; maxx=maxx1; miny=miny1; maxy1=maxy;
      }

      if( flagHist ) plan.aladin.view.zoomview.createPixelHist("Pixels");


      // Calculs des statistiques => sera utilisé immédiatement par le paint
      // Attention, il s'agit de variables statiques
      try {
         double pixelSurf = plan.proj[v.n].getPixResAlpha()*plan.proj[v.n].getPixResDelta();
         surface = nombre*pixelSurf;
         moyenne = total/nombre;
         variance = carre/nombre - moyenne*moyenne;
         sigma = Math.sqrt(variance);
         if( medianeArrayNb==MAXMEDIANE ) mediane=Double.NaN;
         else {
            Arrays.sort(medianeArray,0,medianeArrayNb);
            mediane = medianeArray[medianeArrayNb/2];
         }
         setWithStat(true);
      } catch( Exception e ) { e.printStackTrace(); }

      return true;
   }

   /** Retourne true que sur le dernier segment du polygone pour éviter les doublons */
   public boolean hasSurface() { return bout==3; }

   public double [] getStatistics(Plan p) throws Exception {

      Projection proj = p.projd;
      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
      if( getLastBout().bout!=3 ) throw new Exception("getStats error: it is not a polygon");

      int x,y,i;
      int nb;
      Coord ac = new Coord();
      Coord bc = new Coord();
      Ligne tmp,deb;

      tmp = deb=getFirstBout();
      for( nb=0; tmp.finligne!=null; tmp=tmp.finligne) nb++;
      Segment [] seg = new Segment[nb];

      int minx,maxx;
      int miny,maxy;
      minx=miny=Integer.MAX_VALUE;
      maxx=maxy=Integer.MIN_VALUE;

      for( i=0, tmp=deb; tmp.finligne!=null; tmp=tmp.finligne, i++ ) {
         ac.al = tmp.raj;
         ac.del = tmp.dej;
         proj.getXY(ac);

         bc.al = tmp.finligne.raj;
         bc.del = tmp.finligne.dej;
         proj.getXY(bc);

         int fx=(int)Math.floor(ac.x-0.5);
         int tx=(int)Math.ceil(ac.x-0.5);
         int fy=(int)Math.floor(ac.y-0.5);
         int ty=(int)Math.ceil(ac.y-0.5);
         if( tx>maxx ) maxx=tx;
         if( fx<minx ) minx=fx;
         if( ty>maxy ) maxy=ty;
         if( fy<miny ) miny=fy;

         if( ac.y > bc.y ) seg[i] = new Segment(bc.x-0.5,bc.y-0.5, ac.x-0.5,ac.y-0.5);
         else seg[i] = new Segment(ac.x-0.5,ac.y-0.5, bc.x-0.5,bc.y-0.5);
      }

      Arrays.sort(seg, seg[0]);

      double nombre=0;
      double carre=0;
      double total=0;
      double moyenne, variance, sigma ,surface;

      int n=0;   // Premier segment à tester
      for( y=miny; y<=maxy; y++ )  {
         for( ; n<nb && seg[n].out; n++ );
         for( i=n; i<nb; i++ ) seg[i].init();
         for( x=maxx+1; x>=minx-1; x-- ) {
            int inter=0;  // Nombre de segments intersectés
            for( i=n; i<nb; i++ ) {
               if( seg[i].cut(x,y) ) inter++;
            }
            if( inter%2==1 ) {
               double pix= ((PlanImage)p).getPixelInDouble(x,y);
               if( Double.isNaN(pix) ) continue;
               nombre++;
               total+=pix;
               carre+=pix*pix;
            }
         }
      }

      double pixelSurf = proj.getPixResAlpha()*proj.getPixResDelta();
      surface = nombre*pixelSurf;
      moyenne = total/nombre;
      variance = carre/nombre - moyenne*moyenne;
      sigma = Math.sqrt(variance);

      return new double[]{ nombre, total, sigma, surface };
   }

   protected void drawID(Graphics g , ViewSimple v,Point p1,Point p2) { }


   /** Retourne la fin de la polyligne */
   protected Ligne getLastBout() {
      Ligne tmp = this;
      while( tmp.finligne!=null ) tmp=tmp.finligne;
      return tmp;
   }

   /** Retourne le début de la polyligne */
   protected Ligne getFirstBout() {
      Ligne tmp = this;
      while( tmp.debligne!=null ) tmp=tmp.debligne;
      return tmp;
   }

   /** Retourne true si au-moins un sommet de la polyligne a été sélectionné */
   protected boolean hasOneSelected() {
      if( isSelected() ) return true;
      Ligne tmp = getFirstBout();
      while( tmp.finligne!=null && !tmp.isSelected() ) tmp=tmp.finligne;
      return tmp.isSelected();
   }

   protected void statDraw(Graphics g,ViewSimple v,int dx, int dy) {
      super.statDraw(g,v,dx,dy);
      getFirstBout().id=id;
   }


   //   static final int BETA = (int)(1./Math.tan(90*Math.PI/180));
   //   protected boolean outOfSky(ViewSimple v) {
   //      double al1,del1;
   //      double al2,del2;
   //      double al,del;
   //      Point p1,p2,p;
   //
   //      if( debligne==null ) return false;
   //
   //      al1=raj; del1=dej; al2=debligne.raj; del2=debligne.dej;
   //      al = (al2+al1)/2;
   //      del = (del2+del1)/2;
   //
   //      p1 = debligne.getViewCoord(v);
   //      p2 = getViewCoord(v);
   //      p = (new Ligne(v,al,del)).getViewCoord(v);
   //
   //      double dx1 = p.x-p1.x; //s1.x2-s1.x1;
   //      double dy1 = p.y-p1.y;  //s1.y2-s1.y1;
   //      double dx2 = p2.x-p.x; // s2.x2-s2.x1;
   //      double dy2 = p2.y-p.y; //s2.y2-s2.y1;
   //
   //      double theta1 = Math.atan2(dy1, dx1);
   //      double theta2 = Math.atan2(dy2, dx2);
   //      double angle = Math.abs(theta1-theta2);
   //
   //      System.out.println(p1.x+","+p1.y+" -> "+p.x+","+p.y+" -> "+p2.x+","+p2.y+" => "+(angle*180)/Math.PI);
   //
   //      return Math.abs(dx1*dy2-dx2*dy1) > 1+ Math.abs(dx1*dx2+dy1*dy2)/BETA;
   //   }


   // Bidouillage pour éviter de traverser tout le ciel en passant derrière
   protected boolean tooLarge(ViewSimple v,Point p1, Point p2) {
      Projection proj =  v.getProj();
      if( proj==null || proj.t==Calib.SIN ) return false;
      if( !v.isAllSky() ) return false;

      double dx = p1.x-p2.x;
      double dy = p1.y-p2.y;
      double dist = Math.sqrt(dx*dx+dy*dy);

      return dist/v.rv.width>1/2.;
   }

   /** Trace de la portion de la ligne.
    * @param g        le contexte graphique
    * @param zoomview reference au zoom courant
    */
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) {
      if( !isVisible() ) return false;


      //      if( outOfSky(v) ) return false;

      if( !hidden ) {
         Point p2;
         g.setColor( getColor() );

         if( debligne!=null ) {

            // Le segment
            p2 = getViewCoord(v);
            Point p1 = debligne.getViewCoord(v);
            if( p2==null || p1==null ) return false;
            p1.x+=dx; p1.y+=dy;
            p2.x+=dx; p2.y+=dy;

            if( tooLarge(v,p1,p2) ) return false;

            g.drawLine(p1.x,p1.y, p2.x,p2.y);
            if( bout==3 && hasPhot(v.pref) ){
               fillPolygon(g, v, dx, dy);
               if( hasOneSelected() ) statDraw(g,v,dx,dy);
            }
            drawID(g,v,p1,p2);

            // Trace des fleches
            if( bout==1 || bout==2 ) {
               double theta,delta;
               if( p1.x!=p2.x) {
                  theta = Math.atan( (double)(p2.y-p1.y)/(p2.x-p1.x) );
                  if( p1.x>p2.x ) theta += Math.PI;
               } else {
                  if( p1.y<p2.y ) theta = Math.PI/2;
                  else theta = -Math.PI/2;
               }
               delta = 3.0*Math.PI/4;
               int dx1 = (int)( L*FastMath.cos( theta+delta) );
               int dy1 = (int)( L*FastMath.sin( theta+delta) );
               int dx2 = (int)( L*FastMath.cos( theta-delta) );
               int dy2 = (int)( L*FastMath.sin( theta-delta) );
               g.drawLine(p2.x+dx1,p2.y+dy1,p2.x,p2.y);
               g.drawLine(p2.x,p2.y,p2.x+dx2,p2.y+dy2);
               if( bout==2 ) {
                  g.drawLine(p1.x-dx1,p1.y-dy1,p1.x,p1.y);
                  g.drawLine(p1.x,p1.y,p1.x-dx2,p1.y-dy2);
               }
            }
         } else {
            p2 = getViewCoord(v,0,0);
            if( p2==null ) return false;
            p2.x+=dx; p2.y+=dy;
            if( bout==4 ) Util.fillCircle5(g, p2.x,p2.y);
         }

         // La poignee de selection
         if( isSelected() ) {
            if( plan!=null && plan.type==Plan.APERTURE ) return true;
            int ds=DS/2;
            g.setColor( Color.green );
            g.fillRect( p2.x-ds+1, p2.y-ds+1, DS-1,DS-1 );
            g.setColor( Color.black );
            g.drawRect( p2.x-ds, p2.y-ds, DS,DS );
         }
      }
      return true;
   }

   // Recupération d'un itérator sur les objets qui compose la forme (Ligne, Cote)
   public Iterator<Obj> iterator() { return new ObjetIterator(); }

   class ObjetIterator implements Iterator<Obj> {
      private Ligne line = getFirstBout();
      public boolean hasNext() { return line.finligne!=null && line.bout!=3; }
      public Obj next() { return line=line.finligne; }
      public void remove() { }
   }




}
