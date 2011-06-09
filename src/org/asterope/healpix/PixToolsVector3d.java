//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//

package org.asterope.healpix;

/**
 * A 3 element vector that is represented by double precision floating point
 * x,y,z coordinates. If this value represents a normal, then it should be
 * normalized. Mutable version was originally part of java3d vecmath package,
 * was replaced with immutable version inside PixTools jar
 */
public class PixToolsVector3d {

	public final double x, y, z;

	public PixToolsVector3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final PixToolsVector3d sub(PixToolsVector3d v1) {
		return new PixToolsVector3d(x - v1.x, y - v1.y, z - v1.z);
	}

	public final PixToolsVector3d add(PixToolsVector3d v1) {
		return new PixToolsVector3d(x + v1.x, y + v1.y, z + v1.z);
	}

	/**
	 * Returns the angle in radians between this vector and the vector
	 * parameter; the return value is constrained to the range [0,PI].
	 * 
	 * @param v1
	 *            the other vector
	 * @return the angle in radians in the range [0,PI]
	 */
	public final double angle(PixToolsVector3d v1) {
		// return (double)Math.acos(dot(v1)/v1.length()/v.length());
		// Numerically, near 0 and PI are very bad condition for acos.
		// In 3-space, |atan2(sin,cos)| is much stable.
		double xx = y * v1.z - z * v1.y;
		double yy = z * v1.x - x * v1.z;
		double zz = x * v1.y - y * v1.x;
		double cross = Math.sqrt(xx * xx + yy * yy + zz * zz);
		return Math.abs(Math.atan2(cross, dot(v1)));
	}

	/**
	 * Returns the length of this vector.
	 * 
	 * @return the length of this vector
	 */
	public final double length() {
		return Math.sqrt(lengthSquared());
	}

	/**
	 * Returns the squared length of this vector.
	 * 
	 * @return the squared length of this vector
	 */
	public final double lengthSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Computes the dot product of the this vector and vector v1.
	 * 
	 * @param v1
	 *            the other vector
	 */
	public final double dot(PixToolsVector3d v1) {
		return x * v1.x + y * v1.y + z * v1.z;
	}

	/**
	 * Return normalized vector of this vector
	 */
	public final PixToolsVector3d normalized() {
		double d = length();
		// zero-div may occur.
		return new PixToolsVector3d(x / d, y / d, z / d);
	}

	/**
	 * calculate cross product of two vectors
	 * 
	 * @param v1
	 *            PixToolsVector3d
	 * @param v2
	 *            PixToolsVector3d
	 * @return PixToolsVector3d result of the product
	 */
	public PixToolsVector3d crossProduct(PixToolsVector3d v2) {
		double x = this.y * v2.z - this.z * v2.y;
		double y = this.z * v2.x - this.x * v2.z;
		double z = this.x * v2.y - this.y * v2.x;
		return new PixToolsVector3d(x, y, z);
	}

	/** 
	 * @return Right Ascencion of this vector in radians
	 */
	public double toRa() {
		double phi = 0.;
		if ((x != 0.) || (y != 0))
			phi = Math.atan2(y, x); // phi in [-pi,pi]

		if (phi < 0)
			phi += 2.0 * Math.PI; // phi in [0, 2pi]

		return phi;
	}

	/** 
	 * @return Declination of this vector in radians
	 */
	public double toDe() {
		double z2 = z / length();
		double theta = Math.acos(z2);
		return Math.PI / 2 - theta;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PixToolsVector3d))
			return false;
		PixToolsVector3d other = (PixToolsVector3d) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public String toString() {
		return "Vector3d[" + x + ", " + y + ", " + z + "]";
	}
	
	/** 
	 * @return random normalized 3d vector
	 */
	public static PixToolsVector3d createRandomVector(){
		return new PixToolsVector3d(
				Math.random()-0.5d,Math.random()-0.5d,Math.random()-0.5d)
			.normalized();
	}
	
}