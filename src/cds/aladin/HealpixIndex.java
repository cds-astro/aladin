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

package cds.aladin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.TreeMap;

import cds.tools.pixtools.Util;

/** Gère un losange Healpix contenant la liste des progéniteurs 
 * (images originales qui ont permis de créer le survey Healpix)
 * => Cette arborescence Healpix se trouve toujours dans le répertoire "HpxFinder"
 */
public class HealpixIndex extends TreeMap<String, HealpixIndexItem> implements Iterable<String>{
   
   /** Parsing d'un flux d'entrées, et mémorisation de celles-ci */
   public void loadStream(InputStream stream) throws Exception {
      DataInputStream in=null;
      try {
         in = new DataInputStream(new BufferedInputStream(stream));
         String s;
         while( (s=in.readLine())!=null ) {
            HealpixIndexItem item = new HealpixIndexItem(s);
            put( item.getID(), item);
         }
      } finally  {
         if( in!=null ) in.close();
      }
   }
   
   public void writeStream(OutputStream stream) throws Exception {
      DataOutputStream out = null;
      try {
         out = new DataOutputStream(new BufferedOutputStream(stream));
         for( String k : this ) { out.writeBytes(  get(k).getJson()+Util.CR ); }
      } finally {
         if( out!=null ) out.close();
      }
   }
   
   /** Iterator sur chaque entrée */
   public Iterator<String> iterator() { return keySet().iterator(); }
   
   public String toString() {
      StringBuffer s = new StringBuffer("[");
      for( String k : this ) {
         if( s.length()>1 ) s.append(", ");
         s.append(k);
      }
      s.append(']');
      return s.toString();
   }
   
}
