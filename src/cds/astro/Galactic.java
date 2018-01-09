// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.astro;

/*==================================================================
                Galactic  (Astroframe -> Galactic)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The Galactic frame is defined such that the plane of our Galaxy
 * (the Milky Way) has a latitude close to zero.
 * The galactic center has a position (0,0) in this system.
 * It was originally defined by reference to the FK4 System.
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */

public class Galactic extends Astroframe {

  /**
   * Name of this frame.
  **/
    static public String class_name = "Galactic";

   /**
    * Constants to transform from  Galactic to SuperGalactic:
    * See Supergal
   **/

   //  In FK4/B1950, the Galactic Frame is defined by
   // North Pole at (RA, Dec) = 192.25 +27.4 (12h49 +27d24')
   //    longitude of ascending node = 33 deg
   // Matrix B1950 to Gal = Euler(asc.node-90, 90-lat(Pole), -lon(Pole))
   /** Rotation matrix to move from FK4 to Galactic. */
   static public final double[][] gal_1950 = {	// Euler(-57, 62.6, -192.25)
     {-0.0669887394151508,-0.8727557658519927,-0.4835389146321842},
     { 0.4927284660753235,-0.4503469580199614, 0.7445846332830311},
     {-0.8676008111514348,-0.1883746017229204, 0.4601997847838516}
   } ;

   /** 
    * Rotation matrix to move from FK5 to Galactic.
    * (from Hipparcos documentation vol1, Eq. 1.5.11, see
    * ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
    * transformed to have a truly orthogonal matrix.
   **/
    static public final double[][] gal_2000 = {
    // -0.0548755604,       -0.8734370902,       -0.4838350155
      {-0.0548755604024359, -0.8734370902479237, -0.4838350155267381},
    //  0.4941094279,       -0.4448296300,        0.7469822445},
      { 0.4941094279435681, -0.4448296299195045,  0.7469822444763707},
    // -0.8676661490, -0.1980763734,  0.4559837762}
      {-0.8676661489811610, -0.1980763734646737,  0.4559837762325372}
    };

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an Galactic frame
  **/
    public Galactic() {
    	this.precision = 5;	// Intrinsic precision = 1arcsec 
	this.name = class_name;
	epoch = 2000.;		// May be changed by setFrameEpoch()
	ICRSmatrix = AstroMath.m3t(gal_2000);
    }

   // ===========================================================
   // 			toICRSmatrix
   // ===========================================================

   /**
    * Defines the rotation matrix to rotate to ICRS
    * @return the 3x3 rotation matrix to convert a position in ICRS frame
   **/
    public double[][] toICRSmatrix() {
	return(ICRSmatrix);
    }

   // ===========================================================
   // 			Convert to FK5
   // ===========================================================

   // Default methods are OK.

}
