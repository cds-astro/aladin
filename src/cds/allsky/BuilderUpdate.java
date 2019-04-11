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

import cds.aladin.Tok;

/** Permet de mettre à jour un survey préalablement généré
 * @author Pierre Fernique [CDS]
 */
public class BuilderUpdate extends Builder {

   private Builder b=null;                   // Subtilité pour faire afficher des statistiques

   public BuilderUpdate(Context context) { super(context); }

   public Action getAction() { return Action.UPDATE; }

   public void run() throws Exception {
      context.loadProperties();
      if( !context.isTaskAborting() ) builderMoc(); 
      if( !context.isTaskAborting() ) { (b=new BuilderGunzip(context)).run(); b=null; }
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.done("ALLSKY file done"); }
//      if( !context.isTaskAborting() ) builderHighOrder();
   }
   
   private void builderMoc() throws Exception {
      try { context.loadMoc(); }
      catch( Exception e ) {
         (b=new BuilderMoc(context)).run();
         context.info("MOC rebuilt from low rhombs");
         context.loadMoc();
         b=null;
      }
   }
   
   private void builderHighOrder() throws Exception {
      int [] minmax = context.findMinMaxOrder();
      if( minmax[0]==0 ) return;   // inutile car déjà fait 
      if( context.getMinOrder()==minmax[0] ) return;  // ca commence explicitement au-dela de Norder0
      
      boolean flagFits = false;
      int bitpixOrig = context.getBitpixOrig();
      
      String s = context.prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
      Tok tok = new Tok(s);
      while( tok.hasMoreTokens() ) {
         String fmt = tok.nextToken();
         if( fmt.equals("fits") ) { flagFits=true; continue; } 
         context.info("Building Norder0,1 and 2 for "+fmt+" tiles...");
         context.setColor(fmt);
         (b=new BuilderTree(context)).run();
      }
      
      if( flagFits ) {
         context.info("Building Norder0,1 and 2 for fits tiles...");
         context.bitpixOrig = bitpixOrig;
         (b=new BuilderTree(context)).run();
        
      }
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
