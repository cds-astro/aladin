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

package cds.aladin;

/**
 * Classe représentant un footprint associé à un objet Source
 * Cette classe a été créée afin de faire du nettoyage au niveau de la classe Source 
 * 
 * @author Thomas Boch
 * 
 * @version 0.1 18/04/2006
 * 
 */
public class SourceFootprint {

	private PlanField footprint; // objet PlanField associé à la source
//	private double footprintAngle = 0.; // angle de position pour le footprint
	private boolean showFootprint = false; // doit-on montrer le footprint associé 
	private int idxFootprint = -1; // index du footprint
	
	/** Constructeur */
	public SourceFootprint() {}
	
	/**
	 * @return Returns the footprint.
	 */
	protected PlanField getFootprint() {
		return footprint;
	}
	/**
	 * @param footprint The footprint to set.
	 */
	protected void setFootprint(PlanField footprint) {
		this.footprint = footprint;
	}
	
	/**
	 * @return Returns the footprintAngle.
	protected double getFootprintAngle() {
		return footprintAngle;
	}
	*/
	/**
	 * @param footprintAngle The footprintAngle to set.
	protected void setFootprintAngle(double footprintAngle) {
		this.footprintAngle = footprintAngle;
	}
	*/
	
	/**
	 * @return Returns the idxFootprint.
	 */
	protected int getIdxFootprint() {
		return idxFootprint;
	}
	/**
	 * @param idxFootprint The idxFootprint to set.
	 */
	protected void setIdxFootprint(int idxFootprint) {
		this.idxFootprint = idxFootprint;
	}
	
	/**
	 * @return Returns the showFootprint.
	 */
	protected boolean showFootprint() {
		return showFootprint;
	}
	
	/**
	 * @param showFootprint The showFootprint to set.
	 */
	protected void setShowFootprint(boolean showFootprint) {
		this.showFootprint = showFootprint;
	}
}
