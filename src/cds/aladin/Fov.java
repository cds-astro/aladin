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

import java.awt.*;

import cds.tools.Util;

/** Field Of View affiché pour une branche d'un MetaDataTree
 *  @author Thomas Boch [CDS]
 *  @version    0.3 08 Oct. 2003 : prise en compte des formes polygonales
 *              0.2 30 Sept. 2003 : Calcul direct des alpha,delta en prenant compte de l'angle de position
 *              0.1 ?? 2003 : kickoff
 */

public class Fov {
    //protected final static double X_SPECTRUM = 1./1000.;
    protected final static double X_SPECTRUM = 0.;
    //protected final static double Y_SPECTRUM = 1./150.;
    protected final static double Y_SPECTRUM = 1./50;

    double alpha; // ra of center
    double delta; // dec of center
    double x; // size in alpha (degrees)
    double y; // size in delta (degrees)
    double[] xTab;
    double[] yTab;
    double angle; // position angle
    double cutout_x; // cutout size in alpha (degrees)
    double cutout_y; // cutout size in delta (degrees)
    Color color = Color.red; // default color

    boolean spectrumFov = false; // true s'il s'agit d'un Fov de spectre

	private PointD[] bords = new PointD[4]; // tableau contenant les 4 bords du fov en alpha, delta

	protected PlanField pf;

	// Constructeurs

	/**
	 * @param alpha RA du centre du fov
	 * @param delta declinaison du centre du fov
	 * @param x taille alpha en degrés
	 * @param y taille delta en degrés
	 * @param angle angle de position en degrés
	 */
    Fov(double alpha, double delta, double x, double y, double angle) {
        this.alpha = alpha;
        this.delta = delta;
        this.x = x;
        this.y = y;
        this.angle = angle;

        computePoints();
    }

    /** Constructeur dans le cas où on a la position du centre et les position des 4 coins
     * (SIAP extensions, dans table Characterization)
     */
    Fov(double alpha, double delta, double[] minRADec, double[] maxRADec, double angle) {
        this.alpha = alpha;
        this.delta = delta;
        this.angle = angle;

        // TODO : tenir compte de l'angle

        bords = new PointD[4];
        bords[0] = new PointD(minRADec[0], minRADec[1]);
        bords[1] = new PointD(minRADec[0], maxRADec[1]);
        bords[2] = new PointD(maxRADec[0], maxRADec[1]);
        bords[3] = new PointD(maxRADec[0], minRADec[1]);
    }

    /**
     * Constructeur pou un FoV complexe , polygonal (typiquement pour une image HST)
     * @param alpha RA du centre du fov
     * @param delta declinaison du centre du fov
     * @param xTab tableau de position des x en degrés par rapport au centre
     * @param yTab tableau de position des y en degrés par rapport au centre
     * @param angle angle de position en degrés
     */
    Fov(double alpha, double delta, double[] xTab, double[] yTab, double angle, double x, double y) {
        this.alpha = alpha;
        this.delta = delta;
        this.xTab = xTab;
        this.yTab = yTab;
        this.angle = angle;
        this.x = x;
        this.y = y;

        computePoints();
    }

	/**
	 * @param sexa coordonnées sous forme de chaine sexagésimale du centre du fov
	 * @param x taille alpha en degrés
	 * @param y taille delta en degrés
	 * @param angle angle de position en degrés
	 */
    Fov(String sexa, double x, double y, double angle) throws Exception {
        Coord coo = new Coord(sexa);

        this.alpha = coo.al;
        this.delta = coo.del;
        this.x = x;
        this.y = y;
        this.angle = angle;

        computePoints();
    }

    /** Constructeur pour un FoV de spectre : on donne alpha, delta et l'angle
     *
     */
    Fov(double alpha, double delta, double angle) {
        this.alpha = alpha;
        this.delta = delta;
        this.angle = angle;


        spectrumFov = true;

        computePoints();
    }

    /** Bidouille pour permettre un affichage d'un PlanField dans SIAP Extensions
     * TODO : à virer a priori
     *
     */
//    Fov(PlanField pf,double alpha, double delta, double angle) {
//    	this.pf = pf;
//
//    	this.alpha = alpha;
//        this.delta = delta;
//        this.angle = angle;
//    }

    FootprintBean fpBean;
    String key;

    /** Bidouille pour permettre un affichage d'un PlanField dans SIAP Extensions
     * TODO : à refactorer plus tard
     *
     */
    Fov(Aladin aladin, FootprintBean fpBean, String key, double alpha, double delta, double angle) {
    	this.pf = new PlanField(aladin, fpBean, key);
		this.pf.make(alpha,delta,angle);

		this.fpBean = fpBean;
		this.key = key;

    	this.alpha = alpha;
        this.delta = delta;
        this.angle = angle;
    }

	// FIN Constructeurs

	/** Calcule les bords du fov (en tenant compte de l'angle) */
    private void computePoints() {
        double curAlpha,curDelta;
        double angleRad= deg2rad(-angle);

        // Calcul pour forme polygonale
        if( xTab!=null && yTab!=null ) {
            bords = new PointD[xTab.length];
            for( int i=0; i<xTab.length; i++ ) {
                curDelta = delta + yTab[i]*Math.cos(angleRad)+xTab[i]*Math.sin(angleRad);
                curAlpha = alpha + ( xTab[i]*Math.cos(angleRad)-yTab[i]*Math.sin(angleRad) ) / Math.cos(deg2rad(curDelta));
                bords[i] = new PointD(curAlpha,curDelta);
            }
        }

        // Calcul pour forme rectangulaire
        else {
            int signX, signY;
            double xUsed, yUsed;
            if( spectrumFov ) {
                xUsed = X_SPECTRUM;
                yUsed = Y_SPECTRUM;
            }
            else {
                xUsed = x;
                yUsed = y;
            }

            for( int i=0;i<4;i++ ) {
                signX = (i==1|| i==2)?-1:1;
                signY = (i<2)?1:-1;

                curDelta = delta + signY*yUsed/2.*Math.cos(angleRad)+signX*xUsed/2.*Math.sin(angleRad);
                curAlpha = alpha + ( signX*xUsed/2.*Math.cos(angleRad)-signY*yUsed/2.*Math.sin(angleRad) ) / Math.cos(deg2rad(curDelta));
                bords[i] = new PointD(curAlpha,curDelta);
    	   }
        }
    }

    /**
     * Convertit des degrés en radians
     * @param angle angle en degrés
     * @return double angle en radians
     */
    static private double deg2rad(double angle) {
        return angle*Math.PI/180.0;
    }


	/** Retourne dans la vue courante le tableau des points constituant le bord du fov
	 *  @param proj la projection du plan de référence
	 *  @param zv le zoomview courant
	 *  Modif PF 02/05 - return null si Exception
	 */
    protected Point[] getBorders(Projection proj, ViewSimple v) {
		Point[] tab = new Point[bords.length];
        Coord coord;
        // recherche des coordonnees du centre dans le repere courant (nécessaire pour la rotation)
		coord = new Coord(alpha,delta);
        try {
           coord = proj.getXY(coord);
           if( Double.isNaN(coord.x) ) return null;

      		// calcul des n bords
      		for( int i=0;i<bords.length;i++ ) {
              	coord = new Coord(bords[i].x,bords[i].y);
              	coord = proj.getXY(coord);
              	if( Double.isNaN(coord.x) ) return null;

              	tab[i] = v.getViewCoord(coord.x,coord.y);
              	if( tab[i]==null ) return null;
      		}
        } catch( Exception e ) { return null; }
		return tab;
	}

    /** Returns borders for a spectrum fov
     *
     * @param proj current projection
     * @param zv current zoomview
     * @return PointD[]
     */
    protected PointD[] getBordersSpectrum(Projection proj, ViewSimple v) {
    // TODO : A REFLECHIR, ça me semble bizarre
//if( true ) return null;
        PointD[] tab = new PointD[bords.length];
        Coord coord;
        // recherche des coordonnees du centre dans le repere courant (nécessaire pour la rotation)
        coord = new Coord(alpha,delta);
        try {
           coord = proj.getXY(coord);
           if( Double.isNaN(coord.x) ) return null;

//A REFLECHIR        center = v.getViewCoordDble(coord.x,coord.y);

           // calcul des n bords
           for( int i=0;i<bords.length;i++ ) {
               coord = new Coord(bords[i].x,bords[i].y);
               coord = proj.getXY(coord);
               if( Double.isNaN(coord.x) ) return null;
//             TODO : A REFLECHIR
          tab[i] = v.getViewCoordDble(coord.x,coord.y);
           }
        } catch( Exception e ) { }
        return tab;
    }

	/** Retourne les n points (sans tenir compte du getViewCoord) */
	protected PointD[] getPoints(Projection proj) {
        PointD[] tab = new PointD[bords.length];
        Coord coord;
        // recherche des coordonnees du centre dans le repere courant (nécessaire pour la rotation)
        coord = new Coord(alpha,delta);
        try {
           coord = proj.getXY(coord);
           if( Double.isNaN(coord.x) ) return tab;

           // calcul des n bords
           for( int i=0;i<bords.length;i++ ) {
               coord = new Coord(bords[i].x,bords[i].y);
               coord = proj.getXY(coord);
               if( Double.isNaN(coord.x) ) return tab;

               tab[i] = new PointD(coord.x,coord.y);
           }
         } catch( Exception e ) { }
        return tab;
	}

//    private int SPEC_PIX_SIZE = 10;
	/** Dessine le fov dans le contexte graphique g
	 *	@param proj la projection du plan de référence
	 *	@param zv zoomview courant
	 *	@param g contexte graphique où dessiner
	 *	@param x décalage en x
	 *	@param y décalage en y
	 *	@param col si on veut préciser la couleur
	 *MODIF PF 02/05 return immédiat si bordsXY==null
	 */
	protected void draw(Projection proj, ViewSimple v, Graphics g, int dx, int dy, Color col) {

        // pour les spectres
        if( spectrumFov ) {
            PointD[] bordsXY = getBordersSpectrum(proj,v);
            //System.out.println(bordsXY[0].x);
//            PointD center = new PointD((bordsXY[0].x+bordsXY[2].x)/2, (bordsXY[0].y+bordsXY[2].y)/2);
//            double xLen, yLen;
//            double l = Math.sqrt(Math.pow(bordsXY[0].x-center.x,2)+Math.pow(bordsXY[0].y-center.y,2));
//            xLen = ((bordsXY[0].x-center.x)*SPEC_PIX_SIZE/l);
//            yLen = ((bordsXY[0].y-center.y)*SPEC_PIX_SIZE/l);
            g.setColor(col!=null?col:color);
            g.setFont(Aladin.BOLD);
            Coord coord = new Coord(alpha,delta);

            coord = proj.getXY(coord);
            PointD center = v.getViewCoordDble(coord.x,coord.y);
            if( Double.isNaN(center.x) || Double.isNaN(center.y) ) return;
            g.drawString("S", (int)center.x, (int)center.y);
//            g.drawLine( (int)(center.x+xLen), (int)(center.y+yLen),
//                        (int)(center.x-xLen), (int)(center.y-yLen) );
            return;
        }

        // dessin pour le PlanField
        if( pf!=null ) {
        	pf.c = this.color;
    	    pf.pcat.draw(g,null,v,true,dx,dy);
        	return;
        }

		Point[] bordsXY = getBorders(proj,v);
		if( bordsXY==null ) return;


		g.setColor(col!=null?col:color);
		/*
		System.out.println(alpha);
		System.out.println(delta);
		System.out.println(x);
        System.out.println(y);
        */

		int[] xCoord = new int[bordsXY.length];
		int[] yCoord = new int[bordsXY.length];

		for( int i=0 ; i<bordsXY.length ; i++ ) {
			xCoord[i] = bordsXY[i].x+dx;
			yCoord[i] = bordsXY[i].y+dy;
		}

		// gestion de la transparence
		if( Aladin.ENABLE_FOOTPRINT_OPACITY && g instanceof Graphics2D ) {
			   Graphics2D g2d=null;
			   Composite saveComposite=null;
			   g2d = (Graphics2D)g;
			   saveComposite = g2d.getComposite();
			   Composite myComposite = Util.getFootprintComposite(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);
			   g2d.setComposite(myComposite);

			   // fill FoV polygon
			   g2d.fill(new Polygon(xCoord, yCoord, xCoord.length));


			   // restore previous composite
			   g2d.setComposite(saveComposite);

		}
		// fin gestion de la transparence

		int iNext;
		for( int i=0; i<bordsXY.length; i++ ) {
			iNext = (i+1)%bordsXY.length;
			g.drawLine(xCoord[i], yCoord[i], xCoord[iNext], yCoord[iNext]);
		}
	}

    /** teste si le point (x,y) tombe dans le fov
	 * @param x coordonnée en x dans repère courant
	 * @param y coordonnée en y dans repère courant
	 * @param ref le plan de référence
	 * @param zv zoomview courant
	 * MODIF PF 02/05-return false si bordsXY==null
	 */
    protected boolean contains(double x, double y, Plan ref, ViewSimple v) {
        if( spectrumFov ) return false;

        // TODO : à réfléchir
        if( pf!=null ) return false;

        // tableaux permettant de constuire le polygone
        int[] cx = new int[bords.length];
        int[] cy = new int[bords.length];

		Point[] bordsXY = getBorders(v.getProj(),v);
		if( bordsXY==null ) return false;
		for( int i=0; i<bords.length; i++ ) {
			cx[i] = bordsXY[i].x;
			cy[i] = bordsXY[i].y;
		}
        Polygon polygon = new Polygon(cx,cy,bords.length);

        return polygon.contains(x,y);
    }

   /** rotation de p autour de centre d'un angle angle degres (angle : du N vers l'E)
	* @param p point initial
	* @param centre centre de la rotation
	* @param angle angle de la rotation (en degrés)
	* @return le point résultant de la rotation
	*/
   /*
   private PointD rotation(PointD p, PointD centre, double angle) {
     double angleRad = -angle*Math.PI/180.0;

     double xx = centre.x + (p.x-centre.x)*Math.cos(angleRad) - (p.y-centre.y)*Math.sin(angleRad);
     double yy = centre.y + (p.x-centre.x)*Math.sin(angleRad) + (p.y-centre.y)*Math.cos(angleRad);

     return new PointD(xx, yy);
   }
   */

   /** rotation de p autour de centre d'un angle angle degres (angle : du N vers l'E)
	* @param p point initial
	* @param centre centre de la rotation
	* @param angle angle de la rotation (en degrés)
	* @return le point résultant de la rotation
	*/
   /*
   private Point rotation(Point p, Point centre, double angle) {
     double angleRad = -angle*Math.PI/180.0;

     double xx = centre.x + (p.x-centre.x)*Math.cos(angleRad) - (p.y-centre.y)*Math.sin(angleRad);
     double yy = centre.y + (p.x-centre.x)*Math.sin(angleRad) + (p.y-centre.y)*Math.cos(angleRad);

     return new Point((int)xx, (int)yy);
   }
   */


   protected PointD[] getPoints() {
       return bords;
   }

   private String size;
   /**
    * retourne la taille sous forme de chaine
    * @param cutout si on veut la taille du cutout
    * @return String
    */
   protected String getSizeStr(boolean cutout) {
       if( spectrumFov ) return "";
       // TODO : à réfléchir dans le cas d'un PlanField
       if( pf!=null ) return "";

       if( size==null ) {
           if( cutout ) size = TreeBuilder.getUnit(Double.toString(cutout_x),"deg")+" x "+TreeBuilder.getUnit(Double.toString(cutout_y),"deg");
           else size = TreeBuilder.getUnit(Double.toString(x),"deg")+" x "+TreeBuilder.getUnit(Double.toString(y),"deg");
       }
       return size;
   }




}
