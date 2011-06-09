//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//

package org.asterope.healpix;

import java.util.ArrayList;


/** Contains everything related to nested code.
 * Extracted from PixTools to make it smaller.
 * <p>
 * !!! IS NOT THREAD SAFE !!! 
 */
public class PixToolsNested extends PixTools {
	
	// coordinates of lowest corner of each face
	private static final long[] jrll = { 0, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4 }; // in units of NSIDE

	private static final long[] jpll = { 0, 1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7 }; // in units of NSIDE/2


	public LongRangeSet query_strip_nested(long nside, double theta1, double theta2)
			throws Exception {
				LongRangeIterator iter = query_strip(nside, theta1, theta2).rangeIterator();
				return ringIterator2nested(nside, iter);
			}

	public LongRangeSet query_polygon_nested(long nside, ArrayList<PixToolsVector3d> vlist, long inclusive)
			throws Exception {
			LongRangeIterator iter = query_polygon(nside, vlist, inclusive).rangeIterator();
				return ringIterator2nested(nside,iter);
			}

	private LongRangeSet ringIterator2nested(long nside, LongRangeIterator iter) {
		LongSet s = new LongSet();
		while(iter.moveToNext()){
			//long nestIpix = ring2nest(nside, iter.first());
			for(long ipix = iter.first(); ipix<=iter.last();ipix++){
				s.add(ring2nest(nside,ipix));				
				//s.add(nestIpix);
				//nestIpix = next_in_line_nest(nside, nestIpix);
				//TODO this can be optimized with next_in_line, but it seems to be failing
			}

			
		}
		return s.toLongRangeSet();
	}

	public LongRangeSet query_triangle_nested(long nside, PixToolsVector3d v1, PixToolsVector3d v2,
				PixToolsVector3d v3, long inclusive) throws Exception {
				LongRangeIterator iter = query_triangle(nside, v1, v2, v3, inclusive).rangeIterator();
				return ringIterator2nested(nside, iter);
			}

	public LongRangeSet query_disc_nested(long nside, PixToolsVector3d vector, double radius,
			boolean inclusive) {
				LongRangeIterator iter = query_disc(nside, vector, radius, inclusive).rangeIterator();
				return ringIterator2nested(nside, iter);
			}

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
	public double[] pix2ang_nest(long nside, long ipix) {
		return pix2ang_ring(nside,nest2ring(nside, ipix));
	}

	/**
	 * renders vector (x,y,z) coordinates of the nominal pixel center for the
	 * pixel ipix (NESTED scheme ) given the map resolution parameter nside.
	 * Also calculates the (x,y,z) positions of 4 pixel vertices (corners) in
	 * the order N,W,S,E. These can be get using method pix2vertex_nest.
	 * 
	 * @param nside the map resolution 
	 * @param ipix long pixel number
	 * @return PixToolsVector3d
	 * @throws IllegalArgumentException
	 */
	public PixToolsVector3d pix2vect_nest(long nside, long ipix) {
		return makePix2Vect_ring(nside, nest2ring(nside, ipix)).centre;		
	}

	/**
	 * renders vector (x,y,z) coordinates of the nominal pixel center for the
	 * pixel ipix (NESTED scheme ) given the map resolution parameter nside.
	 * Also calculates the (x,y,z) positions of 4 pixel vertices (corners) in
	 * the order N,W,S,E.
	 * 
	 * @param nside the map resolution
	 * @param ipix long pixel number
	 * @return double[3][4] 4 sets of vector components
	 * @throws IllegalArgumentException
	 */
	public double[][] pix2vertex_nest(long nside, long ipix) {
		double[][] res = new double[3][4];
		double[][] pixVertex = makePix2Vect_ring(nside, nest2ring(nside,ipix)).borders;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++) {
				res[i][j] = pixVertex[i][j];
			}
		}
		return res;
	}


	/**
	 * renders the pixel number pix (NESTED scheme) for a pixel which contains a
	 * point on a sphere at coordinates theta and phi, given map resolution
	 * parameter nside.
	 * 
	 * The computation is made to the highest resolution available and then
	 * degraded to required resolution by integer division. It makes sure that
	 * the treatment of round-off will be consistent for every resolution.
	 * 
	 * @param nside the map resolution parameter
	 * @param theta double theta coordinate in radians
	 * @param phi double phi coordinate in radians
	 * @return pixel number long 
	 * @throws IllegalArgumentException
	 */
	public long ang2pix_nest(long nside, double theta, double phi) {
		return ring2nest(nside, ang2pix_ring(nside, theta, phi));
	}

	/**
	 * make the conversion NEST to RING
	 * 
	 * @param nside the map resolution parameter
	 * @param map Object[] the map in NESTED scheme
	 * @return - Object[] a map in RING scheme
	 * @throws IllegalArgumentException
	 */
	public Object[] convert_nest2ring(long nside, Object[] map) {
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
	public Object[] convert_ring2nest(long nside, Object[] map) {
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
	 * returns 7 or 8 neighbours of any pixel in the nested scheme The neighbours
	 * are ordered in the following way: First pixel is the one to the south (
	 * the one west of the south direction is taken for pixels that don't have a
	 * southern neighbour). From then on the neighbors are ordered in the
	 * clockwise direction.
	 * 
	 * @param nside the map resolution
	 * @param ipix long pixel number
	 * @return LongList
	 * @throws IllegalArgumentException
	 */
	public LongList neighbours_nest(long nside, long ipix) {
		LongList res = new LongList();
		long npix, ipf, ipo, ix, ixm, ixp, iy, iym, iyp, ixo, iyo;
		long face_num, other_face;
		long ia, ib, ibp, ibm, ib2,  nsidesq;
	    int icase;
		long local_magic1, local_magic2;
		long arb_const = 0;
		long[] ixiy = new long[2];
		long[] ixoiyo = new long[2];
		String SID = "neighbours_nest:";
		/* fill the pixel list with 0 */
		res.add(0, 0);
		res.add(1, 0);
		res.add(2, 0);
		res.add(3, 0);
		res.add(4, 0);
		res.add(5, 0);
		res.add(6, 0);
		res.add(7, 0);
		icase = 0;
		/*                                 */
		if ((nside < 1) || (nside > ns_max)) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}
		nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if ((ipix < 0) || (ipix > npix - 1)) {
			throw new IllegalArgumentException(SID + " ipix out of range ");
		}
		if (x2pix[xmax-1] <= 0)
			mk_xy2pix();
	
		local_magic1 = (nsidesq - 1) / 3;
		local_magic2 = 2 * local_magic1;
		face_num = ipix / nsidesq;
		ipf = (long) BitManipulation.MODULO(ipix, nsidesq); // Pixel number in face
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
			res.add(0,  xy2pix_nest(nside, ixm, iym, face_num));
			res.add(1,  xy2pix_nest(nside, ixm, iy, face_num));
			res.add(2,  xy2pix_nest(nside, ixm, iyp, face_num));
			res.add(3,  xy2pix_nest(nside, ix, iyp, face_num));
			res.add(4,  xy2pix_nest(nside, ixp, iyp, face_num));
			res.add(5,  xy2pix_nest(nside, ixp, iy, face_num));
			res.add(6,  xy2pix_nest(nside, ixp, iym, face_num));
			res.add(7,  xy2pix_nest(nside, ix, iym, face_num));
		}
		/*                 */
		ia = face_num / 4; // in [0,2]
		ib = (long) BitManipulation.MODULO(face_num, 4); // in [0,3]
		ibp = (long) BitManipulation.MODULO(ib + 1, 4);
		ibm = (long) BitManipulation.MODULO(ib + 4 - 1, 4);
		ib2 = (long) BitManipulation.MODULO(ib + 2, 4);
	
		if (ia == 0) { // North pole region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, ( (other_face * nsidesq + ipo)));
				res.set(6, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 4 + ib;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, ( (other_face * nsidesq + ipo)));
				res.set(2, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 0 + ibm;
				ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq); // E-W flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(3, ( (other_face * nsidesq + ipo)));
				res.set(4, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 4 + ibp;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, ( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, ( (arb_const - 2)));
				res.set(1, ( arb_const));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, ( arb_const));
				res.set(3, ( (arb_const + 2)));
				res.set(4, ( (ipix + 1)));
				res.set(5, ( (ipix - 1)));
				res.set(6, ( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, ( (ipix - 3)));
				res.set(1, ( (ipix - 1)));
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(2, ( (arb_const - 2)));
				res.set(3, ( arb_const));
				other_face = 0 + ib2;
				res.set(4, ( (other_face * nsidesq + nsidesq - 1)));
				other_face = 0 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(5, ( arb_const));
				res.set(6, ( (arb_const - 1)));
				res.set(7, ( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib;
				res.set(0, ( (other_face * nsidesq + nsidesq - 1)));
				other_face = 4 + ib;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(1, ( arb_const));
				res.set(2, ( (arb_const + 2)));
				res.set(3, ( (ipix + 2)));
				res.set(4, ( (ipix + 3)));
				res.set(5, ( (ipix + 1)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, ( (arb_const + 1)));
				res.set(7, ( arb_const));
				break;
			case 8: // East corner
				other_face = 0 + ibp;
				res.set(1, ( (ipix - 1)));
				res.set(2, ( (ipix + 1)));
				res.set(3, ( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, ( (arb_const + 1)));
				res.set(5, (( arb_const)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, ( (arb_const - 1)));
				res.set(6, ( arb_const));
				res.remove(7);
				break;
			}
		} else if (ia == 1) { // Equatorial region
			switch (icase) {
			case 1: // north-east edge
				other_face = 0 + ibp;
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) BitManipulation.MODULO(BitManipulation.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, ( (other_face * nsidesq + ipo)));
				res.set(6, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invLSB( ipf), nsidesq); // SW-NE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, ((other_face * nsidesq + ipo)));
				res.set(2, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NortWest edge
				other_face = 0 + ibm;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq); // NW-SE flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(2, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, ( (other_face * nsidesq + ipo)));
				res.set(4, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, (xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ib;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq); // SE-NW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(7, ( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, ( (arb_const - 2)));
				res.set(1, ( arb_const));
				other_face = 4 + ibm;
				res.set(2, ( (other_face * nsidesq + local_magic1)));
				other_face = 0 + ibm;
				arb_const = other_face * nsidesq;
				res.set(3, ( arb_const));
				res.set(4, ( (arb_const + 1)));
				res.set(5, ( (ipix + 1)));
				res.set(6, ( (ipix - 1)));
				res.set(7, ( (ipix - 2)));
				break;
			case 6: //  North corner
				other_face = 0 + ibm;
				res.set(0, ( (ipix - 3)));
				res.set(1, ( (ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, ( (arb_const - 1)));
				res.set(3, ( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(4, ( arb_const));
				res.set(5, ( (arb_const - 2)));
				res.set(6, ( (ipix - 2)));
				res.remove(7);
				break;
			case 7: // South corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, ( arb_const));
				res.set(1, ( (arb_const + 2)));
				res.set(2, ( (ipix + 2)));
				res.set(3, ( (ipix + 3)));
				res.set(4, ( (ipix + 1)));
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, ( (arb_const + 1)));
				res.set(6, ( arb_const));
				res.remove(7);
				break;
			case 8: // East corner
				other_face = 8 + ib;
				arb_const = other_face * nsidesq + nsidesq - 1;
				res.set(0, ( (arb_const - 1)));
				res.set(1, ( (ipix - 1)));
				res.set(2, ( (ipix + 1)));
				res.set(3, ( (ipix + 2)));
				res.set(7, ( arb_const));
				other_face = 0 + ib;
				arb_const = other_face * nsidesq;
				res.set(4, ( (arb_const + 2)));
				res.set(5, ( arb_const));
				other_face = 4 + ibp;
				res.set(6, ( (other_face * nsidesq + local_magic2)));
				break;
			}
		} else { // South pole region
			switch (icase) {
			case 1: // North-East edge
				other_face = 4 + ibp;
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				ipo = (long) BitManipulation.MODULO(BitManipulation.invLSB( ipf), nsidesq); // NE-SW flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(4, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(5, ( (other_face * nsidesq + ipo)));
				res.set(6, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				break;
			case 2: // SouthWest edge
				other_face = 8 + ibm;
				ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq); // W-E flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(1, ( (other_face * nsidesq + ipo)));
				res.set(2, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, (xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 3: // NorthWest edge
				other_face = 4 + ib;
				ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq);
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixm, iym, face_num)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixo - 1, iyo,
						other_face)));
				res.set(3, ( (other_face * nsidesq + ipo)));
				res.set(4, ( xy2pix_nest(nside, ixo + 1, iyo,
						other_face)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixp, iym, face_num)));
				res.set(7, ( xy2pix_nest(nside, ix, iym, face_num)));
				break;
			case 4: // SouthEast edge
				other_face = 8 + ibp;
				ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq); // SE-NW
				// flip
				ixoiyo = pix2xy_nest(nside, ipo);
				ixo = ixoiyo[0];
				iyo = ixoiyo[1];
				res.set(0, ( xy2pix_nest(nside, ixo, iyo - 1,
						other_face)));
				res.set(1, ( xy2pix_nest(nside, ixm, iy, face_num)));
				res.set(2, ( xy2pix_nest(nside, ixm, iyp, face_num)));
				res.set(3, ( xy2pix_nest(nside, ix, iyp, face_num)));
				res.set(4, ( xy2pix_nest(nside, ixp, iyp, face_num)));
				res.set(5, ( xy2pix_nest(nside, ixp, iy, face_num)));
				res.set(6, ( xy2pix_nest(nside, ixo, iyo + 1,
						other_face)));
				res.set(7, ( (other_face * nsidesq + ipo)));
				break;
			case 5: // West corner
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq + local_magic1;
				res.set(0, ( (arb_const - 2)));
				res.set(1, ( arb_const));
				other_face = 4 + ib;
				res.set(2, ( (other_face * nsidesq)));
				res.set(3, ( (other_face * nsidesq + 1)));
				res.set(4, ( (ipix + 1)));
				res.set(5, ( (ipix - 1)));
				res.set(6, ( (ipix - 2)));
				res.remove(7);
				break;
			case 6: //  North corner
				other_face = 4 + ib;
				res.set(0, ( (ipix - 3)));
				res.set(1, ((ipix - 1)));
				arb_const = other_face * nsidesq + local_magic1;
				res.set(2, ( (arb_const - 1)));
				res.set(3, ( arb_const));
				other_face = 0 + ib;
				res.set(4, ( (other_face * nsidesq)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq + local_magic2;
				res.set(5, ( arb_const));
				res.set(6, ( (arb_const - 2)));
				res.set(7, ( (ipix - 2)));
				break;
			case 7: // South corner
				other_face = 8 + ib2;
				res.set(0, ( (other_face * nsidesq)));
				other_face = 8 + ibm;
				arb_const = other_face * nsidesq;
				res.set(1, ( arb_const));
				res.set(2, ( (arb_const + 1)));
				res.set(3, ( (ipix + 2)));
				res.set(4, ( (ipix + 3)));
				res.set(5, ( (ipix + 1)));
				other_face = 8 + ibp;
				arb_const = other_face * nsidesq;
				res.set(6, ( (arb_const + 2)));
				res.set(7, ( arb_const));
				break;
			case 8: // East corner
				other_face = 8 + ibp;
				res.set(1, ( (ipix - 1)));
				res.set(2, ( (ipix + 1)));
				res.set(3, ( (ipix + 2)));
				arb_const = other_face * nsidesq + local_magic2;
				res.set(6, ( arb_const));
				res.set(0, ( (arb_const - 2)));
				other_face = 4 + ibp;
				arb_const = other_face * nsidesq;
				res.set(4, ( (arb_const + 2)));
				res.set(5, ( arb_const));
				res.remove(7);
				break;
			}
		}
		return res;
	}

	public LongRangeSet InRing_nested(long nside, long iz, double phi0,
			double dphi) {
				LongRangeIterator iter = InRing(nside, iz, phi0, dphi).rangeIterator();
				return ringIterator2nested(nside, iter);
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
	public long next_in_line_nest(long nside, long ipix) {
			long npix, ipf, ipo, ix, ixp, iy, iym, ixo, iyo, face_num, other_face;
			long ia, ib, ibp, nsidesq;
	//		long ibm, ib2;
	        int icase;
			long local_magic1, local_magic2;
			long[] ixiy = new long[2];
			long inext = 0; // next in line pixel in Nest scheme
			String SID = "next_in_line:";
			if ((nside < 1) || (nside > ns_max)) {
				throw new IllegalArgumentException(SID + " nside should be power of 2 >0 and < "+ns_max);
			}
			nsidesq = nside * nside;
			npix = 12 * nsidesq; // total number of pixels
			if ((ipix < 0) || (ipix > npix - 1)) {
				throw new IllegalArgumentException(SID + " ipix out of range defined by nside");
			}
			// initiates array for (x,y) -> pixel number -> (x,y) mapping
			if (x2pix[xmax-1] <= 0)
				mk_xy2pix();
			local_magic1 = (nsidesq - 1) / 3;
			local_magic2 = 2 * local_magic1;
			face_num = ipix / nsidesq;
			ipf = (long) BitManipulation.MODULO(ipix, nsidesq); // Pixel number in face
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
			ib = (long) BitManipulation.MODULO(face_num, 4); // in [0,3]
			ibp = (long) BitManipulation.MODULO(ib + 1, 4);
	//		ibm = (long) BitManipulation.MODULO(ib + 4 - 1, 4);
	//		ib2 = (long) BitManipulation.MODULO(ib + 2, 4);
	
			if (ia == 0) { // North pole region
				switch (icase) {
				case 1:
					other_face = 0 + ibp;
					ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq);
					inext = other_face * nsidesq + ipo;
					break;
				case 4:
					other_face = 4 + ibp;
					ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq); // SE-NW flip
	
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
					ipo = (long) BitManipulation.MODULO((double)BitManipulation.invLSB( ipf), (double)nsidesq); // NE-SW flip
	//                System.out.println(" ipo="+ipo);
	                
					ixiy = pix2xy_nest(nside, ipo);
					ixo = ixiy[0];
					iyo = ixiy[1];
					inext = xy2pix_nest(nside, ixo, iyo - 1, other_face);
					break;
				case 4: // SouthEast edge
					other_face = 8 + ib;
					ipo = (long) BitManipulation.MODULO(BitManipulation.invMSB( ipf), nsidesq);
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
					ipo = (long) BitManipulation.MODULO(BitManipulation.invLSB( ipf), nsidesq); // NE-SW flip
					ixiy = pix2xy_nest(nside, ipo);
					inext = xy2pix_nest(nside, ixiy[0], ixiy[1] - 1, other_face);
					break;
				case 4: // SouthEast edge
					other_face = 8 + ibp;
					ipo = (long) BitManipulation.MODULO(BitManipulation.swapLSBMSB( ipf), nsidesq); // E-W flip
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
	 * renders the pixel number pix (NESTED scheme) for a pixel which contains a
	 * point on a sphere at coordinate vector (x,y,z), given the map resolution
	 * parameter nside.
	 * 
	 * The computation is made to the highest resolution available (nside=ns_max)
	 * and then degraded to that requared (by Integer division) this doesn't
	 * cost much, and it makes sure that the treatment of round-off will be
	 * consistent for every resolution
	 * 
	 * @param nside
	 *            long the map resolution
	 * @param vector
	 *            Vewctor3d the input vector
	 * @return pixel long
	 * @throws IllegalArgumentException
	 */
	public long vect2pix_nest(long nside, PixToolsVector3d vector) {
		return ring2nest(nside, vect2pix_ring(nside, vector));
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
	private long xy2pix_nest(long nside, long ix, long iy,
			long face_num) {
					long res = 0;
					int ix_low, ix_hi, iy_low, iy_hi;
			        long ipf;
					String SID = "xy2pix_nest:";
					//
					if ((nside < 1) || (nside > ns_max)) {
						throw new IllegalArgumentException(SID + " nside should be power of 2 >0 and < "+ns_max);
					}
			//		if ((ix < 0) || (ix > nside - 1)) {
			//			throw new IllegalArgumentException(SID + " ix out of range [0, nside-1]");
			//		}
			//		if ((iy < 0) || (iy > nside - 1)) {
			//			throw new IllegalArgumentException(SID + " iy out of range [0, nside-1]");
			//		}
					if (x2pix[xmax-1] <= 0)
						mk_xy2pix();
					ix_low = (int) BitManipulation.MODULO(ix, xmax);
					ix_hi = (int) (ix / xmax);
					iy_low = (int) BitManipulation.MODULO(iy, xmax);
					iy_hi = (int) (iy / xmax);
			
					ipf = (x2pix[ (ix_hi + 1)] + y2pix[(iy_hi + 1)]) * xmax * xmax
							+ (x2pix[ (ix_low + 1)] + y2pix[ (iy_low + 1)]);
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
	private long[] pix2xy_nest(long nside, long ipf) {
			long[] ixiy = { 0, 0 };
			int ip_low, ip_trunc, ip_med, ip_hi;
			String SID = "pix2xy_nest:";
	//        System.out.println(" ipf="+ipf+" nside="+nside);
			if ((nside < 1) || (nside > ns_max)) {
				throw new IllegalArgumentException(SID + " nside should be power of 2 >0 and < "+ns_max);
			}
			if ((ipf < 0) || (ipf > nside * nside - 1)) {
				throw new IllegalArgumentException(SID + " ipix out of range defined by nside");
			}
			if (pix2x[pixmax] <= 0)
				mk_pix2xy();
			ip_low = (int) BitManipulation.MODULO(ipf, pixmax); // contents of last 15 bits
			ip_trunc = (int) (ipf / pixmax); // truncation of the last 15 bits
			ip_med = (int) BitManipulation.MODULO(ip_trunc, pixmax); // next 15 bits
			ip_hi = ip_trunc / pixmax; // select high 15 bits
	
			long ix = pixmax * pix2x[ ip_hi] + xmid * pix2x[ip_med] + pix2x[ ip_low];
			long iy = pixmax * pix2y[ip_hi] + xmid * pix2y[ ip_med] + pix2y[ ip_low];
			ixiy[0] = ix;
			ixiy[1] = iy;
			return ixiy;
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
	public long ring2nest(long nside, long ipring) {
			long ipnest = 0;
			double fihip;
			double hip;
			long npix, nl2, nl4, ncap, ip, iphi, ipt, ipring1, kshift, face_num;
			long nr, irn, ire, irm, irs, irt, ifm, ifp, ix, iy, ix_low, ix_hi, iy_low;
			long iy_hi, ipf;
			String SID = "ring2nest:";
			//
			face_num = 0;
			if ((nside < 1) || (nside > ns_max)) {
				throw new IllegalArgumentException(SID + " nside should be power of 2 >0 and < "+ns_max);
			}
			npix = 12 * nside * nside; // total number of points
	
			if ((ipring < 0) || (ipring > npix - 1)) {
				throw new IllegalArgumentException(SID + " ipring out of range [0,npix-1]");
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
				fihip = Math.floor(hip);
				irn = (long)( Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from
				// north pole
				iphi = ipring1 - 2 * irn * (irn - 1);
	
				kshift = 0;
				nr = irn; // 1/4 of the number of points on the current ring
				face_num = (iphi - 1) / irn; // in [0,3 ]
				
			} else if (ipring1 <= nl2 * (5 * nside + 1)) { // equatorial region		
				ip = ipring1 - ncap - 1;
				irn = (long)(ip / nl4) + nside; // counted from north pole
				iphi = (long) BitManipulation.MODULO(ip, nl4) + 1;
	
				kshift = (long) BitManipulation.MODULO(irn + nside, 2); // 1 if odd 0
				// otherwise
				nr = nside;
				ire = irn - nside + 1; // in [1, 2*nside+1]
				irm = nl2 + 2 - ire;
				ifm = (iphi - ire / 2 + nside - 1) / nside; // face boundary
				ifp = (iphi - irm / 2 + nside - 1) / nside;
				if (ifp == ifm) {
					face_num = (long) BitManipulation.MODULO(ifp, 4.) + 4;
				} else if (ifp + 1 == ifm) { // (half-) faces 0 to 3
					face_num = ifp;
				} else if (ifp - 1 == ifm) { // (half-) faces 8 to 11
					face_num = ifp + 7;
				}
			
			
			} else { // south polar cap
				
				ip = npix - ipring1 + 1;
				hip = ip / 2.0;
				fihip = Math.floor(hip);
				irs = (long)( Math.sqrt(hip - Math.sqrt(fihip))) + 1;
				iphi = 4 * irs + 1 - (ip - 2 * irs * (irs - 1));
				kshift = 0;
				nr = irs;
				irn = nl4 - irs;
				face_num = (iphi - 1) / irs + 8; // in [8,11]
				
				
			}
			// finds the (x,y) on the face
			
			
	//
			irt = irn - jrll[(int) (face_num + 1)] * nside + 1; // in [-nside+1,0]
			ipt = 2 * iphi - jpll[(int) (face_num + 1)] * nr - kshift - 1; // in [-nside+1,
			// nside-1]
	//
			if (ipt >= nl2){
				ipt = ipt - 8*nside; // for the face #4		
			}
			ix = (ipt - irt) / 2;
			iy = -(ipt + irt) / 2;
	
			ix_low = (long) BitManipulation.MODULO(ix, xmax);
			ix_hi = ix / xmax;
			iy_low = (long) BitManipulation.MODULO(iy, xmax);
			iy_hi = iy / xmax;
	
	          //
			
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
	public long nest2ring(long nside, long ipnest) {
			long res = 0;
			long npix, npface, face_num, ncap, n_before, ipf, ip_low, ip_trunc;
			long ip_med, ip_hi, ix, iy, jrt, jr, nr, jpt, jp, kshift, nl4;
	//		long[] ixiy = { 0, 0 };
			String SID = "nest2ring:";
			//
			if ((nside < 1) || (nside > ns_max)) {
				throw new IllegalArgumentException(SID + " nside should be power of 2 >0 and < ns_max");
			}
			npix = 12 * nside * nside;
			if ((ipnest < 0) || (ipnest > npix - 1)) {
				throw new IllegalArgumentException(SID + " ipnest out of range [0,npix-1]");
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
				ipf = (long) BitManipulation.MODULO(ipnest, npface); // pixel number in the face
			} else {
				ipf = ipnest;
			}
	
			// finds the x,y on the face
			//  from the pixel number
			ip_low = (long) BitManipulation.MODULO(ipf, pixmax); // last 15 bits
			if (ip_low < 0)
				ip_low = -ip_low;
	
			ip_trunc = ipf / pixmax; // truncate last 15 bits
			ip_med = (long) BitManipulation.MODULO(ip_trunc, pixmax); // next 15 bits
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
			kshift = (long) BitManipulation.MODULO(jr - nside, 2);
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

	public LongRangeSet ring2nestRanges(final long nside, final LongRangeSet ring) {
	    //LongRangeArraySet ret = new LongRangeArraySet();
	    final LongSet ret = new LongSet();
	    final LongRangeIterator iter = ring.rangeIterator();
	    while(iter.moveToNext()){
	        long nest = ring2nest(nside,iter.first());
	        ret.add(nest);
	        final long counter = iter.last() - iter.first();
	        for(long i = 0; i<counter; i++){
	             nest = next_in_line_nest(nside, nest);
	             ret.add(nest);
	        }
	    }
	    return ret.toLongRangeSet();
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
	public Object[] convert_inplace_long(String subcall, Object[] map) {
		Object[] res;
		long npix, nside;
		boolean[] check;
		long ilast, i1, i2;
		String SID = "convert_in_place:";
		Object pixbuf1, pixbuf2;
		npix = map.length;
		nside = (long) Math.sqrt(npix / 12.);
		if (nside > ns_max) {
			throw new IllegalArgumentException(SID + " Map is too big");
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

	
	protected final long[] x2pix = new long[xmax+1];

	protected final long[] y2pix = new long[xmax+1];

	protected final long[] pix2x = new long[pixmax+1];

	protected final long[] pix2y = new long[pixmax+1];

	public PixToolsNested() {
		for (int i = 0; i <= xmax; i++) {
			x2pix[i] = 0;
			y2pix[i] = 0;
		}
		for (int i = 0; i <= pixmax; i++) {
			pix2x[i] = 0;
			pix2y[i] = 0;
		}
	}

	
	
	/**
	 * creates an array of pixel numbers pix2x from x and y coordinates in the
	 * face. Suppose NESTED scheme of pixel ordering Bits corresponding to x and
	 * y are interleaved in the pixel number in even and odd bits.
	 */
	protected void mk_pix2xy() {
		long kpix, jpix, ix, iy, ip, id;
//		boolean flag = true;
		for (kpix = 0; kpix <= pixmax; kpix++) { // loop on pixel numbers
			jpix = kpix;
			ix = 0;
			iy = 0;
			ip = 1; // bit position in x and y

			while (jpix != 0) { // go through all the bits

				id = (long) BitManipulation.MODULO(jpix, 2); // bit value (in kpix), goes in x
				jpix /= 2;
				ix += id * ip;

				id = (long) BitManipulation.MODULO(jpix, 2); // bit value, goes in iy
				jpix /= 2;
				iy += id * ip;

				ip *= 2; // next bit (in x and y )
			}
 
			pix2x[(int) kpix] = ix; // in [0,pixmax]
			pix2y[(int) kpix] = iy; // in [0,pixmax]
			

		}
    //    System.out.println(kpix);
	}    

	/**
	 * fills arrays x2pix and y2pix giving the number of the pixel laying in
	 * (x,y). x and y are in [1,512] the pixel number is in [0, 512**2 -1]
	 * 
	 * if i-1 = sum_p=0 b_p*2^p then ix = sum+p=0 b_p*4^p iy = 2*ix ix + iy in
	 * [0,512**2 -1]
	 * 
	 */
	protected void mk_xy2pix() {
		long k, ip, id;

		for (int i = 1; i <= xmax; i++) {
			long j = i - 1;
			k = 0;
			ip = 1;
			while (j != 0) {
				id = (long) BitManipulation.MODULO(j, 2);
				j /= 2;
				k += ip * id;
				ip *= 4;
			}
			x2pix[i] = k;
			y2pix[i] = 2 * k;
			
		}

	}


}
