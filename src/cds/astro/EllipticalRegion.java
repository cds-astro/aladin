package cds.astro;

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
  	Interface for positional checks in Regions
 *==================================================================*/

/**
 * Elliptical region on the sky, defined by its center + semi-major 
 * axes.
 * The spherical ellipse is defined as the set of points having the
 * sum of their distances to 2 foci <b>F0</b> and <b>F1</b> constant;
 * the relation between <b>a</b> (semi-major axis), <b>b</b> (semi-minor axis)
 * and <b>c</b> (semi-distance between foci) is
 *           <b>cosa = cosb . cosc</b>.
 * The equation of the spherical ellipse is, if f0 and f1 are the distance
 * to the foci:
 *   <b>cos<sup>2</sup>(f0) + cos<sup>2</sup>(f1) - 2 cos(2a) cos(f0) cos(f1) 
 *   = sin<sup>2</sup>(2a)</b>
 * which is a quadratic expression in x, y, z.
 * @author Francois Ochsenbein (see the document "On the spherical ellipse")
 * @version 1.0 04-Jun-2008
 * @version 1.1 04-Jun-2016: corrected compuatation of area
 **/

/* Note on the spherical ellipse:
   the definition is, if f0 and f1 are the angular distances between
   a point M and the 2 foci:
        f0 + f1 = 2a
   Taking the cosine gives:
        cos(f0).cos(f1) - sin(f0).sin(f1) = cos(2a)
   Then isolate sin and cos, take squared values
 */

public class EllipticalRegion extends Region {
  double a, b, pa;		// semi-major, semi-minor, position angle
  Coo f0, f1;			// Coordinates of foci
  double sina, cosa, cos2a, sin2_2a, cosc;
  double e;			// Eccentricity e=sin(c)/sin(a)

  /** Constructor
   * @param	frame the frame in which the ellipse element are given 
   * 			(may be null)
   * @param	c the center of the ellipse
   * @param	a the semi-major diameter, in degrees
   * @param	b the semi-minor diameter, in degrees
   * @param	pa the position angle, in degrees (angle of major axis with NS).
   * 			pa is in the range [0, 180.[
   **/
  EllipticalRegion(Astroframe frame, Coo c, double a, double b, double pa) {
    if (DEBUG) System.out.println("#...new Ellipse(" + c
        + ", a=" + a + ", b=" + b + ", pa=" + pa + ")");
    if ((a >= 180.) || (b >= 180.)) {
      System.err.println("#+++Ellipse(" + c + ", a=" + a + ", b=" + b
          + "): sem-axises too large!");
      return;
    }
    // Verify a bad center ? 
    if ((c.x == 0) && (c.y == 0) && (c.z == 0)) {
      centroid = null;
      return;
    }
    centroid = new Coo(c);
    if (a<b) { this.a = b/2.; this.b = a/2.; this.pa = pa + 90.; }
    else     { this.a = a/2.; this.b = b/2.; this.pa = pa; }
    this.minrad = this.b;
    this.maxrad = this.a;

    // Coordinates of foci: cosc = cosa/cosb
    this.cosa   = AstroMath.cosd(this.a);
    this.sina   = AstroMath.sind(this.a);
    double cosb = AstroMath.cosd(this.b);
    double sinb = AstroMath.sind(this.b);
    this.cosc = cosa/cosb;
    double sinc = Math.sqrt((sina+sinb)*(sina-sinb))/cosb;
    double cpa = AstroMath.cosd(this.pa);
    double spa = AstroMath.sind(this.pa);
    f0 = new Coo(cosc, sinc*spa,  sinc*cpa);
    f1 = new Coo(f0.x, -f0.y, -f0.z);
    if (DEBUG) System.out.println("#...foci(0) " + f0 + " " + f1);

    /* Rotation between the horizontal (EW) ellipse and coordinates:
	  +-                    -+ +-                  -+ +-                -+
	  |cos(lon)  -sin(lon)  0| |cos(lat) 0 -sin(lat)| |1    0        0   |
	  |sin(lon)   cos(lon)  0| |   0     1    O     | |0 sin(pa) -cos(pa)|
	  |   0          0      1| |sin(lat) 0  cos(lat)| |0 cos(pa)  sin(pa)|
	  +-                    -+ +-                  -+ +-                -+
     */
    // Compute the rotation matrix 
    double[] Rl = centroid.localMatrix();
    f0.rotate_1(Rl);
    f1.rotate_1(Rl);
    if (DEBUG) System.out.println("#...foci(1) " + f0 + " " + f1);
    if (frame != null) {
      frame.toICRS(f0);
      frame.toICRS(f1);
      frame.toICRS(centroid);
    }
    // Constants:
    cos2a = AstroMath.cosd(2.*this.a);
    sin2_2a = AstroMath.sind(2.*this.a); sin2_2a *= sin2_2a;
    e = sinc/sina;	// Eccentricity

    // recompute pa in original frame (range 0-180deg)
    this.pa = centroid.posAngle(f0);
    while (this.pa>=180) this.pa -= 180.;
  }

  /** Constructor
   * @param	c the center of the ellipse
   * @param	a the diameter parallell to Equator
   * @param	b the diameter in North-South direction
   **/

  EllipticalRegion(Coo c, double a, double b) {
    this(null, c, a, b, 90.);
  }

  /** 
   * Edition of an Ellipse region
   * @return	ascii equivalent
   **/
  public String toString() {
    StringBuffer bed = new StringBuffer();
    if (centroid == null) bed.append("*INVALID*");
    bed.append("Ellipse(");
    if (centroid != null) bed.append(centroid.toString());
    bed.append(", " + 2.*this.a + "x" + 2.*this.b);
    if (this.pa>=0.) bed.append(", pa=" + this.pa);
    bed.append(")");
    return(bed.toString());
  }

  /** 
   * Area of an elliptical region
   * @return	area.
   * The area is (in steradians)  2\pi - 4.*cos(a)*tan(b)/tan(a)Pi(e^2,sin^2c)
   * where Pi(n,m) is the complete elliptical integral of 3rd kind
   * Pi(n,m) = \int(0,pi/2) du/[(1-n.sin^2(u))sqrt(1-m.sin^2(u))]
   * Use an approximation for angle&lt;0.2deg (0.0035rad), see document
   * The accuracy is better than 10<sup>-9</sup>.
   **/
  public double area() {
    double e2 = e*e;
    double sr;	// Area in steradians
    if(e2<1.e-12 || a<0.2) {	// Small eccentricity or small ellipse
      double rho = 2.*AstroMath.sind(a/2.);
      sr = Math.PI*rho*rho;
      if(e2>0) sr = sr*Math.sqrt(1.-e2)*(1.+(3./8.)*e2*rho*rho);
    }
    else  {		// Large ellipse, use elliptic integral
      sr = 2.*Math.PI 
          - 4.*cosa*Math.sqrt(1.-e2)*AstroMath.ell3(e2, e2*sina*sina);
    }
    //if (DEBUG) System.out.println("#...elliptical area = " + sr + "sr");
    return(AstroMath.DEG2*sr);
  }

  /** 
   * Verify point within Ellipse
   * @param	point  a position
   * @return	true if point within region.
   **/
  public boolean checkCoo(Coo point) {
    // Verify an invalid point ?
    if (centroid == null) return(false);
    double d0 = point.dotprod(f0);	// cos(f0)
    double d1 = point.dotprod(f1);	// cos(f1)
    return(d0*d0 + d1*d1 - 2.*cos2a*d0*d1 >= sin2_2a);
  }

  /* 
   * Intersection of a segment with the ellipse
   * Use point M = (u0 + t*u1)/sqrt(1+t^2+2t*s), find
   * if there is any such point satisfying the ellipse equation.
   */
  private boolean intersects(double[] u0, double[] u1) {
    double s = Coo.dotprod(u0, u1);
    double s00 = f0.dotprod(u0);
    double s01 = f0.dotprod(u1);
    double s10 = f1.dotprod(u0);
    double s11 = f1.dotprod(u1);
    // Coeff. of intersection equation a2*t^2 + 2.a1*t + a0 = 0 
    double a2 = s01*s01 + s11*s11 - 2.*cos2a*s01*s11 - sin2_2a;
    double a0 = s00*s00 + s10*s10 - 2.*cos2a*s00*s10 - sin2_2a;
    double a1 = s00*s01 + s10*s11 - cos2a*(s00*s11 + s01*s10) - sin2_2a*s;
    // Normalize to t^2 + a1*t + a0 = 0 
    if (a2 == 0) return((a1*a0)<0);	// Solution -a0/a1 must be positive
    a1 /= a2; a0 /= a2;
    if (DEBUG) System.out.println("#...Einter: t^2 + 2.*" + a1 + "*t + "
        + a0 + ")");
    double disc = a1*a1 - a0;
    if (disc<=0) return(false);	// Discriminant<0 => no solution
    if (a1<0)    return(true);	// Solution -a1+/-sqrt(disc)
    return(a0<0);
  }

  /** 
   * Verify a circle intersects an Ellipse
   * @param	point  a position
   * @param	r      radius (degrees)
   * @return	DISJOINT / INTERSECTS / INCLUDES.
   *          for no intersection / overlap / cercle fully included
   **/
  public int checkCircle(Coo point, double r) {
    if (centroid == null) return(DISJOINT);
    if (DEBUG) System.out.println("#...Ellipse.checkCoo(" + point + ")");
    Coo c = new Coo(point);
    // Distances with foci
    double rf = (point.distance(f0) + point.distance(f1))/2.;
    if ((rf-r) >= a) 	// Totally outside
      return(DISJOINT);
    if ((rf+r) <= a) 	// Totally inside
      return(INCLUDES);
    return(INTERSECTS);
  }

  /** 
   * Intersection of a Qbox with the Ellipse
   * @param	qbox the Qbox to check
   * @return	DISJOINT / INCLUDES / INTERSECTS
   **/
  public int checkQbox(int qbox) {
    if (centroid == null) return(DISJOINT);
    Qbox abox = new Qbox();
    abox.set(qbox);
    /* Fast elimination */
    int i = checkCircle(abox.center(), Qbox.maxRadius(qbox));
    if (i<INTERSECTS) 			// DISJOINT or INCLUDES
      return(i);

    // Check more accurately: can the box be completely included ?
    // Ellipse is convex -- therefore verifying the number of corners
    // inside the Ellipse should be enough to decide.
    double[][] u4 = new double[5][3];
    Qbox.ucorners(abox.qbox, u4);

    int nin=0, nut=0;	// #points inside/outside
    Coo point = new Coo();

    for (i=0; i<4; i++) {
      point.set(u4[i][0], u4[i][1], u4[i][2]);
      if (checkCoo(point)) nin++; else nut++;
    }
    if (nin == 4) { 	// Qbox included in Ellipse
      return (INCLUDES);
    }
    if (nin >0) {		// Not all corners inside
      return (INTERSECTS);
    }
    // All four corners are outside -- does not mean no overlap...
    u4[4][0] = u4[0][0]; u4[4][1] = u4[0][1]; u4[4][2] = u4[0][2];
    for (i=0; i<4; i++) {
      if(intersects(u4[i], u4[i+1]))
        return (INTERSECTS);
    }
    return(abox.inside(centroid) ?
        IS_PARTOF :	// Ellipse fully included in Qbox
          DISJOINT) ;
  }
}
