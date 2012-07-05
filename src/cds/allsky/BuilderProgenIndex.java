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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeMap;

import cds.aladin.Aladin;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles d'index à partir des tuiles de plus bas
 * niveau. Le but est de donner accès aux progéniteurs
 * @author Pierre Fernique
 */
public class BuilderProgenIndex extends Builder {
   
   private int maxOrder;
   private int minOrder;

   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;

   /**
    * Création du générateur de l'arbre des index.
    * @param context
    */
   public BuilderProgenIndex(Context context) {
      super(context);
   }

   public Action getAction() { return Action.PROGEN; }

   public void run() throws Exception {
      build();
   }
   
   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateIndex();
      validateOrder( context.getHpxFinderPath() );
      maxOrder = context.getOrder();
      minOrder = maxOrder-2;
      if( minOrder<3 ) minOrder=3;
      context.info("Tree HEALPix index built for orders ["+minOrder+".."+maxOrder+"]");
      context.initRegion();
   }
   
   // Vérifie que le répertoire HpxIndex existe et peut être utilisé
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

   // Mise à jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction récursive de la hiérarchie des tuiles FITS à partir des tuiles FITS
    * de plus bas niveau. La méthode employée est la moyenne
    */
   private TreeMap<String,String> createTree(String path,int order, long npix ) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      // Si ni lui, ni ses frères sont dans le MOC, on passe
      boolean ok=false;
      long brother = npix - npix%4L;
      for( int i=0; i<4; i++ ) {
         ok = context.isInMocTree(order,brother+i);
         if( ok ) break;
      }
      if( !ok ) return null;
      
//      System.out.println("createTree("+order+","+npix+")...");
      
      String file = Util.getFilePath(path,order,npix);

      TreeMap<String,String> out = null;
      if( order==maxOrder ) out = createLeave(file);
      else {
         TreeMap<String,String> fils[] = new TreeMap[4];
         boolean found = false;
         for( int i =0; i<4; i++ ) {
            fils[i] = createTree(path,order+1,npix*4+i);
            if (fils[i] != null && !found) found = true;
         }
         if( found ) out = createNode(fils);
      }
      if( out!=null && context.isInMocTree(order,npix) && order<maxOrder) {
         writeIndexFile(file,out);
         Aladin.trace(4, "Writing " + file);

         if( order==maxOrder ) {
            File f = new File(file);
            updateStat(f);
         }
      }
      
      if( order<=minOrder ) return null;
      return out;
   }
   
   // Ecriture du fichier d'index HEALPix correspondant à la map passée en paramètre
   private void writeIndexFile(String file,TreeMap<String,String> map) throws Exception {
      cds.tools.Util.createPath(file);
      BufferedWriter out = new BufferedWriter( new FileWriter( new File(file) ) );
      for( String k : map.keySet() ) {out.write( map.get(k) ); out.write( "\n" ); }
      out.close();
   }
   
   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   private TreeMap<String, String> createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
//      System.out.println("   createLeave("+file+")");
      DataInputStream in = new DataInputStream(new BufferedInputStream( new FileInputStream(f)));
      TreeMap<String,String> out = new TreeMap<String,String>();
      String s;
      while( (s=in.readLine())!=null ) out.put( getKey(s),s);
      in.close();
      return out;
   }
   
   // Retourne la clé associée à une entrée dans un fichier d'index Healpix
   // Il s'agit : soit du champ "name", soit du champ "path" (sans l'extension optionnelle [x,y,w,h])
   // et sinon la ligne en totalité (toujours sans l'extension optionnelle)
   private String getKey(String s) {
      String key = cds.tools.Util.extractJSON("name", s);
      if( key==null ) {
         int first=-1;
         key = cds.tools.Util.extractJSON("path", s);
         if( key==null ) key=s;
         if( key.charAt(key.length()-1)==']' ) first = key.lastIndexOf('[');
         if( first>0 ) key = key.substring(0, first);
      }
      return key;
   }

   /** Construction d'une tuile intermédiaire à partir des 4 tuiles filles */
   private TreeMap<String,String> createNode(TreeMap<String,String> fils[]) throws Exception {
//      System.out.println("   createNode()");
    
      TreeMap<String,String> out = new TreeMap<String,String>();
      for( int i=0; i<4; i++ ) {
         if( fils[i]==null ) continue;
         out.putAll(fils[i]);
         fils[i]=null;
      }
      return out;
   }

}