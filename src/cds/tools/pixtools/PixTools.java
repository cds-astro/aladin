/*
 * Created on Mar 10, 2005
 * Modified on December 18 2007
 *
 */
package cds.tools.pixtools;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 *  contains methods translated from HEALPix Fortran90
 *  with increased map resolution in comparison to original Fortran code.
 * 
 * @author N Kuropatkin
 *  
 */
public abstract class PixTools {

	private static double twothird = 2. / 3.;

	private static double PI = Math.PI;

	private static double TWOPI = 2. * PI;

//	private static double FOURPI = 4. * PI;

	private static double HALFPI = PI / 2.0;

    private static int ns_max = 1048576; // 2^20

//
    private static int xmax = 4096;
//
    private static int pixmax = 262144;

//
    private static int xmid = 512;   
	private static long[] x2pix = new long[xmax+1];

	private static long[] y2pix = new long[xmax+1];

	private static long[] pix2x = new long[pixmax+1];

	private static long[] pix2y = new long[pixmax+1];

	private static BitManipulation bm = new BitManipulation();

	/**
	 * default constructor
	 * 
	 *  
	 */
//	{
//		for (int i = 0; i <= xmax; i++) {
//			x2pix[i] = 0;
//			y2pix[i] = 0;
//		}
//		for (int i = 0; i <= pixmax; i++) {
//			pix2x[i] = 0;
//			pix2y[i] = 0;
//		}
//	}

	
	/**
	 * finds pixels having a colatitude (measured from North pole) : 
	 * theta1 < colatitude < theta2 with o <= theta1 < theta2 <= Pi 
	 * if theta2 < theta1
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
	 * @return  ArrayList of  pixel numbers (long)
	 * @throws Exception 
	 * @throws IllegalArgumentException
	 */
	static public ArrayList query_strip(long nside, double theta1, double theta2,
			long nest) throws Exception {
		ArrayList res = new ArrayList();
		ArrayList listir = new ArrayList();
		long npix, nstrip;
		long iz,  irmin, irmax;
        int is;
		double phi0, dphi;
		double[] colrange = new double[4];
		boolean nest_flag = false;
		/* ---------------------------------------- */
		npix = Nside2Npix(nside);
		if (nest == 1)
			nest_flag = true;
		if (npix < 0) {
			throw new IllegalArgumentException(" QUERY_STRIP Nside should be power of 2");
		}
		if ((theta1 < 0.0 || theta1 > PI) || (theta2 < 0.0 || theta2 > PI)) {
			throw new IllegalArgumentException(" QUERY_STRIP Illegal value of theta1, theta2");
		}
		if (theta1 <= theta2) {
			nstrip = 1;
			colrange[0] = theta1;
			colrange[1] = theta2;
		} else {
			nstrip = 2;
			colrange[0] = 0.0;
			colrange[1] = theta2;
			colrange[2] = theta1;
			colrange[3] = PI;
		}
		/* loops on strips */
		for (is = 0; is < nstrip; is++) {
			irmin = RingNum(nside, Math.cos(colrange[2 * is]));
			irmax = RingNum(nside, Math.cos(colrange[2 * is + 1]));
			/* loop on ring number */
			for (iz = irmin; iz <= irmax; iz++) {
				phi0 = 0.;
				dphi = PI;
				listir = InRing(nside, iz, phi0, dphi, nest_flag);
				res.addAll(listir);
			}
		}
		return res;
	}

	/**
	 * computes the intersection di of 2 intervals d1 (= [a1,b1])
	 * and d2 (= [a2,b2]) on the periodic domain (=[A,B] where A and B
	 * arbitrary) ni is the resulting number of intervals (0,1, or 2) if a1 <b1
	 * then d1 = {x |a1 <= x <= b1} if a1>b1 then d1 = {x | a1 <=x <= B U A <=x
	 * <=b1}
	 * 
	 * @param d1 double[] first interval
	 * @param d2 double[] second interval
	 * @return double[] one or two intervals intersections
	 */
	static public double[] intrs_intrv(double[] d1, double[] d2) {
		double[] res;
		double epsilon = 1.0e-10;
//		double temp = 0.;
//		int ni;
		double[] dk;
		double[] di = { 0. };
		int ik = 0;
		boolean tr12, tr21, tr34, tr43, tr13, tr31, tr24, tr42, tr14, tr32;
		/*                                             */

		tr12 = (d1[0] < d1[1] + epsilon);
		tr21 = !tr12; // d1[0] >= d1[1]
		tr34 = (d2[0] < d2[1] + epsilon);
		tr43 = !tr34; // d2[0]>d2[1]
		tr13 = (d1[0] < d2[0] + epsilon); //  d2[0] can be in interval
		tr31 = !tr13; // d1[0] in longerval
		tr24 = (d1[1] < d2[1] + epsilon); // d1[1] upper limit
		tr42 = !tr24; // d2[1] upper limit
		tr14 = (d1[0] < d2[1] + epsilon); // d1[0] in interval
		tr32 = (d2[0] < d1[1] + epsilon); // d2[0] in interval

		ik = 0;
		dk = new double[] { -1.0e9, -1.0e9, -1.0e9, -1.0e9 };
		/* d1[0] lower limit case 1 */
		if ((tr34 && tr31 && tr14) || (tr43 && (tr31 || tr14))) {
			ik++; // ik = 1;
			dk[ik - 1] = d1[0]; // a1

		}
		/* d2[0] lower limit case 1 */
		if ((tr12 && tr13 && tr32) || (tr21 && (tr13 || tr32))) {
			ik++; // ik = 1
			dk[ik - 1] = d2[0]; // a2

		}
		/* d1[1] upper limit case 2 */
		if ((tr34 && tr32 && tr24) || (tr43 && (tr32 || tr24))) {
			ik++; // ik = 2
			dk[ik - 1] = d1[1]; // b1

		}
		/* d2[1] upper limit case 2 */
		if ((tr12 && tr14 && tr42) || (tr21 && (tr14 || tr42))) {
			ik++; // ik = 2
			dk[ik - 1] = d2[1]; // b2

		}
		di = new double[1];
		di[0] = 0.;
		switch (ik) {

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
	 * renders theta and phi coordinates of the nominal pixel center for the
	 * pixel number ipix (RING scheme) given the map resolution parameter nside
	 * 
	 * @param nside 
	 *            long map resolution
	 * @param ipix 
	 *            long pixel number
	 * @return double[] theta,phi
	 */
	static public double[] pix2ang_ring(long nside, long ipix)  {
		double[] res = { 0., 0. };
		long nl2, nl4, npix, ncap, iring, iphi, ip, ipix1;
		double fodd, hip, fihip, theta, phi;
		/*                            */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException("pix2ang_ring: Nside should be power of 2 >0 and < "+ns_max);
		}
		long nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if (ipix < 0 || ipix > npix - 1) {
			throw new IllegalArgumentException("pix2ang_ring: ipix out of range calculated from nside");
		}
		ipix1 = ipix + 1; //  in [1, npix]
		nl2 = 2 * nside;
		ncap = 2 * nside * (nside - 1); // points in each polar cap, =0 for
		// nside =1

		if (ipix1 <= ncap) { // North polar cap
			hip = ipix1 / 2.0;
			fihip = (long) hip; // get integer part of hip
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from north
			                                                       // pole
			iphi = ipix1 - 2 * iring * (iring - 1);
			theta = Math.acos(1.0 - iring * iring / (3.0 * nsidesq));
			phi = (iphi - 0.5) * PI / (2.0 * iring);

		} else if (ipix1 <= nl2 * (5 * nside + 1)) { // equatorial region
			ip = ipix1 - ncap - 1;
			nl4 = 4 * nside;
			iring = (long) (ip / nl4) + nside; // counted from North pole
			iphi = (long) bm.MODULO(ip, nl4) + 1;
			fodd = 0.5 * (1. + bm.MODULO(iring + nside, 2)); // 1 if iring+nside
			                                                 // is odd, 1/2 otherwise
			theta = Math.acos((nl2 - iring) / (1.5 * nside));
			phi = (iphi - fodd) * PI / (2.0 * nside);
		} else { // South pole cap
			ip = npix - ipix1 + 1;
			hip = ip / 2.0;
			fihip = (long) hip;
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from South
			                                                       // pole
			iphi = 4 * iring + 1 - (ip - 2 * iring * (iring - 1));
			theta = Math.acos(-1.0 + iring * iring / (3.0 * nsidesq));
			phi = (iphi - 0.5) * PI / (2.0 * iring);
		}
		res[0] = theta;
		res[1] = phi;
		return res;
	}

	/**
	 * renders the pixel number ipix (RING scheme) for a pixel which contains a
	 * point with coordinates theta and phi, given the map resolution parameter
	 * nside.
	 * 
	 * @param nside 
	 *            long map resolution parameter
	 * @param theta 
	 *            double theta
	 * @param phi -
	 *            double phi
	 * @return  long ipix
	 */
	static public long ang2pix_ring(long nside, double theta, double phi) {
		long nl4;
        long jp, jm, kshift;
        long ip;
        long ir;
		double z, za, tt, tp, tmp;
		long pix = 0;
		long ipix1;
		long nl2,  ncap, npix;

		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException("ang2pix_ring: Nside should be power of 2 >0 and < "+ns_max);
		}
		if (theta < 0.0 || theta > PI) {
			throw new IllegalArgumentException("ang2pix_ring: Theta out of range [0,pi]");
		}
		
		z = Math.cos(theta);
		za = Math.abs(z);

		if (phi < 0.)
			phi += TWOPI; //  phi in [0, 2pi]
//		tt = phi / HALFPI; // in [0,4]
		tt = bm.MODULO(phi, TWOPI) / HALFPI; // in [0,4]
		nl2 = 2 * nside;
		nl4 = 4 * nside;
		ncap = nl2 * (nside - 1); // number of pixels in the north polar cap
		npix = 12 * nside * nside;
		if (za < twothird) { // equatorial region
			jp = (long) (nside * (0.5 + tt - 0.75 * z)); // index of ascending
			// edge line
			jm = (long) (nside * (0.5 + tt + 0.75 * z)); // index of descending
			// edge line

			ir = nside + 1 + jp - jm; // in [1,2n+1]
			kshift = 0;
			if ((long) bm.MODULO(ir, 2) == 0)
				kshift = 1; // 1 if ir even, 0 otherwise
			ip = (long) ((jp + jm - nside + kshift + 1) / 2) + 1; // in [1,4n]
			ipix1 = ncap + nl4 * (ir - 1) + ip;
		} else { // North and South polar caps
			tp = tt - (long) tt;
			tmp = Math.sqrt(3.0 * (1.0 - za));
			jp = (long) (nside * tp * tmp); // increasing edge line index
			jm = (long) (nside * (1.0 - tp) * tmp); // decreasing edge index

			ir = jp + jm + 1; // ring number counted from closest pole
			ip = (long) (tt * ir) + 1; // in [1,4*ir]
			if (ip > 4 * ir)
				ip = ip - 4 * ir;

			ipix1 = 2 * ir * (ir - 1) + ip;
			if (z <= 0.0)
				ipix1 = npix - 2 * ir * (ir + 1) + ip;
		}
		pix = ipix1 - 1; // in [0, npix-1]
		

		return pix;
	}

    // coordinate of the lowest corner of each face
    static final long[] jrll = { 0, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4 }; // in units of nside
    static final long[] jpll = { 0, 1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7 }; // in units of nside/2


	/**
	 * 
	 * Renders theta and phi coordinates of the normal pixel center for the
	 * pixel number ipix (NESTED scheme) given the map resolution parameter
	 * nside.
	 * 
	 * @param nside 
	 *            map resolution parameter - long
	 * @param ipix 
	 *            long pixel number 
	 * @return double[] (theta, phi)
	 * @throws IllegalArgumentException
	 */
    static public double[] pix2ang_nest(long nside, long ipix)  {
       return pix2ang_nest(null,nside,ipix);
    }
    static public double[] pix2ang_nest(double []res,long nside, long ipix)  {
		if( res==null ) res = new double[2];
		double theta = 0.;
		double phi = 0.;
		long npix, npface, ipf, ip_low, ip_trunc, ip_med, ip_hi;
		long jrt, jr, nr, jpt, jp, kshift, nl4, ix, iy, face_num;
		double z, fn, fact1, fact2;

		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException("pix2ang_nest: Nside should be power of 2 >0 and < "+ns_max);
		}
		long nsidesq = nside * nside;
		npix = 12 * nsidesq;
		if (ipix < 0 || ipix > npix - 1) {
			throw new IllegalArgumentException("pix2ang_nest: ipix out of range calculated from nside");
		}
		if (pix2x[262143] <= 0) mk_pix2xy();
		fn = 1.*nside;
		fact1 = 1.0 / (3.0 * fn * fn);
		fact2 = 2.0 / (3.0 * fn);
		nl4 = 4 * nside;
		/* findes the face, and the number in the face */
		npface = nside * nside;
		face_num = ipix / npface; // face number [0,11]
		ipf = (long) bm.MODULO(ipix, npface); // pixel in the face [0, npface-1]
		/*
		 * finds x,y on the face (starting from the lowest corner) from pixel
		 * number
		 */
		ip_low = (long) bm.MODULO(ipf, pixmax);   // content of the last 18 bits
		ip_trunc = ipf / pixmax;                  // trancation of the last 18 bits
		ip_med = (long) bm.MODULO(ip_trunc, pixmax); // content of the next 18 bits
		ip_hi = ip_trunc / pixmax;                // content of the high wait 18 bits

		ix = pixmax * pix2x[(int)ip_hi] + xmid * pix2x[(int)ip_med] + pix2x[(int) ip_low];
		iy = pixmax * pix2y[(int)ip_hi] + xmid * pix2y[(int)ip_med] + pix2y[(int)ip_low];
		/* transform these in (horizontal, vertical) coordinates */
		jrt = ix + iy; // [0,2*(nside-1)]
		jpt = ix - iy; // [ -nside+1, nside -1]
		/* computes the z coordinate on the sphere */
		jr = jrll[(int) (face_num + 1)] * nside - jrt - 1; // ring number in [1,
		// 4*nside-1]

		nr = nside; // equatorial region (the most frequent )
		z = (2 * nside - jr) * fact2;
		kshift = (long) bm.MODULO(jr - nside, 2);
		if (jr < nside) { // north pole region
			nr = jr;
			z = 1.0 - nr * nr * fact1;
			kshift = 0;
		} else if (jr > 3 * nside) { // south pole region
			nr = nl4 - jr;
			z = -1.0 + nr * nr * fact1;
			kshift = 0;
		}
		theta = Math.acos(z);
		/* computes phi coordinate on the sphere, in [0,2pi] */
		jp = (jpll[(int) (face_num + 1)] * nr + jpt + 1 + kshift) / 2;
		if (jp > nl4)
			jp = jp - nl4;
		if (jp < 1)
			jp = jp + nl4;

		phi = (jp - (kshift + 1) / 2.0) * (HALFPI / nr);
		res[0] = theta;
		res[1] = phi;
		return res;
	}



	/**
	 * renders the pixel number pix (NESTED scheme) for a pixel which contains a
	 * point on a sphere at coordinates theta and phi, given map resolution
	 * parameter nside.
	 * 
	 * The computation is made to the highest resolution available and then
	 * degraded to requared resolution by integer division. It makes sure that
	 * the treatment of roun-off will be consistent for every resolution.
	 * 
	 * @param nside the map resolution parameter
	 * @param theta double theta coordinate
	 * @param phi double phi coordinate
	 * @return pixel number long 
	 * @throws IllegalArgumentException
	 */
	static public long ang2pix_nest(long nside, double theta, double phi) {
		long pix = 0;
//		long ipix1;
		double z, za, tt, tp, tmp;
		long jp, jm, ifp, ifm, face_num, ix, iy, ix_low, ix_hi;
		long iy_low, iy_hi, ipf, ntt;
//		long nl2, nl4, ncap, npix, kshift, ir, ip;
		/*                              */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException("ang2pix_nest: Nside should be power of 2 >0 and < "+ns_max);
		}
		if (theta < 0.0 || theta > PI) {
			throw new IllegalArgumentException("ang2pix_nest: theta is out of range [0.,PI]");
		}
		if (x2pix[xmax-1] <= 0)			
			 mk_xy2pix();
			
		z = Math.cos(theta);
		za = Math.abs(z);

		if (phi < 0.) phi += TWOPI; // phi in [0,2pi]
		if (phi >= TWOPI ) phi -= TWOPI;
		tt = bm.MODULO(phi, TWOPI) / HALFPI; // in [0,4]
//		tt = 2. * phi / PI; // in [0,4]
		if (za <= twothird) { // Equatorial region
			/*
			 * the index of edge lines increases when the longitude = phi goes
			 * up
			 */
			jp = (long) (ns_max * (0.5 + tt - z * 0.75)); // ascending edge line
			// index
			jm = (long) (ns_max * (0.5 + tt + z * 0.75)); // descending edge line
			// index
			/* find the face */
			ifp = jp / ns_max; // in [0,4]
			ifm = jm / ns_max;
			if (ifp == ifm) { // faces 4 to 7
				face_num = (long) bm.MODULO(ifp, 4) + 4;
			} else if (ifp < ifm) { // (half-) faces 0 to 3
				face_num = (long) bm.MODULO(ifp, 4);
			} else { // (half-) faces 8 to 11
				face_num = (long) bm.MODULO(ifm, 4) + 8;
			}

			ix = (long) bm.MODULO(jm, ns_max);
			iy = ns_max - (long) bm.MODULO(jp, ns_max) - 1;
		} else { // polar region, za > 2/3
			ntt = (long) tt;
			if (ntt >= 4) ntt = 3;
			tp = tt - ntt;
			tmp = Math.sqrt(3.0 * (1.0 - za)); // in [0,1]
			/*
			 * the index of edge lines increases when distance from the closest
			 * pole goes up
			 */
			jp = (long) (ns_max * tp * tmp); // line going toward the pole has
			// phi increases
			jm = (long) (ns_max * (1.0 - tp) * tmp); // that one goes away of the
			// closest pole
			jp = (long) Math.min(ns_max - 1, jp); // for pointss too close to the
			// boundary
			jm = (long) Math.min(ns_max - 1, jm);
			/* finds the face and pixel's (x,y) */
			if (z >= 0) {
				face_num = ntt; // in [0,3]
				ix = ns_max - jm - 1;
				iy = ns_max - jp - 1;
			} else {
				face_num = ntt + 8; // in [8,11]
				ix = jp;
				iy = jm;
			}
		}
		ix_low = (long) bm.MODULO(ix, xmax);
		ix_hi = ix / xmax;
		iy_low = (long) bm.MODULO(iy, xmax);
		iy_hi = iy / xmax;

		ipf = (x2pix[(int) (ix_hi + 1)] + y2pix[(int) (iy_hi + 1)]) * (xmax * xmax)
				+ (x2pix[(int) (ix_low + 1)] + y2pix[(int) (iy_low + 1)]);
		ipf = ipf / ((ns_max / nside) * (ns_max / nside)); // in [0, nside**2
		// -1]
		pix = ipf + face_num * nside * nside; // in [0, 12*nside**2 -1]
		return pix;
	}

	/**
	 * make the conversion NEST to RING
	 * 
	 * @param nside the map resolution parameter
	 * @param map Object[] the map in NESTED scheme
	 * @return - Object[] a map in RING scheme
	 * @throws IllegalArgumentException
	 */
	static public Object[] convert_nest2ring(long nside, Object[] map)  {
		Object[] res;
		long npix, ipn;
        int ipr;
		npix = 12 * nside * nside;
		res = new Object[(int) npix];
		for (ipn = 0; ipn < npix; ipn++) {
			ipr = (int) nest2ring(nside, ipn);
			res[ipr] = map[(int) ipn];
		}
		return res;
	}

	/**
	 * makes the conversion RING to NEST
	 * 
	 * @param nside 
	 *            long resolution
	 * @param map 
	 *            map in RING
	 * @return  map in NEST
	 * @throws IllegalArgumentException
	 */
	static public Object[] convert_ring2nest(long nside, Object[] map)  {
		Object[] res;
		long npix, ipn, ipr;
		npix = 12 * nside * nside;
		res = new Object[(int) npix];
		for (ipr = 0; ipr < npix; ipr++) {
			ipn = ring2nest(nside, ipr);
			res[(int) ipn] = map[(int)ipr];
		}
		return res;
	}

	/**
	 * converts a 8 byte Object map from RING to NESTED and vice versa in place,
	 * ie without allocation a temporary map (Has no reason for Java). This
	 * method is more general but slower than convert_nest2ring.
	 * 
	 * This method is a wrapper for functions ring2nest and nest2ring. Their
	 * names are supplied in the subcall argument.
	 * 
	 * @param subcall 
	 *            String name of the method to use.
	 * @param map 
	 *            Object[] map
	 * @return  resulting Object[] map.
	 * @throws IllegalArgumentException
	 */
	static public Object[] convert_inplace_long(String subcall, Object[] map) {
		Object[] res;
		long npix, nside;
		boolean[] check;
		long ilast, i1, i2;
		Object pixbuf1, pixbuf2;
		npix = map.length;
		nside = (long) Math.sqrt(npix / 12.);
		if (nside > ns_max) {
			throw new IllegalArgumentException("convert_in_place: Map is too big");
		}
		check = new boolean[(int) npix];
		for (int i = 0; i < npix; i++)
			check[i] = false;
		ilast = 0; // start from first pixel
		for (int i = 0; i < npix; i++) {
			pixbuf2 = map[(int) ilast];
			i1 = ilast;
			if (subcall.equalsIgnoreCase("ring2nest")) {
				i2 = ring2nest(nside, i1);
			} else {
				i2 = nest2ring(nside, i1);
			}
			while (!check[(int) i2]) {
				pixbuf1 = map[(int) i2];
				map[(int) i2] = pixbuf2;
				pixbuf2 = pixbuf1;
				i1 = i2;
				if (subcall.equalsIgnoreCase("ring2nest")) {
					i2 = ring2nest(nside, i1);
				} else {
					i2 = nest2ring(nside, i1);
				}
			}
			while (!(check[(int) ilast] && (ilast < npix - 1))) {
				ilast++;
			}
		}
		res = map;
		return res;
	}

	/**
	 * returns 7 or 8 neighbours of any pixel in the nested scheme The neighbours
	 * are ordered in the following way: First pixel is the one to the south (
	 * the one west of the south direction is taken for pixels that don't have a
	 * southern neighbour). From then on the neighbors are ordered in the
	 * clockwise direction.
	 * 
	 * @param nside the map resolution
	 * @param ipix long pixel number
	 * @return ArrayList
	 * @throws IllegalArgumentException
	 */
	static public ArrayList neighbours_nest(long nside, long ipix)  {
		ArrayList res = new ArrayList();
		long npix, ipf, ipo, ix, ixm, ixp, iy, iym, iyp, ixo, iyo;
		long face_num, other_face;
		long ia, ib, ibp, ibm, ib2,  nsidesq;
        int icase;
		long local_magic1, local_magic2;
		long arb_const = 0;
		long[] ixiy = new long[2];
		long[] ixoiyo = new long[2];
		/* fill the pixel list with 0 */
		res.add(0, new Long(0));
		res.add(1, new Long(0));
		res.add(2, new Long(0));
		res.add(3, new Long(0));
		res.add(4, new Long(0));
		res.add(5, new Long(0));
		res.add(6, new Long(0));
		res.add(7, new Long(0));
		icase = 0;

		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("neighbours_nest: Nside should be power of 2 >0 and < "+ns_max);
		}
		nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if ((ipix < 0) || (ipix > npix - 1)) {
			throw new IllegalArgumentException("neighbours_nest: ipix out of range ");
		}
		if (x2pix[xmax-1] <= 0) mk_xy2pix();

		local_magic1 = (nsidesq - 1) / 3;
		local_magic2 = 2 * local_magic1;
		face_num = ipix / nsidesq;
		ipf = (long) bm.MODULO(ipix, nsidesq); // Pixel number in face
		ixiy = pix2xy_nest(nside, ipf);
		ix = ixiy[0];
		iy = ixiy[1];
		//
		ixm = ixiy[0] - 1;
		ixp = ixiy[0] + 1;
		iym = ixiy[1] - 1;
		iyp = ixiy[1] + 1;

		icase = 0; // inside the face

		/* exclude corners */
		if (ipf == local_magic2 && icase == 0)
			icase = 5; // West corner
		if (ipf == (nsidesq - 1) && icase == 0)
			icase = 6; // North corner
		if (ipf == 0 && icase == 0)
			icase = 7; // South corner
		if (ipf == local_magic1 && icase == 0)
			icase = 8; // East corner

		/* detect edges */
		if ((ipf & local_magic1) == local_magic1 && icase == 0)
			icase = 1; // NorthEast
		if ((ipf & local_magic1) == 0 && icase == 0)
			icase = 2; // SouthWest
		if ((ipf & local_magic2) == local_magic2 && icase == 0)
			icase = 3; // NorthWest
		if ((ipf & local_magic2) == 0 && icase == 0)
			icase = 4; // SouthEast

		/* iside a face */
		if (icase == 0) {
			res.add(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
			res.add(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
			res.add(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
			res.add(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
			res.add(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
			res.add(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
			res.add(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
			res.add(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
		}
		/*                 */
		ia = face_num / 4; // in [0,2]
		ib = (long) bm.MODULO(face_num, 4); // in [0,3]
		ibp = (long) bm.MODULO(ib + 1, 4);
		ibm = (long) bm.MODULO(ib + 4 - 1, 4);
		ib2 = (long) bm.MODULO(ib + 2, 4);

		if (ia == 0) { // North pole region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 4 + ib;
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long( (other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 0 + ibm;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // E-W flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 4 + ibp;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( arb_const));
				res.set(3, new Long( (arb_const + 2)));
				res.set(4, new Long( (ipix + 1)));
				res.set(5, new Long( (ipix - 1)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long( (ipix - 1)));
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(2, new Long( (arb_const - 2)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib2;
				res.set(4, new Long( (other_face * nsidesq + nsidesq - 1)));
				other_face = 0 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(5, new Long( arb_const));
				res.set(6, new Long( (arb_const - 1)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib;
				res.set(0, new Long( (other_face * nsidesq + nsidesq - 1)));
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(1, new Long( arb_const));
				res.set(2, new Long( (arb_const + 2)));
				res.set(3, new Long( (ipix + 2)));
				res.set(4, new Long( (ipix + 3)));
				res.set(5, new Long( (ipix + 1)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, new Long( (arb_const + 1)));
				res.set(7, new Long( arb_const));
				break;
			case 8: // East corner
				other_face = 0 + ibp;
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, new Long( (arb_const + 1)));
				res.set(5, new Long(( arb_const)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 1)));
				res.set(6, new Long( arb_const));
				res.remove(7);
				break;
			}
		} else if (ia == 1) { // Equatorial region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long((other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NortWest edge
				other_face = 0 + ibm;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // NW-SE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(2, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long(xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ib;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 4 + ibm;
				res.set(2, new Long( (other_face * nsidesq + local_magic1)));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq;
				res.set(3, new Long( arb_const));
				res.set(4, new Long( (arb_const + 1)));
				res.set(5, new Long( (ipix + 1)));
				res.set(6, new Long( (ipix - 1)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long( (ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( (arb_const - 1)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, new Long( arb_const));
				res.set(5, new Long( (arb_const - 2)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 7: // South corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, new Long( arb_const));
				res.set(1, new Long( (arb_const + 2)));
				res.set(2, new Long( (ipix + 2)));
				res.set(3, new Long( (ipix + 3)));
				res.set(4, new Long( (ipix + 1)));
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, new Long( (arb_const + 1)));
				res.set(6, new Long( arb_const));
				res.remove(7);
				break;
			case 8: // East corner
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 1)));
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				res.set(7, new Long( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq;
				res.set(4, new Long( (arb_const + 2)));
				res.set(5, new Long( arb_const));
				other_face = 4 + ibp;
				res.set(6, new Long( (other_face * nsidesq + local_magic2)));
				break;
			}
		} else { // South pole region
			switch (icase) {
			case 1: // North-East edge
				other_face = 4 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // W-E flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( (other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long(xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 4 + ib;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ibp;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // SE-NW
				// flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 4 + ib;
				res.set(2, new Long( (other_face * nsidesq)));
				res.set(3, new Long( (other_face * nsidesq + 1)));
				res.set(4, new Long( (ipix + 1)));
				res.set(5, new Long( (ipix - 1)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 4 + ib;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long((ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( (arb_const - 1)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib;
				res.set(4, new Long( (other_face * nsidesq)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, new Long( arb_const));
				res.set(6, new Long( (arb_const - 2)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib2;
				res.set(0, new Long( (other_face * nsidesq)));
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq;
				res.set(1, new Long( arb_const));
				res.set(2, new Long( (arb_const + 1)));
				res.set(3, new Long( (ipix + 2)));
				res.set(4, new Long( (ipix + 3)));
				res.set(5, new Long( (ipix + 1)));
				other_face = 8 + ibp;
				arb_const = other_face * nsidesq;
				res.set(6, new Long( (arb_const + 2)));
				res.set(7, new Long( arb_const));
				break;
			case 8: // East corner
				other_face = 8 + ibp;
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, new Long( arb_const));
				res.set(0, new Long( (arb_const - 2)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq;
				res.set(4, new Long( (arb_const + 2)));
				res.set(5, new Long( arb_const));
				res.remove(7);
				break;
			}
		}
		return res;
	}

	/**
	 * returns the list of pixels in RING or NEST scheme with latitude in [phi0 -
	 * dpi, phi0 + dphi] on the ring iz in [1, 4*nside -1 ] The pixel id numbers
	 * are in [0, 12*nside^2 - 1] the indexing is in RING, unless nest is set to
	 * 1
	 * 
	 * @param nside 
	 *            long the map resolution
	 * @param iz 
	 *           long ring number
	 * @param phi0 
	 *            double
	 * @param dphi 
	 *            double
	 * @param nest 
	 *            boolean format flag
	 * @return ArrayList of pixels
	 * @throws IllegalArgumentException
	 */
	static public ArrayList InRing(long nside, long iz, double phi0, double dphi,
			boolean nest)  {
		boolean take_all = false;
		boolean to_top = false;
		boolean do_ring = true;
		boolean conservative = true;
//		String SID = "InRing:";
		double epsilon = 1.0e-13; // the constant to eliminate
		// java calculation jitter
		if (nest)
			do_ring = false;
		double shift = 0.;
		long ir = 0;
		long kshift, nr, ipix1, ipix2, nir1, nir2, ncap, npix;
		long ip_low = 0, ip_hi = 0, in, inext, nir;
		ArrayList res = new ArrayList();
		npix = 12 * nside * nside; // total number of pixels
		ncap = 2 * nside * (nside - 1); // number of pixels in the north polar
		// cap
		double phi_low = bm.MODULO(phi0 - dphi, TWOPI) - epsilon; // phi min,
																  // excluding
		// 2pi period
		double phi_hi = bm.MODULO(phi0 + dphi, TWOPI) + epsilon;

		if (Math.abs(dphi - PI) < 1.0e-6)
			take_all = true;
		/* identifies ring number */
		if ((iz >= nside) && (iz <= 3 * nside)) { // equatorial region
			ir = iz - nside + 1; // in [1, 2*nside + 1]
			ipix1 = ncap + 4 * nside * (ir - 1); // lowest pixel number in the
			// ring
			ipix2 = ipix1 + 4 * nside - 1; // highest pixel number in the ring
			kshift = (long) bm.MODULO(ir, 2.);

			nr = nside * 4;
		} else {
			if (iz < nside) { // north pole
				ir = iz;
				ipix1 = 2 * ir * (ir - 1); // lowest pixel number
				ipix2 = ipix1 + 4 * ir - 1; // highest pixel number
			} else { // south pole
				ir = 4 * nside - iz;

				ipix1 = npix - 2 * ir * (ir + 1); // lowest pixel number
				ipix2 = ipix1 + 4 * ir - 1; // highest pixel number
			}
			nr = ir * 4;
			kshift = 1;
		}
		// Construct the pixel list
		if (take_all) {
			nir = ipix2 - ipix1 + 1;
			if (do_ring) {
				long ind = 0;
				for (long i =  ipix1; i <= ipix2; i++) {
					res.add((int) ind, new Long(i));
					ind++;
				}
			} else {
				in = ring2nest(nside, ipix1);
				res.add(0, new Long( in));
				for (int i = 1; i < nir; i++) {
					inext = next_in_line_nest(nside, in);
					in = inext;
					res.add( i, new Long(in));
				}
			}
			return res;
		}
		shift = kshift / 2.0;

		// conservative : include every intersected pixel, even if the
		// pixel center is out of the [phi_low, phi_hi] region
		if (conservative) {
			ip_low = (long) Math.round((nr * phi_low) / TWOPI - shift);
			ip_hi = (long) Math.round((nr * phi_hi) / TWOPI - shift);
			ip_low = (long) bm.MODULO(ip_low, nr); // in [0, nr - 1]
			ip_hi = (long) bm.MODULO(ip_hi, nr); // in [0, nr - 1]
		} else { // strict: includes only pixels whose center is in
			//                                                    [phi_low,phi_hi]

			ip_low = (long) Math.round((nr * phi_low) / TWOPI - shift);
			ip_hi = (long) Math.round((nr * phi_hi) / TWOPI - shift);
			if (ip_low == ip_hi + 1) ip_low = ip_hi;

			if ((ip_low - ip_hi == 1) && (dphi * nr < PI)) {
				// the interval is too small ( and away from pixel center)
				// so no pixels is included in the list
				System.out.println("the interval is too small and avay from center");
				return res; // return empty list
			}
			ip_low = Math.min(ip_low, nr - 1);
			ip_hi = Math.max(ip_hi, 0);
		}
		//
		if (ip_low > ip_hi)
			to_top = true;
		ip_low += ipix1;
		ip_hi += ipix1;
		if (to_top) {
			nir1 = ipix2 - ip_low + 1;
			nir2 = ip_hi - ipix1 + 1;
			nir = nir1 + nir2;
			if (do_ring) {
				int ind = 0;
				for (long i =  ip_low; i <= ipix2; i++) {
					res.add(ind, new Long(i));
					ind++;
				}
				//				ind = nir1;
				for (long i =  ipix1; i <= ip_hi; i++) {
					res.add(ind, new Long(i));
					ind++;
				}
			} else {
				in = ring2nest(nside, ip_low);
				res.add(0, new Long(in));
				for (long i = 1; i <= nir - 1; i++) {
					inext = next_in_line_nest(nside, in);
					in = inext;
					res.add((int) i, new Long(in));
				}
			}
		} else {
			nir = ip_hi - ip_low + 1;
			if (do_ring) {
				int ind = 0;
				for (long i =  ip_low; i <= ip_hi; i++) {
					res.add(ind, new Long(i));
					ind++;
				}
			} else {
				in = ring2nest(nside, ip_low);
				res.add(0, new Long(in));
				for (int i = 1; i <= nir - 1; i++) {
					inext = next_in_line_nest(nside, in);
					in = inext;
					res.add(i, new Long(in));
				}
			}
		}
		return res;
	}

	/**
	 * calculates the pixel that lies on the East side (and the same
	 * latitude) as the given NESTED pixel number - ipix
	 * 
	 * @param nside 
	 *            long resolution
	 * @param ipix 
	 *            long pixel number
	 * @return  long next pixel in line
	 * @throws IllegalArgumentException
	 */
	static public long next_in_line_nest(long nside, long ipix)  {
		long npix, ipf, ipo, ix, ixp, iy, iym, ixo, iyo, face_num, other_face;
		long ia, ib, ibp, nsidesq;
		long ibm, ib2;
        int icase;
		long local_magic1, local_magic2;
		long[] ixiy = new long[2];
		long inext = 0; // next in line pixel in Nest scheme
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("next_in_line: nside should be power of 2 >0 and < "+ns_max);
		}
		nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if ((ipix < 0) || (ipix > npix - 1)) {
			throw new IllegalArgumentException("next_in_line: ipix out of range defined by nside");
		}
		// initiates array for (x,y) -> pixel number -> (x,y) mapping
		if (x2pix[xmax-1] <= 0)
			mk_xy2pix();
		local_magic1 = (nsidesq - 1) / 3;
		local_magic2 = 2 * local_magic1;
		face_num = ipix / nsidesq;
		ipf = (long) bm.MODULO(ipix, nsidesq); // Pixel number in face
		ixiy = pix2xy_nest(nside, ipf);
		ix = ixiy[0];
		iy = ixiy[1];
		ixp = ix + 1;
		iym = iy - 1;
		boolean sel = false;
		icase = -1; // iside the nest flag
		// Exclude corners
		if (ipf == local_magic2) { // west coirner
			inext = ipix - 1;
			return inext;
		}
		if ((ipf == nsidesq - 1) && !sel) { // North corner
			icase = 6;
			sel = true;
		}
		if ((ipf == 0) && !sel) { // Siuth corner
			icase = 7;
			sel = true;
		}
		if ((ipf == local_magic1) && !sel) { // East corner
			icase = 8;
			sel = true;
		}
		// Detect edges
		if (((ipf & local_magic1) == local_magic1) && !sel) { // North-East
			icase = 1;
			sel = true;
		}
		if (((ipf & local_magic2) == 0) && !sel) { // South-East
			icase = 4;
			sel = true;
		}
		if (!sel) { // iside a face
			inext = xy2pix_nest(nside, ixp, iym, face_num);
			return inext;
		}
		//
		ia = face_num / 4; // in [0,2]
		ib = (long) bm.MODULO(face_num, 4); // in [0,3]
		ibp = (long) bm.MODULO(ib + 1, 4);
		ibm = (long) bm.MODULO(ib + 4 - 1, 4);
		ib2 = (long) bm.MODULO(ib + 2, 4);

		if (ia == 0) { // North pole region
			switch (icase) {
			case 1:
				other_face = 0 + ibp;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq);
				inext = other_face * nsidesq + ipo;
				break;
			case 4:
				other_face = 4 + ibp;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // SE-NW flip

				ixiy = pix2xy_nest(nside, ipo);
				ixo = ixiy[0];
				iyo = ixiy[1];

				inext = xy2pix_nest(nside, ixo + 1, iyo, other_face);

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

		} else if (ia == 1) { // Equatorial region
			switch (icase) {
			case 1: // NorthEast edge
				other_face = 0 + ib;
//                System.out.println("ipf="+ipf+" nsidesq="+nsidesq+" invLSB="+bm.invLSB(ipf));
				ipo = (long) bm.MODULO((double)bm.invLSB( ipf), (double)nsidesq); // NE-SW flip
//                System.out.println(" ipo="+ipo);
                
				ixiy = pix2xy_nest(nside, ipo);
				ixo = ixiy[0];
				iyo = ixiy[1];
				inext = xy2pix_nest(nside, ixo, iyo - 1, other_face);
				break;
			case 4: // SouthEast edge
				other_face = 8 + ib;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq);
				ixiy = pix2xy_nest(nside, ipo);
				inext = xy2pix_nest(nside, ixiy[0] + 1, ixiy[1], other_face);
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
			switch (icase) {
			case 1: // NorthEast edge
				other_face = 4 + ibp;
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // NE-SW flip
				ixiy = pix2xy_nest(nside, ipo);
				inext = xy2pix_nest(nside, ixiy[0], ixiy[1] - 1, other_face);
				break;
			case 4: // SouthEast edge
				other_face = 8 + ibp;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // E-W flip
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
	 * gives the pixel number ipix (NESTED) corresponding to ix, iy and face_num
	 * 
	 * @param nside 
	 *            the map resolution parameter
	 * @param ix 
	 *            Integer x coordinate
	 * @param iy 
	 *            Integer y coordinate
	 * @param face_num 
	 *            long face number
	 * @return  long pixel number ipix
	 * @throws IllegalArgumentException
	 */
	static private long xy2pix_nest(long nside, long ix, long iy, long face_num){
		long res = 0;
		long ix_low, ix_hi, iy_low, iy_hi, ipf;
		//
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("xy2pix_nest: nside should be power of 2 >0 and < "+ns_max);
		}
//		if ((ix < 0) || (ix > nside - 1)) {
//			throw new IllegalArgumentException("xy2pix_nest: ix out of range [0, nside-1]");
//		}
//		if ((iy < 0) || (iy > nside - 1)) {
//			throw new IllegalArgumentException("xy2pix_nest: iy out of range [0, nside-1]");
//		}
		if (x2pix[xmax-1] <= 0) mk_xy2pix();
		ix_low = (long) bm.MODULO(ix, xmax);
		ix_hi = ix / xmax;
		iy_low = (long) bm.MODULO(iy, xmax);
		iy_hi = iy / xmax;

		ipf = (x2pix[(int) (ix_hi + 1)] + y2pix[(int) (iy_hi + 1)]) * xmax * xmax
				+ (x2pix[(int) (ix_low + 1)] + y2pix[(int) (iy_low + 1)]);
		res = ipf + face_num * nside * nside; // in [0, 12*nside^2 - 1]

		return res;
	}

	/**
	 * gives the x,y coordinates in a face from pixel number within the face
	 * (NESTED) schema.
	 * 
	 * @param nside 
	 *            long resolution parameter
	 * @param ipf 
	 *            long pixel number
	 * @return ixiy  long[] contains x and y coordinates
	 * @throws IllegalArgumentException
	 */
	static private long[] pix2xy_nest(long nside, long ipf)  {
		long[] ixiy = { 0, 0 };
		long ip_low, ip_trunc, ip_med, ip_hi;
//        System.out.println(" ipf="+ipf+" nside="+nside);
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("pix2xy_nest: nside should be power of 2 >0 and < "+ns_max);
		}
		if ((ipf < 0) || (ipf > nside * nside - 1)) {
			throw new IllegalArgumentException("pix2xy_nest: ipix out of range defined by nside");
		}
		if (pix2x[pixmax] <= 0) mk_pix2xy();
		ip_low = (long) bm.MODULO(ipf, pixmax); // contents of last 15 bits
		ip_trunc = ipf / pixmax; // truncation of the last 15 bits
		ip_med = (long) bm.MODULO(ip_trunc, pixmax); // next 15 bits
		ip_hi = ip_trunc / pixmax; // select high 15 bits

		long ix = pixmax * pix2x[(int) ip_hi] + xmid * pix2x[(int) ip_med] + pix2x[(int) ip_low];
		long iy = pixmax * pix2y[(int) ip_hi] + xmid * pix2y[(int) ip_med] + pix2y[(int) ip_low];
		ixiy[0] = ix;
		ixiy[1] = iy;
		return ixiy;
	}


	/**
	 * fills arrays x2pix and y2pix giving the number of the pixel laying in
	 * (x,y). x and y are in [1,512] the pixel number is in [0, 512**2 -1]
	 * 
	 * if i-1 = sum_p=0 b_p*2^p then ix = sum+p=0 b_p*4^p iy = 2*ix ix + iy in
	 * [0,512**2 -1]
	 * 
	 */
	static private void mk_xy2pix() {
		long k, ip, id;

		for (int i = 1; i <= xmax; i++) {
			long j = i - 1;
			k = 0;
			ip = 1;
			while (j != 0) {
				id = (long) bm.MODULO(j, 2);
				j /= 2;
				k += ip * id;
				ip *= 4;
			}
			x2pix[i] = k;
			y2pix[i] = 2 * k;
			
		}

	}

	/**
	 * creates an array of pixel numbers pix2x from x and y coordinates in the
	 * face. Suppose NESTED scheme of pixel ordering Bits corresponding to x and
	 * y are interleaved in the pixel number in even and odd bits.
	 */
	static private void mk_pix2xy() {
		long kpix, jpix, ix, iy, ip, id;
		boolean flag = true;
		for (kpix = 0; kpix <= pixmax; kpix++) { // loop on pixel numbers
			jpix = kpix;
			ix = 0;
			iy = 0;
			ip = 1; // bit position in x and y

			while (jpix != 0) { // go through all the bits

				id = (long) bm.MODULO(jpix, 2); // bit value (in kpix), goes in x
				jpix /= 2;
				ix += id * ip;

				id = (long) bm.MODULO(jpix, 2); // bit value, goes in iy
				jpix /= 2;
				iy += id * ip;

				ip *= 2; // next bit (in x and y )
			}
 
			pix2x[(int) kpix] = ix; // in [0,511]
			pix2y[(int) kpix] = iy; // in [0,511]
			

		}
	}

	/**
	 * converts pixel number from ring numbering schema to the nested one
	 * 
	 * @param nside 
	 *            long resolution
	 * @param ipring long pixel number in ring schema
	 * @return long pixel number in nest schema
	 * @throws IllegalArgumentException
	 */
	static public long ring2nest(long nside, long ipring)  {
		long ipnest = 0;
		double fihip;
		double hip;
		long npix, nl2, nl4, ncap, ip, iphi, ipt, ipring1, kshift, face_num;
		long nr, irn, ire, irm, irs, irt, ifm, ifp, ix, iy, ix_low, ix_hi, iy_low;
		long iy_hi, ipf;
		//
		face_num = 0;
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("ring2nest: nside should be power of 2 >0 and < "+ns_max);
		}
		npix = 12 * nside * nside; // total number of points
		if ((ipring < 0) || (ipring > npix - 1)) {
			throw new IllegalArgumentException("ring2nest: ipring out of range [0,npix-1]");
		}
		if (x2pix[xmax-1] <= 0)
			mk_xy2pix();

		nl2 = 2 * nside;
		nl4 = 4 * nside;
		ncap = nl2 * (nside - 1); // points in each polar cap, =0 for nside = 1
		ipring1 = ipring + 1;
		// finds the ring number, the position of the ring and the face number
		if (ipring1 <= ncap) { // north polar cap
			hip = ipring1 / 2.0;
//			 ANAIS : round -> floor : 2005ApJ...622..759G p.763 : the largest integer number smaller 
			fihip = Math.floor(hip);
//			fihip = Math.round(hip);
//			 ANAIS : floor --> 2005ApJ...622..759G p.763 : the largest integer number smaller 
			irn = (long) Math.floor(Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from
//			irn = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from

//			fihip = Math.round(hip);
//			irn = (long) Math.sqrt(hip - Math.sqrt(fihip)) + 1; // counted from
			// north pole
			iphi = ipring1 - 2 * irn * (irn - 1);

			kshift = 0;
			nr = irn; // 1/4 of the number of points on the current ring
			face_num = (iphi - 1) / irn; // in [0,3 ]
		} else if (ipring1 <= nl2 * (5 * nside + 1)) { // equatorial region
			ip = ipring1 - ncap - 1;
			irn = (ip / nl4) + nside; // counted from north pole
			iphi = (long) bm.MODULO(ip, nl4) + 1;

			kshift = (long) bm.MODULO(irn + nside, 2); // 1 if odd 0
			// otherwise
			nr = nside;
			ire = irn - nside + 1; // in [1, 2*nside+1]
			irm = nl2 + 2 - ire;
			ifm = (iphi - ire / 2 + nside - 1) / nside; // face boundary
			ifp = (iphi - irm / 2 + nside - 1) / nside;
			if (ifp == ifm) {
				face_num = (long) bm.MODULO(ifp, 4.) + 4;
			} else if (ifp + 1 == ifm) { // (half-) faces 0 to 3
				face_num = ifp;
			} else if (ifp - 1 == ifm) { // (half-) faces 8 to 11
				face_num = ifp + 7;
			}
		} else { // south polar cap
			ip = npix - ipring1 + 1;
			hip = ip / 2.0;
//			 ANAIS : floor --> 2005ApJ...622..759G p.763 : the largest integer number smaller 
			fihip = Math.floor(hip);
//			fihip = Math.rint(hip);
			irs = (long) Math.floor(Math.sqrt(hip - Math.sqrt(fihip)) + 1);
			iphi = 4 * irs + 1 - (ip - 2 * irs * (irs - 1));
			kshift = 0;
			nr = irs;
			irn = nl4 - irs;
			face_num = (iphi - 1) / irs + 8; // in [8,11]
		}
		// finds the (x,y) on the face
		irt = irn - jrll[(int) (face_num + 1)] * nside + 1; // in [-nside+1,0]
		ipt = 2 * iphi - jpll[(int) (face_num + 1)] * nr - kshift - 1; // in [-nside+1,
		// nside-1]
		if (ipt >= nl2)
			ipt -= 8 * nside; // for the face #4

		ix = (ipt - irt) / 2;
		iy = -(ipt + irt) / 2;

		ix_low = (long) bm.MODULO(ix, xmax);
		ix_hi = ix / xmax;
		iy_low = (long) bm.MODULO(iy, xmax);
		iy_hi = iy / xmax;
		ipf = (x2pix[(int) (ix_hi + 1)] + y2pix[(int) (iy_hi + 1)]) * xmax * xmax
				+ (x2pix[(int) (ix_low + 1)] + y2pix[(int) (iy_low + 1)]); // in [0, nside**2 -1]
		ipnest = ipf + face_num * nside * nside; // in [0, 12*nside**2 -1]
		return ipnest;

	}

	/**
	 * converts from NESTED to RING pixel numbering
	 * 
	 * @param nside 
	 *            long resolution
	 * @param ipnest
	 *            long NEST pixel number
	 * @return ipring  long RING pixel number
	 * @throws IllegalArgumentException
	 */
	static public long nest2ring(long nside, long ipnest)  {
		long res = 0;
		long npix, npface, face_num, ncap, n_before, ipf, ip_low, ip_trunc;
		long ip_med, ip_hi, ix, iy, jrt, jr, nr, jpt, jp, kshift, nl4;
//		long[] ixiy = { 0, 0 };
		// coordinates of lowest corner of each face
		//
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("nest2ring: nside should be power of 2 >0 and < ns_max");
		}
		npix = 12 * nside * nside;
		if ((ipnest < 0) || (ipnest > npix - 1)) {
			throw new IllegalArgumentException("nest2ring: ipnest out of range [0,npix-1]");
		}
		if (pix2x[pixmax-1] <= 0)
			mk_pix2xy();
		ncap = 2 * nside * (nside - 1); // number of points in the North polar
		// cap
		nl4 = 4 * nside;
		// finds the face and the number in the face
		npface = nside * nside;

		face_num = ipnest / npface; // face number in [0,11]
		if (ipnest >= npface) {
			ipf = (long) bm.MODULO(ipnest, npface); // pixel number in the face
		} else {
			ipf = ipnest;
		}

		// finds the x,y on the face
		//  from the pixel number
		ip_low = (long) bm.MODULO(ipf, pixmax); // last 15 bits
		if (ip_low < 0)
			ip_low = -ip_low;

		ip_trunc = ipf / pixmax; // truncate last 15 bits
		ip_med = (long) bm.MODULO(ip_trunc, pixmax); // next 15 bits
		if (ip_med < 0)
			ip_med = -ip_med;
		ip_hi = ip_trunc / pixmax; // high 15 bits

		ix = pixmax * pix2x[(int) ip_hi] + xmid * pix2x[(int) ip_med] + pix2x[(int) ip_low];
		iy = pixmax * pix2y[(int) ip_hi] + xmid * pix2y[(int) ip_med] + pix2y[(int) ip_low];

		// transform this in (horizontal, vertical) coordinates
		jrt = ix + iy; // vertical in [0,2*(nside -1)]
		jpt = ix - iy; // horizontal in [-nside+1, nside - 1]
		// calculate the z coordinate on the sphere
		jr = jrll[(int) (face_num + 1)] * nside - jrt - 1; // ring number in [1,4*nside
		// -1]
		nr = nside; // equatorial region (the most frequent)
		n_before = ncap + nl4 * (jr - nside);
		kshift = (long) bm.MODULO(jr - nside, 2);
		if (jr < nside) { // north pole region
			nr = jr;
			n_before = 2 * nr * (nr - 1);
			kshift = 0;
		} else if (jr > 3 * nside) { // south pole region
			nr = nl4 - jr;
			n_before = npix - 2 * (nr + 1) * nr;
			kshift = 0;
		}
		// computes the phi coordinate on the sphere in [0,2pi]
		jp = (jpll[(int) (face_num + 1)] * nr + jpt + 1 + kshift) / 2; // 'phi' number
		// in ring
		// [1,4*nr]
		if (jp > nl4)
			jp -= nl4;
		if (jp < 1)
			jp += nl4;
		res = n_before + jp - 1; // in [0, npix-1]
		return res;
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
	static public long RingNum(long nside, double z) {
		long iring = 0;
		/* equatorial region */

		iring = (long) Math.round(nside * (2.0 - 1.5 * z));
		/* north cap */
		if (z > twothird) {
			iring = (long) Math.round(nside * Math.sqrt(3.0 * (1.0 - z)));
			if (iring == 0)
				iring = 1;
		}
		/* south cap */
		if (z < -twothird) {
			iring = (long) Math.round(nside * Math.sqrt(3.0 * (1.0 + z)));
			if (iring == 0)
				iring = 1;
			iring = 4 * nside - iring;
		}
		return iring;
	}

	/**
	 * returns nside such that npix = 12*nside^2,  nside should be
	 * power of 2 and smaller than ns_max if not return -1
	 * 
	 * @param npix
	 *            long the number of pixels in the map
	 * @return long nside the map resolution parameter
	 */
	static public long Npix2Nside(long npix) {
		long nside = 0;
		long npixmax = 12 *(long) ns_max *(long) ns_max;
 
		String SID = "Npix2Nside:";
		nside = (long) Math.rint(Math.sqrt(npix / 12));
		if (npix < 12) {
			throw new IllegalArgumentException(SID + " npix is too small should be > 12");
		}
		if (npix > npixmax) {
			throw new IllegalArgumentException(SID + " npix is too large > 12 * ns_max^2");
		}
		double fnpix = 12.0 * nside * nside;
		if (Math.abs(fnpix - npix) > 1.0e-2) {
			throw new IllegalArgumentException(SID + "  npix is not 12*nside*nside");
		}
		double flog = Math.log((double) nside) / Math.log(2.0);
		double ilog = Math.rint(flog);
		if (Math.abs(flog - ilog) > 1.0e-6) {
			throw new IllegalArgumentException(SID + "  nside is not power of 2");
		}
		return nside;
	}

	/**
	 * calculates npix such that npix = 12*nside^2 ,nside should be
	 * a power of 2, and smaller than ns_max otherwise return -1 
	 * 
	 * @param nside
	 *            long the map resolution
	 * @return npix long the number of pixels in the map
	 */
	static public long Nside2Npix(long nside) {

		long[] nsidelist = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048,
				4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576,
                2097152, 4194304};

		long res = 0;
		String SID = "Nside2Npix:";
		if (Arrays.binarySearch(nsidelist, nside) < 0) {
			throw new IllegalArgumentException(SID + " nside should be >0, power of 2, <"+ns_max);
		}
		res = 12 * nside * nside;
		return res;
	}

    /**
     * calculates angular resolution of the pixel map
     * in arc seconds.
     * @param nside
     * @return double resolution in arcsec
     */
    static public double PixRes(long nside) {
        double res = 0.;
        double degrad = Math.toDegrees(1.0);
        double skyArea = 4.*PI*degrad*degrad; // 4PI steredian in deg^2
        double arcSecArea = skyArea*3600.*3600.;  // 4PI steredian in (arcSec^2)
        long npixels = 12*nside*nside;
        res = arcSecArea/npixels;       // area per pixel
        res = Math.sqrt(res);           // angular size of the pixel arcsec
        return res;
    }
    /**
     * calculate requared nside given pixel size in arcsec
     * @param pixsize in arcsec
     * @return long nside parameter
     */
    static public long GetNSide(double pixsize) {
    	long[] nsidelist = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048,
				4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576 };
    	long res = 0;
    	double pixelArea = pixsize*pixsize;
    	double degrad = Math.toDegrees(1.);
    	double skyArea = 4.*PI*degrad*degrad*3600.*3600.;
    	long npixels = (long) (skyArea/pixelArea);
    	long nsidesq = npixels/12;
    	long nside_req = (long) Math.sqrt(nsidesq);
    	long mindiff = ns_max;
    	int indmin = 0;
    	for (int i=0; i<nsidelist.length; i++) {
    		if (Math.abs(nside_req - nsidelist[i]) <= mindiff) {
    			mindiff = Math.abs(nside_req - nsidelist[i]);
    			res = nsidelist[i];
    			indmin = i;
    		}
    		if ((nside_req > res) && (nside_req < ns_max)) res = nsidelist[indmin+1];
    	   	if (nside_req > ns_max ) {
        		System.out.println("nside cannot be bigger than "+ns_max);
        		return ns_max;
        	}
    		
    	}
    	return res;
    }
    
    /**
     * returns polar coordinates in radians given ra, dec in degrees
     * @param radec double array containing ra,dec in degrees
     * @return res double array containing theta and phi in radians
     *             res[0] = theta res[1] = phi
     */
    static public double[] RaDecToPolar(double[] radec) {
       double[] res = new double[2];
       res[0] = PI/2. - radec[1]/180.*PI;
       res[1] = radec[0]/180.*PI;
       return res;
    }
    
    /**
     * returns ra, dec in degrees given polar coordinates in radians
     * @param polar double array polar[0] = phi in radians
     *                           polar[1] = theta in radians
     * @return double array radec radec[0] = ra in degrees
     *                radec[1] = dec in degrees
     */
    static public double[] PolarToRaDec(double[] polar) {
       return PolarToRaDec(polar,new double[2]);
    }

    static public double[] PolarToRaDec(double[] polar,double radec[]) {
       radec[1] = (PI/2. - polar[0])*180./PI;
       radec[0] = polar[1]*180./PI;
       return radec;
    }
   

    /*
	 * nside1 < nside2
	 * n begins to 0
	 */
	public static long getHealpixMax(int n1, long n, int n2, boolean nside) {
		if (nside)
			return (n+1)*(long)(Math.pow(4,(log2(n2) - log2(n1))/LOG2OF2)) -1;
		else
			return (n+1)*(long)(Math.pow(4,(n2 - n1)/LOG2OF2)) -1;
	}
	/*
	 * nside1 < nside2
	 */
	public static long getHealpixMin(int n1, long n, int n2, boolean nside) {
		if (nside)
			return n*(long)(Math.pow(4,(log2(n2) - log2(n1))/LOG2OF2));
		else
			return n*(long)(Math.pow(4,(n2 - n1)/LOG2OF2));
	}
	
	static final double LOG2 = Math.log(2);
	static final double LOG2OF2 = log2(2);
	
	public static long log2 (long x) {
		return (long) (Math.log(x)/LOG2);
	}

	public static ArrayList getNeighbours(long nside, long ipix)  {
		ArrayList res = new ArrayList();
		long npix, ipf, ipo, ix, ixm, ixp, iy, iym, iyp, ixo, iyo;
		long face_num, other_face;
		long ia, ib, ibp, ibm, ib2,  nsidesq;
        int icase;
		long local_magic1, local_magic2;
		long arb_const = 0;
		long[] ixiy = new long[2];
		long[] ixoiyo = new long[2];
		/* fill the pixel list with 0 */
		res.add(0, new Long(0));
		res.add(1, new Long(0));
		res.add(2, new Long(0));
		res.add(3, new Long(0));
		res.add(4, new Long(0));
		res.add(5, new Long(0));
		res.add(6, new Long(0));
		res.add(7, new Long(0));
		icase = 0;
		/*                                 */
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException("neighbours_nest: Nside should be power of 2 >0 and < "+ns_max);
		}
		nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if ((ipix < 0) || (ipix > npix - 1)) {
			throw new IllegalArgumentException("neighbours_nest: ipix out of range ");
		}
		if (x2pix[xmax-1] <= 0) mk_xy2pix();

		local_magic1 = (nsidesq - 1) / 3;
		local_magic2 = 2 * local_magic1;
		face_num = ipix / nsidesq;
		ipf = (long) bm.MODULO(ipix, nsidesq); // Pixel number in face
		ixiy = pix2xy_nest(nside, ipf);
		ix = ixiy[0];
		iy = ixiy[1];
		//
		ixm = ixiy[0] - 1;
		ixp = ixiy[0] + 1;
		iym = ixiy[1] - 1;
		iyp = ixiy[1] + 1;

		icase = 0; // inside the face

		/* exclude corners */
		if (ipf == local_magic2 && icase == 0)
			icase = 5; // West corner
		if (ipf == (nsidesq - 1) && icase == 0)
			icase = 6; // North corner
		if (ipf == 0 && icase == 0)
			icase = 7; // South corner
		if (ipf == local_magic1 && icase == 0)
			icase = 8; // East corner

		/* detect edges */
		if ((ipf & local_magic1) == local_magic1 && icase == 0)
			icase = 1; // NorthEast
		if ((ipf & local_magic1) == 0 && icase == 0)
			icase = 2; // SouthWest
		if ((ipf & local_magic2) == local_magic2 && icase == 0)
			icase = 3; // NorthWest
		if ((ipf & local_magic2) == 0 && icase == 0)
			icase = 4; // SouthEast

		/* iside a face */
		if (icase == 0) {
			res.add(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
			res.add(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
			res.add(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
			res.add(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
			res.add(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
			res.add(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
			res.add(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
			res.add(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
		}
		/*                 */
		ia = face_num / 4; // in [0,2]
		ib = (long) bm.MODULO(face_num, 4); // in [0,3]
		ibp = (long) bm.MODULO(ib + 1, 4);
		ibm = (long) bm.MODULO(ib + 4 - 1, 4);
		ib2 = (long) bm.MODULO(ib + 2, 4);

		if (ia == 0) { // North pole region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 4 + ib;
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long( (other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 0 + ibm;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // E-W flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 4 + ibp;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( arb_const));
				res.set(3, new Long( (arb_const + 2)));
				res.set(4, new Long( (ipix + 1)));
				res.set(5, new Long( (ipix - 1)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long( (ipix - 1)));
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(2, new Long( (arb_const - 2)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib2;
				res.set(4, new Long( (other_face * nsidesq + nsidesq - 1)));
				other_face = 0 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(5, new Long( arb_const));
				res.set(6, new Long( (arb_const - 1)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib;
				res.set(0, new Long( (other_face * nsidesq + nsidesq - 1)));
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(1, new Long( arb_const));
				res.set(2, new Long( (arb_const + 2)));
				res.set(3, new Long( (ipix + 2)));
				res.set(4, new Long( (ipix + 3)));
				res.set(5, new Long( (ipix + 1)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, new Long( (arb_const + 1)));
				res.set(7, new Long( arb_const));
				break;
			case 8: // East corner
				other_face = 0 + ibp;
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, new Long( (arb_const + 1)));
				res.set(5, new Long(( arb_const)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 1)));
				res.set(6, new Long( arb_const));
				res.remove(7);
				break;
			}
		} else if (ia == 1) { // Equatorial region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long((other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NortWest edge
				other_face = 0 + ibm;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // NW-SE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(2, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long(xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ib;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 4 + ibm;
				res.set(2, new Long( (other_face * nsidesq + local_magic1)));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq;
				res.set(3, new Long( arb_const));
				res.set(4, new Long( (arb_const + 1)));
				res.set(5, new Long( (ipix + 1)));
				res.set(6, new Long( (ipix - 1)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long( (ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( (arb_const - 1)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, new Long( arb_const));
				res.set(5, new Long( (arb_const - 2)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 7: // South corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, new Long( arb_const));
				res.set(1, new Long( (arb_const + 2)));
				res.set(2, new Long( (ipix + 2)));
				res.set(3, new Long( (ipix + 3)));
				res.set(4, new Long( (ipix + 1)));
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, new Long( (arb_const + 1)));
				res.set(6, new Long( arb_const));
				res.remove(7);
				break;
			case 8: // East corner
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, new Long( (arb_const - 1)));
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				res.set(7, new Long( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq;
				res.set(4, new Long( (arb_const + 2)));
				res.set(5, new Long( arb_const));
				other_face = 4 + ibp;
				res.set(6, new Long( (other_face * nsidesq + local_magic2)));
				break;
			}
		} else { // South pole region
			switch (icase) {
			case 1: // North-East edge
				other_face = 4 + ibp;
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) bm.MODULO(bm.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, new Long( (other_face * nsidesq + ipo)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // W-E flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, new Long( (other_face * nsidesq + ipo)));
				res.set(2, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long(xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 4 + ib;
				ipo = (long) bm.MODULO(bm.invMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, new Long( (other_face * nsidesq + ipo)));
				res.set(4, new Long( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, new Long( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ibp;
				ipo = (long) bm.MODULO(bm.swapLSBMSB( ipf), nsidesq); // SE-NW
				// flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, new Long( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, new Long( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, new Long( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, new Long( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, new Long( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, new Long( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, new Long( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(7, new Long( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, new Long( (arb_const - 2)));
				res.set(1, new Long( arb_const));
				other_face = 4 + ib;
				res.set(2, new Long( (other_face * nsidesq)));
				res.set(3, new Long( (other_face * nsidesq + 1)));
				res.set(4, new Long( (ipix + 1)));
				res.set(5, new Long( (ipix - 1)));
				res.set(6, new Long( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 4 + ib;
				res.set(0, new Long( (ipix - 3)));
				res.set(1, new Long((ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, new Long( (arb_const - 1)));
				res.set(3, new Long( arb_const));
				other_face = 0 + ib;
				res.set(4, new Long( (other_face * nsidesq)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, new Long( arb_const));
				res.set(6, new Long( (arb_const - 2)));
				res.set(7, new Long( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib2;
				res.set(0, new Long( (other_face * nsidesq)));
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq;
				res.set(1, new Long( arb_const));
				res.set(2, new Long( (arb_const + 1)));
				res.set(3, new Long( (ipix + 2)));
				res.set(4, new Long( (ipix + 3)));
				res.set(5, new Long( (ipix + 1)));
				other_face = 8 + ibp;
				arb_const = other_face * nsidesq;
				res.set(6, new Long( (arb_const + 2)));
				res.set(7, new Long( arb_const));
				break;
			case 8: // East corner
				other_face = 8 + ibp;
				res.set(1, new Long( (ipix - 1)));
				res.set(2, new Long( (ipix + 1)));
				res.set(3, new Long( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, new Long( arb_const));
				res.set(0, new Long( (arb_const - 2)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq;
				res.set(4, new Long( (arb_const + 2)));
				res.set(5, new Long( arb_const));
				res.remove(7);
				break;
			}
		}
		
	return res;
	}
    
	public static void main(String[] args) {
		System.out.println(PixRes(1024*8));
	}
}