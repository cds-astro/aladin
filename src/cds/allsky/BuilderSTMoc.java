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

package cds.allsky;
import cds.moc.STMoc;
import cds.tools.Util;

/** Construction d'un STMOC à partir des données HpxFinder
 * TEST TEST TEST TEST
 *
 * @author Pierre Fernique
 */
public class BuilderSTMoc extends BuilderTMoc {
   
   STMoc stMoc;

   public BuilderSTMoc(Context context) {
      super(context);
   }
   
   protected void initIt() {
      try {
         stMoc = new STMoc(31,context.getOrder() );
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   int n=0;
   
   protected void addIt(int order, long npix, double jdtmin, double jdtmax) {
      try {
         stMoc.add(order,npix,jdtmin,jdtmax);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   
   protected void writeIt() throws Exception {
      String file = context.getOutputPath()+Util.FS+"STMoc.fits";
      stMoc.write(file);
   }

   
//   private void testPerf() throws Exception {
//      long tmin = stMoc.rangeSet.r[ stMoc.getTimeRanges()/2 ];
//      long tmax = stMoc.rangeSet.r[ 2*stMoc.getTimeRanges()/3 ];
//      SMoc sp = new SMoc("3/40");
//      sp.toRangeSet();
//      STMoc m = new STMoc();
//      m.add(tmin,tmax, sp.rangeSet.r[0], sp.rangeSet.r[1]);
//      
//      int a=0;
//      long t1 = System.currentTimeMillis();
//      STMoc m1=null;
//      for( int i=0;i<10; i++ ) {
//         m1 = (STMoc)stMoc.intersection(m);
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
