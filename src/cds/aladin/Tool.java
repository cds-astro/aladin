// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

import cds.tools.Util;

/**
 * Gestion des differents outils de la barre des boutons
 *
 * @author Pierre Fernique [CDS]
 * @version 1.4 : (nov 2004) Ajout du bouton Blink
 * @version 1.3 : (27 fev 02) Ajout du bouton Contour
 * @version 1.2 : (31 jan 02) Ajout du bouton RGB
 * @version 1.1 : (28 mars 00) Retoilettage du code
 * @version 1.0 : (10 mai 99)  Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Tool {
   // Couleurs geres
   static final Color CS  = Aladin.COLOR_CONTROL_FOREGROUND;     // Couleur si l'outil est selectionnable
   //   static final Color CNS = Color.gray;      // COuleur si l'outil n'est pas selectionnable
   static final Color CNS = Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE;      // COuleur si l'outil n'est pas selectionnable
   static final Color CD = Aladin.COLOR_TOOL_DOWN;   // Couleur du bouton appuyé
   static final Color CU = Aladin.COLOR_TOOL_UP;      // Couleur du bouton releve

   // Differents modes possible d'un bouton
   static final int UNAVAIL =  0;           //   0 - UP/non-selectionnable
   static final int UP      =  1;           //   1 - UP/selectionnable
   public static final int DOWN    = -1;           //  -1 - DOWN/selectionnable

   // Reference aux autres objets
   Aladin aladin;
   Calque calque;

   // Parametres generaux
   static int H;                       // Hauteur d'un bouton
   static int W;                       // Largeur d'un bouton

   // Valeurs a memoriser
   protected int X=-1,Y=-1;            // position du bouton (coin haut gauche)
   protected String nom;               // nom du bouton
   int ntool;                          // Type de l'outil
   int omode;                          // Precedent mode;
   public int mode;                           // Mode du bouton

   // Liste des outils n'ayant pas d'une icone
   static final int [] noIcone = { };

   // Les labels de chaque outils
   static String [] label=null;

   // L'explication (1 ligne) de chaque outils
   static String [] explanation;


   // Les parametres graphiques...

   // Decalage du logo par rapport au centre du bouton (vers la droite)
   static final int [] dX = {
      /* 0 */      7,
      /* 1 */      6,
      /* 2 */      12,
      /* 3 */      9,
      /* 4 */      13,
      /* 5 */      10,
      /* 6 */      12,
      /* 7 */      12,
      /* 8 */      10,
      /* 9 */      12,
      /*10 */      13,
      /*11 */      5,
      /*12 */      9,
      /*13 */      -6,
      /*14 */      0,
      /*15 */      12,
      /*16 */  	   7,
      /*17 */      15,
      /*18 */      15,
      /*19 */      9,
      /*20 */      9,
      /*21 */      11,
      /*22 */      12,
   };

   // Décalage du logo par rapport au centre du bouton (vers le haut)
   static final int [] dY = {
      /* 0 */      9,
      /* 1 */      10,
      /* 2 */      10,
      /* 3 */      9,
      /* 4 */      2,
      /* 5 */      10,
      /* 6 */      10,
      /* 7 */      10,
      /* 8 */      12,
      /* 9 */      11,
      /*10 */      9,
      /*11 */      8,
      /*12 */      7,
      /*13 */      9,
      /*14 */      2,
      /*15 */      10,
      /*16 */      10,
      /*17 */      12,
      /*18 */      15,
      /*19 */      9,
      /*20 */      9,
      /*21 */      9,
      /*22 */      8,
   };

   // La fleche de selection
   static final int [] selectX = { 0,10, 5, 7, 5, 3, 0 };
   static final int [] selectY = { 0,10,10,15,16,11,14 };

   // Le manche du crayon
   static final int [] craymX = {   3,14,15,17,18, 7, 5, 4, 3, 3};
   static final int [] craymY = {   11,0, 0, 2, 4,15,14,13,12,11 };

   // La pointe du crayon
   static final int [] craypX = {  0, 3, 3, 4, 5, 7, 0 };
   static final int [] craypY = { 18,11,12,13,14,15,18 };

   // Les  deux traits du crayon (dans le manche
   static final int [] craytX = {  3,15,17, 5 };
   static final int [] craytY = { 12, 0, 2,14 };

   // le dessin du A
   static final int [] exAX = {  0, 1,17,18,18,19,14,15,15, 2, 3, 0  };
   static final int [] exAY = { 16,16, 0, 0,16,16,16,16, 3,16,16,16  };

   // La barre du A
   static final int [] bAX = {  8,15, 8};
   static final int [] bAY = { 10,10,10};

   //   //Le dessin du repere (rectangle horizontal)
   //   static final int []  = {  0,18,18,0, 0 };
   //   static final int [] hRhRXY = { 10,10, 8,8,10 };
   //
   //   //Le dessin du repere (rectangle vertical)
   //   static final int [] vRX = {  10,10, 8,8,10 };
   //   static final int [] vRY = {   0,18,18,0, 0 };

   //Le dessin de la fleche de mesure de distance
   static final int [] fmX = { 0,5,3,21,19,24,19,21,3, 5,  0  };
   static final int [] fmY = { 5,0,4, 4, 0, 5,10, 6,6,10,  5  };

   //Le repere 1 de la fleche de mesure de distance
   static final int [] t1mX = {  fmX[0],fmX[0] };
   static final int [] t1mY = {  fmY[1],fmY[6] };

   //Le repere 2 de la fleche de mesure de distance
   static final int [] t2mX = {  fmX[5],fmX[5] };
   static final int [] t2mY = {  fmY[1],fmY[6] };

   // Petit histogramme au-dessus de la fleche
   static final int cutG[] = { 11,10,9,7,5,3,2,2,3,5,7,9,10,11 };

   // Les segments X1,X2,Y pour les ciseaux (extérieurs)
   static final int CROPN[][] = {
      {0,0,0}, {15,15,0},
      {0,1,1}, {14,15,1},
      {0,2,2}, {13,15,2},
      {1,3,3}, {12,14,3},
      {2,4,4}, {11,13,4},
      {3,5,5}, {10,12,5},
      {4,6,6}, {9,11,6},
      {5,10,7},
      {6,9,8},
      {4,12,9},
      {2,4,10}, {6,9,10}, {11,13,10},
      {1,2,11}, {6,6,11}, {9,9,11}, {13,14,11},
      {1,2,12}, {5,6,12}, {9,10,12}, {13,14,12},
      {1,5,13}, {10,14,13},
      {2,4,14}, {11,13,14}
   };
   // Les segments X1,X2,Y pour les ciseaux (intérieurs)
   static final int CROPB[][] = {
      {5,5,10}, {10,10,10},
      {3,5,11}, {10,12,11},
      {3,4,12}, {11,12,12}
   };
   
   // Les segments X1,X2,Y pour le MOC
   static final int MOC[][] = {
         {15,15,0}, {21,21,0},
         {14,14,1}, {16,16,1}, {20,20,1}, {22,22,1},
         {13,13,2}, {17,17,2}, {19,19,2}, {23,23,2},
         {6,6,3},   {12,12,3}, {18,18,3}, {24,24,3},
         {5,5,4},   {7,7,4},   {11,11,4}, {13,13,4}, {17,17,4},{19,19,4}, {23,23,4},
         {4,4,5},   {8,8,5},   {10,10,5}, {14,14,5}, {16,16,5},{20,20,5}, {22,22,5},
         {3,3,6},   {9,9,6},   {15,15,6}, {21,21,6},
         {2,2,7},   {10,10,7}, {14,14,7}, {22,22,7},
         {1,1,8},   {11,11,8}, {13,13,8}, {23,23,8},
         {0,0,9},   {12,12,9}, {24,24,9},
         {1,1,10},  {11,11,10},{13,13,10},{23,23,10},
         {2,2,11},  {10,10,11},{14,14,11}, {22,22,11},
         {3,3,12},  {9,9,12},  {15,15,12}, {21,21,12},
         {4,4,13},  {8,8,13},  {10,10,13}, {14,14,13}, {16,16,13},{20,20,13},
         {5,5,14},  {7,7,14},  {11,11,14}, {13,13,14}, {17,17,14},{19,19,14}, 
         {6,6,15},  {12,12,15},{18,18,15},
   };

   /*
   //Le carre du label
   static final int [] clX = {  8, 3, 3,14,14};
   static final int [] clY = {  0, 0,11,11, 8};

   // La position du texte du label
   static final int lX = 10;
   static final int lY =  7;

   // Le texte du label
   static final String lS = "AZ";
    */
   // La croix
   static final int [] crX = { 2,9,16,17,10,17,16, 9, 2, 1,8,1,2 };
   static final int [] crY = { 0,7, 0, 1, 8,15,16, 9,16,15,8,1,0 };

   // La mini-fenetre des proprietes
   static final int [] p1X = { 0,23,23, 0,   0 };        //Bord externe
   static final int [] p1Y = { 0, 0,18,18,   0 };
   static final int [] p2X = { p1X[0]+2,p1X[1]-2,p1X[2]-2,p1X[3]+2,  p1X[0]+2}; //Bord interne
   static final int [] p2Y = { p1Y[0]+5,p1Y[1]+5,p1Y[2]-2,p1Y[3]-2,  p1Y[0]+5};
   static final int [] p3X = { 1,1,3,3,   1  }; //Petit rectangle
   static final int [] p3Y = { 1,3,3,1,   1  };

   // Les 2 rectangles du resampling
   static final int [] r1X = { 5,15,15,5 };
   static final int [] r1Y = { 13,13,3,3 };
   static final int [] r2X = { 6,12,18,12 };
   static final int [] r2Y = { 6,0,6,12 };

   // Le manche de la loupe
   static final int [] lpX = { 12,19,17,10,12 };
   static final int [] lpY = {  9,16,18,11, 9 };

   // Cercle pour la loupe (X1,X2,Y)  et comme c'est symétrique...
   // On prend le même tableau en Y1,Y2,X
   static final int [][] C = {{4,7,0},{4,7,11},
      {2,3,1},{8,9,1}, {2,3,10},{8,9,10},
   };

   // Les ellipses concentriques pour les contours : (X1,X2,Y)
   static final int [][] ctH = { {5,11,0},
      {3,4,1},{7,13,1},
      {2,2,2},{6,6,2},{12,12,2},{14,14,2},
      {1,1,3},{5,5,3},{9,11,3},{13,13,3},{15,15,3},
      {9,11,6},
      {5,5,7},{13,13,7},
      {6,6,8},{12,12,8},
      {1,1,9},{7,11,9},{15,15,9},
      {2,2,10},{14,14,10},
      {3,4,11},{12,13,11},
      {5,11,12} };

   // Les ellipses concentriques pour les contours : (Y1,Y2,X)
   static final int [][] ctV = { {4,8,0},{4,6,4},{4,5,8},{4,5,12},{4,6,14},{4,8,16} };

   // La main du Move (vecteurs verticaux, horizontaux et les rectangles de remplissage)
   static final int [][] pV = { {3,12,6}, {2,10,9},{2,10,12},{3,10,15},{5,16,17},
      {11,12,2},{13,15,3},{17,19,6},{17,19,16} };
   static final int [][] pH = { {10,11,1},{7,8,2},{13,14,2},{16,16,4},{1,3,9},{0,0,10},
      {4,5,10},{1,1,11},{4,5,16},{17,17,17},{7,15,19}, };
   static final int [][] pF = {{ 6,3,10,17}, {16,5,2,12},{4,11,2,5},{2,10,3,1},{3,11,1,2}};


   // Le Z du Zoom
   static final int [] zX = { 0,11,11, 2,11,11, 0, 0,9,0,   0 };
   static final int [] zY = { 1, 1, 4,13,13,15,15,12,3,3,   1 };

   // Le graphique des couleurs
   static final int [] cmX = { 0, 0,  0,16};      // L'abscisse
   static final int [] cmY = { 0,16, 24,16};      // L'ordonnee
   static final int [][] cmb = {{ 3,9, 4, 7},    // Les barres
      {  6, 3, 4,13},
      {  9, 0, 4,16},
      { 12, 2, 4,14},
      { 15, 7, 4,9}};
   static final int [] cmlX= { 1,15,25};         // Les X de la courbe de reponse
   static final int [] cmlY= {15, 9, 0};         // Les X de la courbe de reponse

   // Le graphique des allumettes
   static final int [] m1X = { 2, 14, 16,  4, 3,  2};  // Le baton
   static final int [] m1Y = { 17, 5,  7, 19, 19, 18};
   static final int [] m2Y = { 18,  7};
   static final int [][] m3X = { {16,18,2},{15,15,3},{18,19,3}, // Le bout rouge (X1,X2,Y)
      {14,19,4},{14,19,5},{15,18,6},{16,17,7}};

   // Les points pour le diagramme de dispersion
   int [][] plot = { { 2,3 }, {4,4}, {5,5}, {5,7}, { 7,5}, {6,4}, {7,9}, {9,10}, {11,12}, {13,13}, {14, 5} };

   
   // Le graphique pour le SED
   static final int [] sedX= {  4,8,12,16,20,22};  // Les X de la courbe
   static final int [] sedY= { 14,4,12, 8,12,12};  // Les Y de la courbe

   /*
   // Le graphique pour le ACE
   static final int [] aceX= {  4, 5,11,12,16,18,19,20};  // Les X des objets
   static final int [] aceY= {  8,14, 4,10,15,8, 8,13};  // Les Y des objets
    */
   /** Construction d'un outil
    * @param n numero de l'outil (cf ToolBox.DRAW...)
    * @param aladin Reference
    */
   protected Tool(int ntool,Aladin aladin) {
      this.aladin = aladin;
      this.calque = aladin.calque;
      this.ntool = ntool;
      if( label==null ) createChaine();
      mode  = UNAVAIL;
      omode = DOWN;
      nom = label[ntool];
   }

   private void createChaine() {
      label=new String[]{
            aladin.chaine.getString("SELECT"),
            aladin.chaine.getString("DRAW"),
            aladin.chaine.getString("TAG"),
            aladin.chaine.getString("PHOT1"),
            aladin.chaine.getString("DIST"),
            aladin.chaine.getString("DEL"),
            aladin.chaine.getString("MGLSS"),
            aladin.chaine.getString("PROP"),
            aladin.chaine.getString("PAN"),
            aladin.chaine.getString("HIST"),
            aladin.chaine.getString("PAD"),
            aladin.chaine.getString("RGB"),
            aladin.chaine.getString("CONT"),
            aladin.chaine.getString("FILTER"),
            aladin.chaine.getString("SYNC"),
            aladin.chaine.getString("BLINK"),
            aladin.chaine.getString("ZOOM"),
            aladin.chaine.getString("MXMATCH"),
            aladin.chaine.getString("RSAMP"),
            aladin.chaine.getString("CROP"),
            aladin.chaine.getString("PLOT"),
            aladin.chaine.getString("SPECT"),
            aladin.chaine.getString("MOC"),
      };

      explanation=new String[]{
            aladin.chaine.getString("HSELECT"),
            aladin.chaine.getString("HDRAW"),
            aladin.chaine.getString("HTAG"),
            aladin.chaine.getString("HPHOT"),
            aladin.chaine.getString("HDIST"),
            aladin.chaine.getString("HDEL"),
            aladin.chaine.getString("HMGLSS"),
            aladin.chaine.getString("HPROP"),
            aladin.chaine.getString("HPAN"),
            aladin.chaine.getString("HHIST"),
            aladin.chaine.getString("HPAD"),
            aladin.chaine.getString("HRGB"),
            aladin.chaine.getString("HCONT"),
            aladin.chaine.getString("HFILTER"),
            aladin.chaine.getString("HSYNC"),
            aladin.chaine.getString("HBLINK"),
            aladin.chaine.getString("HZOOM"),
            aladin.chaine.getString("HXMATCH"),
            aladin.chaine.getString("HRSAMP"),
            aladin.chaine.getString("HCROP"),
            aladin.chaine.getString("HPLOT"),
            aladin.chaine.getString("HSPECT"),
            aladin.chaine.getString("HMOC"),
      };
   }

   /** Fixe la taille des boutons
    * @param newW,newH Nouvelles tailles
    */
   protected static void resize(int newW,int newH) {
      W=newW;
      H=newH;
   }

   /** Selection/Deselection de l'outil (mode UP <-> DOWN) */
   protected boolean Push() {
      if( mode==UNAVAIL ) return false;
      mode=-mode;
      return true;
   }

   /** Positionnement du mode du bouton */
   protected void setMode(int m) { mode=m; }

   /** Retourne la description associee a l'outil */
   protected String getInfo() {
      if( ntool>=explanation.length ) return "";
      return explanation[ntool];
   }

   // Creation et deplacement d'un polygone
   static protected Polygon setPolygon(int [] x, int [] y, int dx, int dy) {
      Polygon p = new Polygon(x,y,x.length);
      for( int i=0; i<p.npoints; i++ ) {
         p.xpoints[i]+=dx;
         p.ypoints[i]+=dy;
      }
      return p;
   }

   // Retourne le decallage en X et en Y d'une icone
   int dX() { return ntool<dX.length?dX[ntool]:0; }
   int dY() { return ntool<dY.length?dY[ntool]:0; }

   /** Retourne true si la coordonnée est dans le bouton */
   protected boolean in(int x,int y) {
      return x>=X && x<=X+W && y>=Y && y<=Y+H;
   }

   /** Retourne la position centrale du bouton ou null s'il n'a pas été tracé */
   protected Point getWidgetLocation() {
      if( X==-1 && Y==-1 ) return null;
      return new Point( X+W/2, Y+H/2);
   }

   /** Dessin du bouton de FORCE
    * @param g       Contexte graphique a utiliser
    * @param idx,idy Origine du dessin
    * @param currentButton true si c'est le bouton courant (affichage vert)
    */
   protected void drawIcone(Graphics g,int idx, int idy, boolean currentButton) {
      updateIcone(g,idx,idy,true,currentButton);
   }

   /** Dessin du bouton de SI NECESSAIRE
    * @param g       Contexte graphique a utiliser
    * @param idx,idy Origine du dessin
    * @param currentButton true si c'est le bouton courant (affichage vert)
    */
   protected void updateIcone(Graphics g,int idx, int idy, boolean currentButton) {
      updateIcone(g,idx,idy,false,currentButton);
   }

   // Dessin du bouton
   void updateIcone(Graphics g,int idx, int idy,boolean flagforce,boolean currentButton) {
      int i;
      if( !flagforce && omode==mode ) return;
      omode=mode;

      Polygon p;
      //      boolean flagIsForTool = ToolBox.isForTool(ntool) /* || ntool==ToolBox.FILTER */;
//      boolean flagIsForTool = false;
      int dx = idx + W/2 - dX();
      int dy = idy + mode + H/3 - dY()+3;
//      Color c1 = (mode==UNAVAIL && !flagIsForTool)?CNS:currentButton?CS:CS;
//      Color c2 = (mode==UNAVAIL && !flagIsForTool)?CNS:CS;

      // Mémorisation de la position du tracé
      X=idx;
      Y=idy;
      
      // Tracé du fond
      Color c1 = Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE;
      if( mode==UP || mode==DOWN ) {
         c1 = Aladin.COLOR_CONTROL_FOREGROUND;
         if( currentButton || mode==DOWN )  c1 = c1.brighter();
      }
      
      Color bg = Aladin.COLOR_MAINPANEL_BACKGROUND;
      if( (mode==UP || mode==DOWN) && currentButton ) bg = bg.brighter();
      g.setColor( bg );
      g.fillRect(idx+2,idy+2,W-3,H-3);
      
      if( mode==DOWN ) Util.drawRect(g, idx+1, idy+1,W-2,H-2,
            Aladin.COLOR_CONTROL_FOREGROUND, Aladin.COLOR_CONTROL_FOREGROUND);

      Color cPapier = Aladin.COLOR_CONTROL_FILL_IN;
      
//      Color CBG = aladin.toolBox.getBackground();
//      Color CBU = new Color(250,249,254);
//
//      g.setColor( mode==DOWN ? CU : currentButton && mode!=UNAVAIL ? CBU : CBG);
//      g.fillRect(idx+2,idy+2,W-3,H-3);
//      if( mode==DOWN ) Util.drawRect(g, idx+1, idy+1,W-2,H-2, Color.gray, Color.gray);
//      else if( mode==UP && currentButton ) Util.drawRect(g, idx+1, idy+1,W-2,H-2, Color.gray, Color.gray);
//      else Util.drawRect(g, idx+1, idy+1,W-2,H-2,  CBG, CBG);


      //      // Couleur du fond
      //      if( mode==DOWN ) {
      //         g.setColor(CD);
      //         g.fillRect(idx+2,idy+2,W-3,H-3);
      //      } else if( mode==UP ) {
      //         g.setColor(currentButton && mode==UP ? Aladin.MYBLUE : CU);
      //         g.fillRect(idx+2,idy+2,W-3,H-3);
      //      } else {
      //         if( flagIsForTool ) g.setColor( currentButton ? Aladin.MYBLUE : CU);
      //         else g.setColor(aladin.toolbox.getBackground());
      //         g.fillRect(idx+1,idy+1,W-1,H-1);
      //      }
      //
      //      // Dessin des bords
      //      if( mode!=UNAVAIL ) {
      //
      //         Util.drawRoundRect(g, idx+1, idy+1,W-2,H-2, 4,
      //               mode==UP?Color.white:Color.darkGray, mode==UP?Color.darkGray:Color.white);
      //
      //      } else if( flagIsForTool ) {
      //         g.setColor( CS );
      //         Util.drawRoundRect(g, idx+1, idy+1,W-2,H-2, 4, CS, Color.white);
      //      }



      // Dessin du logo suivant le type d'outil
      switch(ntool) {
         case ToolBox.SELECT:                // dessin de la fleche du select
            p = setPolygon(selectX,selectY,dx,dy);
            g.setColor( mode==UNAVAIL || Aladin.DARK_THEME ? cPapier :  new Color( 230,230,230 ));
            g.fillPolygon(p);
            g.setColor( c1 );
            g.drawPolygon(p);
            break;
         case ToolBox.DRAW:                // dessin du crayon
            //            g.setColor( cPapier );
            g.setColor( mode==UNAVAIL || Aladin.DARK_THEME ? cPapier :  new Color( 215,198,142) );
            p = setPolygon(craymX,craymY,dx,dy);
            g.fillPolygon(p);
            g.setColor(c1);
            g.drawPolygon(p);
            p = setPolygon(craypX,craypY,dx,dy);
            g.fillPolygon(p);
            g.drawPolygon(p);
            for( i=0; i<craytX.length; i+=2) {
               g.drawLine(craytX[i]+dx,craytY[i]+dy,craytX[i+1]+dx,craytY[i+1]+dy);
            }
            break;
         case ToolBox.TAG:                // dessin du grand A avec le réticule
            p = setPolygon(exAX,exAY,dx,dy);
            //            g.setColor(cPapier);
            g.setColor( mode==UNAVAIL || Aladin.DARK_THEME ? cPapier : new Color( 215,198,142) );
            g.fillPolygon(p);
            g.setColor(c1);
            g.drawPolygon(p);
            p = setPolygon(bAX,bAY,dx,dy);
            g.drawPolygon(p);

            g.setColor( mode==UNAVAIL ? c1 : Aladin.COLOR_RED );
            g.drawLine(dx+4,dy+3,dx+4,dy+3+6);
            g.drawLine(dx+4-3,dy+6,dx+4+3,dy+6);

            break;
         case ToolBox.PHOT:                // le réticule entouré d'un cercle
            g.setColor(cPapier);
            g.fillOval(dx+1,dy,15,15);
            g.setColor(c1);
            g.drawOval(dx+1,dy,14,14);
            //            g.setColor(c1);
            g.setColor( mode==UNAVAIL ? c1 :  Aladin.COLOR_RED );
            g.drawLine(dx+8,dy+4,dx+8,dy+4+6);
            g.drawLine(dx+8-3,dy+7,dx+8+3,dy+7);
            break;
         case ToolBox.DIST:                // la fleche
            p = setPolygon(fmX,fmY,dx,dy);
            //            g.setColor(cPapier);
            g.setColor( mode==UNAVAIL ? cPapier : new Color( 215,198,142) );
            g.fillPolygon(p);
            g.setColor(c1);
            g.drawPolygon(p);
            //            g.setColor(cPapier);
            g.setColor( mode==UNAVAIL ? cPapier : new Color( 85,121,203) );
            for( i=0; i<cutG.length; i++) g.drawLine(dx+i+5,dy+cutG[i]-8,dx+i+5,dy+3);
            break;
         case ToolBox.DEL:                // la croix
            //            g.setColor(c1);
            p = setPolygon(crX,crY,dx,dy);
            g.setColor( mode==UNAVAIL ? c1 :  Aladin.COLOR_RED );
            g.fillPolygon(p);
            g.drawPolygon(p);
            break;
         case ToolBox.RGB:
            if( mode!=UNAVAIL ) {
               g.setColor( Aladin.COLOR_RED);   g.fillOval(dx-5,dy-3,12,12);
               g.setColor( Aladin.COLOR_GREEN); g.fillOval(dx+3,dy-3,12,12);
               g.setColor( Aladin.COLOR_BLUE);  g.fillOval(dx-1,dy+4,12,12);
            }
            g.setColor(c1);
            drawCercle(g,dx-7,dy-4);
            drawCercle(g,dx+1,dy-4);
            drawCercle(g,dx-3,dy+3);
            break;
         case ToolBox.MOC:
            g.setColor( c1 );
            for(i=0; i<MOC.length; i++) { int s[] = MOC[i]; g.drawLine(dx+s[0],dy+s[2],dx+s[1],dy+s[2]); }
            break;
         case ToolBox.BLINK:
            g.setColor(c1);
            g.fillRect(dx+1,dy+2,23,13);
            g.setColor(cPapier);
            g.setColor( mode==UNAVAIL ? cPapier : Aladin.DARK_THEME ? 
                  Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE : new Color( 236,226,181) );
            g.fillRect(dx+1,dy+5,4,7);
            g.fillRect(dx+8,dy+5,10,7);
            g.fillRect(dx+21,dy+5,3,7);
            for( i=0; i<12; i++ ) {
               g.drawLine(dx+i*2+1,dy+3,dx+i*2+1,dy+3);
               g.drawLine(dx+i*2+1,dy+13,dx+i*2+1,dy+13);
            }
            break;
         case ToolBox.XMATCH:
            for( int j=0; j<2; j++,dx+=8 ) {
               //               g.setColor(c1);
               g.setColor( mode==UNAVAIL ? c1 : new Color( 127,88,0) );
               p = setPolygon(m1X,m1Y,dx,dy);
               g.fillPolygon(p);
               g.setColor( c1 );
               g.drawPolygon(p);
               g.setColor(mode==UNAVAIL ? c1 :Aladin.COLOR_RED );
               for( i=0; i<m3X.length; i++ ) {
                  g.drawLine(dx+m3X[i][0],dy+m3X[i][2],dx+m3X[i][1],dy+m3X[i][2]);
               }
               g.setColor(cPapier);
               g.drawLine(dx+m3X[1][0]+1,dy+m3X[1][2],dx+m3X[1][1]+2,dy+m3X[1][2]);
            }
            break;
         case ToolBox.RESAMP:
            drawRsampIcon(g,c1,dx,dy,true);
            break;
         case ToolBox.PROP:
            g.setColor( mode==UNAVAIL ? c1 : new Color( 85,121,203) );
            p = setPolygon(p1X,p1Y,dx,dy);
            g.fillPolygon(p);
            g.setColor( cPapier);
            p = setPolygon(p2X,p2Y,dx,dy);
            g.fillPolygon(p);
            p = setPolygon(p3X,p3Y,dx,dy);
            g.fillPolygon(p);
            g.setColor(c1);
            g.drawLine(dx+3,dy+7,dx+3,dy+7); g.drawLine(dx+6,dy+7,dx+14,dy+7);
            g.drawLine(dx+3,dy+9,dx+3,dy+9); g.drawLine(dx+6,dy+9,dx+12,dy+9);
            break;
         case ToolBox.WEN:                // la loupe
            g.setColor( mode==UNAVAIL ? c1 : new Color( 127,88,0) );
            p = setPolygon(lpX,lpY,dx,dy);
            g.fillPolygon(p);
            g.drawPolygon(p);
            g.setColor( cPapier );
            g.fillOval(dx+3,dy+2,12,12);
            g.setColor(c1);
            drawCercle(g,dx+1,dy+1);
            g.drawLine(dx+6,dy+5,dx+7,dy+5); g.drawLine(dx+6,dy+5,dx+6,dy+6);
            break;
         case ToolBox.ZOOM:                // le Z du Zoom
            p = setPolygon(zX,zY,dx,dy);
            g.setColor( mode==UNAVAIL || Aladin.DARK_THEME ? cPapier : new Color( 215,198,142) );
            g.fillPolygon(p);
            g.setColor(c1);
            g.drawPolygon(p);
            break;
         case ToolBox.PAN:                // la main du PAN
            g.setColor( mode==UNAVAIL || Aladin.DARK_THEME ? cPapier : new Color(239,220,219) );
            for( i=0; i<pF.length; i++) g.fillRect(pF[i][0]+dx,pF[i][1]+dy,pF[i][2],pF[i][3]);
            g.setColor(c1);
            for( i=0; i<pV.length; i++ ) g.drawLine(pV[i][2]+dx,pV[i][0]+dy,pV[i][2]+dx,pV[i][1]+dy);
            for( i=0; i<pH.length; i++ ) g.drawLine(pH[i][0]+dx,pH[i][2]+dy,pH[i][1]+dx,pH[i][2]+dy);
            break;
         case ToolBox.HIST:                // l'histogramme de la table des couleurs
            //            g.setColor( c1 );
            g.setColor( mode==UNAVAIL ? c1 : new Color( 85,121,203) );
            for( i=0; i<5; i++ ) g.fillRect(cmb[i][0]+dx,cmb[i][1]+dy, cmb[i][2],cmb[i][3]);
            g.setColor( c1 );
            g.drawLine(cmX[0]+dx,cmX[1]+dy,cmX[2]+dx,cmX[3]+dy);
            g.drawLine(cmY[0]+dx,cmY[1]+dy,cmY[2]+dx,cmY[3]+dy);
            g.setColor( cPapier );
            g.setColor( mode==UNAVAIL ? cPapier : Aladin.COLOR_RED );
            for( i=0; i<cmlX.length-1; i++ )
               g.drawLine(cmlX[i]+dx,cmlY[i]+dy, cmlX[i+1]+dx,cmlY[i+1]+dy);
            break;
         case ToolBox.SPECT:                // Le spectr
            int x1,y1,x2,y2;
            int t=2;
            g.setColor( c1 );
            g.drawLine((x1=cmX[0]+dx),(y1=cmX[1]+dy),cmX[2]+dx,cmX[3]+dy);
            g.drawLine(cmY[0]+dx,cmY[1]+dy,(x2=cmY[2]+dx),(y2=cmY[3]+dy));
            g.drawLine(x1-t,y1+t,x1,y1); g.drawLine(x1+t,y1+t,x1,y1);
            g.drawLine(x2-t,y2-t,x2,y2); g.drawLine(x2-t,y2+t,x2,y2);
            g.setColor( c1 );
            for( i=0; i<sedX.length-1; i++ )
               g.drawLine(sedX[i]+dx,sedY[i]+dy, sedX[i+1]+dx,sedY[i+1]+dy);
            break;
            /*
         case ToolBox.ACE:                // Le ACE
            int t1=2;
            g.setColor( c );
            for( i=0; i<aceX.length-1; i++ ) {
               int x = aceX[i]+dx;
               int y = aceY[i]+dy;
               g.drawLine(x-t1,y,x+t1,y);
               g.drawLine(x,y-t1,x,y+t1);
            }
            break;
             */
         case ToolBox.CONTOUR:
            g.setColor( cPapier );
            g.fillRect(dx+9,dy+4,3,2);
            //            g.setColor( c1);
            g.setColor( mode==UNAVAIL ? c1 : new Color( 85,121,203));
            for( i=0; i<ctH.length; i++ )
               g.drawLine(dx+ctH[i][0],dy+ctH[i][2],dx+ctH[i][1],dy+ctH[i][2]);
            for( i=0; i<ctV.length; i++ )
               g.drawLine(dx+ctV[i][2],dy+ctV[i][0],dx+ctV[i][2],dy+ctV[i][1]);

            break;
         case ToolBox.FILTER:
            Slide.drawLogoFilter(g,dx-20,dy,mode==UP,true,c1,mode==UNAVAIL ? c1 :  new Color( 215,198,142) );
            break;
            //         case ToolBox.SYNC:
            //            g.setColor(c1);
            //            int L=8;
            //            g.drawLine(dx-L, dy,   dx-3, dy);
            //            g.drawLine(dx+3, dy,   dx+L, dy);
            //            g.drawLine(dx,   dy-L, dx,   dy-3);
            //            g.drawLine(dx,   dy+3, dx,   dy+L);
            //	        break;
         case ToolBox.CROP:
            g.setColor(c1);
            for(i=0; i<CROPN.length; i++) { int s[] = CROPN[i]; g.drawLine(dx+s[0],dy+s[2],dx+s[1],dy+s[2]); }
            g.setColor(mode==UNAVAIL ? c1 : Aladin.COLOR_RED /*new Color( 85,121,203)*/ );
            for(i=16; i<CROPN.length; i++) { int s[] = CROPN[i]; g.drawLine(dx+s[0],dy+s[2],dx+s[1],dy+s[2]); }
            g.setColor( cPapier );
            for(i=0; i<CROPB.length; i++) { int s[] = CROPB[i]; g.drawLine(dx+s[0],dy+s[2],dx+s[1],dy+s[2]); }
            break;
         case ToolBox.PLOT:
            int h=W-17;
            int w=18;
            if( mode!=UNAVAIL ) {
               g.setColor(cPapier);
               g.fillRect(dx,dy+3,w-2,h-3);
            }
            g.setColor(c1);
            g.drawLine(dx,dy,dx,dy+h); g.drawLine(dx-1,dy+1,dx+1,dy+1);
            g.drawLine(dx,dy+h,dx+w,dy+h); g.drawLine(dx+w-1,dy+h-1,dx+w-1,dy+h+1);
            g.setColor( mode==UNAVAIL ? c1 : Aladin.COLOR_RED ); //new Color( 85,121,203) );
            for( i=0; i<plot.length; i++ ) g.drawLine(plot[i][0]+dx,dy+h-plot[i][1],plot[i][0]+dx,dy+h-plot[i][1]);
            break;
      }

      // Affichage du label associe a l'icone
      if( label!=null && ntool<label.length) {
         int x,y;

         //         g.setColor(currentButton && mode==UP ? Color.white : c2);
         g.setColor(c1);

         // Positionnement du label en fonction de la presence
         // ou de l'absence d'une icone
         y = mode;
         x = 0;
         for( i=0; i<noIcone.length && ntool!=noIcone[i]; i++ );
         if( i<noIcone.length ) {
            g.setFont(Aladin.SITALIC);
            x += idx + W/2-g.getFontMetrics().stringWidth(label[ntool])/2;
            y += idy + H/2+Aladin.SIZE/2-5;
         } else {
            g.setFont(Aladin.SPLAIN);
            x += idx + W/2-g.getFontMetrics().stringWidth(label[ntool])/2;
            y += idy + H-4;
         }

         g.drawString(label[ntool],x,y);
      }
   }

   // Trace un cercle de diamètre 12
   private void drawCercle(Graphics g,int dx, int dy) {
      for( int i=0; i<C.length; i++ ) {
         g.drawLine(dx+C[i][0]+2,dy+C[i][2]+1,dx+C[i][1]+2,dy+C[i][2]+1);
         g.drawLine(dx+C[i][2]+2,dy+C[i][0]+1,dx+C[i][2]+2,dy+C[i][1]+1);
      }

   }

   /** Dessin du logo associé au Resampling (également utilisé dans
    * ViewSimple. Le flag "plein" entraine le remplissage en blanc */
   protected void drawRsampIcon(Graphics g,Color c1,int dx,int dy,boolean plein) {
      g.setColor(c1);
      Polygon p = setPolygon(r1X,r1Y,dx,dy);
      g.drawPolygon(p);
      if( plein ) g.setColor( Color.white);
      p = setPolygon(r2X,r2Y,dx,dy);
      if( plein ) {
         g.fillPolygon(p);
         g.setColor(c1);
      }
      g.drawPolygon(p);
   }

   static protected void drawVOPointer(Graphics g,int dx,int dy) {
      Polygon p = setPolygon(selectX,selectY,dx,dy);
      g.setColor(Color.white);
      g.fillPolygon(p);
      g.setColor( Color.gray );
      g.drawPolygon(p);
   }

   static protected void drawVOHand(Graphics g,int dx,int dy) {
      int i;
      g.setColor( Color.white );
      for( i=0; i<pF.length; i++) g.fillRect(pF[i][0]+dx,pF[i][1]+dy,pF[i][2],pF[i][3]);
      g.setColor(Color.gray);
      for( i=0; i<pV.length; i++ ) g.drawLine(pV[i][2]+dx,pV[i][0]+dy,pV[i][2]+dx,pV[i][1]+dy);
      for( i=0; i<pH.length; i++ ) g.drawLine(pH[i][0]+dx,pH[i][2]+dy,pH[i][1]+dx,pH[i][2]+dy);
   }

   static protected void drawVOTable(Graphics g,int dx,int dy) {
      int w=18,h=12;
      g.setColor( Color.white );
      g.fillRect(dx, dy, w, h);

      g.setColor( Color.gray);
      g.drawRect(dx,dy,w,h);
      g.drawLine(dx+1,dy+1,dx+w-2,dy+1);
      g.drawLine(dx+1,dy-1,dx+w-2,dy-1);

      for( int i=0; i<3 ; i++ ) {
         int y = dy+4+i*2;
         g.drawLine(dx+3,y,dx+3,y); g.drawLine(dx+5,y,dx+10,y); g.drawLine(dx+12,y,dx+14,y);
      }
   }
   static final int[] FRX = { 7,7-3,18+3,18 };
   static final int[] FRY = { 12,19,19,12 };

   static protected void drawVOStack(Graphics g,int dx,int dy) {
      Polygon p = new Polygon(FRX,FRY,FRX.length);
      p.translate(dx,dy);
      for( int i=2; i>=0; i-- ) {
         g.setColor( Color.white );
         g.fillPolygon(p);
         g.setColor( Color.gray );
         g.drawPolygon(p);
         g.drawLine(p.xpoints[1],p.ypoints[1]+1,p.xpoints[2],p.ypoints[2]+1);
         p.translate(0, -4);
      }
   }

}
