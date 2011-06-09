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

/*
 * Created on 19-Oct-2006
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.tools.parser;

import cds.astro.Unit;

/** Opérateur pour une 'variable'
 * @author Thomas Boch [CDS]
 * @kickoff October 2006
 */
public final class VariableOp extends AbstractOperateur {
	
    private double value;
	private Unit unit;

	
	public VariableOp() {
		value = 0.0;
		unit = new Unit();
	}
		
	public final double compute() {
		return value;
	}
	
	public final void setValue(double value) {
		this.value = value;
	}
	
    
    /** Retourne la valeur de la variable */	
    public double getValue() {
    	return value;
    }


	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public Unit getUnit() {
		return unit;
	}
	
}
