// Copyright 2010 - UDS/CNRS
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
 */

public class AstroMath {
   public static final double[] powers = { 1., 1.e1, 1.e2, 1.e3,
     1.e4, 1.e5, 1.e6, 1.e7, 1.e8, 1.e9 };

   public static final double DEG = 180.0/Math.PI ;	// Degrees to radians
   public static final double ARCSEC = 3600.*DEG ;	// Arcsec to radians
   static final double ln10 = Math.log(10.) ;		// log10 to natural log

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
    			 y = 1.0 + ax*(6.0 + ax*(9.0/20.0))/6.0;
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
