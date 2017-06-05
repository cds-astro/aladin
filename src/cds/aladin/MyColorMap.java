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

package cds.aladin;


/** Classe permettant la création de color maps personnalisées
 * 
 * Une color map est représentée basiquement par les tableaux de répartition 3 composantes RGB
 * L'ajout d'une d ces color maps personnalisée se fait via la classe ColorMap
 * Classe initialement créée pour pouvoir charger une palette de couleurs provenant d'IDL  
 * 
 * @author Thomas Boch [CDS]
 * @version 0.1 03 avril 2006
 * @see FrameColorMap#addCustomCM(MyColorMap)
 */
public class MyColorMap {
	// les tableaux de répartition des composante
	private int[] red;
	private int[] green;
	private int[] blue;
	
	// une chaine identifiant la colormap
	private String name;
	
	/**
	 * Constructeur
	 * Remarque : pour le moment, on ne fait aucune vérification sur la taille 
	 * de chaque tableau (devrait être 256)
	 * @param name nom de la colormap
	 * @param red
	 * @param green
	 * @param blue
	 */
	public MyColorMap(String name, int[] red, int[] green, int[] blue) {
		setName(name);
		setRed(red);
		setGreen(green);
		setBlue(blue);
	}
	
	
	/**
	 * @return Returns the blue.
	 */
	public int[] getBlue() {
		return blue;
	}
	/**
	 * @param blue The blue to set.
	 */
	public void setBlue(int[] blue) {
		this.blue = blue;
	}
	/**
	 * @return Returns the green.
	 */
	public int[] getGreen() {
		return green;
	}
	/**
	 * @param green The green to set.
	 */
	public void setGreen(int[] green) {
		this.green = green;
	}
	/**
	 * @return Returns the red.
	 */
	public int[] getRed() {
		return red;
	}
	/**
	 * @param red The red to set.
	 */
	public void setRed(int[] red) {
		this.red = red;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
