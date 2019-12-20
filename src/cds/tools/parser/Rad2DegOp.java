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

package cds.tools.parser;

/** Opérateur 'rad2deg' (conversion de radians vers degrés)
 * @author Thomas Boch [CDS]
 * @kickoff October 2006
 */
public final class Rad2DegOp extends AbstractOperateur {

	public Rad2DegOp(AbstractOperateur op) {
		ops = new AbstractOperateur[1];
		ops[0] = op;
	}
	
	public final double compute() {
		return 180.0*ops[0].compute()/Math.PI;
	}
}
