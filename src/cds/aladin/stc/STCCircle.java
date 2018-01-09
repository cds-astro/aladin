// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin.stc;

import java.util.List;

import cds.aladin.Coord;

/**
 * Class representing a circle formed from an stc string
 * @author chaitra
 *
 */
public class STCCircle extends STCObj {
	
	private Coord center;
	private double radius;
	
	public STCCircle() {
		// TODO Auto-generated constructor stub
	}
	
	public STCCircle(STCFrame frame, double ra, double dec, double radius) {
		this();
		this.frame =  frame;
		this.center = new Coord(ra,dec);
		this.radius = radius;
	}
	
	public STCCircle(double ra, double dec, double radius) {
		this();
		this.center = new Coord(ra,dec);
		this.radius = radius;
	}
	
	public STCCircle(STCFrame frame, String ra, String dec, String radius) {
		this(Double.parseDouble(ra),Double.parseDouble(dec),Double.parseDouble(radius));
		this.frame =  frame;
	}
	
	/*public static void main(String[] args) {
		String stc = "circle icrs 84.23 -10.95 0.0005 circle icrs 90.32 -10.95 0.0005";
		STCStringParser parser = new STCStringParser();
		List<STCObj> stcobj = parser.parse(stc);
		for (STCObj stcObj2 : stcobj) {
			System.out.println(stcObj2.toString());
		}
	}*/

	/* (non-Javadoc)
	 * @see cds.aladin.stc.STCObj#getShapeType()
	 */
	@Override
	public ShapeType getShapeType() {
		// TODO Auto-generated method stub
		return STCObj.ShapeType.CIRCLE;
	}

	/* (non-Javadoc)
	 * @see cds.aladin.stc.STCObj#isIn(double, double)
	 */
	@Override
	public boolean isIn(double lon, double lat) {
		boolean isIn = false;
		double distance = Coord.getDist(this.center,
				new Coord(lat, lon));
		if (distance <= this.radius) {
			isIn = true;
		}
		return isIn;
	}

	public void setFrame(STCFrame frame) {
		this.frame = frame;
	}

	public Coord getCenter() {
		return center;
	}

	public void setCenter(Coord center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer toPrint = new StringBuffer();
		toPrint.append(this.getShapeType()).append(" ").append(this.frame).append(" ").append(this.center.al)
				.append(" ").append(this.center.del).append(" ").append(this.radius);
		return toPrint.toString();
	}

}
