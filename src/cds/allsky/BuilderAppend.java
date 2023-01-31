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

import cds.aladin.Tok;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

// JE N'AI PAS TERMINE - PF JUIN 2016
// IL FAUDRAIT ENCORE AJOUTER:
// - Récupérer automatiquement les bons paramètres (color=..) du HiPS target
// - Vérifier que ces paramètres sont bien tous compatibles avec les nouvelles images originales
// - Gérer les interruptions de traitement
// - Faire les tests
// - Ajouter la commande APPEND au Help de Hipsgen (décommenter)

/** Ajout d'observations à un HiPS déjà existant
 * @author Pierre Fernique
 */
public class BuilderAppend extends Builder {
   private String outputPath;
   private String inputPath;
   private ModeMerge mode;
   private boolean live=false;            // true si on doit utiliser les cartes de poids
   private String color;
   private String addHipsPath;
   private int order;
   private String skyval;
   private String skyvalues;
   private String pixelCut=null;
   private String dataRange=null;
   private int bitpix=-1;
   private double bzero=0;
   private double bscale=1;
   private double blank=Double.NaN;
   private String addendumId;

   public BuilderAppend(Context context) {
      super(context);
   }

   public Action getAction() { return Action.APPEND; }

   public void run() throws Exception {
      createAddHips();
      concatHips();
      removeAddHips();
   }
   
   
   private String hipsName=null;
   
   // Retourne le nom du HiPS temporaire
   private String getAddHipsName() {
      if( hipsName==null ) hipsName="AddsHips"+(System.currentTimeMillis()/1000L);
      return hipsName;
   }
   
   // Generation du HiPS additionnel
   private void createAddHips() throws Exception  {
      
      HipsGen hi = new HipsGen();
      String spart=" "+Param.partitioning+"="+(context.isPartitioning()?context.getPartitioning():"false");
      String spixelCut = pixelCut==null  ? "" : " \""+Param.pixelCut+"="+pixelCut+"\"";
      String sdataRange= dataRange==null ? "" : " \""+Param.dataRange+"="+dataRange+"\"";
      String sbitpix = "";
      String sformat= color!=null ? " "+Param.color+"="+color : "";
      if( bitpix!=-1 ) sbitpix=" "+Param.bitpix+"="+bitpix;
      String slive = live ? " "+Param.incremental+"=true":"";
      
      StringBuilder smode=new StringBuilder();
      ModeOverlay mo = context.getModeOverlay();
      if( !mo.equals(ModeOverlay.getDefault()) ) {
         if( smode.length()>0 ) smode.append(',');
         else smode.append(" "+Param.mode+"=");
         smode.append(mo);
      }
      ModeMerge mm = context.getModeMerge();
      if( !mm.equals(ModeMerge.getDefault()) ) {
         if( smode.length()>0 ) smode.append(',');
         else smode.append(" "+Param.mode+"=");
         smode.append(mm);
      }
      ModeTree mt = context.getModeTree();
      if( !mt.equals(ModeTree.getDefault(bitpix)) ) {
         if( smode.length()>0 ) smode.append(',');
         else smode.append(" "+Param.mode+"=");
         smode.append(mt);
      }
      
      String cmd = "in=\""+context.getInputPath()+"\" out=\""+addHipsPath
            +"\" "+Param.id+"="+addendumId
            +" "+Param.order+"="+order
            +slive
            +spart
            +smode
            +sformat
            +sbitpix
            +spixelCut
            +sdataRange
            +(skyvalues!=null ?" \""+Param.skyvalues+"="+skyvalues+"\""
                  : skyval!=null ?" "+Param.skyVal+"="+skyval:"")
            +" INDEX TILES";
      Tok tok = new Tok(cmd);
      String param [] = new String[ tok.countTokens() ];
      for(int i=0; tok.hasMoreTokens(); i++ ) param[i] = tok.nextToken();
      hi.execute(param);
      if( hi.context.isTaskAborting() ) {
         context.taskAbort();
         throw new Exception("Aborting");
      }
   }
   
   // Suppression du HiPS additinnel après le merge
   private void removeAddHips() throws Exception {
      context.info("Cleaning temporary HiPS "+addHipsPath+"...");
      cds.tools.Util.deleteDir( new File(addHipsPath) );
   }

   // Generation du HiPS additionnel
   private void concatHips() throws Exception  {
      HipsGen hi = new HipsGen();
      
      StringBuilder smode=new StringBuilder();
      ModeMerge mm = context.getModeMerge();
      if( !mm.equals(ModeMerge.getDefault()) ) {
         if( smode.length()>0 ) smode.append(',');
         else smode.append(" mode=");
         smode.append(mm);
      }
      ModeTree mt = context.getModeTree();
      if( !mt.equals(ModeTree.getDefault(bitpix)) ) {
         if( smode.length()>0 ) smode.append(',');
         else smode.append(" mode=");
         smode.append(mt);
      }
      
      String cmd = "out=\""+context.getOutputPath()+"\" in=\""+addHipsPath+"\""
            +smode
            +" CONCAT";
      Tok tok = new Tok(cmd);
      String param [] = new String[ tok.countTokens() ];
      for(int i=0; tok.hasMoreTokens(); i++ ) param[i] = tok.nextToken();
      hi.execute(param);
      if( hi.context.isTaskAborting() ) {
         context.taskAbort();;
         throw new Exception("Aborting");
      }
   }
   
  // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      outputPath = context.getOutputPath();
      inputPath = context.getInputPath();
      addHipsPath = cds.tools.Util.concatDir( outputPath,getAddHipsName());
      mode = context.getModeMerge();
      
      if( inputPath==null ) throw new Exception("\"in\" parameter required !");
      File f = new File(inputPath);
      if( !f.exists() ||  !f.canRead() ) throw new Exception("Input directory or image not available ["+inputPath+"]");
//      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("Input directory or image not available ["+inputPath+"]");

      skyval=getSkyVal(outputPath);
      if( skyval!=null && skyval.toLowerCase().equals("auto") ) {
         try {
            skyvalues = loadProperty(outputPath,Constante.KEY_HIPS_SKYVAL_VALUE);
         } catch( Exception e) {}
      }
      
      try {
         pixelCut = loadProperty(outputPath,Constante.KEY_HIPS_PIXEL_CUT);
      } catch( Exception e) {}
      try {
         dataRange = loadProperty(outputPath,Constante.KEY_HIPS_DATA_RANGE);
      } catch( Exception e) {}
      
      order=-1;
      try { 
         order = Util.getMaxOrderByPath( outputPath );
      } catch( Exception e ) { if( context.getVerbose()>=3 ) e.printStackTrace(); }
      
      if( order==-1 )  throw new Exception("No HiPS found in ouput dir");
      context.setOrder(order);
      int inputOrder = Util.getMaxOrderByPath( inputPath );
      if( inputOrder!=-1 )  throw new Exception("The input directory must contains original images/cubes. Use CONCAT if you want to merge two HiPS");
      context.info("Order retrieved from ["+outputPath+"] => "+order);
      
      // Recuperation des parametres du HiPS target pour generer le Hips a ajouter
      // avec les memes parametres
      try {
         double v[] = getBitpixBzeroBscaleBlank(outputPath);
         bitpix=(int)v[0];
         bzero=v[1];
         bscale=v[2];
         blank=v[3];
      } catch( Exception e1 ) { }
      
      // Info sur la méthode
      if( mode==ModeMerge.copy || mode==ModeMerge.link ) {
         throw new Exception("Coadd mode ["+mode+"] not supported for APPEND action");
      }
      context.info("Coadd mode: "+ModeMerge.getExplanation(mode));

      
      // Il faut voir si on peut utiliser les tuiles de poids
      live = checkLiveByProperties(context.getOutputPath());
      if( mode==ModeMerge.mergeMean ) {
         if( !live ) context.warning("Target HiPS does not provide weight tiles => assuming weigth 1 for each output pixel");
      }
      
      color = getColorFormat(context.getOutputPath());
      
      // On s'invente un ID pour l'ajout
      addendumId = "APPEND/P/"+System.currentTimeMillis()/1000;
   }
   
   /** Retourne le format des tuiles si HiPS couleur, sinon null */
   protected String getColorFormat(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_DATAPRODUCT_SUBTYPE);
         if( s==null ||  s.indexOf("color")<0 ) return null;
         return loadProperty(path,Constante.KEY_HIPS_TILE_FORMAT);
      } catch( Exception e ) {}
      return null;
   }

   /** Vérifie que les 2 hips disposent des cartes de poids */
   protected boolean checkLiveByProperties(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_DATAPRODUCT_SUBTYPE);
         return s!=null && s.indexOf("live")>=0;
      } catch( Exception e ) {}
      return false;
   }

   /** Récupère la variable du skyval */
   protected String getSkyVal(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_HIPS_SKYVAL);
         return s;
      } catch( Exception e ) {}
      return null;
   }

   /** Récupère les variables pour faire le même traitement BITPIX, BZERO,BSCALE,BLANK */
   protected double [] getBitpixBzeroBscaleBlank(String path) throws Exception {
      String filename = context.findOneNpixFile(path);
      Fits f = new Fits();
      f.loadFITS(filename);
      return new double[] { f.bitpix, f.bzero, f.bscale, f.blank };
   }


}
