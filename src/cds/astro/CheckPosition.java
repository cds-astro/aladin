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

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
  Interface for positional checks
 *==================================================================*/

/**
 * CheckPosition is an interface which has to tell whether a {@link Qbox}
 * can match a target.
 **/

interface CheckPosition {
    /** QBOX_NONE means that this Qbox is completely outside the target */
    static final int QBOX_NONE = 0;
    /** QBOX_ANY  means that this Qbox is fully included within the target */
    static final int QBOX_ANY = -1;
    /** QBOX_SOME means that this Qbox intersects the Target (need checking) */
    static final int QBOX_SOME = 1;
    static boolean DEBUG = true;
    /**The method which checks a qbox, must return one of the 3 statuses
     * QBOX_NONE QBOX_ANY QBOX_SOME */
    int checkTarget(int qbox);
}

/** Check a Circular Target
 * return	a Vector with marked Qboxes
 **/
class CheckRadius implements CheckPosition {
    double[] XY = new double[4]; // Corners
    Coocube target; // Center
    double[] u0 = new double[3]; // Center: direction cosines
    double radius, sh2, s2r; // Radius, 4sin^2(r/2), sin^2(r)
    Qbox abox = new Qbox();

    /** Constructor
     * @param	c the center of the target
     * @param	r the target radius, in degrees.
     **/
    CheckRadius(Coo c, double r) {
    	//if (DEBUG) System.out.println("....new CheckRadius(" + c
    	//    + ", r=" + r + ")");
    	target = new Coocube(c);
    	u0[0] = target.x;
    	u0[1] = target.y;
    	u0[2] = target.z;
    	radius = r;
    	s2r = AstroMath.sind(r);
    	s2r *= s2r;
    	sh2 = 2. * AstroMath.sind(r / 2);
    	sh2 *= sh2;
    }

    /** Verify Target in Circle
     * @param	qbox the Qbox to check
     * @return	QBOX_NONE / QBOX_ANY / QBOX_SOME
     **/
    public int checkTarget(int qbox) {
    	double[][] u4 = new double[5][3];
    	Coocube cc;
    	int i, f, lev;
    	double r;

    	abox.set(qbox);
    	lev = abox.level();
    	cc = abox.center();
    	r = cc.distance(target); // Distance from Qboxcenter
    	//if (DEBUG) System.out.println("....CheckTarget(" + qbox + "): lev="
    	//    + lev + "\n    pos=" + cc + "\n    r=" + r);
    	if (r >= (Qbox.MAXRAD[lev] + radius)) { // All points outside
    		return (Qbox.QBOX_NONE);
    	}
    	if (radius >= (Qbox.MAXRAD[lev] + r)) {
    		return (Qbox.QBOX_ANY);
    	}

    	// Check more accurately: can the box be completely included ?
    	// Easy: if all corners are inside the circle ...

    	if (radius >= (Qbox.MINRAD[lev] + r)) {
    	    f = Qbox.ucorners(abox.qbox, u4);
    	    for (i = 0; i < 4; i++) {
    	    	if (Qbox.dist2(u4[i], u0) > sh2) {
    	    		return (Qbox.QBOX_SOME);
    	    	}
    	    }
    	    //cc.set(f, XY[0], XY[1]);
    	    //if (cc.dist(target) > radius) return(Qbox.QBOX_SOME);
    	    //cc.set(f, XY[2], XY[1]);
    	    //if (cc.dist(target) > radius) return(Qbox.QBOX_SOME);
    	    //cc.set(f, XY[2], XY[3]);
    	    //if (cc.dist(target) > radius) return(Qbox.QBOX_SOME);
    	    //cc.set(f, XY[0], XY[3]);
    	    //if (cc.dist(target) > radius) return(Qbox.QBOX_SOME);
    	    return (Qbox.QBOX_ANY);
    	}

    	// Check now if the box is completely outside the circle.
    	// More tricky, not only all corners must be outside,
    	//  but the distance to any of the vertices must be smaller
    	// than the target radius
    	// Method used: distance between center and a vertice
    	//  is computed from vector product:
    	//  sin(dist) = (a^b).C/||a^b||
    	//  if (a,b) = vertices and C = center

    	if (r >= Qbox.MINRAD[lev] + radius) {
    	    double[] v = new double[3];
    	    int n = 0; // Count of positive determinant
    	    Qbox.ucorners(abox.qbox, u4);
    	    u4[4][0] = u4[0][0];
    	    u4[4][1] = u4[0][1];
    	    u4[4][2] = u4[0][2];
    	    for (i = 0; i < 4; i++) {
    	    	Qbox.vecprod(u4[i], u4[i + 1], v);
    	    	r = Qbox.dotprod(v, u0); // (v1^v2).v0
    	    	//if (DEBUG) System.out.println("....Check outside, i=" + i
    	    	//    + ": r=" + r + ", d2=" + (r*r/Qbox.norm2(v)));
    	    	if (r >= 0) {
    	    		n++; // Center inside the box ?
    	    	}
    	    	if (r * r <= s2r * Qbox.norm2(v))  
    	    	    return (Qbox.QBOX_SOME);	// Distance to vertice too small
    	    }
    	    if (DEBUG) 
    	    	System.out.println("....Check outside==n=" + n);
    	    if (n == 4) 
    	    	return (Qbox.QBOX_SOME);
    	    return (Qbox.QBOX_NONE);
    	}
    	return (Qbox.QBOX_SOME);
    }
}

