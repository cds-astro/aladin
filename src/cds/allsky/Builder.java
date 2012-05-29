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
import cds.aladin.PlanHealpix;
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
         case MOC:       return new BuilderMoc(context);
         case MOCINDEX:  return new BuilderMocIndex(context);
         case CLEAN:     return new BuilderClean(context);
         case CLEANINDEX:return new BuilderCleanIndex(context);
         case CLEANTILES:return new BuilderCleanTiles(context);
         case CLEANFITS: return new BuilderCleanFits(context);
         case CLEANJPEG: return new BuilderCleanJpg(context);
         case GZIP:      return new BuilderGzip(context);
         case GUNZIP:    return new BuilderGunzip(context);
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
         output = context.getInputPath() + Constante.ALLSKY;
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
   
   // Valide les cuts passés en paramètre, ou à défaut cherche à en obtenir depuis une image étalon
   protected void validateCut() throws Exception {
      if( context.isValidateCut() ) return;
      double [] cutOrig;
      double [] memoCutOrig = context.getCutOrig();

      // Pas de pixelCut positionnés, ou pas de dataCut positionnés
      if( memoCutOrig==null || memoCutOrig[2]==0 && memoCutOrig[3]==0 ) {
         String img = context.getImgEtalon();
         if( img==null && context.getInputPath()!=null) {
            img = context.justFindImgEtalon( context.getInputPath() );
            context.info("Use this reference image => "+img);
         }
         if( img!=null ) {
            try { context.setImgEtalon(img); }
            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
         }
         
         // Replacement des pixelCut paramétrés par l'utilisateur
         if( memoCutOrig!=null && memoCutOrig[2]==0 && memoCutOrig[3]==0 ) {
            cutOrig = context.getCutOrig();
            cutOrig[0]=memoCutOrig[0];
            cutOrig[1]=memoCutOrig[1];
            context.setCutOrig(cutOrig);
         }
      }
      
      cutOrig = context.getCutOrig();
      if( cutOrig==null ) throw new Exception("Argument \"pixelCut\" required");
      if( cutOrig[2]==0 && cutOrig[3]==0 ) throw new Exception("Argument \"pixelRange\" required");
      if( !( cutOrig[2] <= cutOrig[0] && cutOrig[0] < cutOrig[1] && cutOrig[1]<=cutOrig[3]) ) 
         throw new Exception("Uncompatible pixel cut ["+cutOrig[0]+" .. "+cutOrig[1]+"] or pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"]");
      context.info("Pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"], pixel cut ["+cutOrig[0]+" .. "+cutOrig[1]+"]");
      context.setValidateCut(true);
   }
}
