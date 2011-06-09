// Copyright 2010 - UDS/CNRS
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

package cds.aladin;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import cds.astro.Unit;
import cds.image.EPSGraphics;
import cds.tools.Util;
import cds.tools.parser.Parser;
import cds.tools.parser.ParserException;

/**
 * Gestion des actions associees a chaque bloc de contraintes
 *
 * @author Thomas Boch [CDS]
 * @version 0.96 : Fevrier 2005 - ajout d'une fonction draw rectangle
 * @version 0.95 : Janvier 2005 - correction bug multi-vues pour ellipses et proper motion
 * @version 0.91 : Septembre 2002 - nettoyage du code, ajouts de fonctionalites
 * @version 0.9 : Juillet 2002 - Creation
 */

public class Action {

    boolean badSyntax=false;


	// color names
	static final String[] COLORNAME = { "black", "blue" ,"cyan", "darkGray",
	"gray", "green", "lightGray", "magenta", "orange", "pink", "red", "white", "yellow" };

	// corresponding colors
	static final Color[] MYCOLORS = { Color.black, Color.blue, Color.cyan,
	Color.darkGray, Color.gray, Color.green, Color.lightGray, Color.magenta,
	Color.orange, Color.pink, Color.red, Color.white, Color.yellow};

	// different available functions
	protected static final String DRAWOBJECT = "drawobject";
	protected static final String DRAWSTRING = "drawstring";
    protected static final String HIDE = "hide";

    // shape with parameters
    protected static final String SIZE = "circle";
    protected static final String FILLSIZE = "fillcircle";
    protected static final String FIXEDCIRCLE = "fixedcircle"; // pour Bruno Rino (hack-a-thon 08/03/2006)
    protected static final String ELLIPSE = "ellipse";
    protected static final String FILLELLIPSE = "fillellipse";
    protected static final String RECTANGLE = "rectangle";
    protected static final String PM = "pm";
    protected static final String LINE = "line";

    // TODO : encapsulate in color-function objects
	// a function to fully define a color
	protected static final String RGB = "rgb";
	// a function to define a color in the rainbow spectrum
	protected static final String RAINBOW = "rainbow";
	// a function to define a color varying with the saturation (HSV model)
	protected static final String SATURATION = "saturation";

    // geometric shapes (no parameters)
	protected static final String OVAL = "oval";
    protected static final String CARRE = "square";
    protected static final String CROIX = "cross";
    protected static final String PLUS = "plus";
    protected static final String LOSANGE = "rhomb";
    protected static final String TRIANGLE = "triangle";
    protected static final String POINT = "dot";
    protected static final String DOT = "microdot";

    // ensemble des formes sans paramètre
    protected static final String[] NOPARAMSHAPE = {OVAL,CARRE,CROIX,PLUS,LOSANGE,TRIANGLE,POINT,DOT};

    // just draw the source as usual
    protected static final String DRAW = "draw";

    protected static final String COLOR = "color=";
    protected static final String SHAPE = "shape=";

    // parsers used for the ELLIPSE action
    Parser majAxisParser, minAxisParser, posAngleParser;

    // parsers used for the PM (proper motion) ACTION
    Parser pmDecParser, pmRAParser;

    Color color = null; 	// color of selected sources
    Parser sizeParser; 	// parser used to calculate the radius of each source

    //  default values for max/min size of radius (actions SIZE and FILLSIZE and FIXEDCIRCLE) --> in arcsec
    private int maxRadius = 30;
    private int minRadius = 3;

	// parsers used for the RGB function
	Parser redParser, greenParser, blueParser;
	// parser used for the RAINBOW function
	Parser rainbowParser;
	// parser used for the SATURATION function
	Parser saturationParser;
	// parsers used for the LINE function
	Parser lineRa1Parser, lineDec1Parser, lineRa2Parser, lineDec2Parser;

	boolean colorIsVariable = false; // true if the RGB color of this action is variable
	boolean userDefinedMinMax = false; // true if min and max values were given by the user (4th and 5th optional parameters of circle)
	boolean rainbowColorIsVariable = false; // true if the RAINBOW color of this action is variable
	boolean saturationColorIsVariable = false; // true if the SATURATION color of this action is variable
    boolean mustComputeMinMax = true; // true if min/max value is not set yet
	boolean mustComputeRGBMinMax = true; // true if RGB values of color were not computed yet
	boolean mustComputeRainbowMinMax = true; // true if RAINBOW values not computed yet
	boolean userDefinedRainbowMinMax = false;// true if rainbow min and max values were given by the user (optional parameters)
	boolean mustComputeSaturationMinMax = true; // true if SATURATION values not computed yet
    boolean userDefinedSaturationMinMax = false;// true if saturation min and max values were given by the user (optional parameters)

    double minValue,maxValue; // max/min value of sizeParser in the set of sources
	double redMinValue, redMaxValue, blueMinValue, blueMaxValue, greenMinValue, greenMaxValue; // max/min value of R, G, B parsers
	double rainbowMinValue, rainbowMaxValue; // min/max value of rainbowParser
    double saturationMinValue, saturationMaxValue; // min/max value of saturationParser

    private String shape = DRAW;   // name of the shape - default : DRAW
	private String function = DRAWOBJECT;	// function used in this action

	// chaine representant la chaine à afficher (peut-etre un UCD ou une colonne)
	String textToDisp;
	// parser représentant l'expression à afficher à l'écran
	Parser parserToDisp = null;
	// largeur et hauteur et du texte
	int wTexte, hTexte;
	// fonte du texte
	// TODO : les '*' sont mal rendus (pb de fonte), faut il passer à une fonte PLAIN ?
	static private Font FONT = Aladin.SBOLD;//new Font("SansSerif",Font.PLAIN,  Aladin.SIZE-1);
	static private int FONT_SIZE = Aladin.SSIZE;//Aladin.SIZE-1;

    // Reference to Aladin
    Aladin a;
    // Reference to the PlanFilter
    PlanFilter pf;

   String ERR1,ERR2,ERR3,ERR4,ERR5,ERR6,ERR7,ERR8,ERR9,ERR10,ERR11,
          ERR12,ERR13,ERR14,ERR15,ERR16,ERR17,ERR19,ERR20,ERR21,ERR22,
          ERR23,ERR24;

   protected void createChaine() {
      ERR1 = a.chaine.getString("ACERR1");
      ERR2 = a.chaine.getString("ACERR2");
      ERR3 = a.chaine.getString("ACERR3");
      ERR4 = a.chaine.getString("ACERR4");
      ERR5 = a.chaine.getString("ACERR5");
      ERR6 = a.chaine.getString("ACERR6");
      ERR7 = a.chaine.getString("ACERR7");
      ERR8 = a.chaine.getString("ACERR8");
      ERR9 = a.chaine.getString("ACERR9");
      ERR10 = a.chaine.getString("ACERR10");
      ERR11 = a.chaine.getString("ACERR11");
      ERR12 = a.chaine.getString("ACERR12");
      ERR13 = a.chaine.getString("ACERR13");
      ERR14 = a.chaine.getString("ACERR14");
      ERR15 = a.chaine.getString("ACERR15");
      ERR16 = a.chaine.getString("ACERR16");
      ERR17 = a.chaine.getString("ACERR17");
      ERR19 = a.chaine.getString("ACERR19");
      ERR20 = a.chaine.getString("ACERR20");
      ERR21 = a.chaine.getString("ACERR21");
      ERR22 = a.chaine.getString("ACERR22");
      ERR23 = a.chaine.getString("ACERR23");
      ERR24 = a.chaine.getString("ACERR24");
   }

    // constructor with the name of the action and constraints string
    Action(String actionStr, Aladin a, PlanFilter pf) {
    	this.a = a;
    	createChaine();
        this.pf = pf;
    	decodeAction(actionStr);
    }

    protected void action(Source s, Graphics g, ViewSimple v, Point p, int numero, int index, int dx, int dy) {
    	action(s,g,v,p,numero,index,true,dx,dy);
    }

    /** performs an action
     *	@param s : the source which the action is performed on
     *	@param g
     *  @param v
     *	@param p : position(x,y) of the source
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     *  @param dx - decalage en x
     *  @param dy - decalage en y
     */
    protected void action(Source s, Graphics g, ViewSimple v, Point p, int numero, int index, boolean setColor, int dx, int dy) {
    	if( setColor ) setColor(s);

    	Color theColor = color;
		// if the color is not set, set it to the color of the plan of the source
    	if(theColor==null) {theColor = s.plan.c;}
//    	System.out.println("theColor : "+theColor+" pour source "+s);
		if(function.equals(HIDE)) {
			// do nothing
			return;
		}

		else if( function.equals(DRAWOBJECT) ) {
    		if(shape.equals(DRAW)) {
    	    	s.doDraw(g,p,theColor);
				return;
      		}


            else if(shape.equals(OVAL)) {
               g.setColor(theColor);
               s.drawOval(g,p);
               return;
            }

      		else if(shape.equals(CARRE)) {
      	    	g.setColor(theColor);
      	    	s.drawCarre(g,p);
				return;
      		}

      		else if(shape.equals(CROIX)) {
      	    	g.setColor(theColor);
      	    	s.drawCroix(g,p);
				return;
      		}

      		else if(shape.equals(PLUS)) {
      	    	g.setColor(theColor);
      	    	s.drawPlus(g,p);
				return;
      		}

            else if(shape.equals(LOSANGE)) {
                g.setColor(theColor);
                s.drawLosange(g,p);
                return;
            }

            else if(shape.equals(TRIANGLE)) {
                g.setColor(theColor);
                s.drawTriangle(g,p);
                return;
            }

      		else if(shape.equals(POINT)) {
      	    	g.setColor(theColor);
      	    	s.drawPoint(g,p);
				return;
      		}

      		else if(shape.equals(DOT)) {
      	    	g.setColor(theColor);
      	    	s.drawPoint(g,p);
				return;
      		}

      		else if(shape.equals(ELLIPSE) || shape.equals(FILLELLIPSE)) {
      	    	drawEllipse(s,g,v,p,theColor,numero,index,dx,dy,shape.equals(FILLELLIPSE));
				return;
      		}

      		else if(shape.equals(RECTANGLE)) {
      	    	drawRectangle(s,g,v,p,theColor,numero,index,dx,dy);
				return;
      		}

      		else if(shape.equals(PM)) {
      	    	drawPM(s,g,v,p,theColor, numero, index, dx, dy);
				return;
      		}

      		else if(shape.equals(LINE)) {
      		    drawLine(s,g,v,p,theColor,numero,index,dx,dy);
      		}

      		else if(shape.equals(SIZE)||shape.equals(FILLSIZE)||shape.equals(FIXEDCIRCLE)) {
      	    	// rien à faire si la valeur est de -1
      	    	if( s.values[numero][index][0] == -1 ) return;
      	    	int intValue;

      	    	g.setColor(theColor);
      	    	if( shape.equals(FIXEDCIRCLE) ) intValue = (int)(s.values[numero][index][0]);
                else {
                    double scaleFactor = v.getProj().r/(60*v.getProj().rm)*v.getZoom();
                    intValue = (int)(s.values[numero][index][0]*scaleFactor*s.plan.getScalingFactor());
                    if( intValue<3 ) {
                        s.drawPoint(g, p);
                        return;
                    }
                }


    	    	if( shape.equals(SIZE) ) {
    	    	    g.drawOval(p.x-intValue/2,p.y-intValue/2,intValue,intValue);
    	    	}
				else if( shape.equals(FILLSIZE) && Aladin.ENABLE_FOOTPRINT_OPACITY && g instanceof Graphics2D ) {
				    Graphics2D g2d = (Graphics2D)g;
				    Composite saveComposite = g2d.getComposite();
				    float opacityLevel = Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL*s.plan.getOpacityLevel();
			        Composite myComposite = Util.getImageComposite(opacityLevel);
			        g2d.setComposite(myComposite);
			        g2d.fillOval(p.x-intValue/2,p.y-intValue/2,intValue,intValue);
			        g2d.setComposite(saveComposite);
				}
				else {
				    g.fillOval(p.x-intValue/2,p.y-intValue/2,intValue,intValue);
				}
				return;
      		}

      		// default action for DRAWOBJECT : do nothing special, ie draws the source as usual
      		else {
      	    	s.doDraw(g,p,null);
				return;
      		}
		}

		else if( function.equals(DRAWSTRING) ) {
			String texte;
			// the text is variable
			if(	textToDisp!=null && ( textToDisp.startsWith("[") || textToDisp.startsWith("{") ) ) {
				int pos;

				// case of a UCD
				if( textToDisp.startsWith("[") ) {
					if( (pos=s.findUCD(textToDisp.substring(1,textToDisp.length()-1).toUpperCase())) < 0 ) {return;}
					texte = s.getValue(pos);
				}

				// case of a column
				else {
					if( (pos=s.findColumn(textToDisp.substring(1,textToDisp.length()-1))) < 0 ) {return;}
					texte = s.getValue(pos);
				}

                if( texte==null ) texte = "";

			}

			// the text is an arithmetical expression
			else if( parserToDisp!=null ) {
				if( !setAllVariables(parserToDisp,s,false) ) return;
				else texte = new Double(parserToDisp.eval()).toString();
				// we keep only 4 significant digits
				int sepIndex = texte.indexOf(".");
				if( sepIndex>=0 ) {
					int expIndex = texte.indexOf("E");
					// on cherche le minimum pour savoir ou s'arreter
					int end = texte.length()>sepIndex+5?sepIndex+5:texte.length();
					if( expIndex>=0 ) end = expIndex>end?end:expIndex;
					String saveText = new String(texte);
					texte = texte.substring(0, end);
					if( expIndex>=0 ) texte += saveText.substring(expIndex);
				}

			}

			// the text is constant
			else {

				int left,right;
				if( (left=textToDisp.indexOf("\""))>=0 && (right=textToDisp.lastIndexOf("\""))>=0 && left!=right ) {
					texte = textToDisp.substring(left+1,right);
				}
				else texte = textToDisp;
			}

			FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(FONT);
      		wTexte = m.stringWidth(texte)/2;
      		hTexte = FONT_SIZE/2;

			g.setFont(FONT);
			g.setColor(theColor);
			g.drawString(texte,p.x-wTexte,p.y+hTexte);
            return;
		}


    }

    /** idem que la méthode action, mais utilisée pour l'overlay dans une couleur inversée
     * n'est disponible pour le moment que pour l'action RECTANGLE (en test quoi !)
     * @param s
     * @param g
     * @param v
     * @param p
     * @param numero
     * @param index

    protected void drawReverse(Source s, Graphics g, ViewSimple v, Point p, int numero, int index, boolean goOn, int dx, int dy) {
    	if( !shape.equals(RECTANGLE) ) {
    		if( goOn ) action(s,g,v,p,numero,index,dx,dy);
    		return;
    	}

    	setColor(s);
    	Color save = color;
    	color = Util.getReverseColor(color==null?s.plan.c:color);
    	action(s,g,v,p,numero,index,false,dx,dy);
    	// restore the original color
    	color = save;
    }
    */

    // decode the action we have to perform on selected/unselected sources
    private void decodeAction(String s) {
        // we save the original value of s
        // we will use it to decode the new syntax
        String os = new String(s);
		s = UCDFilter.skipSpaces(s);

		while(s.charAt(0)=='\n') {
			s = s.substring(1);
		}

		int beginParam = s.indexOf("(");
		int endParam;
		String paramStr = "";

		if( beginParam < 0 ) {beginParam=s.length();}
		else {

			endParam = getClosingParenthesis(s, beginParam+1);
			if( endParam < 0 ) {
				badSyntax = true;
				Aladin.warning(ERR1,1);
				return;
			}
			paramStr = s.substring(beginParam+1, endParam);
		}

		String myFunction = s.substring(0,beginParam);
		//System.out.println(myFunction);
		//System.out.println(s.substring(beginParam+1, endParam));

        // -- Ancienne syntaxe --

		if( myFunction.equals(DRAWOBJECT) ) {
			function = DRAWOBJECT;
			decodeDrawObject(paramStr);
            return;
		}

		else if( myFunction.equals(DRAWSTRING) ) {
			function = DRAWSTRING;
			decodeDrawString(paramStr);
            return;
		}

		else if( myFunction.equals(HIDE) ) {
			function = HIDE;
            return;
		}

        // -- Nouvelle syntaxe -- (plus simple)
        // même plus besoin du draw
        //if( myFunction.startsWith(DRAW) ) {
        if(true) {
            int beginDraw = os.indexOf(DRAW);
            if( beginDraw>=0 ) os = os.substring(beginDraw+DRAW.length());
            os = os.trim();
            //System.out.println(os);
            // On appelle processFunction 2 fois : pour couleur et forme (ou inversement)
            int suite = processFunction(os);
            if( suite>=0 && suite<os.length() ) processFunction(os.substring(suite).trim());
            //System.out.println(os);
        }
    }

    private void setColor(Source s) {
		if( colorIsVariable ) {
			color = computeColor(s);
		}

		else if( rainbowColorIsVariable ) {
			color = computeRainbowColor(s);
		}

		else if( saturationColorIsVariable ) {
		    color = computeSaturationColor(s);
		}
    }

    /**
     * traite la premiere fonction trouvee dans la chaine s et retourne l'index de fin de cette fonction, -1 si probleme
     * PREcondition : trim a été appliqué sur s
     */
    private int processFunction(String s) {
        // si fonction draw toute bete
        if(s.length()==0) return -1;

        // **** Si il s'agit d'une chaine ****
        if( s.startsWith("\"") ) {
            int index = s.indexOf("\"", 1);
            if( index<0 ) {
                badSyntax = true;
				Aladin.warning(ERR2,1);
				return -1;
            }
            else {
                function = DRAWSTRING;
                textToDisp = s.substring(1,index);
                return index+1;
            }
        }

        // **** Chaine avec parametre ****
        if( s.startsWith("[") || s.startsWith("{") ) {
            int end = s.startsWith("[")?s.indexOf("]"):s.indexOf("}");
            if( end<0 ) {
                badSyntax = true;
				Aladin.warning(ERR3,1);
                return -1;
            }
            else {
                function = DRAWSTRING;
                textToDisp = s.substring(0,end+1);
                return end+1;
            }
        }

        // **** On cherche un blanc ****
        int b1,b2, before;
        b1 = s.indexOf(" ");
        b2 = s.indexOf("(");
        if( b1>=0 && b2>=0 ) before = Math.min( s.indexOf(" "), s.indexOf("(") );
        if( b1<0 ) before = b2;
        else before = b1;

        if( before<0 ) before = s.length();
        String name = s.substring(0,before);


        int pos;
		if ( (pos=findColorName(name))>=0 ) {
			this.color = MYCOLORS[pos];
			return name.length();
		}
		else if( s.startsWith(RGB) ) {
            before = s.indexOf("(");
            if( before<0 ) {
                badSyntax = true;
				Aladin.warning(ERR4,1);
				return -1;
            }
            int end = getClosingParenthesis(s,before+1);
            if( end < 0 ) {
                badSyntax = true;
				Aladin.warning(ERR5,1);
				return -1;
            }
			decodeRGB(s.substring(Math.max(0,before), end+1));
			return end+1;
		}
		else if( s.startsWith(RAINBOW) ) {
            before = s.indexOf("(");
            if( before<0 ) {
                badSyntax = true;
				Aladin.warning(ERR6,1);
				return -1;
            }
            int end = getClosingParenthesis(s,before+1);
            if( end < 0 ) {
                badSyntax = true;
				Aladin.warning(ERR7,1);
				return -1;
            }
			decodeRainbow(s.substring(Math.max(0,before), end+1));
			return end+1;
		}
        else if( s.startsWith(SATURATION) ) {
            before = s.indexOf("(");
            if( before<0 ) {
                badSyntax = true;
                Aladin.warning(ERR6,1);
                return -1;
            }
            int end = getClosingParenthesis(s,before+1);
            if( end < 0 ) {
                badSyntax = true;
                Aladin.warning(ERR7,1);
                return -1;
            }
            decodeSaturation(s.substring(Math.max(0,before), end+1));
            return end+1;
        }

		else if( name.startsWith("#") ) {
		    this.color = decodeColorString(s.substring(0,before));
            return name.length();
        }

        // les shapes
        // on fera plus malin plus tard

        // shapes basiques
        else if( name.equals(OVAL) || name.equals(CARRE) || name.equals(CROIX) || name.equals(PLUS)
              || name.equals(LOSANGE) || name.equals(TRIANGLE) || name.equals(POINT) || name.equals(DOT) ) {
            this.shape = name;
			function = DRAWOBJECT;
            return name.length();
        }

        // shapes avec parametres
        else if( s.startsWith(SIZE) || s.startsWith(FILLSIZE) || s.startsWith(FIXEDCIRCLE)
                 || s.startsWith(ELLIPSE) || s.startsWith(FILLELLIPSE)
        	     || s.startsWith(PM) || s.startsWith(RECTANGLE) || s.startsWith(LINE) ) {

			function = DRAWOBJECT;
            before = s.indexOf("(");
            if( before<0 ) {
                badSyntax = true;
				Aladin.warning(ERR8,1);
				return -1;
            }
            int end = getClosingParenthesis(s,before+1);
            if( end < 0 ) {
                badSyntax = true;
				Aladin.warning(ERR9,1);
				return -1;
            }
            //System.out.println(s.substring(0, end+1));
			decodeShape(SHAPE+s.substring(0, end+1));
			return end+1;
        }

		// à ce stade, on a épuisé toutes les fonctions disponibles !
		// on essaye de parser la chaine comme une expression mathématique dont on afficherait le résultat
		else {
			try {
			parserToDisp = UCDFilter.createParser(s, a);
			function = DRAWSTRING;
			return -1;
		}
			catch( ParserException e) {}
		}

		//if( setAllVariables() )


        badSyntax = true;
		Aladin.warning(ERR10 + "["+s+"]",1);
        return -1;
    }

    /** Decode la couleur passée en paramètre qui peut être soit une chaine prédéfinie,
     * soit une fonction rgb(r,g,b) soit une valeur #RRGGBB
     * @param s
     * @return la couleur correspondante ou null si problème
     */
    static protected Color getColor(String s) {
		int pos;
		if ( (pos=findColorName(s))>=0 ) return MYCOLORS[pos];
		else if( s.startsWith(RGB) ) return decodeStaticRGB(s);
		else return decodeColorString(s);
    }

	/** decode une couleur pouvant etre une couleur predefinie ou une fonction rgb()
	 *
	 *
	 */
	private void processColor(String s) {
		int pos;
		if ( (pos=findColorName(s))>=0 ) {
			this.color = MYCOLORS[pos];
			return;
		}
		else if( s.startsWith(RGB) ) {
			decodeRGB(s);
			return;
		}
		else if( s.startsWith(RAINBOW) ) {
			decodeRainbow(s);
			return;
		}
		else if( s.startsWith(SATURATION) ) {
		    decodeSaturation(s);
		    return;
		}

		else this.color = decodeColorString(s);
	}

	/** decode une fonction saturation(). Set saturationColorIsVariable a true si besoin est
     *  La fonction peut avoir 1 ou 3 parametres.
     *  Le premier parametre represente l'expression correspondant au saturationParser
     *  Les 2 parametres optionnelles sont les valeurs min et max (si elles ne sont pas precisees, on scannera l'ensemble des valeurs pour retrouver min et max)
     *  @param s - chaine a decoder
     */
    private void decodeSaturation(String s) {
        String paramStr = s.substring(s.indexOf("(")+1, Math.max(s.lastIndexOf(")"),0));

        StringTokenizer st = new StringTokenizer(paramStr,",");

        // there can be 1 or 3 parameters
        if( st.countTokens()!=1 && st.countTokens()!=3 ) {
            badSyntax = true;
            Aladin.warning(ERR11+" " + SATURATION + " rainbow(exp[,minValue,maxValue])",1);
            return;
        }

        try {
            saturationParser = UCDFilter.createParser(st.nextToken(),a);
        }
        catch(ParserException e) {Aladin.warning(ERR12,1);badSyntax=true;return;}

        // if value is constant
        if( saturationParser.isConstant() ) {
            color = Color.getHSBColor(getHue((float)saturationParser.eval()),1f,1f);
        }
        else saturationColorIsVariable = true;

        // in case min and max values were given
        if( st.hasMoreTokens() ) {
            try {
                saturationMinValue = Double.valueOf(st.nextToken()).doubleValue();
                saturationMaxValue = Double.valueOf(st.nextToken()).doubleValue();
            }
            catch(NumberFormatException e) {
                Aladin.warning(ERR13,1);
                badSyntax=true;
                return;
            }

            // min and max are already fixed, set mustCompute to false
            mustComputeSaturationMinMax = false;
            userDefinedSaturationMinMax = true;
        }
    }

	/** decode une fonction rainbow(). Set rainbowColorIsVariable a true si besoin est
	 *	La fonction peut avoir 1 ou 3 parametres.
	 *	Le premier parametre represente l'expression correspondant au rainbowParser
	 *	Les 2 parametres optionnelles sont les valeurs min et max (si elles ne sont pas precisees, on scannera l'ensemble des valeurs pour retrouver min et max)
	 *	@param s - chaine a decoder
	 */
	private void decodeRainbow(String s) {
		String paramStr = s.substring(s.indexOf("(")+1, Math.max(s.lastIndexOf(")"),0));

		StringTokenizer st = new StringTokenizer(paramStr,",");

		// there can be 1 or 3 parameters
		if( st.countTokens()!=1 && st.countTokens()!=3 ) {
			badSyntax = true;
			//System.out.println("Incorrect syntax for "+RAINBOW+" function. Syntax is "+RAINBOW+" rainbow(exp[,minValue,maxValue])");
			Aladin.warning(ERR11+" " + RAINBOW + " rainbow(exp[,minValue,maxValue])",1);
			return;
		}

    	try {
			rainbowParser = UCDFilter.createParser(st.nextToken(),a);
		}
    	catch(ParserException e) {Aladin.warning(ERR12,1);badSyntax=true;return;}

		// if value is constant
		if( rainbowParser.isConstant() ) {
			color = Color.getHSBColor(getHue((float)rainbowParser.eval()),1f,1f);
		}
		else rainbowColorIsVariable = true;

		// in case min and max values were given
		if( st.hasMoreTokens() ) {
			try {
				rainbowMinValue = Double.valueOf(st.nextToken()).doubleValue();
				rainbowMaxValue = Double.valueOf(st.nextToken()).doubleValue();
			}
			catch(NumberFormatException e) {
				Aladin.warning(ERR13,1);
				badSyntax=true;
				return;
			}

			// min and max are already fixed, set mustCompute to false
			mustComputeRainbowMinMax = false;
			userDefinedRainbowMinMax = true;
		}
	}

	/** decode une fonction rgb(,,) et retourne la couleur java correspondante. Les valeurs
	 * ne peuvent être variables - PF 26 mai 2006 */
	static protected Color decodeStaticRGB(String s) {
	   int pos,next;
	   int r,g,b;
	   try {
	      pos = s.indexOf('(')+1; next = s.indexOf(',',pos); r = Integer.parseInt(s.substring(pos,next));
	      pos = next+1;           next = s.indexOf(',',pos); g = Integer.parseInt(s.substring(pos,next));
	      pos = next+1;           next = s.indexOf(')',pos); b = Integer.parseInt(s.substring(pos,next));
	      return new Color(r,g,b);

	   } catch( Exception e ) { return null; }
	}

	/** decode une fonction rgb(,,). Set colorIsVariable a true si besoin est
	 *	@param s - chaine a decoder
	 */
	private  void decodeRGB(String s) {
		//System.out.println(s);
		//System.out.println(s.indexOf("("));
		//System.out.println(s.lastIndexOf(")"));
		String paramStr = s.substring(s.indexOf("(")+1, Math.max(s.lastIndexOf(")"),0));
		//System.out.println(paramStr);

		StringTokenizer st = new StringTokenizer(paramStr,",");
		// there must be exactly 3 parameters
		if( st.countTokens() != 3 ) {
			badSyntax = true;
			//System.out.println("rgb function should have exactly 3 parameters");
			Aladin.warning(ERR14,1);
			return;
		}

		// creation of R, G, B parsers
		try {
			redParser = UCDFilter.createParser(st.nextToken(),a);
			greenParser = UCDFilter.createParser(st.nextToken(),a);
			blueParser = UCDFilter.createParser(st.nextToken(),a);
		}
    	catch(ParserException e) {Aladin.warning(ERR15,1);badSyntax=true;return;}

		// if RGB values are constant
		if( redParser.isConstant() && greenParser.isConstant() && blueParser.isConstant() ) {
			this.color = new Color( Math.max(Math.min((float)redParser.eval()/255f,1f),0f), Math.max(Math.min((float)greenParser.eval()/255f,1f),0f), Math.max(Math.min((float)blueParser.eval()/255f,1f),0f) );
		}
		else colorIsVariable = true;
	}

	/** decode une couleur entree en RGB
	 *	@param s - la chaine reprensentant la couleur en RGB
	 *	@return la couleur correspondante, null si erreur durant le decodage
	 */
	static private Color decodeColorString(String s) {
		Color retCol;
		try {
			//retCol = Color.decode("#"+s);
			retCol = Color.decode(s);
		}
		catch (NumberFormatException e) {retCol=null;}
		return retCol;
	}

	/** looks for s in COLORNAME, returns the position of s in COLORNAME, -1 if not found */
	static protected int findColorName(String s) {
		for(int i=0;i<COLORNAME.length;i++) {
			if(s.equalsIgnoreCase(COLORNAME[i])) return i;
		}
		return -1;
	}

	/** looks for the color name corresponding to c, or rgb(r,g,b) if not found - PF 26 mai 06 */
	static protected String findColorName(Color c) {
		for(int i=0;i<MYCOLORS.length;i++) {
			if(c.equals(MYCOLORS[i])) return COLORNAME[i];
		}
		return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
	}

	private Color computeSaturationColor(Source s) {
	    double val;

	    // first, compute min/max values if it has not been done yet
        if( mustComputeSaturationMinMax ) {
            computeSaturationMinMax();
            mustComputeSaturationMinMax = false;
        }

        if( !setAllVariables(saturationParser, s, false) ) return null;

        val = saturationParser.eval();

        if(val==99.9) return null;

	    Color orgColor = s.plan.c;
	    float[] hsbVals = new float[3];
	    Color.RGBtoHSB(orgColor.getRed(), orgColor.getGreen(), orgColor.getBlue(), hsbVals);
	    float hue = hsbVals[0];
	    float value = hsbVals[2];
	    float saturation = (float)( (val-saturationMinValue)/(saturationMaxValue-saturationMinValue) );

	    return Color.getHSBColor(hue, saturation, value);
	}

	/** compute the "rainbow" color when it depends on parameters
	 *	@param s - the source being processed
	 *	@return the computed color, null if the color could not be computed
	 */
	private Color computeRainbowColor(Source s) {
		float hue;
		double val;


		// first, compute min/max values if it has not been done yet
		if( mustComputeRainbowMinMax ) {
			computeRainbowMinMax();
			mustComputeRainbowMinMax = false;
		}

		if( !setAllVariables(rainbowParser, s, false) ) return null;

		val = rainbowParser.eval();

		if(val==99.9) return null;


		hue = (float)( (val-rainbowMinValue)/(rainbowMaxValue-rainbowMinValue) );
        hue = getHue(hue);

		return Color.getHSBColor(hue, 1f, 1f);
	}

	/** petit traitement sur la valeur de hue */
	private float getHue(float hue) {
        // limite superieure pour hue (sinon, on retombe sur du rouge)
        final float upLimit = 0.78f;
	    float ret = 1-hue;
        // parce que hsv(1,x,y)==hsv(0,x,y)
        ret *= upLimit; // permet d'aller du violet au rouge
        // ce genre de situations peut se produire lorsque rainbow{Min,Max}value ont ete fixees par l'utilisateur
        if(ret>upLimit) ret=upLimit;
        if(ret<0) ret=0;

        return ret;
	}

	/** compute the color when it depends on parameters
	 *	@param s - the source being processed
	 *	@return the computed color, null if the color could not be computed (missing variable in source for example)
	 */
	private Color computeColor(Source s) {
		// RGB components of the color
		float redComp, greenComp, blueComp;
		double redVal, greenVal, blueVal;

		if( mustComputeRGBMinMax ) {
			computeRGBMinMax();
			mustComputeRGBMinMax = false;
		}

		if( ! setAllVariables(redParser, s, false) ) return null;
		if( ! setAllVariables(blueParser, s, false) ) return null;
		if( ! setAllVariables(greenParser, s, false) ) return null;

		redVal = redParser.eval();
		greenVal = greenParser.eval();
		blueVal = blueParser.eval();

		// Probleme du 99.9 dans les magnitudes
		if(redVal==99.9 || greenVal==99.9 || blueVal==99.9) return null;

		redComp = (redParser.isConstant())? Math.max(Math.min((float)redVal/255f,1f),0f) : (float) ((redVal-redMinValue)/(redMaxValue-redMinValue));
		greenComp = (greenParser.isConstant())? Math.max(Math.min((float)greenVal/255f,1f),0f) : (float) ((greenVal-greenMinValue)/(greenMaxValue-greenMinValue));

		//System.out.println(blueParser.eval());
		blueComp = (blueParser.isConstant())? Math.max(Math.min((float)blueVal/255f,1f),0f) : (float) ((blueVal-blueMinValue)/(blueMaxValue-blueMinValue));

		// Probleme du 99.9 dans les magnitudes
		/*if(redComp>1 || blueComp>1 || greenComp>1 || redComp<0 || blueComp<0 || greenComp<0) return null;*/
		//System.out.println(redComp+"\t"+greenComp+"\t"+blueComp);

		return new Color(redComp, greenComp, blueComp);
	}

	/** traite les parametres pour une fonction DRAWSTRING
	 *	@param param - la chaine des parametres
	 */
	private void decodeDrawString(String param) {
		StringTokenizer st = new StringTokenizer(param, ",");
		String curStr = null;

		while(st.hasMoreTokens()) {
			curStr = st.nextToken();
			if(curStr.startsWith(COLOR)) {
				// to retrieve all parameters
				try {
					while( countNbOcc('(',curStr) != countNbOcc(')',curStr) ) {
						curStr += ","+st.nextToken();
					}
				}
    	    	catch(NoSuchElementException e) {Aladin.warning(ERR16,1);badSyntax=true;return;}
    	    	processColor(curStr.substring(COLOR.length()));
    	    }
			// sinon on recupere le texte a afficher
			// on fait ca a l affichage pour le moment
			// a reporter ici quand on aura un tableau d objets dans Source --> finalement, on laisse ainsi

			else {
				textToDisp = curStr;
			}

		}
	}

	/** traite les parametres pour une fonction DRAWOBJECT
	 *	@param param - la chaine des parametres
	 */
	private void decodeDrawObject(String param) {
		StringTokenizer st = new StringTokenizer(param, ",");

    	String curStr = null;
    	while(st.hasMoreTokens()) {
    	    curStr = st.nextToken();

    	    if(curStr.startsWith(COLOR)) {
				// to retrieve all parameters
				try {
					while( countNbOcc('(',curStr) != countNbOcc(')',curStr) ) {
						curStr += ","+st.nextToken();
					}
				}
    	    	catch(NoSuchElementException e) {Aladin.warning(ERR16,1);badSyntax=true;return;}
    	    	processColor(curStr.substring(COLOR.length()));
    	    }

    	    else if(curStr.startsWith(SHAPE)) {

    	    	// to retrieve all parameters
    	    	if( ( curStr.indexOf(SIZE)>=0 || curStr.indexOf(FILLSIZE)>=0 || curStr.indexOf(FIXEDCIRCLE)>=0 || curStr.indexOf(LINE)>=0 ||
    	    		  curStr.indexOf(ELLIPSE)>=0 || curStr.indexOf(FILLELLIPSE)>=0 || curStr.indexOf(PM)>=0 || curStr.indexOf(RECTANGLE)>=0 )
    	    	    && curStr.indexOf("(")>=0 ) {
    	    	    try {

    	    	    	while( countNbOcc('(',curStr) != countNbOcc(')',curStr) ) {
    	    	    	    curStr += ","+st.nextToken();
    	    	    	}
    	    	    }
    	    	    catch(NoSuchElementException e) {Aladin.warning(ERR17,1);badSyntax=true;return;}

    	    	}
    	    	decodeShape(curStr);
    	    }
    	}

	}

    private void decodeShape(String s) {
    	//System.out.println(s);
    	String param = s.substring(s.indexOf("(")+1, Math.max(s.lastIndexOf(")"),0) );


    	// is there any parameter ?
    	if(param.length() != 0) {

    	    shape = s.substring(SHAPE.length(),s.indexOf("(")).trim();

    	    if(shape.equals(SIZE)||shape.equals(FILLSIZE)||shape.equals(FIXEDCIRCLE)) {
				StringTokenizer st = new StringTokenizer(param,",");

				// there can be 1 or 3 or 5 parameters
				// Parameters are :
				// 1 --> expression used to compute the radius of the circle
				// 2 --> min radius (pixels of image)
				// 3 --> max radius (pixels of image)
				// 4 --> minValue (value forced by user)
				// 5 --> maxValue (value forced by user)
				if( st.countTokens()!=1 && st.countTokens()!=3 && st.countTokens()!=5 ) {
					badSyntax = true;
					//System.out.println("Incorrect syntax for "+SIZE+" function. Syntax is "+SIZE+" (exp[,minRadius,maxRadius])");
					Aladin.warning(ERR11+" "+SIZE+" (exp[,minRadius,maxRadius])",1);
					return;
				}

    	    	// creation of the sizeParser
    	    	try {
    	    	    this.sizeParser = UCDFilter.createParser(st.nextToken(),a);
    	    	}
    	    	catch(ParserException e) {Aladin.warning(ERR19,1);badSyntax=true;}

				// in this case, the user has given minRadius and maxRadius values
				if( st.hasMoreTokens() ) {
					try {
						minRadius = Integer.valueOf(st.nextToken().trim()).intValue();
						maxRadius = Integer.valueOf(st.nextToken().trim()).intValue();
					}
					catch(NumberFormatException e) {
						Aladin.warning(ERR20,1);
						badSyntax=true;
						return;
					}
				}

				// in this case, the user forces values of minValue and maxValue
				if( st.hasMoreTokens() ) {
					try {
						minValue = Integer.valueOf(st.nextToken().trim()).intValue();
						maxValue = Integer.valueOf(st.nextToken().trim()).intValue();
					}
					catch(NumberFormatException e) {
						Aladin.warning(ERR21,1);
						badSyntax=true;
						return;
					}
					// minValue and maxValue are already fixed, set mustCompute to false
					mustComputeMinMax = false;
					userDefinedMinMax = true;
				}
    	    }

    	    else if(shape.equals(ELLIPSE) || shape.equals(FILLELLIPSE)) {
    	    	decodeEllipseParameters(param);
    	    }
    	    else if(shape.equals(RECTANGLE)) {
    	    	decodeRectangleParameters(param);
    	    }
    	    else if(shape.equals(PM)) {
    	    	decodePMParameters(param);
    	    }
    	    else if(shape.equals(LINE)) {
    	        decodeLineParameters(param);
    	    }
    	}
    	else {
    	    shape = s.substring(SHAPE.length());
    	    if( shape.equals(LINE) || shape.equals(ELLIPSE) || shape.equals(FILLELLIPSE )
    	    		|| shape.equals(RECTANGLE) || shape.equals(SIZE) || shape.equals(FILLSIZE)
					|| shape.equals(FIXEDCIRCLE) || shape.equals(PM)) {
    	    	Aladin.warning(ERR22+" "+shape,1);badSyntax=true;
    	    }
    	}
    }

    /** decodes the parameters of a LINE action
     *  @param param the string of parameters
     */
    private void decodeLineParameters(String param) {
    	StringTokenizer st = new StringTokenizer(param,",");
    	String ra1,dec1,ra2,dec2;
    	ra1=dec1=ra2=dec2="";

    	try {
    	    ra1 = st.nextToken();
    	    dec1 = st.nextToken();
    	    ra2 = st.nextToken();
    	    dec2 = st.nextToken();
    	}
    	catch(NoSuchElementException e) {Aladin.warning("Missing parameter for the line function !",1);badSyntax=true;}

    	lineRa1Parser = UCDFilter.createParser(ra1,a);
    	lineDec1Parser = UCDFilter.createParser(dec1,a);
    	lineRa2Parser = UCDFilter.createParser(ra2,a);
    	lineDec2Parser = UCDFilter.createParser(dec2,a);
    }
//    */

    /** decodes the parameters of a PM action
     *  @param param the string of parameters
     */
    private void decodePMParameters(String param) {
    	StringTokenizer st = new StringTokenizer(param,",");
    	String pmdec,pmra;
    	pmdec=pmra="";

    	try {
    	    pmra = st.nextToken();
    	    pmdec = st.nextToken();
    	}
    	catch(NoSuchElementException e) {Aladin.warning(ERR23,1);badSyntax=true;}

    	//System.out.println(pmdec);
    	//System.out.println(pmra);

    	pmRAParser = UCDFilter.createParser(pmra,a);
    	pmDecParser = UCDFilter.createParser(pmdec,a);
    }


    /** decodes the parameters of an ELLIPSE action
     *  @param param the string of parameters
     */
    private void decodeRectangleParameters(String param) {
    	decodeEllipseParameters(param);
    }

    /** decodes the parameters of an ELLIPSE action
     *  @param param the string of parameters
     */
    private void decodeEllipseParameters(String param) {
    	StringTokenizer st = new StringTokenizer(param,",");
    	String majAxis, minAxis, posAngle;
    	majAxis=minAxis=posAngle="";

    	try {
    	    majAxis = st.nextToken();
    	    minAxis = st.nextToken();
    	    posAngle = st.nextToken();
    	}
    	catch(NoSuchElementException e) {Aladin.warning(ERR24,1);badSyntax=true;}

    	//System.out.println(majAxis);
    	//System.out.println(minAxis);
    	//System.out.println(posAngle);
    	majAxisParser = UCDFilter.createParser(majAxis,a);
    	minAxisParser = UCDFilter.createParser(minAxis,a);
    	posAngleParser = UCDFilter.createParser(posAngle,a);
    }

    /** set maxValue, minValue */
    private void computeMinMax(Source[] sources) {
        // if there is no variable
        if( sizeParser.isConstant() ) {minValue=0;maxValue=sizeParser.eval();return;}

        minValue = Double.POSITIVE_INFINITY;
        maxValue = Double.NEGATIVE_INFINITY;

        Source s;

        double value;

        // loop on all plans and selection of catalogs which are active
        for( int i=sources.length-1; i>=0; i-- ) {
            s = sources[i];

            // Pour laisser la main aux autres threads
            if( Aladin.isSlow && i%50==0 ) Util.pause(10);

            if( ! setAllVariables(sizeParser, s, false) ) continue;
            value = sizeParser.eval();
            // reserved value in FITS to indicate a problem
            if(Math.abs(value)!=99.9) {
                if(value>maxValue) maxValue = value;
                if(value<minValue) minValue = value;
            }
        }
        //System.out.println(minValue+" "+maxValue);
    }

    /** set maxValue, minValue */
    /*
    private void computeMinMax() {
    	// if there is no variable
    	if( sizeParser.isConstant() ) {minValue=0;maxValue=sizeParser.eval();return;}

    	minValue = Double.POSITIVE_INFINITY;
    	maxValue = Double.NEGATIVE_INFINITY;

    	int i,j;

    	Source s;

    	Plan p = null;
    	Obj[] o = null;
		double value;
        Plan[] plans = pf.getConcernedPlans();

    	// loop on all plans and selection of catalogs which are active
    	for( i=plans.length-1; i>=0; i-- ) {
    	    p = plans[i];
    	    	o = p.pcat.o;
    	    	// loop on all objects in PlanCatalog
    	    	for( j=o.length-1; j>=0; j-- ) {

                    // Pour laisser la main aux autres threads
                    if( Aladin.isSlow && j%50==0 ) Util.pause(10);

    	    	    if( o[j] instanceof Source && o[j]!=null) {
    	    	    	s=(Source)o[j];

						//setAllVariables(sizeParser, s, true);
						if( ! setAllVariables(sizeParser, s, false) ) continue;
    	    	    	value = sizeParser.eval();
						// reserved value in FITS to indicate a problem
						if(Math.abs(value)!=99.9) {
    	    	    		if(value>maxValue) maxValue = value;
    	    	    		if(value<minValue) minValue = value;
						}
    	    	    }
    	    	}
    	}
    	//System.out.println(minValue+" "+maxValue);
    }
    */

    // TODO : computeSaturationMinMax et computeRainbowMinMax font la meme chose !!

    /** compute saturation{Min,Max}Value */
    private void computeSaturationMinMax() {
        // initialization
        saturationMinValue = Double.POSITIVE_INFINITY;
        saturationMaxValue = Double.NEGATIVE_INFINITY;

        int i,j;

        Plan p = null;
        Plan[] plans = pf.getConcernedPlans();
        double value = Double.POSITIVE_INFINITY;

        // loop on all plans and selection of catalogs which are active
        for( i=plans.length-1; i>=0; i-- ) {
            p = plans[i];
            if( !p.isCatalog() ) continue;
            Iterator<Obj> it = ((PlanCatalog)plans[i]).iterator();
            for( j=0; it.hasNext(); j++ ) {
               Obj o = it.next();
               if( !(o instanceof Source) ) continue;
               Source s = (Source)o;

                // Pour laisser la main aux autres threads
                if( Aladin.isSlow && j%50==0 ) Util.pause(10);

                if( s!=null) {

                    if( !setAllVariables(saturationParser, s, false)  ) {
                        continue;
                    }

                    value = saturationParser.eval();
                    // reserved value in FITS to indicate a problem
                    if( Math.abs(value)!=99.9) {
                        if(value>saturationMaxValue) saturationMaxValue = value;
                        if(value<saturationMinValue) saturationMinValue = value;
                    }

                }
            }
        }

        if( value == saturationMaxValue ) {saturationMinValue = 0;}

    }

	/** compute rainbow{Min,Max}Value */
	private void computeRainbowMinMax() {
		// initialization
		rainbowMinValue = Double.POSITIVE_INFINITY;
		rainbowMaxValue = Double.NEGATIVE_INFINITY;

		int i,j;

    	Plan p = null;
        Plan[] plans = pf.getConcernedPlans();
		double hue = Double.POSITIVE_INFINITY;

    	// loop on all plans and selection of catalogs which are active
    	for( i=plans.length-1; i>=0; i-- ) {
    	    p = plans[i];
    	    if( !p.isCatalog() ) continue;
    	    Iterator<Obj> it = p.iterator();
    	    for( j=0; it.hasNext(); j++ ) {
    	         Obj o = it.next();
    	         if( !(o instanceof Source) ) continue;
    	         Source s = (Source)o;

                // Pour laisser la main aux autres threads
                if( Aladin.isSlow && j%50==0 ) Util.pause(10);

    	   	    if( s!=null) {

					if( !setAllVariables(rainbowParser, s, false)  ) {
						continue;
                    }

					hue = rainbowParser.eval();
					// reserved value in FITS to indicate a problem
					if( Math.abs(hue)!=99.9) {
    	    	    	if(hue>rainbowMaxValue) rainbowMaxValue = hue;
    	    	    	if(hue<rainbowMinValue) rainbowMinValue = hue;
					}

    	    	}
    	    }
    	}

		if( hue == rainbowMaxValue ) {rainbowMinValue = 0;}

	}

	/** compute {red,green,blue}{Min,Max}Value */
	private void computeRGBMinMax() {
		// initialization
		redMinValue = greenMinValue = blueMinValue = Double.POSITIVE_INFINITY;
		redMaxValue = greenMaxValue = blueMaxValue = Double.NEGATIVE_INFINITY;
		int i,j;

    	Plan p = null;
        Plan[] plans = pf.getConcernedPlans();
		double redValue,greenValue,blueValue;

    	// loop on all plans and selection of catalogs which are active
    	for( i=plans.length-1; i>=0; i-- ) {
    	    p = plans[i];
    	    if( !p.isCatalog() ) continue;
    	    Iterator<Obj> it = ((PlanCatalog)p).iterator();
    	    for( j=0; it.hasNext(); j++ ) {
    	         Obj o = it.next();
    	         if( !(o instanceof Source) ) continue;
    	         Source s = (Source)o;

                // Pour laisser la main aux autres threads
                if( Aladin.isSlow && j%50==0 ) Util.pause(10);


    	        if( s!=null) {

					if( !setAllVariables(redParser, s, false) ||
					!setAllVariables(greenParser, s, false) ||
					!setAllVariables(blueParser, s, false) ) continue;
    	        	redValue = redParser.eval();
					greenValue = greenParser.eval();
					blueValue = blueParser.eval();
					// reserved value in FITS to indicate a problem
					if( Math.abs(redValue)!=99.9) {
    	        		if(redValue>redMaxValue) redMaxValue = redValue;
    	        		if(redValue<redMinValue) redMinValue = redValue;
					}

					if( Math.abs(greenValue)!=99.9 ) {
    	        		if(greenValue>greenMaxValue) greenMaxValue = greenValue;
    	        		if(greenValue<greenMinValue) greenMinValue = greenValue;
					}

					if( Math.abs(blueValue)!=99.9 ) {
    	        		if(blueValue>blueMaxValue) blueMaxValue = blueValue;
    	        		if(blueValue<blueMinValue) blueMinValue = blueValue;
					}
    	        }
    	    }
    	}

		if( redMinValue == redMaxValue ) {redMinValue = 0;}
		if( greenMinValue == greenMaxValue ) {greenMinValue = 0;}
		if( blueMinValue == blueMaxValue ) {blueMinValue = 0;}

        //System.out.println(redMinValue+" "+redMaxValue+" "+greenMinValue+" "+greenMaxValue+" "+blueMinValue+" "+blueMaxValue);
    }


    /** draws the PM vector of a source
     *	@param s - the source
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void computePM(Source s, int numero, int index) {
    	// pour le MP en RA
		if ( ! setAllVariables(pmRAParser, s, false, true) ) return;

    	// pour le MP en dec
		if ( ! setAllVariables(pmDecParser, s, false, true) ) return;

		// store the values first
		s.values[numero][index][0] = pmRAParser.eval();
		s.values[numero][index][1] = pmDecParser.eval();

		//// on convertit en mas/yr RA et Dec (si possible) ////
		Unit uRA = null;
		Unit uDec = null;
		try {
			uRA = pmRAParser.evalUnit();
			uDec = pmDecParser.evalUnit();
		}
		catch(ParseException e) {return;}

		Unit RAunitless = null;
		Unit RAmas = null;
		Unit DEmas = null;

		double newValRA, newValDe;
		boolean succeed = true;
		try {
			RAunitless = new Unit("1ms/yr");
			// on essaye de convertir uRA en s/yr
			RAunitless.convertFrom(uRA);
		}
		catch(java.text.ParseException e) {succeed=false;}
		catch(ArithmeticException e2) {succeed=false;}

		if( succeed ) {
			newValRA = 15*RAunitless.value*Math.cos(s.dej*Math.PI/180.0);
		}
		// sinon, on tente de convertir en arcsec/yr
		else {
			succeed = true;
			try {
				RAmas = new Unit("1mas/yr");
				RAmas.convertFrom(uRA);
			}
			catch(java.text.ParseException e) {succeed=false;}
			catch(ArithmeticException e2) {succeed=false;}
			if( succeed ) {
				newValRA = RAmas.value;
			}
			// on n'a pu convertir RA ni en s/yr ni en arcsec/yr --> on garde les valeurs originales
			else {
				return;
			}
		}

		// On essaye maintenant de convertir Dec en arcsec/yr
		succeed = true;
		try {
			DEmas = new Unit("1mas/yr");
			DEmas.convertFrom(uDec);
		}
		catch(java.text.ParseException e) {succeed=false;}
		catch(ArithmeticException e2) {succeed=false;}
		// on a réussi  convertir RA et Dec
		if( succeed ) {
			newValDe = DEmas.value;
			// si la conversion a donné des choses étranges
			if( Double.isNaN(newValRA)||Double.isNaN(newValDe) ) {
				return;
			}
			s.values[numero][index][0] = newValRA;
			s.values[numero][index][1] = newValDe;
		}
		else {
			return;
		}
    }

    /** draws the LINE vector of a source
     *	@param s - the source
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void computeLine(Source s, int numero, int index) {
        // TODO : que faire si les positions sont données en sexagésimale ?
    	// pour RA du 1er point
		if ( ! setAllVariables(lineRa1Parser, s, false, true) ) return;

    	// pour le DEC du 1er point
		if ( ! setAllVariables(lineDec1Parser, s, false, true) ) return;

    	// pour RA du 2e point
		if ( ! setAllVariables(lineRa2Parser, s, false, true) ) return;

    	// pour le DEC du 2e point
		if ( ! setAllVariables(lineDec2Parser, s, false, true) ) return;

		// store the values first
		s.values[numero][index][0] = lineRa1Parser.eval();
		s.values[numero][index][1] = lineDec1Parser.eval();
		s.values[numero][index][2] = lineRa2Parser.eval();
		s.values[numero][index][3] = lineDec2Parser.eval();
	}
//	*/

    /** private method used by computePM and which actually draws the vector on the graphic context g
     *	@param s - the source
     *	@param g - the graphic context
     *  @param v
     *	@param p - position of the source in g
     *	@param c - color of the ellipse
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void drawPM(Source s, Graphics g, ViewSimple v, Point p, Color c, int numero, int index, int dx, int dy) {
    	double factor = 1000.0; // par combien on multiplie pour voir qqch ?
    	factor *= s.plan.getScalingFactor();

    	// un déplacement de 1 arcsec/yr est représenté par une flèche de 1*factor mas
    	// prendre les valeurs en mas/yr, et multiplier d'office par 1000
		double pmRA = s.values[numero][index][0];
		double pmDec = s.values[numero][index][1];

        double lFleche = (pmRA*pmRA+pmDec*pmDec)/100; // PF longueur du bout de la fleche
        if( lFleche>6 ) lFleche=6;

   		// -1 est la valeur d'initialisation
		if(pmRA==-1 && pmDec==-1) return;

		// fixe un bug qui faisait bouger les sources quand on switchait de vue
		Plan base = v.pref;
		if(base==null) base = s.plan;

		Projection proj =  v.getProj();

    	Coord coord = new Coord();
		coord.al = s.raj;
		coord.del = s.dej;

		coord =proj.getXY(coord);
		if( Double.isNaN(coord.x) ) return;

		double orgX = coord.x;
		double orgY = coord.y;

		// multiplication par le facteur pour y voir qqch
		pmRA *= factor;
		pmDec *= factor;
		// pmRA et pmDec sont en mas/yr
    	coord.al += (pmRA/3600000)/Math.cos(Math.PI*coord.del/180.0);
        coord.del += (pmDec/3600000);

    	coord = new Coord(coord.al,coord.del);
    	coord = proj.getXY(coord);
    	if( Double.isNaN(coord.x) ) return;

		// dessin du mouvement propre
		Point p1 = v.getViewCoord(orgX,orgY);
		Point p2 = v.getViewCoord(coord.x,coord.y);
		if( p1==null || p2==null ) return;
		double dist = Math.sqrt(Math.pow(p1.x-p2.x, 2)+Math.pow(p1.y-p2.y, 2));

		// Passe-t-on de l'autre côté du ciel ?
		// Méthode : on teste la symétrie. Si la distance projetée varie trop, on ne trace pas
		if( proj.t==Calib.AIT || proj.t==Calib.MOL ) {
		   Coord coordBis = new Coord(s.raj,s.dej);
		   coordBis.al -= (pmRA/3600000)/Math.cos(Math.PI*coord.del/180.0);
		   coordBis.del -= (pmDec/3600000);
		   proj.getXY(coordBis);
		   Point p2Bis = v.getViewCoord(coordBis.x,coordBis.y);
		   double distBis = Math.sqrt(Math.pow(p1.x-p2Bis.x, 2)+Math.pow(p1.y-p2Bis.y, 2));
		   if( Math.abs(distBis-dist)>50 ) { return; }
		}

		// calcul pour les fleches
		lFleche *= v.getZoom();
		if( lFleche>dist/5 ) {
		    lFleche = dist/5;
		}
        if( lFleche>17) lFleche=17;         // PF
        else if( lFleche<3 ) lFleche=4;     // PF
		// angle que fait p2 avec l'horizontale dans le repere (p1,x,y)
		double angle = Math.atan((orgY-coord.y)/(orgX-coord.x));
		if( (orgX-coord.x)<0 ) angle = angle+Math.PI;

		double alpha = angle+45*Math.PI/180.0;
		Point f1 = new Point( (int)(lFleche*Math.cos(alpha))+p2.x,
							  (int)(lFleche*Math.sin(alpha))+p2.y );
		alpha = angle-45*Math.PI/180.0;
		Point f2 = new Point( (int)(lFleche*Math.cos(alpha))+p2.x,
							  (int)(lFleche*Math.sin(alpha))+p2.y );

		// prise en compte du décalage induit par dx et dy
		p1.x+=dx;p2.x+=dx;f1.x+=dx;f2.x+=dx;
		p1.y+=dy;p2.y+=dy;f1.y+=dy;f2.y+=dy;

		g.setColor(c);
		g.drawLine(p1.x,p1.y,p2.x,p2.y);
		g.drawLine(p2.x,p2.y,f1.x,f1.y);
		g.drawLine(p2.x,p2.y,f2.x,f2.y);

    }



    /** actually draws the vector (LINE function) on the graphic context g
     *	@param s - the source
     *	@param g - the graphic context
     *  @param v
     *	@param p - position of the source in g
     *	@param c - color of the ellipse
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void drawLine(Source s, Graphics g, ViewSimple v, Point p, Color c, int numero, int index, int dx, int dy) {
		double ra1 = s.values[numero][index][0];
		double de1 = s.values[numero][index][1];
		double ra2 = s.values[numero][index][2];
		double de2 = s.values[numero][index][3];

   		// -1 est la valeur d'initialisation
		if(ra1==-1 && de1==-1) return;

		// fixe un bug qui faisait bouger les sources quand on switchait de vue
		Plan base = v.pref;
		if(base==null) base = s.plan;

    	Coord coord1 = new Coord();
		coord1.al = ra1;
		coord1.del = de1;

		coord1=v.getProj().getXY(coord1);
		if( Double.isNaN(coord1.x) ) return;

    	Coord coord2 = new Coord();
    	coord2.al = ra2;
    	coord2.del = de2;

    	coord2=v.getProj().getXY(coord2);
        if( Double.isNaN(coord2.x) ) return;


		// dessin de la ligne entre les 2 points
		Point p1 = v.getViewCoord(coord1.x,coord1.y);
		Point p2 = v.getViewCoord(coord2.x,coord2.y);
		if( p1==null || p2==null ) return;

		g.setColor(c);

		p1.x+=dx;p2.x+=dx;//f1.x+=dx;f2.x+=dx;
		p1.y+=dy;p2.y+=dy;//f1.y+=dy;f2.y+=dy;

		g.drawLine(p1.x,p1.y,p2.x,p2.y);

    }
//    */

    /** draws the rectangle associated with a source
     *	@param s - the source
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void computeRectangle(Source s, int numero, int index) {
    	computeEllipse(s,numero,index);
    }

    /** draws the ellipse associated with a source
     *	@param s - the source
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
    private void computeEllipse(Source s, int numero, int index) {
		// reference unit for major and minor axis : arcsec
        Unit arcsecMajUnit=null;
        Unit arcsecMinUnit=null;
//      reference unit for position angle : deg
        Unit degPosAngUnit=null;
        try{
            arcsecMajUnit = new Unit("1arcsec");
            arcsecMinUnit = new Unit("1arcsec");
            degPosAngUnit = new Unit("1deg");
        }
        catch(java.text.ParseException e) {}


    	// On commence par mettre les valeurs sans unité
		// on considere que par defaut : grand axe et petit axe en arcsec
		// posangle en degres decimaux
		// On essaye la conversion par la suite

        boolean setVarUnitSucceed = true;
    	// pour le grand axe, on tente d'abord avec les unités, sinon avec les valeurs
		if( ! setAllVariables(majAxisParser, s, false,true) ) {
            setVarUnitSucceed = false;
            if( ! setAllVariables(majAxisParser, s, false) ) return;
        }

    	// pour le petit axe, idem
        if( ! setAllVariables(minAxisParser, s, false,true) ) {
            setVarUnitSucceed = false;
            if( ! setAllVariables(minAxisParser, s, false) ) return;
        }

		// A ce stade, on sauvegarde deja les valeurs brutes de petit axe et grand axe dans s.values (comme ca c'est fait meme si on trouve rien pour l'angle de position)
        s.values[numero][index][0] = majAxisParser.eval();
    	s.values[numero][index][1] = minAxisParser.eval();

        // on essaye de convertir grand axe et petit axe en arcsec
        boolean convertSucceed = true;
        if( setVarUnitSucceed ) {

            try {
                Unit majAxUnit = majAxisParser.evalUnit();
                Unit minAxUnit = minAxisParser.evalUnit();
                //System.out.println("Unit : "+majAxUnit);
                //System.out.println(majAxUnit.unitSI());
                arcsecMajUnit.convertFrom(majAxUnit);
                //System.out.println(arcsecMajUnit);
                //System.out.println("Unit : "+minAxUnit);
                arcsecMinUnit.convertFrom(minAxUnit);
                //System.out.println(arcsecMinUnit);
                // VOIR AVEC FRANCOIS SI IL Y A UNE AUTRE SOLUTION
                if( majAxUnit.toStringInSI().equals("mag") || minAxUnit.toStringInSI().equals("mag")
                    || majAxUnit.symbol.equals("") || minAxUnit.symbol.equals("") ) convertSucceed = false;

            }
            catch(java.text.ParseException e) {convertSucceed = false;System.out.println("ERROR");}
            catch(ArithmeticException e2) {convertSucceed = false;}
            if( Double.isNaN(arcsecMajUnit.value) || Double.isNaN(arcsecMinUnit.value) ) convertSucceed = false;
            // si tout s'est bien passé, on set les nouvelles valeurs
            if( convertSucceed ) {
                s.values[numero][index][0] = arcsecMajUnit.value;
                s.values[numero][index][1] = arcsecMinUnit.value;
            }
        }

        // on s'occupe de l'angle de position de manière similaire
        setVarUnitSucceed = true;
    	// pour l'angle de position, on tente d'abord avec les unités, sinon avec les valeurs
        if( ! setAllVariables(posAngleParser, s, false,true) ) {
            setVarUnitSucceed = false;
            if( ! setAllVariables(posAngleParser, s, false) ) return;
        }

        // A ce stade, on sauvegarde deja la valeur brute de l'angle de pos
    	s.values[numero][index][2] = posAngleParser.eval();

        // on essaye de convertir angle de position en degrés
        convertSucceed = true;
        if( setVarUnitSucceed ) {
            try {
                // TODO : j'obtiens des resultats etranges ici
                Unit posAngUnit = posAngleParser.evalUnit();
                //System.out.println("Unit : "+posAngUnit);
                degPosAngUnit.convertFrom(posAngUnit);
                //System.out.println(degPosAngUnit);
                // VOIR AVEC FRANCOIS SI IL Y A UNE AUTRE SOLUTION
                if( posAngUnit.toStringInSI().equals("mag") || posAngUnit.symbol.equals("") )
                    convertSucceed = false;

            }
            catch(java.text.ParseException e) {convertSucceed = false;}
            catch(ArithmeticException e2) {convertSucceed = false;}
            if( Double.isNaN(degPosAngUnit.value) ) convertSucceed = false;
            // si tout s'est bien passé, on set les nouvelles valeurs
            if( convertSucceed ) {
                s.values[numero][index][2] = degPosAngUnit.value;

            }
        }
    }

    /** private method which actually draws the rectangle on the graphic context g
     *	@param s - the source
     *	@param g - the graphic context
     *  @param v
     *	@param p - position of the source in g
     *	@param c - color of the rectangle
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     */
	 // Remarque : on pourrait ameliorer le temps de calcul en ne recalculant pas a chaque fois
	 // il faudrait verifier que le plan de ref. n'a pas change
    private void drawRectangle(Source s, Graphics g, ViewSimple v, Point p, Color c, int numero, int index, int dx, int dy) {
		// majAxis and minAxis in arcsec, posAngle in deg
    	double majAxis = s.values[numero][index][0];
    	double minAxis = s.values[numero][index][1];
    	double posAngle = s.values[numero][index][2];

		if(posAngle==-1) posAngle=0;
		//System.out.println(posAngle);

		if(majAxis==-1) return;
		//System.out.println(majAxis+" "+minAxis+" "+posAngle);
		// fin test

		// Fixe le bug qui faisait bouger les sources dans tous les sens en multi-vues
		Plan base = v.pref;
		if(base==null) base = s.plan;



    	Coord coord = new Coord();
		coord.al = s.raj;
		coord.del = s.dej;

    	Coord coord2 = new Coord();
		coord2.al = s.raj;
		coord2.del = s.dej;

		coord2=v.getProj().getXY(coord2);
        if( Double.isNaN(coord2.x) ) return;

		double orgX = coord2.x;
		double orgY = coord2.y;


    	coord.al += (majAxis/3600)*Math.sin(posAngle*2*Math.PI/360)/Math.cos(coord.del*2*Math.PI/360);
        coord.del += (majAxis/3600)*Math.cos(posAngle*2*Math.PI/360);

    	coord2.al += (minAxis/3600)*Math.cos(posAngle*2*Math.PI/360)/Math.cos(coord2.del*2*Math.PI/360);
        coord2.del += -(minAxis/3600)*Math.sin(posAngle*2*Math.PI/360);

    	coord = new Coord(coord.al,coord.del);
    	coord = v.getProj().getXY(coord);
        if( Double.isNaN(coord.x) ) return;

    	coord2 = new Coord(coord2.al,coord2.del);
    	coord2 = v.getProj().getXY(coord2);
        if( Double.isNaN(coord2.x) ) return;


		// dessin du grand axe
		Point p1 = v.getViewCoord(coord.x,coord.y);
		double x1 = coord.x;
		double y1 = coord.y;

		Point p2 = v.getViewCoord(coord.x-2*(coord.x-orgX),coord.y-2*(coord.y-orgY));
		if( p1==null || p2==null ) return;

		double gdAxe = Math.sqrt(Math.pow(p2.x-p1.x,2)+Math.pow(p2.y-p1.y,2));
		// dessin du petit axe
		p1 = v.getViewCoord(coord2.x,coord2.y);
		p2 = v.getViewCoord(coord2.x-2*(coord2.x-orgX),coord2.y-2*(coord2.y-orgY));
        if( p1==null || p2==null ) return;

		double petitAxe = Math.sqrt(Math.pow(p2.x-p1.x,2)+Math.pow(p2.y-p1.y,2));
		Point centre = new Point(((p1.x+p2.x)/2),((p1.y+p2.y)/2));

		// de combien faut il tourner l ellipse dans le contexte graphique courant
		double angle = Math.atan((orgY-y1)/(orgX-x1))*180/Math.PI;
		//System.out.println(angle);

		doDrawRectangle(g,c,centre,0.25*gdAxe,0.25*petitAxe,angle,dx,dy);
    }

    /** private method which actually draws the ellipse on the graphic context g
     *	@param s - the source
     *	@param g - the graphic context
     *  @param v
     *	@param p - position of the source in g
     *	@param c - color of the ellipse
     *  @param numero - numero du filtre
     *  @param index - index de l'action
     *  @param transparency true if transparency is activated
     */
	 // Remarque : on pourrait ameliorer le temps de calcul en ne recalculant pas a chaque fois
	 // il faudrait verifier que le plan de ref. n'a pas change
    private void drawEllipse(Source s, Graphics g, ViewSimple v, Point p,
            Color c, int numero, int index, int dx, int dy, boolean transparency) {
        // majAxis and minAxis in arcsec, posAngle in deg
    	double majAxis = s.values[numero][index][0];
    	double minAxis = s.values[numero][index][1];
    	double posAngle = s.values[numero][index][2];

		if(posAngle==-1) posAngle=0;
		//System.out.println(posAngle);

		if(majAxis==-1) return;
		//System.out.println(majAxis+" "+minAxis+" "+posAngle);
		// fin test

		// Fixe le bug qui faisait bouger les sources dans tous les sens en multi-vues
		Plan base = v.pref;
		if(base==null) base = s.plan;

    	Coord coord = new Coord();
		coord.al = s.raj;
		coord.del = s.dej;

    	Coord coord2 = new Coord();
		coord2.al = s.raj;
		coord2.del = s.dej;

		Projection proj = v.getProj();
		coord2=proj.getXY(coord2);

		double orgX = coord2.x;
		double orgY = coord2.y;

    	coord.al += (majAxis/3600)*Math.sin(posAngle*2*Math.PI/360)/Math.cos(coord.del*2*Math.PI/360);
        coord.del += (majAxis/3600)*Math.cos(posAngle*2*Math.PI/360);

    	coord2.al += (minAxis/3600)*Math.cos(posAngle*2*Math.PI/360)/Math.cos(coord2.del*2*Math.PI/360);
        coord2.del += -(minAxis/3600)*Math.sin(posAngle*2*Math.PI/360);

//    	coord = new Coord(coord.al,coord.del);     // ME SEMBLE INUTILE PF - DEC 2010
    	coord = proj.getXY(coord);

//    	coord2 = new Coord(coord2.al,coord2.del);  // ME SEMBLE INUTILE PF - DEC 2010
    	coord2 = proj.getXY(coord2);

		// calcul du grand axe
		Point p1 = v.getViewCoord(coord.x,coord.y);
		double x1 = coord.x;
		double y1 = coord.y;

		Point p2 = v.getViewCoord(coord.x-2*(coord.x-orgX),coord.y-2*(coord.y-orgY));
		if( p1==null || p2==null ) return;

		double gdAxe = Math.sqrt(Math.pow(p2.x-p1.x,2)+Math.pow(p2.y-p1.y,2));

		// calcul du petit axe
		p1 = v.getViewCoord(coord2.x,coord2.y);
		p2 = v.getViewCoord(coord2.x-2*(coord2.x-orgX),coord2.y-2*(coord2.y-orgY));
        if( p1==null || p2==null ) return;

		double petitAxe = Math.sqrt(Math.pow(p2.x-p1.x,2)+Math.pow(p2.y-p1.y,2));

	      // Ne passe-t-on pas "derrière le ciel" - Modif PF déc 2010
        if( proj.t==Calib.AIT || proj.t==Calib.MOL ) {
           Coord coordBis = new Coord(s.raj,s.dej);
           coordBis.al -= (majAxis/3600)*Math.sin(posAngle*2*Math.PI/360)/Math.cos(coordBis.del*2*Math.PI/360);
           coordBis.del -= (majAxis/3600)*Math.cos(posAngle*2*Math.PI/360);
           proj.getXY(coordBis);
           Point p1Bis = v.getViewCoord(coordBis.x,coordBis.y);
           Point p2Bis = v.getViewCoord(coordBis.x-2*(coordBis.x-orgX),coordBis.y-2*(coordBis.y-orgY));
           double gdAxeBis = Math.sqrt(Math.pow(p2Bis.x-p1Bis.x,2)+Math.pow(p2Bis.y-p1Bis.y,2));
           if( gdAxeBis<gdAxe ) gdAxe=gdAxeBis;

           Coord coord2Bis = new Coord(s.raj,s.dej);
           coord2Bis.al -= (minAxis/3600)*Math.cos(posAngle*2*Math.PI/360)/Math.cos(coord2Bis.del*2*Math.PI/360);
           coord2Bis.del -= -(minAxis/3600)*Math.sin(posAngle*2*Math.PI/360);
           proj.getXY(coord2Bis);
           p1Bis = v.getViewCoord(coord2Bis.x,coord2Bis.y);
           p2Bis = v.getViewCoord(coord2Bis.x-2*(coord2Bis.x-orgX),coord2Bis.y-2*(coord2Bis.y-orgY));
           double petitAxeBis = Math.sqrt(Math.pow(p2Bis.x-p1Bis.x,2)+Math.pow(p2Bis.y-p1Bis.y,2));
           if( petitAxeBis<petitAxe ) {
              petitAxe=petitAxeBis;
              p2 = p2Bis;
              p1 = p1Bis;
           }
       }

		Point centre = new Point(((p1.x+p2.x)/2),((p1.y+p2.y)/2));

		// de combien faut il tourner l ellipse dans le contexte graphique courant
		//double angle = Math.asin((orgY-y1)/Math.sqrt(Math.pow(x1-orgX,2)+Math.pow(y1-orgY,2)))*180/Math.PI;
		//System.out.println(angle);
		double angle = Math.atan((orgY-y1)/(orgX-x1))*180/Math.PI;
		//System.out.println(angle);

		doDrawEllipse(g,s,c,centre,0.5*gdAxe,0.5*petitAxe,angle,dx,dy,transparency);

    }

	/** draws a rectangle which can be rotated
	 *	@param g - the graphic context we draw on
	 *	@param c - color of the ellipse
	 *	@param center - the "center" of the ellipse
	 *	@param width - value of the width of the rectangle
	 *	@param height - value of the height of the rectangle
	 *	@param angle - rotation angle around center
	 */
	private void doDrawRectangle(Graphics g, Color c, Point center, double width, double height, double angle, int dx, int dy) {
		// convert the angle into radians
		angle = angle*Math.PI/180.0;

		// number of iterations
		Point[] p = new Point[4];
		double x,y,tmpX,tmpY;

		int signX, signY;
		for( int i=0; i<4; i++ ) {
			signX = (i==0||i==3)?-1:1;
			signY = (i>1)?-1:1;
			tmpX = signX*width;
			tmpY = signY*height;

			x = tmpX*Math.cos(angle)-tmpY*Math.sin(angle)+center.x;
			y = tmpX*Math.sin(angle)+tmpY*Math.cos(angle)+center.y;

			// prise en compte du décalage dx, dy (impression)
			x += dx;
			y += dy;

//			p[i] = new Point( (int)(signX*semiMA), (int)(signY*semiMI));
			p[i] = new Point((int)x, (int)y);
		}


		g.setColor(c);
		// then we draw
		for(int i=0; i<4-1; i++) {
			g.drawLine(p[i].x,p[i].y,p[i+1].x,p[i+1].y);
		}
		g.drawLine(p[4-1].x,p[4-1].y,p[0].x,p[0].y);
	}

    /** Draw an ellipse in an EPS graphics context (PF March 2007) */
    private void doDrawEllipseEPS(Graphics g, Color c, Point center, double semiMA, double semiMI, double angle, int dx, int dy) {
       g.setColor(c);
       ((EPSGraphics)g).drawEllipse(center.x+dx,center.y+dy,semiMA,semiMI,angle);
    }

	/** draws an ellipse which can be rotated
	 *	@param g - the graphic context we draw on
	 *	@param c - color of the ellipse
	 *	@param center - the "center" of the ellipse
	 *	@param semiMA - value of the semi-major axis
	 *	@param semiMI - value of the semi-minor axis
	 *	@param angle - rotation angle around center
	 */
	private void doDrawEllipse(Graphics g, Source s,Color c, Point center, double semiMA, double semiMI,
	        double angle, int dx, int dy, boolean transparency) {

        if( g instanceof EPSGraphics ) {
           doDrawEllipseEPS(g,c,center,semiMA,semiMI,angle,dx,dy); return;
        }

		try {
		    boolean drawInTransparency = transparency && Aladin.ENABLE_FOOTPRINT_OPACITY;
			Graphics2D g2d = (Graphics2D)g;
			g2d.setColor(c);
			AffineTransform saveTransform = g2d.getTransform();
			Composite saveComposite = null;
			if (drawInTransparency) {
			    saveComposite = g2d.getComposite();
                float opacityLevel = Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL*s.plan.getOpacityLevel();
                Composite myComposite = Util.getImageComposite(opacityLevel);
                g2d.setComposite(myComposite);
			}
			// convert the angle into radians
			angle = angle*Math.PI/180.0;
			g2d.rotate(angle, center.x+dx, center.y+dy);
			if (drawInTransparency) {
			    g2d.fill(new Ellipse2D.Double(center.x+dx-semiMA,center.y+dy-semiMI,semiMA*2,semiMI*2));
			}
			else {
			    g2d.draw(new Ellipse2D.Double(center.x+dx-semiMA,center.y+dy-semiMI,semiMA*2,semiMI*2));
			}
			g2d.setTransform(saveTransform);
			if (drawInTransparency) {
			    g2d.setComposite(saveComposite);
			}
		}
		catch(ClassCastException cce) {
			// in this case, we draw the ellipse the old way
			doDrawEllipseOld(g, c, center, semiMA, semiMI, angle, dx, dy);
		}
		catch(Exception e) {e.printStackTrace();}
	}

	private void doDrawEllipseOld(Graphics g, Color c, Point center, double semiMA, double semiMI, double angle, int dx, int dy) {
		// convert the angle into radians
		angle = angle*Math.PI/180.0;

		// number of iterations
		int nbIt = 30;
		Point[] p = new Point[nbIt];
		double x,y,tmpX,tmpY;
		double curAngle;

		// first, we fill the array
		for(int i=0; i<nbIt; i++) {
			curAngle = 2.0*i/nbIt*Math.PI;
			tmpX = semiMA*Math.cos(curAngle);
			tmpY = semiMI*Math.sin(curAngle);
			// rotation
			x = tmpX*Math.cos(angle)-tmpY*Math.sin(angle)+center.x;
			y = tmpX*Math.sin(angle)+tmpY*Math.cos(angle)+center.y;

			// prise en compte du décalage dx, dy (pour l'impression)
			x += dx;
			y += dy;

			//System.out.println(x+" "+y);
			p[i] = new Point((int)x,(int)y);
		}

		g.setColor(c);
		// then we draw
		for(int i=0; i<nbIt-1; i++) {
			g.drawLine(p[i].x,p[i].y,p[i+1].x,p[i+1].y);
		}
		// complete the ellipse
		g.drawLine(p[nbIt-1].x,p[nbIt-1].y,p[0].x,p[0].y);
	}



    /** counts the number of occurences of a char in a string
     *	@param c - the char
     *	@param s - the string we search in
     *	@return the number of occurences of c in s
     */
    static protected int countNbOcc(char c, String s) {
    	int nb=0;
    	for(int i=0;i<s.length();i++) {
    	    if(s.charAt(i) == c) nb++;
    	}
    	return nb;
    }

    private void computeSize(Source s, int numero, int index) {
        /*
    	//System.out.println("dans computeSize");
      	double value;

		if( ! setAllVariables(sizeParser, s, false) ) return;
    	value = sizeParser.eval();

    	// if the value is constant
		if( sizeParser.isConstant() ) {
			s.values[numero][index][0] = value;
			return;
		}

		// ce genre de situtations peut se produire lorsque {min,max}Value ont ete fixes par l'utilisateur
		// dans ce cas, on fixe la taille à la valeur de l'extremum
		if( value>maxValue ) value = maxValue;
		else if( value<minValue ) value=minValue;

        s.values[numero][index][0] = minRadius+(value-minValue)*(maxRadius-minRadius)/(maxValue-minValue);

		//System.out.println("Mag : "+value+"\tValeur : "+s.values[index][0]);
        */
    }

    private void finalComputeSize(Source s, int numero, int index) {
        //System.out.println("dans finalComputeSize");
        double value;

        if( ! setAllVariables(sizeParser, s, false) ) return;
        value = sizeParser.eval();

        // if the value is constant
        if( sizeParser.isConstant() ) {
            s.values[numero][index][0] = value;
            return;
        }

        // ce genre de situtations peut se produire lorsque {min,max}Value ont ete fixes par l'utilisateur
        // dans ce cas, on fixe la taille à la valeur de l'extremum
        if( value>maxValue ) value = maxValue;
        else if( value<minValue ) value=minValue;
        s.values[numero][index][0] = minRadius+(value-minValue)*(maxRadius-minRadius)/(maxValue-minValue);

        //System.out.println("Mag : "+value+"\tValeur : "+s.values[index][0]);
    }

    /**
     * Calcule d'éventuels extremums après avoir choisi les sources !
     * @param sources les sources sélectionnées pour cette action
     */
    protected void computeExtremum(Source[] sources) {
        if( shape.equals(SIZE) || shape.equals(FILLSIZE) || shape.equals(FIXEDCIRCLE) ) {
            computeMinMax(sources);
        }
    }

    /** compute some values which will be stored in the source object
     *	@param s - the source
     *  @param numero - numero du filtre
	 *	@param index - index of the current action in s.actions array
     */
    protected void computeValues(Source s, int numero, int index) {
		// remise a "zero" des values
        //if(s.values==null) System.out.println("s.values est null");
        //if(s.values[numero]==null) System.out.println("s.values[numero] est null");
        //if(s.values[numero][index]==null) System.out.println("s.values[numero][index] est null");
    	s.values[numero][index][0]=s.values[numero][index][1]=s.values[numero][index][2]=s.values[numero][index][3]=-1;

		if( shape.equals(SIZE) || shape.equals(FILLSIZE) || shape.equals(FIXEDCIRCLE) ) {
            // plus pour l'instant
            /*
    	    if( mustComputeMinMax ) {
      	    	computeMinMax();
      	    	mustComputeMinMax = false;
      	    }
            */
    	    computeSize(s, numero, index);
    	}
    	else if( shape.equals(ELLIPSE) || shape.equals(FILLELLIPSE) ) {
    	    computeEllipse(s, numero, index);
		}
    	else if( shape.equals(RECTANGLE) ) {
    	    computeRectangle(s, numero, index);
		}
		else if( shape.equals(PM) ) {
			computePM(s, numero, index);
		}
		else if( shape.equals(LINE) ) {
		    computeLine(s, numero, index);
		}
    }

    /** compute at the end some values which will be stored in the source object
     *  @param s - the source
     *  @param numero - numero du filtre
     *  @param index - index of the current action in s.actions array
     */
    protected void finalcomputeValues(Source s, int numero, int index) {
        if( shape.equals(SIZE) || shape.equals(FILLSIZE) || shape.equals(FIXEDCIRCLE) ) {
            finalComputeSize(s, numero, index);
        }
    }

    // Reenclenche l'action (recalcul des min/max)
    protected void reset() {
        //System.out.println("Reset au niveau Action");
        if( !userDefinedMinMax ) mustComputeMinMax = true;
	    mustComputeRGBMinMax = true;
	    if( !userDefinedRainbowMinMax) mustComputeRainbowMinMax = true;
	    if( !userDefinedSaturationMinMax ) mustComputeSaturationMinMax = true;
    }

    /** searches the closing parenthesis ')' of an action
     *
     *  @param s - the string
     *  @param begin - index where one begins to search
     *  @return the position of ')' in s, skipping the () expressions,
        -1 if not found
     */
    private int getClosingParenthesis(String s, int begin) {
        int result;
        int oBegin = begin;
        String subStr;
        while(true) {
            result = s.indexOf(")",begin);
            if( result==-1 ) return -1;

            subStr = s.substring(oBegin,result+1);
            //System.out.println(subStr);
            if( Action.countNbOcc('(', subStr)+1 == Action.countNbOcc(
')',subStr) ) return result;
            else begin = result+1;
        }
    }



	/** set all variables of a parser with values of a given source
	 *	@param parser - the parser
	 *	@param s - the source from which values are taken
	 *	@param cont - if true, we continue even if a variable was not found. If false, we stop and return false
	 *	@param setUnit - true if we have to set the units of the variables
	 *	@return true if all variables found and cont==false. If cont is set to true, it always returns true
	 */
	protected static boolean setAllVariables(Parser parser, Source s, boolean cont, boolean setUnit) {
		int pos;
    	Iterator iterVar;
    	String curVar;
    	// recuperation de toutes les variables
    	iterVar = parser.getVariables();

    	// set all variables
    	// remarque : il faudra faire les conversions entre differentes unites des variables (argument a ajouter a la methode)
    	while(iterVar.hasNext()) {
    	    curVar = (String)iterVar.next();

    	    // case of a UCD
            if (curVar.startsWith("[")) {
    	    	if( (pos=s.findUCD(curVar.substring(1,curVar.length()-1).toUpperCase())) < 0) {if(cont) continue; else return false;}
            }
    	    // case of a column
    	    else {
    	    	if( (pos=s.findColumn(curVar.substring(1,curVar.length()-1))) < 0) {if(cont) continue; else return false;}
    	    }


			// setting the value of the variable
    	    try {
    	    	parser.setVar(curVar, Double.valueOf(s.getValue(pos)).doubleValue());
    	    }
    	    catch(NumberFormatException exc) {if(cont) continue; else return false;}
			// patch provisoire, a modifier (bug d'un field sans valeur associée)
			catch(NullPointerException npExc) {if(cont) continue; else return false;}

			// setting unit of the variable
			if( setUnit ) {
				if( ! parser.setVarUnit(curVar, s.getUnit(pos)) ) {
					// if there was a problem during the unit set
					return false;
				}
			}
    	}
		return true;
	}


	/** setAllVariables */
	protected static boolean setAllVariables(Parser parser, Source s, boolean cont) {
		return setAllVariables(parser,s,cont,false);
	}

}
