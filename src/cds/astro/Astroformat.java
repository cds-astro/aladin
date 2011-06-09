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

/*==================================================================
                Astroformat class
 *==================================================================*/

/**
 * This class contains definitions used in Parsing and Editing of numbers,
 * and the default representation of the <b>NaN</b> <em>(null)</em> value.
 * @author Francois Ochsenbein -- francois@astro.u-strasbg.fr
 * @version 1.0 : 12-Aug-2004 as a separate entiry
 * @version 1.1 : 12-Aug-2006 added DATE_MJD
 * @version 1.2 : 30-Oct-2006 added DATE_DIFF
 */

public class Astroformat {
    /** The default way of representing the NULL (unknown) numbers .
     * The first element of the array contains the default_null.
     * Implicitely the last character of any NULL representation 
     * may be repeated.
    **/
     public String[] nulls;

    	/** Format of number is <em>decimal</em> */
    static public final int DECIMAL=0;
    	/** Format of number contains the <em>E</em> notation */
    static public final int EFORMAT=1;
    	/** Format of number is of the form 
     	 *  <em>mantissa</em><tt>x10</tt><em>&pm;exp</em> */
    static public final int FACTOR=10;
    	/** Format of number is sexagesimal (2 components h m) */
    static public final int SEXA2=2;
    	/** Format of number is sexagesimal (3 components h m s) */
    static public final int SEXA3=3;
    	/** Format of number is sexagesimal (2 components h:m) */
    static public final int SEXA2c=4;
    	/** Format of number is sexagesimal (3 components h:m:s) */
    static public final int SEXA3c=5;
    	/** Format of number is sexagesimal angle (2 components dm) */
    static public final int SEXA2d=6;
    	/** Format of number is sexagesimal angle (3 components dms) */
    static public final int SEXA3d=7;
    	/** Format of number is sexagesimal time  (2 components hm) */
    static public final int SEXA2h=8;
    	/** Format of number is sexagesimal time  (3 components hms) */
    static public final int SEXA3h=9;
    	/** Format of number is sexagesimal 1 component (with : or d)*/
    static public final int SEXA1d=11;
    	/** Format of number is sexagesimal 1 component (with h)     */
    static public final int SEXA1h=12;
    	/** Format of number is sexagesimal 1 component (with &deg;)     */
    static public final int SEXA1o=13;
    	/** Format of number is sexagesimal 2 component (with &deg; ')   */
    static public final int SEXA2o=14;
    	/** Format of number is sexagesimal 3 component (with &deg; ' ") */
    static public final int SEXA3o=15;
    	/** The number represents a date; variants follow */
    static public final int DATE      =128;
    	/** Format of date contains alphabetical month */
    static public final int DATE_alpha=1|128;
    	/** Format of date   is Year and Month */
    static public final int DATE_YM   =0|128;
    	/** Format of date   is Year and Month */
    static public final int DATE_MY   =2|128;
    	/** Format of date   is Year and Day, day in range [1..366] */
    static public final int DATE_YD   =4|128;
    	/** Format of date   is Day and Year, day in range [1..366] */
    static public final int DATE_DY   =6|128;
    	/** Format of date   is Year Month Day */
    static public final int DATE_YMD  =8|128;
    	/** Format of date   is Day Month Year */
    static public final int DATE_DMY  =10|128;
    	/** Format of date   is Month Day Year */
    static public final int DATE_MDY  =12|128;
    	/** Format of date   is Month Year Day */
    static public final int DATE_MYD  =14|128;
    	/** Time expressed as days (elapsed time) */
    static public final int DATE_DIFF =5|128;
    	/** Number must be signed (contains a '+' sign  if positive) */
    static public final int SIGN_EDIT=16;
    	/** Number must be left-filled with zeroes  */
    static public final int ZERO_FILL=32;
    	/** Numbers are truncated in their edition  */
    static public final int TRUNCATE =64;
    	/** Number represents a date (returned as MJD) */
    static public final int DATE_COMP =128;

    /** The interesting pictures for Complex numbers */
    static protected final String pic1 = "YMDhmsymDdMS";
    /** Explanation of the meaning */
    static protected final String[] pic1_explain = {
	"Year", "Month", "Day", "hour",   "minute", "second", 
	"year", "month", "day", "degree", "Minute", "Second"
    };

    /** The standard edition of month names */
    static public String[] month_list = {
	"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
	"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    /** Characteristics of the formats as bit patterns, where
     *  0x10 = is Sexa
     *  0x01 = is Time
     *  0x02 = is Angle
    **/
    static private final byte[] properties = {
	0x00, 	// 0 = DECIMAL -- edited in editDouble
	0x00,	// 1 = EFORMAT -- edited in editDouble
	0x10, 	// 2 = SEXA2   -- hh mm
	0x10,	// 3 = SEXA3   -- hh mm ss
	0x10,	// 4 = SEXA2c  -- hh:mm
	0x10,	// 5 = SEXA3c  -- hh:mm:ss
	0x12,	// 6 = SEXA2d
	0x12,	// 7 = SEXA3d  -- d m s separator
	0x11,	// 8 = SEXA2h
	0x11,	// 9 = SEXA3h  -- h m s separator
	0x00,	//10 = FACTOR  -- not sexa
	0x02,	//11 = SEXA1d  -- a single number followed by d
	0x01,	//12 = SEXA1h  -- a single number followed by h
	0x02,	//13 = SEXA1o  -- a single number followed by &deg;
	0x12,	//14 = SEXA2o  -- 2 components &deg; '
	0x12,	//15 = SEXA3o  -- 3 components &deg; ' "
    };
    /** Arrays used by Parsing.form() to explain the input */
    static private final String[] explain_form = {
      "decimal",    "E-format", 
      "sexa(h,m)",  "sexa(h,m,s)", "sexa(h:m)",     "sexa(h:m:s)",
      "sexa_d_m",   "sexa_d_m_s",  "sexa_h_m",      "sexa_h_m_s",
      "x10-format",                "decimal_d",     "decimal_h",
                "decimal_\u00B0",  "sexa_\u00B0_'", "sexa_\u00B0_'_\""
    };
    static private final String[] explain_date = {
      "date(YM)",   "date(Ymon)",  "date(MY)",  "date(monY)", 	// Year+Month
      "date(YD)",   "#mjd-5#",     "date(DY)",	"#mjd-7#", 	// year + yday
      "date(YMD)",  "date(YmonD)", "date(DMY)", "date(DmonY)",	// 3 components
      "date(MDY)",  "date(monDY)", "date(MYD)", "date(monYD)",	// 3 components
    };
    static private final String[] explain_pec = {
      "", ",signed", ",zero-filled", ",signed,zero-filled"
    };

    /** 
     * Explain the 'form' of the last parsed number
     * @return A printable variant of format.
     */
    static public final String explain(int bin_form) {
      int k = (bin_form)&0xff;
      String s;
      // System.out.println("....form: flags=0x"+Integer.toHexString(flags)
      //                   +", k="+k);
        s = (k&DATE_COMP)!=0 ? explain_date[k&0xf] :  explain_form[k&0xf];
        return (s + explain_pec[(k>>4)&3]);
    }

    /** 
     * Test whether a format indicates a Sexagesimal coding.
     * @return true if it does.
     */
    static public final boolean isSexa(int bin_form) {
	return((properties[bin_form&0xf]&0x10)!=0);
    }

    /** 
     * Test whether a format indicates a Date (YMD).
     * Units are days (a date is expressed as MJD)
     * @return true if it does.
     */
    static public final boolean isDate(int bin_form) {
	return(((bin_form&DATE)!=0) && (!isDays(bin_form)));
    }

    /** 
     * Test whether a format indicates a number of days.
     * Similar to isTime, but units are days.
     * @return true if it does.
     */
    static public final boolean isDays(int bin_form) {
	return((bin_form&(DATE_DIFF|0xf)) == DATE_DIFF);
    }

    /** 
     * Test whether a format indicates Time (hms). 
     * Units are then hours.
     * @return true if it does.
     */
    static public final boolean isTime(int bin_form) {
	return((properties[bin_form&0xf]&1)!=0);
    }

    /** 
     * Test whether a format indicates Angle (dms or &deg;'"). 
     * Units are then degrees
     * @return true if it does.
     */
    static public final boolean isAngle(int bin_form) {
	return((properties[bin_form&0xf]&2)!=0);
    }

    /*==================================================================
      			Explain the pictures
     *==================================================================*/

    /** 
     * Explain the conventions of "complex" templates.
     * 		Letters are Y y (years) M (month) D (day) h (hour) m (minutes)
     * 		s (seconds) d (degrees) f (fractions), and punctuations like
     * 		: (colon) / (slash), etc. 
     * @param	pic "picture" which specifies the format
     * @return  the explanations
     */
     public static final String explainComplex(String pic) {
      StringBuffer buf = new StringBuffer(128);
      char[] typ6 = new char[6];
      char[] apic = pic.toCharArray();
      int epic = pic.length();
      int ipic = 0;
      int len, k;
      char c;

	typ6[0] = typ6[1] = typ6[2] = typ6[3] = typ6[4] = typ6[5] = 
	    Character.MIN_VALUE;
	while (ipic<epic) {
	    c = apic[ipic++]; 
	    k = pic1.indexOf(c);
	    if (k<0) continue;
	    for (len=1; (ipic<epic) && apic[ipic] == c; ipic++) len++;
	    typ6[k%6] = Character.toLowerCase(c);
	}
	buf.append("Complex ");
	if (typ6[0] != Character.MIN_VALUE) buf.append("date");
	else if ((typ6[2] != Character.MIN_VALUE) || (typ6[3] == 'h'))
	     buf.append("time");
	else buf.append("angle");
	buf.append(" made of:") ;

	for(ipic=0; ipic<epic; ) {
	    c = apic[ipic++];
	    k = pic1.indexOf(c);
	    if ((k<0) && (c != 'f')) continue;
	    buf.append(' ');
	    if (k<0) buf.append("and fraction");
	    else buf.append(pic1_explain[k]);
	    for (len=1; (ipic<epic) && apic[ipic] == c; ipic++) len++;
     	}
	return(buf.toString());
     }

    /*==================================================================
      			Constructors
     *==================================================================*/

    /**
     * Create an Astroformat with the defaut "---" edition for NULL values.
    **/
    public Astroformat() {
	setNaN("---");
    }

    /**
     * Create an Astroformat with the defaut "---" edition for NULL values.
    **/
    public Astroformat(String default_null) {
	setNaN(default_null);
    }

    /*==================================================================
     * 			Additional acceptances
     *==================================================================*/

    /**
     * Change the default edition of NULL.
     * All alternative representations are removed.
     * @param default_null The default <em>null</em> representation
    **/
    public void setNaN(String default_null) {
	nulls = new String[2];
	nulls[0] = default_null;
	nulls[1] = null;
    }

    /**
     * Give alternative representation of nulls.
     * @param representation An additional possible representation of
     * 		a <em>null</em>. The last character may be repeated.
    **/
    public void acceptAsNaN(String representation) {
	int i = nulls.length-1;
	if (nulls[i] != null) {		// Need an extension
	    ++i;
	    String[] array = new String[i+2];
	    System.arraycopy(nulls, 0, array, 0, i);
	    nulls = array;
	    nulls[i+1] = null;
	}
	nulls[i] = representation;
    }

}
