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

package cds.aladin.stc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.aladin.stc.STCObj.ShapeType;

public class STCStringParser {
	public final int mandatoryStcCircleWords = 5;
    public STCStringParser() {}
    
    public List<STCObj> parse(String stcString) {
        return parse(stcString, false);
    }

    /**
     * 
     * @param stcString
     * @param reduced - signifies Siav2 and SODA input params which are similar to STC string except without reference positions, coordinate systems, units, or geometric operators (union,
		intersection, not).
     * @return
     */
    public List<STCObj> parse(String stcString, boolean reduced) {
        stcString = stcString.toUpperCase();
        List<STCObj> stcObjs = new ArrayList<>();

        String[] shapesStrs = splitShapesStrings(stcString, reduced);
        for (String shapeStr : shapesStrs) {
        	List<String> stcWords = Arrays.asList(shapeStr.split("[ \t]+", -1));
            Iterator<String> itWords  = stcWords.iterator();
            String curWord;
            while (itWords.hasNext()) {
                curWord = itWords.next();
                try {
                	if (curWord.equals("POLYGON")) {
                		stcObjs.add(parsePolygon(itWords, reduced));
                	} else if (curWord.equals("CIRCLE") && (reduced || stcWords.size() == mandatoryStcCircleWords)) {
                		stcObjs.add(parseCircle(itWords, reduced));
    				}
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return stcObjs;
    }

    /**
     * sépare la chaîne STC en sous-chaînes, une par "forme"
     * @param stcString
     * @return
     */
    private String[] splitShapesStrings(String stcString, boolean reduced) {
        ArrayList<String> result = new ArrayList<>();

        String shapes = new String("(");
        for (ShapeType shapeType : STCObj.ShapeType.values()) {
            shapes += shapeType.name() + "|";
        }
        shapes = shapes.substring(0, shapes.length() - 1)+")";

        String regexp = new String(shapes);
        //regexp += "( +[A-Za-z0-9]+)( +[-]?[0-9\\.]+)+";
        if (reduced) {
        	regexp +="(\\s+[A-Za-z0-9]+)?(\\s+[-]?[0-9\\.]+)+";
//        	regexp +="(\\s+[A-Za-z0-9]+)?(\\s+[-]?[0-9\\.|+Inf|Inf]+)+";
		} else {
			regexp +="(\\s+[A-Za-z0-9]+)+(\\s+[-]?[0-9\\.]+)+";
		}
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(stcString);
        while (m.find()) {
            result.add(m.group());
        }

        return result.toArray(new String[result.size()]);
    }
    
    // AJOUT Pierre 2/5/2019
    private double getDouble(String s) {
       return Double.parseDouble(s);
    }

    private STCPolygon parsePolygon(Iterator<String> itWords, boolean reduced) throws Exception {
        STCPolygon polygon = new STCPolygon();
        if (!reduced) {
        	polygon.setFrame(STCFrame.valueOf(itWords.next()));
		} else {
			polygon.setFrame(STCFrame.ICRS);
		}
        while (itWords.hasNext()) {
            double ra, dec;
            ra = dec = Double.NaN;
            String nextParam;
            try {
            	nextParam = itWords.next();
                if (!isNumber(nextParam)) {// to ignore all strings [<refpos>] [<flavor>] which are not handled currently.
                	continue;
                }
            	ra = getDouble(nextParam);
            	dec = getDouble(itWords.next());// any words between numbers is unexpected and hence exception
				
            }
            catch(Exception e) {
                e.printStackTrace();
                continue;
            }
            polygon.addCorner(ra, dec);
        }
        return polygon;
    }
    
    private STCObj parseCircle(Iterator<String> stcWords, boolean reduced) {
		STCCircle circle = null;
		STCFrame frame = null;
		if (!reduced) {
			frame = STCFrame.valueOf(stcWords.next());
		} else {
			frame = STCFrame.ICRS;
		}
		double ra, dec, rad;
		while (stcWords.hasNext()) {
            ra = dec = Double.NaN;
            String nextParam;
            try {
            	nextParam = stcWords.next();
                if (!isNumber(nextParam)) {// to ignore all strings [<refpos>] [<flavor>] which are not handled currently.
                	continue;
                }
            	ra = getDouble(nextParam);
            	dec = getDouble(stcWords.next());// any words between numbers is unexpected and hence exception
            	rad = getDouble(stcWords.next());
            }
            catch(Exception e) {
                e.printStackTrace();
                continue;
            }
            circle = new STCCircle(frame, ra, dec, rad);
            break;
        }
		return circle;
	}
    
    /**
     * Method to check for presense of any alphabet
     * @param input
     * @return
     */
    public boolean isNumber(String input) {
    	String regexp = "[A-Za-z]";
    	Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(input);
        return !m.find();
	}

    public static void main(String[] args) {
        STCStringParser parser = new STCStringParser();
        List<STCObj> stc = null;
        stc = parser.parse("Polygon   ICRS   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566  Polygon   J2000   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566");
        stc = parser.parse("Polygon J2000 40.57741 0.07310 40.57741 0.06771 40.60596 -0.06867 40.60597 -0.06868 40.61360 -0.06868 40.74998 -0.04013 40.74999 -0.04012 40.74999 -0.03473 40.72144 0.10165 40.72142 0.10166 40.71380 0.10166 40.57742 0.07311");
        stc =  parser.parse("Polygon 9.4996719542 8.6262877169 9.4996724321 8.6457321609 9.4780377021 8.6457320742 9.4780383392 8.6262876304");
    }
}
