// Copyright 2011 - UDS/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//
package cds.mocmulti;

import java.io.EOFException;
import java.io.RandomAccessFile;

/**
 * Gestion d'un buffer de bytes. D�di� � la relecture binaire
 */
public final class BufReader {
   static private final int MAX = 512;
   public byte [] buf;   // Le buffer
   protected int offset;    // La position courante dans le buffer
   protected int size;   // Le contenu courant du buffer
   
   private RandomAccessFile in;

   /** Cr�ation d'un buffer d'une taille donn�e */
   public BufReader(RandomAccessFile in) {
      buf = new byte[MAX];
      offset=0;
      size=0;
      this.in=in;
   }

   /** Lecture d'une chaine */
   public String readString() throws Exception {
      int n = readShort();
      if( n==-1 ) return null;
      availability(n);
      String s = new String(buf,offset,n);
      offset+=n;
      return s;
   }

   /** Lecture d'une Boolean */
   public boolean readBoolean() throws Exception {
      availability(1);
      return buf[offset++]!=(byte)0;
   }

   /** Lecture d'un byte */
   public byte readByte() throws Exception {
      availability(1);
      return buf[offset++];
   }
   
   /** Lecture d'un entier */
   public short readShort() throws Exception {
      availability(2);
      return
      (short)((buf[offset++]&0xFF)
      | ( (buf[offset++]&0xFF) <<8 ) );
   }

   /** Lecture d'un entier */
   public int readInteger() throws Exception {
      availability(4);
     return
      (buf[offset++]&0xFF)
      | ( (buf[offset++]&0xFF) <<8 ) 
      | ( (buf[offset++]&0xFF) <<16 ) 
      | ( (buf[offset++]&0xFF) <<24 );
   }


   /** Lecture d'un long */
   public long readLong() throws Exception {
      availability(8);
      return
      (buf[offset++]&0xFFL)
      | ( (buf[offset++]&0xFFL) <<8 ) 
      | ( (buf[offset++]&0xFFL) <<16 ) 
      | ( (buf[offset++]&0xFFL) <<24 )
      | ( (buf[offset++]&0xFFL) <<32 )
      | ( (buf[offset++]&0xFFL) <<40 )
      | ( (buf[offset++]&0xFFL) <<48 )
      | ( (buf[offset++]&0xFFL) <<56 );
   }
   
   
   private void availability(int n) throws Exception {
      if( offset+n<size ) return;

      // Pas assez de place � la fin du buf ? => r�allocation
      if( buf.length<offset+n ) {
         int reste = size-offset;
         byte [] nbuf = new byte[ n>MAX?n:MAX];
         if( reste>0 ) System.arraycopy(buf, offset, nbuf, 0, reste);
         size = reste;
         offset=0;
         buf=nbuf;
      }
      
      // Lecture de ce qui manque, et plus si possible
      int manque = n-(size-offset);
      int place = buf.length-size;
      while( manque>0 ) {
         int k = in.read(buf,size,place);
         if( k==-1 ) throw new EOFException();
         size+=k;
         manque-=k;
         place-=k;
      }
      
   }
}
