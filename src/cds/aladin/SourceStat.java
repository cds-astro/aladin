// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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
 * Objet graphique representant une mesure photom�trique manuelle
 *
 * @author Pierre Fernique [CDS]
 * @version 3.0 (juin 2016): Refonte compl�te depuis Repere.java
 * @version 3.1 (f�v 2021): D�coupage en deux classes : SourceInfo et SourceStat
 */
public class SourceStat extends SourceInfo {
   
   protected int L = 5;          // taille des poign�es de saisie
   private double radius;        // Rayon en degr�s image du cercle englobant
   protected int dw,dh;          // mesure du label
   protected Color couleur=null; // Couleur alternative
   private StatPixels statPixels = new StatPixels();   // Gestion des stats associ�es
   
   /** Creation pour les backups */
   protected SourceStat(Plan plan) { super(plan); }

   /** Creation � partir d'une position x,y dans l'image
    * @param plan plan d'appartenance
    * @param v vue de r�f�rence qui d�terminera le PlanBase
    * @param x,y  position
    * @param id identificateur sp�cifique, ou null pour attribution automatique
    */
   protected SourceStat(Plan plan, ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,id);
   }

   /** Creation � partir d'une position c�leste
    * @param plan plan d'appartenance
    * @param v vue de r�f�rence qui d�terminera le PlanBase
    * @param c coordonn�es
    * @param id identificateur sp�cifique, ou null pour attribution automatique
    */
   protected SourceStat(Plan plan,ViewSimple v, Coord c,String id) {
      super(plan,v,c,id);
   }
   
   /** Post-traitement lors de la cr�ation */
   protected void suite() {
      setId();
   }
   
   protected int order=0;   // -1 max, 0 courant, ou explicite
   
   public void setOrder(String order) throws Exception {
      if( order.equalsIgnoreCase("max") ) this.order=-1;
      else this.order=Integer.parseInt(order);
  }
   
   protected void resumeMesures() { }

   /** Retourne la liste des Propri�t�s �ditables */
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
      
      // D�pla�able/etirable ou non ?
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

   /** Retourne une chaine contenant toutes les informations techniques � sauvegarder dans un fichier AJ
    * afin de pouvoir reg�n�rer le rep�re, m�me s'il a un rayon */
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
   
   /** Retourne le FoV � la STC-S */
   protected String getFoV( ) { return "CIRCLE ICRS "+raj+" "+dej+" "+getRadius(); }

   /** Retourne le type d'objet */
   public String getObjType() { return "Phot"; }

   /** D�placement par coordonn�es absolues images + maj des mesures */
   protected void setPosition(ViewSimple v,double x, double y)   {
      if( isLocked() ) return;
      setPosition1(v,x,y);  
      resume();
   }
   
   /** D�placement par coordonn�es relatives images + maj des mesures */
   protected void deltaPosition(ViewSimple v,double x, double y) {
      if( isLocked() ) return;
      deltaPosition1(v,x,y);
      resume();
   }
   
   /** D�placement par coordonn�es relatives c�lestes + maj des mesures */
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

   /** Positionnement d'une couleur sp�cifique */
   public void setColor(Color c) { couleur=c; }

   /** Positionne un rayon (avec possibilit� d'une unit�) + maj des mesures  */
   protected void setRadius(String r) {
      radius = Server.getAngleInArcmin(r,Server.RADIUS)/60.;
      resume();
   }

   /** Change le rayon d'un rep�re CERCLE (r en pixels dans le plan de ref de v)  + maj des mesures */
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

   /** D�termination du clip englobant */
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
   /** Donne la boite englobante afin de dessiner les poign�es */
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

   /** D�termination de la couleur de l'objet */
   public Color getColor() {
      if( couleur!=null ) return couleur;

      if( plan!=null ) {
         if( plan.type==Plan.APERTURE ) couleur = ((PlanField)plan).getColor(this);
         else return plan.c;
      } else return Color.black;
      return couleur;
   }

   private boolean flagLiveMesure=true;
   
//   // True s'il est possible de faire les mesures stats en live (clic&drag)
//   private boolean doLiveMesure() { return flagLiveMesure; }
   
   // Teste s'il est possible de faire les mesures stats en live (cf resumeMesures(). Si le dernier calcul
   // �tait trop lent (20ms), inhibe les prochains calculs jusqu'� ce que le rayon de redevienne
   // suffisamment petit
   private boolean tooSlow(ViewSimple v) {
      long t=0;
      if( v.flagClicAndDrag ) {
         if( flagLiveMesure) {
            t = statPixels.getTime();
            if( t>20 ) flagLiveMesure=false;
         } else if( getRayon(v)<100 ) flagLiveMesure=true;
      } else {
         if( !flagLiveMesure ) {
            flagLiveMesure=true;
            resume();
         }
      }
      return !flagLiveMesure;
   }

   /** Calcule des statistiques � la vol�e en fonction du plan de base de la vue pass�e en param�tre */
   protected boolean statCompute(Graphics g,final ViewSimple v, int z) {

      boolean flagHist = v==v.aladin.view.getCurrentView();

      if( v==null || v.isFree() || !hasPhot(v.pref) ) return false;
      
      if( tooSlow(v) ) return false;
      
      double tripletPix [];
      try {
         getStatistics(v.pref,z);
         tripletPix = statPixels.getStatisticsRaDecPix();
      } catch( Exception e ) { return false; }

      HistItem onMouse=null;
      if( flagHist ) {
         onMouse =v.aladin.view.zoomview.hist==null ? null :  v.aladin.view.zoomview.hist.onMouse;

         // Si le est simplement d� au passage de la souris sur un pr�c�dent histogramme,
         // il ne faut pas reg�n�rer cet histogramme
         if( onMouse==null ) v.aladin.view.zoomview.initPixelHist(this);
         else flagHist=false;
      }
      
      // On colore les pixels qu'il faut, et on met � jour l'histogramme
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
   
   /** Retourne une cl� unique associ� aux statistiques courantes */
//  protected int getStatsHashcode(Plan p, int z) {
//      int k= p.hashCode();
//      k = k*13 + (raj+"").hashCode();
//      k = k*17 + (dej+"").hashCode();
//      k = k*19 + (radius+"").hashCode();
//      if( p.isSync() ) k = k*23;
//      if( z==-1 && p.isCube() ) z = (int)p.getZ();
//      k = k*29 + z;
//      if( p instanceof PlanBG ) k = k*31  + ((PlanBG)p).getOrder();
//      return k;
//   }
   
   protected int getStatsHashcode(Plan p, int z) { return getPixelStatsCle(p,z).hashCode(); }
   
   /** Retourne une cl� unique associ� aux statistiques courantes */
   protected String getPixelStatsCle(Plan p, int z) { 
      if( z==-1 && p.isCube() ) z=(int)p.getZ();
      String sync = p.isSync() ? "sync":"";
      return raj+","+dej+","+radius+","+p.hashCode()
            + ","+sync
            + (p.isCube() ? ","+z : "")
            + (p instanceof PlanBG ? ((PlanBG)p).getOrder()+"" : "");
   }
   
   /** Retourne la liste des triplets associ�es aux pixels des statistiques (raj,dej,val)
    * @param p Le plan de base concern�e
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return le tableau des triplets
    * @throws Exception
    */
   public double [] getStatisticsRaDecPix(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      return statPixels.getStatisticsRaDecPix();
   }
   
   /** Retourne les statistiques en fonction du plan pass� en param�tre
    * @param p Le plan de base concern�e
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return Nombre, total, sigma, surface, min, max, [median]
    */
   public double [] getStatistics(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      boolean withMedian = statPixels.nb<MAXMEDIANE;
      return statPixels.getStatistics( withMedian );
   }
   
   private static int CLE =0;
   
   /** Reg�n�re si n�cessaire les statistiques associ�es � l'objet
    * @param p Le plan de base concern�e
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @param withMedian true si on veut �galement la valeur m�diane
    * @return true si les stats ont �t� reg�n�r�es, false si inutile
    * @throws Exception
    */
   private boolean resumeStatistics(Plan p, int z) throws Exception {
      
      Projection proj = p.projd;
      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
//      if( radius<=0 ) throw new Exception("getStats error: no radius");
      
      // Faut-il re-extraire les pixels concern�s par la stat ?
      String cle = getPixelStatsCle(p,z);
      if( !statPixels.reinit( cle ) ) return false;
      
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

            nombre=statPixels.addPix(coo.al,coo.del, pix);
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

                  c.x=x+0.5; 
                  c.y=y+0.5;
                  proj.getCoord(c);

                  nombre=statPixels.addPix(c.al,c.del, pix);
               }
            }
         } finally {
            if( !isCube ) pi.setLockCacheFree(false);
         }
      }

      statPixels.setSurface( nombre*pixelSurf );
      return true;
   }


   /** Retourne la rayon du rep�re en degr�s */
   public double getRadius() { return radius; }

   /** Retourne true si le rep�re a un rayon associ� */
   protected boolean hasRayon() { return radius>0; }

   /** Retourne true si l'objet contient des informations de photom�trie  */
   public boolean hasPhot() { return true; }
   public boolean hasPhot(Plan p) {
      if( !hasPhot() ) return false;
      return p.hasAvailablePixels();
   }
   
   public boolean hasSurface() { return radius>0; }

   /** Retourne la commande script �quivalente */
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
    * 4 poign�es de controle (en haut, en bas, � droite et � gauche */
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
   
   /** Retourne la position en unit� View des stats */
   protected Rectangle getStatPosition(ViewSimple v) {
      int l = (int)(getRayon(v)*v.getZoom());
      Point p = getViewCoord(v,l,l);
      return new Rectangle(p.x+l+4, p.y-l,LARGSTAT,HAUTSTAT);
   }

   /** Trac� effectif */
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
    * @return true si le CutGraph a pu �tre fait
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

   protected boolean asSource() { return false; }

}
