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
import java.io.FileInputStream;
import java.io.FileOutputStream;

import cds.aladin.Aladin;
import cds.aladin.HealpixIndex;
import cds.tools.pixtools.Util;

/** Construction de la hi�rarchie des tuiles d'index � partir des tuiles de plus bas
 * niveau. Le but est de donner acc�s aux prog�niteurs
 * @author Pierre Fernique
 */
public class BuilderProgenIndex extends Builder {
   
   static public final int MINORDER = 4;   // niveau minimal pris en compte
   
   private int maxOrder;
   private int minOrder;

   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;

   /**
    * Cr�ation du g�n�rateur de l'arbre des index.
    * @param context
    */
   public BuilderProgenIndex(Context context) {
      super(context);
   }

   public Action getAction() { return Action.PROGEN; }

   public void run() throws Exception {
      build();
   }
   
   // Valide la coh�rence des param�tres pour la cr�ation des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();
      validateOrder( context.getHpxFinderPath() );
      maxOrder = context.getOrder();
//      minOrder = maxOrder-(RANGE_ORDER-1);
//      if( minOrder<3 ) minOrder=3;
      minOrder=Math.min(MINORDER,maxOrder);
      context.info("Tree HEALPix index built for orders ["+minOrder+".."+maxOrder+"]");
      context.initRegion();
   }
   
   // V�rifie que le r�pertoire HpxIndex existe et peut �tre utilis�
   private void validateIndex() throws Exception {
      String path = context.getHpxFinderPath();
      if( path==null ) throw new Exception("HEALPix index directory [HpxFinder] not defined => specify the output (or input) directory");
      File f = new File(path);
      if( !f.exists() || !f.isDirectory() || !f.canWrite() || !f.canRead() ) throw new Exception("HEALPix index directory not available ["+path+"]");
   }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, statSize, totalTime);
   }

   public void build() throws Exception {
      initStat();
      context.setProgressMax(768);
      String output = context.getHpxFinderPath();
      maxOrder = context.getOrder();
      
      for( int i=0; i<768; i++ ) {
         createTree(output,3,i);
         context.setProgress(i);
      }
   }
   
   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

   // Mise � jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction r�cursive de la hi�rarchie des tuiles FITS � partir des tuiles FITS
    * de plus bas niveau. La m�thode employ�e est la moyenne
    */
   private HealpixIndex createTree(String path,int order, long npix ) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      // Si son p�re n'est pas dans le MOC, on passe
      if( !context.isInMocTree(order-1,npix/4) ) return null;
      
//      // Si ni lui, ni ses fr�res sont dans le MOC, on passe
//      boolean ok=false;
//      long brother = npix - npix%4L;
//      for( int i=0; i<4; i++ ) {
//         ok = context.isInMocTree(order,brother+i);
//         if( ok ) break;
//      }
//      if( !ok ) return null;
      
//      System.out.println("createTree("+order+","+npix+")...");
      
      String file = Util.getFilePath(path,order,npix);

      HealpixIndex out = null;
      if( order==maxOrder ) out = createLeave(file);
      else {
         HealpixIndex fils[] = new HealpixIndex[4];
         boolean found = false;
         for( int i =0; i<4; i++ ) {
            fils[i] = createTree(path,order+1,npix*4+i);
            if (fils[i] != null && !found) found = true;
         }
         if( found ) out = createNode(fils);
      }
      
      // Si on a trop de Progen
      if( order<maxOrder && out!=null && out.size()>HealpixIndex.TOOMANY ) out.setTooMany(true);

      if( out!=null && !out.hasTooMany() && context.isInMocTree(order,npix) && order<maxOrder) {
         writeIndexFile(file,out);
         Aladin.trace(4, "Writing " + file);

      }
      if( out!=null && order==maxOrder ) {
         File f = new File(file);
         updateStat(f);
      }
      
      if( order<minOrder ) return null;
      return out;
   }
   
   // Ecriture du fichier d'index HEALPix correspondant � la map pass�e en param�tre
   private void writeIndexFile(String file,HealpixIndex map) throws Exception {
      cds.tools.Util.createPath(file);
      map.writeStream(new FileOutputStream(file) );
   }
   
   /** Construction d'une tuile terminale. Lit le fichier est map les entr�es de l'index
    * dans une TreeMap */
   private HealpixIndex createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
//      System.out.println("   createLeave("+file+")");
      HealpixIndex out = new HealpixIndex();
      out.loadStream( new FileInputStream(f));
      return out;
   }
   
   /** Construction d'une tuile interm�diaire � partir des 4 tuiles filles */
   private HealpixIndex createNode(HealpixIndex fils[]) throws Exception {
//      System.out.println("   createNode()");
    
      HealpixIndex out = new HealpixIndex();
      for( int i=0; i<4; i++ ) {
         if( fils[i]==null ) continue;
         if( fils[i].hasTooMany() ) { out.setTooMany(true); break; }
         out.putAll(fils[i]);
         fils[i]=null;
      }
      
      return out;
   }

}