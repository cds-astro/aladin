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
import java.util.StringTokenizer;
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
import cds.allsky.Constante;
import cds.image.Hdecomp;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/**
 * Classe de manipulation d'un fichier image FITS
 */
final public class Fits {
	public static final double DEFAULT_BLANK = Double.NaN;
	public static final double DEFAULT_BSCALE = 1.;
	public static final double DEFAULT_BZERO = 0.;
	public HeaderFits headerFits;    // header Fits
	public byte [] pixels;           // Pixels d'origine "fullbits" (y compté depuis le bas)
	public int bitpix;               // Profondeur des pixels (codage FITS 8,16,32,-32,-64)
	public int width;               // Largeur totale de l'image
	public int height;               // Hauteur totale de l'image
	public double bzero=DEFAULT_BZERO;	// BZERO Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double bscale=DEFAULT_BSCALE;	// BSCALE Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double blank=DEFAULT_BLANK;  // valeur BLANK
	public boolean flagARGB=false;   // true s'il s'agit d'un FITS couleur ARGB

	// Dans le cas où il s'agit d'une cellule sur l'image (seule une portion de l'image sera accessible)
    public int xCell;                // Position X à partir du coin haut gauche de la cellule de l'image (par défaut 0)
    public int yCell;                // Position Y à partir du coin haut gauche de la cellule de l'image (par défaut 0)
    public int widthCell;                // Largeur de la cellule de l'image (par défaut = naxis1)
    public int heightCell;                // Hauteur de la cellule de l'image (par défaut = naxis2)

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
      this.width=this.widthCell=width;
      this.height=this.heightCell=height;
      this.bitpix=bitpix;
      xCell=yCell=0;
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


//   public double raMin=0,raMax=0,deMin=0,deMax=0;
   
   /** Positionnement d'une calibration => initialisation de la coordonnée
    * centrale de l'image (cf center)
    */
   public void setCalib(Calib c) {
      calib=c;
      initCenter();
   }
   
   public void initCenter() {
      try {
         center = new Coord();
         center.x = xCell+widthCell/2.;
         center.y = height-(yCell+heightCell/2.);
         calib.GetCoord(center);
         
//         int margex = width/20;
//         int margey = height/20;
//         if( margex>=widthCell ) margex=widthCell/2;
//         if( margey>=heightCell ) margey=heightCell/2;
//
//         Coord coo = new Coord();
//         for( int i=0; i<4; i++ ) {
//            
//            coo.x= i==0  || i==3 ? (xCell==0?margex:0) : widthCell-(widthCell==width?margex:0);
//            coo.y= i<2 ? (yCell==0?margey:0) : heightCell-(heightCell==height?margey:0);
//            calib.GetCoord(coo);
//            if( i==0 || (coo.al<raMin && Math.abs(coo.al-raMin)<300) ) raMin=coo.al;
//            if( i==0 || (coo.al>raMax && Math.abs(coo.al-raMax)<300) ) raMax=coo.al;
//            if( i==0 || (coo.del<deMin && Math.abs(coo.del-deMin)<300) ) deMin=coo.del;
//            if( i==0 || (coo.del>deMax && Math.abs(coo.del-deMax)<300) ) deMax=coo.del;
//         }
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
      width=widthCell =img.getWidth(observer);
      height=heightCell=img.getHeight(observer);
      xCell=yCell=0;
      bitpix= flagColor ? 0 : 8;
      
      BufferedImage imgBuf = new BufferedImage(widthCell,heightCell, 
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
         pix8 = new byte[widthCell*heightCell];
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
   
   
   // Extraction de la définition d'une cellule FITS pour une ouverture d'un fichier FITS en mode "mosaic"
   // le filename doit être suffixé par [x,y-wxh] (sans aucun blanc).
   // => met à jour les variables xCell, yCell, widthCell et heigthCell
   // S'il n'y a pas de définition de cellule, laisse xCell et yCell à 0 et widthCell et heightCell à -1
   // @return le nom de fichier sans le suffixe
   private String parseCell(String filename) throws Exception {
      xCell=yCell=0;
      widthCell=heightCell=-1;
      int deb = filename.lastIndexOf('[');
      if( deb==-1 ) return filename;
      int fin = filename.indexOf(']',deb);
      if( fin==-1 ) return filename;
      StringTokenizer st = new StringTokenizer(filename.substring(deb+1, fin),",-x");
      try {
         xCell = Integer.parseInt(st.nextToken());
         yCell = Integer.parseInt(st.nextToken());
         widthCell = Integer.parseInt(st.nextToken());
         heightCell = Integer.parseInt(st.nextToken());
      } catch( Exception e ) {
         throw new Exception("Bad cell mosaic FITS definition => "+filename); 
      }
      return filename.substring(0,deb);
   }


   /** Chargement d'une image FITS N&B depuis un fichier */
   public void loadFITS(String filename) throws Exception {loadFITS(filename,false);}
   public void loadFITS(String filename, boolean color) throws Exception {
      filename = parseCell(filename);   // extraction de la descrition d'une cellule éventuellement en suffixe du nom fichier.fits[x,y-wxh]
      MyInputStream is = new MyInputStream( new FileInputStream(filename));
      is = is.startRead();
      if( color ) {
         if( widthCell<0 ) throw new Exception("Mosaic mode not supported yet for FITS color file");
         loadFITSColor(is);
      }
      else loadFITS(is,xCell,yCell,widthCell,heightCell);
      is.close();
      this.setFilename(filename);
   }
   
   /** Chargement d'une image FITS */
   public void loadFITS(MyInputStream dis) throws Exception {
      loadFITS(dis,0,0,-1,-1);
   }

   /** Chargement d'une cellule d'une image FITS */
   public void loadFITS(MyInputStream dis,int x,int y,int w, int h) throws Exception {
	   dis = dis.startRead();
	   boolean flagHComp = (dis.getType() & MyInputStream.HCOMP) !=0;
	   headerFits = new HeaderFits(dis);
	   bitpix = headerFits.getIntFromHeader("BITPIX");
	   width  = headerFits.getIntFromHeader("NAXIS1");
	   height = headerFits.getIntFromHeader("NAXIS2");
	   
	   // Ouverture complète de l'image
	   if( w==-1 ) {
	      widthCell=width;
	      heightCell=height;
	      xCell=yCell=0;
	      
	   // Ouverture juste d'une cellule de l'image
	   } else {
	      if( x<0 || y<0 || x+w>width || y+h>height ) throw new Exception("Mosaic cell outside the image ("+width+"x"+height+") cell=["+x+","+y+" "+w+"x"+h+"]");
	      widthCell=w;
	      heightCell=h;
	      xCell=x;
	      yCell=y;
	   }
	   try { blank = headerFits.getDoubleFromHeader("BLANK");} catch( Exception e ) { blank=DEFAULT_BLANK; }
	   
       int n = (Math.abs(bitpix)/8);
	   
	   // Pas le choix, il faut d'abord tout lire, puis ne garder que la cellule si nécessaire
       if( flagHComp ) {
          byte [] buf = Hdecomp.decomp(dis);
          if( w==-1 ) pixels=buf;
          else {
             pixels = new byte[widthCell*heightCell * n];
             for( int lig=0; lig<heightCell; lig++) System.arraycopy(buf, (lig*width+xCell)*n, pixels, lig*widthCell*n, widthCell*n);
          }
          
       } else {
          pixels = new byte[widthCell*heightCell * n];

          // Lecture d'un coup
          if( w==-1 ) dis.readFully(pixels);

          // Lecture ligne à ligne pour mémoriser uniquement la cellule
          else {
             dis.skip( yCell*width*n);
             byte [] buf = new byte[width * n];  // une ligne complète
             for( int lig=0; lig<heightCell; lig++ ) {
                dis.readFully(buf);
                System.arraycopy(buf, xCell*n, pixels,lig*widthCell*n, widthCell*n);
             }
             dis.skip( (height-(yCell+heightCell) )*width * n);
          }

       }
	   try { bscale = headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
	   try { bzero  = headerFits.getDoubleFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
	   try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { calib=null; }
	   pix8 = new byte[widthCell*heightCell];
	   if( bitpix==8 ) initPix8();
   }
   
   /** Chargement d'une image FITS couleur mode ARGB */
   public void loadFITSARGB(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      width=widthCell  = headerFits.getIntFromHeader("NAXIS1");
      height=heightCell = headerFits.getIntFromHeader("NAXIS2");
      xCell=yCell=0;
      pixels = new byte[widthCell*heightCell*32];
      dis.readFully(pixels);
      setARGB();
      try { setCalib(new Calib(headerFits)); }
      catch( Exception e ) { calib=null; }
   }
   
   /** Chargement d'une image FITS couleur mode RGB cube */
   public void loadFITSColor(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      bitpix = headerFits.getIntFromHeader("BITPIX");
      width=widthCell  = headerFits.getIntFromHeader("NAXIS1");
      height=heightCell = headerFits.getIntFromHeader("NAXIS2");
      xCell=yCell=0;
      pixels = new byte[widthCell*heightCell];
      dis.readFully(pixels);

      byte[] t2 = new byte[widthCell*heightCell];
      dis.readFully(t2);
      byte[] t3 = new byte[widthCell*heightCell];
      dis.readFully(t3);
      rgb = new int[widthCell*heightCell];
      for( int i=0; i<heightCell*widthCell; i++ ){
         int val = 0;
         val = 0 | (((pixels[i])&0xFF)<<16) | (((t2[i])&0xFF)<<8) | (t3[i])&0xFF;
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
      
      // Cas spécial d'un fichier .hhhh
      if( filename.endsWith(".hhh") ) {
         byte [] buf = is.readFully();
         headerFits = new HeaderFits();
         headerFits.readFreeHeader(new String(buf), true, null);
         
      // Cas habituel
      } else headerFits = new HeaderFits(is);
      
      try {
    	  bitpix = headerFits.getIntFromHeader("BITPIX");
      } catch (Exception e1) {
    	  bitpix = 0;
      }
//      if( bitpix!=0 ) bitpix = headerFits.getIntFromHeader("BITPIX");
      width=widthCell  = headerFits.getIntFromHeader("NAXIS1");
      height=heightCell = headerFits.getIntFromHeader("NAXIS2");
      xCell=yCell=0;
      try { blank = headerFits.getDoubleFromHeader("BLANK");} catch( Exception e ) { blank=DEFAULT_BLANK; }
      try { bscale = headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
      try { bzero  = headerFits.getDoubleFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
      try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { calib=null; }
      is.close();
      this.setFilename(filename);
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
      rgb = new int[widthCell*heightCell];
      for( int y=0; y<heightCell; y++ ) {
         for( int x=0; x<widthCell; x++ ) {
            int i = y*widthCell+x;
            int offset = reverse ? (heightCell-y-1)*widthCell+x : i;
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
      createDir(file);
      FileOutputStream fos = new FileOutputStream(new File(file));
      writeJPEG( fos,qual);
      fos.close();
      this.setFilename(file);
   }

   public void writeJPEG(OutputStream os) throws Exception { writeJPEG(os,0.95f); }
   public void writeJPEG(OutputStream os,float qual) throws Exception {
      Image img = Toolkit.getDefaultToolkit().createImage(
            bitpix==0 ? new MemoryImageSource(widthCell,heightCell, rgb, 0,widthCell)
                       : new MemoryImageSource(widthCell,heightCell,getCM(), pix8, 0,widthCell));

      BufferedImage bufferedImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
      Graphics g = bufferedImage.createGraphics();
      g.drawImage(img,xCell,yCell,null);
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
      return 0x00FFFFFF & rgb[ (y-yCell)*widthCell + (x-xCell) ];
   }
   
   public int getPixelRGBJPG(int x, int y) {
      return 0x00FFFFFF & rgb[ ((height-y-1)-yCell)*widthCell+(x-xCell) ];
   }
   
  
   /** Retourne la description de la cellule courante selon la syntaxe [x,y-wxh]
    * ou "" si le fichier n'a pas été ouvert en mode mosaic */
   public String getCellSuffix() throws Exception {
      if( !hasCell() ) return "";
      return "["+xCell+","+yCell+"-"+widthCell+"x"+heightCell+"]";
   }
   
   /** Retourne true si le fichier a été ouvert en mode mosaic */
   public boolean hasCell() { return widthCell!=width || heightCell!=height; }
   
   /** Retourne true s'il y a un pixel connu à la position x,y.
    * Cela concerne le mode mosaic FITS, où l'on peut ouvrir juste une cellule sur le fichier
    * Dans le cas général, cette méthode retournera true dès que le pixel désigné est dans l'image */
   public boolean isInCell(int x,int y) {
      return x>=xCell && x<xCell+widthCell && y>=yCell && y<yCell+heightCell;
   }

   /** Retourne la valeur physique (bscale*pix+bzero) du pixel en (x,y) (y compté à partir du bas) sous forme d'un double */
   public double getPixelFull(int x,int y) {
      return bscale*getPixValDouble(pixels,bitpix,(y-yCell)*widthCell + (x-xCell)) + bzero;
   }

   /** Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme d'un double */
   public double getPixelDouble(int x,int y) {
      return getPixValDouble(pixels,bitpix,(y-yCell)*widthCell + (x-xCell));
   }

   /** Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme d'un entier */
   public int getPixelInt(int x,int y) {
      return getPixValInt(pixels,bitpix,(y-yCell)*widthCell + (x-xCell));
   }

   /** Retourne la valeur du pixel 8 bits en (x,y) (y compté à partir du haut)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   protected int getPix8FromTop(int x,int y) {
      return getPixValInt(pix8,8, (y-yCell)*widthCell + (x-xCell));
   }
   
   /** Retourne la valeur du pixel 8 bits en (x,y) (y compté à partir du bas)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   public int getPix8(int x,int y) {
      return getPixValInt(pix8,8, ((height-y-1)-yCell)*widthCell+(x-xCell));
   }

   /** Positionne la valeur du pixel RGB en (x,y) (y compté à partir du bas) exprimé en ARGB
    * Le facteur Alpha est forcé à 0xFF (pas de transparence) */
   public void setPixelRGB(int x, int y, int val) {
      rgb[(y-yCell)*widthCell + (x-xCell)]=0xFF000000 | val;
   }
   
   public void setPixelRGBJPG(int x,int y, int val) {
      rgb[ ((height-y-1)-yCell)*widthCell+(x-xCell)]=0xFF000000 | val;
   }

   /** Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé en double */
   public void setPixelDouble(int x,int y, double val) {
      setPixValDouble(pixels,bitpix, (y-yCell)*widthCell + (x-xCell), val);
   }

   /** Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé en entier */
   public void setPixelInt(int x,int y, int val) {
      setPixValInt(pixels,bitpix, (y-yCell)*widthCell + (x-xCell), val);
   }

   /** Positionne la valeur du pixel 8 bits en (x,y) (y compté à partir du bas)
    * exprimé en byte => destiné à être sauvegardé en JPEG  */
   public void setPix8(int x,int y, int val) {
      setPixValInt(pix8,8, ((height-y-1)-yCell)*widthCell+x, val);
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

	   if (oldbitpix == bitpix || isBlankPixel(val) ) {
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

   /** Calcule un autocut linéaire des pixels "fullbits"
    * Utilise la méthode "à la Aladin" pour déterminer
    * le meilleur intervalle.
    */
   public static double[] autocut(String filename) throws Exception {
	   Fits f = new Fits();
	   f.loadFITS(filename);
      return f.findAutocutRange();
   }


   /** Remplit le tableau des pixels 8 bits (pix8)  */
   public void toPix8() {
	   double[] minmax = findMinMax();
	   toPix8(minmax[0], minmax[1]);
   }


   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] linéairement */
   public void toPix8(double min, double max) {
      double r = 256./(max - min);
      for( int y=0; y<heightCell; y++) {
         for( int x=0; x<widthCell; x++ ) {
            double pixIn = getPixelDouble(x+xCell,y+yCell);
            byte pixOut = (byte)( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            setPix8(x+xCell,y+yCell,pixOut);
         }
      }
   }
   
   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] et de la table des couleurs cm.
    * S'il y a usage d'une fonction de transfert, elle est déjà appliquée à la table cm (rien à faire de plus)
    * Si la table cm n'est pas monochrome, l'algo travaille uniquement sur la composante bleue */
   public void toPix8(double min, double max, ColorModel cm) {
      double r = 256./(max - min);
      
      for( int y=0; y<heightCell; y++) {
         for( int x=0; x<widthCell; x++ ) {
            double pixIn = getPixelDouble(x+xCell,y+yCell);
            int pix = ( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            byte pixOut =(byte) cm.getBlue(pix);
            setPix8(x+xCell,y+yCell,pixOut);
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
      for( int h=0; h<heightCell; h++ ){
         System.arraycopy(src,h*widthCell, dst,(heightCell-h-1)*widthCell, widthCell);
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
         findMinMax(range,pixels,bitpix,widthCell,heightCell,min,max,true,0);
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


   /** Creation des tabl

	/** Pour aider le GC */
   public void free() {
      pixels =null;
      pix8 = null;
      calib=null;
      center=null;
      headerFits=null;
      width=height=bitpix=0;
      widthCell=heightCell=xCell=yCell=0;
   }

   @Override
   public String toString() {
      return "Fits file: "+width+"x"+height+" bitpix="+bitpix+ " ["+xCell+","+yCell+" "+widthCell+"x"+heightCell+"]";
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
//	   int[] tmp = new int[rgb.length];
//	   for( int h=0; h<heightCell; h++ ){
//		   System.arraycopy(rgb,h*widthCell, tmp,(heightCell-h-1)*widthCell, widthCell);
//	   }
//	   rgb = tmp;
   }
   
   /** Coadditionne les pixels (pix8[], pixels[] et rgb[] */
   public void coadd(Fits a) throws Exception {
      int taille=widthCell*heightCell;
      
      if( a.pixels!=null && pixels!=null ) {
         for( int i=0; i<taille; i++) {
            double v1 = getPixValDouble(pixels,bitpix,i);
            double v2 = a.getPixValDouble(a.pixels,bitpix,i);
            double v = isBlankPixel(v1) ? v2 :  a.isBlankPixel(v2) ? v1 : (v1+v2)/2;
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

   /** Remplace les pixels (pix8[], pixels[] et rgb[] pour les nouvelles valeurs != NaN */
   public void overwriteWith(Fits a) throws Exception {
      int taille=widthCell*heightCell;
      
      if( a.pixels!=null && pixels!=null ) {
         for( int i=0; i<taille; i++) {
            double v = a.getPixValDouble(pixels,bitpix,i);
            if( a.isBlankPixel(v) ) v=-100; //continue;
            
            setPixValDouble(pixels, bitpix, i, v);
            
            if( a.pix8!=null && pix8!=null ) pix8[i] = a.pix8[i];
            if( a.rgb!=null && rgb!=null ) rgb[i] = a.rgb[i];
         }
      }
   }

   /** Ajoute les pixels (pix8[], pixels[] et rgb[] sur les valeurs NaN */
   public void mergeOnNaN(Fits a) throws Exception {
      int taille=widthCell*heightCell;
      
      if( a.pixels!=null && pixels!=null ) {
         for( int i=0; i<taille; i++) {
            double v1 = getPixValDouble(pixels,bitpix,i);
            if( !isBlankPixel(v1) ) continue;
            
            double v = a.getPixValDouble(a.pixels,bitpix,i);
            setPixValDouble(pixels, bitpix, i, v);
            
            if( a.pix8!=null && pix8!=null ) pix8[i] = a.pix8[i];
            if( a.rgb!=null && rgb!=null ) rgb[i] = a.rgb[i];
         }
      }
   }

   public void setFilename(String filename) {
      if( filename==null ) { this.filename=null; return; }
      int p = filename.lastIndexOf('[');
      if( p>=0 ) filename=filename.substring(0,p);
      this.filename = filename;
   }

   public String getFilename() {
      return filename;
   }

   public static void main(String[] args) {
      try {
         Fits f = new Fits();
         f.loadFITS("C:/Test.fits");
         System.out.println("lecture de "+f.getFilename()+" => "+f+" center="+f.center);
         int x= 33;
         int y= 25;
         int x1=1;
         int y1=2;
         int x2=7;
         int y2=27;
         double pix = f.isInCell(x,y) ? f.getPixelFull(x,y) : Double.NaN;
         System.out.println("Valeur du pixel1 ("+x+","+y+") => "+pix);
         double pix1 = f.isInCell(x1,y1) ? f.getPixelFull(x1,y1) : Double.NaN;
         System.out.println("Valeur du pixel2 ("+x1+","+y1+") => "+pix1);
         double pix2 = f.isInCell(x2,y2) ? f.getPixelFull(x2,y2) : Double.NaN;
         System.out.println("Valeur du pixel3 ("+x2+","+y2+") => "+pix2);
         f.free();
         
         Fits g = new Fits();
         g.loadFITS("C:/Test.fits[1,0-10x18]");
         System.out.println("lecture de "+g.getFilename()+" => "+g+" center="+g.center);
         pix = g.isInCell(x,y) ? g.getPixelFull(x,y) : Double.NaN;
         System.out.println("Valeur du pixel1 ("+x+","+y+") => "+pix);
         pix1 = g.isInCell(x1,y1) ? g.getPixelFull(x1,y1) : Double.NaN;
         System.out.println("Valeur du pixel2 ("+x1+","+y1+") => "+pix1);
         pix2 = g.isInCell(x2,y2) ? g.getPixelFull(x2,y2) : Double.NaN;
         System.out.println("Valeur du pixel3 ("+x2+","+y2+") => "+pix2);
         g.free();
         
         Fits h = new Fits();
         h.loadFITS("C:/Test.fits[24,22-10x5]");
         System.out.println("lecture de "+h.getFilename()+" => "+h+" center="+h.center);
         pix = h.isInCell(x,y) ? h.getPixelFull(x,y) : Double.NaN;
         System.out.println("Valeur du pixel1 ("+x+","+y+") => "+pix);
         pix1 = h.isInCell(x1,y1) ? h.getPixelFull(x1,y1) : Double.NaN;
         System.out.println("Valeur du pixel2 ("+x1+","+y1+") => "+pix1);
         pix2 = h.isInCell(x2,y2) ? h.getPixelFull(x2,y2) : Double.NaN;
         System.out.println("Valeur du pixel3 ("+x2+","+y2+") => "+pix2);
         h.free();
         


      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

}
