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
        stcString = stcString.toUpperCase();
        List<STCObj> stcObjs = new ArrayList<STCObj>();

        String[] shapesStrs = splitShapesStrings(stcString);
        for (String shapeStr : shapesStrs) {
        	List<String> stcWords = Arrays.asList(shapeStr.split("[ \t]+", -1));
            Iterator<String> itWords  = stcWords.iterator();
            String curWord;
            while (itWords.hasNext()) {
                curWord = itWords.next();
                try {
                	if (curWord.equals("POLYGON")) {
                		stcObjs.add(parsePolygon(itWords));
                	} else if (curWord.equals("CIRCLE") && stcWords.size() == mandatoryStcCircleWords) {
                		stcObjs.add(parseCircle(itWords));
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
    private String[] splitShapesStrings(String stcString) {
        ArrayList<String> result = new ArrayList<String>();

        String shapes = new String("(");
        for (ShapeType shapeType : STCObj.ShapeType.values()) {
            shapes += shapeType.name() + "|";
        }
        shapes = shapes.substring(0, shapes.length() - 1)+")";

        String regexp = new String(shapes);
        //regexp += "( +[A-Za-z0-9]+)( +[-]?[0-9\\.]+)+";
        regexp +="(\\s+[A-Za-z0-9]+)+(\\s+[-]?[0-9\\.]+)+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(stcString);
        while (m.find()) {
            result.add(m.group());
        }

        return result.toArray(new String[result.size()]);
    }

    private STCPolygon parsePolygon(Iterator<String> itWords) throws Exception {
        STCPolygon polygon = new STCPolygon();
        polygon.setFrame(STCFrame.valueOf(itWords.next()));
        while (itWords.hasNext()) {
            double ra, dec;
            ra = dec = Double.NaN;
            String nextParam;
            try {
            	nextParam = itWords.next();
                if (!isNumber(nextParam)) {// to ignore all strings [<refpos>] [<flavor>] which are not handled currently.
                	continue;
                }
            	ra = Double.parseDouble(nextParam);
            	dec = Double.parseDouble(itWords.next());// any words between numbers is unexpected and hence exception
				
            }
            catch(Exception e) {
                e.printStackTrace();
                continue;
            }
            polygon.addCorner(ra, dec);
        }
        return polygon;
    }
    
    private STCObj parseCircle(Iterator<String> stcWords) {
		STCCircle circle = new STCCircle(STCFrame.valueOf(stcWords.next()), stcWords.next(), stcWords.next(), stcWords.next());
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
        parser.parse("Polygon   ICRS   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566  Polygon   J2000   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566");
        parser.parse("Polygon J2000 40.57741 0.07310 40.57741 0.06771 40.60596 -0.06867 40.60597 -0.06868 40.61360 -0.06868 40.74998 -0.04013 40.74999 -0.04012 40.74999 -0.03473 40.72144 0.10165 40.72142 0.10166 40.71380 0.10166 40.57742 0.07311");
    }
}
