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

package cds.aladin;


/**
 * Default modes
 * @author Pierre Fernique
 * @version 1.0 - January 2018 - creation
 *
 */
public class Default  {
   
   // Choose here the default log behaviour.
   //
   // If you turn it false, at the first launch, Aladin will ask to the user if he would agree
   // to switch on the anonymous log mechanism.
   public static boolean LOG = true;
   
   // Choose here the default version test behaviour
   //
   // If yes, Aladin will check if a new official version of Aladin is available on the CDS site.
   // If it is the case, a popup window will suggest to install the new version
   // by opening the CDS downloading page in the browser
   public static boolean VERSIONTEST = true;
}
