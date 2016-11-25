package cds.astro;

import java.io.*;
import java.util.*;
import java.text.*; // for parseException

/*==================================================================*
  Interface for positional checks in Regions
 *==================================================================*/

/**
 * AbstractRegion is an abstract class which defines regions 
 * on the celestial sphere.
 * Extensions are Circle (circular target), Convex, Zone, Ellipse
 *
 * 5 abstract methods are required:<OL>
 * <LI> toString (edition), 
 * <LI> checkCoo (returne true when a point is inside the region), and
 * <LI> checkCircle compare the region with a circle, and returns the statuses
 *    <UL><LI>DISJOINT when no intersection with circle,
 *    <LI>INTERSECTS when there is an intersection (may include the following
 *    2 statuses)
 *    <LI>INCLUDES when circle is fully included within region, 
 *    <LI>IS_PARTOF when region is fully included within region
 *    </UL>
 * <LI> checkQbox, similar to checkCircle, but applied on a Qbox.
 * <LI> area computes the area of the region (in square degrees)
 * </OL>
 * The centroid is an attribute of the region; it is null for undefined region.
 * The minrad and maxrad attributes specify the circular region completely
 * inside and completely outside the region.
 * @author Francois Ochsenbein
 * @version 1.0 04-Jun-2008
 * 
 **/

public abstract class AbstractRegion implements Serializable {
    static boolean DEBUG = false;
    /** 
     * DISJOINT is the status for non-overlapping regions.
     * Identical to QBOX_NONE.
     * */
    static public final int DISJOINT = Qbox.QBOX_NONE;
    /** 
     * INTERSECTS is the status for overlapping regions.
     * There are 2 other statuses for more precision.
     * */
    static public final int INTERSECTS = Qbox.QBOX_SOME;
    /** 
     * INCLUDES indicates the region fully includes its argument.
     * */
    static public final int INCLUDES = Qbox.QBOX_ANY;
    /** 
     * IS_PARTOF indicates the region is fully included in its argument.
     * */
    static public final int IS_PARTOF = 3;
    /**
     * The region center is called 'centroid'
     **/
    public Coo centroid = null;
    /**
     * radius (degrees) of Circle centered in <em>centroid</em> fully included
     * in region.
     **/
    public double minrad;
    /**
     * radius (degrees) of Circle centered in <em>centroid</em> which fully
     * includes the region.
     **/
    public double maxrad;

    /** 
     * Edition of a Region
     **/
    public abstract String toString();

    /**
     * Computation of the area of a region
     * @return	Surface of the region, in square degrees 
     * 	(360<sup>2</sup>/pi for the whole sphere)
     **/
    public abstract double area() ;

    /** 
     * The method checkCoo verifies a point lies within a region.
     * @param	point a point we want to check
     * @return  true if point inside the region, false otherwise
     * **/
    public abstract boolean checkCoo(Coo point);

    /** 
     * The method checkCircle verifies the intersection of Region 
     * with Circle.
     * @param	centre  center of circle
     * @param	r      radius (degrees) of circle
     * @return	DISJOINT / INTERSECTS / INCLUDES / IS_PARTOF.
     *          <UL><LI>DISJOINT = no intersection
     *          <LI>INTERSECTS = region and circle overlap
     *          <LI>INCLUDES = circle fully included within region.
     *          <LI>IS_PARTOF = region fully included in circle.
     *          </UL>
     **/
    public abstract int checkCircle(Coo centre, double r);

    /** 
     * This method check1 is a broad approximation of intersections.
     * Uses circular approximations.
     * @param	centre center of circle
     * @param	radius radius (degrees) of circle
     * @return	DISJOINT / INTERSECTS / INCLUDES / IS_PARTOF.
     **/
    public final int check1(Coo centre, double radius) {
	double d = centroid.distance(centre);
	// if (DEBUG) System.out.println("#...check1 "
	// 	+ "Circle(" + centre + ",r=" + radius + ")"
	// 	+ "\n    Region." + this 
	// 	+ "\n    maxrad=" + maxrad + ", d=" + d);
    	if (d >= (radius + maxrad)) 	// All points outside
    	    return (DISJOINT);
    	if (minrad >= (d + radius)) 
    	    return (INCLUDES);
	if (radius >= (d+maxrad))
	    return(IS_PARTOF);
	return (INTERSECTS);		// possible intersection
    }

    /** 
     * The method checkQbox verifies the intersection of a Qbox with the region.
     * This method should be superseded by the one in the dedicated classes:
     * here it just assimilates the regions to circles.
     * @param	qbox  an integer representing a Qbox number
     * @return  DISJOINT / INTERSECTS / INCLUDES / IS_PARTOF.
     **/

    public int checkQbox(int qbox) {
	if (centroid == null) return(DISJOINT);
        Qbox abox = new Qbox();
    	abox.set(qbox);
    	Coocube cc = abox.center();
	double rb = abox.radius();	// Circle containing the Qbox
	return(checkCircle(cc, rb));
    }
}
