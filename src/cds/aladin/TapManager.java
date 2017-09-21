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

package cds.aladin;

import static cds.aladin.Constants.ALADINTABLEPREFIX;
import static cds.aladin.Constants.COLUMNNAME;
import static cds.aladin.Constants.COUNT;
import static cds.aladin.Constants.DATATYPE;
import static cds.aladin.Constants.DESCRIPTION;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.GETRESULTPARAMS;
import static cds.aladin.Constants.INDEXED;
import static cds.aladin.Constants.OBSCORE;
import static cds.aladin.Constants.PATHSYNC;
import static cds.aladin.Constants.PRINCIPAL;
import static cds.aladin.Constants.SCHEMANAME;
import static cds.aladin.Constants.SIZE;
import static cds.aladin.Constants.STD;
import static cds.aladin.Constants.TABLENAME;
import static cds.aladin.Constants.TABLETYPE;
import static cds.aladin.Constants.TAPFORM_STATUS_ERROR;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTLOADED;
import static cds.aladin.Constants.UCD;
import static cds.aladin.Constants.UNIT;
import static cds.aladin.Constants.UTF8;
import static cds.aladin.Constants.UTYPE;
import static cds.tools.CDSConstants.DEFAULT;
import static cds.tools.CDSConstants.WAIT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import adql.db.DefaultDBTable;
import adql.query.ADQLQuery;
import cds.aladin.Constants.TapClientMode;
import cds.savot.binary.DataBinaryReader;
import cds.savot.model.FieldSet;
import cds.savot.model.SavotBinary;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.ConfigurationReader;
import cds.tools.MultiPartPostOutputStream;
import cds.tools.TwoColorJTable;
import cds.tools.Util;
import cds.xml.DaliExamplesReader;
import cds.xml.Field;
import cds.xml.TapQueryResponseStatusReader;
import cds.xml.VOSICapabilitiesReader;

public class TapManager {
	
	private static TapManager instance = null;
	private boolean initAllLoad = true;
	ExecutorService executor;
	
	public Aladin aladin;
	protected static List<DataLabel> splTapServerLabels;
	protected static List<DataLabel> allTapServerLabels;
	protected TapFrameServer tapFrameServer=null;
	protected FrameUploadServer uploadFrame;
	
	protected static Map<String, TapClient> tapServerPanelCache = new HashMap<String, TapClient>();//main cache where all the ServerGlu's are loaded on init
	protected static Map<String, TapClient> tapServerTreeCache = new HashMap<String, TapClient>();//cache for the servers loading from tree
	public static final String GETTAPSCHEMACOLUMNCOUNT = "REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+COUNT(*)+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMACOLUMNS = "REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMATABLES = "REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.tables";
	public static final String GETTAPSCHEMACOLUMN = "REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+where+table_name+=+'%1$s'+";
	public static final String GETTAPCAPABILITIES = "capabilities";
	public static final String GETTAPSCHEMATABLE = GETTAPSCHEMATABLES+"+where+table_name+=+'%1$s'+";
	public static final String GETTAPEXAMPLES = "examples";
	public static final String GENERICERROR;
	public static final int MAXTAPCOLUMNDOWNLOADVOLUME = 1000;//limit for the table columns that will be downloaded together. 409 cadc;1000 decided limit
	
	protected List<String> eligibleUploadServers;//URls of servers loaded and allow uploads
	
	public UWSFacade uwsFacade;
	public FrameSimple tapPanelFromTree;
	private int getRequest_timeout = 1000;
	
	FrameTapSettings settingsFrame;
	
	static {
		GENERICERROR = null;//Aladin.getChaine().getString("GENERICERROR");
	}
	
	public TapManager() {
		// TODO Auto-generated constructor stub
		executor = Executors.newFixedThreadPool(10);
	}
	
	public TapManager(Aladin aladin) {
		// TODO Auto-generated constructor stub
		this();
		uwsFacade = UWSFacade.getInstance(aladin);
		this.aladin = aladin;
		
	}
	
	public static synchronized TapManager getInstance(Aladin aladin) {
		if (instance == null) {
			instance = new TapManager(aladin);
		}
		return instance;
	}
	
	/**
	 * Method initializes from the local file for one time
	 * @return
	 * @throws Exception
	 */
	public List<DataLabel> getTapServerList(int mode) {
		List<DataLabel> result = null;
		if (initAllLoad) {
//			populateTapServerListFromLocalFile(); //tintin comment this for commit
			// load from glu here
			populateTapServerListFromLocalFile();// tintin here dummy as well
			populateFromDirectory();//loading from directory
		}
		switch (mode) {
		case 0:
			result = splTapServerLabels;
			break;
		case 1:
			result = allTapServerLabels;
			break;
		default:
			result = new ArrayList<DataLabel>();
			if (allTapServerLabels != null) {
				result.addAll(allTapServerLabels);
			}
			if (splTapServerLabels != null) {
				result.addAll(splTapServerLabels);
			}
			break;
		}
		return result;
	}
	
	/**
	 * Adds new tap server to tapserver list
	 * Also takes care of updates to the server. 
	 * 		ActionName is the key to an update.
	 * @param actionName
	 * @param label
	 * @param url
	 * @param description
	 */
	public void addTapService(String actionName, String label, String url, String description) {
		if (actionName != null) {
			DataLabel newDatalabel = new DataLabel(label, url, description);
			if (splTapServerLabels == null) {
				splTapServerLabels = new ArrayList<DataLabel>();
			}
			removeOldEntries(label);
			splTapServerLabels.add(newDatalabel);
		}
	}
	
	public void removeOldEntries(String actionName) {
		for (DataLabel datalabel : splTapServerLabels) {
			if (datalabel.getLabel().equals(actionName)) {
				splTapServerLabels.remove(datalabel);
				break;
			}
		}
	}

	/**
	 * Method to populate all the tap servers in the configuration file into a tap frame server
	 * @throws Exception 
	 */
	private void populateTapServerListFromLocalFile(){
		BufferedReader bufReader = null;
		try {
			if (allTapServerLabels == null) {
				allTapServerLabels = new ArrayList<DataLabel>();
			}
			String fileLine;
			String label;
			String url;
			String description;
			bufReader = new BufferedReader(new InputStreamReader(aladin.getClass().getResourceAsStream("/"+Constants.TAPSERVERS)));
			while ((fileLine = bufReader.readLine()) != null) {
				if (fileLine.equals("") || fileLine.charAt(0) == '#') continue;
				label = fileLine.split("\\s+")[0];
				url = fileLine.split("\\s+")[1];
				description = fileLine.replaceFirst(label, "").replaceFirst(url, "").replaceFirst("\\s+", "");
				allTapServerLabels.add(new DataLabel(label, url, description));
			}
			
		} catch (Exception e) {
			System.err.println("TapServer.txt not loaded error:"+e);
            e.printStackTrace();
			Aladin.warning("TapServer.txt not loaded !", 1);
		} finally {
			initAllLoad = false;
			if (bufReader!=null) {
				try {
					bufReader.close();
				} catch (IOException e) {
					System.err.println("error when closing TapServer.txt:"+e);
		            e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * To populate the tap servers configured in the directory tree
	 */
	private void populateFromDirectory() {
		try {
			if (splTapServerLabels == null) {
				splTapServerLabels = new ArrayList<DataLabel>();
			}
			String label;
			String url;
			String description;
			ArrayList<String> tapServers = aladin.directory.getBigTAPServers(5);
			for (String dirTapServerInfoLine : tapServers) {
				if (dirTapServerInfoLine.equals("") || dirTapServerInfoLine.charAt(0) == '#') continue;
				label = dirTapServerInfoLine.split("\\s+")[0];
				url = dirTapServerInfoLine.split("\\s+")[1];
				description = dirTapServerInfoLine.replaceFirst(label, "").replaceFirst(url, "").replaceFirst("\\s+", "");
				splTapServerLabels.add(new DataLabel(label, url, description));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace >=3) e.printStackTrace();
			Aladin.warning("Unable to load tap servers from directory !", 1);
		} finally {
			initAllLoad = false;
			Aladin.trace(3, "populateFromDirectory():: Done loading from directory...");
		}
	}
	
	/**
	 * Replaces tap server reference before displaying on the Server selector dialog
	 * @param server
	 */
	public void showTapServerOnServerSelector(Server server) {
		aladin.dialog.findReplaceServer(aladin.dialog.tapServer, server);
		aladin.dialog.tapServer = server;
		this.aladin.dialog.show(server);
	}
	
	public void showTapPanelFromTree(String label, Server resultServer) {
		// TODO Auto-generated method stub
		if (tapPanelFromTree == null) {
			tapPanelFromTree = new FrameSimple(aladin);
		}
		Aladin.makeCursor(tapPanelFromTree, WAIT);
		tapPanelFromTree.show(resultServer, "TAP access with "+label);
		Aladin.makeCursor(tapPanelFromTree, DEFAULT);
	}
	
	/**
	 * Loads tap server in the server dialog
	 * @param label
	 * @param url
	 * @param modeSelected
	 * @throws Exception
	 */
	public void loadTapServer(String label, String url, String modeSelected) throws Exception {
		//either get exixitng details form the configuration file or call url to get tap server details.
		if (label != null) {//not a necessary condition. But adding just in case.
			TapClient tapClient = null;
			if (tapServerPanelCache.containsKey(label)) {
				tapClient = tapServerPanelCache.get(label);
				tapClient.tapBaseUrl = url;//in case there is any updates to the generic server url
			} else {//not in cache..and does not have a glu record
				if (tapServerTreeCache.containsKey(label)){//check in the other cache if there is a reference.
					tapClient = copyTapClientAndDisplay(tapServerTreeCache.get(label), url, TapClientMode.DIALOG);
				} else {
					//final resort is to create generic form ServerTap
					tapClient = new TapClient(TapClientMode.DIALOG, this, label, url);
				}
				tapServerPanelCache.put(label, tapClient);
			}
			Server resultServer = tapClient.getServerToDisplay(modeSelected);
			aladin.dialog.findReplaceServer(aladin.dialog.tapServer, resultServer);
			aladin.dialog.tapServer = resultServer;
		}
	}
	
	/**
	 * Method creates a tap server from label(primary ID) and url
	 * - server is either created from glu records or a generic panel is created
	 * @param label
	 * @param url
	 * @return resultant server tap panel
	 * @throws Exception 
	 */
	public Server loadTapServerForSimpleFrame(String label, String url, String serverType) throws Exception {
		TapClient tapClient = null;
		if (tapServerTreeCache.containsKey(label)) {//get from cache
			tapClient = tapServerTreeCache.get(label);
			tapClient.tapBaseUrl = url;
		} else {//not in cache..and does not have a glu record
			if (tapServerPanelCache.containsKey(label)){//try to create from the main cache
				tapClient = copyTapClientAndDisplay(tapServerPanelCache.get(label), url, TapClientMode.TREEPANEL);
			} else {
				tapClient = new TapClient(TapClientMode.TREEPANEL, this, label, url);
			}
			tapServerTreeCache.put(label, tapClient);
		}
		Server resultServer = tapClient.getServerToDisplay(serverType);
		showTapPanelFromTree(label, resultServer);
		return resultServer;
	}
	
	/**
	 * Creates a generic tap server
	 * Slight changes in resultant server w.r.t to the container: TreePanel or ServerDialog
	 * @param tapClient
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	public DynamicTapForm createAndLoadServerTap(TapClient tapClient, DynamicTapForm newServer) throws Exception {
		newServer.setName(tapClient.tapLabel);
		if (tapClient.tapBaseUrl == null) {
			Object urlFromGlu = aladin.glu.aladinDic.get(tapClient.tapLabel);
			if (urlFromGlu != null) {//as a last resort glu url can also be used for generic
				tapClient.tapBaseUrl = (String) urlFromGlu;
			}
			if (tapClient.tapBaseUrl == null) {
				throw new Exception("Tap server url not found");
			}
		}
		// we only get nodes in trees for now. from tap server list we do not support taking table param as of now.
		String vizierTable = processIfVizier(tapClient);
		// just another control on the nodes feature
		boolean nodeOn = (ConfigurationReader.getInstance().getPropertyValue("TAPNODES").contains("enable"));
		if (tapClient.mode != null && tapClient.mode == TapClientMode.TREEPANEL) {
			tapClient.primaryColor = Aladin.COLOR_FOREGROUND;
			tapClient.secondColor = Color.white;
			nodeOn = true;
		}
		newServer.setOpaque(true);
		newServer.showloading();
		tapClient.capabilities = this.getTapCapabilities(tapClient.tapBaseUrl);
		if (nodeOn && vizierTable != null) {
			//if you have a table name
			this.loadTapColumnSchemasForATable(tapClient, vizierTable, newServer);
		} else {
			this.loadTapColumnSchemas(tapClient, newServer);
		}
		return newServer;
	}

	/**
	 * Only tests out if the service is vizier. 
	 * This is temporary workaround to treat vizier as a special case:
	 * where only vizier's registry tables names are known to be same as the Tap.Schema tables
	 * so for that registry node we can download metadata for that table only
	 * We will enlarge the logic as and when we realize this about the other servers.
	 * because currently this is not the case for other servers.
	 * @return
	 */
	private String processIfVizier(TapClient tapClient) {
		// TODO Auto-generated method stub
		String result = null;
		String vizierTapServiceBaseUrl = "http://tapvizier.u-strasbg.fr/TAPVizieR/tap";
		if (tapClient.tapBaseUrl != null && tapClient.tapBaseUrl.startsWith(vizierTapServiceBaseUrl)) {
			try {
				result = tapClient.tapLabel.substring(4, tapClient.tapLabel.length());
			} catch (Exception e) {
				// TODO: handle exception
				// Won't do anything
			}
		}

		return result;
	}

	public TapClient copyTapClientAndDisplay(TapClient original, String urlInput, TapClientMode mode) {
		TapClient copy = null;
		if (original.serverGlu != null) {
			ServerGlu serverGluCopy = null;
			StringBuffer originalGluRecord = original.serverGlu.record;
			String gluRecord = originalGluRecord.toString();
			if (mode == TapClientMode.TREEPANEL) {
//				gluRecord = gluRecord.concat("%Aladin.LabelPlane TREEPANEL");
				gluRecord = gluRecord.concat("%Aladin.Protocol TAPv1-TREEPANEL");
			} else {
				gluRecord = gluRecord.concat("%Aladin.Protocol TAPv1");
//				gluRecord = gluRecord.concat("%Aladin.LabelPlane GLU");
			}
			serverGluCopy = getCopy(originalGluRecord, gluRecord);
			copy = serverGluCopy.tapClient;
		} 
		
		if (copy == null) {//only if serverglu is null yet
			copy = new TapClient(mode, this, original.tapLabel, urlInput);
		}
		
		copyMetadata(copy, original, mode);
		
		if (original.serverTap != null && original.tapBaseUrl.equalsIgnoreCase(urlInput)) { //at this point generic form ui is not created. only metadata is copied
			copy.serverTap =  new ServerTap(aladin);
			copy.serverTap.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
			copy.serverTap.tapClient = copy;
		}
		return copy;
	}
	
	/**
	 * Specific method This method is to handle a specific case where serverglu
	 * is updated for both the caches and we load the servertap for one cache at
	 * this point the metadata of the tapclient of the other cache also needs to
	 * be updated
	 * 
	 * @param original
	 * @return
	 */
	public void updateServerMetaDataInCache(TapClient original) {
		TapClientMode mode = null;
		TapClient toUpdate = null;
		if (original.mode == TapClientMode.DIALOG) {
			mode = TapClientMode.TREEPANEL;
			toUpdate = tapServerTreeCache.get(original.tapLabel);
		} else {
			mode = TapClientMode.DIALOG;
			toUpdate = tapServerPanelCache.get(original.tapLabel);
		}

		if (toUpdate != null) {
			copyMetadata(toUpdate, original, mode);
			if (toUpdate.serverTap == null) {
				toUpdate.serverTap = getNewServerTapInstance(toUpdate);
				toUpdate.serverTap.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
			}
		}
	}
	
	//we wont deepcopy the data and metadata or the infopanel. we will only duplicate the main form using the metadata
	public void copyMetadata(TapClient copy, TapClient original, TapClientMode mode) {
		// copy most metadata
		copy.tapLabel = original.tapLabel;
		copy.tapBaseUrl = original.tapBaseUrl;
		copy.capabilities = original.capabilities;
		copy.setData(original.tablesMetaData);
		copy.queryCheckerTables = original.queryCheckerTables;
		copy.obscoreTables = original.obscoreTables;
		copy.infoPanel = original.infoPanel;
		if (mode == TapClientMode.TREEPANEL) {
			copy.primaryColor = Aladin.COLOR_FOREGROUND;
			copy.secondColor = Color.white;
		}
	}
	
	
	/**
	 * Method creates copy of a serverglu from its record
	 * @param originalGluRecord
	 * @param gluRecord
	 * @return
	 */
	public ServerGlu getCopy(StringBuffer originalGluRecord, String gluRecord) {
		aladin.glu.vGluServer = new Vector(50);
		ByteArrayInputStream dicStream = new ByteArrayInputStream(gluRecord.getBytes(StandardCharsets.UTF_8));
		aladin.glu.loadGluDic(new DataInputStream(dicStream),true,false);
		Vector serverVector = aladin.glu.vGluServer;
		int n = serverVector.size();
        if( n == 0 ) return null;
        ServerGlu serverGlu = (ServerGlu) serverVector.get(0);
        serverGlu.record = originalGluRecord;
        return serverGlu;
	}
	
	/**
	 * Thread to create tap form
	 * @param newServer
	 * @param mode
	 */
	public void createGenericTapFormFromMetaData(final DynamicTapForm newServer) {
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TloadServerTapForm: "+newServer.getName());
						newServer.createFormDefault(); //default choice is the first table
						newServer.revalidate();
						newServer.repaint();
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			// TODO: handle exception
			newServer.showLoadingError();
			newServer.revalidate();
			newServer.repaint();
			displayWarning(newServer.tapClient, "Unable to get metadata for "+newServer.getName()+"\n Request overload! Please wait and try again.");
		}
	}
	
	public boolean checkDummyInitForServerDialog(Server tapServer) {
		boolean result = true;
		if (aladin.glu.lastTapGluServer == null && tapServer instanceof ServerTap
				&& ((ServerTap) tapServer).isNotLoaded()) {
			try {
				this.showTapRegistryForm();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(aladin.dialog, GENERICERROR);
				e.printStackTrace();
			}
			result = false;
		}
		return result;
	}
	
	/**
	 * Method checks if tap server is on no-loaded state 
	 * i.e: no server was loaded from registry(error or not) or drag-drop
	 * @param server
	 * @return
	 */
	public boolean checkDummyTapServer(Server server) {
		boolean result = false;
		//check if not at all loaded
		//check if selected label is 
		if (server instanceof ServerTap && ((ServerTap)server).isNotLoaded()) {
			result = true;
		}
		return result;
	}
	
	public Plan getPlan(String aladinFileName) {
		// TODO Auto-generated method stub
		Plan resultantPlan = null;
		Plan[] plan = this.aladin.calque.plan;
		for (int i = 0; i < plan.length; i++) {
			if (plan[i].label!=null && plan[i].label.equalsIgnoreCase(aladinFileName)) {
				resultantPlan = plan[i];
				break;
			}
		}
		return resultantPlan;
	}
	
	/**
	 * Method gets tap capabilities
	 * @param tapServiceUrl
	 * @return
	 */
	public Future<VOSICapabilitiesReader> getTapCapabilities(final String tapServiceUrl) {
		Future<VOSICapabilitiesReader> capabilities = null;
		try {
			capabilities = executor.submit(new Callable<VOSICapabilitiesReader>() {
				@Override
				public VOSICapabilitiesReader call() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					VOSICapabilitiesReader vosiCapabilitiesReader = null;
					currentT.setName("TgetCapabilities: "+tapServiceUrl);
					try {
						URL capabilitiesUrl = getUrl(tapServiceUrl, null, GETTAPCAPABILITIES);//new URL(tapServiceUrl+GETTAPCAPABILITIES);
						vosiCapabilitiesReader = new VOSICapabilitiesReader();
						vosiCapabilitiesReader.load(capabilitiesUrl);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.err.println("Unable to get capabilitites for.."+tapServiceUrl);
						if (Aladin.levelTrace >= 3) {//Upload won't be allowed. thats all. no warnings
							e.printStackTrace();
						}
					} finally {
						currentT.setName(oldTName);
					}
					return vosiCapabilitiesReader;
				}
			});
		} catch (RejectedExecutionException e) {
			// TODO: handle exception
			Aladin.trace(3, "RejectedExecutionException. Unable to get capabilitites for.."+tapServiceUrl);
			//Nothing doing
		}
		return capabilities;
	}
	
	public DaliExamplesReader getTapExamples(String tapServiceUrl) {
		DaliExamplesReader daliExamplesReader = new DaliExamplesReader();
		try {//tintin
			URL examplesUrl = getUrl(tapServiceUrl, null, GETTAPEXAMPLES);//new URL(tapServiceUrl+GETTAPEXAMPLES);
//			URL examplesUrl = new URL("http://gaia.ari.uni-heidelberg.de/tap/examples");
//			URL examplesUrl = new URL("http://130.79.129.54:8080/simbadExamples.xhtml");
//			URL examplesUrl = new URL("http://130.79.129.54:8080/view-source_gaia.ari.uni-heidelberg.de_tap_examples.xhtml");
			URLConnection urlConnection = examplesUrl.openConnection();
	        urlConnection.setConnectTimeout(getRequest_timeout);
	        urlConnection.setReadTimeout(getRequest_timeout);
			daliExamplesReader.parseStream(urlConnection.getInputStream());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return daliExamplesReader;
	}
	
	/**
	 * Method to populate tap meta data
	 * @param tableName 
	 * @param newServer 
	 * @param tapServiceUrl
	 */
	public void loadTapColumnSchemasForATable(final TapClient clientToLoad, final String tableName, final DynamicTapForm newServer) {
		try {
			executor.execute(new Runnable(){
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					String tapServiceUrl = clientToLoad.tapBaseUrl;
					currentT.setName("TgetMetaInfoForATable: "+tapServiceUrl);
					Aladin.trace(3, "Getting meta from tree for table ..."+clientToLoad.tapLabel);
					try {
						clientToLoad.nodeName = tableName;
						clientToLoad.setData(new HashMap<String,TapTable>());
						
						//download only table names and first table's columns
						String tableNameQueryParam = URLEncoder.encode(tableName, UTF8);
						
						String gettablesQuery = String.format(GETTAPSCHEMATABLE, tableNameQueryParam);
						SavotResource resultsResource = getResults(tapServiceUrl, gettablesQuery, PATHSYNC);
						populateTables(clientToLoad, resultsResource);
						
						//get single table data and populate it to front end
						String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, tableNameQueryParam);
						SavotResource columnResults = getResults(tapServiceUrl, gettablesColumnsQuery, PATHSYNC);
						populateColumns(clientToLoad, columnResults);
						clientToLoad.preprocessTapClient();
						newServer.createFormDefault(); //default choice is the first table
						newServer.revalidate();
						newServer.repaint();
						clientToLoad.infoPanel = createMetaInfoDisplay((tapServiceUrl+gettablesQuery), clientToLoad.tablesMetaData);
					} catch (Exception e) {
						if (Aladin.levelTrace >= 3) {
							e.printStackTrace();
							System.err.println("Error in getting meta data for node/table..."+clientToLoad.tapLabel);
							System.out.println("Now will try the default get all tables of the tap server...");
						}
						loadTapColumnSchemas(clientToLoad, newServer);
					} finally {
						newServer.revalidate();
						newServer.repaint();
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			// TODO: handle exception
			newServer.showLoadingError();
			newServer.revalidate();
			newServer.repaint();
			displayWarning(clientToLoad, "Unable to get metadata for "+clientToLoad.tapLabel+"\n Request overload! Please wait and try again.");
		}
	}
	
	/**
	 * Method to populate tap meta data
	 * @param newServer 
	 * @param tapServiceUrl
	 */
	public void loadTapColumnSchemas(final TapClient clientToLoad, final DynamicTapForm newServer){
		try {
			executor.execute(new Runnable(){
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					String tapServiceUrl = clientToLoad.tapBaseUrl;
					currentT.setName("TgetMetaInfo: "+tapServiceUrl);
					try {
						int count = MAXTAPCOLUMNDOWNLOADVOLUME;
						try {
							String volume = getFutureResultsVolume(tapServiceUrl);
							if (volume != null) {
								count = Integer.parseInt(volume);
							}
						} catch (Exception e) {
							// TODO: handle exception
							if (Aladin.levelTrace >= 3) 
								e.printStackTrace();
							Aladin.trace(3, "Murky waters..do not know count. will get data table-wise...");
						}
						clientToLoad.setData(new HashMap<String,TapTable>());
						if (count >= MAXTAPCOLUMNDOWNLOADVOLUME) {
							//download only table names and first table's columns
							SavotResource resultsResource = getResults(tapServiceUrl,GETTAPSCHEMATABLES, PATHSYNC);
							populateTables(clientToLoad, resultsResource);
							String defaultTable = clientToLoad.tablesMetaData.keySet().iterator().next();
							String tableNameQueryParam = defaultTable;
							tableNameQueryParam = URLEncoder.encode(tableNameQueryParam, UTF8);
							
							//get single table data and populate it to front end
							String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, tableNameQueryParam);
							SavotResource columnResults = getResults(tapServiceUrl, gettablesColumnsQuery, PATHSYNC);
							populateColumns(clientToLoad, columnResults);
							clientToLoad.infoPanel = createMetaInfoDisplay(tapServiceUrl, clientToLoad.tablesMetaData);
						} else if (count > 0) {
							// download all
							SavotResource resultsResource = getResults(tapServiceUrl, GETTAPSCHEMACOLUMNS, PATHSYNC);
							populateColumns(clientToLoad, resultsResource);
							//update table meta anyway and then create the info panel
							updateTableMetadata(clientToLoad, tapServiceUrl);
						} else {
							newServer.showLoadingError();
							displayWarning(clientToLoad, "Error from tap server "+clientToLoad.tapLabel+" : unable to get metadata !");
							return;
						}
						clientToLoad.preprocessTapClient();
						newServer.createFormDefault(); //default choice is the first table
					} catch (Exception e) {
						e.printStackTrace();
						newServer.showLoadingError();
						displayWarning(clientToLoad, "Error from tap server "+clientToLoad.tapLabel+" : unable to get metadata !");
					} finally {
						newServer.revalidate();
						newServer.repaint();
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			// TODO: handle exception
			newServer.showLoadingError();
			newServer.revalidate();
			newServer.repaint();
			displayWarning(clientToLoad, "Unable to get metadata for "+clientToLoad.tapLabel+"\n Request overload! Please wait and try again.");
		}
	}
	
	/**
	 * Creates the form to load the tap servers from the registry file
	 * @throws Exception 
	 */
	public void showTapRegistryForm(){
		/*String tapGluRecord = ServerTap.getForm(TAP_MAIN_FORM);
		if (tapGluRecord != null) {
			ByteArrayInputStream dicStream = new ByteArrayInputStream(tapGluRecord.getBytes(StandardCharsets.UTF_8));
			aladin.glu.loadGluDic(new DataInputStream(dicStream), false, false);
			aladin.glu.reload(false, true);
		}*/

		loadTapServerList();
		tapFrameServer.tabbedTapThings.setSelectedIndex(0);
		// tapFrameServer = new TapFrameServer(aladin,this);
		tapFrameServer.setVisible(true);
		tapFrameServer.toFront();
	}
	
	/**
	 * Method shows the async panel
	 * @throws Exception 
	 */
	public void showAsyncPanel(){
		loadTapServerList();
		tapFrameServer.tabbedTapThings.setSelectedIndex(1);
		tapFrameServer.setVisible(true);
		tapFrameServer.toFront();
	}
	
	public void loadTapServerList() {
		if (tapFrameServer == null) {
			tapFrameServer = new TapFrameServer(aladin, this);
		}
	}
	
	public void reloadTapServerList() {
		if (tapFrameServer != null) {
			tapFrameServer.createRegistryPanel();
			tapFrameServer.reloadRegistryPanel();
		}
	}
	
	/**
	 * Method updates the tap metadata and calls for a metadata gui update.
	 * @param tableName
	 * @param tapClient
	 * @param tableNames 
	 * @return
	 * @throws Exception
	 */
	public void updateTableColumnSchemas(TapClient tapClient, List<String> tableNames) throws Exception {
		if (tableNames == null || tableNames.isEmpty()) {
			throw new Exception("Error no table name provided !\n");
		}
		try {
			String tapServiceUrl = tapClient.tapBaseUrl;
			
			boolean isNotFirst = false;
			StringBuffer whereCondition = new StringBuffer();
			String gettablesColumnsQuery = null;
			if (tableNames.size() == 1) {
				gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, URLEncoder.encode(tableNames.get(0), UTF8));
			} else {
				for (String tableName : tableNames) {
					if (isNotFirst) {
						whereCondition.append("+OR+table_name+=+");
					} else {
						whereCondition.append("+table_name+=+");
					}
					whereCondition.append(Util.formatterPourRequete(true, URLEncoder.encode(tableName, UTF8)));
					isNotFirst = true;
				}
				gettablesColumnsQuery = "REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+where+"+whereCondition.toString();
			}
			
//			String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, whereCondition.toString());
			SavotResource columnResults = getResults(tapClient.tapBaseUrl, gettablesColumnsQuery, PATHSYNC);
			populateColumns(tapClient, columnResults);
			
			 //update info panel
			Future<JPanel> infoPanel = this.createMetaInfoDisplay(tapServiceUrl, tapClient.tablesMetaData);
			if (infoPanel != null) {
				tapClient.tackleFrameInfoServerUpdate(aladin, infoPanel);
			}
			Aladin.trace(3,"done updating tap info for : "+tableNames.toString()+"| server is : "+tapClient.tapBaseUrl);
			
		} catch (RejectedExecutionException e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw new Exception("Request overload! Please wait and try again.");
		} catch (Exception e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw e;
		}
	}
	
	public static URL getUrl(String baseUrlStr, String query, String subPath) throws URISyntaxException, MalformedURLException {
		URL genUrl = null;
//		URL baseurl = new URL(baseUrlStr);
//		String protocol = baseurl.getProtocol();
//		String host = baseurl.getHost();
//		int port = baseurl.getPort();
		String path = baseUrlStr;
		if (subPath != null) {
			if (!path.endsWith("/")) {
				path = path+"/";
			} 
			path = path+subPath;
		}
		
		if (query != null) {
			path = path+"?"+query;
		}
		URI uri = new URI(path);
		genUrl = uri.toURL();
		
//		uri = new URI(uri+"?"+query);
		genUrl = uri.toURL();
		return genUrl;
	}
	
	/**
	 * Makes small notification flags to user. The initial text set is erased after a small time delay.
	 * Useful to flag user when any gui model updates are made.
	 * @param notificationBar
	 * @param message
	 */
	public void eraseNotification(final JLabel notificationBar, final String resetText) {
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TeraseNotification: ");
						currentT.setPriority(Thread.MIN_PRIORITY);
						TimeUnit.SECONDS.sleep(3);
						notificationBar.setText(resetText);
						notificationBar.revalidate();
						notificationBar.repaint();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						if (Aladin.levelTrace >= 3)
							e.printStackTrace();
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			Aladin.trace(3, "RejectedExecutionException");
			notificationBar.setText(resetText);
		}
	}
	
	/**
	 * Method populates loads all tables from TAP_SCHEMA.tables
	 * @param newServer 
	 * @param resultsResource
	 * @throws Exception 
	 */
	public void populateTables(TapClient tapClient, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableReader(tapClient, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
				case 1:
					binaryTableReader(tapClient, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
					break;
				default:
					if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateTables! Did not read table data for "+tapClient.tapBaseUrl);
					throw new Exception("ERROR in populateTables! Did not read table data"+tapClient.tapBaseUrl);
				}
			}
		}
	}
	
	/**
	 * Tap Table Reader for Savot table Resource
	 * @param newServer
	 * @param tableRows
	 * @param fieldSet
	 * @throws Exception 
	 */
	protected void tableReader(TapClient tapClient, TRSet tableRows, FieldSet fieldSet) throws Exception {
		TapTable table = null;
		if (fieldSet != null && tableRows != null && tableRows.getItemCount() > 0) {
			for (int j = 0; j < tableRows.getItemCount(); j++) {
				table = new TapTable();
				TDSet theTDs = tableRows.getTDSet(j);
				String tableName = null;
				for (int k = 0; k < theTDs.getItemCount(); k++) {
					SavotField field = fieldSet.getItemAt(k);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLETYPE)) {
							table.setTable_type(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(SCHEMANAME)) {
							table.setSchema_name(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName = theTDs.getContent(k);
							table.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							table.setUtype(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							table.setDescription(theTDs.getContent(k));
						}
					}
				}
				if (tableName != null) {
					synchronized (tapClient.tablesMetaData) {
						if (tapClient.tablesMetaData.containsKey(tableName)) {
							//conserve columns if already set. but other details are updated
							TapTable oldEntry = tapClient.tablesMetaData.get(tableName);
							if (oldEntry.getColumns() != null) {
								table.setColumns(oldEntry.getColumns());
							}
							
							if (oldEntry.getFlaggedColumns() != null) {
								table.setFlaggedColumns(oldEntry.getFlaggedColumns());
							}
							
							if (oldEntry.getObsCoreColumns() != null) {
								table.setObsCoreColumns(oldEntry.getObsCoreColumns());
							}
						}
						tapClient.tablesMetaData.put(tableName, table);
					}
				}
			}
		} else {
			if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateTables! Did not read table data for "+tapClient.tapBaseUrl);
			throw new Exception("ERROR in populateTables! Did not read table data"+tapClient.tapBaseUrl);
		}
	}
	
	/**
	 * Tap Table Reader for Savot binary resource
	 * @param newServer
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
	protected void binaryTableReader(TapClient tapClient, SavotBinary binaryData, FieldSet fields) throws IOException {
		DataBinaryReader parser = null;
		try {
			parser = new DataBinaryReader(binaryData.getStream(), fields);
			TapTable table = null;
			while (parser.next()) {
				table = new TapTable();
				String tableName = null;
				for (int j = 0; j < fields.getItemCount(); j++) {
					table.setTable_type(parser.getCellAsString(j));
					SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLETYPE)) {
							table.setTable_type(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(SCHEMANAME)) {
							table.setSchema_name(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName = parser.getCellAsString(j);
							table.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							table.setUtype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							table.setDescription(parser.getCellAsString(j));
						}
					}
				}
				if (tableName != null) {
					synchronized (tapClient.tablesMetaData) {
						if (tapClient.tablesMetaData.containsKey(tableName)) {
							//conserve columns if already set. but other details are updated
							TapTable oldEntry = tapClient.tablesMetaData.get(tableName);
							if (oldEntry.getColumns() != null) {
								table.setColumns(oldEntry.getColumns());
							}
							
							if (oldEntry.getFlaggedColumns() != null) {
								table.setFlaggedColumns(oldEntry.getFlaggedColumns());
							}
							
							if (oldEntry.getObsCoreColumns() != null) {
								table.setObsCoreColumns(oldEntry.getObsCoreColumns());
							}
						} 
						tapClient.tablesMetaData.put(tableName, table);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("ERROR in binaryTableReader! Did not read table data for " + tapClient.tapBaseUrl);
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			throw e;
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
	}
	
	/**
	 * Method tries to categorize savot resultsResource as either in binary or table format
	 * @param tableIndex
	 * @param resultsResource
	 * @return 1 for binary, 0 for votable
	 */
	protected int getType(int tableIndex, SavotResource resultsResource) {
		int type = -1;
		if (resultsResource.getTables() == null || resultsResource.getTables().getItemAt(tableIndex) == null
				|| resultsResource.getTables().getItemAt(tableIndex).getData() == null
				|| (resultsResource.getTables().getItemAt(tableIndex).getData().getTableData() == null
						&& resultsResource.getTables().getItemAt(tableIndex).getData().getBinary() != null)) {
			type = 1; // for binary
		} else if (resultsResource.getTables().getItemAt(tableIndex).getData().getTableData().getTRs() != null) {
			type = 0; // for table
		}
		return type;
	}

	/**TODO::tintin think about this binary deal
	 * Method populates all column metainfo TAP_SCHEMA.columns from savot resource 
	 * @param serverToLoad
	 * @param resultsResource
	 * @throws Exception 
	 */
	public void populateColumns(TapClient tapClient, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableColumnReader(tapClient, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
				case 1:
					binaryColumnReader(tapClient, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
					break;
				default:
					if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
					throw new Exception("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
				}
			}
		}
	}
	
	/**
	 * Method populates column information from savot resource 
	 * @param serverToLoad
	 * @param tableRows
	 * @param fieldSet
	 * @throws Exception 
	 */
	protected void tableColumnReader(TapClient tapClient, TRSet tableRows, FieldSet fieldSet) throws Exception {
		TapTableColumn tableColumn = null;
		if (fieldSet != null && tableRows != null && tableRows.getItemCount() > 0) {
			for (int j = 0; j < tableRows.getItemCount(); j++) {
				tableColumn = new TapTableColumn();
				TDSet theTDs = tableRows.getTDSet(j);
				for (int k = 0; k < theTDs.getItemCount(); k++) {
					SavotField field = fieldSet.getItemAt(k);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableColumn.setTable_name(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(COLUMNNAME)) {
							tableColumn.setColumn_name(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							tableColumn.setDescription(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(UNIT)) {
							tableColumn.setUnit(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(UCD)) {
							tableColumn.setUcd(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							tableColumn.setUtype(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(DATATYPE)) {
							tableColumn.setDatatype(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(SIZE)) {
							tableColumn.setSize(field.getDataType(), theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(PRINCIPAL)) {
							tableColumn.setIsPrincipal(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(INDEXED)) {
							tableColumn.setIsIndexed(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(STD)) {
							tableColumn.setIsStandard( theTDs.getContent(k));
						}
					}
				}
				setTableIntoTapMetaData(tapClient, tableColumn);
			}
			obscorePostProcess(tapClient);
		} else {
			if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
			throw new Exception("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
		}
	}
	
	/**
	 * Current logic to identify obscore: in name as well as having some of the obscore columns
	 * Changes after we clarify from spec
	 * @param tapClient
	 */
	public synchronized void obscorePostProcess(TapClient tapClient) {
		for (TapTable table : tapClient.tablesMetaData.values()) {
			if (table.hasObscoreInTheName()) {
				if (!tapClient.obscoreTables.containsValue(table) && table.obsCoreColumns != null
						&& table.obsCoreColumns.size() > TapTable.MAXOBSCORECOLSCOUNTED) {// magic number. not counting upto 17
					tapClient.obscoreTables.put(table.getTable_name(), table);
				}
			}
		}
		/*synchronized (tapClient.obscoreTables) {
			if (table.hasObscoreInTheName()) {
				if (!tapClient.obscoreTables.containsValue(table)) {
					tapClient.updateObscoreGui(tableName, table);
				}
			}
		}*/
	}
	
	public void setTableIntoTapMetaData(TapClient tapClient, TapTableColumn tableColumn) {
		Vector<TapTableColumn> tableColumns = null;
		TapTable table = null;
		String tableName = tableColumn.getTable_name();
		if (tableName != null) {
			synchronized (tapClient.tablesMetaData) {
				if (tapClient.tablesMetaData.containsKey(tableName)) {
					table = tapClient.tablesMetaData.get(tableColumn.getTable_name());
					tableColumns = table.getColumns();
					if (tableColumns == null) {
						tableColumns = new Vector<TapTableColumn>();
						table.setColumns(tableColumns);
					}
				} else {
					tableColumns = new Vector<TapTableColumn>();
					table = new TapTable();
					table.setTable_name(tableName);
					table.setColumns(tableColumns);
					tapClient.tablesMetaData.put(tableName, table);
				}
				table.parseUcds(tableColumn);
				table.parseForObscore(false, tableColumn);
				tableColumns.add(tableColumn);
			}
		}
	}
	
	/**
	 * Method populates column information from savot binary resource 
	 * @param serverToLoad
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
	protected void binaryColumnReader(TapClient tapClient, SavotBinary binaryData, FieldSet fields) throws IOException {
		TapTableColumn tableColumn = null; 
		DataBinaryReader parser = null;
		try {
			parser = new DataBinaryReader(binaryData.getStream(), fields);
			while (parser.next()) {
				tableColumn = new TapTableColumn();
                for (int j = 0; j < fields.getItemCount(); j++) {
                    SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableColumn.setTable_name(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(COLUMNNAME)) {
							tableColumn.setColumn_name(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							tableColumn.setDescription(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(UNIT)) {
							tableColumn.setUnit(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(UCD)) {
							tableColumn.setUcd(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							tableColumn.setUtype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(DATATYPE)) {
							tableColumn.setDatatype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(SIZE)) {
							tableColumn.setSize(field.getDataType(), parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(PRINCIPAL)) {
							tableColumn.setIsPrincipal(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(INDEXED)) {
							tableColumn.setIsIndexed(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(STD)) {
							tableColumn.setIsStandard(parser.getCellAsString(j));
						}
					}
                }
                setTableIntoTapMetaData(tapClient, tableColumn);
            }
			obscorePostProcess(tapClient);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			System.err.println("ERROR in binaryColumnReader! Did not read column column data for "+tapClient.tapBaseUrl);
			throw e;
		} finally {
			if (parser!=null) {
				parser.close();
			}
		}
	}
	
	/**
	 * Method populates all column metainfo from one of Aladin's loaded plan
	 * @param planToUpload
	 * @param tableName
	 * @param tablesMetaData
	 */
	public static void populateColumnsFromPlan(Plan planToUpload, String tableName, Map<String, TapTable> tablesMetaData) {
		Vector<TapTableColumn> tableColumns = new Vector<TapTableColumn>();
		TapTable table = null; 
		TapTableColumn tableColumn = null; 
		
		Enumeration e = planToUpload.pcat.vField.elements();
		String raName = EMPTYSTRING;
		String decName = EMPTYSTRING;
		
		table = new TapTable();
		table.setTable_name(tableName);
		
		while(e.hasMoreElements()) {
			Field f = (Field) e.nextElement();
			tableColumn = new TapTableColumn();
			tableColumn.setTable_name(tableName);
			
			tableColumn.setColumn_name(f.name);
			tableColumn.setDescription(f.description);
//			tableColumn.setDatatype(f.datatype);
			tableColumn.setUcd(f.ucd);
			tableColumn.setUnit(f.unit);
			tableColumn.setUtype(f.utype);
//			tableColumn.setSize(f.columnSize); nope
			table.parseUcds(tableColumn);
			table.parseForObscore(true, tableColumn);
			tableColumns.add(tableColumn);
			if (f.isRa()) {//bug with upload where it assumes ra and dec. maximum damage here is the appearance of target panel with wrong col identified as ra and dec. all user has todo is to ignore the target panel
				if(Aladin.levelTrace >= 3)System.err.println("f.isRa()"+f.isRa()+" , and its name is: "+f.name);
				raName = f.name;
			}
			if (f.isDe()) {
				if(Aladin.levelTrace >= 3)System.err.println("f.isDe()"+f.isDe()+" , and its name is: "+f.name);
				decName = f.name;
			}
			
		}
		table.setColumns(tableColumns);
//		table.setRaColumnName(((Field) plancatalog.pcat.vField.get(plancatalog.pcat.leg.getRa())).name);
//		table.setDecColumnName(((Field) plancatalog.pcat.vField.get(plancatalog.pcat.leg.getDe())).name);
		tablesMetaData.put(tableName, table);
	}

	/**
	 * Method to create parser to access the web info
	 * @param tapServiceUrl
	 * @return
	 * @throws MalformedURLException, Exception 
	 */
	public static SavotResource getResults(String tapServiceUrl, String file, String path) throws MalformedURLException, Exception {
		SavotResource resultsResource = null;
		try {
			URL url = getUrl(tapServiceUrl, file, path);
			Aladin.trace(3, "TapManager.getResults() for: "+url);
			SavotPullParser savotParser = new SavotPullParser(url, SavotPullEngine.FULL, null, false);
			resultsResource = Util.populateResultsResource(savotParser);
		} catch (MalformedURLException e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw e;
		} catch (Exception e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw e;
		}

		return resultsResource;
	}
	
	/**
	 * Method uploads table from PlanCatalog instance
	 * @param planToLoad
	 * @param uploadTableName
	 */
	public void createTapServerFromAladinPlan(final Plan planToLoad, final String uploadTableName) {
		executor.execute(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				boolean firstUpload = false;
				String tableName = uploadTableName;
				if (uploadTableName == null || uploadTableName.isEmpty()) {
					tableName = uploadFrame.generateUploadTableName(ALADINTABLEPREFIX);
				}
				if (uploadFrame.uploadClient.tablesMetaData.isEmpty()) {
					firstUpload = true;
					uploadFrame.uploadClient.serverTap.showloading();
				}
//				uploadServer.tablesGui.invalidate();
				populateColumnsFromPlan(planToLoad, tableName , uploadFrame.uploadClient.tablesMetaData);
//				uploadFrame.setUploadFileForm();
				if (firstUpload || (uploadFrame.uploadClient.serverTap.formLoadStatus == TAPFORM_STATUS_NOTLOADED
						|| uploadFrame.uploadClient.serverTap.formLoadStatus == TAPFORM_STATUS_ERROR)) {
					uploadFrame.createUploadServer();
				}
				
				if (!firstUpload && uploadFrame.uploadClient.serverTap.formLoadStatus == TAPFORM_STATUS_LOADED) {
//					uploadFrame.uploadServer.createFormDefault();
//					uploadFrame.uploadServer.revalidate();
					uploadFrame.uploadClient.serverTap.tablesGui.addItem(tableName);
					uploadFrame.uploadClient.serverTap.updateQueryChecker(tableName);
					uploadFrame.uploadClient.serverTap.tablesGui.setSelectedItem(tableName);
				} 
				refreshTapSettings(uploadTableName, 1, uploadFrame.uploadClient.tablesMetaData);
				uploadFrame.pack();
			}
		});
	}
	
	/**
	 * Populates a single param value expected to be count. 
	 * @param tapServiceUrl
	 * @return count value
	 * @throws Exception
	 */
	public String getFutureResultsVolume(String tapServiceUrl) throws Exception {
		String count = null;
		SavotResource resultsResource = getResults(tapServiceUrl, GETTAPSCHEMACOLUMNCOUNT, PATHSYNC);
		if (resultsResource != null) {
			for (int i = 0; i < 1; i++) {
				int type = getType(0, resultsResource);
				int index = 0;
				switch (type) {
				case 0:
					count = getTableRowParam(index, resultsResource.getTRSet(index), resultsResource.getFieldSet(index), COUNT);
					break;
				case 1:
					count = getBinarySingleParam(index , resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(index), COUNT);
					break;

				default:
					if( Aladin.levelTrace >= 3 ) System.err.println("ERROR in getFutureResultsVolume()! Did not read count. url:"+tapServiceUrl);
					break;
				}
			}
		}
		return count;
		// count == 0
		// try
		// URL url = new URL(tapServiceUrl + "/tables");
		// if count ==0 - show error
		//
		// if count>maxRows? cadc shows 410. Vizier is 469587
		// just
	}
	
	/**
	 * Gets the value of the single param expected in the votable
	 * @param index
	 * @param tableRows
	 * @param fieldSet
	 * @param paramName
	 * @return
	 */
	protected String getTableRowParam(int index, TRSet tableRows, FieldSet fieldSet, String paramName) {
		String count = String.valueOf(MAXTAPCOLUMNDOWNLOADVOLUME);//set to max. If unable to read the column number, we won't download all the metadata
		TDSet theTDs = tableRows.getTDSet(index);
		SavotField field = fieldSet.getItemAt(index);
		String fieldName = field.getName();
		if (fieldName != null && !fieldName.isEmpty()
				/*&& fieldSet.getItemAt(index).getName().contains(paramName)*/) {
			count = theTDs.getContent(index);
		}
		return count;
	}
	
	/**
	 * Gets the value of the single param expected in the votable
	 * @param index
	 * @param tableRows
	 * @param fieldSet
	 * @param paramName - not in use. see comment below //checking COUNT... 
	 * @return
	 */
	protected String getBinarySingleParam(int index, SavotBinary binaryData, FieldSet fieldSet, String paramName) {
		String count = String.valueOf(MAXTAPCOLUMNDOWNLOADVOLUME);//set to max. If we can't read the column number, we won't download all the metadata
		DataBinaryReader parser = null;
		try {
			parser = new DataBinaryReader(binaryData.getStream(), fieldSet);
			while (parser.next()) {
				SavotField field = fieldSet.getItemAt(index);
				String fieldName = field.getName().toUpperCase();
				if (fieldName != null && !fieldName.isEmpty()) {//checking COUNT fieldname is no use, servers send the param in many diff names
					count = parser.getCellAsString(index);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3) System.err.println("IOError.. Count not known");
			if (Aladin.levelTrace >= 3)	e.printStackTrace();
		}
		
		return count;
	}
	
	/**
	 * Method to lazily gets table metadata.
	 * @param clientToLoad
	 * @param tapServiceUrl
	 * @param tablesMetaData
	 */
	public void updateTableMetadata(final TapClient clientToLoad, final String tapServiceUrl) {
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TupdateTableMetadata: "+tapServiceUrl);
					currentT.setPriority(Thread.NORM_PRIORITY-2);
					try {
						SavotResource resultsResource = getResults(tapServiceUrl, GETTAPSCHEMATABLES, PATHSYNC);
						populateTables(clientToLoad, resultsResource);
						//update info panel
						Future<JPanel> infoPanel = createMetaInfoDisplay(tapServiceUrl, clientToLoad.tablesMetaData);
						if (infoPanel != null) {
							clientToLoad.tackleFrameInfoServerUpdate(aladin, infoPanel);
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						if (Aladin.levelTrace >=3 ) {//we do not bother here if all table description is not obtained. 
							//if there is problem obtaining essential metadata then there will be error actions in the main loadTapColumnSchemas thread
							e.printStackTrace();
						}
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException e) {
			System.err.println("RejectedExecutionException. Unable to update table metadata.."+tapServiceUrl);
			//Nothing doing
		}
	}
	
	/**
	 * Method populates the info panel gui of the server
	 * @param tapServiceUrl
	 * @param tablesMetaData
	 * @return
	 */
	public synchronized Future<JPanel> createMetaInfoDisplay(final String tapServiceUrl, final Map<String, TapTable> tablesMetaData){
		Future<JPanel> result = null;
 		try {
 			result = executor.submit(new Callable<JPanel>() {
 				@Override
 				public JPanel call() throws MalformedURLException {
 					final Thread currentT = Thread.currentThread();
 					final String oldTName = currentT.getName();
 					currentT.setName("TcreateInfoFrame: "+tapServiceUrl);
 					currentT.setPriority(Thread.NORM_PRIORITY-2);
 					GridBagLayout gridbag = new GridBagLayout();
 					GridBagConstraints bagConstraints = null;
 					
 					JPanel infoPanel = new JPanel();
 					infoPanel.setLayout(gridbag);
 					infoPanel.setFont(Aladin.PLAIN);
 					
 					JLabel tableNameLabel = null;
 					TwoColorJTable infoTable = null;
 					
 					JScrollPane scrollPane = null;
 					Vector<String> columnNames = TapTableColumn.getColumnLabels();
 					Vector<Vector<String>> allRows = null;
 					
 					int width = 500;
 					int height = ServerTap.DEFAULT_INFO_TABLE_HEIGHT;
 					
 					bagConstraints = new GridBagConstraints();
 					bagConstraints.gridx = 0;
 					bagConstraints.gridy = 0;
 					bagConstraints.gridwidth = 1;
 					bagConstraints.weightx = 1;
 					bagConstraints.weighty = 0.001;
 					bagConstraints.fill = GridBagConstraints.NONE;
 					bagConstraints.anchor = GridBagConstraints.WEST;
 					bagConstraints.insets = new Insets(20, 2, 2, 2);
 					
 					JLabel displayString = new JLabel("Database Schema: ");
 					displayString.setFont(Aladin.BOLD);
 					gridbag.setConstraints(displayString, bagConstraints);
 					infoPanel.add(displayString);
 					
 					JLabel tableDescription = null;
 					StringBuilder recordInfoTables = new StringBuilder();
 					
 					for (String tableName : tablesMetaData.keySet()) {
 						String description = EMPTYSTRING;
 						Vector<TapTableColumn> columnMetadata = tablesMetaData.get(tableName).getColumns();
 						
 						TapTable table = tablesMetaData.get(tableName);
 						if (table != null && table.getDescription() != null && !table.getDescription().isEmpty()) {
 							description = table.getDescription();
 						}
 						
 						if (columnMetadata != null) {
 							tableNameLabel = new JLabel("Table: "+tableName);
 							tableNameLabel.setFont(Aladin.BOLD);
 							//tableName.setBounds(x, y, width, 10);y+=10;
 							bagConstraints.gridy++;
 							bagConstraints.insets = new Insets(20, 2, 2, 2);
 							infoPanel.add(tableNameLabel, bagConstraints);
 							if (!description.isEmpty()) {
 								tableDescription = new JLabel("<html><p width=\"1000\">Description:"+description);
 								bagConstraints.gridy++;
 								bagConstraints.insets = new Insets(2, 2, 2, 2);
 								infoPanel.add(tableDescription, bagConstraints);
 							}
 							
 							allRows = new Vector<Vector<String>>();
 							for (TapTableColumn tapTableColumn : columnMetadata) {
 								allRows.addElement(tapTableColumn.getRowVector());
 							}
 							infoTable = new TwoColorJTable(allRows, columnNames);
 							infoTable.setPreferredScrollableViewportSize(new Dimension(width, height));
 							scrollPane = new JScrollPane(infoTable);
 							// scrollPane.setBounds(x, y, width, height);y+=height;
 							scrollPane.getVerticalScrollBar().setUnitIncrement(4);
 							bagConstraints.gridy++;
 							bagConstraints.insets = new Insets(2, 2, 2, 2);
 							bagConstraints.fill = GridBagConstraints.BOTH;
 							bagConstraints.weighty = 0.05;
 							infoPanel.add(scrollPane, bagConstraints);
 						} else {
 							recordInfoTables.append("\n------------Table: "+tableName+"--------------");
 							if (!description.isEmpty()) {
 								recordInfoTables.append("\n").append(description);
 									
 							}
 							recordInfoTables.append("\nPlease select this table in the TAP server selector form to get column metadata.\n");
// 							noInfolabel = new JLabel("No column metadata available.");
// 							noInfolabel.setFont(Aladin.ITALIC);
// 							bagConstraints.gridy++;
// 							bagConstraints.insets = new Insets(2, 2, 2, 2);
// 							gridbag.setConstraints(noInfolabel, bagConstraints);
// 							infoPanel.add(noInfolabel);
 						}
 					}
 					if (recordInfoTables.length() > 0) {
 						JTextArea infoTablesDisplay = new JTextArea(20, 85);
 						infoTablesDisplay.setText(recordInfoTables.toString());
 						infoTablesDisplay.setFont(Aladin.COURIER);
 						infoTablesDisplay.setBackground(Color.white);
 						infoTablesDisplay.setEditable(false);
 						scrollPane = new JScrollPane(infoTablesDisplay);
 						bagConstraints.fill = GridBagConstraints.BOTH;
 						bagConstraints.weighty = 0.10;
 						bagConstraints.gridy++;
 						bagConstraints.insets = new Insets(2, 2, 2, 2);
 						gridbag.setConstraints(scrollPane, bagConstraints);
 						infoPanel.add(scrollPane);
 					}
// 					FrameInfoServer frameInfoServer = new FrameInfoServer(aladin, infoPanel);
 					currentT.setName(oldTName);
 					return infoPanel;
 				}
 			});
		} catch (RejectedExecutionException ex) {
			System.err.println("RejectedExecutionException. Unable to create the metainfo display for.."+tapServiceUrl);
			//Nothing doing
		}
 		return result;
	}
	
	/**
	 *	Method to handle sync tap query submissions:
	 * <ol>	<li>Load query</li>
	 * 		<li>Query tap server synchronously</li>
	 * 		<li>Populate results(votable) on Aladin</li>
	 * </ol>
	 * if postParams are null then it executes GET, else this method executes a POST
	 * @param serverExamples 
	 * @param url 
	 * @param string 
	 * @param parserObj
	 * @throws Exception 
	 */
	public void fireSync(final Server server, final String query, final ADQLQuery parserObj,
			final Map<String, Object> postParams) throws Exception {
		// TODO Auto-generated method stub
		final int requestNumber = server.newRequestCreation();
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						if (postParams == null) {
							String queryParam = GETRESULTPARAMS + URLEncoder.encode(query, UTF8);
							URL syncUrl = getUrl(server.tapClient.tapBaseUrl, queryParam, PATHSYNC);
//									new URL(String.format(SYNCGETRESULT, url, queryParam));
							currentT.setName("TsubmitSync: " + syncUrl);
							//TODO:: tintin check for the query status before loading: and remove return- covert to execute
							/*
							 * one INFO element with name=\94QUERY_STATUS\94 and value=\94OK\94 or
							value=\94ERROR\94 must be included before the TABLE. If the TABLE does not
							contain the entire query result, one INFO element with value=\94OVERFLOW\94 or
							value=\94ERROR\94 must be included after the table.*/
							
							URLConnection urlConn = syncUrl.openConnection();
							urlConn.setRequestProperty("Accept", "*/*");
							urlConn.setRequestProperty("Connection", "Keep-Alive");
							urlConn.setRequestProperty("Cache-Control", "no-cache");
							handleSyncGetResponse(aladin, urlConn, server.tapClient.tapLabel, requestNumber, server);
							
						} else {//currently this else part is used only for upload.
							URL syncUrl = getUrl(server.tapClient.tapBaseUrl, null, PATHSYNC);
							currentT.setName("TsubmitSync: " + syncUrl);
							MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
							String boundary = MultiPartPostOutputStream.createBoundary();

							URLConnection urlConn = MultiPartPostOutputStream.createConnection(syncUrl);
							urlConn.setRequestProperty("Accept", "*/*");
							urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
							// set some other request headers...
							urlConn.setRequestProperty("Connection", "Keep-Alive");
							urlConn.setRequestProperty("Cache-Control", "no-cache");
							MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

							// standard request parameters
							out.writeField("request", "doQuery");
							out.writeField("lang", "adql");
							out.writeField("version", "1.0");
							out.writeField("format", "votable");

							if (parserObj != null) {
								int limit = parserObj.getSelect().getLimit();
								if (limit > 0) {
									out.writeField("maxrec", String.valueOf(limit));
								}
							}
							
							out.writeField("query", query);

							if (postParams != null) {// this part only for upload as of now
								for (Entry<String, Object> postParam : postParams.entrySet()) {
									if (postParam.getValue() instanceof String) {
										out.writeField(postParam.getKey(), String.valueOf(postParam.getValue()));
									} else if (postParam.getValue() instanceof File) {
										out.writeFile(postParam.getKey(), "application/x-votable+xml", (File) postParam.getValue(), false);
									}
								}
							}
							out.close();
							handleSyncGetResponse(aladin, urlConn, server.tapClient.tapLabel, requestNumber, server);
						}

					} catch (MalformedURLException e) {
						if (Aladin.levelTrace >= 3) e.printStackTrace();
						displayWarning(server, requestNumber, e.getMessage());
					} catch (IOException e) {
						if( Aladin.levelTrace >= 3 ) e.printStackTrace();
						displayWarning(server, requestNumber, e.getMessage());
					} catch (Exception e) {
						if( Aladin.levelTrace >= 3 ) e.printStackTrace();
						displayWarning(server, requestNumber, e.getMessage());
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			displayWarning(server, requestNumber, "Unable to submit: "+query+"\n Request overload! Please wait and try again.");
		} 
	}
	
	/**
	 * Convenience method
	 * - creates plane for successful response
	 * - shows error otherwise
	 * @param conn
	 * @param requestNumber 
	 * @param planeName
	 * @throws Exception 
	 */
	public static void handleSyncGetResponse(Aladin aladin, URLConnection conn, String planeLabel, int requestNumber, Server server) throws Exception {
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			try {
				InputStream is;
				httpConn.connect();
				if (httpConn.getResponseCode() < 400) {
					// is = httpConn.getInputStream();
					if (requestNumber == -1 || server.requestsSent != requestNumber) {
						server = null;
					} else {
						server.disableStatusForAllPlanes();
					}
					aladin.calque.newPlanCatalog(httpConn, planeLabel, server, requestNumber);
					// stream is closed downstream when it is not an error scenario
				} else {
					is = httpConn.getErrorStream();
					TapQueryResponseStatusReader queryStatusReader = new TapQueryResponseStatusReader();
					queryStatusReader.load(is);
					is.close();
					String errorMessage = queryStatusReader.getQuery_status_message();
					if (errorMessage == null || errorMessage.isEmpty()) {
						errorMessage = "Error. unable to get response for this request.";
					} else {
						errorMessage = queryStatusReader.getQuery_status_value() + " " + errorMessage;
					}
					httpConn.disconnect();
					throw new IOException(errorMessage);
				}
			} catch (IOException e) {
				Aladin.trace(3, "Error when getting job! Http response is: " + httpConn.getResponseCode());
				if (httpConn != null) {
					httpConn.disconnect();
				}
				throw e;
			} /*
				 * finally { if (httpConn!=null) { httpConn.disconnect(); } }
				 */
		}
	}
	
	public String getSyncUrlUnEncoded(Server s, String url, String queryParam) {
		String result = null;
		try {
			result = getUrl(url, null, PATHSYNC).toString();
			if (queryParam != null) {
				result = result+"?"+queryParam;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
			Aladin.warning(s, e.getMessage());
		}
		return result;
	}
	
	/**
	 * Method to :
	 * <ol>	<li>Load query</li>
	 * 		<li>Query tap server asynchronously</li>
	 * 		<li>Populate results on Aladin</li>
	 * </ol>
	 * @param serverExamples 
	 * @param string 
	 * @param adqlParserObj
	 * @throws Exception 
	 */
	public void fireASync(final Server server, final String query, final ADQLQuery adqlParserObj,
			final Map<String, Object> postParams) throws Exception {
		final int requestNumber = server.newRequestCreation();
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TsubmitAsync: " + server.tapClient.tapBaseUrl);
					try {
						showAsyncPanel();
						uwsFacade.handleJob(server, query, adqlParserObj, postParams, requestNumber);
						currentT.setName(oldTName);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Aladin.warning(aladin.dialog, Aladin.getChaine().getString("GENERICERROR"));
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			displayWarning(server, requestNumber, "Unable to submit: "+query+"\n Request overload! Please wait and try again.");
		} 
	}
	
	
	public void setRaDecForTapServer(final ServerTap serverTap, final String selectedTableName) {
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TsetRaDec: "+serverTap.tapClient.tapBaseUrl);
					
					boolean addTargetPanel = false;
					Vector<TapTableColumn> columns = serverTap.tapClient.getServerTapSelectedTableColumns();
					JComboBox raColumn = new JComboBox(columns);
					raColumn.setRenderer(new CustomListCellRenderer());
					raColumn.setSize(raColumn.getWidth(), Server.HAUT);
					JComboBox decColumn = new JComboBox(columns);
					decColumn.setRenderer(new CustomListCellRenderer());
					decColumn.setSize(decColumn.getWidth(), Server.HAUT);
					
					Object[] raAndDec = {
					    "ra:", raColumn,
					    "dec:", decColumn
					};
					
					int option = JOptionPane.showConfirmDialog(aladin.dialog , raAndDec, "Set ra and dec", JOptionPane.OK_CANCEL_OPTION);
					if (option == JOptionPane.OK_OPTION) {
						if (serverTap.getRaColumnName() == null || serverTap.getDecColumnName() == null) {
							addTargetPanel = true;
						}
						serverTap.setRaColumnName(((TapTableColumn) ((JComboBox) raColumn).getSelectedItem()).getColumn_name());
						serverTap.setDecColumnName(((TapTableColumn) ((JComboBox) decColumn).getSelectedItem()).getColumn_name());
						if (addTargetPanel) {
//							setWhereAddConstraintsGui(columnNames);
//							this.queryComponentsGui.revalidate();
//							this.queryComponentsGui.repaint();
							if (serverTap.target == null) {
								serverTap.targetPanel = new JPanel();
								serverTap.createTargetPanel(serverTap.targetPanel);
							}
							serverTap.queryComponentsGui.revalidate();
							serverTap.queryComponentsGui.repaint();
						}
						serverTap.writeQuery();
					}
					currentT.setName(oldTName);
				}
			});
		} catch (RejectedExecutionException e) {
			displayWarning(serverTap.tapClient, "Request overload! Please wait and try again.");
		}
	}
	
	/**
	 * Gentle shut down of all tap threads
	 * plus async job clean up
	 */
	public void cleanUp() {
		this.executor.shutdown();
		this.uwsFacade.deleteAllSetToDeleteJobs();
		tapServerPanelCache.clear();
		tapServerTreeCache.clear();
		Aladin.trace(3,"soft shutdown of tap threads....");
		Aladin.trace(3,"deleting all(set to delete) uws jobs....");
	}
	
	/**
	 * Shuts down all lingering tap threads
	 */
	public void finalCleanUp() {
		this.executor.shutdownNow();
		Aladin.trace(3,"Shutdown of tap service...");
	}
	
	public void	showOnUploadFrame(TapClient tapClient) {
		if (this.uploadFrame == null) {
			this.uploadFrame = new FrameUploadServer(this.aladin, tapClient.tapBaseUrl);
		}
		uploadFrame.show(tapClient);
	}
	
	/**
	 * Hide the registry form of tap
	 */
	public void hideTapRegistryForm() {
		if (tapFrameServer != null) {
			tapFrameServer.setVisible(false);
		}
	}
	
	/**
	 * Add new tap client for tree or server dialog tap client cache
	 * @param isForDialog
	 * @param actionName
	 * @param tapClient
	 */
	public void addNewTapClientToCache(boolean isForDialog, String actionName, TapClient tapClient) {
		if (isForDialog) {
    		TapManager.tapServerPanelCache.put(actionName, tapClient);
		} else {
			TapManager.tapServerTreeCache.put(actionName, tapClient);
		}
	}
	
	public TapClient getExistingTapClientForGluActionName(TapClientMode clientMode, String actionName) {
		TapClient result = null;
		if (clientMode == TapClientMode.TREEPANEL && TapManager.tapServerTreeCache.containsKey(actionName)) {
			result = TapManager.tapServerTreeCache.get(actionName);
		} else if (clientMode == TapClientMode.DIALOG && TapManager.tapServerPanelCache.containsKey(actionName)) {
			result = TapManager.tapServerPanelCache.get(actionName);
		}
		return result;
	}
	
	/**
	 * Gets the selected datalabel on click of load
	 * @throws Exception 
	 */
	public void setSelectedServerLabel(){
//		this.selectedServerLabel = tapServerLabels.get(DataLabel.selectedId);
		this.tapFrameServer.selectedServerLabel = null;
		int whichList = this.tapFrameServer.allRegistryPane.getSelectedIndex();
		for (DataLabel datalabel : getTapServerList(whichList)) {
			if (datalabel.gui.isSelected()) {
				this.loadTapServerList();
				this.tapFrameServer.selectedServerLabel = datalabel;
				break;
			}
		}
	}
	
	/**
	 * For tap clients loaded from AlaGlu.dic - we don't reload
	 * to reload from AlaGlu.dic - user has to drag-drop the said glu to Aladin again
	 * Reload button is mainly for the servers whose metadata is loaded from the internet. 
	 * @return
	 */
	public boolean canReload(String cacheId) {
		boolean result = false;
		if (tapServerPanelCache.containsKey(cacheId) && tapServerPanelCache.get(cacheId).serverTap != null
				&& tapServerPanelCache.get(cacheId).serverTap.isVisible()) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Adds newly loaded plancatalogue into upload options
	 * @param newPlan
	 */
	public void updateAddUploadPlans(Plan newPlan) {
		if (this.uploadFrame != null && this.uploadFrame.uploadOptions != null) {
			if (newPlan.pcat != null && newPlan.pcat.flagVOTable) {
				this.uploadFrame.uploadOptions.addItem(newPlan.label);
				this.uploadFrame.uploadOptions.setEnabled(true);
				if (this.uploadFrame.isVisible()) {
					this.uploadFrame.infoLabel.setText("New option added to upload file choices!");
					this.eraseNotification(this.uploadFrame.infoLabel, EMPTYSTRING);
					this.uploadFrame.uploadOptions.revalidate();
					this.uploadFrame.uploadOptions.repaint();
				}

			}
		}
	}

	public void updateDeleteUploadPlans(Plan planInDeletion) {
		if (this.uploadFrame != null && this.uploadFrame.uploadOptions != null) {
			if (planInDeletion.pcat != null && planInDeletion.pcat.flagVOTable) {
				this.uploadFrame.uploadOptions.removeItem(planInDeletion.label);
				if (this.uploadFrame.uploadOptions.getItemCount() <= 0) {
					this.uploadFrame.uploadOptions.setEnabled(false);
				} else {
					this.uploadFrame.uploadOptions.setEnabled(true);
				}
				if (this.uploadFrame.isVisible()) {
					this.uploadFrame.infoLabel.setText(FrameUploadServer.TABLEDISCARDINFO);
					this.eraseNotification(this.uploadFrame.infoLabel, EMPTYSTRING);
					this.uploadFrame.uploadOptions.revalidate();
					this.uploadFrame.uploadOptions.repaint();
				}

			}
		}
	}
	
	public ServerTapExamples getNewServerTapExamplesInstance(TapClient tapClient) {
		// TODO Auto-generated method stub
		ServerTapExamples newServer = new ServerTapExamples(aladin, tapClient);
		newServer.setName(tapClient.tapLabel);
		newServer.tapClient = tapClient;
		newServer.setOpaque(true);
		newServer.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
		return newServer;
	}
	
	public ServerTap getNewServerTapInstance(TapClient tapClient) {
		// TODO Auto-generated method stub
		ServerTap newServer = new ServerTap(aladin);//final resort is to create generic form ServerTap
		newServer.setName(tapClient.tapLabel);
		newServer.tapClient = tapClient;
		newServer.setOpaque(true);
		newServer.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
		return newServer;
	}
	
	//TODO:: tintin remove below method
	public ServerGlu getNewServerObsTapInstance2(TapClient tapClient) {
		// TODO Auto-generated method stub
		String sodaGluRecord = StandardFormsReader.getInstance().getStdServerForms().get(OBSCORE);
		ByteArrayInputStream dicStream = new ByteArrayInputStream(sodaGluRecord.getBytes(StandardCharsets.UTF_8));
		aladin.glu.vGluServer = new Vector(50);
		aladin.glu.loadGluDic(new DataInputStream(dicStream),true,false);
		Vector serverVector = aladin.glu.vGluServer;
		int n = serverVector.size();
        if( n == 0 ) return null;
        ServerGlu obsTapServer = (ServerGlu) serverVector.get(0);
		return obsTapServer;
	}
	
	public ServerObsTap getNewServerObsTapInstance(TapClient tapClient) {
		// TODO Auto-generated method stub
		ServerObsTap newServer = new ServerObsTap(aladin);
		newServer.setName(tapClient.tapLabel);
		newServer.tapClient = tapClient;
//		newServer.setOpaque(true);
//		newServer.createForm(null);
		return newServer;
	}
	
	/**
	 * Cached tapservers are obtained from either of the caches
	 * For upload- we do not care whether it is a tree panel or a dialog panel
	 * For now only ServerTap has upload feature
	 * @param label
	 * @return
	 */
	public static Server getTapServerForLabel(String label) {
		Server result = null;
		if (tapServerPanelCache.containsKey(label)) {
			result = tapServerPanelCache.get(label).serverTap;
		} else if (tapServerTreeCache.containsKey(label)) {
			result = tapServerTreeCache.get(label).serverTap;
		}
		return result;
	}
	
	//TODO:: tintin delete the below one method if it is not used as much
	public void displayWarning(TapClient tapClient, String message) {
		if (tapClient.mode == TapClientMode.TREEPANEL) {
			Aladin.warning(this.tapPanelFromTree, message);
		} else {
			Aladin.warning(aladin.dialog, message);
		}
	}
	
	public void displayWarning(Server server, int requestNumber, String message) {
		displayWarning(server.tapClient, message);
		server.setStatusForCurrentRequest(requestNumber, Ball.NOK);
	}
	
	/**
	 * Method to obtain the fully qualified table name form a DefaultDBTable
	 * @param defaultDBTable
	 * @return
	 */
	public static String getFullyQualifiedTableName(DefaultDBTable defaultDBTable) {
		StringBuffer tableName = new StringBuffer();
		if (defaultDBTable.getADQLCatalogName() != null) {
			tableName.append(defaultDBTable.getADQLCatalogName()).append(DOT_CHAR);
		}
		if (defaultDBTable.getADQLSchemaName() != null) {
			tableName.append(defaultDBTable.getADQLSchemaName()).append(DOT_CHAR);
		}

		if (defaultDBTable.getADQLName() != null) {
			tableName.append(defaultDBTable.getADQLName());
		}
		return tableName.toString();
	}
	
	public static boolean areSameQueryCheckerTables(DefaultDBTable input1, DefaultDBTable input2) {
		boolean result = false;
		String input1QualifiedName = getFullyQualifiedTableName(input1);
		if (!input1QualifiedName.isEmpty()) {
			String input2QualifiedName = getFullyQualifiedTableName(input2);
			if (!input2QualifiedName.isEmpty()) {
				if (input1QualifiedName.equalsIgnoreCase(input2QualifiedName)) {
					result = true;
				}
			}
		}
		return result;
	}

	public void refreshTapSettings(String uploadTableName, int choice, Map<String, TapTable> tablesMetaData) {
		// TODO Auto-generated method stub
		if (this.settingsFrame != null) {
			switch (choice) {
			case 1:
				this.settingsFrame.refreshTapSettings_Add(uploadTableName, tablesMetaData);
				break;
			case 2:
				this.settingsFrame.refreshTapSettings_Delete(uploadTableName, tablesMetaData);
				break;
			default:
				this.settingsFrame.refreshTapSettings_Add(uploadTableName, tablesMetaData);
				break;
			}
		}
	}

}
