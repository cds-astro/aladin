package cds.aladin.stc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.aladin.stc.STCObj.ShapeType;

public class STCStringParser {
    public STCStringParser() {}

    public List<STCObj> parse(String stcString) {
        stcString = stcString.toUpperCase();
        List<STCObj> stcObjs = new ArrayList<STCObj>();

        String[] shapesStrs = splitShapesStrings(stcString);
        for (String shapeStr : shapesStrs) {
            Iterator<String> itWords  = Arrays.asList(shapeStr.split("[ \t]+", -1)).iterator();
            String curWord;
            while (itWords.hasNext()) {
                curWord = itWords.next();

                if (curWord.equals("POLYGON")) {
                    try {
                        stcObjs.add(parsePolygon(itWords));
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
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

        String shapes = new String();
        for (ShapeType shapeType : STCObj.ShapeType.values()) {
            shapes += shapeType.name() + "|";
        }
        shapes = shapes.substring(0, shapes.length() - 1);

        String regexp = new String(shapes);
        regexp += "( +[A-Za-z0-9]+)( +[-]?[0-9\\.]+)+";
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
            try {
                ra = Double.parseDouble(itWords.next());
                dec = Double.parseDouble(itWords.next());
            }
            catch(Exception e) {
                e.printStackTrace();
                continue;
            }
            polygon.addCorner(ra, dec);
        }
        return polygon;
    }

    public static void main(String[] args) {
        STCStringParser parser = new STCStringParser();
        parser.parse("Polygon   ICRS   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566  Polygon   J2000   211.115036    54.280565  211.115135    54.336616  210.971306    54.336617  210.971403    54.280566");
        parser.parse("Polygon J2000 40.57741 0.07310 40.57741 0.06771 40.60596 -0.06867 40.60597 -0.06868 40.61360 -0.06868 40.74998 -0.04013 40.74999 -0.04012 40.74999 -0.03473 40.72144 0.10165 40.72142 0.10166 40.71380 0.10166 40.57742 0.07311");
    }
}
