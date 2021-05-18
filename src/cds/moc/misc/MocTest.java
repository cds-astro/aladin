// Copyright 2011 - Unistra/CNRS
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
package cds.moc.misc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.Moc;
import cds.moc.MocCell;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.Util;

public class MocTest {


   static String EXEMPLE = "12/9 12 "
         +"13/13-15 24 26-27 33 45 52 54 56-57 "
         +"14/39 45-47 49-51 102 122 141 143 176-177 179 188-189 232-233 236-237 240-242 "
         +"15/111 122-123 126-127 151 177 179 193-195 298-299 400 402-403 412 414-415 480 "
         +"482-483 492 494 560-561 563 569 660-661 663 712-713 715 740-741 743 761 764-765 "
         +"880 882 936-937 940-941 952-953 956 972 974 992-994 "
         +"16/430-431 441-443 483 486-487 498-499 502-503 599 603 615 621-623 705 707 "
         +"713-715 769-771 1208 1210-1211 1214-1215 1606 1624 1626-1627 1652 1654-1655 1832 "
         +"1834-1835 1924 1926-1927 1944 1946 1980 1982 2069 2071 2077 2079 2101 2249 2251 "
         +"2273 2284-2285 2287 2629 2649 2651 2676-2677 2679 2856-2857 2859 2948-2949 2951 "
         +"2969 3040-3041 3065 3068-3069 3400 3402 3424 3426 3432 3434 3552-3554 3560 3562 "
         +"3752-3753 3756-3757 3768-3769 3772-3773 3816-3817 3820-3821 3828-3829 3892 389 4"
         +"3904 3906 3980 3984";

   private static boolean testSetMocOrder() throws Exception {
      title("testSetMocOrder: Create a Moc manually, degrade the resolution step by step, and check the result...");
      long max,min;
      int order;

      System.out.println("SMOC testSetMocOrder");
      min = 1;
      max = SMoc.NBVAL_S/2L;
      SMoc moc1 = new SMoc("29/"+min+"-"+max);
      String ref1 = "0/0-5 4/1536";
      String s1=null;
      for( order=29; order>=0; order-=5) {
         moc1.setMocOrder(order);
         s1=moc1.toString();
         System.out.println("order: "+order+" -> "+s1+" range="+moc1.seeRangeList());
      }
      boolean rep=ref1.equals(moc1.toString());
      if( !rep ) System.out.println("MocTest.testSetMocOrder ERROR: \n.get ["+s1+"]\n.ref ["+ref1+"]\n");

      System.out.println("\nTMOC testSetMocOrder");
      min = 1;
      max = TMoc.NBVAL_T/2L;
      TMoc moc2 = new TMoc("61/"+min+"-"+max);
      String ref2 = "1/0 11/1024";
      String s2=null;
      for( order=61; order>=2; order-=10) {
         moc2.setMocOrder(order);
         s2=moc2.toString();
         System.out.println("order: "+order+" -> "+s2+" range="+moc2.seeRangeList());
      }
      rep&=ref2.equals(moc2.toString());
      if( !rep ) System.out.println("MocTest.testSetMocOrder ERROR TMOC: \n.get ["+s2+"]\n.ref ["+ref2+"]\n");

      System.out.println("\nSTMOC testSetMocOrder");
      min = 1L;
      max = Healpix.pow2(61)/12L;
      long mins = 1L;
      long maxs = Healpix.pow2(29)*Healpix.pow2(29);
      STMoc moc3 = new STMoc("t3/0-1 s3/1-3 t3/2 s3/2-5");
      String ref3 = "t1/0 s0/0";
      String s3=null;
      int sorder=3;
      for( order=61; order>=1; order-=20, sorder--) {
         moc3.setMocOrder(order, sorder);
         s3=moc3.toString();
         System.out.println("torder:"+order+",sorder="+sorder+" -> "+s3+" range="+moc3.seeRangeList());
      }
      rep&=ref3.equals(moc3.toString());
      if( !rep ) System.out.println("MocTest.testSetMocOrder ERROR TMOC: \n.get ["+s3+"]\n.ref ["+ref3+"]\n");

      if( rep ) System.out.println("MocTest.testSetMocOrder OK");
      return rep;
   }


   private static boolean testBasic() throws Exception {
      title("testBasic: Create a Moc manually and check the result...");
      String ref = " 3/1 3/3 3/10 4/16 4/17 4/18 4/22";
      SMoc moc = new SMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("3/1");
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append(" "+p.order+"/"+p.start);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testBasic ERROR: \n.get ["+s+"]\n.ref ["+ref+"]\n");
         return false;
      }
      
      long nbCells=0L;
      Iterator<Long> it1 = moc.valIterator();
      while( it1.hasNext() ) { it1.next(); nbCells++; }
      System.out.println("\nNb used cells at MocOrder "+moc.getMocOrder()+" => "+ moc.getNbValues());
      rep = moc.getNbValues() == nbCells;
      if( !rep ) {
         System.out.println("Moc: "+moc);
         System.out.println("MocTest.testBasic ERROR: \n.nbCells get ["+moc.getNbCells()+"]\n.ref ["+nbCells+"]\n");
         return false;
      }
      
      
      if( rep ) System.out.println("MocTest.testBasic OK");
      return rep;
   }


   private static boolean testBasicTMoc() throws Exception {
      title("testBasic: Create a Moc manually and check the result...");
      String ref = "t59/12 60/26";
      Moc moc = Moc.createMoc( ref );
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         if( s.length()>0 ) s.append(' ');
         else s.append('t');
         s.append(p.order+"/"+p.start);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testBasicTMoc ERROR: \n.get ["+s+"]\n.ref ["+ref+"]\n");
      } else System.out.println("MocTest.testBasicTMoc OK");
      return rep;
   }

   private static boolean testAdvancedTMoc() throws Exception {
      TMoc tmoc = new TMoc();
      double jdmin = Astrodate.dateToJD(2000, 06, 18, 12, 00, 00);  // 18 juin 2000 à midi
      double jdmax = Astrodate.dateToJD(2017, 12, 25, 00, 00, 00);  // Noël 2017 à minuit
      System.out.println("min="+jdmin+" ("+Astrodate.JDToDate(jdmin)+") max="+jdmax+" ("+Astrodate.JDToDate(jdmax)+")");
      tmoc.add(jdmin, jdmax);
      jdmin = Astrodate.dateToJD(2019, 06, 18, 12, 00, 00);  // 18 juin 2019 à midi
      jdmax = Astrodate.dateToJD(2019, 12, 25, 00, 00, 00);  // Noël 2019 à minuit
      System.out.println("min="+jdmin+" ("+Astrodate.JDToDate(jdmin)+") max="+jdmax+" ("+Astrodate.JDToDate(jdmax)+")");
      tmoc.add(jdmin, jdmax);
      System.out.println("Moc="+tmoc);
      System.out.println("min="+Astrodate.JDToDate( tmoc.getTimeMin()));
      System.out.println("max="+Astrodate.JDToDate( tmoc.getTimeMax()));

      boolean rep0 = Astrodate.JDToDate( tmoc.getTimeMin()).equals("2000-06-18T12:00:00") 
            && !Astrodate.JDToDate( tmoc.getTimeMin()).equals("2019-12-25T00:00:00"); 
      if( !rep0 ) {
         System.out.println("MocTest.testAdvancedTMoc ERROR: \n. Wrong min or max time\n");
      }

      String file = "/Data/Tmoc.fits";
      tmoc.write(file,Moc.ASCII);
      TMoc tmoc1 = new TMoc();
      tmoc1.read(file);

      boolean rep =  tmoc1.equals(tmoc);
      if( !rep ) {
         System.out.println("MocTest.testAdvancedTMoc ERROR: \n.get ["+tmoc1+"]\n.ref ["+tmoc+"]\n");
      }

      tmoc.write(file,Moc.JSON);
      TMoc tmoc2 = new TMoc();
      tmoc2.read(file);

      boolean rep2 =  tmoc2.equals(tmoc);
      if( !rep2 ) {
         System.out.println("MocTest.testAdvancedTMoc ERROR: \n.get ["+tmoc2+"]\n.ref ["+tmoc+"]\n");
      }


      if( rep0 && rep && rep2) System.out.println("MocTest.testAdvancedTMoc OK");
      return rep;

   }

   private static boolean testBasicSTMoc() throws Exception {
      title("testBasicSTMoc: Create a STMoc manually and check the result...");
      String ref = "t30/1-10 13-15 17 s3/1-2 5-8 t30/72-75 s5/200";
      STMoc moc = new STMoc();
      moc.add("t30/1-10 13-15 17 s3/1 2 5-8 t28/18 s5/200");
      System.out.println("STMOC => "+moc);
      boolean rep = moc.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testBasicSTMoc ERROR: \n.get ["+moc+"]\n.ref ["+ref+"]\n");
      } else System.out.println("MocTest.testBasicSTMoc OK");
      return rep;
   }

   private static boolean testCoverage() throws Exception {
      title("testCoverage: Create Mocs with various parts of the sky and check the results...");
      double fullMoc = (new SMoc( "0/0-11" )).getCoverage();
      double emptyMoc = (new SMoc( )).getCoverage();
      double partialMoc = (new SMoc( "0/0-3" )).getCoverage();
      boolean rep=true;
      if( fullMoc!=1. )      { System.out.println("MocTest.testCoverage ERROR: \n.get ["+fullMoc+"] should be 1"); rep &= false; } 
      if( emptyMoc!=0. )     { System.out.println("MocTest.testCoverage ERROR: \n.get ["+emptyMoc+"] should be 0"); rep &= false; } 
      if( partialMoc!=1./3 ) { System.out.println("MocTest.testCoverage ERROR: \n.get ["+partialMoc+"] should be 1/3"); rep &= false; } 

      if( rep ) System.out.println("MocTest.testCoverage OK");
      return rep;
   }

   private static boolean testSetLimitOrder() throws Exception {
      title("testSetLimitOrder: Test min and max limit order settings...");
      SMoc moc = (SMoc)Moc.createMoc("0/0 3/700 8/");
      String ref= "1/0-3 2/175";
      int mocOrder=2;

      System.out.println("MOC before: "+moc);
      moc.setMinOrder(1);
      moc.setMocOrder(2);
      System.out.println("MOC order [1..2]: "+moc);

      if( !moc.toString().equals(ref) ) {
         System.out.println("MocTest.testSetLimitOrder ERROR\n");
         return false;
      }

      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testSetLimitOrder ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }

      // Même test mais en ajoutant les pixels un par un
      moc = new SMoc();
      moc.setMinOrder(1);
      moc.setMocOrder(2);
      moc.add(0,0);
      moc.add(4,2810);
      if( !moc.toString().equals(ref) ) {
         System.out.println("MocTest.testSetLimitOrder ERROR\n");
         return false;
      }

      System.out.println("testSetLimitOrder OK");
      return true;
   }

   private static boolean testIterativeInsertion() throws Exception {
      int maxIns= 100000;
      title("testIterativeInsertion: Test various npix insertion methods ("+maxIns+" insertions)...");
      long t0=0,t1=0;
      SMoc moc,moc1;

      long max = SMoc.NBVAL_S-1L;
      long [] start = new long[maxIns];
      long [] end = new long[maxIns];
      for( int i=0; i<maxIns; i++ ) {
         long size = (long)(Math.random()*1000000L +1);
         if( size==0 ) size++;
         long val = (long)(Math.random()*(max-size));
         start[i] = val;
         end[i] = val+size;
      }

      // Insertion bas niveau sans buffer
      moc = new SMoc();
      // On le fait 3 fois pour que le JIT ait le temps de faire ce qu'il faut
      for( int j=0;j<3; j++ ) {
         moc.clear();
         t0=System.currentTimeMillis();
         for( int i=0; i<maxIns; i++ ) moc.add(SMoc.MAXORD_S,start[i],end[i]);
         t1=System.currentTimeMillis();
      }
      System.out.println(".insertion without buffer "+maxIns+" cells: "+(t1-t0)+"ms");
      System.out.println(".mem: "+ Util.getUnitDisk( moc.getMem()));

      // Insertion bas niveau avec buffer
      moc = new SMoc();
      moc.bufferOn();
      // On le fait 3 fois pour que le JIT ait le temps de faire ce qu'il faut
      for( int j=0;j<3; j++ ) {
         moc.clear();
         t0=System.currentTimeMillis();
         for( int i=0; i<maxIns; i++ ) moc.add(SMoc.MAXORD_S,start[i],end[i]);
         t1=System.currentTimeMillis();
      }
      moc.seeRangeList();  // Just to force the last flush()
      System.out.println(".insertion with buffer "+maxIns+" cells: "+(t1-t0)+"ms");
      System.out.println(".mem: "+ Util.getUnitDisk( moc.getMem()));

      // Test de la vitesse de determination du nb de cells hierarchique
      long nbCell;
      t0=System.currentTimeMillis();
      nbCell = moc.getNbCells();
      t1=System.currentTimeMillis();
      System.out.println(".nb hierarchy Cells "+nbCell+" cells: "+(t1-t0)+"ms");
      t0=System.currentTimeMillis();
      nbCell = moc.getNbCells();
      t1=System.currentTimeMillis();
      System.out.println(".nb hierarchy Cells (redo) "+nbCell+" cells: "+(t1-t0)+"ms");
      moc.add("29/123456789");
      t0=System.currentTimeMillis();
      nbCell = moc.getNbCells();
      t1=System.currentTimeMillis();
      System.out.println(".nb hierarchy Cells (after add) "+nbCell+" cells: "+(t1-t0)+"ms");

      // Insertion par lecture d'un MOC pre-existant en FITS
      File f = File.createTempFile("Moc", "fits");
      String filename = f.getAbsolutePath();
      moc.write(filename);
      moc1 = new SMoc();
      t0=System.currentTimeMillis();
      moc1.read(filename);
      t1=System.currentTimeMillis();
      System.out.println(".insertion from FITS file: "+(t1-t0)+"ms");
      f.delete();
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results D:\n.moc::"+moc.toDebug()+"\n.moc1:"+moc1.toDebug());
         return false;
      }

      //      // Insertion par lecture d'un MOC pré-existant en JSON
      //      f = File.createTempFile("Moc", "json");
      //      filename = f.getAbsolutePath();
      //      moc.writeJSON(filename);
      //      moc1 = new SMoc();
      //      t0=System.currentTimeMillis();
      //      moc1.read(filename);
      //      t1=System.currentTimeMillis();
      //      System.out.println(".insertion from JSON file: "+(t1-t0)+"ms");
      //      f.delete();
      //      if( !moc.equals(moc1) ) {
      //         System.out.println("MocTest.testIterativeInsertion ERROR: inconsistency results E:\n.moc::"+moc.toDebug()+"\n.moc1:"+moc1.toDebug());
      //         return false;
      //      }

      System.out.println("Moc result:"+moc.toDebug());

      System.out.println("testIterativeInsertion OK");
      return true;
   }


   private static boolean testIterativeInsertionTmoc() throws Exception {
      int maxIns= 100000;
      title("testIterativeInsertionTmoc: Test various npix insertion methods ("+maxIns+" insertions)...");
      long t0=0,t1=0;
      TMoc moc,moc1;

      long max = TMoc.NBVAL_T-1000000;
      long [] start = new long[maxIns];
      long [] end = new long[maxIns];
      for( int i=0; i<maxIns; i++ ) {
         long size = (long)(Math.random()*10000000L +1);
         if( size==0 ) size++;
         long val = (long)(Math.random()*(max-size));
         start[i] = val;
         end[i] = val+size;
      }

      // Insertion bas niveau
      moc = new TMoc();
      moc.bufferOn();
      for( int j=0;j<3; j++ ) {
         moc.clear();
         t0=System.currentTimeMillis();
         for( int i=0; i<maxIns; i++ ) moc.add(TMoc.MAXORD_T,start[i],end[i]);
         t1=System.currentTimeMillis();
      }
      System.out.println(".insertion "+maxIns+" cells: "+(t1-t0)+"ms");

      // Insertion par lecture d'un MOC pre-existant en FITS
      File f = File.createTempFile("Moc", "fits");
      String filename = f.getAbsolutePath();
      moc.write(filename);
      moc1 = new TMoc();
      t0=System.currentTimeMillis();
      moc1.read(filename);
      t1=System.currentTimeMillis();
      System.out.println(".insertion from FITS file: "+(t1-t0)+"ms");
      f.delete();
      if( !moc.equals(moc1) ) {
         System.out.println("MocTest.testIterativeInsertionTmoc ERROR: inconsistency results D:\n.moc::"+moc.toDebug()+"\n.moc1:"+moc1.toDebug());
         return false;
      }

      //      // Insertion par lecture d'un MOC pré-existant en JSON
      //      f = File.createTempFile("Moc", "json");
      //      filename = f.getAbsolutePath();
      //      moc.writeJSON(filename);
      //      moc1 = new TMoc();
      //      t0=System.currentTimeMillis();
      //      moc1.read(filename);
      //      t1=System.currentTimeMillis();
      //      System.out.println(".insertion from JSON file: "+(t1-t0)+"ms");
      //      f.delete();
      //      if( !moc.equals(moc1) ) {
      //         System.out.println("MocTest.testIterativeInsertionTmoc ERROR: inconsistency results E:\n.moc::"+moc.toDebug()+"\n.moc1:"+moc1.toDebug());
      //         return false;
      //      }

      System.out.println("Moc result:"+moc.toDebug());

      System.out.println("testIterativeInsertionTmoc OK");
      return true;
   }


   private static boolean testContains() throws Exception {
      title("testContains: Create a Moc manually and check contains() methods...");
      Healpix hpx = new Healpix();
      SMoc moc = new SMoc("2/0 3/10 4/35");
      System.out.println("MOC: "+moc);
      boolean rep=true;
      try {
         System.out.println("- contains(028.93342,+18.18931) [asserting IN]    => "+moc.contains(hpx,028.93342,18.18931)); rep &= moc.contains(hpx,028.93342,18.18931);
         System.out.println("- contains(057.23564,+15.34922) [asserting OUT]   => "+moc.contains(hpx,057.23564,15.34922)); rep &= !moc.contains(hpx,057.23564,15.34922);
         System.out.println("- contains(031.89266,+17.07820) [asserting IN]    => "+moc.contains(hpx,031.89266,17.07820)); rep &= moc.contains(hpx,031.89266,17.07820);
      } catch( Exception e ) {
         e.printStackTrace();
         rep=false;
      }
      if( !rep ) System.out.println("MocTest.testContains ERROR:");
      else System.out.println("MocTest.testContains OK");
      return rep;
   }

   private static boolean testFITSTMoc() throws Exception {
      title("testFITSTMoc: Create a TMOC manually, write it in FITS and re-read it...");
      TMoc moc = new TMoc();
      moc.add("32/2-4 35/");
      String mocS="31/1 32/4 35/";
      int mocOrder = 35;

      String file = "/Users/Pierre/Desktop/__TMOC.fits";
      System.out.println("- MOC created: "+moc);
      moc.writeFITS(file);
      System.out.println("- test write (FITS) seems OK");

      StringBuilder trace = new StringBuilder();
      FileInputStream in = new FileInputStream(file);
      int rep = MocLint.checkFits(trace,in);
      in.close();
      if( rep==1 ) System.out.println("- test read (FITS) OK and IVOA valid");
      else if( rep==-1 ) System.out.println("- test read (FITS) WARNING, MOC ok but IVOA unvalid");
      if( rep!=1 ) System.out.println(trace);
      if( rep==0 ) {
         System.out.println("MocTest.testFITS ERROR: not IVOA valid");
         return false;
      }

      moc = new TMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testFITSTMoc ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testFITSTMoc ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }

      System.out.println("testFITSTMoc OK");
      return true;
   }



   private static boolean testFITS() throws Exception {
      title("testFITS: Create a MOC manually, write it in FITS and re-read it...");
      SMoc moc = new SMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19-20");
      moc.add("17/222 28/123456789 29/");
      String mocS="3/3 10 4/16-18 22 5/19-20 17/222 28/123456789 29/";
      int mocOrder = 29;

      String testProp = "BigValue";
      moc.setProperty("MYKEY", testProp,"A test for a key");
      try { moc.setProperty("MOCTOOL", "MyTool","An alternate Tools"); } catch( Exception e) {}

      String file = "/Users/Pierre/Desktop/__MOC.fits";
      System.out.println("- MOC created: "+moc);
      moc.writeFITS(file);
      System.out.println("- test write (FITS) seems OK");

      StringBuilder trace = new StringBuilder();
      FileInputStream in = new FileInputStream(file);
      int rep = MocLint.checkFits(trace,in);
      in.close();
      if( rep==1 ) System.out.println("- test read (FITS) OK and IVOA valid");
      else if( rep==-1 ) System.out.println("- test read (FITS) WARNING, MOC ok but IVOA unvalid");
      if( rep!=1 ) System.out.println(trace);
      if( rep==0 ) {
         System.out.println("MocTest.testFITS ERROR: not IVOA valid");
         return false;
      }

      moc = new SMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);

      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testFITS ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testFITS ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }
      System.out.println("MYKEY: "+moc.getProperty("MYKEY"));
      System.out.println("MOCTOOL: "+moc.getProperty("MOCTOOL"));
      if( !testProp.equals(moc.getProperty("MYKEY")) ) {
         System.out.println("MocTest.testFITS ERROR: wrong property: MYKEY should be equals to "+testProp+ "=> ["+moc.getProperty("MYKEY")+"]");
         return false;
      }

      System.out.println("testFITS OK");
      return true;
   }

   private static boolean testFITSSTMoc() throws Exception {
      title("testFITSSTMoc: Create a STMOC manually, write it in FITS and re-read it...");
      STMoc moc = new STMoc("t61/1 3 5 s3/1-3 t61/50 52 s4/25");
      String mocS="t61/1 3 5 s3/1-3 t61/50 52 s4/25";

      String file = "/Users/Pierre/Desktop/__STMOC.fits";
      System.out.println("- MOC created: "+moc);
      moc.writeFITS(file);
      System.out.println("- test write (FITS) seems OK");

      StringBuilder trace = new StringBuilder();
      FileInputStream in = new FileInputStream(file);
      int rep = MocLint.checkFits(trace,in);
      in.close();
      if( rep==1 ) System.out.println("- test read (FITS) OK and IVOA valid");
      else if( rep==-1 ) System.out.println("- test read (FITS) WARNING, MOC ok but IVOA unvalid");
      if( rep!=1 ) System.out.println(trace);
      if( rep==0 ) {
         System.out.println("MocTest.testFITS ERROR: not IVOA valid");
         return false;
      }

      moc = new STMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testFITSSTMoc ERROR: waiting=["+mocS+"]");
         return false;
      }

      System.out.println("testFITSSTMoc OK");
      return true;
   }


   private static boolean testJSON() throws Exception {
      title("testJSON: Create a MOC manually, write it in JSON and re-read it...");
      SMoc moc = new SMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19-20");
      moc.add("17/222 28/123456789 29/");
      String mocS="3/3 10 4/16-18 22 5/19-20 17/222 28/123456789 29/";
      int mocOrder = 29;

      String file = "/Users/Pierre/Desktop/__MOC.json";
      System.out.println("- MOC created: "+moc);
      moc.writeJSON(file);
      System.out.println("- test write (JSON) seems OK");

      moc = new SMoc();
      moc.read(file);
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testJSON ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testJSON ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }

      System.out.println("testJSON OK");
      return true;
   }

   private static boolean testASCII() throws Exception {
      title("testASCII: read ASCII format...");
      String s = 
            "3/3,10 4/16,17,18,22 5/19,20\n" +
                  "17/222 28/123456789\n";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());

      String mocS="3/3 10 4/16-18 22 5/19-20 17/222 28/123456789";

      SMoc moc = new SMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testASCII ERROR: waiting=["+mocS+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testASCII ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }

      System.out.println("testASCII OK");
      return true;
   }

   private static boolean testSTRING() throws Exception {
      title("testASCII: read STRING format...");
      String s = "3/3 10 4/16-18 22 5/19-20 17/222 28/";
      int mocOrder = 28;
      InputStream stream = new ByteArrayInputStream(s.getBytes());

      SMoc moc = new SMoc();
      moc.read(stream);
      System.out.println("- MOC read: "+moc);
      if( !moc.toString().equals(s) ) {
         System.out.println("MocTest.testSTRING ERROR: waiting=["+s+"]");
         return false;
      }
      if( moc.getMocOrder()!=mocOrder ) {
         System.out.println("MocTest.testSTRING ERROR: wrong mocOrder "+moc.getMocOrder()+" waiting "+mocOrder);
         return false;
      }

      System.out.println("testSTRING OK");
      return true;
   }

   private static boolean testOperation() throws Exception {
      title("testOperation: Create 2 Mocs manually, test intersection(), union(), equals(), clone()...");
      SMoc moc1 = (SMoc)Moc.createMoc("3/1,3-4,9 4/30-31");
      String moc1S = "3/1 3-4 9 4/30-31";
      System.out.println("- Loading moc1: "+moc1);
      if( !moc1.toString().equals(moc1S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc1S+"]");
         return false;
      }

      SMoc moc2 = (SMoc)Moc.createMoc("4/23 3/3 10 4/23-28;4/29 5/65");
      String moc2S = "3/3 6 10 4/23 28-29 5/65";
      System.out.println("- Loading moc2: "+moc2);
      if( !moc2.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc2S+"]");
         return false;
      }

      SMoc moc3 =  moc2.clone();
      System.out.println("- Cloning moc2->moc3: "+moc3);
      if( !moc3.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation clone ERROR: waiting=["+moc2S+"]");
         return false;
      }

      Moc moc4 = moc2.intersection(moc1);
      String moc4S = "3/3 5/65";
      System.out.println("- Intersection moc2 moc1: "+moc4);
      if( !moc4.toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: waiting=["+moc4S+"]");
         return false;
      }
      if( !moc1.intersection(moc2).toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: no commutative");
         return false;
      }

      Moc moc5 = moc3.union(moc1);
      String moc5S = "3/1 3-4 6-7 9-10 4/23 5/";
      System.out.println("- Union moc3 moc1: "+moc5);
      Moc moc5b = moc1.union(moc3);
      System.out.println("- Union moc1 moc3: "+moc5b);
      if( !moc5b.toString().equals(moc5.toString()) ) {
         System.out.println("MocTest.testOperation union ERROR: no commutative (get: "+moc5b+")");
         return false;
      }
      if( !moc5.toString().equals(moc5S) ) {
         System.out.println("MocTest.testOperation union ERROR: waiting=["+moc5S+"]");
         return false;
      }

      Moc moc7 = moc1.subtraction(moc2);
      String moc7S = "3/1 9 4/17-19 30-31 5/64 66-67";
      System.out.println("- Subtraction moc1 - moc2: "+moc7);
      if( !moc7.toString().equals(moc7S) ) {
         System.out.println("MocTest.testOperation subtraction ERROR: waiting=["+moc7S+"]");
         return false;
      }

      String moc6S="3/3 6 10 4/23 28 29";
      SMoc moc6 = new SMoc(moc6S);
      boolean test=moc6.equals(moc2);
      System.out.println("- Not-equals moc2 ["+moc6S+"] : "+test);
      if( test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[false]");
         return false;
      }
      moc6.add("5:65");
      test=moc6.equals(moc2);
      System.out.println("- Equals moc2 ["+moc2S+"] : "+test);
      if( !test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[true]");
         return false;
      }

      Moc moc8 = moc1.difference(moc2);
      String moc8S = "3/1 6-7 9-10 4/17-19 23 5/64 66-67";
      System.out.println("- difference moc1  moc2: "+moc8);
      if( !moc8.toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: waiting=["+moc8S+"]");
         return false;
      }
      if( !moc1.difference(moc2).toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: no commutative");
         return false;
      }


      System.out.println("testOperation OK");
      return true;
   }

   private static boolean testComplement() throws Exception {
      title("testComplement: Create 2 Mocs manually, and test isIntersecting() in both directions...");

      SMoc moc10=new SMoc("0/2-11 1/1-3");
      Moc moc9 = moc10.complement();
      String moc9S = "0/1 1/0";
      System.out.println("- SMoc       : "+moc10);
      System.out.println("    Complement: "+moc9);
      if( !moc9.toString().equals(moc9S) ) {
         System.out.println("MocTest.testComplement SMOC ERROR: waiting=["+moc9S+"]");
         return false;
      }

      TMoc moc8=new TMoc("1/0 2/3-4");
      Moc moc7 = moc8.complement();
      String moc7S = "2/2";
      System.out.println("- TMoc       : "+moc8);
      System.out.println("    Complement: "+moc7);
      if( !moc7.toString().equals(moc7S) ) {
         System.out.println("MocTest.testComplement TMOC ERROR: waiting=["+moc7S+"]");
         return false;
      }

      STMoc moc6=new STMoc("t1/0 2/3-4 s0/2-11 1/1-3");
      Moc moc5 = moc6.complement();
      String moc5S = "t2/0-1 s0/1 1/0 t2/2 s0/0-11 t2/3 s0/1 1/0";
      System.out.println("- STMoc       : "+moc6);
      System.out.println("    Complement: "+moc5);
      if( !moc5.toString().equals(moc5S) ) {
         System.out.println("MocTest.testComplement STMOCERROR: waiting=["+moc5S+"]");
         return false;
      }

      System.out.println("testComplement OK");
      return true;
   }



   private static boolean testIsIntersecting() throws Exception {
      title("testIsIntersecting: Create 2 Mocs manually, and test isIntersecting() in both directions...");
      SMoc moc1 = new SMoc("11/25952612");
      SMoc moc2 = new SMoc("9/1622036,1622038");
      System.out.println("moc1="+moc1);
      System.out.println("moc2="+moc2);
      boolean rep1=moc2.isIntersecting(moc1);
      boolean rep2=moc1.isIntersecting(moc2);
      System.out.println("moc2 inter moc1 = "+rep1);
      System.out.println("moc1 inter moc2 = "+rep2);
      if( !rep1 || !rep2 ) {
         System.out.println("MocTest.isIntersecting ERROR");
         return false;
      }

      System.out.println("isIntersecting OK");
      return true;
   }

   private static boolean testisIncluding() throws Exception {
      title("testisIncluding: Create 2 Mocs manually, and test isContaining() in both directions...");
      SMoc moc1 = new SMoc("11/25952612");
      SMoc moc2 = new SMoc("9/1622036,1622038");
      System.out.println("moc1="+moc1);
      System.out.println("moc2="+moc2);
      boolean rep1=moc2.isIncluding(moc1);
      boolean rep2=moc1.isIncluding(moc2);
      System.out.println("moc1 is included in moc2 = "+rep1);
      System.out.println("moc2 is included in moc1 = "+rep2);
      if( !rep1 || rep2 ) {
         System.out.println("MocTest.isIncluding ERROR");
         return false;
      }

      System.out.println("isIncluding OK");
      return true;
   }

   private static boolean testisEmptyOrFull() throws Exception {
      title("testisEmptyOrFull: Check isEmpty and isFull...");

      SMoc moc1 = new SMoc();
      boolean rep1 = moc1.isEmpty();
      if( !rep1 ) System.out.println("SMoc should be empty");
      moc1.add("0/0-11 29/");
      boolean rep2 = moc1.isFull();
      if( !rep2 ) System.out.println("SMoc should be full");
      moc1 = moc1.subtraction( Moc.createMoc("3/1"));
      boolean rep3 = !moc1.isEmpty() && !moc1.isFull();
      if( !rep3 ) System.out.println("SMoc should be not empty nor full");
      if( !rep1 || !rep2 || !rep3 ) {
         System.out.println("MocTest.testisEmptyOrFull SMOC ERROR");
         return false;
      }

      TMoc moc2 = new TMoc();
      rep1 = moc2.isEmpty();
      if( !rep1 ) System.out.println("TMoc should be empty");
      moc2.add("0/0 61/");
      rep2 = moc2.isFull();
      if( !rep2 ) System.out.println("TMoc should be full");
      moc2 = moc2.subtraction( Moc.createMoc("t31/1"));
      rep3 = !moc2.isEmpty() && !moc2.isFull();
      if( !rep3 ) System.out.println("TMoc should be not empty nor full");
      if( !rep1 || !rep2 || !rep3 ) {
         System.out.println("MocTest.testisEmptyOrFull TMOC ERROR");
         return false;
      }

      STMoc moc3 = new STMoc();
      rep1 = moc3.isEmpty();
      if( !rep1 ) System.out.println("STMoc should be empty");
      moc3.add("t0/0 s0/0-11");
      rep2 = moc3.isFull();
      if( !rep3 ) System.out.println("STMoc should be full");
      moc3 = moc3.subtraction( Moc.createMoc("t31/1 s3/11"));
      rep3 = !moc3.isEmpty() && !moc3.isFull();
      if( !rep3 ) System.out.println("STMoc should be not empty nor full");
      if( !rep1 || !rep2 || !rep3 ) {
         System.out.println("MocTest.testisEmptyOrFull STMOC ERROR");
         return false;
      }

      System.out.println("isEmptyOrFull OK");
      return true;
   }

   private static boolean testSTMocExtraction() throws Exception {
      title("testSTMocExtraction: create a STMoc, and extract SMoc and TMoc from it...");

      STMoc moc = (STMoc)Moc.createMoc("t61/3-10 s3/0-2 t61/13-20 s4/81 83");
      System.out.println("STMOC: "+moc);

      SMoc smoc = moc.getSpaceMoc();
      TMoc tmoc = moc.getTimeMoc();
      System.out.println("TMOC : "+tmoc);
      System.out.println("SMOC : "+smoc);

      String s1 = "59/1 4 60/4 7 61/3 10 13 20";
      if( !tmoc.toString().equals(s1) ) {
         System.out.println("MocTest.testSTMocExtraction TMOC ERROR: should be => "+s1);
         return false;
      }

      String s2 = "3/0-2 4/81 83";
      if( !smoc.toString().equals(s2) ) {
         System.out.println("MocTest.testSTMocExtraction SMOC ERROR: should be => "+s2);
         return false;
      }

      System.out.println("testSTMocExtraction OK");
      return true;
   }


   private static void testSpeedSTMoc() throws Exception {
      STMoc moc = new STMoc();
      long t0,t1;
      t0 = System.currentTimeMillis();
      moc.read("C:\\Users\\Pierre\\Documents\\Fits et XML\\MocImg\\PanSTARRs stmoc.fits");
      t1 = System.currentTimeMillis();
      System.out.println("STMoc read: "+(t1-t0)+"ms => "+moc.toDebug());

      t0 = System.currentTimeMillis();
      TMoc tmoc = moc.getTimeMoc();
      t1 = System.currentTimeMillis();
      System.out.println("TMoc extraction: "+(t1-t0)+"ms => "+tmoc.toDebug());

      t0 = System.currentTimeMillis();
      SMoc smoc = moc.getSpaceMoc();
      t1 = System.currentTimeMillis();
      System.out.println("SMoc extraction: "+(t1-t0)+"ms => "+smoc.toDebug());

   }

   private static boolean testDegrade() throws Exception {
      STMoc moc = new STMoc();
      long t0,t1;
      t0 = System.currentTimeMillis();
      moc.read("C:\\Users\\Pierre\\Documents\\Fits et XML\\MocImg\\PanSTARRs stmoc.fits");
      t1 = System.currentTimeMillis();
      System.out.println("STMoc read: "+(t1-t0)+"ms => "+moc.toDebug());

      System.out.println("Before:  "+moc.toDebug());
      int i=0;
      while( moc.getMem()>100L*1024L ) {
         i++;
         long max = (int)( 2L*(moc.getMem()/3L) ); 
         moc.reduction( max );
         System.out.println("Step "+i+": "+ moc.toDebug());
      }
      return true;
   }


   private static boolean testRange() throws Exception {
      title("testRange: Create a Mocs manually, and test setMin and Max limitOrder()...");
      SMoc moc1 = (SMoc) Moc.createMoc("{ \"1\":[0,1], \"2\":[8,9], \"3\":[40,53] }");
      System.out.println("moc1="+moc1);
      moc1.add("3/37 53");
      System.out.println("adding 3/37 53 => "+moc1);
      String s1 = "1/0-1 2/8-9 3/40 53";
      if( !moc1.toString().equals(s1) ) {
         System.out.println("MocTest.testRange add() ERROR: waiting=["+s1+"]");
         return false;
      }

      SMoc moc2 = moc1.clone();
      moc2.setMinOrder(2);
      System.out.println("minOrder2 => "+moc2);
      String s2 = "2/0-9 3/40 53";
      if( !moc2.toString().equals(s2) ) {
         System.out.println("MocTest.testRange setMinOrder(2) ERROR: waiting=["+s2+"]");
         return false;
      }

      SMoc moc3 = moc1.clone();
      moc3.setMocOrder(2);
      System.out.println("mocOrder3 => "+moc3);
      String s3 = "1/0-1 2/8-10 13";
      if( !moc3.toString().equals(s3) ) {
         System.out.println("MocTest.testRange setMocOrder(2) ERROR: waiting=["+s3+"]");
         return false;
      }

      moc3.setMinOrder(1);
      boolean in1 = moc3.isIncluding(0, 1);
      if( in1 ) {
         System.out.println("MocTest.testRange isIncluding(0,1) ERROR: waiting=false]");
         return false;
      }
      boolean in2 = moc3.isIncluding(1, 1);
      if( !in2 ) {
         System.out.println("MocTest.testRange isIncluding(0,0) ERROR: waiting=true]");
         return false;
      }
      boolean in3 = moc3.isIncluding(3, 33);
      if( !in3 ) {
         System.out.println("MocTest.testRange isIncluding(3,33) ERROR: waiting=true]");
         return false;
      }
      boolean in5 = moc3.isIncluding(3, 56);
      if( in5 ) {
         System.out.println("MocTest.testRange isIncluding(3,56) ERROR: waiting=false]");
         return false;
      }

      System.out.println("testRange OK");
      return true;
   }

   private static boolean testIteratorSTMoc() throws Exception {
      title("testIteratorSTMoc: Test on MOC iterators...");
      String s1 = "t30/1-6 8 s1/3-4 t29/20 s2/8 t31/ s3/";
      String ref = "\n t31/2-13 => s1/3-4\n t31/16-17 => s1/3-4\n t31/80-83 => s2/8";
      STMoc moc = new STMoc();
      moc.add(s1);
      System.out.println(".Loading : "+s1);
      System.out.println(".Getting : "+moc);

      // Iterator order per order
      Iterator<MocCell> it = moc.iterator();
      StringBuilder s = new StringBuilder();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append("\n t"+p.order+"/"+p.start+(p.end-1!=p.start?"-"+(p.end-1):"")+" => s"+p.moc);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testIteratorSTMoc [iterator()] ERROR:\n.get ["+s+"]\n.ref ["+ref+"]\n");
         return false;
      }

      System.out.println("testIteratorSTMoc OK");
      return true;
   }

   private static boolean testInclusive() throws Exception {
      title("MocTest: Test isIncluding()...");
      String ref = "2/1 4/33";
      SMoc moc = new SMoc( ref );
      System.out.println(".moc="+moc);

      SMoc reg1 = new SMoc("3/5,6");
      boolean in1 = moc.isIncluding( reg1 );
      System.out.println(".reg1="+reg1+" is included ? => "+in1);
      if( !in1 ) {
         System.out.println("MocTest.testInclusive ERROR: should be true]");
         return false;
      }
      SMoc reg2 = new SMoc("3/5,8");
      boolean in2 = moc.isIncluding( reg2 );
      System.out.println(".reg2="+reg2+" is included ? => "+in2);
      if( in2 ) {
         System.out.println("MocTest.testInclusive ERROR: should be false]");
         return false;
      }

      SMoc reg3 = new SMoc("4/33");
      boolean in3 = moc.isIncluding( reg3 );
      System.out.println(".reg3="+reg3+" is included ? => "+in3);
      if( !in3 ) {
         System.out.println("MocTest.testInclusive ERROR: should be true]");
         return false;
      }

      SMoc reg4 = new SMoc("4/34");
      boolean in4 = moc.isIncluding( reg4 );
      System.out.println(".reg4="+reg4+" is included ? => "+in4);
      if( in4 ) {
         System.out.println("MocTest.testInclusive ERROR: should be false]");
         return false;
      }

      System.out.println("testInclusive OK");
      return true;

   }

   private static boolean testSyscompatibility() throws Exception {
      title("MocTest: Test testSyscompatibility()...");
      SMoc moc = new SMoc("3/1-2");
      moc.setSys("G");
      SMoc moc1 = new SMoc("3/3-4");
      boolean ok=true;
      try { 
         Moc m = moc.union(moc1);
         ok=false;
      } catch( Exception e ) {
         System.out.println("testSyscompatibility: Get exception => ok ["+e.getMessage()+"]");
      }
      if( ok ) System.out.println("testSyscompatibility Ok");
      else System.out.println("testSyscompatibility ERROR: should return exception");
      return ok;
   }

   private static boolean testHashCode() throws Exception {
      title("MocTest: Test testHashCode()...");
      STMoc moc = new STMoc("t60/1-67 s3/1-2 t61/ s3/");
      STMoc moc1 = new STMoc("t60/1-67 61/5 s3/1-2 4/4");
      int hash = moc1.hashCode();
      int hash1 = moc.hashCode();
      System.out.println("hashCode="+hash+" "+hash1);
      System.out.println("moc="+moc);
      System.out.println("moc1="+moc1);
      System.out.println("Equals = "+moc.equals(moc1));
      boolean ok = hash==hash1;
      if( ok ) System.out.println("testHashCode Ok");
      else System.out.println("testHashCode ERROR: hashcodes should be equal");
      return ok;
   }


   static final private String TEST[][] = {
         { "Ajout à vide",                              "",           "t61/5-10 s29/2",   "t61/5-10 s29/2" ,             ""},
         { "Ajout singleton derrière singleton",        "t61/4 s29/1",       "t61/5 s29/2",      "t61/4 s29/1 t61/5 s29/2",            ""},
         { "Ajout singleton avant singleton",           "t61/5 s29/2",       "t61/4 s29/1",      "t61/4 s29/1 t61/5 s29/2",            ""},
         { "Ajout intervalle entrelacés après",         "t61/4-6 s29/1",     "t61/5-8 s29/2",    "t61/4 s29/1 t61/5-6 s29/1-2 t61/7-8 s29/2", ""},
         { "Ajout intervalle entrelacés avant",         "t61/5-8 s29/2",     "t61/4-6 s29/1",    "t61/4 s29/1 t61/5-6 s29/1-2 t61/7-8 s29/2", ""},
         { "Ajout intervalle englobant (s différents)", "t61/2-6 s29/2",     "t61/1-8 s29/1",    "t61/1 s29/1 t61/2-6 s29/1-2 t61/7-8 s29/1", ""},
         { "Ajout intervalle englobant (s identiques)", "t61/2-6 s29/2",     "t61/1-8 s29/2",    "t61/1-8 s29/2",               "t61/2-6 s29/2"},
         { "Ajout intervalle interne (s différents)",   "t61/1-8 s29/1",     "t61/2-6 s29/2",    "t61/1 s29/1 t61/2-6 s29/1-2 t61/7-8 s29/1", ""},
         { "Ajout intervalle interne (s identiques)",   "t61/1-8 s29/2",     "t61/2-6 s29/2",    "t61/1-8 s29/2" ,              "t61/2-6 s29/2"},
         { "Intercallage",                              "t61/6-7 11 s29/1",  "t61/9 s29/2",      "t61/6-7 s29/1 t61/9 s29/2 t61/11 s29/1",    ""},
         { "Fusion différents s",                       "t61/2-6 8-9 s29/2", "t61/7 s29/1",      "t61/2-6 s29/2 t61/7 s29/1 t61/8-9 s29/2",   ""},
         { "Fusion indentiques s",                      "t61/2-6 8-9 s29/2", "t61/7 s29/2",      "t61/2-9 s29/2",               ""},
         { "Remplacement sur début",                    "t61/2-6 s29/2 t61/7 s29/1", "t61/2-7 s29/2",   "t61/2-6 s29/2 t61/7 s29/1-2",        "t61/2-6 s29/2"},
         { "Remplacement sur fin",                      "t61/3-7 s29/2 t61/8 s29/1", "t61/2-7 s29/2",   "t61/2-7 s29/2 t61/8 s29/1",          "t61/3-7 s29/2"},
         { "Remplacement sur fin2",                     "t61/2-4 s29/2 t61/6 s29/1", "t61/6 s29/2",     "t61/2-4 s29/2 t61/6 s29/1-2",        ""},
         { "Tordu",                                     "t61/3 s29/1 t61/4-5 s29/2", "t61/3-5 s29/3",   "t61/3 s29/1 3 t61/4-5 s29/2-3",      ""},
         { "Inter simple",                              "t61/3-5 s29/1-3",    "t61/4-8 s29/2-4", "t61/3 s29/1-3 t61/4-5 s29/1-4 t61/6-8 s29/2-4",  "t61/4-5 s29/2-3"},
         { "Inter spécial",     "t61/1 s29/1-6 t61/3-9 s29/2","t61/3 s29/5-7 t61/8 s29/1-2", "t61/1 s29/1-6 t61/3 s29/2 5-7 t61/4-7 s29/2 t61/8 s29/1-2 t61/9 s29/2","t61/8 s29/2" },
         { "Ajout en suite",                            "t61/1-4 s29/1",      "t61/5-6 s29/1",   "t61/1-6 s29/1",               "" }
   };

   private static boolean testOperationSTMoc() throws Exception { return testOperationSTMoc(-1); }
   private static boolean testOperationSTMoc(int x) throws Exception {
      title("MocTest: Test testOperationSTMoc()...");
      String listTest[][] = x== -1 ? TEST : new String[][]{ TEST[x] };
      StringBuilder s = new StringBuilder();

      try {
         int i= x==-1 ? 0 : x;
         for( String [] test : listTest ) {
            STMoc stmoc1 = new STMoc(test[1]);
            STMoc stmoc2 = new STMoc(test[2]);
            s.append("\n"+(i++)+") "+test[0]+":");
            s.append("\n A: "+stmoc1+"\n B: "+stmoc2);

            STMoc stmoc4 = stmoc1.intersection( stmoc2);
            s.append("\n Inter: "+stmoc4);
            s.append( test[4].equals(stmoc4.toString()) ? " => OK" : " => ERROR waiting: "+test[4] );

            STMoc stmoc3 = stmoc1.union( stmoc2);
            s.append("\n Union: "+stmoc3);
            s.append( test[3].equals(stmoc3.toString()) ? " => OK" : " => ERROR waiting: "+test[3] );

            STMoc stmoc5 = stmoc1.subtraction( stmoc2);
            s.append("\n subtraction A-B: "+stmoc5);
            STMoc stmoc51 = stmoc2.subtraction( stmoc1);
            s.append("\n subtraction B-A: "+stmoc51);

            STMoc stmoc6 = (STMoc) stmoc1.difference( stmoc2);
            s.append("\n difference: "+stmoc6);
            STMoc stmoc7 = stmoc3.subtraction( stmoc4 );
            s.append( stmoc7.toString().equals( stmoc6.toString() ) ? " => OK" : " => ERROR waiting: "+stmoc7.toString() );

            //               for( int j=0; j<stmoc2.range.sz; j+=2 ) {
            //                  stmoc1.range.add(stmoc2.range.r[j], stmoc2.range.r[j+1], stmoc2.range.rr[j/2]);
            //               }
            //               s.append("\n Add  : "+stmoc1);
            //               s.append( test[3].equals(stmoc1.toString()) ? " => OK" : " => ERROR waiting: "+test[3] );

            s.append("\n");
         }
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      System.out.println(s);
      boolean res = s.indexOf("ERROR")<0;
      if( res ) System.out.println("testOperationSTMoc OK");
      else System.out.println("testOperationSTMoc ERROR");
      return res;
   }

   private static void title(String s) {
      StringBuffer s1 = new StringBuffer(100);
      s1.append('\n');
      for( int i=0; i<20; i++ ) s1.append('-');
      s1.append(" "+s+" ");
      for( int i=0; i<20; i++ ) s1.append('-');
      System.out.println(s1);
   }

   class Source {
      double ra,de,rad;
      Source(double ra,double de,double rad) {
         this.ra=ra; this.de=de; this.rad=rad;
      }
   }


   // Juste pour tester
   public static void main(String[] args) {
      boolean ok=true;

      try {
         Moc.setMocOrderLogic( Moc.LOGIC_MAX );

//                  ok&=testSetMocOrder();
//                  ok&=testBasicSTMoc();
         //         ok&=testIteratorSTMoc();
         //         ok&=testOperationSTMoc();
         //         
         //         ok&=testFITS();
         //         ok&=testFITSTMoc();
         //         ok&=testFITSSTMoc();
         //         
         //         ok&=testJSON();
         //         ok&=testASCII();
         //         ok&=testSTRING();
                  ok&=testBasic();
         //         ok&=testBasicTMoc();
         //         ok&=testAdvancedTMoc();
         //         ok&=testSetLimitOrder();
         //         ok&=testIterativeInsertion();
         //         ok&=testIterativeInsertionTmoc();
         //         ok&=testOperation(); 
         //         ok&=testIsIntersecting();
         //         ok&=testisIncluding(); 
         //         ok&=testInclusive();
         //         ok&=testRange();
         //         ok&=testContains();
         //         ok&=testCoverage();
         //         ok&=testisEmptyOrFull();
         //         ok&=testSTMocExtraction();
         //         ok&=testSyscompatibility();
         //         ok&=testHashCode();
         //         ok&=testComplement();
//         ok&=testDegrade();
         //         
         //         
         //         testSpeedSTMoc();


         if( ok ) System.out.println("-------------- All is fine  -----------");
         else System.out.println("-------------- There is a problem  -----------");
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }



}
