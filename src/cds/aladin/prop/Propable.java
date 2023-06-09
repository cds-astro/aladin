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

package cds.aladin.prop;

import java.util.Vector;

/** Interface pour un objet qui peut fournir une liste de propri�t�s (Prop)
 * @date d�c 2011 - cr�ation
 * @author Pierre Fernique [CDS]
 */
public interface Propable {
   
   /** @return true si l'objet a des propri�t�s qui lui sont associ�es */
   public boolean hasProp();
   
   /** @return fournit la liste des propri�t�s */
   public Vector<Prop> getProp();
}


