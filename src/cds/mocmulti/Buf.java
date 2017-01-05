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

/**
 * Gestion d'un buffer de bytes. D�di� � la sauvegarde et � la relecture
 * binaire
 *
 * Copyright: 2004, Pierre Fernique
 */
public class Buf {
   public byte [] buf;   // Le buffer
   protected int offset;    // La position courante dans le buffer

   /** Cr�ation d'un buffer d'une taille donn�e */
   public Buf(int size) {
      buf = new byte[size];
      offset=0;
   }

   /** Repositionnement au d�but du pointeur de la position courante */
   public void reset() { offset=0; }

   /** Retourne la taille n�cessaire � la m�morisation d'une chaine.
    *  2 byte pour la taille, suivi des lettres
    */
   static public int sizeOfString(String s) { return 2+(s==null?0:s.getBytes().length); }

   /** M�morisation d'une chaine: 2 bytes pour la taille, suivi des lettres */
   public void memoString(String s) {
      if( s==null ) { memoShort( (short)-1); return; }
      byte[] a = s.getBytes();
      memoShort( (short)a.length );
      System.arraycopy(a,0,buf,offset,a.length);
      offset+=a.length;
   }

   /** M�morisation d'un boolean: 0 si false, 1 si true */
   public void memoBoolean(boolean v) { buf[offset++]= (byte)(v?1:0); }

   /** M�morisation d'un byte */
   public void memoByte(byte v) { buf[offset++]= v; }
   
   /** M�morisation d'un short (byte de poids faible en premier) */
   public void memoShort(short v) {
      buf[offset++]= (byte)(v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
   }

   /** M�morisation d'un entier (byte de poids faible en premier) */
   public void memoInteger(int v) {
      buf[offset++]= (byte)(v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
      buf[offset++]= (byte)((v>>16) & 0xFF);
      buf[offset++]= (byte)((v>>24) & 0xFF);
   }

   /** M�morisation d'un long (byte de poids faible en premier) */
   public void memoLong(long v) {
      buf[offset++]= (byte)(  v & 0xFF);
      buf[offset++]= (byte)( (v>>8) & 0xFF);
      buf[offset++]= (byte)((v>>16) & 0xFF);
      buf[offset++]= (byte)((v>>24) & 0xFF);
      buf[offset++]= (byte)((v>>32) & 0xFF);
      buf[offset++]= (byte)((v>>40) & 0xFF);
      buf[offset++]= (byte)((v>>48) & 0xFF);
      buf[offset++]= (byte)((v>>56) & 0xFF);
   }
   
   /** Saut de i octets */
   public void skip(int i) { offset+=i; }

   /** Lecture d'une chaine */
   public String readString() {
      int n = readShort();
      if( n==-1 ) return null;
      String s = new String(buf,offset,n);
      offset+=n;
      return s;
   }

   /** Lecture d'une Boolean */
   public boolean readBoolean() {
      return buf[offset++]!=(byte)0;
   }

   /** Lecture d'un byte */
   public byte readByte() {
      return buf[offset++];
   }
   
   /** Lecture d'un entier */
   public short readShort() {
      return
      (short)((buf[offset++]&0xFF)
      | ( (buf[offset++]&0xFF) <<8 ) );
   }

   /** Lecture d'un entier */
   public int readInteger() {
      return
      (buf[offset++]&0xFF)
      | ( (buf[offset++]&0xFF) <<8 ) 
      | ( (buf[offset++]&0xFF) <<16 ) 
      | ( (buf[offset++]&0xFF) <<24 );
   }


   /** Lecture d'un long */
   public long readLong() {
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
}
