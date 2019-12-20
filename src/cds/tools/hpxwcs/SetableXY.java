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

package cds.tools.hpxwcs;



/**
 * Interface defining a setable position in a 2-dimensional space.
 * 
 * @author F.-X. Pineau
 *
 */
public interface SetableXY extends XY {
    /**
     * Set the coordinate along the x-axis.
     * @param x value of the coordinate along the x-axis.
     */
    void setX(double x);
    /**
     * Set the coordinate along the y-axis.
     * @param y value of the coordinate along the y-axis.
     */
    void setY(double y);
    /**
     * Set the coordinates on both axis at once.
     * @param x value of the coordinate along the x-axis.
     * @param y value of the coordinate along the y-axis.
     */
    void setXY(double x, double y);

}
