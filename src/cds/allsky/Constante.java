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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class Constante {

   static final public String HIPS_VERSION  = "1.3";


   // Noms des différents fichiers HiPS
   static final public String FILE_PROPERTIES  = "properties";
   static final public String FILE_HPXFINDER   = "HpxFinder";
   public static final String FILE_MOC         = "Moc.fits";
   static final public String FILE_METADATAXML = "metadata.xml";
   static final public String FILE_METADATATXT = "metadata.txt";
   
   // Les différents status HiPS
   static final public String PRIVATE      = "private";
   static final public String PUBLIC       = "public";
   static final public String MASTER       = "master";
   static final public String PARTIAL      = "partial";
   static final public String MIRROR       = "mirror";
   static final public String CLONABLE     = "clonable";
   static final public String CLONABLEONCE = "clonableOnce";
   static final public String UNCLONABLE   = "unclonable";

   // Clés utilisés dans le fichier properties
   static public final String OLD_VERSION               = "version";
   static public final String KEY_HIPS_VERSION          = "hips_version";
   static public final String KEY_HIPS_PIXEL_CUT        = "hips_pixel_cut";
   static public final String OLD_HIPS_PIXEL_CUT        = "pixelCut";
   static public final String KEY_HIPS_DATA_RANGE       = "hips_data_range";
   static public final String OLD_HIPS_DATA_RANGE       = "pixelRange";
   static public final String KEY_HIPS_PUBLISHER        = "hips_publisher";
   static public final String KEY_PUBLISHER_ID          = "publisher_id";
   static public final String KEY_CLIENT_CATEGORY       = "client_category";
   static public final String OLD_CLIENT_CATEGORY       = "category";
   static public final String KEY_CLIENT_SORT_KEY       = "client_sort_key";
   static public final String OLD_SURVEY                = "survey";
   static public final String OLD_USECACHE              = "useCache";
   static public final String KEY_HIPS_INITIAL_FOV      = "hips_initial_fov";
   static public final String OLD_HIPS_INITIAL_FOV      = "targetRadius";
   static public final String KEY_HIPS_INITIAL_RA       = "hips_initial_ra";
   static public final String KEY_HIPS_INITIAL_DEC      = "hips_initial_dec";
   static public final String OLD_TARGET                = "target";
   static public final String KEY_HIPS_TILE_WIDTH       = "hips_tile_width";
   static public final String OLD_HIPS_TILE_WIDTH       = "nside";
   static public final String KEY_DATA_COPYRIGHT        = "obs_copyright";
   static public final String OLD_DATA_COPYRIGHT        = "copyright";
   static public final String KEY_DATA_COPYRIGHT_URL    = "obs_copyright_url";
   static public final String OLD_DATA_COPYRIGHT_URL    = "copyrightUrl";
   static public final String KEY_OBS_ACK               = "obs_ack";
   static public final String KEY_PROV_PROGENITOR       = "prov_progenitor";
   static public final String KEY_HIPS_PROGENITOR_URL   = "hips_progenitor_url";
   static public final String KEY_MOC_ACCESS_URL        = "moc_access_url";
   static public final String OLD_OBS_ACK               = "acknowledgement";
   static public final String KEY_HIPS_STATUS           = "hips_status";
   static public final String KEY_PUBLISHER_DID         = "publisher_did";
   static public final String OLD_PUBLISHER_DID         = "id";
   static public final String KEY_OBS_COLLECTION        = "obs_collection";
   static public final String OLD_OBS_COLLECTION        = "label";
   static public final String KEY_OBS_TITLE             = "obs_title";
   static public final String OLD_OBS_TITLE             = "description";
   static public final String KEY_OBS_DESCRIPTION       = "obs_description";
   static public final String OLD_OBS_DESCRIPTION       = "verboseDescription";
   static public final String OLD1_OBS_DESCRIPTION      = "descriptionVerbose";
   static public final String KEY_HIPS_TILE_FORMAT      = "hips_tile_format";
   static public final String KEY_HIPS_SERVICE_URL      = "hips_service_url";
   static public final String KEY_HIPS_PIXEL_BITPIX     = "hips_pixel_bitpix";
   static public final String KEY_DATA_PIXEL_BITPIX     = "data_pixel_bitpix";
   static public final String KEY_HIPS_PROCESS_SAMPLING = "hips_sampling";
   static public final String KEY_HIPS_PROCESS_OVERLAY  = "hips_overlay";
   static public final String KEY_HIPS_PROCESS_SKYVAL   = "hips_skyval";
   static public final String KEY_HIPS_PROCESS_HIERARCHY= "hips_hierarchy";
   static public final String KEY_HIPS_ESTSIZE          = "hips_estsize";
   static public final String OLD_HIPS_TILE_FORMAT      = "format";
   static public final String KEY_HIPS_ORDER            = "hips_order";
   static public final String OLD_HIPS_ORDER            = "maxOrder";
   static public final String KEY_HIPS_ORDER_MIN        = "hips_order_min";
   static public final String OLD_HIPS_ORDER_MIN        = "minOrder";
   static public final String KEY_DATAPRODUCT_TYPE      = "dataproduct_type";
   static public final String KEY_CUBE_CRPIX3           = "data_cube_crpix3";
   static public final String KEY_CUBE_CRVAL3           = "data_cube_crval3";
   static public final String KEY_CUBE_CDELT3           = "data_cube_cdelt3";
   static public final String KEY_CUBE_BUNIT3           = "data_cube_bunit3";
   static public final String KEY_CUBE_FIRSTFRAME       = "hips_cube_firstframe";
   static public final String OLD_CUBE_FIRSTFRAME       = "cubeFirstFrame";
   static public final String KEY_CUBE_DEPTH            = "hips_cube_depth";
   static public final String OLD_CUBE_DEPTH            = "cubeDepth";
   static public final String KEY_DATAPRODUCT_SUBTYPE   = "dataproduct_subtype";
   static public final String KEY_HIPS_FRAME            = "hips_frame";
   static public final String OLD_HIPS_FRAME            = "coordsys";
   static public final String KEY_HIPS_BUILDER          = "hips_builder";
   static public final String OLD_HIPS_BUILDER          = "HiPSBuilder";
   static public final String KEY_HIPS_CREATION_DATE    = "hips_creation_date";
   static public final String OLD_HIPS_CREATION_DATE    = "firstProcessingDate";
   static public final String KEY_HIPS_RELEASE_DATE     = "hips_release_date";
   static public final String OLD_HIPS_RELEASE_DATE     = "processingDate";
   static public final String KEY_S_PIXEL_SCALE         = "s_pixel_scale";
   static public final String KEY_HIPS_PIXEL_SCALE      = "hips_pixel_scale";
   static public final String KEY_T_MIN                 = "t_min";
   static public final String KEY_T_MAX                 = "t_max";
   static public final String KEY_EM_MIN                = "em_min";
   static public final String KEY_EM_MAX                = "em_max";
   static public final String KEY_OBS_REGIME            = "obs_regime";
   static public final String KEY_BIB_REFERENCE         = "bib_reference";
   static public final String KEY_BIB_REFERENCE_URL     = "bib_reference_url";
   static public final String KEY_MOC_SKY_FRACTION      = "moc_sky_fraction";
   static public final String KEY_ADDENDUM_DID          = "addendum_did";

   static public final String KEY_HIPS_RGB_RED          = "hips_rgb_red";
   static public final String OLD_HIPS_RGB_RED          = "red";
   static public final String KEY_HIPS_RGB_GREEN        = "hips_rgb_green";
   static public final String OLD_HIPS_RGB_GREEN        = "green";
   static public final String KEY_HIPS_RGB_BLUE         = "hips_rgb_blue";
   static public final String OLD_HIPS_RGB_BLUE         = "blue";

   static public final String OLD_ALADINVERSION         = "aladinVersion";
   static public final String OLD_LAST_MODIFICATON_DATE = "lastModified";
   static public final String OLD_CURTFORMBITPIX        = "curTFormBitpix";
   static public final String OLD_NBPIXGENERATEDIMAGE   = "nbPixGeneratedImage";
   static public final String OLD_ORDERING              = "ordering";
   static public final String OLD_ISPARTIAL             = "isPartial";
   static public final String OLD_ARGB                  = "ARGB";
   static public final String OLD_TYPEHPX               = "typehpx";
   static public final String OLD_LENHPX                = "lenhpx";
   static public final String OLD_TTYPES                = "ttypes";
   static public final String OLD_TFIELDS               = "tfields";
   static public final String OLD_TILEORDER             = "tileOrder";
   static public final String OLD_NSIDE_FILE            = "nsideFile";
   static public final String OLD_NSIDE_PIXEL           = "nsidePixel";
   static public final String OLD_ISCUBE                = "isCube";
   static public final String OLD_ISMETA                = "isMeta";
   static public final String OLD_ISCAT                 = "isCatalog";
   static public final String OLD_ISCOLOR               = "isColored";

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


   static final public String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm";
   static final public SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
   static {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      sdf.setTimeZone(utc);
   }

   /** Retourne le temps passé en paramètre au format ISO8601 */
   static public String getDate() { return getDate( System.currentTimeMillis() ); }
   static public String getDate(long time) { return sdf.format(new Date(time))+"Z"; }

   /** Retourne une date passée en ISO8601 en temps */
   static public long getTime(String date) throws Exception {
      if( date.endsWith("Z")) date=date.substring(0,date.length()-1);
      return sdf.parse(date).getTime();
   }

}
