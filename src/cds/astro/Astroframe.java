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

import java.io.Serializable;

/**
 * This abstract class defines the required methods to instanciate the
 * definition of a frame used for astronomical coordinates.
 * @author Francois Ochsenbein
 * @version 1.1 23-Apr-2006: addition of toICRS(6-D vector), and ICRSmatrix.
 * @version 1.2 06-Jun-2006: simplified version, only 2 abstract methods
 *          required: toICRSmatrix() and toICRS()
 *
 */
public abstract class Astroframe implements Serializable {
    static private final boolean DEBUG=false;
    static private Editing edframe = new Editing("?frame?");
    /** 
     * The name of the frame (normally assigned at instanciation) 
    **/
    public String name = "*undefined*" ;

   /** 
    * The defaut epoch, a constant which cannot be changed for a frame.
    **/
    public double base_epoch = 2000.;

   /** 
    * The epoch, expressed in Julian Year.
    * This epoch may be changed by setFrameEpoch.
    **/
    public double epoch = 2000.;

    /**
     * The conversion to ICRS, if it is a simple rotation.
    **/
    public double[][] ICRSmatrix = null;

    /** 
     * The defaut precision (0=unknown, 3=1', 5=1").
     * The precision value is expressed as the number of decimals (+1), i.e.
     * 0=unknown, 1=deg 3=1' 5=1" 8=1mas 11=1µas, etc...
    **/
    public byte precision = 8;	// Default edition to 1mas

    /**
     * To a frame is attached a usage in the edition of coordinates.
     * See {@link Astroformat}. The default edition is in decimal degrees.
     * In equatorial systems, the default is sexagesimal, with RA in time units.
    **/
    /** 
     * Default edition of Longitude/Right Ascention *
    **/
     public byte ed_lon = Editing.DECIMAL|Editing.ZERO_FILL;
    /** 
     * Default edition of Latitude/Declination
    **/
     public byte ed_lat = Editing.DECIMAL|Editing.ZERO_FILL|Editing.SIGN_EDIT;

    /**
     * The sexagesimal representation of the longitude may be expressed 
     * in time unit. This is the case of the Equatorial frames,
     * but also in the ICRS.
    **/
     public boolean hms = false;

    /**
     * Edition of an Astroframe.
     * The name contains the epoch when the epoch is non-standard.
     * @return the name of the Astroframe.
    **/
    public String toString() {
	if (base_epoch == epoch) return(this.name);
	return (this.toString(epoch));
    }

    /**
     * Edition of an Astroframe with specification of the epoch.
     * @return the name of the Astroframe.
    **/
    public String toString(double epoch) {
	StringBuffer b = new StringBuffer(this.name);
	int i = this.name.length()-1;
	if (b.charAt(i) == ')') {
	    b.setLength(i);
	    b.append(',');
	}
	else b.append('(');
	b.append("Ep=J"); i = b.length();
	b.append(epoch);
	if ((b.length() - i) > 6) {	// Don't give too many decimals! 
	     b.setLength(i-1);
	     b.append('B');
	     edframe.editDecimal(b, Astrotime.J2B(epoch), 4, -3, 0);
	}
	if ((b.length() - i) > 6) {	// Don't give too many decimals! 
	     b.setLength(i-1);
	     b.append('J');
	     edframe.editDecimal(b, epoch, 4, -3, 0);
	}
	b.append(')');
	return(b.toString());
    }

    /**
     * Make the required verifications to install the ICRS matrix.
    **/
    public final void setICRSmatrix() {
	if ((ICRSmatrix == null) && (toICRSmatrix() == null)) 
	    System.err.println("****Astroframe " + this
		    + ": linkage to ICRS undefined!");
    }

    /**
     * The setFrameEpoch method just set the default epoch.
     * @param epoch epoch of the frame, in Julian year.
    **/
    public void setFrameEpoch(double epoch) {
	this.epoch = epoch;
    }

    /** 
     * Equality of frames -- name + epoch are assumed fully represent
     * any frame. Note that the equinox is part of the frame name, hence
     * 		  an equality of names imples the same equinox.
     * @param o Another object
     * @return True if same Astroframe 
     *
    **/
    public boolean equals(Object o) {
	boolean res = false;
	if (o instanceof Astroframe) {
	    Astroframe a = (Astroframe)o;
	    res = this.epoch == a.epoch &&
		  this.name.equals(a.name);
	}
	return(res);
    }

    /** 
     * Getting the matrix to rotate to ICRS system.
     * This method should return null when the change to ICRS can't be done 
     * by a simple rotation.
     * @return The rotation matrix from current frame to ICRS.
    **/
    public abstract double[][] toICRSmatrix() ;

    /** 
     * Conversion to ICRS.
     * This conversion must be installed only if toICRSmatrix() returns 
     * <tt>null</tt> (as in FK4).
     * The conversion is straightforward if just a rotation is involved:
     * <tt>coo.rotate(toICRSmatrix());</tt>
     * @param coo a Coordinate assumed to express a position in my frame.
     * On return, coo contains the corresponding coordinate in the ICRS.
    **/
    public void toICRS(Coo coo) {
     	if (ICRSmatrix == null) setICRSmatrix();
	coo.rotate(ICRSmatrix);
    }

    /** 
     * Conversion from ICRS. 
     * This conversion must be installed only if toICRSmatrix() returns 
     * <tt>null</tt> (as in FK4).
     * @param coo a Coordinate assumed to express a position in ICRS.
     * On return, coo gives the corresponding coordinate in my frame.
    **/
     public void fromICRS(Coo coo) {
     	if (ICRSmatrix == null) setICRSmatrix();
	coo.rotate_1(ICRSmatrix);
     }

    /** 
     * Conversion to ICRS with derivatives.
     * The code contained here assumes a simple rotation. It has to be
     * overloaded if this assertion is false (see e.g. FK4)
     * @param u6   a 6-vector (phase vector) of position + velocity.
     * 			Velocity in Jyr<sup>-1</sup>
     * 			Note that u6 can restricted be a 3-vector.
    **/
     public void toICRS(double u6[]) {
	if (ICRSmatrix == null) setICRSmatrix();
	if (DEBUG) {
	    System.out.println("....Astroframe.to:   ICRSmatrix");
	    System.out.println(Coo.toString("    ", ICRSmatrix));
	}
	Coo.rotateVector(this.ICRSmatrix, u6);
     }

    /** 
     * Conversion from ICRS with derivatives.
     * The code contained here assumes a simple rotation. It has to be
     * overloaded if this assertion is false (see e.g. FK4)
     * @param u6   a 6-vector (phase vector) of position + velocity.
     * 			Velocity in Jyr<sup>-1</sup>
     * 			Note that u6 can restricted be a 3-vector.
     * @return true/false if the operation is possible.
    **/
     public void fromICRS(double u6[]) {
	if (ICRSmatrix == null) setICRSmatrix();
	if (DEBUG) {
	    System.out.println("....Astroframe.from: ICRSmatrix(transposed):");
	    System.out.println(Coo.toString("    ", ICRSmatrix));
	}
	Coo.rotateVector_1(this.ICRSmatrix, u6);
     }

}
