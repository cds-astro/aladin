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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import cds.fits.HeaderFits;
import cds.image.Bzip2;
import cds.tools.Util;
import cds.xml.TableParser;

/**
 * Plan dedie au stream.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.5 : (dec 2014) nettoyage
 * @version 1.4 : (oct 2006) support de la calib dans l'entête JPEG
 * @version 1.3 : (été 2006) support de Sectractor
 * @version 1.2 : (mai 2005) correction bug inTSV() et suppression AJSx
 * @version 1.1 : (avril 2005) correction bug lookForSignature()
 * @version 1.1 : (fév 2005) GIF
 * @version 1.0 : (16 juin 2003) creation
 */
public final class MyInputStream extends FilterInputStream {

   // La taille des blocs du tampon
   private static final int BLOCCACHE = 65536;

   // Liste des differents types reconnus par la classe
   static final public long UNKNOWN = 0;
   static final public long FITS    = 1;
   static final public long JPEG    = 1<<1;
   static final public long GIF 	= 1<<2;
   static final public long MRCOMP  = 1<<3;
   static final public long HCOMP   = 1<<4;
   static final public long GZ 	    = 1<<5;
   static final public long XML     = 1<<6;
   static final public long ASTRORES= 1<<7;
   static final public long VOTABLE = 1<<8;
   static final public long AJ 	    = 1<<9;
   static final public long AJS 	= 1<<10;
   static final public long IDHA    = 1<<11;
   static final public long SIA_SSA = 1<<12; // SIA ou SSA
   static final public long CSV 	= 1<<13;
   static final public long NOTAVAILABLE= 1<<14;
   static final public long AJSx    = 1<<15;
   static final public long PNG     = 1<<16;
   static final public long XFITS   = 1<<17;
   static final public long FOV	    = 1<<18;
   static final public long FOV_ONLY= 1<<19;
   static final public long CATLIST = 1<<20;
   static final public long RGB     = 1<<21;
   static final public long BSV     = 1<<22;
   static final public long FITST   = 1<<23;
   static final public long FITSB   = 1<<24;
   static final public long CUBE    = 1<<25;
   static final public long SEXTRA  = 1<<26;
   static final public long HUGE    = 1<<27;
   static final public long AIPSTABLE = 1<<28;
   static final public long IPAC    = 1<<29;
   static final public long BMP     = 1<<30;
   static final public long RICE    = 1L<<31;
   static final public long HEALPIX = 1L<<32;
   static final public long GLU     = 1L<<33;
   static final public long ARGB    = 1L<<34;
   static final public long PDS     = 1L<<35;
   static final public long HPXMOC  = 1L<<36;
   static final public long DS9REG  = 1L<<37;
   static final public long SED     = 1L<<38;
   static final public long BZIP2   = 1L<<39;
   static final public long AJTOOL  = 1L<<40;
   static final public long TAP     = 1L<<41;
   static final public long OBSTAP  = 1L<<42;
   static final public long EOF     = 1L<<43;

   static final String FORMAT[] = {
      "UNKNOWN","FITS","JPEG","GIF","MRCOMP","HCOMP","GZIP","XML","ASTRORES",
      "VOTABLE","AJ","AJS","IDHA","SIA","CSV","UNAVAIL","AJSx","PNG","XFITS",
      "FOV","FOV_ONLY","CATLIST","RGB","BSV","FITS-TABLE","FITS-BINTABLE","CUBE",
      "SEXTRACTOR","HUGE","AIPSTABLE","IPAC-TBL","BMP","RICE","HEALPIX","GLU","ARGB","PDS",
      "HPXMOC","DS9REG","SED","BZIP2","AJTOOL","TAP","OBSTAP","EOF" };

   // Recherche de signatures particulieres
   static private final int DEFAULT = 0; // Detection de la premiere occurence
   static private final int FITSEND = 1; // Detection de la fin d'entete FITS


   private boolean withBuffer; // true si on a demandé une bufferisation
   private byte cache[]=null; // Le tampon
   private int offsetCache=0; // Position du prochain octet a lire dans le tampon
   private int inCache=0;     // Nombre d'octets disponibles dans le tampon
   private boolean flagEOF=false; // true si la fin du fichier a été atteinte
   private long type=-1;	      // Le champ de bits qui décrit le type de fichier
   private boolean flagGetType; // true si le type a deja ete determine
   private boolean alreadyRead; // true si le flux a deja ete entame
   private long dejaLu;       // Nombre d'octets déjà lu sur le flux
   private String commentCalib=null;  // Calib trouvée dans un segment commentaire (pour JPEG ou PNG)
   private String filename=null; // Nom du fichier d'origine si connu (pour debug)
   private boolean fitsHeadRead; // true si on a déjà charger (ou essayé)
   // toute l'entête fits courante dans le cache (voir hasFitsKey())

   static public int NBOPENFILE = 0;

   public MyInputStream(InputStream in) {
      this(in,UNKNOWN,true);
   }
   public MyInputStream(InputStream in,long type,boolean withBuffer) {
      super(in);
      this.type=type;
      this.withBuffer = withBuffer;
      flagGetType=false;
      alreadyRead=false;
      fitsHeadRead=false;
      dejaLu=0L;

      
      this.in= in!=null && withBuffer && !(in instanceof BufferedInputStream ) ?
            new BufferedInputStream(in) : in;
      NBOPENFILE++;
   }

   /** Positionnement du fichier d'origine (pour message d'erreur) */
   public void setFileName(String file) { filename=file; }

   /** Retourne le fichier d'origine si connu */
   public String getFileName() { return filename; }

   public void close() throws IOException {
      NBOPENFILE--;
      //      if( Aladin.levelTrace>3 ) System.out.println("MyinputStream.close(): "+this);
      in.close();
      //      System.out.println("MyInputStream NBOPENFILE = "+NBOPENFILE);
   }

   static protected long NativeImage() { return JPEG|GIF|PNG|BMP; }

   /**
    * Dans le cas ou le flux peut etre gzippe, il est necessaire
    * de passer par cette methode pour un eventuel "empilement"
    * de l'objet MyInputStream.
    * S'utilise typiquement: f=f.startRead();
    * @return le flux lui-meme, ou un nouveau flux s'il s'agit d'un flux
    *          gzippe
    */
   public MyInputStream startRead() throws IOException {
      long t = isGZorBzip2();
      if( (t & GZ)!=0 ) {
         return new MyInputStream(new GZIPInputStream(this),GZ,withBuffer);
      } else if( (t & BZIP2)!=0 ) {
         return new MyInputStream(new Bzip2(this),BZIP2,withBuffer);
      }
      return this;
   }

   /** Retourne la position du prochain octet qui va être lu
    * dans le flux
    * @return position du prochain octet qui sera lu (commence à 0)
    */
   public long getPos() { return dejaLu; }

   /** Reset le type courant du flux en ne conservant que
    * GZIP et FITS_EXTENSION.
    * => our analyse des fichiers en FITS EXTENSION
    */
   public void resetType() {
      type = (type & GZ) | (type & XFITS);
      flagGetType = alreadyRead = false;
   }

   //   private boolean alreadyHCOMPtested=false;
   //   private boolean previousHCOMPtest;

   /** Juste pour tester rapidement s'il s'agit d'un FITS HCOMP */
   public boolean isHCOMP() throws Exception {
      return (getType() & HCOMP) != 0;

      //      if( alreadyHCOMPtested ) return previousHCOMPtest;
      //
      //      // le type de stream a deja ete detecte
      //      if( flagGetType ) previousHCOMPtest = (type&HCOMP)==HCOMP;
      //      else {
      //         try {
      //            // Detection de HCOMP
      //            int n = findFitsEnd();
      //            int c0 =  (cache[n]) & 0xFF;
      //            int c1 =  (cache[n+1]) & 0xFF;
      //
      //            //System.out.println("FITS Data magic code "+c0+" "+c1);
      //            previousHCOMPtest = (c0==221 && c1==153);
      //         } catch( Exception e ) {
      //            previousHCOMPtest=false;
      //         }
      //      }
      //      alreadyHCOMPtested=true;
      //      return previousHCOMPtest;

   }

   /** Juste pour tester s'il s'agit d'un flux gzippé */
   public boolean isGZ() throws IOException {
      return (isGZorBzip2() & GZ)!=0;
   }

   /** Juste pour tester s'il s'agit d'un flux gzippé ou Bzippé2 */
   public long isGZorBzip2() throws IOException {
      // le type de stream a deja ete detecte
      if( flagGetType ) return type;

      // Le stream a deja ete entame, impossible de determine le type
      if( alreadyRead ) return 0;

      int c[] = new int[2];
      // On charge qq octets dans le tampon si nécessaire
      if( inCache<c.length ) loadInCache(c.length);

      for( int i=0; i<c.length; i++ ) {
         c[i] = (cache[offsetCache+i]) & 0xFF;
      }

      // Detection de GZIP
      if( c[0]==31  && c[1]==139 ) return GZ;

      // Detection de BZIP2  (il faudrait aussi tester le "h" qui suit)
      else if( c[0]=='B'  && c[1]=='Z' ) return BZIP2;

      return 0;
   }

   /** Sous-types particulier au FITS image */
   private void getTypeFitsImg() throws IOException {

      // Détection d'un CUBE
      //    if( lookForSignature("NAXIS   = 3",false)>0
      if( hasFitsKey("NAXIS","3") && !hasFitsKey("NAXIS3","1")
            || hasFitsKey("NAXIS","4")
            && !hasFitsKey("NAXIS3","1")
            && hasFitsKey("NAXIS4","1")) type |= CUBE;

      // Mode couleur ARGB
      if( hasFitsKey("COLORMOD", "ARGB") )  type |= ARGB;

      // Détection d'une extension FITS à suivre
      if( hasFitsKey("EXTEND",null) || hasFitsKey("NAXIS","0") ) type |= XFITS;

      if( hasFitsKey("CTYPE3","RGB")
            /* || (type&CUBE)==CUBE && hasFitsKey("NAXIS3","3")*/ ) type |= RGB;

      // Détection d'une image HUGE
      if( (type & (CUBE|RGB))==0 ) {
         try {
            int naxis1 = Integer.parseInt(getFitsValue("NAXIS1"));
            int naxis2 = Integer.parseInt(getFitsValue("NAXIS2"));
            int npix = Integer.parseInt(getFitsValue("BITPIX"));
            if( (long)naxis1*naxis2*(Math.abs(npix)/8) > Aladin.LIMIT_HUGEFILE ) type |= HUGE;
         }catch( Exception e ) {}
      }

      // Healpix
      if( (type & XFITS) !=0 && (hasFitsKey("MOCORDER",null) || hasFitsKey("HPXMOC",null) || hasFitsKey("HPXMOCM",null)
            || hasFitsKey("ORDERING","UNIQ") || hasFitsKey("ORDERING","NUNIQ")) ) type |= HPXMOC;
      else if( (hasFitsKey("PIXTYPE", "HEALPIX") || hasFitsKey("ORDERING","NEST") || hasFitsKey("ORDERING","RING"))
            && !hasFitsKey("XTENSION","IMAGE") )  type |= HEALPIX;

      // Detection de HCOMP
      int n = findFitsEnd();
      int c0 =  (cache[n]) & 0xFF;
      int c1 =  (cache[n+1]) & 0xFF;
      //System.out.println("FITS Data magic code "+c0+" "+c1);
      if( c0==221 && c1==153 ) type |= HCOMP;
   }

   /**
    * Determine le type de fichier.
    * Met a jour un champ de bit ou chaque bit decrit un type.
    * Cette methode ne peut etre appelee si le stream a deja ete entame.
    * Le champ de bit peut etre affiche en langage naturelle par la methode
    * decodeType().
    * @param limit nombre d'octets max à lire pour détecter le type (0 sans limite)
    * @return un champ de bit decrivant le type de fichier
    */
   public long getType() throws Exception { return getType(0); }
   public long getType(int limit) throws Exception {
      int csv;
      //System.out.println("call getType()");

      // le type de stream a deja ete detecte, on le retourne tel que
      if( flagGetType ) return type;
      flagGetType=true;

      // Le stream a deja ete entame, impossible de determine le type
      if( alreadyRead ) return NOTAVAILABLE;

      // recherche d'un eventuel MAGIC NUMBER ou d'une premier mot
      // non-equivoque pour determine le type de fichier
      try {

         int c[] = new int[16];
         // On charge qq octets dans le tampon si nécessaire
         if( inCache<c.length ) loadInCache(c.length);

         for( int i=0; i<c.length; i++ ) {
            c[i] = (cache[offsetCache+i]) & 0xFF;
            //System.out.println("MAGIC CODE c["+i+"]="+c[i]);
         }

         // Detection de GZIP
         if( c[0]==31  && c[1]==139 ) type |= GZ;

         // Detection de BZIP2 => ACTUELLEMENT IL Y A UN BUG DANS LE DECOMPRESSEUR BZIP2
         else if( Aladin.PROTO && c[0]=='B'  && c[1]=='Z' )  type |= BZIP2;

         // Détection PDS
         else if( c[0]=='P' && c[1]=='D' && c[2]=='S' ) type |=PDS;

         // Détection HPXMOC (ASCII - ancienne définition ORDERING...)  A VIRER DES QUE POSSIBLE
         else if( c[0]=='O' && c[1]=='R' && c[2]=='D' && c[3]=='E' && c[4]=='R' ) type |=HPXMOC;

         // Détection DS9REG
         else if( c[0]=='#' && c[1]==' ' && c[2]=='R' && c[3]=='e' && c[4]=='g'
               && c[5]=='i' && c[6]=='o' && c[7]=='n' && c[8]==' ' && c[9]=='f'
               &&c[10]=='i' &&c[11]=='l'&& c[12]=='e' ) type |= DS9REG|AJS;

         // Détection HPXMO (ASCII - ancienne définition #HPXMOCM &  #HPXMOC...)
         else if( c[0]=='#' && c[1]=='H' && c[2]=='P' && c[3]=='X'
               && c[4]=='M' && c[5]=='O' && c[6]=='C' ) type |=HPXMOC;

         // Détection MOCORDER (ASCII - nouvelle définition #MOCORDER...)
         else if( c[0]=='#' && c[1]=='M' && c[2]=='O' && c[3]=='C'
               && c[4]=='O' && c[5]=='R' && c[6]=='D' ) type |=HPXMOC;

         // Détection MOC JSON (une ligne de blanc \n{"  )
         else if( isJsonMoc(c) ) type |=HPXMOC;
         
         //         // Detection de BMP
         //         else if( c[0]==66  && c[1]==77 ) type |= BMP;

         // Detection de JPEG
         else if( c[0]==255 && c[1]==216 ) {
            type |= JPEG;
            lookForJpegCalib(limit);
         }

         // Detection de GIF (GIF..a)
         else if( c[0]==71 && c[1]==73 && c[2]==70 && c[5]==97) type |= GIF;

         // Detection de PNG
         else if( c[0]==137 && c[1]==80 && c[2]==78 && c[3]==71
               && c[4]==13 && c[5]==10 && c[6]==26 && c[7]==10) {
            type |= PNG;
            lookForPNGCalib(limit);
         }

         // Detection de MRCOMP
         else if( c[0]==1 && c[1]==121 && c[2]==1 &&
               c[3]==75 && c[4]==1 && c[5]==121 && c[6]==64   ) {

            type |= MRCOMP;
            if( lookForSignature("SIMPLE  =",false)>0 ) type |= FITS;
         }

         // Detection d'une table IPAC
         //         else if( c[0]=='\\' || c[0]=='|' ) type |= IPAC;

         // Detection du début d'une extension FITS IMAGE
         else if( c[0]=='X' && c[1]=='T' && c[2]=='E' &&
               c[3]=='N' && c[4]=='S' && c[5]=='I' &&
               c[6]=='O' && c[7]=='N' && c[8]=='=' &&
               c[11]=='I' && c[12]=='M' && c[13]=='A' && c[14]=='G' && c[15]=='E') {

            type |= FITS;

            getTypeFitsImg();

            // Détection d'un CUBE
            //             if( lookForSignature("NAXIS   = 3",false)>0 ) type |= CUBE;
         }

         // Detection du début d'une extension FITS TABLE
         else if( c[0]=='X' && c[1]=='T' && c[2]=='E' &&
               c[3]=='N' && c[4]=='S' && c[5]=='I' &&
               c[6]=='O' && c[7]=='N' && c[8]=='=' &&
               c[11]=='T' && c[12]=='A' && c[13]=='B' && c[14]=='L' && c[15]=='E') {

            type |= FITST;
         }

         // Detection du début d'une extension FITS BINTABLE
         else if( c[0]=='X' && c[1]=='T' && c[2]=='E' &&
               c[3]=='N' && c[4]=='S' && c[5]=='I' &&
               c[6]=='O' && c[7]=='N' && c[8]=='=' &&
               c[11]=='B' && c[12]=='I' && c[13]=='N' && c[14]=='T' && c[15]=='A') {

            type |= FITSB;

            // Compression RICE
            if(  hasFitsKey("ZCMPTYPE","RICE_1") ) type |= RICE;

            // Pour répérer les tables AIPS CC de calculs intermédiaires
            else if( hasFitsKey("EXTNAME","AIPS CC") && hasFitsKey("TFIELDS","3")
                  && hasFitsKey("TTYPE2","DELTAX") && hasFitsKey("TUNIT2","DEGREES")
                  && hasFitsKey("TTYPE3","DELTAY") && hasFitsKey("TUNIT3","DEGREES")
                  ) type |= AIPSTABLE;
         }

         // Detection de FITS
         else if( c[0]=='S' && c[1]=='I' && c[2]=='M' &&
               c[3]=='P' && c[4]=='L' && c[5]=='E' &&
               c[6]==' ' && c[7]==' ' && c[8]=='=' ) {

            type |= FITS;
            getTypeFitsImg();

         }

         // Détection fichier GLU (parfile)
         else if( lookForSignature("\n%A",false,offsetCache,2048,false)>0
               && lookForSignature("\n%U",false,offsetCache,2048,false)>0 ) type |= GLU;

         // Detection de XML
         else /* if( c[0]=='<' && c[1]=='?' &&
             (c[2]=='X' || c[2]=='x') &&
             (c[3]=='M' || c[3]=='m') &&
             (c[4]=='L' || c[4]=='l')  ) {
             type |= XML; */

            // Detection de ASTRORES
            if( lookForSignature("<!DOCTYPE ASTRO",false)>0 ) type |= ASTRORES|XML;

         // Detection de AJ (Aladin Java stack backup)
            else if( lookForSignature("<ALADINJAVA",false)>0 ) type |= AJ|XML;

         // Detection de VOTABLE
            else if( lookForSignature("<VOTABLE",true)>0 || lookForSignature(":VOTABLE",true)>0) {
               type |= VOTABLE|XML;

               // Detection de IDHA
               if( lookForSignature("name=\"ObservingProgram\"",true)>0 ) type |= IDHA|XML;
               if( lookForSignature("name=\"Observation_Group\"",true)>0 ) type |= IDHA|XML;

               // Detection de SIA
               // TODO : à améliorer car <RESOURCE ID=... type="results" ne sera pas reconnu
               else if( /* lookForSignature("<RESOURCE type=\"results\"",true)>0 && */
                     // SIAP et anciennes versions de SSAP
                     lookForSignature("ucd=\"VOX:Image_Title\"",true)>0
                     || lookForSignature("ucd=\"VOX:Image_AccessReference\"",true)>0
                     // SSAP 1.x (ce n'est qu'un 'should' malheureusement)
                     || lookForSignature("SSAP</INFO>", true)>0
                     // SSAP 1.x
                     || lookForSignature("utype=\"ssa:Access.Reference\"", true)>0
                     // en raison des namespace, je suis obligé de tronquer la partie 'utype='
                     // je mets ici un certain nombre de champs 'MANDATORY' qui suggèrent fortement qu'il s'agit d'un document SSA
                     || (     lookForSignature("Dataset.DataModel", true)>0
                           && lookForSignature("Dataset.Length", true)>0
                           && lookForSignature("Access.Reference", true)>0
                           && lookForSignature("Access.Format", true)>0
                           && lookForSignature("DataID.Title", true)>0  )
                     )
                  type |= SIA_SSA;

               // Detection de FOV
               else if( lookForSignature("name=\"FoVRef\"",true)>0 ||
                     lookForSignature("ID=\"FoVRef\"",true)>0   ||
                     // pour nouveau format
                     lookForSignature("utype=\"dal:footprint.geom.id\"", true)>0
                     ) type |= FOV;

               // Detection de FOV_ONLY
               // TODO : à modifier, beaucoup trop générique
               // il faudrait pouvoir dire qu'on veut cherche utype=.. dans RESOURCE
               else if( lookForSignature("utype=\"dal:fov", true)>0
                     || lookForSignature("utype=\"dal:footprint.geom\"", true)>0
                     || lookForSignature("utype=\"ivoa:characterization/[ucd=pos]/coverage/support", true)>0
                     //                                        || (lookForSignature("FoV",true)>0 && lookForSignature("\"CARTESIAN\"",false)>0)
                     ) {
                  type |= FOV_ONLY;
               }

               else if( lookForSignature("utype=\"photdm:PhotometryFilter.SpectralAxis.Coverage.Location.Value", true)>0 ) {
                  type |= SED;
               }

            }
         /* } */


         // Detection de AJS (Aladin Java script)
            else if(  lookForSignature("#AJS",true)>0 ) type |= AJS;

         // Detection de TSV
            else if( (csv=isCSV())==1 ) {
               type |= CSV;
            }

         // Serait-ce du AJTOOL
            else if( csv==5 ) type |= CSV|AJTOOL;

         // Detection de CATLIST (Liste de catalogues)
            else if(  lookForSignature("#CATLIST",true)>0 ) type |= CATLIST;

         // Detection d'un PDS sans magic code
            else if( lookForSignature("^IMAGE",true)>0 ) type |= PDS;

         // Serait-ce du IPAC
            else if( csv==4 ) type |= BSV|IPAC;

         // Serait-ce du Sextractor
            else if( csv==3 ) type |= BSV|SEXTRA;

         // Bon a va tout de même parier pour du BSV
            else if( csv==2 ) type |= BSV;

         // Perdu
            else return NOTAVAILABLE;

         //         REM: METHODE TRES PEU SURE
         //         else if(    lookForSignature("get ",true)>0
         //                  || lookForSignature("filter ",true)>0 ) type |= AJSx;


      } catch ( EOFException e ) {
         type |= EOF;
         //System.out.println("getType impossible: EOFException !!");
      }

      return type;

   }

   /** Skippe les prochains octets (si nécessaire) pour se caler sur le prochain
    * bloc de 2880 bytes (typique du FITS).
    * @return le nombre d'octets effectivement skippes
    * @throws IOException
    */
   public long skipOnNext2880() throws IOException {
      long pos = getPos();
      if( pos%2880==0 ) return 0;
      long offset = ((pos/2880)+1) *2880  -pos;
      return skip(offset);
   }

   /**
    * Interface InputStream, methode skip()
    * @param n le nombre d'octets a skipper
    * @return le nombre d'octets effectivement skippes
    */
   @Override
   public long skip(long n) throws IOException {
      alreadyRead=true;
      long m=n;
      while( n>0 ) n -= skipInternal(n);
      return m;
   }

   /**
    * Interface InputStream, methode skip()
    * Attention le skip peut etre fait en plusieurs fois si le tampon
    * n'est pas vide
    * @param n le nombre d'octets a skipper
    * @return le nombre d'octets effectivement skippes
    */
   private long skipInternal(long n) throws IOException {
      //System.out.println("Call skip("+n+")");
      if( inCache>0 ) {
         if( n>inCache ) n=inCache;
         offsetCache=offsetCache+(int)n;
         inCache=inCache-(int)n;
         dejaLu+=n;
         return n;
      }
      n=super.skip(n);
      dejaLu+=n;
      return n;
   }

   /**
    * Interface InputStream, methode available()
    * @return le nombre d'octets disponibles
    */
   public int available() throws IOException {
      //System.out.println("Call available()");
      if( inCache!=0 ) return inCache;
      return super.available();
   }

   /**
    * Interface InputStream, methode markSupported()
    * @return toujours false car MyInputStream ne supporte pas les marks
    */
   public boolean markSupported() {
      //System.out.println("Call markSupported()");
      return false;
   }

   private byte bufRead[] = new byte[1];
   /**
    * Interface InputStream, methode read()
    * LA METHODE EST UN PEU BOEUF CAR JE PASSE PAR UN TABLEAU DE 1 OCTET.
    * POUR EVITER LES ALLOCATIONS JE METS CE TABLEAU EN INSTANCE DE CLASSE
    * @return retourne le prochain octet disponible dans le flux sous la forme
    *         d'un entier.
    */
   public int read() throws IOException {
      int rep=read(bufRead,0,1);
      return rep==-1?-1:( (bufRead[0]) &0xFF);
   }

   /**
    * Interface InputStream, methode read(byte buf[])
    * Remplit au mieux le tableau passe en parametre.
    * @param le buffer a remplir
    * @param le nombre d'octets effectivement lus
    */
   public int read(byte buf[]) throws IOException {
      return read(buf,0,buf.length);
   }

   /**
    * Interface InputStream, methode read(byte bbuf[],int offset,int len)
    * Remplit au mieux le tableau passe en parametre.
    * @param le buffer a remplir
    * @param offset position dans le buffer
    * @param len nombre d'octets a lire au mieux
    * @param le nombre d'octets effectivement lus
    */
   public int read(byte buf[], int offset, int len) throws IOException {
      //System.out.println("Call read(buf,"+offset+","+len+") inCache="+inCache);

      // Pour garantir qu'un appel a getType() posterieur ne pourra plus etre
      // valide
      alreadyRead=true;

      // S'il n'y a rien dans le tampon, on lit simplement le flux
      if( cache==null || inCache==0 ) {
         int n=super.read(buf,offset,len);
         if( n<0 ) return n;
         dejaLu+=n;
         return n;
      }

      // Quelque chose dans le tampon, on retourne ce qui s'y
      // trouve, le client devra eventuellement refaire une lecture
      // pour avoir la suite
      if( inCache<len ) len=inCache;
      //System.out.println("Read in cache "+len+" bytes offsetCache="+offsetCache+" inCache="+inCache);
      System.arraycopy(cache,offsetCache,buf,offset,len);
      offsetCache+=len;
      inCache-=len;
      dejaLu+=len;
      return len;
   }

   /**
    * Remplit le tableau passe en parametre d'un nombre precis d'octets.
    * @param le buffer a remplir
    */
   public void readFully(byte buf[]) throws IOException {
      readFully(buf,0,buf.length);
   }

   /**
    * Remplit le tableau passe en parametre d'un nombre precis d'octets.
    * @param le buffer a remplir
    * @param offset position dans le buffer
    * @param len nombre d'octets a lire au mieux
    */
   public void readFully(byte buf[],int offset, int len) throws IOException {
      int m;
      for( int n=0; n<len; n+=m ) {
         m = read(buf,offset+n,(len-n)<512 ? len-n : 512);
         if( m==-1 ) throw new EOFException();
      }
   }

   /**
    * Lecture de la totalite du flux dans un tableau de bytes
    * sans savoir a priori la taille du flux
    * La lecture se fait dans un vecteur de blocs que l'on concatene
    * a la fin de la lecture
    * @return le tableau de byte
    */
   public byte [] readFully() {
      int size=8192;

      ArrayList<byte[]> v = new ArrayList<byte[]>(1000);
      ArrayList<Integer> vSize = new ArrayList<Integer>(1000);
      //      Vector v = new Vector(10);
      //      Vector vSize = new Vector(10);
      int n=0,m=0,i=0,j=0;
      byte [] tmp;

      try {
         tmp = new byte[size];
         while( (n=read(tmp,0,size))!=-1 ) {
            i++;
            //System.out.println("Je lis "+n+" octets (tranche "+i+")");
            v.add(tmp);
            vSize.add( new Integer(n) );
            m+=n;
            tmp = new byte[size];
         }
      } catch( Exception e ) {
         //System.out.println("Fin lecture : "+e+" n="+n);
      }

      //System.out.println("La taille totale est de m="+m);
      byte [] tab = new byte[m];
      j=v.size();
      for( n=i=0; i<j; i++ ) {
         tmp = v.get(i);
         m = vSize.get(i).intValue();
         //System.out.println("Je copie la tranche "+(i+1)+" => "+m+" octets");
         System.arraycopy(tmp,0,tab,n,m);
         n+=m;

      }
      return tab;
   }

   /** Retourne la prochaine ligne (\n compris ventuellement)
    * ou null si le flux est termin
    */
   //   public String readLine() throws IOException {
   //      StringBuilder res = new StringBuilder();
   //      int c;
   //      boolean first=true;
   //
   //      do{
   //         c = read();
   //         if( c==-1 ) {
   //            if( first ) return null;
   //            break;
   //         }
   //         first=false;
   //         res.append((char)c);
   //      } while( c!=(int)'\n' );
   //      return res.toString();
   //   }

   /**
    * Retourne la prochaine ligne du stream
    * @return la ligne lue,
    */
   public String readLine() throws IOException {
      int n;

      try { n = findSignature("\n",false);
      } catch( EOFException eof ) {
         if( inCache==0 ) return null;
         n=offsetCache+inCache;
      }
      String s = new String(cache,offsetCache,n-offsetCache);
      dejaLu+=(n-offsetCache);
      inCache-=(n-offsetCache);
      offsetCache=n;
      return s;
   }

   /**
    * Affichage "en clair" du type de fichier en fonction du cahmp de bits
    * retourne par getType()
    * @return une chaine decrivant le type de fichier
    */
   static public String decodeType(long type) {
      StringBuilder s = new StringBuilder();

      long mode=0x1;
      for( int i=1; i<FORMAT.length; i++ ) {
         if( (type & mode) !=0 && mode!=EOF ) s.append(" "+FORMAT[i]);
         mode = mode << 1;
      }

      if( s.length()==0 ) s.append(FORMAT[0]);
      return s.toString();
   }

   /**
    * Charge "len" octets dans le tampon.
    * Le tampon s'agrandit automatiquement si nécessaire. Sort une EOFException
    * si on rencontre la fin du flux.
    * @param len nombre de bytes a charger
    */
   private void loadInCache(int len) throws IOException {

      if( flagEOF ) throw new EOFException();

      // Premiere allocation du buffer du cache
      if( cache==null ) {
         //System.out.println("Initialisation du cache "+BLOCCACHE+" bytes");
         cache = new byte[BLOCCACHE];
      }

      // Extension eventuelle du cache (avec recopie des bytes
      // non encore lus).
      else {
         int freeCache = cache.length - (offsetCache+inCache);
         //System.out.println("freeCache="+freeCache+" pour="+len+" bytes");
         if( len>freeCache ) {
            int nByte = (((inCache+len)/BLOCCACHE)+1)*BLOCCACHE;
            byte newCache[] = new byte[nByte];
            System.arraycopy(cache,offsetCache,newCache,0,inCache);
            offsetCache=0;
            cache=newCache;
            //System.out.println("modif cache: size="+nByte+" offsetCache="+offsetCache+" inCache="+inCache);
         }
      }

      // Chargement dans le cache de len bytes
      int m;
      int offset=offsetCache+inCache;
      for( int n=0; n<len; n+=m ) {
         m = super.read(cache,offset,len-n);
         //System.out.println("J'ai lu "+m+" bytes");
         if( m==-1 ) { flagEOF=true; throw new EOFException(); }
         offset+=m;
         inCache+=m;
      }
      //System.out.println("Chargement de "+len+" octets dans cache: offsetCache="+offsetCache+" inCache="+inCache);
   }

   /**
    * Substitution dans le cache.
    * Ne marche que si les caractères n'ont pas encore été lus.
    * ATTENTION: Ne marche que si la chaine à insérer est plus petite ou égale
    * à la chaine initiale.
    * @param pos position du 1er octet à substituer dans le cache (compté depuis le début du cache)
    * @param len nombre d'octets à remplacer
    * @param s chaîne de caractères à insérer à la place
    * @throws Exception
    */
   private void substitute(int pos, int len, String s) throws Exception {
      //System.out.println("AVANT:["+(new String(cache,0,inCache))+"]");
      //System.out.println("Substitution dans cache (inCache="+inCache+"/offsetCache="+offsetCache+") pos="+pos+" len="+len+" ["+s+"]");
      char a[] = s.toCharArray();
      if( a.length>len || pos<offsetCache ) throw new Exception("MyInputStream substitution error");
      int i,j;
      for( i=0,j=pos; i<a.length; i++,j++ ) cache[j] = (byte) a[i];
      System.arraycopy(cache,pos+len, cache, pos+a.length, inCache - (pos+len) );
      inCache -= len - a.length;
      //System.out.println("APRES:["+(new String(cache,0,inCache))+"]");
      //System.out.println("cache (inCache="+inCache+"/offsetCache="+offsetCache+")");
   }

   //   private void substituteIPC(int pos, int len, String s) throws Exception {
   ////System.out.println("AVANT:["+(new String(cache,0,inCache))+"]");
   ////System.out.println("Substitution dans cache (inCache="+inCache+"/offsetCache="+offsetCache+") pos="+pos+" len="+len+" ["+s+"]");
   //      char a[] = s.toCharArray();
   //      if( a.length>len || pos<offsetCache ) throw new Exception("MyInputStream substitution error");
   //      int i,j;
   //      for( i=0,j=pos; i<a.length; i++,j++ ) cache[j] = (byte) a[i];
   //      System.arraycopy(cache,pos+len, cache, pos+a.length, inCache - (pos+len) );
   //      inCache -= len - a.length;
   ////System.out.println("APRES:["+(new String(cache,0,inCache))+"]");
   ////System.out.println("cache (inCache="+inCache+"/offsetCache="+offsetCache+")");
   //   }


   /** Retourne le nombre de fois ou un caractere particulier est present dans
    * la chaine
    * @param s La chaine à analyser
    * @param ch le caractère à rechercher
    * @return le nombre d'occurences
    */
   //   private int countChar(String s,char ch) {
   //      char a[] = s.toCharArray();
   //      int n=0;
   //      for( int i=0; i<a.length; i++ ) if( a[i]==ch ) n++;
   //      return n;
   //   }

   /**
    * Retourne true si la ligne est une ligne d'entête à la Sextractor
    * càd  #   2 FLUXERR_ISO     RMS error for isophotal flux                    [count]
    * EN fait on ne teste que les deux premier champ, ça doit suffire
    */
   private boolean isSextra(String s) {
      if( s.length()<7 ) return false;   // trop courte
      if( s.charAt(0)!='#' ) return false;
      try { Integer.parseInt(s.substring(1,5).trim()); } catch( Exception e ) { return false; }
      return true;
   }

   static private int [] count(String s,boolean flagQuote,boolean flagMalin) {
      int n[] = new int[128];
      boolean inQuote=false;

      int len = s.length();
      for( int i=0; i<len; i++ ) {
         char c = s.charAt(i);
         if( flagQuote ) {
            if( inQuote ) {
               if( c=='\\' ) i++;
               else if( c=='"' ) inQuote=false;
            } else inQuote= c=='"';
            if( inQuote ) continue;
         }
         if( flagMalin && i==0 ) continue;   // On ne compte pas le première caractère (voudrait dire que le premier champ de la première ligne est vide
         int ch = c;
         if( ch>=n.length ) continue;
         n[ch]++;
      }
      return n;
   }

   static final private String SEP = "\t;:,!| /&";

   /** Analyse les lignes s[]. Dénombre les catactères présents dans chacune d'elles (>31 & <127)
    * et recherche celui qui pourrait être un séparateur CSV, càd de même dénombrement dans chaque
    * ligne, et parmi un ensemble de cas possible. S'il y a plusieurs candidats possibles,
    * on écartera ceux qui commencent ou terminent la première ligne. S'il y a encore
    * plusieurs candidats, on procédera de même pour les autres lignes de l'échantillon
    * @param s
    * @return
    */
   //   static private int analyseCSV(String [] s) { return analyseCSV(s,s.length); }

   // On va faire deux tentatives, l'une en supposant que les champs peuvent être "quotés",
   // et si ça ne donne rien, on recommence avec des champs supposés non quotés.
   static private int analyseCSV(String [] s,int size) {
      int c = analyseCSV1(s,size,true);
      if( c==-1 ) c = analyseCSV1(s,size,false);
      //      System.out.println("c="+c);
      return c;
   }

   // Retourne true si la première ligne est un commentaire, et pas le reste
   //   static private boolean isFirstLineComment(String [] s, int size ) {
   //      boolean flagFirstComment=true;
   //      for( int i=0; i<size; i++ ) {
   //         flagFirstComment &= s.length>0
   //               && (i==0 && s[i].charAt(0)=='#' || i>0 && s[i].charAt(0)!='#');
   //      }
   //      return flagFirstComment;
   //   }

   // Détecte une ligne de tirets (sans espace ni TAB)
   static public boolean isSimpleDashLine(String s) {
      for( int i=s.length()-1; i>=0; i--) {
         if( s.charAt(i)!='-') return false;
      }
      return true;
   }

   static private int analyseCSV1(String [] s,int size,boolean flagQuote) {
      int [][] m = new int[size][];
      for( int i=0; i<size; i++ ) {
         if( i==1 && isSimpleDashLine(s[i].trim())) { m[i]=m[i-1]; continue; }
         if( Aladin.levelTrace>=4 ) {
            String s1 = s[i];
            if( s1.length()>0 && s1.charAt(s1.length()-1)=='\n' ) s1 = s1.substring(0,s1.length()-1);
            Aladin.trace(4,"analyseCSV (quoted="+flagQuote+") ligne "+i+" ["+s1+"]");
         }
         m[i] = count(s[i],flagQuote,true);
      }
      //      System.out.println("\n***");
      //      for( int j=0; j<size; j++ ) {
      //         System.out.print("Ligne "+j+":");
      //         for( int k=0; k<SEP.length(); k++ ) {
      //            int i = SEP.charAt(k);
      //            System.out.print(" ["+SEP.charAt(k)+"]/"+m[j][i]);
      //         }
      //         System.out.println();
      //      }
      for( int k=0; k<SEP.length(); k++ ) {
         int i = SEP.charAt(k);
         int j;
         if( m[0][i]==0 ) continue;
         for( j=1; j<size && m[j][i]==m[j-1][i]; j++ );
         if( j==size ) return (char)i;
      }
      return -1;
   }

   static public void main(String argv[]) {
      try {
         File dir = new File("C:\\Users\\Pierre\\Desktop\\Data\\Scuba");
         File list [] = dir.listFiles();
         long t1,t2;
         long duree=0L,size=0L;
         int n=0;
         for( File f : list ) {
            size += f.length();
            t1 = System.currentTimeMillis();
            //METHODE 1
            //            MyInputStream mi = new MyInputStream( new FileInputStream(f));
            //            Fits fits = new Fits();
            //            fits.loadFITS(mi);
            //            mi.close();

            //METHODE 2
            //            RandomAccessFile in = new RandomAccessFile(f, "r");
            //            byte [] b = new byte[(int)f.length()];
            //            in.readFully(b);
            //            in.close();

            //METHODE 3
            //            FileInputStream fi = new FileInputStream(f);
            //            FileChannel in = fi.getChannel();
            //
            //            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            //            while(in.read(buf) != -1) {
            //               buf.clear();
            //            }
            //            buf.clear();
            //            in.close();
            //            fi.close();

            t2 = System.currentTimeMillis();
            n++;
            duree+= (t2-t1);
            if( n>0 && n%100==0) System.out.print(".");
         }
         System.out.println("\nn="+n+" size="+Util.getUnitDisk(size)+" tps="+Util.getTemps(duree));
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   /**
    * Transforme une entête Sextractor en une simple ligne de labels
    */
   private String translateSextraHeader(String head) {
      StringTokenizer st = new StringTokenizer(head,"\n\r");
      //      StringBuilder nHead = new StringBuilder("z");
      StringBuilder nHead = new StringBuilder();

      while( st.hasMoreTokens() ) {
         String s = st.nextToken();
         int i = s.indexOf(' ',6);
         String name = s.substring(6, i<0 ? s.length() : i);
         nHead.append("   "+name);
      }
      nHead.append("\n");
      return nHead.toString();
   }
   
   // Recherche d'une chaine {"nn":[
   private boolean isJsonMoc(int c[] ) {
      int mode=0;
      for( int i=0; i<c.length; i++ ) {
         char ch = (char) c[i];
         switch(mode) {
            case 0: // Je cherche le premier non blanc
               if( Character.isSpace(ch) ) continue;
               mode=1;
            case 1: // je dois avoir un {
               if( ch!='{' ) return false;
               mode=2;
               break;
            case 2: // Je cherche le prochain non blanc
               if( Character.isSpace(ch) ) continue;
               mode=3;
            case 3: // Je dois avoir un "
               if( ch!='\"' ) return false;
               mode=4;
               break;
            case 4: // Je dois avoir un nombre jusqu'au prochain "
               if( ch=='\"' ) { mode=5; continue; }
               if( !Character.isDigit(ch) ) return false;
               break;
            case 5: // Je cherche le prochain non blanc
               if( Character.isSpace(ch) ) continue;
               mode=6;
            case 6: // Je dois avoir un :
               if( ch!=':' ) return false;
               mode=7;
               break;
            case 7: // Je peux avoir un blanc
               if( Character.isSpace(ch) ) continue;
               mode=8;
            case 8: // Je dois avoir un [
               if( ch!='[' ) return false;
               return true;
         }
      }
      return true;
      
//      else if( c[0]=='{' && c[1]=='"' ) type |=HPXMOC;
//   else if( c[0]=='\n' && c[1]=='{' && c[2]=='"' ) type |=HPXMOC;
//   else if( c[0]=='\r' && c[1]=='\n' && c[2]=='{' && c[3]=='"' ) type |=HPXMOC;
   }


   /**
    * Determine s'il s'agit d'un flux en CSV.
    * Compare n lignes consecutives. Si elles ont le meme nombres de colonnes,
    * estime que c'est du CSV. Skip les lignes initiales blanches ou qui
    * commencent par #
    * DANS LE CAS D'UNE ENTETE SEXTRACTOR, IL Y A SUBSTITUTION DE L'ENTETE AVEC UNE SIMPLE LIGNE DE
    * LABELS
    * @return 0 ce n'est pas du CSV,
    *         1 c'est du CSV suivant Aladin.CSVCHAR
    *         2 C'est du BSV blanc séparateurs
    *         3 C'est du SEXTRA càd du BSV + entête:
    *                     #   1 NUMBER          Running object number
    *                     #   2 FLUXERR_ISO     RMS error for isophotal flux                    [count]
    *                     #...
    *                           1  3.08751e-05   5.2560   0.0042  0.007450911 4.463027e-05     75.842     72.788  77.4363120  +1.7139901   0.02255782  0.009481858 -21.2  18
    *                           2 1.506869e-05   7.6853   0.0194   0.00145597 3.457046e-05    100.831      4.265  77.3946642  +1.5997848    0.0134634  0.005376848  -1.4  27
    *         4 C'est du IPAC càd du BSV + entête:
    *                     #\ ___ Ks photometric uncertainty of the associated 2MASS All-Sky PSC source
    *                     #\
    *                     #|       source_id|         ra|        dec|   sigra|  sigdec| sigradec| w1mpro|w1sigmpro|  w1snr|  w1rchi2| w2mpro|w2sigmpro|  w2snr|  w2rchi2| w3mpro|w3sigmpro|  w3snr|  w3rchi2| w4mpro|w4sigmpro|  w4snr|  w4rchi2|    rchi2|  nb|  na|  w1mag|w1sigm|w1flg|  w2mag|w2sigm|w2flg|  w3mag|w3sigm|w3flg|  w4mag|w4sigm|w4flg|cc_flags|det_bit|ph_qual|   sso_flg|r_2mass|pa_2mass|n_2mass|j_m_2mass|j_msig_2mass|h_m_2mass|h_msig_2mass|k_m_2mass|k_msig_2mass|             dist|           angle|
    *                     #|            char|     double|     double|  double|  double|   double| double|   double| double|   double| double|   double| double|   double| double|   double| double|   double| double|   double| double|   double|   double| int| int| double|double|  int| double|double|  int| double|double|  int| double|double|  int|    char|    int|   char|       int| double|  double|    int|   double|      double|   double|      double|   double|      double|           double|          double|
    *                     #|                |        deg|        deg|  arcsec|  arcsec|   arcsec|    mag|      mag|       |         |    mag|      mag|       |         |    mag|      mag|       |         |    mag|      mag|       |         |         |    |    |    mag|   mag|     |    mag|   mag|     |    mag|   mag|     |    mag|   mag|     |        |       |       |          | arcsec|     deg|       |      mag|         mag|      mag|         mag|      mag|         mag|           arcsec|             deg|
    *                     #|            null|       null|       null|    null|    null|     null|   null|     null|   null|     null|   null|     null|   null|     null|   null|     null|   null|     null|   null|     null|   null|     null|     null|null|null|   null|  null| null|   null|  null| null|   null|  null| null|   null|  null| null|    null|   null|   null|      null|   null|    null|   null|     null|        null|     null|        null|     null|        null|             null|            null|
    *                     # 02562a148-000137  83.6358261  22.0148582   1.4029   1.6771   -0.3636  11.750     0.407     2.7 9.340e-01  10.812     0.401     2.7 7.390e-01   7.589     0.315     3.4 9.000e-01   5.080      null     0.9 9.740e-01 8.210e-01    1    0   9.342   null    32   8.519   null    32   5.940  0.495     1   3.050  0.541     1     00dd       7    CCBU          0    null     null       0      null         null      null         null      null         null          9.255388        81.990647
    *         5 C'est du AJTOOL càd du TSV avec entête Object\tCont_Flag\tRAJ2000\tDEJ2000\tX\tY\tLabel_Flag\tInfo
    */
   private int isCSV() throws Exception {
      if( inCache<BLOCCACHE-10 ) {
         try { loadInCache(BLOCCACHE-10);
         }
         catch( EOFException e) { }
         catch( IOException e ) { throw e; }
      }
      //       char cs[];
      int deb = offsetCache;
      int n=0;   // Nbre de tokens trouves dans la premiere ligne valide
      //       int i=1;   // Nbre de lignes valides traitees
      int max=4; // Nbre de lignes valides a tester
      boolean inHeader=true; // true si on est dans l'entete du fichier
      //       int rep=1;
      //       StringBuilder debugMsg=null;
      boolean flagSextra = false;   // true si on a détecté une entête Sextractor
      boolean flagIPAC = false;    // true si on a détecté une entête IPAC
      boolean flagAJTool = false;  // ture si on a détecté du AJTOOL
      boolean firstComment = true; // True si on n'a pas encore traité le premier commentaire
      int sextraDeb = deb;         // Position du début de l'entête sextrator (s'il y a lieu)
      int sextraFin = 0;           // Position de fin de l'entête sextrator (s'il y a lieu)
      int IPACDeb = deb;         // Position du début de l'entête IPAC (s'il y a lieu)
      int IPACFin = 0;           // Position de fin de l'entête IPAC (s'il y a lieu)

      int bufN = 0;
      String [] bufLigne = new String[max];
      String firstLine=null;
      int firstLinePos=deb;

      //       try { cs = Aladin.aladin.CSVCHAR.toCharArray(); }
      //       catch( Exception e ) { cs = "\t".toCharArray(); }
      //
      //       if( Aladin.levelTrace>=3 ) debugMsg = new StringBuilder("CSV test result:");

      for( int i=0; bufN<max && deb!=-1; i++ ) {
         StringBuilder ligneb = new StringBuilder(256);
         deb = getLigne(ligneb,deb);
         String ligne = ligneb.toString();
         bufLigne[bufN] = ligne;
         if( inHeader ) {
            if( ligne.trim().length()==0 ) continue;
            if( i==0 && ligne.trim().equals("Object\tCont_Flag\tRAJ2000\tDEJ2000\tX\tY\tLabel_Flag\tInfo") ) flagAJTool=true;
            char c = ligne.charAt(0);
            if( !flagIPAC && bufN==0 && (c=='\\' || c=='|') ) flagIPAC=true;
            if( flagIPAC ) {
               if( c=='\\' ) continue;
               if( c=='|' )  {
                  bufLigne[bufN] = bufLigne[bufN].replace('|', ' ');
                  bufLigne[bufN] = bufLigne[bufN].replace('-', ' ');
                  int j=bufLigne[bufN].length()-2;
                  while( j>0 && bufLigne[bufN].charAt(j)==' ') j--;
                  bufLigne[bufN]=bufLigne[bufN].substring(0,j+1)+'\n';
                  bufN++;
                  IPACFin=deb;
                  continue;
               }
            }
            if( c=='#' ) {
               if( firstComment ) { flagSextra=true; firstComment=false; }
               flagSextra = flagSextra & isSextra(ligne);
               if( flagSextra ) sextraFin = deb;
               if( i==0 ) firstLine=ligne;
               else firstLine=null;
               continue;
            }
            inHeader=false;
         }
         //System.out.println("ligne["+bufN+"] = ["+bufLigne[bufN]+"]");
         bufN++;
      }

      // On vire les lignes blanches éventuelles à la fin
      for( int i=bufN-1; i>=0; i-- ) if( bufLigne[i].trim().length()==0 ) bufN--;

      if( bufN<2 ) return 0;

      if( flagIPAC ) {
         try { substitute(IPACDeb,IPACFin-IPACDeb,bufLigne[0]); }
         catch( Exception e ) { }
         Aladin.trace(3,"IPAC detected");
         return 4;

      }

      int findSep = analyseCSV(bufLigne,bufN);
      if( findSep>=0 ) {
         char c = (char)findSep;
         setSepCSV(c);
         Aladin.trace(3,"CSV detected with ["+c+"] as delimitor");

         // On teste s'il n'y aurait pas une ligne header en commentaire juste avant
         // a la CSV VizieR mode queryCat
         if( firstLine!=null ) {
            String buf1 [] = new String [bufN+1];
            for( int i=1; i<buf1.length; i++ ) buf1[i]=bufLigne[i-1];
            buf1[0]=firstLine;
            if( analyseCSV(buf1,buf1.length)>=0 ) {
               Aladin.trace(3,"First line is certainly a \"comment header\" a la CSV VizieR => remove #");
               try { substitute(firstLinePos,1,""); }
               catch( Exception e ) { }
            }
         }

         if( flagAJTool ) {
            Aladin.trace(3,"AJTOOL detected");
            return 5;
         }

         return 1;
      }

      if( flagSextra ) {
         if( TableParser.countColumn(bufLigne[0],new char[] { ' ' })>1 ) {
            try { substitute(sextraDeb,sextraFin-sextraDeb,translateSextraHeader( new String(cache,sextraDeb,sextraFin))); }
            catch( Exception e ) { }
            Aladin.trace(3,"Sextractor ASCII detected");
            return 3;
         }
      }

      char [] cs = new char[] { ' ' };
      for( int i=0; i<bufN; i++ ) {
         int m = TableParser.countColumn(bufLigne[i],cs);
         if( i==0 ) n=m;
         else if( m!=n ) return 0;
      }

      Aladin.trace(3,"BSD detected (aligned column with blanks");
      return 2;
   }

   private char sepCSV=(char)-1;
   public char getSepCSV() { return sepCSV; }
   private void setSepCSV(char c) { sepCSV=c; }

   private int getLigne(StringBuilder s,int offset) throws Exception {
      int c;
      char ch;
      do {
         c = getValAt(offset++);
         if( c==-1 ) return -1;
         ch = (char)c;
         if( ch!='\r' ) s.append(ch);
      } while( ch!='\n' );
      return offset;
   }

   //    private int getJSonLigne(StringBuilder s,int offset) throws Exception {
   //       int c;
   //       char ch;
   //       do {
   //          c = getValAt(offset++);
   //          if( c==-1 ) return -1;
   //          ch = (char)c;
   //          if( ch!='\r' ) s.append(ch);
   //       } while( ch!='}' );
   //       return offset;
   //    }


   /** Retourne la valeur unsigned dans le cache à la position pos,
    * étend le cache si nécessaire */
   private int getValAt(int pos) throws Exception {
      try { while( offsetCache+pos>=inCache && !flagEOF ) loadInCache(8192); }
      catch( EOFException e ) { }

      if( offsetCache+pos>=inCache && flagEOF ) return -1;

      try { return cache[offsetCache+pos] & 0xFF; }
      catch( Exception e ) { return -1; }
   }

   /** Pour débugging: affichage en hexadécimal d'un octet */
   private String HEX = "0123456789ABCDEF";
   private String H(int b) {
      return ""+HEX.charAt(b/16)+HEX.charAt(b%16);
   }

   /** Pour du débugging : affichage de  */
   private String ASC(byte buf[],int pos, int size) {
      StringBuilder s = new StringBuilder();
      for( int i=pos; i<pos+size  ; i++ ) {
         char c = (char)buf[i];
         s.append( !Character.isISOControl(c) ? c:'.');
         if( i%80==0 && (size<80 || i>0) ) s.append("\n        ");
      }
      return s+"";
   }


   private String lz77Uncompress(byte [] tmp) throws Exception {
      ByteArrayInputStream in = new ByteArrayInputStream(tmp);
      InflaterInputStream zIn = new InflaterInputStream(in);
      byte [] buffer = new byte[8192];
      int n;
      StringBuilder res = new StringBuilder();
      while( (n=zIn.read(buffer))!=-1 ) {
         String s = new String(buffer,0,n);
         res.append(s);
      }
      zIn.close();
      return res.toString();
   }

   /** Mémorisation du segment PNG commentaire supposé contenir une Calib
    * @param pos position dans le cache
    * @param size taille du segment
    * @return true si ça ressemble au moins un peu à une entête FITS
    */
   private boolean memoPNGCalib(int pos,int size,boolean compress) {

      // Un commentaire PNG commence par un mot clé, suivi d'un octet à 0, puis le commentaire libre
      while( cache[pos]!=0 && size>1 ) { pos++; size--; }
      pos++; size--;

      // Mode non compressé
      if( !compress ) commentCalib = new String(cache,pos,size);

      // Mode compressé en LZ77
      else {
         try {
            pos++; size--;   // pour l'octet qui contient le type de compression (toujorus à 0 de fait)
            byte tmp [] = new byte[size];
            System.arraycopy(cache, pos, tmp, 0, size);
            commentCalib= lz77Uncompress(tmp);
         } catch( Exception e ) {
            if( Aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }
      }
      //       System.out.println("CALIB=["+commentCalib+"]");
      if( commentCalib.indexOf("CTYPE1")<0 ) {
         commentCalib=null;
         return false;
      }
      return true;
   }


   /** Mémorisation du segment JPEG commentaire supposé contenir une Calib
    * @param pos position dans le cache
    * @param size taille du segment
    * @return true si ça ressemble au moins un peu à une entête FITS
    */
   private boolean memoJpegCalib(int pos,int size) { return memoJpegCalib(pos,size,cache); }
   private boolean memoJpegCalib(int pos,int size,byte [] cache) {
      commentCalib = new String(cache,pos,size);
      if( commentCalib.indexOf("CTYPE1")<0 ) {
         commentCalib=null;
         return false;
      }
      return true;
   }

   Hashtable<String,String> avm = null;
   double avmRefWidth=-1;

   /** Mémorisation d'un mot clé/valeur AVM */
   private void memoOneAVM(StringBuilder key, StringBuilder val) {
      String value = val.toString().trim();
      if( value.length()==0 ) return;
      if( avm==null ) avm=new Hashtable<String,String>(30);
      avm.put(key.toString().trim(),value);
      if( Aladin.levelTrace>=3 ) System.out.println("AVM tag: "+key+"=["+value+"] ");
   }

   /** Ajout d'un caractère à une chaine en évitant les doublons des blancs
    * et en remplaçant d'éventuels NL par un blanc */
   private void appendValue(StringBuilder buf,char c) {
      int n= buf.length();
      if( c=='\n' || c=='\r' ) c=' ';    // On remplace les NL par un blanc
      if( n==0 || !Character.isSpaceChar(c) ) { buf.append(c); return; }
      char x = buf.charAt(n-1);
      if( Character.isSpaceChar(x) ) return;
      buf.append(c);
   }

   /** Tentative d'extraction des infos AVM d'une entête XMP
    * ATTENTION : CA FAIT PAS DANS LA DENTELLE
    *
    * 1) XMP ancienne méthode (AVM 1.0)
    * <avm:Spatial.ReferenceValue>
    *    <rdf:Seq> <rdf:li>1.4799618052141E+01</rdf:li>
    *    <rdf:li>-7.2178655394068E+01</rdf:li> </rdf:Seq>
    * </avm:Spatial.ReferenceValue>
    *
    * 2) XMP nouvelle méthode (AVM 1.1)
    *  <rdf:Description ...  avm:Spatial.Rotation="0.86206593355939" ... >
    *
    * @return true si une entête AVM a été trouvée
    */
   private boolean memoJpegAVMCalib(int pos,int size) { return memoJpegAVMCalib(pos,size,cache); }
   private boolean memoJpegAVMCalib(int pos,int size,byte [] cache) {
      boolean rep=false;
      int mode=0;
      int omode=-1;
      int depth=0;
      avm=null;
      avmRefWidth=-1;
      StringBuilder key=null;
      StringBuilder value=null;
      boolean flag0,flag1,flag2,flag3,flag4;
      boolean flagXMP2=false;      // Pour distinguer les deux modes XMP
      flag4=flag3=flag2=flag1=flag0=false;

      for( int i=0; i<size; i++) {
         char c = (char)cache[pos+i];
         //         if( mode==omode ) System.out.print(c);
         //         else System.out.print("\n"+mode+" ["+depth+"] : "+c);
         //         omode=mode;
         switch(mode) {
            // recherche de "<avm:"
            case 0:
               flag4=flag3 && c==':';
               flag3=flag2 && c=='m';
               flag2=flag1 && c=='v';
               flag1=flag0 && c=='a';
               flag0=c=='<' || c==' ';  // Pour suporter les 2 types d'XMP
               if( flag0 ) flagXMP2= c==' ';
               if( flag4 ) {
                  key = new StringBuilder();
                  if( !flagXMP2 ) { mode=1; depth=1; }
                  else mode=10;
               }
               break;
               // Mémorisation du mot clé XXXX dans <avm:XXXX>
            case 1:
               if( c=='>' ) { mode=2; value=new StringBuilder(); }
               else key.append(c);
               break;
               // Lecture du paramètre en évitant les éventuels tags
               // emboités du genre "<rdf:Seq>" ou "<rdf:li>"
            case 2:
               if( c=='<' ) mode=3;
               else appendValue(value,c);
               break;
               // Test du prochain tag (début de tag emboité ou fin de tag ?
            case 3:
               if( c=='/' ) depth--;
               else depth++;
               mode=4;
               break;
               // Parcours du tag courant (attention à une fermeture immédiate possible <.... />)
            case 4:
               if( c=='>' ) {
                  if( flag0 ) depth--;
                  if( depth==0 ) { memoOneAVM(key,value); rep=true; mode=0; }
                  else { appendValue(value,' '); mode=2; }
               }
               flag0=c==' '?flag0:c=='/';
               flag0=c=='/';
               break;
               // Mémorisation du mot clé XXXX dans avm:XXXX=
            case 10:
               if( c=='=' ) { mode=11;  value=new StringBuilder(); }
               else key.append(c);
               break;
               // Passage de la première quote avm:XXXX="YYYY"
            case 11:
               if( c=='"' ) mode=12;
               break;
               // Mémorisation de la valeur avm:XXXX="YYYY"
            case 12:
               if( c=='"' ) { memoOneAVM(key,value); rep=true; mode=0; }
               else appendValue(value,c);
               break;
         }
      }
      return rep;
   }

   private void createJpegAVMCalib(int width) {
      StringBuilder fits = new StringBuilder(1000);
      StringTokenizer st;
      String s;
      double ratio=1;

      fits.append("COMMENT FITS header built by Aladin from AVM tags\n");

      String projs=avm.get("Spatial.CoordsystemProjection");
      if( projs==null ) projs="TAN";
      String slon="RA---";
      String slat="DEC--";
      s = avm.get("Spatial.CoordinateFrame");
      if( s!=null ) {
         if( s.equals("GAL") ) { slon="GLON-"; slat="GLAT-"; }
         else if( s.equals("ECL") ) { slon="ELON-"; slat="ELAT-"; }
         if( s.equals("SGAL") ) { slon="SLON-"; slat="SLAT-"; }
      }
      fits.append("CTYPE1  = '"+slon+projs+"'\n");
      fits.append("CTYPE2  = '"+slat+projs+"'\n");

      // Faut-il prendre en compte un changement d'échelle ?
      try {
         st = new StringTokenizer( avm.get("Spatial.ReferenceDimension") );
         ratio=width/Double.parseDouble(st.nextToken());
      } catch( Exception e ) {};


      //      boolean refSpacial = false;
      try {
         st = new StringTokenizer( avm.get("Spatial.ReferencePixel") );
         fits.append("CRPIX1  = "+Double.parseDouble(st.nextToken())*ratio+"\n");
         fits.append("CRPIX2  = "+Double.parseDouble(st.nextToken())*ratio+"\n");
         //         refSpacial=true;
      } catch( Exception e ) {};

      try {
         st = new StringTokenizer( avm.get("Spatial.ReferenceValue") );
         fits.append("CRVAL1  = "+st.nextToken()+"\n");
         fits.append("CRVAL2  = "+st.nextToken()+"\n");
         //         refSpacial=true;
      } catch( Exception e ) {};

      //      if( !refSpacial ) {
      //         try {
      //            st = new StringTokenizer( avm.get("Spatial.Notes") );
      //            fits.append("CRPIX1  = "+Double.parseDouble(st.nextToken())+"\n");
      //            fits.append("CRPIX2  = "+Double.parseDouble(st.nextToken())+"\n");
      //            fits.append("CRVAL1  = "+st.nextToken()+"\n");
      //            fits.append("CRVAL2  = "+st.nextToken()+"\n");
      //         } catch( Exception e ) {};
      //      }

      try {
         st = new StringTokenizer( avm.get("Spatial.Scale") );
         fits.append("CDELT1  = "+Double.parseDouble(st.nextToken())/ratio+"\n");
         fits.append("CDELT2  = "+Double.parseDouble(st.nextToken())/ratio+"\n");
      } catch( Exception e ) {};

      try {
         st = new StringTokenizer( avm.get("Spatial.CDMatrix") );
         fits.append("CD1_1   = "+st.nextToken()+"\n");
         fits.append("CD1_2   = "+st.nextToken()+"\n");
         fits.append("CD2_1   = "+st.nextToken()+"\n");
         fits.append("CD2_2   = "+st.nextToken()+"\n");
      } catch( Exception e ) {};


      if( (s=avm.get("Spatial.Rotation"))!=null )
         fits.append("CROTA2  = "+s+"\n");

      if( (s=avm.get("Spatial.Equinox"))!=null )
         fits.append("EQUINOX = "+s+"\n");

      s=avm.get("Spatial.CoordinateFrame");
      if( s!=null && (s.equals("FK4") || s.equals("FK5") || s.equals("ICRS") ) )
         fits.append("RADECSYS= "+s+"\n");

      fits.append("COMMENT Original AVM tags:\n");
      Enumeration<String> e = avm.keys();
      while( e.hasMoreElements() ) {
         String key = e.nextElement();
         String val = avm.get(key);
         s = "COMMENT "+key+"="+val+"\n";
         if( s.length()>80 ) s=s.substring(0,79)+"\n";
         fits.append(s);
      }

      commentCalib = fits.toString();
   }

   /** Petit rajout en préfixe dans le commentaire JPEG contenant une Calib
    * pour pouvoir compléter les entêtes à la SLOAN ou AVM qui ne comportent pas les
    * NAXIS et NAXIS
    * @param width Largeur de l'image
    * @param height hauteur de l'image
    */
   protected void jpegCalibAddNAXIS(int width,int height) {
      if( avm!=null ) createJpegAVMCalib(width);
      if( commentCalib==null ) return;
      commentCalib =  "SIMPLE  = T\n"
            +"BITPIX  = 8\n"
            //                   +"NAXIS   = 3\n"
            +"NAXIS   = 2\n"
            +"NAXIS1  = "+width+"\n"
            +"NAXIS2  = "+height+"\n"
            //                   +"NAXIS3  = 3\n"
            +commentCalib;
   }

   /** Construction d'un HeaderFits à partir de l'entête JPEG si possible,
    * sinon génère une exception */
   protected FrameHeaderFits createFrameHeaderFitsFromCommentCalib(Plan plan) {
      return new FrameHeaderFits(plan,commentCalib);
   }

   /** Construction d'un HeaderFits à partir de l'entête JPEG si possible,
    * sinon génère une exception */
   public HeaderFits createHeaderFitsFromCommentCalib(int width,int height) throws Exception {
      HeaderFits headerFits;
      try {
         headerFits=new HeaderFits(commentCalib);
      } catch( Exception e ) {
         jpegCalibAddNAXIS(width,height);
         headerFits=new HeaderFits(commentCalib);
      }

      return headerFits;
   }

   /** Extraction directe et rapide des données AVM ou CalibFits du fichier
    * JPEG ou PNG. Il est nécessaire de réouvrir le fichier pour pouvoir
    * faire les seeks qui vont bien => ne peut s'appliquerà un simple Stream
    * => Ne change rien au flux initial (cache, pos...)
    */
   public HeaderFits fastExploreCommentOrAvmCalib(String filename) throws Exception {
      HeaderFits headerFits=null;
      RandomAccessFile file = null;
      try {
         file = new RandomAccessFile(filename, "rw");
         if( (type&JPEG)!=0 ) fastExploreJpg(file);
         //         else if( (type&PNG)!=0 ) explorePNG(file);   // A FAIRE QUAND J'AURAIS LE TEMPS PF Mars 2015
         else return null;
      }
      finally { if( file!=null) try { file.close(); } catch( Exception e) {} }
      return headerFits;
   }

   // Extraction directe et rapide des données AVM ou CalibFits du bon
   // segment commentaire d'un fichier JPEG
   private boolean fastExploreJpg(RandomAccessFile file) throws Exception {
      int c;
      file.seek(2);  // on saute le magic code
      while( (c=file.read())!=-1 ) {
         if( c!=0xFF ) return false;  // Pb de début de segment
         int mode = file.read();
         int size = file.read()<<8 | file.read();
         long pos = file.getFilePointer();
         if( mode==0xE1 ) {
            byte [] buf = new byte[size-2];
            file.readFully(buf);
            if( memoJpegAVMCalib(0,buf.length,buf) ) return true;
         } else if( mode==0xFE ) {
            byte [] buf = new byte[size-2];
            file.readFully(buf);
            if( memoJpegCalib(0,buf.length,buf) ) return true;
         }
         file.seek(pos+size-2);
      }
      return false;
   }

   /** Retourne true si ce flux dispose d'une calib dans un segment commentaire (JPEG ou PNG) */
   public boolean hasCommentCalib() { return commentCalib!=null || avm!=null; }

   public boolean hasCommentAVM() { return avm!=null; }

   /** Recherche dans un flux JPEG le segment commentaire qui peut contenir une
    * calibration. La mémorise (voir getJpegCabib() )
    * Etend automatique le cache pour se faire.
    * @param limit nombre d'octets max à lire pour détecter la calib, (0 jusqu'au bout)
    * @return true si on a trouvé un segment commentaire, false sinon
    */
   //    private boolean lookForJpegCalib() { return lookForJpegCalib(0); }
   private boolean lookForJpegCalib(int limit) {

      int i=2;   // Taille de la signature JPEG
      try {
         while( getValAt(i)==0xFF && (limit<=0 || inCache<limit) ) {
            int mode = getValAt(i+1);
            int size = getValAt(i+2)<<8 | getValAt(i+3);
            try {
               while( offsetCache+i+2+size>=inCache ) loadInCache(8192);
            } catch( EOFException e ) { }
            if( Aladin.levelTrace==6 ) {
               Aladin.trace(6,"("+i+") Segment JPEG "+H(getValAt(i))+" "+H(getValAt(i+1))+" "+size+" octets : ");
               Aladin.trace(6,ASC(cache,offsetCache+i+8,size>128 ? 128 : size));
            }
            if( mode==0xE1 ) {
               memoJpegAVMCalib(offsetCache+i+4,size-2);

            } else if( mode==0xFE ) {
               if( memoJpegCalib(offsetCache+i+4,size-2) ) return true;
            }
            i+=size+2;
         }
      } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      return false;
   }

   /** Recherche dans un flux PNG le segment commentaire qui peut contenir une
    * calibration. La mémorise (voir getPNGCabib() )
    * Etend automatique le cache pour se faire.
    * @param limit nombre d'octets max à lire pour détecter la calib, (0 jusqu'au bout)
    * @return true si on a trouvé un segment commentaire, false sinon
    */
   //    private boolean lookForPNGCalib() { return lookForPNGCalib(0); }
   private boolean lookForPNGCalib(int limit) {
      boolean encore= true;
      int i=8;   // Taille de la signature PNG
      boolean more=true;
      try {
         while( encore && (limit<=0 || inCache<limit)  ) {
            int size = getValAt(i)<<24 | getValAt(i+1)<<16 | getValAt(i+2)<<8 | getValAt(i+3);
            String chunk = new String( new char[] { (char)getValAt(i+4),(char)getValAt(i+5),(char)getValAt(i+6),(char)getValAt(i+7)});
            try {
               if( more ) while( offsetCache+i+8+size>=inCache ) loadInCache(8192);
               else if( offsetCache+i+8+size>=inCache ) encore=false;
            } catch( EOFException e ) { more=false; }
            if( Aladin.levelTrace==6 ) {
               Aladin.trace(6,"("+i+") Segment PNG "+chunk+" "+size+" octets : ");
               Aladin.trace(6,ASC(cache,offsetCache+i+8,size>128 ? 128 : size));
            }
            if( chunk.equals("tEXt") ) {
               if( memoPNGCalib(offsetCache+i+8,size,false) ) return true;

            } if( chunk.equals("zTXt") ) {
               if( memoPNGCalib(offsetCache+i+8,size,true) ) return true;

            } else if( chunk.equals("IEND") ) { encore=false;

            }
            i+=size+12;
         }
      } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      return false;
   }

   /**
    * Recherche d'une signature dans le tampon.
    * @param sig la signature a chercher (uniquement du texte)
    * @param caseInsensitive true si on confond les maj. et les minuscules
    * @param retourne l'indice dans le tampon de l'octet qui suit la signature
    *        ou -1 si la signature n'a pas ete trouvee
    */
   private int lookForSignature(String sig,boolean caseInsensitive)
         throws IOException {
      return lookForSignature(sig,caseInsensitive,offsetCache,false);
   }

   /**
    * Recherche avancee d'une signature dans le tampon.
    * RQ: On ignore les blancs
    * @param sig la signature a chercher (uniquement du texte, éventuellement précédé par \n pour dire début de ligne)
    * @param caseInsensitive true si on confond les maj. et les minuscules
    * @param offset indique a partir de quelle position dans le tampon
    *               on va chercher la signature
    * @param maxOffset valeur limite de la recherche, -1 si aucune borne
    *               (valeur approximative, dépendant des blocs de lecture
    * @param flagEOF true si on sort en EOFException dans le cas ou
    *               l'on a pas trouve la signature
    * @param retourne l'indice dans le tampon de l'octet qui suit la signature
    *        ou -1 si la signature n'a pas ete trouvee
    */
   private int lookForSignature(String sig,boolean caseInsensitive,
         int offset,boolean flagEOF) throws IOException {
      return lookForSignature(sig,caseInsensitive,offset,-1,flagEOF);
   }
   private int lookForSignature(String sig,boolean caseInsensitive,
         int offset,int maxOffset,boolean flagEOF) throws IOException {
      //System.out.println("Call lookForSignature("+sig+","+offset+")");

      boolean EOF=false;	// memorise qu'on a atteint la fin de stream

      // n'est-on pas allé déjà trop loin dans le fichier ?
      if( maxOffset>=0 && offset>=maxOffset ) return -1;

      // Je charge au-moins BLOCCACHE-10 bytes dans le cache pour chercher
      // la signature (evite generalement la reallocation du tampon car on a deja
      // charge le tampon de quelques octets pour chercher un Magic Number)
      if( offsetCache+inCache-offset<BLOCCACHE-10 ) {
         try {
            int oOffsetCache=offsetCache;
            loadInCache(BLOCCACHE-10);
            if( offsetCache==0 ) offset-=oOffsetCache;
         }
         catch( EOFException e) { EOF=true; }
         catch( IOException e ) { throw e; }
      }

      // Recherche de la signature dans le tampon
      char s[]=sig.toCharArray();
      int i,j;
      for( i=offset==0 && s[0]=='\n' && s.length>1 ? 1:0, j=offset-offsetCache; i<s.length && j<inCache; j++ ) {
         char a = s[i];
         while( j<inCache && isSpace((char)cache[offsetCache+j]) ) j++;
         char b = (char)cache[offsetCache+j];
         //         if( b=='\r' ) b='\n';

         if( caseInsensitive ) {
            a=Character.toUpperCase(a);
            b=Character.toUpperCase(b);
         }

         if( a==b ) i++;
         else {
            if( i>0 ) j--;	// On rejouera sur le premier caractère de la signature
            i=0;
         }
         while( i<s.length && isSpace(s[i]) ) i++;
      }

      if( i==s.length ) return offsetCache+j;
      if( EOF && flagEOF ) throw new EOFException();
      return -1;
   }

   /** Retourne true si c'est un espace sans prendre en compte les retours
    * à la ligne
    */
   private final boolean isSpace(char c) { return c==' ' || c=='\t'; }

   /** Le caractère n'est ni un espace, ni un slash (/) ni une quote (') */
   private boolean isFitsVal(char c ) {
      return !isSpace(c) && c!='/' && c!='\'';
   }

   private int posAfterFitsHead=-1;

   /**
    * Detection de la fin de l'entete FITS.
    * Genere un EOFException si non trouve
    * @return la position du premier octet qui suit l'entete FITS (mod 80 et non 2880)
    *         dans le tampon.
    */
   private int findFitsEnd() throws IOException {
      if( fitsHeadRead ) return posAfterFitsHead;
      posAfterFitsHead =  findSignature("END",false,FITSEND);
      fitsHeadRead=true;
      return posAfterFitsHead;
   }

   /** Détermine si le flux contient un mot clé Fits "KEY   = Value" ou  "KEY   = 'Value'"
    *  Va au préalable charger le tampon jusqu'au prochain END en position %80 si nécessaire
    *  @param key Le mot clé fits recherché (sans blanc ni égale (=), en majuscules
    *  @param value la valeur associée, ou null si aucune
    */
   private boolean hasFitsKey(String key,String value) throws IOException {
      if( !fitsHeadRead ) findFitsEnd();
      int len= value==null ? -1 : value.length();
      int k=offsetCache;
      while( (k=lookForSignature(key+"=",false,k,false))>=0 ) {

         //System.out.println("J'ai trouvé ["+key+"] =  => position "+k+" ["+(new String(cache,k-9,80))+"]");
         int i=k;
         if( (i-9)%80!=0 ) continue;   // Pas sur un multiple de 80 => pas un mot clé FITS

         if( len==-1 ) return true;  // Pas de recherche de valeur

         // Comparaison sur la valeur qui suit
         while( !isFitsVal( (char)cache[i]) && i<k+71 ) i++;  //passe blancs et quotes simples
         //System.out.println("  Compare ["+value+" ] len="+len+" à ["+(new String(cache,i,len+1))+"]");
         int j;
         for( j=0; j<len && value.charAt(j)==(char)cache[i]; j++, i++);
         if( j==len && !isFitsVal( (char)cache[i]) ) {
            // System.out.println("   ==> Ok");
            return true;
         }
      }
      return false;
   }

   /** Récupère la valeur d'un mot clé fits ou null si non trouvé */
   private String getFitsValue(String key) throws IOException {
      if( !fitsHeadRead ) findFitsEnd();
      StringBuilder value;
      int k=offsetCache;
      while( (k=lookForSignature(key+"=",false,k,false))>=0 ) {

         //System.out.println("J'ai trouvé ["+key+"] =  => position "+k+" ["+(new String(cache,k-9,80))+"]");
         int i=k;
         if( (i-9)%80!=0 ) continue;   // Pas sur un multiple de 80 => pas un mot clé FITS

         value = new StringBuilder();

         // Comparaison sur la valeur qui suit
         while( !isFitsVal( (char)cache[i]) && i<k+71 ) i++;  //passe blancs et quotes simples
         //System.out.println("  Compare ["+value+" ] len="+len+" à ["+(new String(cache,i,len+1))+"]");
         char c;
         for( ; isFitsVal(c=(char)cache[i]); i++ ) value.append(c);
         // System.out.println("   ==> value ["+value+"]");
         return value.toString();
      }
      return null;
   }

   /**
    * Detection d'une signature particuliere.
    * Genere un EOFException si non trouve
    * @param sig la signature a trouvee
    * @param caseInsensitive true si on confond maj. et minuscule
    * @return la position du premier octet qui suit la signature dans le tampon.
    */
   private int findSignature(String sig,boolean caseInsensitive)
         throws IOException {
      return findSignature(sig,caseInsensitive,DEFAULT);
   }

   /**
    * Detection d'une signature particuliere.
    * Genere un EOFException si non trouve.
    * Cette methode peut eventuellement appele plusieurs fois lookForSignature()
    * dans le cas ou la signature ne se trouverait pas encore dans le tampon.
    * D'autre part, elle peut egalement reiterer l'appel
    * dans le cas de methode particuliere telle que FITSEND pour s'assurer que
    * la signature trouvee se trouve a la bonne place (par ex. mod 80)
    * @param sig la signature a trouvee
    * @param caseInsensitive true si on confond maj. et minuscule
    * @param method choisir DEFAULT, sauf pour des recherche particuliere (ex FITSEND)
    * @return la position du premier octet qui suit la signature dans le tampon.
    */
   private int findSignature(String sig,boolean caseInsensitive,int methode)
         throws IOException {
      int offset=offsetCache;	// position ou l'on commence la recherche
      int n;

      do {

         // Recherche de la signature, avec reiteration (augmente le tampon)
         // tant qu'elle n'a pas ete trouvee
         while( (n=lookForSignature(sig,caseInsensitive,offset,true))<0 ) {
            offset=offsetCache+inCache;
         }

         // Dans le cas de la methode FITSEND, s'assure que la signature END
         // se trouve en position modulo 80, et retourne comme position le prochain
         // octet apres le END sur 80 caracteres.
         if( methode==FITSEND ) {
            //System.out.println("FITSEND a n="+n);
            if( (n-sig.length())%80==0 ) {
               int dataFits = ((n/80)+1)*80;
               //System.out.println("==> Data fits offset "+dataFits);

               // On remplit le cache jusqu'au deux premiers bytes
               // des donnees FITS pour preparer un eventuel test
               // du MAGIC NUMBER de HCOMP
               if( inCache<dataFits+2 ) loadInCache(dataFits+2-inCache);
               //for( int i=n-3; i<dataFits+2; i++ ) {
               //   if( i%10==0 ) System.out.println();
               //   char x = (char)cache[i];
               //   if( x>'A' && x<'Z' ) System.out.print(x);
               //   else System.out.print(" "+(((int)cache[i])&0xFF));
               //}
               //System.out.println();

               return dataFits;
            }

            offset=n;
            n=-1;
         }
      }while(n<0);

      return n;
   }

   //   public static void main(String[] args) {
   //      try {
   //         for( int i=0; i<args.length; i++ ) {
   //            String file=args[i];
   //            InputStream in = new FileInputStream(new File(file));
   ////            OutputStream out = new FileOutputStream(new File(file+".mys"));
   //
   //            MyInputStream f = new MyInputStream(in);
   //            f=f.startRead();
   //            System.out.println(file+" =>"+decodeType(f.getType()));
   ////            byte buf[] = f.readFully();
   ////            out.write(buf);
   //            f.close();
   ////            out.close();
   //         }
   //      } catch( IOException e ) { e.printStackTrace(); }
   //   }

}
