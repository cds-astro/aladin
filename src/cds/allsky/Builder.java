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
import java.util.Properties;

import cds.aladin.Localisation;
import cds.aladin.MyProperties;
import cds.aladin.PlanHealpix;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;


/**
 * Classe abstraite d'écrivant les actions possibles (voir la méthode Task)
 * @author Pierre Fernique [CDS]
 *
 */
public abstract class Builder {
   
   protected Context context;
   protected Builder(Context context) { this.context=context; }
   
   /** Constructeur général pour toutes les actions possibles */
   static public Builder createBuilder(Context context,Action action) throws Exception {
      switch(action) {
         case INDEX:     return new BuilderIndex(context);
         case TILES:     return new BuilderTiles(context);
         case ALLSKY:    return new BuilderAllsky(context);
         case JPEG:      return new BuilderJpg(context);
         case PNG:       return new BuilderPng(context);
         case MOC:       return new BuilderMoc(context);
         case MOCINDEX:  return new BuilderMocIndex(context);
         case CLEAN:     return new BuilderClean(context);
         case CLEANINDEX:return new BuilderCleanIndex(context);
         case CLEANDETAILS:return new BuilderCleanDetails(context);
         case CLEANTILES:return new BuilderCleanTiles(context);
         case CLEANFITS: return new BuilderCleanFits(context);
         case CLEANJPEG: return new BuilderCleanJpg(context);
         case CLEANPNG:  return new BuilderCleanPng(context);
         case CHECK:     return new BuilderCheck(context);
         case GZIP:      return new BuilderGzip(context);
         case GUNZIP:    return new BuilderGunzip(context);
         case RGB:       return new BuilderRgb(context);
         case TREE:      return new BuilderTree(context);
         case CONCAT:    return new BuilderConcat(context);
         case CUBE:      return new BuilderCube(context);
         case DETAILS:   return new BuilderDetails(context);
         case MAPTILES:  return new BuilderMapTiles(context);
         default: break;
      }
      throw new Exception("No builder associated to this action");
   }

   /** Valide les préconditions à l'exécution de la tâche */
   public abstract void validateContext() throws Exception;
   
   /** Retourne true si l'exécution de la tâche est inutile (ex: déjà faite) */
   public boolean isAlreadyDone() { return false; }
   
   /** Exécute la tâche */
   public abstract void run() throws Exception;
   
   /** Retourne l'identificateur de l'action */
   public abstract Action getAction();
   
   /** Affiche des statistiques de progression */
   public void showStatistics() { }
   
   /** Indique le mode Just-print - not run -> retourne true si c'est le cas avec un message d'info */
   public boolean isFake() {
      if( !context.fake ) return false;
      context.info("Action "+getAction()+" not run due to the -n option");
      return true;
   }
   
   // Quelques validateurs génériques utilisés par les différents Builders.
   
   // Vérifie que le répertoire Input a été passé en paramètre et est utilisable
   protected void validateInput() throws Exception {
      if( context.isValidateInput() ) return;
      String input = context.getInputPath();
      if( input==null ) throw new Exception("Argument \"input\" is required");
      File f = new File(input);
      if( !f.isDirectory() || !f.canRead()) throw new Exception("Input directory not available ["+input+"]");
      context.setValidateInput(true);
   }
   
   // Vérifie que le répertoire Output a été passé en paramètre, sinon essaye de le déduire
   // du répertoire Input en ajoutant le suffixe ALLSKY
   // S'il existe déjà, vérifie qu'il s'agit bien d'un répertoire utilisable
   protected void validateOutput() throws Exception {
      if( context.isValidateOutput() ) return;
      String output = context.getOutputPath();
      if( output==null ) {
         output = context.getInputPath() + Constante.HIPS;
         context.setOutputPath(output);
         context.info("the output directory will be "+output);
      }
      File f = new File(output);
      if( f.exists() && (!f.isDirectory() || !f.canWrite() || !f.canRead())) throw new Exception("Ouput directory not available ["+output+"]");
      context.setValidateOutput(true);
   }
   
   // Récupère l'ordre en fonction d'un répertoire. Si un order particulier a été passé en paramètre,
   // vérifie sa cohérence avec celui trouvé
   protected void validateOrder(String path) throws Exception {
      int order = context.getOrder();
      int orderIndex = Util.getMaxOrderByPath( path );
      if( order==-1 || context instanceof ContextGui ) {
         context.info("Order retrieved from ["+path+"] => "+orderIndex);
         context.setOrder(orderIndex);
      } else if( order!=orderIndex ) throw new Exception("Detected order ["+orderIndex+"] does not correspond to the param order ["+order+"]");
   }
   
   /** Récupération de la profondeur (cube) */
   protected void validateDepth() throws Exception {
      
      if( context.depthInit ) return;
      
      // tentative de récupération de la profondeur par une image étalon
      String img = context.getImgEtalon();
      if( img==null && context.getInputPath()!=null) {
         img = context.justFindImgEtalon( context.getInputPath() );
         if( img!=null ) context.info("Use this reference image => "+img);
      }
      if( img!=null ) {
         try { context.setImgEtalon(img); }
         catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
      }
      
      // Tentative de récupération de la profondeur par le fichier des properties
      if( !context.depthInit ) {
         try {
            context.loadProperties();
            String s = (String)context.prop.get(PlanHealpix.KEY_CUBEDEPTH);
            if( s!=null ) {
               int depth = Integer.parseInt(s);
               if( depth>1 ) context.setDepth( depth );
            }
         } catch( Exception e ) { context.warning("Propertie file problem => "+e.getMessage()); }
      }
      
      if( context.depthInit && context.depth>1 ) context.info("Working on HiPS cube => depth="+context.depth);
   }
   
   // Valide les cuts passés en paramètre, ou à défaut cherche à en obtenir depuis une image étalon
   protected void validateCut() throws Exception {
      if( context.isValidateCut() ) return;
      double [] cut = null;
//      double [] cut = context.getCut();
      
      double [] pixelGood = context.pixelGood;
      boolean missingGood = pixelGood!=null && context.good!=null;
      
      boolean missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      boolean missingRange = cut==null || cut[2]==0 && cut[3]==0;

      // S'il y a des pixelRange et/ou pixelCut indiqués sur la ligne de commande, il faut les convertir
      // en cut[] à récupérant le bzero et le bscale depuis le fichier Allsky.fits ou depuis une image étalon
      double [] pixelRangeCut = context.getPixelRangeCut();
      if( (missingCut  || missingRange ) && pixelRangeCut!=null || missingGood ) {
         try {
            setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
         } catch( Exception e ) {

            String img = context.getImgEtalon();
            if( img==null && context.getInputPath()!=null) {
               img = context.justFindImgEtalon( context.getInputPath() );
               if( img!=null ) context.info("Use this reference image => "+img);
            }
            if( img!=null ) {
               try { context.setImgEtalon(img); }
               catch( Exception e1) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
            }
         }

         try {
            if( cut==null ) cut = new double[5];
            for( int i=0; i<4; i++ ) {
               if( Double.isNaN(pixelRangeCut[i]) ) continue;
               cut[i] = (pixelRangeCut[i] - context.bzero)/context.bscale;
//               System.out.println("Retreiving from user pixelRangeCut["+i+"]="+pixelRangeCut[i]+" => cut["+i+"]="+cut[i]);
            }
            
            if( missingGood ) {
               context.good = new double[2];
               for( int i=0; i<2; i++ ) {
                  context.good[i] = (context.pixelGood[i] - context.bzero)/context.bscale;
               }
            }

         } catch( Exception e ) {
            throw new Exception("Cannot retrieve BZERO & BSCALE from previous Allsky.fits file or reference image");
         }
      }
      
      // S'il me manque le cut du pixelCut ou du pixelRange, il faut que je récupère une image étalon
      // que j'en déduise les cutOrig, bzeroOrig, bscaleOrig, puis que j'en calcule le bzero, bscale et donc les cut
      missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      if( missingCut ) {
         String img = context.getImgEtalon();
         if( img==null && context.getInputPath()!=null) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img!=null ) {
            try { context.setImgEtalon(img); }
            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
         }
         
         context.initParameters();
         
         double [] imgCut = context.getCut();
         if( cut==null ) cut = new double[5];
         if( missingCut )   {
            cut[0]= imgCut[0];
            cut[1]= imgCut[1]; 
            if( cut[0]!=cut[1] ) context.info("Estimating pixel cut from the reference image => ["+cut[0]+" .. "+cut[1]+"]");
         }
      }
      
      // S'il me manque toujours le pixelCut, je vais tenter de les récupérer par le fichier des properties
      missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      if( missingCut ) {
         try {
            String propFile = context.getOutputPath()+Util.FS+PlanHealpix.PROPERTIES;
            MyProperties prop = new MyProperties();
            File f = new File( propFile );
            if( f.exists() ) {
               FileInputStream in = new FileInputStream(propFile); 
               prop.load(in);
               in.close();
               String s = (String)prop.get(PlanHealpix.KEY_PIXELCUT);
               context.setPixelCut(s);
               pixelRangeCut = context.getPixelRangeCut();
               
               // Il me faut alors BZERO  et BSCALE
               try {
                  setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
                  if( cut==null ) cut = new double[5];
                  for( int i=0; i<4; i++ ) {
                     if( Double.isNaN(pixelRangeCut[i]) ) continue;
                     cut[i] = (pixelRangeCut[i] - context.bzero)/context.bscale;
                  }
                  context.info("Pixel cut from the propertie file => ["+ip(cut[0],context.bzero,context.bscale)+" .. "+ip(cut[1],context.bzero,context.bscale)+"]");
               } catch( Exception e ) { }
            }
         } catch( Exception e ) {}
        
      }
      
      context.setCut(cut);
      
      double bz=context.bzero;
      double bs=context.bscale;
      if( cut==null || cut[0]==0 && cut[1]==0 ) throw new Exception("Argument \"pixelCut\" required");
      if( !( cut[0] < cut[1] ) ) throw new Exception("pixelCut error ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.info("pixel cut ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.setValidateCut(true);
   }
   
   
   protected void validateBitpix() {
      if( context.bitpix!=-1 ) return;
      try {
         setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
      } catch( Exception e ) { }
   }
   
   protected void validateLabel() {
      if( context.label!=null ) return;
      String label = getALabel(context.getOutputPath());
      if( label!=null && label.length()>0 ) context.label=label;
   }
   
   protected String getALabel(String path) {
      String label=null;
      
      // Je vais essayé de le récupérer depuis le fichier des propriétés
      try {
         String propFile = path+Util.FS+PlanHealpix.PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            FileInputStream in = new FileInputStream(propFile); 
            prop.load(in);
            in.close();
            String s = (String)prop.get(PlanHealpix.KEY_LABEL);
            if( s!=null && s.length()>0 ) label=s;
         }
      } catch( Exception e ) { }
      if( label!=null ) return label;
      
      // Je vais le construire à partir du nom du répertoire
      int offset = path.lastIndexOf(Util.FS);
      if( offset<0 ) offset = path.lastIndexOf('/');
      if( offset>=0 ) label=path.substring(offset+1);
      else label=path;
      
      return label;
   }
   
   // Retourne le code HEALPix correspondant au système de référence des coordonnées
   // du survey HEALPix
   protected String getFrame() {
      if( context.hasFrame() ) return context.getFrameCode();
      try {
         if( context.prop==null ) context.loadProperties();
         return context.prop.getProperty(PlanHealpix.KEY_COORDSYS, "C");
      } catch( Exception e ) { e.printStackTrace(); }
      return context.getFrameCode();
   }
   
//   protected void validateFrame() {
//      String path = context.getOutputPath();
//      String coordsys=null;
//      
//      // Je vais essayé de le récupérer depuis le fichier des propriétés
//      try {
//         String propFile = path+Util.FS+PlanHealpix.PROPERTIES;
//         MyProperties prop = new MyProperties();
//         File f = new File( propFile );
//         if( f.exists() ) {
//            FileInputStream in = new FileInputStream(propFile); 
//            prop.load(in);
//            in.close();
//            String s = (String)prop.get(PlanHealpix.KEY_COORDSYS);
//            if( s!=null && s.length()>0 ) coordsys=s;
//         }
//      } catch( Exception e ) { }
//      if( coordsys==null ) context.frame = Localisation.GAL;
//      else context.setFrameName(coordsys);
//   }
   

//   // Valide les cuts passés en paramètre, ou à défaut cherche à en obtenir depuis une image étalon
//   protected void validateCut() throws Exception {
//      if( context.isValidateCut() ) return;
//      double [] cutOrig;
//      double [] memoCutOrig = context.getCutOrig();
//      boolean flagGaffe= false;   // true s'il faut s'assurer qu'on a pu récupérer le BSCALE,BZERO 
//                                  // soit d'un précédent Allsky.fits, soit d'une image étalon
//      
//      // Attention, les cuts positionnés manuellement doivent
//      // être exprimés en raw (récupération du bscaleOrig/bzeroOrig)
//      double [] cutOrigBefore = context.cutOrigBefore;
//      if( cutOrigBefore!=null ) {
//         try {
//            if( !context.bscaleBzeroOrigSet ) {
//               setBzeroBscaleOrigFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
//            }
//            memoCutOrig = new double[4];
//            for( int i=0; i<4; i++ ) {
//               if( Double.isNaN(cutOrigBefore[i]) ) continue;
//               memoCutOrig[i] = (cutOrigBefore[i] - context.bZeroOrig)/context.bScaleOrig;
//               //            System.out.println("cutOrigBefore["+i+"]="+cutOrigBefore[i]+" => cutOrig["+i+"]="+memoCutOrig[i]);
//            }
//         } catch( Exception e ) {
//            flagGaffe=true;
//         }
//      }
//
//      // Pas de pixelCut positionnés, ou pas de dataCut positionnés
//      if( memoCutOrig==null || memoCutOrig[2]==0 && memoCutOrig[3]==0 ) {
//         String img = context.getImgEtalon();
//         if( img==null && context.getInputPath()!=null) {
//            img = context.justFindImgEtalon( context.getInputPath() );
//            context.info("Use this reference image => "+img);
//         }
//         if( img!=null ) {
//            try { context.setImgEtalon(img); }
//            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
//         }
//         
//         // Replacement des pixelCut paramétrés par l'utilisateur
//         if( memoCutOrig!=null && memoCutOrig[2]==0 && memoCutOrig[3]==0 ) {
//            cutOrig = context.getCutOrig();
//            if( cutOrig==null ) cutOrig = new double[4];
//            cutOrig[0]=memoCutOrig[0];
//            cutOrig[1]=memoCutOrig[1];
//            context.setCutOrig(cutOrig);
//         }
//      }
//      
//      if( context.cutOrigBefore==null && flagGaffe ) {
//         throw new Exception("Cannot retrieve BZERO & BSCALE from original images, nor from previous Allsky.fits file");
//      }
//      
//      cutOrig = context.getCutOrig();
//      double bz=context.bZeroOrig;
//      double bs=context.bScaleOrig;
//      if( cutOrig==null ) throw new Exception("Argument \"pixelCut\" required");
////      if( cutOrig[2]==0 && cutOrig[3]==0 ) throw new Exception("Argument \"pixelRange\" required");
//      if( !( cutOrig[0] < cutOrig[1] ) ) 
//         throw new Exception("pixelCut error ["+ip(cutOrig[0],bz,bs)+" .. "+ip(cutOrig[1],bz,bs)+"]");
////      if( !( cutOrig[2] <= cutOrig[0] && cutOrig[0] < cutOrig[1] && cutOrig[1]<=cutOrig[3]) ) {
////         context.warning("Adjusting pixelRange with pixelCut ["+ip(cutOrig[2],bz,bs)+" .. "+ip(cutOrig[3],bz,bs)+"] => ["+ip(cutOrig[0],bz,bs)+" .. "+ip(cutOrig[1],bz,bs)+"]");
////         if( cutOrig[2] > cutOrig[0] ) cutOrig[2]=cutOrig[0];
////         if( cutOrig[1] > cutOrig[3] ) cutOrig[3]=cutOrig[1];
////      }
//      context.info("Pixel range ["+ip(cutOrig[2],bz,bs)+" .. "+ip(cutOrig[3],bz,bs)+"], pixel cut ["+ip(cutOrig[0],bz,bs)+" .. "+ip(cutOrig[1],bz,bs)+"]");
//      context.setValidateCut(true);
//   }

   
   protected String ip(double raw,double bzero,double bscale) {
      return cds.tools.Util.myRound(raw) + (bzero!=0 || bscale!=1 ? "/"+cds.tools.Util.myRound(raw*bscale+bzero) : "");
   }
   
   /**
    * Initialisation des paramètres FITS à partir d'un Allsky.fits précédent
    */
   protected void setFitsParamFromPreviousAllsky(String allskyFile) throws Exception {
      Fits f = new Fits();
      
//      f.loadHeaderFITS(allskyFile);
//      double cutOrig[] = new double[4];
//      cutOrig[0] = f.headerFits.getDoubleFromHeader("PIXELMIN");
//      cutOrig[1] = f.headerFits.getDoubleFromHeader("PIXELMAX");
//      cutOrig[2] = f.headerFits.getDoubleFromHeader("DATAMIN");
//      cutOrig[3] = f.headerFits.getDoubleFromHeader("DATAMAX");
      
      f.loadFITS(allskyFile);
      double [] cut = f.findAutocutRange(0,0,true);
      
      context.setBitpix(f.bitpix);
      context.setCut(cut);
      try {
         double blank = f.headerFits.getDoubleFromHeader("BLANK");
         context.blank=blank;
      } catch( Exception e ) { }
      try {
         double bzero = f.headerFits.getDoubleFromHeader("BZERO");
         context.bzero=bzero;
      } catch( Exception e ) { }
      try {
         double bscale = f.headerFits.getDoubleFromHeader("BSCALE");
         context.bscale=bscale;
      } catch( Exception e ) { }
    
   }
   
   protected void setBzeroBscaleFromPreviousAllsky(String allskyFile) throws Exception {
      Fits f = new Fits();
      f.loadHeaderFITS(allskyFile);
      try {
         double bzero = f.headerFits.getDoubleFromHeader("BZERO");
         context.bzero=bzero;
      } catch( Exception e ) { }
      try {
         double bscale = f.headerFits.getDoubleFromHeader("BSCALE");
         context.bscale=bscale;
      } catch( Exception e ) { }
      
      // J'en profite
      if( context.bitpix==-1 ) {
         try {
            double bitpix = f.headerFits.getDoubleFromHeader("BITPIX");
            context.bitpix=(int)bitpix;
         } catch( Exception e ) { }

      }
   }
}
