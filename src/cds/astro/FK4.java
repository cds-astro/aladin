package cds.astro;

/*==================================================================
                FK4  (Astroframe -> Equatorial -> FK4)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The FK4 is an equatorial coordinate system (coordinate system linked to 
 * the Earth) based on its B1950 position. 
 * The units used for time specification is the Besselian Year.
 *
 * The conversion to/from FK5 (which coincides with ICRS) uses the 
 * algorithm published by Standish (1982A&A...115...20S)
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */

public class FK4 extends Equatorial {
  /**
   * In addition to the equinox, we store the precession matrix
   * which converts to the standard epoch.
  **/
   static private boolean DEBUG=false;
   static private double[] ev50 = eterm(1950.);	// e-term for B1950
   protected double[][] toBaseEquinox;		// Precession to B1950
   protected double[] ev_eq;			// e-term for Equinox

  /**
   * Initialize the parameters of this equatorial frame.
   * @param equinox the equinox of definition, in Besselian Year.
   * @param epoch   the default epoch, in Julian Year
  **/
   private void initialize(double equinox, double epoch) {
	this.precision = 6;	// Intrinsic precision
        base_epoch = Astrotime.B2J(1950.);
	this.equinox = equinox;
	this.epoch = epoch;
	this.name = "FK4(B" + equinox + ")";
        this.ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        this.ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
	if (Math.abs(equinox-1950.0)>0.0003) {
	     toBaseEquinox = this.precessionMatrix(equinox, 1950.);
	     ev_eq = eterm(equinox);
	}
	else {
	    toBaseEquinox = Coo.Umatrix3;
	    ev_eq = ev50;
	}
   }

   /**
    * Constants for Conversion from FK4 to FK5
    * The 6x6 matrix to move from FK4 to FK5
    * Proper motions are in arcsec/century (10mas/yr) 
   **/

   /** Matrix 6x1 to compute the e-term */
   static protected double[] A = {		// For e-term
      //-0.33530/206265., -0.06584/206265., -0.02855/206265.,
           -1.62557e-6,    -0.31919e-6,      -0.13843e-6,
      // 1.245e-3,      -1.580e-3,      -0.659e-3  
         1.244e-3,      -1.579e-3,      -0.660e-3    };

   /** 
    * Table 2 of Standish (1982A&A...115...20S), rediscussed by Soma and
    * Aoki (1990A&A...240..150S), and apparently slightly modified 
    * by the starlink group.
    **/
   static protected double[][] EM = {	// 
     { 	 0.9999256782, 		-0.0111820611, 		-0.0048579477,
		 2.42395018e-6,		-0.02710663e-6,		-0.01177656e-6},
     {	 0.0111820610,     	 0.9999374784,     	-0.0000271765,
		 0.02710663e-6,      	 2.42397878e-6,      	-0.00006587e-6},
     {	 0.0048579479,     	-0.0000271474,     	 0.9999881997,
		 0.01177656e-6,      	-0.00006582e-6,      	 2.42410173e-6},
     {	-0.000551,         	-0.238565,         	 0.435739,
		 0.99994704e0,       	-0.01118251e0,       	-0.00485767e0},
     {	 0.238514,         	-0.002667,         	-0.008541,
		 0.01118251e0,       	 0.99995883e0,       	-0.00002718e0},
     {	-0.435623,         	 0.012254,         	 0.002117,
		 0.00485767e0,       	-0.00002714e0,       	 1.00000956e0}
     };

   /* For moving from FK4 to FK5 without proper motion, the EM50 matrix
      is used
      EM50[i][j] = EM[i][j] + EM[i+3][j]*dtB/PMF ;
   */
   static private double EM50[][] = {
     { 0.9999256795356672, -0.0111814827996970, -0.0048590039655699},
     { 0.0111814828233251,  0.9999374848650175, -0.0000271557959449},
     { 0.0048590038843768, -0.0000271771046587,  0.9999881945682256}
   } ;

   /** The 6x6 matrix to move from FK5 to FK4 */
   static protected double[][] EM1 = {	/* FK5 ==> FK4	*/
     { /*0.9999256795,      0.0111814828,     0.0048590039,*/
         0.9999256795,      0.0111814829,     0.0048590038,
	    /*-2.42389840e-6,      -0.02710544e-6,     -0.01177742e-6*/
	      -2.42389840e-6,      -0.02710545e-6,     -0.01177742e-6},
     {  -0.0111814828,      0.9999374849,    -0.0000271771,
	     /*0.02710544e-6,      -2.42392702e-6,      0.00006585e-6*/
	       0.02710545e-6,      -2.42392702e-6,      0.00006585e-6},
     {/*-0.0048590040,     -0.0000271557,     0.9999881946,*/
        -0.0048590040,     -0.0000271558,     0.9999881946,
	       0.01177742e-6,       0.00006585e-6,     -2.42404995e-6},
     {/*-0.000551,          0.238509,        -0.435614,*/
        -0.000550,          0.238509,        -0.435613,
  	       0.99990432e0,        0.01118145e0,       0.00485852e0},
     {/*-0.238560,         -0.002667,         0.012254,*/
        -0.238559,         -0.002668,         0.012254,
	      -0.01118145e0,        0.99991613e0,      -0.00002717e0},
     {/* 0.435730,         -0.008541,         0.002117,*/
         0.435730,         -0.008541,         0.002116,
	      -0.00485852e0,       -0.00002716e0,       0.99996684e0}
     };

    /** To estimate the proper motions in FK4 **/
     static double[][] EM2 = null; /* new double[6][6];	*/

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an FK4 frame
   * @param equinox the equinox of definition, in Besselian Year.
   * @param epoch   the epoch in Besselian Year.
  **/
    public FK4(double equinox, double epoch) {
	initialize(equinox, Astrotime.B2J(epoch));
    }

  /**
   * Instanciate an FK4 frame
   * @param equinox the equinox of definition, in Besselian Year.
  **/
    public FK4(double equinox) {
	initialize(equinox, Astrotime.B2J(equinox));
    }

  /**
   * Instanciate an FK4 frame (at default B1950 equinox)
  **/
    public FK4() {
	initialize(1950., base_epoch);
    }

  /**
   * Get the conversion to ICRS matrix
   * @return null (it's impossible to to reduce the transformation to a matrix)
  **/
    public double[][] toICRSmatrix() {
	return(null);
    }

   // ===========================================================
   // 			Precession in FK4 system
   // ===========================================================

   /**
    * Precession matrix from equinox t0 to t1 (Besselian Years)
    * @param eq0 equinox at original equinox (Besselian year)
    * @param eq1 equinox of destination      (Besselian year)
    * @return the rotation that converts t0 into t1 by  u1 = R * u0
   **/
    static public double[][] precessionMatrix(double eq0, double eq1) {
      /* Note: Seems to be not reversible. Choose t0 closest to 1900 */
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
	if (reverse) return(Coo.eulerMatrix(-zeta, -theta, -z));
	else  return(Coo.eulerMatrix(z, theta, zeta));
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

    /* For debugging purposes: display a 6-vector */
    private static void pr6(double u6[], String t1, String t2) {
	double v[] = { u6[0], u6[1], u6[2] };
	    System.out.println(t1 + Coo.toString(v));
	    v[0]=u6[3]; v[1]=u6[4]; v[2]=u6[5];
	    System.out.println(t2 + Coo.toString(v));
    }

    /* Computation e-term */
    /**  
     * Compute the e-term (change due to the ellipticity of Earth orbit).
     * @param  y the epoch, in Besselian years
     * @return the 3 components of the E-term
    **/
    public static final double[] eterm(double y) {
      double t, e, L, eps, cL;
      double ev[] = new double[3];
        t   =  (y - 1900.e0)/1.e2;	// Centuries
	eps = 23.452   - 0.013*t;	// Mean obliquity
	e   = 0.016751 - 0.000042*t;	// Eccentricity
	L   = 281.221  + 1.719*t;	// Mean longitude of perihelion

	e  *= (20.496/(3600.*180./Math.PI));	// Aberration
	cL  = AstroMath.cosd(L);
	ev[0] =  e * AstroMath.sind(L);
	ev[1] = -e * cL * AstroMath.cosd(eps);
	ev[2] = -e * cL * AstroMath.sind(eps);
	if(DEBUG) System.out.println(".... e-term(B" + y + "): " 
		+ Coo.toString(ev));
        return(ev);
    }

  /**
   * Convert the position to FK5 system -- assumed to be ICRS.
   * We assume no proper motion in FK5, and an equinox/epoch in B1950.
   * @param coo a coordinate which is converted from FK4(B1950) to FK5(J2000)
  **/
    public static void toFK5(Coo coo) {
      double u6[] = new double[6]; 
    /*-------------------------------
     * NEW Method using the NEW Method Matrix
     **
	double w, x, y, z;
	double u[] = { coo.x, coo.y, coo.z }; 	// coo.getUvector();
	// Remove e-term (replace r by r - r^(A^r))
	w = u[0]*A[0] + u[1]*A[1] + u[2]*A[2]; // A*r
	u[0] -= A[0] - w*u[0];
	u[1] -= A[1] - w*u[1];
	u[2] -= A[2] - w*u[2];
	// With EM50 matrix:
	x = EM50[0][0]*u[0] + EM50[0][1]*u[1] + EM50[0][2]*u[2];
	y = EM50[1][0]*u[0] + EM50[1][1]*u[1] + EM50[1][2]*u[2];
	z = EM50[2][0]*u[0] + EM50[2][1]*u[1] + EM50[2][2]*u[2];
	// Renormalize the Unit Vector
	w = Math.sqrt(x*x + y*y + z*z);
	x /= w; y /= w; z /= w;
	u[0]=x; u[1]=y; u[2]=z; 
	System.out.println("....toFK5.fast : " + Coo.toString(u));
	coo.set(x, y, z);
      ---------------------------------*/

        // Estimate the proper motion...
        coo.copyUvector(u6);
        estimateFK4motion(u6);
	// Convert to FK5
	toFK5(u6);	// Now, in FK5, Ep.=Eq.=J2000
	/* Verify consecutive transformations if they match... */
	/* ---------------------------------- Test Code
	if(DEBUG) {
	  double v[] = new double[3]; int i;
	    eterm(1950., v);
	    v[0]=u6[0]; v[1]=u6[1]; v[2]=u6[2];
	    System.out.println("....toFK5 gives: " + Coo.toString(v));
	    v[0]=u6[3]; v[1]=u6[4]; v[2]=u6[5];
	    System.out.println("          .dot.  " + Coo.toString(v));
	  for(i=0; i<10; i++) {
	    //u6[3] = u6[4] = u6[5] = 0;
	    fromFK5(u6);
	    v[0]=u6[0]; v[1]=u6[1]; v[2]=u6[2];
	    System.out.println(".... back FK4  : " + Coo.toString(v));
	    v[0]=u6[3]; v[1]=u6[4]; v[2]=u6[5];
	    System.out.println("          .dot.  " + Coo.toString(v));
	    System.out.println("          norm:  " 
		    + Math.sqrt(u6[0]*u6[0]+u6[1]*u6[1]+u6[2]*u6[2]));
	    toFK5(u6);
	    v[0]=u6[0]; v[1]=u6[1]; v[2]=u6[2];
	    System.out.println("....FK5 again  : " + Coo.toString(v));
	    v[0]=u6[3]; v[1]=u6[4]; v[2]=u6[5];
	    System.out.println("          .dot.  " + Coo.toString(v));
	    System.out.println("          norm:  " 
		    + Math.sqrt(u6[0]*u6[0]+u6[1]*u6[1]+u6[2]*u6[2]));
	  }
	}
	---------------------------------------------*/
	coo.set(u6[0], u6[1], u6[2]);
    }

  /**
   * Convert the position from standard B1950 FK4 to standard J2000 FK5 system 
   * with Standish's algorithm.
   * Standish uses the 6x6 matrix applied on the 6-vector
   * (x y z xd yd zd) representing the position + derivative 
   * (derivatives expressed in arcsec/century) with the EM matrix
   * applied on the vector after subtraction of the E-term
   * The E-term (due to the Earth's elliptical orbit) can be expressed
   *  r ^ (A ^ r) = A - (A*r)r
   *  where ^ represents the vectorial product, and * the scalar product.
   * @param u6 6-vector containing position + mouvement (rad/yr)
  **/
    public static void toFK5(double[] u6) {
	double v[] = new double[6];
	double w, wd;
	int i, j;
	// *** Unknown positions: do nothing !
	if ((u6[0]==0)&&(u6[1]==0)&&(u6[2]==0)) return;
	// Convert the derivative part, originally in rad/yr, into arcsec/cy
	v[0] = u6[0]; v[1] = u6[1]; v[2] = u6[2];
	v[3] = u6[3] * (360000.*180./Math.PI);
	v[4] = u6[4] * (360000.*180./Math.PI);
	v[5] = u6[5] * (360000.*180./Math.PI);
	if(DEBUG) pr6(v, "        toFK5(1) ", "                 ");
	// Remove e-term (replace r by r - r^(A^r))
	w  = v[0]*A[0] + v[1]*A[1] + v[2]*A[2]; // A*r
	wd = v[0]*A[3] + v[1]*A[4] + v[2]*A[5];	// A1*r
	v[0] -= A[0] - w*u6[0]; 
	v[1] -= A[1] - w*u6[1]; 
	v[2] -= A[2] - w*u6[2]; 
	// Correction to the proper motions (not in Standish)
	v[3] -= A[3] - wd*u6[0]; 
	v[4] -= A[4] - wd*u6[1]; 
	v[5] -= A[5] - wd*u6[2]; 
	// Rotate with the EM matrix
	for (i=0; i<6; i++) {
	    w = 0;
	    for (j=0; j<6; j++) w += EM[i][j]*v[j];
	    u6[i] = w;
	}
	if(DEBUG) pr6(u6, "        toFK5(2) ", "                 ");
	// Convert the derivative part to rad/yr
	u6[3] /= (360000.*180./Math.PI);
	u6[4] /= (360000.*180./Math.PI);
	u6[5] /= (360000.*180./Math.PI);
	// Renormalize
	w = Math.sqrt(u6[0]*u6[0] + u6[1]*u6[1] + u6[2]*u6[2]);
	for (i=0; i<6; i++) u6[i] /= w;
    }

   /**
    * Compute FK4 position from FK5, assuming no proper motion in FK5
    * and an observation in B1950.
    * @param coo a coordinate which is converted from FK5(J2000) to FK4(B1950)
   **/
    public static void fromFK5(Coo coo) {
    /* Note: could be coded with using the 6-vector method:
     * ----
        double u6[] = new double[6];
        coo.copyUvector(u6); u6[3] = u6[4] = u6[5] = 0.;
        fromFK5(u6);
     *---------------------------------------------------*/
	double w, x, y, z;
	double u[] = { coo.x, coo.y, coo.z }; 	// coo.getUvector();
	// 6-vector Rotation. The velocity is zero.
	x = EM1[0][0]*u[0] + EM1[0][1]*u[1] + EM1[0][2]*u[2];
	y = EM1[1][0]*u[0] + EM1[1][1]*u[1] + EM1[1][2]*u[2];
	z = EM1[2][0]*u[0] + EM1[2][1]*u[1] + EM1[2][2]*u[2];
	// Renormalize the Unit Vector
	w = Math.sqrt(x*x + y*y + z*z);
	if (w==0) return; 		// Undefined vector => no change
	x /= w; y /= w; z /= w;	
	// Apply e-term
	w = x*A[0] + y*A[1] + z*A[2]; 	// A*r
	x += A[0] - w*x;
	y += A[1] - w*y;
	z += A[2] - w*z;
	// Renormalize the Unit Vector (a tiny bit)
	w = Math.sqrt(x*x + y*y + z*z);
	x /= w; y /= w; z /= w;
	coo.set(x, y, z);
    }

  /**
   * Convert the position from standard J2000 FK5 to standard B1950 FK4 system
   * with Standish's algorithm.
   * Standish uses the 6x6 matrix applied on the 6-vector
   * (x y z xd yd zd) representing the position + derivative 
   * (derivatives expressed in arcsec/century) with the EM matrix
   * applied on the vector after subtraction of the E-term
   * The E-term (due to the Earth's elliptical orbit) can be expressed
   *  r ^ (A ^ r) = A - (A*r)r
   *  where ^ represents the vectorial product, and * the scalar product.
   * @param u6 6-vector containing position + mouvement (rad/yr)
  **/
    public static void fromFK5(double[] u6) {
	double v[] = new double[6];
	double w, wd;
	int i, j;
	// *** Unknown positions: do nothing !
	if ((u6[0]*u6[0] + u6[1]*u6[1] + u6[2]*u6[2]) < 1.e-10)
	    return;
	// Convert the derivative part, originally in rad/yr, into arcsec/cy
	// v[0] = u6[0]; v[1] = u6[1]; v[2] = u6[2];
	// v[3] = u6[3] * (360000.*180./Math.PI);
	// v[4] = u6[4] * (360000.*180./Math.PI);
	// v[5] = u6[5] * (360000.*180./Math.PI);
	u6[3] *= (360000.*180./Math.PI);
	u6[4] *= (360000.*180./Math.PI);
	u6[5] *= (360000.*180./Math.PI);
	if(DEBUG) pr6(u6, "        from5(1) ", "                 ");
	// Rotate with the EM1 matrix
	for (i=0; i<6; i++) {
	    w = 0;
	    for (j=0; j<6; j++) w += EM1[i][j]*u6[j];
	    v[i] = w;
	}
	// Renormalize
	w = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
	for (i=0; i<6; i++) v[i] /= w;
	if(DEBUG) pr6(v, "        from5(+) ", "                 ");
	// Include the e-term (replace r by r + r^(A^r))
	w  = v[0]*A[0] + v[1]*A[1] + v[2]*A[2];	// A*r
	wd = v[0]*A[3] + v[1]*A[4] + v[2]*A[5];	// A1*r
	// Correction to the proper motions (not in Standish)
	u6[3] = v[3] + A[3] - wd*v[0]; 
	u6[4] = v[4] + A[4] - wd*v[1]; 
	u6[5] = v[5] + A[5] - wd*v[2]; 
	// Correction to the position
	u6[0] = v[0] + A[0] - w*v[0]; 
	u6[1] = v[1] + A[1] - w*v[1]; 
	u6[2] = v[2] + A[2] - w*v[2]; 
	// Renormalize (a tiny amount only!)
	w = Math.sqrt(u6[0]*u6[0] + u6[1]*u6[1] + u6[2]*u6[2]);
	for (i=0; i<6; i++) u6[i] /= w;
	if(DEBUG) pr6(u6, "        from5(2) ", "                 ");
	// Convert the derivative part to rad/yr
	u6[3] /= (360000.*180./Math.PI);
	u6[4] /= (360000.*180./Math.PI);
	u6[5] /= (360000.*180./Math.PI);
    }

  /**
   * Estimate the proper motions in the FK4 system, assuming a zero
   * proper motion in the FK5.
   * @param u6 6-vector containing position + mouvement (rad/yr);
   * 		the mouvement (derivative, pos. u6[3-5]) is updated.
  **/
    public static void estimateFK4motion(double[] u6) {
	double v1[] = new double[6];
	double w;
	int i, j, k;
	// the matrix EM2 = EM1 * (EM restricted to 3x3):
	// Compute this matrix if not yet done.
	if (EM2 == null) { double tm[][] = new double[6][6];
	    for (i=0; i<6; i++) for (j=0; j<6; j++) {
	        w = 0;
	        for (k=0; k<3; k++) w += EM1[i][k] * EM[k][j];
	        tm[i][j] = w;
	    }
	    EM2 = tm;
	}
	for (i=0; i<6; i++) {
	    w=0;
	    for (j=0; j<3; j++) 
		w += EM2[i][j] * u6[j];
	    v1[i] = w;
	}
	// Compute scalars u.A
	w = v1[0]*A[3] + v1[1]*A[4] + v1[2]*A[5];
	// Include A-term
	for (i=3; i<6; i++) v1[i] += A[i] - w*v1[i-3];
	// Output Derivative (in rad/yr)
	for (i=3; i<6; i++) u6[i] = v1[i] / (360000.*180./Math.PI);
    }

   /**
    * Convert the position to its ICRS equivalent.
    * @param u6  the 6-vector (cartesian position + derivative) 
    * 		Velocity in Jyr<sup>-1</sup>).
    * 	        u6 is on FK4 on input, in ICRS on output)
   **/
    public void toICRS(double[] u6) {
	if (toBaseEquinox != Coo.Umatrix3) {
	  double[] ev = new double[3];
	    Coo.sub(u6, ev_eq);		// Subtract E-term for Equinox
	    Coo.rotateVector(toBaseEquinox, u6);
	    Coo.add(u6, ev50);		// Add E-term for B1950.0
	}
	// System.out.println("....FK4.toICRS, base_epoch=" + base_epoch);
	toFK5(u6);	// Converts rigorously.
    }

   /**
    * Convert the position to its ICRS equivalent.
    * @param coo on input the position in this frame; on ouput the ICRS
   **/
    public void toICRS(Coo coo) {
	if (toBaseEquinox != Coo.Umatrix3) {
	  double[] ev = new double[3];
	    coo.sub(ev_eq);		// Subtract E-term for Equinox
	    coo.rotate(toBaseEquinox);
	    coo.add(ev50);		// Add E-term for B1950.0
	}
	toFK5(coo);	// Converts rigorously.
    }

   /**
    * Convert the position from the ICRS frame to FK4.
    * @param u6  the 6-vector (cartesian position + derivative)
    *            Velocity in Jyr<sup>-1</sup>).
    *            (in ICRS on input, in FK4 on output)
   **/
    public void fromICRS(double[] u6) {
	// System.out.println("....FK4.fromICRS, base_epoch=" + base_epoch);
	fromFK5(u6);
	if (toBaseEquinox != Coo.Umatrix3) {
	  double[] ev = new double[3];
	    Coo.sub(u6, ev50);		// Subtract E-term for B1950.0
	    Coo.rotateVector_1(toBaseEquinox, u6);
	    Coo.add(u6, ev_eq);		// Add E-term for Equinox
	}
    }

   /**
    * Convert the position from the ICRS frame.
    * @param coo on input the ICRS position, on output in FK4.
   **/
    public void fromICRS(Coo coo) {
      double[] u = new double[6];
	coo.copyUvector(u);
	u[3] = u[4] = u[5] = 0.;
	fromFK5(u);
	if(DEBUG) pr6(u, "FK4from ", "FK4from.");
	coo.set(u[0], u[1], u[2]);
	//if(DEBUG) coo.dump("FK4.fromICRS");
	//fromFK5(coo);
	if (toBaseEquinox != null) {
	  double[] ev = new double[3];
	    coo.sub(ev50);		// Subtract E-term for B1950.0
	    coo.rotate_1(toBaseEquinox);
	    coo.add(ev_eq);		// Add E-term for Equinox
	}
    }
}
