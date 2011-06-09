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

package cds.image;

import java.lang.Math;
import java.awt.image.*;
import java.awt.*;
import java.io.*;

/**
 * BMPWritter. Write images in BMP format
 * Example : Writer.write(img,"image.bmp");
 *
 * @author Pierre Fernique [CDS]
 *         from BMPwritter package by Tom West www.holtsoft.com/java/extras - 1998
 * @version 1.0 : (6 feb 2001) creation
 */
public final class BMPWriter {
   
   /** Writes out image as a BMP files called name. */
   static public void write(Image image, OutputStream o) throws Exception {
      int pixels[]=null;
      int w=0,h=0;
      
      try {
         w = image.getWidth(null);
         h = image.getHeight(null);
         
         // Grab the pixels
         PixelGrabber pg = new PixelGrabber(image,0,0,w,h,true);
         pg.grabPixels();
         pixels = (int[])pg.getPixels();
         /*
          if( pixels==null ) {
          pixels = new int[w*h];
          pg = new PixelGrabber(image,0,0,w,h,pixels,0,w);
          pg.grabPixels();
          }
          */
      } catch( Exception e ) {
         e.printStackTrace();
         return;
      }
      
      /*
       
       // Calculate the size of the picture.
        int [] pixels = new int [h * w];
        PixelGrabber pg = new PixelGrabber(image, 0,0, w, h, pixels, 0,w);
        pg.grabPixels();
        */
      write24BitBMP (pixels, w, h, o);
   }
   
   // Juste pour deboguer cette cochonnerie de NETSCAPE JAVA
   static void printStatus(int status) {
      String s = "";
      if( (status & ImageObserver.ABORT)!=0 )      s = s+" ABORT";
      if( (status & ImageObserver.ALLBITS)!=0 )        s = s+" ALLBITS";
      if( (status & ImageObserver.ERROR)!=0 )       s = s+" ERROR";
      if( (status & ImageObserver.SOMEBITS)!=0 )   s = s+" SOMEBITS";
      if( (status & ImageObserver.FRAMEBITS)!=0 )   s = s+" FRAMEBITS";
      System.out.println("Status: "+s);
   }
   
   /** Writes a 24-bit BMP file from the array of direct colour pixels. */
   static public void write24BitBMP (int [] pixels, int w, int h, OutputStream o)
   throws Exception{
      
      // Calculate the size of the picture.
      int padding = (4 - ((w * 3) % 4)) % 4;
      int imageSize = (w * 3 + padding) * h;
      byte [] picture = new byte [imageSize];
      
      int ptr = 0;
      for (int y = 0 ; y < h ; y++) {
         for (int x = 0 ; x < w ; x++) {
            int pixel = pixels [(h - 1 - y) * w + x];
            picture[ptr++] = (byte) (pixel & 0xff);
            picture[ptr++] = (byte) ((pixel >> 8) & 0xff);
            picture[ptr++] = (byte) ((pixel >> 16) & 0xff);
         }
         
         // Pad each line to a 4-byte boundary.
         for (int cnt = 0 ; cnt < padding ; cnt++) picture[ptr++] = 0;
      }
      
      // Write the file out.
      writeBMPFile(o, imageSize, w, h, 24, picture);
   }
   
   
   /** Writes the BMP data to disk. */
   static protected void writeBMPFile(OutputStream o, int imageSize,
         int w, int h, int bitDepth, byte [] buffer)
   throws Exception{
      
      DataOutputStream out = new DataOutputStream (o);
      
      // Write header.
      writeShort(out,0x4d42);        // bfType
      writeInt(out,imageSize + 54 ); // bfSize
      writeShort(out,0);             // bfReserved1
      writeShort(out,0);             // bfReserved2
      writeInt(out,54);              // bfOffBits
      
      writeInt(out,40);              // biSize
      writeInt(out,w);               // biWidth
      writeInt(out,h);               // biHeight
      writeShort(out,1);             // biPlanes
      writeShort(out,bitDepth);      // biBitCount
      writeInt(out,0);               // biCompression
      writeInt(out,imageSize);       // biSizeImage
      writeInt(out,2835);            // biXPelsPerMeter (72 dpi)
      writeInt(out,2835);            // biYPelsPerMeter (72 dpi)
      writeInt(out,0);               // biClrUsed
      writeInt(out,0);               // biClrImportant
      
      
      // Writing image.
      out.write(buffer, 0, imageSize);
      out.close();
   }
   
   /** Writes out a 4-byte int to DataOutputStream "out" in x86 format. */
   private static void writeInt(DataOutputStream out,int number) throws Exception {
      out.writeByte(number & 0xFF);
      out.writeByte((number >> 8) & 0xFF);
      out.writeByte((number >> 16) & 0xFF);
      out.writeByte((number >> 24) & 0xFF);
   }
   
   
   /** Writes out a 2-byte short to DataOutputStream "out" in x86 format. */
   private static void writeShort (DataOutputStream out,int number) throws Exception {
      out.writeByte(number & 0xFF);
      out.writeByte((number >> 8) & 0xFF);
   }
}
