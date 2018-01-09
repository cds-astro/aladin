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

package cds.astro;

import java.io.*;
import java.util.*;
import java.text.*; // for parseException
// import cds.astro.*;

/*
 * @author	Francois Ochsenbein
 * @author      Benoit Baranne (Stagiaire UTBM)
 * @version 1.0	  02-Feb-2007
 * -------------
*/


/**
 * The <em>QboxNumber</em> is just a number representing a <em>cell</em>
 * of the celestial sphere. This number is used in {@link Qbox} and
 * {@link QboxIndex} classes.
 *
 * <P>
 * A <em>qbox</em> number is formed by the values of the projections
 * <em>(X,Y)</em> of the directions on the 6 faces of a cube numbered
 *      1&#160;(<I>z=1</I>),  2&#160;(<I>y=1</I>),  3&#160;(<I>x=1</I>),
 * 	4&#160;(<I>x=-1</I>), 5&#160;(<I>y=-1</I>) and 6&#160;(<I>z=-1</I>).
 * <P>
 * The faces and orientations are defined as:
 * <FONT COLOR='Blue'>
 * <PRE>
 * 
 *                  +---------+
 *                  |    ^    |
 *                  |    |    |
 *                  |   1+---&gt;|
 *                  |      x  |
 *                  |         |
 *        +---------+---------+---------+---------+
 *        |    ^    |    ^    |         |         |
 *        |    |    |    |    |         |         |
 *        |   5+---&gt;|  3 +---&gt;|   2+---&gt;|   4+---&gt;|
 *        |      x  |      x  |   x|    |   x|    |
 *        |         |         |    v    |    v    |
 *        +---------+---------+---------+---------+
 *                  |    ^    |
 *                  |    |x   |
 *                  |&lt;---+    |
 *                  |    6    |
 *                  |         |
 *                  +---------+
 * </PRE>
 * </FONT>
 * Each cube face is divided into 4 areas; each area is further
 * divided into four pieces, etc.
 * The number of valid qboxes is <b>6&times;4<sup>N</sup></b>:
 * <PRE>
 *   Level    Lower #      qboxes   qbox_size
 *      0           9           6     83&ordm;
 *      1          36          24     41&ordm;
 *      2         144          96     20&ordm;
 *      3         576         384     10&ordm;
 *      4        2304        1536      5&ordm;
 *      5        9216        6144      2&ordm;30'
 *      6       36864       24576      1&ordm;15'
 *      7      147456       98304        40'
 *      8      589824      393216        20'
 *      9     2359296     1572864        10'
 *     10     9437184     6291456         5'
 *     11    37748736    25165824         2'30"
 *     12   150994944   100663296         1'15"
 * </PRE>
 * where level is a number which can be changed at any time using
 * <b>setLevel</b>(<EM>level</EM>) method.
 * <P>
 * The present implementation assumes that <EM>level<I><=12</I></EM>,
 * corresponding to a maximum of 28 bits in the qbox number.
 * <P>
 * For a level 6, the qbox number is a short integer with bits
 * (from left to right)
 * <FONT COLOR='Blue'><TT>1ppp xyxy xyxy xyxy</TT></FONT>.
 * The leftmost `1' bit allows the recognition of the level;
 * bits <TT>p</TT> represent the face number (1 to 6),
 * <TT>x</TT> and <TT>y</TT> the position, expressed with <EM>level</EM> bits,
 * along the axises defined above for the face; going one level down
 * in the hierarchy is therefore just a shift of 2 bits and
 * updating the 2 rightmost bits.
 **/

public class QboxNumber implements Serializable {

    /** Level of the chosen Qboxes (for creation), between 0 and 12 */
    public final int level;
    /* Variables common to the Class: all Definitions */
    static private final double PIO2 = Math.PI / 2.;
    static final boolean DEBUG = true;
    static final int ANY = 0x80000000; // The 'ANY' status
    static final int BOX = 0x7fffffff; // The ~ANY  bits
    static private final int MAXLEVEL = 12;
    /** Default level used in {@link #QboxNumber()}, 
     * (default 9 or size about 10') */
    static public int default_level = 9; // Default level: 1.6million of 10x10'
    static private final char[] oct = "01234567".toCharArray();
    static private final double DEG = 180. / Math.PI;
    static final double[] MAXRAD = /* Max radius of Qbox  */ {
       54.735610317, /* Level  0:   radius  54.73561/ 45.00000    */
       30.361193405, /* Level  1:   radius  30.36119/ 21.79319    */
       16.403086517, /* Level  2:   radius  16.40309/  9.61290    */
    	8.639594386, /* Level  3:   radius   8.63959/  4.55775    */
    	4.449359566, /* Level  4:   radius   4.44936/  2.21341    */
    	2.259674852, /* Level  5:   radius   2.25967/  1.08995    */
    	1.138910823, /* Level  6:   radius   1.13891/  0.54074    */
    	0.571763982, /* Level  7:   radius   0.57176/  0.26930    */
    	0.286464128, /* Level  8:   radius   0.28646/  0.13438    */
    	0.143378220, /* Level  9:   radius   0.14338/  0.06713    */
    	0.071725727, /* Level 10:   radius   0.07173/  0.03355    */
    	0.035872027, /* Level 11:   radius   0.03587/  0.01677    */
    	0.017938306  /* Level 12:   radius   0.01794/  0.00838    */
    };
    static final double[] SINRAD = /* Sine of above radii */ {
    	0.8164965809277260, /* Level  0: Surface  6.875e+03 / 6.875e+03  */
    	0.5054494651244236, /* Level  1: Surface  1.719e+03 / 1.719e+03  */
    	0.2823931345695149, /* Level  2: Surface  4.170e+02 / 4.825e+02  */
    	0.1502185901567493, /* Level  3: Surface  9.780e+01 / 1.250e+02  */
    	0.0775779474805375, /* Level  4: Surface  2.345e+01 / 3.154e+01  */
    	0.0394285430318791, /* Level  5: Surface  5.729e+00 / 7.904e+00  */
    	0.0198764347423619, /* Level  6: Surface  1.415e+00 / 1.977e+00  */
    	0.0099789983974146, /* Level  7: Surface  3.517e-01 / 4.944e-01  */
    	0.0049997213877594, /* Level  8: Surface  8.766e-02 / 1.236e-01  */
    	0.0025024194021835, /* Level  9: Surface  2.188e-02 / 3.090e-02  */
    	0.0012518497600656, /* Level 10: Surface  5.466e-03 / 7.725e-03  */
    	0.0006260849416742, /* Level 11: Surface  1.366e-03 / 1.931e-03  */
    	0.0003130824920569  /* Level 12: Surface  3.415e-04 / 4.828e-04  */
    };
    static final double[] MINRAD = /* Min radius of Qbox  */ {
       45.000000000000, /* Level  0:   radius  54.73561/ 45.00000    */
       21.793189128656, /* Level  1:   radius  30.36119/ 21.79319    */
    	9.612900642866, /* Level  2:   radius  16.40309/  9.61290    */
    	4.557754524215, /* Level  3:   radius   8.63959/  4.55775    */
    	2.213411995038, /* Level  4:   radius   4.44936/  2.21341    */
    	1.089949904684, /* Level  5:   radius   2.25967/  1.08995    */
    	0.540737084594, /* Level  6:   radius   1.13891/  0.54074    */
    	0.269302913463, /* Level  7:   radius   0.57176/  0.26930    */
    	0.134384274203, /* Level  8:   radius   0.28646/  0.13438    */
    	0.067125244187, /* Level  9:   radius   0.14338/  0.06713    */
    	0.033545886680, /* Level 10:   radius   0.07173/  0.03355    */
    	0.016768757962, /* Level 11:   radius   0.03587/  0.01677    */
    	0.008383332446  /* Level 12:   radius   0.01794/  0.00838    */
    };
    static private final boolean[] move_x = /* for adjacent boxes */ {
	true, false, false, true 	// 0=+x, 1=+y, 2=-y, 3=-x
    };
    /* List of adjacent faces, when moving toward one of +x +y -y -x
     * The face is flagged by 8 when a rotation of the axes occurs
     * at the border (always a rotation of +pi/2)
     */
    static private final byte[][] adj_face = {
	{0,  2,8|6,8|2,  6,  3,8|3,0},	// ... when x >= 1 (0)
	{0,8|4,  4,  1,8|5,8|1,  5,0},	// ... when y >= 1 (1)
	{0,  3,8|3,8|6,  2,  6,8|2,0},	// ... when y < -1 (2)
	{0,8|5,  1,  5,8|1,8|4,  4,0}	// ... when x < -1 (3)
    };
    static private final byte[][] adj_move = {
	{ 2, 3, 4, 5 },			// (0) toward +x
	{ 1, 4, 3, 6 },			// (1) toward +y 
	{ 5, 0, 7, 2 },			// (2) toward -y 
	{ 6, 7, 0, 1 }			// (3) toward -x
    };
    /* Neighbour coordinate indices for the computation of corners
     * (4=Xo, 5=Yo) */
    static private final byte[] nearby_xy = {
	025, 043, 041, 005, 023, 021, 001, 003 
    };

    /*==================================================================*
      			Initialisations
     *==================================================================*/

    /** 
     * Change the default level.
     * It is a change of the <em>static</em> <b>default_level</b> variable.
     * This means that the creation of a class from the default
     * {@link #QboxNumber()} will use the specified level.
     * @param	lev the default level, between 0 and 12.
     * 		Values outside the [0..12] range are ignored.
     * @return	the previously defined default level
     **/
    public static final int setLevel(int lev) {
    	int old_lev = default_level;
    	if ( (lev >= 0) && (lev <= 12)) 
    	    default_level = lev;
    	return (old_lev);
    }

    /*==================================================================*
      			Vectorial basic operations
     *==================================================================*/

    /** Compute the distance between 2 vectors
     * @param	v1 first vector
     * @param   v2 second vector
     * @return	the squared distance ||v1-v2||^2
     **/
    static double dist2(double[] v1, double[] v2) {
    	double r, d;
    	r = v1[0] - v2[0];
    	r *= r;
    	d = v1[1] - v2[1];
    	d *= d;
    	r += d;
    	d = v1[2] - v2[2];
    	d *= d;
    	r += d;
    	return (r);
    }

    /** Compute the dot-product (scalar product)
     * @param	v1 first vector
     * @param   v2 second vector
     * @return	the dot-product
     **/
    static double dotprod(double[] v1, double[] v2) {
    	return (v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]);
    }

    /** Compute the square norm of a vector
     * @param	v1 the vector
     * @return	the squared norm
     **/
    static double norm2(double[] v1) {
    	return (dotprod(v1, v1));
    }

    /** Compute the vectorial product
     * @param	v1 first vector
     * @param   v2 second vector
     * @param   r the returned resulting vector
     **/
    static void vecprod(double[] v1, double[] v2, double[] r) {
    	r[0] = v1[1] * v2[2] - v1[2] * v2[1];
    	r[1] = v1[2] * v2[0] - v1[0] * v2[2];
    	r[2] = v1[0] * v2[1] - v1[1] * v2[0];
    }

    /** Compute the determinant
     * @param	v1 first vector
     * @param   v2 second vector
     * @param	v3 third vector
     * @return	the determinant = (v1^v2).v3 = (v2^v3).v1 = (v3^v1).v2
     **/
    static double det(double[] v1, double[] v2, double[] v3) {
    	double[] v = new double[3];
    	vecprod(v1, v2, v);
    	return (dotprod(v, v3));
    }

    /*==================================================================*
      			Internal Routines
     *==================================================================*/

    /** Compute the Center of a Qbox
     * @param	qboxno the qbox number
     * @param	XY the position of the center
     * @return	the cube face number in range <b>[1..6]</b> (0 if error)
     **/
    private static final int center_(int qboxno, double[] XY) {
    	int b = qboxno & BOX;
    	int x = 0, y = 0;
    	double f;
    	int qlev;
    	if (b < 9) {		// Whole sphere
    	    XY[0] = 0.;
    	    XY[1] = 0.;
    	    return (0);
    	}
    	for (qlev = 0; (b & (~0xf)) != 0; qlev++) {
    	    y |= (b & 1) << qlev;
    	    b >>>= 1;
    	    x |= (b & 1) << qlev;
    	    b >>>= 1;
    	}
    	/* Now qlev is the level of this Qbox, b is the face number */
    	f = (1 << qlev); // 2**qlev, The maximal value for this level

    	XY[0] = Math.tan( ( ( (x + 0.5) / f) - 0.5) * PIO2);
    	XY[1] = Math.tan( ( ( (y + 0.5) / f) - 0.5) * PIO2);
    	return(b&7);
    }

    /** Compute the Corners of a Qbox from qbox number
     * @param	qboxno the qbox number
     * @param	blur the (xy) position of Bottom-Left-Up-Right points
     * @return	the cube face number in range 1..6 (0 if error)
     **/
    static final int corners(int qboxno, double[] blur) {
    	int b = qboxno & BOX;
    	int x = 0, y = 0;
    	int qlev;
    	double f;
    	if (b == 0) {
    		return (0); // Whole Sky
    	}
    	/* Compute the level, and the (x,y) position in this level */
    	for (qlev = 0; (b & (~15)) != 0; qlev++) {
    	    y |= (b & 1) << qlev;
    	    b >>= 1;
    	    x |= (b & 1) << qlev;
    	    b >>= 1;
    	}
    	f = (1 << qlev);
    	/* Lower-left corner */
    	blur[0] = Math.tan( ( (x / f) - 0.5) * PIO2);
    	blur[1] = Math.tan( ( (y / f) - 0.5) * PIO2);
    	x++;
    	y++;
    	blur[2] = Math.tan( ( (x / f) - 0.5) * PIO2);
    	blur[3] = Math.tan( ( (y / f) - 0.5) * PIO2);

    	return (b & 7);
    }

    /** Compute the 4 vectors representing the corners of a Qbox.
     * @param	qboxno the qbox number
     * @param	ucorn a [4][3] array
     * @return	the cube face number in range 1..6 (0 if error)
     **/
    static final int ucorners(int qboxno, double[][] ucorn) {
    	double[] XY = new double[4];
    	int face;
    	face = corners(qboxno, XY);
    	if (face == 0) 
    	    return (face);
    	Coocube.setUvec(face, XY[0], XY[1], ucorn[0]);
    	Coocube.setUvec(face, XY[2], XY[1], ucorn[1]);
    	Coocube.setUvec(face, XY[2], XY[3], ucorn[2]);
    	Coocube.setUvec(face, XY[0], XY[3], ucorn[3]);
    	return(face);
    }

    /*==================================================================*
      			Which level, from resolution
     *==================================================================*/

    /** 
     * Estimate level from resolution.
     * The level is such that any source having a diameter up to 
     * <em>arcmin</em> is not larger than a Qbox, i.e. requires to look only
     * at the target Qbox and its neighbours (see {@link #nearby})
     * @param  arcmin  resolution, in arcmin.
     * @return the suggested level, in range <b>[0..12]</b>
     **/
    static final int level(double arcmin) {
	int i = MINRAD.length;
	double rad = arcmin/60.;
	while (--i>0) {
	    if (MINRAD[i] > rad) 
		return(i);
	}
	return(0);
    }

    /*==================================================================*
      			Constructors
     *==================================================================*/

    /** 
     * Class of Qbox numbers with default level.
     * Notice that the <em>static</em> default level may be changed 
     * with the {@link #setLevel} method.
     */
    public QboxNumber() {
	this.level = default_level;
    }

    /** 
     * Class of Qbox numbers with a specified level
     * @param level the level to use as default
     * Notice that the <em>static</em> default level may be changed 
     * with the setLevel method.
     */
    public QboxNumber(int level) {
	this.level = level;
    }

    /** 
     * Class of Qbox numbers with a specified resolution.
     * The level computed is such that any source having a diameter up to 
     * <em>arcmin</em> is not larger than a Qbox, i.e. requires to look only
     * at the target Qbox and its neighbours (see {@link #nearby})
     * @param  arcmin  resolution, in arcmin.
     **/
     public QboxNumber(double arcmin) {
	 this.level = level(arcmin);
     }

    /** 
     * Interpret an edited Qbox number (see {@link #toString(int)})
     * @param	text The text to interpret
     * @throws	ParseException when the text contains invalid characters
     **/
    static public int qbox(String text) throws ParseException {
    	char[] a = text.toCharArray();
    	int error = 0;
    	int qbox = 0;
    	int i = 0;

	// Skip leading blanks
	while ((i<a.length) && (a[i] == ' ')) i++;

	// An empty string means "whole sphere" (value 0)
    	if (i >= a.length) 
    	    return(qbox);

	// Get face number
    	if ( (a[i] >= '1') && (a[i] <= '6')) 
    	     qbox = 8 | Character.digit(a[i], 8);
    	else error++;
    	++i;

	// Accept : or . between face# and cell#
    	if ( (i < a.length) && ((a[i] == ':')||(a[i] == '.')))
    	    i++;

	// Interpret the cell# made of digits in base 4.
    	while ( (i < a.length) && (a[i] >= '0') && (a[i] < '4')) {
    	    qbox = (qbox << 2) | Character.digit(a[i], 4);
    	    ++i;
    	}

	// Accept trailing 'A' for 'All'
    	if ( (i < a.length) && (Character.toUpperCase(a[i]) == 'A')) {
    	    ++i;
    	    qbox |= ANY;
    	}

	// Accept final blanks
	while ((i<a.length) && (a[i] == ' ')) i++;

	// Verify all characters interpreted.
    	if (i < a.length)
    	    error++;
    	if (error > 0) {
    	    throw new ParseException
    	    	("****QboxNumber: '" + text + "'+" + i, i);
    	}
	return(qbox);
    }

    /*==================================================================*
      			Manipulation of Qboxes
     *==================================================================*/

    /** 
     * Derive the level of a Qbox number.
     * @param	qbox a Qbox number.
     * @return	the level corresponding to the integer (between -1 and 12)
     **/
    public static final int level(int qbox) {
    	int n = 0;
    	qbox &= BOX;
    	if (qbox == 0) 		// While sphere
    	    return ( -1);
	// Speed-up
    	if ( (qbox & 0x7fff0000) != 0) {
    	    n = 7;
    	    qbox >>>= 14;
    	}
    	while ( (qbox & (~0xf)) != 0) {
    	    qbox >>>= 2;
    	    n++;
    	}
    	return (n);
    }

    /** 
     * Interpret the qbox as an index (offset) number.
     * @param	qbox a Qbox number.
     * @return	a number between <b>0</b> and <b>6&times;4<sup>N</sup>-1</b>
     * 		which can be used e.g. as an index in an array.
     **/
    public static final int index(int qbox) {
    	int n = level(qbox);
    	int i = (qbox & BOX) - (9 << (n<<1));
    	if (i < 0) {
    	    i += 8;
    	}
    	return(i);
    }

    /** 
     * Face number on which the qbox is located.
     * @return	the face number, between <b>1</b> and <b>6</b> .
     * A value of <b>0</b> is returned when <tt>qbox=0</tt> (whole sphere)
     **/
    static public final int face(int qbox) {
    	int n = level(qbox);
    	if (n < 0) 
    	    return (0);
    	return ( (qbox >>> (n<<1)) & 7);
    }

    /*==================================================================*
      			Setting a Qbox from known information
     *==================================================================*/

    /** 
     * Compute qbox number at the specified level.
     * @param	level the level, between <b>0</b> and <b>12</b>
     * @param	face  the face number, between <b>1</b> and <b>6</b>
     * @param	X     projection along X-axis, in range <b>[-1 ... +1[</b>
     * @param	Y     projection along Y-axis, in range <b>[-1 ... +1[</b>
     **/
    static private final int boxno(int level, int face, double X, double Y) {
    	double xy;
    	int x, y, i, count;
    	int qbox = face;
    	qbox |= 8; 	// Insert bit which gives the level

    	// Compute x and y, integer parts of X and Y scaled
    	count = 1 << level;	// 2**level = value of x(y) when X(Y)=+1
    	xy = (0.5 + Math.atan(X) / PIO2) * count;
    	x = (int) xy;
    	xy = (0.5 + Math.atan(Y) / PIO2) * count;
    	y = (int) xy;
    	if (x >= count) x = count - 1;
    	if (y >= count) y = count - 1;
	if (x < 0) x = 0;
	if (y < 0) y = 0;

    	// Add the interleaved bits of x and y
    	qbox <<= (level << 1);	// Face number at its corect bit position
    	for (count >>= 1, i = level - 1; count > 0; count >>= 1, i--) 
    	    qbox |= ( ( (x & count) << i) << 1) | ( (y & count) << i);
        return(qbox);
    }

    /** 
     * Identifies the Qbox containing a position given by RA and Dec.
     * @param	lon   the longitude (RA), degrees
     * @param	lat   the latitude  (Dec), degrees
     **/
    public final int qbox(double lon, double lat) {
    	double[] XY = new double[2];
    	double[]  u = new double[3];
	Coo.setUvec(lon, lat, u);	// compute direction cosines
	int qbox = Coocube.setXY(u, XY);
    	return(boxno(level, qbox, XY[0], XY[1]));
    }

    /** 
     * Identifies the Qbox containing a position given by a Coocube.
     * @param	cc the Coocube equivalent of the position
     **/
    public final int qbox(Coocube cc) {
    	return(boxno(level, cc.face, cc.X, cc.Y));
    }

    /** 
     * Identifies the Qbox containing a given position.
     * @param	c the Coocube equivalent of the position
     **/
    public final int qbox(Coo c) {
    	double[] XY = new double[2];
	double[]  u = new double[3];
	u[0] = c.x; u[1] = c.y; u[2] = c.z;
    	int qbox = Coocube.setXY(u, XY);
	return(boxno(level, qbox, XY[0], XY[1]));
    }

    /*==================================================================*
      			Transform a Qbox into its central Position
     *==================================================================*/

    /** 
     * Get the central position of a Qbox
     * @param	qbox   a Qbox number
     **/
    static public final Coo center(int qbox) {
	double[]  u = new double[3];
    	double[] XY = new double[2];
	int face = center_(qbox, XY);
	if (DEBUG) System.out.println("....center: face=" + face 
		+ ", XY=" + XY[0] + "," + XY[1]);
	Coocube.setUvec(face, XY[0], XY[1], u);
	Coo coo = new Coo(u[0], u[1], u[2]);
	coo.dump("Center");
    	return(coo);
    }

    /** 
     * Set the central position of a Qbox
     * @param	qbox   a Qbox number
     * @param	coo    a Coo, filled with the position of the Qbox center.
     **/
    static public final void center(int qbox, Coo coo) {
	double[]  u = new double[3];
    	double[] XY = new double[2];
	int face = center_(qbox, XY);
	Coocube.setUvec(face, XY[0], XY[1], u);
	coo.set(u[0], u[1], u[2]);
    }

    /*==================================================================*
      			Compute accurate Qbox elements
     *==================================================================*/

    /** 
     * Radius of circle containing any Qbox. 
     * It is the radius of a small circle centered at a Qbox center
     * which includes any Qbox (at level).
     * @return	The maximal radius
     **/
    public final double maxRadius() {
    	return (MAXRAD[level]);
    }

    /** 
     * Radius of circle included in any Qbox.
     * It is the radius of a small circle centered at a Qbox center
     * which is completely included within any Qbox (at level).
     * @return	The minimal radius
     **/
    public final double minRadius() {
    	return (MINRAD[level]);
    }

    /** Compute the area (in square degrees) of a Qbox.
     *	Use the ArcSin formula
     *  		S = A_0 + A_1 + A_2 + A_3 - 2*pi
     *		where A_i are the angles of the rectangle.
     *		The formula is especially simple in a tangential projection.
     * @param  qbox the Qbox number
     * @return	The area for the Qbox in square degrees.
     **/
    static public final double area(int qbox) {
    	double[] XY = new double[4];
    	double S;
    	int i;
    	// Compute the Corners
    	corners(qbox & BOX, XY);
    	for (i = 0; i < 4; i++) 
    	    XY[i] /= Math.sqrt(1. + XY[i] * XY[i]);
    	S = Math.asin(XY[0] * XY[1]) + Math.asin(XY[2] * XY[3])
    	  - Math.asin(XY[0] * XY[3]) - Math.asin(XY[1] * XY[2]);
    	return (DEG * DEG * Math.abs(S));
    }

    /** 
     * Compute radius (degrees) of the Circle including a Qbox.
     * @param  qbox the Qbox number
     * @return	The radius of a circle centered at the Qbox center and
     * 		containing the Qbox.
     **/
    static public final double radius(int qbox) {
    	double[] XY = new double[4];
    	double[] XYc = new double[2];
    	double s2r, num, den, denc, X, Y, rmax = 0;
    	int i;
    	// Compute the Center and Corners
    	center_(qbox & BOX, XYc);
    	corners(qbox & BOX, XY);
    	denc = 1. + XYc[0] * XYc[0] + XYc[1] * XYc[1];
    	for (i = 0; i < 4; i++) {
    	    X = XY[i & 2];
    	    Y = XY[ ( (i & 1) << 1) | 1];
    	    den = denc * (1. + X * X + Y * Y);
    	    num = X * XYc[1] - Y * XYc[0];
    	    num *= num;
    	    X -= XYc[0];
    	    Y -= XYc[1];
    	    num += X * X + Y * Y;
    	    s2r = num / den;
    	    if (s2r > rmax) {
    	    	rmax = s2r;
    	    }
    	    if (DEBUG) {
    	    	System.out.println("....radius(): corner#" + i + "("
    	         + (i & 2) + "," + ( ( (i & 1) << 1) | 1) 
                 + ") -- X,Y=" + X + "," + Y 
		 + "\n          s2r=" + s2r);
    	    }
    	}
    	return (DEG * Math.asin(Math.sqrt(rmax)));
    }

    /*==================================================================*
    				Nearby boxes
     *==================================================================*/

    /** 
     * Find the adjacent qbox in the specified direction.
     * @param	qboxno the starting qbox
     * @param	direction A direction, as a number 0 to 3.
     * 		The direction has the values 
     * 		<b>0</b>=<b>+x</b>
     * 		<b>1</b>=<b>+y</b>
     * 		<b>2</b>=<b>-y</b>
     * 		<b>3</b>=<b>-x</b>
     * @return	the qbox (at the same level) contiguous in the specified 
     * 		direction.
     **/
    static public final int adjacent(int qboxno, int direction) {
	int r = 0;		// remainder of qboxno
	int shifted = 0;	// how many shifts in the hierarchy
	int qb = qboxno;	// qbox number to return
	int dir = direction&3;	// any direction is valid...
	int k;

	/* We try first to move in the 2x2 cell.
	 * If not possible, move up 1 level in the hierarchy.
	 * There is a problem when the face number changes,
	 * which is indicated by a level<0, or qb < 15
	 */
	while ((qb&(~15)) != 0) {
	    k = adj_move[dir][qb&3];
	    if ((k&4)!=0) {	// At the limit of the 2x2 cells
		r |= (k^4)<<shifted;
		qb >>= 2;	// ... one level up
		shifted += 2;
	    }
	    else {
		qb = (qb&(~3))|k;
		break;
	    }
	}
	
	if ((qb&(~15)) == 0) {		// We're here at problematic level=0...
	    k = adj_face[dir][qb&7];	// Here k>=8 when axis direction changes
	    if ((k&8)!=0) {
		// Change of axes: rotation of pi/2, i.e. x-->-y, y-->x
		int comb = 0;		// "comb" to select y bits
		for (qb=shifted; qb>0; qb -= 2) 
		    comb = (comb<<2) | 2;
		if (move_x[dir])	// Change the "comb" to pick x bits
		    comb >>= 1;
		r = (qboxno&comb)^comb;	// r contains now (-y) or (-x) bits
		if (move_x[dir]) r <<= 1;	
		else             r >>= 1;
		// Move toward negative x/y: cell value is maximal.
		if ((dir&2)!=0)     r |= comb;
	    }
	    qb = 8|k;
	}

	if (shifted>0) {
	    qb <<= shifted;
	    qb |= r;
	}

	return(qb);
    }

    /** 
     * Identifies the 8 qboxes contiguous to one qbox.
     * @param	qboxno the starting qbox
     * @param	contig [8] the eight contiguous qboxes.
     * The 8 contiguous qboxes are given in the following order:
     * <FONT COLOR='Blue'><PRE>
     *    +---+---+---+
     *    | 7 | 1 | 4 |
     *    +---+---+---+
     *    | 3 | . | 0 |
     *    +---+---+---+
     *    | 6 | 2 | 5 |
     *    +---+---+---+
     * </PRE> </FONT>
     * Notice that near the corners, only 7 contiguous qboxes exist;
     * the value of the missing qbox is set to 0. And at the zero level
     * (i.e. only 6 qbxoes) there are only 4 contiguous qboxes.
     **/
    static public final void nearby(int qboxno, int[] contig) {
	// Highest index, used to decide whether 2 qboxes lie on the same face.
	int qbmax = 1 << (level(qboxno)<<1);
	int nb = 4;		// Number of boxes found.
	boolean[] same_face 	// adjacent qboxes on the same face as qboxno ?
	    = new boolean[4];
	int i;

	// First first the 4 adjacent qboxes
	for (i=0; i<4; i++) {
	    contig[i] = adjacent(qboxno, i);
	    same_face[i] =  (contig[i] ^ qboxno) < qbmax;
	}

	// Explore how to deal with corners :
	contig[4] = contig[5] = contig[6] = contig[7] = 0;
	if (same_face[0]) {
	    contig[4] = adjacent(contig[0], 1);
	    contig[5] = adjacent(contig[0], 2);
	    nb += 2;
	}
	if (same_face[3]) {
	    contig[6] = adjacent(contig[3], 1);
	    contig[7] = adjacent(contig[3], 2);
	    nb += 2;
	}
	if (nb>=7) return;

	// Not all contiguous qboxes could be found:
	if (same_face[1]) {
	    if (contig[4]==0) {			
		contig[4] = adjacent(contig[1], 0);	// #1 toward +x 
		nb++; 
	    }
	    if (contig[7]==0) {
		contig[7] = adjacent(contig[1], 3);	// #1 toward -x
		nb++;
	    }
	}
	if (nb>=7) return;

	// Still not all contiguous qboxes found...
	if (same_face[2]) {
	    if (contig[5] == 0) {
		contig[5] = adjacent(contig[2], 0);	// #5 toward +x
		nb++; 
	    }
	    if (contig[6] == 0) {
		contig[6] = adjacent(contig[2], 3);	// #5 toward -x
		nb++;
	    }
	}
    }

    /** 
     * Identifies the 9 qboxes close to a target position.
     * @param	center a Coo position
     * @param	contig [9] the 9 neighbouring qboxes, ordered by increasing
     * 		distance fom <em>center</em>
     * @param	sin2d2 [9]    closest squared distance 
     * 		(in <em>SIN2</em> projection where <em>r=2.sin(r/2)</em>).
     * 		The actual distance between  <em>center</em> and 
     * 		<em>contig[i]</em> (in <em>degrees</em>)
     * 		can be computed as 
     * 		<em>r = 2. * AstroMath.asind(0.5 * Math.sqrt(sin2d2))</em>
     * @return  Number of qboxes (5, 8 or 9) in the <em>contig</em> 
     * 		and <em>sin2d2</em> arrays.
     **/
    public final int nearby(Coo center, int[] contig, double[] sin2d2) {
	// First the qbox containing the position!
	int qbox = qbox(center);
	double[]  cXY = new double[2];	// Coordinates of center
	double[] coin = new double[4];	// corners
	double[]    u = new double[3];	// Direction cosine
	double[]   XY = new double[2];	// Projected coordinates
	int face = corners(qbox, coin);
	int i, ix, iy;
	int nb = 9;			// Numpber of qboxes returned.
	double du, w;

	nearby(qbox, contig);		// get the 4, 7 or 8 nearby qboxes
	u[0] = center.x; u[1] = center.y; u[2] = center.z;
    	Coocube.setXY(u, cXY);		// compute cXY

	/* Compute the distances (4sin^2(r/2)) of 8 neighbors */
	for (i=0; i<8; i++) {
	    if (contig[i] == 0)	{	// in a corner of the cube
		sin2d2[i] = 4.;		// corresponds to a 180deg distance
		--nb;
		continue;
	    }
	    ix = nearby_xy[i]>>3;	// 1st octal number
	    iy = nearby_xy[i] &7;	// 2nd octal number
	    XY[0] = (ix&4)==0 ? coin[ix] : cXY[ix^4];
	    XY[1] = (iy&4)==0 ? coin[iy] : cXY[iy^4];
	    Coocube.setUvec(face, XY[0], XY[1], u);
	    du = (u[0] - center.x); w  = du*du;
	    du = (u[1] - center.y); w += du*du;
	    du = (u[2] - center.z); w += du*du;
	    sin2d2[i] = w;
	}
	// Simple swap sort
	for (ix=0; ix<7; ix++) for (iy=ix+1; iy<8; iy++) {
	    if (sin2d2[ix] <= sin2d2[iy]) continue;
	    // swap distances & qbox#
	    w = sin2d2[ix]; sin2d2[ix] = sin2d2[iy]; sin2d2[iy] = w;
	    i = contig[ix]; contig[ix] = contig[iy]; contig[iy] = i;
	}
	// Move target qbox as the first.
	for (i=8; i>0; i--) {
	    contig[i] = contig[i-1];
	    sin2d2[i] = sin2d2[i-1];
	}
	contig[0] = qbox;
	sin2d2[0] = 0;

	return(nb);
    }

    /*==================================================================*
    				Edition
     *==================================================================*/

    /** 
     * Edition of a Qbox value
     * @return	the string equivalent of the coocube
     * 		the letter 'A' is appended for ANY IN QBOX.
     **/
    static public final String toString(int qbox) {
    	char[] buf = new char[20];
    	int i = buf.length;
    	int b = qbox;
    	if ( (b & ANY) != 0) {
    	    buf[--i] = 'A';
    	    b &= BOX;
    	}
    	while ( (b & (~0xf)) != 0) {
    	    buf[--i] = oct[b & 3];
    	    b >>>= 2;
    	}
    	if (b != 0) {
    	    buf[--i] = ':';
    	}
    	buf[--i] = oct[b & 7];
    	return (new String(buf, i, buf.length - i));
    }

    /** 
     * Edition of Qbox level
     * @return	a string.
     **/
    public final String toString() {
	return("[QboxNumber(" + level + ")]");
    }

}
