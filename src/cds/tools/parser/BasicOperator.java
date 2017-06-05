// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
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

/** Classe BasicOperator */
public class BasicOperator extends Operator {
	
	
	int p;
	int op;
	
	/** Constructeur 
	 *  @param p - precedence
	 *  @param op - operateur (voir classe Parser)
	 *  @see cds.tools.parser.Parser
	 */
	BasicOperator(int p, int op) {
	    this.p = p;
	    this.op = op;
	    type = Operator.BASIC;
	} 
	
	public String toString() {
		return op + " " +precedence();
	}
	
	public String keyword() {
		return ""+((char) op);
	}
	
	public int precedence() {
		return this.p;
	}
}
