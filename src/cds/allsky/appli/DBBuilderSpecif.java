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


package cds.allsky.appli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.allsky.BuilderController;
import cds.allsky.Context;
import cds.fits.Fits;
import cds.tools.Util;

public class DBBuilderSpecif extends BuilderController {
	
	static String SURVEY = null;
	HashMap<String, double[]> cuts = null;
	private boolean filter;
	
	public DBBuilderSpecif(Context context) { super(context);  }
	
	public static void main(String[] args) throws Exception {
		
		DBBuilderSpecif db = new DBBuilderSpecif( new Context() );
		
		if (args.length<9) {
			System.out.println("Usage : survey color ordermin ordermax nummin nummax outpath bitpix [-local path] [cutfile]");
			return;
		}
		String survey = args[0];
		String color = args[1];
		// ordre le plus haut dans la pyramide (=> petit ordre) 
		// avec lequel on va compter pour la création 
		db.ordermin =   Integer.parseInt(args[2]);
		// ordre la plus bas dans la pyramide (=> grand ordre) 
		// sur lequel on va créé la meilleure résolution
		int ordermax =   Integer.parseInt(args[3]);
		db.nummin = Long.parseLong(args[4]);
		db.nummax = Long.parseLong(args[5]);
		String path = args[6] + Util.FS;
//		HpxBuilder.createHealpixOrder(ORDER);
//		hpx.setSurvey(survey,color);
		survey = survey+color;
		SURVEY = survey;
		path = path+SURVEY;
		
		db.context.setOutputPath(path);
		db.context.setBitpixOrig( Integer.parseInt(args[8]) );
		
//		if (SURVEY.startsWith("SDSS"))
//			db.setFilter(true);
		
		if (args.length>=10 && args[9].startsWith("-local")) {
			db.context.sethpxFinderPath(args[10]);
			if (args.length>=12) db.readLocalCut(args[11]);
		}
		else db.build(ordermax);
	}
	

	/**
	 * Lecture d'un fichier avec les min et max pour chaque plaque
	 * format : numplaque mincut maxcut 
	 * Dans le cas simple pour tout le survey mettre 0 min max
	 * @param filename
	 */
	private void readLocalCut(String filename) {
		cuts = new HashMap<String, double[]>();
		try {
//			InputStream is = new InputStream( 
//			        new FileInputStream(filename));
			InputStreamReader isr = new InputStreamReader(new FileInputStream(filename));
			BufferedReader in = new BufferedReader(isr);

            String inputLine;
            
            // copie les infos dans la map
            while ((inputLine = in.readLine()) != null){
                String[] valeurs = inputLine.split(" ");
                double[] tab = {Double.parseDouble(valeurs[1]),Double.parseDouble(valeurs[2])};
                cuts.put(valeurs[0], tab);
                
            }
			isr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (cuts.size() == 1) {
			double[] range = cuts.get("0");
			automin = range[0];
			automax = range[1];
		}
	}
	

	protected void toJPG(Fits out, String filename) throws Exception {
		boolean callsuper = false;
		// construit le tableau en 8 bits
		if (SURVEY.startsWith("IRAS")) {
			if (SURVEY.endsWith("12MU"))
				autocutIRIS12(out);
			else if (SURVEY.endsWith("100MU"))
				autocutIRIS100(out);
			else if (SURVEY.endsWith("60MU"))
				autocutIRIS60(out);
			else if (SURVEY.endsWith("25MU"))
				autocutIRIS25(out);
		}
		else if (SURVEY.startsWith("WENS")) {
			autocutWENSS(out);
		}
		else if (SURVEY.startsWith("POSS")) {
			autocutPOSS(out);
		}
		else if (SURVEY.startsWith("COBE")) {
			autocutCOBE(out);
		}
		else if (SURVEY.startsWith("2MASS")) {
			autocut2MASS(out);
		}
		else if (SURVEY.startsWith("GLIMPSEALL1")) {
			autocutGLIMPSE1(out);
		}
		else if (SURVEY.startsWith("GLIMPSEALL2")) {
			autocutGLIMPSE2(out);
		}
		else if (SURVEY.startsWith("GLIMPSEALL3")) {
			autocutGLIMPSE3(out);
		}
		else if (SURVEY.startsWith("GLIMPSEALL4")) {
			autocutGLIMPSE4(out);
		}
		else if (SURVEY.startsWith("SDSS")) {
			autocutSDSS(out);
		}
		else {
			callsuper= true;
		}
		
		if (callsuper) {
			super.toJPG(out,filename);
			return;
		}

		// l'écrit en JPG
		out.writeJPEG(filename);
			
	}

	private String extractField(String filename) {
		String regex = "fpC-(\\d*)-.*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}


	public void autocut2MASS(Fits out) throws Exception {
//		   double [] range = findAutocutRange(0,0, 0.003, 0.9995);
		   out.toPix8(0,20);
	   }
	   public void autocutIRIS12(Fits out) throws Exception {
		      double [] range = {1,8};
		      out.toPix8(range[0],range[1]);
		}
	   public void autocutIRIS100(Fits out) throws Exception {
		      double [] range = {0,30};
		      out.toPix8(range[0],range[1]);
		}
	   public void autocutIRIS25(Fits out) throws Exception {
		      double [] range = {1,15};
		      out.toPix8(range[0],range[1]);
		}
	public void autocutIRIS60(Fits out) throws Exception {
		      double [] range = {0,10};
		      out.toPix8(range[0],range[1]);
		}
	   public void autocutPOSS(Fits out) throws Exception {
//		      double [] range = {4000,22000};
//		      double [] range = {12000,24000};
//		   double[] range = findAutocutRange();
//		   out.toPix8(0, Short.MAX_VALUE);
		   out.toPix8(0,Short.MAX_VALUE);//700, 20000
		}

	   public void autocutWENSS(Fits out) throws Exception {
		      double [] range = out.findAutocutRange();
		      out.toPix8(range[0],range[1]);
		}

	   public void autocutCOBE(Fits out) throws Exception {
		      double [] range = {0,300};
		      out.toPix8(range[0],range[1]);
		}
	   
	   public void autocutGLIMPSE1(Fits out) throws Exception {
		   double [] range = {0,60};
		   out.toPix8(range[0],range[1]);
	   }
	   public void autocutGLIMPSE2(Fits out) throws Exception {
		   double [] range = {0,50};
		   out.toPix8(range[0],range[1]);
	   }
	   public void autocutGLIMPSE4(Fits out) throws Exception {
		      double [] range = {0,180};
		      out.toPix8(range[0],range[1]);
		}
	   public void autocutGLIMPSE3(Fits out) throws Exception {
		      double [] range = {0,85};
		      out.toPix8(range[0],range[1]);
		}
	   public void autocutSDSS(Fits out) throws Exception {
		      double [] range = {-31600,-30000};
//		      if (cuts != null ) {
//		    	  String field = extractField(out.getFilename());
//		    	  range = cuts.get(field);
//		      }
		      out.toPix8(range[0],range[1]);
		}
	   
	   
}
