package cds.astro ;

/*==================================================================
                Editing class (for nice editions)
 *==================================================================*/

//import java.io.*;
//import java.util.*;

/**
 * Class for 'nice' edition of numbers. 
 * This class contains options for the edition of integer / float numbers;
 * the edited value is appended to the StringBuffer generally specified
 * as the first argument. <br>
 * This class is used in the various astronomical editions.
 * @author Francois Ochsenbein [CDS]
 * @version 1.0 : 03-Mar-2004
 * @version 1.1 : 03-Mar-2005 (bug in sexagesimal edition DD.d)
 * @version 1.2 : 03-Apr-2006 (bug in Double edition between 0.1 and 1;
 * 				addition of editFlags)
 * @version 1.3 : 03-Sep-2006 Added editDate
 */

public class Editing extends Astroformat {

    /** How the NULL (NaN) is represented is defined in parent class */
    // public String null_string;  -- is nulls[0]
    /** How the Infinite is represented -- default is Inf */
    public String inf_string;

    /** 
     * Option for edition follow the same conventions as Parsing,
     * which are defined in Astroformat.
     **/

    /** Debugging option */
    public static final boolean DEBUG = false;
    // List of symbols used to edit sexagesimal
    static private char sexachar [] = {
	Character.MIN_VALUE, 	// 0 
	' ',			// 1 = blank
	':', 			// 2 = colon
	'h',			// 3 = hour
	'm',			// 4 = min
	's',			// 5 = sec
	'd',			// 6 = deg
	'\u00B0',		// 7 = {deg}
	'\'',			// 8 = arcmin
	'"',			// 9 = arcsec
    };

    // Factors of Y M D h m s to move to the next
    static private double fac6 [] = {
	12., 30., 
	24., 	/* day -> hr */
	60., 60. };

    // How to edit the sexagesimal. 
    // For each option, this array specifies 4 hexa digits:
    // #1 = number of components-1 (0, 1, or 2)
    // Other = index in sexachar of the symbol that follows the component
    static private short sexadrive[] = {
	0x0000, 	// 0 = DECIMAL -- edited in editDouble
	0x0000,		// 1 = EFORMAT -- edited in editDouble
	0x1100, 	// 2 = SEXA2   -- hh mm
	0x2110,		// 3 = SEXA3   -- hh mm ss
	0x1200,		// 4 = SEXA2c  -- hh:mm
	0x2220,		// 5 = SEXA3c  -- hh:mm:ss
	0x1640,		// 6 = SEXA2d
	0x2645,		// 7 = SEXA3d  -- d m s separator
	0x1640,		// 8 = SEXA2h
	0x2345,		// 9 = SEXA3h  -- h m s separator
	0x0000,		//10 = FACTOR  -- not sexa
	0x0600,		//11 = SEXA1d  -- a single number followed by d
	0x0300,		//12 = SEXA1h  -- a single number followed by h
	0x0700,		//13 = SEXA1o  -- a single number followed by &deg;
	0x1780,		//14 = SEXA2o  -- 2 components &deg; '
	0x2789,		//15 = SEXA3o  -- 3 components &deg; ' "
    };
    // Edition of date: 1=yr 2=Mon 3=MonNum 4=Day 5=Yday
    //            a=add- b=addBlank c=add, d=add/
    static private int datedrive[] = {
	0x1a2,		//  0 = "date(Y-M)",   
	0x13,		//  1 = "date(Ymon)",  
	0x2a1,		//  2 = "date(MY)",  
	0x31,		//  3 = "date(monY)",
	0x1a5,		//  4 = "date(YD)",   
	0x1a5,		//  5 = "#mjd-5#",     
	0x5d1,		//  6 = "date(DY)",  
	0x5d1,		//  7 = "#mjd-7#",   
	0x1a2a4,	//  8 = "date(YMD)",  
	0x1a3a4,	//  9 = "date(YmonD)", 
	0x4d2d1,	// 10 = "date(DMY)", 
	0x4a3a1,	// 11 = "date(DmonY)",
	0x2d4c1,	// 12 = "date(MDY)",  
	0x3a4c1,	// 13 = "date(monDY)", 
	0x2d1a4,	// 14 = "date(MYD)", 
	0x3d1a4,	// 15 = "date(monYD)",
    };
    static private char datesep[] = { '-', ' ', ',', '/' };

    /**
     * The basic constructor: default NaN edition is "-".
    **/
    public Editing() {
	// buf = new StringBuffer();
	super();
	inf_string  = "Inf";
    }

    /**
     * Constructor specifying the NaN representation, and default options.
     * @param null_text what to write in case of NULL (NaN) value.
    **/
    public Editing(String null_text) {
	// buf = new StringBuffer();
	setNaN(null_text);
	inf_string  = "Inf";
    }

    /**
     * Define the NaN representation.
     * @param text the choice for NULL (NaN) representation.
     * ---- Method inherited from parent class (Astroformat)
    public void setNaN(String text) {
	null_string = text;
    }
    **/

    /**
     * Define the Infinite representation
     * @param text the choice for Infinity
    **/
    public void setInfinite(String text) {
	inf_string = text;
    }

   /**
    * Internal edition of the sign and value of an integer number.
    * Used to ensure the right alignment.
    * @param  buf   the buffer to which the string representation is appended
    * @param  value the (positive) number to edit
    * @param  width the total width
    * @param  option  a mixture of options like 
    *  {@link Astroformat#ZERO_FILL},
    *  {@link Astroformat#SIGN_EDIT}, ...
    */
   private static void edN(StringBuffer buf, long value, int width, int opt) {
      char asig = Character.MIN_VALUE;
      String aval = Long.toString(value);	// Number in character.
      int  nb = width - aval.length();		// Number of blanks
      	if ((opt&(SIGN_EDIT|0x80000000)) != 0) {
      	    if ((opt&0x80000000) != 0) asig = '-'; 
	    else asig = '+';
	    nb--;
	}
	if ((opt&ZERO_FILL)!=0) {
	    if (asig != Character.MIN_VALUE) buf.append(asig); 
	    while (nb>0) { buf.append('0'); nb--; }
	}
	else {
	    while (nb>0) { buf.append(' '); nb--; }
	    if (asig != Character.MIN_VALUE) buf.append(asig);
	}
	buf.append(aval);
   }

   /**
    * Internal method to remove the useless decimals
    * @param  buf   the buffer -- remove ending zeroes until '.'
    * @return a boolean: true if decimal point removed.
    */
   private static boolean remove_zeroes(StringBuffer buf) {
      int i;
      boolean removed_point = false;
	for (i = buf.length()-1; buf.charAt(i) == '0'; i--) ;
	if (buf.charAt(i) == '.') removed_point = true; else ++i;
	buf.setLength(i);
	return(removed_point);
   }

   /**
    * Edit a number considered as a set of bits.
    * The bits set are edited from a table; list separated with commas.
    * @param  buf   the buffer to which the flags are appended
    * @param  flags    the list of flags.
    * @param  symbols  the table of symbols
    * @return the StringBuffer parameter
    */
    public StringBuffer editFlags(StringBuffer buf, int flags, String[] symbols) {
        int flag=flags;
        boolean need_comma=false;
      	for (int i=0; (flag!=0)&&(i<symbols.length); i++, flag>>>=1) {
            if((flag&1) == 0) continue;
            if(need_comma) buf.append(',');
            else need_comma = true;
            buf.append(symbols[i]);
	}
        if(flag!=0) buf.append("0x" + Integer.toHexString(flag<<symbols.length) + "??");
	return buf;
   }

   /**
    * Assignment to NaN represention.
    * Just appends the NULL string representation, right-aligned.
    * @param  buf   the buffer to which the string representation is appended
    * @param  width the total width
    * @return the StringBuffer parameter
    */
    public StringBuffer editNaN(StringBuffer buf, int width) {
      	for (int nb = width - nulls[0].length(); nb>0; nb--)
	    buf.append(' ');
	buf.append(nulls[0]);
	return buf;
    }

   /**
    * Assignment to Infinity represention, right-aligned.
    * Just appends the Infinite string representation, right-aligned.
    * @param  buf   the buffer to which the string representation is appended
    * @param  width the total width
    * @param  sign  positive(add +sign), negative(add -sign), or zero.
    * @return the StringBuffer parameter
    */
   public StringBuffer editInfinite(StringBuffer buf, int width, int sign) {
      	int nb = width - inf_string.length(); 
	if (sign != 0) nb--;
	while (--nb>=0) buf.append(' ');
	if (sign<0) buf.append('-');
	else if (sign != 0) buf.append('+');
	buf.append(inf_string);
	return buf;
   }

   /**
    * Edition of a long integer number, right-aligned, with user-specified 
    * options.
    * A long having the MIN_VALUE (0x8000000000000000L) is considered as NaN.
    * A long having &pm;MAX_VALUE (0x7fffffffffffffffL) is considered as Infty
    * @param  buf   the buffer to which the string representation is appended
    * @param  value the number to edit
    * @param  width the total width
    * @param  option  a mixture of options like 
    *  {@link Astroformat#ZERO_FILL},
    *  {@link Astroformat#SIGN_EDIT}, ...
    * @return the edited buffer
    */
   public StringBuffer editLong(StringBuffer buf, long value, int width, 
		   int option) {
      int o = option;
      long val = value;
    	if (val == Long.MIN_VALUE) this.editNaN(buf, width);
	else {
      	    if (val < 0) { o |= 0x80000000; val = -val; }
	    if (val == Long.MAX_VALUE) 
		this.editInfinite(buf, width, o&(0x80000000|SIGN_EDIT));
	    else edN(buf, val, width, o);
	}
	return buf;
   }

   /**
    * Edition of an integer number, right-aligned, with user-specified options.
    * An integer having the MIN_VALUE (0x80000000) is considered as NaN.
    * An integer having &pm;MAX_VALUE (0x7fffffff) is considered as Infty.
    * @param  buf   the buffer to which the string representation is appended
    * @param  value the number to edit
    * @param  width the total width
    * @param  option  a mixture of options like 
    *  {@link Astroformat#ZERO_FILL},
    *  {@link Astroformat#SIGN_EDIT}, ...
    * @return the edited buffer
    */
   public StringBuffer editInt(StringBuffer buf, int value, int width, 
		   int option) {
      int o = option;
      long val = value;
    	if (value == Integer.MIN_VALUE) this.editNaN(buf, width);
	else {
      	    if (value < 0) { o |= 0x80000000; val = -val; }
	    if (val == Integer.MAX_VALUE) 
		this.editInfinite(buf, width, o&(0x80000000|SIGN_EDIT));
	    edN(buf, val, width, o);
	}
	return buf;
   }

   /**
    * Edition of an integer number, right-aligned, with default options.
    * An integer having the MIN_VALUE (0x80000000) is considered as NaN.
    * @param  buf   the buffer to which the string representation is appended
    * @param  value the number to edit
    * @param  width the total width
    * @return the edited buffer
    */
   public StringBuffer edit(StringBuffer buf, int value, int width) {
	this.editInt(buf, value, width, 0);
	return(buf);
   }

   /**
    * Edition of a single floating-point number (%f).
    * The width of the edited number is nint+nd+1
    * The number is truncated when the option includes TRUNCATE;
    * it is rounded by default.<br>
    * Negative number of decimals <b>nd</b> follow the following conventions:
    * <UL>
    * <li><b>0</b>: no decimal, but the decimal point is edited 
    *              (e.g. 1.0 is edited "<tt>1.</tt>")
    * <li><b>-1</b>: the decimal point is not edited 
    *              (e.g. 1.0 is edited "<tt>1</tt>")
    * <li><b>&lt;-1</b>: the number is edited with <b>|nd|</b> decimals,
    * 		but useless decimals are removed. For nd=-3, the
    * 		number 1.123456 is edited "<tt>1.123</tt>",
    * 		but 1.200 will be edited "<tt>1.2</tt>"
    * </UL>
    * @param  buf   the buffer to which the string representation is appended
    * @param  value the number to edit
    * @param  nint  the number of characters for the integer part.
    * 		If <em>nint==0</em> a value in range ]0..1[ starts by a 
    * 		decimal point (e.g. <tt>.123</tt>)
    * @param  nd    the number of decimals -- use -1 to remove the decimal point
    *               -2 to -15 to remove the non-significant zeroes.
    * @param  option a combination of 
    *  {@link Astroformat#ZERO_FILL} /
    *  {@link Astroformat#SIGN_EDIT} /
    *  {@link Astroformat#TRUNCATE}
    * @return the editing buffer.
    */
   public StringBuffer editDecimal(StringBuffer buf, double value, 
 	  int nint, int nd, int option) {
      boolean shortest_form = nd < -1;
      	/*
      	if (DEBUG) System.out.println("....editDecimal(" + value +") nint=" 
	      + nint +", nd=" + nd);
	*/
      double val = value ;
      int o = option;
      long int_part ;
      int n;
        if (shortest_form) nd = -nd;
    	if (Double.isNaN(value)) {
	    // Edit the NaN representation such that the last char is aligned
	    // with the decimal point.
	    n = nint; if (nd>=0) n++;
	    this.editNaN(buf, n);
    	    return buf;
	}
      	if (value < 0) { o |= 0x80000000; val = -val; }
	if (Double.isInfinite(val)) {
	    // Edit the Infinity representation aligned to the decimal point.
	    n = nint; if (nd>=0) n++;
	    this.editInfinite(buf, n, o&(0x80000000|SIGN_EDIT));
    	    return buf;
	}
	if (val >= 1.e19)	// Can't edit such a number !
	    return this.editDouble(buf, value, 2, nint+nd-3, option);
	/* Verify a date, or a sexagesimal ?? */
	if ((option&DATE)!=0) {
	    int_part = (int)value;
	    if (value<0) int_part--;
	    return (this.editDate(buf, (int)int_part, option));
	}
	/* Verify a Sexagesimal edition ? */
        if (sexadrive[option&15] != 0)	// Option corresponds to sexagesimal...
	    return editSexa(buf, value, nint, nd, option);
	/* Round the number if asked */
	if ((option&TRUNCATE) == 0) {
	    if (nd <= 0) val += 0.5;
	    else val += 0.5/AstroMath.dexp(nd);
	}
	//System.out.println("....editDecimal("+value+") nd="+nd);
    	int_part = (long) val; 
    
    	// Edit the integer part -- take care of nint=0
	if ((nint>0) || (int_part!=0))
	    edN(buf, int_part, nint, o);
    
    	// Add now the decimals
    	if (nd >= 0)
    	     buf.append('.');
	if (nd > 0) {
	    val -= int_part;
	    //System.out.print("....editFract("+val+",nd="+nd+") =>");
    	    val *= AstroMath.dexp(nd);
	    //System.out.println(val);
    	    int_part = (long) val; 
	    //System.out.println(" => ="+int_part);
	    edN(buf, int_part, nd, ZERO_FILL);
	    if (shortest_form) {	// Remove the decimals, keep just 1
		if (remove_zeroes(buf))	// ... but decimal point MUST be there
		    buf.append(".0");
	    }
    	}
    
    	// Finally, return.
    	return (buf);
   }

   /**
    * Edition of a single floating-point number (%g or %e).
    * The standard edition is decimal in range [0.1, 1000[, E-format otherwise.
    * The %e format can be forced with the EFORMAT option.<br>
    * Remark: the number is truncated (instead of the default rounding)
    * if the TRUNCATE option is specified.<br>
    * The total width used for the edition is 3+ndig+nexp or 5+ndig+nexp
    * depending on the FACTOR option.
    * @param  buf   buffer to which the string representation is appended
    * @param  value number to edit
    * @param  ndig  number of significant digits (do the best if ndig&leq;0)
    * @param  nexp  number of characters for the exponent value (3 recommended)
    * @param  option a combination of {@link Astroformat#EFORMAT} 
    * / {@link Astroformat#SIGN_EDIT}
    * @return the editing buffer.
    */
   public StringBuffer editDouble(StringBuffer buf, double value, int ndig, 
		   int nexp, int option) {
      String expo = (option&FACTOR)!=0 ? "x10" : "e";
      int o = option;
      int posini = buf.length();
      double val = value ;
      boolean shortest_form = ndig <= 0;
      boolean decimal_form;
      double f;
      int e, n;
        if (sexadrive[option&15] != 0)	// Option corresponds to sexagesimal...
	    return editSexa(buf, value, ndig ,nexp, option);
    	if (Double.isNaN(value) || Double.isInfinite(value)) {
	    // Edit the special values
    	    return editDecimal(buf, value, ndig+2, expo.length(), option);
	}
	if (shortest_form) ndig = 15;
	if (value < 0) { o |= 0x80000000; val = -val; }
	decimal_form = (option&EFORMAT)==0;
	if (decimal_form) {
	    if (val == 0.) ;
	    else decimal_form = (val>=0.1) && (val<1000.) ;
	}
	if (decimal_form) {
	    n = 2;
	    if (val >=  10.) n++;
	    if (val >= 100.) n++;
	    this.editDecimal(buf, value, shortest_form ? 1 : n, ndig-n+1, 
		option);
	    if (shortest_form) remove_zeroes(buf);
	    else for (n=expo.length()+nexp; n>0; n--) buf.append(' ');
	    return buf;
	}
	/* Find the exponent n from IEEE format such that:
	   10^n <= val < 10^(n+1) 
	   i.e. normalize the number in range 1 .. 10
	*/
	e = (int)(Double.doubleToLongBits(val) >> 52) - 0x3ff;  // Expo base 2
	e = (e*3)/10;		// Approximative exponent in base 10
	f = AstroMath.dexp(e);
	while (val < f)      f = AstroMath.dexp(--e);
	while (val >= 10.*f) f = AstroMath.dexp(++e);
	val /= f;		// Value in range [1, 10[
	/* Round the Value */
	if (((option&TRUNCATE) == 0) && (ndig>0)) {
	    val += 5./AstroMath.dexp(ndig);
	    //System.out.println("....editDouble("+value+",ndig="+ndig
	    //  +") truncated="+val);
	    if (val>=10.) { f/=10; ++e; }
	}
	this.editDecimal(buf, val, shortest_form ? 1 : 2, ndig-1, 
	    (o&(~ZERO_FILL))|TRUNCATE);
	if (shortest_form && remove_zeroes(buf)) {	// decimal point removed
	    /* In EFORMAT, 1E10 would not be acceptable for 1.E+10;
	     * and in FACTOR format, avoid 1x10+5, prefer 10+5
	     */
	    if ((option&FACTOR) == 0) buf.append('.');
	    else if ((buf.length() == 1+posini) && (buf.charAt(posini) == '1')){
		buf.setLength(posini);
		expo = "10";
	    }
	}
	buf.append(expo);
	this.editInt(buf, e, nexp, ZERO_FILL|SIGN_EDIT);
    
    	// Finally, return.
    	return (buf);
   }

   /**
    * Edition of a single floating-point number (%g or %e).
    * The standard edition is decimal in range [0.1, 1000[, E-format otherwise.
    * Edits in the best possible way.
    * @param  buf   buffer to which the string representation is appended
    * @param  value number to edit
    * @param  option a combination of {@link Astroformat#EFORMAT} 
    * / {@link Astroformat#SIGN_EDIT}
    * @return the editing buffer.
    */
   public StringBuffer editDouble(StringBuffer buf, double value, int option) {
      return(editDouble(buf, value, 0, 0, option));
   }

   /**
    * Edition of a single floating-point number in Sexagesimal.
    * Remark: the number is truncated -- not rounded.
    * @param  buf   buffer to which the string representation is appended
    * @param  value number to edit
    * @param  nint  number of characters for the integer part (degrees)
    * @param  ndec  number of 'decimals' (2=arcmin, 4=arcsec, ...)
    * 			use a negative number to specify the "maximal"
    * 			number of decimals.
    * @param  option a combination of 
    *   {@link Astroformat#SEXA1d} (and other SEXAxx possibilities) / 
    *   {@link Astroformat#ZERO_FILL} / 
    *   {@link Astroformat#SIGN_EDIT} ...
    * @return the editing buffer.
    */
   public StringBuffer editSexa(StringBuffer buf, double value, 
		   int nint, int ndec, int option) {
      int o = option;
      int nd = Math.abs(ndec);
      boolean remove_zeroes = ndec<0;
      short form = (short)(option&15);	// One of the values SEXA3, SEXA2, ...
      double val = value ;
      double f;
      long part;
      int n;
      char sep;
        if (form == 0) form = SEXA3c;	// Default with colons
    	if (Double.isNaN(value) || Double.isInfinite(value)) {
	    this.editDecimal(buf, value, nint, nd, option);
    	    return buf;
	}
	// Set nd to the number of decimals of LAST part
	// nd = Math.abs(nd);
	form = sexadrive[form];
	if ((form&0x1000) != 0) nd -= 2;
	if ((form&0x2000) != 0) nd -= 4;
	while ((nd < 0) && ((form&0xf000)!=0)) {
	    form -= 0x1000;		// Now form = 0x1... or 0x0...
	    nd += 2;
	    if ((form&0xf000) != 0) {	// No seconds
		if ((form&0xf) == 0) form &= 0xff00;
		form &= 0xfff0;
	    }
	    else {			// No minutes/seconds
		if ((form&0xf0) == 0) form = 0;
		form &= 0xff00;
	    }
	}
	//System.out.println("....editSexa: form=0x"+Integer.toHexString(form)
	// +",nd="+nd);
	// If just a single number ==> do it via the other routines
    	if ((form&0xf000) == 0) {
	    this.editDecimal(buf, value, nint, nd, 	// Mod V1.1
	        o&(ZERO_FILL|SIGN_EDIT|TRUNCATE));
	    n = form>>8;
	    if (n>0) buf.append(sexachar[n]);
    	    return buf;
	}
	if (nd<0) nd=0;
      	if (value < 0) { o |= 0x80000000; val = -val; }
	/* Round the value */
	if ((option&TRUNCATE) == 0) {
	    f = (form&0x2000) == 0 ? 60. : 3600.;
	    f *= AstroMath.dexp(nd);
	    val += 0.5/f;
	    //System.out.println("....editSexa("+value+",nd="+nd+"), rounded="
	    //  +val);
	}

	/* Find the final width required by the fractions */
	part = (long)val; val -= part;
	edN(buf, part, nint, o);
	sep = sexachar[(form>>8)&0xf];
	if ((form&0xf000) != 0) { 	// Edit the minutes
	    if (sep != Character.MIN_VALUE) buf.append(sep);
	    sep = sexachar[(form>>4)&0xf];
	    val *= 60.; 
	    if ((form&0x2000) == 0) {	// Minutes only
		if (nd==0) nd=-1;	// Don't edit the decimal point
		this.editDecimal(buf, val, 2, nd, ZERO_FILL|TRUNCATE);
	    }
	    else {
	    	n = (int)val; val -= n;
		buf.append(Character.forDigit(n/10, 10));
		buf.append(Character.forDigit(n%10, 10));
	    }
	}
	if ((form&0x2000) != 0) {	// Edit the seconds
	    if (sep != Character.MIN_VALUE) buf.append(sep);
	    sep = sexachar[form&0xf];
	    val *= 60.;
	    if (nd == 0) {
	    	n = (int)val; 
		buf.append(Character.forDigit(n/10, 10));
	    	buf.append(Character.forDigit(n%10, 10));
	    }
	    else this.editDecimal(buf, val, 2, nd, ZERO_FILL|TRUNCATE);
	}
	if (sep != Character.MIN_VALUE) buf.append(sep);
	
	// Remove the ending zeroes
	if (remove_zeroes) {
	    for (n = buf.length()-1; (n>1) && (buf.charAt(n) == '0'); n--) ;
	    if (buf.charAt(n) != '.') n++;	// Remove also decimal point
	    buf.setLength(n);
	}
    	// Finally, return.
    	return (buf);
   }

   /**
    * Internal edition of MJD date -- uses elements in datedrive.
    * @param  buf   buffer to which the string representation is appended
    * @param  mjd   the date to edit
    * @param  driver the suite of elements to edit
    * @return the editing buffer.
    */
   private final void  edate(StringBuffer buf, int mjd, int driver) {
      int form, i, j, n4, nd10; // year, month, day, yday;
      int[] ymd = new int[4];
      	
        Astrotime.JD2YMD(Astrotime.JD_MJD0+mjd, ymd);
	/* ---------------
      	j = mjd + 2400001;	// Julian Date
	year = -4712;
	while (j <= 4480)   { j += 146097; year -= 400;}
	n4 = 4*(j+((2*((4*j-17918)/146097)*3)/4+1)/2-37);
	nd10=10*( ((n4-237)%1461)/4)+5;

	year += n4/1461;
	month = (nd10/306+2)%12;
	day   = (nd10%306)/10+1;
	------------------ */

	for (form = driver; form!=0; form <<= 4) switch(i = (form>>28)&15) {
	  case 0: 
	    continue;
	  case 1: 		// Year
	    if (ymd[0] < 0) {
	    	buf.append('-');
	    	edN(buf, -ymd[0], 3, Astroformat.ZERO_FILL);
	    }
	    else edN(buf, ymd[0], 4, Astroformat.ZERO_FILL);
	    continue;
	  case 2:		// Month number
	    edN(buf, ymd[1]+1, 2, Astroformat.ZERO_FILL);
	    continue;
	  case 3:		// Month name
	    buf.append(month_list[ymd[1]]);
	    continue;
	  case 4:		// Day number
	    edN(buf, ymd[2], 2, Astroformat.ZERO_FILL);
	    continue;
	  case 5:		// Day in Year
	    /* ------------
	    n4 = (j+32104)%146097;	// Day in 400-yr cycle, #0=01-Jan-2000
	    if (n4 >= 36525) 		// Century#0 is 25 leap years
	        n4 += (n4-366)/36524;
	    n4 %= 1461;		// Day in 4-yr cycle
	    if (n4 >= 731) n4 += (n4-366)/365;
	    yday = n4%366;
	    -------------- */
	    edN(buf, ymd[3]+1, 3, Astroformat.ZERO_FILL);
	    continue;
	  default:		// Separator
	    buf.append(datesep[i-10]);
	    continue;
	}
    }

   /**
    * Edition of a MJD date into an ISO-8601 date YYYY-MM-DD
    * @param  buf   buffer to which the string representation is appended
    * @param  mjd   the date to edit
    * @return the editing buffer.
    */
   public StringBuffer editDate(StringBuffer buf, int mjd) {
	edate(buf, mjd, datedrive[DATE_YMD&15]);
	return(buf);
   }

   /**
    * Edition of a MJD date into an ISO-8601 date YYYY-MM-DD
    * @param  buf   buffer to which the string representation is appended
    * @param  mjd   the date to edit
    * @param  option the option for date editing (e.g. Astroformat.DATE_YMD)
    * @return the editing buffer.
    */
    public StringBuffer editDate(StringBuffer buf, int mjd, int option) {
	if ((option&DATE) == 0) option = DATE_YMD;
	edate(buf, mjd, datedrive[option&15]);
	return(buf);
    }

   /**
    * Edition of a MJD date into an ISO-8601 date YYYY-MM-DDThh:mm:ss[...]
    * @param  buf   buffer to which the string representation is appended
    * @param  mjd   the date to edit
    * @param  time_prec the precision in the time edition.
    * 		Values are 0=no time, 2=hr, 4=min, 6=sec, 9=ms, etc.
    * 		Negative values indicate a maximal precision.
    * @param  option the option for date editing (e.g. Astroformat.DATE_YMD)
    * @return the editing buffer.
    */
    public StringBuffer editDate(StringBuffer buf, double mjd, 
	    int time_prec, int option) {
      int j;

        if (Double.isNaN(mjd)) {
	    if (time_prec < 2) 
		return(editNaN(buf, time_prec < 0 ? 0 : 11)) ;
	    return(editSexa(buf, mjd, 13, time_prec-2, 0));
	}

	if ((option&DATE) == 0) option = DATE_YMD;
      	j = (int) mjd;
	edate(buf, j, datedrive[option&15]);
	// Edit the time part.
	if (mjd<0)  --j;
	double sec = (mjd - j)*86400;   
	// No time edition when time_prec is zero, or time=0...
	if ((time_prec <= 0) && ((time_prec>= -1) || (sec == 0)))
	    return(buf);
	// Edit time preceded by 'T'
	buf.append('T');
	j = time_prec-2;	// Precision for edition of hours
	if (time_prec < 0) j -= 4;
	editSexa(buf, sec/3600., 2, j, (option&TRUNCATE)|ZERO_FILL|SEXA3c);
	return(buf);
    }

   /**
    * Edit a complex number (date, time, angle).
    * @param  buf   buffer to which the string representation is appended
    * @param  mjd   the value to edit (MJD in case of date/time)
    * @param  pic   the 'picture' to use, includes
    * 		Y=year, M=month, D=day, h=hour, m=min, s=sec, d=deg f=fraction
    * @return the editing buffer.
    */
    public StringBuffer editComplex(StringBuffer buf, double mjd, String pic) {
      int[]  ymd   = new int[4];	// Date elements
      byte[] len6  = new byte[6];	// Length of each element Y M  h m s
      byte[] elm6  = new byte[6];	// Index of components 
      char[] apic  = pic.toCharArray();
      int bpic = 0;			// begining in pic
      int epic = pic.length();		// end      in pic
      int ipic, ielm, len, k, i, j;
      boolean minus = false;
      boolean mixed = false;		// Indicates change M<->m D<->d
      double val = mjd;
      double round = 1.e-11;		// Precision of fraction to edit
      int valunit = 2;			// Unit of val 2=D 3=h 4=m 5=s
      char c, p;

        if (Double.isNaN(val)) 
	    return(editSexa(buf, val, pic.length(), -1, 0));
	if (DEBUG) System.out.println("....editComplex(" + mjd +")  pic=" 
		+ pic);

	// Initialisation
	len6[0] = len6[1] = len6[2] = len6[3] = len6[4] = len6[5] = 0;
	elm6[0] = elm6[1] = elm6[2] = elm6[3] = elm6[4] = elm6[5] = 0;
	if (val>0) val += 0.49*round;
	else       val -= 0.49*round;
	
	// Scan the elements to edit
	// The pic may be sourronded by quotes
	if (apic[0] == '"') { bpic = 1; epic--; }
	for (ielm=0, ipic=bpic; ipic < epic; ) {
	    c = apic[ipic++];
	    k = pic1.indexOf(c);
	    if (k<0) continue;
	    k %= 6;
	    if (len6[k] != 0) {		// Duplication in 'picture' ?
		i = 5-k;		// Month <-> Minute, Day <-> Deg
		if ((k>0) && (i>0) && (len6[i] == 0)) {
		    len6[i] = len6[k];
		    for (j=0; elm6[j] != k; j++) ;
		    elm6[j] = (byte)i;
		    k = i;
		}
	    }
	    elm6[ielm++] = (byte)k;
	    // Count the number of identical elements in picture
	    for(len=1; (ipic<epic) && (apic[ipic] == c); ipic++) len++;
	    if (len6[k] == 0) len6[k] = (byte)len;
	    if (ielm >= 6) break;
	}
	if (DEBUG) {
	    String s;
	    System.out.print("    edCplx[a]:elm"); s="="; 
	    for(i=0;i<6;i++) { System.out.print(s+elm6[i]);s=","; } 
	    System.out.print('\t');
	    System.out.print("    edCplx[a]:len"); s="="; 
	    for(i=0;i<6;i++) { System.out.print(s+len6[i]);s=","; } 
	    System.out.print('\n');
	}

	// Verify possible mixing month[1]/min[4] 
	if (     (len6[1] != 0) /* Month given */ 
	      && (len6[0] == 0) /* but no year */ 
	      && (len6[4] == 0) /* and no min  */ ) {
	    len6[4] = len6[1]; len6[1] = 0;
	    for (j=0; elm6[j] != 1; j++); elm6[j] = 4;
	    mixed = true;
	}
	else if ((len6[1] == 0)	/* No month given */ 
	      && (len6[0] != 0) /* but year given */ 
	      && (len6[4] != 0) /* and minutes... */ 
	      && (len6[5] == 0) /* and no seconds */) {
	    len6[1] = len6[4]; len6[4] = 0;
	    for (j=0; elm6[j] != 4; j++); elm6[j] = 1;
	    mixed = true;
	}

	// Verify possible mixing day[2]/deg[3]
	if (     (len6[2] != 0) /* A day given  */
	      && (len6[3] == 0) /* but no hour  */
	      && (len6[0] == 0) /* and no year  */ 
	      && (len6[1] == 0) /* and no month */) {
	    len6[3] = len6[2]; len6[2] = 0;
	    for (j=0; elm6[j] != 2; j++); elm6[j] = 3;
	    mixed = true;
	}
	else if ((len6[3] != 0)	/* Degree given   */
	      && (len6[0] != 0) /* and a year ... */
	      && (len6[2] == 0) /* but no day ... */) {
	    len6[2] = len6[3]; len6[3] = 0;
	    for (j=0; elm6[j] != 3; j++); elm6[j] = 2;
	    mixed = true;
	}
	if (mixed && (DEBUG)) {
	    String s;
	    System.out.print("    edCplx[b]:elm"); s="="; 
	    for(i=0;i<6;i++) { System.out.print(s+elm6[i]);s=","; } 
	    System.out.print('\t');
	    System.out.print("    edCplx[b]:len"); s="="; 
	    for(i=0;i<6;i++) { System.out.print(s+len6[i]);s=","; } 
	    System.out.print('\n');
	}

	// Convert the values into their components
	if (len6[0] != 0) {	// It's a date
	    Astrotime.JD2YMD(Astrotime.JD_MJD0+val, ymd);
	    val -= Math.floor(val);	// Remaining fraction of days
	    /*---
	    if (DEBUG) {
		String s;
		System.out.print("    edCplx-date=="); s="="; 
		for(i=0;i<4;i++) { System.out.print(s+ymd[i]);s=","; } 
		System.out.print(" h=" + val); System.out.print('\n');
	    }
	    ---*/
	}
	else {				// Not a date -- could dontain days
	    if (val<0) { 	
		minus = true;
		val = -val;
	    }
	    if (len6[2] > 0) {		// There are days
	        ymd[3] = (int)val;	// ymd[3] = yday (1..366)
	        val -= ymd[3];		// Remaining fraction of days
	    }
	    else valunit = 3;		// Not a date, no day, unit is hours.
	}

	/* Edit now the elements */
	c = p = Character.MIN_VALUE;	// p = preceding char
	k = 0;
	for (ielm=0, ipic=bpic; ipic < epic; ) {
	    p = c;
	    c = apic[ipic];
	    if (c == 'f') break;	// Fraction
	    ipic++;
	    k = pic1.indexOf(c);
	    if (k<0) { 			// Unknown element, copy it (e.g. :)
		buf.append(c); 
		continue; 
	    }
	    i = k%6;			// Element 0=y 1=M, 2=D 3=h 4=m 5=s
	    if (ielm < 6) { i = elm6[ielm++]; len = len6[i]; }
	    else len = 0;
	    if (len == 0) {
	        for(len=1; (ipic<epic) && (apic[ipic] == c); ipic++) len++;
	    }
	    ipic += (len-1);
	    if (minus) { buf.append('-'); minus = false; len--; }
	    /* Set value to the correct unit */
	    while (valunit<i)  { 
		val   *= fac6[valunit]; 
		round *= fac6[valunit];
		valunit++;
	    }
	    if (DEBUG) {
		System.out.print("    edCplx["+i+"]: "+c+" len="+len 
			+ ",len6[1]=" + len6[1]); 
		System.out.print('\n');
	    }
	    switch(i) {
	      case 0:	// Year
		j = ymd[0]; if (len6[0] == 2) j %= 100;
		edN(buf, j, len, Astroformat.ZERO_FILL);
		break;
	      case 1:	// Month
		if (len6[1] > 2) buf.append(month_list[ymd[1]]);
		else edN(buf, ymd[1]+1, 2, Astroformat.ZERO_FILL);
		break;
	      case 2:	// Day in month, or yday (if no month)
		if (len<2) len=2;
		j = len6[1] == 0 ? ymd[3] : ymd[2];
		/*---
		if (DEBUG) {
		    System.out.print("    edCplx[2]: len6[1]=" + len6[1] 
			    + ", j=" + j); 
		    System.out.print('\n');
		}
		---*/
		edN(buf, j, len, len6[0]>0 ? ZERO_FILL : 0);
		break;
	      case 3:	// Hour or degree
		if (len<2) len=2;
		j = (int) val; 
		edN(buf, j, len, len6[2]>0 ? ZERO_FILL : 0);
		val -= j;
		break;
	      case 4:	// Minutes
		if (len<2) len=2;
		j = (int) val;
		edN(buf, j, len, j<60 ? ZERO_FILL : 0);
		val -= j;
		break;
	      case 5:	// Seconds
		if (len<2) len=2;
		j = (int) val ;
		edN(buf, j, len, j<60 ? ZERO_FILL : 0);
		val -= j;
		break;
	    }
	    //if (ielm < 6) { i = elm6[ielm]; len6[i] = 0; ielm++; }
	}
	if (minus) { buf.append('-'); minus = false; }

	// edit eventual fraction
	if (DEBUG) {
		System.out.print("    Fraction="+val + " (valunit=" 
			+ valunit + ")\n");
	}
	len = 0;	// Number of 'f'
	if (c == 'f') for (; (ipic<epic) && (apic[ipic] == 'f'); ipic++) 
	    len++;
	if ((len>0) || (val>round)) {
	    if ((len == 0) && (p != '.'))  buf.append('.');
	    i = len>0 ? len : 9;	/* Max = 9 decimals */
	    while (--i >= 0) {
		val *= 10; round *= 10.;
		j = (int)val;
		val -= j;
		buf.append(Character.forDigit(j, 10));
		if ((val <= round) && (len == 0)) break;
	    }
	}

	// and eventual fill characters
	while (ipic < epic) { buf.append(apic[ipic]); ipic++; }
	return(buf);
    }

    /**
     * The conversion to string
    **/
    public String toString() {
      StringBuffer b = new StringBuffer(64);
        b.append("null_text=");  b.append(nulls[0]);
        b.append(", inf_text="); b.append(inf_string);
	return(b.toString());
    }
}
