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


package cds.xml;

import java.io.EOFException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.Save;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Astropos;
import cds.astro.Astrotime;
import cds.astro.Ecliptic;
import cds.astro.FK4;
import cds.astro.FK5;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.astro.Supergal;
import cds.astro.Unit;
import cds.fits.HeaderFits;
import cds.tools.Util;


/**
 * Parser de tables
 * Accepte aussi bien du TSV natif, du TSV à la mode SkyCat,
 * du CSV, du IPAC, de l'ASCII simple (colonnes séparées par des espaces)
 * du AstroRes, du VOTable, du VOTable avec CSV encapsulé, VOTable base64,
 * du FITS ASCII, du FITS BINTABLE, ...
 *
 * @version 3.0 - avr 13 - BINARY correction + BINARY2
 * @version 2.4 - 2009 - Support pour VOTable 1.2
 * @version 2.3 - déc 06 - correction bug <TD/>
 * @version 2.2 - avril 06 - Support pour le CSV et le FITS ASCII ou BINTABLE
 * @version 2.1 - déc 05 - Prise en compte de l'erreur "a la SIAP"
 * @version 2.0 -  mai 05 - Fusion VOTable/Astrores et améliorations diverses
 * @version 1.6 -  19 mars 02 - Amélioration de la gestion de la colonne DE
 * @version 1.5 -  17 jan 02 - gestion des TAGs info sans attribut "name"
 * @version 1.4 -  8 jan 02 - correction bug sur coord (caractère '+' avant DE)
 * @version 1.3 -  19 jan 01 - Gestion du sexagésimal
 * @Version 1.2 - 02/10/00
 * @Version 1.1 - 02/04/00
 * @Version 1.0 - 02/09/99
 * @Author P.Fernique [CDS]
 * @Copyright ULP/CNRS 1999-2006
 */
final public class TableParser implements XMLConsumer {

   static final Astroframe AF_FK4 = new FK4();
   static final Astroframe AF_FK5 = new FK5();
   static final Astroframe AF_GAL = new Galactic();
   static final Astroframe AF_SGAL = new Supergal();
   static final Astroframe AF_ICRS = new ICRS();
   static final Astroframe AF_ECLI = new Ecliptic();

   // Différents formats pour les coordonnées célestes
   static final public int FMT_UNKNOWN=0;
   static final public int FMT_DECIMAL=1;
   static final public int FMT_SEXAGESIMAL=2;
   
   // Différentes unités pour les coordonées célestes
   static final public int UNIT_UNKNOWN=0;
   static final public int UNIT_DEGREES=1;
   static final public int UNIT_RADIANS=2;

   private TableParserConsumer consumer; // Référence au consumer
   private XMLParser xmlparser;	      // parser XML
   private int nField;	      	      // Numéro de champ courant
   private int nRecord;               // Numéro de l'enregistrement courant
   private int nRA,nDEC;              // Numéro de la colonne RA et DEC (-1 si non encore trouvée)
   private int nPMRA,nPMDEC;          // Numéro de la colonne PMRA et PMDEC (-1 si non encore trouvée)
   private int nX,nY;	      	      // Numéro de la colonne X et Y (-1 si non encore trouvée)
   private String sFreq,sFlux,sFluxErr,sSedId; // ID des colonnes portant les valeurs d'un point SED
   private int nFreq,nFlux,nFluxErr,nSedId; // Numéro des colonnes correspondantes
   private int qualRA,qualDEC;        // La qualité de détection des colonnes RA et DEC (1000 mauvais, 0 excellent)
   private int qualPMRA,qualPMDEC;    // La qualité de détection des colonnes RA et DEC (1000 mauvais, 0 excellent)
   private int qualX,qualY;	          // La qualité de détection des colonnes X et Y (1000 mauvais, 0 excellent)
   private Field f;		              // Le champ courant
   private Vector<Field> memoField;   // Mémorisation des Fields en cas de parsing binaire ultérieur
   private Field[] tsvField;          // Idem dans le cas d'un TSV (sans doute à fusionner avec memoField si le coeur m'en dit)
   private String fieldSub=null;      // contient le nom du tag courant fils de <FIELD>, sinon null
   private String tableSub=null;      // contient le nom du tag courant fils de <TABLE>, sinon null
   private String resourceSub=null;   // contient le nom du tag courant fils de <RESOURCE>, sinon null
   private boolean inCSV=false;       // true si on est dans un tag <DATA><CSV>
   private boolean flagTSV;           // True si le document est du TSV natif
   private boolean inError;		      // True si on est dans un tag <INFO QUERY_STATUS="ERROR">...</INFO>
   private boolean inLinkField=false; // true si on est dans un tag <FIELD><LINK>
   private boolean inFieldDesc=false; // true si on est dans un tag <FIELD><DESCRIPTION>
   private boolean inEncode64=false;  // true si on est dans un tag <STREAM encode="base64">
   private boolean inBinary=false;    // true si on est dans un tag <BINARY>
   private boolean inBinary2=false;   // true si on est dans un tag <BINARY2>
   private boolean inFits=false;      // true si on est dans un tag <FITS>
   private boolean inGroup=false;     // true si on est dans un group
   private int fitsExtNum;            // Numéro de l'extension fits en cas de <FITS extnum="x">
   private boolean inTD;			  // true si on est dan sun tag <TD>
   private boolean valueInTD;         // true si le champ courant a une valeur
   private int ngroup;				  // Profondeur de GROUP (pour compenser depth)
   private String colsep,recsep;      // Séparateurs de champs et de lignes en mode CSV/TSV
   private String headlines; 	      // nombre de lignes de l'entête CSV
   private String error; 	          // Pour staicker le message d'erreur
   private String filename;           // Fichier d'origine s'il est connu

   private int format;                // Format des coordonnées (FMT_UNKOWN | FMT_DECIMAL | FMT_SEXAGESIMAL)
   private int unit;                  // Unité des coordonnées (UNIT_UNKNOWN | UNIT_DEGREES | UNIT_RADIANS )
   //   private boolean flagSexa;	      // ture s'il s'agit de coordonnées sexagésimal
   //   private boolean knowFormat;	      // true si on a détecté le format des coordonnées

   private boolean flagXY;	          // true si il s'agit d'un catalogue en XYPOS
   private String[] record;			  // Buffer pour lire chaque enregistrement
   private Vector<String> vRecord;			  // Idem mais lorsqu'on ne connait pas encore le nombre de champs
   private int row;                   // Numéro du champ courant
   private boolean flagNewTable;	  // true si c'est le premier flux d'une table
   private boolean flagSkip;          // true si la table sera en fait ignorée (MEF ignoré)
   private long typeFmt;              // type de table (ex: MyInputStream.IPAC) uniquement dans les cas complexes
   private Hashtable<String,String> coosys;          // Liste des systèmes de coordonnées trouvés dans la VOTable
   private Hashtable<String,String> cooepoch;        // Liste des epoques trouvés dans la VOTable
   private Hashtable<String,String> cooequinox;      // Liste des équinoxes trouvés dans la VOTable
   private Hashtable<String,String> cooFieldref;     // Liste des références de FIELD pour un coosys partitulier
   private boolean inAstroCoords;     // true si on est dans un GROUP de définition d'un système de coordonnées
   private String astroCoordsID;      // Dernier ID d'une définition d'un système de coordonnées
   private Astroframe srcAstroFrame = null;     // Systeme de coord initial
   private Astroframe trgAstroFrame = AF_ICRS;  // System de coord final
   private Astropos c = new Astropos();         // coord courante
   private String filter;          // Le filtre en cours de parsing, null sinon
   private boolean inSEDGroup;     // true si on est dans un GROUP de définition d'un point SED

   // Pour le traitement des tables FITS
   private HeaderFits headerFits;
   private Aladin aladin;

   /** Création d'un parser de table suivant format
    * @param consumer référence au consumer
    * @param type Format de la table (MyInputSdtream.IPAC)
    */
   public TableParser(Aladin aladin,TableParserConsumer consumer,long type) {
      this.aladin = aladin;
      this.consumer=consumer;
      this.typeFmt=type;
   }

   /** Création d'un parser de table Fits (ASCII)
    * @param consumer référence au consumer
    * @param HeaderFits headerFits Le headerFits associé à la table ASCII
    * @param flagSkip indique si cette table doit être skippé (dans le cas d'un MEF)
    */
   public TableParser(Aladin aladin,TableParserConsumer consumer,HeaderFits headerFits,boolean flagSkip) {
      this.aladin = aladin;
      this.consumer=consumer;
      this.headerFits = headerFits;
      this.flagSkip = flagSkip;
   }

   /** Positionnement du fichier d'origine (pour message d'erreur) */
   public void setFileName(String file) { filename=file; }

   /**
    * Parsing des données XML en récupération depuis une URI,
    * soit en binaire, soit en FITS
    * @param uri   L'emplacement des données à récupérer et à perser
    * @throws Exception
    */
   private void hrefCall(String uri) throws Exception{

      // Ouverture du flux pour les données
      MyInputStream in = aladin.glu.getMyInputStream(uri,false);

      // Cas BINARY HREF
      if( inBinary || inBinary2 ) {
         consumer.tableParserInfo("\nParsing VOTable data from an external"+(inBinary2?" BINARY2":" BINARY")+" stream:\n => "+uri);
         byte [] buf = in.readFully();
         parseBin(buf,0,buf.length,inBinary2);

         //         byte [] buf = new byte[100000];
         //         int n;
         //         while( (n=in.readFully(buf))>0 ) parseBin(buf,0,n,inBinary2);

         // Cas FITS HREF
      } else if( inFits ) {

         aladin.calque.seekFitsExt(in,fitsExtNum);
         consumer.tableParserInfo("\nParsing VOTable data from a FITS stream:\n => "+uri+(fitsExtNum>0?" (ext="+fitsExtNum+")":""));
         in.resetType(); in.getType();
         headerFits = new HeaderFits(in);
         parseFits(in,true);
      }
      in.close();
   }

   /**
    * Parsing des données XML en BASE64 inline
    * @param ch Tableau de char contenant le BASE64 (éventuellement partiel - appels en plusieurs fois)
    * @param pos position du segment à parser
    * @param len taille du segment à parser
    * @throws Exception
    */
   private void parseBase64(char ch[],int pos, int len,boolean inBinary2) throws Exception {
      if( memoField!=null ) consumer.tableParserInfo("\nParsing VOTable data from a"+(inBinary2?" BINARY2":" BINARY")+" base64 stream");
      byte b[] = new byte[len];   // un peu grand mais bon !
      int offset = 0;
      if( memoB!=null ) {
         //         System.out.println("memoB!=null !! memoB.length="+memoB.length);
         b = new byte[len+memoB.length];
         System.arraycopy(memoB,0,b,0,offset=memoB.length);
         memoB=null;
      } else b = new byte[len];
      int n = Save.get64(b,offset,ch,pos,len);
      parseBin(b,0,n,inBinary2);
   }

   // Variables d'instance en cas de parseBin consécutifs
   private char [] type=null;
   private int [] pos=null;
   private int [] len=null;
   private int [] prec=null;
   private int sizeRecord=0;
   private int nbField=0;
   private byte[] memoB = null;         // Memorisation du reste du flux à reprendre en cas de reprise
   private byte [] nullMask = null;     // mask BINARY2 de la ligne courante
   private boolean maskRead=false;      // true si le mask BINARY2 de la ligne courante a déjà été lu


   /** Parsing d'un champ de bits issu d'un STREAM BINAIRE votable. Le parsing peut se faire
    * en plusieurs fois mais il est impératif que la limite de buffer ne soit pas à cheval sur
    * un champ. C'est le Vector memoField qui permet de savoir si on parse le premier
    * segment (memoField!=null ) ou les suivants (memoField=null)
    * @param b Le tableau d'octets
    * @param offset La position du début des données
    * @param length La taille des données
    * @param inBinary2 true => Votable 1.3 BINARY2 codage
    * @return true si ok, false sinon
    */
   private boolean parseBin(byte b[],int offset, int length,boolean inBinary2) throws Exception {

      if( memoField!=null ) {
         nbField = memoField.size();
         type = new char[nbField];
         pos = new int[nbField];
         len = new int[nbField];
         prec = new int[nbField];
         if( inBinary2 ) nullMask = new byte[( nbField+7)/8 ];
         nField=nRecord = 0;

         boolean variableField=false;

         for( int i=0; i<nbField; i++ ) {
            Field f = memoField.elementAt(i);
            String t = Field.typeVOTable2Fits(f.datatype);
            if( t==null ) {
               throw new Exception("Missing definition for field "+i+". Parsing aborted");
            }
            type[i]=t.charAt(0);
            len[i] = 1;

            // Champ variable => len=-1, sizeRecord=-1
            if( f.arraysize!=null && f.arraysize.equals("*") ) {
               len[i]=-1;
               sizeRecord=-1;
               variableField=true;
            }  else {
               try { len[i] = Integer.parseInt(f.arraysize); } catch( Exception e ) { len[i]=1; }
            }

            if( !variableField ) {
               if( i<nbField-1 ) pos[i+1] = pos[i] + binSizeOf(type[i],len[i]);
               else sizeRecord = pos[i] + binSizeOf(type[i],len[i]);
            } else pos[i]=-1;

            prec[i]=6;
            try { prec[i] = Integer.parseInt(f.precision); } catch( Exception e ) { prec[i]=6; }
//            System.out.println("Field "+i+" type="+type[i]+" pos="+pos[i]+" len="+len[i]+" prec="+prec[i]);
         }
         pos[0]=0;
         memoField=null;    // va permettre de répérer un appel ultérieur à parseBin en continuation
         System.out.println("length="+length+" sizeRecord="+sizeRecord);
      }

//      System.out.println("\nparseBin position="+offset+" length="+length+" nRecord="+nRecord+" nField="+nField);

      // Parsing du buffer (on reprend éventuellement là où on en était)
      int position=offset;
      while( position<length ) {
         if( nField==nbField ) {
            nField=0;
//            System.out.print(nRecord+": ");
         }

         // En BINARY2, il faut lire le masque de bits en préambule de la ligne
         if( inBinary2 && nField==0 && !maskRead ) {

            // Y a-t-il encore au-moins assez de bytes ?
            // sinon ce sera pour la prochaine fois
            int n = length-position;
            if( n<nullMask.length ) {
//               System.out.println("Memo de "+n+" bytes pour la prochaine lecture à cause du nullMask..");
               memoB = new byte[n];
               System.arraycopy(b,position,memoB,0,n);
               return true;
            }

            //System.arraycopy(b, position, nullMask, 0, nullMask.length);
            position+=nullMask.length;
            maskRead=true;
         } else maskRead=false;

         int nbBytes = len[nField]==-1 ? -1 : binSizeOf(type[nField], len[nField]);
         int lenv=0;   // Pour les champs variables.

         // Champ variable ?
         if( nbBytes==-1 ) {

            // Y a-t-il encore au-moins 4 bytes ? sinon ce sera pour la prochaine fois
            int n = length-position;
            if( n<4 ) {
//               System.out.println("Memo de "+n+" bytes pour la prochaine lecture à cause de la longueur d'un champ variable..");
               memoB = new byte[n];
               System.arraycopy(b,position,memoB,0,n);
               return true;
            }
            
            lenv = getInt(b, position);
            nbBytes =  binSizeOf( type[nField], lenv );
            position+=4;
         }

         // Le flux à du bourrage à la fin, avec en plus un champ variable
         else if( nbBytes==0 ) {
            return true;
         }
         int nextPosition = position + nbBytes;

         // Dernier champ non complet => on le garde pour la prochaine fois
         if( nextPosition>length ) {
            // Dans le cas d'un champ variable, il faut également garder sa taille
            if( len[nField]==-1 ) position-=4;
            int n = length-position;
//            System.out.println("Memo de "+n+" bytes pour la prochaine lecture à cause d'un champ non complet..");
            memoB = new byte[n];
            System.arraycopy(b,position,memoB,0,n);
            return true;
         }


         // Est-ce un champ null ?
         if( inBinary2 && isNull(nullMask,nField) ) record[nField]="null";

         // Sinon
         else record[nField] = getBinField(b,position, len[nField]==-1 ? lenv /*nbBytes */ : len[nField], type[nField],prec[nField], 0.,1.,false,0);

//         System.out.print(" "+nbBytes+"/"+record[nField]);
         position = nextPosition;
         nField++;
         if( nField==nbField ) {
            consumeRecord(record,nRecord++);
            System.out.println();
         }
      }

      return true;
   }

   static final int MASK[] = { 1<<7, 1<<6, 1<<5, 1<<4, 1<<3, 1<<2, 1<<1, 1 };

   private boolean isNull(byte [] nullMask, int nField) {
      int nByte = nField/8;
      int nBit = nField%8;
      return ( (0xFF & nullMask[ nByte ]) & MASK[nBit]) != 0;
   }

   /** Parsing d'une table Fits ASCII ou BINTABLE
    * @param in le flux
    * @param hrefVotable true s'il s'agit en fait d'un FITS référencé par un VOTable
    *        via une balise <STREAM href="...". Dans ce cas, on ne mémorise plus
    *        les métadonnées des ressource/tables/champs pour ne pas faire doublon.
    *        En revanche on se base tout de même sur l'entête FITS pour la position
    *        des champs car il est plus probable que les erreurs se trouvent dans
    *        l'entête VOTable que dans l'entête FITS.
    * @param findName true si l'on cherche à nommer la ou les ressources par leur contenu
    */
   private boolean parseFits(MyInputStream in,boolean hrefVotable) {
      int offset;
      error=null;
      String s;
      double x;

      if( !hrefVotable ) {
         initTable();
         coosys = new Hashtable<String,String>(10);
         cooepoch = new Hashtable<String,String>(10);
         cooequinox = new Hashtable<String,String>(10);
         cooFieldref = new Hashtable<String,String>(10);
      }

      try {

         // Test du fits BINAIRE
         boolean flagBin = (in.getType()& MyInputStream.FITSB )!=0;

         if( !hrefVotable ) {
            consumer.tableParserInfo( "FITS " +(flagBin? "BINTABLE":"TABLE")+" format");
            consumer.startResource("RESOURCE-FITS");
            try {
               s = headerFits.getStringFromHeader("EXTNAME");
               consumer.setResourceInfo("NAME",s);
            } catch( Exception e1 ) {}
            //          consumer.setTableInfo("NAME",s);
         }
         consumer.startTable("TABLE-FITS");

         int nRecord = headerFits.getIntFromHeader("NAXIS2");
         int sizeRecord = headerFits.getIntFromHeader("NAXIS1");
         nField = headerFits.getIntFromHeader("TFIELDS");

         char [] type = null;
         int [] pos = new int[nField];     // position du champ dans l'enregistrement courant
         int [] len = new int[nField];     // nombre d'octets (ASCII) ou nombre d'items (BINAIRE) du champ
         boolean flagTzeroTscal = false;   // true si au moins un champ utilise TZERO et TSCAL
         double [] tzero = new double[nField]; // Valeur tzero de chaque champ
         double [] tscal = new double[nField]; // Valeur tscal de chaque champ
         String [] tnull = new String[nField]; // Valeur tnull de chaque champ (ASCII), ou null si non défini
         int [] tinull = new int[nField];  // Valeur tnull de chaque champ (BINAIRE) si tnull[] correspondant != null
         int [] prec = new int[nField];    // Précision à l'affichage de chaque champ

         // Initialisation par défaut
         for( int k=0; k<nField; k++ ) prec[k]=-1;
         for( int k=0; k<nField; k++ ) tscal[k]=1.;
         if( flagBin ) type = new char[nField];

         // Boucle sur chaque champ
         for( int i=0; i<nField; i++ ) {

            // Récupération du nom du champ
            s=null;
            try { s = headerFits.getStringFromHeader("TTYPE"+(i+1)); } catch( Exception e1 ) {}
            if( s==null ) s = "Col-"+(i+1);
            Field f = new Field(s);

            // Récupération des unites
            s=null;
            try { s = headerFits.getStringFromHeader("TUNIT"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) f.unit=s;

            // Récupération d'une éventuelle descriptino
            s=null;
            try { s = headerFits.getStringFromHeader("TCOMM"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) f.description=s;

            // Récupération d'un éventuel TZERO
            x=0;
            try { x = headerFits.getDoubleFromHeader("TZERO"+(i+1)); } catch( Exception e1 ) {}
            if( x!=0 ) { tzero[i]=x; flagTzeroTscal=true; }

            // Récupération d'un éventuel TSCAL
            x=0;
            try { x = headerFits.getDoubleFromHeader("TSCAL"+(i+1)); } catch( Exception e1 ) {}
            if( x!=0 ) { tscal[i]=x; flagTzeroTscal=true; }

            // Récupération d'un éventuel TNULL
            s=null;
            try { s = headerFits.getStringFromHeader("TNULL"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) {
               tnull[i] = s.trim();
               if( flagBin ) try { tinull[i] = headerFits.getIntFromHeader("TNULL"+(i+1)); } catch( Exception e1 ) {}
            }

            // Récupération d'un éventuel TUCD
            s=null;
            try { s = headerFits.getStringFromHeader("TUCD"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) f.ucd=s;

            //System.out.println("TZERO"+(i+1)+"="+tzero[i]+" TSCAL"+(i+1)+"="+tscal[i]
            //                      +(tnull[i]!=null?" TNULL"+(i+1)+"="+tnull[i] : ""));

            // Récupération du format. Dans le cas de l'ASCII, donne le format de sortie
            // alors que dans le cas du binaire, donne le format d'entrée
            s=null;
            try { s = headerFits.getStringFromHeader("TFORM"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) {

               // Mode ASCII
               if( !flagBin ){
                  //                  f.datatype="BIJFKED".indexOf(s.charAt(0))>=0 ? "D":"A";
                  char code = s.charAt(0);
                  if( code=='E' || code=='F' ) code='D';
                  f.datatype=code+"";

                  // Détermination de la taille du champ
                  int k = s.indexOf('.');
                  if( k<0 ) k = s.length();
                  if( k>1 ) {
                     f.width = s.substring(1,k);
                     f.computeColumnSize();
                  }

                  // Détermination de la précision
                  k = s.indexOf('.');
                  if( k>0 ) {
                     prec[i]=0;
                     for( k++; k<s.length() && Character.isDigit(s.charAt(k)); k++ ) {
                        prec[i] = prec[i]*10 + (s.charAt(k)-'0');
                     }
                  }

                  //System.out.println("TFORM"+(i+1)+" width="+f.width+" prec="+prec[i]);

               }

               // Parsing du type de données FITS BINTABLE
               else {
                  int k;
                  len[i]=0;
                  for( k=0; k<s.length() && Character.isDigit(s.charAt(k)); k++ ) {
                     len[i] = len[i]*10 + (s.charAt(k)-'0');
                  }
                  if( k==0 ) len[i]=1;
                  else f.arraysize = len[i]+"";
                  type[i] = s.charAt(k);
                  pos[i] = i==0 ? 0 : pos[i-1]+binSizeOf(type[i-1],len[i-1]);
                  //System.out.println("TFORM"+(i+1)+" len="+len[i]+" type="+type[i]+" pos="+pos[i]);
                  //                  f.datatype="FBIJEDK".indexOf(type[i])>=0 ? "D":"A";   // Si TDISPn n'est pas donné
               }
            }

            // Petites particularités BINAIRE
            if( flagBin ) {
               //               if( f.datatype==null ) f.datatype="FGBIJEDK".indexOf(type[i])>=0 ? "D":"A";  // BIZARRE ! MAIS CA NE MANGE PAS DE PAIN
               if( f.datatype==null ) f.datatype=type[i]+"";
            }

            // Récupération du display dans le cas BINAIRE
            // Dans le cas ASCII, surcharge d'une éventuelle définition TFORM préalable
            s=null;
            try { s = headerFits.getStringFromHeader("TDISP"+(i+1)); } catch( Exception e1 ) {}
            if( s!=null ) {


               // Détermination de la taille du champ
               int k = s.indexOf('.');
               if( k<0 ) k = s.length();
               if( k>1 ) {
                  f.width = s.substring(1,k);
                  f.computeColumnSize();
               }

               // Détermination de la précision
               k = s.indexOf('.');
               if( k>0 ) {
                  prec[i]=0;
                  for( k++; k<s.length() && Character.isDigit(s.charAt(k)); k++ ) {
                     prec[i] = prec[i]*10 + (s.charAt(k)-'0');
                  }
               }

               //System.out.println("TDISP"+(i+1)+" width="+f.width+" prec="+prec[i]);
            }

            // ON RECUPERERA LA DESCRIPTION DE LA COLONNE (COMMENTAIRE FITS) UNE AUTRE FOIS
            // C'EST DU SUR-MESURE FOX

            // Quels sont les limites des champs dans le cas ASCII
            if( !flagBin ) {
               pos[i] = headerFits.getIntFromHeader("TBCOL"+(i+1));
               int p = sizeRecord;
               try { p = headerFits.getIntFromHeader("TBCOL"+(i+2)); } catch( Exception e1 ) {}
               len[i] = p - pos[i];
               pos[i]--;		// Commence à l'index 0 et non 1 comme FITS
               f.width = len[i]+"";
            }

            // Traitement du champ
            detectPosField(f,i);
            if( !hrefVotable ) consumer.setField(f);

         }

         if( !hrefVotable ) posChooser();

         // On ne fait que skipper cette table (cas MEF ignoré)
         if( flagSkip ) in.skip(nRecord * sizeRecord);

         // Lecture effective
         else {

            // Lecture 1000 enregistrements par 1000 enregistrements
            byte [] buf = new byte[ (nRecord<1000 ? nRecord:1000) * sizeRecord ];

            record = new String[ nField ];
            offset=buf.length;
            for( int i=0; i<nRecord; i++, offset+=sizeRecord ) {

               // Rechargement du buffer
               if( offset==buf.length ) {
                  try { in.readFully(buf); } catch( EOFException e ) {}
                  offset=0;
               }

               // Traitement des champs de l'enregistrement courant
               for( int j=0; j<nField; j++ ) {

                  // case ASCII
                  if( !flagBin ) {
                     record[j] = getStringTrim(buf,offset+pos[j],len[j]);

                     // Valeur NULL
                     if( tnull[j]!=null && record[j].equals(tnull[j]) ) record[j]="";

                     // Changement d'échelle et précision dans le cas ASCII
                     else if( flagTzeroTscal || prec[j]>=0 ) {
                        try {
                           record[j] = fmt( Double.valueOf(record[j]).doubleValue(),
                                 prec[j], tzero[j],tscal[j]);
                        }catch( Exception e ) { record[j] = "[?X?]"; }
                     }

                     // cas BINAIRE
                  } else {
                     //                     String val =
                     record[j] = getBinField(buf,offset+pos[j],len[j],type[j],prec[j],
                           flagTzeroTscal?tzero[j]:0., flagTzeroTscal?tscal[j]:1., tnull[j]!=null,tinull[j]);
                     //                     System.out.println("Lecture champ "+(j+1)+" pos="+pos[j]+" type="+len[j]+type[j]+" => "+val);
                  }
               }

               // "Consommation" de l'enregistrement courant
               consumeRecord(record,i);
            }
         }

         if( !hrefVotable ) {
            consumer.endTable();
            consumer.endResource();

            // On va encore skipper une éventuelle subextension (PCOUNT!=0 )
            try {
               offset = headerFits.getIntFromHeader("PCOUNT");
               if( offset!=0 ) in.skip(offset);
            } catch( Exception e1 ) {}
         }

      } catch( Exception e ) {
         e.printStackTrace();
         error = e.getMessage();
      }

      return (error==null);
   }

   /** Retourne le nombre d'octets d'un champ BINTABLE
    * @param n le nombre d'items
    * @param type le code du type de données
    * @return le nombre d'octets
    */
   final protected int binSizeOf( char type, int n) {
      if( type=='X' ) return n/8 + (n%8>0 ? 1:0);  // Champ de bits
      int sizeOf = type=='L'? 1:
         type=='B'? 1:
            type=='I'? 2:
               type=='J'? 4:
                  type=='K'? 8:
                     type=='A'? 1:
                        type=='E'? 4:
                           type=='D'? 8:
                              type=='C'? 8:
                                 type=='M'? 16:
                                    type=='P'? 8:
                                       type=='U'? 2:
                                       0;
      if( sizeOf==0 ) {
         System.out.println("Problème sérieux pour ["+type+"]");
      }
      return sizeOf * n;
   }


   /**
    * Conversion d'un champ d'octets en valeur sont la forme d'un String
    * @param t le tableau des octets
    * @param i le premier octet concernés
    * @param n le nombre d'éléments
    * @param type la lettre code FITS BINTABLE
    * @param prec la précision à afficher
    * @param tzero pour un éventuel changement d'échelle
    * @param tscal pour un éventuel changement d'échelle
    * @return la chaine correspondante à la valeur
    */
   final private String getBinField(byte t[],int i, int n, char type,
         int prec, double tzero,double tscale,
         boolean hasNull,int tnull) {
      if( n==0 ) return "";
      if( type=='A' ) return getStringTrim(t,i,n);
//      if( type=='U' ) return getStringUTrim(t,i,n);
      if( n==1 ) return getBinField(t,i,type,prec,tzero,tscale,hasNull,tnull);

      StringBuilder a=null;
      for( int j=0; j<n; j++ ) {
         if( j==0 ) a = new StringBuilder();
         else if( type!='U' ) a.append(' ');
         a.append( getBinField(t,i+binSizeOf(type,j),type,prec,tzero,tscale,hasNull,tnull) );
      }
      return a+"";
   }

   final private String getBinField(byte t[],int i, char type,int p,double z,double s,
         boolean hasNull, int n) {

      String a = getBinField1(t,i,type,p,z,s,hasNull,n);
      return a;
   }



   /**
    * Conversion d'un champ d'octets en valeur sont la forme d'un String
    * @param t le tableau des octets
    * @param i le premier octet concernés
    * @param type la lettre code FITS BINTABLE
    * @param p la précision à afficher
    * @param z pour un éventuel changement d'échelle
    * @param s pour un éventuel changement d'échelle
    * @param hasNull s'il y a une valeur indéfinie positionnée
    * @param n valeur indéfinie (uniquement pour les types B,I, et J
    * @return la chaine correspondante à la valeur
    */
   final private String getBinField1(byte t[],int i, char type,int p,double z,double s,
         boolean hasNull, int n) {
      long a,b;
      int c;

      switch(type) {
         case 'L': return t[i]!=0 ? "T":"F";
         case 'B': return fmtInt( ((t[i])&0xFF),p,z,s,hasNull,n);
         case 'I': return fmtInt( getShort(t,i),p,z,s,hasNull,n);
         case 'J': return fmtInt( getInt(t,i),p,z,s,hasNull,n);
         case 'K': a = (((long)getInt(t,i))<<32)
               | ((getInt(t,i+4))&0xFFFFFFFFL);
         return fmtLong(a,p,z,s,hasNull,n);
         case 'E': return fmt( Float.intBitsToFloat( getInt(t,i) ),p,z,s );
         case 'D': a = (((long)getInt(t,i))<<32)
               | ((getInt(t,i+4))& 0xFFFFFFFFL);
         return fmt( Double.longBitsToDouble(a),p,z,s );
         case 'C': c =  getInt(t,i+4);
         return fmt( Float.intBitsToFloat( getInt(t,i) ),p,z,s )
               +(c>=0?"+":"-")
               +fmt( Float.intBitsToFloat(c),p,z,s )+"i";
         case 'M': a = (((long)getInt(t,i))<<32)
               | ((getInt(t,i+4))&0xFFFFFFFFL);
         b = (((long)getInt(t,i+8))<<32)
               | ((getInt(t,i+12))&0xFFFFFFFFL);
         return fmt( Double.longBitsToDouble(a),p,z,s )
               +(b>=0?"+":"-")
               +fmt( Double.longBitsToDouble(b),p,z,s )+"i";
         case 'U': 
            try { return new String(t, i, 2, utf16); }
            catch( Exception e) { return "[??]"; }
         case 'A': return ""+( (char)t[i]);
         default: return "[???]";
      }
   }
   
   static private Charset utf16 = Charset.forName("UTF-16");
   

   /**
    * Mise en forme d'un entier
    * @param x l'entier à traiter
    * @param prec la précision à afficher (-1 si non précisée)
    * @param tzero le décallage
    * @param tscale le changement d'échelle
    * @param hasNull true s'il y a une valeur tnull définie
    * @param tnull la valeur pour non définie
    * @return la valeur formatée
    */
   final private String fmtInt(long x,int prec,double tzero,double tscale,
         boolean hasNull,int tnull)  {
      if( hasNull && tnull==x ) return "";
      double y=x;
      if( tscale!=1. ) y*=tscale;
      if( tzero!=0.  ) y+=tzero;
      if( prec>=0 ) y= Util.round(y,prec);

      if( y!=x ) return y+"";
      return x+"";
   }

   /**
    * Mise en forme d'un entier long
    * @param x l'entier à traiter
    * @param prec la précision à afficher (-1 si non précisée)
    * @param tzero le décallage
    * @param tscale le changement d'échelle
    * @param hasNull true s'il y a une valeur tnull définie
    * @param tnull la valeur pour non définie
    * @return la valeur formatée
    */
   final private String fmtLong(long x,int prec,double tzero,double tscale,
         boolean hasNull,int tnull)  {
      if( hasNull && tnull==x ) return "";
      if( tscale==1 && tzero==0 ) return x+"";

      // Calcul entier (la précision n'est pas prise en compte)
      if( (long)tscale==tscale && (long)tzero==tzero ) {
         long y=x;
         if( tscale!=1. ) y*=(long)tscale;
         if( tzero!=0.  ) y+=(long)tzero;
         return y+"";
      }

      // Calcul réel
      double y=x;
      if( tscale!=1. ) y*=tscale;
      if( tzero!=0.  ) y+=tzero;
      if( prec>=0 ) y= Util.round(y,prec);

      if( y!=x ) return y+"";
      return x+"";
   }

   /**
    * Mise en forme d'un double
    * @param x le double à traiter
    * @param prec la précision à afficher (-1 si non précisée)
    * @param tzero le décallage
    * @param tscale le changement d'échelle
    * @return la valeur formatée
    */
   final private String fmt(double x,int prec,double tzero,double tscale)  {
      if( Double.isNaN(x) ) return "";
      if( tscale!=1. ) x*=tscale;
      if( tzero!=0.  ) x+=tzero;
      if( prec>=0 ) x = Util.round(x,prec);
      return x+"";
   }


   // Conversion byte[] en entier 32
   // Recupere sous la forme d'un entier 32bits un nombre entier se trouvant
   // a l'emplacement i du tableau t[]
   final private int getInt(byte[] t,int i) {
      return ((t[i])<<24) | (((t[i+1])&0xFF)<<16)
            | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF;
   }

   // Conversion byte[] en entier 16
   // Recupere sous la forme d'un entier 16bits un nombre entier se trouvant
   // a l'emplacement i du tableau t[]
   final private int getShort(byte[] t,int i) {
      return  (((t[i])&0xFF)<<8) | (t[i+1])&0xFF;
   }


   //   private void showByte(String s) {
   //      for( int i=0; i<s.length(); i++ ) {
   //         System.out.print(s.charAt(i)+"/"+(byte)s.charAt(i)+" ");
   //      }
   //      System.out.println();
   //   }

   static final char BLC = ' ';
   static final char TABC = '\t';
   static final byte BLB = (byte)' ';
   static final byte TABB = (byte)'\t';

   /** Extrait la chaine de caractères indiquée par la paramètre en trimmant
    * les éventuels blancs en début et fin de chaine
    * => Equivalent a : new String(s,offset,len).trim()
    * @param s le buffer de byte
    * @param offset début de la chaine
    * @param len taille de la chaine
    * @return chaine extraite trimmée
    */
   static final public String getStringTrim(byte s[],int offset,int len) {
      if( len<0 || offset+len>s.length ) {
         System.err.println("problem s.length="+s.length+" offset="+offset+" len="+len+" !!");
         return new String(s,offset,s.length-offset);
      }
      return (new String(s,offset,len)).trim();
      //      int deb,fin,j;
      //      for( deb=offset, j=0; j<len && (s[deb]==BLB || s[deb]==TABB); deb++, j++);
      //      for( fin = offset+len-1; fin>deb && (s[fin]==BLB || s[fin]==TABB); fin--);
      //      return new String(s,deb,fin-deb+1);
   }
   
//   static final public String getStringUTrim(byte s[],int offset,int len) {
//      return (new String(s,offset,len,utf16)).trim();
//   }

   /** Extrait la chaine de caractères indiquée par la paramètre en trimmant
    * les éventuels blancs en début et fin de chaine
    * => Equivalent a : new String(s,offset,len).trim()
    * @param s le buffer de char
    * @param offset début de la chaine
    * @param len taille de la chaine
    * @return chaine extraite trimmée
    */
   static final public String getStringTrim(char s[],int offset,int len) {
      return (new String(s,offset,len)).trim();
   }

   /** Création
    * Mémorise le consumer et création du parser XML
    * @param consumer référence au consumer
    * @param colsep en mode CSV, la liste des séparateurs de colonnes pris en compte
    */
   public TableParser(Aladin aladin, TableParserConsumer consumer) { this(aladin,consumer,(String)null); }
   public TableParser(Aladin aladin,TableParserConsumer consumer,String colsep) {
      this.aladin = aladin;
      this.consumer=consumer;
      if( colsep!=null ) this.colsep=colsep;
      xmlparser = new XMLParser(this);
   }

   /** Lancement du parsing
    * Soit XML/CSV soit FITS si headerFits!=null
    * @param dis
    * @returntrue or false en fonction du résultat du parsing. En cas de problème
    *         l'erreur peut être récupérée par la méthode getError()
    * @throws Exception
    */
   public boolean parse(MyInputStream dis) throws Exception {
      if( headerFits!=null ) return parseFits(dis,false);
      return parse(dis,null);
   }


   /** Lancement du parsing
    * @param dis le flux d'entrée
    * @param endTag tag de fin si parsing partiel (Le MyInputStream reste ouvert en l'état)
    * @return true or false en fonction du résultat du parsing XML. En cas de problème
    *         l'erreur peut être récupérée par la méthode getError()
    * @throws Exception
    */
   public boolean parse(MyInputStream dis,String endTag) throws Exception {
      initTable();
      error=null;
      flagTSV=true;
      inError=false;
      ngroup=0;
      coosys = new Hashtable<String,String>(10);
      cooepoch = new Hashtable<String,String>(10);
      cooequinox = new Hashtable<String,String>(10);
      cooFieldref = new Hashtable<String,String>(10);

      return (xmlparser.parse(dis,endTag) && error==null /* && nField>1 */ );
   }

   /** Retourne les caractères non lus du buffer du parser xml, ou null si fin du stream */
   public byte [] getUnreadBuffer() { return xmlparser.getUnreadBuffer(); }
   /**
    * Initialisation des flags et variables au début d'une nouvelle table
    * à parser.
    */
   private void initTable() {
      nRA=nDEC=nPMRA=nPMDEC=nX=nY=-1;
      qualRA=qualDEC=qualRA=qualDEC=qualX=qualY=qualPMRA=qualPMDEC=1000; // 1000 correspond au pire
      format = FMT_UNKNOWN;
      unit = UNIT_UNKNOWN;
      //      flagSexa=false;	// Par défaut on suppose des coord. en degrés
      //      knowFormat=false; // Par défaut on ne connait pas le format des coord.
      nField=0;
      record=null;
      vRecord=null;
      flagNewTable=true;
      astroCoordsID=null;
      inAstroCoords=false;
      inSEDGroup=false;
   }

   /** Retourne le message d'erreur du parsing, ou null si ok */
   public String getError() { return error!=null?error:xmlparser.getError(); }

   // Mémorisation du début d'un groupe de définitions
   private void memoStartGroup(String name, Hashtable atts) {
      StringBuffer s = new StringBuffer("<"+name);
      Enumeration<String> e = atts.keys();
      while( e.hasMoreElements() ) {
         String k = e.nextElement();
         s.append(" "+k+"=\""+atts.get(k)+"\"");
      }
      s.append(">\n");
      consumer.setTableInfo("GROUP", s.toString());
   }

   // Mémorisation de la fin d'un groupe de définitions
   private void memoEndGroup(String name) {
      consumer.setTableInfo("GROUP", "</"+name+">\n");
   }

   // Mémorisation du contenu d'un groupe de définitions
   private void memoInGroup(char [] s, int offset, int length) {
      consumer.setTableInfo("GROUP", new String(s,offset,length));
   }

   // reset a posteriori d'un groupe de définitions (pour écarter les cas où le GROUP est utilisé
   // pour encadrer des <FIELD> et non pas pour passer des définitions).
   // Pas très joli - mais on fait au plus simple
   private void resetGroup() {
      System.err.println("TableParser.resetGroupe => FIELD GROUP not yet supported => remove all GROUP definition !");
      consumer.setTableInfo("GROUP",null);
   }

   static private int ASTROID=1;

   /** XMLparser interface.
    * Pour accélerer le parsing, on se base sur la profondeur du document XML pour
    * faire uniquement les quelques tests s'y rapportant
    */
   public void startElement (String name, Hashtable atts) {
      String att;
      String id;
      String v;
      //      int depth = xmlparser.getStack().size();
      int depth = xmlparser.getDepth();

      if( inGroup ) memoStartGroup(name,atts);


      // Pour ne pas prendre en compte la profondeur lié aux groupes
      // et également repérer les définitions de systèmes de coordonnées VOTable 1.2
      if( name.charAt(0)=='G' && depth>=1 && name.equalsIgnoreCase("GROUP") ) {
         ngroup++;
         if( !inGroup ) memoStartGroup(name,atts);
         inGroup=true;

         // Récupération de l'ID pour une définition de système de coordonnées
         // et également pour prendre en compte la méthodes suivante:
         // <GROUP ID="id" utype="stc:AstroCoords" ref="ivo://STClib/CoordSys#UTC-ICRS-TOPO"...>
         att =  (String)atts.get("utype");
         id = (String)atts.get("ID");
         if( att!=null && (att.equalsIgnoreCase("stc:AstroCoords")
               || att.equalsIgnoreCase("stc:AstroCoordSystem") || att.equalsIgnoreCase("stc:CatalogEntryLocation")) ) {
            inAstroCoords=true;
            astroCoordsID = (String)atts.get("ID");
            v = (String)atts.get("ref");
            if( v!=null && astroCoordsID!=null ) coosys.put(astroCoordsID,v);
         }

         else if( att!=null && att.equalsIgnoreCase("spec:PhotometryPoint")
               || id!=null && id.equals("gsed")) {
            //            System.out.println("J'ai trouvé un GROUP SED");
            inSEDGroup=true;
         }

         return;
      }
      depth-=ngroup;

      // Positionnement des ID de colonnes portant les valeurs d'un point SED
      if( inSEDGroup && name.equalsIgnoreCase("FIELDref")) {
         att =  (String)atts.get("utype");
         if( att!=null ) {
            if( att.equalsIgnoreCase("photdm:PhotometryFilter.SpectralAxis.Coverage.Location.Value") ) sFreq = (String)atts.get("ref");
            else if( att.equalsIgnoreCase("spec:PhotometryPoint") ) sFlux = (String)atts.get("ref");
            else if( att.equalsIgnoreCase("spec:PhotoMetryPointError") ) sFluxErr = (String)atts.get("ref");
            else if( att.equalsIgnoreCase("photdm:PhotometryFilter.identifier") ) sSedId = (String)atts.get("ref");
         }
      }

      // Support systèmes de coordonnées du genre:
      // <GROUP ID="Coo1" utype="stc:AstroCoords" >
      //    <PARAM ... utype="stc:AstroCoords.coord_system_id" value="UTC-ICRS-TOPO" />
      // ou bien
      // <GROUP utype="stc:CatalogEntryLocation">
      //    <PARAM ... />
      //    <FIELDref ref="ra" utype="stc:AstroCoords.Position2D.Value2.C1" />
      if( inAstroCoords ) {

         // Pas d'ID pour le système de coord, on en invente un
         if( astroCoordsID==null ) {
            astroCoordsID = "_DEFAULTID_"+(ASTROID++);
         }

         if( name.equalsIgnoreCase("PARAM") ) {
            att =  (String)atts.get("utype");

            if( att!=null && (att.equalsIgnoreCase("stc:AstroCoords.coord_sys_id")
                  || att.equalsIgnoreCase("stc:AstroCoords.coord_system_id")
                  || att.equalsIgnoreCase("stc:AstroCoordSystem.href")
                  || att.equalsIgnoreCase("stc:AstroCoordSystem.SpaceFrame.CoordRefFrame")) ) {
               v = (String)atts.get("value");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => "+v);
                  coosys.put(astroCoordsID,v);
               }
            }

            else if( att!=null &&
                  (Util.matchMaskIgnoreCase("stc:Astrocoords.Position?D.Epoch", att)
                        || att.equalsIgnoreCase("stc:AstroCoords.SpaceFrame.Epoch")) ) {
               //                || att.equalsIgnoreCase("stc:AstroCoords.Position2D.Epoch")
               //                || att.equalsIgnoreCase("stc:AstroCoords.Position3D.Epoch") )) {
               v = (String)atts.get("value");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => "+v);

                  // Peut être le B ou le J est-il déjà renseigné séparément ?
                  if( !Character.isDigit( v.charAt(0) ) ) {
                     String s = cooepoch.get(astroCoordsID);
                     if( s!=null && s.length()==1 ) v=s+v;
                  }
                  cooepoch.put(astroCoordsID,v);
               }
            }

            else if( att!=null &&
                  Util.matchMaskIgnoreCase("stc:Astrocoords.Position?D.Epoch.yearDef", att) ) {
               //                  (att.equalsIgnoreCase("stc:AstroCoords.Position2D.Epoch.yearDef")
               //                || att.equalsIgnoreCase("stc:AstroCoords.Position3D.Epoch.yearDef")) ) {
               v = (String)atts.get("value");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => "+v);

                  // Peut être l'année a-t-elle déjà été renseignée ?
                  String s = cooepoch.get(astroCoordsID);
                  if( s!=null && Character.isDigit( s.charAt(0) ) ) {
                     v=v+s;
                  }
                  cooepoch.put(astroCoordsID,v);
               }
            }

            else if( att!=null && att.equalsIgnoreCase("stc:AstroCoordSystem.SpaceFrame.CoordRefFrame.Equinox") ) {
               v = (String)atts.get("value");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => "+v);

                  // Peut être le B ou le J est-il déjà renseigné séparément ?
                  if( !Character.isDigit( v.charAt(0) ) ) {
                     String s = cooequinox.get(astroCoordsID);
                     if( s!=null && s.length()==1 ) v=s+v;
                  }
                  cooequinox.put(astroCoordsID,v);
               }
            }
            else if( att!=null && att.equalsIgnoreCase("stc:AstroCoordSystem.SpaceFrame.CoordRefFrame.Equinox.yearDef") ) {
               v = (String)atts.get("value");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => "+v);

                  // Peut être l'année a-t-elle déjà été renseignée ?
                  String s = cooequinox.get(astroCoordsID);
                  if( s!=null && Character.isDigit( s.charAt(0) ) ) {
                     v=v+s;
                  }
                  cooequinox.put(astroCoordsID,v);
               }
            }

            else if( att!=null ) consumer.tableParserInfo("      *** AstroCoord PARAM utype unknown => ignored: ["+att+"]");

         } else if( name.equalsIgnoreCase("FIELDref") ) {
            att =  (String)atts.get("utype");
            if( att!=null &&
                  Util.matchMaskIgnoreCase("stc:Astrocoords.Position?D.Value*", att) ) {
               //                  att.startsWith("stc:AstroCoords.Position2D.Value2") ) {
               v = (String)atts.get("ref");
               if( v!=null ) {
                  //                  consumer.tableParserInfo("   -"+att+" => ref="+v);
                  cooFieldref.put(v,astroCoordsID);
               }
            }

            else if( att!=null ) consumer.tableParserInfo("      *** AstroCoord FIELDref utype unknown => ignored: ["+att+"]");

         }
      }

      // Traitement de quelques balises qui peuvent avoir des profondeurs diverses
      // sans pour autant entrer dans les FIELD ou DATA
      if( depth<4 ) {
         if( name.equals("INFO") ) {
            att=(String)atts.get("name");
            if( att!=null && att.equals("AladinFilter") ) {
               filter="";
               att=(String)atts.get("value");
               if( att!=null) filter="#"+att+"\n";
               att=(String)atts.get("ID");
               if( att!=null ) filter=filter+"filter "+att+" {\n";

            } else if( att!=null && att.equals("QUERY_STATUS") ) {
               att=(String)atts.get("value");
               if( att.equals("OVERFLOW") ) consumer.tableParserWarning("!!! Truncated result (server OVERFLOW)");
               else if( att.equals("ERROR") ) consumer.tableParserWarning("!!! Result error (server ERROR)");
            }
         } else if( name.equalsIgnoreCase("COOSYS") ) {
            id=(String)atts.get("ID");
            if( id!=null ) {
               if( (att=(String)atts.get("system"))!=null )  coosys.put(id,att);
               if( (att=(String)atts.get("epoch"))!=null )   cooepoch.put(id,att);
               if( (att=(String)atts.get("equinox"))!=null ) cooequinox.put(id,att);
            }
         }
      }

      //System.out.println("StartElement (depth="+depth+"): "+name);
      switch(depth) {
         case 6:
            if( name.equalsIgnoreCase("TR") ) { row=0; }
            else if( name.equalsIgnoreCase("STREAM") ) {
               att = (String)atts.get("encoding");
               if( att!=null && att.equalsIgnoreCase("base64") ) inEncode64=true;
               else {
                  att = (String)atts.get("href");
                  try { hrefCall(att); }
                  catch( Exception e ) {
                     inError=true;
                     error = "VOTable external reference error ["+att+"]";
                     if( Aladin.levelTrace>=3 ) e.printStackTrace();
                     return;
                  }
               }
            }
            break;
         case 7:
            if( name.equalsIgnoreCase("TD") ) { inTD=true; }
            break;
         case 1:
            if( name.equalsIgnoreCase("ASTRO") ) {
               consumer.tableParserInfo("Astrores format");
               flagTSV=false;
            }
            else if( name.equalsIgnoreCase("VOTABLE") ) {
               consumer.tableParserInfo("VOTable format");
               flagTSV=false;
            }
         case 2:
            // Traitement des erreurs "à la VizieR"
            if( name.equalsIgnoreCase("INFO") ) {
               att=(String)atts.get("name");
               if( att!=null && (att.equalsIgnoreCase("ERRORS")
                     || att.equalsIgnoreCase("ERROR") ) ) {
                  att=(String)atts.get("value");
                  error=att==null ? "Unknown VOTable error" : att;
                  return;
               }
               att = (String)atts.get("ID");
               if( att!=null && att.equals("Target") ) {
                  att=(String)atts.get("value");
                  if( att!=null ) consumer.setTarget(att);
               }
            } else if( name.equalsIgnoreCase("RESOURCE") /* xmlparser.in("RESOURCE")*/ ) {
               if( (att=(String)atts.get("ID"))!=null ) consumer.startResource(att);
               else consumer.startResource((String)atts.get("name"));
            }
            break;
         case 3:
            if( name.equalsIgnoreCase("INFO") ) {
               att=(String)atts.get("name");

               // Traitement des erreurs "A la SIAP"
               if( att!=null && att.equals("QUERY_STATUS") ) {
                  att=(String)atts.get("value");
                  if( att!=null && att.equalsIgnoreCase("ERROR")) {
                     error="je me suis planté";
                     inError=true;
                     return;
                  }
               }
            } else if( name.equalsIgnoreCase("TABLE") ) {
               initTable();
               if( (att=(String)atts.get("name"))!=null ) consumer.startTable(att);
               else consumer.startTable(att=(String)atts.get("ID"));

               consumer.tableParserInfo(".Table "+att);

               if( (att=(String)atts.get("name"))!=null ) consumer.setTableInfo("name",att);

            }
            break;
         case 4:
            if( name.equalsIgnoreCase("FIELD") ) {
               if( inGroup ) { resetGroup(); inGroup=false; }
               f = new Field(atts);

               // POUR LE MOMENT ON EN FAIT RIEN
               // Juste pour faire l'allocation de l'objet, sinon il va y avoir des null references
            } else if( name.equalsIgnoreCase("PARAM")) {
               f = new Field(atts);

            } else if( name.equalsIgnoreCase("DATA") ) {
               record = new String[nField];
               posChooser();

            }
            break;
         case 5:
            if( name.equalsIgnoreCase("LINK") ) {
               f.addInfo("href",(String)atts.get("href"));
               f.addInfo("gref",(String)atts.get("gref"));
               f.addInfo("refValue",(String)atts.get("content-type"));
               f.addInfo("refText",(String)atts.get("title"));
               inLinkField=true;

            } else if( name.equalsIgnoreCase("VALUES") ) {
               f.nullValue = (String)atts.get("null");

            } else if( name.equalsIgnoreCase("DESCRIPTION") ) {
               inFieldDesc=true;

            } else if( name.equalsIgnoreCase("CSV") ) {  // Astrores
               inCSV=true;
               colsep=(String)atts.get("colsep");
               recsep=(String)atts.get("recsep");
               headlines=(String)atts.get("headlines");
            } else if( name.equalsIgnoreCase("BINARY") ) inBinary=true;
            else if( name.equalsIgnoreCase("BINARY2") ) inBinary2=true;
            else if( name.equalsIgnoreCase("FITS") ) {
               inFits=true;
               if( (att=(String)atts.get("extnum"))!=null ) {
                  fitsExtNum = Integer.parseInt(att);
               } else fitsExtNum = 1;
            }
            break;

      }

      if( depth>4 ) fieldSub = name;
      else if( depth>3 ) tableSub = name;
      else if( depth>2 ) resourceSub = name;

      //System.out.println("fieldSub="+(fieldSub==null?"null":fieldSub)+
      //                  " tableSub="+(tableSub==null?"null":tableSub)+
      //                  " resourcedSub="+(resourceSub==null?"null":resourceSub));

   }
   
   
   // Indique si on est déjà passé par posChooser()
   private boolean flagPosChooser=false;

   /** Determine si le catalogue dispose de coord., ou de position XY en fonction
    * des valeurs nRA,nDE,nX et nY
    * On en profite pour passer les infos sur les IDs des colonnes portant les mesures
    * d'un point de SED.
    */
   private void posChooser() {

      flagPosChooser=true;
      inAstroCoords=false;

      if( inSEDGroup ) {
         consumer.tableParserInfo("   -SED system found:");
         if( sFreq!=null )    consumer.tableParserInfo("      .frequence col #"+nFreq+1);
         if( sFlux!=null )    consumer.tableParserInfo("      .flux col      #"+nFlux+1);
         if( sFluxErr!=null ) consumer.tableParserInfo("      .fluxError col #"+nFluxErr+1);
         if( sSedId!=null )   consumer.tableParserInfo("      .SEDid col     #"+nSedId+1);
         inSEDGroup=false;
      }

      // Par défaut, ce sera du ICRS
      srcAstroFrame=null;

      // Aucune colonne ne ressemble de près ou de loin à des coordonnées
      if( nRA<0 || nDEC<0 ) {
         if( !(flagXY=(nX>=0 && nY>=0))) { nRA=0; nDEC=1; }
         else consumer.setTableInfo("__XYPOS","true");
      }

      // Si un COOSYS ref a été utilisé, on va se baser sur lui pour déterminer de façon certaine
      // les colonnes utilisant le même COOSYS
      if( coosys!=null ) {
         
         // On recherche le "ref" du champ de coordonnées le plus probable pour RA
         Field f = memoField.elementAt( nRA );
         if( f.ref!=null ) {
            
            // Pour tous les champs associés aux coordonnées qui n'ont pas de ref (ou pas le même ref),
            // je vais vérifier voire ajuster le choix
            boolean flagDec=false, flagPmra=false, flagPmdec=false;  // Liste des éléments des coordonnées à vérifier
            Field f1;
            boolean ok=false;
            if( nDEC>=0 )   { f1 =  memoField.elementAt( nDEC );   ok = f.ref.equals( f1.ref ); }
            flagDec=!ok;
            ok=false;
            if( nPMRA>=0 )  { f1 =  memoField.elementAt( nPMRA );  ok = f.ref.equals( f1.ref ); }
            flagPmra=!ok;
            flagPmra=true;
            ok=false;
            if( nPMDEC>=0 ) { f1 =  memoField.elementAt( nPMDEC ); ok = f.ref.equals( f1.ref ); }
            flagPmdec=!ok;
            
            // On passe en revue tous les champs, et s'ils ont même ref, on "diminue" de mille la qualité de
            // la détection du rôle (on ne le fait que pour le premier champ trouvé => ON POURRAIT LE FAIRE
            // POUR CELUI QUI A LA MEILLEURE QUALITE PARMI CEUX QUI ONT MEME REF ET ROLE)
            if( flagDec || flagPmra || flagPmdec ) {
               Enumeration<Field> e = memoField.elements();
               for( int i=0; e.hasMoreElements(); i++ ) {
                  f1 = e.nextElement();
                  if( f1.ref!=null && f1.ref.equals( f.ref ) ) {
                          if( flagDec   && qualDEC>=0   && f.cooPossibleSignature==Field.DE )   { nDEC=i;   qualDEC-=1000; } 
                     else if( flagPmra  && qualPMRA>=0  && f.cooPossibleSignature==Field.PMRA ) { nPMRA=i;  qualPMRA-=1000; } 
                     else if( flagPmdec && qualPMDEC>=0 && f.cooPossibleSignature==Field.PMDE ) { nPMDEC=i; qualPMDEC-=1000; } 
                  }
               }
            }
         }
      }

      consumer.setTableRaDecXYIndex(nRA,nDEC,nPMRA,nPMDEC,nX,nY,
            (qualRA==1000 || qualDEC==1000) && (nX==1000 || nY==1000));
      if( flagXY ) consumer.tableParserInfo("   -assuming XY positions (column "+(nX+1)+" for X and "+(nY+1)+" for Y)");
      else if( nRA>=0 ) {
         consumer.tableParserInfo("   -assuming RADEC"+(format==FMT_UNKNOWN?" " : (format==FMT_SEXAGESIMAL?" in sexagesimal": (unit==UNIT_RADIANS ? " in radians":" in degrees")))+
               " (column "+(nRA+1)+" for RA and "+(nDEC+1)+" for DEC)");
         if( nPMRA>=0 ) {
            consumer.tableParserInfo("   -Proper motion fields found"+
                  " (column "+(nPMRA+1)+" for PMRA and "+(nPMDEC+1)+" for PMDEC)");
         }
      }
      consumer.tableParserInfo("      [RA="+nRA+" ("+qualRA+") DE="+nDEC+" ("+qualDEC+") "+
            "PMRA="+nPMRA+" ("+qualPMRA+") PMDEC="+nPMDEC+" ("+qualPMDEC+") "+
            "X="+nX+" ("+qualX+") Y="+nY+" ("+qualY+")]");

      if( coosys!=null && coosys.size()>0 ) {
         consumer.tableParserInfo("   -Coordinate system references found:");
         Enumeration<String> ce = coosys.keys();
         while( ce.hasMoreElements() ) {
            String k = ce.nextElement();
            String epoch   = cooepoch.get(k);
            String equinox = cooequinox.get(k);
            String s = "      ID=\""+k+"\" => "+coosys.get(k)
                  + (equinox==null?"":" Eq="+equinox)
                  + (epoch==null  ?"":" Ep="+epoch);
            consumer.tableParserInfo(s);
         }

         try {
            Field f = memoField.elementAt(nRA);

            // Assignation du système de coord par "ref" classique
            if( f.ref!=null ) setSourceAstroFrame(f.ref,null,null,0);

            // Assignation du système de coord par FIELDref (voir startElement())
            else {
               String coosysID = cooFieldref.get(f.ID);
               if( coosysID!=null ) setSourceAstroFrame(coosysID,null,null,0);
            }
         } catch( Exception e) {

            // Pas de désignation du système, mais un seul système défini => on le prend
            if( coosys.size()==1 ) {
               try { setSourceAstroFrame(coosys.keys().nextElement(),null,null,0); }
               catch( Exception e1 ) {}
            }
            else consumer.tableParserInfo("!!! Coordinate system assignation error... assuming ICRS");
         }
      } else {
         consumer.tableParserInfo("   -No coordinate system reference found... assuming ICRS");
      }
   }

   /** Positionnement du Frame source */
   private void setSourceAstroFrame(String ref,String eq, String ep,int n) throws Exception {

      if( n>4 ) throw new Exception();  // Bouclage, on laisse tomber

      String  sys = coosys.get(ref);
      if( ep==null ) ep=cooepoch.get(ref);
      if( eq==null ) eq=cooequinox.get(ref);
      
      // Gestion du système du calcul de l'année par défaut en fonction du système
      char letter='J';
      if( sys.indexOf("FK4")>=0 || sys.indexOf("B1950")>=0 ) letter='B';
      if( ep!=null && ep.length()>1 && Character.isDigit(ep.charAt(0)) ) ep = letter+ep;
      if( eq!=null && eq.length()>1 && Character.isDigit(eq.charAt(0)) ) eq = letter+eq;

      if( sys.indexOf("FK4")>=0 ) {
         if( eq==null ) srcAstroFrame = AF_FK4;
         else srcAstroFrame = new FK4( (new Astrotime(eq)).getByr() );
         if( ep!=null ) srcAstroFrame.setFrameEpoch((new Astrotime(ep)).getByr());
      }
      else if( sys.indexOf("B1950")>=0 ) srcAstroFrame = AF_FK4;
      else if( sys.indexOf("FK5")>=0 ) {
         if( eq==null ) srcAstroFrame = AF_FK5;
         else srcAstroFrame = new FK5( (new Astrotime(eq)).getJyr() );
         if( ep!=null ) srcAstroFrame.setFrameEpoch((new Astrotime(ep)).getJyr());
      }
      else if( sys.indexOf("J2000")>=0 ) srcAstroFrame = AF_FK5;
      else if( sys.indexOf("ECLIPTIC")>=0 || sys.indexOf("ECL")>=0 ) {
         if( eq==null ) srcAstroFrame = AF_ECLI;
         else srcAstroFrame = new Ecliptic( (new Astrotime(eq)).getJyr() );
         if( ep!=null ) srcAstroFrame.setFrameEpoch((new Astrotime(ep)).getJyr());
      }
      else if( sys.indexOf("SUPER_GALACTIC")>=0 || sys.indexOf("SGAL")>=0 ) srcAstroFrame = AF_SGAL;
      else if( sys.indexOf("GALACTIC")>=0 || sys.indexOf("GAL")>=0 )        srcAstroFrame = AF_GAL;
      else if( sys.indexOf("ICRS")>=0 && ep!=null ) {
         srcAstroFrame = new ICRS((new Astrotime(ep)).getJyr());
      }
      else if( sys.indexOf("ICRS")<0 ) {
         String sref = coosys.get(sys);

         // Peut être une déclaration en plusieurs coups (merci Fox)
         if( sref!=null ) { setSourceAstroFrame(sys,eq,ep,n+1); return; }

         // Perdu
         else consumer.tableParserInfo("      !!! Coordinate system unknown... assuming ICRS");
      }

      // Déjà dans le bon référentiel
      if( (srcAstroFrame+"").equals("ICRS") ) srcAstroFrame=null;

      if( ref==null ) ref="null";

      if( srcAstroFrame!=null ) {

         // Pour le moment, identique à ICRS donc inutile
         if( srcAstroFrame==AF_FK5 || (srcAstroFrame+"").equals("FK5(J2000.0)") ) {
            consumer.tableParserInfo("      => RA/DEC coordinate conversion not required: ref=\""+ref+"\" => "+srcAstroFrame+" to "+trgAstroFrame);
            srcAstroFrame=null;
         
         } else {

            c = new Astropos(srcAstroFrame);
            consumer.tableParserInfo("      => RA/DEC coordinate conversion: ref=\""+ref+"\" => "+srcAstroFrame+" to "+trgAstroFrame);
         }
      } else {
         consumer.tableParserInfo("      => RA/DEC coordinate system used: ref=\""+ref+"\" => "+trgAstroFrame);
      }
   }

   /** Positionne des noms de colonne par défaut pour palier à une absence d'entête */
   private void setDefaultField() {
      tsvField = new Field[nField];
      for( int i=0; i<nField; i++ ) {
         Field f = tsvField[i] = new Field("C"+(i+1));
         f.datatype="D";
         consumer.setField(f);
      }
   }

   /** Parsing des coordonnées exprimée en décimales ou en sexagésimales. Prend en compte l'absence de signe
    * sur la déclinaison
    * @param c  Les coordonnées trouvées
    * @param ra La chaine de l'ascension droite
    * @param dec La chaine de la déclinaison
    * @param format Le format (FMT_UNKNOWN | FMT_DECIMAL | FMT_SEXAGESIMAL)
    * @param unit Le code de l'unité (UNIT_UNKOWN | UNIT_RADIANS | UNTI_DEGRES)
    * @return le format trouvé (si FMT_UNKOWN en entrée)
    * @throws Exception
    */
   static public int getRaDec(Astrocoo c, String ra, String dec, int format,int unit) throws Exception {

      // Détermination du format si non spécifié
      if( format==FMT_UNKNOWN ) {
         try {
            char ss = dec.charAt(0);
            format= isSexa(ra+( ss!='-' && ss!='+' ? " +":" " )+dec) ? FMT_SEXAGESIMAL : FMT_DECIMAL;
         }catch( Exception e ) {
            if( Aladin.levelTrace>3 ) e.printStackTrace();
         }
      }

      // Parsing en sexagésimal
      if( format==FMT_SEXAGESIMAL ) {
         try {
            char ss = dec.charAt(0);
            String s;
            if( ss=='-' || ss=='+' ) s=ra+" "+dec;
            else s=ra+" +"+dec;
            c.set(s); }
         catch( Exception e ) {
            if( Aladin.levelTrace>3 ) e.printStackTrace();
         }

         // Parsing en décimal
      } else {
         try { 
            double rax = Double.parseDouble(ra);
            double dex = Double.parseDouble(dec);

            // Conversion éventuelle d'unités
            if( unit==UNIT_RADIANS ) {
               rax = Math.toDegrees(rax);
               dex = Math.toDegrees(dex);
            }

            c.set( rax, dex);
         } catch( Exception e ) {
            //            if( Aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }

      return format;
   }


   /** Retourne true si la coord. passée en paramètre est du sexagésimal.
    * Test simplement s'il y a plus d'un séparateur de champ */
   static private boolean isSexa(String s) {
      int n = s.length();
      int nbb;		// Nombre de separateurs

      for( int i=nbb=0; i<n; i++ ) {
         char c = s.charAt(i);
         if( c==':' ||c==' ' || c=='\t' ) nbb++;
         if( nbb>1 ) return true;
      }
      return false;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que RA,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int raName(String s) {
      if( s.equalsIgnoreCase("_RAJ2000") ) { setEq(); return 0; }
      if( s.equalsIgnoreCase("RAJ2000") )  { setEq(); return 1; }
      if( s.equalsIgnoreCase("_RA") )      { setEq(); return 2; }
      if( s.equalsIgnoreCase("RA(ICRS)") ) { setEq(); return 3; }
      if( s.equalsIgnoreCase("RA") )       { setEq(); return 4; }
      if( s.equalsIgnoreCase("ALPHA_J2000") ) { setEq(); return 5; }
      if( s.equalsIgnoreCase("GLON") )  { setGal(); return 6; }
      if( s.equalsIgnoreCase("SGLON") ) { setSGal(); return 6; }
      if( s.equalsIgnoreCase("SLON") )  { setSGal(); return 6; }
      if( s.equalsIgnoreCase("ELON") )  { setEcl();  return 6; }
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine RA dans le nom de colonne (-1 sinon) */
   private int raSubName(String s) {
      s = s.toLowerCase();
      if( s.indexOf("radius")>=0 ) { setEq(); return -1; }
      if( s.startsWith("_ra") ) { setEq(); return 0; }
      if( s.startsWith("ra") )   { setEq(); return 1; }
      if( s.startsWith("alpha") ) { setEq(); return 2; }
      if( s.startsWith("GLON") )  { setGal();  return 0; }
      if( s.startsWith("SGLON") ) { setSGal(); return 0; }
      if( s.startsWith("SLON") )  { setSGal(); return 0; }
      if( s.startsWith("ELON") )  { setEcl();  return 0; }
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que DE,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int deName(String s) {
      if( s.equalsIgnoreCase("_DEJ2000") )  { setEq(); return 0; }
      if( s.equalsIgnoreCase("_DECJ2000") ) { setEq(); return 1; }
      if( s.equalsIgnoreCase("DEJ2000") )   { setEq(); return 2; }
      if( s.equalsIgnoreCase("DECJ2000") )  { setEq(); return 3; }
      if( s.equalsIgnoreCase("_DE") )       { setEq(); return 4; }
      if( s.equalsIgnoreCase("_DEC") )      { setEq(); return 5; }
      if( s.equalsIgnoreCase("DE(ICRS)") )  { setEq(); return 6; }
      if( s.equalsIgnoreCase("DEC(ICRS)") ) { setEq(); return 6; }
      if( s.equalsIgnoreCase("DEC") )       { setEq(); return 7; }
      if( s.equalsIgnoreCase("DE") )        { setEq(); return 8; }
      if( s.equalsIgnoreCase("DELTA_J2000") ) { setEq(); return 8; }
      if( s.equalsIgnoreCase("GLAT") )  { setGal();  return 9; }
      if( s.equalsIgnoreCase("SGLAT") ) { setSGal(); return 9; }
      if( s.equalsIgnoreCase("SLAT") )  { setSGal(); return 9; }
      if( s.equalsIgnoreCase("ELAT") )  { setEcl();  return 9; }
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine DE dans le nom de colonne (-1 sinon) */
   private int deSubName(String s) {
      s = s.toLowerCase();
      if( s.startsWith("_dec") ) { setEq(); return 0; }
      if( s.startsWith("_de") )  { setEq(); return 1; }
      if( s.startsWith("dec") )  { setEq(); return 2; }
      if( s.startsWith("de") )   { setEq(); return 3; }
      if( s.indexOf("de")>0 )    { setEq(); return 4; }
      if( s.startsWith("delta") ){ setEq(); return 5; }
      if( s.startsWith("GLAT") )  { setGal();  return 0; }
      if( s.startsWith("SGLAT") ) { setSGal(); return 0; }
      if( s.startsWith("SLAT") )  { setSGal(); return 0; }
      if( s.startsWith("ELAT") )  { setEcl();  return 0; }
      return -1;
   }

   private static final String DEFAULT = "Default";
   private Astroframe lastCoordSys=null;             // Systeme de coordonnées associé au champ courant (avant validation)

   private void validLastCoordSys() {
      srcAstroFrame = lastCoordSys;
      if( srcAstroFrame==AF_GAL )  coosys.put(DEFAULT,"GAL");
      else if( srcAstroFrame==AF_SGAL ) coosys.put(DEFAULT,"SGAL");
      else if( srcAstroFrame==AF_SGAL ) coosys.put(DEFAULT,"ECL");
      else coosys.remove(DEFAULT);
   }

   private void setGal()  { lastCoordSys = AF_GAL;  }
   private void setSGal() { lastCoordSys = AF_SGAL; }
   private void setEcl()  { lastCoordSys = AF_ECLI; }
   private void setEq()   { lastCoordSys = null; }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que PMRA,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int pmraName(String s) {
      if( s.equalsIgnoreCase("PMRA") )  return 0;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine PMRA dans le nom de colonne (-1 sinon) */
   private int pmraSubName(String s) {
      s = s.toLowerCase();
      if( s.startsWith("pmra") ) return 0;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que PMDEC,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int pmdecName(String s) {
      if( s.equalsIgnoreCase("PMDE") )  return 0;
      if( s.equalsIgnoreCase("PMDEC") ) return 1;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine PMDEC dans le nom de colonne (-1 sinon) */
   private int pmdecSubName(String s) {
      s = s.toLowerCase();
      if( s.startsWith("pmde") ) return 0;
      return -1;
   }


   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que X,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int xName(String s) {
      if( s.equalsIgnoreCase("XPOS") ) return 0;
      if( s.equalsIgnoreCase("XPIX") ) return 1;
      if( s.equalsIgnoreCase("X") )    return 2;
      if( s.equalsIgnoreCase("POSX") ) return 3;
      if( s.equalsIgnoreCase("X_IMAGE") ) return 4;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine X dans le nom de colonne (-1 sinon) */
   private int xSubName(String s) {
      if( s.startsWith("XPOS") || s.startsWith("Xpos") ) return 0;
      if( s.startsWith("XPIX") || s.startsWith("Xpix") ) return 1;
      if( s.indexOf("XPOS")>=0 || s.indexOf("Xpos")>=0
            || s.indexOf("XPIX")>=0 || s.indexOf("Xpix")>=0 ) return 2;
      if( (s.startsWith("X") || s.startsWith("x")) && !Character.isLetterOrDigit(s.charAt(1))) return 3;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la reconnaissance
    * ou non du nom d'une colonne en tant que Y,
    * (-1 s'il ne s'agit a priori pas de cela)
    */
   private int yName(String s) {
      if( s.equalsIgnoreCase("YPOS") ) return 0;
      if( s.equalsIgnoreCase("YPIX") ) return 1;
      if( s.equalsIgnoreCase("Y") )    return 2;
      if( s.equalsIgnoreCase("POSY") ) return 3;
      if( s.equalsIgnoreCase("Y_IMAGE") ) return 4;
      return -1;
   }

   /** Retourne un indice entre 0 (meilleur) et 9 en fonction de la présence
    * d'une sous chaine Y dans le nom de colonne (-1 sinon) */
   private int ySubName(String s) {
      if( s.startsWith("YPOS") || s.startsWith("Ypos") ) return 0;
      if( s.startsWith("YPIX") || s.startsWith("Ypix") ) return 1;
      if( s.indexOf("YPOS")>=0 || s.indexOf("Ypos")>=0
            || s.indexOf("YPIX")>=0 || s.indexOf("Ypix")>=0 ) return 2;
      if( (s.startsWith("Y") || s.startsWith("y")) && !Character.isLetterOrDigit(s.charAt(1))) return 3;
      return -1;
   }

   /** Cherche à déterminer si f concerne une mesure pour un point SED en
    * fonction des ID de colonnes repérés lors du parsing de l'entête du VOTable
    * lorsqu'il a rencontré le GROUP qui décrivait le point de SED
    * @param f
    */
   private void detectSEDField(Field f,int nField) {
      String name = f.ID!=null ? f.ID : f.name;
      if( name == null ) return;
      if( sFreq!=null    && name.equals(sFreq)    ) { f.sed=Field.FREQ;    nFreq=nField; }
      else if( sFlux!=null    && name.equals(sFlux)    ) { f.sed=Field.FLUX;    nFlux=nField; }
      else if( sFluxErr!=null && name.equals(sFluxErr) ) { f.sed=Field.FLUXERR; nFluxErr=nField; }
      else if( sSedId!=null   && name.equals(sSedId)   ) { f.sed=Field.SEDID;   nSedId=nField; }
      else return;
   }

   /** Cherche à determiner si f concerne RA,DE,PMRA,PMDEC, X ou Y en mettant à jour
    * les indices de qualité (qualRA, qualDEC, qualPMRA, qualPMDEC, qualX, qualY)
    * et mémorise le numéro du champ si la qualité est meilleure (nRA,nDEC,nPMRA,nPMDEC,nX,nY)
    * La règle empirique est la suivante :
    * Priorité à l'UCD, sinon au nom de colonne + unité, sinon à une portion
    * du nom de colonne + unité, sinon au nom de colonne, sinon à une portion
    * du nom de colonne.
    */
   private void detectPosField(Field f,int nField) {
      String name = f.name==null?"":f.name;
      String ucd =  f.ucd==null?"":f.ucd;
      String unit = f.unit==null?"":f.unit;
      boolean numeric = f.isNumDataType();
      int qual;
      int n;
      //System.out.println("name=["+name+"]");

      // Mémorisation du type FITS de chaque champ en cas de parsing BINAIRE ultérieur
      if( memoField==null ) memoField = new Vector<Field>();
      memoField.addElement( f );


      // Détection du RA et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_EQ_RA_MAIN") || ucd.equals("pos.eq.ra;meta.main") ) qual=0;
      else if( ucd.startsWith("POS_EQ_RA") || ucd.startsWith("pos.eq.ra") ) qual=100;
      else if( (n=raName(name))>=0 ) {
         if( unit.indexOf("h:m:s")>=0 ) qual=200+n;
         else if( unit.indexOf("deg")>=0 ) qual=200+n;
         else if( numeric ) qual=200+n;
         else qual=400+n;
      }
      else if( (n=raSubName(name))>=0 ) {
         if( unit.indexOf("h:m:s")>=0 ) qual=300+n;
         else if( unit.indexOf("deg")>=0 ) qual=300+n;
         else if( numeric ) qual=300+n;
         else qual=450+n;
      }
      if( qual>=0 && qualRA>qual ) {
         nRA=nField; qualRA=qual;
         f.cooPossibleSignature = Field.RA;
         this.unit = getUnit(unit);
         format= unit.length()==0 ? FMT_UNKNOWN : unit.indexOf("h")>=0 && unit.indexOf("m")>=0 && unit.indexOf("s")>=0 ?FMT_SEXAGESIMAL : FMT_DECIMAL;
         validLastCoordSys();
      }

      // Détection du DE et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_EQ_DEC_MAIN") || ucd.equals("pos.eq.dec;meta.main") )qual=0;
      else if( ucd.startsWith("POS_EQ_DEC") || ucd.startsWith("pos.eq.dec") ) qual=100;
      else if( (n=deName(name))>=0 ) {
         if( unit.indexOf("d:m:s")>=0 ) qual=200+n;
         else if( unit.startsWith("deg")) qual=200+n;
         else if( numeric ) qual=200+n;
         else qual=400+n;
      }
      else if( (n=deSubName(name))>=0 ) {
         if( unit.indexOf("d:m:s")>=0 ) qual=300+n;
         else if( unit.startsWith("deg")) qual=300+n;
         else if( numeric ) qual=300+n;
         else qual=450+n;
      }

      // on est juste après la colonne RA - une petite gratification
      if( qual>0 && nRA==nField-1 && nRA>=0 ) qual--;

      if( qual>=0 && qualDEC>qual ) {
         nDEC=nField; qualDEC=qual;
         f.cooPossibleSignature = Field.DE;
      }

      // Détection du PMRA et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_PMRA_MAIN") || ucd.equals("pos.pm;pos.eq.ra;meta.main") ) {
         try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=0; }
         catch( Exception e ) { qual=1; }
      }
      else if( ucd.equals("POS_PMRA") || ucd.equals("pos.pm;pos.eq.ra") ) {
         try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=100; }
         catch( Exception e ) { qual=101; }
      }
      else if( (n=pmraName(name))>=0 ) {
         if( ucd.startsWith("pos.pm") ) qual=200+n;
         else {
            try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=300+n; }
            catch( Exception e ) { qual=600+n; }
         }
      }
      else if( (n=pmraSubName(name))>=0 ) {
         if( ucd.startsWith("pos.pm") ) qual=400+n;
         else {
            try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=500+n; }
            catch( Exception e ) { qual=700+n; }
         }
      }
      if( qual>=0 &&  qualPMRA>qual ) {
         nPMRA=nField; qualPMRA=qual;
         f.cooPossibleSignature = Field.PMRA;
      }

      // Détection du PMDE et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_PMDE_MAIN") || ucd.equals("pos.pm;pos.eq.dec;meta.main") ) qual=0;
      else if( ucd.equals("POS_PMDE") || ucd.equals("pos.pm;pos.eq.dec") ) qual=100;
      else if( (n=pmdecName(name))>=0 ) {
         if( ucd.startsWith("pos.pm") ) qual=200+n;
         try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=300+n; }
         catch( Exception e ) { qual=600+n; }
      }
      else if( (n=pmdecSubName(name))>=0 ) {
         if( ucd.startsWith("pos.pm") ) qual=400+n;
         else {
            try { (new Unit(unit)).convertTo(new Unit("mas/yr")); qual=500+n; }
            catch( Exception e ) { qual=700+n; }
         }
      }
      if( qual>=0 &&  qualPMDEC>qual ) {
         nPMDEC=nField; qualPMDEC=qual;
         f.cooPossibleSignature = Field.PMDE;
      }


      // Détection du X et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_CCD_X") || ucd.equals("pos.cartesian.x;instr.det") ) qual=0;
      else if( ucd.startsWith("pos.cartesian.x") ) qual=100;
      else if( (n=xName(name))>=0 ) {
         if( ucd.startsWith("pos.det") ) qual=200+n;
         else if( unit.equals("pix") || unit.equals("mm") ) qual=300+n;
         else qual=600+n;
      }
      else if( (n=xSubName(name))>=0 ) {
         if( ucd.startsWith("pos.det") ) qual=400+n;
         else if( unit.equals("pix") || unit.equals("mm") ) qual=500+n;
         else qual=700+n;
      }
      if( qual>=0 &&  qualX>qual ) { nX=nField; qualX=qual; }

      // Détection du Y et évaluation de la qualité de cette détection
      qual=-1;
      if( ucd.equals("POS_CCD_Y") || ucd.equals("pos.cartesian.y;instr.det") ) qual=0;
      else if( ucd.startsWith("pos.cartesian.y") ) qual=100;
      else if( (n=yName(name))>=0 ) {
         if( ucd.startsWith("pos.det") ) qual=200+n;
         else if( unit.equals("pix") || unit.equals("mm") ) qual=300+n;
         else qual=600+n;
      }
      else if( (n=ySubName(name))>=0 ) {
         if( ucd.startsWith("pos.det") ) qual=400+n;
         else if( unit.equals("pix") || unit.equals("mm") ) qual=500+n;
         else qual=700+n;
      }
      if( qual>=0 &&  qualY>qual ) { nY=nField; qualY=qual; }
      
      // Cas vraiment particulier d'une colonne unique pour les coordonnées
      if( nRA==-1 && (ucd.equals("pos.eq") ||  ucd.equals("pos.eq;meta.main")) ) {
         nRA=nDEC = nField;
         this.unit = getUnit(unit);
         format= unit.length()==0 ? FMT_UNKNOWN : unit.indexOf("h")>=0 && unit.indexOf("m")>=0 && unit.indexOf("s")>=0 ?FMT_SEXAGESIMAL : FMT_DECIMAL;
         validLastCoordSys();
         qualRA=qualDEC=0;
         return;
      }

   }

   /** XMLparser interface */
   public void endElement (String name) {
      if( inError ) inError=false;
      int depth = xmlparser.getDepth()+1; // faut ajouter 1 car il est déjà décompté

      if( inGroup ) memoEndGroup(name);

      if( name.charAt(0)=='G' && depth>=1 && name.equalsIgnoreCase("GROUP") ) {
         ngroup--;
         inGroup=false;
         return;
      }
      depth -= ngroup;

      if( depth==7 && name.equalsIgnoreCase("TD")  )        {
         inTD=false;
         if( !valueInTD ) record[row]=null;    // Dans le cas d'un champ vide
         valueInTD=false;
         row++;
      } else if( depth==6 && name.equalsIgnoreCase("TR")  )         consumeRecord(record,-1);
      else if( depth==6 && name.equalsIgnoreCase("STREAM") )      inEncode64=false;
      else if( depth==3 && name.equalsIgnoreCase("TABLE") )     {
         if( !flagPosChooser ) posChooser();
         fieldSub=tableSub=null;
         consumer.endTable();
      }
      else if( depth==2 && name.equalsIgnoreCase("RESOURCE") )  {
         fieldSub=tableSub=resourceSub=null;
         consumer.endResource();
      }
      else if( name.equalsIgnoreCase("DESCRIPTION") )             inFieldDesc=false;
      else if( depth==5 && name.equalsIgnoreCase("LINK") )        inLinkField=false;
      else if( depth==5 && name.equalsIgnoreCase("CSV") )         inCSV=false;
      else if( depth==5 && name.equalsIgnoreCase("BINARY") )      inBinary=false;
      else if( depth==5 && name.equalsIgnoreCase("BINARY2") )     inBinary2=false;
      else if( depth==5 && name.equalsIgnoreCase("FITS") )        inFits=false;

      // Ajustement de profonceur en fonction des GROUP
      else if( depth==4 && name.equalsIgnoreCase("FIELD") ) {
         fieldSub=null;
         if( f.name==null ) f.name=f.ID;
         detectPosField(f,nField);
         detectSEDField(f,nField);
         nField++;
         consumer.setField(f);

         // POUR LE MOMENT ON EN FAIT RIEN
      } else if( depth==4 && name.equalsIgnoreCase("PARAM") ) {
      }

      //    System.out.println("endElement: "+name+" depth="+depth);
   }

   /** Retourne vrai si la chaine repérée ne contient que des espaces */
   private boolean isEmpty(char ch[], int start, int length) {
      for( int i=0; i<length; i++ ) if( !Character.isSpace(ch[i+start]) ) return false;
      return true;
   }

   /** XMLparser interface */
   public void characters(char ch[], int start, int length) throws Exception {

      try {
         if( inGroup ) { memoInGroup(ch,start,length); return; }

         // Cas d'un segment BASE64
         if( inEncode64 ) { parseBase64(ch,start,length,inBinary2); return; }

         // Ecarte les portions de texte vides
         if( isEmpty(ch,start,length) ) return;

//         System.out.println("characters : ["+(getStringTrim(ch,start,length))+"]");

         if( inLinkField ) {
            if( f.refText==null ) f.addInfo("refText",getStringTrim(ch,start,length));
         }
         else if( inFieldDesc ) {
            if( f!=null ) f.addInfo("DESCRIPTION",getStringTrim(ch,start,length));
         }
         else if( inCSV || flagTSV )  dataParse(ch,start,length);
         else if( inTD )              { record[row] = getStringTrim(ch,start,length); valueInTD=length>0; }
         else if( inError )           error = getStringTrim(ch,start,length);
         else if( f!=null && fieldSub!=null )    f.addInfo(fieldSub,getStringTrim(ch,start,length));
         else if( filter!=null ) consumeFilter(getStringTrim(ch,start,length));
         else if( tableSub!=null )
            consumer.setTableInfo(tableSub,getStringTrim(ch,start,length));
         else if( resourceSub!=null ) consumer.setResourceInfo(resourceSub,getStringTrim(ch,start,length));
      } catch( Exception e ) {
         if( Aladin.levelTrace==4 ) System.err.println("TableParser.character() exception: table line "+xmlparser.getCurrentLine());
         throw e;
      }
   }

   /** Préparation et appel au consumer.setFilter(...) afin de lui passer la description
    * d'un filtre dédié (utilise les variables de classes filter (lID du filter) et filterName
    */
   private void consumeFilter(String filterRule) {
      consumer.setFilter(filter+filterRule+ (filter.length()>0 ? "\n}\n" : "") );
      filter=null;
   }
   
   /** Retourne le code de l'unité correspondant à la chaine passée en paramètre */
   static public int getUnit(String s) {
      if( s==null ) return UNIT_UNKNOWN;
      if( Util.indexOfIgnoreCase(s, "RAD")>=0 ) return UNIT_RADIANS;
      if( Util.indexOfIgnoreCase(s, "DEG")>=0 ) return UNIT_DEGREES;
      return UNIT_UNKNOWN;
   }

   /** Préparation et appel au consumer.setRecord(alpha,delta,rec[]) en calculant
    * les coordonnées en fonction des valeurs nRA,nDEC ou nX,nY suivant le flagXY ou non
    */
   private void consumeRecord(String rec[],int nbRecord) {

      try {

         // Coordonnées en XY
         if( flagXY ) {
            double x = Double.parseDouble(rec[nX]);
            double y = Double.parseDouble(rec[nY]);
            consumer.setRecord(x,y, rec);

            // Coordonnées en RA/DEC
         } else {
            String ra,dec;

            // Champs différents
            if( nRA!=nDEC ) { ra=rec[nRA]; dec=rec[nDEC]; }

            // Même champ => on va devoir découper
            else {
               String s = rec[nRA];
               int i = s.indexOf('+');
               int j=i;
               if( i<0 ) i=j =s.indexOf('-');
               if( i<0 ) { i=j = s.indexOf(','); if( i>0 ) i++; }
               if( i<0 ) i=j = s.indexOf(' ');
               if( i<0 ) throw new Exception("Unsupported syntax for coordinates expressed as an unique field");
               ra = s.substring(0,j).trim();
               dec= s.substring(i).trim();
            }
            
            format = getRaDec(c,ra,dec,format,unit);

            //System.out.println("--> ["+t+"] knowFormat="+knowFormat+" flagSexa="+flagSexa);
            //System.out.println("rec=");
            //for( int w=0; w<rec.length; w++ ) System.out.println("   rec["+w+"]="+rec[w]);
            
            // Changement de repere si nécessaire
            if( srcAstroFrame!=null ) {
               //               System.out.println("BEFORE c="+c);
               c.convertTo(trgAstroFrame);
               //               System.out.println("AFTER c="+c);
               consumer.setRecord(c.getLon(),c.getLat(), rec);
               c = new Astropos(srcAstroFrame);

            } else consumer.setRecord(c.getLon(),c.getLat(), rec);
         }


      } catch( Exception e1 ) {
         System.err.println("Table parser error "+ (nbRecord!=-1 ? "(record "+(nbRecord+1)+")":"") +": "+e1);
         e1.printStackTrace();
      }

   }

   /**
    * Retourne le caractère séparateur s'il s'agit de l'un des séparateurs spécifiés
    * dans le tableau, sinon 0 */
   static final private char isColSep(char c,char cs[]) {
      for( int i=0; i<cs.length; i++ ) if( c==cs[i] ) return c;
      return 0;
   }

   /** Décompte le nombre de colonnes d'une ligne
    * @param s La ligne à tester
    * @param cs liste des séparateurs de colonnes autorisés
    * @return le nombre de colonnes trouvées
    */
   static public int countColumn(String s,char cs[]) {
      char ch[] = s.toCharArray();
      int cur=0;
      int end=ch.length;
      char sep=0;
      int ncol=0;

      while( cur<end ) {
         while( cur<end && (sep=isColSep(ch[cur],cs))==0 ) cur++;
         ncol++;
         if( sep==' ' ) while( cur<end && ch[cur]==' ' ) cur++;
         else cur++;
      }

      return ncol;
   }

   /** Dans le mode CSV, extraction du champ courant et mémorisation dans record[row]
    * ou dans vRecord si non encore alloué
    * @param ch,cur,end  désignation du champ
    * @param rs séparatateur de lignes
    * @param cs séparateur de champ
    * @return retourne l'indice du prochain caractère après le champ
    */
   private int getField(char [] ch, int cur, int end, char rs,char cs[],int nbRecord)
         throws Exception {
      int start=cur;
      char sep=0;   // le séparateur effectivement utilisé
      boolean excelCSV = cs.length>0 && cs[0]==',';

      //      System.out.println("getField ["+getStringTrim(ch,start,end-start)+"]");

      // Cas particulier du CSV mode Excel "xxx","yyyy",zzzz
      if( excelCSV ) {
         boolean inQuote=false;
         for( ; cur<end; cur++ ) {
            if( ch[cur]=='"' ) inQuote=!inQuote;
            if( !inQuote && !( (sep=isColSep(ch[cur],cs))==0 && ch[cur]!=rs ) ) break;
         }
         if( inQuote ) throw new Exception("Bad CSV: Excel quote delimiters not balanced (record "+(nbRecord+1)+" field["+row+"]=["+getStringTrim(ch,start,cur-start)+"])");


         // Mode TSV, CSV courant
      } else {
         while( cur<end && (sep=isColSep(ch[cur],cs))==0 && ch[cur]!=rs ) cur++;
      }
      
      // Dans le cas d'oubli du caractère de fin de ligne sur la dernière ligne
      int plusUn = 0;
      if( cur==end && cur>0 && ch[cur-1]!=rs ) plusUn=1;

      String value;
      if( excelCSV ) {
         if( cur-start>1 && ch[start]=='"' && ch[cur-1]=='"' ) value = getStringTrim(ch,start+1,cur-start-2);
         else value = getStringTrim(ch,start,cur-start+plusUn);
      } else {

         // Dans le cas d'un séparateur espace, on va shifter tous les blancs suivants
         if( sep==' ' ) {
            cur++;
            while( cur<end && ch[cur]==' ' && ch[cur]!=rs ) cur++;
            cur--;
         }
         value = getStringTrim(ch,start,cur-start+plusUn);
      }

      // s'il y a un séparateur avant la première valeur, on doit simplement l'ignorer
      if( sep==' ' && row==0 && value.length()==0 ) return cur;

      if( record!=null ) {
         if( row>=record.length ) {
            String s = "Not aligned CSV catalog (record="+(nbRecord+1)+" extra row value=\""+value+"\") => ignored\n";
            aladin.command.printConsole(s);
         } else record[row]=value;
         
      } else {
         if( vRecord==null ) vRecord = new Vector<String>();
         vRecord.addElement(value);
      }
      row++;

      return cur;
   }

   /** Dans le mode CSV, extraction de l'enregistrement courant et mémorisation
    * dans record[] ou dans vRecord si non encore alloué
    * In one CSV line (described by ch[], cur and end, recsep and colsep)
    * @param ch,cur,end  désignation du champ
    * @param rs séparatateur de lignes
    * @param cs séparateur de champ
    * @return retourne l'indice du prochain caractère après le champ
    */
   private int getRecord(char [] ch, int cur, int end,
         char rs,char cs[],int nbRecord) throws Exception {
      row=0;	// recale à 0 (début d'enr)

      // Extraction de chaque champ
      int un=0;
      while( cur<end && ch[cur]!=rs ) { 
         cur=getField(ch,cur+un,end,rs,cs,nbRecord); 
         un=1; 
      }
      
      // Dans le cas d'oubli du caractère de retour à la ligne sur la dernière ligne, et qu'en
      // plus le dernier champ est vide
      if( record!=null && row==record.length-1 && cur==end && cur>0 && ch[cur-1]!=rs ) {
         System.out.println("dernier champ vide avec absence de retour à la ligne");
         record[row++]="";
      }
      
      if( record!=null && row<record.length &&  !(row==1 && record[0].equals("[EOD]")) ) {
         String s = "Not aligned CSV catalog (record="+(nbRecord+1)+" missing rows nbRow="+row+"/"+record.length+") => ignored"
               + (filename!=null?filename:"");
         aladin.command.printConsole(s);
      }
      return cur;
   }


   /** Dans le mode CSV, saute le séparateur de fin d'enregistrement. Prend en compte
    * la présence éventuelle d'un \r (plate-forme Windows/DOS)
    * @param ch,cur  désignation de l'emplacement courant
    * @param rs le caractère de séparation des enregistrements
    * @return retourne l'indice du prochain caractère après le champ
    */
   static protected int skipRecSep(char ch[],int cur,char rs) {
      if( ch[cur]==rs ) {
         if( rs=='\n' && cur<ch.length-1 && ch[cur+1]=='\r' ) cur+=2;
         else cur++;
      }
      return cur;
   }

   /** Dans le mode CSV, saute l'enregistrement courant.
    * @param ch,cur,end  désignation de l'emplacement courant
    * @param rs le caractère de séparation des enregistrements
    * @return retourne l'indice du prochain caractère après le champ
    */
   static protected int skipRec(char ch[],int cur,char rs) {
      while( ch[cur]!=rs && cur<ch.length-1 ) cur++;
      return skipRecSep(ch,cur,rs);
   }

   /** Dans le mode CSV, retourne true si c'est une ligne vide ou un commentaire
    * @param ch,cur,end  désignation de l'emplacement courant
    * @param rs le caractère de séparation des enregistrements
    */
   private boolean vide(char ch[],int cur, int end,char rs) {
      if( ch[cur]=='#' ) return true;	//commentaire
      while( cur<end && (ch[cur]==' ' || ch[cur]=='\t' || ch[cur]=='\r') ) cur++;
      return ch[cur]==rs;
   }

   /** Dans le mode CSV, retourne true si c'est une ligne de tirets (sans blancs
    * @param ch,cur,end  désignation de l'emplacement courant
    * @param rs le caractère de séparation des enregistrements
    */
   private boolean isSimpleDahsLine(char ch[],int cur, int end,char rs) {
      while( cur<end && ch[cur]=='-' ) cur++;
      return ch[cur]==rs;
   }

   private String colSepInfo(char cs[] ) {
      String s=null;
      for( int i=0; i<cs.length; i++ ) {
         String s1;
         switch( cs[i] ) {
            case '\t' : s1="Tab"; break;
            case '|'  : s1="Pipe (|)"; break;
            case ';'  : s1="Semi-column (;)"; break;
            case ','  : s1="Comma (,)"; break;
            case ' '  : s1="One or several spaces"; break;
            default:    s1=cs[i]+"";
         }
         if( i==0 ) s=s1;
         else s=s+", "+s1;
      }
      return s;
   }


   /** Dans le mode CSV, parsing des données passées en paramètre. Cette méthode
    * peut être appelé plusieurs fois de suite pour un même tableau CSV
    * @param ch,start,length désignation de la chaine à perser
    * @throws Exception
    */
   private void dataParse(char ch[], int start, int length) throws Exception {
      char rs = '\n';		  // Séparateur d'enregistrements par défaut
      char cs[] = "\t".toCharArray();	  // Séparateurs de champs par défaut
      int h;			      // Nombre de lignes d'entête
      int cur = start;		  // Position du caractère courant
      int end = start+length; // Position du dernier caractère
      String s;			      // Buffer
      int i,j;
      int n=0;			      // Compteur
      int nbRecord=0;         // Nombre d'enregsitrements trouvés
      //      boolean flagPos=false; // true si on a parsé dans une entête CSV


      // Autres dséparateurs d'enregistrements/champs ?
      if( recsep!=null ) rs=recsep.charAt(0);
      if( colsep!=null ) cs=colsep.toCharArray();

      // Traitement de l'entete uniquement s'il ne s'agit pas d'une
      // continuation d'un bloc CSV
      h = (headlines==null)?0:Integer.parseInt(headlines);
      headlines=null;	// permet de ne pas reprocesser l'entete

      //System.out.println("dataParse: ["+(new String(ch,start,end))+"]");

      // Traitement de l'entête CSV
      n=0;			// Numéro de ligne courante (effectives)
      boolean flagHeader=false;   // true si on se trouve dans l'entête TSV
      boolean dashLine=false;     // true si on a trouvé une dashline
      boolean flagDejaLu=false;   // true si on cherchant l'entete TSV on a déjà lu le premier enregistrement

      if( flagNewTable ) {
         if( flagTSV ) consumer.tableParserInfo("CSV format ["+colSepInfo(cs)+"]");
         else {
            consumer.tableParserInfo("   -found CSV DATA (field sep="+colSepInfo(cs)
                  +" record sep="+(rs=='\n'?"\\n":"["+(byte)rs+"]")
                  +")");
            if( h==0 ) consumer.tableParserInfo("   -No CSV header");
         }
         flagNewTable=false;
      }

      for( n=0; cur<end && (flagTSV || n<h); n++ ) {

         // Saute les commentaires et les lignes vides
         if( vide(ch,cur,end,rs)
               || (n==1 && isSimpleDahsLine( ch,cur,end,rs))) {
            //System.out.println("on saute ["+(new String(ch,cur,12)).replace('\n',' ')+"...] ("+(end-cur)+" car.)");
            n--;
            cur=skipRec(ch,cur,rs);
            continue;
         }

         // Analyse de la ligne
         //         int curOld=cur;    // Juste pour se rappeler où on était.
         cur = getRecord(ch,cur,end,rs,cs,nbRecord);

         // Dans le cas DU TSV natif, ce n'est qu'à ce moment là
         // qu'on connait le nombre de champs. On va donc allouer record[]
         // en fonction et recopier le contenu de vRecord.
         if( record==null ) {
            nField = vRecord.size();
            consumer.tableParserInfo("   -found CSV DATA ("+nField+" fields)");
            record = new String[nField];
            Enumeration<String> e = vRecord.elements();
            for( i=0; i<nField; i++ ) record[i] = e.nextElement();
         }

         //for( i=0; i<nField; i++ ) System.out.print("["+record[i]+"] ");
         //System.out.println();

         // Détecteur de la ligne de ---- ----
         for( dashLine=true, i=0; i<nField && dashLine; i++ ) {
            char a[] = record[i].toCharArray();
            for( j=0; j<a.length && a[j]=='-'; j++);
            if( j<a.length ) { dashLine=false; break; }
         }
         if( dashLine ) {
            consumer.tableParserInfo("   -Found "+(n+1)+" lines CVS header with dash separator");
            cur=skipRecSep(ch,cur,rs);
            break;
         }

         // Après cela, traitement particulier de l'entête TSV
         if( !flagTSV ) continue;

         // Peut être n'y a-t-il pas du tout d'entête CSV. (1)
         // Ou une entete d'une seule ligne (2)
         // On va tester si les deux premiers champs (cas 1) ou les
         // champs repérés de position (cas 2) sont uniquement numériques...
         if( n==0
               || (n==1 && (qualRA<500 && qualDEC<500 || qualX<700 && qualY<700)) ) {
            int p=0,r=1;   // cas 1
            if( n==1 ) {   // cas 2
               if( qualRA<500 ) { p=nRA; r=nDEC; }
               else { p=nX; r=nY; }
            }
            flagHeader=false;	// Supposons qu'on est dans la 1ère ligne de données
            for( i=0; i<2; i++ ) {
               int k = i==0 ? p : r;
               //System.out.println("Test si champ numero "+k+" de la ligne "+n+" est numérique ?");
               if( k>=nField ) { flagHeader=true; break; }  // dans les choux
               char a[] = record[k].toCharArray();
               for( j=0; j<a.length; j++ ) {
                  char q = a[j];
                  if( (q<'0' || q>'9') && q!='.' && q!='+' && q!='e' && q!='E'
                        && q!='-' && q!=':' && q!=' ' ) break;
               }
               if( j==0 || j<a.length ) { flagHeader=true; break; }
            }
            if( !flagHeader ) {
               flagDejaLu=true;
               if( n==0 ) {
                  consumer.tableParserInfo("   -No CSV header found!");
                  setDefaultField();
               } else consumer.tableParserInfo("   -Found one line CVS header");
               posChooser();
               break;
            }
         }

         // Détermination des colonnes de position RA,DEC ou X,Y
         // en fonction de l'entête CSV
         if( flagHeader && n==0 && (nRA<0 || flagTSV) ) {
            tsvField = new Field[nField];
            for( i=0; i<nField; i++ ) {
               Field f = tsvField[i] = new Field(record[i]/*.trim()*/);
               f.datatype="D";
               detectPosField(f,i);
               if( flagTSV ) { consumer.setField(f); } // TSV natif
            }
            posChooser();
         }

         cur=skipRecSep(ch,cur,rs);
      }

      // Traitement des données après entête CSV
      while( cur<end || flagDejaLu  ) {
         if( !flagDejaLu ) {
            if( vide(ch,cur,end,rs) ) {
               //System.out.println("Je saute une ligne vide");
               cur=skipRec(ch,cur,rs);
               continue;
            }

            cur = getRecord(ch,cur,end,rs,cs,nbRecord);
         } else flagDejaLu=false;	// Pour permettre la prochaine lecture

         //System.out.println("Ligne "+nbRecord+" row="+row);
         nbRecord++;

         // Cas SkyCat
         if( row==1 && record[0].equals("[EOD]") ) {
            consumer.tableParserInfo("   -Stop parsing at SkyCat [EOD] tag");
            cur=end;
            break;
         }

         // Bourrage si champ manquant en fin de ligne
         while( row<nField ) record[row++]="???";

         // Vérification "manuelle" a posteriori du datatype (par défaut double)
         if( tsvField!=null ) {
            for( i=0; i<tsvField.length; i++ ) {
               if( tsvField[i].datatype!=null && tsvField[i].datatype.equals("D") ) {
                  try {
                     String s1=record[i].trim();
                     if( s1.length()>0 && !s1.equals("-") && !s1.equalsIgnoreCase("null")  ) Double.parseDouble(s1);
                  } catch( Exception e ) {
                     tsvField[i].datatype=null;
                  }
               }
            }
         }

         consumeRecord(record,nbRecord);
         cur=skipRecSep(ch,cur,rs);
      }

      // Traitement d'un message d'erreur
      if( nbRecord==0 ) {
         s = getStringTrim(ch,start,length>200?200:length);
         throw new Exception("Data parsing error:\n \n["+s+"...]");
      }
   }

}
