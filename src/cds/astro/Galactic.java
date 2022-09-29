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
                Galactic  (Astroframe -> Galactic)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The Galactic frame is defined such that the plane of our Galaxy
 * (the Milky Way) has a latitude close to zero. 
 * The galactic center has a position (0,0) in this system.
 * It was originally defined by reference to the FK4 System, with
 * <ul>
 * <li> galactic North pole at (12:49+24:24), i.e. (192.25+27.4)°
 * <li> ascending node at 33°, or equivalently an angle of 123° between the 
 *      Equatorial North Pole and the Galactic center
 * </ul>
 *
 * There are several definitions of the transformation:
 * <ul>
 * <li> [m89] Murray (1989A&amp;A...218..325M, Eq.(33)), who reports the matrix elements
 *      with 9 digits only (i.e. 1nrad, corresponding to 0.2mas max. accuracy)
 * <li> [sla] in the slaLib (P.T. Wallace / Starlink 1993) defined by
 *      "applying the standard FK4 to FK5 transformation, for inertially
 *      zero proper motion [in FK5], to the columns of the B1950 equatorial
 *      to Galactic rotation matrix", which does not correspond to a simple
 *      rotation of the Galactic frame w.r.t. FK5;
 * <li> [hip] In the Hipparcos introduction (1997, ESA-SP 1200, Vol.1, pp 91-92, Eq. 1.5.11
 *      see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
 *      Pole at (192.85948 +27.12825) node=32.93192, i.e. with an accuracy
 *      of 5 digits only (36mas). This definition was also adopted in the Gaia-DR2
 *      catalog (I/345), section 3.1.7.1 in the Gaia-DR2 documentation, see
 *      https://gea.esac.esa.int/archive/documentation/GDR2/pdf/GaiaDR2_documentation_1.1.pdf
 * <li> [lzz] from Liu, Zhu &amp; Zhang 2011A&amp;A...526A..16L, Eq.(18):
 *      Pole at (12:51:26.27549+27:07:41.7043 = 192.85948121+27.12825119)
 *      node=32.93191857 (accuracy of ~0.2mas)
 * <li> [apy] in Astropy (version 3.0.4, Aug. 2018) the angles are defined as
 *      "FK4.transform_to(FK5) from FK4 definitions", i.e. the same definitionb
 *      as [lzz], but with full accuracy, the angles being:
 *      Pole at (192.8594812065348 +27.12825118085622) and
 *      node=32.9319185680026.
 * </ul>
 * We adopted here the formal definition (192.25+27.4)° combined with the rotation of 
 * the FK4 vs ICRS as defined in {@link FK4},
 *
 * @author Francois Ochsenbein (CDS)
 * @version 1.0  05-Jun-2006 
 * @version 1.1  25-Oct-2006 : simplified
 * @version 2.0  04-Feb-2019 : 1-D matrices (faster)
 *
 */

public class Galactic extends Astroframe {

    /** Letter used to identify this frame in IAU names */
   static final char letterIAU = 'G';

   /** 
    * Rotation matrix which  moves the FK4 frame to the Galactic frame.
    *  In FK4/B1950, the Galactic Frame is defined by
    * North Pole at (RA, Dec) = 192.25 +27.4 (12h49 +27d24')
    *    longitude of ascending node = 33 deg, i.e.
    * <code>{@link AstroMath#rotation}("zyz", 192.25, 90-27.4, 90-33.0)</code>
    **/
    static public final double[] toFK4matrix = {
       // AstroMath.rotation("zyz", 192.25, 90-27.4, 90-33.0);  
       -0.0669887394151507166,   0.4927284660753235682,  -0.8676008111514348650,
       -0.8727557658519926782,  -0.4503469580199613469,  -0.1883746017229204474,
       -0.4835389146321842459,   0.7445846332830310861,   0.4601997847838516236 };

   /** (not required)
    * Rotation matrix twhich moves the Galactic frame to the FK4 frame.
    * <code>{@link AstroMath#rot_inv}({@link #toFK4matrix})</code>
    static public final double[] fromFK4matrix = {
        // AstroMath.rot_inv(toFK4matrix)
       -0.0669887394151507166,  -0.8727557658519926782,  -0.4835389146321842459,
        0.4927284660753235682,  -0.4503469580199613469,   0.7445846332830310861,
       -0.8676008111514348650,  -0.1883746017229204474,   0.4601997847838516236 };
    **/

   /** 
    * Rotation matrix which moves the Galactic frame to the ICRS frame.
    * It is the matrix product (FK4toGal) * (ICRStoFK4).
   **/
    static public final double[] fromICRSbase = {
       /* [lzz] definition, 2011A&A...526A..16L, Eq.(18), inaccurate
       -0.054875657707,     -0.873437051953,     -0.483835073621,
        0.494109437203,     -0.444829721222,      0.746982183981,
       -0.867666137554,     -0.198076337284,      0.455983813693 */
       // long double computation, as AstroMath.m3p(fromFK4matrix, FK4.fromICRSbase)
       -0.0548756577126196781,  -0.8734370519557791298,  -0.4838350736164183803,
        0.4941094371971076412,  -0.4448297212220537635,   0.7469821839845094133,
       -0.8676661375571625615,  -0.1980763372750705946,   0.4559838136911523476 };

   /** 
    * Rotation matrix which moves the ICRS frame to the Galactic frame.
    * It is the matrix product (FK4toICRS) * (GaltoFK4), or
    * <code>{@link AstroMath#m3p}({@link FK4#toICRSbase}, {@link #toFK4matrix})</code>
   **/
    static public final double[] toICRSbase = {
       // AstroMath.m3p(FK4.toICRSbase, toFK4matrix)
       /* [lzz] definition, 2011A&A...526A..16L, Eq.(18), inaccurate
       -0.054875657707,     -0.873437051953,     -0.483835073621,
        0.494109437203,     -0.444829721222,      0.746982183981,
       -0.867666137554,     -0.198076337284,      0.455983813693 */
       // long double computation, as AstroMath.m3p(FK4.toICRSbase, toFK4matrix)
       -0.0548756577126196781,   0.4941094371971076412,  -0.8676661375571625615,
       -0.8734370519557791298,  -0.4448297212220537635,  -0.1980763372750705946,
       -0.4838350736164183803,   0.7469821839845094133,   0.4559838136911523476 };

   /** 
    * Rotation matrix used by Gaia/Hipparcos, which uses an alternative definition.
   **/
    static public final double[] toHIPmatrix = 
          AstroMath.rotation("zyz", 192.85948, 90.0-27.12825, 90.0-32.93192);

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieved a frame saved in "fixedFrames".
   * The non-standard frame (e.g. as defined by Hipparcos) are never selected.
   * @param epoch   the epoch in <b>Julian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the frame if previously saved, null if not yet existing.
  **/
    public static Galactic scan(double epoch) {
        if(DEBUG) System.out.println("#...Galactic.scan(" + epoch + ")");
        if(fixedFrames==null) return(null);
        boolean anyEpoch = Double.isNaN(epoch);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = i.next();
            if(!(o instanceof Galactic)) continue;
            Galactic f = (Galactic)o;
            if(f.toICRSmatrix != toICRSbase) continue;
            if(anyEpoch||(Math.abs(f.epoch-epoch)<=Astroframe.Jsec))
                return f;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) a Galactic frame
   * @param epoch default epoch of the frame, in <b>Julian Year</b>.
   * @return The corresponding frame, created if not yet existing.
  **/
    public static Galactic create(double epoch) {
        Galactic f = scan(epoch);
        if(f==null) {
            f = new Galactic(epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Create (and mark as fixed) the default Galactic frame
   * @return  the default Galactic frame (ep=J2000), created if not yet existing.
  **/
    public static Galactic create() {
        return create(2000.0);
    }

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an Galactic frame
   * @param epoch Epoch of the frame, in <i>Julian years</i>
  **/
    public Galactic(double epoch) {
        if(Double.isNaN(epoch)) epoch = 2000.0;
    	this.precision = 7;	// Intrinsic precision = 0.01arcsec 
	this.epoch = epoch;
	this.name = "Galactic";
        full_name = name + "(Ep=J" + epoch + ")";
	this.toICRSmatrix   = toICRSbase;
        this.fromICRSmatrix = fromICRSbase;
    }

   // ===========================================================
   // 			Alternative Galactic frames
   // ===========================================================

  /**
   * The galactic frame defined in Gaia-DR2.
   * It is defined the same way as Hipparcos, i.e. the Galactic North pole is
   * at (192.85948 +27.12825) and node of galactic centre at 32.93192,
   * see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
   * @return the Galactic frame used in Gaia-DR2, at Epoch=J2015.5.
  **/
    static public Galactic Gaia2() {
        String frame_name = "Galactic-Gaia2";
        Galactic frame = (Galactic)getFrame(frame_name);
        if(frame==null) {
            frame = new Galactic(2015.5);
            frame.toICRSmatrix   = toHIPmatrix;
            frame.fromICRSmatrix = AstroMath.rot_inv(toHIPmatrix);
            frame.fixed |= HIPdef;	// Indicates non-standard frame
    	    frame.precision = 9;	// Intrinsic precision = 0.1mas 
            frame.name = frame.full_name = frame_name;
            frame.fixFrame(frame_name);
        }
        return frame;
    }

  /**
   * The galactic frame defined in Hipparcos.
   * It is defined the same way as Hipparcos, i.e. the Galactic North pole is
   * at (192.85948 +27.12825) and node of galactic centre at 32.93192,
   * see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
   * @return the Galactic frame used in Hipparcos, at Epoch=J1991.25.
  **/
    static public Astroframe Hipparcos() {
        String frame_name = "Gal-Hip";
        Galactic frame = (Galactic)getFrame(frame_name);
        if(frame==null) {
            frame = new Galactic(1991.25);
            frame.toICRSmatrix   = toHIPmatrix;
            frame.fromICRSmatrix = AstroMath.rot_inv(toHIPmatrix);
            frame.fixed |= HIPdef;	// Indicates non-standard frame
    	    frame.precision = 9;	// Intrinsic precision = 0.1mas 
            frame.name = frame.full_name = frame_name;
            frame.fixFrame(frame_name);
        }
        return frame;
    }

  /**
   * The galactic frame defined in FK4.
   * It is defined the same way as Hipparcos, i.e. the Galactic North pole is
   * at (192.85948 +27.12825) and node of galactic centre at 32.93192,
   * see ftp://cdsarc.u-strasbg.fr/pub/cats/I/239/version_cd/docs/vol1/sect1_05.pdf
   * @return the standard Galactic frame, at Epoch=B1950.0
    static public Astroframe FK4() {
        if(DEBUG) System.out.println("#...Galactic.FK4: currently " + (fixedFrames==null? 0 : fixedFrames.size()) + " frames.");
        String frame_name = "FK4-Gal(Ep=B1950)";
        double epoch = Astrotime.B2J(1950.0);
        Astroframe frame = getFrame(frame_name);
        if(frame!=null) return(frame);
        // Create it -- but only the rotation part
        frame = create(frame_name, epoch, AstroMath.m3p(FK4.toICRSbase, toFK4matrix));
	frame.precision = 6;	// Intrinsic precision = 0.1arcsec
        return frame;
    }
  **/

   // Default methods are OK.

}
