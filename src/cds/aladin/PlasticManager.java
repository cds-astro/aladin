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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.votech.plastic.PlasticHubListener;
import org.votech.plastic.PlasticListener;

import cds.tools.Util;
import net.ladypleaser.rmilite.Client;
import uk.ac.starlink.plastic.MinimalHub;
import uk.ac.starlink.plastic.ServerSet;



/** Classe gérant toutes les connexions avec le hub PLASTIC (PLatform for AStronomical Tools InterCommunication)
 * Cette classe se charge de la connexion avec le hub, et permet d'envoyer des messages au hub
 *
 * Le fait de faire passer tout ce qui est relatif à PLASTIC par une classe dédiée permet de
 * passer relativement facilement d'une implémentation du protocole à une autre
 *
 * Les classes modifiées ou créées pour supporter PLASTIC sont les suivantes (package cds.aladin) :
 *
 * - Aladin : l'ajout des fonctionnalités PLASTIC est subordonné à la variable Aladin.PLASTIC_SUPPORT, qui pour le moment prend la valeur de Aladin.PROTO
 *    --> ajout d'un menu PLASTIC (variable MPLASTIC) permettant de s'inscrire/désinscrire auprès du hub (dans sous-menu SMPLASTIC)
 *    --> modif. dans méthode quit() : on se désincrit du hub avant de quitter l'appli
 *    --> modif. dans suiteInit() : mise à jour de l'état des items du sous-menu PLASTIC
 *    --> modif dans action(Event,Object) : gestion des évts inscription/désinscription
 *    --> ajout de la méthode setPlasticButtonMode()
 *    --> ajout de la méthode broadcastSelectedPlanes
 *
 * - Calque :
 *    --> modif. de selectAllObjectInPlans() : envoi d'un message "showObjects" lors de la sélection de tous les objets d'un plan
 *
 * - Localisation :
 *    --> modif. de la méthode setPos(...) pour permettre l'envoi du message pointAtCoords
 *        (cette méthode est appelée dans ViewSimple.mouseUp(Event,int,int)
 *
 * - PlasticManager (cette classe) : implémente l'interface PlasticListener
 *    --> gère la registration/unregistration auprès du hub PLASTIC
 *    --> gère globalement l'envoi et la réception des messages PLASTIC
 *    On récupère le singleton de cette classe via la méthode statique getSingleton(Aladin)
 *
 * - Select :
 *    --> ajout d'items dans le popup pour permettre le broadcast de plans
 *    --> ajout des actions correspondant à ces items
 *
 * - View :
 *    --> ajout d'une méthode selectSourcesByRowNumber(PlanCatalog pc, int[] rowIdx) pour sélectionner des objets d'après leur numéro d'ordre dans leur plancatalog
 *    --> modif. de setMesure() pour envoyer le message "showObjects" lors de la sélection d'une ou plusieurs sources
 *    --> ajout de getSelectedSources()
 *    --> modif. de showSource(Source o) : ajout de l'envoi du message approprié
 *
 * - ViewSimple :
 *    --> petite modif dans mouseUp(Event,int,int)
 *
 *
 *
 * @author Thomas Boch [CDS]
 *
 * @version 0.75 Ajout du message MSG_HIGHLIGHT_OBJECT
 *          0.7  Ajout des méthodes broadcastTable et broadcastImage. Les broadcast se font désormais sur demande, et plus automatiquement
 *               Broadcaste à tout le monde ou à une liste d'applications
 *               Les URLs broadcastés sont désormais correctes (conformes à la RFC 1738)
 *          0.6  Support de tout hub PLASTIC conforme au standard. Plus de dépendance avec les librairies ACR
 *          0.55 Le message MSG_SELECT_OBJECTS envoie un tableau d'int désormais. En réception, on garde le cas tableau de long "au cas où"
 *          0.5  Ajout de messages (messages envoyés ET messages traités)
 *          0.1  Creation, 02 Dec 2005
 *
 * @see <a href="http://plastic.sourceforge.net/">PLASTIC specification</a>
 *
 */
public class PlasticManager implements PlasticListener, AppMessagingInterface {
	// list of supported messages (see http://wiki.eurovotech.org/bin/view/VOTech/DsSixPlasticInterface for more details)
	static final protected URI MSG_ECHO = URI.create("ivo://votech.org/test/echo");
	static final protected URI MSG_APP_REGISTERED = URI.create("ivo://votech.org/hub/event/ApplicationRegistered");
	static final protected URI MSG_APP_UNREGISTERED = URI.create("ivo://votech.org/hub/event/ApplicationUnregistered");
	static final protected URI MSG_GETNAME = URI.create("ivo://votech.org/info/getName");
	static final protected URI MSG_GETIVORN = URI.create("ivo://votech.org/info/getIVORN");
	static final protected URI MSG_GETVERSION = URI.create("ivo://votech.org/info/getVersion");
	static final protected URI MSG_HUBSTOPPING = URI.create("ivo://votech.org/hub/event/HubStopping");
	static final protected URI MSG_EXCEPTION = URI.create("ivo://votech.org/hub/Exception");
	static final protected URI MSG_LOAD_VOT = URI.create("ivo://votech.org/votable/load");
	static final protected URI MSG_LOAD_VOT_FROM_URL = URI.create("ivo://votech.org/votable/loadFromURL");
	static final protected URI MSG_SELECT_OBJECTS = URI.create("ivo://votech.org/votable/showObjects");
	static final protected URI MSG_HIGHLIGHT_OBJECT = URI.create("ivo://votech.org/votable/highlightObject");
	static final protected URI MSG_GET_ICON_URL = URI.create("ivo://votech.org/info/getIconURL");
	static final protected URI MSG_LOAD_FITS = URI.create("ivo://votech.org/fits/image/loadFromURL");
	static final protected URI MSG_POINT_AT_COORDS = URI.create("ivo://votech.org/sky/pointAtCoords");
	static final protected URI MSG_GET_DESC = URI.create("ivo://votech.org/info/getDescription");
	// TODO : en test (pour les spectres)
	static final protected URI MSG_LOAD_FITS_LINE_FROM_URL = URI.create("ivo://votech.org/fits/line/loadFromURL");
	static final protected URI MSG_LOAD_SPECTRUM_FROM_URL = URI.create("ivo://votech.org/spectrum/loadFromURL");
	// TODO : juste pour un test, à supprimer après
//	static final protected URI MSG_TEST = URI.create("ivo://test");
	// message non "officiel", permet l'envoi de cmdes script à Aladin, via PLASTIC
	static final protected URI MSG_SEND_ALADIN_SCRIPT_CMD = URI.create("ivo://votech.org/aladin/sendScript");
	// Registry resources related messages
    static final protected URI MSG_LOAD_VORESOURCE = URI.create("ivo://votech.org/voresource/load");
    static final protected URI MSG_LOAD_VORESOURCE_LIST = URI.create("ivo://votech.org/voresource/loadList");
    // Characterization message
    static final protected URI MSG_LOAD_CHARAC_FROM_URL = URI.create("ivo://votech.org/charac/loadFromURL");

	// liste des messages supportés (i.e auxquels on répond)
	static final protected URI[] SUPPORTED_MESSAGES = {MSG_ECHO, MSG_GETNAME, MSG_GETIVORN, MSG_GETVERSION, MSG_HUBSTOPPING, MSG_EXCEPTION,
			                      MSG_LOAD_VOT, MSG_LOAD_VOT_FROM_URL, MSG_SELECT_OBJECTS, MSG_GET_ICON_URL, MSG_LOAD_FITS,
			                      MSG_APP_REGISTERED, MSG_APP_UNREGISTERED, MSG_POINT_AT_COORDS, MSG_GET_DESC, MSG_HIGHLIGHT_OBJECT,
								  MSG_SEND_ALADIN_SCRIPT_CMD,MSG_LOAD_VORESOURCE,MSG_LOAD_VORESOURCE_LIST/*MSG_TEST*/};

	// supported plastic version
	static final protected String SUPPORTED_VERSION = "0.4";


	// answer to getName message
	static final protected String ALADIN_NAME = "Aladin";

	// Aladin's IVORN (TODO : à modifier quand Aladin sera enregistré auprès d'un registry !)
	static final protected String ALADIN_IVORN = "ivo://CDS/Aladin";

	// fichier de conf
	static final protected String PLASTIC_CONF_FILE = ".plastic";



	private int curState = NO_PLASTIC;

	// TODO: on pourrait récupérer cette description par la classe Chaine (Aladin.string0)
	// description of what Aladin is and does
	static final protected String ALADIN_DESC = "ALADIN is an interactive software "+
		"sky atlas developed by the CDS, allowing one to visualize digitized images"+
		"of any part of the sky, to superimpose entries from astronomical catalogs,"+
		"and to interactively access related data and information.";

    // les différentes chaines nécessaires
	static String LAUNCH_INTERNAL_HUB, CANT_CONNECT, CANT_LAUNCH_HUB, HUB_STOP,
	              EXCEPTION, CONFIRM_STOP_HUB;

	// référence à Aladin
	private Aladin a;

	// true if an instance of PlasticManager has been created
	static protected boolean callToPlastic = false;

	// variables de travail
	private PlasticHubListener plasticHubListener;
	private boolean isRegistered = false;

	// l'id assigné par le hub
	private URI aladinID;

	// référence au widget reflétant l'état de la connexion
	private PlasticWidget widget;

	/** Constructeur */
	protected PlasticManager(Aladin a) {
		this.a = a;
		callToPlastic = true;
		createChaine();
	}

	// TODO : chercher toutes les chaines comportant "SAMP" ou "PLASTIC" et leur appliquer ce traitement !!

	private void createChaine() {
	    String name = getProtocolName();

		LAUNCH_INTERNAL_HUB = a.chaine.getString("PMLAUNCHHUB").replaceAll("SAMP", name);
		CANT_CONNECT = a.chaine.getString("PMCANTCONNECT").replaceAll("SAMP", name);
		CANT_LAUNCH_HUB = a.chaine.getString("PMCANTLAUNCHHUB").replaceAll("SAMP", name);
		HUB_STOP = a.chaine.getString("PMHUBWILLSTOP").replaceAll("SAMP", name);
		EXCEPTION = a.chaine.getString("PMEXCEPTION").replaceAll("SAMP", name);
		CONFIRM_STOP_HUB = a.chaine.getString("PMCONFIRMSTOPHUB").replaceAll("SAMP", name);
	}

	/**
	 * Register with the plastic hub
	 *
     * @parameter silent if true, in silent mode (no popup menu when register fails !)
     * @param launchHubIfNeeded si silent==true, on essaye automatiquement de lancer un hub s'il n'en existe pas. Sinon, on passe par une boite de dialogue de confirmation
	 * @return true if Aladin was registered, false otherwise
	 */
	public boolean register(boolean silent, boolean launchHubIfNeeded) {
		trace("Try to register Aladin with the plastic hub");

		if( isRegistered() ) {
			trace("Aladin was already registered !");

			return true;
		}

		if( !getHubListener(silent, launchHubIfNeeded) ) {
			trace("Could not register to the Plastic hub");
			return false;
		}

		boolean pbOccured = false;
//		aladinID = plasticHubListener.registerRMI("Aladin", new ArrayList(), a);
		try {
			aladinID = plasticHubListener.registerRMI("Aladin", Arrays.asList(SUPPORTED_MESSAGES), this);
		}
		catch(Exception e) {pbOccured = true;}

		if( pbOccured ) isRegistered = false;
		else {
			isRegistered = true;
			Aladin.trace(3, "Successful registration with the plastic hub with application ID "+aladinID);
		}


//		System.out.println(plasticHubListener.getRegisteredIds().size());
		if( widget!=null ) widget.updateStatus(isRegistered());
		updateState();

		return true;
	}

    /** do we support a given message ? */
    private boolean isSupporting(URI message) {
        for( int i=0; i<SUPPORTED_MESSAGES.length; i++ ) {
            if( SUPPORTED_MESSAGES[i].equals(message) ) return true;
        }

        return false;
    }

    public boolean unregister() {
    	return unregister(false);
    }

    public boolean unregister(boolean force) {
    	return unregister(force, true);
    }

	/**
	 * Unregister from the plastic hub
	 *
	 * @param force on force la désinscription
	 *
	 * @return true if Aladin is unregistered, false otherwise
	 */
	public boolean unregister(boolean force, boolean destroyInternalHub) {
		trace("Try to unregister Aladin from the plastic hub");

		// TODO : test plus pointu !!
		if( !isRegistered() ) {
			trace("Aladin is not registered with the hub, no need to unregister !");
			return true;
		}

		if( !getHubListener(false) ) {
			trace("Could not unregister from the Plastic hub");
			return false;
		}

		try {
			plasticHubListener.unregister(aladinID);
		}
		catch( Exception e) {
			trace("Error while trying to unregister : "+e.getMessage());
			if( !force ) return false;
		}

		isRegistered = false;
		plasticHubListener = null;
		aladinID = null;

		// arrêt du hub interne
		if( destroyInternalHub ) stopInternalHub(force);

		if( widget!=null ) widget.animateWidgetReceive(true, false);
		updateState();

		Aladin.trace(3, "Successful unregistration from the plastic hub");

        return true;
//		System.out.println(plasticHubListener.getRegisteredIds().size());
	}

	public boolean isRegistered() {
		return isRegistered;
	}

	// référence au hub interne
	static private MinimalHub internalHub;

	/**lancement du hub PlasKit, en interne
	 *
	 * @return true si tout s'est bien passé, false sinon
	 */
	public synchronized boolean startInternalHub() {
		Aladin.trace(1, "Starting an internal PlasKit hub");
		try {
			ServerSet servers =  new ServerSet(new File(System.getProperty("user.home"), PlasticHubListener.PLASTIC_CONFIG_FILENAME));
			internalHub = new MinimalHub(servers);

		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		updateState();
		return true;
	}

	public synchronized void stopInternalHub(boolean dontAsk) {
		if( internalHub==null ) return;
		List registeredApps = internalHub.getRegisteredIds();

		// si il y a encore d'autres applis connectés au hub interne, on demande si on souhaite arrêter le hub
		if( ! dontAsk && ! ( registeredApps.size()==0 || (registeredApps.size()==1 && registeredApps.get(0).equals(internalHub.getHubId())) ) ) {
			if( ! Aladin.confirmation(widget, CONFIRM_STOP_HUB) ) return;
		}

		Aladin.trace(1, "Stopping internal PlasKit hub");

		internalHub.stop();
		internalHub = null;

		updateState();
	}

	private boolean getHubListener(boolean silent) {
		return getHubListener(silent, false);
	}

	/**
	 * Cherche le fichier de conf .plastic, lit le port RMI à interroger et récupère l'objet PlasticHubListener
     * @param silent if true, in silent mode (no popup menu to indicate failure)
     * @param launchHubIfNeeded si silent==true, on essaye automatiquement de lancer un hub s'il n'en existe pas. Sinon, on passe par une boite de dialogue de confirmation
	 * @return <i>true</i> si on a pu récupérer le PlasticHubListener, <i>false</i> sinon
	 */
	private boolean getHubListener(boolean silent, boolean launchHubIfNeeded) {
		if( plasticHubListener!=null ) return true;

		trace("Looking for an existing .plastic file");
		File confFile = getLockFile();

		if( !confFile.exists() ) {
			trace("Can't find .plastic file, can't registrate with a PLASTIC hub");

			if( launchHubIfNeeded ) {
				// on demande si on souhaite lancer un hub "interne"
				boolean startHub = false;
				if( !silent ) {
					startHub = Aladin.confirmation(a, LAUNCH_INTERNAL_HUB);
					if( ! startHub ) return false;
				}
				// sinon, on lance un hub manu militari
				else startHub = true;

				if( startHub ) {
					if( !startInternalHub() && !silent ) Aladin.warning(CANT_LAUNCH_HUB);
					// tout s'est bien déroulé, on attend 1 seconde, et on tente de se connecter au hub créé
					else {
						Util.pause(1000);

						return getHubListener(true);
					}
				}
			}

//			if( !silent ) Aladin.warning("Could not find a running PLASTIC hub !\n(missing .plastic file)");
			return false;
		}

		// lecture du fichier de conf
		trace("Reading the .plastic conf file to retrieve the RMI port number");
		Properties prop = new Properties();
		FileInputStream fin = null;
		try {
			prop.load(fin = new FileInputStream(confFile));
			fin.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			if( fin!=null ) try {fin.close();} catch(IOException ioe2) {}
			return false;
		}

		String rmiPortStr = prop.getProperty("plastic.rmi.port");

		if( rmiPortStr==null ) {
			trace("Can not find property 'plastic.rmi.port' !");
			if( !silent ) Aladin.warning(CANT_CONNECT);
			return false;
		}
		int rmiPort;
		try {
			rmiPort = Integer.valueOf(rmiPortStr).intValue();
		}
		catch(NumberFormatException nfe) {
			trace("Can not convert the property 'plastic.rmi.port' to integer !");
			if( !silent ) Aladin.warning(CANT_CONNECT);
			return false;
		}


//		 PF 28 nov 06 : j'ai supprimé l'instanciation par réfléxion parce que ça me pose
//		 un problème lors de l'obfuscation. Et comme maintenant on utilise le microhub.jar
//		 Ca devient inutile.
		try  {
			Client client = new Client("localhost",rmiPort);

			/** Ce bout de code permet d'utiliser une implémentation
			 *  de PlasticListener non Serializable (ce qui est le cas de notre classe)
			 *  (voir doc de RMI-Lite pour plus d'info sur le fonctionnement interne)
			 */
			client.exportInterface(org.votech.plastic.PlasticListener.class);

			// on essaye de récupérer l'objet PlasticHubListener à traver Java RMI
			trace("Get the PlasticHubListener object through RMI");
			plasticHubListener = (PlasticHubListener)client.lookup(PlasticHubListener.class);

		}
		catch(java.rmi.ConnectException ce) {
			Aladin.trace(3, "Unable to connect to the hub, deleting the .plastic file");
			// on efface le fichier ".plastic" qui, apparemment, pointe vers un hub ne tournant plus
			try {
				if( confFile.delete() ) {
					updateState();
					return getHubListener(silent, launchHubIfNeeded);
				}
				else {
					if( !silent ) Aladin.warning(CANT_CONNECT);
					return false;
				}
			}
			catch(Exception e) {return false;}
		}
		catch(Exception e) {
			e.printStackTrace();
			trace("Unable to create the PlasticHubListener object");

			if( !silent ) Aladin.warning(CANT_CONNECT);
			return false;
		}

		return true;
	}

	// variable mémorisant l'association entre nom de l'appli --> URI (ID) de l'appli
	private Hashtable appNamesToURI;
	// mémoire des noms des applis
	private Vector appNames;

	public Object getAppWithName(String s) {
		return appNamesToURI.get(s);
	}

    private String getNameForApp(URI uri) {
        // appel obligatoire pour remplir appNamesToURI
        getAppsSupporting(MSG_GETNAME);

        Enumeration e = appNamesToURI.keys();
        Object o;
        while( e.hasMoreElements() ) {

            o = e.nextElement();
            if( uri.equals(appNamesToURI.get(o)) ) return ((String)o);
        }

        return "Unknown";
    }

    public void sendMessageLoadImage(String url, String name, List recipients) {
        List argList = new ArrayList();
        argList.add(url); // URL
        argList.add(url); // id
        argList.add(name);// name

        sendAsyncMessage(MSG_LOAD_FITS, argList, recipients);
    }
    
    public void sendMessageLoadVOTable(String url, String id, String spectrumName, Map metadata, List recipients) { }

    public void sendMessageLoadSpectrum(String url, String spectrumId, String spectrumName, Map metadata, List recipients) {
        List argList = new ArrayList();
        argList.add(url);
        argList.add(spectrumId);
        argList.add(metadata);

        sendAsyncMessage(MSG_LOAD_SPECTRUM_FROM_URL, argList, recipients==null?null:recipients);
    }

    public void sendMessageLoadCharac(String url, String name, List recipients) {
        List argList = new ArrayList();
        argList.add(url);
        argList.add(name);

        sendAsyncMessage(MSG_LOAD_CHARAC_FROM_URL, argList, recipients);
    }

    private URI getMessage(AppMessagingInterface.AbstractMessage abstractMsg) {
        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_FITS) ) return MSG_LOAD_FITS;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_VOT_FROM_URL) ) return MSG_LOAD_VOT_FROM_URL;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_SPECTRUM_FROM_URL) ) return MSG_LOAD_SPECTRUM_FROM_URL;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_CHARAC_FROM_URL) ) return MSG_LOAD_CHARAC_FROM_URL;

        return null;
    }


    public ArrayList<String> getAppsSupportingTables() {
        return getAppsSupporting(ABSTRACT_MSG_LOAD_VOT_FROM_URL);
    }

    public synchronized ArrayList<String> getAppsSupporting(AppMessagingInterface.AbstractMessage abstractMsg) {
        URI implMsg = getMessage(abstractMsg);
        return getAppsSupporting(implMsg);
    }

	/**
	 *
	 * @param message un message PLASTIC
	 * @return la liste des noms des applications supportant le message passé en param
	 */
	private synchronized ArrayList<String> getAppsSupporting(URI message) {

		if( message==null ) return null;

		if( !isRegistered() || message==null ) return null;
		Object[] listApps = plasticHubListener.getMessageRegisteredIds(message).toArray();

//        System.out.println(message);
//        for( int i=0; i<listApps.length; i++ ) System.out.println(listApps[i]);

        // on ne SE prend pas en compte, d'où le -1
		ArrayList<String> apps = new ArrayList<String>();
		int nbApps;
        if( isSupporting(message) && listApps.length>0 ) nbApps = listApps.length-1;
        else nbApps = listApps.length;

        if( nbApps==0 ) return new ArrayList<String>();

		if( appNamesToURI==null ) appNamesToURI = new Hashtable<String, URI>();
		if( appNames==null ) appNames = new Vector<String>();

		int j = 0;
		String name;
		URI uri;
		for( int i=0; i<listApps.length; i++ ) {
			uri = (URI)listApps[i];

			// on ne retourne pas son propre ID
			if( uri.equals(aladinID) ) continue;

			name = null;
			// on cherche si on a déja mémorisé cette URI, et on récupère le nom correpondant
			Enumeration<String> e = appNamesToURI.keys();
			String tmp;
			while( name==null && e.hasMoreElements() ) {
				tmp = e.nextElement();
				if( appNamesToURI.get(tmp).equals(uri) ) name = tmp;
			}

			// si on avait pas encore mémorisé cette URI, on récupère le nom correspondant, on le rend unique et on l'enregistre
			if( name == null ) {
				name = plasticHubListener.getName(uri);

				// on rend le nom unique
				int k=1;
				String nameRoot = new String(name);
				while( appNames.contains(name) ) name = nameRoot+"-"+k++;

				// mémorisation du nom
				appNames.addElement(name);
				// mémorisation de la correspondance nom --> URI
				appNamesToURI.put(name, uri);
			}

			apps.add(name);
		}

		Collections.sort(apps);

		return apps;
	}

	/** Retourne le fichier de configuration PLASTIC ($HOME/.plastic à l'heure actuelle)
	 *
	 * @return Retourne l'objet File représentant le fichier de conf PLASTIC
	 */
	static protected File getLockFile() {
		File homeDir = new File(System.getProperty("user.home"));
		return new File(homeDir,PLASTIC_CONF_FILE);
	}

	private static final Object VOID = "";
	private static final Object TRUE = Boolean.TRUE;
	private static final Object FALSE = Boolean.FALSE;
	/**
	 * process a message sent by the plastic hub
	 * @param sender sender of the message
	 * @param message ID (URI) of the message
	 * @param argsList arguments
	 * @return
	 */
	private Object processMessage(URI sender, URI message, List argsList) {

		trace("Plastic message : sender is : "+sender);
		trace("Plastic message : message is : "+message);
		final Object[] args = argsList.toArray();
   		for( int i=0; i<args.length; i++ )
   			trace("argument "+i+" is: "+args[i]);

   		int nbArgs = args.length;

//   		System.out.println(argArray[0].getClass());

   		// do we support this message ?
   		if( !supportMessage(message) ) {
   			trace("Aladin is not supporting message "+message);
   			return VOID;
   		}
   		// should we process this message according to the plastic preferences
   		if( !mustProcessMessage(sender, message) ) {
   			trace("According to plastic preferences, the message "+message+" coming from app "+sender+" should not be processed");
   			return VOID;
   		}

   		// traitement proprement dit des différents messages
   		if( message.equals(MSG_ECHO) ) {
   			return args[0];
   		}
   		else if( message.equals(MSG_GETNAME) ) {
   			return ALADIN_NAME;
   		}
   		else if( message.equals(MSG_GETIVORN) ) {
   			// TODO: voir avec John ce qu'on est censé retourner
   			return ALADIN_IVORN;
   		}
   		else if( message.equals(MSG_GET_ICON_URL) ) {
   			return ICON_URL;
   		}
   		else if( message.equals(MSG_GET_DESC) ) {
   			return ALADIN_DESC;
   		}
   		else if( message.equals(MSG_GETVERSION) ) {
   			return SUPPORTED_VERSION;
   		}
   		else if( message.equals(MSG_APP_REGISTERED) ) {
   			updateState();
   			return VOID;
   		}
   		else if( message.equals(MSG_APP_UNREGISTERED) ) {
   			updateState();
   			return VOID;
   		}
   		else if( message.equals(MSG_HUBSTOPPING) ) {
   			// on n'affiche plus le popup avertissant de l'arret du hub (plus dérangeant qu'utile)
//   			Aladin.warning(HUB_STOP, 1);
   			unregister(true);
   			isRegistered = false; // on force la valeur à false
   			return VOID;
   		}
   		else if( message.equals(MSG_EXCEPTION) ) {
   			// TODO : le popup est il nécessaire ?
   			Aladin.warning(EXCEPTION+":\nID: "+
   							((nbArgs>0)?args[0].toString():"")+"\nMessage: "+
							((nbArgs>1)?args[1].toString():""), 1);
   			return VOID;
   		}
   		else if( message.equals(MSG_LOAD_VOT) ) {
   			if( nbArgs<1 ) return FALSE;
   			// on supporte le message, meme si le 2e argument (id) est manquant
   			String votable = args[0].toString();
   			// si le 2e argument est manquant, je n'ai aucun moyen de fabriquer un meilleur id (pas d'URL notamment)
   			String id = (nbArgs>=2)?args[1].toString():"PLASTIC table";

   			int idx = loadVOT(votable, id, sender);

   			Aladin.trace(3, "Receiving table "+id);
   		    a.log("PLASTIC", "receiving table");
   			// TODO : il faudrait blinder ça, ça peut ne pas marcher et sortir TRUE quand meme
   			return idx>=0?TRUE:FALSE;
   		}
   		else if( message.equals(MSG_LOAD_VOT_FROM_URL) ) {
   			if( nbArgs<1 ) return FALSE;
   			String url = args[0].toString();
   			// si l'id est manquant, on prend l'URL
   			String id = (nbArgs>=2)?args[1].toString():url;

   			int idx = loadVOTFromURL(url, id, sender);

   			Aladin.trace(3, "Receiving table "+url);
   			a.log("PLASTIC", "receiving table");
   			return idx>=0?TRUE:FALSE;
   		}
   		else if( message.equals(MSG_LOAD_FITS) ) {
   			if( nbArgs<1 ) return FALSE;
   			String url = args[0].toString();
   			// si l'id est manquant, on prend l'URL
   			String id = (nbArgs>=2)?args[1].toString():url;

   			int idx = loadFitsImageFromURL(url, id, sender);

   			Aladin.trace(3, "Receiving image "+url);
   			a.log("PLASTIC", "receiving image");
   			return idx>=0?TRUE:FALSE;
   		}
   		else if( message.equals(MSG_SELECT_OBJECTS) ) {
   			String[] oidStr = new String[args.length];
            // TODO : à supprimer, concernait une version de PLASTIC préhistorique
   			for( int i=0; i<oidStr.length; i++) oidStr[i] = args[i].toString();
   			selectObjects(oidStr, args);
   			return VOID;
   		}
   		else if( message.equals(MSG_HIGHLIGHT_OBJECT) ) {
   			highlightObject(args[0].toString(), (Integer)args[1]);
   			// TODO : à prendre en compte
   			return TRUE;
   		}
   		// TODO : à bétonner si non visible, cas multi vue, etc, cf. http://eurovotech.org/twiki/bin/view/VOTech/PlasticMessagesProposal
   		else if( message.equals(MSG_POINT_AT_COORDS) ) {
   			if( nbArgs<2 ) return FALSE;
   			double ra, dec;
//   			char sign = argArray[1].toString().charAt(0);
   			try {
   				ra = ((Double)args[0]).doubleValue();
   				dec = ((Double)args[1]).doubleValue();
   			}
   			catch(Exception e) {
   				e.printStackTrace();
   				return FALSE;
   			}
//			String coordDec = argArray[0]+" "+((sign!='-' && sign!='+')?"+":"")+argArray[1];

   			a.execCommand(Coord.getSexa(ra, dec));
   			// TODO : la valeur de retour peut etre différente de TRUE
   			return TRUE;
   		}
   		else if( message.equals(MSG_SEND_ALADIN_SCRIPT_CMD) ) {
   			if( nbArgs<1 ) return FALSE;

   			String scriptCmd = args[0].toString();
   			a.execCommand(scriptCmd);

   			return TRUE;
   		}
        else if( message.equals(MSG_LOAD_VORESOURCE ) ) {
            if( nbArgs<1 ) return FALSE;

            List l = new ArrayList();
            l.add(args[0]);
            return loadVOResources(new Object[] {l}, sender)?TRUE:FALSE;
        }
        else if( message.equals(MSG_LOAD_VORESOURCE_LIST) ) {
//            System.out.println(nbArgs);
            if( nbArgs<1 ) return FALSE;


            return loadVOResources(args, sender)?TRUE:FALSE;
        }


   		/*else if( message.equals(MSG_DRAW) ) {
//   			Coord coo;
//   			try {
//   				coo = new Coord(argArray[0].toString());
//   			}
//   			catch(Exception e) {return FALSE;}
//   				a.execCommand("draw tag("+coo.getSexa(":")+")");
   				return TRUE;
   		}
   		*/
   		/*
   		else if( message.equals(MSG_TEST) ) {
   			counter++;

   			return TRUE;
   		}
   		*/

   		Aladin.trace(3, "The message "+message+" has not been processed (unknown message) !");
   		System.out.println("***"+message+"***"+" not processed");


   		return VOID;
	}

	/** Select in aladin objects designated by their oid
	 *
	 * @param oid
	 */
	private synchronized void selectObjects(String[] oid, Object[] argArray) {
		selectSources(argArray);
	}

	/** Traitement du message MSG_SELECT_OBJECTS
	 *
	 * @param args
	 */
	private void selectSources(Object[] args) {
		if( args==null || args[0]==null ) {
			trace("selectSources : argument is null, can't do anything !");
			return;
		}

		String planeID = args[0].toString();


//		 on cherche le plan concerné
		Plan p = findPlaneByPlasticID(planeID);
		if( p==null ) {
			trace("Could not find plane with plastic ID "+planeID);
			return;
		}


		if( args[1]==null ) {
			trace("selectSources : second parameter is null, can't do anything !");
			return;
		}
//		System.out.println(args[1].getClass());
		if( ! (args[1] instanceof List) ) {
			trace("selectSources : second parameter is not of type List, can't do anything !");
			return;
		}
		Object[] array = ((List)args[1]).toArray();
		int[] idxArray = new int[array.length];
//		System.out.println(array[0].getClass());
		for( int i=0; i<array.length; i++ ) {

			if( array[i] instanceof Integer ) {
				idxArray[i] = ((Integer)array[i]).intValue();
			}
			// provisoirement, en attendant que tout le monde soit passé à Integer !
			// TODO : à supprimer quand plus personne n'utilisera de Long
			else idxArray[i] = ((Long)array[i]).intValue();
		}
//		System.out.println("idxArray.length : "+idxArray.length);
		a.view.selectSourcesByRowNumber((PlanCatalog)p, idxArray);

	}

    Integer oidx;
    String oid;
	/** traitement du message MSG_HIGHLIGHT_OBJECT
	 *
	 * @param id id de la table dans laquelle se trouve l'objet à highlighter
	 * @param idx numéro d'ordre de la source à highlighter
	 *
	 */
	private synchronized void highlightObject(String id, Integer idx) {


        oid = id;
        oidx = idx;
//		 on cherche le plan concerné
		Plan p = findPlaneByPlasticID(id);

		if( p==null ) {
			trace("Could not find plane with plastic ID "+id);
			return;
		}

		a.view.showSource((Source)p.pcat.getObj(idx.intValue()));
	}

	/**
	 * Loads a VOTable into Aladin from an URL
	 * @param votable URL of the VOTable
	 * @param id id de la VOTable
	 * @param sender URI de l'appli ayant envoyé le message
	 */
	private int loadVOTFromURL(String votURL, String id, URI sender) {
		URL url;
		try {
			url = new URL(votURL);
		}
		catch(MalformedURLException e) {
			trace("The provided URL string is malformed, can not load VOTable file !");
			return -1;
		}

		InputStream is;
		try {
			is = url.openStream();
		}
		catch(IOException e) {
			trace("IOException occured when getting stream from VOTable URL, loading is aborted");
			return -1;
		}
		int idx = a.calque.newPlan(is, "PLASTIC", sender.toString());
		if( idx>=0 ) {
			a.calque.plan[idx].addPlasticID(id);
		}
		return idx;
	}

	public String getProtocolName() {
	    return "PLASTIC";
	}

	/**
	 * Loads a FITS file into Aladin from an URL
	 * @param fitsURL URL of the FITS file
	 * @param id to identify this image
	 * @param sender
	 */
	private int loadFitsImageFromURL(String fitsURL, String id, URI sender) {
		URL url;
		try {
			url = new URL(fitsURL);
		}
		catch(MalformedURLException e) {
			trace("The provided URL string is malformed, can not load FITS file !");
			return -1;
		}

		int idx = a.calque.newPlan(fitsURL, "PLASTIC", sender.toString());
		if( idx>=0 ) a.calque.plan[idx].addPlasticID(id);
		return idx;
	}

	/**
	 * Loads a VOTable into Aladin
	 * @param votable the votable string
	 */
	private int loadVOT(String votable, String id, URI sender) {
		int idx =  a.calque.newPlan(new ByteArrayInputStream(votable.getBytes()), "PLASTIC", sender.toString());
		if( idx>=0 ) a.calque.plan[idx].addPlasticID(id);
		return idx;
	}

	/**
	 * ping the hub
	 */
	public boolean ping() {
	    if( plasticHubListener==null ) {
	        return false;
	    }

	    try {
	        plasticHubListener.getHubId();
	    }
	    catch(Exception e) {
	        unregister(true);
            isRegistered = false;

            return false;
	    }

	    return true;
	}

    // TODO : à remplacer par isSupporting
	/**
	 * returns whether the message is supported
	 * @param message
	 * @return
	 */
	private boolean supportMessage(URI message) {
		// pour le moment !
		return true;
	}

	/**
	 * looks in the plastic preferences to see if we support this message
	 * @param sender
	 * @param message
	 * @return
	 */
	private boolean mustProcessMessage(URI sender, URI message) {
		// pour le moment !
		return true;
	}


	/** "Diffusion" du PlanCatalog à toutes les applications Plastic
	 *
	 * @param pc le PlanCatalog à broadcaster
	 * @param recipients tableau des destinataires. Si null, on envoie à tout le monde
	 *
	 */
	public boolean broadcastTable(final Plan pc, final String[] recipients) {
		Aladin.trace(3, "Broadcasting table "+pc.getLabel()+" to "+((recipients==null)?"everyone":(recipients.length+" applications")));

        if( widget!=null ) widget.animateWidgetSend();

		// Pour le moment, on va se contenter d'écrire en VOTable dans un fichier temp,
		// et de passer l'URL du fichier local en question
		// TODO : si originellement, il s'agit d'un VOTable, on pourrait passer directement l'URL
		final File tmpFile;
		if( ( tmpFile = a.createTempFile("plastic"+SAMPUtil.sanitizeFilename(pc.getLabel()), ".xml") ) == null ) {
			trace("Couldn't create temporary file, can't broadcast table !");
			return false;
		}

		// La suite est réalisée dans un thread à part, afin de ne pas bloquer toute l'appli
		new Thread("AladinPlasticSendTable") {
			public void run() {
				if( a.save==null ) a.save = new Save(a);
				a.save.saveCatVOTable(tmpFile, pc, false);
				URL url = SAMPUtil.getURLForFile(tmpFile);
				String urlStr = url.toString();
				String id = pc.getPlasticID();
				if( id==null ) {
					id = urlStr;
					pc.addPlasticID(id);
				}

				// envoi message Plastic
				ArrayList paramList = new ArrayList();
				// ajout du paramètre URL
				paramList.add(urlStr);
				// ajout du paramètre ID
				paramList.add(id);

                trace("Broadcast table with URL: "+urlStr);

				ArrayList recipientsList = null;
				// Si envoi ciblé
				if( recipients!=null ){
					recipientsList = new ArrayList();
					URI uriApp;
					for( int i=0; i<recipients.length; i++ ) {
						// TODO : on pourrait vérifier que l'appli supporte bien le message
						uriApp = (URI)appNamesToURI.get(recipients[i]);
						if( uriApp!=null ) {
							recipientsList.add(uriApp);
							trace("Adding "+uriApp+" to list of recipients");
						}
						else trace("Couldn't find URI of application "+recipients[i]);
					}
				}

				sendAsyncMessage(PlasticManager.MSG_LOAD_VOT_FROM_URL, paramList, recipientsList);
				a.log("PLASTIC", "broadcast table");
			}
		}.start();

		return true;
	}


	/** "Diffusion" du PlanCatalog à toutes les applications Plastic
	 *
	 * @param pi le PlanImage à broadcaster
	 * @param recipients tableau des destinataires. Si null, on envoie à tout le monde
	 */
	public boolean broadcastImage(final Plan pi, final String[] recipients) {
		Aladin.trace(3, "Broadcasting image "+pi.getLabel()+" to "+((recipients==null)?"everyone":(recipients.length+" applications")));

		if (pi==null || ! (pi instanceof PlanImage) ) {
		    return false;
		}

        if( widget!=null ) widget.animateWidgetSend();

        // Pour le moment, on va se contenter d'écrire en FITS dans un fichier temporaire,
		// et de passer l'URL du fichier local en question
		// TODO : si originellement, il s'agit d'un FITS (pas d'un JPEG ou GIF ou PNG), on pourrait passer directement l'URL
		final File tmpFile;
		if( ( tmpFile = a.createTempFile("plastic"+SAMPUtil.sanitizeFilename(pi.getLabel()), ".fits") ) == null ) {
			trace("Couldn't create temporary file, can't broadcast image !");
			return false;
		}

		// La suite est réalisée dans un thread à part, afin de ne pas bloquer toute l'appli
		new Thread("AladinPlasticSendImage") {
			public void run() {
				if( a.save==null ) a.save = new Save(a);
				(a.save).saveImageFITS(tmpFile, (PlanImage)pi);
				URL url = SAMPUtil.getURLForFile(tmpFile);
				String urlStr = url.toString();
				String id = pi.getPlasticID();
				if( id==null ) {
					id = urlStr;
					pi.addPlasticID(id);
				}

				// envoi message Plastic
				ArrayList paramList = new ArrayList();
				// ajout du paramètre URL
				paramList.add(urlStr);
				// ajout du paramètre ID
				paramList.add(id);

				ArrayList recipientsList = null;
				// Si envoi ciblé
				if( recipients!=null ){
					recipientsList = new ArrayList();
					URI uriApp;
					for( int i=0; i<recipients.length; i++ ) {
						// TODO : on pourrait vérifier que l'appli supporte bien le message
						uriApp = (URI)appNamesToURI.get(recipients[i]);
						if( uriApp!=null ) {
							recipientsList.add(uriApp);
							trace("Adding "+uriApp+" to list of recipients");
						}
						else trace("Couldn't find URI of application "+recipients[i]);
					}
				}

				sendAsyncMessage(PlasticManager.MSG_LOAD_FITS, paramList, recipientsList);
				a.log("PLASTIC", "broadcast image");
			}
		}.start();

		return true;
	}

    ResourceChooser resourceChooser;
    /**
     * Crée les objets Server correspondant aux VOResource pointés par les uris
     * @return false si probleme durant parsing, true sinon
     */
    private boolean loadVOResources(Object[] args, URI sender) {
        if( resourceChooser==null ) resourceChooser = new ResourceChooser();

        // basic checking on input parameters
        if( args[0]==null ) {
            trace("loadVOResources : first parameter is null, can't do anything !");
            return false;
        }
        if( ! (args[0] instanceof List) ) {
            trace("loadVOResources : first parameter is not of type List, can't do anything !");
        }
        String[] uris = (String[])((List)args[0]).toArray(new String[((List)args[0]).size()]);

        resourceChooser.updateFrame(uris, sender);
//        for (int i = 0; i < uris.length; i++) {
//            System.out.println(uris[i]);
//        }

        return true;
    }

    // inner class to let user choose VOResource to keep
    class ResourceChooser extends JFrame implements ActionListener {
        // TODO : remplacer JList par des checkbox avec meme code couleur que dans All VO
        JList list;
        DefaultListModel model;
        private boolean firstShow = true;
        String[] uris;

        ResourceChooser() {
            super("Choose resources to keep");
            Util.setCloseShortcut(this, false);

            getContentPane().setLayout(new BorderLayout());



        }

        void updateFrame(final String[] uris, URI sender) {
            this.uris = uris;

            getContentPane().removeAll();
            ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JPanel pTop = new JPanel();
            JLabel label = new JLabel("<html><b>"+getNameForApp(sender)+"</b> has sent some registry resources.<br>Select resources you want to keep:<br></html>");
            pTop.add(label);
            getContentPane().add(pTop, BorderLayout.NORTH);

            JPanel pCenter = new JPanel();
            ListSelectionModel selectionModel = new DefaultListSelectionModel();
            list = new JList();
            list.setSelectionModel(selectionModel);
            JScrollPane scrollPane = new JScrollPane(list);
            model = new DefaultListModel() ;
            for( int i=0; i<uris.length; i++ ) model.addElement(uris[i]);
            list.setModel(model);
            pCenter.add(scrollPane);
            list.setPreferredSize(new Dimension(550,200));
            scrollPane.setPreferredSize(new Dimension(550,200));
            getContentPane().add(pCenter, BorderLayout.CENTER);

            // TODO : variables pour OK et Close
            JPanel pBottom = new JPanel(new FlowLayout());
            JButton okBtn = new JButton("OK");
            okBtn.addActionListener(this);
            okBtn.setFont(Aladin.BOLD);
            pBottom.add(okBtn);
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(this);
            pBottom.add(closeBtn);
            getContentPane().add(pBottom, BorderLayout.SOUTH);

            this.pack();
            if( firstShow ) {
                firstShow = false;
                this.setSize(new Dimension(600,300));
                this.setLocation(200,200);
            }
            this.setVisible(true);

            this.toFront();

            new Thread("PlasticManager:resolveIVORN") {
                public void run() {
                    resolveIVORN(uris);
                }
            }.start();
        }

        // TODO : serait plus propre en mettant les ivorn dans le model
        VOResource[] resources;
        private void resolveIVORN(String[] uris) {
            list.getSelectionModel().clearSelection();
            VOResource ivorn;
            resources = new VOResource[uris.length];
            for( int i=0; i<uris.length; i++ ) {
                ivorn = FrameServer.getIvorn(uris[i]);
                resources[i] = ivorn;
                if( ivorn!=null ) {
                    model.setElementAt(uris[i]+" - "+((ivorn==null)?"":ivorn.desc), i);
                    if( ivorn==null ) continue;
                    if( ivorn.type!=null && (ivorn.type.equals("siap") || ivorn.type.equals("ssap") || ivorn.type.equals("cs")) ) {
                        list.getSelectionModel().addSelectionInterval(i, i);
                    }
                }

            }
        }

        // TODO : rendre tout cela thread-safe

        public void actionPerformed(ActionEvent e) {
           if( true || ! Aladin.PROTO ) return;

           String s = e.getActionCommand();

            if( s.equals("Close") ) this.setVisible(false);
            else if( s.equals("OK") ) {
                // ajout des resource sélectionnées
                int[] indices = list.getSelectedIndices();
                ArrayList selectedResources = new ArrayList();
                for( int i=0; i<indices.length; i++ ) {
                    selectedResources.add(resources[indices[i]]);
                }

                Iterator it = selectedResources.iterator();

                MyByteArrayStream bas = new MyByteArrayStream();
                VOResource curIvorn;
                while( it.hasNext() ) {
                    curIvorn = (VOResource)it.next();
                    FrameServer.createRecord(bas, curIvorn.type, curIvorn.actionName, curIvorn.institute, curIvorn.baseUrl, curIvorn.desc, curIvorn.identifier, "PLASTIC");
                    bas.write("%Aladin.Menu "+ServerDialog.VO_RESOURCES_BY_PLASTIC+"\n\n");
                }

                a.glu.vGluServer = new Vector(50);
                // ajout des resources sélectionnées
                a.glu.loadGluDic(new DataInputStream(bas.getInputStream()),true,false);

                int n = a.glu.vGluServer.size();
                if( n == 0 ) return;

                Server newServer[] = new Server[a.dialog.server.length + n];
                MyButton newButton[] = new MyButton[a.dialog.server.length + n];
                System.arraycopy(a.dialog.server, 0, newServer, 0, a.dialog.server.length);
                System.arraycopy(a.dialog.buttons, 0, newButton, 0, a.dialog.buttons.length);

                for( int i = 0; i < n; i++ ) {
                   newServer[a.dialog.server.length + i] = (Server) a.glu.vGluServer
                         .elementAt(i);
                }



                a.dialog.server = newServer;
                a.dialog.buttons = newButton;
                newServer = null;
                newButton = null;

                Enumeration eServers = a.glu.vGluServer.elements();
                while( eServers.hasMoreElements() ) {
                    Server curServer = ((Server)eServers.nextElement());
//                    System.out.println("type: "+curServer.type);
//                    System.out.println("mypopup: "+curServer.myPopup);
                    a.dialog.voResPopup.addItem(curServer.aladinLabel);
//                    if( curServer.type==Server.CATALOG ) a.dialog.plasticPopupCat.addItem(curServer.nom);
//                    else a.dialog.plasticPopupImg.addItem(curServer.nom);
                    curServer.setOpaque(true);
                    a.dialog.mp.add(curServer.aladinLabel, curServer);
                }

                this.setVisible(false);

                a.dialog.show();
                a.dialog.toFront();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        a.dialog.show(a.dialog.voResPopup);
                    }
                });



            }
        }


    } // end of inner class ResourceChooser


	private Plan lastPlaneWithSelectedSrc; // mémoire du dernier plan avec des sources sélectionnées
	/**
	 * Sends to the plastic hub the MSG_SELECT_OBJECTS message, with all currently selected sources
	 */
	// TODO : on aura p-e besoin d'un param force pour passer outre les prefs
	public void sendSelectObjectsMsg() {

		// on prend en compte les preferences PLASTIC
    	if( ! a.plasticPrefs.getBooleanValue(PlasticPreferences.PREF_SELECT) )
    		return;


		if( !isRegistered() ) {
//			Aladin.trace(3, "Aladin is not registered with a PLASTIC hub, will not send a 'select objects' message");
			return;
		}



		// on lance un nouveau thread pour ne pas bloquer Aladin
		// lors de la récupération des objects sélectionnées
		new Thread("AladinPlasticSendSelect") {
			public void run() {

				Source[] sources = a.view.getSelectedSources();

				if( sources==null ) return;

				// on n'envoie aucun message si auune appli ne peut recevoir le message MSG_SELECT_OBJECTS
				ArrayList<String> apps = getAppsSupporting(MSG_SELECT_OBJECTS);
				if( apps==null || apps.size()==0 ) {
					trace("None of the connected applications supports the 'select objects' message");
					return;
				}

				trace("Sending message "+MSG_SELECT_OBJECTS);
				if(Aladin.levelTrace>=3) {
					for( int i=0; i<sources.length && i<3; i++ ) {
						Aladin.trace(3,"select object : "+sources[i]);
					}
				}


				// TODO : multi tables, multi resources ! --> envoi de plusieurs messages
				// recherche du plan contenant les sources
				Plan p = findPlaneForSources(sources);

				// TODO : conserver l'ID du dernier plan pour lequel o a sélectionné une source
				// si p==null && sources.length==0, on envoie un msg tout de meme
			    if( p==null ) {
			    	if( sources.length==0 && lastPlaneWithSelectedSrc!=null ) {
			    		trace("0 object selected, will send a 'deselection' message");
			    		p = lastPlaneWithSelectedSrc;
			    		lastPlaneWithSelectedSrc = null;
			    	}
			    	else {
			    		lastPlaneWithSelectedSrc = p;
			    		return;
			    	}
			    }
			    else lastPlaneWithSelectedSrc = p;

                // if the plastic ID is null, it means this plane is not shared with any other application
                // if it was, it would have been set, either in broadcastTable or in loadVOTFromURL
                if( p.getPlasticID()==null ) {
                    return;
                }
			    // On recherche numéros d'ordre des Source dans p.pcat
			    Vector v = getSequenceNumber(p, sources);

			    // Preparation du resultat sous forme d'un tableau
			    Integer[] idx = new Integer[v.size()];
			    Enumeration e = v.elements();
			    int k = 0;
			    while( e.hasMoreElements() ) {
			    	idx[k] = ((Integer)e.nextElement());
			    	k++;
			    }

		//		System.out.println("plastic ID : "+p.getPlasticID());
			    List paramList = new ArrayList();
			    paramList.add(p.getPlasticID());
			    paramList.add(Arrays.asList(idx));
		//	  TODO : prendre en compte les préférences plastic, à qui envoie t on, etc
			    sendAsyncMessage(MSG_SELECT_OBJECTS, paramList, null);
			}
		}.start();

	}

	// TODO : prendre en compte si les sources sont dans plusieurs plans
	/** on cherche à quel plan catalogue appartiennent les OID (si plusieurs plans, on laisse tomber pour le moment !)
	 *
	 * @param sources
	 * @return le plan catalogue auquel les sources appartiennent
	 */
	private Plan findPlaneForSources(Source[] sources) {

		return (sources==null || sources.length==0)?null:sources[0].plan;

//		Plan p = null;
//
//	    // Pour chaque OID à trouver, parcours de tous les plans CATALOG
//	    for( int k=0; k<a.calque.plan.length; k++ ) {
//	       if( sources.length==0 ) break;
//
//	       Plan pTmp = a.calque.plan[k];
//	       if( pTmp.type!=Plan.CATALOG || pTmp.pcat==null ) continue;
//	       for( int j=0; j<pTmp.pcat.nb_o; j++ ) {
//	          Source sTmp = ((Source)pTmp.pcat.o[j]);
//	          if( sTmp!=null && sTmp.equals(sources[0]) ) {
//	             p = pTmp;
//	             break;
//	          }
//	       }
//	    }
//
//	    return p;
	}

	public void pointAtCoords(double ra, double dec) {}

	/** Recherche d'un plan catalogue par son PlasticID
	 *
	 * @param plasticID l'id pour lequel on recherche le plan correspondant
	 * @return le plan correspondant, <i>null</i> si on ne trouve rien
	 */
	private Plan findPlaneByPlasticID(String plasticID) {
		Plan p = null;
		// on cherche le plan concerné
		Plan[] plans = a.calque.plan;
		for( int i=0; i<plans.length; i++ ) {
			if( plans[i]==null || !plans[i].isSimpleCatalog() ) continue;
			if( plans[i].hasPlasticID(plasticID) ) p = plans[i];
		}

		return p;
	}


	/** Recherche des numéros d'ordre d'un tableau de sources
	 *
	 * @param p le plan auxquelles les sources appartiennent
	 * @param sources les sources pour lesquels on recherche le numéro d'ordre
	 *
	 * @return le vecteur des numéros d'ordre (vecteur d'objets Integer)
	 */
	private Vector getSequenceNumber(Plan p, Source[] sources) {
		Vector v = new Vector();
		// On recherche numéros d'ordre des Source dans p.pcat
		Iterator<Obj> it = p.iterator();
		for( int i=0; it.hasNext(); i++ ) {
		   Obj o = it.next();
		   if( o==null ) continue;
			for( int j=0; j<sources.length; j++ ) {
				Source sTmp = (Source)o;
				if( sTmp.equals(sources[j])) {
					v.addElement(new Integer(i));
					break;
				}
			}
		}

		return v;
	}

	public boolean internalHubRunning() {
		return internalHub!=null;
	}

	/**
	 * met à jour l'état courant (connecté ou non, etc)
	 *
	 */
	public void updateState() {
        new Thread("AladinPlasticUpdate") {
            public void run() {
            	try {
            		if( isRegistered() ) {
            			if( getAppsConnected().length>0 ) curState = PLASTIC_CAN_TRANSMIT;
            			else curState = PLASTIC_CONNECTED_ALONE;
            		}
            		else {
            			if( getLockFile().exists() )
            				curState = PLASTIC_NOT_CONNECTED;
            			else
            				curState = NO_PLASTIC;
            		}
            	}
            	catch(Exception e) {
            		if( Aladin.levelTrace>=3 ) e.printStackTrace();
            	}

            	if( widget!=null ) widget.updateStatus(curState);
            }
        }.start();
	}


	/**
	 * Retourne les identifiants des applications connectées au hub
	 * Ne retourne ni son propre identifiant ni l'identifiant du hub
	 * @return
	 */
	private String[] getAppsConnected() {
		Vector v = new Vector();
		try {
			Iterator it = plasticHubListener.getRegisteredIds().iterator();

			String curId;
			String hubId = plasticHubListener.getHubId().toString();
			while( it.hasNext() ) {
				curId = it.next().toString();
				if( curId.equals(aladinID.toString()) || curId.equals(hubId) ) continue;
				v.addElement(curId);
			}
		}
		catch(Exception e) {return new String[0];}

		String[] ids = new String[v.size()];
		v.copyInto(ids);
		v = null;

		return ids;
	}

	/** Envoie le message MSG_HIGHLIGHT_OBJECT au hub (si connecté)
	 *
	 * @param source la source à montrer
	 * @see View#showSource(Source)
	 */
	// TODO : on aura p-e besoin d'un param force pour outre passer les prefs
	public void sendHighlightObjectsMsg(Source source) {
		// on prend en compte les preferences PLASTIC
    	if( ! a.plasticPrefs.getBooleanValue(PlasticPreferences.PREF_HIGHLIGHT) )
    		return;

//		Aladin.trace(3, "Sending message "+MSG_HIGHLIGHT_OBJECT+" for source "+source);


		// TODO : que faire si source==null ? envoi de null pour ne plus hilighter le dernier objet hilighté ?
		if( source==null ) {
		    return;
		}

		if( !isRegistered() ) {
//			Aladin.trace(3, "Aladin is not registered with a PLASTIC hub, will not send a 'highlight object' message");
			return;
		}

		Source[] sources = new Source[] {source};

		Plan p = findPlaneForSources(sources);

	    if( p==null ) {
	    	trace("Could not find plane, can't send PLASTIC message");
	    	return;
	    }

	    // On recherche numéros d'ordre des Source dans p.pcat
	    Vector v = getSequenceNumber(p, sources);

	    if( v.size()==0 ) {
	    	trace("Could not find sequence number for source "+source);
	    	return;
	    }



	    Integer idx = ((Integer)v.elementAt(0));

        // évite des appels en aller retour !
        if( oidx!=null && oid!=null && oid.equals(p.getPlasticID()) && oidx.equals(idx) ) {
            return;
        }

        oidx = idx;
        oid = p.getPlasticID();


	    List paramList = new ArrayList();
	    paramList.add(p.getPlasticID());
	    paramList.add(idx);
//	  TODO : prendre en compte les préférences plastic, à qui envoie t on, etc
	    sendAsyncMessage(MSG_HIGHLIGHT_OBJECT, paramList, null);

	}


	/*
	protected Object sendTestMessage() {
		try {
			Object o = plasticHubListener.request(aladinID, MSG_GETIVORN, new ArrayList());
			System.out.println("test message result : "+o);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	*/


	/** all synchronous messages sent to the hub should be sent through this method
	 *
	 * @param sender sender of the message
	 * @param message message to be sent
	 * @param args arguments of the message
	 * @param recipients recipients for this message. If null, the message will be sent to all apps registered in the PLASTIC hub
	 */
	private Object sendMessage(URI sender, URI message, List args, List recipients) {
		if( !isRegistered ) {
//			Aladin.trace(3, "Aladin is not registered with a PLASTIC hub, can not send message !");
			return null;
		}


//		Aladin.trace(3, "Sending synchronous message "+message+" to the PLASTIC hub");

		if( recipients==null ) {
			return plasticHubListener.request(sender, message, args);
		}
		else {
			return plasticHubListener.requestToSubset(sender, message, args, recipients);
		}

	}

	/**
	 * @see PlasticManager#sendMessage(URI, URI, List, List)
     */
	private Object sendMessage(URI message, List args, List recipients) {
		return sendMessage(aladinID, message, args, recipients);
	}

	/** all asynchronous messages sent to the hub should be sent through this method
	 *
	 * @param sender sender of the message
	 * @param message message to be sent
	 * @param args arguments of the message
	 * @param recipients recipients for this message. If null, the message will be sent to all apps registered in the PLASTIC hub
	 */
	private void sendAsyncMessage(URI sender, URI message, List args, List recipients) {
//		System.out.println("sendAsyncMessage : "+message+"  "+System.currentTimeMillis());
		if( !isRegistered ) {
//			Aladin.trace(3, "Aladin is not registered with a PLASTIC hub, can not send message !");
			return;
		}

//		Aladin.trace(3, "Sending asynchronous message "+message+" to the PLASTIC hub");

		if( recipients==null ) {
			plasticHubListener.requestAsynch(sender, message, args);
		}
		else {
			plasticHubListener.requestToSubsetAsynch(sender, message, args, recipients);
		}
	}

	/**
	 * @see PlasticManager#sendAsyncMessage(URI, URI, List, List)
     */
	private void sendAsyncMessage(URI message, List args, List recipients) {
		sendAsyncMessage(aladinID, message, args, recipients);
	}

	public void setPlasticWidget(PlasticWidget widget) {
		this.widget = widget;
	}

	public PlasticWidget getPlasticWidget() {
		return this.widget;
	}


	/****** implémentation de l'interface PlasticListener ******/
	public Object perform(URI sender, URI message, java.util.List args) {
//		System.out.println("perform : "+message+"  "+System.currentTimeMillis());
		if (!Aladin.PLASTIC_SUPPORT) return null;

        if( widget!=null ) widget.animateWidgetReceive();

		return processMessage(sender, message, args);
	}
	/************* EOF implémentation PlasticListener **********/


	private boolean plasticTrace = false;
	// mode "trace" pour débugging PLASTIC
	public void trace(String s) {
		if( !plasticTrace ) return;

		System.out.println("** PLASTIC : "+s);
	}

	/**
	 * @return Returns the plasticTrace.
	 */
	public boolean getPlasticTrace() {
		return this.plasticTrace;
	}
	/**
	 * @param plasticTrace The plasticTrace to set.
	 */
	public void setPlasticTrace(boolean plasticTrace) {
		this.plasticTrace = plasticTrace;
	}


}
