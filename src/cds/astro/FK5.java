// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.astro;

/*==================================================================
                FK5  (Astroframe -> Equatorial -> FK5)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The FK5 is an equatorial coordinate system (coordinate system linked to 
 * the Earth) based on its J2000 position.
 * As any equatorial frame, the FK5-based follows the long-term
 * Earth motion (precession).
 *  
 * @author Francois Ochsenbein (CDS)
 * @version 1.0  15-Nov-2007 Assumed to be identical to ICRS
 * @version 2.0  03-Feb-2019 Conversion to ICRS takes into account the FK5 rotation.
 *               (Mignard &amp; Froeschlé 2000A+A...354..732M).
 *               Also use 1-D matrices (20% faster).
 */

public class FK5 extends Equatorial {

  /** Letter used to identify this frame in IAU names */
   static final char letterIAU = 'J';

    /**
     * FK5 is based on epoch J2000.0
    public static final double base_epoch = 2000.0;
     */

   /** 
    * Rotation angles which align FK5(J2000) to ICRS(J2000), in mas.
    * The rotation matrix which converts positions in ICRS to FK5 ones is
    * ICRStoFK5 = AstroMath.rotation("mxyz", -19.9, -9.1, 22.9)
    */
    static public final double[] XYZrota = {  -19.9, -9.1, 22.9};

   /** 
    * Spin of FK5 relative to ICRS, in mas/Jyr (milli-arcsec per Julian year)
    */
    static public final double[] XYZspin = { -0.30, 0.60, 0.70 };

   /** (Matrix#1 of the 2x3x3 matrix {@see #toICRSbase})
    * Rotation matrix which converts FK5(J2000),Ep=J2000 
    * into ICRS,Ep=J2000 (see "frames.pdf" document):
    * ICRStoFK5 = AstroMath.rotation("mxyz", -19.9, -9.1, 22.9)
    * FK5toICRS = AstroMath.rotation("mzyx", -22.9,  9.1, 19.9)
   static public final double[] XM5I = {
     // AstroMath.rotation("mzyx", -22.9,  9.1, 19.9, )
      0.9999999999999928638,   0.0000001110223372305,   0.0000000441180342698,
     -0.0000001110223329741,   0.9999999999999891830,  -0.0000000964779274389,
     -0.0000000441180449810,   0.0000000964779225408,   0.9999999999999943728};
    */

   /** (Matrix#2 of the 2x3x3 matrix {@see #toICRSbase})
    * Spin Matrix from spin5 angular velocities, derivative of XM5I at J2000. 
    * At first order, this matrix is
    * |  0   ωz -ωy |
    * | -ωz  0   ωx |
    * |  ωy -ωx  0  |
   static public final double[] Xd5I = {
     -0.0000002484418308e-9,   3.3936955512908941e-9,  -2.9088825755493596e-9,
     -3.3936957677667417e-9,  -0.0000005170974623e-9,  -1.4544408701009028e-9,
      2.9088820866572131e-9,   1.4544410433286121e-9,  -0.0000000119872596e-9};
    */

   /** 
    * Rotation + Spin matrix (2x3x3) for conversion from FK5(J2000) to ICRS.
    * The first 9 doubles represent the rotation, the last 9 the spin.
    */
   static public final double[] toICRSbase = {
     //= AstroMath.rotation("mzyx", -22.9,  9.1, 19.9, -0.70, -0.60, -0.30)
      0.9999999999999928638,   0.0000001110223372305,   0.0000000441180342698,
     -0.0000001110223329741,   0.9999999999999891830,  -0.0000000964779274389,
     -0.0000000441180449810,   0.0000000964779225408,   0.9999999999999943728,
     // Spin part
     -0.0000002484418308e-9,   3.3936955512908941e-9,  -2.9088825755493596e-9,
     -3.3936957677667417e-9,  -0.0000005170974623e-9,  -1.4544408701009028e-9,
      2.9088820866572131e-9,   1.4544410433286121e-9,  -0.0000000119872596e-9};

   /** 
    * Rotation + Spin matrix (2x3x3) for conversion from ICRS to FK5(J2000).
    * The first 9 doubles represent the rotation, the last 9 the spin.
    */
   static public final double[] fromICRSbase = {
      //= AstroMath.rotspin("mxyz",  -19.9, -9.1, 22.9,   -0.30, 0.60, 0.70);
      0.9999999999999928638,  -0.0000001110223329741,  -0.0000000441180449810,
      0.0000001110223372305,   0.9999999999999891831,   0.0000000964779225408,
      0.0000000441180342698,  -0.0000000964779274389,   0.9999999999999943728,
      // Spin part
     -0.0000002484418308e-9,  -3.3936957677667417e-9,   2.9088820866572131e-9, 
      3.3936955512908941e-9,  -0.0000005170974623e-9,   1.4544410433286121e-9, 
     -2.9088825755493596e-9,  -1.4544408701009028e-9,  -0.0000000119872596e-9 };

   /** Combined 6x6 matrix to convert the 6-vector (cartesian position and 
    * derivative expressed in <em>rad/Jyr</em> from FK5(J2000),Ep=J2000 into
    * ICRS,Ep=J2000. Its expression is <br><tt>
    * | XM5I   0   |<br>
    * | Xd5I  XM5I |</tt><br>
   static public final double[] PM5I = {
     // AstroMath.rotation("mzyx", -22.9,  9.1, 19.9, -0.70, -0.60, -0.30)
       0.9999999999999928638,   0.0000001110223372305,   0.0000000441180342698, 0.0, 0.0, 0.0,
      -0.0000001110223329741,   0.9999999999999891830,  -0.0000000964779274389, 0.0, 0.0, 0.0,
      -0.0000000441180449810,   0.0000000964779225408,   0.9999999999999943728, 0.0, 0.0, 0.0,
      -0.0000002484418308e-9,   3.3936955512908941e-9,  -2.9088825755493596e-9,  // Xd5I[0]
       0.9999999999999928638,   0.0000001110223372305,   0.0000000441180342698,  //    XM5I[0]
      -3.3936957677667417e-9,  -0.0000005170974623e-9,  -1.4544408701009028e-9,  // Xd5I[1]
      -0.0000001110223329741,   0.9999999999999891830,  -0.0000000964779274389,  //    XM5I[1]
       2.9088820866572131e-9,   1.4544410433286121e-9,  -0.0000000119872596e-9,  // Xd5I[2]
      -0.0000000441180449810,   0.0000000964779225408,   0.9999999999999943728}; //    XM5I[2]
    **/

   /** 
    * Inverse matrix of PM5I, which converts ICRS,Ep=J2000 into FK5(J2000),Ep=J2000.
    * Its expression is, if <tt>X'</tt> is (transposed)<tt>XM5I</tt>:<br><tt>
    * |     X'     0  |<br>
    * | -X'.Xd.X'  X' |</tt><br>
    * = AstroMath.rotspin("mxyz",  -19.9, -9.1, 22.9,   -0.30, 0.60, 0.70);
   static public final double[] PMI5 = {
       0.9999999999999928638,  -0.0000001110223329741,  -0.0000000441180449810, 0.0, 0.0, 0.0,
       0.0000001110223372305,   0.9999999999999891831,   0.0000000964779225408, 0.0, 0.0, 0.0,
       0.0000000441180342698,  -0.0000000964779274389,   0.9999999999999943728, 0.0, 0.0, 0.0,
      -0.0000002484418308e-9,  -3.3936957677667417e-9,   2.9088820866572131e-9, 
       0.9999999999999928639,  -0.0000001110223329741,  -0.0000000441180449810,  //    XMI5[0]
       3.3936955512908941e-9,  -0.0000005170974623e-9,   1.4544410433286121e-9, 
       0.0000001110223372305,   0.9999999999999891831,   0.0000000964779225408,  //    XMI5[1]
      -2.9088825755493596e-9,  -1.4544408701009028e-9,  -0.0000000119872596e-9,
       0.0000000441180342698,  -0.0000000964779274389,   0.9999999999999943729}; //    XMI5[2]
    **/

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieve a FK5 frame saved in "fixedFrames".
   * @param equinox the equinox of definition, in Julian Year.
   * @param epoch   the epoch in <b>Julian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the frame if previsouly saved, null if not yet existing.
  **/
    public static FK5 scan(double equinox, double epoch) {
        if(DEBUG) System.out.println("#...FK5.scan(J" + equinox + ", Ep=J" + epoch + ")");
        if(fixedFrames==null) return(null);
        boolean anyEpoch = Double.isNaN(epoch);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = i.next();
            if(!(o instanceof FK5)) continue;
            FK5 f = (FK5)o;
            if((f.fixed&0xf)!=0) continue;	// non-standard frame
            if(f.equinox!=equinox) continue;
            if(anyEpoch||(Math.abs(f.epoch-epoch)<Jsec))
                return f;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) a FK5 frame
   * @param equinox the equinox of the frame, in <b>Julian Year</b>
   * @param epoch  the default eoich of the frame, in <b>Julian Year</b>
   * @return The frame
  **/
    public static final FK5 create(double equinox, double epoch) {
        FK5 f = scan(equinox, epoch);
        if(f==null) {
            f = new FK5(equinox, epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Create (and mark as fixed) a FK5 frame with standard epoch.
   * @param equinox the equinox of the frame, in <b>Julian Year</b>.
   * @return The corresponding frame
  **/
    public static FK5 create(double equinox) {
	return create(equinox, equinox);
    }

  /**
   * Instanciate the default FK5 frame
   * @return The standard FK5(J2000) coordinate frame.
  **/
    public static FK5 create() {
	return create(2000.0, 2000.0);
    }

   // ===========================================================
   // 			Constructor
   // ===========================================================

  /**
   * Instanciate an FK5 frame
   * @param equinox the equinox of definition, in Julian Year.
   * @param epoch   the epoch of definition, in Julian Year.
  **/
    public FK5(double equinox, double epoch) {
        //System.out.println("#...FK5(" + equinox + ", " + epoch + ")...");
        if(Double.isNaN(epoch)) epoch = equinox;
   	this.precision = 7;	// Intrinsic precision = 0.01arcsec
	this.equinox = equinox;
	this.epoch = epoch;
	this.name = "FK5(J" + equinox + ")";
        full_name = name.substring(0, name.length()-1) 
                  + ",Ep=J" + epoch + ")";
        // ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        // ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
	if(Math.abs(equinox-2000.)>0.0003) {
            toBaseEquinox = this.precessionMatrix(equinox, 2000.);
            toICRSmatrix   = AstroMath.m36p(toICRSbase, toBaseEquinox);
            fromICRSmatrix = AstroMath.rot_inv(toICRSmatrix);
        }
	else {
            toBaseEquinox = AstroMath.U3matrix;
            toICRSmatrix   = toICRSbase;
            fromICRSmatrix = fromICRSbase;
        }
        compute_dotMatrix(this);
        if(DEBUG) {
            AstroMath.checkUnity("#---Verify to/fromICRSbase:", AstroMath.m36p(toICRSbase, fromICRSbase));
            System.out.println("#---Constructing Astroframe: " + full_name);
            AstroMath.printMatrix("#...fromICRSmatrix:\n", fromICRSmatrix);
            AstroMath.printMatrix("#...(recomputed)..:\n",   AstroMath.m36p(AstroMath.rot_inv(toBaseEquinox), AstroMath.rot_inv(toICRSbase)));
            AstroMath.printMatrix("#.....toICRSmatrix:\n",   toICRSmatrix);
            AstroMath.printMatrix("#..6x6toICRSmatrix:\n",   AstroMath.m36p(AstroMath.m6(toICRSbase), AstroMath.m6(toBaseEquinox)));
            AstroMath.printMatrix("#.....Product=.....\n",
                AstroMath.m36p(fromICRSmatrix, toICRSmatrix));
            AstroMath.printMatrix("#....toBaseEquinox:\n",  toBaseEquinox);
        }
    }

   // ===========================================================
   // 			Precession in FK5 system
   // ===========================================================

   /**
    * Precession matrix from equinox t0 to t1 (Julian Years)
    * @param eq0 equinox at original equinox (julian year)
    * @param eq1 equinox of destination      (julian year)
    * @return the rotation matrix R such that   u1 = R * u0
   **/
    static final double[] precessionMatrix(double eq0, double eq1) {
      double t0, dt, w, z, theta, zeta;
      boolean reverse = false;
      // Choose t0 as the closest to 2000.
        t0 = (eq0 - 2000.)/100.;	// Origin J2000.0
	dt = (eq1 - eq0)/100.;		// Centuries
	if (Math.abs(t0) > Math.abs(t0+dt)) {	// t0+dt = t1
	    reverse = true;
	    t0 += dt;
	    dt = -dt;
	}
	w = 2306.2181+(1.39656-0.000139*t0)*t0;	// arcsec
	zeta = (w + ( (0.30188-0.000344*t0) + 0.017998*dt) *dt)
		*dt/3600.;		// to degrees
	z    = (w + ( (1.09468+0.000066*t0) + 0.018203*dt) *dt)
		*dt/3600.;		// to degrees
	theta =  ( (2004.3109 + (-0.85330-0.000217*t0)*t0)
		 +( (-0.42665-0.000217*t0) - 0.041833*dt) *dt) 
		*dt/3600.;
	if (reverse) //return(Coo.eulerMatrix(-zeta, -theta, -z));
              return(AstroMath.rotation("zyz", -zeta, theta, -z));
	else //return(Coo.eulerMatrix(z, theta, zeta));
	      return(AstroMath.rotation("zyz", z, -theta, zeta));
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

   /** (standard conversion is ok)
    * Convert the position to its ICRS equivalent, when proper motion unknown.
    * @param coo on input the position in this frame; on ouput the ICRS
    public void toICRS(Coo coo) {
        if(toBaseEquinox!=AstroMath.U3matrix) coo.rotate(toBaseEquinox);
        coo.rotate(XM5I);
    }
   **/

   /** (standard conversion is ok)
    * Convert the position from the ICRS frame.
    * @param coo on input the ICRS position, on output in FK5.
    public void fromICRS(Coo coo) {
        coo.rotate_1(XM5I);
        if(toBaseEquinox!=AstroMath.U3matrix) coo.rotate_1(toBaseEquinox);
    }
   **/

  /** (standard conversion is ok)
   * Convert the position/velocity from FK5(J2000),Ep=J2000 to ICRS,Ep=J2000.
   * @param u6 6-vector containing position + movement (rad/Jyr).
   *        On input, u6 is the 6-vector in FK5(J2000),Ep=J2000; 
   *        on output, u6 is the 6-vector in ICRS,Ep=J2000.
    public void toICRS(double[] u6) {
        // Simple multiplication 6x6 matrix -> 6-vector
	double[] v = AstroMath.m36v(PM5I, u6);
        System.arraycopy(u6, 0, v, 0, 6);
    }
  **/

  /** (standard conversion is ok)
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
