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

/** Opérateur 'puissance'
 * @author Thomas Boch [CDS]
 * @kickoff October 2006
 */
public final class PowerOp extends BinaryOperateur {

	public PowerOp(AbstractOperateur op1, AbstractOperateur op2) {
		super(op1, op2);
	}
	
	public final double compute() {
		return Math.pow(ops[0].compute(), ops[1].compute());
	}
}
