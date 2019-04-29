// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;


import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
* Gestion d'un plan image Couleur
*
* @author Pierre Fernique [CDS]
* @version 1.0 : février 2005 creation
*/
public class PlanImageColor extends PlanImageRGB {

   protected PlanImageColor(Aladin aladin, String file,URL u,MyInputStream inImg) {
      super(aladin,file,u,inImg);
   }
   
   protected PlanImageColor(Aladin aladin, String file,URL u,MyInputStream inImg, ResourceNode imgNode) {
      super(aladin,file,u,inImg,imgNode);
   }
   
   protected PlanImageColor(Aladin aladin,
         MyInputStream inImg,
         int orig, URL u,
         String label,String objet,
         String param, String from,
         int fmt,int res,
         Obj o, ResourceNode imgNode) {
      super(aladin,inImg,orig,u,label,objet,param,from,fmt,res,o,imgNode);
      flagRed=flagGreen=flagBlue=true;
  }
   
   protected boolean cacheImageNatif(MyInputStream dis)  {
      
      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      setPourcent(1);
      Aladin.trace(2,"Loading colored image");
      long t1,t=System.currentTimeMillis();

      // Recuperation de l'image 3x8 bits
      BufferedImage buf;
      
      ImageInputStream iis = null;
      try {
         // 4EME METHODE POUR EVITER LE DOUBLEMENT DU BUFFER
            long type = dis.getType();
            String fmt = (type & MyInputStream.JPEG) != 0 ? "jpeg" :
               (type & MyInputStream.PNG) != 0 ? "png" : "gif";
            
            setPixMode( (type & MyInputStream.JPEG) != 0 ? PIX_RGB : PIX_ARGB );
               
            Iterator readers = ImageIO.getImageReadersByFormatName(fmt);
            ImageReader reader = (ImageReader)readers.next();
            iis = ImageIO.createImageInputStream(dis);
            reader.setInput(iis,true);
            naxis1=width = reader.getWidth(0);
            naxis2=height = reader.getHeight(0);

            t1 = System.currentTimeMillis();
            Aladin.trace(3,"image ("+fmt+") "+width+"x"+height+" created&loaded in "+(t1-t)+" ms"); t=t1;

            ImageReadParam param = reader.getDefaultReadParam();
            
            double mem = aladin.getMem();
            double taille=(width*height*4.)/(1024.*1024);
            Aladin.trace(4,"PlanImageColor.cacheImageNatif()... RAM="+mem+"MB imageSize="+taille+"MB");
            if( mem<taille ) throw new Exception("Not enough memory for this image => required "+taille+"MB !");
            
//            if( mem<2*taille ) {
               Aladin.trace(4,"PlanImageColor.cacheImageNatif()... loading huge image piece by piece...");

               buf = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
               Graphics g = buf.getGraphics();

               int size=1024;
               double incrPourcent = 98./((double)width*height/(size*size));
               double pourcent=0;
               BufferedImage itmp;
               for( int y=0; y<height; y+=size) {
                  for( int x=0; x<width; x+=size) {
                     setPourcent(pourcent+=incrPourcent);
                     int w = x+size>width ? width-x : size;
                     int h = y+size>height ? height-y : size; 
                     Rectangle r = new Rectangle(x,y,w,h);
                     param.setSourceRegion(r);
                     itmp = reader.read(0,param);
                     g.drawImage(itmp,x,y,aladin);
                  }
               }

               g.finalize(); g=null;
               pixelsRGB = ((DataBufferInt)buf.getRaster().getDataBuffer()).getData();
               
//            } else {
//               Aladin.trace(4,"PlanImageColor.cacheImageNatif()... loading image in one fast step...");
//               setPourcent(10);
//               buf = reader.read(0,param);
//               pixelsRGB = new int[width*height*4];
//               buf.getRGB(0, 0, width, height, pixelsRGB, 0, width);
//            }
            


////          TROISIEME METHODE, DE FAIT C'EST PLUS LENT
////          (SANS DOUTE PARCE QUE UNE GRANDE PARTIE EN PURE JAVA)
//             buf = ImageIO.read(dis);
//             naxis1=width=buf.getWidth();
//             naxis2=height=buf.getHeight();
//             t1 = System.currentTimeMillis();
//             Aladin.trace(3,"imageIO read in "+(t1-t)+"ms"); t=t1;    
//             
//             byte x [] = ((DataBufferByte)buf.getRaster().getDataBuffer()).getData();
//             
//             System.out.println("naxis1="+naxis1+" naxis2="+naxis2+" size="+(naxis1*naxis2*4)+" x.length="+x.length);
//             
//             
//             System.out.println("J'y suis");
//             
//             
////             pixelsRGB = new int[naxis1*naxis2*4];
////             buf.getRGB(0, 0, naxis1, naxis2, pixelsRGB, 0, naxis1);
//

//            // DEUXIEME METHODE EN DEUX ETAPES
//            Image itmp = aladin.getToolkit().createImage(dis.readFully());    
//            
//            aladin.waitImage(itmp);
//            naxis1=width=itmp.getWidth(aladin);
//            naxis2=height=itmp.getHeight(aladin);
//
//            setPourcent(10);
//            t1 = System.currentTimeMillis();
//            Aladin.trace(3,"image "+width+"x"+height+" created&loaded in "+(t1-t)+" ms"); t=t1;
//
//            // PREMIERE METHODE TRES LENTE APRES JVM 1.3
//            //      setPourcent(45);
//            //	  PixelGrabber pg = new PixelGrabber(itmp,0,0,width,height,pixelsRGB,0,width);
//            //	  try { pg.grabPixels(); }
//            //	  catch (Exception e) { return false; }
//            //	  if( (pg.getStatus() & ImageObserver.ABORT) != 0 ) { return false; }
//            //
//
//            // A EVITER CAR RISQUE DE CONVERSION DES PIXELS SI MODE PAR DEFAUT NON ARGB
//            //      BufferedImage buf = aladin.getGraphicsConfiguration().createCompatibleImage(width, height);
//            buf = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
//            Graphics g = buf.getGraphics();
//            g.drawImage(itmp,0,0,aladin);
//            g.finalize(); g=null;
//            itmp.flush(); itmp=null; 
//            
//            pixelsRGB = ((DataBufferInt)buf.getRaster().getDataBuffer()).getData();
            
         buf.flush();  buf=null;
      
         t1 = System.currentTimeMillis();
         Aladin.trace(3,"RGB pixels extracted in "+(t1-t)+" ms"); t=t1;

         d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
         Aladin.trace(3," => Loading in "+temps+" ms");

         cm = ColorModel.getRGBdefault();
         setPourcent(99);

         video = VIDEO_NORMAL;

      } catch( Exception e ) {
         setPourcent(-1);
         aladin.error=e.getMessage();
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      } finally { if( iis!=null ) try { iis.close(); } catch( Exception e1) {} }
      
      return true;
   }
   
}
