// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

import java.text.ParseException;

import cds.astro.Unit;

/**
 * Gabarit � utiliser pour cr�er des fonctions (unaires)
 */
public abstract class Function extends Operator {

	/** le constructeur devra �tre appel� par tous les constructeurs des classes d�riv�es */
	Function() {
		type = Operator.FUNC;
	}
	
	// les fonctions ont une pr�c�dence sup�rieure � toutes les op�rations de base
	int precedence() {
		return 100;
	}
	
	Unit evalUnit(Unit u) {
		Unit retUnit;
		try {
			retUnit = new Unit( eval(u.value) + u.symbol );
		}
		catch( ParseException e ) {retUnit = new Unit();}
		return retUnit;
	}
	
	/** �valuation de la fonction au point x
	 * 
	 * @param x 
	 * @return double valeur de la fonction en x
	 */
	abstract double eval(double x);
	
}
