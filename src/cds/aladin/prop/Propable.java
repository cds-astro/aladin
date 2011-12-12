// Copyright 2010 - UDS/CNRS
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

package cds.aladin.prop;

import java.util.Vector;

/** Interface pour un objet qui peut fournir une liste de propriétés (Prop)
 * @date déc 2011 - création
 * @author Pierre Fernique [CDS]
 */
public interface Propable {
   
   /** @return true si l'objet a des propriétés qui lui sont associées */
   public boolean hasProp();
   
   /** @return fournit la liste des propriétés */
   public Vector<Prop> getProp();
}


