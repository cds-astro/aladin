package cds.astro;

/*==================================================================
                Class Ecliptic
 *==================================================================*/

import java.util.*;
import java.text.*;		// for parseException

/**
 * The ecliptic frame is defined such that the Sun has a latitude = 0.
 * Since the obliquity of the Earth's axis is slightly changing with
 * time, the relation between the Equatorial and Ecliptic frames
 * vary with time.
 *  
 *  The J2000 value of the obliquity is in Hipparcos vol. 1, table 1.1.2
 *  The fomulae used here refer to Earth Ephemeris VSOP 82/ELP 2000
 *
 * @author Francois Ochsenbein (CDS)
 *
 */

public class Ecliptic extends Astroframe {
    /** The ecliptic frame is tightly connected to the Equatorial frame. */
    protected double equinox ;		// also base_epoch
    protected double fromJ2000[][];	// Rotation matrix FK5 -> ecliptic
    //TODO: add conversion to B1950 ?

   /**
    * Compute the matrix to move from this ecliptic frame to FK5/J2000 (ICRS)
    * @param equinox the equinox of definition (in Julian year)
   **/
   private static double[][] Jmatrix(double equinox) {
      double eps, dt;
      double R[][] = new double[3][3];
      	dt = (equinox-2000.)/100.;	// In Julian centuries
	// Compute mean obliquity in arcec:
	eps = (84381.448 + (-46.8150 + (-0.00059+0.001813*dt)*dt)*dt);
	eps /= 3600.;			// obliquity in degrees
	R[0][0] = 1.;
	R[0][1] = 0.;
	R[0][2] = 0.;
	R[1][0] = 0.;
	R[2][0] = 0.;
	R[1][1] = AstroMath.cosd(eps);	// J2000 = 0.9174820620691818
	R[1][2] = AstroMath.sind(eps);	// J2000 = 0.3977771559319137
	R[2][1] = -R[1][2];		// J2000 =-0.3977771559319137
	R[2][2] =  R[1][1];		// J2000 = 0.9174820620691818
	if (equinox != 2000.) 		// Combine with precession
	    R = AstroMath.m3p(R, FK5.precessionMatrix(2000., equinox));
 	return(R);
   }

   /**
    * Install the matrices for the conversion
   **/
   private final void setJmatrix() {
       fromJ2000 = Jmatrix(this.equinox);
       ICRSmatrix = AstroMath.m3t(fromJ2000);
   }

   // ===========================================================
   // 			Constructor
   // ===========================================================

   /**
    * Instanciate an Ecliptic frame.
    * For non-standard default epoch, use setFrameEpoch() method.
    * @param equinox the equinox of definition, in Julian Year.
   **/
    public Ecliptic(double equinox) {
    	this.precision = 8;	// Intrinsic precision = 0.01arcsec
	this.equinox = equinox;
	this.epoch = equinox;
	fromJ2000 = null;
	name = "Ecl(J" + equinox + ")";
    }

   /**
    * Instanciate an Ecliptic frame (at default J2000 equinox)
   **/
    public Ecliptic() {
	this(2000.);
    }

   /**
    * Defines the rotation matrix to rotate to ICRS
    * @return the 3x3 rotation matrix to convert a position to ICRS frame
   **/
    public double[][] toICRSmatrix() {
	if (ICRSmatrix == null) setJmatrix();
	return(ICRSmatrix);
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

   // Defaults are OK.
    
    /**
     * Get the equinox
     * @return the equinox value
     */
    public double getEquinox() {
   	 return equinox;
    }

}
