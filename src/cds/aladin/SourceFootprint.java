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

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;

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
	private Fov stcsFov;
	private boolean showFootprint = false; // doit-on montrer le footprint associé
	private boolean transientShow = false; // Le footprint n'est montré que temporairement (mouseMove dessus par exemple) (PF- nov 17)
	private int idxFootprint = -1; // index du footprint

	/** Constructeur */
	public SourceFootprint() {}

	/** Clonage basique (sans reprendre les flags d'affichage */
	public SourceFootprint copy() {
	   SourceFootprint s = new SourceFootprint();
	   s.footprint = footprint;
	   s.stcsFov = stcsFov;
	   s.idxFootprint = idxFootprint;
	   return s;
	}
	
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
	
	/** PF - Nov 2017 - retourne la liste des STCs qui forment le footprint */
	protected List<STCObj> getStcObjects() {
	   if( stcsFov==null ) return null;
	   return stcsFov.getStcObjects();
	}

	protected void setStcs(double ra, double dec, String stcs) {
	    STCStringParser parser = new STCStringParser();
	    List<STCObj> stcObj = parser.parse(stcs);
	    this.stcsFov = new Fov(stcObj);
	}

	protected void draw(Projection proj, Graphics g, ViewSimple v, int dx, int dy, Color c) {
	    if ( ! showFootprint && !transientShow ) {
	        return;
	    }

	    if (footprint != null) {
	        footprint.c = c;
            footprint.reset(ViewSimple.MOVECENTER);
            footprint.pcat.draw(g, null, v, true, dx, dy);
	    }
	    else if (stcsFov != null) {
	        stcsFov.draw(proj, v, g, dx, dy, c);
	    }
	}
	
	public boolean isSet() {
		return footprint != null || (stcsFov != null && !stcsFov.getStcObjects().isEmpty());
	}

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
    
    protected void setShowFootprintTransient(boolean transientShow) {
       this.transientShow = transientShow;
   }

}
