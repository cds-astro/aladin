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

package cds.tools.parser;

/** Classe abstraite à la base de tout le calcul
 * Chaque opérateur va posséder une méthode 'compute'
 * qui appellera si nécessaire les méthodes compute des opérateurs membres (pattern 'composite')
 * 
 * @see cds.tools.parser.AdditionOp pour un exemple
 * @author Thomas Boch [CDS]
 * @kickoff October 2006
 */
public abstract class AbstractOperateur {

	AbstractOperateur[] ops;
	
	public abstract double compute();
}
