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
import java.awt.geom.Ellipse2D;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import cds.tools.Util;

/**
 * Gestion d'un slide de la pile de plan avec possibilite de dossiers de slides
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (9 septembre 2002) Creation
 */
public final class Slide {
   // Les valeurs accociees aux differents elements graphiques
   static final int gapL =  Select.gapL; // Marge de gauche (reserve pour le triangle)
   static final int DX   =  Select.DX;   // Largeur d'un slide (hors tout)
   static final int DY   =  18;	         // Hauteur d'un slide (hors tout)
   static final int DFOLDER=10;		     // Decalage niveau de folder

   static final int VIDE=0;
   static final int GRIS=1;
   static final int NOIR=2;
   static final int DRAG=3;

   // Quelques couleurs
   static Color orange= new Color(255,102,0);
   static Color vert= new Color(0,187,0);
   static Color jauneGris= new Color(181,181,49);
   static Color grisfonce = new Color(90,90,90);

   // L'icone du calque
   static final int [] frX = Select.frX;
   static final int [] frY = Select.frY;
   static final int frMin  = Select.frMin;
   static final int frMax  = Select.frMax;
   static final int m      = Select.MILIEU;
   static final int xLabel = frMax+4;

   // Le logo pour indiquer qu'il s'agit d'un plan tool
   // - premier triangle, - deuxieme triangle
   static final int [] t1x = { 16,22,31,16 };
   static final int [] t1y = { 12, 7,10,12 };
   static final int [] t2x = { gapL+DX-7-9,gapL+DX-7,gapL+DX-7-2,gapL+DX-7-9 };
//   static final int [] t2x = {   35,44,42,35 };
   static final int [] t2y = {  3, 2, 6, 4 };

   // Le logo pour indiquer qu'il s'agit d'un plan image
   // - Grosse etoile
   static final int [] i1y = {  4, 3, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 6, 7, 8, 9,10,11,12,11,10};
   static final int [] i1x = { 28,26,17,14,12,11,10,10,11,12,15,14,14,16,21,22,22,21,19,15, 8, 6, 5};
   static final int [] i1l = {  1, 2, 9, 5, 3, 2, 2, 2, 2 ,6, 5, 6, 5, 6, 2, 2, 2, 2, 3, 5, 9, 2, 1};
   static final int [] i2y = {  3, 6,10,11,11,12 };
   static final int [] i2x = {  9,26,27, 4,26,27 };
   static final int [] i2l = {  1, 1, 1, 1, 3, 1 };

   // Le triangle reperant le plan de reference courant (dans la marge de gauche)
   // et les plans pouvant servir de reference a la projection
   static final int [] trX = { 4,  4,  12, 4};
   static final int [] trY = { 0, 16,  8,  0};


   Vector slides;	// Liste des sous slides
   Plan p;		// plan associe
   Aladin a;		// reference
   int x1,y1;		// Coin superieur gauche du logo (mise a jour dans draw() )
   int x2,y2;		// Coin inferieur droit du logo (mise a jour dans draw() )

   Plan planRGB=null;
   protected int mode=GRIS;

   static final int OFF = 0;
   static final int ON = 1;
   static final int FILTER = 2;

  /** Creation d'un slide simple
   * @param p References
   */
   protected Slide(Aladin aladin,Plan plan) {
      slides=null;
      p=plan;
      a=aladin;
   }

   /** retourne le plan associe au slide */
   protected Plan getPlan() {
      return p;
   }

   // Retourne vrai si on est sur le triangle de ref
   protected boolean inCheck(int x) {
      if( p.collapse ) return false;
       return( x<gapL );
   }

   // Retourne vrai si on est dans le logo du plan
   protected boolean inLogo(int x) {
      if( p.collapse ) return false;
      return( x1<=x && x<=x2 );
   }

   // Retourne vrai si on est dans le label
   protected boolean inLabel(int x) {
      if( p.collapse ) return false;
      return( x>x2 );
   }
   
   // Retourne vrai si on est dans une checkBox
   protected boolean inLogoCheck(int x){
      if( p.collapse ) return false;
      return x<=gapL  || inLogo(x)  ;
   }

   // Retourne vrai si on est dans le Slide
   protected boolean in(int y) {
      if( p.collapse ) return false;
      return( y1<y && y<=y2+1 );
   }

   // Retourne vrai si on est sous le logo (on compte qq pixels dans le logo lui-meme)
   protected boolean sous(int y) {
      if( p.collapse ) return false;
      return( y2-10<=y && y<=y2+5 );
   }

   // Dessine le repere du plan de reference en fonction
   // de l'etat et du type du plan (a.calque.plan[i]) passe en parametre
   // l'ordonne pour le dessin est passee en parametre (dy).
   // Si newREf est different de -1, on remplit le nouveau triangle
   // de reference
   void drawRepereRef(Graphics g,int dy,int mode) {
      if( !p.flagOk || (!p.ref && !p.isImage()) || mode==DRAG ) return;

      int [] y2  = new int[trX.length];
      for( int k=0; k<trX.length; k++ ) y2[k] = trY[k]+dy;

      if( mode!=VIDE ) {
         Color mc = g.getColor();
         g.setColor(mode==NOIR?Color.blue:Color.lightGray);
         g.fillPolygon(trX,y2,trX.length);
         g.setColor(mc);
      }
//      if( newRef || p.ref) g.fillPolygon(trX,y2,trX.length);
      
      if( g.getColor()==Color.black ) g.setColor(Color.green);
      g.fillPolygon(trX,y2,trX.length);
      g.drawPolygon(trX,y2,trX.length);
   }

   // Affichage d'un fond du logo de plan rempli en fonction d'une
   // valeur de pourcentage (remplissage vertical)
   static void fillLogo(Graphics g,int []x, int [] y,double pourcent,Color c) {
      double p = pourcent/100.;
      int X1,X2,Y;

      // Decallage en ordonnee et tracage
      Y = y[1]-2 - (int)( (y[1]-y[0]-4)*p);
      X1 = x[1]+ (int)( (x[0]-x[1])*p);
      X2 = x[2]- (int)( (x[2]-x[3])*p);
      x[0]=x[4]=X1;
      x[3]=X2;
      y[0]=y[3]=y[4]=Y;
      g.setColor(c);
      g.fillPolygon(x,y,x.length);
   }

   
  /** Affichage d'un fond du logo de plan rempli en fonction d'une
   * valeur de pourcentage (remplissage horizontal)
   * @return l'absisse finale pour pouvoir faire le bouton sinon -1
   */
   static int fillTransparency(Graphics g,int [] x,int [] y,float transp,boolean flagDraw,Color c) {
      double p = transp;
      int X;
      
      g.setColor(c);

      // Remplissage du trapèze en fonction de la transparence
      if( p<0.2 ) {
         p = p*5;
         int Y = y[0]+ (int)((y[1] - y[0]) * (1-p));
         X = x[1]+ (int)((x[0] - x[1]) * p)+1;
         x[0]=x[2]=x[3]=X;
         y[0]=y[3]=Y;
      } else if( p<0.8 ) {
         p = (p-0.2)/0.6;
         X = x[0] + (int)( (x[3] - x[0])* p);
         x[2] = x[3]=X;
      } else {
         p=(p-0.8)/0.2;
         int Y = y[3]+ (int)((y[2] - y[3]) * p);
         X = x[3]+ (int)((x[2] - x[3]) * p)+1;
         x[4]=x[3]; x[2]=x[3]=X;
         y[4]=y[3]; y[3]=Y;
      }
      if( flagDraw ) g.fillPolygon(x,y,x.length);
      return X;
   }

   // Adaptation du logo pour un plan TOOL
   static void drawLogoTool(Graphics g,int dx,int dy) { drawLogoTool(g,dx,dy,orange); }
   static void drawLogoTool(Graphics g,int dx,int dy,Color c) {
      int i;
      int [] y1 = new int[t1y.length];
      int [] y2 = new int[t2y.length];
      int [] x1 = new int[t1x.length];
      int [] x2 = new int[t2x.length];

      // Decalage
      for( i=0; i<y1.length; i++ ) y1[i]=t1y[i]+dy;
      for( i=0; i<y2.length; i++ ) y2[i]=t2y[i]+dy;
      for( i=0; i<x1.length; i++ ) x1[i]=t1x[i]+dx;
      for( i=0; i<x2.length; i++ ) x2[i]=t2x[i]+dx;

      // Trace
      g.setColor( c );
      g.fillPolygon(x1,y1,y1.length);                // Triangle devant
      g.fillPolygon(x2,y2,y2.length);                // Triangle derriere
      g.drawLine(x1[0],y1[0], x2[1],y2[1]);          // Ligne intermediaire

   }

   // Adaptation du logo pour un plan CATALOG
   static void drawLogoCat(Graphics g,int dx,int dy) { drawLogoCat(g,dx,dy,vert); }
   static void drawLogoCat(Graphics g,int dx,int dy,Color c) {
      int i;
      int L = (DX-10)/2-3;

      g.setColor( c );
      int x=gapL+DX/2+dx;
      for( i=0; i<5; i++ ) {
         int h = 4+dy+i*2;
         g.drawLine(x-L-i,h, x-5-i,h);
         g.drawLine(x-2-i,h, x,h);
         g.drawLine(x+3,h, x+5+i,h);
         g.drawLine(x+8+i,h, x+L+1+i,h);
      }
   }
   
   static final int EP=4; // Epaisseur d'un logo de filter

   // Adaptation du logo pour un plan Filter
   static void drawLogoFilter(Graphics g,int dx,int dy,
                              boolean active,boolean button,Color c,Color bord) {
      int L=button?Tool.W-7:DX;		// Largeur
      int H=button?Tool.H-24:frY[1]-frY[0];	// Hauteur
      int i;
      int deb=-3,fin=3;
      int x1=L/2-5,x2=L/2-6;

      // Dessin de la grille du fond
      g.setColor( c );

      int x=(button?-2:gapL)+DX/2+dx;
      if( button ) { deb=-2; fin=2; }

      for( i=0; i<4; i++ ) {
         int h = 4+dy+i*2;
         g.drawLine(x-x1-i,h, x+x2+i,h);
      }
      for( i=deb; i<=fin; i++ ) {
         g.drawLine(x+i*3,2+dy, x+i*5,dy+H);
      }

      g.setColor( bord );
      // Dessin des 4 bords du logo du filtre
      for( i=0; i<4; i++ ) {
         int bx[] = new int[5];
         int by[] = new int[5];
         switch(i) {
            // Bord du fond
            case 0: bx[0]=bx[1]=bx[4]=frX[0]-gapL; bx[2]=bx[3]=frX[3]-gapL-DX+L;
                    by[0]=by[3]=by[4]=frY[0]; by[1]=by[2]=frY[0]-EP+2;
                    break;
            // Bord gauche
            case 1: bx[0]=bx[3]=bx[4]=frX[0]-gapL; bx[1]=bx[2]=frX[1]-gapL;
                    by[0]=by[4]=frY[0]; by[3]=frY[0]-EP+2; by[1]=frY[0]+H;
                    by[2]=frY[0]+H-EP;
                    break;
            // Bord droit
            case 2: bx[0]=bx[3]=bx[4]=frX[3]-gapL-DX+L; bx[1]=bx[2]=frX[2]-gapL-DX+L;
                    by[0]=by[4]=frY[3]; by[3]=frY[3]-EP+2; by[1]=frY[0]+H;
                    by[2]=frY[0]+H-EP;
                    break;
            // Bord de devant
            case 3: bx[0]=bx[1]=bx[4]=frX[1]-gapL; bx[2]=bx[3]=frX[2]-gapL-DX+L;
                    by[0]=by[3]=by[4]=frY[0]+H; by[1]=by[2]=frY[0]+H-EP;
                    break;
         }
         for( int j=0; j<bx.length; j++ ) {
            bx[j]+= dx + (!button?gapL:0);
            by[j]+=(dy+2);
         }

         g.setColor( button?bord:
                            (!active?Color.yellow:jauneGris) );
         g.fillPolygon(bx,by,bx.length);
         g.setColor( !button?Color.black:c);
         g.drawPolygon(bx,by,bx.length);
      }

   }
   
   // Adaptation du logo pour un plan IMAGE
   static void drawLogoImg(Graphics g,int dx,int dy,Color c) {
      int i;

      // La galaxie
      if( c!=null ) g.setColor(c);
      for( i=0; i<i1x.length; i++ ) {
         
         // Couleur spéciale pour le RGB (c==null)
         if( c==null ) {
            int j = (i*3)/i1x.length;
            g.setColor(j==0?Color.red:j==1?Color.blue:Color.green);
         }
         
         g.drawLine(i1x[i]+dx+gapL,i1y[i]+dy,i1x[i]+i1l[i]+dx-1+gapL,i1y[i]+dy);
      }

      // Les étoiles
      g.setColor(c!=null?c:Color.black);
      for( i=0; i<i2x.length; i++ ) {
         g.drawLine(i2x[i]+dx+gapL,i2y[i]+dy,i2x[i]+i2l[i]+dx-1+gapL,i2y[i]+dy);
      }
   }
   
   // Adaptation du logo pour un plan IMAGEHUGE
   static void drawLogoImgHuge(Graphics g,int dx,int dy,Color c) {
      drawLogoImg(g,dx,dy,c);
      g.setColor( Color.yellow );
      g.drawRect(dx+frX[2]-15,dy+frY[2]-8,5,5);
   }
   
   // Adaptation du logo pour un plan MOC
   static void drawLogoMOC(Graphics g,int dx,int dy,Color c) {
      int x = dx+gapL+10;//frX[2]/2;
      int y = dy+3;
      Grid.fillMOC(g,x,y,Color.white);
      Grid.drawMOC(g, x, y, c);
   }
   
   // Adaptation du logo pour un plan SED
   static void drawLogoSED(Graphics g,int dx,int dy,Color c) {
      int x = dx+gapL+8;
      int y = dy+1;
      Grid.drawSED(g, x, y, c);
   }

   // Adaptation du logo pour un plan BGHPX
   static void drawLogoImgBG(Graphics g,int dx,int dy,Color c) {
      int x = dx+gapL+10;//frX[2]/2;
      int y = dy+3;
      Grid.fillBG(g,x,y,Color.white);
      Grid.drawGrid(g, x, y, c);
   }

   // Adaptation du logo pour un plan BGHPX en mode POLARISATION
   static void drawLogoPolarisation(Graphics g,int dx,int dy,Color c) {
      int x = dx+gapL+10;//frX[2]/2;
      int y = dy+3;
      Grid.fillBG(g,x,y,Color.white);
      Grid.drawPolar(g, x, y, c);
   }

   // Dessine le blink d'etat vert/blanc
   static protected void drawBlink(Graphics g, int x,int y) {
      drawBlink(g,x,y,Color.green,Color.white);
   }

   // Dessine le blink d'etat (clignotant entre deux couleurs)
   static protected void drawBlink(Graphics g, int x,int y,Color c1,Color c2) {
      drawBall(g,x,y,blinkState?c1:c2);
   }

   static boolean blinkState = true;      // Memorise l'etat de clignotement des blinks
   synchronized void setBlink(boolean f) {
      a.calque.select.setSlideBlink(f);
   }

   static protected void drawBall(Graphics g, int x,int y,Color c) {
      x+=4; y+=3;
      g.setColor(c);
      Util.fillCircle8(g,x,y);
      g.setColor(Color.black);
      Util.drawCircle8(g,x,y);
      g.setColor(Color.white);
      g.fillRect(x-1,y-1,2,2);
   }


   // Dessin du voyant d'etat barre d'une croix
   static protected void drawCross(Graphics g, int x, int y) {
      drawBall(g,x,y,Color.white);
      g.setColor(Color.red);
      int W = 10;
      x--;
      y-=2;
      g.drawLine(x,y,x+W,y+W);
      g.drawLine(x,y+1,x+W,y+W+1);
      g.drawLine(x,y+W,x+W,y);
      g.drawLine(x,y+W+1,x+W,y+1);
   }

   private int [] xc=null,yc=null;

   protected void redraw(Graphics g,int xMouse,int yMouse,int dx,int dy) {
      try {
         Color labelBG= a.calque.select.getBackground();
         Color colorForeground = p.c;
         Color colorBorder = Color.gray;
         Color colorFillFG = Aladin.MYGRAY;
         Color colorFillBG = Color.white;
         
         if( p.collapse  ) return;
         
         if( xc==null ) {
            yc = new int[frX.length];
            xc = new int[frX.length];
         }
         // Calcul la taille des lettres
         int ht = Aladin.SIZE;
         
         // Determination de la couleur du dessin du logo dans le cas où
         // il y a un plan couleur (choix des trois composantes RGB)
         if( planRGB!=null ) {
            boolean flagInv = ((PlanImage)planRGB).video==PlanImage.VIDEO_INVERSE;
            if( p.hashCode()==((PlanImageRGB)planRGB).pi[0] )
               colorForeground=flagInv?Color.cyan:Color.red;
            // on teste le bleu avant au cas ou il n'y aurait que 2 couleurs
            else if( p.hashCode()==((PlanImageRGB)planRGB).pi[2] )
               colorForeground=flagInv?Color.yellow:Color.blue;
            else if( p.hashCode()==((PlanImageRGB)planRGB).pi[1] )
               colorForeground=flagInv?Color.magenta:Color.green;
         }
//         g.setColor( Color.black );
         
         // ref==true => repérage du plan
         boolean ref = xMouse<=0 && p.type!=Plan.NO && p.underMouse && a.view.isVisible(p) && a.view.isMultiView();
                  
         // Decallage du logo en fonction de la position
         for( int i=0; i<frX.length; i++ ) yc[i]=frY[i]+dy;
         for( int i=0; i<frX.length; i++ ) xc[i]=frX[i]+dx;
         
         // Paramètres pour tracer le cadre et la bordure
         int y = dy-1;
         int x = dx+xLabel-1; // 1;
         int H = ht+7;
         int W = Select.ws-x;
         
         // Tracage du fond particulier sur le label
         if(  mode!=DRAG && (
               ref || p.selected  || p.type!=Plan.NO && in(yMouse) && inLabel(xMouse)) ) {
            labelBG=(p.selected ? Aladin.STACKBLUE : Aladin.STACKGRAY );
            g.setColor(labelBG);
            Graphics2D g2d = (Graphics2D)g;
            Paint gradient = new GradientPaint(x, y, labelBG.brighter(), x, y+H, labelBG);
            g2d.setPaint(gradient);
            g.fillRect(x,y,W,H);
         } else if( mode!=DRAG ) {
            g.setColor(labelBG);
            g.fillRect(x,y,W,H);
         }
         
         boolean isRefForVisibleView = p.isRefForVisibleView();
         
         // Le gris de remplissage des plan de référence est plus appuyé
         // que pour les autres plans
         if( /* p.ref */ isRefForVisibleView ) {
            colorFillFG = Color.gray;
            colorBorder = new Color(50,50,50);
         }
         
         boolean canBeTransparent= a.calque.canBeTransparent(p);
         boolean isViewable = p.isViewable();
         boolean inLogo = inLogo(xMouse) && in(yMouse);
         
         // On penche un peu plus le logo si la souris est dessus
         if( inLogo && isRefForVisibleView ) {
            xc[0]--; yc[0]--; xc[4]--; yc[4]--;
            xc[3]++; yc[3]--; 
            xc[1]--; yc[1]++; 
            xc[2]++; yc[2]++; 
         }
         
         int xPoignee=frMin+dx;

         // Remplissage du pourcentage du plan (ajout thomas : remplissage pour PlanContour et pour PlanFilter)
         if( (p.isImage() || p.type==Plan.TOOL && p instanceof PlanContour || p instanceof PlanBG )
               && p.error==null && !p.flagOk ) {
            double pourcent = p.getPourcent();
            g.setColor(colorFillBG);
            g.fillPolygon(xc,yc,frX.length);   // Faut bien mettre un fond 
            fillLogo(g,xc,yc,pourcent,colorFillFG);
            
            // Sinon, dessin du calque en fonction du mode activé ou non
         } else {
            if( p.type==Plan.FOLDER ) g.setColor(!p.active? Color.yellow:jauneGris);
            else if( /* p.ref */ isRefForVisibleView && (p.isUnderImgBkgd() && p.type!=Plan.ALLSKYIMG) ) g.setColor(colorFillBG);
            else g.setColor( !p.active || /*!p.ref*/ !isRefForVisibleView && isViewable && canBeTransparent  ? colorFillBG : colorFillFG ) ;
//            else g.setColor( p.active && !canBeTransparent && p.isViewable() ? Color.gray : colorFillBG );
//            if( g.getColor()==Color.blue &&  (colorForeground==Color.blue || colorForeground==Color.black) ) colorForeground=Color.white;
            g.fillPolygon(xc,yc,frX.length);

            if( canBeTransparent ) {
               float transp = p.getOpacityLevel();
               if( transp!=0 ) {
                  xPoignee=fillTransparency(g,xc,yc,transp,p.active,colorFillFG); 
//                  xPoignee=fillTransparency(g,xc,yc,transp,p.active || p.isImage(),colorFillFG ); 
               }
            }
         }
         
//         if( p.type!=Plan.NO ) g.setColor( colorBorder = p.ref ? Color.black : Color.gray);
//         else g.setColor(colorBorder = Aladin.MYGRAY);
         
         g.setColor( colorBorder );
         
         // on n'affiche le bord du cadre que pour les plans de référence
         // et on fait un effet de relief si la souris est dessus
//         drawSlideBorder(g,xc,yc,inLogo,p.ref);
         if( isRefForVisibleView || inLogo ) g.drawPolygon(xc,yc,frX.length);
         
         // Cote clair à droite et en bas du calque courant
//         if( p.ref ) {
//            g.setColor(colorBorder.brighter());
//            g.drawLine(xc[2],yc[2],xc[3],yc[3]);
//         }
         g.drawLine(xc[1],yc[1],xc[2],yc[2]);

         // Affichage de la petite languette du folder + le logo indiquant le localScope
         // ou le numéro indiquant le nombre de plans
         if( p.type==Plan.FOLDER ) {
            int xf[] = new int[5];
            int yf[] = new int[5];
            xf[0]=xf[4]=xc[1]; yf[0]=yf[4]=yf[3]=yc[1];
            xf[1]=xc[1]+3;     yf[1]=yf[2]=yc[1]+3;
            xf[2]=xc[1]+15;
            xf[3]=xc[1]+18;
            if( !a.calque.isCollapsed(p) ) {
               g.drawLine(xc[2]-2,yc[1]+1,xc[2]-2,yc[1]+1);
               g.drawLine(xc[1]+2,yc[1]+2,xc[2]-1,yc[1]+2);
            } else {
               g.drawLine(xc[1],yc[1]+1,xc[2],yc[1]+1);
               g.drawLine(xc[1],yc[1]+2,xc[2],yc[1]+2);
            }
            
            // On dessine une enveloppe comme logo pour le localScope
            if( ((PlanFolder)p).localScope ) {
//               g.setColor(Color.black);
               int a = (xc[0]+xc[3])/2;
               int b = (yc[0]+yc[1])/2 +1;
               g.drawLine(xc[0],yc[0],a,b);
               g.drawLine(a,b,xc[3],yc[3]);
               g.drawLine(xc[1],yc[1],a-2,b-1);
               g.drawLine(a+2,b-1,xc[2],yc[2]);
            }
                        
            g.setColor(Color.yellow );
            g.fillPolygon(xf,yf,xf.length);
            g.setColor(colorBorder);
            g.drawPolygon(xf,yf,xf.length);
         } else if( p instanceof PlanImageBlink ) {
            g.drawLine(xc[1]+1,yc[1]+1,xc[1]+1,yc[1]+1);
            g.drawLine(xc[2]-2,yc[1]+1,xc[2]-2,yc[1]+1);
            g.drawLine(xc[1],yc[1]+2,xc[2]-1,yc[1]+2);
         } else {
            g.drawLine(xc[1],yc[1]+1,xc[2],yc[1]+1);
         }
         
         // Le logo du plan en fonction de son type
         switch( p.type ) {
            case Plan.IMAGE:
            case Plan.IMAGEALGO:
            case Plan.IMAGERSP:    drawLogoImg(g,dx,dy,colorForeground);                      break;
            case Plan.IMAGECUBERGB:
            case Plan.IMAGERGB:    drawLogoImg(g,dx,dy,null);                                 break;
            case Plan.IMAGEHUGE:   drawLogoImgHuge(g,dx,dy,colorForeground);                  break;
            case Plan.ALLSKYMOC:   drawLogoMOC(g,dx,dy,isViewable?p.c:colorFillFG);           break;
            case Plan.ALLSKYCAT:   drawLogoImgBG(g,dx,dy,isViewable?p.c:colorFillFG);         break;
            case Plan.ALLSKYIMG:   drawLogoImgBG(g,dx,dy,isViewable?Color.black:colorFillFG); break;
            case Plan.ALLSKYPOL:   drawLogoPolarisation(g,dx,dy,isViewable?p.c:colorFillFG);  break;
            case Plan.IMAGEMOSAIC:
            case Plan.IMAGECUBE: 
            case Plan.IMAGEBLINK:  drawLogoImg(g,dx,dy,Color.black);                          break;
            case Plan.APERTURE:
            case Plan.TOOL:        drawLogoTool(g,dx,dy,colorForeground);                     break;
            case Plan.CATALOG:     if( p.isSED() ) drawLogoSED(g,dx,dy,colorForeground);
                                   else drawLogoCat(g,dx,dy,colorForeground);                 break;
            case Plan.FILTER:      drawLogoFilter(g,dx,dy,p.active,false,Color.black,Color.black); break;
         }
         
         // Affichage de la checkbox
//         if( p.type!=Plan.NO && mode!=DRAG && p.flagOk && !p.hasError()
//               && !(p.isCatalog() && !p.hasObj())) {
//            
//            boolean isRefForVisibleView = p.isRefForVisibleView();
////            Util.drawRadio(g,Select.ws-23,dy+3, Color.gray ,
////                  inLogoCheck(xMouse) && in(yMouse) ? Aladin.BLUE : null, 
////                        isRefForVisibleView ? Aladin.GREEN : Color.black, 
////                        p.ref);
//            Util.drawCheckbox(g, 3, dy+3, Color.gray ,
//                  inLogoCheck(xMouse) && in(yMouse) ? Aladin.BLUE : null, 
//                  isRefForVisibleView ? Color.red : Color.black, 
//                  p.ref || p.active /* && !(p.isImage() && !isRefForVisibleView) */);
//         }
         
         // Dessin de la checkbox de contrôle de la référence si nécessaire
          drawCheckBox(g,3, dy+3, xMouse,yMouse, mode, p);
    
         // Les barres d'appartenance à un folder    
         if( mode!=DRAG && p.folder>0 ) {
            Plan [] allPlan = a.calque.getPlans();
            int n=a.calque.getIndex(allPlan,p);
            g.setColor( Color.black );
            for( int i=0; i<p.folder; i++ ) {

               // tjrs une barre vert. pour le dernier niveau
               boolean last=(i==p.folder-1); 

               // Détermine s'il faut ou non une barre verticale au niveau i
               boolean trouve=false;
               for( int j=n+1; j<allPlan.length; j++ ) {
                  if( allPlan[j].folder==i ) break;
                  if( allPlan[j].folder==i+1 ) {
                     trouve=true;
                     break;
                  }
               }
               if( !trouve && !last ) continue;

               // Demi-barre verticale ?
               boolean demi= !trouve && last
               && (n==allPlan.length-1 || allPlan[n+1].folder!=p.folder);
               x=(gapL-10)+ (i+1)*DFOLDER;
               g.drawLine(x+5,dy,x+5,dy+ (demi ? DY/2 : DY));

               //System.out.println(p.label+" ("+p.folder+") trouve="+trouve+" lst="+last+" demi="+demi
               //    +" ["+(n<allPlan.length?allPlan[n+1].label+"/"+allPlan[n+1].folder:"-")+"]");               

               // Barre horizontale pour le dernier niveau
               if( last ) g.drawLine(x+5,dy+DY/2,x+12,dy+DY/2);
            }
         }
         
         // Le libelle
         g.setFont( Aladin.BOLD );
         if( p.label!=null ) {
            int py = dy+15-1;
            int px = Select.ws-12;
            int px1 = px;//4;
            x = dx+xLabel;
            
            try {
               if( (labelBG==Aladin.STACKGRAY
                     || labelBG==Aladin.STACKBLUE) && (p.c.equals(Couleur.DC[2])) ) g.setColor(Aladin.GREEN);
               else if( labelBG==Aladin.STACKBLUE 
                     && (p.c.equals(Couleur.DC[7]) || p.c.equals(Couleur.DC[8])) ) g.setColor(Color.white);
               else g.setColor(p.c);
            } catch( Exception e) {}
            g.drawString(p.getLabel(),x,py);
            
           //le voyant d'état
            if( mode!=DRAG ) {
               if( p.type==Plan.FOLDER && !p.isSync() ) { drawBlink(g,px,py-9);setBlink(true); }
               else if( p.isSimpleCatalog() || p.isImage()
                     || p instanceof PlanContour || p.type==Plan.FILTER
                     || p instanceof PlanBG ) {
                  if( p.error!=null ) {
                     boolean hasObj = p.pcat!=null && p.pcat.hasObj();
                     if( p.hasNoReduction() && (!p.isSimpleCatalog() || hasObj)) drawBall(g,px,py-9,Color.orange);
                     else if( p.isSimpleCatalog() && !hasObj ) drawCross(g,px1,py-9);
                     else if( p instanceof PlanMoc && ((PlanMoc)p).getMoc().getSize()==0 ) drawCross(g,px1,py-9);
                     else drawBall(g,px1,py-9,Color.red);
                  } else {
                     boolean flag=false;
                     Color green = p instanceof PlanBG && ((PlanBG)p).hasMoreDetails() ? Color.yellow : Color.green; // Color.green: Aladin.GREEN ;
                     if( !p.flagOk ||
                           ( (p instanceof PlanContour) && (((PlanContour)p).mustAdjustContour)) ||
                           (flag=(p.flagProcessing 
                                 || (p.type==Plan.IMAGEHUGE && ((PlanImageHuge)p).isExtracting) 
                                 || (p instanceof PlanBG && ((PlanBG)p).isLoading()) )) )
                     {
                        if( flag ) drawBlink(g,px,py-9,Color.orange,green);
                        else drawBlink(g,px1,py-9);
                        
                        setBlink(true);
                        
                     } else {
                        if( p instanceof PlanBG  && p.active ) drawBall(g,px,py-9, green );
                     }
                  }
               }
            }
         }
         
         // Le curseur de réglage de la transparence
         if( mode!=DRAG && canBeTransparent ) {
            int largeur = 3;
            int debut = frX[1];
            int fin = frX[2];
            int haut = frY[1];
            int xPos = xPoignee;
            if( xPos<dx+debut+largeur ) xPos = dx+debut+largeur;
            if( xPos>dx+fin-largeur ) xPos = dx+fin-largeur;
            g.setColor( Color.black );
            g.drawLine( dx+debut,dy+haut,dx+fin,dy+haut);
            g.drawLine( dx+debut,dy+haut+1,dx+fin,dy+haut+1);
            float transp = p.getOpacityLevel();
//            g.setColor( transp<=0.1 ? Color.white : transp>=0.9 ? Color.green : Color.yellow);
            g.setColor( transp<=0.1 ? Color.red : Color.green );
            g.fillRect(xPos-1,dy+haut-largeur,largeur,largeur*2+1);
            g.setColor(Color.black);
            g.drawRect(xPos-1,dy+haut-largeur,largeur,largeur*2+1);
         }

      } catch( Exception e ) { e.printStackTrace(); }
   }
   
   private void drawCheckBox(Graphics g, int x, int y, int xMouse, int yMouse, int mode, Plan p) {
      p.setHasCheckBox(false);
      
      // Plan pas encore prêt ?
      if( !p.isReady() ) return;
      
      // Il s'agit d'un folder
      if( p.type==Plan.FOLDER ) return;
      
      // Plan en cours de déplacement ?
      if( mode==DRAG ) return;
      
      // Plan overlays qui peut être projeté sur un autre plan => on évite de mettre
      // la checkbox pour ne pas surcharger les contrôles
      if( !p.shouldHaveARefCheckBox() ) return;
      
      // Faut-il faire temporairement clignoter la checkbox pour signaler à l'utilisateur
      // qu'il doit l'utiliser pour changer de référence
      if( p.isCheckBoxBlink() ) {
         Util.drawCheckbox(g, x, y,  Color.gray , inCheck(xMouse) && in(yMouse) ? Aladin.BLUE : null,  Color.red, blinkState );
         setBlink(true);
         a.calque.select.drawMessage(g, a.calque.select.getLastMessage(), Color.red);
      } else {
         Util.drawCheckbox(g, x, y,  Color.gray , inCheck(xMouse) && in(yMouse) ? Aladin.BLUE : null,  Color.black, 
               /* p.ref */p.isRefForVisibleView() );
      }
      
      p.setHasCheckBox(true);
      
   }
   
   // Dessin du bord d'un calque, avec effet de relief si la souris se situe dessus
//   private void drawSlideBorder(Graphics g,int [] xc, int [] yc, boolean mouseIn, boolean isRef) {
//      if( mouseIn ) {
//         g.drawLine(xc[0]-1,yc[0]-1,xc[1]-1,yc[1]+1);
//         g.drawLine(xc[0]-1,yc[0]-1,xc[3]+1,yc[3]-1);
//         g.drawLine(xc[2]+1,yc[2]+1,xc[1]-1,yc[1]+1);
//         g.drawLine(xc[2]+1,yc[2]+1,xc[3]+1,yc[3]-1);
//      }
//      if( isRef ) g.drawPolygon(xc,yc,xc.length);
//      
//   }
   
   /** Dessin d'un slide en cours de déplacement */
   protected void dragDraw(Graphics g,int x,int y ) {
      mode=DRAG;
      redraw(g,-1,-1,x-gapL-DX/2,y-DY/2);
   }
   
   /** Retraçage du slide en se basant sur les calculs précédents */
   protected void redraw(Graphics g, int xMouse, int yMouse) {
      redraw(g,xMouse,yMouse,x1-gapL,y1);
   }

  /** Dessin de slide */
   protected int draw(Graphics g, int dy, int xMouse, int yMouse, Plan planRGB,int mode) {
      // Slide collapse, on ne fait rien
      if( p.collapse  ) return dy;

      int dx = p.folder*DFOLDER;

      // Calcul de la position du Slide
      x1 = gapL+dx;  x2 = frMax+4+dx;
      y1 = dy; y2 = dy+DY;

      // Memorisation des infos de trace
      this.planRGB=planRGB;
      this.mode=mode;

      redraw(g,xMouse,yMouse,dx,dy);

      return dy-=DY;
   }
}
