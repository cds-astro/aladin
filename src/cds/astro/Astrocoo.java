// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.astro ;

/*==================================================================
                Astrocoo class
 *==================================================================*/

import java.io.*;
import java.text.*; 	 // for parseException

/**
 * This class defines the spherical coordinates used in Astronomy.
 * It associates the coordinates on the sphere (defined in the
 * <b>Coo</b> class) with the system (<b>Astroframe</b>) in which
 * they are expressed. It also includes the accuracy (number of digits) 
 * in which the coordinates are expressed, and the epoch of the position.
 * <P> The typical usage of the Astrocoo class consists in:<OL>
 * <LI> Define an astronomical coordinate frame by means of one of
 *	the constructors;
 * <LI> Assign a position in this frame via one of the <B>set</B> 
 *       methods
 * <LI> Convert to another frame via the  <B>convertTo</B> method
 * <LI> The angles (in degrees) are best extracted via <B>getLon</B> and
 *	<B>getLat</B> methods
 * </OL>
 * The edition of the position in an Astrocoo can be done in a StringBuffer
 * (<B>edit</B> methods) with options like sexagesimal or decimal edition,
 * or as a String with one of the available <B>toString</B> methods.
 * @author Francois Ochsenbein, Pierre Fernique [CDS].
 * Constants from slaLIB (P.T. Wallace)
 * @see Astroframe
 * @see Astroformat
 * @version 1.0 : 03-Mar-2000<br>
 * @version 1.1 : 24-Mar-2000 (Bug in ICRS Edition)<br>
 * @version 1.2 : 14-Nov-2002 (Sylvaine Jaehn (Stage UTBM)) :
 *                 Ajout methodes getLatPrec(),getLonPrec() et getPoint()<br>
 * @version 1.3 : Jan 2004 (BB) : ajout de la methode equals.<br>
 * @version 1.4 : Apr 2006 (FO) : Epoch may be within ()
 * @version 1.4a: Apr 2006 (FO) : Be sure sexagesimal hours at least h m
 * @version 1.5 : Oct 2006 (FO) : Be sure precision not larger than frame 
 * 				   intrinsic precision
 * @version 1.6 : Dec 2006 (FO) : Add <b>convert</b> method
 * @version 1.61: 07-May-2007: parseSexa when less than 5 numbers for lon+lat
 * @version 1.7 : 15-Nov-2008: toIAU method
 * @version 1.8 : 15-Apr-2009: accept IAU input in set() or constructor
 * @version 1.81: 18-Nov-2014: initialize editing
 */

public class Astrocoo extends Coo implements Serializable {
    static public boolean DEBUG = false; // true;
    //* One Coo consists in lon lat x y z (see Coo class)
    /** The associated frame */
    public Astroframe frame;
    /** Epoch (Jyr) of the position */
    public double epoch ;	// Epoch in Jyr.
    /** The precision (number of decimals+1) of the longitude */
    protected byte dlon;	// Original decimals for lon
    /** The precision (number of decimals+1) of the latitude */
    protected byte dlat;	// Original decimals for lat
    /** 
     * The precision is 0=unknown, 1=DEG, 3=ARCMIN, 5=ARCSEC, 8=MAS, etc .
     * There is also an intrinsic precision associated to the frame. 
     **/
    protected byte precision ;	// 0 = unknown, 1=deg, 3=arcmin, 5=arcsec, 8=mas
    /** How RA was entered, see {@link Astroformat} */
    protected byte formRA;	// Entered format of RA / Longitude */

    /** The editing option includes basic options described in 
     * {@link Astroformat}, plus the EDIT_FRAME, EDIT_FULL and EDIT_2 options.
     * It can be changed via the {@link #setEditing} method.
     **/
    protected short editing;

    /** Definitions of Precisions for the angles */
    static public final byte NONE = 0, DEG = 1, ARCMIN=3, ARCSEC=5, MAS=8 ;

    /** Editing option to insert the name of the frame */
    static public final short EDIT_FRAME = 0x100;
    /** Editing option to separate the 2 components by a blank */
    static public final short EDIT_2NUMBERS  = 0x200;
    /** Editing option to write the Epoch */
    static public final short EDIT_EPOCH = 0x400;
    /** Editing option to write the MeanEpoch */
    static public final short EDIT_MEAN_EPOCH = 0x800;
    /** Editing option to show all decimals */
    static public final short EDIT_FULL  = 0x1000;
    /** The default edition options */
    static public final short EDIT_DEFAULT = 
            EDIT_FRAME|EDIT_EPOCH|EDIT_MEAN_EPOCH;

    static final private String[] explain_precision = {
        "unknown", "1degree", "0.1degree", "1arcmin", "0.1arcmin", "1arcsec",
        "0.1arcsec", "10mas", "1mas", "0.1mas", "10\u00b5as",
        "1\u00b5as", "0.1\u00b5as"
    } ;
    static final private String[] explain_edition = {
        "frame", "separate_components", "epoch", "meanEpoch",
        "full_precision"
    };

    /** Definitions of the symbols for the Edition of Coordinates */
    static final char[] editing_options = {
        's', 	// Sexagesimal edition
        'd', 	// Decimal edition
        ':', 	// Sexagesimal edition with :
        'u', 	// Sexagesimal edition with units
        'f',	// Edit the Frame
        'F',	// Edit with Full precision
        '2',	// Edit with a blank between numbers
        'E',	// Edit the epoch
        'M',	// Edit the Mean Epoch 
    };
    static final String string_options = new String(editing_options);
    static final short[] Editing_options = {
        Editing.SEXA3, 		// 's'
        Editing.DECIMAL,	// 'd'
        Editing.SEXA3c,		// ':'
        Editing.SEXA3h,		// 'u'
        EDIT_FRAME,		// 'f'
        EDIT_FULL,		// 'F'
        EDIT_2NUMBERS,		// '2'
        EDIT_EPOCH,		// 'E'
        EDIT_MEAN_EPOCH,	// 'M'
    };
    /** 
     * Explain the editing option of the coordinate
     * @return A printable variant of format.
     **/
    static public final String explain(int editing) {
        String xplain1 = Astroformat.explain(editing);
        int flags = editing >> 8;
        int i;
        if (flags == 0) return (xplain1);
        StringBuffer buf = new StringBuffer(xplain1);
        for (i=0; flags != 0; i++, flags >>= 1) {
            if ((flags&1) == 0) continue;
            if(buf.length()>0) buf.append(',');
            buf.append(explain_edition[i]);
        }
        return(buf.toString());
    }

    //  ===========================================================
    //			Constructors
    //  ===========================================================

    /**
     * Create the default ICRS Astrocoo
     */
    public Astrocoo() {
        this(new ICRS()) ;
    }

    /**
     * Create an Astrocoo object: specify just frame, equinox, precision.
     * Actual positions are normally specified by the <B>set</B> method.
     * @param frame	one of the possible Astroframes
     */
    public Astrocoo(Astroframe frame) {
        this.frame = frame;
        epoch = 0./0.;				// Unspecified epoch
        dlon = dlat = precision = 0;
        setEditing(); // (V1.81) editing = (short)(frame.ed_lon|EDIT_DEFAULT);
        super.set();
    }

    /**
     * Create an Astrocoo object with a known position.
     * @param frame	one of the possible Astroframes
     * @param lon Longitude of the position (degrees)
     * @param lat Latitude of the position (degrees)
     */
    public Astrocoo(Astroframe frame, double lon, double lat) {
        this(frame, lon, lat, 0./0.);
    }

    /**
     * Create an Astrocoo object with a known position.
     * @param frame	one of the possible Astroframes
     * @param coo	the position.
     --- Too many constructors !
     public Astrocoo(Astroframe frame, Coo coo) {
	this(frame, coo, 0./0.);
    }
     */

    /**
     * Create an Astrocoo object from an existing one.
     * It is equivalent to (<em>coo</em>.clone()).convertTo(<em>frame</em>)
     * @param frame	one of the possible Astroframes
     * @param coo	the position in some other frame.
     */
    /* --- Useless
     public Astrocoo(Astroframe frame, Astrocoo coo) {
       	this.frame = frame;
	editing = (short)(frame.ed_lon|EDIT_FRAME|EDIT_EPOCH);
	this.convertFrom(coo);
    o}
    --- */

    /**
     * Create an Astrocoo object with a known position + Epoch
     * @param frame	one of the possible Astroframes
     * @param coo	the position.
     * @param epoch 	Epoch of the position, in Jyr (see Astrotime)
     --- Too many constructors !
     public Astrocoo(Astroframe frame, Coo coo, double epoch) {
       	this.frame = frame;
	editing = (short)(frame.ed_lon|EDIT_FRAME|EDIT_EPOCH);
    	this.set(coo, epoch);
    }
     */

    /**
     * Create an Astrocoo object with a known position at given epoch.
     * @param frame	one of the possible Astroframes
     * @param lon Longitude of the position (degrees)
     * @param lat Latitude of the position (degrees)
     * @param epoch Epoch of the position, in Jyr (see Astrotime)
     */
    public Astrocoo(Astroframe frame, double lon, double lat, double epoch) {
        this.frame = frame;
        setEditing(); // (V1.81)editing = (frame.ed_lon|EDIT_FRAME|EDIT_EPOCH);
        this.epoch = epoch;		// Specified epoch
        dlon = dlat = precision = 0; 	// Precision unknown
        super.set(lon, lat);
    }

    /**
     * Create an Astrocoo object from a position (Epoch)
     * @param frame	one of the possible Astroframes
     * @param text Text with position, possibly followed by an epoch.
     * 		Valid texts are e.g.
     * 		"12 34 12.45 -42 12 76.4 J1991.25"    or
     * 		"12 34 12.45 -42 12 76.4 (J1991.25)"
     */
    public Astrocoo(Astroframe frame, String text) throws ParseException {
        this.frame = frame;
        this.set(text);
    }

    /**
     * Create an Astrocoo object from just a string.
     * @param text Text with frame, position, possibly followed by an epoch.
     * 		Valid texts are e.g.
     * 		"J2000: 12 34 12.45 -42 12 76.4 J1991.25"    or
     * 		"(ICRS) 12 34 12.45 -42 12 76.4 (J1991.25)"
     * 		"J123456.12+781245.1" (IAU-type)
     */
    public Astrocoo(String text) throws ParseException {
        Parsing txt = new Parsing(text);
        // No blank, no colon ==> IAU-style
        if ((text.indexOf(' ')<0) && (text.indexOf(':')<0)) {
            char c = txt.currentChar(); txt.advance(1);
            if (Astroframe.isIAU(c)) {
                frame = Astroframe.create(c);
                editing = (short)(frame.ed_lon|EDIT_FRAME|EDIT_EPOCH); // V1.81
                while(txt.pos<txt.length) {
                    c = txt.currentChar(); txt.advance(1);
                    if (Character.isDigit(c) || (c == '.')) ;
                    else break;
                }
                if ((frame != null) && Parsing.isSign(c)) {
                    this.setIAU(text);
                    return;
                }
            }
        }
        this.frame = Astroframe.parse(txt);
        if(DEBUG) System.out.println("#---Astrocoo(" + text + ") => frame=" 
                + this.frame);
        if (frame == null) throw new ParseException
        ("****Astrocoo: argument '" + text + "' (no frame)", txt.pos);
        editing = (short)(frame.ed_lon|EDIT_DEFAULT); // V1.81 (2 numbers)
        this.set(txt.toString());
    }

    public Object clone() {
        Astrocoo c = (Astrocoo) super.clone();
        return c;
    }


    //  ===========================================================
    //			Interpretation of text (Parsing)
    //  ===========================================================
    //

    /**
     * Interpret an epoch, eventually within ( ).
     * We accept JD, MJD, J or B times.
     * @param txt Text to interpret
     * @return    the epoch in Jyr, NULL if not found
     **/
    public static double getEpoch(Parsing txt) {
        Astrotime t = new Astrotime();
        int posini = txt.pos;
        boolean bracketed, has_epoch;
        double epoch = 0./0.;
        txt.gobbleSpaces();
        if ((bracketed = txt.match("("))) txt.gobbleSpaces();
        else if (Character.isLetter(txt.currentChar())) ;
        else {
            txt.set(posini);
            return(0./0.);
        }
        if ((has_epoch = t.parsing(txt))) {
            epoch = t.getJyr();
            if (bracketed) 		// Must match the )
                has_epoch = txt.match(")");
        }
        if (!has_epoch) {		// Epoch not matched, ignore...
            epoch = 0./0.;
            txt.set(posini);
        }
        return(epoch);
    }

    /**
     * Interpret the string and convert to Coo + Epoch.
     * Called from set and parse routines.
     * @param txt to interpret as a Parsing object "12 34.5 -72 54 J1991.25"
     * @param option is 1 for IAU-format
     * @return true if OK (2 coordinates found, and possibly an epoch).
     * 	If false returned, current position in txt is unchanged.
     **/
    private boolean parse2(Parsing txt, int option) {
        boolean f15 = frame.hms;
        boolean sexa;
        double lon, lat;
        precision = dlon = dlat = 0;
        int posini = txt.pos;
        int pos, form;
        set(); this.epoch = 0./0.;

        // Extract the longitude or RA
        if (DEBUG) System.out.print("#...parse2(" + txt + "," + option 
                + "), frame=" + frame 
                + ", edit=(" + frame.ed_lon + "," + frame.ed_lat + ")");
        if ((option==1) && f15) {   // In IAU-format, special parsing
            //System.out.print("parse2("+txt+"): lon=");
            lon = txt.parseIAU();
            //System.out.println(lon);
        }
        else {
            txt.gobbleSpaces();
            lon = txt.parseSexa2();	// First part of 2 numbers
        }
        //System.out.println("parse2("+txt+"): lon=" + lon);
        dlon = (byte)(1+txt.decimals());
        if (txt.inError() || (dlon==0)) { txt.set(posini); return(false); }
        sexa = txt.isSexa();
        formRA = (byte)txt.format();	// Tells how RA/Longitude was coded.
        // Modify time to angle if necessary, e.g. "12h38m-27&deg;23'
        if (option != 1) {
            if (txt.isTime()) f15 = true;
            else if (txt.isAngle()) f15 = false;
            else if (sexa) ;	// Stick to default hms
            else if (dlon>0)	// Decimal values are always degrees.
                f15 = false;
        }
        if (f15) { lon *= 15.; if (dlon>0) dlon--; }
        // Extract the Latitude or Dec
        if ((option==1) && f15) {
            lat = txt.parseIAU();
        }
        else {
            txt.gobbleSpaces();
            lat = txt.parseSexa();
        }
        //System.out.println("parse2("+txt+"): lat=" + lat);
        dlat = (byte)(1+txt.decimals());// Don't accept latitude in time...
        if (txt.inError() || (dlat == 0)) { txt.set(posini); return(false); }
        sexa |= txt.isSexa();
        // Precision is the highest number of decimals
        // The actual precision should be the lowest, but...
        precision = dlon>dlat ? dlon : dlat;
        /*+++  Don't touch at the Editing options !
	// Set the editing options.
 	editing = (editing&(~0xff)) | formRA; 	// The RA way of edition.
	if (sexa && !Astroformat.isSexa(editing)) 
	    editing = (short)Editing.SEXA3c;
	---*/
        // Complete the coordinate
        super.set(lon, lat);
        // ... nothing more in case of IAU positions.
        if (DEBUG) System.out.println(" (ok for lon/lat)");
        if (option==1) return(true);

        // Position may be followed by an epoch -- the epoch may be within ()
        pos = txt.pos;
        txt.gobbleSpaces();
        if (txt.pos < txt.length) { 
            Astrotime t = new Astrotime(); 
            boolean has_epoch, bracketed;
            if ((bracketed = txt.match("(") )) txt.gobbleSpaces();
            if ((has_epoch = t.parsing(txt))) {
                this.epoch = t.getJyr();
                if (bracketed) 		// Must match the )
                    has_epoch = txt.match(")");
            }
            if (!has_epoch) {		// Epoch not matched...
                this.epoch = 0./0.;
                txt.set(pos);
            }
        }
        return(true);
    }

    /**
     * Interpret the string and convert to Coo + Epoch.
     * Called from set and parse routines.
     * @param txt to interpret as a Parsing object "12 34.5 -72 54 J1991.25"
     * @return true if OK (2 coordinates found, and possibly an epoch).
     * 	If false, no mouvement in txt.
     **/
    public boolean parsing(Parsing txt) {
        return(parse2(txt, 0));
    }

    /**
     * Interpret the string representing a position in IAU-style.
     * @param txt to interpret as a Parsing object "12 34.5 -72 54 J1991.25"
     * @return true if OK (2 coordinates found, corresponding to frame)
     * 	If false, no mouvement in txt.
     **/
    public boolean parseIAU(Parsing txt) {
        int posini = txt.pos;
        char c = txt.currentChar();
        if (Astroframe.isIAU(c)) {
            if(DEBUG) System.out.println("#...parseIAU(" + txt + "): frame=" 
                    + frame);
            if (frame == null) { 	// V1.81
                frame = Astroframe.create(c);
                this.setEditing();
            }
            if (frame.equals(c)) txt.advance(1);
            else return(false);
        }
        boolean st = parse2(txt, 1);
        if (!st) txt.set( posini);
        return(st);
    }

    /**
     * Parsing method: interpret a String.
     * @param txt to interpret.
     * @param offset starting position in text
     * @return new position.
     **/
    public int parse (String txt, int offset) {
        Parsing t = new Parsing(txt, offset);
        if (parsing(t)) return(t.pos);
        return(offset);
    }

    //  ===========================================================
    //			Set in Astrocoo
    //  ===========================================================

    /**
     * Inherited from super class (Coo): 
     *     set(lon, lat)  
     *     set(x, y, z) 
     *     set(Coo)
     **/

    /**
     * Set a position from an existing one (copy).
     * @param coo  the Astrocoo to copy.
     **/
    public void set(Astrocoo coo) {
        super.set((Coo)coo);
        this.frame = coo.frame;
        this.epoch = coo.epoch;
        this.dlon  = coo.dlon;
        this.dlat  = coo.dlat;
        this.precision = coo.precision;
    }

    /**
     * Set a position from a text which may contain position and Epoch.
     * The frame is not modified.
     * The precision is adapted to the number of significant digits
     * existing in the input text string.
     * @param text  Longitude + latitude in text
     * @throws  ParseException when the text can't br fully interpreted.
     **/
    public void set(String text) throws ParseException {
        Parsing t = new Parsing(text);
        if (parse2(t, 0))		// Various items found.
            t.gobbleSpaces();
        if (t.pos != t.length) throw new ParseException
        ("****Astrocoo: argument '" + text + "'", t.pos);
    }

    /**
     * Set a position from a text -- accept IAU-format (HHMM...+DDMM...).
     * The frame is not modified.
     * @param text  HHMM..+DDMM.. or classical sexagesimal/decimal coordinates.
     * @throws  ParseException when the argument text is neither an IAU string,
     * 			nor a position.
     **/
    public void setIAU(String text) throws ParseException {
        Parsing t = new Parsing(text);
        char c = t.currentChar();
        if (Astroframe.isIAU(c)) {
            if (frame.equals(c)) t.advance(1);
            else throw new ParseException
            ("****Astrocoo: setIAU(" + c + "...) for " + frame, t.pos);
        }
        if (DEBUG) System.out.println("#...setIAU(" + text +"), editing=0x"
                + Integer.toHexString(editing));
        if (parse2(t, 1))		// Various items found.
            t.gobbleSpaces();
        // Accept standard string 
        else if (parse2(t, 0))
            t.gobbleSpaces();
        if (t.pos != t.length) throw new ParseException
        ("****Astrocoo: setIAU '" + text + "'", t.pos);
    }

    /**
     * The following 'set' methods are inherited from the parent
     * set(lon, lat); set(x, y, z); set(Coo); set();
     * For these methods, the Epoch is kept.
     **/

    /**
     * Set position + epoch.
     * The precision is not changed.
     * @param coo   the lon+lat
     * @param epoch the epoch.
     **/
    public void set(Coo coo, double epoch) {
        this.epoch = epoch;
        super.set(coo);
    }

    /**
     * Change the precision of the data
     * @param  precision integer number, typically one of the values NONE (0),
     * 	DEG (1), ARCMIN (3), ARCSEC (5),  MAS (8);
     * 	use ARCSEC+1 for 0.1arcsec, MAS-1 for 10mas, etc...
     **/
    public void setPrecision(int precision) {
        this.precision = (byte)precision;
    }

    /**
     * Change the precision of the data
     * @param  dlon Precision number on longitude (RA).
     * @param  dlat Precision number on latitude (Dec).
     **/
    public void setPrecision(int dlon, int dlat) {
        this.dlon = (byte)dlon;
        this.dlat = (byte)dlat;
        this.precision = this.dlon>this.dlat ? this.dlon : this.dlat;
    }

    /**
     * Change the default way of edition
     * @param  edit_option  A mixture of {@link Astroformat} options,
     * 			and EDIT_FRAME EDIT_FULL.
     **/
    public void setEditing(int edit_option) {
        this.editing = (short)(edit_option&0xffff);
        if ((edit_option&EDIT_FULL)!=0) 	// Full precision ~ 0.01uas
            setPrecision(12);
    }

    /**
     * Change the default way of edition
     * @param options  a string with the option letters (see edit)
     * @see   Astrocoo#editingOptions
     **/
    public void setEditing(String options) {
        setEditing(editingOptions(options));
    }

    /**
     * Set the default editing option (added V1.81)
     **/
    public void setEditing() {
        int o = EDIT_DEFAULT;
        if (frame != null) o |= frame.ed_lon;
        setEditing(o);
    }

    /**
     * Change the Epoch of the data
     * @param  epoch the epoch of the coordinates 
     * @return true when OK.
     **/
    public boolean setEpoch(double epoch) {
        this.epoch = epoch;
        return(true);
    }

    //  ===========================================================
    //			Get parts of Astrocoo
    //  ===========================================================

    /**
     * Get an explicit designation of the frame
     * @return	the explanation as a string
     **/
    public final Astroframe getFrame() {
        return(frame);
    }

    /**
     * Get the precision of the current value
     * @return	the value.
     **/
    public final int getPrecision() {
        return precision;
    }

    /**
     * Get the precision on the latitude
     * @return the number representing the precision of the latitude
     */
    public byte getLonPrec() {
        return (dlon);
    }

    /**
     * Get the precision on the longitude
     * @return the number representing the longitude precision
     */
    public byte getLatPrec() {
        return (dlat);
    }

    /**
     * Get the defaut editing option.
     * Can be edited via e.g. Astroformat.explain()
     * @return	the value.
     **/
    public final int getEditing() {
        return editing;
    }

    //  ===========================================================
    //			Dump the coordinates
    //  ===========================================================

    /**
     * Dump the contents of an Astrocoo.
     * @param title A title to precede the dump
     **/
    public void dump(String title) {
        StringBuffer b = new StringBuffer(256);
        int i; double e;
        // Subtitle = same length as title, but blank.
        for (i=title.length();--i>=0;) b.append(' ');
        String blanks = b.toString(); b.setLength(0);
        b.append(title); b.append("Astroframe="); b.append(this.frame);
        b.append(", def.Ep=J");
        b.append(this.frame.base_epoch);
        /* Edit epoch below 
	b.append(";");
	if (Double.isNaN(this.epoch)) { 
	    b.append(" defEp=J"); 
	    e = this.frame.base_epoch; 
	}
	else { b.append(" epoch=J"); e = this.epoch; }
	b.append(e); 
         */
        b.append('\n'); b.append(blanks);
        b.append("editing=0x"); b.append(Integer.toHexString(editing));
        b.append('='); b.append(explain(editing));
        b.append('\n'); b.append(blanks);
        b.append("precision="); b.append(precision);
        b.append(", dlon="); b.append(dlon);
        b.append(", dlat="); b.append(dlat);
        b.append(", formRA=0x"); b.append(Integer.toHexString(formRA));
        b.append('('); b.append(Astroformat.explain(formRA)); b.append(')'); 
        // Add the position + Epoch
        b.append('\n'); b.append(blanks); b.append("  "); super.editCoo(b, 12);
        b.append("  Epoch=J"); b.append(this.epoch);
        System.out.println(b.toString());
        super.dump(blanks);
    }

    //  ===========================================================
    //			Compare two Coordinates
    //  ===========================================================

    /**
     * Compare 2 coordinates.
     * @param o Objet a comparer.
     * @return Vrai si o est identique a this.
     **/
    public boolean equals(Object o) {
        boolean res = false;
        if(o instanceof Astrocoo) {
            Astrocoo a = (Astrocoo)o;
            res = this.frame.equals(a.frame)	// Must have same frame
                    && super.equals(a);		// and same position
        }
        return res;
    }

    /**
     * Compute the hashcode
     * @return the hascode value
     */
    public int hashCode() {
        int hcode = frame.hashCode();
        hcode = hcode * 123 + super.hashCode();
        return hcode;
    }

    //  ===========================================================
    //			Edit the Coordinates
    //  ===========================================================

    /**
     * Convert the String of editing options into an integer
     * @param text List of options:<PRE>
     *	d = edit in Decimal
     *	s = edit in Sexagesimal
     *	: = separate sexagesimal parts with the colon
     *	u = separate sexagesimal parts with the relevant unit (h m s d m s)
     *	f = edit also the frame (system+equinox)
     *	F = edit in full precision (ignore the precision of the system)
     * 	2 = edit with a blank between numbers
     *	E = edit the epoch
     *	M = edit the Mean Epoch 
     *	* = default edition options
     *</PRE>
     **/
    public static int editingOptions(String text) 
            throws IllegalArgumentException
            {
        char b[] = text.toCharArray();
        int i, j, n; int o = 0;
        for (i=0; i<b.length; i++) {
            if (b[i] == '*') { o |= EDIT_DEFAULT; continue; }
            for (j=0; j<editing_options.length; j++) {
                if (b[i] == editing_options[j]) break;
            }
            if (j==editing_options.length) throw new IllegalArgumentException
            ("****Astrocoo: '" + text + "'; accepted="
                    + string_options);
            if (Editing_options[j] < 0x10) o &= ~0xf;
            o |= Editing_options[j];
        }
        return(o);
            }

    /**
     * Function to edit one number representing an apoch in Jyr
     * @param buf  Buffer where the result is appended as Jxxx or Bxxx
     * @param Jyr The epoch, in Julian Years. A NaN value is edited as null.
     * @return	the StringBuffer
     **/
    protected final StringBuffer editEpoch(StringBuffer buf, double Jyr) {
        double ep = epoch;
        if (Double.isNaN(ep)) 
            buf.append(' ');
        else if (this.frame instanceof FK4) {
            buf.append('B');
            ep =  Astrotime.J2B(epoch);
        }
        else buf.append('J');
        // Edit number Up to 3 decimals
        ed.editDecimal(buf, ep, 4, -3, Editing.ZERO_FILL);
        return(buf);
    }

    /**
     * FXP: I let this method for backward compatibility.
     * In my opinion:
     * StringBuffer --> StringBuilder
     * return type StringBuffer --> void
     */
    public final StringBuffer edit(final StringBuffer buf, final int opt) {
        return this.edit(buf, opt, this.precision & 15);
    }
    
    /**
     * FXP: I let this method for backward compatibility.
     * In my opinion:
     * StringBuffer --> StringBuilder
     * return type StringBuffer --> void
     */
    /*public final StringBuffer edit(final StringBuffer buf, final int opt,
            final int prec) {
        final StringBuilder strBuild = new StringBuilder();
        this.edit(strBuild, opt, prec);
        buf.append(strBuild);
        return buf;
    }*/
    
    /**
     * Method to edit the Coordinates in a StringBuffer
     * @param buf  Buffer where the result is appended
     * @param opt  A mixture of the options ED_COLON, ED_DECIMAL,
     *          EDIT_FULL, EDIT_SEXA, EDIT_FRAME, EDIT_2NUMBERS
     * @return  the StringBuffer
     **/
    /*public final StringBuilder edit(final StringBuilder buf, final int opt) {
        return this.edit(buf, opt, this.precision & 15);
    }*/
    
    /**
     * Method to edit the Coordinates in a StringBuffer
     * @param buf  Buffer where the result is appended
     * @param opt  A mixture of the options ED_COLON, ED_DECIMAL,
     *			EDIT_FULL, EDIT_SEXA, EDIT_FRAME, EDIT_2NUMBERS
     * @param prec precission
     * @return	the StringBuffer
     **/
    public final StringBuffer edit(final StringBuffer buf, final int opt, int prec) {
        boolean f15 = false;
        if (prec == 0) { 	// Precision unknown: up to precision of frame.
            prec = frame.precision;
        } else if (prec > frame.precision) { 	// Limit to intrinsic precision
            prec = frame.precision;
        }
        int o = opt & (~(EDIT_FULL | EDIT_FRAME));
        if (DEBUG) {
            System.out.println(
                "....edit(opt=0x" + Integer.toHexString(opt) 
                + "), epoch=" + epoch 
                + ", precision=0x" + Integer.toHexString(precision)
                + ", dlon=" + dlon + ", dlat=" + dlat 
                + ", lon=" + lon + ", lat=" + lat);
        }
        if ((opt & EDIT_FULL) != 0) { // Edit full precision
            prec=12;
        }
        // Edit the Frame
        if ((opt & EDIT_FRAME) != 0) {
            buf.append(frame.toString());
            buf.append(' ');
        }
        // Edit the RA or Longitude
        double x = this.getLon();
        int nint = 3;
        int nd = prec;
        // Must it be expressed in time ?
        if (Astroformat.isSexa(o)) {
            f15 = frame.hms;
        }
        /*--- old code:
	    if (Astroformat.isTime(o))       f15 = true;
	    else if (Astroformat.isAngle(o)) f15 = false;
	    else if (Astroformat.isSexa(o))  f15 = frame.hms;
	    --- */
        if (f15) {
            nint--;
            x /= 15.0;
            if (nd>0) { 	// v1.4a: Sexagesimal at least h min 
                nd++; 
                if (nd<3) {
                    nd=3;
                }
            } else {
                nd--;
                if (nd>-3) {
                    nd=-3;
                }
            }
        }
        // editDecimal does edit in Sexagesimal if this option is set.
        ed.editDecimal(buf, x, nint, nd-1, Editing.ZERO_FILL|o);
        // Separate the numbers if wished
        if ((opt & EDIT_2NUMBERS) != 0) {
            buf.append(' ');
        }
        // Edit the Dec or Latitude
        if (Astroformat.isTime(o)) {
            if (Astroformat.isSexa(o)) {
                o -= 2;	// Replace h m s by d m s
            } else {
                o++;
            }
        }
        ed.editDecimal(buf, this.getLat(), 3, prec - 1,
                Editing.ZERO_FILL | Editing.SIGN_EDIT | o);

        // Add the Epoch within parentheses
        if (((opt&EDIT_EPOCH)!=0) && (!Double.isNaN(epoch))) {
            buf.append(" (");
            editEpoch(buf, epoch);
            buf.append(')');
        }
        return (buf);
    }

    /**
     * Customized edition of Coordinates to a String
     * @param  edit_option  A mixture of {@link Astroformat} options,
     * 			and EDIT_FRAME EDIT_FULL.
     * @return the edited coordinates in a string
     */
    public String toString(int edit_option) {
        StringBuffer buf = new StringBuffer(80) ;
        this.edit(buf, edit_option) ;
        return buf.toString();
    }

    /**
     * Customized edition of Coordinates to a String
     * @param options  a string with the option letters (see edit)
     * @see   Astrocoo#editingOptions
     * @return the edited coordinates in a string
     */
    public String toString(String options) throws IllegalArgumentException {
        StringBuffer buf = new StringBuffer(80) ;
        this.edit(buf, editingOptions(options)) ;
        return buf.toString();
    }

    /**
     * Default edition: use what's stored
     * @return the edited string
     **/
    public String toString() {
        // System.out.println("equinox==" + equinox) ;
        // System.out.println("z=" + z) ;
        StringBuffer buf = new StringBuffer(80) ;
        this.edit(buf, editing) ;	// Frame Edition
        return buf.toString();
    }

    /**
     * Edition with IAU-style.
     * @param IAUtemplate a string starting by J B G (for IRCS/FK4/Galactic).
     * 	      A conversion is done if necessary.
     * 	      Examples of valid templates:<UL>
     * 	  <LI>JHHMMSSss+DDMMSSs   (as in 2MASS)
     * 	  <LI>GLLL.llll+BB.bbbb   (as in MSX)
     * 	  <LI>JHHMM.m+DDMM        (as in RX)
     * 	  <LI>JHHMMm+DDMM         (as in WDS)
     * 	  <LI>BHHMM+DDd           (as in PKS)
     *   </UL>
     * @return the edited string. 
     * @throws  ParseException when the argument text is incompatible.
     **/
    public String toIAU(String IAUtemplate) throws ParseException {
        Astrocoo position = this;
        Editing ed = new Editing();
        char letter = IAUtemplate.charAt(0);
        char Letter;
        Astroframe frame = null;

        // Verify a coordinate transformation is required ?
        if (letter == 'J') {
            if (this.frame instanceof ICRS) frame = this.frame;
            else if (this.frame instanceof FK5) {
                FK5 f = (FK5)this.frame;
                if (f.equinox == 2000) frame = this.frame;
            }
            if (frame == null) frame = new ICRS();
        }
        else if (letter == 'B') {
            if (this.frame instanceof FK4) {
                FK4 f = (FK4)this.frame;
                if (f.equinox == 1950) frame = this.frame;
            }
            if (frame == null) frame = new FK4();
        }
        else if (letter == 'G') {
            if (this.frame instanceof Galactic) frame = this.frame;
            if (frame == null) frame = new Galactic();
        }
        else throw new ParseException(
                "****Astrocoo.toIAU: template does not start with J|B|G", 0);

        // Compute the position in the correct frame
        if (frame != this.frame) {
            position = new Astrocoo(frame);
            convert(this, position);
        }

        // Parse the template
        char b[] = IAUtemplate.toCharArray();
        StringBuffer bed = new StringBuffer();
        bed.append(letter);
        int nint, nd, opt, sexa;
        double value = position.lon;
        boolean bad, has_point;
        int i, k=0;			// 0=lon, 1=lat
        nint=nd=sexa=0; bad = false; has_point = false;
        opt=Astroformat.ZERO_FILL|Astroformat.TRUNCATE;
        for (i=1; (i<b.length) && (k<2); ) {
            if (b[i] == '+') { 
                opt |= Astroformat.SIGN_EDIT; 
                i++; nint++; 
                continue; 
            }
            letter = b[i]; Letter = Character.toUpperCase(letter);
            if ((Letter == 'H') && (k == 0)) {
                bad = (!frame.hms);
                value /= 15.;
            }
            else if (Letter == 'L') {
                bad = frame.hms || (k!=0);
            }
            else if (Letter == 'B') {
                bad = frame.hms || (k == 0);
            }
            else if (Letter == 'D') ;
            else bad = true;
            if (bad) throw new ParseException(
                    "***Astrocoo.toIAU: '" + letter + "' invalid", i);
            // Count the number of occurences of letter 
            // ==> number of digits _before_ the decimal point.
            has_point = false;
            while ((i<b.length) && (b[i] == letter)) { i++; nint++; }
            while ((i<b.length) && (!bad)) {
                if ((b[i] == '+') && (k == 0)) break;
                if (b[i] == '.') { has_point = true; i++; continue; }
                char M = Character.toUpperCase(b[i]);
                /*System.out.println("# k=" + k + ", i=" + i + ": b[i]=" + b[i] 
			+ ", M=" + M 
			+ ", Letter=" + Letter + ", nint=" + nint + ", nd=" + nd
			+ ", sexa=" + sexa + ", opt=" + opt); */
                if ((M == Letter)||(b[i] == 'f')) { 	// Decimals 
                    letter = b[i];
                    while ((i<b.length) && (b[i] == letter)) { i++; nd++; }
                    break;
                }
                else if ((M == 'M') && ((Letter == 'D') || (Letter == 'H'))) {
                    letter = b[i]; Letter = M;
                    while ((i<b.length) && (b[i] == letter)) { i++; nd++; }
                    sexa = Astroformat.SEXA2;
                    continue;
                }
                else if ((M == 'S') && (Letter == 'M')) {
                    letter = b[i]; Letter = M;
                    while ((i<b.length) && (b[i] == letter)) { i++; nd++; }
                    sexa = Astroformat.SEXA3;
                    continue;
                }
                else bad = true; 
            }
            if (bad) throw new ParseException(
                    "***Astrocoo.toIAU: '" + b[i] + "' invalid", i);
            int j = bed.length();
            if (DEBUG) System.out.print("# Edition(nint=" + nint + ",nd=" 
                    + nd + ",opt=" + opt + "): " + bed); 
            ed.editDecimal(bed, value, nint, nd, opt|sexa);
            // Remove the decimal points and blanks
            if ((sexa!=0) || (!has_point)) while (j < bed.length()) {
                letter = bed.charAt(j);
                if (letter == ' ') bed.deleteCharAt(j);
                else if ((letter == '.') && (!has_point))  bed.deleteCharAt(j);
                else j++;
            }
            if (DEBUG) System.out.println (" => " + bed); 
            nint=nd=sexa=0; bad = false; has_point = false;
            opt=Astroformat.ZERO_FILL|Astroformat.TRUNCATE;
            value = position.lat;
            k++;
        }
        if ((i<b.length) || (k!=2)) throw new ParseException(
                "***Astrocoo.toIAU: '" + b[i] + "' invalid", i);
        return(bed.toString());
    }

    //  ===========================================================
    //			Convert Coordinates
    //  ===========================================================

    /**
     * Transform the position into another frame.
     * @param new_frame	The frame of the resulting position.
     **/
    public void convertTo(Astroframe new_frame) {
        /* Verify first if frames identical -- then nothing to do ! */
        if (this.frame.equals(new_frame)) {
            if(DEBUG) System.out.println("....Frame " + this.frame 
                    + "=" + new_frame);
            return;
        }

        // Move via ICRS
        if(DEBUG) System.out.println("....Astrocoo.convert: via ICRS:  "
                + this.frame + " => ICRS => " + new_frame);
        this.frame.toICRS((Coo)this);	// Position now in ICRS
        if(DEBUG) this.dump("#ICRS: ");
        new_frame.fromICRS((Coo)this);	// Position now in new_frame
        if(DEBUG) this.dump("#NewF: ");
        this.frame = new_frame;
        this.lon = this.lat = 0./0.;	// Actual angles not recomputed
    }

    /**
     * Transform the celestial position.
     * @param source	Source position (with its frame)
     * @param target	Target position (coordinates part replaced)
     **/
    public static void convert(Astrocoo source, Astrocoo target) {
        Coo coo = (Coo) target;
        coo.set((Coo)source);		// Sets x y z lon lat
        if (target.frame.equals(source.frame)) {
            if(DEBUG) System.out.println("....Astrocoo.convert: Frame " 
                    + source.frame + "=" + target.frame);
            return;
        }
        source.frame.toICRS(coo);	// now in ICRS
        if(DEBUG) target.dump("#ICRS: ");
        target.frame.fromICRS(coo);
        if(DEBUG) target.dump("#NewF: ");
    }


    /**
     * Transform the celestial position given in the argument into
     * 	its own frame.
     * @param coo	Position expressed in some other frame (not modified)
     **/
    /* -- Removed (use convertTo)
    public void convertFrom(Astrocoo coo) {
      Astroframe new_frame = this.frame;

	// Copy the epoch & precision definitions.
	this.epoch = coo.epoch;
	this.dlon  = coo.dlon;
	this.dlat  = coo.dlat;
	this.precision = coo.precision;
	this.formRA = coo.formRA;
	// Editing options: no change.

	// Copy coordinate details.
	super.set((Coo)coo);
	this.frame = coo.frame;

	this.convertTo(new_frame);
  }
  -- */

}
