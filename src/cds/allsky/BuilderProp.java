// Copyright 1999-2018 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

import java.io.OutputStreamWriter;

/** Permet de visualiser les properties maj d'un survey pr�alablement g�n�r�
 * @author Pierre Fernique [CDS]
 */
public class BuilderProp extends Builder {

   public BuilderProp(Context context) { super(context); }

   public Action getAction() { return Action.PROP; }

   public void run() throws Exception {
      context.scriptCommand=null;
      System.out.println();
      try {
         context.loadMoc();
         context.moc = context.mocIndex;
      } catch( Exception e ) {
         e.printStackTrace();
      }
      context.loadProperties();
      context.writePropertiesFile( new OutputStreamWriter( System.out ) );
      System.out.println();
   }

   public void validateContext() throws Exception {
      validateOutput();
      context.loadProperties();
      context.setHipsId(context.hipsId!=null && context.hipsId.startsWith("ivo://UNK.AUTH?") ? null : context.hipsId);
   }
}
