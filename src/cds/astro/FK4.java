package cds.astro;

/*==================================================================
                FK4  (Astroframe -> Equatorial -> FK4)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The FK4 is an equatorial coordinate system (coordinate system linked to 
 * the Earth) based on its B1950 position. 
 * 2 particularities are important:<ol>
 * <li> the elliptical term of the aberration (abbreviated e-term) is
 *      not removed in the (RA,Dec) expressed in this system; 
 * <li> the unit of time is the Besselian year, slighly differing from
 *      the Julian year (365.25days).
 * </ol>
 * It is assumed that these 2 peculiarities have been 
 * <em>removed in the cartesian components</em> of the coordinates. 
 * Removing the e-term is achieved by {@link #subEterm}, and
 * computing the (RA,Dec) in a FK4 position is achieved by {@link #addEterm}.
 *
 * @author Francois Ochsenbein (CDS)
 * @version 1.0  03-Aug-2004
 * @version 2.0  03-Feb-2019 Conversion to ICRS takes into account the FK5 rotation.
 *               (Mignard &amp; Froeschlé 2000A+A...354..732M).
 *               Also use 1-D matrices (20% faster).
 * References: <UL>
 * <LI> Murray C.A. (1989A+A...218..325M) matricial relation FK4/FK5
 * <LI> Mignard &amp; Froeschlé 2000A+A...354..732M, FK5 rotation vs ICRS
 * </UL>
 *
 */

public class FK4 extends Equatorial {

  /** Letter used to identify this frame in IAU names */
   static final char letterIAU = 'B';
  /**
   * In addition to the equinox, we store the precession matrix
   * which converts to the standard epoch, and the E-term of the
   * aberration which is not removed in the FK4 coordinates.
   * In the cartesian representation of the coordinates, this 
   * term is removed, i.e. the transformation of polar to cartesian
   * is more complex than in the other frames.
  **/

   /** Value of the e-term aberration for the base equinox (B1950): 
    * { -1.6256018e-6, -0.3191954e-6, -0.1384293e-6 }
    */
    static private double[] ev50 = eterm(1950.);	// e-term for B1950
   /** 3x3 matrix Precession matrix which converts a Coo from this equinox
    * to the standard equinox B1950. 
    */
    protected double[] toBaseEquinox;		// 3x3 matrix Precession to B1950

   /** 1x3 vector of the e-term of the aberration for this equinox. */
    protected double[] ev_eq;			// e-term for Equinox

   /**
    * In version 2.0, the conversion of FK4(B1950) is directly made into ICRS, 
    * using Murray's (1989A+A...218..325M) method combined to the correction
    * of the rotation of FK5 vs Hipparcos (ICRS) frame (Mignard &amp; Froeschlé,
    * 2000A+A...354..732M); see the "frames.pdf" document for details.
    * The rotation matrix which converts FK4 to ICRS is XM4I, 
    * its derivative at B1950 epoch is Xd4I.
    * The 6x6 matrix to move the (u,ud) 6-vector from FK4 to ICRS is PM4I
    * Proper motions are in arcsec/century (10mas/yr) 
   **/

   /** 
    * Ratio of Julian year (or century) to Besselian year (or tropical century)
    */
    static public final double FJB = 1.000021359027778; 

   /** 
    * time interval between B1950.0 and J2000.0, in Julian years
    * (ICRS.base_epoch - FK4.base_epoch)
    */
    static public final double TJ = 50.0002095577002;  

   /** Base epoch for FK4 definitions, in <b>Julian years</b>
    public static final double base_epoch = 2000.0 - TJ;  // epoch for FK4<=>ICRS
    */

   /** Definition of FK4-FK5 transformation from Murray 1989A+A...218..325M,
    *  Eqs(28) and (29), but using Jumian years instead of Julian centuries.
    */
    public static final double[] X0 = {   
        // Eq.(8)
        0.9999256794956877, -0.0111814832204662, -0.0048590038153592,
        0.0111814832391717,  0.9999374848933135, -0.0000271625947142,
        0.0048590037723143, -0.0000271702937440,  0.9999881946023742,
        // Eq.(29)
       -0.0026455262e-8,    -1.1539918689e-8,     2.1111346190e-8,
        1.1540628161e-8,    -0.0129042997e-8,     0.0236021478e-8,
       -2.1112979048e-8,    -0.0056024448e-8,     0.0102587734e-8 };

   /** 
    * Rotation angles which align FK4(B1950) to ICRS, in arcsec.
    */
    static public final double[] XYZrota = { 5.584340790153,  1002.236538581694, -2306.398766625517 };

   /** 
    * Spin of FK4 relative to ICRS, in mas/Jyr (milli-arcsec per Julian year)
    */
    static public final double[] XYZspin = { -0.2817204053, -3.7515987371, -1.6789411256}; 

   /** 
    * Combined Rotation + Spin matrix (2x3x3 matrix). 
    * Derivative (vs Julian year) of XM4I, which represents the spin of the FK4
    *  with respect to the ICRS. It is computed as
    *  <code>{@link AstroMath#m36p}({@link FK5#toICRSbase}, {@link #X0})</code>
    */
   static public final double[] toICRSbase = {
      // AstroMath.m36p(FK5.toICRSbase, X0);
      0.9999256809514446605,  -0.0111813722062681620,  -0.0048589597008613673,
      0.0111813717563032290,   0.9999374861373183740,  -0.0000272585320447865,
      0.0048589607363144413,  -0.0000270733285476070,   0.9999881948141177111, 
      // spin
     -0.0026428817769940e-9,  -8.1463562741672220e-9,  18.2024058051186966e-9,
      8.1401195144477102e-9,  -0.0910561571313818e-9,  -1.2019145949013101e-9,
    -18.1880492284604517e-9,   1.3658005511963221e-9,   0.0884130375706395e-9 };

   /** 
    * Inverse of toICRSmatrix, which converts ICRS into FK4(B1950).
    * It is identical to:<br>
    * <code>{@link AstroMath#rot_inv}({@link #toICRSbase})</code>.
    **/
   static public final double[] fromICRSbase = {
    // AstroMath.rot_inv(toICRSbase)
       0.9999256809514446605,   0.0111813717563032290,   0.0048589607363144413,
      -0.0111813722062681620,   0.9999374861373183740,  -0.0000270733285476070,
      -0.0048589597008613673,  -0.0000272585320447865,   0.9999881948141177111,
      // spin
      -0.0026428817769940e-9,   8.1401195144477114e-9, -18.1880492284604536e-9,
      -8.1463562741672231e-9,  -0.0910561571313818e-9,   1.3658005511963220e-9,
      18.2024058051186977e-9,  -1.2019145949013101e-9,   0.0884130375706395e-9 };

   /** 
    * Combined 6x6 matrix to convert the 6-vector (cartesian position and 
    * derivative expressed in <em>rad/Jyr</em> from FK4(B1950),Ep=B1950 into
    * ICRS,Ep=J2000. Its expression is <br><tt>
    * | XM4I+T*Xd4I  T*XM4I |<br>
    * |     Xd4I       XM4I |</tt><br>
    * where T is the time interval between B1950.0 and J2000.0 ({@link #TJ})
    * It is identical to: <br>
    * <code>{@link AstroMath#motionMatrix)({@link #TJ}, {@link #toICRSbase})</code>
   static public final double[] PM4I = {
    // AstroMath.motionMatrix(TJ, X0)
       0.9999256808193000178,  -0.0111817795257890021,  -0.0048580495767566571, // XM4I+T*Xd4I[0]
      49.9964935896983064950,  -0.5590709534560527534,  -0.2429490032754886528, //      T*XM4I[0]
       0.0111817787639847761,   0.9999374815844914359,  -0.0000273186280264020, // XM4I+T*Xd4I[1]
       0.5590709309577118072,  49.9970838514658597711,  -0.0013629323144746095, //      T*XM4I[1]
       0.0048580513300415725,  -0.0000270050382338332,   0.9999881992347881173, // XM4I+T*Xd4I[2]
       0.2429490550483593421,  -0.0013536721008048170,  49.9996192959322203198, //      T*XM4I[2]
      -0.0026428817769940e-9,  -8.1463562741672220e-9,  18.2024058051186966e-9, // Xd4I[0]
       0.9999256809514446605,  -0.0111813722062681620,  -0.0048589597008613673, //        XM4I[0]
       8.1401195144477102e-9,  -0.0910561571313818e-9,  -1.2019145949013101e-9, // Xd4I[1]
       0.0111813717563032290,   0.9999374861373183740,  -0.0000272585320447865, //        XM4I[1]
     -18.1880492284604517e-9,   1.3658005511963221e-9,   0.0884130375706395e-9, // Xd4I[2]
       0.0048589607363144413,  -0.0000270733285476070,   0.9999881948141177111};//        XM4I[2]
    **/

   /** 
    * Inverse matrix of PM4I, which converts ICRS,Ep=J2000 into FK4(B1950),Ep=B1950.
    * It is identical to:<br>
    * <code>{@link AstroMath#motionMatrix}({@link #fromICRSbase}, -TJ, {@link AstroMath#U3matrix})</code>
   static public final double[] PMI4 = {
    // AstroMath#motionMatrix}(fromICRSbase, -TJ, AstroMath.U3matrix)
       0.9999256809514446604,   0.0111813717563032290,   0.0048589607363144413, // i=0 (x)  
     -49.9964935896983064881,  -0.5590709309577118072,  -0.2429490550483593421,
      -0.0111813722062681620,   0.9999374861373183739,  -0.0000270733285476070, // i=1 (y) 
       0.5590709534560527533, -49.9970838514658597676,   0.0013536721008048170,
      -0.0048589597008613673,  -0.0000272585320447865,   0.9999881948141177111, // i=2 (z) 
       0.2429490032754886528,   0.0013629323144746095, -49.9996192959322203198,
      -0.0026428817769940e-9,   8.1401195144477102e-9, -18.1880492284604517e-9, // i=3 (vx)
       0.9999256810835893031,   0.0111809647486216819,   0.0048598701425873101,
      -8.1463562741672220e-9,  -0.0910561571313818e-9,   1.3658005511963221e-9, // i=4 (vy)
      -0.0111809648867473220,   0.9999374906901453120,  -0.0000271416188613808,
      18.2024058051186966e-9,  -1.2019145949013101e-9,   0.0884130375706395e-9, // i=5 (vz)
      -0.0048598698249660775,  -0.0000271984360631709,   0.9999881903934473050}; 
    **/

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieve a specific FK4 frame saved in "fixedFrames".
   * @param equinox the equinox of definition, in <b>Besselian Year</b>.
   * @param epoch   the epoch in <b>Besselian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the frame if previsouly saved, null if not yet existing.
  **/
    public static FK4 scan(double equinox, double epoch) {
        if(DEBUG) System.out.println("#...FK4.scan(B" + equinox + ", Ep=B" + epoch + ")");
        if(fixedFrames==null) return(null);
        boolean anyEpoch = Double.isNaN(epoch);
        // Remember: epoch in saved in *Julian Years*
        double Jep = Astrotime.B2J(epoch);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = i.next();
            if(!(o instanceof FK4)) continue;
            FK4 f = (FK4)o;
            if((f.fixed&0xf)!=0) continue;	// non-standard frame
            //System.out.print("#...FK4.get(" + equinox + ", " + epoch + "): examine " + f);
            //System.out.print("[epoch=" + f.epoch + ",Jep=" + Jep + "]");
            //System.out.print(", diff_eq=" + (f.equinox-equinox));
            //System.out.print(", diff_ep=" + (f.epoch-Jep));
            //System.out.println("; compare=" + (f.equinox==equinox && Math.abs(f.epoch-Jep)<3.e-8));
            if(f.equinox!=equinox) continue;
            if(anyEpoch||(Math.abs(f.epoch-Jep)<Jsec))
                return f;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) a FK4 frame
   * @param equinox the frame <em>equinox</em>, in <b>Besselian</b> years
   * @param epoch default <i>epoch</i> of the frame, in <b>Besselian</b> years
   * @return The frame
  **/
    public static FK4 create(double equinox, double epoch) {
        FK4 f = scan(equinox, epoch);
        if(f==null) {
            f = new FK4(equinox, epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Create (and mark as fixed) a FK4 frame with standard epoch
   * @param equinox the equinox of definition, in <b>Besselian Year</b>.
   * @return  the corresponding FK4 frame.
  **/
    public static FK4 create(double equinox) {
	return create(equinox, equinox);
    }

  /**
   * Instanciate the default FK4 frame
   * @return  the standard FK4(B1950) coordinate frame.
  **/
    public static FK4 create() {
	return create(1950.0, 1950.0);
    }

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an FK4 frame
   * @param equinox the equinox of definition, in Besselian Year.
   * @param epoch   the epoch in <b>Besselian</b> Year.
  **/
    public FK4(double equinox, double epoch) {
        if(Double.isNaN(epoch)) epoch = equinox;
	this.precision = 6;	// Intrinsic precision 0.1"
	this.equinox = equinox;
	this.epoch = Astrotime.B2J(epoch);
	this.name = "FK4(B" + equinox + ")";
        full_name = name.substring(0, name.length()-1)
                  + ",Ep=B" + epoch + ")";
        ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
	if (Math.abs(equinox-1950.0)>1.e-4) {   // 1.e-4yr ~ 1hr
	    toBaseEquinox = precessionMatrix(equinox, 1950.);
            toICRSmatrix   = AstroMath.m36p(toICRSbase, toBaseEquinox);
            fromICRSmatrix = AstroMath.rot_inv(toICRSmatrix);
	    ev_eq = eterm(equinox);
	}
	else {  	// B1950.0
	    toBaseEquinox  = AstroMath.U3matrix;
            toICRSmatrix   = toICRSbase;
            fromICRSmatrix = fromICRSbase;
	    ev_eq = ev50;
	}
        compute_dotMatrix(this);
        if(DEBUG) {
            AstroMath.checkUnity("#---Verify to/fromICRSbase:", AstroMath.m36p(toICRSbase, fromICRSbase));
            System.out.println("#---Constructing Astroframe: " + full_name);
            System.out.print(AstroMath.toString("#...fromICRSmatrix:\n", fromICRSmatrix));
            System.out.print(AstroMath.toString("#...(recomputed)..:\n",   AstroMath.m36p(AstroMath.rot_inv(toBaseEquinox), AstroMath.rot_inv(toICRSbase))));
            System.out.print(AstroMath.toString("#.....toICRSmatrix:\n",   toICRSmatrix));
            System.out.print(AstroMath.toString("#..6x6toICRSmatrix:\n",   AstroMath.m36p(AstroMath.m6(toICRSbase), AstroMath.m6(toBaseEquinox))));
            System.out.print(AstroMath.toString("#.....Product=.....\n",
                AstroMath.m36p(fromICRSmatrix, toICRSmatrix)));
            System.out.print(AstroMath.toString("#....toBaseEquinox:\n",  toBaseEquinox));
        }
    }

   // ===========================================================
   // 			Precession in FK4 system
   // ===========================================================

   /**
    * Precession matrix from equinox t0 to t1 (Besselian Years)
    * @param eq0 equinox at original equinox (Besselian year)
    * @param eq1 equinox of destination      (Besselian year)
    * @return the rotation matrix R that converts t<sub>0</sub> into t<sub>1</sub> as
    *                      u(t<sub>1</sub>) = R * u(t<sub>0</sub>)
   **/
    static public double[] precessionMatrix(double eq0, double eq1) {
      /* Note: Choose t0 closest to 1900 */
      double t0, dt, z, theta, zeta;
      boolean reverse = false;
        t0 = eq0 - 1900; dt = eq1 - eq0; 
	if (Math.abs(t0) > Math.abs(t0+dt)) {	// t0+dt = t1
            reverse = true;
	    t0 += dt; 
	    dt = -dt;
	}
        t0 /= 1000.; dt /= 1000.;	// Express in Milleniums
	zeta = dt*(23042.53+ t0*(139.73+0.06*t0)
	     + dt*(30.23+18.*dt-0.27*t0) ) /3600.;
	z    = zeta + dt*dt*(79.27+0.66*t0+0.32*dt)/3600.;
	theta = dt* (20046.85 - t0*(85.33+0.37*t0)
	      - dt*(42.67+0.37*t0+41.8*dt) )/3600.;
	if (reverse) //return(Coo.eulerMatrix(-zeta, -theta, -z));
              return(AstroMath.rotation("zyz", -zeta, theta, -z));
	else//return(Coo.eulerMatrix(z, theta, zeta));
	      return(AstroMath.rotation("zyz", z, -theta, zeta));
    }

   // ===========================================================
   // 			Compute the e-term of aberration
   // ===========================================================

    /**  Compute the e-term (change due to the ellipticity of Earth orbit).
     * @param  y the epoch, in Julian years
     * @return the 3 components of the E-term
    **/
    public static final double[] eterm(double y) {
      double t, e, L, eps, cL;
      double ev[] = new double[3];
        t   =  (y - 1900.e0)/1.e2;	// Centuries
	eps = 23.452   - 0.013*t;	// Mean obliquity
	e   = 0.016751 - 0.000042*t;	// Eccentricity
	L   = 281.221  + 1.719*t;	// Mean longitude of perihelion

	e  *= (20.496/AstroMath.ARCSEC);	// Aberration
	cL  = AstroMath.cosd(L);
	ev[0] =  e * AstroMath.sind(L);
	ev[1] = -e * cL * AstroMath.cosd(eps);
	ev[2] = -e * cL * AstroMath.sind(eps);
	if(DEBUG) System.out.println("#...FK4.e-term(B" + y + "): " 
		+ AstroMath.toString(ev));
        return(ev);
    }

    /**  Subtract the E-term of aberration from a cartesian position.
     * @param  u  the direction vector, modified by Eterm
    **/
    public final void subEterm(double[] u) {
        Coo.sub(u, ev_eq);
    }

    /** Add the the E-term of aberration from a cartesian position.
     * @param  u  the direction vector, modified by Eterm
    **/
    public final void addEterm(double[] u) {
        Coo.add(u, ev_eq);
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

   /** (standard method is ok)
    * Convert the position to its ICRS equivalent, assuming zero proper motion in ICRS
    * @param coo on input the position in this frame; on ouput the ICRS
    public void toICRS(Coo coo) {
        if(toBaseEquinox != AstroMath.U3matrix) 
            coo.rotate(toBaseEquinox);
        coo.rotate(XM4I);
    }
   **/

   /** (standard method is ok)
    * Compute FK4 position from ICRS, assuming zero proper motion in ICRS
    * @param coo a coordinate which is converted from ICRS to FK4(B1950), Ep=B1950.
    public void fromICRS(Coo coo) {
	coo.rotate_1(XM4I);
        if(toBaseEquinox != AstroMath.U3matrix) 
            coo.rotate_1(toBaseEquinox);
    }
   **/

  /** (standard method is ok)
   * Convert the position/velocity from this.frame to ICRS,Ep=J2000.
   * A dedicated method is required because u6 may contain only x,y,z
   * @param u6 6-vector containing position + movement (rad/Jyr) in frame.
   * @param v6 6-vector containing position + movement (rad/Jyr) in ICRS, epoch 2000.
   *        Note that the 6-vector assumes no e-term (was removed), and
   *        a derivative vs Jyr (motion was converted from 
   *        Byr<sup>-1</sup> to Jyr<sup>-1</sup>).
    public void toICRS(double[] u6, double[] v6) {
        if(u6.length==3) {  // no proper motion!
            AstroMath.m36v(ICRSmatrix, u6, v6);
        }
        else {
            if(toICRSbase==null) setICRSbase();
            // Simple multiplication 6x6 matrix -> 6-vector
	    AstroMath.m36v(toICRSbase, u6, v6);
        }
    }
  **/

  /** (standard method is ok)
   * Convert the position/velocity from ICRS,Ep=J2000 to this.frame.
   * A dedicated method is required because u6 may contain only x,y,z
   * @param u6 6-vector containing position + movement (rad/Jyr).
   * @param u6 6-vector containing position + movement (rad/Jyr) in ICRS. epoch 2000.
   * @param v6 6-vector containing position + movement (rad/Jyr) in frame.
   *        Note that the 6-vector computed has no e-term (was removed), and
   *        the derivative is vs Jyr (motion not converted into Byr<sup>-1</sup>)).
    public void fromICRS(double[] u6, double[] v6) {
        if(u6.length==3) {  // no proper motion!
            AstroMath.m36v(ICRSmatrix_1, u6, v6);
        }
        else {
            if(fromICRSbase==null) setICRSbase();
            // Simple multiplication 6x6 matrix -> 6-vector
	    AstroMath.m36v(fromICRSbase, u6, v6);
        }
    }
  **/

  /** (not useful, use fromICRS(u6, v6) with (0,0,0) as last three values in u6)
   * Estimate the proper motions in the FK4 system, assuming a zero
   * proper motion in the ICRS.
   * @param u6 6-vector containing position + derivative (rad/Jyr);
   *        the derivative (u6[3-5]) is updated.
  **/
    /*--- not really useful ...
    public static void estimateFK4motion(double[] u6) {
        // the motion is -scalar(Vz4I[i],u) 
        for(int i=0; i<3; i++) {
            int i3 = i*3;
	    double w = Vz4I[i3+0]*u6[0] + Vz4I[i3+1]*u6[1] + Vz4I[i3+2]*u6[2];
            u6[i+3] = -w;
        }
    }
    ---*/

}
