/**
 * 
 */
package cds.aladin;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import cds.aladin.stc.STCFrame;
import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCPolygon;
import cds.aladin.stc.STCStringParser;
import cds.astro.Astrotime;
import cds.astro.Unit;
import cds.moc.HealpixMoc;
import cds.tools.Astrodate;
import cds.tools.ConfigurationReader;
import cds.tools.pixtools.CDSHealpix;
import healpix.essentials.Moc;
import healpix.essentials.MocQuery;
import healpix.essentials.Pointing;
import healpix.essentials.Vec3;

/**
 * @author chaitra
 *
 */
public class AAtintin{
	// prep the param resource
	// create Glu from xml
//	ServerGlu glu= new ServerGlu(aladin, actionName, description, verboseDescr, aladinMenu,
//            aladinMenuNumber, aladinLabel, aladinLabelPlane, docUser, paramDescription, paramDataType, paramValue,
//            resultDataType, institute, aladinFilter, aladinLogo, dir, system, record, aladinProtocol);
	//2012-01-26T03:36:48
	
	private static final Map<String, String> DATE_FORMATS = new HashMap();
	private static final String MJD_REGEX = "^\\d{1,5}$";
	
	static {
		DATE_FORMATS.put("^\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy");
		DATE_FORMATS.put("^\\d{1,2}-[a-zA-Z]{3}-\\d{4}", "dd-MMM-yyyy");
		DATE_FORMATS.put("^\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd");
		DATE_FORMATS.put("^\\d{4}-[a-zA-Z]{3}-\\d{1,2}", "yyyy-MMM-dd");
	}
	
	public static Vector<TapTableColumn> getColumns() {
		Vector<String> columnnames = new Vector<String>();
		columnnames.addElement("num_tuples");
		columnnames.addElement("max_time_bounds_cval1");
		columnnames.addElement("collection");
		columnnames.addElement("telescope_name");
		columnnames.addElement("instrument_name");
		columnnames.addElement("type");
		columnnames.addElement("intent");
		columnnames.addElement("dataProductType");
		columnnames.addElement("calibrationLevel");
		columnnames.addElement("energy_emBand");
		columnnames.addElement("energy_bandpassName");
		TapTableColumn column = null;
		Vector<TapTableColumn> columns = new Vector<TapTableColumn>();
		for (String columnname : columnnames) {
			column = new TapTableColumn();
			column.setColumn_name(columnname);
			columns.addElement(column);
		}
		return columns;
	}
	
	public static Vector<String> getColumnsStrings() {
		Vector<String> columnnames = new Vector<String>();
		columnnames.addElement("num_tuples");
		columnnames.addElement("max_time_bounds_cval1");
		columnnames.addElement("collection");
		columnnames.addElement("telescope_name");
		columnnames.addElement("instrument_name");
		columnnames.addElement("type");
		columnnames.addElement("intent");
		columnnames.addElement("dataProductType");
		columnnames.addElement("calibrationLevel");
		columnnames.addElement("energy_emBand");
		columnnames.addElement("energy_bandpassName");
		return columnnames;
	}
	
	public void raDecTester(TapTable tapTable) {
		// TODO Auto-generated method stub
		
		Vector<TapTableColumn> columns = tapTable.getColumns();
		JComboBox raColumn = new JComboBox(columns);
		raColumn.setRenderer(new TapTableColumnRenderer());
		raColumn.setSize(raColumn.getWidth(), Server.HAUT);
		JComboBox decColumn = new JComboBox(columns);
		decColumn.setRenderer(new TapTableColumnRenderer());
		decColumn.setSize(decColumn.getWidth(), Server.HAUT);
		
		Object[] raAndDec = {
			    "ra:", raColumn,
			    "dec:", decColumn
			};
			
			
			int option = JOptionPane.showConfirmDialog(null , raAndDec, "Set ra and dec", JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.OK_OPTION) {
				String ra = ((TapTableColumn) raColumn.getSelectedItem()).getColumn_name();
				String dec = ((TapTableColumn) decColumn.getSelectedItem()).getColumn_name();
				System.out.println(ra+" "+dec);
			}
	}
	
	public static void main(String[] args) {
		boolean addTargetPanel = false;
		
		Vector<TapTableColumn> columns = getColumns();
		JComboBox raColumn = new JComboBox(columns);
		raColumn.setRenderer(new TapTableColumnRenderer());
		raColumn.setSize(raColumn.getWidth(), Server.HAUT);
		JComboBox decColumn = new JComboBox(columns);
		decColumn.setRenderer(new TapTableColumnRenderer());
		decColumn.setSize(decColumn.getWidth(), Server.HAUT);
    	
		Object[] raAndDec = {
		    "ra:", raColumn,
		    "dec:", decColumn
		};
		
		
		int option = JOptionPane.showConfirmDialog(null , raAndDec, "Set ra and dec", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			String ra = ((TapTableColumn) raColumn.getSelectedItem()).getColumn_name();
			String dec = ((TapTableColumn) decColumn.getSelectedItem()).getColumn_name();
			System.out.println(ra+" "+dec);
		}
	}
	
	public static void mainForSimpleJOptionPane(String[] args) {
		Vector<String> columns = new Vector<String>();
		for (int i = 0; i < 100; i++) {
			columns.addElement("tintin"+i);
		}
		JComboBox raColumn = new JComboBox(columns);
		raColumn.setSize(raColumn.getWidth(), Server.HAUT);
		JComboBox decColumn = new JComboBox(columns);
		decColumn.setSize(decColumn.getWidth(), Server.HAUT);
    	
		Object[] raAndDec = {
		    "ra:", raColumn,
		    "dec:", decColumn
		};
		int option = JOptionPane.showConfirmDialog(null , raAndDec, "Set ra and dec", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			String selectedRa = (String) raColumn.getSelectedItem();
			String selectedDec = (String) decColumn.getSelectedItem();
			System.out.println(selectedRa+" "+selectedDec);
		}
	}
	
	public static void mainForCoord(String[] args) {
		double RA5=78.06308333333332;
		double DEC5=-13.000333333333332;
		 Coord c1 = new Coord(RA5,DEC5); 
		 System.out.println("input: "+RA5+"  "+DEC5);
		 System.out.println("output: "+c1.al+"  "+c1.del);
	}
	
	public static void mainforMoc(String[] args) {
		//inside the circle
		double RA5=78.06308333333332;
		double DEC5=-13.000333333333332;
		double RAduis5=0.5936666666666667;
		double RAdians5=0.010361437992673004;
		//Draw circle (78.06308333333332, -13.000333333333332, 0.5936666666666667)
		
		//outside circle
		double RA2=86.08095833333334;
		double DEC2=-7.9502500000000005;
		double RAduis2=1.3;
		//Draw circle (86.08095833333334,-7.9502500000000005,1.3)
		
		//totally outside circle
				double RA4=60.37;
				double dec4=-10.9;
				double raduis4= 0.89;
		
		//not working stc
		String stc="Polygon ICRS UNKNOWNREFPOS SPHERICAL2 86.99090120499412 -16.10035084469397 74.10746861021282 -16.03801023309523 74.40891177132114 -3.6639367763034927 86.81225899553208 -3.7233925881787573";
		//Draw Polygon(86.99090120499412 ,-16.10035084469397 ,74.10746861021282 ,-16.03801023309523 ,74.40891177132114 ,-3.6639367763034927 ,86.81225899553208 ,-3.7233925881787573)
		String anticlockstc = "Polygon ICRS UNKNOWNREFPOS SPHERICAL2  86.81225899553208 -3.7233925881787573 74.40891177132114 -3.6639367763034927 74.10746861021282 -16.03801023309523 86.99090120499412 -16.10035084469397";
		//draw ting colse to the above stc works for this one
		//String stc4= "Polygon ICRS 86.77654551805824 -3.7105707900324165 74.45871727211944 -3.722555907311696 74.0868453737034 -16.02363591738269 86.88082741416824 -16.127943780240773 86.77654551805824 -3.7105707900324165";
		//Draw polygon(86.77654551805824,-3.7105707900324165,74.45871727211944,-3.722555907311696,74.0868453737034,-16.02363591738269,86.88082741416824,-16.127943780240773,86.77654551805824,-3.7105707900324165)
		String stc4 = "Polygon ICRS 86.77654551805824 -3.7105707900324165 74.45871727211944 -3.722555907311696 74.0868453737034 -16.02363591738269 86.88082741416824 -16.127943780240773";
		
		
		//working stc
		String stc2="Polygon ICRS UNKNOWNREFPOS SPHERICAL2 83.2668638028998 -5.843302823184818 74.6952366806686 -5.633617804852619 75.20463326372128 -15.000330031872647 84.02594118517658 -15.063054296657345 83.2668638028998 -5.843302823184818";
		//Draw Polygon (83.2668638028998 ,-5.843302823184818 ,74.6952366806686 ,-5.633617804852619 ,75.20463326372128 ,-15.000330031872647 ,84.02594118517658 ,-15.063054296657345, 83.2668638028998, -5.843302823184818)

		
		//another stc
		String stc3="Polygon ICRS UNKNOWNREFPOS SPHERICAL2 96.99118974854264 -16.148853319545047 84.10462779535877 -16.08560661179259 84.4081960624216 -3.7115790777208817 96.81222142388407 -3.771895783146152 96.99118974854264 -16.148853319545047";
		
		//String stc3polyoutside ="Polygon ICRS 66.12899416422688 -9.591386886578302 62.10292466185209 -9.258445462426243 61.806518930340594 -11.65733184248849 65.86061953765973 -12.009181953807 66.12899416422688 -9.591386886578302";
		
		
		try {
			HealpixMoc healpixMoc = createMocRegionPol(anticlockstc);
			HealpixMoc healpixMoc6 = createMocRegionCircle(RA5, DEC5, RAduis5);//inside circle 
			HealpixMoc healpixMoc5 = createMocRegionCircle(RA2, DEC2, RAduis2);//outside circle
			HealpixMoc healpixMoc8 = createMocRegionCircle(RA4, dec4, raduis4);//totoally outside circle
			//HealpixMoc healpixMoc7 = createMocRegionPol(stc3polyoutside);//outside poly 
			
			System.out.println("circle inside: "+healpixMoc.rangeSet.containsAll(healpixMoc6.rangeSet));
			System.out.println("circle outside: "+healpixMoc.rangeSet.containsAll(healpixMoc5.rangeSet));
			System.out.println("totally circle outside: "+healpixMoc.rangeSet.containsAll(healpixMoc8.rangeSet));
			//System.out.println("poly outside: "+healpixMoc.rangeSet.containsAll(healpixMoc7.rangeSet));
			
			
			//System.out.println("circle inside the polygon: "+healpixMoc.rangeSet.containsAll(healpixMoc4.rangeSet));
			//System.out.println("circle outside the polygon: "+healpixMoc.rangeSet.containsAll(healpixMoc5.rangeSet));
			
			
			//System.out.println("Draw circle ("+RA+","+DEC+","+RAduis+")");
//			String stcresult = "Draw moc("+(healpixMoc.getMocOrder()+1)+"/"+healpixMoc.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
//			System.out.println(stcresult.replaceAll("[\\{\\}]", ""));
//			
//			String aladincercilr = "Draw moc("+(healpixMoc2.getMocOrder()+1)+"/"+healpixMoc2.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
//			System.out.println(aladincercilr.replaceAll("[\\{\\}]", ""));
			System.out.println("----");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public static void mainMocTesting1(String[] args) {
		//inside the circle
		double RA5=78.06308333333332;
		double DEC5=-13.000333333333332;
		double RAduis5=0.5936666666666667;
		double RAdians5=0.010361437992673004;
		
		
		double RA=83.894125;
		double DEC=-11.928666666666665;
		double RAduis=2.73;
		double RAdians=0.047647488579445195;
		
		
		double RA2=86.08095833333334;
		double DEC2=-7.9502500000000005;
		double RAduis2=1.3;
		
		//these ones are inside the circle
		double RA3=84.09174999999999;
		double DEC3=-10.977944444444445;
		double RAduis3=0.8985;
		double RAdians3=0.01568178332916905;
		
		//these circel are outside 
		double RA4=60.37;
		double dec4=-10.9;
		double raduis4= 0.89;
		
		String stc="Polygon ICRS 83.2668638028998 -5.843302823184818 74.6952366806686 -5.633617804852619 75.20463326372128 -15.000330031872647 84.02594118517658 -15.063054296657345 83.2668638028998 -5.843302823184818";
		String stc2smallerInsideBigger="Polygon ICRS 81.90068953545865 -8.767227193327788 78.30695176283383 -8.851167873758229 78.37033742788361 -11.988678099919532 81.53013092021548 -11.36513555807756 81.90068953545865 -8.767227193327788";
		String stc3SmallerOutsideBigger = "Polygon ICRS 93.30291603189944 0.19018493062669528 87.81345577249154 0.3896957819002509 88.21684181081507 -2.76640235466537 92.97490972303262 -2.7055271591768335 93.30291603189944 0.19018493062669528";
		String stc3polyoutside ="Draw polygon(66.12899416422688,-9.591386886578302,62.10292466185209,-9.258445462426243,61.806518930340594,-11.65733184248849,65.86061953765973,-12.009181953807,66.12899416422688,-9.591386886578302)";

		try {
			HealpixMoc healpixMoc2 = createMocRegionPol(stc2smallerInsideBigger);
			HealpixMoc healpixMoc21 = createMocRegionPol(stc3polyoutside);
			HealpixMoc healpixMoc = createMocRegionPol(stc);
			HealpixMoc healpixMoc3 = createMocRegionCircle(RA2, DEC2, RAduis);//outside circle 
			HealpixMoc healpixMoc6 = createMocRegionCircle(RA5, DEC5, RAduis5);//outside circle 
			
			
			
//			HealpixMoc healpixMoc4 = createMocRegionCircle(RA3, DEC3, RAduis3);//inside circle
//			HealpixMoc healpixMoc5 = createMocRegionCircle(RA4, dec4, raduis4);//inside circle
			
			System.out.println("poly inside: "+healpixMoc.rangeSet.containsAll(healpixMoc2.rangeSet));
			System.out.println("poly outside: "+healpixMoc.rangeSet.containsAll(healpixMoc2.rangeSet));
			System.out.println("circle outside: "+healpixMoc.rangeSet.containsAll(healpixMoc3.rangeSet));
			System.out.println("circle inside: "+healpixMoc.rangeSet.containsAll(healpixMoc6.rangeSet));
			System.out.println("poly outside: "+healpixMoc.rangeSet.containsAll(healpixMoc21.rangeSet));
			
			
			//System.out.println("circle inside the polygon: "+healpixMoc.rangeSet.containsAll(healpixMoc4.rangeSet));
			//System.out.println("circle outside the polygon: "+healpixMoc.rangeSet.containsAll(healpixMoc5.rangeSet));
			
			
			//System.out.println("Draw circle ("+RA+","+DEC+","+RAduis+")");
//			String stcresult = "Draw moc("+(healpixMoc.getMocOrder()+1)+"/"+healpixMoc.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
//			System.out.println(stcresult.replaceAll("[\\{\\}]", ""));
//			
//			String aladincercilr = "Draw moc("+(healpixMoc2.getMocOrder()+1)+"/"+healpixMoc2.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
//			System.out.println(aladincercilr.replaceAll("[\\{\\}]", ""));
			System.out.println("tintin");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	protected static HealpixMoc createMocRegionPol(String stc) throws Exception {
	      HealpixMoc moc=null;
	      String stcresult = "Draw polygon(";
	      double maxSize=0;
	      Coord c1=null;
	      boolean first=true;
	      int order=0;
	      double firstRa = 0.0d,firstDec = 0.0d;
	      
	      STCStringParser parser = new STCStringParser();
		  List<STCObj> stcObjects = parser.parse(stc);
		  if (stcObjects != null) {
	      for( int sens=0; sens<2; sens++ ) {
	         ArrayList<Vec3> cooList = new ArrayList<Vec3>();
	         if( sens==1 ) System.out.println("createMocRegion("+stc+") trying reverse polygon order...");
	         try {
               for (STCObj stcObj : stcObjects) {
                   if ( stcObj.getShapeType() != STCObj.ShapeType.POLYGON ) {
                       continue;
                   }

                   STCPolygon stcPolygon = (STCPolygon)stcObj;
                   STCFrame frame = stcPolygon.getFrame();
                   // currently, we only support FK5, ICRS and J2000 frames
                   if ( ! (frame==STCFrame.FK5 || frame==STCFrame.ICRS || frame==STCFrame.J2000)) {
                       continue;
                   }
                   
					for (int i = 0; i < stcPolygon.getxCorners().size(); i++) {
						if (first) {
							firstRa = stcPolygon.getxCorners().get(i);
							firstDec = stcPolygon.getyCorners().get(i);
							c1 = new Coord(firstRa, firstDec);
							first = false;
						} else {
							double size = Coord.getDist(c1,
									new Coord(stcPolygon.getxCorners().get(i), stcPolygon.getyCorners().get(i)));
							if (size > maxSize)
								maxSize = size;
						}
						
						stcresult=stcresult+stcPolygon.getxCorners().get(i)+","+stcPolygon.getyCorners().get(i)+",";
						double theta = Math.PI / 2 - Math.toRadians(stcPolygon.getyCorners().get(i));
						double phi = Math.toRadians(stcPolygon.getxCorners().get(i));
						cooList.add(new Vec3(new Pointing(theta, phi)));
					}
					
					
					double theta = Math.PI / 2 - Math.toRadians(firstDec);
					double phi = Math.toRadians(firstRa);
					cooList.add(new Vec3(new Pointing(theta, phi)));
               }

	            if( sens==0 ) {
	               // L'ordre est déterminé automatiquement par la largeur du polygone
	               order=getAppropriateOrder(maxSize);
	               System.out.println("MocRegion generation:  maxRadius="+maxSize+"deg => order="+order);
	               if( order<10 ) order=10;
	               else if( order>29 ) order=29;
	            }

	            Moc m=MocQuery.queryGeneralPolygonInclusive(cooList,order,order+4>29?29:order+4);
	            moc = new HealpixMoc();
	            moc.rangeSet = m.getRangeSet();
	            moc.toHealpixMoc();

	            // moins de la moitié du ciel => ca doit être bon
	            if( moc.getCoverage()<0.5 ) break;

	            // On va essayer dans l'autre sens avant d'estimer que ça ne fonctionne pas
	         } catch( Throwable e ) {
	            if( sens==1 && e instanceof Exception ) throw (Exception)e;
	         }
	      }
		  }
		  
		  stcresult=stcresult.replaceAll(",$", ")");
		  System.out.println(stcresult.replaceAll("[\\{\\}]", ""));


	      return moc;
	   }
	
	public static void mocBlunders_circle(String[] args) {
	    //String input = "2016 Sep 28 20:29";
		//String input = "2012-01-26 12:30:10";System.out.println(input);
	    //getDateInMJDFormat(input);

		/*double ra = 80.59991;
		double dec = -9.94;
		double radius = 17.539;
		
		double ra2 = 10.59991;
		double dec2 = 23.94;
		double radius2 = 50.539;*/
		double ra= 119.52038939062832;double dec= 43.59344215450867;double radius= 0.8826123227270523;
		double ra2=114.21482909870261;double dec2=114.21482909870261;double radius2=114.21482909870261;
		double ra21=88.6549166666666;double dec21=-6.031222222222222;double radius21=0.3972369377539094;
		double RA=86.08095833333334;
		double DEC=-7.9502500000000005;
		double RAduis=27.3;
		double RAdians=0.47647488579445196;


		try {
			//healpix.queryDisc(2, 80.59991, -9.94, 17.539);
			//hpxBase[2].queryDiscInclusive(CDSHealpix.pointing(ra,dec),radius,4);
			long[] rangeSet1 = CDSHealpix.query_disc(8, ra, dec, radius, true);
			long[] rangeSet2 = CDSHealpix.query_disc(8, ra21, dec21, radius21, true);
			System.out.println(rangeSet1.hashCode());
			System.out.println(rangeSet2.hashCode());
			System.out.println(Arrays.toString(rangeSet1));
			System.out.println(Arrays.toString(rangeSet2));
			
			HealpixMoc healpixMoc = createMocRegionCircle(RA, DEC, RAduis);
			
			HealpixMoc healpixMoc2 = createMocRegionCircle(RA, DEC, 10.3);
			System.out.println("Results here tintin: "+healpixMoc.rangeSet.containsAll(healpixMoc2.rangeSet)); //circle works fine, but draw moc shows sumtin else.
			
			System.out.println("Draw circle ("+RA+","+DEC+","+RAduis+")");
			
			String result = "Draw moc("+(healpixMoc.getMocOrder()+1)+"/"+healpixMoc.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
			System.out.println(result.replaceAll("[\\{\\}]", ""));
			
			String result2 = "Draw moc("+(healpixMoc2.getMocOrder()+1)+"/"+healpixMoc2.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
			System.out.println(result2.replaceAll("[\\{\\}]", ""));
		    
			/*
			 String firstone = "8/329, 330, 331, 336, 344, 345, 346, 347, 353, 354, 356, 360, 368, 369, 370, 371";
			String secondone = "10/5, 7, 13, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 168, 169, 170, 171, 172, 173, 174, 175, 184, 185, 186, 187, 188, 189, 190, 191, 253, 255, 335, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 357, 359, 365, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 424, 425, 426, 427, 428, 429, 430, 431, 440, 441, 442, 443, 444, 445, 446, 447, 638, 639";
			System.out.println("------------------------");
			Tok st1 = new Tok(secondone);
		    String cmd1 = st1.nextToken();
		    System.out.println(cmd1);
			
			Tok st = new Tok(firstone);
		    String cmd = st.nextToken();
		    System.out.println(cmd);
			  
			  Range rangeSet1 = CDSHealpix.query_disc(8, ra, dec, radius, true);
			
			URL u = new URL("http://alasky.unistra.fr/MocServer/query?client_application=AladinDesktop*&hips_service_url=*&stc=Polygon+124.55544015510463+48.38841492342816+103.8742180423006+48.38841492342816+105.42669938353599+38.72004200601674+123.00295881386924+38.72004200601673");
			MyInputStream dis = Util.openStream(u);
			HealpixMoc moc = new HealpixMoc();
            if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
            else moc.readASCII(dis);
            moc.*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	 protected static HealpixMoc createMocRegionCircle(double ra, double dec, double radius) throws Exception {
	      HealpixMoc m = new HealpixMoc();
	      int order=getAppropriateOrder(radius);
	      
	      long i=0;
	      m.setCheckConsistencyFlag(false);
	      for( long pix : CDSHealpix.query_disc( CDSHealpix.pow2(order), ra, dec, Math.toRadians(radius)) ) {
	         m.add(order,pix);
	         i++;
	         if( i%10000L==0 ) m.checkAndFix();
	      }
	      m.setCheckConsistencyFlag(true);
	      m.toRangeSet();
	      return m;
	}
	 
	 private static int getAppropriateOrder(double size) {
	      int order = 4;
	      if( size==0 ) order=HealpixMoc.MAXORDER;
	      else {
	         double pixRes = size/75;
	         double degrad = Math.toDegrees(1.0);
	         double skyArea = 4.*Math.PI*degrad*degrad;
	         double res = Math.sqrt(skyArea/(12*16*16));
	         while( order<HealpixMoc.MAXORDER && res>pixRes) { res/=2; order++; }
	      }
	      return order;
	   }
	
	
	
	
	/**
	 * Method to parse date given in natural language.
	 * Parses date in the below formats only: the delimiters could include "-" or "/" or " "
	 * <ol><li>dd-MM-yyyy</li>
	 * <li>dd-MMM-yyyy</li>
	 * <li>yyyy-MM-dd</li>
	 * <li>yyyy-MMM-dd</li>
	 * </ol>
	 * 
	 * Including the combination resulting with time provided in <b>HH:mm</b> or <b>HH:mm:ss</b> formats.
	 * 
	 * @param input
	 * @return 
	 */
	public static Date parseDate(String input) {
		String dateFormat = null;
		Date date = null;
		input = input.trim();
	    
		if(input.contains(" ") || input.contains("/") || input.contains("-")){
			input = input.replaceAll("[\\s/-]+", "-");
			SimpleEntry<String, String> timeFormat = null;

			int hourMinDelimiter = input.indexOf(":");
			if (hourMinDelimiter != -1) {
				if (input.indexOf(":", hourMinDelimiter + 1)==-1) {
					timeFormat = new SimpleEntry<String, String>("-\\d{1,2}:\\d{1,2}$", "-HH:mm");
				} else {
					timeFormat = new SimpleEntry<String, String>("-\\d{1,2}:\\d{1,2}:\\d{1,2}$", "-HH:mm:ss");
				}
			}
			
			StringBuffer completeRegEx = null;
			for (String regExp : DATE_FORMATS.keySet()) {
				if (timeFormat != null) {
					completeRegEx = new StringBuffer(regExp).append(timeFormat.getKey());
					 if (input.matches(completeRegEx.toString())) {
						 dateFormat = DATE_FORMATS.get(regExp) + timeFormat.getValue();
						 break;
					}
				} else if(input.matches(regExp)){
					dateFormat = DATE_FORMATS.get(regExp);
					break;
				}
			}
		}
		
		System.out.println(dateFormat);
		DateFormat dateformat = new SimpleDateFormat(dateFormat);
		dateformat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    try {
			date =  dateformat.parse(input);
			System.out.println(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
		return date;
	}
	
	/**
	 * Method to process date to MJD
	 * @param input
	 * @return dateinMJD
	 */
	public static double getDateInMJDFormat(String input) {
		double result = 0.0d;
		Pattern p = Pattern.compile(MJD_REGEX);
		Matcher m = p.matcher(input);
		if (m.find()) {
			result = Double.parseDouble(input);
		} else {
			Date date = parseDate(input);
			result = ISOToMJD(date);
		}
		System.out.println(result);
		return result;
	}
	
	/**
	 * Method to convert date to MJD.
	 * @param date
	 * @return dateinMJD
	 */
	public static double ISOToMJD(Date date) {
		double result = 0.0d;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		result = Astrodate.dateToJD(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		System.out.println(cal.get(Calendar.YEAR)+" " +cal.get(Calendar.MONTH)+1+" " +cal.get(Calendar.DAY_OF_MONTH)+" " +
				cal.get(Calendar.HOUR_OF_DAY)+" " +cal.get(Calendar.MINUTE)+" " +cal.get(Calendar.SECOND));
		result = Astrodate.JDToMJD(result);
		return result;
	}
	public static void mainForNewMJD(String[] args) throws Exception {
	    String target = "-0.002";
	   String onlydigistswithExponent = "^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$";
	 // String to be scanned to find the pattern.
      Pattern p = Pattern.compile(onlydigistswithExponent);
      Matcher m = p.matcher(target);
      if (m.find()) {
    	  System.out.println("MJD!"+target);
      }
      System.err.println(Double.parseDouble(target));
      
	}
	
	public static void mainMjd(String[] args) throws Exception {
	    String target = "2016 Sep 28 20:29:30";
	    target = target.trim();
	    
	 // String to be scanned to find the pattern.
      System.out.println(target);
      
      Pattern p = Pattern.compile(MJD_REGEX);
      Matcher m = p.matcher(target);
      if (m.find()) {
          System.out.println();
      }
      
      target = target.replaceAll("[\\s/-]+", "-");
      System.out.println("After replacement: "+target);
      //TODO:: trim!!!!
      
	    DateFormat df = new SimpleDateFormat("yyyy MMM dd kk:mm:ss", Locale.ENGLISH);
	    Date result =  df.parse(target);  
	    System.out.println(result);
	}

}


class ScientificUnitsTintin {

	final static double light_speed_m_per_second = 2.998e8;
	final static double hc_wavelength_to_ev_constant = 1.239996754057428E-6;
	final static  Map<String, String> metricPrefixes;//TODO:: ask if we use array instead
	static {
		metricPrefixes = new HashMap<String, String>();
		metricPrefixes.put("-24", "y");
		metricPrefixes.put("-21", "z");
		metricPrefixes.put("-18", "a");
		metricPrefixes.put("-15", "f");
		metricPrefixes.put("-12", "p");
		metricPrefixes.put("-9", "n");
		metricPrefixes.put("-6", "u");
		metricPrefixes.put("-3", "m");
		metricPrefixes.put("0", "");
		metricPrefixes.put("3", "k");
		metricPrefixes.put("6", "M");
		metricPrefixes.put("9", "G");
		metricPrefixes.put("12", "T");
		metricPrefixes.put("15", "P");
		metricPrefixes.put("18", "E");
		metricPrefixes.put("21", "Z");
		metricPrefixes.put("24", "Y");
	}
	
	/**
	 * @param args
	 */
	public static void main1(String[] args) {
		// TODO Auto-generated method stub
		Double valueToProcess = 9e-9;//223.45654543434d;
		
		String spectralValfromXml =  "9e-9";// big wave "500E-4";//"4.078E-13"
		spectralProcessing(spectralValfromXml);
		

	}
	
	public static void prefixProcessing2(Unit unitToProcess) {
		Double valueToProcess = unitToProcess.getValue();
		String unit = unitToProcess.getUnit();
		System.out.println("prefixProcessing1 input: "+valueToProcess+"  "+unit);
		
		String metricPrefix = null;
		
		NumberFormat formatter = new DecimalFormat("##0.#####E0");
		String displayText = formatter.format(unitToProcess.value);
		
		if (displayText.contains("E")) {
			
			String numeral  = displayText.split("E")[0];
			String basePower = displayText.split("E")[1] ;
			metricPrefix = metricPrefixes.get(basePower);
			metricPrefix = metricPrefix.concat(unit);
			System.out.println("result: "+numeral+"  "+metricPrefix);
		} else {
			System.out.println("result: "+displayText+"  "+unit);
		}
		
	}
	
	public static void prefixProcessing1(Unit unitToProcess) {
		Double valueToProcess = unitToProcess.getValue();
		String unit = unitToProcess.getUnit();
		System.out.println("prefixProcessing1 input: "+valueToProcess+"  "+unit);
		String [] smallMetricPrefixes = {"m:m","u:\u00B5","n:n","p:p","f:f","a:a","z:z","y:y"}; String microSeconds = "\u00B5"+"s";
		String [] largeMetricPrefixes = {"K:K","M:M","G:G","T:T","P:P","E:E","Z:Z","Y:Y"};
		int formattingNumeral = 2;
		
		String metricPrefix = null;
		
		try {
			String valueInProcess= BigDecimal.valueOf(valueToProcess).stripTrailingZeros().toPlainString();
			System.out.println("Before processing: "+valueInProcess+"  "+unit);
			
			if (valueToProcess > 1) {
				if (valueInProcess.contains(".")) {
					valueInProcess= valueInProcess.split("\\.")[0];
				}
				int result = valueInProcess.length()/3-formattingNumeral;System.out.println("Power i take is: "+result);
				if (result > 0) {
					metricPrefix = largeMetricPrefixes[result];
				}
			} else {
				String units= valueInProcess.split("\\.")[1];
				int result = units.length()/3-1;System.out.println("Power i take is: "+result);
				metricPrefix = smallMetricPrefixes[result];
			}

			//System.out.println("Using string formatter: "+String.format("%9.5e",valueToProcess));
			if (metricPrefix!=null && !metricPrefix.isEmpty()) {
				Unit newUnit = null;
				unitToProcess.value = valueToProcess;
				newUnit = new Unit(metricPrefix.split(":")[0]+unit);
				unitToProcess.convertTo(newUnit);
				NumberFormat formatter = new DecimalFormat("0.#####E0");
				unitToProcess.value = Double.parseDouble(formatter.format(unitToProcess.value));
			} else {
				unitToProcess.value = valueToProcess;
			}
			
			System.out.println("final string: "+unitToProcess.value+" "+unitToProcess.symbol);
		} catch (ParseException e) {
			// TODO: handle exception
		}
		
		//displayString = m1.
		
	}
	
	public static void spectralProcessing(String spectralValfromXml) {
		String unit= "m";
		Double spectralVal = Double.parseDouble(spectralValfromXml);
		System.out.println("Original spectral value: "+spectralVal);
		Double wavelengthRange1 = Double.parseDouble(ConfigurationReader.getInstance().getPropertyValue("WAVELENGTHRANGE1"));
		Double wavelengthRange2 = Double.parseDouble(ConfigurationReader.getInstance().getPropertyValue("WAVELENGTHRANGE2"));
		Unit spectralUnit = null;
		try {
			if (spectralVal < wavelengthRange1) {
				//convert to eV
				spectralUnit = convertMeter2eV(spectralVal);
	            
			} else if (spectralVal >= wavelengthRange1 && spectralVal < wavelengthRange2) {
				//Do nothing
				spectralUnit = new Unit(unit);
				spectralUnit.value = spectralVal;
			} else if (spectralVal >= wavelengthRange2) {
				// Convert to hertz
				spectralUnit = convertMeter2Frequency(spectralVal);
			}
			if (spectralUnit!=null) {
                System.out.println("After conv: "+spectralUnit.getValue()+" "+spectralUnit.getUnit());
                prefixProcessing2(spectralUnit);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void addMetricPrefixes1(Double vaDouble) {
		try {
			Unit m1 = new Unit("m");
			m1.value = vaDouble;
			Unit kmunit = new Unit("um");
			m1.convertTo(kmunit);
			System.out.println(m1.value+"  "+ m1.symbol);
			System.out.println(kmunit.value+"  "+ kmunit.symbol);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String addMetricPrefixes(Double valueToProcess, String[] metricPrefixes) {
		try {
			Unit m1 = new Unit("m");
			m1.value = valueToProcess;
			Unit kmunit = new Unit("km");
			m1.convertTo(kmunit);
			System.out.println(m1.value+"  "+ m1.symbol);
			System.out.println(kmunit.value+"  "+ kmunit.symbol);
			
			
			int result = (int) (valueToProcess /3)-1;
			String metricPrefix = metricPrefixes[result];
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	public static Unit convertMeter2Frequency(Double wavelength) {
		Unit frequency = null;
		try {
			Double frequencyValue = light_speed_m_per_second/wavelength;
			frequency= new Unit("Hz");
			frequency.setValue(frequencyValue);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return frequency;
	}
	
	public static Unit convertMeter2eV(Double wavelength) {
		Unit energy = null;
		try {
			Double energyValue = hc_wavelength_to_ev_constant/wavelength;
			energy= new Unit("eV");
			energy.setValue(energyValue);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return energy;
	}
	
	public static void prefixProcessingSimple(Double valueToProcess, String unit) {
		System.out.println("prefixProcessingSimple input: "+valueToProcess+"  "+unit);
		Map<String, String> metricPrefixes = new HashMap<String, String>();//TODO:: ask if we use array instead
		metricPrefixes.put("-24", "y");
		metricPrefixes.put("-21", "z");
		metricPrefixes.put("-18", "a");
		metricPrefixes.put("-15", "f");
		metricPrefixes.put("-12", "p");
		metricPrefixes.put("-9", "n");
		metricPrefixes.put("-6", "u");
		metricPrefixes.put("-3", "m");
		metricPrefixes.put("+3", "k");
		metricPrefixes.put("+6", "M");
		metricPrefixes.put("+9", "G");
		metricPrefixes.put("+12", "T");
		metricPrefixes.put("+15", "P");
		metricPrefixes.put("+18", "E");
		metricPrefixes.put("+21", "Z");
		metricPrefixes.put("+24", "Y");
		
		String metricPrefix = "";
		
		System.out.println("precision is: "+BigDecimal.valueOf(valueToProcess).precision());
		String valueInProcess= BigDecimal.valueOf(valueToProcess).toEngineeringString();System.out.println("Big decimal output:"+valueInProcess);
		if (valueInProcess.contains("E")) {
			String numeral  = valueInProcess.split("E")[0];
			String basePower = valueInProcess.split("E")[1];
			metricPrefix = metricPrefixes.get(basePower);
			metricPrefix = metricPrefix.concat(unit);
			System.out.println("result: "+numeral+"  "+metricPrefix);
		}else {
			System.out.println("result: "+valueInProcess+"  "+unit);
		}
		
		
		System.out.println("Using string formatter: "+String.format("%9.5e",valueToProcess));

		
	}
	public static void prefixProcessing(Double valueToProcess, String unit) {
		Map<String, String> metricPrefixes = new HashMap<String, String>();//TODO:: ask if we use array instead
		metricPrefixes.put("-24", "y");
		metricPrefixes.put("-21", "z");
		metricPrefixes.put("-18", "a");
		metricPrefixes.put("-15", "f");
		metricPrefixes.put("-12", "p");
		metricPrefixes.put("-9", "n");
		metricPrefixes.put("-6", "u");
		metricPrefixes.put("-3", "m");
		metricPrefixes.put("+3", "k");
		metricPrefixes.put("+6", "M");
		metricPrefixes.put("+9", "G");
		metricPrefixes.put("+12", "T");
		metricPrefixes.put("+15", "P");
		metricPrefixes.put("+18", "E");
		metricPrefixes.put("+21", "Z");
		metricPrefixes.put("+24", "Y");
		
		String metricPrefix = "";
		
		System.out.println("precision is: "+BigDecimal.valueOf(valueToProcess).precision());
		String valueInProcess= BigDecimal.valueOf(valueToProcess).toEngineeringString();System.out.println("Big decimal output:"+valueInProcess);
		if (valueInProcess.contains("E")) {
			String numeral  = valueInProcess.split("E")[0];
			String basePower = valueInProcess.split("E")[1];
			metricPrefix = metricPrefixes.get(basePower);
			metricPrefix = metricPrefix.concat(unit);
			System.out.println("result: "+numeral+"  "+metricPrefix);
		}else {
			System.out.println("result: "+valueInProcess+"  "+unit);
		}
		
	}
	
	
	public static void convertMJDToISO() {
		String mjdString = "49987";
		double MJDToJD= Astrodate.MJDToJD(Double.valueOf(mjdString)); System.out.println("MJDToJD: "+MJDToJD);
		double JDToDate= Astrodate.MJDToJD(MJDToJD); System.out.println("JDToDate: "+JDToDate);
		int [] timeInISO = new int[5];
		Astrotime.JD2YMD(MJDToJD, timeInISO);
		System.out.println(timeInISO[0]+" "+timeInISO[1]+" "+timeInISO[2]+" "+timeInISO[3]);
		
		System.out.println("this is in IST:"+Astrodate.JDToDate(MJDToJD));
	}

}
