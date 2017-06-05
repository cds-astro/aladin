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

/**
 * Fonction logarithme népérien
 * this class used to be called "Ln" but some Windows JVM did not like that (!)
 */
public class NepLog extends Function {

    /* 
     * @see cds.tools.parser.Operator#keyword()
     */
    public String keyword() {
        return "ln";
    }

    /* 
     * @see cds.tools.parser.Function#eval(double)
     */
    public double eval(double x) {
        return Math.log(x);
    }

}
