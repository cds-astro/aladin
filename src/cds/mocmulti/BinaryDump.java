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

package cds.mocmulti;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;

import cds.aladin.MyProperties;
import cds.moc.Moc;
import cds.moc.Moc1D;
import cds.moc.Moc2D;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;

/**
 * Binary "dumper" dedicated for MultiMoc
 * Writter and reader for MultiMoc as a binary dump
 * @version 1.0 - sept 2011
 * @author Pierre Fernique [CDS]
 */
public final class BinaryDump {
   
   static private boolean debug=false;
   static private final byte BINVERSION[]  = { 'M','C','1','2' };  // Binary magic code
   static private final long MAGICODE = 2021042317L;
   
   public BinaryDump() { }
   
   
   /** Load a MultiMoc from a binary dump file
    * @param path filename of the binary dump file
    * @return the MultiMoc
    * @throws Exception
    */
   public MultiMoc load(String path) throws Exception {
      long deb = System.currentTimeMillis();
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

      buf = new BufReader(rf);
      MultiMoc moc;
      try {
         moc = parseDump(buf);
      rf.close();
         rf=null;
      } catch( Exception e ) {
         e.printStackTrace();
         throw e;
      } finally { if( rf!=null ) rf.close(); }

      long fin = System.currentTimeMillis();
      long duree = fin-deb;
      if( debug ) System.out.println("MultiMoc binary dump read in "+(duree/1000.)+"s => "+Unite.getUnitDisk( moc.getMem(),0));
      
      return moc;
   }

   
   /** Save a MultiMoc as a binary dump file
    * @param mMoc MultiMoc to save
    * @param path filename of the binary dump file
    */
   public void save(MultiMoc mMoc,String path) throws Exception {
      long deb = System.currentTimeMillis();
      File tmp=null;
      long size=0L;
      
      try {
         tmp = new File(path+".tmp"+(System.currentTimeMillis()%1000));
         RandomAccessFile rf = new RandomAccessFile(tmp,"rw");
         rf.write(BINVERSION);
         createDump(mMoc,rf);
         rf.close();

         File f = new File(path);
         f.delete();
         tmp.renameTo(f);
         size=f.length();
         tmp=null;
      } finally {
         if( tmp!=null ) tmp.delete();
      }

      long fin = System.currentTimeMillis();
      long duree = fin-deb;
      if( debug ) System.out.println("MultiMoc binary dump written in "+(duree/1000.)+"s => RAM="+Unite.getUnitDisk( mMoc.getMem(),0) 
                                                +" Dump="+Unite.getUnitDisk( size,0));
   }
   
   /** Binary parsing of a MultiMoc stored in a Buf
    * @param buf Binary buf containing the MultiMoc
    * @return a valid MultiMoc
    */
   public MultiMoc parseDump(BufReader buf) throws Exception {
      int b=0;
      
      MultiMoc mMoc = new MultiMoc();
      mMoc.clear();
      buf.readString();
      int nbMoc = buf.readInteger();
      for( int i=0; i<nbMoc; i++ ) {
         
         // Lecture d'un MOC
         long dateMoc = buf.readLong();
         String mocId = buf.readString();
         Moc moc = null;
         byte typeMoc = buf.readByte();
         if( typeMoc!=NOMOC ) {
            
                 if( typeMoc==SMOC ) moc = new SMoc();
            else if( typeMoc==TMOC ) moc = new TMoc();
            else if( typeMoc==STMOC) moc = new STMoc();
            else throw new Exception("Unknown MOC type => ["+typeMoc+"]");
                 
            if( moc instanceof Moc1D ) {
               ((Moc1D)moc).setMocOrder( buf.readInteger() );
               
            } else {
              ((Moc2D)moc).setMocOrder1( buf.readInteger() );
              ((Moc2D)moc).setMocOrder2( buf.readInteger() );
            }
            
            int nbBytes = buf.readInteger();
            byte [] a = new byte[nbBytes];
            for( int j=0; j<a.length; j++ ) a[j] = buf.readByte();
            moc.readSpecificDataRange( nbBytes/8, a, Moc.COMPRESS_SINGLETON);
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
      long mc = buf.readLong();
      if( mc!=MAGICODE ) throw new Exception("Multimoc dump error. Bad end MAGIC CODE");
      return mMoc;
   }

   
//   /** Binary parsing of a MultiMoc stored in a Buf
//    * @param buf Binary buf containing the MultiMoc
//    * @return a valid MultiMoc
//    */
//   public MultiMoc parseDump(BufReader buf) throws Exception {
//      MultiMoc mMoc = new MultiMoc();
//      buf.readString();
//      int nbMoc = buf.readInteger();
//      for( int i=0; i<nbMoc; i++ ) {
//         
//         // Lecture d'un MOC
//         long dateMoc = buf.readLong();
//         String mocId = buf.readString();
//         SMoc moc = null;
//         int maxOrder = buf.readInteger();
//         if( maxOrder!=-1 ) {
//            moc=new SMoc(maxOrder);
//            int sz = buf.readInteger();
//            Range range = new Range(sz/2);
//            for( int j=0; j<sz; j++ ) range.push( buf.readLong() );
//            moc.setRangeList( range );
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
//         mMoc.add(mocId,moc,prop,dateMoc,dateProp);
//      }
//      long mc = buf.readLong();
//      if( mc!=MAGICODE ) throw new Exception("Multimoc dump error. Bad end MAGIC CODE");
//      return mMoc;
//   }

   
   private static final byte NOMOC = 0;
   private static final byte SMOC  = 1;
   private static final byte TMOC  = 2;
   private static final byte STMOC = 3;


   /** Generate the dump associated to a MultiMoc
    * @param mMoc MultiMoc to dump
    * @return binary buffer 
    * @seealso parseDump(Buf)
    */
   public void createDump(MultiMoc mMoc,RandomAccessFile rf) throws Exception {
      BufWriter buf = new BufWriter(rf);

      buf.memoString(mMoc.getCoordSys());
      buf.memoInteger(mMoc.size());
      for( MocItem mi : mMoc ) {
         
         // Enregistrement d'un MOC
         buf.memoLong(mi.dateMoc);
         Moc moc = mi.moc;
         String mocId = mi.mocId;
         buf.memoString(mocId);
         
         if( moc==null ) buf.memoByte(NOMOC);
         else {
            
            
            int nbCoding=-1;
            
            // SMOc et TMOC
            if( moc instanceof SMoc ) {
               buf.memoByte( SMOC );
               buf.memoInteger( ((Moc1D)moc).getMocOrder() );
               nbCoding = ((Moc1D)moc).seeRangeList().sz;   // Codage en range, par en Nuniq !!
               
            // TMOC
            } else if( moc instanceof TMoc ) {
               buf.memoByte( TMOC );
               buf.memoInteger( ((Moc1D)moc).getMocOrder() );
              
            // STMOC
            } else {
               buf.memoByte(STMOC);
               buf.memoInteger( ((Moc2D)moc).getMocOrder1() );
               buf.memoInteger( ((Moc2D)moc).getMocOrder2() );
            }
            
            if( nbCoding==-1) nbCoding = moc.getNbCoding();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(nbCoding*8);
            int nbBytes = moc.writeSpecificDataRange(byteStream,Moc.COMPRESS_SINGLETON);
            buf.memoInteger(nbBytes);
            byte [] a = byteStream.toByteArray();
            for( int i=0; i<nbBytes; i++ ) buf.memoByte(a[i]);
         }

         // Enregistrement de ses propriétés
         buf.memoLong(mi.dateProp);
         MyProperties prop = mi.prop;
         if( prop==null ) buf.memoInteger(0);
         else {
            int n = prop.size();
            buf.memoInteger( n );
            for( String key : prop.getKeys() ) {
               buf.memoString(key);
               String val = prop.get(key);
               buf.memoString(val);
            }
         }
         
         buf.flush();
      }
      // Marque de fin de fichier
      buf.memoLong(MAGICODE);
      buf.flush();

   }

//   /** Generate the dump associated to a MultiMoc
//    * @param mMoc MultiMoc to dump
//    * @return binary buffer 
//    * @seealso parseDump(Buf)
//    */
//   public void createDump(MultiMoc mMoc,RandomAccessFile rf) throws Exception {
//      BufWriter buf = new BufWriter(rf);
//
//      buf.memoString(mMoc.getCoordSys());
//      buf.memoInteger(mMoc.size());
//      for( MocItem mi : mMoc ) {
//         
//         // Enregistrement d'un MOC
//         buf.memoLong(mi.dateMoc);
//         SMoc moc = mi.moc;
//         String mocId = mi.mocId;
//         buf.memoString(mocId);
//         if( moc==null ) buf.memoInteger(-1);
//         else {
//            int mocOrder = moc.getMocOrder();
//            buf.memoInteger(mocOrder);
//            Range range = moc.seeRangeList();
//            buf.memoInteger(range.sz);
//            for( int i=0; i<range.sz; i++ ) buf.memoLong(range.r[i]);
//               }
//
//         // Enregistrement de ses propriétés
//         buf.memoLong(mi.dateProp);
//         MyProperties prop = mi.prop;
//         if( prop==null ) buf.memoInteger(0);
//         else {
//            int n = prop.size();
//            buf.memoInteger( n );
//            for( String key : prop.getKeys() ) {
//               buf.memoString(key);
//               String val = prop.get(key);
//               buf.memoString(val);
//            }
//         }
//         
//         buf.flush();
//            }
//      // Marque de fin de fichier
//      buf.memoLong(MAGICODE);
//      buf.flush();
//        
//      }
   
   // Tests
//   static private void test() throws Exception {
//      debug=true;
//      Moc moc = Moc.createMoc( new FileInputStream( new File("/Data/Moc/Data/CDS_B_avo.rad_wsrt.fits")));
//      MultiMoc mMoc = new MultiMoc();
//      mMoc.add("moc", moc, null,0,0);
//      
//      System.out.println("BEFORE :\n"+mMoc+":");
//      for( MocItem mi : mMoc ) {
//         System.out.println(mi.mocId+" : "+mi.moc.toDebug() );
//      }
//
//      System.out.println("Dumping and reloading...");
//      String file = "/Data/Mmoctest.bin";
//      BinaryDump dump = new BinaryDump();
//      dump.save(mMoc, file);
//      mMoc = dump.load(file);
//      (new File(file)).delete();
//
//      System.out.println("\nAFTER :\n"+mMoc+":");
//      for( MocItem mi : mMoc ) {
//         System.out.println(mi.mocId+" : "+mi.moc.toDebug() );
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
