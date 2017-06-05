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

/*
 * Created on 04-Jan-2006
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

/**
 * @author Thomas Boch [CDS]
 */
public class XMatchEllipseElem extends XMatchElem {
    double maj, min, pa; // parametres de l'ellipse d'erreur : maj. axis, min. axis, position angle
    
    XMatchEllipseElem(double ra, double dec, int idx, double maj, double min, double pa) {
        super(ra,dec,idx);
        
        this.maj = maj;
        this.min = min;
        this.pa = pa;
    }
    
    XMatchEllipseElem() {}
    
    public String toString() {
        return ra+"\t"+dec+"\t"+idx+"\t\t"+maj+"\t"+min+"\t"+pa;
    }
}

