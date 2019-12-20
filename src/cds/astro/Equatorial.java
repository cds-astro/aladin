// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

/**
 * This abstract class introduces the specificities of the equatorial
 * coordinates.
 * <BR>An equatorial coordinate system is a system linked to the Earth,
 * and therefore varies with the long-term Earth motion (precession).
 * A full definition of this frame therefore requires the knowledge
 * of an Equinox which specifies when exactly this frame coincides with
 * the Earth frame.
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */
public abstract class Equatorial extends Astroframe {
    /** Any equatorial frame must have its equinox defined.
     *  The exact definition is Besselian or Julian, depending on type
     *  of frame (FK5-based is Julian, FK4-based is Besseilan)
    **/
    protected double equinox = 0;	// The actual equinox in J/B year
    /* The Equatorial frame also has a matrix to precess to standard equinox*/
    protected double[][] toBaseEquinox;

    /**
     * Constructor: editions are sexagesimal time in Lon/Lat 
    **/
    Equatorial() {
	hms = true;
	// Default edition in sexagesimal separated by : 
        ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
    }

    /**
     * Edition of Equatorial frame.
     * @return the name of the Astroframe.
    **/
    public String toString() {
	if ((base_epoch != epoch) || (equinox != base_epoch)) 
	    return (super.toString(this.epoch));
	return (super.toString());
    }
    
    /**
     * Get the equinox
     * @return the equinox value
     */
    public double getEquinox() {
   	 return equinox;
    }
}
