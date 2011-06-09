package cds.allsky.appli;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.fits.Fits;
import cds.tools.pixtools.Util;

public class Autocut {

	public static void main(String[] args) {
		String regex = "fpC-(\\d*)-.*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = null;
		String field = null;
		double[] cut;
		HashMap<String,double[]> cuts = new HashMap<String,double[]>();
		File path = new File(args[0]);
		String[] files = path.list();
		for (String file : files) {
			matcher = pattern.matcher(file);
			if (matcher.matches()) {
				field = matcher.group(1);
				if (!cuts.containsKey(field)) {
					Fits f = new Fits();
					try {
						f.loadFITS(path+Util.FS+file);
						cut = f.findAutocutRange();
					} catch (Exception e) {
						System.err.println("Fichier : " + file);
						e.printStackTrace();
						return;
					}
					cuts.put(field,cut);
				}
			}
		}
		
		for (Map.Entry<String, double[]> e : cuts.entrySet())
		    System.out.println(e.getKey() + " " + e.getValue()[0]+ " " + e.getValue()[1]);

	}
}
