//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//


package org.asterope.healpix;


import java.util.ArrayList;
import java.util.Arrays;


/**
 * 
 *  contains methods translated from HEALPix Fortran90
 *  with increased map resolution in comparison to original Fortran code.
 *  <p>
 *  Is thread safe.
 *  
 * 
 * @author N Kuropatkin
 *   
 */
public class PixTools {
	

	static final class Pixel{
		final PixToolsVector3d centre;
		final double[][] borders;
		public Pixel(PixToolsVector3d centre, double[][] borders) {
			super();
			this.centre = centre;
			this.borders = borders;
		}
		
		
	}

	protected static final double twothird = 2. / 3.;

	protected static final double PI = Math.PI;

	protected static final double TWOPI = 2. * PI;

//	private static double FOURPI = 4. * PI;

	protected static final double HALFPI = PI / 2.0;

    protected static final int ns_max = 1048576; // 2^20

//
    protected static final  int xmax = 4096;
//
    protected static final int pixmax = 262144;

//
    protected static final int xmid = 512;   

	/**
	 * default constructor
	 * 
	 *  
	 */
	public PixTools() {
	}

	/**
	 * finds pixels having a colatitude (measured from North pole) : 
	 * theta1 < colatitude < theta2 with 0 <= theta1 < theta2 <= Pi 
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
	 * @return  LongList of  pixel numbers (long)
	 * @throws Exception 
	 * @throws IllegalArgumentException
	 */
	public LongRangeSet query_strip(long nside, double theta1, double theta2) throws Exception {
		LongRangeSetBuilder res = new LongRangeSetBuilder();
		long npix, nstrip;
		long iz,  irmin, irmax;
        int is;
		double phi0, dphi;
		double[] colrange = new double[4];
		String SID = " QUERY_STRIP";
		/* ---------------------------------------- */
		npix = Nside2Npix(nside);
		if (npix < 0) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2");
		}
		if ((theta1 < 0.0 || theta1 > PI) || (theta2 < 0.0 || theta2 > PI)) {
			throw new IllegalArgumentException(SID + " Illegal value of theta1, theta2");
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
				InRing(nside, iz, phi0, dphi,res);
			}
		}
		return res.build();
	}

	/**
	 * finds pixels that lay within a CONVEX polygon defined by its vertex on
	 * sphere
	 * 
	 * @param nside 
	 *            the map resolution
	 * @param vlist 
	 *            ArrayList of vectors defining the polygon vertices
	 * @param inclusive 
	 *            if set 1 returns all pixels crossed by polygon boundaries
	 * @return  LongList of pixels
	 * 
	 * algorithm: the polygon is divided into triangles vertex 0 belongs to all
	 * triangles
	 * @throws Exception 
	 * @throws IllegalArgumentException
	 */
	public LongRangeSet query_polygon(long nside, ArrayList<PixToolsVector3d> vlist, 
			long inclusive) throws Exception {
		LongSet res = new LongSet();
		int nv = vlist.size();
		PixToolsVector3d vp0, vp1, vp2;
		PixToolsVector3d vo;
		LongList vvlist = new LongList();
//		double surface, fsky;
		double hand;
		double[] ss = new double[nv];
//		int n_in_trg, ilist, ntl;
//        long npix;
		int ix = 0;
		
		int n_remain, np, nm, nlow;
		String SID = "QUERY_POLYGON";

		//		System.out.println("Start polygon");
		for (int k = 0; k < nv; k++)
			ss[k] = 0.;
		/* -------------------------------------- */
		n_remain = nv;
		if (n_remain < 3) {
			throw new IllegalArgumentException(SID + " Number of vertices should be >= 3");
		}
		/*---------------------------------------------------------------- */
		/* Check that the poligon is convex or has only one concave vertex */
		/*---------------------------------------------------------------- */
		int i0 = 0;
		int i2 = 0;
		if (n_remain > 3) { // a triangle is always convex
			for (int i1 = 1; i1 <= n_remain - 1; i1++) { // in [0,n_remain-1]
				i0 = (int) BitManipulation.MODULO(i1 - 1, n_remain);
				i2 = (int) BitManipulation.MODULO(i1 + 1, n_remain);
				vp0 =vlist.get(i0); // select vertices by 3
												// neighbour
				vp1 =vlist.get(i1);
				vp2 = vlist.get(i2);
				// computes handedness (v0 x v2) . v1 for each vertex v1
				vo = vp0.crossProduct(vp2);
				hand = vo.dot(vp1);
				if (hand >= 0.) {
					ss[i1] = 1.0;
				} else {
					ss[i1] = -1.0;
				}

			}
			np = 0; // number of vert. with positive handedness
			for (int i = 0; i < nv; i++) {
				if (ss[i] > 0.)
					np++;
			}
			nm = n_remain - np;

			nlow = Math.min(np, nm);

			if (nlow != 0) {
				if (nlow == 1) { // only one concave vertex
					if (np == 1) { // ix index of the vertex in the list
						for (int k = 0; k < nv - 1; k++) {
							if (Math.abs(ss[k] - 1.0) <= 1.e-12) {
								ix = k;
								break;
							}
						}
					} else {
						for (int k = 0; k < nv - 1; k++) {
							if (Math.abs(ss[k] + 1.0) <= 1.e-12) {
								ix = k;
								break;
							}
						}
					}

					// rotate pixel list to put that vertex in #0
					int n_rot = vlist.size() - ix;
					int ilast = vlist.size() - 1;
					for (int k = 0; k < n_rot; k++) {
						PixToolsVector3d temp = (PixToolsVector3d) vlist
								.get(ilast);
						vlist.remove(ilast);
						vlist.add(0,  temp);
					}
				}
				if (nlow > 1) { // more than 1concave vertex
					System.out
							.println(" The polygon has more than one concave vertex");
					System.out.println(" The result is unpredictable");
				}
			}
		}
		/* fill the polygon, one triangle at a time */
//		npix = Nside2Npix(nside);
		while (n_remain >= 3) {
			vp0 =  vlist.get(0);
			vp1 =  vlist.get(n_remain - 2);
			vp2 =  vlist.get(n_remain - 1);

			/* find pixels within the triangle */
			LongRangeSet templist = query_triangle(nside, vp0, vp1, vp2, inclusive);

			vvlist.addAll(templist.longIterator());
			n_remain--;
		}
		/* make final pixel list */
        res.addAll(vvlist);

		return res.toLongRangeSet();
	}

	/**
	 * generates a list of pixels that lay inside a triangle defined by
	 * the three vertex vectors
	 * 
	 * @param nside 
	 *            long map resolution parameter
	 * @param v1 
	 *            PixToolsVector3d defines one vertex of the triangle
	 * @param v2 
	 *            PixToolsVector3d another vertex
	 * @param v3 
	 *            PixToolsVector3d yet another one
	 * @param inclusive
	 *            long 0 (default) only pixels whose centers are inside the
	 *            triangle will be listed, if set to 1 all pixels overlaping the
	 *            triangle will be listed
	 * @return LongList with pixel numbers
	 * @throws Exception 
	 * @throws IllegalArgumentException
	 */
	public LongRangeSet query_triangle(long nside, PixToolsVector3d v1, PixToolsVector3d v2,
			PixToolsVector3d v3,  long inclusive) throws Exception {
		LongSet res = new LongSet(); //TODO organize pixels and use LongRangeSetBuilder instead
		long npix, iz, irmin, irmax, n12, n123a, n123b, ndom = 0;
		boolean test1, test2, test3;
//		boolean test1a, test1b, test2a, test2b, test3a, test3b;
		double dth1, dth2, determ, sdet;
		double zmax, zmin, z1max, z1min, z2max, z2min, z3max, z3min;
		double z, tgth, st, offset, sin_off;
		double phi_pos, phi_neg;
		PixToolsVector3d[] vv = new PixToolsVector3d[3];
		PixToolsVector3d[] vo = new PixToolsVector3d[3];
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
//		long nir, ip, status;
		boolean do_inclusive = false;
		String SID = "QUERY_TRIANGLE";
		long nsidesq = nside * nside;
		/*                                       */

		//		System.out.println("in query_triangle");
		npix = Nside2Npix(nside);
		if (npix < 0) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}
		if (inclusive == 1)
			do_inclusive = true;
		vv[0] = v1;
		vv[0] = vv[0].normalized();
		vv[1] = v2;
		vv[1] = vv[1].normalized();
		vv[2] = v3;
		vv[2] = vv[2].normalized();

		/*                                  */
		dth1 = 1.0 / (3.0 * nsidesq);
		dth2 = 2.0 / (3.0 * nside);
		/*
		 * determ = (v1 X v2) . v3 determines the left ( <0) or right (>0)
		 * handedness of the triangle
		 */
		PixToolsVector3d vt = new PixToolsVector3d(0., 0., 0.);
		vt = vv[0].crossProduct(vv[1]);
		determ = vt.dot(vv[2]);

		if (Math.abs(determ) < 1.0e-20) {
			throw new HealpixException(
					SID
							+ ": the triangle is degenerated - query cannot be performed");
		}
		if (determ >= 0.) { // The sign of determinant
			sdet = 1.0;
		} else {
			sdet = -1.0;
		}

		sprod[0] = vv[1].dot(vv[2]);
		sprod[1] = vv[2].dot( vv[0]);
		sprod[2] = vv[0].dot(vv[1]);
		/* vector orthogonal to the great circle containing the vertex doublet */

		vo[0] = vv[1].crossProduct( vv[2]);
		vo[1] = vv[2].crossProduct( vv[0]);
		vo[2] = vv[0].crossProduct( vv[1]);
		vo[0] = vo[0].normalized();
		vo[1] = vo[1].normalized();
		vo[2] = vo[2].normalized();

		/* test presence of poles in the triangle */
		zmax = -1.0;
		zmin = 1.0;
		test1 = (vo[0].z * sdet >= 0.0); // north pole in hemisphere defined by
		// 2-3
		test2 = (vo[1].z * sdet >= 0.0); // north pole in the hemisphere defined
		// by 1-2
		test3 = (vo[2].z * sdet >= 0.0); // north pole in hemisphere defined by
		// 1-3
		if (test1 && test2 && test3)
			zmax = 1.0; // north pole in the triangle
		if ((!test1) && (!test2) && (!test3))
			zmin = -1.0; // south pole in the triangle
		/* look for northenest and southernest points in the triangle */


		/* sin of theta for orthogonal vector */
		for (int i = 0; i < 3; i++) {
			sto[i] = Math.sqrt((1.0 - vo[i].z) * (1.0 + vo[i].z));
		}
		/*
		 * for each segment ( side of the triangle ) the extrema are either -
		 * -the 2 vertices - one of the vertices and a point within the segment
		 */
		// segment 2-3
		z1max = vv[1].z;
		z1min = vv[2].z;

		// segment 1-3
		z2max = vv[2].z;
		z2min = vv[0].z;

		// segment 1-2
		z3max = vv[0].z;
		z3min = vv[1].z;

		zmax = Math.max(Math.max(z1max, z2max), Math.max(z3max, zmax));
		zmin = Math.min(Math.min(z1min, z2min), Math.min(z3min, zmin));
		/*
		 * if we are inclusive, move upper point up, and lower point down, by a
		 * half pixel size
		 */
		offset = 0.0;
		sin_off = 0.0;
		if (do_inclusive) {
			offset = PI / (4.0 * nside); // half pixel size
			sin_off = Math.sin(offset);
			zmax = Math.min(1.0, Math.cos(Math.acos(zmax) - offset));
			zmin = Math.max(-1.0, Math.cos(Math.acos(zmin) + offset));
		}

		irmin = RingNum(nside, zmax);
		irmax = RingNum(nside, zmin);

		//		System.out.println("irmin = " + irmin + " irmax =" + irmax);

		/* loop on the rings */
		for (int i = 0; i < 3; i++) {
			tgthi[i] = -1.0e30 * vo[i].z;
			phi0i[i] = 0.0;
		}
		for (int j = 0; j < 3; j++) {
			if (sto[j] > 1.0e-10) {
				tgthi[j] = -vo[j].z / sto[j]; // - cotan(theta_orth)

				phi0i[j] = Math.atan2(vo[j].y, vo[j].x); // Should make it 0-2pi
														 // ?
				/* Bring the phi0i to the [0,2pi] domain if need */

				if (phi0i[j] < 0.) {
					phi0i[j] = BitManipulation.MODULO(
							(Math.atan2(vo[j].y, vo[j].x) + TWOPI), TWOPI); //  [0-2pi]
				}

			}
		}
		/*
		 * the triangle boundaries are geodesics: intersection of the sphere
		 * with plans going through (0,0,0) if we are inclusive, the boundaries
		 * are the intersection of the sphere with plains pushed outward by
		 * sin(offset)
		 */
//		double temp = 0.;
		boolean found = false;
		for (iz = irmin; iz <= irmax; iz++) {
			found = false;
			if (iz <= nside - 1) { // North polar cap
				z = 1.0 - iz * iz * dth1;
			} else if (iz <= 3 * nside) { // tropical band + equator
				z = (2.0 * nside - iz) * dth2;
			} else {
				z = -1.0 + (4.0 * nside - iz) * (4.0 * nside - iz) * dth1;
			}

			/* computes the 3 intervals described by the 3 great circles */
			st = Math.sqrt((1.0 - z) * (1.0 + z));
			tgth = z / st; // cotan(theta_ring)
			for (int j = 0; j < 3; j++) {
				dc[j] = tgthi[j] * tgth - sdet * sin_off
						/ ((sto[j] + 1.0e-30) * st);

			}
			for (int k = 0; k < 3; k++) {
				if (dc[k] * sdet <= -1.0) { // the whole iso-latitude ring is on
					// right side of the great circle
					dom[k][0] = 0.0;
					dom[k][1] = TWOPI;
				} else if (dc[k] * sdet >= 1.0) { // all on the wrong side
					dom[k][0] = -1.000001 * (k + 1);
					dom[k][1] = -1.0 * (k + 1);
				} else { // some is good some is bad
					phi_neg = phi0i[k] - (Math.acos(dc[k]) * sdet);
					phi_pos = phi0i[k] + (Math.acos(dc[k]) * sdet);
					//					
					if (phi_pos < 0.)
						phi_pos += TWOPI;
					if (phi_neg < 0.)
						phi_neg += TWOPI;

					//

					dom[k][0] = BitManipulation.MODULO(phi_neg, TWOPI);
					dom[k][1] = BitManipulation.MODULO(phi_pos, TWOPI);

				}
				//

			}
			/* identify the intersections (0,1,2 or 3) of the 3 intervals */

			dom12 = PixToolsUtils.intrs_intrv(dom[0], dom[1]);
			n12 = dom12.length / 2;
			if (n12 != 0) {
				if (n12 == 1) {
					dom123a = PixToolsUtils.intrs_intrv(dom[2], dom12);
					n123a = dom123a.length / 2;

					if (n123a == 0)
						found = true;
					if (!found) {
						for (int l = 0; l < dom123a.length; l++) {
							alldom[l] = dom123a[l];
						}

						ndom = n123a; // 1 or 2
					}
				}
				if (!found) {
					if (n12 == 2) {
						double[] tmp = { dom12[0], dom12[1] };
						dom123a = PixToolsUtils.intrs_intrv(dom[2], tmp);
						double[] tmp1 = { dom12[2], dom12[3] };
						dom123b = PixToolsUtils.intrs_intrv(dom[2], tmp1);
						n123a = dom123a.length / 2;
						n123b = dom123b.length / 2;
						ndom = n123a + n123b; // 0, 1, 2 or 3

						if (ndom == 0)
							found = true;
						if (!found) {
							if (n123a != 0) {
								for (int l = 0; l < 2 * n123a; l++) {
									alldom[l] = dom123a[l];
								}
							}
							if (n123b != 0) {
								for (int l = 0; l < 2 * n123b; l++) {
									alldom[(int) (l + 2 * n123a)] = dom123b[l];
								}
							}
							if (ndom > 3) {
								throw new HealpixException(SID
										+ ": too many intervals found");
							}
						}
					}
				}
				if (!found) {
					for (idom = 0; idom < ndom; idom++) {
						a_i = alldom[(int) (2 * idom)];
						b_i = alldom[(int) (2 * idom + 1)];
						phi0 = (a_i + b_i) / 2.0;
						dphiring = Math.abs(b_i - a_i) / 2.0;

						if (dphiring < 0.0) {
							phi0 += PI;
							dphiring += PI;
						}

						/* finds pixels in the triangle on that ring */
						LongRangeSet listir = InRing(nside, iz, phi0, dphiring);
						res.addAll(listir);

					}
				}
			}

		}
		return res.toLongRangeSet();
	}

	
	/**
	 * an obsolete method. Use query_disc instead.
	 * 
	 * @param nside
	 * @param vector0
	 * @param radius
	 * @return - LongList of long
	 */
	public LongRangeSet getDisc_ring(long nside, PixToolsVector3d vector0, double radius) {				
		return  query_disc(nside, vector0, radius, false);		
	}

	/**
	 * generates in the RING or NESTED scheme all pixels that lays within an
	 * angular distance Radius of the center.
	 * 
	 * @param nside 
	 *            long map resolution
	 * @param vector 
	 *            PixToolsVector3d pointing to the disc center
	 * @param radius 
	 *            double angular radius of the disc (in RADIAN )
	 * @param inclusive 
	 *            boolean false (default) only pixels whose centers lay in the disc
	 *            are listed, if set to true, all pixels overlapping the disc
	 *            are listed. In the inclusive mode the radius is increased by half the pixel size.
	 *            In this case most probably all neighbor pixels will be listed even with very small
	 *            radius.
	 *            In case of exclusive search and very small radius when the disc lays completely
	 *            inside a pixel the pixel number is returned using vector2pix method.
	 * @return  LongList of pixel numbers
	 * 
	 * calls: RingNum(nside, ir) InRing(nside, iz, phi0, dphi,nest) vector2pix(nside,ipix)
	 */
	public LongRangeSet query_disc(long nside, PixToolsVector3d vector, double radius,
			 boolean inclusive)  {
		LongRangeSetBuilder res = new LongRangeSetBuilder();
		long irmin, irmax, iz; 
//		long ip, nir, npix, ilist;

//		double norm_vect0;
		double x0, y0, z0, radius_eff;
		double a, b, c, cosang;
		double dth1, dth2;
		double phi0, cosphi0, cosdphi, dphi;
		double rlat0, rlat1, rlat2, zmin, zmax, z;
//		long status, list_size, nlost;
		String SID = "QUERY_DISC";
		/*                             */
//		long nsidesq = nside * nside;
//		npix = 12 * nsidesq;
		double pixres = PixRes(nside); // in arc seconds
		if (radius < 0.0 || radius > PI) {
			throw new IllegalArgumentException(SID 
					+ ": angular radius is in RADIAN and should be in [0,pi]");
		}

		dth1 = 1.0 / (3.0 * nside * nside);
		dth2 = 2.0 / (3.0 * nside);

		radius_eff = radius;
		
	//	System.out.println("dth1="+dth1+" dth2="+dth2+" radius="+radius);
		
		if (inclusive)
			radius_eff += PI / (4.0 * nside); // increase radius by half pixel
		cosang = Math.cos(radius_eff);
		/* disc center */
		vector = vector.normalized();
		x0 = vector.x; // norm_vect0;
		y0 = vector.y; // norm_vect0;
		z0 = vector.z; // norm_vect0;
//		System.out.println("x0="+x0+" y0="+y0+" z0="+z0);
		phi0 = 0.0;
		dphi = 0.0;
		if (x0 != 0. || y0 != 0.)
			phi0 = BitManipulation.MODULO(Math.atan2(y0, x0) + TWOPI, TWOPI);  // in [0, 2pi]
			cosphi0 = Math.cos(phi0);
//			System.out.println("phi0="+phi0+" cosphi0="+cosphi0);
		a = x0 * x0 + y0 * y0;
		/* coordinate z of highest and lowest points in the disc */
		rlat0 = Math.asin(z0); // latitude in RAD of the center
		rlat1 = rlat0 + radius_eff;
		rlat2 = rlat0 - radius_eff;
		//
		if (rlat1 >= HALFPI) {
			zmax = 1.0;
		} else {
			zmax = Math.sin(rlat1);
		}
		irmin = RingNum(nside, zmax);
		irmin = Math.max(1, irmin - 1); // start from a higher point to be safe
		if (rlat2 <= -HALFPI) {
			zmin = -1.0;
		} else {
			zmin = Math.sin(rlat2);
		}
		irmax = RingNum(nside, zmin);
		irmax = Math.min(4 * nside - 1, irmax + 1); // go down to a lower point
//		System.out.println(" irmax="+irmax+" irmin="+irmin);
//		ilist = -1;
		/* loop on ring number */
		for (iz = irmin; iz <= irmax; iz++) {
			if (iz <= nside - 1) { // north polar cap
				z = 1.0 - iz * iz * dth1;
			} else if (iz <= 3 * nside) { // tropical band + equator
				z = (2.0 * nside - iz) * dth2;
			} else {
				z = -1.0 + (4.0 * nside - iz) * (4.0 * nside - iz) * dth1;
			}
			/* find phi range in the disc for each z */
			b = cosang - z * z0;
			c = 1.0 - z * z;
			cosdphi = b / Math.sqrt(a * c);
			long done = 0;

			if (Math.abs(x0) <= 1.0e-12 && Math.abs(y0) <= 1.0e-12) {
				cosdphi = -1.0;
				dphi = PI;
				done = 1;
			}
			if (done == 0) {
				if (Math.abs(cosdphi) <= 1.0) {
					dphi = Math.acos(cosdphi); // in [0,pi]
				} else {
					if (cosphi0 >= cosdphi) {
						dphi = PI; // all the pixels at this elevation are in
						// the disc
					} else {
						done = 2; // out of the disc
					}
				}

			}
			if (done < 2) { // pixels in disc
				/* find pixels in the disc */
//				System.out.println("iz="+iz+" phi="+phi0+" dphi="+dphi);

				 InRing(nside, iz, phi0, dphi,res);				
			}
            
		}
//
// if no intersections and radius less than pixel size return the pixel number
//
		long pixel = 0;
		if (res.size() == 0 && pixres > Math.toDegrees(radius)/3600.) {
			pixel = vect2pix_ring(nside,vector);

			res.append(pixel);
		}
		
//make sure that center vector is contained in result set. 
//this is HOTFIX for rounding errors in InRing method
		LongRangeSet ret2 = res.build();
		long centerIpix = vect2pix_ring(nside,vector);
		if(!ret2.contains(centerIpix)){
			//construct new long range set and add it content to result
			LongRangeSetBuilder b2 = new LongRangeSetBuilder();
			b2.append(centerIpix);
			ret2 = ret2.union(b2.build());
		}
				
		return ret2;
		

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
	public double[] pix2ang_ring(long nside, long ipix)  {
		double[] res = { 0., 0. };
		long nl2, nl4, npix, ncap, iring, iphi, ip, ipix1;
		double fodd, hip, fihip, theta, phi;
		String SID = "pix2ang_ring:";
		/*                            */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}
		long nsidesq = nside * nside;
		npix = 12 * nsidesq; // total number of pixels
		if (ipix < 0 || ipix > npix - 1) {
			throw new IllegalArgumentException(SID + " ipix out of range calculated from nside");
		}
		ipix1 = ipix + 1; //  in [1, npix]
		nl2 = 2 * nside;
		nl4 = 4 * nside;
		ncap = 2 * nside * (nside - 1); // points in each polar cap, =0 for
		// nside =1

		if (ipix1 <= ncap) { // North polar cap
			hip = ipix1 / 2.0;
			fihip = (long) hip; // get integer part of hip
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from north
			                                                       // pole
			iphi = ipix1 - 2 * iring * (iring - 1);
			theta = Math.acos(1.0 - iring * iring / (3.0 * nsidesq));
			phi = ((double)iphi - 0.5) * PI / (2.0 * iring);


		} else if (ipix1 <= nl2 * (5 * nside + 1)) { // equatorial region
			ip = ipix1 - ncap - 1;
			iring = (long) (ip / nl4) + nside; // counted from North pole
			iphi = (long) BitManipulation.MODULO(ip, nl4) + 1;
			fodd = 0.5 * (1. + BitManipulation.MODULO(iring + nside, 2)); // 1 if iring+nside
			                                                 // is odd, 1/2 otherwise
			theta = Math.acos((nl2 - iring) / (1.5 * nside));
			phi = ((double)iphi - fodd) * PI / (2.0 * nside);

		} else { // South pole cap
			ip = npix - ipix1 + 1;
			hip = ip / 2.0;
			fihip = (long) hip;
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from South
			                                                       // pole
			iphi = 4 * iring + 1 - (ip - 2 * iring * (iring - 1));
			theta = Math.acos(-1.0 + iring * iring / (3.0 * nsidesq));
			phi = ((double)iphi - 0.5) * PI / (2.0 * iring);

		}
		res[0] = theta;
		res[1] = phi;
		return res;
	}

	/**
	 * returns the vector pointing in the center of the pixel ipix. The vector
	 * is calculated by makePix2Vect_ring method
	 * 
	 * @param nside map resolution
	 * @param ipix pixel number
	 * @return PixToolsVector3d
	 */
	public PixToolsVector3d pix2vect_ring(long nside, long ipix)  {
		return makePix2Vect_ring(nside, ipix).centre;
	}

	/**
	 * returns double [][] with coordinates of the pixel corners. The array is
	 * calculated by makePix2Vect_ring method
	 * 
	 * @param nside map resolution
	 * @param ipix pixel number
	 * @return  double[][] list of vertex coordinates
	 */
	public double[][] pix2vertex_ring(long nside, long ipix)  {
		return makePix2Vect_ring(nside, ipix).borders;
	}

	/**
	 * renders vector (x,y,z) coordinates of the nominal pixel center for pixel
	 * ipix (RING scheme) given the map resolution parameter nside. It also
	 * calculates (x,y,z) positions of the four vertices in order N,W,S,E. These
	 * results are stored in pixVect and pixVertex structures. Those can be
	 * obtained using pix2Vect_ring and pix2vert_ring methods
	 * 
	 * @param nside 
	 *            long map resolution
	 * @param ipix 
	 *            pixel number
	 */
	protected Pixel makePix2Vect_ring(long nside, long ipix)  {
		long nl2;
        long nl4;
        long iring, iphi, ip, ipix1;
        long npix,ncap;
		double phi_nv, phi_wv, phi_sv, phi_ev;
		double z_nv, z_sv, sth_nv, sth_sv, hdelta_phi;
		double fact1, fact2, fodd, hip, fihip, z, sth, phi;
		long iphi_mod;
        long iphi_rat;
//		boolean do_vertex = true;
		long nsidesq = nside * nside;
		String SID = " Pix2Vect_ring:";
		/*                                 */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}

		npix = 12 * nsidesq;
		if (ipix < 0 || ipix > npix - 1) {
			throw new IllegalArgumentException(SID + " ipix out of range calculated from nside");
		}

		ipix1 = ipix + 1; //  in [1, npix]
		nl2 = 2 * nside;
		nl4 = 4 * nside;
		ncap = 2 * nside * (nside - 1); // points in each polar cap
		fact1 = 1.5 * nside;
		fact2 = 3.0 * nsidesq;
		phi_nv = 0.0;
		phi_sv = 0.0;
		if (ipix1 <= ncap) { // north polar cap
			hip = ipix1 / 2.0;
			fihip = (long) hip;
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from north
			                                                       // pole
			iphi = ipix1 - 2 * iring * (iring - 1);
			z = 1.0 - iring * iring / fact2;
			phi = (iphi - 0.5) * PI / (2.0 * iring);

			hdelta_phi = PI / (4.0 * iring); // half pixel width
			z_nv = 1.0 - (iring - 1) * (iring - 1) / fact2;
			z_sv = 1.0 - (iring + 1) * (iring + 1) / fact2;
			iphi_mod = (long) BitManipulation.MODULO(iphi - 1, iring); // in [0,1,...,iring-1]
			iphi_rat = (iphi - 1) / iring; // in [0,1,2,3]
			if (iring > 1)
				phi_nv = HALFPI * (iphi_rat + iphi_mod / (iring - 1.0));
			phi_sv = HALFPI * (iphi_rat + (iphi_mod + 1.0) / (iring + 1.0));
		} else if (ipix1 <= nl2 * (5 * nside + 1)) { // equatorial region
			ip =  (ipix1 - ncap - 1);
			iring = (long) (ip / nl4) + nside; // counted from North pole
			iphi = (long) BitManipulation.MODULO(ip, nl4) + 1;
			fodd = 0.5 * (1. + BitManipulation.MODULO(iring + nside, 2)); // 1 if iring+nside
			                                                 // is odd or 1/2
			z = (nl2 - iring) / fact1;
			phi = (iphi - fodd) * PI / (2.0 * nside);
			hdelta_phi = PI / (4.0 * nside); // half pixel width
			phi_nv = phi;
			phi_sv = phi;
			z_nv = (nl2 - iring + 1) / fact1;
			z_sv = (nl2 - iring - 1) / fact1;
			if (iring == nside) { // nothern transition
				z_nv = 1.0 - (nside - 1) * (nside - 1) / fact2;
				iphi_mod = (long) BitManipulation.MODULO(iphi - 1, nside); // in [0,1,...,nside-1]
				iphi_rat = (iphi - 1) / nside; // in [0,1,2,3]
				if (nside > 1)
					phi_nv = HALFPI * (iphi_rat + iphi_mod / (nside - 1.));
			} else if (iring == 3 * nside) { // southern transition
				z_sv = -1.0 + (nside - 1) * (nside - 1) / fact2;
				iphi_mod = (long) BitManipulation.MODULO(iphi - 1, nside); // in [0,1,... iring-1]
				iphi_rat = (iphi - 1) / nside; // in [0,1,2,3]
				if (nside > 1)
					phi_sv = HALFPI * (iphi_rat + iphi_mod / (nside - 1.0));
			}

		} else { // South polar cap
			ip = npix - ipix1 + 1;
			hip = ip / 2.0;
			fihip = (long) hip;
			iring = (long) (Math.sqrt(hip - Math.sqrt(fihip))) + 1; // counted from South
			                                                       // pole
			iphi = 4 * iring + 1 - (ip - 2 * iring * (iring - 1));
			z = -1.0 + iring * iring / fact2;
			phi = (iphi - 0.5) * PI / (2.0 * iring);
			hdelta_phi = PI / (4.0 * iring); // half pixel width
			z_nv = -1.0 + (iring + 1) * (iring + 1) / fact2;
			z_sv = -1.0 + (iring - 1) * (iring - 1) / fact2;
			iphi_mod = (long) BitManipulation.MODULO(iphi - 1, iring); // in [0,1,...,iring-1]
			iphi_rat = (iphi - 1) / iring; // in [0,1,2,3]
			phi_nv = HALFPI * (iphi_rat + (iphi_mod + 1) / (iring + 1.0));
			if (iring > 1)
				phi_sv = HALFPI * (iphi_rat + iphi_mod / (iring - 1.0));

		}
		/* pixel center */
		sth = Math.sqrt((1.0 - z) * (1.0 + z));
		//pixVect.x = sth * Math.cos(phi);
		//pixVect.y = sth * Math.sin(phi);
		//pixVect.z = z;
		PixToolsVector3d pixVect = new PixToolsVector3d(sth * Math.cos(phi), sth * Math.sin(phi), z);
		/* west vertex */
		phi_wv = phi - hdelta_phi;
		double pixVertex[][] = new double[3][4];
		pixVertex[0][1] = sth * Math.cos(phi_wv);
		pixVertex[1][1] = sth * Math.sin(phi_wv);
		pixVertex[2][1] = z;
		/* east vertex */
		phi_ev = phi + hdelta_phi;
		pixVertex[0][3] = sth * Math.cos(phi_ev);
		pixVertex[1][3] = sth * Math.sin(phi_ev);
		pixVertex[2][3] = z;
		/* north vertex */
		sth_nv = Math.sqrt((1.0 - z_nv) * (1.0 + z_nv));
		pixVertex[0][0] = sth_nv * Math.cos(phi_nv);
		pixVertex[1][0] = sth_nv * Math.sin(phi_nv);
		pixVertex[2][0] = z_nv;
		/* south vertex */
		sth_sv = Math.sqrt((1.0 - z_sv) * (1.0 + z_sv));
		pixVertex[0][2] = sth_sv * Math.cos(phi_sv);
		pixVertex[1][2] = sth_sv * Math.sin(phi_sv);
		pixVertex[2][2] = z_sv;
		return new Pixel(pixVect, pixVertex);
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
	public long ang2pix_ring(long nside, double theta, double phi) {
		long nl4;
        long jp, jm, kshift;
        long ip;
        long ir;
		double z, za, tt, tp, tmp;
		long pix = 0;
		long ipix1;
		long nl2,  ncap, npix;
		String SID = "ang2pix_ring:";
		/*                                       */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}
		if (theta < 0.0 || theta > PI) {
			throw new IllegalArgumentException(SID + " Theta out of range [0,pi]");
		}
		
		z = Math.cos(theta);
		za = Math.abs(z);



		if (phi >= TWOPI)  phi = phi -TWOPI ;

		if (phi < 0.)
			phi =phi + TWOPI; //  phi in [0, 2pi]
		tt = phi / HALFPI; // in [0,4]
//		tt = BitManipulation.MODULO(phi, TWOPI) / HALFPI; // in [0,4]
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
			if ((long) BitManipulation.MODULO(ir, 2) == 0)
				kshift = 1; // 1 if ir even, 0 otherwise
			ip = (long) ((jp + jm - nside + kshift + 1) / 2) + 1; // in [1,4n]
			if (ip > nl4) ip = ip - nl4;
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

	/**
	 * renders the pixel number ipix (RING scheme) for a pixel which contains a
	 * point on a sphere at coordinate vector (x,y,z), given the map resolution
	 * parameter nside
	 * 
	 * @param nside 
	 *            long map resolution
	 * @param vector 
	 *            PixToolsVector3d of the point coordinates
	 * @return  long pixel number
	 * @throws IllegalArgumentException
	 */
	public long vect2pix_ring(long nside, PixToolsVector3d vector)  {
		long res = 0;
		long nl2, nl4, ncap, npix, jp, jm, ipix1;
		double z, za, tt, tp, tmp, dnorm, phi;
		long ir, ip, kshift;
		String SID = " vect2pix_ring:";
		/*                                      */
		if (nside < 1 || nside > ns_max) {
			throw new IllegalArgumentException(SID + " Nside should be power of 2 >0 and < "+ns_max);
		}
		dnorm = vector.length();
		z = vector.z / dnorm;
		phi = 0.;
		if (vector.x != 0. || vector.y != 0.)
			phi = Math.atan2(vector.y, vector.x); // phi in [-pi,pi]
		za = Math.abs(z);
		if (phi < 0.)
			phi += TWOPI; //  phi in [0, 2pi]
		tt = phi / HALFPI; // in [0,4]

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
			if ((long) BitManipulation.MODULO(ir, 2) == 0)
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
		res = ipix1 - 1; // in [0, npix-1]
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
	 * @return LongRangeSet with results           
	 * 
	 * @throws IllegalArgumentException
	 *
	 * Modified by N. Kuropatkin 07/09/2008  Corrected several bugs and make test of all cases.
	 *
	 */			
	public LongRangeSet InRing(long nside, long iz, double phi0, double dphi)  {
		LongRangeSetBuilder b = new LongRangeSetBuilder();
		InRing(nside, iz, phi0, dphi, b);
		return b.build();
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
	 * @param res store result in this builder           
	 * 
	 * @throws IllegalArgumentException
	 *
	 * Modified by N. Kuropatkin 07/09/2008  Corrected several bugs and make test of all cases.
	 *
	 */		
	public void InRing(long nside, long iz, double phi0, double dphi,LongRangeSetBuilder res)  {
		boolean take_all = false;
		boolean to_top = false;
	
		boolean conservative = false;
//		String SID = "InRing:";
		double epsilon = Double.MIN_VALUE; // the constant to eliminate
		// java calculation jitter
		double shift = 0.;
		long ir = 0;
		long kshift, nr, ipix1, ipix2,  ncap, npix;//nir1, nir2,
		long ip_low = 0, ip_hi = 0; //,in, nir;
//		long inext;
		npix = 12 * nside * nside; // total number of pixels
		ncap = 2 * nside * (nside - 1); // number of pixels in the north polar
										// cap
		double phi_low = BitManipulation.MODULO(phi0 - dphi, TWOPI) - epsilon; // phi min,
																  // excluding
																  // 2pi period
		double phi_hi = BitManipulation.MODULO(phi0 + dphi, TWOPI) + epsilon;

//
		if (Math.abs(dphi - PI) < epsilon)  take_all = true;

		/* identifies ring number */
		if ((iz >= nside) && (iz <= 3 * nside)) { // equatorial region
			ir = iz - nside + 1; // in [1, 2*nside + 1]
			ipix1 = ncap + 4 * nside * (ir - 1); // lowest pixel number in the
											     // ring
			ipix2 = ipix1 + 4 * nside - 1; // highest pixel number in the ring
			kshift = (long) BitManipulation.MODULO(ir, 2.);

			nr = nside * 4;
		} else {
			if (iz < nside) { // north pole
				ir = iz;
				ipix1 = 2 * ir * (ir - 1); // lowest pixel number
				ipix2 = ipix1 + 4 * ir - 1; // highest pixel number
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
			ip_low = (long) Math.round((nr * phi_low) / TWOPI - shift);
			ip_hi = (long) Math.round((nr * phi_hi) / TWOPI - shift);

			ip_low = (long) BitManipulation.MODULO(ip_low, nr); // in [0, nr - 1]
			ip_hi = (long) BitManipulation.MODULO(ip_hi, nr); // in [0, nr - 1]
//			System.out.println("ip_low="+ip_low+" ip_hi="+ip_hi);
		} else { // strict: includes only pixels whose center is in
			//                                                    [phi_low,phi_hi]

			ip_low = (long) Math.ceil((nr * phi_low) / TWOPI - shift);
			ip_hi = (long)((nr * phi_hi) / TWOPI - shift);
			if (ip_low == ip_hi + 1)
				ip_low = ip_hi;

			if ((ip_low - ip_hi == 1) && (dphi * nr < PI)) {
				// the interval is too small ( and away from pixel center)
				// so no pixels is included in the list
				
				System.out
						.println("the interval is too small and avay from center");
	
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
		} else {
			if (ip_low < 0 ){
				ip_low = Math.abs(ip_low) ;
				//nir1 = ip_low;
				//nir2 = ip_hi + 1;

                res.appendRange(ipix1, ipix1+ip_hi);
                res.appendRange(ipix2-ip_low +1, ipix2);

				return ;

			}
			ip_low += ipix1;
			ip_hi += ipix1;

            res.appendRange(ip_low,ip_hi);

		}

		return;
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
	public long RingNum(long nside, double z) {
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
	 * calculates vector corresponding to angles theta (co-latitude
	 * measured from North pole, in [0,pi] radians) phi (longitude measured
	 * eastward in [0,2pi] radians) North pole is (x,y,z) = (0, 0, 1)
	 * 
	 * @param theta double
	 * @param phi double
	 * @return PixToolsVector3d
	 * @throws IllegalArgumentException
	 */
	public PixToolsVector3d Ang2Vec(double theta, double phi)  {
		double PI = Math.PI;
		String SID = "Ang2Vec:";
		PixToolsVector3d v;
		if ((theta < 0.0) || (theta > PI)) {
			throw new IllegalArgumentException(SID + " theta out of range [0.,PI]");
		}
		double stheta = Math.sin(theta);
		double x = stheta * Math.cos(phi);
		double y = stheta * Math.sin(phi);
		double z = Math.cos(theta);
		v = new PixToolsVector3d(x, y, z);
		return v;
	}

	/**
	 * returns nside such that npix = 12*nside^2,  nside should be
	 * power of 2 and smaller than ns_max if not return -1
	 * 
	 * @param npix
	 *            long the number of pixels in the map
	 * @return long nside the map resolution parameter
	 */
	public long Npix2Nside(long npix) {
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
	public long Nside2Npix(long nside) {

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
    public double PixRes(long nside) {
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
    public long GetNSide(double pixsize) {
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

}