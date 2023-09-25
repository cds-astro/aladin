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
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashMap;

import cds.image.EPSGraphics;
import cds.tools.Util;
import cds.xml.Field;

import cds.savot.model.SavotResource;

/**
 * Objet graphique correspondant a une source d'un catalogue
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class Source extends Position implements Comparator {

   static final int MDS = DS/2;      // demi-taille des poignees de selection
//   static public int L = 3;          // demi-taille de la source
   
   protected int getL() {
      if( plan==null || plan.getScalingFactor()==1) return 3;
      return (int)( (2*plan.getScalingFactor()/3.)*3 );
   }

   // Gestion des formes en fonction du nombre d'elements
   static final int [] LIMIT =      { 3,     10,       100,      250,   500,   1000,       2000,   5000,          13000, 100000 };
   static final String [] TYPENAME= { "oval","square","circle","rhomb","cross","triangle","plus","small circle","dot","microdot",
      "solid oval","solid square","solid circle","solid rhomb","solid triangle" };

   protected byte sourceType=SQUARE;    //Type de representation de la source par d�faut (CARRE, ...)
   protected String info;       // Information supplementaire associee a la source (en plus de id)
   private Legende leg;       // La legende associee a la source
   private String oid=null;     // L'OID de la source s'il a ete defini

   /**** variables li�s aux filtres ****/
   // TODO : � remplacer par un objet, ce qui �viterait d'avoir 4 double pour chaque Source si on en utilise que 2 par exemple
   // stockage de valeurs pour les filtres (Thomas)
   // values[i][j][k] est la keme valeur pour la jeme action du filtre numero i
   protected double[][][] values;
   // isSelected[i]==true si la source est selectionnee par le filtre numero i
   protected boolean[] isSelected;
   // actions associee a la source lorsque les filtres sont actifs
   // null si aucune action associee (draw classique)
   // action[i][j] = j_eme action pour le PlanFilter numero i
   protected Action[][] actions;

   /**** objet wrappant les infos relatives au footprint associ� � la source ****/
   protected SourceFootprint sourceFootprint;
   
   /**** objet wrappant les infos relatives au serviceDescriptor associ� � la source ****/
   protected SimpleData sourceServiceDescriptor ;
   /** For plugin */
   protected Source() {}
   
   /** For SourcePhot */
   protected Source(Plan plan, ViewSimple v,double x, double y, double raj, double dej,int methode, String id) {
      super(plan,v,x,y,raj,dej,methode,id);
   }
   
   /** For SourcePhot */
   protected Source(Plan plan) { super(plan); }
   
  /** Creation d'un objet source
   * @param plan plan d'appartenance de la ligne
   * @param raj,dej  coordonnees de l'objet
   * @param id       identificateur de l'objet
   * @param info     information supplementaire
   */
   protected Source(Plan plan, double raj, double dej, String id, String info) {
      super(plan,null,0.,0.,raj,dej,RADE,id);
      this.info = info;
      sourceType=(byte)plan.sourceType;
      
      fixInfo();
   }
   
   /** Creation d'un objet source
    * @param plan plan d'appartenance de la ligne
    * @param raj,dej  coordonnees de l'objet
    * @param jdtime   date associ�e � l'objet
    * @param id       identificateur de l'objet
    * @param info     information supplementaire
    */
    protected Source(Plan plan, double raj, double dej, double jdtime, String id, String info) {
       this(plan,raj,dej,id,info);
       this.jdtime = jdtime;
    }

  /** Creation d'un objet source
   * @param plan plan d'appartenance de la ligne
   * @param raj,dej  coordonnees de l'objet
   * @param id       identificateur de l'objet
   * @param info     information supplementaire
   */
   protected Source(Plan plan, double raj, double dej, String id,
                    String info, Legende leg) {
      super(plan,null,0.,0.,raj,dej,RADE,id);
      this.info = info;
      this.leg=leg;
      sourceType=(byte)plan.sourceType;
      
      fixInfo();
   }

  /** Creation d'un objet source (methode generale)
   * @param plan plan d'appartenance de la ligne
   * @param x,y  coordonnees de l'objet
   * @param raj,dej  coordonnees de l'objet
   * @param methode (Voir position.class)
   * @param id       identificateur de l'objet
   * @param info     information supplementaire
   */
   protected Source(Plan plan,
                    double x, double y, double raj,double dej, int methode,
                    String id, String info, Legende leg) {
      super(plan,null,x,y,raj,dej,methode,id);
      this.info = info;
      this.leg=leg;
      sourceType=(byte)plan.sourceType;
      
      fixInfo();

   }
   
   /** Retourne la l�gende associ�e � la source */
   public Legende getLeg() {
      
      // Dans le cas d'un plan HiPs catalogue, la l�gende peut �tre g�n�rique
      if( plan!=null && plan instanceof PlanBGCat ) {
         Legende l = plan.getFirstLegende();
         if( l!=null ) return l;
      }
      
      return leg;
   }

   /** Positionne la l�gende associ�e � la source */
   protected void setLeg(Legende leg) { this.leg = leg; }

   /** Accroit ou d�croit la taille du type de source */
   void increaseSourceSize(int sens) { 
      sourceType+=sens;
      if( sourceType>=TYPENAME.length ) sourceType=(byte)(TYPENAME.length-1);
      else if( sourceType<0 ) sourceType=0;
   }

   public boolean hasProp() { return false; }
   
   /** Projection de la source => calcul (x,y).
    * @param proj la projection a utiliser
    */
    protected void projection(ViewSimple v) {

       if( v.isPlot() ) {
          double [] xy = v.plot.getXY(this);
          xv[v.n] = xy[0];
          yv[v.n] = xy[1];

       } else {
           super.projection(v);
       }
    }

   /** Positionne le flag de tag */
   final protected void setTag(boolean tag) {
      if( plan!=null && tag ) plan.aladin.calque.taggedSrc=true;
      if( tag ) flags |= TAG;
      else flags &= ~TAG;
   }

   /** Retourne true si la source est tagu�e */
   final protected boolean isTagged() { return (flags & TAG) !=0; /* == TAG;*/ }

   /** Positionne le flag de mise en �vidence temporaire */
   final protected void setHighlight(boolean fl) {
      if( fl ) flags |= HIGHLIGHT;
      else flags &= ~HIGHLIGHT;
   }

   /** Retourne true si la source est mise en �vidence temporairement */
   final protected boolean isHighlighted() { return (flags & HIGHLIGHT) !=0; /* == HIGHLIGHT;*/ }

   /** Retourne true si l'objet contient des informations de photom�trie  */
   public boolean hasPhot() { return getLeg().hasGroup(); }

   /** Retourne le nom de la forme en fonction de l'indice */
   static protected final String getShape(int i) { return TYPENAME[i]; }

   /** Retourne l'indice de la forme en fonction de son nom, -1 si introuvable */
   static protected final int getShapeIndex(String shape) {
      for( int i=0; i<TYPENAME.length; i++ ) if( shape.equalsIgnoreCase(TYPENAME[i]) ) return i;
      return -1;
   }

   /** fix for the AVO demo : sometimes, info is shorter than leg ! */
   protected void fixInfo() {
       if( getLeg()==null || getLeg().field==null || info==null ) return;

       StringTokenizer st = new StringTokenizer(this.info,"\t");
       int nbInfo = st.countTokens()-1; // skip du triangle
       
       int nbFields = getLeg().field.length;

       while( nbInfo<nbFields ) {
           this.info = new String(this.info)+"\t ";
           nbInfo++;
           if( Aladin.levelTrace>=3) System.err.println("Source.fixInfo() =>  pour "+id);
       }
   }
   
   /**
    * Retourne l'identificateur unique de la source, fourni par une application
    * externe tel VOPlot. S'il ne s'agit pas d'un objet de ce type, retourne
    * simplement null
    */
   protected String getOID() { return oid; }

   /**
    * Positionnement d'un OID sur cette source
    * @param oid
    */
   protected void setOID(String oid ) { this.oid = oid; }

   /**
    * Generation automatique d'un OID, unique pour la session
    * IL VA FALLOIR FAIRE GAFFE, SI UN UTLISATEUR CHARGE UN VOTABLE AVEC DES OID
    * AYANT ETE GENERES A LA SESSION PRECEDENTE. IL PEUT Y AVOIR DES
    * MELANGES !!!
    * @return l'OID genere
    */
   static int NOID=0;
   protected String setOID() {
      NOID++;
      oid = "Aladin."+NOID;
      return oid;
   }

/*
   // Affichage sur sortie standard des infos concernant l'objet
   void debug() {
      super.debug();
      String s;
      s=Coord.getSexa(raj,dej,":");
      System.out.println("   .coordonnees J2000    : "+raj+","+dej+" (cad "+s+")");
      System.out.println("   .representation       : "+TYPENAME[sourceType]);
      System.out.println("   .information          : "+info);
      System.out.println("   .taille               : "+L);
      System.out.println("   .affiche avec label   : "+withlabel);
      System.out.println("   .montre par inversion : "+show);
   }
*/

  /** Representation par defaut.
   * Retourne le type de representation de la source en fonction du nombre de source.
   * Le principe est tres simple, plus il y a de sources concernees, plus
   * la representation sera petite.
   *
   * @param nombre nombre de sources concernees
   * @return numero de la representation (ex: Source.CARRE)
   */
   protected static int getDefaultType(int nombre) {
      int i;
      for( i=0; i<LIMIT.length && nombre>LIMIT[i]; i++ );
      if( i>=LIMIT.length ) i=LIMIT.length-1;
      return i;
   }

  /** Modification de l'identificateur
   * @param id nouvel identificateur de la source
   */
   protected void setText(String id) {
      super.setText(id);
   }

  /** Modification de l'information associee a la source
   * @param info la nouvelle info supplementaire
   */
   public void setInfo(String info) { 
      this.info = info; 
      oid="";
   }

  /** Modification de la legende associee a la source
   * @param leg la nouvelle legende
   */
   protected void setLegende(Legende leg) {  this.leg=leg; oid=""; }

   // Affichage de l'info lie a la source
   protected void info(Aladin aladin) { aladin.mesure.setInfo(this); }

  /** Affichage l'info lie a l'objet
   * Affiche l'identifacteur dans le statut de l'objet aladin
   * @param aladin reference
   */
   protected void status(Aladin aladin) {
      aladin.status.setText(id+" "+aladin.view.HCLIC);
   }

  /** Modification de la position (absolue)
   * @param x,y nouvelle position
   */
   protected void setPosition(ViewSimple v,double x, double y) { }

  /** Modification de l'identificateur
   * @param id nouvel identificateur
   */
   protected void deltaPosition(ViewSimple v,double dx, double dy) {
      if( plan==null || !plan.recalibrating ) return;
      xv[v.n] += dx;
      yv[v.n] += dy;
//      resetVP();
   }

   static private Rectangle box = new Rectangle();      // Box qui contient le label + la source

   // Calcul le decalage du label en fct de la font
   // et de la taille de la source. On utilise une variable statique pour �viter
   // les allocations inutiles
   private Rectangle setBox() { return setBox(null); }
   private Rectangle setBox(Graphics g) {
      int dx,dy,dw,dh;
      if( g!=null ) g.setFont(DF);
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw=dx = (byte)(m.stringWidth(id)/2);
      dh=dy = (byte)(HF/2);
      int L =getL();
      if( dx>L ) { dx=L-1; dw+=(dw-dx); }
      if( dy>L ) { dy=L-1; dh+=(dh-dy); }
      box.x=dx; box.y=dy; box.width=dw; box.height=dh;
      return box;
   }

  /** Test d'appartenance.
   * Retourne vrai si le point (x,y) de l'image se trouve dans l'objet
   * @param x,y le point a tester
   * @param z zoom courant
   * @return <I>true</I> si on est dedans, <I>false</I> sinon
   */
   protected boolean inside(ViewSimple v,double x, double y) {
      int L =getL();
      double l=L/v.getZoom();
      double xc = xv[v.n];
      double yc = yv[v.n];
      return(xc<=x+l && xc>=x-l && yc<=y+l && yc>=y-l);
   }

  /** Generation d'un clip englobant.
   * Retourne un rectangle qui englobe l'objet
   * @param zoomview reference au zoom courant
   * @return         le rectangle enblobant
   */
//   protected Rectangle getClip(ViewSimple v) {
//      if( !visible ) return null;
//      Point p = getViewCoord(v,L*2,L*2);
//      if( p==null ) return null;
//      if( !withlabel ) {
//         if( select ) return new Rectangle(p.x-L-MDS,p.y-L-MDS, L*2+DS, L*2+DS);
//         else return new Rectangle(p.x-L,p.y-L, L*2, L*2);
//      } else {
//         setBox();
//         if( select ) return new Rectangle(p.x-L-MDS,p.y-L-box.height-MDS, box.width+L*2+DS,box.height+L*2+DS);
//         else return new Rectangle(p.x-L,p.y-L-box.height, box.width+L*2,box.height+L*2);
//      }
//   }

   /** Extension d'un clip pour ajouter la zone de l'objet
    * A l'avantage de ne pas faire d'allocation
    */
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      int L =getL();
      Point p = getViewCoord(v,L*2,L*2);
      if( p==null ) return clip;

      if (sourceFootprint != null) {
          // TODO : �tendre le clip
      }

      if( !isWithLabel() ) {
         if( isSelected() ) return unionRect(clip, p.x-L-MDS,p.y-L-MDS, L*2+DS, L*2+DS);
         else return unionRect(clip, p.x-L,p.y-L, L*2, L*2);
      } else {
         setBox();
         if( isSelected() ) return unionRect(clip, p.x-L-MDS,p.y-L-box.height-MDS, box.width+L*2+DS,box.height+L*2+DS);
         return unionRect(clip, p.x-L,p.y-L-box.height, box.width+L*2,box.height+L*2);
      }
   }

   /** Teste l'intersection m�me partielle avec le clip */
   protected boolean inClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return false;
      int L =getL();
      Point p = getViewCoord(v,L*2,L*2);
      if( p==null ) return false;
      int x,y,w,h;
      if( !isWithLabel() ) {
         if( isSelected() ) { x=p.x-L-MDS; y=p.y-L-MDS; w=h=L*2+DS; }
         else { x=p.x-L; y=p.y-L; w=h=L*2; }
      } else {
         setBox();
         if( isSelected() ) { x=p.x-L-MDS; y=p.y-L-box.height-MDS; w=box.width+L*2+DS; h=box.height+L*2+DS; }
         else { x=p.x-L; y=p.y-L-box.height; w=box.width+L*2; h=box.height+L*2; }
      }
      return Obj.intersectRect(clip,x,y,w,h);
   }

   // Tracage d'un carre
   void drawCarre(Graphics g,Point p) { drawCarre(g,p,false); }
   void drawCarre(Graphics g,Point p,boolean solid) {
      int L =getL();
      
      if( !isWithLabel() ) {
         g.drawRect(p.x-L,p.y-L, L*2, L*2);
         if( solid ) g.fillRect(p.x-L,p.y-L, L*2, L*2);
      }
      else {
         setBox(g);
         g.drawLine(p.x+L,p.y-L+box.y+2,  p.x+L,p.y+L);
         g.drawLine(p.x-L,p.y+L, p.x+L,p.y+L);
         g.drawLine(p.x-L,p.y+L, p.x-L,p.y-L);
         g.drawLine(p.x-L,p.y-L, p.x+L-box.x-2,p.y-L);
         if( solid ) g.fillRect(p.x-L,p.y-L, L*2, L*2);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

//   final static int R=8;
//   final static int LR=6;
//   final static int SR=4;

   // Tracage d'un cercle
   void drawCircleS(Graphics g,Point p) {
      int L =getL();
      
      if( g instanceof EPSGraphics || L!=3 ) {
         int SR = (int)(L+L/3.);
//         if( SR%2==0 ) SR++;
         g.drawOval(p.x-SR/2, p.y-SR/2, SR, SR);
      } else Util.drawCircle5(g,p.x,p.y);

      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

   // Tracage d'un cercle
   void drawOval(Graphics g,Point p) { drawOval(g,p,false); }
      void drawOval(Graphics g,Point p,boolean solid) {
      int L =getL();
      int R = (int)( (L+L/3.)*2);
      g.drawOval(p.x-R/2, p.y-R/3, R, (2*R)/3);
      if( solid ) g.fillOval(p.x-R/2, p.y-R/3, R, (2*R)/3);
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

   // Tracage d'un cercle
   void drawCircle(Graphics g,Point p) { drawCircle(g,p,false); }
   void drawCircle(Graphics g,Point p,boolean solid) {
      int L =getL();
      if( g instanceof EPSGraphics || L!=3 )  {
         int LR = L*2;
//         if( LR%2==0 ) LR++;
         g.drawOval(p.x-LR/2, p.y-LR/2, LR, LR);
         if( solid )  g.fillOval(p.x-LR/2, p.y-LR/2, LR, LR);
      }
      else {
         if( solid ) Util.fillCircle7(g,p.x,p.y);
         else Util.drawCircle7(g,p.x,p.y);
      }

      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

   // Tracage d'un losange
   void drawLosange(Graphics g,Point p) { drawLosange(g,p,false ); }
   void drawLosange(Graphics g,Point p,boolean solid) {
      int L =getL();
      Polygon pol = new Polygon(new int[] {p.x,  p.x+L, p.x,  p.x-L},
                                new int[] {p.y-L,p.y,   p.y+L,p.y  },
                                4);
      g.drawPolygon(pol);
      if( solid ) g.fillPolygon(pol);
      
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

   // Tracage d'un triangle
   void drawTriangle(Graphics g,Point p) { drawTriangle(g,p,false); }
   void drawTriangle(Graphics g,Point p,boolean solid) {
      int L =getL();
      Polygon pol = new Polygon(new int[] {p.x-L,  p.x+L,   p.x},
                                new int[] {p.y+L/3,p.y+L/3, p.y+L,p.y-(2*L)/3  },
                                3);
      g.drawPolygon(pol);
      if( solid ) g.fillPolygon(pol);

      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x,p.y-L+box.y);
      }
   }

   // Tracage d'une croix (vertical/horizontal)
   void drawPlus(Graphics g,Point p) {
      int L =getL();
      g.drawLine(p.x-L,p.y, p.x+L,p.y );
      g.drawLine(p.x,p.y-L, p.x,p.y+L );
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x/2,p.y-L+box.y/2);
      }
   }

   // Tracage d'un r�ticule (ne sert que pour SourcePhot)
   void drawReticule(Graphics g,Point p) {
      int L = getL();
      int m=4;
      g.drawLine(p.x-L,p.y, p.x-m,p.y ); g.drawLine(p.x+m,p.y, p.x+L,p.y );
      g.drawLine(p.x,p.y-L, p.x,p.y-m ); g.drawLine(p.x,p.y+m, p.x,p.y+L );
      g.drawLine(p.x,p.y, p.x,p.y );
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L-box.x/2,p.y-L+box.y/2);
      }
   }

   // Tracage d'une croix (45 degres)
   void drawCroix(Graphics g,Point p) {
      int L = getL();
      g.drawLine(p.x-L,p.y-L, p.x+L,p.y+L );
      g.drawLine(p.x-L,p.y+L, p.x+L,p.y-L );
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+L,p.y+box.y/2);
      }
   }

   // Tracage d'un point
   void drawPoint(Graphics g,Point p) {
      if( plan!=null && plan.getScalingFactor()>2 ) { drawCircleS(g,p); return; }
      g.drawLine(p.x-1,p.y, p.x+1,p.y );
      g.drawLine(p.x,p.y-1, p.x,p.y+1 );
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+2-box.x/2,p.y-2+box.y/2);
      }
   }

   // Tracage d'un dot
   void drawDot(Graphics g,Point p) {
      if( plan!=null && plan.getScalingFactor()>1 ) { drawPoint(g,p); return; }
      g.drawLine(p.x,p.y, p.x,p.y );
      if( isWithLabel() ) {
         setBox(g);
         g.drawString(id,p.x+2-box.x/2,p.y-2+box.y/2);
      }
   }

  /** Dessine la source pour la legende
   * @param g   le contexte graphique
   * param x,y	position
   */
   protected void print(Graphics g,int x, int y) {
      Point p = new Point(x,y);

      // Memorisation des parametres
      boolean mwithlabel = isWithLabel();
      setWithLabel(false);

      g.setColor( plan.c );
      switch(sourceType) {
         case SQUARE:  drawCarre(g,p);   break;
         case CROSS:   drawCroix(g,p);   break;
         case PLUS:    drawPlus(g,p);    break;
         case RHOMB:   drawLosange(g,p); break;
         case POINT:   drawPoint(g,p);   break;
         case DOT:     drawDot(g,p);     break;
      }

      setWithLabel(mwithlabel);
   }

   /** retourne true si la source est selectionnee par l'un des filtres actif */
   protected boolean isSelectedInFilter() {
      PlanFilter pf;
      for(int i=0;i<PlanFilter.allFilters.length;i++) {
         pf = PlanFilter.allFilters[i];
         if(pf.isOn() && isSelected!=null && isSelected[pf.numero]) {
            return true;
         }
      }
      return false;
   }

   /** retourne true si la source n'est sous l'influence d'aucun filtre (ie pas concernee ou concernee mais filtre off) */
   protected boolean noFilterInfluence() {
      PlanFilter pf;
      for( int i=0;i<PlanFilter.allFilters.length;i++) {
         pf = PlanFilter.allFilters[i];
         if( !pf.isOn() ) {
            continue;
         }
         if( pf.numero<plan.influence.length && plan.influence[pf.numero]) return false;
      }
      return true;
   }

   /** retourne le nombre de filtres ON et flagOk qui influencent la source */
   private int nbFiltersOk() {
      int nb=0;
      PlanFilter pf;
      for( int i=0;i<PlanFilter.allFilters.length;i++) {
         pf = PlanFilter.allFilters[i];
         if( !pf.isOn() || !pf.flagOk ) continue;
         if(plan.influence[pf.numero]) nb++;
      }

      return nb;
   }

   /** Ecriture d'info ASCII permettant de construire des links html
    * pour une carte cliquable */
   protected void writeLinkFlex(OutputStream o,ViewSimple v) throws Exception {
      int L =getL();
      PointD p = getViewCoordDouble(v,L,L);
      if( p==null ) return;  // hors champ
      o.write((plan.label+"\t"+(id!=null?id:"-")+"\t"+p.x+"\t"+p.y+"\t"+getFirstLink()+"\n").getBytes());
   }

   /** Ecriture d'info ASCII permettant de construire des links html
    * pour une carte cliquable */
   protected void writeLink(OutputStream o,ViewSimple v) throws Exception {
      int L =getL();
      Point p = getViewCoord(v,L,L);
      if( p==null ) return;  // hors champ
      o.write((plan.label+"\t"+(id!=null?id:"-")+"\t"+p.x+"\t"+p.y+"\t"+getFirstLink()+"\n").getBytes());
   }

   /** Retourne le premier link associ�e � la source si il existe, sinon retourne "-" */
   protected String getFirstLink() {
      if( getLeg()==null ) return "-";
      int i = getLeg().getFirstLink();
      if( i<0 ) return "-";
      String s;
      try {
         s = getCodedValue(i);
         int pipe = s.indexOf('|');
         int blanc = s.indexOf(' ');
         int sup = s.lastIndexOf('>');
         String id = s.substring(2,blanc>0?blanc:pipe>0?pipe:sup);
         String param = blanc>0?s.substring(blanc+1,pipe>0?pipe:sup):"";

         boolean flagEncode = id.equals("Http");
         URL url = plan.aladin.glu.getURL(id,param,flagEncode,false);  // Resolution GLU
         return url.toString();
      }
      catch( Exception e ) { return "-"; }
   }
   

  /** Dessine la source
   * @param g        le contexte graphique
   * @param v reference � la vue o� on dessine
   */
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) {
      if( !inTime( v )  ) return false;
      
      if( plan.aladin.TESTANAGLYPH ) {
         if( drawAnaglyph(g, v) ) return true;
      }
      
      int L =getL();
      Point p = getViewCoord(v,L,L);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      if( plan.aladin.view.flagHighlight && !isHighlighted() ) return false;

      boolean iAmSelected = isSelectedInFilter();

      // si filtre==ON on n'affiche le rectangle vert encadrant la source
      // selectionnee que si la source a ete selectionnee
      boolean noInfluence = noFilterInfluence();
      if( isSelected() ) {
	  	//System.out.println("la source est selectionnee");
      	 if( noInfluence || iAmSelected ) {
            g.setColor( isTagged() ? Color.magenta : Color.green);
            g.drawRect(p.x-L-1,p.y-L-1, L*2+2, L*2+2);
         }
      }

      int nbFiltersOk = nbFiltersOk();

      // if the source is in the filter selection, we proceed the action associated with the active filter
      if( !noInfluence && iAmSelected ) {
      	  drawAssociatedFootprint(g,v,dx,dy);
      	  // si aucun des plans dont la source subit l'influence n'est pret
      	  // on dessine la source comme d'habitude
      	  if( nbFiltersOk == 0 ) doDraw(g, p, plan.c);
      	  
      	  // on applique les differentes actions associees aux differents filtres
      	  else {
      	     boolean success = drawWithFilter(g, v, p, dx, dy);
      	     if ( ! success ) return false;
      	  }

      // pour les sources qui ne sont sous l'influence d'aucun filtre
      } else if( noInfluence ) {
         doDraw(g, p, plan.c);
         drawAssociatedFootprint(g,v,dx,dy);
         
      } else if(!iAmSelected && nbFiltersOk==0) {
         doDraw(g, p, plan.c);
         drawAssociatedFootprint(g,v,dx,dy);
      }
      return true;
   }

    private boolean drawWithFilter(Graphics g, ViewSimple v, Point p, int dx, int dy) {
        PlanFilter pf;
        // boucle sur les filtres
        for (int i = 0; i < PlanFilter.allFilters.length; i++) {
            pf = PlanFilter.allFilters[i];

            if (pf.isOn() && plan.influence[pf.numero] && isSelected[pf.numero]
                    && pf.flagOk) {
                // boucle sur les actions
                if (actions[pf.numero] == null) {
                    return false;
                }
                for (int j = 0; j < actions[pf.numero].length; j++) {
                    this.actions[pf.numero][j].action(this, g, v, p, pf.numero, j, dx, dy);
                }
            }
        }
        return true;
    }

    private void drawAssociatedFootprint(Graphics g, ViewSimple v, int dx, int dy) {
        // dessin du FoV �ventuellement associ� � la source
        if (sourceFootprint != null) {
            sourceFootprint.draw(v.getProj(), g, v, dx, dy, getColor());
        }
    }


    static final int MAXR=200;
    
    
    private double getAnaglyphRadius() {
       int i = plan.getIndexAnaglyphMag();
       if( i==-1 ) return 0;
       String s = getValue(i);
       if( s.length()==0 ) return 0;
       double val = Double.parseDouble(s);
       return 25-val;                     // Entre 22 et 0
    }
    
    private double getAnaglyphDistance() {
       int i = plan.getIndexAnaglyphParallax();
       if( i==-1 ) return 0;
       String s = getValue(i);
       if( s.length()==0 ) return 0;
       double val = Double.parseDouble(s);
       return val+2;                    // entre 300 et 0
    }
    
    private void drawStar(Graphics g, int col, int lig, int ik ) {
       int cx=0,cy=0;
       Graphics2D g2d = (Graphics2D)g;
       Composite saveComposite = g2d.getComposite();
       try {
          double k[][] = plan.aladin.kernelList.getKernel(ik);
          int m = k[0].length;
          for( int lk = 0; lk < m; lk++ ) {
             for( int ck = 0; ck < m; ck++ ) {
                float opacityLevel = (float) (k[lk][ck]) * plan.getOpacityLevel();
                Composite myComposite = Util.getImageComposite(opacityLevel);
                g2d.setComposite(myComposite);
                int x = cx + (col - m / 2 + ck);
                int y = cy + (lig - m / 2 + lk);
                g2d.drawLine(x,y,x,y);
             }
          }
       } finally {
          g2d.setComposite(saveComposite);
       }

    }

    private boolean drawAnaglyph(Graphics g, ViewSimple v) {
       try {
          double sizeView = v.getTaille();   // largeur du champ en degres
          
          Point p = getViewCoord(v,MAXR,MAXR);
          if( p==null || p.x<0 ) return false;
          
          double radius = getAnaglyphRadius();
          if( radius==0 ) throw new Exception("null radius");
          double distance = getAnaglyphDistance();
                
          int r= (int) radius/4; if( r<2 ) r=2;
          r= (int)radius*4;
          int k = r<10?1:r<20?2:r<30?3:r<40?4:r<50?5:6;
          int offset = (int) ( (distance/2)*plan.getScalingFactor() );
          
          g.setColor( Color.cyan );
//          g.fillOval(p.x-r-offset, p.y-r, r*2, r*2);
          drawStar(g,p.x-offset, p.y, k); 
          
          g.setColor( Color.red );
//          g.fillOval(p.x-r+offset, p.y-r, r*2, r*2);
          drawStar(g,p.x+offset, p.y, k); 
         
          return true;
      } catch( Exception e ) {
//         e.printStackTrace();
      }
       return true;
    }

   /** Dessine la source en inversant sa couleur (ne concerne que les surcharges dues aux filtres)
    * @param g        le contexte graphique
    * @param v        r�f�rence � la vue sur laquelle on doit dessiner
    */
   /*
    protected void drawReverse(Graphics g,ViewSimple v,int dx,int dy) {
       //System.out.println("On repaint");
       Point p = getViewCoord(v,L,L);
       if( p.x<0 ) return;
       p.x+=dx; p.y+=dy;

       boolean iAmSelected = isSelected();

       boolean noInfluence = noFilterInfluence();

       int nbFiltersOk = nbFiltersOk();

       // if the source is in the filter selection, we proceed the action associated with the active filter
       if( !noInfluence && iAmSelected ) {

           if( nbFiltersOk == 0 ) {
           }
           // on applique les differentes actions associees aux differents filtres
           else {
              PlanFilter pf;
              // boucle sur les filtres
              for(int i=0;i<PlanFilter.allFilters.length;i++) {
                 pf = PlanFilter.allFilters[i];

                 if( pf.isOn() && plan.influence[pf.numero] && isSelected[pf.numero] && pf.flagOk ) {
                    // boucle sur les actions
                    if( actions[pf.numero]==null ) return;
 		           for(int j=0;j<actions[pf.numero].length;j++) {
       	      	      this.actions[pf.numero][j].drawReverse(this,g,v,p,pf.numero,j,false);
 		           }
                 }
              }
           }
       }

       // pour les sources qui ne sont sous l'influence d'aucun filtre
       else if(noInfluence) {
       }
       else if(!iAmSelected && nbFiltersOk==0) {
       }
    }
    */

   /** method that actually draws the source */
   protected void doDraw(Graphics g, Point p, Color c) {

    	if( c==null ) g.setColor(plan.c);
		else g.setColor( c );
      	switch(sourceType) {
           case SOLIDOVAL:    drawOval(g,p,true);    break;
           case OVAL:         drawOval(g,p);         break;
           case SOLIDSQUARE:  drawCarre(g,p,true);   break;
           case SQUARE:       drawCarre(g,p);        break;
           case CROSS:        drawCroix(g,p);        break;
           case PLUS:         drawPlus(g,p);         break;
           case SOLIDRHOMB:   drawLosange(g,p,true); break;
           case RHOMB:        drawLosange(g,p);      break;
           case SOLIDTRIANGLE:drawTriangle(g,p,true);     break;
           case TRIANGLE:     drawTriangle(g,p);break;
           case CIRCLES:      drawCircleS(g,p);      break;
           case SOLIDCIRCLE:  drawCircle(g,p,true);  break;
           case CIRCLE:       drawCircle(g,p);       break;
           case POINT:        drawPoint(g,p);        break;
           case DOT:          drawDot(g,p);          break;
           
           case RETICULE:     drawReticule(g, p);    break;
      	}
   }

   ////////////////////////////////////////////////////////
   // thomas (below this point are thomas modifications)///
   ////////////////////////////////////////////////////////


   /**
    * @param mask mask used for the comparison
    * @param word string to compare
    * @param wildcard if true, wildcards ('*' and '?') are taken into account
    * @return true if word matches the mask
    * @see #findUCD(String)
    * @see #findColumn(String)
    */
   private boolean match(String mask, String word, boolean wildcard) {
      if( wildcard ) return Util.matchMask(mask, word);
      else return word.equals(mask);
   }

   /** findUCD returns the position of ucd in the leg.fields array
    *  @param         ucd - the ucd we are looking for. May contain '*' or '?'
    *  wildcard  ; in this case, returns the position of the 1st match
    *  Use "\*" to search character '*'
    *  @return the position of ucd in leg.fields array, -1 if not found
	*  if the last character of ucd is a star "*", it returns the position of the first column with prefix equals to the string before the star
    */
   protected int findUCD(String ucd) {
    	int curPos;
    	String curUCD = null;

        if( getLeg()==null ) return -1;

        Field[] fields = getLeg().field;
    	// ucd contient-elle des wildcards ?
    	boolean wildcard = useWildcard(ucd);

		//	thomas (AVO 2005)
		ucd = MetaDataTree.replace(ucd, "\\*", "*", -1);

		ucd = ucd.toUpperCase();

    	for(curPos=0; curPos<fields.length; curPos++) {
    	    curUCD = fields[curPos].ucd;
    	    if( curUCD!=null ) curUCD = curUCD.toUpperCase();
    	    String myVal;
			// pour eviter les cas ou l'on prend en compte un UCD avec une valeur vide alors qu il en existerait un "bon"
			if( curUCD == null || ( wildcard && ( (myVal=this.getValue(curPos))==null || myVal.trim().length()==0 ) ) ) continue;
			if( match(ucd, curUCD, wildcard) ) return curPos;
    	}
    	return -1;
   }

   protected int findUtype(String utype) {
       int curPos;
       String curUtype = null;
       
       if( getLeg()==null ) return -1;

       Field[] fields = getLeg().field;
       // utype contient-il des wildcards ?
       boolean wildcard = useWildcard(utype);

       utype = MetaDataTree.replace(utype, "\\*", "*", -1);

       utype = utype.toUpperCase();

       for(curPos=0; curPos<fields.length; curPos++) {
           curUtype = fields[curPos].utype;
           if( curUtype!=null ) curUtype = curUtype.toUpperCase();
           String myVal;
           // pour eviter les cas ou l'on prend en compte un utype avec une valeur vide alors qu il en existerait un "bon"
           if( curUtype == null || ( wildcard && ( (myVal=this.getValue(curPos))==null || myVal.trim().length()==0 ) ) ) continue;
           if( match(utype, curUtype, wildcard) ) return curPos;
       }
       return -1;
  }

/*
   protected int findUtype(String utype) {
       int curPos;
       String curUtype = null;

       Field[] fields = leg.field;
       // utype contient-elle des wildcards ?
       boolean wildcard = useWildcard(utype);

       utype = MetaDataTree.replace(utype, "\\*", "*", -1);

       utype = utype.toUpperCase();

       for(curPos=0; curPos<fields.length; curPos++) {
           curUtype = fields[curPos].unit;
           if( curUtype!=null ) curUtype = curUtype.toUpperCase();
           String myVal;
           // pour eviter les cas ou l'on prend en compte un utype avec une valeur vide alors qu il en existerait un "bon"
           if( curUtype == null || ( wildcard && ( (myVal=this.getValue(curPos))==null || myVal.trim().length()==0 ) ) ) continue;
           if( match(utype, curUtype, wildcard) ) return curPos;
       }
       return -1;
  }
*/

   /** findColumn returns the position of this column name in the leg.fields array
    *  @param name - the column name we are looking for. May contain '*'
    *  or '?' wildcard ; in such a case, returns the position of the 1st match
    *  Use "\*" to search character '*'
    *  @return the position of name in leg.fields array, -1 if not found
    */
   protected int findColumn(String name) {
    	int curPos;
    	String curName = null;

        if( getLeg()==null ) return -1;

        // replace ajout� pour la d�mo AVO
        name = MetaDataTree.replace(name, " ", "", -1);

    	Field[] fields = getLeg().field;
    	// name contient-elle des wildcards ?
		boolean wildcard = useWildcard(name);

		//	thomas (AVO 2005)
		name = MetaDataTree.replace(name, "\\*", "*", -1);

    	for(curPos=0; curPos<fields.length; curPos++) {
            // replace ajout� pour la d�mo
    	    curName = MetaDataTree.replace(fields[curPos].name.trim(), " ", "", -1);
    	    if(curName == null) continue;
			if( match(name, curName, wildcard) ) return curPos;
    	}
    	return -1;
   }

   /** Retourne true si s contient '?' ou ('*' non pr�c�d� de '\')
    *
    * @param s la chaine test�e
    * @return boolean
    */
   static protected boolean useWildcard(String s) {
      char curChar,oldChar;
      oldChar=' ';

      int n = s.length();
      for( int i=0; i<n; i++ ) {
         curChar = s.charAt(i);
         if( curChar=='?' ) return true;
         if( curChar=='*' && oldChar!='\\' ) return true;
         oldChar = curChar;
      }
      return false;

   }


    /** Returns the catalog name for the source */
    protected String getCatalogue() {
        if( info!=null) {
            int tab = info.indexOf("\t");
            if(tab<0) return null;
            String name = info.substring(0,tab);
            // on vire les marques GLU
            if( name.startsWith("<&") ) {
              int a = name.indexOf('|');
              if( a>0 ) {
                 int b = name.indexOf('>',a+1);
                 if( b>=0 ) name=name.substring(a+1,b);
              }
           }
           return name;
        }
        else return null;
    }

    /** Retourne la valeur du champ � la position index (avec un �ventuel
     * tag GLU pour les liens
     */
    protected String getCodedValue(int index) throws NoSuchElementException {
       index++;   // skip du triangle  (il y a toujours une premi�re valeur)
       int deb= -1;
       int n=info.length();
       int i=0;
       for( int j=0; i<n && j<index+1; i++) {
          if( info.charAt(i)=='\t') {
             j++;
             if( j==index && deb==-1 ) deb=i+1;
          }
       }
       if( deb==-1 ) throw new NoSuchElementException();
       
       // BUG FIX 1/1/2020 PF
       if( i<n ) i--;
       
       return info.substring(deb,i);
    }

    // VERSION ORIGINALE BIEN PLUS LENTE
//    protected String getCodedValue(int index) throws NoSuchElementException {
//       StringTokenizer st = new StringTokenizer(this.info,"\t");
//       st.nextElement();     // skip the triangle
//       for(int i=0;i<index;i++) st.nextElement();
//       return st.nextElement().toString();
//    }


   /** Returns the value of the field at position index
    *	@param index - the position of the field one wants
    *	@return the value of the field at position index, <b>null</b> if not found
    */
    protected String getValue(int index) {
    	String ret;
      	try {
      	   ret = getCodedValue(index);
      	   if( getLeg().isNullValue(ret, index) ) ret="";

           // Pierre: En cas de marques GLU
           if( ret.startsWith("<&") ) {
              int a = ret.indexOf('|');
              if( a>0 ) {
                 int b = ret.indexOf('>',a+1);
                 if( b>=0 ) ret=ret.substring(a+1,b);
              }
           }
      	} catch(NoSuchElementException e) {return null;}

      	return ret.trim();
    }

    /** Retourne le type d'objet */
    public String getObjType() { return "Source"; }

    /** Retourne un tableau de chaines contenant les valeurs de chaque champ */
    public String [] getValues() {
       StringTokenizer st = new StringTokenizer(info,"\t");

       // Si on connait le nombre de champ, on alloue imm�diatement le tableau
       // sinon on passe par un Vector temporaire et on recopie � la fin
       String [] v = null;
       Vector tmp=null;
       if( getLeg().field.length>0 ) v = new String[ getLeg().field.length ];
       else tmp = new Vector();

       st.nextElement();  // Skip le triangle
       boolean encore;
       for( int i=0; (encore=st.hasMoreTokens()) ||  (v!=null && i<v.length) ; i++ ) {
         String ret = encore?st.nextToken():"";
          // Pierre: En cas de marques GLU
          if( ret.startsWith("<&") ) {
             int a = ret.indexOf('|');
             if( a>0 ) {
                int b = ret.indexOf('>',a+1);
                if( b>=0 ) ret=ret.substring(a+1,b);
             }
          }
          if( v!=null ) v[i]=ret;
          else tmp.add(ret);
       }

       // Recopie n�cessaire ?
       if( v==null ) {
          v = new String[ tmp.size() ];
          Enumeration e=tmp.elements();
          for( int i=0; i<v.length; i++) v[i] = (String)e.nextElement();
       }

       return v;
    }

    /** Modify the value of the specifical column
     * @param index column index eventually greater than the current dimension
     * @param value new value
     */
    public boolean setValue(int index,String value) {
       StringTokenizer st = new StringTokenizer(info,"\t");
       StringBuilder nInfo=null;
       boolean encore;
       index++;
       for( int i=0; (encore=st.hasMoreTokens()) || i<=index; i++ ) {
          String s = encore ? st.nextToken() : "";
          if( i==index ) s=value;
          if( i==0 ) nInfo = new StringBuilder(s);
          else nInfo.append("\t"+s);
       }
       info = nInfo.toString();

       return true;
    }

    /** Set the drawing shape
     * @param sourceType Obj.OVAL, SQUARE, CIRCLE, RHOMB, PLUS, CROSS, TRIANGLE, CIRCLES, POINT, DOT
     */
     public void setShape(int shape) { setSourceType(shape); }
     protected void setSourceType(int sourceType) { this.sourceType = (byte)sourceType; }

     /** Highlight or unhighlight the source */
     public void setHighlighted(boolean flag) { plan.aladin.view.setHighlighted(this,flag); }

    /**
     * Set metadata for a specifical column (name, unit, ucd, display width).
     * null or <0 values are not modified.
     * If the index is greater than the number of columns, the additionnal columns
     * are automatically created and all other sources using the same legende
     * will be size fixed.
     * @param index number of column (0 is the first one)
     * @param name new name or null for no modification
     * @param datatype new datatype or null for no modification
     * @param unit new unit or null
     * @param ucd new ucd or null
     * @param width new width or -1. 0 to use the default display width.
     */
    public void setColumn(int index, String name,String unit,String ucd,int width) {
       setColumn(index,name,null,unit,ucd,width);
    }
    public void setColumn(int index, String name,String datatype,String unit,String ucd,int width) {
       if( leg==null ) leg=new Legende();
       int newCol = getLeg().setField(index,name,datatype,unit,ucd,width);
       if( newCol>0 && plan!=null && plan.pcat!=null ) plan.pcat.fixInfo(getLeg());
    }

    /** Return the index of a column (Source object). Proceed in 2 steps,
     * Look into the column name, if there is no match, look into the ucd.
     * If nothing match, return -1.
     * The string key can use wilcards (* and ?).
     * @param key name or ucd to find
     * @return index of first column matching the key
     */
    public int indexOf(String key) {
       if( getLeg()==null ) return -1;
       for( int i=0; i<getLeg().field.length; i++ ) {
          if( Util.matchMask(key,getLeg().field[i].name) ) return i;
       }
       for( int i=0; i<getLeg().field.length; i++ ) {
          if( Util.matchMask(key,getLeg().field[i].ucd) ) return i;
       }
       return -1;
    }

    /** Return the number of columns associated to this object */
    public int getSize() {
       if( getLeg()!=null ) return getLeg().getSize();
       if( info==null ) return 0;
       return new StringTokenizer(info,"\t").countTokens();
    }
    
    /** Fournit un enregistrement nom = valeur unit description pour tous les champs */
    public String getRecord() {
       StringBuilder rep = new StringBuilder();
       String [] names = getNames();
       String [] values = getValues();
       String [] desc = getDescriptions();
       String [] units = getUnits();
       
       for( int i=0; i<names.length; i++ ) {
          if( values[i].trim().length()==0 ) continue;
          String d = desc[i]== null || desc[i].length()==0 ? "" : 
             desc[i].length()>40 ? " / "+desc[i].substring(0,38)+"...": " / "+desc[i];
          String u = units[i]==null || units[i].length()==0 ? "" : " "+units[i];
          rep.append(Util.align(names[i], 15)+"= "+ Util.align(values[i]+u,20)+d+"\n");
       }
       return rep.toString();
    }

    /** Retourne la liste des noms de chaque valeur */
    public String [] getNames() { return getMeta(0); }

    /** Retourne la liste des unit�s de chaque valeur */
    public String [] getUnits() { return getMeta(1); }

    /** Retourne la liste des UCDs pour chaque valeur */
    public String [] getUCDs() { return getMeta(2); }

    /** Retourne la liste des Datatypes pour chaque valeur */
    public String [] getDataTypes() { return getMeta(3); }

    /** Retourne la liste des Arraysizes pour chaque valeur */
    public String [] getArraysizes() { return getMeta(4); }

    /** Retourne la liste des Widths pour chaque valeur */
    public String [] getWidths() { return getMeta(5); }

    /** Retourne la liste des Precisions pour chaque valeur */
    public String [] getPrecisions() { return getMeta(6); }

    /** Retourne la liste des nullValues pour chaque valeur */
    public String [] getNullValues() { return getMeta(7); }

    /** Retourne la liste des descriptions pour chaque valeur */
    public String [] getDescriptions() { return getMeta(8); }

    /** Retourne la liste d'une metadata particuli�re associ�e aux valeurs
     *  @param m 0:label, 1:unit,  2:ucd
     */
    private String [] getMeta(int m) {
       if( getLeg()==null ) return new String[0];
       String [] u = new String[getLeg().getSize()];
       for( int i=0; i<u.length; i++ ) {
          switch(m) {
             case 0: u[i]=getLeg().field[i].name; break;
             case 1: u[i]=getLeg().field[i].unit; break;
             case 2: u[i]=getLeg().field[i].ucd;  break;
             case 3: u[i]=getLeg().field[i].datatype;  break;
             case 4: u[i]=getLeg().field[i].arraysize;  break;
             case 5: u[i]=getLeg().field[i].width;  break;
             case 6: u[i]=getLeg().field[i].precision;  break;
             case 7: u[i]=getLeg().field[i].nullValue;  break;
             case 8: u[i]=getLeg().field[i].description;  break;
          }
       }
       return u;
    }

    /** Return XML meta information associated to this object
     * @return XML string, or null
     */
    public String getXMLMetaData() {
       return getLeg().getGroup();
    }

   /** Returns the unit for the field at position pos */
   protected String getUnit(int pos) {
    	if(pos<0) return "";
    	String u = getLeg().field[pos].unit;
    	if( u!=null ) u = u.replace("year","yr");   // pour faire plaisir � l'ESAC pour Gaia qui utilise des unit�s non conformes ni � l'IVOA, ni � l'UAI
    	return u;
   }

   /** VOTable just for this source */
   public InputStream getVOTable() throws Exception {
      return plan.aladin.writeObjectInVOTable(null, this, null, true, false, false,false).getInputStream();
   }
   
   /** Retourne le FoV extrait des colonnes SIA, null sinon
    * => cf TreeBuilder.createSIAPNode() de TB
    * @return une chaine STC-S 
    */
   public String createSIAFoV() {
      if( getLeg()==null ) return null;
      try {
         int iScale = getLeg().findUCD("VOX:Image_Scale");
         int iNaxis = getLeg().findUCD("VOX:Image_Naxes");
         int iSize  = getLeg().findUCD("VOX:Image_Naxis");
         int iCD    = getLeg().findUCD("VOX:WCS_CDMatrix");
         
         // Nombre de dimensions ?
         String val[] = getValues();
         if( iNaxis>=0 ) {
            int axis = Integer.parseInt( val[ iNaxis ]);
            if( axis!=2 ) return null;  // Pas une image
         }
         
         // Taille en pixels de l'image ?
         String s = val[ iSize ];
         Tok st = new Tok(s, ", ");
         int width = Integer.parseInt( st.nextToken() );
         int height = Integer.parseInt( st.nextToken() );
         
         // Taille angulaire du pixel ?
         s = val[ iScale ];
         st = new Tok(s, ", ");
         double scaleW = Double.parseDouble( st.nextToken() );
         double scaleH;
         try { scaleH = Double.parseDouble( st.nextToken() ); } 
         catch( Exception e1 ) { scaleH=scaleW; }  // une seule composante comme pour SDSS SIA
         
         // Et donc de l'image
         scaleW *= width;
         scaleH *= height;
         
         // D�termination de l'angle par la matrice CD ?
         double angle=0;
         if( iCD>=0 ) {
            try {
               st = new Tok(val[ iCD ], ", ");
               double cd01 = Double.parseDouble( st.nextToken() );
               st.nextToken();
               double cd11 = Double.parseDouble( st.nextToken() );
               angle = 90 - Math.atan2(cd01, cd11)*180.0/Math.PI;
               if( Double.isNaN(angle) ) angle=0;
            } catch( Exception e ) { angle=0; }
         }
         
         // Construction du FOV
         Fov fov = new Fov(raj,dej,scaleW,scaleH,angle);
         StringBuilder s1 = new StringBuilder("Polygon ICRS");
         for( PointD p : fov.getPoints() ) s1.append(" "+p.x+" "+p.y);
         
         return s1.toString();
         
      } catch( Exception e ) { return null; }
   }

  /**
    * Cr�e l'objet sourceFootprint s'il n'a pas d�ja �t� cr��
    *
    */
   private void createSourceFootprint() {
      if( sourceFootprint==null ) {
          sourceFootprint = new SourceFootprint();
      }
   }

   /** Retourne le footprint attach� � la source (peut �tre <i>null</i>) */
   protected SourceFootprint getFootprint() {
      return sourceFootprint;
   }
   
   /** Retourne le Service Descriptor attach� � la source (peut �tre <i>null</i>) */
   protected SimpleData getServiceDescriptor() {
      return sourceServiceDescriptor;
   }


   /** Attache un footprint donn� � la source */
   protected void setFootprint(PlanField footprint) {
   	  createSourceFootprint();
   	  sourceFootprint.setFootprint(footprint);
   }

   /** Attache un footprint donn� � la source */
   protected void setServiceDescriptor(SimpleData ServiceDescriptor) {
	  // System.out.println("value "+params.get("clef")) ;
   	 sourceServiceDescriptor = ServiceDescriptor ;
   }
   protected void setFootprint(String stcs) {
       createSourceFootprint();
       sourceFootprint.setStcs(this.raj, this.dej, stcs);
   }
   
   protected void resetFootprint() {
	   sourceFootprint = new SourceFootprint(); //just in case. because setting to null results in a null pointer elsewhere
	   //when the stc is "" then the table initialises sourceFootprint with "". So redoing the instantiation. 
	   int idxSTCS = findUtype(TreeBuilder.UTYPE_STCS_REGION1);
       if( idxSTCS<0 ) idxSTCS = findUtype(TreeBuilder.UTYPE_STCS_REGION2);
//       if( idxSTCS<0 ) idxSTCS = indexSTC;
       if (idxSTCS>=0) {
          try {
             setFootprint(getValue(idxSTCS));
             setIdxFootprint(idxSTCS);
          } catch(Exception e) {
             e.printStackTrace();
          }
       }
   }
   
	protected boolean isSetFootprint() {
		return sourceFootprint!=null && sourceFootprint.isSet();
	}

   /**
    * Switch the state (on/off) of the associated footprint
    *
    */
   protected void switchFootprint() {
      setShowFootprint(!sourceFootprint.showFootprint(),true);
   }

   /**
    * Shows/hides the footprint associated to a source
    * @param show
    */
   protected void setShowFootprint(boolean show,boolean withRepaint) {
   	  createSourceFootprint();
   	  sourceFootprint.setShowFootprint(show);
   	  if( withRepaint ) plan.aladin.calque.repaintAll();
   }
   
   protected void setShowFootprintTransient(boolean show,boolean withRepaint) {
      createSourceFootprint();
      sourceFootprint.setShowFootprintTransient(show);
      if( withRepaint ) plan.aladin.calque.repaintAll();
   }
   
   /** True is a footprint is associated to this source and displayed */
   protected boolean isShowingFootprint() {
      if( sourceFootprint==null ) return false;
      return sourceFootprint.showFootprint();
   }

   /**
    * @return Retourne l'index du footprint associ� (valeur par d�faut : -1)
    */
   protected int getIdxFootprint() {
      return sourceFootprint==null?-1:sourceFootprint.getIdxFootprint();
   }

   /**
    * @param idxFootprint valeur � donner � l'index du footprint associ�
    */
   public void setIdxFootprint(int idxFootprint) {
      createSourceFootprint();
      sourceFootprint.setIdxFootprint(idxFootprint);
   }

   // Variables m�morisant le mode de tri courant
   static private Source sortSource;    // La source �talon utilis� pour les comparaisons
   static private int sortNField;       // Le num�ro du champ concern�
   static private boolean sortNumeric;  // true si le tri est num�rique, alphab�tique sinon
   static private int sortSens;         // 1:ascendant, -1:descendant

   public int compare(Object a1, Object b1) {
      Source a = (Source)a1;
      Source b = (Source)b1;
      if( sortSource==null || a==null || b==null ) return 0;
      if( sortNField==-1 ) {
         if( a.isTagged()==b.isTagged() ) return 0;
         else return a.isTagged() ? -sortSens : sortSens;
      }

      // Il s'agit d'une source non concern�e, on met � la fin
      if( a.getLeg()!=sortSource.getLeg() ) return 1;
      if( b.getLeg()!=sortSource.getLeg() ) return -1;

      String aVal = a.getValue(sortNField);
      String bVal = b.getValue(sortNField);
      if( !sortNumeric ) {
         if( sortSens==1 ) return aVal.compareTo(bVal);
         else return bVal.compareTo(aVal);
      } else {
         double aNVal,bNVal;
         if( aVal.length()==0 ) aNVal=Double.MAX_VALUE;
         else aNVal = Double.valueOf(aVal).doubleValue();
         if( bVal.length()==0 ) bNVal=Double.MAX_VALUE;
         else bNVal = Double.valueOf(bVal).doubleValue();
         if( aNVal==bNVal ) return 0;
         return aNVal>bNVal ? sortSens : -sortSens;
      }
   }

   /** Retourne la source utilis�e pour effectuer les comparaisons */
   static protected Comparator getComparator() { return sortSource; }

   /**
    * Positionne les param�tres pour un tri ult�rieur.
    * Tri sur le champ d'indice nField toutes les sources de m�me l�gende que
    * celle pass�e en param�tre. On utilise un tri par tr�s performant mais
    * qui simplifie le traitement pour les enregistrements non concern�s.
    * @param s la source de r�f�rence
    * @param nField l'indice du champ cl� de tri
    * @param sens 1 - ascendant, -1 descendant
    */
   static protected void setSort(Source s, int nField, int sens) {
      sortNumeric = s.getLeg().isNumField(nField);
      sortSource  = s;
      sortNField  = nField;
      sortSens    = sens;
      Aladin.trace(1,"Measurement "+(sortNumeric?"numerical ":"alphanumerical ")
            +(sens==1?"ascending":"descending")+" sort on "
            +(s.getLeg()==null||nField==-1?"field "+nField:"${"+s.getLeg().field[nField].name)+"}");

   }

   /** Retourne vrai si l'objet doit �tre consid�r� comme une Source, et par cons�quent repris
    * dans la tables des mesures, dans les VOTables export�s, etc...
    */
   protected boolean asSource() { return true; }

}
