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

  /**
   * Name of this firame.
  **/
    static public String class_name = "ICRS";

   // ===========================================================
   // 			Contructor
   // ===========================================================

  /**
   * Instanciate an ICRS frame
   * @param epoch the default epoch, in Julian years
  **/
    public ICRS(double epoch) {
    	this.precision = 9;		// Intrinsic precision = 0.1mas
	ICRSmatrix = Coo.Umatrix3;	// Identity matrix
	this.name = class_name;
	this.epoch = epoch;
	hms = true;			// Sexagesimal is h m s in RA
        ed_lon = Editing.SEXA3c|Editing.ZERO_FILL;
        ed_lat = Editing.SEXA3c|Editing.ZERO_FILL|Editing.SIGN_EDIT;
    }

  /**
   * Instanciate an ICRS frame
  **/
    public ICRS() {
	this(2000.);
    }

  /**
   * Instanciate an ICRS frame
   * @param text the default epoch, e.g. "J1991.25"
  **/
    public ICRS(String text) throws ParseException {
        this();
        Astrotime t = new Astrotime();
        int o = t.parse(text, 0);
        if (o > 0) 		// Epoch expressed in Julian years
	    epoch = t.getJyr();
	while (o<text.length() && Character.isWhitespace(text.charAt(o))) o++;
	if (o<text.length()) throw new ParseException
	    ("****Astroframe: '" + text + "'+" + o, o);
    }

   // ===========================================================
   // 			Convert To/From ICRS
   // ===========================================================

  /**
   * Get the conversion to ICRS matrix
   * @return Indentity matrix
  **/
    public double[][] toICRSmatrix() {
	return(Coo.Umatrix3);
    }

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
  **/
    public void toICRS(double[] u) {
	// Nothing to do !
    }

  /**
   * Convert the position from the ICRS frame.
   * @param u a 6-vector
  **/
    public void fromICRS(double[] u) {
	// Nothing to do !
    }

}
