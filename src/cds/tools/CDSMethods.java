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

package cds.tools;

/**
 * CDS Methods
 * Contains general methods
 * Copyright: CDS (c) 2002
 *
 * @author Andre SCHAAFF [CDS]
 * @version 1.0 : (june 2002) creation
 */

import java.net.URLEncoder;

public class CDSMethods {

  /**  Appends all parameters for an URL.
   * Formating NAME=VALUE et encoding %XX if necessary and adding
   * of & prefix if necessary.
   *
   * @param s      StringBuffer which is updated
   * @param nom    parameter name
   * @param valeur parameter value
   */
   public static void append(StringBuffer s,String nom,String valeur) {
    if( s.length() > 0 )
      s.append("&");
    s.append(nom);
    s.append("=");
    s.append(URLEncoder.encode(valeur));
  }
}
