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

package cds.aladin;



/**
 * Classe dediee � l'affichage d'un enregistrement de propri�t�s
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : sept 2017 - Creation
 */
public final class FrameRecord extends FrameHipsProperties {
    
    /**
     * Visualisation de propri�t�s cl� = valeur pass�es en param�tre
     * @param prop
     * @throws Exception
     */
    protected FrameRecord(String title, String prop) throws Exception {
       super(title,prop);
    }
    
}
