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
                Otype class (includes also Odef)
 *==================================================================*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*; // for parseException

//import com.braju.format.Parameters;
//import com.braju.format.Format;

/**
 * The <em>Otype</em> is a classification of the astronomical objects
 * in a hierarchical structure.
 * 
 * @author	Francois Ochsenbein
 * @author      Benoit Baranne (Stagiaire UTBM)
 * @version 1.0	16-sep-2002<br>
 *          1.1 29-oct-2002 (Sylvaine Jaehn (Stage UTBM)) : 
 *          	ajout de la methode numericToHexa()<br>
 *          1.2 14-nov-2002 (SJ):   ajout du test : 
 *            if (!initialized) init(); ds constructeur otype sans parametre<br>
 *               ajout de la methode hexaToNumeric(String strHex)<br>
 *          1.3 24-jan-2003 (SJ) :   methode init() mise public<br>
 * @version 1.4 jan 2004 (BB) : ajout de la methode equals.
 * -------------
 *
 **/

public class Otype implements Serializable {

    // The Otype is just a number -- the bits are arranged to map to
    // the hierarchical structure.
    public int otype; // The Otype is just a number!!
    
    // Variables common to the Class: all Definitions
    static final boolean DEBUG = true;
    static boolean initialized = false;
    static private final int B0 = 4; // Wavelength
    static private final int B1 = 4; // Main Class
    static private final int B2 = 5; // Subclass
    static private final int B3 = 4; // Nova/SN
    static private final int Bf = 15; // Not Yet Used
    static private final char[] dig = "0123456789".toCharArray();
    static private final int[] bits = {
    	B0, B1, B2, B3, Bf};
    static private final int[] Nt = {
    	(1 << B0) - 1, 
    	(1 << B1) - 1, 
    	(1 << B2) - 1, 
    	(1 << B3) - 1, 
    	(1 << Bf) - 1};
    static private final int[] Nlev = new int[bits.length + 1];
    //  0, 0xf0000000, 0xff000000,		// Levels 0 1 2
    //  0xfff80000, 0xffff8000, 0xffffffff	// Levels 3 4 5
    static private Hashtable Hsymbols = new Hashtable(300);
    static private Vector otypes = new Vector(200);
    static private Vector otrees = new Vector(100);
    // static private final String def_url = 
    // 	"http://simbad.u-strasbg.fr/otype.def";
    static private final String def_url =
    	"http://localhost:2001/Tables/Otype/otype.def";

    /*==================================================================*
     			Constructors
     *==================================================================*/
    
    /** Define the default Otype (object of unknown nature)
     */
    public Otype() {
    	if (!initialized) {
    		init();
    	}
    	otype = 0;
    }
    
    /** Define the Otype from a String
     * @param text the Otype as a string
     * @throws ParseException when otype is not understandable
     */
    public Otype(String text) throws ParseException {
    	set(text);
    }
    
    /*==================================================================*
     	Upload the list of Object Types
     *==================================================================*/
    /** Insert a definition -- verify it's not alreay in
     * @param	symb the symbol of this object type
     * @param	odef the object type definition
     * @return	the previously defined level
     **/
    private static void hadd(String symb, Odef odef) {
    	String s = symb.toLowerCase();
    	Object o = Hsymbols.put(s, odef);
    	if (o != null) {
    		if (odef.equals(o)) {
    			return;
    		}
    		System.err.println("****Otype: '" + symb + "' ambiguous: "
				 + odef + " <> " + (Odef) o);
    	}
    }
    
    /** Read the List of the Object Types
     * @param	plug the input stream
     * @return	the previously defined level
     **/
    private static void upload(BufferedReader plug) {
    	//File odef = new File("otype.def");
    	String s1, s2, s3;
    	Odef odef; // An Object Type
    	char[] a;
    	String line;
    	int i, o, k;
    	o = 0xffffffff;
    	i = bits.length;
    	while (i > 0) {
    		Nlev[i] = o;
    		o <<= bits[--i];
    	}
    	Nlev[0] = 0;
    	while (true) {
    		try {
    			line = plug.readLine();
    		}
    		catch (Exception e) {
    			System.err.println(e);
    			return;
    		}
    		if (line == null) {
    			break;
    		}
    		if (line.length() < 10) {
    			continue;
    		}
    		a = line.toCharArray();
    		if (a[0] == '#') {
    			continue; // comment
    		}
    		if (a[0] == '-') {
    			continue; // ignored
    		}
    		if (a[0] == '(') {
    			continue; // ignored
    		}
    		if (a[0] == '+') {
    			continue; // ignored
    		}
    		for (i=0; (i < a.length) && Character.isWhitespace(a[i]); i++);
    		if (i >= a.length) {
    			continue;
    		}
    		//if (DEBUG) System.out.println("Got: " + line);
    		// Find the ':' which defines Synonyms
    		k = i;
    		while ( (i < a.length) && (a[i] != ':') && (a[i] != '/')) {
    			i++;
    			/* Interpret the line: numeric type / equivalences */
    		}
    		if (Character.isDigit(a[k])) {
    		    /* Here the line is like
    		      00.00.00.0: Unknown     ?    "Object of unknown nature"
    		    --------------------------------------------------------*/
    			o = ntype(new String(a, k, i - k));
    			if (i < a.length) {
    				i++; // Skip the :
    			}
    			while ( (i < a.length) && Character.isSpaceChar(a[i])) {
    				i++;
    			}
    			k = i;
    			while ( (i < a.length) && (!Character.isSpaceChar(a[i]))) {
    				i++;
    			}
    			s1 = new String(a, k, i - k);
    			while ( (i < a.length) && Character.isSpaceChar(a[i])) {
    				i++;
    			}
    			if ( (i < a.length) && (a[i] != '"')) { // "
    				k = i;
    				while ( (i < a.length) && (!Character.isSpaceChar(a[i]))) {
    					i++;
    				}
    				s2 = new String(a, k, i - k);
    				while ( (i < a.length) && Character.isSpaceChar(a[i])) {
    					i++;
    				}
    			}
    			else {
    				s2 = s1; // The 3-letter abbreviation
    			}
    			if ( (i < a.length) && (a[i] == '"')) { // "
    				k = ++i;
    				while ( (i < a.length) && (a[i] != '"')) {
    					i++; // "
    				}
    				s3 = new String(a, k, i - k);
    				if (i < a.length) {
    					i++;
    				}
    				while ( (i < a.length) && Character.isSpaceChar(a[i])) {
    					i++;
    				}
    			}
    			else {
    				s3 = s2;
    			}
    			odef = new Odef(o, s1, s2, s3);
    			otypes.addElement(odef);
    			hadd(s1, odef);
    			if (s2 != s1) {
    				hadd(s2.trim(), odef);
    			}
    			if (s3 != s2) {
    				hadd(s3, odef);
    				/* Are there other synonyms ? */
    			}
    			while (i < a.length) {
    				k = i;
    				while ( (i < a.length) && (!Character.isSpaceChar(a[i]))) {
    					i++;
    				}
    				s3 = new String(a, k, i - k);
    				hadd(s3, odef);
    				while ( (i < a.length) && Character.isSpaceChar(a[i])) {
    					i++;
    				}
    			}
    			continue;
    		}
    		if (i >= a.length) {
    			continue; // Empty Line
    		}
    		s1 = new String(a, k, i - k);
    		s1 = s1.trim();
    		k = i + 1; // Second part
    		s2 = new String(a, k, a.length - k);
    		s2 = s2.trim();
    		//if(DEBUG) System.out.println("....additional: " + s1 + a[i] + s2);
    		if (a[i] == ':') { // There is a : ==> Synonym
    			hadd(s1, (Odef) Hsymbols.get(s2.toLowerCase()));
    			continue;
    		}
    		if (a[i] == '/') { // There is a / ==> Subtree
    			otrees.addElement(Hsymbols.get(s1.toLowerCase()));
    			otrees.addElement(Hsymbols.get(s2.toLowerCase()));
    		}
    	}
    	initialized = true;
    	otypes.trimToSize();
    	otrees.trimToSize();
    	/*if (DEBUG) System.out.println("....Otype init: " + otypes.size()
    	  + "otypes, " + otrees.size() + "subtrees, " + Hsymbols.size()
    	  + "symbols");*/
    }

    /*==================================================================*
     		Initialisations
     *==================================================================*/

    /** Initialisation
     */
    public static void init() {
    	DataInputStream plug;
    	URL odef;
    	System.out.println("---> Otype.init() sans parametres appele");

    	try {
    		odef = new URL(def_url);
    		plug = new DataInputStream(odef.openStream());
    		upload(new BufferedReader(new InputStreamReader(plug)));
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
    }

    /** Initialisation
     * @param	filename name of file with the list of Object Types
     * @throws Exception e.g. IOException if file not existing, parsing...
     */
    public static void init(String filename) throws Exception {
    	File odef;
    	FileInputStream plug;
    	odef = new File(filename);
    	plug = new FileInputStream(odef);
    	upload(new BufferedReader(new InputStreamReader(plug)));
    }

    /** Compute the level in the hierarchy
     * @param	otype an object type
     * @return	the level (0 1 2 3 4)
     **/
    private static final int level(int otype) {
    	int i;
    	for (i = 0; otype != 0; otype <<= bits[i++]) ;
    	return (i);
    }

    /** Test whether ot is in class cl
     * @param	cl a 'class'
     * @param	ot a 'subclass'
     * @return	true if ot if subclass of cl
     **/
    private static final boolean otinclass(int cl, int ot) {
    	int i = level(cl);
    	return ( (ot & Nlev[i]) == cl);
    }

    /*==================================================================*
     			Internal Routines
     *==================================================================*/

    /** Edit the otype as xx.xx.xx.xx
     * @param	buf the buffer to which the ascii equivalent of otype
     *			is appended.
     * @param	otype the object type
     * @return	the previously defined level
     **/
    protected static void edit(StringBuffer buf, int otype) {
    	int[] otc = new int[4];
    	int o = otype >>> Bf;
    	int i;
    	for (i = bits.length - 1; --i >= 0; o >>>= bits[i]) {
    		otc[i] = o & Nt[i];
    	}
    	for (i = 0; i < 4; i++) {
    		buf.append(dig[otc[i] / 10]);
    		buf.append(dig[otc[i] % 10]);
    		buf.append('.');
    	}
    	i = buf.length();
    	buf.setLength(i - 1);
    }

    /** Interpret the 4 numbers as an Otype class.
     * @param	s the text to interpret
     * @return	The object type / -1 when error
     **/
    private static final int ntype(String s) {
    	char a[] = s.toCharArray();
    	int ot = 0;
    	int i, h, n;
    	for (i = 0, h = 0; (i < a.length) && (h < bits.length); h++) {
    		ot <<= bits[h];
    		if (a[i] == '.') {
    			i++;
    		}
    		for (n = 0; (i < a.length) && Character.isDigit(a[i]); i++) {
    			n = (n * 10) + Character.digit(a[i], 10);
    		}
    		if (n >= (1 << bits[h])) {
    			return ( -1);
    		}
    		ot |= n;
    	}
    	while (h < bits.length) {
    		ot <<= bits[h++];
    	}
    	return (ot);
    }

    /** Verify the number corresponds to an existing Otype
     * @throws ParseException when the otype is incorrect
     */
    private final void verify() throws ParseException {
    	Odef o = new Odef(otype, null, null, null);
    	if (otypes.indexOf(o) < 0) {
    		throw new ParseException
    		("****Otype: invalid number to assignation: " + otype, 0);
    	}
    }

    /*==================================================================*
     Manipulation of Otypes
     *==================================================================*/

    /** Truncate the otype to some upper level (e.g. Algol --> V*)
     * @param	lev the level (between 1 and 4)
     **/
    public final void truncate(int lev) {
    	if (!initialized) {
    		init();
    	}
    	if (lev < 0) {
    		lev = 0;
    	}
    	if (lev > bits.length) {
    		lev = bits.length;
    	}
    	otype &= Nlev[lev];
    }

    /** Check if is a subtype (in same branch)
     * @param	t1 the other Otype
     * @return	true t1 is an upper class
     **/
    public final boolean implies(Otype t1) {
    	return (implies(otype, t1.otype));
    }

    /**
     * Checks whether t0 implies t1
     * @param t0 first otype
     * @param t1 second otype
     * @return true if t0 implies t1
     */
    private static final boolean implies(int t0, int t1) {
    	int lev;
    	if (!initialized) {
    		init();
    	}
    	lev = level(t1);
    	return ( (t0 & Nlev[lev]) == t1);
    }

    /** Check if the 2 Otypes are compatible
     * @param	ot1 the other Otype
     * @return	true t1 is compatible
     **/
    public final boolean agrees(Otype ot1) {
    	Otype type, clas;
    	int i;
    	Odef o;
    	if (ot1.otype == otype) {
    		return (true);
    	}
    	// Order in clas (less accurate) / type (more accurate)
    	type = merge(ot1);
    	clas = ot1 == type ? this : ot1;
    	if (type.implies(clas)) {
    		return (true);
    	}
    	for (i = 0; i < otrees.size(); i += 2) {
    		o = (Odef) otrees.elementAt(i);
    		if (!implies(type.otype, o.otype)) {
    			continue;
    		}
    		o = (Odef) otrees.elementAt(i + 1);
    		if (implies(o.otype, clas.otype)) {
    			return (true);
    		}
    	}
    	return (false);
    }

    /** Choose the most accurate Otype: the deepest one ine the hierarchy.
     * @param	ot1 a second otype
     * @return	the 'most accurate' otype
     **/
    public final Otype merge(Otype ot1) {
    	int lev, lev1;
    	if (!initialized) {
    		init();
    	}
    	lev = level(otype);
    	lev1 = level(ot1.otype);
    	/* The otype should be UNSIGNED --- but unsigned int in not
    	 known in Java. Therefore compare the right-shifted values
    	 */
    	if (lev == lev1) {
    		return
    			( (otype >>> 1) > (ot1.otype >>> 1) ? this : ot1);
    	}
    	if (lev < lev1) {
    		return (ot1);
    	}
    	return (this);
    }

    /*==================================================================*
     		Setting an Otype from input details
     *==================================================================*/

    /** Convert an integer into an Otype
     * @param	o a number representing an otype
     * @throws ParseException when the text can' be interpreted.
     **/
    protected final void set(int o) throws ParseException {
    	if (!initialized) {
    		init();
    	}
    	otype = o;
    	verify();
    }

    /** Convert a text into an object type
     * @param	text the text representing the otype, either in numeric,
     *		or as one of the available abbreviations
     * @throws ParseException when the text can' be interpreted.
     **/
    public final void set(String text) throws ParseException {
    	Odef odef;
    	if (!initialized) {
    		init();
    	}
    	odef = (Odef) Hsymbols.get(text.toLowerCase());
    	if (odef != null) {
    		otype = odef.otype;
    	}
    	else if (text.length() < 1) {
    		otype = 0;
    	}
    	else if (Character.isDigit(text.charAt(0))) {
    		otype = ntype(text);
    		verify();
    	}
    	else {
    		throw new ParseException("****Otype: " + text, 0);
    	}
    }

    /*==================================================================*
     		Give the list of Otypes
     *==================================================================*/

    /** List in an enumeration all Otypes belonging to a class.
     * @return	An enumeration of Otypes in subtree
     **/
    public Enumeration list() {
    	return new Enumeration() {
    	    int o = otype; // Current otype
    	    int mask = 0;
    	    int pos = -1; // Index in otypes
    	    int ori = otype; // Originally asked type
    	    int omask = 0; // Mask corresponding to original type
    	    int inc = 1; // Index in otrees
    	    public boolean hasMoreElements() {
    	    	Odef odef;
    	    	//if(DEBUG) System.out.println("....hasMore(class=" + o 
		//   + ") pos=" + pos + ", inc=" + inc);
    	    	if (pos < 0) {
    	    		if (!initialized) {
    	    			init();
    	    		}
    	    		odef = new Odef(o, null, null, null);
    	    		pos = otypes.indexOf(odef);
    	    		omask = mask = Nlev[level(o)];
    	    		return (pos >= 0);
    	    	}
    	    	if (pos < otypes.size()) {
    	    		odef = (Odef) otypes.elementAt(pos);
    	    		if ( (odef.otype & mask) == o) {
    	    			return (true);
    	    		}
    	    	}
    	    	// Are there subtrees ? [0]=subtree [1]=class
    	    	while (inc < otrees.size()) {
    	    		odef = (Odef) otrees.elementAt(inc);
    	    		if ( (odef.otype & omask) == ori) {
    	    			break;
    	    		}
    	    		inc += 2;
    	    	}
    	    	if (inc >= otrees.size()) {
    	    		return (false);
    	    	}
    	    	odef = (Odef) otrees.elementAt(inc - 1);
    	    	pos = otypes.indexOf(odef);
    	    	o = odef.otype;
    	    	mask = Nlev[level(o)];
    	    	inc += 2;
    	    	return (true);
    	    }

    	    public Object nextElement() {
    	    	Odef odef;
    	    	Otype e;
    	    	odef = (Odef) otypes.elementAt(pos);
    	    	e = new Otype();
    	    	e.otype = odef.otype;
    	    	pos++;
    	    	return (e);
    	    }
    	};
    }

    /*==================================================================*
      			Edition
     *==================================================================*/

    /** Default Edition of the Otype value
     * @param	choice 0=numeric, 1=standard, 2=verbose, 3=3-letter
     * @return	the meaning of the Object Type
     */
    public final String toString(int choice) {
    	Odef o;
    	int i;
    	if (choice == 0) {
    		StringBuffer b = new StringBuffer(40);
    		edit(b, otype);
    		return ("" + b);
    	}
    	o = new Odef(otype, null, null, null);
    	//if(DEBUG) o.dump();
    	i = otypes.indexOf(o);
    	//for (i=0; i<otypes.size() ; i++) {
    	//   System.out.println(o.equals((Odef)otypes.elementAt(i)));
    	//}
    	o = (Odef) otypes.elementAt(i);
    	if (choice == 1) {
    		return (o.symb);
    	}
    	if (choice == 2) {
    		return (o.expl);
    	}
    	if (choice == 3) {
    		return (o.abbr);
    	}
    	return (null);
    }

    /** 
     * Default Edition of the Otype value
     * @return	the "standard" explanation of hte object type
     **/
    public final String toString() {
    	return (this.toString(1));
    }

    /** 
     * Dump contents to stdout
     * @return	the "standard" explanation of hte object type
     **/
    public final void dump() {
    	System.out.println("....Otype: " + Integer.toHexString(otype));
    }

    /**
     * Methode qui permet de transformer un otype decimal en hexadecimal
     * @return chaine representant la valeur de otype en hexadecimal
     */
    public String numericToHexa() {
    	StringBuffer str = new StringBuffer();
    	StringTokenizer st = new StringTokenizer(this.toString(0), ".");
    	while (st.hasMoreTokens()) {
    		String shex = Integer.toHexString(Integer.parseInt(st.nextToken()));
    		if (shex.length() == 1) {
    			shex = "0" + shex;
    		}
    		str.append(shex);
//			Parameters p = new Parameters();
//			str.append(Format.sprintf("%02x",p.add(Integer.parseInt(st.nextToken()))));
    	}
    	return str.toString();
    }

    /**
     * Methode qui transforme une chaine de caractere hexadecimale en otype sous forme numerique
     * @param strHex chaine representant un otype sous forme hexadecimale
     * @return otype sous forme numerique
     */
    public static String hexaToNumeric(String strHex) {
    	StringBuffer strtmp = new StringBuffer(strHex);
    	StringBuffer str = new StringBuffer();

    	if (strHex.compareTo("0") == 0) {
    		str.append("00.00.00.00");
    	}
    	else {
    		if (strHex.charAt(0) != '0') {
    			strtmp.insert(0, "0");
    		}
    		for (int i = 0; i < strtmp.length(); i += 2) {
    			str.append(Integer.parseInt(String.valueOf(strtmp.substring(i,
    				i + 2)), 16)).append("."); //Chaque caractere hexa est transforme en decimal
    		}
    		str.delete(str.length() - 1, str.length());
    	}

    	return str.toString();
    }

        /**
         * MOD-BB 21-01-04 Ajout de cette methode.
         * Test d'egalite de Otype.
         * @param o Objet a comparer.
         * @return Vrai si o est identique a this, faux sinon.
         */
        public boolean equals(Object o)
        {
          boolean res = false;
          if(o instanceof Otype)
            res = ((Otype)o).otype == this.otype;
          return res;
        }
}

/**
 * Otype Definition object
 * This internal class defines the basic Otype symbols
 * loaded from a file.
 */
class Odef implements Serializable {
    public int otype; // The coded object type
    public String symb; // Main symbol of the Otype
    public String abbr; // 3-letter abbreviation of otype
    public String expl; // Long-text of the Symbol

   /**
    * Constructor of a otype object
    * @param o otype numeric value
    * @param s otype symbol (short name)
    * @param a otype abbreviation
    * @param text otype description
    */
    public Odef(int o, String s, String a, String text) {
    	//System.out.println("...Odef: " + s + " -- " + a + " -- " + text);
    	otype = o;
    	symb = s;
    	expl = text;
    	abbr = a;
    	if (a != null) {
    		int len = a.length();
    		if (len < 3) {
    			abbr = a + "   ".substring(0, 3 - len);
    		}
    	}
    }

   /**
    * Comparison of 2 Otypes
    * @param o Object for which we would like to compare the type
    * @return true if the two otypes are identical
   **/
    public final boolean equals(Object o) {
    	Odef odef = (Odef) o;
    	//System.out.println("....Compare " + this + " / " + odef);
    	return (otype == odef.otype);
    }

   /**
    * Display the object type in a string
    * @return the string containing the otype
    */
    public final String toString() {
    	StringBuffer b = new StringBuffer(80);
    	Otype.edit(b, otype);
    	b.append(": ");
    		if (abbr != null) {
    		b.append(abbr);
    		b.append(' ');
    		b.append('(');
    		b.append(expl);
    		b.append(')');
    	}
    	return ("" + b);
    }

   /**
    * Display for debugging purposes the otype on stdout
    */
    public final void dump() {
    	System.out.println("....Otypedef: " + toString());
    }
}

