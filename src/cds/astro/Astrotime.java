package cds.astro;

/*==================================================================
                Astrotime class
 *==================================================================*/

import java.text.*;	// for parseException

/**
 * The astronomical time may be expressed in (Modified) Julian Days,
 * Julian Years or Besselian years. The conversion to Calendar can use the
 *       Calendar.setTimeInMillis(JD2Millis(date))
 * @author Francois Ochsenbein [CDS]
 * @version 1.0 : 21-Feb-2004: Imported from original Astroframe
 * @version 1.1 : 15-Aug-2004: Method getByr getJyr getJD getMJD getTime
 * @version 1.2 : 02-Sep-2006: Interpret date/time
 * @version 1.3 : 01-Aug-2011: Methods getLST
 *
 */

public class Astrotime {
    /** The actual date/epoch is stored as MJD value (JD-2400000.5) */
    protected double mjd;
    /** The original choice of the date (index in 'prefix' table) */
    protected byte unit;
    /** The original precision  = number of decimals + 1 (in specified unit);
      * it's 0 when only month is known.
     **/
    protected byte precision;
    /** The default choices of the edition for this class */
    static public Editing ed = new Editing("--");	// How to edit time
    /* Default Equinoxes in Julian Years */
    /** Value of Besselian Year in days */
    static final public double Byr = 365.242198781e0 ;	// Besselian Year
    /** Value of the Julian Year in days */
    static final public double Jyr = 365.25e0 ;		// Julian Year
    /** Julian date of J2000 epoch */
    static final public double JD_J2000 = 2451545. ;	// Julian Date of J2000
    /** Julian date of B1900 epoch */
    static final public double JD_B1900 = 2415020.31352;// Julian Date of B1900
    /** Julian date of 1 Jan 1970  */
    static final public double JD_1970 = 2440587.5;	// Julian Date of Origin
    /** Modified Julian date */
    static final public double JD_MJD0 = 2400000.5;	// Origin of MJD
    /** Conversion of time: time in unit = mjd*factor + offset */
    static final public String[] prefix = {
	"\"date\"", "MJD", "JD", "J", "B", "ms"
    };
    static final private double[] factor = {
	1., 1., 1., 1./Jyr, 1./Byr, 86400.e3
    };
    static final private String[] is_now = { "now", "." }; // Acceptable...
    /** Origin of the different time scales from MJD0 (1858-11-17T00:00:00) */
    static final public double[] offset = {
	0., 0., JD_MJD0, 
	2000.-((JD_J2000-JD_MJD0)/Jyr), 
	1900.-((JD_B1900-JD_MJD0)/Byr), 
	-86400.e3*(JD_1970-JD_MJD0)
    };
    static final private byte[] ilen = {  // Default length of integer part
	10, 5, 7, 4, 4, 12 
    };
    static final private double[] C_teq = { // Constants time equation
       24110.54841, 8640184.812866, 0.093104, -0.0000062
    };

  /*  Static methods (functions) in Java are very close to C ones;
      they do not require any object instanciation.
      Typical example of static methods are in the Math class
  */

   /* Conversions of Times between B(esselian Year) / J(ulian year) / JD  */
   /**
    * Conversion of a Julian epoch to a Julian Date
    * @param y Julian epoch
    * @return Julian Date
   */
   public static final double J2JD(double y) {
      return JD_J2000 + (y-2000)*Jyr ;
   }
   /**
    *  Conversion of a Besselian epoch to a Julian Date
    * @param y Besselian epoch
    * @return Julian date
    */
   public static final double B2JD(double y) {
      return JD_B1900 + (y-1900)*Byr ;
   }

   /**
    * Conversion of a calendar date into a Julian Date.
    * Note that if month=0, day may represent the day-in-year.
    * When month is outside range 0..11, it's assumed to be January.
    * @param year  Year number
    * @param month  Month number, range 0=January to 11=December
    * @param day  Day number, range 1 to 31
    * @return Julian Date
    */
   public static final double YMD2JD(int year, int month, int day) {
     int i, jdi;
     double jd;
	/* Convert to a Julian Date */
	if (year <= -4712) {
	    i = 1 + (year + 4712)/400;
	    jdi = -i * 146097;		// 146097 days = 400 yrs
	    i = year + 400*i;
	}
	else { jdi = 0; i = year; }
	if (day <= 0) day = 1;		// In case just year/month
	if (month<0 || month>11) month = 0;
	// if ((status&2)!=0) {		// Month is present
	    i  -= (11 - month)/10;
	    jdi += (1461* ( i + 4712))/4 + (306 * ((month+10)%12) + 5)/10
		- (3* ((i + 4900)/100))/4 + day + 96 ;
	// }
	/* --- No problem if month not specified -- it's January...
	else {	// Month not specified -- use 400-yr = 146097 days cycle 
	    i = year % 400 ; year /= 400 ;
	    if (i < 0) { i += 400 ; year-- ; }
	    jdi = 365*i + (i+3)/4 - (i-1)/100 ;
	    jdi += 1721059 + (year * 146097) ;
	}
        ---------------------------------------- */
	jd = jdi;
	return(jd+0.5);
   }

   /**
    * Conversion of a Julian Date into a calendar year/month/day.
    * When month is outside range 0..11, it's assumed to be January.
    * @param jd    Julian date
    * @param ymd   array on return with year month day.
    * 		year=ymd[0], month=ymd[1](range 0..1), day=ymd[2](range 1..31).
    * 		Day in year (range 1..366) is given as 
    * 		ymd[1] if ymd has 2 elements, ymd[3] if 4 elements.
    */
   public static final void JD2YMD (double jd, int[] ymd) {
     int j, n4, nd10, year;
        j = (int)(jd+0.5);
	year = -4712;
	while (j <= 4480)   { j += 146097; year -= 400;}
	n4 = 4*(j+((2*((4*j-17918)/146097)*3)/4+1)/2-37);
	nd10=10*( ((n4-237)%1461)/4)+5;

	ymd[0] = year + n4/1461;
	if (ymd.length>1) {
	    ymd[1] = (nd10/306+2)%12;
	    ymd[2] = (nd10%306)/10+1;
	}

	/* Compute mday */
	if (ymd.length!=3) {
	    n4 = (j+32104)%146097;	// Day in 400-yr cycle, #0=01-Jan-2000
	    if (n4 >= 36525)		// Century#0 is 25 leap years
		n4 += (n4-366)/36524;
	    n4 %= 1461;			// Day in 4-yr cycle
	    if (n4 >= 731) n4 += (n4-366)/365;
	    j = 1 + (n4%366);		// Day-in-year number
	    if (ymd.length == 2) 
		 ymd[1] = j;
	    else ymd[3] = j;
	}
   }

   /**
    * Conversion of a calendar time expressed in Milliseconds to a Julian Date
    * @param ms  UTC millisconds got e.g. via Calendar.getTimeInMillis()
    * @return Julian Date
    */
   public static final double ms2JD(long ms) {
      return JD_1970 + ((double)ms/86400.e3);
   }

   /**
    * Conversion of a MJD to JD
    * @param mjd Modified Julian Date
    * @return Milliseconds e.g. to set date via Calendar.setTimeInMillis()
    * @return corresponding Julian Date
    */
   public static final double MJD2JD(double mjd) {
      return JD_MJD0 + mjd;
   }

   /**
    * Conversion of a Julian Date to a Julian  epoch
    * @param jd Julian Date
    * @return Julian epoch
    */
   public static final double JD2J(double jd) {
      return 2000 + (jd-JD_J2000)/Jyr ;
   }

   /**
    * Conversion of a Julian Date to a Besselian epoch
    * @param jd Julian Date
    * @return Besselian epoch
    */
   public static final double JD2B(double jd) {
      return 1900 + (jd-JD_B1900)/Byr ;
   }

   /**
    * Conversion of a Julian Date to a Milliseconds since 1 January 1970
    * @param jd Julian Date
    * @return Milliseconds e.g. to set date via Calendar.setTimeInMillis()
    */
   public static final long JD2ms(double jd) {
      return (long)((jd-JD_1970)*86400e3);
   }

   /**
    * Conversion of a JD to MJD
    * @param jd Julian Date
    * @return Modified Julian Date = JD - 2400000.5
    */
   public static final double JD2MJD(double jd) {
      return jd - JD_MJD0;
   }

   /**
    * Conversion of a Besselian epoch to a Julian  epoch
    * @param y Besselian epoch
    * @return Julian epoch
    */
   public static final double B2J(double y) {
      return JD2J(B2JD(y)) ;
   }

   /**
    * Conversion of a Julian epoch to a Besselian epoch
    * @param y Julian epoch
    * @return Besselian epoch
    */
   public static final double J2B(double y) {
      return JD2B(J2JD(y)) ;
   }

  //  ===========================================================
  //                    Constructors
  //  ===========================================================

   /**
    * Default contructor (undefined time)
    */
    public Astrotime () {
	mjd = 0./0.;
	unit = 0;
	precision = 8;
    }

   /**
    * Constructor from a String.
    * @param text Sting expression of an Astrotime.
    * 	Valid input values can be "Byyyy...", "Jyyyy...", "JDxxx" or "MJD..."
    * An empty string, a single dot (".") or the text "now" is
    * interpreted as the current date/time.
    * @throws ParseException when <em>text</em> can't be interpreted.
    */
    public Astrotime (String text) throws ParseException {
      	this.set(text);
    }

   /**
    * Dump the time contents.
    */
    public final void dump (String title) {
	System.out.println(title + " unit=" + unit + ", precision=" + precision
		+ ", mjd=" + mjd);
    }

   /**
    * Interpret a string for a time.
    * @param t	Text to interpret as a date/time object; 
    *           "", ".", "now" are interpreted as current date/time
    * @return true if something could be interpretated.
    */
    public boolean parsing (Parsing t) {
      int posini = t.pos;
      int k, pos1, prec;
      double x;
      	t.gobbleSpaces();
	prec = 0;		// Lowest precision, i.e. year/month
      	k = t.lookup(prefix); 
	pos1 = t.pos;
	if ((t.pos == t.length) || (t.lookupIgnoreCase(is_now)>=0)) {
	    set(System.currentTimeMillis());
	    return(true);
	}
	if (k<=0) {		/* V1.2 -- No prefix, try a standard date ? */
	    int pdate = t.parseDate();
	    boolean sexatime = false;
	    x = 0;		// Default time part
	    if (t.pos > pos1) {
	        k = 0;		// It's "date" unit
		int pos2 = t.pos;
		prec = t.format()&(~Astroformat.DATE_alpha);
		if (prec == Astroformat.DATE_YM || prec == Astroformat.DATE_MY)
		     prec = 0;
		else prec = 1;
		if (prec == 1) {
      	            char c = t.currentChar();
		    if (c == '.') {		// Decimal day, or time ?
			x = t.parseDecimal();
			if ((sexatime = t.match(':')))
			     t.set(pos2);
			else prec = 1+t.decimals();
		    }
		    /* ISO-8601 uses T to separate date/time */
		    else sexatime = (c == 'T') || (c == ':') || (c == ' ');
		    if (sexatime) {
			t.advance(1);
			x = t.parseSexa();
			if ((t.pos-pos2)>1) {
			    x /= 24.;
			    prec = 2+t.decimals();
			}
			else {
			    t.set(pos2);
			    x = 0;
			}
		    }
		}
		// System.out.print  ("...Astrotime.parse: prec=" + prec);
	        x += pdate;
		// System.out.println(", date=" + x);
	    }
	}
	else {
      	    x = t.parseDouble();
	    prec = 1+t.decimals();
	}
	if (t.pos == pos1) {	// No value found -- can't work
	    t.set(posini);
	    return(false);
	}
	precision = (byte)prec;
        if (k<0) k = t.lookup(prefix);	// Accept type of time as a suffix.
	if (k<0) k=3;			// Default (no prefix) == Julian year.
	// System.out.println("....k=" + k + ", offset=" + offset[k]);
	mjd = (x-offset[k])/factor[k];
	return(true);
    }

   /**
    * Interpret a string for a time. 
    * Accepts a time surrounded by brackets, or preceded by , /
    * @param text   String expression of an Astrotime
    * @param offset Where to start in the text
    * @return the number of bytes interpretated (offset if an error)
    */
    public int parse(String text, int offset) {
      Parsing t = new Parsing(text, offset);
      return( parsing(t) ? t.pos : offset );
    }

   /**
    * Set the time from MJD.
    * @param mjd Date/time in MJD (JD-2400000.5)
    */
    public void set(double mjd) {
	this.mjd = mjd;
	unit = 1;
	precision = 8;
    }

   /**
    * Set the time from milliseconds.
    * @param ms Date/Time in milliseconds.
    *            or System.currentTimeMillis()
    * 		(e.g. set(System.currentTimeMillis()) sets to current
    * 		date/time). The default edition is in JD, with 8 decimals.
    */
    public void set(long ms) {
	mjd = (JD_1970-JD_MJD0) + (double)ms/86400.e3;
	unit = 0;
	precision = 8;
    }

   /**
    * Set the time from a String.
    * Valid times are "Byyyy...", "Jyyyy...", "JDxxx" or "MJD..."
    * eventually surrounded by brackets, or preceded by , /
    * An empty string, a single dot (".") or the text "now" is
    * interpreted as the current date/time.
    * @param text Sting expression of an Astrotime
    * @throws ParseException when <em>text</em> can't be interpreted.
    */
    public void set(String text) throws ParseException {
      Parsing t = new Parsing(text);
      	if (parsing(t)) 	// Verify text completely interpretated
	    t.gobbleSpaces();
	if (t.pos != t.length) throw new ParseException
	    ("****Astrotime: argument '" + text + "'", t.pos);
    }

  //  ===========================================================
  //                    Get the time in its different forms
  //  ===========================================================

   /**
    * Get the time expressed in MJD (JD-2400000.5)
    */
    public double getMJD() {
	return(mjd);
    }

   /**
    * Get the time expressed in Julian yr
    */
    public double getJyr() {
	return(2000.+((mjd+(JD_MJD0-JD_J2000))/Jyr));
    }

   /**
    * Get the time expressed in Besselian yr
    */
    public double getByr() {
	return(1900.+((mjd+(JD_MJD0-JD_B1900))/Byr));
    }

   /**
    * Get the time expressed in JD
    */
    public double getJD() {
	return(mjd+JD_MJD0);
    }

   /**
    * Get the time expressed in milliseconds since 1 Jan 1970
    */
    public long getTime() {
      	return (long)((mjd+(JD_MJD0-JD_1970))*86400e3);
    }

   /**
    * Get the Local Sideral Time (in sideral seconds at Greenwich meridian)
    * between 0 and 86400.
    */
    public double getLST() {
	// j0h = MJD day at 0h GMT
	double j0h = Math.floor(mjd);
	// tcj = Julian centuries since 2000, at 0hGMT
	double tcj = (j0h - (JD_J2000-JD_MJD0))/36525.; 
	// tsg = mean sideral time at 0h GMT (in sideral s)
	double tsg = ((((C_teq[3]*tcj) + C_teq[2])*tcj) + C_teq[1])*tcj 
	           + C_teq[0];
	// Normalize in range 0,86400
	tsg -= 86400.*Math.floor(tsg/86400.);
	// Add the current time, converted into sideral seconds
	// (Byr+1)/Byr = 1.00273790935 adopted IAU 
	tsg += 1.00273790935*86400.*(mjd-j0h);
	// Final normalization
	if (tsg >= 86400.) tsg -= 86400.;
      	return (tsg);
    }

   /**
    * Get the Local Sideral Time at a location specified by its longitude
    * @param  lon   Longitude of place, given in <b>degrees</b> 
    *               positive at <b>East</b> of Greenwich, e.g. Strasbourg=7.75)
    * @return the local sideral time in sideral seconds between 0 and 86400.
    */
    public double getLST(double lon) {
	double tsl = getLST() + lon*(86400./360.);
	// Normalize in range 0,86400
	if (tsl<0) tsl += 86400.;
	else if (tsl>86400) tsl -= 86400.;
	return (tsl);
    }

  //  ===========================================================
  //                    Edit the time
  //  ===========================================================

   /**
    * Internal edition
    * @param i     Index in "unit"
    * @param value The number to edit
    */
    private final void edit(StringBuffer b, int i, double v) {
	if (i>0) b.append(prefix[i]);
	if (Double.isNaN(v) || (i>0)) {
	    ed.editDecimal(b, v, ilen[i], precision-1, 0);
	    return;
	}
	// System.out.println("\n....Astrotime.edit: precision=" + precision);
	// Here edit ISO-8601 DAte+Time
	int iv = (int)v;
	double dv = v-iv;	// Fraction of day
	if (dv< -1.e-11) {
	    --iv;
	    dv += 1.0;
	}
	else if (dv<0) dv = 0;
	
	ed.editDate(b, iv);	// Standard date edition.
	if (precision == 0) 	// Month only
	    b.setLength(b.length()-3);
	if (precision <= 1)	// No fraction
	    return;
	b.append('T');
	ed.editSexa(b, dv*24., 2, precision-2, 
		Editing.ZERO_FILL|Editing.SEXA3c);
    }

   /**
    * Edit the time in the specified choice.
    * @param unit One of the possibilities "JD" "MJD" "J" "B" "ms"
    */
    public String toString(String unit) {
      StringBuffer b = new StringBuffer(32);
      double v; int i;
	for (i=0; i<prefix.length; i++) {
	    if (unit.equals(prefix[i])) break;
	}
	if (i == prefix.length) i = this.unit;	// Default
	v = this.mjd*factor[i] + offset[i];
	this.edit(b, i, v);
	return(b.toString());
    }

   /**
    * Get the time expressed in milliseconds since 1 Jan 1970
    */
    public String toString() {
      double v = this.mjd*factor[this.unit] + offset[this.unit];
      StringBuffer b = new StringBuffer(32);
        this.edit(b, this.unit, v);
	return(b.toString());
    }

}
