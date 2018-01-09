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

import java.awt.Component;
import java.awt.FileDialog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import cds.fits.HeaderFits;
import cds.tools.Util;


/** <p>Title : IDHAGenerator</p>
 *  <p>Description : GUI allowing to build IDHA XML files describing a set of local files</p>
 *  <p>Copyright: 2003</p>
 *  <p>Company: CDS </p>
 *  @author Thomas Boch [CDS]
 *  @version 0.1: 20 mai 2003 - Création
 */

public class IDHAGenerator extends JFrame {

    Server server;

    static final String CR = Util.CR;

    //  on ne conserve que les fichiers FITS, VOTable, ASTRORES, IDHA, SIA,
    // scripts Aladin (détectés de manière non équivoque), AJ
    static long keptMask = MyInputStream.FITS | MyInputStream.VOTABLE | MyInputStream.ASTRORES |
        MyInputStream.IDHA | MyInputStream.SIA | MyInputStream.SSA |  MyInputStream.AJS | MyInputStream.AJ;


    private static final String BARATIN_OBSERVING_PROGRAM = "<DESCRIPTION>" +CR+
        	"This is a few resource information for the" +CR+
        	"ObservingProgram " +CR+
        	"</DESCRIPTION>" +CR+
        	"<FIELD ID=\"Name\" name=\"Name\" datatype=\"char\" ucd=\"ID_SURVEY\">" +CR+
        	"<DESCRIPTION> ObservingProgram Name </DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"Organisation\" name=\"Organisation\" datatype=\"char\" ucd=\"CURATOR\">" +CR+
        	"<DESCRIPTION> Name of Organisation(s) " +CR+
        	"performing Observing Program" +CR+
			"</DESCRIPTION>" +CR+
			"</FIELD>" +CR+
			"<FIELD ID=\"Begin\" name=\"beginning_date\" datatype=\"char\">" +CR+
			"<DESCRIPTION> Begin date of the survey" +CR+
			"</DESCRIPTION>" +CR+
			"</FIELD>" +CR+
			"<FIELD ID=\"End\" name=\"end date\" datatype=\"char\">" +CR+
			"<DESCRIPTION> End date of Observing program" +CR+
			"</DESCRIPTION>" +CR+
			"</FIELD>" +CR+
			"<FIELD ID=\"SpectralDomain\" name=\"SpectralCoverageName\" datatype=\"char\">" +CR+
			"<DESCRIPTION> General spectral domain (Optical X-ray ...)" +CR+
			"</DESCRIPTION>" +CR+
			"</FIELD>";

    private static final String BARATIN_OBSERVATION_GROUP = "<DESCRIPTION> This is a subset of images"+CR+
    		"belonging to a survey, an experiment,"+CR+
    		"and organised according to the same"+CR+
    		"common criterion. e.g. exemple of"+CR+
    		"criterion: color or wavelength,"+CR+
    		"polarimetry, etc... exemple of"+CR+
    		"Observation_Group: POSSII band J,"+CR+
    		"DENIS band K, etc.."+CR+
    		"</DESCRIPTION>"+CR+
    		"<FIELD ID=\"Selection_Criterion\" name=\"Selection_Criterion\" datatype=\"char\">"+CR+
    		"<DESCRIPTION> Sampled Parameter"+CR+
    		"</DESCRIPTION>"+CR+
    		"</FIELD>"+CR+
    		"<FIELD ID=\"Selection-Range\" name=\"Selection-Range\" datatype=\"char\">"+CR+
    		"<DESCRIPTION> Constraint on sampled parameter"+CR+
    		"</DESCRIPTION>"+CR+
    		"</FIELD>";

    private static final String BARATIN_OBSERVATION = "<DESCRIPTION>" +CR+
        	"This resource describes list of processed observations " +CR+
        	"</DESCRIPTION>" +CR+
        	"<FIELD ID=\"Observation_Name\" name=\"Observation_Name\" datatype=\"char\" ucd=\"ID_IMAGE\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Name of the Observation" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"ReferenceNumber\" name=\"ReferenceNumber\" datatype=\"char\" ucd=\"ID_IMAGE\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Reference Number of the Image" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"Size_alpha\" name=\"Size_alpha\" datatype=\"float\" precision=\"7\" width=\"11\" unit=\"deg\" ucd=\"INST_DET_SIZE\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Observation range in alpha (angular) " +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"Size_delta\" name=\"Size_delta\" datatype=\"float\" precision=\"F7\" width=\"11\" unit=\"deg\" ucd=\"INST_DET_SIZE\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Observation range in delta (angular) " +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"PixelSize\" name=\"Angular Pixel Size\"  datatype=\"float\" unit =\"deg\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Pixel size measured in sky units" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"Origin\" name=\"Origin\" datatype=\"char\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Data provider references" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"OriginalCoding \" name=\"OriginalCoding\" datatype=\"char\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Image coding provided by the data producer" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"AvailableCodings\" name=\"AvailableCodings\" datatype=\"char\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Codings which may be  produced on the fly" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"alpha\" name=\"CentralPoint_RA\" ucd=\"POS_EQ_RA_MAIN\"  datatype=\"float\" precision=\"F7\" width=\"11\" unit=\"deg\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Position of center	" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"delta\" name=\"CentralPoint_DEC\" ucd=\"POS_EQ_DEC_MAIN\"  datatype=\"float\" precision=\"F7\" width=\"11\" unit=\"deg\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Position of center	" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"date\"  name=\"DateAndTime\" datatype=\"char\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Observation date" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"AP\" name=\"Position Angle\"  datatype=\"float\" precision=\"F7\"  width=\"11\" unit=\"deg\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Position Angle of th Y axis" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>";

    private static final String BARATIN_STORAGE_MAPPING = "<DESCRIPTION>" +CR+
        	"This resource describes list of processed observations " +CR+
        	"</DESCRIPTION>" +CR+
        	"<FIELD ref=\"Observation_Name\" >" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"Cutout\" name=\"Organisation\"  datatype=\"char\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Status of cutout availability " +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"number\" name=\"NumberOfPatches\"  datatype=\"int\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Number of subimages" +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>" +CR+
        	"<FIELD ID=\"size\" name=\"Maximum size\"  datatype=\"int\">" +CR+
        	"<DESCRIPTION>" +CR+
        	"Maximum size " +CR+
        	"</DESCRIPTION>" +CR+
        	"</FIELD>";

   private static final String BARATIN_STORED_IMAGE = "<DESCRIPTION>" +CR+
      		"This resource describes the actual retrieved file " +CR+
      		"</DESCRIPTION>" +CR+
      		"<FIELD ref=\"Observation_Name\" >" +CR+
      		"</FIELD>" +CR+
      		"<FIELD ID=\"Location\" name=\"LinktoPixels\" datatype=\"char\">" +CR+
      		"<DESCRIPTION>" +CR+
      		"File location" +CR+
      		"</DESCRIPTION>" +CR+
      		"</FIELD>";


	// tableau des noms des champs qu'on écrit dans Observation
//    private static final String[] FIELD_NAME = {"CentralPoint_RA","CentralPoint_DE","Size_alpha","Size_delta","Position Angle"};
    // tableau des id des champs
    private static final String[] FIELD_ID = {"alpha","delta","Size_alpha","Size_delta","AP"};
    // infos supplémentaires
//    private static final String[] FIELD_MORE = {"ucd=\"POS_EQ_RA_MAIN\"  datatype=\"float\" unit=\"deg\"",
//							"ucd=\"POS_EQ_DEC_MAIN\"  datatype=\"float\" unit=\"deg\"",
//							"ucd=\"INST_DET_SIZE\" datatype=\"float\" unit=\"deg\"",
//							"ucd=\"INST_DET_SIZE\" datatype=\"float\" unit=\"deg\"",
//							"ucd=\"POS_POS-ANG\" datatype=\"float\" unit=\"deg\"" };


	// nom du fichier dans lequel on sauvegardera (et cherchera) les fichiers de description
	static final String SAVE_DIR_FILE = ".aladin_idha";



	// vecteur des progs d'observation
	private Vector progs = new Vector();
	// programme d'obs couramment sélectionné
	private ObsProg currentProg;
	// groupe d'obs couramment sélectionné
//	private ObsGroup currentGroup;
	// fichier couramment sélectionné
//	private Image currentImage;

	IDHAGenerator() {
		super("IDHA generator");
        Aladin.setIcon(this);

	}

    IDHAGenerator(Server server) {
        this();
        this.server = server;
    }

	/** Ajoute un programme d'observation
	 * 	@param s nom du programme d'observation
	 *  @return l'objet ObsProg créé
	 */
    private ObsProg addObsProg(String s) {
		if(s.length()>0) {
			// création du nouveau prog d'obs et ajout dans progs
			ObsProg prog = new ObsProg(s);
			progs.addElement(prog);
			// ajout dans la liste
            // uniquement pour utilisation de la classe en Standalone
			//progList.add(prog.name);

			if( currentProg==null ) {
				currentProg = prog;
                // uniquement pour utilisation de la classe en Standalone
				//progList.select(0);
			}

			return prog;
		}
		return null;
	}

	private static final String allFiles = "*";

	/**
	 * Remove all structures in class
	 */
	protected void reset() {
	    progs.removeAllElements();
	}

	/** retourne un stream décrivant la structure des fichiers dans startDir
	 * Méthode appelée à partir d'Aladin
	 * On recherche d'abord si un fichier décrivant ce répertoire existe déja
	 * @param startDir répertoire de départ
	 * @return InputStream
	 */
	protected InputStream getStream(File startDir,Component c) {
	    // première étape : on recherche un fichier de descrition des données de startDir
	    File descFile = buildDescFile(startDir);
	    if( descFile.exists() ) {
	    	int answer = JOptionPane.showConfirmDialog(c, "Do you want to use existing description file ?",
                  "Confirmation",JOptionPane.YES_NO_OPTION );
	    	if( answer==JOptionPane.YES_OPTION ) {
	    	    try {
	    	        Aladin.trace(3,"Return stream from existing description file");
	    	    	return new BufferedInputStream(new FileInputStream(descFile));
	    	    }
	    	    catch(Exception e) {
	    	        return new BufferedInputStream(new ByteArrayInputStream("".getBytes()));
	    	    }
	    	}
	    }
	    if( server!=null ) server.waitCursor();
	    Aladin.trace(3,"Build description file for directory "+startDir);
	    doScan(startDir);
	    StringBuffer vot = generateXML();
	    // on écrit dans descFile
        try {
            DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(descFile)));
                // on utilise writeBytespour éviter d'avoir un encodage 16-bit Unicode
                Aladin.trace(3,"Write file "+descFile);
                out.writeBytes(vot.toString());
                out.close();
        }
        catch(IOException e) {System.out.println("Could not produce write description file "+descFile+" : "+e);}

        if( server!=null ) server.defaultCursor();
	    // on renvoie le stream
        return new BufferedInputStream(new ByteArrayInputStream(vot.toString().getBytes()));
	}


	private File buildDescFile(File dir) {
	    return new File(dir+File.separator+SAVE_DIR_FILE);
	}

	/** Checks whether file is a known format
	 *
	 * @param file file to test
	 * @return boolean true if file has a known format, false otherwise
	 */
	private boolean checkFile(File file) {
	   MyInputStream mis =null;
		try {
			mis = new MyInputStream(new FileInputStream(file));
			mis = mis.startRead();
			long type = mis.getType();
            //System.out.println("type de "+file.getName()+" : "+MyInputStream.decodeType(type));
			
            return (type&keptMask) != 0;
		}
		catch(Exception e) {return false;}
		finally { if( mis!=null ) try { mis.close(); } catch( Exception e1 ) {}}
	}

	/** PRE : dir est un répertoire */
	private void doScan(File startDir) {
        Aladin.trace(3,"Setting start directory: "+startDir);

        getFilesAndDirs(startDir);

        File[] obsProgDirs = dirs;
        File[] filesFirstLevel = files;

        // on crée un ObsProg spécial pour les fichiers du premier niveau
        ObsProg opFantome = addObsProg("First level files obsprog");
        opFantome.hide = true;
        ObsGroup group = new ObsGroup("");
        opFantome.groups.addElement(group);
        Image curImage;
        for( int i=0; i<filesFirstLevel.length; i++ ) {
        	if( checkFile(filesFirstLevel[i]) ) {
        		curImage = new Image(filesFirstLevel[i]);
        		group.images.addElement(curImage);
            	processImage(curImage);
        	}
        }


        Aladin.trace(3,"Retrieving Observing programs directories: "+obsProgDirs.length+" directories found");

        String progName;
        ObsProg curProg;
        // boucle sur les répertoires d'ObsProg
        for( int i=0; i<obsProgDirs.length; i++ ) {
            progName = obsProgDirs[i].getName();
            Aladin.trace(3,"Adding Observation Program "+progName);
            curProg = addObsProg(progName);
            fillObsProg(curProg,new Vector(),obsProgDirs[i],0);

        }
        currentProg = null;

        cleanup();
	}


    /** fait le menage en supprimant les ObsProg qui n'ont pas d'ObsGroup
     * (ie supprime les "repertoires" vides)
     */
    private void cleanup() {
        Vector toDel = new Vector();
        ObsProg op;
        Enumeration e = progs.elements();
        while( e.hasMoreElements() ) {
            op = (ObsProg) e.nextElement();
            if( op.groups.size()==0 ) toDel.addElement(op);
        }

        e = toDel.elements();
        while( e.hasMoreElements() )
            progs.removeElement(e.nextElement());
    }

	/** Scan un répertoire et construit la structure correspondante */
	private void scanDirectory() {
        FileDialog fd = new FileDialog(this,"Go into the start directory",FileDialog.LOAD);
        fd.setFile(allFiles);
        fd.show();
        String dir = fd.getDirectory();
        String name =  fd.getFile();
        String s = (dir==null?"":dir)+(name==null?"":name);
        if( name==null ) return;

        File startDir = new File(dir);
        doScan(startDir);
        /*
        Aladin.trace(3,"Setting start directory: "+startDir);

        File[] obsProgDirs = getDirs(startDir);
        Aladin.trace(3,"Retrieving Observing programs directories: "+obsProgDirs.length+" directories found");

        String progName;
        ObsProg curProg;
        // boucle sur les répertoires d'ObsProg
        for( int i=0; i<obsProgDirs.length; i++ ) {
            progName = obsProgDirs[i].getName();
            Aladin.trace(3,"Adding Observation Program "+progName);
            curProg = addObsProg(progName);
            fillObsProg(curProg,new Vector(),obsProgDirs[i],0);

        }
        currentProg = null;
        */
	}

	private void fillObsProg(ObsProg prog, Vector values, File dir, int level) {
	    // check whether it is a terminal dir (ie no more subdir)
	    getFilesAndDirs(dir);
	    File[] subdirs = dirs;
	    // *** cas terminal ***, on crée un groupe d'observation, on ajoute les fichiers
	    if( subdirs==null || subdirs.length==0 ) {
	        ObsGroup group = new ObsGroup("group"+prog.groups.size());
	        // ajout de toutes les valeurs de critères
	        for( int i=0; i<values.size(); i++ ) {
	            group.criteriaValues.put(prog.criteriaClasses.elementAt(i),values.elementAt(i));
	        }
	        // ajout des fichiers
	        String[] list = dir.list();
            FilterProperties.sortLexico(list);
	        Image curImage;
	        for( int i=0; i<list.length; i++ ) {
	        	File f = new File(dir,list[i]);
	        	if( checkFile(f) ) {
	            	curImage = new Image(f);
	        		if( curImage.file.getName().indexOf(SAVE_DIR_FILE)>=0 ) continue;
	            	group.images.addElement(curImage);
	            	processImage(curImage);
	        	}
	        }
            if( group.images.size()>0 ) prog.groups.addElement(group);
	    }
	    // *** récursion ***
	    else {
	        String newCrit = "criterion"+level;
            if( prog.criteriaClasses.indexOf(newCrit)<0 ) {
            	prog.criteriaClasses.addElement(newCrit);
                Aladin.trace(3,"Adding constraint "+newCrit);
            }

	        for( int i=0; i<subdirs.length; i++ ) {
	        	Vector v = new Vector();
	        	Enumeration enumVal = values.elements();
	        	while(enumVal.hasMoreElements()) {
	        		v.addElement(enumVal.nextElement());
	        	}
                v.addElement(subdirs[i].getName());
                Aladin.trace(3,"Adding constraint value "+subdirs[i].getName());
	        	fillObsProg(prog,v,subdirs[i],level+1);
	        }
	    }
	}

	/** Lit dans le header fits associé à image pour en extraire les infos nécessaire :
	 * 	centre de l'image, taille, etc ...
	 */
	private void processImage(Image image) {
        HeaderFits hf = null;
        MyInputStream myStream = null;
	    try {
            myStream = new MyInputStream(new FileInputStream(image.file));
            myStream = myStream.startRead();
            hf = new HeaderFits(myStream);
	    }
	    catch(Exception e) {Aladin.trace(3,"Error : could not create HeaderFits object");return;}
        finally { if( myStream!=null ) try { myStream.close(); } catch( Exception e1) {}}
	    
	    Calib calib = null;
	    try {
	        calib = new Calib(hf);
	    }
	    catch(Exception e) {Aladin.trace(3,"Error : could not create Calib object");return;}

	    Coord coo;
	    try {
	    	coo = calib.getImgCenter();
	    }
	    catch(Exception e) {Aladin.trace(3,"Error while retrieving image center");coo=null;}

	    if( coo!=null ){
	        image.values[0] = Float.toString((float)coo.al);
	        image.values[1] = Float.toString((float)coo.del);
	    }

	    image.values[2] = Float.toString((float)calib.getImgWidth());
	    image.values[3] = Float.toString((float)calib.getImgHeight());
	    // faut il multiplier cette valeur par -1 ???
	    // --> surement que non, l'erreur est dans Calib
	    image.values[4] = Float.toString((float)(calib.getProjRot()));
	}

	File[] dirs;
	File[] files;
	// remplit dirs et files
	private void getFilesAndDirs(File dir) {
	    String[] list = dir.list();
        FilterProperties.sortLexico(list);

	    File curFile;
	    Vector v = new Vector();
	    Vector f = new Vector();
        for( int i=0; i<list.length; i++ ) {
        	curFile = new File(dir, list[i]);
            if( curFile.isDirectory() ) {
                v.addElement(curFile);
            }
            else {
                // on ne souhaite pas récupérer le fichier de description
                if( curFile.getName().indexOf(SAVE_DIR_FILE)<0 )
                	f.addElement(curFile);
            }
        }

        dirs = new File[v.size()];
        v.copyInto(dirs);
        v = null;

        files = new File[f.size()];
        f.copyInto(files);
        f = null;


	}


	/**
	 * Ecrit des <td></td> nb fois
	 * @param sb le StringBuffer ou l'on écrit
	 * @param nb le nombre de fois que l'on répète <td></td>
	 */
	private void writeEmptyTD(StringBuffer sb, int nb) {
	    for( int i=0; i<nb; i++ ) {
	        sb.append("<TD></TD>");
	    }
	}

	/** Génère le XML à partir des données entrées par l'utilisateur */
	private StringBuffer generateXML() {
		boolean firstProg=true;
		boolean firstGroup=true;
		boolean firstObs=true;
		boolean firstStorageMapping=true;
		boolean firstStoredImage=true;

		StringBuffer ret = new StringBuffer();

		ret.append("<?xml version=\"1.0\"?>"+CR+"<!DOCTYPE VOTABLE SYSTEM \"http://us-vo.org/xml/VOTable.dtd\">");
		ret.append(CR+"<VOTABLE ID=\"v1.0\">");

		// itération sur les ObsProg
		for( Enumeration eProg = progs.elements(); eProg.hasMoreElements(); ) {
			ObsProg curProg = (ObsProg)eProg.nextElement();

			if( !curProg.hide ) {


			ret.append(CR+"<RESOURCE name=\"ObservingProgram\">");
			if( firstProg ) {
				ret.append(CR+"<TABLE ID=\"ObservingProgram\">");
				ret.append(CR+BARATIN_OBSERVING_PROGRAM);
				//ret.append(CR+"<FIELD ID=\"Name\" name=\"Name\" datatype=\"char\" ucd=\"ID_SURVEY\" /> ");
				firstProg=false;
			}
			else {
				ret.append(CR+"<TABLE ref=\"ObservingProgram\">");
			}

			ret.append(CR+"<DATA><TABLEDATA>");
			//ret.append(CR+"<TR><TD>"+curProg.name+"</TD></TR>");
			ret.append(CR+"<TR>");
			ret.append("<TD>"+curProg.name+"</TD>");
			// on remplit avec des blancs pour les FIELD dont on n'a pas la valeur
			writeEmptyTD(ret, 4);
            ret.append("</TR>");
			ret.append(CR+"</TABLEDATA></DATA>");
			ret.append(CR+"</TABLE>");

			}

			// itération sur les ObsGroup de curProg
			for( Enumeration eGroup = curProg.groups.elements(); eGroup.hasMoreElements(); ) {
				ObsGroup curGroup = (ObsGroup)eGroup.nextElement();
				ret.append(CR+"<RESOURCE name=\"Observation_Group\">");
				if( firstGroup ) {
					ret.append(CR+"<TABLE ID=\"Observation_Group\">");
					ret.append(CR+BARATIN_OBSERVATION_GROUP);
					//ret.append(CR+"<FIELD ID=\"Selection_Criterion\" name=\"Selection_Criterion\" datatype=\"char\" />");
					//ret.append(CR+"<FIELD ID=\"Selection-Range\" name=\"Selection-Range\" datatype=\"char\" />");
					firstGroup=false;
				}
				else {
					ret.append(CR+"<TABLE ref=\"Observation_Group\">");
				}

				ret.append(CR+"<DATA><TABLEDATA>");
				// itération sur les classes de critères
				Enumeration eCrit;
				for( eCrit = curProg.criteriaClasses.elements(); eCrit.hasMoreElements(); ) {
					String crit = (String)eCrit.nextElement();
					ret.append(CR+"<TR><TD>"+crit+"</TD><TD>"+curGroup.criteriaValues.get(crit)+"</TD></TR>");
				}

				// si aucun critère n'existe, on met le critère "groupe" avec pour valeur le nom du groupe
				if( curProg.criteriaClasses.size()==0 && !curProg.hide ) {
                    ret.append(CR+"<TR><TD>Group</TD><TD>"+curGroup.name+"</TD></TR>");
				}


				ret.append(CR+"</TABLEDATA></DATA>");
				ret.append(CR+"</TABLE>");


                // début Observation
                ret.append(CR+"<RESOURCE name=\"Observation\">");
                if( firstObs ) {
                    ret.append(CR+"<TABLE ID=\"Observation\">");
                    ret.append(CR+BARATIN_OBSERVATION);
                    //ret.append(CR+"<FIELD ID=\"Observation_Name\" name=\"Observation_Name\" datatype=\"char\" ucd=\"ID_IMAGE\" />");
                    //for( int i=0; i<FIELD_ID.length; i++ ){
                    //    ret.append(CR+"<FIELD ID=\""+FIELD_ID[i]+"\" name=\""+FIELD_NAME[i]+"\" "+FIELD_MORE[i]+" />");
                    //}
                    firstObs=false;
                }
                else {
                    ret.append(CR+"<TABLE ref=\"Observation\">");
                }

                ret.append(CR+"<DATA><TABLEDATA>");

				// itération sur les fichiers contenus dans curGroup
				for( Enumeration eFile = curGroup.images.elements(); eFile.hasMoreElements(); ) {
					Image image= (Image)eFile.nextElement();


					ret.append(CR+"<TR>");
                  	// on met le nom du fichier comme nom de la ressource
					ret.append("<TD>"+buildName(image.file)+"</TD>");
					// 1 blanc
					writeEmptyTD(ret,1);
					// taille en alpha
                    ret.append("<TD>"+image.values[2]+"</TD>");
					// taille en delta
                    ret.append("<TD>"+image.values[3]+"</TD>");
                    // 4 blancs
                    writeEmptyTD(ret,4);
                    // position centre alpha
                    ret.append("<TD>"+image.values[0]+"</TD>");
                    // position centre delta
                    ret.append("<TD>"+image.values[1]+"</TD>");
                    // 1 blanc
                    writeEmptyTD(ret,1);
                    // angle d eposition
                    ret.append("<TD>"+image.values[4]+"</TD>");

					// impression des infos sur l'image (taille, centre, etc)
					//for( int i=0; i<image.values.length; i++ ) {
					//    ret.append("<TD>"+image.values[i]+"</TD>");
					//}

					ret.append(CR+"</TR>");
				}
				ret.append(CR+"</TABLEDATA></DATA>");
				ret.append(CR+"</TABLE>");
				ret.append(CR+"</RESOURCE>"); // fin Observation

				// début StorageMapping (ne sert à rien pour le moment)
				ret.append(CR+"<RESOURCE name=\"StorageMapping\">");
				if( firstStorageMapping ) {
					ret.append(CR+"<TABLE ID=\"StorageMapping\">");
					ret.append(CR+BARATIN_STORAGE_MAPPING);
					// On répète le nom
					/*
					ret.append(CR+"<FIELD ref=\"Observation_Name\" />");
					ret.append(CR+"<FIELD ID=\"Location\" name=\"Location\" datatype=\"char\" />");
					*/
					firstStorageMapping = false;
				}
				else {
					ret.append(CR+"<TABLE ref=\"StorageMapping\">");
				}

				ret.append(CR+"<DATA><TABLEDATA>");
				for( Enumeration eFile = curGroup.images.elements(); eFile.hasMoreElements(); ) {
					File file = ((Image)eFile.nextElement()).file;
					/*String path;
					try {
						path = file.getCanonicalPath();
					}
					catch(IOException ioe) {
						path = "";
					}*/
					//ret.append(CR+"<TR><TD>"+file.getName()+"</TD><TD>file:/"+path+"</TD></TR>");
                    ret.append(CR+"<TR>");
                    // on répète le nom de l'observation
                    ret.append("<TD>"+buildName(file)+"</TD>");
                    // 3 blancs
                    writeEmptyTD(ret,3);
                    ret.append("</TR>");
                }
                ret.append(CR+"</TABLEDATA></DATA>");
				ret.append(CR+"</TABLE>");
				ret.append(CR+"</RESOURCE>"); // fin StorageMapping

                // début StoredImage
                ret.append(CR+"<RESOURCE name=\"StoredImage\">");
                if( firstStoredImage ) {
                    ret.append(CR+"<TABLE ID=\"StoredImage\">");
                    ret.append(CR+BARATIN_STORED_IMAGE);
                    // On répète le nom
                    //ret.append(CR+"<FIELD ref=\"Observation_Name\" />");
                    //ret.append(CR+"<FIELD ID=\"Location\" name=\"LinktoPixels\" datatype=\"char\" />");
                    firstStoredImage = false;
                }
                else {
                    ret.append(CR+"<TABLE ref=\"StoredImage\">");
                }

                ret.append(CR+"<DATA><TABLEDATA>");
                for( Enumeration eFile = curGroup.images.elements(); eFile.hasMoreElements(); ) {
                    File file = ((Image)eFile.nextElement()).file;
                    String path;
                    try {
                        //path = java.net.URLEncoder.encode(file.getCanonicalPath());
                        path = file.getCanonicalPath();
                    }
                    catch(IOException ioe) {
                        path = "";
                    }
                    ret.append(CR+"<TR><TD>"+buildName(file)+"</TD><TD>file://"+path+"</TD></TR>");
                }
                ret.append(CR+"</TABLEDATA></DATA>");
                ret.append(CR+"</TABLE>");
                ret.append(CR+"</RESOURCE>"); // fin StoredImage


				ret.append(CR+"</RESOURCE>"); // fin Observation_Group
			}


			if( !curProg.hide ) ret.append(CR+"</RESOURCE>"); // fin ObservingProgram
		}

        ret.append(CR+"</VOTABLE>");

		return ret;
	}

	private String buildName(File file) {
		String name = file.getName();
        return name;
        /*
		if( name.endsWith(".fits") || name.endsWith(".FITS") ) {
		    return name.substring(0,name.length()-5);
		}
		else {
		    return name;
		}
        */
	}


	class ObsProg {
	    // si true, il s'agit d'un ObsProg "fantôme"
	    boolean hide = false;
		// groupes que comprend un ObsProg
		Vector groups = new Vector();
		// classes de critères (eg : epoch, filter)
		Vector criteriaClasses = new Vector();
		String name;

		ObsProg(String name) {this.name = name;}
	}

	class ObsGroup {
		// vecteur des images que contient un ObsGroup
		Vector images = new Vector();
		// valeurs associées aux critères (eg : epoch1, filtre untel)
		Hashtable criteriaValues = new Hashtable();
		String name;

		ObsGroup(String name) {this.name = name;}
	}

	class Image {
	    File file;
	    String[] values;

	    Image(File file) {
	        this.file = file;
	        values = new String[FIELD_ID.length];
	        for( int i=0; i<values.length; i++ ) values[i]="";
	    }
	}

//  uniquement pour utilisation de la classe en Standalone
/*
	public static void main(String[] args) {
		IDHAGenerator generator = new IDHAGenerator();
		generator.show();
	}
*/
}
