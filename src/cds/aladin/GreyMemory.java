// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.aladin;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import cds.tools.Util;

/**
 * Implemente un PixelGraber adapte aux besoins d'Aladin Java
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (28 mai 00) ReToilettage du code
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class GreyMemory implements ImageConsumer {
   // Variables de stockage
   private boolean BYTEMODE;		// true si on memorise en byte, false en int
   private byte [] bytePix;		// pixels memorises en byte
   private int [] intPix;		// pixels  memorises en int
//   private byte [] red;		    // pixels composante rouge
//   private byte [] green;		// pixels composante verte
//   private byte [] blue;		// pixels composante bleu
  
   // Variables locales
   private int status;			// Status du producteur de l'image
   private ImageProducer source;	// Source de l'image
   private boolean flagPixels=false;	// true si j'ai recu des pixels
   private boolean ok;			// True si l'image est prete

  /** Creation d'un PixelGrabber sur mesure.
    * Extrait les pixels de la source indiquee et remplit un
    * tableau de pixels en 8 bits ou 32 bits suivant le type du tableau
    *
    * @param source la provenance des pixels
    * @param pixels le tableau a remplir (deja alloue et assez grand)
    */
   protected GreyMemory(ImageProducer source, int[] intPix) {
      super();
      BYTEMODE=false;
      this.setOk(false);
      this.intPix = intPix;
      this.source=source;
      source.startProduction(this);   //Demande la production de l'image
   } 
   
   protected GreyMemory(ImageProducer source, byte[] bytePix) {
      super();
      BYTEMODE=true;
      this.setOk(false);
      this.bytePix = bytePix;
      this.source=source;
      source.startProduction(this);   //Demande la production de l'image
   } 
   
//   protected GreyMemory(ImageProducer source, byte[] r,byte[] g,byte[] b) {
//      super();
//      BYTEMODE=true;
//      this.setOk(false);
//      red=r; green=g; blue=b;
//      this.source=source;
//      source.startProduction(this);   //Demande la production de l'image
//   } 
   
   // Positionne le flag qui indique que l'image est prete
   synchronized private void setOk(boolean ok) { this.ok = ok; }
   
  /** Synchronisation de l'image.
   * Attente jusqu'a ce que l'extraction des pixels ait ete terminee
   * @return <I>true</I> Ok pour l'image, sinon <I>false</I>
   */
   protected boolean waitImage() {
      while( !ok ) Util.pause(400);
      return (status & ImageConsumer.IMAGEABORTED)==0;
   }
   
/*
   // Juste pour deboguer cette cochonnerie de NETSCAPE JAVA
   static void printStatus(String label,int status) {
      String s = "";
      if( (status & ImageConsumer.IMAGEABORTED)!=0 )      s = s+" IMAGEABORTED";
      if( (status & ImageConsumer.IMAGEERROR)!=0 )        s = s+" IMAGEERROR";
      if( (status & ImageConsumer.SINGLEFRAME)!=0 )       s = s+" SINGLEFRAME";
      if( (status & ImageConsumer.SINGLEFRAMEDONE)!=0 )   s = s+" SINGLEFRAMEDONE";
      if( (status & ImageConsumer.STATICIMAGEDONE)!=0 )   s = s+" STATICIMAGEDONE";
      print(label+s);
   }
   
   // Juste pour deboguer cette cochonnerie de NETSCAPE JAVA
   static void printHints(String label,int status) {
      String s = "";
      if( (status & ImageConsumer.SINGLEPASS)!=0 )        s = s+" SINGLEPASS";
      if( (status & ImageConsumer.SINGLEFRAME)!=0 )       s = s+" SINGLEFRAME";
      if( (status & ImageConsumer.COMPLETESCANLINES)!=0 ) s = s+" COMPLETESCANLINES";
      if( (status & ImageConsumer.RANDOMPIXELORDER)!=0 )  s = s+" RANDOMPIXELORDER";
      if( (status & ImageConsumer.TOPDOWNLEFTRIGHT)!=0 )  s = s+" TOPDOWNLEFTRIGHT";
      print(label+s);
   }
*/   

   public void imageComplete(int status) {
      this.status=status;
      if( !flagPixels ) return;   	// Je n'ai recu encore aucun pixel   
      if( (status & ImageConsumer.STATICIMAGEDONE)!=0 ||
          (status & ImageConsumer.IMAGEABORTED)!=0) {
         source.removeConsumer(this);
         setOk(true);
      }
   }
   
   public void setColorModel(ColorModel model) {}
   public void setProperties(Hashtable props) {}
   public void setDimensions(int width,int height) {}
   public void setHints(int status) { status=0;}   
   
   public void setPixels(int x, int y, int w, int h, ColorModel model,
                         byte[] pixels, int off, int scansize) {
                         
      flagPixels=true;	// Ca y est j'ai eu des pixels
      
//System.out.println("setPixels("+x+","+y+","+w+","+h+") off="+off+" scansize="+scansize);
      if( BYTEMODE ) {
         for( int yc=y; yc<y+h; yc++ ) {
            System.arraycopy(pixels,off+(yc-y)*scansize, bytePix,yc*scansize+x, w);
         } 
         
      } else {
         for( int yc=y; yc<y+h; yc++ ) {
            int iTarget=yc*scansize+x;
            int iSource=off+(yc-y)*scansize+x;
            for( int j=0; j<w; j++) {
               intPix[iTarget++]=(int)(pixels[iSource++])&0xFF;
            }
         } 
      }
                        
   }
   
   
  /** Recuperation des pixels couleurs.
   * <BR><B>Rq :</B> Les images en couleurs ne sont pas prises en compte par
   * cette classe => genere imageComplete(ImageConsumer.IMAGEABORTED)
   */
   public void setPixels(int x, int y, int w, int h, ColorModel model,
                         int[] pixels, int off, int scansize) {
      flagPixels=true;	// Ca y est j'ai eu des pixels
      
//System.out.println("setPixels("+x+","+y+","+w+","+h+") off="+off+" scansize="+scansize);

//      for( int yc=y; yc<y+h; yc++ ) {
//         int iTarget=yc*scansize+x;
//         int i=off+(yc-y)*scansize+x;
//         for( int j=0; j<w; j++) {
//            red[iTarget]   = (byte)( (pixels[i]>>16) & 0xFF );
//            green[iTarget] = (byte)( (pixels[i]>> 8) & 0xFF );
//            blue[iTarget]  = (byte)(  pixels[i]      & 0xFF );
//            i++; iTarget++;
//         }
//      } 
   }
}

