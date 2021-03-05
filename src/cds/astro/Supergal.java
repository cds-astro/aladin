package cds.astro;

/*==================================================================
                Galactic  (Astroframe -> Supergal)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The Supergalactic frame is defined such (0,0) represents the direction 
 * toward the center of the Local Group of galaxies.
 * The Supergalactic frame is defined by reference to the Galactic System,
 * if <i>glon, glat</i> are the galactic longitude/latitude and
 * <i>sglon, sglat</i> the supergalactic longitude/latitude:
 * <pre>
 * | sglon sglat |   glon   glat  |
 * |   0   +90   |  47.37  +6.32  |
 * |   0     0   | 137.37   0.00  |
 * </pre>
 *  
 * @author Francois Ochsenbein (CDS)
 * @version 1.0  05-Jun-2006 
 * @version 1.1  25-Oct-2006 : simplified
 * @version 2.0  04-Feb-2019 : 1-D matrices (faster)
 *
 */

public class Supergal extends Astroframe {

  /** Letter used to identify this frame in IAU names */
   static final char letterIAU = 'S';

   /**
    * Constants for Galactic to SuperGalactic.
    * Pole of SuperGalactic at (Glon, Glat) = 47.37 +06.32, and
    * the longitude of ascending node = 0.0
   **/

   /** 
    * Rotation matrix to align the Galactic frame to the Supergalactic one.
    * The alignment is obained with
    * <code>{@link AstroMath#rotation}("zx", 137.37, 90-6.32)</code>
   **/
    static public final double[] toGalMatrix = {
        // AstroMath.rotate("zx",137.37, 90-6.32)
       -0.7357425748043749364,  -0.0745537783652337489,   0.6731453021092075892,
        0.6772612964138942666,  -0.0809914713069767324,   0.7312711658169645168,
        0.0                  ,   0.9939225903997749305,   0.1100812622247820664 };

   /** 
    * Rotation matrix to align the Supergalactic frame to the Galactic one.
    * It is: <code>{@link AstroMath#rot_inv}({@link #toGalMatrix})</code>
   **/
    static public final double[] fromGalMatrix = {
        // AstroMath.rot_inv(toGalMatrix)
      -0.7357425748043749364,   0.6772612964138942666,   0.0,
      -0.0745537783652337489,  -0.0809914713069767324,   0.9939225903997749305,
       0.6731453021092075892,   0.7312711658169645168,   0.1100812622247820664 };

   /** 
    * Rotation matrix to change ICRS position into Supergal.
    * It is defined as (Gal.to.SuperG) * (ICRS.to.Gal), or
    * <code>{@link AstroMath#m3p}({@link #fromGalMatrix}, {@link Galactic#fromICRSbase})</code>
    */
    static public final double[] fromICRSbase = { 
       // AstroMath.m3p(fromGalMatrix, Galactic.fromICRSbase)
       0.3750155557060191496,   0.3413588718572082374,   0.8618801851666388868,
      -0.8983204377254853439,  -0.0957271002509969235,   0.4287851600069993011,
       0.2288749093788964371,  -0.9350456902643365859,   0.2707504994914917474 };

   /** 
    * Rotation matrix to change Supergal position into ICRS.
    * It is: <code>{@link AstroMath#rot_inv}({@link #fromICRSbase})</code>
    */
    static public final double[] toICRSbase = { 
       // AstroMath.rot_inv(fromICRSbase)
       0.3750155557060191496,  -0.8983204377254853439,   0.2288749093788964371,
       0.3413588718572082374,  -0.0957271002509969235,  -0.9350456902643365859,
       0.8618801851666388868,   0.4287851600069993011,   0.2707504994914917474 };

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieved a frame saved in "fixedFrames".
   * @param epoch   the epoch in <b>Julian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the frame if previsouly saved, null if not yet existing.
  **/
    public static Supergal scan(double epoch) {
        if(DEBUG) System.out.println("#...Supergal.scan(" + epoch + ")");
        if(fixedFrames==null) return(null);
        boolean anyEpoch = Double.isNaN(epoch);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = i.next();
            if(!(o instanceof Supergal)) continue;
            Supergal f = (Supergal)o;
            if(Math.abs(f.epoch-epoch)<=Astroframe.Jsec)
                return f;;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) a SuperGalactic frame
   * @param epoch Epoch asked, in <em>Julian Years</em>
   * @return The frame
  **/
    public static Supergal create(double epoch) {
        Supergal f = scan(epoch);
        if(f==null) {
            f = new Supergal(epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Instanciate the default Supergal frame
   * @return The standard Supergalactic coordinate frame, for Epoch=J2000.
  **/
    public static Supergal create() {
        return create(2000.0);
    }

   // ===========================================================
   // 			Constructor
   // ===========================================================

  /**
   * Instanciate an Supergal frame. Default epoch is 2000.
   * @param epoch epoch of frame, in Jyr.
  **/
    public Supergal(double epoch) {
        if(Double.isNaN(epoch)) epoch = 2000.0;
    	this.precision = 5;	// Intrinsic precision = 1arcsec 
	this.epoch = epoch;	// May be changed by setFrameEpoch()
	this.name = "Supergal";
        full_name = "Supergal(Ep=J" + epoch + ")";
        toICRSmatrix = toICRSbase;
        fromICRSmatrix = fromICRSbase;
    }

   // ===========================================================
   // 			Conversion to ICRS
   // ===========================================================

   // Default methods are OK.

}
