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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class SpaceTimeMoc extends Moc {
   
   public Range2 timeRange;
   private int timeOrder = -1;
   private int spaceOrder = -1;

   public SpaceTimeMoc() {
      init();
      timeRange = new Range2();
   }

   public SpaceTimeMoc( int spaceOrder , int timeOrder ) {
      this();
      this.timeOrder=timeOrder;
      this.spaceOrder=spaceOrder;
   }

   public SpaceTimeMoc(int spaceOrder , int timeOrder ,Range2 rangeSet) {
      init();
      this.spaceOrder=spaceOrder;
      this.timeOrder=timeOrder;
      this.timeRange = new Range2(rangeSet);
   }
   
   public SpaceTimeMoc(String s)  throws Exception {
      this();
      if( s!=null && s.length()>0 ) append(s);
      else timeOrder=spaceOrder=MAXORDER;
   }
   
   private void init() {
      property = new HashMap<>();
      property.put("MOCTOOL","CDSjavaAPI-"+VERSION);
      property.put("DATE",String.format("%tFT%<tR", new Date()));
   }
   
   /** True if the npix (deepest level) and jd date is in the SpaceTimeMOC */
   public  boolean contains(long npix, double jd) {
      long npixTime = (long)( jd * TimeMoc.DAYMICROSEC );
      if( !timeRange.contains(npixTime) ) return false;
      for( Range range : timeRange.rangeArray ) {
         if( range.contains(npix) ) return true;
      }
      return false;
   }

   @Override
   public void clear() { timeRange.clear(); }
   
   public void setTimeOrder(int order) throws Exception {
      if( order<timeOrder ) degradeTimeOrder(order);
      timeOrder=order;
   }
   public int getTimeOrder() { return timeOrder; }

   public void setSpaceOrder(int order) throws Exception { 
      if( order<spaceOrder ) degradeSpaceOrder(order);
      spaceOrder=order; 
   }
   public int getSpaceOrder() { return spaceOrder; }
   
   private void degradeSpaceOrder(int order) { degradeOrder(-1,order); }
   private void degradeTimeOrder(int order)  { degradeOrder(order,-1); }
   
   private void degradeOrder(int timeOrder, int spaceOrder) {
      int shift1 = timeOrder==-1  ? 0 : (MAXORDER - timeOrder  )*2;
      int shift2 = spaceOrder==-1 ? 0 : (MAXORDER - spaceOrder )*2;
      timeRange = timeRange.degrade(shift1, shift2);
   }
   
   public void setMocOrder(int order) throws Exception { setTimeOrder(order); }
   public int getMocOrder() { try{ return getTimeOrder(); } catch( Exception e ) {} return -1; }

   @Override
   public long getMem() { return timeRange.getMem()+20L; }

   @Override
   public int getSize() {
      // les indices temporels
      int size=timeRange.sz;   
      // Les indices spatiaux
      for( int i=timeRange.sz/2 -1; i>=0; i-- ) size += timeRange.rangeArray[i].sz;
      return size;
   }
   
   public int getTimeRanges() { return timeRange.nranges(); }

   @Override
   public void add(String s) throws Exception {
   }

   @Override
   public void add(Moc moc) throws Exception {
   }
   
   @Override
   public void check() throws Exception {
   }

   @Override
   public void setProperty(String key, String value) throws Exception {
   }

   @Override
   public boolean isIntersecting(Moc moc) {
      return false;
   }

   private int maxT(Moc moc) { 
      int o = moc.getTimeOrder();
      return timeOrder<o ? o : timeOrder;
   }
   
   private int maxS(Moc moc) { 
      int o = moc.getSpaceOrder();
      return spaceOrder<o ? o : spaceOrder;
   }
   
   @Override
   public Moc union(Moc moc) throws Exception {
      return new SpaceTimeMoc( maxS(moc), maxT(moc), timeRange.union( ((SpaceTimeMoc)moc).timeRange ));
   }

   @Override
   public Moc intersection(Moc moc) throws Exception {
      SpaceTimeMoc m;
      if( moc instanceof SpaceTimeMoc ) {
         m = (SpaceTimeMoc) moc;
      } else if( moc instanceof TimeMoc ) {
         throw new Exception("Not yet supported with TimeMoc");
      } else {
         // On crée un STMoc avec une seule plage temporaire correspondante
         // à l'étendue maximale du temps du Moc avec lequel on veut faire l'intersection
         Range2 r = new Range2(2);
         r.add( timeRange.r[0], timeRange.r[ timeRange.sz-1 ], moc.getRange() );
         m = new SpaceTimeMoc( moc.getMocOrder(), getMocOrder(), r);
      }
      return new SpaceTimeMoc( maxS(m), maxT(m), timeRange.intersection( m.timeRange ));
   }
   
   @Override
   public Moc subtraction(Moc moc) throws Exception {
      return new SpaceTimeMoc( maxS(moc), maxT(moc), timeRange.difference( ((SpaceTimeMoc)moc).timeRange ));
   }
   
   public boolean isSpace() { return true; }
   public boolean isTime()  { return true; }

   @Override
   public boolean isEmpty() {
      return timeRange.isEmpty();
   }

   @Override
   public void trim() {
   }

   @Override
   public Iterator<MocCell> iterator() {
      return null;
   }

   @Override
   public int getSize(int order) {
      return 0;
   }

   @Override
   public Array getArray(int order) {
      return null;
   }

   @Override
   public void setCurrentOrder(int order) {
   }

   @Override
   public void setCoordSys(String s) {
      System.err.println("Not yet implemented");
   }
   
   
   /** Ajustement d'une valeur à l'ordre indiquée */
   public long getVal(long val, int order) {
      if( order==MAXORDER ) return val;
      int deltaOrder = (MAXORDER - order)<<1;
      val = (val>>>deltaOrder) << deltaOrder;
      return val;
   }

   public void add(long tmin, long tmax, long smin, long smax) {
      tmin = getVal(tmin, timeOrder);
      tmax = getVal(tmax, timeOrder);
      if( tmax==tmin ) tmax++;
      smin = getVal(smin, spaceOrder);
      smax = getVal(smax, spaceOrder);
      if( smax==smin ) smax++;
     
      // Moc Spatial réduit à un intervalle spatial
      Range r = new Range();
      r.append(smin,smax);
      
      // Ajout de la cellule spatio-temporelle
      timeRange.add(tmin,tmax,r);
   }
   
   public void sortAndFix() {
      timeRange.sortAndFix();
   }

   @Override
   public boolean add(int order, long npix) throws Exception {
      return false;
   }

   @Override
   public int getMaxOrder() { return -1; }

   @Override
   public String getCoordSys() {
      return null;
   }

   @Override
   public void setCheckConsistencyFlag(boolean flag) throws Exception {
   }

   @Override
   public void toHealpixMoc() throws Exception {
   }
   
   // Pour construire au fur et à mesure le STMOC via addHpix
   private StringBuilder buf=null;   // Le buffer de lecture
   private HealpixMoc tmoc=null;     // Le dernier TMOC lu lors du parsing

   @Override
   public void addHpix(String s) throws Exception {
      
      // Fin de traitement ou nouvelle dimension temporelle ?
      if( s==null || s.charAt(0)=='t' ) {
         
         // Insertion du précédent couple si existant 
         if( tmoc!=null ) {
            // Je crée le MOC spatial du contenu bufferisé, juste récupérer son rangeSet
            HealpixMoc moc = new HealpixMoc( buf.toString() );
            moc.toRangeSet();
            if( moc.getMocOrder()>spaceOrder ) spaceOrder=moc.getMocOrder();
            
           // J'insère chaque élément 
            for( int i=0; i<tmoc.spaceRange.sz; i+=2 ) {
               timeRange.append(tmoc.spaceRange.r[i], tmoc.spaceRange.r[i+1], moc.spaceRange);
            }
            
            tmoc=null;
            buf=null;
         }
         
         // Memorisation du contenu jusqu'à la dimension spatiale (sans le préfixe de la dimension)
         if( s!=null ) buf = new StringBuilder( s.substring(1) );
         
      // Nouvelle dimension spatiale ?
      } else if( s.charAt(0)=='s' ) {
         
         // Génération du rangeset temporel du contenu bufferisé
         
         tmoc = new HealpixMoc( buf.toString() );
         tmoc.toRangeSet();
         if( tmoc.getMocOrder()>timeOrder ) timeOrder=tmoc.getMocOrder();

         // Memorisation du contenu jusqu'à la dimension temporelle suivante (sans le prefixe de la dimension)
         buf = new StringBuilder( s.substring(1) );
         
      // Bufferisation du mot courant   
      } else {
         if( buf==null ) throw new Exception("Moc syntax error [token="+s+"]");
         buf.append(' ');
         buf.append(s);
      }
   }
   
   public void append(String s) throws Exception {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t{}");
      while( st.hasMoreTokens() ) {
         String s1 = st.nextToken();
         if( s1.length()==0 ) continue;
         addHpix(s1);
      }
      addHpix(null);
   }
   
//   public void append(String s1) throws Exception {
//      if( s1.charAt(0)!='t' ) throw new Exception("Invalid STMOC syntax (expecting 't')");
//      
//      StringTokenizer tok = new StringTokenizer(s1,"t");
//      while( tok.hasMoreTokens() ) {
//         String s="t"+tok.nextToken();
////         System.out.println("["+s+"]");
//         int e = s.indexOf('s');
//         HealpixMoc moc=null;
//         HealpixMoc tmoc=null;
//
//         // Ai-je un SMOC associé au TMOC ?
//         if( e>0 ) {
//
//            // Les orders sont-il indiqués ?
//            int se = s.indexOf('/',e);
//
//            // Je crée le MOC juste pour récupérer le rangeSet
//            moc = new HealpixMoc( (se<0?"29/":"")+s.substring(e+1));
//            moc.toRangeSet();
//
//         } else e=s.length();
//
//         // Les orders sont-ils indiqués ?
//         int st = s.lastIndexOf('/',e-1);
//
//         // Je crée le MOC juste pour récupérer le rangeSet
//         tmoc = new HealpixMoc( (st<0?"29/":"")+s.substring(1,e));
//         tmoc.toRangeSet();
//
//         // J'insère chaque élément 
//         for( int i=0; i<tmoc.spaceRange.sz; i+=2 ) {
//            timeRange.append(tmoc.spaceRange.r[i], tmoc.spaceRange.r[i+1], moc.spaceRange);
//         }
//      }
//   }
   
   public String toString2() { return toString2(false); }
   public String toString2( boolean flagNL ) {
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
   
   public String toString3() {
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
   
   public String toString4() {
      StringBuilder s = new StringBuilder();
      HealpixMoc moc;
      int shift = (Moc.MAXORDER-timeOrder)*2;

      for( int i=0; i<timeRange.sz; i+=2 ) {
         s.append("t");
         try {
//            moc = new HealpixMoc(MAXORDER+"/"+timeRange.r[i]
//                  +( timeRange.r[i+1]>timeRange.r[i]+1?"-"+(timeRange.r[i+1]-1):""));
//            s.append(moc.toASCII());
            
//            s.append(MAXORDER+"/"+timeRange.r[i]
//                  +( timeRange.r[i+1]>timeRange.r[i]+1?"-"+(timeRange.r[i+1]-1):""));
            
            long deb = timeRange.r[i];
            long fin = timeRange.r[i+1];
            deb = deb>>>shift;
            fin = (fin-1)>>>shift;
            s.append(timeOrder+"/"+deb+ (fin==deb?"":"-"+fin));
            
         } catch( Exception e ) {  e.printStackTrace(); }
         Range m = timeRange.rangeArray[i>>>1];
         if( m!=null && !m.isEmpty() ) {
            s.append(" s");
            try {
               moc = new HealpixMoc(m);
               s.append(moc.toASCII());
            } catch( Exception e ) {  e.printStackTrace(); }
         }
         if( i<timeRange.sz-2 ) s.append(' ');
      }
      return s.toString();
   }
   
   
   static final private String TEST[][] = {
      { "Ajout à vide",                              "",           "t29/5-10 s29/2",   "t29/5-10 s29/2" ,             ""},
      { "Ajout singleton derrière singleton",        "t29/4 s29/1",       "t29/5 s29/2",      "t29/4 s29/1 t29/5 s29/2",            ""},
      { "Ajout singleton avant singleton",           "t29/5 s29/2",       "t29/4 s29/1",      "t29/4 s29/1 t29/5 s29/2",            ""},
      { "Ajout intervalle entrelacés après",         "t29/4-6 s29/1",     "t29/5-8 s29/2",    "t29/4 s29/1 t29/5-6 s29/1-2 t29/7-8 s29/2", ""},
      { "Ajout intervalle entrelacés avant",         "t29/5-8 s29/2",     "t29/4-6 s29/1",    "t29/4 s29/1 t29/5-6 s29/1-2 t29/7-8 s29/2", ""},
      { "Ajout intervalle englobant (s différents)", "t29/2-6 s29/2",     "t29/1-8 s29/1",    "t29/1 s29/1 t29/2-6 s29/1-2 t29/7-8 s29/1", ""},
      { "Ajout intervalle englobant (s identiques)", "t29/2-6 s29/2",     "t29/1-8 s29/2",    "t29/1-8 s29/2",               "t29/2-6 s29/2"},
      { "Ajout intervalle interne (s différents)",   "t29/1-8 s29/1",     "t29/2-6 s29/2",    "t29/1 s29/1 t29/2-6 s29/1-2 t29/7-8 s29/1", ""},
      { "Ajout intervalle interne (s identiques)",   "t29/1-8 s29/2",     "t29/2-6 s29/2",    "t29/1-8 s29/2" ,              "t29/2-6 s29/2"},
      { "Intercallage",                              "t29/6-7 11 s29/1",  "t29/9 s29/2",      "t29/6-7 s29/1 t29/9 s29/2 t29/11 s29/1",    ""},
      { "Fusion différents s",                       "t29/2-6 8-9 s29/2", "t29/7 s29/1",      "t29/2-6 s29/2 t29/7 s29/1 t29/8-9 s29/2",   ""},
      { "Fusion indentiques s",                      "t29/2-6 8-9 s29/2", "t29/7 s29/2",      "t29/2-9 s29/2",               ""},
      { "Remplacement sur début",                    "t29/2-6 s29/2 t29/7 s29/1", "t29/2-7 s29/2",   "t29/2-6 s29/2 t29/7 s29/1-2",        "t29/2-6 s29/2"},
      { "Remplacement sur fin",                      "t29/3-7 s29/2 t29/8 s29/1", "t29/2-7 s29/2",   "t29/2-7 s29/2 t29/8 s29/1",          "t29/3-7 s29/2"},
      { "Remplacement sur fin2",                     "t29/2-4 s29/2 t29/6 s29/1", "t29/6 s29/2",     "t29/2-4 s29/2 t29/6 s29/1-2",        ""},
      { "Tordu",                                     "t29/3 s29/1 t29/4-5 s29/2", "t29/3-5 s29/3",   "t29/3 s29/1 3 t29/4-5 s29/2-3",      ""},
      { "Inter simple",                              "t29/3-5 s29/1-3",    "t29/4-8 s29/2-4", "t29/3 s29/1-3 t29/4-5 s29/1-4 t29/6-8 s29/2-4",  "t29/4-5 s29/2-3"},
      { "Inter spécial",     "t29/1 s29/1-6 t29/3-9 s29/2","t29/3 s29/5-7 t29/8 s29/1-2", "t29/1 s29/1-6 t29/3 s29/2 5-7 t29/4-7 s29/2 t29/8 s29/1-2 t29/9 s29/2","t29/8 s29/2" },
      { "Ajout en suite",                            "t29/1-4 s29/1",      "t29/5-6 s29/1",   "t29/1-6 s29/1",               "" }
   };
   
   static final void test() throws Exception { test(-1); }
   static final void test(int x) throws Exception {
      String listTest[][] = x== -1 ? TEST : new String[][]{ TEST[x] };
      StringBuilder s = new StringBuilder();

      try {
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
            
            for( int j=0; j<stmoc2.timeRange.sz; j+=2 ) {
               stmoc1.timeRange.add(stmoc2.timeRange.r[j], stmoc2.timeRange.r[j+1], stmoc2.timeRange.rangeArray[j/2]);
            }
            s.append("\n Add  : "+stmoc1);
            s.append( test[3].equals(stmoc1.toString()) ? " => OK" : " => ERROR waiting: "+test[3] );

            s.append("\n");
         }
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      System.out.println(s);
   }

   /** Write an HEALPix STMOC to an output stream in ASCII encoded format */
   public void writeASCII(OutputStream out) throws Exception {
      if( isEmpty() ) return;
      int spaceOrder = 0;
      int timeOrder = getTimeOrder();
      boolean flagNL = getTimeRanges()>MAXWORD;
      int shift = (Moc.MAXORDER-timeOrder)*2;

      StringBuilder res= new StringBuilder(1000);
      for( int i=0; i<getTimeRanges(); i++ ) {
         
         // Ecriture du range temporel (à la résolution temporelle spécifique)
         long deb = timeRange.r[i*2] >>> shift;
         long fin = (timeRange.r[i*2+1]-1) >>> shift;
         if( i>0 ) res.append( flagNL ? CR:" ");
         res.append("t"+timeOrder+"/"+deb+ (fin==deb?"":"-"+fin));
         res.append( flagNL ? CR:" ");
         
         // Ecriture du Moc spatial associé
         res.append('s');
         writeASCIIFlush(out,res,false);
         int order=Moc.writeASCII(out,timeRange.rangeArray[i],flagNL);
         if( order>spaceOrder ) spaceOrder=order;
      }

      // Ajout de la résolution temporelle et spatiale si nécessaire
      if( spaceOrder!=getSpaceOrder() ) {
         res.append(flagNL ? CR : " " );
         res.append("t"+timeOrder+"/ s"+getSpaceOrder()+"/");
         if( flagNL ) res.append(CR);
      }

      writeASCIIFlush(out,res,false);
   }

   /** Write specifif FITS keywords
    * @param out
    * @return number of written bytes
    */
   protected int writeSpecificFitsProp( OutputStream out  ) throws Exception {
      int n=0;
      //      out.write( MocIO.getFitsLine("MOC","SPACETIME","Space Time MOC") );    n+=80;      
      out.write( MocIO.getFitsLine("MOC","TIME.SPACE","STMOC: Time dimension first, ") );    n+=80;      
      out.write( MocIO.getFitsLine("ORDERING","RANGE29","Range coding") );    n+=80;      
      //      out.write( MocIO.getFitsLine("MOCORDER",""+getMocOrder(),"Space MOC resolution") );    n+=80;      
//      out.write( MocIO.getFitsLine("TORDER",""+getTimeOrder(),"Time MOC resolution") );    n+=80;      
      out.write( MocIO.getFitsLine("MOCORDER",""+getTimeOrder(),"Time MOC resolution") );    n+=80;      
      out.write( MocIO.getFitsLine("MOCORD_1",""+getSpaceOrder(),"Space MOC resolution") );    n+=80;      
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
  
  protected void readSpecificData( InputStream in, int naxis1, int naxis2, int nbyte, cds.moc.MocIO.HeaderFits header) throws Exception {
     
     // Entête proto => MOC=SPACETIME ou même pas mentionné
     String type = header.getStringFromHeader("MOC");
     if( type==null || type.equals("SPACETIME") ) {
        timeOrder = header.getIntFromHeader("TORDER");
        spaceOrder = header.getIntFromHeader("MOCORDER");
        
     // Format TIME.SPACE
     } else {
        timeOrder = header.getIntFromHeader("MOCORDER");
        spaceOrder = header.getIntFromHeader("MOCORD_1");
     }
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
      if( tmin>=tmax ) { tmin=-1; tmax = Long.MAX_VALUE; }
      
      SpaceMoc moc = new SpaceMoc();
      int pos = timeRange.indexOf(tmin);
      if( (pos&1)==1 ) pos++;
      
      // On remplit un tableau de chaque intervalle spatial concerné (à la queue leu leu)
      long [] buf = new long[ getSize() ];
      int size=0;
      for( int i=pos; i<timeRange.sz; i+=2 ) {
         if( timeRange.r[i]>tmax ) break;
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
//         SpaceMoc moc = new SpaceMoc("3/3-5 4/456 5/");
//         System.out.println("result: "+moc);
         
//         SpaceTimeMoc moc = new SpaceTimeMoc();
//         moc.read("C:/Users/Pierre/Downloads/Moc.txt"); 
//         System.out.println( moc.toASCII() );
         
//         SpaceTimeMoc stm = new SpaceTimeMoc(29,29);
//         stm.append("t28/1 s3/40");
//         System.out.println("stm = "+stm);
         
         test(0);
         
//         stmoc1.write( "D:/STMoc.fits");
//         stmoc1.read("D:/STMoc.fits");
//         System.out.println("Resulat ecriture/lecture:\n"+stmoc1);
         
     } catch( Exception e ) { e.printStackTrace(); }
   }

   @Override
   public Moc clone() {
      SpaceTimeMoc moc = new SpaceTimeMoc();
      moc.timeOrder = timeOrder;
      moc.spaceOrder = spaceOrder;
      moc.timeRange = new Range2( timeRange );
      return moc;
   }
   
   /** Provide array of ranges at the deepest order */
   public Range getRange() { return timeRange; }
   
}
