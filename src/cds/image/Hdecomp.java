
package cds.image;

import java.lang.Math;
import java.io.*;

/**
 * Hdecomp. Uncompress method to astronomical images
 * Example :
 *
 * <PRE>
 * public class ImageDemo {
 *       
 *    // To read fits header
 *    static void readFitsHeader(DataInputStream dis) throws Exception {
 *       byte [] line = new byte[80];
 *       do {
 *          dis.readFully(line);
 *       } while(line[0]!='E' || line[1]!='N' || line[2]!='D' || line[3]!=' ');
 *    }
 *    
 *    // The main method to test it
 *    static public void main(String [] arg) {
 *        
 *    
 *       try {
 *          // Open the first arg as a file
 *          DataInputStream dis = new DataInputStream( new FileInputStream(arg[0]));
 *          
 *          // read the FITS header
 *          System.out.println("Reading Fits header...");
 *          readFitsHeader(dis);
 *          
 *          // Parse the file
 *          System.out.println("Uncompressing...");
 *          byte [] pixels=Hdecomp.decomp( (InputStream)dis );
 *          System.out.println("I've uncompressed "+pixels.length+" bytes");
 *                   
 *       } catch( Exception e ) {
 *          System.err.println("There is a problem: "+e);
 *          e.printStackTrace();
 *       }
 *    }
 * }
 * </PRE>
 *
 * @author Pierre Fernique [CDS] from hdecomp package (C language) by R. White - 1991
 *         with the help of the Pat Dowler java code (CADC)
 * @Copyright (c) 1993 Association of Universities for Research in Astronomy
 * @version 1.0 : (10 jun 2000) creation
 */
public final class Hdecomp {
   private static int SIZEBUF=8192;
   private static byte [] buf = new byte[SIZEBUF]; // buffer
   private static int ptBuf=0;			   // Pointer to the buffer
   private static int maxBuf=0;			   // # of bytes in buffer
   private static InputStream dis;		   // Input stream
   
   private static double log2 = Math.log(2.);
   private static int[] code_magic = { 0xDD, 0x99 };	
   
   private static int nx,ny;		// Image size
   private static int nel;		// Number of pixels
   private static int scale;		// Hcompress scale
   private static int a[];		// image
   
   private static int buffer;		// Bits waiting to be input
   private static int bits_to_go;	// Number of bits still in buffer
   private static int [] nbitplanes = new int[3];

   /**
    * Hdecompress static method.
    * @param fdis the input Stream (begins by 0xDD 0x99 => without the Fits
    *             header)
    * @return byte[] the uncompressed Fits image
    */
   public static byte[] decomp(InputStream fdis) throws Exception {
      dis=fdis;			// Memo
      decode();			// Launch decoding
      undigitize();		// Un-Digitize
      hinv();			// Inverse H-transform
      return getPixels();	// Write data
   }
   
  /** Sets into a byte[] the uncompressed Fits image
   * from a[]
   */
   private static byte [] getPixels() {
      byte [] pixels=new byte[nel*2];
      int i,j;
      
      for( j=i=0; i<pixels.length; i+=2 ) {
         pixels[i]=(byte)( (a[j]>>8) & 0xff);
         pixels[i+1]=(byte)( a[j++] & 0xff);
      }
      return pixels;
   }
   
  /* Input bufferisation.
   * @return the next byte */
   private static int getc() throws Exception {
      while( ptBuf==maxBuf ) {
         ptBuf=0;
         if( (maxBuf=dis.read(buf,0,buf.length))==-1 ) throw new EOFException();
      }
      return (int)buf[ptBuf++] & 0xFF;
   }
   
  /* Input bufferisation.
   * @return the next int */
   private static int getint() throws Exception {
      return (getc()<<24) | (getc()<<16) | (getc()<<8) | getc();
   }
   
   /* Initialize bit input */
   private static void start_inputing_bits() {
      bits_to_go = 0;
   }

   /* Input a bit */
   private static int input_bit() throws Exception {

      if( bits_to_go == 0) {      /* Read the next byte if no     */
         buffer = getc();    /* bits are left in buffer      */
         bits_to_go = 8;
      }

      /* Return the next bit */      
      return((buffer>>(--bits_to_go)) & 1);
   }


   /* INPUT N BITS (N must be <= 8) */
   private static int input_nbits(int n) throws Exception {
      int c;

      if (bits_to_go < n) {

         /* need another byte's worth of bits */
         buffer <<= 8;
         buffer |= getc();
         bits_to_go += 8;
      }

      /*  now pick off the first n bits */
      bits_to_go -= n;
      return( (buffer>>bits_to_go) & ((1<<n)-1) );
   }

   /*
    * Huffman decoding for fixed codes
    *
    * Coded values range from 0-15
    *
    * Huffman code values (hex):
    *
    *	3e, 00, 01, 08, 02, 09, 1a, 1b,
    *	03, 1c, 0a, 1d, 0b, 1e, 3f, 0c
    *
    * and number of bits in each code:
    *
    *	6,  3,  3,  4,  3,  4,  5,  5,
    *	3,  5,  4,  5,  4,  5,  6,  4
    */
   private static int input_huffman() throws Exception {
      int c;

      /* get first 3 bits to start */
      c = input_nbits(3);
      if (c < 4) {
         /* this is all we need
          * return 1,2,4,8 for c=0,1,2,3 */
         return(1<<c);
      }

      /* get the next bit */
      c = input_bit() | (c<<1);
      if (c < 13) {
         /* OK, 4 bits is enough */
         switch (c) {
            case  8 : return(3);
            case  9 : return(5);
            case 10 : return(10);
            case 11 : return(12);
            case 12 : return(15);
         }
      }

      /* get yet another bit */
      c = input_bit() | (c<<1);
      if (c < 31) {
         /* OK, 5 bits is enough */
         switch (c) {
            case 26 : return(6);
            case 27 : return(7);
            case 28 : return(9);
            case 29 : return(11);
            case 30 : return(13);
         }
      }

      /* need the 6th bit */
      c = input_bit() | (c<<1);
      if (c == 62) return(0);
      else return(14);
   }

   /*
    * Copy 4-bit values from a[(nx+1)/2,(ny+1)/2] to b[nx,ny], expanding
    * each value to 2x2 pixels and inserting into bitplane BIT of B.
    * A,B may NOT be same array (it wouldn't make sense to be inserting
    * bits into the same array anyway.)
    * @param n declared y dimension of b
    */
   private static void qtree_bitins(byte d[],int nx,int ny,
                                    int off,int n,int bit) {
      int i, j, k;
      int s00, s10;
      int nxN=nx-1;
      int nyN=ny-1;
      int c;
      int dc;

      /* expand each 2x2 block */
      c=0;				/* k   is index of a[i/2,j/2] */
      for (i = 0; i<nxN; i += 2) {
         s00 = off+n*i;			/* s00 is index of a[i,j] */
         s10 = s00+n;			/* s10 is index of a[i+1,j] */
         for (j = 0; j<nyN; j += 2) {
            dc = d[c];
            a[s10+1] |= ( dc     & 1) << bit;
            a[s10  ] |= ((dc>>1) & 1) << bit;
            a[s00+1] |= ((dc>>2) & 1) << bit;
            a[s00  ] |= ((dc>>3) & 1) << bit;
            s00 += 2;
            s10 += 2;
            c++;
         }
         if (j < ny) {
            /* row size is odd, do last element in row
             * s00+1, s10+1 are off edge */
            dc = d[c];
            a[s10  ] |= ((dc>>1) & 1) << bit;
            a[s00  ] |= ((dc>>3) & 1) << bit;
            c++;
         }
      }
      if (i < nx) {
         /* column size is odd, do last row
          * s10, s10+1 are off edge */
         s00 = off+n*i;
         for (j = 0; j<nyN; j += 2) {
            dc = d[c];
            a[s00+1] |= ((dc>>2) & 1) << bit;
            a[s00  ] |= ((dc>>3) & 1) << bit;
            s00 += 2;
            c++;
         }
         if (j < ny) {
            /* both row and column size are odd, do corner element
             * s00+1, s10, s10+1 are off edge */
            a[s00  ] |= ((d[c]>>3) & 1) << bit;
            c++;
         }
      }
   }

   private static void read_bdirect(int off,int n,int nqx,int nqy,
			    byte scratch[],int bit) throws Exception {
      int i;
      int j=((nqx+1)/2) * ((nqy+1)/2);
      
      /* read bit image packed 4 pixels/nybble */
      for (i = 0; i < j; i++) {
         scratch[i] = (byte)(input_nbits(4));
      }

      /* insert in bitplane BIT of image A */
      qtree_bitins(scratch,nqx,nqy,off,n,bit);
   }

   /*
    * copy 4-bit values from a[(nx+1)/2,(ny+1)/2] to b[nx,ny], expanding
    * each value to 2x2 pixels
    * a,b may be same array
    * @param n declared y dimension of b
    */
   private static void qtree_copy(byte d[],int nx,int ny,byte b[],int n) {
      int i, j, k, nx2, ny2;
      int s00, s10;
      int nxN=nx-1;
      int nyN=ny-1;
      int bs00;

      /* first copy 4-bit values to b
       * start at end in case a,b are same array */
      nx2 = (nx+1)/2;
      ny2 = (ny+1)/2;
      k = ny2*(nx2-1)+ny2-1;  		/* k   is index of a[i,j] */
      for (i = nx2-1; i >= 0; i--) {
         s00 = (n*i+ny2-1)<<1;		/* s00 is index of b[2*i,2*j] */
         for (j = ny2-1; j >= 0; j--) {
            b[s00] = d[k--];
            s00 -= 2;
         }
      }

      /* now expand each 2x2 block */
      for (i = 0; i<nxN; i += 2) {
         s00 = n*i;		/* s00 is index of b[i,j] */
         s10 = s00+n;		/* s10 is index of b[i+1,j] */
         for (j = 0; j<nyN; j += 2) {
            bs00=b[s00];
            b[s10+1] = (byte)( bs00     & 1);
            b[s10  ] = (byte)((bs00>>1) & 1);
            b[s00+1] = (byte)((bs00>>2) & 1);
            b[s00  ] = (byte)((bs00>>3) & 1);
            s00 += 2;
            s10 += 2;
         }
         if (j < ny) {
            /* row size is odd, do last element in row
             * s00+1, s10+1 are off edge */
            bs00=b[s00];
            b[s10  ] = (byte)((bs00>>1) & 1);
            b[s00  ] = (byte)((bs00>>3) & 1);
         }
      }
      if (i < nx) {
         /* column size is odd, do last row
          * s10, s10+1 are off edge */
         s00 = n*i;
         for (j = 0; j<nyN; j += 2) {
            bs00=b[s00];
            b[s00+1] = (byte)((bs00>>2) & 1);
            b[s00  ] = (byte)((bs00>>3) & 1);
            s00 += 2;
         }
         if (j < ny) {
            /* both row and column size are odd, do corner element
             * s00+1, s10, s10+1 are off edge */
            b[s00  ] = (byte)((b[s00]>>3) & 1);
         }
      }
   }

   /*
    * do one quadtree expansion step on array a[(nqx+1)/2,(nqy+1)/2]
    * results put into b[nqx,nqy] (which may be the same as a)
    */
   private static void qtree_expand(byte d[],int nx,int ny, byte b[])
                       throws Exception {
      int i;

      /* first copy a to b, expanding each 4-bit value */
      qtree_copy(d,nx,ny,b,ny);

      /* now read new 4-bit values into b for each non-zero element */
      for (i = nx*ny-1; i >= 0; i--) {
         if( b[i]!= 0 ) b[i] = (byte) input_huffman();
      }
   }
   
   /*
    * @param n length of full row in a
    * @param nqx partial length of row to decode
    * @param nqy partial length of column (<=n)
    * @param nbitplanes number of bitplanes to decode 
    */
   private static void qtree_decode(int off,int n,int nqx,int nqy,int nbitplanes)
                            throws Exception {
      int log2n, k, bit, b, nqmax;
      int nx,ny,nfx,nfy,c;
      int nqx2, nqy2;
      byte scratch[];

      /* log2n is log2 of max(nqx,nqy) rounded up to next power of 2 */
      nqmax = (nqx>nqy) ? nqx : nqy;
      log2n = (int)( Math.log(nqmax)/log2+0.5 );
      if (nqmax > (1<<log2n)) log2n += 1;

      /* allocate scratch array for working space */
      nqx2=(nqx+1)/2;
      nqy2=(nqy+1)/2;
      scratch = new byte[nqx2 * nqy2];

      /*
       * now decode each bit plane, starting at the top
       * A is assumed to be initialized to zero
       */
      for ( bit = nbitplanes-1; bit >= 0; bit--) {

         /* Was bitplane was quadtree-coded or written directly? */
         b = input_nbits(4);
         if( b == 0) {
            /* bit map was written directly */
            read_bdirect(off,n,nqx,nqy,scratch,bit);
         } else if (b != 0xf) {
            throw new Exception("qtree_decode: bad format code "+b);
         } else {

            /* bitmap was quadtree-coded, do log2n expansions
             * read first code */
            scratch[0] = (byte) input_huffman();

            /* now do log2n expansions, reading codes from file as necessary */
            nx = 1;
            ny = 1;
            nfx = nqx;
            nfy = nqy;
            c = 1<<log2n;
            for (k = 1; k<log2n; k++) {
	       /* this somewhat cryptic code generates the sequence
	        * n[k-1] = (n[k]+1)/2 where n[log2n]=nqx or nqy */
	       c = c>>1;
	       nx = nx<<1;
	       ny = ny<<1;
	       if (nfx <= c) { nx -= 1; } else { nfx -= c; }
	       if (nfy <= c) { ny -= 1; } else { nfy -= c; }
	       qtree_expand(scratch,nx,ny,scratch); 
            }

            /* now copy last set of 4-bit codes to bitplane bit of array a */
            qtree_bitins(scratch,nqx,nqy,off,n,bit);
         }
      }
      scratch=null;
   }

   private static void dodecode() throws Exception {
      int i, nx2, ny2, aa;

      nx2 = (nx+1)/2;
      ny2 = (ny+1)/2;

      /* Initialize bit input */
      start_inputing_bits();

      /* read bit planes for each quadrant */
      qtree_decode( 0,          ny, nx2,  ny2,  nbitplanes[0]);
      qtree_decode( ny2,        ny, nx2,  ny/2, nbitplanes[1] );
      qtree_decode( ny*nx2,     ny, nx/2, ny2,  nbitplanes[1] );
      qtree_decode( ny*nx2+ny2, ny, nx/2, ny/2, nbitplanes[2] );

      /* make sure there is an EOF symbol (nybble=0) at end */
      if (input_nbits(4) != 0) {
    	  throw new IOException("dodecode: bad bit plane values\n");
//         System.err.println("dodecode: bad bit plane values\n");
//         System.exit(-1);
      }

      /* now get the sign bits - Re-initialize bit input*/
      start_inputing_bits();
      for (i=0; i<nel; i++) {        
         if( (aa=a[i])!=0 && input_bit() != 0) a[i] = -aa;
      }

   }

   private static void decode() throws Exception {
      int sumall;
      int q=0,w=0;
      
      //init the buffer mechanism
      ptBuf=maxBuf = 0;
      
      //read magic number
      if( (q=getc())!=code_magic[0] || (w=getc())!=code_magic[1] ) {
         throw new Exception("Bad magic number");
      }
      
      // read size
      nx = getint();	
      ny = getint();
      nel=nx*ny;
      
      // read scale
      scale=getint();
      
      // allocation
      a = new int[nel];
      
      // sum of all pixels
      sumall=getint();

      // # bits in quadrants
      nbitplanes[0]=getc();
      nbitplanes[1]=getc();
      nbitplanes[2]=getc();
      
      // Go
      dodecode();
        
      // at the end
      a[0]=sumall;
   }

   private static void undigitize() {
      if (scale <= 1) return;
      for( int i=nel-1; i>=0; i-- ) a[i] *= scale;
   }


   /*
    * @param off offset in the a[] array to shuffle
    * @param n number of elements to shuffle
    * @param n2 second dimension
    * @param tmp scratch storage
    */
   private static void unshuffle(int off,int n,int n2,int tmp[]) {
      int i;
      int nhalf;
      int p1, p2, pt;
      int n22=n2<<1;
      
      /* copy 2nd half of array to tmp */
      nhalf = (n+1)>>1;
      pt = 0;
      p1=off + n2*nhalf;
      for (i=nhalf; i<n; i++) {
         tmp[pt++] = a[p1];
         p1 += n2;
      }

      /* distribute 1st half of array to even elements */
      p2= off + (n2*(nhalf-1));
      p1=off + ((n2*(nhalf-1))<<1);
      for (i=nhalf-1; i >= 0; i--) {
         a[p1] = a[p2];
         p2 -= n2;
         p1 -= n22;
      }

      /* now distribute 2nd half of array (in tmp) to odd elements */
      pt = 0;
      p1 = off + n2;      
      for (i=1; i<n; i += 2) {
         a[p1] = tmp[pt++];
         p1 += n22;
      }


   }

   private static void hinv() {
      int nmax, log2n, i, j, k;
      int nxtop,nytop,nxf,nyf,c;
      int oddx,oddy;
      int shift, bit0, bit1, bit2, mask0, mask1, mask2,
	      prnd0, prnd1, prnd2, nrnd0, nrnd1, nrnd2, lowbit0, lowbit1;
      int h0, hx, hy, hc;
      int s10, s00;
      int [] tmp;

      // need to init to keep javac quiet
      h0 = hx = hy = hc = 0;
      lowbit0 = 0;
      
      /* log2n is log2 of max(nx,ny) rounded up to next power of 2 */
      nmax = (nx>ny) ? nx : ny;
      log2n = (int)(Math.log(nmax)/log2+0.5);
      if ( nmax > (1<<log2n) ) log2n++;

      /* get temporary storage for shuffling elements */  
      tmp = new int[ (nmax+1)/2 ];

      /* set up masks, rounding parameters */
      shift  = 1;
      bit0   = 1 << (log2n - 1);
      bit1   = bit0 << 1;
      bit2   = bit0 << 2;
      mask0  = -bit0;
      mask1  = mask0 << 1;
      mask2  = mask0 << 2;
      prnd0  = bit0 >> 1;
      prnd1  = bit1 >> 1;
      prnd2  = bit2 >> 1;
      nrnd0  = prnd0 - 1;
      nrnd1  = prnd1 - 1;
      nrnd2  = prnd2 - 1;

      /* round h0 to multiple of bit2 */
      a[0] = (a[0] + ((a[0] >= 0) ? prnd2 : nrnd2)) & mask2;

      /* do log2n expansions
       * We're indexing a as a 2-D array with dimensions (nx,ny). */
      nxtop = 1;
      nytop = 1;
      nxf = nx;
      nyf = ny;
      c = 1<<log2n;
      for (k = log2n-1; k>=0; k--) {

         /* this somewhat cryptic code generates the sequence
          * ntop[k-1] = (ntop[k]+1)/2, where ntop[log2n] = n */
         c = c>>1;
         nxtop = nxtop<<1;
         nytop = nytop<<1;
         if (nxf <= c) { nxtop -= 1; } else { nxf -= c; }
         if (nyf <= c) { nytop -= 1; } else { nyf -= c; }

         /* double shift and fix nrnd0 (because prnd0=0) on last pass */
         if (k == 0) {
            nrnd0 = 0;
            shift = 2;
         }

         /* unshuffle in each dimension to interleave coefficients */
         for (i = 0; i<nxtop; i++)  unshuffle(ny*i,nytop,1,tmp) ;
         for (j = 0; j<nytop; j++)  unshuffle(j,nxtop,ny,tmp);

         oddx = nxtop % 2;
         oddy = nytop % 2;
         
         for (i = 0; i<nxtop-oddx; i += 2) {
            s00 = ny*i;			/* s00 is index of a[i,j] */
            s10 = s00+ny;			/* s10 is index of a[i+1,j] */
            for (j = 0; j<nytop-oddy; j += 2) {
	       h0 = a[s00  ];
	       hx = a[s10  ];
	       hy = a[s00+1];
	       hc = a[s10+1];

	       /* round hx and hy to multiple of bit1, hc to multiple of bit0
	        * h0 is already a multiple of bit2 */
	       hx = (hx + ((hx >= 0) ? prnd1 : nrnd1)) & mask1;
	       hy = (hy + ((hy >= 0) ? prnd1 : nrnd1)) & mask1;
	       hc = (hc + ((hc >= 0) ? prnd0 : nrnd0)) & mask0;

	       /* propagate bit0 of hc to hx,hy */
	       lowbit0 = hc & bit0;
	       hx = (hx >= 0) ? (hx - lowbit0) : (hx + lowbit0);
	       hy = (hy >= 0) ? (hy - lowbit0) : (hy + lowbit0);

	       /* Propagate bits 0 and 1 of hc,hx,hy to h0.
	        * This could be simplified if we assume h0>0, but then
	        * the inversion would not be lossless for images with
	        * negative pixels. */
	       lowbit1 = (hc ^ hx ^ hy) & bit1;
	       h0 = (h0 >= 0)
		       ? (h0 + lowbit0 - lowbit1)
		       : (h0 + ((lowbit0 == 0) ? lowbit1 : (lowbit0-lowbit1)));

	       /* Divide sums by 2 (4 last time) */
	       a[s10+1] = (h0 + hx + hy + hc) >> shift;
	       a[s10  ] = (h0 + hx - hy - hc) >> shift;
	       a[s00+1] = (h0 - hx + hy - hc) >> shift;
	       a[s00  ] = (h0 - hx - hy + hc) >> shift;
	       s00 += 2;
	       s10 += 2;
            }
            if( oddy!=0) {
               /* do last element in row if row length is odd
                * s00+1, s10+1 are off edge */
               h0 = a[s00  ];
               hx = a[s10  ];
               hx = ((hx >= 0) ? (hx+prnd1) : (hx+nrnd1)) & mask1;
               lowbit1 = hx & bit1;
               h0 = (h0 >= 0) ? (h0 - lowbit1) : (h0 + lowbit1);
               a[s10  ] = (h0 + hx) >> shift;
               a[s00  ] = (h0 - hx) >> shift;
            }
         }
         if( oddx!=0) {
            /* do last row if column length is odd
             * s10, s10+1 are off edge */
            s00 = ny*i;
            for (j = 0; j<nytop-oddy; j += 2) {
             h0 = a[s00  ];
             hy = a[s00+1];
             hy = ((hy >= 0) ? (hy+prnd1) : (hy+nrnd1)) & mask1;
             lowbit1 = hy & bit1;
             h0 = (h0 >= 0) ? (h0 - lowbit1) : (h0 + lowbit1);
             a[s00+1] = (h0 + hy) >> shift;
             a[s00  ] = (h0 - hy) >> shift;
             s00 += 2;
            }
            if( oddy!=0) {
             /* do corner element if both row and column lengths are odd
              * s00+1, s10, s10+1 are off edge */
             h0 = a[s00  ];
             a[s00  ] = h0 >> shift;
            }
         }

         /* divide all the masks and rounding values by 2 */
         bit2 = bit1;
         bit1 = bit0;
         bit0 = bit0 >> 1;
         mask1 = mask0;
         mask0 = mask0 >> 1;
         prnd1 = prnd0;
         prnd0 = prnd0 >> 1;
         nrnd1 = nrnd0;
         nrnd0 = prnd0 - 1;
      }
      tmp=null;
   }
}
