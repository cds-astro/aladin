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
 * The origin of the frame is assumed to be the Solar System barycentre.
 *  
 *  The J2000 value of the obliquity is in Hipparcos vol. 1, table 1.1.2
 *  (ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_02.pdf).
 *  It's worth noting that in Hipparcos/Gaia, the frame is assumed to coincide 
 *  with FK5(J2000), but it's not exactly true, since FK5 is not exactly 
 *  aligned with ICRS and spins slowly ({@link FK5}).
 *  The fomulae used here refer to Earth Ephemeris VSOP 82/ELP 2000
 *
 * @author Francois Ochsenbein (CDS)
 * @version 1.0  06-Jun-2006 
 * @version 1.1  27-Oct-2006 Removed useless functions.
 * @version 2.0  04-Feb-2019 1-D matrices (faster).
 *
 */

public class Ecliptic extends Astroframe {

   /** Letter used to identify this frame in IAU names */
    static final char letterIAU = 'E';
    /** The ecliptic frame is tightly connected to the Equatorial frame. */
    protected double equinox ;
    /** Ecliptic coordinates are based on FK5(J2000) 
    public static final double base_epoch = 2000.0;
     */
    /** Rotation 3x3 matrix ecliptic &srarr; FK5(J2000) */
    protected double[] toJ2000;	// Rotation 3x3 matrix ecliptic -> FK5(J2000)

    /** Definition of the rotation part at J2000, which converts into J2000
     */
    static final double[] toJ2000rot = AstroMath.rotation("sx", 84381.448);
    /** Definition of the rotation part at J2000, which converts into J2000
     */
    static final double[] fromJ2000rot = AstroMath.rot_inv(toJ2000rot);

   /**
    * Compute the matrix to move from FK5/J2000 to this ecliptic frame.
    * @param equinox the equinox of definition (in Julian year)
    * @return the matrix <b>M</b> which convert a position in J2000 equinox to the one specified, i.e.
    *         <b>pos</b>(equinox) = <b>M</b> <b>pos</b>(J2000).
   **/
   public static double[] Jmatrix(double equinox) {
      	double dt = (equinox-2000.)/100.;	// In Julian centuries
	// Compute mean obliquity in arcec, 23°26'21.448" in J2000):
	double eps = (84381.448 + (-46.8150 + (-0.00059+0.001813*dt)*dt)*dt);
	eps /= 3600.;			// obliquity in degrees
        /* is just rotation("x", -eps)
        double R[] = new double[9];
	R[0] = 1.;
	R[1] = 0.;
	R[2] = 0.;
	R[3] = 0.;
	R[4] = AstroMath.cosd(eps);	// J2000 = 0.9174820620691818
	R[5] = AstroMath.sind(eps);	// J2000 = 0.3977771559319137
	R[6] = 0.;
	R[7] = -R[5];			// J2000 =-0.3977771559319137
	R[8] =  R[4];			// J2000 = 0.9174820620691818
        ---*/
        double[] R = AstroMath.rotation("x", -eps);
	if (equinox != 2000.) 		// Combine with precession
	    R = AstroMath.m3p(R, FK5.precessionMatrix(2000., equinox));
 	return(R);
   }

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieved a frame saved in "fixedFrames".
   * @param equinox the equinox of the frame, in <b>Julian</b> Year.
   * @param epoch   the epoch in <b>Julian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the frame if previously saved, null if not yet existing.
  **/
    public static Ecliptic scan(double equinox, double epoch) {
        if(DEBUG) System.out.println("#...Ecliptic.scan(" + equinox + ", " + epoch + ")");
        if(fixedFrames==null) return(null);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = i.next();
            if(!(o instanceof Ecliptic)) continue;
            Ecliptic f = (Ecliptic)o;
            if((f.fixed&0xf)!=0)	// non-standard frame
                continue;
            if(f.equinox==equinox && Math.abs(f.epoch-epoch)<3.e-8)
                return f;;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) an Ecliptic frame
   * @param equinox the equinox of the frame, in <b>Julian</b> Year.
   * @param epoch   the epoch in <b>Julian</b> Year.
   * @return The corresponding frame
  **/
    public static Ecliptic create(double equinox, double epoch) {
        Ecliptic f = scan(equinox, epoch);
        if(f==null) {
            f = new Ecliptic(equinox, epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Create (and mark as fixed) the standard Ecliptic frame at given equinox
   *         and default epoch.
   * @param  equinox The equinox, in Julian years.
   * @return The corresponding frame for (equinox, equinox)
  **/
    public static Ecliptic create(double equinox) {
    	return create(equinox, equinox);
    }

  /**
   * Instanciate the default Ecliptic frame
   * @return The Ecliptic frame for (J2000, J2000)
  **/
    public static Ecliptic create() {
        return create(2000.0, 2000.0);
    }

   // ===========================================================
   // 			Constructor
   // ===========================================================

   /**
    * Instanciate an Ecliptic frame at a specified equinox and epoch.
    * @param equinox the equinox of definition, in Julian Year.
    * @param epoch   the epoch   of the coordinates, in Julian Year.
   **/
    public Ecliptic(double equinox, double epoch) {
        if(Double.isNaN(epoch)) epoch=equinox;
    	this.precision = 8;	// Intrinsic precision = 0.01arcsec
	this.equinox = equinox;
	this.epoch = epoch;
	this.name = "Ecl(J" + equinox + ")";
        full_name = name.substring(0, name.length()-1) 
                  + ",Ep=J" + epoch + ")";
        // Compmute matrices to move to ICRS.
	toJ2000 = AstroMath.transposed(Jmatrix(equinox));
        toICRSmatrix = AstroMath.m36p(FK5.toICRSbase, toJ2000);
        fromICRSmatrix = AstroMath.rot_inv(toICRSmatrix);
        compute_dotMatrix(this);
    }

   // ===========================================================
   //                   Alternative Ecliptic frames
   // ===========================================================

  /**
   * The ecliptic frame defined in Gaia-DR2.
   * It is defined the same way as Hipparcos, i.e. considered as fixed
   * with the standard orientation of J2000.0 (23°26'21.448").
   * at (192.85948 +27.12825) and node of galactic centre at 32.93192,
   * see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
   * @return the Gaia-Hipparcos Galactic frame, at Epoch=J2015.5.
  **/
    static public Ecliptic Gaia2() {
        String frame_name = "Ecliptic-Gaia2";
        Ecliptic frame = (Ecliptic)getFrame(frame_name);
        if(frame==null) {
            frame = new Ecliptic(2000.0, 2015.5);
            frame.toICRSmatrix   = toJ2000rot;
            frame.fromICRSmatrix = AstroMath.rot_inv(toJ2000rot);
            frame.fixed |= HIPdef;	// Indicates non-standard frame
    	    frame.precision = 9;	// Intrinsic precision = 0.1mas 
            frame.name = frame.full_name = frame_name;
            frame.fixFrame(frame_name);
        }
        return frame;
    }

  /**
   * The galactic frame defined in Hipparcos.
   * It is defined the same way as Hipparcos, i.e. the Galactic North pole is
   * at (192.85948 +27.12825) and node of galactic centre at 32.93192,
   * see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
   * @return the Gaia-Hipparcos Galactic frame, at Epoch=J2015.5.
  **/
    static public Ecliptic Hipparcos() {
        String frame_name = "Ecliptic-Hip";
        Ecliptic frame = (Ecliptic)getFrame(frame_name);
        if(frame==null) {
            frame = new Ecliptic(2000.0, 1991.25);
            frame.toICRSmatrix   = toJ2000rot;
            frame.fromICRSmatrix = AstroMath.rot_inv(toJ2000rot);
            frame.fixed |= HIPdef;	// Indicates non-standard frame
    	    frame.precision = 9;	// Intrinsic precision = 0.1mas 
            frame.name = frame.full_name = frame_name;
            frame.fixFrame(frame_name);
        }
        return frame;
    }

    // ===========================================================
    //                  Get elements.
    // ===========================================================

    /**
     * Get the equinox
     * @return the equinox value (Julian years)
     */
    public double getEquinox() {
   	 return equinox;
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

   /** (default method is ok)
    * Convert the position to its ICRS equivalent, when proper motion unknown.
    * @param coo on input the position in this frame; on ouput the ICRS
    public void toICRS(Coo coo) {
        coo.rotate(toJ2000);
        coo.rotate(FK5.XM5I);
    }
   **/

   /** (default method is ok)
    * Convert the position from the ICRS frame.
    * @param coo on input the ICRS position, on output in FK5.
    public void fromICRS(Coo coo) {
        coo.rotate_1(FK5.XM5I); // now on FK5(J2000)
	coo.rotate_1(toJ2000);  // now at current equinox.
    }
   **/


  /** (default method is ok, since base epochs are identical)
   * Convert the position/velocity from Ecliptic(equinox,epoch) to ICRS,Ep=J2000.
   * @param u6 6-vector containing position + movement (rad/Jyr).
   *        On input, u6 is the 6-vector in FK5(J2000),Ep=J2000; 
   *        on output, u6 is the 6-vector in ICRS,Ep=J2000.
    public void toICRS(double[] u6) {
        // Simple multiplication 6x6 matrix -> 6-vector
	double[] v = AstroMath.m36v(PM5I, u6);
        System.arraycopy(u6, 0, v, 0, 6);
    }
  **/

  /** (default method is ok, since base epochs are identical)
   * Convert the position/velocity from FK5(J2000),Ep=J2000 to ICRS,Ep=J2000.
   * @param u6 6-vector containing position + movement (rad/Jyr).
   *        On input, u6 is the 6-vector in ICRS,Ep=J2000;
   *        on output, u6 is the 6-vector in FK5(J2000),Ep=J2000.
    public void fromICRS(double[] u6) {
        // Simple multiplication 6x6 matrix -> 6-vector
	double[] v = AstroMath.m36v(PMI5, u6);
        System.arraycopy(u6, 0, v, 0, 6);
    }
  **/

}
