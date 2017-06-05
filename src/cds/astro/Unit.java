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

/**
 *==========================================================================
 * @author  Fran&ccedil;ois Ochsenbein -- francois@astro.u-strasbg.fr
 * @version 0.7 07-sep-2002
 * @version 0.9 15-sep-2002: 
 * @version 1.0 15-Aug-2004: added getUnit getValue setUnit setValue parse
 * @version 1.1 02-Sep-2006: Unicode symbols
 * @version 1.2 02-Nov-2006: Conversion of dates
 * @version 1.3 02-May-2012: Correct log scales
 *==========================================================================
 */

import java.util.*;
import java.text.*;	// for parseException

/*==================================================================
                Unit class
 *==================================================================*/

/**
 * Class able to deal with scientific units and make the appropriate
 * conversions. 
 * This class is an application of the
 * "Adopted Standards for Astronomical Catalogues"
 * (http://vizier.u-strasbg.fr/doc/catstd.htx).
 * It is tightly related to the class {@link Converter} which can
 * define <em>non-standard</em> conversions.
 * <P>
 * Basically, a unit consists in a <em>value</em> associated to a 
 * <em>unit symbol</em> which is itself related to the SI 
 * <em>(Syst&egrave;me International)</em>. Most of the unit symbols in use in 
 * astronomy are included, but it is possible to add the definition of new units
 * with their equivalence via the {@link #addSymbol} method.
 * <P>
 * The class can also deal with units expressed in <em>log scale</em> like
 * <b>[km/s]</b> equivalent to <em>log(km/s)</em>, or in <em>mag scale</em>
 * which is defined as <em>-2.5log</em>. Values may be transformed between the
 * linear and log scales with the <b>log</b>, <b>mag</b> and <b>dexp</b>
 * methods; conversions may also be performed via the <b>convert</b> methods.
 * <P>
 * The operations on units include the basic operations <b>plus</b>, 
 * <b>minus</b>, <b>mult</b>, <b>div</b> and <b>power</b> and 
 * the square root <b>sqrt</b>.  Addition or multiplications on physical values
 * would however better use the <b>sum</b> or <b>prod</b> methods, which take
 * care of converting the log scales. To illustrate the differences, assuming
 * that <em>u1</em> and <em>u2</em> have the same content of <tt>5mag</tt>,
 * the operation <tt>u1.add(u2)</tt> gives the result <tt>10mag</tt>,
 * while <tt>u1.sum(u2)</tt> gives a value around <tt>4.25mag</tt>
 * corresponding to the brightness of a source merging the two brightnesses.
 * <P>
 * For repetitive operations, like working on values from a table, it is
 * suggested to use the <b>setValue</b> method which assigns the value
 * but keeps the unit symbol and definition.
 * <P>
 * The numbers with their associated unit are edited as a single word
 * (without embedded blanks); the SI equivalent may be edited with the
 * <b>toStringInSI</b> method.
 * The numbers in their edited form use an exponential notation 
 * <em>m</em><tt>x10+</tt><em>n</em>; the classical form 
 * <em>m</em><tt>E+</tt><em>n</em> is also correctly interpreted.
 * Values in sexagesimal notation are also interpreted correctly in
 * <b>setValue</b> provided their associated unit is <tt>"h:m:s"</tt> (time)
 * or <tt>"d:m:s"</tt> (angle) --- note that the quotes <tt>"</tt> around the
 * unit symbol are required!
 * <P>
 * An example of a program reading data all with the same <em>km/s</em> unit
 * could be:
 * <PRE>
 * Unit aUnit = new Unit("km/s") ; 
 * while (true) {
 *     aUnit.setValue(stdin.readLine()) ;
 *     System.out.println("Complete Value: " + aUnit);
 *     System.out.println(" SI Equivalent: " + aUnit.toStringInSI());
 *     System.out.println("  Unit Meaning: " + aUnit.explainUnit());
 * }
 * </PRE>
 * 
 * <P>The recursive (BNF) definition of a <b>Unit</b> is as follows:
 * <PRE>
 * Full_Unit = factor Complex_unit
 * Complex_Unit = single_Unit
 * 	        | single_Unit OP single_Unit
 * 	        | "log[" single_Unit OP single_Unit "]"
 * 	        | "mag[" single_Unit OP single_Unit "]"
 * single_Unit  = extended_UnitSymbol
 * 	        | extended_UnitSymbol power
 * extended_UnitSymbol = UnitSymbol
 * 	   	       | Magnitude_Prefix UnitSymbol
 * 		       | "(" Full_Unit ")"
 * power = Number
 * 	 | +Number
 * 	 | -Number
 * OP	 = . | /
 * </PRE>
 *
 */

public class Unit {
  @SuppressWarnings("unchecked")
  /** The value, expressed in unit <B>symbol</B> */
  public double value;		// The value expressed in "symbol" unit
  /** The symbolic unit, like <em>km/s</em> */
  public String symbol;		// The symbol for this Unit
  /* protected String explain; */ /* Detailed explanation of the unit */
  /** Exponents in 8 units "mag", "kg", "m", "s", "A", "K", "cd", "mol" */
  private long mksa;		// Exponents in mag kg m s A K cd mol
  protected double factor;	// Factor to convert "symbol" into "SI"
  protected double offset;	// Shift from SI value (used also in log)

  //  ===========================================================
  //			Constants
  //  ===========================================================
  /** Definitions of the SI basic units */

  /* The exponent in each component of SI is stored in a 6-bit (2 octal 
   * digits), the 'zero' being represented by the _e0 (32=040) value.
   * except for the Magnitude where it's represented by 02.
   * The sign (leftmost) bit of the MKSA indicates a log scale
   * of the unit; associated to the 'mag' bit (_mag), this
   * indicates a 'mag' scale (-2.5log)
  */
  static final char[]  x = "0123456789abcdef".toCharArray();
  static boolean initialized = false;
  // static private byte accuracy = 8;	// Number of Significant Digits
  static int DEBUG = 0;
  static final private byte _e0 = 0x30 ;		// Unitless MKSA
  static final private byte _m0 = 0x02 ;		// Unitless mag
  static final private long _    = 0x0230303030303030L;	// Unitless
  static final private long _LOG = 0x8100000000000000L;	// Mag or Log (~02)
  static final private long _log = 0x8000000000000000L;	// Log scale
  static final private long _mag = 0x0100000000000000L;	// Mag scale
  static final private long _MJD = 0x4230303130303030L;	// Absolute (date)
  static final private long _pic = 0x2000000000000000L;	// Use picture
  static final private long _sex = 0x1000000000000000L;	// Use Sexagesimal
  static final private long _abs = 0x4000000000000000L;	// Absolute flag
  static final Editing editing = new Editing("-- ");	// Default NULL
  // static private double scaling = 10.e-8;
  static private Hashtable hUnit = 
             new Hashtable(149);
  static private ArrayList aUnit = null;		// Additional Units
  static private Hashtable hConv = new Hashtable(31);	// Special conversions
  /** The list of basic symbols */
  static final private String[] MKSA = {
     "mag",               "kg", "m", "s", "A", "K", "cd", "mol"
  };
  static final private char[] MKSAdim = {
      Character.MIN_VALUE, 'M', 'L', 'T', 'A', 'K', 
      Character.MIN_VALUE, Character.MIN_VALUE };
  static final private String[] mul_symb = {
	"mu",		"da",		"k",		"m",
	"c",		"u",		"M",		"n",
	"G",		"d",		"h",		"p",
	"T",		"f",		"P",		"a",
	"E",		"z",		"Z",		"y",
	"Y",		"\u00B5"
  } ;
  static final private String[] mul_text = {
	"micro",	"deca",		"kilo",		"milli",
	"centi",	"micro",	"mega",		"nano",
	"giga",		"deci",		"hecto",	"pico",
	"tera",		"femto(10-15)",	"peta(10+15)",	"atto(10-18)",
	"exa(10+18)",	"zepto(10-21)",	"zetta(10+21)",	"yocto(10-24)",
	"yotta(10+24)",	"micro"
  } ;
  static final private int[] mul_fact = {
	-6,		1,		3,		-3,
	-2,		-6,		6,		-9,
	9,		-1,		2,		-12,
	12,		-15,		15,		-18,
	18,		-21,		21,		-24,
	24,		-6
  } ;

  static final private String[] op_symb = {
	"2",		"3",		"+",		"-",	
	"/",		".",		"*",		" "
  } ;
  static final private String[] op_text = {
	"square ",	"cubic ",	"power+",	"power-",	
	"per ",		"times ",	"times ",	"times "
  } ;

  /* WARNING: index of "mag" in the following array must be >= 4 */
  static final private String[] log_symb = {
   	"log(",		"log[",		"[", 		"dex",
	"mag(",		"mag["
  } ;
  static final private char[]   log_end = {
  	')',		']',		']',	Character.MIN_VALUE,
	')',		']'
  };

  //  ===========================================================
  //                    Definitions of all symbols
  //		(generated from  "gawk -f unit.awk")
  //  ===========================================================

  /*** List of ALL Symbols ***/

  static final private Udef[] uDef = { 
    new Udef("---"        , "",
			     0x0230303030303030L, 1.0),
    new Udef("%"          , "percent",
			     0x0230303030303030L, 1.e-2),
    new Udef("h"          , "hour ",
			     0x0230303130303030L, 3.6e3),
    new Udef("min"        , "minute ",
			     0x0230303130303030L, 60.),
    new Udef("s"          , "second ",
			     0x0230303130303030L, 1.0),
    new Udef("mag"        , "magnitude ",
			     0x0330303030303030L, 1.0),
    new Udef("Jy"         , "Jansky(10-26W/m2/Hz) ",
			     0x0231302e30303030L, 1.e-26),
    new Udef("deg"        , "degree ",
			     0x0230303030303030L, (1./360.)),
    new Udef("rad"        , "radian ",
			     0x0230303030303030L, (0.5/Math.PI)),
    new Udef("sr"         , "steradian ",
			     0x0230303030303030L, (0.25/Math.PI)),
    new Udef("arcmin"     , "minute of arc ",
			     0x0230303030303030L, (1./21600.)),
    new Udef("arcsec"     , "second of arc ",
			     0x0230303030303030L, (1.e-3/1296.)),
    new Udef("mas"        , "milli-second of arc ",
			     0x0230303030303030L, (1.e-6/1296.)),
    new Udef("uas"        , "micro-second of arc ",
			     0x0230303030303030L, (1.e-9/1296.)),
    new Udef("Sun"        , "Solar unit ",
			     0x0230303030303030L, 1.0),
    new Udef("Msun"       , "solar mass ",
			     0x0231303030303030L, 1.989e+30),
    new Udef("solMass"    , "solar mass ",
			     0x0231303030303030L, 1.989e+30),
    new Udef("Rsun"        , "solar radius ",
			     0x0230313030303030L, 6.9599e+8),
    new Udef("solRad"     , "solar radius ",
			     0x0230313030303030L, 6.9599e+8),
    new Udef("Lsun"        , "solar luminosity ",
			     0x0231322d30303030L, 3.826e+26),
    new Udef("solLum"     , "solar luminosity ",
			     0x0231322d30303030L, 3.826e+26),
    new Udef("Mgeo"       , "Earth mass ",
			     0x0231303030303030L, 5.976e+24),
    new Udef("geoMass"    , "Earth mass ",
			     0x0231303030303030L, 5.976e+24),
    new Udef("Mjup"       , "Jupiter mass ",
			     0x0231303030303030L, 1.902e+27),
    new Udef("jovMass"    , "Jupiter mass ",
			     0x0231303030303030L, 1.902e+27),
    new Udef("m"          , "metre ",
			     0x0230313030303030L, 1.0),
    new Udef("Hz"         , "Herz ",
			     0x0230302f30303030L, 1.0),
    new Udef("kg"         , "kilogram ",
			     0x0231303030303030L, 1.0),
    new Udef("g"          , "gram ",
			     0x0231303030303030L, 1.e-3),
    new Udef("K"          , "Kelvin ",
			     0x0230303030313030L, 1.0),
    new Udef("Pa"         , "Pascal ",
			     0x02312f2e30303030L, 1.0),
    new Udef("T"          , "Tesla ",
			     0x0231302e2f303030L, 1.0),
    new Udef("V"          , "Volt ",
			     0x0231322d2f303030L, 1.0),
    new Udef("W"          , "Watt ",
			     0x0231322d30303030L, 1.0),
    new Udef("J"          , "Joule ",
			     0x0231322e30303030L, 1.0),
    new Udef("eV"         , "electron-Volt ",
			     0x0231322e30303030L, 1.602177e-19),
    new Udef("Ry"         , "Rydberg(13.6eV) ",
			     0x0231322e30303030L, 21.798948e-19),
    new Udef("yr"         , "year ",
			     0x0230303130303030L, 31.5576e+6),
    new Udef("a"          , "year ",
			     0x0230303130303030L, 31.5576e+6),
    new Udef("d"          , "day ",
			     0x0230303130303030L, 86.4e+3),
    new Udef("AU"         , "astronomical unit ",
			     0x0230313030303030L, 1.49598e+11),
    new Udef("au"         , "astronomical unit ",
			     0x0230313030303030L, 1.49598e+11),
    new Udef("pc"         , "parsec ",
			     0x0230313030303030L, 3.0857e+16),
    new Udef("al"         , "light-year ",
			     0x0230313030303030L, .946053e+16),
    new Udef("JD"         , "Julian Date ",
			     0x0230303130303030L|_abs, 86400,-86400.*2400000.5),
    new Udef("pix"        , "pixel ",
			     0x0230303030303030L, 1.0),
    new Udef("ct"         , "count ",
			     0x0230303030303030L, 1.0),
    new Udef("ph"         , "photon ",
			     0x0230303030303030L, 1.0),
    new Udef("A"          , "Ampere ",
			     0x0230303031303030L, 1.0),
    new Udef("barn"       , "barn(10-28m2) ",
			     0x0230323030303030L, 1.e-28),
    new Udef("bit"        , "binary information unit ",
			     0x0230303030303030L, 1.0),
    new Udef("byte"       , "byte(8bits) ",
			     0x0230303030303030L, 1.0),
    new Udef("C"          , "Coulomb ",
			     0x0230303131303030L, 1.0),
    new Udef("D"          , "Debye (dipole)",
			     0x0230313131303030L, (1.e-29/3.)),
    new Udef("cd"         , "candela(lumen/sr) ",
			     0x0230303030303130L, 1.0),
    new Udef("F"          , "Farad ",
			     0x022f2e3432303030L, 1.0),
    new Udef("H"          , "Henry ",
			     0x0231322e2e303030L, 1.0),
    new Udef("lm"         , "lumen ",
			     0x0230303030303130L, (0.25/Math.PI)),
    new Udef("lx"         , "lux(lm/m2) ",
			     0x02302e3030303130L, (0.25/Math.PI)),
    new Udef("mol"        , "mole ",
			     0x0230303030303031L, 1.0),
    new Udef("N"          , "Newton ",
			     0x0231312e30303030L, 1.0),
    new Udef("Ohm"        , "Ohm(V/A) ",
			     0x0231322d2e303030L, 1.0),
    new Udef("S"          , "Siemens(A/V) ",
			     0x022f2e3332303030L, 1.0),
    new Udef("Wb"         , "Weber(V.s) ",
			     0x0231322e2f303030L, 1.0),
    new Udef("u"          , "atomic mass unit ",
			     0x0231303030303030L, 1.6605387e-27),
    new Udef("\u00B5as"   , "micro-second of arc ",
			     0x0230303030303030L, (1.e-9/1296.)),
    new Udef("\"d:m:s\""  , "degree arcminute arcsecond (sexagesimal angle from degree)",
			     0x0230303030303030L|_sex, (1./360.)),
    new Udef("\"h:m:s\""  , "hour minutes seconds (sexagesimal time from hours)",
			     0x0230303130303030L|_sex, 3600.0),
    new Udef("\"h:m\""    , "hour minutes (sexagesimal time from hours)",
			     0x0230303130303030L|_pic, 3600.0),
    new Udef("\"m:s\""    , "minutes seconds (sexagesimal time from minutes)",
			     0x0230303130303030L|_pic, 60.0),
    new Udef("\"hhmmss\"" , "hour minutes seconds (sexagesimal time without separator)",
			     0x0230303130303030L|_pic, 3600.0  ),
    new Udef("\"ddmmss\"" , "degree arcminute arcsecond (sexagesimal angle without separator)",
			     0x0230303030303030L|_pic, (1./360.)),
    new Udef("\"date\""   , "Fully qualified date",
			     0x0230303130303030L|_MJD, 86400.),
    new Udef("\"datime\"",  "Fully qualified date/time (ISO-8601)",
			     0x0230303130303030L|_MJD, 86400.),
    new Udef("\"YYYYMMDD\"" , "Fully qualified date (without separator)",
			     0x0230303130303030L, 86400.),
    new Udef("\"month\""  , "Month name or number (range 1..12)",
			     0x0230303030303030L, 86400.),
    new Udef("\"MM/YY\""  , "Month/Year(from 1900)",
			     0x0230303130303030L, 86400.),
    new Udef("\"MM/yy\""  , "Month/Year(from 2000 when yy<50)",
			     0x0230303130303030L, 86400.),
    new Udef("\"day\""    , "Day of month number",
			     0x0230303030303030L, 86400.),
    new Udef("pi"         , "pi(=3.14...)",
			     0x0230303030303030L, Math.PI),
    new Udef("\u03C0"     , "pi(=3.14...)",
			     0x0230303030303030L, Math.PI),
    new Udef("c"          , "c(speed_of_light)",
			     0x0230312f30303030L, 299792458.),
    new Udef("G"          , "G(gravitation constant)",
			     0x022f332e30303030L, 6.673e-11),
    new Udef("\\h"        , "hbar(Planck constant)",
			     0x0231322f30303030L, 1.0545716e-34),
    new Udef("\u210F"     , "hbar(Planck constant)",
			     0x0231322f30303030L, 1.0545716e-34),
    new Udef("e"          , "e(electron_charge) ",
			     0x0230303131303030L, 1.602177e-19),
    new Udef("k"          , "k(Boltzmann) ",
			     0x0231322e302f3030L, 1.38065e-23),
    new Udef("R"          , "R(gas_constant) ",
			     0x0231322e302f302fL, 8.3143),
    new Udef("mp"         , "mp(proton_mass) ",
			     0x0231303030303030L, 1.672661e-27),
    new Udef("me"         , "me(electron_mass) ",
			     0x0231303030303030L, 9.109382e-31),
    new Udef("a0"         , "(Bohr radius) ",
			     0x0230313030303030L, 5.29177208e-11),
    new Udef("eps0"       , "(electric constant) ",
			     0x022f2d3432303030L, 8.854187817620389e-12),
    new Udef("mu0"        , "(magnetic constant) ",
			     0x0231312e2e303030L, (4.e-7*Math.PI)),
    new Udef("\u00B50"    , "(magnetic constant) ",
			     0x0231312e2e303030L, (4.e-7*Math.PI)),
    new Udef("alpha"      , "(fine structure constant) ",
			     0x0230303030303030L, (1./137.036)),
    new Udef("muB"        , "(Bohr magneton) ",
			     0x0230323031303030L, /* e.hbar/2me */9.274009e-28),
    new Udef("degC"       , "Celsius ",
			     0x0230303030313030L, 1.0, 273.15),
    new Udef("MJD"        , "Modified Julian Date (JD-2400000.5) ",
			     0x0230303130303030L|_abs, 86.4e+3),
    new Udef("atm"        , "atmosphere ",
			     0x02312f2e30303030L, 101325.),
    new Udef("mmHg"       , "mercury_mm ",
			     0x02312f2e30303030L, 133.3224),
    new Udef("l"          , "litre ",
			     0x0230333030303030L, 1.e-3),
    new Udef("hr"         , "hour(use 'h') ",
			     0x0230303130303030L, 3.6e3),
    new Udef("sec"        , "second (use 's')",
			     0x0230303130303030L, 1.0),
    new Udef("inch"       , "inch ",
			     0x0230313030303030L, 2.54e-2),
    new Udef("t"          , "ton ",
			     0x0231303030303030L, 1.e+3),
    new Udef("\"month\""  , "month ",
			     0x0230303030303030L, 1.0),
    new Udef("erg"        , "erg(10-7J) ",
			     0x0231322e30303030L, 1.e-7),
    new Udef("dyn"        , "dyne(10-5N) ",
			     0x0231312e30303030L, 1.e-5),
    new Udef("bar"        , "bar(10+5Pa) ",
			     0x02312f2e30303030L, 1.e+5),
    new Udef("gauss"      , "Gauss(10-4T) ",
			     0x0231302e2f303030L, 1.e-4),
    new Udef("cal"        , "calorie ",
			     0x0231322e30303030L, 4.1854),
    new Udef("\u00C5",      "Angstroem(0.1nm) ",
			     0x0230313030303030L, 1.e-10),
    new Udef("Angstrom"   , "Angstroem(0.1nm) ",
			     0x0230313030303030L, 1.e-10),
    new Udef("lyr"        , "light-year (c*yr) ",
			     0x0230313030303030L, .946053e+16),
    new Udef("degF"       , "Fahrenheit ",
			     0x0230303030313030L, (5./9.), (273.15-(32.*5./9.))),
};

    //  ===========================================================
    //                    Initialisation
    //  ===========================================================
    private static synchronized void init() {
	int deb = DEBUG; DEBUG=0;
        for (int i=0; i < uDef.length; i++) 
          hUnit.put(uDef[i].symb, new Integer(i)) ;
	// Accept conversions d:m:s <-> h:m:s
        initialized = true;
	Converter hms2dms = new Converter("\"h:m:s\"", "\"d:m:s\"", 15.);
	Converter dms2hms = new Converter("\"d:m:s\"", "\"h:m:s\"", 1./15.);
	DEBUG=deb;
    }

    /*
     **** REMOVED --- no accuracy defined here.
     ** Change the default number of significant digits for Editions (8)
     * @param	digits the number of significant digits
     * @return	the previously defined number of significant digits
    ** 
    public static final int setAccuracy(int digits) {
      int old_value = accuracy;
	if (!initialized) init();
        if (digits < 2) accuracy = 2;
	else if (digits > 16) accuracy = 16;
	else accuracy = (byte)digits;
	scaling = AstroMath.dexp(1-accuracy);
	return(old_value);
    }
    */

    /*==================================================================
			Management of the List of Units
     *==================================================================*/

  /** Retrieve one definition of the units from its symbol
   * @param	symbol the symbol to find out
   * @return	its object
  **/
    private static final Udef uLookup (String symbol) {
      int i;
      Udef u;
	if (!initialized) init();
        Object o = hUnit.get(symbol) ;	// hUnit may contain aliases
	if (o instanceof String) o = hUnit.get((String)o);
        if (o instanceof Integer) ; else return(null);
	i = ((Integer)o).intValue();
	if (i < uDef.length) 		// In basic list
	    return(uDef[i]) ;
	i -= uDef.length;		// Is in additional list
	u = (Udef)aUnit.get(i);
	return(u);
    }

  /** Retrieve one definition of the units from its dimension
   * @param	symbol the symbol to find out
   * @return	its object
  **/
    private static final Udef uLookup (long mksa) {
      int i;
      Udef u = null;
	if (!initialized) init();
	// Among the possible units, return the one without offset, and
	// if possible having a factor of 1.
	for (i=0; i<uDef.length; i++) {
	    if ((uDef[i].mksa == mksa) && (uDef[i].orig == 0)) {
		if (uDef[i].fact == 1) return(uDef[i]);
		if (u == null) u = uDef[i];
	    }
	}
	if (u != null) return(u);
	// Maybe in additinal list ?
        if (aUnit != null) { 
	    int n = aUnit.size();
	    for(i=0; i<n; i++) {
		u = (Udef)aUnit.get(i);
		if (u.mksa == mksa) return(u);
	    }
	}
	return(u);
    }

  /** Retrieve one definition of the units from the uDef table
   * @param	t text to lookup
   * @param len length of the text
   * @return	its object
  **/
    private static final Udef uLookup (Parsing t, int len) {
      String symb = new String(t.a, t.pos, len);
      Udef u = uLookup(symb);
        if (u != null) t.pos += len;
	return(u);
    }

  /** 
   * List all symbols.
   * Basic symbols only (without the multiplicity indexes) are listed.
   * Use {@link #checkSymbol} to verify one symbol.
   * @return	an Enumeration of the symbols. 
   *		The symbols can be used as argument in the <b>explain</b>
   *		method to get details.
  **/
    public static final Enumeration symbols() {
	if (!initialized) init();
	return(hUnit.keys());
    }

  /** Introduce a new Symbol into the list of Symbols.
   * An example could be 
   * <tt>Unit.addSymbol("Rayleigh", "10+6/(4*pi).ph.cm-2.s-1.sr-1", 
   * 		"apparent emission rate")</tt>
   *
   * @param	symbol the text of the symbol -- made of <em>letters</em> only,
   *		or letters preceded by a backslash, or surrounded by quotes
   * @param	equiv the definition of the symbol in terms of existing symbols
   * @param	explain the text to be used for explanation 
   *		<em>(should be terminated by a blank)</em>
   * @return	The unit having the same symbol which was removed, or null
   * @throws	ParseException when <em>equiv</em> can't be interpreted.
  **/
  @SuppressWarnings("unchecked")
    public static synchronized Unit addSymbol (String symbol, String equiv, 
      String explain) throws ParseException {
      Udef old_def, new_def;
      Unit new_unit, old_unit;
      int i;

	// if (!initialized) init(); -- executed in uLookup
	old_def = uLookup(symbol) ; 
	if (old_def == null) old_unit = null;
	else old_unit = new Unit(old_def.symb);
	new_unit = new Unit(equiv);

	if (DEBUG>0) System.out.println("----addSymbol(" + symbol + ") = " 
		+ equiv + " = " + new_unit);

	// Create a vector of additional units if not existing
	if (aUnit == null) aUnit = new ArrayList(16);

	// Transform the unit into a new definition
	if (!Double.isNaN(new_unit.value)) {
	    if ((new_unit.mksa&_LOG) != 0) {	// Convert the log scale
	      Unit u = new Unit(new_unit);
	        u.dexp();
		new_unit.factor = u.factor * u.value;
	    }
	    else new_unit.factor *= new_unit.value;
	}
	new_def = new Udef(symbol, explain, new_unit.mksa, new_unit.factor);

	// Add the new unit into the ArrayList, and into the Hashtable
	i = aUnit.size() + uDef.length;		// Index of new unit for Htable
	aUnit.add(new_def);
	hUnit.put(symbol, new Integer(i));

	if (DEBUG>0) System.out.println("====Added units #" + i + " = " 
		+ new_def);

	// Return the old Explanation
	return(old_unit);
    }

  /** Introduce a new Symbol into the list of Symbols.
   * @param	symbol the text of the symbol -- made of <em>letters</em> only,
   *		or letters preceded by a backslash, or surrounded by quotes
   * @param	equiv the definition of the symbol in terms of existing symbols
   * @return	The text of the explanation of the unit previously defined 
   *		having the same symbol
   * @throws	ParseException when <em>equiv</em> can't be interpreted.
  **/
    public static synchronized Unit addSymbol (String symbol, String equiv) 
      throws ParseException {
	return(addSymbol(symbol, equiv, equiv + " ")) ;
    }

  /** 
   * Introduce a special converter.
   *	Converters created by the {@link Converter} class 
   *	are automatically registered (no need to register them).
   *	Once registered, the converter is used by {@link #convert},
   *	{@link #convertFrom} and {@link #convertTo} preferably to
   *	the standard conversion rules.
   * @param	source_unit symbol of source unit, e.g. <tt>mag</tt>
   * @param	target_unit symbol of target unit, e.g. <tt>Jy</tt>
   * @param	special_converter a {@link Converter} object
  **/
    public static synchronized void registerConverter (String source_unit, 
	    String target_unit, Converter special_converter) {
	String symbols = source_unit +  Converter.SEP + target_unit;
	hConv.put(symbols, special_converter);
    }

  /** 
   * Retrieve a special converter.
   * 	(previously registered by {@link #registerConverter})
   * @param	source_unit symbol of source unit, e.g. <tt>"d:m:s"</tt>
   * @param	target_unit symbol of target unit, e.g. <tt>"h:m:s"</tt>
   * @return	The known {@link Converter} object.
   * 		Object which know how to convert <em>source_unit</em> into 
   * 		<em>target_unit</em>
  **/
    public static Converter getConverter (String source_unit, 
	    String target_unit) {
	String symbols = source_unit +  Converter.SEP + target_unit;
	Converter c = (Converter)hConv.get(symbols);
	return(c);
    }

  /** 
   * Check whether a symbol exists.
   * Use {@link #symbols} to get all symbols.
   * @param	symbol the text of the symbol -- made of <em>letters</em> only,
   *		or letters preceded by a backslash, or surrounded by quotes
   * @return	<b>true</b> if the symbol exists, <b>false</b> if the
   * 		symbol was not registered.
  **/
    public static boolean checkSymbol (String symbol) {
	return(uLookup(symbol)!=null) ;
    }

    /*==================================================================
			Constructors
     *==================================================================*/

    /** Define the default unit: <em>unitless</em> undefined value 
    **/
    public Unit() {
	this.set();
    }

    /** Define a Unit and Value from an existing one (clone)
     * @param	u the Unit to clone
    **/
    public Unit(Unit u) {		// Just copy the instance
	this.set(u);
    }

    /** Define a Value+Unit by interpreting a String
     * @param	symbol a text to interpret, e.g. 20km/s, or 2(75km/s/Mpc)
     * @throws  ParseException when the argument contains non-interpretable
     * 		text
    **/
    public Unit(String symbol) throws ParseException {
        this.set(symbol) ;
    }

    /*==================================================================
		Get the contents of a Unit
     *==================================================================*/

    /** 
     * Get the value of a unit (identical to unit.value)
     * @return	the value, expressed in units <b>getUnit()</b>
    **/
    public double getValue() {
	return(value);
    }

    /** 
     * Get the value converted to SI system
     * @return	the value, expressed in units <b>getSIunit()</b>
    **/
    public double getSIvalue() {
	return(value*factor + offset);
    }

    /** 
     * Get a representation of the unit dimension.
     *     The result is unique for a combination of basic dimensions.
     * @return	the value
    **/
    public long getSIdim() {
	return(mksa);
    }

    /** 
     * Get a representation of the unitless dimension
     * @return	the representation of a unitless dimension.
    **/
    static public long unitless() {
	return(_);
    }

    /** 
     * Get the unit symbol (identical to unit.symbol)
     * @return	the symbol representing the unit
    **/
    public String getUnit() {
	return(symbol);
    }

    /** 
     * Get the unit expressed in terms of SI units.
     * @return	the symbol representing the unit
    **/
    public String getSIunit() {
      StringBuffer b = new StringBuffer(64);
	toSI(b, 0);
	return(""+b);
    }

    /*==================================================================
		Very Basic Utilities
     *==================================================================*/
    /** Add the () to transform an expression into a simple expression
     *  The () are added only if the string contains non-letter.
     * @param s string to transform
    **/
    private static final String toExpr(String s) {
      char[] b = s.toCharArray();
      int i;
	for (i=0; (i<b.length) && Character.isLetter(b[i]); i++) ;
	if (i<b.length) return("(" + s + ")");
	return(s) ;		// Just letters
    }

    /** Remove the enclosing parentheses in a StringBuffer.
     *  Changes the length of the buffer, and returns
     *  the position of the first significant character.
     * @param b buffer to modify
    **/
    private static final int skipBrackets(StringBuffer b) {
      int len1 = b.length()-1;
      int ini1 = 0;
	if (len1 <= 0) return(0);
	while ((b.charAt(ini1) == '(') && (b.charAt(len1) == ')')) {
	    while (b.charAt(--len1) == ' ');
	    while (b.charAt(++ini1) == ' ');
	    b.setLength(++len1);
	}
	return(ini1);
    }

    /*==================================================================
		Operations on a single Unit
     *==================================================================*/

    /** Compute the power of the text, from (...)+/-p or symb+/-p
     *  @return as an int the power.
     *  Remark 1: set the `position' at the location where the power starts
     *  Remark 2: no change when the returned power is 0.
    **/
    private static final int getPower(Parsing t) {
      char s='+';		// Exponent Sign
      int pow=0;		// Exponent Value
      int inum=0;		// Index of exponent
      int posini=t.pos;
	//System.out.print("....getPower on '" + this + "', pos=" + pos + ": ");
	t.pos = t.length-1;
	if (Character.isDigit(t.a[t.pos])) {
	    while (Character.isDigit(t.a[t.pos])) --t.pos;
	    s = t.a[t.pos];
	    if ((s == '+') || (s == '-')) ;	// Exponent starts here
	    else ++t.pos;
	    inum = t.pos;
	    pow = t.parseInt();
	//System.out.print("found pow=" + pow + ",pos=" + pos);
	}
	if (pow != 0) {		// Verify the power is meaningful
	    if (t.a[0] == '(') {
	        if (t.a[inum-1] != ')') pow = 0;
	    }
	    else {		// could be e.g. m2
	        for (int i=0; i<inum; i++) {
	    	// Verify the unit is not a complex one
	            if (!Character.isLetter(t.a[i])) pow = 0;
	        }
	    }
	}
	if (pow == 0) t.pos = posini;
	else t.pos = inum;
	//System.out.println(" pow=" + pow + ", pos=" + pos);
	return(pow);
    }

    /** Convert a value to a decimal log scale
     * @throws ArithmeticException when the unit contains already logs
    **/
    public final void log() throws ArithmeticException {
	if ((mksa&_LOG) != 0) throw new ArithmeticException
	    ("****Unit: log(" + symbol + ")") ;
	value = AstroMath.log(value);
	mksa |= _log;
	if (symbol != null) 
	    symbol = "[" + symbol + "]";	// Enough to indicate log
    }

    /** Convert a value to a mag scale
     * @throws  ArithmeticException when the unit contains already logs
    **/
    public final void mag() throws ArithmeticException {
	if ((mksa&_LOG) != 0) throw new ArithmeticException
	    ("****Unit: mag(" + symbol + ")") ;
	value = -2.5*AstroMath.log(value);
	if ((mksa == _) && (factor == 1)) {	// Now we've mag
	    mksa |= _mag;
	    if (symbol != null) symbol = "mag";
	} else {
	    mksa |= (_log|_mag);
	    if (symbol != null) symbol = "mag[" + symbol + "]";
	}
    }

    /** Convert a value to its linear scale
     * @throws  ArithmeticException when the unit is not a log
    **/
    public final void dexp() throws ArithmeticException {
      double ls; char c; int i, last;
        if ((mksa&_log) == 0) throw new ArithmeticException 
            ("****Unit: dexp("  + symbol + ")") ;
	ls = (mksa&_mag) != 0 ? -2.5 : 1.;
        value = AstroMath.dexp(value/ls);
	mksa &= (~_LOG) ;
	if (symbol == null) return;

      	// Change the symbol
      	i = -1; last = symbol.length()-1;
      	c = symbol.charAt(last);		// Closing bracket
	if (c == ']') i = symbol.indexOf('[');
	else if (c == ')') i = symbol.indexOf('(');
	if (i >= 0) symbol = symbol.substring(i+1, last);
	else { 					// Symbol can't be found, use SI
	  StringBuffer b = new StringBuffer(64);
	    toSI(b, 0);
	    symbol = "" + b;
	}
    }

    /** Compute the Power of a number/unit
     * @param	expo Power value
     * @throws  ArithmeticException when the power is too large
    **/
    public final void power (int expo) throws ArithmeticException {
      double error = 0;
      int i = expo; 
      double r = 1;
      double v = 1; 
      long u = _;
    	if ((mksa&(_log|_mag)) != 0) throw new ArithmeticException
	    ("****Unit: can't power log[unit]: " + symbol );
    	while (i > 0) { 
	    r *= factor; v *= value; 
	    u += mksa; u -= _;  
	    if ((u&0x8480808080808080L) != 0) error++;
	    i--; 
	}
    	while (i < 0) { 
	    r /= factor; v /= value; 
	    u += _; u -= mksa;
	    if ((u&0x8480808080808080L) != 0) error++;
	    i++; 
	}
	if (error > 0) throw new ArithmeticException
	   ("****Unit: power too large: (" + ")^" + expo) ;
    	factor = r;
	value = v;
	mksa = u;
	/* Decision for the new symbol */
	if ((expo != 1) && (symbol != null)) {
	    if (expo == 0) symbol = "";
	    else if (symbol.length()>0) {
	      Parsing t = new Parsing(symbol);
	      int pow;
	        pow = getPower(t);
		if (pow == 0) symbol = toExpr(symbol) + expo;
		else {			// Combine exponents
		    i=0;
		    pow *= expo;
		    if (t.a[0] == '(') { i = 1; t.advance(-1); }
		    if (expo == 1) symbol = symbol.substring(i,t.pos);
		    else symbol = toExpr(symbol.substring(i,t.pos)) + pow;
		}
	    }
	}
    } 

    /** Take Square Root of a Value/Unit
     * @throws  ArithmeticException when the unit is not a log, or not-even
    **/
    public final void sqrt() throws ArithmeticException {
	if ((mksa&_LOG) != 0) throw new ArithmeticException
	    ("****Unit: log(" + symbol + ")") ;
	if ((mksa&0x0101010101010101L)!=0) throw new ArithmeticException
	    ("****Unit: sqrt(" + symbol + ") is impossible");
	value = Math.sqrt(value);
	factor = Math.sqrt(factor);
	mksa = (mksa+_)>>1;
	/* Try to remove the squared edition */
	if ((symbol != null) && (symbol.length()>1)) {
          Parsing t = new Parsing(symbol);
	  int i=0;
	  int pow=0;
	    pow = getPower(t);
	    if ((pow&1) != 0) 	// Odd power not possible
	      pow = 0;
	    if (t.a[0] == '(') { i = 1; t.advance(-1); }
	    if (pow == 2) symbol = symbol.substring(i, t.pos);
	    else if (pow != 0) {
		pow /= 2;
		symbol = toExpr(symbol.substring(i,t.pos)) + pow;
	    }
	    if ((pow == 0) && (t.length>0)) symbol = "sqrt(" + symbol + ")";
	}
    }

    /*==================================================================
		Conversion of a Unit into another one
     *==================================================================*/

    /** Checks whether a number has no unit (is unitless)
     * @return	true if unit has no associated dimension
    **/
    public final boolean isUnitless () {
	return((mksa&(~_log)) == _);
    }

    /** 
     * Checks whether 2 units are compatible.
     * Compatible units can be summed via the <b>sum</b> method, as e.g.
     * <b>m/s</b> and <b>[km/h]</b>
     * @param	unit another Unit, to be verified.
    **/
    public final boolean isCompatibleWith (Unit unit) {
	if (((unit.mksa|this.mksa)&_abs) != 0) 		// Special dates
	    return((this.mksa^_abs) == unit.mksa) ;
	return((mksa&(~(_log|_mag))) == (unit.mksa&(~(_log|_mag)))) ;
    }

    /** 
     * Check whether 2 quantities are equal.
     * Two units are equal when these have the same physical dimension, and
     * have the same numeric value
     * (e.g. 100cm and 1m should be equal).
     * @param   o any object
     * @return  true if identical values.
    **/
    public final boolean equals(Object o) {
	boolean res = false;
	if(o instanceof Unit) {
	    Unit u = (Unit)o;
	    if (u.mksa == this.mksa) res = (mksa&_log) != 0 ? 
		this.factor == u.factor && this.value == u.value :	// LOG
		this.factor*this.value == u.factor*u.value ;	// linear
	}
	return(res);
    }

 	/**
 	 * Compute the hashcode
 	 * @return the hascode value
 	 */
 	public int hashCode() {
 		long l = Double.doubleToLongBits(value);
 		int hcode = (int) (l^(l >>> 32));
 		l = Double.doubleToLongBits(factor);
 		hcode = hcode * 123 + (int) (l^(l >>> 32));
 		hcode = hcode * 123 + (int) (mksa^(mksa >>> 32));
 		return hcode;
 	}

    /** 
     * Convert a unit+value into another unit+value.
     * 		The conversion uses first the registered conversions
     * 		(via {@link #registerConverter})
     * @param	source_unit the value+unit to convert.
     *		This value is unchanged.
     * @param	target_unit the resulting value+unit.
     *		On return, its <tt>value</tt> part is modified.
     * @throws  ArithmeticException when source_unit is not compatible with
     * 		target_unit
    **/
    public static void convert(Unit source_unit, Unit target_unit) 
    throws ArithmeticException {
      String s = source_unit.symbol + Converter.SEP + target_unit.symbol;
      Object o = hConv.get(s);
      	if (o != null) {
	    Converter c = (Converter)o;
	    double r = c.convert(source_unit.value);
	    target_unit.value = r;
	    return;
      	}
      	convertUnit(source_unit, target_unit);
    }

    /** 
     * Convert a unit+value into another unit+value.
     * 		The conversions registered via {@link #registerConverter}
     * 		are <b>not</b> used.
     * @param	source_unit the value+unit to convert.
     *		This value is unchanged.
     * @param	target_unit the resulting value+unit.
     *		On return, the <tt>value</tt> part is modified.
     * @throws  ArithmeticException when source_unit is not compatible with
     * 		target_unit
    **/
    public static void convertUnit(Unit source_unit, Unit target_unit) 
    throws ArithmeticException {
      double f = source_unit.factor/target_unit.factor;
      double ls;

        if (DEBUG>0) {
	    source_unit.dump("...convert:source="); 
	    target_unit.dump("...convert:target="); 
	}

	if (target_unit.mksa == source_unit.mksa) {
	    /*if ((target_unit.mksa&_log) == 0) */
		target_unit.value = f*source_unit.value 
		+ (source_unit.offset-target_unit.offset)/target_unit.factor;
	    /*else {	// Convert to log scale: log(km) --> log(m)+log(k)
		if ((source_unit.offset != 0) || (target_unit.offset != 0)) 
		    throw new ArithmeticException
		   ("****Unit: can't convert non-standard unit " 
		    + source_unit.symbol + " into " + target_unit.symbol);
		f = AstroMath.log(f);
	        if ((target_unit.mksa&_mag) != 0) f = -2.5*f;
		target_unit.value = source_unit.value + f;
	    }*/
	    return ;
	}

	if ((target_unit.mksa&(~_LOG)) != (source_unit.mksa&(~_LOG))) {	
	    throw new ArithmeticException
	    ("****Unit: can't convert " + source_unit.symbol 
	                     + " into " + target_unit.symbol);
	}

	/* Convert to / from Log scale */
	if ((target_unit.mksa&_log) == 0) {		// Remove the log
	    ls = (source_unit.mksa&_mag) != 0 ? -2.5 : 1.;
            target_unit.value = f*AstroMath.dexp(source_unit.value/ls);
	    return;
	}

	/* The target is a log. Check whether the source is also in log scale */
	if ((source_unit.mksa&_log) != 0) {	// Convert from log to log
	    target_unit.value = (source_unit.mksa&_mag) != 0 ? 
	        -0.4*source_unit.value : 	// From a magnitude
		source_unit.value;
	    target_unit.value += AstroMath.log(f);  // Target assumed to be log
	    if ((target_unit.mksa&_mag) != 0) 	// Target is a magnitude
	        target_unit.value *= -2.5;
	    return ;
	}

	/* The target is a log, the source is in a linear scale. */
	ls = (target_unit.mksa&_mag) != 0 ? -2.5 : 1.;
	target_unit.value = ls*AstroMath.log(f*source_unit.value);
    }

    /** 
     * Convert the value in argument to the target Unit.
     * @param	source_unit the value+unit to convert.
     *		This value is unchanged.
     * @throws  ArithmeticException when source_unit is not compatible with
     * 		the unit of <tt>this</tt>.
    **/
    public void convertFrom(Unit source_unit) throws ArithmeticException {
	convert(source_unit, this);
    }

    /** 
     * Convert the value into another unit.
     * @param	unit the target unit (unchanged)
     * 	On return, <tt>this</tt> contains the value in another unit.
     * @throws  ArithmeticException when the units are not compatible
    **/
    public void convertTo(Unit unit) throws ArithmeticException {
      double f = factor/unit.factor;

	if(DEBUG>0) {
	    this.dump("...convertTo:source= "); 
	    unit.dump("...convertTo:target= "); 
	}

	if ((mksa == unit.mksa) && ((mksa&_log) == 0)) {
	    	value  = (value*factor + offset - unit.offset)/unit.factor;
		factor = unit.factor;
		offset = unit.offset;
		symbol = unit.symbol;
		return;
	}
	// More complex transformation: Use convertFrom
	Unit temp = new Unit(this);
	symbol = unit.symbol;
	mksa   = unit.mksa;
	factor = unit.factor;
	offset = unit.offset;
	convertFrom(temp);
	return;
    }

    /*==================================================================
		Addition of 2 Units
     *==================================================================*/

    /** 
     * Addition of 2 quantities.
     * Compute the Addition of 2 numbers with their associated units.
     * @param	unit 2nd Unit, to add.
     * @throws  ArithmeticException when the units are not compatible
    **/
    public final void plus (Unit unit) throws ArithmeticException {
      boolean error = false;
        if (((mksa|unit.mksa)&_abs)!=0) {	// Special dates
	    if (mksa != (_abs^unit.mksa)) error = true;
	    else mksa |= _abs;			// Result is a date
	}
	else error = (mksa&(~_pic)) != (unit.mksa&(~_pic));
	if (error) throw new ArithmeticException
	    ("****Unit: can't combine: " + symbol + " + " + unit.symbol);
	/* Addition in log scale: a+b=a+f*log(1.+10^((b-a)/f)) */
	value += (unit.value*unit.factor)/factor;
    }

    /** 
     * Subtraction of 2 quantities.
     * Compute the Subtraction of 2 numbers with their associated units.
     * @param	unit 2nd Unit to be subtracted
     * @throws  ArithmeticException when the units are not compatible
    **/
    public final void minus (Unit unit) throws ArithmeticException {
      boolean error = false;
	if (((mksa|unit.mksa)&_abs)!=0) {	// Special dates
	    if (mksa == unit.mksa)		// date1 - date2
		mksa ^= _abs;			// ... not a date any more
	    else if ((unit.mksa&_abs)!=0) error = true;
	}
	else error = (mksa&(~_pic)) != (unit.mksa&(~_pic));
	if (error) throw new ArithmeticException
	    ("****Unit: can't combine: " + symbol + " - " + unit.symbol);
	value -= (unit.value*unit.factor)/factor;
    }

    /** 
     * Sum of 2 quantities.
     * Compute the Sum of 2 numbers with their associated units;
     * The difference with "plus" exists only for log scales or magnitudes:
     *     the resulting value corresponds to a physical sum (e.g. of fluxes),
     *     whereas the "plus" corresponds to an arithmetic sum.
     * @param	unit 2nd Unit 
     * @throws  ArithmeticException when the units are not compatible
    **/
    public final void sum (Unit unit) throws ArithmeticException {
      double ls, dv;
      int error = 0;
      long rlog, ulog;
      	if (((mksa|unit.mksa)&_LOG) == 0) { 	// No log scale at all !
	    this.plus(unit); 
	    return; 
	}
	if (DEBUG>0) { dump("...sum:term1"); unit.dump("...sum:term2"); }
	if ((mksa&(~_LOG)) != (unit.mksa&(~_LOG))) throw new ArithmeticException
	    ("****Unit: can't sum: " + symbol + " + " + unit.symbol);
	rlog = mksa&(_log|_mag);
	ulog = unit.mksa&(_log|_mag);
	if (rlog != ulog) {		// Not the Same log scale -- convert
	  Unit tunit = new Unit(this);
	    tunit.convertFrom(unit);	// Convert the argument unit to target.
	    this.sum(tunit);		// Now, the units are the same!
	    return;
	}
	// Here, we have the same log scales
	// if (rlog == 0) { this.plus(unit); return; } -- already seen
	ls = (rlog&_mag) != 0 ? -2.5 : 1.0 ;
	/* Addition in log scale: a+b=a+f*log(1.+10^((b-a)/f)) */
	dv = unit.value - value;
	value += ls*AstroMath.log(1.+(unit.factor/factor)*Math.exp(dv/ls)) ;
    }

    /*==================================================================
		Multiplication of 2 Units
     *==================================================================*/

    /** Multiplication by a Scalar.
     * @param	s Scalar value for the multiplication
     * @throws ArithmeticException when the units can't be combined
    **/
    public final void mult (double s) throws ArithmeticException {
	if (((mksa&_abs)!=0) && (s != 1)) throw new 	// On a date
	    ArithmeticException("****Unit.mult on a date!");
	value *= s;
	// Offset not changed.
    }

    /** 
     * Multiplication of 2 quantities.
     * Compute the arithmetic Multiplication of 2 numbers with their 
     * associated units.
     * The difference with <b>prod</b> exists only for log scales or magnitudes.
     * @param	unit 2nd Unit
     * @throws  ArithmeticException when the units can't be combined
    **/
    public final void mult (Unit unit) throws ArithmeticException {
      long u = mksa ; double r = factor; double v = value; double o = offset;
	/* dump("mult:this "); unit.dump("mult:unit"); */
        if (((mksa&_abs)!=0) && (unit.factor != 1)) throw	// On a date
	    new ArithmeticException("****Unit.mult on a date!");
        if (((mksa|unit.mksa)&_log) != 0) {
	    if ((mksa == _) && (factor == 1.)) ;
	    else if ((unit.mksa == _) && (unit.factor == 1.)) ;
	    else throw new ArithmeticException
	    ("****Unit: can't multiply logs: " + symbol + " x " + unit.symbol);
	}
        /* As soon as there is an operation, the offset is ignored 
	 * except if one of the factors is unity.
	 */
        if ((offset!=0) || (unit.offset!=0)) {
	    if (mksa == _) offset = unit.offset;
	    else if (unit.mksa == _) ;
	    else offset = 0;
	}
	v *= unit.value; 
	r *= unit.factor;
      	u += unit.mksa; u -= _;
	if ((u&0x0c80808080808080L) != 0) throw new ArithmeticException
	    ("****too large powers in: " + symbol + " x " + unit.symbol);
	mksa = u;
	factor = r;
	value  = v;
	/* Decision for the new symbol */
	if ((symbol != null) && (unit.symbol != null)) {
	    if ((unit.mksa == _) && (unit.factor == 1)) return;	// No unit ...
	    if ((     mksa == _) && (     factor == 1)) symbol = unit.symbol;
	    else if ((symbol.equals(unit.symbol)) && (factor == unit.factor))
		 symbol = toExpr(symbol) + "2" ;
	    else symbol = toExpr(symbol) + "." + toExpr(unit.symbol) ;
	}
    }

    /** 
     * Division of 2 quantities.
     * Compute the Division of 2 numbers with their associated units.
     * @param	unit 2nd Unit
     * @throws  ArithmeticException when the units can't be combined
    **/
    public final void div (Unit unit) throws ArithmeticException {
      long u = mksa ; double r = factor; double v = value;
        if (((mksa&_abs)!=0) && (unit.factor != 1)) throw	// On a date
	    new ArithmeticException("****Unit.div  on a date!");
        if (((mksa|unit.mksa)&_log) != 0) {
	    if ((mksa == _) && (factor == 1.)) ;
	    else if ((unit.mksa == _) && (unit.factor == 1.)) ;
	    else throw new ArithmeticException
	    ("****Unit: can't divide logs: " + symbol + " x " + unit.symbol);
	}
        /* As soon as there is an operation, the offset is ignored 
	 * except if one of the factors is unity.
	 */
        if ((offset!=0) || (unit.offset!=0)) {
	    if (mksa == _) offset = unit.offset;
	    else if (unit.mksa == _) ;
	    else offset = 0;
	}
	v /= unit.value; 
	r /= unit.factor; 
      	u += _; u -= unit.mksa;
	if ((u&0x8c80808080808080L) != 0) throw new ArithmeticException
	    ("****too large powers in: " + symbol + " / " + unit.symbol);
	mksa = u;
	factor = r;
	value  = v;
	/* Decision for the new symbol */
	if ((symbol != null) && (unit.symbol != null)) {
	    if ((unit.mksa == _) && (unit.factor == 1)) return;	// No unit ...
	    if ((     mksa == _) && (     factor == 1))
		symbol = toExpr(unit.symbol) + "-1";
	    else if (symbol.equals(unit.symbol)) symbol = edf(factor);
	    else symbol = toExpr(symbol) + "/" + toExpr(unit.symbol) ;
	}
    }

    /** 
     * Product of 2 quantities.
     * Compute the Product of 2 numbers with their associated units.
     * The difference with "mult" exists only for log scales or magnitudes:
     *     the resulting value corresponds to a physical product 
     *     whereas the "mult" corresponds to an arithmetic multiplication.
     * @param	unit 2nd Unit 
     * @throws  ArithmeticException when the units are not compatible
    **/
    public final void prod (Unit unit) throws ArithmeticException {
      Unit runit, tunit;

	if (((mksa&_log)==0) && ((unit.mksa&_log)==0)) {
            mult(unit); 
	    return; 
	}

        /* Convert to non-log Unit */
	runit = new Unit(this);
	tunit = new Unit(unit);
	if ((runit.mksa&_log) != 0) {
	    runit.mksa &= ~(_log|_mag);
	    runit.convertFrom(this);
	}
	if ((tunit.mksa&_log) != 0) {
	    tunit.mksa &= ~(_log|_mag);
	    tunit.convertFrom(this);
	}
	runit.mult(tunit) ;
	if ((mksa&_log) != 0) {		// Convert to log scale
	    if ((mksa&_mag) != 0) runit.mag() ;
	    else runit.log() ;
	}
	// Copy the result
	set(runit);
	// System.arraycopy(runit, 0, this, 0, 1) ; // this = runit;
    }

    // ==================================================================
    //		Internal Parsing for Unit Interpretation
    // ==================================================================
    /** 
     * Interpret for a SimpleUnit as  
     *  [multiplicator-prefix]Unit-Symbol[power]<br>
     * Only the UNIT part (value not touched)
     * @param  	text text to interpret
     * @param  	edited result of edited unit (may be null)
     * @return	the number of bytes interpreted
    **/
    private final int unit1 (Parsing text, StringBuffer edited) 
      throws ParseException {
      int posini = text.pos;		// Initial position in text
      Udef theUnit = uDef[0];		// Default Unit = Unitless
      int mult_index = -1;		// Index in mul_fact by e.g. &mu;
      int power = 1;
      char op = Character.MIN_VALUE;	// Operator power
      int error = 0;			// Error counter
      boolean close_bracket = false;	// () not edited when buffer empty
      int edited_pos = -1;
      int i, s;
    
    	/* Initialize the Unit to unitless */
    	mksa = _; factor = 1.;
    	if (text.pos >= text.length) return(0);	// Unitless

	if (DEBUG>0) System.out.println("....unit1(" + text + ")");
    	switch(text.a[text.pos]) {
      	  case '(':		/* Match parenthesized expression */
	    theUnit = null;	// Parenthese do NOT define any unit.
	    text.pos++;		// Accept the '('
	    close_bracket = (edited != null) ; /*&& (edited.length() > 0)*/
	    if (close_bracket) {
		edited_pos = edited.length();	// where to insert 'square'
	        edited.append('(') ;
	    }
	    this.unitec(text, edited) ;
	    if ((text.pos < text.length) && (text.a[text.pos] == ')'))
	        text.pos++;
	    else throw new ParseException
		("****Unit: Missing ) in '" + text + "'", text.pos);
	    if (close_bracket) {
	        // Remove the Ending blanks, before closing the bracket
		i = edited.length(); 
		while ((--i >= 0)  && (edited.charAt(i) == ' ')) ;
		edited.setLength(++i);
	        edited.append(')') ;
		close_bracket = false;
	    }
	    break ;
      	  case '"':		/* Quoted units must match exactly */
	    i = text.matchingQuote();
	    if (i<text.length) i++;	// Matching quote
	    theUnit = uLookup(text, i-text.pos);
	    if (theUnit == null) throw new ParseException
	        ("****Unit: quoted unit does not match", text.pos);
	    break ;
      	  case '-':		// Unitless ?
	    s = text.pos++;
	    if (text.pos >= text.length) break;
	    if (Character.isDigit(text.a[text.pos])) { 	// Number ?
	        text.pos = s;
	        break;
	    }
	    while ((text.pos<text.length) && (text.a[text.pos]=='-')) 
	        text.pos++;	// Accept unitless as "--" or "---"
	    break;
      	  case '%':
	    theUnit = uLookup(text, 1);
	    break;
      	  case '\\':		// Special constants
	    for (i=text.pos+1; (i<text.length) && Character.isLetter(text.a[i]); 
	  	i++) ;
	    theUnit = uLookup(text, i-text.pos) ;
	    if (theUnit == null) error++ ;
	    break;
      	  default:
	    for (i=text.pos; (i<text.length) && Character.isLetter(text.a[i]); 
	  	i++) ;
	    // A unit may be terminated by 0 (a0 = classical electron radius)
	    if ((i<text.length) && (text.a[i] == '0')) i++;
	    theUnit = uLookup(text, i-text.pos) ;
	    if (theUnit != null) break;
	    /* Simple unit not found. Look for multiple prefix */
	    s = text.pos ;	// Save 
	    if ((text.length-text.pos)>1) 
	        mult_index = text.lookup(mul_symb) ;
	    if (mult_index < 0) break;
	    theUnit = uLookup(text, i-text.pos) ;
	    if (theUnit == null) text.pos = s; 
    	}
    	/* Look now for a Power: */
    	if ((error == 0) && (text.pos < text.length)) 
	    op = text.a[text.pos];
	/* Power is however not acceptable for complex and date numbers */
	if (theUnit != null) {
	    if ((theUnit.mksa&(_abs|_pic)) != 0)
		op = Character.MIN_VALUE;
	}
	if ((op == '+') || (op == '-') || (op == '^') || 
		(Character.isDigit(op) && (op != '0'))) {
	    if (DEBUG>0) System.out.print("    look for power with op=" + op);
	    if (op == '^') text.pos += 1;
	    if (text.pos < text.length) {
	        op = text.a[text.pos];
	        if (op == '+') text.pos++ ;
	        if (op != '-') op = '+';
	        power = text.parseInt() ;
		// A zero-power is illegal !!
		if (power == 0) error++;
	        // 'square' or 'cubic' is spelled out BEFORE the unit name
	        else if ((power > 0) && (power < 10) && (edited != null)) {
		    text.pos--;		// Now text is the digit of power
		    i = text.lookup(op_symb) ;
		    if (i >= 0) { 	// Square or cubic
			if (edited_pos >= 0)
			     edited.insert(edited_pos, op_text[i]);
			else edited.append(op_text[i]) ; 
			op = ' '; 	// Power is now edited
		    }
		    else text.pos++;
	        }
		if (DEBUG>0) System.out.print(", power=" + power);
	    }
	    else error++;
	    if (DEBUG>0) System.out.println(", error=" + error);
	}

    	if (error>0) throw new ParseException
	    ("****Unit: '" + text + "'+" + text.pos, text.pos) ;

    	if (mult_index >= 0) {		// Multiplicities like 'k', '&mu;', ...
	    factor *= AstroMath.dexp(mul_fact[mult_index]);
	    if (edited != null) edited.append(mul_text[mult_index]) ;
        }

    	if (theUnit != null) {
	    factor *= theUnit.fact ;
	    mksa = theUnit.mksa;
	    offset = theUnit.orig;
            if (edited != null)
	        edited.append(theUnit.expl) ;
    	}

    	if (power != 1) {
	    this.power (power) ;
	    if ((op != ' ') && (edited != null)) {
	        edited.append("power") ;
	        if (power>=0) edited.append(op);	// - sign included...
	        edited.append(power);			// by this edition!
	    }
   	}
	s = text.pos - posini;
	if (DEBUG>0) System.out.println("  =>unit1: return=" + s 
		+ ", f="+factor + ", val="+value);
    	return(s);
    }

    /** Interpret for a CompoundUnit
     *  [factor] SimpleUnit [operator unit]<br>
     * Only the UNIT part (value not touched)
     * @param	text text to interpret
     * @param  	edited result of edited unit
     * @return 	false when nothing could be interpreted
    **/
    private final boolean unitec (Parsing text, StringBuffer edited) 
      throws ParseException {
      int posini = text.pos;
      Unit tunit = null;		// Temporary Unit
      int log_index = -1;		// Index in log_symb
      char op =  Character.MIN_VALUE;	// Operator
      char end = Character.MIN_VALUE;
      int error = 0;
      int i, s;

    	/* Initialize the Unit to unitless */
    	mksa = _; 

	if (DEBUG>0) System.out.print("....unitec(" + text + "): factor=");
	//System.out.println("..unitec(0): edited=<" + edited + ">");
	/* Ignore the leading blanks */
	text.gobbleSpaces();

    	/* Interpret the Factor */
	s = text.pos;			// Save
	factor = text.parseFactor();	// Default is 1.0
	if (DEBUG>0) System.out.println(factor);
	if ((edited != null) && (s != text.pos))
	    edited.append(text.a, s, text.pos-s) ;

	/* Possibly all as Log */
	log_index = text.lookup(log_symb) ;
	if (log_index >= 0) {		// A log(...) or mag(...)
	    end = log_end[log_index] ;
	    if (edited != null) edited.append(log_symb[log_index&(~3)]) ;
	    /* Eventual factor within the [] */
	    s = text.pos;		// Save
	    double facilog = text.parseFactor();
	    if ((edited != null) && (s != text.pos)) {
		edited.append(text.a, s, text.pos-s) ;
		offset = AstroMath.log(facilog);
	    }
	    if (DEBUG>0) System.out.println(", offset=" + offset);
	}

	/* Find out the Units and Operators */
	s = -1;					// Index of Operator in op_symb
	while (text.pos < text.length) {
	    if (text.a[text.pos] == end) break;	/* Closing the log() */
	    if (op == Character.MIN_VALUE) {
		i = this.unit1(text, edited);
	        if (DEBUG>0) { if (tunit != null) tunit.dump("..unitec"); }
		op = 'x';			// Default operator
	    }
	    else {
	        if (tunit == null) tunit = new Unit();
		i = tunit.unit1(text, edited);
	    	//System.out.println("..unitec(1): edited=<" + edited + ">");
	        if (DEBUG>0) {
		    tunit.dump("..unitec");
	    	    System.out.println("    combining with op=" + op);
		    System.out.println("    this=" + this + " \tfactor=" + this.factor);
		    System.out.println("    temp=" + tunit+ " \tfactor=" + tunit.factor);
		}
	        // if (i == 0) break;		// Accept Nothing matched
	        if (op == '/') this.div(tunit) ;
	        else this.mult(tunit) ;
	    }

	    /* V1.1: offset may be specified with #+/- , e.g.  K#+273.15 */
	    if (text.match('#')) { 
		int old_pos = text.pos-1;	// Just before the #
		double o = text.parseFactor();
		if (text.pos <= old_pos+3) 	// Not an offset, need 3 bytes
		    text.pos = old_pos;
		else this.offset = o;
		if ((edited != null) && (this.offset != 0)) {
		    edited.append("(offseted by ");
		    editing.editDouble(edited, this.offset, 
			    Astroformat.SIGN_EDIT);
		    edited.append(')');
		}
	    }

	    /* Which operator is applied on on the value ? */
	    if ((mksa&_abs)!=0) s = -1;		// No operator on date...
	    else s = text.lookup(op_symb);	// Can there be an Operator ?
	    if (s < 0) break;			// Can't interpret continuation

	    op = op_symb[s].charAt(0);		// Is always '/' for division

	    if (edited != null) {		// Edit the Operator
	        // Remove the Ending blanks, to install a single blank
		i = edited.length(); 
		while ((--i >= 0)  && (edited.charAt(i) == ' ')) ;
		edited.setLength(++i);
		edited.append(' ');
		edited.append(op_text[s]);
		//System.out.println("..unitec(2): edited=<" + edited + ">");
	    }
	}

	/* Close the log() */
	if (log_index >= 0) {
	    if (edited != null) {		// Close the parenthese
	        // Remove the Ending blanks
		i = edited.length(); 
		while ((--i >= 0)  && (edited.charAt(i) == ' ')) ;
		edited.setLength(++i);
	        edited.append(log_end[log_index&(~3)]) ;
	    }
	    if ((text.pos < text.length) && (end != Character.MIN_VALUE)
	          &&(text.a[text.pos] == end)) {
	        end = Character.MIN_VALUE;
	        text.pos++;
	    }
	    mksa |= _log ;
	    /* V1.3: conetr factor */
	    if (factor != 1) {
		offset = AstroMath.log(factor);
		factor = 1.;
	    }
	    if ((log_index&4) != 0) mksa |= _mag;
	    //System.err.println("unitec: factor="+factor);
	}

	/* Final Verifications */
	if (s >= 0) throw new ParseException	// Missing 2nd argument
	    ("****Unit.text+" + text.pos + " (missing operand): " + text, 
	    text.pos) ;
	if (end != Character.MIN_VALUE) throw new ParseException
	    ("****Unit.text+" + text.pos + " (missing '" + end + "'): " + text, 
	    text.pos) ;

	/* Skip the trailing blanks */
	text.gobbleSpaces();
	if (DEBUG>0) System.out.println("  =>unitec: return=" 
		+ (text.pos > posini) + "\tfactor=" + factor);
    	return(text.pos > posini);
    }


    /*==================================================================
		Set components of a Unit
     *==================================================================*/

    /** 
     * Reset a unit to unitless.
    **/
    public final void set () {	
	symbol = null;	
    	mksa = _; factor = 1;
	value = 0./0.;			// NaN
	offset = 0.;
    }

    /** 
     * Copy a quantity.
     * Define a Unit and Value from an existing one (clone)
     * @param	u the Unit to clone
    **/
    public final void set (Unit u) {		// Just copy the instance
      	  mksa = u.mksa ;
      	symbol = u.symbol ;
      	value  = u.value  ;
      	factor = u.factor ;
	offset = u.offset ;
    }

    /** 
     * Interpret a string for a value + Unit.
     * @param  	t text to interpret. For instance <br>
     *	100km/s is interpreted as  Unit=<b>km/s</b>; Value=<b>100</b><br>
     *		To add a numeric factor in Unit, use parentheses, e.g. <br>
     *		123(100km/s) is: Unit: <b>100km/s</b>; Value=<b>123</b>.
     *		The unit symbol may <em>precede</em> the value -- 
     *		this way is required for sexagesimal and dates.
     * @return	true if something found, false if nothing found.
    **/
    public boolean parsing (Parsing t) {
      int posini = t.pos;
      boolean has_value, has_symbol;
      int pos, j;
      double val;
        if (!initialized) init();		// Be sure everyting in place

	t.gobbleSpaces();			// Ignore the leading blanks
	pos = t.pos;

	if (DEBUG>0) System.out.print("....parsing(" + t + "):");

	/* Interpret the Value, if any */
	val  = t.parseFactor();			// Default value is 1
	if (DEBUG>0) System.out.print(" val=" + val);
	has_value = t.pos > pos; 		// NaN when nothing interpreted

        /* Skip blanks between value and Unit */
	t.gobbleSpaces();

	/* It may happen that interpreting the Value first generates an error,
	   for instance 1/4. That means that the number (1) is part of the
	   unit, and is not the value in front of the unit.
	*/
	if (t.lookup(op_symb) >= 0) {
	    has_value = false;
	    t.pos = pos;
	    if (DEBUG>0) System.out.print("(FALSE!)");
	}

	/* Interpret the Unit */
	pos    = t.pos;				// To keep the Unit symbol
	offset = 0.;
	symbol = null;
	if (DEBUG>0) System.out.println("\n    Interpret '" + t + "'");
      	try { 
	    has_symbol = unitec(t, null); 
	    symbol = String.copyValueOf(t.a, pos, t.pos-pos);
	    Object o = hUnit.get(symbol);
	    if (o instanceof String) symbol = (String)o;
	    if (has_value & (mksa&_pic) != 0) {	// A misinterpretation ? Rescan
		int pos_end = t.pos;
		t.set(posini); t.gobbleSpaces();
		try { t.parseComplex(symbol); t.set(pos_end); }
		catch(Exception e) { 
		    t.set(posini); 
		    return(false); 
		}
	    }
	}
	catch(Exception e) { 
	    if (DEBUG>0) {
	         System.out.println("++++unitec catched: " + e);
	         e.printStackTrace();
	         try { Thread.sleep(2000); } catch(Exception i) {;}
	    }
	    has_symbol = false; 
	    t.pos = pos;
	}
	if (DEBUG>0) System.out.println("\n    interpret '" + t 
		+ "', has_value=" + has_value);

	// Default value in log scale is 0 !
	// if (no_value && ((mksa&_log) != 0)) value = 0;
	/* Skip the trailing blanks */

	/* The value may follow the unit -- only for dates and "special"
	 * quoted unit
	 */
	if ((!has_value) && (t.pos < t.length)) {
	    pos = t.pos;
	    // The symbol may be a "picture" e.g. "YYYY/MMM/DD"
	    if ((!has_symbol) && (t.currentChar() == '"')) {
		if ((j = t.matchingQuote()) > 0) {
		    Udef u; int ip1, ip2;
		    String msg;
		    if (DEBUG>0) System.out.println(
			    "....parsing: t.matchingQuote()=" + j);
		    j -= pos; j++;		// Length including quotes
		    symbol = t.toString(j);
		    t.advance(j);
		    t.gobbleSpaces();
		    pos = t.pos;
		    try { val = t.parseComplex(symbol); }
		    catch(ParseException e) {	// Mismatch: could be mixed up
			msg = e.getMessage();
			if (msg.indexOf("parseComplex((")>=0) 	// ERROR in pic
			    System.err.println(e);
			t.set(posini);
			return(false);
		    }
		    if (t.status() != 0) {	// Something wrong in 'picture'
			msg = t.error_message;
			if ((ip1 = msg.indexOf(" interpreted as (")) >0) {
			    ip1 = msg.indexOf('(', ip1);
			    ip2 = msg.indexOf(')', ip1);
			    String correct_symbol = msg.substring(ip1+1, ip2);
			    if (DEBUG>0) System.out.println(
				    "....parsing: adding Hash "+symbol
				    +" => "+correct_symbol);
			    /* Add the new 'picture' as an alias 
			     * to the correctly spelled one.
			     */
			    hUnit.put(symbol, correct_symbol);
			    symbol = correct_symbol;
			    t.pos = pos;
			    try { val = t.parseComplex(symbol); }
			    catch (ParseException pe) {
				System.err.println(pe);
				pe.printStackTrace();
				t.set(posini);
				return(false);
			    }
			}
			else {
			    System.err.println(msg);
			    t.set(posini);
			    return(false);
			}
		    }
		    has_value = true;
		    if (t.isDate())      u = uLookup("MJD");
		    else if (t.isDays()) u = uLookup("d");
		    else if (t.isTime()) u = uLookup("\"h:m:s\"");
		    else                 u = uLookup("\"d:m:s\"");
		    // The quoted symbol is added, to speed up its retrieval
		    // in the next search.
		    try {
		      addSymbol(symbol, u.symb, Parsing.explainComplex(symbol));
		      u = uLookup(symbol);
		      u.mksa |= _pic;
		    }
		    catch (ParseException pe) {
			System.err.println(pe); 
			pe.printStackTrace();
		    }
		    mksa   = u.mksa;
		    factor = u.fact;
		}
	    }
	    if (has_symbol && (t.pos<t.length)) {
	    if (DEBUG>0) System.out.println("....parsing: symbol=" + symbol 
		    + ", interpret: " + t);
		if (mksa == _MJD) {		// Get Date + Time
		    if (DEBUG>0) System.out.print("    parsing via Astrotime(");
		    Astrotime datime = new Astrotime();
		    if (Character.isLetter(symbol.charAt(0)))
			t.set(posini);		// JD or MJD followed by number
		    if (DEBUG>0) System.out.print(t + ") symbol=" + symbol);
		    has_value = datime.parsing(t);
		    if (has_value) val = datime.getMJD();
		    if (DEBUG>0) {
			System.out.println(" has_value=" + has_value 
			    + ", MJD=" + val);
			datime.dump("datime ");
		    }
		}
		else if ((mksa&_pic) != 0) {	// Interpret complex
		     try { val = t.parseComplex(symbol); has_value = true; }
		     catch (Exception e) { t.set(posini); return(false); }
		}
		//* No other case! else val = t.parseFactor();
	    }
	}

	// Final: Store the results.
	value = has_value ? val : 0./0.;
	if (has_symbol|has_value) {
	    return(true);
	}
	t.pos = posini;
	return(false);				// Nothing found...
    }


    /** Interpret a String to a Unit + Value
     * @param  	text text to interpret. For instance <br>
     *	100km/s is interpreted as  Unit=<b>km/s</b>; Value=<b>100</b><br>
     *		To add a numeric factor in Unit, use parentheses, e.g. <br>
     *		123(100km/s) is: Unit: <b>100km/s</b>; Value=<b>123</b>.
     *		The unit symbol may <em>precede</em> the value -- 
     *		this way is required for sexagesimal and dates.
     * @throws  ParseException when the argument contains non-interpretable
     * 		text
    **/
    public void set (String text) throws ParseException {
      Parsing t = new Parsing(text);
      boolean p;
        this.set();				// Reset
	if (DEBUG>0) dump("set(" + t + ")");
        p = this.parsing(t);
	t.gobbleSpaces();
	/* Accept NULL */
	if (DEBUG>0) System.out.println("....Unit.set("+text+")" + p 
		+ " => " + this.toString());
      	if (t.pos < t.length) throw new ParseException
           ("****Unit: set '"  + text + "'+" + t.pos, t.pos);
    }

    /** 
     * Examine a String for a Unit + Value
     * @param  	text text to interpret; for instance <br>
     *	100km/s is interpreted as  Unit=<b>km/s</b>; Value=<b>100</b><br>
     *		To add a numeric factor in Unit, use parentheses, e.g. <br>
     *		123(100km/s) is: Unit: <b>100km/s</b>; Value=<b>123</b>
     * @param  	offset where to start in the text string.
     * @return	the new position in text following what's interpretated
     * 		text
    **/
    public int parse (String text, int offset) {
      Parsing t = new Parsing(text, offset);
      if (true) {
	this.parsing(t);
      }
      else {				//*** OLD CODE
        double val;
        int pos1, pos2, pos3;
        boolean has_value, has_unit;
        if (!initialized) init();	// Be sure everyting in place

        /* Ignore the leading blanks */
	t.gobbleSpaces();
	pos1 = t.pos;			// Just before the possible value

	/* Interpret the Value, if any */
	val  = t.parseFactor();		// Default value is 1
	pos2 = t.pos;			// After interpretation of Value
	has_value = pos2 > pos1; 	// Something was found.

        /* Skip blanks between value and Unit */
	t.gobbleSpaces();

	/* It may happen that interpreting the Value first generates an error,
	   for instance 1/4. That means that the number (1) is part of the
	   unit, and is not the value in front of the unit.
	*/
	if (t.lookup(op_symb) >= 0) {
	    has_value = false;
	    t.pos = pos1;
	    pos2  = pos1;
	}

	/* Interpret the Unit */
	has_unit = true;
	pos3 = t.pos;			// Starting offset of unit
      	try { unitec(t, null) ; }
	catch (Exception e) {
	    has_unit = false;
	    t.pos = pos2;
	    if (!has_value) t.pos = offset;
	}
	if (has_unit) symbol = text.substring(pos3, t.pos);
	value = has_value ? val : 0./0. ;
      }
      return(t.pos);
    }


    /** 
     * Look in the input string for a Unit
     * @param  	text to be interpreted as a Unit.
     * 		The value part is always set to NaN.
     * @param  	offset where to start in the text string.
     * @return	the new position in text following what's interpretated
    **/
    public int parseUnit (String text, int offset) {
      int o = parse(text, offset);
	this.setUnit();
	return(o);
    }

    /** 
     * Look in the input string for a value
     * @param  	text value (set to NaN when not correct), which can use the
     *		exponential notation like <tt>10+8</tt> to express 
     *		<b>10<sup>8</sup></b>
     * @param  	offset where to start in the text string.
     * @return	the new position in text following what's interpretated
    **/
    public int parseValue (String text, int offset) {
      Parsing t = new Parsing(text, offset);
      int posini;
        /* Ignore the leading blanks */
	t.gobbleSpaces();
	posini = t.pos;
	/* Take care of units requiring data in Sexagesimal */
	if ((mksa&_sex) != 0)
	     value  = t.parseSexa();
	else value  = t.parseFactor();
	if (t.pos == posini) 		// Nothing (NaN checked in Parsing)
	    return(offset);
	    // while ((t.pos < t.length) && (t.a[t.pos] == '-')) t.pos++;
	return(t.pos);
    }

    /** 
     * Convert the current number+Unit into a Unit.
     * 		For instance, if value is 100 and unit is km/s,
     * 		the value becomes 1 and the unit becomes (100km/s).
     * 		Similar to setUnit(toString())
    **/
    public void setUnit () {
	if (Double.isNaN(factor)) factor = 1;
        double f = factor;
        /* Transform the value as a part of Unit */
        if (Double.isNaN(value)) value = 1;
	/* if ((mksa&_log) != 0) {
	    if (Double.isNaN(value)) value = 0;
	    if ((mksa&_mag) != 0) value *= -2.5;
	    factor *= AstroMath.dexp(value);
	    value = 0;
	}
	else {
	    factor *= value;
	    value = 1.;
	}*/
	factor *= value;
	value = 1;
	// Transform also the symbol
	if (f != factor) {
	    if (symbol == null) symbol = edf(factor);
	    else symbol = edf(factor) + toExpr(symbol);
	}
    }

    /** 
     * Assigns the Unit.
     * The value of the quantity is unchanged.
     * @param  	text to be interpreted as a Unit.
     * 		The value part is always set to NaN.
     * @throws  ParseException when the argument text which does not 
     *		represent a number
    **/
    public void setUnit (String text) throws ParseException {
	//DEBUG=1;
	if (DEBUG>0) System.out.println("....Unit.setUnit("+text+")");
        this.set(text);
	//this.dump("SetUnit(1)");
	this.setUnit();
	//this.dump("SetUnit(2)");
	DEBUG=0;
    }

    /** 
     * Assign the <em>value</em>.
     * The unit of the quantity is unchanged.
     * @param  	text value (set to NaN when not correct), which can use the
     *		exponential notation like <tt>10+8</tt> to express 
     *		<b>10<sup>8</sup></b>
     * @throws  ParseException when the argument text which does not 
     *		represent a number
    **/
    public void setValue (String text) throws ParseException {
      Parsing t = new Parsing(text);
      int posini;
        /* Ignore the leading blanks */
	t.gobbleSpaces();
	posini = t.pos;
	/* Take care of units requiring data in Sexagesimal */
	if ((symbol.charAt(0) == '"') && (symbol.indexOf(':')>0)) 	// "
	     value  = t.parseSexa();
	else value  = t.parseFactor();
	if (t.pos == posini) {		// Nothing (NaN checked in Parsing)
	    value = 0./0. ;
	    // while ((t.pos < t.length) && (t.a[t.pos] == '-')) t.pos++;
	}
	/* Skip the trailing blanks */
	t.gobbleSpaces();
	if (t.pos < t.length) throw new ParseException
           ("****Unit: setValue '" + text + "'+" + t.pos, t.pos);
    }

    /** 
     * Assign the <em>value</em>.
     * The unit of the quantity is unchanged.
     * @param  	value value to set
    **/
    public void setValue (double value) {
      this.value = value;
    }

    // ==================================================================
    //		Internal methods for Editing Units
    // ==================================================================

    /** 
     * Edition of a single factor number if differs from 1 <br>
     * Edition as x.fraction&times;10&pm;pow
     * @param	buf  the edition buffer to which the edited number is appended
     * @param	factor the number to edit
     * @return	the number of bytes added to buf
    **/
    private static final StringBuffer edf (StringBuffer buf, double factor) {
        /* Finally, return the edition with the appropriate routine */
	//System.err.println("edf("+factor+")");
        return(editing.editDouble(buf, factor, Editing.FACTOR));
    }

    /** 
     * Edition of a single factor number if differs from 1 as a String
     * @param	factor the number to edit
     * @return	the edited factort
    **/
    private static final String edf (double factor) {
      StringBuffer buf ;
	if (factor == 1) return("") ;
	buf = new StringBuffer(32);
	edf(buf, factor);
	return("" + buf);
    }
	
    /** 
     * Edit the MKSA part, choose the most appropriate unit
     * @param  	buf edition buffer
     * @param  	mksa the 'dimension' of the unit (the 'log' is ignored)
     * @param  	option 0 = only basic SI; 1 = try best
     * @return 	the number of bytes used to explain the MKSA
    **/
    private static final int edu (StringBuffer buf, long mksa, int option) {
      int len0 = buf.length();
      int i, e, o, xff;
      int nu=0; 
      boolean sep = false;
      long m;

	/* Remove the 'log' or 'mag[]' part */
	if ((mksa&_log) != 0) {
	    mksa &= ~_log;
	    if ((mksa&_mag) != 0) mksa &= ~_mag;
	}

    	/* Check unitless -- edited as empty string */
    	if (mksa == _) return(0);

	/* Find the best symbol for the physical dimension */
	if (option > 0) {
	    nu = (mksa&_mag) == 0 ? 0 : 1;
    	    for (m=(mksa<<8)>>8; m!=0; m >>>= 8) {
                if ((m&0xff) != _e0 ) nu++ ;
    	    }

    	    /* Find whether this number corresponds to a composed unit */
    	    for (i=0; i<uDef.length; i++) {
                if (uDef[i].mksa != mksa) continue;
	        if (uDef[i].fact != 1) continue;
	        buf.append(uDef[i].symb) ;
	        break;
    	    }

    	    // A basic unit need no explanation 
    	    if (nu == 1) return(buf.length() - len0) ;

    	    // Add the explanation in MKSA within parenthesis
    	    if (buf.length() != len0) buf.append(" [") ;
    	    else nu = 0;	// nu used as an indicator to add the ) 
	}

	o = _m0;	// Zero power of magnitude
	xff = 7;	// Mask for magnitude
    	for (i=0; i<8; i++) {
	    e = (int)((mksa>>>(56-(i<<3)))&xff) - o;
	    o = _e0 ;	// Other units: the mean is _e0 
	    xff = 0xff;	// Other units
	    if (e == 0) continue;
	    if (sep) buf.append("."); sep = true;
	    buf.append(MKSA[i]);
	    if (e == 1) continue;
	    if (e>0) buf.append("+") ;
	    buf.append(e) ;
    	}
    	if (nu>0) buf.append("]");
    	return(buf.length() - len0) ;
    }

    /** 
     * Edit the unit in terms of SI basic units.
     * @param 	b Buffer to fill
     * @param 	option level of Explanation
     *		0 = only SI, 1 = detail
     * @return 	the explanation with basic SI units
    **/
    private void toSI (StringBuffer b, int option) {
	boolean close = false;
	if ((mksa&_log) != 0) b.append(
	    (mksa&_mag) != 0 ? "mag[" : "log[");
	if (factor != 1) {
	    edf(b, factor);
	    if (offset != 0) { b.append('('); close = true; }
	}
	edu(b, mksa, option);
	if (offset != 0) {	// V1.1: Edit offset
	    b.append("#");
	    edf(b, -offset);
	    if (close) b.append(')'); 
	}
	if ((mksa&_log) != 0) b.append("]") ;
    }

    /*==================================================================
		Editions of Units
     *==================================================================*/

    /** 
     * Dump the details.
     * Essentially for debugging purposes.
     * @param	title title of dump
    **/
    public void dump (String title) {
      char[] xdim = new char[24];
      long m;
      int i;
	/* Hexadecimal edition of mksa */
	for (m=mksa, i=24; i>0; ) {
	    xdim[--i] = x[(int)(m&15)]; m>>>=4; 
	    xdim[--i] = x[(int)(m&15)]; m>>>=4; 
	    xdim[--i] = '.';
	}
	xdim[i] = 'x';
	System.out.println(title + ": symbol='" + symbol 
	  + "', value=" + value);
	System.out.print  ("        factor=" + factor 
	  + ", Dim0" + new String(xdim));
	if (offset!=0) System.out.print(" offset=" + offset);
	System.out.println("");
    }

   /**
    * Edit the physical dimension of a unit.
    * @param buf  Buffer where the result is appended. A question mark (?)
    * 			indicates problems.
    * @return   the StringBuffer
    */
    public final StringBuffer editDimension(StringBuffer buf) {
	int i, dim;
	for (i=1; i<MKSAdim.length; i++) {
	    if (MKSAdim[i] == Character.MIN_VALUE) continue;
	    dim = (int)((mksa>>((7-i)*8))&0xff) - _e0;
	    if (dim == 0) continue;
	    buf.append(MKSAdim[i]);
	    if (dim>0) buf.append('+');
	    buf.append(dim);
	}
	return(buf);
    }

   /**
    * Physical dimension of a unit.
    * @return   the dimension with <b>MLTAK</b> and exponent.
    *  		The symbols mean Mass, Length, Time, Ampere
    * 		and Kelvin respectively.
    */
    public final String dimension() {
	StringBuffer b = new StringBuffer(16);
	this.editDimension(b);
	return(b.toString());
    }


   /**
    * Edit the value in a StringBuffer
    * @param buf  Buffer where the result is appended
    * @return   the StringBuffer
    */
    public final StringBuffer editValue(StringBuffer buf) {

	if (Double.isNaN(value)) 
	    return(edf(buf, value));

	if (mksa == _MJD) {				// Date
	    if (symbol.startsWith("\"dat")) {
		double mjd = value + offset/factor;
		int day = (int)mjd;
		if (value<0) --day;
	        double sec = (mjd - day)*86400;		// seconds.
		editing.editDate(buf, day);
		if (symbol.startsWith("\"datim")) {
		    buf.append('T');
		    editing.editSexa(buf, sec/3600., 2, 
			    -10 /* precision up to micro-sec */, 
			    editing.ZERO_FILL|editing.SEXA3c);
		}
		else if (sec >= 1.e-6) 		// Edit fraction of day
		    editing.editDecimal(buf, sec/86400., 0 /* No fraction! */,
			    -11 /* 10-11day =~ 1 micro-sec */, 0);
	    }
	    else if (symbol.charAt(0) == '"') 
		editing.editComplex(buf, value, symbol);
	    else { 				// JD or MJD 
		// Precision = 10-10day if JD, 10-11d if MJD
		int ndec = symbol.charAt(0) == 'J' ? -10 : -11;
		editing.editDecimal(buf, value, 0, ndec, 0);
	    }
	}
	else if ((mksa&_pic) != 0)
	    editing.editComplex(buf, value, symbol);
	else if ((mksa&_sex) != 0)
	    editing.editSexa(buf, value, 1, -9, editing.SEXA3c);
	else edf(buf, value);
	return(buf);
    }

    /** 
     * Edition of the quantity.
     * This edition expresses the value and its associated unit;
     * the unit is attached to the value as e.g. <b>10km/s</b>,
     * but precedes the value for complex values or dates as e.g.
     * <b>MJD51234</b> or <b>"h:m:s"23:59:58</b>
     * @param buf  Buffer where the result is appended
     * @return 	the edited value and unit
    **/
    public final StringBuffer edit(StringBuffer buf) {
      boolean symb_edited = false;
      int symlen = 0;
	if (DEBUG>0) dump("unit.edit");
	if (symbol != null) symlen = symbol.length();
	// For dates & pictures, edit the symbol first
	if ((mksa&(_pic|_abs|_sex)) != 0) {
	    symb_edited = true;
	    buf.append(symbol);
	}
	// When no value given, there must be a representation.
        if ((!Double.isNaN(value)) || (symlen == 0)) {
	    editValue(buf);
	}
	if ((mksa == _) && (factor == 1));
	else if (!symb_edited) {
	    if (symlen > 0) {
		boolean add_bracket = Character.isDigit(symbol.charAt(0));
		if (add_bracket) buf.append('(');
		buf.append(symbol) ;
		if (add_bracket) buf.append(')');
	    }
	    else { buf.append('('); toSI(buf, 0); buf.append(')'); }
	}
	return(buf);			// Buffer converted to String
    }


   /** 
    * Explain the unit given in argument.
    * Interpret a String representing a Unit, and return
    *          a complete explanation.
    * @param  	text the text containing the units to explain
    * @return 	the full explanation of the Unit -- or a text 
    * 		<tt>?***bad Unit</tt> when <em>text</em> is not interpretable
   **/
    public static final String explainUnit (String text) {
      Parsing  t = new Parsing(text);
      StringBuffer b = new StringBuffer(120) ;
      Unit u = new Unit();
      int ini1 = 0;
	if (!initialized) init();
      	try { u.unitec(t, b); }
	catch (Exception e) { 
	    if (DEBUG>0) {
	        System.out.println("++++explainUnit: catched: " + e); 
	        try { Thread.sleep(2000); } catch(Exception i) {;}
	    }
	    if (t.currentChar() == '"') {	// "picture" unit
	 	b.append(Parsing.explainComplex(t.toString()));
	    }
	    else { String prefix = "?***bad Unit <";
	        b.insert(0, prefix);
	        b.insert(prefix.length(), text);
	        b.insert(prefix.length() + text.length(), "> ");
	    }
	}
	//System.out.println("\n..explainUni........<" + b + ">");
	ini1 = skipBrackets(b);			// Remove enclosing Parentheses
	// return(""+b.substring(ini1));	// Java>=1.2
	return(b.toString().substring(ini1));	// AnyJavaVersion
    }

    /** 
     * Give a full explanation of the unit.
     * @return 	the explanation in terms of SI units
    **/
    public final String explainUnit () {
      StringBuffer b = new StringBuffer(120) ;
      Parsing  t = new Parsing(symbol);
      Unit u = new Unit();
      int ini1 = 0;
	try { u.unitec(t, b); }
	catch (Exception e) {
	    if (DEBUG>0) {
		System.out.println("++++explainUnit: catched: " + e);
		try { Thread.sleep(2000); } catch(Exception i) {;}
	    }
	    if (t.currentChar() == '"') {	// "picture" unit
	 	b.append(Parsing.explainComplex(t.toString()));
	    }
	    else { String prefix = "?***bad Unit <";
	        b.insert(0, prefix);
	        b.insert(prefix.length(), symbol);
	        b.insert(prefix.length() + symbol.length(), "> ");
	    }
	}			// Should never happen!
	ini1 = skipBrackets(b);			// Remove enclosing Parentheses
	b.append(" ("); toSI(b, 1); b.append(")") ;
      	// return(""+b.substring(ini1));	// Java>=1.2
	return(b.toString().substring(ini1));	// AnyJavaVersion
    }

    /** Explain the Unit+Value in terms of SI units
     * (Convert to non-log)
     * @return 	the explanation with basic SI units
    **/
    public final String toStringInSI() {
      StringBuffer b = new StringBuffer(64) ;
      Unit u = new Unit(this) ;
	/* Convert the value to linear scale, and then edit */
	if ((u.mksa&_log) != 0) u.mksa &= ~(_log|_mag);
	if (u.offset != 0) {		// Find the corresponding SI !
	    Udef base = uLookup(u.mksa);
	    u.symbol = base.symb;
	    u.offset = base.orig;
	}
        u.factor = 1; 
        u.convertFrom(this);
	if (!Double.isNaN(value)) { 
	    edf(b, u.value);
	    if (b.length() == 0) b.append('1');
	}
        u.toSI(b, 0);
      	return(""+b);			// Buffer converted to String
    }

    /*==================================================================
		Standard Edition
     *==================================================================*/

    /** 
     * Standard Edition of the Unit.
     * Both value and associated unit are given; the conventions are
     * detailed in {@link #edit}
     * @return 	the edited value and unit.
     * To edit the value only, see {@link #editedValue}
    **/
    public String toString () {
      StringBuffer b = new StringBuffer(120) ;
      boolean symb_edited = false;
	if (DEBUG>0) dump("unit.toString");
	// For dates & pictures, edit the symbol first
	if ((mksa&(_pic|_abs|_sex)) != 0) {
	    symb_edited = true;
	    b.append(symbol);
	}
        if (!Double.isNaN(value)) {
	    editValue(b); 		// Value '1' not edited
	    if (b.length() == 0) b.append((mksa&_log)!=0 ? '0' : '1');
	}
	if ((mksa == _) && (factor == 1));
	else if (!symb_edited) {
	    if (symbol != null) {
		boolean add_bracket = Character.isDigit(symbol.charAt(0));
		if (add_bracket) b.append('(');
		b.append(symbol) ;
		if (add_bracket) b.append(')');
	    }
	    else { b.append('('); toSI(b, 0); b.append(')'); }
	}
	return(b.toString());		// Buffer converted to String
    }

    /** 
     * String represntation of the value.
     * The unit is not edited.
     * @return 	the edited value
     * To edit the value and the number, see {@link #toString}
    **/
    public String editedValue() {
      StringBuffer b = new StringBuffer(120) ;
        editValue(b);
	return(b.toString());		// Buffer converted to String
    }

}
