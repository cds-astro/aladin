//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//

package org.asterope.healpix;

/** various utilities not directly related to Healpix.*/
public class PixToolsUtils {
	
	/**
	 * calculates the surface of spherical triangle defined by
	 * vertices v1,v2,v3 Algorithm: finds triangle sides and uses l'Huilier
	 * formula to compute "spherical excess" = surface area of triangle on a
	 * sphere of radius one see, eg Bronshtein, Semendyayev Eq 2.86 half
	 * perimeter hp = 0.5*(side1+side2+side3) l'Huilier formula x0 = tan( hp/2.)
	 * x1 = tan((hp - side1)/2.) x2 = tan((hp - side2)/2.) x3 = tan((hp -
	 * side3)/2.)
	 * 
	 * @param v1 PixToolsVector3d
	 * @param v2 PixToolsVector3d
	 * @param v3 PixToolsVector3d vertices of the triangle
	 * @return  double the triangle surface
	 * @throws Exception
	 *  
	 */
	public static double SurfaceTriangle(PixToolsVector3d v1, PixToolsVector3d v2, PixToolsVector3d v3)
			throws Exception {
		double res = 0.;
		double side1 = v2.angle( v3) / 4.0;
		double side2 = v3.angle(v1) / 4.0;
		double side3 = v1.angle(v2) / 4.0;
		double x0 = Math.tan(side1 + side2 + side3);
		double x1 = Math.tan(side2 + side3 - side1);
		double x2 = Math.tan(side1 + side3 - side2);
		double x3 = Math.tan(side1 + side2 - side3);
		res = 4.0 * Math.atan(Math.sqrt(x0 * x1 * x2 * x3));

		return res;
	}
	
    /**
     * returns polar coordinates in radians given ra, dec in degrees
     * @param radec double array containing ra,dec in degrees
     * @return res double array containing theta and phi in radians
     *             res[0] = theta res[1] = phi
     */
    public static double[] RaDecToPolar(double[] radec) {
    	double[] res = {0.0,0.0};
    	
			double ra =  radec[0];
			double dec =  radec[1];
			double theta = Math.PI/2. - Math.toRadians(dec);
			double phi = Math.toRadians(ra);
			res[0] = theta;
			res[1] = phi;
    	
    	return res;
    }
    /**
     * returns ra, dec in degrees given polar coordinates in radians
     * @param polar double array polar[0] = phi in radians
     *                           polar[1] = theta in radians
     * @return double array radec radec[0] = ra in degrees
     *                radec[1] = dec in degrees
     */
    public static double[] PolarToRaDec(double[] polar) {
    	double[] radec = {0.0,0.0};
			double phi =  polar[1];
			double theta = polar[0];
			double dec = Math.toDegrees(Math.PI/2. - theta);
			double ra = Math.toDegrees(phi);
			radec[0] = ra;
			radec[1] = dec;
    	
    	return radec;
    }
    
    /**
     * returns polar coordinates of a point on unit sphere given Cartesian coordinates
     * @param x - Cartesian coordinate x of a point on unit sphere
     * @param y - y coordinate
     * @param z - z coordinate
     * @return double [] theta,phi
     */
    public static double[] xyzToPolar(double x, double y, double z) {
    	double[] res;
    	PixToolsVector3d vv = new PixToolsVector3d(x,y,z);
    	res = Vect2Ang(vv);
    	return res;
    }
    
	/**
	 * converts a PixToolsVector3d in a tuple of angles tup[0] = theta 
	 * co-latitude measured from North pole, in [0,PI] radians, tup[1] = phi 
	 * longitude measured eastward, in [0,2PI] radians
	 * 
	 * @param v
	 *            PixToolsVector3d
	 * @return double[] out_tup out_tup[0] = theta out_tup[1] = phi
	 */
	public static double[] Vect2Ang(PixToolsVector3d v) {
		double[] out_tup = new double[2];
		double norm = v.length();
		double z = v.z / norm;
		double theta = Math.acos(z);
		double phi = 0.;
		if ((v.x != 0.) || (v.y != 0)) {
			phi = Math.atan2(v.y, v.x); // phi in [-pi,pi]
		}
		if (phi < 0)
			phi += 2.0 * Math.PI; // phi in [0, 2pi]
//		phi += Math.PI;
		out_tup[0] = theta;
		out_tup[1] = phi;
		return out_tup;
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
	public static double[] intrs_intrv(double[] d1, double[] d2) {
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



}
