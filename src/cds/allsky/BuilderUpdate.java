// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.allsky;

import java.io.File;

import cds.aladin.Tok;
import cds.tools.Util;

/** Permet de mettre à jour un survey préalablement généré
 * @author Pierre Fernique [CDS]
 */
public class BuilderUpdate extends Builder {

   private Builder b=null;                   // Subtilité pour faire afficher des statistiques

   public BuilderUpdate(Context context) { super(context); }

   public Action getAction() { return Action.UPDATE; }

   public void run() throws Exception {
      keepOldCopy();
      fillupContext();
      if( !context.isTaskAborting() ) builderMoc(); 
      if( !context.isTaskAborting() ) builderGunzip();
      if( !context.isTaskAborting() ) builderAllsky();
      if( !context.isTaskAborting() ) builderHighOrder();
   }
   
   private void fillupContext() throws Exception {
      context.loadProperties();
      String s = context.prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      boolean color = s!=null && s.indexOf("color")>=0;
      if( color ) context.setColor("true");
      
      int [] minmax = context.findMinMaxOrder();
      if( minmax[0]==-1 ) throw new Exception("No HiPS found in target dir ["+context.getOutputPath()+"] !");
      context.setOrder(minmax[1]);
      
      int depth=1;
      s = context.prop.getProperty(Constante.KEY_CUBE_DEPTH);
      try { depth=Integer.parseInt(s); } catch( Exception e) {} 
      context.setDepth(depth);

      context.initRegion();
   }
   
   private String [] OLD = { "properties","index.html" };
   
   // Copie de sauvegarde des précédents fichiers
   private void keepOldCopy() throws Exception {
      String path = context.getOutputPath();
      for( String f: OLD )  keepOldCopy(path+Util.FS+f);
   }
   
   
   // Copie de sauvegarde du fichier indiqué
   private void keepOldCopy(String f) throws Exception {
      if( !( new File(f)).exists() ) return;
      
      // Destruction du 10e si existe
      File max = new File( f+".9" );
      if( max.exists() ) max.delete();
      
      // Décalage des anciennes versions
      for( int i=9; i>1; i-- ) {
         File fo = new File( f+(i==1?"":"."+(i-1)) );
         if( fo.exists() ) fo.renameTo( new File( f+"."+i ) );
      }
      
      // Copie de la version courante
      BuilderMirror.copyLocal(f, f+".1");
   }
   
   
   private void builderGunzip() throws Exception {
      context.info("Scanning and gunzipping required tiles (order <=5)...");
      (b=new BuilderGunzip(context)) .run();
      context.info("Gunzipping done");
      b=null; 
   }

   private void builderMoc() throws Exception {
      try { context.loadMoc(); }
      catch( Exception e ) {
         context.info("Regenerating MOC from low rhombs...");
         (b=new BuilderMoc(context)).run();
         context.done("MOC regenerated from low rhombs");
         context.loadMoc();
         b=null;
      }
   }
   
   private void builderAllsky() throws Exception {
      
      // Y aurait-il des allsky manquants ? si oui, on refait tout
      for( int z=0; z<context.depth; z++ ) {
         String filename = BuilderAllsky.getFileName(context.getOutputPath(), 3,z);
         
         String s = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
         Tok tok = new Tok(s);
         while( tok.hasMoreTokens() ) {
            String fmt = tok.nextToken();
            
            // Manquant => on refait le tout
            if( !(new File(filename+"."+fmt)).exists() ) {
               context.done("Regenerating Allsky file(s) from Norder3 tiles");
               b=new BuilderAllsky(context);
               b.run();
               context.done("Allsky file(s) regenerated");
               return;
            }
         }
        
      }
      
      // Tous les allsky sont bons => Juste les actions en postJob à refaire
      b=new BuilderAllsky(context);
      ((BuilderAllsky)b).postJob();
   }

   private void builderHighOrder() throws Exception {
      int [] minmax = context.findMinMaxOrder();
      if( minmax[0]==0 ) return;   // inutile car déjà fait 
      if( context.getMinOrder()==minmax[0] ) return;  // ca commence explicitement au-dela de Norder0
      
      boolean flagFits = false;
      int bitpixOrig = context.getBitpixOrig();
      context.setMode( Mode.KEEPTILE );
      int order=context.getOrder();
      context.setOrder(3);
      
      context.info("Building Norder0,1 and 2...");
      
      String s = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
      Tok tok = new Tok(s);
      while( tok.hasMoreTokens() ) {
         String fmt = tok.nextToken();
         if( fmt.equals("fits") ) { flagFits=true; continue; } 
         context.info("Building Norder0,1 and 2 for "+fmt+" tiles...");
         context.setColor(fmt);
         (b=new BuilderTree(context)).build();
      }
      
      context.bitpixOrig = bitpixOrig;
      context.order = order;
      if( flagFits && bitpixOrig!=0 ) {
         context.info("Building Norder0,1 and 2 for fits tiles...");
         (b=new BuilderTree(context)).build();
      }
      
      context.done("Norder0,1 and 2 built");

   }

   public void validateContext() throws Exception {
      validateOutput();
      if( context.hipsId!=null ) context.setHipsId(context.hipsId);
      else {
         context.loadProperties();
         context.setHipsId(null);
      }
   }
}
