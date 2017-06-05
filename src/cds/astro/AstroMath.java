// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.astro;

/**
 * Trigonometric and a few other functions used in the astronomical context.
 * This class includes also 3x3 matrix manipulation.
 * Extracted from Class Coo
 * @author Francois Ochsenbein
 * @version 1.0: 20-Apr-2004
 * @version 1.1: 15-Jun-2008: elliptical integral of 1st kind.
 * @version 1.2: 03-Jun-2016: all 3 kinds of complete elliptical integrals
 *               (see also the "ellipse.pdf" documentation)
 */

public class AstroMath {
    public static final double[] powers = { 1., 1.e1, 1.e2, 1.e3,
        1.e4, 1.e5, 1.e6, 1.e7, 1.e8, 1.e9 };

    public static final double DEG = 180.0/Math.PI ;	// Degrees to radians
    public static final double DEG2 = DEG*DEG ;		// sq.deg in steradian
    public static final double ARCSEC = 3600.*DEG ;	// Arcsec to radians
    static final double ln10 = Math.log(10.) ;		// log10 to natural log
    static final double ellEPS = 5.e-16;			// Precision for ellptical integrals
    //public static boolean DEBUG=true;

    //  ===========================================================
    //		Trigonometry in Degrees
    //  ===========================================================

    /*  Static methods (functions) in Java are very close to C ones;
    	 they do not require any object instanciation.
    	 Typical example of static methods are in the Math class
    	 Note that the functions toDegrees and toRadians can be used
    	 in JDK1.2 -- we stick here strictly to JDK1.1
     */

    /**
     * Cosine when argument in degrees
     * @param x angle in degrees
     * @return	the cosine
     */
    public static final double cosd(double x) {
        return Math.cos( x/DEG );
    }

    /**
     * Sine  when argument in degrees
     * @param x angle in degrees
     * @return	the sine
     */
    public static final double sind(double x) {
        return Math.sin( x/DEG );
    }

    /**
     * Tangent  when argument in degrees
     * @param x angle in degrees
     * @return	the tan
     */
    public static final double tand(double x) {
        return Math.tan( x/DEG );
    }

    /**
     * sin-1 (inverse function of sine), gives argument in degrees
     * @param	x argument
     * @return	y value such that sin(y) = x
     */
    public static final double asind(double x) {
        return Math.asin(x)*DEG;
    }

    /**
     * tan-1 (inverse function of tangent), gives argument in degrees
     * @param x argument
     * @return	angle in degrees
     */
    public static final double atand(double x) {
        return Math.atan(x)*DEG;
    }

    /**
     * get the polar angle from 2-D cartesian coordinates
     * @param y cartesian y coordinate
     * @param x cartesian x coordinate
     * @return	polar angle in degrees
     */
    public static final double atan2d(double y,double x) {
        return Math.atan2(y,x)*DEG;
    }

    //  ===========================================================
    //		Hyperbolic Functions (not in Math ??)
    //  ===========================================================

    /**
     * Hyperbolic cosine cosh = (exp(x) + exp(-x))/2
     * @param  x argument
     * @return	corresponding hyperbolic cosine (>= 1)
     */
    public static final double cosh (double x) {
        double ex ;
        ex = Math.exp(x) ;
        return 0.5 * (ex + 1./ex) ;
    }

    /**
     * Hyperbolic tangent = (exp(x)-exp(-x))/(exp(x)+exp(-x))
     * @param x argument
     * @return	corresponding hyperbolic tangent (in range ]-1, 1[)
     */
    public static final double tanh (double x) {
        double ex, ex1 ;
        ex = Math.exp(x) ;
        ex1 = 1./ex ;
        return (ex - ex1) / (ex + ex1) ;
    }

    /**
     * tanh-1 (inverse function of tanh)
     * @param x argument, in range ]-1, 1[ (NaN returned otherwise)
     * @return	corresponding hyperbolic inverse tangent
     */
    public static final double atanh (double x) {
        return (0.5*Math.log((1.+(x))/(1.-(x))));
    }

    //  ===========================================================
    //		sin(x)/x and Inverse
    //  ===========================================================

    /**
     * Function sinc(x) = sin(x)/x
     * @param x argument (radians)
     * @return	corresponding value
     */
    public static final double sinc(double x) {
        double ax, y;
        ax = Math.abs(x);
        if (ax <= 1.e-4) {
            ax *= ax;
            y = 1 - ax*(1.0-ax/20.0)/6.0;
        }
        else y = Math.sin(ax)/ax;
        return y;
    }

    /** Reciprocal */
    /**
     * Function asinc(x), inverse function of sinc
     * @param	x argument
     * @return	y such that sinc(y) = x
     */
    public static final double asinc(double x) {
        double ax,y;
        ax = Math.abs(x);
        if( ax <= 1.e-4) {
            ax *= ax;
            y = 1.0 + ax*(1.0 + ax*(9.0/20.0))/6.0;
        }
        else y = Math.asin(ax)/ax;
        return (y);
    }

    //  ===========================================================
    //		Exponential/Logarithm base 10
    //  ===========================================================

    /**
     * Compute just 10<sup>n</sup>
     * @param	n Power to which to compute the value
     * @return	10<sup>n</sup>
     */
    public static final double dexp(int n) {
        int i = n;
        int m = powers.length-1;
        double x = 1;
        boolean inv = false;
        if (n < 0) { inv = true; i = -n; }
        while (i > m) { x *= powers[m]; i -= m; }
        x *= powers[i];
        if (inv) x = 1./x ;
        return(x);
    }


    /**
     * Compute just 10<sup>x</sup>
     * @param	x Power to which to compute the value
     * @return	10<sup>x</sup>
     */
    public static final double dexp(double x) {
        return(Math.exp(x*ln10));
    }

    /**
     * Compute the log base 10
     * @param	x Number (positive)
     * @return	log<sub>10</sub>(x)
     */
    public static final double log(double x) {
        return(Math.log(x)/ln10);
    }

    //  ===========================================================
    //		Elliptical Integrals
    //		See http://dlmf.nist.gov/19.8
    //  ===========================================================

    /**
     * Computation of complete elliptic integral of first kind.
     * K(a,b) = Integral{0,&pi;/2} du/sqrt(a<sup>2</sup>cos<sup>2</sup>u+b<sup>2</sup>sin<sup>2</sup>u).
     * <br>Computed with arithmetico-geometrical mean M(a,b) = common limit of
     * a<sub>n+1</sub>=(a<sub>n</sub>+b<sub>n</sub>)/2,
     * b<sub>n+1</sub>=sqrt(a<sub>n</sub>*b<sub>n</sub>).<br>
     * The arithmetico-geometrical mean M(a,b) is given by (Gauss):
     * 1/M(a,b) = (2/&pi;) K(a,b)<br>
     * M(a,b) being the common limit of suites 
     * a<sub>n+1</sub>=(a<sub>n</sub>+b<sub>n</sub>)/2
     * b<sub>n+1</sub>=sqrt(a<sub>n</sub>.b<sub>n</sub>)
     * @param a (positive)
     * @param b (positive)
     * @return value of the elliptic integral function
     **/
    public static final double ell1(double a, double b) {
        double a0=a, b0=b, a1, b1; int i;
        if(a<b) { a0=b; b0=a; }	// a_n suite decreasing
        for(i=50; (--i>=0) && ((a0-b0)>ellEPS); a0=a1, b0=b1) {
            a1 = (a0+b0)/2.;
            b1 = Math.sqrt(a0*b0);
        }
        /* System.err.println("#...AstroMath.ell1(" + a + "," + b + "): " 
	                  + (50-i) + " iterations"); */
        if(i<0) System.err.println(
                "#+++AstroMath.ell1(" + a + "," + b + ") did not converge!");
        /* if (DEBUG) System.out.println("#...ell1(" + a + "," + b + " => "
		+ (Math.PI/(a0+b0))); */
        return(Math.PI/(a0+b0));
    }
    public static final double ell1(double m) {
        /**
         * 1-argument complete elliptical K(m) = Integral{0,&pi;/2} du/sqrt(1-m.sin<sup>2</sup>u)
         * @param m argument, in range [0,1[.
         * @return value of the elliptic integral function K(m)
         **/
        return(ell1(1., Math.sqrt(1.-m)));
    }

    /**
     * Computation of complete elliptic integral of second kind.
     * J(a,b) = Integral{0,&pi;/2} du.sqrt(a<sup>2</sup>cos<sup>2</sup>u+b<sup>2</sup>sin<sup>2</sup>u)
     * <br>Computed with property that 
     * E(a,b)/K(a,b) = (a<sup>2</sup>+a<sup>2</sup>)/2 
     *      - Sum[n=1^Infinity] 2<sup>n-1</sup>c<sub>n</sub><sup>2</sup>} where
     * <br> c<sub>n+1</sub>=(a<sub>n</sub>-b<sub>n</sub>)/2.
     * @param a (positive)
     * @param b (positive)
     * @return the integral
     **/
    public static final double ell2(double a, double b) {
        double a0=a, b0=b, a1, b1, S, d2, f2=1.; int i;
        if(a<b) { a0=b; b0=a; }	// a_n suite decreasing
        S = (a0*a0+b0*b0)/2.;
        for(i=50; (--i>=0) && ((a0-b0)>ellEPS); a0=a1, b0=b1) {
            d2 = (a0-b0)/2.; S -= f2*d2*d2; f2 *= 2.;
            a1 = (a0+b0)/2.; 
            b1 = Math.sqrt(a0*b0);
        }
        /* System.err.println("#...AstroMath.ell2(" + a + "," + b + "): " 
	                  + (50-i) + " iterations"); */
        if(i<0) System.err.println(
                "#+++AstroMath.ell2(" + a + "," + b + ") did not converge!");
        return(Math.PI*S/(a0+b0));
    }
    /**
     * Computation of complete elliptic integral of second kind.
     * E(m) = Integral{0,&pi;/2} du.sqrt(1-m.sin<sup>2</sup>u)
     * @param m argument, in range [0,1[
     * @return value of the elliptic integral function E(m)
     **/
    public static final double ell2(double m) {
        return(ell2(1., Math.sqrt(1.-m))); /*
	double a0, b0, a1, b1, S, d2, f2; int i;
	a0 = 1.; b0 = Math.sqrt(1.-m);
	S = 1. - m/2.; f2 = 0.25;
	for(i=50; (--i>=0) && (Math.abs(a0-b0)>ellEPS); a0=a1, b0=b1) {
	    S -= f2*(a0-b0)*(a0-b0); f2 *= 2.;
	    a1 = (a0+b0)/2.; 
	    b1 = Math.sqrt(a0*b0);
	}
	if(i<0) System.err.println(
	    "#+++AstroMath.ell1(" + m + ") did not converge!");
	return(Math.PI*S/(a0+b0)); */
    }

    /**
     * Computation of complete elliptic integral of third kind.
     * Pi(n,m) = Integral{0,&pi;/2} du(1-n.sin<sup>2</sup>u).sqrt(1-m.sin<sup>2</sup>u)]
     * See http://functions.wolfram.com/EllipticIntegrals/
     * http://scitation.aip.org/content/aip/journal/jap/34/9/10.1063/1.1729771
     * @param n [0,1[
     * @param m [0,1[
     * @return the integral
     **/
    public static final double ell3(double n, double m) {
        double a0, b0, a1, b1, zeta, delta, eps0, eps1; int i;
        a0 = 1.; b0 = Math.sqrt(1.-m);
        delta = (1.-n)/b0;
        eps0  = n/(1.-n);
        zeta  = 0;
        for(i=50; (--i>=0) && ((a0-b0)>ellEPS||Math.abs(delta-1.)>ellEPS); 
                a0=a1, b0=b1, eps0=eps1) {
            eps1 = (delta*eps0 + zeta)/(1.+delta);
            zeta = (eps0+zeta)/2.;
            a1 = (a0+b0)/2.; b1 = Math.sqrt(a0*b0);
            delta = 0.25*b1/a1*(2.+delta+1./delta);
        }
        /* System.err.println("#...AstroMath.ell3(" + n + "," + m + "): " 
	                  + (50-i) + " iterations"); */
        if(i<0) System.err.println(
                "#+++AstroMath.ell3(" + n + "," + m + ") did not converge!");
        return(Math.PI*(1.+zeta)/(a0+b0));
    }

    //  ===========================================================
    //		Matrices and Vectors 3x3
    //  ===========================================================

    /**
     * 3-Matrices Products
     * @param  A 3x3 matrix
     * @param  B 3x3 matrix
     * @return R    = A * B
     */
    public static final double[][] m3p(double A[][], double B[][]) {
        double[][] R = new double[3][3];
        int i, j;
        for (i = 0; i < 3; i++) for (j = 0; j < 3; j++)
            R[i][j] = A[i][0] * B[0][j] + A[i][1] * B[1][j] + A[i][2] * B[2][j];
        return (R);
    }

    /** Transposed of a Matrix
     * @param  A input matric
     * @return R  = <sup>t</sup>(A)
     */
    public static final double[][] m3t(double A[][]) {
        double R[][] = new double[3][3];
        int i, j;
        for (i = 0; i < 3; i++) for (j = 0; j < 3; j++)
            R[i][j] = A[j][i];
        return (R);
    }

}
