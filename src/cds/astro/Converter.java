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
 * @author  Brice GASSMANN, Francois Ochsenbein-- francois@astro.u-strasbg.fr
 * @version 0.9 15-Nov-2006: 
 *==========================================================================
 */

import java.util.*;
import java.text.*;	// for parseException

/**
 * This class, tightly connected to the {@link Unit} class,
 * gathers <em>non-standard</em> unit conversions.<P>
 * <em>Standard</em> conversions are made between units having the same physical
 * dimension, as e.g. a conversion between <b>Hz</b> and <b>km/s/Mpc</b>.
 * Conversion bewteen units having different physical dimensions are called
 * <em>non-standard</em>, e.g. between hours (time) and degrees (angle).
 * This default class contains linear transformations only; a derived class
 * can be used if necessary to generate more complex converters.
 * @author  Brice GASSMANN, Francois Ochsenbein-- francois@astro.u-strasbg.fr
 * @version 1.0 15-Nov-2006: Finalisation
 */
public class Converter {
    /**
     * The source unit
    **/
     public Unit source;
    /**
     * The target unit
    **/
     public Unit target;
    /**
     * The conversion factor, if used (target = factor * source)
    **/
     public double factor;
    /**
     * The conversion offset, if used (target = factor * source + offset)
    **/
     // public double offset;
    /**
     * separator beween the units (a Unicode arrow)
    **/
     static char SEP = '\u27fe';

    /*==================================================================
			Constructors
     *==================================================================*/

     /**
      * Creation of a <em>standard converter</em>.
      * The standard rules of unit conversion are applied 
      * (see {@link Unit#convert})
      * @param source_unit unit of source value
      * @param target_unit unit of target value
      **/
     public Converter(String source_unit, String target_unit) {
	try {  this.source = new Unit(source_unit); }
	catch (Exception e) { System.err.println(e); source = null; }
	try {  this.target = new Unit(target_unit); }
	catch (Exception e) { System.err.println(e); target = null; }
	this.source.setUnit(); this.target.setUnit();
	this.factor = source.factor/target.factor; // this.offset = 0;
	// No need to register, uses the standard Unit.convertUnit
    }

     /**
      * Creation (and registration) of a unit converter.
      * New objects of this class are known in {@link Unit#convert},
      * i.e. the {@link #convert} method defined here is applied.<p>
      * Examples could be
      * <tt> Converter("h", "deg", 15.)</tt> or 
      * <tt> Converter("\"d:m:s\"", "\"h:m:s\"", 1./15.)</tt>
      * @param source_unit unit of source value.
      * @param target_unit unit of target value
      * @param factor factor of conversion in: target = factor * source 
      **/
     public Converter(String source_unit, String target_unit, double factor) {
	// Prepare the units, and register them 
	try { this.source = new Unit(source_unit); this.source.setUnit(); }
	catch (Exception e) {
	     System.err.println(e);
	     source = null;
	}
	try { this.target = new Unit(target_unit); this.target.setUnit(); }
	catch (Exception e) {
	    System.err.println(e);
	    target =null;
	}
	this.factor = factor;
	Unit.registerConverter(source_unit, target_unit, this);
    }

     /**
      * Creation of a unit converter. An example can be:
      * <tt> Converter("h", "deg", 15.)</tt> or 
      * <tt> Converter("\"d:m:s\"", "\"h:m:s\"", 1./15.)</tt>
      * @param source_unit unit of source value.
      * @param target_unit unit of target value
      * @param factor factor of conversion in: target = factor * source + offset
      * @param offset offset of conversion in: target = factor * source + offset
      ** 
     public Converter(String source_unit, String target_unit, 
	     double factor, double offset) {
	// Prepare the units, and reigster them 
	try { this.source = new Unit(source_unit); this.source.setUnit(); }
	catch (Exception e) {
	     System.err.println(e);
	     source = null;
	}
	
	// Not necessary to define a new symbol...
    	if (!Unit.checkSymbol(target_unit)) {
	    try   { this.target = Unit.addSymbol(target_unit, target_unit); }
	    catch (Exception e) { 
		System.err.println(e);
		target =null;
	    }
	}
	try { this.target = new Unit(target_unit); this.target.setUnit(); }
	catch (Exception e) {
	    System.err.println(e);
	    target =null;
	}
	this.factor = factor;
	this.offset = offset;
	Unit.registerConverter(source_unit, target_unit, this);
    }
    **/

    /*==================================================================
			Dump
     *==================================================================*/
    /**
     * Dump the object
     * @param	title title line of the dump
     **/
    public void dump(String title) {
	System.out.println(title+"(factor="+factor+")");
	source.dump("source_unit ");
	target.dump("target_unit ");
    }

    /*==================================================================
			Conversion
     *==================================================================*/

    /**
     * Convert a number.
     * Convert the value from <em>source</em> unit into <em>target</em> unit
     * @param	value the value (expressed in source)
     * @return	the corresponding value, expressed in <em>target</em> units.
     **/
    public double convert(double value) throws ArithmeticException {
	source.setValue(value);
	if (factor == factor) 	// factor is not NaN, apply the factor
	    target.setValue(value*factor/*+offset*/);
	else 			// Apply the standard rules:
	    Unit.convert(source, target);
	return(target.value);
    }

    /**
     * Convert a value.
     * Convert the value from <em>source</em> unit into <em>target</em> unit
     * @param	value the value (expressed in source)
     * @return	the corresponding value, expressed in <em>target</em> units.
     **/
    public double convert(String value) 
	throws ParseException, ArithmeticException {
	source.setValue(value);
	if (factor == factor) 	// factor is not NaN, apply the factor
	    target.setValue(source.value*factor);
	else 			// Apply the standard rules:
	    Unit.convert(source, target);
	return(target.value);
    }

    /**
     * Convert a value, return its edited form.
     * Convert the value from <em>source</em> unit into <em>target</em> unit
     * @param	value the value (expressed in source)
     * @return	the corresponding edited value, expressed in 
     * 		<em>target</em> units.
     **/
    public String transform(String value) 
	throws ParseException, ArithmeticException {
	source.setValue(value);
	if (factor == factor) 	// factor is not NaN, apply the factor
	    target.setValue(source.value*factor/*+offset*/);
	else 			// Apply the standard rules:
	    Unit.convert(source, target);
	return(target.editedValue());
    }

    /**
     * Convert a value, return its edited form.
     * Convert the value from <em>source</em> unit into <em>target</em> unit
     * @param	value the value (expressed in source)
     * @return	the corresponding edited value, expressed in 
     * 		<em>target</em> units.
     **/
    public String transform(double value) throws ArithmeticException {
	source.setValue(value);
	if (factor == factor) 	// factor is not NaN, apply the factor
	    target.setValue(value*factor/*+offset*/);
	else 			// Apply the standard rules:
	    Unit.convert(source, target);
	return(target.editedValue());
    }

    /*==================================================================
			Edition
     *==================================================================*/

     /**
      * Standard edition of the unit converter
     **/
    public String toString() {
	return(source.symbol+"=>"+target.symbol+"(x"+factor+")");
    }
	
}
