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


package cds.aladin;


import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;

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
Aladin.trace(2,"Loading colored image isSync()="+isSync());
long t1,t=System.currentTimeMillis();

      // Recuperation de l'image 3x8 bits
      BufferedImage buf;
      
      try {

         // TROISIEME METHODE, DE FAIT C'EST PLUS LENT
         // (SANS DOUTE PARCE QUE UNE GRANDE PARTIE EN PURE JAVA)
         //    buf = ImageIO.read(dis);
         //    naxis1=width=buf.getWidth();
         //    naxis2=height=buf.getHeight();
         //    t1 = System.currentTimeMillis();
         //    Aladin.trace(3,"imageIO read in "+(t1-t)+"ms"); t=t1;    


         // DEUXIEME METHODE EN DEUX ETAPES
         Image itmp = aladin.getToolkit().createImage(dis.readFully());    
         
         aladin.waitImage(itmp);
         naxis1=width=itmp.getWidth(aladin);
         naxis2=height=itmp.getHeight(aladin);

         setPourcent(10);
         t1 = System.currentTimeMillis();
         Aladin.trace(3,"image "+width+"x"+height+" created&loaded in "+(t1-t)+" ms"); t=t1;

         // PREMIERE METHODE TRES LENTE APRES JVM 1.3
         //      setPourcent(45);
         //	  PixelGrabber pg = new PixelGrabber(itmp,0,0,width,height,pixelsRGB,0,width);
         //	  try { pg.grabPixels(); }
         //	  catch (Exception e) { return false; }
         //	  if( (pg.getStatus() & ImageObserver.ABORT) != 0 ) { return false; }
         //

         // A EVITER CAR RISQUE DE CONVERSION DES PIXELS SI MODE PAR DEFAUT NON ARGB
         //      BufferedImage buf = aladin.getGraphicsConfiguration().createCompatibleImage(width, height);
         buf = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
         Graphics g = buf.getGraphics();
         g.drawImage(itmp,0,0,aladin);
         g.finalize(); g=null;
         itmp.flush(); itmp=null; 
         
         pixelsRGB = ((DataBufferInt)buf.getRaster().getDataBuffer()).getData();

//         pixelsRGB = new int[width*height];
//
//         // Pour faire avancer le % de lecture
//         if( height>1000 ) {
//            int pasY = height/89;
//            int startY=0;
//            int i=0;
//            do {
//               setPourcent(10+i);
//               if( startY+pasY>height ) pasY= height-startY;
//               pixelsRGB=buf.getRGB(0,startY,width,pasY,pixelsRGB,startY*width,width);
//               startY+=pasY;
//               i++;
//            } while( startY<height );
//
//            // Si trop petit, lecture d'un coup !
//         } else {
//            pixelsRGB=buf.getRGB(0,0,width,height,pixelsRGB,0,width);
//         }
         
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
      }
      
      return true;
   }
   
}
