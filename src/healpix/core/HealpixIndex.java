/*
 * HEALPix Java code supported by the Gaia project.
 * Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package healpix.core;

import healpix.core.base.BitManipulation;
import healpix.core.base.HealpixException;
import healpix.core.base.Xyf;
import healpix.core.base.set.LongList;
import healpix.core.base.set.LongRangeIterator;
import healpix.core.base.set.LongRangeSet;
import healpix.core.base.set.LongRangeSetBuilder;
import healpix.core.base.set.LongSet;
import healpix.tools.Constants;
import healpix.tools.SpatialVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Generic healpix routines but tied to a given NSIDE in the constructor Java
 * version of some healpix routines from DSRI in java everthing must be in a
 * class - no functions floating about. Original algorithms Eric Hivon and
 * Krzysztof M. Gorski. This code written by William O'Mullane extended by
 * Emmanuel Joliet with some methods added from pix_tools F90 code port to Java.
 * Performance for 64bits resolution improved using code from Jan Kotek and
 * inspired in PCJ (http://pcj.sourceforge.net/)
 * 
 * @author William O'Mullane, extended by Emmanuel Joliet
 * @version $Id: HealpixIndex.java,v 1.10 2011/06/07 11:36:28 fernique Exp $
 */

public class HealpixIndex implements Serializable {
	/**
	 * Default serial version
	 */
	private static final long serialVersionUID = 2L;
    /**
     * the actual version  from SVN
     */
    public static final String REVISION =
        "$Id: HealpixIndex.java,v 1.10 2011/06/07 11:36:28 fernique Exp $";
	/** The Constant ns_max. */
	public static final int ns_max = 536870912;// 1048576;
	/** Max order 
	public static final int order_max=29;
	
    /*! The order of the map; -1 for nonhierarchical map. */
    protected int order;

	static short ctab[], utab[];

	/** Available nsides ..always poer of 2 ..**/
	public static int[] nsidelist = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048,
			4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288,
			1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 
			67108864, 134217728,  268435456, 536870912 };

	// coordinate of the lowest corner of each face
	int jrll[] = {  2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4 };
	int jpll[] = {  1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7 };

	 static  int xoffset[] = { -1,-1, 0, 1, 1, 1, 0,-1 };
	 static  int yoffset[] = {  0, 1, 1, 1, 0,-1,-1,-1 };
	 static  int facearray[][] =
	        { {  8, 9,10,11,-1,-1,-1,-1,10,11, 8, 9 },   // S
	          {  5, 6, 7, 4, 8, 9,10,11, 9,10,11, 8 },   // SE
	          { -1,-1,-1,-1, 5, 6, 7, 4,-1,-1,-1,-1 },   // E
	          {  4, 5, 6, 7,11, 8, 9,10,11, 8, 9,10 },   // SW
	          {  0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11 },   // center
	          {  1, 2, 3, 0, 0, 1, 2, 3, 5, 6, 7, 4 },   // NE
	          { -1,-1,-1,-1, 7, 4, 5, 6,-1,-1,-1,-1 },   // W
	          {  3, 0, 1, 2, 3, 0, 1, 2, 4, 5, 6, 7 },   // NW
	          {  2, 3, 0, 1,-1,-1,-1,-1, 0, 1, 2, 3 } }; // N
	 static  int swaparray[][] =
	        { {  0,0,0,0,0,0,0,0,3,3,3,3 },   // S
	          {  0,0,0,0,0,0,0,0,6,6,6,6 },   // SE
	          {  0,0,0,0,0,0,0,0,0,0,0,0 },   // E
	          {  0,0,0,0,0,0,0,0,5,5,5,5 },   // SW
	          {  0,0,0,0,0,0,0,0,0,0,0,0 },   // center
	          {  5,5,5,5,0,0,0,0,0,0,0,0 },   // NE
	          {  0,0,0,0,0,0,0,0,0,0,0,0 },   // W
	          {  6,6,6,6,0,0,0,0,0,0,0,0 },   // NW
	          {  3,3,3,3,0,0,0,0,0,0,0,0 } }; // N
	/** The Constant z0. */
	public static final double z0 = Constants.twothird; // 2/3

	/** The nside. */
	public int nside = 1024;

	/** The ncap. */
	protected long  nl2, nl3, nl4, npface, npix, ncap;

	/** The fact2. */
	protected double fact1, fact2;

	/** The bm. */
	transient private BitManipulation bm = new BitManipulation();

	/**
	 * Inits the.
	 */
	protected void init() {
		// tablefiller
		int tabmax=0x100;
		ctab=new short[tabmax];
		utab=new short[tabmax];
		for (int m=0; m<0x100; ++m)
	    {
	    ctab[m] =(short)(
	         (m&0x1 )       | ((m&0x2 ) << 7) | ((m&0x4 ) >> 1) | ((m&0x8 ) << 6)
	      | ((m&0x10) >> 2) | ((m&0x20) << 5) | ((m&0x40) >> 3) | ((m&0x80) << 4));
	    utab[m] = (short)(
	         (m&0x1 )       | ((m&0x2 ) << 1) | ((m&0x4 ) << 2) | ((m&0x8 ) << 3)
	      | ((m&0x10) << 4) | ((m&0x20) << 5) | ((m&0x40) << 6) | ((m&0x80) << 7));
	    }
		// end tablefiller
		nl2 = 2 * nside;
		nl3 = 3 * nside;
		nl4 = 4 * nside;
		npface = (long)nside * (long)nside;
		ncap = 2 * (long)nside * ( (long)nside - 1 );// points in each polar cap, =0 for
		// nside =1
		npix =  12 * npface ;
		fact2 = 4.0 / npix;
		fact1 = (nside << 1) * fact2;
		
		order = nside2order(nside);
	}

	/**
	 * Gets the order from the nside
	 * @param nside
	 * @return order
	 */
	public static int nside2order(int nside) {
		int ord=0;
		assert (nside > 0);
	    if ( ((nside)&(nside-1)) > 0 ) {
	    	return -1;
	    }
	    // ok c++ uses a a log - lookup should be better and
	    // we do not have iog2 in java 
	    // the posiiton in the array of nsides is the order !
		ord = Arrays.binarySearch(nsidelist,nside);
		ord = (int) log2(nside);
		return ord;
	}

	/**
	 * Log base two
	 * @param num
	 * @return log2
	 */
	public static double log2(double num) {
		return (Math.log(num) / Math.log(2));
	}

	/**
	 * Default constructor nside = 1024.
	 */
	public HealpixIndex() {
		init();
	}

	/**
	 * Construct healpix routines tied to a given nside
	 * 
	 * @param nSIDE2
	 *            resolution number
	 * @throws Exception
	 */
	public HealpixIndex(int nSIDE2) throws Exception {
		if ( nSIDE2 > ns_max || nSIDE2 < 1 ) {
			throw new Exception("nsides must be between 1 and " + ns_max);
		}
		this.nside = nSIDE2;
		init();
	}

	/**
	 * renders the pixel number ipix ( scheme as defined for object) 
	 * for a pixel which contains
	 * a point on a sphere at coordinates theta and phi, given the map
	 * resolution parameter nside 
	 * 
	 * @param theta
	 *            angle (along meridian), in [0,Pi], theta=0 : north pole
	 * @param phi
	 *            angle (along parallel), in [0,2*Pi]
	 * @return pixel index number
	 * @throws Exception
	 */
	public long ang2pix_nest(double theta, double phi) throws Exception {
		long ipix;
		double z, za, tt, tp;
		long ifp, ifm;
		long jp, jm;
		int  ntt, face_num, ix, iy;

		if ( phi >= Constants.twopi )
			phi = phi - Constants.twopi;
		if ( phi < 0. )
			phi = phi + Constants.twopi;
		if ( theta > Constants.PI || theta < 0 ) {
			throw new Exception("theta must be between 0 and " + Constants.PI);
		}
		if ( phi > Constants.twopi || phi < 0 ) {
			throw new Exception("phi must be between 0 and " + Constants.twopi);
		}
		// Note exception thrown means method does not get further.

		z = Math.cos(theta);
		za = Math.abs(z);
		tt = phi / Constants.piover2;// in [0,4]

		
		// System.out.println("Za:"+za +" z0:"+z0+" tt:"+tt+" z:"+z+"
		// theta:"+theta+" phi:"+phi);
		if ( za <= z0 ) { // Equatorial region
			// System.out.println("Equatorial !");
			// (the index of edge lines increase when the longitude=phi goes up)
		    double temp1 = nside*(0.5+tt);
		    double temp2 = nside*(z*0.75);

			jp = (long) (temp1 - temp2);
			// ascending edge line index
			jm = (long) (temp1 + temp2);
			// descending edge line index

			// finds the face
			ifp = jp >> order; // in {0,4}
			ifm = jm >> order;
			if ( ifp == ifm ) { // faces 4 to 7
				face_num = (int)( ifp == 4 ?  4 : ifp+4);
			} else {
				if ( ifp < ifm ) { // (half-)faces 0 to 3
					face_num = (int)ifp ;
				} else { // (half-)faces 8 to 11
					face_num = (int) ifm   + 8;
				};
			};

			ix = (int)( jm & (nside -1));
			iy = (int) (nside - ( jp &  (nside -1 )) - 1);
		} else { // polar region, za > 2/3

			ntt = (int) ( tt );
			if ( ntt >= 4 )
				ntt = 3;
			tp = tt - ntt;
			double tmp = nside * Math.sqrt(3.0 * ( 1.0 - za )); 

			// (the index of edge lines increase when distance from the closest
			// pole goes up)
			jp = (long) ( tp * tmp);// line going toward the
			// pole as phi increases
			jm = (long) (( 1.0 - tp ) * tmp); // that one goes
			// away of the closest pole
			jp = Math.min(ns_max - 1, jp); 
			// for points too close to the boundary
			jm = Math.min(ns_max - 1, jm);

			// finds the face and pixel's (x,y)
			if ( z >= 0 ) { // North Pole
				// System.out.println("Polar z>=0 ntt:"+ntt+" tt:"+tt);
				face_num = ntt; // in {0,3}
				ix = (int) (nside - jm - 1);
				iy = (int) (nside - jp - 1);
			} else {
				// System.out.println("Polar z<0 ntt:"+ntt+" tt:"+tt);
				face_num = ntt + 8;// in {8,11}
				ix = (int)jp;
				iy = (int)jm;
			};
		};

		ipix = xyf2nest(ix,iy,face_num);
	
		return ipix;
	}

	protected  long xyf2nest(int ix, int iy, int face_num) {
		  return ((long)(face_num)<<(2*order)) +
		    (   ((long)(utab[ ix     &0xff]))
		      | ((long)(utab[(ix>> 8)&0xff])<<16)
		      | ((long)(utab[(ix>>16)&0xff])<<32)
		      | ((long)(utab[(ix>>24)&0xff])<<48)
		      | ((long)(utab[ iy     &0xff])<<1)
		      | ((long)(utab[(iy>> 8)&0xff])<<17)
		      | ((long)(utab[(iy>>16)&0xff])<<33)
		      | ((long)(utab[(iy>>24)&0xff])<<49) ); 
	}

	/**
	 * Convert from pix number to angle renders theta and phi coordinates of the
	 * nominal pixel center for the pixel number ipix (NESTED scheme) given the
	 * map resolution parameter nside
	 * 
	 * @param ipix
	 *            pixel index number
	 * @return double array of [theta, phi] angles in radians [0,Pi], [0,2*Pi]
	 * @throws Exception
	 */
	public double[] pix2ang_nest(long ipix) throws Exception {

		if ( ipix < 0 || ipix > npix - 1 )
			throw new Exception("ipix out of range");

		long  nr, jp, kshift;
		double z, theta, phi;
		
		Xyf x =    nest2xyf(ipix);

		int ix = x.ix;
		int iy = x.iy;
		int face_num = x.face_num;

		// TODO this c++ bit shift givesa differnt jr to the Healpix Code - why ?
		long jr = ((long)(jrll[face_num ] << order)) -ix -iy -1;
		//int jr =((jrll[face_num + 1]) * nside )- ix -iy - 1;// ring number in

		// ring number in {1,4*nside-1}

		if ( jr < nside ) { // north pole region
			nr = jr;
			z = 1.0 - nr * nr * fact2;
			kshift = 0;
		} else if ( jr > nl3 ) { // south pole region
				nr = nl4 - jr;
				z =  nr * nr * fact2 -1.0;
				kshift = 0;
		} else {
			nr = nside;
			z = ( nl2 - (long)jr ) * fact1;
			kshift = ((long)jr - nside) & 1;
		}
		theta = Math.acos(z);

		// computes the phi coordinate on the sphere, in [0,2Pi]
		jp = ( (long)jpll[face_num] * nr + (long)ix - (long)iy + (long)1 + (long)kshift ) / 2;
		// 'phi' number in the ring in {1,4*nr}
		if ( jp > nl4 )
			jp = jp - nl4;
		if ( jp < 1 )
			jp = jp + nl4;

		phi = ( jp - ( kshift + 1L ) * 0.50 ) * ( Constants.piover2 / nr );

		// if (phi < 0)
		// phi += 2.0 * Math.PI; // phi in [0, 2pi]

		double[] ret = { theta, phi };
		return ret;

	}

	private Xyf nest2xyf(long ipix) {
		
		Xyf ret = new Xyf();
		ret.face_num =(int)( ipix>>(2*order));
		long pix = ipix& (npface-1);
		// need o check the & here - they were unsigned in cpp ...
		int raw = (int)(((pix&0x555500000000L)>>16) 
		             | ((pix&0x5555000000000000L)>>31)
		             | (pix&0x5555)
		             | ((pix&0x55550000)>>15));
		  ret.ix =  ctab[raw&0xff]
		     | (ctab[(raw>>8)&0xff]<<4)
		     | (ctab[(raw>>16)&0xff]<<16)
		     | (ctab[(raw>>24)&0xff]<<20);
		  pix >>= 1;
		  raw = (int)(((pix&0x555500000000L)>>16) 
		             | ((pix&0x5555000000000000L)>>31)
		             | (pix&0x5555)
		             | ((pix&0x55550000)>>15));
		  ret.iy =  ctab[raw&0xff]
		     | (ctab[(raw>>8)&0xff]<<4)
		     | (ctab[(raw>>16)&0xff]<<16)
		     | (ctab[(raw>>24)&0xff]<<20);
		return ret;
	}

	/**
	 * Convert from pix number to angle renders theta and phi coordinates of the
	 * nominal pixel center for the pixel number ipix (RING scheme) given the
	 * map resolution parameter nside
	 * 
	 * @param ipix
	 *            pixel index number
	 * @return double array of [theta, phi] angles in radians [0,Pi], [0,2*Pi]
	 * @throws Exception
	 */
	public double[] pix2ang_ring(long ipix) throws Exception {

		double theta, phi;
		long iring, iphi, ip, ipix1;
		double fodd, hip, fihip;
		// -----------------------------------------------------------------------
		if ( ipix < 0 || ipix > npix - 1 )
			throw new Exception("ipix out of range");

		ipix1 = ipix + 1;// in {1, npix}

		if ( ipix1 <= ncap ) { // North Polar cap -------------

			hip = ipix1 / 2.0;
			fihip = (long) ( hip );
			iring = (long) ( Math.sqrt(hip - Math.sqrt(fihip)) ) + 1L;
			// counted from North pole
			iphi = ipix1 - 2 * iring * ( iring - 1 );

			theta = Math.acos(1.0 - (iring* iring * fact2));
			phi = ( (double) ( iphi ) - 0.50 ) * Constants.PI / ( 2.0 * iring );

		} else {
			if ( ipix < (npix - ncap)  ) { // Equatorial region
				ip = ipix - ncap;
				iring = (long) ( ip / nl4 ) + nside;// counted from North pole
				iphi = (long) ip % nl4 + 1;

				fodd = (((iring+nside)&1)>0) ? 1 : 0.5; 
				// 1 if iring+nside is odd, 1/2 otherwise
				theta = Math.acos(( nl2 - iring ) * fact1);
				phi = ( (double) ( iphi ) - fodd ) * Constants.PI
						/ (double) nl2;
			} else { // South Polar cap -----------------------------------
				ip = npix - ipix;
				iring = (long) (0.5*(1+Math.sqrt(2*ip-1)));
				// counted from South pole
				iphi = 4 * iring + 1 - ( ip - 2 * iring * ( iring - 1 ) );

				theta = Math.acos(-1.0 + Math.pow(iring, 2) * fact2);
				phi = ( (double) ( iphi ) - 0.50 ) * Constants.PI
						/ ( 2.0 * iring );

			}
		};

		double[] ret = { theta, phi };
		return ret;
	}

	/**
	 * renders the pixel number ipix (RING scheme) for a pixel which contains a
	 * point on a sphere at coordinates theta and phi, given the map resolution
	 * parametr nside the computation is made to the highest resolution
	 * available (nside=8192) and then degraded to that required (by integer
	 * division) this doesn't cost more, and it makes sure that the treatement
	 * of round-off will be consistent for every resolution
	 * 
	 * @param theta
	 *            angle (along meridian), in [0,Pi], theta=0 : north pole
	 * @param phi
	 *            angle (along parallel), in [0,2*Pi]
	 * @return pixel index number
	 * @throws Exception
	 */
	public long ang2pix_ring(double theta, double phi) throws Exception {

		if ( nside < 1 || nside > ns_max )
			throw new Exception("nside out of range");
		if ( theta < 0.0 || theta > Constants.PI )
			throw new Exception("theta out of range");

		long ipix;
		long jp, jm, ir, ip;
		double z, za, tt, tp, tmp, temp1, temp2;
		int  kshift;

		// -----------------------------------------------------------------------
		z = Math.cos(theta);
		za = Math.abs(z);
		if ( phi >= Constants.twopi )
			phi = phi - Constants.twopi;
		if ( phi < 0. )
			phi = phi + Constants.twopi;
		tt = phi / Constants.piover2;// in [0,4)

		if ( za <= z0 ) {
		    temp1 = nside*(0.5+tt);
		    temp2 = nside*z*0.75;
		    jp = (long)(temp1-temp2); // index of  ascending edge line
		    jm = (long)(temp1+temp2); // index of descending edge line

			ir = nside + 1 + jp - jm;// in {1,2n+1} (ring number counted from
			// z=2/3)
			kshift = 1 - (int)(ir&1);
	
			ip = (long) ( (jp+jm-(long)nside+(long)kshift+1L)/2L);// in {1,4n}
			ip = ip % nl4;

			ipix = ncap + ( ir - 1 )* nl4 + ip;
			return ipix;

		} 
		tp = tt - (int)( tt );// MOD(tt,1.0)
		tmp = (long)nside * Math.sqrt(3.0 * ( 1.0 - za ));

		jp = (long) (  tp * tmp );// increasing edge line index
		jm = (long) ( ( 1.0 - tp ) * tmp );// decreasing edge line index

		ir = jp + jm + 1L;// ring number counted from the closest pole
		ip = (long) ( tt * ir );// in {1,4*ir})
		ip = ip % (4L * ir);
		if ( z > 0.0 ) {
			ipix = 2L * ir * ( ir - 1L ) + ip;			
		} else {
			ipix = npix - 2L * ir * ( ir + 1L ) + ip;
		}
		return ipix;
	}

	/**
	 * performs conversion from NESTED to RING pixel number
	 * 
	 * @param ipnest
	 *            pixel NEST index number
	 * @return RING pixel index number
	 * @throws Exception
	 */
	public long nest2ring(long ipnest) throws Exception {
		Xyf xyf = nest2xyf(ipnest);
		long ipring = xyf2ring(xyf.ix,xyf.iy,xyf.face_num);
		return ipring;
	}

	private long xyf2ring(int ix, int iy, int face_num) {
		  long jr = ((long)jrll[face_num]*(long)nside) - (long)ix - (long)iy  - 1L;

		  long nr, kshift, n_before;
		  if (jr<(long)nside)
		    {
		    nr = jr;
		    n_before = 2*nr*(nr-1);
		    kshift = 0;
		    }
		  else if (jr > 3*(long)nside)
		    {
		    nr = nl4-jr;
		    n_before = npix - 2*(nr+1)*nr;
		    kshift = 0;
		    }
		  else
		    {
		    nr = (long)nside;
		    n_before = ncap + (jr-(long)nside)*nl4;
		    kshift = (jr-(long)nside)&1;
		    }

		  long jp = ((long)jpll[face_num]*nr + (long)ix - (long)iy + 1L + (long)kshift) / 2L;
		  if (jp>nl4)
		    jp-=nl4;
		  else
		    if (jp<1) jp+=nl4;

		  return n_before + jp - 1L;
		
	}

	/**
	 * performs conversion from RING to NESTED pixel number
	 * 
	 * @param ipring
	 *            pixel RING index number
	 * @return NEST pixel index number
	 * @throws Exception
	 */
	public long ring2nest(long ipring) throws Exception {
		  Xyf xyf=ring2xyf(ipring);
		  return xyf2nest (xyf.ix, xyf.iy, xyf.face_num);
	}

	private Xyf ring2xyf(long pix) {
		Xyf ret = new Xyf();
		long iring, iphi, kshift, nr;


		  if (pix<ncap) // North Polar cap
		    {
		    iring = (long)(0.5*(1+Math.sqrt(1L+2L*pix))); //counted from North pole
		    iphi  = (pix+1) - 2*iring*(iring-1);
		    kshift = 0;
		    nr = iring;
		    ret.face_num=0;
		    long tmp = iphi-1;
		    if (tmp>=(2L*iring))
		      {
		      ret.face_num=2;
		      tmp-=2L*iring;
		      }
		    if (tmp>=iring) ++ret.face_num;
		    }
		  else if (pix<(npix-ncap)) // Equatorial region
		    {
		    long ip = pix - ncap;
		    if (order>=0)
		      {
		      iring = (ip>>(order+2)) + (long)nside; // counted from North pole
		      iphi  = (ip&(nl4-1)) + 1;
		      }
		    else
		      {
		      iring = (ip/(nl4)) + (long)nside; // counted from North pole
		      iphi  = (ip%(nl4)) + 1L;
		      }
		    kshift = (iring+(long)nside)&1;
		    nr = (long)nside;
		    long ire = iring-(long)nside+1;
		    long irm = nl2+2-ire;
		    long ifm, ifp;
		    if (order>=0)
		      {
		      ifm = (iphi - ire/2 + (long)nside -1) >> order;
		      ifp = (iphi - irm/2 + (long)nside -1) >> order;
		      }
		    else
		      {
		      ifm = (iphi - ire/2 + (long)nside -1) / (long)nside;
		      ifp = (iphi - irm/2 + (long)nside -1) / (long)nside;
		      }
		    if (ifp == ifm) // faces 4 to 7
		      ret.face_num = (ifp==4) ? 4 : (int)ifp+4;
		    else if (ifp<ifm) // (half-)faces 0 to 3
		      ret.face_num = (int)ifp;
		    else // (half-)faces 8 to 11
		      ret.face_num = (int)ifm + 8;
		    }
		  else // South Polar cap
		    {
		    long ip = npix - pix;
		    iring = (long)(0.5*(1+Math.sqrt(2L*ip-1L))); //counted from South pole
		    iphi  = 4L*iring + 1 - (ip - 2L*iring*(iring-1L));
		    kshift = 0;
		    nr = iring;
		    iring = 2L*nl2-iring;
		    ret.face_num=8;
		    long tmp = iphi-1L;
		    if (tmp>=(2L*nr))
		      {
		      ret.face_num=10;
		      tmp-=2L*nr;
		      }
		    if (tmp>=nr) ++ret.face_num;
		    }

		  long irt = iring - ((long)jrll[ret.face_num]*(long)nside) + 1L;
		  long ipt = 2L*iphi- (long)jpll[ret.face_num]*nr - kshift -1L;
		  if (ipt>=nl2) ipt-=8L*(long)nside;

		  ret.ix = (int)( (ipt-irt) >>1);
		  ret.iy =(int) ((-(ipt+irt))>>1);
		
		return ret;
	}


	/**
	 * integration limits in cos(theta) for a given ring i_th, i_th > 0
	 * 
	 * @param i_th
	 *            ith ring
	 * @return limits
	 */
	public double[] integration_limits_in_costh(int i_th) {

		double a, ab, b, r_n_side;

		// integration limits in cos(theta) for a given ring i_th
		// i > 0 !!!

		r_n_side = 1.0 * (long)nside;
		if ( i_th <= (long)nside ) {
			ab = 1.0 - ( Math.pow(i_th, 2.0) / 3.0 ) / (double) npface;
			b = 1.0 - ( Math.pow(( i_th - 1 ), 2.0) / 3.0 ) / (double) npface;
			if ( i_th == (long)nside ) {
				a = 2.0 * ( (long)nside - 1.0 ) / 3.0 / r_n_side;
			} else {
				a = 1.0 - Math.pow(( i_th + 1 ), 2) / 3.0 / (double) npface;
			};

		} else {
			if ( i_th < nl3 ) {
				ab = 2.0 * ( 2 * (long)nside - i_th ) / 3.0 / r_n_side;
				b = 2.0 * ( 2 * (long)nside - i_th + 1 ) / 3.0 / r_n_side;
				a = 2.0 * ( 2 * (long)nside - i_th - 1 ) / 3.0 / r_n_side;
			} else {
				if ( i_th == nl3 ) {
					b = 2.0 * ( -(long)nside + 1 ) / 3.0 / r_n_side;
				} else {
					b = -1.0 + Math.pow(( 4 * (long)nside - i_th + 1 ), 2) / 3.0
							/ (double) npface;
				}

				a = -1.0 + Math.pow(( nl4 - i_th - 1 ), 2) / 3.0
						/ (double) npface;
				ab = -1.0 + Math.pow(( nl4 - i_th ), 2) / 3.0 / (double) npface;
			}

		}
		// END integration limits in cos(theta)
		double[] ret = { b, ab, a };
		return ret;
	}

	/**
	 * calculate the points of crossing for a given theata on the boundaries of
	 * the pixel - returns the left and right phi crossings
	 * 
	 * @param i_th
	 *            ith pixel
	 * @param i_phi
	 *            phi angle
	 * @param i_zone
	 *            ith zone (0,...,3), a quarter of sphere
	 * @param cos_theta
	 *            theta cosinus
	 * @return the left and right phi crossings
	 */
	public double[] pixel_boundaries(double i_th, double i_phi, int i_zone,
			double cos_theta) {
		double sq3th, factor, jd, ju, ku, kd, phi_l, phi_r;
		double r_n_side = 1.0 * (long)nside;

		// HALF a pixel away from both poles
		if ( Math.abs(cos_theta) >= 1.0 - 1.0 / 3.0 / (double) npface ) {
			phi_l = i_zone * Constants.piover2;
			phi_r = ( i_zone + 1 ) * Constants.piover2;
			double[] ret = { phi_l, phi_r };
			return ret;
		}
		// -------
		// NORTH POLAR CAP
		if ( 1.50 * cos_theta >= 1.0 ) {
			sq3th = Math.sqrt(3.0 * ( 1.0 - cos_theta ));
			factor = 1.0 / r_n_side / sq3th;
			jd = (double) ( i_phi );
			ju = jd - 1;
			ku = (double) ( i_th - i_phi );
			kd = ku + 1;
			// System.out.println(" cos_theta:"+cos_theta+" sq3th:"+sq3th+"
			// factor:"+factor+" jd:"+jd+" ju:"+ju+" ku:"+ku+" kd:"+kd+ "
			// izone:"+i_zone);
			phi_l = Constants.piover2
					* ( Math.max(( ju * factor ), ( 1.0 - ( kd * factor ) )) + i_zone );
			phi_r = Constants.piover2
					* ( Math.min(( 1.0 - ( ku * factor ) ), ( jd * factor )) + i_zone );

		} else {
			if ( -1.0 < 1.50 * cos_theta ) {
				// -------
				// -------
				// EQUATORIAL ZONE
				double cth34 = 0.50 * ( 1.0 - 1.50 * cos_theta );
				double cth34_1 = cth34 + 1.0;
				int modfactor = (int) ( (long)nside + ( i_th % 2 ) );

				jd = i_phi - ( modfactor - i_th ) / 2.0;
				ju = jd - 1;
				ku = ( modfactor + i_th ) / 2.0 - i_phi;
				kd = ku + 1;

				phi_l = Constants.piover2
						* ( Math.max(( cth34_1 - ( kd / r_n_side ) ),
								( -cth34 + ( ju / r_n_side ) )) + i_zone );

				phi_r = Constants.piover2
						* ( Math.min(( cth34_1 - ( ku / r_n_side ) ),
								( -cth34 + ( jd / r_n_side ) )) + i_zone );
				// -------
				// -------
				// SOUTH POLAR CAP

			} else {
				sq3th = Math.sqrt(3.0 * ( 1.0 + cos_theta ));
				factor = 1.0 / r_n_side / sq3th;
				long ns2 = 2 * (long)nside;

				jd = i_th - ns2 + i_phi;
				ju = jd - 1;
				ku = ns2 - i_phi;
				kd = ku + 1;

				phi_l = Constants.piover2
						* ( Math.max(( 1.0 - ( ns2 - ju ) * factor ),
								( ( ns2 - kd ) * factor )) + i_zone );

				phi_r = Constants.piover2
						* ( Math.min(( 1.0 - ( ns2 - jd ) * factor ),
								( ( ns2 - ku ) * factor )) + i_zone );
			}// of SOUTH POLAR CAP
		}
		// and that's it
		// System.out.println(" nside:"+nside+" i_th:"+i_th+" i_phi:"+i_phi+"
		// izone:"+i_zone+" cos_theta:"+cos_theta+" phi_l:"+phi_l+"
		// phi_r:"+phi_r);

		double[] ret = { phi_l, phi_r };
		return ret;

	}

	/**
	 * return ring number for given pix in ring scheme
	 * 
	 * @param ipix
	 *            pixel index number in ring scheme
	 * @return ring number
	 * @throws Exception
	 */
	public int ring(long ipix) throws Exception {
		int iring = 0;
		long ipix1 = ipix + 1;// in {1, npix}
		int ip;
		double hip, fihip = 0;
		if ( ipix1 <= ncap ) { // North Polar cap -------------
			hip = ipix1 / 2.0;
			fihip = (int) ( hip );
			iring = (int) ( Math.sqrt(hip - Math.sqrt(fihip)) ) + 1;// counted
			// from
			// North
			// pole
		} else {
			if ( ipix1 <= nl2 * ( 5 * (long)nside + 1 ) ) { // Equatorial region
				// ------
				ip = (int)(ipix1 - ncap - 1);
				iring = (int) (( ip / nl4 ) + (long)nside);// counted from North pole
			} else { // South Polar cap -----------------------------------
				ip = (int)(npix - ipix1 + 1L);
				hip = ip / 2.0;
				fihip = (int) ( hip );
				iring = (int) ( Math.sqrt(hip - Math.sqrt(fihip)) ) + 1;// counted
				// from
				// South
				// pole
				iring = (int)(nl4 - iring);
			}
		};
		return iring;
	}

	/**
	 * Construct a {@link SpatialVector} from the angle (theta,phi)
	 * 
	 * @param theta
	 *            angle (along meridian), in [0,Pi], theta=0 : north pole
	 * @param phi
	 *            angle (along parallel), in [0,2*Pi]
	 * @return vector {@link SpatialVector}
	 */
	public static SpatialVector vector(double theta, double phi) {
		double x, y, z;
		x = 1 * Math.sin(theta) * Math.cos(phi);
		y = 1 * Math.sin(theta) * Math.sin(phi);
		z = 1 * Math.cos(theta);
		return new SpatialVector(x, y, z);
	}

	/**
	 * Converts the unit vector to pix number in NEST scheme
	 * 
	 * @param vec
	 *            {@link SpatialVector}
	 * @return pixel index number in nest scheme
	 * @throws Exception
	 */
	public long vec2pix_nest(SpatialVector vec) throws Exception {
		double[] angs = vec2Ang(vec);//ang(vec);
		return ang2pix_nest(angs[0], angs[1]);
//		return ring2nest(vec2pix_ring(vec));
	}
	/**
	 * Converts the unit vector to pix number in RING scheme
	 * 
	 * @param vec
	 *            {@link SpatialVector}
	 * @return pixel index number in ring scheme
	 * @throws Exception
	 */
	public long vec2pix_ring(SpatialVector vec) throws Exception {
		double[] angs = vec2Ang(vec);
		return ang2pix_ring(angs[0], angs[1]);
	}

	/**
	 * Converts pix number in NEST scheme to the unit vector
	 * 
	 * @param pix
	 *            pixel index number in nest scheme
	 * @return {@link SpatialVector}
	 * @throws Exception
	 */
	public SpatialVector pix2vec_nest(long pix) throws Exception {
		double[] angs = pix2ang_nest(pix);
		return vector(angs[0], angs[1]);
	}

	/**
	 * Converts pix number in RING scheme to the unit vector
	 * 
	 * @param pix
	 *            pixel index number in ring scheme
	 * @return {@link SpatialVector}
	 * @throws Exception
	 */
	public SpatialVector pix2vec_ring(long pix) throws Exception {
		double[] angs = pix2ang_ring(pix);
		return vector(angs[0], angs[1]);
	}

	/**
	 * Returns set of points along the boundary of the given pixel in NEST
	 * scheme. Step 1 gives 4 points on the corners.
	 * 
	 * @param pix
	 *            pixel index number in nest scheme
	 * @param step
	 * @return {@link SpatialVector} for each points
	 * @throws Exception
	 */
	public SpatialVector[] corners_nest(long pix, int step) throws Exception {
		long pixr = nest2ring(pix);
		return corners_ring(pixr, step);
	}

	/**
	 * Returns set of points along the boundary of the given pixel in RING
	 * scheme. Step 1 gives 4 points on the corners.
	 * Mainly for graphics = you may not want to use LARGE NSIDEs..
	 * 
	 * @param pix
	 *            pixel index number in ring scheme
	 * @param step
	 * @return {@link SpatialVector} for each points
	 * @throws Exception
	 */
	public SpatialVector[] corners_ring(long pix, int step) throws Exception {
		int nPoints = step * 2 + 2;
		SpatialVector[] points = new SpatialVector[nPoints];
		double[] p0 = pix2ang_ring(pix);
		double cos_theta = Math.cos(p0[0]);
		double theta = p0[0];
		double phi = p0[1];

		int i_zone = (int) ( phi / Constants.piover2 );
		int ringno = ring(pix);
		int i_phi_count = Math.min(ringno, (int)Math.min((long)nside, ( nl4 ) - ringno));
		int i_phi = 0;
		double phifac = Constants.piover2 / i_phi_count;
		if ( ringno >= (long)nside && ringno <= nl3 ) {
			// adjust by 0.5 for odd numbered rings in equatorial since
			// they start out of phase by half phifac.
			i_phi = (int) ( phi / phifac + ( ( ringno % 2 ) / 2.0 ) ) + 1;
		} else {
			i_phi = (int) ( phi / phifac ) + 1;
		}
		// adjust for zone offset
		i_phi = i_phi - ( i_zone * i_phi_count );
		int spoint = (int) ( nPoints / 2 );
		// get north south middle - middle should match theta !
		double[] nms = integration_limits_in_costh(ringno);
		double ntheta = Math.acos(nms[0]);
		double stheta = Math.acos(nms[2]);
		double[] philr = pixel_boundaries(ringno, i_phi, i_zone, nms[0]);
		if ( i_phi > ( i_phi_count / 2 ) ) {
			points[0] = vector(ntheta, philr[1]);
		} else {
			points[0] = vector(ntheta, philr[0]);
		}
		philr = pixel_boundaries(ringno, i_phi, i_zone, nms[2]);
		if ( i_phi > ( i_phi_count / 2 ) ) {
			points[spoint] = vector(stheta, philr[1]);
		} else {
			points[spoint] = vector(stheta, philr[0]);
		}
		if ( step == 1 ) {
			double mtheta = Math.acos(nms[1]);
			philr = pixel_boundaries(ringno, i_phi, i_zone, nms[1]);
			points[1] = vector(mtheta, philr[0]);
			points[3] = vector(mtheta, philr[1]);
		} else {
			double cosThetaLen = nms[2] - nms[0];
			double cosThetaStep = ( cosThetaLen / ( step + 1 ) ); // skip
			// North
			// and south
			for ( int p = 1; p <= step; p++ ) {
				/* Integrate points along the sides */
				cos_theta = nms[0] + ( cosThetaStep * p );
				theta = Math.acos(cos_theta);
				philr = pixel_boundaries(ringno, i_phi, i_zone, cos_theta);
				points[p] = vector(theta, philr[0]);
				points[nPoints - p] = vector(theta, philr[1]);
			}
		}
		return points;
	}

	/**
	 * calculates angular resolution of the pixel map in arc seconds.
	 * 
	 * @param nside
	 * @return double resolution in arcsec
	 */
	static public double getPixRes(long nside) {
		double res = 0.;
		double degrad = Math.toDegrees(1.0);
		double skyArea = 4. * Constants.PI * degrad * degrad; // 4PI steredian
		// in deg^2
		double arcSecArea = skyArea * 3600. * 3600.; // 4PI steredian in
		// (arcSec^2)
		long npixels = 12 * (long)nside * (long)nside;
		res = arcSecArea / npixels; // area per pixel
		res = Math.sqrt(res); // angular size of the pixel arcsec
		return res;
	}

	/**
	 * calculate required nside given pixel size in arcsec
	 * 
	 * @param pixsize
	 *            in arcsec
	 * @return long nside parameter
	 */
	static public int calculateNSide(double pixsize) {
		int res = 0;
		double pixelArea = pixsize * pixsize;
		double degrad = Math.toDegrees(1.);
		double skyArea = 4. * Constants.PI * degrad * degrad * 3600. * 3600.;
		long npixels = (long) ( skyArea / pixelArea );
		long nsidesq = npixels / 12;
		long nside_req = (long) Math.sqrt(nsidesq);
		long mindiff = ns_max;
		int indmin = 0;
		for ( int i = 0; i < nsidelist.length; i++ ) {
			if ( Math.abs(nside_req - nsidelist[i]) <= mindiff ) {
				mindiff = Math.abs(nside_req - nsidelist[i]);
				res = nsidelist[i];
				indmin = i;
			}
			if ( ( nside_req > res ) && ( nside_req < ns_max ) )
				res = nsidelist[indmin + 1];
			if ( nside_req > ns_max ) {
				System.out.println("nside cannot be bigger than " + ns_max);
				return ns_max;
			}

		}
		return res;
	}

	/**
	 * calculates vector corresponding to angles theta (co-latitude measured
	 * from North pole, in [0,pi] radians) phi (longitude measured eastward in
	 * [0,2pi] radians) North pole is (x,y,z) = (0, 0, 1)
	 * 
	 * @param theta
	 *            angle (along meridian), in [0,Pi], theta=0 : north pole
	 * @param phi
	 *            angle (along parallel), in [0,2*Pi]
	 * @return SpatialVector
	 * @throws IllegalArgumentException
	 */
	public static SpatialVector ang2Vec(double theta, double phi) {
		double PI = Math.PI;
		String SID = "Ang2Vec:";
		SpatialVector v;
		if ( ( theta < 0.0 ) || ( theta > PI ) ) {
			throw new IllegalArgumentException(SID
					+ " theta out of range [0.,PI]");
		}
		double stheta = Math.sin(theta);
		double x = stheta * Math.cos(phi);
		double y = stheta * Math.sin(phi);
		double z = Math.cos(theta);
		v = new SpatialVector(x, y, z);
		return v;
	}

	/**
	 * converts a SpatialVector in a tuple of angles tup[0] = theta co-latitude
	 * measured from North pole, in [0,PI] radians, tup[1] = phi longitude
	 * measured eastward, in [0,2PI] radians
	 * 
	 * @param v
	 *            SpatialVector
	 * @return double[] out_tup out_tup[0] = theta out_tup[1] = phi
	 */
	public static double[] vec2Ang(SpatialVector v) {
		double[] out_tup = new double[2];
		double norm = v.length();
		double z = v.z() / norm;
		double theta = Math.acos(z);
		double phi = 0.;
		if ( ( v.x() != 0. ) || ( v.y() != 0 ) ) {
			phi = Math.atan2(v.y(), v.x()); // phi in [-pi,pi]
		}
		if ( phi < 0 )
			phi += 2.0 * Math.PI; // phi in [0, 2pi]
		out_tup[0] = theta;
		out_tup[1] = phi;
		return out_tup;
	}

	/**
	 * returns nside such that npix = 12*nside^2 nside should by power of 2 and
	 * smaller than ns_max if not return -1
	 * 
	 * @param npix
	 *            long the number of pixels in the map
	 * @return nside long the map resolution parameter
	 */
	public static long npix2Nside(long npix) {
		long nside = 0;
		long npixmax = 12 * (long) ns_max * (long) ns_max;
		System.out.println("ns_max=" + ns_max + "  npixmax=" + npixmax);
		String SID = "Npix2Nside:";
		nside = (long) Math.rint(Math.sqrt(npix / 12));
		if ( npix < 12 ) {
			throw new IllegalArgumentException(SID
					+ " npix is too small should be > 12");
		}
		if ( npix > npixmax ) {
			throw new IllegalArgumentException(SID
					+ " npix is too large > 12 * ns_max^2");
		}
		double fnpix = 12.0 * (long)nside * (long)nside;
		if ( Math.abs(fnpix - npix) > 1.0e-2 ) {
			throw new IllegalArgumentException(SID
					+ "  npix is not 12*nside*nside");
		}
		double flog = Math.log((double) nside) / Math.log(2.0);
		double ilog = Math.rint(flog);
		if ( Math.abs(flog - ilog) > 1.0e-6 ) {
			throw new IllegalArgumentException(SID
					+ "  nside is not power of 2");
		}
		return nside;
	}

	/**
	 * calculates npix such that npix = 12*nside^2 nside should be a power of 2,
	 * and smaller than ns_max otherwise return -1
	 * 
	 * @param nside
	 *            long the map resolution
	 * @return npix long the number of pixels in the map
	 */
	public static long nside2Npix(int nside) {

		long res = 0;
		String SID = "Nside2Npix:";
		if ( Arrays.binarySearch(nsidelist, nside) < 0 ) {
			throw new IllegalArgumentException(SID
					+ " nside should be >0, power of 2, <" + ns_max);
		}
		res = 12 * nside * nside;
		return res;
	}

	/**
	 * calculates the surface of spherical triangle defined by vertices v1,v2,v3
	 * Algorithm: finds triangle sides and uses l'Huilier formula to compute
	 * "spherical excess" = surface area of triangle on a sphere of radius one
	 * see, eg Bronshtein, Semendyayev Eq 2.86 half perimeter hp =
	 * 0.5*(side1+side2+side3) l'Huilier formula x0 = tan( hp/2.) x1 = tan((hp -
	 * side1)/2.) x2 = tan((hp - side2)/2.) x3 = tan((hp - side3)/2.)
	 * 
	 * @param v1
	 *            SpatialVector
	 * @param v2
	 *            SpatialVector
	 * @param v3
	 *            SpatialVector vertices of the triangle
	 * @return double the triangle surface in steradians of the spherical
	 *         triangle with vertices vec1, vec2, vec3
	 * @throws Exception
	 */
	public static double surfaceTriangle(SpatialVector v1, SpatialVector v2,
			SpatialVector v3) throws Exception {
		double res = 0.;
		double side1 = angDist(v2, v3) / 4.0;
		double side2 = angDist(v3, v1) / 4.0;
		double side3 = angDist(v1, v2) / 4.0;
		double x0 = Math.tan(side1 + side2 + side3);
		double x1 = Math.tan(side2 + side3 - side1);
		double x2 = Math.tan(side1 + side3 - side2);
		double x3 = Math.tan(side1 + side2 - side3);
		res = 4.0 * Math.atan(Math.sqrt(x0 * x1 * x2 * x3));

		return res;
	}

	/**
	 * calculates angular distance (in radians) between 2 Vectors v1 and v2 In
	 * general dist = acos(v1.v2) except if the vectors are almost aligned
	 * 
	 * @param v1
	 *            SpatialVector
	 * @param v2
	 *            SpatialVector
	 * @return double dist
	 * @throws Exception
	 */
	public static double angDist(SpatialVector v1, SpatialVector v2) throws Exception {
		double dist = 0.;
		double aligned = 0.999;
		/* Normalize both vectors */
		SpatialVector r1 = v1;
		SpatialVector r2 = v2;
		r1.normalized();
		r2.normalized();
		double sprod = r1.dot(r2);
		/* This takes care about the bug in vecmath method from java3d project */
		if ( sprod > aligned ) { // almost aligned
			r1.sub(r2);
			double diff = r1.length();
			dist = 2.0 * Math.asin(diff / 2.0);

		} else if ( sprod < -aligned ) {
			r1.add(r2);
			double diff = r1.length();
			dist = Math.PI - 2.0 * Math.asin(diff / 2.0);
		} else {
			// javax.vecmath.Vector3d r3d1 = new Vector3d(r1.x(), r1.y(),
			// r1.z());
			// javax.vecmath.Vector3d r3d2 = new Vector3d(r2.x(), r2.y(),
			// r2.z());
			dist = Math.acos(sprod);// r3d1.angle(r3d2);
		}
		return dist;
	}

	/**
	 * calculates a dot product (inner product) of two 3D vectors the result is
	 * double
	 * 
	 * @param v1
	 *            3d Vector of Number Objects (Double, long .. )
	 * @param v2
	 *            3d Vector
	 * @return double
	 * @throws Exception
	 */
	public double dotProduct(SpatialVector v1, SpatialVector v2)
			throws Exception {
		double prod = v1.x() * v2.x() + v1.y() * v2.y() + v1.z() * v2.z();

		return prod;
	}

	/**
	 * calculate cross product of two vectors
	 * 
	 * @param v1
	 *            SpatialVector
	 * @param v2
	 *            SpatialVector
	 * @return SpatialVector result of the product
	 */
	public SpatialVector crossProduct(SpatialVector v1, SpatialVector v2) {
		return v1.cross(v2);
	}
	
	/**
	 * generates in the RING or NESTED scheme all pixels that lies within an
	 * angular distance Radius of the center.
	 * 
	 * @param nside
	 *            long map resolution
	 * @param vector
	 *            Vector3d pointing to the disc center
	 * @param radius
	 *            double angular radius of the disk (in RADIAN )
	 * @param nest
	 *            int 0 (default) if output is in RING scheme, if set to 1
	 *            output is in NESTED
	 * @param inclusive
	 *            int 0 (default) only pixsels whose center lie in the triangle
	 *            are listed, if set to 1, all pixels overlapping the triangle
	 *            are listed
	 * @return ArrayList of pixel numbers calls: RingNum(nside, ir)
	 *         InRing(nside, iz, phi0, dphi,nest)
	 */
	public LongRangeSet queryDisc(SpatialVector vector,
			double radius, int nest, int inclusive) {

		String SID = "QUERY_DISC";
		if ( radius < 0.0 || radius > Constants.PI ) {
			throw new IllegalArgumentException(SID
					+ ": angular radius is in RADIAN and should be in [0,pi]");
		}

		LongRangeSetBuilder res = new LongRangeSetBuilder();
		long irmin, irmax, iz;
		double[] ang=null;
		double  z0, radius_eff,theta,phi, cosang, x, ysq;
		double dth1, dth2, dphi;
		double rlat1, rlat2, zmin, zmax, z, xa;
		boolean do_inclusive = false;
		boolean do_nest = false;

		if ( inclusive == 1 )
			do_inclusive = true;
		if ( nest == 1 ) {
			do_nest = true;
		} 

		radius_eff = radius;
		if ( do_inclusive )
			radius_eff += Constants.PI / (double)( nl4 ); // increase radius by
		// half pixel: different in C++ version where a 'magic' number is used.

		// this pix back abnf fourth is ok until you put in  precise vector like a pole .
		// then it shifts the whole elipse...
		ang=vec2Ang(vector);
		
	/**	try {
		   long pix = this.vec2pix_nest(vector);
		   ang=this.pix2ang_nest(pix);
		} catch (Exception e) {
			ang=vec2Ang(vector);
		}**/
		theta=ang[0];
		phi=ang[1];
		dth1 = this.fact2;
		dth2 = this.fact1;
        z0 = Math.cos(theta);
        xa = 1./Math.sqrt((1.0-z0)*(1.0+z0));
        		
		/* coordinate z of highest and lowest points in the disc */
	
		rlat1 = theta - radius_eff;				
		rlat2 = theta + radius_eff;
	

		cosang=Math.cos(radius_eff);
		zmax = Math.cos(rlat1);
		irmin = ringAbove(zmax) +1;
	    zmin = Math.cos(rlat2);	
		irmax = ringAbove(zmin);
		
    	if (irmax < irmin) {// in this case no pixels are returned - need irmax=irmin to loop
			if (irmax==0) {
				irmax=irmin;
			}
		}

		if (rlat1<=0) {// north pole in the disc
		    for (int m=1; m<irmin; ++m) {// rings completely in the disc
		      inRing(m, 0, Math.PI, res);
		    }
		}
	
		/* loop on ring number */
		for ( iz = irmin; iz <= irmax; ++iz ) {
			if ( iz < nside  ) { // north polar cap
				z = 1.0 - (double)iz* (double)iz * dth1;
			} else if ( iz <= (nl3) ) { // tropical band + equator
				z = (double)(nl2 - iz ) * dth2;
			} else {
				z = -1.0 + (double)( nl4 - iz ) * (double)( nl4 - iz ) * dth1;
			}
			/* find phi range in the disc for each z */
			x = (cosang - z * z0)*xa;
			ysq  = 1.0 - z * z - x * x;
			// up north (and south ?) this atan does not work
			// dphi becomes NaN.
			dphi = Math.atan2(Math.sqrt(ysq),x);
			if (Double.isNaN(dphi) ) {
				dphi = radius_eff;
			}
			inRing( iz, phi, dphi, res);				
			
		}
		 if (rlat2>=Math.PI) {// south pole in the disc
		    for (int m=(int)irmax+1; m<(nl4); ++m)  {
		    	// rings completely in the disc
		      inRing(m, 0, Math.PI, res);
		    }
		 }
		 
         LongRangeSet ret= res.build();

	     if (do_nest) {
	         long r=0; 
	         long n=0;
	         int i=0;
		     long[] sorted = new long[(int)ret.size()];
	    	 for (Iterator<Long> iterator = ret.iterator(); iterator.hasNext();) {
	    		 try {
	    			 
	    			 r=iterator.next();
	    			 n=ring2nest(r);
	    		     sorted[i++]=n;
	    		 } catch (Exception e){
	    			 System.err.println(" Failed to convert to nest "+e);
	    		 }
			}
	    	 
	    	Arrays.sort(sorted);
	    	LongRangeSetBuilder lrsb = new LongRangeSetBuilder();
	    	for (int p=0; p < sorted.length; p++){
	    		lrsb.append(sorted[p]);
	    	}
	    	ret = lrsb.build();
	     }
			
		return ret;

	}

	/**
	 * return ring above z value I think - no docs in C++ version..
	 * @param z
	 * @return
	 */
	protected int ringAbove (double z) {
	  double az=Math.abs(z);
	  if (az> Constants.twothird) // polar caps
	    {
	    int iring = (int)(nside*Math.sqrt(3*(1-az)));
	    return (z>0) ? iring : 4*nside-iring-1;
	    }
	  else // ----- equatorial region ---------
	    return (int)((double)nside*(2.0-1.5*z));
	  }

	/**
	 * returns the ring number in {1, 4*nside - 1} calculated from z coordinate
	 * 
	 * @param nside
	 *            long resolution
	 * @param z
	 *            double z coordinate
	 * @return long ring number
	 */
	public long ringNum(int nside, double z) {
		long iring = 0;
		/* equatorial region */

		iring = (long) Math.round(nside * ( 2.0 - 1.5 * z ));
		/* north cap */
		if ( z > Constants.twothird ) {
			iring = (long) Math.round(nside * Math.sqrt(3.0 * ( 1.0 - z )));
			if ( iring == 0 )
				iring = 1;
		}
		/* south cap */
		if ( z < -Constants.twothird ) {
			iring = (long) Math.round(nside * Math.sqrt(3.0 * ( 1.0 + z )));
			if ( iring == 0 )
				iring = 1;
			iring = 4 * nside - iring;
		}
		return iring;
	}

	//	// Left for records an attempt to get nested scheme speed up fails
//	//TODO why?
//	private LongRangeSet ringIterator2nested_longset(int nside, LongRangeIterator iter) throws Exception {
//		LongSet s = new LongSet();
//		setOrder(nside2order(nside));
//		long inext = 0L;
//		while(iter.moveToNext()){
////		for (int i = 0; i < iter.length; i++) {
//			long nestIpix = ring2nest(iter.first());
////			long nestIpix = ring2nest(iter[i].first());
//			for(long ipix = iter.first(); ipix<=iter.last();ipix++){
////				s.add(ring2nest(ipix));				
//				inext = next_in_line_nest(nside, nestIpix);
//				nestIpix = inext;
//				s.add(nestIpix);
//			}
//
//			
//		}
//		return s.toLongRangeSet();
//	}
	
	/**
	 * Method called whenever a nested scheme is needed. This is not as fast as
	 * ring method performance
	 * 
	 * @param nside
	 * @param iter
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private LongRangeSet ringIterator2nested_longset(int nside, LongRangeIterator iter)
			throws Exception {
		LongSet s = new LongSet();
		setOrder(nside2order(nside));
		long inext = 0L;
		while (iter.moveToNext()) {
			long nestIpix = ring2nest(iter.first());
			for (long ipix = iter.first(); ipix <= iter.last(); ipix++) {
				s.add(ring2nest(ipix));
				// inext = next_in_line_nest(nside, nestIpix);
				// nestIpix = inext;
				// s.add(nestIpix);
				// TODO this can be optimized with next_in_line, but it seems to
				// be failing
			}

		}
		return s.toLongRangeSet();
	}
	/**
	 * returns the list of pixels in NEST scheme with latitude in [phi0 -
	 * dpi, phi0 + dphi] on the ring iz in [1, 4*nside -1 ] The pixel id numbers
	 * are in [0, 12*nside^2 - 1] the indexing is in Nest,
	 * @param nside
	 *            long the map resolution
	 * @param iz
	 *            long ring number
	 * @param phi0
	 *            double
	 * @param dphi
	 *            double
	 * @return Long range set
	 * @throws Exception
	 */
	public LongRangeSet inRing_nested_longset(long iz, double phi0,
			double dphi) throws Exception {
		LongRangeIterator iter = inRingLongSet( iz, phi0, dphi).rangeIterator();
		return ringIterator2nested_longset(nside, iter);
	}
	
	/**
	 * returns the list of pixels in RING scheme with latitude in [phi0 -
	 * dpi, phi0 + dphi] on the ring iz in [1, 4*nside -1 ] The pixel id numbers
	 * are in [0, 12*nside^2 - 1] the indexing is in RING
	 * NOTE: this is the f90 code 'in_ring' method ported to java with 'conservative' flag to false
	 * 
	 * @param nside
	 *            long the map resolution
	 * @param iz
	 *            long ring number
	 * @param phi0
	 *            double
	 * @param dphi
	 *            double
	 * @return set result
	 */
	public LongRangeSet inRingLongSet(long iz, double phi0, double dphi) {
		LongRangeSetBuilder b = new LongRangeSetBuilder();
		inRing(iz, phi0, dphi, b);
		return b.build();
	}

	/**
	 * default in ring with conservative false. Sometimes near poles 
	 * conservitave true is better.
	 * @param iz
	 * @param phi0
	 * @param dphi
	 * @param res
	 */
	public void inRing( long iz, double phi0, double dphi,LongRangeSetBuilder res)  {
		inRing(iz, phi0, dphi, res, false);
	}

	/**
	 * returns the list of pixels in RING scheme with latitude in [phi0 -
	 * dpi, phi0 + dphi] on the ring iz in [1, 4*nside -1 ] The pixel id numbers
	 * are in [0, 12*nside^2 - 1] the indexing is in RING, unless nest is set to
	 * 1
	 * NOTE: this is the f90 code 'in_ring' method ported to java with 'conservative' flag to false
	 * 
	 * @param nside
	 *            long the map resolution
	 * @param iz
	 *            long ring number
	 * @param phi0
	 *            double
	 * @param dphi
	 *            double
	 * @param res result
	 */
	public void inRing( long iz, double phi0, double dphi,LongRangeSetBuilder res, boolean conservative)  {
		boolean take_all = false;
		boolean to_top = false;

//		String SID = "InRing:";
		double epsilon = 1e-12;//Double.MIN_VALUE; // the constant to eliminate
		// java calculation jitter
		double shift = 0.;
		long ir = 0;
		long kshift, nr, ipix1, ipix2;//nir1, nir2,
		long ip_low = 0, ip_hi = 0; //,in, nir;
//		long inext;
		
		double phi_low = bm.MODULO(phi0 - dphi, Constants.twopi) - epsilon; // phi min,															  // excluding
																  // 2pi period
//		double phi_low = phi0 - dphi - epsilon; // phi min, 
		// excluding
		double phi_hi = phi0 + dphi + epsilon;
	
		// this was being moduloed but why ?? around the 2pi that casues a problem
		double phi_hi_mod = bm.MODULO(phi0 + dphi, Constants.twopi) + epsilon;

//
		if (Math.abs(dphi - Constants.PI) < epsilon)  take_all = true;
		// what happens when phi_hi wraps round ??

		/* identifies ring number */
		if ((iz >= nside) && (iz <= nl3)) { // equatorial region
			ir = iz - nside + 1; // in [1, 2*nside + 1]
			ipix1 = ncap + nl4 * (ir - 1); // lowest pixel number in the
											     // ring
			ipix2 = ipix1 + nl4 - 1; // highest pixel number in the ring
			kshift = (long) bm.MODULO(ir, 2.);

			nr = nl4;
		} else {
			if (iz < nside) { // north pole
				ir = iz;
				ipix1 = 2 * ir * (ir - 1); // lowest pixel number
				ipix2 = ipix1 + (4 * ir) - 1; // highest pixel number
			} else { // south pole
				ir = 4 * nside - iz;

				ipix1 = npix - 2 * ir * (ir + 1); // lowest pixel number
				ipix2 = ipix1 + 4 * ir - 1;       // highest pixel number
			}
			nr = ir * 4;
			kshift = 1;
		}

		// Construct the pixel list
		if (take_all) {
             res.appendRange(ipix1,ipix2);
			return;
		}

		shift = kshift / 2.0;

		// conservative : include every intersected pixel, even if the
		// pixel center is out of the [phi_low, phi_hi] region
		if (conservative) {
			ip_low = (long) Math.round((nr * phi_low) / Constants.twopi - shift);
			ip_hi = (long) Math.round((nr * phi_hi) / Constants.twopi - shift);

			ip_low = (long) bm.MODULO(ip_low, nr); // in [0, nr - 1]
			if (ip_hi > nr) { // ifit is =nr then this sets it to zero - not good
				ip_hi = (long) bm.MODULO(ip_hi, nr); // in [0, nr - 1]
			}
//			System.out.println("ip_low="+ip_low+" ip_hi="+ip_hi);
		} else { // strict: includes only pixels whose center is in
			//                                                    [phi_low,phi_hi]

			ip_low = (long) Math.ceil(((double)nr * phi_low) / Constants.twopi - shift);
			ip_hi = (long)(((double)nr * phi_hi_mod) / Constants.twopi - shift);
			if (ip_hi < ip_low && iz==1){//this is not good - problem on pole with direction.
			   ip_hi = (long)(((double)nr * phi_hi) / Constants.twopi - shift);				
			}
			if (ip_low == ip_hi + 1)
				ip_low = ip_hi;

			if ((ip_low - ip_hi == 1) && (dphi * (double)nr < Constants.PI)) {
				// the interval is too small ( and away from pixel center)
				// so no pixels is included in the list
				
				System.err.println("the interval is too small and avay from center");
	
				return; // return empty list 
			}

			ip_low = Math.min(ip_low, nr - 1);
			ip_hi = Math.max(ip_hi, 0);
		}

		//
		if (ip_low > ip_hi)
			to_top = true;

		if (to_top) {
			ip_low += ipix1;
			ip_hi += ipix1;
			//nir1 = ipix2 - ip_low + 1;

			//nir2 = ip_hi + 1;

            res.appendRange(ipix1,ip_hi);
            res.appendRange(ip_low,ipix2);    
//            res.addAll(ipix1,ip_hi);
//            res.addAll(ip_low,ipix2);        
		} else {
			if (ip_low < 0 ){
				ip_low = Math.abs(ip_low) ;
				//nir1 = ip_low;
				//nir2 = ip_hi + 1;

                res.appendRange(ipix1, ipix1+ip_hi);
                res.appendRange(ipix2-ip_low +1, ipix2);
//                res.addAll(ipix1, ipix1+ip_hi);
//                res.addAll(ipix2-ip_low +1, ipix2);
				return ;

			}
			ip_low += ipix1;
			ip_hi += ipix1;

            res.appendRange(ip_low,ip_hi);
//            res.addAll(ip_low,ip_hi);
		}
	}

	/**
	 * calculates the pixel that lies on the East side (and the same latitude)
	 * as the given NESTED pixel number - ipix
	 * 
	 * @param nside
	 *            long resolution
	 * @param ipix
	 *            long pixel number
	 * @return long next pixel in line
	 * @throws Exception
	 * @throws IllegalArgumentException
	 */
	public long next_in_line_nest(long nside, long ipix) throws Exception {
		long npix, ipf, ipo, ix, ixp, iy, iym, ixo, iyo, face_num, other_face;
		@SuppressWarnings("unused")
		long ia, ib, ibp, ibm, ib2, nsidesq;
		int icase;
		long local_magic1, local_magic2;
		long[] ixiy = new long[2];
		long inext = 0; // next in line pixel in Nest scheme
		String SID = "next_in_line:";
		if ( ( nside < 1 ) || ( nside > ns_max ) ) {
			throw new IllegalArgumentException(SID
					+ " nside should be power of 2 >0 and < " + ns_max);
		}
		nsidesq = (long)nside * (long)nside;
		npix = 12 * nsidesq; // total number of pixels
		if ( ( ipix < 0 ) || ( ipix > npix - 1 ) ) {
			throw new IllegalArgumentException(SID
					+ " ipix out of range defined by nside");
		}
		// initiates array for (x,y) -> pixel number -> (x,y) mapping

		local_magic1 = ( nsidesq - 1 ) / 3;
		local_magic2 = 2 * local_magic1;
		ipf = (long) bm.MODULO(ipix, nsidesq); // Pixel number in face
		Xyf xyf = nest2xyf(ipix);
		ixiy[0] = xyf.ix;
		ixiy[1] = xyf.iy;
		face_num = xyf.face_num;
		ix = ixiy[0];
		iy = ixiy[1];
		ixp = ix + 1;
		iym = iy - 1;
		boolean sel = false;
		icase = -1; // iside the nest flag
		// Exclude corners
		if ( ipf == local_magic2 ) { // west corner
			inext = ipix - 1;
			return inext;
		}
		if ( ( ipf == nsidesq - 1 ) && !sel ) { // North corner
			icase = 6;
			sel = true;
		}
		if ( ( ipf == 0 ) && !sel ) { // Siuth corner
			icase = 7;
			sel = true;
		}
		if ( ( ipf == local_magic1 ) && !sel ) { // East corner
			icase = 8;
			sel = true;
		}
		// Detect edges
		if ( ( ( ipf & local_magic1 ) == local_magic1 ) && !sel ) { // North-East
			icase = 1;
			sel = true;
		}
		if ( ( ( ipf & local_magic2 ) == 0 ) && !sel ) { // South-East
			icase = 4;
			sel = true;
		}
		if ( !sel ) { // iside a face
			inext = xyf2nest((int) ixp, (int) iym, (int) face_num);
			return inext;
		}
		//
		ia = face_num / 4; // in [0,2]
		ib = (long) bm.MODULO(face_num, 4); // in [0,3]
		ibp = (long) bm.MODULO(ib + 1, 4);
		ibm = (long) bm.MODULO(ib + 4 - 1, 4);
		ib2 = (long) bm.MODULO(ib + 2, 4);

		if ( ia == 0 ) { // North pole region
			switch ( icase ) {
				case 1:
					other_face = 0 + ibp;
					ipo = (long) bm.MODULO(bm.swapLSBMSB(ipf), nsidesq);
					inext = other_face * nsidesq + ipo;
					break;
				case 4:
					other_face = 4 + ibp;
					ipo = (long) bm.MODULO(bm.invMSB(ipf), nsidesq); // SE-NW
					// flip
					xyf = nest2xyf(ipo);
					ixiy[0] = xyf.ix;
					ixiy[1] = xyf.iy;
					ixo = ixiy[0];
					iyo = ixiy[1];

					inext = xyf2nest((int) ixo + 1, (int) iyo,
							(int) other_face);

					break;
				case 6: // North corner
					other_face = 0 + ibp;
					inext = other_face * nsidesq + nsidesq - 1;
					break;
				case 7:
					other_face = 4 + ibp;
					inext = other_face * nsidesq + local_magic2 + 1;
					break;
				case 8:
					other_face = 0 + ibp;
					inext = other_face * nsidesq + local_magic2;
					break;
			}

		} else if ( ia == 1 ) { // Equatorial region
			switch ( icase ) {
				case 1: // NorthEast edge
					other_face = 0 + ib;
					// System.out.println("ipf="+ipf+" nsidesq="+nsidesq+"
					// invLSB="+bm.invLSB(ipf));
					ipo = (long) bm.MODULO((double) bm.invLSB(ipf),
							(double) nsidesq); // NE-SW flip
					// System.out.println(" ipo="+ipo);

					xyf = nest2xyf (ipo);
					ixiy[0] = xyf.ix;
					ixiy[1] = xyf.iy;

					ixo = ixiy[0];
					iyo = ixiy[1];
					inext = xyf2nest((int) ixo, (int) iyo - 1,
							(int) other_face);
					break;
				case 4: // SouthEast edge
					other_face = 8 + ib;
					ipo = (long) bm.MODULO(bm.invMSB(ipf), nsidesq);
					xyf=nest2xyf(ipo);
					ixiy[0] = xyf.ix;
					ixiy[1] = xyf.iy;

					inext = xyf2nest((int) ixiy[0] + 1, (int) ixiy[1],
							(int) other_face);
					break;
				case 6: // Northy corner
					other_face = 0 + ib;
					inext = other_face * nsidesq + local_magic2 - 2;
					break;
				case 7: // South corner
					other_face = 8 + ib;
					inext = other_face * nsidesq + local_magic2 + 1;
					break;
				case 8: // East corner
					other_face = 4 + ibp;
					inext = other_face * nsidesq + local_magic2;
					break;

			}
		} else { // South pole region
			switch ( icase ) {
				case 1: // NorthEast edge
					other_face = 4 + ibp;
					ipo = (long) bm.MODULO(bm.invLSB(ipf), nsidesq); // NE-SW
					// flip
					xyf=nest2xyf(ipo);
					ixiy[0] = xyf.ix;
					ixiy[1] = xyf.iy;
					inext = xyf2nest((int) ixiy[0], (int) ixiy[1] - 1,
							(int) other_face);
					break;
				case 4: // SouthEast edge
					other_face = 8 + ibp;
					ipo = (long) bm.MODULO(bm.swapLSBMSB(ipf), nsidesq); // E-W
					// flip
					inext = other_face * nsidesq + ipo; // (8)
					break;
				case 6: // North corner
					other_face = 4 + ibp;
					inext = other_face * nsidesq + local_magic2 - 2;
					break;
				case 7: // South corner
					other_face = 8 + ibp;
					inext = other_face * nsidesq;
					break;
				case 8: // East corner
					other_face = 8 + ibp;
					inext = other_face * nsidesq + local_magic2;
					break;
			}
		}
		return inext;
	}

	/**
	 * finds pixels that lie within a CONVEX polygon defined by its vertex on
	 * sphere
	 * 
	 * @param nside
	 *            the map resolution
	 * @param vlist
	 *            ArrayList of vectors defining the polygon vertices
	 * @param nest
	 *            if set to 1 use NESTED scheme
	 * @param inclusive
	 *            if set 1 returns all pixels crossed by polygon boundaries
	 * @return ArrayList of pixels algorithm: the polygon is divided into
	 *         triangles vertex 0 belongs to all triangles
	 * @throws Exception
	 */
	public LongRangeSet query_polygon(int nside, ArrayList<Object> vlist,
			long nest, long inclusive) throws Exception {
//		ArrayList<Long> res = new ArrayList<Long>();
		LongSet res = new LongSet();
		int nv = vlist.size();
		healpix.tools.SpatialVector vp0, vp1, vp2;
		healpix.tools.SpatialVector vo;
		LongList vvlist = new LongList();
//		LongRangeSetBuilder vvlist = new LongRangeSetBuilder();
		double hand;
		double[] ss = new double[nv];
		@SuppressWarnings("unused")
		long npix;
		int ix = 0;

		int n_remain, np, nm, nlow;
		String SID = "QUERY_POLYGON";

		// Start polygon
		for ( int k = 0; k < nv; k++ )
			ss[k] = 0.;
		/* -------------------------------------- */
		n_remain = nv;
		if ( n_remain < 3 ) {
			throw new IllegalArgumentException(SID
					+ " Number of vertices should be >= 3");
		}
		/*---------------------------------------------------------------- */
		/* Check that the poligon is convex or has only one concave vertex */
		/*---------------------------------------------------------------- */
		int i0 = 0;
		int i2 = 0;
		if ( n_remain > 3 ) { // a triangle is always convex
			for ( int i1 = 1; i1 <= n_remain - 1; i1++ ) { // in [0,n_remain-1]
				i0 = (int) bm.MODULO(i1 - 1, n_remain);
				i2 = (int) bm.MODULO(i1 + 1, n_remain);
				vp0 = (SpatialVector) vlist.get(i0); // select vertices by 3
				// neighbour
				vp1 = (SpatialVector) vlist.get(i1);
				vp2 = (SpatialVector) vlist.get(i2);
				// computes handedness (v0 x v2) . v1 for each vertex v1
				vo = vp0.cross(vp2);
				hand = dotProduct(vo, vp1);
				if ( hand >= 0. ) {
					ss[i1] = 1.0;
				} else {
					ss[i1] = -1.0;
				}

			}
			np = 0; // number of vert. with positive handedness
			for ( int i = 0; i < nv; i++ ) {
				if ( ss[i] > 0. )
					np++;
			}
			nm = n_remain - np;

			nlow = Math.min(np, nm);

			if ( nlow != 0 ) {
				if ( nlow == 1 ) { // only one concave vertex
					if ( np == 1 ) { // ix index of the vertex in the list
						for ( int k = 0; k < nv - 1; k++ ) {
							if ( Math.abs(ss[k] - 1.0) <= 1.e-12 ) {
								ix = k;
								break;
							}
						}
					} else {
						for ( int k = 0; k < nv - 1; k++ ) {
							if ( Math.abs(ss[k] + 1.0) <= 1.e-12 ) {
								ix = k;
								break;
							}
						}
					}

					// rotate pixel list to put that vertex in #0
					int n_rot = vlist.size() - ix;
					int ilast = vlist.size() - 1;
					for ( int k = 0; k < n_rot; k++ ) {
						SpatialVector temp = (SpatialVector) vlist.get(ilast);
						vlist.remove(ilast);
						vlist.add(0, (Object) temp);
					}
				}
				if ( nlow > 1 ) { // more than 1concave vertex
					System.out
							.println(" The polygon has more than one concave vertex");
					System.out.println(" The result is unpredictable");
				}
			}
		}
		/* fill the poligon, one triangle at a time */
		npix = (long) nside2Npix(nside);
		while ( n_remain >= 3 ) {
			vp0 = (SpatialVector) vlist.get(0);
			vp1 = (SpatialVector) vlist.get(n_remain - 2);
			vp2 = (SpatialVector) vlist.get(n_remain - 1);

			/* find pixels within the triangle */
			LongRangeSet templist = query_triangle(nside, vp0, vp1, vp2, nest, inclusive);

			vvlist.addAll(templist.longIterator());
			n_remain--;
		}
		/* make final pixel list */
//		npix = vvlist.size();
//		long[] pixels = new long[(int) npix];
//		for ( int i = 0; i < npix; i++ ) {
//			pixels[i] = vvlist.get(i).longValue();
//		}
//		Arrays.sort(pixels);
//		int k = 0;
//		res.add(k, new Long(pixels[0]));
//		for ( int i = 1; i < pixels.length; i++ ) {
//			if ( pixels[i] > pixels[i - 1] ) {
//				k++;
//				res.add(k, new Long(pixels[i]));
//			}
//		}

//		return res;
		res.addAll(vvlist);
		return res.toLongRangeSet();
	}
	
	/**
	 * Prints the vec.
	 * 
	 * @param vec the vec
	 */
	public void printVec(double[] vec) {
		System.out.print("[");
		for ( int i = 0; i < vec.length; i++ ) {
			System.out.print(vec[i] + " ");
		}
		System.out.println("]");
	}
	/**
	 * generates a list of pixels that lie inside a triangle defined by the
	 * three vertex vectors
	 * 
	 * @param nside
	 *            long map resolution parameter
	 * @param v1
	 *            Vector3d defines one vertex of the triangle
	 * @param v2
	 *            Vector3d another vertex
	 * @param v3
	 *            Vector3d yet another one
	 * @param nest
	 *            long 0 (default) RING numbering scheme, if set to 1 the NESTED
	 *            scheme will be used.
	 * @param inclusive
	 *            long 0 (default) only pixels whose centers are inside the
	 *            triangle will be listed, if set to 1 all pixels overlaping the
	 *            triangle will be listed
	 * @return ArrayList with pixel numbers
	 * @throws Exception
	 *             if the triangle is degenerated
	 */
	public LongRangeSet query_triangle(int nside, SpatialVector v1,
			SpatialVector v2, SpatialVector v3, long nest, long inclusive)
			throws Exception {
//		ArrayList<Long> res;
//		res = new ArrayList<Long>();
//		LongRangeSet res = new LongRangeSet();
		LongSet res = new LongSet();
		@SuppressWarnings("unused")
		ArrayList<Long> listir;
		long npix, iz, irmin, irmax, n12, n123a, n123b, ndom = 0;
		boolean test1, test2, test3;
		double dth1, dth2, determ, sdet;
		double zmax, zmin, z1max, z1min, z2max, z2min, z3max, z3min;
		double z, tgth, st, offset, sin_off;
		double phi_pos, phi_neg;
		SpatialVector[] vv = new SpatialVector[3];
		SpatialVector[] vo = new SpatialVector[3];
		double[] sprod = new double[3];
		double[] sto = new double[3];
		double[] phi0i = new double[3];
		double[] tgthi = new double[3];
		double[] dc = new double[3];
		double[][] dom = new double[3][2];
		double[] dom12 = new double[4];
		double[] dom123a = new double[4];
		double[] dom123b = new double[4];
		double[] alldom = new double[6];
		double a_i, b_i, phi0, dphiring;
		long idom;
		boolean do_inclusive = false;
		boolean do_nest = false;
		String SID = "QUERY_TRIANGLE";
		long nsidesq = nside * nside;
		/*                                       */

		// System.out.println("in query_triangle");
		npix = nside2Npix(nside);
		if ( npix < 0 ) {
			throw new IllegalArgumentException(SID
					+ " Nside should be power of 2 >0 and < " + ns_max);
		}
		if ( inclusive == 1 )
			do_inclusive = true;
		if ( nest == 1 )
			do_nest = true;
		vv[0] = new SpatialVector(v1);
//		vv[0].normalize();
		vv[1] = new SpatialVector(v2);
//		vv[1].normalize();
		vv[2] = new SpatialVector(v3);
//		vv[2].normalize();
//		printVec(vv[0].get());
//		printVec(vv[1].get());
//		printVec(vv[2].get());
		/*                                  */
		dth1 = 1.0 / ( 3.0 * nsidesq );
		dth2 = 2.0 / ( 3.0 * nside );
		/*
		 * determ = (v1 X v2) . v3 determines the left ( <0) or right (>0)
		 * handedness of the triangle
		 */
		SpatialVector vt = new SpatialVector(0., 0., 0.);
		vt = crossProduct(vv[0], vv[1]);
		determ = dotProduct(vt, vv[2]);

		if ( Math.abs(determ) < 1.0e-20 ) {
			throw new HealpixException(
					SID
							+ ": the triangle is degenerated - query cannot be performed");
		}
		if ( determ >= 0. ) { // The sign of determ
			sdet = 1.0;
		} else {
			sdet = -1.0;
		}

		sprod[0] = dotProduct(vv[1], vv[2]);
		sprod[1] = dotProduct(vv[2], vv[0]);
		sprod[2] = dotProduct(vv[0], vv[1]);
		/* vector ortogonal to the great circle containing the vertex doublet */

		vo[0] = crossProduct(vv[1], vv[2]);
		vo[1] = crossProduct(vv[2], vv[0]);
		vo[2] = crossProduct(vv[0], vv[1]);
		vo[0].normalized();
		vo[1].normalized();
		vo[2].normalized();
//		System.out.println("Orthogonal vectors:");
		
//		printVec(vo[0].get());
//		printVec(vo[1].get());
//		printVec(vo[2].get());
		/* test presence of poles in the triangle */
		zmax = -1.0;
		zmin = 1.0;
		test1 = ( vo[0].z() * sdet >= 0.0 ); // north pole in hemisphere
		// defined
		// by
		// 2-3
		test2 = ( vo[1].z() * sdet >= 0.0 ); // north pole in the hemisphere
		// defined
		// by 1-2
		test3 = ( vo[2].z() * sdet >= 0.0 ); // north pole in hemisphere
		// defined
		// by
		// 1-3
		if ( test1 && test2 && test3 )
			zmax = 1.0; // north pole in the triangle
		if ( ( !test1 ) && ( !test2 ) && ( !test3 ) )
			zmin = -1.0; // south pole in the triangle
		/* look for northenest and southernest points in the triangle */
		// ! look for northernest and southernest points in the triangle
		// ! node(1,2) = vector of norm=1, in the plane defined by (1,2) and
		// with z=0
		
		 boolean test1a = ((vv[2].z() - sprod[0] * vv[1].z()) >= 0.0); //
		 boolean test1b = ((vv[1].z() - sprod[0] * vv[2].z()) >= 0.0);
		 boolean test2a = ((vv[2].z() - sprod[1] * vv[0].z()) >= 0.0); //
		 boolean test2b = ((vv[0].z() - sprod[1] * vv[2].z()) >= 0.0);
		 boolean test3a = ((vv[1].z() - sprod[2] * vv[0].z()) >= 0.0); //
		 boolean test3b = ((vv[0].z() - sprod[2] * vv[1].z()) >= 0.0);
		/* sin of theta for orthogonal vector */
		for ( int i = 0; i < 3; i++ ) {
			sto[i] = Math.sqrt(( 1.0 - vo[i].z() ) * ( 1.0 + vo[i].z() ));
		}
		/*
		 * for each segment ( side of the triangle ) the extrema are either -
		 * -the 2 vertices 
		 * - one of the vertices and a point within the segment
		 */
		z1max = vv[1].z();
		z1min = vv[2].z();
		double zz;
//		segment 2-3
		if ( test1a == test1b ) {
			zz = sto[0];
			if ( ( vv[1].z() + vv[2].z() ) >= 0.0 ) {
				z1max = zz;
			} else {
				z1min = -zz;
			}
		}
		// segment 1-3
		z2max = vv[2].z();
		z2min = vv[0].z();
		if ( test2a == test2b ) {
			zz = sto[1];
			if ( ( vv[0].z() + vv[2].z() ) >= 0.0 ) {
				z2max = zz;
			} else {
				z2min = -zz;
			}
		}
		// segment 1-2
		z3max = vv[0].z();
		z3min = vv[1].z();
		if ( test3a == test3b ) {
			zz = sto[2];
			if ( ( vv[0].z() + vv[1].z() ) >= 0.0 ) {
				z3max = zz;
			} else {
				z3min = -zz;
			}
		}

		zmax = Math.max(Math.max(z1max, z2max), Math.max(z3max, zmax));
		zmin = Math.min(Math.min(z1min, z2min), Math.min(z3min, zmin));
		/*
		 * if we are inclusive, move upper point up, and lower point down, by a
		 * half pixel size
		 */
		offset = 0.0;
		sin_off = 0.0;
		if ( do_inclusive ) {
			offset = Constants.PI / ( this.nl4 ); // half pixel size
			sin_off = Math.sin(offset);
			zmax = Math.min(1.0, Math.cos(Math.acos(zmax) - offset));
			zmin = Math.max(-1.0, Math.cos(Math.acos(zmin) + offset));
		}

		irmin = ringNum(nside, zmax);
		irmax = ringNum(nside, zmin);

//		System.out.println("irmin = " + irmin + " irmax =" + irmax);

		/* loop on the rings */
		for ( int i = 0; i < 3; i++ ) {
			tgthi[i] = -1.0e30 * vo[i].z();
			phi0i[i] = 0.0;
		}
		for ( int j = 0; j < 3; j++ ) {
			if ( sto[j] > 1.0e-10 ) {
				tgthi[j] = -vo[j].z() / sto[j]; // - cotan(theta_orth)

				phi0i[j] = Math.atan2(vo[j].y(), vo[j].x()); // Should make
				// it
				// 0-2pi
				// ?
				/* Bring the phi0i to the [0,2pi] domain if need */

				 if ( phi0i[j] < 0.) {
					phi0i[j] = bm
							.MODULO(
									( Math.atan2(vo[j].y(), vo[j].x()) + Constants.twopi ),
									Constants.twopi); // [0-2pi]
				}
//				System.out.println("phi0i = " + phi0i[j] + " tgthi = "
//						+ tgthi[j]);
			}
		}
		//MOD(ATAN2(X,Y) + TWOPI, TWOPI) : ATAN2 in 0-2pi
		/*
		 * the triangle boundaries are geodesics: intersection of the sphere
		 * with plans going through (0,0,0) if we are inclusive, the boundaries
		 * are the intersection of the sphere with plains pushed outward by
		 * sin(offset)
		 */
		boolean found = false;
		for ( iz = irmin; iz <= irmax; iz++ ) {
			found = false;
			if ( iz <= nside - 1 ) { // North polar cap
				z = 1.0 - iz * iz * dth1;
			} else if ( iz <= 3 * nside ) { // tropical band + equator
				z = ( 2.0 * nside - iz ) * dth2;
			} else {
				z = -1.0 + ( 4.0 * nside - iz ) * ( 4.0 * nside - iz ) * dth1;
			}

			/* computes the 3 intervals described by the 3 great circles */
			st = Math.sqrt(( 1.0 - z ) * ( 1.0 + z ));
			tgth = z / st; // cotan(theta_ring)
			for ( int j = 0; j < 3; j++ ) {
				dc[j] = tgthi[j] * tgth - sdet * sin_off
						/ ( ( sto[j] + 1.0e-30 ) * st ) ;

			}
			for ( int k = 0; k < 3; k++ ) {
				if ( dc[k] * sdet <= -1.0 ) { // the whole iso-latitude ring
					// is on
					// right side of the great circle
					dom[k][0] = 0.0;
					dom[k][1] = Constants.twopi;
				} else if ( dc[k] * sdet >= 1.0 ) { // all on the wrong side
					dom[k][0] = -1.000001 * ( k + 1 );
					dom[k][1] = -1.0 * ( k + 1 );
				} else { // some is good some is bad
					phi_neg = phi0i[k] - ( Math.acos(dc[k]) * sdet );
					phi_pos = phi0i[k] + ( Math.acos(dc[k]) * sdet );
					//					
					 if ( phi_pos < 0. )
						phi_pos += Constants.twopi;
					if ( phi_neg < 0. )
						phi_neg += Constants.twopi;
					//
					dom[k][0] = bm.MODULO(phi_neg, Constants.twopi);
					dom[k][1] = bm.MODULO(phi_pos, Constants.twopi);
//					double domk0 = (phi0i[k] - ( Math.acos(dc[k]) * sdet )) % Constants.twopi;
//					double domk1 = (phi0i[k] + ( Math.acos(dc[k]) * sdet )) %  Constants.twopi;
				}
//				System.out.println("dom["+k+"][0] = " + dom[k][0] + " [1]= "
//						+ dom[k][1]);
				//

			}
			/* identify the intersections (0,1,2 or 3) of the 3 intervals */

			dom12 = intrs_intrv(dom[0], dom[1]);
			n12 = dom12.length / 2;
			if ( n12 != 0 ) {
				if ( n12 == 1 ) {
					dom123a = intrs_intrv(dom[2], dom12);
					n123a = dom123a.length / 2;

					if ( n123a == 0 )
						found = true;
					if ( !found ) {
						for ( int l = 0; l < dom123a.length; l++ ) {
							alldom[l] = dom123a[l];
						}

						ndom = n123a; // 1 or 2
					}
				}
				if ( !found ) {
					if ( n12 == 2 ) {
						double[] tmp = { dom12[0], dom12[1] };
						dom123a = intrs_intrv(dom[2], tmp);
						double[] tmp1 = { dom12[2], dom12[3] };
						dom123b = intrs_intrv(dom[2], tmp1);
						n123a = dom123a.length / 2;
						n123b = dom123b.length / 2;
						ndom = n123a + n123b; // 0, 1, 2 or 3

						if ( ndom == 0 )
							found = true;
						if ( !found ) {
							if ( n123a != 0 ) {
								for ( int l = 0; l < 2 * n123a; l++ ) {
									alldom[l] = dom123a[l];
								}
							}
							if ( n123b != 0 ) {
								for ( int l = 0; l < 2 * n123b; l++ ) {
									alldom[(int) ( l + 2 * n123a )] = dom123b[l];
								}
							}
							if ( ndom > 3 ) {
								throw new HealpixException(SID
										+ ": too many intervals found");
							}
						}
					}
				}
				if ( !found ) {
					for ( idom = 0; idom < ndom; idom++ ) {
						a_i = alldom[(int) ( 2 * idom )];
						b_i = alldom[(int) ( 2 * idom + 1 )];
						phi0 = ( a_i + b_i ) * 0.5;
						dphiring = (b_i - a_i) * 0.5;
						if ( dphiring < 0.0 ) {
							phi0 += Constants.PI;
							dphiring += Constants.PI;
						}
						/* finds pixels in the triangle on that ring */
//						listir = inRing( iz, phi0, dphiring, do_nest);
//						ArrayList<Long> listir2 = InRing(nside, iz, phi0, dphiring, do_nest);						
//						res.addAll(listir);
						LongRangeSet listir2;
						if(do_nest){
							listir2 = inRing_nested_longset( iz, phi0, dphiring);
						}else{
							listir2 = inRingLongSet( iz, phi0, dphiring);
						}				
						res.addAll(listir2.longIterator());
					}
				}
			}
		}		
		return res.toLongRangeSet();
	}

	/**
	 * computes the intersection di of 2 intervals d1 (= [a1,b1]) and d2 (=
	 * [a2,b2]) on the periodic domain (=[A,B] where A and B arbitrary) ni is
	 * the resulting number of intervals (0,1, or 2) if a1 <b1 then d1 = {x |a1 <=
	 * x <= b1} if a1>b1 then d1 = {x | a1 <=x <= B U A <=x <=b1}
	 * 
	 * @param d1
	 *            double[] first interval
	 * @param d2
	 *            double[] second interval
	 * @return double[] one or two intervals intersections
	 */
	public double[] intrs_intrv(double[] d1, double[] d2) {
		double[] res;
		double epsilon = 1.0e-10;
		double[] dk;
		double[] di = { 0. };
		int ik = 0;
		boolean tr12, tr21, tr34, tr43, tr13, tr31, tr24, tr42, tr14, tr32;
		/*                                             */

		tr12 = ( d1[0] < d1[1] + epsilon );
		tr21 = !tr12; // d1[0] >= d1[1]
		tr34 = ( d2[0] < d2[1] + epsilon );
		tr43 = !tr34; // d2[0]>d2[1]
		tr13 = ( d1[0] < d2[0] + epsilon ); // d2[0] can be in interval
		tr31 = !tr13; // d1[0] in longerval
		tr24 = ( d1[1] < d2[1] + epsilon ); // d1[1] upper limit
		tr42 = !tr24; // d2[1] upper limit
		tr14 = ( d1[0] < d2[1] + epsilon ); // d1[0] in interval
		tr32 = ( d2[0] < d1[1] + epsilon ); // d2[0] in interval

		ik = 0;
		dk = new double[] { -1.0e9, -1.0e9, -1.0e9, -1.0e9 };
		/* d1[0] lower limit case 1 */
		if ( ( tr34 && tr31 && tr14 ) || ( tr43 && ( tr31 || tr14 ) ) ) {
			ik++; // ik = 1;
			dk[ik - 1] = d1[0]; // a1

		}
		/* d2[0] lower limit case 1 */
		if ( ( tr12 && tr13 && tr32 ) || ( tr21 && ( tr13 || tr32 ) ) ) {
			ik++; // ik = 1
			dk[ik - 1] = d2[0]; // a2

		}
		/* d1[1] upper limit case 2 */
		if ( ( tr34 && tr32 && tr24 ) || ( tr43 && ( tr32 || tr24 ) ) ) {
			ik++; // ik = 2
			dk[ik - 1] = d1[1]; // b1

		}
		/* d2[1] upper limit case 2 */
		if ( ( tr12 && tr14 && tr42 ) || ( tr21 && ( tr14 || tr42 ) ) ) {
			ik++; // ik = 2
			dk[ik - 1] = d2[1]; // b2

		}
		di = new double[1];
		di[0] = 0.;
		switch ( ik ) {

			case 2:
				di = new double[2];

				di[0] = dk[0] - epsilon;
				di[1] = dk[1] + epsilon;
				break;
			case 4:

				di = new double[4];
				di[0] = dk[0] - epsilon;
				di[1] = dk[3] + epsilon;
				di[2] = dk[1] - epsilon;
				di[3] = dk[2] + epsilon;
				break;
		}
		res = di;

		return res;
	}

	/**
	 * finds pixels having a colatitude (measured from North pole) : theta1 <
	 * colatitude < theta2 with o <= theta1 < theta2 <= Pi if theta2 < theta1
	 * then pixels with 0 <= colatitude < theta2 or theta1 < colatitude < Pi are
	 * returned
	 * 
	 * @param nside
	 *            long the map resolution parameter
	 * @param theta1
	 *            lower edge of the colatitude
	 * @param theta2
	 *            upper edge of the colatitude
	 * @param nest
	 *            long if = 1 result is in NESTED scheme
	 * @return ArrayList of pixel numbers (long)
	 * @throws Exception
	 */
	public LongRangeSet query_strip(int nside, double theta1,
			double theta2, long nest) throws Exception {
//		LongRangeSet res = new LongRangeSet();
		
		LongRangeSetBuilder res = new LongRangeSetBuilder();
		long npix, nstrip;
		long iz, irmin, irmax;
		int is;
		double phi0, dphi;
		double[] colrange = new double[4];
		boolean nest_flag = false;
		String SID = " QUERY_STRIP";
		/* ---------------------------------------- */
		npix = nside2Npix(nside);
		if ( nest == 1 )
			nest_flag = true;
		if ( npix < 0 ) {
			throw new IllegalArgumentException(SID
					+ " Nside should be power of 2");
		}
		if ( ( theta1 < 0.0 || theta1 > Constants.PI )
				|| ( theta2 < 0.0 || theta2 > Constants.PI ) ) {
			throw new IllegalArgumentException(SID
					+ " Illegal value of theta1, theta2");
		}
		if ( theta1 <= theta2 ) {
			nstrip = 1;
			colrange[0] = theta1;
			colrange[1] = theta2;
		} else {
			nstrip = 2;
			colrange[0] = 0.0;
			colrange[1] = theta2;
			colrange[2] = theta1;
			colrange[3] = Constants.PI;
		}
		/* loops on strips */
		for ( is = 0; is < nstrip; is++ ) {
			irmin = ringNum(nside, Math.cos(colrange[2 * is]));
			irmax = ringNum(nside, Math.cos(colrange[2 * is + 1]));
			/* loop on ring number */
			for ( iz = irmin; iz <= irmax; iz++ ) {
				phi0 = 0.;
				dphi = Constants.PI;
				LongRangeSet listir;
				if(nest_flag) {
					listir = inRing_nested_longset( iz, phi0, dphi);//InRing(nside, iz, phi0, dphi, nest_flag);
				}else {
					listir = inRingLongSet( iz, phi0, dphi);//InRing(nside, iz, phi0, dphi, nest_flag);
				}
				res.appendRanges(listir.rangeIterator());
				//if res is LongRangeSet, then res.addAll(listir.longIterator());
			}
		}
		return res.build();
	}

	/**
	 * returns 7 or 8 neighbours of any pixel in the nested scheme The neighbours
	 * are ordered in the following way: First pixel is the one to the south (
	 * the one west of the south direction is taken for pixels that don't have a
	 * southern neighbour). From then on the neighbors are ordered in the
	 * clockwise direction.
	 * 
	 * @param ipix long pixel number
	 * @return ArrayList
	 * @throws Exception 
	 * @throws IllegalArgumentException
	 */
	public List<Long> neighbours_nest( long ipix) throws Exception  {
		
		ArrayList<Long> result = new ArrayList<Long>(8);
		Xyf xyf = nest2xyf(ipix);
		int ix, iy, face_num;
		ix = xyf.ix;
		iy=xyf.iy;
		face_num=xyf.face_num;
		

		long nsm1 = (long)nside-1;
		long tmp=0;
		  if ((ix>0)&&(ix<nsm1)&&(iy>0)&&(iy<nsm1)){
		      for (int m=0; m<8; ++m){
		        tmp = xyf2nest(ix+xoffset[m],iy+yoffset[m],face_num);
		        result.add(m, tmp);
		      }
		  }else {
		    for (int i=0; i<8; ++i)
		      {
		      int x=ix+xoffset[i];
		      int y=iy+yoffset[i];
		      int nbnum=4;
		      if (x<0)
		        { x+=nside; nbnum-=1; }
		      else if (x>=nside)
		        { x-=nside; nbnum+=1; }
		      if (y<0)
		        { y+=nside; nbnum-=3; }
		      else if (y>=nside)
		        { y-=nside; nbnum+=3; }

		      int f = facearray[nbnum][face_num];
		      
		      if (f>=0)
		        {
		        if ((swaparray[nbnum][face_num]&1)>0) x=(int)((long)nside-(long)x-1L);
		        if ((swaparray[nbnum][face_num]&2)>0) y=(int)((long)nside-(long)y-1L);
		        if ((swaparray[nbnum][face_num]&4)>0) {
		        	int tint = x;
		        	x=y; y=tint;
		        }
		        tmp =  xyf2nest(x,y,f);
		        result.add(i,tmp);
		        }
		      else
		        result.add(i, -1L);
		      }
		    }
		return result;
	}

	/**
	 * return the parent PIXEL of a given pixel at some higher NSIDE. 
	 * One must also provide the nsode of the given pixel as otherwise it
	 * can not be known.
	 * 
	 * This only makes sense for Nested Scheme.
	 * This is basically a simple bit shift in the difference
	 * of number of bits between the two NSIDEs. 
	 * 
	 * @param child  the pixel 
	 * @param childnside nside of the pixel
	 * @param requirednside nside to upgrade to
	 * 
	 * @return the new pixel number
	 * @throws Exception 
 	 */
	static public long parentAt(long child, int childnside, int requirednside) throws Exception{
	    // nside is the number of bits .. 
		if (childnside < requirednside) {
			throw new Exception ("Parent ("+requirednside+
					") should have smaller NSIDE than Child("+childnside+")");
		}
		long ppix =0;
		
		// number of bits in aid depdens on the depth of the nside

		int bitdiff = bitdiff(requirednside, childnside); 
	    ppix = child >> bitdiff;
    	return ppix;	 		
	}

	/**
	 * return difference of number of bits in pixels of two nsides.
	 * @param nside1
	 * @param nside2
	 * @return  number of bits difference between the pixel ids.
	 */
	public static int bitdiff(long nside1, long nside2){
		int pbits = 2;
		long childnside=nside2;
		long parentnside=nside1;
		if (nside1>=nside2){
			childnside=nside1;
			parentnside=nside2;
		}
		int tnside = 2;
		while (tnside < parentnside) {
			pbits+=2;
			tnside=tnside<<1 ;// next power of 2
		}
		// child is deeper 
		int cbits = pbits;
		while (tnside < childnside) {
			cbits+=2;
			tnside=tnside<<1 ;// next power of 2
		}
		return (cbits- pbits);//  
		
	}
	/**
	 * for a given pixel list all children pixels for it. 
	 * This is simply a matter of shifting the pixel number left by
	 * the difference in NSIDE bits and then listing all numbers 
	 * which fill the empty bits. 
	 * 
	 * BEWARE - not checking you are not trying to go too DEEP. 
	 * 
	 * @param nside  nside of pix
	 * @param pix  the pixel 
	 * @param requiredNside  the nside you want the children at
	 * @return children pixels
	 * @throws Exception 
	 */
	public static long[] getChildrenAt(long nside, long pix, int requiredNside) throws Exception{
	 
		if (nside >= requiredNside){
			throw new Exception("The requirend NSIDE should be greater than the pix NSIDE");
		}
		int bitdiff=bitdiff(nside,requiredNside);
		int numpix = bitdiff<<1;// square num bits is count of pix
		long[] pixlist= new long[numpix];
		long ppix=pix<<bitdiff; // shift the current pix over
		// nopw just keep adding to it ..
		for (int i=0;i < numpix; i++){
			pixlist[i]=ppix+i;
		}
		return pixlist;
	}

	/**
	 * Gets the order value
	 * 
	 * @return order
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Sets the order
	 * 
	 * @param order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * returns the list of pixels in RING or NEST scheme with latitude in [phi0 -
	 * dpi, phi0 + dphi] on the ring iz in [1, 4*nside -1 ] The pixel id numbers
	 * are in [0, 12*nside^2 - 1] the indexing is in RING, unless nest is set to
	 * 1
	 * NOTE: this is the f90 code 'in_ring' method ported to java with 'conservative' flag to false
	 * 
	 * @param nside
	 *            long the map resolution
	 * @param iz
	 *            long ring number
	 * @param phi0
	 *            double
	 * @param dphi
	 *            double
	 * @param nest
	 *            boolean format flag
	 * @return ArrayList of pixels
	 * @throws IllegalArgumentException
	 * @deprecated Don't use anymore, was only for 
	 */
	public ArrayList<Long> inRingCxx(long nside, long iz, double phi0,
			double dphi, boolean nest) {
		long nr, ir, ipix1;

		double shift = 0.5;

		if ( iz < nside ) // north pole
		{
			ir = iz;
			nr = ir * 4;
			ipix1 = 2 * ir * ( ir - 1 ); // lowest pixel number in the ring
		} else if ( iz > ( 3 * nside ) ) // south pole
		{
			ir = 4 * nside - iz;
			nr = ir * 4;
			ipix1 = npix - 2 * ir * ( ir + 1 ); // lowest pixel number in the
			// ring
		} else // equatorial region
		{
			ir = iz - nside + 1; // within {1, 2*nside + 1}
			nr = nside * 4;
			if ( ( ir & 1 ) == 0 )
				shift = 0.;
			ipix1 = ncap + ( ir - 1 ) * nr; // lowest pixel number in the ring
		}

		long ipix2 = ipix1 + nr - 1; // highest pixel number in the ring
		ArrayList<Long> listir = new ArrayList<Long>();
		// ----------- constructs the pixel list --------------
		if ( dphi > ( Constants.PI - 1e-7 ) )
			for ( Long i = ipix1; i <= ipix2; ++i )
				listir.add(i);
		else {
			int ip_lo = (int) ( Math.floor(nr * ( 1 / Constants.twopi )
					* ( phi0 - dphi ) - shift) + 1 );
			int ip_hi = (int) ( Math.floor(nr * 1 / Constants.twopi
					* ( phi0 + dphi ) - shift) );
			long pixnum = (int) ( ip_lo + ipix1 );
			if ( pixnum < ipix1 )
				pixnum += nr;
			for ( int i = ip_lo; i <= ip_hi; ++i, ++pixnum ) {
				if ( pixnum > ipix2 )
					pixnum -= nr;
				listir.add(pixnum);
			}
		}
		 ArrayList<Long> listirnest = new ArrayList<Long>();
		listir.trimToSize();
		if ( nest ) {
			int i = 0;
			while ( i < listir.size() ) {
				long ipring = listir.get(i);
				try {
					long ipnest = ring2nest((int) ipring);
					listirnest.add(ipnest);
					i++;
				} catch ( Exception ex ) {
					ex.printStackTrace();
					break;// Very bad!
				}
			}
			return listirnest;
		}
		return listir;

	}

//	/**
//	 * Integer square root
//	 * 
//	 * @param arg
//	 *            value
//	 * @return long square root
//	 */
//	private static long isqrt(double arg) {
//		double d = Math.sqrt(arg + 0.5);
//		return (long) d;
//
//	}

//	//! Returns the largest integer \a n that fulfills \a 2^n<=arg.
//	template<typename I> inline unsigned int ilog2 (I arg)
//	  {
//	  unsigned int res=0;
//	  while (arg > 0x0000FFFF) { res+=16; arg>>=16; }
//	  if (arg > 0x000000FF) { res|=8; arg>>=8; }
//	  if (arg > 0x0000000F) { res|=4; arg>>=4; }
//	  if (arg > 0x00000003) { res|=2; arg>>=2; }
//	  if (arg > 0x00000001) { res|=1; }
//	  return res;
//	  }
}
