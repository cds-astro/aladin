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
import java.text.*;	// for parseException

/**
 * Class defining the Mathematical Projections of the Celestial Sphere.
 * This class contains only the mathematical projections ---
 * WCS and Aladin projections are in a derived class.
 * <P>The available projections are defined by the formulae, where
 * <PRE>
 *          l,b   = longitude and latitude
 *          theta = angle to center of projection 
 *          x,y   = projections (cartesian) along x (East) and y (North)
 *          r,phi = projections (polar)
 * TAN      / Standard = Gnomonic     
 *            r = tan(theta)          phi
 * TAN2     / Stereographic           
 *            r = 2.tan(theta/2)      phi
 * SIN      / Orthographic  
 *            r = sin(theta)          phi
 * SIN2     / Equal-area    
 *            r = 2.sin(theta/2)      phi
 * ARC      / Schmidt proj. 
 *            r = theta               phi
 * AITOFF   / Aitoff (equal area)     if D = sqrt(0.5*(1+cos(b)cos(l/2)))
 *            x = 2cos(b)sin(l/2)/D   y = sin(b)/D
 * SANSON   / Global Sinusoidal (equal area)
 *            x = l cos(b)            y = b
 * MERCATOR / with poles at infinity
 *            x = l                   y = atanh(b)
 * LAMBERT  / equal area projection
 *            x = l                   y = sin(b)
 * </PRE>
 * <P> The typical usage of the Proj3 class consists in:<OL>
 * <LI> Define a projection (type and the center of projection) by means of
 *	one of the constructors; the default center is the (0,0) point.
 * <LI> Compute the projection values <EM>X,Y</EM> from a position with
 *	the <B>set(Coo)</B> method; the projections can be retrieved
 *	either via the <B>getX</B> and <B>getY</B> methods,
 *	or in <EM>aProj3.X</EM> and <EM>aProj3.Y</EM> elements.
 * <LI> The reverse computation (from projections to coordinates)
 *	is done with the <B>set(X,Y)</B>  method; the corresponding point
 *	is obtained by means of the <B>getCoo</B> method.
 * </OL>
 *
 * @author Pierre Fernique, Francois Ochsenbein [CDS]
 * @version 1.0 : 03-Mar-2000
 * @version 1.1 : 24-Mar-2000: better documentation
 * @version 1.11: 24-Apr-2006: qualified exceptions
 * @version 1.2:  24-May-2008: creation from a Coo + set methods
 * @version 1.21: 24-May-2013: gug in set(Coo)
 */

public class Proj3 {
    protected byte type ;	// Projection type
    private double R[][] ;	// Rotation Matrix
    protected double clon, clat; // Center of the Projection (degrees)
    /** The values of the projections */
    protected double X,Y ;	// One point in the projection (cartesian)
    /** The corresponding polar angles */
    protected Coo point ;	// One point in the projection (original angles)

    // Constants
    public static final int NONE     = 0;
    public static final int TAN      = 1; // Standard Gnomonic (r = tan(theta))
    public static final int TAN2     = 2; // Stereographic (r = 2.tan(theta/2))
    public static final int SIN      = 3; // Orthographic  (r = sin(theta))
    public static final int SIN2     = 4; // Equal-area    (r = 2.sin(theta/2))
    public static final int ARC      = 5; // Schmidt proj. (r = theta)
    public static final int AITOFF   = 6; // Aitoff Projection
    public static final int SANSON   = 7; // Global Sinusoidal
    public static final int MERCATOR = 8; // 
    public static final int LAMBERT  = 9; // 
    public static final String [] name = { "-", 
    	"Gnomonic (TAN)", "Stereographic (TAN2)", 
	"Orthographic (SIN)", "Zenithal Equal-area (SIN2)",
	"Schmidt (ARC)", "Aitoff", "Sanson", "Mercator", "Lambert"
    };

  //  ===========================================================
  //		Constructors
  //  ===========================================================

  /** 
   * Standard (TAN) projection.
   * At creation, the center and the type of projection is specified
   * @param centre Point defining the center of projection.
   *             (coordinates of the tangent point, expressed in degrees).
   */
   public Proj3(Coo centre) {
        this(TAN, centre);
   }

  /** 
   * Projection of a point on the sphere.
   * At creation, the center and the type of projection is specified
   * @param type projection type -- default (standard) = TAN
   * @param centre Point defining the center of projection.
   *             (coordinates of the tangent point, expressed in degrees).
   */
   public Proj3(int type, Coo centre) {
	this.type = (byte)type ;
	this.point = new Coo(centre);
	this.clon = point.getLon();
	this.clat = point.getLat();
	if ((clon != 0) || (clat != 0))  // Keep null for unit matrix
      	    R = point.localMatrix();
   }

  /** Creation of object used for Projections.
   * At creation, the center and the type of projection is specified
   * @param type projection type -- default (standard) = TAN
   * @param lon  longitude of the center of projection.
   *             (coordinates of the tangent point, expressed in degrees).
   * @param lat latitude of the center of projection.
   *             (coordinates of the tangent point, expressed in degrees).
   */
   public Proj3(int type, double lon, double lat) {
	this(type, new Coo(lon, lat)) ;
   }

  /** Creation of object used for Projections from a String.
   * @param type     projection type 
   * @param text     the center in a string
   */
   public Proj3(int type, String text) throws 
       ParseException {
	this.point = new Coo(text) ;
	this.type  = (byte)type ;
	this.clon  = this.point.lon ;
	this.clat  = this.point.lat ;
	if ((clon != 0) || (clat != 0)) 	// Keep null for unit matrix
      	    R = Coo.localMatrix(clon,clat);
   }

  /** Projection at the Origin.
   * Projection at tangent point (lon=0, lat=0)
   * @param type     projection type 
   */
   public Proj3(int type) {
	this(type, 0., 0.) ;	// Default = Standard Projection
   }

   /** Standard projection.
    * At creation, the center and the type of projection is specified
    * @param lon  longitude of the center of projection.
    *             (coordinates of the tangent point)
    * @param lat  latitude of the center of projection.
    *             (coordinates of the tangent point)
   **/
   public Proj3(double lon, double lat) {
	this(new Coo(lon, lat)) ;	// Default = Standard Projection
   }

  //  ===========================================================
  //			Static Methods
  //  ===========================================================

  /*  Static methods (functions) in Java are very close to C ones;
      they do not require any object instanciation.
      Typical example of static methods are in the Math class
  */

  //  ===========================================================
  //			Class Methods
  //  ===========================================================

  /** 
   * Get only the X value of the projection.
   * @return  the X projection
   */
   public final double getX() {
	return(X) ;
   }

  /** 
   * Get the Y value of the projection
   * @return  the Y projection
   */
   public final double getY() {
	return(Y) ;
   }

  /** 
   * Get the coordinate
   * @return the point
   */
   public final Coo getCoo() {
	return(new Coo(point)) ;
   }

  /** 
   * Get the longitude of the point on the projection.
   * @return the longitude in degrees of the point (point.lon)
   **/
   public final double getLon() {
	return(point.lon) ;
   }

  /**
   * Get the latitude of the point on the projection.
   * @return the latitude in degrees of the point (point.lat)
   **/
   public final double getLat() {
	return(point.lat) ;
   }

   /**
    * Returns a definition of this projection
    * @return a string containing the definition
    */
   public String toString() {
     	return(name[type] + " projection centered at " + clon + " " + clat 
	    + ": " + X + " " + Y) ;
   }

  //  ===========================================================
  //		Projections from Angles
  //  ===========================================================

  /** 
   * Compute a projection from initial coordinates.
   * <BR>
   * <B>Rem:</B> the method was named computeXY in a previous version.
   *
   * @param coo coordinates (lon + lat)
   * @return status (true if the projection is possible).
   *     	false when the position can't be projected.
   *		The values of the projections are in this.X and this.Y
   */
   public boolean set(Coo coo) {
      double x,y,z, r, w ;

	/* Was this position already computed ?? */
	if (point.equals(coo)) 
	    return(!Double.isNaN(X)) ;

	/* Set angles + unit vector, but X and Y are not yet computed */
	X = 0./0.; Y = 0./0.; 

	if (R == null) { x = coo.x; y = coo.y; z = coo.z ; }
	else {
      	    x = R[0][0]*coo.x + R[0][1]*coo.y + R[0][2]*coo.z ;
      	    y = R[1][0]*coo.x + R[1][1]*coo.y + R[1][2]*coo.z ;
      	    z = R[2][0]*coo.x + R[2][1]*coo.y + R[2][2]*coo.z ;
	}
	switch(type) {
    	  case TAN      :	// Only 1 hemisphere valid
	    if (x <= 0)  return false ; 
	    X = y/x; Y = z/x; 
	    break ;

    	  case TAN2     :	// All positions valid, just opposite pole
	    w = (1.0 + x)/2.0;
	    if (w <= 0)  { X = 0./0.; Y = 0./0.; return false; }
	    X = y/w; Y = z/w;
	    break ;

    	  case SIN      :	// Only 1 hemisphere valid, r <= 1
	    if (x <= 0)  return false ; 
	    X = y; Y = z ;
	    break ;

    	  case SIN2     :	// Whole sphere, r <= 2 (equal area)
	    w = Math.sqrt((1.0 + x)/2.0);
	    if (w > 0)  { X = y/w; Y = z/w; }
	    else        { X = 2;   Y = 0;   }
	    break ;

    	  case ARC      :	// r <= pi
	    if (x > -1.0)  { 		// Angular distance = acos(x)
	        r = Math.sqrt(y*y + z*z) ;
	    	if (x > 0) w = AstroMath.asinc(r);
		else       w = Math.acos(x)/r ;
		X = y*w; Y = z*w;
	    }
	    else 	{ X = Math.PI ; Y = 0 ; }
	    break ;

    	  case AITOFF   :	// Ellipse, 
	    r = Math.sqrt(x*x + y*y) ;
	    w = Math.sqrt (r*(r+x)/2.0);	// cos lat . cos lon/2
	    w = Math.sqrt ((1.0 + w)/2.0);
	    X = Math.sqrt(2.*r*(r-x)) / w ;
	    Y = z / w ;
	    if (y<0) X = -X ;
	    break ;

    	  case SANSON   :	// Sinusoidal |X| <= pi, Y <= pi/2
	    r = Math.sqrt(x*x + y*y) ;
	    Y = Math.asin(z);
	    if (r == 0) X = 0 ;
	    else X = Math.atan2(y,x) * r;
	    break ;

    	  case MERCATOR :
	    r = Math.sqrt(x*x + y*y) ;
	    if (r == 0)  return false ; 
	    X = Math.atan2(y,x);
	    Y = AstroMath.atanh(z);
	    break ;

    	  case LAMBERT  :	// Equal Area (lon,sin(lat))
	    r = Math.sqrt(x*x + y*y) ;
	    Y = z ;
	    if (r == 0) X = 0 ;
	    else X = Math.atan2(y,x);
	    break ;
	
	  default:
	    throw new IllegalArgumentException(
	     "****Proj3: Invalid Projection type #" + type) ;
	}
	point.set(coo);
	return(true) ;
   }

  //  ===========================================================
  //		Projected values to Coordinates
  //  ===========================================================

  /** 
   * Reverse projection: compute the polar angle corresponding to (x,y)
   * <BR>
   * <B>Rem:</B> Method was called computeAngles in a previous version.
   *
   * @param  px  x projection values
   * @param  py  y projection values
   * @return status true if the X / Y values are within the projection area
   *     	and false otherwise,
   *		The values of the angles are obtained via getLon() and getLat().
   */
   public boolean set(double px, double py) {
      double x,y,z, /*x0,y0,z0,*/ r, w ;
    //  boolean angles_set = false ;
	/* Was this position already computed ?? */
	if ((px == this.X) && (py == this.Y)) 
	    return(!((point.x==0)&&(point.y==0)&&(point.z==0)));

	/* Set the projection values, but angles not yet computed */
	X = px; Y = py; 
	point.lon = point.lat = 0./0. ;
	point.x = point.y = point.z = 0;

	switch(type) {
    	  case TAN      :	// Only 1 hemisphere valid
	    x = 1.0 / Math.sqrt(1.0 + X*X + Y*Y);
	    y = X * x;
	    z = Y * x;
	    break ;

    	  case TAN2     :	// All positions valid, just opposite pole
	    r = (X*X + Y*Y)/4.0 ;
	    w = 1.0 + r ;
	    x = (1.0 - r)/w ;
	    y = X/w ;
	    z = Y/w ;
	    break ;

    	  case SIN      :	// Only 1 hemisphere valid, r <= 1
	    w = 1.0 - X*X - Y*Y;
	    if (w < 0) {	// Accept some rounding error
		if (w > -2.e-16) w = 0 ;
		else  return false ; 
	    }
	    x = Math.sqrt(w) ;
	    y = X;
	    z = Y;
	    break;

    	  case SIN2     :	// Whole sphere, r <= 2 (equal area)
	    r = (X*X + Y*Y)/4.0 ;
	    if (r > 1.)  return false ; 
	    w = Math.sqrt(1.0 - r) ;
	    x = 1.0 - 2.0 * r;
	    y = w * X;
	    z = w * Y;
	    break ;

    	  case ARC      :	// r <= pi
	    r = Math.sqrt(X*X + Y*Y) ;
	    if (r > Math.PI)  return false ; 
	    w = AstroMath.sinc(r);
	    x = Math.cos(r);
	    y = w * X;
	    z = w * Y;
	    break ;

    	  case AITOFF   :	// Ellipse, dimensions sqrt(2) x 2.sqrt(2)
	    r = X*X/8.0 + Y*Y/2.0; 	// 1 - cos b . cos l/2
	    if (r > 1.0)  return false ; 
	    x = 1. - r ;		//     cos b . cos l/2
	    w = Math.sqrt(1. - r/2.) ;	// sqrt(( 1 + cos b . cos l/2)/2)
	    y = X * w / 2. ;
	    z = Y * w ;
	    // Convert from Cartesian (l/2,b) to Cartesian (l,b) 
	    r = Math.sqrt(x*x + y*y) ;	// cos(b)
	    if (r > 0) {
	        w = x;
		x = (w*w - y*y) /r;
		y = 2.0 * w * y /r;
	    }
	    break ;

    	  case SANSON   :	// Sinusoidal |X| <= pi, Y <= pi/2
	    z = Math.sin(Y);
	    r = 1 - z*z; 	// cos^2(b)
	    if (r < 0)  return false ; 
	    r = Math.sqrt(r);	// cosb
	    if (r == 0) w = 0. ;
	    else    w = X/r;	// Longitude
	    x = r * Math.cos(w);
	    y = r * Math.sin(w);
	    break ;

    	  case MERCATOR :
	    r = 1./AstroMath.cosh(Y);
	    z = AstroMath.tanh(Y) ;
	    x = r * Math.cos(X);
	    y = r * Math.sin(X);
	    break ;

    	  case LAMBERT  :	// Equal Area (lon,sin(lat))
	    z = Y;
	    r = 1 - z*z;	// cos(b) ** 2 
	    if (r < 0)  return false ; 
	    r = Math.sqrt(r);	// cosb
	    x = r * Math.cos(X);
	    y = r * Math.sin(X);
	    break ;
	
	  default:
	    throw new IllegalArgumentException(
	     "****Proj3: Invalid Projection type #" + type) ;
	}

	/* From Cartesian: just rotate (used transposed matrix) */
	if (R != null) point.set(
      	    R[0][0]*x + R[1][0]*y + R[2][0]*z ,
      	    R[0][1]*x + R[1][1]*y + R[2][1]*z ,
      	    R[0][2]*x + R[1][2]*y + R[2][2]*z 
	) ;
	else point.set(x, y, z) ;
     	return true ;
   }

  //  ===========================================================
  //		Change the Projection Center
  //  ===========================================================

  /** 
   * Modify the projection center reflecting a translation in projection.
   * @param X2  New X-position of current location
   * @param Y2  New Y-position of current location
   * @return status (true if it's possible).
   *     	false when the change is not possible.
   * <br> On return with <b>true</b>, the values of the projection are
   * <b>X2</b> and <b>Y2</b>, but correspond to the same spherical angles.
   * <br>If L(c) is the localMatrix associated to a position, u1 and
   * u2 the coordinates, the new rotation matrix is
   * L' = L(c) . L(u2) . tL(u1)
   */
   public boolean moveCenter(double X2, double Y2) {
      // No change if same position...
      if ((X == X2) && (Y == Y2))
	  return(true);
      double M1[][] = point.localMatrix();
      //System.out.println("#...moveCenter from (" + this +")");
      if (!this.set(X2, Y2))  // Not possible...
	  return(false);
      //System.out.println("    to (" + X2 +"," +Y2 + ") = " + this.getCoo());
      // The new rotation matrix is:
      //System.out.println(Coo.toString("#  oldR: ", R));
      double M2[][] = point.localMatrix();
      M2 = AstroMath.m3p(M2, AstroMath.m3t(M1));
      if (R == null) R = M2;
      else R = AstroMath.m3p(R, M2);
      //System.out.println(Coo.toString("#  newR: ", R));
      point.set(R[0][0], R[0][1], R[0][2]);
      //System.out.println("#  newC: " + point);
      clon = point.getLon(); clat = point.getLat(); 
      this.set(X2, Y2);
      //System.out.println("#...new position is (" + this + ")");
      return(true);
   }
}
