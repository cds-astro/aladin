package cds.astro;

/*==================================================================
                ICRS  (Astroframe -> ICRS)
 *==================================================================*/

import java.util.*;
import java.text.*;	// for parseException

/**
 * The ICRS frame (International Celestial Reference System) is defined 
 * by the Hipparcos mission. It is also named <em>ICRF</em>.
 *
 *  
 * @author Francois Ochsenbein (CDS)
 *
 */

public class ICRS extends Astroframe {

  /** Letter used to identify this frame in IAU names */
   static final char letterIAU = 'I';

  /**
   * ICRS at base_epoch is the pivot (intertial frame).
   * Expressed in <b>Julian years</b>
    static public final double base_epoch = 2000.0;
   */
    static public final double[] toICRSbase = AstroMath.U3matrix;

   // ===========================================================
   // 	Retrieve an existing frame among fixedFrames
   // ===========================================================

  /**
   * Retrieve a frame saved in "fixedFrames".
   * @param epoch   the epoch in <b>Julian</b> Year.
   *        (accept difference in epoch of ~1sec)
   * @return  the corresponding frame if existing, <em>null</em> if not existing.
  **/
    public static ICRS scan(double epoch) {
        if(DEBUG) System.out.println("#...ICRS.scan(" + epoch + ")");
        if(fixedFrames==null) return(null);
        boolean anyEpoch = Double.isNaN(epoch);
        Iterator i = fixedFrames.iterator();
        while(i.hasNext()) {
            Object o = (Object)i.next();
            if(!(o instanceof ICRS)) continue;
            ICRS f = (ICRS)o;
            if((f.fixed&0xf)!=0)	// non-standard frame
                continue;
            if(anyEpoch||(Math.abs(f.epoch-epoch)<=Astroframe.Jsec))
                return f;
        }
        return(null);
    }

  /**
   * Create (and mark as fixed) an ICRS frame
   * @param epoch default epoch, in <b>Julian</b> Year.
   * @return The corresponding frame, created if necessary.
  **/
    public static ICRS create(double epoch) {
        ICRS f = scan(epoch);
        if(f==null) {
            f = new ICRS(epoch);
            f.fixFrame();
        }
        return(f);
    }

  /**
   * Create (and mark as fixed) the default ICRS frame
   * @return The default ICRS(Ep=J2000) coordinate frame.
  **/
    public static ICRS create() {
	return create(2000.0);
    }

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an ICRS frame
   * @param epoch the default epoch, in Julian years
  **/
    public ICRS(double epoch) {
    	this.precision = 9;		// Intrinsic precision = 0.1mas
	this.epoch = Double.isNaN(epoch) ? 2000: epoch;
	this.name = "ICRS";
        full_name = "ICRS(Ep=J" + epoch + ")";
	toICRSmatrix = toICRSbase;	// Identity matrix
	fromICRSmatrix = toICRSbase;	// Identity matrix
	hms = true;			// Sexagesimal is h m s in RA
        ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

  /**
   * Get the conversion to ICRS matrix
   * @return Indentity matrix
    public double[] toICRSmatrix() {
	return(AstroMath.U3matrix);
    }
  **/

  /**
   * Convert the position to its ICRS equivalent.
   * @param coo on input the position in this frame; on ouput the ICRS
  **/
    public void toICRS(Coo coo) {
	// Nothing to do !
    }

  /**
   * Convert the position from the ICRS frame.
   * @param coo on input the ICRS position, on output its local equivalent
  **/
    public void fromICRS(Coo coo) {
	// Nothing to do !
    }

  /**
   * Convert the position to its ICRS equivalent.
   * @param u a 6-vector
    public void toICRS(double[] u) {
	// Nothing to do !
    }
  **/

  /**
   * Convert the position from the ICRS frame.
   * @param u a 6-vector
    public void fromICRS(double[] u) {
	// Nothing to do !
    }
  **/

}
