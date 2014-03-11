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

import cds.tools.*;

import cds.image.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.Inflater;

/**
 * Plan dedie a une image (IMAGE)
 *
 * @author Pierre Fernique [CDS]
 * @version 2.2 : 22 février 2004 Gestion d'un cache pour les vrais pixels
 * @version 2.1 : 3 octobre 2003 Conservation des vrais pixels
 *                         + gestion du vieux serveur d'image Aladin
 * @version 2.0 : 2 sept 2002 lecture sur un unique tableau des images
 * @version 1.8 : 14 mars 2002 autocropping implante
 * @version 1.7 : 8 mars 2001 Bidouillage pour EROSI
 * @version 1.6 : 10 octobre 2000 Bidouillage pour DENIS et 2MASS
 * @version 1.5 : 19 juin 00 Utilisation du PushbackInputStream
 * @version 1.4 : 26 mai 00 Gestion de la decompression hcompress
 * @version 1.3 : 10 nov 99 Gestion de la decompression mrcomp
 * @version 1.2 : 26 mai 99 Chargement d'une image locale (FITS)
 * @version 1.1 : 19 mai 99 Implantation de la classe HeaderFits
 * @version 1.0 : 5 mai 99 Toilettage du code
 * @version 0.9 : (??) creation
 */
public class PlanImage extends Plan {
   
   static protected final int PIX_ARGB = 0;    // FITS ARGB, PNG couleur   => couleur avec transparence
   static protected final int PIX_RGB  = 1;    // FITS RGB, JPEG couleur   => Couleur sans transparence
   static protected final int PIX_TRUE = 2;    // FITS => vraie valeur (définie par le BITPIX) => transparence sur NaN ou BLANK
   static protected final int PIX_256  = 3;    // JPEG N&B => 256 niveaux
   static protected final int PIX_255  = 4;    // PNG N&B => 255 niveaux - gère la transparence
   
   static final public String [] PIX_MODE = { "RGB composite & transparency", "RGB composite", "true pixel mode & transparency", 
                                              "256 grey levels","255 grey levels & transparency" };
   
   protected int pixMode=-1;         // Mode du losange (PIX_ARGB,PIX_RGB, PIX_TRUE, PIX_256, PIX_255

   static protected int LASTID=0;    // Dernier number d'image donné
   
   static private float DEFAULT_OPACITITY = 1f;

   //Le separateur du path / ou \
   static String DEFIMG,LOOKCALIB,INPROGRESS,OK,FAIL,IMGERR,ERROR,CALIBERR,
                 IMGERR1,LOADIMG,HTTPERR,UNK,ONEDIM,NAXIS2,LOCFILE,VOAPP;

   // Type de table des couleurs supportees
   static final int CMGRAY = 0;
   static final int CMBB   = 1;
   static final int CMA    = 2;
   static final int CMSTERN= 3;

   // modes video possibles
   public static final int VIDEO_NORMAL  = 0;  // Video mode normal
   public static final int VIDEO_INVERSE = 1;  // Video inverse

   // Origines possibles
   protected static final int ALADIN  = 0;  // Par le serveur d'images Aladin
   protected static final int LOCAL   = 1;  // Par un fichier local
   protected static final int OTHER   = 2;  // Par un serveur distants via une URL
   protected static final int COMPUTED= 3;  // Par un calcul interne

   // Resolution possibles
   protected static final int UNDEF  = 0;  // Non specifiee
   protected static final int FULL   = 1;  // Pleine res
   protected static final int LOW    = 2;  // Resolution basse
   protected static final int PLATE  = 3;  // Resolution pour plaque
   protected static final int STAND  = 4;  // Image standalone

   // Formats possibles
   protected static final int UNKNOWN= 0;  // Inconnu a priori
   protected static final int JPEG   = 1;  // JPEG niveau de gris (dédié au serveur Aladin)
   protected static final int FITS   = 2;  // FITS
   protected static final int HFITS  = 3;  // FITS hcompressed
   protected static final int GFITS  = 4;  // FITS gzipped
   protected static final int MRCOMP = 5;  // Mrcomp (methode CEA)
   protected static final int RGB    = 6;  // Composition couleur sans format /* Anaïs */
   protected static final int NATIVE = 7;  // Image couleur type JPEG/GIF/PNG...
   protected static final int PDS    = 8;  // Image PDS...

   public static final int ASINH  = 0;
   public static final int LOG    = 1;
   public static final int SQRT   = 2;
   public static final int LINEAR = 3;
   public static final int SQR    = 4;
   public static final int MULTFCT= 5;
   public static final String TRANSFERTFCT[] = { "Asinh", "Log","Sqrt","Linear","Pow2"," -- " };

   protected RandomAccessFile fCache; // L'accès au fichier cache
   protected byte [] pixels;		  // Tableau des pixels de l'image (sur 8 bits)
   protected byte [] pixelsZoom;      // Tabluea des pixels de l'image vignette (8 bits) pour le ZoomView
   protected byte[] pixelsOrigin;     // Tableau des pixels d'origine (LIGNES NON INVERSEES - format FITS)
   protected ColorModel cm;			  // La table des couleurs associee a l'image
   public int typeCM;			  // memorise la table des couleurs (CMGRAY ou CMBB ou CMA)
   public int cmControl[];	      // Valeurs de controle de la table des couleurs
   public int transfertFct;           // Fonction de transfert (LINEAR,LOG,SQR...)
   protected double hist[],histA[];   // Histogrammes des pixels (voir ColorMap)
   protected boolean flagHist;        // true si on dispose de l'histogramme des pixels à jour
   protected int width;				  // largeur de l'image
   protected int height;			  // hauteur de l'image
   public int video;				  // memorise le mode video (NORMAL ou INVERSE)
   protected int bitpix;              // profondeur de l'image a l'origine
   protected int naxis1;              // Largeur de l'image (diffère de width dans le cas de PlanImageHuge)
   protected int naxis2;              // Hauteur de l'image (diffère de height dans le cas de PlanImageHuge)
   protected int npix;                // nombre d'octets par pixel (pour éviter de le recalculer tout le temps à partir de bitpix)
   protected double dataMinFits=0.;   // La valeur DATAMIN indiquée dans l'enête FITS (si elle existe)
   protected double dataMaxFits=0.;   // La valeur DATAMAX indiquée dans l'enête FITS (si elle existe)
   protected double dataMin,dataMax;  // Plus grande et plus petite valeur de pixel effectivement trouvée 
                                      // (après suppression des pixels erronés) - sans prendre en compte BSACLE et BZERO
   protected double pixelMin,pixelMax;// Les min et max des cuts - sans prendre en compte BSACLE et BZERO
   protected boolean isBlank;         // True s'il y a une valeur consideree comme BLANK
   protected double blank;            // La valeur BLANK si elle existe
   public double bZero;            // La valeur BZERO si elle existe
   public double bScale=1.;        // La valeur BSCALE si elle existe

   // Les caracteristiques du plan Image
   Obj o=null;		   // La source associee a une image archive
   protected int fmt=0;	           // Format : FITS ou JPEG
   protected int res=0;	   	   // Resolution : FULL, LOW, ou PLATE
   protected int orig=0;	   // Origine (ALADIN | LOCAL | OTHER)

   // Parametres internes
   protected int imgID=-1;          // Numero incrementé pour reperer les changements d'etats de l'image
   String status,progress;       // Status ``en langage naturel'' de l'image
//   boolean flagAutoCropping=false;	// true si un auto-crop a ete applique

   // noeud image décrivant l'image à charger
   // (utile pour trouver la calibration d'une image JPEG ou RGB à partir des données du SIAP)
   protected ResourceNode imgNode;

   protected Plan forPourcent;  // Sert uniquement dans le cas des MEF, dans le cas du chargement
                                // de la première extension (voir Calque.newFitsExt()

   protected PlanImage(){}

  /** Creation d'un plan de type IMAGE (via un fichier)
   * @param file  Le nom du fichier
   */
   protected PlanImage(Aladin aladin, String file,MyInputStream inImg) {
      int fmt=FITS;
//      int res=UNDEF;
      String label;


      // Recuperation du nom du plan a partir du nom du fichier
      int i = file.lastIndexOf(Util.FS);
      label=(i>=0)?file.substring(i+1):file;

      try {
         // Creation d'une URL a partir du nom de fichier si nécessaire
         filename = (new File(file)).getCanonicalPath();
         u = new URL("file:"+(new File(file)).getCanonicalPath());

//         int type = inImg.getType();
//         if( (type & MyInputStream.MRCOMP)!=0 ) fmt=MRCOMP;
//         else if( (type & (MyInputStream.JPEG|MyInputStream.GIF))!=0 ) fmt=NATIF;
         dis = inImg;

      } catch( Exception e ) {
         String s=file+" "+ERROR+" !\n"+e;
         Aladin.warning(s,1);
         return;
      }

      Suite(aladin,LOCAL,label,
      		null,		// objet
                "",		// param
                LOCFILE+" ["+file+"]",	// from
                fmt,
                UNDEF,		// res
                null);		// o
   }
   
   /** Creation d'un plan de type IMAGE (via un stream) */
   protected PlanImage(Aladin aladin, MyInputStream inImg,String label) {
       int fmt=FITS;
//       int res=UNDEF;

       if( label==null ) label="VOApp";
       dis=inImg;

       Suite(aladin,LOCAL,label,
       		null,		// objet
                 "",		// param
                 VOAPP,	// from
                 fmt,
                 UNDEF,		// res
                 null);		// o
    }

   private InputStream inputStream;
   protected PlanImage(Aladin aladin, InputStream inImg,String label,String from) {
       int fmt=FITS;
//       int res=UNDEF;

       if( label==null ) label="";
       inputStream=inImg;

       Suite(aladin,LOCAL,label,
            null,       // objet
                 "",        // param
                 from, // from
                 fmt,
                 UNDEF,     // res
                 null);     // o
    }

   /** Creation d'un plan de type IMAGE (via un fichier)
    * @param file  Le nom du fichier
    * @param imgNode noeud image décrivant l'image à charger
    */
    protected PlanImage(Aladin aladin, String file,MyInputStream inImg,String label,String from,
          Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
       this.doClose=doClose;
       int fmt= FITS;
//       int res=UNDEF;
       this.imgNode = imgNode;
       flagSkip = skip;
       this.forPourcent = forPourcent;
//       if( forPourcent!=null ) isOldPlan=true;  // Subtilité pour éviter un Calque.bestPlacePost()
       if( forPourcent!=null ) noBestPlacePost=true;
       flagWaitTarget=true;  // voir Command.waitingPlanInProgress

       // Recuperation du nom du plan a partir du nom du fichier
       if( file!=null ) {
          int i = file.lastIndexOf(Util.FS);
          if( label==null ) setLabel( (i>=0)?file.substring(i+1):file );
          if( from==null ) from = file;
       }

       if( label==null ) label="";
       if( from==null )  from="";

       try {
          // Creation d'une URL a partir du nom de fichier
          if( file!=null ) {
              // patch Thomas 30/05/06 (sous Windows, on ne pouvait pas charger de FITS extensions à partir d'une URL distante)
              // TODO : à montrer à Pierre
              if( file.startsWith("http") ) u = new URL(file);
              else {
                 filename = (new File(file)).getCanonicalPath();
                 u = new URL("file:"+(new File(file)).getCanonicalPath());
              }
          }
          else u=null;

          dis = inImg;

       } catch( Exception e ) {
          error="_END_XFITS_";
          String s=file+" error !\n"+e;
          Aladin.warning(s,1);
          return;
       }

       Suite(aladin,LOCAL,label,
       		     null,		// objet
                 "",		// param
                 from,	   // from
                 fmt,
                 UNDEF,		// res
                 o);		// o
    }
    
    /** Creation d'un plan de type IMAGE synchrone à partir d'un fichier */
//    protected PlanImage(Aladin aladin, String fileName)  throws Exception {
//       this.aladin  = aladin;
//       setLogMode(false);
//       type         = IMAGE;
//       dis = new MyInputStream(new FileInputStream(fileName));
//       setLabel(fileName);
//       isBlank      = false;
//       video        = aladin.configuration.getCMVideo();
//       transfertFct = aladin.configuration.getCMFct();
//       typeCM       = aladin.configuration.getCMMap();
//       if( cmControl==null ) cmControl = new int[3];
//       cmControl[0] = 0; cmControl[1] = 128; cmControl[2] = 255;
//       setFmt();
//       waitForPlan();
//    }

    static protected void createChaine(Chaine chaine) {
//       DEFIMG = chaine.getString("PIDEFIMG");
       DEFIMG = "";
       LOOKCALIB = chaine.getString("PILOOKCALIB");
       INPROGRESS = chaine.getString("PIINPROGRESS");
       OK = chaine.getString("PIOK");
       FAIL = chaine.getString("PIFAIL");
       IMGERR = chaine.getString("PIIMGERR");
       ERROR = chaine.getString("PIERROR");
       CALIBERR = chaine.getString("PICALIBERR");
       IMGERR1 = chaine.getString("PIIMGERR1");
       LOADIMG = chaine.getString("PILOADIMG");
       HTTPERR = chaine.getString("PIHTTPERR");
       UNK = chaine.getString("PIUNK");
       ONEDIM = chaine.getString("PIONEDIM");
       NAXIS2 = chaine.getString("PINAXIS2");
       LOCFILE = chaine.getString("PILOCFILE");
       VOAPP = chaine.getString("PIVOAPP");
    }

  /** Creation d'un plan de type IMAGE (via une URL)
   * @param u     l'URL qu'il va falloir appeler
   * @param label le nom du plan (dans la pile des plans)
   * @param objet le target central (objet ou coord)
   * @param param les parametres de l'image (SERC J MAMA...)
   * @param fmt   le format de l'image (JPEG, FITS, MRC, ...)
   * @param res la res (FULL, PLATE...)
   *
   * Pour les images 2MASS et DENIS et EROSI, rajout temporaire d'un test
   * pour les charger obligatoirement en FITS, interpretation
   * du WCS.
   */
   protected PlanImage(Aladin aladin,
                       MyInputStream inImg,
                       int orig, URL u,
                       String label,String objet,
                       String param, String from,
                       int fmt,int res,
                       Obj o) {
      this.u =u;
      this.dis=inImg;
      Suite(aladin,orig,label,objet,param,from, fmt,res,o);
   }

   /** Creation d'un plan de type IMAGE (via une URL)
    * @param u     l'URL qu'il va falloir appeler
    * @param label le nom du plan (dans la pile des plans)
    * @param objet le target central (objet ou coord)
    * @param param les parametres de l'image (SERC J MAMA...)
    * @param fmt   le format de l'image (JPEG, FITS, MRC, ...)
    * @param res la res (FULL, PLATE...)
    * @param imgNode noeud image décrivant l'image à charger
    *
    * Pour les images 2MASS et DENIS et EROSI, rajout temporaire d'un test
    * pour les charger obligatoirement en FITS, interpretation
    * du WCS.
    */
    protected PlanImage(Aladin aladin,
                        MyInputStream inImg,
                        int orig, URL u,
                        String label,String objet,
                        String param, String from,
                        int fmt,int res,
                        Obj o, ResourceNode imgNode) {
       this.imgNode = imgNode;
       server = server!=null?imgNode.server:null;
       this.u =u;
       this.dis=inImg;
       Suite(aladin,orig,label,objet,param,from, fmt,res,o);
    }

  /** Creation d'un plan de type IMAGE (pour un backup) */
   protected PlanImage(Aladin aladin) {
      initImage(aladin);
   }

   protected void initImage(Aladin aladin) {
       this.aladin= aladin;
       type       = IMAGE;
       c          = Color.black;
       askActive  = true;
       isOldPlan  = true;       // Pour éviter entre autre de trier la pile lorsque le plan est créé
       cmControl  = new int[3];
       cmControl[0] = 0; cmControl[1]=128; cmControl[2]=255;
       transfertFct = LINEAR;
       opacityLevel=DEFAULT_OPACITITY;
   }

   /** Duplication d'un plan */
   protected PlanImage(Aladin aladin,PlanImage p) {
      this(aladin);
      p.copy(this);
   }

  /** Suite de la creation d'un plan de type IMAGE
   * @param label le nom du plan (dans la pile des plans)
   * @param objet le target central (objet ou coord)
   * @param param les parametres de l'interrogation (SERC J MAMA...)
   * @param from  La description de la provenance
   * @param fmt   le format de l'image
   * @param res la res
   * @param compress Le mode de compression
   */
   protected void Suite(Aladin aladin, int orig,
              String label,String objet,
              String param, String from,
              int fmt,int res,
              Obj o) {
      setLogMode(true);
      this.aladin  = aladin;
      this.orig    = orig;
      this.objet   = objet;
      this.param   = param;
      this.fmt     = fmt;
      this.res     = res;
      this.copyright    = from;
      this.opacityLevel = DEFAULT_OPACITITY;
      type         = IMAGE;
      setLabel(label);
      c            = Color.black;
      aladin.calque.selectPlan(this);
      isBlank      = false;
      video        = aladin.configuration.getCMVideo();
      transfertFct = aladin.configuration.getCMFct();
      typeCM       = aladin.configuration.getCMMap();
      this.o       = o;
      if( cmControl==null ) cmControl = new int[3];
      cmControl[0] = 0; cmControl[1] = 128; cmControl[2] = 255;

      if( dis!=null ) setFmt();
      else if( res==STAND )  this.fmt=FITS;

      // Plan en attente
      if( u==null && dis==null && inputStream==null) return;

      threading();
   }

   /** Détermine si fichier passé en paramètre peut être utilisé comme cache pour
    * accéder aux vraies pixels (sans avoir à sauvegarder dans le cache Aladin
    * @param in le flux déjà ouvert à la bonne position
    */
   protected boolean setCacheFromFile(MyInputStream in) {
      cacheOffset=0L;
      try {
         if( filename==null ) return false;
         long t=in.getType();
         if( (t & MyInputStream.FITS)==0 ) return false;
         if( (t & (MyInputStream.GZ|MyInputStream.HCOMP))!=0 ) return false;
         File f = new File(filename);
         if( f.isFile() && f.canRead() ) {
            cacheID = filename;
            cacheFromOriginalFile=true;
            cacheOffset = in.getPos();
Aladin.trace(3,"Direct pixel file access ["+cacheID+"] pos="+cacheOffset);
         }
      } catch( Exception e ) { e.printStackTrace();  return false; }
      return true;
   }

   static int id=0;

   /** Duplication du Plan (et de ses paramètres)
    * @param p1 le plan à copier
    * @param copyPixels true si on doit copier les pixels (uniquement 8 bits)
    */
   protected void copy(Plan p1) {
       super.copy(p1);
       PlanImage p = (PlanImage)p1;

       // Attention, on ne duplique que les données 8 bits, les pixels originaux sont partagés
       try {
         if( pixels!=null  ) {
             p.pixels = new byte[pixels.length];
             System.arraycopy(pixels,0,p.pixels,0,pixels.length);
          } else p.pixels = null;
      } catch( Exception e ) { p.pixels = null; }
       p.pixMode=pixMode;
       p.pixelsOrigin=pixelsOrigin;
       p.projD = null;
       p.projd = p.projInit = null;
       if( projd!=null ) p.setNewProjD(projd.copy());
       p.transfertFct=transfertFct;
       p.headerFits=headerFits;
       p.width=width;
       p.height=height;
       p.naxis1=naxis1;
       p.naxis2=naxis2;
       p.video=video;
       p.bitpix=bitpix;
       p.npix=npix;
       p.dataMin=dataMin;
       p.dataMax=dataMax;
       p.pixelMin=pixelMin;
       p.pixelMax=pixelMax;
       p.dataMinFits=dataMinFits;
       p.dataMaxFits=dataMaxFits;
       p.isBlank=isBlank;
       p.blank=blank;
       p.bZero=bZero;
       p.bScale=bScale;
       p.o=o;
       p.fmt=fmt;
       p.res=res;
       p.orig=orig;
       p.cacheID=cacheID;
       p.fCache=null;    // Pour forcer la réouverture
       p.cacheOffset = cacheOffset;
       p.cacheFromOriginalFile = cacheFromOriginalFile;
       p.typeCM=typeCM;
       System.arraycopy(cmControl,0,p.cmControl,0,3);
       p.cm = ColorMap.getCM(p.cmControl[0],p.cmControl[1],p.cmControl[2],
             p.video==PlanImage.VIDEO_INVERSE,
             p.typeCM, p.transfertFct,p.isTransparent());

   }

   /** Cropping de l'image sur la sous-image indiquée en paramètre et demande de réaffichage.
    * Si la sous-image sort de l'image, la sous-image sera automatiquement réduite à la zone
    * d'intersection.
    * @param x,y coin supérieur gauche
    * @param w,h taille
    * @param repaint true pour demander les réaffichages nécessaires
    * @return false si de fait l'image n'a pas été croppée (zoom couvre toute l'image)
    */
   protected boolean crop(double x,double y, double w, double h,boolean repaint) {

      // En cas de hors image
      int test=0;
      if( x<=0 ) { w +=x; x=0; test++; }
      if( y<=0 ) { h +=y; y=0; test++; }
      if( x+w>=width ) { w = width-x; test++; }
      if( y+h>=height ) { h = height-y; test++; }

      // inutile, le zoom couvre toute l'image
      if( test==4 ) return false;
      
      int wi = (int)Math.ceil(w);
      int hi = (int)Math.ceil(h);
      int xi = (int)Math.floor(x);
      int yi = (int)Math.floor(y);

      // Crop des pixels 8 bits
      byte npixels[] = new byte[wi*hi];
      getPixels(npixels,xi,yi,wi,hi);
      setBufPixels8(npixels);

      // Crop des pixels d'origine et mise à jour du cache
      if( hasOriginalPixels() ) {
         byte [] npixelsOrigin = new byte[wi*hi*npix];
         if( pixelsOrigin==null ) getPixelsFromCache(npixelsOrigin,npix,xi,yi,wi,hi);
         else getPixelsOrigin(npixelsOrigin,npix,xi,yi,wi,hi);
         pixelsOrigin = npixelsOrigin;

         cacheID=null;
         noCacheFromOriginalFile();
//         setInCache();
      }

      crop1(x,y,w,h,repaint);
      return true;
   }

    /** Crop particulier dans le cas où l'image d'origine était une image HUGE
     * car il faut construire de toutes pièces le tableaux pixels[] et pixelsOrigin[]
     * depuis les données du disque */
  protected boolean cropHuge(double x,double y, double w, double h,boolean repaint) {

     double r = 256./(pixelMax - pixelMin);
     try {
        int wi = (int)Math.ceil(w);
        int hi = (int)Math.ceil(h);
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);
        
        int size = wi*hi;
        setBufPixels8(new byte[size]);
        pixelsOrigin= new byte[size*npix];
        byte buf [] = new byte[wi*npix];
        int len = wi;
        int pos=0;
        int posOrig=0;

        openCache();

        for( int i=naxis2-(yi+hi), ligne=0; i<naxis2-y; i++, ligne++ ) {
           fCache.seek( cacheOffset + (i*(long)naxis1 + xi) * npix );
           fCache.readFully(buf);

           System.arraycopy(buf,0,pixelsOrigin,posOrig,buf.length);
           posOrig+=buf.length;

           for( int j=0; j<len; j++ ) {
              double c = getPixVal(buf,bitpix,j);
              if( Double.isNaN(c) ) { getBufPixels8()[pos++] = 0; continue; }
              getBufPixels8()[pos++] = (byte)( c<=pixelMin?0x00:c>=pixelMax?0xff
                    :(int)( ((c-pixelMin)*r) ) & 0xff);
           }
           setPourcent( 99. * ligne/h );
        }
        invImageLine(wi,hi,getBufPixels8());

     } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); return false; }

     cacheID=null;
     noCacheFromOriginalFile();

     crop1(x,y,w,h,repaint);
     return true;
  }

  // Suite du crop
  protected void  crop1(double x,double y,double w,double h,boolean repaint) {

//     aladin.console.setCommand("crop "+Tok.quote(label)+" "+x+","+y+" "+w+"x"+h);

     // Ajustement de la calib
     if( Projection.isOk(projd) ) projd.crop(x,y,w,h);

     // Validation du changement de taille
     naxis1=width = (int)Math.ceil(w);
     naxis2=height = (int)Math.ceil(h);
     setHasSpecificCalib();

     // Mise à jour des flags de recalcul et de retraçage
     changeImgID();
     if( repaint ) {
        aladin.view.newView(1);
        aladin.calque.repaintAll();
     }
  }

   /** Indique l'objet central et les parametres de l'image pour un log */
   protected String getLogInfo() {
      return (transfertFct!=LINEAR?TRANSFERTFCT[transfertFct]+"/":"")+
             super.getLogInfo();
   }

   /** Retourne true si la coordonnée est dans l'image */
   protected boolean contains(Coord c) {
      try {
         projd.getXY(c);
         if( Double.isNaN(c.x) ) return false;
         if( c.x<0 || c.x>=naxis1 || c.y<0 || c.y>=naxis2 ) return false;
         return true;
      } catch( Exception e) {}
      return false;
   }

  /** Retourne vrai si les deux plans passes en parametre
   * ont meme resolution et meme format */
   protected static boolean sameFmtRes(Plan pa,Plan pb) {
      PlanImage p1,p2;
      try { p1=(PlanImage)pa; p2=(PlanImage)pb; }
      catch( Exception e ) { return false; }
      if( p1==null && p2==null ) return true;
      if( p1==null || p2==null ) return false;
      return p1.fmt==p2.fmt && p1.res==p2.res;
   }

  /** Donne le Survey decrit par le parametre.
   * Retourne l'explication du survey en fonction de la ligne d'interrogation
   *
   * @param param description d'une image (ex: SERC J MAMA)
   */
   protected String survey() {
      if( orig==ALADIN && (param==null || param.trim().length()==0) ) return(DEFIMG);
      if( orig==ALADIN ) return ServerAladin.whichQualifier(param).trim();
      return param;
   }

  /** Retourne un baratin explicatif pour le fmt et la resolution */
   protected static String describeFmtRes(MyInputStream dis,int res) {
      String a=null;
      try { a = MyInputStream.decodeType( dis.getType() ); }
      catch( Exception e ) { a=null; }
//      String f = fmt==RGB?"none (composed image)":fmt==JPEG?"Jpeg":fmt==FITS||fmt==HFITS||fmt==GFITS||fmt==MRCOMP?"Fits":null;
//      String c = fmt==HFITS?"Hcompressed":fmt==GFITS?"gzipped":fmt==MRCOMP?"MR compressed":null;
      String r = res==FULL?"Full resolution":res==LOW?"Low resolution":
                 res==PLATE?"Plate view":res==STAND?"(Aladin standalone image)":null;

      StringBuffer s = new StringBuffer();
      if( a!=null ) s.append(a+" ");
//      if( f!=null ) s.append(f+" ");
//      if( c!=null ) s.append(c+" ");
//      if( s.length()>0 /*anais*/ && fmt!=RGB) s.append("format ");
      if( r!=null ) s.append("- "+r);

      return s.toString();

   }

   /** Retourne l'Url qui a permis de constuire le plan.
    * Petit bidouillage dans le cas d'une image JPEG provenant du serveur Aladin afin que l'Url
    * retourne du FITS
    */
   protected String getUrl() {
      if( u==null ) return null;
      String url = u.toString();

      // Cas particulier d'aladin en JPEG
      if( isAladinJpeg() ) url=ServerAladin.change2FITS(url);
      return url;
   }
   
   /** Retourne la couleur de fond du plan */
   protected Color getBackGroundColor() {
      if( colorBackground!=null)  return colorBackground;
      return isPixel() && active && video==PlanImage.VIDEO_NORMAL ? Color.black : Color.white;
   }
   
   /** retourne la table des couleurs associée à l'image */
   public ColorModel getCM() { return cm; }
   
   /** Restauration de la Colormap par défaut */
   public void restoreCM() {
      IndexColorModel  ic = ColorMap.getCM(cmControl[0],cmControl[1],cmControl[2],
            video==PlanImage.VIDEO_INVERSE,
            typeCM,transfertFct,isTransparent());
      setCM(ic);
   }
   
   /** Retourne La taille angulaire du pixel (alpha, delta) en degrées, null si pas possible */
   protected double [] getPixelSize() {
      if( !Projection.isOk(projd) ) return null;
      try {
         double resAlpha = projd.getPixResAlpha();
         double resDelta = projd.getPixResDelta();
         return new double[]{ resAlpha,resDelta };
      } catch( Exception e ) {}
      return null;
   }

   /** Petit test pour du bidouillage */
   protected boolean isAladinJpeg() { return orig==ALADIN && fmt==JPEG; }

  /** Retourne le format sous forme d'une chaine */
   protected static String getFormat(int fmt) {
      return fmt==JPEG?"JPEG":fmt==FITS?"FITS":fmt==GFITS?"GFITS":
             fmt==HFITS?"HFITS":fmt==MRCOMP?"MRCOMP":"UNKNOWN";
   }

  /** Retourne la resolution sous forme d'une chaine*/
   protected static String getResolution(int r) {
      return r==FULL?"FULL":r==LOW?"LOW":r==PLATE?"PLATE":r==STAND?"STAND":"UNDEF";
   }

  /** Retourne le code du format */
   protected static int getFmt(String fmt) {
      return fmt.equalsIgnoreCase("FITS")?FITS:fmt.equalsIgnoreCase("GFITS")?GFITS:
             fmt.equalsIgnoreCase("HFITS")?HFITS:fmt.equalsIgnoreCase("MRCOMP")?MRCOMP:
             fmt.equalsIgnoreCase("JPEG")?JPEG:UNKNOWN;
   }

  /** Retourne le code de la resolution */
   protected static int getRes(String res) {
      return res.equalsIgnoreCase("FULL")?FULL:res.equalsIgnoreCase("LOW")?LOW:
             res.equalsIgnoreCase("PLATE")?PLATE:res.equalsIgnoreCase("STAND")?STAND:UNDEF;
   }

   /** Supprime la mémorisation des histogramme de pixels (voir ColorMap) */
   protected void freeHist() { hist=histA = null; }

   /** Indique que l'histogramme des pixels n'est plus à jour */
   protected void histOk(boolean flag) { flagHist=flag; }

   /** Retourne true si on dispose de l'histogramme des pixels à jour */
   protected boolean hasHist() { return !(hist==null || !flagHist); };

   protected void setPourcent(double x) {
      if( forPourcent!=null ) forPourcent.setPourcent(x);
      pourcent=x;
   }
   
   protected boolean isSync() {
      return flagOk && !flagProcessing || error!=null;
   }

  /** Libere le plan.
   * cad met toutes ses variables a <I>null</I> ou a <I>false</I>
   */
   protected boolean Free() {
      aladin.view.free(this);
      super.Free();
//      close();
      if( dis!=null ) { try{ dis.close(); dis=null; } catch( Exception e ) {} }
      if( fCache!=null ) {
         try {
            fCache.close();
            fCache=null;
         } catch(Exception e){}
      }
      dataMinFits=dataMaxFits=0;
      headerFits=null;
      setBufPixels8(null);
      pixelsOrigin=null;
      removeFromCache();
      cacheFromOriginalFile=false;
      cacheID=null;
      cacheOffset=0L;
      naxis1 =naxis2 = width = height = -1;
      setPourcent(-1);
      tailleLoad=-1;
      cm=null;
      fmt=res=0;
      video=aladin.configuration.getCMVideo();
      typeCM=aladin.configuration.getCMFct();
      freeHist();
      isBlank = false;
      forPourcent=null;
      if( image!=null ) image.flush();
      image=null;
      changeImgID();
      return true;
   }

   protected String cacheID=null;   // filename du cache des pixels d'origine, null si aucun
   protected boolean cacheFromOriginalFile=false; // true si on accède aux vrais pixels directement dans le fichier d'origine
   protected long cacheOffset=0L;   // position des pixels dans fichier pointé par cacheID (avec cacheFromFile=true)
   protected String cacheFileName=null; // Nom du fichier d'origine

   // Prochain indice pour les suffixes des noms de fichiers temporaires
   static private int MAXCACHEID=1;

   /** Fabrique un nom de fichier temporaire unique pour la session
    *  fnn.tmp ou nn est un numero incrémenté
    */
   static private String getNextCacheID() {
      return "f"+(MAXCACHEID++)+".tmp";
   }

   private boolean cache=true;
   protected void cacheAvailable(boolean flag ) {
      cache=flag;
   }

   /** Suspend l'usage du fichier d'origine comme cache */
   protected void noCacheFromOriginalFile() {
//      if( cacheFromOriginalFile ) {
         cacheFromOriginalFile=false;
         cacheOffset=0;
         cacheID=null;
//      }
   }

   protected boolean rewriteInCache(byte []buf) { return setInCache(1,buf); }

   /** Mémorisation dans le cache des pixels d'origine (via buf[])
    * @param mode 1 si on force la sauvegarde, même si cacheID!=null
    *  @return true si la mémorisation a fonctionné, sinon false
    */
   private boolean setInCache(byte [] buf) { return setInCache(0,buf); }
   private boolean setInCache(int mode,byte [] buf) {
      if( !Aladin.STANDALONE || !cache || cacheFromOriginalFile ) return false;
      if( mode!=1 && cacheID!=null ) return true;    // Deja dans le cache
      cacheOffset=0L;
      RandomAccessFile f=beginInCache(buf);
      if( f!=null ) { try{ f.close(); } catch(Exception e){}; return true; }
      return false;
   }

   /** Mémorisation partielle des pixels d'origine. Le flux n'est pas fermé */
   synchronized protected RandomAccessFile beginInCache(byte [] buf) {
      if( !Aladin.STANDALONE || !cache || cacheFromOriginalFile ) return null;
      if( buf==null || !aladin.createCache() ) return null;
      try {
         if( cacheID==null ) cacheID = aladin.CACHEDIR+Util.FS+getNextCacheID();
         if( fCache!=null ) try {
            fCache.close();
            fCache=null;
         } catch( Exception e ) {}
         File f = new File(cacheID);
         RandomAccessFile rf = new RandomAccessFile(f,"rw");
         
         //  Si on écrite d'un coup un trop grop fichier, ça explose la mémoire (pb procédure native !!)
//         rf.write(buf);
         int bloc=4*1024*1024;
         for( int pos=0,len=0; pos<buf.length; pos+=len ) {
            len = buf.length-pos<bloc ? buf.length-pos : bloc;
            rf.write(buf,pos,len);
         }

         aladin.setInCache(buf.length);
Aladin.trace(3,"Original pixels saved in cache ["+cacheID+"]");
         return rf;
      } catch( Exception e ) { e.printStackTrace(); cacheID=null; return null; }
   }

   /** Relecture dans le cache des pixels d'origine.
    *  @return true si les pixels sont disponibles dans pixelsOrigin[], sinon false
    */
   protected boolean getFromCache() {
      if( !Aladin.STANDALONE ) return false;
      if( pixelsOrigin!=null ) {
         return true;
      }
      if( cacheID==null ) {
         return false;
      }
      try {
         openCache();
         fCache.seek(cacheOffset);
         byte [] pixelsOrigin1 = new byte[width*height*npix];
         
         //  Si on lit d'un coup un trop grop fichier, ça explose la mémoire (pb précédure native !!)
//         fCache.readFully(pixelsOrigin);  
         int bloc=4*1024*1024;
         for( int pos=0,len=0; pos<pixelsOrigin1.length; pos+=len ) {
            len = pixelsOrigin1.length-pos<bloc ? pixelsOrigin1.length-pos : bloc;
            fCache.readFully(pixelsOrigin1,pos,len);
         }
         pixelsOrigin=pixelsOrigin1;
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
Aladin.trace(3,"Original pixels lost ["+cacheID+"]");
         cacheID=null;
         pixelsOrigin=null;
         bitpix=8;
         return false;
      }
Aladin.trace(3,"Original pixels reloaded "
      +(cacheOffset!=0L?"offset="+cacheOffset+" ":"")+(cacheFromOriginalFile?"":"from cache ")+"["+cacheID+"]");
      return true;
   }

   /** Relecture dans le cache d'une portion des pixels d'origine.
    * @param pixels le tableau qui va accueillir les pixels lus (doit avoir été taillé assez grand)
    * @param npix  le nombre d'octets des pixels
    * @param x,y coin supérieur gauche de la zone à extraire  (attention, les lignes sont à l'envers !!!)
    * @param w,h taille de la zone à extraire
    * @return true si les pixels sont disponibles dans le cache, sinon false
    */
   protected boolean getPixelsFromCache(byte [] pixels,int npix, int x,int y, int w, int h) {
      if( !Aladin.STANDALONE ) return false;
      if( cacheID==null ) return false;
      try {
         openCache();

         int len = w*npix;

         for( int i=naxis2-y-h, k=0; i<naxis2-y; i++,k++ ) {
            fCache.seek( cacheOffset+ (i*(long)naxis1 + x) * npix);
            fCache.readFully(pixels, k*w*npix, len);
         }

      } catch( Exception e ) { e.printStackTrace(); return false; }
Aladin.trace(3,"Original pixel sub-image ("+x+","+y+","+w+","+h+") extracted"
      +(cacheOffset!=0L?"offset="+cacheOffset+" ":"")+(cacheFromOriginalFile?"":"from cache ")+"["+cacheID+"]");

      return true;
   }

   /** Retourne true s'il est préférable et possible d'accéder à la valeur
    * du pixel Origin courant par un accès disque direct */
   protected boolean pixelsOriginFromDisk() {
      if( cacheID==null ) return false;

      if( aladin.view.getPixelMode()==View.INFILE ) return false;

      // L'image est petite, on la charge entièrement pour s'éviter des
      // accès disques
      if( naxis1*naxis2*npix<=Aladin.LIMIT_PIXELORIGIN_INMEM ) return false;
      return true;
   }

   /** Ouverture du cache si nécessaire */
   protected void openCache() {
      if( fCache==null && cacheID!=null ) {
         try {
            File f = new File(cacheID);
            fCache = new RandomAccessFile(f,"r");
         } catch( Exception e ) { fCache=null; cacheID=null; cacheOffset=0L; }
      }
   }

   /** Relecture dans le cache d'un unique pixel d'origine.
    * Si besoin, ouvre le flux sur le fichier cache, mais ne le referme pas (voir Free() )
    * @param pixels le tableau qui va accueillir le pixel lu (doit avoir été taillé assez grand)
    * @param bitpix  le nombre d'octets des pixels
    * @param x,y coord du pixel à extraire  (attention, les lignes sont à l'envers !!!)
    * @return true si le pixel est disponible, sinon false
    */
   protected boolean getOnePixelFromCache(byte [] pixels,int npix, int x,int y) {
      if( !Aladin.STANDALONE ) return false;
      if( cacheID==null ) return false;
      try {
         openCache();
         fCache.seek( cacheOffset+((naxis2-y-1)*(long)naxis1 + x) * npix );
         fCache.readFully(pixels, 0, npix);

      } catch( Exception e ) { e.printStackTrace(); return false; }
//Aladin.trace(3,"Original one pixel ("+x+","+y+") extracted "
//      +(cacheOffset!=0L?"offset="+cacheOffset+" ":"")+(cacheFromOriginalFile?"":"from cache ")+"["+cacheID+"]");

      return true;
   }

//   /** Ecriture dans le cache d'un unique pixel d'origine.
//    * Si besoin, ouvre le flux sur le fichier cache, mais ne le referme pas (voir Free() )
//    * @param pixels le tableau qui décrit le pixel
//    * @param bitpix  le nombre d'octets des pixels
//    * @param x,y coord du pixel à écrire  (attention, les lignes sont à l'envers !!!)
//    * @return true si ok, sinon false
//    */
//   private boolean setOnePixelToCache(byte [] pixels,int bitpix, int x,int y) {
//      if( !Aladin.STANDALONE ) return false;
//      if( cacheID==null ) return false;
//      try {
//         openCache();
//         fCache.seek( cacheOffset+((naxis2-y-1)*naxis1 + x) * bitpix );
//         fCache.write(pixels, 0, bitpix);
//
//      } catch( Exception e ) { e.printStackTrace(); return false; }
////Aladin.trace(3,"Original one pixel ("+x+","+y+") written "
////      +(cacheOffset!=0L?"offset="+cacheOffset+" ":"")+(cacheFromOriginalFile?"":"from cache ")+"["+cacheID+"]");
//
//      return true;
//   }

   /**
    * Vérifie que le plan est le dernier à utiliser le cache sur les pixels d'origine
    * Cela peut ne pas être le cas en cas de copie de plan
    */
   protected boolean cacheCanBeFree() {
      if( lockCacheFree ) return false;
      Plan [] allPlan = aladin.calque.getPlans();
      for( int i=0; i<allPlan.length; i++ ) {
         if( allPlan[i] == this || !(allPlan[i] instanceof PlanImage) ) continue;
         if( ((PlanImage)allPlan[i]).cacheID==null ) continue;
         if( ((PlanImage)allPlan[i]).cacheID.equals(cacheID) ) return false;
      }
      return true;
   }
   
   // Si true, pixelsOrigin[] ne peut être nettoyé
   private boolean lockCacheFree=false;
   
   /** Empêche (temporairement) la suppression du buffer pixelsOrigin[] en cas d'utilisation d'un cache,
    * par exemple pendant un calcul */
   protected void setLockCacheFree(boolean flag) { lockCacheFree=flag; }

   /** Suppression du fichier des pixels d'origine se trouvant dans le cache
    * Uniquement si aucun autre plan ne partage ces pixels (même cacheID)
    * et que l'on ne prend pas les pixels du fichier d'origine
    */
   private void removeFromCache() {
      if( cacheFromOriginalFile || cacheID==null ||
            aladin.CACHEDIR==null || !cacheID.startsWith(aladin.CACHEDIR)) return;
      if( cacheCanBeFree() ) {
         try {
            File f = new File(cacheID);
            aladin.setInCache(-f.length());
            f.delete();
         } catch( Exception e ) { e.printStackTrace(); }
         Aladin.trace(3,"Original pixels removed from cache ["+cacheID+"]");
      }
      cacheID=null;
   }

   /** Sauvegarde des pixels d'origine dans le cache et libération du bloc mémoire
    * si ce n'est déjà fait
    * @return true si une liberation a eu lieu, sinon false
    */
   protected boolean pixelsOriginIntoCache() {
      // Cas d'accès direct au fichier d'origine
      // ou cas du cache classique
      if( pixelsOrigin!=null && (cacheFromOriginalFile || setInCache(pixelsOrigin))) {
Aladin.trace(3,"Original pixels RAM free for "+label);
         pixelsOrigin=null;
         return true;
      }
      return false;
   }

   /** Rechargement des pixels d'origine depuis le cache si c'est possible */
   protected boolean pixelsOriginFromCache() { 
      return getFromCache(); 
    }
   
   // Indique que l'image a change en incrementant le numero de version
   // de l'image.
   synchronized void changeImgID() { imgID=LASTID++; pixelsZoom=null; }
   synchronized void nextImgID() { imgID=LASTID++;  }

  /** Etat de l'image.
   * En cas de modif de la table des couleurs ou de l'image elle-meme,
   * la valeur retournee par cette fonction est incrementee
   * @return la nouvelle valeur de l'etat de l'image
   */
   protected int getImgID() { return imgID; }

   protected Image image;             // Image au sens Java correspondant aux pixels (voir getImage())
   protected int oImgID=-2;           // Numéro de l'image pour savoir s'il vaut en générer une nouvelle

   /** Return une Image (au sens Java). Mémorise cette image pour éviter de la reconstruire
    * si ce n'est pas nécessaire 
    * @param now paramètre ignoré, voir PlanBG
    */
   protected Image getImage(ViewSimple v,boolean now) {
      if( oImgID==imgID ) return image;
      image = Toolkit.getDefaultToolkit().createImage(
            new MemoryImageSource(width,height,cm, getBufPixels8(), 0, width));
      oImgID=imgID;
      return image;
   }

  /** Extraction d'une portion de l'image en 8 bits.
   * Retourne une portion de l'image sur la forme d'un tableau de pixels
   * @param newpixels Le tableau a remplir (il doit etre assez grand)
   * @param x,y,w,h   Le rectangle de la zone a extraire
   */
   protected void getPixels(byte [] newpixels,int x,int y,int w,int h) {
      getPixels(newpixels,pixels,width,height,x,y,w,h);
   }
   protected void getPixels(byte [] newpixels,byte[]pixels,int width,int height,
                            int x,int y,int w,int h) {
      int i,n;
      int k=0;
      int aw,ah;	// Difference en abs et ord lorsqu'on depasse l'image

      // Ajustement de la taille en cas de depassement
      aw=ah=0;
      if( x+w>width )  { aw = x+w-width;  w-=aw; }
      if( y+h>height ) { ah = y+h-height; h-=ah;}

      for( i=y, n=y+h; i<n; i++ ) {
         System.arraycopy(pixels,i*width+x, newpixels,k, w);
         k+=w+aw;
      }
   }

   /** Extraction d'une portion de l'image des pixels d'origines (en bytes)
    * Retourne une portion de l'image sur la forme d'un tableau de pixels
    * @param newpixels Le tableau a remplir (il doit etre assez grand)
    * @param bitpix le nombre d'octets par pixel
    * @param x,y,w,h   Le rectangle de la zone a extraire (les lignes sont comptées depuis le bas !!!)
    */
    protected void getPixelsOrigin(byte [] newpixels,int bitpix,int x,int y,int w,int h) {
       for( int i = height-y-h, k=0; i<height-y; i++,k++ ) {
          System.arraycopy(pixelsOrigin, (i*width +x )*bitpix, newpixels, k*w * bitpix, w*bitpix);
       }
    }

    /** Retourne une copie des pixels d'origine en double */
    protected double [][] getPixels() {
       double doublePixels [][] = new double[width][height];
       getPixels(doublePixels,0,0,width,height);
       return doublePixels;
    }

    /** Remplace les pixels d'origine */
    protected void setPixels(double pix[][] ) {
       setPixels(pix,bitpix==0 ? -32 : bitpix);
    }

    /** Modifie l'image d'origine.
     * @param pix tableau des pixels en double
     * @param bitpix codage à la FITS
     */
    protected void setPixels(double pix[][],int bitpix) {
       naxis1=width = pix.length;
       naxis2=height = width==0 ? 0 : pix[0].length;
       this.bitpix = bitpix;
       npix = Math.abs(bitpix)/8;
       if( pixelsOrigin==null
             || pixelsOrigin.length!=width*height*npix ) pixelsOrigin = new byte[ width*height*npix ];
       if( getBufPixels8()==null || getBufPixels8().length!=width*height ) setBufPixels8(new byte[width*height]);

       for( int y=0; y<height; y++ ) {
          for( int x=0; x<width; x++ ) {
             setPixelOriginInDouble(x,y, pix[x][y]);
//             setPixVal(pixelsOrigin,bitpix, y*width+x, (pix[x][y]-bZero)/bScale);
          }
       }

       npix = Math.abs(bitpix)/8;
    }

    /** Regénère le cache et les pixels 8 bits en fonction du tableau des pixels
     * originaux (pixelsOrigin) */
    protected void reUseOriginalPixels() {
       noCacheFromOriginalFile();
       rewriteInCache(pixelsOrigin);
       recut(0,0,aladin.configuration.getCMCut() );
    }

    /** Extraction d'une portion de l'image en double. (a la FITS)
     * Retourne une portion de l'image sur la forme d'un tableau de pixels, centré sur le pixel
     * le plus brillant
     * @param newpixels Le tableau a remplir (il doit etre assez grand)
     * @param Point Le pixel central proposé => le pixel central trouvé
     * @param w   La largeur du tableau
     */
    protected void getPixelsCentroid(double [] newpixels,Point p,int w) {
       getPixels(newpixels,p.x-w/2,p.y-w/2,w,w);
       int pos=(w/2)*w+w/2;
       double max=newpixels[pos];
       for( int i=0; i<newpixels.length; i++ ) {
          if( newpixels[i]<=max ) continue;
          pos=i;
          max=newpixels[i];
       }
       p.y=(p.y-w/2)+pos/w; p.x=(p.x-w/2)+pos%w;
       getPixels(newpixels,p.x-w/2,p.y-w/2,w,w);
    }

   /** Extraction d'une portion de l'image en double. (a la FITS)
    * Retourne une portion de l'image sur la forme d'un tableau de pixels
    * @param newpixels Le tableau a remplir (il doit etre assez grand)
    * @param x,y,w,h   Le rectangle de la zone a extraire
    */
    protected void getPixels(double [] newpixels,int x,int y,int w,int h) {
       int i,j;
       int aw,ah;	// Difference en abs et ord lorsqu'on depasse l'image

       // Ajustement de la taille en cas de depassement
       aw=ah=0;
       if( x+w>width )  { aw = x+w-width;  w-=aw; }
       if( y+h>height ) { ah = y+h-height; h-=ah;}

       // Récupération des pixels originaux un à un
       if( pixelsOriginFromDisk() ) {
          byte [] b = new byte[npix];
          for( i=0; i<h; i++ ) {
             for( j=0; j<w; j++ ) {
                getOnePixelFromCache(b,npix,j+x,i+y);
                newpixels[i*w+j] = getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero;
             }
          }

       // Récupération des pixels originaux ?
       } else if( hasOriginalPixels() && getFromCache() ) {
          for( i=0; i<h; i++ ) {
             for( j=0; j<w; j++ ) newpixels[i*w+j] = getPixelOriginInDouble(j+x,i+y);
          }

       // Sinon on prend les pixels 8 bits
       } else {
          for( i=0; i<h; i++ ) {
             for( j=0; j<w; j++ ) {
                newpixels[i*w+j] = ( (getBufPixels8()[(height-(i+y)-1)*width+j+x] & 0xFF));
             }
          }
       }
    }

    /** Extraction d'une portion de l'image en double. (a la FITS)
     * Retourne une portion de l'image sur la forme d'un tableau de pixels
     * @param newpixels Le tableau a remplir (il doit etre assez grand)
     * @param x,y,w,h   Le rectangle de la zone a extraire
     */
     protected void getPixels(double [][] newpixels,int x,int y,int w,int h) {
        int i,j;
        int aw,ah;  // Difference en abs et ord lorsqu'on depasse l'image

        // Ajustement de la taille en cas de depassement
        aw=ah=0;
        if( x+w>width )  { aw = x+w-width;  w-=aw; }
        if( y+h>height ) { ah = y+h-height; h-=ah;}

        // Récupération des pixels originaux ?
        if( hasOriginalPixels() && getFromCache() ) {
           for( i=0; i<h; i++ ) {
              for( j=0; j<w; j++ ) newpixels[j][i] = getPixelOriginInDouble(j+x,i+y);
           }

        // Sinon on prend les pixels 8 bits
        } else {
           for( i=0; i<h; i++ ) {
              for( j=0; j<w; j++ ) {
                 newpixels[j][i] = ( (getBufPixels8()[(height-(i+y)-1)*width+j+x] & 0xFF));
              }
           }
        }
     }

  /** Attente d'une image en construction.
   * Emballe simplement une attente pour connaitre la taille avec une boucle
   * plus rapide que celle utilisee par MedaTracker.
   * @param o   Un objet implantant l'interface ImageObserver
   * @param img L'image qu'il faut attendre
   */
/*
   protected static void waitImage(ImageObserver o, Image img) {
      while( img.getWidth(o)<0 || img.getHeight(o)<0) {
         try { Thread.currentThread().sleep(100); }
         catch(Exception e) {  }
      }
   }
*/
  /** Generation du label du plan.
   * Retourne le label en fonction de l'etat courant du plan
   * Il s'agit simplement d'ajouter des "..." quand le plan est en
   * cours de construction ainsi que le pourcentage de loading
   * @return Le label genere
   */
//   protected String getLabel() {
//      int p = (int)getPourcent();
//      int l = /*type==Plan.IMAGERGB||type==Plan.IMAGEBLINK?0:*/ 9;
//      if( p>0 ) {
//         return Util.align(label+"...",l)+p+"%";
//      }
//      return super.getLabel();
//   }

  /** Retourne le statut (en langage naturel) de progression de chargement de l'image */
   protected String getStatus() {
      return status+": "+progress;
   }


//  // Juste pour rire
//  static final String[] SOLAR = { "moon","lune","sun","soleil","mercury","mercure",
//  			"venus","mars","jupiter","saturn","saturne","uranus",
//                        "neptune","pluto","pluton","aladin","milky way",
//                        "J2000","sky","io" };
//
//  /** Retourne true s'il s'agit d'un objet du systeme solaire */
//  protected static boolean isSolar(String s) {
//     if( s==null ) return false;
//     s = s.trim();
//     for( int i=0; i<SOLAR.length; i++ ) {
//        if( s.equalsIgnoreCase(SOLAR[i]) ) return true;
//     }
//     return false;
//  }


 /** Fermeture du flux (jamais sur un FITS extension */
  protected void close() {
     if( dis==null ) return;
     try {
        if( (dis.getType() & MyInputStream.XFITS) != 0 ) return;
        dis.close();
     } catch( Exception e) {}
  }

  /** Recherche de la calibration Aladin
   private boolean getCalibration() throws Exception {
      Calib c=null;
      boolean r;

      status="- Look for image/calibration"; progress="in progress...";
      try {

         // On prend l'objet central et eventuellement on lui ajoute
         // les qualifiers de l'image
         String query = Glu.quote(objet);
         if( res==UNDEF ) query += " FULL";
         else query += " "+getResolution(res);
         if( param!=null && param.length()>0 ) query = query+" "+Glu.quote(param);

         URL ux = aladin.glu.getURL(Glu.debugTag("Calibration"),query,false,false);
         DataInputStream flux = new DataInputStream(ux.openStream());
         c = new Calib(flux);
         flux.close();
         status=status+": ok\n"; progress="";

      } catch( Exception e2 ) {
         status=status+": fail\n"; progress="";
         aladin.error = " Image server error\n \n"+getStatus()+"\n"+e2;
         close();
         return false;
      }

      // Mini-test sur la calibration
      double taille = (int)(c.widtha*600)/10.0;
      if( taille<=0.0 ) {
         status=status+": error\n"; progress="";
         aladin.error = "calibration error\n \n"+getStatus();
         close();
         return false;
      }

      // Recuperation de la taille de l'image
      Dimension d = c.getImgSize();
      width = d.width;
      height = d.height;

      // On installe l'image en memoire
      status=status+"- Download the image"; progress="in progress...";
      if( fmt==FITS ) r = cacheImageFits(u);
// CETTE LIGNE DOIT ETRE COMMENTEE SI ON NE VEUT PLUS SUPPORTER MRCOMP
//      else if( fmt==MRCOMP ) r = cacheImageMrc(u);
      else r = cacheImageJpeg(u);

      if( flagAutoCropping ) c.cropping(width,height);

      if( !r ) {
         status=status+": fail\n"; progress="";
         aladin.error = "image format unknown or server error\n \n"+getStatus();
         close();
         return false;
      } else status=status+": ok\n"; progress="";

      // On associe la projection
      setNewProjD(new Projection(Projection.ALADIN,c));
      return r;
   }
*/

  /**
   * Recuperation du Glutag pour une Calib en fonction
   * d'un glutag pour une image (ancien serveur)
   * A VIRER AU PLUS VITE
   */
  private String getGluTabCalib(String tag) {

     StringTokenizer st = new StringTokenizer(tag.substring(2)," >");
     StringBuffer buf=null;
     while( st.hasMoreTokens() ) {
        String p = st.nextToken();

        if( p.indexOf("FITS")>=0 || p.indexOf("JPEG")>=0 ) continue;
        if( p.indexOf("Image,")==0 ) p="Calibration,"+p.substring("Image,".length());

        if( buf==null ) buf = new StringBuffer("<&");
        else buf.append(' ');
        buf.append(p);
     }

     buf.append('>');
     return buf.toString();
  }

  /**
   * Recuperation de l'URL pour la calibration
   * A VIRER AU PLUS VITE
   */
//  private String getUrlCalib(String u) throws Exception {
//     int deb = u.indexOf('?');
//     if( deb==-1 ) return null;
//
//     StringTokenizer st = new StringTokenizer(u.substring(deb+1),"&");
//     StringBuffer buf=null;
//     while( st.hasMoreTokens() ) {
//        String p = st.nextToken();
//
//        if( p.equals("out=image") ) p="out=calibration";
//        else {
//           String pDec = Util.myDecode(p);
//           if( pDec.startsWith("<&Image,") ) {
//              pDec=getGluTabCalib(pDec);
//              p=URLEncoder.encode(pDec);
//           }
//        }
//        if( buf==null ) buf = new StringBuffer();
//        else buf.append('&');
//
//        buf.append(p);
//     }
//
//     u = u.substring(0,deb)+"?"+buf;
//
//     return u;
//  }

  /**
   * Chargement d'une image du vieux serveur Aladin. Il est necessaire de
   * rechercher la calibration astrometrique en construisant une URL derivee
   * de l'URL d'acces a l'image. Dans le cas d'une image FITS, on utilise
   * egalement cette methode car le WCS est moins bon.
   * TOUT CA EST VRAIMENT DU BRICOLAGE !!! BOF
   * @return true si c'est bon, false sinon
   * @throws java.lang.Exception
   */
//   private boolean getOldAladinImage() throws Exception {
//      Calib c=null;
//      boolean r;
//
//      status="- "+LOOKCALIB;
//      progress=INPROGRESS;
//
//      // Construction de l'URL de chargement de la calibration astrometrique
//      // (simplement deduite) de l'URL d'acces a l'image. S'il s'agit du
//      // standalone, il suffit de remplacer le parameter "out=image" par
//      // "out=calibration". Dans le cas de l'applet, il faut modifier
//      // la marque GLU appelee a distance par nph-glu, a savoir "Image"
//      // par "Calibration", et virer le parametre du format 'JPEG' ou 'FITS'
//      String urlCalib=getUrlCalib(u+"");
//Aladin.trace(2,"Get: "+urlCalib);
//      try {
//         URL ux = new URL(urlCalib);
//         DataInputStream flux = new DataInputStream(ux.openStream());
//         c = new Calib(flux);
//         flux.close();
//         status=status+": "+OK+"\n"; progress="";
//      } catch( Exception e2 ) {
//         status=status+": "+FAIL+"\n"; progress="";
//         aladin.error = IMGERR+"\n \n"+getStatus()+"\n"+e2;
//         sendLog("Error","getOldAladinImage() ["+e2+"] urlCalib="+(urlCalib==null?"null":urlCalib));
//         close();
//         return false;
//      }
//
//      // Mini-test sur la calibration
//      double taille = (int)(c.widtha*600)/10.0;
//      if( taille<=0.0 ) {
//         status=status+": "+ERROR+"\n"; progress="";
//         aladin.error = CALIBERR+"\n \n"+getStatus();
//         sendLog("Error","getOldAladinImage() [taille<=0.0] urlCalib="+(urlCalib==null?"null":urlCalib));
//         close();
//         return false;
//      }
//
//      // Recuperation de la taille de l'image
//      Dimension d = c.getImgSize();
//      naxis1 = width = d.width;
//      naxis2 = height = d.height;
//
//      // On installe l'image en memoire
//      status=status+"- "+LOADIMG; progress=INPROGRESS;
//
//      if( dis==null ) { if( !openUrlImage() ) return false; }
//      if( fmt==JPEG ) r = cacheImageNatif(dis);
//      else r = cacheImageFits(u);
//
////      if( flagAutoCropping ) c.cropping(width,height);
//
//      if( !r ) {
//         status=status+": "+FAIL+"\n"; progress="";
//         aladin.error = IMGERR1+"\n \n"+getStatus();
////         sendLog("Error","getOldAladinImage() [error in cacheImage"+(fmt==JPEG?"Jpeg":"Fits")+
////                        "] u="+(u==null?"null":u.toString()));
//         close();
//         return false;
//      } else status=status+": "+OK+"\n"; progress="";
//
//      // On associe la projection
//      setNewProjD(new Projection(Projection.ALADIN,c));
//      setHasSpecificCalib();
//      return r;
//   }

//  /** Chargement d'une image associee a un objet du systeme solaire */
//   private void loadSolarObject() {
//      width = height = 500;
//      cacheImageNatif(u);
//      status = "The CDS is dedicated to\nthe extra solar system objects\nonly\n";
//      Aladin.warning(status);
//      progress="";
//      error = Plan.NOREDUCTION;
//      System.out.println("!!! "+error);
//   }

  /** Ouverture du stream depuis une image d'archive */
   private boolean openUrlImage() {
      status=LOADIMG;
      progress=INPROGRESS;
Aladin.trace(3,"Load the image at: "+u);
      try {
//         dis = new MyInputStream(u.openStream());
//         dis = dis.startRead();
         dis = Util.openStream(u);
      } catch(Exception e) {
         if( orig==LOCAL ) {

         }
Aladin.trace(3,"Second try for opening the stream due to: "+e+"...");
         try{
//            dis = new MyInputStream(u.openStream());
//            dis = dis.startRead();
            dis = Util.openStream(u);
         } catch(Exception e1) {
            if( u!=null ) System.err.println("Pb with: "+u);
            error=aladin.error=HTTPERR;
            Aladin.warning(error,1);
            close();
            return false;
         }
      }
      setFmt();
      return true;
   }

   private void setFmt() {
      try {
         long type=dis.getType();
         fmt = (type & MyInputStream.GZ)!=0?GFITS:
               (type & MyInputStream.HCOMP)!=0?HFITS:
               (type & MyInputStream.MRCOMP)!=0?MRCOMP:
               (type & MyInputStream.PDS)!=0?PDS:
               (type & MyInputStream.NativeImage())!=0?JPEG:
               FITS;
         pixMode = fmt==JPEG ? PIX_256 : PIX_TRUE;
      } catch( Exception e) {}
   }

  /** Attente pendant la construction du plan.
   * @return <I>true</I> si ok, <I>false</I> sinon.
   */
   protected boolean waitForPlan() {
      // verrue thomas pour SWarpServer
      if( dis==null && inputStream!=null ) {
          try {
              dis = new MyInputStream(inputStream);
              dis = dis.startRead();
          }
          catch(Exception ioe) {
              ioe.printStackTrace();
              callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.ERROR, "error"));
              return false;
          }
      }

      try {
         // Utilisation du threading pour le resampling (resp. le traitement)
         if( type==IMAGERSP ) { return ((PlanImageResamp)this).resample(); }
         else if( type==IMAGEALGO ) { return ((PlanImageAlgo)this).compute(); }
         else {
            Calib c=null;

            if( orig==LOCAL ) { status=LOADIMG; progress=INPROGRESS; }
            if( fmt!=MRCOMP ) {
                    if (dis == null) {
                        if (!openUrlImage()) {
                            callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.ERROR, "error"));
                            return false;
                        }
                    }
            }

            // Differentes methodes de lecture suivant le format
            boolean r;
            if( fmt==JPEG || this instanceof PlanImageColor ) r = cacheImageNatif(dis);
            else if( fmt==PDS ) r = cacheImagePDS(dis);
            else if( fmt==FITS || fmt==HFITS || fmt==GFITS ) r = cacheImageFits(dis);


// CETTE LIGNE DOIT ETRE COMMENTEE SI ON NE VEUT PLUS SUPPORTER MRCOMP
//            else if( fmt==MRCOMP )  r = cacheImageMrc(dis);

            else { close(); return false; }

            if( !flagSkip ) {

               if( !r ) {
                  status=status+": "+FAIL+"\n"; progress="";
                  close();
                  return false;
               } else status=status+": "+OK+"\n"; progress="";

               try {
                  if( fmt==JPEG || fmt==RGB ) {

                     // Cas où la calib se trouverait dans un commentaire de l'image JPEG ou PNG
                     if( dis.hasCommentCalib() ) {
                        try {
                           headerFits=dis.createFrameHeaderFitsFromCommentCalib(this);
                           c = new Calib(headerFits.getHeaderFits());
                        } catch( Exception e ) {
                           dis.jpegCalibAddNAXIS(width,height);   // Peut être une entete partielle à la Sloan
                           headerFits=dis.createFrameHeaderFitsFromCommentCalib(this);
                           c = new Calib(headerFits.getHeaderFits());
                        }
if( c!=null ) Aladin.trace(3,"Reading FITS key words embedded in the comment segment");
                     }

                     //  cas où on peut récupérer la calibration depuis la réponse SIAP
                     if( c==null && imgNode!=null ) {
                        c = imgNode.getCalib();
                        if( c==null ) throw new Exception();
                        setHasSpecificCalib();
Aladin.trace(3,"Creating calibration from SIA metadata");

                     }
                     
                     // Juste pour Anaïs
                     if( c==null && fmt==JPEG ) {
                        try {
                           headerFits=DSShhh();
                           c = new Calib(headerFits.getHeaderFits());
Aladin.trace(3,"Creating calibration from hhh additional file");
                        } catch( Exception e ) { }
                     }

                     if( c==null ) throw new Exception();

                  } else  c = new Calib(headerFits.getHeaderFits());

//                  if( !Aladin.BETA && c.system==7 ) { c=null; projd=null; throw new Exception(); }

                  // On determine le x,y et le alpha,delta du centre de l'image
                  if( c!=null ) {
                     co = c.getImgCenter();
                     setNewProjD(new Projection(Projection.WCS,c));

                     // En cas de chargement par un fichier local
                     if( objet==null ) {
                        objet = co.getSexa();
                        aladin.dialog.setDefaultTarget(objet);
                        aladin.dialog.setDefaultTaille(this);

                     } else {
                        // Pour que le repere soit positionne non au centre de l'image
                        // mais en fonction des coordonnees de la requete
                        co=null;
                     }
                  }
               } catch( Exception e3 ) {
                  //                if( aladin.levelTrace>=3 ) e3.printStackTrace();
                  error=NOREDUCTION;
                  progress="";
               }

            }

            close();
         }

         // On change l'etat et le repere
         if( !flagSkip ) {
            setExtName();
            changeImgID();
            aladin.view.setRepere(this);
         }


      } catch( Exception ex ) {

         ex.printStackTrace();

         // Cas particulier d'un FITS avec extension
         // On ne peut pas bien repérer la fin
         long t=0;
         try { t = dis.getType(); } catch( Exception e ) { }
         if( (t&MyInputStream.XFITS)!=0 ) {
            error="_END_XFITS_";
            return false;
         }

         ex.printStackTrace();
         error=aladin.error!=null?aladin.error:ex.toString();
         Aladin.warning(error,1);
         close();
         callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.ERROR, error));
         return false;

      } catch( Error eall ) {
         eall.printStackTrace();
         error=aladin.error = eall.toString();
         Aladin.warning(error+" ",1);
         close();
         callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.ERROR, error));
         return false;
      }
      callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.SUCCESS, null));
      return true;
   }
   
   // Charger l'entête fits à partir de la même URL avec l'extension hhh
   private FrameHeaderFits DSShhh() throws Exception  {
      RandomAccessFile rf=null;
      try {
         String f = filename;
         f=f.substring(0,f.lastIndexOf('.'))+".hhh";
         if( !(new File(f)).exists() ) return null;
         rf = new RandomAccessFile(new File(f), "r");
         byte [] b = new byte[(int)rf.length()];
         rf.readFully(b);
         FrameHeaderFits h = new FrameHeaderFits(this,new String(b),true);
         return h;
      }  finally { if( rf!=null ) try { rf.close(); } catch( Exception e1) {} }
   }

  /** Determination pour un tableau de bean[] de l'indice du bean min
   * et du bean max en fonction d'un pourcentage d'information desire
   * @param bean les valeurs des beans provenant de l'analyse d'une image
   * @return mmBean[2] qui contient les indices du bean min et du bean max
   */
   private int[] getMinMaxBean(int [] bean) {
      double minLimit=0.003; 	// On laisse 3 pour mille du fond
      double maxLimit=0.9995;    // On laisse 1 pour mille des etoiles
      int totInfo;			// Volume de l'information
      int curInfo;			// Volume courant en cours d'analyse
      int [] mmBean = new int[2];	// indice du bean min et du bean max
      int i;

      // Determination du volume de l'information
      for( totInfo=i=0; i<bean.length; i++ ) {
// System.out.println("bean["+i+"]="+bean[i]);
         totInfo+=bean[i];
      }

      // Positionnement des indices des beans min et max respectivement
      // dans mmBean[0] et mmBean[1]
      for( mmBean[0]=mmBean[1]=-1, curInfo=i=0; i<bean.length; i++ ) {
         curInfo+=bean[i];
         double p = (double)curInfo/totInfo;
         if( mmBean[0]==-1 ) {
            if( p>minLimit ) mmBean[0]=i;
         } else if( p>maxLimit ) { mmBean[1]=i; break; }
      }

//      // Verification que tout s'est bien passe
//      if( mmBean[0]==-1 || mmBean[1]==-1 ) {
//         System.err.println("Image autocut problem => no autocut applied");
//         mmBean[0]=0;
//         mmBean[1]=bean.length-1;
//      }

      return mmBean;
   }


   // Conversion byte[] en entier 32
   // Recupere sous la forme d'un entier 32bits un nombre entier se trouvant
   // a l'emplacement i du tableau t[]
//   static final protected int getInt(byte[] t,int i) {
//      return (((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF);
//   }
   // Conversion byte[] en entier 16
   // Recupere sous la forme d'un entier 16bits un nombre entier se trouvant
   // a l'emplacement i du tableau t[]
//   static final protected int getShort(byte[] t,int i) {
//      return  (((t[i])&0xFF)<<8) | (t[i+1])&0xFF;
//   }
   
   static final protected int getByte(byte[] t, int i) {
      return (int)(t[i]&0xFF);
   }
   static final protected int getShort(byte[] t, int i) { 
      return (t[i]<<8) | t[i+1]&0xFF; 
   }
   static final protected int getInt(byte[] t, int i) {
      return ((t[i]<<24) | ((t[i+1]&0xFF)<<16) | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF);
   }
   static final protected long getLong(byte[] t, int i) {
      return (((long)((t[i]<<24) | ((t[i+1]&0xFF)<<16) | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF))<<32)
      | ((((t[i+4]<<24) | ((t[i+5]&0xFF)<<16) | ((t[i+6]&0xFF)<<8) | t[i+7]&0xFF)) & 0xFFFFFFFFL);
   }
   static final protected double getFloat(byte[] t, int i) {
      return Float.intBitsToFloat(((t[i]<<24) | ((t[i+1]&0xFF)<<16) 
            | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF));
   }
   static final protected double getDouble(byte[] t, int i) {
      long a = (((long)(((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF))<<32)
      | (((((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF)) & 0xFFFFFFFFL);
      return Double.longBitsToDouble(a);
   }

   /**
    * Recuperation de la valeur du pixel dans le tableau d'origine
    * @param t le tableau des pixels d'origine (en byte[])
    * @param bitpix la profondeur des pixels (a la mode FITS)
    * @param i la position du pixel (sans tenir compte de la taille du pixel)
    * @return
    */
   static final protected double getPixVal1(byte[] t,int bitpix,int i) {
      try {
         switch(bitpix) {
            case   8: return getByte(t,i);
            case  16: return getShort(t,i*2);
            case  32: return getInt(t,i*4);
            case  64: return getLong(t,i*8);
            case -32: return getFloat(t,i*4);
            case -64: return getDouble(t,i*8);
         }
         return Double.NaN;
      } catch( Exception e ) { return Double.NaN; }

   }
   
   final protected double getPixVal(byte[] t,int bitpix,int i) {
      double pix = getPixVal1(t,bitpix,i);
      if( isBlank && pix==blank ) return Double.NaN;
      return pix;
   }


//   static final protected double getPixVal(byte[] t,int bitpix,int i) {
//      try {
//         switch(bitpix) {
//            case   8: return ((t[i])&0xFF);
//            case  16: i*=2; return ( ((t[i])<<8) | (t[i+1])&0xFF );
//            case  32: i*=4; return (((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF);
//            case  64: i*=8; 
//                      return (((long)(((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF))<<32)
//                             | (((((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF)) & 0xFFFFFFFFL);
//            case -32: i*=4; return Float.intBitsToFloat((((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF));
//            case -64: i*=8;
//                      long a = (((long)(((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF))<<32)
//                            | (((((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF)) & 0xFFFFFFFFL);
//                       return Double.longBitsToDouble(a);
//         }
//         return 0.;
//      } catch( Exception e ) { return Double.NaN; }
//
//   }

   // Conversion entier 32 en byte dans le tableau t[] à partir de l'emplacement i
   static final protected void setInt(byte[] t,int i,int val) {
      t[i]   = (byte)(0xFF & (val>>>24));
      t[i+1] = (byte)(0xFF & (val>>>16));
      t[i+2] = (byte)(0xFF & (val>>>8));
      t[i+3] = (byte)(0xFF &  val);
   }

   /**
    * Ecriture de la valeur du pixel dans le tableau d'origine
    * @param t le tableau des pixels d'origine (en byte[])
    * @param bitpix la profondeur des pixels (a la mode FITS)
    * @param i la position du pixel (sans tenir compte de la taille du pixel)
    * @param val la valeur du pixel
    * @return
    */
   static final protected void setPixVal(byte[] t,int bitpix,int i,double val) {
      int c;
      long c1;
      switch(bitpix) {
         case   8: t[i]=(byte)(0xFF & (int)val);
                   break;
         case  16: i*=2;
                   c = (int)val;
                   t[i]  =(byte)(0xFF & (c>>>8));
                   t[i+1]=(byte)(0xFF & c);
                   break;
         case  32: i*=4;
                   setInt(t,i,(int)val);
                   break;
         case  64: i*=8;
                   c1 = (long)val;
                   c = (int)(0xFFFFFFFFL & (c1>>>32));
                   setInt(t,i,c);
                   c = (int)(0xFFFFFFFFL & c1);
                   setInt(t,i+4,c);
                   break;
         case -32: i*=4;
                   c=Float.floatToIntBits((float)val);
                   setInt(t,i,c);
                   break;
         case -64: i*=8;
                   c1 = Double.doubleToLongBits(val);
                   c = (int)(0xFFFFFFFFL & (c1>>>32));
                   setInt(t,i,c);
                   c = (int)(0xFFFFFFFFL & c1);
                   setInt(t,i+4,c);
                   break;
      }
   }

   /** Recopie de la valeur d'un pixel depuis un tableau de pixelOrigin[]
    * en fonction de la position (sans tenir compte de la taille du pixel
    * @param src pixelOrigin[] source
    * @param pos1 position du pixel dans src
    * @param dst pixelOrigin[] destination
    * @param pos2 position du pixel dans dst
    */
   protected void copyPixVal(byte src[],int pos1,byte dst[],int pos2) {
       System.arraycopy(src,pos1*npix,dst,pos2*npix,npix);
   }

   /** Essaye de positionner la fonction de transfert courante
    * et recalcul les pixels
    * @return true si ça a marché (les vrais pixels doivent être disponibles
    */
   final protected boolean setTransfertFct(int fct) { return setTransfertFct(fct,false); }
   final protected boolean setTransfertFct(int fct,boolean recut) {
      int oldTransfertFct=transfertFct;
      boolean rep=true;
      transfertFct=fct;

//      if( recut ) rep=recut(minPixCut,maxPixCut,false);
      if( !rep ) transfertFct=oldTransfertFct;
      return rep;
   }

//   final protected boolean setTransfertFct(int fct,boolean recut) {
//      int oldTransfertFct=transfertFct;
//      boolean rep=true;
//      transfertFct=fct;
//      if( recut ) rep=recut(minPixCut,maxPixCut,false);
//      if( !rep ) transfertFct=oldTransfertFct;
//      return rep;
//   }

   /** Calcul la valeur du pixel en appliquant la fonction de transfert
    *  Si le min est négatif, on effectue un shift pour le LOG et SQRT
    */
//   final private double getTransfertPixel(double pixel,int fct) {
//      double shift;
//      switch( fct ) {
//         case LINEAR: return pixel;
//         case LOG:    shift = (minPixCut<=0)?-minPixCut+1:1;
//                      return Math.log(shift+pixel);
//         case SQRT:   shift = (minPixCut<=0)?-minPixCut:0;
//                      return Math.sqrt(shift+pixel);
//         case SQR:    shift = (minPixCut<=0)?-minPixCut:0;
//                      return (shift+pixel)*(shift+pixel);
//      }
//      return 0;
//   }

   /** Retrouve la valeur approximative du pixel original en appliquant l'inverse
    *  de la fonction de transfert.
    *  Si le min est négatif, on effectue un shift pour le LOG et SQRT
    */
//   final private double getInvTransfertPixel(double pixel,int fct) {
//      double shift;
//      switch( fct ) {
//         case LINEAR: return pixel;
//         case LOG:    shift = (minPixCut<=0)?-minPixCut+1:1;
//                      return Math.exp(pixel)-shift;
//         case SQR:    shift = (minPixCut<=0)?-minPixCut:0;
//                      return Math.sqrt(pixel)-shift;
//         case SQRT:   shift = (minPixCut<=0)?-minPixCut:0;
//                      return pixel*pixel - shift;
//      }
//      return pixel;
//   }

   /** Retourne la valeur approximative du pixel original en appliquant l'inverse
    *  du changementd'échelle */
   protected double getInvPixel(double pix8) {
      return pixelMin+pix8*(pixelMax-pixelMin)/256.;
   }
   
   /** Retourne la valeur niveau de gris correspondant à la valeur du pixel */
   protected double getGreyPixel(double pixel) {
      return (pixel-pixelMin)*256./(pixelMax-pixelMin);
   }

   /** Mise sur 8 bits de l'image
    * @param pIn le tableau des pixels en entree
    * @param bitpix la profondeur des pixels (format FITS)
    * @param width,height Largeur et hauteur de l'image
    * @param minCut,maxCut valeurs limites
    * @param autocut true s'il faut appliquer l'algo d'autocut
    * @return pOut Le tableau des pixels sur 8 bits
    */
   protected byte[] getPix8Bits(byte[] pOut, byte[] pIn, int bitpix, int width, int height,
         double minCut,double maxCut,boolean autocut) {

      findMinMax(pIn,bitpix,width,height,minCut,maxCut,autocut,0);
      if( pOut==null ) pOut = new byte[width*height];

      int npix = Math.abs(bitpix/8);

      // Passage en 8 bits
      to8bits(pOut,0,pIn,pIn.length/npix,bitpix,/*isBlank,blank,*/pixelMin,pixelMax,true);
      return pOut;
   }
   
   /** Retourne true si le point (xImg,yImg) est bien sur un pixel */
   protected boolean isOnPixel(int xImg, int yImg) {
      int pixel = (int)getPixel8Byte(xImg, yImg);
      System.out.println("Je suis sur "+pixel);
      return pixel>=20;
   }
   
   protected boolean isBlank(int pixel) { return isBlank && pixel==blank; }
   protected boolean isBlank(double pixel) { return isBlank && pixel==blank || Double.isNaN(pixel); }
   
   /** True si la coordonnée x,y se trouve dans l'image */
   protected boolean isInside(int x,int y) { return x>=0 && x<naxis1 && y>=0 && y<naxis2; }

   /**
    * Détermination du min et max des pixels passés en paramètre
    * Met à jour les variables minPixCut et maxPixCut
    * @param pIn Tableau des pixels à analyser
    * @param bitpix codage FITS des pixels
    * @param width Largeur de l'image
    * @param height hauteur de l'image
    * @param minCut Limite min, ou 0 si aucune
    * @param maxCut limite max, ou 0 si aucune
    * @param autocut true si on doit appliquer l'autocut
    * @param ntest Nombre d'appel en cas de traitement récursif.
    */
   protected void findMinMax(byte[] pIn, int bitpix, int width, int height,
         double minCut,double maxCut,boolean autocut,int ntest) {
      int i,j,k;
      boolean flagCut=(ntest>0 || minCut!=0. && maxCut!=0.);

//    Recherche du min et du max
      if( !(this instanceof PlanImageCube) && !(this instanceof PlanImageRGB) ) setPourcent(75);
      double max = 0, max1 = 0;
      double min = 0, min1 = 0;

//    Marge pour l'échantillonnage (on recherche min et max que sur les 1000 pixels centraux en
//    enlevant éventuellement un peu de bord
      int MARGEW=(int)(width*0.05);
      int MARGEH=(int)(height*0.05);
      

//    LES DEUX LIGNES QUI SUIVENT SONT A COMMENTER SI ON VEUT ETRE SUR DE NE PAS LOUPER
//    DES PARTICULARITES LOCALES SUR LES GROSSES IMAGES.
      if( width - 2*MARGEW>1000 ) MARGEW = (width-1000)/2;
      if( height - 2*MARGEH>1000 ) MARGEH = (height-1000)/2;

      double c;

      if( !autocut && (minCut!=0. || maxCut!=0.) ) {
         max=maxCut;
         min=minCut;

      } else {
         boolean first=true;
         long nmin=0,nmax=0;
         for( i=MARGEH; i<height-MARGEH; i++ ) {
            for( j=MARGEW; j<width-MARGEW; j++ ) {
               c = getPixVal(pIn,bitpix,i*width+j);

//             On ecarte les valeurs sans signification
               if( isBlank(c) ) continue;

               if( flagCut ) {
                  if( c<minCut || c>maxCut ) continue;
               }

               if( first ) { max=max1=min=min1=c; first=false; }

               if( min>c ) { min=c; nmin=1; }
               else if( max<c ) { max=c; nmax=1; }
               else {
                  if( c==min ) nmin++;
                  if( c==max ) nmax++;
               }

               if( c<min1 && c>min || min1==min && c<max1 ) min1=c;
               else if( c>max1 && c<max || max1==max && c>min1 ) max1=c;
            }
         }

//         Aladin.trace(4,"Min/Max detect. on "+(width-2*MARGEW)+"x"+(height-2*MARGEH)+" pix => ["+min+"("+nmin+"x)/"+min1+" .. "+max1+"/"+max+"("+nmax+"x)]");

         if( autocut && max-min>256 ) {
            if( min1-min>max1-min1 && min1!=Double.MAX_VALUE && min1!=max ) min=min1;
            if( max-max1>max1-min1 && max1!=-Double.MAX_VALUE && max1!=min  ) max=max1;
         }
      }

      if( autocut ) {
         if( !(this instanceof PlanImageCube || this instanceof PlanImageHuge || this instanceof PlanImageRGB) ) setPourcent(80);

//       Histogramme
         int nbean = 10000;
         double l = (max-min)/nbean;
//         Aladin.trace(3,"image autocut for: min="+min+" max="+max+" nbean="+nbean+" beansize="+l);
         int[] bean = new int[nbean];
         for( i=MARGEH; i<height-MARGEH; i++ ) {
            for( k=MARGEW; k<width-MARGEW; k++) {
               c = getPixVal(pIn,bitpix,i*width+k);
               if( isBlank(c) ) continue;

               j = (int)((c-min)/l);
               if( j==bean.length ) j--;
               if( j>=bean.length || j<0 ) continue;
               bean[j]++;
            }
         }

//       Selection du min et du max en fonction du volume de l'information
//       que l'on souhaite garder
         int [] mmBean = getMinMaxBean(bean);

//       Verification que tout s'est bien passe
         if( mmBean[0]==-1 || mmBean[1]==-1 ) {
            min1=dataMinFits; max1=dataMaxFits;
//            Aladin.trace(3,"Image autocut problem => no autocut applied => min="+min1+" max="+max1);
         } else {
            min1=min;
            max1 = mmBean[1]*l+min1;
            min1 += mmBean[0]*l;
//            Aladin.trace(3,"image autocut("+ntest+"): beanMin="+ mmBean[0]+" beanMax="+mmBean[1]+" => min="+min+" max="+max);
         }

         if( mmBean[0]!=-1 && mmBean[0]>mmBean[1]-5 && ntest<3 ) {
            if( min1>min ) min=min1;
            if( max1<max ) max=max1;
            findMinMax(pIn,bitpix,width,height,min,max,autocut, ntest+1);
            return;
         }

         min=min1; max=max1;
      }
      
//    Juste pour que le message n'apparaisse que pour les images normales ou le
//    premier plan d'une cube
      if( Aladin.levelTrace>=4 && (!(this instanceof PlanImageCube) || autocut) ) {
         Aladin.trace(4,"PlanImage.findMinMax(minCut="+minCut+",maxCut"+maxCut
               +",autocut="+autocut+",ntest="+ntest+") => min="+min+" max="+max);
      }


//    Memorisation des parametres de l'autocut
      pixelMin=min;
      pixelMax=max;
      if( !autocut ) { dataMin=pixelMin; dataMax=pixelMax; }
   }
   
   /** Retourne true si le format d'image permet la transparence */
   public boolean isTransparent() { return pixMode!=PIX_256 && pixMode!=PIX_RGB ; } // fmt!=JPEG; }
   
//   public boolean isTransparent() {
//      return pixMode == PIX_255 || pixMode == PIX_TRUE || pixMode == PIX_ARGB;
//   }


   /**
    * Passage en 8 bits avec normalisation et fonction de transfert.
    * RQ. Mémorise maxPix et minPix au passage
    * @param pOut tableau d'arrivée (8bits) déjà dimensionné
    * @param offsetOut position du premier octet dans tableau d'arrivée
    * @param pIn tableau d'origine (suivant bitpix)
    * @param len taille utile du tableau d'origine
    * @param bitpix codage FITS du tableau d'origine
    * @param transfertFct fonction de transfert
    * @param isBlank true si une valeur NULL est définie
    * @param blankOrig valeur des valeur non définies si isBlank==true
    * @param min valeur min pour la normalisation (avant application fct de transfert)
    * @param max valeur max pour la normalisation (avant application fct de transfert)
    */
   final protected void to8bits(byte [] pOut, int offsetOut, byte [] pIn, int len, int bitpix,
         /* boolean isBlank, double blank, */ double min, double max,boolean memoMinMax) {
      
      int range  = isTransparent() ? 255 : 256;
      int gapTransp = isTransparent() ?   1 :   0;

      // Simple cut du min et du max, puis extension/reduction sur les 8 bits
      double r = range/(max - min);
      range--;
      for( int i = 0; i < len; i++) {
         double c = getPixVal(pIn,bitpix,i);

         if( isBlank(c)) { pOut[i+offsetOut] = 0; continue; }

         // Pour info dans les properties
         if( memoMinMax ) {
            if( c>dataMax ) dataMax=c;
            else if( c<dataMin ) dataMin=c;
         }

         pOut[i+offsetOut] = (byte)( (gapTransp+ (c<=min?0x00:c>=max?range:(int)( (c-min)*r) )) & 0xff);
      }
   }

   /**
    * Calcul les pixels de l'imagette pour le ZoomView en prenant le pixel au plus proche
    * C'est très rapide et le rendu visuel est quasi le même que par interpolation
    */
   protected void calculPixelsZoom() { calculPixelsZoom( getBufPixels8() ); }
   protected void calculPixelsZoom(byte pixels[]) {
      
      // calcul du rapport Largeur/Hauteur de l'image
      int W = ZoomView.SIZE;
      int H = (int)(((double)ZoomView.SIZE/width)*height);
      if( H>W ) {
         W = (int)((double)W*W / H);
         H = ZoomView.SIZE;
      }

      double fctX = (double)width/W;
      double fctY = (double)height/H;

      if( pixelsZoom==null ) pixelsZoom = new byte[ZoomView.SIZE*ZoomView.SIZE];
      else for( int i=0; i<pixelsZoom.length; i++ ) pixelsZoom[i]=0;

      for( int y=0; y<H; y++ ) {
         int i = y*ZoomView.SIZE;
         int j = (int)(y*fctY);
         for( int x=0; x<W; x++ ) {
            pixelsZoom[i++] = pixels[ j*width + (int)(x*fctX) ];
         }
      }
   }

  /**
   * Arrondi "guide" d'un nombre en fonction de sa valeur
   * @param x
   * @return
   */
   protected String X(double x) {
      if( Double.isNaN(x) ) return "";
      if( bitpix>0 ) return Util.myRound(x+"");
      return Y(x);
   }
   protected String Y(double x) {
      if( Double.isNaN(x) ) return "";
      return Util.myRound(x);
//      double y = Math.abs(x);
//      int p=y>100000?4:y>1000?1:y>100?2:y>10?3:4;
//      return Util.myRound(x+"",p);
   }

   /** Retourne la chaine d'explication du mode de codage des pixels d'origine */
   protected String getPixelCodingInfo(int bitpix) {
      String s = bitpix==-64?"double" : bitpix==-32?"real"
            :bitpix==64?"long" :bitpix==32?"integer"
                  :bitpix==16?"short" : bitpix==8?"byte" : "unknown";
      return s+" (bitpix="+bitpix+")";
   }
   
   /** retourne la description du mode graphique */
   protected String getPixModeInfo() { return PIX_MODE[ pixMode ]; }

   /** Retourne la chaine d'explication de la taille et du codage de l'image
    * d'origine */
   protected String getSizeInfo() {
      return naxis1 + "x" + naxis2 +" / encoding:"+getPixelCodingInfo(bitpix)+" / "
      + Util.getUnitDisk(naxis1*naxis2*npix);
   }

   // Pour pouvoir charger juste un pixel d'origine
   protected byte onePixelOrigin[] = null;

   /**
    * Retourne sous forme d'une chaine éditable
    * la valeur du pixel suivant le mode courant (PIXEL,INFILE,REAL)
    * et de la position (x,y) dans l'image
    */
   protected String getPixelInfo(int x,int y,int mode) {
      if( !flagOk || y<0 || y>=height || x<0 || x>=width ) return "";
      
      switch(mode) {
          case View.LEVEL:
             return Util.align3(getBufPixels8()[y*width+x] & 0xFF)/*+" / 255"*/;
          case View.INFILE:
                 return pixelsOrigin==null?UNK:
                    X(getPixVal(pixelsOrigin,bitpix,(height-y-1)*width+x));
          case View.REALX:
          case View.REAL:
             if( fmt==JPEG ) return UNK;
             if( type!=ALLSKYIMG && pixelsOrigin!=null ) {
                double val = getPixVal(pixelsOrigin,bitpix,(height-y-1)*width+x)*bScale+bZero;
                if( aladin.levelTrace<4 || mode==View.REALX ) return Y(val);
                
                double infileVal=getPixVal1(pixelsOrigin,bitpix,(height-y-1)*width+x);
                return Y(val)+(Double.isNaN(infileVal) || val!=infileVal?"("+infileVal+")":"")+(isBlank && infileVal==blank ? " BLANK":"");
             };
             if( !pixelsOriginFromDisk() ) return UNK;
             if( onePixelOrigin==null ) onePixelOrigin = new byte[npix];
             if( !getOnePixelFromCache(onePixelOrigin,npix,x,y) ) return UNK;
             double val = getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero;
             if( aladin.levelTrace<4 || mode==View.REALX  ) return Y(val);
             
             double infileVal=getPixVal1(onePixelOrigin,bitpix,0);
             return Y(val)+(Double.isNaN(infileVal) || val!=infileVal?"("+infileVal+")":"")+(isBlank && infileVal==blank ? " BLANK":"");
      }
      return null;
   }

   /** Retourne la valeur du pixel en x,y sous la forme d'un double,
    * de préférence le pixel d'origine, et sinon le pixel 8 bits
    * Sert notamment pour les calculs sur les images
    * @param x,y coord. du pixel (y=0 en haut - à la java)
    */
   protected double getPixel(int x,int y) {
      // On retourne la valeur 8 bits faute de mieux
      if( pixelsOrigin==null && !isBigImage()
            || fmt==JPEG ) return (getBufPixels8()[(height-y-1)*width+x] & 0xFF);

      try {
         double pix = getPixVal(pixelsOrigin,bitpix,y*width+x);
         if( Double.isNaN(pix) ) return Double.NaN;
         return pix*bScale+bZero;
      } catch( Exception e ) { return Double.NaN; }
   }
   
   /**
    *   [0] => x,
    *   [1] => fwhmX,
    *   [2] => y,
    *   [3] => fwhmY,
    *   [4] => symetryAngle,
    *   [5] => ObjectPeak
    *   [6] => meanBackGround;
    */
   protected double [] getPixelStats(Point p) {
      int w=32;
      double pix[] = new double[w*w];
      int i=0;
      for( int y1 = p.y - w/2; y1<p.y+w/2; y1++ ) {
         for( int x1 = p.x - w/2; x1<p.x+w/2; x1++ ) pix[ i++ ] = getPixelInDouble(x1, y1);
      }

      double param[] = new double[8], sdev[] = new double[8];
      int error = Iqefunc.iqe(pix, w, w, param, sdev);
      if( error!=0 ) {
         aladin.trace(4,"PlanImage.getPixelStats("+p.x+","+p.y+") : Iqefunc error: code "+error);
         /* if( error==-1 ) */ return null;
      }

      aladin.trace(4,"PlanImage.getPixelStats("+p.x+","+p.y+") : mean=("+param[0]+","+param[2]+" fwhm="+param[1]+","+param[3]+" symetryAngle="+param[4]+
            " objectPeak="+param[5]+" meanBackground="+param[6]);
      
      param[0] = (float)( (p.x +0.5 ) + (param[0] - w/2) );
      param[2] = (float)( (p.y +0.5 ) + (param[2] - w/2) );
      
//      System.out.println("X="+param[0]+" Y="+param[2]);
      return param;
   }
   
   /** Retourne la valeur du pixel en double au mieux. IL FAUT AVOIR ESSAYER DE CHARGER
    * LES PIXELS ORIGINAUX DANS PIXELORIGIN AVANT D'APPELER CETTE METHODE
    * Rq : y dans le sens java (ordonnée orientée vers le bas) */
   protected double getPixelInDouble(int x,int y) {

      // On retourne la valeur 8 bits faute de mieux
      if( pixelsOrigin==null && !isBigImage() || fmt==JPEG ) return (getBufPixels8()[y*width+x] & 0xFF);

      // On accède directement à la valeur sur disque
      if( pixelsOriginFromDisk() ) {
         if( onePixelOrigin==null ) onePixelOrigin = new byte[npix];
         if( !getOnePixelFromCache(onePixelOrigin,npix,x,y) ) return Double.NaN;
         return getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero;
      }

      // On retourne la valeur en mémoire
      return getPixVal(pixelsOrigin,bitpix,(height-y-1)*width+x)*bScale+bZero;
   }


   /** Retourne la valeur du pixel en x,y sous la forme d'un double, ou NaN si impossible
    * @param x,y coord. du pixel (y=0 en bas - à la FITS)
    */
   protected double getPixelOriginInDouble(int x,int y) {
      if( pixelsOrigin==null || fmt==JPEG ) return Double.NaN;
      try {
         return getPixVal(pixelsOrigin,bitpix,y*width+x)*bScale+bZero;
      } catch( Exception e ) { return Double.NaN; }
   }

   /** Positionne la valeur du pixel en x,y en prenant en compte une valeur
    * en double */
   protected void setPixelOriginInDouble(int x,int y, double val) {

//      // On accède directement à la valeur sur disque
//      if( pixelsOriginFromDisk() ) {
//         if( onePixelOrigin==null ) onePixelOrigin = new byte[npix];
//         setPixVal(onePixelOrigin,bitpix,0, (val-bZero)/bScale);
//         setOnePixelToCache(onePixelOrigin,npix,x,y);
//
//      } else
      setPixVal(pixelsOrigin,bitpix, y*width+x, (val-bZero)/bScale);
   }
   
   /** Retourne le nom de la fonction de transition (asinh, log, sqrt, linear ou sqr) */
   static public String getTransfertFctInfo(int i) { return TRANSFERTFCT[i]; }
   public String getTransfertFctInfo() { return getTransfertFctInfo(transfertFct); }
   
   /** Retourne le code de la fonction de transition */
   static public int getTransfertFct(String s) { return Util.indexInArrayOf(s, TRANSFERTFCT,true); }

   /** Retourne les valeurs de pixel qui ont été mémorisées soit dans
    *  l'unité du fichier (INFILE) sinon en niveau d'énergie (REAL) */
   public String getDataMinInfo()  { return getSpecialPixel(dataMin); }
   public String getDataMaxInfo()  { return getSpecialPixel(dataMax); }
   public String getPixelMinInfo() { return getSpecialPixel(pixelMin); }
   public String getPixelMaxInfo() { return getSpecialPixel(pixelMax); }
   
   protected String getSpecialPixel(double x) {
      if( aladin.view.getPixelMode()==View.INFILE ) return X(x);
      return Y(x*bScale+bZero);
   }
   
   /** Retourne l'indice de la fonction de transfert courante */
   protected int getTransfertFct() { return transfertFct; }

   /** Retourne la valeur du pixel minimale pour le cut (bcale et bzero ont été déjà appliqué) */
   public double getPixelMin() { return pixelMin*bScale + bZero; }

   /** Retourne la valeur du pixel maximale pour le cut (bcale et bzero ont été déjà appliqué) */
   public double getPixelMax() { return pixelMax*bScale + bZero; }
   
   /** Retourne la valeur du pixel médiane (approximative) pour le cut (bcale et bzero ont été déjà appliqué) */
   public double getPixelMiddle() { return getInvPixel( cmControl[1] )*bScale + bZero; }

   /** Retourne le bitpix */
   protected int getBitpix() { return bitpix; }
   
   /** Retourne la valeur du cut min, sans appliquer le BSCALE et BZERO */
   public double getCutMin() {return pixelMin; }

   /** Retourne la valeur du cut max, sans appliquer le BSCALE et BZERO */
   public double getCutMax() {return pixelMax; }
   
   /** Retourne la plus petite valeur de pixel dans le fichier, sans appliquer le BSCALE et BZERO */
   public double getDataMin() { return dataMin; }

   /** Retourne la plus grande valeur de pixel dans le fichier, sans appliquer le BSCALE et BZERO */
   public double getDataMax() { return dataMax; }


   /** Retourne sous forme d'une chaine editable
    *  la valeur du pixel dans le mode courant en fonction
    *  de sa valeur en niveau de gris
    */
   protected String getPixelInfoFromGrey(int greyLevel) {
      return getPixelInfoFromGrey(greyLevel,aladin.view.getPixelMode());
   }
   protected String getPixelInfoFromGrey(int greyLevel,int mode) {
      if( greyLevel<0 || greyLevel>255 ) return "";
      switch(mode) {
          case View.LEVEL: return greyLevel+"";
          case View.INFILE: return X(getInvPixel(greyLevel));
          case View.REAL: return Y(getInvPixel(greyLevel)*bScale+bZero);
      }
      return null;
   }

   /**
    * Retourne la valeur du pixel dans l'unité du fichier (INFILE)
    * en supposant que le pixel indiqué a été exprimé dans l'unité
    * du sélecteur Pixel dans le cas REAL
    * @param s La valeur du pixel dans l'unité courante (sélecteur)
    * @return la valeur du pixel dans l'unité INFILE
    */
   protected double getPixelValue(String s) {
	  double pixel = Double.valueOf(s).doubleValue();
	  pixel = (pixel - bZero)/bScale;
	  return pixel;
   }

   /** Retourne la valeur 8 bits du pixel indiqué en coordonnées image*/
   public int getPixel8(int x,int y) {
      return (pixels[y*width+x] & 0xFF);
   }

   
   /** Retourne la valeur 8 bits du pixel indiqué en coordonnées image*/
   protected byte getPixel8Byte(int x,int y) {
      return pixels==null ? 0 : pixels[y*width+x];
   }


/** INUTILE POUR LE MOMENT
   protected double getPixelValue(int x,int y,int mode) {
      if( y<0 || y>=height || x<0 || x>=width ) return 0.;
      switch(mode) {
          case Pixel.LEVEL:  return (pixels[y*width+x] & 0xFF);
//          case Pixel.APPROX: return pixels[y*width+x]/fctPixCut+minPixCut;
          case Pixel.INFILE:
                 return pixelsOrigin==null?0.:
                    getPixVal(pixelsOrigin,bitpix,(height-y-1)*width+x);
          case Pixel.REAL:
                 return ( pixelsOrigin==null )?0.:
                    getPixVal(pixelsOrigin,bitpix,(height-y-1)*width+x)*bScale+bZero;
      }
      return 0.;
   }
*/

   /** Retourne la date d'observation de l'image si on la connait via le header fits,
    * sinon null
    */
   protected String getDateObs() {
      if( !hasFitsHeader() ) return null;
      String s = headerFits.getStringFromHeader("EPOCH");
      if( s==null ) headerFits.getStringFromHeader("DATE-OBS");
      else s="J"+s;
      return s;
   }

   /** Retourne true si on dispose d'une entête FITS */
   protected boolean hasFitsHeader() { return headerFits!=null; }

   /** Retourne true si on dispose (ou peut disposer) des pixels originaux */
   protected boolean hasOriginalPixels() {
      return fmt!=JPEG && (pixelsOrigin!=null || cacheID!=null);
   }

   /** Abandon de l'accès aux pixels d'origine */
   protected void noOriginalPixels() {
      pixelsOrigin=null;
      cacheID=null;
   }

   /** Retourne les pixels sur 8 bits format FITS
    *  (il faut inverser les lignes du buffer pixels
    */
   protected byte[] getFits8Pixels() {
      byte [] res = new byte[getBufPixels8().length];
      System.arraycopy(getBufPixels8(),0,res,0,getBufPixels8().length);
      invImageLine(width,height,res);
      return res;
   }

   /** Retourne les pixels originaux (format FITS) si possible, sinon null */
   synchronized protected byte[] getFitsPixels() {
      if( !hasOriginalPixels() ) return null;

      // Si les pixels sont en cache, on va les relire, mais laisser
      // à null la référence pixelsOrigin histoire de laisser quelque
      // chose de cohérent
      boolean flagClear=false;
      if( pixelsOrigin==null ) { getFromCache(); flagClear=true; }
      byte [] res = pixelsOrigin;
      if( flagClear ) pixelsOrigin=null;

      return res;
   }

  /** Retournement de l'image
   * @param methode 0-N/S, 1-D/G, 2-N/S+D/G
   */
   protected void flip(int methode) {
      setLockCacheFree(true);
      try {
         pixelsOriginFromCache();

         if( methode==0 || methode==2 ) { 
            invImageLine(width,height,getBufPixels8());
            if( pixelsOrigin!=null ) invImageLine(width,height,pixelsOrigin,npix);
         }
         if( methode==1 || methode==2 ) {
            invImageRow(width,height,getBufPixels8());
            if( pixelsOrigin!=null ) invImageRow(width,height,pixelsOrigin,npix);
         }

         if( pixelsOrigin!=null )  reUseOriginalPixels();
      } finally { setLockCacheFree(false); }

      calculPixelsZoom();
      aladin.calque.zoom.zoomView.repaint();

      if( Projection.isOk(projd) ) projd.flip(methode);

      changeImgID();
   }


  /** Retournement de l'image (inversion des lignes)
   * @param width largeur de l'image
   * @param height hauteur de l'image
   * @param pixels Tableau des pixels (en entree et en sortie)
   * @param bitpix nombre d'octets par pixel (1 par défaut)
   */
   protected static void invImageLine(int width, int height,byte [] pixels) { invImageLine(width,height,pixels,1); }
   protected static void invImageLine(int width, int height,byte [] pixels,int bitpix) {
      byte[] tmp = new byte[width*bitpix];
      for( int h=height/2-1; h>=0; h-- ) {
         int offset1=h*width *bitpix;
         int offset2=(height-h-1)*width *bitpix;
         System.arraycopy(pixels,offset1, tmp,0, width*bitpix);
         System.arraycopy(pixels,offset2, pixels,offset1, width*bitpix);
         System.arraycopy(tmp,0, pixels,offset2, width*bitpix);
      }
      tmp=null;
   }


   static byte tmp_inv [] = new byte[8];

  /** Retournement de l'image (Inversion des colonnes)
   * @param width largeur de l'image
   * @param height hauteur de l'image
   * @param pixels Tableau des pixels (en entree et en sortie)
   * @param bitpix nombre d'octets par pixel
   */
   protected static void invImageRow(int width, int height,byte [] pixels) { invImageRow(width,height,pixels,1); }
   protected static void invImageRow(int width, int height,byte [] pixels,int bitpix) {
      for( int h=0; h<height; h++) {
         int offset1=h*width *bitpix;
         for( int w=width/2-1; w>=0; w-- ) {
            int offset2=offset1+(width-w-1)*bitpix;
            System.arraycopy(pixels,offset1+w*bitpix,tmp_inv,0,bitpix);
            System.arraycopy(pixels,offset2,pixels,offset1+w*bitpix,bitpix);
            System.arraycopy(tmp_inv,0,pixels,offset2,bitpix);

//            tmp = pixels[offset1+w];
//            pixels[offset1+w]=pixels[offset2];
//            pixels[offset2]=tmp;
         }
      }
   }


   // Met dans un cache memoire l'image au format JPEG
   // afin d'eviter d'avoir systematiquement recours a l'URL
   // pour toutes les manipulations de cette image
   // On lui associe une table de couleurs (256 niveaux de gris)
   protected boolean cacheImageNatif(MyInputStream dis) {
      ImageProducer source=null;

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;


Aladin.trace(2,"Loading Native image");

         Image itmp = aladin.getToolkit().createImage(dis.readFully());
         aladin.waitImage(itmp);

         // Si je ne connais pas encore la taille de l'image (dans le cas d'un
         // chargement NATIF ne provenant pas du serveur Aladin
         if( width==0 ) {
            naxis1=width=itmp.getWidth(aladin);
            naxis2=height=itmp.getHeight(aladin);
         }
         source=itmp.getSource();

d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Waiting for server during "+temps+" ms");

      // Recuperation de l'image 8 bits
      setPourcent(10);
      pixelsOrigin = new byte[width*height];
      GreyMemory gm = new GreyMemory(source,pixelsOrigin);
      if( !gm.waitImage() ) return false;
      setPourcent(66);
d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Loading in "+temps+" ms");

      // conserver la prof de l'image a l'origine
      bitpix=8;
      npix=1;

      setBufPixels8(getPix8Bits(null,pixelsOrigin,bitpix,width,height,
            dataMinFits,dataMaxFits,aladin.configuration.getCMCut()));

d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Autocutting in "+temps+" ms");
      creatDefaultCM();
      setPourcent(99);

      // On calcule l'imagette du zoom
      calculPixelsZoom();

      return true;
   }

   // Recuperation d'une image FITS (via une URL)
   protected boolean cacheImageFits(URL u) throws Exception {
      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      try{
//         dis = new MyInputStream(u.openStream());
//         dis = dis.startRead();
         dis = Util.openStream(u);
      } catch( Exception e ) {
         sendLog("Error","cacheImageFits("+(u==null?"null":u.toString())+") ["+e+"]");
         return false;
      }
d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Waiting for server during "+temps+" ms");
      setFmt();
      return cacheImageFits(dis);
   }


   protected int offsetLoad=-1;		// Nombre d'octets deja charges
   protected long tailleLoad=-1;		// Nombre d'octets a charger


  /** Retourne le nombre de KB deja charge */
   protected String getProgress() {
      if( !flagOk && error==null && pourcent>=0 && tailleLoad>0 )
         return "  - "+((int)pourcent)+"% of "+(tailleLoad/1024)+"KB ...";
      return super.getProgress();

   }

//   /**
//    * Envoit un log indiquant le debit (source, volume, tmps) . IL doit s'agir
//    * d'une version standalone ou de l'applet avec la restriction que l'image
//    * doit provenir de aladin.u-strasbg.fr et que le serveur
//    * de l'applet soit egalement aladin.u-strasbg.fr (afin de ne pas mesurer
//    * les proxy)
//    * @param url Url de la source
//    * @param size Nombre d'octets
//    * @param ms Temps en millisecondes
//    */
//   protected void networkPerf(String url,int size,int ms) {
//      String host;
//      if( (host=Aladin.getSite(url))==null || size/1024<=0 ) return;
//      if( !aladin.STANDALONE && orig!=ALADIN ) return;
//      if( ms>0 ) Aladin.trace(3," => Throughput "+throughput((size/ms)*1000));
//      sendLog("Throughput",host+" "+(size/1024)+" KB "+ms+" ms");
//   }

   /**
    * Rechargement de l'image pour retrouver les pixels originaux s'ils
    * n'ont pas ete memorises.
    */
//   protected boolean reloadPixelsOrigin() {
//      if( pixelsOrigin!=null ) return true;
//      if( this instanceof PlanImageResamp || this instanceof PlanImageMosaic ) return false;
//      try {
//         if( fmt==JPEG ) cacheImageNatif(u,false);
//         else {
//            if (fmt != MRCOMP) {
//               if (!openUrlImage())
//                  throw new Exception("Problem");
//            }
//            if (fmt == FITS || fmt == HFITS || fmt == GFITS)
//               cacheImageFits(dis, false);
//         }
//      } catch (Exception e) {
//         System.out.println("Reload error : " + e);
//         sendLog("Error","reloadPixelsOrigin("+(u==null?"null":u.toString())+") ["+e+"]");
//         return false;
//      }
//      return true;
//   }
   
   protected String getBlankString() {
      return !isBlank ? "--" : Double.isNaN(blank) ? "NaN":""+blank;
   }
   
   /** Positionnement de la valeur du blank */
   protected void setBlankString(String b) {
      try {
         String s =b.trim();
         if( s.equalsIgnoreCase("NaN") ) blank = Double.NaN;
         else blank = Double.parseDouble(s);
         isBlank=true;
         if( pixMode==PIX_256 ) pixMode=PIX_255;
      } catch( Exception e ) {
         isBlank=false;
         blank=Double.NaN;
         if( pixMode==PIX_255 ) pixMode=PIX_256;
      }
      recut(pixelMin,pixelMax,false);
      restoreCM();
      changeImgID();
   }

   /**
    * Rejoue l'autocut en fonction d'un min et d'un max donnes
    * par l'utilisateur. Si les pixels d'origine ne sont pas memorises,
    * il y aura un nouveau chargement.
    * @param min pixel min
    * @param max pixel max
    * @param autocut true s'il faut appliquer l'algo d'autocut, sinon effectue
    *                simplement un changement d'echelle de [min..max] vers
    *                les 256 niveaux de gris
    * @return false si impossible de recharger les pixels d'origine
    */
    protected boolean recut() { return recut(pixelMin,pixelMax,false); };
    protected boolean recut(double min,double max,boolean autocut) {

       if( min==-1 && max==-1 ) { min=dataMinFits; max=dataMaxFits; }

       // Traitement particulier pour les grosses images
       if( pixelsOrigin==null && isBigImage() ) {
          int taille = width*height*npix;
          int offsetLoad=0;		// octets effectivement lus
          int len = 512;	    // taille des blocs par defaut  (frontière de mots)
          byte buf[];

          // Juste pour faire clignoter
          flagUpdating=true;
          flagOk=false;
          aladin.calque.select.repaint();

          try {
             int w=Math.min(1024,width);
             int h=Math.min(1024,height);
             int x = width/2  -w/2;
             int y = height/2 -h/2;
             buf = new byte[w*h*npix];
             getPixelsFromCache(buf,npix,x,y,w,h);
             findMinMax(buf,bitpix,w,h,min,max,autocut,0);
             min=pixelMin; max=pixelMax;

             buf = new byte[len];
             openCache();
             fCache.seek( cacheOffset );

             // Lecture par tranches
             while( offsetLoad<taille) {
                if( taille-offsetLoad<len ) len=taille-offsetLoad;
                fCache.readFully(buf,0,len);

                // Normalisation de la tranche
                to8bits(getBufPixels8(),offsetLoad/npix,buf,len/npix,bitpix,
                      /*isBlank,blank,*/min,max,false);

                offsetLoad+=len;
                setPourcent(offsetLoad*100./taille);
             }

          } catch( Exception e ) { e.printStackTrace(); }

          flagUpdating=false;
          flagOk=true;
          buf=null;	// pour aider gc
          aladin.gc();

       } else {
          if( !pixelsOriginFromCache() /* && !reloadPixelsOrigin() */ ) return false;
          setBufPixels8(getPix8Bits(getBufPixels8(),pixelsOrigin,bitpix,width,height,min,max,autocut));
       }

       freeHist();
       if( fmt!=JPEG ) invImageLine(width,height,getBufPixels8());
       changeImgID();
//       sendLog("RecutPixel","["+getLogInfo()+"]");

       setPourcent(-1);
       return true;
    }

  /** Calcul la marge necessaire pour une config memoire donnee
   * @param maxmem limite de la memoire en octets
   * @param width,height taille de l'image originale
   * @param n nombre d'octets par pixel
   * @return la marge a enlever en pixels pour que ca tienne en memoire, -1 si pb
   */
   private int getMarge(int maxmem,int width, int height, int n) {
      double a = 4, b=-2*(width+height), c=width*height-maxmem/n;
      double marge = (-b-Math.sqrt(b*b-4*a*c))/(2*a);
      marge += marge%n;
      if( marge<0 || marge>width/2 ) {
         marge=-1;	// Probleme !!
      }
      return (int)marge;
   }


   /** Cherche dans l'entête FITS les paramètres optionnels qui
    * peuvent servir    */
   protected void loadFitsHeaderParam(FrameHeaderFits headerfits) {

      // Y a-t-il une valeur BLANK
      try {
         int b =  headerFits.getIntFromHeader("BLANK");
         blank = b;
         isBlank=true;
Aladin.trace(3," => BLANK value = "+blank);
      } catch( Exception eblank ) { isBlank=false; }

      // Y a-t-il un BZERO
      try {
         bZero  =  headerFits.getDoubleFromHeader("BZERO");
Aladin.trace(3," => BZERO = "+bZero);
      } catch( Exception ebzero ) { bZero=0.;}
      
      // Y a-t-il un BSCALE
      try {
         bScale =  headerFits.getDoubleFromHeader("BSCALE");
Aladin.trace(3," => BZERO = "+bZero+" BSCALE = "+bScale);
      } catch( Exception ebscale ) { bScale=1.; }

//      // Y a-t-il des valeurs DATAMIN et DATAMAX
//      try {
//         dataMin =  headerFits.getDoubleFromHeader("DATAMIN");
//         dataMax =  headerFits.getDoubleFromHeader("DATAMAX");
//Aladin.trace(3," => DATAMIN = "+dataMin+" DATAMAX = "+dataMax);
//
//         // Conversion en valeurs stockées
//         dataMin = (dataMin - bZero)/bScale;
//         dataMax = (dataMax - bZero)/bScale;
//      } catch( Exception ebzero ) { dataMin=dataMax=0.; }

      // Y a-t-il des valeurs GOODMIN et GOODMAX (Kepler)
      try {
         dataMinFits =  headerFits.getDoubleFromHeader("GOODMIN");
         dataMaxFits =  headerFits.getDoubleFromHeader("GOODMAX");
         dataMaxFits=150000;
         Aladin.trace(3," => GOODMIN = "+dataMinFits+" GOODMAX = "+dataMaxFits);

         // Conversion en valeurs stockées
         dataMinFits = (dataMinFits - bZero)/bScale;
         dataMaxFits = (dataMaxFits - bZero)/bScale;
      } catch( Exception ebzero ) { dataMinFits=dataMaxFits=0.; }

      // Y a-t-il un nom particulier (Fits extension)
      if( label==null || label.length()==0 ) {
         try {
            String name =  headerFits.getStringFromHeader("EXTNAME");
            if( name!=null && name.length()>0 ) setLabel(name);
         } catch( Exception ename ) {}
      }
   }

   /** Retourne true s'il s'agit d'une grosse image et qu'on peut y accéder en local */
   protected boolean isBigImage() {
      return width*height*npix>Aladin.LIMIT_PIXELORIGIN_INMEM && cacheID!=null;
   }

   /**
    * Recuperation d'une image FITS
    * Charge l'image FITS en lisant tout d'abord le header (via HeaderFits)
    * puis les pixels.
    * Apres lecture, les pixels vont etre "autocutes" par un
    * algo maison puis memorises en 8 bits (pixels[]). Les pixels d'origine
    * sont conservés, ou en cache en mode standalone (pixelsOrigin[])
    * afin de faire des economies de memoire.
    * Si l'image est très grosse, on précède par étapes.
    * @param dis le flux des donnees
    */
   protected boolean cacheImageFits(MyInputStream dis) throws Exception {

      int naxis = 2;
      int i;
      int taille;		// nombre d'octets a lire
      int n;			// nombre d'octets pour un pixel

Aladin.trace(2,"Loading FITS image");

      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);

      bitpix = headerFits.getIntFromHeader("BITPIX");
      naxis = headerFits.getIntFromHeader("NAXIS");

      // Il s'agit juste d'une entête FITS indiquant des EXTENSIONs
      if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
         error="_HEAD_XFITS_";

         // Je saute l'éventuel baratin de la première HDU
         if( naxis==1 ) {
            try {
               naxis1 = headerFits.getIntFromHeader("NAXIS1");
               dis.skip(naxis1);
            } catch( Exception e ) { e.printStackTrace(); }
         }

         return false;
      }

      width = naxis1 = headerFits.getIntFromHeader("NAXIS1");
      height = naxis2 = headerFits.getIntFromHeader("NAXIS2");

      if (naxis <= 1 || width<=0 || height<=0) {
         error=aladin.error=ONEDIM;
         Aladin.warning(error,1);
         close();
         return false;
      }
      if (bitpix==0) {
         error=aladin.error="FITS format error: BITPIX=0 !";
         Aladin.warning(error,1);
         close();
         return false;
      }

      npix = n = Math.abs(bitpix)/8;	// Nombre d'octets par valeur
      taille=width*height*n;	// Nombre d'octets
      setPourcent(0);
Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" BITPIX="+bitpix+" => size="+taille);

      // Les paramètres FITS facultatifs
      loadFitsHeaderParam(headerFits);

      // Gestion du cache à partir du fichier d'origine (si possible)
      setCacheFromFile(dis);

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      // Allocation du buffer d'arrivée
      tailleLoad=taille;	// nombres d'octets a lire
      boolean cut = aladin.configuration.getCMCut();

      setBufPixels8(new byte[width*height]);

      // Lecture HCompress
      if( (dis.getType() & MyInputStream.HCOMP) !=0 ) {
Aladin.trace(2,"Hdecompressing");
         fmt=HFITS;			// On force le format
         pixelsOrigin=Hdecomp.decomp(dis);
         d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Hdecompressing in "+temps+" ms");
         findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0);
         to8bits(getBufPixels8(),offsetLoad/npix,pixelsOrigin,width*height,bitpix,
               /*isBlank,blank,*/pixelMin,pixelMax,true);

      // Il s'agit d'une image MEF que l'on ne va pas garder, on se contente de skipper l'image
      } else if( flagSkip ) { dis.skip( taille );

      } else {


         // Grande image avec accès disque, optimisation du buffer
         if( isBigImage() ) {

            // Lecture directe d'un bloc central de l'image de 1024*1024 pour détermination min/max
            int w=Math.min(1024,width);
            int h=Math.min(1024,height);
            int x = width/2  -w/2;
            int y = height/2 -h/2;
            byte buf [] = new byte[w*h*npix];
            getPixelsFromCache(buf,npix,x,y,w,h);
            findMinMax(buf,bitpix,w,h,dataMinFits,dataMaxFits,cut,0);

            offsetLoad=0;		// octets effectivement lus

            int len=512;		// Taille des blocs - Optimisation pour la lecture
            buf = new byte[len];

            // Lecture par tranches
            while( offsetLoad<taille) {
               if( taille-offsetLoad<len ) len=taille-offsetLoad;
               dis.readFully(buf,0,len);

               // Normalisation de la tranche
               to8bits(getBufPixels8(),offsetLoad/npix,buf,len/npix,bitpix, pixelMin,pixelMax,true);

               offsetLoad+=len;
               setPourcent(offsetLoad*99./taille);
            }

         // Chargement dans une unique buffer pour Petite image ou pas d'accès disque
         } else {
            pixelsOrigin = new byte[taille];

            // Lecture par bloc pour afficher une progresse
            offsetLoad=0;      // octets effectivement lus
            int len = taille/100;  // taille des blocs par defaut
            if( len<512 ) len=512;

            // Lecture par tranches pour permettre l'affichage de la progression
            try {
               while( offsetLoad<taille) {
                  if( taille-offsetLoad<len ) len=taille-offsetLoad;
                  dis.readFully(pixelsOrigin,offsetLoad,len);
                  offsetLoad+=len;
                  setPourcent(offsetLoad*85./taille);
               }
            } catch( Exception e ) {
               error=aladin.error="Loading error: "+e.getMessage();
               e.printStackTrace();
               close();
               return false;
            }

            findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0);
            to8bits(getBufPixels8(),0,pixelsOrigin,width*height,bitpix,
                  /*isBlank,blank,*/pixelMin,pixelMax,true);
         }
      }

      // On se recale si jamais il y a encore une extension FITS qui suit
      if( naxis>2 ) {
         try {
            long offset=n*naxis1*naxis2;
            for( i=2; i<naxis; i++ ) offset *= headerFits.getIntFromHeader("NAXIS"+(i+1));
            offset -= n*naxis1*naxis2;
            dis.skip(offset);
         } catch( Exception e ) { e.printStackTrace(); return false; }
      }

      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
      if( flagSkip ) return true;

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Reading "+(cut?"and autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");

      // Retournement de l'image (les lignes ne sont pas rangees dans le meme ordre
      // en FITS et en JAVA
      invImageLine(width,height,getBufPixels8());

      creatDefaultCM();
      setPourcent(99);
      return true;
   }
   
   
   // BIEN TENTE, MAIS MALHEUREUSEMEN CA NE MARCHE PAS. LES IMAGES PDS IMQ UTILISENT UN CODE DE HUFFMAN CODé SUR MESURE
   // VOIR LES SOURCES MOCUNCOMPRESS 
   private void mocDecode(byte [] src,byte [] target) throws Exception {
      Inflater decompresser = new Inflater();
      decompresser.setInput(src);
      decompresser.inflate(target);
      decompresser.end();
   }

   /**
    * Recuperation d'une image PDS
    * Charge l'image PDS en lisant tout d'abord le header (via HeaderFits)
    * puis les pixels.
    * Apres lecture, les pixels vont etre "autocutes" par un
    * algo maison puis memorises en 8 bits (pixels[]). Les pixels d'origine
    * sont conservés, ou en cache en mode standalone (pixelsOrigin[])
    * afin de faire des economies de memoire.
    * Si l'image est très grosse, on précède par étapes.
    * @param dis le flux des donnees
    */
   protected boolean cacheImagePDS(MyInputStream dis) throws Exception {

      int tailleImg;       // nombre d'octets de l'image
      int taille;          // nombre d'octets à lire

Aladin.trace(2,"Loading PDS image");

      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderPDS(this,dis);
      
      // Taille image, profondeur pixel
      width = naxis1 = headerFits.getIntFromHeader("LINE_SAMPLES");
      height = naxis2 = headerFits.getIntFromHeader("LINES");
      npix = headerFits.getIntFromHeader("SAMPLE_BITS")/8;
      bitpix = npix*8;
      String mode = headerFits.getStringFromHeader("SAMPLE_TYPE");
      if( mode.indexOf("REAL")>=0 ) bitpix=-bitpix;
      taille=tailleImg=width*height*npix;    // Nombre d'octets
      
      // Bscale et BZero ?
      try { bScale = headerFits.getDoubleFromHeader("SCALING_FACTOR"); }
      catch( Exception e1 ) { bScale=1; }
      try { bZero = headerFits.getDoubleFromHeader("OFFSET"); }
      catch( Exception e1 ) { bZero=0; }
      
      // détermination de l'offset de l'image
      int recordBytes = headerFits.getIntFromHeader("RECORD_BYTES");
      int offset,skip;
      String s = headerFits.getStringFromHeader("^IMAGE");
      if( (offset=s.indexOf("BYTE"))>0 ) skip = Integer.parseInt(s.substring(0,offset)); 
      else skip = ( headerFits.getIntFromHeader("^IMAGE") -1)*recordBytes;
      dis.skip(skip-dis.getPos());
      
      setPourcent(0);
      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" BITPIX="+bitpix+" => size="+taille+" image offset="+skip);

      // Compression ?
      boolean isCompressed = headerFits.getStringFromHeader("ENCODING_TYPE")!=null;
      if( isCompressed ) {
         int fileSize = headerFits.getIntFromHeader("FILE_RECORDS") * recordBytes;
         taille = fileSize-skip;
         
      // Gestion du cache à partir du fichier d'origine (si possible)
      } else setCacheFromFile(dis);

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      // Allocation du buffer d'arrivée
      tailleLoad=taille;    // nombres d'octets a lire
      boolean cut = aladin.configuration.getCMCut();

      setBufPixels8(new byte[width*height]);
      pixelsOrigin = new byte[taille];

      // Lecture par bloc pour afficher une progresse
      offsetLoad=0;      // octets effectivement lus
      int len = taille/100;  // taille des blocs par defaut
      if( len<512 ) len=512;

      // Lecture par tranches pour permettre l'affichage de la progression
      try {
         while( offsetLoad<taille) {
            if( taille-offsetLoad<len ) len=taille-offsetLoad;
            dis.readFully(pixelsOrigin,offsetLoad,len);
            offsetLoad+=len;
            setPourcent(offsetLoad*85./taille);
         }
      } catch( Exception e ) {
         error=aladin.error="Loading error: "+e.getMessage();
         e.printStackTrace();
         close();
         return false;
      }
      
      // Decompression
      if( isCompressed ) {
         byte [] pixelsOrigin1 = new byte[tailleImg];
         mocDecode(pixelsOrigin, pixelsOrigin1);
         setPourcent(92);
         pixelsOrigin=pixelsOrigin1;
      }
      
      findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0);
      to8bits(getBufPixels8(),0,pixelsOrigin,width*height,bitpix,
            /*isBlank,blank,*/pixelMin,pixelMax,true);
      
      // Retournement de l'image (les lignes ne sont pas rangees dans le meme ordre
      // en PDS et en JAVA
      invImageLine(width,height,getBufPixels8());

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Reading "+(isCompressed?", uncompressing ":"")+(cut?", autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");

      creatDefaultCM();
      setPourcent(99);
      return true;
   }
   
  /** Creation d'une table de couleurs par defaut */
   protected void creatDefaultCM() {
      cm = ColorMap.getCM(0,128,255,aladin.configuration.getCMVideo()==VIDEO_INVERSE,
                                    aladin.configuration.getCMMap(),
                                    aladin.configuration.getCMFct(),isTransparent());
   }
   

  /** Affectation d'une nouvelle table des couleurs
   * a l'image du plan
   * @param cm la nouvelle table des couleurs
   */
   protected void setCM(Object cm) {
      this.cm = (ColorModel)cm;
      changeImgID();

      if( Aladin.ISLINUX ) aladin.view.recreateMemoryBufferFor(this);  // Pour palier au bug linux
//      aladin.view.endDynamicCM();
   }

   /**
    * Traitement des paramètres de changement de l'autocut et/ou de la palette
    * @param s  gray|bb|A|stern   | log|sqrt|linear|pow2
    *         | noreverse|reverse | autocut|noautocut
    *         | all|minpix..maxpix
    * @return true si l'opération a été effectuée, false sinon
    */
   public boolean setCmParam(String s) {
      int i;
      boolean flagCM=false,flagPixel=false;
      double minPix = pixelMin; // par défaut, on reprend le min/max du dernier cut
      double maxPix = pixelMax; // --
//      boolean autocut=true;      // par défaut on applique l'autocut
      boolean autocut=false;

      Tok tok = new Tok(s);

      while( tok.hasMoreTokens() ) {
         s= tok.nextToken();

         // S'agit-il d'un nom d'une palette prédéfine ?
         if( (i=Util.indexInArrayOf(s,FrameColorMap.CMA,true))!=-1 ) {
            if( i!=typeCM ) { typeCM=i; flagCM=true; }

            // S'agit-il d'un nom de palette additionnelle ?
         } else if( ColorMap.customCMName!=null &&
               (i=ColorMap.customCMName.indexOf(s))>=0 ) {
            if( typeCM!=ColorMap.LAST_DEFAULT_CM_IDX+1+i ) { typeCM = ColorMap.LAST_DEFAULT_CM_IDX+1+i; flagCM=true; }

            // S'agit-il d'une fonction de transfert ?
         } else if( (i=Util.indexInArrayOf(s, TRANSFERTFCT, true))>=0 ) {
            if( i!=transfertFct ) { transfertFct=i; flagCM=true; }

            // S'agit-il d'un reverse ?
         } else if( s.equalsIgnoreCase("reverse") || s.equalsIgnoreCase("inverse") ) {
            if( video!=VIDEO_INVERSE ) { video=VIDEO_INVERSE; flagCM=true; }

            // S'agit-il d'un noreverse ?
         } else if( s.equalsIgnoreCase("noreverse") ) {
            if( video!=VIDEO_NORMAL ) { video=VIDEO_NORMAL; flagCM=true; }

            // Faut-il appliquer un autoCut ?
         } else if( s.equalsIgnoreCase("autocut") || s.equalsIgnoreCase("cut")) {
            autocut=true;
            flagPixel=true;

            // Faut-il ne pas appliquer un autocut ?
         } else if( s.equalsIgnoreCase("noautocut") || s.equalsIgnoreCase("nocut")) {
            autocut=false;
            flagPixel=true;

            // Faut-il prendre tous les pixels ?
         } else if( s.equalsIgnoreCase("all") ) {
            minPix=this.dataMin;
            maxPix=this.dataMax;
            flagPixel=true;

            // Faut-il prendre un interval de pixels ?
         } else if( (i=s.indexOf(".."))>0 ) {
            try {
               int sgn=1;
               int deb=0;
               if( s.charAt(deb)=='-' ) { deb++; sgn=-1; }
               minPix=sgn*Double.parseDouble(s.substring(deb,i));
               minPix = (minPix - bZero)/bScale;
               
               sgn=1; deb=i+2;
               if( s.charAt(deb)=='-' ) { deb++; sgn=-1; }
               maxPix=sgn*Double.parseDouble(s.substring(deb));
               flagPixel=true;
               maxPix = (maxPix - bZero)/bScale;
            } catch( Exception e ) { e.printStackTrace(); }
         }
      }

      // On doit changer la palette !
      if( flagCM ) {
         IndexColorModel cm = ColorMap.getCM(
               cmControl[0], cmControl[1],cmControl[2],
               video==VIDEO_INVERSE,typeCM,transfertFct);
         setCM(cm);
      }

      // On doit changer les pixels !
      if( flagPixel) recut(minPix,maxPix,autocut);

      // on a rien pu changer !!
      if( !flagCM && !flagPixel ) return false;

      if( aladin.frameCM!=null ) aladin.frameCM.majCMByScript(this);
      changeImgID();
      return true;
   }
   
   /**Inversion de la colormap (uniquement en niveaux de gris) */
   public void reverse() {
      if( !(cm instanceof IndexColorModel) ) return;

      video = video==PlanImage.VIDEO_INVERSE ? PlanImage.VIDEO_NORMAL :PlanImage.VIDEO_INVERSE;
      IndexColorModel cm = ColorMap.getCM(
            cmControl[0], cmControl[1],cmControl[2],
            video==PlanImage.VIDEO_INVERSE,typeCM,transfertFct);
      setCM(cm);
   }

   /** Retourne true s'il s'agit du planImage de base */
   protected boolean isPlanBase() { return aladin.calque.getPlanBase()==this; }

  /** Manipulation dynamique de la table des couleurs.
   * Permet de modifier dans une portion de l'image la table des couleurs
   * afin de se rendre compte du rendu
   * @param cm la table des couleurs courante
   */
//   protected void newCM(Object cm) { aladin.view.dynamicCM(cm); }

  /** Dessin des bordures de l'image
   * @param g Le contexte graphique
   * @param dx,dy Offset en cas d'impression
   */
   protected void drawBord(Graphics g,ViewSimple v,int dx,int dy,float opacite,boolean aplat) {

      Projection projv;
      if( v.isFree()  || !Projection.isOk(projv=v.getProj()) ) return;

      g.setColor(Color.red);

      Coord c;
      try { c = projd.c.getImgCenter(); } catch( Exception e ) { return; }
      double x = c.x;
      double y = c.y;

      Coord ct = new Coord();
      int polX[] = new int[4];
      int polY[] = new int[4];
      for( int i=0; i<4; i++ ) {
         int sgnX=(i<2)?-1:1;
         int sgnY=(i==1 || i==2 )?1:-1;
         ct.x = c.x+sgnX*x;	 // X d'un coin dans la calib courante
         ct.y = c.y+sgnY*y;	 // Y du coin dans la calib courante
         projd.getCoord(ct); // Calcul alpha,delta dans la calib courante
         if( Double.isNaN(ct.al) ) return;
         projv.getXY(ct);     // Calcul X,Y dans la calib de ref
         if( Double.isNaN(ct.x) ) return;
         Point p = v.getViewCoord(ct.x,ct.y); // Calcul X,Y dans la View
         if( p==null ) return;
         polX[i]=p.x+dx;
         polY[i]=p.y+dy;
         if( i==0) {
            g.setFont(Aladin.SSPLAIN);
            g.drawString(label,p.x+dx,p.y-2+dy);
         }
      }

      // Traçage en transparence ?
      if( aplat && opacite!=0 && Aladin.ENABLE_FOOTPRINT_OPACITY && g instanceof Graphics2D ) {
         Graphics2D g2d=(Graphics2D)g;
         Composite saveComposite=g2d.getComposite();
         // TODO : faudrait il forcer la valeur (avec peu d'opacité) ?
         Composite myComposite = Util.getFootprintComposite(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL*opacite);
         g2d.setComposite(myComposite);
         g2d.fillPolygon(polX,polY, 4);
         g2d.setComposite(saveComposite);

      } /*else*/ g.drawPolygon(polX,polY, 4);
   }

   /** Dessin de l'image via une transformée affine, mais en utilisant
    * deux triangles complémentaires alignés sur la diagonale + clip en conséquence.
    */

//  3     0
// 2 1   1 3
//  0     2
//   protected void draw(Graphics g,ViewSimple v,int dx, int dy,float op) {
//      if( v==null ) return;
//      long t1=Util.getTime();
//      if( op==-1 ) op=getOpacityLevel();
//      try {
//         if(  op<=0.1 ) return;
//         if( !projd.agree(projd, v) ) return;
//         if( !(g instanceof Graphics2D) ) return;
//
//         boolean flagRectangle=false;
//
//         Graphics2D g2d = (Graphics2D)g;
//         Composite saveComposite = g2d.getComposite();
//         AffineTransform saveTransform = g2d.getTransform();
//
//         PointD[] b = getBords(v);
////         b[0].y-=30;
//
//         try {
//            // Test  derrière le ciel
//            if( (b[3].x-b[2].x)*(b[1].y-b[2].y) - (b[1].x-b[2].x)*(b[3].y-b[2].y) >=0 ) return;
//
//            // Test dessin 1 rectangle plutôt que 2 triangles
//            if( (int)b[2].x==(int)(b[3].x+b[1].x-b[0].x)
//                  && (int)b[2].y == (int)(b[1].y+b[3].y-b[0].y) ) flagRectangle=true;
//
//            if( op<0.9 ) {
//               Composite myComposite = Util.getImageComposite(op);
//               g2d.setComposite(myComposite);
//            }
//
//            Image img = getImage();
//            drawTriangle(g2d, img, width, height, b, v, 0,false && !flagRectangle);
//            g2d.setTransform(saveTransform);
//            if( !flagRectangle ) {
//               drawTriangle(g2d, img, width, height, b, v, 2, true);
//               g2d.setTransform(saveTransform);
//            }
//
//            g2d.setClip(null);
//            g2d.setComposite(saveComposite);
//            long t2 = Util.getTime();
//            statTimeDisplay=t2-t1;
//         } catch( Exception e ) {
//            g2d.setTransform(saveTransform);
//            g2d.setComposite(saveComposite);
//         }
////         drawControl(g,b);
//      } catch( Exception e1 ) { }
//
//   }
//
//   /** Traçage d'un des triangles (voir draw())
//    * @param b les 4 coins dans l'ordre 0-1-2-3
//    * @param h Numéro du sommet du triangle
//    * @param clip false => traçage du rectangle sans clip
//    */
//   private void drawTriangle(Graphics2D g2d, Image img, int width, int height,
//         PointD []b, ViewSimple v, int h, boolean clip) {
//      int d,g;
//      switch(h) {
//         case 0:   d=3; g=1; break;
//         default : d=1; g=3; break;
//      }
//      if( b[d]==null || b[g]==null ) return;
//
//      if( clip ) {
//         Polygon p = new Polygon(new int[]{ (int)b[h].x,(int)b[d].x,(int)b[g].x},
//            new int[]{ (int)b[h].y,(int)b[d].y,(int)b[g].y}, 3);
//         g2d.setClip(p);
//      }
//
//      // On tourne l'image pour l'aligner sur h-d
//      double hdx = b[d].x - b[h].x;     if( h==2 ) hdx= -hdx;
//      double hdy = b[d].y - b[h].y;     if( h==2 ) hdy= -hdy;
//      double angle = Math.atan2(hdy,hdx);
//
//      // On écrase la longueur
//      double hd = Math.sqrt( hdx*hdx + hdy*hdy );
//      double mx= hd/width;
//      if( projd.sensDirect()!=v.pref.projd.sensDirect() ) mx=-mx;
//
//      // On écrase la hauteur
//      double hgx = b[g].x - b[h].x;    if( h==2 ) hgx= -hgx;
//      double hgy = b[g].y - b[h].y;    if( h==2 ) hgy= -hgy;
//      double hg = Math.sqrt( hgx*hgx + hgy*hgy );
//      double angle1 = Math.atan2(hgy,hgx);
//      double anglehg = angle1 - angle;
//      double my= hg*Math.sin(anglehg)/height;
//
//      // On fait glisser selon les x pour longer l'axe d-h
//      double sx = hg*Math.cos(anglehg) /hd;
//
////System.out.println("angle="+Math.toDegrees(angle)+"\n" +
////      "angle1="+Math.toDegrees(angle1)+"\n" +
////      "anglehd="+Math.toDegrees(anglehg)+"\n" +
////		"hd="+hd+"\n" +
////        "hg="+hg+"\n" +
////        "mx="+mx+"\n" +
////        "my="+my+"\n" +
////        "sx="+sx+"\n");
//
//      AffineTransform tr = new AffineTransform();
//      if( h==2 ) tr.translate((int)(b[d].x+b[g].x-b[h].x), (int)(b[d].y+b[g].y-b[h].y));
//      else tr.translate((int)b[h].x,(int)b[h].y);
//      tr.rotate(angle);
//      tr.scale(mx,my);
//      tr.shear(sx,0);
//      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
////            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//      //RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//              RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//      g2d.setTransform(tr);
//      g2d.drawImage(img,0,0,null);
//   }
//
//   private void drawControl(Graphics g,PointD b[]) {
//      g.setColor(Color.green);
//      g.drawLine((int)b[0].x,(int)b[0].y,(int)b[1].x,(int)b[1].y);
//      g.drawLine((int)b[1].x,(int)b[1].y,(int)b[2].x,(int)b[2].y);
//      g.drawLine((int)b[2].x,(int)b[2].y,(int)b[3].x,(int)b[3].y);
//      g.drawLine((int)b[3].x,(int)b[3].y,(int)b[0].x,(int)b[0].y);
//      g.setColor(Color.red);
//      for( int i=0; i<4; i++ ) g.drawString(i+"",(int)b[i].x,(int)b[i].y);
//   }


   /** Dessin de l'image par transformée affine
    * @param op niveau d'opacité forcé, -1 si prendre celui du plan
    */
   protected void draw(Graphics g,ViewSimple v,int dx, int dy,float op) {
      if( v==null ) return;
      long t1=Util.getTime();
      if( op==-1 ) op=getOpacityLevel();
      
      Graphics2D g2d =null;
      Composite saveComposite=null;
      AffineTransform saveTransform=null;
      try {
         if(  op<=0.1 ) return;
         if( !projd.agree(projd, v) ) return;
         if( !(g instanceof Graphics2D) ) return;

         g2d = (Graphics2D)g;
         saveComposite = g2d.getComposite();
         saveTransform = g2d.getTransform();
         if( op<0.9 ) {
            Composite myComposite = Util.getImageComposite(op);
            g2d.setComposite(myComposite);
         }

         AffineTransform tr = getAffineTransform(v);
         if( tr==null ) {
            g2d.setComposite(saveComposite);
            return;
         }

         g2d.setTransform(tr);
         g2d.drawImage(getImage(v,false),dx,dy,aladin);

         long t2 = Util.getTime();
         statTimeDisplay=t2-t1;
      }catch( Exception e ) { 
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      } finally { 
         if( g2d!=null && saveComposite!=null ) g2d.setComposite(saveComposite);
         if( g2d!=null && saveTransform!=null ) g2d.setTransform(saveTransform);
      }
   }

   /** récupération de la transformée affine correspondant au tracé sur v */
   protected AffineTransform getAffineTransform(ViewSimple v) {
      if( v==null ) return null;
       PointD[] b = getBords(v);

       // Test  derrière le ciel
// SANS DOUTE FAUDRAIT-IL PRENDRE EN COMPTE LE SENS POSITIF OU NEGATIF (sensDirect()... A TESTER)
//       if( b==null || (b[3].x-b[2].x)*(b[1].y-b[2].y) - (b[1].x-b[2].x)*(b[3].y-b[2].y) >=0 ) return null;
       if( b==null ) return null;

       // On tourne l'image pour l'aligner sur 0-3
       double b03x = b[3].x - b[0].x;
       double b03y = b[3].y - b[0].y;
       double angle = Math.atan2(b03y,b03x);

       // On écrase la longueur
       double d03 = Math.sqrt( b03x*b03x + b03y*b03y );
       double mx= d03/width;

       // On écrase la hauteur
       double b01x = b[1].x - b[0].x;
       double b01y = b[1].y - b[0].y;
       double d01 = Math.sqrt( b01x*b01x + b01y*b01y );
       double angle01 = Math.atan2(b01y,b01x) - angle;
       double my= (d01*Math.sin(angle01))/height;

       // On fait glisser selon les x pour longer l'axe 0-1
       double sx = ( d01*Math.cos(angle01) )/ d03;

       AffineTransform tr = new AffineTransform();
       tr.translate(b[0].x, b[0].y);
       tr.rotate(angle);
       tr.scale(mx,my);
       tr.shear(sx,0);

       return tr;
   }

   /** Retourne les coordonnées X,Y des 4 coins de l'image dans la vue v
    * ou null si problème */
   protected PointD[] getBords(ViewSimple v) {
      Projection projv;
      if( v.isFree() || !Projection.isOk(projv=v.getProj()) ) return null;

      Coord c;
      try { c = projd.c.getImgCenter(); } catch( Exception e ) { return null; }
      double x = c.x;
      double y = c.y;

      Coord ct = new Coord();
      PointD p[] = new PointD[4];    //    O     3
      for( int i=0; i<4; i++ ) {     //
         int sgnX=(i<2)?-1:1;        //    1     2
         int sgnY=(i==1 || i==2 )?1:-1;
         ct.x = c.x+sgnX*x;     // X d'un coin dans la calib courante
         ct.y = c.y+sgnY*y;     // Y du coin dans la calib courante
         projd.getCoord(ct); // Calcul alpha,delta dans la calib courante
         if( Double.isNaN(ct.al) ) return null;
         projv.getXY(ct);     // Calcul X,Y dans la calib de ref
         if( Double.isNaN(ct.x) ) return null;
         p[i] = v.getViewCoordDble(ct.x,ct.y); // Calcul X,Y dans la View

      }
      return p;
   }

   protected Point getCenter(Graphics g,ViewSimple v,int dx,int dy) {
       Plan pr = v.pref;
       if( pr==null || !Projection.isOk(pr.projd) ) return null;
       Projection proj = pr.projd;

       g.setColor(v.getInfoColor());

       Coord c;
       try { c = projd.c.getImgCenter(); } catch( Exception e ) { return null; }
       projd.getCoord(c);
       proj.getXY(c);
       return  v.getViewCoord(c.x, c.y);
   }

   protected void setBufPixels8(byte [] pixels) {
      this.pixels = pixels;
   }

   protected byte [] getBufPixels8() {
      return pixels;
   }

   protected void setPixels(byte [] pixels) {
      this.pixels = pixels;
   }
   
   protected String getBookmarkCode() {
      String s= super.getBookmarkCode();
      if( s==null ) return null;
      if( typeCM!=CMGRAY ) {
         s+="\ncm "+ColorMap.getCMName(typeCM);
         if( video==PlanImage.VIDEO_NORMAL ) s+=" noreverse";
         if( transfertFct!=LINEAR) s+=" "+TRANSFERTFCT[transfertFct];
      }
      return s;
   }

   
   /** Retourne le tableau des pixels 8 bits qui prennent en compte la table des couleurs */
   protected byte[] getLinearPixels8() { return getLinearPixels8(null); }
   protected byte[] getLinearPixels8(byte [] buf) {
      if( buf==null ) buf = new byte[width*height];
      for( int i=0; i < buf.length; i++ ) {
         buf[i]=(byte) getGreyPixel8(cm,pixels[i]);
      }
      return buf;
   }
   
   /** Conversion en niveau de gris (byte) d'un pixel de l'affichage (en prenant en compte la tables
    * des couleurs */
   static public byte getGreyPixel8(ColorModel cm,byte pix) {
      int p = 0xff & (int) pix;
      int red   = cm.getRed(p);
      int green = cm.getGreen(p);
      int blue  = cm.getBlue(p);
      return (byte) ( red*0.299  + green*0.587 + blue*0.114 );
   }
   
   public void setBitpix(int b) {
	   bitpix = b;
   }

}

/*
// CETTE CLASSE DOIT ETRE COMMENTEE SI ON NE VEUT PLUS SUPPORTER MRCOMP
class MrDecomp {
   MrDecomp(byte []a) {
      System.out.println("Creation de l'objet MrDecomp avec buffer de "+a.length+" octets");

   }

   int getWidth() { return(500); }
   int getHeight() { return(500); }
   int getPixels(byte [] pixels) { return 1; }
}
*/
