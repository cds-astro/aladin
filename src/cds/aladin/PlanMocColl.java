// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;

import cds.allsky.MocGen;
import cds.tools.Util;

/** Generation d'un plan MOC à partir d'une collection d'images
 * @author P.Fernique [CDS]
 * @version 1.0 - janvier 2014
 */
public class PlanMocColl extends PlanMoc {
   
   private String directory;            // Répertoire des images sources
   private int order;                   // Résolution (ordre) demandée
   private boolean strict;
   private double blank;
   private boolean recursive;
   private int [] hdu;
   
   protected PlanMocColl(Aladin aladin,String label,String directory,int order,
         boolean strict,boolean recursive,double blank,int [] hdu) {
      super(aladin,null,null,label,null,0);
      this.directory = directory;
      this.order = order;
      this.strict=strict;
      this.recursive=recursive;
      this.blank=blank;
      this.hdu = hdu;
      
      pourcent=0;
      
      suiteSpecific();
      threading();
      log();
   }
   
   protected void log() {
    aladin.log("MOCcoll",null);
   }
   
   protected void suite1() {}
   
   protected boolean Free() {
      if( generator!=null ) generator.abort();
      return super.Free();
   }
   
   protected String getProgress() {
      if( generator==null ) return super.getProgress();
      int n = generator.getNbImages();
      if( moc!=null ) return " - "+n+" original img";
      return " - "+n+" img - scanning "+generator.getScanningDir()+"...";
   }

   
   private MocGen generator=null;
   private int step=1;

   protected boolean waitForPlan() {
      try {
         frameOrigin=Localisation.ICRS;
         generator = new MocGen(directory,order,recursive,strict,blank,hdu);
//         generator.verbose=true;
         generator.start();
         while( !generator.isReady() ) {
            pourcent+=step;
            if( pourcent==99 ) step=-1;
            else if( pourcent==0 ) step=1;
            Util.pause(1000);
         }
         if( generator.isError() ) throw new Exception(generator.getError());
         moc = generator.getMoc();

      } catch( Exception e ) {
         aladin.error=error=e.getMessage();
         flagProcessing=false;
         flagOk=true;
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      pourcent=100;
      flagProcessing=false;
      flagOk=true;
      return true;
   }
   

      
}

