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

package cds.aladin;

public interface GrabItFrame {

	/**
	 * Mise en place du target en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	void setGrabItCoord(Coord c); //double x, double y);

	/**
	 * Arrete le GrabIt
	 */
	void stopGrabIt();

	/**
	 * Retourne true si le bouton grabit du formulaire existe et qu'il est
	 * enfoncé
	 */
	boolean isGrabIt();

	/**
	 * Mise en place du radius en calculant la position courante dans la Vue en
	 * fonction du x,y de la souris
	 * 
	 * @param x,y
	 *            Position dans la vue
	 */
	void setGrabItRadius(double x1, double y1, double x2, double y2);
	
	public void toFront();

}
