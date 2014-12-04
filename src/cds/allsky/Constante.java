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

package cds.allsky;

import cds.tools.pixtools.CDSHealpix;


public class Constante {
   
   public static final int INDEX = 0;
   public static final int TESS = 1;
   public static final int JPG = 2;
   public static final String HIPS = "HiPS";
   public static String SURVEY = HIPS; // sous répertoire final contenant la hierarchie healpix
   public static final String HPX_FINDER = "HpxFinder";

   // Taille max d'une cellule FITS dans le cas d'une ouverture en mode Mosaic
   // => voir cds.fits.loadFits(InputStream,x,y,w,h)

   // Taille des imagettes HEALPix
   final static public int ORDER = 9; // 2^9 = 512 = SIDE
   final static public int SIDE = (int)CDSHealpix.pow2(ORDER);
   public static final int FITSCELLSIZE = 2*SIDE; 
   
   public static final int GZIPMAXORDER = 5;  // On gzippe les tiles que jusqu'au niveau 5
   public static final int MAXDEPTHINRAM = 4;
   public static int NBTILESINRAM;
   static {
      NBTILESINRAM=1;
      for( int i=1; i<=MAXDEPTHINRAM; i++ ) NBTILESINRAM+=Math.pow(4,i);
//      System.out.println("NBTILESINRAM = "+NBTILESINRAM+ " side="+SIDE);
   }

   // Nombre max de recouvrement pris en compte
   public static final int MAXOVERLAY = 10; 
   
   // MOC ORDER minimal
   public static final int DEFAULTMOCORDER = 8;
   
   // Différence entre l'ordre nominal du survey et son MOC dans le cas d'un MOC à haute résolution
   public static final int DIFFMOCORDER = 4;
   
   // Rapport max par défaut entre la largeur et la longueur d'une image acceptable, pas testé si <0
   public static final int MAXRATIO = 10;
}
