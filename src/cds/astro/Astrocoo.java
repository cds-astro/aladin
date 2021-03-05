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
 * @version 2.0 : 08-Feb-2019: (x y z) has the Eterm removed
 */
public class Astrocoo extends Coo implements Serializable {
	static public boolean DEBUG = false;
	static protected double eps = AstroMath.eps;	// for test()
	//* One Coo consists in lon lat x y z (see Coo class)
	/** The associated frame */
	public Astroframe frame=null;
	/** Epoch (Jyr) of the position.
	 * This epoch corresponds to the date of observation, 
	 * and may differ from the {@link Astroframe#epoch}.
	 * Is set to <code>NaN</code>, unless explicitely supplied.
	 */
	public double epoch ;	// Epoch in Jyr.
	/** The precision (number of decimals+1) of the longitude */
	protected byte dlon;	// Original decimals for lon
	/** The precision (number of decimals+1) of the latitude */
	protected byte dlat;	// Original decimals for lat
	/** 
	 * The precision is 0=unknown, 1=DEG, 3=ARCMIN, 5=ARCSEC, 8=MAS, 11=uas, 14=nas;
	 * it can be modified by the {@link setPrecision}.
	 * There is also an intrinsic precision associated to the frame. 
	 **/
	protected byte precision ;	// 0 = unknown, 1=deg, 3=arcmin, 5=arcsec, 8=mas 11=µas
	/** How RA was entered, when set by a String; see {@link Astroformat} */
	protected byte formRA;	// Entered format of RA / Longitude */

	/** The editing option includes basic options described in 
	 * {@link Astroformat}, plus the EDIT_FRAME, EDIT_FULL and EDIT_2 options.
	 * It can be changed via the {@link #setEditing} method.
	 **/
	protected int editing;

	/** Definitions of Precisions for the angles */
	static public final byte NONE = 0, DEG = 1, ARCMIN=3, ARCSEC=5, MAS=8 ;

	/** (f) Editing option to insert the name of the frame */
	static public final int EDIT_FRAME = 0x100;
	/** (2) Editing option to separate the 2 components by a blank */
	static public final int EDIT_2NUMBERS  = 0x200;
	/** (E) Editing option to write the Epoch */
	static public final int EDIT_EPOCH = 0x400;
	/** (M) Editing option to write the MeanEpoch */
	static public final int EDIT_MEAN_EPOCH = 0x800;
	/** (F) Editing option to show all decimals */
	static public final int EDIT_FULL  = 0x1000;
	/** (5) Editing option to show the 5xsigma's rather than error ellipses */
	static public final int EDIT_5SIGMAS = 0x2000;
	/** (=) Editing option to add the name of the parameters as µ=  ϖ= etc  */
	static public final int EDIT_NAMING = 0x4000;
	/** (C) Editing option to add the correlations in the edition */
	static public final int EDIT_CORRELATIONS = 0x8000;
	/** The default edition options */
	static public final int EDIT_DEFAULT = 
			EDIT_FRAME|EDIT_EPOCH|EDIT_MEAN_EPOCH;

	static final protected String[] explain_precision = {
			"unknown", "1degree", "0.1degree", "1arcmin", "0.1arcmin", "1arcsec",
			"0.1arcsec", "10mas", "1mas", "0.1mas", "10\u00b5as",
			"1\u00b5as", "0.1\u00b5as", "10nas", "1nas"
	} ;
	static final protected String[] explain_edition = {
			"frame", "2separate_components", "Epoch", "MeanEpoch",
			"Full_precision", "5sigmas", "=parameter_name", "correlations"
	};

	/** Definitions of the symbols for the Edition of Coordinates */
	static final public char[] editing_options = {
			's', 	// Sexagesimal edition
			'd', 	// Decimal edition
			':', 	// Sexagesimal edition with :
			'u', 	// Sexagesimal edition with units
			'f',	// Edit the Frame
			'F',	// Edit with Full precision
			'2',	// Edit with a blank between numbers
			'E',	// Edit the epoch
			'M',	// Edit the Mean Epoch 
			'5',	// Edit the 5 sigmas on RA Dec plx pmRA pmDec
			'=',	// Edit the name of the field(s) in front of proper motions etc
			'c',	// Edit the correlations
	};
	static final public String string_options = new String(editing_options);
	static final protected int[] Editing_options = {
			Editing.SEXA3, 		// 's'
			Editing.DECIMAL,	// 'd'
			Editing.SEXA3c,		// ':'
			Editing.SEXA3h,		// 'u'
			EDIT_FRAME,		// 'f'
			EDIT_FULL,		// 'F'
			EDIT_2NUMBERS,		// '2'
			EDIT_EPOCH,		// 'E'
			EDIT_MEAN_EPOCH,	// 'M'
			EDIT_5SIGMAS,		// '5'
			EDIT_NAMING,		// '='
			EDIT_CORRELATIONS,	// 'C'
	};

	/** 
	 * Explain a flag.
	 * @param flags Composite flag to explain
	 * @param explication Array of strings with the corresponding explanations:
	 *        element #i corresponds to mask (2<sup>i</sup>=1&lt;&lt;i) in flags.
	 * @return A printable variant of format.
	 **/
	static public final String explain(int flags, String[] explication) {
		if (flags == 0) return("");
		int flag = flags;
		boolean need_comma=false;
		StringBuilder buf = new StringBuilder();
		for (int i=0; (flag!=0)&&(i<explication.length); i++, flag >>>= 1) {
			if((flag&1) == 0) continue;
			if(need_comma) buf.append(',');
			need_comma = true;
			if(i>=explication.length) break;
			buf.append(explication[i]);
		}
		if(flag!=0) buf.append("+0x" + Integer.toHexString(flag<<explication.length) + "??");
		return(buf.toString());
	}

	/** 
	 * Explain the editing flag
	 * @param flags Composite flag to explain, e.g. anAstrocoo.editing
	 * @return A explicit string
	 **/
	static public final String explainEditing(int flags) {
		if (flags == 0) return("");
		String xplain = Astroformat.explain(flags);
		int flag = flags>>>8;
		if(flag==0) return(xplain);
		String more = explain(flag, explain_edition);
		if(xplain.length() == 0) return(more);
		return(xplain + ',' + more);
	}

	/** Compute coordinates from x,y,z.
	 * Supersedes the "Coo" method, because the FK4 RA/Dec contain
	 * the RA/Dec E-term aberration.
	 */
	protected void computeLonLat() {
		if(this.frame instanceof FK4) {
			FK4 f = (FK4)this.frame;
			double x=this.x; double y=this.y; double z=this.z; 
			super.add(f.ev_eq);
			super.computeLonLat();
			this.x=x; this.y=y; this.z=z;
		}
		else super.computeLonLat();
	}

	//  ===========================================================
	//			Constructors
	//  ===========================================================

	/**
	 * Create the default ICRS Astrocoo
	 */
	public Astrocoo() {
		this(Astroframe.create("ICRS(2000)")) ;
		Astroframe f = Astroframe.create("ICRS(2000)");
		if(!f.isFixed()) f.fixFrame();
	}

	/**
	 * Create an Astrocoo object: specify just frame, equinox, precision.
	 * Actual positions are normally specified by the <B>set</B> method.
	 * @param frame	one of the possible Astroframes
	 */
	public Astrocoo(Astroframe frame) {
		this.frame = frame;
		this.frame.usage += 1;
		epoch = 0./0.;	// Unspecified epoch
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
	this(frame, coo, frame.epoch);
    }
	 */

	/** (not necessary)
	 * Create an Astrocoo object from an existing one.
	 * It is equivalent to (<em>coo</em>.clone()).convertTo(<em>frame</em>)
	 * @param frame	one of the possible Astroframes
	 * @param coo	the position in some other frame.
     public Astrocoo(Astroframe frame, Astrocoo coo) {
       	this.frame = frame;
	editing = (short)(frame.ed_lon|EDIT_FRAME|EDIT_EPOCH);
	convert(this, coo);
    }
	 */

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
		this.frame.usage += 1;
		setEditing(); // (V1.81)editing = (frame.ed_lon|EDIT_FRAME|EDIT_EPOCH);
		this.epoch = epoch;
		dlon = dlat = precision = 0; 	// Precision unknown
		super.set(lon, lat);
		if(this.frame instanceof FK4) { // FK4: must remove the Eterm!
			FK4 f = (FK4)this.frame;
			super.sub(f.ev_eq);
		}
	}

	/**
	 * Create an Astrocoo object from a position (Epoch)
	 * @param frame	one of the possible Astroframes
	 * @param text Text with position, possibly followed by an epoch.
	 * 		Valid texts are e.g.
	 * 		"12 34 12.45 -42 12 76.4 J1991.25"    or
	 * 		"12 34 12.45 -42 12 76.4 (J1991.25)"
	 * @throws  ParseException for non-conforming data; note that a missing
	 *          epoch does not generate an exception.
	 */
	public Astrocoo(Astroframe frame, String text) throws ParseException {
		this.frame = frame;
		this.frame.usage += 1;
		this.set(text);
	}

	/**
	 * Create an Astrocoo object from just a string.
	 * @param text Text with frame, position, possibly followed by an epoch.
	 * 		Valid texts are e.g.
	 * 		"J2000: 12 34 12.45 -42 12 76.4 J1991.25"    or
	 * 		"(ICRS) 12 34 12.45 -42 12 76.4 (J1991.25)"
	 * 		"J123456.12+781245.1" (IAU-type)
	 * @throws  ParseException for non-conforming data; note that a missing
	 *          frame does generate an exception, but not a missing epoch.
	 */
	public Astrocoo(String text) throws ParseException {
		Parsing txt = new Parsing(text);
		// Start letter then digit => could be IAU designation
		char c = txt.currentChar(); txt.advance(1);
		if(Character.isLetter(c) && Character.isDigit(txt.currentChar())) {
			frame = Astroframe.create(c);
			if(frame != null) {
				editing = (frame.ed_lon|EDIT_FRAME|EDIT_EPOCH); // V1.81
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
			txt.set(0);
		}
		this.frame = Astroframe.parse(txt);
		if(DEBUG) System.out.println("#---Astrocoo(" + text + ") => frame=" 
				+ this.frame + ": usage=" + this.frame.usage);
		if (frame == null) throw new ParseException
		("[Astrocoo] argument '" + text + "' (no frame)", txt.pos);
		editing = (frame.ed_lon|EDIT_DEFAULT); // V1.81 (2 numbers)
		this.set(txt.toString());
	}

	public Object clone() {
		Astrocoo c = (Astrocoo) super.clone();
		// We have a reference on the same Frame (without copying it).
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
	 * @return    the epoch in Jyr, NaN if not found.
	 *          When NaN returned, the argument was not changed.
	 **/
	public static double getEpoch(Parsing txt) {
		int posini = txt.pos;
		double epoch = 0./0.;
		txt.gobbleSpaces();
		boolean bracketed = txt.match('(');
		if(Character.isLetter(txt.currentChar())) ;
		else if(!bracketed) {
			txt.set(posini);
			return(epoch);
		}
		Astrotime t = new Astrotime();
		if (t.parse(txt)) {
			epoch = t.getJyr();
			if(bracketed) {		// Must match the )
				txt.gobbleSpaces();
				if(!txt.match(')')) epoch = 0./0.;
			}
		}
		if (Double.isNaN(epoch)) 	// Epoch not matched, ignore...
			txt.set(posini);
		return(epoch);
	}

	/**
	 * Interpret the string and convert to Coo + Epoch.
	 * Called from set and parse routines.
	 * The frame must be defined!
	 * @param txt to interpret as a Parsing object "12 34.5 -72 54 J1991.25"
	 * @param IAUform  "true" if txt contains an IAU-name, like "B12345-1234" 
	 * @return true if OK (2 coordinates found, and possibly an epoch).
	 * 	If false returned, current position in txt is unchanged.
	 **/
	private boolean parse2(Parsing txt, boolean IAUform) {
		int posini = txt.pos;
		if(IAUform) {
			if(DEBUG) System.out.println("#...Astrocoo.parse2(IAUform=true) <" + txt + ">, frame=" + this.frame);
			if(frame.equals(txt.currentChar())) txt.advance(1);
			else return(false);
		}
		boolean f15 = frame.hms;
		double lon, lat;
		byte dlon, dlat;
		set(); 	// Init no position (in Coo)
		dlon = dlat = 0; 
		this.epoch = 0./0.;	// Unspecified epoch

		// Extract the longitude or RA
		if (DEBUG) System.out.println("#...Astrocoo.parse2(IAUform=" + IAUform + "<" + txt  
				+ ">), frame=" + frame 
				+ ", edit=(0x" + Integer.toHexString(frame.ed_lon) + ",0x" 
				+ Integer.toHexString(frame.ed_lat) + ")");
		if (IAUform && f15) {   // In IAU-format, special parsing
			//System.out.print("parse2("+txt+"): lon=");
			lon = txt.parseIAU();
			if(DEBUG) System.out.println("#...Astrocoo.parse2[IAU]: lon=" + lon);
		}
		else {
			txt.gobbleSpaces();
			lon = txt.parseSexa2();	// First part of 2 numbers
		}
		//System.out.println("parse2("+txt+"): lon=" + lon);
		int prec = 1+txt.decimals();	// Precision = Number of specified decimals+1
		if (txt.inError() || (prec<1)) { 
			if(DEBUG) System.out.println(" prec(lon)=" + prec + " **false**");
			txt.set(posini); 
			txt.setError("[Astrocoo.parse] missing RA/longitude");
			return(false); 
		}
		// For an IAU designation, no blank allowed, longitude must be followed by latitude sign
		if(IAUform&&(!Parsing.isSign(txt.currentChar()))) {
			txt.setError("[Astrocoo.parse] missing sign in IAU name");
			return(false);
		}
		boolean sexa = txt.isSexa();
		formRA = (byte)txt.format();	// Tells how RA/Longitude was coded.
		// Modify time to angle if necessary, e.g. "12h38m-27&deg;23'
		if (!IAUform) {
			if(txt.isTime()) f15 = true;
			else if(txt.isAngle()) f15 = false;
			else if(sexa) ;	// Stick to default hms
			else if(prec>0)	// Decimal values are always degrees.
				f15 = false;
		}
		if(f15) { lon *= 15.; if (prec>0) prec--; }
		if(prec>15) prec=15;	// larger precision is meaningless
		dlon = (byte)prec;
		if(DEBUG) System.out.print(" dlon=" + dlon);
		// Extract the Latitude or Dec
		if (IAUform && f15) {
			lat = txt.parseIAU();
			if(DEBUG) System.out.println("#...Astrocoo.parse2[IAU]: lat=" + lat);
		}
		else {
			txt.gobbleSpaces();
			lat = txt.parseSexa();
		}
		//System.out.println("parse2("+txt+"): lat=" + lat);
		prec = (1+txt.decimals());
		if(prec>15) prec=15;	// larger precision is meaningless
		dlat = (byte)prec;
		if(DEBUG) System.out.print(" dlat=" + dlat);
		if (txt.inError() || (prec<1)) { 
			if(DEBUG) System.out.println(" **false**");
			txt.setError("[Astrocoo.parse] missing Dec/latitude");
			txt.set(posini); 
			return(false); 
		}
		sexa |= txt.isSexa();
		/*+++  Don't touch at the Editing options !
	// Set the editing options.
 	editing = (editing&(~0xff)) | formRA; 	// The RA way of edition.
	if (sexa && !Astroformat.isSexa(editing)) 
	    editing = Editing.SEXA3c;
	---*/
		// Complete the coordinate
		this.set(lon, lat);     // V2.0: E-term removed in set(lon, lat)
		this.dlon = dlon; this.dlat = dlat;
		// Precision is the highest number of decimals
		// The actual precision should be the lowest, but...
		this.precision = dlon>dlat ? dlon : dlat;
		// ... nothing more in case of IAU positions.
		if(DEBUG) System.out.println("; lon=" + lon + " lat=" + lat + " (ok), IAUform=" + IAUform);
		// if(IAUform) return(true); Accept also an epoch for IAU positions

		// Position may be followed by an epoch -- the epoch may be within ()
		int pos = txt.pos;
		txt.gobbleSpaces();
		if (txt.pos < txt.length) this.epoch = getEpoch(txt);
		if (DEBUG) System.out.println("#...Astrocoo.parse2: epoch=" + this.epoch);
		return(true);
	}

	/**
	 * Interpret the string and convert to Coo + Epoch.
	 * Called from set and parse routines.
	 * @param txt to interpret as a Parsing object "12 34.5 -72 54 J1991.25"
	 * @return true if OK (2 coordinates found, and possibly an epoch).
	 * 	If false, no mouvement in txt.
	 **/
	public boolean parse(Parsing txt) {
		return(parse2(txt, false));
	}

	/**
        switch(txt.currentChar()) {
          case 'B': 
            if(frame instanceof FK4) break;
            return(null);
          case 'J':
            if(frame instanceof FK5) break;
            else break;
          case 'I':
            if(frame instanceof ICRS) break;
            return(null);
          default:
            return(null)<F9>;
        }
	 */

	/**
	 * Interpret the string representing a position in IAU-style.
	 * @param txt to interpret as a Parsing IAU name, e.g. "B1234.5-7254"
	 * @return true if OK (2 coordinates found, corresponding to frame)
	 * 	If false, no mouvement in txt.
	 **/
	public boolean parseIAU(Parsing txt) {
		if(DEBUG) System.out.println("#...Astrocoo.parseIAU <" + txt + ">, frame=" + (frame==null?"null":frame.name));
		if(frame==null) frame = Astroframe.create(txt.currentChar());
		boolean status = parse2(txt, true);
		if(DEBUG) System.out.println("#...Astrocoo.parseIAU: status=" + status + "<" + txt + ">");
		return(status);
	}

	/**
	 * Parsing method: interpret a String.
	 * @param txt to interpret.
	 * @param offset starting position in text to explore
	 * @return (end+1) position of explored text, for further parsing.
	 **/
	public int parse(String txt, int offset) {
		Parsing t = new Parsing(txt, offset);
		if (parsing(t)) return(t.pos);
		return(offset);
	}

	//  ===========================================================
	//			Set in Astrocoo
	//  ===========================================================

	/**
	 * Inheritance from super class (Coo): 
	 *     set(lon, lat) *** NO *** due to E-term in FK4
	 *     set(x, y, z)  === ok, E-term not included in (x,y,z)
	 *     set(Coo)      *** BEWARE, RA/Dec not correct!
	 *     set()         === ok
	 **/

	/**
	 * Set a position from its longitude and latitude (RA/Dec).
	 * Convert (lon,lat) into its direction cosines (x,y,z);
	 * replaces the Coo method because of FK4 where E-terms mut be removed.
	 * Note that the epoch is NOT removed.
	 * @param lon longitude in degrees
	 * @param lat latitude angle in degrees
	 */
	public void set(double lon, double lat) {
		//System.out.print("[Astrocoo.set]: dlon=" + dlon + ", dlat=" + dlat);
		super.set(lon, lat);
		this.frame.usage += 1;
		//System.out.println("#...Astrocoo.set(" + lon + ", " + lat + "); dlon=" + dlon + ", dlat=" + dlat);
		if(this.frame instanceof FK4) { // FK4: must remove the Eterm!
			FK4 f = (FK4)this.frame;
			super.sub(f.ev_eq);
			lon = lat = 0.0/0.0;
		}
	}

	/**
	 * Set a position from a Coo object.
	 * replaces the Coo method because of FK4 where E-terms mut be removed.
	 * @param coo the coordinates (cartesian components are assumed ok)
	 */
	public void set(Coo coo) {
		x = coo.x; y = coo.y; z = coo.z; lon = lat = 0.0/0.0;
		this.frame.usage += 1;
		// E-term not included in (x,y,z)
	}

	/**
	 * Set a position from an existing one (copy).
	 * @param coo  the Astrocoo to copy.
    public void set(Astrocoo coo) {
	this.frame = coo.frame;
	this.epoch = coo.epoch;
	this.dlon  = coo.dlon;
	this.dlat  = coo.dlat;
	this.precision = coo.precision;
	this.set((Coo)coo);
        this.frame.usage += 1;
    }
	 **/

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
		if (parse(t))		// Various items found.
			t.gobbleSpaces();
		this.frame.usage += 1;
		if (t.pos != t.length) {
			String msg = "[Astrocoo.set, frame=" + frame + "] argument '" + text + "'+" + t.pos;
			if(t.error_message != null) msg = msg + ": " + t.error_message;
			throw new ParseException(msg, t.pos);
		}
	}

	/**
	 * Set a position from a text -- accept IAU-format (HHMM...+DDMM...).
	 * The frame is <b>not</b> modified.
	 * @param text  HHMM..+DDMM.. or classical sexagesimal/decimal coordinates.
	 * @throws  ParseException when the argument text is neither an IAU string,
	 * 			nor a position.
	 **/
	public void setIAU(String text) throws ParseException {
		Parsing t = new Parsing(text);
		char c = t.currentChar();
		if(frame.equals(t.currentChar())) t.advance(1);
		else throw new ParseException
		("[Astrocoo.setIAU, frame=" + frame + "] '" + text + "' (incompatible frames)", t.pos);
		if (DEBUG) System.out.println("#...setIAU(" + text +"), editing=0x"
				+ Integer.toHexString(editing));
		if (parse2(t, true))		// Various items found.
			t.gobbleSpaces();
		// Accept standard string 
		else if (parse(t))
			t.gobbleSpaces();
		if (t.pos != t.length) {
			String msg = "[Astrocoo.setIAU, frame=" + frame +  "] '" + text  + "'+" + t.pos;
			if(t.error_message!=null) msg = msg + ": " + t.error_message;
			throw new ParseException(msg, t.pos);
		}
	}

	/**
	 * Set position + epoch.
	 * The precision is not changed.
	 * @param coo   the lon+lat
	 * @param epoch the epoch.
	 **/
	public void set(Coo coo, double epoch) {
		this.epoch = epoch;
		this.set(coo);
		this.frame.usage += 1;
	}

	/**
	 * Change the precision of the data
	 * @param  precision integer number, typically one of the values NONE (0),
	 * 	DEG (1), ARCMIN (3), ARCSEC (5),  MAS (8);
	 * 	use ARCSEC+1 for 0.1arcsec, MAS-1 for 10mas, etc...
	 * 	Should be limited to 14 = 1nas, larger values are not meaningful.
	 **/
	public void setPrecision(int precision) {
		if((precision<0)||(precision>15)) precision=15;
		this.precision = (byte)precision;
	}

	/**
	 * Change the precision of the data
	 * @param  dlon Precision number on longitude (RA).
	 * @param  dlat Precision number on latitude (Dec).
	 **/
	public void setPrecision(int dlon, int dlat) {
		if((dlon<0)||(dlon>15)) dlon=15;
		if((dlat<0)||(dlat>15)) dlat=15;
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
		this.editing = edit_option;
		if ((edit_option&EDIT_FULL)!=0) 	// Full precision ~ 1nas (nano-arcsec)
			setPrecision(15);
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
	 * Get the Longitude (RA) in degrees.
	 * The E-term is removed when necessary (FK4)
	 * @return   the longitude (RA) in degrees
	 **/
	public double getLon() {
		if (Double.isNaN(lat)) computeLonLat();
		return(lon);
	}

	/**
	 * Get the Latitude (Dec) in degrees.
	 * The E-term is removed when necessary (FK4)
	 * @return   the latitude (Dec) in degrees
	 **/
	public double getLat() {
		if (Double.isNaN(lat)) computeLonLat();
		return(lat);
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
	 * @param title A title to precede the dump.
	 *        When the title contains <code>(R)</code>, dump is recursive.
	 **/
	public void dump(String title) {
		boolean recursive = title.indexOf("(R)")>=0;
		System.out.print("#...Astrocoo.dump (" + this.getClass() + ") " + title);
		if(!title.endsWith("\n")) System.out.print("\n");
		String indent = title.charAt(0) == '#' ? "#   " : "    ";
		super.dump(indent);
		System.out.println(indent + "epoch=" + epoch + "; Astroframe=" + this.frame);
		if(recursive) frame.dump(indent + "    ");
		System.out.println(indent + "precision=" + precision + "; editing=0x" + Integer.toHexString(editing) + '(' + explainEditing(editing) + ')');
		System.out.println(indent + "inputprec: dlon=" + dlon + ", dlat=" + dlat + ", formRA=0x" + Integer.toHexString(formRA)
		+ '(' + Astroformat.explain(formRA) + ')');
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
	 *    5 = edit the 5 sigmas on RA Dec plx pmRA pmDec rather than error ellipses
	 *    = = edit the name of the field(s) in front of proper motions etc
	 *	* = default edition options
	 *</PRE>
	 * @return the option as an int.
	 **/
	public static int editingOptions(String text) 
			throws IllegalArgumentException
	{
		char[] b = text.toCharArray();
		int i, j, n; int o = 0;
		for (i=0; i<b.length; i++) {
			if (b[i] == '*') { o |= EDIT_DEFAULT; continue; }
			for (j=0; j<editing_options.length; j++) {
				if (b[i] == editing_options[j]) break;
			}
			if (j==editing_options.length) throw new IllegalArgumentException
			("[Astrocoo.editingOptions] '" + text + "'; accepted=" + string_options);
			if (Editing_options[j] < 0x10) o &= ~0xf;
			o |= Editing_options[j];
		}
		return(o);
	}

	/**
	 * Function to edit one number representing an epoch in Jyr
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
		// Edit number Up to 4 decimals (changed 3->4 in V2.0, or ~1h)
		ed.editDecimal(buf, ep, 4, -4, Editing.ZERO_FILL);
		return(buf);
	}

	/**
	 * Method to edit the Coordinates in a StringBuffer
	 * @param buf  Buffer where the result is appended
	 * @param opt  A mixture of the options ED_COLON, ED_DECIMAL,
	 *			EDIT_FULL, EDIT_SEXA, EDIT_FRAME, EDIT_2NUMBERS
	 * @return	the StringBuffer
	 **/

	public final StringBuffer edit(StringBuffer buf, int opt) {
		boolean f15 = false;
		int prec = precision&15;
		if (prec == 0) 	// Precision unknown: up to precision of frame.
			prec = frame.precision;
		/*-- Should the position be marked as having too many digits?
      else if (prec > frame.precision) 	// Limit to intrinsic precision
	  prec = frame.precision;
      --*/
		int o = opt&(~(EDIT_FULL|EDIT_FRAME));
		if (DEBUG) System.out.println(
				"#...Astrocoo.edit(opt=0x" + Integer.toHexString(opt) 
				+ "), epoch=" + epoch 
				+ ", precision=" + precision
				+ ", dlon=" + dlon + ", dlat=" + dlat 
				+ ", lon=" + lon + ", lat=" + lat
				+ " x=" + x + " y=" + y + " z=" + z);
		if ((opt&EDIT_FULL) != 0) 	// Edit full precision
			prec=15;
		//System.out.println("#...Astrocoo.edit(prec=" + prec + ")");
		// Edit the Frame
		if ((opt&EDIT_FRAME) != 0) {
			buf.append(frame.toString());
			buf.append(' ');
		}
		// Edit the RA or Longitude
		double x = this.getLon();
		int nint = 3;
		int nd = prec;
		// Must it be expressed in time ?
		if (Astroformat.isSexa(o))       f15 = frame.hms;
		/*--- old code:
	if (Astroformat.isTime(o))       f15 = true;
	else if (Astroformat.isAngle(o)) f15 = false;
	else if (Astroformat.isSexa(o))  f15 = frame.hms;
	--- */
		if (f15) {
			nint--; x /= 15.0;
			if (nd>0) { 	// v1.4a: Sexagesimal at least h min 
				nd++; 
				if (nd<3) nd=3;
			}
			else {
				nd--;
				if (nd>-3) nd=-3;
			}
		}
		// editDecimal does edit in Sexagesimal if this option is set.
		ed.editDecimal(buf, x, nint, nd-1, Editing.ZERO_FILL|o);
		// Separate the numbers if wished
		if ((opt&EDIT_2NUMBERS) != 0) buf.append(' ');
		// Edit the Dec or Latitude
		if (Astroformat.isTime(o)) {
			if (Astroformat.isSexa(o)) o -= 2;	// Replace h m s by d m s
			else o++;
		}
		ed.editDecimal(buf, this.getLat(), 3, prec-1,
				Editing.ZERO_FILL| Editing.SIGN_EDIT|o);

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
	 * Edition with IAU-style (TRUNCATED position).
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
			if (frame == null) frame = Astroframe.create("J2000");
		}
		else if (letter == 'B') {
			if (this.frame instanceof FK4) {
				FK4 f = (FK4)this.frame;
				if (f.equinox == 1950) frame = this.frame;
			}
			if (frame == null) frame = FK4.create();
		}
		else if (letter == 'G') {
			if (this.frame instanceof Galactic) frame = this.frame;
			if (frame == null) frame = Galactic.create();
		}
		else throw new ParseException(
				"[Astrocoo.toIAU] template does not start with J|B|G: " + IAUtemplate, 0);

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
					"[Astrocoo.toIAU] '" + IAUtemplate + "': " + letter, i);
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
					"[Astrocoo.toIAU] '" + IAUtemplate + "': " + b[i], i);
			int j = bed.length();
			if (DEBUG) System.out.print("# Edition(nint=" + nint + ",nd=" 
					+ nd + ",opt=0x" + Integer.toHexString(opt) + "): " + bed); 
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
				"[Astrocoo.toIAU] '" + IAUtemplate + "': " + b[i], i);
		return(bed.toString());
	}

	//  ===========================================================
	//			Convert Coordinates
	//  ===========================================================

	/**
	 * Transform the position into another frame.
	 * @param new_frame	The frame of the resulting position.
	 * @return true if ok
	 **/
	public boolean convertTo(Astroframe new_frame) {
		// Verify first if frames identical -- then nothing to do ! 
		// ... but epochs may differ, therefore checking names is enough.
		if((x==0) && (y==0) && (z==0))
			return(false);
		if (this.frame.name.equals(new_frame.name)) {
			if(DEBUG) System.out.println("#...Frame " + this.frame 
					+ "=" + new_frame);
			return true;
		}
		// Move via ICRS
		if(DEBUG) System.out.println("#...Astrocoo.convert: via ICRS:  "
				+ this.frame + " => ICRS => " + new_frame);
		this.frame.toICRS((Coo)this);	// Position now in ICRS
		if(DEBUG) this.dump("#ICRS: ");
		new_frame.fromICRS((Coo)this);	// Position now in new_frame
		this.frame = new_frame;
		if(DEBUG) this.dump("#NewF: ");
		this.lon = this.lat = 0./0.;	// Actual angles not recomputed
		return(true);
	}

	/**
	 * Transform the celestial position.
	 * @param source	Source position (with its frame)
	 * @param target	Target position (coordinates part replaced).
	 * The {@link #precision} of the target position is not modified.
	 * @return true if ok
	 **/
	public static boolean convert(Astrocoo source, Astrocoo target) {
		if((source.x==0) && (source.y==0) && (source.z==0))
			return(false);
		Coo coo = (Coo)target;
		coo.set((Coo)source);		// Sets x y z 
		if(DEBUG) target.dump("#source:");
		source.frame.toICRS(coo);	// now in ICRS
		target.frame.fromICRS(coo);
		// Copy the other parameters:
		target.epoch = source.epoch;
		target.dlon  = source.dlon;
		target.dlat  = source.dlat;
		//target.formRA = source.formRA;
		//target.precision = source.precision;
		if(DEBUG) target.dump("#target:");
		target.lon = target.lat = 0./0.;
		return(true);
	}


	/**
	 * Testing the functions of Astroframe.
	 * or of a matrix (3x3, 2x3x3 or 6x6).
	 * @param   level   a depth level 
	 * @param   verbose a verbosity level, number with 0 = less verbose, 1 = more verbose, ...
	 * @return  true if ok.
	 */
	public static boolean test(int verbose, int level) {
		System.out.print("#===Astrocoo.test: verbosity=" + verbose
				+ ", level=" + level + "; ε=" + eps);
		// Tests convert from IAU name
		Astrocoo IAUpos;
		System.out.println("#===Get from IAU name:");
		try { IAUpos=new Astrocoo("B1234567-123456"); IAUpos.dump("B1234567-123456"); }
		catch(Exception e) { System.err.println("#***" + e); }
		// Barycentric position from Gaia-DR2, J2015.5
		ICRS Gaia2 = new ICRS(2015.5);
		Astrocoo V1234_Cyg = new Astrocoo(Gaia2, 318.20647269273, 41.39116740897);
		Astrocoo Q3C273    = new Astrocoo(Gaia2, 187.27791587249,  2.05238861030);
		Q3C273.setEditing("fEMF");
		V1234_Cyg.setEditing("fEMF");
		boolean ok=true;

		// Compute angles in Galactic frame
		Astrocoo ngp = new Astrocoo(Astroframe.create('G'), 0, 90); ngp.convertTo(Astroframe.create('I'));
		Astrocoo  gc = new Astrocoo(Astroframe.create('G'), 0, 00);  gc.convertTo(Astroframe.create('I'));
		System.out.println("#---Position of Galactic North Pole in ICRS: " + ngp.toString("F") 
		+ "; galactic centre: " + gc.toString("F"));

		// Testing the matrices for changing frames
		Astroframe[] frame = { 
				Astroframe.create("ICRS"), 
				Astroframe.create("ICRS(2015.5)"), 
				Astroframe.create("J2000"), 
				Astroframe.create("FK5(B1950)"), 
				Astroframe.create("B1950"), 
				Astroframe.create("B1875"), 
				Astroframe.create("Gal(B1950)"),
				Astroframe.create("SGal(B1950)"),
				Astroframe.create("Ecl(2015.5") };
		double[] M1, M2;
		int nf = frame.length;
		Astrocoo[] mycoo = new Astrocoo[nf];
		for(int i=0; i<nf; i++) mycoo[i] = new Astrocoo(frame[i]);
		if(verbose>0) for(int i=0; i<nf; i++) {
			mycoo[i].setEditing("fEMF");
			System.out.println("\n#...Frame#" + i + ": " + frame[i]);
			AstroMath.printMatrix("#.....toICRSmatrix:\n", frame[i].toICRSmatrix);
			AstroMath.printMatrix("#...fromICRSmatrix:\n", frame[i].fromICRSmatrix);
			double[] product = AstroMath.m36p(frame[i].fromICRSmatrix, frame[i].toICRSmatrix);
			AstroMath.checkUnity("#.....Product=.....\n", product);
			convert(Q3C273,    mycoo[i]); System.out.println("#====3C273 " + mycoo[i]);
			convert(V1234_Cyg, mycoo[i]); System.out.println("#=V1234Cyg " + mycoo[i]);
		}
		System.out.println("\n#---Example of 3C273: " + Q3C273);
		for(int i=0; i<nf; i++) {
			convert(Q3C273,     mycoo[i]); System.out.println("#...Direct from Gaia-2: " + mycoo[i]);
			if(i==0) continue;
			convert(mycoo[i-1], mycoo[i]); System.out.println("#...from preceding....: " + mycoo[i]);
		}
		// Astromath.checkArray
		System.out.println("\n#---Example of V1234 Cyg: " + V1234_Cyg);
		for(int i=0; i<nf; i++) {
			convert(V1234_Cyg,  mycoo[i]); System.out.println("#...Direct from Gaia-2: " + mycoo[i]);
			if(i==0) continue;
			convert(mycoo[i-1], mycoo[i]); System.out.println("#...from preceding....: " + mycoo[i]);
		}

		if(verbose==0) System.out.println(ok? " (ok)" : " **problem(s)**");
		else if(ok) System.out.println("#---End of tests: ok");
		else        System.out.println("#***Bad ** tests?");
		return(ok);
	}

}
