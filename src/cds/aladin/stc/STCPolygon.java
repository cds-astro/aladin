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

package cds.aladin.stc;

import java.util.ArrayList;
import java.util.Collections;

public class STCPolygon extends STCObj {
    private STCFrame frame;
    private ArrayList<Double> xCorners = new ArrayList<Double>();
    private ArrayList<Double> yCorners = new ArrayList<Double>();


    public STCPolygon() {}

    public STCFrame getFrame() {
        return frame;
    }

    public void setFrame(STCFrame frame) {
        this.frame = frame;
    }

    public void addCorner(double x, double y) {
        xCorners.add(x);
        yCorners.add(y);
    }

    @Override
    public ShapeType getShapeType() {
        return STCObj.ShapeType.POLYGON;
    }

    public ArrayList<Double> getxCorners() {
        return xCorners;
    }

    public void setxCorners(ArrayList<Double> xCorners) {
        this.xCorners = xCorners;
    }

    public ArrayList<Double> getyCorners() {
        return yCorners;
    }

    public void setyCorners(ArrayList<Double> yCorners) {
        this.yCorners = yCorners;
    }
    
    public void reverseDrawDirection() {
		Collections.reverse(xCorners);
		Collections.reverse(yCorners);
	}
    
    @Override
    public boolean isIn(double lon,double lat) {
       return true;
    }


}
