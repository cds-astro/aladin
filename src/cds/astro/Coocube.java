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

package cds.astro ;

import java.io.*;
import java.util.*;
//import cds.astro.*;

/**
 * The routines provided in this module define a "coordinate" as
 * a <em>face</em> number (between 1 and 6), and two <em>(X,Y)</em> values 
 * in the range [-1,1[ representing the projection of a direction onto
 * a cube. The <em>face</em> numbers, and the axis orientations, are:<BR>
 * <PRE>
 * <b>1 = +z</b> X=+y Y=-x &nbsp;(North Pole)
 * <b>2 = +y</b> X=-z Y=-x &nbsp;(azimuth +90)
 * <b>3 = +x</b> X=+y Y=+z &nbsp;(azimuth 0)
 * <b>4 = -x</b> X=-z Y=-y &nbsp;(azimuth 180)
 * <b>5 = -y</b> X=+x Y=+z &nbsp;(azimuth -90)
 * <b>6 = -z</b> X=+x Y=-y &nbsp;(South Pole)
 *           +---------+
 *           |    ^    |
 *           |    |x   |
 *           |&lt;---+    |
 *           |    1    |
 * +---------+---------+---------+---------+
 * |    ^    |    ^    |         |         |
 * |    |    |    |    |         |         |
 * |   5+---&gt;|  3 +---&gt;|   2+---&gt;|   4+---&gt;|
 * |      x  |      x  |   x|    |   x|    |
 * |         |         |    v    |    v    |
 * +---------+---------+---------+---------+
 *           |    ^    |
 *           |    |x   |
 *           |&lt;---+    |
 *           |    6    |
 *           +---------+
 *
 *</PRE>
 * These coordinates can be used to address accurately a point with
 * 2 floating-point numbers associated to a 3-bit face number
 * <P>
 * @author	Francois Ochsenbein
 * @version 1.0	16-sep-2002
**/ 

public class Coocube extends Coo {

  /** 
   * The face number in range 1 to 6 -- zero for undefined. 
  **/
  public byte face;
  /** 
   * The position on the face, a number between -1 and 1.  
  **/
  public double X, Y;

  /* Variables common to the Class: all Definitions */
  /* Definitions of the directions of the (X,Y) axises on 
     each face of the cube, with 
     1=+z, 2=+y, 3=+x, 4=-x, 5=-y, 6=-z
  */
  static private final byte[] axis6 = {
     0,2,6,2,6,3,3,0 ,		// 01..06 : Direction of X axis
     0,4,4,1,5,1,5,0 ,		// 09..14 : Direction of Y axis
     0,1,2,3,4,5,6,0 		// 17..22 : z = original direction
  };
  static private Editing edition = new Editing();

 /*==================================================================*
 		Conversion (xyz) <--> (nXY)
  *==================================================================*/

  /** Compute the face number from the unit vector
   * @param	u unit vector (vector of 3 direction cosines)
   * @return	the face number in range 1..6 (0 if error)
  **/
    public static final int face(double u[]) {
      int s[] = { 0, 1, 2 };
      int n;
	/* Sort the components of the unit vector to guess the face number */
	if(u[1] < u[0])	{ s[0] = 1; s[1] = 0; }
	if(u[2] < u[s[0]])	{ s[2] = s[0]; s[0] = 2; }
	if(u[s[2]] < u[s[1]])	{ n = s[2]; s[2] = s[1]; s[1] = n; }
	/* Derive the face number */
	if(u[s[2]] > -u[s[0]]) n = 3^s[2];	// 0 1 2 ==> 3 2 1
	else			n = 4^s[0];	// 0 1 2 ==> 4 5 6
	// Special case when it's not a unit vector (minimum 1/sqrt(3) = 0.577)
	if((u[s[0]] >= -0.577) && (u[s[2]] <= 0.577)) n=0;
	return(n);
    }

  /** Compute the (face, X, Y)
   * @param	u the Direction Cosines
   * @param	XY the (X,Y) components 
   * @return	the face number in range 1..6 (0 if error)
  **/
    public static final int setXY(double[] u, double [] XY) {
      int face = face(u) ;
      double z;
      int n;
	/* Component perpendicular to a face number (z) is
	    3^face    for faces 1,2,3	(gives z,y,x)
	   -4^face    for faces 4,5,6	(gives -x,-y,-z)
	*/
	z = (face&4) == 0 ? u[3^face] : -u[4^face];	// Denominator
	if (z < 0.577)	{				// ERROR, not normal !!
	    XY[0] = XY[1] = 0;
	    return(0);
	}
	n     = axis6[face];  
	XY[0] = (n&4) == 0 ? u[3^n] : -u[4^n];	// X-axis
	n     = axis6[8|face];  
	XY[1] = (n&4) == 0 ? u[3^n] : -u[4^n];	// Y-axis
	XY[0] /= z;
	XY[1] /= z;
	return(face);
    }
  
   /** Transformation (face + Projections) --&gt; unit vector
    * @param face the face number (1 to 6)
    * @param	X X value of the projection on the face n
    * @param	Y Y value of the projection on the face n
    * @param	u the unit vector (modified)
    * @return	the face number
   **/
    public static final int setUvec(int face, double X, double Y, double[] u) {
      double factor;
      int n;

	if ((face<1) || (face>6)) { u[0] = u[1] = u[2] = 0; return(0); }

	/* Compute Factor to convert Projections into Direction Cosines */
	factor = 1./Math.sqrt(1. + X*X + Y*Y) ;
	n = face;
	if ((n&4) == 0) u[3^n] =  factor;
	else            u[4^n] = -factor;

	/* Compute the Direction Cosine from X */
	n = axis6[face];
	if ((n&4) == 0) u[3^n] =  X * factor;
	else            u[4^n] = -X * factor;

	/* Compute the Direction Cosine from Y */
	n = axis6[8|face];
	if ((n&4) == 0) u[3^n] =  Y * factor;
	else            u[4^n] = -Y * factor;

	return(face);
    }


 /*==================================================================*
 			Assign the Coocube
  *==================================================================*/

  /** Compute the Coocube coordinates from the Direction Cosines
   * @param	u the Direction Cosines
   * @return	the face number in range 1..6 (0 if error)
  **/
    public final void set(double[] u) {
      double[] XY = new double[2];
	super.set(u[0], u[1], u[2]);
	face = (byte)setXY(u, XY);
	X = XY[0]; Y = XY[1];
    }

  /** Compute the Coocube coordinates
   * @param	coo coordinates of a point
   * @return	the face number in range 1..6 (0 if error)
  **/
    public final void set(Coo coo) {
      double[] u = { coo.x, coo.y, coo.z };
      	set(u);
    }

  /** Compute the Coocube from its (XY) components
   * @param 	face the face number between 1 and 6
   *		face is set to 0 in case of error.
   * @param 	X  x position on the face
   * @param		Y  y position on the face
   * @return	the face number in range 1..6 (0 if error)
  **/
    public final void set(int face, double X, double Y) {
      double[] u = new double[3];
      	if ((face < 1) || (face > 6)) 
	     u[0] = u[1] = u[2] = 0;
	else setUvec (face, X, Y, u);
	set(u);
    }

 /*==================================================================*
 			Constructors
  *==================================================================*/

  /** 
   * Define the default undefined Coocube 
  **/
    public Coocube() {
        face = 0;
        X = 0; Y = 0;
    }

  /** 
   * Define the Coocube from a Coordinate
  **/
    public Coocube(Coo coo) {
        this.set(coo);
    }

  /** 
   * Define the Coocube from its components
   * @param 	face the face number between 1 and 6
   *		face is set to 0 in case of error.
   * @param 	X  x position on the face
   * @param 	Y  y position on the face
  **/
    public Coocube(int face, double X, double Y) {
        this.set(face, X, Y);
    }

 /*==================================================================*
 			Edition
  *==================================================================*/

  /** 
   * Edit the Coocube value as <tt>f:&plusmn;X&plusmn;Y</tt>
   * @param	buf buffer for the edition
   * @param	ndec number of decimals for edition 
   *		(a value of 6 corresponds to an accuracy of 0.3arcsec)
   * @return	the face number in range 1..6 (0 if error)
  **/
    public StringBuffer edit(StringBuffer buf, int ndec) {
	buf.append(face);
	buf.append(":");
	if (face == 0) return(buf);
	edition.editDecimal(buf, X, 2, ndec, Editing.SIGN_EDIT);
	edition.editDecimal(buf, Y, 2, ndec, Editing.SIGN_EDIT);
	return(buf);
    }

  /** 
   * Default Edition of the Coocube values
   * @return	the string equivalent of the Coocube
   * the angles in degrees.
  **/
    public String toString() {
      StringBuffer b = new StringBuffer(20) ;
	edit(b, 6);
	b.append("(");
	super.edit(b, 4);	// 4 decimals corespond to 0.3arcsec
	b.append(")");
     	return(b.toString());	// Buffer converted to String
    }
}
