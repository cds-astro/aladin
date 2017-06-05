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
 	Convex polygon
 *==================================================================*/

/**
 * Convex is a polygon (triangle, quadrangle, etc) delimiting a Region.
 * This class includes also the <em>box</em> region.
 * A non-valid convex has its number of vertices (nvert) set to zero.
 * @author Francois Ochsenbein
 * @version 1.0 04-Jun-2008
 **/

/** 
 * Convex Target (e.g. Rectangle, polygon)
 * return	a Vector with marked Qboxes
 **/
public class ConvexRegion extends Region {
    boolean direct;		// Direct (det>0) or retrograde orientation
    int nvert;			// Number of vert = vert.length
    Coo[] vert;			// Vertices (n+1 [0] === [n]
    double[][] vecs;		// Vectorial product of vertices
    double dim1, dim2, pa;	// In case of box.

    /** 
     * Constructor
     * @param	points vertices of convex polygon
     **/
    ConvexRegion(Coo[] points) /* throws new IllegalArgumentException */ {
    	//if (DEBUG) System.out.println("....new Convex(" + c
    	//    + ", r=" + r + ")");
	dim1 = dim2 = pa = 0;
	nvert = 0;
	int i; double d;
    	if (points.length < 3) {
	    System.err.println("#+++Convex(" + points.length 
		    + "points) requires at least 3 points!");
	    return;
	}
	nvert = points.length;
	vert = new Coo[nvert+1];
	centroid = new Coo();
	for (i=0; i<nvert; i++) {
	    // System.out.println("#...Convex#" + i + ": " + points[i]);
	    if ((points[i].x==0) && (points[i].y==0) && (points[i].x==0)) {
		System.err.println("#+++Convex(" + points.length
		    + "points) has invalid point#" + i);
		nvert = 0;
		return;
	    }
	    vert[i] = points[i];
	    centroid.x += points[i].x;
	    centroid.y += points[i].y;
	    centroid.z += points[i].z;
	}
	vert[nvert] = vert[0];	// Close the path
	centroid.normalize();	// Renormalize center.

	// Compute the vectorial products
	vecs = new double[nvert][3];
	for (i=0; i<nvert; i++) {
	    vert[i].vecprod(vert[i+1], vecs[i]);
	    // Normalise (perpendicular)
	    Coo.normalize(vecs[i]);
	}

	// Verify it's a convex
	minrad = 100.; maxrad = 0.;
	for (i=0; i<nvert; i++) {
	    d = centroid.dotprod(vecs[i]);
	    if (i == 0) direct = d>=0;
	    else if ((d>0)^direct) {
		System.err.println("#+++Convex(" + points.length
			+ "points) not convex at point#" + i + "=" + vert[i]
			+ "; direct=" + direct + ", d=" + d);
		nvert = 0;
		return;
	    }
	    // Compute distance to the edge
	    d = Math.abs(AstroMath.asind(d));
	    if (d<minrad) minrad=d; 
	    if (d>maxrad) maxrad=d; 
	    // Distance to corner
	    d = centroid.distance(vert[i]);
	    if (d<minrad) minrad=d; 
	    if (d>maxrad) maxrad=d; 
	}
	if(DEBUG) System.out.println("#...Created: " + this.toString()
		+ " dist=" + minrad + "/" + maxrad);
    }

    /** 
     * The convex is a Box
     * @param	dim1 dimension of box along axis1
     * @param	dim2 dimension of box along perpendicular to axis1
     * @param	dim3 position angle of axis1
     **/
    final void setBox(double dim1, double dim2, double pa) {
	if ((this.nvert != 4) || (dim1<=0) || (dim2<=0)) {
	    System.err.println("#***setBox(" + dim1 + "," + dim2 + ","
		    + pa + ") -- not a box:");
	    System.err.println("#   " + this);
	    return;
	}
	if (dim1<dim2) { this.dim1 = dim2; this.dim2 = dim1; this.pa = pa+90; }
	else           { this.dim1 = dim1; this.dim2 = dim2; this.pa = pa   ; }
	while (this.pa<0)    this.pa += 180;
	while (this.pa>=180) this.pa -= 180;
    }

    /** 
     * Edition of a Region
     * @return	ascii equivalent
     **/
    public final String toString() {
	StringBuffer b = new StringBuffer();
	if (nvert<3) b.append("*INVALID*");
	if (dim1>0) {
	    b.append("Box(" + centroid);
	    b.append(", " + dim1 + "x" + dim2);
	    if (pa>=0.) b.append(", pa=" + pa);
	}
	else {
	    b.append("Convex[");
	    b.append(nvert);
	    b.append("]");
	    String sep = "(";
	    for (int i=0; i<nvert; i++) {
	        b.append(sep);
	        b.append(vert[i].toString());
	        sep = ", ";
	    }
	}
	b.append(")");
	return(b.toString());
    }

    /** 
     * Area of a polygon
     * @return	Area, in square degrees.
     **/
    public double area () {
	if (nvert<3) return(0./0.);
	double S = (2-nvert)*180.;
	S += vert[0].angle(vert[nvert-1], vert[1]);
	for (int i=1; i<nvert; i++) {
	    S += vert[i].angle(vert[i-1], vert[i+1]);
	}
	return(AstroMath.DEG*S);
    }

    /** Verify point within Polygon
     * @param	point  a position
     * @return	true if point within region.
     **/
    public boolean checkCoo(Coo point) {
	double r; int i;
	// Verify an invalid point ?
	if (nvert<3) return(false);
	// Verify keep the sign
	if (DEBUG) System.out.println("#...Convex.checkCoo(" + point + ")");
	for (i=0; i<nvert; i++) {
	    r = point.dotprod(vecs[i]);
	    if ((r>0)^direct) return(false);
	}
	return(true);
    }

    /** 
     * Verify a circle intersects a Convex polygon
     * @param	point  center of circle
     * @param   r      radius of circle
     * @return	DISJOINT / INTERSECTS / INCLUDES. / IS_PARTOF
     *          for no intersection / overlap / cercle fully included in Convex
     **/
    public int checkCircle(Coo point, double r) {
	// Verify an invalid point ?
	if (centroid == null) return(DISJOINT);
	if (DEBUG) System.out.println("#...Convex.checkCircle(" + point + 
		", r=" + r + ")");
	// Use first the circular approximation
	int st = check1(point, r);
	if (st != INTERSECTS) return(st);

	// Verify vertices within circle ?
	int nin=0, nut=0, i;
	double sh2 = 2.*AstroMath.sind(r/2.); sh2 *= sh2;
	double sinr = AstroMath.sind(r);
	double d2, ps, s0, s1;
	for (i=0; i<nvert; i++) {
	    d2 = point.dist2(vert[i]);
	    if (d2<sh2) {	// vertice inside circle
		nin++;
		if (nut>0) return(INTERSECTS);
	    }
	    if (d2>sh2) {	// vertice outside circle
		nut++;
		if (nin>0) return(INTERSECTS);
	    }
	    // There could be an intersection...
	    // Find the closest point along the edge defined as
	    //  M = (A + tB)/sqrt(1+t^2+t.(A.B)) (t>0 to have A between A and B)
	    ps = vert[i].dotprod(vert[i+1]);
	    s0 = point.dotprod(vert[i]);
	    s1 = point.dotprod(vert[i+1]);
	    if (((ps*s0-s1)*(ps*s1-s0)) <= 0) continue;
	    // Here there is a point on the segment which instersects.
	    if (Math.abs(point.dotprod(vecs[i])) < sinr)
		return(INTERSECTS);
	}
	// Here either all outside or all inside.
	if(DEBUG) System.out.print("    nin="+nin+", nut=" + nut);
	if (nin>0)		// all vertices inside circle
	    return(IS_PARTOF);
	// all vertices outside, and no intersection between polygon and
	// circle: is fully included if its center 
	// belongs to Convex, or fully outside.
	return(checkCoo(point) ? INCLUDES : DISJOINT);
    }

    /** Verify intersection Qbox + Convex (polygon)
     * @param	qbox the Qbox to check
     * @return	DISJOINT / INCLUDES / INTERSECTS / IS_PARTOF
     **/
    public int checkQbox(int qbox) {
	if (centroid == null) return(DISJOINT);
        Qbox abox = new Qbox();
    	abox.set(qbox);
    	Coocube cc = abox.center();
	double rb = abox.radius();		// Circle containing the Qbox
	int stat1 = checkCircle(cc, rb);
	if (stat1 != INTERSECTS) {
	    if(DEBUG) System.out.println("#-->" + stat1);
	    return(stat1);
	}
	
	if(DEBUG) System.out.print("#...convex next");
    	// Check more accurately the intersection of a Qbox with polygon
	// Method: the intersection of 2 great circles defined by (AB)
	// and (PQ) is the vector M = (A^B)^(P^Q) 
	// The test for M between A and B is true if A.M>A.B and B.M>A.B
	//     and similarly P.M>P.Q and Q.M>P.Q
    	double[][] u4 = new double[5][3];
    	Qbox.ucorners(abox.qbox, u4);
	u4[4][0] = u4[0][0];
	u4[4][1] = u4[0][1];
	u4[4][2] = u4[0][2];
    	double[][] v4 = new double[4][3];
    	double[] ps = new double[4];		// Scalar products P.Q
	double[] vm = new double[3];		// Intersection of 2 segments
    	int i, j;

	for (i=0; i<4; i++) {
	    Coo.vecprod(u4[i], u4[i+1], v4[i]);	// Vectors (P^Q)
	    Coo.normalize(v4[i]);
	    ps[i] = Coo.dotprod(u4[i], u4[i+1]);// Scalars (P.Q)
	}

    	for (i=0; i<nvert; i++) {
	    double s01 = vert[i].dotprod(vert[i+1]);	// Scalar (A.B)
    	    for (j=0; j<4; j++) {
		if(DEBUG) System.out.print(" (" + i + "," + j + ")");
		Coo.vecprod(vecs[i], v4[j], vm);	// vm = (A^B)^(P^Q)
		Coo.normalize(vm);
		if (vert[i].dotprod(vm) < s01) {	// Always 2 solutions
		    if (DEBUG) System.out.print(" (-)");
		    vm[0] = -vm[0];
		    vm[1] = -vm[1];
		    vm[2] = -vm[2];
		}
		if ((vert[i].dotprod(vm) >  s01)
		  &&(vert[i+1].dotprod(vm) >  s01)
		  &&(Coo.dotprod(u4[j], vm) > ps[j])
		  &&(Coo.dotprod(u4[j+1], vm) > ps[j])) {
		    if (DEBUG) System.out.println("");
		    return(INTERSECTS);
		}
	    }
	}

	// No intersection between polygon and Qbox.
	// Either Qbox included, or regions disjoint.
	if (DEBUG) System.out.println(" (no intersection)");
	if (checkCoo(cc)) return(INCLUDES);
	return(DISJOINT);
    }
}

