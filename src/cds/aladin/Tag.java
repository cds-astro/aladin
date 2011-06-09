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
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import cds.tools.Util;

/**
 * Objet graphique Tag/Label affichable dans la vue
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : (nov 2010) Refonte complète, changement de nom (Texte -> Tag)
 * @version 1.1 : (11 mai 99) Correctoin du bug de la font
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Tag extends Position {

   static private final int MINDIST=3, MAXDIST=300;        // Distances min - max
   static private final int MINFONT=7, MAXFONT=60;          // Taille de la fonte min - max

   // Types de réticules, et tailles associées
   static public final int RETICLE    = 0;
   static public final int BIGRETICLE = 1;
   static public final int SMALLCIRCLE= 2;
   static public final int CIRCLE     = 3;
   static public final int BIGCIRCLE  = 4;
   static public final int ARROW      = 5;
   static public final int BIGARROW   = 6;
   static public final int NOPOLE     = 7;
   
   static private final String [] TAGS = { "reticle","bigreticle","smallcircle","circle","bigcircle","arrow","bigarrow","nopole" };
   static private final int [] TAGSIZE = { 5,        8,            3,             5,       8,            2,      2,         0 };
   
   // Position courante de la souris (voir variable "on")
   final private int NOTHING = 0;
   final private int LABEL   = 1;
   final private int TAG     = 2;
   final private int POIGNEE = 3;
   final private int CORNER  = 4;
   
   private int L = 5;                 // Demi-taille du réticule
   private int tag = RETICLE;         // Type de tag
   private double angle=Math.PI/4;    // Angle de la hampe (sens positive, Y vers le bas)
   private double dist=40;            // Taille de la hampe sans l'accroche
   private int accroche = 20;         // Taille de l'accroche de la hampe (petit trait juste avant le label)
   private Color couleur=null;        // Couleur alternative
   private float fond=0f;             // Niveau de transparence du fond (0, pour aucun)
   private int bord=0;                // Type de bord
   private Font F=Aladin.BOLD;        // Font du texte
   private boolean editing;           // Vrai si le texte est en cours d'edition
   private int on = NOTHING;          // Indique on se trouvait la souris dans le cas d'une modif via la souris
   private double distAngulaireOrig = 0; // Taille angulaire d'origine du tag (pour ne pas afficher le label ni la hampe si devient trop petit)
   
   private Rectangle rect1=null;      // rectangle recouvrant le réticule (le centre du tag est en 0,0)
   private Rectangle rect2=null;      // rectangle recouvrant le label + la poignée de contrôle (le centre du tag est en 0,0)
   
   private Tag() {}
   
  /** Creation du tag. */
   protected Tag(Plan plan,ViewSimple v, double x, double y) {
     super(plan,v,x,y,0,0,XY|RADE_COMPUTE,"");
   }

  /** Creation du tag. */
   protected Tag(Plan plan,ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,0,0,XY|RADE_COMPUTE,id);
      setText(id);
      setWH();
   }

  /** Creation d'un tag dont la position est donnee en coord RA/DEC */
   protected Tag(Plan plan,Coord c,String id) {
      super(plan,null,0,0,c.al,c.del,RADE,id);
      setText(id);
      setWH();
   }

  /** Creation d'un tag pour les backups */
   protected Tag(Plan plan) { super(plan); }
   
   /** Fournit une copie du Tag */
   protected Tag copy() {
      Tag t = new Tag();
      t.L=L;
      t.tag=tag;
      t.angle=angle;
      t.dist=dist;
      t.accroche=accroche;
      t.couleur=couleur;
      t.fond=fond;
      t.F=F;
      t.editing = editing;
      t.on = on;
      t.distAngulaireOrig=distAngulaireOrig;
      return t;
   }

   /** Retourne le type d'objet */
   static private final String C= "|";
   
   /** Retourne une chaine contenant toutes les informations techniques à sauvegarder dans un fichier AJ
    * afin de pouvoir regénérer le tag */
   protected String getSpecificAJInfo() {
      return (!hasLabel()?"":id.replace("\n","\\n"))+C
            +TAGS[tag]+C +L+C +angle+C +dist+C +fond+C +bord+C +getFont().getSize()+C +distAngulaireOrig;
   }
   
   /** Traite une chaine contenant toutes les informations techniques issues d'un fichier AJ */
   protected void setSpecificAJInfo(String s) {
      try {
         // Pour le premier champ, on n'utilise pas Tok, car il peut contenir des " et de '
         int offset = s.indexOf(C);
         String s1 = s.substring(0,offset);
         setText(s1);
//         id = s1.length()==0 ? null : s1.replace("\\n","\n");
         
         Tok tok = new Tok(s.substring(offset+C.length()),C);
         tag = Util.indexInArrayOf(tok.nextToken(), TAGS, true);
         if( tag==-1 ) tag=0;
         L = Integer.parseInt(tok.nextToken());
         angle = Double.parseDouble(tok.nextToken());
         dist = Double.parseDouble(tok.nextToken());
         fond = Float.parseFloat(tok.nextToken());
         bord = Integer.parseInt(tok.nextToken());
         float size = Float.parseFloat(tok.nextToken()); setFont(Aladin.BOLD.deriveFont(size));
         distAngulaireOrig = Double.parseDouble(tok.nextToken());
      } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      setWH();
   }
   
   public String getObjType() { return "Tag"; }
   
   /** Positionnement de la distance (en pixels) du tag au label */
   protected void setDist(int dist) { this.dist=dist; setWH();}

   /** Positionnement de l'angle (en degrés, sens trigo) du tag au label */
   protected void setAngle(int angle) { this.angle=Math.toRadians(270-angle); setWH();}
   
   /** Positionnement du type de hampe (reticle,big reticle,smallcircle,circle,bigcircle,arrow,bigarrow,nopole) */
   protected void setPole(String pole) {
      tag = Util.indexInArrayOf(pole, TAGS, true);
      if( tag==-1 ) tag=0;
      L = TAGSIZE[tag];
      setWH();
   }
   
   /** Positionnement de la taille du label (en pixels) */
   protected void setFontSize(int size) { setFont( getFont().deriveFont((float)size)); setWH(); }
   
   /** Modification de l'identificateur.
   * Cela revient a modifier le texte
   * @param id le nouveau texte
   */
   protected void setText(String id) {
      this.id= id==null || id.length()==0 ? null : id.replace("\\n","\n");
      setWH();
   }

  /** Positionne le flag d'édition en cours
   * Cela signifie que le texte est en cours d'edition et qu'il faut
   * afficher le ``caret'' a la fin */
   protected void setEditing(boolean flag) {
      editing = flag;
      setSelected(flag);
      if( editing ) {
         plan.aladin.view.startTimer(500);
      }
   }
   
   /** mémorise une taille angulaire du tag afin de ne pas afficher son label sur des champs trop petits */
   protected void setDistAngulaireOrig(ViewSimple v) {
      Projection proj = v.getProj();
      if( !Projection.isOk(proj) ) { distAngulaireOrig=0; return; }
      Coord c = new Coord();
      c.y = dist; c.x = 0;
      proj.getCoord(c);
      distAngulaireOrig = Math.abs(c.del * v.zoom);
   }
   
   /** Le label est-il trop petit pour ce champ ? */
   private boolean isTooSmallForLabel(ViewSimple v) {
      if( distAngulaireOrig==0 || v.getProj()==null ) return false;
      Coord c = new Coord();
      c.y = dist; c.x = 0;
      v.getProj().getCoord(c);
      double nDistAngulaire = Math.abs(c.del * v.zoom);
      return nDistAngulaire<distAngulaireOrig/4;
   }
   
   /** Retourne true si on est en train d'éditer le tag */
   protected boolean isEditing() { return editing; }

   /** Precalcul la taille du tag */
   void setWH() {
      rect2=null;
      rect1 = new Rectangle(-L,-L,2*L,2*L);
      if( hasLabel() ) {
         Point p = getXYLabel();
         Dimension d = getDimLabel();
         rect2 = new Rectangle(p.x-d.width/2, p.y-d.height/2, d.width, d.height);
         rect2.add( getXYPoignee() );
      } else if( isArrow() ) rect1.add( getXYPoignee() );
   }
   
   // Retourne true s'il s'agit d'une fleche
   private boolean isArrow() { return tag==ARROW || tag==BIGARROW; }
   
// Retourne true s'il s'agit d'un reticule
   private boolean isReticle() { return tag==BIGRETICLE || tag==RETICLE; };
   
   /** Retourne true si le tag a un label */
   protected boolean hasLabel() { return id!=null && id.trim().length()>0; }

  /**  Retourne vrai si le point (x,y) de l'image se trouve sur le tag */
   protected boolean inside(ViewSimple v,double x, double y) {
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      return rect1.contains(x, y) || !isTooSmallForLabel(v) && rect2!=null && rect2.contains(x,y);
   }
   
   /** Retourne true si x,y (coordonnées image) se trouve sur la poignée
    * de contrôle de la taille et de la direction de la hampe */
   protected boolean onPoignee(ViewSimple v,double x, double y) {
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      Point p = getXYPoignee();
      return Math.abs(p.x-x)<4 && Math.abs(p.y-y)<4;
   }
   
   /** Retourne true si x,y (coordonnées image) se trouve sur le label */
   protected boolean onLabel(ViewSimple v,double x, double y) {
      if( isTooSmallForLabel(v) || rect2==null ) return false;
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      return rect2.contains(x,y);
   }
   
   /** Retourne true si x,y (coordonnées image) se trouve sur le tag */
   protected boolean onTag(ViewSimple v,double x, double y) {
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      return rect1.contains(x,y);
   }
   
   /** Retourne true si x,y (coordonnées image) se trouve sur le coin */
   protected boolean onCorner(ViewSimple v,double x, double y) {
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      Point p = getXYCorner();
      return Math.abs(p.x-x)<4 && Math.abs(p.y-y)<4;
   }
   
   /** Retourne true si x,y (coordonnées image) se trouve sur un élément
    * qui peut être modifiable par la molette de la souris. Si c'est le cas, mémorise
    * l'élément en question dans "on", et retourne true */
   protected boolean onViaWheel(ViewSimple v,double x, double y) {
      if( onTag(v,x,y ) ) on = TAG;
      else if( onLabel(v,x,y) )on = LABEL;
      else on = NOTHING;
      return on!=NOTHING;
   }
   
   /** Reset de la variable indiquant où se trouve la souris */
   protected void resetOn() { on=NOTHING; }
   
   /** Modification d'un élément du tag via la molette de la souris. L'élément
    * en question aura été mémorisé auparavant par onWheel(...) */
   protected void modifyViaWheel(int sens) {
      if( on==TAG ) modifyTag(sens);
      else if( on==LABEL ) modifyFond(sens);
   }
      
   /** Retourne true si x,y (coordonnées image) se trouve sur un élément
    * qui peut être modifiable par étirement via la souris. Si c'est le cas, mémorise
    * l'élément en question dans "on", et retourne true */
   protected boolean onViaMouse(ViewSimple v,double x, double y) {
      if( onPoignee(v,x,y ) ) on = POIGNEE;
      else if( onCorner(v,x,y) ) on = CORNER;
      else on = NOTHING;
      return on!=NOTHING;
   }
   
   /** Modification d'un élément du tag via la molette de la souris. L'élément
    * en question aura été mémorisé auparavant par onWheel(...) */
   protected boolean modifyViaMouse(ViewSimple v,PointD p, PointD fixe) {
      if( on==POIGNEE ) return modifyPoignee(v,p.x,p.y);
      else if( on==CORNER ) { double dx = p.x-fixe.x; fixe.x=p.x; return modifyCorner(v,dx); }
      return false;
   }
      
   /** Utilisation de la poignée pour modifier la taille et la direction de la hampe
    * x et y sont données en coordonnées image */
   protected boolean modifyPoignee(ViewSimple v,double x, double y) {
      x = (x-xv[v.n])*v.zoom;
      y = (y-yv[v.n])*v.zoom;
      double ndist = Math.round(Math.sqrt(x*x + y*y));
      if( ndist<MINDIST ) ndist=MINDIST;
      double nangle = ndist==MINDIST ? angle : Math.atan2(y,x);
      if( nangle<0 ) nangle += Math.PI*2;
      else if( ndist>MAXDIST ) ndist=MAXDIST;
      if( Math.round(Math.toDegrees(nangle))==Math.toDegrees(angle) && dist==ndist  ) return false;
      angle=nangle; dist=ndist;
      if( tag==NOPOLE ) tag=0;
      setWH();
      return true;
   }
   
   /** Utilisation d'un coin pour modifier la taille de la fonte
    * x et y sont données en coordonnées image */
   protected boolean modifyCorner(ViewSimple v,double dx) {
      float oSize = getFont().getSize();
      
      float cran = (float)(dx*v.zoom)/3;
      if( cran==0 ) return false;
      if( angle>Math.PI/2 && angle<3*Math.PI/2 ) cran = -cran;
      
      float nSize = oSize+cran;
      if( nSize<MINFONT ) nSize=MINFONT;
      else if( nSize>MAXFONT ) nSize=MAXFONT;
      setFont(getFont().deriveFont(nSize));
      
      setWH();
      return true;
   }
   
   /** Changement de tag (taille et forme), (via la roulette) - on ne prend pas en compte le NOPOLE */
   protected void modifyTag(int sens) {
      tag+=sens;
      if( tag<0 ) tag=TAGS.length-2;
      else if( tag>=TAGS.length-1 ) tag=0;
      adjustTagSize();
   }
   
   // ajustement de la taille du tag en fonction de sa forme
   private void adjustTagSize() { L = TAGSIZE[tag]; }
   
   /** Changement de fond/bord, (via la roulette) */
   protected void modifyFond(int sens) {
      fond+= (float)sens*0.2f;
      if( fond>1 ) { fond=0f; bord = bord==0 ? 1 : 0; }
      else if( fond<0 ) { fond=1f; bord = bord==0 ? 1 : 0; }
   }
   
   protected void drawSelect(Graphics g,ViewSimple v) {
      super.drawSelect(g,v);
      if( !hasLabel() && !isArrow() ) return;
      
      // la poignée pour changer l'ancrage
      Point p = getViewCoord(v,50,50);
      Point poignee = getXYPoignee();
      int x = p.x+poignee.x;
      int y = p.y+poignee.y;
      g.setColor( on==POIGNEE ? Color.orange : Color.green );
      Util.fillCircle5(g,x,y );
      g.setColor( Color.black );
      Util.drawCircle5(g,x,y);
      
      // La poignée pour changer la taille
      if( hasLabel() ) {
         Point corner = getXYCorner();
         x = p.x+corner.x;
         y = p.y+corner.y;
         g.setColor( on==CORNER ? Color.orange : Color.green );
         Util.fillCircle5(g,x,y );
         g.setColor( Color.black );
         Util.drawCircle5(g,x,y);
      }
   }

  /** Generation d'un clip englobant.
   * Retourne un rectangle qui englobe l'objet
   * @param zoomview reference au zoom courant
   * @return         le rectangle enblobant
   */
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      if( rect1==null ) setWH();
      Point p = getViewCoord(v, 50,50);
      if( p==null ) return clip;
      int sel=0;
      if( isSelected() ) sel=DS;
      Point p1 = new Point(p.x+rect1.x-sel,p.y+rect1.y-sel);
      Point p2 = new Point(p.x+rect1.x+rect1.width+sel,p.y+rect1.y+rect1.height+sel);
      
      if( clip==null ) clip = new Rectangle(p1.x,p1.y, p2.x-p1.x+1, p2.y-p1.y+1);
      else {
         clip.add(p1);
         clip.add(p2);
      }
      if( hasLabel() ) {
         p1 = new Point(p.x+rect2.x-sel,p.y+rect2.y-sel);
         p2 = new Point(p.x+rect2.x+rect2.width+sel,p.y+rect2.y+rect2.height+sel);
         clip.add(p1);
         clip.add(p2);
      }
      return clip;
   }

   /** Détermination de la couleur de l'objet */
   protected Color getColor() {
      if( !isVisible() ) return null;
   	  if( couleur!=null ) return couleur;
   	  if( plan!=null && plan.type==Plan.APERTURE ) {
   	  	couleur = ((PlanField)plan).getColor(this);
   	  	if( couleur==null ) return plan.c;
   	  	return couleur;
   	  }
   	  if( plan!=null ) return plan.c;
   	  return Color.black;
   }
   
   // Retourne les coordonnées de l'accroche de la hampe (le tag est en 0,0)
   private Point getXYAncrage() {
      if( isReticle() ) {
         int x=0, y=0;
         if( angle<=Math.PI/4 || angle>=7*Math.PI/4 ) x=L;
         else if( angle>=3*Math.PI/4 && angle<=5*Math.PI/4 ) x=-L;
         if( angle>Math.PI/4 && angle<3*Math.PI/4 ) y=L;
         else if( angle>5*Math.PI/4 && angle<7*Math.PI/4 ) y=-L;
         return new Point(x,y);
      }
      return new Point( (int)Math.round( L*Math.cos(angle) ), (int)Math.round( L*Math.sin(angle) ));
   }
   
   // Retourne les coordonnées de la poignée de rotation (le tag est en 0,0)
   private Point getXYPoignee() {
      return new Point( (int)Math.round( dist*Math.cos(angle) ), (int)Math.round( dist*Math.sin(angle) ));
   }
   
   private Font getFont() { return F; }
   private void setFont(Font f) { F=f; }
   
   /** Set specifical color (dedicated for catalog sources) */
   public void setColor(Color c) { couleur=c; }
   
   // Retourne les dimensions du label
   private Dimension getDimLabel() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics( getFont() );
      int h=0,w=0;
      if( !hasLabel() ) return new Dimension(w,h);
      StringTokenizer st = new StringTokenizer(id,"\n");
      while( st.hasMoreTokens() ) {
         h += m.getHeight();
         int d = m.stringWidth(st.nextToken());
         if( d>w ) w=d;
      }
      if( id.charAt(id.length()-1)=='\n' ) h+= m.getHeight();
      return new Dimension(w,h);
   }
   
   // Retourne les coordonnées du centre du label (le tag est en 0,0)
   private Point getXYLabel() {
      Point p = getXYPoignee();
      int dx =  (tag==NOPOLE ? 0 : accroche) + getDimLabel().width/2;
      p.x += angle>Math.PI/2 && angle<3*Math.PI/2 ? -dx : dx;
      return p;
   }
   
   // retourne les coordonnées du coin extérieur du label (le tag est en 0,0)
   private Point getXYCorner() {
      Point p= getXYLabel();
      Dimension d = getDimLabel();
      int M=3;
      if( angle>Math.PI/2 && angle<3*Math.PI/2 ) p.x -= (d.width/2-M);
      else p.x += (d.width/2-M);
      if( angle>Math.PI && angle<2*Math.PI ) p.y -= (d.height/2-M);
      else p.y += (d.height/2-M);
      return p;
   }
   
   // Retourne les coordonnées du dernier caractère saisie (le tag est en 0,0)
   private Point getXYLastChar() {
      Dimension dim = getDimLabel();
      Point p = getXYLabel();
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics( getFont() );
      boolean aDroite =angle>Math.PI/2 && angle<3*Math.PI/2;
      int y1 = p.y - dim.height/2 + m.getAscent();
      Point p1 = new Point(p.x,y1-7);
      if( !hasLabel() ) return p1;
      StringTokenizer st = new StringTokenizer(id,"\n");
      while( st.hasMoreTokens() ) {
         String s = st.nextToken();
         p1.x = p.x + (aDroite ? dim.width/2 : -dim.width/2 +  m.stringWidth(s));
         p1.y = y1;
         y1 += m.getHeight();
      }
      if( id.charAt(id.length()-1)=='\n' ) {
         p1.y += m.getHeight();
         p1.x = p.x + (aDroite ? dim.width/2 : -dim.width/2);
      }
      return p1;
   }
   
   private void drawLabel(Graphics g,int x, int y) {
      Dimension dim = getDimLabel();
      Point p = getXYLabel();
      
      if( fond!=0 ) Util.drawArea(plan.aladin,g, p.x-(dim.width+4)/2 + x, p.y-(dim.height+4)/2 + y, dim.width+4, dim.height+4, 
               Color.white, fond*plan.getOpacityLevel(), false);
      if( bord==1 ) g.drawRect(p.x-(dim.width+4)/2 + x, p.y-(dim.height+4)/2 + y, dim.width+4, dim.height+4);
      
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics( getFont() );
      boolean aDroite =angle>Math.PI/2 && angle<3*Math.PI/2;
      int y1 = p.y - dim.height/2 + m.getAscent();
      StringTokenizer st = new StringTokenizer(id,"\n");
      while( st.hasMoreTokens() ) {
         String s = st.nextToken();
         int x1 = p.x + (aDroite ? dim.width/2 - m.stringWidth(s) : -dim.width/2);
         g.drawString(s,x+x1,y+y1);
         y1 += m.getHeight();
      }
   }
   
   private void drawHampe(Graphics g, int x, int y) {
      if( tag==NOPOLE || dist<=MINDIST ) return;
      Point ancrage = getXYAncrage();
      Point poignee = getXYPoignee();
      if( isArrow() ) Util.drawFleche(g,x+poignee.x,y+poignee.y,x+ancrage.x,y+ancrage.y,tag==BIGARROW ? 10 : 6,null);
      else g.drawLine(x+poignee.x,y+poignee.y,x+ancrage.x,y+ancrage.y);
      
      if( hasLabel() || editing ) {
         int att = angle>Math.PI/2 && angle<3*Math.PI/2 ? -(accroche-3) : (accroche-3);
         g.drawLine(x+poignee.x,y+poignee.y,x+poignee.x+att,y+poignee.y);
      }
   }
   
   private void drawCaret(Graphics g,int x, int y) {
      boolean blink=(System.currentTimeMillis()/500)%2==0;
      if( !blink ) return;
      Color c = g.getColor();
      Point p = getXYLastChar();
      x+=p.x; y+=p.y;
      int h = Toolkit.getDefaultToolkit().getFontMetrics( getFont() ).getHeight();
      g.setColor(Color.black);
      g.drawLine(x, y+2, x, y-h+2);
      g.setColor(Color.white);
      g.drawLine(x+1, y+2, x+1, y-h+2);
      g.setColor(c);
   }
   
   // Tracé d'une petite flèche en remplacement d'une grande lorsque le tag est trop petit
   private void drawMiniFleche(Graphics g, int x, int y) {
      int tmp = L; L=10;
      Point p = getXYAncrage();
      L=tmp;
      Util.drawFleche(g, x+p.x, y+p.y, x, y, 4, null);
   }
   
   private void drawTag(Graphics g, int x, int y) {
      if( tag==NOPOLE || dist<=MINDIST ) return;
      switch( tag ) {
         case RETICLE:
         case BIGRETICLE:
            g.drawLine(x-L, y,   x-2, y);   g.drawLine(x+2, y,   x+L, y);
            g.drawLine(x,   y-L, x,   y-2); g.drawLine(x,   y+2, x,   y+L);
            break;
         case SMALLCIRCLE:
         case CIRCLE:
         case BIGCIRCLE:
            if( L<3 ) Util.drawCircle5(g, x, y);
            else if( L<=4 ) Util.drawCircle8(g, x, y);
            else g.drawOval(x-L, y-L, L*2, L*2);
            break;
      }
   }
   
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) {
      if( !isVisible() ) return false;
      Point p = getViewCoord(v,50,50);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setFont( getFont() );
      g.setColor( getColor() );
      boolean isTooSmallForLabel = isTooSmallForLabel(v);
      
      // Le tag
      drawTag(g,p.x,p.y);
      
      if( isTooSmallForLabel ) {
         if( isArrow() ) drawMiniFleche(g,p.x,p.y);
         return true;
      }
      
      // Le label + la hampe
      if( hasLabel() || isArrow() ) {
         drawHampe(g,p.x,p.y);
         if( hasLabel() && !isTooSmallForLabel ) drawLabel(g,p.x,p.y);
      } 
      
      
      // L'édition
      if( editing ) {
         drawHampe(g,p.x,p.y);
         drawCaret(g,p.x,p.y);
      }

      if( isSelected() ) {
         if( plan!=null && plan.type==Plan.APERTURE ) return true;
//         g.setColor( Color.green );
         drawSelect(g,v);
      }
      return true;
   }
}
