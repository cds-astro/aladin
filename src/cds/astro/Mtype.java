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

import java.io.*;

/**
 * @author Marc Wenger/CDS
 * @version 1.0
 * @version 1.1 Jan 2004 (BB) ajout de la methode equals.
 */

/*++++++
.PURPOSE Converts a string containing a morphological class to a long integer;
.RETURNS -1 for error, 0 for completely coded string, 1 if incomplete
.REMARKS If an error is encountered, message logged.
--------*/

public class Mtype implements Serializable {
	private static final int MT_miss = 0x100;
	private static final int MT_u1 = 0x2000;
	private static final int MT_u2 = 0x20;
	private static final int MT_p = 0x10;
	private static final int MT_r = 0x08;
	private static final int MT_s = 0x04;
	private static final int MT_D = 0x02;
	// Sequence of Spiral Galaxies
	private static final String sspiral = "0abcdm";
	private static final char[] digits = {
		'0','1','2','3','4','5','6','7','8','9'};

	int mt;
	String str;
	int pos, lgstr;
	char cc;
	char colon;
	int incomplete, barred, format;

	public Mtype() {
		mt = 0x80800000;
	}
	public Mtype(String str) {
		set(str);
	}

	/**
	 * Sets the coded value of a string representing a morphological type
	 *
	 */
	public int set(String str) {
		this.str = str;
		lgstr = str.length();
		pos = 0;

		int m1, m2;
		int	mp=0, second=0;
		incomplete = 0;

		// Check first if deVaucouleurs / Hubble
		nextChar();
		if (Character.isDigit(cc))
			m1 = getDV();
		else if (Character.isLetter(cc))
			m1 = getHubble();
		else if (Character.isDigit(followingChar()))
			m1 = getDV();
		else
			m1 = getHubble();

		// Undefined type is universal..
		if (m1 == -128)
			format = 0;

		// Save Pecularities
		mp = (barred << 14) | (format << 10);
		if (colon != '\0')	mp |= MT_u1;
		barred = colon = 0;

		 /* Get Second Type, if any	*/

		m2 = -128; second = 0;
		if (cc == '+')	{
			mp |= 0x1000;
			second = 1;
			nextChar();
		} else if (cc == '/')	{
			second = 1;
			nextChar();
		}

//		if (second == 1 && format == 2) {
		if (second == 1 && Character.isLetter(cc)) {
			// Only for Hubble type
			if (colon != '\0')	mp |= MT_u2;
			m2 = getHubble();
				/* m2 = (format == 2 ? get_hubble() : get_dV()); */
			if (barred != 0) mp |= (barred << 6);
			if (colon != 0)
				mp |= MT_u2;
		}

		// Pecularities

		while(cc != '\0') {
			switch(Character.toLowerCase(cc)) {
				case 'p':
					mp |= MT_p;
					match("peculiar");
					break;
				case 'r':
					mp |= MT_r;
					break;
				case 's':
					mp |= MT_s;
					break;
				case 'd':
					mp |= MT_D;
					if (followingChar() == 'b')
						match("dble");
					else
						match("double");
					break;
				default:
					mp |= MT_miss;
					break;
			}
			nextChar();
		}

		if (incomplete != 0)
			mp |= MT_miss;
		mt = (m1 << 24) | ((m2&0xff)<<16) | (mp&0xffff);
		/*
		  if (Str.error)
			 mp = -1;
		*/
//		return (mp & MT_miss) == 0 ? 0 : 1;
		return (mp & MT_miss);
	}

	public int get() {
		return mt;
	}

	/**
	 * Edits a coded morphological type
	 * @return the edited morphological type
	 */

	public String toString() {
		StringBuffer s = new StringBuffer(24);
		int	m;

		format = (mt >> 10) & 3;
		barred = (mt >> 14) & 3;
		if (mt == 0x80800000)	return("");

		m = mt >> 24;
		if   (m == -128) s.append('?');
		else if (format < 2)	s.append(edDV (m, format));
		else s.append(edHubble(m));
		if ((mt & MT_u1) != 0)	s.append(':');

		format = 0;
		barred = (mt >> 6) & 3;
		m = (mt << 8) >> 24;
		if ((mt & 0x1000) != 0) s.append('+');
		else if (m != -128)	s.append('/');
		if (m != -128)	s.append(edHubble(m));
		if ((mt & MT_u2) != 0)	s.append(':');

		//	Now, edit Pecularities

		if ((mt & MT_r) != 0)	s.append('r');
		if ((mt & MT_s) != 0)	s.append('s');
		if ((mt & MT_p) != 0)	s.append('p');
		if ((mt & MT_D) != 0)	s.append('D');
		if ((mt & MT_miss) != 0)	s.append("...");

		return(s.toString());
}

	/**
	 * Get next char (skip blanks), and translate special symbols () :
	 * I (Irregular) may be written I or Irr
	 * @return The char, 0 when NULL
	 */
	private char nextChar() {
		while (pos < lgstr) {
			cc = str.charAt(pos++);
			switch (cc) {
				case ' ': case '\t':
					continue;
				case ',':
					continue;
				case '(':
					continue;
				case ')': case ':':
					colon = cc;
					continue;
				case 'I':
					if (pos+1 < lgstr &&
						 str.charAt(pos) == 'r' && str.charAt(pos+1) == 'r') pos += 2;
					break;
				case '.':
					if (pos+1 < lgstr &&
						 str.charAt(pos) == '.' && str.charAt(pos+1) == '.') {
						pos += 2;
						incomplete = 1;
						continue;
					}
					break;
				default:
					//pos++;
			}
			return cc;
		}
		cc = '\0';
		return cc;
	}

	private char followingChar() {
		return pos < lgstr ? str.charAt(pos) : '\0';
	}

	/**
	 * Looks for a specific string in the main analysed string
	 * it is assumed that s[0] == cc
	 */
	private int match(String s) {
		int i,j;
		for (i=pos, j=1; i<lgstr && j<s.length(); i++,j++) {
			if (Character.toLowerCase(str.charAt(i)) != Character.toLowerCase(s.charAt(j)))
				 break;
		}
		pos += (i-pos);   // skip the matched letters
		return j-1;
	}

	/**
	 * Returns the Hubble code of a morph. type
	 */
	private int getHubble() {
		int	mt, i;
		char old_colon;

		format = 2;			/* It'a s Hubble type	*/
		colon = '\0';
		barred = 0;
		mt = -128;				/*     Default 	*/

		switch(cc) {
			case '?':
				nextChar();	/* NO break	*/
			case 0:
				break;
			case 'c':	case 'C':	/* Compact */
			case 'd':  	case 'D':	/* Dwarf (or double?)  */
				if (cc == 'c' || cc == 'C') {
					// compact
					match("compact");
					mt = -60;
				} else {
					// dwarf ou double (dble)
					if (str.charAt(pos) == 'o' || str.charAt(pos) == 'b')	break;
					mt = -50;
				}
				// [compact] dwarf   Followed by E / G / Irr
				match("dwarf");
				nextChar();
				switch(cc) {
					case 'E':
						nextChar();
						break;
					case 'G':
						nextChar();
					default:
						mt -= 5;
						break;
					case 'I':
						nextChar();
						mt = 80 - mt/2;
						break;
				}
				break;
			case 'E':	/*Elliptical	*/
				mt = -40;
				nextChar(); 	break;
			case 'L':	/* Lenticular	*/
				mt = -20;
				nextChar(); 	break;
			case 'S':	/* Spiral	*/
				nextChar();
				if (cc == 'B')	{	/* Barred Spiral	*/
					old_colon = colon;	colon = 0;
					barred = 1;	nextChar();
					if (colon != '\0') 	barred = 2;
					colon = old_colon;
				}
				// sspiral = une String, + indexOf(char)
				if ((i = sspiral.indexOf(cc)) >= 0) {
					nextChar();
					mt = i*20 - 10;
					if (i+1 < sspiral.length() && sspiral.charAt(i+1) == cc) {
						nextChar();
						mt += 10;
					}
					if (mt < 0)	mt = -20;	/* Only for S0	*/
				}
				else mt = 45;			/* S alone ...	*/
				break;
			case 'I':	/* Irregular	*/
				nextChar();	mt = 100;
				if (cc == 'm') {
					nextChar();
					mt = 101;
				}
				break;
			default:	/* May be a pecularity	*/
				break;
		}
	//	System.out.println("....get_hubble returns "+mt);
		return(mt);
	}

	/**
	 * Converts the string to deVaucouleurs... + Bar.
	 * @return the class
	 */
	private int getDV() {
		int sign, mt;
		char old_colon;

		mt = 0;
		sign = format = barred = 0;
		colon = '\0';

		if (cc == '-')	{
			nextChar();
			sign = 1;
		}
		while (Character.isDigit(cc)) {
			mt = mt*10 + (cc-'0');
			nextChar();
		}

		mt *= 10;
		if (cc == '.' && str.charAt(pos) != '.') {
			/* There is a decimal */
			nextChar();
			format = 1;
			mt += (cc - '0');
			nextChar();
		}
		if (sign == 1)	mt = -mt;

		if (cc == 'B')	{
			/* Barred Spiral	*/
			old_colon = colon;
			colon = 0;
			barred = 1;
			nextChar();
			if (colon != 0) 	barred = 2;
			colon = old_colon;
		}

		// Check that mt is in correct interval
/*
	 if (mt <= -70)	error("deVaucouleurs Type too small", p);
	 if (mt >  110)	error("deVaucouleurs Type too large", p);
*/
//		System.out.println("....get_dV returns "+mt);
		return mt;
	}

	/**
	 * Edit de Vaucouleurs morphological type
	 * @param mt morphological type (-128 / 127)
	 * @param nd Number of decimals (0 or 1)
	 * @return Edited string
	 */
	private String edDV(int mt, int nd) {
		StringBuffer s = new StringBuffer(8);
		if (mt <= -128)
			return "";

		if (mt >= 100)	s.append('1');
		else if (mt < 0)	{
			s.append('-');
			mt = -mt;
		}
		else s.append(' ');

		mt %= 100;
		s.append(digits[mt/10]);
		if (nd > 0) {
			s.append('.');
			s.append(digits[mt % 10]);
		}
		if (barred != 0)  {
			s.append('B');
			if ((barred & 2) != 0) s.append(':');
		}
		return (s.toString());
	}


	/**
	 * Edit Hubble morphological type
	 * @param mt Morphological type (-128 / 127)
	 * @return Edited string
	 */
	private String edHubble(int mt) {
		StringBuffer s = new StringBuffer(24);
		int i, k, bar;

		if (mt <= -128)
			return "";

		bar = barred;

		if (mt >= -20 && mt <= 90)   {	/* Spiral Galaxies */
			i = (mt < 0 ? 0 : mt+10);
			s.append('S');
			if (bar > 0)  {
				s.append('B');
				if ((bar & 2) != 0) s.append(':');
				bar = 0;
			}
			if (i != 55) {
				k = i/20;
				s.append(sspiral.charAt(k));
				switch (i%20) {
					case 0: break;
					case 10: s.append(sspiral.charAt(k+1)); break;
					default: s.append('?'); break;
				}
			}
		} else if (mt == -40) {
			s.append('E');
		} else if (mt <= -50) {
			k = -50 - mt;
			s.append(k/10 != 0 ? 'c' : 'd');
			s.append(k%10 != 0 ? 'G' : 'E');
			if (k%5 != 0 || k > 15) s.append('?');
		}
		else if (mt == 100)	s.append('I');
		else if (mt == 101)	s.append("Im");
		else if (mt == 105)	s.append("dI");
		else if (mt == 110)	s.append("cI");
		else	s.append('?');

		if (bar != 0)  {
			s.append('B');
			if ((bar & 2) != 0) s.append(':');
		}

		return s.toString();
}

	public static void main(String[] args) {
		String str = null;
		BufferedReader rdr = null;
		boolean stdin = true;
		int ctlig = 0;

		if (args.length == 0)
//		if (true)
			rdr = new BufferedReader(new InputStreamReader(System.in));
		else {
			try {
				rdr = new BufferedReader(new InputStreamReader(
						new FileInputStream(args[0])));
			} catch (Exception e) {
				System.err.println("Open file error: "+e);
			}
			stdin = false;
		}
		while (true) {
			if (stdin) System.out.print("morph type :");
			try {
				str = rdr.readLine();
			} catch (IOException e) {}
			if (str == null || str.compareTo("fin") == 0) break;
			ctlig++;
			Mtype mtyp1 = new Mtype(str);
			String str1 = mtyp1.toString().trim();
			Mtype mtyp2 = new Mtype(str1);
			String str2 = mtyp2.toString().trim();
			if (stdin) System.out.println(str+" -> "+str1+" -> "+str2);
			else {
				if (str.compareTo(str1) != 0 || str1.compareTo(str2) != 0)
					System.out.println("Err ligne "+ctlig+" : "+str+" -> "+str1+" -> "+str2);
//				else
//					System.out.println("Ok  ligne "+ctlig+" : "+str+" -> "+str1+" -> "+str2);
			}
		}
	}

        /**
   * MOD-BB 21-01-04 Ajout de cette methode.
   * Comparaison de Mtype.
   * @param o Objet a comparer.
   * @return Vrai si o est identique a this, faux sinon.
   */
   public boolean equals(Object o)
   {
     boolean res = false;
     if(o instanceof Mtype)
     {
       Mtype m = (Mtype)o;
       res = m.barred == this.barred &&
           m.cc == this.cc &&
           m.colon == this.colon &&
           m.format == this.format &&
           m.incomplete == this.incomplete &&
           m.lgstr == this.lgstr &&
           m.mt == this.mt &&
           m.pos == this.pos &&
           (m.str == null ? this.str == null : m.str.equals(this.str));
     }
     return res;
   }

}