// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.aladin;

import java.awt.image.ColorModel;
import java.util.Vector;

/**
 * Gestion d'un plan image Cube couleur
 * @version 1.0 : mars 2011
 */
public final class PlanImageCubeRGB extends PlanImageCube implements PlanRGBInterface {
   
   private int [] pixelsZoomRGB=null;
   
   protected PlanImageCubeRGB(Aladin aladin,String file,MyInputStream in,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,in,label,from,o,imgNode,skip,doClose,forPourcent);
      type=IMAGECUBERGB;
      initDelay=400;
   }
   
   protected void copy(Plan p1) {
      super.copy(p1);
      if( !(p1 instanceof PlanImageRGB) ) return;
      PlanImageRGB p = (PlanImageRGB)p1;
      p.pixelsRGB = getPixelsRGB();
   }
   
   /** Retourne la frame numéro n */
   protected int[] getFrameRGB(int n) {
      return vFrames.elementAt(n).pixelsRGB;
   }
   
   protected boolean cacheImageFits(MyInputStream dis) throws Exception {
      int naxis;
      long taille;      // nombre d'octets a lire
      int n;            // nombre d'octets pour un pixel
      
Aladin.trace(2,"Loading FITS "+Tp[type]);

      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);
 
      bitpix = headerFits.getIntFromHeader("BITPIX");
      if( bitpix!=32 && bitpix!=24) {
         error="BITPIX must be 24 or 32 for RGB or ARGB FITS cube !";
         return false;
      }
      pixMode = bitpix==32 ? PIX_ARGB : PIX_RGB;
      naxis = headerFits.getIntFromHeader("NAXIS");
      
      // Il s'agit juste d'une entête FITS indiquant des EXTENSIONs
      if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
         error="_HEAD_XFITS_";

         // Je saute l'éventuel baratin de la première HDU
         if( naxis==1 ) {
            try {
               naxis1 = headerFits.getIntFromHeader("NAXIS1");
               dis.skip(naxis1);
            } catch( Exception e ) { e.printStackTrace(); }
         }

         return false;
      }
            
      naxis1=width = headerFits.getIntFromHeader("NAXIS1");
      naxis2=height = headerFits.getIntFromHeader("NAXIS2");
      depth = headerFits.getIntFromHeader("NAXIS3");
      
      npix = n = bitpix/8;  // Nombre d'octets par valeur
      taille=(long)width*height*depth*n;      // Nombre d'octets
      setPourcent(0);
Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" NAXIS3="+depth+" BITPIX="+bitpix+" => size="+taille);

      //Les paramètres FITS facultatifs
      loadFitsHeaderParam(headerFits);
      
      // Il s'agit d'une image MEF que l'on ne va pas garder, on se contente de skipper l'image
      if( flagSkip ) {
         dis.skip( taille );
          
      // Lecture effective
      } else {
         double requiredMo =  taille/(1024.*1024); 
         boolean loadInRam = aladin.getMem() - requiredMo > 10;
         Aladin.trace(4,"PlanImageCubeColor.loadImageFits() required "+requiredMo+"MB "+(loadInRam?"":"=> not enough space in RAM"));
         if( !loadInRam ) {
            error = "Not enough RAM space";
            return false;
         }

         byte [] buf = new byte[width*height*n];
         for( int i=0; i<depth; i++ ) {
            dis.readFully(buf);
            int [] pixelsRGB = extractImageRGB(buf,width,height,n,i);
            addFrame(label,pixelsRGB);
            setPourcent((99.*i)/depth);       
         }
         noOriginalPixels();
      }
       
      // On se recale si jamais il y a encore une extension FITS qui suit
      if( naxis>3 ) {
         try {
            long offset=n*width*height;
            for( int i=3; i<naxis; i++ ) offset *= headerFits.getIntFromHeader("NAXIS"+(i+1));
            offset -= n*width*height;
            dis.skip(offset);
         } catch( Exception e ) { e.printStackTrace(); return false; }
      }
      
      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
      if( flagSkip ) return true;
            
      cm = ColorModel.getRGBdefault();
      setPourcent(-1);
      return true;
   }   
   
   /** Extraction de l'image RGB ou ARGB depuis le buffer en byte[] vers le buffer en int[]
    * On en profite pour retourner les lignes de l'image (FITS => Java)
    * @param buf[] le tableau des pixels byte[] ARGB ou RGB
    * @param width largeur de l'image
    * @param height hauteur de l'image
    * @parma npix le nombre d'octets pour chaque pixel (3 => RGB, 4 => ARGB)
    * @return pixelsRGB Tableau des pixels int[] ARGB
    */
    private int[] extractImageRGB(byte [] buf, int width, int height,int npix,int frame) {
       int [] pixelsRGB = new int[width*height];
       
       int j=0;
       for( int y=0; y<height; y++ ) {
          for( int x=0; x<width; x++) {
             int pix=0;
             for( int i=0; i<npix; i++ ) {
                int c = 0xFF & buf[j++];
                pix = (pix<<8) | c;
             }
             pixelsRGB[ (height-y-1)*width + x] = 0xFF000000 | pix;
          }
       }
       return pixelsRGB;
    }

    synchronized protected void addFrame(String label,int pixelsRGB[]) {
       if( vFrames==null ) vFrames = new Vector<PlanImageBlinkItem>();
       vFrames.addElement( new PlanImageBlinkItem(label,pixelsRGB));
    }

   public int[] getPixelsRGB() {
      double frame = aladin.view.getCurrentView().getCurrentFrameLevel();
      return getFrameRGB((int)frame);
   }
   
   /** Retourne les 3 composantes du pixel repéré dans l'image de la frame courante */
   public int getPixel8(int x,int y) {
      int [] pixelsRGB = getPixelsRGB();
      return pixelsRGB[y*width+x];
   }
   
   /** Extraction d'une portion de de la frame spécifié en entier ARGB.
    * Retourne une portion de l'image sur la forme d'un tableau de pixels
    * sens des lignes JPEG
    * @param newpixels Le tableau a remplir (il doit etre assez grand)
    * @param x,y,w,h   Le rectangle de la zone a extraire
    * @param frame frame du cube
    */
    public void getPixels(int [] newpixels,int x,int y,int w,int h,int frame) {
       int i,n;
       int k=0;
       int aw,ah;   // Difference en abs et ord lorsqu'on depasse l'image
       int [] pixelsRGB = getFrameRGB(frame);

       // Ajustement de la taille en cas de depassement
       aw=ah=0;
       if( x+w>width )  { aw = x+w-width;  w-=aw; }
       if( y+h>height ) { ah = y+h-height; h-=ah; }

       for( i=y, n=y+h; i<n; i++ ) {
          System.arraycopy(pixelsRGB,i*width+x, newpixels,k, w);
          k+=w+aw;
       }
    }
    
    /** Extraction d'une portion de de la frame courante en entier ARGB.
     * Retourne une portion de l'image sur la forme d'un tableau de pixels
     * sens des lignes JPEG
     * @param newpixels Le tableau a remplir (il doit etre assez grand)
     * @param x,y,w,h   Le rectangle de la zone a extraire
     */
    public void getPixels(int [] newpixels,int x,int y,int w,int h) {
       double frame = aladin.view.getCurrentView().getCurrentFrameLevel();
       getPixels(newpixels,x,y,w,h,(int)frame);
    }

   public int[] getPixelsZoomRGB() { return pixelsZoomRGB; }

   /**
    * Calcul les pixels de l'imagette pour le ZoomView en prenant le pixel au plus proche
    * C'est très rapide et le rendu visuel est quasi le même que par interpolation
    */
   public void calculPixelsZoomRGB() {
      int [] pixelsRGB = getPixelsRGB();
      pixelsZoomRGB = PlanImageRGB.calculPixelsZoomRGB1(aladin,pixelsZoomRGB,pixelsRGB,width,height);
   }
}
