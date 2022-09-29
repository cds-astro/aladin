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

/** Vérification des Check codes associés à un HiPS
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Juillet 2022
 */
public class BuilderCheck extends BuilderCheckCode {

   public BuilderCheck(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CHECK; }
   
   protected void validateContextMore() throws Exception {
      String s = context.getCheckCode();
      if( s==null ) throw new Exception("Check codes unknown => CHECK action not available. Use CHECKCODE action to create it");
   }
   
   public void run() throws Exception {
      boolean rep=true;
      String hipscrc = context.getCheckCode();
      
      Tok tok = new Tok(format," ,");
      while( tok.hasMoreTokens() ) {
         String fmt = tok.nextToken();
         String v = Context.getCheckCode(fmt, hipscrc);
         if( v==null ) {
            context.warning("Unknown check code for "+fmt+" tiles. No verification!");
            continue;
         }
         Info info = scanDir(new File( context.getOutputPath() ),fmt);
         if( !info.getCode().equals(v) ) {
            context.error("Check code error in "+fmt+" tiles !");
            rep=false;
         }
      }
      
      if( !rep ) throw new Exception("HiPS is not compliant to check codes");
      context.info("HiPS compliant with check codes!");
   }
}
