// Copyright 2012 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
// This file is part of Aladin.
//
// Aladin is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3 of the License.
//
// Aladin is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// The GNU General Public License is available in COPYING file
// along with Aladin.
//

package cds.allsky;

import static cds.tools.Util.FS;

import java.io.File;

import cds.aladin.PlanHealpix;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Cr�ation d'un fichier Moc.fits correspondant aux tuiles de plus bas niveau
 * @author Ana�s Oberto [CDS] & Pierre Fernique [CDS]
 */
public class BuilderMoc extends Builder {

   public static final String MOCNAME = "Moc.fits";

   private HealpixMoc moc;

   private String ext; // Extension � traiter, null si non encore affect�e.

   public BuilderMoc(Context context) {
      super(context);
      moc = new HealpixMoc();
   }
   
   public Action getAction() { return Action.MOC; }

   public void run() throws Exception {
      createMoc(context.getOutputPath());
   }
   
   public void validateContext() throws Exception { validateOutput(); }

   public HealpixMoc getMoc() { return moc; }

   /** Cr�ation d'un Moc associ� � l'arborescence trouv�e dans le r�pertoire path */
   protected void createMoc(String path) throws Exception {
      createMoc(path, path + FS + MOCNAME);
   }

   private void createMoc(String path, String outputFile) throws Exception {
      generateMoc(path);
      moc.sort();
      moc.write(outputFile, HealpixMoc.FITS);
   }
   
   /** Retourne la surface du Moc (en nombre de cellules de plus bas niveau */
   public long getUsedArea() { return moc.getUsedArea(); }

   /** Retourne le nombre de cellule de plus bas niveau pour la sph�re compl�te */
   public long getArea() { return moc.getArea(); }

   private void generateMoc(String path) throws Exception {
      ext = null;
      moc.clear();
      moc.setCoordSys(getFrame());
      int order = Util.getMaxOrderByPath(path);
      File f = new File(path + Util.FS + "Norder" + order);

      // Ajout des pixels de plus bas niveau uniquement
      // et cr�ation imm�diate de l'arborescence par r�cursivit� d�s qu'on a 4
      // fr�res
      // cons�cutifs
      File[] sf = f.listFiles();
      if( sf.length==0 ) throw new Exception("No tiles found !");
      for( int i = 0; i < sf.length; i++ ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         if( !sf[i].isDirectory() ) continue;
         File[] sf1 = sf[i].listFiles();
         for( int j = 0; j < sf1.length; j++ ) {
            String file = sf1[j].getAbsolutePath();

            long npix = Util.getNpixFromPath(file);
            if( npix == -1 ) continue;

            // Ecarte les fichiers n'ayant pas l'extension requise
            String e = getExt(file);
            if( ext == null ) ext = e;
            else if( !ext.equals(e) ) continue;

            moc.add(order, npix);
         }
      }
   }
   
   // Retourne le code HEALPix correspondant au syst�me de r�f�rence des coordonn�es
   // du survey HEALPix
   private String getFrame() {
      try {
         if( context.prop==null ) context.loadProperties();
         return context.prop.getProperty(PlanHealpix.KEY_COORDSYS, "C");
      } catch( Exception e ) { e.printStackTrace(); }
      return "C";
   }

   // Retourne l'extension du fichier pass� en param�tre, "" si aucune
   private String getExt(String file) {
      int offset = file.lastIndexOf('.');
      if( offset == -1 ) return "";
      return file.substring(offset + 1, file.length());
   }

}
