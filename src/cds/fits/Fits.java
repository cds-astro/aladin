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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Rectangle;
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
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
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
	public byte [] pixels;           // Pixels d'origine "fullbits" (y compt� depuis le bas)
	public int bitpix;               // Profondeur des pixels (codage FITS 8,16,32,-32,-64)
	public int width;               // Largeur totale de l'image
	public int height;               // Hauteur totale de l'image
	public double bzero=DEFAULT_BZERO;	// BZERO Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double bscale=DEFAULT_BSCALE;	// BSCALE Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
	public double blank=DEFAULT_BLANK;  // valeur BLANK
	public boolean flagARGB=false;   // true s'il s'agit d'un FITS couleur ARGB
	private long bitmapOffset=-1;   // Rep�re le positionnement du bitmap des pixels (voir releaseBitmap());

	// Dans le cas o� il s'agit d'une cellule sur l'image (seule une portion de l'image sera accessible)
    public int xCell;                // Position X � partir du coin haut gauche de la cellule de l'image (par d�faut 0)
    public int yCell;                // Position Y � partir du coin haut gauche de la cellule de l'image (par d�faut 0)
    public int widthCell;                // Largeur de la cellule de l'image (par d�faut = naxis1)
    public int heightCell;                // Hauteur de la cellule de l'image (par d�faut = naxis2)

	public byte [] pix8;             // Pixels 8 bits en vue d'une sauvegarde JPEG (y compt� depuis le haut)
	public int [] rgb;               // pixels dans le cas d'une image couleur RGB

	private Calib calib;             // Calibration astrom�trique


	public static String FS = System.getProperty("file.separator");
	
   /** Cr�ation en vue d'une lecture */
   public Fits() { }

   /** Cr�ation en vue d'une construction "manuelle"
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
   
   /** Positionnement d'une calibration => initialisation de la coordonn�e
    * centrale de l'image (cf center)
    */
   public void setCalib(Calib c) {
      calib=c;
//      initCenter();
   }
   
//   public void initCenter() {
//      try {
//         center = new Coord();
//         center.x = xCell+widthCell/2.;
//         center.y = height-(yCell+heightCell/2.);
//         calib.GetCoord(center);
//      } catch( Exception e ) { e.printStackTrace(); }
//   }

   /** Retourne la calib ou null */
   public Calib getCalib() { return calib; }
   
// /** Chargement d'une image Jpeg N&B depuis un fichier */
// public void loadJpeg(String filename) throws Exception { loadJpeg(filename,false); }
//
// /** Chargement d'une image Jpeg N&B ou COULEUR depuis un fichier */
// public void loadJpeg(String filename,boolean flagColor) throws Exception {
//    MyInputStream is = new MyInputStream( new FileInputStream(filename));
//    loadJpeg(is,flagColor);
//    is.close();
//    this.setFilename(filename);
// }
// 

   /* Chargement d'une image N&B sous forme d'un JPEG */
   protected void loadJpeg(MyInputStream dis) throws Exception { loadJpeg(dis,false); }
   
   
   /** Chargement d'une image JPEG depuis un fichier */
   public void loadJpeg(String filename,int x, int y, int w, int h) throws Exception { loadJpeg(filename+"["+x+","+y+"-"+w+"x"+h+"]"); }
   public void loadJpeg(String filename) throws Exception {loadJpeg(filename,false);}
   public void loadJpeg(String filename, boolean color) throws Exception {
      filename = parseCell(filename);   // extraction de la descrition d'une cellule �ventuellement en suffixe du nom fichier.fits[x,y-wxh]
      MyInputStream is = new MyInputStream( new FileInputStream(filename));
      is = is.startRead();
      is.getType();   // Pour �tre s�r de lire le commentaire �ventuel
      if( is.hasCommentCalib() ) {
         headerFits = is.createHeaderFitsFromCommentCalib();
         try { setCalib(new Calib(headerFits)); } catch( Exception e ) { calib=null; }

      }
      loadJpeg(is,xCell,yCell,widthCell,heightCell,color);
      is.close();
      this.setFilename(filename);
   }
   
   
   protected void loadJpeg(MyInputStream dis,boolean flagColor) throws Exception {
      loadJpeg(dis,0,0,-1,-1,flagColor);
   }

   static private Component observer = (Component)new Label();

//   /** Chargement d'une image N&B ou COULEUR sous forme d'un JPEG */
//   protected void loadJpeg(MyInputStream dis,boolean flagColor) throws Exception {
//      Image img = Toolkit.getDefaultToolkit().createImage(dis.readFully());
//      boolean encore=true;
//      while( encore ) {
//         try {
//            MediaTracker mt = new MediaTracker(observer);
//            mt.addImage(img,0);
//            mt.waitForID(0);
//            encore=false;
//         } catch( InterruptedException e ) { }
//      }
//      width=widthCell =img.getWidth(observer);
//      height=heightCell=img.getHeight(observer);
//      xCell=yCell=0;
//      bitpix= flagColor ? 0 : 8;
//      
//      BufferedImage imgBuf = new BufferedImage(widthCell,heightCell, 
//            flagColor ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_BYTE_GRAY);
//      Graphics g = imgBuf.getGraphics();
//      g.drawImage(img,0,0,observer);
//      g.finalize(); g=null;
//      if( flagColor ) rgb = ((DataBufferInt)imgBuf.getRaster().getDataBuffer()).getData();
//      else pixels = ((DataBufferByte)imgBuf.getRaster().getDataBuffer()).getData();
//      
////      BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
////      Graphics g = imgBuf.getGraphics();
////      g.drawImage(img,0,0,observer);
////      g.finalize(); g=null;
////      int taille=width*height;
////      rgb = new int[taille];
////      imgBuf.getRGB(0, 0, width, height, rgb, 0, width);
////      if( bitpix!=0 ) {
////         pixels = new byte[taille];
////         for( int i=0; i<taille; i++ ) pixels[i] = (byte)(rgb[i] & 0xFF);
////         rgb=null;
////      }
//      
//      imgBuf.flush(); imgBuf=null;
//      img.flush(); img=null;
//      if( bitpix==8 ) {
//         pix8 = new byte[widthCell*heightCell];
//         initPix8();
//      }
//   }
   
   public void loadJpeg(MyInputStream dis,int x,int y, int w, int h,boolean flagColor) throws Exception {
      Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");
      ImageReader reader = (ImageReader)readers.next();
      ImageInputStream iis = ImageIO.createImageInputStream(dis);
      reader.setInput(iis,true);
      width = reader.getWidth(0);
      height = reader.getHeight(0);
      
      // Ouverture compl�te de l'image
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
      bitpix= flagColor ? 0 : 8;
      
      ImageReadParam param = reader.getDefaultReadParam();
      if( this.widthCell!=width || this.heightCell!=height ) {
         int yJpegCell;
         if( RGBASFITS ) yJpegCell = height-yCell-heightCell;
         else yJpegCell=yCell;
         Rectangle r = new Rectangle(xCell,yJpegCell,widthCell,heightCell);
         param.setSourceRegion(r);
      }
      BufferedImage imgBuf = reader.read(0,param);
      
      if( flagColor ) {
         rgb = new int[widthCell*heightCell];
         imgBuf.getRGB(0, 0, widthCell, heightCell, rgb, 0, widthCell);
         if( RGBASFITS ) invImageLine(widthCell,heightCell,rgb);

      } else {
         pixels = ((DataBufferByte)imgBuf.getRaster().getDataBuffer()).getData();
         if( RGBASFITS ) {
            invImageLine(widthCell,heightCell,pixels);
            pix8 = pixels;
         }
         
//      System.out.print("1�re ligne:");
//      for( int i=0; i<5; i++ ) System.out.print(" "+pixels[i]);
//      System.out.println();
//      
//      System.out.print("Derni�re ligne :");
//      for( int i=0; i<5; i++ ) System.out.print(" "+pixels[pixels.length-widthCell+i]);
//      System.out.println();
      }
      

      imgBuf.flush(); imgBuf=null;
      if( !RGBASFITS && bitpix==8 ) {
         pix8 = new byte[widthCell*heightCell];
         initPix8();
      }

   }
   
   protected static void invImageLine(int width, int height,byte [] pixels) {
      byte[] tmp = new byte[width];
      for( int h=height/2-1; h>=0; h-- ) {
         int offset1=h*width;
         int offset2=(height-h-1)*width;
         System.arraycopy(pixels,offset1, tmp,0, width);
         System.arraycopy(pixels,offset2, pixels,offset1, width);
         System.arraycopy(tmp,0, pixels,offset2, width);
      }
      tmp=null;
   }

   protected static void invImageLine(int width, int height,int [] pixels) {
      int[] tmp = new int[width];
      for( int h=height/2-1; h>=0; h-- ) {
         int offset1=h*width;
         int offset2=(height-h-1)*width;
         System.arraycopy(pixels,offset1, tmp,0, width);
         System.arraycopy(pixels,offset2, pixels,offset1, width);
         System.arraycopy(tmp,0, pixels,offset2, width);
      }
      tmp=null;
   }

   
   // Extraction de la d�finition d'une cellule FITS pour une ouverture d'un fichier FITS en mode "mosaic"
   // le filename doit �tre suffix� par [x,y-wxh] (sans aucun blanc).
   // => met � jour les variables xCell, yCell, widthCell et heigthCell
   // S'il n'y a pas de d�finition de cellule, laisse xCell et yCell � 0 et widthCell et heightCell � -1
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


   /** Chargement d'une image FITS depuis un fichier */
   public void loadFITS(String filename,int x, int y, int w, int h) throws Exception { loadFITS(filename+"["+x+","+y+"-"+w+"x"+h+"]"); }
   public void loadFITS(String filename) throws Exception {loadFITS(filename,false);}
   public void loadFITS(String filename, boolean color) throws Exception {
      filename = parseCell(filename);   // extraction de la descrition d'une cellule �ventuellement en suffixe du nom fichier.fits[x,y-wxh]
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
//       boolean flagHComp = (dis.getType() & MyInputStream.HCOMP) !=0;
       boolean flagHComp = dis.isHCOMP();
	   headerFits = new HeaderFits(dis);
	   bitpix = headerFits.getIntFromHeader("BITPIX");
	   
// MODIF D'ANAIS QUI NE PEUT PAS FONCTIONNER CAR UN FITS PEUT ETRE INDIQUE COMME ETANT SUSCEPTIBLE
// D'AVOIR UNE EXTENSION SANS POUR AUTANT EN AVOIR UNE !!  [EXTEND = T]
//
//	   // Si on a une image avec extension
//	   // ouvrir et lire le reste des infos depuis une image de l'extension
//	   long type = dis.getType();
//	   if ( (type & MyInputStream.XFITS)!=0)  {
//		   headerFits = new HeaderFits(dis);
//		   int naxis = headerFits.getIntFromHeader("NAXIS");
//		   // Il s'agit juste d'une ent�te FITS indiquant des EXTENSIONs
//		   if( headerFits.getStringFromHeader("EXTEND")!=null ) {
//			   while( naxis<2 ) {
//				   // Je saute l'�ventuel baratin de la premi�re HDU
//				   if (!headerFits.readHeader(dis))
//					   throw new Exception("Naxis < 2");
//				   naxis = headerFits.getIntFromHeader("NAXIS");
//			   }
//		   }
//		   bitpix = headerFits.getIntFromHeader("BITPIX");
//	   }
	   width  = headerFits.getIntFromHeader("NAXIS1");
	   height = headerFits.getIntFromHeader("NAXIS2");
	   
	   // Ouverture compl�te de l'image
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
	   try { blank = headerFits.getDoubleFromHeader("BLANK");} 
	   catch( Exception e ) {
	      blank= /* bitpix>0 ? 0 : */ DEFAULT_BLANK;
	   }
	   
       int n = (Math.abs(bitpix)/8);
       bitmapOffset = dis.getPos();
	   
	   // Pas le choix, il faut d'abord tout lire, puis ne garder que la cellule si n�cessaire
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

          // Lecture ligne � ligne pour m�moriser uniquement la cellule
          else {
             dis.skip( yCell*width*n);
             byte [] buf = new byte[width * n];  // une ligne compl�te
             for( int lig=0; lig<heightCell; lig++ ) {
                dis.readFully(buf);
                System.arraycopy(buf, xCell*n, pixels,lig*widthCell*n, widthCell*n);
             }
             dis.skip( (height-(yCell+heightCell) )*width * n);
          }

       }
	   try { bscale = headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
	   try { bzero  = headerFits.getDoubleFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
	   try { setCalib(new Calib(headerFits)); }                   catch( Exception e ) { calib=null; }
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
   
   static final public int GZIP = 1;
   static final public int HHH = 1<<1;
   static final public int COLOR = 1<<2;
   static final public int XFITS = 1<<3;
   

   /** Chargement de l'entete d'une image FITS depuis un fichier 
    * @return un code GZIP|HHH|COLOR pour savoir de quoi il s'agit
    */
   public int loadHeaderFITS(String filename) throws Exception {
      filename = parseCell(filename);   // extraction de la descrition d'une cellule �ventuellement en suffixe du nom fichier.fits[x,y-wxh]
      int code=0;
      MyInputStream is = new MyInputStream( new FileInputStream(filename));
      if( is.isGZ() ) code |= GZIP; 
      is = is.startRead();
      long type = is.getType();
      
      // Cas sp�cial d'un fichier .hhhh
      if( filename.endsWith(".hhh") ) {
         byte [] buf = is.readFully();
         headerFits = new HeaderFits();
         headerFits.readFreeHeader(new String(buf), true, null);
         code |= HHH;
         
      // Cas d'un fichier PNG ou JPEG avec un commentaire contenant la calib
      } else if( is.hasCommentCalib() ) {
         headerFits = is.createHeaderFitsFromCommentCalib();
         bitpix = 0;
      }
      // Si on a une image avec extension
      // ouvrir et lire le reste des infos depuis une image de l'extension
      else if ( (type& MyInputStream.XFITS)!=0)  {
    	  headerFits = new HeaderFits(is);
    	  code |= XFITS;
    	  int naxis = headerFits.getIntFromHeader("NAXIS");
    	  // Il s'agit juste d'une ent�te FITS indiquant des EXTENSIONs
    	  if( headerFits.getStringFromHeader("EXTEND")!=null ) {
    		  while( naxis<2 ) {
    			  // Je saute l'�ventuel baratin de la premi�re HDU
    			  if (!headerFits.readHeader(is))
    				  throw new Exception("Naxis < 2");
    			  naxis = headerFits.getIntFromHeader("NAXIS");
    		  }
    	  }
    	  bitpix = headerFits.getIntFromHeader("BITPIX");
      }
      
      // Cas habituel
      else {
         headerFits = new HeaderFits(is);
         try {
            bitpix = headerFits.getIntFromHeader("BITPIX");
        } catch (Exception e1) {
            bitpix = 0;
        }
      }
      
      if( bitpix==0 ) code |= COLOR;
      
      width  = headerFits.getIntFromHeader("NAXIS1");
      height = headerFits.getIntFromHeader("NAXIS2");
      if( !hasCell() ) {
         xCell=yCell=0;
         widthCell=width;
         heightCell=height;
      }
      try { blank = headerFits.getDoubleFromHeader("BLANK");   } catch( Exception e ) { blank=DEFAULT_BLANK; }
      try { bscale = headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { bscale=DEFAULT_BSCALE; }
      try { bzero  = headerFits.getDoubleFromHeader("BZERO");  } catch( Exception e ) { bzero=DEFAULT_BZERO;  }
      try { setCalib(new Calib(headerFits)); }                catch( Exception e ) { 
         if( Aladin.levelTrace>=3 ) e.printStackTrace(); calib=null; }
      is.close();
      this.setFilename(filename);
      return code;
   }
   

   /** Retourne la valeur BSCALE (1 si non d�finie) */
   public double getBscale() { return bscale; }
   
   /** Retourne la valeur BZERO (0 si non d�finie) */
   public double getBzero() { return bzero; }
   
   /** Retourne la valeur BLANK si elle existe (tester avec hasBlank() */
   public double getBlank() { return blank; }
   
   /** Positionement d'une valeur BSCALE - si �gale � 1, supprime le mot cl� du header fits */
   public void setBscale(double bscale) {
      this.bscale=bscale;
      if( headerFits!=null ) headerFits.setKeyValue("BSCALE", bscale==1 ? (String)null : bscale+"" );
   }
   
   /** Positionement d'une valeur BZERO - si �gale � 0, supprime le mot cl� du header fits */
   public void setBzero(double bzero) {
      this.bzero=bzero;
      if( headerFits!=null ) headerFits.setKeyValue("BZERO", bzero==0 ? (String)null : bzero+"" );
   }
   
   /** Positionement d'une valeur BLANK. Double.NaN est support� */
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

      // G�n�ration du tableau rgb[] � partir des pixels ARGB stock�s dans pixels[]
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
    * Cr�e si n�cessaire le r�pertoire correspondant au filename
    * @param filename
    */
   private void createDir(String filename) throws Exception {
      cds.tools.Util.createPath(filename);
//       File dir = new File(filename).getParentFile();
//       if( !dir.exists() ) {
//           dir.mkdirs();
//       }
   }

   /** G�n�ration d'un fichier FITS (sans calibration) */
   public void writeFITS8(OutputStream os) throws Exception {
      headerFits.setKeyValue("BITPIX", bitpix+"");
      headerFits.writeHeader(os);
      os.write(pix8);
   }

   /** G�n�ration d'un fichier FITS (sans calibration) */
   public void writeFITS8(String filename) throws Exception {
        createDir(filename);
        OutputStream os = new FileOutputStream(filename);
        writeFITS8(os);
        os.close();
        this.setFilename(filename);
    }


   static public byte [] getBourrage(int currentPos) {
      int n = currentPos%2880;
      int size = n==0 ? 0 : 2880 - n;
      byte [] b = new byte[size];
      return b;
   }

   /** G�n�ration d'un fichier FITS (sans calibration) */
   public void writeFITS(OutputStream os) throws Exception {
      int size = headerFits.writeHeader(os);
      bitmapOffset=size;

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
         size += buf.length;
         buf=null;

      // Fits classique
      } else {
         os.write(pixels);
         size += pixels.length;
      }

      // Ecriture des �ventuelles extensions
      if( extHeader==null ) return;
      int n = extHeader.size();
      for( int i=0; i<n; i++ ) {
         byte [] b = getBourrage(size);
         size += b.length;
         os.write(b);
         HeaderFits h = (HeaderFits)extHeader.elementAt(i);
         h.writeHeader(os);
         byte [] p = (byte[])extPixels.elementAt(i);
         os.write(p);
         size += p.length;
      }
      
      // Bourrage final
      os.write(getBourrage(size));  // Quel gachi !
   }

   /** G�n�ration d'un fichier FITS (sans calibration) */
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

   /** G�n�ration d'un fichier JPEG � partir des pixels 8bits
    * Ceux-ci doivent �tre positionn�s manuellement via la m�thode
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
      Image img;
      if( bitpix==0 ) {
         if( RGBASFITS ) invImageLine(widthCell,heightCell, rgb);
         img = Toolkit.getDefaultToolkit().createImage( new MemoryImageSource(widthCell,heightCell, rgb, 0,widthCell) );
      } else {
         img = Toolkit.getDefaultToolkit().createImage( new MemoryImageSource(widthCell,heightCell,getCM(), pix8, 0,widthCell) );
      }

      BufferedImage bufferedImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
      Graphics g = bufferedImage.createGraphics();
      int yJpegCell = RGBASFITS ? height-yCell-heightCell : yCell;
      g.drawImage(img,xCell,yJpegCell,observer);
      g.dispose();
      
      if( RGBASFITS && bitpix==0 ) invImageLine(widthCell,heightCell, rgb);

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
   
   
//   static final public boolean INCELLS=false;
//   static final public boolean JPEGORDERCALIB=false;
//   static final public boolean JPEGFROMTOP=true;
//   static final public boolean RGBASFITS=false;

   
   static final public boolean INCELLS=true;         // true => permet le d�coupage en cellule d'un fichier .hhh
   static final public boolean JPEGORDERCALIB=false;  // true => Ne fait pas le compl�ment � la hauteur lors des calculs de coord via Calib pour les JPEG
   static final public boolean JPEGFROMTOP=false;      // true => les lignes dans rgb[] sont compt�es depuis le haut
   static final public boolean RGBASFITS=true;       // true => les lignes dans rgb[] sont compt�es depuis le bas comme en FITS
  
   /** Retourne la valeur RGB du pixel dans le cas d'une image couleur RGB
    * => le facteur ALPHA est mis � z�ro */
   public int getPixelRGB(int x, int y) {
      return 0x00FFFFFF & rgb[ (y-yCell)*widthCell + (x-xCell) ];
   }
   
   public int getPixelRGBJPG(int x, int y) {
      if( Fits.JPEGFROMTOP )  return 0x00FFFFFF & rgb[ ((height-y-1)-yCell)*widthCell+(x-xCell) ];
      return 0x00FFFFFF & rgb[ (y-yCell)*widthCell + (x-xCell) ];
   }
  
   /** Retourne la description de la cellule courante selon la syntaxe [x,y-wxh]
    * ou "" si le fichier n'a pas �t� ouvert en mode mosaic */
   public String getCellSuffix() throws Exception {
      if( !hasCell() ) return "";
      return "["+xCell+","+yCell+"-"+widthCell+"x"+heightCell+"]";
   }
   
   /** Retourne true si le fichier a �t� ouvert en mode mosaic */
   public boolean hasCell() { return widthCell!=-1 && heightCell!=-1 && (widthCell!=width || heightCell!=height); }
   
   /** Retourne true s'il y a un pixel connu � la position x,y.
    * Cela concerne le mode mosaic FITS, o� l'on peut ouvrir juste une cellule sur le fichier
    * Dans le cas g�n�ral, cette m�thode retournera true d�s que le pixel d�sign� est dans l'image */
   public boolean isInCell(int x,int y) {
      return x>=xCell && x<xCell+widthCell && y>=yCell && y<yCell+heightCell;
   }

   /** Retourne la valeur physique (bscale*pix+bzero) du pixel en (x,y) (y compt� � partir du bas) sous forme d'un double */
   public double getPixelFull(int x,int y) {
      return bscale*getPixValDouble(pixels,bitpix,(y-yCell)*widthCell + (x-xCell)) + bzero;
   }

   /** Retourne la valeur du pixel en (x,y) (y compt� � partir du bas) sous forme d'un double */
   public double getPixelDouble(int x,int y) {
      return getPixValDouble(pixels,bitpix,(y-yCell)*widthCell + (x-xCell));
   }

   /** Retourne la valeur du pixel en (x,y) (y compt� � partir du bas) sous forme d'un entier */
   public int getPixelInt(int x,int y) {
      return getPixValInt(pixels,bitpix,(y-yCell)*widthCell + (x-xCell));
   }

//   /** Retourne la valeur du pixel 8 bits en (x,y) (y compt� � partir du haut)
//    * exprim� en byte => destin� � �tre sauvegard� en JPEG  */
//   public int getPix8FromTop(int x,int y) {
//      return getPixValInt(pix8,8, (y-yCell)*widthCell + (x-xCell));
//   }
//   
//   /** Retourne la valeur du pixel 8 bits en (x,y) (y compt� � partir du bas)
//    * exprim� en byte => destin� � �tre sauvegard� en JPEG  */
//   public int getPix8(int x,int y) {
//      return getPixValInt(pix8,8, ((height-y-1)-yCell)*widthCell+(x-xCell));
//   }

   /** Positionne la valeur du pixel RGB en (x,y) (y compt� � partir du bas) exprim� en ARGB
    * Le facteur Alpha est forc� � 0xFF (pas de transparence) */
   public void setPixelRGB(int x, int y, int val) {
      rgb[(y-yCell)*widthCell + (x-xCell)]=0xFF000000 | val;
   }
   
   public void setPixelRGBJPG(int x,int y, int val) {
      if( Fits.JPEGFROMTOP ) rgb[ ((height-y-1)-yCell)*widthCell+(x-xCell)]=0xFF000000 | val;
      else rgb[(y-yCell)*widthCell + (x-xCell)]=0xFF000000 | val;
   }

   /** Positionne la valeur du pixel en (x,y) (y compt� � partir du bas) exprim� en double */
   public void setPixelDouble(int x,int y, double val) {
      setPixValDouble(pixels,bitpix, (y-yCell)*widthCell + (x-xCell), val);
   }

   /** Positionne la valeur du pixel en (x,y) (y compt� � partir du bas) exprim� en entier */
   public void setPixelInt(int x,int y, int val) {
      setPixValInt(pixels,bitpix, (y-yCell)*widthCell + (x-xCell), val);
   }

   /** Positionne la valeur du pixel 8 bits en (x,y) (y compt� � partir du bas)
    * exprim� en byte => destin� � �tre sauvegard� en JPEG  */
   private void setPix8(int x,int y, int val) {
      setPixValInt(pix8,8, ((height-y-1)-yCell)*widthCell+x, val);
//      setPixValInt(pix8,8, y*width+x, val);
   }
   
   
   
//   /**
//    * Convertit la valeur double donn�e dans le type du bitpix et l'affecte
//    * 8 : 0 255
//    * 16 : 0 32767
//    * 32 : 0 32767
//    * -32 : 0 10000
//    * -64 : 0 10000
//    * @param x
//    * @param y
//    * @param val
//    */
//   public void setPixelDoubleFromBitpix(int x, int y, double val, int oldbitpix, double[] oldminmax) {
//
//	   if (oldbitpix == bitpix || isBlankPixel(val) ) {
//		   setPixelDouble(x,y,val);
//		   return;
//	   }
//	   
//	   double newval = toBitpixRange(val, bitpix, oldminmax);
//	   setPixelDouble(x,y,newval);
//
//   }
//
//   public static double toBitpixRange(double val, int bitpix, double[] oldminmax) {
//	   double[] minmax = new double[2];
//	   minmax = getBitpixRange(bitpix);
//
//	   double r = (minmax[1]-minmax[0])/(oldminmax[1]-oldminmax[0]);
//	   double newval = (val-oldminmax[0])*r + minmax[0];
//	   return newval;
//   }
//
//   static private double[] getBitpixRange(int bitpix) {
//      switch (bitpix) {
//         case 8 :	return minmax8;
//         case 16 : return minmax16;
//         case 32 : return minmax32;
//         case -32 : return minmax_32;
//         case -64 : return minmax_64;
//         default : return null;
//      }
//   }

   /** Retourne les coordonn�es c�lestes en J2000 de (x,y) (y compt� � partir du bas)
    * Le buffer "c" peut �tre fourni pour �viter des allocations inutiles, null sinon */
   protected double [] getRaDec(double [] c, double x, double y) throws Exception {
      if( c==null ) c = new double[2];
      cTmp.x = x;
      cTmp.y = y;
      calib.GetCoord(cTmp);
      c[0]=cTmp.al;
      c[1]=cTmp.del;
      return c;
   }

   /** Retourne les coordonn�es image (x,y) (y compt� � partir du bas) en fonction
    * de coordonn�es c�lestes en J2000
    * Le buffer "c" peut �tre fourni pour �viter des allocations inutiles, null sinon */
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
//    * le tableau pix8 []. Utilise la m�thode "� la Aladin" pour d�terminer
//    * le meilleur intervalle.
//    */
//   public void autocutLog() throws Exception {
//      double [] range = findAutocutRange();
//      toPix8Log(range[0],range[1]);
//   }
   
//   public void cut() throws Exception {
//	   double[] range = {Double.MIN_VALUE,Double.MAX_VALUE};
//	   switch (bitpix) {
//	   case 8:
//		   range[0] = 0;
//		   range[1] = 255;
//	   case 16:
//		   range[0] = 0;
//		   range[1] = Short.MAX_VALUE;
//	   case 32:
//		   range[0] = 0;
//		   range[1] = Integer.MAX_VALUE;
//	   case -32:
//		   range[0] = Float.MIN_VALUE;
//		   range[1] = Float.MAX_VALUE;
//	   case -64:
//		   range[0] = Double.MIN_VALUE;
//		   range[1] = Double.MAX_VALUE;
//
//	   }
//	   toPix8(range[0],range[1]);
//   }
   
   /** Effectue un autocut lin�aire des pixels "fullbits" et une conversion en 8bits dans
    * le tableau pix8 []. Utilise la m�thode "� la Aladin" pour d�terminer
    * le meilleur intervalle.
    */
//   public void autocut8() throws Exception {
//      double [] range = findAutocutRange();
//      toPix8(range[0],range[1]);
//   }

   /** Calcule un autocut lin�aire des pixels "fullbits"
    * Utilise la m�thode "� la Aladin" pour d�terminer
    * le meilleur intervalle.
    */
//   public static double[] autocut(String filename) throws Exception {
//	   Fits f = new Fits();
//	   f.loadFITS(filename);
//      return f.findAutocutRange();
//   }


   /** Remplit le tableau des pixels 8 bits (pix8)  */
//   public void toPix8() {
//	   double[] minmax = findMinMax();
//	   toPix8(minmax[0], minmax[1]);
//   }


   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] lin�airement */
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
   
// VOIR REMARQUE DANS toPix8(double,double,byte[]);
//   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
//    * des valeurs des pixels originaux [min..max] et de la table des couleurs cm.
//    * S'il y a usage d'une fonction de transfert, elle est d�j� appliqu�e � la table cm (rien � faire de plus)
//    * Si la table cm n'est pas monochrome, l'algo travaille uniquement sur la composante bleue */
//   public void toPix8(double min, double max, ColorModel cm) {
//      double r = 256./(max - min);
//      
//      for( int y=0; y<heightCell; y++) {
//         for( int x=0; x<widthCell; x++ ) {
//            double pixIn = getPixelDouble(x+xCell,y+yCell);
//            int pix = ( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
//                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
//            byte pixOut =(byte) cm.getBlue(pix);
//            setPix8(x+xCell,y+yCell,pixOut);
//         }
//      }
//   }

   /** Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] et de la table des couleurs tcm.
    * S'il y a usage d'une fonction de transfert, elle est d�j� appliqu�e � la table tcm (rien � faire de plus)
    * Il est pr�f�rable de travailler sur byte[] tcm plutot que sur ColorModel directement, c'est trop lent */
   public void toPix8(double min, double max, byte [] tcm) {
      double r = 256./(max - min);
      
      for( int y=0; y<heightCell; y++) {
         for( int x=0; x<widthCell; x++ ) {
            double pixIn = getPixelDouble(x+xCell,y+yCell);
            int pix = ( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
                  0xff : (int)( ((pixIn-min)*r) ) & 0xff);
            byte pixOut = tcm[pix];
            setPix8(x+xCell,y+yCell,pixOut);
         }
      }
   }



   /** Remplit le tableau des pixels 8 bits (pix8) � partir des pixels
    * fullbits (uniquement si bitpix==8) avec inversion des lignes */
   public void initPix8() throws Exception {
      if( bitpix!=8 ) throw new Exception("bitpix!=8");
      initPix(pixels,pix8);
   }

   /** Remplit le tableau des pixels Fits (pixels) � partir des pixels
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

   /** D�termine l'intervalle pour un autocut "� la Aladin".
    * @return range[0]..[1] => minPixCut..maxPixCut
    *         range[2]..[3] => minPix..maxPix
    */
   public double [] findFullAutocutRange() throws Exception {
      double[] cut = findAutocutRange(0,0);
      cut[0] = bscale * cut[0] + bzero;
      cut[1] = bscale * cut[1] + bzero;
      return cut;
   }

   /** D�termine l'intervalle pour un autocut "� la Aladin".
    * @return range[0]..[1] => minPixCut..maxPixCut
    *         range[2]..[3] => minPix..maxPix
    */
   public double [] findAutocutRange() throws Exception {
      return findAutocutRange(0,0);
   }
   
   /** D�termine l'intervalle pour un autocut "� la Aladin" en ne consid�rant
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
      return Double.isNaN(pix) || /* !Double.isNaN(blank) &&*/  pix==blank;
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

   private boolean bitmapReleaseDone=false;
   
   /** Lib�ration de la m�moire utilis� par le bitmap des pixels 
    * (on suppose que le fichier pourra �tre relu ult�rieurement)
    * Rq : ne fonctionne que pour les fichiers FITS locaux (RandomAccessFile) qui ont
    * �t� ouvert en mode normal (pas de cellule)
    */
   public void releaseBitmap() throws Exception {
      if( bitpix==0 ) return;  // De fait du JPEG
      testBitmapReleaseFeature();
      bitmapReleaseDone=true;
      pixels=null;
//      System.out.println("releaseBitmap() size="+width+"x"+height+"x"+Math.abs(bitpix)/8+" offset="+bitmapOffset+" de "+filename);
   }
   
   /** Rechargmeent de la m�moire utilis�e par le bitmap des pixels
    * ==> voir releaseBitmap()
    */
   public void reloadBitmap() throws Exception {
      if( bitpix==0 ) return;  // De fait du JPEG
      if( pixels!=null ) return;
      if( !bitmapReleaseDone ) throw new Exception("no releaseBitmap done before");
      testBitmapReleaseFeature();
//      System.out.println("reloadBitmap() size="+width+"x"+height+"x"+Math.abs(bitpix)/8+" offset="+bitmapOffset+" de "+filename);
      RandomAccessFile f = new RandomAccessFile(filename, "r");
      f.seek(bitmapOffset);
      pixels = new byte[width*height*(Math.abs(bitpix)/8)];
      f.readFully(pixels);
      f.close();
      bitmapReleaseDone=false;

   }
   
   private void testBitmapReleaseFeature() throws Exception {
      if( filename==null || bitmapOffset==-1 ) throw new Exception("FITS stream not compatible (not a true file ["+filename+"])");
      if( xCell!=0 || yCell!=0 || widthCell!=width || heightCell!=height ) throw new Exception("Fits subcell not compatible ["+filename+"]");
      if( !(new File(filename)).canRead() ) throw new Exception("FITS does not exist on disk ["+filename+"]");
   }

	/** Pour aider le GC */
   public void free() {
      pixels =null;
      pix8 = null;
      calib=null;
//      center=null;
      headerFits=null;
      width=height=bitpix=0;
      widthCell=heightCell=xCell=yCell=0;
   }

   @Override
   public String toString() {
      return "Fits file: "+width+"x"+height+" bitpix="+bitpix+ " ["+xCell+","+yCell+" "+widthCell+"x"+heightCell+"]";
   }

   // -------------------- C'est priv� --------------------------


  /**
   * D�termination du min et max des pixels pass�s en param�tre
   * @param En sortie : range[0]=minPixCut, range[1]=maxPixCut, range[2]=minPix, range[3]=maxPix;
   * @param pIn Tableau des pixels � analyser
   * @param bitpix codage FITS des pixels
   * @param width Largeur de l'image
   * @param height hauteur de l'image
   * @param minCut Limite min, ou 0 si aucune
   * @param maxCut limite max, ou 0 si aucune
   * @param autocut true si on doit appliquer l'autocut
   * @param ntest Nombre d'appel en cas de traitement r�cursif.
   */
   private void findMinMax(double [] range, byte[] pIn, int bitpix, int width, int height,
            double minCut,double maxCut,boolean autocut,int ntest) throws Exception {
     int i,j,k;
     boolean flagCut=(ntest>0 || minCut!=0. && maxCut!=0.);

     //   Recherche du min et du max
     double max = 0, max1 = 0;
     double min = 0, min1 = 0;

     //   Marge pour l'�chantillonnage (on recherche min et max que sur les 1000 pixels centraux en
     //   enlevant �ventuellement un peu de bord
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
         f.loadFITS("F:/A.fits"); 
//         f.loadFITS("F:/A.fits[900,150,200,400]");
         System.out.println("lecture de "+f.getFilename()+" => "+f);
         int x= 899;
         int y= 149;
         System.out.print("Valeur des pixels en FITS: ");
         for( int i=0; i<10; i++,x++ ) {
            double pix = f.isInCell(x,y) ? f.getPixelDouble(x,y) : Double.NaN;
            System.out.print(" "+pix);
         }
         System.out.println();
         f.free();
                  
         f = new Fits();
//         f.loadJpeg("F:/A.jpg");
         f.loadJpeg("F:/A.jpg[900,150,200,400]");
         System.out.println("lecture de "+f.getFilename()+" => "+f);
         x= 899;
         y= 149;
         System.out.print("Valeur des pixels en JPEG: ");
         for( int i=0; i<10; i++,x++ ) {
            double pixInt = (double) (f.isInCell(x,y) ? f.getPixelInt(x,y) : -1 );
            System.out.print(" "+pixInt);
         }
         System.out.println();

         f.free();


      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

}
