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

/** Permet de mettre à jour un survey préalablement généré
 * @author Pierre Fernique [CDS]
 */
public class BuilderUpdate extends Builder {

   private Builder b=null;                   // Subtilité pour faire afficher des statistiques

   public BuilderUpdate(Context context) { super(context); }

   public Action getAction() { return Action.UPDATE; }

   public void run() throws Exception {
      if( !context.isTaskAborting() ) { (b=new BuilderMoc(context)).run(); b=null; }
      if( !context.isTaskAborting() ) { (b=new BuilderGzip(context)).run(); b=null; }
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.info("ALLSKY file done"); }
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
