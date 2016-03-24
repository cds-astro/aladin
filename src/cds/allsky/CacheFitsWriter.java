// Copyright 2010 - UDS/CNRS
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

import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.Util;

/**
 * Extension de la classe CacheFits pour gérer l'écriture optimisé d'un ensemble de fichiers Fits
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 - novembre 2014
 */
public class CacheFitsWriter extends CacheFits {
   
   CacheFitsWriter(long maxMem) { super(maxMem); }
   
   // réécriture du fichier lors du retrait du cache
   protected void remove(String name) throws Exception {
      FitsFile f = find(name);
      
      Util.createPath(name);
      f.fits.writeFITS(name);
      map.remove(name);
   }
   
   /** Ajout direct d'un Fits dans la gestion du cache */
   public void addFits(String filename,Fits fits) throws Exception {
      synchronized( lockObj ) {
         // On s'assure qu'il va y avoir assez de place pour ajouter un nouveau fits dans le cache
         if( isOver() || map.size()>10000 ) clean();
         FitsFile f = new FitsFile();
         f.fits = fits;
         map.put(filename,f);
         statNbOpen++;
      }
   }

   
}
