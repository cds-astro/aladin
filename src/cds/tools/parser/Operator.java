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

/** Classe Operator, classe abstraite commune aux les op�rations de base et aux fonctions
 */
public abstract class Operator {
	
	static final int BASIC = 0;
	static final int FUNC = 1;
	
	// s'agit-il d'une op�rateur de base (+,-,*,/) ou d'une fonction
	int type;
		
	/**
	 * @return int entier repr�sentant la pr�c�dence de l'op�rateur ( nombre �lev� --> pr�cedence importante)
	 */
	abstract int precedence();
	
	/**
	 * @return String cha�ne permettant de reconna�tre la fonction
	 */
	abstract String keyword();
}
