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

package cds.allsky;

import static cds.tools.Util.FS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

public class InitLocalAccess {


	private double progress = 0;
	private String initpath = null;
	private String currentfile = null;
	private String pausepath = null;
	private String currentpath = "";
	
	boolean stopped = false;
	
	public boolean build(String input, String output, int order) {
		return build(input, output, order,null);
	}
	public boolean build(String input, String output, int order, String regex) {
	   //output += FS + AllskyConst.SURVEY;
	   File f = new File(output);
	   if (!f.exists()) f.mkdir();
	   String pathDest = output + FS + AllskyConst.HPX_FINDER;
	   stopped = false;

	   //		NMAX = Util.getMax(order);
	   progress = 0;
	   f = new File(pathDest+FS+"Norder"+order);
	   pausepath = pathDest+FS+"Norder"+order+FS+"pause";
	   if (f.exists()) {
	      File fpause = new File(pausepath);
	      if (fpause.exists()) {
	         BufferedReader r;
	         try {
	            r = new BufferedReader(new InputStreamReader((new FileInputStream(fpause))));
	            initpath = r.readLine();

	         } catch (FileNotFoundException e) {
	            e.printStackTrace();
	         } catch (IOException e) {
	            e.printStackTrace();
	         }
	      }
	      else {
	         progress=100;
	         return false;
	      }
	      //			cds.tools.Util.deleteDir(f);
	   }
	   create(input, pathDest, regex, order);

	   // s'il ya eu une interruption -> sortie rapide
	   if (stopped) return false;
	   else {
	      progress=100;
	      File fpause = new File(pausepath);
	      fpause.delete();
	   }
	   return true;
	}

	/**
	 * @param args
	 */
	/*
	public static void main(String[] args) {
		long t=System.currentTimeMillis();
		String pathSource = args[0]+ FS;
		String pathDest = pathSource + HPX_FINDER;
		String regex = args[1];
		int order =   Integer.parseInt(args[2]);
		
		create(pathSource, pathDest, regex, order);
		System.out.println("done => "+(System.currentTimeMillis()-t)+"ms");
	
	}*/

//	/** Création si nécessaire des répertoires et sous-répertoire du fichier 
//	 * passé en paramètre 
//	 */
//	public static void createPath(String filename) {
//		for( int pos=filename.indexOf(FS,3); pos>=0; pos=filename.indexOf(FS,pos+1)) {
//			File f = new File( filename.substring(0,pos) );
//			if( !f.exists() ) {
//				f.mkdir();
//			}
//		}
//	}
	

	/** Création si nécessaire du fichier passé en paramètre
	 * et ouverture en écriture 
	 * @throws FileNotFoundException 
	 */
	public static FileOutputStream openFile(String filename) throws FileNotFoundException {
		File f = new File( filename/*.replaceAll(FS+FS, FS)*/ );
		if( !f.exists() ) {
		   cds.tools.Util.createPath(filename);
			return new FileOutputStream(f);
		}
		return new FileOutputStream(f, true);
	}
	
	/** Writes a String to a local file
     * 
     * @param outfile the file to write to
     * @param content the contents of the file
     * @exception IOException 
     */
    private static void createAFile(FileOutputStream out, String content) throws IOException {
        DataOutputStream dataoutputstream = new DataOutputStream(out);
        dataoutputstream.writeBytes(content);
        dataoutputstream.flush();
        dataoutputstream.close();
    }

	/**
	 * Pour chaque fichiers FITS, cherche la liste des losanges couvrant la zone.
	 * Créé (ou complète) un fichier HPX text contenant le
	 * chemin vers les fichiers FITS 
	 *
	 */
	public void create(String pathSource, String pathDest, String regex, int order) {

		String hpxname;
		FileOutputStream out;
		long npix;
		long[] npixs = null;
//		double radius = 0;
		Fits fitsfile = new Fits();
		
		// pour chaque fichier dans le sous répertoire
		File main = new File(pathSource);
		
		String[] list = main.list();
		currentpath = pathSource;
		// trie la liste pour garantir la reprise au bon endroit 
		Arrays.sort(list);
		if (list==null) return;

		progress=0;
		for (int f = 0 ; f < list.length && !stopped; f++) {
			progress = f*100./(list.length-1);
			
			currentfile = pathSource+FS+list[f];

			if ((new File(currentfile)).isDirectory() && !list[f].equals(AllskyConst.SURVEY)) {
				System.out.println("Look into dir " + currentfile);
//				currentpath = currentfile;
				create(currentfile, pathDest, regex, order);
				currentpath = pathSource;
			}
			else if (regex == null || currentfile.matches(regex)) {
				// en cas de reprise, saute jusqu'au dernier fichier utilisé
				if (initpath != null) { 
					if (initpath.equals(currentfile)) {
						initpath=null;
					}
					else continue;
				}

				try {
					try {
						fitsfile.loadHeaderFITS(currentfile);
					}  catch (Exception e) {
						Aladin.trace(3,e.getMessage() + " " + currentfile);
						continue;
					}

					// transforme les coordonnées du point de ref de l'image en GAL
					Coord centerGAL;
					double[] aldel = Calib.RaDecToGalactic(
							fitsfile.center.al, fitsfile.center.del);
					centerGAL = new Coord(aldel[0], aldel[1]);

					double[] inc = fitsfile.getCalib().GetResol();
					double radius = Math.max(
							Math.abs(inc[0]) * fitsfile.width/2.,
							Math.abs(inc[1]) * fitsfile.height/2.)
							;
					// rayon jusqu'à l'angle, au pire * sqrt(2)
					radius *= Math.sqrt(2.);

					npixs = getNpixList(order, centerGAL, radius);

					// pour chacun des losanges concernés
					for (int i = 0; i < npixs.length; i++) {
						npix = npixs[i];
						// vérifie la validité du losange trouvé
						if (!isInImage(fitsfile.getCalib(),Util.getCorners(order, npix))) continue;
						
						// initialise les chemins
						if (!pathDest.endsWith(FS)) {
							pathDest = pathDest + FS;
						}
						hpxname = pathDest+ Util.getFilePath("", order,npix);
						cds.tools.Util.createPath(hpxname);
						out = openFile(hpxname);
						// ajoute le chemin du fichier Source FITS
						createAFile(out, currentfile+"\n");
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.err.println(currentfile);
					return;
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println(currentfile);
					return;
				}
			}
		}

		if (!stopped) progress = 100;
	}
	
	public String getCurrentpath() {
		return currentpath;
	}
	/**
	 * Récupère la liste des numéros de pixels healpix dans un cercle
	 * @param order
	 * @param center centre de la recherche en gal
	 * @param radius rayon de la recherche (sera agrandi) en degrés 
	 * @return
	 */
	static long [] getNpixList(int order, Coord center, double radius) {
		long nside = CDSHealpix.pow2(order);
		try {
			// augmente le rayon de la taille d'un demi pixel en plus de l'option inclusive => +1 pixel
//			long[] npix = CDSHealpix.query_disc(nside,center.al,center.del,
//					Math.toRadians(radius)+Math.PI/(4.*nside),true);
			
			// --- calcule la taille réel de la plus grande diagonale du pixel
			// récupère le pixel central
			double[] thetaphi = Util.RaDecToPolar(new double[]{center.al,center.del});
			long n = CDSHealpix.ang2pix_nest(nside, thetaphi[0], thetaphi[1]);
			// calcule ses 4 coins et son centre
			Coord[] corners = Util.getCorners(order,n);
			double[] c = CDSHealpix.pix2ang_nest(nside, n);
			c = Util.PolarToRaDec(c);
			Coord c1 = new Coord(c[0],c[1]);
			// cherche la plus grande distance entre le centre et chaque coin
			double dist = 0;
			for (int i = 0 ; i < 4 ; i++)
				dist = Math.max(dist, Coord.getDist(c1, corners[i]));
			
			long[] npix = CDSHealpix.query_disc(nside,center.al,center.del,Math.toRadians(radius+dist),true);

			return npix;
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} 
	}

	static boolean isInImage(Calib calib, Coord[] corners) {
		int signeX = 0;
		int signeY = 0;
		try {
			int marge = 2;
//			Coord coo = new Coord();
//			coo.al=ra; coo.del=dec;
			for (int i = 0; i < corners.length; i++) {
				Coord coo = corners[i];
				double[] radec = Calib.GalacticToRaDec(coo.al, coo.del);
				coo.al=radec[0]; coo.del = radec[1];
				calib.GetXY(coo);
				if( Double.isNaN(coo.x) ) continue;
				int width = calib.getImgSize().width+marge;
				int height = calib.getImgSize().height+marge;
				if(coo.x>=-marge && coo.x<width && coo.y>=-marge && coo.y<height) {
					return true;
				}
				// tous d'un coté => x/y tous du meme signe
				signeX += (coo.x>=width)?1:(coo.x<-marge)?-1:0;
				signeY += (coo.y>=height)?1:(coo.y<-marge)?-1:0;
				
			}
		} catch (Exception e) {return false;}

		if (Math.abs(signeX) == Math.abs(corners.length) || Math.abs(signeY) == Math.abs(corners.length))
			return false;
		
		return true;

	}
	 
	public double getProgress() {
		return progress;
	}

	public static int getNbNpix(String output, int order) {
		return Util.computeNFiles(new File(output 
					+FS+ AllskyConst.HPX_FINDER
					+FS +"Norder"+order ));
		
	}

	public void stop() {
		stopped = true;
		if (pausepath == null)
			return;
		
		// on enregistre l'état
		File fpause = new File(pausepath);
		BufferedWriter r;
		try {
			r = new BufferedWriter(new OutputStreamWriter((new FileOutputStream(fpause))));
			r.write(currentfile);
			r.flush();
			r.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			stopped = false;
		} catch (IOException e) {
			e.printStackTrace();
			stopped = false;
		}
	}

	
	/*
	 public static void main(String[] args) {
		   Calib c = new Calib();
		   InitLocalAccess init = new InitLocalAccess();
		   
		   long t = System.currentTimeMillis();
		   for (int i = 0 ; i < 10000000 ; i++) {
			  c.GalacticToRaDec(i%360,(i%360)-100, init.cooeq, init.framegal);
		   }
		   System.out.println((System.currentTimeMillis()-t)+"ms");
		   // 4700 ms avec la déclaration en externe de la méthode / 8400 ms en interne
	 }
	 */
}
