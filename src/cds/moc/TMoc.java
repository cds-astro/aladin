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

package cds.moc;

import java.io.OutputStream;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cds.aladin.Coord;

/**
 * Extension of MOC principle to temporal axis. 
 * Convention:  1 seconde in JD barycentric is coded in 1 MOC cell level 29
 * @author Pierre Fernique [ CDS]
 * @version 1.0 March 2018 - creation
 *
 */
public class TMoc extends SMoc {
   
   static public final double DAYMICROSEC = 86400000000.;
   static public final double MAXDAY = ( 1L<<(MAXORDER*2) ) / 86400L;
   
   
   public TMoc() { this(-1); }
   public TMoc( int mocOrder) {
      init("JD",0,mocOrder);
      range = new Range(1024);
   }
   public TMoc(String s) throws Exception {
      this();
      add(s);
   }
   public TMoc(Range range) throws Exception { super(range); }
   
   protected void initPropSys(String sys) { property.put("TIMESYS",sys); }
   
   /** Compute the microsec from JD=0 from a date jd (in JD unit=day decimal) and an offset (in JD unit=day) */
   static public long getMicrosec(double jd, long offset) {
      long micron = (long)(jd*DAYMICROSEC);
      return micron + (offset*86400000000L);
   }
   
   /** Add JD range
    * @param jdmin start time (in JD - unit=day) - included in the range
    * @param jdmax end time (in JD - unit=day) - included in the range
    */
   public void add(double jdmin, double jdmax) {
      long min = (long)(jdmin*DAYMICROSEC);
      long max = (long)(jdmax*DAYMICROSEC);
      add( min,max);
   }
   
   /** Deep copy */
   public Moc clone() {
      TMoc moc = new TMoc();
      return clone1(moc);
   }
   
   /** Retourne la composante temporelle du MOC */
   public TMoc getTimeMoc() throws Exception { return this; }
   
   /** Retourne la composante spatiale du MOC */
   public SMoc getSpaceMoc() throws Exception { throw new Exception("No spatial dimension"); }
   
   /** Retourne la composante en énergie du MOC */
   public EMoc getEnergyMoc() throws Exception { throw new Exception("No energy dimension"); }
   
   public int getSpaceOrder() { return -1; }  // No space dimension
   public int getTimeOrder() { return getMocOrder(); }
   
   public void setSpaceOrder(int order) throws Exception { throw new Exception("No spatial dimension"); }
   public void setTimeOrder(int order) throws Exception { setMocOrder(order); }
   
   public boolean isSpace()   { return false; }
   public boolean isTime()    { return true; }
   public boolean isEnergy()  { return false; }

   public Moc union(Moc moc) throws Exception {
      return operation(moc.getTimeMoc(),0);
   }
   public Moc intersection(Moc moc) throws Exception {
      return operation(moc.getTimeMoc(),1);
   }
   public Moc subtraction(Moc moc) throws Exception {
      return operation(moc.getTimeMoc(),2);
   }

   public Moc difference(Moc moc) throws Exception {
      TMoc m = moc.getTimeMoc();
      Moc inter = intersection(m);
      Moc union = union(m);
      return union.subtraction(inter);
   }
   
   public TMoc complement() throws Exception {
      TMoc allsky = new TMoc();
      allsky.add("0/0");
      allsky.toRangeSet();
      toRangeSet();
      TMoc res = new TMoc(mocOrder);
      res.range = allsky.range.difference(range);
      res.toMocSet();
      return res;
   }
   
   public void accretion() throws Exception { accretion( getTimeOrder() ); }
   public void accretion(int order) throws Exception {
      toRangeSet();
      Range r = new Range(range.sz);
      int shift = (MAXORDER-order)*2;
      long add = 1L<<shift;
      for( int i=0; i<range.sz; i+=2 ) {
         long a = range.r[i] -add;
         long b = range.r[i+1] +add;
         r.add(a,b);
      }
      range = r;
      toMocSet();
  }
  
   /** True if the jd date is in TMOC */
   public  boolean contains(double jd) {
      long npix = (long)( jd * DAYMICROSEC );
      toRangeSet();
      return range.contains(npix);
   }

   // Throw an exception if the coordsys of the parameter moc differs of the coordsys
   protected void testCompatibility(SMoc moc) throws Exception {
      if( !(moc instanceof TMoc) ) throw new Exception("Incompatible => not a TMOC");
   }
   
   /** Return minimal time in JD - -1 if empty*/
   public double getTimeMin() {
      if( isEmpty() ) return -1;
      toRangeSet();
      return range.begins(0) / DAYMICROSEC;
   }
   
   
   /** Return maximal time in JD - -1 if empty*/
   public double getTimeMax() {
      if( isEmpty() ) return -1;
      toRangeSet();
      return range.ends( range.nranges()-1 ) / DAYMICROSEC;
   }
   
   /** Return JD time for a couple order/npix */
   public static double getTime(int order, long npix) {
      int shift = 2*(MAXORDER - order);
      long t = npix<<shift;
      return t/DAYMICROSEC;
   }
   
   /** Return the duration of a cell for a specifical order (in microsec) */
   public static long getDuration(int order) {
      int shift = 2*(MAXORDER - order);
      return 1L<<shift;
   }
   
   /** Returns a rangeIterator, which iterates over all individual range
    * @param jdStart JD start time
    * @param jdStop JD end time
    * @return iterator of range in microseconds
    */
   public Iterator<long[]> jdIterator(double jdStart, double jdStop) {
      if( jdStart>jdStop ) throw new InputMismatchException();
      toRangeSet();
      return new JDIterator((long)(jdStart*DAYMICROSEC),(long)(jdStop*DAYMICROSEC));
   }
   
   class JDIterator implements Iterator<long[]>{
      int pos, endpos;
      
      JDIterator(long start, long end) {
         pos = range.indexOf(start)/2;;
         if( pos<0 ) pos=0;
         endpos = range.indexOf(end)/2+1;
      }
      
      public boolean hasNext() { return pos<endpos; }

      public long [] next() {
         if( pos>endpos ) throw new NoSuchElementException();
         long ret [] = new long[2];
         ret[0] = range.begins(pos);
         ret[1] = range.ends(pos);
         pos++;
         return ret;
      }

      public void remove() { }
   }
   
   /** Write specifif FITS keywords
    * @param out
    * @return number of written bytes
    */
  protected int writeSpecificFitsProp( OutputStream out  ) throws Exception {
      int n=0;
      out.write( MocIO.getFitsLine("MOC","TIME","Temporal MOC") );    n+=80;      
      out.write( MocIO.getFitsLine("ORDERING","NUNIQ","NUNIQ coding method") );    n+=80;      
//      out.write( MocIO.getFitsLine("TORDER",""+getMocOrder(),"Time MOC resolution (best order)") );    n+=80;      
      out.write( MocIO.getFitsLine("MOCORDER",""+getMocOrder(),"Time MOC resolution (best order)") );    n+=80;      
      out.write( MocIO.getFitsLine("TIMESYS","JD","Time ref system JD BARYCENTRIC TCB, 1 microsec order 29") ); n+=80;
      return n;
   }
   
   static public void main( String argv[] ) {
      try {
         System.out.println("  Order    Space res.   Time resolution");
         for( int i=0; i<=MAXORDER; i++ ) {
            String spaceRes = Coord.getUnit(  Math.sqrt( SMoc.getPixelArea( i ) ));
            String timeRes = getTemps(  getDuration(i) );
            
            System.out.printf("   %2d     %-10s %s\n",i,spaceRes,timeRes);
         }
//         long max = getDuration(MAXORDER) * ( 1L<<(2*MAXORDER) );
//         System.out.println("At order "+MAXORDER+" we can address "+getTemps(max)+" with a resolution of 1µs");
         
//         TMoc tmoc = new TMoc();
//         double jdmin = Astrodate.dateToJD(2000, 06, 18, 12, 00, 00);  // 18 juin 2000 à midi
//         double jdmax = Astrodate.dateToJD(2017, 12, 25, 00, 00, 00);  // Noël 2017 à minuit
//         tmoc.add(jdmin, jdmax);
//         tmoc.toHealpixMoc();
//         System.out.println("Moc="+tmoc);
//         System.out.println("min="+Astrodate.JDToDate( tmoc.getTimeMin()));
//         System.out.println("max="+Astrodate.JDToDate( tmoc.getTimeMax()));
//         System.out.println("duration="+getTemps( tmoc.getUsedArea() ));
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   
   static private long Y =  (long)(365.25*86400) * 1000000L;
   static private long D =  86400000000L;
   static private long H =  3600000000L;
   static private long M =  60000000L;
   static private long S =  1000000L;
   static private long MS = 1000L;
   
   /** retourne un temps exprimé en ms sous une forme lisible 3j 5h 10mn 3.101s */
   static public String getTemps(long microsec) { return getTemps(microsec,false); }
   static public String getTemps(long microsec, boolean flagRound) { 

      StringBuilder s = new StringBuilder();
      if( microsec<MS ) s.append( microsec+"µs");
      else if( microsec<S ) s.append( microsec/(double)MS+"ms");
      else {
         long y=-1,j=-1,h=-1,m=-1;
         if( microsec>Y ) { y = microsec/Y; microsec -= y*Y; s.append(y+"y"); }
         if( microsec>D ) { j = microsec/D; microsec -= j*D; if( s.length()>0 ) s.append(' '); s.append(j+"d"); }
         if( flagRound && y!=-1 && y>1 ) return s.toString();
         if( microsec>H ) { h = microsec/H; microsec -= h*H; if( s.length()>0 ) s.append(' '); s.append(h+"h"); }
         if( flagRound && j!=-1 && j>1 ) return s.toString();
         if( microsec>M ) { m = microsec/M; microsec -= m*M; if( s.length()>0 ) s.append(' '); s.append(m+"m"); }
         if( flagRound && h!=-1 && h>1 ) return s.toString();
         if( microsec>0 ) { if( s.length()>0 ) s.append(' '); s.append( microsec/(double)S+"s"); }
      }
      return s.toString();
   }


   
}
