// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

package cds.allsky;

import static cds.tools.Util.FS;

import java.io.File;
import java.io.FileInputStream;

import cds.aladin.MyInputStream;
import cds.fits.HeaderFits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Cr�ation d'un fichier Moc.fits correspondant aux tuiles de plus bas niveau
 *  qui ont une syntaxe ou un contenu incorrect
 * @author  Pierre Fernique [CDS]
 */
public class BuilderMocError extends BuilderMoc {

   public BuilderMocError(Context context) {
      super(context);
    }
   
   public Action getAction() { return Action.MOCERROR; }

   public void validateContext() throws Exception { 
      validateOutput();
   }

   /** Cr�ation d'un Moc associ� � l'arborescence trouv�e dans le r�pertoire path */
   protected void createMoc(String path) throws Exception {
      
      moc = new HealpixMoc();
      fileOrder = mocOrder = Util.getMaxOrderByPath(path);
      tileOrder = context.getTileOrder();

      // dans le cas d'un survey � faible r�solution
      // ou qui couvre une petite partie du ciel, 
      try { 
         if( context.mocIndex==null ) context.loadMocIndex();
      } catch( Exception e ) { }
      
       mocOrder=fileOrder;
      
      // Quel type de tuile utiliser ?
      ext = getDefaultExt(path);
      if( ext!=null ) context.info("MOCERROR scanning based on "+ext+" tiles (order="+fileOrder+") ");
      
      // S'agit-il d'un gros cube => oui, alors on ne fait le MOC que sur la tranche du milieu
      // sinon c'est bien trop long
      frameCube = -1;
      if( context.getDepth()>10 ) frameCube=context.getDepth()/2;
      
      moc.setMocOrder(mocOrder);

      String outputFile = path + FS + Constante.FILE_MOCERROR;
      
      moc.setCoordSys(getFrame());
      moc.setCheckConsistencyFlag(false);
      generateMoc(moc,fileOrder, path);
      moc.setCheckConsistencyFlag(true);
      if( moc.getSize()==0 ) context.info("MOCERROR empty (no error tiles detected)");
      else {
         context.warning("MOCERROR saved as "+Constante.FILE_MOCERROR);
         moc.write(outputFile);
      }
   }
   
   private String getDefaultExt(String path) { return "fits"; }
   
   protected void generateMoc(HealpixMoc moc, int fileOrder,String path) throws Exception {
      
      initStat();
      
      File f = new File(path + Util.FS + "Norder" + fileOrder );
      

      File[] sf = f.listFiles();
      if( sf==null || sf.length==0 ) throw new Exception("No tiles found !");
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
            
            // Ecarte les frames non concern�es dans le cas d'un cube>10frames
            if( frameCube>-1 ) {
               if( getCubeFrameNumber(file)!=frameCube ) continue;
            }

            generateTileMoc(moc,sf1[j], fileOrder, npix);
         }
         moc.checkAndFix();
      }
   }
   

   protected void generateTileMoc(HealpixMoc moc,File f,int fileOrder, long npix) throws Exception {
      updateStat();
      
      MyInputStream dis = null;
      try {
         dis = new MyInputStream(new FileInputStream(f));
         dis=dis.startRead();
         HeaderFits header = new HeaderFits(dis);
         dis.close();
         dis=null;
         
         long w = header.getIntFromHeader("NAXIS1");
         long h = header.getIntFromHeader("NAXIS2");
         long n = Math.abs( header.getIntFromHeader("BITPIX")/8 );
         
         long size = 2880L + w*h*n;
         long len = f.length();
         
         if( len<size  ) moc.add(mocOrder,npix);
         
      }catch( Exception e ) {
         moc.add(mocOrder,npix);
      } finally { if( dis!=null ) dis.close(); }
      
      if( npix%10000 == 0 ) moc.checkAndFix();
   }
}
