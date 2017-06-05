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

import java.io.Serializable;

/**
 * This abstract class defines the required methods to instanciate the
 * definition of a frame used for astronomical coordinates.
 * @author Francois Ochsenbein
 * @version 1.1 23-Apr-2006: addition of toICRS(6-D vector), and ICRSmatrix.
 * @version 1.2 06-Jun-2006: simplified version, only 2 abstract methods
 *          required: toICRSmatrix() and toICRS()
 * @version 1.3 13-Nov-2008: method create(String)
 * @version 1.4 08-Apr-2009: method create(char)
 * @version 1.5 18-Nov-2014: avoid interpretation as epoch
 *
 */
public abstract class Astroframe implements Serializable {
    static private final boolean DEBUG=false;
    static private Editing edframe = new Editing("?frame?");
    /** 
     * List of valid 1-letter frames used in IAU designations
     */
    static public String IAUframes = "JGBISE"; 
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
     * The sexagesimal representation of the longitude may be expressed 
     * in time unit. This is the case of the Equatorial frames,
     * but also in the ICRS.
    **/
     public boolean hms = false;

    /** 
     * The defaut precision (0=unknown, 3=1', 5=1").
     * The precision value is expressed as the number of decimals (+1), i.e.
     * 0=unknown, 1=deg 3=1' 5=1" 8=1mas 11=1uas, etc...
    **/
    public byte precision = 8;	// Default edition to 1mas

    /** 
     * Default edition of Longitude/Right Ascension.
     * It contains one of the options in {@link Astroformat} among
     * DECIMAL, SEXA..., eventualled or'ed with SIGN_EDIT | ZERO_FILL
    **/
     public byte ed_lon = Editing.DECIMAL|Editing.ZERO_FILL;
    /** 
     * Default edition of Latitude/Declination.
     * It contains one of the options in {@link Astroformat} among
     * DECIMAL, SEXA..., eventualled or'ed with SIGN_EDIT | ZERO_FILL
    **/
     public byte ed_lat = Editing.DECIMAL|Editing.ZERO_FILL|Editing.SIGN_EDIT;

    /**
     * Interpret a text representing a frame
     * @param  txt A text which contains a frame definition.
     * <UL>
     * <LI> I = IRCS (no equinox)
     * <LI> J = FK5 eventually followed by equinox (Julian)
     * <LI> B = FK4 eventually followed by equinox (Besselian)
     * <LI> E = Ecliptic, eventually followed by equinox (Julian)
     * <LI> G = Galactic (no equinox)
     * <LI> S = Supergalactic (no equinox)
     * <LI> FK4(equinox), equivalent to Bequinox, e.g. FK4(1950)
     * <LI> FK5(equinox), equivalent to Jequinox, e.g. FK5(2000)
     * </UL>
     * An epoch might be added, e.g. 
     * <font color='darkred'>ICRS(1991.25)</font> or 
     * <font color='darkred'>ICRS(Epoch=1991.25)</font> 
     * for the frame used in Hipparcos.
     * @return the astroframe interpreted.
     * When <tt>null</tt> is returned, the parsing object is not modified.
    **/
    public static Astroframe parse(Parsing txt) {
      int posini = txt.pos;
      Astroframe frame = null;
      boolean bracketed, has_par=false, has_epoch=false, is_ok = true;
      Astrotime t;
      char c;
        txt.gobbleSpaces();
	bracketed = txt.match("(");
	c = Character.toUpperCase(txt.currentChar());
	if (c == 'I') {
	    if ((is_ok = txt.match("ICRS"))) 
	        frame = (Astroframe) new ICRS();
	}
	else if (c == 'G') {	// Galactic: accept G Gal, etc
	    while (Character.isLetter(txt.currentChar()))
		txt.advance(1);
	    frame = (Astroframe) new Galactic();
	}
	else if (c == 'S') {	// Supergalactic
	    while (Character.isLetter(txt.currentChar()))
		txt.advance(1);
	    frame = (Astroframe) new Supergal();
	}
	else if (c == 'F') {	// FK4 or FK5 ?
	    if (txt.match("FK4")) {
		if ((has_par = txt.match('('))) 
		     frame = (Astroframe) new FK4(txt.parseDecimal());
		else frame = (Astroframe) new FK4();
	    }
	    else if (txt.match("FK5")) {
		if ((has_par = txt.match('('))) 
		     frame = (Astroframe) new FK5(txt.parseDecimal());
		else frame = (Astroframe) new FK5();
	    }
	    else is_ok = false;
	}
	else if (c == 'B') {	// B1950
	    txt.match('B');
	    if (Character.isDigit(txt.currentChar())) 
		frame = (Astroframe) new FK4(txt.parseDecimal());
	    else is_ok = false;
	}
	else if (c == 'J') {	// J2000
	    txt.match('J');
	    if (Character.isDigit(txt.currentChar())) 
		frame = (Astroframe) new FK5(txt.parseDecimal());
	    else is_ok = false;
	}
	else if (c == 'E') {	// Ecliptic
	    while (Character.isLetter(txt.currentChar()))
		txt.advance(1);
	    if ((has_par = txt.match('('))) {
		t = new Astrotime();
		if (t.parsing(txt))
	             frame = (Astroframe) new Ecliptic(t.getJyr());
		else frame = (Astroframe) new Ecliptic();
	    }
	    else if (Character.isDigit(txt.currentChar())) 
		 frame = (Astroframe) new Ecliptic(txt.parseDecimal());
	    else frame = (Astroframe) new Ecliptic();
	}
	else is_ok = false;
	if (!is_ok) {
	    txt.set(posini);
	    return(null);
	}

	// Get Epoch, may be written ,Ep= or , or (...)
	posini = txt.pos;
	is_ok = true;
	/* System.out.println("frame.parse: is_ok=" + is_ok + ", text=" + txt
	+ ", has_par=" + has_par); */
	txt.gobbleSpaces();		// V1.5
	if (has_par) txt.match(',');
	else has_par = txt.match('(');	// V1.5
	txt.gobbleSpaces();
	/* System.out.println("frame.parse: is_ok=" + is_ok + ", text=" + txt
	+ ", has_par=" + has_par); */
	if (Character.toUpperCase(txt.currentChar()) == 'E') {
	    txt.advance(1);
	    if (Character.toLowerCase(txt.currentChar()) == 'p') {
		while (Character.isLetter(txt.currentChar())) txt.advance(1);
	        is_ok = txt.match('=') ;
		has_epoch = true;	// V1.5
	    }
	    else is_ok = false;
	}
	else if ((has_par || bracketed) 
	    && Character.isLetterOrDigit(txt.currentChar())) 
	    has_epoch = true; 		// V1.5
	if (has_epoch) { 
	    t = new Astrotime();
	    if ((is_ok = t.parsing(txt))) {
		frame.setFrameEpoch(t.getJyr());
	    }
	}
	if (has_par && is_ok)   is_ok = txt.match(')');
	if (bracketed && is_ok) is_ok = txt.match(')');
	if (is_ok) txt.match(':');	// Frame may be followed by :
	if (!is_ok) txt.set(posini);
	return(frame);
    }

    /**
     * Creation of an astroframe from the interpretation of a text
     * @param  name the frame name + equinox + epoch
     * @return the astroframe
     * @see parse
    **/
    public static Astroframe create(String name) {
	Parsing text = new Parsing(name);
	Astroframe frame = parse(text);
	text.gobbleSpaces();
	if (text.pos < text.length) {   // Not complete, error
	    System.err.println("#+++Astroframe(" + name + "): " + text);
	    return(null);
	}
	return(frame);
    }

    /**
     * Verify letter is a valid frame.
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal)
     * @return true/false
    **/
    public static final boolean isIAU(char sym) {
	return(IAUframes.indexOf(Character.toUpperCase(sym))>=0) ;
    }

    /**
     * Creation of an astroframe from a single char (J, B, G, etc)
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal)
     * @return the corresponding astroframe / null
    **/
    public static Astroframe create(char sym) {
	switch(Character.toUpperCase(sym)) {
            case 'J': return(new FK5());
	    case 'G': return(new Galactic());
	    case 'B': return(new FK4());
	    case 'I': return(new ICRS());
	    case 'S': return(new Supergal());
            case 'E': return(new Ecliptic());
	}
	return(null);
    }

    /**
     * Creation of an astroframe from a single char (J, B, G, etc)
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal)
     * @return the corresponding astroframe / null
    **/
    public boolean equals(char sym) {
	switch(Character.toUpperCase(sym)) {
	    case 'I':
            case 'J': return(this instanceof ICRS 
			  || this.name.equals("FK5(J2000.0)"));
	    case 'G': return(this instanceof Galactic);
	    case 'B': return(this.name.equals("FK4(B1950.0)"));
	    case 'S': return(this instanceof Supergal);
            case 'E': return(this instanceof Ecliptic);
	}
	return(false);
    }

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
 	 * Compute the hashcode
 	 * @return the hascode value
 	 */
 	public int hashCode() {
 		int hcode = name.hashCode();
 		long l = Double.doubleToLongBits(epoch);
 		hcode = hcode * 123 + (int) (l^(l >>> 32));
 		return hcode;
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
    **/
     public void fromICRS(double u6[]) {
	if (ICRSmatrix == null) setICRSmatrix();
	if (DEBUG) {
	    System.out.println("....Astroframe.from: ICRSmatrix(transposed):");
	    System.out.println(Coo.toString("    ", ICRSmatrix));
	}
	Coo.rotateVector_1(this.ICRSmatrix, u6);
     }

     /**
      * Get the epoch
      * @return the epoch value
      */
     public double getEpoch() {
   	  return epoch;
     }
}
