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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.JTextField;

import cds.aladin.Hist.HistItem;
import cds.aladin.Ligne.Segment;
import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.astro.AstroMath;
import cds.astro.Coo;
import cds.astro.Proj3;
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
   static final int CARTOUCHE  =6;  // Graphisme pour un cartouche
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
   void setD() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw = m.stringWidth(id)+4;
      dh=HF;
   }
   
   /** Set specifical color (dedicated for catalog sources) */
   public void setColor(Color c) { couleur=c; }
   
   /** Positionne un rayon (avec possibilité d'une unité) */
   protected void setRadius(String r) {
      radius = Server.getAngle(r,Server.RADIUS)/60.;
//      setWithLabel(true);
   }

   /** Change le rayon d'un repère CERCLE (r en pixels dans le plan de ref de v */
   void setRayon(ViewSimple v,double r) {
      Coord c = new Coord();
      Projection proj = v.getProj().copy();
      proj.setProjCenter(0,0);
      c.al=c.del=0;
      proj.getXY(c);
      c.y+=r;
      proj.getCoord(c);
      radius=Math.abs(c.del);
   }

   /** Positionnement d'un ID particulier */
   protected void setId(String s) { id=s; setD(); }

  /** Positionne l'id par defaut */
   void setId() {
      id=plan.aladin.localisation.J2000ToString(raj,dej);
      setD();
   }

  /** Test d'appartenance.
   * Retourne vrai si le point (x,y) de l'image se trouve sur le texte
   * @param x,y le point a tester
   * @param z valeur courante du zoom
   * @return <I>true</I> c'est bon, <I>false</I> sinon
   */
   protected boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      
      // Cas d'un repère avec surface
      double r = getRayon(v);
      if( r>0 ) {
         return (x-xv[v.n])*(x-xv[v.n]) + (y-yv[v.n])*(y-yv[v.n]) <= (r+1)*(r+1);

      // Cas courant
      } else {
         double l=L/v.getZoom();
         double xc,yc;
         xc = xv[v.n];
         yc = yv[v.n];
         
         return(xc<=x+l+dw/2 && xc>=x-l-dw/2 && yc<=y+l+dh/2 && yc>=y-l-dh/2);
      }
   }
   
   protected boolean inLabel(ViewSimple v,double x, double y) {
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
      int L = ViewSimple.top(Math.max(this.L,getRayon(v)*v.getZoom()));
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
      int L = ViewSimple.top(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,L,L);
      if( p==null ) return null;
      if( isSelected() )  clip = unionRect(clip, p.x-L-DS,p.y-L-DS,L*2+DDS,L*2+DDS);
      return clip;

   }

   /** Détermination de la couleur de l'objet */
   protected Color getColor() {
	  if( type==TARGET || type==TARGETL ) couleur=Color.magenta.darker();
   	  if( couleur!=null ) return couleur;

      if( type==CARTOUCHE ) couleur = Color.blue;
      else if( type==ARROW ) couleur = Color.red;
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
      if( v!=null && !v.isFree() && v.pref.type==Plan.ALLSKYIMG ) {
         ((PlanBG)v.pref).setDebugIn(raj,dej,getRadius());
      }
      
      boolean flagHist = v==v.aladin.view.getCurrentView();
      
      if( v==null || v.isFree() || !v.pref.hasAvailablePixels() ) return false;
      statInit();
      
      double xc,yc;
      xc=xv[v.n]-0.5;
      yc=yv[v.n]-0.5;
      double r=getRayon(v);
      
      // Si cercle large ou s'il s'agit d'un allsky, on ne calcule pas pendant le changement de taille ou les déplacements
      if( (r>100 /* || v.pref instanceof PlanBG */ ) && v.flagClicAndDrag) return false;
      
      // TODO : j'ai des doutes sur ces valeurs si v.pref.type==Plan.IMAGEBKGD
      minx=ViewSimple.floor(xc-r);
      maxx=ViewSimple.top(xc+r);
      miny=ViewSimple.floor(yc-r);
      maxy=ViewSimple.top(yc+r);
      
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
      if( v.pref.type==Plan.ALLSKYIMG ) {
         try {
            PlanBG pbg = (PlanBG)v.pref;
            pixelSurf = Math.pow(pbg.getPixelResolution(),2);
            int orderFile = pbg.getOrder();
            if( pbg.maxOrder!=pbg.getOrder() ) return false;
            long nsideFile = CDSHealpix.pow2(orderFile);
            long nsideLosange = CDSHealpix.pow2(pbg.getLosangeOrder());
            long nside = nsideFile * nsideLosange;
            Coord coo = new Coord(raj,dej);
            coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
            double radiusRadian = Math.toRadians(getRadius());
            long [] npix = CDSHealpix.query_disc(nside, coo.al, coo.del, radiusRadian, false);
            for( int i=0; i<npix.length; i++ ) {
               long npixFile = npix[i]/(nsideLosange*nsideLosange);
               double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.ONLYIFDISKAVAIL);
               if( Double.isNaN(pix) ) continue;
               pix = pix*pbg.bScale+pbg.bZero;
               double polar[] = CDSHealpix.pix2ang_nest(nside, npix[i]);
               polar = CDSHealpix.polarToRadec(polar);
               coo.al = polar[0]; coo.del = polar[1];
               coo = Localisation.frameToFrame(coo,pbg.frameOrigin,Localisation.ICRS);
               statPixel(g,pix,coo.al,coo.del,v,onMouse);
               if( flagHist ) v.aladin.view.zoomview.addPixelHist(pix);
            }
         } catch( Exception e ) { e.printStackTrace(); }

      } else {
         try { pixelSurf = v.pref.projd.getPixResAlpha()* v.pref.projd.getPixResDelta();
         } catch( Exception e ) { }
         for( int y=miny; y<=maxy; y++ ) {
            for( int x=minx; x<=maxx; x++ ) {
               if( (x-xc)*(x-xc) + (y-yc)*(y-yc) > carreRayon ) continue;
               double pix = statPixel(g, x, y, v,onMouse);
               if( Double.isNaN(pix) ) continue;
               if( flagHist ) v.aladin.view.zoomview.addPixelHist(pix);
            }
         }
      }
      
      if( flagHist ) v.aladin.view.zoomview.createPixelHist(v.pref.type==Plan.ALLSKYIMG ? "HEALPixels":"Pixels");

      if( v.pref.type==Plan.ALLSKYIMG ) {
         xc=xv[v.n]-0.5;
         yc=yv[v.n]-0.5;
         minx=ViewSimple.floor(xc-r);
         maxx=ViewSimple.top(xc+r);
         miny=ViewSimple.floor(yc-r);
         maxy=ViewSimple.top(yc+r);
      }

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
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
      if( radius<=0 ) throw new Exception("getStats error: no radius");
      
      double nombre=0;
      double carre=0;
      double total=0;
      double pixelSurf;
      
      // Cas d'une map HEALPix
      if( p.type==Plan.ALLSKYIMG ) {
         PlanBG pbg = (PlanBG)p;
         pixelSurf = Math.pow(pbg.getPixelResolution(),2);
         int orderFile = pbg.getOrder();
//         if( pbg.maxOrder!=pbg.getOrder() ) return false;
         long nsideFile = CDSHealpix.pow2(orderFile);
         long nsideLosange = CDSHealpix.pow2(pbg.getLosangeOrder());
         long nside = nsideFile * nsideLosange;
         Coord coo = new Coord(raj,dej);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
         double radiusRadian = Math.toRadians(getRadius());
         long [] npix = CDSHealpix.query_disc(nside, coo.al, coo.del, radiusRadian, false);
         for( int i=0; i<npix.length; i++ ) {
            long npixFile = npix[i]/(nsideLosange*nsideLosange);
            double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],HealpixKey.ONLYIFDISKAVAIL);
            if( Double.isNaN(pix) ) continue;
            pix = pix*pbg.bScale+pbg.bZero;
            nombre++;
            total+=pix;
            carre+=pix*pix;
         }
         
      // Cas d'une image "classique"
      } else {
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

         int minx=ViewSimple.floor(xc-r);
         int maxx=ViewSimple.top(xc+r);
         int miny=ViewSimple.floor(yc-r);
         int maxy=ViewSimple.top(yc+r);

         for( int y=miny; y<=maxy; y++ ) {
            for( int x=minx; x<=maxx; x++ ) {
               if( (x-xc)*(x-xc) + (y-yc)*(y-yc) > carreRayon ) continue;
               double pix= ((PlanImage)p).getPixelInDouble(x,y);
               if( Double.isNaN(pix) ) continue;
               nombre++;
               total+=pix;
               carre+=pix*pix;
            }
         }
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
         c.del = dej + radius*Math.sin(theta);
         c.al  = raj + radius*Math.cos(theta);
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

   
   static final Color JAUNEPALE = new Color(255,255,225);

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
               if( isSelected() && plan.aladin.view.nbSelectedObjet()<=2 ) cutOn();
               else cutOff();
            } else {
//               if( v.pref instanceof PlanBG ) {
//                  int l = (int)(getRayon(v)*v.getZoom());
//                  g.drawOval(p.x-l, p.y-l, l*2, l*2);
//                  
////                  String s = plan.aladin.localisation.J2000ToString(raj, dej);
////                  g.drawString(s, p.x - g.getFontMetrics().stringWidth(s)/2, p.y-2);
////                  s = Coord.getUnit(getRadius());
////                  g.drawString(s, p.x - g.getFontMetrics().stringWidth(s)/2, p.y+15);
//                  
////                  demiLargeur = (int)Math.min(Math.max(2,v.getZoom()*3),16);
////                  demiCentre = 2*demiLargeur/3;
////                  drawReticule(g,p.x,p.y,demiLargeur,demiCentre,getColor());
////                  drawSpecialCircle(g,v);
////                  id = PlanBG.CURRENTMODE;
//               } else {
                  int l = (int)(getRayon(v)*v.getZoom());
                  Util.drawFillOval(g, p.x-l, p.y-l, l*2, l*2, 0.1f * plan.getOpacityLevel(), null);
//               }
               if( isSelected() ) statDraw(g, v,dx,dy);
            }
            break;
         case ARROW:
//            g.setColor(Color.red);
            g.drawLine(p.x,   p.y-L, p.x,   p.y-3);
            g.drawLine(p.x,   p.y-3,   p.x-3,   p.y-6);
            g.drawLine(p.x,   p.y-3,   p.x+3,   p.y-6);
            break;
         case CARTOUCHE:
            g.setColor(JAUNEPALE);
            g.drawLine(p.x-L+1,   p.y-L, p.x+1,   p.y);
            g.setColor(Color.black);
            g.drawLine(p.x-L,   p.y-L, p.x,   p.y);
            g.setColor(JAUNEPALE);
            Util.fillCircle5(g, p.x, p.y);
            g.setColor(Color.black);
            Util.drawCircle7(g, p.x, p.y);
            break;
//         case TARGET:
//            g.drawLine(p.x-L, p.y,   p.x-3, p.y);
//            g.drawLine(p.x+3, p.y,   p.x+L, p.y);
//            g.drawLine(p.x,   p.y-L, p.x,   p.y-3);
//            g.drawLine(p.x,   p.y+3, p.x,   p.y+L);
//            break;
      }

      if( isWithLabel() && !hasRayon() ) {
         if( id==null ) setId();
         if( type==CARTOUCHE ) {
            Util.drawCartouche(g,p.x-dw/2,p.y-L-dh-1, dw-2, dh+3, 1f, Color.black,JAUNEPALE);
            g.setColor( getColor() );
            g.setFont(Aladin.SPLAIN);
            g.drawString(id,p.x-dw/2,p.y-L-1);
//            g.drawLine(p.x-dw/2,p.y-L,p.x+dw/2,p.y-L);
            
         } else g.drawString(id,p.x-dw/2,p.y-L-1);
      }

      if( isSelected()  ) {
//         if( plan!=null && plan.type==Plan.APERTURE ) return;
         g.setColor( Color.green );
         drawSelect(g,v);
      }
      return true;
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

      // Trace des poignees de selection
      for( int i=0; i<4; i++ ) {
         switch(i ) {
            case 0: xc=r.x+r.width/2-DS; yc=r.y; break;                // Bas
            case 1: xc=r.x+r.width/2-DS; yc=r.y+r.height-DS; break;       // Haut
            case 2: xc=r.x+r.width-DS; yc=r.y+r.height/2-DS;  break;      // Droite
            case 3: xc=r.x; yc=r.y+r.height/2-DS;  break;              // Gauche
         }
         g.setColor( Color.green );
         g.fillRect( xc+1,yc+1 , DS,DS );
         g.setColor( Color.black );
         g.drawRect( xc,yc , DS,DS );
      }
   }

   protected void drawRotCenterSelect(Graphics g,ViewSimple v) {
      Point p = getViewCoord(v, L, L);
      g.setColor( Color.green );
      Util.fillCircle5(g, p.x, p.y);
      g.setColor( Color.black );
      Util.drawCircle5(g, p.x, p.y);
   }

   protected void remove() { cutOff(); }

   /** Suppression de la coupe memorise dans le zoomView
    * => arret de son affichage
    */
   protected void cutOff() { 
      plan.aladin.calque.zoom.zoomView.setHist(); 
      plan.aladin.calque.zoom.zoomView.cutOff(this);
   }


   /** Passage d'une coupe du segment au zoomView
    * => affichage d'un histogramme dans le zoomView en surimpression
    * de la vignette courante.
    * @return true si le CutGraph a pu être fait
    */
   protected boolean cutOn() {
      ViewSimple v=plan.aladin.view.getCurrentView();
      if( v==null || plan.aladin.toolBox.getTool()==ToolBox.PAN ) return false;
      Plan p=v.pref;
      if( p==null || !(p instanceof PlanImageBlink) ) return false;
      PlanImageBlink pc = (PlanImageBlink)p;

      int x=(int)xv[v.n];
      int y=(int)yv[v.n];
      int n=pc.getNbFrame();
      int res[] = new int[n];
      try {
         for( int i=0; i<n; i++ ) res[i] = (pc.getPixel8bit(i,x,y)) & 0xFF;
      } catch( Exception e ) {}

      plan.aladin.calque.zoom.zoomView.setCut(this,res,ZoomView.CUTNORMAL);

      return true;
   }

}
