package cds.astro;

import java.io.Serializable;
import java.util.*;

/**
 * This abstract class defines the required methods to instanciate the
 * definition of a frame used for astronomical coordinates.
 * @author Francois Ochsenbein
 * @version 1.1 23-Apr-2006: addition of toICRS(6-D vector), and ICRSmatrix.
 * @version 1.2 06-Jun-2006: simplified version, only 2 abstract methods
 * @version 1.3 13-Nov-2008: method create(String)
 * @version 1.4 08-Apr-2009: method create(char)
 * @version 1.5 18-Nov-2014: avoid interpretation as epoch
 * @version 2.0 03-Feb-2019: 1-D matrix (faster); simplified
 * 		Accept non-standard frame creation as e.g. Galactic.Gaia
 *
 */
public abstract class Astroframe implements Serializable { //, Cloneable
    static protected boolean DEBUG=false;
    static protected double eps = AstroMath.eps;	// for test()
    /* 
     * The limit of precision for epoch/mean epoch: 1s, about 3.e-8yr
     */
    static public final double Jsec = 1.0/(365.25*86400.);
    /*
    static private Editing edframe = new Editing("?frame?");
    */
    /**
     * Derivative of the propagation matrix */
    static public final double[] Udot = {
        0, 0, 0,  1, 0, 0, 
        0, 0, 0,  0, 1, 0,
        0, 0, 0,  0, 0, 1,
        0, 0, 0,  0, 0, 0,
        0, 0, 0,  0, 0, 0,
        0, 0, 0,  0, 0, 0 };
    /**
     * Frames having a fixed epoch are saved in a list
     */
    static protected ArrayList fixedFrames = null;
    /**
     * ... and the names of these frames are saved in a HashMap(key=name, value=integer) 
     */
    static protected HashMap fixedNames = null;
    /** 
     * List of valid 1-letter frames used in IAU designations.
     * Numbers 4,5,6 require an equinox.
     * FK4 must be #4, FK5 #5
     */
    static public final char[] IAUframes = { 
        '\u001b', // escape, currently Gaia
        ICRS.letterIAU, 	// 'I' [1]
        Galactic.letterIAU, 	// 'G' [2]
        Supergal.letterIAU, 	// 'S' [3]
        FK4.letterIAU,		// 'B' [4]
        FK5.letterIAU,		// 'J' [5]
        Ecliptic.letterIAU, 	// 'E' [6]
    };
    /** 
     * The name of the frame (does not include the epoch)
    **/
    public String name;
    /** 
     * The full name of the frame (includes the epoch, is unique).
     * It uses parentheses () for the epoch, or brackets [] if rame fixed.
    **/
    public String full_name;


   /** 
    * The defaut epoch, a constant which is fixed for a class of frames.
    * Is always expressed in <b>Julian years</b>.
    * It must be defined in each sub-class.
    public static final double base_epoch = 0./0.;
    **/

   /** 
    * The epoch, expressed in <b>Julian Year</b>.
    * This epoch may be changed by setFrameEpoch, <b>if not marked fixed</b>
    * by {@link #fixFrame}.
    **/
    public double epoch = 0./0.;

    /**
     * The sexagesimal representation of the longitude may be expressed 
     * in time unit. This is the case of the Equatorial frames,
     * but also in the ICRS.
    **/
     public boolean hms = false;

    /**
     * Conversion from ICRS to this frame, must be defined in each sub-class.
     * This matrix can be made of:<ul>
     * <li> 9 doubles: the frame is in rotation only with ICRS (no spin).
     * Is {@link AstroMath#rotation}("xyz", ax, ay, az) in the canonical definition
     * of rotations around <i>x y z</i> axes.
     * <li> 18 doubles: there is a spin in addition to the rotation.
     * Is {@link AstroMath#rotspin}("xyz", ax, ay, az, sx, sy, sz) in the canonical
     * definition of rotations and spins around <i>x y z</i> axes.
     * </ul>
     * This matrix is the inverse of {@link #fromICRSmatrix}.
     *
    **/
    public double[] fromICRSmatrix = null;

    /**
     * Conversion from this frame to ICRS, must be defined in each sub-class.
     * This matrix consists of 9 or 18 doubles, and 
     * is the inverse of {@link #fromICRSmatrix}.
    **/
    public double[] toICRSmatrix = null;

    /** Highest (absolute) value of spin matrix, an indicator to state whether
     * it's necessary to take the spin into account in a change of epoch 
     */
    protected double max_spin = 0;

    /**
     * Change of epoch in the frame. It is the "Udot" matrix, unless the
     * frame is spinning like FK4 or FK5. 
    **/
    public double[] dotMatrix = Udot;

    /**
     * Merker which indicates a frame with a fixed epoch, and also compatibility.
     * A frame marked as <i>fixed</i> (via {@link #fixFrame}) has its left-bit set to <tt>1</tt>
     * (<tt>0x80</tt>). Once fixed,  a frame is included in the {@link #fixedFrames} list.
     * The interest of a <i>fixed frame</i> is to avoid a recomputation
     * of the matrices required for a conversion between frames.<br>
     * The four low-order bits (<tt>0x0f</tt>) of this marker indicate non-standard frames;
     * currently only Hipparcos-Gaia defined Galactic and Ecliptic frames are marked with HIPdef.
    **/
     public byte fixed = 0;
     /** Mark sued for HIP-Gaia definition */
     static final byte HIPdef = 1;

    /** 
     * The defaut precision (0=unknown, 3=1', 5=1").
     * The precision value is expressed as the number of decimals (+1), i.e.
     * 0=unknown, 1=deg 3=1' 5=1" 8=1mas 11=1uas, etc...
    **/
    public byte precision = 8;	// Default edition to 1mas = 3e-7 deg

    /** 
     * Default edition of Longitude/Right Ascension.
     * It contains one of the options in {@link Astroformat} among
     * DECIMAL, SEXA..., eventualled or'ed with SIGN_EDIT | ZERO_FILL
    **/
     public byte ed_lon = Editing.DECIMAL|Editing.ZERO_FILL;
    /** 
     * Default edition of Latitude/Declination.
     * It contains one of the options in {@link Astroformat} among
     * DECIMAL, SEXA..., eventualled or'ed with SIGN_EDIT | ZERO_FILL
    **/
     public byte ed_lat = Editing.DECIMAL|Editing.ZERO_FILL|Editing.SIGN_EDIT;

     /**
      * Usage statistics: number of gets
      */
     protected int usage=0;

    /** 
     * Conversion matrices to destination frames are saved, avoiding the
     * recomputation of these conversion matrices.
    **/
     public HashMap savedConversions = null;

    /**
     * Dump the contents of an Astroframe.
     * @param title A title to precede the dump.
     *        When the title contains <code>(R)</code>, dump is recursive:
     *        all conversions related to the frame are listed.
    **/
    public void dump(String title) {
        //System.out.println("#...Astroframe.dump of " + this + "; class:" + this.getClass());
        boolean recursive = title.indexOf("(R)")>=0;
        String indent = title.charAt(0) == '#' ? "#   " : "    ";
        System.out.print(title);
        if(title.endsWith("\n")) System.out.print(indent);
        System.out.println("[" + this.getClass() + "] name=(" + name + ") full_name=(" + full_name + ") epoch=J" + epoch + " fixed=0x" + Integer.toHexString(fixed&0xff)
                 + " usage=" + usage);
        System.out.print(indent + " precision=" + precision + ", ed_lon=0x" + Integer.toHexString(ed_lon) + '(' + Astroformat.explain(ed_lon) + ')');
        System.out.print(" ed_lat=0x" + Integer.toHexString(ed_lat) + '(' + Astroformat.explain(ed_lat) + ')');
        if((fixed&0x80)!=0) {	// Edit all names
            int iframe = fixedFrames.indexOf(this);
            System.out.print("\n" + indent + "#" + iframe + ";\tNames:");
            Set names = fixedNames.keySet();
            Iterator lof = names.iterator();
            while(lof.hasNext()) {
                String fname = (String)lof.next();
                Integer fno = (Integer)fixedNames.get(fname);
                if(fno.intValue() == iframe) System.out.print(" \"" + fname + "\"");
            }
        }
        if(savedConversions!=null) {
            System.out.print("; savedConversions=" + savedConversions.size());
            if(recursive) {
                System.out.print(":");
                Set soc = savedConversions.keySet();
                Iterator loc = soc.iterator();
                for(int ic=0; loc.hasNext(); ic++) {
                    Astroframe o = (Astroframe)loc.next();
                    double[] m = (double[])savedConversions.get(o);
                    System.out.print("\n" + indent + " * [" + m.length + "]=>" + o.full_name);
                }
            }
        }
        System.out.print("\n");
        AstroMath.printMatrix(indent + "toICRSmatrix:\n", toICRSmatrix);
        AstroMath.printMatrix(indent + "fromICRSmatrix:\n", fromICRSmatrix);
        AstroMath.printMatrix(indent + "dotMatrix(max_spin=" + max_spin + "):\n", dotMatrix);
     }

    /**
     * Dump the contents of all saved Astroframes.
     * @param title A title to precede the dump.
     *        When the title contains <code>(R)</code>, dump is recursive:
     *        all conversions related to all frame ares listed.
    **/
    public static void dumpAll(String title) {
        System.out.print(title);
        if(fixedFrames==null) { System.out.println(" (empty)"); return; }
        System.out.println(" (" + fixedFrames.size() + " recorded)");
        boolean recursive = title.indexOf("(R)")>=0;
        Iterator lof = Astroframe.fixedFrames.iterator();
        int count = 0;
        while(lof.hasNext()) {
            Astroframe f = (Astroframe)lof.next();
            String ftitle = "[" + count + "]";
            if(count<10)  ftitle = ftitle + " ";
            if(recursive) ftitle = ftitle + " (R)";
            f.dump(ftitle);
            count++;
        }
        System.out.println(title + " (End)");
    }

    /**
     * Retrieve or create Gaia-DR2 frame.
     * It is the #0 in the list of fixedFrames.
     * @return the Gaia-DR2 frame (ICRS, Ep=J2015.5)
    **/
    public static Astroframe Gaia2() {
        String frame_name = "Gaia2";
        Astroframe gaia = getFrame(frame_name);
        if(gaia == null)  {
            gaia = (Astroframe)ICRS.create(2015.5);
            gaia.fixFrame(frame_name);
        }
        gaia.usage += 1;
	return gaia;
    }

    /**
     * Retrieve or create Hipparcos frame.
     * It is the #0 in the list of fixedFrames.
     * @return the Hipparcos frame (ICRS, Ep=J1991.25)
    **/
    public static Astroframe Hipparcos() {
        String frame_name = "Hipparcos";
        Astroframe hip = getFrame(frame_name);
        if(hip==null) {
            hip = (Astroframe)ICRS.create(1991.25);
            hip.fixFrame(frame_name);
        }
        hip.usage += 1;
	return hip;
    }

    /**
     * Retrieve (or create if non-existing) the standard Astroframe from a single char (J, B, G, etc)
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal).
     *         Lowercase characters are not recognized.
     * @return the corresponding astroframe / null if character not used.
    **/
    public static Astroframe create(char sym) {
        if(fixedFrames==null)
            ICRS.create();	// this forces the initialization
        for(int i=1; i<IAUframes.length; i++) {
            if(sym!=IAUframes[i]) continue;
            Astroframe f = (Astroframe)fixedFrames.get(i);
            f.usage += 1;
            return(f);
        }
	return(null);
    }

    /**
     * Retrieve (or create if non-existing) the default Astroframe (ICRS at Ep=J2000)
     * @return the default Astroframe
    **/
    public static Astroframe create() {
        return (Astroframe)ICRS.create();
    }

    /**
     * Retrieve a frame from its name.
     * Terminating colon (:) or surrounding quote (") are not part of the name.
     * @param name Name which was used in {@link #fixFrame}
     * @return the corresponding frame, or null when not existing
     **/
    public final static Astroframe getFrame(String name) {
        if(name.length()<1)
            return(null);
        if(fixedNames==null) 
            ICRS.create();	// this forces the initialization
        Integer ordi = (Integer)fixedNames.get(name);
        if(ordi==null) {	// Test Quoted name
            int start = 0;
            int end = name.length()-1;
            boolean mod = false;
            if(name.charAt(end)==':') { mod=true; end--; }
            if((name.charAt(0)=='"') && (name.charAt(end)=='"')) { mod=true; start++; end--; }
            //System.out.println("#...trying getFrame(" + name + "▶" + name.substring(start, end+1) + "◀)");
            if(mod) ordi = (Integer)fixedNames.get(name.substring(start, end+1));
        }
        // if(DEBUG) System.out.println("#...trying getFrame with name=<" + name + "> => " + (ordi==null ? "null" : ordi.intValue()));
        return ordi==null ? null : (Astroframe)fixedFrames.get(ordi.intValue());
    }

    /**
     * Insert or replace the epoch
     **/
    //private final void changeEditedEpoch(boolean full_name) 
    private final static String updateEpoch(String input_name, double epoch, boolean add) {
        String name = input_name;
        int i = input_name.indexOf("Ep=");
        if(i>=0) {
            char c = name.charAt(i+3);
            if(c=='B') 
                name = name.substring(0, i+4) + Astrotime.J2B(epoch);
            else 
                name = name.substring(0, i+3) + "J" + epoch;
        }
        else if(!add) return(input_name);
        else name = name + ",Ep=J" + epoch;
        if((name.indexOf('(')>=0)&&(name.indexOf(')')<0))
            name = name + ')';
        return(name);
    }

    /**
     * Interpret a text for an Astroframe.
     * @param  txt A text which contains, either a name used in a previous {@link #fixFrame},
     * or a text representing a "standard" frame among:
     * <UL>
     * <LI> (1) I = IRCS (no equinox)
     * <LI> (2) G = Galactic (no equinox)
     * <LI> (3) S = Supergalactic (no equinox)
     * <LI> (4) B = FK4 eventually followed by equinox (Besselian)
     * <LI> (5) J = FK5 eventually followed by equinox (Julian)
     * <LI> (6) E = Ecliptic, eventually followed by equinox (Julian)
     * <LI> FK4(equinox), equivalent to Bequinox, e.g. "FK4(1950)" or "B1950"
     * <LI> FK5(equinox), equivalent to Jequinox, e.g. "FK5(2000)" or "J2000"
     * </UL>
     * An epoch might be added, e.g. 
     * <font color='darkred'>ICRS(1991.25)</font> or 
     * <font color='darkred'>ICRS(Epoch=1991.25)</font> 
     * for the frame used in Hipparcos.
     * @return null if can't be interpreted, can't be created, or conflicts
     *         with existing fixed Astroframes.
    **/
    public static Astroframe parse(Parsing txt) {
        return(parse2(txt, true));
    }

    /**
     * Interpret a text which should contain the name of an existing frame.
     * @param  txt A text which contains, either a name used in a previous {@link #fixFrame},
     * @param  create "true" if frame should be created if non-existent
     * @return null if can't be interpreted, can't be created, or conflicts
     *         with existing fixed Astroframes.
    **/
    public static Astroframe parse2(Parsing txt, boolean create) {
        //if(DEBUG) System.out.println("#---Astroframe.parse2(" + txt + ")\t-- starting, fixedFrames=" + (fixedFrames==null? 0 : fixedFrames.size())
        //        + ", fixedNames=" + (fixedNames==null? 0 : fixedNames.size()));
        if(fixedNames==null) 
            ICRS.create();	// this forces the initialization
        boolean has_par=false;
        double equinox = 0./0.;
        double epoch = 0./0.;
        boolean basic = true;	// indicates a basic frame
        Astrotime atime = new Astrotime();
        boolean time_ok = false;
        Astroframe frame = null;
        txt.gobbleSpaces();
        int posini = txt.pos;
        // Could be within brackets
	boolean bracketed = txt.currentChar() == '(';
        int end = txt.length;	// Where the frame name stops
        int posend = end;	// position once interpreted
        if(bracketed) {
            end = txt.matchingBracket(); 
            if(end<0) 		// Non-closed ()
                return(null);
            posend = end+1;	// after )
            txt.advance(1); 	// skip '('
            txt.gobbleSpaces();
        }
        else {	// Name normally does not include blank.
            int o=txt.pos;
            while((txt.pos<end)&&(txt.currentChar()!=' ')) txt.advance(1);
            posend = end = txt.pos;
            // However, last char of name should not be ':' or '='
            txt.advance(-1); char c=txt.currentChar();
            if((c==':')||((c=='='))) end--; 
            txt.set(o);
        }
        frame = getFrame(txt.toString(end-txt.pos));
        if(frame!=null) {	// Was found:
            txt.set(posend);
            frame.usage += 1;
            return(frame);
        }
        // Not a standard name, try to interpret...
        int id = txt.lookup(IAUframes);
        if(DEBUG) System.out.println("#...Astroframe.parse: id=" + id + " <" + txt + ">");
        if(id>=0) {	// Skip the full name (e.g. "ICRS" or "Galactic")
            if((id==2)&&(txt.matchIgnoreCase("aia"))) id = 1; 	// ICRS
            if((id!=4)&&(id!=5)) {
                // J or B: indicates FK4/FK5, must be followed by digit
                while (Character.isLetter(txt.currentChar())) txt.advance(1);
                txt.match('.');	// Accept e.g. Gal.
            }
        }
        else if(txt.matchIgnoreCase("FK")) {
            //if(DEBUG) System.out.println("#...Astroframe.parse matched 'FK' followed by " + txt.currentChar());
            id = Character.getNumericValue(txt.currentChar());
            if((id==4)||(id==5)) txt.advance(1); 
            else id = -1;
        }
        else if(txt.matchIgnoreCase("Hip")) {
            epoch = 1991.25;
            id = 1;	// ICRS
            while (Character.isLetter(txt.currentChar()))
                txt.advance(1);
            txt.match('.');	// Accept Hip.
        }
        //if(DEBUG) System.out.println("#...Astroframe.parse <" + txt + ">: id=" + id);
        if(id<0) {	// Standard frame not recognized...
            txt.set(posini);
            return(null);
        }
        // Frame type (ICRS, FKx, galactic...) detected, interpret equinox/epoch
        has_par = txt.match('(');
        if((id>=4)&&(id<=6)) {
            // Equinox is required.
            txt.matchIgnoreCase("Eq=");
            double basic_equinox = id==4 ? 1950.0 : 2000.0;
            if(Character.isDigit(txt.currentChar())) 
                equinox = txt.parseDecimal();
            else if(time_ok = atime.parse(txt)) 
                equinox = id==4 ? atime.getByr() : atime.getJyr();
            else 
                equinox = basic_equinox;
            basic = equinox == basic_equinox;
            if(DEBUG) System.out.println("#...Astroframe.parse: recognized Eq=" + equinox);
        }
        if(has_par && txt.match(')')) has_par=false;
        // Recognize the Epoch, if present
        // May be written ,Ep= or , or (...)
	if(has_par || bracketed) txt.gobbleSpaces();	
        if(!has_par) has_par = txt.match('(');
	if(!txt.match(',')) txt.match('/');	// Accept comma or slash between equinox/epoch
	if(Character.toUpperCase(txt.currentChar()) == 'E') {
            //System.out.println("#...Astroframe.parse: examine <" + txt + ">");
	    txt.advance(1);
	    if (Character.toLowerCase(txt.currentChar()) == 'p') {
		while (Character.isLetter(txt.currentChar())) txt.advance(1);
	    }
            //System.out.println("#...Astroframe.parse: examine <" + txt + ">");
	    if(!txt.match('=')) txt.match(':');
	}
	if(has_par || bracketed) txt.gobbleSpaces();	
        if(DEBUG) System.out.println("#...Astroframe.parse: examine <" + txt + ">; equinox=" + equinox + ", has_par=" + has_par);
        if(txt.pos>=posend) txt.set(posend);
        else if(Character.isDigit(txt.currentChar())) 
            epoch = txt.parseDecimal();
        else if(time_ok = atime.parse(txt)) 
            epoch = id==4 ? atime.getByr() : atime.getJyr();
	if(has_par || bracketed) txt.gobbleSpaces();
        if(has_par) txt.match(')');
	if(bracketed) { txt.gobbleSpaces(); txt.match(')'); }
        if(!txt.match(':')) txt.match('=');
        //txt.set(posend);

        // If epoch undefined, it could happen that an epoch exists after the position.
        // When such an epoch exists, and indicates a 'possible and simple' epoch [1800..2210], 
        // use it for the frame.
        if(Double.isNaN(epoch) && ((txt.length-txt.pos)>5)) {
            int i = txt.indexOf('J');
            if(i<0) i = txt.indexOf('B');
            if(time_ok = i>0) {
                int ipos = txt.pos; txt.set(i);
                if(DEBUG) System.out.println("#...Astroframe.parse: possible epoch as <" + txt.toString() + ">");
                time_ok = atime.parse(txt); 
                txt.set(ipos);
            }
            if(time_ok) {
                epoch = id==4 ? atime.getByr() : atime.getJyr();
                // Accept only a rather integer epoch
                double epint = 4.*epoch;
                if(DEBUG) System.out.println("#...Astroframe.parse: possible epoch: " + epoch + "; frac="
                        + Math.abs(epint - Math.rint(epint)));
                time_ok = (epoch>=1800)&&(epoch<=2100)
                        &&(Math.abs(epint - Math.rint(epint))<=Jsec);
            }
            if(!time_ok) epoch = 0./0.;
        }
	
        // Retrieve Frame if existing
        //if(DEBUG) System.out.println("#...Astroframe.parse: examine <" + txt + ">; id=" + id);
        if(basic && Double.isNaN(epoch)) 
            frame = (Astroframe)fixedFrames.get(id);
        else switch(IAUframes[id]) {
          case 'I': 
            frame = ICRS.scan(epoch);
            if((frame==null)&&create) 
                frame = (Astroframe) new ICRS(epoch);
            break;
          case 'G':
            frame = Galactic.scan(epoch);
            if((frame==null)&&create)
                frame = (Astroframe) new Galactic(epoch);
            break;
          case 'B':   // FK4, need Besselian
            frame = FK4.scan(equinox, epoch);
            if((frame==null)&&create)
                frame = (Astroframe) new FK4(equinox, epoch);
            break;
          case 'J':   // FK5
            frame = FK5.scan(equinox, epoch);
            if((frame==null)&&create)
                frame = (Astroframe) new FK5(equinox, epoch);
            break;
          case 'S':
            frame = Supergal.scan(epoch);
            if((frame==null)&&create)
                frame = (Astroframe) new Supergal(epoch);
            break;
          case 'E':
            frame = Ecliptic.scan(equinox, epoch);
            if((frame==null)&&create)
                frame = (Astroframe) new Ecliptic(equinox, epoch);
            break;
        }
        // Frame just created has a usage==0
        //System.out.println("#...Astroframe.parse(" + originalText + ") => " + frame.full_name);
        // If an existing frame had to be scanned, add the name specified therein as a synonym
        if((frame!=null) && ((frame.fixed&0x80)!=0)) 
            frame.fixFrame(txt.toString(posini+(bracketed?1:0), end));
        return frame;
    }

    /**
     * Create, or retrieve if already exists, an astroframe from the interpretation of a text.
     * The Astroframe is created <i>only if not yet existing</i>.
     * It is also saved (fixed).
     * @param  name the frame name + equinox + epoch, e.g. 
     *              "ICRS(2015.5)" or "FK4(B1950,Ep=J2000)",
     *         or the named used in the {@link #fixFrame} method.
     * @return the astroframe, null when name could not be interpreted.
     * @see parse
    **/
    public static Astroframe create(String name) {
        // First, does it already exist ?
        Astroframe frame = getFrame(name);
        if(frame!=null) return(frame);
        // Not yet in fixedFrames set: try to interpret
	Parsing text = new Parsing(name);
        frame = parse2(text, true);
        text.gobbleSpaces();
        if((frame==null)||(text.pos<text.length)) {
            System.err.println("#+++Astroframe.create(" + name 
                    + "): stopped with <" + text + ">");
            return(null);
        }
        // Add this name to fixedNames, it will then later be retrieved.
        frame.fixFrame(name);
        return(frame);
    }

    /**
     * Creation of a <i>new</i> Astroframe with identical definitions as 
     * an existing Astroframe, with a possible different epoch.
     * It is a way of cloning an existing astroframe, if the specified epoch is NaN.
     * @param frame the frame of the type we expect
     * @param epoch the epoch we want, always in <b>Julian years</b>.
     * @return the astroframe, null when name could not be interpreted.
    **/
    public static Astroframe create(Astroframe frame, double epoch) {
        if(frame==null) return(null);
        if(Double.isNaN(epoch)) epoch = frame.epoch;
        // Non-standard frame: create it first with the same frame.name
        // to avoid the new frame being fixed. Change its full_name afterward.
        Astroframe new_frame;
        // Non-zero value for "special_frame" indicates non-standard frame
        int special_frame = frame.fixed&0xf;
        if(frame instanceof ICRS) {
            new_frame = (Astroframe)new ICRS(epoch);
        }
        else if(frame instanceof Galactic) {
            new_frame = (Astroframe)new Galactic(epoch);
        }
        else if(frame instanceof Supergal) {
            new_frame = (Astroframe)new Supergal(epoch);
        }
        else if(frame instanceof FK5) {
            FK5 f = (FK5)frame;
            new_frame = (Astroframe)new FK5(f.equinox, epoch);
        }
        else if(frame instanceof FK4) {
            FK4 f = (FK4)frame;
            new_frame = (Astroframe)new FK4(f.equinox, Astrotime.J2B(epoch));
        }
        else if(frame instanceof Ecliptic) {
            Ecliptic f = (Ecliptic)frame;
            new_frame = (Astroframe)new Ecliptic(f.equinox, epoch);
        }
        else return(null);
        if(special_frame!=0) {
            // Non-standard frame: update name, full_name, toICRSmatrix
            new_frame.fixed |= special_frame;
            if(new_frame.epoch != frame.epoch) {
                // Add the epoch in the full_name, replace only in name
                new_frame.name = updateEpoch(new_frame.name, new_frame.epoch, false);
                new_frame.full_name = updateEpoch(new_frame.full_name, new_frame.epoch, true);
            }
        }
        return(new_frame);
    }

    /** (no, created in sub-classes)
     * Creation of a non-standard Astroframe from a name and toICRSmatrix.
     * @param  name the name to assign to this frame, e.g. "Gaia-DR2".
     *         The frame will be fetchable with the {@link #getFrame} method.
     * @param  epoch: the epoch to assign to this frame.
     * @param  toICRSmatrix: the 3x3 or 2x3x3 matrix (rotation + spin) which describes
     *         how this frame is related to the ICRS.
     * @param  IAUtype: specifies 
     * @return the astroframe, null when impossible.
    public static Astroframe create(String name, double epoch, double[] toICRSmatrix, char IAUcompatible) {
        if(DEBUG) System.out.println("#---Astroframe.create(" + name + ", " + epoch + "): non-standard frame.");
        Astroframe frame = (Astroframe) new ICRS(epoch);
        frame.name = frame.full_name = name;
        frame.toICRSmatrix = toICRSmatrix;
        frame.fromICRSmatrix = AstroMath.rot_inv(toICRSmatrix);
        frame.fixed |= 1;	// Non-standard frame indicator
        // Add it, unless it's already known.
        if((fixedNames!=null)&&fixedNames.containsKey(frame.name)) ;
        else frame.fixFrame(name);
        return(frame);
    }
    **/

    /**
     * Verify letter is a valid frame.
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal)
     * @return true/false
    public static final boolean isIAU(char sym) {
        char c = Character.toUpperCase(sym);
        for(int i=0; i<IAUframes.length; i++) {
            if(c==IAUframes[i]) return(true);
        }
        return(false);
    }
    **/

    /**
     * Check an astroframe compatible with char designation (J, B, G, etc)
     * @param  sym the letter defining the frame.
     *         (I=ICRS, J=J2000, B=B1950, E=EclJ2000, G=Gal, S=SuperGal)
     * @return the corresponding astroframe / null
    **/
    public boolean equals(char sym) {
	switch(Character.toUpperCase(sym)) {
            case 'J': 	// Accepts FK5(2000) or ICRS
                if(toICRSmatrix == FK5.toICRSbase) return(true);
                // NO BREAK
	    case 'I': 
                return (this instanceof ICRS);
	    case 'G': 
                // Accept all definitions of Galactic frame
                return (this instanceof Galactic);
	    case 'B': 
                return (toICRSmatrix == FK4.toICRSbase);
	    //case 'S': return(this instanceof Supergal);
            //case 'E': return(this.name.equals("Ecl(J2000)"));
	}
	return(false);
    }

    /**
     * Edition of an Astroframe: name and epoch.
     * The name of the frame without the epoch is frame.name
     * @return the edited Astroframe, 
     *         within quotes (") if the frame is fixed.
    **/
    public String toString() {
        return (fixed&0x80)==0 ? full_name : ('"' + name + '"');
    }

    /**
     * Mark a frame as "permanent". It can then be retrieved, via the supplied name.
     * The computations of change from/to this frame can then be made
     * faster (transformation matrices do not need to be recompûted).
     * @param name the name under which it will be retrieved via {@link #getFrame}
     * @return true (is ok) / false (could not be done)
     */
     public final synchronized boolean fixFrame(String name) {
         if(DEBUG) {
             System.out.print("#...Astroframe.fixFrame(" + name + ") for: " + this + "; fixedFrames=");
             if(fixedFrames==null) System.out.println("0");
             else System.out.println(fixedFrames.size());
         }
         boolean done = false;
         if(name.length()<1)		// Refuse empty string!
             return(done);
         Integer ordi;			// Position in fixedFrames
         if(fixedFrames==null) {
             if(DEBUG) System.out.println("#...installing basic frames...");
             fixedFrames = new ArrayList(15);
             if(fixedNames == null) fixedNames = new HashMap(20);
             // Insert the basic frames, in the order of IAUframes
             fixedFrames.add(new ICRS(2015.5));		// 0
             fixedFrames.add(new ICRS(2000.0));		// 1
             fixedFrames.add(new Galactic(2000.0));	// 2
             fixedFrames.add(new Supergal(2000.0));	// 3
             fixedFrames.add(new FK4(1950.0, 1950.0));	// 4
             fixedFrames.add(new FK5(2000.0, 2000.0));	// 5
             fixedFrames.add(new Ecliptic(2000.,2000));	// 6
             if(DEBUG) System.out.println("#...installed " + fixedFrames.size() + " basic frames...");
             for(int i=0; i<fixedFrames.size(); i++) {
                 Astroframe f = (Astroframe)fixedFrames.get(i);
                 ordi = new Integer(i);
                 if(this.equals(f)) { 
                     if(DEBUG) System.out.println("#...replace basic frame <" + this.full_name + "> by <" + f.full_name + ">");
                     fixedFrames.set(i, f); 	// replace frame
                     f.name = name;
                     fixedNames.put(name, ordi);
                     done=true; 
                 }
                 else f.name = f.full_name;	// This is the name of standard frames.
                 f.fixed |= 0x80; 
                 if(!fixedNames.containsKey(f.full_name))
                     fixedNames.put(f.full_name, ordi);
             }
         }
         else if(fixedNames.containsKey(name)) {
             // Name already used, can't be assigned.
             ordi = (Integer)fixedNames.get(name);
             //Astroframe named_frame = fixedFrames.get(ordi.intValue());
             if(DEBUG) System.out.println("#+++Astroframe.fixFrame: " + name + " already assigned to frame#" + ordi);
             return(fixedFrames.get(ordi.intValue()) == this);
         }
         if((this.fixed&0x80)!=0) {	// Frame already fixed, we add a synonym
             Integer fno = (Integer)fixedNames.get(this.full_name);
             if(DEBUG) System.out.println("#...Astroframe.fixFrame: adding name <" + name + "> to frame#"
                     + (fno==null ? full_name : fno.toString()));
             return fixedNames.put(name, fno)==null;	// put() returns the previous object in HashMap.
         }

         if(done) return(done);
         // Add in ArrayList...
         ordi = new Integer(fixedFrames.size());
         if(fixedFrames.add(this)) {
             this.name = name;
             fixedNames.put(name, ordi);
             this.fixed |= 0x80;
             // The full_name is systematically saved, in addition to the asked name.
             if(!fixedNames.containsKey(full_name))
                 fixedNames.put(full_name, ordi);
         }
         /* if(DEBUG) {
             System.out.println("#===List of fixedFrames has now size=" + fixedFrames.size());
             System.out.print("#..."); 
             Iterator lof = Astroframe.fixedFrames.iterator();
             while(lof.hasNext()) System.out.print(" " + lof.next());
             System.out.print("\n...list of fixedNames:");
             Set fixedNames = Astroframe.fixedNames.keySet();
             Iterator lon = fixedNames.iterator();
             while(lon.hasNext()) {
                 System.out.print(" " + lon.next());
             }
             System.out.println("");
         } */
         return(true);
     }

    /**
     * Mark a frame as "permanent". It can then be retrieved, but its epoch can't be changed.
     * The name is the "full_name".
     * @return true (is ok) / false (could not be done)
     */
     public final boolean fixFrame() {
         return(fixFrame(this.full_name));
     }

    /**
     * The setFrameEpoch method just sets the default epoch.
     * This change may be impossible, a new Astroframe has then to be created.
     * @param epoch epoch of the frame, always in <b>Julian year</b>.
     * @return true (done) or false (impossible)
    **/
    public boolean setFrameEpoch(double epoch) {
        // if(DEBUG) System.out.println("#...Astroframe.setFrameEpoch: " + epoch);
        // Don't care about too small modification (1yr~30Ms):
        // if(Math.abs(this.epoch-epoch)<3.e-8)
        if(this.epoch == epoch) 
            return(true);
        if((fixed&0x80)!=0) {
            /* System.err.println("#+++Astroframe.setFrameEpoch(" + epoch + "): <"
                    + this.full_name + "> has fixed Epoch!"); */
            return(false);
        }
	this.epoch = epoch;
        // Add the epoch in the full_name, replace only in name
        this.name = updateEpoch(this.name, this.epoch, false);
        this.full_name = updateEpoch(this.full_name, this.epoch, true);
        return(true);
    }

     /**
      * Get the epoch
      * @return the frame epoch, always in <b>Julian years.</b>
      */
     public double getEpoch() {
   	  return this.epoch;
     }

     /**
      * Test a frame is fixed 
      * @return the frame epoch, always in <b>Julian years.</b>
      */
     public final boolean isFixed() {
   	  return (fixed&0x80)!=0;
     }

    /** 
     * Equality of frames: same epoch and same transformation matrix
     * @param o Another object
     * @return True if same Astroframe 
     *
    **/
    public boolean equals(Object o) {
	boolean res = false;
	if (o instanceof Astroframe) {
	    Astroframe a = (Astroframe)o;
	    res = this.toICRSmatrix == a.toICRSmatrix
               && (Math.abs(this.epoch-a.epoch)<=Jsec);
	}
	return(res);
    }

    /** 
     * Clone an Astroframe
     * (not accepted?)
     public Object clone() {
        Astroframe c = (Astroframe) super.clone();
        fixed &= ~0x80;
        usage = 0;
        return c;
    }
     * */

    /**
     * Compute the hashcode
     * @return the hascode value
     */
    public int hashCode() {
    	int hcode = name.hashCode();
    	long l = Double.doubleToLongBits(epoch);
    	hcode = hcode * 123 + (int) (l^(l >>> 32));
    	return hcode;
    }

    /** 
     * Getting the matrix to rotate to ICRS system.
     * This method should return null when the change to ICRS can't be done 
     * by a simple rotation.
     * @return The rotation matrix from current frame to ICRS.
    public abstract double[] toICRSmatrix() ;
    **/

    /** 
     * Impossible conversion.
     * @param text text of error message
    **/
    private final void notPossible() {
        System.out.println("#***Astroframe(" + this + ", " + this.getClass() 
                + "): linkage to ICRS is not possible!");
    }

    /** 
     * Conversion to ICRS.
     * This conversion must be installed when <tt>ICRSmatrix</tt> is <tt>null</tt>.
     * @param coo a Coordinate assumed to express a position in my frame.
     * On return, coo contains the corresponding coordinate in the ICRS.
    **/
    public void toICRS(Coo coo) {
	//if(ICRSmatrix==null) notPossible();
        coo.rotate(toICRSmatrix);
    }

    /** 
     * Conversion from ICRS. 
     * This conversion must be installed when <tt>ICRSmatrix</tt> is <tt>null</tt>.
     * @param coo a Coordinate assumed to express a position in ICRS.
     * On return, coo contains the corresponding coordinate in my frame.
    **/
     public void fromICRS(Coo coo) {
     	//if (ICRSmatrix==null) notPossible();
        coo.rotate(fromICRSmatrix);
     }

    /**
     * Compute the toICRSbase and fromICRSbase matrices.
     * This default computation only works for frames which are not spinning;
     * a special implementation is required for FK4/FK5/Ecliptic frames.
    public void setICRSbase() {
	if(ICRSmatrix==null) notPossible();
        double[] M1 = AstroMath.transposed(ICRSmatrix);
        if(ICRS.base_epoch == epoch) {
            toICRSbase = ICRSmatrix;
            fromICRSbase = M1;
        }
        else {	// motion matrix * ICRSmatrix
            // In fact the order in the matrix product does not matter, 
            // a motion matrix commutes with a rotation matrix.
            toICRSbase = AstroMath.motionMatrix(ICRS.base_epoch-epoch, ICRSmatrix);
            fromICRSbase = AstroMath.motionMatrix(M1, epoch-ICRS.base_epoch);
        }
    }
    ---*/

    /** 
     * Conversion to ICRS with derivatives.
     * The code contained here is not valid for different base_epoch's (FK4)
     * @param u6   input 6-vector (phase vector) of position + velocity in frame.
     * @param v6  output 6-vector (phase vector) of position + velocity in ICRS,
     *                  may be the same as u6.
     * 			Velocity in Jyr<sup>-1</sup>
     * 			Note that u6 can restricted be a 3-vector.
     public void toICRS(double[] u6, double[] v6) {
        if(u6.length==3) {  // no proper motion, use ICRS matrix
            double x,y,z;
            x=u6[0]; y=u6[1]; z=u6[2];
            v6[0] = ICRSmatrix[0]*x + ICRSmatrix[1]*y + ICRSmatrix[2]*z;
            v6[1] = ICRSmatrix[3]*x + ICRSmatrix[4]*y + ICRSmatrix[5]*z;
            v6[2] = ICRSmatrix[6]*x + ICRSmatrix[7]*y + ICRSmatrix[8]*z;
            return;
        }
        if(toICRSbase==null) setICRSbase();
	if (DEBUG) System.out.println(Coo.toString(
                "#...Astroframe: " + this + " toICRS:\n", toICRSbase));
        AstroMath.m36v(toICRSbase, u6, v6);
     }
    **/

    /** 
     * Conversion to ICRS with derivatives.
     * @param u6  on input, 6-vector (phase vector) of position + velocity in frame.
     *           on output, 6-vector (phase vector) of position + velocity in ICRS,
     public void toICRS(double[] u6) {
        toICRS(u6, u6);
     }
    **/

    /** 
     * Conversion from ICRS with derivatives.
     * The code contained here is not valid for different base_epoch's (FK4)
     * @param u6   input 6-vector (phase vector) of position + velocity in ICRS
     * @param v6  output 6-vector (phase vector) of position + velocity in frame,
     *                  may be the same as u6.
     * 			Velocity in Jyr<sup>-1</sup>
     * 			Note that u6 can restricted be a 3-vector.
     public void fromICRS(double[] u6, double[] v6) {
        if(u6.length==3) {  // no proper motion, use ICRS matrix
            double x,y,z;
            x=u6[0]; y=u6[1]; z=u6[2];
            v6[0] = ICRSmatrix[0]*x + ICRSmatrix[3]*y + ICRSmatrix[6]*z;
            v6[1] = ICRSmatrix[1]*x + ICRSmatrix[4]*y + ICRSmatrix[7]*z;
            v6[2] = ICRSmatrix[2]*x + ICRSmatrix[5]*y + ICRSmatrix[8]*z;
            return;
        }
        if(fromICRSbase==null) setICRSbase();
	if (DEBUG) System.out.println(Coo.toString(
                "#...Astroframe: ICRS to " + this + "\n", fromICRSbase));
        AstroMath.m36v(fromICRSbase, u6, v6);
     }
    **/

    /** 
     * Conversion from ICRS with derivatives.
     * @param u6  on input, 6-vector (phase vector) of position + velocity in ICRS.
     *           on output, 6-vector (phase vector) of position + velocity in frame.
     public void fromICRS(double[] u6) {
        fromICRS(u6, u6);
     }
    **/

    /** 
     * Save a conversion matrix from <code>this</code> to the frame given in argument.
     * It is generally a 6x6 matrix, but restricts to 3x3 when the epochs are
     * identical and a simple rotation exists between the frames (which does not
     * work for FK4, FK5 or Ecliptic frames).
     * Inversing the frames results in the inverse matrix, i.e.<br>
     * <b>M</b>(<i>f<sub>1</sub>,f<sub>2</sub></i>) &sdot; <b>M</b>(<i>f<sub>2</sub>,f<sub>1</sub></i>) = <b>1</b>
     * @param frame   the destination frame the conversion applies.
     * @param M   the conversion matrix such that <code>coo<sub>frame</sub> = <b>M</b>coo<sub>this</sub></code>
     * @return     true when ok.
     *             "false" can be returned when the specified <i>frame</i> has already a conversion matrix (duplication).
     }
    **/

     public final synchronized boolean saveConversion(Astroframe frame, double[] M) {
         if(DEBUG) System.out.println("#...Astroframe.saveConversion(" + frame + "): associate with " 
                 + M.length + "-double matrix");
         if((this.fixed&0x80)==0)  this.fixFrame();
         if((frame.fixed&0x80)==0) frame.fixFrame();
         if(this.savedConversions == null) savedConversions = new HashMap(7);
         if(this.savedConversions.containsKey(frame)) return(false);
         this.savedConversions.put(frame, M);
         // Mark saved conversions.
         //this.fixed  |= 0x20;
         //frame.fixed |= 0x20;
         return(true);
     }

    /** 
     * Get the matrix conversion from Frame1 to Frame2.
     * It is generally a 6x6 matrix, but restricts to 3x3 when the epochs are
     * identical and a simple rotation exists between the frames (which does not
     * work for FK4, FK5 or Ecliptic frames).
     * Inversing the frames results in the inverse matrix, i.e.<br>
     * <b>M</b>(<i>f<sub>1</sub>,f<sub>2</sub></i>) &sdot; <b>M</b>(<i>f<sub>2</sub>,f<sub>1</sub></i>) = <b>1</b>
     * @param f1   the source frame
     * @param f2   the target frame
     * @return     the matrix <b>T</b> (9 or 36 doubles) from f1 to f2, i.e.<br>
     * 		<code>coo<sub>f2</sub> = <b>T</b> coo<sub>f1</sub></code>
    **/
     public final static double[] convertMatrix(Astroframe f1, Astroframe f2) {
        if(f1.equals(f2)) return(AstroMath.U3matrix);
        boolean fixed = ((f1.fixed & f2.fixed & 0x80)!=0);
        double[] M = null;
        //if(DEBUG) System.out.println("#...convertMatrix(" + f1 + " => " + f2 + "): fixed=0x" + Integer.toHexString((f1.fixed&0xff)));
        if(fixed && (f1.savedConversions!=null)) {
            // This conversion could have been saved:
            M = (double[])f1.savedConversions.get(f2);
            if(M!=null) {
                if(DEBUG) System.out.println("#...Astroframe.convertMatrix(<" + f1 + ">, <" + f2 + ">) retrieved in savedConversions.");
                // Increase statistics
                f1.usage  += 1;
                f2.usage += 1;
                return(M);
            }
        }
        M = AstroMath.motionMatrix(f2.fromICRSmatrix, f2.epoch-f1.epoch, f1.toICRSmatrix);
        if(DEBUG) System.out.println("#...Astroframe.convertMatrix(<" + f1 + ">, <" + f2 + ">) computed, size=" + M.length);
        // It may happen, if epochs are identical, that M=(R,r); but we accept only 3x3 or 6x6 matrices.
        if(M.length==18) M = AstroMath.m6(M);
        if(fixed) f1.saveConversion(f2, M);
        return(M);
        /*
        if(f1.epoch == f2.epoch && f1.toICRSmatrix.length==9 && f2.toICRSmatrix.length==9)
            return(AstroMath.m3p(AstroMath.m3t(f2.ICRSmatrix), f1.ICRSmatrix));
        if(f1.toICRSbase == null)   f1.setICRSbase();
        if(f2.fromICRSbase == null) f2.setICRSbase();
        return(AstroMath.m36p(f2.fromICRSbase, f1.toICRSbase));
        */
     }

    /** 
     * Compute the dotMatrix from toICRSmatrix.
     * It is used to change the epoch in a spinning frame, and is equal to
     * fromICRS * Udot * toICRS  or, if the toICRS matrix is composed of (X,Xdot):
     * <pre>
     * |    X'    0  | = | 0 1 |  | X   0 | = |  X'Xd        1   | = |  Ω   1 |
     * | -X'XdX'  X' | = | 0 0 |  | Xd  X | = | -X'XdX'Xd  -X'Xd | = | -Ω² -Ω |
     * </pre>
     * @param  frame  the frame concerned
    **/
    public final static void compute_dotMatrix(Astroframe frame) {
        frame.max_spin = 0; frame.dotMatrix = null;
        double[] M = frame.toICRSmatrix;
        if(M.length == 9) return;
        double[] dM = new double[36];
        double maxO = 0; // max|Ω|
        int i,j, o;
        // Upper-left, zero upper-right and bottom-right quarters
        for(i=o=0; i<3; i++, o+=3) for(j=0; j<3; j++, o++) {
            dM[o] = M[0+i]*M[9+j] + M[3+i]*M[12+j] + M[6+i]*M[15+j];
            if(dM[o]>maxO) maxO=dM[o];
            dM[3+o] = 0;
            dM[21+o] = -dM[o];
            if(dM[21+o]>maxO) maxO=dM[21+o];
        }
        // Bottom-left quarter is the product Upper-left*bottom-right
        for(i=o=0; i<18; i+=6) for(j=0; j<3; j++) 
            dM[18+i+j] = dM[i+0]*dM[21+j] + dM[i+1]*dM[27+j] + dM[i+2]*dM[33+j];
        // Upper-right quarter: set Ones
        dM[3] = dM[10] = dM[17] = 1.0;
        /*---
        // Compare with direct computation
        if(DEBUG) {
            AstroMath.printMatrix("#...Astroframe.compute_dotMatrix as [ Ω 1 ; -Ω² -Ω ] for " + frame + " (max_spin=" + maxO + "):\n", dM);
            double[] ddM = AstroMath.m36p(AstroMath.m36p(AstroMath.rot_inv(M), Udot), M);
            AstroMath.printMatrix("#...compute_dotMatrix " + frame + " (direct):\n", ddM);
            AstroMath.checkArray("#...Differences:", ddM, dM, eps);
        }
        --*/
        // Install the values
        frame.max_spin = maxO;
        frame.dotMatrix = dM;
        /*
        if(f1.epoch == f2.epoch && f1.toICRSmatrix.length==9 && f2.toICRSmatrix.length==9)
            return(AstroMath.m3p(AstroMath.m3t(f2.ICRSmatrix), f1.ICRSmatrix));
        if(f1.toICRSbase == null)   f1.setICRSbase();
        if(f2.fromICRSbase == null) f2.setICRSbase();
        return(AstroMath.m36p(f2.fromICRSbase, f1.toICRSbase));
        */
     }

    /**
     * Testing the functions of Astroframe.
     * or of a matrix (3x3, 2x3x3 or 6x6).
     * @param   level   a depth level 
     * @param   verbose a verbosity level, number with 0 = less verbose, 1 = more verbose, ...
     * @return  true if ok.
     */
    public static boolean test(int verbose, int level) {
        System.out.println("#===Astroframe.test: verbosity=" + verbose
                            + ", level=" + level + "; ε=" + eps);
        if(verbose>1) DEBUG=true;
        boolean ok=true;
        double[] u = new double[3];
        for(int i=0; i<3; i++) u[i] = 3.14*Math.random();
        double[] u0 = { u[0], u[1], u[2] };
        /* Check Eterm operation */
        int trials=level*1000;
        double eq = Math.rint(1855+200.*Math.random());
        FK4 fk4 = new FK4(eq, eq);
        if(verbose>0) System.out.print("\n#...Testing Eterm for " + fk4 + " (random epoch)");

        for(int trial=0; trial<trials; trial++) {
            if((trial<5)&&(verbose>1)) System.out.println(AstroMath.toString("#...before addEterm", u));
            fk4.addEterm(u);
            if((trial<5)&&(verbose>1)) System.out.println(AstroMath.toString("#....after addEterm", u));
            fk4.subEterm(u);
            if((trial<5)&&(verbose>1)) System.out.println(AstroMath.toString("#....after subEterm", u));
        }
        ok &= AstroMath.checkArray("#...#...Eterm change with " + trials + " trials:", u, u0);
        /*--
        double[] du = { u[0]-u[0], u[1]-u[1], u[2]-u[2] };
        if(verbose>0) {
            System.out.print("\n");
            System.out.print("#...Eterm change with " + trials + " trials: ");
            System.out.print("Δx=" + du[0] + " Δy=" + du[1] + " Δz=" + du[2]);
            System.out.print("\n");
        }
        if((Math.abs(du[0])>=4.0*eps)||(Math.abs(du[0])>=4.0*eps)||(Math.abs(du[0])>=4.0*eps)) {
            System.out.print(" **Eterm=" + (Math.sqrt(du[0]+du[0]+du[1]+du[1]+du[2]+du[2])/eps) + "**");
            ok = false;
        }
        --*/
        // Compute FK4 -> ICRS rotation+spin
        double[] FK4toICRS = AstroMath.m36p(FK5.toICRSbase, FK4.X0);
        if(verbose>0) {
            System.out.print("\n#---Recompute FK4 to ICRS rotation+spin:\n");
            AstroMath.printMatrix("#...FK4.toICRSmatrix ........\n", FK4.toICRSbase);
            AstroMath.printMatrix("#...FK5.toICRSmatrix * FK4.X0\n", FK4toICRS);
            AstroMath.checkArray("#--Differences:", FK4toICRS, FK4.toICRSbase);
        }
        else ok &= AstroMath.diffArray(FK4toICRS, FK4.toICRSbase) > 8.0*eps;
        if(verbose>0) {
            // Compute PM4I
            double[] rot;
            //AstroMath.printMatrix("#---6x6 matrix which gives coordinates in ICRS(J2000) from FK4(B1950):\n", FK4.PM4I);
            rot = AstroMath.motionMatrix(FK4.TJ, FK4.toICRSbase);
            AstroMath.printMatrix("#...6x6 matrix which gives coordinates in ICRS from FK4, recomputed:\n", rot);
            //ok &= AstroMath.checkArray("#...PM4I:", rot, FK4.PM4I);
            // Verify PMI4
            //AstroMath.printMatrix("#---6x6 matrix which gives coordinates in FK4(B1950) from ICRS(J2000):\n", FK4.PMI4);
            rot = AstroMath.motionMatrix(FK4.fromICRSbase, -FK4.TJ, AstroMath.U3matrix);
            AstroMath.printMatrix("#---6x6 matrix which gives coordinates in FK4 from ICRS, recomputed:\n", rot);
            //ok &= AstroMath.checkArray("#...PM4I:", rot, FK4.PMI4);
            // Verify Galactic
            rot = AstroMath.m3p(FK4.toICRSbase, AstroMath.rotation("zyz", 192.25, 90-27.4, 90-33.0));
            ok &= AstroMath.checkArray("#...GalToICRS:", rot, Galactic.toICRSbase);
            //rot = AstroMath.m3p(Galactic.fromFK4matrix, FK4.fromICRSbase);
            ok &= AstroMath.checkArray("#...ICRStoGal:", rot, Galactic.fromICRSbase);
            // Verify Supergal.
            rot = AstroMath.rotation("zx",137.37, 90-6.32);
            ok &= AstroMath.checkArray("#...toGalMatrix:", Supergal.toGalMatrix, rot);
            rot = AstroMath.m3p(Supergal.fromGalMatrix, Galactic.fromICRSbase);
            ok &= AstroMath.checkArray("#...ICRStoSGal:", rot, Supergal.fromICRSbase);
            rot = AstroMath.m3p(Galactic.toICRSbase, Supergal.toGalMatrix);
            ok &= AstroMath.checkArray("#...SGalToICRS:", rot, Supergal.toICRSbase);
            System.out.println("#---Trying to get Frames: Galactic.FK4(), then Galactic.Gaia2()");
            //Astroframe gFK4 = Galactic.FK4(); gFK4.dump("#...Galactic.FK4 (R)");
            //AstroMath.checkArray("--Diff. with toICRSbase:", gFK4.toICRSmatrix, Galactic.toICRSbase);
            Astroframe gGaia = Galactic.Gaia2(); gGaia.dump("#...Galactic.Gaia (R)");
            AstroMath.checkArray("--Diff. with toICRSbase:", gGaia.toICRSmatrix, Galactic.toICRSbase);
        }
        // Testing the matrices for changing frames
        Astroframe gaga = Galactic.Gaia2();
        Astroframe[] frame = { 
            create("ICRS(2000)"), 
            create("ICRS(2015.5)"), 
            create("J2000"), 
            create("FK5(B1950)"), 
            create("B1950"), 
            create("B1875"), 
            create("Gal(B1950)"),
            create("SGal(B1950)"),
            create("Ecl(2015.5"),
            Hipparcos(),
            gaga,
            create("Bad.12") };
        double[] M1, M2;
        int nf = frame.length;
        for(int i=0; i<nf; i++) System.out.println("#...Frame#" + i + ":\t" + (frame[i]==null ? "null" : frame[i].toString()));
        nf--;
        if(frame[nf]!=null) { ok=false; frame[nf].dump("#***should be null??"); }
        // Create a duplicate frame
        Astroframe gaga2 = Astroframe.create(gaga, 2000.);
        if(verbose>0) {
            gaga.dump ("#...gaga (GalacticGaia):");
            gaga2.dump("#...gaga2 (cloned):");
            ok &= gaga2.fixFrame("gaga2k"); gaga2.dump("#...gaga2 (fixed):");
        }
        if(verbose>0) for(int i=0; i<nf; i++) {
            System.out.println("\n#...Frame#" + i + ": " + frame[i]);
            AstroMath.printMatrix("#.....toICRSmatrix:\n", frame[i].toICRSmatrix);
            AstroMath.printMatrix("#...fromICRSmatrix:\n", frame[i].fromICRSmatrix);
            double[] product = AstroMath.m36p(frame[i].fromICRSmatrix, frame[i].toICRSmatrix);
            AstroMath.checkUnity("#.....Product=.....", product);
        }
        for(int i=0; i<nf; i++) for(int j=i; j<nf; j++) {
            System.out.println("#...fixedFrames[1]=" + fixedFrames.get(1));
            if(verbose>0) System.out.println("\n#---Conversion " + frame[i] + " <=> " + frame[j]);
            M1 = convertMatrix(frame[i], frame[j]);
            M2 = convertMatrix(frame[j], frame[i]);
            if(verbose>0) {
                AstroMath.printMatrix("#=>", M1);
                AstroMath.printMatrix("#<=", M2);
                AstroMath.printMatrix("#==", AstroMath.m36p(M1, M2));
            }
            ok &= AstroMath.checkUnity("#...Diff [1]:", AstroMath.m36p(M1, M2));
            if(verbose>0) System.out.println("#===for " + frame[i] + " <=> " + frame[j] 
                    + ", Δt=" + (frame[j].epoch-frame[i].epoch) + "Jyr.");
        }

        // Make a complete tour 
            System.out.println("#...fixedFrames[1]=" + fixedFrames.get(1));
        System.out.println("\n\n#===Tour between all " + nf + " frames===");
        M1 = convertMatrix(frame[1], frame[0]);
        M2 = convertMatrix(frame[nf-1], frame[0]);
        for(int i=2; i<nf; i++) {
            M1 = AstroMath.m36p(M1, convertMatrix(frame[i], frame[i-1]));
            if(verbose>0) {
                AstroMath.printMatrix("#...Conversion " + frame[i] + " => " + frame[0] + "\n", M1);
                AstroMath.checkArray("#...compare with direct conversion:", M1, convertMatrix(frame[i], frame[0]));
            }
        }

        // List all saved frames & conversion matrices
        //Set fixedFrames = Astroframe.fixedFrames.keySet();
        dumpAll("\n#===List of All Saved Frames (R):");
        /*---
        Iterator lof = Astroframe.fixedFrames.iterator();
        int count = 0;
            System.out.println("#...fixedFrames[1]=" + fixedFrames.get(1));
        System.out.println("\n#===List of All Saved Frames:");
        while(lof.hasNext()) {
            Astroframe f = (Astroframe)lof.next();
            System.out.print("#" + ++count + "\t" + f + ", usage=" + f.usage);
            if(f.savedConversions != null) {
                System.out.print("; " + f.savedConversions.size() + " saved conversions.");
                Set soc = f.savedConversions.keySet();
                Iterator loc = soc.iterator();
                for(int ic=0; loc.hasNext(); ic++) {
                    Astroframe o = (Astroframe)loc.next();
                    System.out.print("\n#\t => " + o.full_name + " <" + o.getClass() + ">");
                    double[] m = (double[])f.savedConversions.get(o);
                    if(m!=null) System.out.print(": matrix size=" + m.length);
                }
            }
            System.out.println();
        }
        --*/

        if(verbose==0) System.out.println(ok? " (ok)" : " **problem(s)**");
        else if(ok) System.out.println("#---End of tests: ok");
        else        System.out.println("#***Bad ** tests?");
        return(ok);
    }
}
