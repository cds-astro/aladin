// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.fits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import cds.aladin.MyInputStream;
import cds.image.Hdecomp;
import cds.tools.Util;

/**
 * Utilitaires FITS dont notamment la décompression RICE,GZIP,etc...
 * @version 1.0 - Janvier 2020 - suite à une réorganisation du code (PlanImageFits, MyInputStreamCached)
 * @author Pierre Fernique
 *
 */
public class UtilFits {
   
   static final int NOCOMPRESS = 0;
   static final int RICE1      = 1;
   static final int RICEONE    = 2;
   static final int GZIP1      = 3;
   static final int GZIP2      = 4;
   static final int HCOMPRESS1 = 5;
   static final int PLIO1      = 6;
   
   // Liste des différents modes de compressions supportés par le standard FITS 4.0
   static final String [] ZCMPTYPE = { "NOCOMPRESS","RICE_1","RICE_ONE","GZIP_1","GZIP_2","HCOMPRESS_1","PLIO_1" };
   
   // Liste des mots clés FITS à ignorer pour la génération d'une entête FITS standard
   static private String [] KEYIGNORE = { "TFIELDS","TFIELDS","TTYPE1","TFORM1",
      "ZIMAGE","ZTILE1","ZTILE2","ZCMPTYPE","ZNAME1","ZVAL1","ZNAME2","ZVAL2","ZSIMPLE","ZBITPIX",
      "ZNAXIS","ZNAXIS1","ZNAXIS2","ZEXTEND","ZPCOUNT","ZGCOUNT","ZTENSION" };

   // Liste des mots clés supplémentaires à ignorer dans le cas d'une compression Lupton (Pan-STARRs)
   static private String [] KEYIGNORELUPTON = { "BZERO","BSCALE","BLANK","BOFFSET","BSOFTEN" };
   
   /** Décompression d'une image dans un "flux" FITS. ne traite que le HDU courant
    * @param outHeader entête FITS de sortie correspondant à la matrice de pixels retournée (ou à la table s'il ne s'agit pas d'une image)
    * @param inHeader entête FITS , éventuellement déjà créée et/ou lue
    * @param dis le flux d'entrée
    * @return la matrice des pixels décompressés (s'il ne s'agit pas d'une image, contenu de la table et du tas)
    * @throws Exception
    */
   static public byte [] uncompress( HeaderFits headerFits, MyInputStream dis) throws Exception {
      return uncompress(null,headerFits,dis,false);
   }
   
   static public byte [] uncompress( HeaderFits outHeader, HeaderFits inHeader, MyInputStream dis, boolean flagSkip) throws Exception {
       
       int taille;       // nombre d'octets a lire
       int n;            // nombre d'octets pour un pixel
       int bitpix, naxis1,naxis2,npix;

       // Creation et/ou Lecture de l'entete Fits si pas déjà fait
       if( inHeader==null ) inHeader=new HeaderFits(dis);
       else if( inHeader.isEmpty() ) inHeader.readHeader(dis);
       
       // Détermination de la taille du tas et de son emplacement
       int nnaxis1 = inHeader.getIntFromHeader("NAXIS1");
       int nnaxis2 = inHeader.getIntFromHeader("NAXIS2");
       int theap=nnaxis1*nnaxis2;
       try  { theap = inHeader.getIntFromHeader("THEAP"); } catch( Exception e ) {}
       int pcount=inHeader.getIntFromHeader("PCOUNT");    // nombres d'octets a lire en tout
       
       // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
       if( flagSkip ) {
          dis.skip( theap+pcount );
          return new byte [0];
       }

       // Proriétés de l'image finale
       try { bitpix = inHeader.getIntFromHeader("ZBITPIX"); }
       catch( Exception e ) {
//          throw new Exception("Not a FITS image compressed HDU (missing ZBITPIX)");
            System.err.println("Not a compressed image in this HDU => return as is");
            if( outHeader!=null ) inHeader.copyTo( outHeader );
            byte [] buf = new byte [theap + pcount];
            dis.readFully(buf);
            return buf;
       }
       naxis1 = inHeader.getIntFromHeader("ZNAXIS1");
       naxis2 = inHeader.getIntFromHeader("ZNAXIS2");
       npix = n = Math.abs(bitpix)/8;    // Nombre d'octets par pixel
       taille = naxis1*naxis2*n;            // Taille de la matrice final en octets
       
       // Détermination de la méthode de compression
       String sCmp = inHeader.getStringFromHeader("ZCMPTYPE");
       int nCmp = Util.indexInArrayOf(sCmp, ZCMPTYPE);
       if( nCmp<0 ) throw new Exception("Unknown FITS compression method ["+sCmp+"]");
       if( nCmp==PLIO1 ) throw new Exception("Unsupported FITS compression method ["+sCmp+"]");
       if( nCmp==HCOMPRESS1 ) throw new Exception("Unsupported FITS compression method ["+sCmp+"]");

       // Caractéristiques des tuiles (par défaut une ligne)
       int tile1 = naxis1;
       try { tile1 = inHeader.getIntFromHeader("ZTILE1"); } catch( Exception e ) {}
       int tile2 = 1;
       try { tile2 = inHeader.getIntFromHeader("ZTILE2"); } catch( Exception e ) {}
       
       // Eventuels paramètres spécifique à un mode de compression particulier
       int val1=32;   // nblock pour RICE
       try { val1 = inHeader.getIntFromHeader("ZVAL1"); } catch( Exception e ) {}
       int val2=4;    // bsize pour RICE
       try { val2 = inHeader.getIntFromHeader("ZVAL2"); } catch( Exception e ) {}

       // Paramètre de quantification ?
       try {
          String quantiz  = inHeader.getStringFromHeader("ZQUANTIZ");
//          String zdither0 = inHeader.getStringFromHeader("ZDITHER0");
//          if( quantiz!=null && !quantiz.equals("NO_DITHER") ) {
//             System.err.println(sCmp+" FITS image problem (unsupported ZQUANTIZ ["+quantiz+"] => assuming NO_DITHER)");
//          }
       } catch( Exception e ) {}

       int posCompress=0;            // Position du champ du segment compressé
       int posgzipCompress=-1;       // Position du champ du segment s'il est directement GZIPé
       int posUncompress=-1;         // Position du champ du segment s'il est non compressé (non standard)
       int posZscale=-1;             // Position du champ du gacteur d'échelle
       int posZzero=-1;              // Position du champ d'origine pour l'application d'un facteur d'échelle

       // Parcours des champs de la table binaire
       char sForm = ' ';
       int tfields = inHeader.getIntFromHeader("TFIELDS");
       for( int i=1,pos=0; i<=tfields; i++ ) {
          String type = inHeader.getStringFromHeader("TTYPE"+i);
               if( type.equals("COMPRESSED_DATA") )        { posCompress = pos;     sForm='B'; }
          else if( type.equals("GZIP_COMPRESSED_DATA") )   { posgzipCompress = pos; sForm='B'; }
          else if( type.equals("UNCOMPRESSED_DATA") )      { posUncompress = pos;   sForm='B'; }
          else if( type.equals("ZSCALE") )                 { posZscale = pos;       sForm='D'; }
          else if( type.equals("ZZERO") )                  { posZzero = pos;        sForm='D'; }
               
          String form = inHeader.getStringFromHeader("TFORM"+i);
          pos+=Util.binSizeOf(form);
          
          // Vérification du type de données de chaque champ
//          if( form.indexOf(sForm)<0 ) throw new Exception(sCmp+" FITS image problem ("
//                                                +type+" fields with data type ["+form+"] not supported yet)");
       }
       
       if( posUncompress>=0 ) System.err.println(sCmp+" FITS image warning (deprecated UNCOMPRESSED_DATA field)");

       // Allocation de l'image finale
       byte [] pixelsOrigin = new byte[taille];
       
       // Allocation de la table des champs et du tas
       byte [] table = new byte[nnaxis1*nnaxis2];
       byte [] heap = new byte[pcount];

       try {
          // Lecture totale de la table et du tas
          // (ON POURRAIT FAIRE PLUS MALIN POUR EVITER D'ALLOUER LA TOTALITE DE LA MEMOIRE -> SI BESOIN)
          dis.readFully(table);
          dis.skip(theap - nnaxis1*nnaxis2);
          dis.readFully(heap);
          
          // Combien de tuiles dans la largeur ?
          int nbTileInWidth = naxis1/tile1;
          if( naxis1%tile1!=0 )  nbTileInWidth++;

          int pixPos=0;   // indice du pixel courant dans la matrice de pixel finale
          
          // Itération de traitement sur chaque tuile
          for( int numTile=0; numTile<nnaxis2; numTile++ ) {
             int offsetRec = numTile*nnaxis1;
             int size = getInt(table,offsetRec+posCompress);    // Taille du champ variable (supposé en Bytes)
             int pos =  getInt(table,offsetRec+posCompress+4);  // position du champ variable
             
             // Facteurs d'échelle propre à chaque tuile
             double bzero  = posZzero<0  ? 0 : getDouble(table,offsetRec+posZzero);  // Paramètre origine fact d'échelle (supposé en double)
             double bscale = posZscale<0 ? 1 : getDouble(table,offsetRec+posZscale); // Paramètre fact d'échelle (supposé en double)

             // Calcul de la position de la tuile dans la matrice finale de pixels
             // On suppose que les tuiles sont rangées de gauche à droite et de haut en bas
             // (pas d'écrit dans le standards FITS mais semblerait logique)
             int nbRowOfTiles       = numTile/nbTileInWidth;  // nombre de rangés de tuiles déjà traitées
             int nbTileInCurrentRow = numTile%nbTileInWidth;  // nombre de tuiles déjà traitées dans la ligne 
             pixPos = nbRowOfTiles * tile2 * naxis1 + nbTileInCurrentRow*tile1;

             // Directement compress GZIP
             if( size==0 && posgzipCompress>=0 ) {
                size = getInt(table,offsetRec+posgzipCompress);
                pos  = getInt(table,offsetRec+posgzipCompress+4);
                byte [] tile = gunzip( heap, pos, size);
                copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
             
             // Non compressé (méthode désormais obsolète mais que je laisse au cas où)
             } else if( size==0 && posUncompress>=0 ) {
                size = getInt(table,offsetRec+posUncompress);
                pos  = getInt(table,offsetRec+posUncompress+4);
                
                byte [] tile = new byte[ size ];
                System.arraycopy(heap, pos, tile, 0, size);
                copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
                
             // Compressé suivant méthode indiquée
             } else {
                
                // RICE
                if( nCmp==RICE1 || nCmp==RICEONE)  {
                
                   // Ligne par ligne ? => on peut écrire directement dans la matrice de pixels
                   if( tile1==naxis1 && tile2==1 ) {
                      decompRice(heap,pos,pixelsOrigin,pixPos,tile1*tile2,val1,val2,bitpix,bzero,bscale);
                      
                   // Sinon il faut passer par un tableau intermédiaire puis, dans un deuxième
                   // temps, placer la tuile au bon endroit
                   } else {
                      byte [] tile = new byte [ tile1*tile2*npix ];
                      decompRice( heap,pos,tile,0,tile1*tile2,val1,val2,bitpix,0,1);
                      copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
                   }
                   
                // GZIP1
                } else if( nCmp==GZIP1 ) {
                   byte [] tile = decompGzip1( heap, pos, size);
                   copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);

                // GZIP2
                } else if( nCmp==GZIP2 ) {
                   byte [] tile = decompGzip2( heap, pos, size, bitpix);
                   copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);

                // HCOMP
                } else if( nCmp==HCOMPRESS1 ) {
                   byte [] tile = decompHcomp( heap, pos, size, bitpix);
                   copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);

                // NOCOMPRESS
                } else if( nCmp==NOCOMPRESS ) {
                   byte [] tile = new byte [ tile1*tile2*npix ];
                   System.arraycopy(heap, pos, tile, 0, tile.length);
                   copyTile( tile,tile1,tile2, pixelsOrigin,pixPos,naxis1,naxis2, bitpix,bzero,bscale);
                }
             }
          }
       } catch (Exception e ) { e.printStackTrace(); }
       
       // Lupton ? =>  On décode en float 32 bits
       boolean flagLupton=false;
       if( inHeader.getStringFromHeader("BSOFTEN")!=null ) {
          try {
             double boffset = inHeader.getDoubleFromHeader("BOFFSET");
             double bsoften = inHeader.getDoubleFromHeader("BSOFTEN");
             double bzero = 0.;
             double bscale = 1.;
             double blank2 = Double.NaN;
             try{ bzero  = inHeader.getDoubleFromHeader("BZERO");  } catch( Exception e ) {};
             try{ bscale = inHeader.getDoubleFromHeader("BSCALE"); } catch( Exception e ) {};
             try{ blank2 = inHeader.getDoubleFromHeader("BLANK");  } catch( Exception e ) {};
             int bitpix2 = -32;              // On décompresse en float
             int npix2 = Math.abs(bitpix2)/8;
             int taille2 = naxis1*naxis2*npix2;
             
             // On travaille sur le même buffer, ou sur un autre ?
             byte pix2[] = taille==taille2 ? pixelsOrigin : new byte[ taille2 ];
             
             for( int y=0; y<naxis2; y++ ) {
                for( int x=0; x<naxis1; x++ ) {
                   int pos = y*naxis1 +x;
                   double val = getPixVal1(pixelsOrigin, bitpix, pos );
                   val = Double.isNaN(val) || val==blank2 ? Double.NaN 
                         : uncompressLupton(val,bzero,bscale,bsoften,boffset);
                   setPixVal( pix2, bitpix2, pos, val);
                }
             }
             pixelsOrigin = pix2;
             bitpix = bitpix2;
             flagLupton=true;
          } catch( Exception e ) { throw new Exception("Lupton uncompress error"); }
       }
       
       
       // Génération de l'entête de sortie si demandé
       if( outHeader!=null ) {
          
          Hashtable<String,String> map = inHeader.getHashHeader();
          Enumeration<String> e = inHeader.getKeys();
          while( e.hasMoreElements() ) {
             String key = e.nextElement();
             if( Util.indexInArrayOf(key, KEYIGNORE)>=0 ) continue;
             if( flagLupton && Util.indexInArrayOf(key, KEYIGNORELUPTON)>=0 ) continue;
             
             String  val;
                  if( key.equals("XTENSION") ) val="IMAGE";
             else if( key.equals("BITPIX") )   val=bitpix+"";
             else if( key.equals("NAXIS1") )   val=naxis1+"";
             else if( key.equals("NAXIS2") )   val=naxis2+"";
             else if( key.equals("NAXIS") )    val="2";
             else if( key.equals("PCOUNT") )   val="0";
             else if( key.equals("GCOUNT") )   val="1";
             else val = map.get(key);
             
             outHeader.setKeyValue(key, val);
          }
       }
       return pixelsOrigin;
   }
   
   /** Décompression Lupton -> cf https://arxiv.org/pdf/1612.05245.pdf (page 18) */
   static final double ALPHA = 0.4* Math.log(10);
   static public double uncompressLupton(double pixComp, 
         double bzero, double bscale,double bsoften, double boffset) {
      pixComp = bzero + bscale * pixComp;
      double ca = pixComp / ALPHA;
      return boffset + bsoften * (Math.exp(ca) - Math.exp(-ca));
   }

   // Décompression d'une tuile codée en GZIP1
   private static byte [] decompGzip1( byte buf[], int pos,int size ) throws Exception {
      return gunzip(buf,pos,size);
   }

   // Décompression d'une tuile codée en GZIP2 (les octets de poids ont été placés en premier)
   private static byte [] decompGzip2( byte buf[], int pos,int size, int bitpix ) throws Exception {
      byte [] b = gunzip(buf,pos,size);
      int nbyte = Math.abs(bitpix)/8;

      // Dépermutation GZIP2
      int n = b.length/nbyte;
      byte [] c = new byte[ b.length ];

      int k=0;
      for( int j=0; j<n; j++ ) {
         for( int i=0; i<nbyte; i++ ) {
            c[k++] = b[ i*n + j ];
         }
      }
      return c;
   }
   
   // Décompression d'une tuile codée en HCOMPRESS
   private static byte [] decompHcomp( byte buf[], int pos,int size, int bitpix ) throws Exception {
      ByteArrayInputStream bytein = new ByteArrayInputStream(buf,pos,size);
      return Hdecomp.decomp(bytein);
   }

   /** Decompression Gzip d'un buffer de bytes */
   public static byte [] gunzip( byte buf[], int pos,int size ) throws Exception {
      ByteArrayInputStream bytein = new ByteArrayInputStream(buf,pos,size);
      GZIPInputStream gzin = new GZIPInputStream(bytein);
      ByteArrayOutputStream byteout = new ByteArrayOutputStream();

      int res = 0;
      byte tmp[] = new byte[1024];
      while (res >= 0) {
         res = gzin.read(tmp, 0, tmp.length);
         if (res > 0) byteout.write(tmp, 0, res);
      }
      return byteout.toByteArray();
   }

   // Décompression d'une tuile codée RICE et recodage immédiat dans la matrice de pixel (array)
   // Code courageusement traduit de CFITSIO
   public static void decompRice(byte buf[],int pos,byte array[], int offset, int tileSize,int nblock,int bsize,
         int bitpix,double bzero,double bscale) throws Exception {
      int i, k, imax;
      int nbits, nzero, fs;
      int b, diff;
      int lastpix;
      int bytevalue;
      int fsmax, fsbits, bbits;

      switch (bsize) {
         case 1:
            fsbits = 3;
            fsmax = 6;
            break;
         case 2:
            fsbits = 4;
            fsmax = 14;
            break;
         case 4:
            fsbits = 5;
            fsmax = 25;
            break;
         default: throw new Exception("RICE FITS decompRice error: bitpix must be 8, 16 or 32");
      }

      bbits = 1<<fsbits;

      lastpix = 0;
      for( i=0; i<bsize; i++ ) {
         bytevalue = 0xFF & buf[pos++];
         lastpix = (lastpix<<8) | bytevalue;
      }

      b = 0xFF & buf[pos++];         /* bit buffer           */
      nbits = 8;                 /* number of bits remaining in b    */
      for (i = 0; i<tileSize; ) {
         /* get the FS value from first fsbits */
         nbits -= fsbits;
         while (nbits < 0) {
            b = (b<<8) | (0xFF & buf[pos++]);
            nbits += 8;
         }

         fs = (b >>> nbits) - 1;
         b &= (1<<nbits)-1;
         /* loop over the next block */
         imax = i + nblock;
         if (imax > tileSize) imax = tileSize;
         if (fs<0) {
            /* low-entropy case, all zero differences */
            for ( ; i<imax; i++) setPixVal(array,bitpix,i+offset,lastpix*bscale+bzero);
         } else if (fs==fsmax) {
            /* high-entropy case, directly coded pixel values */
            for ( ; i<imax; i++) {
               k = bbits - nbits;
               diff = b<<k;
               for (k -= 8; k >= 0; k -= 8) {
                  b = 0xFF & buf[pos++];
                  diff |= b<<k;
               }
               if (nbits>0) {
                  b = 0xFF & buf[pos++];
                  diff |= b>>>(-k);
               b &= (1<<nbits)-1;
               } else {
                  b = 0;
               }
               /*
                * undo mapping and differencing
                * Note that some of these operations will overflow the
                * unsigned int arithmetic -- that's OK, it all works
                * out to give the right answers in the output file.
                */
               if ((diff & 1) == 0) {
                  diff = diff>>>1;
               } else {
                  diff = ~(diff>>>1);
               }
               lastpix = diff+lastpix;
               setPixVal(array,bitpix,i+offset,lastpix*bscale+bzero);
            }
         } else {
            /* normal case, Rice coding */
            for ( ; i<imax; i++) {
               /* count number of leading zeros */
               while (b == 0) {
                  nbits += 8;
                  b = 0xFF & buf[pos++];
               }
               nzero = nbits - nonzero_count[b];
               nbits -= nzero+1;
               /* flip the leading one-bit */
               b ^= 1<<nbits;
               /* get the FS trailing bits */
               nbits -= fs;
               while (nbits < 0) {
                  b = (b<<8) | (0xFF & buf[pos++]);
                  nbits += 8;
               }
               diff = (nzero<<fs) | (b>>>nbits);
               b &= (1<<nbits)-1;
               /* undo mapping and differencing */
               if ((diff & 1) == 0) {
                  diff = diff>>>1;
               } else {
                  diff = ~(diff>>>1);
               }
               lastpix = diff+lastpix;
               setPixVal(array,bitpix,i+offset,lastpix*bscale+bzero);
            }
         }
      }
   }

   private final static long MASK = 0xFFFFFFFFL;

   private static final int[] nonzero_count = {
         0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4,
         5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
         6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
         6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
   };


    // Copie des pixels de la tuile au bon endroit de la matrice des pixels
    private static void copyTile(byte [] tile, int tileWidth, int tileHeight, 
                         byte [] pix,  int pixPos,  int width,    int height,
                         int bitpix, double bzero, double bscale ) throws Exception {

       // Si pas de facteur d'échelle, on va recopier d'ou coup les bytes
       // constitutifs de chaque pixel
       boolean flagScale = bscale!=1 || bzero!=0;
       if( !flagScale ) {
          
          int npix = Math.abs(bitpix)/8;
          
          // si la tuile à la même largeur que l'image ou va faire un simple dump
          if( tileWidth==width ) {
             System.arraycopy( tile, 0, pix, pixPos*npix, tileWidth*tileHeight*npix );
             return;
          }
             
          // sinon il va falloir travailler ligne à ligne
          for( int y=0; y<tileHeight; y++ ) {
             int src = y*tileWidth;
             int trg = (pixPos + y*width) * npix;
             System.arraycopy(tile, src, pix, trg, tileWidth*npix);
          }
          return;
       }
       
       // Méthode pixel par pixel pour pouvoir appliquer le facteur d'échelle
       for( int y=0; y<tileHeight; y++ ) {
          for( int x=0; x<tileWidth; x++ ) {
             double val = getPixVal1(tile, bitpix, y*tileWidth +x );
             val = val*bscale+bzero;
             setPixVal( pix, bitpix, pixPos + y*width +x, val);
          }
       }
    }
    
    
    // ----------------------- Utilitaires de manipulation des types primitifs en bytes --------------------
    
    static final public int getByte(byte[] t, int i) {
       return t[i]&0xFF;
    }
    static final public int getShort(byte[] t, int i) {
       return ( t[i]<<8) | t[i+1]&0xFF;
    }
    static final public int getInt(byte[] t, int i) {
       return ((t[i]<<24) | ((t[i+1]&0xFF)<<16) | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF);
    }
    static final public long getLong(byte[] t, int i) {
       return (((long)((t[i]<<24) | ((t[i+1]&0xFF)<<16) | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF))<<32)
             | ((((t[i+4]<<24) | ((t[i+5]&0xFF)<<16) | ((t[i+6]&0xFF)<<8) | t[i+7]&0xFF)) & 0xFFFFFFFFL);
    }
    static final public double getFloat(byte[] t, int i) {
       return Float.intBitsToFloat(((t[i]<<24) | ((t[i+1]&0xFF)<<16)
             | ((t[i+2]&0xFF)<<8) | t[i+3]&0xFF));
    }
    static final public double getDouble(byte[] t, int i) {
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
    static final public double getPixVal1(byte[] t,int bitpix,int i) {
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

    // Conversion entier 32 en byte dans le tableau t[] à partir de l'emplacement i
    static final public void setInt(byte[] t,int i,int val) {
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
    static final public void setPixVal(byte[] t,int bitpix,int i,double val) {
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
}
