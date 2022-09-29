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

package cds.astro ;

/*==================================================================
                Astropos Class  (Coo -> Astrocoo -> Astropos)
 *==================================================================*/
import java.io.Serializable;
// for parseException
import java.text.ParseException;
import java.util.Arrays;

/**
 * The class <b>Astropos</b> adds to <b>Astrocoo</b> the notions 
 * of <i>movement</i> (proper motions, velocity) and <i>error</i> ellipses.
 * <P>The edition of the position in an Astropos can be done in a StringBuffer
 * (<B>edit</B> methods) with options like sexagesimal or decimal edition,
 * or as a String with one of the available <B>toString</B> methods.
 * In string format, an astropos looks like:<br>
 * <i>longitude  +/- latitude</i> <b>(</b><i>epoch</i><b>)</b>
 * <b>[</b><i>errorEllipse_mas</i> <b>(</b><i>meanEpoch</i><b>)</b><b>]</b>
 * <i>pm_lon pm_lat</i>  <b>[</b><i>errorEllipse_mas/yr</i> <b>]</b>
 * <i>parallax_mas</i> <b>[</b><i>error</i><b>]</b>
 * <i>radialVelocity_km/s</i> <b>[</b><i>error</i><b>]</b>
 * <P>The values are expressed internally as cartesian components in the
 * associated frame. Errors are expressed internally as a 6x6 symmetric
 * covariance matrix, stored as a 21-double array with [0..5] representing
 * variance and [6..20] the 15 covariances x:y, x:z, x:xd, x:yd, x-zd, y:z...,
 * also aligned with the frame main axes.
 * Errors in other frames are computed using the property that,
 * if <b>x</b> is multinormal with covariance matrix <b>V</b>
 * then <b>Tx</b> is multinormal with covariance matrix <b>TV<sup>t</sup>T</b>.
 * For instance, the variation of the 6-vector (x,y,x,xd,yd,zd) with time 
 * is linear with the following <b>T(t)</b> matrix:<PRE>
 *      1   0   0   t   0   0
 *      0   1   0   0   t   0
 *      0   0   1   0   0   t
 *      0   0   0   1   0   0
 *      0   0   0   0   1   0
 *      0   0   0   0   0   1
 * </PRE>
 * (notice that <b>T<sup>-1</sup>(t) = T(-t)</b> and 
 * <b>T(t<sub>1</sub>) T(t<sub>2</sub>) = T(t<sub>1</sub> + t<sub>2</sub>)</b>)
 * and the error propagations are easily computed by this method.
 * <P>Conversions into other frames ({@link #convert} method) imply a
 * change of all parameters (position, proper motions, errors). A change
 * of the epoch is achieved with the {@link #toEpoch} method.
 * @author Francois Ochsenbein [CDS]
 * @version 1.0  03-Aug-2004
 * @version 1.1  03-Sep-2006
 * @version 1.2  12-Jul-2007
 * @version 1.21 12-Jul-2007: bug in toEpoch
 * @version 1.22 18-Nev-2014: use setEditing
 * @version 2.0  18-Feb-2019: Simplified. Added Gaia-2 specific.
 */
public class Astropos extends Astrocoo implements Serializable {
	static boolean DEBUG = false; 
	/** Derivative of <b>x</b> cartesian component (in Jyr<sup>-1</sup>), at epoch */
	protected double xd;
	/** Derivative of <b>y</b> cartesian component (in Jyr<sup>-1</sup>), at epoch */
	protected double yd;
	/** Derivative of <b>z</b> cartesian component (in Jyr<sup>-1</sup>), at epoch */
	protected double zd;
	/** Elements of 3x3 rotation matrix from local frame (where (x,y,z)=(1,0,0)
	 *  to (x,y,z). 
	 *  Notice that R00=x, R01=y, R02=z at mean Epoch.
	 **/
	private double[] R;		// localMatrix.
	/*
    private double 
	// R00=x R01=y R02=z
	   R10,  R11, // R12=0 
           R20,  R21,  R22;
  /** 
	 * The 3-D angular velocities, in mas/yr, in the local frame.
	 * These are (Vϖ µα µδ), V being the radial velocity.
	 */
	protected double mu0, mu1, mu2;
	/** The velocity vector is a rotation of the Unit derivative:
	 *  v0 is radial, v1 and v2 are the proper motions mu(alpha) and mu(delta),
	 *  and are expressed in mas/yr (mas = milli-arcsec).
	 **/
	/** The mean epoch, expressed in Julian years, represents the epoch of the proper motions,
	 *  and also the epoch with minimal uncertainty on the position.
	 **/
	protected double meanEp;
	/** Parallax in mas, at mean Epoch */
	public double plx;	
	/** Radial velocity in km/s, at mean Epoch */
	public double rv;	
	/** Mean error on parallax in mas, at mean Epoch */
	public double e_plx;
	/** factor to transform correlation (plx,param) to correlation (mu,param) */
	public double e_rv;
	// Permanent values
	//public double permanent_epos;	// Permanent error on pos, in mas
	//public double permanent_epm ;	// Permanent error on pm , in mas
	/** specified components and errors are flagged by:
	 */
	private final static short HASpos = 1;	// position specified
	private final static short HASpm  = 2;	// proper motion specified
	private final static short HASplx = 4;	// parallax specified
	private final static short HASrv  = 8;	// radial velocity specified
	private final static short HASepos = 0x10;	// position error specified
	private final static short HASepm  = 0x20;	// proper motion error specified
	private final static short HASeplx = 0x40;	// parallax error specified
	private final static short HAServ  = 0x80;	// radial velocity error specified
	private final static short HAScorr = 0x100;	// correlations specified
	private final static short HASep  = 0x200;	// epoch explicitely specified
	private final static short HASmep = 0x400;	// mean epoch specified
	private final static short HASerr = HASepos|HASepm|HASeplx;	
	private short permanent;	// Status of permanent (fixed) parameters
	private short specified;	// Status of specified (or permanent) parameters.
	private short estimated;	// indicates estimated parameters
	/**
	 * How many significant digits should be used to edit the errors.
	 * If specified, it means that the error is edited with <i>at least</i> the 
	 * number of significant digits.
	 * It can be changed by {@link #setErrorPrecision}.
	 * When not specified, the {@link Astrocoo#precision} is used.
	 */
	protected byte errorprec=0;
	private byte  ready=0;	// 0x1: values set on fixed; 0x10: local; 0x20: errors

	private static String[] partnames = { "pos", "pm", "plx", "rv", 
			"e_pos", "e_pm", "e_plx", "e_rv", "corr",  "Ep", "mEp" };

	/** Covariance matrix between all 6 components in the fixed frame.
	 * The covariance matrix is symmetrically stored, i.e. is made of 21 doubles:
	 * <pre>
	 * | 0  6  7   8  9 10 |
	 * |    1 11  12 13 14 |
	 * |       2  15 16 17 |
	 * |           3 18 19 |
	 * |              4 20 |
	 * |                 5 |
	 *</pre>
	 * @see sym6index
	 **/
	protected double[] cov=null;
	/**
	 * The sigma's and correlations on the 5 astrometric parameters (in mas, mas/yr)
	 * in the same order as what's given by Gaia:<ul>
	 * <li> [0-4]: sigmas on α δ ϖ µα µδ
	 * <li> [5-14]: correlations 5=α:δ 6=α:ϖ 7=α:µα 8=α:µδ 9=δ:ϖ 
	 *              10=δ:µα 11=δ:µδ 12=ϖ:µα 13=ϖ:µδ 14=µα:µδ
	 * </ul>
	 * It is symmetrically stored:
	 * <pre>
	 * | 0  5  6  7  8 |
	 * |    1  9 10 11 |
	 * |       2 12 13 |
	 * |          3 14 |
	 * |             4 |
	 * </pre>
	 */
	protected double[] sig5corr=null;

	/**
	 * Estimation of the errors on position, from the precision.
	 **/
	static private double[] def_err = {
			180.*3.6e6,	// 0: no position
			1.2*3.6e6,	// 1: deg
			1.5*3.6e5,	// 2: 0.1deg
			2.1*6e4,	// 3: 1'
			2.1*6e3,	// 4: 0.1'
			2.1*1000.,	// 5: 1"
			2.1*100.,	// 6: 0.1"
			2.1*10.,	// 7: 0.01"
			2.1,		// 8: 1mas
			2.1/10.,	// 9: 0.1mas
			2.1/100.,	//10: 0.01mas
			2.1/1000.,	//11: 1uas
			2.1/10000.,	//12: 0.1uas
			2.1/100000.,	//13: 0.01uas
			2.1/1000000.,	//14: 1nas
			2.1/10000000.,	//15: 0.1nas
	};
	//static private double[]  def_rv  = { 0., 50. };	// Default RV
	//static private double[]  def_plx = { 9., 10. };	// Default plx (mas)
	/** Index in a symmetrically stored 6x6 matrix (21 elements)
	 *  in each of the 3 quarters 0(upper-left), 1(upper-right) 
	 *  and 2(lower-right).
	 *  <pre>
	 *  | 0 6  7   8  9 10 |
	 *  |   1 11  12 13 14 |
	 *  |      2  15 16 17 |
	 *  |          3 18 19 |
	 *  |             4 20 |
	 *  |                5 |
	 *  </pre>
	 **/
	static final int[] sym6index = { 
			0,6,7,    6,1,11,   7,11,2,	// upper-left
			8,9,10, 12,13,14, 15,16,17,	// upper-right and lower-left
			3,18,19, 18,4,20,  19,20,5 };	// lower-right
	/** Factor which converts proper motion into velocity */
	static public final double PMtoVEL = 4.740470446;
	/** The differnt strings expressing angular units */
	static public final String[] unitsymb = {
			"mas", "arcsec", "\"", "uas", "km/s", "\u00B5as", "\u03BCas", "m/s", "nas" };
	static public final double[] unit2mas = {
			1.0,   1000.0, 1000.0, 0.001,   1.0,   0.001,       0.001,    0.001, 1.e-6 };
	/** Symbols recognized to introduce a data: */
	static public final String[] datasymb = {
			"pm=", "\u00B5=", "\u03BC=",
			"plx=", "\u03D6=", "\u03C0=",
			"Vr=", "rv=", "RV=",
			"sigma=", "err=", "\u03C3=",
			"corr=", "cor=", "\u03C1=",
			"Ep=", "Epoch=", "epoch=", 
			"meanEp=", "mean=",     
	};
	static public final short[] datatype = {
			HASpm,  HASpm,  HASpm,  
			HASplx, HASplx, HASplx,  
			HASrv,  HASrv,  HASrv,	
			HASerr, HASerr, HASerr, 
			HAScorr, HAScorr, HAScorr, 
			HASep,  HASep,  HASep,	
			HASmep, HASmep,
	};
	/** Conversion factor for variances from mas² to rad²
	 */
	static final public double MAS2 = AstroMath.MAS*AstroMath.MAS;

	//  ===========================================================
	//			In-line utilities
	//  ===========================================================

	/* Transpose a 3x3 matrix */
	private static final void tr3(double[] m) {
		double v;
		v = m[1]; m[1] = m[3]; m[3] = v;
		v = m[2]; m[2] = m[6]; m[6] = v;
		v = m[5]; m[5] = m[7]; m[7] = v;
	}
	/* Compute the product R.u = v */
	private final void rot2local(double[] u, double[] v) {
		v[0] = R[0]*u[0] + R[1]*u[1] + R[2]*u[2];
		v[1] = R[3]*u[0] + R[4]*u[1] ;	
		v[2] = R[6]*u[0] + R[7]*u[1] + R[8]*u[2];
	}
	/* Compute the product tR.v = u */
	private final void rot2fixed(double[] v, double[] u) {
		u[0] = R[0]*v[0] + R[3]*v[1] + R[6]*v[2];
		u[1] = R[1]*v[0] + R[4]*v[1] + R[7]*v[2];
		u[2] = R[2]*v[0]             + R[8]*v[2];
	}

	/*---
    private final void rot3(double[] R, double[] v) {
      int i, m; double x,y,z;
        x = v[0]; y = v[1]; z = v[2];
	for(i=0, m=0; i<9; i+=3)
	     v[m++] = R[i]*x + R[i+1]*y + R[i+2]*z;
    }
    ---*/

	/** 
	 * Rotate the 6x6 covariance (symmetrical) matrix.
	 * If Y=RX, then var(Y)=R.var(X).tR.
	 * Element  =  Sum(k,l) Rik.Vkl.Rjl, which writes:
	 * <pre>
	 *  |R 0| |A  C| |R' 0| = |RAR'  RCR'|
	 *  |0 R| |C' B| |0 R'| = |RC'R' RBR'|
	 * </pre>
	 * @param  R 3x3 (9 elements) rotation matrix
	 * @param  V the 6x6 covariance matrix, symmetrically stored.
	 *         (21 elements).
	 * @param  W Resulting matrix = T . V . <sup>t</sup>T.
	 *         (may be at same location as V)
	 *
	 **/
	private final static void rotate_cov(double[] R, double[] V, double[] W) {
		if(V==null) return;
		double[] Vt = new double[9];
		int i,j,k,l,q;
		for(q=0; q<3; q++) {
			// Explore the sub-matries upper-left, upper-right, bottom-right
			// Copy elements of V into Vt
			int oq = q*9;	// Offset in sym6index
			for(i=0; i<9; i++) Vt[i] = V[sym6index[oq+i]];
			// Compute elements and place them into W
			boolean diag = (q&1)==0;	// Sub-matrix along diagonal
			int iw=0;
			for(i=0; i<9; i+=3) for(j=0; j<9; j+=3, iw++) {
				// Compute element(i,j) = Σ(kl) R(i,k).R(j,l).V(k,l)
				if(diag && (j<i)) continue;
				double w=0;
				int it=0;
				for(k=0;k<3;k++) for(l=0;l<3;l++)
					w += R[i+k]*R[j+l]*+Vt[it++];
				W[sym6index[oq+iw]] = w;
			}
		}
		//printMatrix("#...Combined matrix (bottom-left not filled):\n", W);
		//AstroMath.m36pt(R, V, W);
	}

	/** 
	 * Retrieve one specific variance/covariance (in local frame).
	 *           Sum(i).Sum(j) { R[i1,i] . R[i2,j] . cov[i,j]
	 * @param i1 Index of param.1 (1=RA, 2=Dec, 3=mu0, 4=mu1, 5=mu2)
	 * @param i2 Index of param.2 (1=RA, 2=Dec, 3=mu0, 4=mu1, 5=mu2)
	 * @return variance or covariance, in mas<sup>2</sup>.
	 *         The parameter <i>mu0</i> is the combination of parallax and radial velocity.
	 */
	public final double getVariance(int i1, int i2) {
		//if((ready&0x20)!=0) System.out.println("#...getVariance(" + i1 + "," + i2 + "): ready=0x" + Integer.toHexString(ready));
		if((ready==0) || (R == null))
			return(0./0.);
		if(cov == null) return(0);
		if((ready&0x10)==0) compute_loc();	// Need normalize
		int i=i1; int j=i2;
		double w=0;
		if(((ready&0x20)!=0)&&(i1>0)&&(i2>0)) {
			// Compute directly from sig5corr for α δ µ0 µα µδ
			// Compute var(µ0) = var(ϖ).var(V) + var(ϖ).V² + var(V).ϖ²
			// and   cov(µ0,x) = V.cov(ϖ,x)
			w = sig5corr[--i] * sig5corr[--j];
			if(i!=j) {	// covariance from correlation
				w *= sig5corr[AstroMath.symIndex(5,i,j)];
				if((i==2)||(j==2)) w *= rv/PMtoVEL;
			}
			else if(i==2) {	// var(µ0) from var(ϖ)
				double v = rv/PMtoVEL;
				double var_v = e_rv/PMtoVEL; var_v *= var_v;
				w = w*(var_v+v*v) + plx*plx*var_v;
			}
			if(DEBUG&&(i1==i2)&&(w<0)) 
				System.out.println("#+++Astropos.getVariance[loc](" + i1 + ","+ i2 + ") => " + w);
			return(w);	
		}
		if(i>j) { i=i2; j=i1; }
		int q=((i/3)<<1)|(j/3);	if(q>=2) q--;	// Quarter
		int oq = 9*q;
		i = 3*(i%3); j = 3*(j%3);
		for (int k=0; k<3; k++) for (int l=0; l<3; l++)
			w += R[i+k]*R[j+l]*cov[sym6index[oq++]];
		w *= MAS2;	// Express in mas²
		//if((d!=0)&&(d!=w)) System.out.println("#+++getVariance(" + i1 + "," + i2 + "): methods give "
		//        + d + "/" + w + ", Δ=" + (d-w));
		if(DEBUG&&(i1==i2)&&(w<0)) 
			System.out.println("#+++Astropos.getVariance[fix](" + i1 + ","+ i2 + ") => " + w);
		return(w);
	}

	/** 
	 * Compute the error propagation on a symmetric covariance matrix.
	 * <pre>
	 *	 | 1  0  0  t  0  0 |               | 1  0  0  0  0  0 |
	 *   | 0  1  0  0  t  0 |   | A  C |    | 0  1  0  0  0  0 |
	 *   | 0  0  1  0  0  t | * |      |  * | 0  0  1  0  0  0 |
	 *   | 0  0  0  1  0  0 |   | C' B |    | t  0  0  1  0  0 |
	 *   | 0  0  0  0  1  0 |               | 0  t  0  0  1  0 |
	 *   | 0  0  0  0  0  1 |               | 0  0  t  0  0  1 |
	 *
	 *     | A+t(C+C')+Bt²  C+Bt |
	 *   = |                     |
	 *     |   C'+Bt        B    |
	 * </pre>

	 */
	private static final void propagate_error(double var[], double t) {
		double[] check=null;
		if(DEBUG) check=AstroMath.mpt(AstroMath.motionMatrix(t), var);
		int o;
		double ht2 = 0.5*t*t;
		// C+Bt
		for(o=0; o<9; o++) var[sym6index[9+o]] += t*var[sym6index[18+o]];
		// A+t(C+C')+Bt² -- take care of diagonal term
		for(o=0; o<9; o++) {
			int i = sym6index[o];
			double v = t*var[sym6index[9+o]] - ht2*var[sym6index[18+o]];
			if(i<3) var[i] += 2.0*v;
			else    var[i] += v;
		}
		/* Verification
        if(DEBUG) {
            double eps = AstroMath.amax(var);
            System.out.println("#...propagate_error(t=" + t + "): diff. with direct computation=" 
                    + AstroMath.diffArray(var, check));
            if(AstroMath.diffArray(var, check)>eps) {
                AstroMath.checkArray("#***propagate_error problem(t=" + t + "):",
                        var, check, AstroMath.eps);
                AstroMath.printMatrix("#...Direct computation:\n", check);
                AstroMath.printMatrix("#.... here computation:\n", var);
            }
        } */
		/*
	for (j=0; j<3; j++) for (i=0; i<6; i++)
	    var[i][j] += t*var[i][j+3];
	for (i=0; i<3; i++) for (j=0; j<6; j++)
	    var[i][j] += t*var[i+3][j]; */
	}

	/* Compute Product Matrix(3x3) applied to 6-vector: v = R . u */
	private static final void rot6(double[] R, double[] u, double[] v) {	
		int i;
		for(i=0; i<3; i++) {
			int o = 3*i;
			v[i]   = R[o]*u[0] + R[o+1]*u[1] + R[o+2]*u[2];
			v[i+3] = R[o]*u[3] + R[o+1]*u[4] + R[o+2]*u[5];
		}
	}

	//  ===========================================================
	//			Error ellipses and Variances
	//  ===========================================================

	/**
	 * Renormalize the vectors: update parallax, mu's and variances
	 **/
	public double normalize() {
		double norm = x*x+y*y+z*z;
		if (norm <= 0) return (norm);
		if (norm == 1) return (norm);
		/* Covariance matrix: normalize with r² */
		if(DEBUG) System.out.print("#...calling normalize: r²=" + norm);
		if((specified&HASerr)!=0) for(int i=0; i<cov.length; i++)
			cov[i] /= norm;
		norm = Math.sqrt(norm);
		if(DEBUG) System.out.println(", r=" + norm);
		x  /= norm;  y /= norm;  z /= norm;
		xd /= norm; yd /= norm; zd /= norm;
		plx /= norm; e_plx /= norm;
		return(norm);
	}

	/**
	 * Rotate an Astropos. Its changes position, proper motions and errors
	 * @param R [3][3] matrix
    public final void rotate(double[] R) {
      double v[] = new double[3];
        super.rotate(R); 	// Rotate x, y, z
	// Rotate derivatives: 
        v[0] = xd; v[1] = yd; v[2] = zd;
	xd = R[0]*v[0] + R[1]*v[1] + R[2]*v[2];
	yd = R[3]*v[0] + R[4]*v[1] + R[5]*v[2];
	zd = R[6]*v[0] + R[7]*v[1] + R[8]*v[2];
	// Rotate the variance: R . var . tR 
	if (cov != null) 
	    rotate_cov(R, cov, cov);
    }
	 **/

	/**
	 * Convert a variance matrix <i>(var(x), var(y), cov(x,y))</i>
	 * into an error ellipse <i>(a, b, posAngle)</i>.
	 * The axises of the error ellipse are the squared eigenvalues of variance
	 * matrix, i.e. <br> 
	 *     (var(x)-a)(var(y)-a) = cov(x,y) <br>
	 *     (var(x)-b)(var(y)-b) = cov(x,y) <br>
	 * a<sup>2</sup> and b<sup>2</sup> are solutions of a simple 2nd order equation.
	 *     The <i>posAngle</i> defines the eigenvector of the largest eigenvalue
	 * @param var  Vector {var(x)  var(y)  cov(x,y) }
	 * @param ee3  Vector {  a       b     theta }   axises+PA of ellipse
	 **/
	public static final void varianceToErrorEllipse(double[] var, double[] ee3){
		/*--- changed in V2.0 
      	double delta, a2, b2, t;
      //System.out.println(Coo.toString("   var: ", var));
	t = (var[0]-var[1])/2.; delta = Math.sqrt(t*t+var[2]*var[2]);
	t = (var[0]+var[1])/2.; a2 = t+delta; b2 = t-delta;
	ee3[0] = Math.sqrt(a2);
	ee3[1] = Math.sqrt(b2);
	// Theta
	if (var[2] == 0) ee3[2] = var[0]<=var[1] ? 0: 90.;
	else ee3[2] = AstroMath.atand((a2-var[1])/var[2]);
	if (ee3[2] < 0)      ee3[2] += 180. ;
      //System.out.println(Coo.toString(" =>ell: ", ee3));
      ---*/
		double dvar = var[1]-var[0];  /* var(y)-var(x) */
		double cov2 = var[2]*var[2];
		if(cov2 < 1.e-12*var[0]*var[1]) { /* Correlation<1.e-6 */
			if(dvar>0) { ee3[0]=var[1]; ee3[1]=var[0]; ee3[2]= 0.; }
			else       { ee3[0]=var[0]; ee3[1]=var[1]; ee3[2]=90.; }
		}
		else {
			double d2ab = Math.sqrt(dvar*dvar + 4.*cov2);
			ee3[0] = 0.5*(var[0]+var[1]+d2ab);
			ee3[1] = 0.5*(var[0]+var[1]-d2ab);
			ee3[2] = 0.5*AstroMath.atan2d(2.*var[2], dvar);
			if(ee3[2]<0) ee3[2] += 180.;
		}
		/*-- Just for test...
        if((ee3[0]<0)||(ee3[1]<0)||Double.isNaN(var[0])||Double.isNaN(var[1])||Double.isNaN(var[2])) {
            AstroMath.printArray("#...BAD var: ", var); 
            AstroMath.printMatrix("; BAD ee3: ", ee3); 
        } ---*/
		ee3[0] = ee3[0]>0 ? Math.sqrt(ee3[0]):0;
		ee3[1] = ee3[1]>0 ? Math.sqrt(ee3[1]):0;
	}

	/**
	 * Convert an error ellipse <i>(a, b, posAngle)</i> into a 
	 * variance matrix <i>(var(x), var(y), cov(x,y))</i>.
	 * 	  X = sin(theta)*x - cos(theta)*y = st*x - ct*y<br>
	 * 	  Y = cos(theta)*x + sin(theta)*y = ct*x + st*y<br>
	 * the covariances of (X,Y) are:<br>
	 *     var(X) = st<sup>2</sup>*var(x) + ct<sup>2</sup>*var(y)<br>
	 *     var(Y) = ct<sup>2</sup>*var(x) + st<sup>2</sup>*var(y)<br>
	 *     cov(X,Y) = ct*st*(var(x)-var(y))
	 * @param ee3  Vector {  a       b     theta}   axises+PA of ellipse
	 * @param var  Vector {var(x)  var(y)  cov(x,y)}
	 * The relation
	 **/
	public static final void errorEllipseToVariance(double[] ee3, double[] var){
		double ct, st, a2, b2;
		//System.out.println(Coo.toString("   ell: ", ee3));
		a2 = ee3[0] * ee3[0] ; b2 = ee3[1] * ee3[1] ;
		ct = AstroMath.cosd(ee3[2]) ;   st = AstroMath.sind(ee3[2]) ;
		var[0] = a2*st*st + b2*ct*ct ;
		var[1] = a2*ct*ct + b2*st*st ;
		var[2] = ct*st*(a2-b2) ;
		//System.out.println(Coo.toString(" =>var: ", var));
	}

	/**
	 * Interpret an error ellipse written  
	 *     <b>[</b> <i>a b theta</i> <b>]</b>.
	 * If theta is missing, its value is defaulted to 90&deg; (i.e. 
	 * a and b represent the mean errors on RA*cos(Dec) and Dec).
	 * If a single number is found, it's assumed to be the error circle.
	 * @param txt  Text to interpret
	 * @param ee3  Vector (  a       b     theta)   axises+PA of ellipse
	 * @return true if error ellipse could be parsed.
	 **/
	public static boolean parseErrorEllipse(Parsing txt, double[] ee3) {
		ee3[0] = ee3[1] = 0./0.;
		ee3[2] = 90.;	// Default theta
		boolean bracketted = txt.match('[');
		txt.gobbleSpaces();
		int n = txt.parseArray(ee3);
		if(bracketted) { 
			txt.gobbleSpaces(); 
			if(!txt.match(']')) return(false);
		}
		if (n==0) return(false);	// Not a single number found! 
		if (Double.isNaN(ee3[2])) ee3[2] = 90.;		// Default theta
		if (Double.isNaN(ee3[1])) ee3[1] = ee3[0];	// Default b=a
		return(true);
	}

	/**
	 * Interpret an array of (bracketed or not) set of values,
	 * eventually separated by commas.
	 *     <b>(</b> <i>a<sub>0</sub>, a<sub>1</sub>,...</i> <b>)</b>.
	 * @param txt  Text to interpret
	 * @param vec  Vector to complete
	 * @return actual number of values interpreted
    public static int parseBracketedArray(Parsing txt, double[] vec) {
        int posini = txt.pos;
        txt.gobbleSpaces();
        boolean bracketed = txt.match('(');
        int n = txt.parseArray(vec);
        if(n<vec.length) {
            char sep = txt.currentChar();
            if(sep==',') {
                txt.set(posini);
                n = txt.parseArray(vec, sep);
            }
        }
        if(bracketed) {
	    txt.gobbleSpaces();
            if(!txt.match(')')) {
                txt.set(posini);
                return(0);
            }
        }
        for(int i=n; i<vec.length; i++) vec[i] = 0./0.;
	return(n);
    }
	 **/

	/*===========================================================
   			Compute all elements
	 *===========================================================*/

	/**
	 * Compute all elements in the fixed frame (x,y,z), and mark it as 'ready'.
	 * On input, the various elements set by the various <tt>set</tt>... methods.
	 * On output, the values in the fixed frame are ready.
	 */
	private final void compute_fix() {
		if((ready&1)!=0) return;
		// Be sure R exists -- in principle yes, but...
		if(R == null) R = new double[9];
		int i,j;

		if(Double.isNaN(epoch))   epoch = frame.epoch;
		if(Double.isNaN(meanEp)) meanEp = epoch;
		if(sig5corr==null) sig5corr = new double[15];
		if(DEBUG) dump("<<compute_fix: ");

		// Get parallax . If specified, plx is not null; refuse negative parallax.
		if ((specified&HASplx) == 0) {	// Parallax not specified
			this.plx   = 0.;	// def_plx[0];
			this.e_plx = 0.;	// def_plx[1];
			estimated |= HASplx|HASeplx;
		}
		else if(this.plx<0) this.plx=0;
		if (Double.isNaN(this.e_plx)) {	// Added V1.2
			this.e_plx = 0;
			estimated |= HASeplx;
			// Minimal assumed error of 1mas
			// if (this.e_plx < 1.) this.e_plx = 1.;
		}

		// Get Radial Velocity. If specified, RV is not null
		if ((specified&HASrv) == 0) {	// Radial Velocity not specified
			this.rv   = 0; 	// def_rv[0];
			this.e_rv = 0; 	// def_rv[1];
			estimated |= HASrv|HAServ;
		}

		// When RV + plx supplied, compute mu0 = (plx*rv)/4.74
		if((specified&(HASplx|HASrv)) == (HASplx|HASrv)) {
			mu0 = plx*rv/PMtoVEL;	// in mas/yr
			if(DEBUG) System.out.println("#...compute_fix: µ0=" + mu0 + ", µ1=" + mu1 + ", µ2=" + mu2);
		}
		else mu0 = 0;

		// Compute xd yd zd
		if((specified&HASpm)!=0) {
			xd = (R[0]*mu0 + R[3]*mu1 + R[6]*mu2)/AstroMath.MAS;
			yd = (R[1]*mu0 + R[4]*mu1 + R[7]*mu2)/AstroMath.MAS;
			zd = (R[2]*mu0 + R[5]*mu1 + R[8]*mu2)/AstroMath.MAS;
		}

		// Error ellipse on position, compute or estimate when not given
		if(((specified&HASepos)==0) && (dlon>0) && (dlat>0)) {
			// Estimate error ellipse on {lon.cos(lat), lat}
			// (cos(lat) is also R[8]
			i = dlon; if (i>=def_err.length) i = def_err.length-1;
			j = dlat; if (j>=def_err.length) j = def_err.length-1;
			estimated |= HASepos;
			this.setErrorEllipse(def_err[i]*R[8], def_err[j], 90., 0./0.);
		}

		// Install the covariance matrix, from sig5corr.
		// +-------------------+
		// | .  α  δ  µ0 µα µδ | local (sig5corr)
		// +-------------------+
		// |    0  5   6  7  8 |
		// |       1   9 10 11 |
		// |           2 12 13 |
		// |              4 14 |
		// |                 5 |
		// +-------------------+
		// | x  y  z  xd yd zd | fixed (cov)
		// +-------------------+
		// | 0  6  7   8  9 10 |
		// |    1 11  12 13 14 |
		// |       2  15 16 17 |
		// |           3 18 19 |
		// |              4 20 |
		// |                 5 |
		// +-------------------+
		if((specified&HASerr)!=0) {
			if(DEBUG) AstroMath.printMatrix("#   sig5cov(local), specified=0x" + Integer.toHexString(specified) 
			+ ", estimated=" + Integer.toHexString(estimated) + "\n", this.sig5corr);
			// Complete the sig5corr symmetric matrix, in mas.
			sig5corr[2] = Double.isNaN(e_plx) ? 0 : e_plx;
			// Install the "fix" covariance matrix
			if(cov==null) cov = new double[21];
			cov[0] = cov[6] = cov[7] = cov[8] = cov[9] = cov[10] = 0;
			int m=5;
			for(i=0; i<5; i++) {
				cov[i+1] = sig5corr[i]*sig5corr[i]/MAS2;	// variances, in rad²
				for(j=i+1; j<5; j++, m++) cov[6+m] = 		// Covariances, in rad²
						sig5corr[m]*sig5corr[i]*sig5corr[j]/MAS2;
			}
			if(DEBUG) System.out.print("#...Compute correlations: ϖ=" + plx + "+/-" + e_plx + ", Vr=" + rv + "+/-" + e_rv);
			// Compute var(µ0) = var(ϖ).var(V) + var(ϖ).V² + var(V).ϖ²
			// and   cov(µ0,x) = V.cov(ϖ,x)
			double v   = rv/PMtoVEL;		// rv in mas/yr
			double e_v = Double.isNaN(e_rv) ? 0 : e_rv/PMtoVEL;
			double var_p = sig5corr[2]*sig5corr[2];
			double var_v = e_v*e_v;
			cov[3] = (var_p*var_v + var_p*v*v + var_v*plx*plx)/MAS2;
			if(DEBUG) System.out.print(", σ(µ0)=" + Math.sqrt(cov[3]));
			if((specified&HAScorr)!=0) {
				cov[12] *= v;	// cov. α,µ0
				cov[15] *= v;	// cov. δ,µ0
				cov[18] *= v;	// cov.µα,µ0
				cov[19] *= v;	// cov.µδ,µ0
			}
			// There is a problem when v[0] is too large compared to 
			// other components; in this case, set this parameter to
			// zero, but increase its error.
			/*--- TODO: check this
	    double musq = xd*xd + yd*yd + zd*zd;	// Total mu^2, in rad/Jyr
	    if (mu0*mu0 >= 0.5*musq) {
		if (cov != null) cov[3] += mu0*mu0;
	        mu0 = 0;
	    }
	    else {	// Assign the radial component to velocity
		xd += x*mu0;
		yd += y*mu0;
		zd += z*mu0;
	    }
            --*/
			if(DEBUG) AstroMath.printMatrix("#   cov6(0), rad\n", this.cov);
			// Progapate the error on proper motions 
			if((this.epoch != this.meanEp) && ((specified&(HASepos|HASmep))==(HASepos|HASmep))) {
				propagate_error(cov, this.epoch - this.meanEp);
				if(DEBUG) AstroMath.printMatrix("#   cov6(t), rad\n", this.cov);
			}
			// 6-D covariance is now correcly set in the local frame.
			// Rotate the 6-D covariance matrix to absolute frame, i.e.
			// with inverse(R) = transposed(R).
			tr3(R);	// transpose R
			rotate_cov(this.R, cov, cov);
			tr3(R);	// transpose again, i.e. return to original
			if(DEBUG) AstroMath.printMatrix("#   cov6(1), after rotate_cov:\n", this.cov);
			//for(m=0; m<cov.length; m++) { if(Double.isNaN(cov[m])) { AstroMath.printMatrix("#...NaN in cov! ", cov); break; }}
		}
		ready = 1;
		if(DEBUG) dump(" >compute_fix: ");
	}

	/**
	 * Compute the proper motions (local frame).
	 */
	private final void compute_loc() {
		if((ready&0x10)!=0) return;
		if(ready==0) compute_fix();
		if(DEBUG) System.out.println("#...compute_loc: mu was " + mu0 + ", " + mu1 + ", " + mu2);
		normalize();
		// Compute proper motions mu0, mu1, mu2
		mu0 = AstroMath.MAS*(R[0]*xd + R[1]*yd + R[2]*zd);
		mu1 = AstroMath.MAS*(R[3]*xd + R[4]*yd + R[5]*zd);
		mu2 = AstroMath.MAS*(R[6]*xd + R[7]*yd + R[8]*zd);
		ready |= 0x10;
		if(DEBUG) {
			System.out.println("#...compute_loc: mu now " + mu0 + ", " + mu1 + ", " + mu2);
			AstroMath.printMatrix("#...compute_loc: cov is:\n", cov);
		}
	}

	/**
	 * Compute the sigma5 elements.
	 */
	private final void compute_cor() {
		if((ready&0x20)!=0) return;
		if((ready&0x10)==0) compute_loc();
		// Compute sigma's and correlations in local frame
		if(sig5corr==null) sig5corr = new double[15];
		if((specified&HAScorr)==0) estimated |= HAScorr;
		if(DEBUG) {
			AstroMath.printMatrix("#...compute_cor:  cov=\n", cov);
			AstroMath.printMatrix("#...original sig5corr:\n", sig5corr);
		}
		// Compute first the Sigmas
		sig5corr[0] = Math.sqrt(getVariance(1,1));  // σ(α)
		sig5corr[1] = Math.sqrt(getVariance(2,2));  // σ(δ)
		sig5corr[2] = e_plx;                        // σ(ϖ)
		sig5corr[3] = Math.sqrt(getVariance(4,4));  // σ(µα)
		sig5corr[4] = Math.sqrt(getVariance(5,5));  // σ(µδ)
		// Correlations with plx (6, 9, 12, 13) do not change
		double sij;
		sij = sig5corr[0]*sig5corr[1]; sig5corr[5]  = sij>0 ? getVariance(1,2)/sij : 0;
		sij = sig5corr[0]*sig5corr[3]; sig5corr[7]  = sij>0 ? getVariance(1,4)/sij : 0;
		sij = sig5corr[0]*sig5corr[4]; sig5corr[8]  = sij>0 ? getVariance(1,5)/sij : 0;
		sij = sig5corr[1]*sig5corr[3]; sig5corr[10] = sij>0 ? getVariance(2,4)/sij : 0;
		sij = sig5corr[1]*sig5corr[4]; sig5corr[11] = sij>0 ? getVariance(2,5)/sij : 0;
		sij = sig5corr[3]*sig5corr[4]; sig5corr[14] = sij>0 ? getVariance(4,5)/sij : 0;
		if(DEBUG) AstroMath.printMatrix("#........now sig5corr:\n", sig5corr);
		ready |= 0x30;
	}

	/*===========================================================
   			Constructors
	 *===========================================================*/

	/**
	 * Create the default (empty) Astropos (ICRS)
	 * (better to avoid?)
	 */
	public Astropos() {
		this(ICRS.create(2000.0));
	}

	/**
	 * Create an empty Astropos.
	 * @param frame	in which frame
	 */
	public Astropos(Astroframe frame) {
		super(frame);
		cov = null;
		permanent = 0;
		reset();
	}

	/**
	 * Create a Astropos object with just longitude + latitude
	 * @param frame	the frame to which coordinates refer
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 */
	public Astropos(Astroframe frame, double lon, double lat) {
		super(frame);
		cov = null;
		permanent = 0;
		reset();
		this.set(lon, lat);
	}

	/** (may be problematic)
	 * Create a Astropos object: coordinates + proper motion.
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param epoch	epoch of the longitude/latitude, year
	 * @param mu1	proper motion along longitude, mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
     public Astropos(Astroframe frame, double lon, double lat, double epoch, 
		   double mu1, double mu2) {
	super(frame);
	this.set(lon, lat, epoch, (double[])null, 
		 mu1, mu2, epoch, (double[])null, 
		(double[])null, (double[])null);
     }
	 */

	/**
	 * Create a Astropos object from an existing Astrocoo
	 * @param coo	Astrocoo object
	 */
	public Astropos(Astrocoo coo) {
		this(coo.frame);
		// Copy the data already known:
		if((coo.x * coo.x + coo.y * coo.y + coo.z * coo.z) > 0) {
			this.x = coo.x; this.y = coo.y; this.z = coo.z;
			specified |= HASpos;
			// FXP: Add the same elements as in `set(double lon, double lat)`
			if(R == null) R = super.localMatrix();
			else super.localMatrix(R);
			specified = HASpos; 
			estimated = 0;
			ready = 0;
		}
		this.precision = coo.precision;
		this.editing   = coo.editing  ;
		this.dlon      = coo.dlon     ;
		this.dlat      = coo.dlat     ;
		if(!Double.isNaN(coo.epoch))
			this.setEpoch(coo.epoch);
		// super(coo.getFrame());
		// this.set(coo.getLon(), coo.getLat(), coo.epoch, null,
		//      0./0., 0./0., 0./0., null, null, null);
	}

	/**
	 * Create a Astropos object from an existing Astrocoo
	 * @param coo	Astrocoo object
	 * @param eepos	Error ellipse on position (mas, mas, deg)
	 * @param meanEp	epoch of the errors on position (minimal error)
	 * @param mu1	proper motion along longitude, mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
	 * @param eepm	Error ellipse on proper motion (mas/yr, mas/yr, deg)
	 * @param plx2	Parallax and its mean error (mas)
	 * @param rv2	Radial velocity and its error (km/s)
	 * ------ Not necessary
     public Astropos(Astrocoo coo, double[] eepos, double meanEp,
	     double mu1, double mu2, double[] eepm, 
	     double[] plx2, double[] rv2) {
	super(coo.getFrame());
	this.set(coo.getLon(), coo.getLat(), coo.epoch, eepos,
	     mu1, mu2, meanEp, eepm, plx2, rv2);
     }
	 */

	/**
	 * Create an Astropos object from a position (Epoch)
	 * @param frame	one of the possible Astroframes
	 * @param text Text with position, possibly followed by an epoch
	 * @throws ParseException when the input text cannot be fully interpreted
	 *         for a position and an eventual epoch.
	 */
	public Astropos(Astroframe frame, String text) throws ParseException {
		super(frame);
		this.set(text);
	}

	/**
	 * Create an Astropos object from just a string.
	 * @param text Text with frame, position, possibly followed by an epoch
	 * @throws ParseException when the input text cannot be fully interpreted
	 *         for a frame specification, a position and an eventual epoch.
	 */
	public Astropos(String text) throws ParseException {
		if(DEBUG) System.out.println("#...Astropos <" + text + ">");
		frame=null;
		Parsing txt = new Parsing(text);
		txt.gobbleSpaces();
		boolean matched=false;
		// Could be IAU position ?
		char c = txt.currentChar(); txt.advance(1);
		if(Character.isLetter(c)&&Character.isDigit(txt.currentChar())) {
			while(txt.advance(1)) {
				c = txt.currentChar();
				if(Character.isLetterOrDigit(c) || (c=='.')) continue;
				break;
			}
			if(Parsing.isSign(c)) {
				txt.set(0);
				matched = super.parseIAU(txt);
			}
		}
		if(!matched) { txt.set(0); frame = Astroframe.parse(txt); }
		if (frame == null) throw new ParseException
		("[Astropos] argument '" + text + "' (no frame)", txt.pos);
		//System.out.println("#...Astropos befor setEditing: <" + txt + ">; dlon=" + dlon + ", dlat=" + dlat);
		super.setEditing();	// Added V1.22
		//System.out.println("#...Astropos after setEditing: <" + txt + ">; dlon=" + dlon + ", dlat=" + dlat);
		if(DEBUG) System.out.println("#...Astropos <" + text + ">: interpreted frame=" + this.frame + "; now: " + txt);
		matched = parse(txt);		// Various items found.
		txt.gobbleSpaces();
		if((!matched) || (txt.pos != txt.length)) {
			String msg = "[Astropos, frame=" + frame + "] argument '" + text + "'";
			if(txt.error_message!=null) msg = msg + " " + txt.error_message;
			throw new ParseException(msg + "+" + txt.pos, txt.pos);
		}
	}

	public Object clone() {
		Astropos c = (Astropos) super.clone();
		if(this.R!=null)   c.R = this.R.clone();
		if(this.cov!=null) c.cov = this.cov.clone();
		if(this.sig5corr!=null) c.sig5corr = this.sig5corr.clone();
		return c;
	}


	/**
	 * Create an Astropos from position + proper motion.
	 * @param frame	one of the possible Astroframes
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param eepos	error ellipse, mas
	 * @param epoch	epoch of the longitude/latitude, Julian year
	 * @param mu1	proper motion along longitude,  mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
	 * @param eepm	error ellipse of proper motion, mas/yr
	 * @param meanEp	epoch of position error (minimal error), Jyr
	 * @param plx2	parallax + its error (mas)
	 * @param rv2	radial velocity + its error (km/s)
	 */
	public Astropos(Astroframe frame, 
			double lon, double lat, double epoch, double[] eepos, 
			double mu1, double mu2, double meanEp, double[] eepm, 
			double[] plx2, double[] rv2) {
		super(frame);
		this.set(lon, lat, epoch, eepos, 
				mu1, mu2, meanEp, eepm, plx2, rv2);
	}

	//  ===========================================================
	//			Dump (print object's contents)
	//  ===========================================================

	/**
	 * Dump the contents of an Astropos
	 * @param title title line.
	 *        When the title contains <code>(R)</code>, dump is recursive.
	 **/
	public void dump(String title) {
		String[] xplain_ready = { "NOT_ready", "fixed_ok", "mu's_ok", "full_ok" };
		int state=ready&1; if((ready&0x10)!=0) state++; if((ready&0x20)!=0) state++;
		System.out.print("#...Astropos.dump (" + xplain_ready[state] + ") " + title);
		if(!title.endsWith("\n")) System.out.print("\n");
		boolean recursive = title.indexOf("(R)")>=0;
		String indent = title.charAt(0) == '#' ? "#   " : "    ";
		System.out.println(indent + "epoch=" + epoch + "; Astroframe=" + this.frame);
		System.out.println(indent + "x=" + x + " y=" + y + " z=" + z
				+ "\t; lon=" + lon + " lat=" + lat);
		System.out.println(indent + "xd=" + xd + " yd=" + yd + " zd=" + zd + "; meanEp=J" + meanEp);
		System.out.println(indent + "precision=" + precision + "; editing=0x" + Integer.toHexString(editing) + '(' + explainEditing(editing) + ')');
		System.out.println(indent + "errorprec=" + errorprec + "; inputprec: dlon=" + dlon + ", dlat=" + dlat + ", formRA=0x" + Integer.toHexString(formRA)
		+ '(' + Astroformat.explain(formRA) + ')');
		System.out.println(indent + "mu0=" + mu0 + " mu1=" + mu1 + " mu2=" + mu2 + " (mas/yr)" + ((ready&0x10)==0 ? " [NOT_COMPUTED]" : ""));
		System.out.println(indent + "plx=" + plx + "[" + e_plx + "]; rv=" + rv + "[" + e_rv + "]");
		System.out.print(indent + "specified=0x" + Integer.toHexString(specified));
		System.out.print('(' + explain(specified, partnames) + ')');
		System.out.print(         " estimated=0x" + Integer.toHexString(estimated));
		System.out.print('(' + explain(estimated, partnames) + ')');
		System.out.print(         " permanent=0x" + Integer.toHexString(permanent));
		System.out.print('(' + explain(permanent, partnames) + ')');
		System.out.print(         " ready=0x" + Integer.toHexString(ready));
		System.out.println('[' + xplain_ready[state] + ']');
		AstroMath.printMatrix(indent + "R:", R);
		AstroMath.printMatrix(indent + "cov (6x6 covariance matrix) in fixed frame" + ((ready&0x01)==0 ? " [NOT_COMPUTED]" : "") + ":\n", cov);
		AstroMath.printMatrix(indent + "sig5corr (sigma + correlations)" + ((ready&0x20)==0 ? " [NOT_COMPUTED]" : "") + ":\n", sig5corr);
		if(recursive) frame.dump(indent + "Details of frame: ");
		//ed.editDecimal(b, xd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
		//ed.editDecimal(b, yd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
		//ed.editDecimal(b, zd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
		//b.append("(dot)");
		//b.append('('); ed.editFlags(b, permanent, partnames); 
		//b.append(')');
		// Edit the  Pm Vr plx
	}

	/**
	 * Compare 2 astropos contents.
	 * @param title title line.
	 * @param ref the Astropos to compare with.
	 **/
	public void printDifferences(String title, Astropos ref) {
		System.out.println("#...Astropos.printDifferences: " + title);
		if(!this.frame.equals(ref.frame)) System.out.println("#   Frames: " + this.frame + " != " + ref.frame);
		if((this.dlon!=ref.dlon)||(this.dlat!=ref.dlat)||(this.precision!=ref.precision)||(this.editing!=ref.editing)) {
			System.out.print("#     ....:");
			if(this.dlon!=ref.dlon) System.out.print(" dlon=" + this.dlon + "/" + ref.dlon);
			if(this.dlat!=ref.dlat) System.out.print(" dlat=" + this.dlat + "/" + ref.dlat);
			if(this.precision!=ref.precision) System.out.print(" precision=" + this.precision + "/" + ref.precision);
			if(this.editing!=ref.editing) System.out.print(" editing=" + this.editing + "/" + ref.editing);
			System.out.println("");
		}
		if((this.specified!=ref.specified)||(this.permanent!=ref.permanent)||(this.estimated!=ref.estimated)||(this.ready!=ref.ready)) {
			System.out.print("#     ....:");
			if(this.specified!=ref.specified) System.out.print(" specified=" + this.specified + "/" + ref.specified);
			if(this.permanent!=ref.permanent) System.out.print(" permanent=" + this.permanent + "/" + ref.permanent);
			if(this.estimated!=ref.estimated) System.out.print(" estimated=" + this.estimated + "/" + ref.estimated);
			if(this.ready!=ref.ready) System.out.print(" ready=0x" + Integer.toHexString(this.ready) + "/" + Integer.toHexString(ref.ready));
			System.out.println("");
		}
		if(this.epoch!=ref.epoch)         System.out.println("#   Epochs: " + this.epoch + " != " + ref.epoch);
		if(this.meanEp!=ref.meanEp)       System.out.println("#  meanEps: " + this.meanEp + " != " + ref.meanEp);
		if((this.plx!=ref.plx)||(this.e_plx!=ref.e_plx)) System.out.println("#     plxs: " + this.plx + ":Δ=" + (this.plx-ref.plx)
				+ " σ=" + this.e_plx + ":Δ=" + (this.e_plx-ref.e_plx));
		if((this.rv!=ref.rv)||(this.e_rv!=ref.e_rv)) System.out.println("#      rv: " + this.rv + ":Δ=" + (this.rv-ref.rv)
				+ " σ=" + this.e_rv + ":Δ=" + (this.e_rv-ref.e_rv));
		if((this.x!=ref.x)||(this.y!=ref.y)||(this.z!=ref.z)) { double[] u1={x,y,z}; double[] u0={ref.x,ref.y,ref.z}; 
		AstroMath.checkArray("#    x,y,z: ", u1, u0, AstroMath.eps); }
		if((this.xd!=ref.xd)||(this.yd!=ref.yd)||(this.zd!=ref.zd)) { 
			double[] u1={xd,yd,zd}; double[] u0={ref.xd,ref.yd,ref.zd}; 
			AstroMath.checkArray("#  d.x,y,z: ", u1, u0, AstroMath.eps*AstroMath.amax(u0)); 
		}
		if((this.R!=null)&&(ref.R!=null)&&(!this.R.equals(ref.R))) 
			AstroMath.checkArray("#   Rmatrix:", this.R, ref.R, AstroMath.eps);
		if((this.cov!=null)&&(ref.cov!=null)&&(!this.cov.equals(ref.cov))) {
			if(!AstroMath.checkArray("#   covar..:", this.cov, ref.cov, 36*AstroMath.eps*AstroMath.amax(this.cov))) {
				AstroMath.printMatrix("#   Covariances(this):\n", this.cov);
				AstroMath.printMatrix("#   Covariances(ref):\n", ref.cov);
			}
		}
		if(((this.ready&ref.ready&0x31)==0x31)&&(this.sig5corr!=null)&&(ref.sig5corr!=null)) {
			AstroMath.checkArray("#   sig5cor:", this.sig5corr, ref.sig5corr, 36*AstroMath.eps*AstroMath.amax(this.sig5corr));
			AstroMath.printMatrix("#   σ+corr(this):\n", this.sig5corr);
			AstroMath.printMatrix("#   σ+corr(ref):\n", ref.sig5corr);
		}
	}

	//  ===========================================================
	//			Interpretation of text (Parsing)
	//  ===========================================================


	/**
	 * Interpret the string and convert to Coo + Epoch.
	 * Called from set and parse routines. Interprets until mismatch.
	 * @param txt to interpret as a Parsing object e.g. <ul>
	 * <li> <tt>"12 34 56.789 -72 54 21.19 [30 20 65 (J1991.25)] J2000.0 +125.2 -35.2 [3.9 2.5 45] 123[4]km/s 10[5]"</tt>
	 * <li> <tt>"12 34 56.789 -72 54 21.19 [30 20 65 (J1991.25)] J2000.0 pm=130.1/105.7[3.9 2.5 45] RV=123[4]km/s plx=10[5]mas"</tt>
	 * <li> <tt>"12 34 56.789 -72 54 21.19 (J2000.0) pm=130.1/105.7 RV=123[4]km/s plx=10 sigma=(28.5,22.1,5,3.3,3.3) corr=(0.006,0,0,0,0,0,0,0,0,0.065)"</tt>
	 * </ul>
	 * The order for sigma's and correlation is the same as Gaia2, i.e. (RA, Dec, Plx, pmRA, pmDec) and 
	 * the 10 correlations are (RA:Dec, RA:Plx, RA:pmRA, RA:pmDec, Dec:Plx, Dec:pmRA, Dec:pmDec, Plx:pmRA, Plx:pmDec, pmRA:pmDec )
	 * @return true if OK.
	 **/
	public boolean parse(Parsing txt) {
		double[] v3 = new double[3];
		double[] v2 = new double[2];
		double[] eepos=null;	// Error ellipse on position
		double[] eepm=null;	// Error ellipse on proper motion
		double[] plx=null;	// Parallax + Error
		double[] rv=null;	// Radial Velocity + Error
		double[] sigma=null;	// All 5 sigmas: RA, Dec, Plx, pmRA, pmDec
		double[] corr=null;	// All 10 correlations: RA:Dec, RA:Plx, ... RA:pmDec, Dec:Plx, ... Dec:pmDec, Plx:pmRA... pmRA:pmDec.
		double[] epochs = { epoch, meanEp };	// Locally specified epochs
		int  specif = 0;	// locally defined specifications
		double mu1, mu2, val;	// proper motions, temporary value
		int n, u;		// number of elements in arrays, specified unit.
		boolean ok = true;
		boolean bracketed;
		int posini = txt.pos;
		if(DEBUG) System.out.println("#...Parsing(0): calling Astropos.parse: " + txt);

		//reset(); No, the RA/Dec may already be parsed !
		// Use parent class to match the Coo + Epoch, but take care to
		// save the current epoch (could be permanent).
		// Astrocoo.parse sets epoch to NaN if not explicitely specified.
		val = epoch;
		if((dlon>0)&&(dlat>0)) ok=true;	// already parsed
		else ok = super.parse(txt); 
		epochs[0]=epoch; epoch=val;
		if(DEBUG) System.out.println("#...Parsing(1): ok=" + ok + " x=" + x + " y=" + y + " z=" + z + "; " + txt);
		if(!ok) return(false);
		specif = HASpos;
		if(!Double.isNaN(epochs[0])) specif |= HASep;

		// Try to match the error ellipse, which may include meanEp
		txt.gobbleSpaces();
		if (txt.match('[')) {
			txt.gobbleSpaces();
			if(!parseErrorEllipse(txt, v3))
				return(false);
			eepos = v3.clone(); //dup(v3);
			// The error ellipse may include mean epoch;
			epochs[1] = getEpoch(txt);
			if(!Double.isNaN(epochs[1])) specif |= HASmep;
			txt.gobbleSpaces();
			if(txt.match(']')) specif |= HASepos;
			else return(false); 
			// Could be followed by a unit (default is mas)
			if((u = txt.lookup(unitsymb))>=0) {
				if(DEBUG) System.out.println("#.............. matched unit <" + unitsymb[u] + "> *" + unit2mas[u]);
				eepos[0] *= unit2mas[u];
				eepos[1] *= unit2mas[u];
			}
		}
		if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]
				+ "\n#...Parsing(2): " + txt);

		// The epoch, or mean epoch
		while((specif&(HASep|HASmep))!=(HASep|HASmep)) {
			txt.gobbleSpaces();
			val = getEpoch(txt);
			if(Double.isNaN(val)) break;
			if((specif&HASep)==0) { epochs[0]=val; specif|=HASep;  }
			else                  { epochs[1]=val; specif|=HASmep; }
		}
		if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]
				+ "\n#...Parsing(3): " + txt);

		mu1 = mu2 = 0./0.;
		while(ok && (txt.pos<txt.length)) {
			posini = txt.pos;
			txt.gobbleSpaces();
			int itype = txt.lookup(datasymb);
			if(itype<0)	{	// anonymous component: take first among non-specified element.
				if(txt.pos>=txt.length) continue;
				for(itype=0; (itype<datatype.length)&&((specif&datatype[itype])!=0); itype++) ;
				if(itype>=datatype.length) break;	// All elements already found.
			}
			int type=datatype[itype]; 
			if(DEBUG) System.out.println("#...Astropos.parse <" + txt + ">, ok=" + ok 
					+ ", type=0x" + Integer.toHexString(type) + "=" + explain(type, partnames));
			switch(type) {
			case HASpm:	// proper motion, may be specified as µα,µδ or µ/θ
				boolean pm_pa   = false;
				boolean pm_unit = false;
				int ket = 0;	// Position of matching bracket (>0 if exists)
				if(DEBUG) System.out.println("#...Trying to match µ from: " + txt);
				n = txt.parseArray(v2);
				if(DEBUG) System.out.println("#...returned number of values n=" + n);
				if(n<0) {	// Mismatched bracket ?
					ket = txt.matchingBracket();
					if((ket>0) && (txt.indexOf('/')<ket)) {
						txt.advance(1);	// skip (
						n = txt.parseArray(v2);	// if (µ/pa) => n=1
					}
					else ket=0;	// mismatched bracket not solved...
				}
				if(n==1) {	// We could have a separator: comma (,) or slash(/)
					txt.gobbleSpaces();
					char sep = txt.currentChar();
					pm_pa = sep == '/';
					if(pm_pa || (sep==',')) {
						txt.advance(1);
						v2[1] = txt.parseDouble();
						if(!Double.isNaN(v2[1])) n++;
					}
				}
				if(n==2) {
					mu1 = pm_pa ? v2[0]*AstroMath.sind(v2[1]) : v2[0]; 
					mu2 = pm_pa ? v2[0]*AstroMath.cosd(v2[1]) : v2[1];
					txt.gobbleSpaces();
				}
				else {
					mu1 = mu2 = 0./0.;
					txt.setError("[Astropos.parse] single proper motion component");
					ok = false;
					continue;	// Will stop...
				}
				// Last chance for mean epoch
				if(DEBUG) System.out.println("#...parsing µ: mu1=" + mu1 + ", mu2=" + mu2);
				if((specif&HASmep)==0) {
					epochs[1] = getEpoch(txt);
					if(!Double.isNaN(epochs[1])) specif |= HASmep;
				}
				// if(DEBUG) System.out.println("#...Astropos.parse_pm, ket=" + ket + ", after_mu: " + txt);
				if(ket>0) {	// be sure the bracket is closed
					if(txt.lookup(Parsing.brackets)>0) txt.set(ket);
					else { 
						txt.setError("[Astropos.parse] non-closed brackets");
						ok=false; 
					}
				}
				if(!ok) continue;
				// proper motion could be followed by units
				if((u = txt.lookup(unitsymb))>=0) {
					pm_unit = true;
					mu1 *= unit2mas[u];
					mu2 *= unit2mas[u];
					txt.match("/yr");
				}
				if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]
						+ "\n#...Parsing(4): " + txt);
				// Error ellipse of proper motion
				txt.gobbleSpaces();
				posini = txt.pos;
				if ((bracketed = txt.match('['))) {
					// Matching the proper motion error ellipse
					txt.gobbleSpaces();
					if (parseErrorEllipse(txt, v3))
						eepm = v3.clone(); //dup(v3);
					if (txt.match(']')) specif |= HASepm;
					else ok = false;
					if((u = txt.lookup(unitsymb))>=0) {
						eepm[0] *= unit2mas[u];
						eepm[1] *= unit2mas[u];
						// The unit may concern also the pm values.
						if(!pm_unit) { mu1 *= unit2mas[u]; mu2 *= unit2mas[u]; }
					}
					// if(DEBUG) AstroMath.printMatrix("#...Astropos.parse_pm: error ellipse = ", eepm);
				}
				// Very last trial for mean epoch of error on position
				if((specif&HASmep)==0) {
					epochs[1] = getEpoch(txt);
					if(!Double.isNaN(epochs[1])) specif |= HASmep;
				}
				if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]
						+ "\n#...Parsing(5): " + txt);
				break;
			case HASplx:	// Parallax
			case HASrv:	// Radial Velocity
				n = txt.parseWithError(v2);	// May return 0, 1, 2
				if (n>0) {
					txt.gobbleSpaces();
					if((u = txt.lookup(unitsymb))>=0) {
						if(DEBUG) System.out.println("#...Parsing(v): u=" + u + ": " + unitsymb[u]);
						v2[0] *= unit2mas[u];
						v2[1] *= unit2mas[u];
						if((unitsymb[u].indexOf("m/s")>=0) && (type==HASplx)) {
							// Mixed up plx and RV
							if(DEBUG) System.out.println("#...Parsing(v): mixed rv/plx => " + type);
							type = HASrv;
						}
						else if((unitsymb[u].indexOf("m/s")<0) && (type==HASrv)) {
							if(DEBUG) System.out.println("#...Parsing(v): mixed rv/plx => " + type);
							type = HASplx;
						}
					}
					if(type==HASplx) plx = v2.clone();
					else              rv = v2.clone();
				}
				if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]
						+ "\n#...Parsing(pv), " + (rv==null?"rv=null " : "") + (plx==null?"plx=null ":"") + txt);
				break;
			case  HASerr:	// σ
				sigma = new double[5];
				n = txt.parseArray(sigma);
				if(n!=5) {
					txt.setError("[Astropos.parse]: " + n + "/5 sigmas");
					//System.err.println("#+++Astropos.parse: " + n + "/5 sigmas in: " + txt);
					while(n<5) sigma[n++]=0;
					ok = false;
				}
				if(DEBUG) { 
					System.out.println("#...Parsing(σ): " + txt + " => n=" + n);
					AstroMath.printMatrix("σ ", sigma); System.out.println(""); 
				}
				break;
			case  HAScorr:	
				corr = new double[10];
				n = txt.parseArray(corr);
				if(n!=10) {
					txt.setError("[Astropos.parse]: " + n + "/10 correlations");
					while(n<10) corr[n++]=0;
					ok = false;
				}
				// Verify within [-1,+1] (done in set)
				break;
			case HASep:
				specif |= HASep;
				epochs[0] = getEpoch(txt);
				break;
			case HASmep:
				specif |= HASmep;
				epochs[1] = getEpoch(txt);
				break;
			}
			if(ok) specif |= type;
			else txt.set(posini);
			if(DEBUG) dump("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1]);
		}
		if(!ok) return(ok);
		if(DEBUG) System.out.println("#     specif=0x" + Integer.toHexString(specif) + "; epochs=" + epochs[0] + ", " + epochs[1] + " => ok=" + ok
				+ "\n#...Parsing(p): " + txt);
		if((corr!=null)&&(sigma==null)&&((specif&HASerr)==0)) {
			System.err.println("#+++Astropos.parse: correlations without sigmas");
			corr=null;
		}
		if(!ok) return(ok);

		// check too many / contradictory epochs ??
		// About epoch: verify permanent epoch is correct!
		if((specif&HASep)!=0) {
			if((permanent&HASep)==0) this.epoch = epochs[0];
			else if(Math.abs(epochs[0]-epoch)>Astroframe.Jsec) System.err.println(
					"#+++Astropos.parse: ignore epoch: " + epochs[0]);
		}
		if((specif&HASep)!=0) {
			if((permanent&HASmep)==0) this.epoch = epochs[1];
			else if(Math.abs(epochs[1]-epochs[1])>Astroframe.Jsec) System.err.println(
					"#+++Astropos.parse: ignore meanEp: " + epochs[1]);
		}

		// Install all parameters found.
		if(DEBUG) dump("#...before calling set...");
		if((specif&HASpm)!=0) 
			ok &= this.setProperMotion(mu1, mu2);
		if(DEBUG) dump("#...after called this.set(lon, lat, " + mu1 + ", " + mu2 + ")");
		if(((permanent&HASep)==0) && ((specif&HASep)!=0))
			ok &= this.setEpoch(epochs[0]);
		if(((permanent&HASmep)==0) && ((specif&HASmep)!=0))
			ok &= this.setMeanEpoch(epochs[1]);
		if((specif&HASplx)!=0) 
			ok &= this.setParallax(plx[0], plx[1]);
		if((specif&HASrv)!=0)
			ok &= this.setRadialVelocity(rv[0], rv[1]);
		if(eepos!=null)	// could be in σ
			ok &= this.setErrorEllipse(eepos[0], eepos[1], eepos[2]);
		if(eepm!=null) 	// could be in σ
			ok &= this.setErrorProperMotion(eepm[0], eepm[1], eepm[2]);
		if(DEBUG) dump("#...before calling setSigmas");
		if(sigma!=null) 
			ok &= this.setSigmas(sigma);
		if(DEBUG) dump("#...before calling setCorrelations");
		if(corr!=null) 
			ok &= this.setCorrelations(corr);
		if(DEBUG) dump("#...after  calling setCorrelations, ok=" + ok);
		return(ok);
	}

	/**
	 * Parsing method: interpret a String.
	 * @param txt to interpret.
	 * @param offset starting position in text
	 * @return new position.
	 **/
	public int parse(String txt, int offset) {
		Parsing t = new Parsing(txt, offset);
		if (parse(t)) return(t.pos);
		return(offset);
	}

	//  ===========================================================
	//			Set in Astropos
	//  ===========================================================


	/**
	 * Clear up: unknown position, keep just epoch/meanEpoch if
	 * these are fixed.
        if((specif&HASepos))
            ok &= this.setErrorEllipse(eepos);
        if(DEBUG) dump("#...before calling setSigmas");
        if(sigma!=null) 
            ok &= this.setSigmas(sigma);
        if(DEBUG) dump("#...before calling setCorrelations");
        if(corr!=null) 
            ok &= this.setCorrelations(corr);
        if(DEBUG) dump("#...after  calling setCorrelations, ok=" + ok);
	return(ok);
     }

  //  ===========================================================
  //			Set in Astropos
  //  ===========================================================


    /**
	 * Clear up: unknown position, keep just epoch/meanEpoch if
	 * these are fixed.
	 */
	private final void reset() {
		specified = permanent;
		estimated = 0;
		x = y = z = xd = yd = zd = 0;
		mu0 = mu1 = mu2 = 0;
		dlon = dlat = 0; ready = 0;
		rv = e_rv = plx = e_plx = 0;
		if((permanent&HASep)==0)  epoch = 0./0.;	// Default epoch
		else if(Double.isNaN(epoch)) specified &= ~HASep;
		if((permanent&HASmep)==0) meanEp=frame.epoch;
		else if(Double.isNaN(meanEp)) specified &= ~HASmep;
		if(cov!=null) for(int i=0; i<cov.length; i++) cov[i]=0;
		if(sig5corr!=null) for(int i=0; i<sig5corr.length; i++) sig5corr[i]=0;
	}

	/**
	 * Reset position to unknown.
	 **/
	public void set() {
		super.set();
		reset();
	}

	/** (not really useful)
	 * New position from a Coo
	 * @param coo  A coordinate
    public void set(Coo coo) {
	reset();
	super.set(coo);
	if (R == null) R = super.localMatrix();
	else super.localMatrix(R);
	specified = HASpos;
        ready = 0;
    }
	 **/

	/**
	 * Set position from another Astrocoo
	 * @param coo  A coordinate
    public void set(Astrocoo coo) {
	reset();
	super.set(coo);
	if (R == null) R = super.localMatrix();
	else super.localMatrix(R);
	ready = 0;
    }
	 **/

	/** (not really useful)
	 * Set position from Coordinate and Epoch.
	 * @param coo    A coordinate
	 * @param epoch  Epoch of position
    public void set(Coo coo, double epoch) {
	reset();
	super.set(coo, epoch);
	if (R == null) R = super.localMatrix();
	else super.localMatrix(R);
	specified = HASpos;
        if(!Double.isNaN(epoch)) specified |= HASep;
        ready = 0;
    }
	 **/

	/**
	 * Set position from RA + Dec.
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 **/
	public void set(double lon, double lat) {
		reset();
		super.set(lon, lat);
		if(R == null) R = super.localMatrix();
		else super.localMatrix(R);
		specified = HASpos; 
		estimated = 0;
		ready = 0;
	}

	/**
	 * Set position from RA + Dec and epoch (replaces Astrocoo)
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param epoch	epoch of the longitude/latitude, Jyr
	 * @return true if ok, false if epoch not compatible
    public boolean set(double lon, double lat, double epoch) {
	set(lon, lat);
        if(Double.isNaN(epoch)) return(true);
	if((permanent&HASep)==0) {
            this.epoch = epoch;
            specified |= HASep;
            return(true);
        }
        // is OK when epochs close
        return(Math.abs(epoch-this.epoch)<=Astroframe.Jsec);
    }
	 **/

	/** (ambiguous with Gaia)
	 * Set position from RA + Dec.
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param epoch	epoch of the longitude/latitude, year
	 * @param mu1	proper motion along longitude,  mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
    public void set(double lon, double lat, double epoch, double mu1, double mu2) {
	set(lon, lat, epoch);
	if (Double.isNaN(mu1) || Double.isNaN(mu2)) ;
	else setProperMotion(mu1, mu2);
    }
	 **/

	/** 
	 * Set position from RA + Dec.
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param mu1	proper motion along longitude,  mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
	 **/
	public void set(double lon, double lat, double mu1, double mu2) {
		set(lon, lat);
		if(DEBUG) System.out.println("#...Astropos.set(" + lon + ", " + lat + ", " + mu1 + ", " + mu2 + ")");
		if(Double.isNaN(mu1) || Double.isNaN(mu2)) ;
		else setProperMotion(mu1, mu2);
	}

	/**
	 * Set a Astropos from position + proper motion. To remove ?
	 * @param lon	longitude (RA), degrees
	 * @param lat	latitude (Dec), degrees
	 * @param epoch	epoch of the longitude/latitude, year
	 * @param eepos	error ellipse, mas
	 * @param mu1	proper motion along longitude,  mas/yr
	 * @param mu2	proper motion along latitude,  mas/yr
	 * @param meanEp	epoch of proper motion (minimal position error)
	 * @param eepm	error ellipse of proper motion, mas/yr
	 * @param plx2	parallax + its error (mas)
	 * @param rv2	radial velocity + its error (km/s)
	 */
	public void set(double lon, double lat, double epoch, double[] eepos, 
			double mu1, double mu2, double meanEp, double[] eepm, 
			double[] plx2, double[] rv2) {
		set(lon, lat, mu1, mu2);
		setEpoch(epoch);
		if (eepos != null) 
			this.setErrorEllipse(eepos[0], eepos[1], eepos[2], meanEp);
		if (eepm != null)
			this.setErrorProperMotion(eepm[0], eepm[1], eepm[2]);
		if (plx2 != null)
			this.setParallax(plx2[0], plx2[1]);
		if (rv2 != null)
			this.setRadialVelocity(rv2[0], rv2[1]);
	}

	/**
	 * Set a particuliar set of positions in the Astropos.
	 * The precision is adapted to the number of significant digits
	 * existing in the input text string.
	 * @param text  Longitude + latitude in text
	 * @throws ParseException when the input text cannot be fully interpreted
	 *         for a position and an eventual epoch.
	 **/
	public void set(String text) throws ParseException {
		if(DEBUG) System.out.println("#...Astropos.set(String): " + text);
		if(frame==null) throw new ParseException
		("[Astropos.set, frame=null] argument=" + text, 0);
		reset();
		Parsing t = new Parsing(text);
		boolean ok = parse(t);
		if(ok) t.gobbleSpaces();
		if((!ok) || (t.pos!=t.length)) {
			String msg = "[Astropos.set, frame=" + frame + "] argument '" + text + "'";
			if(t.error_message!=null) msg = msg + " " + t.error_message;
			throw new ParseException(msg + "+" + t.pos, t.pos);
		}
	}

	/**
	 * Set a particuliar set of positions in the Astropos.
	 * Added to replace the Coo method (see set(String text) )
    public void set(String text, boolean equatorial) throws ParseException {
	set(text);
    }
	 **/


	//  ===========================================================
	//			Set parts 
	//  ===========================================================

	/** 
	 * Assign another frame to an Astropos.
	 * @param frame the frame in which we want to express position
	 * @return true if opeation is ok.
	 **/
	public boolean setFrame(Astroframe frame) {
		reset();
		this.frame = frame;
		return true;
	}

	/** (problematic)
	 * Set the default epoch of the frame to current epoch.
	 * @return true if opeation is ok.
    public boolean setFrameEpoch() {
        if(ready!=0) return(false);
	return(this.frame.setFrameEpoch(this.epoch));
    }
	 **/

	/**
	 * Set a Default Epoch. (e.g. 1991.25 for Hipparcos data)
	 * @param epoch: The default epoch, in Julian years.
	 * 		A <tt>NaN</tt>value removes the default epoch.
	 * @return true if opeation is ok.
	 **/
	public boolean setDefaultEpoch(double epoch) {
		if(ready!=0) return(false);
		if(Double.isNaN(epoch)) 
		{ permanent &= ~HASep; specified &= ~HASep; }
		else { permanent |=  HASep; specified |=  HASep; }
		this.epoch = epoch;
		return(true);
	}

	/**
	 * Set a Default Mean Epoch. (e.g. 1991.25 for Hipparcos data)
	 * @param meanEp The default mean epoch, in Julian years.
	 * 		A <tt>NaN</tt>value removes the default mean epoch.
	 * @return true if opeation is ok.
	 **/
	public boolean setDefaultMeanEpoch(double meanEp) {
		if(ready!=0) return(false);
		if(Double.isNaN(meanEp)) 
		{ permanent &= ~HASmep; specified &= ~HASmep; }
		else { permanent |=  HASmep; specified |=  HASmep; }
		this.meanEp = meanEp;
		return(true);
	}

	/**
	 * Set how many significant digits should be used to edit the errors.
	 * If unspecified, the number of significant digits used for the edition
	 * of errors is related to the coordinate precision (which can be specified
	 * with the {@link Astrocoo#setPrecision} method). When secified, the precision
	 * of the edition is increased if necessary to ensure that the errors are edited
	 * with at least the specified number of significant digits.
	 * @param errorprec the minimal level of precision for error (and values) editions;
	 *           it is limited between 0 (don't care) and 7 (max.)
	 **/
	public void setErrorPrecision(int errorprec) {
		if(errorprec>7) this.errorprec = 7;
		else if(errorprec<0) this.errorprec = 0;
		else this.errorprec = (byte)errorprec;
	}

	/**
	 * Set a Default Error on Position -- later !
	 * @param e_pos Error on position.
	 * 		An <tt>NaN</tt>value remove the default error on position.
	 **/
	/*
    public void setErrorPosition(double e_pos) {
	permanent_epos = e_pos;
	if (Double.isNaN(e_pos))
	     permanent &= ~0x10;
	else permanent |= 0x10;
    }
	 */

	/**
	 * Set a Default Error on Proper Motion. -- later !
	 * @param e_pm Error on position.
	 * 		An <tt>NaN</tt>value remove the default error on position.
	 **/
	/*
    public void setErrorProperMotion(double e_pm) {
	permanent_epm = e_pm;
	if (Double.isNaN(e_pm ))
	     permanent &= ~0x20;
	else permanent |= 0x20;
    }
	 */

	/**
	 * Set the Epoch of the position.
	 * @param epoch The epoch of the position. 
	 * 		A <tt>NaN</tt>value removes the epoch.
	 * @return   true when parameter OK, false if ignored, or if contradicts the DefaultEpoch
	 **/
	public boolean setEpoch(double epoch) {
		if(ready!=0) return(false);	// too late, already computed!
		if((permanent&HASep)!=0) 	// can't be changed
			return(Math.abs(epoch-this.epoch)>Astroframe.Jsec);
		else if(Double.isNaN(epoch)) specified &= ~HASep;
		else {  this.epoch = epoch;  specified |=  HASep; }
		return(true);
	}

	/**
	 * Set the Mean Epoch of the position (epoch of minimal error).
	 * @param meanEp The mean epoch of the position. 
	 * 		A <tt>NaN</tt>value removes the epoch.
	 * @return   true when parameter OK, false if ignored, or contradicts the DefaultMeanEpoch.
	 **/
	public boolean setMeanEpoch(double meanEp) {
		if(ready!=0) return(false);	// too late, already computed!
		if((permanent&HASmep)!=0) 
			return(Math.abs(meanEp-this.meanEp)>Astroframe.Jsec);
		else if(Double.isNaN(meanEp)) specified &= ~HASmep;
		else { this.meanEp = meanEp;  specified |=  HASmep; }
		return(true);
	}

	/**
	 * Set the Error Ellipse. Note that the position MUST have been set
	 * 			before, otherwise this parameter is ignored...
	 * @param e_maj Semi-major axis of error ellipse (mas)
	 * @param e_min Semi-minor axis of error ellipse (mas
	 * @param pa    Position angle of error ellipse (deg)
	 * @return   true when parameter OK, false if positions not known.
	 *
	 **/
	public boolean setErrorEllipse(double e_maj, double e_min, double pa) {
		return setErrorEllipse(e_maj, e_min, pa, 0./0.);
	}

	/**
	 * Set the Error Ellipse, with the epoch of this error (meanEp, mean epoch)
	 * @param e_maj Semi-major axis of error ellipse (mas)
	 * @param e_min Semi-minor axis of error ellipse (mas
	 * @param pa    Position angle of error ellipse (deg)
	 * @param meanEp  Mean epoch (epoch of the error)
	 * @return   true when parameter OK, false if positions not known or
	 *           error ellipse already specified.
	 **/
	public boolean setErrorEllipse(double e_maj, double e_min, double pa, double meanEp) {
	  if(DEBUG) System.out.println("ready: " + ready + "; (specified&HASepos): " + (specified&HASepos));
		if(DEBUG) System.out.println("#...Astropos.setErrorEllipse(" + e_maj + ", " + e_min + ", " + pa + ", meanEp=" + meanEp + ")");
		if(ready!=0) return(false);	// too late, already computed!
		if((specified&HASepos)!=0)	// already specified
			return(false);
		if(Double.isNaN(pa)) pa=90.0;
		// Check validity of meanEp: don't accept if contradictory
		if(Double.isNaN(meanEp)) ;
		else if(((specified&HASmep)!=0)&&(meanEp!=this.meanEp)) {
			System.err.println("#+++Astropos.setErrorEllipse: can't change meanEp=" 
					+ this.meanEp + " to " + meanEp);
			return(false);	
		}
		else { this.meanEp = meanEp; specified |= HASmep; }
		if((e_maj<=0)||(e_min<=0)) 	// Don't accept zero errors...
			return(false);
		if(sig5corr == null) sig5corr = new double[15];
		sig5corr[2] = 0;		// correlation RA:Dec
		if((pa==90.0)||(e_maj==e_min)) { sig5corr[0] = e_maj; sig5corr[1] = e_min; }
		else if (pa==0.0)              { sig5corr[0] = e_min; sig5corr[1] = e_maj; }
		else {
			// Need to compute via error ellipse
			double[] ell = { e_maj, e_min, pa };
			double[] var = new double[3];
			errorEllipseToVariance(ell, var);
			//if(cov == null) cov = new double[21];
			// Reset to zero all positional parts in the covariance matrix
			// (exception the bottom-right quarter)
			//for(int i=6; i<18; i++) cov[i]=0;
			sig5corr[0] = Math.sqrt(var[0]);
			sig5corr[1] = Math.sqrt(var[1]);
			sig5corr[5] = var[2]/(sig5corr[0]*sig5corr[1]);
			if(DEBUG) { 
				System.arraycopy(sig5corr, 0, var, 0, 3); 
				AstroMath.printMatrix("#...setErrorEllipse with: ", ell); 
				AstroMath.printMatrix("#...............sig5corr: ", var);
			}
		}
		specified |= HASepos;
		return(true);
	}

	/**
	 * Set the Proper Motion.
	 * Note that, for FK4 only, the proper motions are expressed in mas/Byr.
	 * @param mu1   Proper motion along longitude or Right Ascension (mas/yr)
	 * @param mu2   Proper motion along latitude or Declination (mas/yr)
	 * @return   true when parameter OK, false if positions not known
	 *           or already set.
	 **/
	public boolean setProperMotion(double mu1, double mu2) {
		if(ready!=0) return(false);	// too late, already computed!
		if((specified&(HASpos|HASpm))!=HASpos) 
			return(false);
		if(Double.isNaN(mu1) || Double.isNaN(mu2)) 
			return(false);
		this.mu1 = mu1;
		this.mu2 = mu2;
		// FK4 uses another unit for time...
		if(this.frame instanceof FK4) {
			this.mu1 *= FK4.FJB;
			this.mu2 *= FK4.FJB;
		}
		specified |= HASpm;
		/*-- done in compute_fix
        // Move proper motion to fixed frame.
	double v[] = new double[3];
	double d[] = new double[3];
	v[0] = mu0 / AstroMath.MAS;
	v[1] = mu1 / AstroMath.MAS;
	v[2] = mu2 / AstroMath.MAS;
	rot2fixed(v, d);
	xd = d[0]; yd = d[1]; zd = d[2];
        --*/
		return(true);
	}

	/**
	 * Set the Error on Proper Motion
	 * @param e_mu1 Error on longitude or Right Ascension (mas/yr)
	 * @param e_mu2 Error on latitude or Declination (mas/yr)
	 * @return   true when parameter OK, false if positions not known.
	 **/
	public boolean setErrorProperMotion(double e_mu1, double e_mu2) {
		return this.setErrorProperMotion(e_mu1, e_mu2, 90.) ;
	}

	/**
	 * Set the Error on Proper Motions
	 * @param e_maj Semi-major axis of error ellipse (mas)
	 * @param e_min Semi-minor axis of error ellipse (mas
	 * @param pa    Position angle of error ellipse (deg)
	 * @return   true when parameter OK, false if positions and/or proper motions not known.
	 **/
	public boolean setErrorProperMotion(double e_maj, double e_min, double pa) {
		if(DEBUG) System.out.println("#...Astropos.setErrorProperMotion(" + e_maj + ", " + e_min + ", " + pa + ")");
		if(ready!=0) return(false);	// too late, already computed!
		if ((specified&(HASpos|HASpm|HASepm))!=(HASpos|HASpm)) 
			return(false);
		if((e_maj<=0)||(e_min<=0)) 	// Don't accept zero errors...
			return(false);
		if(sig5corr == null) sig5corr = new double[15];
		sig5corr[14] = 0;
		if((pa==90.0)||(e_maj==e_min)) { sig5corr[3] = e_maj; sig5corr[4] = e_min; }
		else if (pa==0.0)              { sig5corr[3] = e_min; sig5corr[4] = e_maj; }
		else {	// Need to compute covariance matrix
			double[] ell = { e_maj, e_min, pa };
			double[] var = new double[3];
			errorEllipseToVariance(ell, var);
			sig5corr[3]  = Math.sqrt(var[0]);
			sig5corr[4]  = Math.sqrt(var[1]);
			sig5corr[14] = var[2]/(sig5corr[3]*sig5corr[4]);
		}
		specified |= HASepm;
		return(true);
	}

	/**
	 * Set the Radial Velocity
	 * @param rv    Radial velocity  in km/s
	 * @param err   Error on radial velocity  in km/s
	 * @return   true when parameter OK, false if position not known, or NaN
	 **/
	public boolean setRadialVelocity(double rv, double err) {
		if(ready!=0) return(false);	// too late, already computed!
		if(DEBUG) System.out.println("#...Astropos.setRadialVel(beg)(" + rv + ", " + err + ";   specified=0x" + Integer.toHexString(specified));
		if((specified&HASrv)!=0)	// already specified
			return(false);
		if(Double.isNaN(rv))
			return(false);
		this.rv   = rv; specified |= HASrv;
		this.e_rv = err;
		if(!Double.isNaN(err)) specified |= HAServ;
		if(DEBUG) System.out.println("#...Astropos.setRadialVelocity(" + rv + ", " + err + " => specified=0x" + Integer.toHexString(specified));
		return(true);
	}

	/**
	 * Set the Parallax
	 * @param plx   Parallax, in mas (kpc<sup>-1</sup>)
	 * @param err   Error on parallax (mas)
	 * @return   true when parameter OK, false if positions not known, or NaN
	 **/
	public boolean setParallax(double plx, double err) {
		if(DEBUG) System.out.println("#...Astropos.setParallax(" + plx + ", err=" + err + ")");
		if(ready!=0) return(false);	// too late, already computed!
		if((specified&HASplx)!=0)	// already specified
			return(false);
		if(Double.isNaN(plx))
			return(false);
		this.plx   = plx; specified |= HASplx;
		this.e_plx = err;
		if(!Double.isNaN(err)) specified |= HASeplx;
		return(true);
	}

	/**
	 * Set the Sigmas on the 5 astrometric parameters.
	 * @param sigmas Array with the sigmas, in <i>mas</i> and <i>mas/yr</i>, 
	 *        of the 5 astrometric parameters  RA(α), Dec(δ), Parallax(ϖ),
	 *        Proper motion in RA (µα), Proper motion in Dec (µδ).
	 *        It may contain also the correlations ??
	 * @return   true when parameters OK, false if positions not known, 
	 *           any of the Sigma's in unklnown (NaN)
	 **/
	public boolean setSigmas(double[] sigmas) {
		if(ready!=0) return(false);	// too late, already computed!
		if((sigmas.length!=5)&&(sigmas.length!=15)) {
			System.err.println(
					"#+++Astropos.setSigmas: require RA/Dec/plx/pmRA/pmDec array, "
							+ sigmas.length + "-array supplied");
			return(false);
		}
		if((specified&HASerr)!=0) {	// already specified
			int mod = specified&HASerr;
			if(((mod&HASeplx)!=0) && (e_plx==sigmas[2])) 
				mod &= ~HASeplx; 
			if(((mod&HASepos)!=0) && (sigmas[0]==sig5corr[0]) && (sigmas[1]==sig5corr[1]))
				mod &= ~HASepos; 
			if(((mod&HASepm)!=0) && (sigmas[3]==sig5corr[3]) && (sigmas[4]==sig5corr[4]))
				mod &= ~HASepm; 
			if(mod!=0) System.err.println("#+++Astropos.setSigmas: values of "
					+ explain(permanent, partnames) + " supersede previously assigned values");
		}
		// Check all values specified
		if(Double.isNaN(sigmas[0]+sigmas[1]+sigmas[2]+sigmas[3]+sigmas[4]))
			return(false);
		// Copy the values
		if(sig5corr == null) sig5corr = new double[15];
		System.arraycopy(sigmas, 0, sig5corr, 0, 5);
		/*---
        // Unit is rad
        System.arraycopy(
	if(cov == null) cov = new double[21];
        cov[1] = sigmas[0]*sigmas[0]/MAS2;
        cov[2] = sigmas[1]*sigmas[1]/MAS2;
        e_plx  = sigmas[2];
        cov[4] = sigmas[3]*sigmas[3]/MAS2;
        cov[5] = sigmas[4]*sigmas[4]/MAS2;
        --*/
		// Copy also the error on plx
		e_plx = sig5corr[2];
		specified |= HASerr;
		return(true);
	}

	/**
	 * Set the Correlations on the 5 astrometric parameters.
	 * @param corr Array with the correlations, in mas and mas/yr, 
	 *        of the 5 astrometric parameters  RA(α), Dec(δ), Parallax(ϖ),
	 *        Proper motion in RA (µα), Proper motion in Dec (µδ).
	 * @return   true when parameters OK, false if positions not known, 
	 *           any of the Sigma's in unklnown (NaN)
	 **/
	public boolean setCorrelations(double[] corr) {
		if(ready!=0) return(false);	// too late, already computed!
		if((specified&HAScorr)!=0) 	// Already specified
			return(false);
		int ocor=0;
		if(corr.length==15) ocor=5;
		else if(corr.length!=10) {
			System.err.println(
					"#+++Astropos.setCorrelations: require 10-correlations array, "
							+ corr.length + "-array supplied");
			return(false);
		}
		if((specified&HASerr)!=HASerr) {
			System.err.println("#+++Astropos.setCorrelations: no sigmas?");
			return(false);
		}
		if(sig5corr == null) sig5corr = new double[15];
		boolean status=true;
		double acceptable = 1.0+1.e-6;
		// Copy and check correlations
		System.arraycopy(corr, ocor, sig5corr, 5, 10);
		for(int i=5; status&&(i<sig5corr.length); i++) {
			if(Double.isNaN(sig5corr[i])) status=false;
			double acor = Math.abs(sig5corr[i]);
			if(acor<=1.0) continue;
			// Accept small out-of-limit values
			if(acor>acceptable) {
				System.err.println("#***Astropos.setCorrelations: corr[" + i + "]=" + sig5corr[i]);
				status=false;
			}
			else if(sig5corr[i]<0) sig5corr[i] = -1.0;
			else                   sig5corr[i] =  1.0;
		}
		// Reset correlations to zero if failed
		if(status) specified |= HAScorr;
		else for(int i=5; i<sig5corr.length; i++) sig5corr[i]=0;
		if(DEBUG) AstroMath.printMatrix("#...Installed correlations => sig5corr:\n", sig5corr);
		return(true);
	}

	//  ===========================================================
	//			Get parts of Astropos
	//  ===========================================================

	/**
	 * getFrame, getPrecision, getLon, getLat, getEditing -- inherited
	 **/

	/**
	 * Get the Error Ellipse of the position
	 * @param ee3 Vector of 3 components <i>(a, b, posAngle)</i> 
	 *        (a and b in mas, posAngle in degrees)
	 * @return true for actual error ellipse, false for estimated one.
	 **/
	public boolean copyErrorEllipse(double[] ee3) {
		if(ready!=0x31) compute_cor();
		double[] var = { sig5corr[0]*sig5corr[0], 
				sig5corr[1]*sig5corr[1], 
				sig5corr[1]*sig5corr[0]*sig5corr[5] };
		varianceToErrorEllipse(var, ee3);
		return((specified&(~estimated)&HASepos) != 0);
	}

	/**
	 * Get the longitude (overload the Coo method)
	 * @return The longitude (degrees)
	 **/
	public double getLon() {
		if(ready==0) compute_fix();
		return(super.getLon());
	}

	/**
	 * Get the latitude (overload the Coo method)
	 * @return The latitude (degrees)
	 **/
	public double getLat() {
		if(ready==0) compute_fix();
		return(super.getLat());
	}

	/**
	 * Get the mean error on Longitude ({sigma}RA*cos(Dec))
	 * @return The mean error (estimated or actual) in mas
	 **/
	public double sigmaLon() {
		if(ready==0x31) return(sig5corr[0]);
		if(ready==0) compute_fix();
		return(Math.sqrt(getVariance(1,1)));
	}

	/**
	 * Get the mean error on Latitude ({sigma}(Dec))
	 * @return The mean error (estimated or actual) in mas
	 **/
	public double sigmaLat() {
		if(ready==0x31) return(sig5corr[1]);
		if(ready==0) compute_fix();
		return(Math.sqrt(getVariance(2,2)));
	}


	/**
	 * Get the proper motions (2 components)
	 * @param pm Vector of 2 components (pmRA*cos(Dec), pmDec)
	 * @return true for actual proper motion, false for estimated one.
	 **/
	public boolean copyProperMotion(double[] pm) {
		if((ready&0x10)==0) compute_loc();
		boolean status = (specified&(~estimated)&HASpm)!=0;
		pm[0] = mu1;
		pm[1] = mu2;
		// Different unit for FK4 !
		if(this.frame instanceof FK4) {
			pm[0] /= FK4.FJB;
			pm[1] /= FK4.FJB;
		}
		return(status);
	}

	/**
	 * Get the Longitude proper motion (mas/yr)
	 * @return Proper motion in Longitude (RA), in mas/yr.
	 *         Is NaN when unknown.
	 **/
	public double getProperMotionLon() {
		if((specified&HASpm)==0) return(0./0.);
		if((ready&0x10)==0) compute_loc();
		double pm = mu1;
		if(this.frame instanceof FK4) pm /= FK4.FJB;
		return(pm);
	}

	/**
	 * Get the Latitude proper motion (mas/yr)
	 * @return Proper motion in Latitude (Dec), in mas/yr
	 **/
	public double getProperMotionLat() {
		if((specified&HASpm)==0) return(0./0.);
		if((ready&0x10)==0) compute_loc();
		double pm = mu2;
		if(this.frame instanceof FK4) pm /= FK4.FJB;
		return(pm);
	}

	/**
	 * Get the total proper motion, in mas/yr.
	 *         Is just Math.hypot(getProperMotionLon(), getProperMotionLat())
	 * @return Total proper motion (positive or null)
	 **/
	public double getTotalProperMotion() {
		if((specified&HASpm)==0) return(0./0.);
		if((ready&0x10)==0) compute_loc();
		double pm = Math.hypot(mu1, mu2);
		if(this.frame instanceof FK4) pm /= FK4.FJB;
		return(pm);
	}

	/**
	 * Get the  proper motion position angle, in degrees.
	 * @return Proper motion in Latitude (Dec), in mas/yr
	 **/
	public double getProperMotionPositionAngle() {
		if((specified&HASpm)==0) return(0./0.);
		if((ready&0x10)==0) compute_loc();
		return(AstroMath.atan2d(mu1, mu2));
	}

	/**
	 * Get the Error Ellipse of the Proper Motion
	 * @param ee3 Vector of 3 components <i>(a, b, posAngle)</i> 
	 *        (a and b in mas/Jyr, posAngle in degrees)
	 * @return true for actual error ellipse, false for estimated one.
	 **/
	public boolean copyProperMotionErrorEllipse(double[] ee3) {
		boolean status = (specified&(~estimated)&HASepm)!=0;
		if(ready!=0x31) compute_cor();
		double[] var = { sig5corr[3]*sig5corr[3], 
				sig5corr[4]*sig5corr[4], 
				sig5corr[3]*sig5corr[4]*sig5corr[14] };
		varianceToErrorEllipse(var, ee3);
		if(this.frame instanceof FK4) { ee3[0] /= FK4.FJB; ee3[1] /= FK4.FJB; }
		return(status);
	}

	/**
	 * Get the mean error on Longitude proper motion ({sigma}pmRA*cos(Dec))
	 * @return The mean error (estimated or actual) in mas/yr
	 **/
	public double sigmaProperMotionLon() {
		if((specified&HASepm)==0) return(0./0.);
		double f = this.frame instanceof FK4 ? FK4.FJB : 1.0;
		if(ready==0x31) return(sig5corr[3]/f);
		if(ready==0) compute_fix();
		return(Math.sqrt(getVariance(4,4)/f));
	}

	/**
	 * Get the mean error on Latitude proper motion ({sigma}(Dec))
	 * @return The mean error (estimated or actual) in mas
	 **/
	public double sigmaProperMotionLat() {
		if((specified&HASepm)==0) return(0./0.);
		double f = this.frame instanceof FK4 ? FK4.FJB : 1.0;
		if(ready==0x31) return(sig5corr[4]/f);
		if(ready==0) compute_fix();
		return(Math.sqrt(getVariance(5,5)/f));
	}

	/**
	 * Get the Parallax
	 * @param valerr Vector of 2 components <i>(plx, e_plx)</i> (mas)
	 * @return true for actual parallax, false for estimated one.
	 **/
	public boolean copyParallax(double[] valerr) {
		// not necessary to compute the correlations
		if((ready&0x10)!=0) compute_loc();
		valerr[0] = plx;
		valerr[1] = e_plx;
		return((specified&(~estimated)&HASplx) != 0);
	}

	/**
	 * Get the Velocity
	 * @param valerr Vector of 2 components <i>(Rvel, e_Rvel)</i> (mas)
	 * @return true for actual parallax, false for estimated one.
	 **/
	public boolean copyVelocity(double[] valerr) {
		// not necessary to compute the correlations
		if((ready&0x10)!=0) compute_loc();
		valerr[0] = rv;
		valerr[1] = e_rv;
		return((specified&(~estimated)&HASrv) != 0);
	}

	/**
	 * Get the Sigma's for RA, Dec, Plx, pmRA, pmDec
	 * @param sigmas Vector of 5 components RA, Dec, Plx, pmRA, pmDec;
	 * results in mas, and mas/yr for the proper motion errors.
	 * If the array is large enough (≥15) corelations are also copied.
	 * @return true for actual values, false for estimated ones.
	 **/
	public boolean copySigmas(double[] sigmas) {
		if(ready!=0x31) compute_cor();
		if(sigmas.length>=15) System.arraycopy(sig5corr, 0, sigmas, 0, 15);
		else System.arraycopy(sig5corr, 0, sigmas, 0, 5);
		return((specified&(~estimated)&HASerr) == HASerr);
	}

	/**
	 * Get the correlations for RA, Dec, Plx, pmRA, pmDec
	 * @param corr Vector of 10 correlations
	 * If the array is large enough (≥15) sigma's are also copied.
	 * results in mas, and mas/yr for the proper motion errors.
	 * @return true for actual values, false for estimated ones.
	 **/
	public boolean copyCorrelations(double[] corr) {
		if(ready!=0x31) compute_cor();
		if(corr.length>=15) System.arraycopy(sig5corr, 0, corr, 0, 15);
		else System.arraycopy(sig5corr, 5, corr, 0, 10);
		return((specified&(~estimated)&HAScorr) == HAScorr);
	}

	//  ===========================================================
	//			Compare two Coordinates
	//  ===========================================================

	/**
	 * Compare 2 coordinates.
	 * @param o Objet a comparer.
	 * @return Vrai si o est identique a this (sauf errors/covariances)
	 **/
	public boolean equals(Object o) {
		boolean res = false;
		if(!(o instanceof Astropos)) return(res);
		Astropos a = (Astropos)o;
		// Warning: must be in same state (normalized or not)
		if((this.ready&a.ready^0x11)!=0) {
			// Let's prepare them instead of leaving!!
			//if((this.ready&a.ready&0x01)==0) return(res);
			if((this.ready&0x10)==0) this.compute_loc();
			if((a.ready&0x10)==0) a.compute_loc();
		}
		if(!super.equals(o)) return(res);
		res = this.xd == a.xd && this.yd == a.yd && this.zd == a.zd;
		return res;
	}

	/**
	 * Compute the hashcode
	 * @return the hascode value
	 */
	public int hashCode() {
		int hcode = super.hashCode();
		long l = Double.doubleToLongBits(xd);
		hcode = hcode * 123 + (int) (l^(l >>> 32));
		l = Double.doubleToLongBits(yd);
		hcode = hcode * 123 + (int) (l^(l >>> 32));
		l = Double.doubleToLongBits(zd);
		hcode = hcode * 123 + (int) (l^(l >>> 32));
		l = Double.doubleToLongBits(meanEp);
		hcode = hcode * 123 + (int) (l^(l >>> 32));
		return hcode;
	}

	//  ===========================================================
	//			Edit the Astropos
	//  ===========================================================

	/**
	 * Method to edit the Coordinates in a StringBuffer
	 * @param buf  Buffer where the result is appended
	 * @param opt  A mixture of the options ED_COLON, ED_DECIMAL,
	 *			EDIT_FULL, EDIT_SEXA, EDIT_FRAME
	 * @return	Number of bytes used in edition

    private final StringBuffer ed1(StringBuffer buf, int opt) {
	super.edit(buf, opt); 
    	return (buf);
     }
	 **/

	/**
	 * edit an error ellipse (without the brackets)
	 * @param ee3  the error ellipse
	 * @param full_prec   "true" for edition with full precision
	 * @param nd   the number of decimals in the edition, ≥10 for full precision
	 */
	static private void editErrorEllipse(StringBuffer buf, double[] ee3, boolean full_prec, int nd) {
		if (nd <= 0) nd = -1;
		if(full_prec) buf.append(ee3[0]);
		else ed.editDecimal(buf, ee3[0], 0, nd, 0); 
		buf.append(' '); 
		if(full_prec) buf.append(ee3[1]);
		else ed.editDecimal(buf, ee3[1], 0, nd, 0); buf.append(' ');
		buf.append(' '); 
		if(full_prec) buf.append(ee3[2]);
		else {	// How many decimals for θ ? 
			double ae = AstroMath.dexp(nd)*(ee3[0]-ee3[1]);
			int m=0;
			//if(DEBUG) buf.append(" {ae=" + ae);
			while(ae>100.) { m++; ae /= 10; }
			//if(DEBUG) buf.append("=>m=" + m + "} ");
			ed.editDecimal(buf, ee3[2], 0, m, 0); 
		}
	}

	/**
	 * Default edition: use what's stored
	 * @return the edited string
	 **/
	public String toString() {
		return(toString(editing));
	}

	/**
	 * Default edition: use what's stored
	 * @return the edited string
	 **/
	public String toString(String opt) {
		return(toString(editingOptions(opt)));
	}

	/**
	 * Edition with integer option
	 * @param  opt Option, as in Astrocoo
	 * @return the edited string
	 **/
	public String toString(int opt) {
		if(DEBUG) System.out.println("#...Astropos.toString(" + Integer.toHexString(opt) + ")");
		double[] ee3 = new double[3];
		double[]  pm = new double[2];
		int derr = precision + errorprec - 8;	// Number of decimals for error.
		char sep = ' ';
		if(ready!=0x31) compute_cor();
		// System.out.println("equinox==" + equinox) ;
		// System.out.println("z=" + z) ;
		StringBuffer buf = new StringBuffer(200) ;
		int o = (precision&0x80) != 0 ? Editing.SEXA3 : Editing.DECIMAL;
		int merged_opt = editing|opt;
		boolean full_prec = (merged_opt&EDIT_FULL)!=0;
		boolean add_names = (merged_opt&EDIT_NAMING)!=0;
		boolean add_corr  = (merged_opt&EDIT_CORRELATIONS)!=0;
		boolean add_sigma = (merged_opt&EDIT_5SIGMAS)!=0;
		// System.out.println("...meanEp here, specified="+specified + 
		//                    ", editing=" + editing);
		if((!full_prec)&&(errorprec>0)&&((specified&HASerr)!=0)) {
			// Use an extended precision if necessary.
			double sigmin = 0.5*AstroMath.dexp(errorprec);
			int m=0;
			for(int i=0; i<5; i++) { 
				if(sig5corr[i]<1.e-8) continue;	// impossible
				while(sig5corr[i]<sigmin) { 
					//if(DEBUG) buf.append(" {σ[" + i + "]=" + sig5corr[i] + "<" + sigmin + "++}");
					m++; sigmin/=10.0; 
				}
			}
			//if(DEBUG) buf.append(" {precision+" + m + " since sigmin=" + sigmin + "}");
			byte saved_precision = precision;
			this.setPrecision(precision+m);
			super.edit(buf, o|opt);	// Edit coo + Ep
			precision = saved_precision;
			if(m>derr) derr=m;
		}
		else super.edit(buf, o|opt);	// Edit coo + Ep
		// Edit the error ellipse, unless EDIT_5SIGMAS specified
		if(add_sigma) ;
		else if (copyErrorEllipse(ee3)) {
			buf.append(" [");
			editErrorEllipse(buf, ee3, full_prec, derr);
			/*-- Mean Epoch is edited, only if differs from apoch */
			if (((merged_opt&EDIT_MEAN_EPOCH) != 0) && ((specified&HASmep) != 0) 
					&& (meanEp!=epoch)) {	
				buf.append(" (");
				editEpoch(buf, meanEp);
				buf.append(")");
			}
			buf.append(']');
			if(add_names) buf.append("mas");
		}
		// Edit the proper motion (max = 6 decimales) and RV
		int nd = derr; if(nd<1) nd=1; if(nd>6) nd=6;
		if (copyProperMotion(pm)) {
			buf.append(' ');
			if(add_names) buf.append("\u00B5=(");
			if(full_prec) buf.append(pm[0]);
			else ed.editDecimal(buf, pm[0], 0, nd, Editing.SIGN_EDIT);
			buf.append(' ');
			if(full_prec) buf.append(pm[1]);
			else ed.editDecimal(buf, pm[1], 0, nd, Editing.SIGN_EDIT);
			if(add_names) buf.append(")");
			if(add_sigma) ;
			else if (copyProperMotionErrorEllipse(ee3)) {
				buf.append(" [");
				editErrorEllipse(buf, ee3, full_prec, derr);
				buf.append(']');
			}
			if(add_names) buf.append("mas");
		}
		// Edit the Parallax
		if ((specified&HASplx) != 0) {
			buf.append(' ');
			if(add_names) buf.append("\u03D6=");
			//else buf.append(" plx=");
			if(full_prec) buf.append(plx);
			else ed.editDecimal(buf, plx, 0, nd, 0);
			if(add_sigma) ;
			else if((specified&HASeplx)!=0) {
				buf.append('[');
				if(full_prec) buf.append(e_plx);
				else ed.editDecimal(buf, e_plx, 0, nd, 0);
				buf.append(']');
			}
			buf.append("mas");
		}
		// Edit the Radial Velocity
		if ((specified&HASrv) != 0) {
			// Number of decimals:
			int m = errorprec>1 ? errorprec : 1;
			if((errorprec>0)&&((specified&HAServ)!=0)) {
				double lim=e_rv*AstroMath.dexp(errorprec);
				while(lim<1) { m++; lim*=10.0; }
			}
			buf.append(' ');
			if(add_names) buf.append("RV=");
			if(full_prec) buf.append(rv);
			else ed.editDecimal(buf, rv, 0, m, Editing.SIGN_EDIT);
			if((specified&HAServ)!=0) {
				buf.append('[');
				if(full_prec) buf.append(e_rv);
				else ed.editDecimal(buf, e_rv, 0, m, 0);
				buf.append(']');
			}
			buf.append("km/s");
		}
		// Edit the Sigmas
		if((add_sigma) && ((specified&HASerr)!=0)) {
			buf.append(' ');
			if(add_names) buf.append("\u03C3=");
			sep = '[';
			for(int i=0; i<5; i++) {
				buf.append(sep);
				if(full_prec) buf.append(sig5corr[i]);
				else ed.editDecimal(buf, sig5corr[i], 0, derr, 0);
				sep = ',';
			}
			buf.append(']');
		}
		// Edit the Correlations
		if(add_corr || ((specified&HAScorr)!=0)) {
			buf.append(' ');
			if(add_names) buf.append("\u03C1=");
			sep = '{';
			for(int i=5; i<15; i++) {
				buf.append(sep);
				// Limit number of decimals to 6
				if(full_prec) buf.append(sig5corr[i]);
				else ed.editDecimal(buf, sig5corr[i], 0, -6, 0);
				sep = ',';
			}
			buf.append('}');
		}
		return buf.toString();
	}
	//  ===========================================================
	//			Convert Astropos
	//  ===========================================================

	/**
	 * Change the epoch of the position (in the same frame).
	 * To be rigorous, it should be multiplied by the matrix
	 *       fromICRSmatrix * motionMatrix(Jyr-this.epoch) * toICRSmatrix
	 * @param Jyr the new epoch
	 * @return true if ok, false when operation has no effect (no proper motion)
	 **/
	public boolean toEpoch(double Jyr) {
		if(DEBUG) {
			dump("#...toEpoch(" + this.epoch + ") => " + Jyr);
		}
		if(ready==0) compute_fix();
		if(this.epoch == Jyr) return true;
		if((specified&HASpm)==0)	// No proper motion => no effect.
			return(false);
		if((permanent&HASep)!=0) 	// Can't change epoch !
			return(false);
		double t = Jyr - this.epoch; 	// Epoch change
		if((this.frame.dotMatrix == Astroframe.Udot) 
				||(Math.abs(this.frame.max_spin*t)<=AstroMath.eps)) {
			x += t*xd;
			y += t*yd;
			z += t*zd;
			if((specified&HASerr)!=0) 
				propagate_error(cov, t);
		}
		else {				// Use dotMatrix
			double[] Tep = this.frame.dotMatrix.clone();
			for(int i=0; i<Tep.length; i++)  Tep[i] *= t;
			for(int i=0; i<Tep.length; i+=7) Tep[i] += 1.0;
			if(DEBUG) AstroMath.printMatrix("#...change epoch from " + this.epoch
					+ " to " + this.frame.epoch + " with:\n", Tep);
			double[] u = { x, y, z, xd, yd, zd };
			AstroMath.m36v(Tep, u, u); 
			x = u[0]; y = u[1]; z = u[2]; xd = u[3]; yd = u[4]; zd = u[5];
			if((specified&HASerr)!=0) 
				cov = AstroMath.mpt(Tep, cov);
		}
		this.localMatrix(R);
		this.lon = this.lat = 0./0.;
		epoch = Jyr;		// This is my new epoch.
		ready = 1;		// ok in fixed frame
		return(true);
	}

	/**
	 * Transform the position into another frame.
	 * On return, all astrometric components are converted into the
	 * new frame specified, at the default epoch of new_frame.
	 * @param new_frame	The frame of the resulting position.
	 * @return false when not possible (when epoch or mean epochs are permanent)
	 **/
	public boolean convertTo(Astroframe new_frame) {
	  //if (DEBUG) System.out.println("# READY = " + ready);
		if(ready==0) compute_fix();
	  if (DEBUG) System.out.println("@@ COV: " + Arrays.toString(cov));
	  
		double new_epoch = new_frame.epoch;
		if((new_frame.equals(frame))&&(this.epoch==new_epoch))
			return(true);
		if(((permanent&HASep)!=0) && (this.epoch!=new_epoch)) {
			System.err.println("#***Astroframe.convertTo(" + frame 
					+ "): epoch can't be changed, is: " + this.epoch);
			return(false);
		}
		if(DEBUG) System.out.println("#...Astropos.convert from " 
				+ this.getFrame() + ",Ep=" + this.epoch + " to " 
				+ new_frame + ",Ep=" + new_epoch);

		// 6x6 matrix used for the conversion:
		double[] M = Astroframe.convertMatrix(this.frame, new_frame);
		if(DEBUG) AstroMath.printMatrix("#...matrix used to convert from " + this.frame 
				+ " to " + new_frame + "\n", M);

		// Use a 3-vector for the conversions if proper motion not known
		// (simple rotation), otherwise a 6-vector.
		double[] u;
		if((specified&HASpm)!=0) {
			// Verify first if epoch is same as the frame one
			// ... otherwise we've to apply a correction via derivative, i.e.
			// multiply by [1]+dt*dotMatrix
			if(this.epoch != this.frame.epoch) {
				double t = this.frame.epoch - this.epoch;
				double[] Tep = this.frame.dotMatrix.clone();
				for(int i=0; i<Tep.length; i++) Tep[i] *= t;
				for(int i=0; i<Tep.length; i+=7) Tep[i] += 1.0;
				if(DEBUG) AstroMath.printMatrix("#...change epoch from " + this.epoch
						+ " to " + this.frame.epoch + " with:\n", Tep);
				M = AstroMath.mp(M, Tep);
				if(DEBUG) AstroMath.printMatrix(
						"#... which results in the final conversion matrix:\n", M);
			}
			u = new double[6];
			u[3] = xd; u[4] = yd; u[5] = zd;
		}
		else { 
			// Unknown proper motions: the epoch can't be changed!
			new_epoch = this.epoch;
			u = new double[3];
		}
		u[0] = x;  u[1] = y;  u[2] = z;
		if(DEBUG) AstroMath.printMatrix("#... u(in) = ", u); 

		// Convert the phase-space vector
		AstroMath.m36v(M, u, u);
		// Convert the covariance matrix
		double[] new_cov = null;
		if(((specified|estimated)&(HASerr|HAScorr))!=0) {
      if(DEBUG) System.out.println("#...Astropos.convertTo. HASerr: " + HASerr + "; HAScorr: " + HAScorr);
			if(DEBUG) System.out.println("#...Astropos.convertTo: compute new covariance, length=" + M.length);
			if (cov != null) {
			  if(M.length==9 && cov != null)  rotate_cov(M, cov, cov);
			  else     new_cov = AstroMath.mpt(M, cov);
			}
			//if(DEBUG) AstroMath.printMatrix("#....M. ", M);
			//if(DEBUG) AstroMath.printMatrix("#...cov ", cov);
			//if(DEBUG) AstroMath.printMatrix("#=>prod ", new_cov);
		}

		// Install the new values
		if(DEBUG) AstroMath.printMatrix("#... u(out)= ", u); 
		this.lon = this.lat = 0./0.;	// Actual angles yet computed
		this.x  = u[0]; this.y  = u[1]; this.z  = u[2];
		if(u.length>3) { this.xd = u[3]; this.yd = u[4]; this.zd = u[5]; }
		this.epoch = new_epoch;
		this.frame = new_frame;
		this.meanEp = new_epoch;
		this.localMatrix(R);
		if(DEBUG) AstroMath.printMatrix("\n#new_cov", new_cov);
		if(new_cov!=null) {
			if(this.cov==null) this.cov=new_cov;
			else System.arraycopy(new_cov, 0, cov, 0, cov.length);
			if(DEBUG) {
				System.out.println("#...Astropos.convertTo: update covariance");
				AstroMath.printMatrix("#=>.cov ", cov);
				AstroMath.printMatrix("#thicov ", this.cov);
			}
		}
		ready = 1;	// ok in fixed frame.
		return(true);
	}

	/**
	 * Transform the position into another frame.
	 * On return, all astrometric components are converted into the
	 * new frame specified, at the default epoch of new_frame.
	 * @param source	The source position, unchanged
	 * @param dest	The destination position: all parameters but the frame are set.
	 * @return true if operation is OK.
	 **/
	public static boolean convert(Astropos source, Astropos dest) {
		if(source.ready==0) source.compute_fix();
		if(DEBUG) System.out.println("#...Astropos.convert from " 
				+ source.getFrame() + ",Ep=" + source.epoch + " to " 
				+ dest.frame);

		// Copy the fixed part, including the correlations
		double new_epoch = dest.frame.epoch;
		dest.ready = 0;
		dest.permanent = 0;
		dest.specified = source.specified;
		dest.estimated = source.estimated;
		dest.epoch = dest.meanEp = new_epoch;
		dest.dlon  = source.dlon;
		dest.dlat  = source.dlat;
		//dest.formRA = source.formRA;
		//dest.precision = source.precision;
		dest.lon = dest.lat = 0./0.;    // Actual angles not recomputed
		dest.rv    = source.rv;
		dest.e_rv  = source.e_rv;
		dest.plx   = source.plx;
		dest.e_plx = source.e_plx;
		if(source.sig5corr!=null) {
			if(dest.sig5corr==null) dest.sig5corr = source.sig5corr.clone();
			else System.arraycopy(source.sig5corr, 0, dest.sig5corr, 0, 
					source.sig5corr.length);
		}

		// Find the appropriate conversion matrix.
		double[] M = Astroframe.convertMatrix(source.frame, dest.frame);
		if(DEBUG) AstroMath.printMatrix("#...matrix used to convert from " + source.frame 
				+ " to " + dest.frame + "\n", M);

		// Use a 3-vector for the conversions if proper motion not known
		// (simple rotation), otherwise a 6-vector.
		double[] u;
		if((source.specified&HASpm)!=0) {
			// Verify first if epoch is same as the frame one
			// ... otherwise we've to apply a correction via derivative, i.e.
			// multiply by [1]+dt*dotMatrix
			if(source.epoch != source.frame.epoch) {
				double t = source.frame.epoch - source.epoch;
				double[] Tep = source.frame.dotMatrix.clone();
				for(int i=0; i<Tep.length; i++) Tep[i] *= t;
				for(int i=0; i<Tep.length; i+=7) Tep[i] += 1.0;
				if(DEBUG) AstroMath.printMatrix("#...change epoch from " + source.epoch
						+ " to " + source.frame.epoch + " with:\n", Tep);
				M = AstroMath.mp(M, Tep);
				if(DEBUG) AstroMath.printMatrix(
						"#... which results in the final conversion matrix:\n", M);
			}
			u = new double[6];
			u[3] = source.xd; u[4] = source.yd; u[5] = source.zd;
		}
		else { 
			// Unknown proper motions: the epoch can't be changed!
			new_epoch = source.epoch;
			u = new double[3];
		}
		u[0] = source.x;  u[1] = source.y;  u[2] = source.z;
		if(DEBUG) AstroMath.printMatrix("#... u(in) = ", u); 
		// Convert the phase-space vector
		AstroMath.m36v(M, u, u);
		// Convert the covariance matrix
		if(((source.specified|source.estimated)&(HASerr|HAScorr))!=0) {
			if(DEBUG) System.out.println("#...Astropos.convert: compute new covariance, length=" + M.length);
			if(M.length==9) {
				if(dest.cov==null) dest.cov = new double[21];
				rotate_cov(M, source.cov, dest.cov);
			}
			else {
				double[] new_cov = AstroMath.mpt(M, source.cov);
				if(dest.cov==null) dest.cov = new_cov;
				else System.arraycopy(new_cov, 0, dest.cov, 0, new_cov.length);
			}
		}
		else if(dest.cov!=null) {	// All sigmas/correlations to zero
			for(int i=0; i<dest.cov.length; i++) dest.cov[i] = 0;
		}
		// Install the new values
		if(DEBUG) AstroMath.printMatrix("#... u(out)= ", u); 
		dest.lon = dest.lat = 0./0.;	// Actual angles yet computed
		dest.x  = u[0]; dest.y  = u[1]; dest.z  = u[2];
		if(u.length>3) { dest.xd = u[3]; dest.yd = u[4]; dest.zd = u[5]; }
		dest.epoch = new_epoch;
		if(dest.R==null) 
			dest.R = dest.localMatrix();
		else dest.localMatrix(dest.R);
		dest.specified = source.specified;
		dest.estimated = source.estimated;
		dest.ready = 1;	// ok in fixed frame.
		//if(DEBUG) dest.dump("#...convert(" + source + " => " + dest + "): dest is");
		return(true);
	}

	
}
