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

/*
 * Created on 12-Oct-2005
 *

 */
package cds.aladin;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Vector;

import cds.tools.Util;

/**
 * @author Thomas Boch [CDS]
 *
 * Un bean conservant toutes les infos pour créer les Objet
 * représentant une sous-partie de field of view (PlanField)
 */
public class SubFootprintBean {
	// TODO : refactoring pour faire une classe par type d'objet ??

	protected static final int POLYGON = 1;
	protected static final int CIRCLE = 2;
	protected static final int PICKLE = 3;
	protected static final int STRING = 4;

	// le type de forme décrit par le bean (CIRCLE, POLYGON, ...)
	protected int type = -1;
	// nom associé au bean
	private String name;

	// TODO : comment gérer les sub-sub-footprintbean ? (TABLEs des RESOURCEs)
	private Vector<SubFootprintBean> subSubsFootprints;

	private boolean inSphericalCoords = false;

	// variables de travail

	// pour objets de type POLYGON
	private double[] raOffset, decOffset; // offsets des polygones en deg

	// pour objets de type CIRCLE/PICKLE
	private double centerOffRa, centerOffDec; // offsets du centre en deg dans le plan tangent RA/DEC
	private double circleRadius; // rayon du cercle en deg
	private double internalRad, externalRad; // rayons interne/externe du PICKLE en deg
	private double startAngle, angle; // angle de départ et angle, en deg

	// pour objets STRING
	private double ra, dec;
	private String content;
	private String align; // "center", "left" ou "right"

	private Color c; // couleur du FoV

	protected SubFootprintBean() {
		subSubsFootprints = new Vector<SubFootprintBean>();
	}



	/**
	 * Constructeur pour un bean représentant un polygone
	 * @param raOffset tableau des offsets en RA dans le plan tangent
	 * @param decOffset tableau des offsets en DE dans le plan tangent
	 * @param name nom de la sous-partie
	 */
	protected SubFootprintBean(double[] raOffset, double[] decOffset, String name) {
		this();
		type = POLYGON;
		this.raOffset = raOffset;
		this.decOffset = decOffset;
	}

	/**
	 * Constructeur pour un bean représentant un cercle
	 * @param ctrXOffset offset en X du centre dans plan tangent (en degrés)
	 * @param ctrYOffset offset en Y du centre dans plan tangent (en degrés)
	 * @param radius rayon du cercle en degrés
	 * @param name nom de la sous-partie
	 */
	protected SubFootprintBean(double ctrXOffset, double ctrYOffset, double radius, String name) {
		this();
		type = CIRCLE;
		this.centerOffRa = ctrXOffset;
		this.centerOffDec = ctrYOffset;
		this.circleRadius = radius;
	}

	/**
	 * Constructeur pour un bean représentant un pickle
	 * @param ctrXOffset offset en X du centre dans plan tangent (en degrés)
	 * @param ctrYOffset offset en Y du centre dans plan tangent (en degrés)
	 * @param startAngle angle de départ du pickle
	 * @param angle angle total du secteur
	 * @param intRad rayon interne du pickle
	 * @param extRad rayon externe du pickle
	 * @param name nom de la sous-partie
	 */
	protected SubFootprintBean(double ctrXOffset, double ctrYOffset, double startAngle, double angle, double intRad, double extRad, String name) {
		this();
		type = PICKLE;
		this.centerOffRa = ctrXOffset;
		this.centerOffDec = ctrYOffset;
		this.startAngle = startAngle;
		this.angle = angle;
		this.internalRad = intRad;
		this.externalRad = extRad;
	}

	/**
	 * Constructeur pour un bean représentant une chaine de caractères
	 * @param ra position
	 * @param dec
	 * @param align "center", "left" ou "right"
	 * @param content
	 */
	protected SubFootprintBean(double ra, double dec, String align, String content) {
		this();
		type = STRING;
		this.ra = ra;
		this.dec = dec;
		this.align = align;
		this.content = content;
	}

	protected int getNbOfSubParts() {
		return subSubsFootprints.size();
	}

	public boolean isInSphericalCoords() {
        return inSphericalCoords;
    }



    public void setInSphericalCoords(boolean inSphericalCoords) {
        this.inSphericalCoords = inSphericalCoords;
    }



    protected void addSubFootprintBean(SubFootprintBean sub) {
		subSubsFootprints.addElement(sub);
	}

	/**
	 * Construit la liste des objets correspondant au bean
	 * @return le tableau des Objet
	 */
	protected Obj[] buildObjets(PlanField pf) {
		Vector<Obj> v  = new Vector<Obj>();

		switch(type) {
			case POLYGON: {
				// Creation of polygons
				int nbPts = raOffset.length;

                Ligne curLine = new Ligne(pf);
                v.addElement(curLine);


                double x = isInSphericalCoords()
                           ? Math.toRadians(Math.cos(Math.toRadians(decOffset[decOffset.length-1])) * raOffset[raOffset.length-1])
                           : Util.tand(raOffset[raOffset.length-1]);
                double y = isInSphericalCoords()
                           ? Math.toRadians(decOffset[decOffset.length-1])
                           : Util.tand(decOffset[decOffset.length-1]);

                curLine.setXYTan(x, y);

                // boucle sur les n points du FoV
                for( int j=0; j<nbPts; j++ ) {
                    Ligne newLine = new Ligne(pf);
                    v.addElement(newLine);

                    x = isInSphericalCoords() ? Math.toRadians(Math.cos(Math.toRadians(decOffset[j])) * raOffset[j]) : Util.tand(raOffset[j]);
                    y = isInSphericalCoords() ? Math.toRadians(decOffset[j]) : Util.tand(decOffset[j]);
                    newLine.setXYTan(Util.tand(raOffset[j]), Util.tand(decOffset[j]));
                    newLine.debligne = curLine;
                    curLine = newLine;
                }

				break;
			}

			case CIRCLE: {
			    double xv = isInSphericalCoords() ? Math.toRadians(Math.cos(Math.toRadians(centerOffDec))  * centerOffRa) : Util.tand(centerOffRa);
			    double yv = isInSphericalCoords() ? Math.toRadians(centerOffDec) : Util.tand(centerOffDec);
			    // TODO : ce tand(circleRadius) me semble étrange ...
			    // TODO : doit fonctionner car tand(x)=x pour un x petit
			    double rv = isInSphericalCoords() ? Math.toRadians(circleRadius) : Util.tand(circleRadius);

				Cercle c = new Cercle(pf, null, xv, yv, rv);
				v.addElement(c);

				break;
			}

			case PICKLE: {
				if( angle==0 ) {
					Aladin.trace(3, "Can not create a pickle with an angle of 0 !");
					break;   // Pour éviter de créer des cochonneries (à montrer à Thomas PF 14/12/05)
				}
				Pickle p = new Pickle(pf, null, Util.tand(centerOffRa), Util.tand(centerOffDec), Util.tand(internalRad), Util.tand(externalRad), startAngle, angle);
				v.addElement(p);

				break;
			}

			case STRING : {
				Tag t = new Tag(pf);
				t.setText(content);
				t.setXYTan(Util.tand(ra), Util.tand(dec));
				v.addElement(t);

				break;
			}

			default : {

				break;
			}

		}

		// ajout des objets provenant des subSubsFootprints
		Enumeration<SubFootprintBean> e = subSubsFootprints.elements();
		SubFootprintBean sub;
		while( e.hasMoreElements() ) {
			sub = e.nextElement();
			addArrayObjectsToVector(v, sub.buildObjets(pf));
		}

		Obj[] o = new Obj[v.size()];
		v.copyInto(o);
		v = null;

		return o;
	}

	static protected void addArrayObjectsToVector(Vector<Obj> v, Obj[] o) {
		if( o==null ) return;
		for( int i=0; i<o.length; i++ ) {
			if( o[i]==null ) continue;
			v.addElement(o[i]);
		}
	}

	protected String getName() {
		return name==null?"":name;
	}

	// retourne une chaine vide pour le moment
	protected String getDesc() {
		return "";
	}

	/** Retourne la couleur du FoV, null si couleur non précisée
	 *
	 * @return
	 */
	protected Color getColor() {
		return c;
	}

	protected void setColor(Color c) {
		this.c = c;
	}

	/**
	 * @param name The name to set.
	 */
	protected void setName(String name) {
		this.name = name;
	}
}
