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

/*
 * Created on 19-Oct-2006
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.tools.parser;

/**
 * @author boch
 */
public abstract class BinaryOperateur extends AbstractOperateur {

	public BinaryOperateur(AbstractOperateur op1, AbstractOperateur op2) {
		ops = new AbstractOperateur[2];
		// pour la négation unaire
		ops[0] = op1==null?new ConstantValOp(0.0):op1;
		ops[1] = op2;
	}
	/* (non-Javadoc)
	 * @see cds.tools.parser.AbstractOperateur#compute()
	 */
	public abstract double compute();

}
