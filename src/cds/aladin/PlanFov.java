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

package cds.aladin;

import java.util.ArrayList;
import java.util.Iterator;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCPolygon;

// TODO : supprimer cette classe au profit de PlanField

/** Plan graphique dédié à l'affichage d'un ou plusieurs FoV
 * 	@author Thomas Boch [CDS]
 *  @version    0.2 30 Sept. 2003 : Réécriture suite à la réécriture de Fov
 *              0.1 Sept. 2003 : kickoff
 */

public class PlanFov extends Plan {
	private Fov[] fov;

	// Constructeurs

	/**
	 * @param aladin référence à l'objet Aladin
	 * @param label Le label du plan
	 * @param fov tableau des Fov à afficher
	 */
	PlanFov(Aladin aladin, String label, Fov[] fov) {
		this.aladin= aladin;
		type       = FOV;
		c          = Couleur.getNextDefault(aladin.calque);
		setLabel(label);
		pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
		//aladin.calque.unSelectAllPlan();
		selected   = true;
		//objet	   = "";

	    //super(aladin, label);
		//flagProj=true;
		this.fov = fov;
	    suite();
	    flagOk = true;
	    askActive=true;
	    aladin.view.repaintAll();
	    aladin.calque.repaintAll();
	}
	
	private static double max(double a, double b) {
		return a>b?a:b;
	}

	private static double min(double a, double b) {
		return a<b?a:b;
	}

	private static double abs(double a) {
		return a<0?-a:a;
	}

	/** Retourne une projection adaptée aux fields of view
	 *
	 * @param fovs les fields of view pour lesquels on veut une projection
	 * @return Projection
	 */
	static protected Projection getProjection(Fov[] fovs) {
		// coordonnées du barycentre des fov
		double ra,de;
		ra=de=0;
		double minRA,maxRA,minDE,maxDE;
        minRA = maxRA = fovs[0].alpha;
		minDE = maxDE = fovs[0].delta;
		double raTmp,deTmp;
        int nbProcessed=0;
        double x,y;
		for( int i=0; i<fovs.length; i++ ) {
            if( fovs[i]==null ) continue;
			//System.out.println("alpha : "+fovs[i].alpha);
			raTmp = fovs[i].alpha;
			deTmp = fovs[i].delta;

			if( raTmp>180. ) raTmp = -360.+raTmp;
			if( deTmp>180. ) deTmp = -360.+deTmp;
			//System.out.println("raTmp : "+raTmp);

			ra += raTmp;
			de += deTmp;
            if( fovs[i].spectrumFov ) {
                x = Fov.X_SPECTRUM;
                y = Fov.Y_SPECTRUM;
            }
            else {
                x = fovs[i].x;
                y = fovs[i].y;
            }

			minRA = min(minRA,raTmp-x);
			maxRA = max(maxRA,raTmp+x);
			minDE = min(minDE,deTmp-y);
			maxDE = max(maxDE,deTmp+y);

            nbProcessed++;
		}
		//System.out.println("ra total : "+ra);
		ra = ra/nbProcessed;
		de = de/nbProcessed;
		minRA = minRA%360.;
		maxRA = maxRA%360.;
		minDE = minDE%360.;
		maxDE = maxDE%360.;
		//System.out.println("ra: "+ra);
		//System.out.println("de: "+de);
		//System.out.println("minRA: "+minRA);
		//System.out.println("maxRA: "+maxRA);
		//System.out.println("minDE: "+minDE);
		//System.out.println("maxDE: "+maxDE);
		double[] diff = new double[4];
		diff[0] = abs(de-minDE);
		diff[1] = abs(de-maxDE);
		diff[2] = abs(ra-minRA);
		diff[3] = abs(ra-maxRA);
		for( int i=0; i<4; i++ ) {
			if( diff[i]>180. ) diff[i] = abs(-360.+diff[i]);
		}
		//System.out.println("le barycentre a pour coordonnées : "+ra+"  "+de);
		double radius = max( max( diff[0], diff[1] ), max( diff[2], diff[3] ) );
		//System.out.println("radius vaut : "+radius);
		return new Projection(null,Projection.SIMPLE,ra,de,radius*60*2,250.0,250.0,500.0,0.0,false,Calib.TAN,Calib.FK5);
	}

	/**
	 * Construit les objets Ligne correspondant aux différents FoV
	 */
	private void suite() {
        setOpacityLevel(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);

        PointD curPoint;
        Fov curFov;
        Ligne curLine;
        Plan ref = aladin.calque.getPlanRef();
        //Projection proj;

        if( fov==null || fov.length==0 ) return;

		if( ref==null || !Projection.isOk(ref.projd) ) {
			//System.out.println(fov[0].x*60.);
			projd = getProjection(fov);
		}
		else projd = ref.projd;

		// boucle sur tous les FoV
        for( int i=0; i<fov.length; i++ ) {
            curFov = fov[i];
            if( curFov==null ) continue;

            PlanField pf;
            if( curFov.pf!=null ) {
            	// on doit recréer un nouvel objet PlanField
            	// TODO : à simplifier pour limiter la création denouveaux objets
            	pf = new PlanField(aladin, curFov.fpBean, curFov.key);
        		pf.make(curFov.alpha,curFov.delta,curFov.angle);

                Iterator<Obj> it = pf.pcat.iterator();
                while( it.hasNext() ) {
                   Obj o = it.next();
                   if( o instanceof Position ) ((Position)o).plan = this;
                   pcat.setObjet(o);
                }

            }

            ArrayList<PointD[]> polygons = new ArrayList<PointD[]>();
            if (curFov.getStcObjects() != null) {
                Iterator<STCObj> itStcObjs = curFov.getStcObjects().iterator();
                while (itStcObjs.hasNext()) {
                    STCObj stcObj = itStcObjs.next();
                    if (stcObj.getShapeType() != STCObj.ShapeType.POLYGON) {
                        continue;
                    }
                    STCPolygon stcPolygon = (STCPolygon) stcObj;

                    PointD[] polygonBords = new PointD[stcPolygon.getxCorners().size()];
                    for (int k=0; k<polygonBords.length; k++) {
                        polygonBords[k] = new PointD(stcPolygon.getxCorners().get(k), stcPolygon.getyCorners().get(k));
                    }

                    polygons.add(polygonBords);
                }
            }
            else {
                polygons.add(curFov.getPoints());
            }

            // boucle sur chaque polygone du fov
            Iterator<PointD[]> itPoly = polygons.iterator();
            while (itPoly.hasNext()) {
                PointD[] polygon = itPoly.next();

                curPoint = polygon[polygon.length-1];

                if( curPoint==null ) continue;
                // on initialise curLine
                curLine = new Ligne(this);
                curLine.raj = curPoint.x;
                curLine.dej = curPoint.y;
                pcat.setObjet(curLine);

                // boucle sur les n points du FoV
                for( int j=0; j<polygon.length; j++ ) {
                    curPoint = polygon[j];
                    Ligne newLine = new Ligne(this);
                    newLine.raj = curPoint.x;
                    newLine.dej = curPoint.y;
                    newLine.debligne = curLine;
                    pcat.setObjet(newLine);
                    curLine = newLine;
                }
            }
        }
	}

	protected boolean Free() {
		fov = null;
		return super.Free();
	}
}
