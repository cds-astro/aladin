// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
// This file is part of Aladin.
//
// Aladin is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3 of the License.
//
// Aladin is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// The GNU General Public License is available in COPYING file
// along with Aladin.
//

package cds.fits;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.ImageConsumer;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

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
import cds.image.Hdecomp;
import cds.tools.Util;

/**
 * Classe de manipulation d'un fichier image FITS
 */
final public class Fits {

   // Modes previews supportés
   static public final int PREVIEW_UNKOWN = 0;
   static public final int PREVIEW_JPEG   = 1;
   static public final int PREVIEW_PNG    = 2;

   static public final int PIX_ARGB = 0; // FITS ARGB, PNG couleur => couleur avec transparence
   static public final int PIX_RGB = 1;  // FITS RGB, JPEG couleur => Couleur sans transparence
   static public final int PIX_TRUE = 2; // FITS => vraie valeur (définie par le BITPIX) => transparence sur NaN ou BLANK
   static public final int PIX_256 = 3;  // JPEG N&B => 256 niveaux
   static public final int PIX_255 = 4;  // PNG N&B => 255 niveaux - gère la transparence

   protected int pixMode; // Mode du losange (PIX_ARGB,PIX_RGB, PIX_TRUE, PIX_256, PIX_255

   public static final double DEFAULT_BLANK = Double.NaN;
   public static final double DEFAULT_BSCALE = 1.;
   public static final double DEFAULT_BZERO = 0.;

   public HeaderFits headerFits; // header Fits
   public HeaderFits headerFits0; // Header Fits du HDU0

   public byte[] pixels; // Pixels d'origine "fullbits" (y compté depuis le bas)
   public int bitpix;    // Profondeur des pixels (codage FITS 8,16,32,-32,-64)
   public int width;     // Largeur totale de l'image
   public int height;    // Hauteur totale de l'image
   public int depth=1;   // profondeur de l'image (dans le cas d'un cube)

   public double bzero  = DEFAULT_BZERO;  // BZERO Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
   public double bscale = DEFAULT_BSCALE; // BSCALE Fits pour la valeur physique du pixel (BSCALE*pix+BZEO)
   public double blank  = DEFAULT_BLANK;  // valeur BLANK

   public long bitmapOffset = -1; // Repère le positionnement du bitmap des pixels (voir releaseBitmap());

   // Dans le cas où il s'agit d'une cellule sur l'image (seule une portion de
   // l'image sera accessible)
   public int xCell; // Position X à partir du coin haut gauche de la cellule de l'image (par défaut 0)
   public int yCell; // Position Y à partir du coin haut gauche de la cellule de l'image (par défaut 0)
   public int zCell; // Dans le cas d'un cube, indice de la première frame (par défaut 0)

   public int widthCell; // Largeur de la cellule de l'image (par défaut = naxis1)
   public int heightCell; // Hauteur de la cellule de l'image (par défaut = naxis2)
   public int depthCell;  // Profondeur de la cellule de l'image (par défaut = naxis3)

   public int ext;        // Numéro de l'extension dans le cas d'un MEF

   // public byte [] pix8; // Pixels 8 bits en vue d'une sauvegarde JPEG (y
   // compté depuis le haut)
   public int[] rgb; // pixels dans le cas d'une image couleur RGB

   public Calib calib; // Calibration astrométrique

   /** Donne une approximation de l'occupation mémoire (en bytes) */
   public long getMem() {
      long mem = 12 * 4 + 4 * 8;
      if( calib != null ) mem += calib.getMem();
      if( headerFits != null ) mem += headerFits.getMem();
      if( headerFits0 != null ) mem += headerFits0.getMem();
      if( pixels != null ) mem += pixels.length;
      // if( pix8!=null ) mem+=pix8.length;
      if( rgb != null ) mem += 4*rgb.length;
      return mem;
   }

   public static String FS = System.getProperty("file.separator");

   /** Création en vue d'une lecture */
   public Fits() { }

   /**
    * Création en vu d'une construction "manuelle"
    * @param width  Largeur
    * @param height Hauteur
    * @param depth  Profondeur (cube)
    * @param bitpix Profondeur des pixels (codage FITS 8,16,32,-32,-64 ou 0 pour
    *           RGB)
    */
   public Fits(int width, int height, int bitpix) { this(width,height,1,bitpix); }
   public Fits(int width, int height, int depth, int bitpix) {
      this.width  = this.widthCell  = width;
      this.height = this.heightCell = height;
      this.depth  = this.depthCell  = depth;
      this.bitpix = bitpix;
      ext = xCell = yCell = zCell = 0;
      if( bitpix == 0 ) {
         pixMode = PIX_ARGB;
         rgb = new int[width * height * depth];
      } else {
         pixMode = PIX_TRUE;
         pixels = new byte[width * height * depth * Math.abs(bitpix) / 8];
         headerFits = new HeaderFits();
         headerFits.setKeyValue("SIMPLE", "T");
         headerFits.setKeyValue("BITPIX", bitpix + "");
         headerFits.setKeyValue("NAXIS", depth>1 ? "3" : "2");
         headerFits.setKeyValue("NAXIS1", width + "");
         headerFits.setKeyValue("NAXIS2", height + "");
         if( depth>1 ) headerFits.setKeyValue("NAXIS3", depth + "");
      }
   }


   // public double raMin=0,raMax=0,deMin=0,deMax=0;

   /**
    * Positionnement d'une calibration => initialisation de la coordonnée
    * centrale de l'image (cf center)
    */
   public void setCalib(Calib c) {
      calib = c;
   }

   /** Retourne la calib ou null */
   public Calib getCalib() {
      return calib;
   }

   /* Chargement d'une image preview N&B sous forme d'un JPEG ou d'un PNG */
   public void loadPreview(MyInputStream dis) throws Exception {
      loadPreview(dis, false);
   }

   /** Chargement d'une image Preview (JPEG|PNG) depuis un fichier */
   public void loadPreview(String filename, int x, int y, int w, int h)
         throws Exception {
      loadPreview(filename + "[" + x + "," + y + "-" + w + "x" + h + "]");
   }

   public void loadPreview(String filename) throws Exception {
      loadPreview(filename, false, true, PREVIEW_UNKOWN);
   }

   // Ca particulier où le headerFits ne contient pas la dimension de l'image
   private void bidouilleJPEGPNG(HeaderFits headerFits, String filename) {
      try { headerFits.getIntFromHeader("NAXIS1"); }
      catch( Exception e ) {
         try{
            Fits fits = new Fits();
            MyInputStream is1 = new MyInputStream(new FileInputStream(filename));
            fits.loadPreview(is1);
            headerFits.setKeyValue("NAXIS", "2");
            headerFits.setKeyValue("NAXIS1", fits.width+"");
            headerFits.setKeyValue("NAXIS2", fits.height+"");
            headerFits.setKeyValue("NAXIS3", null);
            fits.free();
         }catch( Exception e1 ) {}
      }
   }

   private Dimension getSizeJPEGPNG(String filename) {
      Dimension dim=null;
      try{
         Fits fits = new Fits();
         MyInputStream is1 = new MyInputStream(new FileInputStream(filename));
         fits.loadPreview(is1);
         dim = new Dimension(fits.width,fits.height);
         fits.free();
      }catch( Exception e1 ) {}
      return dim;
   }

   public void loadPreview(String filename, boolean color,boolean scanCommentCalib,int format) throws Exception {
      filename = parseCell(filename); // extraction de la descrition d'une
      // cellule éventuellement en suffixe du
      // nom fichier.fits[x,y-wxh]
      MyInputStream is = null;
      try {
         is = new MyInputStream(new FileInputStream(filename));
         if( scanCommentCalib ) {
            is.getType(); // Pour être sûr de lire le commentaire éventuel
            if( is.hasCommentCalib() ) {
               Dimension dim = new Dimension(0,0);;
               if( is.hasCommentAVM() )  dim = getSizeJPEGPNG(filename);  // il me faut les dimensions a priori
               headerFits = is.createHeaderFitsFromCommentCalib(dim.width,dim.height);
               bidouilleJPEGPNG(headerFits,filename);

               try {
                  setCalib(new Calib(headerFits));
               } catch( Exception e ) {
                  System.err.println("loadJpeg("+filename+") : no calib found !");
                  calib = null;
               }

            }
         }
         loadPreview(is, xCell, yCell, widthCell, heightCell, color, format);
      } finally {
         if( is != null ) is.close();
      }
      this.setFilename(filename);
   }

   public void loadPreview(MyInputStream dis, boolean flagColor) throws Exception {
      loadPreview(dis, 0, 0, -1, -1, flagColor, PREVIEW_UNKOWN);
   }

   //   /** Chargement d'une image N&B ou COULEUR sous forme d'un JPEG */
   //   protected void loadJpegOne(MyInputStream dis,boolean flagColor) throws Exception {
   //      MyObserver observer = new MyObserver();
   //      Image img = Toolkit.getDefaultToolkit().createImage(dis.readFully());
   //      boolean encore=true;
   //      while( encore ) {
   //         try {
   //            MediaTracker mt = new MediaTracker(null);
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
   ////       BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
   //      // Graphics g = imgBuf.getGraphics();
   //      // g.drawImage(img,0,0,observer);
   //      // g.finalize(); g=null;
   //      // int taille=width*height;
   //      // rgb = new int[taille];
   //      // imgBuf.getRGB(0, 0, width, height, rgb, 0, width);
   //      // if( bitpix!=0 ) {
   //      // pixels = new byte[taille];
   //      // for( int i=0; i<taille; i++ ) pixels[i] = (byte)(rgb[i] & 0xFF);
   //      // rgb=null;
   //      // }
   //
   //      imgBuf.flush(); imgBuf=null;
   //      img.flush(); img=null;
   //   }

   // Mon propre ImageLoader pour éviter le MediaTracker qui nécessite un DISPLAY
   // Attention : je ne teste pas les erreurs
   final class MyImageLoader implements ImageConsumer {

      boolean ready=false;
      boolean hasAlpha=true;

      MyImageLoader() {}

      public void setDimensions(int w, int h) {
         width=w;
         height=h;
         if( widthCell==-1 ) {
            widthCell=w;
            heightCell=h;
            depthCell=1;
            xCell=yCell=zCell=0;
         }
         rgb = new int[widthCell*heightCell];
         //        System.out.println("xCell,yCell="+xCell+","+yCell+" "+widthCell+"x"+heightCell+" wxh="+width+"x"+height);
      }

      public void setPixels(int x, int y, int w, int h, ColorModel model,
            int[] pixels, int off, int scansize) {

         //         for( int j=0; j<h; j++ ) {
         //            System.arraycopy(pixels, off+scansize*j,
         //                  rgb, (height-(y+j)-1)*width+x, w);
         //         }

         y = height - y - 1;
         if( y+h<yCell || y>=yCell+heightCell ) return;

         for( int j=0; j<h; j++ ) {
            int y1 = j+y;
            int yc = y1-yCell;
            if( yc<0 || yc>=heightCell) continue;

            if( hasAlpha && w>=widthCell ) {
               int destPos = yc*widthCell + x-xCell;
               System.arraycopy(pixels, off+scansize*j, rgb, destPos, widthCell);

            } else {
               for( int i=0; i<w; i++ ) {

                  // Coordonnées image originale
                  int x1 = i+x;

                  // Coordonnées cellules
                  int xc = x1-xCell;
                  if( xc<0 || xc>=widthCell ) continue;

                  int pix = pixels[ off+scansize*j +i ];
                  if( !hasAlpha ) pix |=0xFF000000;
                  rgb[ yc*widthCell + xc ] = pix;

               }
            }
         }
      }
      public void imageComplete(int status) { ready=true; }
      public boolean ready() { return ready; }

      public void setProperties(Hashtable< ? , ? > props) { }
      public void setColorModel(ColorModel model) {
         hasAlpha=model.hasAlpha();
      }
      public void setHints(int hintflags) { }
      public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixel, int off, int scansize) {
         y = height - y - 1;
         if( y+h<yCell || y>=yCell+heightCell ) return;
         for( int j=0; j<h; j++ ) {
            int y1 = j+y;
            int yc = y1-yCell;
            if( yc<0 || yc>=heightCell) continue;
            for( int i=0; i<w; i++ ) {

               // Coordonnées image originale
               int x1 = i+x;

               // Coordonnées cellules
               int xc = x1-xCell;
               if( xc<0 || xc>=widthCell ) continue;

               int pix = (0xFF & pixel[ off+scansize*j +i ]);
               pix = (pix<<16) | (pix<<8) | pix;
               if( !hasAlpha ) pix |=0xFF000000;
               rgb[ yc*widthCell + xc ] = pix;
            }
         }
      }
   }


   //   static public void main(String []arg) {
   //      try {
   //         String input  = "/Users/Pierre/Desktop/Data/PNG/OD121_0x500018feL_SpirePhotoLargeScan_rcw120_ExtEmiGainsApplied_destriped_RGB_S.png";
   //         String output = "/Users/Pierre/Desktop/Data/PNG/Test.png";
   //         Fits f = new Fits();
   //         MyInputStream dis = new MyInputStream(new FileInputStream(input));
   //         System.out.println("Lecture de "+input+"...");
   //         f.loadJpeg(dis,true);
   //         System.out.println("Ecriture de "+output+"...");
   //         f.writeRGBcompressed(output, "png");
   //         System.out.println("The end");
   //      } catch( Exception e ) {
   //         e.printStackTrace();
   //      }
   //   }

   
   // Test si un Display X11 est bien accessible
   static boolean X11OK=true;;
   static {
      try { Toolkit.getDefaultToolkit( ); } catch( Exception e) { X11OK=false; }
      System.out.println("X11 Display => "+X11OK);
   }
   
   public void loadPreview(MyInputStream dis, int x, int y, int w, int h, boolean flagColor, int format) throws Exception {
      BufferedImage imgBuf;

      bitpix = flagColor ? 0 : 8;

      if( (long)width*height>Integer.MAX_VALUE ) throw new Exception("Too large image for JAVA API");

      // Détermination du format si inconnu
      if( format==PREVIEW_UNKOWN ) {
         format = dis.getType() == MyInputStream.PNG ? PREVIEW_PNG : PREVIEW_JPEG;
      }

      // Lecture de l'image complète avec la méthode de base (plus rapide que le
      // BufferedImage loader)
      // En revanche il faut réimplanter un ImageConsumer pour éviter l'usage
      // d'un Component qui requière un DISPLAY (via MediaTracker)
      if(( w==-1 && flagColor && X11OK )  ) {
         widthCell = w;
         heightCell = h;
         xCell = x;
         yCell = y;
         pixMode = format==PREVIEW_PNG ? PIX_ARGB : PIX_RGB;
         Image img = Toolkit.getDefaultToolkit( ).createImage(dis.readFully());
         MyImageLoader loader = new MyImageLoader();
         img.getSource().startProduction(loader);
         while( !loader.ready() ) Util.pause(5);
         if( width==-1 ) throw new Exception("MyLoader error");

      // Lecture via ImageReader et éventuellement par cellules, plus lente
      } else {
         String coding = format==PREVIEW_PNG ? "png" : "jpeg";
         Iterator readers = ImageIO.getImageReadersByFormatName(coding);
         ImageReader reader = (ImageReader) readers.next();
         ImageInputStream iis = null;
         try {
            iis = ImageIO.createImageInputStream(dis);
            reader.setInput(iis, true);
            width = reader.getWidth(0);
            height = reader.getHeight(0);
            if( w==-1 ) {
               w=width;
               h=height;
               y=x=0;
            }

            // Ouverture juste d'une cellule de l'image
            if( x < 0 || y < 0 || x + w > width || y + h > height ) throw new Exception(
                  "Mosaic cell outside the image (" + width + "x" + height
                  + ") cell=[" + x + "," + y + " " + w + "x" + h + "]");
            widthCell = w;
            heightCell = h;
            xCell = x;
            yCell = y;

            ImageReadParam param = reader.getDefaultReadParam();
            if( this.widthCell != width || this.heightCell != height ) {
               int yJpegCell;
               if( RGBASFITS ) yJpegCell = height - yCell - heightCell;
               else yJpegCell = yCell;
               Rectangle r = new Rectangle(xCell, yJpegCell, widthCell, heightCell);
               param.setSourceRegion(r);
            }
            imgBuf = reader.read(0, param);
            reader.dispose();

         } finally { if( iis!=null ) iis.close(); }

         if( flagColor ) {
            pixMode = format==PREVIEW_PNG ? PIX_ARGB : PIX_RGB;
            int[] rgb1 = imgBuf.getRGB(0, 0, widthCell, heightCell, null, 0, widthCell);
            if( RGBASFITS ) invImageLine(widthCell, heightCell, rgb1);
            rgb = rgb1;
            rgb1 = null;

         } else {
            pixMode = format==PREVIEW_PNG ? PIX_255 : PIX_256;
            pixels = ((DataBufferByte) imgBuf.getRaster().getDataBuffer()).getData();
            if( RGBASFITS ) invImageLine(widthCell, heightCell, pixels);
         }

         try { dis.close(); }
         catch( Exception e ) {}

         imgBuf.flush();
         imgBuf = null;
      }

   }

   protected static void invImageLine(int width, int height, byte[] pixels) {
      byte[] tmp = new byte[width];
      for( int h = height / 2 - 1; h >= 0; h-- ) {
         int offset1 = h * width;
         int offset2 = (height - h - 1) * width;
         System.arraycopy(pixels, offset1, tmp, 0, width);
         System.arraycopy(pixels, offset2, pixels, offset1, width);
         System.arraycopy(tmp, 0, pixels, offset2, width);
      }
      tmp = null;
   }

   protected static void invImageLine(int width, int height, int[] pixels) {
      int[] tmp = new int[width];
      for( int h = height / 2 - 1; h >= 0; h-- ) {
         int offset1 = h * width;
         int offset2 = (height - h - 1) * width;
         System.arraycopy(pixels, offset1, tmp, 0, width);
         System.arraycopy(pixels, offset2, pixels, offset1, width);
         System.arraycopy(tmp, 0, pixels, offset2, width);
      }
      tmp = null;
   }

   // Extraction de la définition d'une cellule FITS et d'un numéro d'extension
   // pour une ouverture d'un fichier FITS en mode "mosaic"
   // le filename doit être suffixé par [e:x,y,z-wxhxd] (sans aucun blanc).
   // ou bien [e] ou [x,y,z-wxhxd] (sachant que z et d sont facultatifs)
   // => met à jour les variables xCell, yCell, zCell, widthCell, heigthCell et depthCell
   // S'il n'y a pas de définition de cellule xCell, yCell et zCell à 0 et
   // widthCell,heightCell et depthCell à -1. S'il n'y a pas de définition d'extension mef=0
   // @return le nom de fichier sans le suffixe
   public String parseCell(String filename) throws Exception {
      ext = xCell = yCell = yCell = 0;
      widthCell = heightCell = heightCell = -1;
      int deb = filename.lastIndexOf('[');
      if( deb == -1 ) return filename;
      int fin = filename.indexOf(']', deb);
      if( fin == -1 ) return filename;
      int p;
      boolean flagCell = (p=filename.indexOf(',',deb))>0;
      boolean flagZ = filename.indexOf(',',p+1)>0;
      boolean flagMef = !flagCell || filename.indexOf(':',deb)>0 ;
      StringTokenizer st = new StringTokenizer( filename.substring(deb + 1, fin), ":,-x");
      try {
         if( flagMef ) ext=Integer.parseInt(st.nextToken());
         if( flagCell ) {
            xCell = Integer.parseInt(st.nextToken());
            yCell = Integer.parseInt(st.nextToken());
            zCell = flagZ ? Integer.parseInt(st.nextToken()) : 0;
            widthCell = Integer.parseInt(st.nextToken());
            heightCell = Integer.parseInt(st.nextToken());
            depthCell = flagZ ? Integer.parseInt(st.nextToken()) : 1;
         }
      } catch( Exception e ) {
         throw new Exception("Bad cell mosaic FITS definition => " + filename);
      }
      return filename.substring(0, deb);
   }

   /** Chargement d'une image FITS depuis un fichier */
   public void loadFITS(String filename, int ext, int x, int y, int w, int h)
         throws Exception {
      loadFITS(filename + "[" + ext + ":" + x + "," + y + "-" + w + "x" + h + "]");
   }

   /** Chargement d'une image FITS depuis un fichier */
   public void loadFITS(String filename, int ext, int x, int y, int z, int w, int h, int d)
         throws Exception {
      loadFITS(filename + "[" + ext + ":" + x + "," + y + "," + z + "-" + w + "x" + h + "x" + d + "]");
   }

   public void loadFITS(String filename) throws Exception {
      loadFITS(filename, false, true);
   }

   public void loadFITS(String filename, boolean color, boolean flagLoad)
         throws Exception {
      filename = parseCell(filename); // extraction de la descrition d'une
      // cellule éventuellement en suffixe du
      // nom fichier.fits[x,y-wxh]
      MyInputStream is = null;
      try {
         is = new MyInputStream(new FileInputStream(filename));
         is = is.startRead();
         if( color ) {
            if( widthCell < 0 ) throw new Exception( "Mosaic mode not supported yet for FITS color file");
            loadFITSColor(is);
         } else loadFITS(is, ext, xCell, yCell, zCell, widthCell, heightCell, depthCell, flagLoad);
      } finally {
         if( is != null ) is.close();
      }
      this.setFilename(filename);
   }

   /** Chargement d'une image FITS */
   public void loadFITS(MyInputStream dis) throws Exception {
      loadFITS(dis, 0,  0,0,0, -1,-1,-1);
   }

   /** Chargement d'une cellule d'une image FITS */
   public void loadFITS(MyInputStream dis, int ext, int x, int y, int z, int w, int h, int d) throws Exception {
      loadFITS(dis,ext, x, y, z, w, h, d, true);
   }

   // Se cale sur le début de l'extension indiqué par le numéro ext
   private void moveOnHUD(MyInputStream dis, int ext) throws Exception {
      boolean first=true;
      for( ; ext>0; ext-- ) {
         HeaderFits h = new HeaderFits(dis);
         if( first ) { headerFits0 = h; first=false; }  // pour conserver le HDU0
         int bitpix = h.getIntFromHeader("BITPIX");
         int naxis = h.getIntFromHeader("NAXIS");
         if( naxis==0 ) continue;
         int gcount=1;
         try { gcount = h.getIntFromHeader("GCOUNT"); } catch( Exception e) {}
         int pcount=0;
         try { pcount = h.getIntFromHeader("PCOUNT"); } catch( Exception e) {}
         long taille = 1L;
         for( int i=1; i<=naxis; i++ ) {
            int n = h.getIntFromHeader("NAXIS"+i);
            taille *= n;
         }
         taille += pcount;
         taille *= gcount*Math.abs(bitpix)/8;
         dis.skip(taille);
         dis.skipOnNext2880();
      }
   }

   private boolean skipHDU0 = true;

   /** Dans le cas d'un MEF, indique qu'il faut, ou ne faut pas passer la première entête si elle est vide */
   public void setSkipHDU0(boolean flag) { skipHDU0 = flag; }

   public void loadFITS(MyInputStream dis, int ext, int x, int y, int z, int w, int h, int d, boolean flagLoad) throws Exception {

      dis = dis.startRead();
      // boolean flagHComp = (dis.getType() & MyInputStream.HCOMP) !=0;
      boolean flagHComp = dis.isHCOMP();
      if( flagHComp || dis.isGZ() ) {
         releasable = false;
         flagLoad = true;
      }

      // On passe les extensions qu'on ne veut pas
      moveOnHUD(dis,ext);

      pixMode = PIX_TRUE;
      headerFits = new HeaderFits(dis);

      // Si jamais la première HDU est vide, on se cale automatiquement sur la suivante, sinon on sort
      if( ext==0 &&  headerFits.getIntFromHeader("NAXIS")==0 ) {
         if( !skipHDU0 ) return;
         headerFits = new HeaderFits(dis);
      }

      bitpix = headerFits.getIntFromHeader("BITPIX");
      width = headerFits.getIntFromHeader("NAXIS1");
      height = headerFits.getIntFromHeader("NAXIS2");
      try { depth = headerFits.getIntFromHeader("NAXIS3"); }
      catch( Exception e ) { depth=1; }
      
      // ATTENTION RICE IMAGE NON ENCORE SUPPORTEE
      if( headerFits.getStringFromHeader("ZBITPIX")!=null ) throw new Exception("RICE image not supported");

      // Ouverture complète de l'image
      if( w == -1 ) {
         widthCell  = width;
         heightCell = height;
         depthCell  = depth;
         xCell = yCell = zCell = 0;

         // Ouverture juste d'une cellule de l'image
      } else {
         if( x < 0 || y < 0 || x + w > width || y + h > height || z<0 || z+d > depth ) throw new Exception(
               "Mosaic cell outside the image (" + width + "x" + height
               + ") cell=[" + x + "," + y + "," + z + " " + w + "x" + h + "x" + d + "]");
         widthCell  = w;
         heightCell = h;
         depthCell  = d;
         xCell = x;
         yCell = y;
         zCell = z;
      }
      try {
         blank = headerFits.getDoubleFromHeader("BLANK");
      } catch( Exception e ) {
         blank = /* bitpix>0 ? 0 : */DEFAULT_BLANK;
      }

      int n = (Math.abs(bitpix) / 8);
      bitmapOffset = dis.getPos();

      int size = widthCell * heightCell * depthCell * n;

      // Pas le choix, il faut d'abord tout lire, puis ne garder que la cellule
      // si nécessaire
      if( flagHComp ) {
         byte[] buf = Hdecomp.decomp(dis);
         if( w == -1 ) pixels = buf;
         else {
            pixels = new byte[size];
            for( int frame=0; frame<depthCell; frame++ ) {
               for( int lig = 0; lig < heightCell; lig++ )
                  System.arraycopy(buf, ( (zCell+frame)*width*height+  (yCell+lig) * width + xCell) * n, pixels,
                        (frame*widthCell*heightCell +lig * widthCell) * n, widthCell * n );
            }
         }

      } else {
         if( flagLoad || bitpix == 8 ) {
            pixels = new byte[size];

            // Lecture d'un coup
            if( w==-1 ) dis.readFully(pixels);

            // Lecture continue d'une cellule qui contient des lignes entières
            else if( w==width && depth==1 ) {
               dis.skip( (long)yCell * width * n);
               dis.readFully(pixels);
            }

            // Lecture ligne à ligne pour mémoriser uniquement la cellule
            else {

               dis.skip( (long)zCell * width*height*n );
               for( int frame=0; frame<depthCell; frame++ ) {
                  dis.skip( (long)yCell * width * n);
                  byte[] buf = new byte[ width * n ]; // une ligne complète
                  for( int lig = 0; lig < heightCell; lig++ ) {
                     dis.readFully(buf);
                     System.arraycopy(buf, xCell * n , pixels, frame*widthCell*heightCell + lig * widthCell * n, widthCell * n);
                  }
                  dis.skip((height - (yCell + heightCell)) * width * (long)n);
               }
               dis.skip( (depth - (zCell + depthCell)) *width*height * (long)n );
            }
         } else bitmapReleaseDone = true;

      }
      try {
         bscale = headerFits.getDoubleFromHeader("BSCALE");
      } catch( Exception e ) {
         bscale = DEFAULT_BSCALE;
      }
      try {
         bzero = headerFits.getDoubleFromHeader("BZERO");
      } catch( Exception e ) {
         bzero = DEFAULT_BZERO;
      }
      try {
         setCalib(new Calib(headerFits));
      } catch( Exception e ) {
         calib = null;
      }
      // if( bitpix==8 ) initPix8();
   }

   /** Chargement d'une image FITS couleur mode ARGB */
   public void loadFITSARGB(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      width = widthCell = headerFits.getIntFromHeader("NAXIS1");
      height = heightCell = headerFits.getIntFromHeader("NAXIS2");
      xCell = yCell = 0;
      pixMode = PIX_ARGB;
      pixels = new byte[widthCell * heightCell * 32];
      dis.readFully(pixels);
      setARGB();
      try {
         setCalib(new Calib(headerFits));
      } catch( Exception e ) {
         calib = null;
      }
   }

   /** Chargement d'une image FITS couleur mode RGB cube */
   public void loadFITSColor(MyInputStream dis) throws Exception {
      headerFits = new HeaderFits(dis);
      bitpix = headerFits.getIntFromHeader("BITPIX");
      width  = widthCell  = headerFits.getIntFromHeader("NAXIS1");
      height = heightCell = headerFits.getIntFromHeader("NAXIS2");
      depth  = depthCell  = 1;
      xCell = yCell = zCell = 0;
      pixMode = PIX_RGB;
      pixels = new byte[widthCell * heightCell];
      dis.readFully(pixels);

      byte[] t2 = new byte[widthCell * heightCell];
      dis.readFully(t2);
      byte[] t3 = new byte[widthCell * heightCell];
      dis.readFully(t3);
      rgb = new int[widthCell * heightCell];
      for( int i = 0; i < heightCell * widthCell; i++ ) {
         int val = 0;
         val = 0 | (((pixels[i]) & 0xFF) << 16) | (((t2[i]) & 0xFF) << 8)
               | (t3[i]) & 0xFF;
         rgb[i] = val;
      }
      try {
         bscale = headerFits.getIntFromHeader("BSCALE");
      } catch( Exception e ) {
         bscale = DEFAULT_BSCALE;
      }
      try {
         bzero = headerFits.getIntFromHeader("BZERO");
      } catch( Exception e ) {
         bzero = DEFAULT_BZERO;
      }
      try {
         setCalib(new Calib(headerFits));
      } catch( Exception e ) {
         calib = null;
      }

   }

   static final public int GZIP  = 1;
   static final public int HHH   = 1 << 1;
   static final public int COLOR = 1 << 2;
   static final public int XFITS = 1 << 3;
   static final public int HDU0SKIP = 1 << 4;

   /**
    * Chargement de l'entete d'une image FITS depuis un fichier
    * @return un code GZIP|HHH|COLOR pour savoir de quoi il s'agit
    */
   public int loadHeaderFITS(String filename) throws Exception {
      filename = parseCell(filename); // extraction de la descrition d'une
      // cellule éventuellement en suffixe du
      // nom fichier.fits[ext:x,y-wxh]
      int code = 0;
      MyInputStream is = null;
      try {
         is = new MyInputStream(new FileInputStream(filename));
         if( is.isGZ() ) {
            code |= GZIP;
            is = is.startRead();
         }
         //         long type = is.getType();
         long type = is.getType(10000);

         if( (type&(MyInputStream.JPEG|MyInputStream.PNG))!=0 && !is.hasCommentCalib() ) {
            is.fastExploreCommentOrAvmCalib(filename);
         }

         // Cas spécial d'un fichier .hhhh
         if( filename.endsWith(".hhh") ) {
            byte[] buf = is.readFully();
            headerFits = new HeaderFits();
            headerFits.readFreeHeader(new String(buf), true, null);
            code |= HHH;

            // Cas d'un fichier PNG ou JPEG avec un commentaire contenant la
            // calib
         } else if( is.hasCommentCalib() ) {
            Dimension dim = new Dimension(0,0);;
            if( is.hasCommentAVM() )  dim = getSizeJPEGPNG(filename);  // il me faut les dimensions a priori
            headerFits = is.createHeaderFitsFromCommentCalib(dim.width,dim.height);
            bidouilleJPEGPNG(headerFits,filename);
            
            // Cas habituel
         }  else {
            moveOnHUD(is, ext);
            headerFits = new HeaderFits(is);

            // Si jamais la première HDU est vide, on se cale automatiquement sur la suivante, sinon on sort
            if( ext==0 &&  headerFits.getIntFromHeader("NAXIS")==0 ) {
               if( !skipHDU0 ) return 0;
               headerFits0 = headerFits;   // on garde la première HDU pour recherche éventuel de mots clés
               headerFits = new HeaderFits(is);
               
            }
            
            // Cas particulier d'une image RICE
            if( headerFits.getStringFromHeader("ZBITPIX")!=null ) {
               // Nécessaire pour que la création de la Calib ne se plante pas
               headerFits.setKeyword("BITPIX", headerFits.getStringFromHeader("ZBITPIX") );
               headerFits.setKeyword("NAXIS1", headerFits.getStringFromHeader("ZNAXIS1") );
               headerFits.setKeyword("NAXIS2", headerFits.getStringFromHeader("ZNAXIS2") );
               headerFits.setKeyword("NAXIS","2");
            }

            bitmapOffset = is.getPos();

            try {
               bitpix = headerFits.getIntFromHeader("BITPIX");
            } catch( Exception e1 ) {
               bitpix = 0;
            }
         }
         
         if( bitpix == 0 ) code |= COLOR;

         try {
            width = headerFits.getIntFromHeader("NAXIS1");
            height = headerFits.getIntFromHeader("NAXIS2");
            try { depth = headerFits.getIntFromHeader("NAXIS3"); }
            catch( Exception e ) { depth = 1; }

            if( !hasCell() ) {
               xCell = yCell = zCell = 0;
               widthCell  = width;
               heightCell = height;
               depthCell  = depth;
            }
            try {
               blank = headerFits.getDoubleFromHeader("BLANK");
            } catch( Exception e ) {
               blank = DEFAULT_BLANK;
            }
            try {
               bscale = headerFits.getDoubleFromHeader("BSCALE");
            } catch( Exception e ) {
               bscale = DEFAULT_BSCALE;
            }
            try {
               bzero = headerFits.getDoubleFromHeader("BZERO");
            } catch( Exception e ) {
               bzero = DEFAULT_BZERO;
            }
            try {
               setCalib(new Calib(headerFits));
            } catch( Exception e ) {
               calib=null;
//               if( Aladin.levelTrace >= 3 ) e.printStackTrace();
            }
         } catch( Exception e ) {
            if( Aladin.levelTrace >= 3 ) e.printStackTrace();
            calib = null;
         }
         this.setFilename(filename);
      } finally {
         if( is != null ) is.close();
      }
      return code;
   }

   /** Retourne la valeur BSCALE (1 si non définie) */
   public double getBscale() {
      return bscale;
   }

   /** Retourne la valeur BZERO (0 si non définie) */
   public double getBzero() {
      return bzero;
   }

   /** Retourne la valeur BLANK si elle existe (tester avec hasBlank() */
   public double getBlank() {
      return blank;
   }

   /**
    * Positionement d'une valeur BSCALE - si égale à 1, supprime le mot clé du
    * header fits
    */
   public void setBscale(double bscale) {
      this.bscale = bscale;
      if( headerFits != null ) headerFits.setKeyValue("BSCALE",
            bscale == 1 ? (String) null : bscale + "");
   }

   /**
    * Positionement d'une valeur BZERO - si égale à 0, supprime le mot clé du
    * header fits
    */
   public void setBzero(double bzero) {
      this.bzero = bzero;
      if( headerFits != null ) headerFits.setKeyValue("BZERO",
            bzero == 0 ? (String) null : bzero + "");
   }

   /** Positionement d'une valeur BLANK. Double.NaN est supporté */
   public void setBlank(double blank) {
      this.blank = blank;
      if( headerFits != null ) headerFits.setKeyValue("BLANK",
            Double.isNaN(blank) ? (String) null : (bitpix>0?(int)blank:blank) + "");
   }

   /**
    * Positionnement du flag COLORMOD = ARGB pour signifier qu'il s'agit d'un
    * FITS couleur ARGB
    */
   public void setARGB() {
      setARGB(true);
   }

   public void setARGB(boolean reverse) {
      pixMode = PIX_ARGB;
      bitpix = 0;
      if( headerFits != null ) headerFits.setKeyValue("COLORMOD", "ARGB");

      // Génération du tableau rgb[] à partir des pixels ARGB stockés dans
      // pixels[]
      rgb = new int[widthCell * heightCell];
      for( int y = 0; y < heightCell; y++ ) {
         for( int x = 0; x < widthCell; x++ ) {
            int i = y * widthCell + x;
            int offset = reverse ? (heightCell - y - 1) * widthCell + x : i;
            rgb[offset] = (pixels[i * 4] & 0xFF) << 24
                  | (pixels[i * 4 + 1] & 0xFF) << 16
                  | (pixels[i * 4 + 2] & 0xFF) << 8
                  | (pixels[i * 4 + 3] & 0xFF);
         }
      }
   }

   /**
    * Crée si nécessaire le répertoire correspondant au filename
    * @param filename
    */
   private void createDir(String filename) throws Exception {
      cds.tools.Util.createPath(filename);
      // File dir = new File(filename).getParentFile();
      // if( !dir.exists() ) {
      // dir.mkdirs();
      // }
   }

   // /** Génération d'un fichier FITS (sans calibration) */
   // public void writeFITS8(OutputStream os) throws Exception {
   // headerFits.setKeyValue("BITPIX", bitpix+"");
   // headerFits.writeHeader(os);
   // os.write(pix8);
   // }
   //
   // /** Génération d'un fichier FITS (sans calibration) */
   // public void writeFITS8(String filename) throws Exception {
   // createDir(filename);
   // OutputStream os = new FileOutputStream(filename);
   // writeFITS8(os);
   // os.close();
   // this.setFilename(filename);
   // }

   static public byte[] getBourrage(int currentPos) {
      int n = currentPos % 2880;
      int size = n == 0 ? 0 : 2880 - n;
      byte[] b = new byte[size];
      return b;
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS(OutputStream os) throws Exception {
      int size = headerFits.writeHeader(os);
      bitmapOffset = size;

      // FITS couleur en mode ARGB
      if( pixMode == PIX_ARGB || pixMode == PIX_RGB ) {
         byte[] buf = new byte[rgb.length * 4];
         for( int i = 0; i < rgb.length; i++ ) {
            int pix = rgb[i];
            buf[i * 4] = (byte) (pixMode == PIX_ARGB ? (pix >> 24) & 0xFF
                  : 0xFF);
            buf[i * 4 + 1] = (byte) ((pix >> 16) & 0xFF);
            buf[i * 4 + 2] = (byte) ((pix >> 8) & 0xFF);
            buf[i * 4 + 3] = (byte) (pix & 0xFF);
         }
         os.write(buf);
         size += buf.length;
         buf = null;

         // Fits classique
      } else {
         os.write(pixels);
         size += pixels.length;
      }

      // Ecriture des éventuelles extensions
      if( extHeader == null ) return;
      int n = extHeader.size();
      for( int i = 0; i < n; i++ ) {
         byte[] b = getBourrage(size);
         size += b.length;
         os.write(b);
         HeaderFits h = (HeaderFits) extHeader.elementAt(i);
         h.writeHeader(os);
         byte[] p = (byte[]) extPixels.elementAt(i);
         os.write(p);
         size += p.length;
      }

      // Bourrage final
      // POUR DES TUILES HiPS, PAS NECESSAIRE
      //      os.write(getBourrage(size)); // Quel gachi !
   }

   /** Génération d'un fichier FITS (sans calibration) */
   public void writeFITS(String filename) throws Exception { writeFITS(filename,false); }
   public void writeFITS(String filename,boolean zip) throws Exception {
      createDir(filename);
      OutputStream os = null;
      try {
         os = new FileOutputStream(filename);
         if( zip ) os = new GZIPOutputStream(os);
         writeFITS(os);
      } finally {
         os.close();
      }
      this.setFilename(filename);
   }

   private Vector extHeader = null;

   private Vector extPixels = null;

   /** Ajout d'extensions (uniquement des images - ou des cubes) */
   public void addFitsExtension(HeaderFits header, byte[] pixels) {
      if( extHeader == null ) {
         extHeader = new Vector();
         extPixels = new Vector();
      }
      headerFits.setKeyValue("EXTEND", "T");
      extHeader.addElement(header);
      extPixels.addElement(pixels);
   }

   /** Suppression de toutes les extensions */
   public void clearExtensions() {
      extHeader = extPixels = null;
   }

   /**
    * Génération d'un fichier JPEG à partir des pixels 8bits Ceux-ci doivent
    * être positionnés manuellement via la méthode setPix8(x,y,val)
    */
   public void writeCompressed(String file, double pixelMin, double pixelMax,
         byte[] tcm, String format) throws Exception {
      createDir(file);
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(new File(file));
         writePreview(fos, pixelMin, pixelMax, tcm, format);
         setReleasable(false);
      } finally {
         if( fos!=null ) fos.close();
      }
      this.setFilename(file);
   }
   
   private static Toolkit kit = null;
   private static Object lockKit = new Object();
   
   public static void setToolKit() {
      if( kit==null ) {
         synchronized( lockKit ) {
            try { ImageIO.setUseCache(false); } catch( Throwable e) {}
            if( kit==null ) kit = Toolkit.getDefaultToolkit();
         }
      }
   }
   
//   BufferedImage bufferedImage;
//   if( img instanceof BufferedImage ) bufferedImage = (BufferedImage)img;
//   else {
//      bufferedImage = new BufferedImage(
//            img.getWidth(null),
//            img.getHeight(null),
//            RGB ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_GRAY );
//      Graphics g = bufferedImage.createGraphics();
//      g.drawImage(img,0,0,aladin);
//      g.dispose();
//   }
//   aladin.waitImage(img);
   
   
//   /** writes a BufferedImage of type TYPE_INT_ARGB to PNG using PNGJ */
//   public static void writeARGB(BufferedImage bi, OutputStream os) {
//       if(bi.getType() != BufferedImage.TYPE_INT_ARGB) 
//          throw new PngjException("This method expects  BufferedImage.TYPE_INT_ARGB" );
//       ImageInfo imi = new ImageInfo(bi.getWidth(), bi.getHeight(), 8, true);
//       PngWriter pngw = new PngWriter(os, imi);
//       pngw.setCompLevel(9);// maximum compression, not critical usually
//       pngw.setFilterType(FilterType.FILTER_ADAPTIVE_FAST); // see what you prefer here
//       DataBufferInt db =((DataBufferInt) bi.getRaster().getDataBuffer());
//       SinglePixelPackedSampleModel samplemodel =  (SinglePixelPackedSampleModel) bi.getSampleModel();
//       if(db.getNumBanks()!=1) 
//           throw new PngjException("This method expects one bank");
//       int [] scanline = new int[ imi.samplesPerRow*imi.rows ];
//       ImageLineInt line = new ImageLineInt(imi,scanline);
//       for (int row = 0; row < imi.rows; row++) {
//           int elem=samplemodel.getOffset(0,row);
//           for (int col = 0,j=0; col < imi.cols; col++) {
//               int sample = db.getElem(elem++);
//               scanline[j++] =  (sample & 0xFF0000)>>16; // R
//               scanline[j++] =  (sample & 0xFF00)>>8; // G
//               scanline[j++] =  (sample & 0xFF); // B
//               scanline[j++] =  (((sample & 0xFF000000)>>24)&0xFF); // A
//           }
//           pngw.writeRow(line, row);
//       }
//       pngw.end();
//   }


   public void writePreview(OutputStream os, double pixelMin,
         double pixelMax, byte[] tcm, String format) throws Exception {
      Image imgSrc;
      BufferedImage imgTarget;
      int[] rgb = this.rgb;
      int typeInt = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

      setToolKit();
      
      if( bitpix == 0 || pixMode == PIX_RGB || pixMode == PIX_ARGB ) {
         if( RGBASFITS ) {
            rgb = new int[this.rgb.length];
            System.arraycopy(this.rgb, 0, rgb, 0, rgb.length);
            invImageLine(widthCell, heightCell, rgb);
         }
         imgSrc = kit.createImage( new MemoryImageSource(widthCell, heightCell, rgb, 0, widthCell));
         imgTarget = new BufferedImage(width, height, typeInt);
      } else {
         int targetPixMode = format.equals("png") ? PIX_255 : PIX_256;
         byte[] pix8 = toPix8(pixelMin, pixelMax, tcm, targetPixMode);
         imgSrc = kit.createImage(
               new MemoryImageSource(widthCell, heightCell, getCM(targetPixMode), pix8, 0, widthCell));
         //          imgTarget = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED,(IndexColorModel) getCM(targetPixMode));
         imgTarget = new BufferedImage(width, height, typeInt);
      }

      Graphics g = imgTarget.createGraphics();
      int yJpegCell = RGBASFITS ? height - yCell - heightCell : yCell;
      g.drawImage(imgSrc, xCell, yJpegCell, null); // observer);
      g.dispose();
      
//      if( true ) { writeARGB(imgTarget, os); return; }

      // if( RGBASFITS && bitpix==0 ) invImageLine(widthCell,heightCell, rgb);

      ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
      ImageWriteParam iwp = writer.getDefaultWriteParam();
      if( format.equals("jpeg") ) {
         iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
         iwp.setCompressionQuality(0.95f);
      }

      ImageOutputStream out = null;
      try {
         out = ImageIO.createImageOutputStream(os);
         writer.setOutput(out);
         writer.write(null, new IIOImage(imgTarget, null, null), iwp);
         writer.dispose();
      } finally { if( out!=null ) out.close(); }
   }

   /**
    * Génération d'un fichier JPEG ou PNG à partir des pixels RGB
    * @param format "jpeg" ou "png"
    */
//   public void writeRGBPreview(String file, String format) throws Exception {
//      createDir(file);
//      FileOutputStream fos = null;
//      try{
//         fos = new FileOutputStream(new File(file));
//         writeRGBPreview(fos, format);
//      } finally { if( fos!=null )  fos.close(); }
//      this.setFilename(file);
//   }

//   public void writeRGBPreview(OutputStream os, String format)
//         throws Exception {
//      Image img;
//      int typeInt = format.equals("png") ? BufferedImage.TYPE_INT_ARGB
//            : BufferedImage.TYPE_INT_RGB;
//
//      img = Toolkit.getDefaultToolkit().createImage(
//            new MemoryImageSource(widthCell, heightCell, rgb, 0, widthCell));
//
//      BufferedImage bufferedImage = new BufferedImage(width, height, typeInt);
//      Graphics g = bufferedImage.createGraphics();
//      g.drawImage(img, xCell, yCell, null); //observer);
//      g.dispose();
//
//      ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
//      ImageWriteParam iwp = writer.getDefaultWriteParam();
//      if( format.equals("jpeg") ) {
//         iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//         iwp.setCompressionQuality(0.95f);
//      }
//
//      ImageOutputStream out = null;
//      try {
//         out = ImageIO.createImageOutputStream(os);
//         writer.setOutput(out);
//         writer.write(null, new IIOImage(bufferedImage, null, null), iwp);
//         writer.dispose();
//      } finally { if( out!=null ) out.close(); }
//   }

   /**
    * Retourne true s'il s'agit d'un mode graphique qui supporte la transparence
    */
   protected boolean isTransparent() {
      return isTransparent(pixMode);
   }

   static public boolean isTransparent(int pixMode) {
      return pixMode == PIX_255 || pixMode == PIX_TRUE || pixMode == PIX_ARGB;
   }

   private ColorModel getCM(int pixMode) {
      byte[] r = new byte[256];
      boolean transp = isTransparent(pixMode);
      int gap = transp ? 1 : 0;
      for( int i = 1; i < r.length; i++ )
         r[i] = (byte) (i - gap);
      return transp ? new IndexColorModel(8, 256, r, r, r, 0)
      : new IndexColorModel(8, 256, r, r, r);
   }

   static final public boolean JPEGORDERCALIB = false; // true => Ne fait pas le
   // complément à la
   // hauteur lors des
   // calculs de coord via
   // Calib pour les JPEG

   static final public boolean JPEGFROMTOP = false; // true => les lignes dans
   // rgb[] sont comptées
   // depuis le haut

   static final public boolean RGBASFITS = true; // true => les lignes dans
   // rgb[] sont comptées depuis
   // le bas comme en FITS

   /** Retourne la valeur RGB du pixel dans le cas d'une image couleur RGB */
   public int getPixelRGB(int x, int y) {
      return rgb[(y - yCell) * widthCell + (x - xCell)];
   }

   public int getPixelRGBJPG(int x, int y) {
      if( Fits.JPEGFROMTOP ) return rgb[((height - y - 1) - yCell) * widthCell + (x - xCell)];
      return rgb[(y - yCell) * widthCell + (x - xCell)];
   }

   /**
    * Retourne la description de la cellule courante selon la syntaxe [x,y-wxh] ou [x,y,z-wxhxd]
    * ou "" si le fichier n'a pas été ouvert en mode mosaic
    */
   public String getCellSuffix() {
      if( !hasCell() )  return ext==0 ? "" : "["+ext+"]";
      return "["
      +(ext==0?"":ext+":")
      + xCell + "," + yCell + (depth>1?","+zCell:"") + "-" + widthCell + "x" + heightCell + (depth>1?"x"+depthCell:"")
      + "]";
   }


   /**
    * Retourne la description de l'extension courante selon la syntaxe [e]
    * ou "" si le fichier n'a pas été ouvert en mode MEF
    */
   public String getMefSuffix() {
      if( ext==0 ) return "";
      return "["+ext+"]";
   }

   /** Retourne true si le fichier a été ouvert en mode mosaic */
   public boolean hasCell() {
      return widthCell != -1 && heightCell != -1 && depthCell!=-1
            && (widthCell != width || heightCell != height || depthCell!=depth );
   }

   /**
    * Retourne true s'il y a un pixel connu à la position x,y. Cela concerne le
    * mode mosaic FITS, où l'on peut ouvrir juste une cellule sur le fichier
    * Dans le cas général, cette méthode retournera true dès que le pixel
    * désigné est dans l'image
    */
   //   public boolean isInCell(int x, int y) { return isInCell(x,y,0); }
   public boolean isInCell(int x, int y,int z) {
      return x >= xCell && x < xCell + widthCell
            && y >= yCell && y < yCell + heightCell
            && z >= zCell && z < zCell + depthCell;
   }


   private RandomAccessFile fDirectAccess=null;

   /**
    * Retourne la valeur du pixel en (x,y,z) (y compté à partir du bas) sous forme d'un double
    * en effectuant un accès direct au disque au bon endroit
    * @param x,y,z position du pixel
    */
   public double getPixelDirectAccess(int x, int y) throws Exception { return getPixelDirectAccess(x,y,0); }
   public double getPixelDirectAccess(int x, int y, int z) throws Exception {
      if( filename == null || bitmapOffset == -1 ) throw new Exception(
            "FITS stream not compatible (not a true file [" + filename + "])");

      if( fDirectAccess==null ) fDirectAccess = new RandomAccessFile(filename, "r");
      int n = Math.abs(bitpix) / 8;
      byte [] pixels = new byte[n];

      long offset = bitmapOffset + ((long)z*width*height + (long)y * width  + x) * n;
      fDirectAccess.seek(offset);
      fDirectAccess.readFully(pixels);

      return getPixValDouble(pixels, bitpix, 0);
   }

   /**
    * Retourne la valeur physique (bscale*pix+bzero) du pixel en (x,y) (y compté
    * à partir du bas) sous forme d'un double
    */
   public double getPixelFull(int x, int y) {
      double pix = getPixValDouble(pixels, bitpix, (y - yCell) * widthCell
            + (x - xCell));
      if( isBlankPixel(pix) ) return pix;
      return bscale * pix  + bzero;
   }

   public double getPixelFull(int x, int y, int z) {
      double pix = getPixValDouble(pixels, bitpix, (z-zCell)*widthCell*heightCell + (y - yCell) * widthCell
            + (x - xCell));
      if( isBlankPixel(pix) ) return pix;
      return bscale * pix  + bzero;
   }

   /**
    * Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme
    * d'un double
    */
   public double getPixelDouble(int x, int y) {
      return getPixValDouble(pixels, bitpix, (y - yCell) * widthCell + (x - xCell));
   }

   public double getPixelDouble(int x, int y, int z) {
      return getPixValDouble(pixels, bitpix, (z-zCell)*widthCell*heightCell + (y - yCell) * widthCell + (x - xCell));
   }

   /**
    * Retourne la valeur du pixel en (x,y) (y compté à partir du bas) sous forme
    * d'un entier
    */
   public int getPixelInt(int x, int y) {
      return getPixValInt(pixels, bitpix, (y - yCell) * widthCell + (x - xCell));
   }

   public int getPixelInt(int x, int y,int z) {
      return getPixValInt(pixels, bitpix, (z-zCell)*widthCell*heightCell + (y - yCell) * widthCell + (x - xCell));
   }

   /**
    * Positionne la valeur du pixel RGB en (x,y) (y compté à partir du bas)
    * exprimé en ARGB
    */
   public void setPixelRGB(int x, int y, int val) {
      rgb[(y - yCell) * widthCell + (x - xCell)] = val;
   }

   public void setPixelRGBJPG(int x, int y, int val) {
      if( Fits.JPEGFROMTOP ) rgb[((height - y - 1) - yCell) * widthCell + (x - xCell)] = val;
      else rgb[(y - yCell) * widthCell + (x - xCell)] = val;
   }

   /**
    * Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé
    * en double
    */
   public void setPixelDouble(int x, int y, double val) {
      setPixValDouble(pixels, bitpix, (y - yCell) * widthCell + (x - xCell), val);
   }

   public void setPixelDouble(int x, int y, int z, double val) {
      setPixValDouble(pixels, bitpix, z*widthCell*heightCell + (y - yCell) * widthCell + (x - xCell), val);
   }

   /**
    * Positionne la valeur du pixel en (x,y) (y compté à partir du bas) exprimé
    * en entier
    */
   public void setPixelInt(int x, int y, int val) {
      setPixValInt(pixels, bitpix, (y - yCell) * widthCell + (x - xCell), val);
   }

   public void setPixelInt(int x, int y, int z, int val) {
      setPixValInt(pixels, bitpix, (z-zCell)*widthCell*heightCell + (y - yCell) * widthCell + (x - xCell), val);
   }

   // /** Positionne la valeur du pixel 8 bits en (x,y) (y compté à partir du
   // bas)
   // * exprimé en byte => destiné à être sauvegardé en JPEG */
   // private void setPix8(int x,int y, int val) {
   // setPixValInt(pix8,8, ((height-y-1)-yCell)*widthCell+x, val);
   // // setPixValInt(pix8,8, y*width+x, val);
   // }

   /**
    * Retourne les coordonnées célestes en J2000 de (x,y) (y compté à partir du
    * bas) Le buffer "c" peut être fourni pour éviter des allocations inutiles,
    * null sinon
    */
   protected double[] getRaDec(double[] c, double x, double y) throws Exception {
      if( c == null ) c = new double[2];
      cTmp.x = x;
      cTmp.y = y;
      calib.GetCoord(cTmp);
      c[0] = cTmp.al;
      c[1] = cTmp.del;
      return c;
   }

   /**
    * Retourne les coordonnées image (x,y) (y compté à partir du bas) en
    * fonction de coordonnées célestes en J2000 Le buffer "c" peut être fourni
    * pour éviter des allocations inutiles, null sinon
    */
   protected double[] getXY(double[] c, double ra, double dec) throws Exception {
      if( c == null ) c = new double[2];
      cTmp.al = ra;
      cTmp.del = dec;
      calib.GetXY(cTmp);
      c[0] = cTmp.x;
      c[1] = cTmp.y;
      return c;
   }

   // /** Remplit le tableau des pixels 8 bits (pix8) en fonction de
   // l'intervalle
   // * des valeurs des pixels originaux [min..max] linéairement */
   // public byte [] toPix8(double min, double max,int pixMode) {
   // int range = 256;
   // int gap = 0;
   // if( isTransparent(pixMode) ) { range=255; gap=1; }
   //
   // byte [] pix8 = new byte[widthCell*heightCell];
   // double r = range/(max - min);
   // range--;
   //
   // byte pixOut;
   // for( int y=0; y<heightCell; y++) {
   // for( int x=0; x<widthCell; x++ ) {
   // double pixIn = getPixelDouble(x+xCell,y+yCell);
   // if( isBlankPixel(pixIn) ) pixOut=0;
   // else pixOut = (byte)( gap+ ( pixIn<=min ?0x00:pixIn>=max ? range
   // : (int)( ((pixIn-min)*r) )) & 0xff);
   // // setPix8(x+xCell,y+yCell,pixOut);
   // setPixValInt(pix8,8, (height-y-1)*widthCell+(x+xCell), pixOut);
   //
   // }
   // }
   //
   // return pix8;
   // }

   // VOIR REMARQUE DANS toPix8(double,double,byte[]);
   // /** Remplit le tableau des pixels 8 bits (pix8) en fonction de
   // l'intervalle
   // * des valeurs des pixels originaux [min..max] et de la table des couleurs
   // cm.
   // * S'il y a usage d'une fonction de transfert, elle est déjà appliquée à la
   // table cm (rien à faire de plus)
   // * Si la table cm n'est pas monochrome, l'algo travaille uniquement sur la
   // composante bleue */
   // public void toPix8(double min, double max, ColorModel cm) {
   // double r = 256./(max - min);
   //
   // for( int y=0; y<heightCell; y++) {
   // for( int x=0; x<widthCell; x++ ) {
   // double pixIn = getPixelDouble(x+xCell,y+yCell);
   // int pix = ( pixIn<=min || isBlankPixel(pixIn) ?0x00:pixIn>=max ?
   // 0xff : (int)( ((pixIn-min)*r) ) & 0xff);
   // byte pixOut =(byte) cm.getBlue(pix);
   // setPix8(x+xCell,y+yCell,pixOut);
   // }
   // }
   // }

   /**
    * Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] et de la table des couleurs
    * tcm. S'il y a usage d'une fonction de transfert, elle est déjà appliquée à
    * la table tcm (rien à faire de plus) Il est préférable de travailler sur
    * byte[] tcm plutot que sur ColorModel directement, c'est trop lent
    */
   public byte[] toPix8(double min, double max, byte[] tcm, int pixMode) {
      int range = 256;
      int gap = 0;
      if( isTransparent(pixMode) ) {
         range = 254;
         gap = 2;
      }

      byte[] pix8 = new byte[widthCell * heightCell];
      double r = range / (max - min);
      range--;
      byte pixOut;

      for( int y = 0; y < heightCell; y++ ) {
         for( int x = 0; x < widthCell; x++ ) {
            double pixIn = getPixelDouble(x + xCell, y + yCell);
            if( isBlankPixel(pixIn) ) pixOut = 0;
            else {
               int pix = ((gap + (pixIn <= min ? 0x00 : pixIn >= max ? range
                     : (int) (((pixIn - min) * r)))) & 0xff);
               pixOut = tcm == null ? (byte) pix : tcm[pix];
            }
            // setPix8(x+xCell,y+yCell,pixOut);
            setPixValInt(pix8, 8, (height - y - 1) * widthCell + (x + xCell),
                  pixOut);

         }
      }
      return pix8;
   }

   public byte[] toPix8Int(double min, double max, int pixMode) {
      int range = 256;
      int gap = 0;
      if( isTransparent(pixMode) ) {
         gap = 2;
         range -= gap;
      }

      byte[] pix8 = new byte[widthCell * heightCell * 4];
      double r = range / (max - min);
      range--;
      int pixOut;

      for( int y = 0; y < heightCell; y++ ) {
         for( int x = 0; x < widthCell; x++ ) {
            double pixIn = getPixelDouble(x + xCell, y + yCell);
            if( isBlankPixel(pixIn) ) pixOut = 0;
            else {
               pixOut = ((gap + (pixIn <= min ? 0x00 : pixIn>Integer.MAX_VALUE ? Integer.MAX_VALUE
                     : (int) (((pixIn - min) * r)))) );
            }
            setPixValInt(pix8, 32, (height - y - 1) * widthCell + (x + xCell), pixOut);

         }
      }
      return pix8;
   }

   /**
    * Remplit le tableau des pixels 8 bits (pix8) en fonction de l'intervalle
    * des valeurs des pixels originaux [min..max] et de la table des couleurs
    * tcm. S'il y a usage d'une fonction de transfert, elle est déjà appliquée à
    * la table tcm (rien à faire de plus) Il est préférable de travailler sur
    * byte[] tcm plutot que sur ColorModel directement, c'est trop lent
    */
   public byte[] toPix4(double min, double max, byte[] tcm) {
      int range = 15;
      int gap = 1;

      byte[] pix4 = new byte[widthCell * heightCell];
      double r = range / (max - min);
      range--;
      byte pixOut;

      for( int y = 0; y < heightCell; y++ ) {
         for( int x = 0; x < widthCell; x++ ) {
            double pixIn = getPixelDouble(x + xCell, y + yCell);
            if( isBlankPixel(pixIn) ) pixOut = 0;
            else {
               int pix = (gap
                     + (pixIn <= min ? 0x00 : pixIn >= max ? range
                           : (int) (((pixIn - min) * r))) & 0xff);
               pixOut = tcm == null ? (byte) pix : tcm[pix];
            }
            // setPix8(x+xCell,y+yCell,pixOut);
            setPixValInt(pix4, 8, (height - y - 1) * widthCell + (x + xCell),
                  pixOut);

         }
      }
      return pix4;
   }

   // /** Remplit le tableau des pixels 8 bits (pix8) à partir des pixels
   // * fullbits (uniquement si bitpix==8) avec inversion des lignes */
   // public void initPix8() throws Exception {
   // if( bitpix!=8 ) throw new Exception("bitpix!=8");
   // if( pix8==null ) pix8 = new byte[widthCell*heightCell];
   // initPix(pixels,pix8);
   // }
   //
   // /** Remplit le tableau des pixels Fits (pixels) à partir des pixels
   // * 8bits (uniquement si bitpix==8) avec inversion des lignes */
   // public void initPixFits() throws Exception {
   // if( bitpix!=8 ) throw new Exception("bitpix!=8");
   // initPix(pix8,pixels);
   // }
   //
   // private void initPix(byte src[],byte dst[]) {
   // for( int h=0; h<heightCell; h++ ){
   // System.arraycopy(src,h*widthCell, dst,(heightCell-h-1)*widthCell,
   // widthCell);
   // }
   // }

   //   /**
   //    * Détermine l'intervalle pour un autocut "à la Aladin".
   //    * @return range[0]..[1] => minPixCut..maxPixCut range[2]..[3] =>
   //    *         minPix..maxPix
   //    */
   //   public double[] findFullAutocutRange() throws Exception {
   //      double[] cut = findAutocutRange(0, 0,false);
   //      cut[0] = bscale * cut[0] + bzero;
   //      cut[1] = bscale * cut[1] + bzero;
   //      return cut;
   //   }


   /** Détermine les paramètres d'une ellipse ou d'un rectangle qui contient les pixels observés, afin d'éliminer le bord.
    * Par sur l'hypothèse que l'observation est connexe, mais pas nécessairement centrée.
    * @return [x,y,rayonX,rayonY] dans le sens FITS
    * @throws Exception
    */
   public double[] findData() throws Exception {
      double [] shape = new double[4];
      try {
         if( isReleased() ) reloadBitmap();
         double c;
         double pixBord = getPixValDouble(pixels, bitpix,0);   // Pixel du coin qui sera la référence de la couleur du bord
         int haut,bas,droite,gauche;
         haut=bas=droite=gauche=-1;

         haut: for( int y = 0; y< height; y++ ) {
            for( int x = 0; x < width; x++ ) {
               c = getPixValDouble(pixels, bitpix, y*width + x);
               if( c!=pixBord ) { haut=y; break haut; }
            }
         }
         bas: for( int y = height-1; y>=0; y-- ) {
            for( int x = 0; x < width; x++ ) {
               c = getPixValDouble(pixels, bitpix, y*width + x);
               if( c!=pixBord ) { bas=y; break bas; }
            }
         }
         gauche:  for( int x = 0; x < width; x++ ) {
            for( int y = 0; y< height; y++ ) {
               c = getPixValDouble(pixels, bitpix, y*width + x);
               if( c!=pixBord ) { gauche=x; break gauche; }
            }
         }
         droite:  for( int x = height-1; x >=0; x-- ) {
            for( int y = 0; y< height; y++ ) {
               c = getPixValDouble(pixels, bitpix, y*width + x);
               if( c!=pixBord ) { droite=x; break droite; }
            }
         }


         shape[0] = (droite+gauche)/2;
         shape[1] = (haut+bas)/2;
         shape[2] = (droite-gauche)/2;
         shape[3] = (bas-haut)/2;

         if( droite==-1 || gauche==-1 ) { shape[0]=width/2;  shape[2]=width/2; }
         if( haut==-1   || bas==-1 )    { shape[1]=height/2; shape[3]=height/2; }

      } catch( Exception e ) {
         shape=null;
      }
      return shape;
   }
   
   
   static final private double POURCENT_MIN = 0.003;
   static final private double POURCENT_MAX = 0.9995;
   

   /**
    * Détermine l'intervalle pour un autocut "à la Aladin".
    * @return range[0]..[1] => minPixCut..maxPixCut range[2]..[3] =>
    *         minPix..maxPix
    */
   public double[] findAutocutRange() throws Exception {
      return findAutocutRange(0, 0, -1, -1, false);
   }

   /**
    * Détermine l'intervalle pour un autocut "à la Aladin".
    * @param pourcentMin % de l'info ignoré en début d'histogramme (ex: 0.003), -1 pour défaut
    * @param pourcentMax % de l'info ignoré en fin d'histogramme (ex: 0.9995), -1 pour défaut
    * @return range[0]..[1] => minPixCut..maxPixCut range[2]..[3] =>
    *         minPix..maxPix
    */
   public double[] findAutocutRange(double pourcentMin, double pourcentMax) throws Exception {
      return findAutocutRange(0, 0, pourcentMin,pourcentMax, false);
   }


   /**
    * Détermine l'intervalle pour un autocut "à la Aladin" en ne considérant que
    * les valeurs comprises entre min et max
    * Rq: pour un cube, on prend la tranche du milieu
    * @param min valeur initiale du cut bas (0 si aucune)
    * @param max valeur initiale du cut haut (0 si aucune)
    * @param pourcentMin % de l'info ignoré en début d'histogramme (ex: 0.003)
    * @param pourcentMax % de l'info ignoré en fin d'histogramme (ex: 0.9995)
    * @param full true si on opère sur toute l'image, sinon juste la partie centrale
    * @return range[0]..[1] => minPixCut..maxPixCut range[2]..[3] =>
    *         minPix..maxPix  range[4]=[0..1]: proportion pixel significatif
    */
   public double[] findAutocutRange(double min, double max, boolean full) throws Exception {
      return findAutocutRange(min,max,-1,-1,full);
   }
   public double[] findAutocutRange(double min, double max,
         double pourcentMin, double pourcentMax, boolean full) throws Exception {
      double[] range = new double[5];
      if( pourcentMin==-1 ) pourcentMin=POURCENT_MIN;
      if( pourcentMax==-1 ) pourcentMax=POURCENT_MAX;
      try {
         if( isReleased() ) reloadBitmap();
         findMinMax(range, pixels, bitpix, widthCell, heightCell, depthCell, min, max, 
               pourcentMin, pourcentMax,true, full,0);
      } catch( Exception e ) {
         range[0] = range[2] = min;
         range[1] = range[3] = max;
         range[4]=0;
      }
      return range;
   }

   /** Retourne true si la valeur du pixel est blank ou NaN */
   public boolean isBlankPixel(double pix) {
      return Double.isNaN(pix) || /* !Double.isNaN(blank) && */pix == blank;
   }

   //   public double[] findMinMax() {
   //      boolean first = true;
   //      long nmin = 0, nmax = 0;
   //      double c;
   //      double max = 0, max1 = 0;
   //      double min = 0, min1 = 0;
   //      int n = pixels.length / (Math.abs(bitpix) / 8);
   //      for( int i = 0; i < n; i++ ) {
   //         c = getPixValDouble(pixels, bitpix, i);
   //         if( isBlankPixel(c) ) continue;
   //
   //         if( first ) {
   //            max = max1 = min = min1 = c;
   //            first = false;
   //         }
   //
   //         if( min > c ) {
   //            min = c;
   //            nmin = 1;
   //         } else if( max < c ) {
   //            max = c;
   //            nmax = 1;
   //         } else {
   //            if( c == min ) nmin++;
   //            if( c == max ) nmax++;
   //         }
   //
   //         if( c < min1 && c > min || min1 == min && c < max1 ) min1 = c;
   //         else if( c > max1 && c < max || max1 == max && c > min1 ) max1 = c;
   //      }
   //      double[] minmax = new double[] { min, max };
   //      return minmax;
   //   }

   private int users = 0;
   public boolean hasUsers() { return users > 0; }
   synchronized public void addUser() { users++; }
   synchronized public void rmUser() { users--; }

   /** Retourne true si le bitmap est releasé */
   private boolean bitmapReleaseDone = false;
   public boolean isReleased() { return bitmapReleaseDone; }

   private boolean releasable = true;
   public void setReleasable(boolean flag) { releasable = flag; }
   public boolean isReleasable() { return releasable; }

   //   // Gestion d'un lock
   //   transient private boolean lock;
   //   static private final Object lockObj= new Object();
   //   private void waitLock() {
   //      while( !getLock() ) sleep(100);
   //   }
   //   private void unlock() { lock=false; }
   //   private boolean getLock() {
   //      synchronized( lockObj ) {
   //         if( lock ) return false;
   //         lock=true;
   //         return true;
   //      }
   //   }
   //   // Mise en pause
   //   private void sleep(int delay) {
   //      try { Thread.currentThread().sleep(delay); }
   //      catch( Exception e) { }
   //   }


   /**
    * Libération de la mémoire utilisé par le bitmap des pixels (on suppose que
    * le fichier pourra être relu ultérieurement) Rq : ne fonctionne que pour
    * les fichiers FITS locaux (RandomAccessFile)
    */
   synchronized public long releaseBitmap() throws Exception {
      if( bitpix == 0 ) return 0; // De fait du JPEG
      if( hasUsers() ) return 0; // Pas possible, qq s'en sert
      if( filename==null ) return 0;
      testBitmapReleaseFeature();
      long size = pixels.length;
      pixels = null;
      bitmapReleaseDone = true;
      //      System.out.println("releaseBitmap() size="+width+"x"+height+"x"+Math.abs(bitpix)/8+" offset="+bitmapOffset+" "+getCellSuffix()+" de "+filename);
      return size;
   }

   /**
    * Rechargmeent de la mémoire utilisée par le bitmap des pixels ==> voir
    * releaseBitmap()
    */
   synchronized public void reloadBitmap() throws Exception {
      if( bitpix == 0 ) return; // De fait du JPEG
      if( pixels != null ) return;
      if( filename==null ) return;
      if( !bitmapReleaseDone ) throw new Exception("no releaseBitmap done before");
      testBitmapReleaseFeature();
      //      System.out.println("reloadBitmap() size="+widthCell+"x"+heightCell+"x"+Math.abs(bitpix)/8+" offset="+bitmapOffset+" "+getCellSuffix()+" de "+filename);
      RandomAccessFile f = null;
      try {
         f = new RandomAccessFile(filename, "r");
         int n = Math.abs(bitpix) / 8;
         pixels = new byte[widthCell * heightCell * depth *n];

         // Lecture d'un coup
         if( !hasCell() ) {
            f.seek(bitmapOffset);
            f.readFully(pixels);
         }

         // Lecture ligne à ligne pour mémoriser uniquement la cellule
         else {
            for( int z=0; z<depth; z++ ) {
               long offset = bitmapOffset + (long)z*width*height + (long)yCell * width * n;
               f.seek(offset);
               byte[] buf = new byte[width * n]; // une ligne complète
               for( int lig = 0; lig < heightCell; lig++ ) {
                  f.readFully(buf);
                  System.arraycopy(buf, xCell * n, pixels, z*widthCell*heightCell + lig * widthCell * n, widthCell * n);
               }
            }
         }
      } finally {
         if( f != null ) f.close();
      }

      bitmapReleaseDone = false;
   }

   private void testBitmapReleaseFeature() throws Exception {
      if( filename == null || bitmapOffset == -1 ) throw new Exception(
            "FITS stream not compatible (not a true file [" + filename + "])");
      if( !releasable ) throw new Exception(
            "FITS not compatible (compressed or reserved by user)");
      // if( xCell!=0 || yCell!=0 || widthCell!=width || heightCell!=height )
      // throw new Exception("Fits subcell not compatible ["+filename+"]");
      if( !(new File(filename)).canRead() ) throw new Exception(
            "FITS does not exist on disk [" + filename + "]");
   }

   public void finalize()  throws Throwable { free(); }

   /** Pour aider le GC */
   public void free() {
      pixels = null;
      rgb = null;
      calib = null;
      headerFits = headerFits0 = null;
      width = height = bitpix = 0;
      widthCell = heightCell = depthCell = xCell = yCell = zCell = ext = depth = 0;
      if( fDirectAccess!=null ) {
         try { fDirectAccess.close(); } catch( Exception e ) {}
         fDirectAccess=null;
      }
   }

   public void freeHeader() {
      headerFits = headerFits0 = null;
   }

   @Override
   public String toString() {
      if( depth>1 ) return "Fits file: " + width + "x" + height + "x" + depth + " bitpix=" + bitpix + " ["
            + xCell + "," + yCell + "," + zCell + " " + widthCell + "x" + heightCell + "x" + depthCell+ "]";
      return "Fits file: " + width + "x" + height + " bitpix=" + bitpix + " ["
      + xCell + "," + yCell  + " " + widthCell + "x" + heightCell + "]";
   }

   // -------------------- C'est privé --------------------------


   static private int FINDMINMAXSIZE  = 1500;
   static private int FINDMINMAXDEPTH = 100;
   /**
    * Détermination du min et max des pixels passés en paramètre
    * @param En sortie : range[0]=minPixCut, range[1]=maxPixCut,
    *           range[2]=minPix, range[3]=maxPix;
    * @param pIn Tableau des pixels à analyser
    * @param bitpix codage FITS des pixels
    * @param width Largeur de l'image
    * @param height hauteur de l'image
    * @param depth indice de la frame (pour un cube, sinon 0)
    * @param minCut Limite min, ou 0 si aucune
    * @param maxCut limite max, ou 0 si aucune
    * @param pourcentMin % de l'info ignoré en début d'histogramme (ex: 0.003)
    * @param pourcentMax % de l'info ignoré en fin d'histogramme (ex: 0.9995)
    * @param autocut true si on doit appliquer l'autocut
    * @param ntest Nombre d'appel en cas de traitement récursif.
    */
   private void findMinMax(double[] range, byte[] pIn, int bitpix, int width,
         int height, int depth, double minCut, double maxCut, 
         double pourcentMin, double pourcentMax, boolean autocut, boolean full, int ntest)
               throws Exception {
      int i, j, k;
      boolean flagCut = (ntest > 0 || minCut != 0. && maxCut != 0.);

      // Recherche du min et du max
      double max = 0, max1 = 0;
      double min = 0, min1 = 0;

      int margeW=0,margeH=0,margeZ=0;


      // Marge pour l'échantillonnage (on recherche min et max que sur les 1000
      // pixels centraux en
      // enlevant éventuellement un peu de bord
      if( !full ) {
         margeW = (int) (width  * 0.05);
         margeH = (int) (height * 0.05);
         margeZ = (int) (depth  * 0.05);

         if( width  - 2 * margeW > FINDMINMAXSIZE  ) margeW = (width - FINDMINMAXSIZE)   / 2;
         if( height - 2 * margeH > FINDMINMAXSIZE  ) margeH = (height - FINDMINMAXSIZE)  / 2;
         if( depth  - 2 * margeZ > FINDMINMAXDEPTH ) margeZ = (depth  - FINDMINMAXDEPTH) / 2;
         if( depth-2*margeZ<1 ) margeZ=0;
      }

      double c;

      if( !autocut && (minCut != 0. || maxCut != 0.) ) {
         range[2] = min = minCut;
         range[3] = max = maxCut;

      } else {
         int nblank=0; // Nombre de valeurs BLANK
         boolean first = true;
         long nmin = 0, nmax = 0;
         for( i = margeH; i < height - margeH; i++ ) {
            for( j = margeW; j < width - margeW; j++ ) {
               for( int z = margeZ; z< depth - margeZ; z++ ) {
                  c = getPixValDouble(pIn, bitpix, z*height*width+ i*width + j);

                  // On ecarte les valeurs sans signification
                  if( isBlankPixel(c) ) { nblank++; continue; }

                  if( flagCut ) {
                     if( c < minCut || c > maxCut ) continue;
                  }

                  if( first ) {
                     max = max1 = min = min1 = c;
                     first = false;
                  }

                  if( min > c ) {
                     min = c;
                     nmin = 1;
                  } else if( max < c ) {
                     max = c;
                     nmax = 1;
                  } else {
                     if( c == min ) nmin++;
                     if( c == max ) nmax++;
                  }

                  // On mémorise les extremums et les "penultièmes" extremums
                  if( c < min1 && c > min || min1 == min && c < max1 ) min1 = c;
                  else if( c > max1 && c < max || max1 == max && c > min1 ) max1 = c;
               }
            }
         }

         // On ne prend pas en compte les 2 extremums (uniquement si l'intervalle
         // est plus grand que 256)
         if( autocut && max - min > 256 ) {
            if( min1 - min > max1 - min1 && min1 != Double.MAX_VALUE
                  && min1 != max ) min = min1;
            if( max - max1 > max1 - min1 && max1 != Double.MIN_VALUE
                  && max1 != min ) max = max1;
         }
         range[2] = min;
         range[3] = max;

         // Proportion de pixel significatif [0..1]
         try {
            range[4] = (double)nblank / ((height-2*margeH) * (width-2*margeW));
         } catch( Exception e ) { e.printStackTrace(); }
      }

      if( autocut ) {
         int nbean = 10000;
         double l = (max - min) / nbean;
         int[] bean = new int[nbean];
         for( i = margeH; i < height - margeH; i++ ) {
            for( k = margeW; k < width - margeW; k++ ) {
               c = getPixValDouble(pIn, bitpix, i * width + k);
               if( isBlankPixel(c) ) continue;

               j = (int) ((c - min) / l);
               if( j == bean.length ) j--;
               if( j >= bean.length || j < 0 ) continue;
               bean[j]++;
            }
         }

         // Selection du min et du max en fonction du volume de l'information
         // que l'on souhaite garder
         int[] mmBean = getMinMaxBean(bean,pourcentMin,pourcentMax);

         // Verification que tout s'est bien passe
         if( mmBean[0] == -1 || mmBean[1] == -1 ) {
            throw new Exception("beaning error");
         } else {
            min1 = min;
            max1 = mmBean[1] * l + min1;
            min1 += mmBean[0] * l;
         }

         if( mmBean[0] != -1 && mmBean[0] > mmBean[1] - 5 && ntest < 3 ) {
            if( min1 > min ) min = min1;
            if( max1 < max ) max = max1;
            findMinMax(range, pIn, bitpix, width, height, depth, 
                  min, max, pourcentMin, pourcentMax, autocut, full,
                  ntest + 1);
            return;
         }

         min = min1;
         max = max1;
      }

      // Memorisation des parametres de l'autocut
      range[0] = min;
      range[1] = max;
   }

   /**
    * Determination pour un tableau de bean[] de l'indice du bean min et du bean
    * max en fonction d'un pourcentage d'information desire
    * @param bean les valeurs des beans provenant de l'analyse d'une image
    * @param pourcentMin % de l'info ignoré en début d'histogramme (ex: 0.003)
    * @param pourcentMax % de l'info ignoré en fin d'histogramme (ex: 0.9995);
    * @return mmBean[2] qui contient les indices du bean min et du bean max
    */
   private int[] getMinMaxBean(int[] bean, double pourcentMin,double pourcentMax) {
      int totInfo; // Volume de l'information
      int curInfo; // Volume courant en cours d'analyse
      int[] mmBean = new int[2]; // indice du bean min et du bean max
      int i;

      // Determination du volume de l'information
      for( totInfo = i = 0; i < bean.length; i++ ) {
         totInfo += bean[i];
      }

      // Positionnement des indices des beans min et max respectivement
      // dans mmBean[0] et mmBean[1]
      for( mmBean[0] = mmBean[1] = -1, curInfo = i = 0; i < bean.length; i++ ) {
         curInfo += bean[i];
         double p = (double) curInfo / totInfo;
         if( mmBean[0] == -1 ) {
            if( p > pourcentMin ) mmBean[0] = i;
         } else if( p > pourcentMax ) {
            mmBean[1] = i;
            break;
         }
      }

      return mmBean;
   }

   private Coord cTmp = new Coord();

   public String filename;

   private void setPixValInt(byte[] t, int bitpix, int i, int val) {
      int c;
      switch( bitpix ) {
         case 8:
            t[i] = (byte) (0xFF & val);
            break;
         case 16:
            i *= 2;
            c = val;
            t[i] = (byte) (0xFF & (c >> 8));
            t[i + 1] = (byte) (0xFF & c);
            break;
         case 32:
            i *= 4;
            setInt(t, i, val);
            break;
         case -32:
            i *= 4;
            c = Float.floatToIntBits(val);
            setInt(t, i, c);
            break;
         case -64:
            i *= 8;
            long c1 = Double.doubleToLongBits(val);
            c = (int) (0xFFFFFFFFL & (c1 >>> 32));
            setInt(t, i, c);
            c = (int) (0xFFFFFFFFL & c1);
            setInt(t, i + 4, c);
            break;
      }
   }

   static public void setPixValDouble(byte[] t, int bitpix, int i, double val) {
      int c;
      switch( bitpix ) {
         case -32:
            setInt(t, i << 2, Float.floatToIntBits((float) val));
            break;
         case 16:
            i *= 2;
            c = (int) val;
            t[i] = (byte) (0xFF & (c >> 8));
            t[i + 1] = (byte) (0xFF & c);
            break;
         case 32:
            i *= 4;
            setInt(t, i, (int) val);
            break;
         case 8:
            t[i] = (byte) (0xFF & (int) val);
            break;
         case -64:
            i *= 8;
            long c1 = Double.doubleToLongBits(val);
            c = (int) (0xFFFFFFFFL & (c1 >>> 32));
            setInt(t, i, c);
            c = (int) (0xFFFFFFFFL & c1);
            setInt(t, i + 4, c);
            break;
      }
   }

   public  int getPixValInt(byte[] t, int bitpix, int i) {
      try {
         switch( bitpix ) {
            case 8:
               return (t[i]) & 0xFF;
            case 16:
               i *= 2;
               return ((t[i]) << 8) | (t[i + 1]) & 0xFF;
            case 32:
               return getInt(t, i * 4);
            case -32:
               return (int) Float.intBitsToFloat(getInt(t, i * 4));
            case -64:
               i *= 8;
               long a = (((long) getInt(t, i)) << 32)
                     | ((getInt(t, i + 4)) & 0xFFFFFFFFL);
               return (int) Double.longBitsToDouble(a);
         }
         return 0;
      } catch( Exception e ) {
         return 0;
      }
   }

   public double getPixValDouble(byte[] t, int bitpix, int i) {
      try {
         switch( bitpix ) {
            case -32:
               return Float.intBitsToFloat(getInt(t, i << 2));
            case 16:
               i *= 2;
               return (((t[i]) << 8) | (t[i + 1]) & 0xFF);
            case 32:
               return getInt(t, i * 4);
            case 8:
               return ((t[i]) & 0xFF);
            case -64:
               i *= 8;
               long a = (((long) getInt(t, i)) << 32)
                     | ((getInt(t, i + 4)) & 0xFFFFFFFFL);
               return Double.longBitsToDouble(a);
         }
         return 0.;
      } catch( Exception e ) {
         return DEFAULT_BLANK;
      }
   }

   static private void setInt(byte[] t, int i, int val) {
      t[i] = (byte) (0xFF & (val >>> 24));
      t[i + 1] = (byte) (0xFF & (val >>> 16));
      t[i + 2] = (byte) (0xFF & (val >>> 8));
      t[i + 3] = (byte) (0xFF & val);
   }

   private int getInt(byte[] t, int i) {
      return ((t[i]) << 24) | (((t[i + 1]) & 0xFF) << 16)
            | (((t[i + 2]) & 0xFF) << 8) | (t[i + 3]) & 0xFF;
   }

   //   /** recopie les pixels (pixels[] et rgb[] avec éventuellement
   //    * un changement de bitpix*/
   //   public void overwriteWith(Fits a) throws Exception {
   //      if( a.pixels != null && pixels != null ) {
   //         if( bitpix==a.bitpix ) {
   //            if( a.pixels.length!=pixels.length ) throw new Exception("Not compatible Fits pixels[]");
   //            System.arraycopy(a.pixels, 0, pixels, 0, pixels.length);
   //         } else {
   //            int taille = widthCell * heightCell;
   //            for( int i = 0; i < taille; i++ ) {
   //               double v = a.getPixValDouble(a.pixels, a.bitpix, i);
   //               if( a.isBlankPixel(v) ) v=blank;
   //               setPixValDouble(pixels, bitpix, i, v );
   //            }
   //         }
   //      }
   //      if( a.rgb != null && rgb != null ) {
   //         if( a.rgb.length!=rgb.length ) throw new Exception("Not compatible Fits rgb[]");
   //         System.arraycopy(a.rgb, 0, rgb, 0, rgb.length);
   //      }
   //   }


   /** Retourne la plus grande valeur codable en double en fonction du bitpix */
   static public double getMax(int bitpixOrig) {
      return bitpixOrig==-64?Double.MAX_VALUE : bitpixOrig==-32? Float.MAX_VALUE
            : bitpixOrig==64?Long.MAX_VALUE : bitpixOrig==32?Integer.MAX_VALUE : bitpixOrig==16?Short.MAX_VALUE:255;

   }

   /** Coadditionne les pixels (pixels[] et rgb[], en faisant la moyenne ou par simple addition en bloquant sur la valeur max */
   public void coadd(Fits a,boolean average) throws Exception {
      int taille = widthCell * heightCell * depthCell;

      if( a.pixels != null && pixels != null ) {
         double max = getMax(bitpix);
         for( int i = 0; i < taille; i++ ) {
            double v1 = getPixValDouble(pixels, bitpix, i);
            double v2 = a.getPixValDouble(a.pixels, a.bitpix, i);
            double v;
            if( average ) v = isBlankPixel(v1) ? v2 : a.isBlankPixel(v2) ? v1 : (v1 + v2) / 2;
            else v = isBlankPixel(v1) ? v2 : a.isBlankPixel(v2) ? v1 : ((v1/2) + (v2/2) > max/2) ? max :  (v1 + v2);
            setPixValDouble(pixels, bitpix, i, v);
         }
      }
      if( a.rgb != null && rgb != null ) {
         for( int i = 0; i < taille; i++ ) {
            if( (a.rgb[i] & 0xFF000000)==0 ) continue;
            if( (rgb[i] & 0xFF000000)==0 ) { rgb[i]=a.rgb[i]; continue; }
            int r,g,b;
            if( average ) {
               r = (((rgb[i] >> 16) & 0xFF)  + ((a.rgb[i] >> 16) & 0xFF))/2 << 16;
               g = (((rgb[i] >> 8) & 0xFF) + ((a.rgb[i] >> 8) & 0xFF))/2 << 8;
               b = ((rgb[i] & 0xFF) + (a.rgb[i] & 0xFF))/2;
            } else {
               r = ((rgb[i] >> 16) & 0xFF)  + ((a.rgb[i] >> 16) & 0xFF);
               g = ((rgb[i] >> 8) & 0xFF) + ((a.rgb[i] >> 8) & 0xFF);
               b = ((rgb[i] & 0xFF) + (a.rgb[i] & 0xFF));
               if( r>255 ) r=255;
               if( g>255 ) g=255;
               if( b>255 ) b=255;
               r = r << 16;
               g = g << 8;
            }
            rgb[i] = 0xFF000000 | r | g | b;
         }
      }
   }

   /** Coadditionne les pixels (pix8[], pixels[] et rgb[] */
   public void coadd(Fits a, double[] weightOut, double[] weightIn)
         throws Exception {
      int taille = widthCell * heightCell * depthCell;

      if( a.pixels != null && pixels != null ) {
         for( int i = 0; i < taille; i++ ) {
            double v1 = getPixValDouble(pixels, bitpix, i);
            double v2 = a.getPixValDouble(a.pixels, a.bitpix, i);
            double fct1 = weightOut[i] / (weightOut[i] + weightIn[i]);
            double fct2 = weightIn[i] / (weightOut[i] + weightIn[i]);
            weightOut[i] += weightIn[i];
            weightIn[i] = 0;
            double v = isBlankPixel(v1) ? v2 : a.isBlankPixel(v2) ? v1 :
               v1 * fct1 + v2 * fct2;
            setPixValDouble(pixels, bitpix, i, v);
         }
      }
      if( a.rgb != null && rgb != null ) {
         for( int i = 0; i < taille; i++ ) {
            double fct1 = weightOut[i] / (weightOut[i] + weightIn[i]);
            double fct2 = weightIn[i] / (weightOut[i] + weightIn[i]);
            weightOut[i] += weightIn[i];
            weightIn[i] = 0;
            if( (a.rgb[i] & 0xFF000000)==0 ) continue;
            if( (rgb[i] & 0xFF000000)==0 ) { rgb[i]=a.rgb[i]; continue; }

            int r = (int) (((rgb[i] >> 16) & 0xFF) * fct1 + ((a.rgb[i] >> 16) & 0xFF)
                  * fct2) << 16;
            int g = (int) (((rgb[i] >> 8) & 0xFF) * fct1 + ((a.rgb[i] >> 8) & 0xFF)
                  * fct2) << 8;
            int b = (int) ((rgb[i] & 0xFF) * fct1 + (a.rgb[i] & 0xFF) * fct2);
            rgb[i] = 0xFF000000 | r | g | b;
         }
      }
   }

   /**
    * Remplace les pixels (pixels[] et rgb[] pour les nouvelles valeurs
    * != NaN
    */
   //   public void overwriteWith(Fits a) throws Exception {
   //      int taille = widthCell * heightCell;
   //
   //      if( a.pixels != null && pixels != null ) {
   //         for( int i = 0; i < taille; i++ ) {
   //            double v = a.getPixValDouble(pixels, bitpix, i);
   //            if( a.isBlankPixel(v) ) v = -100; // continue;
   //            setPixValDouble(pixels, bitpix, i, v);
   //            if( a.rgb != null && rgb != null ) rgb[i] = a.rgb[i];
   //         }
   //      }
   //   }

   /** Ajoute les pixels (pixels[] et rgb[] sur les valeurs NaN, idem pour les poids corresondants */
   public void mergeOnNaN(Fits a, double[] weightOut, double[] weightIn) throws Exception {
      int taille = widthCell * heightCell;

      if( a.pixels != null && pixels != null ) {
         for( int i = 0; i < taille; i++ ) {
            double v = getPixValDouble(pixels, bitpix, i);
            boolean vblank = isBlankPixel(v);
            double va = a.getPixValDouble(a.pixels, a.bitpix, i);
            boolean vablank = a.isBlankPixel(va);

            if( !vblank || vablank ) continue;
            setPixValDouble(pixels, bitpix, i, va);
            weightOut[i] = weightIn[i];
         }
      }

      if( a.rgb != null && rgb != null ) {
         for( int i = 0; i < taille; i++ ) {
            if( (rgb[i] & 0xFF000000)!=0 ) continue;
            if( (a.rgb[i] & 0xFF000000)==0 ) continue;
            rgb[i] = a.rgb[i];
            weightOut[i] = weightIn[i];
         }
      }

   }

   /** Ajoute les pixels (pixels[] et rgb[] sur les valeurs NaN */
   public void mergeOnNaN(Fits a) throws Exception {
      int taille = widthCell * heightCell;

      if( a.pixels != null && pixels != null ) {
         for( int i = 0; i < taille; i++ ) {
            double v = getPixValDouble(pixels, bitpix, i);
            boolean vblank = isBlankPixel(v);
            double va = a.getPixValDouble(a.pixels, a.bitpix, i);
            boolean vablank = a.isBlankPixel(va);

            if( !vblank || vablank ) continue;
            setPixValDouble(pixels, bitpix, i, va);
         }
      }

      if( a.rgb != null && rgb != null ) {
         for( int i = 0; i < taille; i++ ) {
            if( (rgb[i] & 0xFF000000)!=0 ) continue;
            if( (a.rgb[i] & 0xFF000000)==0 ) continue;
            rgb[i] = a.rgb[i];
         }
      }

   }

   public void setFilename(String filename) {
      if( filename == null ) {
         this.filename = null;
         return;
      }
      int p = filename.lastIndexOf('[');
      if( p >= 0 ) filename = filename.substring(0, p);
      this.filename = filename;
   }

   public String getFileNameExtended() {
      return getFilename() + getCellSuffix();
   }

   public String getFilename() {
      return filename;
   }

   public void write(String file, double pixelMin, double pixelMax, byte[] tcm,
         String format) throws Exception {
      createDir(file);
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(new File(file));
         write(fos, pixelMin, pixelMax, tcm, format);
      } finally { if( fos!=null )  fos.close(); }
      this.setFilename(file);
   }

   public void write(OutputStream os, double pixelMin, double pixelMax,
         byte[] tcm, String format) throws Exception {
      BufferedImage imgTarget;
      byte[] r = new byte[16];
      for( int i = 0; i < r.length; i++ )
         r[i] = (byte) (i * 16);
      // IndexColorModel cm = new IndexColorModel(8, 256, r, r, r, 0);
      // byte [] pix8 = toPix8(pixelMin, pixelMax, r, PIX_255);
      IndexColorModel cm = new IndexColorModel(4, 16, r, r, r);
      byte[] pix8 = toPix4(pixelMin, pixelMax, null);
      // for( int i=0; i<pix8.length; i++ ) pix8[i] = (byte)(i%16);
      // for( int y=100; y<200; y++ ) for( int x=300; x<450; x++ ) pix8[
      // y*width+x ] = 0;
      WritableRaster raster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE,
            width, height, 1, 4, null);
      raster.setDataElements(0, 0, width, height, pix8);

      // imgTarget = new
      // BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
      imgTarget = new BufferedImage(width, height,
            BufferedImage.TYPE_BYTE_INDEXED, cm);
      imgTarget.setData(raster);

      ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
      ImageWriteParam iwp = writer.getDefaultWriteParam();
      if( format.equals("jpeg") ) {
         iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
         iwp.setCompressionQuality(0.95f);
      }

      ImageOutputStream out = null;
      try {
         out = ImageIO.createImageOutputStream(os);
         writer.setOutput(out);
         writer.write(null, new IIOImage(imgTarget, null, null), iwp);
         writer.dispose();
      } finally { if( out!=null ) out.close(); }
   }

   //   public static void convert(String filename) throws Exception {
   //      Fits f = new Fits();
   //      f.loadJpeg(filename, true,false);
   //      invImageLine(f.width, f.height, f.rgb);
   //      for( int i = 0; i < f.rgb.length; i++ )
   //         f.rgb[i] |= 0xFF000000;
   //      f.writeRGBcompressed(filename + ".jpg", "jpeg");
   //   }
   //
   //   public static void main(String[] args) {
   //      try {
   //         convert("C:\\Users\\Pierre\\Desktop\\PLANCK\\HFIColor100-217-545\\Norder3\\Allsky.jpg");
   //      } catch( Exception e ) {
   //         e.printStackTrace();
   //      }
   //   }

   public static void main(String[] args) {
      try {

         Fits f = new Fits();
         f.loadFITS("C:\\Users\\Pierre\\Desktop\\Data\\Herschell\\hspire1342239942browse_18217979027709026.fits[2]");
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }



}
