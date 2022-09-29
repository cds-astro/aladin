// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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
import java.io.File;

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
      
      if( ruleSize==null ) ruleSize="tts";

      while( m.getMem()>maxSize && (m.getTimeOrder()>0 || m.getSpaceOrder()>0) ) {
         char c = ruleSize.charAt(0);
         if( c!='t' && c!='s' ) throw new Exception("Unknown MOC degration rule character ["+c+"]");
         if( c=='t' && m.getTimeOrder()>0 ) m.setTimeOrder( m.getTimeOrder()-1 );
         else if( c=='s' &&  m.getSpaceOrder()>0 ) m.setSpaceOrder( m.getSpaceOrder()-1 );
         else break;  // Pas applicable
         ruleSize = ruleSize.substring(1)+(c+"");
      }
   }

   protected void initIt() throws Exception {
      stmoc = new STMoc(timeOrder,spaceOrder );
   }
   
   protected void info() {
      String s = maxSize<=0 ? "": " sizeLimit<"+Util.getUnitDisk(maxSize)+(ruleSize!=null?"(degradationRule:"+ruleSize+")":"");
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

   protected void cleanIt() throws Exception {
      String file = context.getOutputPath()+Util.FS+"STMoc.fits";
      (new File(file)).delete();      
   }
   
   protected void writeIt() throws Exception {
      adjustSize(stmoc,true);
      stmoc.seeRangeList().checkConsistency();     // A virer
      if( stmoc.isEmpty() ) throw new Exception("Empty MOC => not generated");
      String file = context.getOutputPath()+Util.FS+"STMoc.fits";
      stmoc.write(file);
   }
}
