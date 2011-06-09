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

import cds.astro.Unit;

/**
 * Fonction logarithme base 10
 */
public class Sqrt extends Function {

    /* 
     * @see cds.tools.parser.Function#eval(double)
     */
    double eval(double x) {
        return Math.sqrt(x);
    }

    Unit evalUnit(Unit u) {
        Unit retUnit;
        try {
            u.sqrt();
            retUnit = new Unit(u);
        }
        catch( ArithmeticException e ) {retUnit = new Unit();}
        return retUnit;
    }

    /* 
     * @see cds.tools.parser.Operator#keyword()
     */
    String keyword() {
        return "sqrt";
    }

}

