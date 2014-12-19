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
   
   // Noms des différents fichiers HiPS
   static final public String FILE_PROPERTIES  = "properties";
   static final public String FILE_HPXFINDER   = "HpxFinder";
   public static final String FILE_MOC         = "Moc.fits";
   static final public String FILE_METADATAXML = "metadata.xml";
   static final public String FILE_METADATATXT = "metadata.txt";
   
   // Clés utilisés dans le fichier properties
   static public final String KEY_CUBEFIRSTFRAME        = "cubeFirstFrame";
   static public final String KEY_CUBEDEPTH             = "cubeDepth";
   static public final String KEY_ISCUBE                = "isCube";
   static public final String KEY_PIXELCUT              = "pixelCut";
   static public final String KEY_PIXELRANGE            = "pixelRange";
   static public final String KEY_PUBLISHER             = "publisher";
   static public final String KEY_CATEGORY              = "category";
   static public final String KEY_VERSION               = "version";
   static public final String KEY_SURVEY                = "survey";
   static public final String KEY_USECACHE              = "useCache";
   static public final String KEY_TARGETRADIUS          = "targetRadius";
   static public final String KEY_TARGET                = "target";
   static public final String KEY_NSIDE                 = "nside";
   static public final String KEY_COPYRIGHT_URL         = "copyrightUrl";
   static public final String KEY_COPYRIGHT             = "copyright";
   static public final String KEY_ACK                   = "acknowledgement";
   static public final String KEY_DESCRIPTION_VERBOSE   = "verboseDescription";
   static public final String KEY_DESCRIPTION           = "description";
   static public final String KEY_LABEL                 = "label";
   static public final String KEY_FORMAT                = "format";
   static public final String KEY_MINORDER              = "minOrder";
   static public final String KEY_MAXORDER              = "maxOrder";
   static public final String KEY_ISMETA                = "isMeta";
   static public final String KEY_ISCAT                 = "isCatalog";
   static public final String KEY_ISCOLOR               = "isColored";
   static public final String KEY_COORDSYS              = "coordsys";
   static public final String KEY_HIPSBUILDER           = "HiPSBuilder";
   static public final String KEY_ALADINVERSION         = "aladinVersion";
   static public final String KEY_CURTFORMBITPIX        = "curTFormBitpix";
   static public final String KEY_NBPIXGENERATEDIMAGE   = "nbPixGeneratedImage";
   static public final String KEY_ORDERING              = "ordering";
   static public final String KEY_ISPARTIAL             = "isPartial";
   static public final String KEY_ARGB                  = "ARGB";
   static public final String KEY_TYPEHPX               = "typehpx";
   static public final String KEY_LENHPX                = "lenhpx";
   static public final String KEY_TTYPES                = "ttypes";
   static public final String KEY_TFIELDS               = "tfields";
   static public final String KEY_TILEORDER             = "tileOrder";
   static public final String KEY_NSIDE_FILE            = "nsideFile";
   static public final String KEY_NSIDE_PIXEL           = "nsidePixel";
   static public final String KEY_LAST_MODIFICATON_DATE = "lastModified";
   static public final String KEY_FIRST_PROCESSING_DATE = "firstProcessingDate";
   static public final String KEY_PROCESSING_DATE       = "processingDate";
   static public final String KEY_SIZERECORD            = "sizeRecord";
   static public final String KEY_OFFSET                = "offset";
   static public final String KEY_GZ                    = "gzipped";
   static public final String KEY_LOCAL_DATA            = "localData";
   static public final String KEY_ORIGINAL_PATH         = "dataPath";

   // Numéro des fomulaires lors de l'Hipselisation via GUI
   static public final int PANEL_INDEX       = 0;
   static public final int PANEL_TESSELATION = 1;
   static public final int PANEL_PREVIEW     = 2;
   
   static public final String HIPS = "HiPS";
   static public String SURVEY = HIPS;  // sous répertoire final contenant la hierarchie healpix

   static public final int ORDER           = 9;    // Taille des imagettes HEALPix
   static public final int ORIGCELLWIDTH   = 1024; // Taille des cellules des images originales lors de la Hipselisation 
   static public final int GZIPMAXORDER    = 5;    // On gzippe les tiles que jusqu'au niveau 5
   static public final int MAXOVERLAY      = 10;   // Nombre max de recouvrement pris en compte
   static public final int DEFAULTMOCORDER = 8;    // MOC ORDER minimal
   static public final int DIFFMOCORDER    = 4;    // Différence entre l'ordre nominal du survey et son MOC dans le cas d'un MOC à haute résolution
   static public final int PIXELMAXRATIO   = 2;    // Rapport max par défaut entre la largeur et la longueur d'une image acceptable, pas testé si <0
   
   // Zone d'observation dans les images originales lors de lHipselisation (tout, ellipsoïde, ou rectangulaire)
   static final public int SHAPE_UNKNOWN     = 0;
   static final public int SHAPE_ELLIPSE     = 1;
   static final public int SHAPE_RECTANGULAR = 2;
   
   // Modes supportés pour les tuiles
   static final public int TILE_PNG=0;
   static final public int TILE_JPEG=1;
   static final public int TILE_FITS=2;
   static final public String [] TILE_EXTENSION = { ".png",".jpg", ".fits" };
   static final public String [] TILE_MODE      = { "png", "jpeg", "fits" };
   
}
