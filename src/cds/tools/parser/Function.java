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

import java.text.ParseException;

import cds.astro.Unit;

/**
 * Gabarit à utiliser pour créer des fonctions (unaires)
 */
public abstract class Function extends Operator {

	/** le constructeur devra être appelé par tous les constructeurs des classes dérivées */
	Function() {
		type = Operator.FUNC;
	}
	
	// les fonctions ont une précédence supérieure à toutes les opérations de base
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
	
	/** évaluation de la fonction au point x
	 * 
	 * @param x 
	 * @return double valeur de la fonction en x
	 */
	abstract double eval(double x);
	
}
