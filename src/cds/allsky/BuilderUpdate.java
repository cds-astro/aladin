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
import java.io.FileInputStream;

import cds.aladin.MyInputStream;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** Permet de mettre à jour un survey préalablement généré
 * @author Pierre Fernique [CDS]
 */
public class BuilderUpdate extends Builder {

   private Builder b=null;                   // Subtilité pour faire afficher des statistiques

   public BuilderUpdate(Context context) { super(context); }

   public Action getAction() { return Action.UPDATE; }

   public void run() throws Exception {
      fillupContext();
      if( !context.isTaskAborting() ) builderMoc(); 
//      if( !context.isTaskAborting() ) builderGunzip();
      if( !context.isTaskAborting() ) builderDataSum();
      if( !context.isTaskAborting() ) builderLowOrder();
      if( !context.isTaskAborting() ) builderAllsky();
      if( !context.isTaskAborting() ) builderCheck();
   }
   
   private void fillupContext() throws Exception {
      context.loadProperties();
      
      String s = context.prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      boolean color = s!=null && s.indexOf("color")>=0;
      if( color ) context.setColor("true");
      
      boolean live = s!=null && s.indexOf("live")>=0;
      if( live ) context.setLive( live );
      
      int [] minmax = context.findMinMaxOrder();
      if( minmax[0]==-1 ) throw new Exception("No HiPS found in target dir ["+context.getOutputPath()+"] !");
      context.setOrder(minmax[1]);
      
      int depth=1;
      s = context.prop.getProperty(Constante.KEY_CUBE_DEPTH);
      try { depth=Integer.parseInt(s); } catch( Exception e) {} 
      context.setDepth(depth);
      
      s = context.prop.getProperty(Constante.KEY_HIPS_FRAME);
      try { 
         int frame = context.getFrameVal(s);
         context.setFrame(frame );
      } catch( Exception e) {} 

      s = context.prop.getProperty(Constante.KEY_HIPS_STATUS);
      if( s!=null ) context.setStatus(s);
      
      int tileSideByNpixFile = context.getTileWidthByNpixFile( context.getOutputPath() );
      context.setTileOrder( (int)CDSHealpix.log2(tileSideByNpixFile ));
      


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
   
   private void builderDataSum() throws Exception {
      Fits f = new Fits();
      f.loadFITS( context.findOneNpixFile( context.getOutputPath()) );
      if( f.headerFits.hasKey("DATASUM" ) ) {
         context.info("DATASUM seems to be already present in tiles (to update them use \"UPDATEDATASUM\" action)");
         return;
      }
      context.info("Scanning and adding DATASUM on FITS tiles...");
      (b=new BuilderUpdateDataSum(context)) .run();
      context.done("Datasum done");
      b=null; 
   }

   private void builderCheck() throws Exception {
      if( context.getCheckCodeFromProp()!=null ) {
         context.info("Checkcodes already present (to update them use \"CHECKCODE\" action)");
         return;
      }
      context.info("Adding check codes...");
//      context.setCheckForce(true);
      Task.factoryRunner(context, Action.CHECKCODE);
      context.writePropertiesFile();
      context.done("Check code done");
      b=null; 
   }


   private void builderGunzip() throws Exception {
      MyInputStream in = null;
      boolean isGZ = false;
      try { 
         in = new MyInputStream( new FileInputStream( context.findOneNpixFile( context.getOutputPath())) ); 
         isGZ = in.isGZ();
      } finally { if( in!=null ) in.close(); }
      if( !isGZ ) {
         context.info("Tiles seem to be already gunzipped (use \"GUNZIP\" to be sure)");
         return;
      }
      context.info("Scanning and gunzipping required tiles (order<=5) if required...");
      (b=new BuilderGunzip(context)) .run();
      if( ((BuilderGunzip)b).nbFile==0 ) context.info("Nothing gzipped");
      else context.done("Gunzip done");
      b=null; 
   }

   private void builderMoc() throws Exception {
      try {
         context.loadMoc();
         if( context.moc.getMocOrder()==29 ) {
            context.info("Suspicious MOC order (29) !");
            keepOldCopy("Moc.fits");
         }
      } catch( Exception e ) {
         context.info("Regenerating MOC from max order tiles...");
         (b=new BuilderMoc(context)).run();
         context.done("MOC regenerated from max order tiles");
         context.loadMoc();
         b=null;
      }
   }
   
   private void builderAllsky() throws Exception {
      
//      // Y aurait-il des allsky manquants ? si oui, on refait tout
//      for( int z=0; z<context.depth; z++ ) {
//         String filename = BuilderAllsky.getFileName(context.getOutputPath(), 3,z);
//         
//         String s = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
//         Tok tok = new Tok(s);
//         while( tok.hasMoreTokens() ) {
//            String fmt = tok.nextToken();
//            if( fmt.equals("jpeg") ) fmt="jpg";
//            
//            File f = new File(filename+"."+fmt);
//            // Manquant => on refait le tout
//            if( !f.exists() ) {
//               context.done("Regenerating Allsky file(s) from Norder3 tiles");
//               b=new BuilderAllsky(context);
//               b.run();
//               context.done("Allsky file(s) regenerated");
//               return;
//            }
//         }
//
//      }
      
      // Tous les allsky sont bons => Juste les actions en postJob à refaire
      b=new BuilderAllsky(context);
      ((BuilderAllsky)b).postJob();
   }

   private void builderLowOrder() throws Exception {
      int [] minmax = context.findMinMaxOrder();
      if( minmax[0]==0 ) return;   // inutile car déjà fait 
      if( context.getMinOrder()==minmax[0] ) return;  // ca commence explicitement au-dela de Norder0
      
      boolean flagFits = false;
      int bitpixOrig = context.getBitpixOrig();
      context.setModeMerge( ModeMerge.mergeKeepTile );
      int order=context.getOrder();
      context.setOrder(3);
      
      context.info("Building missing HiPS low orders (Norder0,1 and 2)...");
      
      String s = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
      Tok tok = new Tok(s);
      while( tok.hasMoreTokens() ) {
         String fmt = tok.nextToken();
         if( fmt.equals("fits") ) { flagFits=true; continue; } 
         context.info("- building Norder0,1 and 2 for "+fmt+" tiles...");
         context.setColor(fmt);
         (b=new BuilderTree(context)).build();
      }
      
      context.bitpixOrig = bitpixOrig;
      context.order = order;
      if( flagFits && bitpixOrig!=0 ) {
         context.info("- building Norder0, 1 and 2 for fits tiles...");
         (b=new BuilderTree(context)).build();
      }
      
      context.setMinOrder(0);
      context.done("Norder0,1 and 2 successfully built");
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
