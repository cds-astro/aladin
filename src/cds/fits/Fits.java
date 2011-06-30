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

package cds.fits;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.MyInputStream;
import cds.allsky.AllskyConst;
import cds.image.Hdecomp;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/**
 * Classe de manipulation d'un fichier image FITS
 */
public class Fits {
	public static final double DEFAULT_BLANK = Double.NaN;
	public static final double DEFAULT_BSCALE = 1.;
	public static final double DEFAULT_BZERO = 0.;
	public HeaderFits headerFits;    // header Fits
	public byte [] pixels;           // Pixels d'origine "fullbits" (y compté depuis le bas)
	public int bitpix;               // Profondeur des pixels (codage FITS 8,16,32,-32,-64)
	public int width;                // Largeur de l'image
	public int height;               // Hauteur de l'image
	public double bzero=DEFAULT_BZERO;	// BZERO Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double bscale=DEFAULT_BSCALE;	// BSCALE Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double blank=DEFAULT_BLANK;  // valeur BLANK
	public boolean flagARGB=false;   // true s'il s'agit d'un FITS couleur ARGB

	public Coord center;             // Coord J2000 du centre de l'image
	public byte [] pix8;             // Pixels 8 bits en vue d'une sauvegarde JPEG (y compté depuis le haut)
	public int [] rgb;               // pixels dans le cas d'une image couleur RGB
	//    (y compté depuis le haut) => bitpix==0

	private Calib calib;             // Calibration astrométrique


	public static String FS = System.getProperty("file.separator");
	
	static double [] minmax8;
	static double [] minmax16;
	static double [] minmax32;
	static double [] minmax_32;
	static double [] minmax_64;
	static {
		minmax8 = new double[] {0, 255};
		minmax16 = new double[] {0, Short.MAX_VALUE}; 
		minmax32 = new double[] {0, Short.MAX_VALUE}; 
		minmax_32 = new double[] {0, 10000};
		minmax_64 = new double[] {0, 10000};
	}

   /** Création en vue d'une lecture */
   public Fits() { }

   /** Création en vue d'une construction "manuelle"
    * @param width Largeur
    * @param height Hauteur
    * @param bitpix Profondeur des pixels (codage FITS 8,16,32,-32,-64  ou 0 pour RGB)
    */
   public Fits(int width,int height,int bitpix) {
      this.width=width;
      this.height=height;
      this.bitpix=bitpix;
      if( bitpix==0 ) {
         rgb = new int[width*height];
      } else {
         pixels = new byte[width*height * Math.abs(bitpix)/8];
         pix8 = new byte[width*height];
         headerFits = new HeaderFits();
         headerFits.setKeyValue("SIMPLE","T");
         headerFits.setKeyValue("BITPIX",bitpix+"");
         headerFits.setKeyValue("NAXIS","2");
         headerFits.setKeyValue("NAXIS1",width+"");
         headerFits.setKeyValue("NAXIS2",height+"");
      }
   }


   public double raMin=0,raMax=0,deMin=0,deMax=0;

   /** Positionnement d'une calibration => initialisation de la coordonnée
    * centrale de l'image (cf center)
    */
   public void setCalib(Calib c) {
      calib=c;
      try {
         center = new Coord();
         center.x = width/2.;
         center.y = height/2.;
         calib.GetCoord(center);

         Coord coo = new Coord();
         for( int i=0; i<4; i++ ) {
            coo.x= i==0 || i==3 ? 24 : width-24;
            coo.y= i<2 ? 24 : height-24;
            calib.GetCoord(coo);
            if( i==0 || (coo.al<raMin && Math.abs(coo.al-raMin)<300) ) raMin=coo.al;
            if( i==0 || (coo.al>raMax && Math.abs(coo.al-raMax)<300) ) raMax=coo.al;
            if( i==0 || (coo.del<deMin && Math.abs(coo.del-deMin)<300) ) deMin=coo.del;
            if( i==0 || (coo.del>deMax && Math.abs(coo.del-deMax)<300) ) deMax=coo.del;
         }
      } catch( Exception e ) { e.printStackTrace(); }
   }

   /** Retourne la calib ou null */
   public Calib getCalib() { return calib; }

   /* Chargement d'une image N&B sous forme d'un JPEG */
   protected void loadJpeg(MyInputStream dis) throws Exception { loadJpeg(dis,false); }

   /** Chargement d'une image N&B ou COULEUR sous forme d'un JPEG */
   protected void loadJpeg(MyInputStream dis,boolean flagColor) throws Exception {
      Label observer = new Label();
      Image img = Toolkit.getDefaultToolkit().createImage(dis.readFully());
      boolean encore=true;
      while( encore ) {
         try {
            MediaTracker mt = new MediaTracker(observer);
            mt.addImage(img,0);
            mt.waitForID(0);
            encore=false;
         } catch( InterruptedException e ) { }
      }
      width =img.getWidth(observer);
      height=img.getHeight(observer);
      bitpix= flagColor ? 0 : 8;
      
      BufferedImage imgBuf = new BufferedImage(width,height, 
            flagColor ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_BYTE_GRAY);
      Graphics g = imgBuf.getGraphics();
      g.drawImage(img,0,0,observer);
      g.finalize(); g=null;
      if( flagColor ) rgb = ((DataBufferInt)imgBuf.getRaster().getDataBuffer()).getData();
      else pixels = ((DataBufferByte)imgBuf.getRaster().getDataBuffer()).getData();
      
//      BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
//      Graphics g = imgBuf.getGraphics();
//      g.drawImage(img,0,0,observer);
//      g.finalize(); g=null;
//      int taille=width*height;
//      rgb = new int[taille];
//      imgBuf.getRGB(0, 0, width, height, rgb, 0, width);
//      if( bitpix!=0 ) {
//         pixels = new byte[taille];
//         for( int i=0; i<taille; i++ ) pixels[i] = (byte)(rgb[i] & 0xFF);
//         rgb=null;
//      }
      
      imgBuf.flush(); imgBuf=null;
      img.flush(); img=null;
      if( bitpix==8 ) {
         pix8 = new byte[width*height];
         initPix8();
      }
   }

   /** Chargement d'une image Jpeg N&B depuis un fichier */
   public void loadJpeg(String filename) throws Exception { loadJpeg(filename,false); }

   /** Chargement d'une image Jpeg N&B ou COULEUR depuis un fichier */
   public void loadJpeg(String filename,boolean flagColor) throws Exception {
      MyInputStream is = new MyInputStream( new FileInputStream(filename));
      loadJpeg(is,flagColor);
      is.close();
      this.setFilename(filename);
   }


   /** Chargement d'une image FITS N&B depuis un fichier */
   public void loadFITS(String filename) throws Exception {loadFITS(filename,false);}
   public void loadFITS(String filename, boolean color) throws Exception {
      MyInputStream is = new MyInputStream( new FileInputStream(filename));
      is = is.startRead();
      if( color ) loadFITSColor(is);
      else loadFITS(is);
      is.close();
      this.setFilename(filename);
   }

   /** Chargement d'une image FITS */
   public void loadFITS(MyInputStream dis) throws Exception {
	   dis = dis.startRead();
	   headerFits = new HeaderFits(dis);
	   bitpix = headerFits.getIntFromHeader("BITPIX");
	   width  = headerFits.getIntFromHeader("NAXIS1");
	   height = headerFits.getIntFromHeader("NAXIS2");
	   pixels = new byte[width*height * (Math.abs(bitpix)/8)];
	   try { blank = headerFits.getDoubleFromHeader("BLANK");} catch( Exception e ) { blank=DEFAULT_BLANK; }
	   if (headerFits.isHCOMP()) pixels = Hdecomp.decomp(dis);
	   else dis.readFully(pixels);
	   try { bscale = headerFits.getIntFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
	   try { bzero  = headerFits.getIntFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
	   try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { calib=null; }
	   pix8 = new byte[width*height];
	   if( bitpix==8 ) initPix8();
   }
   
   /** Chargement d'une image FITS couleur mode ARGB */
   public void loadFITSARGB(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      width  = headerFits.getIntFromHeader("NAXIS1");
      height = headerFits.getIntFromHeader("NAXIS2");
      pixels = new byte[width*height*32];
      dis.readFully(pixels);
      setARGB();
      try { setCalib(new Calib(headerFits)); }
      catch( Exception e ) { calib=null; }
   }
   
   /** Chargement d'une image FITS couleur mode RGB cube */
   public void loadFITSColor(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      bitpix = headerFits.getIntFromHeader("BITPIX");
      width  = headerFits.getIntFromHeader("NAXIS1");
      height = headerFits.getIntFromHeader("NAXIS2");
      pixels = new byte[width*height];
      if (headerFits.isHCOMP()) {
    		  pixels = Hdecomp.decomp(dis);
      }
      else dis.readFully(pixels);

      byte[] t2 = new byte[width*height];
      dis.readFully(t2);
      byte[] t3 = new byte[width*height];
      dis.readFully(t3);
      rgb = new int[width*height];
	   for( int i=0; i<height*width; i++ ){
			   int val = 0;
			   val = 0 | (((pixels[i])&0xFF)<<16)
	              | (((t2[i])&0xFF)<<8) | (t3[i])&0xFF;
			   rgb[i] = val;
	   }
      try { bscale = headerFits.getIntFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
      try { bzero  = headerFits.getIntFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
      try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { calib=null; }

   }

   /** Chargement de l'entete d'une image FITS depuis un fichier */
   public void loadHeaderFITS(String filename) throws Exception {
      MyInputStream is = new MyInputStream(
            new FileInputStream(filename));
      is = is.startRead();
      headerFits = new HeaderFits(is);
      try {
    	  bitpix = headerFits.getIntFromHeader("BITPIX");
      } catch (Exception e1) {
    	  bitpix = 0;
      }
//      if( bitpix!=0 ) bitpix = headerFits.getIntFromHeader("BITPIX");
      width  = headerFits.getIntFromHeader("NAXIS1");
      height = headerFits.getIntFromHeader("NAXIS2");
      try { blank = headerFits.getDoubleFromHeader("BLANK");} catch( Exception e ) { blank=DEFAULT_BLANK; }
      try { bscale = headerFits.getIntFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
      try { bzero  = headerFits.getIntFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
      try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { calib=null; }
      is.close();
      this.setFilename(filename);
   }

   public void setHeaderFrom(double center_ra, double center_dec, double incA) {
	   headerFits.setKeyValue("CRVAL1", String.valueOf(center_ra));
	   headerFits.setKeyValue("CRVAL2", String.valueOf(center_dec));
	   headerFits.setKeyValue("CRPIX1", String.valueOf((width+1)/2.));
	   headerFits.setKeyValue("CRPIX2", String.valueOf((height+1)/2.));
	   headerFits.setKeyValue("CDELT1", String.valueOf(-incA));
	   headerFits.setKeyValue("CDELT2", String.valueOf(incA));
	   headerFits.setKeyValue("CTYPE1", "RA---TAN");
	   headerFits.setKeyValue("CTYPE2", "DEC--TAN");
//	   headerFits.setKeyValue("CROTA1", String.valueOf(-135));
	   headerFits.setKeyValue("CROTA2", String.valueOf(-135));
   }
   
   /** Retourne la valeur BSCALE (1 si non définie) */
   public double getBscale() { return bscale; }
   
   /** Retourne la valeur BZERO (0 si non définie) */
   public double getBzero() { return bzero; }
   
   /** Retourne la valeur BLANK si elle existe (tester avec hasBlank() */
   public double getBlank() { return blank; }
   
   /** Vrai la valeur BLANK a été définie explicitement */
   public boolean hasBlank() { return !Double.isNaN(blank); }

   /** Positionement d'une valeur BSCALE - si égale à 1, supprime le mot clé du header fits */
   public void setBscale(double bscale) {
      this.bscale=bscale;
      if( headerFits!=null ) headerFits.setKeyValue("BSCALE", bscale+"" );
   }
   
   /** Positionement d'une valeur BZERO - si égale à 0, supprime le mot clé du header fits */
   public void setBzero(double bzero) {
      this.bzero=bzero;
      if( headerFits!=null ) headerFits.setKeyValue("BZERO", bzero+"" );
   }
   
   /** Positionement d'une valeur BLANK. Double.NaN est supporté */
   public void setBlank(double blank) {
      this.blank=blank;
      if( headerFits!=null ) headerFits.setKeyValue("BLANK", Double.isNaN(blank) ? (String)null : blank+"");
   }
   
   /** Positionnement du flag COLORMOD = ARGB pour signifier qu'il s'agit d'un FITS couleur ARGB */
   public void setARGB() { setARGB(true); }
   public void setARGB(boolean reverse) {
      this.flagARGB=true;
      bitpix=0;
      if( headerFits!=null ) headerFits.setKeyValue("COLORMOD", "ARGB");

      // Génération du tableau rgb[] à partir des pixels ARGB stockés dans pixels[]
      rgb = new int[width*height];
      for( int y=0; y<height; y++ ) {
         for( int x=0; x<width; x++ ) {
            int i = y*width+x;
            int offset = reverse ? (height-y-1)*width+x : i;
            rgb[offset] = (pixels[i*4]   & 0xFF)  << 24
            | (pixels[i*4+1] & 0xFF)  << 16
            | (pixels[i*4+2] & 0xFF)  << 8
            | (pixels[i*4+3] & 0xFF) ;
         }
      }
   }

   /**
    * Crée si nécessaire le répertoire correspondant au filename
    * @param filename
    */
   private void createDir(String filename) {
       File dir = new File(filename).getParentFile();
       if( !dir.exists() ) {
           dir.mkdirs();
       }
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS8(OutputStream os) throws Exception {
      headerFits.setKeyValue("BITPIX", bitpix+"");
      headerFits.writeHeader(os);
      os.write(pix8);
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS8(String filename) throws Exception {
        createDir(filename);
        OutputStream os = new FileOutputStream(filename);
        writeFITS8(os);
        os.close();
        this.setFilename(filename);
    }


   static public byte [] getBourrage(int currentPos) {
      int size = 2880 - currentPos%2880;
      byte [] b = new byte[size];
      return b;
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS(OutputStream os) throws Exception {
      headerFits.writeHeader(os);
      int size;

      // FITS couleur en mode ARGB
      if( flagARGB ) {
         byte [] buf = new byte[rgb.length*4];
         for( int i=0; i<rgb.length; i++ ) {
            int pix = rgb[i];
            buf[i*4]   = (byte)( (pix>>24)&0xFF );
            buf[i*4+1] = (byte)( (pix>>16)&0xFF );
            buf[i*4+2] = (byte)( (pix>> 8)&0xFF );
            buf[i*4+3] = (byte)(  pix     &0xFF );
         }
         os.write(buf);
         size = buf.length;
         buf=null;

      // Fits classique
      } else {
         os.write(pixels);
         size=pixels.length;
      }

      // Ecriture des éventuelles extensions
      if( extHeader==null ) return;
      int n = extHeader.size();
      for( int i=0; i<n; i++ ) {
         os.write(getBourrage(size));
         HeaderFits h = (HeaderFits)extHeader.elementAt(i);
         h.writeHeader(os);
         byte [] p = (byte[])extPixels.elementAt(i);
         os.write(p);
         size = p.length;
      }
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS(String filename) throws Exception {
      createDir(filename);
      OutputStream os = new FileOutputStream(filename);
      writeFITS(os);
      os.close();
      this.setFilename(filename);
   }

   private Vector extHeader = null;
   private Vector extPixels = null;

   /** Ajout d'extensions (uniquement des images - ou des cubes) */
   public void addFitsExtension(HeaderFits header,byte [] pixels) {
      if( extHeader==null ) {
         extHeader = new Vector();
         extPixels = new Vector();
      }
      headerFits.setKeyValue("EXTEND","T");
      extHeader.addElement(header);
      extPixels.addElement(pixels);
   }

   /** Suppression de toutes les extensions */
   public void clearExtensions() {
       extHeader = extPixels = null;
   }

   /** Génération d'un fichier JPEG à partir des pixels 8bits
    * Ceux-ci doivent être positionnés manuellement via la méthode
    * setPix8(x,y,val)
    */
   public void writeJPEG(String file) throws Exception { writeJPEG(file,0.95f); }
   public void writeJPEG(String file,float qual) throws Exception {
      FileOutputStream fos = new FileOutputStream(new File(file));
      writeJPEG( fos,qual);
      fos.close();
      this.setFilename(file);
   }

   public void writeJPEG(OutputStream os) throws Exception { writeJPEG(os,0.95f); }
   public void writeJPEG(OutputStream os,float qual) throws Exception {
//	   if (bitpix==0)
//		   inverseYColor();
      Image img = Toolkit.getDefaultToolkit().createImage(
            bitpix==0 ? new MemoryImageSource(width,height, rgb, 0,width)
                       : new MemoryImageSource(width,height,getCM(), pix8, 0,width));

      BufferedImage bufferedImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
      Graphics g = bufferedImage.createGraphics();
      g.drawImage(img,0,0,null);
      g.dispose();

      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam iwp = writer.getDefaultWriteParam();
      iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      iwp.setCompressionQuality(qual);

      ImageOutputStream out = ImageIO.createImageOutputStream(os);
      writer.setOutput(out);
      writer.write(null, new IIOImage(bufferedImage,null,null), iwp);
      writer.dispose();
   }

   static private ColorModel cm = null;

   private ColorModel getCM() {
      if( cm!=null ) return cm;
      byte [] r = new byte[256];
      for( int i=0; i<r.length; i++) r[i]=(byte)i;
      cm = new IndexColorModel(8,256,r,r,r);
      return cm;
   }


   /** Retourne la valeur RGB du pixel dans le cas d'une image couleur RGB
    * => le facteur ALPHA est mis à zéro */
   public int getPixelRGB(int x, int y) {
      return 0x00FFFFFF & rgb[y*width+x];
   }

   /** Retourne la valeur physique (bscale*pix+bzero) du pixel en (x,y) (y compté à partir du bas) sous forme d'un double */
   public double getPixelFull(int x,int y) {
      return bscale*getPixValDouble(pixels,bitpix,y*width+x) + bzero;
   }

   /** Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme d'un double */
   public double getPixelDouble(int x,int y) {
      return getPixValDouble(pixels,bitpix,y*width+x);
   }

   /** Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme d'un entier */
   public int getPixelInt(int x,int y) {
      return getPixValInt(pixels,bitpix,y*width+x);
   }

   /** Retourne la valeur du pixel 8 bits en (x,y) (y compté à partir du bas)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   protected int getPix8FromTop(int x,int y) {
      return getPixValInt(pix8,8, y*width+x);
   }

   /** Positionne la valeur du pixel RGB en (x,y) (y compté à partir du bas) exprimé en ARGB
    * Le facteur Alpha est forcé à 0xFF (pas de transparence) */
   public void setPixelRGB(int x, int y, int val) {
      rgb[y*width+x]=0xFF000000 | val;
   }

   /** Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé en double */
   public void setPixelDouble(int x,int y, double val) {
      setPixValDouble(pixels,bitpix, y*width+x, val);
   }

   /** Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé en entier */
   public void setPixelInt(int x,int y, int val) {
      setPixValInt(pixels,bitpix, y*width+x, val);
   }

   /** Positionne la valeur du pixel 8 bits en (x,y) (y compté à partir du bas)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   public void setPix8(int x,int y, int val) {
      setPixValInt(pix8,8, (height-y-1)*width+x, val);
//      setPixValInt(pix8,8, y*width+x, val);
   }
   
   /**
    * Convertit la valeur double donnée dans le type du bitpix et l'affecte
    * 8 : 0 255
    * 16 : 0 32767
    * 32 : 0 32767
    * -32 : 0 10000
    * -64 : 0 10000
    * @param x
    * @param y
    * @param val
    */
   public void setPixelDoubleFromBitpix(int x, int y, double val, int oldbitpix, double[] oldminmax) {
	   double[] minmax = {0,0};

	   if (oldbitpix == bitpix) {
		   setPixelDouble(x,y,val);
		   return;
	   }
	   
	   double newval = toBitpixRange(val, bitpix, oldminmax);
	   setPixelDouble(x,y,newval);

   }

   public static double toBitpixRange(double val, int bitpix, double[] oldminmax) {
	   double[] minmax = new double[2];
	   minmax = getBitpixRange(bitpix);

	   double r = (minmax[1]-minmax[0])/(oldminmax[1]-oldminmax[0]);
	   double newval = (val-oldminmax[0])*r + minmax[0];
	   return newval;
   }

   static private double[] getBitpixRange(int bitpix) {
	   switch (bitpix) {
	   case 8 :	return minmax8;
	   case 16 : return minmax16;
	   case 32 : return minmax32;
	   case -32 : return minmax_32;
	   case -64 : return minmax_64;
	   default : return null;
	   }
}

/** Retourne la valeur du pixel 8 bits en (x,y) (y compté à partir du bas)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   public int getPix8(int x,int y) {
      return getPixValInt(pix8,8, (height-y-1)*width+x);
   }

   /** Retourne les coordonnées célestes en J2000 de (x,y) (y compté à partir du bas)
    * Le buffer "c" peut être fourni pour éviter des allocations inutiles, null sinon */
   protected double [] getRaDec(double [] c, double x, double y) throws Exception {
      if( c==null ) c = new double[2];
      cTmp.x = x;
      cTmp.y = y;
      calib.GetCoord(cTmp);
      c[0]=cTmp.al;
      c[1]=cTmp.del;
      return c;
   }

   /** Retourne les coordonnées image (x,y) (y compté à partir du bas) en fonction
    * de coordonnées célestes en J2000
    * Le buffer "c" peut être fourni pour éviter des allocations inutiles, null sinon */
   protected double [] getXY(double [] c, double ra, double dec) throws Exception {
      if( c==null ) c = new double[2];
      cTmp.al = ra;
      cTmp.del = dec;
      calib.GetXY(cTmp);
      c[0]=cTmp.x;
      c[1]=cTmp.y;
      return c;
   }

//   /** Effectue un autocut logarithmique des pixels "fullbits" et une conversion en 8bits dans
//    * le tableau pix8 []. Utilise la méthode "à la Aladin" pour déterminer
//    * le meilleur intervalle.
//    */
//   public void autocutLog() throws Exception {
//      double [] range = findAutocutRange();
//      toPix8Log(range[0],range[1]);
//   }
   
   public void cut() throws Exception {
	   double[] range = {Double.MIN_VALUE,Double.MAX_VALUE};
	   switch (bitpix) {
	   case 8:
		   range[0] = 0;
		   range[1] = 255;
	   case 16:
		   range[0] = 0;
		   range[1] = Short.MAX_VALUE;
	   case 32:
		   range[0] = 0;
		   range[1] = Integer.MAX_VALUE;
	   case -32:
		   range[0] = Float.MIN_VALUE;
		   range[1] = Float.MAX_VALUE;
	   case -64:
		   range[0] = Double.MIN_VALUE;
		   range[1] = Double.MAX_VALUE;

	   }
	   toPix8(range[0],range[1]);
   }
   /** Effectue un autocut linéaire des pixels "fullbits" et une conversion en 8bits dans
    * le tableau pix8 []. Utilise la méthode "à la Aladin" pour déterminer
    * le meilleur intervalle.
    */
   public void autocut8() throws Exception {
      double [] range = findAutocutRange();
      toPix8(range[0],range[1]);
   }
   /** 
    * Effectue un autocut linéaire des pixels "fullbits" (modifie pixels). 
    * Utilise la méthode "à la Aladin" pour déterminer le meilleur intervalle.
    */
   public void autocut() throws Exception {
      double [] range = findAutocutRange();
//      System.out.println("RANGE = " +range[0] + " " + range[1]);
      cutOut(range[0],range[1]);
   }

   /** Calcule un autocut linéaire des pixels "fullbits"
    * Utilise la méthode "à la Aladin" pour déterminer
    * le meilleur intervalle.
    */
   public static double[] autocut(String filename) throws Exception {
	   Fits f = new Fits();
	   f.loadFITS(filename);
      return f.findAutocutRange();
   }

   /** Ecrase le tableau des pixels (pixels) selon l'intervalle [min..max]
    * depuis les valeurs des pixels originaux suivant une fonction linéaire
    * et les étend entre [0;999]
    * */
   public void cutOut(double min, double max) {
	   double[] oldrange = findMinMax();
	   double r = 1000./(oldrange[1]-oldrange[0]);
	   if (bitpix>0) {
		   for( int y=0; y<height; y++) {
			   for( int x=0; x<width; x++ ) {
				   int pixIn = getPixelInt(x,y);
				   int pixOut = pixIn<=min?0:pixIn>=max ?
						   999 : (int)((pixIn-min+1)*r);
				   setPixelInt(x,y,pixOut);
			   }
		   }
	   }
	   else {
		   for( int y=0; y<height; y++) {
			   for( int x=0; x<width; x++ ) {
				   double pixIn = getPixelFull(x,y);
				   double pixOut = pixIn<=min?0:pixIn>=max ?
						   999 : (pixIn-min+1)*r;
				   setPixelDouble(x,y,pixOut);
			   }
		   }
	   }
   }

   /** Ecrase le tableau des pixels (pixels) sur 256 niveaux en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] suivant une fonction logarithme
    * */
   /*
   public void cutOutLog(double min, double max) {
	   double r = 256./Math.log( (max-min) +1 );
	   if (bitpix>0) {
		   for( int y=0; y<height; y++) {
	    		  for( int x=0; x<width; x++ ) {
	    			  int pixIn = getPixelInt(x,y);
	    			  int pixOut = ( pixIn<=min?0:pixIn>=max ?
	    					  255 : (int) (Math.log( (pixIn-min+1) )*r) );
	    			  setPixelInt(x,y,pixOut);
	    		  }
	    	  }
	      }
	      else {
	       	  for( int y=0; y<height; y++) {
	    		  for( int x=0; x<width; x++ ) {
	    			  double pixIn = getPixelDouble(x,y);
	    			  double pixOut = ( pixIn<=min?0:pixIn>=max ?
	    					  255 : Math.log( (pixIn-min+1) )*r );
	    			  setPixelDouble(x,y,pixOut);
	    		  }
	    	  }
	      }
   }
   */

   /** Remplit le tableau des pixels 8 bits (pix8)  */
   public void toPix8() {
	   double[] minmax = findMinMax();
	   toPix8(minmax[0], minmax[1]);
//	   toPix8(0, 256);
   }


   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] linéairement */
   public void toPix8(double min, double max) {
      double r = 256./(max - min);
      for( int y=0; y<height; y++) {
         for( int x=0; x<width; x++ ) {
            double pixIn = getPixelDouble(x,y);
            byte pixOut = (byte)( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            setPix8(x,y,pixOut);
         }
      }
   }
   
   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] et de la table des couleurs cm.
    * S'il y a usage d'une fonction de transfert, elle est déjà appliquée à la table cm (rien à faire de plus)
    * Si la table cm n'est pas monochrome, l'algo travaille uniquement sur la composante bleue */
   public void toPix8(double min, double max, ColorModel cm) {
      double r = 256./(max - min);
      for( int y=0; y<height; y++) {
         for( int x=0; x<width; x++ ) {
            double pixIn = getPixelDouble(x,y);
            int pix = ( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            byte pixOut =(byte) cm.getBlue(pix);
            setPix8(x,y,pixOut);
         }
      }
   }

   
//   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
//    * des valeurs des pixels originaux [min..max] suivant une fonction logarithme */
//   public void toPix8Log(double min, double max) {
//	   double r = 256./Math.log( (max-min) +1 );
//	   for( int y=0; y<height; y++) {
//		   for( int x=0; x<width; x++ ) {
//			   double pixIn = getPixelDouble(x,y);
//			   byte pixOut = (byte)( pixIn<=min?0x00:pixIn>=max ?
//					   0xff : (int)( Math.log((pixIn/10.+1-min))*r ) & 0xff);
//			   setPix8(x,y,pixOut);
//		   }
//	   }
//   }
//   

//   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
//    * des valeurs des pixels originaux [min..max] suivant une fonction logarithme */
//   public void toPix8Pow(double min, double max) {
//	   double r = 256./Math.pow( (max-min),2 );
//	   for( int y=0; y<height; y++) {
//		   for( int x=0; x<width; x++ ) {
//			   double pixIn = getPixelDouble(x,y);
//			   byte pixOut = (byte)( pixIn<=min?0x00:pixIn>=max ?
//					   0xff : (int)( Math.pow((pixIn-min),2)*r ) & 0xff);
//			   setPix8(x,y,pixOut);
//		   }
//	   }
//   }
//
//   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
//    * des valeurs des pixels originaux [min..max] suivant une fonction logarithme */
//   public void toPix8Sqrt(double min, double max) {
//	   double r = 256./Math.sqrt( (max-min));
//	   for( int y=0; y<height; y++) {
//		   for( int x=0; x<width; x++ ) {
//			   double pixIn = getPixelDouble(x,y);
//			   byte pixOut = (byte)( pixIn<=min?0x00:pixIn>=max ?
//					   0xff : (int)( Math.sqrt((pixIn/10.-min))*r ) & 0xff);
//			   setPix8(x,y,pixOut);
//		   }
//	   }
//   }
//
//   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
//    * des valeurs des pixels originaux [min..max] suivant une fonction logarithme */
//   public void toPix8ASinH(double min, double max) {
//	   double r = 256./Math.log( (max-min) + Math.sqrt(Math.pow((max-min), 2.)+1) );
//	   for( int y=0; y<height; y++) {
//		   for( int x=0; x<width; x++ ) {
//			   double pixIn = getPixelDouble(x,y);
//			   byte pixOut = (byte)( pixIn<=min?0x00:pixIn>=max ?
//					   0xff : (int)(
//							   Math.log((pixIn/10.-min)+Math.sqrt(Math.pow((pixIn/10.-min), 2.)+1))*r 
//							   ) & 0xff);
//			   setPix8(x,y,pixOut);
//		   }
//	   }
//   }
   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] linéairement */
   public void toFullPix8(double min, double max) {
      double r = 256./(max - min);
      for( int y=0; y<height; y++) {
         for( int x=0; x<width; x++ ) {
            double pixIn = getPixelFull(x,y);
            byte pixOut = (byte)( pixIn<=min?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            setPix8(x,y,pixOut);
         }
      }
   }

   /** Remplit le tableau des pixels 8 bits (pix8) à partir des pixels
    * fullbits (uniquement si bitpix==8) avec inversion des lignes */
   public void initPix8() throws Exception {
      if( bitpix!=8 ) throw new Exception("bitpix!=8");
      initPix(pixels,pix8);
   }

   /** Remplit le tableau des pixels Fits (pixels) à partir des pixels
    * 8bits (uniquement si bitpix==8) avec inversion des lignes */
   public void initPixFits() throws Exception {
      if( bitpix!=8 ) throw new Exception("bitpix!=8");
      initPix(pix8,pixels);
   }

   private void initPix(byte src[],byte dst[]) {
      for( int h=0; h<height; h++ ){
         System.arraycopy(src,h*width, dst,(height-h-1)*width, width);
      }
   }

   /** Détermine l'intervalle pour un autocut "à la Aladin".
    * @return range[0]..[1] => minPixCut..maxPixCut
    *         range[2]..[3] => minPix..maxPix
    */
   public double [] findFullAutocutRange() throws Exception {
      double[] cut = findAutocutRange(0,0);
      cut[0] = bscale * cut[0] + bzero;
      cut[1] = bscale * cut[1] + bzero;
      return cut;
   }

   /** Détermine l'intervalle pour un autocut "à la Aladin".
    * @return range[0]..[1] => minPixCut..maxPixCut
    *         range[2]..[3] => minPix..maxPix
    */
   public double [] findAutocutRange() throws Exception {
      return findAutocutRange(0,0);
   }
   
   /** Détermine l'intervalle pour un autocut "à la Aladin" en ne considérant
    * que les valeurs comprises entre min et max
    * @return range[0]..[1] => minPixCut..maxPixCut
    *         range[2]..[3] => minPix..maxPix
    */
   public double [] findAutocutRange(double min, double max) throws Exception {
      double [] range = new double[4];
      try {
         findMinMax(range,pixels,bitpix,width,height,min,max,true,0);
      } catch (Exception e) {
         System.err.println("Erreur  MinMax");
         range[0] = range[2] = min;
         range[1] = range[3] = max;
      }
      return range;
   }
   
   /** Retourne true si la valeur du pixel est blank ou NaN */
   public boolean isBlankPixel(double pix) {
      return Double.isNaN(pix) || !Double.isNaN(blank) && pix==blank;
   }

   public double[] findMinMax() {
		 boolean first=true;
        long nmin=0,nmax=0;
        double c;
        double max = 0, max1 = 0;
        double min = 0, min1 = 0;
        int n = pixels.length/ (Math.abs(bitpix)/8);
        for(int i=0; i<n; i++ ) {
           c = getPixValDouble(pixels,bitpix,i);
           if( isBlankPixel(c) ) continue;

           if( first ) { max=max1=min=min1=c; first=false; }

           if( min>c ) { min=c; nmin=1; }
           else if( max<c ) { max=c; nmax=1; }
           else {
              if( c==min ) nmin++;
              if( c==max ) nmax++;
           }

           if( c<min1 && c>min || min1==min && c<max1 ) min1=c;
           else if( c>max1 && c<max || max1==max && c>min1 ) max1=c;
        }
        double[] minmax= new double[] {min,max};
		return minmax;
	}


   /** Creation des tableaux de correspondance indice Healpix <=> indice XY */
   static private void createHealpixOrder(int order) {
      int nsize = (int)CDSHealpix.pow2(order);
      xy2hpx = new int[nsize*nsize];
      hpx2xy = new int[nsize*nsize];
      fillUp(xy2hpx,nsize,null);
      for( int i=0; i<xy2hpx.length; i++ ) hpx2xy[ xy2hpx[i] ] = i;
   }

   /** Retourne l'indice XY en fonction d'un indice Healpix
    * => nécessité d'initialiser au préalable avec createHealpixOrdre(int) */
   static final public int hpx2xy(int hpxOffset) {
      return xy2hpx[hpxOffset];
   }

   /** Retourne l'indice Healpix en fonction d'un indice XY
    * => nécessité d'initialiser au préalable avec createHealpixOrdre(int) */
   static final public int xy2hpx (int xyOffset) {
      return hpx2xy[xyOffset];
   }

   static private int [] xy2hpx = null;
   static private int [] hpx2xy = null;

   /** Méthode récursive utilisée par createHealpixOrder */
   static private void fillUp(int [] npix, int nsize, int [] pos) {
      int size = nsize*nsize;
      int [][] fils = new int[4][size/4];
      int [] nb = new int[4];
      for( int i=0; i<size; i++ ) {
         int dg = (i%nsize) < (nsize/2) ? 0 : 1;
         int bh = i<(size/2) ? 0 : 1;
         int quad = (bh<<1) | dg;
         int j = pos==null ? i : pos[i];
         npix[j] = npix[j]<<2 | quad;
         fils[quad][ nb[quad]++ ] = j;
      }
      if( size>4 )  for( int i=0; i<4; i++ ) fillUp(npix, nsize/2, fils[i]);
   }


	/** Pour aider le GC */
   public void free() {
      pixels =null;
      pix8 = null;
      calib=null;
      center=null;
      headerFits=null;
      width=height=bitpix=0;
   }

   @Override
public String toString() {
      return "Fits file: "+width+"x"+height+" bitpix="+bitpix;
   }

   // -------------------- C'est privé --------------------------


  /**
   * Détermination du min et max des pixels passés en paramètre
   * @param En sortie : range[0]=minPixCut, range[1]=maxPixCut, range[2]=minPix, range[3]=maxPix;
   * @param pIn Tableau des pixels à analyser
   * @param bitpix codage FITS des pixels
   * @param width Largeur de l'image
   * @param height hauteur de l'image
   * @param minCut Limite min, ou 0 si aucune
   * @param maxCut limite max, ou 0 si aucune
   * @param autocut true si on doit appliquer l'autocut
   * @param ntest Nombre d'appel en cas de traitement récursif.
   */
   private void findMinMax(double [] range, byte[] pIn, int bitpix, int width, int height,
            double minCut,double maxCut,boolean autocut,int ntest) throws Exception {
     int i,j,k;
     boolean flagCut=(ntest>0 || minCut!=0. && maxCut!=0.);

     //   Recherche du min et du max
     double max = 0, max1 = 0;
     double min = 0, min1 = 0;

     //   Marge pour l'échantillonnage (on recherche min et max que sur les 1000 pixels centraux en
     //   enlevant éventuellement un peu de bord
     int MARGEW=(int)(width*0.05);
     int MARGEH=(int)(height*0.05);

     //   LES DEUX LIGNES QUI SUIVENT SONT A COMMENTER SI ON VEUT ETRE SUR DE NE PAS LOUPER
     //   DES PARTICULARITES LOCALES SUR LES GROSSES IMAGES.
     if( width - 2*MARGEW>1000 ) MARGEW = (width-1000)/2;
     if( height - 2*MARGEH>1000 ) MARGEH = (height-1000)/2;

     double c;

     if( !autocut && (minCut!=0. || maxCut!=0.) ) {
        range[2]=min=minCut;
        range[3]=max=maxCut;

     } else {
        boolean first=true;
        long nmin=0,nmax=0;
        for( i=MARGEH; i<height-MARGEH; i++ ) {
           for( j=MARGEW; j<width-MARGEW; j++ ) {
              c = getPixValDouble(pIn,bitpix,i*width+j);

              // On ecarte les valeurs sans signification
              if( isBlankPixel(c) ) continue;

              if( flagCut ) {
                 if( c<minCut || c>maxCut ) continue;
              }

              if( first ) { max=max1=min=min1=c; first=false; }

              if( min>c ) { min=c; nmin=1; }
              else if( max<c ) { max=c; nmax=1; }
              else {
                 if( c==min ) nmin++;
                 if( c==max ) nmax++;
              }

              if( c<min1 && c>min || min1==min && c<max1 ) min1=c;
              else if( c>max1 && c<max || max1==max && c>min1 ) max1=c;
           }
        }

        if( autocut && max-min>256 ) {
           if( min1-min>max1-min1 && min1!=Double.MAX_VALUE && min1!=max ) min=min1;
           if( max-max1>max1-min1 && max1!=Double.MIN_VALUE && max1!=min  ) max=max1;
        }
        range[2]=min;
        range[3]=max;
     }

     if( autocut ) {
        int nbean = 10000;
        double l = (max-min)/nbean;
        int[] bean = new int[nbean];
        for( i=MARGEH; i<height-MARGEH; i++ ) {
           for( k=MARGEW; k<width-MARGEW; k++) {
              c = getPixValDouble(pIn,bitpix,i*width+k);

              j = (int)((c-min)/l);
              if( j==bean.length ) j--;
              if( j>=bean.length || j<0 ) continue;
              bean[j]++;
           }
        }

        //      Selection du min et du max en fonction du volume de l'information
        //      que l'on souhaite garder
        int [] mmBean = getMinMaxBean(bean);

        //      Verification que tout s'est bien passe
        if( mmBean[0]==-1 || mmBean[1]==-1 ) {
           throw new Exception("beaning error");
        } else {
           min1=min;
           max1 = mmBean[1]*l+min1;
           min1 += mmBean[0]*l;
        }

        if( mmBean[0]!=-1 && mmBean[0]>mmBean[1]-5 && ntest<3 ) {
           if( min1>min ) min=min1;
           if( max1<max ) max=max1;
           findMinMax(range,pIn,bitpix,width,height,min,max,autocut, ntest+1);
           return;
        }

        min=min1; max=max1;
     }

     //   Memorisation des parametres de l'autocut
     range[0]=min;
     range[1]=max;
  }

  /** Determination pour un tableau de bean[] de l'indice du bean min
   * et du bean max en fonction d'un pourcentage d'information desire
   * @param bean les valeurs des beans provenant de l'analyse d'une image
   * @return mmBean[2] qui contient les indices du bean min et du bean max
   */
  private int[] getMinMaxBean(int [] bean) {
     double minLimit=0.003;    // On laisse 3 pour mille du fond
     double maxLimit=0.9995;    // On laisse 1 pour mille des etoiles
     int totInfo;          // Volume de l'information
     int curInfo;          // Volume courant en cours d'analyse
     int [] mmBean = new int[2];   // indice du bean min et du bean max
     int i;

     // Determination du volume de l'information
     for( totInfo=i=0; i<bean.length; i++ ) {
        totInfo+=bean[i];
     }

     // Positionnement des indices des beans min et max respectivement
     // dans mmBean[0] et mmBean[1]
     for( mmBean[0]=mmBean[1]=-1, curInfo=i=0; i<bean.length; i++ ) {
        curInfo+=bean[i];
        double p = (double)curInfo/totInfo;
        if( mmBean[0]==-1 ) {
           if( p>minLimit ) mmBean[0]=i;
        } else if( p>maxLimit ) { mmBean[1]=i; break; }
     }

     return mmBean;
  }

  private Coord cTmp = new Coord();
  private String filename;


   private void setPixValInt(byte[] t,int bitpix,int i,int val) {
      int c;
      switch(bitpix) {
         case   8: t[i]=(byte)(0xFF & val);
                   break;
         case  16: i*=2;
                   c = val;
                   t[i]  =(byte)(0xFF & (c>>8));
                   t[i+1]=(byte)(0xFF & c);
                   break;
         case  32: i*=4;
                   setInt(t,i,val);
                   break;
         case -32: i*=4;
                   c=Float.floatToIntBits(val);
                   setInt(t,i,c);
                   break;
         case -64: i*=8;
                   long c1 = Double.doubleToLongBits(val);
                   c = (int)(0xFFFFFFFFL & (c1>>>32));
                   setInt(t,i,c);
                   c = (int)(0xFFFFFFFFL & c1);
                   setInt(t,i+4,c);
                   break;
      }
   }

   protected void setPixValDouble(byte[] t,int bitpix,int i,double val) {
      int c;
      switch(bitpix) {
         case   8: t[i]=(byte)(0xFF & (int)val);
                   break;
         case  16: i*=2;
                   c = (int)val;
                   t[i]  =(byte)(0xFF & (c>>8));
                   t[i+1]=(byte)(0xFF & c);
                   break;
         case  32: i*=4;
                   setInt(t,i,(int)val);
                   break;
         case -32: i*=4;
                   c=Float.floatToIntBits((float)val);
                   setInt(t,i,c);
                   break;
         case -64: i*=8;
                   long c1 = Double.doubleToLongBits(val);
                   c = (int)(0xFFFFFFFFL & (c1>>>32));
                   setInt(t,i,c);
                   c = (int)(0xFFFFFFFFL & c1);
                   setInt(t,i+4,c);
                   break;
      }
   }

   private int getPixValInt(byte[]t ,int bitpix,int i) {
      try {
         switch(bitpix) {
            case   8: return (t[i])&0xFF;
            case  16: i*=2;
            return  ((t[i])<<8) | (t[i+1])&0xFF ;
            case  32: return getInt(t,i*4);
            case -32: return (int)Float.intBitsToFloat(getInt(t,i*4));
            case -64: i*=8;
            long a = (((long)getInt(t,i))<<32)
            | ((getInt(t,i+4)) & 0xFFFFFFFFL);
            return (int)Double.longBitsToDouble(a);
         }
         return 0;
      } catch( Exception e ) { return 0; }
   }

   private double getPixValDouble(byte[]t ,int bitpix,int i) {
      try {
         switch(bitpix) {
            case   8: return ((t[i])&0xFF);
            case  16: i*=2;
            return ( ((t[i])<<8) | (t[i+1])&0xFF );
            case  32: return getInt(t,i*4);
            case -32: return Float.intBitsToFloat(getInt(t,i*4));
            case -64: i*=8;
            long a = (((long)getInt(t,i))<<32)
            | ((getInt(t,i+4)) & 0xFFFFFFFFL);
            return Double.longBitsToDouble(a);
         }
         return 0.;
      } catch( Exception e ) { return DEFAULT_BLANK; }
   }

   private void setInt(byte[] t,int i,int val) {
      t[i]   = (byte)(0xFF & (val>>>24));
      t[i+1] = (byte)(0xFF & (val>>>16));
      t[i+2] = (byte)(0xFF & (val>>>8));
      t[i+3] = (byte)(0xFF &  val);
   }

   private int getInt(byte[] t,int i) {
      return ((t[i])<<24) | (((t[i+1])&0xFF)<<16)
              | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF;
   }

   /**
    * Inverse les lignes d'une image couleur (tableau rgb)
    */
   public void inverseYColor() {
	   int[] tmp = new int[rgb.length];
	   for( int h=0; h<height; h++ ){
		   System.arraycopy(rgb,h*width, tmp,(height-h-1)*width, width);
	   }
	   rgb = tmp;
   }
   
   /** Coadditionne les pixels (pix8[], pixels[] et rgb[] */
   public void coadd(Fits a) throws Exception {
      int taille=width*height;
      
      if( a.pixels!=null && pixels!=null ) {
         for( int i=0; i<taille; i++) {
            double v1 = getPixValDouble(pixels,bitpix,i);
            double v2 = a.getPixValDouble(a.pixels,bitpix,i);
            double v = (v1+v2)/2;
            setPixValDouble(pixels, bitpix, i, v);
         }
      }
      if( a.pix8!=null && pix8!=null ) {
         for( int i=0; i<taille; i++) {
            pix8[i] = (byte)(0xFF & ( (int)pix8[i]+ (int)a.pix8[i] )/2 );
         }
      }
      if( a.rgb!=null && rgb!=null ) {
         for( int i=0; i<taille; i++) {
            int r  = (0x00FF0000 & a.rgb[i]) + (0x00FF0000 & rgb[i]) /2;
            int g  = (0x0000FF00 & a.rgb[i]) + (0x0000FF00 & rgb[i]) /2;
            int b  = (0x000000FF & a.rgb[i]) + (0x000000FF & rgb[i]) /2;
            rgb[i] = (0xFF000000 & rgb[i]) | r | g | b;
         }
      }
   }

   /**
    * Cherche un fichier de type FITS dans le répertoire donné
    * @param aladinTree
    * @return null s'il y a eu une erreur ou le chemin du 1er fichier fits trouvé
    */
   public static Fits getFits(String rootpath) {
	   File main = new File(rootpath);
	   Fits fitsfile = new Fits();
	   String[] list = main.list();
	   String path = rootpath;
	   if (list==null)
		   return null;
	   for (int f = 0 ; f < list.length ; f++) {
		   if (!rootpath.endsWith(FS)) {
			   rootpath = rootpath+FS;
		   }
		   path = rootpath+list[f];
		   if ((new File(path)).isDirectory()) {
			   if (!list[f].equals(AllskyConst.SURVEY)) {
				   Fits f1 = getFits(path);
				   if( f1!=null ) return f1;
			   }
			   else {
				   continue;
			   }
		   }
		   try {
			   // essaye de lire l'entete du fichier comme un fits
			   fitsfile.loadHeaderFITS(path);
			   // il n'y a pas eu d'erreur, donc c'est bien un FITS
			   fitsfile.loadFITS(path);
			   return fitsfile;
		   }  catch (Exception e) {
//			   e.printStackTrace();
			   System.err.println("Fits header error : " + path);
			   continue;
		   }
	   }
	   return null;
   }

public void setFilename(String filename) {
	this.filename = filename;
}

public String getFilename() {
	return filename;
}
   

}
