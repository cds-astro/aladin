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

/**
 * 
 */
package cds.allsky;

import cds.aladin.Calib;
import cds.fits.Fits;

class DownFile {
	Fits fitsfile;
	Calib calib;
	
	DownFile() { }
	
	static final int PROP=6;        // Proportion pour le fadding
	static final int BORD=768;
	static final int W = 23040;     // Taille de la plaque (carrée)
	static final int R = W/2;       // Rayon de la plaque
	static final int R23 = (int)(2.*R/3);
	static final int MIN=BORD;    // Marge de la plaque
	static final int MAX=W-BORD;
	static final int R2 = (R+R/8)*(R+R/8);
    static final int MAXXCARTOUCHE = (int)( W - (1.27/6.38)*W );
    static final int MINYCARTOUCHE = (int)( (1/6.37)*W );
    double cnpix1=-1,cnpix2=-1;
    int nPlaque=-1;
    
    @Override
	public String toString() {
		return fitsfile.getFilename().toString();
	}

	// Récupère la position dans la plaque DSS
    void initDSS() {
       if( cnpix1!=-1 ) return;
       try {
          cnpix1 = fitsfile.headerFits.getDoubleFromHeader("CNPIX1");
          cnpix2 = fitsfile.headerFits.getDoubleFromHeader("CNPIX2");
          String s = fitsfile.headerFits.getStringFromHeader("REGION");
          nPlaque = Integer.parseInt(s.substring(2));
       } catch( Exception e ) { e.printStackTrace(); }
    }
    
    int getDSSPlaque() { return nPlaque; }
    
    // Vérifie qu'on est dans le cercle centré sur le milieu de la plaque
    // d'origine
    boolean inDSS(double x, double y) {
       initDSS();
       return true;
       
//       x += cnpix1;
//       y += cnpix2;
//       boolean in = x>MIN && y>MIN && x<MAX && y<MAX                  // Pas sur le bord
//                   && !(x>MAXXCARTOUCHE && y<MINYCARTOUCHE );         // Pas dans le cartouche
//       if( !in ) return false;
//       x-=R;
//       y-=R;
//       return x*x + y*y < R2;  // Pas dans les coins
    }
    
    // Rectifie la valeur du pixel DSS s'il est sur le bord
//    double getDSSCorrection(double pix, double x, double y) {
//       initDSS();
//       x += cnpix1 -R;
//       y += cnpix2 -R;
//       double d= Math.sqrt(x*x+y*y);
//       double d1 = d/R;
//       return pix+ d1*d1 * 600;
//    }
    
    // Rectifie la valeur du pixel DSS par une méthode bilinéaire + une correction proportionnelle au carré de la distance au centre
    double getDSSCorrection(double pix, double x, double y, double [] min) {
       initDSS();
       x += cnpix1;
       y += cnpix2;
       
       double c1 = min[0] + (min[3]-min[0]) * ((W-y)-W/4.)/(3*W/4.);
       double c2 = min[1] + (min[2]-min[1]) * ((W-y)-W/4.)/(3*W/4.);
       double c3 = c1 + (c2-c1) * (x-W/4.)/(3*W/4.);  
       pix -= c3;
       
       double a = min[4] - min[5];
       x -= R;
       y -= R;
       double d= Math.sqrt(x*x+y*y);
       double d1 = d/R;
       pix += d1*d1 * a;
       
       pix -= min[4];
       return pix;
     }
    
    // Retourne le coefficient de Fading pour une plaque DSS
    double getDSSFading(double x, double y) {
       initDSS();
       if( x<0 ) x=0;
       if( y<0 ) y=0;
       
       // Bords de plaque
       int w=W-2*BORD;
       x-=BORD;
       y-=BORD;
       double m = (double)w/PROP;
       double coefx=1, coefy=1;
       if( x<m ) coefx =  x/m;
       else if( x>w-m ) coefx = (w-x)/m;
       if( y<m ) coefy =  y/m;
       else if( y>w-m ) coefy = (w-y)/m;
       double fad1 = Math.min(coefx,coefy);
       return fad1;
       
//       x+=BORD;
//       y+=BORD;
       
//       // Bords du cartouche
//       coefx=coefy=1;
//       if( x>MAXXCARTOUCHE-m && x<=MAXXCARTOUCHE  && y<=MINYCARTOUCHE ) coefx = (MAXXCARTOUCHE-x)/m;
//       if( y>=MINYCARTOUCHE  && y<MINYCARTOUCHE+m && x>=MAXXCARTOUCHE ) coefy = (y-MINYCARTOUCHE)/m;
//       double fad3 = Math.min(coefx,coefy);
//       
//       // Coin du cartouche
//       if( MAXXCARTOUCHE-m<x && x<=MAXXCARTOUCHE  && y>=MINYCARTOUCHE  && y<MINYCARTOUCHE+m ) {
//          double xc = MAXXCARTOUCHE-x;
//          double yc = y-MINYCARTOUCHE;
//          double r = (int)Math.sqrt(xc*xc + yc*yc);
//          fad3 = Math.min(fad3,r/m);
//       }
       
       
//        Distance au centre
//       x -= R;
//       y -= R;
//      double fad2 = 1 - (x*x+y*y)/R2;
//      
//      return Math.min(fad1,fad2);
//      return Math.min(fad3, Math.min(fad1,fad2));
    }
    

}