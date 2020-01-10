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

package cds.aladin;

import java.util.Date;

import cds.tools.Util;

/**
 * Plan dedie a une image RICE
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : sept 2011 - Prise en compte ZSCALE, et ZZERO
 * @version 1.0 : mars 2008 - creation
 */
public class PlanImageRice extends PlanImage {

   protected PlanImageRice(Aladin aladin, String file,MyInputStream inImg,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,inImg,label,from,o,imgNode,skip,doClose,forPourcent);
   }
   
   
   protected boolean cacheImageFits(MyInputStream dis) throws Exception {

      int taille;       // nombre d'octets a lire
      int n;            // nombre d'octets pour un pixel


      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);

      bitpix = headerFits.getIntFromHeader("ZBITPIX");
      width = naxis1 = headerFits.getIntFromHeader("ZNAXIS1");
      height = naxis2 = headerFits.getIntFromHeader("ZNAXIS2");
      npix = n = Math.abs(bitpix)/8;    // Nombre d'octets par valeur
      taille=width*height*n;    // Nombre d'octets
      setPourcent(0);
      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" BITPIX="+bitpix+" => size="+taille);

      // Les paramètres FITS facultatifs
      loadFitsHeaderParam(headerFits);

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      int nnaxis1 = headerFits.getIntFromHeader("NAXIS1");
      int nnaxis2 = headerFits.getIntFromHeader("NAXIS2");
      int theap=nnaxis1*nnaxis2;
      try  { theap = headerFits.getIntFromHeader("THEAP"); } catch( Exception e ) {}

      int pcount=headerFits.getIntFromHeader("PCOUNT");    // nombres d'octets a lire en tout
      int tile = headerFits.getIntFromHeader("ZTILE1");
      boolean cut = aladin.configuration.getCMCut();

      int nblock=32;
      try { nblock = headerFits.getIntFromHeader("ZVAL1"); } catch( Exception e ) {}

      int bsize=4;
      try { bsize = headerFits.getIntFromHeader("ZVAL2"); } catch( Exception e ) {}


      setBufPixels8(new byte[width*height]);

      if( flagSkip ) {
         dis.skip( theap+pcount );

      } else {

         Aladin.trace(2,"Loading RICE FITS image extension (NBLOCK="+nblock+" BSIZE="+bsize+")");

         int posCompress=0;
         int posZscale=-1;
         int posZzero=-1;
         int posUncompress=-1;

         int tfields = headerFits.getIntFromHeader("TFIELDS");
         for( int i=1,pos=0; i<=tfields; i++ ) {
            String type = headerFits.getStringFromHeader("TTYPE"+i);
            if( type.equals("COMPRESSED_DATA") ) posCompress = pos;
            else if( type.equals("ZSCALE") ) posZscale = pos;
            else if( type.equals("ZZERO") ) posZzero = pos;
            else if( type.equals("UNCOMPRESSED_DATA") ) posUncompress = pos;
            String form = headerFits.getStringFromHeader("TFORM"+i);
            pos+=Util.binSizeOf(form);
         }
         Aladin.trace(2,"Loading RICE FITS image extension (TFIELDS="+tfields+" NBLOCK="+nblock+" BSIZE="+bsize+")");

         pixelsOrigin = new byte[taille];

         byte [] table = new byte[nnaxis1*nnaxis2];
         byte [] heap = new byte[pcount];

         try {
            dis.readFully(table);
            dis.skip(theap - nnaxis1*nnaxis2);
            dis.readFully(heap);

            int offset=0;
            for( int row=0; row<nnaxis2; row++ ) {
               int offsetRec = row*nnaxis1;
               int size = getInt(table,offsetRec+posCompress);
               int pos = getInt(table,offsetRec+posCompress+4);
               double bzero = posZzero<0 ? 0 : getDouble(table,offsetRec+posZzero);
               double bscale = posZscale<0 ? 1 : getDouble(table,offsetRec+posZscale);
               
               System.out.println("row="+row+" size="+size+" pos="+pos+" bscale="+bscale+" bzero="+bzero);

               // Non compressé
               if( size==0 && posUncompress>=0 ) {
                  size = getInt(table,offsetRec+posUncompress);
                  pos  = getInt(table,offsetRec+posUncompress);
                  direct(heap,pos,pixelsOrigin,offset,tile,bitpix,bzero,bscale);

                  // Compressé
               } else decomp(heap,pos,pixelsOrigin,offset,tile,bsize,nblock,bitpix,bzero,bscale);

               offset+=tile;
            }
         }catch (Exception e ) { e.printStackTrace(); }

         findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0,0,0,0);
         to8bits(getBufPixels8(),0,pixelsOrigin,width*height,bitpix, pixelMin,pixelMax,true);
      }


      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
      if( flagSkip ) return true;

      // Nécessaire pour que la création de la Calib ne se plante pas
      headerFits.setKeyword("BITPIX",bitpix+"");
      headerFits.setKeyword("NAXIS1",width+"");
      headerFits.setKeyword("NAXIS2",height+"");
      headerFits.setKeyword("NAXIS","2");

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Reading, uncompressing "+(cut?"and autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");

      // Retournement de l'image (les lignes ne sont pas rangees dans le meme ordre
      // en FITS et en JAVA
      invImageLine(width,height,getBufPixels8());

      creatDefaultCM();
      setPourcent(99);
      return true;
   }

//   static int nonzero_count[] =null;
   static byte b[] = new byte[1];

//   static final public void setPixVal(byte[] t,int bitpix,int i,int c) {
//      switch(bitpix) {
//         case   8: t[i]=(byte)(0xFF & c);
//         break;
//         case  16: i*=2;
//         t[i]  =(byte)(0xFF & (c>>>8));
//         t[i+1]=(byte)(0xFF & c);
//         break;
//         case  32: i*=4;
//         setInt(t,i,c);
//         break;
//         case -32: i*=4;
//         c=Float.floatToIntBits(c);
//         setInt(t,i,c);
//         break;
//         case -64: i*=8;
//         long c1 = Double.doubleToLongBits(c);
//         c = (int)(0xFFFFFFFFL & (c1>>>32));
//         setInt(t,i,c);
//         c = (int)(0xFFFFFFFFL & c1);
//         setInt(t,i+4,c);
//         break;
//      }
//   }

   public static void direct(byte buf[],int pos,byte array[], int offset,int nx,
         int bitpix,double bzero,double bscale) throws Exception {
      int size = Math.abs(bitpix)/8;
      for( int i=0; i<nx; i+=size ) {
         double val = getPixVal1(buf, bitpix, pos+i);
         setPixVal(array, bitpix, offset+i, val*bscale+bzero);
      }
   }
   
   public static void decomp(byte buf[],int pos,byte array[], int offset,int nx,int bsize,int nblock,
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
         default: throw new Exception("Rice.decomp error: bitpix must be 8, 16 or 32");
      }
      
      bbits = 1<<fsbits;

//      if (nonzero_count == null) {
//         /*
//          * nonzero_count is lookup table giving number of bits
//          * in 8-bit values not including leading zeros
//          */
//         nonzero_count = new int[256];
//         nzero = 8;
//         k = 128;
//         for (i=255; i>=0; ) {
//            for ( ; i>=k; i--) nonzero_count[i] = nzero;
//            k = k/2;
//            nzero--;
//         }
//      }

      /*
       * Decode in blocks of nblock pixels
       */

      lastpix = 0;
      for( i=0; i<bsize; i++ ) {
         bytevalue = 0xFF & buf[pos++];
         lastpix = (lastpix<<8) | bytevalue;
      }

      b = 0xFF & buf[pos++];         /* bit buffer           */
      nbits = 8;                 /* number of bits remaining in b    */
      for (i = 0; i<nx; ) {
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
         if (imax > nx) imax = nx;
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

   
   // En cours de correction => ne pmarche toujours pas pour les RICE_ONE
//   public static void decomp(byte buf[],int pos,byte array[], int offset,int nx,int bsize,int nblock,
//         int bitpix,double bzero,double bscale) throws Exception {
//      int i, k, imax;
//      int nbits, nzero;
//      long fs,b,diff;
//      long lastpix;
//      long bytevalue;
//      int fsmax, fsbits, bbits;
//
//      switch (bsize) {
//         case 1:
//            fsbits = 3;
//            fsmax = 6;
//            break;
//         case 2:
//            fsbits = 4;
//            fsmax = 14;
//            break;
//         case 4:
//            fsbits = 5;
//            fsmax = 25;
//            break;
//         default: throw new Exception("Rice.decomp error: bitpix must be 8, 16 or 32");
//      }
//      
//      bbits = 1<<fsbits;
//
////      if (nonzero_count == null) {
////         /*
////          * nonzero_count is lookup table giving number of bits
////          * in 8-bit values not including leading zeros
////          */
////         nonzero_count = new int[256];
////         nzero = 8;
////         k = 128;
////         for (i=255; i>=0; ) {
////            for ( ; i>=k; i--) nonzero_count[i] = nzero;
////            k = k/2;
////            nzero--;
////         }
////      }
//
//      /*
//       * Decode in blocks of nblock pixels
//       */
//
//      lastpix = 0L;
//      for( i=0; i<bsize; i++ ) {
//         bytevalue = 0xFF & buf[pos++];
//         lastpix = (lastpix<<8) | bytevalue;
//      }
//      lastpix &= MASK;
//
//      b = 0xFF & buf[pos++];         /* bit buffer           */
//      nbits = 8;                 /* number of bits remaining in b    */
//      for (i = 0; i<nx; ) {
//         /* get the FS value from first fsbits */
//         nbits -= fsbits;
//         while (nbits < 0) {
//            b = (b<<8) | (0xFF & buf[pos++]);
//            nbits += 8;
//         }
//
//         fs = (b >>> nbits) - 1L;
//         b &= (1<<nbits)-1;
//         /* loop over the next block */
//         imax = i + nblock;
//         if (imax > nx) imax = nx;
//         if (fs<0) {
//            /* low-entropy case, all zero differences */
//            for ( ; i<imax; i++) setPixVal(array,bitpix,i+offset,lastpix,bscale,bzero);
//         } else if (fs==fsmax) {
//            /* high-entropy case, directly coded pixel values */
//            for ( ; i<imax; i++) {
//               k = bbits - nbits;
//               diff = b<<k;
//               for (k -= 8; k >= 0; k -= 8) {
//                  b = 0xFF & buf[pos++];
//                  diff |= b<<k;
//               }
//               if (nbits>0) {
//                  b = 0xFF & buf[pos++];
//                  diff |= b>>>(-k);
//                  b &= (1<<nbits)-1L;
//               } else {
//                  b = 0;
//               }
//               /*
//                * undo mapping and differencing
//                * Note that some of these operations will overflow the
//                * unsigned int arithmetic -- that's OK, it all works
//                * out to give the right answers in the output file.
//                */
//               diff &= MASK;
//               if ((diff & 1) == 0) {
//                  diff = diff>>>1;
//               } else {
//                  diff = ~(diff>>>(1&MASK));
//               }
//               lastpix = diff + (lastpix&MASK);
//               setPixVal(array,bitpix,i+offset,lastpix,bscale,bzero);
//            }
//         } else {
//            /* normal case, Rice coding */
//            for ( ; i<imax; i++) {
//               /* count number of leading zeros */
//               while (b == 0) {
//                  nbits += 8;
//                  b = 0xFF & buf[pos++];
//               }
//               nzero = nbits - nonzero_count[ (int)(b & 0xFF) ];
//               nbits -= nzero+1;
//               /* flip the leading one-bit */
//               b ^= 1<<nbits;
//               /* get the FS trailing bits */
//               nbits -= fs;
//               while (nbits < 0) {
//                  b = (b<<8) | (0xFF & buf[pos++]);
//                  nbits += 8;
//               }
//               diff = (nzero<<fs) | (b>>>nbits);
//               b &= (1<<nbits)-1L;
//               
//               diff &= MASK;
//               /* undo mapping and differencing */
//               if ((diff & 1) == 0) {
//                  diff = diff>>>1;
//               } else {
//                  diff = ~(diff>>>(1&MASK));
//               }
//               lastpix = diff + (lastpix&MASK);
//               setPixVal(array,bitpix,i+offset,lastpix,bscale,bzero);
//            }
//         }
//      }
//   }
//   
//   static final public void setPixVal(byte[] t,int bitpix,int i,long val, double bscale, double bzero) {
//      double x;
//      if( bitpix>0 ) x = (int)val;
//      else if( bitpix==-32 ) {
//         x = Float.intBitsToFloat((int)(MASK & val));
//      } else {
//         x = Double.longBitsToDouble(val);
//      }
//      setPixVal(t,bitpix,i,x*bscale+bzero);
//   }
}

