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

package cds.tools.pixtools;
import java.io.File;

import cds.aladin.Coord;



/** Divers utilitaires de manipulation Healpix */
public class Util {

   public static final int DIRSIZE = 10000;
   public static final String FS = cds.tools.Util.FS;
   public static final String CR = cds.tools.Util.CR;
   private static double twothird = 2. / 3.;

   /** Retourne le numéro du pixel père (pour l'order précédent) */
   //   static public long getFather(long npix) { return npix/4; }

   /** Retourne les numéros des 4 pixels fils 
    * nota Utiliser pixChild[]!=null pour éviter les allocations */
   //   static public long [] getChildren(long npix) { return getChildren(null,npix); }
   //   static public long [] getChildren(long [] pixChild,long npix) {
   //      if( pixChild==null ) pixChild = new long[4];
   //      pixChild[0]= npix*4;
   //      pixChild[1]= npix*4 +1;
   //      pixChild[2]= npix*4 +2;
   //      pixChild[3]= npix*4 +3;
   //      return pixChild;
   //   }

   static public Coord [] getCorners(int order, long npix) throws Exception {
      return getCorners(null,order,npix);
   }

   static public Coord [] getCorners(Coord [] corners,int order, long npix) throws Exception {
      long nside = CDSHealpix.pow2(order);
      double [][] x = CDSHealpix.corners(nside, npix);
      if( corners==null ) corners = new Coord[4];
      for( int i=0; i<4; i++ ) {
         corners[i] = new Coord(x[i][0],x[i][1]);  
      }
      return corners;
   }
   //   static protected Coord [] getCorners(Coord [] corners,int order, long npix) {
   //      if( corners==null ) corners = new Coord[4];
   //      int orderFile = 20-order;  // Je ne dois pas dépasser la limite Healpix de 2^20
   //      long nSidePix = CDSHealpix.pow2(orderFile);
   //      
   //      // Numéro des pixels des 4 coins
   //      long c0,c1,c2,c3;
   //      c0=c1=c2= 0;
   //      c3 = (nSidePix*nSidePix)-1;
   //      for( int i=0; i<orderFile; i++ ) c1 = (c1 << 2) | 1;
   //      for( int i=0; i<orderFile; i++ ) c2 = (c2 << 2) | 2;
   //      
   //      // Chaque pixel "Fichier" va être remplacé par nsidePix*nsidePix pixels
   //      // d'où l'offset suivant
   //      long offset = npix * nSidePix*nSidePix;
   //      c0+=offset; c1+=offset; c2+=offset; c3+=offset;
   //      
   //      long nSideFile = CDSHealpix.pow2(order+orderFile);
   //      
   //      double x [];
   //      x=PixTools.PolarToRaDec( PixTools.pix2ang_nest(nSideFile, c0) );
   //      corners[0] = new Coord(x[0],x[1]);
   //      x=PixTools.PolarToRaDec( PixTools.pix2ang_nest(nSideFile, c1) );
   //      corners[1] = new Coord(x[0],x[1]);
   //      x=PixTools.PolarToRaDec( PixTools.pix2ang_nest(nSideFile, c2) );
   //      corners[2] = new Coord(x[0],x[1]);
   //      x=PixTools.PolarToRaDec( PixTools.pix2ang_nest(nSideFile, c3) );
   //      corners[3] = new Coord(x[0],x[1]);
   //      
   //      return corners;
   //   }

   /**
    * Donne le chemin d'un fichier selon la base Healpix
    * Ajouter devant le chemin général + derrière l'extension .hpx / .fits / .jpeg
    * @param survey
    * @param order
    * @param npix
    * @param z frame du cube, pour 0 => pas d'extension
    * @return
    */
   static public String getFilePath(String survey,int order, long npix) { return getFilePath(survey,order,npix,0); }
   static public String getFilePath(String survey,int order, long npix, int z) {
      String prefix = survey!=null && survey.length()>0 ? survey : "";
      String suffix = getFilePath(order,npix,z);
      return cds.tools.Util.concatDir(prefix, suffix);
   }

   /**
    * Donne le chemin d'un fichier selon la base Healpix
    * Ajouter devant le chemin général + derrière l'extension .hpx / .fits / .jpeg
    * @param survey
    * @param order
    * @param npix
    * @param z frame du cube, pour 0 => pas d'extension
    * @return
    */
   static public String getFilePath(int order, long npix) { return getFilePath(order,npix,0); }
   static public String getFilePath(int order, long npix,int z) {
      return
      "Norder" + order + "/" +
      "Dir" + ((npix / DIRSIZE)*DIRSIZE) + "/" +
      "Npix" + npix
      + ( z<=0 ? "" : "_"+z);
   }

   static public int getOrderFromPath(String filename) {
      int fromIndex = filename.indexOf("Norder");
      if( fromIndex==-1 ) return -1;
      int order = Integer.parseInt(
            filename.substring(
                  fromIndex+6, 
                  filename.indexOf(Util.FS, fromIndex)
            )
      );
      return order;
   }

   static public long getNpixFromPath(String filename) {
      int fromIndex = filename.lastIndexOf("Npix");
      if( fromIndex<0 ) return -1;
      int lastIndex = filename.indexOf('_',fromIndex);
      if( lastIndex<0 ) lastIndex = filename.indexOf('.',fromIndex);
      if( lastIndex<0 ) lastIndex = filename.length();
      return Long.parseLong( filename.substring(fromIndex+4,lastIndex) );
   }

   static public long getNDirFromPath(String filename) {
      int fromIndex = filename.indexOf("Dir");
      long npix = Long.parseLong(
            filename.substring(
                  fromIndex+3, 
                  filename.indexOf(Util.FS, fromIndex)
            )
      );
      return npix;
   }

   /** retourne le plus grand order d'une distribution Healpix à la CDS
    * en scannant le nom des répertoire Nordernn, -1 si problème */
   static public int getMaxOrderByPath(String path) {
      int maxOrder=-1;
      File f = new File(path);
      File [] sf = f.listFiles();
      if( sf!=null ) {
         for( int i=0; i<sf.length; i++ ) {
            if( !sf[i].isDirectory() ) continue;
            String name = sf[i].getName();
            if( name.startsWith("Norder") ) {
               int n = Integer.parseInt(name.substring(6));
               if( n>maxOrder ) maxOrder=n;
            }
         }
      }
      return maxOrder;
   }

  /**
    * Calcule de facon récursive le nombre de fichier finaux Npix... d'un répertoire
    * @param dir
    * @return le nombre de fichiers
    */
   public static int computeNFiles(File dir) {
      int sum = 0;
      if (dir.isDirectory()) {
         String[] children = dir.list();
         // pour tous les enfants du répertoire
         for (int i=0; i<children.length; i++) {
            // relance le calcul récursivement
            int n = computeNFiles(new File(dir, children[i]));
            sum += n;
         }
      }
      else
         sum++;

      return sum;
   }

   /**
    * Donne le numero de pixel parent de la resolution nside1
    * connaissant un numero de pixel n dans un referenciel de resolution nside2
    * nside1<nside2
    * n begins to 0
    */
   //	static long getHealpixParentFromNside(int nside1, long n, int nside2) {
   //		return (long) Math.ceil( 
   //				(n+1)/
   //					Math.pow(4,(CDSHealpix.log2(nside2) - CDSHealpix.log2(nside1))) 
   //				) -1;
   //	}

   /**
    * Donne le numero de pixel parent de la resolution nside1
    * connaissant un numero de pixel n dans un referenciel de resolution nside2
    * order1<order2
    * n begins to 0
    */
   //	public static long getHealpixParentFromOrder(int order1, long n, int order2) {
   //		return (long) Math.ceil( 
   //				(n+1)/
   //					Math.pow(4,order2 - order1) 
   //				) -1;
   //	}


   /**
    * Donne la liste des numeros de pixels enfants de la resolution order1
    * connaissant un numero de pixel n dans un referenciel de resolution order2
    * order2<order1
    * n begins to 0
    */
   //	public static ArrayList<Long> getHealpixChildrenFromOrder(int order1, long n, int order2) {
   //		
   //		long f = (long) Math.ceil( 
   //				n*Math.pow(4,order2 - order1));
   //		long nb = (long)Math.pow(4,order2 - order1);
   //		ArrayList<Long> r = new ArrayList<Long> ();
   //		
   //		for (long i = f ; i < f+nb ; i++) {
   //			r.add(new Long(i));
   //		}
   //		
   //		return r;
   //	}


   /**
    * Donne le numéro Max du pixel à la résolution nside2 contenu dans le losange
    * donné la résolution nside1
    * nside1 < nside2
    * n begins to 0
    */
//   public static long getHealpixMax(int n1, long n, int n2, boolean nside) {
//      if (nside) return (n+1)*(long)(Math.pow(4,(CDSHealpix.log2(n2) - CDSHealpix.log2(n1))/CDSHealpix.log2(2))) -1;
//      else return (n+1)*(long)(Math.pow(4,(n2 - n1)/CDSHealpix.log2(2))) -1;
//   }

   /**
    * Donne le numéro Min du pixel à la résolution nside2 contenu dans le losange
    * donné la résolution nside1
    * nside1 < nside2
    */
//   public static long getHealpixMin(int n1, long n, int n2, boolean nside) {
//      if (nside) return n*(long)(Math.pow(4,(CDSHealpix.log2(n2) - CDSHealpix.log2(n1))/CDSHealpix.log2(2)));
//      else return n*(long)(Math.pow(4,(n2 - n1)/CDSHealpix.log2(2)));
//   }

   /**
    * Donne le numéro max existant pour une résolution donnée
    * @param order
    * @return
    */
//   public static long getMax(int order) {
//      return (long) Math.pow(4,order) * 12 -1;
//   }


   //    private int [] xy2hpx = null;
   //    private int [] hpx2xy = null;
   /** Méthode récursive utilisée par createHealpixOrder */
   static private void fillUp(int [] npix, int nsize, int [] pos) {
      int size = nsize*nsize;
      int [][] fils = new int[4][size/4];
      int [] nb = new int[4];
      for( int i=0; i<size; i++ ) {
         int dg = (i%nsize) < (nsize/2) ? 0 : 1;
         int bh = i<(size/2) ? 1 : 0;
         int quad = (dg<<1) | bh;
         int j = pos==null ? i : pos[i];
         npix[j] = npix[j]<<2 | quad;
         fils[quad][ nb[quad]++ ] = j;
      }
      if( size>4 )  for( int i=0; i<4; i++ ) fillUp(npix, nsize/2, fils[i]);
   }

   /** Creation des tableaux de correspondance indice Healpix <=> indice XY */
   /*void createHealpixOrder(int order) {
        if (order==0) {
            xy2hpx = hpx2xy = new int[] {0};
            return;
        }

       int nsize = (int)Math.pow(2,order);
       xy2hpx = new int[nsize*nsize];
       hpx2xy = new int[nsize*nsize];
       fillUp(xy2hpx,nsize,null);
       for( int i=0; i<xy2hpx.length; i++ ) hpx2xy[ xy2hpx[i] ] = i;
    }*/

   /**Creation des tableaux de correspondance indice Healpix <=> indice XY */
   static public int[] createHpx2xy(int order) {
      int [] xy2hpx = null;
      int [] hpx2xy = null;

      if (order==0) {
         hpx2xy = new int[] {0};
         return hpx2xy;
      }

      int nsize = (int)CDSHealpix.pow2(order);
      xy2hpx = new int[nsize*nsize];
      hpx2xy = new int[nsize*nsize];
      fillUp(xy2hpx,nsize,null);
      for( int i=0; i<xy2hpx.length; i++ ) hpx2xy[ xy2hpx[i] ] = i;
      return hpx2xy;
   }

   /**Creation des tableaux de correspondance indice Healpix <=> indice XY */
   static public int[] createXy2Hpx(int order) {
      int [] xy2hpx = null;
      if (order==0) {
         xy2hpx = new int[] {0};
         return xy2hpx;
      }

      int nsize = (int)CDSHealpix.pow2(order);
      xy2hpx = new int[nsize*nsize];
      fillUp(xy2hpx,nsize,null);
      return xy2hpx;
   }

   static int[] HPX1024XY = new int[1024*1024+1];
   /*
	static {
		System.out.println("Build static convertion HPX > XY");
		int size = 1024*1024;
		for (int i = 1 ; i <= size ; i++) {
			int[] xy = hpx2XY(i,10);
			// on inverse volontairement X et Y
			HPX1024XY[i] = xy[0]*1024 + xy[1];
		}
	}
    */
   /**
    * Transformation d'un numéro de pixel (de 0 à size-1) ordonné en NESTED
    * vers une position dans un fichier Fits en X,Y
    * Fichiers de taille 1024*1024
    * @param n
    * @return
    */
   public static int[] hpx2XY(int n) {
      int[] xy = new int[2];
      xy[1] = HPX1024XY[n+1] / 1024;
      xy[0] = HPX1024XY[n+1] - xy[1]*1024;
      return xy;
   }
   /**
    * Transformation d'un numéro de pixel (de 1 à size) ordonné en NESTED
    * vers une position dans un fichier Fits en X,Y
    * @param n
    * @param MAX_LEVEL difference de order log2(taille du cote du carré en pixel)
    * @return
    */
   public static int[] hpx2XY(long n, int MAX_LEVEL) {
      double limLo = 0;
      double limHi = Math.pow(2.,MAX_LEVEL)* Math.pow(2.,MAX_LEVEL);//512*512;
      int[] partX = new int[MAX_LEVEL];
      int[] partY = new int[MAX_LEVEL];

      for (int level = MAX_LEVEL-1 ; level >= 0 ; level--)
      {
         limHi=Math.pow(2.,level+1)* Math.pow(2.,level+1);
         if (n <= (limLo +(limHi -limLo)/4) ) {
            partX[level] = 0 ;
            partY[level] = 0 ;
         }
         else if ( n <= (limLo + ((limHi -limLo)/2))) {
            partX [level]= 1 ; 
            partY[level]= 0 ; 
            n=n-(long)Math.pow(4.,level);
         }
         else if (n <= (limLo + 3*(limHi -limLo)/4)) {
            partX [level]= 0  ;   
            partY[level]= 1  ;
            n=n-2*(long)Math.pow(4.,level);
         }
         else if ( n <= limHi ) {
            partX[level]= 1 ;
            partY [level]= 1 ;
            n=n-3*(long)Math.pow(4.,level);
         }
      }
      int[] xy = new int[2];
      xy[0]=0;xy[1]=0;
      //for (int level = 0 ; level < MAX_LEVEL ; level++) {
      //x |= (partX[level]<<level); 
      //y |= (partY[level]<<level); 
      //}
      for (int level = 0 ; level < MAX_LEVEL ; level++) {
         xy[0] += partX[level] * (long)Math.pow(2.,level) ; 
         xy[1] += partY[level] * (long)Math.pow(2.,level) ; 
      }
      return xy;
   }


   public static long XY2Hpx(int x, int y, int MAX_LEVEL) {
      double limLox = 0;
      double limHix = Math.pow(2.,MAX_LEVEL);
      double limLoy = 0;
      double limHiy = Math.pow(2.,MAX_LEVEL);
      int[] N = new int[MAX_LEVEL];
      double limidx,limidy;

      for (int level = MAX_LEVEL-1 ; level >= 0 ; level--)
      {
         limidx = (limLox + limHix)/2;
         limidy = (limLoy + limHiy)/2;
         //
         //   4
         //  2  3
         //    1
         //
         // losange 1
         if (x < limidx && y < limidy) {
            N[level] = 0;
            limHix = limidx;
            limHiy = limidy;
         }
         // losange 2
         else if (x >= limidx && y < limidy) {
            N[level] = 1;
            limLox = limidx;
            limHiy = limidy;
         }
         // losange 3
         else if (x < limidx && y >= limidy) {
            N[level] = 2;
            limLoy = limidy;
            limHix = limidx;
         }
         // losange 4
         else if (x >= limidx && y >= limidy) {
            N[level] = 3;
            limLox = limidx;
            limLoy = limidy;
         }

      }

      long n=0;
      //for (int level = 0 ; level < MAX_LEVEL ; level++) {
      //x |= (partX[level]<<level); 
      //y |= (partY[level]<<level); 
      //}
      for (int level = 0 ; level < MAX_LEVEL ; level++) {
         n += N[level] * (long)Math.pow(4.,level) ; 
      }
      return n;
   }


   /**
    * returns the ring number in {1, 4*nside - 1} calculated from z coordinate
    * 
    * @param nside 
    *            long resolution
    * @param z 
    *            double z coordinate
    * @param floor
    * 				need to around floor or ceil
    * @return long ring number
    */
//   static public long RingNum(long nside, double z, boolean floor) {
//      long iring = 0;
//      /* equatorial region */
//      if (floor)
//         iring = (long) Math.floor(nside * (2.0 - 1.5 * z));
//      else
//         iring = (long) Math.ceil(nside * (2.0 - 1.5 * z));
//      /* north cap */
//      if (z > twothird) {
//         if (floor)
//            iring = (long) Math.floor(nside * Math.sqrt(3.0 * (1.0 - z)));
//         else
//            iring = (long) Math.ceil(nside * Math.sqrt(3.0 * (1.0 - z)));
//         if (iring == 0)
//            iring = 1;
//      }
//      /* south cap */
//      if (z < -twothird) {
//         if (floor)
//            iring = (long) Math.floor(nside * Math.sqrt(3.0 * (1.0 + z)));
//         else
//            iring = (long) Math.ceil(nside * Math.sqrt(3.0 * (1.0 + z)));
//
//         if (iring == 0)
//            iring = 1;
//         iring = 4 * nside - iring;
//      }
//      return iring;
//   }


   /**
    * returns polar coordinates in radians given ra, dec in degrees
    * @param radec double array containing ra,dec in degrees
    * @return res double array containing theta and phi in radians
    *             res[0] = theta res[1] = phi
    */
   //    public static double[] RaDecToPolar(double[] radec) {
   //    	double[] res = {0.0,0.0};
   //    	
   //			double ra =  radec[0];
   //			double dec =  radec[1];
   //			double theta = Math.PI/2. - Math.toRadians(dec);
   //			double phi = Math.toRadians(ra);
   //			res[0] = theta;
   //			res[1] = phi;
   //    	
   //    	return res;
   //    }
   /**
    * returns ra, dec in degrees given polar coordinates in radians
    * @param polar double array polar[0] = theta in radians
    *                           polar[1] = phi in radians
    * @return double array radec radec[0] = ra in degrees
    *                radec[1] = dec in degrees
    */
   //    public static double[] PolarToRaDec(double[] polar) {
   //    	double[] radec = {0.0,0.0};
   //			double phi =  polar[1];
   //			double theta = polar[0];
   //			double dec = Math.toDegrees(Math.PI/2. - theta);
   //			double ra = Math.toDegrees(phi);
   //			radec[0] = ra;
   //			radec[1] = dec;
   //    	
   //    	return radec;
   //    }

   public static final int nside(int order){ return 1<<order;}
   public static final int order(int nside){ int i=0; while((nside>>>(++i))>0); return --i; }
   public static final long nbrPix(int nside){ return 12*(long)nside*nside; }

   /**
    * Returns the index of the healpix pixel of order <i>orderTo</i>
    * containing the pixel of healpix index <i>idx</i> in order <i>orderFrom</i>.
    * @param idx
    * @param orderFrom
    * @param orderTo
    * @return the index of the healpix pixel of order <i>orderTo</i>
    * containing the pixel of healpix index <i>idx</i> in order <i>orderFrom</i>.
    */
   public static final long idx(long idx, int orderFrom, int orderTo){
      if(orderFrom<orderTo) throw new IllegalArgumentException("'orderFrom' must be greatest than 'orderTo'!");
      return idx>>>((orderFrom-orderTo)<<1);
   }

   /**
    * Returns the smallest index in order <i>orderTo</i> of healpix pixels contained by
    * the healpix pixel of index <i>idx</i> in order <i>orderFrom</i>.
    * @param idx
    * @param orderFrom
    * @param orderTo
    * @return the smallest index in order <i>orderTo</i> of healpix pixels contained by
    * the healpix pixel of index <i>idx</i> in order <i>orderFrom</i>.
    */
   public static final int min(int idx, int orderFrom, int orderTo){
      if(orderFrom>orderTo) throw new IllegalArgumentException("'orderFrom' must be smaller than 'orderTo'!");
      return idx<<((orderTo-orderFrom)<<1);
   }

   /**
    * Returns the greatest index in order <i>orderTo</i> of healpix pixels contained by
    * the healpix pixel of index <i>idx</i> in order <i>orderFrom</i>.
    * @param idx
    * @param orderFrom
    * @param orderTo
    * @return the greatest index in order <i>orderTo</i> of healpix pixels contained by
    * the healpix pixel of index <i>idx</i> in order <i>orderFrom</i>.
    */
   public static final int max(int idx, int orderFrom, int orderTo){
      if(orderFrom>orderTo) throw new IllegalArgumentException("'orderFrom' must be smaller than 'orderTo'!");
      return ((++idx)<<((orderTo-orderFrom)<<1))-1;
   }
   
   
   static private final long UN = 1L<<63;
   
   /** Returns the HEALPix NESTed number from x,y array coordinate (shuffle bit algorithm) */
   public static long getHpxNestedNumber(int x, int y) {
      long mask=0x1;
      long res=0L;

      for( int i=0; i<32; i++ ) {
         res >>>= 1;
         if( (mask&x)!=0 ) res |= UN;
          res >>>= 1;
         if( (mask&y)!=0 ) res |= UN;
         mask <<= 1;
      }
      return res;
   }
   
   /** Returns the binary representation of a long integer */
   public static String bits(long a) {
      StringBuilder res = new StringBuilder();
      long mask = 1L<<63;
      for( int i=0; i<64; i++ ) {
         if( (mask&a)!=0 ) res.append('1');
         else res.append('0');
         mask >>>= 1;
      }
      return res.toString();
   }
   

}
