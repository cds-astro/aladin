// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.astro;

/*==================================================================
                Parsing class
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * This class interprets text and is able to convert to numbers,
 * or find a symbol in a list of symbols. 
 * Numbers can use <B>...&times;10+/-exp</B> as well as 
 *                 <B>...e+/-exp</B> notation;
 * numbers may also be expressed in sexagesimal.<P>
 * This class returns, for the most recently analyzed number:<UL> 
 * <LI> the number of <em>decimals</em> (-1 indicates no decimal point)
 * <LI> the number of significant <em>digits</em> 
 * <LI> the <em>format</em> of the number, as <tt>Parsing.DECIMAL</tt>,
 * 	<tt>Parsing.EFORMAT</tt>, <tt>Parsing.FACTOR</tt>, or various
 *	calendar or sexagesimal forms with the interpretation of the symbols 
 *	<B> <tt>: Y M D h m s d o &deg;; ' "</tt></B>
 * <LI> the <em>status</em>, currently used only in sexagesimal and
 * 	complex parsings:
 * 	a <tt>Parsing.WARNING</tt> indicates the last component is 60,
 * 	while a <tt>Parsing.ERROR</tt> indicates a component &gt;60.
 * </UL>
 * @author Francois Ochsenbein -- francois@astro.u-strasbg.fr
 * @version 0.7 : 07-Sep-2002 inside Unit class<br>
 * @version 1.0 : 28-Feb-2004 separated Udef class 
 * @version 1.1 : 10-Aug-2004 Homogenize Parsing + Editing classes
 * @version 1.2 : 02-Sep-2006 Interpret dates
 * @version 1.3 : 26-Oct-2006 Accept blanks after +/-; added matchingQuote
 * 				and toString(int len)
 * @version 1.4 : 06-May-2007 parseSexa2()
 * @version 1.5 : 22-May-2007 parseSexa2(): bug for unit symbols
 * @version 1.6 : 28-Jan-2008 parseSexa2(): bug for unit symbols
 * @version 1.7 : 01-Jul-2008 parseSexa2(): bug for no sign between RA/Dec
 * @version 1.8 : 15-Apr-2009 parseIAU()
 * @version 1.81: 08-May-2009 parseArray accepts commas between numbers
 * @version 1.82: 02-Feb-2010 parse_sexa bug fix 
 * @version 2.0 : 02-Feb-2019 don't accept a single point in date.
 *                      parseArray: accept it as bracketed ()[]{}
 *                      advance / set / gobbleSpaces: return boolean.
 *                      setError(String) added
 */

public class Parsing extends Astroformat {
    /** The text as an array of chars */
    public char[] a ;		// The text as an array of chars
    /** The length of the array, can be shorter than the actual size */
    public int length ;		// Length of text
    /** The current position in the array */
    public int pos ;		// Current position in text
    /** The flags contains the status of the last parsed number.
     * It contains the following parts:
     * Mask 0x000000ff: number of decimals+1 (0 = no decimal point)
     * Mask 0x0000ff00: number of significant digits (0 = no digit)
     * Mask 0x00ff0000: format of the number, see Astroformat
     * Mask 0x7f000000: error detected in sexagesimal:
     * 			1 (last number equal to 60)
     * 			2 (one value is larger than 60)
     * 			3 (final error)
     * Mask 0x80000000: Internally ask for a reset.
    **/
    private int flags;
    /** Error message, if any (only in parseComplex) */
    public String error_message = null;
    /** The status of the last parsing operation is OK */
    static public final int OK=0;
    /** The last (sexagesimal) parsing has minutes or seconds equal to 60. */
    static public final int WARNING=1;
    /** The last (sexagesimal) parsing has a component greater than 60 */
    static public final int ERROR=2;
    /** Debugging level */
    static public boolean DEBUG = false;
    ///** A terminal error encountered, error_message set */
    //static public final int FATAL=3;
    /** The format of the numbers are defined in parent class Astroformat */
    /** The acceptable abbreviations for h m s.
     * The unit is 2=: 4=deg 6=hour 1=1/60, 9=1/3600, 3=2+1, 11=9+2
     * flag 16 for special symbol &deg; ' "
     */
    static private final char[] sexa_letter = { ':', 'm', 's' };
    static private final char[] sexa_symbol = { '\u00B0', '\'', '"' };
    static private final char[] sexa_symb1  = {
	':', 'd', 'o', '\u00B0', 'h', 'm', 's', '\'', '"'
    };
    static private final byte[] sexa_unit1  = {
	 2 ,  4 ,  4 , 16|4,  6 , 1|6, 9|6,16|5, 20|9
    };
    /** The different sorts of parenthesis or brackets **/
    static public final char[] brackets = "()[]{}<>\u3008\u3009\u3010\u3011".toCharArray();
    /** The different sorts of error matchers */
    static private final String[] error_symb = 
                      { "+/-", "+", "-", "\u00B1", "[" };

    /*==============================================================
                Constructors
     *==============================================================*/

    /** 
     * Create a Parsing unit from a string
     * @param s the text to parse
     */
    public Parsing(String s) {
	this(s, 0);
    }

    /** 
     * Create a Parsing unit from a string + offset
     * @param s the text to parse
     * @param offset position of first character in <i>s</i> to consider.
     */
    public Parsing(String s, int offset) {
	super();
	length = s.length();
	a = s.toCharArray();
	pos = offset;
    }

    /*==============================================================
                Change the parsing contents
     *==============================================================*/

    /** 
     * Force the position within the Parsing piece
     * @param n the position -- if necessary, adjusted between 0 and length
     * @return true when done, false when bounded
     */
    public final boolean set(int n) {
        boolean ok = (n>=0) && (n<length);
	if(ok) pos = n ;
        else if(n<0) pos = 0;
        else         pos = length;
        return(ok);
    }

    /** 
     * Install a new text in the Parsing
     * @param text the string to parse
     */
    public final void set(String text) {
	a = text.toCharArray();
	length = text.length();
	pos = 0;
    }

    /** 
     * Remove the error status
     * @return true if it was in error.
     */
    public final boolean clearError() {
        boolean state = (flags&0x2000000)!=0;
        flags &= ~0x2000000;
	return(state);
    }

    /** 
     * Install an error message
     * @param text the message text
     * @return the previous error message
     */
    public final String setError(String text) {
        String msg = this.error_message;
        flags |= 0x2000000;
	error_message = text;
	return(msg);
    }

    /** 
     * Move (forward / backward) in the Parsing piece
     * @param n the value of the step
     * @return true if possible
     */
    public final boolean advance(int n) {
	return this.set(pos+n) ;
    }

    /*==============================================================
                Interpretations of symbols
     *==============================================================*/

    /**
     * Get the current char
     * @return the current character in the parsing buffer.
     */
    public final char currentChar() {
	if (pos >= length) return(Character.MIN_VALUE);
	return(a[pos]);
    }

    /**
     * Skip the spaces in the Parsing unit
     * @return true if something exists, false when we're at end.
     */
    public final boolean gobbleSpaces() {
        boolean status;
	while ((status=pos<length) && Character.isWhitespace(a[pos])) pos++;
        return(status);
    }

    /** 
     * Try to match a Character from a list.
     *  @param	tSymbol table of Symbols
     *  @return the index in table of Symbols (-1 if not found)
    **/ 
    public final int lookup(char[] tSymbol) {
      char c;
      int i;
        if (pos >= length) return(-1);
        c = a[pos];
        for (i=0; i<tSymbol.length; i++) {
	    if (tSymbol[i] == c) {
		pos += 1;
		return(i);
	    }
        }
        return(-1) ;	// Not Found
    }

    /** 
     * Verify we're starting by a specific character
     *  @param	c the character to match, possibly preceded by blanks.
     *  @return true (position changed)/false
    **/ 
    public final boolean match(char c) {
      int posini = pos;
	gobbleSpaces();
	if ((pos<length) && (a[pos++] == c))
	    return(true);
	pos = posini;
	return(false);
    }

    /** 
     * Verify we're starting by a specific string.
     *  Note that an, empty string will always match...
     *  @param	text the string that should be there.
     *  @return true (position changed)/false
    **/ 
    public final boolean match(String text) {
      int posini = pos;
      int len = text.length();
      boolean matching = (len+pos) <= length;
      int i;
        for (i=0; matching && i<len; i++) 
	    matching = a[pos++] == text.charAt(i);
	if (matching) matching = i==len;
        if (!matching) pos = posini;
	return(matching);
    }

    /** 
     * Verify we're starting by a specific string, case insensitive.
     *  Note that an, empty string will always match...
     *  @param	text the string that should be there.
     *  @return true (position changed)/false
    **/ 
    public final boolean matchIgnoreCase(String text) {
      int posini = pos;
      int len = text.length();
      boolean matching = (len+pos) <= length;
      int i;
        for (i=0; matching && i<len; i++) {
	    matching = Character.toLowerCase(text.charAt(i))
                    == Character.toLowerCase(a[pos++]);
        }
	if (matching) matching = i==len;
        if (!matching) pos = posini;
	return(matching);
    }

    /** 
     * Try to match a Symbol in a defined piece of the text.
     *  The matching is successful when a symbol of the table
     *  with the specified length is matched.
     *  @param	tSymbol table of Symbols
     *  @param	len exact length of text to match
     *  @return the index in table of Symbols (-1 if not found)
    **/ 
    public final int lookup(String[] tSymbol, int len) {
      int i, j;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
        if (pos+len <= length) for (i=0; i<tSymbol.length; i++) {
	    if (tSymbol[i].length() != len) continue ;
	    for (j=0; j<len; j++) {
	        if (tSymbol[i].charAt(j) != a[pos+j]) break;
	    }
	    if (j >= len) {		// Symbol found -- set what's parsed
		pos += len;
	        return(i) ;
	    }
        }
        return(-1) ;	// Not Found
    }

    /** 
     * Try to match a Symbol in the current text.
     * The matching is successful with the first symbol of the
     * list that coincides -- and the position is incremented.
     *  @param  tSymbol a table of Symbols
     *  @return	the index in table of Symbols (-1 if not found)
    **/ 
    public final int lookup(String[] tSymbol) {
      int i, j;
      int maxlen = length - pos;
      int symlen  = 0;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
      	for (i=0; i<tSymbol.length; i++) {
	    symlen = tSymbol[i].length();
            if (symlen > maxlen) continue;
	    for (j=0; j<symlen; j++) {
	        if (tSymbol[i].charAt(j) != a[pos+j]) break;
	    }
	    if (j < symlen) continue ;
	    pos += symlen; 
	    return(i);
      	}
      	return(-1) ;	// Not Found
    }

    /** 
     * Try to match a Symbol in the current text, case insensitive.
     *  @param	tSymbol table of Symbols
     *  @return the index in table of Symbols (-1 if not found)
    **/ 
    public final int lookupIgnoreCase(String[] tSymbol) {
      int i, j;
      int maxlen = length - pos;
      int symlen  = 0;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
	for (i=0; i<tSymbol.length; i++) {
	    symlen = tSymbol[i].length();
	    if (symlen > maxlen) continue;
	    for (j=0; j<symlen; j++) {
		if (Character.toLowerCase(tSymbol[i].charAt(j))
		 != Character.toLowerCase(a[pos+j])) break;
	    }
	    if (j < symlen) continue ;
	    pos += symlen;
	    return(i) ;
        }
        return(-1) ;	// Not Found
    }

    /*==============================================================
                Locate matching parenthesis / Quotes
     *==============================================================*/

    /** 
     * Locate a specific char from the current position.
     * No mouvement in the Parsing structure (pos does not change)
     * @param symb  the symbol to locate
     * @return the position of first occurence of <i>symb</i> following (and including) the
     *         current position (i.e. a value &geq; than the current position);
     *         <b>-1</b> is returned if <i>symb</i> not found.
     */
    public final int indexOf(char symb) {
        for(int i=pos; i<length; i++) {
            if(a[i]==symb) return(i);
        }
        return(-1);
    }

    /** 
     * From current position assumed to contain a parenthesis or bracket,
     * return the location of the corresponding parenthesis.
     * No mouvement in the Parsing structure (pos does not change)
     * @return the position / -1 if not found
     */
    public final int matchingBracket() {
        int posini = pos;
	if (pos >= length) return(-1);
	int i = this.lookup(brackets);
	if (i<0) return(i);	// Current char not in list of brackets...
	pos = posini;		// Reset to original position.
	char c = brackets[i];	// Note that c is also a[pos]
	char o = brackets[i^1];	// This gives the corresponding bracket
	int depth = 1;
	int j = pos;
	if ((i&1)==0) 		// I have a left bracket. Move forward
	  for (++j; j<length; j++) {
	    if (a[j] == c) depth++;
	    else if (a[j] == o) depth--;
	    if (depth == 0) break;
	}
	else 			// I have a right bracket. Move backward.
	  for (--j; j>=0 ; j--) {
	    if (a[j] == c) depth++;
	    else if (a[j] == o) depth--;
	    if (depth == 0) break;
	}
	if (j>=length) j = -1;
	return(j);
    }

    /** 
     * From current position assumed to contain a Quote (' " or `)
     * return location of the matching quote.
     * The next character identical to the current one is searched.
     * No quote escaping allowed.
     * No mouvement in the Parsing structure (pos does not change)
     * Added in V1.3
     * The quote may be escaped by 2 consecutive quotes, or a backslashed quote.
     * @return the position / -1 if not found.
     */
    public final int matchingQuote() {
        int j=pos+1;
        while(j<length) {
            if(a[j]==a[pos]) {
                int j1 = j+1;
                if((j1>=length)||(a[j1]!=a[pos]))
                    break;
                else j++;
            }
            else if(a[j]=='\\') j++;
            j++;
        }
	if (j>=length) j = -1;
	return(j);
    }

    /*==============================================================
                Interpretations for Numbers
     *==============================================================*/

    /** 
     * Verify a '-' sign. Plenty of Unicode chars can be found!
     *  @param  c  char to test
     *  @return	true / false
    **/ 
    public static final boolean isMinus(char c) {  // Interpret the minus sign
        if(c<'\u0080') return(c=='-');	// pure ascii.
	return ((c>='\u2010')&&(c<='\u2015')) ||
		(c=='\u2212')||(c=='\u00ad') ;
    }

    /** 
     * Verify a sign.
     *  @param  c  char to test
     *  @return	true / false
    **/ 
    public static final boolean isSign(char c) {  // Interpret an Integer
	return((c=='+') || isMinus(c)) ;
    }

    /** 
     * Try to match a Sign.
     * No change is made in <em>pos</em> when no sign could be matched;
     *  @return	+1 ('+' found), -1 ('-' found) or 0 (no sign)
    **/ 
    public final int parseSign() {	// Interpret an Integer
        if (pos<length) {
	    char c = a[pos++];
	    if (c == '+')   return(1);
	    if (isMinus(c)) return(-1);
	    --pos;
	}
	return(0);
    }

    /** 
     * Try to match a Positive Integer Number.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @return	the integer read --  0 by default.
    **/ 
    private final int parseNum() {	// Interpret an Integer
      int i = pos;
      int val = 0;
	while ((i<length) && Character.isDigit(a[i])) i++;
	if (i>pos) val = Integer.parseInt(new String(a, pos, i-pos));
	// Compute the number of significant digits, and set ZERO_FILL
	if (flags == 0) {
	    flags = (i-pos) << 8;
	    if ((flags>256) && (a[pos]=='0') && Character.isDigit(a[pos+1]))
	        flags |= ZERO_FILL << 16;
	}
	// System.out.println("....ParseNum("+this.toString()+"): val="+val
	//                 +", flags=0x"+Integer.toHexString(flags));
	pos = i ;
	return(val);
    }

    /** 
     * Try to match a Positive Integer Number made of up to nd digits.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @param  nd  max. number of digits to match.
     *  @return	the integer read --  0 by default.
    **/ 
    private final int parseNum(int nd) {  // Interpret an Integer
      int i = pos;
      int val = 0;
	while ((nd>0) && (i<length)) {
	    if (Character.isDigit(a[i])) val = (val*10) + (a[i]&0xf);
	    else if ((val == 0) && Character.isWhitespace(a[i])) ;
	    else break;
	    --nd; ++i;
	}
	if (flags == 0) {   // Number of digits
	    flags = (i-pos) << 8;
	}
	pos = i;
	return(val);
    }

    /** 
     * Try to match a Long Positive Integer Number.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @return	the long integer read --  0 by default.
    **/ 
    private final long parseNum8() {	// Interpret an Integer
      int i = pos;
      long val = 0;
	while ((i<length) && Character.isDigit(a[i])) i++;
	if (i>pos) val = Long.parseLong(new String(a, pos, i-pos));
	// Compute the number of significant digits, and set ZERO_FILL
	if (flags == 0) {
	    flags = (i-pos) << 8;
	    if ((flags>256) && (a[pos]=='0') && Character.isDigit(a[pos+1]))
	        flags |= ZERO_FILL << 16;
	}
	// System.out.println("....ParseLong("+this.toString()+"): val="+val
	//                 +", flags=0x"+Integer.toHexString(flags));
	pos = i ;
	return(val);
    }

    /** 
     * Try to match one of the possible NaN representations.
     * No change is made in <em>pos</em> when no NULL representation could be
     * matched.
     *  @return	true (a NaN representation found) / false
    **/ 
    public final boolean parseNaN() {		// Interpret NaN
      int posini = pos;
      int postart, i;
	gobbleSpaces();
        if (pos>=length) { pos = posini; return(false); }
	postart = pos;

	for (i=0; i<nulls.length; i++) {
	    if (nulls[i] == null) continue;
	    pos = postart;
	    if (match(nulls[i])) {
		char c = a[pos-1];
		while ((pos<length) && (a[pos] == c)) pos++;
		if (pos<length) {	// Verify the word stops here.
		    c = a[pos];
		    if (Character.isLetterOrDigit(c)) continue;
		    if (c == '.') continue;
		    return(true);
		}
	    }
	}
	pos = posini; 
	return(false);
    }

    /** 
     * Try to match a Positive Integer Number.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @return	the integer read --  0 by default, 0x80000000 for NaN
    **/ 
    public final int parsePositiveInt() {	// Interpret an Integer
      int posini = pos;
      int val = 0;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
	while ((pos < length) && Character.isWhitespace(a[pos])) pos++;
	if (pos >= length) { pos = posini; return(val); }
	val = this.parseNum();
	if (flags == 0) {	// Try NaN
	    if (this.parseNaN()) val = 0x80000000;
	    else pos = posini;
	}
	return(val);
    }

    /** 
     * Try to match an Integer Number.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @return	the integer read --  0 by default, 0x80000000 for NaN
    **/ 
    public final int parseInt() {	// Interpret an Integer
      int posini = pos;
      int val = 0;
      int pec = 0;	// Pecularities found here: sign edited.
      int sign = 0;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
	gobbleSpaces();
	if (pos >= length) { pos = posini; return(val); }
	if ((sign = parseSign())>0) pec = SIGN_EDIT;
	val = this.parseNum();
	if (sign<0) val = -val;
	if (flags == 0) {
	    if (this.parseNaN()) val = 0x80000000;
	    else pos = posini;
	}
	else flags |= pec << 16 ;
	return(val);
    }

    /** 
     * Try to match a Long Integer Number.
     * No change is made in <em>pos</em> when no number could be matched;
     *  @return	the integer read --  0 by default, 0x80000000 for NaN
    **/ 
    public final long parseLong() {	// Interpret an Integer
      int posini = pos;
      long val = 0;
      int pec = 0;	// Pecularities found here: sign edited.
      int sign = 0;
	if (flags>0) { flags = 0; /* error_message = null;*/ }
	gobbleSpaces();
	if (pos >= length) { pos = posini; return(val); }
	if ((sign = parseSign())>0) pec = SIGN_EDIT;
	val = this.parseNum8();
	if (sign<0) val = -val;
	if (flags == 0) {
	    if (this.parseNaN()) val = 0x8000000000000000L;
	    else pos = posini;
	}
	else flags |= pec << 16 ;
	return(val);
    }

    /** 
     * Internal method which can accept te "x10+/-exp" notation (x10 is true)
     * @param x10 is 10 if the notation e.g. 314.16x10-2 is acceptable.
     * @return	the double which could be interpreted -- 1 by default.
    **/
    private final double parseValue(int x10) {
      double val = 1;
      int posini = pos;
      int i, i1, nd;
      int hasexpo = 0;
      int e = 0;			// Exponent
      int pec = 0;	// Pecularities found here: sign edited, zero_fill
      char c = Character.MIN_VALUE;	// Terminator for Exponent
      int sign = 0;

	if (flags>0) { flags = 0; /* error_message = null;*/ }
	if (pos >= length) return(val) ;
	if ((sign = parseSign())>0) pec |= SIGN_EDIT;
	// i1 is set to the position of first significant digit
	for (i1=pos; (i1<length) && (a[i1] == '0'); i1++) ;
	// i is set to the position of first non-digit
	for (i=i1; (i<length) && Character.isDigit(a[i]); i++) ;
	if (((i1-pos)>0) && ((i-pos)>1)) pec |= ZERO_FILL;
	if ((i<length) && (a[i] == '.')) {
	    if (i1 == i) 		// No significant digit yet
		for (++i1; (i1<length) && (a[i1] == '0'); i1++) ;
	    else ++i1;			// To take into account the .
	    for (nd = ++i; (i<length) && Character.isDigit(a[i]); i++) ;
	    nd = i - nd;
	}
	else nd = -1;			// Indicates no decimal part
	i1 = i - i1;			// Number of significant digits
	if (i1 == 0) i1 = 1;
	if (i == pos) {			// No digit at all -- could be NaN
	    pos = posini ;
	    if (this.parseNaN()) val = 0./0.;
	    else pos = posini;
	    return(val);
	}

	val = Double.valueOf(new String(a,pos,i-pos)).doubleValue();
	if (sign<0) val = -val;
	pos = i ;			// Could also be this.set(i)

	// Set the number of decimals etc
	// System.out.println("....ParseValue("+this.toString()+"): val="+val
	//  +", flags=0x"+Integer.toHexString(flags)+", pec="+pec+", nd="+nd);
	if (flags == 0) flags = (nd+1) | (i1<<8) | (pec << 16);

	if (i >= (length-1)) return(val);
	if (x10 == 0) return(val);

	// Look for an Exponent part -- We may already have read 10 in 10^8
	//  hasexpo is 2 when the '10' already read.
	//  Note that 2^8 is also accepted -- but not 2.0^8
	posini = pos;
	c = Character.MIN_VALUE;	// Terminator in 10^8^
	if ((a[i] == 'e') || (a[i] == 'E')) { hasexpo = 1; i++; }
	else if (x10>1) {		// Can be x10^8 or just ^8
	    if (nd < 0) hasexpo = 2;
	    if (((a[i] == 'x') || (a[i] == '\u2715')) && (i<=(length-4))) {
		++i ;
		if ((a[i] == '1') && (a[i+1] == '0')) i += 2; 
		hasexpo = 10; 
	    }
	    if (a[i] == '^') { c = '^'; i++; }
	    else if (isSign(a[i]) || Character.isDigit(a[i]));
	    else hasexpo = 0;
	}

	// Interpret the Exponent part
	if (hasexpo != 0) {
	    pos = i;
	    flags |= 0x80000000;	// Turn into negative to avoid modify
	    e = parseInt();
	    flags &= 0x7fffffff;	// Turn into negative to avoid modify
	    if (pos == i) hasexpo = 0;	// No number in exponent ??
	}
	if (hasexpo == 0) 
	    pos = posini;
	else {
	    if (hasexpo == 2) {			// Just 10^8
	        hasexpo = (int)val;		// Normally +/-10
		if (hasexpo < 0) { hasexpo = -hasexpo; val = -1; }
		else val = 1;
	        while (e>0) { val *= hasexpo; e--; }
	        while (e<0) { val /= hasexpo; e++; }
		hasexpo = 0;
	    }
	    else val *= AstroMath.dexp(e);
	    flags |= (hasexpo<<16);
	    // Accept 10^8^ (2 carrets) -- c contains the ^
	    if ((pos<length) && (a[pos] == c)) pos++;
	}
	return(val) ;
    }

    /** 
     * Interpret a real number as (+/-)num.decimals.
     * No change occurs in <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * @return	the double which could be interpreted (NaN if no match)
     */
    public final double parseDecimal() {
      double value = parseValue(0);
        if ((flags&0xff00) == 0) return(0./0.);
	return value;
    }

    /** 
     * Interpret a real number as (+/-)numE+/-pow.
     * No change occurs in <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * @return	the double which could be interpreted (NaN if no match)
     */
    public final double parseDouble() {	// Interpret a Floating-Point
      double value = parseValue(1);
        if ((flags&0xff00) == 0) return(0./0.);
	return value;
    }

    /** 
     * Interpret a real number as (+/-)numx10+pow OR (+/-)numE+pow.
     * No change occurs in the <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * @return	the double which could be interpreted -- 1 by default.
     */
    public final double parseFactor() {
	return parseValue(10);
    }

    /** 
     * Interpret a sexagesimal number.
     * no change occurs in the <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * An error is stored when a component is outside [0,60[.
     * The exponent is accepted.
     * @param   comp 0 (no blank, IAU-style), 1 or 2.
     * @return	the double which could be interpreted -- NaN by default.
     */
    private final double parse_sexa(int comp) {	// Interpret 1 or 2 components
      double val = 0;
      double f = 1.;
      int sign = 0;
      int components = 0;
      int upmost_component = 2;
      int unit  = 0;			// 2 for :, 4 for deg, 6 for hour
      int error = 0;
      int pec   = 0;
      int posini = pos;
      double x;
      int hms, local_flags, i;

	if (flags>0) { flags = 0; error_message = null; }

	/* Default (undefined) is NaN */
	if (comp>0) gobbleSpaces();
	if (pos >= length) {
	    pos = posini;
	    return(0./0.) ;
	}
	if (DEBUG) System.out.print("#...Parsing.parse_sexa(" + comp + "): " + this );
	if ((sign = parseSign())>0) pec = SIGN_EDIT;
	if (comp>0)
	    while ((pos<length) && (a[pos] == ' ')) pos++;	// 26-Oct-2006

	/* Get Hours or Degrees */
	hms = comp>0 ? parseNum() : parseNum(2);
	if (DEBUG) System.out.print("; hms=" + hms + ", flags=" + flags);
	if (flags == 0) {	// No number at all ==> NaN
	    pos = posini;
	    if (!this.parseNaN()) pos = posini ;
	    return(0./0.);	
	}
	flags |= (pec<<16);
	val = hms;		// hours
	local_flags = flags;

	/* Look for possible fraction -- means a single component allowed */
	if ((pos < length) && (a[pos] == '.')) {
	    pos++;	// Gobble the decimal point
	    upmost_component = 0;
	    if ((pos < length) && Character.isDigit(a[pos])) {
		--pos;  // Restore the decimal point
		val += parseDecimal();
		local_flags += flags;
	    }
	    else local_flags |= 1;	// Indicates "just a decimal point".
	    if (DEBUG) System.out.print("=>" + local_flags);
        }

	/* Look for possible 'd' (deg) or 'h' specification */
	if ((comp>0) && (pos<length)) {
	    for (i=0; i<sexa_symb1.length; i++) {
		if (a[pos] == sexa_symb1[i]) { unit = sexa_unit1[i]; break; }
	    }
	    // Here unit takes the values:
	    // 1 : 1/60 (')
	    // 2 : colon(:)
	    // 4 : degrees; 5='; 13="
	    // 6 : hours  ; 7=m; 15=s
	    // 9 : 1/3600 (")
	    // and all combinations.
	    if (unit != 0) {
	        if ((unit&9) != 0) {
		    val /= 60.; f /= 60.; components++;
		    if ((unit&8) != 0) { val /= 60.; f /= 60.; components++; }
		    unit &= ~9;
	        }
		pos++;
	    }
	}

	/* V1.4: Check number of components */
	if ((comp > 1) && (components == 0) && (upmost_component>0)) {
	    // Look for 2 angles 
	    i = pos;
	    while (i<length) {
		if (a[i] == ':') { i++; continue; }
		if (a[i] == ' ') { i++; continue; }
		if (Character.isDigit(a[i]) || (a[i] == '.')) {
		    components++;
		    while ((i<length) && Character.isDigit(a[i])) i++;
		    if ((i<length) && (a[i] == '.')) break;
		}
		else if (isSign(a[i]) || (a[i] == ','))
			break;
		else {				// May be a unit symbol
		    int j = pos;	// save
		    pos = i;
		    boolean isaunit = lookup(sexa_symb1)>=0 ;
		    pos = j;		// restore
		    if (isaunit) i++;
		    else break;
		}
		if (components >= 5) break;
	    }
	    // The number of components is between 0 and 5.
	    upmost_component = i<length ? 
		components :	// Stopped due to e.g. sign
		components/2;
	    if (upmost_component >= 3)	// V1.6 addition
		upmost_component = 2;
	    components = 0;	// Reset number
	}

	while (pos < length) {
	    if (comp>0) {
	        if (a[pos] == sexa_letter[components]) { pos++; continue; }
	        if (a[pos] == sexa_symbol[components]) { pos++;
		                                       unit|=16;continue; }
	        if (a[pos] == ':') { pos++; continue; }
	        if (a[pos] == ' ') { pos++; continue; }
	    }
	    if (components >= upmost_component) 
		break; 
	    if (!Character.isDigit(a[pos])) break;
	    flags = 0;			// Changed by parseNum
	    /* error_message = null;*/
	    if (comp==0) {  // IAU-style: must be 2 digits
		hms = parseNum(2);
		i = flags&0xff00;	// Number of significant digits (<< 8)
		if (i == 0x100) { --pos; break; } // 1-digit = decimal
	    }
	    else {          // : or blank-separated
	        hms = parseNum();
		i = flags&0xff00;	// Number of significant digits (<< 8)
	        if (i==0x100) i=0x200;	// :2: is significant as 2 digits 02
	    }
	    local_flags += i;		// Total number of significant digits
	    if (hms>60) { 
		error=2; 
		error_message = "component " + hms + ">60";
	    }
	    components++;
	    f /= 60.; val += f*hms;
	}
	if ((components > 0) && (hms == 60) && (error == 0)) {
	    error = 1;
	    error_message = "";
	}
	boolean has_decimals = false;
	if (pos<length) {
	    if (a[pos] == '.') { pos++; has_decimals = true; }
	    else if ((comp == 0) && Character.isDigit(a[pos])) {
		has_decimals = true;
		local_flags |= (TRUNCATE<<16);
	    }
	}
	if ((flags&0xff) != 0);   // V1.82 fix: decimals already found
	else if ((pos < length) && has_decimals && Character.isDigit(a[pos])) {	
	    // Get Fraction
	    i = --pos;	// Restore the decimal point
	    char dot = a[i]; a[i] = '.';
	    double fraction = parseDecimal();
	    a[i] = dot;
	    val += f*fraction;
	    if ((error == 1) && (fraction != 0.)) {
		error = 2;
		error_message = "component > 60" + fraction;
	    }
	    local_flags += flags;	// Increase both decimals+significant
	}
	else local_flags |= 1;		// Indicates "just a decimal point".
	// V1.6: May be terminated by a unit symbol
	if ((unit>0) && (pos<length)) {
	    if (a[pos] == sexa_letter[components]) pos++; else
	    if (a[pos] == sexa_symbol[components]) pos++; 
	}
	// Accept e.g. 1.5e-3 as a valid number.
	if ((components == 0) && (pos < length) && (comp>0)) {
	    if (Character.toLowerCase(a[pos]) == 'e') {
		++pos;
		i = this.parseInt();
		if (this.digits() == 0) --pos;	// Not a valid exponent
		else {
		    val *= AstroMath.dexp(i);
		    i = (local_flags&0xff) - i;	// Number of decimals
		    if (i<0)   i=0;
		    if (i>=32) i=31;
		    local_flags = (local_flags & ~0xff) | i 
			        | (Astroformat.EFORMAT<<16);
		}
	    }
	}
	    
	flags = local_flags;
	if (DEBUG) System.out.print("=>" + flags);
	if (sign<0) val = -val;
	if (error>0) {			// Create the error message
	    String tag = error > 1 ? "#***" : "#+++";
	    error_message = tag + "parseSexa("
		+ String.valueOf(a, posini, pos-posini) + ")" + error_message;
	}
	//System.out.println("....components=" + components + ", unit=" + unit);
	if (components>0) {		// components is 1 (h:m) or 2 (h:m:s)
	    pec = components+1+unit;
	    if ((pec&16)!=0) pec = (pec&0xf)+8;	// SEXA2d --> SEXA2o
	    flags |= (pec<<16) | (error<<24);
	    // Number of decimals: no dot is equivalent to a dot in components
	    if ((flags&0xff) == 0) flags |= 1;
	    flags += (components*2);		// 4 decimals when h:m:s
	}
	else if (unit>0) {
	    if ((unit&16)!=0) pec = SEXA1o;
	    else if (unit==6) pec = SEXA1h;
	    else pec = SEXA1d;
	    flags |= (pec<<16);
	}
	if (DEBUG) System.out.println("=>" + flags);
	return(val);
    }

    /** 
     * Interpret a sexagesimal number.
     * no change occurs in the <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * An error is stored when a component is outside [0,60[.
     * The exponent is accepted.
     * @return	the double which could be interpreted -- NaN by default.
     */
    public final double parseSexa() {	// Interpret in Sexagesimal
	return(parse_sexa(1));
    }

    /** 
     * Interpret the first of 2 sexagesimal numbers.
     * no change occurs in the <em>pos</em> when no number could be matched,
     * which can be tested to verify that a number was actually matched.
     * An error is stored when a component is outside [0,60[.
     * The exponent is accepted.
     * @return	the double which could be interpreted -- NaN by default.
     */
    public final double parseSexa2() {	// Interpret in Sexagesimal
	return(parse_sexa(2));
    }

    /** 
     * Interpret a Sexagesimal written in IAU-style (no space).
     * An error is stored when a component is outside [0,60[.
     * The exponent is accepted.
     * @return	the double which could be interpreted -- NaN by default.
     */
    public final double parseIAU() {	// Interpret in Sexagesimal
	return(parse_sexa(0));
    }

    /** 
     * Interpret a Date.
     * No change occurs in the <em>pos</em> when date (Y-M-D) could not be
     * matched. 
     * On return, indications about format.
     * @return	a date in MJD (Modified Julian Date = JD-2400000.5,
     * 		or number of days elapsed since 17 Nov 1858 UTC).
     */
    public final int parseDate() {	// Interpret in Sexagesimal
      int[] datelem = new int[3];	// Date elements
      int[] datype  = new int[3];	// Date type 1=day 2=mon 4=yr
      int   status = 0;			// Found elements: 1=day 2=mon 4=yr
      int year, month, day;
      int pos1, i, nc, jd;
      int posini = pos;
      int sign = 0;
      int pec = Astroformat.DATE;	// Peculiarity: DATE_alpha
      char sep = Character.MIN_VALUE;
      int  nc_max = 3;
      char c; 

	if (flags>0) { flags = 0; /* error_message = null;*/ }

	/* Default (undefined) is NaN */

	gobbleSpaces();
	if (pos >= length) {
	    pos = posini;
	    return(0) ;
	}
	/* A negative number must be a year (BC) */
	if ((sign = parseSign())>0) pec = SIGN_EDIT;

	/* Get Day or Year */
	// System.out.print("...parseDate: status=0");
	for (i=0; i<nc_max; i++) {
	    if (i>0) {	/* Gobble the separator - or . or / */
	        c = currentChar();
		if ((sep == Character.MIN_VALUE) && (i == 1) &&
	            ((c == '-') || (c == '.') || (c == '/'))) sep = c;
		// sep contains the first valid separator.
		// System.out.print("...sep="+sep+"["+this+"]");
		if (c == sep) pos++;			// Skip the separator
		else if (sep != Character.MIN_VALUE) 	// no separator ?
		    break;
	    }
	    gobbleSpaces();
	    if (pos >= length) break;
	    pos1 = pos;
	    datelem[i] = parseNum();
	    if (pos1 == pos) {		// Not a number, try month
		if ((status&2) != 0) break;
	        datelem[i] = lookupIgnoreCase(month_list);
		if (datelem[i]<0) 	// Not a month either give up
		    break;
		pec |= DATE_alpha;	// Month is alphabetical
		datelem[i] += 1;	// Range of month in [1..12]
		datype[i] = 2;		// It's a month
		while(Character.isLetter(currentChar()))
		    pos++;
	    }
	    else {			// A number -- 4 digits is a year
		// System.out.print("(len="+(pos-pos1)+")");
		if ((sign<0) || ((pos-pos1) >= 4)) {
		    // System.out.print("(in1)");
		    if ((status&4)!= 0) break;	// Already a year!
		    datype[i] = 4;
		}
		else if ((pos-pos1) >= 3) {	// Could be yday or year ?
		    // System.out.print("(in2)");
		    datype[i] = (status&3)==0 ? 1 : 4 ;
		    if (datype[i] == 1)		// yday ==> only 2 components
			nc_max = 2;	
		}
		if (sign<0) {
		    datelem[i] = -datelem[i] ;
		    sign = 0;
		}
	    }
	    status |= datype[i];
	    // System.out.print(" " + status + "[+" + pos + "]");
	}
	nc = i;				// Number of components
	// System.out.println(" (end)");
        if((nc==2)&&(sep=='.')) 	// V2.0: not a date
            nc=0;

	/* A date requires at least 2 components */
	if (nc < 2) {
	    pos = posini;
	    return(0);
	}

	/* Ambiguities: when not all items are recognised,
	 * assume ISO-8601 order year.Mon.Day 
	 */
	if (status == 0) {
	    datype[0] = 4;
	    if (nc == 2) {	// Only 2 components, assume Year.Day */
		datype[1] = 1;
		status = 4|1;
	    }
	    else { 
		datype[1] = 2; datype[2] = 1; 
		status = 7;
	    }
	}
	for (i=0; i<nc; i++) {
	    if (datype[i] != 0) continue;
	    if ((status&4)==0)      datype[i] = 4;
	    else if ((status&2)==0) datype[i] = 2;
	    else datype[i] = 1;
	    status |= datype[i];
	}

	/* Keep the info about what was matched  (for this.format()) */
	if (status == 5) 		// Y+D 
	    pec |= datype[0] == 4 ? DATE_YD : DATE_DY;
	else if (status == 6)		// Y+M 
	    pec |= datype[0] == 4 ? DATE_YM : DATE_MY;
	else if (datype[0] == 4) 	// 3 components Y+...
	    pec |= datype[1] == 2 ? DATE_YMD : DATE_DY+1;
	else if (datype[0] == 1)	// 3 compoments D+...
	    pec |= datype[1] == 2 ? DATE_DMY : DATE_DY-1;
	else                    	// 3 compoments M+...
	    pec |= datype[1] == 4 ? DATE_MYD : DATE_MDY;

	/* Order the components datem as year month[0-11] day[1-31] */
	year  = month = day = 0;	// Required by the compiler!!!
	for (i=0; i<nc; i++) {
	    if (datype[i] == 4) year  = datelem[i];
	    if (datype[i] == 2) month = datelem[i]-1;	// in range [0..11]
	    if (datype[i] == 1) day   = datelem[i];
	}

	/* Verify it's possible -- month and day in a correct range ? */
	if ((month*30+day) > 367) {
	    pos = posini;
	    return(0);
	}

	// V1.3: Conversion Year Month Date to Julian Date is in Astrotime!
	jd = (int)(Astrotime.YMD2JD(year, month, day) - 2400000.5);

	flags |= (pec<<16);		// Keep what was matched.
	return(jd);
    }


    /** 
     * Interpret some Complex number (date, sexagesimal).
     * On return, a date / angle / time
     * @param	pic "picture" which specifies the format of the input.
     * 		Letters are Y y (years) M (month) D (day) h (hour) m (minutes)
     * 		s (seconds) d (degrees) f (fractions), and punctuations like
     * 		: (colon) / (slash), etc. <b>Y</b> and <b>y</b> have slightly
     * 		different meanings when the century is omitted (2-digit years):
     * 		<b>YY</b> have their offset in 1900, while <b>yy</b> assumes
     * 		dates between 1950 and 2049: e.g. <tt>01</tt> means
     * 		1901 with <b>Y</b> and 2001 with <b>y</b>
     *		<P>
     * 		The text must follow exactly the "picture" when the components
     * 		are specified with several letters. For instance, a picture
     * 		<tt>YYYY-MM-DD</tt> indicates that a conforming text
     * 		must be made of 4 digits representing the year, followed by a
     * 		dash, 2 digits representing the month, a dash, and 2 digits
     * 		representing the day -- while <tt>Y-M-D</tt> accept a variable
     * 		number of digits in each of the components: "<tt>2006-7-5</tt>"
     * 		would conform to the latter "picture", but not to the 
     * 		"<tt>YYYY-MM-DD</tt>" one.
     * @return	a date, time or angle.
     *   Date is expressed in MJD (days elapsed since 17 Nov 1858 UTC), 
     *   time in days or hours, angles in degrees.  The functions 
     *   {@link Astroformat#isDate}, {@link Astroformat#isTime},
     *   {@link Astroformat#isDate}, {@link Astroformat#isDays}
     *   specify the actual interpretation done in the parsing.
     * @throws  ParseException for invalid picture or non-conforming data.
     * 		The error_message of the exception starts by:<br>
     * 		"#+++" when upper/lower case were mixed; <br>
     * 		"#***" for terminal error, and contains ((pic)) for invalid arg
     */
    public final double parseComplex(String pic) throws ParseException {
      int[]  comp6 = new int[6];	// Date/time elements
      int[]  ipic6 = new int[6];	// Position of components in pic
      char[] pic6  = new char[6];	// Element number in pic1
      boolean check_century = false;
      char[] apic = pic.toCharArray();
      int ipic = 0;			// Index in pic
      int epic = pic.length();
      int last_comp = 0;		// Which is the last component
      double factor, fraction_factor;
      double value = 0./0.;
      int posini = pos;
      int k = 0;			// 0=yr 1=mon 2=day 3=hr 4=min 5=s
      int i, plen=0;			// #characters in field
      int sign = 0;
      int pec = 0;
      char c = Character.MIN_VALUE; 
      int mixed = 0;			// Indicates an interpretation made
      StringBuffer mixed_buf = null;	// Error message of interpretation

	if (DEBUG) System.out.println("....cplx("+pic+")\tin=\"" 
		+ this.toString()+"\"");
	if (flags>0) { flags = 0; error_message = null; }

	/* Initialisation */
	comp6[0] = comp6[1] = comp6[2] = comp6[3] = comp6[4] = comp6[5] =0;
	ipic6[0] = ipic6[1] = ipic6[2] = ipic6[3] = ipic6[4] = ipic6[5] =0;
	pic6[0] = pic6[1] = pic6[2] = pic6[3] = pic6[4] = pic6[5] 
	        = Character.MIN_VALUE;

	if (apic[ipic] == '"') ipic++;
	// Possible sign
	if (pos<length) {
	    if ((sign = parseSign())>0) pec = SIGN_EDIT;
	}

	while ((pos<length) && (ipic<epic)) {
	    c = apic[ipic++]; 
	    k = pic1.indexOf(c);
	    if (k<0) {			// Not a specification letter
		if (c == a[pos]) { pos++; continue; }
		if (c == ':') { continue; }		// Next field
		if (c == '.') break;			// Decimals
		if (c == 'f') break;			// Fraction
		if (c == '"') break;	// Ending '"' in pic.
		error_message = "[Parsing.parseComplex] ((" 
		    + pic + ")): what is '" + c + "'?";
		throw new ParseException(error_message, ipic);
	    }
	    k %= 6;			// Year Month Day Hour Min Sec
	    if (Character.isLetter(a[pos])) {	// Must be month name
		pec |= DATE_alpha;
		i = lookupIgnoreCase(month_list);
		if (k == 4) { 		// error, 'm' instead of 'M'
		    k = 1; 
		    mixed |= (1<<k);	// mixed upper/lowercase in this element
		}
		if ((k!= 1) || (pic6[1] != Character.MIN_VALUE) || (i<0)) {
		   error_message = "[Parsing.parseComplex] (("
		       + pic + ")): month? "+ this.toString();
		   throw new ParseException(error_message, ipic);
		}
		comp6[1] = i;		// Range 0..11
		pic6[1] = 'a';
	    }
	    else if (pic6[k] != Character.MIN_VALUE) {	// Field already used ?
		i = 5-k;		// Month <-> Minute, Day <-> Deg
		if ((k>0) && (i>0) && (pic6[i] == Character.MIN_VALUE)) {
		    comp6[i] = comp6[k];
		    pic6[i]  = pic6[k];
		    ipic6[i] = ipic6[k];
		    k = i;
		    mixed |= (1<<k);	// mixed upper/lowercase in this element
		}
	        if (pic6[k] != Character.MIN_VALUE) {  // Duplication ??
		    error_message = "[Parsing.parseComplex] ((" + pic + ")): "
			+ "duplicated component '" + pic1.charAt(k) + "'";
		    throw new ParseException(error_message, ipic);
		}
	    }
	    ipic6[k] = ipic;		// Index in pic of this element.
	    last_comp = k;

	    /* Count the number of identical elements in picture */
	    for(plen=1; (ipic<epic) && (apic[ipic] == c); ipic++) plen++;
	    if (pic6[k] != Character.MIN_VALUE)		// Was already filled
		continue;
	    if (plen == 1) {		// Number of digits not specified
		gobbleSpaces();
		if (a[pos] == ':') pos++;
		gobbleSpaces();
		comp6[k] = parseNum();
	    	pic6[k] = c;
		if ((k == 0) && (c == 'y')) check_century = true;
		continue;
	    }
	    // Number of digits given a priori
	    if ((k == 0) && (plen == 2)) check_century = true;
	    for (i=plen; (i>0) && (pos<length); --i, pos++) {
		if (a[pos] == ' ') continue;
		else if (Character.isDigit(a[pos])) 
		    comp6[k] = comp6[k]*10 + Character.digit(a[pos], 10);
		else {
		    error_message = "[Parsing.parseComplex] ((" + pic + ")): "
			+ this.toString();
		    throw new ParseException(error_message, ipic);
		}
	    }
	    pic6[k] = c;
	}
	if (DEBUG) {
	    System.out.print("....cplx[a]=");
	    for(i=0;i<6;i++) System.out.print("("+pic6[i]+")"+comp6[i]);
	    System.out.println(",last_comp=" + last_comp 
		    + ", check_century=" + check_century);
	}

	/* Verify possible mixing month[1]/min[4] */
	if ((Character.toUpperCase(pic6[1]) == 'M') 
	      && (pic6[4] == Character.MIN_VALUE)
	      && (pic6[0] == Character.MIN_VALUE)) {	// No date !!
	    pic6[4]  = pic6[1];   pic6[1] = Character.MIN_VALUE;
	    comp6[4] = comp6[1]; comp6[1] = 0;
	    mixed |= (1<<1);	// mixed upper/lowercase in this element
	}
	else if ((pic6[1] == Character.MIN_VALUE)
	      && (pic6[4] != Character.MIN_VALUE)
	      && (pic6[0] != Character.MIN_VALUE)
	      && (pic6[5] == Character.MIN_VALUE)) {	// no second, no month
	    pic6[1] = pic6[4];    pic6[4] = Character.MIN_VALUE;
	    comp6[1] = comp6[4]; comp6[4] = 0;
	    mixed |= (1<<4);	// mixed upper/lowercase in this element
	}

	/* Verify possible mixing day[2]/deg[3] */
	if (     (pic6[3] == Character.MIN_VALUE)
	      && (pic6[2] != Character.MIN_VALUE)
	      && (pic6[0] == Character.MIN_VALUE)
	      && (pic6[1] == Character.MIN_VALUE)) {	// No date at all...
	    pic6[3] = pic6[2];    pic6[2] = Character.MIN_VALUE;
	    comp6[3] = comp6[2]; comp6[2] = 0;
	    mixed |= (1<<2);
	}
	else if ((pic6[2] == Character.MIN_VALUE)
	      && (pic6[0] != Character.MIN_VALUE)) {	// Incomplete date
	    pic6[2] = pic6[3];    pic6[3] = Character.MIN_VALUE;
	    comp6[2] = comp6[3]; comp6[3] = 0;
	    mixed |= (1<<3);
	}

	/* Prepare message in case of mixed picture */
	if (mixed!=0) {
	  String mod_pic = new String(pic) ;
	  char[] mpic = mod_pic.toCharArray();
	  char M, m;
	    for (k=0; mixed!=0; mixed>>=1, k++) {
		if ((mixed&1)==0) continue;
		i = ipic6[k]-1;			// Position in pic
		m = mpic[i];			// The character to change
		M = Character.toUpperCase(m);
		if (M == m) M = Character.toLowerCase(m);
		while ((i<mpic.length) && (mpic[i] == m)) 
		    mpic[i++] = M;
	    }
	    mixed_buf = new StringBuffer(100);
	    mixed_buf.append("#+++parseComplex(");
	    mixed_buf.append(pic);
	    mixed_buf.append(") interpreted as (");
	    mixed_buf.append(mpic);
	    mixed_buf.append(")");
	}

	/* Month must be in range 0.11 is specified numerically */
	if ((pic6[1] != Character.MIN_VALUE) && (pic6[1] != 'a')
		&& (comp6[1]>0)) comp6[1] -= 1;

	/* Keep the info about what was matched  (for this.format()),
	 * and compute the date part if any
	 */
	value = 0; factor = fraction_factor = 1;
	if (DEBUG) {
	    System.out.print("....cplx[b]=");
	    for(i=0;i<6;i++) System.out.print("("+pic6[i]+")"+comp6[i]);
	    System.out.println("");
	}
	if (pic6[0] != Character.MIN_VALUE) {		// Full date
	    pec |= DATE;
	    if (pic6[2] != Character.MIN_VALUE) {
		pec |= DATE_YD;
		if (pic6[1] != Character.MIN_VALUE)  pec |= DATE_YMD;
	    }
	    if (check_century && (comp6[0] < 200)) {
		comp6[0] += 1900;
		if ((pic6[0] != 'Y') && (comp6[0] < 1950)) 
		    comp6[0] += 100;
	        check_century = false;
	    }
	    if (sign<0) { comp6[0] = -comp6[0]; sign = 0; }
	    if (DEBUG) {
	        System.out.print("....cplx[c]=");
	        for(i=0;i<6;i++) System.out.print("("+pic6[i]+")"+comp6[i]);
	        System.out.println("");
	    }
	    value = Astrotime.YMD2JD(comp6[0], comp6[1], comp6[2]) - 2400000.5;
	    factor = 24.; 
	}
	else if (pic6[2] != Character.MIN_VALUE) {	// Elapsed days
	    if (pic6[1] != Character.MIN_VALUE) {	// month/day, no year?
		error_message = "[Parsing.parseComplex] ((" + pic + ")): "
		    + "month without year ? " + this.toString();
		throw new ParseException(error_message, ipic);
	    }
	    pec |= DATE_DIFF;
	    value = comp6[2];
	    factor = 24.;
	}
	else if (Character.toLowerCase(pic6[3]) == 'h')
	    pec |= SEXA3h;
	for (i=3; i<6; i++) {
	    if (pic6[i] != Character.MIN_VALUE) {
	        if (DEBUG) System.out.print("    value=" + value + " => ");
		value += comp6[i]/factor;
	        if (DEBUG) System.out.println(value);
	        if (i == last_comp) fraction_factor = factor;
	    }
	    factor *= 60.;
	}

	/* Look for decimal fraction -- 
	 * c is the last char in picture 
	 * plen is the length of last picture figure (e.g. 2 for ss)
	 */
	if (pos >= length) {	// Number exhausted -- skip remaining elements
	    ipic = epic;
	    c = Character.MIN_VALUE;
	}
	else if ((plen == 1) && (a[pos] == '.')) {
	    // Always accept fractional part of last field
	    value += parseValue(0)/fraction_factor;
	}
	else while ((c == 'f') && (pos<length) && Character.isDigit(a[pos])) {
	    fraction_factor *= 10;
	    value += Character.digit(a[pos], 10)/fraction_factor;
	    c = ipic<epic ? apic[ipic++] : Character.MIN_VALUE;
	    pos++;
	}
	if ((c != '"') && (c != Character.MIN_VALUE)) {
	    error_message = "[Parsing.parseComplex] ((" + pic + ")): "
		+ "mismatch from \"" + pic.substring(ipic-1) + "\": "
		+ this.toString();
	    throw new ParseException(error_message, ipic);
	}

	// A problem in the 'picture' is reflected by a WARNING status
	// and an error message.
	if (mixed_buf != null) {
	    error_message = mixed_buf.toString();
	    flags |= (1<<24);		// indicates WARNING
	    // System.err.println(b.toString());
	}
	flags |= (pec<<16);		// Keep what was matched.
	if (sign<0) value = -value;
	if (DEBUG) System.out.println("#...parseComplex("+pic+"): value=" 
		+ value + ", pec="+pec+", flags="+flags);
	return(value);
    }


    /*==============================================================
                Retrieve details about the last parsing
     *==============================================================*/

    /** 
     * Retrieves the number of decimals of the last parsed number.
     * For a sexagesimal representation, the minutes and seconds
     * count each for 2 decimals, i.e. 00:01:02 has 4 decimals.
     * @return number of decimals, -1 if no decimal point exists.
     */
    public final int decimals() {
	return((flags&0xff)-1);
    }

    /** 
     * Retrieves the number of significant digits of the last parsed number.
     * @return number of significant digits -- 0 meaning an emtpy (NaN) number.
     */
    public final int digits() {
	return((flags>>8)&0xff);
    }

    /** 
     * Retrieves the 'format' of the last parsed number.
     * @return One of DECIMAL, EFORMAT, FACTOR, SEXA2, SEXA3, SEXA2c, SEXA3c,
     * 	       SEXA2d, SAX2h, SEXA3d, SEXA3h, DATE
     * 	       possibly combined with ZERO_FILL, SIGN_EDIT, TRUNCATE
     * 	       (TRUNCATE indicates no decimal point in IAU-style)
     * @see Astroformat
     */
    public final int format() {
	return((flags>>16)&0xff);
    }

    /** 
     * Retrieves the 'format' of the last parsed number as a string.
     * @return A printable variant of format.
     */
    public final String form() {
      // int k = (flags>>16)&0xff;
      // System.out.println("....form: flags=0x"+Integer.toHexString(flags)
      //                   +", k="+k);
      //  return (explain_form[k&0xf] + explain_pec[k>>4]);
	return(Astroformat.explain((flags>>16)&0xff));
    }

    /** 
     * About the last parsed number: was it sexagesimal ?
     * @return true if was expressed in sexagesimal
     */
    public final boolean isSexa() {
	return(isSexa(flags>>16));
    }

    /** 
     * About the last parsed number: was it expressed as a date ?
     * @return true if parsed number contained 'h' and/or 'm' 's'
     */
    public final boolean isDate() {
	return(isDate(flags>>16));
    }

    /** 
     * About the last parsed number: does it represent days (date_diff) ?
     * @return true if parsed number expressed time in days.
     */
    public final boolean isDays() {
	return(isDays(flags>>16));
    }

    /** 
     * About the last parsed number: was it expressed as time (hms) ?
     * @return true if parsed number contained 'h' and/or 'm' 's'
     */
    public final boolean isTime() {
	return(isTime(flags>>16));
    }

    /** 
     * About the last parsed number: was it expressed as angle (&deg; or d)
     * @return true if parsed number contained '&deg;' and/or ' "
     */
    public final boolean isAngle() {
	return(isAngle(flags>>16));
    }

    /** 
     * About the last parsed number: was it in error ?
     * @return true if parsed number contained '&deg;' and/or ' "
     */
    public final boolean inError() {
	return((flags&0x2000000)!=0);
    }

    /** 
     * Retrieves the status (OK, WARNING, ERROR) of last (sexagesimal) parsing.
     * @return One of OK, WARNING, ERROR
     */
    public final int  status() {
        return(flags>>24);
    }

    /** 
     * About the last parsed number: get the error message
     * @return The error message, null when no error.
     */
    public final String getMessage() {
	if ((flags&0x3000000) == 0) return(null);
	return(error_message);
    }

    /*==============================================================
                	Parse an Array
     *==============================================================*/

   /**
    * Interpret an array (several double numbers).
    * Accepts an array within brackets (or parentheses/braces).
    * Accept a separator between numbers if exists.
    * @param vec  Array containing on output the values parsed.
    * @param sep  Character accepted as separator
    * @return number of numbers found. 
   **/
    public int parseArray(double[] vec, char sep) {
        int i;
        // Initialize all number to NaN -- no!
	// for (i=0; i<vec.length; i++) vec[i] = 0./0.;
	// Move in the text as long as possible.
	for (i=0; (i<vec.length) && (this.pos<this.length); i++) {
	    int postart = this.pos;
	    this.gobbleSpaces(); 
	    if (this.currentChar() == sep) {
		if (i == 0) continue;
		this.advance(1);
	        this.gobbleSpaces(); 
	    }
	    int ipos = this.pos;
	    double x = this.parseDouble();
	    if (this.pos == ipos) {	// Nothing found...
		this.pos = postart;
		break;
	    }
	    vec[i] = x;
	}
	return(i);
    }

   /**
    * Interpret an array (several numbers).
    * Accept a comma between numbers if within a bracketed list.
    * @param vec  Array containing on output the values parsed.
    * @return number of numbers found, between 0 and vec.length;
    *         returns -1 for a bracketed expression with missing closing bracket.
    *         In this last case, the current position is not changed.
   **/
    public int parseArray(double[] vec) {
        int posini = this.pos; 
        this.gobbleSpaces();
        // Could it be bracketed set ?
        int ibracket = this.lookup(brackets);
        if((ibracket&1)==0) {
            ibracket++;	// matching closing bracket.
            int n = parseArray(vec, ',');
            this.gobbleSpaces();
            if(this.currentChar()==brackets[ibracket]) advance(1);
            else { 
                n = -1; this.set(posini); 
                error_message = "[Parsing.parseArray] mismatched brackets " + brackets[ibracket-1] + brackets[ibracket];
            }
            return(n);
        }
	return(parseArray(vec, ' '));
    }

   /**
    * Interpret a number + Error.
    * Text to interpret. May be "300+23-25" or "300[23]"
    * 			or "300+/-23" or "300+/-23"
    * @param vec  Array containing value PositiveError NegativeError
    * @return number of numbers found (0, 1, 2 or 3). 
    * 		When 0, the position was not changed.
    * 		1 means a value without error, 2 value+error, 
    * 		3 value + positive error + negative error.
   **/
    public int parseWithError(double[] vec) {
      int postart, ipos, poserr, i;
      boolean bracket = false;
      int type = 0;	// 0=+/- 1=+ 2=- 3=+/- 4=[]
      double x;
        // Initialize all number to NaN.
	for (i=0; i<vec.length; i++) vec[i] = 0./0.;
	// Move in the text as long as possible.
	postart = this.pos;
	this.gobbleSpaces(); ipos = this.pos;
	x = this.parseDouble();
	if (this.pos == ipos) {	// Nothing found...
	    this.pos = postart;
	    return(0);
	}
	vec[0] = x;
	// Find some error symbol.
	poserr = ipos;
	for (i=1; (i<vec.length) && (ipos<length); i++) {
	    postart = this.pos;
            type = lookup(error_symb); 
	    if (type<0) break;
	    bracket |= type == 4;
	    ipos = this.pos;
	    x = this.parseDouble();
	    if (this.pos == ipos) {	// Error not found
	        this.pos = postart;
	        break;
	    }
	    if ((type==1) || (type==2)) vec[type] = x;
	    else vec[i] = x;
	}
	if (bracket) {
	    this.gobbleSpaces();
	    if (match("]")) ;
	    else { 
		ipos=poserr; 
		for (i=1; i<vec.length; i++) vec[i] = 0./0.;
		i=1;
	    }
	}
	return(i);
    }

    /*==============================================================
                Edition
     *==============================================================*/

    /**
     * View the Parsing as a String
     * @return the not-yet-parsed part of the string.
     */
    public final String toString() {
	return(new String(a, pos, length-pos)) ;
    }

    /**
     * View a part of the Parsing as a String. Added in V1.3
     * @param len  Length to keep (truncated if necessary)
     * @return the not-yet-parsed part of the string.
     */
    public final String toString(int len) {
	if ((len+pos) > length) len = length - pos;
	return(new String(a, pos, len)) ;
    }

    /**
     * View a part of the Parsing as a String. Added in V2.0
     * @param start  beginning offset (from 0)
     * @param end  ending(+1) offset
     * @return a String made of chars [start ... end[
     */
    public final String toString(int start, int end) {
        if(end>length) end=length;
	return(new String(a, start, end-start)) ;
    }

}
