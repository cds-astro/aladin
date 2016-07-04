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
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/**
 * Classe abstraite d'�crivant les actions possibles (voir la m�thode Task)
 * @author Pierre Fernique [CDS]
 *
 */
public abstract class Builder {

   protected Builder b=null;                 // Subtilit� pour faire afficher des statistiques

   protected Context context;
   protected Builder(Context context) { this.context=context; }

   /** Constructeur g�n�ral pour toutes les actions possibles */
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
         case CLEANDATE: return new BuilderCleanDate(context);
         case CLEANWEIGHT:return new BuilderCleanWeight(context);
         case CHECK:     return new BuilderCheck(context);
         case GZIP:      return new BuilderGzip(context);
         case GUNZIP:    return new BuilderGunzip(context);
         case RGB:       return new BuilderRgb(context);
         case TREE:      return new BuilderTree(context);
         case CONCAT:    return new BuilderConcat(context);
         case APPEND:    return new BuilderAppend(context);
         case CUBE:      return new BuilderCube(context);
         case DETAILS:   return new BuilderDetails(context);
         case MAPTILES:  return new BuilderMapTiles(context);
         case UPDATE:    return new BuilderUpdate(context);
         case PROP:      return new BuilderProp(context);
         case MIRROR:    return new BuilderMirror(context);
         case MAP:       return new BuilderMap(context);
         default: break;
      }
      throw new Exception("No builder associated to this action");
   }

   /** Valide les pr�conditions � l'ex�cution de la t�che */
   public abstract void validateContext() throws Exception;

   /** Retourne true si l'ex�cution de la t�che est inutile (ex: d�j� faite) */
   public boolean isAlreadyDone() { return false; }

   /** Ex�cute la t�che */
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

   // Quelques validateurs g�n�riques utilis�s par les diff�rents Builders.

   // V�rifie que le r�pertoire Input a �t� pass� en param�tre et est utilisable
   protected void validateInput() throws Exception {
      if( context.isValidateInput() ) return;
      String input = context.getInputPath();
      if( input==null ) throw new Exception("Argument \"input\" is required");
      File f = new File(input);
      if( !f.canRead()) throw new Exception("Input not available ["+input+"]");
      if( f.isFile() ) {
         context.info("Unique input image detected");
         context.setFlagInputFile(true);
      }
      if( context.isExistingAllskyDir(input) && context.hasPropertyFile(input) ) {
         throw new Exception("The input directory must be a image collection, not a HiPS => aborted");
      }
      context.setValidateInput(true);
   }

   // V�rifie que le r�pertoire Output a �t� pass� en param�tre, sinon essaye de le d�duire
   // du r�pertoire Input en ajoutant le suffixe ALLSKY
   // S'il existe d�j�, v�rifie qu'il s'agit bien d'un r�pertoire utilisable
   protected void validateOutput() throws Exception { 
      String output = context.getOutputPath();
      if( output==null ) {
         output = context.getInputPath();
         if( output!=null && (output.startsWith("http://") || output.startsWith("https://"))) {
            output = context.getInputPath();
            int n = output.length();
            if( output.charAt(n-1)=='/' ) n--;
            int offset = output.lastIndexOf('/',n);
            output = output.substring(offset+1,n);
         } else {
            
            String id = context.getHipsId();
            
            // Pas d'id => On ajoute simplement le suffixe HiPS au r�pertoire d'origine
            if( id==null ) {
               output = (output==null?"":output) + Constante.HIPS;
               
            // Un Id => on l'utilise comme nom de r�pertoire cible (avec des _ � la place des / et ?)
            } else {
               output = context.getInputPath();
               output = output.replace('\\', '/');
               int n = output.length();
               if( output.charAt(n-1)=='/' ) n--;
               int offset = output.lastIndexOf('/',n);
               output = output.substring(0,offset+1);
               id = id.substring(6);
               id = id.replace('/','_');
               id = id.replace('?','_');
               output = output+id;
            }
            
         }
         context.setOutputPath(output);
         context.info("the output directory will be "+output);
      }
      File f = new File(output);
      if( f.exists() && (!f.isDirectory()  || !f.canRead())) throw new Exception("Ouput directory not available ["+output+"]");
      context.setValidateOutput(true);
   }

   // R�cup�re l'ordre en fonction d'un r�pertoire. Si un order particulier a �t� pass� en param�tre,
   // v�rifie sa coh�rence avec celui trouv�
   protected void validateOrder(String path) throws Exception {
      int order = context.getOrder();
      int orderIndex = Util.getMaxOrderByPath( path );
      if( order==-1 || context instanceof ContextGui ) {
         context.info("Order retrieved from ["+path+"] => "+orderIndex);
         context.setOrder(orderIndex);
      } else if( orderIndex!=-1 && order!=orderIndex ) throw new Exception("Detected order ["+orderIndex+"] does not correspond to the param order ["+order+"]");
   }

   /** R�cup�ration de la profondeur (cube) */
   protected void validateDepth() throws Exception {

      if( context.depthInit ) return;

      // tentative de r�cup�ration de la profondeur par une image �talon
      if( !context.isColor() ) {
         String img = context.getImgEtalon();
         if( img==null && context.getInputPath()!=null) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img!=null ) {
            try { context.setImgEtalon(img); }
            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
         }
      }

      // Tentative de r�cup�ration de la profondeur par le fichier des properties
      if( !context.depthInit ) {
         try {
            context.loadProperties();
            String s = context.prop.getProperty(Constante.KEY_CUBE_DEPTH);
            if( s==null ) s = context.prop.getProperty(Constante.OLD_CUBE_DEPTH);
            if( s!=null ) {
               int depth = Integer.parseInt(s);
               if( depth>1 ) context.setDepth( depth );
            }
         } catch( Exception e ) { context.warning("Propertie file problem => "+e.getMessage()); }
      }

      if( context.depthInit && context.depth>1 ) {
         String s="";
         if( context.isCubeCanal() ) {
            s=" (crpix3="+context.crpix3+" crval3="+context.crval3+" cdelt3="+context.cdelt3+")";
         }
         context.info("Working on HiPS cube => depth="+context.depth+s);
      }
   }

   // Valide les cuts pass�s en param�tre, ou � d�faut cherche � en obtenir depuis une image �talon
   protected void validateCut() throws Exception {
      if( context.isValidateCut() ) return;
      double [] cut = null;
      //      double [] cut = context.getCut();

      double [] pixelGood = context.pixelGood;
      boolean missingGood = pixelGood!=null && context.good!=null;

      boolean missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      boolean missingRange = cut==null || cut[2]==0 && cut[3]==0;

      // S'il y a des pixelRange et/ou pixelCut indiqu�s sur la ligne de commande, il faut les convertir
      // en cut[] � r�cup�rant le bzero et le bscale depuis le fichier Allsky.fits ou depuis une image �talon
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

      // S'il me manque le cut du pixelCut ou du pixelRange, il faut que je r�cup�re une image �talon
      // que j'en d�duise les cutOrig, bzeroOrig, bscaleOrig, puis que j'en calcule le bzero, bscale et donc les cut
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

      // S'il me manque toujours le pixelCut, je vais tenter de les r�cup�rer par le fichier des properties
      missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      if( missingCut ) updateCutByProperties(cut);

      context.setCut(cut);

      double bz=context.bzero;
      double bs=context.bscale;
      if( cut==null || cut[0]==0 && cut[1]==0 ) throw new Exception("Argument \"pixelCut\" required");
      if( !( cut[0] < cut[1] ) ) throw new Exception("pixelCut error ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.info("pixel cut ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.setValidateCut(true);
   }


   /** Met � jour les champs manquants du cut[] via les infos du fichier des properties */
   protected void updateCutByProperties(double [] cut) {
      try {
         String propFile = context.getOutputPath()+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            FileInputStream in = new FileInputStream(propFile);
            prop.load(in);
            in.close();
            String s = prop.getProperty(Constante.KEY_HIPS_PIXEL_CUT);
            if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_PIXEL_CUT);
            context.setPixelCut(s);
            double [] pixelRangeCut = context.getPixelRangeCut();

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


   protected void validateBitpix() {
      if( context.bitpix!=-1 ) return;
      try {
         setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
      } catch( Exception e ) { }
   }

   protected void validateLabel() {
      if( context.getLabel()!=null ) return;
      String label = getALabel(context.getOutputPath(),context.getInputPath());
      if( label!=null && label.length()>0 ) context.label=label;
   }

   protected String getALabel(String outputPath,String inputPath) {
      String label=null;

      // Je vais essay� de le r�cup�rer depuis le fichier des propri�t�s
      try {
         String propFile = outputPath+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            FileInputStream in = new FileInputStream(propFile);
            prop.load(in);
            in.close();
            String s = prop.getProperty(Constante.KEY_OBS_COLLECTION);
            if( s==null ) s = prop.getProperty(Constante.OLD_OBS_COLLECTION);
            if( s!=null && s.length()>0 ) label=s;
         }
      } catch( Exception e ) { }
      if( label!=null ) return label;

      // Je vais le construire � partir du nom du r�pertoire
      if( inputPath!=null ) {
         int offset = inputPath.lastIndexOf(Util.FS);
         if( offset<0 ) offset = inputPath.lastIndexOf('/');
         if( offset>=0 ) label=inputPath.substring(offset+1);
         else label=inputPath;
      } else {
         int offset = outputPath.lastIndexOf(Util.FS);
         if( offset<0 ) offset = outputPath.lastIndexOf('/');
         if( offset>=0 ) label=outputPath.substring(offset+1);
         else label=outputPath;
      }

      return label;
   }

   // Retourne le code HEALPix correspondant au syst�me de r�f�rence des coordonn�es
   // du survey HEALPix
   protected String getFrame() {
      if( context.hasFrame() ) return context.getFrameCode();
      try {
         if( context.prop==null ) context.loadProperties();
         String s = context.prop.getProperty(Constante.KEY_HIPS_FRAME);
         if( s==null ) s = context.prop.getProperty(Constante.OLD_HIPS_FRAME);
         if( s==null ) s="G";
         if( s.equals("equatorial") ) return "C";
         if( s.equals("ecliptic")) return "E";
         if( s.equals("galactic")) return "G";
         return s;
      } catch( Exception e ) { e.printStackTrace(); }
      return context.getFrameCode();
   }

   protected String ip(double raw,double bzero,double bscale) {
      return cds.tools.Util.myRound(raw) + (bzero!=0 || bscale!=1 ? "/"+cds.tools.Util.myRound(raw*bscale+bzero) : "");
   }

   /**
    * Initialisation des param�tres FITS � partir d'un Allsky.fits pr�c�dent
    */
   protected void setFitsParamFromPreviousAllsky(String allskyFile) throws Exception {
      Fits f = new Fits();

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
