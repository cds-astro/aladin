// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

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

//import org.apache.xmlrpc.WebServer;
//import org.apache.xmlrpc.XmlRpcClient;
//import org.apache.xmlrpc.XmlRpcException;
//import org.apache.xmlrpc.XmlRpcHandler;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.astrogrid.samp.xmlrpc.SampXmlRpcClient;
import org.astrogrid.samp.xmlrpc.SampXmlRpcHandler;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServer;
import org.astrogrid.samp.xmlrpc.internal.InternalClient;
import org.astrogrid.samp.xmlrpc.internal.InternalServer;

import cds.tools.Util;


// TODO : subscribe à samp.hub.event.xxx (voir TOPCAT), et ne remplir les tableau apptoURI qu'à ce moment là :-)

/** Classe gérant toutes les connexions avec le hub SAMP (Simple Application Messaging Protocol,
 * aussi appelé Stupid Alias Meaning PLASTIC)
 * Cette classe se charge de la connexion avec le hub, et permet d'envoyer des messages au hub
 *
 * Le fait de faire passer tout ce qui est relatif à SAMP par une classe dédiée permet de
 * passer relativement facilement d'une implémentation du protocole à une autre
 *
 * Les classes modifiées ou créées pour supporter SAMP sont les suivantes (package cds.aladin) :
 *
 * - Aladin : l'ajout des fonctionnalités SAMP est subordonné à la variable Aladin.PLASTIC_SUPPORT, qui pour le moment prend la valeur de Aladin.PROTO
 *    --> ajout d'un menu SAMP (variable MPLASTIC) permettant de s'inscrire/désinscrire auprès du hub (dans sous-menu SMPLASTIC)
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
 * - SAMPManager (cette classe) :
 *    --> gère la registration/unregistration auprès du hub SAMP
 *    --> gère globalement l'envoi et la réception des messages SAMP
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
 * @version 0.1  Creation, Sept 2008
 * @version 0.2  Suppression support PLASTIC, et utilisation directe jsamp.jar (PF Mai 2017 - quick&dirty à vérifier par TB dès qu'il peut)
 *
 * @see <a href="http://www.ivoa.net/cgi-bin/twiki/bin/view/IVOA/SampProgress">SAMP page on the IVOA web site</a>
 *
 */
public class SAMPManager implements AppMessagingInterface, SampXmlRpcHandler, PlaneLoadListener {

    // PF Mai 2017 : remplacement interface XmlRpcHandler par SampXmlRpcHandler

    static final protected String NOTIFY = "samp.hub.notify";
    static final protected String NOTIFY_ALL = "samp.hub.notifyAll";

    // some important keys
    static final protected String KEY_SELF_ID = "samp.self-id";
    static final protected String KEY_HUB_ID = "samp.hub-id";
    static final protected String KEY_PRIVATE_KEY = "samp.private-key";
    static final protected String KEY_MTYPE = "samp.mtype";
    static final protected String KEY_PARAMS = "samp.params";



    // client methods
    static final protected String METHOD_RECEIVE_NOTIFICATION = "samp.client.receiveNotification";
    static final protected String METHOD_RECEIVE_CALL = "samp.client.receiveCall";
    static final protected String METHOD_RECEIVE_RESPONSE = "samp.client.receiveResponse";

    ///////////////////// List of SAMP messages /////////////////////////////////

    ////       List of SAMP Hub messages /////
    static final protected String HUB_MSG_REGISTER = "samp.hub.register";
    static final protected String HUB_MSG_DECLARE_METADATA = "samp.hub.declareMetadata";
    static final protected String HUB_MSG_DECLARE_SUBSCRIPTIONS = "samp.hub.declareSubscriptions";
    static final protected String HUB_MSG_UNREGISTER = "samp.hub.unregister";
    static final protected String HUB_MSG_PING = "samp.hub.ping";
    static final protected String HUB_MSG_DISCONNECT = "samp.hub.disconnect";
    static final protected String HUB_MSG_REPLY = "samp.hub.reply";
    static final protected String HUB_MSG_GET_REGISTERED_CLIENTS = "samp.hub.getRegisteredClients";
    static final protected String HUB_MSG_GET_METADATA = "samp.hub.getMetadata";
    static final protected String HUB_MSG_GET_SUBSCRIBED_CLIENTS = "samp.hub.getSubscribedClients";

    static final protected String HUB_MSG_SHUTDOWN = "samp.hub.event.shutdown";

    static final protected String HUB_MSG_REGISTRATION = "samp.hub.event.register";
    static final protected String HUB_MSG_UNREGISTRATION = "samp.hub.event.unregister";
    // TODO : faire qqch avec ce message, y réagir
    static final protected String HUB_MSG_SUBSCRIPTIONS = "samp.hub.event.subscriptions";


    static final protected String MSG_LOAD_FITS_IMAGE = "image.load.fits";
    static final protected String MSG_POINT_AT_COORDS = "coord.pointAt.sky";
    static final protected String MSG_LOAD_VOT_FROM_URL = "table.load.votable";
    static final protected String MSG_LOAD_FITS_TABLE_FROM_URL = "table.load.fits";
    static final protected String MSG_LOAD_FITS_MOC_COVERAGE = "coverage.load.moc.fits";
    static final protected String MSG_HIGHLIGHT_OBJECT = "table.highlight.row";
    static final protected String MSG_SELECT_OBJECTS = "table.select.rowList";

    static final protected String MSG_LOAD_SPECTRUM = "spectrum.load.ssa-generic";

    // message non "officiel", permet l'envoi de cmdes script à Aladin, via SAMP
    static final protected String MSG_SEND_ALADIN_SCRIPT_CMD = "script.aladin.send";
    // message non officiel, pour récupérer les coordonnées courantes
    static final protected String MSG_GET_COORDS = "coord.get.sky";
    // message non officiel pour faire un snapshot de la vue courante
    static final protected String MSG_GET_SNAPSHOT = "snapshot.get.jpg";


    static final protected String MSG_PING = "samp.app.ping";

    // liste des messages supportés (i.e auxquels on répond)
    static final protected String[] SUPPORTED_MESSAGES = {
            MSG_LOAD_FITS_IMAGE, MSG_POINT_AT_COORDS, MSG_GET_COORDS,MSG_GET_SNAPSHOT,
            MSG_LOAD_VOT_FROM_URL,
            MSG_LOAD_FITS_TABLE_FROM_URL, MSG_HIGHLIGHT_OBJECT, MSG_SELECT_OBJECTS,
            MSG_PING, MSG_SEND_ALADIN_SCRIPT_CMD,
            HUB_MSG_SHUTDOWN, HUB_MSG_REGISTRATION, HUB_MSG_UNREGISTRATION,
            HUB_MSG_DISCONNECT,
            HUB_MSG_SUBSCRIPTIONS
            };
    


    // strings associated to reply to a message
    static final protected String MSG_REPLY_SAMP_STATUS = "samp.status";
    static final protected String MSG_REPLY_SAMP_STATUSOK = "samp.ok";
    static final protected String MSG_REPLY_SAMP_STATUSERROR = "samp.error";
    static final protected String MSG_REPLY_SAMP_RESULT = "samp.result";
    static final protected String MSG_REPLY_SAMP_ERROR = "samp.error";

    // map faisant la correspondance entre plans et msgId
    private Hashtable<Plan, String> planesToMsgIds = new Hashtable<>();

    // answer to getName message
    static final protected String ALADIN_NAME = "Aladin";

    // Aladin's IVORN (TODO : à modifier quand Aladin sera enregistré auprès d'un registry !)
    static final protected String ALADIN_IVORN = "ivo://CDS/Aladin";

    // fichier de conf
    static final protected String SAMP_CONF_FILE = ".samp";


    private int curState = NO_PLASTIC;

    // true si on doit mettre à jour la liste des applis
    private boolean updateAppsListNeeded = true;

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

    // variables de travail
    private boolean isRegistered = false;

    // référence au widget reflétant l'état de la connexion
    private PlasticWidget widget;

    // ###SAMP
    private String hubUrl;
    private String sampSecret;
    // handle to contact the hub
    //    private XmlRpcClient hubClient;  //PF Mai 2017 - Remplacement par SampXmlRpcClient
    private SampXmlRpcClient hubClient;
    // Aladin's XML-RPC Server to be reached by other apps
    //    private WebServer aladinXmlRpcServer; //PF Mai 2017 - Remplacement par SampXmlRpcServer
    private SampXmlRpcServer aladinXmlRpcServer;


    // ###SAMP
    static final private String SAMP_SECRET_KEY = "samp.secret";
    static final private String SAMP_HUB_URL_KEY = "samp.hub.xmlrpc.url";


    // ma propre ID
    private String selfId;
    // ID du hub
    private String hubId;
    // ma clé privée à passer dans chaque message
    private String myPrivateKey;


    /** Constructeur */
    public SAMPManager(Aladin a) {
        this.a = a;
        createChaine();
    }

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
     * Register with the SAMP hub
     *
     * @parameter silent if true, in silent mode (no popup menu when register fails !)
     * @param launchHubIfNeeded si silent==true, on essaye automatiquement de lancer un hub s'il n'en existe pas. Sinon, on passe par une boite de dialogue de confirmation
     * @return true if Aladin was registered, false otherwise
     */
    public boolean register(boolean silent, boolean launchHubIfNeeded) {
        trace("Try to register Aladin with the SAMP hub");

        Vector params = new Vector();
        Object result;
        
        // PF - Mar 2021 - J'ai ajouté ce verrou exclusif sur la variable isRegistered pour éviter
        // qu'Aladin puisse s'inscrire deux fois. La conséquence était que le message table.highlight.row
        // était alors envoyé à lui-même lorsque l'on survolait les sources dans la vue, et donc automatiquement
        // sélectionnées
        // Le problème n'arrivait que de temps en temps, probablement lié à un délai sur la réponse
        // du getHubListener(...)
        synchronized( lockRegistered ) {
           
           if( isRegistered() ) {
              trace("Aladin was already registered !");
              return true;
           }

           if( !getHubListener(silent, launchHubIfNeeded) ) {
              trace("Could not register to the SAMP hub");
              return false;
           }

           boolean pbOccured = false;
           // ###SAMP

           params.add(sampSecret);
           try {
              // PF Mai 2017 - Tous les execute(...) ont été remplacés par callAndWait(...)
              //           Map resultMap = (Map)hubClient.execute(HUB_MSG_REGISTER, params);
              Map resultMap = (Map)hubClient.callAndWait(HUB_MSG_REGISTER, params);
              // TODO : se prévenir contre les pbs de cast incorrect et de clé absentes !!
              myPrivateKey = resultMap.get(KEY_PRIVATE_KEY).toString();
              selfId = resultMap.get(KEY_SELF_ID).toString();
              hubId = resultMap.get(KEY_HUB_ID).toString();
              trace("Aladin has registered, self-id="+selfId);
              
           }
           catch(Exception e) {
              pbOccured = true;
           }
           if( pbOccured ) setRegistered(false);
           else {
              setRegistered(true);
              Aladin.trace(3, "Successful registration with the SAMP hub");
           }
        }

        // declare metadata
        params = new Vector();
        Map map = new Hashtable();
        map.put("samp.name", "Aladin");
        map.put("samp.description.text", "The Aladin Sky Atlas");
        map.put("samp.icon.url", ICON_URL);
        map.put("samp.documentation.url", "https://"+Aladin.ALADINMAINSITE+"/java/AladinManual.pdf");
        map.put("author.name", "Pierre Fernique, Thomas Boch, Anais Oberto, Francois Bonnarel, Chaitra");
        map.put("author.email", "cds-question@astro.unistra.fr");
        map.put("author.affiliation", "CDS, Observatoire astronomique de Strasbourg");
        map.put("home.page", "https://"+Aladin.ALADINMAINSITE);
        map.put("aladin.version", a.VERSION);
        params.add(myPrivateKey);
        params.add(map);
        try {
            // PF Mai 2017 - execute(...) -> callAndWait(...)
            result = hubClient.callAndWait(HUB_MSG_DECLARE_METADATA, params);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        // PF Mai 2017 - Utilisation du InternalServer()
        // start XML-RPC server
        //        int xmlRpcPort = findFreePort();
        //        aladinXmlRpcServer = new WebServer(xmlRpcPort);
        //        aladinXmlRpcServer.start();
        //        String callbackAddress = "http://"+getLocalhost()+":"+xmlRpcPort+"/";


        //        trace(" Aladin callback address is: "+callbackAddress);
        //        aladinXmlRpcServer.addHandler("samp.client", this);

        try {
            aladinXmlRpcServer = new InternalServer();
            aladinXmlRpcServer.addHandler(this);
        } catch( IOException e1 ) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String callbackAddress = aladinXmlRpcServer.getEndpoint().toString();

        // set XML-RPC callback
        params = new Vector();
        params.add(myPrivateKey);
        params.add(callbackAddress);
        try {
            trace("setting Aladin XMLRPC callback : "+callbackAddress);
            result = hubClient.callAndWait("samp.hub.setXmlrpcCallback", params);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        // set MTypes
        params = new Vector<>();
        params.add(myPrivateKey);
        Map<String, Map<String, String>> subscriptionMap = new Hashtable<>();
        for( int i=0; i<SUPPORTED_MESSAGES.length; i++ ) {
            Map<String, String> subscriptionAnnotation = new Hashtable<>();
            subscriptionAnnotation.put("x-samp.mostly-harmless", "1");
            subscriptionMap.put(SUPPORTED_MESSAGES[i], subscriptionAnnotation);
        }
        params.add(subscriptionMap);
        try {
            result = hubClient.callAndWait(HUB_MSG_DECLARE_SUBSCRIPTIONS, params);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        if( widget!=null ) widget.updateStatus(isRegistered());
        updateState();

        return true;
    }

    // TODO : faut il la synchronizer ??? SAMP4IDL ne fonctionnait plus bien avec les commandes script
    // PF Mai 2017 - Suppression du throws XmlRpcException
    public synchronized Object execute(String method, Vector params) {
        trace("Receiving XML request :\nmethod="+method+"\nparams="+params);


        // TODO : définir des objets messages qui gèrent tout : message.process(params)

        // TODO : catcher les problemes de cast !
        if( method.equals(METHOD_RECEIVE_NOTIFICATION) || method.equals(METHOD_RECEIVE_CALL) ) {
            Object retValue = TRUE;
            String senderId = (String)params.get(1);

            boolean responseNeeded = method.equals(METHOD_RECEIVE_CALL);
            String msgId = null;
            if( responseNeeded ) {
                msgId = (String)params.get(2);
            }
            int paramIdx = responseNeeded?3:2;
            Map map = (Map)params.get(paramIdx);
            Object mType = map.get(KEY_MTYPE);
            Map msgParams = (Map)map.get(KEY_PARAMS);

            if( mType==null || msgParams==null ) {
                System.err.println("mtype or msgParams is null");
                return null;
            }
            trace("msgParams is "+msgParams);
            trace("mType is "+mType);

            // hub shutdown
            if( mType.equals(HUB_MSG_SHUTDOWN) ) {
                trace("hub is shutting down");
                unregister(true);
                setRegistered(false);
                updateState();
            }

            else if( mType.equals(HUB_MSG_DISCONNECT) ) {
                trace("Hub wants us to disconnect ... proceed");
                unregister(true);
                setRegistered(false);
                a.dontReconnectAutomatically = true;
                updateState();
            }

            // registration of new client
            else if( mType.equals(HUB_MSG_REGISTRATION) ) {
                updateState();
            }

            // unregistration of client
            else if( mType.equals(HUB_MSG_UNREGISTRATION) ) {
                updateState();
            }

            // new declaration of supported MTypes
            else if( mType.equals(HUB_MSG_SUBSCRIPTIONS) ) {
                updateState();
                updateAppsListNeeded = true;
            }

            // ping
            else if( mType.equals(MSG_PING) ) {
                replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
            }

            // load VOTable/FITS table
            else if( mType.equals(MSG_LOAD_VOT_FROM_URL) || mType.equals(MSG_LOAD_FITS_TABLE_FROM_URL) ) {
                String url = (String)msgParams.get("url");
                String tableId = (String)msgParams.get("table-id");
                String name = (String)msgParams.get("name");

                if( url==null ) {
                    System.err.println("Missing URL !");
                    if( responseNeeded ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load data, URL is missing !");
                    }
                    return FALSE;
                }
                String senderName = getNameForApp(senderId);
                Plan p = loadVOTFromURL(url, tableId, name, senderName);
                if( responseNeeded ) {
                    if( p==null ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load VOTable !");
                    }
                    // si le plan est deja pret
                    else if( p.flagOk ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                    }
                    // reply is deferred until plane is loaded
                    else {
                        trace("Associating message ID "+msgId+" to catalogue plane "+p.getLabel());
                        planesToMsgIds.put(p, msgId);
                        p.addPlaneLoadListener(this);
                    }
                }

                Aladin.trace(3, "Receiving table "+url);
                a.log("SAMP", "receiving table");
            }

            // load FITS image
            else if( mType.equals(MSG_LOAD_FITS_IMAGE) ) {
                String url = (String)msgParams.get("url");
                String imageId = (String)msgParams.get("image-id");
                String name = (String)msgParams.get("name");

                if( url==null ) {
                    System.err.println("Missing URL !");
                    if( responseNeeded ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load image, URL is missing !");
                    }
                    return FALSE;
                }

                if( imageId==null ) {
                    imageId = url;
                }

                String senderName = getNameForApp(senderId);
                Plan p = loadFitsImageFromURL(url, imageId, name, senderName);
                if( responseNeeded ) {
                    if( p==null ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load image !");
                    }
                    // si le plan est deja pret
                    else if( p.flagOk ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                    }
                    // reply is deferred until plane is loaded
                    else {
                        trace("Associating message ID "+msgId+" to image plane "+p.getLabel());
                        planesToMsgIds.put(p, msgId);
                        p.addPlaneLoadListener(this);
                    }
                }

                Aladin.trace(3, "Receiving image "+url);
                a.log("SAMP", "receiving image");
            }
            // load FITS MOC coverage
            else if( mType.equals(MSG_LOAD_FITS_MOC_COVERAGE) ) {
                String url = (String)msgParams.get("url");
                String coverageId = (String)msgParams.get("coverage-id");
                String name = (String)msgParams.get("name");

                if( url==null ) {
                    System.err.println("Missing URL !");
                    if( responseNeeded ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load MOC coverage, URL is missing !");
                    }
                    return FALSE;
                }

                if( coverageId==null ) {
                    coverageId = url;
                }

                String senderName = getNameForApp(senderId);
                Plan p = loadFitsImageFromURL(url, coverageId, name, senderName);
                if( responseNeeded ) {
                    if( p==null ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not load MOC coverage !");
                    }
                    // si le plan est deja pret
                    else if( p.flagOk ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                    }
                    // reply is deferred until plane is loaded
                    else {
                        trace("Associating message ID " + msgId + " to MOC coverage plane " + p.getLabel());
                        planesToMsgIds.put(p, msgId);
                        p.addPlaneLoadListener(this);
                    }
                }

                Aladin.trace(3, "Receiving MOC "+url);
                a.log("SAMP", "receiving MOC");
            }

            // Aladin script submission
            else if( mType.equals(MSG_SEND_ALADIN_SCRIPT_CMD) ) {
                String script = (String)msgParams.get("script");
                if( script==null) {
                    String errorMsg = "Missing script parameter !";
                    System.err.println(errorMsg);
                    if( responseNeeded ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, errorMsg);
                    }
                    return FALSE;
                }
                String result = a.execCommand(script);
                if( responseNeeded ) {
                    if( result.startsWith("Error") ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, result);
                    }
                    else {
                        Map<String, String> scriptResult = new Hashtable<>();
                        scriptResult.put("script.result", result);
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, scriptResult, null);
                    }
                }
            }
            else if( mType.equals(MSG_POINT_AT_COORDS) ) {
                try {
                    double ra= Double.parseDouble(((String)msgParams.get("ra")));
                    double dec = Double.parseDouble(((String)msgParams.get("dec")));

                    //PF - sept 2010 - pour éviter le bug dans le cas où le frame n'est pas en ICRS
                    a.view.gotoThere( new Coord(ra,dec) );
                }
                catch(Exception e) {
                    String errorMsg = "Error while processing SAMP message "+MSG_POINT_AT_COORDS+":"
                            +"Missing 'ra' or 'dec' parameter or incorrect type for params";
                    a.command.println(errorMsg);
                    retValue = FALSE;
                    e.printStackTrace();
                    if( responseNeeded ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, errorMsg);
                    }
                }
                if( responseNeeded ) {
                    replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                }
            }
            else if( mType.equals(MSG_HIGHLIGHT_OBJECT) ) {
                boolean success;
                try {
                    success = highlightObject((String)msgParams.get("table-id"), new Integer((String)msgParams.get("row")));
                }
                catch(Exception e) {
                    e.printStackTrace();
                    success = false;
                }
                if( responseNeeded ) {
                    if( success ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                    }
                    else {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not find object to highlight");
                    }
                }
            }
            else if( mType.equals(MSG_SELECT_OBJECTS) ) {
                String tableId = (String)msgParams.get("table-id");
                trace("id recu : "+tableId);
                List rowList = (List)msgParams.get("row-list");
                int[] rowIdxArray = new int[rowList.size()];
                Iterator it = rowList.iterator();
                int k = 0;
                try {
                    while( it.hasNext() ) {
                        rowIdxArray[k] = Integer.parseInt((String)it.next());
                        k++;
                    }
                }
                catch(NumberFormatException nfe) {
                    nfe.printStackTrace();
                    replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Incorrect format for indices !");
                    return FALSE;
                }
                boolean success = selectSources(tableId, rowIdxArray);
                if( responseNeeded ) {
                    if( success ) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
                    }
                    else {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Could not find sources to select");
                    }
                }
            }
            else if( mType.equals(MSG_GET_COORDS) ) {
                if (a.view.repere==null || Double.isNaN(a.view.repere.raj) || Double.isNaN(a.view.repere.dej)) {
                    retValue = FALSE;
                    replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "no repere has been set");
                }
                else {
                    Map<String, String> positionMap = new Hashtable<>();
                    positionMap.put("ra", Double.toString(a.view.repere.raj));
                    positionMap.put("dec", Double.toString(a.view.repere.dej));
                    replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, positionMap, null);
                }
            }
            else if( mType.equals(MSG_GET_SNAPSHOT) ) {
                File tmp;
                BufferedInputStream bis = null;
                StringBuffer base64Str = new StringBuffer("data:image/jpeg;base64,");
                try {
                    tmp = File.createTempFile("samp", ".jpg");
                    tmp.deleteOnExit();

                    int w, h;
                    w = h = 300;
                    a.save.saveView(tmp.getAbsolutePath(), w, h, Save.JPEG, 0.7f);
                    if (tmp.length()==0) {
                        replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, "Unable to generate snapshot of current view");
                        return FALSE;
                    }
                    // TODO : passer plutot une URL qu'une chaine en base64
                    bis = new BufferedInputStream(new FileInputStream(tmp));
                    byte[] buffer = new byte[(int)tmp.length()];
                    int offset = 0;
                    for (int read = bis.read(buffer, offset, buffer.length - offset); read>=0 && offset<buffer.length; read = bis.read(buffer, offset, buffer.length - offset)) {
                        offset += read;
                    }
                    base64Str.append(Util.toB64(buffer));
                }
                catch(Exception e) {
                    replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, e.getMessage());
                    return FALSE;
                }
                finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                Map<String, String> resultMap = new Hashtable<>();
                resultMap.put("data", base64Str.toString().replaceAll("(\r\n|\n)", ""));

                replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, resultMap, null);
            }
            else {
                System.err.println("Unprocessed message "+mType);

            }

            return retValue;
        }
        // réponse à un appel antérieur
        else if( method.equals(METHOD_RECEIVE_RESPONSE) ) {
            // TODO !!
        }

        // unknown method
        System.err.println("Unknown method "+method);
        return null;
    }

    static int startPort = 42195;
    static int findFreePort() {
        final int nbTry = 20;
        for( int iPort = startPort; iPort < startPort + nbTry; iPort++ ) {
            try {
                Socket trySocket = new Socket(getLocalhost(), iPort);
                if( ! trySocket.isClosed() ) {
                    trySocket.close();
                }
            }
            catch(ConnectException ce) {
                /* Can't connect - this hopefully means that the socket is
                 * unused. */
                startPort = iPort+1;
                return iPort;
            }
            catch(Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        return -1;
    }


    /** do we support a given message ? */
    protected boolean isSupporting(String message) {
        for( int i=0; i<SUPPORTED_MESSAGES.length; i++ ) {
            if( SUPPORTED_MESSAGES[i].equals(message) ) return true;
        }

        return false;
    }

    public boolean unregister() {
        return unregister(false);
    }

    public boolean unregister(boolean force) {
        return unregister(force, false);
    }

    /**
     * Unregister from the SAMP hub
     *
     * @param force on force la désinscription
     *
     * @return true if Aladin is unregistered, false otherwise
     */
    public boolean unregister(boolean force, boolean destroyInternalHub) {
        trace("Try to unregister Aladin from the SAMP hub");

        // TODO : test plus pointu !!
        if( !isRegistered() ) {
            trace("Aladin is not registered with the hub, no need to unregister !");
            return true;
        }

        if( !getHubListener(false) ) {
            trace("Could not unregister from the SAMP hub");

            if( force ) {
                // TODO : à factoriser
                setRegistered(false);
                selfId = null;

                // arrêt du hub interne
                if( destroyInternalHub ) stopInternalHub(force);

                // arrêt serveur XMLRPC
                if( aladinXmlRpcServer!=null ) {
                    // PF Mai 2017
                    ((InternalServer)aladinXmlRpcServer).getHttpServer().stop();
                    //                    aladinXmlRpcServer.shutdown();
                }

                if( widget!=null ) widget.animateWidgetReceive(true, false);
                updateState();
            }

            return false;
        }
        Vector params = new Vector();
        params.add(myPrivateKey);
        try {
            hubClient.callAndWait(HUB_MSG_UNREGISTER, params);

        }
        catch( Exception e) {
            trace("Error while trying to unregister : "+e.getMessage());
            if( !force ) return false;
        }

        setRegistered(false);
        selfId = null;

        // arrêt du hub interne
        if( destroyInternalHub ) stopInternalHub(force);

        // arrêt serveur XMLRPC
        if( aladinXmlRpcServer!=null ) {
            ((InternalServer)aladinXmlRpcServer).getHttpServer().stop();
            //            aladinXmlRpcServer.shutdown();
        }

        if( widget!=null ) widget.animateWidgetReceive(true, false);
        updateState();

        Aladin.trace(3, "Successful unregistration from the SAMP hub");

        return true;
    }
    
    private Object lockRegistered = new Object();

    public boolean isRegistered() {
       synchronized( lockRegistered ) { return isRegistered; }
    }
    
    // PF, pour éviter les accès parallèle => Aladin pouvait s'inscrire deux fois
    public void setRegistered(boolean flag) {
       synchronized( lockRegistered ) { isRegistered=flag; }
    }

    // référence au hub interne
    private Hub internalHub;

    public static final String LOCALHOST_PROP = "samp.localhost";
    // if the user has not specified the localhost IP through the samp.localhost system property, force it to 127.0.0.1
    static private String getLocalhost() {
        String localhost = System.getProperty(LOCALHOST_PROP, "");
        if( localhost.length()==0 ) {
            localhost = "127.0.0.1";
        }
        return localhost;
    }

    /**lancement du hub PlasKit, en interne
     *
     * @return true si tout s'est bien passé, false sinon
     */
    public synchronized boolean startInternalHub() {
        Aladin.trace(1, "Starting an internal JSAMP hub");
        try {
            // by default, no message logging
            Logger.getLogger("org.astrogrid.samp").setLevel(Level.OFF);

            String localhost = getLocalhost();
            // set samp.localhost property, so that the JSAMP toolkit use this URL for its hub
            System.setProperty(LOCALHOST_PROP, localhost);

            trace("Hub IP set to "+localhost);


            internalHub = Hub.runHub(HubServiceMode.NO_GUI);
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
        String[] registeredApps = getAppsConnected();


        // si il y a encore d'autres applis connectés au hub interne, on demande si on souhaite arrêter le hub
        if( ! dontAsk &&  ( registeredApps.length>0) ) {
            if( ! Aladin.confirmation(widget, CONFIRM_STOP_HUB) ) return;
        }

        Aladin.trace(1, "Stopping internal SAMP hub");

        internalHub.shutdown();
        internalHub = null;

        updateState();
    }

    /**
     * Retourne un label pour le plan passé en paramètre
     * @param p
     * @return
     */
    private String createLabel(Plan p) {
        return p.label==null?"Plane":p.label;
    }

    private boolean getHubListener(boolean silent) {
        return getHubListener(silent, false);
    }

    // TODO : virer le plasticHubListener
    // TODO : nom methode a renommer !!!
    /**
     * Cherche le fichier de conf .samp, lit l'URL du hub
     * @param silent if true, in silent mode (no popup menu to indicate failure)
     * @param launchHubIfNeeded si silent==true, on essaye automatiquement de lancer un hub s'il n'en existe pas. Sinon, on passe par une boite de dialogue de confirmation
     * @return <i>true</i> si on a pu récupérer le PlasticHubListener, <i>false</i> sinon
     */
    private boolean getHubListener(boolean silent, boolean launchHubIfNeeded) {

        trace("Looking for an existing .samp file");
        File confFile = getLockFile();

        if( !confFile.exists() ) {
            trace("Can't find .samp file, can't registrate with a SAMP hub");

            // ###SAMP

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
                    if( !startInternalHub() && !silent ) Aladin.error(CANT_LAUNCH_HUB);
                    // tout s'est bien déroulé, on attend 1 seconde, et on tente de se connecter au hub créé
                    else {
                        Util.pause(1000);

                        return getHubListener(true);
                    }
                }
            }


            return false;
        }

        // lecture du fichier de conf
        trace("Reading the .samp conf file to retrieve hub info");
        HashMap map = new HashMap();
        try {
            map = readKeysFromHubFile(confFile);
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            return false;
        }

        hubUrl = (String)map.get(SAMP_HUB_URL_KEY);
        if( hubUrl==null ) {
            trace("Can not find key '"+SAMP_HUB_URL_KEY+"' !");
            if( !silent ) Aladin.error(CANT_CONNECT);
            return false;
        }

        sampSecret = (String)map.get(SAMP_SECRET_KEY);
        if( sampSecret==null ) {
            trace("Can not find key '"+SAMP_HUB_URL_KEY+"' !");
            if( !silent ) Aladin.error(SAMP_SECRET_KEY);
            return false;
        }


        try  {
            // testing if the hub is alive (by pinging it)
            URL url = new URL(hubUrl);
            //            hubClient = new XmlRpcClient(url);
            hubClient = new InternalClient(url);

            try {
                hubClient.callAndWait(HUB_MSG_PING, new Vector());
            }
            catch(ConnectException xre) {
                Aladin.trace(3, "ConnectException: Unable to connect to the hub, deleting the .samp file");
                // on efface le fichier ".samp" qui, apparemment, pointe vers un hub ne tournant plus
                try {
                    if( confFile.delete() ) {
                        updateState();
                        return getHubListener(silent, launchHubIfNeeded);
                    }
                    else {
                        if( !silent ) Aladin.error(CANT_CONNECT);
                        return false;
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        catch(java.rmi.ConnectException ce) {
            Aladin.trace(3, "rmi.ConnectException: Unable to connect to the hub, deleting the .samp file");
            // on efface le fichier ".plastic" qui, apparemment, pointe vers un hub ne tournant plus
            try {
                if( confFile.delete() ) {
                    updateState();
                    return getHubListener(silent, launchHubIfNeeded);
                }
                else {
                    if( !silent ) Aladin.error(CANT_CONNECT);
                    return false;
                }
            }
            catch(Exception e) {return false;}
        }
        catch(Exception e) {
            trace("Unable to create the HubListener object");

            //            if( !silent ) Aladin.warning(CANT_CONNECT);
            return false;
        }

        return true;
    }

    private HashMap readKeysFromHubFile(File hubFile) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(hubFile));
        HashMap map = new HashMap();
        String str;

        while( (str=dis.readLine()) != null ) {
            if( str.startsWith("#") ) {
                continue;
            }
            int idx;
            if( (idx=str.indexOf('='))<0 ) {
                continue;
            }
            map.put(str.substring(0,idx), str.substring(idx+1));
        }

        dis.close();

        return map;
    }

    // variable mémorisant l'association entre nom de l'appli --> URI (ID) de l'appli
    private Hashtable appNamesToURI;
    // mémoire des noms des applis
    private Vector appNames;

    public Object getAppWithName(String s) {
        return appNamesToURI.get(s);
    }

    private String getNameForApp(String id) {
        if( appNamesToURI==null ) {
            return "";
        }
        Enumeration e = appNamesToURI.keys();
        Object o;
        while( e.hasMoreElements() ) {

            o = e.nextElement();
            if( id.equals(appNamesToURI.get(o)) ) return ((String)o);
        }

        return "Unknown";
    }

    public String getProtocolName() {
        return "SAMP";
    }

    public void sendMessageLoadImage(String url, String name, List recipients) {
        Map<String, String>paramMap = new Hashtable<>();
        // ajout de l'url
        paramMap.put("url", url);
        // ajout de image-id
        paramMap.put("image-id", url);
        // ajout du name
        paramMap.put("name", name);

        Map message = new Hashtable();
        message.put(KEY_MTYPE, MSG_LOAD_FITS_IMAGE);
        message.put(KEY_PARAMS, paramMap);

        sendNotification(message, (String[])recipients.toArray(new String[recipients.size()]));
    }

    public void sendMessageLoadMOC(String url, String name, List recipients) {
        Map<String, String>paramMap = new Hashtable<>();
        // ajout de l'url
        paramMap.put("url", url);
        // ajout de image-id
        paramMap.put("coverage-id", url);
        // ajout du name
        paramMap.put("name", name);

        Map message = new Hashtable();
        message.put(KEY_MTYPE, MSG_LOAD_FITS_MOC_COVERAGE);
        message.put(KEY_PARAMS, paramMap);

        sendNotification(message, (String[])recipients.toArray(new String[recipients.size()]));
    }


    // Bricolage PIERRE pour Petr Skoda - nov 2015
    // TOPcat refuse de recevoir un spectre ou une série temporelle, je lui envoie comme une table simple
    public void sendMessageLoadVOTable(String url, String id, String name, Map metadata, List recipients) {
        Map paramMap = new Hashtable();
        // ajout de l'url
        paramMap.put("url", url);
        // ajout du spectrum-id
        paramMap.put("table-id", id);
        // ajout des métadonnées
        paramMap.put("meta", metadata);
        // ajout du name
        paramMap.put("name", name);

        Map message = new Hashtable();
        message.put(KEY_MTYPE, MSG_LOAD_VOT_FROM_URL);
        message.put(KEY_PARAMS, paramMap);

        sendNotification(message, recipients==null?null:(String[])recipients.toArray(new String[recipients.size()]));
    }


    public void sendMessageLoadSpectrum(String url, String spectrumId, String spectrumName, Map metadata, List recipients) {
        Map paramMap = new Hashtable();
        // ajout de l'url
        paramMap.put("url", url);
        // ajout du spectrum-id
        paramMap.put("spectrum-id", url);
        // ajout des métadonnées
        paramMap.put("meta", metadata);
        // ajout du name
        paramMap.put("name", spectrumName);

        Map message = new Hashtable();
        message.put(KEY_MTYPE, MSG_LOAD_SPECTRUM);
        message.put(KEY_PARAMS, paramMap);

        sendNotification(message, recipients==null?null:(String[])recipients.toArray(new String[recipients.size()]));
    }

    public void sendMessageLoadCharac(String url, String name, List recipients) {
        // TODO
        System.err.println("Not implemented yet");
    }

    public String getMessage(AppMessagingInterface.AbstractMessage abstractMsg) {
        if( abstractMsg==null ) return null;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_FITS) ) return MSG_LOAD_FITS_IMAGE;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_VOT_FROM_URL) ) return MSG_LOAD_VOT_FROM_URL;

        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_SPECTRUM_FROM_URL) ) return MSG_LOAD_SPECTRUM;

        // TODO : à modifier/compléter
        if( abstractMsg.equals(ABSTRACT_MSG_LOAD_CHARAC_FROM_URL) ) return null;

        return null;
    }

    /**
     * Renvoie les applications pouvant lire du VOTable ET celles pouvant lire des tables FITS
     */
    public ArrayList<String> getAppsSupportingTables() {
        ArrayList<String> tableApps = getAppsSupporting(MSG_LOAD_VOT_FROM_URL);
        tableApps.addAll(getAppsSupporting(MSG_LOAD_FITS_TABLE_FROM_URL));

        return tableApps;
    }

    public synchronized ArrayList<String> getAppsSupporting(AppMessagingInterface.AbstractMessage abstractMsg) {
        String implMsg = getMessage(abstractMsg);
        return getAppsSupporting(implMsg);
    }

    /**
     *
     * @param message un message SAMP
     * @return la liste des noms des applications supportant le message passé en param
     */
    private synchronized ArrayList<String> getAppsSupporting(String message) {
        ArrayList<String> apps = new ArrayList<>();

        if( message==null ) return apps;

        if( !isRegistered() || message==null ) {
            return apps;
        }

        Vector params = new Vector();
        params.add(myPrivateKey);
        params.add(message);
        Map appsMap;
        try {
            appsMap = (Map)hubClient.callAndWait(HUB_MSG_GET_SUBSCRIBED_CLIENTS, params);
        }
        catch(Exception e) {
            e.printStackTrace();
            return apps;
        }

        if( appsMap.size()==0 ) return apps;

        if( appNamesToURI==null ) appNamesToURI = new Hashtable();
        if( appNames==null ) appNames = new Vector();

        String name;
        String appId;
        Iterator<String> it = appsMap.keySet().iterator();
        while( it.hasNext() ) {
            //        for( int i=0; i<listApps.length; i++ ) {
            appId = it.next();

            // on ne retourne pas son propre ID
            if( appId.equals(selfId) ) continue;

            name = null;
            // on cherche si on a déja mémorisé cet id, et on récupère le nom correpondant
            Enumeration e = appNamesToURI.keys();
            String tmp;
            while( name==null && e.hasMoreElements() ) {
                tmp = (String)e.nextElement();
                if( appNamesToURI.get(tmp).equals(appId) ) name = tmp;
            }

            // si on avait pas encore mémorisé ce id, on récupère le nom correspondant, on le rend unique et on l'enregistre
            if( name == null ) {
                name = getAppName(appId);

                if ( name==null ) {
                    name = "Unknown";
                }
                // on rend le nom unique
                int k=1;
                String nameRoot = new String(name);
                while( appNames.contains(name) ) name = nameRoot+"-"+k++;

                // mémorisation du nom
                appNames.addElement(name);
                // mémorisation de la correspondance nom --> URI
                appNamesToURI.put(name, appId);
            }

            apps.add(name);
        }

        Collections.sort(apps);

        return apps;
    }

    /** Retourne le fichier de configuration SAMP ($HOME/.samp à l'heure actuelle)
     *
     * @return Retourne l'objet File représentant le fichier de conf SAMP
     */
    static protected File getLockFile() {
        // TODO : this is not reliable on Windows, see http://www.ivoa.net/Documents/WD/App/SAMP-20080625.html#tthFtNtAAC

        String homeDir;
        if( windowsPlatform() ) {
            try {
                homeDir = System.getenv("USERPROFILE");
            }

            // System.getenv only exists in Java>=1.5
            // Earlier version throws an error
            catch(Throwable e) {
                // in that case, let's try with a windows command
                try {
                    String[] argv = { "cmd", "/c", "echo", "%USERPROFILE%", };
                    homeDir = exec(argv);
                }
                catch (Throwable e2) {
                    homeDir = null;
                }
            }

            // en dernier recours !
            if( homeDir==null ) {
                homeDir = System.getProperty("user.home");
            }

        }
        else {
            homeDir = System.getProperty("user.home");
        }
        return new File(homeDir,SAMP_CONF_FILE);
    }

    // are we running on a windows platform ?
    static protected boolean windowsPlatform() {
        String osName = System.getProperty("os.name");

        return    osName.toLowerCase().indexOf("windows")>=0
                || osName.toLowerCase().indexOf("microsoft")>=0;
    }

    /**
     * execute an external command, and retrieve the ouput string
     * @param args
     * @return
     * @throws IOException
     */
    static private String exec(String[] args) throws IOException {
        String argv = Arrays.asList(args).toString();
        Process process;
        try {
            process = Runtime.getRuntime().exec(args);
            process.waitFor();
        }
        catch(InterruptedException e) {
            throw new IOException("Execution failed: " + argv);
        }
        catch(IOException e) {
            throw (IOException)
            new IOException("Execution failed: " + argv).initCause(e);
        }
        if( process.exitValue() == 0 ) {
            return readStream(process.getInputStream());
        }
        else {
            String err;
            try {
                err = readStream(process.getErrorStream());
            }
            catch(IOException e) {
                err = "??";
            }
            throw new IOException("Execution failed: " + argv + " - " + err);
        }
    }

    /**
     * fully read a stream, and return the result as a string
     * @param in
     * @return
     * @throws IOException
     */
    static private String readStream(InputStream in) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            for( int c; (c = in.read()) >= 0; ) {
                sb.append((char)c);
            }
            return sb.toString();
        }
        finally {
            try {
                in.close();
            }
            catch(IOException e) {
            }
        }
    }

    private static final Object VOID = "";
    
    // PF 3/7/2022 - Les booléens ne sont pas supporté par SAMP, uniquement par PLASTIC
    // Il faut utiliser une chaine "0" ou "1" à la place.
//    private static final Object TRUE = Boolean.TRUE;
//    private static final Object FALSE = Boolean.FALSE;
    private static final Object TRUE = "1";
    private static final Object FALSE = "0";

    /** Select in aladin objects designated by their oid
     *
     * @param oid
     */
    //    private synchronized void selectObjects(String[] oid, Object[] argArray) {
    //        selectSources(argArray);
    //    }

    /** Traitement du message MSG_SELECT_OBJECTS
     *
     * @param args
     */
    private boolean selectSources(String tableId, int[] rowIdxArray) {
        if( tableId==null ) {
            trace("selectSources : argument is null, can't do anything !");
            return false;
        }


        //       on cherche le plan concerné
        Plan p = findPlaneByPlasticID(tableId);
        if( p==null ) {
            trace("Could not find plane with plastic ID "+tableId);
            return false;
        }


        if( rowIdxArray==null ) {
            trace("selectSources : second parameter is null, can't do anything !");
            return false;
        }

        a.view.selectSourcesByRowNumber((PlanCatalog)p, rowIdxArray);

        return true;
    }

    public void pointAtCoords(final double ra, final double dec) {
        if( hubClient==null ) return;

        new Thread() {
            public void run() {
                Object result;
                Map msgParams = new Hashtable();
                msgParams.put("ra", String.valueOf(ra));
                msgParams.put("dec", String.valueOf(dec));
                Map pointAtMsg = new Hashtable();
                pointAtMsg.put(KEY_MTYPE, MSG_POINT_AT_COORDS);
                pointAtMsg.put(KEY_PARAMS, msgParams);

                sendNotification(pointAtMsg, null);
            }
        }.start();
    }

    Integer oidx;
    String oid;
    /** traitement du message MSG_HIGHLIGHT_OBJECT
     *
     * @param id id de la table dans laquelle se trouve l'objet à highlighter
     * @param idx numéro d'ordre de la source à highlighter
     *
     */
    private synchronized boolean highlightObject(String id, Integer idx) {

        oid = id;
        oidx = idx;
        //       on cherche le plan concerné
        Plan p = findPlaneByPlasticID(id);

        if( p==null ) {
            trace("Could not find plane with plastic ID "+id);
            return false;
        }

        if( idx.intValue()>=p.pcat.getCount() ) {
            return false;
        }
        final Source src = (Source)p.pcat.getObj(idx.intValue());

        if( a.mesure.findSrc(src)==-1 ) a.view.setSelected(src,true);
        a.mesure.mcanvas.show(src,2);

        // PF - sept 2010 - c'est plus propre via gotoThere(...)
        a.view.gotoThere(src);
        //        a.view.repaintAll();
        //        // make the object blink
        //        a.view.showSource(src);

        return true;
    }

    /**
     * Loads a VOTable into Aladin from an URL
     * @param votable URL of the VOTable
     * @param id id de la VOTable
     * @param name name to label the plane, might be null
     * @param sender URI de l'appli ayant envoyé le message
     */
    private synchronized Plan loadVOTFromURL(String votURL, String id, String name, String sender) {
        URL url;
        try {
            url = new URL(votURL);
        }
        catch(MalformedURLException e) {
            trace("The provided URL string is malformed, can not load VOTable file !");
            return null;
        }

        InputStream is;
        try {
            is = Util.openStream(url);
        }
        catch(IOException e) {
            trace("IOException occured when getting stream from VOTable URL, loading is aborted");
            return null;
        } catch (Exception e) {
            trace("Exception occured when getting stream from VOTable URL, loading is aborted");
            return null;
        }
        String planeName = name==null?"SAMP":name;
        if( name==null && id!=null && ! id.startsWith("http") ) {
            planeName = id;
        }

        int idx = a.calque.newPlan(is, planeName, sender);
        if( idx>=0 ) {
            Plan p = a.calque.plan[idx];
            p.addPlasticID(id);
            return p;
        }
        else {
            return null;
        }
    }

    /**
     * Loads a FITS file into Aladin from an URL
     * @param fitsURL URL of the FITS file
     * @param id to identify this image
     * @param name name to label the plane, might be null
     * @param sender
     */
    private synchronized Plan loadFitsImageFromURL(String fitsURL, String id, String name, String sender) {
        // just to test that the URL is well-formed
        try {
            new URL(fitsURL);
        }
        catch(MalformedURLException e) {
            trace("The provided URL string is malformed, can not load FITS file !");
            return null;
        }

        String planeName = name==null?"SAMP":name;
        if( name==null && id!=null && ! id.startsWith("http") ) {
            planeName = id;
        }

        // la methode est synchronisee pour eviter que l'idx ne change entre les 2 lignes suivantes...
        int idx = a.calque.newPlan(fitsURL, planeName, sender);
        if( idx>=0 ) {
            Plan p = a.calque.plan[idx];
            p.addPlasticID(id);
            return p;
        }
        else {
            return null;
        }
    }

    /**
     * Loads a VOTable into Aladin
     * @param votable the votable string
     */
    private synchronized Plan loadVOT(String votable, String id, URI sender) {
        int idx =  a.calque.newPlan(new ByteArrayInputStream(votable.getBytes()), "SAMP", sender.toString());
        if( idx>=0 ) {
            Plan p = a.calque.plan[idx];
            p.addPlasticID(id);
            return p;
        }
        else {
            return null;
        }
    }

    // TODO : à remplacer par isSupporting
    /**
     * returns whether the message is supported
     * @param message
     * @return
     */
    protected boolean supportMessage(URI message) {
        // pour le moment !
        return true;
    }

    /**
     * looks in the SAMP preferences to see if we support this message
     * @param sender
     * @param message
     * @return
     */
    protected boolean mustProcessMessage(URI sender, URI message) {
        // pour le moment !
        return true;
    }

    protected Map getAppMetadata(String appId) {
        try {
            Vector params = new Vector();
            params.add(myPrivateKey);
            params.add(appId);
            return (Map)hubClient.callAndWait(HUB_MSG_GET_METADATA, params);
        }
        catch(Exception e) {
            return null;
        }
    }

    protected String getAppName(String appId) {
        Map m = getAppMetadata(appId);
        if( m == null ) return null;

        Object name = m.get("samp.name");
        return name!=null ? name.toString() : null;
    }

    /**
     * @param message the message map, made of
     * @param recipients array of recipient ids. If null, notify to all
     */
    private void sendNotification(Map message, final String[] recipients) {

        if( !isRegistered() ) {
            return;
        }

        // notify all apps
        if( recipients==null ) {
            Vector params = new Vector();
            params.add(myPrivateKey);
            params.add(message);
            try {
                hubClient.callAndWait(NOTIFY_ALL, params);
            }
            catch(Exception e) {
                System.err.println("Problem when sending message "+message);
                e.printStackTrace();
            }
        }
        else {

            for( int i=0; i<recipients.length; i++ ) {
                Vector params = new Vector();
                params.add(myPrivateKey);
                params.add(recipients[i]);
                params.add(message);
                try {
                    hubClient.callAndWait(NOTIFY, params);
                }
                catch(Exception e) {
                    System.err.println("Problem when sending message "+message);
                    e.printStackTrace();
                }
            }
        }
    }



    /** "Diffusion" du PlanCatalog à toutes les applications SAMP
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
        if( ( tmpFile = a.createTempFile("samp"+SAMPUtil.sanitizeFilename(pc.getLabel()), ".xml") ) == null ) {
            trace("Couldn't create temporary file, can't broadcast table !");
            return false;
        }

        // La suite est réalisée dans un thread à part, afin de ne pas bloquer toute l'appli
        new Thread("AladinSampSendTable") {
            public void run() {
                if( a.save==null ) a.save = new Save(a);
                a.save.saveCatVOTable(tmpFile, pc, false,false);
                URL url = SAMPUtil.getURLForFile(tmpFile);
                String urlStr = url.toString();
                String id = pc.getPlasticID();
                if( id==null ) {
                    id = urlStr;
                    pc.addPlasticID(id);
                }

                // envoi message SAMP
                Map message = new Hashtable();
                message.put(KEY_MTYPE, MSG_LOAD_VOT_FROM_URL);

                Map paramMap = new Hashtable();
                // ajout de l'url
                paramMap.put("url", urlStr);
                // ajout de table-id
                paramMap.put("table-id", id);
                trace("id initial : "+id);
                // ajout du name
                String label = createLabel(pc);
                paramMap.put("name", label);
                message.put(KEY_PARAMS, paramMap);

                trace("Broadcast table with URL: "+urlStr);

                ArrayList recipientsList = null;
                // Si envoi ciblé
                if( recipients!=null ){
                    recipientsList = new ArrayList();
                    Object app;
                    for( int i=0; i<recipients.length; i++ ) {
                        // TODO : on pourrait vérifier que l'appli supporte bien le message
                        app = appNamesToURI.get(recipients[i]);
                        if( app!=null ) {
                            recipientsList.add(app);
                            trace("Adding "+app+" to list of recipients");
                        }
                        else trace("Couldn't find ID of application "+recipients[i]);
                    }
                }

                sendNotification(message, recipients==null?null:(String[])recipientsList.toArray(new String[recipientsList.size()]));
                //                sendAsyncMessage(PlasticManager.MSG_LOAD_VOT_FROM_URL, paramList, recipientsList);
                a.log("SAMP", "broadcast table");
            }
        }.start();

        return true;
    }

    // TODO : classe message


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
        if( ( tmpFile = a.createTempFile("samp"+SAMPUtil.sanitizeFilename(pi.getLabel()), ".fits") ) == null ) {
            trace("Couldn't create temporary file, can't broadcast image !");
            return false;
        }

        // La suite est réalisée dans un thread à part, afin de ne pas bloquer toute l'appli
        new Thread("AladinSAMPSendImage") {
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

                // envoi message SAMP
                Map message = new Hashtable();
                message.put(KEY_MTYPE, MSG_LOAD_FITS_IMAGE);

                Map paramMap = new Hashtable();
                // ajout de l'url
                paramMap.put("url", urlStr);
                // ajout de image-id
                paramMap.put("image-id", id);
                // ajout du name
                String label = createLabel(pi);
                paramMap.put("name", label);
                message.put(KEY_PARAMS, paramMap);

                ArrayList recipientsList = null;
                // Si envoi ciblé
                if( recipients!=null ){
                    recipientsList = new ArrayList();
                    Object app;
                    for( int i=0; i<recipients.length; i++ ) {
                        // TODO : on pourrait vérifier que l'appli supporte bien le message
                        app = appNamesToURI.get(recipients[i]);
                        if( app!=null ) {
                            recipientsList.add(app);
                            trace("Adding "+app+" to list of recipients");
                        }
                        else trace("Couldn't find ID of application "+recipients[i]);
                    }
                }

                sendNotification(message, recipients==null?null:(String[])recipientsList.toArray(new String[recipientsList.size()]));
                a.log("SAMP", "broadcast image");
            }
        }.start();

        return true;
    }

    ResourceChooser resourceChooser;
    /**
     * Crée les objets Server correspondant aux VOResource pointés par les uris
     * @return false si probleme durant parsing, true sinon
     */
    private boolean loadVOResources(Object[] args, String senderId) {
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

        resourceChooser.updateFrame(uris, senderId);
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

        void updateFrame(final String[] uris, String senderId) {
            this.uris = uris;

            getContentPane().removeAll();
            ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JPanel pTop = new JPanel();
            JLabel label = new JLabel("<html><b>"+getNameForApp(senderId)+"</b> has sent some registry resources.<br>Select resources you want to keep:<br></html>");
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

            new Thread("SAMPManager:resolveIVORN") {
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
            if( ! Aladin.PROTO ) return;

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
                    FrameServer.createRecord(bas, curIvorn.type, curIvorn.actionName, curIvorn.institute, curIvorn.baseUrl, curIvorn.desc, curIvorn.identifier, "SAMP");
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

        // on prend en compte les preferences SAMP
        if( ! a.plasticPrefs.getBooleanValue(PlasticPreferences.PREF_SELECT) )
            return;


        if( !isRegistered() ) {
            //          Aladin.trace(3, "Aladin is not registered with a PLASTIC hub, will not send a 'select objects' message");
            return;
        }



        // on lance un nouveau thread pour ne pas bloquer Aladin
        // lors de la récupération des objects sélectionnées
        new Thread("AladinSampSendSelect") {
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

                // TODO : on recupere une map, il faut faire une boucle sur tous les plans en question !!

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
                String[] idxItems = new String[v.size()];
                Enumeration e = v.elements();
                int k = 0;
                Integer idx;
                while( e.hasMoreElements() ) {
                    try {
                        idx = (Integer)e.nextElement();
                    }
                    catch(ClassCastException cce) {
                        a.command.println("Encountered bad format for SAMP int");
                        cce.printStackTrace();
                        continue;
                    }
                    idxItems[k] = idx.toString();
                    k++;
                }

                Map message = new Hashtable();
                message.put(KEY_MTYPE, MSG_SELECT_OBJECTS);
                Map paramMap = new Hashtable();
                message.put(KEY_PARAMS, paramMap);

                // TODO : enlever log SAMP, sauf en mode SAMP trace

                //              TODO : String plasticId ?? que faire si plasticId est null ? --> en crer un !!
                // TODO : plasticId est par exemple null pour les tables issues d'exportation de filtre
                // TODO : pas besoin d'envoyer ce message si le plan correspondant n'a jamais été broadcasté
                paramMap.put("table-id", p.getPlasticID());
                trace("envoi id : "+p.getPlasticID());
                // this parameter needs to be casted as an ArrayList in order to be properly serialized
                paramMap.put("row-list", Arrays.asList(idxItems));

                sendNotification(message, null);
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

        // TODO : construire une Map plan-->arraylist de sources

        return (sources==null || sources.length==0)?null:sources[0].plan;

        //      Plan p = null;
        //
        //      // Pour chaque OID à trouver, parcours de tous les plans CATALOG
        //      for( int k=0; k<a.calque.plan.length; k++ ) {
        //         if( sources.length==0 ) break;
        //
        //         Plan pTmp = a.calque.plan[k];
        //         if( pTmp.type!=Plan.CATALOG || pTmp.pcat==null ) continue;
        //         for( int j=0; j<pTmp.pcat.nb_o; j++ ) {
        //            Source sTmp = ((Source)pTmp.pcat.o[j]);
        //            if( sTmp!=null && sTmp.equals(sources[0]) ) {
        //               p = pTmp;
        //               break;
        //            }
        //         }
        //      }
        //
        //      return p;
    }

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
            for( int j=0; j<sources.length; j++ ) {
                if( o==null ) continue;
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
        new Thread("AladinSAMPUpdate") {
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
    protected String[] getAppsConnected() {
        Vector v = new Vector();
        try {
            Vector params = new Vector();
            params.add(myPrivateKey);
            Iterator it = ((Vector)hubClient.callAndWait(HUB_MSG_GET_REGISTERED_CLIENTS, params)).iterator();

            String curId;
            while( it.hasNext() ) {
                curId = it.next().toString();
                if( curId.equals(selfId.toString()) || curId.equals(hubId) ) continue;
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
        // on prend en compte les preferences SAMP
        if( ! a.plasticPrefs.getBooleanValue(PlasticPreferences.PREF_HIGHLIGHT) )
            return;

        //      Aladin.trace(3, "Sending message "+MSG_HIGHLIGHT_OBJECT+" for source "+source);


        // TODO : que faire si source==null ? envoi de null pour ne plus hilighter le dernier objet hilighté ?
        if( source==null ) {
            return;
        }

        if( !isRegistered() ) {
            //          Aladin.trace(3, "Aladin is not registered with a SAMP hub, will not send a 'highlight object' message");
            return;
        }

        Source[] sources = new Source[] {source};

        Plan p = findPlaneForSources(sources);

        if( p==null ) {
            trace("Could not find plane, can't send SAMP message");
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

        Map message = new Hashtable();
        message.put(KEY_MTYPE, MSG_HIGHLIGHT_OBJECT);

        Map paramMap = new Hashtable();
        // ajout de table-id
        paramMap.put("table-id", p.getPlasticID());
        // ajout de l'index de la source à mettre en valeur
        paramMap.put("row", idx.toString());
        message.put(KEY_PARAMS, paramMap);

        // TODO : permettre de choisir les destinataires ??
        sendNotification(message, null);
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




    /**
     * ping the hub
     */
    public boolean ping() {
        if( hubClient==null ) {
            return false;
        }

        try {
            hubClient.callAndWait(HUB_MSG_PING, new Vector());
        }
        catch(Exception e) {
            unregister(true);
            setRegistered(false);
            updateState();
            return false;
        }

        if( updateAppsListNeeded ) {
            getAppsSupporting(MSG_PING);
            updateAppsListNeeded = false;
        }
        return true;
    }

    /**
     * méthode à appeler quand on souhaite répondre à un message
     * @param msgId
     * @param status
     * @param result
     * @param error
     */
    private void replyToMessage(String msgId, String status, Map result, String error) {
        if (msgId==null) {
            trace("Can not reply to message because message-id has not been set in initial message !");
            return;
        }
        Map responseMap = new Hashtable();
        responseMap.put(MSG_REPLY_SAMP_STATUS, status);
        responseMap.put(MSG_REPLY_SAMP_RESULT, result==null?new Hashtable():result);
        if( error!=null ) {
            Map errorMap = new Hashtable();
            errorMap.put("samp.errortxt", error);
            responseMap.put(MSG_REPLY_SAMP_ERROR, errorMap);
        }

        final Vector params = new Vector();
        params.add(myPrivateKey);
        params.add(msgId);
        params.add(responseMap);
        new Thread() {
            public void run() {
                try {
                    hubClient.callAndWait(HUB_MSG_REPLY, params);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public void setPlasticWidget(PlasticWidget widget) {
        this.widget = widget;
    }

    public PlasticWidget getPlasticWidget() {
        return this.widget;
    }

    private boolean sampTrace = false;

    // mode "trace" pour débugging PLASTIC
    public void trace(String s) {
        if( !sampTrace ) return;

        System.out.println("** SAMP : "+s);
    }

    /**
     * @return Returns the sampTrace object.
     */
    public boolean getPlasticTrace() {
        return this.sampTrace;
    }
    /**
     * @param plasticTrace The plasticTrace value to set.
     */
    public void setPlasticTrace(boolean plasticTrace) {
        this.sampTrace = plasticTrace;

        if( this.sampTrace ) {
            // enable JSAMP message logging if trace is on
            Logger.getLogger("org.astrogrid.samp").setLevel(Level.ALL);
        }
        else {
            // disable JSAMP message logging if trace is off
            Logger.getLogger("org.astrogrid.samp").setLevel(Level.OFF);
        }
    }

    public void planeLoaded(PlaneLoadEvent ple) {
        // find msgId corresponding to the plane being loaded
        String msgId = planesToMsgIds.get(ple.plane);
        if( msgId==null ) {
            System.err.println("Ohoh, something weird happened : could not find msgId for plane "+ple.plane.getLabel());
            return;
        }
        trace("Received PlaneLoadEvent from plane "+ple.plane.getLabel());
        if( ple.status==PlaneLoadEvent.SUCCESS ) {
            replyToMessage(msgId, MSG_REPLY_SAMP_STATUSOK, null, null);
        }
        else {
            replyToMessage(msgId, MSG_REPLY_SAMP_STATUSERROR, null, ple.errorMsg);
        }
        ple.plane.removeListener(this);
        planesToMsgIds.remove(ple.plane);

    }

    @Override
    // PF 25 mai 2017
    public boolean canHandleCall(String arg0) {
        return true;
    }

    @Override
    // PF 25 mai 2017
    public Object handleCall(String arg0, List arg1, Object arg2) throws Exception {
        return execute(arg0, new Vector(arg1) );
    }

}
