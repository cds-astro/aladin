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
 * Created on 15-Apr-2005
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import cds.savot.model.ResourceSet;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotTR;
import cds.savot.model.SavotTable;
import cds.savot.model.SavotVOTable;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.model.TableSet;
import cds.savot.pull.SavotPullParser;

/** Cette classe permet le parsing propre des extensions SIAP
 * Une attention toute particulière a été portée à
 * être le plus générique et tolérant aux erreurs possible
 *
 * @author Thomas Boch [CDS]
 * (kickoff : 15/04/2005)
 */
public class SIAPExtBuilder extends TreeBuilder {

	// les utypes sur lesquels on se base pour prendre des décisions
	static final String UT_SIMPLEQUERY = "dal:SimpleQueryResponse";
	static final String UT_DALEXTENSIONS = "dal:QueryResponseExtensions";

	static final String UT_GENFEATURES = "Observation.DataID.Collection";
	static final String UT_GENFEATURES_DEPRECATED = "Observation/DataCollection";

	static final String UT_CHARACTERIZATION = "Observation/ivoa:Characterization";

	static final String UT_DATAACCESS = "Observation.Provenance.DataViewsAndAccess";
	static final String UT_DATAACCESS_DEPRECATED = "Observation/DataViewsAndAccess";

	static final String UT_COMPOSITION = "Observation/Provenance/Processing/Composition";

	static final String UT_RADEC = "ivoa:Characterization[ucd=pos]/Coverage/Location";
	static final String UT_MINRADEC = "ivoa:Characterization[ucd=pos]/Coverage/Bounds/min";
	static final String UT_MAXRADEC = "ivoa:Characterization[ucd=pos]/Coverage/Bounds/max";

	static final String UT_OBS_DATASET_ID = "Observation.DataID.DatasetID";
	static final String UT_OBS_DATASET_ID_DEPRECATED = "Observation/Identifier";

	// pour trouver l'angle de position
	static final String FOV_POS_ANGLE = "stc:AstroCoordSystem.CoordFrame.Cart2DRefFrame.Transform2.PosAngle";

	// parser à utiliser
	private SavotPullParser parser;

	private SavotVOTable votable;

	// variables de travail
	SavotResource orgSiapRes; // resource SIAP "originelle"
	SavotResource dalExtRes; // DAL extensions
	SavotTable genFeatTab; // general features
	SavotTable characTab; // characterization
	SavotTable dataAccessTab; // data views and access
	SavotTable compositionTab; // table explaining members nodes
    private Hashtable namesToNodes;
    private Hashtable altNamesToNodes;
    private int obsRefIndex;
    private int idxRefRelObs;
    private Vector alreadyAdded;

    private Vector otherTabs;

    // s'agit-il d'un document avec des footprints attachés
    private boolean isFootprintDoc = false;

    // clés possibles
    private Vector potentialKeys;

    // fait le lien keyID+"__"+keyVal --> noeud
    private Hashtable keyToNodes;


    private String obsRefID, obsNameID;

	SIAPExtBuilder(Aladin aladin, String target) {
		super(aladin, target);
	}

	/**
	 * construit l'arbre correspondant à un document SIAP Extensions
	 * @param parser parser VOT déja initialisé
	 * @return le noeud racine de l'arbre correpsondant au document
	 */
	protected ResourceNode build(SavotPullParser p) throws Exception {
		Aladin.trace(3, "Begin parsing of SIAP extensions document");

		// raz des variables membres utilisées pour le parsing
		reset();

		parser = p;

		if( parser==null ) throw new Exception("Null parser passed to SIAPExtBuilder class");

		votable = parser.getVOTable();

		// on remplit les références aux tables et resources
		preCheck();

		// on cherche une éventuelle indication du tri à appliquer
		searchSortOrder(votable);

		// si la resource siap originelle est manquante, on ne peut pas faire grand chose !
		if( orgSiapRes==null )
			throw new Exception("Could not find SIAP results section, parsing aborted !");

		// traitement section "dal/SimpleQueryResponse"

		// création du noeud racine
        ResourceNode root = new ResourceNode(aladin, "root");
        root.type = ResourceNode.VOID;
        root.isSIAPEvol = true;

	    SavotField[] initFields = createDescription(orgSiapRes.getFieldSet(0));
	    // TODO : à supprimer ?
	    Aladin.trace(3, "orgsiapres : "+orgSiapRes);
	    Aladin.trace(3, "initFields.length : "+initFields.length);
        String[] idFields = new String[initFields.length];
        for (int i = 0; i < initFields.length; i++) {
			idFields[i] = initFields[i].getId();
			if( initFields[i].getUtype().equals(UT_OBS_DATASET_ID)
				|| initFields[i].getUtype().equals(UT_OBS_DATASET_ID_DEPRECATED)) {
				potentialKeys.addElement(initFields[i].getId());
			}
		}

        obsRefID = getObsRefID(initFields);
        obsNameID = getObsNameID(initFields);

        int bandPassIdx =  findFieldByUCD(SIAP_BANDPASS_ID, initFields);

        // TODO : à supprimer ?
        Aladin.trace(3, "obsRefID : "+obsRefID);
        Aladin.trace(3, "obsNameID : "+obsNameID);

        obsRefIndex = findValIndex(idFields, obsRefID);
        idxRefRelObs = findValIndex(idFields, obsNameID);
        // les descriptions disponibles
        Vector desc = new Vector();



        namesToNodes = new Hashtable();
        altNamesToNodes = new Hashtable();
        // on récupère tous les enregistrements de la resource SIAP de base
        ResourceNode[] siapRes = processSIAPEvolResource(orgSiapRes);

        // on garde en mémoire les clé+"__+valeur
        registerKeyVal(initFields, siapRes);

        if( siapRes.length>0 ) {
        	root.siapSortFields = siapRes[0].description;

        	ResourceNode n = siapRes[0];

        	for( int i=0; i<n.description.length; i++ ) desc.addElement(n.description[i]);

        	// TEST
//        	if( couldBeSSA(votable) ) {
//        	    for( int i=0; i<siapRes.length; i++ ) {
//        	        siapRes[i].type = SSA;
//        	    }
//        	}
        }


		// dans le cas d'un SIAP de base, avec uniquement la première section
		if( isOnlyOrgSiap() ) {
			Aladin.trace(3, "Processed as basic SIAP");
//        if( true){
		    for( int i=0; i<siapRes.length; i++ )
		        root.addChild(siapRes[i]);
		}


		// traitement de genFeatTab
		if( dalExtRes!=null ) {
			Aladin.trace(3, "Entering processing of gen feat tab");
		    processGenFeatTab(siapRes, desc);
		}

		alreadyAdded = new Vector();
        // traitement de characTab
		if( characTab!=null ) {
			Aladin.trace(3, "Entering processing of charac");
		    processCharac();
		}

		// traitement de compositionTab
		if( compositionTab!=null ) {
			Aladin.trace(3, "Entering processing of composition table");
		    processCompositionTab();
		}

		// traitement de dataAccessTab
		if( dataAccessTab!=null ) {
			Aladin.trace(3, "Entering processing of dataaccess table");
		    processDataAccessTab();
		}

		// traitement des tables génériques
		Enumeration eGenTab = otherTabs.elements();
		while( eGenTab.hasMoreElements() ) {
			processGenericTable((SavotTable)eGenTab.nextElement());
		}

		// au final, on boucle pour rattacher les obs restantes au noeud racine
		if( !isOnlyOrgSiap() ) {
			for( int i=0; i<siapRes.length; i++ ) {
			    if( !alreadyAdded.contains(siapRes[i]) ) {
			        root.addChild(siapRes[i]);
			        if( siapRes[i].getNbOfChildren()>0 ) siapRes[i].isLeaf = false;
			        else siapRes[i].isLeaf = true;
			        alreadyAdded.addElement(siapRes[i]);
			    }

			}
		}

        Enumeration e = root.getChildren();
        while( e.hasMoreElements() ) ((ResourceNode)e.nextElement()).isObs = true;

        // tri par bandpass par défaut
        if( /*isFootprintDoc &&*/ sortItems==null && bandPassIdx>=0 ) {
                sortItems = new String[1];
                sortItems[0] = initFields[bandPassIdx].getId();
                // on prend le name si l'ID est vide
                if( sortItems[0]==null || sortItems[0].length()==0 ) {
                    sortItems[0] = initFields[bandPassIdx].getName();
                }
                // si ID et name sont vides, on annule le tri !
                if( sortItems[0]==null || sortItems[0].length()==0 ) {
                    sortItems = null;
                }
        }


        // finalement, on effectue le tri
        if( sortItems!=null ) MetaDataTree.doSortSiapEvol(sortItems, root);

        reset();

		return root;
	}

	/** traitement d'une table "générique":
	 * ajout d'un noeud avec le nom de la table, puis ajout dans ce nouveau noeud des éléments
	 * @param table
	 */
	private void processGenericTable(SavotTable table) {
		String tabName = table.getId();
		Hashtable keyToTabRes = new Hashtable();

		SavotField[] fields = createDescription(table.getFields());

		int obsRefIndex = findKey(fields);

		if( obsRefIndex<0 ) {
			Aladin.trace(3, "Could not find key field when processing generic table");
			return;
		}

		// TODO : à supprimer ?
		Aladin.trace(3, tabName);
		Aladin.trace(3, "+++ : "+obsRefIndex);
		TRSet trs = table.getData().getTableData().getTRs();
		TDSet tds;

		if( trs.getItemCount()==0 ) return;

		List<SavotTR> e = trs.getItems();
		String keyVal;
		ResourceNode node, parentNode;
		
		// boucle sur les TR
		for (SavotTR tr : e) {
			tds = tr.getTDs();

	        keyVal = tds.getContent(obsRefIndex);
//	        node = (ResourceNode)namesToNodes.get(key);
	        node = getNodeFromKey(fields[obsRefIndex], keyVal);
	        if( node==null ) {
	        	Aladin.trace(3, "Could not find an observation with key "+keyVal);
	        	continue;
	        }
	        if( keyToTabRes.get(node)==null ) {
	        	node.isLeaf = false;
	        	// permet de distinguer un noeud container d'u noeud container qui represente des données et peut etre chargé !!
	        	if( node.location!=null ) node.hasData = true;
	        	ResourceNode child = new ResourceNode(aladin, tabName);
	        	child.isLeaf = false;
	        	node.addChild(child);
	        	keyToTabRes.put(node, child);
	        }
	        parentNode = (ResourceNode)keyToTabRes.get(node);
	        // n = nouvau noeud correspondant au TR courant
	        ResourceNode n = createSIAPNode(tr, fields);

	        parentNode.addChild(n);

	        setDatasetType(n);

	        // TODO : à supprimer ?
	        Aladin.trace(3, "key : "+keyVal);
	        Aladin.trace(3, "***"+namesToNodes.get(keyVal));

	    }

	}



	/** on s'occupe des data views and access
	 *
	 */
	private void processDataAccessTab() {
	    SavotField[] fields = createDescription(dataAccessTab.getFields());
	    // TODO : à changer, prendre en compte le ref !

	    // fait le lien nom-->noeud "Data Access"
	    Hashtable namesToDataAccess = new Hashtable();

	    TRSet trSet = dataAccessTab.getData().getTableData().getTRs();
	    if( trSet.getItems()==null ) return;

	    List<SavotTR> e = trSet.getItems();

	    String obsRef, relatedObs;
	    ResourceNode parent;
	    Vector children;
	    ResourceNode curDataAccessNode;

//        System.out.println("obsRefId is "+obsRefID);
	    int obsRefIndex = findFieldByID(obsRefID, fields);

	    int locationIndex = findFieldByUtype("Observation.Provenance.DataViewsAndAccess.AccessReference", fields);
	    if( locationIndex<0 ) locationIndex = findFieldByID("LinktoPixels", fields);

        int dataOrgaIndex = findFieldByUtype("Observation.Provenance.DataViewsAndAccess.ViewType", fields);
        if( dataOrgaIndex<0 ) dataOrgaIndex = findFieldByID("DataOrganisation", fields);

        int descIndex = findFieldByUtype("Observation.Provenance.DataViewsAndAccess.ViewDescription", fields);
        if( descIndex<0 ) descIndex = findFieldByID("desc", fields);

        int numberIndex = findFieldByID(NB_OF_PATCHES, fields);
        int mapParamIndex = findFieldByID(MAPPARAM, fields);
        boolean cutout;

        String locationStr, dataOrgaStr, descStr;
        locationStr=dataOrgaStr=descStr=null;
        TDSet tds;

	    // boucle sur les TR
        for (SavotTR tr : e) {
        	tds = tr.getTDs();
	        obsRef = tds.getContent(obsRefIndex);
//	        System.out.println("obsref : "+obsRef);
	        parent = (ResourceNode)namesToNodes.get(obsRef);
//	        System.out.println("parent.name : "+parent.name);

	        if( parent!=null ) {
	            // on cherche le noeud data views
	            // s'il n'existe pas encore, on le crée
	            curDataAccessNode = (ResourceNode)namesToDataAccess.get(obsRef);
	            if( curDataAccessNode==null ) {
	                curDataAccessNode = new ResourceNode(aladin, "Data Access");
	                namesToDataAccess.put(obsRef, curDataAccessNode);
	                parent.addChild(curDataAccessNode);
//	                System.out.println("ajout de "+parent.name+" --> "+curDataAccessNode.name);
	                parent.isLeaf = false;
	            }

	            String nbPatch = "";
	            if( numberIndex>=0 ) nbPatch = tds.getContent(numberIndex).trim();
	            String mapParam = "";
	            if( mapParamIndex>=0 ) mapParam = tds.getContent(mapParamIndex).trim();

	            cutout = false;
	            if( locationIndex>=0 ) locationStr = tds.getContent(locationIndex);
	            if( dataOrgaIndex>=0 ) dataOrgaStr = tds.getContent(dataOrgaIndex);
	            if( descIndex>=0 ) descStr = tds.getContent(descIndex);


	            cutout = dataOrgaStr!=null && dataOrgaStr.equalsIgnoreCase("CUTOUTS");

	            ResourceNode newNode = new ResourceNode(aladin, parent);
	            newNode.name = "View";
	            newNode.isLeaf = true;
	            newNode.cutout = cutout;
	            newNode.location = locationStr;

	            if( newNode.cutout ) {
	            	String cutoutCenter = getRequestedPos()!=null?getRequestedPos():newNode.explanation[newNode.ra]+" "+newNode.explanation[newNode.de];
	            	newNode.setCutoutTarget(cutoutCenter, false);
	            }

	            // traitement du mode RETRIEVAL
	            if( dataOrgaStr.equals("RETRIEVAL") ) newNode.indexing = "HTML";


	    		// TODO : à modifier, à voir avec François
	    		if( descStr!=null && descStr.indexOf("characterization XML")>=0 ) {
	    			newNode.type = ResourceNode.CHARAC;
	    		}


	            // parametres associés au slice
	            // nb of patches
	            if( nbPatch.length()>0 ) newNode.maxImgNumber = nbPatch;

	            if( mapParam.length()>0 ) {
	            	String[] params = split(mapParam,",");
	            	if( params.length==2 ) {
	            		try {
	            			newNode.beginVel = Double.valueOf(params[0]).doubleValue();
	            			newNode.velStep = Double.valueOf(params[1]).doubleValue();
	            		}
	            		catch( NumberFormatException nfe ) {}
	            	}
	            }

	            newNode.dataOrga = dataOrgaStr.trim();

	            // on met un nom plus sympathique !
	            if( descStr!=null && descStr.length()>0 ) newNode.name = descStr.trim();
	            else if( dataOrgaStr!=null && dataOrgaStr.length()>0 ) newNode.name = dataOrgaStr.trim();

	            setDatasetType(newNode);

	            curDataAccessNode.addChild(newNode);
	        }
	    }
	}

	/** on traite la composition
	 *
	 */
	private void processCompositionTab() {
	    SavotField[] fields = createDescription(compositionTab.getFields());
	    // TODO : à changer, prendre en compte le ref !
	    int obsRefIndex = findFieldByID(obsRefID, fields);
	    // TODO : comment trouver ce champ là ??
	    int relatedObsIndex = findFieldByID("RelatedObservation", fields);

	    int compositionDescIdx = findFieldByUtype("Observation/Provenance/Processing/Composition/description", fields);

	    // fait le lien nom-->noeud "Members"
	    Hashtable namesToMembers = new Hashtable();

	    TRSet trSet = compositionTab.getData().getTableData().getTRs();
	    List<SavotTR> e = trSet.getItems();
	    String obsRef, relatedObs;
	    ResourceNode parent;
	    ResourceNode curMembersNode;
	    Vector children;

	    // boucle sur les TR
	    for (SavotTR tr : e) {
	        obsRef = (tr.getTDs().getItemAt(obsRefIndex)).getContent();
	        parent = (ResourceNode)namesToNodes.get(obsRef);

	        if( parent!=null ) {

	            // on cherche le noeud Members
	            // s'il n'existe pas encore, on le crée
	        	curMembersNode = (ResourceNode)namesToMembers.get(obsRef);
	            if( curMembersNode==null ) {
	            	// on donne un autre nom que Members si possible
	            	if(  compositionDescIdx>=0 ) {
	            		curMembersNode = new ResourceNode(aladin, (tr.getTDs().getItemAt(compositionDescIdx)).getContent());
	            	}
	            	else {
	            		curMembersNode = new ResourceNode(aladin, "Members");
	            	}
	            	namesToMembers.put(obsRef, curMembersNode);
	                parent.addChild(curMembersNode);

	                parent.isLeaf = false;
	            }

	            relatedObs = (tr.getTDs().getItemAt(relatedObsIndex)).getContent();
	            children = (Vector)altNamesToNodes.get(relatedObs);



	            if( children!=null ) {
	                Enumeration eChildren = children.elements();
	                ResourceNode child;
	                while( eChildren.hasMoreElements() ) {
	                    child = (ResourceNode)eChildren.nextElement();
	                    curMembersNode.addChild(child);
	                    alreadyAdded.addElement(child);
	                    if( child.altName!=null ) child.name = child.altName;
	                }
	            }
	        }
	    }

	}

	/** on traite la characterisation :
	 * on ajoute un NOEUD "Characterization"
	 *
	 */
	private void processCharac() {
	    SavotField[] fields = createDescription(characTab.getFields());
	    // TODO : à changer, prendre en compte le ref !
	    int obsRefIndex = findFieldByID(obsRefID, fields);

	    // fait le lien nom-->noeud "Info"
	    Hashtable namesToInfo = new Hashtable();

	    TRSet trSet = characTab.getData().getTableData().getTRs();
	    List<SavotTR> e = trSet.getItems();
	    String obsRef;
	    ResourceNode curInfoNode;

	    for (SavotTR tr : e) {
	    	obsRef = (tr.getTDs().getItemAt(obsRefIndex)).getContent();
	        ResourceNode parent = (ResourceNode)namesToNodes.get(obsRef);

	        if( parent!=null ) {

	            // on cherche le noeud Info
	            // s'il n'existe pas encore, on le crée
	            curInfoNode = (ResourceNode)namesToInfo.get(obsRef);
	            if( curInfoNode==null ) {
	            	curInfoNode = new ResourceNode(aladin, "Info/Metadata");
	            	namesToInfo.put(obsRef, curInfoNode);
	                parent.addChild(curInfoNode);

	                parent.isLeaf = false;
	            }

	            // récupération du FOV
	            Fov fov = getFovFromCharac(tr.getTDs(), fields);
	            if( fov!=null ) parent.setFov(fov);


	            ResourceNode n = createSIAPNode(tr, fields);
	            n.name = "CHARACTERIZATION";
	            n.type = ResourceNode.INFO;
	            curInfoNode.addChild(n);
	        }
	    }
	}

	/** on s'occupe de la table General Features
	 *
	 * @param tab le tableau des noeuds de la resource SIAP originale
	 */
	private void processGenFeatTab(ResourceNode[] tab, Vector desc) {
		processFov(tab, dalExtRes);

		if( idxRefRelObs<0 ) {
            Aladin.trace(3, "idxRefRelObs is null, stop processing of genFeatTab");
            return;
        }

        Hashtable genFeatNodes = new Hashtable();
        processGenFeat(dalExtRes, genFeatNodes, desc);

        // REPRENDRE ICI en attachant les noeuds !!
//        Enumeration e = root.getChildren();
        ResourceNode associatedNode;

        ResourceNode n;
        String key;

//        while( e.hasMoreElements() ) {
        for( int i=0; i<tab.length; i++) {
//          n = (ResourceNode)e.nextElement();
            n = tab[i];
          // pour les references des sousobs
//          namesToNodes.put(n.explanation[obsRefIndex], n);
//          System.out.println(n.explanation[obsRefIndex]);


//          if( altNamesToNodes.get(n.explanation[idxRefRelObs])==null ) altNamesToNodes.put(n.explanation[idxRefRelObs], new Vector());
          ((Vector)altNamesToNodes.get(n.explanation[idxRefRelObs])).addElement(n);
//          System.out.println("on met dans altNames : "+n.explanation[idxRefRelObs]);


            n.links = new Hashtable();
            //System.out.println("\n node : "+n.name);
            for( int j=0; j<n.description.length; j++ ) {
                key = n.description[j]+n.explanation[j];

                associatedNode = (ResourceNode)genFeatNodes.get(key);

                if( associatedNode!=null ) {
                    associatedNode.isLeaf = false;
                    n.links.put(n.description[j], associatedNode);
                    //System.out.println(key);

                    // pour l'origine de la resource (.origin), qui apparait dans Properties
                    for( int k=0; k<associatedNode.description.length; k++) {
                    	if( associatedNode.description[k].equals("Organisation") ) {
                    		n.origin = associatedNode.explanation[k];
                    		break;
                    	}
                    }

                }
            }
        }

	}

	/**
	 * On recherche les resources et tables
	 * dont on aura besoin par la suite
	 */
	private void preCheck() {
		ResourceSet resources = votable.getResources();
		TableSet tables;
		SavotResource resource;
		// TODO : à supprimer ?
		Aladin.trace(3, "nb rsources : "+resources.getItemCount());

		for( int i=0; i<resources.getItemCount(); i++ ) {
			resource = resources.getItemAt(i);
			checkIfOrgSiap(resource);
			if( checkIfDalExt(resource) ) continue;

			// on parcourt l'ensemble des tables et on conserve
			// les références à celles qui nous intéressent
			TableSet tSet = resource.getTables();
			SavotTable tab;
			for( int j=0; j<tSet.getItemCount(); j++ ) {
				// TODO : à supprimer ?
				Aladin.trace(3, "Process table "+j);
				tab = tSet.getItemAt(j);

//                System.out.println(tab.getUtype());

				// checks if general features table
				if( genFeatTab==null &&
					( tab.getUtype().trim().equalsIgnoreCase(getUType(UT_GENFEATURES)) ||
					  tab.getUtype().trim().equalsIgnoreCase(getUType(UT_GENFEATURES_DEPRECATED)) ) ) {

					genFeatTab = tab;
				}

				// checks if characterization table
				else if( characTab==null &&
						 tab.getUtype().trim().equalsIgnoreCase(getUType(UT_CHARACTERIZATION)) )
					characTab = tab;

				// checks if data views and access table
				else if( dataAccessTab==null &&
						 ( tab.getUtype().trim().equalsIgnoreCase(getUType(UT_DATAACCESS)) ||
						   tab.getUtype().trim().equalsIgnoreCase(getUType(UT_DATAACCESS_DEPRECATED)) ) ) {

					dataAccessTab = tab;
				}

				// checks if composition table
				else if( compositionTab==null &&
						 tab.getUtype().trim().equalsIgnoreCase(getUType(UT_COMPOSITION)) )
					compositionTab = tab;

				// autre table
				else if( i!=0 ) otherTabs.addElement(tab);
		}

//			System.out.println(characTab);
//			System.out.println(dataAccessTab);
//			System.out.println(genFeatTab);
//			System.out.println(compositionTab);
		}

	}

	/**
	 *
	 * @return true if the processed document holds only the original SIAP resource
	 */
	//TODO : changer nom de cette méthode, on peut en fait avoir original SIAP + tables génériques !
	private boolean isOnlyOrgSiap() {
	    return dalExtRes==null && characTab==null && compositionTab==null
	           && dalExtRes==null && dataAccessTab==null && genFeatTab==null;
	}

	private ResourceNode[] processSIAPEvolResource(SavotResource res) {
		Aladin.trace(3, "Processing original SIAP resource");

	    // on récupère le tableau de FIELD correspondant à mainRes
	    SavotField[] fields = createDescription(res.getFieldSet(0));

	    // d'après la spécif. SIAP, la RESOURCE results ne contient qu'une table

	    // quand il n'y a pas de réponse !
	    if( res.getData(0)==null ) return null;
	    if( res.getData(0).getTableData()==null ) return null;

	    TRSet obsSet = res.getData(0).getTableData().getTRs();
	    ResourceNode[] resTab = new ResourceNode[obsSet.getItemCount()];

	    for( int i=0; i<resTab.length; i++ ) {
	        resTab[i] = createSIAPNode(obsSet.getItemAt(i), fields);
			resTab[i].isSIAPEvol = true;
			if( idxRefRelObs>=0 && obsRefIndex>=0 ) resTab[i].altName = resTab[i].explanation[obsRefIndex];

//			namesToNodes.put(resTab[i].explanation[obsRefIndex], resTab[i]);
//			if( altNamesToNodes.get(resTab[i].explanation[idxRefRelObs])==null ) altNamesToNodes.put(resTab[i].explanation[idxRefRelObs], new Vector());

			if( obsRefIndex>=0 ) namesToNodes.put(resTab[i].explanation[obsRefIndex], resTab[i]);
			if( idxRefRelObs>=0 && altNamesToNodes.get(resTab[i].explanation[idxRefRelObs])==null ) altNamesToNodes.put(resTab[i].explanation[idxRefRelObs], new Vector());


	    }


	    return resTab;
	}

	/**
	 * vérifie si resource est la section results d'un document SIAP
	 * Si oui, on conserve la référence
	 * @param resource la resource à vérifier
	 */
	private void checkIfOrgSiap(SavotResource resource) {
		// si déja trouvé, on ne teste même pas
		if( orgSiapRes!=null ) return;

		if( resource.getUtype().trim().equalsIgnoreCase(getUType(UT_SIMPLEQUERY)) ||
            resource.getType().equalsIgnoreCase("results") )
			orgSiapRes = resource;
	}

	/**
	 * vérifie si resource constitue la section DAL extensions
	 *
	 * @param resource la resource à vérifier
	 */
	private boolean checkIfDalExt(SavotResource resource) {
		// si déja trouvé, on ne teste même pas
		if( dalExtRes!=null ) return false;

		if( resource.getUtype().trim().equalsIgnoreCase(getUType(UT_DALEXTENSIONS)) ||
            resource.getUtype().trim().equalsIgnoreCase("dal:footprint.geom") ) {
		    isFootprintDoc = true;
			dalExtRes = resource;
			return true;
	}

		return false;


	}

	/** crée un FoV à patir des données de characterization
	 *
	 * @param tds
	 * @param fields
	 * @return
	 */
	private Fov getFovFromCharac(TDSet tds, SavotField[] fields) {
		Fov fov = null;

		// position
		int indexPos = findFieldByUtype(getUType(UT_RADEC), fields);
		int indexMinPos = findFieldByUtype(getUType(UT_MINRADEC), fields);
		int indexMaxPos = findFieldByUtype(getUType(UT_MAXRADEC), fields);

		// si un des champs est manquant, on ne peut pas calculer le FOV
		if( indexPos<0 || indexMinPos<0 || indexMaxPos<0 ) return null;

		String posStr, minPosStr, maxPosStr;
		double posRA, posDEC, minPosRA, minPosDEC, maxPosRA, maxPosDEC;

		posStr = tds.getContent(indexPos);
		minPosStr = tds.getContent(indexMinPos);
		maxPosStr = tds.getContent(indexMaxPos);

		String[] tab;

		try {
			tab = split(posStr, " ");
//			posRA = Double.parseDouble(tab[0]);
//			posDEC = Double.parseDouble(tab[1]);
			posRA = Double.valueOf( tab[0] ).doubleValue();
			posDEC = Double.valueOf( tab[1] ).doubleValue();

			tab = split(minPosStr, " ");
//			minPosRA = Double.parseDouble(tab[0]);
//			minPosDEC = Double.parseDouble(tab[1]);
			minPosRA = Double.valueOf( tab[0] ).doubleValue();
			minPosDEC = Double.valueOf( tab[1] ).doubleValue();

			tab = split(maxPosStr, " ");
//			maxPosRA = Double.parseDouble(tab[0]);
//			maxPosDEC = Double.parseDouble(tab[1]);
			maxPosRA = Double.valueOf( tab[0] ).doubleValue();
			maxPosDEC = Double.valueOf( tab[1] ).doubleValue();
		}
		catch(NumberFormatException nfe) {
			return null;
		}
		catch(NullPointerException npe) {
			return null;
		}

//		fov= new Fov(posRA, posDEC, Math.abs(maxPosRA-minPosRA)*Math.cos(posDEC*Math.PI/180.0), Math.abs(maxPosDEC-minPosDEC), 0);
		fov = new Fov(posRA, posDEC, new double[] {minPosRA, minPosDEC}, new double[] {maxPosRA, maxPosDEC}, 0);
//		System.out.println((maxPosDEC-minPosDEC)*60);
		return fov;
	}

	/** Cette méthode constitue une couche de traduction pour les utypes
	 *  Le but est de ne pas avoir à redistribuer une version au cas où
	 *  les noms des utypes à utiliser seraient modifiés
	 *  En pratique et pour le moment, on se contente de renvoyer la chaine telle quelle.
	 *  On pourrait p-e utiliser le GLU
	 * @param s
	 * @return
	 */
	private String getUType(String s) {
		return s;
	}

	/**
	 * réinitialise certaines variables temporaires
	 * utilisées pour le parsing
	 */
	private void reset() {
		parser = null;
		votable = null;
		orgSiapRes = null;
		dalExtRes = null;
		otherTabs = new Vector();
		potentialKeys = new Vector();
		keyToNodes = new Hashtable();
		sortItems = null;
		isFootprintDoc = false;
	}

	/**
	 * Recherche et retourne l'ID du champ faisant office de "ObservationReference"
	 * @param fields
	 * @return
	 */
	private String getObsRefID(SavotField[] fields) {
		for( int i=0; i<fields.length; i++ ) {
			// TODO : à supprimer ?
			Aladin.trace(3, "Reading field : "+fields[i].getId());
			if( fields[i].getUtype().equals(UT_OBS_DATASET_ID) || fields[i].getUtype().equals(UT_OBS_DATASET_ID_DEPRECATED) ) {
				return fields[i].getId();
			}
		}
		return null;
	}
	/**
	 * Recherche et retourne l'ID du champ faisant office de "ObservationName"
	 * @param fields
	 * @return
	 */
	private String getObsNameID(SavotField[] fields) {
		for( int i=0; i<fields.length; i++ ) {
			if( fields[i].getUtype().equals("Observation/TargetName") ) return fields[i].getId();
		}
		return null;
	}

	// TODO : refactoring pour remonter cette méthode au niveau de ResourceNode
	/**
	 * Fixe le type de données représenté par le noeud (IMAGE, SPECTRUM, CATALOGUE, RETRIEVAL)
	 * @param node
	 */
	private void setDatasetType(ResourceNode node) {
		String type = node.getFieldValFromUtype("Observation.Provenance.DataViewsAndAccess.ViewType");
		if( type==null ) node.getFieldValFromUtype("DatasetType");
		if( type==null ) type = node.dataOrga;
		// bidouille pour SAADA (workshop Euro-VO)
		if( type==null ) type = node.getFieldValFromUtype("Access.Format");

		if( type==null ) return;
		if( type.equalsIgnoreCase("RETRIEVAL") ) node.indexing = "HTML";
		else if( type.equalsIgnoreCase("IMAGE")) node.type = ResourceNode.IMAGE;
		else if( type.equalsIgnoreCase("CUBE")) node.type = ResourceNode.CUBE;
		else if( type.equalsIgnoreCase("SPECTRUM") || type.startsWith("spectrum") ) node.type = ResourceNode.SPECTRUM;
		// TODO : A REFLECHIR !!
		else if( type.equalsIgnoreCase("CATALOGUE") ) node.type = ResourceNode.OTHER;
		// bidouille SAADA
		else if( type.startsWith("text/xml" ) ) node.type = ResourceNode.CAT;

	}

	private int findKey(SavotField[] fields) {
//		return findFieldByID(obsRefID, fields);
		for( int i=0; i<fields.length; i++ ) {
			if( isPotentialKey(fields[i]) ) return i;
		}
		return -1;
	}

	private ResourceNode getNodeFromKey(SavotField field, String keyVal) {
		return (ResourceNode)keyToNodes.get(field.getId()+"__"+keyVal);
//		return (ResourceNode)namesToNodes.get(keyVal);
	}

	private boolean isPotentialKey(SavotField field) {
		String id = field.getId();
		if( id==null || id.length()==0 ) return false;
		return potentialKeys.contains(id);
	}

	/**
	 * pour les clés "Observation/Identifier" multiples !!
	 * @param fields
	 * @param nodes
	 */
	private void registerKeyVal(SavotField[] fields, ResourceNode[] nodes) {
		String keyID, keyVal;
		ResourceNode node;
		for( int i=0; i<fields.length; i++ ) {
			if( isPotentialKey(fields[i]) ) {
				Aladin.trace(3, "Field "+fields[i].getId()+" might be a key");
				keyID = fields[i].getId();
				for( int j=0; j<nodes.length; j++) {
					node = nodes[j];
					keyVal = node.explanation[i];
					keyToNodes.put(keyID+"__"+keyVal, node);
				}
			}
		}

	}



	/**
	 * traite une description éventuelle des FoV
	 * @param siapNodes
	 * @param res
	 */
	private void processFov(ResourceNode[] siapNodes, SavotResource res) {
        Aladin.trace(3, "Entering processFov");
		ResourceSet resources = res.getResources();
		int nbRes;
		if( resources==null || (nbRes=resources.getItemCount())==0 ) return;

		// on récupère les ressources étant des description de FoV
		Vector v = new Vector();
		SavotResource curRes;
		for( int i=0; i<nbRes; i++ ) {
			curRes = resources.getItemAt(i);
			if( curRes.getUtype().equalsIgnoreCase("ivoa:characterization[ucd=pos]/coverage/support") ||
                curRes.getUtype().equalsIgnoreCase("char:SpatialAxis.coverage.support") ) {
                v.addElement(curRes);
			}
		}
		if( v.size()==0 ) {
		    Aladin.trace(3, "Did not find any FoV description !");
            return;
        }

		SavotResource[] resToParse = new SavotResource[v.size()];
		v.copyInto(resToParse);
		v = null;

		FootprintParser fpParse = new FootprintParser(resToParse);
		Hashtable fpHash = fpParse.getFooprintHash();

		// on attache le bon FoV aux différents noeuds
		ResourceNode curNode;
		String key;
		for( int i=0; i<siapNodes.length;  i++ ) {
			curNode = siapNodes[i];
			key = curNode.getFieldValFromUtype("ivoa:characterization[ucd=pos]/coverage/support/@id");
            // nouveau format
            if( key==null ) key = curNode.getFieldValFromUtype("dal:footprint.geom.id");

            if( key==null ||key.length()==0 ) continue;

			// --- Création nouveau PlanField ---

			// on commence par récupérer le bean avec les infos nécessaires
			FootprintBean fpBean = (FootprintBean)fpHash.get(key);
			if( fpBean==null ) continue ; // dans ce cas, on n'a pas trouvé le FoV associé

			// finally attach a new PlanField to the current node
			PlanField pf;
			double ra, dec, posAngle;
			String raStr, decStr;
			try {
				// TODO : vraiment pas beau, à changer pour ne pas avoir à faire
				// l'aller-retour deciaml --> sexa --> decimal
                // TODO : se baser plutot sur les ref (ou penser aux UCD1+)
				raStr = curNode.getFieldValFromUcd(SIAP_RA);
				if( raStr==null ) raStr = curNode.getFieldValFromUcd(SIAP_RA_UCD1P);
				decStr = curNode.getFieldValFromUcd(SIAP_DE);
				if( decStr==null ) decStr = curNode.getFieldValFromUcd(SIAP_DE_UCD1P);

				double[] pos = TreeView.getDeciCoord(raStr, decStr);
				ra = pos[0];
				dec = pos[1];
				// TODO : affreux, affreux, affreux
				String posAngStr = curNode.getFieldValFromUtype(FOV_POS_ANGLE);
				if( posAngStr==null ) posAngStr = curNode.getFieldValFromName("PA");
				if( posAngStr==null ) posAngStr = curNode.getFieldValFromName("Position Angle");

				// if still null, set to 0
				if( posAngStr==null ) posAngStr = "0";

				// au cas où on a une unité accolée, exemple : "103 deg"
				// TODO : quand on séparera unité et description, on pourra supprimer cette partie
				int idx = posAngStr.indexOf('°');
                if( idx<0 ) idx = posAngStr.indexOf("deg");
                if( idx<0 ) idx = posAngStr.indexOf(" ");
                if( idx<0 ) idx = posAngStr.indexOf("'");
                if( idx<0 ) idx = posAngStr.indexOf('"');
                if( idx<0 ) idx = posAngStr.indexOf("arc");

				if( idx>0 ) posAngStr = posAngStr.substring(0, idx);
				try {
					posAngle = Double.valueOf(posAngStr).doubleValue();
				}
				catch(Exception e) {Aladin.trace(3, "Could not find position angle field, assuming value is 0");posAngle = 0.0;}
			}
			catch(NullPointerException e) {e.printStackTrace();continue;}

			pf = new PlanField(aladin, fpBean, key);
			pf.make(ra,dec,posAngle);

			curNode.setFov(new Fov(aladin, fpBean, key, ra, dec, posAngle));
		}
	}



}
