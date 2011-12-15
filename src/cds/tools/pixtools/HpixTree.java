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

package cds.tools.pixtools;

import java.io.InputStream;

import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.moc.Array;
import cds.moc.HealpixMoc;

/** Gestion d'un ensemble de pixels Healpix, d'ordres variables
 * Notamment utilisé pour les Multi-Order Coverage Maps
 * @author Pierre Fernique [CDS]
 * @version 1.0 March 2011
 */
public final class HpixTree extends HealpixMoc implements Cloneable {
   
    
   public HpixTree(String s) throws Exception { super(s); }
   
   public HpixTree(InputStream in) throws Exception {
      super();
      read(in);
   }
   
   /** Retourne le système de coordonnées utilisé (Localisation.GAL, Localisation.ICRS) */
   public int getFrame() { 
      String c = getCoordSys();
      if( c==null || c.charAt(0)=='G' ) return Localisation.GAL;
      return Localisation.ICRS;
   }
   
   /** Ajout d'un pixel */
   public boolean add(int order,long npix) throws Exception {
      
      // On n'accepte pas en dessous de l'ordre 3 => sinon impossible à tracer
      // On insère les fils d'ordre 3 équivalents
      if( order<3 ) {
         npix *=4;
         for( int i=0; i<4; i++ ) add(order+1,npix+i);
         return true;
      }
      return super.add(order,npix);
   }
   
   
   /** Read HEALPix tree from a stream. Autodetect encoded format */
   public void read(InputStream in) throws Exception {
      MyInputStream mis = (in instanceof MyInputStream) ? (MyInputStream)in : (new MyInputStream(in)).startRead();
      if( (mis.getType() & MyInputStream.FITS)!=0 ) readFits(mis);
      else readASCII(mis);

   }
   
}
