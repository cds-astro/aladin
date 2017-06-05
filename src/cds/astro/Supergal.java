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

/*==================================================================
                Galactic  (Astroframe -> Supergal)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The Supergalactic frame is defined such (0,0) represents the direction 
 * toward the center of the Local Group of galaxies.
 * The Supergalactic frame is defined by reference to the Galactic System.
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */

public class Supergal extends Astroframe {

  /**
   * Name of this frame
  **/
    static public String class_name = "Supergal";

  /** 
   * Supergalactic is defined on Galactic.
   * We therefore need the knowledge of the Galactic system.
  **/

   /**
    * Constants for Galactic to SuperGalactic.
    * Pole of SuperGalactic at (Glon, Glat) = 47.37 +06.32
    * longitude of ascending node = 0.0
   **/

   /** 
    * Rotation matrix to move from Galactic to Supergalactic. 
   **/
    static public final double[][] supergal = {  // Euler(-90, 83.68, -47.37)
      {-0.7357425748043749, 0.6772612964138942,        0.},
      {-0.0745537783652337,-0.0809914713069767, 0.9939225903997749},
      { 0.6731453021092076, 0.7312711658169645, 0.1100812622247821}
    };

   // ===========================================================
   // 			Constructor
   // ===========================================================

  /**
   * Instanciate an Supergal frame. Default epoch is B1950.
  **/
    public Supergal() {
    	this.precision = 5;	// Intrinsic precision = 1arcsec 
	this.name = class_name;
	epoch = 2000.;		// May be changed by setFrameEpoch()
    }

   // ===========================================================
   // 			toICRSmatrix
   // ===========================================================

   /**
    * Defines the rotation matrix to rotate to ICRS
    * @return the 3x3 rotation matrix to convert a position in ICRS frame
    * = (supergal * gal_2000)<sup>t</sup>
   **/
    public double[][] toICRSmatrix() {
	if (ICRSmatrix == null) ICRSmatrix = AstroMath.m3t(
		AstroMath.m3p(supergal, Galactic.gal_2000));
    	return(ICRSmatrix);
    }

   // ===========================================================
   // 			Convert to FK5
   // ===========================================================

   // Default methods are OK.

}
