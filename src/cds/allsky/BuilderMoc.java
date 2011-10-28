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

import static cds.tools.Util.FS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import cds.aladin.Aladin;
import cds.aladin.Localisation;
import cds.aladin.PlanHealpix;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;


/**
 * Création d'un fichier Moc.fits
 */
final public class BuilderMoc {
   
   public static final String MOCNAME = "Moc.fits";
   
   private HealpixMoc moc;
   private String ext;   // Extension à traiter, null si non encore affectée.
   
   public BuilderMoc() {
      moc = new HealpixMoc();
//      moc.setCheckUnicity(true);  // inutile puisque l'on teste les extensions des fichiers NPixNNN
   }
   
   public HealpixMoc getMoc() { return moc; }
   
   /** Création d'un Moc associé à l'arborescence de l'index HPX_FINDER
    * ou du répertoire lui-même puis écriture sur le disque à la racine
    */
   public void createMoc(String path) { createMoc(path,path); }
   public void createMoc(String path,String output) {
      try {
         BuilderMoc bdMoc = new BuilderMoc();
         bdMoc.generateMoc(path);
         HealpixMoc moc = bdMoc.getMoc();
         moc.sort();
         moc.write(output+FS+MOCNAME, HealpixMoc.FITS);
//         moc.write(output+FS+"Moc.txt", HealpixMoc.ASCII);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   public void generateMoc(String path) throws Exception {
      ext=null;
      moc.clear();
      moc.setCoordSys( getFrame(path) );
      int order = Util.getMaxOrderByPath(path);
      File f = new File(path+Util.FS+"Norder"+order);      
      
      
      // Ajout des pixels de plus bas niveau uniquement
      // et création immédiate de l'arborescence par récursivité dès qu'on a 4 frères
      // consécutifs
      File [] sf = f.listFiles();
      for( int i=0; i<sf.length; i++ ) {
         if( !sf[i].isDirectory() ) continue;
         File [] sf1 = sf[i].listFiles();
         for( int j=0; j<sf1.length; j++ ) {
            String file = sf1[j].getAbsolutePath();
            
            int npix = Util.getNpixFromPath(file);
            if( npix==-1 ) continue;
            
           // Ecarte les fichiers n'ayant pas l'extension requise
            String e=getExt(file);
            if( ext==null ) ext=e;
            else if( !ext.equals(e) ) continue;
            
            add(order,npix);
         }
      }
   }
   
   // retourne l'extension du fichier passé en paramètre, "" si aucune
   private String getExt(String file) {
      int offset = file.lastIndexOf('.');
      if( offset==-1 ) return "";
      return file.substring(offset+1,file.length());
   }
   
   private String getFrame(String path) {
      try {
         File f = new File(path+Util.FS+PlanHealpix.PROPERTIES);
         Properties prop = new java.util.Properties();
         prop.load(new FileInputStream(f));
         return prop.getProperty(PlanHealpix.KEY_COORDSYS,"C");
      } catch( Exception e ) { }
      return "C";
   }
   
   
   // Insertion récursive : dès qu'on a 4 frères consécutifs, on les supprime
   // et on insère récursivement le père
   private void add(int order, int npix) {
      int me = npix%4;
      int firstBrother = npix - me;
      
      // Y a-t-il un frangin encore absent (en plus de moi) ? si oui, on insère
      for( int i=0; i<4; i++ ) {
         if( i==me ) continue;
         if( !moc.isIn(order, firstBrother+i) ) {
            moc.add(order,npix);
            return;
         }
      }
      
      // Les 3 frangins sont là, on les supprime et on insère le père
      for( int i=0; i<4; i++ ) {
         if( i==me ) continue;
         moc.delete(order, firstBrother+i);
      }
      add(order-1,firstBrother/4);
   }
   
   static public void main(String [] argv) {
      try {
         BuilderMoc bdMoc = new BuilderMoc();
         bdMoc.generateMoc("C:/Documents and Settings/Standard/Bureau/2MASStestALLSKY");
         HealpixMoc moc = bdMoc.getMoc();
         System.out.println(moc);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}
