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
import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import cds.tools.Util;

/** Construction d'un STMOC à partir des données HpxFinder
 * @author Pierre Fernique
 */
public class BuilderSTMoc extends BuilderTMoc {
   
   STMoc stmoc;
   private int nbAdd=0;    // Nombre d'ajouts avant une éventuelle reduction()

   public BuilderSTMoc(Context context) {
      super(context);
   }
   
   protected void reduction(Moc m) throws Exception { 
      String priority="ts";
      if( m.getSpaceOrder()<=hipsOrder ) priority="t";
      if( m.getTimeOrder()<=20 ) priority="sst";
      ((STMoc)m).reduction(maxSize,priority); 
   }

   protected void initIt() throws Exception {
      stmoc = new STMoc(timeOrder,spaceOrder );
   }
   
   protected void info() {
      String s = maxSize>0 ? " maxSize="+Util.getUnitDisk(maxSize):"";
      String s1 = stmoc.getMem()>0 ? " currentSize="+cds.tools.Util.getUnitDisk(stmoc.getMem()):"";
      context.info("STMOC generation (timeOrder="+stmoc.getTimeOrder()
                       +" spaceOrder="+stmoc.getSpaceOrder()+s+s1+")...");
   }
   
   // retourne true s'il est temps de tester un ajustement de taille
   protected boolean mustAdjustSize(Moc m, boolean force) {
      if( force ) return true;
      if( nbAdd<1000 ) { nbAdd++; return false; }
      nbAdd=0;
      return true;
   }
   

   protected void addIt(TMoc tmoc1, SMoc smoc1) throws Exception {
      double jdtmin = tmoc1.getTimeMin();
      double jdtmax = tmoc1.getTimeMax();
      stmoc.add(jdtmin, jdtmax, smoc1);
      adjustSize(stmoc,false);
   }

   protected void writeIt() throws Exception {
      adjustSize(stmoc,true);
      stmoc.seeRangeList().checkConsistency();     // A virer
      String file = context.getOutputPath()+Util.FS+"STMoc.fits";
      stmoc.write(file);
   }
}
