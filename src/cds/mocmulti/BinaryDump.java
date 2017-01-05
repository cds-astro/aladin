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

import java.io.File;
import java.io.RandomAccessFile;

import cds.aladin.MyProperties;
import cds.moc.Array;
import cds.moc.HealpixMoc;
import cds.moc.IntArray;
import cds.moc.LongArray;
import cds.moc.ShortArray;

/**
 * Binary "dumper" dedicated for MultiMoc
 * Writter and reader for MultiMoc as a binary dump
 * @version 1.0 - sept 2011
 * @author Pierre Fernique [CDS]
 */
public final class BinaryDump {
   
   static private boolean debug=false;
   static private final byte BINVERSION[]  = { 'M','C','0','8' };  // Binary magic code
   
   public BinaryDump() { }
   
   
   /** Load a MultiMoc from a binary dump file
    * @param path filename of the binary dump file
    * @return the MultiMoc
    * @throws Exception
    */
   public MultiMoc load(String path) throws Exception {
      long deb = System.currentTimeMillis();
      int taille;
      BufReader buf;
      File f = new File(path);
      RandomAccessFile rf = new RandomAccessFile(f,"r");
      byte version[] = new byte[4];

      rf.readFully(version);
      String va = new String(version);
      String vb = new String(BINVERSION);
      if( !va.equals(vb) ) {
         rf.close();
         throw new Exception("MultiMoc binary dump not compatible (found ["+va+"], required ["+vb+"]");
      }
      taille = (int)rf.length() - 4;   // On retranche le code de version (4 octets)

      buf = new BufReader(rf);
      MultiMoc moc = parseDump(buf);
      rf.close();

      long fin = System.currentTimeMillis();
      long duree = fin-deb;
      if( debug ) System.out.println("MultiMoc binary dump read in "+(duree/1000.)+"s");
      
      return moc;
   }

   
//   public MultiMoc load(String path) throws Exception {
//      long deb = System.currentTimeMillis();
//      int taille;
//      Buf buf;
//      File f = new File(path);
//      RandomAccessFile rf = new RandomAccessFile(f,"r");
//      byte version[] = new byte[4];
//
//      rf.readFully(version);
//      String va = new String(version);
//      String vb = new String(BINVERSION);
//      if( !va.equals(vb) ) {
//         rf.close();
//         throw new Exception("MultiMoc binary dump not compatible (found ["+va+"], required ["+vb+"]");
//      }
//      taille = (int)rf.length() - 4;   // On retranche le code de version (4 octets)
//
//      buf = new Buf(taille);
//      for( int lu=0,n; lu<taille; lu+=n ) {
//         n=rf.read(buf.buf,lu,taille-lu>8192?8192:taille-lu);
//      }
//      rf.close();
//      MultiMoc moc = parseDump(buf);
//
//      long fin = System.currentTimeMillis();
//      long duree = fin-deb;
//      if( debug ) System.out.println("MultiMoc binary dump read in "+(duree/1000.)+"s");
//      
//      return moc;
//   }
   
   /** Save a MultiMoc as a binary dump file
    * @param mMoc MultiMoc to save
    * @param path filename of the binary dump file
    */
   public void save(MultiMoc mMoc,String path) throws Exception {
      long deb = System.currentTimeMillis();
      File f = new File(path);
      f.delete();
      RandomAccessFile rf = new RandomAccessFile(f,"rw");
      rf.write(BINVERSION);
      createDump(mMoc,rf);
      rf.close();
      long fin = System.currentTimeMillis();
      long duree = fin-deb;
      if( debug ) System.out.println("MultiMoc binary dump written in "+(duree/1000.)+"s");
   }
   
   /** Binary parsing of a MultiMoc stored in a Buf
    * @param buf Binary buf containing the MultiMoc
    * @return a valid MultiMoc
    */
   public MultiMoc parseDump(BufReader buf) throws Exception {
      MultiMoc mMoc = new MultiMoc();
      mMoc.clear();
      buf.readString();
      int nbMoc = buf.readInteger();
      for( int i=0; i<nbMoc; i++ ) {
         
         // Lecture d'un MOC
         long dateMoc = buf.readLong();
         String mocId = buf.readString();
         HealpixMoc moc = new HealpixMoc();
         int maxOrder = buf.readInteger();
         if( maxOrder==-1 ) moc=null;
         else {
            for( int o=0; o<=maxOrder; o++ ) {
               int size = buf.readInteger();
               int type=HealpixMoc.getType(o);
               switch(type) {
                  case HealpixMoc.SHORT:
                     short [] val = new short[size];
                     for( int j=0; j<size; j++) val[j] = buf.readShort();
                     moc.setPixLevel(o,val);
                     break;
                  case HealpixMoc.INT:
                     int [] val1 = new int[size];
                     for( int j=0; j<size; j++) val1[j] = buf.readInteger();
                     moc.setPixLevel(o,val1);
                     break;
                  case HealpixMoc.LONG:
                     long [] val2 = new long[size];
                     for( int j=0; j<size; j++) val2[j] = buf.readLong();
                     moc.setPixLevel(o,val2);
                     break;
               }
            }
         }
         
         // Lecture de ses propriétés
         long dateProp = buf.readLong();
         int n = buf.readInteger();
         MyProperties prop = n>0 ? new MyProperties() : null;
         for( int j=0; j<n; j++ ) {
            String key = buf.readString();
            String value = buf.readString();
            prop.put(key, value);
         }
         mMoc.add(mocId,moc,prop,dateMoc,dateProp);
      }
      return mMoc;
   }

//   public MultiMoc parseDump(Buf buf) throws Exception {
//      MultiMoc mMoc = new MultiMoc();
//      mMoc.clear();
//      buf.readString();
//      int nbMoc = buf.readInteger();
//      for( int i=0; i<nbMoc; i++ ) {
//         
//         // Lecture d'un MOC
//         long dateMoc = buf.readLong();
//         String mocId = buf.readString();
//         HealpixMoc moc = new HealpixMoc();
//         int maxOrder = buf.readInteger();
//         if( maxOrder==-1 ) moc=null;
//         else {
//            for( int o=0; o<=maxOrder; o++ ) {
//               int size = buf.readInteger();
//               int type=HealpixMoc.getType(o);
//               switch(type) {
//                  case HealpixMoc.SHORT:
//                     short [] val = new short[size];
//                     for( int j=0; j<size; j++) val[j] = buf.readShort();
//                     moc.setPixLevel(o,val);
//                     break;
//                  case HealpixMoc.INT:
//                     int [] val1 = new int[size];
//                     for( int j=0; j<size; j++) val1[j] = buf.readInteger();
//                     moc.setPixLevel(o,val1);
//                     break;
//                  case HealpixMoc.LONG:
//                     long [] val2 = new long[size];
//                     for( int j=0; j<size; j++) val2[j] = buf.readLong();
//                     moc.setPixLevel(o,val2);
//                     break;
//               }
//            }
//         }
//         
//         // Lecture de ses propriétés
//         long dateProp = buf.readLong();
//         int n = buf.readInteger();
//         MyProperties prop = n>0 ? new MyProperties() : null;
//         for( int j=0; j<n; j++ ) {
//            String key = buf.readString();
//            String value = buf.readString();
//            prop.put(key, value);
//         }
//         System.out.println("load binary "+mocId);
//         mMoc.add(mocId,moc,prop,dateMoc,dateProp);
//      }
//      return mMoc;
//   }
//   
   /** Generate the dump associated to a MultiMoc
    * @param mMoc MultiMoc to dump
    * @return binary buffer 
    * @seealso parseDump(Buf)
    */
   public void createDump(MultiMoc mMoc,RandomAccessFile rf) throws Exception {
//      int taille = sizeOfBinary(mMoc);
//      if( debug ) System.out.println("Buf size = "+taille);
      BufWriter buf = new BufWriter(rf);

      buf.memoString(mMoc.getCoordSys());
      buf.memoInteger(mMoc.size());
      for( MocItem mi : mMoc ) {
         
         // Enregistrement d'un MOC
         buf.memoLong(mi.dateMoc);
         HealpixMoc moc = mi.moc;
         String mocId = mi.mocId;
         buf.memoString(mocId);
         if( moc==null ) buf.memoInteger(-1);
         else {
            int maxOrder = moc.getMaxOrder();
            buf.memoInteger(maxOrder);
            for( int o=0; o<=maxOrder; o++ ) {
               int size = moc.getSize(o);
               buf.memoInteger( size );
               Array a = moc.getArray(o);
               int type = HealpixMoc.getType(o);
               switch(type) {
                  case HealpixMoc.SHORT:
                     short [] val = ((ShortArray)a).seeArray();
                     for( int i=0; i<size; i++ ) buf.memoShort(val[i]);
                     break;
                  case HealpixMoc.INT:
                     int [] val1 = ((IntArray)a).seeArray();
                     for( int i=0; i<size; i++ ) buf.memoInteger(val1[i]);
                     break;
                  case HealpixMoc.LONG:
                     long [] val2 = ((LongArray)a).seeArray();
                     for( int i=0; i<size; i++ ) buf.memoLong(val2[i]);
                     break;
               }
            }
         }

         // Enregistrement de ses propriétés
         buf.memoLong(mi.dateProp);
         MyProperties prop = mi.prop;
         if( prop==null ) buf.memoInteger(0);
         else {
            int n = prop.size();
            buf.memoInteger( n );
            
//            Enumeration<String> e = prop.keys();
//            while( e.hasMoreElements() ) {
//               String key = e.nextElement();
               
            for( String key : prop.getKeys() ) {
               buf.memoString(key);
               String val = prop.get(key);
               buf.memoString(val);
            }
         }
         
         buf.flush();
      }
   }

   
//   public Buf createDump(MultiMoc mMoc) {
//      int taille = sizeOfBinary(mMoc);
//      if( debug ) System.out.println("Buf size = "+taille);
//      Buf buf = new Buf(taille);
//
//      buf.memoString(mMoc.getCoordSys());
//      buf.memoInteger(mMoc.size());
//      for( MocItem mi : mMoc ) {
//         
//         // Enregistrement d'un MOC
//         buf.memoLong(mi.dateMoc);
//         HealpixMoc moc = mi.moc;
//         String mocId = mi.mocId;
//         buf.memoString(mocId);
//         if( moc==null ) buf.memoInteger(-1);
//         else {
//            int maxOrder = moc.getMaxOrder();
//            buf.memoInteger(maxOrder);
//            for( int o=0; o<=maxOrder; o++ ) {
//               int size = moc.getSize(o);
//               buf.memoInteger( size );
//               Array a = moc.getArray(o);
//               int type = HealpixMoc.getType(o);
//               switch(type) {
//                  case HealpixMoc.SHORT:
//                     short [] val = ((ShortArray)a).seeArray();
//                     for( int i=0; i<size; i++ ) buf.memoShort(val[i]);
//                     break;
//                  case HealpixMoc.INT:
//                     int [] val1 = ((IntArray)a).seeArray();
//                     for( int i=0; i<size; i++ ) buf.memoInteger(val1[i]);
//                     break;
//                  case HealpixMoc.LONG:
//                     long [] val2 = ((LongArray)a).seeArray();
//                     for( int i=0; i<size; i++ ) buf.memoLong(val2[i]);
//                     break;
//               }
//            }
//         }
//
//         // Enregistrement de ses propriétés
//         buf.memoLong(mi.dateProp);
//         MyProperties prop = mi.prop;
//         if( prop==null ) buf.memoInteger(0);
//         else {
//            int n = prop.size();
//            buf.memoInteger( n );
//            Enumeration e = prop.keys();
//            while( e.hasMoreElements() ) {
//               String key = (String)e.nextElement();
//               buf.memoString(key);
//               String val = prop.get(key);
//               buf.memoString(val);
//            }
//         }
//      }
//      return buf;
//   }
   
   // return the number of bytes required for dumping the MultiMoc
//   private int sizeOfBinary(MultiMoc mMoc) {
//      int size = 4;  // nbMoc
//      size+=Buf.sizeOfString(mMoc.getCoordSys());
//      for( MocItem mi : mMoc ) {         
//         size+=8;   // Date Moc
//         String mocId = mi.mocId;
//         HealpixMoc moc = mi.moc;
//         size+=Buf.sizeOfString(mocId);
//         if( moc==null ) size+=4;
//         else {
//            int maxOrder = moc.getMaxOrder();
//            size+= 4;  // maxOrder
//            for( int o=0; o<=maxOrder; o++) {
//               size+= 4;  // val.length;
//               int type = HealpixMoc.getType(o);
//               int sizePix = type==HealpixMoc.SHORT ? 2 : type==HealpixMoc.INT ? 4 : 8;
//               size+= sizePix * moc.getSize(o);
//            }
//         }
//         
//         MyProperties prop = mi.prop;
//         size+=8;   // Date Prop
//         size+= 4;  // Nombre de propriétés
//         if( prop!=null ) {
//            Enumeration e = prop.keys();
//            while( e.hasMoreElements() ) {
//               String key = (String)e.nextElement();
//               size+=Buf.sizeOfString( key );
//               size+=Buf.sizeOfString( prop.get(key) );
//            }
//         }
//        
//      }
//      return size;
//   }
   
   // Tests
//   static private void test() throws Exception {
//      debug=true;
//      String s = "3/280 28/"+(Integer.MAX_VALUE+100L);
//      HealpixMoc moc1 = new HealpixMoc(s); moc1.trim();
//      HealpixMoc moc2 = new HealpixMoc("3/281 28/1"); moc2.trim();
//      MultiMoc mMoc = new MultiMoc();
//      mMoc.add("moc1", moc1, null,0,0);
//      mMoc.add("moc2", moc2, null,0,0);
//      
//      System.out.println("BEFORE :\n"+mMoc+":");
//      for( MocItem mi : mMoc ) {
//         System.out.println(mi.mocId+" : "+mi.moc );
//      }
//      
//      System.out.println("Dumping and reloading...");
//      String file = "Mmoctest.bin";
//      BinaryDump dump = new BinaryDump();
//      dump.save(mMoc, file);
//      mMoc = dump.load(file);
//      (new File(file)).delete();
//      
//      System.out.println("\nAFTER :\n"+mMoc+":");
//      for( MocItem mi : mMoc ) {
//         System.out.println(mi.mocId+" : "+mi.moc );
//      }
//      
//   }
//   
//   public static void main(String[]args) {
//      try {
//         test();
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }
}
