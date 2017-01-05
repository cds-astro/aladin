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

import java.io.RandomAccessFile;

/**
 * Gestion d'un buffer de bytes. Dédié à la sauvegarde binaire
 */
public final class BufWriter {
   static private final int MAX = 512;
   public byte [] buf;     // Le buffer
   protected int offset;   // La position courante dans le buffer
   
   private RandomAccessFile in;

   /** Création d'un buffer d'une taille donnée */
   public BufWriter(RandomAccessFile in) {
      buf = new byte[MAX];
      offset=0;
      this.in=in;
   }

   /** Retourne la taille nécessaire à la mémorisation d'une chaine.
    *  2 byte pour la taille, suivi des lettres
    */
   static public int sizeOfString(String s) { return 2+(s==null?0:s.getBytes().length); }

   /** Mémorisation d'une chaine: 2 bytes pour la taille, suivi des lettres */
   public void memoString(String s) throws Exception {
      if( s==null ) { memoShort( (short)-1); return; }
      byte[] a = s.getBytes();
      memoShort( (short)a.length );
      availability(a.length);
      System.arraycopy(a,0,buf,offset,a.length);
      offset+=a.length;
   }

   /** Mémorisation d'un boolean: 0 si false, 1 si true */
   public void memoBoolean(boolean v) throws Exception {
      availability(1);
      buf[offset++]= (byte)(v?1:0); 
   }

   /** Mémorisation d'un byte */
   public void memoByte(byte v) throws Exception {
      availability(1);
      buf[offset++]= v;
   }
   
   /** Mémorisation d'un short (byte de poids faible en premier) */
   public void memoShort(short v) throws Exception {
      availability(2);
      buf[offset++]= (byte)(v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
   }

   /** Mémorisation d'un entier (byte de poids faible en premier) */
   public void memoInteger(int v) throws Exception {
      availability(4);
      buf[offset++]= (byte)(v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
      buf[offset++]= (byte)((v>>16) & 0xFF);
      buf[offset++]= (byte)((v>>24) & 0xFF);
   }

   /** Mémorisation d'un long (byte de poids faible en premier) */
   public void memoLong(long v) throws Exception {
      availability(8);
      buf[offset++]= (byte)(  v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
      buf[offset++]= (byte)((v>>16) & 0xFF);
      buf[offset++]= (byte)((v>>24) & 0xFF);
      buf[offset++]= (byte)((v>>32) & 0xFF);
      buf[offset++]= (byte)((v>>40) & 0xFF);
      buf[offset++]= (byte)((v>>48) & 0xFF);
      buf[offset++]= (byte)((v>>56) & 0xFF);
   }
   
   public void flush() throws Exception {
      in.write(buf,0,offset);
      offset=0;
   }

   private void availability(int n) throws Exception {
      if( buf.length-offset>=n ) return;   // Il reste assez de place 
      flush();
      if( n>buf.length ) buf = new byte[n];  // buffer trop petit ?
   }
   
}
