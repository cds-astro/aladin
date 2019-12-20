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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JTextField;

import cds.aladin.ZoomHist.HistItem;
import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.tools.FastMath;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;


/**
 * Objet graphique representant un repere
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : (1 dec 00) Gestion des differents graphismes
 * @version 1.1 : (27 avril 00) Suppression des ':' pour le notePad
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Repere extends Position {
   static final int DEFAULT=0;	// Graphisme par defaut (croix simple)
   static final int TARGET =1;  // Graphisme pour le target courant (croix evidee)
   static final int TARGETL=2;  // Réticule en barre horizontale et verticale
   static final int ARROW  =3;  // Graphisme pour l'objet demandé (flèche)
   static final int CENTER =4;  // Graphisme pour le centre d'un FoV (croix)
   static final int ROTCENTER =5;  // Graphisme pour le centre de rotationd'un FoV (rond)
   protected int L = 5;         // demi-taille du repere
   private double radius;      // Rayon en degrés image du cercle englobant
   protected int dw,dh;         // mesure du label
   protected int type=DEFAULT;	// Type de graphisme

   protected Color couleur=null; // Couleur alternative

   /** Creation d'un repere graphique sans connaitre RA/DE.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position
    */
   protected Repere(Plan plan, ViewSimple v, double x, double y) {
      super(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
   }

   /** Creation d'un repere pour les bakcups */
   protected Repere(Plan plan) { super(plan); }

   /** Creation d'un repere speciale de positionnement dans l'ecran */
   protected Repere(Plan plan,Coord c) {
      super(plan,null,0,0,c.al,c.del,RADE,null);
   }

   /** Creation d'un repere speciale de positionnement dans l'ecran */
   protected Repere(Plan plan, ViewSimple v, Coord c) {
      super(plan,v,0,0,c.al,c.del,RADE,null);
   }

   /** Creation d'un repere graphique en connaissant RA/DE.
    * Cela signifie que ce repere est issu d'une capture de sources
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position
    * @param raj,dej  coordonnees
    */
   protected Repere(Plan plan, ViewSimple v,double x, double y, double raj, double dej) {
      super(plan,v,x,y,raj,dej,XY|RADE,null);
      setId();
      setWithLabel(false);
   }

   public Vector getProp() {
      Vector propList = super.getProp();

      if( hasRayon() ) {
         final Obj myself = this;
         final JTextField testRadius = new JTextField( 10 );
         final PropAction updateRadius = new PropAction() {
            public int action() { testRadius.setText( Coord.getUnit(getRadius()) ); return PropAction.SUCCESS; }
         };
         PropAction changRadius = new PropAction() {
            public int action() {
               testRadius.setForeground(Color.black);
               String oval = Coord.getUnit(getRadius());
               try {
                  String nval = testRadius.getText();
                  if( nval.equals(oval) ) return PropAction.NOTHING;
                  ((Repere)myself).setRadius(nval);
                  return PropAction.SUCCESS;
               } catch( Exception e1 ) {
                  updateRadius.action();
                  testRadius.setForeground(Color.red);
               }
               return PropAction.FAILED;
            }
         };
         propList.add( Prop.propFactory("radius","Radius","",testRadius,updateRadius,changRadius) );
      }

      final Couleur col = new Couleur(couleur,true);
      final PropAction changeCouleur = new PropAction() {
         public int action() {
            Color c= col.getCouleur();
            if( c==couleur ) return PropAction.NOTHING;
            couleur=c;
            return PropAction.SUCCESS;
         }
      };
      col.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeCouleur.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("color","Color","Alternative color",col,null,changeCouleur) );

      return propList;
   }

   /** Retourne le type d'objet */
   static private final String C= "|";

   /** Retourne une chaine contenant toutes les informations techniques à sauvegarder dans un fichier AJ
    * afin de pouvoir regénérer le repère, même s'il a un rayon */
   protected String getSpecificAJInfo() {
      return id+C +( hasRayon() ? getRadius():"");
   }

   /** Traite une chaine contenant toutes les informations techniques issues d'un fichier AJ */
   protected void setSpecificAJInfo(String s) {
      StringTokenizer tok = new StringTokenizer(s,C);
      String s1 = tok.nextToken(); id = s1.length()==0 ? null : s1;
      if( tok.hasMoreTokens() ) {
         try {
            radius = Double.parseDouble(tok.nextToken());
         } catch( Exception e ) { if( Aladin.levelTrace==3 ) e.printStackTrace(); }
      }
   }

   /** Retourne le type d'objet */
   public String getObjType() { return "Phot"; }

   /** Modifie le type de graphisme associe au repere */
   protected void setType(int type) { this.type=type; }

   Position rotcenter=null;

   /** Indique que le repère est un centre de rotation, et donne la position
    * du centre de projection associé afin de pouvoir tracer un segment entre les deux */
   protected void setRotCenterType(Position o) { this.type=ROTCENTER; rotcenter=o;  }

   /** Retourne le type du graphisme associé au repere */
   protected int getType() { return type; }

   /** Retourne true si le repere est un réticule large (Deux lignes horizontales et verticales) */
   protected boolean isLargeReticle() { return type==TARGETL; }

   /** Modifie la taille du repere */
   protected void setSize(int L) { this.L=L; }

   /** Positionne les coordonnees de l'objet et son id */
   protected void setCoord(ViewSimple v) { super.setCoord(v); setId(); }

   // En cas de déplacement il faut recalculer l'id le cas échéant
   protected void setPosition(ViewSimple v,double x, double y) { super.setPosition(v,x,y); setId(); }
   protected void deltaPosition(ViewSimple v,double x, double y) { super.deltaPosition(v,x,y); setId(); }
   protected void deltaRaDec(double dra, double dde) { super.deltaRaDec(dra,dde); setId(); }

   /** Force la projection qq soit l'état du buffer */
   protected void reprojection(ViewSimple v) {
      super.projection(v);
   }

   /** Determine le decalage pour ecrire l'id */
   protected void setD() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw = m.stringWidth(id)+25;
      dh=  HF;
   }

   /** Set specifical color (dedicated for catalog sources) */
   public void setColor(Color c) { couleur=c; }

   /** Positionne un rayon (avec possibilité d'une unité) */
   protected void setRadius(String r) {
      radius = Server.getAngleInArcmin(r,Server.RADIUS)/60.;
      //      setWithLabel(true);
   }

   /** Change le rayon d'un repère CERCLE (r en pixels dans le plan de ref de v */
   void setRayon(ViewSimple v,double r) {
      Coord c = new Coord();
      Projection proj = v.getProj().copy();
      proj.setProjCenter(0,0);
      double d=0;
      c.al=c.del=0;

      proj.getXY(c);

      // Y a blême pour les Calibs qui ne sont pas en equatorial.
      // Dans ce cas, je prend comme référence le point lui-même
      // et je ne change pas le centre de projection
      if( Double.isNaN(c.del) ) {
         proj = v.getProj().copy();
         c.al=raj;
         c.del=dej;
         d=dej;
         proj.getXY(c);
      }
      c.y+=r;
      proj.getCoord(c);
      radius=Math.abs(d-c.del);
   }

   /** Positionnement d'un ID particulier */
   protected void setId(String s) { id=s; setD(); }

   /** Positionne l'id par defaut */
   void setId() {
      id=plan.aladin.localisation.J2000ToString(raj,dej);
      setD();
   }

   protected  boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;

      // Cas d'un repère avec surface
      double r = getRayon(v);
      if( r>0 ) {
         return (x-xv[v.n])*(x-xv[v.n]) + (y-yv[v.n])*(y-yv[v.n]) <= r*r; // +1;

         // Cas courant
      } else {
         double l=L/v.getZoom();
         double xc,yc;
         xc = xv[v.n];
         yc = yv[v.n];
         return(xc<=x+l && xc>=x-l && yc<=y+l && yc>=y-l);
      }
   }

   private boolean flagIn=false;
   protected boolean inLabel(ViewSimple v,double x, double y) {
      boolean rep = inLabel1(v,x,y);
      flagIn = rep;
      return rep;
   }
   private boolean inLabel1(ViewSimple v,double x, double y) {
      Point p = getViewCoord(v,L,L);
      if( p==null ) return false;
      return x>=p.x-dw/2 && x<=p.x+dw/2 && y>=p.y-L-dh-1 && y<=p.y-L+5;
   }

   /** Generation d'un clip englobant.
    * Retourne un rectangle qui englobe l'objet/2
    * @param zoomview reference au zoom courant
    * @return         le rectangle enblobant
    */
   //   protected Rectangle getClip(ViewSimple v) {
   //      if( !visible ) return null;
   //      Rectangle r;
   //      Point p = getViewCoord(v,L,L);
   //      if( p==null ) return null;
   //
   //      int D=L;
   //      if( type==CENTER ) D = (int)Math.min(Math.max(2,v.getZoom()*3),16);
   //      r = new Rectangle(p.x-D,p.y-D,D*2,D*2);
   //
   //      if( select )    r = r.union(new Rectangle(p.x-L-DS,p.y-L-DS,L*2+DDS,L*2+DDS));
   //      if( withlabel ) r = r.union(new Rectangle(p.x-dw/2,p.y-L-1-dh-1,dw,dh));
   //      return r;
   //   }
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      int L = (int)Math.ceil(Math.max(this.L,getRayon(v)*v.getZoom()));
      Point p = getViewCoord(v,L,L);
      if( p==null ) return clip;

      int D=L;
      if( type==CENTER || type==ROTCENTER) D = (int)Math.min(Math.max(2,v.getZoom()*3),16);
      clip = unionRect(clip, p.x-D,p.y-D,D*2,D*2);
      if( type==ROTCENTER ) {
         Point p1 = v.getViewCoord(rotcenter.xv[v.n],rotcenter.yv[v.n]);
         if( p1!=null ) clip = unionRect(clip,p1.x-D,p1.y-D,D*2,D*2);
      }
      if( isSelected() )  clip = unionRect(clip, p.x-L-DS,p.y-L-DS,L*2+DDS,L*2+DDS);
      if( isWithLabel() ) clip = unionRect(clip, p.x-dw/2,p.y-L-1-dh-1,dw,dh);

      if( hasRayon() && isSelected() ) clip = unionRect(clip,getStatPosition(v));
      return clip;
   }

   protected Rectangle getClipRayon(ViewSimple v ) {
      Rectangle clip=null;
      if( !isVisible() ) return null;
      int L = (int)Math.ceil(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,L,L);
      if( p==null ) return null;
      if( isSelected() )  clip = unionRect(clip, p.x-L-DS,p.y-L-DS,L*2+DDS,L*2+DDS);
      return clip;

   }

   /** Détermination de la couleur de l'objet */
   public Color getColor() {
      if( type==TARGET || type==TARGETL ) couleur=Color.magenta.darker();
      if( couleur!=null ) return couleur;

      if( type==ARROW ) couleur = Color.red;
      else if( plan!=null ) {
         if( plan.type==Plan.APERTURE ) couleur = ((PlanField)plan).getColor(this);
         else return plan.c;
      } else return Color.black;
      return couleur;
   }

   /**
    * Trace un réticule évidé
    * @param g le contexte du graphique
    * @param x,y la position centrale
    * @param demiLargeur la demi-taille du réticule
    * @param demiCentre la demi-taille de la portion évidée du réticule
    * @param c la couleur
    */
   private void drawReticule(Graphics g,int x,int y,int demiLargeur,int demiCentre,Color c) {
      g.setColor(c);
      g.drawLine(x-demiLargeur,y,   x-demiCentre,   y);
      g.drawLine(x+demiCentre, y,    x+demiLargeur, y);
      g.drawLine(x,   y-demiLargeur, x,   y-demiCentre);
      g.drawLine(x,   y+demiLargeur, x,   y+demiCentre);
   }

   protected boolean statCompute(Graphics g,ViewSimple v) {
      //      if( v!=null && !v.isFree() && v.pref.type==Plan.ALLSKYIMG ) {
      //         ((PlanBG)v.pref).setDebugIn(raj,dej,getRadius());
      //      }

      boolean flagHist = v==v.aladin.view.getCurrentView();

      if( v==null || v.isFree() || !hasPhot(v.pref) ) return false;
      statInit();

      double xc,yc;
      xc=xv[v.n]-0.5;
      yc=yv[v.n]-0.5;
      double r=getRayon(v);

      // Si cercle large ou s'il s'agit d'un allsky, on ne calcule pas pendant le changement de taille ou les déplacements
      if( r>100 &&  v.flagClicAndDrag) return false;

      // TODO : j'ai des doutes sur ces valeurs si v.pref.type==Plan.IMAGEBKGD
      minx=(int)Math.floor(xc-r);
      maxx=(int)Math.ceil(xc+r);
      miny=(int)Math.floor(yc-r);
      maxy=(int)Math.ceil(yc+r);

      double carreRayon = r*r;
      double pixelSurf = 0;

      HistItem onMouse=null;
      if( flagHist ) {
         onMouse =v.aladin.view.zoomview.hist==null ? null :  v.aladin.view.zoomview.hist.onMouse;

         // Si le est simplement dû au passage de la souris sur un précédent histogramme,
         // il ne faut pas regénérer cet histogramme
         if( onMouse==null ) v.aladin.view.zoomview.initPixelHist();
         else flagHist=false;
      }

      // Dans le cas d'un plan HEALPix, il faut passer par les routines query_disk()
      if( /* v.pref.type==Plan.ALLSKYIMG */ v.pref instanceof PlanBG ) {
         try {
            PlanBG pbg = (PlanBG)v.pref;
            int orderFile = pbg.getOrder();
//            long nsideFile = CDSHealpix.pow2(orderFile);
            long nsideLosange = CDSHealpix.pow2(pbg.getTileOrder());
//            long nside = nsideFile * nsideLosange;
            int orderPix = orderFile + pbg.getTileOrder();
            pixelSurf = CDSHealpix.pixRes(orderPix)/3600;
            pixelSurf *= pixelSurf;
            //            System.out.println("order="+CDSHealpix.log2(nside)+" => surf="
            //                  +Coord.getUnit(pixelSurf, false, true));
            Coord coo = new Coord(raj,dej);
            coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
            double radiusRadian = Math.toRadians(getRadius());
            long [] npix = CDSHealpix.query_discFXCenters(orderPix, coo.al, coo.del, radiusRadian);
            //            System.out.println("npix="+npix.length+" coo="+coo+" nside="+nside+" radius="+getRadius()+" nsideFile="+nsideFile+" nsideLosange="+nsideLosange);
            for( int i=0; i<npix.length; i++ ) {
               long npixFile = npix[i]/(nsideLosange*nsideLosange);
               double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.SYNC);
               //               double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.ONLYIFDISKAVAIL);
               if( Double.isNaN(pix) ) continue;
               pix = pix*pbg.bScale+pbg.bZero;
               double polar[] = CDSHealpix.pix2ang_nest(orderPix, npix[i]);
               polar = CDSHealpix.polarToRadec(polar);
               coo.al = polar[0]; coo.del = polar[1];
               coo = Localisation.frameToFrame(coo,pbg.frameOrigin,Localisation.ICRS);
               statPixel(g,pix,coo.al,coo.del,v,onMouse);
               //               System.out.println("pix["+i+"]="+pix);
               if( flagHist ) v.aladin.view.zoomview.addPixelHist(pix);
            }
            //            System.out.println("==> nombre="+npix.length+" total="+total+" => moyenne="+(total/nombre));
         } catch( Exception e ) { e.printStackTrace(); }

      } else {
         try {
            pixelSurf = v.pref.projd.getPixResAlpha()* v.pref.projd.getPixResDelta();
         } catch( Exception e ) { }
         for( double y=miny; y<=maxy; y++ ) {
            for( double x=minx; x<=maxx; x++ ) {
               if( (x-xc)*(x-xc) + (y-yc)*(y-yc) > carreRayon ) continue;
               double pix = statPixel(g, (int)x, (int)y, v,onMouse);
               if( Double.isNaN(pix) ) continue;
               if( flagHist ) v.aladin.view.zoomview.addPixelHist(pix);
            }
         }
      }

      if( flagHist ) v.aladin.view.zoomview.createPixelHist(v.pref.type==Plan.ALLSKYIMG ? "HEALPixels":"Pixels");

      //      if( v.pref.type==Plan.ALLSKYIMG ) {
      //         xc=xv[v.n]-0.5;
      //         yc=yv[v.n]-0.5;
      //         minx=(int)Math.floor(xc-r);
      //         maxx=(int)Math.ceil(xc+r);
      //         miny=(int)Math.floor(yc-r);
      //         maxy=(int)Math.ceil(yc+r);
      //      }

      // Valeurs en float pour la bounding box
      xc=xv[v.n];
      yc=yv[v.n];
      minx=xc-r;
      maxx=xc+r;
      miny=yc-r;
      maxy=yc+r;

      // Calculs des statistiques => sera utilisé immédiatement par le paint
      // Attention, il s'agit de variables statiques
      try {
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
      } catch( Exception e ) { }

      return true;
   }

   public boolean hasSurface() { return radius>0; }

   public double [] getStatistics(Plan p) throws Exception {

      Projection proj = p.projd;
      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
      if( radius<=0 ) throw new Exception("getStats error: no radius");

      double nombre=0;
      double carre=0;
      double total=0;
      double pixelSurf;

      // Cas d'une map HEALPix
      if( p.type==Plan.ALLSKYIMG ) {
         PlanBG pbg = (PlanBG)p;
         int orderFile = pbg.getOrder();
         //         if( pbg.maxOrder!=pbg.getOrder() ) return false;
//         long nsideFile = CDSHealpix.pow2(orderFile);
         long nsideLosange = CDSHealpix.pow2(pbg.getTileOrder());
//         long nside = nsideFile * nsideLosange;
         int orderPix = orderFile + pbg.getTileOrder();
         pixelSurf = CDSHealpix.pixRes(orderPix)/3600;
         pixelSurf *= pixelSurf;
         Coord coo = new Coord(raj,dej);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
         double radiusRadian = Math.toRadians(getRadius());
         long [] npix = CDSHealpix.query_disc(orderPix, coo.al, coo.del, radiusRadian, false);
         for( int i=0; i<npix.length; i++ ) {
            long npixFile = npix[i]/(nsideLosange*nsideLosange);
            //            double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.ONLYIFDISKAVAIL);
            double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.SYNC);
            if( Double.isNaN(pix) ) continue;
            pix = pix*pbg.bScale+pbg.bZero;
            nombre++;
            total+=pix;
            carre+=pix*pix;
         }

         // Cas d'une image "classique"
      } else {
         PlanImage pi = (PlanImage)p;
         pi.setLockCacheFree(true);
         pi.pixelsOriginFromCache();

         pixelSurf = proj.getPixResAlpha()*proj.getPixResDelta();
         Coord c = new Coord(raj,dej);
         proj.getXY(c);
         double  xc=c.x-0.5;
         double  yc=c.y-0.5;

         c.del=dej+radius;
         proj.getXY(c);
         double dy=(yc+0.5)-c.y;
         double dx=(xc+0.5)-c.x;
         double r = Math.sqrt(dx*dx + dy*dy);
         double carreRayon = r*r;

         int minx=(int)Math.floor(xc-r);
         int maxx=(int)Math.ceil(xc+r);
         int miny=(int)Math.floor(yc-r);
         int maxy=(int)Math.ceil(yc+r);

         for( int y=miny; y<=maxy; y++ ) {
            for( int x=minx; x<=maxx; x++ ) {
               if( (x-xc)*(x-xc) + (y-yc)*(y-yc) > carreRayon ) continue;
               double pix= pi.getPixelInDouble(x,y);
               if( Double.isNaN(pix) ) continue;
               nombre++;
               total+=pix;
               carre+=pix*pix;
            }
         }
         pi.setLockCacheFree(false);
      }

      double surface = nombre*pixelSurf;
      double moyenne = total/nombre;
      double variance = carre/nombre - moyenne*moyenne;
      double sigma = Math.sqrt(variance);

      return new double[]{ nombre, total, sigma, surface };
   }


   /** Retourne la rayon du repère en degrés */
   public double getRadius() { return radius; }

   /** Retourne true si le repère a un rayon associé */
   protected boolean hasRayon() { return radius>0; }

   /** Retourne true si l'objet contient des informations de photométrie  */
   public boolean hasPhot() { return hasRayon(); }
   public boolean hasPhot(Plan p) {
      if( !hasPhot() ) return false;
      return p.hasAvailablePixels();
   }

   public String getCommand() {
      String r;
      if( plan.aladin.localisation.getFrame()==Localisation.XY ) r=Util.myRound(getRayon(plan.aladin.view.getCurrentView()));
      else r=Coord.getUnit(getRadius());
      return "draw phot("+getLocalisation()+","+r+")";
   }

   /** Retourne le rayon en pixels d'un repère cerclé */
   protected double getRayon(ViewSimple v) {
      Coord c = new Coord();
      Projection proj = v.getProj();
      if( radius==0 || v.pref==null || !Projection.isOk(proj) ) return 0;
      c.al=raj;
      c.del=dej+radius;
      proj.getXY(c);
      double dy=yv[v.n]-c.y;
      double dx=xv[v.n]-c.x;
      return Math.sqrt(dx*dx + dy*dy);
   }

   protected void drawSpecialCircle(Graphics g,ViewSimple v) {
      Coord c = new Coord();
      Point p1 = new Point(0,0);
      Point p2 = new Point(0,0);
      Projection proj = v.getProj().copy();
      //      Coord center1 = proj.getXY(proj.getProjCenter());
      //      Point pCenter1=v.getViewCoord(center1.x, center1.y);
      //
      //      proj.setProjCenter(0, 0);
      //      Coord center2 = proj.getXY(proj.getProjCenter());
      //      Point pCenter2=v.getViewCoord(center2.x, center2.y);

      for( double theta=0; theta<6.3; theta+=0.1 ) {
         c.del = dej + radius*FastMath.sin(theta);
         c.al  = raj + radius*FastMath.cos(theta);
         proj.getXY(c);
         if( Double.isNaN(c.x) ) continue;
         p2=v.getViewCoord(p2, c.x, c.y);
         //         p2.x += (pCenter1.x-pCenter2.x);
         //         p2.y += (pCenter1.y-pCenter2.y);
         if( theta>0 ) g.drawLine(p1.x,p1.y,p2.x,p2.y);
         p1.x=p2.x; p1.y=p2.y;
      }
   }


   /** Retourn true si la position x,y se trouve sur une des 4 poignées de controle
    * du Repere circulaire (en haut, en bas, à droite et à gauche */
   protected boolean onPoignee(ViewSimple v,double x, double y) {
      double r = getRayon(v)+1;
      for( int i=0; i<4; i++ ) {
         double xc = xv[v.n];
         double yc = yv[v.n];
         if( i==0 ) yc+=r;
         else if( i==2 ) yc-=r;
         else if( i==1 ) xc+=r;
         else xc-=r;
         double dx = x-xc;
         double dy = y-yc;
         double l=L/v.getZoom();
         if( l<1 ) l=1;
         if( dx*dx + dy*dy<l*l ) return true;
      }
      return false;
   }


   static final Color JAUNEPALE  = new Color(255,255,225);
   static final Color CARTOUCHE_FOREGROUND = new Color(200,203,207);
   static final Color CARTOUCHE_BACKGROUND = new Color(50,50,50);

   /** Affiche le repere
    * @param g        le contexte graphique
    * @param zoomview reference au zoom courant
    */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      int demiLargeur,demiCentre;
      if( !isVisible() ) return false;
      Point p = getViewCoord(v,L,L);

      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor() );
      switch(type) {
         case TARGETL:
         case TARGET:
            demiLargeur = (int)Math.min(Math.max(8,v.getZoom()*12),32);
            demiCentre = Math.max(Math.min(demiLargeur-7,demiLargeur/3),2);
            if( demiLargeur>10 ) g.drawLine(p.x, p.y, p.x,p.y);   // un petit point central
            if( type==TARGETL ) demiLargeur=3000;
            drawReticule(g,p.x,p.y,demiLargeur,demiCentre,getColor());
            break;
         case CENTER:
            demiLargeur = (int)Math.min(Math.max(2,v.getZoom()*3),16);
            demiCentre = 2*demiLargeur/3;
            drawReticule(g,p.x,p.y,demiLargeur,demiCentre,getColor());
            break;
         case ROTCENTER:
            if( !Aladin.ROTATEFOVCENTER ) return false;
            Util.drawCircle7(g, p.x, p.y);
            Point p1 = v.getViewCoord(rotcenter.xv[v.n],rotcenter.yv[v.n]);
            if( p1==null ) return false;
            g.drawLine(p.x,p.y,p1.x,p1.y);
            break;
         case DEFAULT:
            if( !hasRayon() ) {
               g.drawLine(p.x-L, p.y,   p.x+L, p.y);
               g.drawLine(p.x,   p.y-L, p.x,   p.y+L);
//               if( isSelected() && plan.aladin.view.nbSelectedObjet()<=2 ) cutOn();
//               else cutOff();
            } else {
               int l = (int)(getRayon(v)*v.getZoom());
               if( hasPhot(v.pref) ) {
                  Util.drawFillOval(g, p.x-l, p.y-l, l*2, l*2, 0.1f * plan.getOpacityLevel(), null);
                  if( isSelected() ) statDraw(g, v,dx,dy);
               } else g.drawOval(p.x-l, p.y-l, l*2, l*2);
            }
            break;
         case ARROW:
            //            g.setColor(Color.red);
            g.drawLine(p.x,   p.y-L, p.x,   p.y-3);
            g.drawLine(p.x,   p.y-3,   p.x-3,   p.y-6);
            g.drawLine(p.x,   p.y-3,   p.x+3,   p.y-6);
            break;
      }

      if( isWithLabel() && !hasRayon() ) {
         if( id==null ) setId();
         g.drawString(id,p.x-dw/2,p.y-L-1);
      }

      if( isSelected()  ) {
         //         if( plan!=null && plan.type==Plan.APERTURE ) return;
         g.setColor( Color.green );
         drawSelect(g,v);
      }
      return true;
   }
   
   static private String getId( String id ) {
      int i=id.lastIndexOf('(');
      return id.substring(0,i).trim();
   }

   static private String getMag( String id ) {
      int i=id.lastIndexOf('(');
      int j=id.indexOf(',',i+1);
      return id.substring(i+1,j).trim();
   }

   static private String getType( String id ) {
      int i=id.lastIndexOf('(');
      int j=id.indexOf(',',i+1);
      return id.substring(j+1,id.length()-1).trim();
   }

   protected void drawSelect(Graphics g,ViewSimple v) {
      if( type==ROTCENTER ) drawRotCenterSelect(g,v);
      else if( !hasRayon() ) super.drawSelect(g,v);
      else drawSelect1(g,v);
   }

   protected void drawSelect1(Graphics g,ViewSimple v) {
      Rectangle r = getClipRayon(v);
      int xc=0;
      int yc=0;
      Color c = g.getColor();

      // Trace des poignees de selection
      for( int i=0; i<4; i++ ) {
         switch(i ) {
            case 0: xc=r.x+r.width/2-DS; yc=r.y; break;                // Bas
            case 1: xc=r.x+r.width/2-DS; yc=r.y+r.height-DS; break;       // Haut
            case 2: xc=r.x+r.width-DS; yc=r.y+r.height/2-DS;  break;      // Droite
            case 3: xc=r.x; yc=r.y+r.height/2-DS;  break;              // Gauche
         }
         g.setColor( c );
         g.fillRect( xc+1,yc+1 , DS,DS );
         g.setColor( Color.black );
         g.drawRect( xc,yc , DS,DS );
      }
      g.setColor( c );
   }

   protected void drawRotCenterSelect(Graphics g,ViewSimple v) {
      Point p = getViewCoord(v, L, L);
      g.setColor( Color.green );
      Util.fillCircle5(g, p.x, p.y);
      g.setColor( Color.black );
      Util.drawCircle5(g, p.x, p.y);
   }
}
