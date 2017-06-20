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
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Cr�ation d'un fichier Moc.fits correspondant aux tuiles de plus bas niveau
 * @author Ana�s Oberto [CDS] & Pierre Fernique [CDS]
 */
public class BuilderMoc extends Builder {

   protected HealpixMoc moc;
   protected int mocOrder;
   protected int fileOrder;
   protected int tileOrder;
   protected boolean isMocHight;
   
   protected String ext=null; // Extension � traiter, null si non encore affect�e.
   protected int frameCube=-1; // Num�ro de la frame � utiliser pour g�n�rer le MOC dans le cas d'un gros cube (depth>10)

   public BuilderMoc(Context context) {
      super(context);
    }
   
   public Action getAction() { return Action.MOC; }

   public void run() throws Exception {
      createMoc(context.getOutputPath());
   }
   
   public void validateContext() throws Exception { 
      validateOutput();
      if( !context.verifTileOrder() ) throw new Exception("Uncompatible tileOrder !");
   }

   public HealpixMoc getMoc() { return moc; }

   /** Cr�ation d'un Moc associ� � l'arborescence trouv�e dans le r�pertoire path */
   protected void createMoc(String path) throws Exception {
      
      moc = new HealpixMoc();
      fileOrder = mocOrder = Util.getMaxOrderByPath(path);
      tileOrder = context.getTileOrder();

      // dans le cas d'un survey � faible r�solution
      // ou qui couvre une petite partie du ciel, 
      boolean isLarge=true;
      try { 
         if( context.mocIndex==null ) context.loadMocIndex();
         isLarge = context.mocIndex.getCoverage()>1/6.;
      } catch( Exception e ) { }
      
      
      // mocOrder explicitement fourni par l'utilisateur
      if( context.getMocOrder()!=-1 ) mocOrder = context.getMocOrder();
      
      // mocOrder d�termin� par la nature du survey
      else {
         if( mocOrder<Constante.DEFAULTMOCORDER || !isLarge ) {
            mocOrder = context.getOrder()+context.getTileOrder()-Constante.DIFFMOCORDER;
         }
         if( mocOrder< Constante.DEFAULTMOCORDER ) mocOrder = Constante.DEFAULTMOCORDER;
      }
      
      // On ne peut prendre un MOC order sup�rieur � la r�solution nomimale
      if( mocOrder>tileOrder+fileOrder ) mocOrder=tileOrder+fileOrder;
      
      // Couleur
      if( context.isColor() ) mocOrder=fileOrder;
      
      // Quel type de tuile utiliser ?
      ext = getDefaultExt(path);
      if( ext!=null ) context.info("MOC generation based on "+ext+" tiles");
      
      // S'agit-il d'un gros cube => oui, alors on ne fait le MOC que sur la tranche du milieu
      // sinon c'est bien trop long
      frameCube = -1;
      if( context.getDepth()>10 ) frameCube=context.getDepth()/2;
      
      isMocHight = mocOrder>fileOrder && ext!=null && ext.equals("fits");
      
      moc.setMocOrder(mocOrder);

      String outputFile = path + FS + Constante.FILE_MOC;
      
      long t = System.currentTimeMillis();
      context.info("MOC generation ("+(isMocHight?"hight resolution":"low resolution")+" mocOrder="+moc.getMocOrder()+")...");
      moc.setCoordSys(getFrame());
      moc.setCheckConsistencyFlag(false);
      generateMoc(moc,fileOrder, path);
      moc.setCheckConsistencyFlag(true);
      moc.write(outputFile);
      
      long time = System.currentTimeMillis() - t;
      context.info("MOC done in "+cds.tools.Util.getTemps(time,true)
                        +": mocOrder="+moc.getMocOrder()
                        +" size="+cds.tools.Util.getUnitDisk( moc.getSize()));
   }
   
   private String getDefaultExt(String path) {
      if( (new File(path+FS+"Norder3"+FS+"Allsky.fits")).exists() ) return "fits";
      if( (new File(path+FS+"Norder3"+FS+"Allsky.jpg")).exists() ) return "jpg";
      if( (new File(path+FS+"Norder3"+FS+"Allsky.png")).exists() ) return "png";
      return null;
   }
   
   
   long startTime=0L;
   int nbTiles = -1;
   
   protected void initStat() {
      startTime = System.currentTimeMillis();
      nbTiles=1;
   }
   
   protected void updateStat() {
      nbTiles++;
   }
   
   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      long now = System.currentTimeMillis();
      long cTime = now-startTime;
      if( cTime<2000 ) return;
      
      context.stat(nbTiles+" tile"+(nbTiles>1?"s":"")+" scanned in "+cds.tools.Util.getTemps(cTime) );
   }
   
   /** Retourne la surface du Moc (en nombre de cellules de plus bas niveau */
   public long getUsedArea() { return moc.getUsedArea(); }

   /** Retourne le nombre de cellule de plus bas niveau pour la sph�re compl�te */
   public long getArea() { return moc.getArea(); }

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
      if( isMocHight ) generateHighTileMoc(moc,fileOrder,f,npix);
      else moc.add(fileOrder,npix);
   }
   
   private void generateHighTileMoc(HealpixMoc moc,int fileOrder, File f, long npix) throws Exception {
      Fits fits = new Fits();
      MyInputStream dis = null;
      try {
         dis = new MyInputStream(new FileInputStream(f));
         dis=dis.startRead();
         fits.loadFITS(dis);
         dis.close();
         dis=null;
      }catch( Exception e ) {
         System.err.println("f="+f.getAbsolutePath());
         throw e;
      } finally { if( dis!=null ) dis.close(); }
      
      long nside = fits.width;
      long min = nside * nside * npix;
      int mocOrder = moc.getMocOrder();
      int tileOrder = (int) CDSHealpix.log2(nside);
      
      int div = (fileOrder+tileOrder - mocOrder) *2;
      context.createHealpixOrder( tileOrder );
      
      long oNpix=-1;  
      for( int y=0; y<fits.height; y++ ) {
         for( int x=0; x<fits.width; x++ ) {
            try {
               npix = min + context.xy2hpx(y * fits.width + x);
               
               npix = npix >>> div;
               
               // Juste pour �viter d'ins�rer 2x de suite le m�me npix
               if( npix==oNpix ) continue;
               
               double pixel = fits.getPixelDouble(x,y);
               
               // Pixel vide
               if( fits.isBlankPixel(pixel) ) continue;

               moc.add(mocOrder,npix);
               
               oNpix=npix;
            } catch( Exception e ) {
               e.printStackTrace();
            }
         }
      }
      moc.checkAndFix();
   }

   
   // Retourne l'extension du fichier pass� en param�tre, "" si aucune
   protected String getExt(String file) {
      int offset = file.lastIndexOf('.');
      if( offset == -1 ) return "";
      int pos = file.indexOf(Util.FS,offset);
      if( pos!= -1 ) return "";
      return file.substring(offset + 1, file.length());
   }
   
   // Retourne le num�ro de la frame dans le cas d'une tuile de cube, 0 si non trouv�
   protected int getCubeFrameNumber(String file) {
      try {
         int fin = file.lastIndexOf('.');
         int deb = file.lastIndexOf('_',fin);
         if( deb==-1 || fin==-1 ) return 0;
         return Integer.parseInt( file.substring(deb+1, fin));
      } catch( Exception e ) {}
      return 0;
   }

}
