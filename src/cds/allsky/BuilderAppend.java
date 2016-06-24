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

import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.tools.pixtools.Util;

// JE N'AI PAS TERMINE - PF JUIN 2016
// IL FAUDRAIT ENCORE AJOUTER:
// - R�cup�rer automatiquement les bons param�tres (color=..) du HiPS target
// - V�rifier que ces param�tres sont bien tous compatibles avec les nouvelles images originales
// - G�rer les interruptions de traitement
// - Faire les tests
// - Ajouter la commande APPEND au Help de Hipsgen (d�commenter)

/** Ajout d'observations � un HiPS d�j� existant
 * @author Pierre Fernique
 */
public class BuilderAppend extends Builder {
   private String outputPath;
   private String inputPath;
   private Mode mode;
   private boolean live=false;            // true si on doit utiliser les cartes de poids
   private String addHipsPath;
   private int order;
   private String skyval;
   private String skyvalues;
   private String bitpix;

   public BuilderAppend(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CONCAT; }

   public void run() throws Exception {
      createAddHips();
      concatHips();
      removeAddHips();
   }
   
   // Generation du HiPS additionnel
   private void createAddHips() throws Exception  {
      HipsGen hi = new HipsGen();
      String cmd = "in=\""+context.getInputPath()+"\" out=\""+addHipsPath
            +"\" order="+order
            +" bitpix="+bitpix
            +" -live"
            +(skyvalues!=null ?" \"skyvalues="+skyvalues+"\"": skyval!=null ?" skyval="+skyval:"")
            +" INDEX TILES";
      Tok tok = new Tok(cmd);
      String param [] = new String[ tok.countTokens() ];
      for(int i=0; tok.hasMoreTokens(); i++ ) param[i] = tok.nextToken();
      hi.execute(param);
   }
   
   // Suppression du HiPS additinnel apr�s le merge
   private void removeAddHips() throws Exception {
      context.info("Cleaning temporary HiPS "+addHipsPath+"...");
      cds.tools.Util.deleteDir( new File(addHipsPath) );
   }

   // Generation du HiPS additionnel
   private void concatHips() throws Exception  {
      HipsGen hi = new HipsGen();
      String cmd = "out=\""+context.getOutputPath()+"\" in=\""+addHipsPath+"\" CONCAT";
      Tok tok = new Tok(cmd);
      String param [] = new String[ tok.countTokens() ];
      for(int i=0; tok.hasMoreTokens(); i++ ) param[i] = tok.nextToken();
      hi.execute(param);
   }
  // Valide la coh�rence des param�tres
   public void validateContext() throws Exception {
      outputPath = context.getOutputPath();
      inputPath = context.getInputPath();
      addHipsPath = cds.tools.Util.concatDir( outputPath,"AddsHiPS");
      mode = context.getMode();
      
      if( inputPath==null ) throw new Exception("\"in\" parameter required !");
      File f = new File(inputPath);
      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("\"inputPath\" directory not available ["+inputPath+"]");

      skyval=getSkyVal(outputPath);
      if( skyval!=null && skyval.toLowerCase().equals("true") ) {
         try {
            skyvalues = loadProperty(outputPath,Constante.KEY_HIPS_SKYVAL_VALUE);
         } catch( Exception e) {}
      }
      
      bitpix=getBitpix(outputPath);

      order = Util.getMaxOrderByPath( outputPath );
      if( order==-1 )  throw new Exception("No HiPS found in ouput dir");
      context.setOrder(order);
      int inputOrder = Util.getMaxOrderByPath( inputPath );
      if( inputOrder!=-1 )  throw new Exception("The input directory must contains original images/cubes. Use CONCAT if you want to merge two HiPS");
      context.info("Order retrieved from ["+outputPath+"] => "+order);
      
      // Il faut voir si on peut utiliser les tuiles de poids
      live = checkLiveByProperties(context.getOutputPath());
      if( mode==Mode.AVERAGE ) {
         if( !live ) context.warning("Target HiPS does not provide weight tiles => assuming weigth 1 for each output pixel");
      }
   }
   
   /** V�rifie que les 2 hips disposent des cartes de poids */
   protected boolean checkLiveByProperties(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_DATAPRODUCT_SUBTYPE);
         return s!=null && s.indexOf("live")>=0;
      } catch( Exception e ) {}
      return false;
   }

   /** R�cup�re la variable du skyval */
   protected String getSkyVal(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_HIPS_SKYVAL);
         return s;
      } catch( Exception e ) {}
      return null;
   }

   /** R�cup�re la variable du skyval */
   protected String getBitpix(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_HIPS_PIXEL_BITPIX);
         return s;
      } catch( Exception e ) {}
      return null;
   }

   /** Charge une valeur d'un mot cl� d'un fichier de properties pour un r�pertoire particulier, null sinon */
   protected String loadProperty(String path,String key) throws Exception {
      MyProperties prop = loadProperties(path);
      if( prop==null ) return null;
      return prop.getProperty(key);
   }
   
   /** Charge les propri�t�s d'un fichier properties, retourne null si probl�me */
   protected MyProperties loadProperties(String path) {
      FileInputStream in = null;
      try {
         String propFile = path+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            in = new FileInputStream(propFile);
            prop.load(in);
            in.close();
            in=null;
            return prop;
         }
      } 
      catch( Exception e ) {}
      finally { if( in!=null ) try { in.close(); } catch( Exception e ) {} }
      return null;
   }
}