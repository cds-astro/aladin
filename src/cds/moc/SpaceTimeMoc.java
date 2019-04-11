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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class SpaceTimeMoc extends Moc {
   
   public Range2 timeRange;
   private int timeOrder = MAXORDER;
   private int spaceOrder = MAXORDER;

   public SpaceTimeMoc() {
      init();
      timeRange = new Range2();
   }

   public SpaceTimeMoc( int timeOrder, int spaceOrder ) {
      this();
      this.timeOrder=timeOrder;
      this.spaceOrder=spaceOrder;
   }

   public SpaceTimeMoc(Range2 rangeSet) {
      init();
      this.timeRange = rangeSet;
   }
   
   public SpaceTimeMoc(String s)  throws Exception {
      this();
      if( s!=null && s.length()>0 ) append(s);
   }
   
   private void init() {
      property = new HashMap<>();
      property.put("MOCTOOL","CDSjavaAPI-"+VERSION);
      property.put("DATE",String.format("%tFT%<tR", new Date()));
   }

   @Override
   public void clear() { timeRange.clear(); }
   
   public void setTimeOrder(int order) throws Exception { timeOrder=order; }
   public int getTimeOrder() { return timeOrder; }

   public void setSpaceOrder(int order) throws Exception { spaceOrder=order; }
   public int getSpaceOrder() { return spaceOrder; }
   
   
   public void setMocOrder(int order) throws Exception { setSpaceOrder(order); }
   public int getMocOrder() { return getSpaceOrder(); }

   @Override
   public long getMem() { return timeRange.getMem()+20L; }

   @Override
   public int getSize() {
      // les indices temporels
      int size=timeRange.sz;   
      // Les indices spatiaux
      for( int i=0; i<timeRange.sz/2; i++ ) size += timeRange.rangeArray[i].sz;
      return size;
   }
   
   public int getTimeRanges() { return timeRange.nranges(); }

   @Override
   public void add(String s) throws Exception {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void add(Moc moc) throws Exception {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void check() throws Exception {
      // TODO Auto-generated method stub
   }

   @Override
   public void setProperty(String key, String value) throws Exception {
      // TODO Auto-generated method stub
      
   }

   @Override
   public String toASCII() throws Exception {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isIntersecting(Moc moc) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public Moc union(Moc moc) throws Exception {
      return new SpaceTimeMoc( timeRange.union( ((SpaceTimeMoc)moc).timeRange ));
   }

   @Override
   public Moc intersection(Moc moc) throws Exception {
      return new SpaceTimeMoc( timeRange.intersection( ((SpaceTimeMoc)moc).timeRange ));
   }
   
   @Override
   public Moc subtraction(Moc moc) throws Exception {
      return new SpaceTimeMoc( timeRange.difference( ((SpaceTimeMoc)moc).timeRange ));
   }

   @Override
   public boolean isEmpty() {
      return timeRange.isEmpty();
   }

   @Override
   public void trim() {
      // TODO Auto-generated method stub
      
   }

   @Override
   public Iterator<MocCell> iterator() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int getSize(int order) {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public Array getArray(int order) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setCurrentOrder(int order) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void setCoordSys(String s) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void addHpix(String s) throws Exception {
      // TODO Auto-generated method stub
      
   }
   
   public void add(long tmin, long tmax, long smin, long smax) {
      
      // Changement de résolution temporelle ?
      int deltaTimeOrder = (MAXORDER - timeOrder)<<1;
      if( deltaTimeOrder>0 ) {
         tmin = (tmin>>>deltaTimeOrder) << deltaTimeOrder;
         tmax = (tmax>>>deltaTimeOrder) << deltaTimeOrder;
         if( tmin==tmax ) tmax++;
      }
      
   // Changement de résolution spatiale ?
      int deltaSpaceOrder = (MAXORDER - spaceOrder)<<1;
      if( deltaSpaceOrder>0 ) {
         smin = (smin>>>deltaSpaceOrder) << deltaSpaceOrder;
         smax = (smax>>>deltaSpaceOrder) << deltaSpaceOrder;
         if( smin==smax ) smax++;
      }
      
      // Moc Spatial réduit à un intervalle spatial
      Range r = new Range();
      r.append(smin,smax);
      
      // Ajout de la cellule spatio-temporelle
      timeRange.add(tmin,tmax,r);
   }

   @Override
   public boolean add(int order, long npix) throws Exception {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public int getMaxOrder() {
      return getSpaceOrder();
   }

   @Override
   public String getCoordSys() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setCheckConsistencyFlag(boolean flag) throws Exception {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void toHealpixMoc() throws Exception {
      // TODO Auto-generated method stub
      
   }
   
   public void append(String s1) throws Exception {
      if( s1.charAt(0)!='t' ) throw new Exception("Invalid STMOC syntax (expecting 't')");
      
      StringTokenizer tok = new StringTokenizer(s1,"t");
      while( tok.hasMoreTokens() ) {
         String s="t"+tok.nextToken();
//         System.out.println("["+s+"]");
         int e = s.indexOf('s');
         HealpixMoc moc=null;
         HealpixMoc tmoc=null;

         // Ai-je un SMOC associé au TMOC ?
         if( e>0 ) {

            // Les orders sont-il indiqués ?
            int se = s.indexOf('/',e);

            // Je crée le MOC juste pour récupérer le rangeSet
            moc = new HealpixMoc( (se<0?"29/":"")+s.substring(e+1));
            moc.toRangeSet();

         } else e=s.length();

         // Les orders sont-ils indiqués ?
         int st = s.lastIndexOf('/',e-1);

         // Je crée le MOC juste pour récupérer le rangeSet
         tmoc = new HealpixMoc( (st<0?"29/":"")+s.substring(1,e));
         tmoc.toRangeSet();

         // J'insère chaque élément 
         for( int i=0; i<tmoc.spaceRange.sz; i+=2 ) {
            timeRange.append(tmoc.spaceRange.r[i], tmoc.spaceRange.r[i+1], moc.spaceRange);
         }
      }
   }
   
   public String toString() { return toString(false); }
   public String toString( boolean flagNL ) {
      StringBuilder s = new StringBuilder();
      for( int i=0; i<timeRange.sz; i+=2 ) {
         long a=timeRange.r[i];
         long b=timeRange.r[i+1];
         s.append("t"+a);
         if( b-1L!=a) s.append("-"+(b-1L));
         Range m = timeRange.rangeArray[i>>>1];
         if( m!=null && !m.isEmpty() ) {
            s.append( flagNL ? " s":"s");
            
            if( flagNL ) {
               HealpixMoc moc;
               try {
                  moc = new HealpixMoc(m);
                  s.append(moc.toASCII());
               } catch( Exception e ) {  e.printStackTrace(); }

            } else {

               for( int j=0; j<m.sz; j+=2 ) {
                  s.append(m.r[j]);
                  if( m.r[j+1]-1!=m.r[j] ) s.append("-"+(m.r[j+1]-1));
                  if( j<m.sz-2 ) s.append(',');
               }
            }
         }
         if( i<timeRange.sz-2 ) s.append(flagNL ?'\n':' ');
      }
      return s.toString();
   }
   
   public String toString2() {
      StringBuilder s = new StringBuilder();
      for( int i=0; i<timeRange.sz; i+=2 ) {
         s.append("t"+timeRange.r[i]);
         if( timeRange.r[i+1]-1!=timeRange.r[i]) s.append("-"+(timeRange.r[i+1]-1));
         Range m = timeRange.rangeArray[i>>>1];
         if( m!=null && !m.isEmpty() ) {
            s.append(" s");
            HealpixMoc moc;
            try {
               moc = new HealpixMoc(m);
               s.append(moc.toASCII());
            } catch( Exception e ) {  e.printStackTrace(); }
         }
         if( i<timeRange.sz-2 ) s.append('\n');
      }
      return s.toString();
   }
   
   public String toString3() {
      StringBuilder s = new StringBuilder();
      HealpixMoc moc;
      for( int i=0; i<timeRange.sz; i+=2 ) {
         s.append("t");
         try {
            moc = new HealpixMoc(timeRange);
            s.append(moc.toASCII());
         } catch( Exception e ) {  e.printStackTrace(); }
         Range m = timeRange.rangeArray[i>>>1];
         if( m!=null && !m.isEmpty() ) {
            s.append(" s");
            try {
               moc = new HealpixMoc(m);
               s.append(moc.toASCII());
            } catch( Exception e ) {  e.printStackTrace(); }
         }
         if( i<timeRange.sz-2 ) s.append('\n');
      }
      return s.toString();
   }

   
   
   static final private String TEST[][] = {
      { "Ajout à vide",                              "",           "t5-10s2",   "t5-10s2" ,             ""},
      { "Ajout singleton derrière singleton",        "t4s1",       "t5s2",      "t4s1 t5s2",            ""},
      { "Ajout singleton avant singleton",           "t5s2",       "t4s1",      "t4s1 t5s2",            ""},
      { "Ajout intervalle entrelacés après",         "t4-6s1",     "t5-8s2",    "t4s1 t5-6s1-2 t7-8s2", ""},
      { "Ajout intervalle entrelacés avant",         "t5-8s2",     "t4-6s1",    "t4s1 t5-6s1-2 t7-8s2", ""},
      { "Ajout intervalle englobant (s différents)", "t2-6s2",     "t1-8s1",    "t1s1 t2-6s1-2 t7-8s1", ""},
      { "Ajout intervalle englobant (s identiques)", "t2-6s2",     "t1-8s2",    "t1-8s2",               "t2-6s2"},
      { "Ajout intervalle interne (s différents)",   "t1-8s1",     "t2-6s2",    "t1s1 t2-6s1-2 t7-8s1", ""},
      { "Ajout intervalle interne (s identiques)",   "t1-8s2",     "t2-6s2",    "t1-8s2" ,              "t2-6s2"},
      { "Intercallage",                              "t6-7 11s1",  "t9s2",      "t6-7s1 t9s2 t11s1",    ""},
      { "Fusion différents s",                       "t2-6 8-9s2", "t7s1",      "t2-6s2 t7s1 t8-9s2",   ""},
      { "Fusion indentiques s",                      "t2-6 8-9s2", "t7s2",      "t2-9s2",               ""},
      { "Remplacement sur début",                    "t2-6s2 t7s1", "t2-7s2",   "t2-6s2 t7s1-2",        "t2-6s2"},
      { "Remplacement sur fin",                      "t3-7s2 t8s1", "t2-7s2",   "t2-7s2 t8s1",          "t3-7s2"},
      { "Remplacement sur fin2",                     "t2-4s2 t6s1", "t6s2",     "t2-4s2 t6s1-2",        ""},
      { "Tordu",                                     "t3s1 t4-5s2", "t3-5s3",   "t3s1,3 t4-5s2-3",      ""},
      { "Inter simple",                              "t3-5s1-3",    "t4-8s2-4", "t3s1-3 t4-5s1-4 t6-8s2-4",  "t4-5s2-3"},
      { "Inter spécial",  "t1s1-6 t3-9s2","t3s5-7 t8s1-2", "t1s1-6 t3s2,5-7 t4-7s2 t8s1-2 t9s2","t8s2" }
   };
   
   static final void test() throws Exception { test(-1); }
   static final void test(int x) throws Exception {
      String listTest[][] = x== -1 ? TEST : new String[][]{ TEST[x] };
      StringBuilder s = new StringBuilder();

      int i= x==-1 ? 0 : x;
      for( String [] test : listTest ) {
         SpaceTimeMoc stmoc1 = new SpaceTimeMoc(test[1]);
         SpaceTimeMoc stmoc2 = new SpaceTimeMoc(test[2]);
         s.append("\n"+(i++)+") "+test[0]+":");
         s.append("\n A: "+stmoc1+"\n B: "+stmoc2);

         SpaceTimeMoc stmoc4 = (SpaceTimeMoc) stmoc1.intersection( stmoc2);
         s.append("\n Inter: "+stmoc4);
         s.append( test[4].equals(stmoc4.toString()) ? " => OK" : " => ERROR waiting: "+test[4] );
        
         SpaceTimeMoc stmoc3 = (SpaceTimeMoc) stmoc1.union( stmoc2);
         s.append("\n Union: "+stmoc3);
         s.append( test[3].equals(stmoc3.toString()) ? " => OK" : " => ERROR waiting: "+test[3] );
         
         stmoc1.timeRange.add(stmoc2.timeRange.r[0], stmoc2.timeRange.r[1], stmoc2.timeRange.rangeArray[0]);
         s.append("\n Add  : "+stmoc1);
         s.append( test[3].equals(stmoc1.toString()) ? " => OK" : " => ERROR waiting: "+test[3] );

         s.append("\n");
      }

      System.out.println(s);
   }

   /** Write specifif FITS keywords
    * @param out
    * @return number of written bytes
    */
  protected int writeSpecificFitsProp( OutputStream out  ) throws Exception {
      int n=0;
      out.write( MocIO.getFitsLine("MOC","SPACETIME","Space Time MOC") );    n+=80;      
      out.write( MocIO.getFitsLine("ORDERING","RANGE29","Range coding") );    n+=80;      
      out.write( MocIO.getFitsLine("MOCORDER",""+getMocOrder(),"Space MOC resolution") );    n+=80;      
      out.write( MocIO.getFitsLine("TORDER",""+getTimeOrder(),"Time MOC resolution") );    n+=80;      
      out.write( MocIO.getFitsLine("COORDSYS","C","Space reference frame (C=ICRS)") );  n+=80;
      out.write( MocIO.getFitsLine("TIMESYS","JD","Time ref system JD BARYCENTRIC TCB, 1 microsec order 29") ); n+=80;
      return n;
   }
   
  // Write the Data FITS HDU (time ranges followed by space ranges, and so on)
  protected int writeSpecificData(OutputStream out) throws Exception {
     byte [] buf = new byte[ 8 ];
     int size = 0;
     for( int i=0; i<timeRange.sz; i+=2 ) {
        long tmin = -timeRange.r[i];
        size+=MocIO.writeVal(out,tmin,buf);
        long tmax = -timeRange.r[i+1];
        size+=MocIO.writeVal(out,tmax,buf);
        
        Range m = timeRange.rangeArray[i>>>1];
        for( int j=0; j<m.sz; j+=2 ) {
           long smin = m.r[j];
           size+=MocIO.writeVal(out,smin,buf);
           long smax = m.r[j+1];
           size+=MocIO.writeVal(out,smax,buf);
        }
     }
     return size;
  }
  
  protected void readSpecificData( InputStream in, int naxis1, int naxis2, int nbyte) throws Exception {
     byte [] buf = new byte[naxis1*naxis2];
     MocIO.readFully(in,buf);
     createMocByFits((naxis1*naxis2)/nbyte,buf);
  }
  
  protected long readLong(byte t[], int i) {
     int a = ((  t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF;
     int b = ((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF;
     long val = (((long)a)<<32) | (b & 0xFFFFFFFFL);
     return val;
  }
  
  protected void createMocByFits(int nval,byte [] t) throws Exception {
     int i=0;
     int startT = 0;
     Range m = new Range(100000);
     
     // Lecture range par range (2 indices simultanément)
     for( int k=0; k<nval; k+=2, i+=16 ) {
        
        long min = readLong(t,i);
        long max = readLong(t,i+8);
        
        // Liste temporelle
        if( min<0 ) {
           
           // Affectations des précédents indices spatiaux aux intervalles temporelles correspondants
           if( !m.isEmpty() ) {
              for( int j = startT; j<timeRange.sz; j+=2 )  timeRange.rangeArray[j>>>1] = new Range(m);
              
              // On prépare le range pour la prochaine liste spatiale
              m.clear();
              startT=timeRange.sz;
           }
           
           // Mémorisation du range temporel
           timeRange.append( -min, -max, null);

        // Liste spatiale
        } else m.append(min,max);
     }
     
     // Dernier "flush"
     if( !m.isEmpty() ) {
        for( int j = startT; j<timeRange.sz; j+=2 )  timeRange.rangeArray[j>>>1] = m;
     }
   }
  
   protected int getType() { return SpaceMoc.LONG; }
  
   /** Return minimal time in JD - -1 if empty*/
   public double getTimeMin() {
      if( isEmpty() ) return -1;
      return timeRange.begins(0) / TimeMoc.DAYMICROSEC;
   }


   /** Return maximal time in JD - -1 if empty*/
   public double getTimeMax() {
      if( isEmpty() ) return -1;
      return timeRange.ends( timeRange.nranges()-1 ) / TimeMoc.DAYMICROSEC;
   }
   
   /** TimeMoc from the whole STMOC */
   public TimeMoc getTimeMoc() throws Exception {
      TimeMoc moc = new TimeMoc();
      moc.spaceRange = timeRange;
      moc.toHealpixMoc();
      return moc;
   }

   /** TimeMoc from the intersection with the spaceMoc */
   public TimeMoc getTimeMoc( SpaceMoc spaceMoc) throws Exception {
      if( spaceMoc==null || spaceMoc.isEmpty() ) return getTimeMoc();
      TimeMoc moc = new TimeMoc();
      Range range = new Range();
      spaceMoc.toRangeSet();
      
      for( int i=0; i<timeRange.sz; i+=2 ) {
         Range m = timeRange.rangeArray[i>>>1];
         if( spaceMoc.spaceRange.overlaps(m) ) range.append( timeRange.r[i], timeRange.r[i+1] );
      }
      
      moc.spaceRange = range;
      moc.toHealpixMoc();
      return moc;
   }

   /** SpaceMoc from the whole STMOC */
   public SpaceMoc getSpaceMoc() throws Exception { return getSpaceMoc(-1,Long.MAX_VALUE); }
   
   /** SpaceMoc extraction from a temporal time
    * @param tmin  min of range (order 29)
    * @param tmax max of range (included - order 29)
    */
   public SpaceMoc getSpaceMoc(long tmin,long tmax) throws Exception {
      SpaceMoc moc = new SpaceMoc();
      int pos = timeRange.indexOf(tmin);
      if( (pos&1)==1 ) pos++;
      
      // On remplit un tableau de chaque intervalle spatial concerné (à la queue leu leu)
      long [] buf = new long[ getSize() ];
      int size=0;
      for( int i=pos; i<timeRange.sz; i+=2 ) {
         if( timeRange.r[i+1]>tmax ) break;
         Range m = timeRange.rangeArray[i>>>1];
         for( int j=0; j<m.sz; j++ ) buf[size++] = m.r[j];
      }
      
      // On en fait un range qui met en forme proprement
      Range range = new Range(buf,size);
      range.sortAndFix();
      
      // On l'utilise pour en faire un MOC
      moc.spaceRange = range;
      moc.toHealpixMoc();
      return moc;
   }
   
   static public void main(String a[] ) {
      try {
         
//         test();
                
//         SpaceTimeMoc stmoc1 = new SpaceTimeMoc("t3-5s3-10 t4-6s1-9 t7s3-12 t10s15-18");
//         stmoc1.setTimeOrder(14);
//         stmoc1.setMocOrder(13);
//         System.out.println("\nA:\n"+stmoc1);
//         System.out.println();
//         SpaceMoc moc = stmoc1.getSpaceMoc();
//         System.out.println("\nB:\n"+moc);
         
         SpaceTimeMoc stmoc1 = new SpaceTimeMoc(TEST[17][1]);
         System.out.println("\nA:\n"+stmoc1);
         System.out.println();
         
//         for( int i=0; i<stmoc1.timeRange.size; i++ ) System.out.println("range["+i+"]="+stmoc1.timeRange.range[i]);
//         System.out.println();
//         
//         for( int i=(int)stmoc1.timeRange.range[0]-1; i<stmoc1.timeRange.range[stmoc1.timeRange.size-1]+1; i++ ) {
//            int j = stmoc1.timeRange.indexOf(i);
//            boolean avant = j<0;
//            boolean apres = j==stmoc1.timeRange.size-1;
//            boolean in = (j&1)==0;
//            String vals = avant ? "out .."+stmoc1.timeRange.range[j+1]+"[" :
//                          apres ? "out ["+stmoc1.timeRange.range[j]+".." :
//                          in    ? "in ["+stmoc1.timeRange.range[j]+".."+stmoc1.timeRange.range[j+1]+"[" :
//                                  "out ["+stmoc1.timeRange.range[j]+".."+stmoc1.timeRange.range[j+1]+"[" ;
//            System.out.println("i="+i+" indice="+j+ " ==> "+vals);
//         }
//
//         System.out.println("\nAjout:"+TEST[17][2]);
         
         test(17);
//         
//         stmoc1.write( "D:/STMoc.fits");
//         stmoc1.read("D:/STMoc.fits");
//         System.out.println("Resulat ecriture/lecture:\n"+stmoc1);
         
     } catch( Exception e ) { e.printStackTrace(); }
   }

   @Override
   public Moc clone() {
      // TODO Auto-generated method stub
      return null;
   }


   
}
