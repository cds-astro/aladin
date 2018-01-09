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
 * @version 1.0	  16-sep-2002
 * @version 1.01  Oct 2003 (MW) add a copy constructor
 * @version 1.02 jan 2004 (BB) ajout de la methode equals dans QBox.
 * @version 1.03 voc 2007 (FO) unitinialized values
 * @version 1.1  05-Jun-2008 (FO) method corners
 * @version 1.11 25-Jun-2008 (FO) special algorithm for small regions
 * -------------
*/


/**
 * The <em>Qbox</em> divides the celestial sphere into "cells" of approximative
 * constant size.
 *
 * The routines provided in this module all deal with "qboxes".
 * A <b>qbox</b> is just a number which represents one of the cells.
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
 *      0           9           6     83&deg;
 *      1          36          24     41&deg;
 *      2         144          96     20&deg;
 *      3         576         384     10&deg;
 *      4        2304        1536      5&deg;
 *      5        9216        6144      2&deg;30'
 *      6       36864       24576      1&deg;15'
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
 *	<P>
 * The selection of valid qboxes makes usage of an <b>Enumeration</b>
 * which returns the valid qboxes via its  <b>nextElement</b> method.
 **/

public class Qbox implements Serializable {

    /** The Qbox is just a number -- the lefmost bit (sign bit)
    	 being set for a Qbox with status 'ANY' (no need to make test)
     **/
    int qbox; // The Qbox is just a number!!

    /* Variables common to the Class: all Definitions */
    static private final double PIO2 = Math.PI / 2.;
    static boolean DEBUG = false; /*true; */
    static final int ANY = 0x80000000; // The 'ANY' status
    static final int BOX = 0x7fffffff; // The ~ANY  bits
    static private final int MAXLEVEL = 12;
    static public int level = 9; // The current level
    static final int QBOX_NONE = 0;
    static final int QBOX_ANY = -1;
    static final int QBOX_SOME = 1;
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

    /*==================================================================*
      Initialisations
     *==================================================================*/

    /** Change the default Level which is 9.
     * @param	lev the new level, between 0 and 12 -- other values do
     *		not change it.
     * @return	the previously defined level
     **/
    public static final int setLevel(int lev) {
    	int old_lev = level;
    	if ( (lev >= 0) && (lev <= 12)) 
    	    level = lev;
    	return (old_lev);
    }

    /*==================================================================*
      Internal Routines
     *==================================================================*/

    /** Compute the Center of a Qbox
     * @param	qboxno the qbox number
     * @param	XY the position of the center
     * @return	the cube face number in range 1..6 (0 if error)
     **/
    private static final int center_(int qboxno, double[] XY) {
    	int b = qboxno & BOX;
    	int x = 0, y = 0;
    	double f;
    	int qlev;
    	if (b < 9) {
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
    	return (b);
    }

    /** Compute the Corners of a Qbox from qbox number
     * @param	qboxno the qbox number
     * @param	blur the (xy) position of Bottom-Left and Up-Right points
     * 		in the tangential projection.
     * @return	the cube face number in range 1..6 (0 if full sky or error)
     **/
    public static final int corners(int qboxno, double[] blur) {
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

    /** Compute the unit vector of the center of a Qbox.
     * @param	qboxno	the qbox number
     * @param	ucenter	a [3] array containing on return the unit vector
     * @return	the cube face number in range 1..6 (0 if error)
     **/
    public static final int ucenter(int qboxno, double[] ucenter) {
    	double XY[] = new double[2];
	int face = center_(qboxno, XY);
    	if (face == 0)
	    return (face);
	Coocube.setUvec(face, XY[0], XY[1], ucenter);
    	return(face);
    }

    /** Compute the 4 vectors representing the corners of a Qbox.
     * 		The corners are stored in the direct (trigonometric) order.
     * @param	qboxno the qbox number
     * @param	ucorn a [4][3] array of vertices vector.
     * @return	the cube face number in range 1..6 (0 if error)
     **/
    public static final int ucorners(int qboxno, double[][] ucorn) {
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

    /** Compute the box range at the default level corresponding to the
     * box given as first argument.
     * @param	qbox the qbox number
     * @param	qb returned values: qb[0] = lower limit, qb[1] = upper limit+1
     **/
    private static final void range(int qbox, int[] qb) {
    	int any = qbox & ANY;
    	int mask;
    	if ( (qbox & BOX) == 0) { // Whole Sky
    	    qb[0] = (9 << (level << 1)) | any;
    	    qb[1] = (15 << (level << 1)) | any;
    	}
    	else {
    	    qb[0] = qb[1] = qbox;
    	    mask = (8 << (level << 1));
    	    while ( (qb[0] & mask) == 0) {
    	    	qb[0] <<= 2;
    	    	qb[1] = (qb[1] << 2) | 3;
    	    }
    	    qb[1]++;
    	    qb[0] |= any;
    	    qb[1] |= any;
    	}
    }

    /*==================================================================*
      Constructors
     *==================================================================*/

    /** Define the default Qbox which represents the whole sphere
     **/
    public Qbox() {
    	qbox = 0;
    }

    /**
     * Copy constructor.
     * @param qb Qbox object to copy
     */
    public Qbox(Qbox qb) {
       set(qb);
    }

    /** Define the Qbox containing a given position, at the current level.
     * @param coo coordinates object used to create the Qbox
     **/
    public Qbox(Coo coo) {
    	set(coo);
    }

    /** Transform an integer into a Qbox
     * @param b integer to transform into a Qbox
     **/
    private Qbox(int b) {
    	qbox = b;
    }

    /** Define the Qbox from a text similar to the Edited qbox (see toString)
     * @param	text The text to interpret
     * @throws	ParseException when the text contains invalid characters
     **/
    public Qbox(String text) throws ParseException {
    	char[] a = text.toCharArray();
    	int error = 0;
    	int i = 0;
    	qbox = 0;
    	if (i >= a.length) 
    	    return;
    	if ( (a[i] >= '1') && (a[i] <= '6')) {
    	    qbox = 8 | Character.digit(a[i], 8);
    	}
    	else {
    	    error++;
    	}
    	++i;
    	if ( (i < a.length) && (a[i] == ':')) {
    	    i++;
    	}
    	while ( (i < a.length) && (a[i] >= '0') && (a[i] < '4')) {
    	    qbox = (qbox << 2) | Character.digit(a[i], 4);
    	    ++i;
    	}
    	if ( (i < a.length) && (Character.toUpperCase(a[i]) == 'A')) {
    	    ++i;
    	    qbox |= ANY;
    	}
    	if (i < a.length) {
    	    error++;
    	}
    	if (error > 0) {
    	    throw new ParseException
    	    	("****Qbox: '" + text + "'+" + i, i);
    	}
    }

    /*==================================================================*
      Manipulation of Qboxes
     *==================================================================*/

    /** Compute the level of any integer considered as a qbox
     * @param	b the integer considered as a Qbox.
     * @return	the level corresponding to the integer (between -1 and 13)
     **/
    public static final int level(int b) {
    	int n = 0;
    	b &= BOX;
    	if (b == 0) {
    	    return ( -1);
    	}
    	if ( (b & 0x7fff0000) != 0) {
    	    n = 7;
    	    b >>>= 14;
    	}
    	while ( (b & (~0xf)) != 0) {
    	    b >>>= 2;
    	    n++;
    	}
    	return (n);
    }

    /** Actual qbox level
     * @return	the level corresponding to the qbox (between -1 and 12)
     **/
    public final int level() {
    	return (level(qbox));
    }

    /** Status of a Qbox: check whether it's completely included in
     *  the Selection <em>(no need to make further tests on Position)</em>
     * @return	true if the box is "any"
     **/
    public final boolean isAny() {
    	return ( (qbox & ANY) != 0);
    }

    /** Set the Qbox to the 'Any' status.
     *  the Selection <em>(no need to make further tests on Position)</em>
     **/
    public final void toAny() {
    	qbox |= ANY;
    }

    /** Actual qbox value as an Integer
     * @return	a box value (in the range specified in the introduction)
     **/
    public final int box() {
    	return (qbox & BOX);
    }

    /** Actual qbox index
     * @return	the index, a number between 0 and 6*4^level -1; 
     *          -1 for uninitialized qbox (no position).
     **/
    public final int index() {
    	int n = level();	// is -1 for uninitialized qbox (mod. V1.03)
    	int i = n;	
    	if (n>=0) i = (qbox & BOX) - (9 << (2 * n));
    	return (i);
    }

    /** On which face is located the qbox ?
     * @return	the face number, between 1 and 6 -- 0 for whole sphere
     **/
    public final int face() {
    	int n = level();
    	if (n < 0) {
    	    return (0);
    	}
    	return ( (qbox >>> (2 * n)) & 7);
    }

    /*==================================================================*
      Setting a Qbox from known information
     *==================================================================*/

    /** Convert a Coocube into a Qbox at the default level
     * @param	cc the Coocube equivalent of the position
     **/
    public final void set(Coocube cc) {
    	double xy;
    	int x, y, i, count;
    	qbox = cc.face;
    	// System.out.println("#...qbox set(" + cc + ") qbox=" + qbox);
    	if (level <= 0) { return; }
	if (qbox == 0)  { return; }
    	qbox |= 8; // Insert bit to indicate level

    	// Compute x and y, integer parts of X and Y scaled
    	count = 1 << level; // 2**level = value of x(y) when X(Y)=+1
    	xy = (0.5 + Math.atan(cc.X) / PIO2) * count;
    	x = (int) xy;
    	if (x >= count) {
    	    x = count - 1;
    	}
    	xy = (0.5 + Math.atan(cc.Y) / PIO2) * count;
    	y = (int) xy;
    	if (y >= count) 
    	    y = count - 1;

    	    // Add the interleaved bits of x and y
    	qbox <<= (level << 1); // Face number at its corect bit position
    	for (count >>= 1, i = level - 1; count > 0; count >>= 1, i--) 
    	    qbox |= ( ( (x & count) << i) << 1) | ( (y & count) << i);
    }

    /**
     * Sets the Qbox value from another Qbox object
     * @param qb Qbox object to copy
     */
    public final void set(Qbox qb) {
       qbox = qb.qbox;
    }

    /** 
     * Convert a Position into its Qbox at the default level
     * @param	c the Coocube equivqlent of the position
     **/
    public final void set(Coo c) {
    	Coocube cc = new Coocube(c);
    	set(cc);
    	// System.out.println("#...qbox c=" + c + "=>" + cc);
    }

    /** 
     * Set the qbox value. 
     * Notice that the value may be invalid, tu use with caution!
     * @param	n the Coocube equivalent of the position
     **/
    final void set(int n) {
    	qbox = n;
    }

    /*==================================================================*
      Transform a Qbox into its central Position
     *==================================================================*/

    /** Convert a Qbox into the Coocube position of its center.
     **/
    public final Coocube center() {
    	double XY[] = new double[2];
    	int b;
    	b = center_(qbox, XY);
    	return (new Coocube(b & 7, XY[0], XY[1]));
    }

    /*==================================================================*
      Compute accurate Qbox elements
     *==================================================================*/

    /** 
     * Tests whether a point is inside a Qbox
     * @param	point a point to test
     * @return	true is the point is inside the Qbox
     **/
    public final boolean inside(Coo point) {
	if (qbox == 0) {	// While sky
	    return(true);
	}
    	double[] XY = new double[4];
	int face = corners(qbox & BOX, XY);
	Coocube cc = new Coocube(point);
	//System.out.println("#...inside(" + point + "), qbox#" + this
	//	+ " - faces are " + cc.face + "," + face);
	if (cc.face != face) 
	    return(false);
	//System.out.println("# cc=" + cc);
	//System.out.println("# XY=" + XY[0] + ", " + XY[1] + ", " + XY[2] + ", " + XY[3]);
	return ((cc.X >= XY[0]) && (cc.X <= XY[2])
	     && (cc.Y >= XY[1]) && (cc.Y <= XY[3]));
    }

    /** 
     * Return the maximal radius (in degrees) of any Qbox at the default level
     * @return	The maximal radius
     **/
    public final static double maxRadius() {
    	return (MAXRAD[level]);
    }

    /** 
     * Return the maximal radius (in degrees) of a Qbox
     * @param	qbox a valid qbox number
     * @return	The maximal radius (circle includes the Qbox)
     **/
    public final static double maxRadius(int qbox) {
	if (qbox == 0) return(180.);
	int lev = level(qbox);
	if (lev<0) return(0.);
    	return (MAXRAD[lev]);
    }

    /** 
     * Return the minimal radius (in degrees) of a Qbox.
     * @return	The minimal radius (circle fully included in Qbox)
     **/
    public final static double minRadius() {
    	return (MINRAD[level]);
    }

    /** 
     * Return the minimal radius (in degrees) of any Qbox at the default level
     * @param	qbox a valid qbox number
     * @return	The minimal radius (circle fully included in Qbox)
     **/
    public final static double minRadius(int qbox) {
	if (qbox == 0) return(180.);
	int lev = level(qbox);
	if (lev<0) return(0.);
    	return (MINRAD[lev]);
    }

    /** Compute the area (in square degrees) of a Qbox.
     *	Use the ArcSin formula
     *  		S = A_0 + A_1 + A_2 + A_3 - 2*pi
     *		where A_i are the angles of the rectangle.
     *		The formula is especially simple in a tangential projection.
     * @return	The area for the Qbox in square degrees.
     **/
    public final double area() {
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
     * Compute the radius (in degrees) of the Circle containing the Qbox.
     * @return	The radius of the Qbox
     **/
    public final double radius() {
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
    	    /* if (DEBUG) {
    	    	System.out.println("#...radius(): i=" + i + "("
    	         + (i & 2) + "," + ( ( (i & 1) << 1) | 1) 
                 + ") -- X,Y=" + X + "," + Y + " -- s2r=" + s2r);
    	    } */
    	}
    	return (DEG * Math.asin(Math.sqrt(rmax)));
    }

    /** 
     * Get the 4 corners of a Qbox.
     * @return	the 4 coordinates (which define a rectangle)
     **/
    public final Coo[] corners() {
    	Coo[] c = new Coo[4];
	double[] XY = new double[4];
    	int face = corners(qbox, XY);
    	if (face == 0) 		// Invalid position
    	    return (c);
	double[] u = new double[3];
    	Coocube.setUvec(face, XY[0], XY[1], u); c[0] = new Coo(u[0],u[1],u[2]);
    	Coocube.setUvec(face, XY[2], XY[1], u); c[1] = new Coo(u[0],u[1],u[2]);
    	Coocube.setUvec(face, XY[2], XY[3], u); c[2] = new Coo(u[0],u[1],u[2]);
    	Coocube.setUvec(face, XY[0], XY[3], u); c[3] = new Coo(u[0],u[1],u[2]);
    	return(c);
    }

    /*==================================================================*
    	Edition
     *==================================================================*/

    /** Default Edition of the Qbox value
     * @return	the string equivalent of the coocube
     * 		the letter 'A' is appended for ANY IN QBOX.
     **/
    public final String toString() {
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

    /*==================================================================*
      Selection in a Circle
     *==================================================================*/

    private final static void call_down(int qbox, Region geom,
    	    ArrayList<Qbox> found) {
    	int leaf = 8 << (level << 1);
    	int status, qb, qb0, qb1;
    	//if (DEBUG) System.out.println("....call_down(" + qbox
    	//    + ", vector=" + found.size() + ")" + " leaf=" + leaf);

    	/* The Whole sky... */
    	if (qbox == 0) {
    	    qb0 = 9;
    	    qb1 = 14;
    	}
    	else {
    	    qb0 = qbox << 2;
    	    qb1 = qb0 | 3;
    	}

    	for (qb = qb0; qb <= qb1; qb++) {
    	    status = geom.checkQbox(qb);
    	    if (status == QBOX_NONE) {
		if (DEBUG) System.out.println("#...checkQ(" + qb + ") => NONE");
    	    	continue;
    	    }
    	    if (status == QBOX_ANY) {
		if (DEBUG) System.out.println("#...checkQ(" + qb + ") => +ANY");
    	    	found.add(new Qbox(qb | ANY));
    	    }
    	    else if ( (qb & leaf) != 0) {
		if (DEBUG) System.out.println("#...checkQ(" + qb + ") => +add");
    	    	found.add(new Qbox(qb));
    	    }
    	    else {
		if (DEBUG) System.out.println("#...checkQ(" + qb + ") => ....");
    	    	call_down(qb, geom, found);
    	    }
    	}
    }

    /** Create a ArrayList of matching Qboxes in a Selection by Circle
     * @param	center the center of the target
     * @param	radius the target radius, in degrees.
     * @return	a ArrayList with marked Qboxes
     **/
    static ArrayList in_region(Region region) {
	ArrayList<Qbox> vector = new ArrayList<Qbox>();
	if (region.maxrad < MAXRAD[level]) {
	    Qbox cbox = new Qbox(region.centroid);
	    if (region.checkQbox(cbox.qbox) == Region.IS_PARTOF) {
		vector.add(cbox);
		return (vector);
	    }
	}
	call_down(0, region, vector);
	return (vector);
    }
    /*
    static Vector in_circle(Coo center, double radius) {
    	RegionCircle circle = new RegionCircle(center, radius);
    	Vector vector = new Vector();

    	if (radius >= 180) {
    	    vector.addElement(new Qbox(ANY));
    	    return (vector);
    	}
    	call_down(0, circle, vector);
    	return (vector);
    }
    */

    /*==================================================================*
      Selection of Qboxes at the Current Level
     *==================================================================*/

    /** List in an enumeration all Qboxes corresponding to a qbox
     * To get all qboxes, just do the following:
     *		cq = new Qbox(); e=cq.list();
     * @return	a list of Qboxes existing at the default level
     *		contained in the Object.
     **/
    public Enumeration list() {
    	return new Enumeration() {
    	    int[] qb = { qbox, 0};
       	    public boolean hasMoreElements() {
    	    	if (qb[1] == 0)  	// Not Yet Initialized
    	    	    range(qb[0], qb);
    	    	return (qb[0] < qb[1]);
    	    }
    	    public Object nextElement() {
    	    	Qbox e = new Qbox();
    	    	e.qbox = qb[0]++;
    	    	return (e);
    	    }
    	};
    }

    /** List in an enumeration all Qboxes selected in a ArrayList
     * @param	vector a ArrayList of qboxes returned from a select routine
     * @return	an Enumeration of leaf Qboxes.
     **/
    public static Enumeration list(final ArrayList vector) {
    	return new Enumeration() {
    	    ArrayList v = vector;
    	    int pos = 0; // Position in ArrayList
    	    int[] qb = { 0, 0}; // Range to cover for this vector

    	    public boolean hasMoreElements() {
    	        //if (DEBUG) System.out.println("....moreEnumVec(" + pos + "/"
    	        //+ v.size() + ") qb=" + qb[0] + "/" + qb[1]);
    	        if (qb[0] >= qb[1]) { // Step not yet initialized
    		    Qbox o;
    		    if (pos >= v.size()) return (false);
    		    o = (Qbox) v.get(pos++);
    		    range(o.qbox, qb);
    		    //if (DEBUG) System.out.println("......list,qbox=" + o
    		    //   + ", range=" + qb[0] + "," + qb[1]);
    	        }
    	        return (true);
    	    }

    	    public Object nextElement() {
    		Qbox e = new Qbox(qb[0]);
    		qb[0] += 1;
    		return (e);
    	    }
    	};
    }

    /** 
     * Return all Qboxes concerned by a Region (circle, box, zone, etc...)
     * @param	region A region defined according to AbstractRegion.
     * @return	an Enumeration of leaf Qboxes.
     **/
    public static Enumeration list(Region region) {
    	ArrayList vector = in_region(region);
    	return list(vector);
    }
    /*
     * Return all Qboxes concerned by a Circular Target
     * @param	center Center of circular region
     * @param   radius Radius of circular region
     * @return  an Enumeration of leaf Qboxes.
     **/
    public static Enumeration circle(Coo center, double radius) {
	return list(Region.circle(center, radius));
    }

    /** ------------------------------------ (removed)
    public static Enumeration list() {
    	 return new Enumeration() {
    	     int abox =  9 << (level<<1);
    	     int ebox = 15 << (level<<1);
    	 public boolean hasMoreElements() {
    	     return (abox < ebox);
    	 }
    	 public Object nextElement() {
             Qbox e = new Qbox();
    	     e.qbox = abox++;
    	    return(e);
    	 }
      };
    }
    ----------------------------------------- **/

    /**
     * MOD-BB 21-01-04 Ajout de cette methode.
     * Test d'egalite de QBox.
     * @param o Object a comparer.
     * @return Vrai si o est identique a this, faux sinon.
    **/
    public boolean equals(Object o) {
   if (this == o) return true;
	boolean res = false;
	if (o instanceof Qbox)
	res = ((Qbox)o).qbox == this.qbox;
	return res;
    }
    
 	/**
 	 * Compute the hashcode
 	 * @return the hascode value
 	 */
 	public int hashCode() {
 		return 123 * qbox;
 	}
   
}
