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

/*==================================================================
                FK5  (Astroframe -> Equatorial -> FK5)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The FK5 is an equatorial coordinate system (coordinate system linked to 
 * the Earth) based on its J2000 position.
 * As any equatorial frame, the FK5-based follows the long-term
 * Earth motion (precession).
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */

public class FK5 extends Equatorial {

  /**
   * Initialize the parameters of this equatorial frame.
   * @param equinox the equinox of definition (in Julian year)
   * @param epoch   the default epoch (in Julian year)
  **/
   private void initialize(double equinox, double epoch) {
   	this.precision = 7;	// Intrinsic precision = 0.01arcsec
	this.equinox = equinox;
	this.epoch = epoch;
	this.name = "FK5(J" + equinox + ")";
        // ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        // ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
	if (Math.abs(equinox-2000.0)>0.0003) 
	     toBaseEquinox = this.precessionMatrix(equinox, 2000.);
	else toBaseEquinox = Coo.Umatrix3;
	ICRSmatrix = toBaseEquinox;
   }

   // ===========================================================
   // 			Constructor
   // ===========================================================

  /**
   * Instanciate an FK5 frame
   * @param equinox the equinox of definition, in Julian Year.
  **/
    public FK5(double equinox) {
	initialize(equinox, 2000.);
    }

  /**
   * Instanciate an FK5 frame (at default J2000 equinox)
  **/
    public FK5() {
	initialize(2000., 2000);
    }

   // ===========================================================
   // 			Precession in FK5 system
   // ===========================================================

   /**
    * Precession matrix from equinox t0 to t1 (Julian Years)
    * @param eq0 equinox at original equinox (julian year)
    * @param eq1 equinox of destination      (julian year)
    * @return the rotation matrix R such that   u1 = R * u0
   **/
    static final double[][] precessionMatrix(double eq0, double eq1) {
      double t0, dt, w, z, theta, zeta;
      boolean reverse = false;
      // Choose t0 as the closest to 2000.
        t0 = (eq0 - 2000.)/100.;	// Origin J2000.0
	dt = (eq1 - eq0)/100.;		// Centuries
	if (Math.abs(t0) > Math.abs(t0+dt)) {	// t0+dt = t1
	    reverse = true;
	    t0 += dt;
	    dt = -dt;
	}
	w = 2306.2181+(1.39656-0.000139*t0)*t0;	// arcsec
	zeta = (w + ( (0.30188-0.000344*t0) + 0.017998*dt) *dt)
		*dt/3600.;		// to degrees
	z    = (w + ( (1.09468+0.000066*t0) + 0.018203*dt) *dt)
		*dt/3600.;		// to degrees
	theta =  ( (2004.3109 + (-0.85330-0.000217*t0)*t0)
		 +( (-0.42665-0.000217*t0) - 0.041833*dt) *dt) 
		*dt/3600.;
	if (reverse) return(Coo.eulerMatrix(-zeta, -theta, -z));
	else return(Coo.eulerMatrix(z, theta, zeta));
    }

   // ===========================================================
   // 			toICRSmatrix
   // ===========================================================

   /**
    * Defines the rotation matrix to rotate to ICRS
    * @return the 3x3 rotation matrix to convert a position to ICRS frame
   **/
    public double[][] toICRSmatrix() {
	return(ICRSmatrix);
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

    // Default methods are OK.
}
