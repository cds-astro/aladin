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

import cds.fits.Fits;

/** Vérification du check code et des DATASUM associés aux tuiles FITS
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Juillet 2022
 */
public class BuilderCheckDataSum extends BuilderCheckCode {
   static final int MAXCORRUPT = 99;  // Nombre MAX de fichiers corrompus avant d'arrêter
   static final int MAXMISSING = 10;  // Nombre MAX de fichiers tolérés sans DATASUM
   
   int missingDataSum=0;              // Nombre de fichiers sans DATASUM
   int corruptDataSum=0;              // Nombre de fichiers corrompus
   boolean flagGlobalDataSum=false;   // true si on doit calculer le DATASUM global
   long globalDataSum=0L;             // DATASUM global sur toutes les tuiles Fits

   public BuilderCheckDataSum(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.CHECKDATASUM; }
   
   protected void validateContextMore() throws Exception {
      if( format.indexOf("fits")<0 ) throw new Exception("No Fits tiles for this HiPS!");
      activateGlobalDataSum( context.flagGlobalDataSum );
   }
   
   /** Retourne le Global DATASUM qui a été calculé */
   public long getDataGlobalDataSum() throws Exception { 
      if( !flagGlobalDataSum ) throw new Exception("Global DATASUM not computed (use activateGlobalDataSum(true) before)");
      return globalDataSum; 
   }
   
   /** True si on veut calculer puis recuperer le DATASUM global
    * => cf. getGlobalDataSum() 
    * @param flag
    */
   private void activateGlobalDataSum(boolean flag ) {flagGlobalDataSum=flag; }
   
   public void run() throws Exception {

      Info info = scanDir(new File( context.getOutputPath() ),"fits");
      
      // A priori pas de fichiers avec DATASUM
      if( missingDataSum>MAXMISSING  && corruptDataSum==0 ) {
         context.warning("This HiPS has not DATASUM in their FITS tiles => No DATASUM check");
      
      // Rapport des fichiers éventuellement corrompus
      } else if( corruptDataSum==0 ) {
         context.info("All"+(missingDataSum==0?"":" tested")+" HiPS Fits tiles DATASUM Ok");
         
      } else {
         context.error(corruptDataSum+" HiPS Fits tile(s) corrupted (DATASUM not compliant)!");
         report( info );
         
         if( missingDataSum>0 ) context.warning(missingDataSum+" HiPS Fits tile(s) without DATASUM => not tested");
         throw new Exception("HiPS is corrupted");
      }
      
      if( flagGlobalDataSum ) context.info("Global DATASUM: "+globalDataSum);
      
      // Vérification du Check code Fits
      String v = Context.getCheckCode("fits", context.getCheckCode());
      if( v==null ) {
         context.warning("Unknown original check code for fits tiles => No check code verification!");
      } else  {
         if( !info.getCode().equals(v) ) throw new Exception("HiPS is not compliant to Fits check code");
         else context.info("HiPS compliant with Fits check code!");
      }
   }
   
   // Affichage des fichiers corrompus
   private void report(Info info) {
      for( File f: info.corruptedFile ) {
         context.info("   ."+f.getAbsolutePath());
      }
   }
   
   protected void updateInfo(File f, Info info) throws Exception {
      super.updateInfo(f, info);
      
      if( missingDataSum>MAXMISSING && corruptDataSum==0 ) return;

      Fits fits = new Fits();
      fits.loadFITS( f.getAbsolutePath(),false );
      int check = fits.checkDataSum();
      if( check==0 ) { corruptDataSum++; info.addCorruptedFile(f); }
      else if( check==-1 ) missingDataSum++;
      
      // On va calculer un DATASUM global
      if( flagGlobalDataSum ) globalDataSum = fits.computeDataSum(globalDataSum);
      
      if( corruptDataSum>MAXCORRUPT ) {
         context.error("A lot of HiPS Fits tile(s) corrupted (DATASUM not compliant)!");
         report(info);
         context.info("   ...");
         throw new Exception("Too many corrupted Fits tiles (DATASUM not compliant))");
      }
      
   }

}
