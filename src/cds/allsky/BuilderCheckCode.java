// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.allsky;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import cds.aladin.Tok;
import cds.tools.Util;

/** Génération/Maj des Check codes (en fait des clés de hash) associés à un HiPS
 * en profite pour mettre à jour la taille du HiPS et le nombre total de tuiles
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Juillet 2022
 */
public class BuilderCheckCode extends Builder {
   protected int nbFile;         // Nombre de fichiers traités
   protected String format;    // Les formats des tuiles HiPS pris en compte
   protected String hipsCheckCode;   // Check code précalculé (s'il existe dans le fichier properties)
   long hipsEstsize;            // La taille totale du HiPS (sommes de toutes les tuiles + allsky) en bytes
   int hipsNbTiles;            // Le nombre total de tuiles (tous formats confondus)
   
   // Classe interne permettant de mémoriser les informations
   // au fur et à mesure de la génération du checkCode
   class Info  { 
      String fmt;    // Format concerné
      int n;         // Nombre de tuiles
      long length;   // taille cumulative (en bytes)
      int code;      // clé de hash = check code
      ArrayList<File> corruptedFile =null;  // Liste des fichiers corrompus (DATASUM);
      
      Info(String fmt) { this.fmt=fmt; code=0; length=0L; }
      
      void update( long v) {
         n++;
         length+=v;
         code = code*31 + (int)( v ^ (v >>> 32) );
      }
      
      // Ajout d'un fichier que l'on a détecté comme corrompu
      // (le DATASUM ne matche pas)
      void addCorruptedFile(File f) {
         if( corruptedFile==null ) corruptedFile = new ArrayList<>();
         corruptedFile.add(f);
      }
      
      // On ajoute sur le hash le nombre de fichiers
      // Puis Edition en entier 32 bits non signé
      String getCode() {
         return ((code*31+n) & 0xFFFFFFFFL )+"";
      }
   }

   public BuilderCheckCode(Context context) {
      super(context);
      nbFile=0;
      hipsEstsize=0L;
   }

   public Action getAction() { return Action.CHECKCODE; }

   public void run() throws Exception {
      hipsCheckCode = context.getCheckCode();
      StringBuilder r = null;
      
      // Passe en revue chaque jeu de tuiles, format par format
      Tok tok = new Tok(format," ,");
      while( tok.hasMoreTokens() ) {
         String fmt = tok.nextToken();
         Info info = scanDir(new File( context.getOutputPath() ),fmt);
         if( r==null ) r=new StringBuilder();
         else r.append(' ');
         r.append(fmt+":"+info.getCode());
         hipsEstsize += info.length;
         hipsNbTiles += info.n;
      }
      
      context.info("Full HiPS size: "+Util.getUnitDisk(hipsEstsize));
      context.info("Check codes: "+r);
      
      // Peut-on mémoriser le Check code ?
      if( context.getCheckForce() || hipsCheckCode==null ) {
         context.setCheckCode( r.toString() );
         context.info("Check codes and HiPS metrics stored/updated in properties file");
//         context.setPropriete("#hips_size", Util.getUnitDisk(hipsEstsize));
         context.setPropriete(Constante.KEY_HIPS_ESTSIZE, (hipsEstsize/1024L)+"" );
         context.setPropriete(Constante.KEY_HIPS_NB_TILES, hipsNbTiles+"" );
         context.writePropertiesFile();
      } else {
//         context.warning("Check codes not store. Use -clean option to overwrite it");
      }
   }
   
   // Scanning du format spécifié à partir du répertoire indiqué
   public Info scanDir(File dir, String fmt) throws Exception {
      context.info("Scanning "+fmt+" tiles...");
      Info info = new Info(fmt);
      scanDir(dir,fmt,info);
      context.info(info.n+" "+fmt+" files for "+Util.getUnitDisk(info.length));
      
      String flagOk="";
      if( hipsCheckCode!=null ) {
         String v = Context.getCheckCode(fmt, hipsCheckCode);
         flagOk = info.getCode().equals(v) ? " => identical":" => modified";
      }

      context.info("Check code for "+fmt+" tiles: "+info.getCode()+flagOk);
      return info;
   }
   
   public void validateContext() throws Exception {      
      validateOutput();

      context.loadProperties();
      
      format = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
      if( format==null ) format = context.prop.getProperty(Constante.OLD_HIPS_TILE_FORMAT);
      if( format==null ) throw new Exception("Out dir not a HiPS (or properties file missing)");
      
      format = format.replace("jpeg","jpg");
      
      String s = context.prop.getProperty(Constante.KEY_HIPS_CHECK_CODE);
      context.setCheckCode( s );
      
      validateContextMore();
   }
   
   protected void validateContextMore() throws Exception {
      
      // Peut-on recalculer les check codes ?
      if( !context.getCheckForce() ) {
         String s = context.getCheckCode();
//         if( s!=null ) context.warning("Check codes already stored ["+s+"]. Use -"+ParamOption.clean+" to overwrite it");
      }
   }
   
   public boolean isAlreadyDone() { return false; }

   public void showStatistics() {
      if( context instanceof ContextGui ) return;
      context.stat(nbFile+" file"+(nbFile>1?"s":"")+" scanned");
   }

   public boolean mustBeScanned(File f, String fmt) {
      String name = f.getName();
      if( !name.startsWith("Npix") 
            && !name.startsWith("Allsky.")) return false;
      if( !name.endsWith("."+fmt) )  return false;
      return true;
   }
   
   public void scanDir(File dir, String fmt, Info info) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      // répertoire
      if( dir.isDirectory() ) {
         File [] list = dir.listFiles();
         Arrays.sort(list);    // Important car cette liste n'est pas ordonnée tjrs de la même manière suivant OS
         for ( File f : list ) scanDir(f,fmt,info);
         
         if( Files.isSymbolicLink( dir.toPath()) ) {
            Path target = Files.readSymbolicLink( dir.toPath() );
            updateInfo( target.toFile(), info);
         }

         // simple fichier
      } else if( mustBeScanned(dir,fmt) ) {
         updateInfo( dir, info );
         nbFile++;
         context.setProgress(nbFile);
      }
   }
   
   protected void updateInfo(File f, Info info) throws Exception {
      long len = f.length();
      info.update( len );
   }
}
