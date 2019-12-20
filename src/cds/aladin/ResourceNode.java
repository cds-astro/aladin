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

/** <p>Title : ResourceNode</p>
 *  <p>Description : Node of a MetaDataTree</p>
 *  <p>Copyright: 2002</p>
 *  <p>Company: CDS </p>
 *  @author Thomas Boch
 *  @version 0.5 : 27 Novembre 2002 (Creation)
 */


package cds.aladin;

import java.awt.Color;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Map;

import cds.tools.Util;

public class ResourceNode extends BasicNode implements Cloneable {
    // VOID : rien
    // IMAGE : ressource image (noeud ou feuille)
    // CAT : ressource catalogue (noeud ou feuille)
    // SPECTRUM : ressource spectre
	// CUBE : un cube
	// INFO : un noeud informatif
	// CHARAC : characterization
	// OTHER : autre chose ...
    static final int VOID = 0;
    static final int IMAGE = 1;
    static final int CAT = 2;
    static final int SPECTRUM = 3;
    static final int CUBE = 4;
    static final int INFO = 5;
    static final int CHARAC = 6;
    static final int OTHER = 7;

	// exemple : "spectrum/fits"
	String format;

    // IMAGE, CAT ou VOID, VOID par defaut
    int type = VOID;
    // description du catalogue
    //String catDesc;

	// formats disponibles (JPEG, FITS, ...)
	String[] formats;
	String curFormat;

    boolean[] hidden;

    boolean hasData=false;

    // distance in degrees to the requested position in the initial query
    private double distanceToCenter = Double.NaN;

    // pour SSA version ESAC !
    String[] axes;
    String[] units;
    String[] dimeq;
    String[] scaleq;

    // modes de processing disponibles (ORIGIN, MOSAIC, ...)
    String[] modes;
    String curMode;

    // angular pixel size
    private String pixSize;

    // angular pixel size in decimal degrees
    private double pixSizeDeg;

    // observation date
    String obsDate;
    // origine de la ressource
    String origin;
    // longueur d'onde
    String wavelength;
    // court texte expliquant la longueur d'onde
    String wavelengthExpla;

    // mode d'indexation (HTML, URL, TEMPLATE ...)
    String indexing;

    // pour tri SIAP extensions
    String[] siapSortFields;

    // nom de l'objet entré par l'utilisateur
    String objet;
    String targetObjet;

    // reference number
    String refNumber;

    // si != null, couleur des fov des fils
    Color col;

    // si differents processing modes, il s'agir du fov de l'image "originelle" (non mosaiquée)
    private Fov fov;

	int ra = -1;
	int de = -1;

	boolean cutout=false;

	// target du centre du cutout
	// ATTENTION : sert également pour les catalogues !!
	private String cutoutTarget;
    // target pour une mosaic
    private String mosaicTarget;

    // dans le cas de slices par exemple, on aura plusieurs images possibles (tranches)
    // ce sera donc à l'utilisateur de choisir celle qu'il désire
    String curImgNumber = "1";
    String maxImgNumber = "1";

    double beginVel = 0.; // begin velocity for first channel of a cube
    double velStep = 0.; // step between 2 slices

	// serveur d'où proviennent les ressources
	Server server;

	// location to access the resource
	String location;
    // GLU access to resource
    String gluLink;

	String resol; // résolution de l'image
	String survey;
    String bandPass; // 'couleur' de l'image
    String epoch; // si epoch!=null, on est dans la config. nouveau serveur
    String machine; // champ spécifique pour bidouille section SIAP dans XML
    String plateNumber; // numéro de plaque pour images DSS
    String[] description; // e.g: survey, color, position, ...
    String[] explanation; // e.g: GOODS, ISAAC-J, 53 -27, ...

    // TODO : il faudrait enlever explanation, et construire la chaine correspondante à la volée
    String[] originalExpla; // valeurs originales, sans l'unité attachée !

    String[] ucds; // UCDs correspondant aux champs de description
    String[] utypes; // utypes corresponsant aux champs de description
    String[] allUnits;

    String[] filterDesc;
    String[] filterExpla;

    String criteria;
    String valueCriteria;

    // description d'une sous-observation
    String desc;

    // "SLICES", ou "SPECTRUM", ou "RETRIEVAL", ou "STANDALONE" ...
    String dataOrga;

    String xPos, yPos;

    // critères de tri applicables sur les sous-noeuds
    String[] sortCriteria;
    // map des valeurs des critères
    Hashtable criteriaVal;

    // utilisé pour SIAP evolution, fait simplement le lien description[i]-->explanation[i]
    Hashtable properties;
    // utilisé pour SIAP evolution --> fait le lien description[i] --> noeud décrivant ce parametre
    Hashtable links;
    // pour SIAP evolution
    boolean isSIAPEvol = false;

    Hashtable propertiesUnits;

    ResourceNode(Aladin aladin) {
		super(aladin);
    }

	// constructeur par recopie
	// /!\ ATTENTION : ce constructeur ne recopiera pas les enfants
	ResourceNode(Aladin aladin, ResourceNode n) {
	    super(aladin, n);

	    this.properties = n.properties;
	    this.links = n.links;
	    this.propertiesUnits = n.propertiesUnits;

	    this.type = n.type;
	    this.ra = n.ra;
	    this.de = n.de;
	    //this.catDesc = n.catDesc;
	    this.col = null;
	    this.fov = n.fov;
	    this.cutout = n.cutout;
	    this.survey = n.survey;
	    this.bandPass = n.bandPass;
	    this.epoch = n.epoch;
	    this.resol = n.resol;
	    this.machine = n.machine;
	    this.plateNumber = n.plateNumber;
	    this.description = n.description;
	    //this.explanation = n.explanation;
        if( n.explanation!=null ) {
            this.explanation = new String[n.explanation.length];
            for( int i=0; i<n.explanation.length; i++ )
                this.explanation[i] = n.explanation[i];
        }
	    this.filterDesc = n.filterDesc;
	    this.filterExpla = n.filterExpla;
	    this.sortCriteria = n.sortCriteria;
	    this.criteriaVal = n.criteriaVal;
	    this.criteria = n.criteria;
	    this.valueCriteria = n.valueCriteria;
	    this.location = n.location;
        this.gluLink = n.gluLink;
	    this.server = n.server;
	    this.cutoutTarget = n.cutoutTarget;
	    this.formats = n.formats;
	    this.curFormat = n.curFormat;
        this.pixSize = n.pixSize;
        this.pixSizeDeg = n.pixSizeDeg;
        this.obsDate = n.obsDate;
        this.modes = n.modes;
        this.curMode = n.curMode;
        this.objet = n.objet;
        this.targetObjet = n.targetObjet;
        this.origin = n.origin;
        this.wavelengthExpla = n.wavelengthExpla;
        this.indexing = n.indexing;
        this.curImgNumber = n.curImgNumber;
        this.maxImgNumber = n.maxImgNumber;
        this.desc = n.desc;
        this.refNumber = n.refNumber;

        this.axes = n.axes;
        this.units = n.units;
        this.dimeq = n.dimeq;
        this.scaleq = n.scaleq;

        this.format = n.format;

        this.siapSortFields = n.siapSortFields;

        this.originalExpla = n.originalExpla;

        this.ucds = n.ucds;
        this.utypes = n.utypes;
        this.allUnits = n.allUnits;

        this.hidden = n.hidden;

        this.dataOrga = n.dataOrga;

        this.xPos = n.xPos;
        this.yPos = n.yPos;

        this.hasData = n.hasData;

        this.distanceToCenter = n.distanceToCenter;
	}



    ResourceNode(Aladin aladin, String name) {
		super(aladin, name);
    }

    ResourceNode(Aladin aladin, String name, boolean isOpen, boolean isLeaf) {
		super(aladin, name, isOpen, isLeaf);
    }
    // clone the object
    public Object clone() {
        ResourceNode o = null;
        try {
            o = (ResourceNode)super.clone();
        }
        catch(Exception e) {
            System.err.println("Can't clone !!");
            e.printStackTrace();
        }

        // clone des children
        o.children = new java.util.Vector();
        o.nbChildren = 0;
        for( int i=0; i<children.size(); i++ ) {
           	o.addChild((ResourceNode)((ResourceNode)children.elementAt(i)).clone());
        }

        return o;
    }

	// implémentation de la méthode abstraite de BasicNode
	public BasicNode createNode(String name) {
		return new ResourceNode(aladin, name);
	}

	public void setCutoutTarget(String t) {
	    setCutoutTarget(t, true);
	}

	/** Fixe le target pour le cutout
	 *
	 * @param t le nouveau target
	 * @param updateInfo si true, on update la FrameInfo courante si elle correspond au noeud
	 */
	public void setCutoutTarget(String t, boolean updateInfo) {
	    if( t!=null && t.length()>0 ) {
	    	cutoutTarget = t;
	    	// on teste si la FrameInfo courante affiche les infos de this
	    	if( updateInfo && aladin.getFrameInfo().getNode()!=null && aladin.getFrameInfo().getNode().equals(this) ) {
	    		aladin.getFrameInfo().setTargetTF(t);
	    	}
	    }
	}

	public String getCutoutTarget() {
	    return cutoutTarget;
	}


    public void setMosaicTarget(String t) {
        setMosaicTarget(t, true);
    }

    /** Fixe le target pour une mosaic
     *
     * @param t le nouveau target
     * @param updateInfo si true, on update la FrameInfo courante si elle correspond au noeud
     */
    public void setMosaicTarget(String t, boolean updateInfo) {
        if( t!=null && t.length()>0 ) {
            mosaicTarget = t;
            // on teste si la FrameInfo courante affiche les infos de this
            if( updateInfo && aladin.getFrameInfo().getNode()!=null && aladin.getFrameInfo().getNode().equals(this) ) {
            	aladin.getFrameInfo().setMosaicTargetTF(t);
            }
        }
    }

    public String getMosaicTarget() {
        return mosaicTarget;
    }

    public void setImagePosTarget(String x, String y) {
        setImagePosTarget(x, y, true);
    }

    /** Fixe la position x,y pour une image
     *
     * @param x
     * @param y
     * @param updateInfo si true, on update la FrameInfo courante si elle correspond au noeud
     */
    public void setImagePosTarget(String x, String y, boolean updateInfo) {
        if( x!=null && x.length()>0 ) {
        	xPos = x;
        	yPos = y;
            // on teste si la FrameInfo courante affiche les infos de this
            if( updateInfo && aladin.getFrameInfo().getNode()!=null && aladin.getFrameInfo().getNode().equals(this) ) {
            	aladin.getFrameInfo().setImagePosTargeTFt(x,y);
            }
        }
    }

    public String[] getImagePosTarget() {
        return new String[] {xPos==null?"":xPos, yPos==null?"":yPos};
    }

    /** Fixe le format */
    /*
    public void setFormat(String f) {
        if( formats==null || f.equals(curFormat) ) return;
        boolean found = false;
        for( int i=0; i<formats.length && !found ; i++ ) {
            if( f.equals(formats[i]) ) found = true;
        }
        if( found ) curFormat = f;
    }

    public String getFormat() {
        return curFormat;
    }
    */

	protected boolean isAvailableFormat(String f) {
        if( formats==null || f==null ) return false;
        for( int i=0; i<formats.length; i++ )
            if( f.equalsIgnoreCase(formats[i]) ) return true;

        return false;
	}


    /**
     * @return Returns the fov.
     */
    protected Fov getFov() {
	    // traitement spécial pour un noeud IMAGE si processingMode == MOSAIC
	    if( this.modes!=null && this.modes.length>0 && this.curMode.equals("MOSAIC") ) {
	        Fov fov;
	        try {
	            fov = new Fov(MetaDataTree.resolveTarget(this.getMosaicTarget(), aladin), this.fov.x, this.fov.y, this.fov.angle);
	        }
	        catch(Exception e) {return null;}
	        return fov;
	    }
        return fov;
    }
    /**
     * @param fov The fov to set.
     */
    protected void setFov(Fov fov) {
        this.fov = fov;
    }

    /** retourne la valeur d'un champ d'après son UCD
     *
     * @param ucd l'UCD pour lequel on veut la valeur du champ correspondant
     * @param useOriginalExpla si on renvoie l'explication non 'retouchée'
     *
     * @return la valeur, null si UCD non trouvé
     */
    public String getFieldValFromUcd(String ucd, boolean useOriginalExpla) {
    	int idx = Util.indexInArrayOf(ucd, ucds);
    	if( idx<0 ) return null;

    	return useOriginalExpla?originalExpla[idx]:explanation[idx];
    }

    public String getFieldValFromUcd(String ucd) {
        return getFieldValFromUcd(ucd, false);
    }


    /** retourne la valeur d'un champ d'après son utype
     *
     * @param utype l'utype pour lequel on veut la valeur du champ correspondant
     * @return la valeur, null si utype non trouvé
     */
    public String getFieldValFromUtype(String utype) {
    	int idx = Util.indexInArrayOf(utype, utypes);
    	if( idx<0 ) return null;

    	return explanation[idx];
    }

    public String getFieldValFromName(String name) {
    	int idx = Util.indexInArrayOf(name, description);
    	if( idx<0 ) return null;

    	return explanation[idx];
    }

    /** retourne une calibration associée à l'image décrite par ce noeud
     *
     * @return calibration associée au noeud image, null si construction de la calib impossible
     */
    public Calib getCalib() {
    	Aladin.trace(3, "Trying to build a calibration for a color image");

    	// récupération des valeurs de CRPIX
    	Aladin.trace(3, "Looking for CRPIX");
    	// vieil UCD
    	String crpixStr = getFieldValFromUcd("VOX:WCS_CoordRefPixel", true);
    	// UCD 1+
    	if( crpixStr==null || crpixStr.length()==0 ) crpixStr = getFieldValFromUcd("pos.wcs.crpix", true);

    	if( crpixStr==null || crpixStr.length()==0 ) return null;
    	String[] crpix = TreeBuilder.split(crpixStr, " ,");
    	if( crpix.length<2 ) return null;
    	double crpix1, crpix2;
    	try {
    		crpix1 = Double.valueOf(crpix[0]).doubleValue();
    		crpix2 = Double.valueOf(crpix[1]).doubleValue();
    	}
    	catch( NumberFormatException e ) {return null;}

    	// récupération des valeurs de CRVAL
    	Aladin.trace(3, "Looking for CRVAL");
    	// vieil UCD
    	String crvalStr = getFieldValFromUcd("VOX:WCS_CoordRefValue", true);
    	// UCD 1+
    	if( crvalStr==null || crvalStr.length()==0 ) crvalStr = getFieldValFromUcd("pos.wcs.crval", true);

    	if( crvalStr==null || crvalStr.length()==0 ) return null;
    	String[] crval = TreeBuilder.split(crvalStr, " ,");
    	if( crval.length<2 ) return null;
    	double crval1, crval2;
    	try {
    		crval1 = Double.valueOf(crval[0]).doubleValue();
    		crval2 = Double.valueOf(crval[1]).doubleValue();
    	}
    	catch( NumberFormatException e ) {return null;}

    	// récupération des valeurs de la matrice CD
    	Aladin.trace(3, "Looking for CDMatrix");
    	// vieil UCD
    	String cdStr = getFieldValFromUcd("VOX:WCS_CDMatrix", true);
    	// UCD 1+
    	if( cdStr==null || cdStr.length()==0 ) cdStr = getFieldValFromUcd("pos.wcs.cdmatrix", true);

    	if( cdStr==null || cdStr.length()==0 ) return null;
    	String[] cd = TreeBuilder.split(cdStr, " ,");
    	if( cd.length<4 ) return null;
    	double cd00, cd01, cd10, cd11;
    	try {
    		cd00 = Double.valueOf(cd[0]).doubleValue();
    		cd01 = Double.valueOf(cd[1]).doubleValue();
    		cd10 = Double.valueOf(cd[2]).doubleValue();
    		cd11 = Double.valueOf(cd[3]).doubleValue();
    	}
    	catch( NumberFormatException e ) {return null;}

    	// récupération des valeurs de NAXIS
    	Aladin.trace(3, "Looking for NAXIS");
    	// vieil UCD
    	String naxisStr = getFieldValFromUcd("VOX:Image_Naxis", true);
    	// UCD 1+
    	if( naxisStr==null || naxisStr.length()==0 ) naxisStr = getFieldValFromUcd("pos.wcs.naxis", true);

    	if( naxisStr==null || naxisStr.length()==0 ) return null;
    	String[] naxis = TreeBuilder.split(naxisStr, " ,");
    	if( naxis.length<2 ) return null;
    	int naxis1, naxis2;
    	try {
    		naxis1 = Integer.valueOf(naxis[0]).intValue();
    		naxis2 = Integer.valueOf(naxis[1]).intValue();
    	}
    	catch( NumberFormatException e ) {return null;}

    	// récupération de la projection ("TAN", "ARC", ...)
    	Aladin.trace(3, "Looking for projection");
    	// vieil UCD
    	String projStr = getFieldValFromUcd("VOX:WCS_CoordProjection", true);
    	// UCD 1+
    	if( projStr==null || projStr.length()==0 ) projStr = getFieldValFromUcd("pos.wcs.ctype", true);

    	if( projStr==null || projStr.length()==0 ) return null;
    	int proj = Calib.getProjType(projStr);
    	if( proj<0 ) return null;

    	// récupération de l'équinoxe (si non trouvé, on prend 2000.0 comme valeur par défaut)
    	Aladin.trace(3, "Looking for equinox");
    	double equinox;
    	String equinoxStr = getFieldValFromUcd("VOX:STC_CoordEquinox", true);
    	if( equinoxStr==null || equinoxStr.length()==0 ) equinox = 2000.0;
    	try {
    		equinox = Double.valueOf(equinoxStr).doubleValue();
    	}
    	catch( Exception e ) {equinox = 2000.0;}

    	// récupération du système de coordonées (si non trouvé, "ICRS" par défaut)
    	Aladin.trace(3, "Looking for coord ref frame");
    	// vieil UCD
    	String frameStr = getFieldValFromUcd("VOX:STC_CoordRefFrame", true);
    	// UCD 1+
    	if( frameStr==null || frameStr.length()==0 ) frameStr = getFieldValFromUcd("pos.frame", true);

    	if( frameStr==null || frameStr.length()==0 ) frameStr = "ICRS";


    	// on a récupéré les params nécessaires à la construction de la calibration,
    	// on la crée !
    	return new Calib(crval1, crval2, crpix1, crpix2, naxis1, naxis2, cd00, cd01, cd10, cd11, equinox, 2000.0, proj);
    }

    protected boolean isColorImage() {
    	return curFormat!=null &&
		       (curFormat.indexOf("png")>=0 || curFormat.indexOf("jpeg")>=0 || curFormat.indexOf("gif")>=0 || curFormat.indexOf("tif")>=0);
    }

    /**
     * retourne l'ensemble des métadonnées du noeud sous forme d'une Map
     * on renvoie l'ensemble des UCD --> valeur, utype --> valeur
     *
     * @return
     */
    protected Map getMetadata() {
    	Map map = new Hashtable();


    	try {
    		String value;
    		int idx;
    		for( int i=0; i<ucds.length; i++ ) {
    			value = originalExpla[i];

    			if( ucds[i]!=null && ucds[i].length()>0 ) {
//    				System.out.println("ucd "+ucds[i]+" = "+value);
    				map.put(ucds[i], value);
    			}
    			if( utypes[i]!=null && utypes[i].length()>0 ) {
//    				System.out.println("utype "+utypes[i]+" = "+value);
    				map.put(utypes[i], value);
    			}
    		}
    	}
    	catch( Exception e ) {
    		System.out.println("problem while extracting metadata");
    		if( Aladin.levelTrace>=3 ) e.printStackTrace();
    	}

    	return map;
    }

	/**
	 * @return Returns the pixSize.
	 */
	protected String getPixSize() {
		return pixSize;
	}
	/**
	 * @param pixSize The pixSize to set.
	 */
	protected void setPixSize(String pixSize) {
		// on a parfois des tailles négatives, on les corrige
		if( pixSize!=null && pixSize.charAt(0)=='-' ) pixSize = pixSize.substring(1);
		this.pixSize = pixSize;
	}
	/**
	 * @return Returns the pixSizeDeg.
	 */
	protected double getPixSizeDeg() {
		return pixSizeDeg;
	}
	/**
	 * @param pixSizeDeg The pixSizeDeg to set.
	 */
	protected void setPixSizeDeg(double pixSizeDeg) {
		// on a parfois des tailles négatives, on les corrige
		pixSizeDeg = Math.abs(pixSizeDeg);
		this.pixSizeDeg = pixSizeDeg;
	}

    /**
     * Trouve le message PLASTIC/SAMP approprié pour charger ce noeud
     * (basé sur ResourceNode.type)
     *
     * @return l'URI correspondant au message PLASTIC, null si aucun message ne convient
     */
    protected AppMessagingInterface.AbstractMessage getPlasticMsg() {
        // cas spécial pour SSAP Igor (Euro-3D client)
        if( format!=null && ( format.equals("application/fits-euro3d") || format.equals("application/fits-flames-giraffe") ) ) {
            return AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS;
        }

        // TODO : classe AbstractMessage et classe Message
        if( this.type==ResourceNode.IMAGE ) return AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS;
        else if( this.type==ResourceNode.CAT ) return AppMessagingInterface.ABSTRACT_MSG_LOAD_VOT_FROM_URL;
        else if( this.type==ResourceNode.SPECTRUM ) return AppMessagingInterface.ABSTRACT_MSG_LOAD_SPECTRUM_FROM_URL;
        else if( this.type==ResourceNode.CHARAC ) return AppMessagingInterface.ABSTRACT_MSG_LOAD_CHARAC_FROM_URL;

        return null;
    }

    protected String getLocation() {
        if( curFormat!=null && curFormat.indexOf("image/tif")>=0 && location!=null && location.startsWith("http")) {
            return getConvertProxyUrl(location);
        }
        return location;
    }

    /**
     * checks whether the node matches the free constraint c,
     * ie if the substring c appears somewhere in the node values
     * @param c
     * @return
     */
    protected boolean matchFreeConstraint(String c, boolean ignoreCase) {
        if( ignoreCase ) c = c.toLowerCase();
        String curExpla;
        for( int i=0; i<explanation.length; i++ ) {
            curExpla = explanation[i];
            if( curExpla!=null && ignoreCase ) curExpla = curExpla.toLowerCase();
            if( curExpla!=null && curExpla.indexOf(c)>=0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks whether the node matches the field constraint key, value
     * ie if there is a field with name/ID/UCD/utype containing key and whose value contains value
     * @param c
     * @return
     */
    protected boolean matchFieldConstraint(String key, String value, boolean ignoreCase) {
        if( ignoreCase ) {
            key = key.toLowerCase();
            value = value.toLowerCase();
        }
        String curDesc, curUcd, curUtype, curExpla;
        for( int i=0; i<description.length; i++ ) {
            curDesc = description[i];
            curUcd = ucds[i];
            curUtype = utypes[i];
            curExpla = explanation[i];

            if( ignoreCase ) {
                if( curDesc!=null ) curDesc = curDesc.toLowerCase();
                if( curUcd!=null ) curUcd = curUcd.toLowerCase();
                if( curUtype!=null ) curUtype = curUtype.toLowerCase();
                if( curExpla!=null ) curExpla = curExpla.toLowerCase();
            }
            if( (  curDesc!=null && curDesc.indexOf(key)>=0
                || curUcd!=null && curUcd.indexOf(key)>=0
                || curUtype!=null && curUtype.indexOf(key)>=0
                )
                && curExpla!=null && curExpla.indexOf(value)>=0
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a URL allowing to convert the image designated by its URL to JPEG
     * @param url
     * @return
     */
    private String getConvertProxyUrl(String url) {
        URL baseUrl = aladin.glu.getURL("ConvertToJpg");
        if( url==null || baseUrl==null ) {
            return null;
        }
        return baseUrl+URLEncoder.encode(url);
    }

	/**
	 * Retourne la commande script permettant de charger les données décrites par ce noeud
	 * Retourne null si la commande n'existe pas
	 * @return
	 */
	protected String getScriptCommand() {
		// TODO : à tester plus en profondeur
		// pour le moment, on ne le fait que pour les noeuds de l'arbre Aladin
		if( server==null || ! (server instanceof ServerAladin) ) {
			return null;
		}

		String cmd = "get aladin(";
		cmd += survey;

		if( bandPass!=null ) cmd += "," + bandPass;

		cmd += ") ";



		String position = cutoutTarget!=null?cutoutTarget:getFov().alpha+" "+getFov().delta;
		cmd += position;

		return cmd;
	}

    public double getDistanceToCenter() {
        return distanceToCenter;
    }

    public void setDistanceToCenter(double distanceToCenter) {
        this.distanceToCenter = distanceToCenter;
    }
}
