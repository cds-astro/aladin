// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.aladin;

import java.io.File;

import cds.tools.Util;

class AladinStatic extends Aladin { 
   
   /** Fournit une instance d'Aladin permettant de générer des images en mode serveur
    * => voir ImageMaker
    * @param trace niveau de verbosité (0-rien, 3-pas mal, 6-debug++
    * @throws Exception
    */
   AladinStatic(int trace) throws Exception {
      super();
      creatFonts();
      levelTrace=trace;
      aladin=this;
      configuration = new Configuration(aladin);
      configuration.load();
      initColors();
      localisation = new LocalisationStatic(aladin);
      view = new ViewStatic(aladin);
      calque = new CalqueStatic(aladin,(ViewStatic)aladin.view);
   }
   
   protected boolean createCache() { 
      if( CACHEDIR!=null ) return CACHEDIR.length()!=0;

      try {
         // Existe-il déjà un répertoire générique .aladin sinon je le crée ?
         CACHEDIR = System.getProperty("user.home")+Util.FS+CACHE;
         File f = new File(CACHEDIR);
         if( !f.isDirectory() ) if( !f.mkdir() ) { CACHEDIR=""; return false; }

         Aladin.trace(3,"Create cache directory: "+CACHEDIR);
         
      } catch( Exception e ) { e.printStackTrace(); return false; }
      return true;
   }
}
