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
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import cds.aladin.ZoomHist.HistItem;
import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;


/**
 * Objet graphique representant une mesure photométrique manuelle
 *
 * @author Pierre Fernique [CDS]
 * @version 3.0 (juin 2016): Refonte complète depuis Repere.java
 */
public class SourceStat extends SourceTag {
   
   static protected Legende legende=createLegende();
   
   /** Création ou maj d'une légende associée à un SourcePhot */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "_RAJ2000","_DEJ2000","ID",  "s_region", "Image", "RA (ICRS)","DE (ICRS)","Count",  "Sum",   "Sigma",  "Min",   "Avg",   "Max",   "Radius","Area",       });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "double",  "double",  "char","char",     "char",  "char",     "char",     "integer","double","double", "double","double","double","double","double"  });
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "deg",     "deg",     "",    "",         "",      "\"h:m:s\"","\"h:m:s\"","pixel",  "",      "",       "",      "",      "",      "arcmin","arcmin^2"       });
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "10",      "10",      "10",  "5",        "20",    "13",      "13",        "10",     "10",    "10",    "10",    "10",     "10",    "10",    "10"    });
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "6",       "6",       "",    "",         "",      "4",        "5",        "2",      "4",     "4",     "4",     "4",      "4" ,    "4",     "4",         });
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "RA","DEC", "Identifier",  "Field of View", "Reference image", "Right ascension",  "Declination","Pixel count","Sum of pixel values","Median of the distribution", "Minimum value","Average value", "Maximum value",
                           "Radius","Area (pixels)" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "pos.eq.ra;meta.main","pos.eq.dec;meta.main","meta.id;meta.main","","","pos.eq.ra","pos.eq.dec","","","","","","","","" });
      legende.name="Pixel statistics";
      hideRADECLegende(legende);
      return legende;
   }
   
   protected int L = 5;          // taille des poignées de saisie
   private double radius;        // Rayon en degrés image du cercle englobant
   protected int dw,dh;          // mesure du label
   protected Color couleur=null; // Couleur alternative
   
   protected boolean asSource =false;   // Par défaut, n'apparait pas en tant que Source
   
   /** Creation pour les backups */
   protected SourceStat(Plan plan) { super(plan); }

   /** Creation à partir d'une position x,y dans l'image
    * @param plan plan d'appartenance
    * @param v vue de référence qui déterminera le PlanBase
    * @param x,y  position
    * @param id identificateur spécifique, ou null pour attribution automatique
    */
   protected SourceStat(Plan plan, ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,id);
   }

   /** Creation à partir d'une position céleste
    * @param plan plan d'appartenance
    * @param v vue de référence qui déterminera le PlanBase
    * @param c coordonnées
    * @param id identificateur spécifique, ou null pour attribution automatique
    */
   protected SourceStat(Plan plan,ViewSimple v, Coord c,String id) {
      super(plan,v,c,id);
   }
   
   /** Post-traitement lors de la création */
   protected void suite() {
      setLeg(legende);
      setId();
      resumeMesures();
   }
   
   private int order=0;   // -1 max, 0 courant, ou explicite
   
   public void setOrder(String order) throws Exception {
      if( order.equalsIgnoreCase("max") ) this.order=-1;
      else this.order=Integer.parseInt(order);
//      System.out.println("setOrder="+this.order);
      resumeMesures();
  }

   /** (Re)énération de la ligne des infos (détermine les mesures associées) */
   protected void resumeMesures() {
      double stat[] = null;
      int z=-1;
      
      String nomPlan = planBase.label;
      if( planBase.isCube() ) {
         z=(int)planBase.getZ();
         int d = 1+z;
         nomPlan+="/"+d;
      }
      
      try { stat = getStatistics(planBase,z); }
      catch( Exception e ) { stat=null; }
      
//      System.out.println("resumeMesures z="+z+" for "+id);
      
      String cnt  = stat==null ? " " : ""+stat[0];
      String tot  = stat==null ? " " : ""+stat[1];
      String avg  = stat!=null && stat[0]>0 ? ""+stat[1]/stat[0] : " ";;
      String sig  = stat==null ? " " : ""+stat[2];
      String surf = stat==null ? " " : ""+stat[3]*3600;
      String rad  = ""+getRadius()*60;
      String min = stat==null ? " " : ""+stat[4];
      String max = stat==null ? " " : ""+stat[5];
      
      Coord c = new Coord(raj,dej);
      
      if( planBase instanceof PlanBG ) {
         PlanBG pbg = (PlanBG)planBase;
         int orderFile=order==-1 ? pbg.maxOrder : order==0 ?  pbg.getOrder() : order;
         nomPlan=orderFile+"/"+nomPlan;
      }
      
      String fov = "CIRCLE ICRS "+raj+" "+dej+" "+getRadius();
      setFootprint(fov);
      setIdxFootprint(3);
      
      info = "<&_A Phots>\t"+raj+"\t"+dej+"\t"+id+"\t"+fov+"\t"+nomPlan+"\t"+"\t"+c.getRA()+"\t"+c.getDE()+"\t"+cnt+"\t"+tot+"\t"+sig+"\t"+min+"\t"+avg+"\t"+max+"\t"+rad+"\t"+surf;
   }
   
   /** Retourne la liste des Propriétés éditables */
   public Vector getProp() {
      Vector propList = super.getProp();

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
               ((SourceStat)myself).setRadius(nval);
               return PropAction.SUCCESS;
            } catch( Exception e1 ) {
               updateRadius.action();
               testRadius.setForeground(Color.red);
            }
            return PropAction.FAILED;
         }
      };
      propList.add( Prop.propFactory("radius","Radius","",testRadius,updateRadius,changRadius) );
      
      // Déplaçable/etirable ou non ?
      final JCheckBox lockCheck =  new JCheckBox("mouse locked");
      lockCheck.setSelected(isLocked());
      final PropAction changeLock = new PropAction() {
         public int action() {
            if( lockCheck.isSelected()==isWithLabel() ) return PropAction.NOTHING;
            setLocked( lockCheck.isSelected() );
            return PropAction.SUCCESS;
         }
      };
      lockCheck.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeLock.action(); plan.aladin.view.repaintAll(); }
      });
      propList.add( Prop.propFactory("lock","Locked","Not movable and/or extensible by mouse",lockCheck,null,changeLock) );


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

   /** Déplacement par coordonnées absolues images + maj des mesures */
   protected void setPosition(ViewSimple v,double x, double y)   {
      if( isLocked() ) return;
      setPosition1(v,x,y);  
      resume();
   }
   
   /** Déplacement par coordonnées relatives images + maj des mesures */
   protected void deltaPosition(ViewSimple v,double x, double y) {
      if( isLocked() ) return;
      deltaPosition1(v,x,y);
      resume();
   }
   
   /** Déplacement par coordonnées relatives célestes + maj des mesures */
  protected void deltaRaDec(double dra, double dde) {
      if( isLocked() ) return;
      deltaRaDec1(dra,dde); 
      resume();
   }

   /** Determine le decalage pour ecrire l'id */
   void setD() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw = m.stringWidth(id)+4;
      dh=HF;
   }

   /** Positionnement d'une couleur spécifique */
   public void setColor(Color c) { couleur=c; }

   /** Positionne un rayon (avec possibilité d'une unité) + maj des mesures  */
   protected void setRadius(String r) {
      radius = Server.getAngleInArcmin(r,Server.RADIUS)/60.;
      resume();
   }

   /** Change le rayon d'un repère CERCLE (r en pixels dans le plan de ref de v)  + maj des mesures */
   void setRayon(ViewSimple v,double r) {
      Coord c = new Coord();
      Projection proj  = v.getProj();
      c.al=raj;
      c.del=dej;
      proj.getXY(c);

      c.y+=r;
      proj.getCoord(c);
      
      radius=Math.abs(dej-c.del);
      resume();
   }

   /** Positionnement d'un ID particulier */
   protected void setId(String s) { id=s; setD(); }

   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve sur le texte
    * @param x,y le point a tester
    * @param z valeur courante du zoom
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      double xc = Math.abs(x-xv[v.n]);
      double yc = Math.abs(y-yv[v.n]);
      return( Math.sqrt(xc*xc + yc*yc) <= getRayon(v) );
   }

   /** Détermination du clip englobant */
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      int l = (int)Math.ceil(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,l,l);
      if( p==null ) return clip;

      int D=l;
      clip = unionRect(clip, p.x-D,p.y-D,D*2,D*2);
      if( isSelected() )  clip = unionRect(clip, p.x-l-DS,p.y-l-DS,l*2+DDS,l*2+DDS);
      if( isWithLabel() ) clip = unionRect(clip, p.x-dw/2,p.y-L-1-dh-1,dw,dh);
      if( isWithStat() )  clip = unionRect(clip,getStatPosition(v));
      return clip;
   }
   /** Donne la boite englobante afin de dessiner les poignées */
   protected Rectangle getClipRayon(ViewSimple v ) {
      Rectangle clip=null;
      if( !isVisible() ) return null;
      int L = (int)Math.ceil(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,L,L);
      if( p==null ) return null;
      if( isSelected() ) {
         clip = unionRect(clip, p.x-L-DS,p.y-L-DS,L*2+DDS,L*2+DDS);
      }
      return clip;
   }

   /** Détermination de la couleur de l'objet */
   public Color getColor() {
      if( couleur!=null ) return couleur;

      if( plan!=null ) {
         if( plan.type==Plan.APERTURE ) couleur = ((PlanField)plan).getColor(this);
         else return plan.c;
      } else return Color.black;
      return couleur;
   }


   /** Calcule des statistiques à la volée en fonction du plan de base de la vue passée en paramètre */
   protected boolean statCompute(Graphics g,ViewSimple v, int z) {

      boolean flagHist = v==v.aladin.view.getCurrentView();

      if( v==null || v.isFree() || !hasPhot(v.pref) ) return false;

      // Si cercle large ou s'il s'agit d'un allsky, on ne calcule pas pendant le changement de taille ou les déplacements
      if( getRayon(v)>100 && v.flagClicAndDrag) return false;
      
      pixelStats.reinit( getPixelStatsCle(v.pref, z) );
      double tripletPix [] = pixelStats.getStatisticsRaDecPix();

      HistItem onMouse=null;
      if( flagHist ) {
         onMouse =v.aladin.view.zoomview.hist==null ? null :  v.aladin.view.zoomview.hist.onMouse;

         // Si le est simplement dû au passage de la souris sur un précédent histogramme,
         // il ne faut pas regénérer cet histogramme
         if( onMouse==null ) v.aladin.view.zoomview.initPixelHist();
         else flagHist=false;
      }
      
      // On colore les pixels qu'il faut, et on met à jour l'histogramme
      for( int i=0; i<tripletPix.length; i+=3 ) {
         double ra =tripletPix[i];
         double de =tripletPix[i+1];
         double val=tripletPix[i+2];
         if( v.pref instanceof PlanBG ) statPixelBG(g,val,ra,de,v,onMouse);
         else statPixel(g,val,ra,de,v,onMouse);
         if( flagHist ) v.aladin.view.zoomview.addPixelHist(val);
      }


      if( flagHist ) v.aladin.view.zoomview.createPixelHist(v.pref.type==Plan.ALLSKYIMG ? "HEALPixels":"Pixels");
      setWithStat(true);
      
      return true;
   }

   
   private PixelStats pixelStats = new PixelStats();
   
   /** Retourne une clé unique associé aux statistiques courantes */
   String getPixelStatsCle(Plan p, int z) { 
      if( z==-1 && p.isCube() ) z=(int)p.getZ();
      String sync = p.isSync() ? "sync":"";
      return raj+","+dej+","+radius+","+p.hashCode()
            + ","+sync
            + (p.isCube() ? ","+z : "")
            + (p instanceof PlanBG ? ((PlanBG)p).getOrder()+"" : "");
   }
   
   /** Retourne la liste des triplets associées aux pixels des statistiques (raj,dej,val)
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return le tableau des triplets
    * @throws Exception
    */
   public double [] getStatisticsRaDecPix(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      return pixelStats.getStatisticsRaDecPix();
   }
   
   /** Retourne les statistiques en fonction du plan passé en paramètre
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return Nombre, total, sigma, surface, min, max, [median]
    */
   public double [] getStatistics(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      boolean withMedian = pixelStats.nb<MAXMEDIANE;
      return pixelStats.getStatistics( withMedian );
   }
   
   /** Regénère si nécessaire les statistiques associées à l'objet
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @param withMedian true si on veut également la valeur médiane
    * @return true si les stats ont été regénérées, false si inutile
    * @throws Exception
    */
   private boolean resumeStatistics(Plan p, int z) throws Exception {
      
      Projection proj = p.projd;
      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
//      if( radius<=0 ) throw new Exception("getStats error: no radius");
      
      // Faut-il re-extraire les pixels concernés par la stat ?
      String cle = getPixelStatsCle(p,z);
      if( !pixelStats.reinit( cle ) ) return false;
      
      double pixelSurf;
      int nombre=0;

      // Cas HiPS
      if( p.type==Plan.ALLSKYIMG || p.type==Plan.ALLSKYCUBE ) {

         PlanBG pbg = (PlanBG) p;
         int orderFile = pbg.getOrder();
         long nsideLosange = CDSHealpix.pow2(pbg.getTileOrder());
         int orderPix = pbg.getOrder() + pbg.getTileOrder();
         pixelSurf = CDSHealpix.pixRes(orderPix)/3600;
         pixelSurf *= pixelSurf;
         Coord coo = new Coord(raj,dej);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
         double radiusRadian = Math.toRadians(getRadius());
         long [] npix = CDSHealpix.query_disc(orderPix, coo.al, coo.del, radiusRadian, false);
         for( int i=0; i<npix.length; i++ ) {
            long npixFile = npix[i]/(nsideLosange*nsideLosange);
            double pix = pbg.getHealpixPixel(orderFile,npixFile,npix[i],z,HealpixKey.SYNC);
            if( Double.isNaN(pix) ) continue;
            pix = pix*pbg.bScale+pbg.bZero;
            double polar[] = CDSHealpix.pix2ang_nest(orderPix, npix[i]);
            polar = CDSHealpix.polarToRadec(polar);
            coo.al = polar[0]; coo.del = polar[1];
            coo = Localisation.frameToFrame(coo,pbg.frameOrigin,Localisation.ICRS);

            nombre=pixelStats.addPix(coo.al,coo.del, pix);
         }

         // Cas d'une image ou d'un cube "classique"
      } else {
         boolean isCube = p instanceof PlanImageBlink;
         PlanImage pi = (PlanImage)p;

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

         try {
            // Cas d'une image "classique"
            if( !isCube ) {
               pi.setLockCacheFree(true);
               pi.pixelsOriginFromCache();

               // Pour un cube
            } else {
               if( z<0 || z>((PlanImageBlink)pi).getDepth() ) throw new Exception("Cube index out of frame range");
            }

            for( int y=miny; y<=maxy; y++ ) {
               for( int x=minx; x<=maxx; x++ ) {
                  if( (x-xc)*(x-xc) + (y-yc)*(y-yc) > carreRayon ) continue;
                  if( !pi.isIn(x,y) ) continue;
                  double pix= isCube ? ((PlanImageBlink)pi).getPixel(x, pi.height-y-1, z) : pi.getPixelInDouble(x,y);
                  if( Double.isNaN(pix) ) continue;
                  pix = pix*pi.bScale+pi.bZero;

                  c.x=x+0.5; 
                  c.y=y+0.5;
                  proj.getCoord(c);

                  nombre=pixelStats.addPix(c.al,c.del, pix);
               }
            }
         } finally {
            if( !isCube ) pi.setLockCacheFree(false);
         }
      }

      pixelStats.setSurface( nombre*pixelSurf );
      return true;
   }


   /** Retourne la rayon du repère en degrés */
   public double getRadius() { return radius; }

   /** Retourne true si le repère a un rayon associé */
   protected boolean hasRayon() { return radius>0; }

   /** Retourne true si l'objet contient des informations de photométrie  */
   public boolean hasPhot() { return true; }
   public boolean hasPhot(Plan p) {
      if( !hasPhot() ) return false;
      return p.hasAvailablePixels();
   }
   
   public boolean hasSurface() { return radius>0; }

   /** Retourne la commande script équivalente */
   public String getCommand() {
      String r;
      if( plan.aladin.localisation.getFrame()==Localisation.XY ) r=Util.myRound(getRayon(plan.aladin.view.getCurrentView()));
      else r=Coord.getUnit(getRadius());
      return "draw phot("+getLocalisation()+","+r+")";
   }

   /** Retourne le rayon en pixels  */
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

   /** Retourn true si la position x,y se trouve sur une des
    * 4 poignées de controle (en haut, en bas, à droite et à gauche */
   protected boolean onPoignee(ViewSimple v,double x, double y) {
      if( isLocked() ) return false;
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
   
   /** Retourne la position en unité View des stats */
   protected Rectangle getStatPosition(ViewSimple v) {
      int l = (int)(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,l,l);
      return new Rectangle(p.x+l+4, p.y-l,LARGSTAT,HAUTSTAT);
   }

   /** Tracé effectif */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      int l = (int)(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,l,l);

      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor() );
      if( hasPhot(v.pref) && v.pref==planBase ) {
         Util.drawFillOval(g, p.x-l, p.y-l, l*2, l*2, 0.2f * plan.getOpacityLevel(), null);
      } else g.drawOval(p.x-l, p.y-l, l*2, l*2);
      
      if( isWithLabel() ) g.drawString(id,p.x-dw/2,p.y-1);
      
      if( hasPhot(v.pref) && isSelected() ) statDraw(g, v, p.x,p.y, p.x+l+4, p.y-l);
     
      if( isSelected()  ) {
         g.setColor( Color.green );
         drawSelect(g,v);
      }
      
      return true;
   }

   protected void drawSelect(Graphics g,ViewSimple v) {
      Rectangle r = getClipRayon(v);
      int xc=0;
      int yc=0;
      Color c = g.getColor();

      // Trace des poignees de selection
      for( int i=0; i<4; i++ ) {
         switch(i ) {
            case 0: xc=r.x+r.width/2-DS; yc=r.y; break;                // Bas
            case 1: xc=r.x+r.width/2-DS; yc=r.y+r.height-DS; break;    // Haut
            case 2: xc=r.x+r.width-DS; yc=r.y+r.height/2-DS;  break;   // Droite
            case 3: xc=r.x; yc=r.y+r.height/2-DS;  break;              // Gauche
         }
         g.setColor( c );
         g.fillRect( xc+1,yc+1 , DS,DS );
         g.setColor( Color.black );
         g.drawRect( xc,yc , DS,DS );
      }
      g.setColor( c );
   }

   protected void remove() { cutOff(); }

   /** Suppression de la coupe memorise dans le zoomView
    * => arret de son affichage
    */
   protected void cutOff() {
      plan.aladin.calque.zoom.zoomView.stopHist();
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
      Plan pc=v.pref;
      if( !pc.isCube() ) return false;

      double x= xv[v.n];
      double y= yv[v.n];
      int n=pc.getDepth();
      int res[] = new int[n];
      try {
         for( int z=0; z<n; z++ ) res[z] = (pc.getPixel8bit(z,x,y)) & 0xFF;
      } catch( Exception e ) {}

      plan.aladin.calque.zoom.zoomView.setCut(this,res,ZoomView.CUTNORMAL);

      return true;
   }

   protected boolean asSource() { return true; }

}
