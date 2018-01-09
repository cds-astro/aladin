// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.astro ;

/*==================================================================
                Astropos Class  (Coo -> Astrocoo -> Astropos)
 *==================================================================*/

import java.io.*;
import java.text.*; 	 // for parseException

/**
 * The class <b>Astropos</b> adds to <b>Astrocoo</b> the notions 
 * of <i>mouvement</i> (proper motions, velocity) and <i>error</i> ellipses.
 * <P>The edition of the position in an Astropos can be done in a StringBuffer
 * (<B>edit</B> methods) with options like sexagesimal or decimal edition,
 * or as a String with one of the available <B>toString</B> methods.
 * In string format, an astropos looks like:<br>
 * <i>longitude  +/- latitude</i> <b>(</b><i>epoch</i><b>)</b>
 * <b>[</b><i>errorEllipse_mas</i> <b>(</b><i>meanEpoch</i><b>)</b><b>]</b>
 * <i>pm_lon pm_lat</i>  <b>[</b><i>errorEllipse_mas/yr</i> <b>]</b>
 * <i>parallax_mas</i> <b>[</b><i>error</i><b>]</b>
 * <i>radialVelocity_km/s</i> <b>[</b><i>error</i><b>]</b></i>
 * <P>The values are expressed internally as cartesian components in the
 * associated frame. Errors are expressed internally as a 6x6 covariance matrix,
 * (multinormal error distribution is asumed), also aligned with the frame
 * main axes.
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
 * <P>Conversions into other frames ({@link #convertTo} method) imply a
 * change of all parameters (position, proper motions, errors). A change
 * of the epoch is achieved with the {@link #toEpoch} method.
 * @author Francois Ochsenbein [CDS]
 * @version 1.0  03-Aug-2004
 * @version 1.1  03-Sep-2006
 * @version 1.2  12-Jul-2007
 * @version 1.21 12-Jul-2007: bug in toEpoch
 * @version 1.22 18-Nev-2014: use setEditing
 */

public class Astropos extends Astrocoo implements Serializable {
    static private boolean DEBUG = false; 
    /** Derivative of <b>x</b> cartesian component (in Jyr<sup>-1</sup>) */
    protected double xd;
    /** Derivative of <b>y</b> cartesian component (in Jyr<sup>-1</sup>) */
    protected double yd;
    /** Derivative of <b>z</b> cartesian component (in Jyr<sup>-1</sup>) */
    protected double zd;
    /** Elements of rotation matrix. Notice that R00=x, R01=y, R02=z
    	This is a duplication of part of the data already stored
	but array manipulations is not really flexible in java...
     **/
    private double R[][];		// localMatrix.
    /*
    private double 
	// R00=x R01=y R02=z
	   R10,  R11, // R12=0 
           R20,  R21,  R22;
  /** The velocity vector is a rotation of the Unit derivative:
     *  v0 is radial, v1 and v2 are the proper motions pm(alpha) and pm(delta),
     *  and are expressed in mas/yr (mas = milli-arcsec).
     **/
    /** The epoch of the position with minimal uncertainty (mean epoch).
     * Unit = Jyr
     **/
    protected double epocho;
    /** Parallax in mas */
    public double plx;	
    /** Radial velocity in km/s */
    public double rv;	
    /** Mean error on parallax in mas */
    public double e_plx;
    /** Mean error on radial Velocity in km/s */
    public double e_rv;
    // Permanent values
    //public double permanent_epos;	// Permanent error on pos, in mas
    //public double permanent_epm ;	// Permanent error on pm , in mas
    // Components of Astropos may have a permanent status, e.g. epocho, epoch...
    // 1=plx, 2=RV, 4=pm, 8=epocho, 0x10=cov.pos, 0x20=cov.pm, 0x80=Epoch,
    //                              0x40=cov.pos from precision
    private byte permanent;	// Status of permanent parameters
    private byte specified;
    private boolean ready;	// when all elements computed.

    private static String partnames[] = { "plx", "rv", "pm", "meanEp", 
        "cov.pos", "cov.pm", "cov.from.prec",  "epoch" };
    /** Covariance matrix between all components (angles in mas, time in Jyr).
     * it includes errors as well as correlations between position 
     * and proper motions.
     **/
    protected double[][] cov;
    /**
     * Estimation of the errors on position, from the precision.
     **/
    static private double[] def_err = {
        180.*3.6e6,	// 0: no position
        2.1*3.6e6,	// 1: deg
        2.1*3.6e5,	// 2: 0.1deg
        2.1*6e4,	// 3: 1'
        2.1*6e3,	// 4: 0.1'
        2.1*1000.,	// 5: 1"
        2.1*100.,	// 6: 0.1"
        2.1*10.,	// 7: 0.01"
        2.1,		// 8: 1mas
        2.1/10.,	//10: 0.1mas
        2.1/100.,	//11: 0.01mas
        2.1/1000.,	//12: 1uas
    };
    //static private double[]  def_rv  = { 0., 50. };	// Default RV
    //static private double[]  def_plx = { 9., 10. };	// Default plx (mas)

    //  ===========================================================
    //			In-line utilities
    //  ===========================================================

    /* Transpose a 3x3 matrix */
    private static final void tr3(double m[][]) {
        double v;
        //System.out.println(Coo.toString("...tr3(0)", m));
        v = m[0][1]; m[0][1] = m[1][0]; m[1][0] = v;
        v = m[0][2]; m[0][2] = m[2][0]; m[2][0] = v;
        v = m[1][2]; m[1][2] = m[2][1]; m[2][1] = v;
        //System.out.println(Coo.toString("   tr3(1)", m));
    }
    /* Compute the product R.u = v */
    private final void rot2local(double u[], double v[]) {
        v[0] = R[0][0]*u[0] + R[0][1]*u[1] + R[0][2]*u[2];
        v[1] = R[1][0]*u[0] + R[1][1]*u[1] ;	
        v[2] = R[2][0]*u[0] + R[2][1]*u[1] + R[2][2]*u[2];
    }
    /* Compute the product tR.v = u */
    private final void rot2fixed(double v[], double u[]) {
        u[0] = R[0][0]*v[0] + R[1][0]*v[1] + R[2][0]*v[2];
        u[1] = R[0][1]*v[0] + R[1][1]*v[1] + R[2][1]*v[2];
        u[2] = R[0][2]*v[0]                + R[2][2]*v[2];
    }

    private final void rot3(double R[][], double v[]) {
        int i; double x,y,z;
        x = v[0]; y = v[1]; z = v[2];
        for(i=0; i<3; i++)
            v[i]   = R[i][0]*x + R[i][1]*y + R[i][2]*z;
    }

    /* Rotate the 6x6 covariance (symetrical) matrix.
     * If Y=RX, then var(Y)=R.var(X).tR.
     * Element  =  Sum(k,l) Rik.Vkl.Rjl.
     * Product |R 0| |V00 V01| |tR 0| = |R.V00.tR R.V01.tR|
     *         |0 R| |V10 V11| |0 rR| = |R.V10.tR R.V11.tR|
     **/
    private final static void rotate_cov (double[][] R, double[][] cov) {
        if (cov == null) return;
        double t3[][] = new double[3][3];	// Temporary
        double t; int i,j,k,l;
        //if(DEBUG) pr_cov("   rot6(0) ", cov);
        // 1. Upper left
        for (i=0; i<3; i++) for (j=0; j<3; j++)
            t3[i][j] = cov[i][j];
        for (i=0; i<3; i++) for (j=0; j<3; j++) {
            t=0.;
            for (k=0; k<3; k++) for (l=0; l<3; l++)
                t += R[i][k]*R[j][l]*t3[k][l];
            cov[i][j] = t;
        }
        //if(DEBUG) pr_cov("   rot6(1) ", cov);
        // When only 3-D, that's all!
        if (cov[0].length == 3) return;

        // 2. Lower right
        for (i=0; i<3; i++) for (j=0; j<3; j++)
            t3[i][j] = cov[i+3][j+3];
        for (i=0; i<3; i++) for (j=0; j<3; j++) {
            t=0.;
            for (k=0; k<3; k++) for (l=0; l<3; l++)
                t += R[i][k]*R[j][l]*t3[k][l];
            cov[i+3][j+3] = t;
        }
        //if(DEBUG) pr_cov("   rot6(2) ", cov);

        // 3. Off-diagonal (symetrical):
        k = 0;		// Counter non-null values
        for (i=0; i<3; i++) for (j=0; j<3; j++) {
            t3[i][j] = cov[i][j+3];
            if (t3[i][j] != 0) k++;
        }
        if (k!=0) for (i=0; i<3; i++) for (j=0; j<3; j++) {
            t=0.;
            for (k=0; k<3; k++) for (l=0; l<3; l++)
                t += R[i][k]*R[j][l]*t3[k][l];
            cov[i][j+3] = cov[i+3][j] = t;
        }
        //if(DEBUG) pr_cov("   rot6(3) ", cov);
    }

    /** 
     * Rotate one specific variance/covariance.
     *           Sum(i).Sum(j) { R[i1,i] . R[i2,j] . cov[i,j]
     * @param i1 Index of param.1 (1=RA, 2=Dec, 4=mu1, 5=mu2)
     * @param i2 Index of param.2 (1=RA, 2=Dec, 4=mu1, 5=mu2)
     */
    private final double getVar(int i1, int i2) {
        if ((cov == null) || (R == null))
            return(0./0.);
        int o1, o2, k1, k2, i, j;
        o1 = 3*(i1/3); o2 = 3*(i2/3); 
        k1 = i1%3;     k2 = i2%3;
        double t = 0;	// Sum(i).Sum(j) { R[i1,i] . R[i2,j] . cov[i,j] }
        for (i=0; i<3; i++) for (j=0; j<3; j++)
            t += R[k1][i]*R[k2][j]*cov[o1+i][o2+j];
        //System.out.print("....getVar(" + i1 + "," + i2 + ")" );
        //System.out.print("; k1=" + k1 + ", k2=" + k2);
        //System.out.print("; o1=" + o1 + ", o2=" + o2);
        //System.out.println(" ==> " + t);
        return(t);
    }

    private static final void propagate_error(double var[][], double t) {
        /* Compute the error propagation as 
 	   | 1  0  0  t  0  0 |           | 1  0  0  0  0  0 |
	   | 0  1  0  0  t  0 |           | 0  1  0  0  0  0 |
	   | 0  0  1  0  0  t | *  var  * | 0  0  1  0  0  0 |
	   | 0  0  0  1  0  0 |           | t  0  0  1  0  0 |
	   | 0  0  0  0  1  0 |           | 0  t  0  0  1  0 |
	   | 0  0  0  0  0  1 |           | 0  0  t  0  0  1 |
         */
        int i,j;
        for (j=0; j<3; j++) for (i=0; i<6; i++)
            var[i][j] += t*var[i][j+3];
        for (i=0; i<3; i++) for (j=0; j<6; j++)
            var[i][j] += t*var[i+3][j];
    }

    /* Compute Product Matrix(3x3) applied to 6-vector: v = R . u */
    private static final void rot6(double R[][], double[] u, double[] v) {	
        int i;
        for(i=0; i<3; i++) {
            v[i]   = R[i][0]*u[0] + R[i][1]*u[1] + R[i][2]*u[2];
            v[i+3] = R[i][0]*u[3] + R[i][1]*u[4] + R[i][2]*u[5];
        }
    }

    /* Compute T . V . tT product of matrices */
    /* Note that the matrices must be in different locations ! */
    /** Finally unused... 
    private static final void TVtT(int n, double T[][], double V[][], 
	    double out[][]) {
      double t; int i,j,k,l;
	// Simple computation
        for (i=0; i<n; i++) for (j=0; j<n; j++) {
	    t=0.;
	    for (k=0; k<n; k++) for (l=0; l<n; l++) 
		t += T[i][k]*T[j][l]*V[k][l] ;
	    out[i][j] = t;
	}
    }
     **/

    //  ===========================================================
    //			Error ellipses and Variances
    //  ===========================================================

    /**
     * Renormalize the vectors
     **/
    public double  normalize() {
        double norm = x*x+y*y+z*z;
        if (norm <= 0) return (norm);
        if (norm == 1) return (norm);
        if (cov != null) { int i, j;
        for(i=0; i<6; i++) for (j=0; j<6; j++)
            cov[i][j] /= norm;
        }
        norm = Math.sqrt(norm);
        x  /= norm;  y /= norm;  z /= norm;
        xd /= norm; yd /= norm; zd /= norm;
        plx /= norm; e_plx /= norm;
        if (R != null) { 
            R[0][0] /= norm; R[0][1] /= norm; R[0][2] /= norm;
            R[2][0] /= norm; R[2][1] /= norm; R[2][2] /= norm;
        }
        return(norm);
    }

    /**
     * Rotate an Astropos. Its changes position, proper motions and errors
     * @param R [3][3] matrix
     **/
    public final void rotate (double[][] R) {
        double v[] = new double[3];
        super.rotate(R); 	// Rotate x, y, z
        /* Rotate derivatives: */
        v[0] = xd; v[1] = yd; v[2] = zd;
        xd = R[0][0]*v[0] + R[0][1]*v[1] + R[0][2]*v[2];
        yd = R[1][0]*v[0] + R[1][1]*v[1] + R[1][2]*v[2];
        zd = R[2][0]*v[0] + R[2][1]*v[1] + R[2][2]*v[2];
        /* Rotate the variance: R . var . tR */
        if (cov != null) 
            rotate_cov(R, cov);
    }

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
     * The relation
     **/
    public static final void varianceToErrorEllipse(double[] var, double[] ee3){
        double delta, a2, b2, t;
        //System.out.println(Coo.toString("   var: ", var));
        t = (var[0]-var[1])/2.; delta = Math.sqrt(t*t+var[2]*var[2]);
        t = (var[0]+var[1])/2.; a2 = t+delta; b2 = t-delta;
        ee3[0] = Math.sqrt(a2);
        ee3[1] = Math.sqrt(b2);
        // Theta
        if (var[0] == var[1]) {
            ee3[2] = 90;
        } else if (var[2] == 0) {
            ee3[2] = (var[0] <= var[1] ? 0 : 90.);
        } else {
            ee3[2] = AstroMath.atand((a2 - var[1]) / var[2]);
        }
        if (ee3[2] < 0) {
            ee3[2] += 180. ;
        }
        //System.out.println(Coo.toString(" =>ell: ", ee3));
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
    public static boolean parseErrorEllipse (Parsing txt, double[] ee3) {
        int posini = txt.pos;
        int pos, postart, n;
        ee3[0] = ee3[1] = 0./0.;
        ee3[2] = 90.;	// Default theta
        // Text could start by the opening bracket.
        txt.gobbleSpaces();
        n = txt.parseArray(ee3);
        if (n == 0) return(false);	// Not a single number found! 
        if (Double.isNaN(ee3[2])) ee3[2] = 90.;		// Default theta
        if (Double.isNaN(ee3[1])) ee3[1] = ee3[0];	// Default b=a
        return(true);
    }

    /*===========================================================
   			Compute all elements
     *===========================================================*/

    /**
     * Debugging: print position + velocity
     */
    private final void pru6(String title) {
        double  u[] = new double[3];
        String tit2 = "                                           ";
        u[0] = x ; u[1] = y ; u[2] = z ;
        System.out.println(Coo.toString(title, u));
        u[0] = xd; u[1] = yd; u[2] = zd;
        System.out.println(Coo.toString(tit2.substring(0, title.length()), u));
    }

    /**
     * Debugging: print covariance matrix
     */
    private final static void pr_cov(String title, double[][] cov) {
        StringBuffer b = new StringBuffer(256);
        int len0 = title.length();
        int i, j;
        b.append(title);
        for(i=0; i<6; i++) {
            for(j=0; j<6; j++) {
                ed.editDouble(b, cov[i][j], 4, 3, 0);
                b.append(' ');
            }
            System.out.println(b.toString());
            b.setLength(len0);
            if (i == 0) {
                b.setLength(0);
                for (j=0; j<len0; j++) b.append(' ');
            }
        }
    }

    /**
     * Compute all elements in an Astropos, and mark it as 'ready'.
     * On input, the Astrocoo part is computed. 
     * On output, the missing elements are computed or estimated.
     */
    private final void compute() {
        double  u6[] = null;		// 6-vector (u, u'), all 0
        double   v[] = new double[3];
        double mu0, mu2;
        int i, j, k, l;

        if (ready) return;

        if(DEBUG) dump("<<compute: ");
        // Verify what's really specified.
        // if (Double.isNaN(plx))    specified &= ~0x1;
        // if (Double.isNaN(rv))     specified &= ~0x2;
        // if (Double.isNaN(epocho)) specified &= ~0x8;
        // if (Double.isNaN(epoch))  specified &= ~0x80;
        // if (cov == null)	  specified &= ~0x30;	// No sigma pos,pm
        // else {
        //     if (Double.isNaN(cov[0][0])) specified &= ~0x10;
        //     if (Double.isNaN(cov[3][3])) specified &= ~0x20;
        // }

        // Epoch of position.
        if (((specified|permanent)&0x80) == 0)
            this.epoch = this.frame.epoch;
        if (((specified|permanent)&0x08) == 0) 	// epocho not specified
            this.epocho = this.epoch;

        // Get parallax . If specified, plx is not null
        if (((specified|permanent)&0x1) == 0) {	// Parallax not specified
            this.plx   = 0.;	// def_plx[0];
            this.e_plx = 0.;	// def_plx[1];
        }
        if (Double.isNaN(this.e_plx)){	// Added V1.2
            this.e_plx = plx;
            // Minimal assumed error of 5mas
            if (this.e_plx < 5.) this.e_plx = 5.;
        }

        // Get Radial Velocity. If specified, RV is not null
        if (((specified|permanent)&0x2) == 0) {	// Radial Velocity not specified
            this.rv   = 0; 	// def_rv[0];
            this.e_rv = 0; 	// def_rv[1];
        }
        if (Double.isNaN(this.e_rv)){	// Added V1.2: assume 50% error
            this.e_rv = Math.abs(this.rv)/2.;
            if (this.e_rv > 300.) this.e_rv = 300.;
            if (this.e_rv < 10.)  this.e_rv = 10.;
        }
        /*System.out.println("#...compute: plx="+this.plx+"["+this.e_plx+"] RV="
		+ this.rv+"["+this.e_rv+"]"); */

        // Proper motions: Estimate
        if (((specified|permanent)&0x4) == 0) {	// Proper motion not specified
            if (this.frame instanceof FK4) {
                if (u6 == null) u6 = new double[6];
                u6[0] = x; u6[1] = y; u6[2] = z;
                FK4.estimateFK4motion(u6);
                xd = u6[3]; yd = u6[4]; zd = u6[5];
            }
            else xd = yd = zd = 0;
            // No proper motion ==> cov(mu) = 0
            if (cov != null) for (i=3; i<6; i++) for (j=3; j<6; j++)
                cov[i][j] = 0;
        }
        mu2 = xd*xd + yd*yd + zd*zd;	// Total mu^2, in rad/yr

        // When RV + plx supplied, change the velocity vector
        if (((specified|permanent)&0x3) == 0x3) {
            mu0 = (plx * rv /4.740470446)	// in mas/yr
                    / (180.*3.6e6/Math.PI) ;	// in rad/yr
            // Added 12 Oct 2006: if no error on position, 
            //       and no proper motion... cov can be nil!
            if (cov != null) cov[3][3] = e_plx * e_rv /4.740470446;
            // There is a problem when v[0] is too large compared to 
            // other components; in this case, set this parameter to
            // zero, but increase its error.
            if (mu0*mu0 >= 0.5*mu2) {
                mu0 = 0;
                if (cov != null) cov[3][3] += mu0*mu0;
            }
            else {	// Assign the radial component to velocity
                xd += x*mu0;
                yd += y*mu0;
                zd += z*mu0;
            }
        }
        if(DEBUG) pru6("   u+du(1) ");

        // Error ellipse on position, compute or estimate when not given
        if ((specified&0x50) == 0 && dlon>0 && dlat>0) {
            if ((permanent&0x10) == 0) {	// Estimate error ellipse
                i = dlon; if (i>=def_err.length) i = def_err.length-1;
                j = dlat; if (j>=def_err.length) j = def_err.length-1;
                this.setErrorEllipse(def_err[i], def_err[j], 90., 0./0.);
                specified ^= 0x50;	// Was changed in setErrorEllipse
            }
            // else this.setErrorEllipse(permanent_epos, permanent_epos, 0);
        }

        // Error ellipse on proper motion.
        if ((specified&0x20) == 0) {		// No error pm specified
            //if ((permanent&0x20) != 0) 
            //	setErrorProperMotion(permanent_epm, permanent_epm, 0);
            //specified &= ~0x20;			// Reset to zero
        }

        /* Progapate the error on proper motions */
        if (this.epoch != this.epocho) {
            if(DEBUG) pr_cov("   cov6(0) ", this.cov);
            propagate_error(cov, this.epoch - this.epocho);
            if(DEBUG) pr_cov("   cov6(t) ", this.cov);
        }

        // 6-D covariance is now correcly set in the local frame.
        // Rotate the 6-D covariance matrix to absolute frame, i.e.
        // with inverse(R) = transposed(R).
        if (this.R != null) {
            tr3(R);	// transpose R
            rotate_cov(this.R, cov);
            tr3(R);	// transpose again, i.e. return to original
        }

        if(DEBUG) dump(" >compute: ");
        ready = true;
    }


    /*===========================================================
   			Constructors
     *===========================================================*/

    /**
     * Create the default (empty) Astropos (ICRS)
     */
    public Astropos() {
        super(new ICRS());
        cov = null;
        permanent = 0;
        epocho = 0./0.;	// No Mean Position
        reset();
    }

    /**
     * Create an empty Astropos.
     * @param frame	in which frame
     */
    public Astropos(Astroframe frame) {
        super(frame);
        cov = null;
        permanent = 0;
        epocho = 0./0.;	// No Mean Position
        reset();
    }

    /**
     * Create a Astropos object with just longitude + latitude
     * @param lon	longitude (RA), degrees
     * @param lat	latitude (Dec), degrees
     */
    public Astropos(Astroframe frame, double lon, double lat) {
        super(frame);
        cov = null;
        permanent = 0;
        epocho = 0./0.;	// No Mean Position
        reset();
        this.set(lon, lat);
    }

    /**
     * Create a Astropos object: coordinates + proper motion.
     * @param lon	longitude (RA), degrees
     * @param lat	latitude (Dec), degrees
     * @param epoch	epoch of the longitude/latitude, year
     * @param mu1	proper motion along longitude, mas/yr
     * @param mu2	proper motion along latitude,  mas/yr
     */
    public Astropos(Astroframe frame, double lon, double lat, double epoch, 
            double mu1, double mu2) {
        super(frame);
        this.set(lon, lat, epoch, (double[])null, 
                mu1, mu2, epoch, (double[])null, 
                (double[])null, (double[])null);
    }

    /**
     * Create a Astropos object from an existing Astrocoo
     * @param coo	Astrocoo object
     */
    public Astropos(Astrocoo coo) {
        this.set(coo);
        // super(coo.getFrame());
        // this.set(coo.getLon(), coo.getLat(), coo.epoch, null,
        //      0./0., 0./0., 0./0., null, null, null);
    }

    /**
     * Create a Astropos object from an existing Astrocoo
     * @param coo	Astrocoo object
     * @param eepos	Error ellipse on position (mas, mas, deg)
     * @param epocho	epoch of the errors on position (minimal error)
     * @param mu1	proper motion along longitude, mas/yr
     * @param mu2	proper motion along latitude,  mas/yr
     * @param eepm	Error ellipse on proper motion (mas/yr, mas/yr, deg)
     * @param plx2	Parallax and its mean error (mas)
     * @param rv2	Radial velocity and its error (km/s)
     * ------ Not necessary
     public Astropos(Astrocoo coo, double eepos[], double epocho,
	     double mu1, double mu2, double eepm[], 
	     double plx2[], double rv2[]) {
	super(coo.getFrame());
	this.set(coo.getLon(), coo.getLat(), coo.epoch, eepos,
	     mu1, mu2, epocho, eepm, plx2, rv2);
     }
     */

    /**
     * Create an Astropos object from a position (Epoch)
     * @param frame	one of the possible Astroframes
     * @param text Text with position, possibly followed by an epoch
     */
    public Astropos(Astroframe frame, String text) throws ParseException {
        super(frame);
        epocho = 0./0.;	// No Mean Position
        this.set(text);
    }

    /**
     * Create an Astropos object from just a string.
     * @param text Text with frame, position, possibly followed by an epoch
     */
    public Astropos(String text) throws ParseException {
        Parsing txt = new Parsing(text);
        // No blank, no colon ==> IAU-style
        char c = txt.currentChar();
        /* System.out.println("#---Astropos(" + text + "): isIAU=" 
		+ Astroframe.isIAU(c) + "; indexes=" 
		+ text.indexOf(' ') + text.indexOf(':')); */
        if (Astroframe.isIAU(c) 
                && (text.indexOf(' ')<0) && (text.indexOf(':')<0)) {
	    // (V1.22) frame = Astroframe.create(c); -- done in Astrocoo
	    frame = null; 
            //	    System.out.println("#...frame="+frame);
            if (super.parseIAU(txt)) {
                txt.gobbleSpaces();
                if (txt.pos >= txt.length) return;
            }
            frame = null;
        }
        frame = Astroframe.parse(txt);
        if (frame == null) throw new ParseException
        ("****Astropos: argument '" + text + "' (no frame)", txt.pos);
	super.setEditing();	// Added V1.22
        epocho = 0./0.;	// No Mean Position
        this.set(txt.toString());
    }

    public Object clone() {
        Astropos c = (Astropos) super.clone();
        return c;
    }



    /**
     * Set a Astropos from position + proper motion.
     * @param frame	one of the possible Astroframes
     * @param lon	longitude (RA), degrees
     * @param lat	latitude (Dec), degrees
     * @param eepos	error ellipse, mas
     * @param epoch	epoch of the longitude/latitude, Julian year
     * @param mu1	proper motion along longitude,  mas/yr
     * @param mu2	proper motion along latitude,  mas/yr
     * @param eepm	error ellipse of proper motion, mas/yr
     * @param epocho	epoch of position error (minimal error), Jyr
     * @param plx2	parallax + its error (mas)
     * @param rv2	radial velocity + its error (km/s)
     */
    public Astropos(Astroframe frame, 
            double lon, double lat, double epoch, double[] eepos, 
            double mu1, double mu2, double epocho, double[] eepm, 
            double[] plx2, double[] rv2) {
        super(frame);
        this.set(lon, lat, epoch, eepos, 
                mu1, mu2, epocho, eepm, plx2, rv2);
    }

    //  ===========================================================
    //			Dump (print object's contents)
    //  ===========================================================

    /**
     * Dump the contents of an Astropos
     * @param title title line
     **/
    public void dump(String title) {
        StringBuffer b = new StringBuffer(256);
        int len0 = title.length();
        int i, j;
        // Subtitle = same length as title, but blank.
        for (i=len0;--i>=0;) b.append(' ');
        String blanks = b.toString(); b.setLength(0);
        super.dump(title);		// Edit the coordinate
        // Edit the derivatives
        b.setLength(0); b.append(blanks);
        ed.editDecimal(b, xd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
        ed.editDecimal(b, yd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
        ed.editDecimal(b, zd, 2, 15, Editing.SIGN_EDIT); b.append(' ');
        b.append("(dot)");
        if ((specified&4)==0)  b.append("::");
        System.out.println(b.toString());
        // Edit the R matrix
        b.setLength(len0); System.out.println(Coo.toString(b.toString(), R));
        // Edit the flags (specified + permanent)
        b.setLength(len0); b.append("    Epoch=J"); b.append(this.epoch);
        i = len0+20; while(b.length() < i) b.append(" ");
        b.append("specified=0x"); b.append(Integer.toHexString(specified&0xff));
        b.append('('); ed.editFlags(b, specified, partnames); 
        b.append(')'); System.out.println(b);
        b.setLength(len0); b.append("MeanEpoch=J"); b.append(this.epocho);
        i = len0+20; while(b.length() < i) b.append(" ");
        b.append("permanent=0x"); b.append(Integer.toHexString(permanent&0xff));
        b.append('('); ed.editFlags(b, permanent, partnames); 
        b.append(')');
        System.out.println(b);
        // Edit the  Pm Vr plx
        b.setLength(len0); b.append("pm(mas/yr) ");
        ed.editDecimal(b, this.getProperMotionLon(), 6, 5, Editing.SIGN_EDIT); 
        b.append(" ");
        ed.editDecimal(b, this.getProperMotionLat(), 6, 5, Editing.SIGN_EDIT); 
        b.append(" ");
        ed.editDecimal(b, this.plx, 4, 5, 0); b.append("mas");
        ed.editDecimal(b, this.rv, 6, 5, Editing.SIGN_EDIT); b.append("km/s");
        //b.append('['); b.append(e_plx); b.append(']');
        //b.append('['); b.append(e_rv); b.append(']');
        System.out.println(b.toString());
        // Edit the Covariance Matrix
        b.setLength(len0);
        b.append("----6x6 Covariance Matrix ");
        if (cov == null) {
            b.append("(NULL) [");
            if (!ready) b.append("NOT ");
            b.append("ready]");
            System.out.println(b.toString());
        }
        else {
            b.append("(mas , yr) [");
            if (!ready) b.append("NOT ");
            b.append("ready]");
            System.out.println(b.toString());
            b.setLength(len0);
            pr_cov(b.toString(), this.cov);
        }
    }

    //  ===========================================================
    //			Interpretation of text (Parsing)
    //  ===========================================================

    /**
     * Duplicate an array
     **/
    static private final double[] dup(double[] v) {
        double[] w = new double[v.length];
        for (int i=0; i<v.length; i++) w[i] = v[i];
        return(w);
    }

    /**
     * Interpret the string and convert to Coo + Epoch.
     * Called from set and parse routines. Interprets until mismatch.
     * @param txt to interpret as a Parsing object e.g.
     *  "12 34.5 -72 54 [30 20 65 (J1991.25)] J2000.0 +125.2 -35.2 [3.9 2.5 45] 123[4]km/s 10[5]"
     * @return true if OK.
     **/
    public boolean parsing(Parsing txt) {
        double[] v3 = new double[3];
        double[] v2 = new double[2];
        double[] eepos=null;	// Error ellipse on position
        double[] eepm=null;	// Error ellipse on proper motion
        double[] plx=null;	// Parallax + Error
        double[] rv=null;		// Radial Velocity + Error
        double mu1, mu2, val, ep, epo;
        boolean bracketed;
        int posini = txt.pos;
        int pos, n;
        if(DEBUG) System.out.println("....Parsing(0): " + txt);

        reset();
        // Use parent class to match the Coo + Epoch
        if (!super.parsing(txt)) return(false);
        ready = false;
        epo = epocho;		// Mean epoch from defaults
        ep = this.epoch;	// Epoch from Astrocoo result
        if(DEBUG) System.out.println("....astrocoo => " + super.toString());
        if(DEBUG) System.out.println("....Parsing(1): " + txt);

        // Try to match the error ellipse
        txt.gobbleSpaces();
        if ((bracketed = txt.match("["))) {
            txt.gobbleSpaces();
            if (parseErrorEllipse(txt, v3)) {	// Error ellipse given
                eepos = dup(v3);
                txt.gobbleSpaces();
                val = getEpoch(txt);
                if (!Double.isNaN(val)) 
                    epo = val;
            }
            txt.gobbleSpaces();
            if (!txt.match("]")) {
                txt.set(posini); 
                return(false); 
            }
        }
        if(DEBUG) System.out.println("....Parsing(2): " + txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // The epoch, or mean epoch
        txt.gobbleSpaces();
        val = getEpoch(txt);
        if (!Double.isNaN(val)) {
            if (Double.isNaN(ep)) ep = val;
            else epo = val;
        }

        // mean epoch could be given as a second epoch within [ ... ]
        txt.gobbleSpaces();
        if (Double.isNaN(epo) && (txt.currentChar() == '[')) {
            // Second epoch bracketed
            posini = txt.pos;
            txt.match('[');
            val = getEpoch(txt);
            if (Double.isNaN(val)) txt.set(posini);
            else if (txt.match(']')) epo = val;
            else txt.set(posini); 
        }
        if(DEBUG) System.out.println("....Parsing(3): " + txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // Get proper motions mu1 mu2 (in mas/year)
        pos = txt.pos;
        txt.gobbleSpaces();
        n = txt.parseArray(v2);
        if (n==2) { 			// Both proper motions required
            mu1 = v2[0]; 
            mu2 = v2[1]; 
        }
        else {
            mu1 = mu2 = 0./0.;
            txt.set(pos);
        }
        if(DEBUG) System.out.println("....Parsing(4): " + txt);

        // Last change for mean epoch
        if (Double.isNaN(epo) && (txt.pos < txt.length))
            epo = getEpoch(txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // Error ellipse of proper motion
        pos = txt.pos;
        txt.gobbleSpaces();
        if ((bracketed = txt.match("["))) {
            txt.gobbleSpaces();
            if (parseErrorEllipse(txt, v3)) 	// pm Error ellipse 
                eepm = dup(v3);
            if (!txt.match("]")) {
                txt.set(posini); 
                return(false); 
            }
        }
        // Last trial for mean epoch of error on position
        if (Double.isNaN(epo) && (txt.pos < txt.length)) 
            epo = getEpoch(txt);
        if(DEBUG) System.out.println("....Parsing(5): " + txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // Parallax
        n = txt.parseWithError(v2);	// May return 0, 1, 2
        if (n > 0) {
            txt.gobbleSpaces();
            if (txt.match("km/s")) {	// Mixed up RV and parallax !
                plx = rv;
                rv = dup(v2);
            }
            else {
                plx = dup(v2);
                txt.match("mas");
            }
        }
        if(DEBUG) System.out.println("....Parsing(p): " + txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // Radial Velocity
        n = txt.parseWithError(v2);	// May return 0, 1, 2
        if (n > 0) {
            txt.gobbleSpaces();
            if (txt.match("mas")) {	// Mixed up RV and parallax !
                plx = dup(v2);
            }
            else {
                rv = dup(v2);
                txt.match("km/s");
            }
        }
        if(DEBUG) System.out.println("....Parsing(V): " + txt);
        if(DEBUG) System.out.println("           epo= " + epo);

        // Install all parameters found.
        set(this.lon, this.lat, ep, eepos, mu1, mu2, epo, eepm,
                plx, rv);
        return(true);
    }

    /**
     * Parsing method: interpret a String.
     * @param txt to interpret.
     * @param offset starting position in text
     * @return new position.
     **/
    public int parse (String txt, int offset) {
        Parsing t = new Parsing(txt, offset);
        if (parsing(t)) return(t.pos);
        return(offset);
    }

    //  ===========================================================
    //			Set in Astropos
    //  ===========================================================


    /**
     * Clear up: unknown position
     */
    private final void reset() {
        specified = 0;
        xd = yd = zd = 0;
        ready = false;
    }

    /**
     * Reset position to unknown.
     **/
    public void set() {
        super.set();
        reset();
    }

    /**
     * New position from a Coo
     * @param coo  A coordinate
     **/
    public void set(Coo coo) {
        reset();
        super.set(coo);
        if (R == null) R = super.localMatrix();
        else super.localMatrix(R);
        ready = false;
    }

    /**
     * Set position from another Astrocoo
     * @param coo  A coordinate
     **/
    public void set(Astrocoo coo) {
        reset();
        super.set(coo);
        if (R == null) R = super.localMatrix();
        else super.localMatrix(R);
        ready = false;
    }

    /**
     * Set position from Coordinate and Epoch.
     * @param coo    A coordinate
     * @param epoch  Epoch of position
     **/
    public void set(Coo coo, double epoch) {
        reset();
        super.set(coo, epoch);
        if (R == null) R = super.localMatrix();
        else super.localMatrix(R);
        ready = false;
    }

    /**
     * Set position from RA + Dec.
     **/
    public void set(double lon, double lat) {
        reset();
        super.set(lon, lat);
        if (R == null) R = super.localMatrix();
        else super.localMatrix(R);
        ready = false;
    }

    /**
     * Set position from RA + Dec and epoch
     **/
    public void set(double lon, double lat, double epoch) {
        set(lon, lat);
        if (Double.isNaN(epoch)) return;
        specified |= 0x80;
        this.epoch = epoch;
    }

    /**
     * Set position from RA + Dec.
     **/
    public void set(double lon, double lat, double epoch,
            double mu1, double mu2) {
        set(lon, lat, epoch);
        if (Double.isNaN(mu1) || Double.isNaN(mu2)) ;
        else setProperMotion(mu1, mu2);
        if (R == null) R = super.localMatrix();
        else super.localMatrix(R);
        ready = false;
    }

    /**
     * Set a Astropos from position + proper motion. To remove ?
     * @param lon	longitude (RA), degrees
     * @param lat	latitude (Dec), degrees
     * @param epoch	epoch of the longitude/latitude, year
     * @param eepos	error ellipse, mas
     * @param mu1	proper motion along longitude,  mas/yr
     * @param mu2	proper motion along latitude,  mas/yr
     * @param epocho	epoch of proper motion (minimal position error)
     * @param eepm	error ellipse of proper motion, mas/yr
     * @param plx2	parallax + its error (mas)
     * @param rv2	radial velocity + its error (km/s)
     */
    public void set (double lon, double lat, double epoch, double[] eepos, 
            double mu1, double mu2, double epocho, double[] eepm, 
            double[] plx2, double[] rv2) {

        set(lon, lat, epoch, mu1, mu2);
        setProperMotion(mu1, mu2);
        if (eepos != null) 
            this.setErrorEllipse(eepos[0], eepos[1], eepos[2], epocho);
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
     **/
    public void set (String text) throws ParseException {
        Parsing t = new Parsing(text);
        reset();
        if (parsing(t))		// Various items found.
            t.gobbleSpaces();
        if (t.pos != t.length) throw new ParseException
        ("****Astropos: argument '" + text + "'+" + t.pos, t.pos);
    }

    /**
     * Overload Coo method (see set(String text) )
     **/
    public void set(String text, boolean equatorial) throws ParseException {
        set(text);
    }


    //  ===========================================================
    //			Set parts 
    //  ===========================================================

    /**
     * Set the default epoch of the frame to current epoch
     **/
    public void setFrameEpoch() {
        if (!Double.isNaN(this.epoch))
            this.frame.setFrameEpoch(this.epoch);
    }

    /**
     * Set a Default Mean Epoch. (e.g. 1991.25 for Hipparcos data)
     * @param epocho The default mean epoch, in Julian years.
     * 		An <tt>NaN</tt>value remove the default mean epoch.
     **/
    public void setDefaultMeanEpoch(double epocho) {
        this.epocho = epocho;
        if (Double.isNaN(epocho))
            permanent &= ~0x8;
        else permanent |= 0x8;
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
     * @param epoch The epoch of the positions.
     * 		An <tt>NaN</tt>value remove the default mean epoch.
     * @return   true when parameter OK, false if ignored.
     **/
    public boolean setEpoch(double epoch) {
        if (ready) return(false);
        if (Double.isNaN(epoch)) 
            specified &= ~0x80;
        else specified |= 0x80;
        this.epoch = epoch;
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
     * Set the Error Ellipse, with the epoch of this error (epocho, mean epoch)
     * @param e_maj Semi-major axis of error ellipse (mas)
     * @param e_min Semi-minor axis of error ellipse (mas
     * @param pa    Position angle of error ellipse (deg)
     * @param epocho  Mean epoch (epoch of the error)
     * @return   true when parameter OK, false if positions not known.
     **/
    public boolean setErrorEllipse(double e_maj, double e_min, double pa, 
            double epocho) {
        double[] ell = new double[3];
        double[] var = new double[3];
        int i, j;
        if (ready) return(false);
        if (Double.isNaN(epocho)) ;
        else { this.epocho = epocho; specified |= 0x8; }
        ell[0] = e_maj; ell[1] = e_min; ell[2] = pa;
        this.errorEllipseToVariance(ell, var);
        if (cov == null) 
            cov = new double[6][6];
        // Reset to zero all positional parts in the covariance matrix
        for (i=0; i<3; i++) for (j=i; j<6; j++) 
            cov[i][j] = cov[j][i] = 0;
        cov[1][1] = var[0];		// RA variance
        cov[2][2] = var[1];		// Dec variance
        cov[2][1] = cov[1][2] = var[2];	// RA/Dec covariance
        specified |= 0x10;
        return(true);
    }

    /**
     * Set the Proper Motion
     * @param mu1   Proper motion along longitude or Right Ascension (mas/yr)
     * @param mu2   Proper motion along latitude or Declination (mas/yr)
     * @return   true when parameter OK, false if positions not known.
     **/
    public boolean setProperMotion (double mu1, double mu2) {
        if (ready) return(false);
        if (Double.isNaN(mu1) || Double.isNaN(mu2)) {
            specified &= ~4;	// No proper motion at all...
            return(true);
        }
        double v[] = new double[3];
        double d[] = new double[3];
        v[0] = 0; 
        v[1] = mu1 / (180.*3.6e6/Math.PI);
        v[2] = mu2 / (180.*3.6e6/Math.PI);
        rot2fixed(v, d);
        xd = d[0]; yd = d[1]; zd = d[2];
        specified |= 0x04;
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
     * @return   true when parameter OK, false if positions not known.
     **/
    public boolean setErrorProperMotion(double e_maj, double e_min, double pa) {
        if (ready) return(false);
        double[] ell = new double[3];
        double[] var = new double[3];
        ell[0] = e_maj; ell[1] = e_min; ell[2] = pa;
        errorEllipseToVariance(ell, var);
        if (cov == null) 
            cov = new double[6][6];
        cov[4][4] = var[0];		// mu1 variance
        cov[5][5] = var[1];		// mu2 variance
        cov[5][4] = cov[4][5] = var[2];	// mu1/mu2 covariance
        specified |= 0x20;
        return(true);
    }

    /**
     * Set the Radial Velocity
     * @param rv    Radial velocity  in km/s
     * @param err   Error on radial velocity  in km/s
     * @return   true when parameter OK, false if position not known, or NaN
     **/
    public boolean setRadialVelocity (double rv, double err) {
        if (ready) return(false);
        if (Double.isNaN(rv))	// Added V1.2
            return(false);
        this.rv   = rv;
        this.e_rv = err;
        specified |= 0x02;
        return(true);
    }

    /**
     * Set the Parallax
     * @param plx   Parallax, in mas (kpc<sup>-1</sup>)
     * @param err   Error on parallax (mas)
     * @return   true when parameter OK, false if positions not known, or NaN
     **/
    public boolean setParallax (double plx, double err) {
        if (ready) return(false);
        if (Double.isNaN(plx))	// Added V1.2
            return(false);
        this.plx   = plx;
        this.e_plx = err;
        specified |= 0x01;
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
    public boolean copyErrorEllipse(double ee3[]) {
        double var[] = new double[3];
        if (!ready) compute();
        var[0] = getVar(1, 1);
        var[1] = getVar(2, 2);
        var[2] = getVar(1, 2);
        varianceToErrorEllipse(var, ee3);
        return((specified&0x10) != 0);
    }

    /**
     * Get the mean error on Longitude ({sigma}RA*cos(Dec))
     * @return The mean error (estimated or actual) in mas
     **/
    public double sigmaLon() {
        if (!ready) compute();
        return(Math.sqrt(getVar(1, 1)));
    }

    /**
     * Get the mean error on Latitude ({sigma}(Dec))
     * @return The mean error (estimated or actual) in mas
     **/
    public double sigmaLat() {
        if (!ready) compute();
        return(Math.sqrt(getVar(2, 2)));
    }


    /**
     * Get the proper motions (2 components)
     * @param pm Vector of 2 components (pmRA*cos(Dec), pmDec)
     * @return true for actual proper motion, false for estimated one.
     **/
    public boolean copyProperMotion(double pm[]) {
        if (R == null) pm[0] = pm[1] = 0./0.;
        else {
            pm[0] = (R[1][0]*xd + R[1][1]*yd)*180.*3.6e6/Math.PI; // R[1][2]=0
            pm[1] = (R[2][0]*xd + R[2][1]*yd + R[2][2]*zd)*180.*3.6e6/Math.PI;
        }
        return((specified&4) != 0);
    }

    /**
     * Get the Longitude proper motion (mas/yr)
     * @return Proper motion in Longitude (RA), in mas/yr
     **/
    public double getProperMotionLon() {
        if (R == null) return(0);
        return (R[1][0]*xd + R[1][1]*yd)*180.*3.6e6/Math.PI; // R[1][2]=0
    }

    /**
     * Get the Latitude proper motion (mas/yr)
     * @return Proper motion in Latitude (Dec), in mas/yr
     **/
    public double getProperMotionLat() {
        if (R == null) return(0);
        return (R[2][0]*xd + R[2][1]*yd + R[2][2]*zd)*180.*3.6e6/Math.PI;
    }

    /**
     * Get the Error Ellipse of the Proper Motion
     * @param ee3 Vector of 3 components <i>(a, b, posAngle)</i> 
     *        (a and b in mas/Jyr, posAngle in degrees)
     * @return true for actual error ellipse, false for estimated one.
     **/
    public boolean copyProperMotionErrorEllipse(double ee3[]) {
        double var[] = new double[3];
        if (!ready) compute();
        var[0] = getVar(4, 4);
        var[1] = getVar(5, 5);
        var[2] = getVar(4, 5);
        varianceToErrorEllipse(var, ee3);
        return((specified&0x20) != 0);
    }

    /**
     * Get the mean error on Longitude proper motion ({sigma}pmRA*cos(Dec))
     * @return The mean error (estimated or actual) in mas/yr
     **/
    public double sigmaProperMotionLon() {
        if (!ready) compute();
        return(Math.sqrt(getVar(4, 4)));
    }

    /**
     * Get the mean error on Latitude proper motion ({sigma}(Dec))
     * @return The mean error (estimated or actual) in mas
     **/
    public double sigmaProperMotionLat() {
        if (!ready) compute();
        return(Math.sqrt(getVar(5, 5)));
    }

    /**
     * Get the Parallax
     * @param valerr Vector of 2 components <i>(plx, e_plx)</i> (mas)
     * @return true for actual parallax, false for estimated one.
     **/
    public boolean copyParallax(double valerr[]) {
        if (!ready) compute();
        valerr[0] = plx;
        valerr[1] = e_plx;
        return((specified&1) != 0);
    }

    /**
     * Get the Velocity
     * @param valerr Vector of 2 components <i>(Rvel, e_Rvel)</i> (mas)
     * @return true for actual parallax, false for estimated one.
     **/
    public boolean copyVelocity(double valerr[]) {
        if (!ready) compute();
        valerr[0] = rv;
        valerr[1] = e_rv;
        return((specified&2) != 0);
    }

    //  ===========================================================
    //			Compare two Coordinates
    //  ===========================================================

    /**
     * Compare 2 coordinates.
     * @param o Objet a comparer.
     * @return Vrai si o est identique a this.
     **/
    public boolean equals(Object o) {
        boolean res = false;
        if(!(o instanceof Astropos)) return(res);
        Astropos a = (Astropos)o;
        if(!super.equals((Coo)o)) return(res);
        res = this.xd == a.xd && this.yd == a.yd && this.zd == a.zd
                && this.epocho == a.epocho;
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
        l = Double.doubleToLongBits(epocho);
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
     *			ED_FULL, ED_SEXA, ED_FRAME
     * @return	Number of bytes used in edition
     **/

    private final StringBuffer ed1 (StringBuffer buf, int opt) {
        super.edit(buf, opt); 
        return (buf);
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
     * @param  opt Option, as in Astrocoo
     * @return the edited string
     **/
    public String toString(int opt) {
        boolean isactual;
        double[] ee3 = new double[3];
        double[]  pm = new double[2];
        int nd;
        // System.out.println("equinox==" + equinox) ;
        // System.out.println("z=" + z) ;
        StringBuffer buf = new StringBuffer(200) ;
        int o = (precision&0x80) != 0 ? Editing.SEXA3 : Editing.DECIMAL;
        if (!ready) compute();
        // System.out.println("...epocho here, specified="+specified + 
        //                    ", editing=" + editing);
        if (!ready) compute();
        super.edit(buf, o|opt);	// Edit coo + Ep
        // Edit the error ellipse
        if ((isactual = copyErrorEllipse(ee3))) {
            nd = precision - 6;
            if (nd <= 0) nd = -1;
            buf.append(" [");
            ed.editDecimal(buf, ee3[0], 0, nd, 0); 
            if (isactual) buf.append(' '); else buf.append(": ");
            ed.editDecimal(buf, ee3[1], 0, nd, 0); buf.append(' ');
            if (isactual) buf.append(' '); else buf.append(": ");
            ed.editDecimal(buf, ee3[2], 0, -2, 0); 
            // Mean Epoch
            if (((specified&8) != 0) || ((editing&EDIT_MEAN_EPOCH) != 0)) {	
                buf.append(" (");
                editEpoch(buf, epocho);
                buf.append(")");
            }
            buf.append(']');
        }
        // Edit the proper motion (max = 6 decimales) and RV
        if ((isactual = copyProperMotion(pm))) {
            buf.append(" ");
            ed.editDecimal(buf, pm[0], 0, -6, Editing.SIGN_EDIT);
            buf.append(" ");
            ed.editDecimal(buf, pm[1], 0, -6, Editing.SIGN_EDIT);
            if ((isactual = copyProperMotionErrorEllipse(ee3))) {
                nd = -2;
                buf.append(" [");
                ed.editDecimal(buf, ee3[0], 0, nd, 0); 
                if (isactual) buf.append(' '); else buf.append(": ");
                ed.editDecimal(buf, ee3[1], 0, nd, 0); buf.append(' ');
                if (isactual) buf.append(' '); else buf.append(": ");
                ed.editDecimal(buf, ee3[2], 0, -2, 0); 
                buf.append(']');
            }
        }
        // Edit the Radial Velocity
        if ((specified&2) != 0) {
            buf.append(" ");
            ed.editDecimal(buf, rv, 0, -6, Editing.SIGN_EDIT);
            buf.append('[');
            ed.editDecimal(buf, e_rv, 0, -6, 0);
            buf.append("]km/s");
        }
        // Edit the Parallax
        if ((specified&1) != 0) {
            buf.append(" ");
            ed.editDecimal(buf, plx, 0, -6, 0);
            buf.append('[');
            ed.editDecimal(buf, e_plx, 0, -6, 0);
            buf.append("]");
        }
        return buf.toString();
    }
    //  ===========================================================
    //			Convert Astropos
    //  ===========================================================

    /**
     * Change the epoch of the position (in the same frame)
     * @param Jyr the new epoch
     **/
    public void toEpoch(double Jyr) {
        double t = Jyr - this.epoch; 	// Epoch change

        if(DEBUG) {
            System.out.println("....toEpoch(" + this.epoch + ") => " + Jyr);
        }
        if (t == 0.) return;
        if (!ready) compute();
        x += t*xd;
        y += t*yd;
        z += t*zd;
        this.lon = this.lat = 0./0.;	// Long/Lat not valid any more, V1.21
        if (cov != null)
            propagate_error(cov, t);
        epoch = Jyr;		// This is my new epoch.
        normalize();
        return;
    }

    /**
     * Transform the position into another frame.
     * On return, all astrometric components are converted into the
     * new frame specified, at the default epoch of new_frame.
     * @param new_frame	The frame of the resulting position.
     **/
    public void convertTo(Astroframe new_frame) {
        double new_epoch, t;
        double inflation = 1;			// Change of scale
        double Rmov[][] = null;
        double u[] = null;

        if (!ready) compute();
        new_epoch = new_frame.epoch;
        if(DEBUG) System.out.println("....Astropos convert from " 
                + this.getFrame() + ",Ep=" + this.epoch + " to " 
                + new_frame + ",Ep=" + new_epoch);

        /* Verify first if frames identical -- then it's just changing Epoch */
        if (this.frame.equals(new_frame)) {
            if(DEBUG) System.out.println("....(identical frames)");
            if (new_epoch != this.epoch)
                toEpoch(new_epoch);
            return;
        }

        // Move via ICRS
        if(DEBUG) System.out.println("....Astropos.convert via ICRS:  "
                + this.frame + " => ICRS => " + new_frame);

        // Use a 6-vector for the conversions
        u = new double[6];
        u[0] = x;  u[1] = y;  u[2] = z;
        u[3] = xd; u[4] = yd; u[5] = zd;
        if(DEBUG) System.out.println(Coo.toString(".... convert(1): ", u));

        // FK4 is a problem: must move to Epoch+Equinox B1950, 
        //    then convert pos+pm to Equinox+Epoch J2000.

        /* Apply the change of epoch on the original frame... */
        t = this.frame.base_epoch - this.epoch;
        if (t!=0) {
            if(DEBUG) System.out.println("     Change epoch: " + this.epoch
                    + " to " + this.frame.base_epoch + " on " + this.frame);
            u[0] += u[3] * t;
            u[1] += u[4] * t;
            u[2] += u[5] * t;
            inflation *= Coo.normalize(u);
            if(DEBUG) System.out.println(Coo.toString(".... base_ep(2): ", u));
        }
        // this.dump("  (1). ");

        /* Convert to ICRS, J2000 */
        this.frame.toICRS(u);	// now for Epoch+Equinox=J2000 */
        if(DEBUG) System.out.println(Coo.toString(".... in.ICRS(3): ", u));

        /* Convert now to what's asked */
        new_frame.fromICRS(u);		// now in 'new' frame
        if(DEBUG) System.out.println(Coo.toString(".... baseNew(4): ", u));

        /* Apply the change of epoch on the final frame... */
        t = new_epoch - new_frame.base_epoch;
        if (t!=0) {
            if(DEBUG) System.out.println("     Change epoch: " 
                    + new_frame.base_epoch + " to " 
                    + new_epoch + " on " + new_frame);
            u[0] += u[3] * t;
            u[1] += u[4] * t;
            u[2] += u[5] * t;
            inflation *= Coo.normalize(u);
            if(DEBUG) System.out.println(Coo.toString(".... finalEp(5): ", u));
        }
        // this.dump("  (2). ");

        // Propagate the errors
        if (cov != null) {
            Coo tcoo = new Coo(u[0], u[1], u[2]);
            propagate_error(cov, new_epoch - epoch);
            Rmov = this.moveMatrix(tcoo);	// Matrix for rotation of errors
        }
        // this.dump("  (3). ");

        // Copy the final result
        if(DEBUG) System.out.println(Coo.toString(".... convEnd(6): ", u));
        this.lon = this.lat = 0./0.;	// Actual angles yet computed
        this.x  = u[0];
        this.y  = u[1];
        this.z  = u[2];
        this.xd = u[3];
        this.yd = u[4];
        this.zd = u[5];
        this.epoch = new_epoch;
        this.frame = new_frame;
        // this.dump("  (4). ");
        inflation *= this.normalize();

        // Propagate parallax change...
        if(DEBUG) System.out.println(".... Total inflation=" + inflation);
        this.plx   /= inflation;
        this.e_plx /= inflation;
        // this.dump("  res. ");

        // rotate the variance
        if (Rmov != null) 
            rotate_cov(Rmov, cov);

        // recompute the Rxx
        this.localMatrix(R);
    }

    /**
     * Express a celestial position in another Coordinate Frame
     * @param coo	another frame to which convert the coordinates.
     * "<tt>this</tt>" object is modified and will contain the new coordinates
     * ----- NOT NECESSARY
    public void convertFrom(Astropos coo) {
      Astroframe new_frame = this.frame;
      double t = this.epoch - coo.epoch;
      int i, j;

	if (!ready) compute();
	if(DEBUG) System.out.println("....Astropos convert from " 
		+ coo.getFrame() +" to " + this.getFrame());

	// Copy the values
	super.set((Astrocoo)coo);
	this.formRA = coo.formRA;	// Is it really necessary ?
	this.specified = coo.specified;
	this.epocho = coo.epocho;
	this.plx = coo.plx;
	this.e_plx = coo.e_plx;
	this.rv = coo.rv;
	this.e_rv = coo.e_rv;
	this.xd = coo.xd; this.yd = coo.yd; this.zd = coo.zd;

	// Propagate the change of error ellipse
	if ((this.cov == null) && (coo.cov != null))
	    this.cov = new double[6][6];
	if (this.cov != null) {
	    for (i=0; i<6; i++) for (j=0; j<6; j++)
	       this.cov[i][j] = coo.cov[i][j];
	    propagate_error(this.cov, t);
	}

	this.convertTo(new_frame, true);
    }
     **/

}
