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


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import cds.tools.FastMath;
import cds.tools.pixtools.CDSHealpix;

/**
  * Gère un losange Healpix pour un PlanBG
  * @author Anaïs Oberto + Pierre Fernique [CDS]
  */
class HealpixKeyPol extends HealpixKey {

   protected int bitpix=0;        // bitpix original (FITS uniquement)
   protected int bitpix2=0;       // bitpix original 2ème extension (FITS uniquement)
   protected byte pixelsOrigin[]=null;  // Tableau des pixels d'origine (ordre des lignes à la FITS)
   protected byte pixelsOrigin2[]=null; // Tableau des pixels de la deuxième extension (FITS uniquement)
   
   protected double deltaAngle=0; // Rotation de la polarisation en fonction du frame courant (pour tout le losange)

   protected HealpixKeyPol() {}

   protected HealpixKeyPol(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix);
   }

   protected HealpixKeyPol(HealpixKeyPol father,int child) {
      super(father,child);
      bitpix=father.bitpix;
      bitpix2=father.bitpix2;
      pixelsOrigin=null;
      pixelsOrigin2=null;
   }

   static final private int PIX8 = 0;
   static final private int PIXORIGIN = 1;
   static final private int PIXORIGIN2 = 2;

   /** Génération du tableau des pixels d'un losange issu d'une filiation en fonction
    * des pixels de son ancètre
    * @ext 0: concerne pixels[], 1: concerne pixelsOrigin[], 2:concerne pixelsOrigin2[] */
   private byte [] getPixelFromAncetre(int mode) throws Exception {
      byte [] pixAnc=null;

      switch( mode ) {
         case PIX8:
            return super.getPixelFromAncetre();
         case PIXORIGIN:
            if( bitpix==0 ) throw new Exception("No pixelsOrigin in memory !");
            pixAnc = ((HealpixKeyPol)anc).pixelsOrigin==null ? ((HealpixKeyPol)anc).getPixelFromAncetre(mode) : ((HealpixKeyPol)anc).pixelsOrigin;
            return getPixelFromAncetre1(pixAnc,Math.abs(bitpix)/8);
         case PIXORIGIN2:
            if( bitpix2==0 ) throw new Exception("No pixelsOrigin2 in memory !");
            pixAnc = ((HealpixKeyPol)anc).pixelsOrigin2==null ? ((HealpixKeyPol)anc).getPixelFromAncetre(mode) : ((HealpixKeyPol)anc).pixelsOrigin2;
            return getPixelFromAncetre1(pixAnc,Math.abs(bitpix2)/8);
      }
      return null;
   }

   private byte[] getPixelFromAncetre1(byte [] pixAnc,int npix) {
      byte [] pixels = new byte[width*height*npix];
      for( int y=0; y<width; y++) {
         for( int x=0; x<width; x++) {
            System.arraycopy(pixAnc, ( ( (y+p.y)*anc.width + (x+p.x) )*npix )  , pixels, (y*width+x)*npix, npix);
         }
      }
      return pixels;
   }

   @Override
   protected int free1() {
      int rep=super.free1();
      if( pixelsOrigin!=null ) { pixelsOrigin=null; rep=1; }
      if( pixelsOrigin2!=null ) { pixelsOrigin2=null; rep=1; }
      return rep;
   }
   

   final private double getPixVal(byte[]t ,int bitpix,int i) {
      switch(bitpix) {
         case   8: return ((t[i])&0xFF);
         case  16: i*=2;
         return ( ((t[i])<<8) | (t[i+1])&0xFF );
         case  32: i*=4;
         return  ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ;
         case -32: i*=4;
         return Float.intBitsToFloat(( ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ));
         case -64: i*=8;
         return Double.longBitsToDouble((((long)( ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ))<<32)
               | (( (((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF )) & 0xFFFFFFFFL));
      }
      return 0;
   }
   


   /** Chargement de la tuile FITS contenant pixU et pixQ en deux extensions HDU */
   protected int loadFits(String filename) throws Exception {

      byte [] buf = loadStream(filename);

      // Lecture de l'entete Fits (à la brute - elle ne doit pas dépasser 2880 catactères)
      byte [] head = new byte[2880];
      System.arraycopy(buf, 0, head, 0, 2880);
      try {
         width  = (int)getValue(head,"NAXIS1");
         height = (int)getValue(head,"NAXIS2");
         bitpix = (int)getValue(head,"BITPIX");
      } catch( Exception e ) { width=height=1024; bitpix=8; }

      // Allocation puis lecture du premier HDU pour U
      int taille=width*height*(Math.abs(bitpix)/8);
      byte [] in = new byte[taille];
      System.arraycopy(buf, 2880, in, 0, taille);
      pixelsOrigin = new byte[in.length];
      invLine(in,pixelsOrigin,bitpix);


      // Allocation puis lecture du deuxième HDU pour Q
      int pos = 2880+taille;
      int deb = (pos/2880)*2880;
      if( pos%2880!=0 ) deb+=2880;
      System.arraycopy(buf, deb, head, 0, 2880);
      try {
         bitpix2 = (int)getValue(head,"BITPIX");
         taille = width*height*(Math.abs(bitpix2)/8);
         in = new byte[taille];
         System.arraycopy(buf, deb+2880,in, 0, taille);
         pixelsOrigin2 = new byte[taille];
         invLine(in,pixelsOrigin2,bitpix2);

         pos= deb+taille+2880;
      } catch( Exception e ) {
         if( planBG.aladin.levelTrace>=3 ) {
            e.printStackTrace();
            System.err.println("Erreur à la lecture de "+filename);
         }
      }

      return pos;
   }

   private int drawPolarisationFils(Graphics g, ViewSimple v,int parente) {
      int n=0;
      if( width>=1 ) {
         fils = getChild();
         if( fils!=null ) {
            for( int i=0; i<4; i++ ) if( fils[i]!=null ) n+=((HealpixKeyPol)fils[i]).drawPolarisation(g,v,parente+1);
         }
      }
      return n;
   }

   protected double getPixRes() {
//      long nside = CDSHealpix.pow2(order+PlanHealpix.log2(width));
      return CDSHealpix.pixRes(order+(int)CDSHealpix.log2(width))/3600;
   }

   static final private int MAXPOLARSIZE=15;
   
//   static private boolean bingo=true;

   /** Trace les segments de polarisation décrits dans ce losange Healpix */
   protected int drawPolarisation(Graphics g,ViewSimple v) { return drawPolarisation(g,v,0); }
   protected int drawPolarisation(Graphics g,ViewSimple v,int parente) {
      try {
         PointD[] b = getProjViewCorners(v);
         if( b==null || b[0]==null || b[1]==null || b[2]==null || b[3]==null ) return 0;
         if( isBehindSky(b,v) ) return 0;

         if( parente<8 && mustBeDivided(b) ) {
            int  n=drawPolarisationFils(g,v,parente+1);
            resetTimer();
//            resetTimeAskRepaint();
            return n;
         }

         double nbLosange = v.getTaille()/getPixRes();
         double nbPixelParLosange = v.getWidth()/nbLosange;
         int gap=1;
         double maxSize = MAXPOLARSIZE*planBG.getSegmentDensityFactor();
         while( nbPixelParLosange<maxSize && gap*2<width ) {
            gap*=2;
            nbPixelParLosange*=2;
         }

         byte pix1[]=null,pix2[]=null;

         pix1 = parente==0 ? pixelsOrigin : getPixelFromAncetre(PIXORIGIN);
         pix2 = parente==0 ? pixelsOrigin2 : getPixelFromAncetre(PIXORIGIN2);
         
         double deltaAngle= getDeltaAngle(v);

         g.setColor(planBG.c==Color.black ? Color.green : planBG.c);
         double m=0.5;
         for( int y=gap/2; y<height; y+=gap ) {
            for( int x=gap/2; x<width; x+=gap ) {

               double fx = (x+m)/width;
               double fy = (y+m)/height;
               double x1 = b[0].x+(b[2].x-b[0].x)*fx;
               double y1 = b[0].y+(b[2].y-b[0].y)*fx;
               double x2 = b[1].x+(b[3].x-b[1].x)*fx;
               double y2 = b[1].y+(b[3].y-b[1].y)*fx;

               double x3 = x1+(x2-x1)*fy;
               double y3 = y1+(y2-y1)*fy;

               int Y=y;
               int X=x;

               int offset = Y*width+X;

               double polaQ = getPixVal(pix1,bitpix,offset);
               double polaU = getPixVal(pix2,bitpix2,offset);
               
               if( planBG.isSegmentIAUConv() ) {
//                  if( bingo ) { System.out.println("bingo IAU"); bingo=false; }
                  polaU= -polaU;
               }

               // TODO : angle si GAL, ICRS, etc (voir rose vents : ViewSimple.drawNE )
               

               // TODO : traitement des valeurs blanks ou invalides
               double angle = Math.toDegrees(0.5 * Math.atan2(polaU, polaQ));
               double norme = Math.sqrt(polaU * polaU + polaQ * polaQ);

//               double angle = getMoyenne(pix,8,X-gap/2,Y-gap/2,gap,gap);
//               double norme = getMoyenne(pix2,bitpix2,X-gap/2,Y-gap/2,gap,gap);

               // je multiplie par un facteur pour qu'on y voit qqch
               norme *= 100 * planBG.getSegmentLenFactor();
               // j'ajoute 90° pour avoir un résultat similaire à PISTOU
               // TODO : preference IAU ou HEALPIX
               angle = 90 + angle +deltaAngle;
               drawSegment(g,x3,y3,Math.toRadians(angle),norme,planBG.getSegmentThickness());
            }
         }

//         drawLosangeBorder(g,b);
         return 1;
      } catch( Exception e ) {
         return 0;
      }
   }
   
   // Calcul la variation de l'angle de polarisation en fonction du frame courant
   private double getDeltaAngle(ViewSimple v) {
      Coord corners [] = getCorners();
      if( corners==null ) return 0;
      
      Coord c = new Coord(corners[0].al,corners[0].del);
      Projection proj = v.getProj();
      proj.getXY(c);
      Coord c1 = new Coord(corners[3].al,corners[3].del);
      c1.del = c1.del+0.01>90 ? c1.del-0.01 : c1.del+0.01;
      proj.getXY(c1);
      double angle1 = Math.toDegrees( Math.atan2(c1.y-c.y, c1.x-c.x) );
      
      return Double.isNaN(angle1) ? 0 : 90+angle1;
   }
   

//   private double getMoyenne(byte [] pix,int bitpix,int x1,int y1, int w, int h) {
//
//      double moy=0;
//      for( int y=y1; y<y1+h; y++ ) {
//         for( int x=x1; x<x1+w; x++ ) {
//            int offset = y*width + x;
//            moy += getPixVal(pix, bitpix, offset);
//         }
//      }
//      moy /= w*h;
//      return moy;
//   }

   private void drawSegment(Graphics g,double x, double y, double angle, double norme, int thickness) {
      double x1 = x+norme*FastMath.cos(angle);
      double y1 = y+norme*FastMath.sin(angle);
      double dx = (x1-x)/2;
      double dy = (y1-y)/2;
      if (thickness>1 && g instanceof Graphics2D ) {
          Graphics2D g2d = (Graphics2D)g;
          Stroke saveStroke = g2d.getStroke();
          g2d.setStroke(new BasicStroke(thickness));
          g2d.drawLine((int)(x-dx),(int)(y-dy),(int)(x+dx),(int)(y+dy));
          // restore original stroke
          g2d.setStroke(saveStroke);
      }
      else {
          g.drawLine((int)(x-dx),(int)(y-dy),(int)(x+dx),(int)(y+dy));
      }
   }
}
