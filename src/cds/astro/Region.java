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

package cds.astro;

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
  Interface for positional checks in Regions
 *==================================================================*/

/**
 * Region is a Factory which implements the AbstractRegion and the
 * constructors of the various regions.
 * @author Francois Ochsenbein
 * @version 1.0 04-Jun-2008
 * 
 **/

public class Region extends AbstractRegion implements Serializable {
    /** 
     * Edition of a Region (see dedicated class)
     **/
    public String toString() {
	return(this.toString());
    }

    /** 
     * Compute area of a Region (see dedicated class)
     **/
    public double area() {
	return(this.area());
    }

    /** 
     * The method checkCoo verifies a point lies within a region.
     * @param	point a point we want to check
     * @return  0 (=DISJOINT) if ouside the region, 1 (INTERSECTS) if inside.
     * **/
    public boolean checkCoo(Coo point) {
	return(this.checkCoo(point));
    }

    /** 
     * The method checkCircle verifies the intersection of Region with Circle.
     * @param	point  a position
     * @param	r      radius (degrees)
     * @return	DISJOINT / INTERSECTS / INCLUDES.
     *          for no intersection / overlap / cercle fully included
     **/
    public int checkCircle(Coo point, double r) {
	return(this.checkCircle(point, r));
    }

    /** 
     * The method checkQbox is inherited from AbstractRegion.
    public int checkQbox(int qbox) { return (DISJOINT); }
     **/

    /* =================================================================
     * Constructor (invalid region only)
     * ================================================================ */
    Region() {
	centroid = null;
    }

    /* =================================================================
     * Creators of the known regions.
     * ================================================================ */

    /** 
     * Circular Region is defined from a center + region.
     * No frame argument, the circle has no orientation...
     * @param 	center the center of the box.
     * @param	radius  the radius of the circular region.
     * @return	CircularRegion.
     **/
    static public Region circle(Coo center, double radius) {
	CircularRegion reg = new CircularRegion(center, radius);
	return((Region)reg);
    }

    /** 
     * Define a box from center + width + height
     * @param 	frame  the frame in which the box is expressed. 
     * 		May be null (implies a unique coordinate frame)
     * @param 	center the center of the box.
     * @param	width  width of the box, in degrees
     * @param	height height of the box, in degrees
     * @return	Convex region.
     **/
    static public Region box(Astroframe frame, Coo center, 
	    double width, double height) {
	Coo[] v4 = new Coo[4];
	Proj3 proj = new Proj3(center); // Tangential projection
	double x = AstroMath.tand(width/2.);
	double y = AstroMath.tand(height/2.);
	proj.set( x,  y); v4[0] = proj.getCoo(); 
	proj.set( x, -y); v4[1] = proj.getCoo();
	proj.set(-x, -y); v4[2] = proj.getCoo();
	proj.set(-x,  y); v4[3] = proj.getCoo();
	double pa = 90;
	if (frame != null) {
	    for (int i=0; i<4; i++) frame.toICRS(v4[i]); 
	    Coo c = new Coo(center); frame.toICRS(c);
	    proj.set(x, 0); 
	    Coo w = proj.getCoo();   frame.toICRS(w);
	    pa = c.posAngle(w);
	}
	if (DEBUG) {
	    System.out.print("#...Creating box(" + center + " " 
		    + width + "x" + height + "deg)");
	    if (frame != null) System.out.print("[" + frame.toString() + "]");
	    System.out.println(" as convex:");
	    for (int i=0; i<4; i++) v4[i].dump("#" + i + "#");
	}
	ConvexRegion reg = new ConvexRegion(v4);
	reg.setBox(width, height, pa);
	return((Region)reg);
    }

    /** 
     * Define a rotated box from center + sides.
     * Notice that the first dimension is the height when posangle=0
     * @param 	center the center of the box.
     * @param	dim1  first angular size of the box, in degrees
     * @param	dim2  second angular size of the box, in degrees
     * @param	posangle position angle of side1 in degrees, 
     *                   measured clockwise from North.
     * @return	Convex region.
     **/
    static public Region rotatedBox(Coo center, 
	    double dim1, double dim2, double posangle) {
	Coo[] v4 = new Coo[4];
	Proj3 proj = new Proj3(center); // Tangential projection
	double h = AstroMath.tand(dim1/2.);
	double w = AstroMath.tand(dim2/2.);
	double c = AstroMath.cosd(posangle);
	double s = AstroMath.sind(posangle);
	double x =  c*w + s*h;
	double y = -s*w + c*h;
	proj.set(x, y); v4[0] = proj.getCoo(); 
	w = -w ; x =  c*w + s*h; y = -s*w + c*h;
	proj.set(x, y); v4[1] = proj.getCoo();
	h = -h ; x =  c*w + s*h; y = -s*w + c*h;
	proj.set(x, y); v4[2] = proj.getCoo();
	w = -w ; x =  c*w + s*h; y = -s*w + c*h;
	proj.set(x, y); v4[3] = proj.getCoo();
	if (DEBUG) {
	    System.out.println("#...Creating box(" + center + " " 
		    + dim1 + "x" + dim2 + "deg, pa=" 
		    + posangle + ") as convex:");
	    for (int i=0; i<4; i++) v4[i].dump("#" + i + "#");
	}
	ConvexRegion reg = new ConvexRegion(v4);
	reg.setBox(dim1, dim2, posangle);
	return((Region)reg);
    }

    /** 
     * Define a polygonal region (convex).
     * No frame argument, the polygon has no orientation...
     * A bad region is returned in case of non-convex result.
     * @param 	vertices of the polygon. 
     * 		The number of vertices should be at least 3 (for a triangle).
     * @return	Convex region.
     **/
    static public Region polygon(Coo[] vertices) {
	ConvexRegion reg = new ConvexRegion(vertices);
	return((Region)reg);
    }

    /** 
     * Define a zonal region.
     * A zone is defined in some frame, delimited by 2 parallel small circles
     * (limits in declination or latitude) and eventually by 2 great circles
     * (limits in right ascension or longitude).
     * Notice that the order of lon0/lon1 is important, e.g. 
     * (359,1) extends over 2deg, while
     * (1,359) extends over 358deg.
     * @param   frame Astroframe in which the limits are defined.
     * 			May be null when a single frame is involved.
     * @param   lon0 Minimal longitude in specified frame
     * @param   lon1 Maximal longitude in specified frame
     * @param   lat0 Minimal latitude in specified frame
     * @param   lat1 Maximal latitude in specified frame
     * @return	Zonal region
     **/
    static public Region zone(Astroframe frame, 
	    double lon0, double lon1, 
	    double lat0, double lat1) {
	ZonalRegion reg = new ZonalRegion(frame, lon0, lon1, lat0, lat1) ;
	return((Region)reg);
    }

    /** 
     * Zonal region defined from a center and extensions.
     * @param   frame Astroframe in which the limits are defined.
     * 			May be null when a single frame is involved.
     * @param 	center the center of the zone
     * @param	width  width of the box, in degrees (between 0 and 360)
     * @param	height height of the box, in degrees (between 0 and 180)
     * @return  the corresponding region
     **/
    static public Region zone(Astroframe frame, Coo center, 
	    double width, double height) {
	ZonalRegion reg = new ZonalRegion(frame, center, width, height);
	return((Region)reg);
    }

    /** 
     * Define an Elliptical region.
     * A spherical ellipse is projected as an ellipse on the tangential plane.
     * @param   frame Astroframe in which the ellipse is defined.
     * 			May be null when a single frame is involved.
     * @param   center Position of the ellipse center
     * @param   londim  the dimension of the ellipse along longitude
     * @param   latdim  the dimension of the ellipse along latitude
     * @return	Ellipse region
     **/
    static public Region ellipse(Astroframe frame, Coo center,
	    double londim, double latdim) {
	EllipticalRegion reg = new EllipticalRegion(frame, center, 
		londim, latdim, 90.);
	return((Region)reg);
    }

    /** 
     * Tilted elliptical region.
     * A spherical ellipse is projected as an ellipse on the tangential plane.
     * @param   center Position of the ellipse center
     * @param   a      the major diameter, in degrees
     * @param   b      the minor diameter, in degrees
     * @param   pa     position angle of major axis, in degrees,
     *                   measured North to East.
     * @return	Ellipse region
     **/
    static public Region ellipse(Coo center, double a, double b, double pa) {
	EllipticalRegion reg = new EllipticalRegion(null, center, a, b, pa);
	return((Region)reg);
    }

}

