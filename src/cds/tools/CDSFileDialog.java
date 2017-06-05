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

package cds.tools;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFileChooser;

import cds.aladin.Aladin;

/** Gestion de la boite de dialogue de sélection de fichiers
 * Comme le FileDialog AWT est meilleur mais ne permet pas de sélectionner
 * un répertoire sauf sous Windows, on va utiliser le JFileChooser dans les
 * autres cas.
 * 
 * @author Pierre Fernique
 * @version 1.0 Création  - Fevrier 2014
 */
public final class CDSFileDialog  {
   private Aladin aladin;
   FileDialog fd;
   JFileChooser jfc;
   
   static final boolean isWin = Util.indexOfIgnoreCase( System.getProperty("os.name"),"win")>=0;
   
   public CDSFileDialog(Aladin aladin) {
      this.aladin = aladin;
      if( isWin ) fd = new FileDialog(aladin.dialog);
      else jfc = new JFileChooser();
   }

   
   public String getDirectory() {
      if( isWin ) return fd.getDirectory();
      else {
         File f = jfc.getCurrentDirectory();
         return f==null ? null : f.toString();
      }
   }
   
   public void setDirectory(String dir) {
      if( isWin ) fd.setDirectory(dir);
      else jfc.setCurrentDirectory( new File(dir));
   }
   
   private static final String DEFAULT_FILENAME = "-";
   
   public String getFile() { 
      if( isWin ) {
         fd.setFile(DEFAULT_FILENAME);
         fd.setVisible(true);
         String directory = fd.getDirectory();
         String name =  fd.getFile();
         // si on n'a pas changé le nom, on a selectionne un repertoire
         if( name!=null && name.equals(DEFAULT_FILENAME) ) return directory;
         return name;
         
      } else {
         jfc.setVisible(true);
         File f = jfc.getSelectedFile();
         return f==null ? null : f.toString();
      }
  }

}
