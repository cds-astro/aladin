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
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import cds.tools.Util;


/**
 * Formulaire graphique pour la représentation des pixels visibles dans Aladin
 * par rapport à la totalité de la plage des pixels
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (13 dec 2014)  creation
 */
public final class CanvasPixelRange extends JPanel  implements MouseMotionListener, MouseListener {

   static final private int MX = 5;    // Marges de droite et de gauche
   static final private int MY = 5;    // Marges du haut et du bas

   static final private int MINIMUM_HEIGHT = 65;   // Hauteur minimale autorisée pour le Panel
   static final private int H_HAMPE = 3;           // Taille de la partie verticale des hampes

   static final private int COLORMAP_POSX_CUTMIN = 39;              // Position X absolue du CUTMIN dans l'objet ColorMap
   static final private int COLORMAP_POSX_CUTMAX = COLORMAP_POSX_CUTMIN+256;   // Position X absolue du CUTMAX dans l'objet ColorMap

   private FrameColorMap frameColorMap;

   private Color BKGD = null;       // Couleur du fond du panel
   private PlanImage pimg;          // Image à décrire
   private boolean isFullDynamic;   // true si on est en mode Full fits, sinon en Preview
   private boolean isFitsHiPS;      // True s'il s'agit d'un plan HiPS en true Fits
   private int width,height;        // Taille du panel
   private Rectangle rectRange;     // Rectangle représentant le Range des pixels de l'image
   private Rectangle rectVisible;   // Rectangle représentant la portion visible des pixels de l'image
   private double sizeOnePixel;     // Taille d'un pixel représenté sur le graphique

   private double pixelRangeMin, pixelRangeMax;   // Valeurs du Range (BZERO et BSCALE appliqués)
   private double pixelCutMin, pixelCutMax;       // Valeurs du Cut (BZERO et BSCALE appliqués)

   private int xStart=-1;     // Lors d'un clic/drag, position initiale
   private int xMove;         // Lors d'un clic/drag, position courante
   private int mode=0;        // Mode courant (voir ci-dessous)

   static final private int IN    = 1;      // Dans rectVisible
   static final private int LEFT  = 2;      // Sur le bord gauche de rectVisible
   static final private int RIGHT = 3;      // Sur le bord droit de rectVisible

   /** Creation du graphique de la table de reponse des gris
    * @param pimg le Planimage associe
    */
   protected CanvasPixelRange(FrameColorMap frameColorMap,PlanImage pimg) {
      this.frameColorMap = frameColorMap;
      addMouseMotionListener(this);
      addMouseListener(this);
      this.pimg = pimg;
      BKGD = pimg.aladin.frameCM.getBackground();
      rectRange = new Rectangle();
      rectVisible = new Rectangle();
   }

   public Dimension getPreferredSize() {
      Dimension dim = super.getPreferredSize();
      return new Dimension( dim.width, MINIMUM_HEIGHT);
   }

   // Met à jour l'ensemble des paramètres en fonction de l'image courante
   protected void resume() {
      width = getWidth();
      height = getHeight();

      if( pimg==null ) return;

      isFullDynamic = pimg.hasAvailablePixels();
      isFitsHiPS = pimg instanceof PlanBG && ((PlanBG)pimg).isTruePixels();

      rectRange.x = MX;
      rectRange.y = MY +15;
      rectRange.width  = width-2*MX;
      rectRange.height = 17;

      if( isFitsHiPS ) { rectRange.x+=20; rectRange.width -= 2*20; }

      if( isFullDynamic ) {
         pixelRangeMin = pimg.dataMin * pimg.bScale + pimg.bZero;
         pixelRangeMax = pimg.dataMax * pimg.bScale + pimg.bZero;
         pixelCutMin   = pimg.pixelMin* pimg.bScale + pimg.bZero;
         pixelCutMax   = pimg.pixelMax* pimg.bScale + pimg.bZero;

         if( pixelCutMin<pixelRangeMin ) pixelRangeMin=pixelCutMin;
         if( pixelCutMax>pixelRangeMax ) pixelRangeMax=pixelCutMax;
      } else {
         pixelRangeMin = pixelRangeMax = 0;
         pixelCutMin = rectRange.width/4;
         pixelCutMax = 2*rectRange.width/3;
      }


      double range = pixelRangeMax-pixelRangeMin;
      sizeOnePixel = range<=0 ? 1 : rectRange.width / range;
      rectVisible.x = (int)( rectRange.x +  (pixelCutMin-pixelRangeMin) * sizeOnePixel );
      rectVisible.y = rectRange.y +1;
      rectVisible.width  = (int)( (pixelCutMax - pixelCutMin) * sizeOnePixel );
      rectVisible.height = rectRange.height -2;

      if( mode!=0 ) {
         int delta = xMove-xStart;
         switch( mode ) {
            case IN:    rectVisible.x += delta; break;
            case LEFT:  rectVisible.x +=delta; rectVisible.width-=delta; break;
            case RIGHT: rectVisible.width += delta; break;
         }
      }
   }

   protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      int x,y,ws;
      String s;
      FontMetrics fm = g.getFontMetrics();

      resume();

      // Le fond
      g.setColor(BKGD);
      g.fillRect(0,0,width,height);

      if( pimg==null ) return;

      // Le bandeau représentant la totalité des pixels possibles
      //      g.setColor( Color.white );
      //      g.fillRect( rectRange.x, rectRange.y, rectRange.width, rectRange.height );

      g.setColor( frameColorMap.cm.getColor(0) );
      g.fillRect( rectRange.x, rectRange.y, rectVisible.x-rectRange.x, rectRange.height );
      g.setColor( frameColorMap.cm.getColor(255) );
      g.fillRect( rectVisible.x+rectVisible.width, rectRange.y,
            rectRange.x+rectRange.width-rectVisible.x-rectVisible.width, rectRange.height );

      // Le bandeau représentant la portion des pixels visibles
      g.setColor( pimg.hasAvailablePixels() ? Color.cyan : Color.lightGray );
      //      g.setColor( Color.lightGray );
      g.fillRect( rectVisible.x, rectVisible.y, rectVisible.width, rectVisible.height );
      g.drawRect( rectVisible.x, rectVisible.y, rectVisible.width, rectVisible.height );

      // Le pourtour extérieur du bandeau représentant la totalité des pixels possibles
      g.setColor( Color.black );
      g.drawRect( rectRange.x, rectRange.y, rectRange.width, rectRange.height );


      // Dans le cas d'un HiPS FITS, on ajoute des ... de part et d'autres pour signifier
      // qu'il peut encore y avoir plus de valeurs de pixels
      if( isFitsHiPS ) {
         //         g.setColor( Color.lightGray );
         y = rectRange.y+ rectRange.height/2;
         x = rectRange.x-5;
         for( int i=0; i<3; i++ ) Util.fillCircle2(g, x-i*5, y);
         x= rectRange.x+rectRange.width+3;
         for( int i=0; i<3; i++ ) Util.fillCircle2(g, x+i*5, y);
      }

      // Les deux "hampes" qui relient la portion des pixels visibles à l'histogramme dans ColorMap
      int a,b;
      g.setColor( Color.lightGray );
      g.drawLine( a=rectVisible.x, b=rectVisible.y+rectVisible.height+3, a, b+H_HAMPE);
      g.drawLine( a,b+H_HAMPE, COLORMAP_POSX_CUTMIN, height-H_HAMPE);
      g.drawLine( COLORMAP_POSX_CUTMIN, height-H_HAMPE, COLORMAP_POSX_CUTMIN, height);
      g.drawLine( a=rectVisible.x+rectVisible.width, b=rectVisible.y+rectVisible.height+3, a, b+H_HAMPE);
      g.drawLine( a,b+H_HAMPE, COLORMAP_POSX_CUTMAX, height-H_HAMPE);
      g.drawLine( COLORMAP_POSX_CUTMAX, height-H_HAMPE, COLORMAP_POSX_CUTMAX, height);

      // Les valeurs pixelRange de part et d'autre du graphique
      g.setColor( Color.black);
      y = rectRange.y -2;
      s = !isFullDynamic ? "???" : Util.myRound(pixelRangeMin);
      g.drawString( s, rectRange.x, y );
      s = !isFullDynamic ? "???" : Util.myRound(pixelRangeMax);
      ws = fm.stringWidth(s);
      g.drawString(s, rectRange.x+rectRange.width+2-ws, y );

      // Les différents label
      // Le titre
      g.setColor( Aladin.MYBLUE );
      s = isFullDynamic ? frameColorMap.CMRANGE : frameColorMap.CMRANGE1;
      ws = fm.stringWidth( s );
      x = rectRange.x + rectRange.width/2 - ws/2;
      //      y = rectRange.y +rectRange.height/2 + fm.getDescent()+1;
      y = rectRange.y -3;
      g.drawString(s,x,y);

   };

   @Override
   public void mouseClicked(MouseEvent e) { }

   @Override
   public void mousePressed(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      mode=getMode(x,y);
      if( mode!=0 ) xStart=x;
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      if( !pimg.hasAvailablePixels() ) return;
      xMove=e.getX();
      resume();

      double myPixelCutMin = pixelRangeMin +  (rectVisible.x - rectRange.x) / sizeOnePixel ;
      double myPixelCutMax = myPixelCutMin +  rectVisible.width / sizeOnePixel;
      frameColorMap.setMinMax(myPixelCutMin,myPixelCutMax, true);

      xStart=-1;
      mode = 0;
      repaint();
   }

   @Override
   public void mouseEntered(MouseEvent e) { }

   @Override
   public void mouseExited(MouseEvent e) { }

   @Override
   public void mouseDragged(MouseEvent e) {
      if( !pimg.hasAvailablePixels() ) return;
      xMove=e.getX();
      resume();

      double myPixelCutMin = pixelRangeMin + rectVisible.x / sizeOnePixel ;
      double myPixelCutMax = (rectVisible.x+rectVisible.width) / sizeOnePixel;
      frameColorMap.setMinMax(myPixelCutMin,myPixelCutMax, false);

      repaint();
   }

   private int oM=-1;  // Dernier curseur utilisé

   public void mouseMoved(MouseEvent e) {
      int m = getMode( e.getX(), e.getY() );
      if( oM==m || m==0 ) return;
      switch( m ) {
         case IN:    setCursor( Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR));     break;
         case LEFT:  setCursor( Cursor.getPredefinedCursor( Cursor.E_RESIZE_CURSOR)); break;
         case RIGHT: setCursor( Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR)); break;
         default :   setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR)); break;
      }
      oM=m;
   }

   // retourne le mode courant de la souris par rapport au rectVisible
   // (dessus, sur le bord gauche ou sur le bord droit)
   private int getMode(int x, int y) {
      if( !pimg.hasAvailablePixels() ) return 0;
      int mode=0;
      if( y>rectVisible.y && y<rectVisible.y+rectVisible.height ) {
         if( Math.abs(x-rectVisible.x)<4 ) mode=LEFT;
         else if( Math.abs(x-(rectVisible.x+rectVisible.width))<4 ) mode=RIGHT;
         else if( rectVisible.contains(x, y) ) mode=IN;
      }
      return mode;
   }


}

