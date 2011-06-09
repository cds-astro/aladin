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

/** Opérateur 'arctan'
 * @author Thomas Boch [CDS]
 * @kickoff April 2010
 */
public final class AtanOp extends AbstractOperateur {

    public AtanOp(AbstractOperateur op) {
        ops = new AbstractOperateur[1];
        ops[0] = op;
    }

    @Override
    public final double compute() {
        return Math.atan(ops[0].compute());
    }
}
