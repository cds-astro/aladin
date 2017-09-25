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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.OutputStream;

import cds.aladin.Hist.HistItem;
import cds.tools.Util;
import healpix.essentials.FastMath;

/**
 * Manipulation d'un objet graphique affichable dans la vue
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Position extends Obj {

   // Les autres constantes
   static final int DS  = 4;         // Taille des poignees de selection
   static final int DDS = DS*2;      // Decalage du aux poignees de selection
   static final int HF  = Aladin.SIZE;
   static final Font DF = Aladin.BOLD;

   protected double x,y;        // Position initiale de l'objet en X,Y (soit catalogue sans coordonnée, soit graphique sans calib)
   protected double xv[],yv[];   // Position de l'objet pour chaque vue

   /** Variables statiques utilisées pour le calcul des statistiques sur un polygone */
   static double minx,maxx;
   static double miny,maxy;
   static double posx,posy;    // position pour l'accrochage du label (-1,-1 si non fourni)
   static double total;
   static double carre;
   static int nombre;
   static double surface;
   static double moyenne;
   static double variance;
   static double sigma;
   static double minimum,maximum;
   static double mediane;
   static final int MAXMEDIANE = 10000;
   static int medianeArrayNb=0;
   static double [] medianeArray = new double[MAXMEDIANE];


   /*
   Un peu d'explication sur les caches pour les objets graphiques Aladin en multivues.
   La classe Position gère 4 tableaux de cache :

   1) xv[]
   2) yv[] position de l'objet en x,y dans la projection du plan de référence de chaque vue
           (relis, tu vas comprendre !)

   xv[] et yv[] sont remis à jour pour une vue donnée par la méthode Position.projection(ViewSimple v)
   - elle-même appelée par ViewSimple.projection() si nécessaire (champs se superposant
   au-moins partiellement et/ou projection différente de celle qui a servi pour le dernier calcul
   - via le tableau PlanObjet.proj[] qui contient la dernière projection utilisée pour chaque vue
   pour le plan en question), elle-même appelée par la méthode PlanObjet.Draw(Graphics g).

   On peut forcer le recalcul des xv[], yv[] pour une vue donnée en appelant ViewSimple.newView().
   Cette méthode va mettre à null le PlanObjet.proj[] pour la vue en question, d'où le recaclul.
   On peut également appeler View.newView() qui fait de même mais pour toutes les vues.
   */

   protected void createCacheXYVP() {
//      createCacheXYVP( plan==null ? ViewControl.MAXVIEW : plan.type==Plan.X ? 0:plan.aladin.view.getNbView());
      createCacheXYVP( plan==null ? ViewControl.MAXVIEW : plan.aladin.view.getNbView());
   }
   protected void createCacheXYVP(int dim) {
     if( dim==0 ) return;
     xv=new double[dim];
     yv=new double[dim];
   }

   public Position() {}

  /** Creation d'un objet graphique vide (pour les backups) */
   protected Position(Plan plan) { this.plan=plan; createCacheXYVP(); }


  /**  Creation d'un objet graphique (methode generalisee)
   * @param plan plan d'appartenance de la ligne
   * @param x,y  position XY
   * @param raj,dej  position RA,DEC
   * @param methode masque de bits pour indiquer quels sont les champs qu'on
   *                doit mettre a jour :
   *                   XY : la position (x,y) dans les coordonnees de l'image
   *                   RADE : la position RA,DEC
   *                   RADE_COMPUTE : la position RA,DEC est deduite de XY
   *                          en fonction de l'astrometrie courante
   *                   XY_COMPUTE : la position XY est deduite de RA,DEC
   *                          en fonction de l'astrometrie courante
   * @param id   identificateur associe a l'objet graphique
   */
   protected Position(Plan plan, ViewSimple v,double x, double y,
                                 double raj, double dej,int methode,
                                 String id) {
      this.id=id;
      this.plan=plan;
      createCacheXYVP();

      // On me passe un XY
      if( (methode & XY)!=0 ) {
         if( v!=null ) { xv[v.n]=x; yv[v.n]=y; }

         // Pas de calibration astrométrique associée ?
         if( v==null || v.pref==null || !Projection.isOk(v.getProj()) ) {
            this.x=x;
            this.y= (v==null?y:v.getPRefDimension().height-y);
         }

         // Calcul des coordonnées correspondantes ?
         else if( (methode & RADE_COMPUTE)!=0 ) {
            setCoord(v);
         }
      }

      // On me passe des coord.
      if( (methode & RADE)!=0 ) {
         this.raj=raj;
         this.dej=dej;
         if( (methode & XY_COMPUTE)!=0 ) {
            try { projection(v); } catch( Exception e ) { }
         }
      }
   }

   // Utilise par les Sources et par les reperes captures
   protected Position(Plan plan, String id) {
      this.id=id;
      this.plan=plan;
      createCacheXYVP();
   }
   
   /** Retourne la localisation de la source dans le frame courant */
   public String getLocalisation() {
      String s = plan.aladin.localisation.getLocalisation(this);
      if( s==null ) return "null,null";
      s=s.replace(' ', ',');
      return s;
   }

   /** Dans le cas de vues synchronisées, cette méthode permet de recopier
    * les variables xv et yv
    * @param vs viewSimple source (qui possède les bonnes valeurs)
    * @param vt viewSimple target
    */
   protected void syncPos(ViewSimple vs,ViewSimple vt) {
      xv[vt.n]=xv[vs.n];
      yv[vt.n]=yv[vs.n];
   }

   /** Positionne le flag VISIBLE */
   protected final void setVisible(boolean visible) {
      if( visible ) flags |= VISIBLE;
      else flags &= ~VISIBLE;
   }

   /** Retourne true si la source a le flag VISIBLE positionné */
   final protected boolean isVisible() { return (flags & VISIBLE) !=0; /* == VISIBLE;*/ }

   /** Positionne le flag LABEL */
   protected final void setWithLabel(boolean withLabel) {
      if( withLabel ) flags |= WITHLABEL;
      else flags &= ~WITHLABEL;
   }

   /** Positionne le flag STAT */
   protected final void setWithStat(boolean withStat) {
      if( withStat ) flags |= WITHSTAT;
      else flags &= ~WITHSTAT;
   }

   /** Positionne le flag LOCKED */
   protected final void setLocked(boolean movable) {
      if( movable ) flags |= LOCKED;
      else flags &= ~LOCKED;
   }

   /** Retourne true si la source a le flag WITHLABEL positionné */
   final protected boolean isWithLabel() { return (flags & WITHLABEL) !=0; }

   /** Retourne true si la source a le flag WITHSTAT positionné */
   final protected boolean isWithStat() { return (flags & WITHSTAT) !=0; }

   /** Retourne true si la source a le flag LOCKED positionné */
   final protected boolean isLocked() { return (flags & LOCKED) !=0; }


   protected void setCoord(ViewSimple v) {
      Coord c = new Coord();
      boolean ok=false;

      Projection proj;
      if( Projection.isOk(proj=v.getProj()) ) {
         c.x=xv[v.n]; c.y=yv[v.n];
         proj.getCoord(c);
         raj = c.al;
         dej = c.del;
         ok=true;
      }

      if( !ok && plan instanceof PlanTool ) {
        ((PlanTool)plan).setXYorig(true);
      }

      if( ok ) {
         View view= plan.aladin.view;
         int m = view.getNbView();
         for( int i=0; i<m; i++ ) {
            ViewSimple vx = view.viewSimple[i];
            if( vx!=v && !vx.isFree() ) projection(vx);
         }
      }
   }

   protected void setCoord(ViewSimple v,Projection proj) {
      Coord c = new Coord();
      // J'affecte les coordonnees associees a (x,y)
      c.x=xv[v.n]; c.y=yv[v.n];
      proj.getCoord(c);
      raj = c.al;
      dej = c.del;
   }

   /**
    * Calcul de la position XY natif en fonction de alpha,delta
    * @param proj
    */
   protected void setXY(Projection proj) {
      Coord c = new Coord();
      c.al = raj; c.del = dej;
      proj.getXY(c);
      if( !Double.isNaN(c.x) ) {
         x = Math.round(c.x*1000)/1000.;
         y = proj.r1-Math.round(c.y*1000)/1000.;
      }
   }

   /**
    * Modifie les x,y dans le plan tangent (sert pour les plan FOV) */
   protected void setXYTan(double x, double y) {
      this.x = x;
      this.y = y;
   }
   
   /**
    * Modifie les x,y dans le plan tangent en fonction du centre de projection (sert pour les plan FOV) */
   protected void setXYTan(Coord center) {
      x = Math.toRadians( Math.cos(Math.toRadians(center.del))*(raj - center.al) );
      y = Math.toRadians( dej - center.del);
//      x = Util.tand( Math.cos(Math.toRadians(center.del))*(raj - center.al));
//      y = Util.tand( dej - center.del);
   }
   
//   /** Modifie la propriété "rotable" de l'objet */
//   protected void setRollable(boolean rollable) { this.rollable=rollable; }


   // PEUT ETRE UN PEU DANGEREUX DE LA METTRE EN STATIC ?
//   static protected Coord c = new Coord();	// Pour eviter les new inutiles

  /** Projection de la source => calcul (x,y).
   * @param proj la projection a utiliser
   */
   protected void projection(ViewSimple v) {

      Projection proj;
      Coord c = new Coord();
      
      if( plan!=null && plan.hasNoPos ) return;
      
      // S'il n'y a pas de calibration, on prend les x,y natifs
      if( (plan!=null && plan.hasXYorig) || !Projection.isOk(proj=v.getProj()) ) {
         xv[v.n]=x-0.5;
         yv[v.n]= v.getPRefDimension().height-y+0.5;

      // Sinon on calcule les xy en fonction des coord. ra,de
      } else {
         // Calcul initial d'un objet n'ayant des des positions xy natives
         if( raj==Double.NaN && xv[v.n]!=0 ) setCoord(v,proj);

         c.al  = raj;
         c.del = dej;
         proj.getXY(c);
         xv[v.n] = c.x;
         yv[v.n] = c.y;
      }
   }
   
   /** Retourne le plan d'appartenance de l'objet */
   protected Plan getPlan() { return plan; }

  /** Modification de la position (absolue)
   * @param x,y nouvelle position
   */
   protected void setPosition(ViewSimple v,double x, double y) { setPosition1(v,x,y); }
   protected void setPosition1(ViewSimple v,double x, double y) {
      // Positionnement d'un objet n'ayant que des positions x,y natives
      if( raj==Double.NaN ) {
         this.x = x;
         this.y = v.getPRefDimension().height-y;
      }

      xv[v.n] = x;
      yv[v.n] = y;
      setCoord(v);
   }

  /** Modification de la position en xy (relative)
   * @param dx,dy decalages
   */
   protected void deltaPosition(ViewSimple v,double dx, double dy) { deltaPosition1(v,dx,dy); }
   protected void deltaPosition1(ViewSimple v,double dx, double dy) {
      // Déplacement d'un objet n'ayant que des positions x,y natives
      if( raj==Double.NaN ) {
         x+= dx;
         y-= dy;
      }

      xv[v.n] += dx;
      yv[v.n] += dy;
      setCoord(v);
  }
   

   /** Modification de la position en ra,dec (relative)
    * C'EST BIZARRE, CA MARCHE MIEUX SANS TESTER LES DEPASSEMENT EN ra,dec
    * @param dra,dde decalages
    */
   protected void deltaRaDec(double dra, double dde) { deltaRaDec1(dra,dde); }
   protected void deltaRaDec1(double dra, double dde) {
      raj+=dra;
      dej+=dde;
//      if( dej>90.) { dej=180-dej; raj+=180.; }
//      if( dej<90.) { dej=-180-dej; raj+=180.; }
//      if( raj>=360.) raj-=360;
//      if( raj<0.) raj+=360;
      View view= plan.aladin.view;
      int m = view.getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = view.viewSimple[i];
         if( !v.isFree() ) projection(v);
      }
   }

   /** Application d'une rotation (relative)
    * @param theta angle de rotation en radians
    * @param x0,y0 centre de la rotation
    */
   protected void rotatePosition(ViewSimple v,double theta,double x0,double y0) {
      double x = xv[v.n]-x0;
      double y = yv[v.n]-y0;
      double cost,sint;
      xv[v.n] = x0+ x*(cost=FastMath.cos(theta)) - y*(sint=FastMath.sin(theta));
      yv[v.n] = y0+ x*sint + y*cost;
      setCoord(v);
   }

  /** Modification de l'identificateur
   * @param id nouvel identificateur
   */
   protected void setText(String id) { this.id = id; }

   /** permutation du flag de selection */
   protected void switchSelect() {
      setSelect( !isSelected() );
   }

   /** Retourne Vrai si l'objet est selectionne */
//   protected boolean isSelect() { return isFlagSelected(); }

   /** Positionnement du flag visible */
   protected void setVisibleGenerique(boolean visible) { setVisible(visible); }
   
  /** Coordonnees dans la vue courante.
   * Retourne la position dans les coord. de la vue courante.
   * Memorise l'etat du zoom pour s'eviter du travail ulterieur.
   * Tolere une marge (dw,dh) de bordure, sinon null.
   * @param v la vue courante
   * @return         les coordonnees
   */
   protected Point getViewCoord(ViewSimple v,int dw, int dh) {
      if( Double.isNaN(xv[v.n]) ) return null;
      return v.getViewCoordWithMarge(null,xv[v.n],yv[v.n],dw,dh);
   }

   // ajout Thomas pour generation LINK avec coordonnees en flottants
   protected PointD getViewCoordDouble(ViewSimple v,int dw, int dh) {
       if( Double.isNaN(xv[v.n]) ) return null;
       return v.getViewCoordDoubleWithMarge(null,xv[v.n],yv[v.n],dw,dh);
    }

   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve dans l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inside(ViewSimple v, double x, double y) {
//       double a=mouseDist(v);
//       return Math.abs(xv[v.n]-x)<a && Math.abs(yv[v.n]-y)<a;
       return inBout(v,x,y);
    }

    /** Test d'appartenance sur un bout
     * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
     * @param v la vue courante
     * @param x,y le point a tester
     */
     protected boolean inBout(ViewSimple v, double x, double y) {
        PointD p = v.getViewCoordDble(x, y);
        PointD p1 = v.getViewCoordDble(xv[v.n], yv[v.n]);
        double d =  mouseDist(v);
        return p1.x*p1.x + p.x*p.y < d*d;
        
//        double d =  mouseDist(v);
//        d = d*d*4;
//        return (xv[v.n]-x) * (xv[v.n]-x) + (yv[v.n]-y) * (yv[v.n]-y) < d;
     }

  /** Test d'appartenance a l'objet
   * Retourne vrai si le point (x,y) de la vue se trouve sur l'objet
   * @param x,y le point a tester
   * @param z zoom
   */
   protected boolean in(ViewSimple v, double x, double y) { return inside(v,x,y); }

   /** Retourne vrai si l'objet se trouve dans le rectangle indiqué */
   protected boolean inRectangle(ViewSimple v,RectangleD r) {
      return r.contains(xv[v.n],yv[v.n]);
   }

  /** Generation d'un clip englobant.
   * Retourne un rectangle qui englobe l'objet
   * @param zoomview reference au zoom courant
   * @return         le rectangle enblobant
   */
//   protected Rectangle getClip(ViewSimple v) {
//      if( !visible ) return null;
//      Point p = getViewCoord(v,0,0);
//      if( p==null ) return null;
//      if( select ) return new Rectangle(p.x-DS,p.y-DS,1+DDS,1+DDS);
//      else return new Rectangle(p.x,p.y,1,1);
//   }

   /** Extension d'un clip pour ajouter la zone de l'objet
    * A l'avantage de ne pas faire d'allocation si inutile
    */
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      Point p = getViewCoord(v,0,0);
      if( p==null ) return clip;
      if( isSelected() ) return unionRect(clip, p.x-DS,p.y-DS,1+DDS,1+DDS);
      return unionRect(clip, p.x,p.y,1,1);
   }

   protected void info(Aladin aladin) {}

  /** Affichage l'info lie a l'objet
   * Affiche l'identifacteur dans le statut de l'objet aladin
   * @param aladin reference
   */
   protected void status(Aladin aladin) { aladin.status.setText(id); }

   /** Ecriture d'info ASCII permettant de construire des links html
    * pour une carte cliquable */
   protected void writeLink(OutputStream o,ViewSimple v)  throws Exception {}

  /** Dessine l'objet
   * @param g        le contexte graphique
   * @param zoomview reference au zoom courant
   */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      Point p = getViewCoord(v,0,0);
      if( p==null ) return false;

      g.setColor( plan.c );
      p.x+=dx; p.y+=dy;
      g.drawLine(p.x,p.y,1+dx,1+dy);
      if( isSelected() ) {
         g.setColor( Color.green );
         drawSelect(g,v);
      }
      return true;
   }

   // Dessine les poignees de selection de l'objet
   protected void drawSelect(Graphics g,ViewSimple v) {
      drawSelect(g,getClip(v));
   }   
   
   protected void drawSelect(Graphics g,Rectangle r) {
      int xc=0;
      int yc=0;

      // Trace des poignees de selection
      for( int i=0; i<4; i++ ) {
         switch(i ) {
            case 0: xc=r.x; yc=r.y+r.height-DS-1; break;            // coin IG
            case 1: xc=r.x; yc=r.y; break;                        // coin SG
            case 2: xc=r.x+r.width-DS-1; yc=r.y; break;               // coin SD
            case 3: xc=r.x+r.width-DS-1; yc=r.y+r.height-DS-1; break; // coin ID
         }
         drawPoignee(g,xc,yc);
      }
   }
   
   protected void drawPoignee(Graphics g, int xc,int yc) {
      g.setColor( Color.green );
      g.fillRect( xc+1,yc+1 , DS,DS );
      g.setColor( Color.black );
      g.drawRect( xc,yc , DS,DS );
   }

   protected boolean cutOn() { return false; }
   
   protected void histOn() {
      plan.aladin.calque.zoom.zoomView.activeHistPixel(id);
   }


//   protected void histOn() {
//
//      // recherche du maximum
//      int max=0,i;
//      for( i=0; i<statHist.length; i++ ) if( max<statHist[i] ) max = statHist[i];
//
//      // Mise a l'echelle des histogrammes
//      int Hp = plan.aladin.calque.zoom.zoomView.SIZE;
//      max+=max/5;
//      if( max!=0 ) for( i=0; i<statHist.length; i++ ) {
//         statHist[i] =  (statHist[i]*Hp)/max;
//      }
//
//      plan.aladin.calque.zoom.zoomView.setCut(this,statHist,ZoomView.CUTHIST);
//   }

   /** Initialisation des variables des statistiques */
   protected void statInit() {
      nombre=0;
      carre=total=0;
      medianeArrayNb=0;
      posx=posy=-1;
      minimum=Double.MAX_VALUE;
      maximum=-Double.MAX_VALUE;
   }

   /** Utilisé pour le calcul des statistiques sur un polygone */
   protected double statPixel(Graphics g,int x,int y,ViewSimple v,HistItem onMouse) {

      // Mise à jour des stats
      double pix;
      PlanImage pi = (PlanImage)v.pref;
      Color col = g.getColor();
      
      if( !(plan instanceof PlanBG) && (y<0 || y>=pi.naxis2 || x<0 || x>=pi.naxis1
            || (pi.fmt==PlanImage.JPEG || pi.pixelsOrigin==null && !pi.isBigImage())
            )) {
         pix=Double.NaN;
      } else {
         pix= pi.getPixelInDouble(x,y);
      }
      
      double zoom = v.getZoom();
      
      // Coloriage du pixel si concerné
      if( g!=null && onMouse!=null && onMouse.contains(pix)) {
         Point p = v.getViewCoord(x+0.5,y+0.5);
         if( p!=null ) {
            g.setColor(Color.cyan);
            int z1=(int)zoom;
            if( z1<1 ) z1=2;
            g.fillRect(p.x-z1/2,p.y-z1/2,z1,z1);
         }
      }
      
      if( zoom>2 ) {
         Point p = v.getViewCoord(x+0.5,y+0.5);
         if( !Double.isNaN(pix) ) {
            g.setColor( col );
            if( zoom>4 ) Util.fillCircle5(g, p.x, p.y);
            else Util.fillCircle2(g, p.x, p.y);
         }
//         if( Double.isNaN(pix) ) g.setColor(Color.orange);
//         else g.setColor(Color.red);
//         if( zoom>4 ) Util.fillCircle5(g, p.x, p.y);
//         else Util.fillCircle2(g, p.x, p.y);
      }
      g.setColor(col);
      
      return statPixel(pix);
   }
   
   
   /** Utilisé pour le calcul des statistiques sur un polygone */
   protected double statPixel(Graphics g,double pix, double ra, double dec,ViewSimple v,HistItem onMouse) {

     Coord coo = new Coord(ra,dec);
     v.getProj().getXY(coo);
     Color col = g.getColor();
     
     // Détermination de la taille d'un pixel Healpix sur la vue.
     double pixelSize = v.rv.height/(v.getTailleDE()/((PlanBG)v.pref).getPixelResolution())/Math.sqrt(2);
      
      // Coloriage du pixel si concerné
      if( g!=null && onMouse!=null && onMouse.contains(pix)) {
         Point p = v.getViewCoord(coo.x,coo.y);
         if( p!=null ) {
            g.setColor(Color.cyan);
            int z1=(int)pixelSize;
            if( z1<1 ) z1=2;
            Polygon pol = new Polygon( new int[]{ p.x,    p.x+z1, p.x,    p.x-z1}, 
                                       new int[]{ p.y-z1, p.y,    p.y+z1, p.y}, 4);
            g.fillPolygon(pol);
         }
      }
      
      if( pixelSize>4 ) {
         Point p = v.getViewCoord(coo.x,coo.y);
         if( !Double.isNaN(pix) ) {
            g.setColor( col );
            if( pixelSize>8 ) Util.fillCircle5(g, p.x, p.y);
            else Util.fillCircle2(g, p.x, p.y);
         }
//         if( Double.isNaN(pix) ) g.setColor(Color.orange);
//         else g.setColor(Color.red);
//         if( pixelSize>8 ) Util.fillCircle5(g, p.x, p.y);
//         else Util.fillCircle2(g, p.x, p.y);
      }
      g.setColor(col);
      
      return statPixel(pix);
   }
   

   protected double statPixel(double pix) {
      if( Double.isNaN(pix) ) return pix;
      nombre++;
      if( pix<minimum ) minimum=pix;
      if( pix>maximum ) maximum=pix;
      if( medianeArrayNb<MAXMEDIANE ) medianeArray[medianeArrayNb++] = pix;
      total+=pix;
      carre+=pix*pix;
      return pix;
   }


   protected boolean statCompute(Graphics g, ViewSimple v) { return false; };

   static final int STATDY = 13;            // Hauteur d'une ligne de texte pour les stats
   static final int HAUTSTAT = STATDY*9;    // Hauteur de la boite des stats
   static final int LARGSTAT = 120;         // Largeur de la boite des stats

   /** Retourne la position en unité View des stats */
   protected Rectangle getStatPosition(ViewSimple v) {
      Point hg = v.getViewCoord(minx,miny);
      Point bd = v.getViewCoord(maxx,maxy);
      if( hg==null || bd==null ) return null;
      return new Rectangle(bd.x+5,hg.y,LARGSTAT,HAUTSTAT);
   }

    /** Affichage des statistiques d'un polygone */
    protected void statDraw(Graphics g,ViewSimple v,int dx, int dy) {
       
       // Juste pour afficher le débugging des losanges HEALPix couvrant
//       if( v.pref instanceof PlanBG && ((PlanBG)v.pref).DEBUGMODE )  { statCompute(g,v); return; } 

       if( !v.flagPhotometry || !v.pref.hasAvailablePixels() || v.pref instanceof PlanImageRGB ) return;

       if( !statCompute(g,v) ) return;
       
       String cnt=Util.myRound(nombre);
       String sum=Util.myRound(total);
       String avg=Util.myRound(moyenne);
       String med=Double.isNaN(mediane) ? "" : Util.myRound(mediane);
       String sig=Util.myRound(sigma);
       String surf=Coord.getUnit(surface,false,true)+"²";
       String min=Util.myRound(minimum);
       String max=Util.myRound(maximum);

       if( isWithStat() || isWithLabel() ) {
          Color c1=g.getColor();
          Color c2=c1==Color.red || c1==Color.blue?Color.white:null;
          Rectangle r = getStatPosition(v);
          if( r!=null && (isWithLabel() || v.aladin.view.isMultiView()) ) {
             r.x+=dx;
             r.y+=dy;
             g.drawLine(r.x,r.y,r.x,r.y+HAUTSTAT);
             if( posx==-1 ) {
                posx = minx+3*(maxx-minx)/4.;
                posy = (maxy+miny)/2;
             }
             Point c = v.getViewCoord(posx,posy);
             if( c!=null ) {
                g.drawLine(r.x,r.y+HAUTSTAT/2,c.x,c.y);
                Util.fillCircle5(g,c.x,c.y);

                r.x+=2; r.y+=STATDY-2;

                g.setFont(Aladin.BOLD);
                Util.drawStringOutline(g,"Cnt",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,cnt,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                Util.drawStringOutline(g,"Sum",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,sum,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                Util.drawStringOutline(g,"Sigma",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,sig,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                Util.drawStringOutline(g,"Min",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,min,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                Util.drawStringOutline(g,"Avg",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,avg,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                if( !Double.isNaN(mediane) ) {
                   Util.drawStringOutline(g,"Med",r.x,r.y,c1,c2);
                   Util.drawStringOutline(g,med,r.x+43,r.y,c1,c2); 
                   r.y+=STATDY;
                }
                Util.drawStringOutline(g,"max",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,max,r.x+43,r.y,c1,c2);  r.y+=STATDY;
                if( this instanceof SourceStat ) {
                   Util.drawStringOutline(g,"Rad",r.x,r.y,c1,c2);
                   Util.drawStringOutline(g,Coord.getUnit( ((SourceStat)this).getRadius()),r.x+43,r.y,c1,c2); 
                   r.y+=STATDY;
                }
                Util.drawStringOutline(g,"Surf",r.x,r.y,c1,c2);
                Util.drawStringOutline(g,surf,r.x+43,r.y,c1,c2);  r.y+=STATDY;
             }
          }
       }

       if( v.pref==plan.aladin.calque.getPlanBase() ) {
          if( !(this instanceof SourceStat) ) {
             id="Cnt "+cnt+" / Sum "+sum
                   +" / Sigma "+sig
                   +" / Min "+min
                   +" / Avg "+avg
                   +(Double.isNaN(mediane)?"":" / Med "+med)
                   +" / Max "+max
                   + (this instanceof SourceStat ? " / Rad "+Coord.getUnit( ((SourceStat)this).getRadius()):"")
                   +" / Surf "+surf
                   ;
          }
          histOn();
       }
//       status(plan.aladin);
    }
}
