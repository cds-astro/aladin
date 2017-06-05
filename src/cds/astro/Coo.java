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

package cds.astro ;

/*==================================================================
                Coo class
 *==================================================================*/
import java.io.Serializable;
//import java.util.*;
// for parseException
import java.text.ParseException;

/**
 * Class that manipulates the coordinates defining a point on the 
 * celestial sphere.
 * The class includes conversions between polar angles (lon,lat)
 * expressed in degrees, and Cartesian 3-vectors.
 * The typical way of converting between polar and cartesian is:
 * <PRE>
 * Coo aCoo = new Coo ; double u[] = new double[3] ;
 * while (true) {
 *     aCoo.set(stdin.readLine()) ;
 *     System.out.println("Coordonnees   : " + aCoo) ;
 *     u[0] = aCoo.x; u[1] = aCoo.y; u[2] = aCoo.z;
 *     System.out.println("Cos. directeurs: " + Coo.toString(u)) ;
 * }
 * </PRE>
 * This class also deals with 3x3 matrices.
 * @author Pierre Fernique, Francois Ochsenbein [CDS]
 * @version 1.0 : 03-Mar-2000<br>
 * @version 1.1 : 24-Mar-2000: Bug in dist<br>
 * @version 1.2 : 17-Apr-2002: Accept decimal degrees in scientific notation<br>
 * @version 1.3 : 17-Sep-2002: Method "dist" between 2 coordinates<br>
 * @version 1.4 : 21-Jan 2004 (BB): ajout de la methode equals.<br>
 * @version 1.5 : 12-Aug 2004: methods rotate rotate_1
 * 				dlon and dlat moved to Astrocoo
 * @version 1.6 : 24-Apr-2006: methods moveMatrix
 * @version 1.7 : 04-Jun-2006: methods add sub
 * @version 1.8 : 02-Feb-2007: editingDecimals
 * @version 1.81: 07-May-2007: parseSexa when less than 5 numbers for lon+lat
 * @version 1.82: 07-May-2007: check NaN values in set
 * @version 1.9 : 12-Jun-2008: Added vecprod and dotproc variations + distc
 * 				+ posAngle + perpendicular
 * @version 1.91: 12-Dec-2008: Take care of NULL vector (x=y=z=0)
 * @version 1.92: 03-Apr-2009: Edition of matrix toString()
 */

public class Coo implements Serializable, Cloneable {
    /** Components of unit vector (direction cosines) */
    public double x, y, z ;
    /** Longitude in degrees, range [0, 360[ */
    protected double lon;
    /** Latitude in degrees, range [-90, +90]  */
    protected double lat ;
    /** Number of decimals on lon and lat -- reported to subclass */
    // public byte dlon, dlat ;
    /** Whether longitude part was in time units (Equatorial) */
    // public boolean eq;

    /** The edition of Coordinates */
    static public Editing ed = new Editing("--");

    /** Number of decimals edited in the default toString method.
     * Can be changed with the setDecimals() method. */
    static public int decimals = -10;
    /** The unit 3-D unit matrix . */
    static public double Umatrix3[][] = { 
        { 1.0, 0.0, 0.0}, { 0.0, 1.0, 0.0}, { 0.0, 0.0, 1.0} };

    //  ===========================================================
    //		Constructors
    //  ===========================================================

    /**
     * The basic contructor: undefined position
     */
    public Coo() {
        this.set();
    }

    /**
     * Define a coordinate from another one (clone).
     * 	It is faster than the creation from the angles or vector
     * 	because there is no conversion between angles and vectors.
     * @param coo the coordinates to clone.
     */
    public Coo(Coo coo) {
        this.set(coo);
    }

    /**
     * Define a coordinate from its angles
     * @param lon longitude angle in degrees
     * @param lat latitude angle in degrees
     */
    public Coo(double lon, double lat) {
        this.set(lon, lat);
    }

    /**
     * Define a coordinate from its direction cosines.
     * (Note that (x,y,z) doesn't need to have a norm=1)
     * @param x x item in unit vector (direction cosines)
     * @param y y item in unit vector (direction cosines)
     * @param z z item in unit vector (direction cosines)
     */
    public Coo(double x, double y, double z) {
        this.set(x, y, z);
    }

    /**
     * Define a coordinate from a string
     * @param  text	a position as a string (decimal or sexagesimal)
     * @param  equatorial     true when text represents equatorial coordinates
     * 			(RA, if sexagesimal, is interpretated in time units)
     * @throws ParseException when the string is not interpretable
     */
    /* --- Not useful -- remove
    public Coo(String text, boolean equatorial) throws ParseException {
    	this.set(text, equatorial);
    }
     */

    /**
     * Define a coordinate from a string
     * @param  text	a position as a string
     * @throws ParseException when the string is not interpretable
     */
    public Coo(String text) throws ParseException {
        this.set(text);
    }

    /**
     * Clone the Coo object
     */
    public Object clone() {
        Coo c = null;
        try {
            c = (Coo) super.clone();
        } catch (CloneNotSupportedException e) {
            // should not happen
        }
        return c;
    }

    //  ===========================================================
    //		Polar angles (lon,lat) <--> Cartesian
    //  ===========================================================

    /**
     * Compute coordinates from x,y,z
     */
    private final void computeLonLat() {
        double r2 = x * x + y * y;
        lon = 0.0;
        if (r2 == 0.0) { /* in case of poles */
            if (z == 0.0) {
                lon = 0./0.; lat = 0./0.;
            }
            else
                lat = (z > 0.0) ? 90.0 : -90.0;
        }
        else {
            lon = AstroMath.atan2d(y, x);
            lat = AstroMath.atan2d(z, Math.sqrt(r2));
            if (lon < 0.0)
                lon += 360.0;
        }
    }

    /**
     * Sets the position to its default (unknown)
     */
    public void set() {
        x = y = z = 0.;
        lon = 0./0.; lat = 0./0.;
    }

    /**
     * Set the position from an existing one.
     * 	It is faster than the set methods from angles or vector components
     * 	because there is no conversion.
     * @param coo the coordinates
     */
    public void set(Coo coo) {
        this.x = coo.x;
        this.y = coo.y;
        this.z = coo.z;
        this.lon = coo.lon;
        this.lat = coo.lat;
    }

    /**
     * Compute the unit vector from angles in degrees.
     * @param lon longitude in degrees
     * @param lat latitude angle in degrees
     * @param u   resulting unit vector
     */
    static public final void setUvec(double lon, double lat, double[] u) {
        u[0] = u[1] = AstroMath.cosd(lat);
        u[0] *= AstroMath.cosd(lon);
        u[1] *= AstroMath.sind(lon);
        u[2] = AstroMath.sind(lat);
        if (Double.isNaN(u[0])) { u[0] = u[1] = u[2] = 0; }
    }

    /**
     * Set a position from its longitude and latitude (RA/Dec).
     * Convert (lon,lat) into its direction cosines (x,y,z)
     * @param lon longitude in degrees
     * @param lat latitude angle in degrees
     */
    public void set(double lon, double lat) {
// PF - NOV 2016 - PAS COMPATIBLE AVEC CALIB.JAVA - J'ENLEVE POUR REVENIR COMME C'ETAIT AVANT
//        if (lon < -180 || 360 < lon) {
//            throw new IllegalArgumentException("lon value '" + lon + "' must be"
//                    + " in [-180, 360]");
//            // In galactic, ]-180, 180] may be used instead of [0, 360[
//        }
//        if (lat < -90 || 90 < lat) {
//            throw new IllegalArgumentException("lat value '" + lat + "' must be"
//                    + " in [-90, 90]");
//        }
        double coslat = AstroMath.cosd(lat);
        this.lon = lon; this.lat = lat;
        if (Double.isNaN(lon) || Double.isNaN(lat)) { this.set(); return; }
        x = coslat * AstroMath.cosd(lon);
        y = coslat * AstroMath.sind(lon);
        z = AstroMath.sind(lat);
    }

    /**
     * Set a position from its unit vectors.
     * Revert conversion of (x,y,z) into (lon,lat).<br>
     * (Note that (x,y,z) is assxumed to have a norm=1)
     * @param x x item in unit vector (direction cosines)
     * @param y y item in unit vector (direction cosines)
     * @param z z item in unit vector (direction cosines)
     */
    public void set(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z))
            this.x = this.y = this.z = 0;
        lon = lat = 0./0.;
        computeLonLat();
    }

    /**
     * Define a coordinate as the direct perpendicular to 2 positions.
     * Is normalized vectorial product.
     * @param  pos1	first position
     * @param  pos2	second position
     * @return coordinate directly perpendicular pos1{perp}pos2
     */
    static public Coo perpendicular(Coo pos1, Coo pos2) {
        double[] u3 = new double[3];
        pos1.vecprod(pos2, u3);
        double norm = Math.sqrt(u3[0]*u3[0]+u3[1]*u3[1]+u3[2]*u3[2]);
        if (norm < 1.e-15) return(new Coo());
        return(new Coo(u3[0]/norm, u3[1]/norm, u3[2]/norm));
    }

    //  ===========================================================
    //		Interpret a string for Position
    //  ===========================================================

    //* REMOVED --- used only in Astrocoo
    /**
     * Compute the number of decimals.
     * @param  nd   number of decimals
     * @param  form the format
     **/
    /* ----------------
    private static final int set_nd(int decimals, int form) {
	int nd = decimals+1;
	if (nd < 0) nd = 0;
	if (nd >31) nd =31;
	if (form >= Parsing.SEXA2) {
	    nd += 2;
	    if ((form&1)!=0) nd += 2;
	    nd |= 128;		// Indicates SEXAgesimal
	}
	return(nd);
    }
    ----*/

    /**
     * Interpret the string and convert to Coo.
     *               Called from set(...) and parse(...) methods
     * @param  acoo	a text ready for parsing
     *			(may contain the hms or {deg}'" characters)
     * @return true if OK.
     */
    public boolean parsing (Parsing acoo) {
        boolean f15 = false;
        x = y = z = 0;		// Nothing found yet.
        // Extract the longitude / RA
        acoo.gobbleSpaces();
        lon  = acoo.parseSexa2();	// First part of 2 sexagesimal values
        if (acoo.inError()) return(false);
        if (acoo.isTime()) f15 = true;
        else if (acoo.isAngle()) f15 = false;
        if (f15) lon *= 15.;	// { lon *= 15.; nd--; }
        // Extract the Latitude / Declination
        acoo.gobbleSpaces();
        if (acoo.currentChar() == ',') { // Accept HEASARC way with a comma
            acoo.advance(1);
            acoo.gobbleSpaces();
        }
        lat  = acoo.parseSexa();
        if (acoo.inError()) return(false);
        // Both coordinates OK.
        this.set(lon, lat);
        return(true);
    }

    /**
     * Interpret the string and convert to Coo.
     * @param  text	a text containing 2 angles, in decimal or Sexagesimal
     *			(may contain the hms or {deg}'" characters)
     * @param  offset	where to start the analysis
     * @return new position in text after interpretation.
     */
    public int parse(String text, int offset) {
        Parsing acoo = new Parsing(text, offset);
        // Interpret, and in case of error return original offset
        return(parsing(acoo) ? acoo.pos : offset) ;
    }

    /**
     * Define a coordinate from its angles.
     *
     * @param  text	a text containing 2 angles, in decimal or Sexagesimal
     // @param  equatorial	true when text represents equatorial coordinates
     //			(the RA in units of time)
     * @throws ParseException when the tring is not interpretable
     */
    /*
    public void set(String text, boolean equatorial)
	   throws ParseException {
      Parsing acoo = new Parsing(text);
	if (!parsing(acoo, false)) throw new ParseException
	    ("****Coo: component larger than 60 in: " + text, acoo.pos);
    	// Error Check -- is there something left in the string ?
	acoo.gobbleSpaces();
	if (acoo.pos < acoo.length) throw new ParseException
	    ("****Coo: '" + text + "'+" + acoo.pos, acoo.pos);
    	set(lon, lat);
    }
     */

    /**
     * Define a non-equatorial coordinate from its text (RA is in degrees)
     * @param  text	a text containing 2 angles, in decimal or Sexagesimal
     * @throws ParseException when the string is not interpretable
     */
    public void set(String text) throws ParseException {
        Parsing acoo = new Parsing(text);
        if (!parsing(acoo)) throw new ParseException
        ("****Coo: component larger than 60 in: " + text, acoo.pos);
        // Error Check -- is there something left in the string ?
        acoo.gobbleSpaces();
        if (acoo.pos < acoo.length) throw new ParseException
        ("****Coo: '" + text + "'+" + acoo.pos, acoo.pos);
        set(lon, lat);
    }

    /**
     * Define equatorial coordinate for its angles (the RA in units of time)
     * @param  text	a text containing 2 angles, in decimal or Sexagesimal
     * @throws ParseException when the string is not interpretable
     */
    /* Not really useful
     public void setEquatorial(String text) throws ParseException {
    	 this.set(text) ;
     }
     */

    //  ===========================================================
    //		Get special components
    //  ===========================================================

    /**
     * Get the Longitude (RA) in degrees.
     * @return   the longitude (RA) in degrees
     **/
    public double getLon() {
        if (Double.isNaN(lat)) computeLonLat();
        return(lon);
    }

    /**
     * Get the Latitude (Dec) in degrees.
     * @return   the latitude (Dec) in degrees
     **/
    public double getLat() {
        if (Double.isNaN(lat)) computeLonLat();
        return(lat);
    }

    /**
     * Get the spherical angles (lon, lat) as a 2-vector
     **/
    public void copyAngles(double[] o) {
        if (Double.isNaN(lat)) computeLonLat();
        o[0] = lon; o[1] = lat; 
    }

    /**
     * Get the unit vector (x, y, z) as a 3-vector
     **/
    public void copyUvector(double[] u) {
        u[0] = x; u[1] = y; u[2] = z;
    }

    //  ===========================================================
    //		Spherical distance
    //  ===========================================================

    /**
     * Distance between 2 points on the sphere.
     * @param lon1 longitude of first point in degrees
     * @param lat1 latitude of first point in degrees
     * @param lon2 longitude of second point in degrees
     * @param lat2 latitude of second point in degrees
     * @return distance in degrees in range [0, 180]
     **/
    public static final double distance(double lon1, double lat1,
            double lon2, double lat2) {
        double c1 = AstroMath.cosd(lat1);
        double c2 = AstroMath.cosd(lat2);
        double w, r2;
        w = c1 * AstroMath.cosd(lon1) - c2 * AstroMath.cosd(lon2);
        r2 = w * w;
        w = c1 * AstroMath.sind(lon1) - c2 * AstroMath.sind(lon2);
        r2 += w * w;
        w = AstroMath.sind(lat1) - AstroMath.sind(lat2);
        r2 += w * w; // 4.sin^2(r/2)
        return (2. * AstroMath.asind(0.5 * Math.sqrt(r2)));
    }

    /**
     * Squared distance between 2 points (= 4.sin<sup>2</sup>(r/2))
     * @param  pos      another position on the sphere
     * @return ||pos-this||<sup>2</sup> = 4.sin<sup>2</sup>(r/2)
     **/
    public final double dist2(Coo pos) {
        if ((this.x==0)&&(this.y==0)&&(this.z==0)) return(0./0.);
        if ((pos.x==0)&&(pos.y==0)&&(pos.z==0)) return(0./0.);
        double w = pos.x - x;
        double r2 = w * w;
        w = pos.y - y; r2 += w * w;
        w = pos.z - z; r2 += w * w;
        return (r2);
    }

    /**
     * Squared distance between point and a vector
     * @param  u        a 3-vector
     * @return ||u-this||<sup>2</sup>
     **/
    public final double dist2(double[] u) {
        if ((this.x==0)&&(this.y==0)&&(this.z==0)) return(0./0.);
        if ((u[0]==0)&&(u[1]==0)&&(u[2]==0)) return(0./0.);
        double w = u[0] - x;
        double r2 = w * w;
        w = u[1] - y; r2 += w * w;
        w = u[2] - z; r2 += w * w;
        return (r2);
    }

    /**
     * Squared distance between 2 vectors
     * @param  u        a 3-vector
     * @param  v        a 3-vector
     * @return ||u-v||<sup>2</sup>
     **/
    public static final double dist2(double[] u, double[] v) {
        double w = u[0] - v[0];
        double r2 = w * w;
        w = u[1] - v[1]; r2 += w * w;
        w = u[2] - v[2]; r2 += w * w;
        return (r2);
    }

    /**
     * Distance between 2 points on the sphere.
     * @param  pos another position on the sphere
     * @return distance in degrees in range [0, 180]
     **/
    public final double distance(Coo pos) {
        // Take care of NaN:
        if ((pos.x==0)&&(pos.y==0)&&(pos.z==0))
            return(0./0.);
        if ((this.x==0)&&(this.y==0)&&(this.z==0))
            return(0./0.);
        return (2. * AstroMath.asind(0.5 * Math.sqrt(dist2(pos))));
    }

    /**
     * Distance between a point and a circle (defined by 2 coordinates).
     * @param  pos1 First point defining the circle
     * @param  pos2 Second point defining the circle
     * @return distance to great circle (pos1, pos2). 
     * The distance is always between 0 and 90deg.
     **/
    public final double distc(Coo pos1, Coo pos2) {
        // Verify undefined vector coordinate ?
        if ((this.x==0)&&(this.y==0)&&(this.z==0))
            return(0./0.);
        // Compute vectorial product
        double[] v = new double[3];
        pos1.vecprod(pos2, v); 
        double n = normalize(v);
        if (n>0)
            return(AstroMath.asind(Math.abs(dotprod(v))));
        else return(0./0.);	// NaN 
    }

    /**
     * Angle with 2 directions.
     * @param  pos1 First point A
     * @param  pos2 Second point B
     * @return angle (in degrees) AOB
     * The distance is between 0 and 180deg.
     **/
    public final double angle(Coo pos1, Coo pos2) {
        if ((this.x==0)&&(this.y==0)&&(this.z==0))
            return(0./0.);
        // Compute vectorial product
        double[] v1 = new double[3];
        double[] v2 = new double[3];
        this.vecprod(pos1, v1); normalize(v1);
        this.vecprod(pos2, v2); normalize(v2);
        return(90.-AstroMath.asind(dotprod(v1, v2)));
    }

    /**
     * Position angle (wrt North) of point (range 0-180).
     * Identical to angle(North, pos)
     * @param  pos  Point
     * @return position angle (in degrees) of pos.
     **/
    public final double posAngle(Coo pos) {
        if ((this.x==0)&&(this.y==0)&&(this.z==0))
            return(0./0.);
        /* -- Other method
	double ds = (this.x-pos.x);
	double s2 = ds*ds;
	ds = (this.y-pos.y); s2 += ds*ds;
	ds = (this.z-pos.z); s2 += ds*ds;
	double sinpa = (this.x*pos.y - this.y*pos.x)/Math.sqrt(x*x+y*y);
	sinpa /= Math.sqrt(s2*(1.-s2*s2/4.));
	double pa = AstroMath.asind(sinpa);
	if (pos.z<this.z) pa = 180. - pa;
	--*/
        double Z = pos.z*(this.x*this.x+this.y*this.y) 
                - this.z*(this.x*pos.x+this.y*pos.y);
        double Y = this.x*pos.y - this.y*pos.x;
        double pa = 90. - AstroMath.atan2d(Z, Y);
        if (pa<0) pa += 360.;
        return(pa);
    }

    //  ===========================================================
    //		Vectorial & dot products.
    //  ===========================================================

    /** Compute the dot-product (scalar product)
     * @param	v1 first vector
     * @param   v2 second vector
     * @return	the dot-product
     **/
    public static final double dotprod(double[] v1, double[] v2) {
        return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]);
    }

    /** Compute the dot-product (scalar product)
     * @param	v second vector
     * @return	the dot-product = (this) . v
     **/
    public final double dotprod(double[] v) {
        return (x*v[0] + y*v[1] + z*v[2]);
    }

    /** Compute the dot-product (scalar product)
     * @param	pos second vector
     * @return	the dot-product = (this) . v
     **/
    public final double dotprod(Coo pos) {
        return (this.x*pos.x + this.y*pos.y + this.z*pos.z);
    }

    /** Compute the square norm of a vector
     * @param	v1 the vector
     * @return	the squared norm
     **/
    public static final double norm2(double[] v1) {
        return (dotprod(v1, v1));
    }

    /** Compute the vectorial product (result in vector r)
     * @param	v1 first vector
     * @param   v2 second vector
     * @param   r the returned resulting vector
     **/
    public static final void vecprod(double[] v1, double[] v2, double[] r) {
        r[0] = v1[1]*v2[2] - v1[2]*v2[1];
        r[1] = v1[2]*v2[0] - v1[0]*v2[2];
        r[2] = v1[0]*v2[1] - v1[1]*v2[0];
    }

    /** Compute the vectorial product (result in vector r)
     * @param	v second vector
     * @param   r the returned resulting vector r = this ^ v
     **/
    public final void vecprod(double[] v, double[] r) {
        r[0] = y*v[2] - z*v[1];
        r[1] = z*v[0] - x*v[2];
        r[2] = x*v[1] - y*v[0];
    }

    /** Compute the vectorial product (result in vector r)
     * @param	pos second vector
     * @param   r the returned resulting vector r = this ^ v
     **/
    public final void vecprod(Coo pos, double[] r) {
        r[0] = this.y*pos.z - this.z*pos.y;
        r[1] = this.z*pos.x - this.x*pos.z;
        r[2] = this.x*pos.y - this.y*pos.x;
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

    //  ===========================================================
    //		Apply a rotation matrix
    //  ===========================================================

    /**
     * Rotate a vector.
     * The vector <b>v</b> becomes <b>R . v</b>
     * @param R [3][3] Rotation Matrix
     * @param v Vector to rotate, may be of dimension 3 or 6.
     */
    public static final void rotateVector(double[][] R, double[] v) {
        int i; double x, y, z;
        if (R == Umatrix3) return;
        for (i=0; i<v.length; i+=3) {
            x = v[i]; y = v[i+1]; z = v[i+2];
            v[i+0] = R[0][0]*x + R[0][1]*y + R[0][2]*z;
            v[i+1] = R[1][0]*x + R[1][1]*y + R[1][2]*z;
            v[i+2] = R[2][0]*x + R[2][1]*y + R[2][2]*z;
        }
    }

    /**
     * Reversely rotate a vector.
     * The vector <b>v</b> becomes <b>R<sup>-1</sup> . v</b>
     * @param R [3][3] Rotation Matrix
     * @param v Vector to rotate, may be of dimension 3 or 6.
     */
    public static final void rotateVector_1(double[][] R, double[] v) {
        int i; double x, y, z;
        if (R == Umatrix3) return;
        for (i=0; i<v.length; i+=3) {
            x = v[i]; y = v[i+1]; z = v[i+2];
            v[i+0] = R[0][0]*x + R[1][0]*y + R[2][0]*z;
            v[i+1] = R[0][1]*x + R[1][1]*y + R[2][1]*z;
            v[i+2] = R[0][2]*x + R[1][2]*y + R[2][2]*z;
        }
    }

    /**
     * Rotate a coordinate (apply a rotation to the position).
     * @param R [3][3] Rotation Matrix
     */
    public void rotate (double[][] R) {
        double X, Y, Z;
        if (R == Umatrix3) return;
        X = R[0][0]*x + R[0][1]*y + R[0][2]*z;
        Y = R[1][0]*x + R[1][1]*y + R[1][2]*z;
        Z = R[2][0]*x + R[2][1]*y + R[2][2]*z;
        // this.set(X, Y, Z); Not necessary to compute positions each time.
        x = X; y = Y; z = Z;
        lon = lat = 0./0.;
    }

    /**
     * Rotate a coordinate (apply a rotation to the position)
     * 		in reverse direction.
     * 	The method is the inverse of rotate.
     * @param R [3][3] Rotation Matrix
     */
    public void rotate_1 (double[][] R) {
        double X, Y, Z;
        if (R == Umatrix3) return;
        X = R[0][0]*x + R[1][0]*y + R[2][0]*z;
        Y = R[0][1]*x + R[1][1]*y + R[2][1]*z;
        Z = R[0][2]*x + R[1][2]*y + R[2][2]*z;
        // this.set(X, Y, Z); Not necessary to compute positions each time.
        x = X; y = Y; z = Z;
        lon = lat = 0./0.;
    }

    /** Generate the rotation matrix from the Euler angles
     * @param z	Euler angle
     * @param theta	Euler angle
     * @param zeta	Euler angles
     * @return R [3][3]		the rotation matrix
     * The rotation matrix is defined by:<pre>
     *    R =      R_z(-z)      *        R_y(theta)     *     R_z(-zeta)
             |cos.z -sin.z  0|   |cos.the  0 -sin.the|   |cos.zet -sin.zet 0|
	   = |sin.z  cos.z  0| x |   0     1     0   | x |sin.zet  cos.zet 0|
	     |   0      0   1|   |sin.the  0  cos.the|   |   0        0    1|
     * </pre>
     */
    public static double[][] eulerMatrix(double z, double theta, double zeta) {
        double R[][] = new double[3][3];
        R[0][2] =  AstroMath.cosd(z);
        R[1][2] =  AstroMath.sind(z);
        R[2][2] =  AstroMath.cosd(theta);
        double  w =  AstroMath.sind(theta) ;
        R[2][0] =  AstroMath.cosd(zeta);
        R[2][1] =  AstroMath.sind(zeta);
        R[0][0] =  R[2][0]*R[2][2]*R[0][2] - R[2][1]*R[1][2];
        R[1][0] =  R[2][0]*R[2][2]*R[1][2] + R[2][1]*R[0][2];
        R[0][1] = -R[2][1]*R[2][2]*R[0][2] - R[2][0]*R[1][2];
        R[1][1] = -R[2][1]*R[2][2]*R[1][2] + R[2][0]*R[0][2];
        R[2][0] =  R[2][0]*w;
        R[2][1] = -R[2][1]*w;
        R[0][2] = -w*R[0][2];
        R[1][2] = -w*R[1][2];
        //* System.err.println("Compute tR . R ") ;
        //* System.err.println(Coo.toString(Coo.prod(Coo.t(R), R))) ;
        return(R) ;
    }

    /**
     * Compute the rotation matrix that transforms a direction
     * into the local frame.
     * The axises of the local frame are defined by:
     *  <UL>
     *  <LI>R[0] (first axis)  = unit vector towards Zenith
     *  <LI>R[1] (second axis) = unit vector towards East
     *  <LI>R[2] (third axis)  = unit vector towards North
     *  </UL><PRE>
     *  +-                             -+   +-                  -+
     *  | cosb.cosl   cosb.sinl    sinb |   |  x        y      z |
     *  |  -sinl        cosl        0   | = | (-y/r)   (x/r)   0 |
     *  |-sinb.cosl  -sinb.sinl    cosb |   |-z(x/r)  z(-y/r)  r |
     *  +-                             -+   +-                  -+
     *  </PRE>
     * r = sqrt(x*x+y*y) ; if r==0,take (x/r)=1, (y/r)=0
     * @param lon  longitude of the center of local frame
     * @param lat  latitude  of the center of local frame
     * @return R[3][3] = Rotation Matrix  [(x,y,z)_local = R * (x,y,z)_global].
     *     With the result matrix, <b>rotate(R)</b> converts the position 
     *     to (0,0).
     */
    public static final double[][] localMatrix(double lon, double lat) {
        double R[][] = new double[3][3];
        R[2][2] =   AstroMath.cosd(lat);
        R[0][2] =   AstroMath.sind(lat);
        R[1][1] =   AstroMath.cosd(lon);
        R[1][0] =  -AstroMath.sind(lon);
        R[1][2] =  0.0;
        R[0][0] =  R[2][2] * R[1][1];
        R[0][1] = -R[2][2] * R[1][0];
        R[2][0] = -R[0][2] * R[1][1];
        R[2][1] =  R[0][2] * R[1][0];
        return (R);
    }

    /**
     * Compute the rotation matrix associated to current position.
     * Simple mathematics R[0][i] = <b>I</b> (x y z)
     *                    R[1][i] = <b>J</b> (u v 0) 
     *                    R[2][i] = <b>K</b> (u v 0) 
     * @param R = Rotation Matrix[3][3] to transform the current position 
     * 			into (0,0)
     */
    public final void localMatrix(double[][] R) {
        double r = Math.sqrt(x*x+y*y);
        R[0][0] =  x;  R[0][1] =  y;   R[0][2] =  z;
        if (r == 0.) { R[1][0] = 0;    R[1][1] = 1.; }
        else         { R[1][0] = -y/r; R[1][1] = x/r; }
        R[1][2] =  0.0;
        R[2][0] = -z * R[1][1];
        R[2][1] =  z * R[1][0];
        R[2][2] =  r;
    }

    /**
     * Compute the rotation matrix associated to current position.
     * @return R[3][3] = Rotation Matrix to transform the current position 
     * 			into (0,0)
     */
    public final double[][] localMatrix() {
        double R[][] = new double[3][3];
        this.localMatrix(R);
        return (R);
    }

    /**
     * Compute the rotation matrix to move between 2 directions.
     *  		This matrix is coo.localMatrix * t(this.localMatrix) 
     * @param  coo2  target direction.
     * @return R[3][3] = Rotation Matrix to transform the current position 
     * 			into the position given in argument
     */
    public final double[][] moveMatrix(Coo coo2) {
        double R[][] = new double[3][3];
        double R1[][] = this.localMatrix();
        double R2[][] = coo2.localMatrix();
        int i,j;

        for (i=0; i<3; i++) for (j=0; j<3; j++) 
            R[i][j] = R2[0][i]*R1[0][j] 
                    + R2[1][i]*R1[1][j] 
                            + R2[2][i]*R1[2][j];
        return (R);
    }

    //  ===========================================================
    //		Normalisation
    //  ===========================================================

    /**
     * Normalisation of a vector (set its norm to 1)
     * @param   u  3- or 6-Vector to normalize
     * @return the norm of the 3-D vector
     */
    public static final double normalize(double[] u) {
        double n = Math.sqrt(u[0]*u[0] + u[1]*u[1] + u[2]*u[2]);
        if (n>0) {
            u[0] /= n; u[1] /= n; u[2] /= n; 
            if (u.length > 3) {
                u[3] /= n; u[4] /= n; u[5] /= n;
            }
        }
        return(n);
    }

    /**
     * Normalisation of coordinates (set its norm to 1)
     * @return the norm of the 3-D vector
     */
    public double normalize() {
        double n = x*x + y*y + z*z;
        if (n>0) {
            n = Math.sqrt(n);
            x /= n; y /= n; z /= n; 
        }
        return(n);
    }

    //  ===========================================================
    //		Addition / Subtraction of Coordinates
    //  ===========================================================

    /**
     * Addition of a vector (addition, then normalisation)
     * @param   u  3- or 6-Vector to modify
     * @param  du  3-Vector to add
     * @return the norm of the 3-D vector
     */
    public static final double add(double[] u, double[] du) {
        u[0] += du[0]; u[1] += du[1]; u[2] += du[2];
        return(normalize(u));
    }

    /**
     * Subtraction of a vector (subtravtion, then normalisation)
     * @param   u  3- or 6-Vector to modify
     * @param  du  3-Vector to subtract
     * @return the norm of the 3-D vector
     */
    public static final double sub(double[] u, double[] du) {
        u[0] -= du[0]; u[1] -= du[1]; u[2] -= du[2]; 
        return(normalize(u));
    }

    /**
     * Addition of a vector (addition, then normalisation)
     * @param  du  3-Vector to add
     * @return the norm of the 3-D vector
     */
    public final double add(double[] du) {
        if ((x==0)&&(y==0)&&(z==0)) return(0);
        x += du[0];  y += du[1];  z += du[2];
        lon = 0./0.; lat = 0./0.;
        return(normalize());
    }

    /**
     * Subtraction of a vector (subtravtion, then normalisation)
     * @param  du  3-Vector to subtract
     * @return the norm of the 3-D vector
     */
    public final double sub(double[] du) {
        if ((x==0)&&(y==0)&&(z==0)) return(0);
        x -= du[0]; y -= du[1];  z -= du[2];
        lon = 0./0.; lat = 0./0.;
        return(normalize());
    }

    //  ===========================================================
    //		Comparison of 2 Coordinates.
    //  ===========================================================

    /**
     * Test equality of Coo.
     * @param o Object to compare
     * @return  True if o is identical to Coo.
     */
    public boolean equals(Object o) {
        boolean res = false;
        if(o instanceof Coo) {
            Coo c = (Coo)o;
            res = c.x == this.x && c.y == this.y && c.z == this.z;
        }
        return res;
    }

    /**
     * Compute the hashcode
     * @return the hascode value
     */
    public int hashCode() {
        long l = Double.doubleToLongBits(x);
        int hcode = (int) (l^(l >>> 32));
        l = Double.doubleToLongBits(y);
        hcode = 123 * hcode + (int) (l^(l >>> 32));
        l = Double.doubleToLongBits(z);
        hcode = 123 * hcode + (int) (l^(l >>> 32));
        return hcode;
    }

    //  ===========================================================
    //		Editions of Angles, uvectors and Matrices
    //  ===========================================================

    /**
     * Edition of the Coordinates with specified number of decimals.
     * @param  b the StringBuffer for edition
     * @param  nd the number of decimals in the edition of each coordinate.
     * 		Possible to use a negative nd to minimize the length.
     * @return  the StringBuffer
     */
    public final StringBuffer editCoo(StringBuffer b, int nd) {
        int o = b.length();

        //System.out.println("....editCoo("+lon+","+lat+") nd="+nd);

        if (Double.isNaN(lat)) computeLonLat();

        // We'll replace 360 by 0
        ed.editDecimal(b, lon, 3, nd, Editing.ZERO_FILL);
        //System.out.println("....editCoo(1): " + b);
        if ((b.charAt(o) == '3') && (b.charAt(o+1) == '6')) {
            String pb = b.substring(o+2);
            b.setLength(o);
            b.append("00");
            b.append(pb);
        }
        ed.editDecimal(b, lat, 3, nd, Editing.ZERO_FILL|Editing.SIGN_EDIT);
        return(b);
    }

    /**
     * Default Edition of the Coordinates, as 2 numbers expressing
     * the angles in degrees.
     * @param  b the StringBuffer for edition
     * @param  nd the number of decimals in the edition of each coordinate.
     * 		Possible to use a negative nd to minimize the length.
     * @return  the StringBuffer
     */
    public StringBuffer edit(StringBuffer b, int nd) {
        return(editCoo(b, nd)) ;
    }

    /**
     * Set (and get) the number of decimals for the default edition of the
     * cordinates.
     * @param  nd the number of decimals in the edition of each coordinate.
     * 		Possible to use a negative nd to minimize the length.
     * @return  the previous deifnition.
     */
    public static int setDecimals(int nd) {
        int ret = decimals;
        decimals = nd;
        return(ret);
    }

    /**
     * Default Edition of the Coordinates, as 2 numbers expressing
     * the angles in degrees.
     * @return The edited coordinates
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        editCoo(b, decimals);
        return (b.toString()); // Buffer converted to String
    }

    /**
     * Edition of the 3 components of a vector
     * @param buf Stringbuffer to use for edition.
     * @param u a vector to edit
     * @param offset first element to edit.
     */
    private static final StringBuffer ed3(StringBuffer buf, 
            double u[], int offset) {
        ed.editDecimal(buf, u[offset+0], 2, 15, Astroformat.SIGN_EDIT);
        buf.append(' ');
        ed.editDecimal(buf, u[offset+1], 2, 15, Astroformat.SIGN_EDIT);
        buf.append(' ');
        ed.editDecimal(buf, u[offset+2], 2, 15, Astroformat.SIGN_EDIT); 
        return(buf);
    }

    /**
     * Edition of the 3 components of a vector.
     * @param	u the 3-vector
     * @return	the equivalent string (edited vector)
     */
    protected static final String toString(double u[]) {
        return(toString("", u));
    }

    /**
     * Edition of the 3 components of a vector.
     * @param	title text prefixing each line.
     * @param	u the 3-vector
     * @return	the equivalent string (edited vector)
     */
    protected static final String toString(String title, double u[]) {
        StringBuffer b = new StringBuffer(title);
        int n = title.length();
        int i, o;
        ed3(b, u, 0);
        for (o=3; o < u.length; o += 3) {
            b.append('\n');
            for (i=0; i<n; i++) b.append(' ');
            ed3(b, u, o);
        }
        return (b.toString()); // Buffer converted to String
    }

    /**
     * Edition of a 3x3 matrix
     * @param title text prefixing each line of the matrix
     * @param m the 3x3 matrix
     * @return	the equivalent string (edited matrix)
     */
    protected static final String toString(String title, double m[][]) {
        StringBuffer b = new StringBuffer(title);
        int n = title.length();
        int i;
        if (m == null)  b.append(" (3x3matrix)(nil)");
        else for (int j=0; j<m.length; j++) {
            if (j>0) { b.append('\n'); for(i=0; i<n; i++) b.append(' '); }
            ed3(b, m[j], 0);
        }
        return (b.toString()); // Buffer converted to String
    }

    /**
     * Dump the contents of a Coordinate
     * @param title A title to precede the dump
     **/
    public void dump (String title) {
        StringBuffer b = new StringBuffer(256);
        int i = title.length();
        b.append(title); b.append("  "); this.editCoo(b, 12);
        b.append("\n"); while (--i>=0) b.append(' ');
        ed.editDecimal(b, x, 2, 15, Editing.SIGN_EDIT); b.append(' ');
        ed.editDecimal(b, y, 2, 15, Editing.SIGN_EDIT); b.append(' ');
        ed.editDecimal(b, z, 2, 15, Editing.SIGN_EDIT);
        System.out.println(b.toString());
    }
}
