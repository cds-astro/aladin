//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//


package org.asterope.healpix;


import java.util.ArrayList;




/**
 * @author kuropat
 * The class represents square region on a sphere defined by raMin, raMax
 * decMin,decMax in degrees. The dec varies from Pi/2 to -Pi/2.
 * The ra varies from 0. inclusive to 2Pi exclusive. Negative value of ra 
 * will be corrected by adding 2Pi
 * 
 */
public class Region {
	private static final double TWOPI = 2.0*Math.PI;
	private double raMin;
	private double raMax;
	private double decMin;
	private double decMax;
	private double tetMin;
	private double tetMax;
	private double phiMin;
	private double phiMax;
	private static final double epsilon = 1.0e-10;
//	private boolean normalized = false;
	private ArrayList<PixToolsVector3d> vertices;
	private double PI = Math.PI;
	/**
	 * default constructor
	 * @param xMin ra min in degrees
	 * @param xMax ra max in degrees
	 * @param yMin dec min in degrees
	 * @param yMax dec max in degrees
	 */
	public Region(double xMin, double xMax, double yMin, double yMax) {
		super();
		this.raMin = xMin;
		this.raMax = xMax;
	    this.phiMin = Math.toRadians(raMin);
	    this.phiMax = Math.toRadians(raMax); 
	    if (phiMin < 0.) phiMin += TWOPI;
	    if (phiMax < 0. ) phiMax += TWOPI;
	    if (this.phiMax < this.phiMin) this.phiMax += TWOPI;
		this.phiMin = BitManipulation.MODULO(this.phiMin, TWOPI) - epsilon; // phi min, excluding 2pi period
		this.phiMax = BitManipulation.MODULO(this.phiMax, TWOPI) + epsilon;
	    this.raMin = Math.toDegrees(this.phiMin);
	    this.raMax = Math.toDegrees(this.phiMax);
		this.decMin = yMin;
		this.decMax = yMax;


		this.tetMax = PI/2. - Math.toRadians(decMin);
		this.tetMin = PI/2. - Math.toRadians(decMax);

// create list of vertex vectors
		vertices = new ArrayList<PixToolsVector3d>();
		PixTools pt = new PixTools();
		PixToolsVector3d vv = pt.Ang2Vec(tetMin,phiMin);
		vertices.add(vv);
		vv = pt.Ang2Vec(tetMin,phiMax);
		vertices.add(vv);
		vv = pt.Ang2Vec(tetMax,phiMin);
		vertices.add(vv);
		vv = pt.Ang2Vec(tetMax,phiMax);
		vertices.add(vv);		
	    
	}
	/**
	 * return true if the point ra,dec is inside region
	 * @param ra in degrees
	 * @param dec in degrees
	 * @return boolean true if inside the region
	 */
	public boolean inReg(double ra, double dec) {
		boolean res = false;
//		double racomp = ra;
		double phiComp = Math.toRadians(ra);
		if (phiComp < 0.) phiComp += TWOPI;
		phiComp = BitManipulation.MODULO(phiComp, TWOPI) - epsilon;
		
//		racomp = Math.toDegrees(phiComp);


		if ((phiComp >= phiMin - epsilon) && (phiComp <= phiMax + epsilon) && (dec>= decMin - epsilon) && (dec <= decMax + epsilon)) res = true; 
		return res;		
	}
	/**
	 * return true if point phi, theta is inside region
	 * @param phi in radians
	 * @param theta in radians
	 * @return boolean true if in region
	 */
	public boolean inRegPol(double phi, double theta) {
		boolean res = false;
		double  phicomp = phi;
		if (phicomp < 0.) phicomp += TWOPI;
		phicomp = BitManipulation.MODULO(this.phiMin, TWOPI) - epsilon;
		if ((phicomp >= phiMin - epsilon) && (phicomp <= phiMax + epsilon) && (theta>= tetMin - epsilon) && (theta <= tetMax + epsilon)) res = true;
		return res;
	}
	/**
	 * @return ArrayList of 3d vectors of vertices of the region
	 */
	public ArrayList<PixToolsVector3d> getVertices() {
		return vertices;
	}
	
	/**
	 * divides region on HealPix pixels of given size including
	 * pixels that only partly inside the region
	 * @param precision - angular size of the division element in arcsec
	 * @return ArrayList of pixel numbers in ring schema for specifyed resolution
	 * @throws Exception 
	 */
	public LongRangeSet pixelize(double precision){
		LongRangeSetBuilder res = new LongRangeSetBuilder();
		PixTools pt = new PixTools();
		long nside = pt.GetNSide(precision);
		long rnmin = pt.RingNum(nside,Math.cos(tetMin));
		long rnmax = pt.RingNum(nside,Math.cos(tetMax));
		for (long ir = rnmin; ir < rnmax; ir++) {
			
			double phi = (phiMin + phiMax)/2.;
			double dphi = (phiMax - phiMin)/2.;
			pt.InRing(nside, ir, phi, dphi,res);			
		}

		return res.build();
	}
	/**
	 * provides polar coordinates of the region vertices
	 * @return  array of corner coordinates in form of polar thete,phi angles.
	 */
	public double[][] getPolReg() {
		double[][] res = new double[4][2];
		res[0][1] = phiMin;
		res[0][0] = tetMin;
		res[1][1] = phiMax;
		res[1][0] = tetMin;
		res[2][1] = phiMin;
		res[2][0] = tetMax;
		res[3][1] = phiMax;
		res[3][0] = tetMax;
		return res;
	}
}
