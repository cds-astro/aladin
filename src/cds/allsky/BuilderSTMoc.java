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

package cds.allsky;
import cds.moc.SpaceTimeMoc;
import cds.tools.Util;

/** Construction d'un STMOC à partir des données HpxFinder
 * TEST TEST TEST TEST
 *
 * @author Pierre Fernique
 */
public class BuilderSTMoc extends BuilderTMoc {
   
   static public final double DAYMICROSEC = 86400000000.;

   SpaceTimeMoc stMoc;

   public BuilderSTMoc(Context context) {
      super(context);
   }
   
   protected void initIt() {
      stMoc = new SpaceTimeMoc(14, context.getOrder() );
   }
   
   int n=0;

   protected void addIt(int order, long npix, double jdtmin, double jdtmax) {
      
      try {
         long tmin = (long)(jdtmin*DAYMICROSEC);
         long tmax = (long)(jdtmax*DAYMICROSEC);
                 
         long smin = npix<<(2*(29-order));
         long smax = (npix+1)<<(2*(29-order));
         
         stMoc.add(tmin,tmax,smin,smax);
         
         n++;
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
   protected void writeIt() throws Exception {
      System.out.println("Order="+stMoc.getTimeOrder()+" Ntimes="+stMoc.getTimeRanges()+" Size="+Util.getUnitDisk( stMoc.getMem()));
      String file = context.getOutputPath()+Util.FS+"STMoc.fits";
      stMoc.write(file);
   }

   
//   private void testPerf() throws Exception {
//      long tmin = stMoc.rangeSet.r[ stMoc.getTimeRanges()/2 ];
//      long tmax = stMoc.rangeSet.r[ 2*stMoc.getTimeRanges()/3 ];
//      SpaceMoc sp = new SpaceMoc("3/40");
//      sp.toRangeSet();
//      SpaceTimeMoc m = new SpaceTimeMoc();
//      m.add(tmin,tmax, sp.rangeSet.r[0], sp.rangeSet.r[1]);
//      
//      int a=0;
//      long t1 = System.currentTimeMillis();
//      SpaceTimeMoc m1=null;
//      for( int i=0;i<10; i++ ) {
//         m1 = (SpaceTimeMoc)stMoc.intersection(m);
//         a+=m1.getMocOrder();
//      }
//      long t2 = System.currentTimeMillis();
//      System.out.println("Inter en "+((t2-t1)/10.)+"ms\n=>"+m1);
//   }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
     super.showStatistics();
     context.info("STMOC time ranges:"+stMoc.getTimeRanges()+" size="+Util.getUnitDisk( stMoc.getMem()));
   }

   
}
