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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.PixTools;
import cds.tools.pixtools.Util;

public class HpxBuilder {

	public static final int ORDER = 9;
	private static final int SIDE = 512;
	private int bitpix;
	private boolean keepBB = true;

	public int getBitpix() {
		return bitpix;
	}

	private double blank = Fits.DEFAULT_BLANK;
	private double bscale = Fits.DEFAULT_BSCALE;
	private double bzero = Fits.DEFAULT_BZERO;
	private int coaddFlagMode = DescPanel.KEEP;
	String localServer = null;

	public String getLocalServer() {
		return localServer;
	}

	public void setLocalServer(String localServer) {
		this.localServer = localServer;
	}

//	HpxBuilder(int bitpix, String path, boolean keepBB) {
//		this.bitpix = bitpix;
//		this.localServer = path;
//		this.keepBB = keepBB;
//	}
//	HpxBuilder(double bscale, double bzero, double blank, int bitpix, String path, boolean keepBB) {
//		this(bitpix,path,keepBB);
//		this.bscale = bscale;
//		this.bzero = bzero;
//		this.blank = blank;
//	}

	public HpxBuilder() {
	}
/*
	Fits buildHealpix(int nside_file, long l, int nside) {
		try {
			switch (bitpix) {
			case 8:
			case 16:
			case 32:
				return buildIntHealpix(nside_file, l, nside);
			case -32:
			case -64:
				return buildDoubleHealpix(nside_file, l, nside);
			case 0:
				return buildColorHealpix(nside_file, l, nside);
			}
		} catch (Exception e) {
		    StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        e.printStackTrace(pw);
		    Aladin.trace(2, sw.toString());
			return null;
		}
		return null;
	}
*/
	private int recouvrement = 0;
	private boolean filter = false;


//	/**
//	 * Rempli le tableau de pixels correspondant au fichier (losange) Healpix
//	 * donné
//	 * 
//	 * @param nside_file
//	 * @param npix_file
//	 * @param nside
//	 * @param pixels
//	 * @return
//	 * @throws Exception
//	 * @deprecated
//	 */
//	Fits buildDoubleHealpix(int nside_file, long npix_file, int nside)
//			throws Exception {
//		boolean empty = true;
//		long min;
//		long index;
//		double point[] = new double[2];
//		double radec[] = new double[2];
//		Coord coo;
//		Fits file;
//
//		// cherche les numéros de pixels Healpix dans ce losange
//		min = Util.getHealpixMin(nside_file, npix_file, nside, true);
//
//		// initialisation de la liste des fichiers originaux pour ce losange
//		ArrayList<DownFile> downFiles = new ArrayList<DownFile>();
//		
//		point = CDSHealpix.pix2ang_nest(nside_file, npix_file);
//		PixTools.PolarToRaDec(point, radec);
//
//		if (!askLocalFinder(downFiles, localServer, npix_file, Util.order(nside)))
//			return null;
//
//		Fits out = new Fits(SIDE, SIDE, bitpix);
//		out.setBlank(getBlank());
//		if (bscale != Double.NaN && bzero != Double.NaN)  { 
//			out.setBscale(getBscale());
//			out.setBzero(getBzero());
//		}
//		// cherche la valeur à affecter dans chacun des pixels healpix
//		for (int y = 0; y < out.height; y++) {
//			for (int x = 0; x < out.width; x++) {
//				index = min + xy2hpx(y * out.width + x);
//				// recherche les coordonnées du pixels HPX
//				point = CDSHealpix.pix2ang_nest(nside, index);
//				PixTools.PolarToRaDec(point, radec);
//
//				coo = new Coord(radec[0], radec[1]);
//
//				// recherche dans mes fichiers downloadé
//				if ((file = searchDownloaded(downFiles,coo, recouvrement)) != null) {
//					out.setPixelDouble(x, y, file.getPixelDouble((int) coo.x,
//							file.height - 1 - (int) coo.y));
//					empty = false;
//				}
//				// si rien trouvé
//				else {
//					out.setPixelDouble(x, y, Double.NaN);
//					// out.setPixelDouble(x, y, -1);
//				}
//			}
//		}
//		// System.out.println("search + setPixel=> "+(System.currentTimeMillis()-t)+"ms");
//		return (!empty) ? out : null;
//	}
	
    /**
     * Rempli le tableau de pixels correspondant au fichier (losange) Healpix
     * donné
     * 
     * @param nside_file
     * @param npix_file
     * @param nside
     * @param pixels
     * @return
     */
	Fits buildHealpix(int nside_file, long npix_file, int nside, boolean fast,boolean fading) {
	   boolean empty = true;
	   long min;
	   long index;
	   double point[] = new double[2];
	   double radec[] = new double[2];
	   Coord coo = new Coord();
	   DownFile file = null;
	   Fits out=null;

	   try {
	      // cherche les numéros de pixels Healpix dans ce losange
	      min = Util.getHealpixMin(nside_file, npix_file, nside, true);

	      // initialisation de la liste des fichiers originaux pour ce losange
	      ArrayList<DownFile> downFiles = new ArrayList<DownFile>(20);
	      point = CDSHealpix.pix2ang_nest(nside_file, npix_file);
	      PixTools.PolarToRaDec(point, radec);

	      double blank = getBlank();
	      if (!askLocalFinder(downFiles,localServer, npix_file, Util.order(nside), blank)) return null;

	      out = new Fits(SIDE, SIDE, bitpix);
	      out.setBlank(blank);
	      out.setBscale(getBscale());
	      out.setBzero(getBzero());
	      
	      // cherche la valeur à affecter dans chacun des pixels healpix
          double pixval[] = new double[100];   // on va éviter de passer par le total afin d'éviter un débordement
          double pixcoef[] = new double[100];  
	      for (int y = 0; y < out.height; y++) {
	         for (int x = 0; x < out.width; x++) {
	            index = min + xy2hpx(y * out.width + x);
	            // recherche les coordonnées du pixels HPX
	            point = CDSHealpix.pix2ang_nest(nside, index);
	            PixTools.PolarToRaDec(point, radec);

	            if (fast) {
	            	Fits fitsfile;
	            	coo.al = radec[0]; coo.del = radec[1];
					// recherche dans mes fichiers downloadé
					if ((fitsfile = searchDownloaded(downFiles,coo, recouvrement)) != null) {
						double pixelDouble = fitsfile.getPixelDouble((int) coo.x,
								fitsfile.height - 1 - (int) coo.y);
						if (!keepBB)
							pixelDouble = fitsfile.getPixelFull((int) coo.x,
									fitsfile.height - 1 - (int) coo.y);
						out.setPixelDoubleFromBitpix(x, y, pixelDouble,fitsfile.bitpix, dataminmax);
						empty = false;
					}
					// si rien trouvé
					else {
						out.setPixelDouble(x, y, blank);
					}
	            }
	            else  {
	            	radec = Calib.GalacticToRaDec(radec[0], radec[1]);
	            	coo.al = radec[0]; coo.del = radec[1];
	            	// Moyenne des pixels pour toutes les images trouvées
	            	double pixelFinal=0;
	            	int nbPix=0;
	            	double totalCoef=0;
	            	for( int i=downFiles.size()-1; i>=0 && nbPix<pixval.length; i-- ) {
						file = downFiles.get(i);
	            		file.calib.GetXY(coo);
	            		coo.y = file.fitsfile.height-coo.y -1;  // Correction manuelle de 1 en comparaison avec les originaux
	            		coo.x -= 1;                             // Correction manuelle de 1 en comparaison avec les originaux
	            		double pix = getBilinearPixel(file.fitsfile,coo);
	            		if( Double.isNaN(pix) ) continue;
	            		pixval[nbPix]=pix;
	            		totalCoef+= pixcoef[nbPix] = fading ? getCoef(file.fitsfile,coo,10.) : 1;
	            		nbPix++;
	            	}
	            	if( nbPix==0 ) pixelFinal = blank;
	            	else if( totalCoef==0 )  { empty=false; pixelFinal = pixval[0]; }
	            	else {
	            		empty=false;
	            		for( int i=0; i<nbPix; i++ ) pixelFinal += (pixval[i]*pixcoef[i])/totalCoef;
	            	}
	            	
	            	// Juste pour vérifier
//	            	if( nbPix==0 ) pixelFinal = -1;
//	            	else if( totalCoef==0 )  { empty=false; pixelFinal = -2; }
//	            	else {
//	            	   empty=false;
//	            	   pixelFinal = totalCoef;
//	            	}

	            	out.setPixelDoubleFromBitpix(x, y, pixelFinal,file.fitsfile.bitpix,dataminmax);
	            }
	         }
	      }
	   } catch( Exception e ) { }
	   return (!empty) ? out : null;
	}
	
	
	private final String [][] DSSEXT = { {"m7","m9","k7","k9"}, {"mk","mm","kk","km"}, 
	                                     {"6m","8m","6k","8k"}, {"67","69","87","89"}, 
	                                     {"ee","eg","ge","gg"}, {"nn","no","on","oo"} };

	private void calculeDSSMin(double [] min,DownFile file) {

	   try {
	      // Autour des imagettes 67 - 6m, m7 - mm
	      String filename = file.fitsfile.getFilename();
	      int index = filename.lastIndexOf('.');
	      index = filename.lastIndexOf('.',index-1);
	      String subname = filename.substring(0,index);
	      for( int i=0; i<DSSEXT.length; i++ ) {
	         double m=0;
	         for( int j=0; j<4; j++ ) {
	            String name = subname + "." + DSSEXT[i][j] + ".fits";
	            Fits f = new Fits();
	            f.loadFITS(name);
	            m += f.findAutocutRange()[0];
	         }
	         min[i] = m/4;
	      }
	      System.out.println("calculeDSSMin pour "+subname+" => "+min[0]+","+min[1]+","+min[2]+","+min[3]+" c="+min[4]+","+min[5]);

	   } catch( Exception e ) {
	      e.printStackTrace();
	   }
	}

	
//	   private final String [] DSSEXT = { "67","6m","ef","m7","mm" };
//
//	   private void calculeDSSMinMax(double [] minMax,DownFile file) {
//
//	      minMax[0]=0;
//	      minMax[1]=10000;
//
//	      try {
//	         // Imagettes 67 - 6m, ef, m7 - mm
//	         String filename = file.fitsfile.getFilename();
//	         int index = filename.lastIndexOf('.');
//	         index = filename.lastIndexOf('.',index-1);
//	         String subname = filename.substring(0,index);
//	         Fits [] f = new Fits[DSSEXT.length];
//	         int width=0;
//	         for( int i=0; i<DSSEXT.length; i++ ) {
//	            String name = subname + "." + DSSEXT[i] + ".fits";
//	            f[i] = new Fits();
//	            f[i].loadFITS(name);
//	            width += f[i].width;
//	         }
//	         int height = f[0].height;
//	         int bitpix = f[0].bitpix;
//	         Fits f1 = new Fits(width, height, bitpix);
//	         f1.setBlank(f[0].getBlank());
//	         f1.setBscale(f[0].getBscale());
//	         f1.setBzero(f[0].getBzero());
//	         int offset=0;
//	         for( int i=0; i<DSSEXT.length; i++ ) {
//	            System.arraycopy(f[i].pixels, 0, f1.pixels, offset, f[i].pixels.length);
//	            offset+=f[i].pixels.length;
//	            f[i].free();
//	         }
//	         double range [] = f1.findAutocutRange();
//	         f1.free();
//	         minMax[0] = range[0];
//	         minMax[1] = range[1];
//	         System.out.println("calculeDSSMinMax pour "+subname+" => "+minMax[0]+" .. "+minMax[1]);
//
//	      } catch( Exception e ) {
//	         e.printStackTrace();
//	      }
//
//	   }
	
	static private double [][] DSSMin = new double[1500][6];
	static { for( int i=0; i<DSSMin.length; i++) DSSMin[i][5]=Double.NaN; }
	
	private double [] getDSSMin(DownFile file) {
	   int nPlaque = file.getDSSPlaque();
	   double [] min = DSSMin[nPlaque];
	   if( Double.isNaN(min[5]) ) calculeDSSMin(min,file);
	   return min;
	}
	
	// Juste pour des tests
//    Fits buildTestHealpix(int nside_file, long npix_file, int nside) {
//       Fits out=null;
//       try {
//          // cherche les numéros de pixels Healpix dans ce losange
//
//          out = new Fits(SIDE, SIDE, bitpix);
//          for (int y = 0; y < out.height; y++) {
//             for (int x = 0; x < out.width; x++) {
//                double pixel = x+y; //(out.width+out.height)-(x+y);
//                out.setPixelDouble(x, y, pixel);
//             }
//          }
//       } catch( Exception e ) { }
//       return out;
//    }

	// Pour le DSS explicitement
	Fits buildDSSHealpix(int nside_file, long npix_file, int nside) {
	   boolean empty = true;
	   long min;
	   long index;
	   double point[] = new double[2];
	   double radec[] = new double[2];
	   Coord coo = new Coord();
	   Fits out=null;

	   try {
	      // cherche les numéros de pixels Healpix dans ce losange
	      min = Util.getHealpixMin(nside_file, npix_file, nside, true);

	      // initialisation de la liste des fichiers originaux pour ce losange
	      ArrayList<DownFile> downFiles = new ArrayList<DownFile>(20);
	      point = CDSHealpix.pix2ang_nest(nside_file, npix_file);
	      PixTools.PolarToRaDec(point, radec);

	      double blank = getBlank();
	      if (!askLocalFinder(downFiles,localServer, npix_file, Util.order(nside), blank)) return null;

	      out = new Fits(SIDE, SIDE, bitpix);
	      out.setBlank(blank);
	      if (bscale != Double.NaN && bzero != Double.NaN)  { 
	         out.setBscale(getBscale());
	         out.setBzero(getBzero());
	      }

	      // cherche la valeur à affecter dans chacun des pixels healpix
	      double pixval[] = new double[100];   // on va éviter de passer par le total afin d'éviter un débordement
	      double pixcoef[] = new double[100];  
	      for (int y = 0; y < out.height; y++) {
	         for (int x = 0; x < out.width; x++) {
	            index = min + xy2hpx(y * out.width + x);
	            // recherche les coordonnées du pixels HPX
	            point = CDSHealpix.pix2ang_nest(nside, index);
	            PixTools.PolarToRaDec(point, radec);
	            radec = Calib.GalacticToRaDec(radec[0], radec[1]);
	            coo.al = radec[0]; coo.del = radec[1];

	            // Moyenne des pixels pour toutes les images trouvées
	            double pixelFinal=0;
	            int nbPix=0;
	            double totalCoef=0;
	            for( int i=downFiles.size()-1; i>=0 && nbPix<pixval.length; i-- ) {
	               DownFile file = downFiles.get(i);
	               file.calib.GetXY(coo);
	               coo.y = file.fitsfile.height-coo.y -1;  // Correction manuelle de 1 en comparaison avec les originaux
	               coo.x -= 1;                             // Correction manuelle de 1 en comparaison avec les originaux
	               if( !file.inDSS(coo.x,coo.y) ) continue;
	               
	               double pix = getBilinearPixel(file.fitsfile,coo);
	               if( Double.isNaN(pix) ) continue;
	               
	               double [] minCorr = getDSSMin(file);
	               pix = file.getDSSCorrection(pix,coo.x,coo.y,minCorr);
	               pixval[nbPix]=pix;
	               totalCoef+= pixcoef[nbPix] = file.getDSSFading(coo.x,coo.y);
	               nbPix++;
	            }
	            if( nbPix==0 ) pixelFinal = Double.NaN;
//	            else { empty=false; pixelFinal=totalCoef*1000; }   // Juste pour voir le fading

	            else if( totalCoef==0 )  { empty=false; pixelFinal = pixval[0]; }
	            else {
	               empty=false;
	               for( int i=0; i<nbPix; i++ ) pixelFinal += (pixval[i]*pixcoef[i])/totalCoef;
	            }
            	out.setPixelDouble(x, y, pixelFinal);
	         }
	      }
	   } catch( Exception e ) { }
	   return (!empty) ? out : null;
	}

	// Détermination d'un coefficent d'atténuation de la valeur du pixel en fonction de sa distance au bord 
	private double getCoef1(Fits f,Coord coo,double proportion) {
	   double mx = f.width/proportion;
	   double my = f.height/proportion;
	   double coefx=1, coefy=1;
	   if( coo.x<mx ) coefx =  coo.x/mx;
	   else if( coo.x>f.width-mx ) coefx = (f.width-coo.x)/mx;
       if( coo.y<my ) coefy =  coo.y/my;
       else if( coo.y>f.height-my ) coefy = (f.height-coo.y)/my;
       return Math.min(coefx,coefy);
	}
	
	   // Détermination d'un coefficent d'atténuation de la valeur du pixel en fonction de sa distance au centre 
    private double getCoef(Fits f,Coord coo,double proportion) {
       double cx = f.width/2;
       double cy = f.height/2;
       double dx = coo.x-cx;
       double dy = coo.y-cy;
       double d = Math.sqrt(dx*dx + dy*dy);
       double maxd = Math.sqrt(cx*cx + cy*cy);
       return (maxd - d)/maxd;
    }

    private double getBilinearPixel(Fits f,Coord coo) {
       double x = coo.x;
       double y = coo.y;
       
       int x1 = (int)x;
       int y1 = (int)y;
       if( x1<0 || y1<0 || x1>=f.width || y1>= f.height ) return Double.NaN;
       
       int x2=x1+1;
       int y2=y1+1;
       
       int ox2= x2;
       int oy2= y2;
       
       // Sur le bord, on dédouble le dernier pixel
       if( ox2==f.width ) ox2--;
       if( oy2==f.height ) oy2--;
       
       double a0 = getPixelDouble(f,x1,y1);
       double a1 = getPixelDouble(f,ox2,y1);
       double a2 = getPixelDouble(f,x1,oy2);
       double a3 = getPixelDouble(f,ox2,oy2);
       
       if( f.isBlankPixel(a0) ) return Double.NaN;
       if( f.isBlankPixel(a1) ) a1=a0;
       if( f.isBlankPixel(a2) ) a2=a0;
       if( f.isBlankPixel(a3) ) a3=a0;
       
       double d0,d1,d2,d3,pA,pB;
       if( x==x1 ) { d0=1; d1=0; }
       else if( x==x2 ) { d0=0; d1=1; }
       else { d0 = 1./(x-x1); d1 = 1./(x2-x); }
       if( y==y1 ) { d2=1; d3=0; }
       else if( y==y2 ) { d2=0; d3=1; }
       else { d2 = 1./(y-y1); d3 = 1./(y2-y); }
       pA = (a0*d0+a1*d1)/(d0+d1);
       pB = (a2*d0+a3*d1)/(d0+d1);
       return (pA*d2+pB*d3)/(d2+d3);
    }
    
	private double getPixelDouble(Fits f, int x, int y) {
		if (!keepBB)
			return f.getPixelFull(x, y);
		else
			return f.getPixelDouble(x, y);
	}

	/**
	 * Rempli le tableau de pixels correspondant au fichier (losange) Healpix
	 * donné
	 * 
	 * @param nside_file
	 * @param npix_file
	 * @param nside
	 * @param pixels
	 * @return
	 * @throws Exception
	 * @deprecated
	 */
//	Fits buildColorHealpix(int nside_file, long npix_file, int nside)
//			throws Exception {
//		boolean empty = true;
//		long min;
//		long index;
//		double point[] = new double[2];
//		double radec[] = new double[2];
//		Coord coo;
//		Fits file;
//
//		// cherche les numéros de pixels Healpix dans ce losange
//		min = Util.getHealpixMin(nside_file, npix_file, nside, true);
//
//		// initialisation de la liste des fichiers originaux pour ce losange
//		ArrayList<DownFile> downFiles = new ArrayList<DownFile>();
//		point = CDSHealpix.pix2ang_nest(nside_file, npix_file);
//		PixTools.PolarToRaDec(point, radec);
//
//		double blank = getBlank();
//		if (!askLocalFinder(downFiles,localServer, npix_file, Util.order(nside),blank))
//			return null;
//
//		Fits out = new Fits(SIDE, SIDE, bitpix);
//		out.setBlank(blank);
//		if (bscale != Double.NaN && bzero != Double.NaN)  { 
//			out.setBscale(getBscale());
//			out.setBzero(getBzero());
//		}
//		// cherche la valeur à affecter dans chacun des pixels healpix
//		for (int y = 0; y < out.height; y++) {
//			for (int x = 0; x < out.width; x++) {
//				index = min + xy2hpx(y * out.width + x);
//				// recherche les coordonnées du pixels HPX
//				point = CDSHealpix.pix2ang_nest(nside, index);
//				PixTools.PolarToRaDec(point, radec);
//				coo = new Coord(radec[0], radec[1]);
//
//				// recherche dans mes fichiers downloadé
//				if ((file = searchDownloaded(downFiles,coo, recouvrement)) != null) {
//					out.setPixelRGB(x, y, file.getPixelRGB((int) coo.x,
//							file.height - 1 - (int) coo.y));
//					empty = false;
//				}
//				// si rien trouvé
//				else {
//					out.setPixelRGB(x, y, -1);
//				}
//			}
//		}
//		// System.out.println("search + setPixel=> "+(System.currentTimeMillis()-t)+"ms");
//		return (!empty) ? out : null;
//	}

	/**
	 * Rempli le tableau de pixels correspondant au fichier (losange) Healpix
	 * donné
	 * 
	 * @param nside_file
	 * @param npix_file
	 * @param nside
	 * @param pixels
	 * @return true si des pixels ont été écrits
	 * @throws Exception
	 * @deprecated
	 */
//	Fits buildIntHealpix(int nside_file, long npix_file, int nside)
//			throws Exception {
//		boolean empty = true;
//		long min;
//		long index;
//		double point[] = new double[2];
//		double radec[] = new double[2];
//		Coord coo;
//		Fits file = null;
//
//		// initialisation de la liste des fichiers originaux pour ce losange
//		ArrayList<DownFile> downFiles = new ArrayList<DownFile>();
////		downFiles.clear();
//
//		if (!askLocalFinder(downFiles, localServer, npix_file, Util.order(nside)))
//			return null;
//		
//		// cherche les numéros de pixels Healpix dans ce losange
//		min = Util.getHealpixMin(nside_file, npix_file, nside, true);
//		
//		point = CDSHealpix.pix2ang_nest(nside_file, npix_file);
//		PixTools.PolarToRaDec(point, radec);
//		Fits out = new Fits(SIDE, SIDE, bitpix);
//		out.setBlank(getBlank());
//		if (bscale != Double.NaN && bzero != Double.NaN) { 
//			out.setBscale(getBscale());
//			out.setBzero(getBzero());
//		}
//		// cherche la valeur à affecter dans chacun des pixels healpix
//		for (int y = 0; y < out.height; y++) {
//			for (int x = 0; x < out.width; x++) {
//				index = min + xy2hpx(y * out.width + x);
//				// recherche les coordonnées du pixels HPX
//				point = CDSHealpix.pix2ang_nest(nside, index);
//				PixTools.PolarToRaDec(point, radec);
//				coo = new Coord(radec[0], radec[1]);
//
//				// recherche simple depuis l'ancien gagnant
//				file = searchDownloaded(downFiles,coo, recouvrement);
//				if (file != null) {
//					out.setPixelInt(x, y, file.getPixelInt((int) coo.x,
//							file.height - 1 - (int) coo.y));
//					empty = false;
//				}
//				// si rien trouvé
//				else {
//				   if( out.hasBlank() ) out.setPixelInt(x,y,(int)out.getBlank());
//				   else {
//				      out.setPixelInt(x, y, Integer.MIN_VALUE);
//				      out.setBlank(Integer.MIN_VALUE);
//				   }
////                   out.setPixelInt(x, y, -1);
//				}
//
//			}
//		}
//		return (!empty) ? out : null;
//	}

	/**
	 * This method does the actual GET
	 * 
	 * @param theUrl
	 *            The URL to retrieve
	 * @param filename
	 *            the local file to save to
	 * @exception IOException
	 */
	public void get(String theUrl, String filename) throws IOException {
		try {
			URL gotoUrl = new URL(theUrl);
			InputStreamReader isr = new InputStreamReader(gotoUrl.openStream());
			BufferedReader in = new BufferedReader(isr);

			StringBuffer sb = new StringBuffer();
			String inputLine;

			// grab the contents at the URL
			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine + "\r\n");
			}
			// write it locally
			createAFile(filename, sb.toString());
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			throw ioe;
		}
	}

	// creates a local file
	/**
	 * Writes a String to a local file
	 * 
	 * @param outfile
	 *            the file to write to
	 * @param content
	 *            the contents of the file
	 * @exception IOException
	 */
	private static void createAFile(String outfile, String content)
			throws IOException {
		FileOutputStream fileoutputstream = new FileOutputStream(outfile);
		DataOutputStream dataoutputstream = new DataOutputStream(
				fileoutputstream);
		dataoutputstream.writeBytes(content);
		dataoutputstream.flush();
		dataoutputstream.close();
	}

	/**
	 * Interroge les répertoires locaux HpxFinder pour obtenir une liste de
	 * fichiers pour le losange donné Rempli le tableau downFiles
	 * 
	 * @param path
	 * @param npix
	 * @param order
	 * @return
	 */
    boolean askLocalFinder(ArrayList<DownFile> downFiles,String path, long npix, int order,double blank) {
		String hpxfilename = path + cds.tools.Util.FS + Util.getFilePath("", order - ORDER, npix);
		File f = new File(hpxfilename);
		String fitsfilename = null;
		if (f.exists()) {
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(f));
			while( (fitsfilename = reader.readLine()) != null) {
				try {
//					récupère l'image
					Fits fitsfile = new Fits();
					if (fitsfilename.endsWith("hhh")) {
						fitsfile.loadHeaderFITS(fitsfilename);
						fitsfilename=fitsfilename.replaceAll("hhh$", "jpg");
						fitsfile.loadJpeg(fitsfilename,true);
						fitsfile.inverseYColor();
					}
					else if (bitpix==0) fitsfile.loadFITS(fitsfilename,true);
					else fitsfile.loadFITS(fitsfilename);

					fitsfile.setFilename(fitsfilename);
					if( !Double.isNaN(blank) ) fitsfile.setBlank(blank);

					DownFile file = new DownFile();
					file.fitsfile = fitsfile;
					file.calib = fitsfile.getCalib();
					
					// applique un filtre spécial
					if (isFilter() ) filter(fitsfile);
					
					downFiles.add(file);

				} catch (Exception e) {
					System.err.println("Erreur de chargement de : " + fitsfilename);
					e.printStackTrace();
					continue;
				}
			}
			} catch (Exception e1) { // FileNotFound sur f=File(hpxfilename) et IO sur reader.readLine
				// this should never happens
				e1.printStackTrace();
				return false;
			}
			return true;
		}
		else {
//			System.err.println("File Not Found : " + hpxfilename);
			return false;
		}
	}

	private void filter(Fits f) {
		// enlève le fond de ciel
		int skyval = 0;
		
		try {
//			skyval = (int)f.headerFits.getDoubleFromHeader("SOFTBIAS");
			try {
				skyval = (int)f.headerFits.getDoubleFromHeader("SKYVAL");
			} catch (NullPointerException e) {
				skyval = (int)f.headerFits.getDoubleFromHeader("SKY");
			}
		} catch (NullPointerException e) {
		}
		if (skyval != 0) {
			for( int y=0; y<f.height; y++ ) {
				for( int x=0; x<f.width; x++ ) {
					// applique un nettoyage pour enlever les valeurs aberrantes
					if (f.getPixelFull(x, y) < skyval)
						f.setPixelInt(x, y, (int)blank);
					else
						f.setPixelInt(x, y, f.getPixelInt(x, y)-skyval);
				}
			}
		}
		// else
		// on n'a pas de valeur de fond à enlever
//		// on fait un autocut
//		for( int y=0; y<f.height; y++ ) {
//			for( int x=0; x<f.width; x++ ) {
//				f.autocut();
//			}
//		}

	}
int n =0;
	/**
	 * Cherche dans la liste des fichiers récupérés sur Aladin si les
	 * coordonnées y sont, et renvoie sa position dans l'objet de Coordonnées
	 * @param coo_gal coordonnée d'entrée en ra,dec (en GAL), où le x,y est enregistré
	 * @return l'accès au fichier FITS lu
	 * @see Calib#GetXY(Coord)
	 */
	private Fits searchDownloaded(ArrayList<DownFile> downFiles, Coord coo_gal, int recouvrement) {
		if (downFiles == null)
			return null;

		int nfiles = downFiles.size();
		for (int i = 0 ; i<nfiles ; i++,gagnant++) {
			if( gagnant>=nfiles ) gagnant=0;
			// cherche d'abord dans l'ancien gagnant
			DownFile file = downFiles.get(gagnant);
			Calib calib = file.calib;
			// transforme les coordonnées en ICRS
			double[]radec = Calib.GalacticToRaDec(coo_gal.al,coo_gal.del);
			Coord c = new Coord(radec[0],radec[1]);
			
			if (isInFile(c, recouvrement, calib)) {
			   double pix = file.fitsfile.getPixelDouble((int)c.x, file.fitsfile.height-1-(int)c.y);
			   coo_gal.x = c.x;
			   coo_gal.y = c.y;
			   if( !file.fitsfile.isBlankPixel(pix ) ) return file.fitsfile;
			   
			}
			
//			else
//				System.out.println("\t inutile : " + downFiles.get(gagnant).fitsfile.filename);
		}
		return null;
	}

	/**
	 * sans recouvrement si elle est un tout petit peu sur le bord on copie la
	 * valeur
	 * 
	 * @param c
	 *            coordonnées en ICRS (x,y modifiés)
	 */
	private boolean isInFile(Coord c, int recouvrement, Calib calib) {
		try {
			calib.GetXY(c);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		// si la coordonnée est bien dedans
		double xnpix = calib.getImgSize().getWidth();
		double ynpix = calib.getImgSize().getHeight();
		if (c.x >= recouvrement && c.x <= xnpix - 1 - recouvrement
				&& c.y >= recouvrement && c.y <= ynpix - 1 - recouvrement) {

			return true;
		}
		// sans recouvrement si elle est un tout petit peu sur le bord on copie la valeur
		if (recouvrement==0 &&
				c.x >= -2 && c.x <= xnpix+1
						&& c.y >= -2 && c.y <= ynpix+1
		) {
			if (c.x >=-2 && c.x < 0) {
				c.x = 0;
			}
			if (c.x > xnpix-1 && c.x <= xnpix+2) {
				c.x = xnpix-1;
			}
			if (c.y >= -2 && c.y < 0) {
				c.y = 0;
			}
			if (c.y > ynpix-1 && c.y <= ynpix+2) {
				c.y = ynpix-1;
			}

			return true;
		}
		return false;
	}

	int gagnant=0;

	/**
	 * Ecrit les pixels dans un fichier .hpx avec notre format Healpix
	 * 
	 * @param filename_base
	 * @param nside_file
	 * @param npix
	 * @param nside
	 * @param out
	 * @throws Exception
	 */
	void writeHealpix(String filename_base, int nside_file, long npix,
			int nside, Fits out) throws Exception {

		// prépare l'entete
		double incA = -(90./nside_file)/(SIDE);
		double incD = (90./nside_file)/(SIDE);
		double[] proj_center = new double[2];
		proj_center = CDSHealpix.pix2ang_nest(nside_file,npix);
		out.setCalib( new Calib(
				proj_center[0],proj_center[1],
				(SIDE+1)/2,(SIDE+1)/2,
				SIDE,SIDE,
				incA,incD,0,Calib.TAN,false,Calib.FK5));

		out.headerFits.setKeyValue("ORDERING", "CDSHEALPIX");
		if (bitpix > 0)
			out.headerFits.setKeyValue("BLANK", String
					.valueOf(Integer.MAX_VALUE));

		// gestion des niveaux de gris
		// cherche le min max
		double minmax[] = out.findMinMax();
		out.headerFits.setKeyValue("DATAMIN", String.valueOf(minmax[0]));
		out.headerFits.setKeyValue("DATAMAX", String.valueOf(minmax[1]));

		// Ecriture de l'image générée sous forme FITS "fullbits"
		out.writeFITS(filename_base);
		System.out.println("file " + filename_base + " written !!");

	}

	private int[] xy2hpx = null;
	private int[] hpx2xy = null;
	private double[] dataminmax = new double[2];

	/** Méthode récursive utilisée par createHealpixOrder */
	private void fillUp(int[] npix, int nsize, int[] pos) {
		int size = nsize * nsize;
		int[][] fils = new int[4][size / 4];
		int[] nb = new int[4];
		for (int i = 0; i < size; i++) {
			int dg = (i % nsize) < (nsize / 2) ? 0 : 1;
			int bh = i < (size / 2) ? 1 : 0;
			int quad = (dg << 1) | bh;
			int j = pos == null ? i : pos[i];
			npix[j] = npix[j] << 2 | quad;
			fils[quad][nb[quad]++] = j;
		}
		if (size > 4)
			for (int i = 0; i < 4; i++)
				fillUp(npix, nsize / 2, fils[i]);
	}

	/** Creation des tableaux de correspondance indice Healpix <=> indice XY */
	void createHealpixOrder(int order) {
		int nsize = (int) CDSHealpix.pow2(order);
		xy2hpx = new int[nsize * nsize];
		hpx2xy = new int[nsize * nsize];
		fillUp(xy2hpx, nsize, null);
		for (int i = 0; i < xy2hpx.length; i++)
			hpx2xy[xy2hpx[i]] = i;
	}

	/**
	 * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
	 * d'initialiser au préalable avec createHealpixOrdre(int)
	 */
	final public int xy2hpx(int hpxOffset) {
		return xy2hpx[hpxOffset];
	}

	/**
	 * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
	 * d'initialiser au préalable avec createHealpixOrdre(int)
	 */
	final public int hpx2xy(int xyOffset) {
		return hpx2xy[xyOffset];
	}

	public void setBitpix(int bitpix, boolean keepBB) {
		this.bitpix = bitpix;
		this.keepBB = keepBB;
	}
	
	public void setDataCut(double[] minmax) {
		dataminmax = minmax;
	}
	/*
	public static void main(String[] args) {
		long t= System.currentTimeMillis();
		int bitpix = 16;
		long n =0;
		
		
		for (long i = 0 ; i < 100000000 ; i++) {
//			switch (bitpix) {
//			case 8:
//			case 16:
//			case 32:
				int j = (int)i;
//			case -32:
//			case -64:
//				double d = (double)i;
//			case 0:
//				int k = (int)i%256;
//			}
		}
		System.out.println((System.currentTimeMillis()-t)+"ms");
	}
	*/

    /**
     * @param Positionne le flag de co-addition avec des losanges pré-existants 
     */
    public void setCoadd(int coaddMode) {
        this.coaddFlagMode = coaddMode;
    }

    /**
     * @param blank the blank to set
     */
    public void setBlank(double blank) {
        this.blank = blank;
    }

	/**
	 * @return the blank
	 */
	public double getBlank() {
		return blank;
	}

	/**
	 * @param bscale the bscale to set
	 */
	public void setBscale(double bscale) {
		this.bscale = bscale;
	}

	/**
	 * @return the bscale
	 */
	public double getBscale() {
		return bscale;
	}

	/**
	 * @param bzero the bzero to set
	 */
	public void setBzero(double bzero) {
		this.bzero = bzero;
	}

	/**
	 * @return the bzero
	 */
	public double getBzero() {
		return bzero;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	/**
	 * @return the filter
	 */
	public boolean isFilter() {
		return filter;
	}

}
