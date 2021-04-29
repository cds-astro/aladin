// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.moc;

/** MOC cell object
 * 
 * @author Pierre Fernique [CDS]
 * @version 2.0 apr 2021 - refactoring (Moc 2.0)
 * @version 1.0 dec 2011 - creation
 */
public class MocCell {
   public char dim;             // Char signature of the dimension ('s' for SPACE, 't' for TIME...)
   public int order;            // Order of the Moc cell;
   public long start;           // Cell value, or start index for a Range
   public long end;             // Cell value+1, or end index (excluded) for a Range
   public Moc1D moc;            // Moc1D associated to the cell (ex: for STMoc)
}
