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

