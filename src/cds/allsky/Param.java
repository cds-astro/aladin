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

public enum Param {
   
   in               ("dir",             "Source directory", 
         "Indicates the directory that contains the source images (FITS, PNG, JPEG (+ HHH)). "
         + "If there is only one image, the directory name can be replaced by the file name. "
         + "The source images can be directly localized in the directory or in one of its subdirectories. "
         + "In the case of MIRROR, MAP, CONCAT actions, indicate the source HiPS.",A.REQ),
   out              ("dir",             "Ouput directory",
         "Specifies the output directory. If not specified, it will be created "
         + "at the same level as the source directory using the HiPS identifier as directory name "
         + "(see id parameter).",0),
   
   order       ("nn",              "HiPS order",
         "Specifies the HiPS order. By default, the HiPS order is calculated to obtain a "
         + "resolution equivalent to, or slightly better than, the original images (based "
         + "on the reference image - see img parameter). Altering the default value is "
         + "intended to oversample, or undersample, the output HiPS. Note that each additional "
         + "order quadruples the size of the resulting HiPS.",A.HIPS),
   minOrder   ("nn",              "HiPS min order",
         "Specifies the minimum order of the HiPS. The default is 0. Altering this setting "
         + "is not recommended unless you want to generate a partial HiPS and then rebuild "
         + "the tree afterwards (see action TREE).",A.HIPS),
   frame       ("equatorial|galactic|ecliptic","HiPS coordinate frame",
         "Indicates the coordinate system of the HiPS. By default a HiPS is equatorial (ICRS) "
         + "and it is recommended to keep this value except in the case where the HiPS "
         + "is generated directly from a HEALPix map which is not equatorial.",A.HIPS),
   tileWidth  ("nn",              "HiPS tile width",
         "Specifies the size of HiPS tiles by indicating the number of pixels on one side "
         + "(necessarily a power of 2). The default is 512 (x 512) pixels. This value can "
         + "be reduced to limit null edges, especially for very \"fragmented\" HiPS (e.g. 128). "
         + "On the contrary, it can be increased to reduce the number of tiles (e.g. 1024).",A.HIPS),
   bitpix ("8|16|32|64|-32|-64", "HiPS bitpix",
         "Specifies the pixel encoding of the HiPS. Follows the FITS convention: "
         + "8, 16, 32, 64 for 8, 16, 32 or 64 bit integer encoding, -32 or -64 for 32 "
         + "(float) or 64 (double) bit real encoding. By default, uses the same encoding as "
         + "the original images (standard image - see img parameter). Reducing the number "
         + "of bits mechanically reduces the size of the final HiPS. But it also reduces "
         + "the number of possible values and introduces rounding phenomena. In this situation, "
         + "it is recommended to check the range of values selected, and if necessary modify it "
         + "(cf. dataRange parameter).",A.HIPS),
   dataRange  ("min max",         "Original pixel range",
         "Specifies the range of pixel values to be considered when performing a pixel encoding "
         + "conversion (see bitpix parameter). All values below or above will be "
         + "considered equal to the smallest or largest value respectively. By default, "
         + "this value range is automatically determined from the reference image taken from "
         + "the source images (see img parameter). The specified values are to be considered as "
         + "physical pixel values (i.e. taking into account a possible linear bzero/bscale operation "
         + "on the encoded pixel values)",A.HIPS),
   pixelCut   ("min max [fct]",    "8 bits pixel mapping",
         "Specifies the range of pixel values and how these pixels will be rendered in the "
         + "HiPS preview tiles (PNG or JPG). The pixel values between min and max are "
         + "distributed over the 255 (resp. 256) possible values using by default a linear "
         + "change, or one of the possible transfer functions (log, sqrt, asinh, pow2). The "
         + "specified values are to be considered as "
         + "physical pixel values (i.e. taking into account a possible linear bzero/bscale operation "
         + "on the encoded pixel values)",A.HIPS),
   img              ("filename",            "Reference image for default initializations",
         "Indicates the file name of the source image used as a reference. This image determines "
         + "the parameters of the final HiPS: order, pixel encoding, value ranges, etc. By default, "
         + "the first image in the source directory will be used. If it is not representative, "
         + "an alternative image is recommended.",A.HIPS),

   hdu              ("n1,n2-n3,...|all","List of FITS HDU numbers (original images)",
         "Specifies the extension numbers of the original FITS images to be considered "
         + "for HiPS generation. By default, only the first FITS extension containing a "
         + "valid image is taken into account. The first extension has the number 0.",A.IMG),
   blank            ("nn|key",          "Alternative BLANK value (or alternate BLANK fits keyword)",
         "Specifies an alternative pixel value to be considered null. This value would normally "
         + "have been specified directly in the FITS image header using the BLANK keyword. "
         + "The indicated value is to be considered at the FITS coding level, i.e. without "
         + "taking into account a possible linear bzero/bscale change.",A.IMG),
   validRange        ("min max",         "Range of valid pixels",
         "Specifies the range of valid pixels. All other values are considered null "
         + "(see `"+Param.blank+"` parameter). Note that, unlike the blank parameter, "
         + "the min and max values are to be considered as physical pixel values "
         + "(for which a linear bzero/bscale change will already have been applied). \n"
         + "This parameter should not be confused with `"+Param.dataRange+"` "
         + "(describes the range of values retained for a pixel coding conversion). "
         + "In the first case, values outside the range correspond to a transparent "
         + "(or non-existent) pixel, in the second case, the pixel is reduced to the "
         + "smallest, respectively largest possible pixel value.",A.IMG),
   skyVal           ("key|auto|%info|%min %max","Background removal method",
         "Sky background removal method. Specifies either the FITS keyword to be used to "
         + "find out the sky background value or the values to be applied for the automatic method. "
         + "In the second case, 1 value represents the percentage of the histogram of detected pixel "
         + "values to be retained; 2 values represent the min and max of the histogram to be "
         + "retained (central ex 99, or min max ex 0.3 99.7)",A.IMG),
   expTime          ("key",             "Method of adjusting the exposure time",
         "Indicates the FITS keyword associated with the exposure time of the original "
         + "images. Activates the division of the original pixels by this exposure time.",
         A.IMG),
   maxRatio         ("nn",              "Image source pixel ratio test",
         "Security filtering of source images. Specifies the maximum tolerated ratio of the angular "
         + "size of a pixel in longitude and latitude to consider a source image as correct. "
         + "By default, a ratio greater than 3 causes the image to be rejected. ",A.IMG),
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
         A.IMG),
   border           ("nn|N W S E",      "Edge removal",
         "Remove the edges of original images. This masking is constant for all images. It is "
         + "expressed either as a single value indicating the number of pixels to be ignored "
         + "on the 4 edges, or as a quadruplet N W S E indicating the number of pixels to be "
         + "ignored respectively at the top, left, bottom and right of the images. For more "
         + "advanced masking, see the `fov` parameter.",
         A.IMG),
   shape            ("rectangle|ellipse", "Image FoV",
         "Indicates the shape of the field of view of the original images. Activates "
         + "the autodetection of the pixels to be taken into account according to this shape.",
         A.IMG),
   
   mode             ("m1,m2..",         "Coadd pixel modes",
         "Pixel coaddition modes. Concerns overlays of original images ("
         + ModeOverlay.overlayMean+", "+ModeOverlay.overlayAdd+", "
         + ModeOverlay.overlayNone+", "+ModeOverlay.overlayFading+"), tile merges for HiPS "
         + "updates ("+ModeMerge.mergeOverwrite+", "+ModeMerge.mergeKeep+", "
         + ModeMerge.mergeMean+", "+ModeMerge.mergeAdd+", "+ModeMerge.mergeSub+", "
         + ModeMerge.mergeMul+", "+ModeMerge.mergeDiv+", "+ModeMerge.mergeKeepTile+", "
         + ModeMerge.mergeOverwriteTile+"), "
         + "HiPS hierarchy pixel aggregations ("+ModeTree.treeMean+", "
         + ModeTree.treeMedian+", "+ModeTree.treeFirst+"). The use of a "
         + "common suffix is valid for all associated modes (e.g. mode=add is equivalent "
         + "to mode="+ModeOverlay.overlayAdd+","+ModeMerge.mergeAdd+").",A.CTRL),
   incremental    ("false|true", "Incremental HiPS",
         "Activates the memorization of the weights assigned to each HiPS pixel in "
         + "order to be able to add new images afterwards, or concatenate two HiPS while "
         + "keeping a weighting proportional to the number of source images that contributed "
         + "to the calculation of each pixel. By default, this option is disabled because it "
         + "doubles the final size of the HiPS (generates weight tiles).",
         A.IMG),
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
         A.CTRL),
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
         A.CTRL),
   fitsKeys         ("key1,key2...",    "FITS keywords for image characteristics extraction",
         "The INDEX and DETAILS actions respectively record and provide the characteristics "
         + "of the original images (observation date, exposure time, etc). A list of common "
         + "keywords is used by default to enumerate these characteristics and store them in "
         + "the HpxFinder spatial index tiles. This parameter allows to substitute the "
         + "default list with specific keywords.",
         0),

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
         A.RGB),
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
         "Method of converting the red component pixels for an `RGB` action. Specifies "
         + "the range of original pixel values and the conversion function used (log, sqrt, "
         + "linear (default), asinh, pow2) to map them to the 256 possible colour channel values.",
         A.RGB),
   cmGreen          ("min [mid] max [fct]","Green color mapping",
         "Method of converting the green component pixels for an `RGB` action. Specifies "
         + "the range of original pixel values and the conversion function used (log, sqrt, "
         + "linear (default), asinh, pow2) to map them to the 256 possible colour channel values.",
         A.RGB),
   cmBlue           ("min [mid] max [fct]","Blue color mapping",
         "Method of converting the blue component pixels for an `RGB` action. Specifies "
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
         ,A.CTRL),
   cacheSize        ("nnMB",              "Alternative cache size limit",
         "Indicates the maximum size of the buffer (see cache parameter). By default, the "
         + "buffer is half a TB or less if the buffer partition does not allow it. "
         + "The expected unit is MB."
         ,A.CTRL),
   cacheRemoveOnExit("true|false",      "Removing cache disk control",
         "Indicates that the contents of the buffer should be preserved (or not) from one session to "
         + "the next. This allows a session to be restarted without having to decompress "
         + "images unnecessarily.",
         A.CTRL),
   
//   mocOrder         ("[s[/t]] [<nnMB[:tts]]","Specifical MOC order (MOC & STMOC action, s-spaceOrder, t-timeOrder, maxLimit, degradation rule",A.SPE),
   mocOrder         ("nn [<nnMB]",       "Specifical MOC order an/or size limit",
         "Specifies the resolution and/or size of the MOC associated with the HiPS (file `Moc.fits`). "
         + "By default the resolution of the MOC depends on the spatial distribution of the HiPS. "
         + "If this coverage results in an overly large MOC, its resolution will automatically "
         + "be degraded (but not below the order of the HiPS itself). This parameter allows "
         + "to explicitly specify an order for the MOC, and/or a volume limit.",A.SPE),
   mapNside            ("nn",              "HEALPix map NSIDE",
         "Dedicated to the MAP action. Allows you to specify a particular "
         + "NSIDE value (default 1024). For this resolution to be identical "
         + "to the original HiPS it should correspond to the formula: nside = tileWidth x 2^order. ",
         A.SPE),
   mirrorFormat           ("fmt1 fmt2 ...",    "Tile formats to copy",
         "Dedicated to the MIRROR action. Specifies the list of tile formats to be copied "
         + "(e.g. `fits jpeg` - by default all formats).",
         A.SPE),
   mirrorSplit            ("size;altPath]",   "Multi disk partition split",
         "Dedicated to the MIRROR action. Indicate one or more alternative directories needed for the "
         + "copy if the default directory does not have the required size. "
         + "For example `10g;/data/hips-ext1 200g;/data/hips-ext2` will cause the first "
         + "10GB to be copied into the default directory `out`, then 200GB into the "
         + "alternative directory /data/hips-ext1, and all the rest into /data/hips-ext2",
         A.SPE),

   pilot            ("nn",              "Pilot HiPS for testing",
         "Dedicated to generating a `pilot' HIPS for testing. Indicates the "
         + "number of original images to be considered.",
         A.CTRL),
   
   verbose          ("nn",              "Debug information",A.UNDOC),
   skyvalues        ("x1 x2 x3 x4",     "4 skyvalues",A.IMG|A.UNDOC),
 ;
   
   String synopsis;             // syntaxe d'utilisation
   String info;                 // Description courte
   String description=null;     // Description longue
   int m;                       // Caract�ristiques (voir classe A)
   
   Param(String synopsis,String info, int m ) { this.synopsis=synopsis; this.info=info; this.m=m; }
   Param(String synopsis,String info, String description, int m ) { this.synopsis=synopsis; this.info=info; this.description=description; this.m=m; }
   
   String info() { return info; }
   String synopsis() { return synopsis; }
   String description() { return Action.fold(description); }
   
   static String help(String launcher, boolean full) {
      StringBuilder s = new StringBuilder();
      for( Param a : values() ) {
         if( (a.m&(A.TEST|A.UNDOC)) !=0 ) continue;
         if( full ) s.append( "\n\n"+Action.LINE+a.fullHelp(launcher) );
         else {
            String s1 = String.format("%-20s: ",(a+"="+a.synopsis));
            s.append("   "+s1+a.info+"\n");
         }
      }
      if( full ) s.append("\n\n"+Action.LINE);
      return s.toString();
   }
   
   String fullHelp(String launcher) {
      StringBuilder s = new StringBuilder();
      s.append("PARAMETER\n   "+this+" - "+info);
      String a[] = ParamObsolete.aliases(this.toString());
      s.append("\n\nSYNOPSIS\n   "+this+"="+synopsis);
      if( a!=null ) {
         s.append("\n\nALIAS");
         for( String s1: a ) s.append("\n   "+s1);
      }
      if( description!=null ) s.append("\n\nDESCRIPTION\n   "+description());
      return s.toString();
   }
   
   public boolean equals(String s) {
      if( s==null ) return false;
      return toString().toLowerCase().equals( s.toLowerCase() );
   }
   
   static Param get(String s) throws Exception {
      for( Param p : values() ) {
         if( p.equals(s) ) return p;
      }
      throw new Exception("Param unknown");
   }
   
   class A {
      static final int REQ  = 1;        // Requis
      static final int META = 2;        // M�tadonn�e
      static final int RGB  = 4;        // D�di� � l'action RGB
      static final int SPE  = 8;        // D�di� � une action sp�cifique
      static final int XXX  = 16;       // 
      static final int HIPS = 32;       // Param�tre du HiPS
      static final int IMG  = 64;       // Gestion des images en input
      static final int CTRL = 128;      // Controle du processus de g�n�ration
      static final int UNDOC= 256;      // Param�tre non document�
      static final int TEST = 512;      // Param�tre en cours de d�veloppement
   }
}
