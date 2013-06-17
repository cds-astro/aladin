// Copyright 2012 - UDS/CNRS
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

package cds.allsky;

import java.io.File;

import cds.aladin.Aladin;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles FITS à partir des tuiles de plus bas
 * niveau. La méthode employée est la moyenne.
 * @author Pierre Fernique
 */
public class BuilderTreeMerge extends BuilderTree {

   /**
    * Création du générateur de l'arbre FITS.
    * @param context
    */
   public BuilderTreeMerge(Context context) {
      super(context);
   }

   public Action getAction() { return Action.MERGE; }

   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      validateToBeMerged();
      if( !context.isExistingAllskyDir() ) throw new Exception("No tile found in ouput dir");
      if( !context.isExistingAllskyDir( context.getToBeMergedPath() ) ) throw new Exception("No tile found in tobemerged dir");

      int order = Util.getMaxOrderByPath( context.getOutputPath() );
      context.setOrder(order);
      int mergeOrder = Util.getMaxOrderByPath( context.getToBeMergedPath() );
      if( order!=mergeOrder ) throw new Exception("Uncompatible all-skies: out.order="+order+" merge.order="+mergeOrder);
      context.info("Order retrieved from ["+context.getOutputPath()+"] => "+order);
      
      // On initialise la région à traiter avec le MOC du allsky à merger.
      HealpixMoc moc = new HealpixMoc();
      File f = new File(context.getToBeMergedPath()+Util.FS+BuilderMoc.MOCNAME);
      if( !f.exists() ) f= new File( context.getToBeMergedPath()+Util.FS+Constante.HPX_FINDER+Util.FS+BuilderMoc.MOCNAME );
      if( f.exists() ) moc.read( f.getCanonicalPath() );
      if( moc.getSize()>0 ) context.setMocArea(moc);
      
      context.initRegion();
   }
   
   // Vérifie que le répertoire tobemerged a été passé en paramètre
   // et qu'il s'agit bien d'un répertoire utilisable
   protected void validateToBeMerged() throws Exception {
      String dir = context.getToBeMergedPath();
      if( dir==null ) throw new Exception("\"tobemerged\" parameter required !");
      File f = new File(dir);
      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("\"tobemerged\" directory not available ["+dir+"]");
   }
   
   private boolean firstOut=true;
   private boolean firstMerge=true;
   private void checkConstantes(Fits f) throws Exception {
      if( bitpix!=f.bitpix ) throw new Exception("Uncompatible all-skies: out.bitpix="+bitpix+" merge.bitpix="+bitpix);
      if( bscale!=f.bscale ) throw new Exception("Uncompatible all-skies: out.bscale="+bscale+" merge.bscale="+bscale);
      if( bzero!=f.bzero ) throw new Exception("Uncompatible all-skies: out.bzero="+bzero+" merge.bzero="+bzero);
      if( width!=f.width ) throw new Exception("Uncompatible all-skies: out.width="+width+" merge.width="+width);
      if( Double.isNaN(blank) && !Double.isNaN(f.blank) || !Double.isNaN(blank) && Double.isNaN(f.blank)
            || !Double.isNaN(blank) && !Double.isNaN(f.blank) && blank!=f.blank ) 
         throw new Exception("Uncompatible all-skies: out.blank="+blank+" merge.blank="+blank);
   }
   
   protected boolean testTree(int order,int maxOrder) { return true; }
   
   protected Fits createLeaveFits(String path, String toBeMergedPath, int order, long npix) throws Exception {
      String sOut   = Util.getFilePath(path,order,npix);
      String sMerge = Util.getFilePath(toBeMergedPath,order,npix);
      Fits out      = createLeaveFits(sOut);
      if( firstOut && out!=null ) { firstOut=false; setConstantes(out); }
      Fits merge = createLeaveFits(sMerge);
      if( firstMerge && merge!=null ) { firstMerge=false; checkConstantes(merge); }
      
      CoAddMode mode = context.getCoAddMode();
      switch(mode) {
         case REPLACETILE: if( merge!=null )     out=merge;  break;
         case KEEPTILE :   if( out==null )       out=merge;  break;
         case AVERAGE:     out.coadd(merge);                 break;
         case OVERWRITE:   out.mergeOnNaN(merge);            break;
         case KEEP:        merge.mergeOnNaN(out); out=merge; break;
      }
      return out;
   }

}