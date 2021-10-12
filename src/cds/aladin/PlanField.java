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
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import cds.astro.AstroMath;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Coo;
import cds.astro.Proj3;
import cds.tools.Util;
import cds.tools.VOApp;


/**
 * FoV Aladin Plane
 *
 * @author Pierre Fernique [CDS]
 * @version 1.4 : 19 jan 2009 : Rotation center support
 * @version 1.3 : 31 aug 2005 : cleaning and translation in english (STScI collaboration)
 * @version 1.2 : (24 mai 2002) methode reset() pour la reprojection apres translation
 * @version 1.1 : (7 dec 00) toilettage du code + ajout d'un gap pour CFH12K
 * @version 1.0 : (14 sept 00) creation
 */
public final class PlanField extends Plan {

   // Instrument FoV predefined
   private static final int CFH12K    = 0;
   private static final int EPICMOS   = 1;
   private static final int EPICPN    = 2;
   //   private static final int WFPC2     = 3;
   private static final int MEGACAM   = 3;
   private static final int MEGAPRIME = 4;
   private static final int WIRCAM    = 5;
   private static final int ESPADONS  = 6;

   private static String [] INSTR= { "CFH12K", "EPICMOS", "EPICpn", // "WFPC2",
      "MEGACAM", "MEGAPRIME", "WIRCAM",
      "ESPADONS",};

   private int instr;		   // Instrument FoV code (CFH12K, EPICMOS...)
   private boolean flagRoll=true;   // True if the Aperture is rollable
   private boolean flagCenterRoll=false; // true if the roll center is movable
   private boolean flagMove=true;   // True if the Aperture can be moved
   private double roll;		   // Current angle in degrees (if it is rollable)
   protected boolean needTarget;  // True si on attend la resolution du target

   private boolean almaFP = false; // True if the current instance is an ALMA footprint

   private FootprintBean fpBean;

   // Conventions for predefined instrument FoV which are rectangular arrays
   // of CCDs separated by gaps.
   //    XXX_RA : CCD width in degrees
   //    XXX_DE : CCD height in degrees
   //    XXX_GAPRA : RA gap between each CCD in arcsec
   //    XXX_GAPDE : DE gap between each CCD in arcsec
   //    XXX_DRA : offset factor in RA (0:no move, -1:at left with an offset of 1 CCD width+gapRA,+1:...)
   //    XXX_DDE : the same in DE
   //    XXX_ORA : RA offset instrument field center in degres
   //    XXX_ODE : DE offset instrument field center in degres
   //    XXX_CCD : CCD labels


   private static final double EPICMOS_RA = 10.9/60.0;
   private static final double EPICMOS_DE = 10.9/60.0;
   private static final double EPICMOS_DRA[] =   { -1.5,-1.5,-0.5,-0.5,-0.5, 0.5,0.5 };
   private static final double EPICMOS_DDE[] =   { -1.0, 0.0,-1.5,-0.5, 0.5,-1.0,0.0 };

   private static final double EPICPN_RA = 4.4/60.0;
   private static final double EPICPN_DE = 13.6/60.0;
   private static final double EPICPN_DRA[] =   {-3.,-2.,-1., 0., 1., 2., -3.,-2.,-1.,0.,1.,2.};
   private static final double EPICPN_DDE[] =   {-1.,-1.,-1.,-1.,-1.,-1.,  0., 0., 0.,0.,0.,0.};

   private static final double CFH12K_RA = 2048*0.206/3600.;
   private static final double CFH12K_DE = 4096*0.206/3600.;
   private static final double CFH12K_DRA[] =   {  -3.,-2.,-1., 0., 1., 2., -3.,-2.,-1.,0.,1., 2.};
   private static final double CFH12K_DDE[] =   {  -1.,-1.,-1.,-1.,-1.,-1.,  0., 0., 0.,0.,0., 0.};
   private static final double CFH12K_GAPRA[] = { -15.,-9.,-3., 3., 9.,15.,-15.,-9.,-3.,3, 9.,15.};
   private static final double CFH12K_GAPDE[] = {  -3.,-3.,-3.,-3.,-3.,-3.,  3., 3., 3.,3.,3., 3.};
   private static final double CFH12K_ORA = (-15.0/3600.0); // 15 arcsecs
   private static final double CFH12K_ODE = (30.0/3600.0);  // 30 arcsecs

   // CFH12K CCD labels (specifical definitions)
   private static final String CFH12K_CCD[] =   {  	"06","07","08","09","10","11",
      "00","01","02","03","04","05"};

   private static final double MEGACAM_RA = 2048*0.187/3600.;
   private static final double MEGACAM_DE = 4612*0.187/3600.;

   // Position des CCDs de la mosaique MEGACAM

   /* Mosaique avec les "oreilles" - non utilise
   private static final double MEGACAM_DRA[] =   {      -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
                                                   -5.5,-4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,4.5,
                                                   -5.5,-4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,4.5,
                                                        -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
                                                 };
    */
   // Mosaique sans les "oreilles"
   private static final double MEGACAM_DRA[] =   {      -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
      -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
      -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
      -4.5,-3.5,-2.5,-1.5,-0.5,0.5,1.5,2.5,3.5,
   };

   /* Mosaique avec les "oreilles" - non utilise
   private static final double MEGACAM_DDE[] =   {    -2,-2,-2,-2,-2,-2,-2,-2,-2,
                                                   -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
                                                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                                       1, 1, 1, 1, 1, 1, 1, 1, 1                                                							  };
   						 };
    */
   // Mosaique sans les "oreilles"
   private static final double MEGACAM_DDE[] =   {    -2,-2,-2,-2,-2,-2,-2,-2,-2,
      -1,-1,-1,-1,-1,-1,-1,-1,-1,
      0, 0, 0, 0, 0, 0, 0, 0, 0,
      1, 1, 1, 1, 1, 1, 1, 1, 1
   };
   // Gaps (additifs) entre les CCDs de la mosaique MEGACAM
   /* Mosaique avec les "oreilles" - non utilise
   private static final double MEGACAM_GAPRA[] = {     -56,-42,-28,-14,0,14,28,42,56,
                                                   -70,-56,-42,-28,-14,0,14,28,42,56,70,
                                                   -70,-56,-42,-28,-14,0,14,28,42,56,70,
                                                       -56,-42,-28,-14,0,14,28,42,56,
                                                 };
    */
   // Mosaique sans les "oreilles"
   private static final double MEGACAM_GAPRA[] = {     -56,-42,-28,-14,0,14,28,42,56,
      -56,-42,-28,-14,0,14,28,42,56,
      -56,-42,-28,-14,0,14,28,42,56,
      -56,-42,-28,-14,0,14,28,42,56,
   };

   /* Mosaique avec les "oreilles" - non utilise
   private static final double MEGACAM_GAPDE[] = {        -88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,
                                                     -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5,
                                                      5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,
                                                           88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5,
                                                 };
    */
   // Mosaique sans les "oreilles"
   private static final double MEGACAM_GAPDE[] = {        -88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,-88.5,
      -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5, -5.5,
      5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,  5.5,
      88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5, 88.5,
   };

   // Offset du centre optique par rapport au centre de la mosaique MEGACAM
   // Per CV,2003.01 (15as,15as) of NW corner of Amp 44, chip 22
   private static final double MEGACAM_ORA = (15.0/3600.0); 	// 15 arcsecs
   private static final double MEGACAM_ODE = (-20.5/3600.0); 	// 15 arcsecs + 1/2gap=5.5 = 20.5 arcsecs

   // Nom des CCDs a afficher sur la mosaique MEGACAM: NB: Les 2 CCDs exterieurs W et E ne sont pas couvertes
   // par les filtres et sont donc en dernier

   /* Mosaique avec les "oreilles" - non utilise
   private static final String MEGACAM_CCD[] =   {       "35","34","33","32","31","30","29","28","27",
                                                    "39","26","25","24","23","22","21","20","19","18","38",
                                                    "37","17","16","15","14","13","12","11","10","09","36",
                                                         "08","07","06","05","04","03","02","01","00",
                                                 };
    */
   private static final String MEGACAM_CCD[] =   {       "35","34","33","32","31","30","29","28","27",
      "26","25","24","23","22","21","20","19","18",
      "17","16","15","14","13","12","11","10","09",
      "08","07","06","05","04","03","02","01","00",
   };

   // MEGAPRIME = MEGACAM + MEGAGUI (zones de guidage). On definit la les zones de guidage...
   // Taille des zones de guidage
   private static final double MEGAGUI_RA = 1233.0/3600.0; // per BC,2002.01.08 90mm*13.7as/mm old:6144*0.187/3600.;
   private static final double MEGAGUI_DE = 890.5/3600.0;  // per BC,2002.01.08 65mm*13.7as/mm old:2048*0.187/3600.;
   // Geometrie des zones de guidage
   private static final double MEGAGUI_DRA[] =   {-0.5,-0.5	};
   private static final double MEGAGUI_DDE[] =   {-1,0};
   // Gaps pour positionner les zones de guidage
   private static final double MEGAGUI_GAPRA[] = {0,0};
   // per BC,2002.01.08, 127mm*13.7as
   private static final double MEGAGUI_GAPDE[] = {-1739.9,+1739.9};  // in arcsec
   // Nom des zones de guidage: S=South, N=North
   private static final String MEGAGUI_BOX[] =   {"S","N"};

   // Define WIRCAM field of view
   private static final double WIRCAM_RA = 2048*0.3/3600.;
   private static final double WIRCAM_DE = 2048*0.3/3600.;

   private static final double WIRCAM_DRA[] = {-1, 0, -1, 0};
   private static final double WIRCAM_DDE[] = {-1, -1, 0, 0};

   private static final double WIRCAM_GAPRA[] = {-45, 0, -45, 0};
   private static final double WIRCAM_GAPDE[] = {-45,-45,  0, 0};

   private static final double WIRCAM_ORA = (30/3600.0);
   private static final double WIRCAM_ODE = (30/3600.0);

   private static final String WIRCAM_CCD[] = {"52","54","77","60"};
   private static final double WIRCAM_RD = 0.;

   //Define ESPADON field of view.
   private static final double ESPADONS_RA = 2/60.;
   private static final double ESPADONS_DE = 2/60.;

   private static final double ESPADONS_DRA[] = {-0.5};
   private static final double ESPADONS_DDE[] = {-0.5};

   private static final double ESPADONS_GAPRA[] = {0};
   private static final double ESPADONS_GAPDE[] = {0};

   private static final double ESPADONS_ORA = (0);
   private static final double ESPADONS_ODE = (0);

   private static final String ESPADONS_CCD[] = {""};
   private static final double ESPADONS_RD = 0.;

   public static final double tand(double x) { return Math.tan( x*(Math.PI/180.0) ); }

   private boolean showSubFPInProperties = true; // doit-on montrer les sous-parties du footprint dans les properties


   /** Plan Field creation
    * @param target target astronomical object or J2000 coordinates
    * @param roll roll angle
    * @param label Aladin plane label
    * @param instrument instrument name (see INST[])
    */
   protected PlanField(Aladin aladin, String target ,double roll,
         String label,String instrument) {
      this.aladin= aladin;
      type       = APERTURE;
      c          = Couleur.getNextDefault(aladin.calque);
      setLabel(label);
      setOpacityLevel(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);
      pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
      aladin.calque.selectPlan(this);
      objet	 = null;

      // set instrument code
      setInstr(instrument);

      // Determination du target.
      // S'il s'agit d'un nom d'objet, il va y avoir une demande
      // de resolution Simbad pour le plan lui-meme, (objet renseigne)
      // si le target n'est pas mentionne, on n'attendra la resolutoin
      // Simbad du prochain plan.

      // Target computing. In case of astronomical object name, the instrument FoV will
      // be built when the CDS sesame resolver will provide the corresponding coordinates
      // via the PlanField.setCenter() method
      if( target.length()==0 || Localisation.notCoord(target) ) {
         needTarget=true;
         setActivated(false);
         this.roll=roll;
         if( target.length()>0 ) objet=target;

         // The FoV instrument can be immediately built
      } else {
         Coord co;
         try { co = new Coord(target); }
         catch( Exception e ) { error = aladin.error="Unknown object"; return; }
         needTarget=false;
         setActivated(true);
         make(co.al,co.del,roll);
      }
   }

   protected PlanField(Aladin aladin, String label,Coord center,double angle,boolean canbeRoll,boolean canbeMove) {
      this.aladin= aladin;
      type       = APERTURE;
      c          = Couleur.getNextDefault(aladin.calque);
      setLabel(label);
      setOpacityLevel(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);
      pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
      selected   = true;
      flagOk = true;
      askActive=true;
      flagRoll=canbeRoll;
      flagMove=canbeMove;
      instr=-1;
      roll=angle;
      co= center!=null ? center : new Coord(0,0);

      // Field of View projCenter
      Repere projCenter = new Repere(this,co);
      projCenter.setType(Repere.CENTER);

      // Field of View rotCenter
      Repere rotCenter = new Repere(this,co);
      rotCenter.setRotCenterType(projCenter);

      pcat.setObjetFast(rotCenter);
      pcat.setObjetFast(projCenter);

      needTarget=false;
      make(co.al,co.del,roll);

   }

   /**
    * Constructor for PlanField
    *
    * @param aladin reference to aladin instance
    * @param xOffset array of array of ra offsets in the tangent plane
    * @param yOffset array of array of dec offsets in the tangent plane
    * @param rep center of the field of view
	protected PlanField(Aladin aladin, double[][] xOffset, double[][] yOffset, int[] boundary, String[] names,
			            String label) {
		this.aladin = aladin;
		type = APERTURE;
		c = Couleur.getNextDefault(aladin.calque);
		setLabel(label);
		pcat = new PlanObjet(this, c, aladin.calque, aladin.status, aladin);
		aladin.calque.selectPlan(this);
		objet = null;

		instr = -1;

		setPolygons(xOffset, yOffset, boundary, names);

	}
    */

   /** Constructor for PlanField
    *
    * @param aladin reference to aladin instance
    * @param bean bean holding needed infos to build the PlanField
    * @param label label of the plane
    */
   protected PlanField(Aladin aladin, FootprintBean fpBean, String label) {
      this.aladin = aladin;
      type = APERTURE;
      c = Couleur.getNextDefault(aladin.calque);
      setLabel(label);
      setOpacityLevel(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);
      pcat = new Pcat(this, c, aladin.calque, aladin.status, aladin);
      aladin.calque.selectPlan(this);
      objet = null;
      instr = -1;
      this.fpBean = fpBean;
      setObjects(fpBean);
      needTarget=false;
   }

   protected PlanField(Aladin aladin,String target,FootprintBean fpBean,String label,double roll) {
      this.aladin = aladin;
      type = APERTURE;
      c = Couleur.getNextDefault(aladin.calque);
      setLabel(label);
      setOpacityLevel(Aladin.DEFAULT_FOOTPRINT_OPACITY_LEVEL);
      pcat = new Pcat(this, c, aladin.calque, aladin.status, aladin);
      aladin.calque.selectPlan(this);
      objet = null;

      instr = -1;
      this.fpBean = fpBean;
      setObjects(fpBean);


      // Determination du target.
      // S'il s'agit d'un nom d'objet, il va y avoir une demande
      // de resolution Simbad pour le plan lui-meme, (objet renseigne)
      // si le target n'est pas mentionne, on n'attendra la resolutoin
      // Simbad du prochain plan.

      // Target computing. In case of astronomical object name, the instrument FoV will
      // be built when the CDS sesame resolver will provide the corresponding coordinates
      // via the PlanField.setCenter() method
      if( target==null || target.length()==0 || Localisation.notCoord(target) ) {
         needTarget=true;
         setActivated(false);
         this.roll=roll;
         if( target.length()>0 ) objet=target;

         // The FoV instrument can be immediately built
      } else {
         Coord co;
         try { co = new Coord(target); }
         catch( Exception e ) { error = aladin.error="Unknown object"; return; }
         needTarget=false;
         setActivated(true);
         make(co.al,co.del,roll);
      }
   }

   protected void setLabel(String s) {
      if( s==null ) s="FoV";
      super.setLabel(s);
   }

   /** Constructeur par recopie
    * (marche pas très bien pour le moment)
    * @param aladin
    * @param pf
    * @param target
    * @param roll

	protected PlanField(Aladin aladin,PlanField pf,String target,double roll,String label) {
	      this.aladin= aladin;
	      type       = APERTURE;
	      c          = Couleur.getNextDefault(aladin.calque);
	      setLabel(label);
	      pcat       = new PlanObjet(this,c,aladin.calque,aladin.status,aladin);
	      aladin.calque.selectPlan(this);
	      objet	 = null;

	      this.flagRoll = pf.flagRoll;
	      this.instr = pf.instr;

	      pcat = pf.pcat;


	      // Determination du target.
	      // S'il s'agit d'un nom d'objet, il va y avoir une demande
	      // de resolution Simbad pour le plan lui-meme, (objet renseigne)
	      // si le target n'est pas mentionne, on n'attendra la resolutoin
	      // Simbad du prochain plan.

	      // Target computing. In case of astronomical object name, the instrument FoV will
	      // be built when the CDS sesame resolver will provide the corresponding coordinates
	      // via the PlanField.setCenter() method
	      if( target.length()==0 || View.notCoord(target) ) {
	         setActivated(false);
	         flagOk=false;
	         this.roll=roll;
	         if( target.length()>0 ) objet=target;

	      // The FoV instrument can be immediately built
	      } else {
	         Coord co;
	         try { co = new Coord(target); }
	         catch( Exception e ) { error = aladin.error="Unknown object"; return; }
	         setActivated(true);
	         flagOk     = true;
	         make(co.al,co.del,roll);
	      }
	}
    */

   /**
    * Post treatement for the creation of the FoV after a required CDS sesame
    *  resolution (see constructor)
    * @param ra,de fov center
    * @return false if the FoV is already ok, true otherwise
    */
   protected boolean resolveTarget(double ra,double de) {
      //      if( flagOk ) return false;
      //System.out.println("setCenter for "+this+" ra="+ra+" de="+de);
      needTarget=false;
      make(ra,de,roll);
      planReady(true);
      return true;
   }

   // Lorsqu'on désactive un plan Field, on déselectionne automatiquement
   // tous ses objets.
   protected boolean setActivated() {
      boolean rep = super.setActivated();
      if( !rep ) {
         aladin.view.unSelectObjetPlanField(this);
      }
      return rep;

   }

   /** Build or rebuild the FoV with ra,dec center and roll rotation angle
    * @param ra,de FoV center
    * @param roll rotation angle from the North
    */
   protected void make(double ra,double de,double roll) {
      make(ra,de,ra,de,roll);
   }
   protected void make(double ra,double de,double raRot, double deRot, double roll) {
      projd = new Projection(null,Projection.SIMPLE,
            ra,de,/*instr==WFPC2?5:*/instr==MEGACAM || instr==MEGAPRIME ?100:45,
                  250,250,500,
                  0,false,Calib.TAN,Calib.FK5);
      switch(instr) {
         case MEGACAM:
            setRollable(false);
            makeCCDs(MEGACAM_ORA,MEGACAM_ODE,MEGACAM_RA,MEGACAM_DE,MEGACAM_DRA,MEGACAM_DDE,
                  MEGACAM_GAPRA,MEGACAM_GAPDE,MEGACAM_CCD);
            break;
         case MEGAPRIME:
            setRollable(false);
            makeCCDs(MEGACAM_ORA,MEGACAM_ODE,MEGACAM_RA,MEGACAM_DE,MEGACAM_DRA,MEGACAM_DDE,
                  MEGACAM_GAPRA,MEGACAM_GAPDE,MEGACAM_CCD);
            makeCCDs(MEGACAM_ORA,MEGACAM_ODE,MEGAGUI_RA,MEGAGUI_DE,MEGAGUI_DRA,MEGAGUI_DDE,
                  MEGAGUI_GAPRA,MEGAGUI_GAPDE,MEGAGUI_BOX);
            break;
         case CFH12K:
            setRollable(false);
            makeCCDs(CFH12K_ORA,CFH12K_ODE,CFH12K_RA,CFH12K_DE,CFH12K_DRA,CFH12K_DDE,
                  CFH12K_GAPRA,CFH12K_GAPDE,CFH12K_CCD);
            break;
         case WIRCAM:
            setRollable(false);
            makeCCDs(WIRCAM_ORA,WIRCAM_ODE,WIRCAM_RA,WIRCAM_DE,WIRCAM_DRA,WIRCAM_DDE,
                  WIRCAM_GAPRA,WIRCAM_GAPDE,WIRCAM_CCD);
            break;
         case ESPADONS:
            setRollable(false);
            makeCCDs(ESPADONS_ORA,ESPADONS_ODE,ESPADONS_RA,ESPADONS_DE,ESPADONS_DRA,
                  ESPADONS_DDE,ESPADONS_GAPRA,ESPADONS_GAPDE,ESPADONS_CCD);
            break;
         case EPICMOS:
            setRollable(true);
            makeCCDs(0,0,EPICMOS_RA,EPICMOS_DE,EPICMOS_DRA,EPICMOS_DDE,null,null,null);
            break;
         case EPICPN:
            setRollable(true);
            makeCCDs(0,0,EPICPN_RA,EPICPN_DE,EPICPN_DRA,EPICPN_DDE,null,null,null);
            break;
            //          case WFPC2:
            //             setRollable(true);
            //             makeWFPC2();
            //             break;
      }
      setTarget(ra,de,raRot,deRot,roll);
   }

   /** Generic CCD instrument creation */
   private void makeCCDs(double ORA,double ODE, double RA,double DE,
         double DRA[],double DDE[],double GAPRA[],double GAPDE[],String CCD[]) {
      int j=0;		// current drawing object index in pcat.o[]

      // pcat allocation or reallocation
      if( pcat.o!=null ) {
         Obj o[] = pcat.o;
         pcat.o = new Obj[ pcat.nb_o=(o.length + DRA.length*5 + (CCD==null ? 0 : CCD.length) ) ];
         System.arraycopy(o,0,pcat.o,0,o.length);
         j=o.length;
      } else {
         pcat.o = new Obj[pcat.nb_o=(DRA.length*5+2+ (CCD==null ? 0 : CCD.length))];
         j=2;

         // Field of View projection center
         Repere center = new Repere(this); // in x=0, y=0 position by default
         center.setType(Repere.CENTER);
         pcat.o[1] = center;

         // Field of View rotation center
         Repere rotCenter = new Repere(this); // in x=0, y=0 position by default
         rotCenter.setRotCenterType(center);
         pcat.o[0] = rotCenter;
      }

      // i: rectangle counter
      for( int i=0; i<DRA.length; i++ ) {  // i = compteur de rectangles
         Ligne p=null;

         // k: rectangle side counter
         for( int k=0; k<5; k++, j++ ) {
            p = new Ligne(this);
            pcat.o[j] = p;

            double gapra=(GAPRA==null)?0.:GAPRA[i]/3600.;
            double gapde=(GAPDE==null)?0.:GAPDE[i]/3600.;

            p.x = Util.tand( (DRA[i]+((k==1||k==2)?1.0:0.0))*RA+gapra -ORA );
            p.y = Util.tand( (DDE[i]+((k==2||k==3)?1.0:0.0))*DE+gapde -ODE );

            if( k>0 ) p.debligne=(Ligne)pcat.o[j-1];
            if( k<4 ) p.finligne=(Ligne)pcat.o[j+1]; 
            else p.bout=3;
         }

         // CCD labels ?
         if( CCD!=null  ) {
            Tag t = new Tag(this,null,0,0,CCD[i]);
            t.x = p.x + Util.tand(RA/3 -ORA);
            t.y = p.y + Util.tand(60.0/3600.0 -ODE);
            pcat.o[j++] = t;
         }

      }
   }

   /** Modifie le target, le centre de roatation et la rotation. Si la rotation ne peut être modifiée,
    * la précédente valeur sera conservée. Idem pour le centre
    * @param raProjCenter nouvelle ascension droite du centre de la projection
    * @param deProjCenter nouvelle déclinaison du centre de la projection
    * @param raRotCenter nouvelle ascension droite du centre de rotation
    * @param deRotCenter nouvelle déclinaison du centre de rotation
    * @param roll nouvelle rotation
    */
   protected void setParameters(double raProjCenter, double deProjCenter,
         double raRotCenter, double deRotCenter,
         double roll) {
      Position p = getProjCenterObjet();
      if( !flagMove || Double.isNaN(raProjCenter) || Double.isNaN(deProjCenter) ) {
         raProjCenter = p.raj;
         deProjCenter = p.dej;
      }
      if( !flagRoll || Double.isNaN(raRotCenter) || Double.isNaN(deRotCenter)) {
         roll = this.roll;
         raRotCenter=raProjCenter;
         deRotCenter=deProjCenter;
      } else {
         roll = angle(roll);
      }

      setTarget(raProjCenter,deProjCenter,raRotCenter,deRotCenter,roll);
   }

   synchronized private void setTarget(double raP,double deP,double raR, double deR,double roll) {
      double x,y;
      Proj3 a;
      double cosr=1.,sinr=0.;
      double offsetX,offsetY;

      // Pour pouvoir empiler la stack correctement
      if( co==null ) co=new Coord();
      co.al=raP; co.del=deP;
      objet=co.getSexa();

      // Quelle était la position du centre de projection lorsque this.roll==0
      //       if( this.roll!=0 ) {
      //          a = new Proj3(Proj3.TAN,raR,deR);
      //          cosr=AstroMath.cosd(-this.roll);
      //          sinr=AstroMath.sind(-this.roll);
      //          a.set( new Coo(raP,deP));
      ////          a.computeXY(raP, deP);
      //          offsetX = a.getX();
      //          offsetY = a.getY();
      //          x =  offsetX*cosr + offsetY*sinr;
      //          y = -offsetX*sinr + offsetY*cosr;
      //          a.set(x,y);
      ////          a.computeAngles(x,y);
      //          raP = a.getLon();
      //          deP = a.getLat();
      //       }

      a = new Proj3(Proj3.TAN,raP,deP);

      Position rot = getRotCenterObjet();
      a.set( new Coo(raR,deR));
      //      a.computeXY(raR, deR);
      offsetX = a.getX();
      offsetY = a.getY();
      rot.x = offsetX;
      rot.y = offsetY;
      rot.raj=raR;
      rot.dej=deR;

      this.roll = roll;
      if( roll!=0. ) { cosr=AstroMath.cosd(roll); sinr=AstroMath.sind(roll); }

      for( int i=1; i<pcat.nb_o; i++ ) {
         Position p = (Position)pcat.o[i];

         if( p instanceof Forme ) {
            Forme f = (Forme)p;
            for( int j=0; j<f.o.length; j++ ) {
               p = f.o[j];
               x = p.x;
               y = p.y;
               if( roll!=0. ) {
                  x-=offsetX;
                  y-=offsetY;
                  double xr = x*cosr+y*sinr;
                  double yr = -x*sinr+y*cosr;
                  x=xr + offsetX;
                  y=yr + offsetY;
               }
               //                a.computeAngles(x,y);
               a.set(x,y);
               p.raj = a.getLon();
               p.dej = a.getLat();

            }
         } else {
            x = p.x;
            y = p.y;
            if( roll!=0. ) {
               x-=offsetX;
               y-=offsetY;
               double xr = x*cosr+y*sinr;
               double yr = -x*sinr+y*cosr;
               x=xr + offsetX;
               y=yr + offsetY;
            }
            //             a.computeAngles(x,y);
            a.set(x,y);
            p.raj = a.getLon();
            p.dej = a.getLat();
            
//            System.out.println("x,y="+x+","+y+" -> raj,dej="+p.raj+","+p.dej);
         }
      }

      // Mise à jour des propriétés si nécessaires
      Properties.majProp(this);

   }
   
//   static public void main(String [] argv) {
//      try {
//         double x = -9.739519033341476E-5;
//         double y = -0.011581459001125264;
//         Proj3 a = new Proj3(Proj3.TAN,0.,0.);
//         a.set( x,y );
//         double raj = a.getLon();
//         double dej = a.getLat();
//         System.out.println("x,y="+x+","+y+" -> raj,dej="+raj+","+dej);
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }

   /** Reset the FoV for recomputing all drawing objects (after a translation
    * or a rotation to avoid distortions and repaint it
    * @param flag ViewSimple.MOVE|ViewSimple.ROLL|ViewSimple.MOVECENTER
    */
   protected void reset(int flag) {
      Position c = getProjCenterObjet();
      Position cr = getRotCenterObjet();
      if( c!=null ) {
         setTarget(c.raj,c.dej,cr.raj,cr.dej,roll);
         if( (flag&ViewSimple.MOVE)!=0 )       sendTargetObserver(false);
         if( (flag&ViewSimple.ROLL)!=0 )       sendRollObserver(false);
         if( (flag&ViewSimple.MOVECENTER)!=0 ) sendRotCenterObserver(false);
      }
      aladin.view.newView();
   }

   /** Changement de l'angle de rotation autour du centre de rotation
    * => altère le centre de projection */
   protected void changeRoll(double roll) {
      Position c = getProjCenterObjet();
      Position cr = getRotCenterObjet();
      setTarget(c.raj,c.dej,cr.raj,cr.dej,roll);
   }

   /** Déplacement du Target ce qui entraine le déplacement de tout
    * le FoV, centre de rotation compris */
   protected void changeTarget(double raj,double dej) {
      Position c = getProjCenterObjet();
      Position cr = getRotCenterObjet();
      if( Double.isNaN(raj) || Double.isNaN(dej) ) { raj=c.raj; dej=c.dej; }
      double deltaRa = raj-c.raj;
      double deltaDE = dej-c.dej;
      setTarget(raj,dej,cr.raj+deltaRa,cr.dej+deltaDE,roll);
   }

   /** Déplacement du centre de rotation uniquement */
   protected void changeRotCenter(double raj,double dej) {
      Position c = getProjCenterObjet();
      if( Double.isNaN(raj) || Double.isNaN(dej) ) { raj=c.raj; dej=c.dej; }
      setTarget(c.raj,c.dej,raj,dej,roll);
   }

   protected VOApp observer=null;// VOApp/ExtApp observer, null if there is no one

   /** Inscription d'un observer pour le Plan (compatible VOApp) */
   protected void addObserver(VOApp observer) { this.observer=observer; }

   /** Transmission des infos de position à un éventuel observer
    * @param cont true si on doit préfixer de "..." pour indiquer un évènement transitoire
    */
   protected void sendTargetObserver(boolean cont) {
      //      System.out.println("set "+Tok.quote(label)+" Target="+getTarget()+(cont?" ...":""));
      if( observer==null ) return;
      observer.execCommand("set "+Tok.quote(label)+" Target="+getTarget()+(cont?" ...":""));
   }

   /** Transmission des infos de position du centre de rotation à un éventuel observe
    * @param cont true si on doit préfixer de "..." pour indiquer un évènement transitoire
    */
   protected void sendRotCenterObserver(boolean cont) {
      //      System.out.println("set "+Tok.quote(label)+" RotCenter="+getRotCenter()+(cont?" ...":""));
      if( observer==null ) return;
      observer.execCommand("set "+Tok.quote(label)+" RotCenter="+getRotCenter()+(cont?" ...":""));
   }

   /** Transmission des infos de position et d'angle à un éventuel observer
    * @param cont true si on doit préfixer de "..." pour indiquer un évènement transitoire
    */
   protected void sendRollObserver(boolean cont) {
      //      System.out.println("set "+Tok.quote(label)+" Roll="+roll+(cont?" ...":""));
      if( observer==null ) return;
      observer.execCommand("set "+Tok.quote(label)+" Roll="+roll+(cont?" ...":""));
   }

   /** Transmission des infos de changement d'état d'un subFoV à un éventuel observer */
   protected void sendSubFovObserver(String subFoVID,boolean visible) {
      if( observer==null ) return;
      observer.execCommand("set "+Tok.quote(label+"/"+subFoVID)+" Status="+( visible ? "shown":"hidden"));
   }

   /**
    * Create FoV objects on the basis of infos in the FootprintBean
    *
    * @param bean bean holding infos to build the corresponding FoV (PlanField)
    */
   private void setObjects(FootprintBean bean) {
      SubFootprintBean[] fovParts = bean.getBeans();
      int nbO = 0;

      int nbSubFov = fovParts.length;

      Obj[][] o = new Obj[nbSubFov][];
      for( int i=0; i<nbSubFov; i++ ) {
         o[i] = fovParts[i].buildObjets(this);
         nbO += o[i].length;
      }

      pcat.o = new Obj[pcat.nb_o = nbO+2];
      //		System.out.println("nbO : "+nbO);

      // Field of View center
      Repere projCenter = new Repere(this);
      projCenter.setType(Repere.CENTER);  // in x=0, y=0 position by default
      pcat.o[1] = projCenter;

      // Field of View center
      Repere rotCenter = new Repere(this);
      rotCenter.setRotCenterType(projCenter);  // in x=0, y=0 position by default
      pcat.o[0] = rotCenter;

      int idxStart = 2;
      for( int i=0; i<nbSubFov; i++ ) {
         System.arraycopy(o[i], 0, pcat.o, idxStart, o[i].length);
         addSubFoV(fovParts[i].getName(),fovParts[i].getDesc(),
               idxStart,idxStart+o[i].length-1,true,fovParts[i].getColor());
         idxStart += o[i].length;
      }
   }

   /**
    * Create a FoV based on polygons
    *
    * @param xOffset
    *            arrays of x offsets of polygons
    * @param yOffset
    *            arrays of y offsets of polygons
   private void setPolygons(double[][] xOffset, double[][] yOffset, int[] boundary, String[] names) {


   	  int sizePcat = 0;
   	  for( int i=0; i<xOffset.length; i++ ) {
   	  	 sizePcat += xOffset[i].length;
   	  }
      pcat.o = new Objet[pcat.nb_o = sizePcat+1];
//      System.out.println(pcat.nb_o);

      // Field of View center
      Repere center = new Repere(this);
      center.setType(Repere.CENTER);  // in x=0, y=0 position by default
      pcat.o[0] = center;

      int k=1;
      int nbPts;
      Ligne l,lMem;
      lMem = null;
      int currentBoundIdx = 0;

      // Creation of polygons
      for( int i=0; i<xOffset.length; i++ ) {
      	 nbPts = xOffset[i].length;
      	 for( int j=0; j<nbPts; j++ ) {
//      	 	System.out.println("i : "+i);
//      	 	System.out.println("k : "+k);
//      	 	System.out.println("j : "+j);
      	 	l = new Ligne(this);
      	 	pcat.o[k] = l;

      	 	l.setXYTan(tand(xOffset[i][j]), tand(yOffset[i][j]));
//      	 	System.out.println("setXYTan : "+xOffset[i][j]+" "+yOffset[i][j]);

            if( j>0 ) l.debligne=(Ligne)pcat.o[k-1];
            if( j<nbPts-1 ) l.finligne=(Ligne)pcat.o[k+1];
            if( j==0 ) lMem = l;
            if( j==nbPts-1 ) lMem.debligne = l;

            k++;
      	 }
      }

      // Creation of sub-FOV
      addSubFoV(names[0],names[0],1,4,true,null);
   }
    */

   /** Return instrument code, or -1 if not found */
   private void setInstr(String instrument) {
      for( int i=0; i<INSTR.length; i++) {
         if( INSTR[i].equalsIgnoreCase(instrument) ) { instr=i; return; }
      }
      instr=-1;
   }

   /** Override Server.memTarget() to avoid it */
   protected void memTarget() { }

   /** Demande d'activation/désactivation d'un plan
    * On mémorise le dernier état demandé au cas où ce n'est pas possible
    * immédiatement. Si aucun paramètre, on tente d'activer le dernier
    * état demandé.
    * @param flag true pour activer le plan, false sinon
    * @return true si le plan a pu être activé, false sinon
    */
   protected boolean setActivated(boolean flag) {
      askActive = flag;
      boolean rep=setActivated();
      if( rep==flag && observer!=null ) {
         observer.execCommand("set "+Tok.quote(label)+" Status="+( flag ? "shown":"hidden"));
      }
      return rep;
   }

   /** Modifie (si possible) une propriété du plan (dépend du type de plan) */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      if( prop.equalsIgnoreCase("Target") ) {
         if( !isMovable() ) throw new Exception("Not movable aperture");
         Coord co = new Coord(value);
         changeTarget(co.al,co.del);

      } else if( prop.equalsIgnoreCase("Roll") ) {
         if( !isRollable() ) throw new Exception("Not rollable aperture");
         double roll = Double.valueOf(value).doubleValue();
         changeRoll(roll);

      } else if( prop.equalsIgnoreCase("RotCenter") ) {
         if( !isRollable() ) throw new Exception("Not rollable aperture");
         Coord co;
         if( value.trim().length()==0 ) {
            Position c = getProjCenterObjet();
            changeRotCenter(c.raj,c.dej);
         } else {
            co = new Coord(value);
            changeRotCenter(co.al,co.del);
         }

      } else if( prop.equalsIgnoreCase("Rollable") ) {
         if( value.equalsIgnoreCase("True") ) setRollable(true);
         else if( value.equalsIgnoreCase("False") ) setRollable(false);
         else throw new Exception("Rollable propertie should be \"true\" or \"false\" !");

      } else if( prop.equalsIgnoreCase("Movable") ) {
         if( value.equalsIgnoreCase("True") ) setMovable(true);
         else if( value.equalsIgnoreCase("False") ) setMovable(false);
         else throw new Exception("Movable propertie should be \"true\" or \"false\" !");

      } else if( prop.equalsIgnoreCase("Status") && specif!=null ) {
         int n [] = findSubFoVs(specif);
         if( n.length==0 ) throw new Exception("Unknown "+label+" FoV ["+specif+"]");
         boolean flag;
         if( value.indexOf("shown")>=0 ) flag=true;
         else if( value.indexOf("hidden")>=0 ) flag=false;
         else throw new Exception("Unknown FoV status ["+value+"]");
         for( int i=0; i<n.length; i++ ) setVisibleSubFoV(n[i],flag);

      } else super.setPropertie(prop,null,value);

      if( observer!=null && !prop.equals("Status") ) {
         observer.execCommand("set " +Tok.quote(label)+" "+prop+"="+value);
      }

      Properties.majProp(this);
      aladin.view.resetClip();
      aladin.view.newView(1);
   };

   /** Return plan information */
   protected String getInfo() {
      return label+super.addDebugInfo();
   }

   /** Return the central coordinate */
   protected Coord getCenter() {
      Position pos = getProjCenterObjet();
      return new Coord(pos.raj,pos.dej);
   }

   /** Return the FoV projection center as a Position java object, or null  */
   protected Position getProjCenterObjet() {
      try { return (Position)pcat.o[1]; }
      catch( Exception e) { return null; }
   }

   /** Return the FoV projection center as a Position java object, or null  */
   protected Position getRotCenterObjet() {
      try { return (Position)pcat.o[ 0 ]; }
      catch( Exception e) { return null; }
   }

   /** Repositionne le centre de rotation du FOV sur le centre de projection */
   protected void resetRotCenterObjet() {
      try { pcat.o[0].raj = pcat.o[1].raj; pcat.o[0].dej = pcat.o[1].dej; }
      catch( Exception e) { }

   }

   /** Return FoV projection center in the current Aladin coordinate frame */
   protected String getProjCenter() {
      Position c = getProjCenterObjet();
      if( c==null ) return "";
      return aladin.localisation.J2000ToString(c.raj,c.dej,Astrocoo.MAS+3,false);
   }

   /** Return FoV rotation center in the current Aladin coordinate frame */
   protected String getRotCenter() {
      Position c = getRotCenterObjet();
      if( c==null ) return "";
      return aladin.localisation.J2000ToString(c.raj,c.dej,Astrocoo.MAS+3,false);
   }

//   private Astrocoo afs = new Astrocoo(new ICRS());    // Frame ICRS (la reference de base)
   private Astrocoo afs = new Astrocoo( Astroframe.create("ICRS") );    // Frame ICRS (la reference de base)

   /** Return the FoV center in J2000 sexagesimal coordinates */
   protected String getTarget() {
      Position c = getProjCenterObjet();
      if( c==null ) return null;
      afs.setPrecision(Astrocoo.MAS+1);
      afs.set(c.raj,c.dej);
      return afs.toString("2:");
   }

   /** Return FoV rotation angle */
   protected String getRoll() {
      //      if( !flagRoll ) return "0.0";
      return ((int)Math.round(roll*10))/10.+"";
   }

   /** Retourne true si l'angle de rotation va être modifiée */
   protected boolean isNewRoll(double x) {
      return roll!=angle(x);
   }

   /** Angle entre 0 et 360° */
   private double angle(double x) {
      x = x%360.;
      if( x<0 ) x+=360.;
      return x;
   }

   /** Modify rotation angle by incrementation */
   protected void deltaRoll(double x) {
      setRoll(this.roll+x);
   }

   /** Modify rotation angle by incrementation */
   private void setRoll(double x) {
      if( !flagRoll ) return;
      this.roll = angle(x);
   }

   /** Return true if the FoV is rollable */
   protected boolean isRollable() { return flagRoll; }

   /** Positionne l'attibut Rollable pour l'Aperture */
   protected void setRollable(boolean b) { this.flagRoll = b; }

   /** Return true if the FoV is rollable and its roll center is movable */
   protected boolean isCenterRollable() { return flagCenterRoll; }

   /** Positionne l'attribut Roll center Movable */
   protected void setCenterRollable(boolean b) { flagCenterRoll = b; }

   /** Positionne l'attibut Movable pour l'Aperture */
   protected void setMovable(boolean b) { this.flagMove = b; }

   /** Return true if the FoV is movable */
   protected boolean isMovable() { return flagMove; }

   protected Vector subFoV=null;		// Sub FoV list
   protected JCheckBox cbSubFoV[];		// Checkbox concerning each sub FoV (see getPanelSubFoV()

   /**
    * Show/hide the sub FoV corresponding to the Checkbox (see getPanelSubFoV())
    * @param cb the checkbox which was pressed in the Properties frame
    * @return true if this PlanField is concerned by this Checkbox, false otherwise
    */
   protected boolean switchCheckbox(JCheckBox cb) {
      if( cbSubFoV==null ) return false;
      for( int i=0; i<cbSubFoV.length; i++) {
         if( cb==cbSubFoV[i]) {
            setVisibleSubFoV(i,cb.isSelected());
            return true;
         }
      }
      return false;
   }

   /**
    * Build the JPanel allowing the user to select individual sub FoV
    */
   protected JPanel getPanelSubFov(ActionListener al) {
      if( subFoV==null || subFoV.size()==0 || isAlmaFP() ) return null;
      JPanel p = new JPanel();
      p.setLayout( new GridLayout(0,1));

      int nb = subFoV.size();
      cbSubFoV = new JCheckBox[nb];

      for( int i=0; i<nb; i++ ) {
         PlanFieldSub sfov = getSubFoV(i);
         cbSubFoV[i] = new JCheckBox((i+1)+".- "+sfov.ID+" "+sfov.desc,sfov.visible);
         cbSubFoV[i].addActionListener(al);
         p.add(cbSubFoV[i]);
      }

      return p;
   }

   /**
    * Export pointing centers, as a new catalogue plane (dedicated to ALMA footprints)
    */
   protected void exportAlmaPointings() {
      List<Forme> pointings = new ArrayList<>();
      for (Obj obj: pcat.o) {
         if (obj instanceof Cercle && ! (obj instanceof Pickle)) {
            pointings.add((Forme)obj);
         }
      }

      String vot = Util.createVOTable(pointings);

      aladin.calque.newPlanCatalog(new MyInputStream(new BufferedInputStream(
            new ByteArrayInputStream(vot.getBytes()))), "Pointings");
   }

   protected boolean isAlmaFP() {
      return almaFP;
   }

   protected void setIsAlmaFP(boolean b) {
      this.almaFP = b;
   }

   /**
    * Return the status of each individual sub FoV, or null
    */
   protected String getStatusSubFov() {
      if( subFoV==null || subFoV.size()==0 ) return null;

      StringBuffer res = new StringBuffer(100);
      int nb = subFoV.size();

      for( int i=0; i<nb; i++ ) {
         PlanFieldSub sfov = getSubFoV(i);
         res.append("FoV      "+Tok.quote(sfov.ID)+" Status="+(sfov.visible?"shown":"hidden")+"\n");
      }

      return res.toString();
   }

   /**
    * Addition of a new Sub FoV specification
    * @param ID subFoV identification  eg: WFPC2
    * @param desc Description (one line)  eg: Wide Field Peak Camera 2
    * @param first First concerned object index in PlanField.pcat.o[]
    * @param last Last concerned object index in PlanField.pcat.o[]
    * @param visible true if the subFoV is visible, hidden otherwise
    * @param color Color or null for plan default color
    */
   protected void addSubFoV(String ID, String desc, int first, int last, boolean visible,Color color) {
      PlanFieldSub sfov = new PlanFieldSub(ID,desc,first,last,visible,color);
      if( subFoV==null ) subFoV = new Vector(10);
      subFoV.addElement(sfov);
   }

   /**
    * Recherche des indices des subFoVs correspondants a un masque avec jokers
    * @param masq
    * @return le tableau des indices
    */
   protected int [] findSubFoVs(String masq) {
      int n=subFoV.size();
      int res [] = new int[n];
      int j=0;

      for( int i=0; i<n; i++) {
         if( Util.matchMask(masq,((PlanFieldSub)subFoV.elementAt(i)).ID) ) res[j++]=i;
      }

      int r [] = new int[j];
      System.arraycopy(res,0,r,0,j);
      return r;
   }

   /**
    * Find the sub FoV index in subFoV Vector
    * @param ID Identifaction
    * @return index in subFoV Vector or -1 if not found
    */
   protected int findSubFoV(String ID) {
      if( subFoV==null ) return -1;
      for( int i=subFoV.size()-1; i>=0; i-- ) if( ((PlanFieldSub)subFoV.elementAt(i)).ID.equals(ID) ) return i;
      return -1;
   }

   /** Find the sub FoV index in subFoV Vector by object index
    * @param index Index of object
    * @return index in subFoV Vector or -1 if not found
    */
   protected int findSubFoV(int index) {
      if( subFoV==null ) return -1;
      for( int i=subFoV.size()-1; i>=0; i-- ) {
         PlanFieldSub sFov = (PlanFieldSub)subFoV.elementAt(i);
         if( sFov.first<=index && index<=sFov.last ) return i;
      }
      return -1;
   }

   /**
    * Return a sub FoV, or null
    * @param n index of sub FoV in subFoV Vector
    */
   protected PlanFieldSub getSubFoV(int n) {
      //      if( subFoV==null || n<0 || n>=subFoV.size() ) return null;
      return (PlanFieldSub)subFoV.elementAt(n);
   }

   /**
    * Show or hide a sub FoV
    * @param n index of the sub FoV in the subFoV vector
    * @param visible true to show
    */
   protected void setVisibleSubFoV(int n,boolean visible) {
      PlanFieldSub sfov = getSubFoV(n);
      if( sfov==null || sfov.visible==visible ) return;
      sfov.visible=visible;
      for( int i=sfov.first; i<=sfov.last; i++ ) pcat.o[i].setVisibleGenerique(visible);
      aladin.view.repaintAll();
      sendSubFovObserver(sfov.ID,visible);
   }

   /** Return the drawing color of an object depending of sub FoV container */
   protected Color getColor(Obj o) {
      int i = pcat.getIndex(o);
      if( i<0 ) return null;
      int j = findSubFoV(i);
      if( j<0) return null;
      Color x = getSubFoV(j).color;

      return x;
   }

   protected String getInstrumentName() {
      if( fpBean!=null ) return fpBean.getInstrumentName();
      return "";
   }

   protected String getInstrumentDesc() {
      if( fpBean!=null ) return fpBean.getInstrumentDesc();
      return "";
   }

   protected String getTelescopeName() {
      if( fpBean!=null ) return fpBean.getTelescopeName();
      return "";
   }

   protected String getOrigine() {
      if( fpBean!=null ) return fpBean.getOrigin();
      return "";
   }


   /**
    * Sub Field of View structure
    */
   private class PlanFieldSub {
      protected String ID;	 // subFoV identification  eg: WFPC2 (les / sont remplacés par des '.')
      protected String desc; // Description (one line)  eg: Wide Field Peak Camera 2
      protected int first;   // First concerned object index in PlanField.pcat.o[]
      protected int last;    // Last concerned object index in PlanField.pcat.o[]
      protected boolean visible; // true if the subFoV is visible, hidden otherwise
      protected Color color;  // Coloror null for plan default color

      private PlanFieldSub(String ID, String desc, int first, int last, boolean visible,Color color) {
         this.ID=ID.replace('/','.');
         this.desc=desc;
         this.first=first;
         this.last=last;
         this.visible=visible;
         this.color=color;
      }
   }
}

