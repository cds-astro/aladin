package cds.astro;

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
 	Zonal region.
 *==================================================================*/

/**
 * A <em>zone</em> is a region delimited by 2 parallel small circles
 * (limits in declination or latitude) and eventually by 2 great circles
 * (limits in right ascension or longitude).
 * A non-valid zone has a null rotation matrix.
 * @author Francois Ochsenbein
 * @version 1.0 04-Jun-2008
 * @version 1.1 15-Apr-2009: bug in iCircle
 **/

/** 
 * Zone Target (e.g. Rectangle, polygon)
 * return	a Vector with marked Qboxes
 **/
public class ZonalRegion extends Region {
    Astroframe frame;		// Frame in which limits are defined.
    double lon0, lon1, dlon;	// Limits in longitude; dlon = (lon1-lon0)/2
    double lat0, lat1;		// Limits in latitude
    double R[][];		// Rotation to bring position to original frame
    double zmin, zmax, smax;	// Limits on z = sin(lat), and max(sec(dec))
    double xmin, ymax, tmax;	// cos(dlon), sin(dlon), tan(dlon/2)

    /**
     * Do all initialisations here
     **/
    private void set_zone(double lon0, double lon1, double lat0, double lat1) {
    	if (DEBUG) System.out.println("#...new Zone(" 
		+ lon0 + ".." + lon1 + ", " + lat0 + ".." + lat1 
		+ ")[" + frame + "]");
	this.lon0 = lon0; this.lon1 = lon1;
	if (lat0 > lat1) { this.lat0 = lat1; this.lat1 = lat0; }
	else             { this.lat0 = lat0; this.lat1 = lat1; }

	// Compute the centroid of the zone.
	double clat = 0.5*(lat0+lat1);
	double clon = 0.5*(lon0+lon1); 
	if (lon0>lon1) clon += 180.;
	centroid = new Coo(clon, clat);

	// Estimate the min/max radius
	Coo v3[] = new Coo[4];
	minrad=180.; maxrad=0;
	v3[0] = new Coo(lon0, lat0);	// left edge
	v3[1] = new Coo(lon0, lat1);	// right edge
	v3[2] = new Coo(clon, lat0);	// lower
	v3[3] = new Coo(clon, lat1);	// upper
	// Distance with left edge
	double r = centroid.distc(v3[0], v3[1]);
	if (r<minrad) minrad=r; if(r>maxrad) maxrad=r;
	int i;
	for (i=0; i<4; i++) {
	    r = centroid.distance(v3[i]);
	    if (r<minrad) minrad=r; if(r>maxrad) maxrad=r;
	}
	if (DEBUG) System.out.println("#   min/max radius = " 
		+ minrad + "/" + maxrad);
	
	// Compute the rotation matrix to transform ICRS position into peculiar
	v3[0] = new Coo(clon, 0.); 	// x-axis in centered frame
	v3[1] = new Coo(clon+90., 0.); 	// y-axis in centered frame
	v3[2] = new Coo(0., 90.); 	// z-axis in centered frame
	if (frame != null) {
	    for (i=0; i<3; i++)
	        frame.toICRS(v3[i]);
	    frame.toICRS(centroid);
	}
	R = new double[3][3];
	for (i=0; i<3; i++) {
	    R[i][0] = v3[i].x; 
	    R[i][1] = v3[i].y; 
	    R[i][2] = v3[i].z; 
	}

	// Compute the zone extensions.
	dlon = lon1 - lon0;
	if (dlon < 0) dlon += 360;
	if (dlon > 360) {
	    System.err.println("#+++Zone(lon=" + lon0 + "," + lon1
		    + ") lon.range reduced to +/-180deg");
	    dlon = 360;
	}
	dlon /= 2;
	xmin = AstroMath.cosd(dlon);
	ymax = AstroMath.sind(dlon);
	tmax = AstroMath.tand(dlon/2.);
	zmin = AstroMath.sind(lat0);
	zmax = AstroMath.sind(lat1);
	smax = Math.abs(zmin);
	double az = Math.abs(zmax);
	if (az > smax) smax = az;
	smax = 1.0/Math.sqrt(1.0-smax*smax);	// max(sec(lat))

	if (DEBUG) {
	    System.out.println(Coo.toString("Rotation matrix ", R));
	    System.out.println("    xmin=" + xmin + ", ymax=" + ymax
		    + ", tmax=" + tmax + "\n    smax=" + smax);
	}
    }

    /** 
     * Constructor: define a zone extending between specified limits.
     * Notice that the order of lon0/lon1 is important, e.g. 
     * (359,1) extends over 2deg, while
     * (1,359) extends over 358deg.
     * @param	frame Astroframe in which the limits are defined.
     * @param	lon0 Minimal longitude in specified frame
     * @param	lon1 Maximal longitude in specified frame
     * @param	lat0 Minimal latitude in specified frame
     * @param	lat1 Maximal latitude in specified frame
     **/
     ZonalRegion(Astroframe frame, 
	     double lon0, double lon1, 
	     double lat0, double lat1) {
	this.frame = frame;
	set_zone(lon0, lon1, lat0, lat1);
    }

    /** 
     * Zone on the same frame
     * Notice that the order of lon0/lon1 is important, e.g. 
     * (359,1) extends over 2deg, while
     * (1,359) extends over 358deg.
     * @param	lon0 Minimal longitude in specified frame
     * @param	lon1 Maximal longitude in specified frame
     * @param	lat0 Minimal latitude in specified frame
     * @param	lat1 Maximal latitude in specified frame
     **/
    ZonalRegion(double lon0, double lon1, double lat0, double lat1) {
	this.frame = null;
	set_zone(lon0, lon1, lat0, lat1);
    }

    /** 
     * Zone from a center, size along lon and lat.
     * @param 	frame  the frame in which the bix is expressed
     * @param 	center the center of the box.
     * @param	width  width of the box, in degrees
     * @param	height height of the box, in degrees
     * @param	posangle position angle of the width direction (NS), in degrees
     **/
    ZonalRegion(Astroframe frame, Coo center, double width, double height) {
	if ((width>360.)||(height>180.)) {
	    System.err.println("#+++Zone(" + center + ", dim=" + 
		    width + "/" + height + "): too large width/height!");
	    if (width>360.) width=360;
	    if(height>180.) height=180.;
	}
	this.frame = frame;
	double w = width/2.; 
	double h = height/2.;
	double lon = center.getLon();
	double lat = center.getLat();
	set_zone(lon-w, lon+w, lat-h, lat+h);
    }

    /** 
     * Zone from a center, size along lon and lat.
     * @param 	center the center of the box.
     * @param	width  width of the box, in degrees
     * @param	height height of the box, in degrees
     * @param	posangle position angle of the width direction (NS), in degrees
     **/
    ZonalRegion(Coo center, double width, double height) {
	this((Astroframe)null, center, width, height);
    }

    /** 
     * Edition of a Zone
     * @return	ascii equivalent
     **/
    public final String toString() {
	StringBuffer b = new StringBuffer();
	if (R == null) b.append("*INVALID*");
	b.append("Zone");
	if (this.frame != null) b.append("[" + this.frame + "]");
	b.append("(" + centroid + ", " + 2.*dlon + "x" + (lat1-lat0) + ")");
	return(b.toString());
    }

    /** 
     * Area of a Zone
     * @return	Area in square degrees.
     **/
    public final double area() {
	if (centroid == null) return(0./0.);
	return(AstroMath.DEG * (zmax-zmin) * 2.*dlon);
    }

    /** 
     * Verify point within Zone
     * @param	point  a position
     * @return	true if point within region.
     **/
    public boolean checkCoo(Coo point) {
	// Verify an invalid point ?
	if (R == null) return(false);
	// Verify the point is within limits
	if (DEBUG) System.out.println("#...Zone.checkCoo(" + point + ")");
	double v[] = new double[3];
	v[0] = point.x;
	v[1] = point.y;
	v[2] = point.z;
	Coo.rotateVector(R, v);
	if ((v[2]>=zmin) && (v[2]<=zmax)) {
	    if (v[0] >= xmin) return(true);	// includes case [0,360]
	    double cos2b = v[0]*v[0]+v[1]*v[1];	// cos^2(b)
	    if (xmin>0.707) 	// Small region, more accurate with y test
		return(v[1]*v[1] <= ymax*ymax*cos2b);
	    return(v[0] >= xmin*Math.sqrt(cos2b));
	}
	return(false);
    }

    /* Intersection of a segment with the zonal region.
     * Returns true when intersection exists.
     * Method: 
     * -- for small circles: (A^B).North = sin(lat) is within longitude limits
     * -- for intersection with great circle: (A^B)^(P^Q)
     */
    private boolean intersects(double[] u0, double[] u1) {
	// 0. Is an intersection possible ?
	/*--- 
	if ((u0[2] > zmax) && (u1[2] > zmax))
	    return(false);
	if ((u0[2] < zmin) && (u1[2] < zmin))
	    return(false);
	---*/

	// 1. Search intersections with latitude circles
	double s = Coo.dotprod(u0, u1);
	double[] v = new double[3];
	Coo.vecprod(u0, u1, v);		// v = u0^u1
	double x, y, z, cosb, tanb, den;
	int i;
	for (i=0; i<2; i++) {
	    z = i == 0 ? zmin : zmax;
	    cosb = Math.sqrt(1.-z*z);
	    double A = cosb*v[0];
	    double B = cosb*v[1];
	    double C =    z*v[2];
	    // Longitude of intersection is solution of 
	    // A.cos(lon) + B.sin(lon) + C = 0
	    // Solve in t=tan(lon/2) with 
	    // cos(lon)=(1-t^2)/(1+t^2), sin(lon)=2t/(1+t^2)
	    double D = A*A + B*B - C*C;
	    if(DEBUG) {
		Coo c1 = new Coo(u0[0], u0[1], u0[2]);
		Coo c2 = new Coo(u1[0], u1[1], u1[2]);
		System.out.println("#...inter(z=" + z + ") " 
			+ c1 + "->" + c2 + " : " + (D>=0));
	    }
	    if (D<0) continue;
	    if (dlon >= 180.) return(true);
	    double[] t = new double[2];
	    int nt = 1;			// Number of solutions
	    den = C-A;
	    if (den == 0.) 		// 1 solution
		t[0] = A/B;
	    else {			// 2 solutions
		D = Math.sqrt(D);
	        t[0] = (-B-D)/den;
	        t[1] = (-B+D)/den;
		nt = 2;
	    }
	    if (DEBUG) System.out.println("#      tmax=" + tmax + ": "
		    + nt + "solutions: " + t[0] + "," + t[1]);
	    while (--nt >= 0) {
		if (Math.abs(t[nt]) > tmax) continue;	// Outside LON limits
		// Verify the intersection is between u0 and u1
		den = 1.0+t[nt]*t[nt]; 
		x = cosb * (1.0 - t[nt]*t[nt]) / den;
		y = cosb * (2.0 * t[nt])       / den;
		if (((x*u0[0]+y*u0[1]+z*u0[2])>=s) &&
		    ((x*u1[0]+y*u1[1]+z*u1[2])>=s))
		    return(true);
	    }
	}

	// 2. Search intersections with longitude circles.
	// The intersection of the great circle from u0(x0 y0 z0) to
	// u1 (x1 y1 z1) with the great circle lon=xmin is
	//  | xmin   x0  x1 |
	//  | ymax   y0  y1 | = 0
	//  | tan(b) z0  z1 |
	if (dlon >= 180.) return(false);

	double num1 = -xmin*v[0]/v[2];
	double num2 = -ymax*v[1]/v[2];
	if(DEBUG) {
	    Coo c1 = new Coo(u0[0], u0[1], u0[2]);
	    Coo c2 = new Coo(u1[0], u1[1], u1[2]);
	    System.out.println("#...solutions(lon) in " + c1 + " -> " + c2
		    + " tanb=" + (num1+num2) + "," + (num1-num2));
	}
	for (i=0; i<2; i++) {
	    tanb = i == 0 ? num1+num2 : num1-num2;
	    cosb = 1./Math.sqrt(1.+tanb*tanb);
	    z = tanb*cosb;
	    if ((z>=zmin) && (z<=zmax)) {
	        x = cosb*xmin;
	        y = cosb*ymax; if(i>0) y = -y;
		if (((x*u0[0]+y*u0[1]+z*u0[2])>=s) && 
		    ((x*u1[0]+y*u1[1]+z*u1[2])>=s)) 
		    return(true);
	    }
	}
	return(false);
    }

    /** 
     * Verify a circle intersects a Zone
     * @param	centre  center of circle
     * @param   radius  radius of circle
     * @return	DISJOINT / INTERSECTS / INCLUDES / IS_PARTOF
     *          for no intersection / overlap / cercle fully included
     **/
     int iCircle(Coo centre, double radius, boolean deep_check) {
	// Verify an invalid centre ?
	if (R == null) return(DISJOINT);
	Coo c = new Coo(centre);
	if (DEBUG) {
	    String type = deep_check ? "[deep]" : "[light]";
	    System.out.println("#...Zone.checkCircle(" + c + 
		", r=" + radius + ")" + type);
	}
	c.rotate(R);
	// Verify the circle is fully inside the zone:
	// ... on Latitude
	double clat = c.getLat();
	if ((clat-radius) > lat1) return(DISJOINT);
	if ((clat+radius) < lat0) return(DISJOINT);
	if (dlon >= 180.) return(
		((clat+radius)<=lat1) && ((clat-radius)>= lat0) ?
		INCLUDES :
		INTERSECTS);
	// ... on Longitude (between -dlon and +dlon)*sec(dec)
	double clon = c.getLon();
	if (clon > 180.) clon -= 360.;
	clon = Math.abs(clon);
	if (clon > (dlon + radius*smax))
	    return(DISJOINT);
	if (!deep_check)	// light check: it's all
	    return(INTERSECTS);
	// Look at the 4 corners of the zone.
	int nin = 0;
	Coo point = new Coo();
	double s2r = 2.*AstroMath.sind(radius/2.); s2r *= s2r;
	point.set(lat0, -clon);   if (c.dist2(point) <= s2r) nin++;
	point.set(point.x, -point.y,  point.z);
	                          if (c.dist2(point) <= s2r) nin++;
	point.set(lat1, -clon);   if (c.dist2(point) <= s2r) nin++;
	point.set(point.x, -point.y,  point.z);
	                          if (c.dist2(point) <= s2r) nin++;
	if (DEBUG) System.out.println("#...circle(" + c + ") nin=" + nin);
	if (nin == 4) return (IS_PARTOF);
	if (nin != 0) return (INTERSECTS);
	// Here the 4 corners are outside the circle.
	// Can there be an intersection with +/-dlon ?
	// Method: extremum of circle in longitude is when sinb = sin(b0)/cos(r)
	double cosr = 1. - s2r/2.;	// cos(r) = 1 - 2sin^2(r/2)
	double zext = c.z/cosr;
	if (Math.abs(zext) >= 1)	// Pole included in circle
	    return (INTERSECTS);
	double cosdl = cosr*Math.sqrt((1.-zext*zext)/(c.x*c.x+c.y*c.y));
	double dl = 90. - AstroMath.asind(cosdl);
	if (clon > (dlon+dl)) return(DISJOINT);
	// Verification the intersection is outside the zone...
	// TODO...
	// No intersection, outside or included
	return((c.z<=zmax) && (c.z>=zmin) && (clon < dlon) ? 
		INCLUDES:		// Circle within zone
		INTERSECTS);
    }

    /** 
     * Verify a circle intersects a Zone
     * @param	centre  center of circle
     * @param   radius  radius of circle
     * @return	DISJOINT / INTERSECTS / INCLUDES / IS_PARTOF
     *          for no intersection / overlap / cercle fully included
     **/
    public int checkCircle(Coo centre, double radius) {
	return(iCircle(centre, radius, true));
    }

    /** 
     * Verify intersection Qbox + Zone (polygon)
     * @param	qbox the Qbox to check
     * @return	DISJOINT / INCLUDES / INTERSECTS / IS_PARTOF
     **/
    public int checkQbox(int qbox) {
	if (centroid == null) return(DISJOINT);
        Qbox abox = new Qbox();
    	abox.set(qbox);
	/* Fast elimination */
	int i = iCircle(abox.center(), Qbox.maxRadius(qbox), false);
	if(DEBUG) System.out.println("#...................=" + i);
	if (i<INTERSECTS) 			// DISJOINT or INCLUDES
	    return(i);
	
    	// Check more accurately: can the box be completely included ?
    	// Count number of corners inside the Zone
    	double[][] u4 = new double[5][3];
    	Qbox.ucorners(abox.qbox, u4);

    	int inside=0;
    	for (i=0; i<4; i++) {
	    Coo.rotateVector(R, u4[i]);
	    if (DEBUG) {
		Coo c = new Coo(u4[i][0], u4[i][1], u4[i][2]);
		System.out.print("#   Corner#" + i + " = " + c);
		c.rotate_1(R);
		System.out.println(" (" + c + ")");
	    }
	    if ((u4[i][2]>=zmin) && (u4[i][2]<=zmax)) {
		if (u4[i][0] >= xmin) inside++;	// includes [0,360]
		else { double 
		    cosb = Math.sqrt(u4[i][0]*u4[i][0]+u4[i][1]*u4[i][1]);
		    if (u4[i][0] >= (xmin*cosb)) inside++;
		}
	    }
    	}
    	if (DEBUG) System.out.println("#..." + abox + abox.center()
		+ " (xmin=" + xmin + "): inside=" + inside);
	if ((inside>0) && (inside<4))
	    return(INTERSECTS);
	// Close path:
	u4[4][0] = u4[0][0]; 
	u4[4][1] = u4[0][1]; 
	u4[4][2] = u4[0][2];
	// If there is any intersection ==> INTERSECTS
	for (i=0; i<4; i++) {
	    if (DEBUG) {
		Coo c1 = new Coo(u4[i][0], u4[i][1], u4[i][2]);
		Coo c2 = new Coo(u4[i+1][0], u4[i+1][1], u4[i+1][2]);
		System.out.println("#...intersection " + c1 + " -> " + c2 +
			" = " + intersects(u4[i], u4[i+1]));
	    }
	    if(intersects(u4[i], u4[i+1])) {
		return (INTERSECTS);
	    }
	}
	if (inside == 4) { 	// No intersection, Qbox inside
	    return (INCLUDES);
	}
	// No intersection between Qbox and zone
        if (DEBUG) System.out.println("#...DISJOINT/IS_PARTOF: " + 
		(abox.inside(centroid) ? IS_PARTOF : DISJOINT));
	return(abox.inside(centroid) ?
		IS_PARTOF :
		DISJOINT);
    }
}

