// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

package cds.allsky;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

/**
 * Liste des paramètres associés par les différentes actions de Hipsgen
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 *
 */
public enum Param {
   
   in               ("dir",             "Source directory", 
         "Indicates the directory that contains the source images (FITS, PNG, JPEG (+ HHH)). "
         + "If there is only one image, the directory name can be replaced by the file name. "
         + "The source images can be directly localized in the directory or in one of its subdirectories. "
         + "In the case of MIRROR, MAP, CONCAT actions, indicate the source HiPS.",0),
   out              ("dir",             "Ouput directory",
         "Specifies the output directory. If not specified, it will be created "
         + "at the same level as the source directory using the HiPS identifier as directory name "
         + "(see id parameter).",0),
   
   order       ("nn",              "HiPS order",
         "Specifies the HiPS order. By default, the HiPS order is calculated to obtain a "
         + "resolution equivalent to, or slightly better than, the original images (based "
         + "on the reference image - see img parameter). Altering the default value is "
         + "intended to oversample, or undersample, the output HiPS. Note that each additional "
         + "order quadruples the size of the resulting HiPS.",A.INDEX|A.TILES|A.MIRROR),
   minOrder   ("nn",              "HiPS min order",
         "Specifies the minimum order of the HiPS. The default is 0. Altering this setting "
         + "is not recommended unless you want to generate a partial HiPS and then rebuild "
         + "the tree afterwards (see action TREE).",A.TILES|A.DETAILS),
   frame       ("equatorial|galactic|ecliptic","HiPS coordinate frame",
         "Indicates the coordinate system of the HiPS. By default a HiPS is equatorial (ICRS) "
         + "and it is recommended to keep this value except in the case where the HiPS "
         + "is generated directly from a HEALPix map which is not equatorial.",A.INDEX),
   tileWidth  ("nn",              "HiPS tile width",
         "Specifies the size of HiPS tiles by indicating the number of pixels on one side "
         + "(necessarily a power of 2). The default is 512 (x 512) pixels. This value can "
         + "be reduced to limit null edges, especially for very \"fragmented\" HiPS (e.g. 128). "
         + "On the contrary, it can be increased to reduce the number of tiles (e.g. 1024).",A.INDEX|A.TILES),
   bitpix ("8|16|32|64|-32|-64", "HiPS bitpix",
         "Specifies the pixel encoding of the HiPS. Follows the FITS convention: "
         + "8, 16, 32, 64 for 8, 16, 32 or 64 bit integer encoding, -32 or -64 for 32 "
         + "(float) or 64 (double) bit real encoding. By default, uses the same encoding as "
         + "the original images (standard image - see img parameter). Reducing the number "
         + "of bits mechanically reduces the size of the final HiPS. But it also reduces "
         + "the number of possible values and introduces rounding phenomena. In this situation, "
         + "it is recommended to check the range of values selected, and if necessary modify it "
         + "(cf. dataRange parameter).",A.TILES),
   dataRange  ("min max",         "Original pixel range",
         "Specifies the range of pixel values to be considered when performing a pixel encoding "
         + "conversion (see bitpix parameter). All values below or above will be "
         + "considered equal to the smallest or largest value respectively. By default, "
         + "this value range is automatically determined from the reference image taken from "
         + "the source images (see img parameter). The specified values are to be considered as "
         + "physical pixel values (i.e. taking into account a possible linear bzero/bscale operation "
         + "on the encoded pixel values)",A.TILES),
   pixelCut   ("[min[%] max[%]] [byRegion[/size]] [fct]",    "8 bits pixel mapping method",
         "Specifies the range of pixel values and how these pixels will be rendered in the "
         + "HiPS preview 8 bits tiles (PNG or JPG). "
         + "The pixel values between min and max (or min% and max% of the pixel histogram) "
         + "distributed over the 255 (resp. 256) possible values using by default a linear "
         + "mapping, or one of the possible transfer functions (log, sqrt, asinh, pow2). The "
         + "specified values are to be considered as "
         + "physical pixel values (i.e. taking into account a possible linear bzero/bscale operation "
         + "on the encoded pixel values). "
         + "By default, these 2 thresholds are automatically evaluated "
         + "based on the reference image (see img parameter). "
         + "In the case of pointed observation survey, the alternative `byRegion` "
         + "indicates to Hipsgen to automatically evaluate the min% and max% thresholds "
         + "of the pixel distribution according to each observed region and no longer globally "
         + "(based on 'size' pixels of each region - default '1Mpix'). " 
         + "This evaluation by regions will be done if required by the CUT action, before "
         + "generating the PNG, JPEG or RGB tiles)",A.PNG|A.JPEG|A.RGB),
   img              ("filename",            "Reference image for default initializations",
         "Indicates the file name of the source image used as a reference. This image determines "
         + "the parameters of the final HiPS: order, pixel encoding, value ranges, etc. By default, "
         + "the first image in the source directory will be used. If it is not representative, "
         + "an alternative image is recommended.",A.INDEX|A.TILES|A.PNG|A.JPEG),

   hdu              ("n1,n2-n3,...|all","List of FITS HDU numbers (original images)",
         "Specifies the extension numbers of the original FITS images to be considered "
         + "for HiPS generation. By default, only the first FITS extension containing a "
         + "valid image is taken into account. The first extension has the number 0.",A.INDEX|A.TILES),
   blank            ("nn|key",          "Alternative BLANK value (or alternate BLANK fits keyword)",
         "Specifies an alternative pixel value to be considered null. This value would normally "
         + "have been specified directly in the FITS image header using the BLANK keyword. "
         + "The indicated value is to be considered at the FITS coding level, i.e. without "
         + "taking into account a possible linear bzero/bscale change.",A.TILES|A.PNG|A.JPEG),
   validRange        ("min max",         "Range of valid pixels",
         "Specifies the range of valid pixels. All other values are considered null "
         + "(see `"+Param.blank+"` parameter). Note that, unlike the blank parameter, "
         + "the min and max values are to be considered as physical pixel values "
         + "(for which a linear bzero/bscale change will already have been applied). \n"
         + "This parameter should not be confused with `"+Param.dataRange+"` "
         + "(describes the range of values retained for a pixel coding conversion). "
         + "In the first case, values outside the range correspond to a transparent "
         + "(or non-existent) pixel, in the second case, the pixel is reduced to the "
         + "smallest, respectively largest possible pixel value.",A.TILES),
   skyVal           ("key|auto|%info|%min %max","Background removal method",
         "Sky background removal method. Specifies either the FITS keyword to be used to "
         + "find out the sky background value or the values to be applied for the automatic method. "
         + "In the second case, 1 value represents the percentage of the histogram of detected pixel "
         + "values to be retained; 2 values represent the min and max of the histogram to be "
         + "retained (central ex 99, or min max ex 0.3 99.7)",A.TILES),
   expTime          ("key",             "Method of adjusting the exposure time",
         "Indicates the FITS keyword associated with the exposure time of the original "
         + "images. Activates the division of the original pixels by this exposure time.",
         A.TILES),
   maxRatio         ("nn",              "Image source pixel ratio test",
         "Security filtering of source images. Specifies the maximum tolerated ratio of the angular "
         + "size of a pixel in longitude and latitude to consider a source image as correct. "
         + "By default, a ratio greater than 3 causes the image to be rejected. ",A.INDEX),
   fov              ("true|x1,y1..",    "Masks on the original images.",
         "Mask activation by polygon or circle allowing to define a field of view, "
         + "or any sub-area in the original image. These masks are provided in the form of "
         + "a file with the same name as the image to which it is associated, but with the "
         + "extension `.fov'. This file contains the cartesian coordinates of the mask: one "
         + "pair of X Y coordinates (FITS convention) per line describing the polygon in a "
         + "counter-clockwise direction, or a single `X Y radius` triplet in the case of a circle. "
         + "The `.fov` file associated to a directory is applied to all the images in the "
         + "directory and its sub-directories, except for images having their own `.fov` file. "
         + "When the mask is constant for all images, it can be directly provided as a "
         + "parameter value. \n"
         + "See the `border` parameter for basic edge masking.",
         A.TILES),
   border           ("nn|N W S E",      "Edge removal",
         "Remove the edges of original images. This masking is constant for all images. It is "
         + "expressed either as a single value indicating the number of pixels to be ignored "
         + "on the 4 edges, or as a quadruplet N W S E indicating the number of pixels to be "
         + "ignored respectively at the top, left, bottom and right of the images. For more "
         + "advanced masking, see the `fov` parameter.",
         A.TILES),
   shape            ("rectangle|ellipse", "Image FoV",
         "Indicates the shape of the field of view of the original images. Activates "
         + "the autodetection of the pixels to be taken into account according to this shape.",
         A.TILES),
   
   mode             ("m1,m2..",         "Coadd pixel modes",
         "Pixel coaddition modes. Concerns overlays of original images ("
         + ModeOverlay.overlayMean+", "+ModeOverlay.overlayAdd+", "
         + ModeOverlay.overlayNone+", "+ModeOverlay.overlayFading+"), tile merges for HiPS "
         + "updates ("+ModeMerge.mergeOverwrite+", "+ModeMerge.mergeKeep+", "
         + ModeMerge.mergeMean+", "+ModeMerge.mergeAdd+", "+ModeMerge.mergeSub+", "
         + ModeMerge.mergeMul+", "+ModeMerge.mergeDiv+", "+ModeMerge.mergeKeepTile+", "
         + ModeMerge.mergeOverwriteTile+"), "
         + "HiPS hierarchy pixel aggregations ("+ModeTree.treeMean+", "
         + ModeTree.treeMedian+", "+ModeTree.treeMiddle+", "+ModeTree.treeFirst+"). The use of a "
         + "common suffix is valid for all associated modes (e.g. mode=add is equivalent "
         + "to mode="+ModeOverlay.overlayAdd+","+ModeMerge.mergeAdd+").",
         A.CTRL),
   incremental    ("false|true", "Incremental HiPS",
         "Activates the memorization of the weights assigned to each HiPS pixel in "
         + "order to be able to add new images afterwards, or concatenate two HiPS while "
         + "keeping a weighting proportional to the number of source images that contributed "
         + "to the calculation of each pixel. By default, this option is disabled because it "
         + "doubles the final size of the HiPS (generates weight tiles).",
         A.TILES),
   region           ("inline moc|moc.fits",             "Working region",
         "Reduces the working region. The syntax may be an inline ASCII MOC, "
         + "ex: `orderA/npix1 npix2-npix3 ... orderB/npix`. It indicates the HEALPix indices "
         + "of the area concerned. It can also be a file name containing a binary MOC "
         + "(.fits extension).Without this parameter, the whole sphere is processed.",
         A.CTRL),
   partitioning     ("false|nnn",       "Splitting large original images into blocks",
         "By default, the original images "
         + "are processed in blocks of 4096 x 4096 pixels. This partitioning mechanism can be "
         + "removed (value: `false`) or modified (value: block width). Small blocks save "
         + "RAM but slow down processing.",
         A.INDEX|A.TILES),
   maxThread        ("nn",              "CPU thread limitation",
         "By default, all available CPU cores are used in order to operate as fast as possible. "
         + "Limiting the number of cores allows the machine to keep power for other tasks.",
         A.CTRL),
   fastCheck ("true|false", "Mirror check method",
         "Specifies how previously copied tiles will be checked in the particular "
         + "context of a duplicate copy (`MIRROR` action). By default, the "
         + "check is fast, and is based only on a reasonable size of the tile, "
         + "with no exchange with the server providing the original HiPS. By "
         + "forcing this parameter to `false`, the check will be more complete "
         + "to ensure that the size and date is consistent, but much slower, "
         + "especially when the HiPS was almost copied entirely. Note that the "
         + "`MIRROR` action will do a CHECK action anyway if the HiPS to be copied "
         + "provides these digital keys.",
         A.MIRROR),
   fitsKeys         ("key1,key2...",    "FITS keywords for image characteristics extraction",
         "The INDEX and DETAILS actions respectively record and provide the characteristics "
         + "of the original images (observation date, exposure time, etc). A list of common "
         + "keywords is used by default to enumerate these characteristics and store them in "
         + "the HpxFinder spatial index tiles. This parameter allows to substitute the "
         + "default list with specific keywords.",
         A.INDEX),

   id               ("AUTH/P/...",              "HiPS identifier",
         "Conforms to the IVOA syntax `AUTHORITY/P/...` (e.g. CDS/P/DSS/E). The assignment "
         + "of a unique identifier is strongly recommended, and is essential when the HiPS "
         + "is distributed. The authority is usually an acronym of the institution, the separator "
         + "`/P/` is specific to `P`ixel HiPS, and the end of the identifier describes "
         + "the HiPS, usually the name of the mission, and its main feature(s) "
         + "(slash `/` is recommended as a separator rather than underscore `_`)."
         ,A.META),
   title            ("title",           "HiPS title",
         "The title of the HiPS should describe it in a few words (e.g. ZTF DR7 g). "
         + "Assigning a title is strongly recommended and will by default be built "
         + "from the identifier (see `id` parameter)."
         ,A.META),
   creator          ("name",            "HiPS creator",
         "Name of the person and/or institute that generated the HiPS."
         ,A.META),
   target           ("ra dec [rad]",  "Default HiPS target",
         "Default RA DEC display position (format: real real real (ICRS frame, unit: degrees) ."
         + "If not specified, central position of the first image (reference image)."
         ,A.META),
   targetRadius     ("radius",          "Default HiPS target radius",
         "Default display size (format: real, unit: degrees)."
         ,A.UNDOC),
   status           ("private|public [clonable|clonableOnce|unclonable]",
         "HiPS status",
         "Status of HiPS. The default is `public` for free browsing, and `clonableOnce` "
         + "to allow duplication but only from this instance. Can be changed to `private` "
         + "to restrict browsing, and `clonable` or `unclonable` to change duplication rights.",
         A.META),
   color            ("jpeg|png",        "Tile format of a colour HiPS",
         "In case the original images are jpeg or png colour files, or with the RGB action, "
         + "specify the HiPS colour format. By default, keeps the original image format.",
         A.RGB|A.TILES),
   inRed            ("hipspath",        "Red HiPS path",
         "Indicates the path of the original HiPS when using an `RGB` action to "
         + "calculate the red component.",
         A.RGB),
   inGreen          ("hipspath",        "Green HiPS path",
         "Indicates the path of the original HiPS when using an `RGB` action to "
         + "calculate the green component.",
         A.RGB),
   inBlue           ("hipspath",        "Blue HiPS path",
         "Indicates the path of the original HiPS when using an `RGB` action to "
         + "calculate the blue component.",
        A.RGB),
   cmRed            ("min [mid] max [fct]","Red color mapping",
         "Method to convert the red component pixels for an `RGB` action. Specifies "
         + "the range of original pixel values and the conversion function used (log, sqrt, "
         + "linear (default), asinh, pow2) to map them to the 256 possible colour channel values.",
         A.RGB),
   cmGreen          ("min [mid] max [fct]","Green color mapping",
         "Method to convert the green component pixels for an `RGB` action. Specifies "
         + "the range of original pixel values and the conversion function used (log, sqrt, "
         + "linear (default), asinh, pow2) to map them to the 256 possible colour channel values.",
         A.RGB),
   cmBlue           ("min [mid] max [fct]","Blue color mapping",
         "Method to convert  the blue component pixels for an `RGB` action. Specifies "
         + "the range of original pixel values and the conversion function used (log, sqrt, "
         + "linear (default), asinh, pow2) to map them to the 256 possible colour channel values.",
         A.RGB),
   luptonQ          ("x",               "Q coef Lupton RGB builder",A.RGB),
   luptonS          ("x/x/x",           "Scale coefs Lupton RGB builder",A.RGB),
   luptonM          ("x/x/x",           "M coefs Lupton RGB builder",A.RGB),
   
   cache            ("dir",             "Alternative cache directory",
         "Specifies a directory buffer instead of the one provided by the operating system. "
         + "This buffer is used for decompressing the original images when necessary. "
         + "See parameters cacheSize and cacheRemoveOnExit."
         ,A.INDEX|A.TILES),
   cacheSize        ("nnMB",              "Alternative cache size limit",
         "Indicates the maximum size of the buffer (see cache parameter). By default, the "
         + "buffer is half a TB or less if the buffer partition does not allow it. "
         + "The expected unit is MB."
         ,A.INDEX|A.TILES),
   cacheRemoveOnExit("true|false",      "Removing cache disk control",
         "Indicates that the contents of the buffer should be preserved (or not) from one session to "
         + "the next. This allows a session to be restarted without having to decompress "
         + "images unnecessarily.",
         A.INDEX|A.TILES),
   
//   mocOrder         ("[s[/t]] [<nnMB[:tts]]","Specifical MOC order (MOC & STMOC action, s-spaceOrder, t-timeOrder, maxLimit, degradation rule",A.SPE),
   mocOrder         ("nn [<nnMB]",       "Specifical MOC order an/or size limit",
         "Specifies the resolution and/or size of the MOC associated with the HiPS (file `Moc.fits`, resp. `STMoc.fits`). "
         + "By default the resolution of the MOC depends on the spatial distribution of the HiPS. "
         + "If this coverage results in an overly large MOC, its resolution will automatically "
         + "be degraded (but not below the order of the HiPS itself). This parameter allows "
         + "to explicitly specify an order for the MOC, and/or a volume limit.",
         A.TILES|A.MOC|A.STMOC),
   mapNside            ("nn",              "HEALPix map NSIDE",
         "Dedicated to the MAP action. Allows you to specify a particular "
         + "NSIDE value (default 1024). For this resolution to be identical "
         + "to the original HiPS it should correspond to the formula: nside = tileWidth x 2^order. ",
         A.MAP),
   mirrorFormat           ("fmt1 fmt2 ...",    "Tile formats to copy",
         "Dedicated to the MIRROR action. Specifies the list of tile formats to be copied "
         + "(e.g. `fits jpeg` - by default all formats).",
         A.MIRROR),
   mirrorSplit            ("size;altPath ...",   "Multi disk partition split",
         "Dedicated to the MIRROR action. Indicate one or more alternative directories needed for the "
         + "copy if the default directory does not have the required size. "
         + "For example `10g;/data/hips-ext1 200g;/data/hips-ext2` will cause the first "
         + "10GB to be copied into the default directory `out`, then 200GB into the "
         + "alternative directory /data/hips-ext1, and all the rest into /data/hips-ext2",
         A.MIRROR),

   pilot            ("nn",              "Pilot HiPS for testing",
         "Dedicated to generating a `pilot' HIPS for testing. Indicates the "
         + "number of original images to be considered.",
         A.INDEX|A.TILES),
   
   verbose          ("nn",              "Debug information",A.UNDOC),
   skyvalues        ("x1 x2 x3 x4",     "4 skyvalues",A.TILES|A.UNDOC),
 ;

   class A {
      static final int UNDOC     = 1;      // Paramètre non documenté
      static final int META      = 2;      // Métadonnée
      static final int CTRL      = 4;      // Controle du processus de génération
      static final int TEST      = 8;      // Paramètre en cours de développement
      
      static final int INDEX    = 16;      // Dédié à l'action INDEX
      static final int TILES    = 32;      // Dédié à l'action TILES
      static final int MOC      = 64;      // Dédié à l'action MOC
      static final int STMOC    =128;      // Dédié à l'action STMOC
      static final int JPEG    = 256;      // Dédié à l'action JPEG
      static final int PNG     = 512;      // Dédié à l'action PNG
      static final int RGB     =1024;      // Dédié à l'action RGB
      static final int MIRROR  =2048;      // Dédié à l'action MIRROR
      static final int MAP     =4096;      // Dédié à une action MAP
      static final int DETAILS =8192;      // Dédié à une action DETAILS
      
      static final int MAX    =65536;      // Borne MAX
   }
   
   /** Retourne l'action correspondant au mask */
   static private Action getFromMask(int mask ) {
      return mask==A.INDEX ? Action.INDEX : mask==A.TILES ? Action.TILES : mask==A.MOC ? Action.MOC
            : mask==A.PNG ? Action.PNG : mask==A.RGB ? Action.RGB : mask==A.MIRROR ? Action.MIRROR
            : mask==A.JPEG ? Action.JPEG : mask==A.STMOC ? Action.STMOC : mask==A.MAP ? Action.MAP 
            : mask==A.DETAILS ? Action.DETAILS: null;
   }
   
   /** Les champs */
   String info;                 // Description courte
   String synopsis;             // syntaxe d'utilisation
   String description=null;     // Description longue
   int m;                       // Caractéristiques (voir classe A)

   Param(String synopsis,String info, int m ) { this.synopsis=synopsis; this.info=info; this.m=m; }
   Param(String synopsis,String info, String description, int m ) { this.synopsis=synopsis; this.info=info; this.description=description; this.m=m; }

   /** Retourne la description courte */
   String info() { return info; }

   /** Retourne la syntaxe d'utilisation */
   String synopsis() { return synopsis; }

   /** Retourne la description longue, repliée en ligne de 80 caractères et 3 blancs en marge gauche */
   String description() { return Action.fold(description); }

   /** Aide en ligne correspondante au paramètre
    * @param launcher le lanceur (Hipsgen ou Aladin)
    * @param mode le mode d'affichage => Hipsgen.HTML:au format HTML
    * @return le paragraphe de l'aide en ligne
    */
   String fullHelp(String launcher,int mode) {
      boolean html = (mode&HipsGen.HTML)!=0;
      StringBuilder s = new StringBuilder();
      if( html ) {
         s.append("<B>PARAMETER  <FONT COLOR=green SIZE=+1>"+this+"</FONT></B> - "+info);
         String a[] = ParamObsolete.aliases(this.toString());
         s.append("\n<P><B>SYNOPSIS</B>   <PRE>     "+this+"="+htmlEncode(synopsis)+"</PRE>");
         if( a!=null ) {
            s.append("\n<P><B>ALIAS</B>");
            for( String s1: a ) s.append(" "+s1);
         }
         ArrayList<Action> b = listOfActions();
         if( b!=null ) {
            s.append("\n<P><B>ACTIONS</B><UL>");
            for( Action a1: b ) s.append("\n<LI>   "+a1+" - "+a1.info());
            s.append("\n</UL>");
         }
         if( description!=null ) s.append("\n<P><B>DESCRIPTION</B><P>   "+description());
      } else {
         s.append("PARAMETER\n   "+this+" - "+info);
         String a[] = ParamObsolete.aliases(this.toString());
         s.append("\n\nSYNOPSIS\n   "+this+"="+synopsis);
         if( a!=null ) {
            s.append("\n\nALIAS");
            for( String s1: a ) s.append("\n   "+s1);
         }
         ArrayList<Action> b = listOfActions();
         if( b!=null ) {
            s.append("\n\nACTIONS");
            for( Action a1: b ) s.append("\n   "+a1+" - "+a1.info());
         }
         if( description!=null ) s.append("\n\nDESCRIPTION\n   "+description());
      }
      return s.toString();
   }

   /** Surcharge de l'égalité pour ignorer la case des lettres */
   boolean equals(String s) {
      if( s==null ) return false;
      return toString().toLowerCase().equals( s.toLowerCase() );
   }
   
   /** Vérifie que le param concerne au-moins l'une des actions à exécuter
    * @param a la liste des actions
    * @return true si c'est bon, false si ce paramètre ne sera pas exploité
    */
   boolean checkActions(Vector<Action> a) {
      if( m<A.INDEX ) return true;    // non concerné par une restriction d'usage
      
      for( int mask=A.INDEX; mask<=A.MAX; mask=mask<<1) {
         if( (m&mask)==0 ) continue;
         Action b = getFromMask(mask);
         if( b==null ) continue;
         Iterator<Action> it = a.iterator();
         while( it.hasNext() ) {
            if( b==it.next() ) return true;   // Une action attendue a été trouvée
         }
      }
      return false;
   }

   /** Retourne la liste des Actions spécifiquements concernées par le paramètre */
   ArrayList<Action> listOfActions() {
      if( m<A.INDEX ) return null;    // non concerné par une restriction d'usage
      ArrayList<Action> res = null;
      for( int mask=A.INDEX; mask<=A.MAX; mask=mask<<1) {
         if( (m&mask)==0 ) continue;
         Action b = getFromMask(mask);
         if( b==null ) continue;
         if( res==null ) res = new ArrayList<>();
         res.add(b);
      }
      return res;

   }
   

   /********************************* Méthodes statiques  *********************************/

   /** Retournele paramètre correspondante à la chaine
    * @param param paramètre demandée
    * @return Le paramètre correspondant, ou exception sinon
    * @throws Exception
    */
   static Param get(String param) throws Exception {
      for( Param p : values() ) if( p.equals(param) ) return p;
      throw new Exception("Param unknown");
   }

   /**
    * Retourne l'aide en ligne pour l'ensemble des paramètres
    * @param launcher le lanceur (Hipsgen ou Aladin)
    * @param mode Le mode d'affichage 
    *                Hipsgen.FULL: un paragraphe au lieu d'une ligne
    *                Hipsgen.HTML: en codage HTML
    * @return l'aide en ligne
    */
   static String help(String launcher, int mode) {
      boolean full = (mode&HipsGen.FULL)!=0;
      boolean html = (mode&HipsGen.HTML)!=0;

      StringBuilder s = new StringBuilder();
      for( Param a : values() ) {
         if( (a.m&(A.TEST|A.UNDOC)) !=0 ) continue;
         if( html ) {
            if( full ) s.append( "\n<HR>\n"+a.fullHelp(launcher,mode) );
            else {
               String s1 = String.format("%-20s: ",(a+"="+a.synopsis));
               s.append("   "+s1+a.info+"<BR>");
            }
         } else {
            if( full ) s.append( "\n\n"+Action.LINE+a.fullHelp(launcher,mode) );
            else {
               String s1 = String.format("%-20s: ",(a+"="+a.synopsis));
               s.append("   "+s1+a.info+"\n");
            }
         }
      }
      if( full ) {
         if( html ) s.append("\n<HR>\n");
         else s.append("\n\n"+Action.LINE);
      }
      return s.toString();
   }

   /** Encodage HTML du < */
   static final private String htmlEncode(String s) {
      return s.replace("<","&lt;");
   }


}
