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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
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
   
   public BuilderMoc() {
      moc = new HealpixMoc();
      moc.setCheckUnicity(true);
   }
   
   public HealpixMoc getMoc() { return moc; }
   
   /** Création d'un Moc associé à l'arborescence de l'index HPX_FINDER
    * Si celui-ci n'existe pas/plus, se sera sur le répertoire lui-même
    * puis écriture sur le disque à la racine
    */
   public void createMoc(String path) {
      try {
        BuilderMoc bdMoc = new BuilderMoc();
        try {
           bdMoc.generateMoc(path + FS + Constante.HPX_FINDER);
        } catch( Exception e ) {
           System.err.println("CreateMoc: "+Constante.HPX_FINDER+" not found => inspect the base directory");
           bdMoc.generateMoc(path);
        }
        HealpixMoc moc = bdMoc.getMoc();
        moc.sort();
        moc.write(path+FS+MOCNAME, HealpixMoc.FITS);
//        moc.write(path+FS+"Moc.txt", HealpixMoc.ASCII);
     } catch( Exception e ) {
        e.printStackTrace();
     }
   }
   
   public void generateMoc(String path) throws Exception {
      moc.clear();
      int order = Util.getMaxOrderByPath(path);
      File f = new File(path+Util.FS+"Norder"+order);      
      
      File [] sf = f.listFiles();
      for( int i=0; i<sf.length; i++ ) {
         if( !sf[i].isDirectory() ) continue;
         File [] sf1 = sf[i].listFiles();
         for( int j=0; j<sf1.length; j++ ) {
            int npix = Util.getNpixFromPath(sf1[j].getAbsolutePath());
            if( npix==-1 ) continue;
            add(order,npix);
         }
      }
   }
   
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
         bdMoc.generateMoc("C:/Documents and Settings/Standard/Bureau/HalphaNorthALLSKY");
         HealpixMoc moc = bdMoc.getMoc();
         System.out.println(moc);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}
