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

import cds.aladin.Tok;

/**
 * Liste des actions support�es par Hipsgen
 * @author Ana�s Oberto & Pierre Fernique [CDS]
 *
 */
public enum Action {

   INDEX     ("Build spatial index",
         "The INDEX action generates the spatial index needed to build the HiPS. "
               + "This spatial index is stored in a dedicated HpxFinder directory at the root of "
               + "the final HiPS. It has the same structure as a HiPS with the difference that each "
               + "tile of this index does not contain pixels but the list of original images "
               + "intersecting the tile. The format used is ASCII JSON, one record per line. "
               + "This index is eventually completed by the DETAILS action to allow access to "
               + "the characteristics of the source images when visualizing the final HiPS. \n"
               + "The main associated parameters are `order` to adjust the HiPS resolution, "
               + "`hdu` to specify the correct image in multi-extension FITS and `fitskeys` to "
               + "specifically store some characteristics of the original images. In the case "
               + "of compressed images, the `cache` parameter is used to manage the disk "
               + "buffer needed for decompression.",
         "in=/data/img out=/data/hips hdu=all INDEX"),
   TINDEX    ("Build time index (in TimeFinder directory) + TMOC index",A.TEST),
   TILES     ("Build all true value pixel tiles",
         "The TILES action generates the HiPS tiles in the target directory (see out parameter) "
               + "from the source images (see in parameter). Except in the case where the source "
               + "data are extracted from a Healpix card, the INDEX action must necessarily have "
               + "been executed previously. The transcription of the pixels depends on the nature "
               + "of the original images, and may be controlled by the `bitpix` and "
               + "`dataRange` parameters. The method used for the various pixel co-additions "
               + "also depends on various parameters, the main one being `mode`.) Finally, "
               + "the performance obtained can be adjusted by the `maxThread` and `partitioning` "
               + "parameters. \n"
               + "Pixels not taken into account will appear transparent (see `blank`, `pixelGood` "
               + "`fov` and `border` parameters). \n"
               + "The TILES action also updates the HiPS characteristics `properties` file, generates "
               + "the HiPS spatial coverage (see `MOC` action), the HiPS cover page (`index.html` file) "
               + "and, eventually, the Order3 `Allsky` file (see `ALLSKY` action).",
         "in=/data/img out=/data/hips maxThread=6 TILES"),
   PNG       ("Build PNG preview tiles",
         "The PNG action generates tiles for the preview of a HiPS. They are built from "
               + "the FITS tiles that were previously generated by the TILES action. These tiles "
               + "use the PNG format in 8 bits per pixel. The `hips_pixel_cut` parameter allows you to "
               + "specify how pixels are mapped in this value range.",
         "out=/data/hips pixelCut=\"10 800\" PNG"),
   JPEG      ("Build JPG preview tiles",
         "The JPEG action generates preview tiles in JPEG format. This is an alternative to the PNG "
               + "action. JPEG tiles are generally smaller and faster to generate than their PNG counterpart, "
               + "but do not have a transparency channel, so blank pixels will appear black and not transparent.",
         "out=/data/hips pixelCut=\"10 800\" JPG"),
   MOC       ("Regenerate the HiPS spatial coverage (MOC)",
         "The MOC action (re)generates the `Moc.fits` file describing the spatial coverage of "
               + "the HiPS. This action is rarely necessary as it is already performed by the "
               + "other actions when necessary, unless you wish to modify the resolution "
               + "of the generated MOC. The `mocorder` parameter allows to impose the order "
               + "of the Moc to be generated.",
         "out=/data/hips MOC"),
   MOCERROR  ("Build the MOC of suspected erroneous FITS tiles (TEST)",A.TEST),                   // PROTO
   MAP       ("Build an HEALPix map from the HiPS tiles",
         "The MAP action generates an HEALPix map from a HiPS. This HiPS must provide "
               + "FITS tiles. The nside parameter allows to modify the resolution "
               + "(by default the resolution of the HiPS). Be careful to indicate a "
               + "reasonable nside size (<= 4096) to avoid producing a map that is "
               + "too large to be usable.",
         "in=/data/hips out=/data/map.fits MAP"),
   MOCINDEX  ("(Re)build the index MOC (MultiOrder Coverage map) in HpxFinder directory",A.NODOC),
   MOCHIGHT  ("Build a high resolution output coverage map (MOC order=pixelRes)",A.NODOC),
   ALLSKY    ("(Re)build all Allsky files",
         "The ALLSKY action (re)generates the `Allsky' auxiliary file for all used formats"
               + " (fits, png, jpeg) for HiPS order 3 (=> in the Norder3 directory). As "
               + "this action is executed automatically and by default at the end of the tile "
               + "generation (action `TILES`), it is rarely useful individually.",
         "out=/data/hips ALLSKY"),
   GZIP      ("Compress FITS tiles",A.NODOC),                                 
   GUNZIP    ("Uncompress FITS tiles",A.NODOC),                               
   TRIM      ("Trim FITS tiles",A.NODOC),                                 
   UNTRIM    ("Untrim FITS tiles",A.NODOC),                               
   CLEAN     ("Delete all HiPS files (except properties file)",
         "The CLEAN action deletes all HiPS tiles, subdirectories and HiPS auxiliary "
               + "files except the `properties` file.",
         "out=/data/hips CLEAN"),
   CLEANALL  ("Delete all HiPS files",A.NODOC),
   CLEANINDEX("Delete spatial index",
         "The CLEANINDEX action fully deletes the spatial index (HpxFinder directory)",
         "out=/data/hips CLEANINDEX"),
   CLEANTINDEX("Delete temporal index (TimeFinder dir)",A.TEST),
   CLEANDETAILS("Delete detail index (HpxFinder tree except last order dir)",A.NODOC),
   CLEANTILES("Delete all HiPS files except index (tiles, dir, Allsky, MOC, ...)",A.NODOC),
   CLEANFITS ("Delete all FITS tiles",
         "The CLEANFITS action deletes all FITS tiles (+Allsky.fits) and any empty subdirectories.",
         "out=/data/hips CLEANFITS"),
   CLEANJPEG ("Delete all JPEG tiles",
         "The CLEANJPEG action deletes all JPEG tiles (+Allsky.jpg) and any empty subdirectories.",
         "out=/data/hips CLEANJPEG"),
   CLEANPNG  ("Delete all PNG tiles",
         "The CLEANPNG action deletes all PNG tiles (+Allsky.png) and any empty subdirectories.",
         "out=/data/hips CLEANPNG"),
   CLEANWEIGHT ("Delete all WEIGHT tiles",
         "Removes weight tiles. Weight tiles are generated by the "+Param.incremental+"=true option. "
         + "They allow HiPS updates to be made while respecting the "
         + "weighting on the pixels. Without scheduled updates, these tiles "
         + "are not needed and double the size of the HiPS unnecessarily.",
         "out=/data/hips CLEANWEIGHT"),
   CLEANDATE ("Delete all tiles older than a specifical date",A.TEST),
   TREE      ("(Re)build HiPS hierarchy from existing tiles",
         "The TREE action builds, or rebuilds, the HiPS hierarchy from the deepest order tiles. "
               + "This operation is rarely necessary as it is already done by TILES action,"
               + "unless you have modified or generated the deepest "
               + "order tiles by methods other than Hipsgen.",
         "out=/data/hips TREE"),
   APPEND    ("Append new images to an already existing HiPS",
         "The APPEND action adds new images to a pre-existing HIPS. This can be done on a "
               + "HiPS for which the original images are no longer available. The parameters "
               + "used to build the original HiPS will be used to process the new images. The "
               + "generated HiPS pixels will be added to the existing HiPS or averaged if they "
               + "are already present. This default behaviour can be modified by the `mode` parameter. "
               + "In case the original HiPS was created with the `-live` option, the average will "
               + "be weighted by the number of progenitors.",
         "in=/data/newImgs out=/data/hips APPEND"),
   CONCAT    ("Concatenate two HiPS",
         "The CONCAT action merges one HiPS into another. The two HiPS must be compatible "
               + "(same bitpix, frame, order and tile format). Common pixels are averaged. The `mode` "
               + "parameter is used to change this default behaviour. The average will be weighted "
               + "by the number of progenitors in case the HiPS to be merged have weight tiles "
               + "(option -live). Concatenation is also applied to the spatial index information "
               + "(HpxFinder). \nSee the APPEND action to perform a simple addition of images "
               + "to an existing HiPS.",
         "in=/data/hipsToAdd out=/data/hips CONCAT"),
   CUBE      ("Create a HiPS cube based on a list of HiPS",
         "The CUBE action generates a HiPS cube from 2 or more pre-existing HiPS. These HiPS "
               + "must be compatible (same bitpix, frame, order and format). They are indicated by means "
               + "of the `in` parameter using the semicolon as separator. Tiles from the original "
               + "HiPS are copied into the target HiPS cube. The `mode=link` parameter allows to "
               + "change this default behaviour by using relative symbolic links on the original tiles.",
         "in=/data/hips0;...;/data/hipsN out=/data/hips CUBE"),
   DETAILS   ("Extends HiPS spatial index for supporting 'progenitor' facility",
         "The DETAILS action extends the spatial index generated by the INDEX action to make "
               + "accessible the characteristics of each original image (date of observation, "
               + "exposure time, etc), and even to provide direct links to these images. This "
               + "action generates a hierarchy of metadata tiles, directly in the HpxFinder "
               + "spatial index directory, and sets up a mapping file `metada.xml`. Editing "
               + "this file allows you to adjust the features to be made accessible.",
         "out=/data/hips DETAILS"),
   STMOC     ("Build a STMOC.fits based on HpxFinder tile descriptions",
         "Generates a file `STMoc.fits` at the root of the HiPS. This describes the "
         + "space-time coverage of the HiPS. It is based on the information collected "
         + "during the INDEX action to determine the observation date and duration associated "
         + "with each HiPS tile (takes into account all images contributing to the tile).",
         "out=/data/hips STMOC"),
   UPDATE    ("Upgrade HiPS metadata additionnal files to the last HiPS standard",
         "If necessary, the UPDATE action upgrades the HiPS designated by the `out` parameter to be compliant "
               + "with the IVOA HiPS 1.0 standard (=hips_version 1.4). This operation also may update the "
               + "compliance codes (DATASUM and `hips_check_code`) and the `hips_nb_tiles` and "
               + "`hips_extsize` metrics.",
         "out=/data/hips UPDATE"),
   CHECKCODE ("Compute and store the check codes",
         "The CHECKCODE action calculates the numerical check codes allowing to verify a posteriori "
               + "that the number and size of the tiles are unchanged (see CHECK action). These codes "
               + "are stored in the `properties` file in the `hips_check_codes` field. The "
               + "`hips_nb_tiles` and `hips_estsize` fields are also updated. If previous values "
               + "have already been stored, it is necessary to use the `-clean` option to force "
               + "their replacement.",
         "out=/data/hips CHECKCODE"),
   UPDATEDATASUM  ("Add/update DATASUM in all FITS HiPS tiles",
         "The UPDATEDATASUM action calculates and updates the integrity check codes of the FITS "
               + "tiles (DATASUM). This action is rarely needed, and it is often better "
               + "to use the UPDATE action",
               "out=/data/hips UPDATEDATASUM",A.NODOC),
   PROP      ("Display HiPS properties files in HiPS version "+Constante.HIPS_VERSION+" syntax",A.NODOC), // DEBUG
   MIRROR    ("Duplication of a HiPS (local or remote)",
         "The MIRROR action makes a copy of a HiPS, local or remote. The `in` parameter specifies "
               + "the directory of the HiPS or its URL for remote copy. The copy will be total by default "
               + "or partial using the `format` parameter to specify the format(s) of the tiles involved "
               + "(fits, png or jpg), and/or the `order` parameter to restrict the order of the "
               + "copied HiPS, and/or `region` to consider only a spatial portion of the HiPS. \n"
               + "The `split` parameter allows the resulting HiPS to be spread over several directories. "
               + "In the case of a remote copy, the -nice option deliberately slow down "
               + "the copy to avoid overloading (and possibly being banned) from the server. \n"
               + "When resuming an interrupted copy, the -nocheck option accelerate and simplify the verification "
               + "of the already copied tiles (do not compare the dates and sizes).",
         "in=http://remote/hips out=/data/hips MIRROR"),
   RGB       ("Build and RGB HiPS based on 2 or 3 other HiPS",
         "The RGB action generates a colour HiPS from 3 greyscale HiPS, each of which will "
               + "be used for one of the red, green and blue colour components. The parameters "
               + "inRed, inGreen, inBlue designate the original HiPS. Two methods can be used. "
               + "1 - The classic method which uses the parameters cmRed, cmGreen and cmBlue by "
               + "explicitly specifying the pixel mapping of each HiPS. 2 - The Lupton method which "
               + "uses the parameters luptonM, luptonS and luptonQ. The omission of a HiPS will "
               + "be handled by using the average of the values of the other two.",
         "inRed=hips1 inGreen=hips2 inBlue=hips3 out=hipsRGB RGB"),
   CHECK     ("Basic HiPS integrity check",
         "The CHECK action controls the conformity of the security codes stored in the `properties' file "
               + "to the tiles present in the HiPS. This is a fast test to check that no tile has been "
               + "deleted or modified, for example after a HiPS copy (see MIRROR action). For a more elaborate "
               + "check, see the CHECKDATASUM action.",
         "out=/data/hips CHECK"),
   CHECKDATASUM ("HiPS FITS tiles full integrity check",
         "The CHECKDATASUM action checks the integrity of FITS tiles. When generating tiles with "
               + "a recent version of Hipsgen (>12.010), or as a result of the UPDATEDATASUM action, "
               + "each FITS tile has a numerical code (DATASUM) in its header linked to its content. "
               + "CHECKDATASUM recalculates the numerical code of each tile and verifies that "
               + "it corresponds to the one that was stored in the header. This operation requires a "
               + "lot of computing time, depending on the context, it may be sufficient to check the "
               + "integrity of a HiPS only by the size and number of tiles (see CHECK action), "
               + "This operation requires a lot of computing time, depending on the context, "
               + "it may be sufficient to check the integrity of a HiPS only by the size and number of "
               + "tiles (cf. CHECK action) or only by a few tiles chosen randomly (cf. CHECKFAST action).",
         "out=/data/hips CHECKDATASUM"),
   CHECKFAST ("Fast HiPS integrity test",
         "The CHECKFAST action tests the integrity of a HiPS by checking the compliance of "
               + "some randomly designated tiles. This action is a fast alternative to the CHECKDATASUM "
               + "action, and allows to detect a possible corruption of a HiPS following the activation "
               + "of a ransomware.",
         "out=/data/hips CHECKFAST"),
   LINT      ("Check HiPS IVOA 1.0 standard compatibility",
         "The LINT action checks that the HiPS specified by the out parameter is compliant with "
               + "the IVOA HiPS 1.0 standard. If not, it indicates the non-conforming elements."),
   ZIP       ("ZIP HiPS directories",A.TEST),                                                     // PROTO
   //   INFO      ("Generate properties and index.html information files"),
   MAPTILES  ("Build HiPS tiles from a HEALPix FITS map",
         "The MAPTILES action generates the FITS tiles and the HiPS hierarchy from a "
               + "HEALPix map. This map must be specified with the `in` parameter and replaces "
               + "the source images. The resulting HiPS takes the same coordinate frame and pixel "
               + "coding as the HEALPix map (see `frame` parameter). Thus, by default the HiPS pixel "
               + "values will be strictly identical to those of the original map (no resampling, "
               + "no coding changes). The HEALPix map can be expressed in RING or NESTED coding. "
               + "It can have implicit or explicit addressing.",
         "in=/data/map.fits out=/data/hips MAPTILES"),
   FINDER,PROGEN,                                                                          // DEPRECATED
   ABORT, PAUSE, RESUME,

   SMOC      ("Build a SMOC.fits based on HpxFinder tile descriptions (TEST)",A.TEST),            // PROTO
   TMOC      ("Build a TMOC.fits based on HpxFinder tile descriptions (TEST)",A.TEST),            // PROTO

   VALIDATOR ("Global Hipsgen validator (TEST)",A.NODOC),                                          // PROTO
   CUT ("Pixel cut evaluation by regions",
         "Computation of the range of pixel values to be displayed in 8-bit mode, specific "
         + "to each observation region. Stored in the FITS tile headers under the keywords "
         + "'CUTMIN' and 'CUTMAX'. Taken into account for PNG, JPEG and RGB actions "
         + "with the 'pixelcul=byRegion' parameter.",
         "out=/data/hips CUT",
         A.TEST);  // PROTO

   /** Liste des actions effectu�es par d�faut */
   static final Action[] DEFAULT = { INDEX, TILES, PNG, CHECKCODE, DETAILS };


   /** Diff�rents modes pour les actions (choix multiples) */
   class A {
      static final int NODOC=1;   // Non document�
      static final int TEST=2;    // En phase de d�veloppement
   }

   /** Les champs */
   private String info;           // Courte description
   private String description;    // Longue description
   private String example;        // Un example d'utilisation
   private int m=0;               // mode : voir classe A
   public long startTime=0L;      // date du d�but de l'action
   public long stopTime=0L;       // date de fin de l'action

   Action() { m=A.NODOC; }
   Action(String s ) { this(s,null,null,0); }
   Action(String s,int m ) { this(s,null,null,m); }
   Action(String s,String description ) { this(s,description,null,0); }
   Action(String s,String description,String example ) { this(s,description,example,0); }
   Action(String s,String description,String example,int m ) { 
      info=s; 
      this.description=description; 
      this.example=example;
      this.m=m; 
   }

   /** Retourne l'explication courte */
   String info() { return info; }

   /** Retourne l'explication longue, 
    * repli�e sur 80 caract�res et avec 3 carat�res de marge � gauche */
   String description() { return fold(description); }

   /** Aide en ligne correspondante � l'action
    * @param launcher le lanceur (Hipsgen ou Aladin)
    * @param mode le mode d'affichage => Hipsgen.HTML:au format HTML
    * @return le paragraphe de l'aide en ligne
    */
   String fullHelp(String launcher, int mode) {
      boolean flagHtml = (mode&HipsGen.HTML)!=0;
      StringBuilder s = new StringBuilder();
      if( flagHtml ) {
         s.append("<B>ACTION <FONT COLOR=blue SIZE=+1>"+this+"</FONT></B> - "+info);
         if( description!=null ) s.append("\n<P><B>DESCRIPTION</B><P>   "+description());
         if( example!=null ) s.append("\n<P><B>EXAMPLE</B><PRE>\n   java -jar "+launcher+".jar "+example+"\n</PRE>\n");
      } else {
         s.append("ACTION\n   "+this+" - "+info);
         if( description!=null ) s.append("\n\nDESCRIPTION\n   "+description());
         if( example!=null ) s.append("\n\nEXAMPLE\n   java -jar "+launcher+".jar "+example);
      }
      return s.toString();
   }

   /** Surcharge de l'�galit� pour ignorer la case des lettres */
   boolean equals(String s) {
      if( s==null ) return false;
      return toString().toLowerCase().equals( s.toLowerCase() );
   }

   /** M�morise la date de d�marrage de l'action */
   void startTime() { startTime=System.currentTimeMillis(); }

   /** M�morise la date de fin de l'action */
   void stopTime()  { stopTime=System.currentTimeMillis(); }

   /** Retourne la dur�e de l'action en ms (m�me si elle n'a pas encore termin�e) */
   long getDuration() {
      return (stopTime==0 ? System.currentTimeMillis():stopTime)-startTime;
   }

   /********************************* M�thodes statiques  *********************************/

   /** Retourne l'Action correspondante � la chaine
    * @param action action demand�e
    * @return L'action correspondante, ou exception sinon
    * @throws Exception
    */
   static Action get(String action) throws Exception {
      for( Action p : values() ) if( p.equals(action) ) return p;
      throw new Exception("Action unknown");
   }

   /** Retourne la liste des actions effectu�es par d�faut, s�par�es par un espace */
   static String defaultList() {
      StringBuilder s = new StringBuilder();
      for( Action a: DEFAULT ) {
         if( s.length()>0 ) s.append(" ");
         s.append(a.toString());
      }
      return s.toString();
   }

   /**
    * Retourne l'aide en ligne pour l'ensemble des actions
    * @param launcher le lanceur (Hipsgen ou Aladin)
    * @param mode Le mode d'affichage 
    *                Hipsgen.FULL: un paragraphe au lieu d'une ligne
    *                Hipsgen.HTML: en codage HTML
    * @return l'aide en ligne
    */
   static String help(String launcher, int mode) {
      boolean flagHtml = (mode&HipsGen.HTML)!=0;
      boolean flagFull = (mode&HipsGen.FULL)!=0;
      StringBuilder s = new StringBuilder();
      for( Action a : values() ) {
         if( (a.m&(A.TEST|A.NODOC)) !=0 ) continue;
         if( flagHtml ) {
            if( flagFull ) s.append( "\n<HR>\n"+a.fullHelp(launcher,mode) );
            else {
               String s1 = String.format("%-13s: ", a.toString());
               s.append("   "+s1+a.info()+"<BR>\n");
            }
         } else {
            if( flagFull ) s.append( "\n\n"+LINE+a.fullHelp(launcher,mode) );
            else {
               String s1 = String.format("%-13s: ", a.toString());
               s.append("   "+s1+a.info()+"\n");
            }
         }
      }
      if( flagFull ) {
         if( flagHtml ) s.append("\n<HR>\n");
         else s.append("\n\n"+LINE);
      }
      return s.toString();
   }


   /********************************** Utilitaires ****************************************/

   /** Repliage d'une chaine */
   static public String fold(String s) { return fold(s,"   ",78); }

   /** Repliage d'une chaine
    * @param s La chaine � replier
    * @param prefix un �ventuel pr�fixe � ajouter en d�but de chaque ligne
    * @param size Le nombre max de caract�res par ligne
    * @return la chaine repli�e
    */
   static public String fold(String s,String prefix, int size) {
      StringBuilder s1 = new StringBuilder();
      int taille=0,i;
      Tok tok = new Tok(s," ");
      while( tok.hasMoreTokens() ) {
         String w=tok.nextToken();
         if( (i=w.indexOf('\n'))>=0 ) {
            w = w.substring(0,i)+"\n"+prefix+w.substring(i+1);
            taille=size;
         }
         if( taille+w.length()>size ) { 
            s1.append("\n"+prefix+w); 
            taille=prefix.length()+w.length();
         } else { 
            if( s1.length()>0 ) {s1.append(' '); taille++; }
            s1.append(w); taille+=w.length(); 
         }
      }
      return s1.toString();
   }

   // Une ligne de tirets
   static public String LINE = "--------------------------------------------------------------------------------\n";


}
