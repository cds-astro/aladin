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

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
  	Interface for positional checks in Regions
 *==================================================================*/

/**
 * CircularRegion is a circular region on the sky, defined by its center
 * and a radius.
 * @author Francois Ochsenbein
 * @version 1.0 04-Jun-2008
 * @version 1.1 03-Apr-2009: Bug fixed.
 **/

public class CircularRegion extends Region {
    double sh2, s2r;	// Radius, 4sin^2(r/2), sin^2(r)

    /** Constructor
     * @param	c the center of the target
     * @param	r the target radius, in degrees.
     **/
    CircularRegion(Coo c, double r) {
    	if (DEBUG) System.out.println("#...new Circle(" + c
    	    + ", r=" + r + ")");
    	centroid = new Coo(c);
    	minrad = maxrad = r;
	if (r >= 180.) {
	    System.err.println("#+++Circle(" + c + ", r=" + r 
		    + "): radius set to 180deg!");
	    minrad = maxrad = 180.;
	}
    	s2r = AstroMath.sind(r);
    	s2r *= s2r;
    	sh2 = 2. * AstroMath.sind(r / 2);
    	sh2 *= sh2;
	// Verify a bad center ? 
	if ((centroid.x == 0) && (centroid.y == 0) && (centroid.z == 0)) {
	    centroid = null;
	    s2r = sh2 = -1;
	}
    }

    /** 
     * Edition of a Circular region
     * @return	ascii equivalent
     **/
    public String toString() {
	StringBuffer b = new StringBuffer();
	if (centroid == null) b.append("*INVALID*");
	b.append("Circle(");
	if (centroid != null) b.append(centroid.toString());
	b.append(", r=" + minrad + ")");
	return(b.toString());
    }

    /** 
     * Area of circular region.
     * @return	Area of rgion, in square degrees
     **/
    public double area() {
	// Verify an invalid point ?
	if (centroid == null) return(0./0.);
	return(180.*180.*sh2/Math.PI);
    }

    /** 
     * Verify point within CircularRegion
     * @param	point  a position
     * @return	true if point within region.
     **/
    public boolean checkCoo(Coo point) {
	// Verify an invalid point ?
	if (centroid == null) return(false);
	return(centroid.dist2(point)<=sh2);
    }

    /** 
     * Verify intersection with another circle
     * @param	centre  center of another circular region
     * @param	radius  radius of circle
     * @return	DISJOINT (no overlap), INCLUDES (circle inside region),
     * 		IS_PARTOF (included in circle) INTERSECTS (intersection)
     **/
    public int checkCircle(Coo centre, double radius) {
	// Verify an invalid point ?
	if (centroid == null) return(DISJOINT);
	return(check1(centre, radius));
    }

    /** 
     * Intersection of a Qbox with the Circle
     * @param	qbox the Qbox to check
     * @return	DISJOINT / INCLUDES / INTERSECTS
     **/
    public int checkQbox(int qbox) {
	if (centroid == null) return(DISJOINT);
        Qbox abox = new Qbox();
    	abox.set(qbox);
    	Coocube cc = abox.center();
    	double r   = abox.maxRadius(qbox);
	int stat1  = check1(cc, r);	// First approximation
    	if (DEBUG) System.out.println("#...CheckQbox(" + qbox + "=" + abox
		+ "[" + cc + "]"
		+ "\n    in Region " + this + " => " + stat1);
	if (stat1 == IS_PARTOF) {	// Added v1.1
	    // Here my circle is fully included within the circle containing
	    // the qbox. But is does not mean that my circle is contained
	    // within the Qbox -- have to clarify..
	    stat1 = check1(cc, abox.minRadius(qbox));
    	    if (DEBUG) System.out.println("#   [minRad?] " 
		+ this + " => " + stat1);
	    if (stat1 != IS_PARTOF) stat1 = INTERSECTS;
	}
	if (stat1 != INTERSECTS)
	    return(stat1);

    	// Check more accurately: can the box be completely included ?
    	// Count how many corners of the Qbox are inside the circle

    	double[][] u4 = new double[5][3];	// Closed path
	Qbox.ucorners(abox.qbox, u4);
	int inside = 0;
	int i;
    	for (i=0; i<4; i++) {
    	    if (centroid.dist2(u4[i]) <= sh2) 
		inside++;
    	}
	if (DEBUG) System.out.println("#...inside=" + inside);
	if (inside == 4) return (INCLUDES);
	if (inside  > 0) return (INTERSECTS);

	/*---
    	The 4 corners of the Qbox are outside the circle.
    	Look for possible intersection of the circle with 
	any side of the Qbox:
	-> at least one intersection => overlapping
	-> no intersection: overlapping only if circle center inside the box.
	The distance between the center of the circle and a side is computed
	from vector product if (a,b) = vertices and C = center
    	    sin(dist) = (a^b).C/||a^b||
	--*/

    	double[] v = new double[3];
    	Qbox.ucorners(abox.qbox, u4);
	if (DEBUG) System.out.println(Coo.toString("#corners ", u4));
    	u4[4][0] = u4[0][0];
    	u4[4][1] = u4[0][1];
    	u4[4][2] = u4[0][2];
    	for (i=0; i<4; i++) {
    	    Coo.vecprod(u4[i], u4[i+1], v);
    	    r = centroid.dotprod(v); 	// (v1^v2).v0 = distance to side
    	    //if (DEBUG) System.out.println("#...Check outside, i=" + i
    	    //    + ": r=" + r + ", d2=" + (r*r/Qbox.norm2(v)));
    	    if (r >= 0) 
		inside++;
	    if (DEBUG) System.out.println("#   i=" + i + ", r=" + r 
		    + ", ||v||=" + Coo.norm2(v));
    	    if (r*r <= s2r*Coo.norm2(v))  
    	    	return (INTERSECTS);	// Circle intersects Qbox side
    	}
    	// if (DEBUG) System.out.println("#...Check outside==n=" + n);
	return (inside == 4 ? IS_PARTOF : DISJOINT);
    }
}

