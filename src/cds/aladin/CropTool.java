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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import cds.tools.Util;

/**
 * Outil de recadrage. Permet de définir une zône rectangulaire en vue de faire
 * une extraction d'une portion de l'image
 * @author Pierre Fernique [CDS]
 * @version 1 (création Juillet 2010)
 */
public class CropTool  {
   // Poignées pour étirer le rectangle
   static final int IN=0,HG=1,HD=2,BD=3,BG=4,H=5,D=6,B=7,G=8,START=9;
   
   // Curseur en fonction de la position de la souris
   static final int [] CURSOR = { Cursor.HAND_CURSOR, Cursor.NW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR,
      Cursor.SE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR,
      Cursor.E_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR,Cursor.SE_RESIZE_CURSOR };
   
   // Numéro des différents boutons et labels
   static final private int OK = 0;
   static final private int CLOSE = 1;
   static final private int LABEL_POS   = 2;
   static final private int LABEL_RES   = 3;
   static final private int LABEL_WIDTH = 4;
   static final private int LABEL_HEIGHT= 5;
   static final private int CHECKBOX_RES   = 6;
   
   // Taille de la poignée d'étirement
   static final int W=20;
   
   private Aladin aladin;
   private Color color = Color.red;
   private Plan plan;               // Le Plan à cropper
   private RectangleD r;            // La taille du rectangle dans les coordonnées de l'image
   private double resMult;          // Facteur multiplicatif pour la résolution finale (uniquement pour PlanBG)
   private boolean withFullRes;     // true si on utilise fullRes
   private boolean fullRes;         // Statut du checkbox du full résolution
   private boolean visible=false;   // true si l'outil de cadrage est visible
   private int edit=-1;             // indique le code du label en cours d'édition
   private StringBuffer stringEdit=null;  // La chaine en cours d'édition dans le label indiqué par "edit"
   
   private int moveLabel=-1;        // Label sous la souris (simple survol par la souris)
   private int movePoignee=-1;      // Poignée active suite à un simple survol par la souris
   private int dragPoignee=-1;      // Poignée active du clic & drag en cours
   private double dragX=-1,dragY=-1;   // Coordonnées de référence d'un clic & drag
   
   private RectangleD rectButton[] = new RectangleD[7];  // Zone couverte par chaque boutons et labels (coordonnées view)
   private boolean hasButton=false;                    // true si les boutons sont affichés


   /** Création de l'outil, à la position indiquée - on suppose qu'on démarre
    * immédiatement un clic & drag d'extension (équivalent à la poignée BD (Bas-Droit) */
   public CropTool(Aladin aladin,ViewSimple v,Plan plan,double xview,double yview,boolean withFullRes) {
      this.aladin=aladin;
      this.plan = plan;
      PointD p = v.getPosition(xview,yview);
      r = new RectangleD(p.x, p.y, 1/v.zoom, 1/v.zoom);
      dragX=p.x;
      dragY=p.y;
      dragPoignee=START;
      visible=true;
      this.withFullRes=withFullRes;
      fullRes=false;
      resMult=1;
   }
   
   /** Déplacement du rectangle du crop notamment dans le cas d'un changement de projection
    * d'un plan PlanBG */
   public void deltaXY(double x, double y) {
      r.x+=x; r.y+=y;
   }
   
   /** Traitement d'un clic sur un bouton. Retourne true i on a cliqué dans un boutons
    * sinon false */
   public boolean submit(ViewSimple v,double xview, double yview) {
      if( !hasButton ) return false;  // les boutons ne sont pas affichés
      
      edit=-1;
            
      if( rectButton[OK].contains(xview, yview) ) {
         doCrop(v);
         return true;
      }
      
      if( rectButton[CLOSE].contains(xview, yview) ) return true;
      
      // Dans un label éditable ?
      for( int i=LABEL_POS; i<CHECKBOX_RES; i++ ) {
         if( rectButton[i]!=null && rectButton[i].contains(xview, yview) ) startEdit(i,v);
      }
      
      // Dans la checkbox de résolution ?
      if( rectButton[CHECKBOX_RES]!=null && rectButton[CHECKBOX_RES].contains(xview, yview) ) switchCheckFullRes(v);
      
      return false;
   }
   
   /** Effectue le crop sur l'image courante */
   public void doCrop(ViewSimple v) {
      if( doMocCrop(v) ) return;
      aladin.calque.newPlanImageByCrop(v, getRectangle(), getResMult(),fullRes );
      aladin.log("Crop",v.pref.getLogInfo());
   }
   
   public boolean doMocCrop(ViewSimple v) {
      Plan p = plan;
      if( !(p instanceof PlanMoc) ) return false;
      Projection proj = v.getProj();
      Coord [] coo = new Coord[4];
      for( int i=0; i<4; i++ ) {
         Coord c = new Coord();
         c.x = r.x + ((i==1 || i==2) ? r.width : 0);
         c.y = r.y + (i>1 ? r.height : 0);
         proj.getCoord(c);
         coo[i]=c;
      }
      aladin.calque.newPlanMoc("Crop "+p.label,(PlanMoc)p,coo);
      return true;
   }
   
   /** Permute la checkbox de full résolution */
   public void switchCheckFullRes(ViewSimple v) {
      fullRes = !fullRes;
      resMult = computeResMult(v);
      aladin.view.repaintAll();
   }
   
   /** Taille angulaire du pixel de la vue pour le crop en cours */
   private double getPixelSizeInView(ViewSimple v) {
      double tailleDE;
      Coord coo1 = new Coord();
      Coord coo2 = new Coord();
      Projection proj = v.getProj();
      coo1.x = r.x+r.width/2; coo1.y = r.y+r.height/2;
      coo2.x = r.x+r.width/2; coo2.y = r.y+r.height;
      proj.getCoord(coo1);
      proj.getCoord(coo2);
      tailleDE=Coord.getDist(coo1,coo2)*2;
      return tailleDE/(r.height*v.zoom);
   }
   
   /** Détermine le facteur multiplicatif du crop pour avoir une full résolution */
   private double computeResMult(ViewSimple v) {
      if( !fullRes ) return 1.;
      double resPixelView = getPixelSizeInView(v); //v.getPixelSize(); // Taille du pixel écran
      double resPixelSurvey = ((PlanBG)v.pref).getPixelResolution();  // Taille du pixel du survey HEALPix
      return resPixelView/resPixelSurvey;
   }
   
   /** Démarre l'édition du label i */
   public void startEdit(int i,ViewSimple v) {
      edit=i;
      stringEdit = new StringBuffer(""+(
            i==LABEL_POS ? Util.myRound(r.x+"",0)+","+Util.myRound( (((PlanImage)v.pref).naxis2-(r.y+r.height))+"",0) : 
            i==LABEL_HEIGHT ? Util.myRound(""+getHeight(v),0) : i==LABEL_WIDTH ?  
                  Util.myRound(""+getWidth(v)) : getResMult() ));
      aladin.view.startTimer(500);
   }
   
   /** Traitement d'un évènement caractère */
   public boolean keyPress(ViewSimple v,KeyEvent e) {
      if( !isEditing() ) return false;
      
      int key = e.getKeyCode();
      char k = e.getKeyChar();
      
      if( key==KeyEvent.VK_ESCAPE ) {
         stopEditing();
         return true;
      }
      
      if( key==KeyEvent.VK_ENTER ) {
         try {
            int i;
            switch(edit) {
               case LABEL_POS:
                  i = stringEdit.indexOf(",");
                  if( i>0 ) {
                     r.x = Double.parseDouble(stringEdit.substring(0,i).trim());
                     
                     r.y = Double.parseDouble(stringEdit.substring(i+1).trim());
                     r.y = (((PlanImage)v.pref).naxis2-r.y) -r.height;
                  }
                  break;
               case LABEL_WIDTH: 
                  r.width  = Double.parseDouble(stringEdit.toString().trim());
                  if( v.pref instanceof PlanBG ) r.width/=(v.zoom*getResMult());
                  break;
               case LABEL_HEIGHT:
                  r.height = Double.parseDouble(stringEdit.toString().trim());
                  if( v.pref instanceof PlanBG ) r.height/=(v.zoom*getResMult());
                  break;
               case LABEL_RES:
                  resMult = Double.parseDouble(stringEdit.toString());
                  break;
            }
         } catch( NumberFormatException e1 ) { }
         stopEditing();
         return true;
      } 
      
      // On efface le dernier caractere
      if( key==KeyEvent.VK_BACK_SPACE || key==KeyEvent.VK_DELETE ) {
         int n = stringEdit.length();
         if( n==0 ) return false;
         stringEdit.deleteCharAt(n-1);
         return true;
      }
      
      // On insere un nouveau caractere
      if( k>=31 && k<=255 ) {
         stringEdit.append(k);
         return true;
      }
      
      return false;
   }
   
   /** retourne la largeur du rectangle (dans les coordonnées de l'image sauf s'il s'agit d'un planBG.
    * Dans ce cas ce sont les coordonnées de la vue) */
   public double getWidth(ViewSimple v) {
      if( v.pref instanceof PlanBG ) return Math.round(r.width*v.zoom*getResMult()); //v.top(r.width*v.zoom*getResMult());
      return r.width;
   }
   
   /** retourne la hauteur du rectangle (dans les coordonnées de l'image sauf s'il s'agit d'un planBG.
    * Dans ce cas ce sont les coordonnées de la vue) */
   public double getHeight(ViewSimple v) {
      if( v.pref instanceof PlanBG ) return Math.round(r.height*v.zoom*getResMult()); //v.top(r.height*v.zoom*getResMult());
      return r.height;
   }
   /** retourne le rectangle du crop (dans les coordonnées de l'image) */
   public RectangleD getRectangle() {
      return new RectangleD(r.x,r.y,r.width,r.height);
   } 
   
   /** Retourne le facteur multiplicateur de la résolution (uniquement pour les PlanBG) */
   public double getResMult() { return !withFullRes ? 1. : resMult; }
   
   /** Réutilisation "en l'état" */
   public void reset() { setVisible(true); }
   
   /** Affiche ou cache */
   public void setVisible(boolean flag) { visible=flag; }
   
   /** True si l'outil de recadrage est visible */
   public boolean isVisible() { return visible; }
   
   /** Retourne les coordonnées sur lesquelles il faudrait zoomer (coordonnées image) */
   public PointD getFocusPos() {
      if( isEditing() ) return new PointD(rectButton[edit].x+rectButton[edit].width,rectButton[edit].y);
      return new PointD(r.x+r.width/2, r.y+r.height/2);
   }
   
   /** True si on est en train de faire un clic & drag */
   public boolean isDragging() { return visible && dragPoignee!=-1; }
   
   /** True si on est en train d'éditer en élement */
   public boolean isEditing() { return visible && edit!=-1; }
   
   /** Arrêt de l'édition en cours */
   public void stopEditing() { edit=-1; }
   
   /** Reçoit un évènement de la vue suite à un survol de la souris */
   public void mouseMove(double xview, double yview,ViewSimple v) {
      if( !visible ) return;
      moveLabel = getLabel(xview,yview);
      if( moveLabel!=-1 ) return;
      PointD p = v.getPosition(xview,yview);
      movePoignee = getPoignee(p.x,p.y,v);
   }
   
   /** Evènement débutant un clic & drag */
   public boolean startDrag(ViewSimple v,double xview,double yview) {
      if( !visible ) return false;
      PointD p = v.getPosition(xview,yview);
      dragPoignee = getPoignee(p.x,p.y,v);
      if( dragPoignee==-1 ) { dragX=dragY=-1; return false; }
      dragX=p.x;
      dragY=p.y;
      return true;
   }
   
   /** Evènement lors d'un clic & drag => extension du rectangle */
   public void mouseDrag(ViewSimple vs, int xview, int yview,boolean shift) {
      if( !visible ) return;
      PointD p = vs.getPosition((double)xview,(double)yview);
      double x = p.x;
      double y = p.y;
      double dx = x-dragX;
      double dy = y-dragY;
      if( shift && (dragPoignee==HG || dragPoignee==HD || dragPoignee==BG || dragPoignee==BD) ) {
         if( r.width<r.height ) r.width=r.height;
         else r.height=r.width;
         if( Math.abs(dx)<Math.abs(dy) ) dx=dy;
         else dy=dx;
      }
      switch(dragPoignee) {
         case IN: r.x+=dx; r.y+=dy; break;
         case HG: r.x+=dx; r.y+=dy; r.width-=dx; r.height-=dy; break;
         case HD: r.y+=dy; r.width+=dx; r.height-=dy; break;
         case BD: case START: r.width+=dx; r.height+=dy; break;
         case BG: r.x+=dx; r.width-=dx; r.height+=dy; break;
         case H:  r.y+=dy; r.height-=dy; break;
         case D:  r.width+=dx; break;
         case B:  r.height+=dy; break;
         case G:  r.x+=dx; r.width-=dx; break;
      }
      
      // On ne peut donner une taille inf à 1
      if( r.width*vs.zoom<1 ) r.width=1/vs.zoom;
      if( r.height*vs.zoom<1 ) r.height=1/vs.zoom;
            
      dragX=x;
      dragY=y;
   }
   
   /** Fin d'un clic & drag */
   public void endDrag(ViewSimple vs) {
      dragPoignee=-1;
      dragX=dragY=-1;
      
      // On s'aligne sur des pixels entiers
      if( vs.pref instanceof PlanBG /* fullRes */ ) {
         double f = vs.zoom*getResMult();
         r.x = Math.round(r.x*f)/f;
         r.y = Math.round(r.y*f)/f;
         r.height = Math.round(r.height*f)/f;
         r.width = Math.round(r.width*f)/f;
      } else {
         r.x=Math.round(r.x);    
         r.y=Math.round(r.y);
         r.width=Math.round(r.width);  
         r.height=Math.round(r.height);
      }
   }
   
   // Retourne le numéro du label sous la souris (coordonnées de la vue)
   private int getLabel(double xview,double yview) {
      try {
         for( int i=LABEL_POS; i<=LABEL_HEIGHT; i++ ) {
            if( !fullRes && i==LABEL_RES ) continue;
            if( rectButton[i].contains(xview,yview) ) return i;
         }
      } catch( Exception e ) { }
      return -1;
   }
   
   // Retourne la poignée sous la souris (coordonnées images)
   private int getPoignee(double x, double y,ViewSimple v) {
      if( !visible ) return -1;
      if( r.width==1 || r.height==1 ) return BD;
      for( int i=1; i<=8; i++ ) {
         RectangleD rc = getRectPoignee(i,v);
         if( rc.contains(x,y) ) return i;
      }
      return r.contains(x,y) ? IN : -1;
   }
   
   // Retourne le rectangle correspondant à la poignée
   private RectangleD getRectPoignee(int poignee,ViewSimple v) {
      if( !visible ) return null;
      double w = W/v.zoom;
      double w2 = w/2;
      switch(poignee) {
         case HG:  return new RectangleD(r.x-w2,r.y-w2, w,w);
         case HD:  return new RectangleD(r.x+r.width-w2,r.y-w2, w,w);
         case BD:  return new RectangleD(r.x+r.width-w2,r.y+r.height-w2, w,w);
         case BG:  return new RectangleD(r.x-w2,r.y+r.height-w2, w,w);
         
         case H:   return new RectangleD(r.x+w,r.y-w2, r.width-2*w,w);
         case D:   return new RectangleD(r.x+r.width-w2,r.y+w, w,r.height-2*w);
         case G:   return new RectangleD(r.x-w2,r.y+w, w,r.height-2*w);
         case B:   return new RectangleD(r.x+w,r.y+r.height-w2, r.width-2*w,w);
      }
      return null;
   }
   
   // Dessin du checkbox dans les coordonnées de la vue
   private void drawCheckbox(Graphics g,int x,int y) {
      if( !withFullRes ) return;
      String s = "original resolution";
      FontMetrics f = g.getFontMetrics();
      int w = f.stringWidth(s)+17;
      int h = f.getHeight();
      Util.drawCartouche(g, x, y, w, h, 0.6f,  null, Color.white);
      Util.drawCheckbox(g, x, y+3, null, null, Color.red, fullRes);
      g.setColor(Color.black);
      Font ft = g.getFont();
      g.setFont(ft.deriveFont(Font.BOLD));
      g.drawString(s,x+15,y+h/2+5);
      g.setFont(ft);
      rectButton[CHECKBOX_RES] = new RectangleD(x,y,w,h);
   }
   
   // Dessin des boutons dans les coordonnées de la view
   // @param code OK ou CLOSE
   private void drawButton(Graphics g,int code,int x,int y,int w,int h) {
      g.setColor(Aladin.BKGD );
      g.fillRect(x, y, w, h);
      rectButton[code] = new RectangleD(x,y,w,h);
      g.setColor(Color.white);
      g.drawLine(x,y,x+w,y); g.drawLine(x,y,x,y+h);
      g.setColor(Color.black);
      g.drawLine(x,y+h,x+w,y+h); g.drawLine(x+w,y,x+w,y+h);
      String s = aladin.chaine.getString(code==0 ? "OK" : "CLOSE");
      FontMetrics f = g.getFontMetrics();
      g.drawString(s, x+w/2-f.stringWidth(s)/2, y+h-3);
   }
   
   private void drawLabel(Graphics g,int code,final ViewSimple v) {
      PointD hg = v.getPositionInView(r.x, r.y);
      PointD bd = v.getPositionInView(r.x+r.width, r.y+r.height);
      int x=0,y=0,w=0,h=15;
      boolean ed = edit==code;
      String s= ed ? stringEdit.toString() : null;
      switch(code) {
         case LABEL_RES:
            if( fullRes || !withFullRes ) return;
            if( s==null ) {
               double r = getResMult();
               s="res. x "+Util.myRound(r+"",2);
            }
            w=g.getFontMetrics().stringWidth(s);
            x=(int)(hg.x+h/2+2); y=(int)(bd.y-h-3);
            break;
         case LABEL_POS:   
            if( v.pref instanceof PlanBG ) return;      // Pas de positionnement pour un plan BG
            if( s==null ) s = Util.myRound(r.x+"",0)+","+Util.myRound(""+(((PlanImage)v.pref).naxis2-(r.y+r.height)),0); // s=r.x+","+r.y; 
            w=g.getFontMetrics().stringWidth(s);
            x=(int)(hg.x+h/2+2); y=(int)(bd.y-h-3);
            break;
         case LABEL_WIDTH: 
            if( s==null ) s=Util.myRound(""+getWidth(v),0);
            w=g.getFontMetrics().stringWidth(s);
            x=(int)(bd.x+hg.x)/2-w/2; y=(int)(hg.y+3);
            break;
         case LABEL_HEIGHT:
            if( s==null ) s=Util.myRound(""+getHeight(v),0);
            w=g.getFontMetrics().stringWidth(s);
            x=(int)(bd.x -w-h/2-1); y=(int)(bd.y+hg.y)/2 -h/2;
            break;
      }
      Util.drawCartouche(g, x, y, w, h, ed ? 1f : 0.6f,  ed ? Color.black : null, Color.white);
      g.setColor(Color.black);
      Font ft = g.getFont();
      g.setFont(ft.deriveFont(Font.BOLD));
      g.drawString(s,x,y+h/2+5);
      g.setFont(ft);
      rectButton[code] = new RectangleD(x,y,w,h);
      
      if( ed ) {
         x = x+w-1;
         boolean blink=(System.currentTimeMillis()/500)%2==0;
         if( blink ) {
            g.setColor(Color.black);
            g.drawLine(x, y+2, x, y+h-2);
            g.drawLine(x+1, y+2, x+1, y+h-2);
         }
      }
   }
   
   /** Dessin de l'outil de cadrage: le cadre, les labels de tailles, les boutons de contrôle */
   public void draw(Graphics g,ViewSimple v) {
      if( !visible ) return;
      
      withFullRes = v.pref instanceof PlanBG && v.pref.hasAvailablePixels();
      
      g.setColor(color);
      
      // Conversion dans les coordonnées de la vue
      PointD hg = v.getPositionInView(r.x, r.y);
      PointD bd = v.getPositionInView(r.x+r.width, r.y+r.height);
      RectangleD rc = new RectangleD(hg.x, hg.y, bd.x-hg.x+1,bd.y-hg.y+1 );
      int w = v.getWidth();
      int h = v.getHeight();
      Util.drawArea(aladin, g, 0,0,(int)w,(int)rc.y, color, 0.15f, false);
      Util.drawArea(aladin, g, 0,(int)rc.y,(int)rc.x,(int)rc.height, color,0.15f, false);
      Util.drawArea(aladin, g, (int)(rc.x+rc.width),(int)rc.y,(int)(w-(rc.x+rc.width)),(int)rc.height, color,0.15f, false);
      Util.drawArea(aladin, g, 0,(int)rc.y+(int)rc.height,w,(int)(h-(rc.y+rc.height)), color,0.15f, false);
      g.drawRect((int)hg.x, (int)hg.y, (int)(bd.x-hg.x+1),(int)(bd.y-hg.y+1) );
      
      // Les boutons et les labels ne seront pas affichés
      // si le rectangle est trop petit (il est tjs possible de zoomer)
      hasButton=false;
      boolean flagMoc = plan instanceof PlanMoc;
      if( !flagMoc && (bd.x-hg.x)>80 && (bd.y-hg.y)>50 ) {

         // Tracé des labels
         drawLabel(g,LABEL_RES,v);
         drawLabel(g,LABEL_POS,v);
         drawLabel(g,LABEL_WIDTH,v);
         drawLabel(g,LABEL_HEIGHT,v);
      }

      // Tracé des boutons
      int x = (int)((bd.x+hg.x)/2-45);
      int y = (int)( bd.y+ (bd.y-hg.y>2.*v.getHeight()/3 ? -25 : 15) );
      if( dragX==-1 ) {
         drawButton(g,OK,x,y,45,15);
         drawButton(g,CLOSE,x+45+5,y,50,15);
         hasButton=true;
      }
      
      if( !flagMoc) {
         // Tracé de la checkbox
         x = (int)( hg.x+10 );
         y = (int)( hg.y+ (bd.y-hg.y>2.*v.getHeight()/3 ? 4: -20) );
         drawCheckbox(g,x,y);
      }

      // Mise en forme du curseur
      int cursor = Cursor.DEFAULT_CURSOR;
      if( moveLabel!=-1 ) cursor=Cursor.TEXT_CURSOR;
      else {
         int fleche = dragPoignee!=-1 ? dragPoignee : movePoignee;
         if( fleche!=-1 ) cursor = CURSOR[fleche];
      }
      if( cursor!=oCursor ) { oCursor=cursor; v.setCursor(new Cursor(cursor)); }
   }

   private int oCursor=-1;
}
