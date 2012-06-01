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


public class Constante {
   public static final int INDEX = 0;
   public static final int TESS = 1;
   public static final int JPG = 2;
   public static final String ALLSKY = "ALLSKY";
   public static String SURVEY = ALLSKY; // sous répertoire final contenant la hierarchie healpix
   public static final String HPX_FINDER = "HpxFinder";

   // Taille max d'une cellule FITS dans le cas d'une ouverture en mode Mosaic
   // => voir cds.fits.loadFits(InputStream,x,y,w,h)
   public static final int FITSCELLSIZE = 1024; 

   // Taille des imagettes HEALPix
   final static public int ORDER = 9; // 2^9 = 512 = SIDE
   final static public int SIDE = (int)Math.pow(2,ORDER);
   
   // Nombre max de mégaoctets qu'un Thread BuilberHpx est "censé" pouvoir utiliser.
// public static final int MAXMBPERTHREAD = 400;
 
   public static final int GZIPMAXORDER = 5;  // On gzippe les tiles que jusqu'au niveau 5
   public static final int MAXDEPTHINRAM = 4;
   public static int NBTILESINRAM;
   static {
      NBTILESINRAM=1;
      for( int i=1; i<=MAXDEPTHINRAM; i++ ) NBTILESINRAM+=Math.pow(4,i);
//      System.out.println("NBTILESINRAM = "+NBTILESINRAM+ " side="+SIDE);
   }

   // Nombre max de recouvrement pris en compte
   public static final int MAXOVERLAY = 6; 
}
