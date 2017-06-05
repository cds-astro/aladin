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

/** <p>Title : TreeBuilder</p>
 *  <p>Description : Builds the hierarchical tree of available resources from a VOTable file</p>
 *  <p>Copyright: 2003</p>
 *  <p>Company: CDS </p>
 *  @author Thomas Boch
 *  @version 1.1 : 22 mars 2006 - Correction du calcul de l'angle de position de l'image
 *                                en utilisant la matrice CD (UCD=VOX:WCS_CDMatrix)
 *
 *
 *           0.7 : 24 juin 2003 - Prise en compte de la modif StoredImage
 * 			 0.65: 10 avril 2003 - Correction de bugs, ajout du tri sur les critères, et suppression de code non générique
 * 			 0.6 : 17 mars 2003 - Prise en compte de la modif de François B. (ajout de StorageMapping dans le fichier XML)
                                  Suppressions de la possibilité de parser du parfile
 *           0.5 : 21 Novembre 2002 (Creation)
 */


package cds.aladin;

import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;
import cds.astro.Astrocoo;
import cds.astro.Coo;
import cds.savot.model.FieldSet;
import cds.savot.model.InfoSet;
import cds.savot.model.ParamSet;
import cds.savot.model.ResourceSet;
import cds.savot.model.SavotField;
import cds.savot.model.SavotInfo;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotTD;
import cds.savot.model.SavotTR;
import cds.savot.model.SavotVOTable;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.Util;

public class TreeBuilder {

	// different available types
	static final int VOTABLE_IDHA=0;
	static final int VIZIER=1;
	static final int SIAP=2;
    static final int SSA=3;
    static final int SIAP_EVOL=4; // ancienne version, sans les utype
    static final int SIAP_EXT=5; // nouvelle version, avec les utype

	// ces valeurs devront être remplies et non prédéfinies comme actuellement
	private int indexCriteria=0; // index dans le TRSet du critère
	private int indexValue=1; // index dans le TRSet de la valeur du critère


	// variables needed for files complying with SIAP
	private static final String SIAP_IMAGE_TITLE = "VOX:Image_Title";
    private static final String SIAP_IMAGE_TITLE2 = "OBS_ID"; // test pour XMM
    protected static final String SIAP_RA = "POS_EQ_RA_MAIN";
    protected static final String SIAP_DE = "POS_EQ_DEC_MAIN";
    private static final String SSAP_UTYPE_SPATIAL_COVERAGE = "Char.SpatialAxis.Coverage.Location.Value";
    protected static final String SIAP_RA_UCD1P = "pos.eq.ra;meta.main";
    protected static final String SIAP_DE_UCD1P = "pos.eq.dec;meta.main";
    private static final String SIAP_SCALE = "VOX:Image_Scale";
    private static final String SIAP_NAXIS = "VOX:Image_Naxis";
    private static final String SIAP_IMAGE_FORMAT = "VOX:Image_Format";
    private static final String SIAP_URL = "VOX:Image_AccessReference";
    private static final String SIAP_URL2 = "DATA_LINK"; // test pour XMM
    private static final String FORMAT = "FORMAT"; // pour données ISO

    // proposition de l'ESAC (Vilspa)
    private static final String ESAC_AXES = "VOX:Spectrum_axes"; // nom des axes wavelength/flux
    private static final String ESAC_UNITS = "VOX:Spectrum_units";
	private static final String ESAC_DIMEQ = "VOX:Spectrum_dimeq";
	private static final String ESAC_SCALEQ = "VOX:Spectrum_scaleq";

    protected static final String MAPPARAM = "MapPare"; // Mapping parameters

	// pour le SIAP (modifié pour démo) généré par le serveur Aladin, on va prendre en compte les ID plutot que les UCD
//	private static final String SIAP_ALADIN_FORMAT = "OriginalCoding";
	protected static final String SIAP_BANDPASS_ID = "VOX:BandPass_ID";

    // utype pour reference faire footprint attaché
    private static final String FOOTPRINT_REF_UTYPE = "dal:footprint.geom.id";

    // type de ressource (valeurs possibles : SPECTRUM, IMAGE, ...)
    private static final String RESTYPE = "resourceType";

    private static final String IDSURVEY = "ObservingProgram";
    private static final String IDBAND = "Observation_Group";
    private static final String IDIMAGE = "Observation";
    private static final String IDMAPPING = "StorageMapping";
    private static final String IDSTORED = "StoredImage";

    private static final String OBS_NAME = "Observation_Name";
    private static final String CUTOUT = "CUTOUTS";
    private static final String AVAILABLE_CODINGS = "COMPRESSION";
    private static final String AVAILABLE_PROCESSINGS = "MODE";
    private static final String OBS_DATE = "date";
    private static final String ORIGIN = "Origin";
    private static final String INDEXING = "Indexing";
    protected static final String NB_OF_PATCHES = "number";
    private static final String RESOLUTION = "RESOLUTION";
    private static final String MACHINE = "MACHINE";
    private static final String MAX_SIZE = "size";
    private static final String LOCATION = "Location";
    private static final String GLULINK = "GLink";
    private static final String FILTER_NAME = "Filter_Name"; // usual name of the observed band

    private static final String UTYPE_DM_SSA = "Dataset.DataModel";
    private static final String UTYPE_DSLENGTH_SSA = "Dataset.Length";
    private static final String UTYPE_ACREF_SSA = "Access.Reference";
    private static final String UTYPE_DATATITLE_SSA = "DataID.Title";
    private static final String UCD1P_TITLE = "meta.title";

    public static final String UTYPE_STCS_REGION1 = "*ObservationLocation.AstroCoordArea.Region";
    public static final String UTYPE_STCS_REGION2 = "*Char.SpatialAxis.Coverage.Support.Area";

    // variables de travail pour le Fov
    private double xVal, yVal, alphaVal, deltaVal;
    private double[] xValTab, yValTab;

    String[] descFilter;
    double angleVal = Double.NaN;

	// type = [VOTABLE_IDHA | VIZIER | SIAP]
    private int type;

    // serveur d'ou proviennent les ressources
    private Server server = null;

    static private Color[] colTab = {Color.green, Color.orange, Color.magenta, Color.cyan, Color.pink};

    static private int colorNb=0;

    // memorisation du dernieur noeud "band: cree
    private ResourceNode nodeMemo;

    // pour determiner la couleur du fov
    private Color fovColor;

    // pour recuperer la taille d'un cutout
    private double maxSize,pixSize;
    private String pixSizeUnit="";

    // pour conversion sexa<->decimal
    private Astrocoo frame = new Astrocoo();

    String nameSiapEvolStr;

	// stream permettant de constuire l'arbre
	private InputStream is;
	// url permettant de construire l'arbre
    private URL url;
	// nom du fichier permettant de construire l'arbre
    private String file;

    private SavotPullParser savotParser;

    // mémorisation du nom d'objet entré par l'utilisateur
    private String objet;
    // variable de travail contenant le target en sexa de objet
    private String targetObjet;
    // coordinates of requested target
    private Coord targetObjetCoo;

    // message d'erreur eventuel
    private String error;


    // tableau des ID ou names des FIELD sur lesquels effectuer le tri
    protected String[] sortItems;

    // variables de travail
    private int nameIndex = 0;
    private String currentSurvey, currentColor, currentWavelength, currentFilterName;

    private FieldSet surveyFieldSet, bandFieldSet, imageFieldSet, filterFieldSet, mappingFieldSet, storedFieldSet;
    // contient la correspondance Resource --> FieldSet
    private Hashtable fieldSetMapping = new Hashtable();

    Aladin aladin;

    /** Constructor
     *  @param file - xml file
     *  @param type - type of file being processed : [VOTABLE_IDHA | VIZIER | SIAP]
     * 	@param server - serveur d'où proviennent les ressources (peut être null)
     */
//    TreeBuilder(Aladin aladin, String file, int type, Server server) {
//    	this.aladin = aladin;
//        this.file = file;
//        this.type = type;
//        this.server = server;
//    }

    /** Constructor
     *  @param url - url to retrieve the xml document
     *  @param type - type of file being processed : [VOTABLE_IDHA | VIZIER | SIAP]
     * 	@param server - serveur d'où proviennent les ressources (peut être null)
     */
    TreeBuilder(Aladin aladin, URL url, int type, Server server, String target) {
        this.aladin = aladin;
    	this.url = url;
        this.type = type;
        this.server = server;
        this.objet = target;

    }

    TreeBuilder(Aladin aladin, String target) {
        this.aladin = aladin;
        this.objet = target;
    }

    /** Constructor
     *  @param is - the stream holding the xml document
     *  @param type - type of file being processed : [VOTABLE_IDHA | VIZIER | SIAP]
     * 	@param server - serveur d'où proviennent les ressources (peut être null)
     */
    TreeBuilder(Aladin aladin, InputStream is, int type, Server server, String target) {
    	this.aladin = aladin;
        this.is = is;
        this.type = type;
        this.server = server;
        this.objet = target;
    }

    private void suite() {
        if( objet==null && server!=null && server.target!=null ) {
            objet = server.target.getText();
        }
        targetObjet = TreeView.resolveTarget(objet,aladin);
        targetObjetCoo = TreeView.resolveTargetCoo(objet,aladin);

        createSavotParser();
    }

    protected boolean mayBeSSA(SavotVOTable vot) {
        SavotResource firstRes = vot.getResources().getItemAt(0);

        // detection de SSA
        InfoSet infos = firstRes.getInfos();
        SavotInfo info;
        for( int i=0; i<infos.getItemCount(); i++ ) {
            info = infos.getItemAt(i);
            if( info.getContent().equals("SSAP") ) {
                return true;
            }
        }

        if( firstRes.getFieldSet(0)==null || firstRes.getFieldSet(0).getItems()==null ) {
        	return false;
        }

//        Enumeration<SavotField> fields = firstRes.getFieldSet(0).getItems().elements();
        //Enumeration<Object> fields = firstRes.getFieldSet(0).getItems().elements();
        List<SavotField> fields = firstRes.getFieldSet(0).getItems();
        SavotField curField1;
        String curUtype, curUCD;
        // TODO : pour distinguer vraiment SSAP de SIAP, on pourrait peut-etre se baser sur le namespace
        for (SavotField curField : fields) {
            curUtype = stripNSForUtype(curField.getUtype().trim());
            curUCD = curField.getUcd();
            if(    curUtype.equalsIgnoreCase(UTYPE_ACREF_SSA) && ! curUCD.equalsIgnoreCase(SIAP_URL)
                || curUtype.equalsIgnoreCase(UTYPE_DM_SSA)
                || curUtype.equalsIgnoreCase(UTYPE_DSLENGTH_SSA) ) {
                return true;
            }
        }

        return false;
    }

    /** détecte s'il s'agit d'un fichier au format VOTABLE_IDHA ou SIAP ou VIZIER */
    private void detectFormat() {
        String tmp = file!=null?file:(url!=null?url.toString():"stream");
        if( type>=0 ) {
            Aladin.trace(3,"detect format of "+tmp+": format was already specified");
            return;
        }
        // on récupère la première ressource
        SavotResource firstRes = savotParser.getVOTable().getResources().getItemAt(0);
        int nbRes = savotParser.getVOTable().getResources().getItemCount();
        SavotResource secondRes = null;
        if( nbRes>=2 ) {
            secondRes = savotParser.getVOTable().getResources().getItemAt(1);
        }

        if( firstRes==null ) {
            Aladin.trace(3,"Could not determine type, stream contains no RESOURCE");
            return;
        }

        // detection de SIAP_EXTENSION
        String utype = firstRes.getUtype();
        if( utype!=null && utype.equals(SIAPExtBuilder.UT_SIMPLEQUERY) ) {
        	type = SIAP_EXT;
        	return;
        }
        // pour les footprints attachés
        if( firstRes.getTableCount()>0 ) {
        	FieldSet fSet = firstRes.getFieldSet(0);
        	if( fSet!=null && fSet.getItems()!=null ) {
        		List<SavotField> e = fSet.getItems();
        		for (SavotField savotField : e) {
        			if(savotField.getUtype().equalsIgnoreCase(FOOTPRINT_REF_UTYPE) ) {
        				type = SIAP_EXT;
        				return;
        			}
        		}
        	}
        }

        // detection de SSA
        if( mayBeSSA(savotParser.getVOTable()) ) {
            type = SSA;
            return;
        }


        // détection de SIAP
        String myType = firstRes.getType();
        if( myType==null ) myType = "";
        if( myType.equals("results") ) {
            // détection de SIAP_EVOL
            if( secondRes!=null && secondRes.getType()!=null &&
                        (secondRes.getType().equals("GeneralFeatures") || secondRes.getName().equals("GeneralFeatures") ) ) {
                type = SIAP_EVOL;
                return;
            }

            type = SIAP;
            return;
        }

        // détection de IDHA
        String name = firstRes.getId();
        if( name==null || name.length()==0 ) name = firstRes.getName();
        if( name!=null && ( name.equalsIgnoreCase(IDSURVEY) || name.equals(IDBAND) ) ) {
            type = VOTABLE_IDHA;
            return;
        }




        // méthode très médiocre en attendant mieux
//        if( myType.equals("meta") ) {
//            type = VIZIER;
//            return;
//        }
        // QUESTION : que faire dans ce cas ? affecter un type par défaut qui serait le moins restrictif ???
        //type=SIAP;
    }

    /**
     * Supprime la partie namespace d'un utype (et le préfixe "SSA." s'il existe)
     * @param utype
     * @return
     */
    private String stripNSForUtype(String utype) {
        String ret;
        final String prefix = "SSA.";
        // suppression du namespace
        int idx = utype.indexOf(':');
        ret = ( idx!=-1 && utype.length()>idx+1)?utype.substring(idx+1):utype;
        // suppression du préfixe "SSA."
        if( ret.startsWith(prefix) ) ret = ret.substring(prefix.length());

        return ret;
    }

	// modifie le flux
	/*
	private InputStream getBidouilledStream() {
	    DataInputStream dis=null;
        String CR = System.getProperty("line.separator");
        StringBuffer output = new StringBuffer();
        try {
        	if( file!=null ) dis = new DataInputStream(new FileInputStream(file));
	    	else dis = new DataInputStream(url.openStream());
        }
        catch(Exception e) {e.printStackTrace();}


	    String line;
	    try {
	        while( (line=dis.readLine())!=null ) {
	            int begin = line.indexOf("<![CDATA[");
	    		int end = line.indexOf("]]>");
	    		if( begin>=0 && end>begin ) {
	    		    line = replaceBidouille(line,begin,end);
	    		}

	    		output.append(line);
	    		output.append(CR);
	    	}
	    }
	    catch(IOException ioe) {ioe.printStackTrace();}

        return new BufferedInputStream(new ByteArrayInputStream(output.toString().getBytes()));

	}


	private String replaceBidouille(String str,int begin,int end) {
	    String ret = str.substring(0,begin)+URLEncoder.encode(str.substring(begin+9,end))+str.substring(end+3);
	    return ret;
	}
	*/

    /** builds the tree */
    protected ResourceNode build() throws Exception {
        suite();

        // recherche du target
        searchTarget();

        detectFormat();

        Aladin.trace(3, "Detected format of document : "+readableFormat(type));

        if( type==VOTABLE_IDHA )
			return buildVotable();
        else if( type==VIZIER )
            return buildCatVotable();
        else if( type==SIAP || type==SSA )
        	return buildSIAPVotable();
        else if( type==SIAP_EVOL )
            return buildSIAPEvolVotable();
        else if( type==SIAP_EXT )
        	return buildSIAPExtVotable();
        // sinon, on essaye SIAP tout de même ?
        else return buildSIAPVotable();

        //return null;
    }

    private String readableFormat(int t) {
    	if( t==VOTABLE_IDHA ) return "VOTABLE_IDHA";
    	if( t==VIZIER ) return "VIZIER";
    	if( t==SIAP ) return "SIAP";
    	if( t==SSA ) return "SSA";
    	if( t==SIAP_EVOL ) return "SIAP_EVOL";
    	if( t==SIAP_EXT ) return "SIAP_EXT";

    	return "unknown";
    }

    // cette variable est fixée lors du build()
    // elle correspond au target trouvé dans le VOTable
    private String targetFound = null;
    /** search for the target in the VOTABLE file */
    private void searchTarget() {
        if( savotParser==null ) return;
        // pour recherche IDHA
        InfoSet infos = savotParser.getVOTable().getInfos();
        SavotInfo info;
        for( int i=0; i<infos.getItemCount(); i++ ) {
            info = infos.getItemAt(i);
            if( info.getId().equalsIgnoreCase("position") ) {
                targetFound = info.getValue();
                if( targetObjet==null ) targetObjet = targetFound;
                return;
            }
        }

        // pour recherche SIAP
        ParamSet params;
        SavotParam param;
        try {
        	params = savotParser.getVOTable().getResources().getItemAt(0).getParams();
        }
        // au cas ou on ne trouve pas de ressource par exemple
        catch( Exception e ) {return;}

        for( int i=0; i<params.getItemCount(); i++ ) {
            param = params.getItemAt(i);
            if( param.getName().equalsIgnoreCase("INPUT:POS") ) {
                targetFound = param.getValue();
                return;
            }
        }
    }

    protected String getTarget() {
        return targetFound;
    }

    private String requestedPos;

    protected void setRequestedPos(String s) {
    	this.requestedPos = s;
    }

    /**
     * retourne la position centrale de la requeted
     * @return
     */
    protected String getRequestedPos() {
    	return this.requestedPos;
    }



/** --------- Methods to build the tree from a VOTable file ---------- */

   /** Construit l'arbre des catalogues a partir du votable provenant de VizieR */
   private ResourceNode buildCatVotable() {
		if( savotParser==null ) {
        	return null;
		}

        // La racine catalogues
        ResourceNode n = new ResourceNode(aladin, "root");
        n.type = ResourceNode.VOID;

        // on recupere l'objet votable
        SavotVOTable voTable = savotParser.getVOTable();
        // on recupere toutes les resources
        ResourceSet resources = voTable.getResources();

        // pour chaque resource
        for( int i=0; i<resources.getItemCount(); i++ ) {
            SavotResource curResource = resources.getItemAt(i);
            ResourceNode newNode = new ResourceNode(aladin, curResource.getName());
            newNode.isLeaf = true;
            newNode.type = ResourceNode.CAT;
            //newNode.catDesc = curResource.getDescription();
            // recuperation des infos
            InfoSet infos = curResource.getInfos();
            String[] expla = new String[infos.getItemCount()];
            String[] desc = new String[infos.getItemCount()];

            // memorisation du keyword precedent
            String oKeyword=null;
            for( int j=0; j<infos.getItemCount(); j++ ) {
                SavotInfo info = infos.getItemAt(j);
                String keyWord = info.getName();
                String value = info.getValue();

                // on enleve le le tiret
                if( keyWord.startsWith("-") ) keyWord = keyWord.substring(1);
                // on enleve le "kw."
                if( keyWord.startsWith("kw.") ) keyWord = keyWord.substring(3);

                // nouveau keyword
                if( oKeyword==null || !keyWord.equals(oKeyword) ) {
                    desc[j] = keyWord;
                    expla[j] = value;
                    oKeyword = keyWord;
                }
                // keyword deja cree
                else {
                    desc[j] = "  \"   \"";
                    expla[j] = value;
                }
            }

            n.addChild(newNode);
            newNode.description = desc;
            newNode.explanation = expla;
        }
        return n;
   }

	private void createSavotParser() {
        if( url!=null ) {
            try {
                savotParser = new SavotPullParser(url, SavotPullEngine.FULL, null);
            }
            catch(Exception e) {e.printStackTrace();savotParser=null;}
        }
        else if( file!=null ) {
            try {
                savotParser = new SavotPullParser(file, SavotPullEngine.FULL);
            }
            catch(Exception e) {e.printStackTrace();savotParser=null;}
        }
        else if( is!=null ) {
            savotParser = new SavotPullParser(is, SavotPullEngine.FULL, null);
        }

	}

	private ResourceNode buildSIAPExtVotable() throws Exception {
		SIAPExtBuilder builder = new SIAPExtBuilder(aladin, objet);
		builder.setRequestedPos(this.getRequestedPos());
		return builder.build(savotParser);
	}

/////////////////////////////////////////////////////////////////////////////
/////////// Parsing d'un stream de type SIAP_EVOL ///////////////////////////
/////////////////////////////////////////////////////////////////////////////

    /* ---- Méthodes pour constuire l'arbre partant d'un fichier SIAP ---- */
    private ResourceNode buildSIAPEvolVotable() {
        int obsLocationIndex = -1;

        if( savotParser==null ) {
            return null;
        }

        SavotVOTable vot = savotParser.getVOTable();

        ResourceSet rootResSet = vot.getResources();
        // noeud racine
        ResourceNode root = new ResourceNode(aladin, "root");
        root.type = ResourceNode.VOID;
        root.isSIAPEvol = true;

        SavotResource res=null;
        // on suppose que la resource dont le type="results" est la premiere
        res = rootResSet.getItemAt(0);

        if( res==null ) return null;

        SavotField[] initFields = createDescription(res.getFieldSet(0));
        String[] idFields = new String[initFields.length];
        for( int i=0; i<initFields.length; i++ ) idFields[i] = initFields[i].getId();


        SavotResource res2 = rootResSet.getItemAt(2);
        SavotField[] fields2 = createDescription(res2.getFieldSet(0));

        int toto = findFieldByID("RelatedObservation", fields2);

        String totoStr = fields2[toto].getRef();

        nameSiapEvolStr = totoStr.trim();
//        System.out.println("totoStr : "+totoStr);

        processSIAPEvolResource(res, root);


        Hashtable namesToNodes = new Hashtable();

        Hashtable altNamesToNodes = new Hashtable();

        // ici, je fais un gros présupposé !!!
        String refName = (rootResSet.getItemAt(2).getFieldSet(0).getItemAt(0)).getRef();
//        System.out.println("ref name : "+refName);




        // on suppose que la resource dont le type="GeneralFeatures" est la deuxieme
        res = rootResSet.getItemAt(1);
        if( res!=null ) {
            if( root.nbChildren>0 ) {
                // les descriptions disponibles
                Vector desc = new Vector();
                ResourceNode n = (ResourceNode)root.getChildrenAt(0);
                //int obsRefIndex = findValIndex(n.description, "ObservationReference");
                int obsRefIndex = findValIndex(idFields, refName);
                obsLocationIndex = findValIndex(idFields, "Location");

                int idxRefRelObs = findValIndex(idFields, totoStr);
//System.out.println(obsRefIndex+"\t"+idxRefRelObs);
                for( int i=0; i<n.description.length; i++ ) desc.addElement(n.description[i]);

                Hashtable genFeatNodes = new Hashtable();
                processGenFeat(res, genFeatNodes, desc);

                // REPRENDRE ICI en attachant les noeuds !!
                Enumeration e = root.getChildren();
                ResourceNode associatedNode;


                String key;
                while( e.hasMoreElements() ) {
                  n = (ResourceNode)e.nextElement();

                  // pour les references des sousobs
                  namesToNodes.put(n.explanation[obsRefIndex], n);
//                  System.out.println(n.explanation[obsRefIndex]);

                  //if( altNamesToNodes.get(n.name)==null ) altNamesToNodes.put(n.name, new Vector());
                  //((Vector)altNamesToNodes.get(n.name)).addElement(n);

                  if( altNamesToNodes.get(n.explanation[idxRefRelObs])==null ) altNamesToNodes.put(n.explanation[idxRefRelObs], new Vector());
                  ((Vector)altNamesToNodes.get(n.explanation[idxRefRelObs])).addElement(n);
//                  System.out.println("on met dans altNames : "+n.explanation[idxRefRelObs]);


                    n.links = new Hashtable();
                    //System.out.println("\n node : "+n.name);
                    for( int i=0; i<n.description.length; i++ ) {
                        key = n.description[i]+n.explanation[i];
                        associatedNode = (ResourceNode)genFeatNodes.get(key);

                        if( associatedNode!=null ) {
                            associatedNode.isLeaf = false;
                            n.links.put(n.description[i], associatedNode);
                            //System.out.println(key);
                        }
                    }
                }
            }
        }


        // on suppose que la 3e resource est ObservationDetails
        res = rootResSet.getItemAt(2);
        if( res!=null ) {
          root.removeAllChild();

          SavotField[] fields = createDescription(res.getFieldSet(0));


          int nbTr = res.getTRCount(0);
          TRSet obsSet = res.getData(0).getTableData().getTRs();

          boolean cutout;
          String key;
          String location;
          TDSet tdSet;
          ResourceNode curNode, newNode;
          Hashtable obsToSubObs = new Hashtable();

          Vector alreadyAdded = new Vector();

          //System.out.println(remainingObs.size());

          int locationIndex = findFieldByUtype("Observation.Provenance.DataViewsAndAccess.AccessReference", fields);
          if( locationIndex<0 ) locationIndex = findFieldByID("LinktoPixels", fields);

          int dataOrgIndex = findFieldByID("DataOrganisation", fields);
          int relatedObsIndex = findFieldByID("RelatedObservation", fields);


          int obsRefIndex = findFieldByID("ObservationReference", fields);
          int numberIndex = findFieldByID(NB_OF_PATCHES, fields);
          int mapParamIndex = findFieldByID(MAPPARAM, fields);
          int indexingIndex = findFieldByID(INDEXING, fields);
          int descIndex = findFieldByID("desc", fields);

          String relatedObs;
          /*
          System.out.println(locationIndex);
          System.out.println(dataOrgIndex);
          System.out.println(relatedObsIndex);
          */

          for( int i=0;i<nbTr;i++) {
              tdSet = obsSet.getTDSet(i);

              // au cas ou on a des TD qui manquent !
              int nbTD = tdSet.getItemCount();

              // TODO index hard-codé : a changer
              key = tdSet.getContent(0).trim();
              String cutoutVal = tdSet.getContent(dataOrgIndex).trim();
              cutout = cutoutVal.equals("CUTOUTS");
              boolean lowerLev = cutoutVal.equals("LOWERLEVEL");
              location = tdSet.getContent(locationIndex);

              String nbPatch = "";
              if( numberIndex>=0 && (numberIndex<nbTD) ) nbPatch = tdSet.getContent(numberIndex).trim();
              String mapParam = "";
              if( mapParamIndex>=0 && mapParamIndex<nbTD ) mapParam = tdSet.getContent(mapParamIndex).trim();
              String indexing = "";
              if( indexingIndex>=0 && indexingIndex<nbTD  ) indexing = tdSet.getContent(indexingIndex).trim();

//              REPRENDRE ICI

              //System.out.println("loc : "+location);
              relatedObs = tdSet.getContent(relatedObsIndex);

              //System.out.println(location);
              // ici, on recupere le pere des sous-obs !
              curNode = (ResourceNode)namesToNodes.get(key);


              //System.out.println("on remove "+curNode.name);

              //System.out.println(curNode.name);
              if( curNode==null ) continue;




              newNode = new ResourceNode(aladin, curNode);
              // nb of patches
              if( nbPatch.length()>0 ) newNode.maxImgNumber = nbPatch;
//              REPRENDRE

              if( mapParam.length()>0 ) {
              		String[] params = split(mapParam,",");
                    if( params.length==2 ) {
                        try {
                            newNode.beginVel = Double.valueOf(params[0]).doubleValue();
                            newNode.velStep = Double.valueOf(params[1]).doubleValue();
                        }
                        catch( NumberFormatException e ) {}
                    }
              }

              if( indexing.length()>0 ) {
              	newNode.indexing = indexing;
              }

              newNode.cutout = cutout;
              newNode.location = location;

              if( cutoutVal!=null && cutoutVal.length()>0 ) newNode.altName = cutoutVal;

              if( cutoutVal.equals("PREVIEW") ) {
                  newNode.name += "_PREVIEW";
              }

              if( newNode.cutout ) {
                  if( targetObjet!=null ) {
                      newNode.setCutoutTarget(targetObjet);
                      newNode.targetObjet = targetObjet;
                  }
                  else newNode.setCutoutTarget(newNode.explanation[newNode.ra]+" "+newNode.explanation[newNode.de], false);
              }

              newNode.name = cutoutVal;

              // on met un nom plus sympa si c'est possible
              if( descIndex>=0 && descIndex<nbTD ) {
              	String desc = tdSet.getContent(descIndex);
              	if( desc!=null ) desc = desc.trim();
              	if( desc.length()>0 ) newNode.name = desc;
              }

              if( !lowerLev ) {
                  curNode.addChild(newNode);
                  curNode.isLeaf = false;
                  //if( obsLocationIndex>=0 ) curNode.explanation[obsLocationIndex] = "";
                  newNode.isLeaf = true;
              }
              else {

                  // pas d'explication pour les LOWERLEVEL
                  newNode.description = null;
//                  newNode.name = cutoutVal; // c'est sur cette ligne que ça bloque !

                  curNode.isLeaf = false;
                  newNode.isLeaf = false;
                  // location == cl
                  Vector v = (Vector)altNamesToNodes.get(relatedObs);
//                  System.out.println("relatedObs vaut : "+relatedObs);
                  Enumeration e = v.elements();
                  ResourceNode n;
                  int l = 0;
                  boolean cycle = false;
//                  System.out.println(newNode.name);
                  while( e.hasMoreElements() ) {
                      n = (ResourceNode)e.nextElement();
                      newNode.addChild(n);
//                      System.out.println(n.name);
                      if( n==curNode) cycle = true;
                      alreadyAdded.addElement(n);
//                      System.out.println("j ajoute a alreadyadded : "+n);
                      n.name += "-"+(++l);
                  }
                  if( cycle ) {
                      System.out.println("cycle detected !");
                      ResourceNode pere = new ResourceNode(aladin, curNode);
                      curNode.isLeaf = true;
                      root.addChild(pere);
                      pere.isLeaf = false;
                      pere.addChild(newNode);
                  }
                  else curNode.addChild(newNode);
              }

//              System.out.println("curNode est : "+curNode);
              if( ! alreadyAdded.contains(curNode) ) {
                  root.addChild(curNode);
                  curNode.isLeaf = false;
                  alreadyAdded.addElement(curNode);
              }




              //System.out.println(lowerLev);

              Enumeration e = root.getChildren();
              while( e.hasMoreElements() ) ((ResourceNode)e.nextElement()).isObs = true;

              /*//NEW

              // on garde trace du lien obs-->sous-obs
              if( obsToSubObs.get(curNode)==null ) obsToSubObs.put(curNode, new Vector());
              ((Vector)obsToSubObs.get(curNode)).addElement(newNode);

              if( lowerLev ) {

                  newNode.isLeaf = false;
                  Vector v = (Vector)altNamesToNodes.get(location);
                  if( v==null ) System.out.println("vector is null !");
                  else {
                      Enumeration e = v.elements();
                      ResourceNode n;
                      while( e.hasMoreElements() ) {
                          n = (ResourceNode)e.nextElement();
                          newNode.addChild(n);
                      }
                  }
              }

              //System.out.println(newNode.name);
              //NEW /*
              newNode = new ResourceNode(curNode);
              newNode.cutout = cutout;
              newNode.location = location;

              if( cutoutVal.equals("PREVIEW")) newNode.name += "_PREVIEW";

              if( newNode.cutout ) {
                  if( targetObjet!=null ) {
                      newNode.setCutoutTarget(targetObjet);
                      newNode.targetObjet = targetObjet;
                  }
                  else newNode.setCutoutTarget(newNode.explanation[newNode.ra]+" "+newNode.explanation[newNode.de], false);
              }

              root.addChild(newNode);
              //NEW
          }


          Enumeration enum1 = obsToSubObs.keys();
          Vector v;
          ResourceNode tmp;
          while( enum1.hasMoreElements() ) {
              curNode = (ResourceNode)enum1.nextElement();
              curNode.isObs = true;

              System.out.println("node : "+curNode.name);
              //System.out.println("Traitement de l'obs "+curNode.name);
              v = (Vector)obsToSubObs.get(curNode);
              // dans ce cas, on crée des sous-obs
              if( v.size()>1 ) {
                  //System.out.println("Plus d'1 élt");
                  root.addChild(curNode);
                  curNode.isLeaf = false;
                  Enumeration subObsEnum = v.elements();
                  while( subObsEnum.hasMoreElements() ) {
                      tmp = (ResourceNode)subObsEnum.nextElement();
                      tmp.isSIAPEvol = true;
                      //System.out.println("ON ajoute à "+curNode.name+" la subobs "+tmp.name);
                      curNode.addChild(tmp);
                  }
              }
              // dans ce cas, on a juste une observation
              else if( v.size()==1 ) {
                  //System.out.println("Juste 1 élt");
                  tmp = (ResourceNode)v.elementAt(0);
                  tmp.isObs = true;
                  tmp.isSIAPEvol = true;
                  //System.out.println("ON ajoute à root la subobs "+tmp.name);
                  root.addChild(tmp);
              }
          }

        }


        Enumeration e2 = remainingObs.elements();
        System.out.println(remainingObs.size());
        while( e2.hasMoreElements() ) root.addChild((BasicNode)e2.nextElement());
        */ //NEW
    }
    }
        // final clean : we remove location for non-leaves nodes
        if( obsLocationIndex>=0 ) {
            Vector v = new Vector();
            BasicTree.getAllNonLeaves(root, v);
            Enumeration e = v.elements();
            ResourceNode curNode;
            while( e.hasMoreElements() ) {
                curNode = (ResourceNode)e.nextElement();
                curNode.explanation[obsLocationIndex] = "";
            }
        }


        return root;
    }

  /** Traite la resource "GeneralFeatures"
   *
   * @param res reference vers la resource
   * @param nodes noeuds qui seront remplis
   * @param desc ensemble des descriptions
   */
  protected void processGenFeat(SavotResource res, Hashtable nodes, Vector desc) {
      int nbTab = res.getTableCount();

      // boucle sur les tables
      for( int i=0; i<nbTab; i++ ) {

          SavotField[] fields = createDescription(res.getFieldSet(i));

          // d'après la spécif. SIAP, la RESOURCE results ne contient qu'une table

          // quand il n'y a pas de réponse !
          if( res.getData(i)==null ) continue;
          if( res.getData(i).getTableData()==null ) continue;

          TRSet obsSet = res.getData(i).getTableData().getTRs();
          ResourceNode n;
          int nbRows = obsSet.getItemCount();

          for( int j=0; j<nbRows; j++ ) {
              n = createSIAPNode(obsSet.getItemAt(j), fields);
              n.name = n.explanation[0];
              nodes.put(n.description[0]+n.name, n);
              //System.out.println(n.description[0]+" : "+n.explanation[0]+"**");
          }
      }
  }

private void processSIAPEvolResource(SavotResource res, ResourceNode root) {
    // on récupère le tableau de FIELD correspondant à mainRes
    SavotField[] fields = createDescription(res.getFieldSet(0));

    // d'après la spécif. SIAP, la RESOURCE results ne contient qu'une table

    // quand il n'y a pas de réponse !
    if( res.getData(0)==null ) return;
    if( res.getData(0).getTableData()==null ) return;

    TRSet obsSet = res.getData(0).getTableData().getTRs();
    ResourceNode[] resTab = new ResourceNode[obsSet.getItemCount()];

    for( int i=0; i<resTab.length; i++ ) {
        resTab[i] = createSIAPNode(obsSet.getItemAt(i), fields);
		resTab[i].isSIAPEvol = true;
        root.addChild(resTab[i]);
    }
}


///////////////////////////////////////////////////////////////////////////////
///////////// Fin parsing d'un stream SIAP_EVOL ///////////////////////////////
///////////////////////////////////////////////////////////////////////////////


	/* ---- Méthodes pour constuire l'arbre partant d'un fichier SIAP ---- */
	private ResourceNode buildSIAPVotable() {
        if( savotParser==null ) {
            return null;
        }

        SavotVOTable vot = savotParser.getVOTable();

		// on cherche une éventuelle indication du tri à appliquer
		searchSortOrder(vot);

        ResourceSet rootResSet = vot.getResources();
        // noeud racine
        ResourceNode root = new ResourceNode(aladin, "root");
        root.type = ResourceNode.VOID;
        //root.type = ResourceNode.IMAGE;

        SavotResource mainRes=null;
        // il peut y avoir plusieurs resources, on recherche celle avec type="results"
        SavotResource curRes=null;
        for( int i=0; i<rootResSet.getItemCount(); i++ ) {
            curRes = rootResSet.getItemAt(i);
            if( curRes.getType().equals("results") ) {
                mainRes = curRes;
                break;
            }
        }

        if( mainRes==null ) {
            // Que faire dans ce cas ?? affecter curRes à mainRes ?
            mainRes = curRes;
            if( mainRes==null ) return null;
            //return null;
        }
        processSIAPResource(mainRes, root);

        if( type==SSA ) root.type = ResourceNode.SPECTRUM;

        // finalement, on effectue le tri
        if( sortItems!=null ) MetaDataTree.doSortSiapEvol(sortItems, root);

        return root;
	}

	/**
	 *
	 * @param res la resource à traiter
	 * @param root le noeud auquel rattacher les nouveaux noeuds créés
	 * @param sort si true, on trie par survey puis par color (pour cas spécial Aladin)
	 */
	private void processSIAPResource(SavotResource res, ResourceNode root, boolean sort) {
        // traitement de la balise INFO
        processInfo(res);

        // si res ne contient aucune table
        if( res.getTables().getItemCount()==0 ) return;

        // on récupère le tableau de FIELD correspondant à mainRes
        SavotField[] fields = createDescription(res.getFieldSet(0));

        // d'après la spécif. SIAP, la RESOURCE results ne contient qu'une table

        // quand il n'y a pas de réponse !
        if( res.getData(0)==null ) return;
        if( res.getData(0).getTableData()==null ) return;

        TRSet obsSet = res.getData(0).getTableData().getTRs();
        ResourceNode[] resTab = new ResourceNode[obsSet.getItemCount()];

        if( sort ) {
        	ResourceNode curSurvey = null;
        	ResourceNode curColor = null;
            for( int i=0; i<resTab.length; i++ ) {
            	resTab[i] = createSIAPNode(obsSet.getItemAt(i), fields);
            	// création d'un nouveau noeud survey
                if( curSurvey==null || !resTab[i].survey.equals(curSurvey.name) ) {
            	    curSurvey = new ResourceNode(aladin, resTab[i].survey);
            	    curSurvey.type = ResourceNode.IMAGE;
            	    root.addChild(curSurvey);
                    // reset de la couleur
                    curColor=null;
            	}

            	// création d'un nouveau noeud color
            	if( curColor==null || !resTab[i].bandPass.equals(curColor.name) ) {
            	    curColor = new ResourceNode(aladin, resTab[i].bandPass);
            	    curColor.type = ResourceNode.IMAGE;
            	    curSurvey.addChild(curColor);
            	}

                curColor.addChild(resTab[i]);
            }
        }
        else {
            // "couleur" --> ResourceNode
            Hashtable colors = new Hashtable();
            ResourceNode parent,tmp;
        	for( int i=0; i<resTab.length; i++ ) {
                parent = root;
            	resTab[i] = createSIAPNode(obsSet.getItemAt(i), fields);
                if( resTab[i].bandPass!=null ) {
                     if( (tmp=(ResourceNode)colors.get(resTab[i].bandPass))!=null ) parent = tmp;
                     else {
                         parent = new ResourceNode(aladin);
                         parent.name = resTab[i].bandPass;
                         parent.type = ResourceNode.IMAGE;
                         colors.put(parent.name, parent);
                         root.addChild(parent);
                     }
                }
            	parent.addChild(resTab[i]);
        	}
        }
	}

    /** Traite la balise INFO dans le cas d'un stream SIAP
     * Permet de récupérer un éventuel message d'erreur
     * @param res
     */
    private void processInfo(SavotResource res) {
        InfoSet infos = res.getInfos();
        if( infos.getItemCount()==0 ) return;
        SavotInfo curInfo, qStatusInfo;
        qStatusInfo = null;
        for( int i=0; i<infos.getItemCount() ; i++ ) {
            curInfo = infos.getItemAt(i);
            if( curInfo.getName().equalsIgnoreCase("QUERY_STATUS") ) {
                qStatusInfo = curInfo;
                break;
            }
        }
        if( qStatusInfo==null ) return;
        String val = qStatusInfo.getValue();
        String content = qStatusInfo.getContent();
        if( ! val.equalsIgnoreCase("OK") ) {
            error = val+": "+content;
        }
    }

    protected String getError() {
        return error;
    }

	private void processSIAPResource(SavotResource res, ResourceNode root) {
	    processSIAPResource(res,root,false);
	}

	/** Crée un noeud d'après une SavotTR provenant d'un fichier SIAP */
    protected ResourceNode createSIAPNode(SavotTR tr, SavotField[] desc) {
        myIndexRA=myIndexDE=-1;
        boolean cutout = false;
        TDSet tdSet = tr.getTDs();
        int nbTd = tdSet.getItemCount();
        String[] expla = new String[nbTd];
        boolean[] hidden = new boolean[nbTd];
        String[] ucds = new String[nbTd];
        String[] utypes = new String[nbTd];
        String[] allUnits = new String[nbTd];
        String[] descStr = new String[desc.length];
        String[] originalExpla = new String[nbTd];
        String naxis,scale,imgFormat,color,survey,machine,resol,plateNumber,ssaAxes,ssaUnits,ssaDimeq,ssaScaleq,stcRegion;
        ssaAxes=ssaUnits=ssaDimeq=ssaScaleq=stcRegion=plateNumber=resol=machine=survey=color=imgFormat=naxis=scale=null;
        int index = -1;
        int indexSpatialLocation,indexRA,indexDE,indexLocation,indexNaxis,indexScale,indexImgFormat,indexOrigin,idxAngleVal;
        indexSpatialLocation=idxAngleVal=indexOrigin=indexImgFormat=indexScale = indexNaxis = indexLocation=indexRA=indexDE = -1;

        String name;
        String[] descId = new String[desc.length];
        // type de ressource : image, spectre, cube3D, ...
        String resType=null;
        // remplissage de descStr
        for( int i=0; i<desc.length; i++ ) {
            name = desc[i].getName();
            if( name.length()>0 ) {
                descStr[i] = name;
            }
            else {
                descStr[i] = desc[i].getId();
            }
            descId[i] = desc[i].getId();
            hidden[i] = false;
        }

        String unit;
        String strippedUtype;
        String ucd, utype;
        // recuperation des valeurs des TD
        for( int i=0; i<nbTd; i++ ) {
            expla[i] = tdSet.getItemAt(i).getContent();
            originalExpla[i] = expla[i];
            //System.out.println("**"+expla[i]+"**");

            try {
                ucd = desc[i].getUcd().trim();
                ucds[i] = ucd;
                utype = desc[i].getUtype().trim();
                utypes[i] = utype;

                strippedUtype = stripNSForUtype(utype);

                allUnits[i] = desc[i].getUnit();
//                System.out.println("utype : ***"+utypes[i]);
                // on récupère le nom de l'image
                if(    ucd.equalsIgnoreCase(SIAP_IMAGE_TITLE)
                	|| ucd.equalsIgnoreCase(UCD1P_TITLE)
                	|| ucd.equalsIgnoreCase(SIAP_IMAGE_TITLE2)
                	|| strippedUtype.equalsIgnoreCase(UTYPE_DATATITLE_SSA) ) {

                    index = i;
                }
                // on récupère la location spatiale (contient à la fois RA et DEC)
                else if( utype.toLowerCase().endsWith(SSAP_UTYPE_SPATIAL_COVERAGE.toLowerCase()) ) {
                	indexSpatialLocation = i;
                	String[] coo = Util.split(expla[i], " ");
                	alphaVal = new Double(coo[0]).doubleValue();
                	deltaVal = new Double(coo[1]).doubleValue();
                	// permet de transformer les coordonnées .03 .07 en 0.03 0.07
                	// (car AstroCoo utilisé plus tard n'aime pas les notations sans 0)
                	expla[indexSpatialLocation] = alphaVal+" "+deltaVal;
                }
                // on récupère RA
                else if( ucd.equalsIgnoreCase(SIAP_RA) || ucd.equalsIgnoreCase(SIAP_RA_UCD1P) || strippedUtype.equalsIgnoreCase("Char.SpatialAxis.Coverage.Ra") ) {
                    indexRA = i;
                    alphaVal = new Double(expla[i]).doubleValue();
                }
                // on récupère DE
                else if( ucd.equalsIgnoreCase(SIAP_DE) || ucd.equalsIgnoreCase(SIAP_DE_UCD1P) || strippedUtype.equalsIgnoreCase("Char.SpatialAxis.Coverage.Dec") ) {
                    indexDE = i;
                    deltaVal = new Double(expla[i]).doubleValue();
                }
                // récupération de la provenance des images
                else if( descId[i].equals(ORIGIN) ) {
                    indexOrigin = i;
                }
                // on récupère l'URL
                else if(    ucd.equalsIgnoreCase(SIAP_URL)
                         // seulement si on n'a pas déja trouvé l'URL !
                		 || ( ucd.equalsIgnoreCase(SIAP_URL2 ) && indexLocation==-1 )
                		 // SSA >= 1.0 (on gère aussi SSA<1.0 car on enlève la partie "SSA." dans stripNSForUtype
                		 || strippedUtype.equalsIgnoreCase(UTYPE_ACREF_SSA) ) {
                     indexLocation = i;
                }

                // pour les paresseux non respectueux du standard qui ne mettent pas l'UCD SIAP_URL
                else if( indexLocation==-1 && (desc[i].getName().toLowerCase().startsWith("url") || desc[i].getId().toLowerCase().startsWith("url")) ) {
                	Aladin.trace(3, "Using field starting with 'url' for location, possible misrespect of SIAP standard");
                	indexLocation = i;
                }
                // on récupère l'index du scale
                else if( ucd.equalsIgnoreCase(SIAP_SCALE) ) {
                    indexScale = i;
                    scale = expla[i];
                }
                else if( ucd.equalsIgnoreCase(SIAP_NAXIS) ) {
                    indexNaxis = i;
                    naxis = expla[i];
                }
                // recherche de l'index pour format de l'image (FITS, JPEG) (pour bidouille Aladin)
                else if( ucd.equalsIgnoreCase(SIAP_IMAGE_FORMAT) ) {
                    indexImgFormat = i;
                    imgFormat = expla[i];
                }
                // recherche du type de ressource spectre
                else if( desc[i].getId().equalsIgnoreCase(FORMAT) || desc[i].getName().equalsIgnoreCase(FORMAT) ) {
                    resType = expla[i];
                }

                // pour SSA version ESAC !!
                else if( ucd.equalsIgnoreCase(ESAC_AXES) ) {
                	ssaAxes = expla[i];
                }
				else if( ucd.equalsIgnoreCase(ESAC_UNITS) ) {
					ssaUnits = expla[i];
				}
				else if( ucd.equalsIgnoreCase(ESAC_DIMEQ) ) {
					ssaDimeq = expla[i];
				}
				else if( ucd.equalsIgnoreCase(ESAC_SCALEQ) ) {
					ssaScaleq = expla[i];
				}

                // STC region
				else if (descId[i].equals("regionSTCS") || descStr[i].equals("stcs") || descStr[i].equals("position_bounds") || utypes[i].equals(UTYPE_STCS_REGION1)) {
				    stcRegion = expla[i];
				}

                // récupération de la couleur (filtre) (pour bidouille Aladin)
                else if( ucd.equalsIgnoreCase(SIAP_BANDPASS_ID) ) {
                    color = expla[i];
                }
                // récupération du survey (pour bidouille Aladin)
                else if( desc[i].getId().equalsIgnoreCase("ObservingProgram") ) {
                    survey = expla[i];
                }
                // récupération du numéro de plaque (pour bidouille Aladin)
                else if( desc[i].getId().equalsIgnoreCase("Machine_Name") ) {
                    machine = expla[i];
                }
                // récupération résolution (pour bidouille Aladin)
                else if( desc[i].getId().equalsIgnoreCase("Resolution") ) {
                    resol = expla[i];
                }
                // récupération du numéro de plaque
                else if( desc[i].getId().equalsIgnoreCase("PlateNumber") ) {
                	plateNumber = expla[i];
            	}
            	// récupération de cutout
            	else if( desc[i].getId().equalsIgnoreCase("Cutout") ) {
            	    //cutout = expla[i].equals(CUTOUT);
                    cutout = expla[i].equals("CUTOUT");
            	}

                /*
                // recuperation de pixSize
                if( descId[i].equals("PixelSize") ) pixSize = new Double(expla[i]).doubleValue();

                if(descId[i].equals("Size_alpha")) xVal = new Double(expla[i]).doubleValue();
                else if(descId[i].equals("Size_delta")) yVal = new Double(expla[i]).doubleValue();
                else if(descId[i].equals("alpha")) {
                    alphaVal = new Double(expla[i]).doubleValue();
                    indexRA = i;
                }
                else if(descId[i].equals("delta")) {
                    deltaVal = new Double(expla[i]).doubleValue();
                    indexDE = i;
                }
                */
                else if(descId[i].equals("AP") || descId[i].equals("PA") || descStr[i].equals("Position Angle")) {
                	idxAngleVal = i;
                	angleVal = new Double(expla[i]).doubleValue();
                }


            }
            catch(Exception e) {}
            finally {
                // ajout de l'unité
                // (ce test sur desc.length est obligatoire car parfois on a trop de TD)
                if( i<desc.length && i!=indexSpatialLocation && i!=indexRA && i!=indexDE && i!=indexLocation && i!=index ) {
                    unit = desc[i].getUnit();
                    if( expla[i].length()>0 && unit.length()>0 ) {
                        //expla[i] += " "+unit;
                        expla[i] = getUnit(expla[i],unit);
                    }
                }
            }
        } // fin de la boucle sur tous les expla


        // on transforme RA et DE de degres decimaux a degres sexagesimaux (pour l'affichage)
        if( (indexRA>=0 && indexDE>=0) || indexSpatialLocation>=0 ) {
            try {
                if( indexSpatialLocation>=0 ) frame.set(expla[indexSpatialLocation]);
                else frame.set(expla[indexRA]+" "+expla[indexDE]);
                frame.setPrecision(Astrocoo.ARCSEC+1);
                String coord = frame.toString(":");
                int beginDE = coord.indexOf("+");
                if( beginDE==-1 ) beginDE= coord.indexOf("-");
                if( indexSpatialLocation>=0 ) {
                	expla[indexSpatialLocation] = coord;
                }
                else {
                	expla[indexRA] = coord.substring(0,beginDE);
                	expla[indexDE] = coord.substring(beginDE);
                }
            }
            catch( Exception e) {e.printStackTrace();}
        }
		myIndexRA = indexRA;
		myIndexDE = indexDE;

        ResourceNode node = new ResourceNode(aladin);
        node.type = ResourceNode.IMAGE;
        node.server = this.server;
        if( objet!=null ) node.objet = objet;
        node.description = descStr;

		node.ra = indexRA;
		node.de = indexDE;

        // provenance de l'image
        if( indexOrigin>=0 ) {
            node.origin = expla[indexOrigin];
        }

        // on n'affiche pas les champs cachés
        for( int i=0; i<descStr.length; i++ ) {
//            if( desc[i].getType().equals("hidden") ) expla[i] = "";
        	if( desc[i].getType().equals("hidden") ) hidden[i] = true;
        }

        node.explanation = expla;
        node.hidden = hidden;
        node.originalExpla = originalExpla;
        node.ucds = ucds;
        node.utypes = utypes;
        node.allUnits = allUnits;
        node.bandPass = color; // pour bidouille aladin
        node.survey = survey; // pour bidouille aladin
        node.machine = machine; // pour bidouille aladin
        node.resol = resol; // pour bidouille aladin
        node.plateNumber = plateNumber; // pour bidouille Aladin
        node.isLeaf = true;
        if( index>=0 ) {
            node.name = expla[index];
        }

        // si on n'a pas pu récupérer le nom du filtre, on essaie de le deviner à partir du titre
        if( color==null ) {
        	// pour le moment, on ne le fait que pour Sloan
        	if( node.server!=null && node.server.aladinLabel.indexOf("SDSS")>=0 ) {
        		node.bandPass = guessBandIDFromTitle(node.name);
        	}
        }

        // on ne le fait pas pour le moment
        //node.cutout = cutout;
        // BIDOUILLE A CAUSE DES VUES LOW & FULL
        if( cutout ) {
            // ce passage par resolveTarget est AFFREUX
            String target = server.tree.resolveTarget(server.getTarget(), aladin);
            String coord=null;
            try {
                frame.set(target);
            	frame.setPrecision(Astrocoo.ARCSEC+1);

            	//coord = TreeView.getDeciCoord(frame.toString(":"));
                coord = TreeView.getDeciCoord(target).trim();
            }
            catch(Exception e) {}
            //System.out.println(coord);
            if( coord!=null ) {
            	int beginDE = coord.indexOf("+");
            	if( beginDE==-1 ) beginDE= coord.indexOf("-");
            	// forçage des valeurs alphaVal et deltaVal
            	alphaVal = new Double(coord.substring(0,beginDE)).doubleValue();
            	deltaVal = new Double(coord.substring(beginDE)).doubleValue();
            	//expla[indexRA] = coord.substring(0,beginDE);
            	//expla[indexDE] = coord.substring(beginDE);
            }
        }
		// si cutout, on set le target cutout à la target de la zone disponible lors de la création d'un noeud
        if( node.cutout /*&& server!=null*/ ) {
        	//node.setCutoutTarget(server.getTarget(),false);
        	if( myIndexRA>=0 && myIndexDE>=0 )
        		//node.setCutoutTarget(node.explanation[myIndexRA]+" "+node.explanation[myIndexDE],false);
                if( targetObjet!=null ) {
                    node.setCutoutTarget(targetObjet);
                    node.targetObjet = targetObjet;
                }
        }
        // bidouille pour Aladin
        if( indexLocation>=0 /*&& (! (server instanceof AladinServer) || !Aladin.test)*/ ) {
            node.location = expla[indexLocation];
            //System.out.println("node has location "+node.location);
        }


        if( resType==null ) {
        	resType = node.getFieldValFromUtype("Access.Format");
        }

        // ajout pour Pierre : SIAP pour ConeSearch !
        if( resType!=null && resType.equalsIgnoreCase("catalog") ) {
        	node.type = ResourceNode.CAT;
        }
        // bidouille pour SAADA (workshop DCA)
        else if( resType!=null && resType.toLowerCase().startsWith("catalog")) {
        	node.type = ResourceNode.CAT;
        }
        // pour récupérer le fait que c'est un spectre !
        else if( (resType!=null && resType.startsWith("spectrum")) || type==SSA ) {
            node.type = ResourceNode.SPECTRUM;
            node.format = resType;
        }


        // SSA à la sauce ESAC
        if( ssaAxes!=null ) node.axes = split(ssaAxes, " ");
		if( ssaUnits!=null ) node.units = split(ssaUnits, " ");
		if( ssaDimeq!=null ) node.dimeq = split(ssaDimeq, " ");
		if( ssaScaleq!=null ) node.scaleq = split(ssaScaleq, " ");

        //xVal=0;
        //yVal=0;
        // on va calculer la taille de l'image
        if( indexScale>=0 && indexNaxis>=0 ) {
            String[] imScale = split(scale,", ");
            String[] format = split(naxis,", ");
            // patch pour le service SIAP SDSS : le scale n'est pas (toujours) indiqué pour chaque axe
            if( imScale.length==1 ) {
                String[] tmp = new String[2];
                tmp[0] = imScale[0];
                tmp[1] = tmp[0];
                imScale = tmp;
            }
            if( format.length>=2 && imScale.length>=2 ) {
                try {
                	xVal = Math.abs(new Double(imScale[0]).doubleValue() * new Double(format[0]).doubleValue());
            		yVal = Math.abs(new Double(imScale[1]).doubleValue() * new Double(format[1]).doubleValue());
                }
                catch(NumberFormatException e) {}
            }

            String unitScale = desc[indexScale].getUnit();

            if( unitScale!=null && unitScale.length()>0 && imScale.length>1 ) {
            	expla[indexScale] = getUnit(imScale[0],unitScale)+"  "+getUnit(imScale[1],unitScale);
                // taille du pixel
                node.setPixSize(getUnit(imScale[0],unitScale)+"/pix");
                try {
                    double p = new Double(imScale[0]).doubleValue();
                    p = toDegrees(unitScale, p);
                    node.setPixSizeDeg(p);
                }
                catch(Exception e) {}

                // NEW for AVO
				if( format.length>=2 && imScale.length>=2 ) {
					try {
						double p1 = toDegrees(unitScale, new Double(imScale[0]).doubleValue());
						double p2 = toDegrees(unitScale, new Double(imScale[1]).doubleValue());
						xVal = Math.abs(p1 * new Double(format[0]).doubleValue());
						yVal = Math.abs(p2 * new Double(format[1]).doubleValue());
					}
					catch(Exception e) {}
				}
				// END FOR NEW for AVO


            }
//            System.out.println(xVal);
//            System.out.println(yVal);
//            System.out.println(alphaVal);
//            System.out.println(deltaVal);

        }

        // format de l'image (FITS,JPEG)
        if( indexImgFormat>=0 ) {
            node.formats = split(imgFormat,",");
            // par défaut, le format est le premier
            node.curFormat = node.formats[0];
        }


        // calcul de l'angle de position avec matrice CD (Thomas, 23/09/2005)
        if( idxAngleVal==-1 ) { // si on n'a pas récupéré l'angle d'une autre façon
        	// selon spécif. SIAP, l'UCD pour CD matrix est "VOX:WCS_CDMatrix"
        	String cd = node.getFieldValFromUcd("VOX:WCS_CDMatrix");
        	if( cd!=null ) {
        		boolean computeAngle = true;
        		String[] cdVal = split(cd, " ,");
        		// selon spécif. SIAP, les éléments de la matrice apparaissent dans cet ordre:
        		// CD[i,j] = [0,0], [0,1], [1,0], [1,1]
        		double cd01, cd11;
        		cd01 = cd11 = 0.;
        		try {
        			cd01 = Double.valueOf(cdVal[1]).doubleValue();
        			cd11 = Double.valueOf(cdVal[3]).doubleValue();
        		}
        		catch( Exception e ) {computeAngle = false;}

        		if( computeAngle ) {
//        			Aladin.trace(3, "Computing position angle from WCS matrix");
        			angleVal = Math.atan2(cd01, cd11)*180.0/Math.PI;
        			if (Double.isNaN(angleVal)) {
        			    angleVal = 0;
        			}
        		}
        	}
        }

        setDistanceToCenter(node, alphaVal, deltaVal);

        // Attention : cette méthode doit absolument être appelé APRES l'acquisition de angleVal
        createFov(node, stcRegion);


        // pour permettre le tri par champ
        setProperties(node);



        return node;
    }

    /** set the distance field of a node, ie set the distance to the requested position */
    private void setDistanceToCenter(ResourceNode node, double ra, double dec) {
        if( targetObjetCoo!=null && !Double.isNaN(ra) && !Double.isNaN(dec)) {
            double dist = Coo.distance(ra, dec, targetObjetCoo.al, targetObjetCoo.del);
            node.setDistanceToCenter(dist);
        }
    }

    // tente de retrouver le nom de la bande photomŽtrique d'un enregistrement SIAP
    // ˆ partir du titre de l'enregistrement
    // Fonctionne pour le SIAP SDSS, le titre est de la forme : Sloan Digital Sky Survey - Filter u
    private String guessBandIDFromTitle(String title) {
    	int idx = title.lastIndexOf('-');
    	return (idx!=-1 && idx+1<title.length())?title.substring(idx+1).trim():null;
    }

    // Retourne le résultat d'un tokenizer sous forme de tableau
    static protected String[] split(String str,String sep) {

        StringTokenizer st = new StringTokenizer(str,sep);
        String[] ret = new String[st.countTokens()];
        int i=0;
        while( st.hasMoreTokens() ) {
            ret[i] = st.nextToken();
            i++;
        }
        return ret;
    }

    /** Remove the ![CDATA[...]] part around the URL string */
    /*
    private String skipCDATA(String s) {
    	s = s.trim();
    	if( s.startsWith("![CDATA[")) s = s.substring(8);
    	if( s.endsWith("]]>")) s = s.substring(0,s.length()-3);
    	return s;
    }
    */

    /* ---- FIN Méthodes pour constuire l'arbre partant d'un fichier SIAP ---- */

	/* ---- Méthodes pour la construction de l'arbre à partir d'un fichier VOTABLE_IDHA ---- */

    /** Construit l'arbre des images a partir du votable */
    private ResourceNode buildVotable() {
        if( savotParser==null ) {
        	return null;
        }

        SavotVOTable vot = savotParser.getVOTable();

        ResourceSet rootResSet = vot.getResources();
        // noeud racine
        ResourceNode root = new ResourceNode(aladin, "root");
        root.type = ResourceNode.VOID;


        // on lance le processus : appel de processResource pour tous les RESOURCE racines
        for( int i=0; i<rootResSet.getItemCount(); i++ ) {
            processResource(rootResSet,i,root);
        }
        return root;
    }



	/** fully process a resource which is an Observation_Group in the IDHA terminology
	 *
	 * @param sr - the resource being an Observation_group
	 * @param parent - the parent node
	 */
	private void processObsGroup(SavotResource sr, ResourceNode parent) {

	    TRSet trSet = sr.getTRSet(0);
	    int nbTR = trSet.getItemCount();
	    String[] criteria = new String[nbTR];
	    String[] value = new String[nbTR];
	    // map nom critère --> valeur
        Hashtable<String, String> critVal = new Hashtable<String, String>();
        // map nom critère --> SavotResource avec infos
        Hashtable<String, SavotResource> infoVal = new Hashtable<String, SavotResource>();
        SavotResource storageMapping = null;
        SavotResource storedImage = null;
        SavotResource processedObs = null;

	    TDSet tdSet;
	    String epoch=null;
        boolean resetWavelength=true;
	    // on récupère les couples "nom critère" <-> "valeur"

	    for( int i=0; i<nbTR; i++ ) {
            tdSet = trSet.getTDSet(i);

	        criteria[i] = tdSet.getContent(indexCriteria).replace('"',' ').trim().toLowerCase();
	        value[i] = tdSet.getContent(indexValue);
	        critVal.put(criteria[i],value[i]);
	        // récupération de currentColor
	        if( criteria[i].equals("filter") ) {
                if( value[i].equals(currentColor)) resetWavelength=false;
	            currentColor = value[i];
	        }
	        // récupération de l'epoch
            if( criteria[i].equals("epoch") ) {
                epoch = value[i];
            }
	        //System.out.println("on récupère le couple "+criteria[i]+"  "+value[i]);
	    }

        // RESET currentWavelength
        if( resetWavelength ) {
            currentWavelength = null;
            currentFilterName = null;
        }

	    // ajout des critères dispos dans parent si nécessaire
	    if( parent.sortCriteria==null ) {
	        parent.sortCriteria = criteria;
	    }

	    // on traite les resources se trouvant dans sr
	    ResourceSet set = sr.getResources();
	    SavotResource curRes;
	    for( int i=0; i<set.getItemCount() ; i++) {
	        curRes = set.getItemAt(i);
	        //System.out.println("On s'occupe de : "+curRes.getName());
	        // on arrive sur les images
	        if( curRes.getName().equalsIgnoreCase(IDIMAGE) ) {
	            processedObs = curRes;
	        }
	        else if( curRes.getName().equalsIgnoreCase(IDMAPPING) ) {
	            storageMapping = curRes;
	        }
	        else if( curRes.getName().equalsIgnoreCase(IDSTORED) ){
	            storedImage = curRes;
	        }
	        // sinon, il faut avoir un map conservant les descriptions etc
	        // pour pouvoir remplir desc et expla
	        else {
	            infoVal.put(curRes.getName().toLowerCase(), curRes);
	        }
	    }

	    // toutes les sous-resource ont été récupérées, on va pouvoir faire joujou avec

	    // création d'un tableau de ResourceNode(penser au storage mapping)
	    // ATTENTION : que faire si processedObs est null ?
	    TRSet obsSet = processedObs.getData(0).getTableData().getTRs();
	    ResourceNode[] resTab = new ResourceNode[obsSet.getItemCount()];
        // fais la correspondance index dans resTab --> noeud
        Hashtable indexToNode = new Hashtable();
        // sauvegarde des valeurs xVal et yVal, alphaVal, deltaVal, angleVal
        double[] xValSave = new double[resTab.length];
        double[] yValSave = new double[resTab.length];
        double[][] xValTabSave = new double[resTab.length][];
        double[][] yValTabSave = new double[resTab.length][];
        double[] alphaValSave = new double[resTab.length];
        double[] deltaValSave = new double[resTab.length];
        double[] angleValSave = new double[resTab.length];

        if( imageFieldSet==null) {
            imageFieldSet = processedObs.getFieldSet(0);
        }
        SavotField[] desc = getDescription(processedObs);
        //System.out.println(desc.length);
        TRSet trMapping = storageMapping.getData(0).getTableData().getTRs();
        if(mappingFieldSet==null) {
            mappingFieldSet = storageMapping.getFieldSet(0);
        }
        SavotField[] descMapping = getDescription(storageMapping);

        TRSet trStored = storedImage.getData(0).getTableData().getTRs();
        if(storedFieldSet==null) {
            storedFieldSet = storedImage.getFieldSet(0);
        }
        SavotField[] descStored = getDescription(storedImage);

        // première étape: on récupère toutes les observations
	    for( int i=0; i<resTab.length; i++ ) {
            resTab[i] = createNode(obsSet.getItemAt(i), 0, desc, IDIMAGE);
            resTab[i].criteriaVal = critVal;
            //father.addChild(newNode);
            xValSave[i] = xVal;
            // TOTO
            yValSave[i] = yVal;
            xValTabSave[i] = xValTab;
            yValTabSave[i] = yValTab;
            alphaValSave[i] = alphaVal;
            deltaValSave[i] = deltaVal;
            angleValSave[i] = angleVal;

			angleVal = alphaVal = deltaVal = xVal = yVal = Double.NaN;
			xValTab = yValTab = null;

            // survey et color sont pour le moment nécessaires pour pouvoir charger les images
            resTab[i].survey = currentSurvey;
            resTab[i].bandPass = currentColor;

            // epoch pour les labels des plans
            resTab[i].epoch = epoch;

            // pixel size
            if( pixSize>0 ) {
                resTab[i].setPixSize(getUnit(Double.toString(pixSize),pixSizeUnit)+"/pix");
                try {
                    double p = toDegrees(pixSizeUnit, pixSize);
                    resTab[i].setPixSizeDeg(p);
                }
                catch(Exception e) {}
             }
            indexToNode.put(resTab[i].name, new Integer(i));
        }

        int idxNameColumn = findFieldByID("Observation_Name", descMapping);
        // Traitement du Storage Mapping
        if( storageMapping!=null && idxNameColumn>=0 ) {
            // noeud en dessous de l'observation
            ResourceNode subObs, fatherSubObs;
            // boucle sur les lignes de StorageMapping
            for( int i=0; i<trMapping.getItemCount(); i++ ) {
                TDSet tds = trMapping.getItemAt(i).getTDs();
                String mapName = tds.getItemAt(idxNameColumn).getContent();
                Integer indexFather = (Integer)indexToNode.get(mapName);
                //if( indexFather==null ) continue;
                if( indexFather==null ) indexFather = new Integer(i);
                int intIndexFather = indexFather.intValue();
                fatherSubObs = resTab[intIndexFather];
                if( fatherSubObs==null ) continue;
                subObs = new ResourceNode(aladin);
                subObs.type = fatherSubObs.type;
                subObs.survey = fatherSubObs.survey;
                subObs.origin = fatherSubObs.origin;
                subObs.isLeaf = true;
                fatherSubObs.addChild(subObs);
                SavotTD td;
                // boucle sur les TD
                for( int j=0; j<tds.getItemCount(); j++ ) {
                    td = tds.getItemAt(j);
                    String descStr = descMapping[j].getId();
                    if( descStr.length()==0 ) descStr = descMapping[j].getName();

                    if( descStr.equalsIgnoreCase("Cutout") ) {
                        subObs.name = td.getContent();
                        if( td.getContent().equals(CUTOUT) ) {
                            //System.out.println("c est un cutout");
                            subObs.cutout = true;
                            // si cutout, on set le target cutout à la target de la zone disponible lors de la création d'un noeud
                            if( myIndexRA>=0 && myIndexDE>=0 ) {
								//resTab[i].setCutoutTarget(resTab[i].explanation[myIndexRA]+" "+resTab[i].explanation[myIndexDE],false);
                                if( targetObjet!=null ) {
                                    subObs.setCutoutTarget(targetObjet);
                                    subObs.targetObjet = targetObjet;
                                }

                            }
                        }
                    }
                    // on récupère la description d'une sous-observation
                    else if( descStr.equalsIgnoreCase("desc") ) {
                        subObs.desc = td.getContent();
                    }
                    // on récupère le mode d'indexation
                    else if( descStr.equalsIgnoreCase(INDEXING) ) {
                        subObs.indexing = td.getContent();
                    }
                    else if( descStr.equalsIgnoreCase(MAX_SIZE) ) {
                        try {
                            maxSize = new Double(td.getContent()).doubleValue();
                        }
                        catch( NumberFormatException e) {maxSize = -1.0;}
                    }
                    else if( descStr.equals(RESOLUTION) ) {
                        // à traiter
                        subObs.resol = td.getContent();
                    }
                    else if( descStr.equals(NB_OF_PATCHES) ) {
                        String maxImg = td.getContent();
                        if( maxImg.length()>0 ) subObs.maxImgNumber = maxImg;
                    }
                    else if( descStr.equals(MAPPARAM) ) {
                        String content = td.getContent();
                        if( content.trim().length()>0 ) {
                            String[] params = split(content,",");
                            if( params.length==2 ) {
                                try {
                                    subObs.beginVel = Double.valueOf(params[0]).doubleValue();
                                    subObs.velStep = Double.valueOf(params[1]).doubleValue();
                                }
                                catch( NumberFormatException e ) {}
                            }
                        }
                    }
                } // fin boucle sur TD

                // restore xVal and yVal values
                xVal = xValSave[intIndexFather];
                yVal = yValSave[intIndexFather];
                xValTab = xValTabSave[intIndexFather];
                yValTab = yValTabSave[intIndexFather];
                alphaVal = alphaValSave[intIndexFather];
                deltaVal = deltaValSave[intIndexFather];
                angleVal = angleValSave[intIndexFather];
                pixSize = fatherSubObs.getPixSizeDeg();

                createFov(subObs, null);
                maxSize = -1.0;
            }
        } // fin Traitement Storage Mapping
        // ensemble des noeuds observationnels terminaux
        ResourceNode[] terminalObs = new ResourceNode[trMapping.getItemCount()];
        int idxTerminal=0;
        // on parcourt resTab : les élts avec un seul fils le perdent et on réintègre les infos du fils
        for( int i=0; i<resTab.length; i++ ) {
            ResourceNode curSubObs;
            if( resTab[i].nbChildren==1 ) {
                curSubObs = (ResourceNode)resTab[i].getChildrenAt(0);
                resTab[i].removeAllChild();
                resTab[i].cutout = curSubObs.cutout;
                resTab[i].targetObjet = curSubObs.targetObjet;
                resTab[i].setCutoutTarget(curSubObs.targetObjet);
				// NEW AVO
				if ( resTab[i].cutout && resTab[i].name!=null && resTab[i].name.indexOf("CDF-SOUTH")>=0 ) {
					//System.out.println("DONE");
					//System.out.println(resTab[i].explanation[myIndexRA]);
					resTab[i].setCutoutTarget(resTab[i].explanation[myIndexRA]+" "+resTab[i].explanation[myIndexDE],false);
				}
				// FIN NEW AVO
                resTab[i].resol = curSubObs.resol;
                resTab[i].setFov(curSubObs.getFov());

                resTab[i].isLeaf = true;
                terminalObs[idxTerminal++] = resTab[i];
            }
            else if( resTab[i].nbChildren>1 ) {
                Enumeration e = resTab[i].getChildren();
                while( e.hasMoreElements() ) {
                    terminalObs[idxTerminal++] = (ResourceNode)e.nextElement();
                }
            }
        }

        // pour traitement LOWERLEVEL
        Hashtable lowerLevelNodes = new Hashtable();
        // ici traitement du nouvel élément de François
        // Traitement de StoredImage
        if( storedImage!=null ) {
            ResourceNode curNode;
            // boucle sur les lignes de StoredImage
            for( int i=0; i<trStored.getItemCount(); i++ ) {
                curNode = terminalObs[i];
                if( curNode==null ) continue;

                TDSet tds = trStored.getItemAt(i).getTDs();
                SavotTD td;

                for( int j=0; j<tds.getItemCount(); j++ ) {
                    td = tds.getItemAt(j);

                    String descStr = descStored[j].getId();
                    if( descStr.length()==0 ) descStr = descStored[j].getName();

                    // récupération de la location
                    if( descStr.equalsIgnoreCase(LOCATION) ) {
                        String location = td.getContent();
                        if( location!=null && location.length()>0 )
                            curNode.location = location;
                    }
                    if( descStr.equalsIgnoreCase(GLULINK) ) {
                        String gluLink = td.getContent();
                        if( gluLink!=null && gluLink.trim().length()>0 ) {
                            curNode.gluLink = gluLink;
                        }
                    }
                }

                // pour traitement LOWERLEVEL
                if( curNode.name.startsWith("LOWERLEVEL") ) {
                    curNode.isLeaf = false;
                    // attention, actuellement la clé est location
                    lowerLevelNodes.put(curNode.location, curNode);
                }
            }
            // pour traitement LOWERLEVEL
            if( lowerLevelNodes.size()>0 ) {
                //SavotResource lowLevRes = (SavotResource)storedImage.getResources().getItemAt(0);
                //SavotResource lowLevRes = (SavotResource)storedImage.getResources().getItemAt(0);
                processLowerLevel(lowerLevelNodes, storedImage, desc);
            }
        }

     // fin if
/////////////////////////
/*
            TDSet tds = ((SavotTR)trMapping.getItemAt(i)).getTDs();
            SavotTD td;
                for( int j=0; j<tds.getItemCount(); j++ ) {
                	td = (SavotTD)tds.getItemAt(j);

                    String descStr = descMapping[j].getId();
                    if( descStr.length()==0 ) descStr = descMapping[j].getName();
                    String ref = descMapping[j].getRef();

                    if( descStr.equalsIgnoreCase("Cutout") && td.getContent().equals(CUTOUT) ) {
                        //System.out.println("c est un cutout");
                    	resTab[i].cutout = true;
                        // si cutout, on set le target cutout à la target de la zone disponible lors de la création d'un noeud
                        if( myIndexRA>=0 && myIndexDE>=0 ){
                        	//resTab[i].setCutoutTarget(resTab[i].explanation[myIndexRA]+" "+resTab[i].explanation[myIndexDE],false);
                            if( targetObjet!=null ) {
                                resTab[i].setCutoutTarget(targetObjet);
                                resTab[i].targetObjet = targetObjet;
                            }
                        }
                    }
                    else if( descStr.equalsIgnoreCase(MAX_SIZE) ) {
                        try {
                            maxSize = new Double(td.getContent()).doubleValue();
                        }
                        catch( NumberFormatException e) {maxSize = -1.0;}
                    }
                    else if( descStr.equals(RESOLUTION) ) {
                        // à traiter
                        resTab[i].resol = td.getContent();
                    }
                }
            }

            // ici traitement du nouvel élément de François
            // Traitement de StoredImage
            if( storedImage!=null ) {
                TDSet tds = ((SavotTR)trStored.getItemAt(i)).getTDs();
                SavotTD td;
                for( int j=0; j<tds.getItemCount(); j++ ) {
                    td = (SavotTD)tds.getItemAt(j);

                    String descStr = descStored[j].getId();
                    if( descStr.length()==0 ) descStr = descStored[j].getName();
                    // récupération de la location
                    if( descStr.equalsIgnoreCase(LOCATION) ) {
                        String location = td.getContent();
                        // bidouille car pour le moment, les location ALADIN ne sont pas entièrement renseignées
                        // les 2 conditions de fin serontà supprimer !!
                        if( location!=null && location.length()>0 )
                            //resTab[i].location = URLDecode(location);
                            resTab[i].location = location;
                    }
                    if( descStr.equalsIgnoreCase(GLULINK) ) {
                        String gluLink = td.getContent();
                        if( gluLink!=null && gluLink.trim().length()>0 ) {
                            resTab[i].gluLink = gluLink;
                        }
                    }
                }
            }

            resTab[i].isLeaf = true;
            // survey et color sont pour le moment nécessaires pour pouvoir charger les images
            resTab[i].survey = currentSurvey;
            resTab[i].color = currentColor;
            // epoch pour les labels des plans
            resTab[i].epoch = epoch;
            // pixel size
            if( pixSize>0 ) {
                resTab[i].pixSize = getUnit(Double.toString(pixSize),pixSizeUnit)+"/pix";
                try {
                    double p = toDegrees(pixSizeUnit, pixSize);
                    resTab[i].pixSizeDeg = p;
                }
                catch(Exception e) {}
             }

            createFov(resTab[i]);
            pixSize = -1.;
            //System.out.println(resTab[i].name);
	    }
        */

	    // A ce stade, resTab est rempli de toutes les ResourceNode correspondant aux données
	    // qu'on va refiler à une fonction avec l'ordre de tri
	    // ladite fonction sera réutilisée pour trier correctement (popup)
	    sortAndCreate(resTab, parent, parent.sortCriteria, infoVal);
	}

    /** traitement des elts LOWERLEVEL
     *
     * @param nodes hash des noeuds LOWERLEVEL
     * @param res resource
     */
    private void processLowerLevel(Hashtable lowLevNodes, SavotResource res, SavotField[] desc) {
        SavotResource obsRes = res.getResources().getItemAt(0);
        SavotResource mapRes = res.getResources().getItemAt(1);
        SavotResource storedRes = res.getResources().getItemAt(2);
        TRSet obsSet = obsRes.getData(0).getTableData().getTRs();
        int nbNodes = obsSet.getItemCount();
        SavotTR tr;
        ResourceNode curNode, father;
        Hashtable nodes = new Hashtable();
        for( int i=0; i<nbNodes; i++ ) {
            tr = obsSet.getItemAt(i);
            curNode = createNode(tr, 0, desc, IDIMAGE);
            //System.out.println(curNode.name);
            father = (ResourceNode)lowLevNodes.get(curNode.name);
            if( father!=null ) {
                curNode.name = curNode.refNumber;
                curNode.isLeaf = true;
                father.addChild(curNode);
                nodes.put(curNode.name, curNode);
                createFov(curNode, null);
            }
            //System.out.println(curNode.refNumber);
        }


        // Traitement du Storage Mapping
        SavotField[] descMapping = getDescription(mapRes);
        TRSet trMapping = mapRes.getData(0).getTableData().getTRs();
        int idxNameColumn = findFieldByID("Observation_Name", descMapping);
        if( type==SIAP_EVOL ) {
            idxNameColumn = findFieldByID(nameSiapEvolStr, descMapping);
        }

        if( mapRes!=null && idxNameColumn>=0 ) {
            // noeud en dessous de l'observation
            ResourceNode curObs;
            // boucle sur les lignes de StorageMapping
            for( int i=0; i<trMapping.getItemCount(); i++ ) {
                TDSet tds = trMapping.getItemAt(i).getTDs();
                String mapName = tds.getItemAt(idxNameColumn).getContent();
                curObs = (ResourceNode)nodes.get(mapName);
                if( curObs==null ) continue;
                /*
                Integer indexFather = (Integer)indexToNode.get(mapName);
                //if( indexFather==null ) continue;
                if( indexFather==null ) indexFather = new Integer(i);
                int intIndexFather = indexFather.intValue();
                fatherSubObs = resTab[intIndexFather];
                if( fatherSubObs==null ) continue;
                subObs = new ResourceNode();
                subObs.type = fatherSubObs.type;
                subObs.survey = fatherSubObs.survey;
                subObs.origin = fatherSubObs.origin;
                subObs.isLeaf = true;
                fatherSubObs.addChild(subObs);
                */
                SavotTD td;
                // boucle sur les TD
                for( int j=0; j<tds.getItemCount(); j++ ) {
                    td = tds.getItemAt(j);
                    String descStr = descMapping[j].getId();
                    if( descStr.length()==0 ) descStr = descMapping[j].getName();
                    /*
                    if( descStr.equalsIgnoreCase("Cutout") ) {
                        curObs.name = td.getContent();
                        if( td.getContent().equals(CUTOUT) ) {
                            //System.out.println("c est un cutout");
                            curObs.cutout = true;
                            // si cutout, on set le target cutout à la target de la zone disponible lors de la création d'un noeud
                            if( myIndexRA>=0 && myIndexDE>=0 ) {
                                //resTab[i].setCutoutTarget(resTab[i].explanation[myIndexRA]+" "+resTab[i].explanation[myIndexDE],false);
                                if( targetObjet!=null ) {
                                    curObs.setCutoutTarget(targetObjet);
                                    curObs.targetObjet = targetObjet;
                                }

                            }
                        }
                    }
                    */
                    // on récupère la description d'une sous-observation
                    else if( descStr.equalsIgnoreCase("desc") ) {
                        curObs.desc = td.getContent();
                    }
                    // on récupère le mode d'indexation
                    else if( descStr.equalsIgnoreCase(INDEXING) ) {
                        curObs.indexing = td.getContent();
                    }
                    else if( descStr.equalsIgnoreCase(MAX_SIZE) ) {
                        try {
                            maxSize = new Double(td.getContent()).doubleValue();
                        }
                        catch( NumberFormatException e) {maxSize = -1.0;}
                    }
                    else if( descStr.equals(RESOLUTION) ) {
                        // à traiter
                        curObs.resol = td.getContent();
                    }
                    else if( descStr.equals(NB_OF_PATCHES) ) {
                        String maxImg = td.getContent();
                        if( maxImg.length()>0 ) curObs.maxImgNumber = maxImg;
                    }
                    else if( descStr.equals(MAPPARAM) ) {
                        String content = td.getContent();
                        if( content.trim().length()>0 ) {
                            String[] params = split(content,",");
                            if( params.length==2 ) {
                                try {
                                    curObs.beginVel = Double.valueOf(params[0]).doubleValue();
                                    curObs.velStep = Double.valueOf(params[1]).doubleValue();
                                }
                                catch( NumberFormatException e ) {}
                            }
                        }
                    }
                } // fin boucle sur TD
/*
                // restore xVal and yVal values
                xVal = xValSave[intIndexFather];
                yVal = yValSave[intIndexFather];
                alphaVal = alphaValSave[intIndexFather];
                deltaVal = deltaValSave[intIndexFather];
                angleVal = angleValSave[intIndexFather];
                pixSize = fatherSubObs.pixSizeDeg;
  */
                //createFov(curObs);
                //System.out.println(curObs);
                maxSize = -1.0;
            }
        } // fin Traitement Storage Mapping



        // Traitement de StoredImage
        if( storedRes!=null ) {
            TRSet trStored = storedRes.getData(0).getTableData().getTRs();
            SavotField[] descStored = getDescription(storedRes);
            // boucle sur les elts de nodes
//            Enumeration e = nodes.elements();
            for( int i=0; i<trStored.getItemCount(); i++ ) {


                TDSet tds = trStored.getItemAt(i).getTDs();
                SavotTD td;

                // indispensable pour retrouver les bonnes locations !!
                String mapName = tds.getItemAt(idxNameColumn).getContent();
                curNode = (ResourceNode)nodes.get(mapName);
                if( curNode==null ) continue;

                for( int j=0; j<tds.getItemCount(); j++ ) {
                    td = tds.getItemAt(j);

                    String descStr = descStored[j].getId();
                    if( descStr.length()==0 ) descStr = descStored[j].getName();

                    // récupération de la location
                    if( descStr.equalsIgnoreCase(LOCATION) ) {
                        String location = td.getContent();
                        if( location!=null && location.length()>0 )
                            curNode.location = location;
//                            System.out.println(curNode.name + " " + location);
                    }
                    if( descStr.equalsIgnoreCase(GLULINK) ) {
                        String gluLink = td.getContent();
                        if( gluLink!=null && gluLink.trim().length()>0 ) {
                            curNode.gluLink = gluLink;
                        }
                    }
                }
            }
        }
        // Fin traitement StoredImage

    }

	protected int findFieldByUtype(String utype, SavotField[] fields) {
	    for( int i=0; i<fields.length; i++ ) {
	        if( fields[i].getUtype().equalsIgnoreCase(utype) ) return i;
	    }
	    return -1;
	}


    protected int findFieldByID(String id, SavotField[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getId().equalsIgnoreCase(id)
                    || fields[i].getRef().equalsIgnoreCase(id))
                return i;
        }
        return -1;
    }

    protected int findValIndex(String[] tab, String val) {
        for (int i = 0; i < tab.length; i++) {
            if (tab[i].equals(val))
                return i;
        }
        return -1;
    }

    protected int findFieldByUCD(String ucd, SavotField[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if( fields[i].getUcd().equalsIgnoreCase(ucd) ) {
                return i;
            }
        }
        return -1;
    }

	/** fait le boulot de URLDecoder.decode() qui n'existe pas en 1.1 */
    public static String URLDecode(String encoded)
      {
        StringBuffer decoded = new StringBuffer();
        int i=0;
        String charCode;
        char currentChar, decodedChar;
        while(i < encoded.length())
        {
          currentChar = encoded.charAt(i);
          //'+' becomes 'space'
          if (currentChar == '+')
          {
            decoded.append(" ");
            i = i + 1;
          }
          else if (currentChar == '%')
          {
            charCode = encoded.substring(i+1, i+3);
            decodedChar = (char)Integer.parseInt(charCode, 16);
            decoded.append(decodedChar);
            i = i + 3;
          }
          //common case 'a' to 'z' ...
          else
          {
            decoded.append(currentChar);
            i = i + 1;
          }
        }
        return(decoded.toString());
      }



	/** crée les noeuds nécessaires pour placer les feuilles contenus dans tab dans l'ordre défini dans criteria
	 *
	 * @param tab
	 * @param parent
	 * @param criteria
	 */
	private void sortAndCreate(ResourceNode[] tab, ResourceNode parent, String[] criteria, Hashtable info) {
	    if( tab.length==0 ) return;
	    // tous les noeuds dans tab sont réputés faire référence au même criteriaVal
	    Hashtable value = tab[0].criteriaVal;
	    //System.out.println(tab[0].name+" thomas");
	    // parent auquel raccrocher le nouveau noeud créé
	    ResourceNode curParent=parent;
        ResourceNode node;
	    String name = null;
	    // on crée les noeuds dans l'ordre imposé par criteria
	    for( int i=0; i<criteria.length; i++ ) {
	        //System.out.println("nom de critère : "+criteria[i]);
	        name = (String)value.get(criteria[i]);
	        //System.out.println("valeur de critère : "+name);
	        node = (ResourceNode)curParent.getChild(name);
	        if( node==null ) {
	            // on récupère la resource contenant des infos sur le critère
	            SavotResource sr = (SavotResource)info.get(criteria[i]);
	            if( sr!=null ) {
                    node = createNode(sr.getData(0).getTableData().getTRs().getItemAt(0),-1,getDescription(sr),criteria[i]);
	            }
	            else {
	                node = new ResourceNode(aladin);
	                node.type = ResourceNode.IMAGE;
	            }

	            // si on veut mettre cela comme nom, il faut changer ce qu'on met dans criteriaVal
	            //node.name = criteria[i]+"."+name;
                node.name = name;
	            node.criteria = criteria[i];
	            node.valueCriteria = name;
	            curParent.addChild(node);

	        }
            // le nouveau parent est le noeud créé ou trouvé
            curParent = node;
	    }

	    curParent.col = getNextColor();

	    // fin de la boucle : on ajoute les membres de tab au dernier noeud créé ou trouvé
	    for( int i=0; i<tab.length; i++ ) {
            curParent.addChild(tab[i]);
	        tab[i].col = curParent.col;
            if( tab[i].getFov()!=null ) tab[i].getFov().color = curParent.col;
            tab[i].wavelength = currentWavelength;
            if( currentFilterName!=null && currentFilterName.length()>0 ) tab[i].wavelengthExpla = currentFilterName;
	    }
	}

    // pour memoriser les filtres ACS deja crees
    Hashtable ACSFilters = new Hashtable();

    /** Recursive function which processes one resource (VOTABLE mode)
     *  @param sr - the SavotRessource being processed
     *  @param father - the node to which the new sub-nodes will be bound to
     */
    private void processResource(ResourceSet set, int indexRes, ResourceNode father) {
        SavotResource sr = set.getItemAt(indexRes);


        // -----------------------------------------------------------
        // cas spécial pour VOTable provenant de Aladin : on a une section SIAP à la fin du IDHA
        // A DECOMMENTER QUAND ON A TROEUVE LE BUG

        if( sr.getType().equals("results") ) {
            //ResourceNode n = new ResourceNode("SIA section");
            //father.addChild(n);
            processSIAPResource(sr,father,true);
            return;
        }
        // -----------------------------------------------------------


        SavotField[] desc = null;
        // on remplit le tableau des descriptions, et on recupere l'index pour le nom
        String id = (sr.getTables().getItemAt(0)).getId();
        String ref = (sr.getTables().getItemAt(0)).getRef();
        //System.out.println("process resource");

        /*System.out.println("id vaut "+id);
        System.out.println("ref vaut "+ref);*/

        if( id.length()!=0 ) {
            if( id.equalsIgnoreCase(IDSURVEY) ) {
                surveyFieldSet = sr.getFieldSet(0);
                //desc = getDescription(sr);
            }
            else if( id.equalsIgnoreCase(IDBAND) ){
                bandFieldSet = sr.getFieldSet(0);
                //desc = getDescription(sr);
            }
            else if( id.equalsIgnoreCase(IDIMAGE) ){
                imageFieldSet = sr.getFieldSet(0);
                //desc = getDescription(sr);
            }
            // Ici, on ignore les storage mapping (ils sont traités différemment, en meme temps que l'image)
            /*else if( id.equals(IDMAPPING) ){
                //mappingFieldSet = sr.getFieldSet(0);
                //desc = getDescription(mappingFieldSet);
                return;
            }*/
        }
        else {
            id = ref;
            // desc est récupéré de manière uniforme à la fin
            /*
            if( ref.equalsIgnoreCase(IDSURVEY) ) {
                desc = getDescription(sr);
            }
            else if( ref.equalsIgnoreCase(IDBAND) ){
                desc = getDescription(sr);
            }
            else if( ref.equalsIgnoreCase(IDIMAGE) ){
                desc = getDescription(sr);
            }
            */

            // Ici, on ignore les storage mapping (ils sont traités différemment, en meme temps que l'image)
            /*else if( ref.equals(IDMAPPING) ){
                //desc = getDescription(mappingFieldSet);
                // traité à un autre endroit
                return;
            }*/
        }
		// on récupère l'ensemble des FIELD
		desc = getDescription(sr);

		// traité à un autre endroit
        if( id.equalsIgnoreCase(IDMAPPING) ) return;
        //System.out.println(desc.length);

        // creation des nouveaux noeuds
        TRSet trSet = sr.getData(0).getTableData().getTRs();
        String nameRes = sr.getName();


        int nbTr = trSet.getItemCount();
        if( nameRes.equalsIgnoreCase(IDBAND) ) nameIndex = 1;
        else nameIndex = 0;

        ResourceNode newNode = null;



        // traitement particulier pour Observation_group
        if( nameRes.equalsIgnoreCase(IDBAND) /*&& nbTr>0*/ ) {
            processObsGroup(sr, father);
            return;
        }
            // création des noeuds pour chaque TR
            for( int i=0; i<nbTr; i++ ) {
                newNode = createNode(trSet.getItemAt(i), nameIndex, desc, nameRes);
                father.addChild(newNode);
                /*
                if( nameRes.equals(IDIMAGE) ) {
                    // ici traitement du storage mapping
                    if( storageMapping!=null ) {
                        TDSet tds = ((SavotTR)trMapping.getItemAt(i)).getTDs();
                        SavotTD td;
                        for( int j=0; j<tds.getItemCount(); j++ ) {
                            td = (SavotTD)tds.getItemAt(j);
                            //System.out.println(descMapping[j]);
                            //System.out.println(td.getContent());

                            // le nom pour cutout est "Organisation" --> bizarre
                            if( descMapping[j].equals("Organisation") && td.getContent().equals(CUTOUT) ) {
                                newNode.cutout = true;
                            }
                            if( descMapping[j].equals("Maximum size") ) maxSize = new Double(td.getContent()).doubleValue();
                        }
                    }

                    newNode.isLeaf = true;
                    newNode.survey = currentSurvey;
                    newNode.color = currentColor;
                    createFov(newNode);
                }
                // ajout de l'attribut epoch si necessaire
                if( nameRes.equals(IDIMAGE) && newNode.getParent().name.startsWith("epoch") ) newNode.epoch = newNode.getParent().name;
                */

            }
        //    System.out.println("in the loop");
        //}

        if( id.equalsIgnoreCase(IDSURVEY) && newNode!=null ) currentSurvey = newNode.explanation[0];


        // recursion
        ResourceSet rSet = sr.getResources();

        //System.out.println(sr.getName());
        for( int i=0; i<rSet.getItemCount(); i++ ) {
            processResource(rSet,i,newNode);
        }

    }

    /** Retourne une couleur pour le fov */
    static private Color getNextColor() {
        float factor = 0.3f*colorNb+0.05f;
        colorNb++;
        //le facteur 0.7f premet d'obtenir des couleurs moins saturées, je prefere
        return Color.getHSBColor((float)(factor-Math.floor(factor)),0.7f,1f);
    }

    private void createFov(ResourceNode node, String stcRegion) {
        // pour un spectre, on crée un FoV particulier
        boolean isSpectra = node.type==ResourceNode.SPECTRUM;

        // on ne crée pas un FoV inutilement
        // A REPRENDRE !!
        if( (xValTab==null || yValTab==null) && (Double.isNaN(xVal) || Double.isNaN(yVal)) && !isSpectra && stcRegion==null ) {
        	return;
        }

        if (Double.isNaN(angleVal)) {
            angleVal = 0;
        }

        if (stcRegion != null) {
//            Aladin.trace(3, "Creating FoV from STC-S description for node "+node.name);
            STCStringParser parser = new STCStringParser();
            List<STCObj> stcObjs = parser.parse(stcRegion);
            node.setFov(new Fov(stcObjs));
        }
        // voir alphaVal et compagnie pour ne pas créer le fov si non nécessaire
        // Création d'un Fov aux formes complexes (cas des images HST)
        else if( xValTab!=null && yValTab!=null) {
            node.setFov(new Fov(alphaVal, deltaVal, xValTab, yValTab, angleVal, xVal, yVal));
        }
        else if( isSpectra ) {
            node.setFov(new Fov(alphaVal, deltaVal, angleVal));
        }
        // Création d'un Fov rectangulaire
        else {
            node.setFov(new Fov( alphaVal, deltaVal, xVal, yVal, angleVal));
        }

        // reset des valeurs
        angleVal = alphaVal = deltaVal = xVal = yVal = Double.NaN;
        xValTab = yValTab = null;

        // couleur
        if( node.getParent() != null ) node.getFov().color = ((ResourceNode)(node.getParent())).col;

        if( node.cutout ) {
            node.getFov().cutout_x = node.getFov().cutout_y = maxSize*pixSize;
        }

    }

    /** Retourne le tableau des fields correspondant à la resource passée en paramètre
     * 	@param sr l'objet SavotResource
	 *	@return le tableau des champs correspondant
     */
    private SavotField[] getDescription(SavotResource sr) {
    	String id = (sr.getTables().getItemAt(0)).getId();
        String ref = (sr.getTables().getItemAt(0)).getRef();
        SavotField[] desc;

		if( id.length()!=0 ) {
		    desc = createDescription(sr.getFieldSet(0));
		    // ajout de la description au hashtable
		    fieldSetMapping.put(id,desc);
		}
		else {
		    desc = (SavotField[])fieldSetMapping.get(ref);
		}

		return desc;
    }

Hashtable fieldsPool = new Hashtable();
protected SavotField[] createDescription(FieldSet fs) {
    int nb = fs.getItemCount();
    SavotField[] desc = new SavotField[nb];
    String id, ref;
    for( int i=0; i<nb; i++ ) {
        desc[i] = (fs.getItemAt(i));
        id = desc[i].getId();
        ref = desc[i].getRef();
        //System.out.println("ID : "+id);
        //System.out.println("ref : "+ref);
        SavotField tmp;
        if( id.length()==0 && ref.length()>0 && (tmp=(SavotField)fieldsPool.get(ref))!=null )
            desc[i] = tmp;
        else if( id.length()>0 )
            fieldsPool.put(id, desc[i]);
    }
    return desc;
}

	int myIndexRA,myIndexDE;
    private ResourceNode createNode(SavotTR tr, int index, SavotField[] desc, String resourceName) {
        myIndexRA=myIndexDE=-1;
        TDSet tdSet = tr.getTDs();
        int nbTd = tdSet.getItemCount();
        String[] expla = new String[nbTd];
        String[] descStr = new String[desc.length];

        String[] originalExpla = new String[nbTd];

        double xLim, yLim;
        xLim = yLim = 0.;
        int indexRefNb, indexRA,indexDE,indexImgFormat,indexPosAng, indexDate, indexProcModes, indexOrigin, indexMachine, indexResType;
        indexRefNb=indexResType=indexMachine=indexOrigin=indexProcModes=indexDate=indexPosAng=indexImgFormat=indexRA=indexDE = -1;

        String name;
        String[] descId = new String[desc.length];
        // remplissage de descStr
        for( int i=0; i<desc.length; i++ ) {
            name = desc[i].getName();
            if( name.length()>0 ) {
                descStr[i] = name;
            }
            else {
                descStr[i] = desc[i].getId();
            }
            descId[i] = desc[i].getId();
        }

        String unit;
        // recuperation des valeurs des TD
        for( int i=0; i<nbTd; i++ ) {
            expla[i] = tdSet.getItemAt(i).getContent();
            originalExpla[i] = expla[i];

            try {
                // recuperation de pixSize
                if( descId[i].equals("PixelSize") ) {
                    pixSize = Math.abs(new Double(expla[i]).doubleValue());
                    pixSizeUnit = desc[i].getUnit();
                }

                // pour xVal et yVal, on prend la valeur absolue, car dans certains cas on récupérait des valeurs négatives !
                if(descId[i].equals("Size_alpha")) {
                	xVal = Math.abs(new Double(expla[i]).doubleValue());
                }
                else if(descId[i].equals("Size_delta")) yVal = Math.abs(new Double(expla[i]).doubleValue());
                else if(descId[i].equals("alpha")) {
                    alphaVal = new Double(expla[i]).doubleValue();
                    indexRA = i;
                }
                else if(descId[i].equals("delta")) {
                    deltaVal = new Double(expla[i]).doubleValue();
                    indexDE = i;
                }
                else if(descId[i].equals("AP") || descStr[i].equals("Position Angle")) {
                    indexPosAng = i;
                    angleVal = new Double(expla[i]).doubleValue();
                }
                // récupération de xLim
                else if( descId[i].equals("XLim")) {
                    String[] split = split(expla[i],", ");
                    // si une seule valeur, on remplit xLim
                    if( split.length==1 ) xLim = new Double(expla[i]).doubleValue();
                    // sinon on remplit xValTab
                    else {
                        xValTab = new double[split.length];
                        for( int k=0; k<split.length; k++ ) {
                            xValTab[k] = new Double(split[k]).doubleValue()/2.;
                        }
                    }
                }
                // récupération de yLim
                else if( descId[i].equals("YLim")) {
                    String[] split = split(expla[i],", ");
                    // si une seule valeur, on remplit yLim
                    if( split.length==1 ) yLim = new Double(expla[i]).doubleValue();
                    // sinon on remplit yValTab
                    else {
                        yValTab = new double[split.length];
                        for( int k=0; k<split.length; k++ ) {
                            yValTab[k] = new Double(split[k]).doubleValue()/2.;
                        }
                    }
                }
                // récupération du format (JPEG,FITS,...)
                else if( descId[i].equals(AVAILABLE_CODINGS) ) {
                    indexImgFormat = i;
                }
                // récupération des modes de processing
                else if( descId[i].equals(AVAILABLE_PROCESSINGS) ) {
                    indexProcModes = i;
                }
                // récupération de la machine à digitaliser
                else if( descId[i].equals(MACHINE) ){
                    indexMachine = i;
                }
                // récupération de la provenance des images
                else if( descId[i].equals(ORIGIN) ) {
                    indexOrigin = i;
                }
                // récupération de la date
                else if( descId[i].equals(OBS_DATE) ) {
                    indexDate = i;
                }
                // récupération de la longueur d'onde
                else if( descId[i].endsWith("wavelength") && expla[i].length()>0 ) {
                    if( descId[i].equals("Effective_wavelength") ) currentWavelength = getUnit(expla[i],desc[i].getUnit());
                    else if( currentWavelength==null ) currentWavelength = getUnit(expla[i],desc[i].getUnit());
                }
                // récupération du nom courant de la bande
                else if( descId[i].equals(FILTER_NAME) ){
                    currentFilterName = expla[i];
                }
                // récupération du type de ressource (test pour spectre)
                else if( descId[i].equalsIgnoreCase(RESTYPE) ) {
                    indexResType = i;
                }
                // récupération du refNumber
                else if( descId[i].equalsIgnoreCase("ReferenceNumber") ) {
                    indexRefNb = i;
                }

            }
            catch(Exception e) {}
            finally {
                // ajout de l'unité
                // (ce test sur desc.length est obligatoire car parfois on a trop de TD)
                if( i<desc.length && i!=indexRA && i!=indexDE && i!=index ) {
                	unit = desc[i].getUnit();
                	if( expla[i].length()>0 && unit.length()>0 ) {
                        if( i!=indexPosAng )
                        	expla[i] = getUnit(expla[i],unit);
                        // FOX veut l'angle de position en degrés
                        else {
                            expla[i] = floor(angleVal,2,3)+getUnit(unit);
                        }
                    }
                }
            }
		} // fin de la boucle sur tous les expla

        // on transforme RA et DE de degres decimaux a degres sexagesimaux
        if( indexRA>=0 && indexDE>=0 ) {
            try {
                frame.set(expla[indexRA]+" "+expla[indexDE]);
                frame.setPrecision(Astrocoo.ARCSEC+1);
                String coord = frame.toString(":");
                int beginDE = coord.indexOf("+");
                if( beginDE==-1 ) beginDE= coord.indexOf("-");
                expla[indexRA] = coord.substring(0,beginDE);
                expla[indexDE] = coord.substring(beginDE);
            }
            catch( Exception e) {}
        }

        ResourceNode node = new ResourceNode(aladin);
        node.type = ResourceNode.IMAGE;
        node.server = this.server;
        if( objet!=null ) node.objet = objet;
        node.description = descStr;
        node.explanation = expla;
        node.originalExpla = originalExpla;
        node.ra = indexRA;
		node.de = indexDE;

        // déporté et non dans la boucle pour écraser les valeurs données par Size_Alpha, Size_Delta
        if( xLim!=0. && yLim!=0. ) {
            xVal = Math.abs(2*xLim);
            yVal = Math.abs(2*yLim);
        }

        if( index>=0 ) {
        	node.name = expla[index];
        }

        myIndexRA = indexRA;
        myIndexDE = indexDE;

        // format de l'image (FITS,JPEG,...)
        if( indexImgFormat>=0 ) {
            node.formats = split(expla[indexImgFormat]," ");
            // par défaut, le format est le premier
            if( node.formats!=null && node.formats.length>0 ) node.curFormat = node.formats[0];
        }

        // modes de processing (MOSAIC, ...)
        if( indexProcModes>=0 ) {
            node.modes = split(expla[indexProcModes]," ");
            // par défaut, le mode est le premier
            if( node.modes!=null && node.modes.length>0 ) node.curMode = node.modes[0];
            if( node.modes!=null && node.modes.length>1 ) {
                if( targetObjet!=null ) {
                    node.setMosaicTarget(targetObjet);
                    node.targetObjet = targetObjet;
                }
            }
        }
        // machine à digitaliser
        if( indexMachine>=0 ) {
            node.machine = expla[indexMachine];
        }
        // provenance de l'image
        if( indexOrigin>=0 ) {
            node.origin = expla[indexOrigin];
        }
        // date
        if( indexDate>=0 ) {
            node.obsDate = expla[indexDate];
        }
        // test pour spectres
        if( indexResType>=0 ) {
            if( expla[indexResType].equalsIgnoreCase("SPECTRUM") )
                node.type = ResourceNode.SPECTRUM;
            else if( expla[indexResType].equalsIgnoreCase("CUBE") )
                node.type = ResourceNode.CUBE;
        }
        // referenceNumber
        if( indexRefNb>=0 ) {
            node.refNumber = expla[indexRefNb];
        }

        // on n'affiche pas les champs cachés
        for( int i=0; i<descStr.length; i++ ) {
            if( desc[i].getType().equals("hidden") ) expla[i] = "";
        }

        // pour permettre le tri par champ
//        setProperties(node);

        return node;
    }
    /* ---- FIN Méthodes pour la construction de l'arbre à partir d'un fichier VOTABLE_IDHA ---- */

    private void setProperties(ResourceNode node) {
    	if( node==null ) return;
    	// pour SIAP evolution
        node.properties = new Hashtable();
        node.propertiesUnits = new Hashtable();
        String s;
        for( int i=0; i<node.description.length; i++ ) {
            s = node.description[i];
            //System.out.println(node.explanation[i]);
            if( s!=null ) {
            	try {
            		node.properties.put(s, node.originalExpla[i]);
            		node.propertiesUnits.put(s, node.allUnits[i]);
            	}
            	catch(Exception e) {if( Aladin.levelTrace>=3 ) e.printStackTrace();};
            }
        }
        node.isObs = true;
    }

    /** Affichage dans la bonne unite.
     * Retourne un angle dont l'unité est unit sous forme de chaine dans la bonne unite
     * @param xCell l'angle
     * @param unit l'unité de x
     * @return l'angle dans une unite coherente + l'unite utilisee
     */
    protected static String getUnit(String angleStr, String unit) {
        if( unit==null ) return angleStr;

        double x;
        try {
            x = new Double(angleStr).doubleValue();
        }
        catch(NumberFormatException e) {
            return angleStr+unit;
        }

        if( Double.isNaN(x) ) return x+"";

        try {
            x = toDegrees(unit, x);
        }
        catch(Exception e) {
           x = floor(x,1,3);
           return x+unit;
        }
        /*
        // on met d'abord x en degrés
        if( unit.equalsIgnoreCase("deg") ) x = x/1.0;
        else if( unit.equalsIgnoreCase("arcmin") ) x = x/60.0;
        else if( unit.equalsIgnoreCase("arcsec") ) x = x/3600.0;
        else return x+""+unit;
        */
        String s=null;
        if( Math.abs(x)>=1.0 ) {s="deg";}
        if( Math.abs(x)<1.0 ) { s="arcmin"; x=x*60.0; }
        if( Math.abs(x)<1.0 ) { s="arcsec"; x=x*60.0;}

        /*
        if( flagCeil ) x=Math.floor(x*10.0)/10.0;
        else x=Math.ceil(x*1000.0)/1000.0;
        */
        x=floor(x,1,3);
        s=x+getUnit(s);

        return s;
     }

     /**
      * Méthode d'arrondi d'un double
      * @param d le double à arrondir
      * @param nbDec1 nombre de décimales si |d|>1
      * @param nbDec2 nombre de décimales si |d|<1
      * @return double
      */
     private static double floor(double d, int nbDec1, int nbDec2) {
         double fact = 1.;
         int nbDec = Math.abs(d)>1?nbDec1:nbDec2;
         for( int i=0; i<nbDec; i++ ) fact *= 10.;

         return Math.ceil(d*fact)/fact;
     }

     protected static double toDegrees(String unitDes, double value) throws Exception {
        // on convertit value en degrés
        if( unitDes.equalsIgnoreCase("deg") ) value =value/1.0;
        else if( unitDes.equalsIgnoreCase("arcmin") ) value = value/60.0;
        else if( unitDes.equalsIgnoreCase("arcsec") ) value = value/3600.0;
        else throw new Exception("Unknown unit");

        return value;
     }

     static private String getUnit(String unit) {
         if( unit.equalsIgnoreCase("deg") ) return "°";
         if( unit.equalsIgnoreCase("arcmin") ) return "'";
         if( unit.equalsIgnoreCase("arcsec") ) return "\"";
         return unit;
     }

 	/**
 	 * Recherche les paramètres de TRI (<PARAM name="SORTORDER" ... >)
 	 * et remplit le tableau sortItems en conséquence
 	 *
 	 */
 	protected void searchSortOrder(SavotVOTable vot) {
 		ParamSet params = vot.getParams();
 		int nbParam = params.getItemCount();
 		SavotParam param;
 		for( int i=0; i<nbParam; i++ ) {
 			param = params.getItemAt(i);
 			if( param.getName().equalsIgnoreCase("SORTORDER") ) {
 				String value = param.getValue();
 				if( value!=null && value.length()>0 ) {
 					sortItems = split(param.getValue(), " ");
 				}
 				return;
 			}
 		}
 	}

}

