// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.moc;

import java.io.OutputStream;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cds.tools.Astrodate;

/**
 * Extension of MOC principle to temporal axis. 
 * Convention:  1 seconde in JD barycentric is coded in 1 MOC cell level 29
 * @author Pierre Fernique [ CDS]
 * @version 1.0 March 2018 - creation
 *
 */
public class TimeMoc extends SpaceMoc {
   
   static public final double DAYMICROSEC = 86400000000.;
   static public final double MAXDAY = ( 1L<<(MAXORDER*2) ) / 86400L;
   
   
   public TimeMoc() { this(-1); }
   public TimeMoc( int mocOrder) {
      init("JD",0,mocOrder);
      spaceRange = new Range(1024);
   }
   public TimeMoc(Range range) throws Exception { super(range); }
   
   /** Add JD range
    * @param jdmin start time (in JD - unit=day) - included in the range
    * @param jdmax end time (in JD - unit=day) - included in the range
    */
   public void add(double jdmin, double jdmax) {
      long min = (long)(jdmin*DAYMICROSEC);
      long max = (long)(jdmax*DAYMICROSEC)+1L;
      Range rtmp=new Range();
      rtmp.append(min,max);
      if( !rtmp.isEmpty() ) spaceRange=spaceRange.union(rtmp);
   }
   
   /** Deep copy */
   public Moc clone() {
      TimeMoc moc = new TimeMoc();
      return clone1(moc);
   }
   
   public TimeMoc complement() throws Exception {
      TimeMoc allsky = new TimeMoc();
      allsky.add("0/0");
      allsky.toRangeSet();
      toRangeSet();
      TimeMoc res = new TimeMoc(maxLimitOrder);
      res.spaceRange = allsky.spaceRange.difference(spaceRange);
      res.toHealpixMoc();
      return res;
   }

   
   // Generic operation
   protected SpaceMoc operation(SpaceMoc moc,int op) throws Exception {
      testCompatibility(moc);
      toRangeSet();
      moc.toRangeSet();
      SpaceMoc res = new TimeMoc();
      switch(op) {
         case 0 : res.spaceRange = spaceRange.union(moc.spaceRange); break;
         case 1 : res.spaceRange = spaceRange.intersection(moc.spaceRange); break;
         case 2 : res.spaceRange = spaceRange.difference(moc.spaceRange); break;
      }
      res.toHealpixMoc();
      return res;
   }
   
   /** True if the jd date is in TMOC */
   public  boolean contains(double jd) {
      long npix = (long)( jd * DAYMICROSEC );
      toRangeSet();
      return spaceRange.contains(npix);
   }

   // Throw an exception if the coordsys of the parameter moc differs of the coordsys
   protected void testCompatibility(SpaceMoc moc) throws Exception {
      if( !(moc instanceof TimeMoc) ) throw new Exception("Incompatible => not a TMOC");
   }
   
   /** Return minimal time in JD - -1 if empty*/
   public double getTimeMin() {
      if( isEmpty() ) return -1;
      toRangeSet();
      return spaceRange.begins(0) / DAYMICROSEC;
   }
   
   
   /** Return maximal time in JD - -1 if empty*/
   public double getTimeMax() {
      if( isEmpty() ) return -1;
      toRangeSet();
      return spaceRange.ends( spaceRange.nranges()-1 ) / DAYMICROSEC;
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
         pos = spaceRange.indexOf(start)/2;;
         if( pos<0 ) pos=0;
         endpos = spaceRange.indexOf(end)/2+1;
      }
      
      public boolean hasNext() { return pos<endpos; }

      public long [] next() {
         if( pos>endpos ) throw new NoSuchElementException();
         long ret [] = new long[2];
         ret[0] = spaceRange.begins(pos);
         ret[1] = spaceRange.ends(pos);
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
         for( int i=0; i<=MAXORDER; i++ ) {
            long d = getDuration(i);
            System.out.println("order "+i+" cellRes="+ d+"µs => "+getTemps(d));
         }
         long max = getDuration(MAXORDER) * ( 1L<<(2*MAXORDER) );
         System.out.println("At order "+MAXORDER+" we can address "+getTemps(max)+" with a resolution of 1µs");
         
         TimeMoc tmoc = new TimeMoc();
         double jdmin = Astrodate.dateToJD(2000, 06, 18, 12, 00, 00);  // 18 juin 2000 à midi
         double jdmax = Astrodate.dateToJD(2017, 12, 25, 00, 00, 00);  // Noël 2017 à minuit
         tmoc.add(jdmin, jdmax);
         tmoc.toHealpixMoc();
         System.out.println("Moc="+tmoc);
         System.out.println("min="+Astrodate.JDToDate( tmoc.getTimeMin()));
         System.out.println("max="+Astrodate.JDToDate( tmoc.getTimeMax()));
         System.out.println("duration="+getTemps( tmoc.getUsedArea() ));
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
