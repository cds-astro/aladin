package cds.astro;

/**
 *==========================================================================
 * @author: Francois Ochsenbein -- francois@astro.u-strasbg.fr
 * @version: 0.7 07-sep-2002
 * @version: 0.9 15-sep-2002: 
 * @version: 1.0 01-sep-2006: <tt>orig</tt> added
 *==========================================================================
 */

import java.util.*;
import java.text.*;	// for parseException

/**
 * This class defines what is a unit; it is required for the Unit class.
 * It is used by Unit, and is not public.
 */

class Udef {
    public String symb;	// Symbol, in principle letters only
    public String expl;	// Text used in 'explain' method
    public   long mksa;	// The interpretation in terms of units
    public double fact;	// How to convert to SI
    public double orig;	// Origin from SI unit.

    /* CONSTRUCTOR */
    public Udef(String s, String text, long m, double f) {
	symb = s;
	expl = text ;
	mksa = m;
	fact = f;
	orig = 0.;
    }
    public Udef(String s, String text, long m, double f, double o) {
	symb = s;
	expl = text ;
	mksa = m;
	fact = f;
	orig = o;
    }

    /* EDITOR */
    public final String toString() {
	return(symb + ": " + expl);
    }
}
