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

import static cds.aladin.Constants.ALADINTABLEPREFIX;
import static cds.aladin.Constants.COLUMNNAME;
import static cds.aladin.Constants.COUNT;
import static cds.aladin.Constants.DATATYPE;
import static cds.aladin.Constants.DESCRIPTION;
import static cds.aladin.Constants.DIRQUERY_GETALLTAPSERVERS;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.FROM_COLUMN;
import static cds.aladin.Constants.FROM_TABLE;
import static cds.aladin.Constants.GETRESULTPARAMS;
import static cds.aladin.Constants.INDEXED;
import static cds.aladin.Constants.PATHASYNC;
import static cds.aladin.Constants.PATHSYNC;
import static cds.aladin.Constants.PRINCIPAL;
import static cds.aladin.Constants.SCHEMANAME;
import static cds.aladin.Constants.SIZE;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.STD;
import static cds.aladin.Constants.TABLENAME;
import static cds.aladin.Constants.TABLETYPE;
import static cds.aladin.Constants.TAP;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTLOADED;
import static cds.aladin.Constants.TARGET_COLUMN;
import static cds.aladin.Constants.TARGET_TABLE;
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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import adql.db.DefaultDBTable;
import cds.aladin.Constants.TapClientMode;
import cds.allsky.Constante;
import cds.mocmulti.MocItem;
import cds.mocmulti.MocItem2;
import cds.savot.binary.DataBinaryReader;
import cds.savot.binary.SavotDataReader;
import cds.savot.binary2.DataBinary2Reader;
import cds.savot.model.FieldSet;
import cds.savot.model.SavotBinary;
import cds.savot.model.SavotBinary2;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.MultiPartPostOutputStream;
import cds.tools.TwoColorJTable;
import cds.tools.Util;
import cds.xml.ExamplesReader;
import cds.xml.Field;
import cds.xml.VOSICapabilitiesReader;

public class TapManager {
   
	
	private static TapManager instance = null;
	private boolean initAllLoad = true;
	
	public Aladin aladin;
	protected static Vector<Vector<String>> splTapServerLabels;
	protected static Vector<String> allTapServerLabels;
	protected TapFrameServer tapFrameServer = null;
	protected UploadFacade uploadFacade;
	MutableComboBoxModel uploadTablesModel = new DefaultComboBoxModel();
	protected FrameSimple joinFrame;
	
	protected static final int TIMEOUT = 45000;   // 45s pour les requêtes TAP
    protected static final String FORMAT = "FORMAT="+URLEncoder.encode("application/x-votable+xml;serialization=TABLEDATA")+"&";
//    protected static final String FORMAT = "";
	
	protected static Map<String, TapClient> tapServerPanelCache = new HashMap<>();//main cache where all the ServerGlu's are loaded on init
	protected static Map<String, TapClient> tapServerTreeCache = new HashMap<>();//cache for the servers loading from tree
	public static final String STANDARDQUERYPARAMSTEMPLATE = "REQUEST=doQuery&LANG=ADQL&MAXREC="+TapManager.MAXTAPROWS+"&"+FORMAT+"QUERY=";
	public static final String GETTAPSCHEMACOLUMNCOUNT = STANDARDQUERYPARAMSTEMPLATE + "SELECT+COUNT%28*%29+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMACOLUMNS = STANDARDQUERYPARAMSTEMPLATE + "SELECT+*+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMATABLES = STANDARDQUERYPARAMSTEMPLATE + "SELECT+*+FROM+TAP_SCHEMA.tables";
	public static final String GETTAPSCHEMATABLENAMES = STANDARDQUERYPARAMSTEMPLATE + "SELECT+table_name+FROM+TAP_SCHEMA.tables";
	public static final String GETTAPSCHEMACOLUMN = "SELECT * FROM TAP_SCHEMA.columns WHERE table_name = '%1$s'";
	public static final String GETTAPCAPABILITIES = "capabilities";
//	public static final String GETTAPSCHEMATABLE = GETTAPSCHEMATABLES+"+where+table_name+%3D+'%1$s'+";
	public static final String GETTAPEXAMPLES = "examples";
	public static final String GETTAPFOREIGNRELFORTABLE = "SELECT target_table, from_table, target_column, from_column FROM TAP_SCHEMA.keys JOIN TAP_SCHEMA.key_columns ON TAP_SCHEMA.keys.key_id = TAP_SCHEMA.key_columns.key_id WHERE target_table ='%1$s'";
	public static final String GENERICERROR, TAPLOADINGMESSAGE, TAPLOADERRORMESSAGE, UPLOADTABLECHANGEWARNING;
	public static final int MAXTAPCOLUMNDOWNLOADVOLUME = 1000;//limit for the table columns that will be downloaded together. 409 cadc;1000 decided limit
	public static final long MAXTAPROWS = 20000000;  // MAXREC TAP rows
	
	protected List<String> eligibleUploadServers;//URls of servers loaded and allow uploads
	
	public UWSFacade uwsFacade;
	public FrameSimple tapPanelFromTree;
	public boolean hideTapSchema;
	
	static {
		GENERICERROR = Aladin.getChaine().getString("GENERICERROR");
		TAPLOADINGMESSAGE = Aladin.getChaine().getString("TAPLOADINGMESSAGE");
		TAPLOADERRORMESSAGE = Aladin.getChaine().getString("TAPLOADERRORMESSAGE");
		UPLOADTABLECHANGEWARNING = Aladin.getChaine().getString("UPLOADTABLECHANGEWARNING");
	}
	
	public TapManager() {
		// TODO Auto-generated constructor stub
	}
	
	public TapManager(Aladin aladin) {
		// TODO Auto-generated constructor stub
		this();
		aladin.initThreadPool();
		uwsFacade = UWSFacade.getInstance(aladin);
		this.aladin = aladin;
		hideTapSchema = aladin.configuration.hideTapSchema();
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
	public void instantiateTapServers() {
		if (initAllLoad) {
			populateTapServersFromTree(null);// loading all tap from tree
			populateSplFromDirectory();//loading spl list directory
		}
	}
	
	public Vector<Vector<String>> getPreSelectedTapServers() {
		instantiateTapServers();
		return splTapServerLabels;
	}
	
	public List<String> getRegistryTapServers() {
		instantiateTapServers();
		return allTapServerLabels;
	}
	
	public boolean isValidTapServerList() {
		boolean result = false;
		instantiateTapServers();
		if (splTapServerLabels != null && !splTapServerLabels.isEmpty()) {
			result = true;
		} else if (allTapServerLabels != null && !allTapServerLabels.isEmpty()) {
			result = true;
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
			Vector<String> newDatalabel = new Vector<>();
			newDatalabel.add(TapFrameServer.labelId, label);
			newDatalabel.add(TapFrameServer.descriptionId, description);
			newDatalabel.add(TapFrameServer.urlId, url);
			if (splTapServerLabels == null) {
				splTapServerLabels = new Vector<>();
			}
			removeOldEntries(label);
			splTapServerLabels.add(newDatalabel);
		}
	}
	
	public void removeOldEntries(String actionName) {
		for (Vector<String> datalabel : splTapServerLabels) {
			if (datalabel.get(0) != null && datalabel.get(0).equals(actionName)) {
				splTapServerLabels.remove(datalabel);
				break;
			}
		}
	}

	public void populateTapServersFromTree(String mask){
		try {
			if (allTapServerLabels == null) {
				allTapServerLabels = new Vector<>();
			} else {
				allTapServerLabels.clear();
			}
			if (mask == null || mask.isEmpty()) {
				mask = DIRQUERY_GETALLTAPSERVERS;
			}
			allTapServerLabels.addAll(aladin.directory.getTAPServers(mask));
			
		} catch (Exception e) {
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			Aladin.error(tapFrameServer, "Tap servers not loaded from directory tree!", 1);
		}
	}
	
	/**
	 * To populate the tap servers configured in the directory tree
	 */
	private void populateSplFromDirectory() {
		try {
			if (splTapServerLabels == null) {
				splTapServerLabels = new Vector<>();
			}
			String url;
			String description;
            ArrayList<String> tapServersMocId = aladin.directory.getPredefinedTAPServersMultiProp(); // PF test
            for( String id : tapServersMocId ) {
                // String auth = Util.getSubpath(id, 0);
                MocItem mi = aladin.directory.multiProp.getItem(id);
                url = mi.prop.get("tap_service_url");
                description = mi.prop.get("obs_title");
                if( description == null ) description = mi.prop.get("obs_collection");
                Vector<String> tapServer = new Vector<>();
                tapServer.add(TapFrameServer.labelId, id);
                tapServer.add(TapFrameServer.descriptionId, description);
                tapServer.add(TapFrameServer.urlId, url);
                splTapServerLabels.add(tapServer);
             }
			Aladin.trace(3, "populateFromDirectory():: Done loading from directory...");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace >=3) e.printStackTrace();
			Aladin.error(tapFrameServer, "Unable to load tap servers from directory !", 1);
		} finally {
			initAllLoad = false;
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
	 * @param cacheId
	 * @param url
	 * @param modeSelected
	 * @throws Exception
	 */
	public void loadTapServer(String gluActionName, String label, String url, String defaultTables) throws Exception {
		//either get exixitng details form the configuration file or call url to get tap server details.
		if (label != null) {//not a necessary condition. But adding just in case.
			TapClient tapClient = null;
			if (this.joinFrame != null && this.joinFrame.isVisible()) {
				this.joinFrame.setVisible(false);
			}
			String cacheId = labelInCache(gluActionName, label, tapServerPanelCache);
			if (cacheId != null) {
				tapClient = tapServerPanelCache.get(cacheId);
				tapClient.tapBaseUrl = url;//in case there is any updates to the generic server url
				if (defaultTables != null) {
					tapClient.updateNodeAndSetModes(defaultTables);
				}
			} else {//not in cache..and does not have a glu record
				cacheId = labelInCache(gluActionName, label, tapServerTreeCache);
				if (cacheId != null){//check in the other cache if there is a reference.
					tapClient = copyTapClientAndDisplay(tapServerTreeCache.get(cacheId), url, TapClientMode.DIALOG, defaultTables);
				} else {
					//final resort is to create generic form ServerTap
					cacheId = label;
					tapClient = new TapClient(TapClientMode.DIALOG, this, label, url, defaultTables);
				}
				tapServerPanelCache.put(cacheId, tapClient);
			}
			Server resultServer = tapClient.getServerToDisplay(null);
			aladin.dialog.findReplaceServer(aladin.dialog.tapServer, resultServer);
			aladin.dialog.tapServer = resultServer;
			
			//log below
			StringBuffer params = new StringBuffer("Displaying ");
			if (tapClient.model.getSelectedItem() != null) {
				params.append(tapClient.model.getSelectedItem()).append(SPACESTRING)
				.append(tapClient.tapBaseUrl);
			}
			aladin.glu.log(TAP, params.toString());
		}
	}
	
	/**
	 * Method creates a tap server from label(primary ID) and url
	 * - server is either created from glu records or a generic panel is created
	 * @param cacheId
	 * @param url
	 * @param defaultTables 
	 * @return resultant server tap panel
	 * @throws Exception 
	 */
	public Server loadTapServerForSimpleFrame(String gluActionName, String label, String url, String defaultTables) throws Exception {
		TapClient tapClient = null;
		String cacheId = labelInCache(gluActionName, label, tapServerTreeCache);
		if (cacheId != null) {//get from cache
			tapClient = tapServerTreeCache.get(cacheId);
			tapClient.tapBaseUrl = url;
			if (defaultTables != null) {
				tapClient.updateNodeAndSetModes(defaultTables);
			}
		} else {//not in cache..and does not have a glu record
			cacheId = labelInCache(gluActionName, label, tapServerPanelCache);
			if (cacheId != null){//try to create from the main cache
				tapClient = copyTapClientAndDisplay(tapServerPanelCache.get(cacheId), url, TapClientMode.TREEPANEL, defaultTables);
			} else {
				cacheId = label;
				tapClient = new TapClient(TapClientMode.TREEPANEL, this, label, url, defaultTables);
			}
			tapServerTreeCache.put(cacheId, tapClient);
		}
		Server resultServer = tapClient.getServerToDisplay(null);
		showTapPanelFromTree(cacheId, resultServer);
		
		//log below
		StringBuffer params = new StringBuffer("Displaying ");
		if (tapClient.model.getSelectedItem() != null) {
			params.append(tapClient.model.getSelectedItem()).append(SPACESTRING)
			.append(tapClient.tapBaseUrl);
		}
		aladin.glu.log(TAP, params.toString());
		
		return resultServer;
	}
	
	/**
	 * its Glu's action ID or tree label that becomes the primary key for tap cache
	 * based on whether there is a glu record for the gluActionName or not
	 * @param gluActionName
	 * @param actionName
	 * @return
	 */
	public String labelInCache(String gluActionName, String actionName, Map<String, TapClient> cache) {
		String label = null;
		if (gluActionName != null && cache.containsKey(gluActionName)) {
			label = gluActionName;
		} else if (actionName != null && cache.containsKey(actionName)) {
			label = actionName;
		}
		return label;
	}
	
	/**
	 * Creates a generic tap server
	 * Slight changes in resultant server w.r.t to the container: TreePanel or ServerDialog
	 * @param tapClient
	 * @param newServer
	 * @return
	 * @throws Exception
	 */
	public DynamicTapForm createAndLoadATapServer(TapClient tapClient, DynamicTapForm newServer) throws Exception {
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
		
		// just another control on the nodes feature
//		if (tapClient.mode != null && tapClient.mode == TapClientMode.TREEPANEL) {
//			tapClient.primaryColor = Aladin.COLOR_FOREGROUND;
//			tapClient.secondColor = Color.white;
//		}
		newServer.setOpaque(true);
		newServer.showloading();
		tapClient.capabilities = this.getTapCapabilities(tapClient.tapBaseUrl);
		/*if (nodeOn && vizierTable != null) {
			//if you have a table name
			this.loadTapColumnSchemasForATable(tapClient, vizierTable, newServer);
		} else {
			this.loadTapColumnSchemas(tapClient, newServer);
		}*/
		this.loadTapColumnSchemas(tapClient, newServer);
		return newServer;
	}

	public TapClient copyTapClientAndDisplay(TapClient original, String urlInput, TapClientMode mode, String nodeTables) {
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
			if (nodeTables != null) {
				copy.updateNodeAndSetModes(nodeTables);
			}
		} 
		
		if (copy == null) {//only if serverglu is null yet
			copy = new TapClient(mode, this, original.tapLabel, urlInput, nodeTables);
		} 
		
		copyMetadata(copy, original, mode, false);
		copy.tapBaseUrl = urlInput;
		
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
	 * @param b 
	 * @return
	 */
	public void updateServerMetaDataInCache(TapClient original, boolean onlyInfoPanel) {
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
			copyMetadata(toUpdate, original, mode, onlyInfoPanel);
			/*if (toUpdate.serverTap == null) {
				toUpdate.serverTap = getNewServerTapInstance(toUpdate, true);
				toUpdate.serverTap.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
			}*/
		}
	}
	
	//we wont deepcopy the metadata and the infopanel. we will only duplicate the main form using the metadata
	public void copyMetadata(TapClient copy, TapClient original, TapClientMode mode, boolean onlyInfoPanel) {
		// copy most metadata
		if (!onlyInfoPanel) {
			copy.tapLabel = original.tapLabel;
			copy.tapBaseUrl = original.tapBaseUrl;
			copy.capabilities = original.capabilities;
			copy.setData(original.tablesMetaData);
			copy.queryCheckerTables = original.queryCheckerTables;
			copy.obscoreTables = original.obscoreTables;
//			if (mode == TapClientMode.TREEPANEL) {
//				copy.primaryColor = Aladin.COLOR_FOREGROUND;
//				copy.secondColor = Color.white;
//			}
		}
		copy.infoPanel = original.infoPanel;
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
	 */
	public void createGenericTapFormFromMetaData(final DynamicTapForm newServer) {
		try {
			aladin.executor.execute(new Runnable() {
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
				tapServer.makeCursor(Aladin.WAITCURSOR);
				this.showTapRegistryForm();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.error(aladin.dialog, GENERICERROR);
				e.printStackTrace();
			} finally {
				tapServer.makeCursor(Aladin.DEFAULTCURSOR);
			}
			result = false;
		}
		return result;
	}
	
	/**
	 * Idea is to set some known FOVs from moc server.
	 * If not we'l set a dummy one.
	 * @param tc
	 */
	protected void setTargetDimensions(TapClient tc) {
		MocItem2 mocItem = aladin.directory.multiProp.getItem(tc.tapLabel);
		if (mocItem != null) {
			MyProperties prop = aladin.directory.multiProp.getItem(tc.tapLabel).prop;
			
			String s = prop.getProperty(Constante.KEY_HIPS_INITIAL_RA);
//			s = prop.getProperty("obs_initial_ra");
		      if( s!=null) {
		         String s1 = prop.getProperty(Constante.KEY_HIPS_INITIAL_DEC);
//		         s1 = prop.getProperty("obs_initial_dec");
		         if( s1!=null ) s = s+" "+s1;
		         else s=null;
		      }
			if( s==null ) tc.target=null;
		      else {
		         try { tc.target = new Coord(s); }
		         catch( Exception e) { aladin.trace(3,"target error!"); tc.target=null; }
		      }
		      double div2=2;
		      s = prop.getProperty(Constante.KEY_HIPS_INITIAL_FOV);
//		      s = prop.getProperty("obs_initial_fov");
			if( s==null ) tc.radius=-1;
		      else {
		         try { tc.radius=(Server.getAngleInArcmin(s, Server.RADIUSd)/60.)/div2; }
		         catch( Exception e) { aladin.trace(3,"radius error!"); tc.radius=-1; }
		      }
		}
	}
	
	/*private String getDefaultTarget() {
	      Coord coo;
	      if( aladin.view.isFree() || !Projection.isOk( aladin.view.getCurrentView().getProj()) ) {
	         String s = aladin.localisation.getTextSaisie();
	         if( s.length()!=0 ) return s;
	         coo = getTarget();
	         s = coo==null ? null : coo.toString();
	         if( s!=null ) return s;
	         return null;
//	         aladin.info("Pas encore implanté, il faudrait demander Target+Radius");
	      }
	      coo = aladin.view.getCurrentView().getCooCentre();
	      coo = aladin.localisation.ICRSToFrame( coo );
	      return coo.getDeg();
	   }*/
	
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
			capabilities = aladin.executor.submit(new Callable<VOSICapabilitiesReader>() {
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
						Aladin.trace(3, "Unable to get capabilitites for.."+tapServiceUrl);
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
	
	public Map getTapExamples(String tapServiceUrl) {
		ExamplesReader daliExamplesReader = new ExamplesReader();
		MyInputStream is = null;
		Map examples = null;
		try {
			URL url = getUrl(tapServiceUrl, null, GETTAPEXAMPLES);//new URL(tapServiceUrl+GETTAPEXAMPLES);
//			URL examplesUrl = new URL("http://gaia.ari.uni-heidelberg.de/tap/examples");
//			URL examplesUrl = new URL("http://130.79.129.54:8080/simbadExamples.xhtml");
//			URL examplesUrl = new URL("http://130.79.129.54:8080/view-source_gaia.ari.uni-heidelberg.de_tap_examples.xhtml");
			Aladin.trace(3, "TapManager.getResults() for: "+url);
			long startTime = getTimeToLog();
			is = Util.openStreamForTapAndDL(url, null, true, TIMEOUT);
			long time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("DaliExamples got inputstream: "+time+" time taken: "+(time - startTime));
			startTime = getTimeToLog();
			examples = daliExamplesReader.parse(is);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace > 3) e.printStackTrace();
		}
		return examples;
	}
	
	/**
	 * Method to populate tap meta data
	 * @param tableName 
	 * @param newServer 
	 * @param tapServiceUrl
	 */
	/*public void loadTapColumnSchemasForATable(final TapClient clientToLoad, final String tableName, final DynamicTapForm newServer) {
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
	}*/
	
	/**
	 * Method to populate tap meta data
	 * @param newServer 
	 * @param tapServiceUrl
	 */
	public void loadTapColumnSchemas(final TapClient clientToLoad, final DynamicTapForm newServer){
		try {
			aladin.executor.execute(new Runnable(){
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					String tapServiceUrl = clientToLoad.tapBaseUrl;
					currentT.setName("TgetMetaInfo: "+tapServiceUrl);
					final long startTime = getTimeToLog();
					if (Aladin.levelTrace >= 4) System.out.println("loadTapColumnSchemas starting: "+startTime);
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
							SavotResource resultsResource = getResults("getAllTables", tapServiceUrl,GETTAPSCHEMATABLENAMES, PATHSYNC);
							//update table meta anyway and then create the info panel
							updateTableMetadata(newServer, tapServiceUrl);
							populateTables(clientToLoad, resultsResource);
							String defaultTable = clientToLoad.tablesMetaData.keySet().iterator().next();
							String tableNameQueryParam = defaultTable;
//							tableNameQueryParam = URLEncoder.encode(tableNameQueryParam, UTF8);
							
							//get single table data and populate it to front end
							String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, tableNameQueryParam);
							gettablesColumnsQuery = URLEncoder.encode(gettablesColumnsQuery, UTF8);
							gettablesColumnsQuery = STANDARDQUERYPARAMSTEMPLATE + gettablesColumnsQuery;
							SavotResource columnResults = getResults("getOneTableColums", tapServiceUrl, gettablesColumnsQuery, PATHSYNC);
							populateColumns(clientToLoad, columnResults);
						} else if (count > 0) {
							// download all
							SavotResource resultsResource = getResults("getAllTableColums", tapServiceUrl, GETTAPSCHEMACOLUMNS, PATHSYNC);
							//update table meta anyway and then create the info panel
							updateTableMetadata(newServer, tapServiceUrl);
							populateColumns(clientToLoad, resultsResource);
						} else {
							newServer.showLoadingError();
							displayWarning(clientToLoad, "Error from tap server "+clientToLoad.tapLabel+" : unable to get metadata !");
							return;
						}
						clientToLoad.preprocessTapClient();
						newServer.createFormDefault(); //default choice is the first table
						long time = getTimeToLog();
						if (Aladin.levelTrace >= 4) System.out.println("all done at: "+time+" time taken: "+(time - startTime));
						
						// when we are explicitly getting metadata for one cache, we try to update in the other as well
						updateServerMetaDataInCache(clientToLoad, false);
					} catch (Exception e) {
						e.printStackTrace();
						newServer.showLoadingError();
						displayWarning(clientToLoad, e.getMessage());
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
	 * Method to load all foreign key relations for main table
	 * @param clientToLoad
	 * @param joinFacade
	 * @param table_name
	 */
	public void loadForeignKeyRelationsForSelectedTable(final TapClient clientToLoad, final JoinFacade joinFacade, final String table_name){
		try {
			aladin.executor.execute(new Runnable(){
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					String tapServiceUrl = clientToLoad.tapBaseUrl;
					currentT.setName("TgetForeignRelMetaInfo: "+table_name+" - "+tapServiceUrl);
					final long startTime = getTimeToLog();
					if (Aladin.levelTrace >= 4) System.out.println("loadTapColumnSchemas starting: "+startTime);
					try {
						if (clientToLoad.tablesMetaData.get(table_name).foreignKeyColumns == null) {
							try {
								String tableNameQueryParam = URLEncoder.encode(table_name, UTF8);
								//get single table data and populate it to front end
								String gettablesForeignKeyColumnsQuery = String.format(GETTAPFOREIGNRELFORTABLE, tableNameQueryParam);
								gettablesForeignKeyColumnsQuery = URLEncoder.encode(gettablesForeignKeyColumnsQuery, UTF8);
								gettablesForeignKeyColumnsQuery = STANDARDQUERYPARAMSTEMPLATE + gettablesForeignKeyColumnsQuery;
								SavotResource keysResults = getResults("getAllForeignRelsForSelTable", tapServiceUrl, gettablesForeignKeyColumnsQuery, PATHSYNC);
								populateForeignKeys(clientToLoad, table_name, keysResults);
								// when we are explicitly getting metadata for one cache, we try to update in the other as well
								updateServerMetaDataInCache(clientToLoad, false);
							} catch (Exception e) {
								// TODO: handle exception
								Aladin.trace(3, "No foreign keys read!");
								// we won't bother with the exceptions here
							}
						}
						joinFacade.setJoinTableForm(null, null);
						long time = getTimeToLog();
						if (Aladin.levelTrace >= 4) System.out.println("all done at: "+time+" time taken: "+(time - startTime));
					} catch (Exception e) {
						e.printStackTrace();
						joinFacade.showLoadingError();
						Aladin.error(joinFacade, e.getMessage());
					} finally {
						joinFacade.revalidate();
						joinFacade.repaint();
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			// TODO: handle exception
			joinFacade.showLoadingError();
			joinFacade.revalidate();
			joinFacade.repaint();
			Aladin.error(joinFacade, "Unable to get metadata for "+clientToLoad.tapLabel+"\n Request overload! Please wait and try again.");
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
//		tapFrameServer.tabbedTapThings.setSelectedIndex(0);
		// tapFrameServer = new TapFrameServer(aladin,this);
		tapFrameServer.setVisible(true);
		tapFrameServer.toFront();
	}
	
	public void loadTapServerList() {
		if (tapFrameServer == null) {
			tapFrameServer = new TapFrameServer(aladin, this);
			tapFrameServer.createCenterPane();
			Aladin.makeCursor(tapFrameServer.options, Aladin.WAITCURSOR);
			loadPreselectedServers();
		}
	}
	
	public void loadPreselectedServers() {
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TinitPreselectedServers: ");
						tapFrameServer.initPreselectedServers();
						Aladin.makeCursor(tapFrameServer.options, Aladin.DEFAULTCURSOR);
						Aladin.makeCursor(tapFrameServer.treeRegistryPanel, Aladin.WAITCURSOR);
						loadAllServers();
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			Aladin.trace(3, "RejectedExecutionException");
			tapFrameServer.showRegistryNotLoaded();
			Aladin.makeCursor(tapFrameServer.options, Aladin.DEFAULTCURSOR);
			Aladin.makeCursor(tapFrameServer.treeRegistryPanel, Aladin.DEFAULTCURSOR);
		}
	}
	
	public void loadAllServers() {
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TinitAllServers: ");
						tapFrameServer.initAllServers();
						Aladin.makeCursor(tapFrameServer.treeRegistryPanel, Aladin.DEFAULTCURSOR);
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			Aladin.trace(3, "RejectedExecutionException");
			tapFrameServer.showCompleteListNotLoaded();
			Aladin.makeCursor(tapFrameServer.treeRegistryPanel, Aladin.DEFAULTCURSOR);
		}
	}
	
	public void reloadTapServerList() {
		if (tapFrameServer != null) {
			tapFrameServer.initPreselectedServers();
			tapFrameServer.reloadRegistryPanel();
		}
	}
	
	public void initTapExamples(final ServerTapExamples serverTapExamples) {
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TinitTapExamples: "+serverTapExamples.tapClient.tapLabel);
						serverTapExamples.ball.setMode(Ball.WAIT);
						serverTapExamples.info1.setText("Loading service examples...");
						serverTapExamples.loadTapExamples();
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			Aladin.trace(3, "RejectedExecutionException");
			// we do not display server examples.
		}
	}
	
	/**
	 * Method updates the tap metadata and calls for a metadata gui update.
	 * @param tableName
	 * @param dynamicTapForm
	 * @param tableNames 
	 * @return
	 * @throws Exception
	 */
	public void updateTableColumnSchemas(DynamicTapForm dynamicTapForm, List<String> tableNames) throws Exception {
		dynamicTapForm.ball.setMode(Ball.WAIT);
		if (tableNames == null || tableNames.isEmpty()) {
			throw new Exception("Error no table name provided !\n");
		}
		try {
			String tapServiceUrl = dynamicTapForm.tapClient.tapBaseUrl;
			
			boolean isNotFirst = false;
			StringBuffer whereCondition = new StringBuffer();
			String gettablesColumnsQuery = null;
			if (tableNames.size() == 1) {
				gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, tableNames.get(0));
				
			} else {
				for (String tableName : tableNames) {
					if (isNotFirst) {
						whereCondition.append(" OR table_name = ");
					} else {
						whereCondition.append(" table_name = ");
					}
					whereCondition.append(Util.formatterPourRequete(true, tableName));
					isNotFirst = true;
				}
				gettablesColumnsQuery = "SELECT * FROM TAP_SCHEMA.columns WHERE "+whereCondition.toString();
			}
			
			gettablesColumnsQuery = URLEncoder.encode(gettablesColumnsQuery, UTF8);
			gettablesColumnsQuery = STANDARDQUERYPARAMSTEMPLATE + gettablesColumnsQuery;
			
//			String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, whereCondition.toString());
			SavotResource columnResults = getResults("updateTableColumnSchemas", dynamicTapForm.tapClient.tapBaseUrl, gettablesColumnsQuery, PATHSYNC);
			populateColumns(dynamicTapForm.tapClient, columnResults);
			
			dynamicTapForm.updateQueryChecker(tableNames);
			 //update info panel
			Future<JPanel> infoPanel = this.createMetaInfoDisplay(tapServiceUrl, dynamicTapForm.tapClient.tablesMetaData);
			if (infoPanel != null) {
				dynamicTapForm.tapClient.tackleFrameInfoServerUpdate(aladin, dynamicTapForm, infoPanel);
			}
			Aladin.trace(3,"done updating tap info for : "+tableNames.toString()+"| server is : "+dynamicTapForm.tapClient.tapBaseUrl);
			dynamicTapForm.ball.setMode(Ball.OK);
		} catch (RejectedExecutionException e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			dynamicTapForm.ball.setMode(Ball.NOK);
			throw new Exception("Request overload! Please wait and try again.");
		} catch (Exception e) {
			dynamicTapForm.ball.setMode(Ball.NOK);
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
		if (baseUrlStr != null) {
			String path = baseUrlStr;
			int slash = 0;
			if (subPath != null) {
				if (path.endsWith("/")) {
					slash ++;
				} 
				if (subPath.startsWith("/")) {
					slash++;
				}
				switch (slash) {
				case 0:
					path = path+"/";
					break;
				case 2:
					subPath = subPath.replaceFirst("/", EMPTYSTRING);
					break;
				default:
					break;
				}
				path = path+subPath;
			}
			
			if (query != null) {
				path = path+"?"+query;
			}
			URI uri = new URI(path);
			
//			uri = new URI(uri+"?"+query);
			genUrl = uri.toURL();
		}
		
		return genUrl;
	}
	
	/**
	 * Makes small notification flags to user. The initial text set is erased after a small time delay.
	 * Useful to flag user when any gui model updates are made.
	 * @param notificationBar
	 * @param message
	 */
	public void eraseNotification(final JLabel notificationBar, final String message, final String resetText) {
		notificationBar.setFont(Aladin.LITALIC);
		notificationBar.setText(message);
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TeraseNotification: ");
						currentT.setPriority(Thread.MIN_PRIORITY);
						TimeUnit.SECONDS.sleep(3);
						notificationBar.setFont(Aladin.LPLAIN);
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
			notificationBar.setFont(Aladin.LPLAIN);
			notificationBar.setText(resetText);
		}
	}
	
	public void activateWaitMode(final ServerTap serverTap) {
		// TODO Auto-generated method stub
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						currentT.setName("TmakeWait: "+serverTap.tapClient.tapLabel);
						currentT.setPriority(Thread.MIN_PRIORITY);
						serverTap.waitCursor();
						serverTap.info1.setText(TAPLOADINGMESSAGE);
						serverTap.revalidate();
						serverTap.repaint();
					} finally {
						currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			Aladin.trace(3, "RejectedExecutionException");
			serverTap.ball.setMode(Ball.NOK);
			serverTap.info1.setText(TAPLOADERRORMESSAGE);
		}
	}
	
	/**
	 * Method populates loads all tables from TAP_SCHEMA.tables
	 * @param tapClient
	 * @param resultsResource
	 * @throws Exception
	 */
	public void populateTables(TapClient tapClient, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			if (resultsResource.getTableCount() <= 0) {
				if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateTables! Did not read table data for "+tapClient.tapBaseUrl);
				throw new Exception("ERROR while getting table information! Did not read table data from: "+tapClient.tapBaseUrl
						+" .\n\n Perhaps TAP_SCHEMA is not available for this server.");
			}
			long startTime = getTimeToLog();
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableReader(tapClient, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
                case 1:
                   binaryTableReader(tapClient, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
                   break;
                   
                // MODIF PF 20 nov 2017
                case 2:
                	binaryTableReader(tapClient, resultsResource.getData(i).getBinary2(), resultsResource.getFieldSet(i));
                   break;
				default:
					if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateTables! Did not read table data for "+tapClient.tapBaseUrl);
					throw new Exception("ERROR in populateTables! Did not read table data"+tapClient.tapBaseUrl);
				}
			}
			long time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("populateTables parsing: "+time+" time taken : "+(time - startTime));
		}
	}
	
	/**
	 * Method populates loads all foreign keys for a table from TAP_SCHEMA.keys
	 * @param tapClient
	 * @param table_name 
	 * @param resultsResource
	 * @throws Exception
	 */
	public void populateForeignKeys(TapClient tapClient, String table_name, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			if (resultsResource.getTableCount() <= 0) {
				if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateForeignKeys! Did not read foreign keys data for "+tapClient.tapBaseUrl);
				throw new Exception("ERROR while getting foreign key information! Did not read foreign key data from: "+tapClient.tapBaseUrl
						+" .\n\n Perhaps TAP_SCHEMA is not available for this server.");
			}
			long startTime = getTimeToLog();
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					foreignKeysReader(tapClient, table_name, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
                case 1:
                   foreignKeysbinaryReader(tapClient, table_name, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
                   break;
                // MODIF PF 20 nov 2017
                case 2:
                   foreignKeysbinaryReader(tapClient, table_name, resultsResource.getData(i).getBinary2(), resultsResource.getFieldSet(i));
                   break;
				default:
					if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateForeignKeys! Did not read foreign keys data for "+tapClient.tapBaseUrl);
					throw new Exception("ERROR in populateForeignKeys! Did not read table data"+tapClient.tapBaseUrl);
				}
			}
			long time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("populateForeignKeys parsing: "+time+" time taken : "+(time - startTime));
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
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName = theTDs.getContent(k);
							table.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(TABLETYPE)) {
							table.setTable_type(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(SCHEMANAME)) {
							table.setSchema_name(theTDs.getContent(k));
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
	
	protected void binaryTableReader(TapClient tapClient, SavotBinary binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinaryReader(binaryData.getStream(), fields);
		binaryTableReader(tapClient, dataReader, fields);
	}

	protected void binaryTableReader(TapClient tapClient, SavotBinary2 binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinary2Reader(binaryData.getStream(), fields);
		binaryTableReader(tapClient, dataReader, fields);
	}
	
	/**
	 * Tap Table Reader for Savot binary resource
	 * @param newServer
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
    protected void binaryTableReader(TapClient tapClient, SavotDataReader parser, FieldSet fields) throws IOException {
		try {
			TapTable table = null;
			while (parser.next()) {
				table = new TapTable();
				String tableName = null;
				for (int j = 0; j < fields.getItemCount(); j++) {
					table.setTable_type(parser.getCellAsString(j));
					SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName = parser.getCellAsString(j);
							table.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(TABLETYPE)) {
							table.setTable_type(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(SCHEMANAME)) {
							table.setSchema_name(parser.getCellAsString(j));
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
			Aladin.trace(3, "ERROR in binaryTableReader! Did not read table data for " + tapClient.tapBaseUrl);
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			throw e;
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
	}
	
    /**
	 * Tap Table Reader for Savot table Resource
     * @param selectedTableName 
	 * @param newServer
	 * @param tableRows
	 * @param fieldSet
	 * @throws Exception 
	 */
	protected void foreignKeysReader(TapClient tapClient, String selectedTableName, TRSet tableRows, FieldSet fieldSet) throws Exception {
		ForeignKeyColumn keyColumn = null;
		if (fieldSet != null && tableRows != null && tableRows.getItemCount() > 0) {
			for (int j = 0; j < tableRows.getItemCount(); j++) {
				String tableName = null;
				keyColumn = new ForeignKeyColumn();
				TDSet theTDs = tableRows.getTDSet(j);
				for (int k = 0; k < theTDs.getItemCount(); k++) {
					SavotField field = fieldSet.getItemAt(k);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TARGET_TABLE)) {
							tableName = theTDs.getContent(k);
						} else if (fieldName.equalsIgnoreCase(FROM_TABLE)) {
							keyColumn.setFrom_table(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(TARGET_COLUMN)) {
							keyColumn.setTarget_column(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(FROM_COLUMN)) {
							keyColumn.setFrom_column(theTDs.getContent(k));
						}
					}
				}
				setForeignKeyColumn(tapClient, tableName, keyColumn);
			}
		} else {
			if (Aladin.levelTrace >= 3)	System.err.println("ERROR in foreignKeysReader! Did not read keys/key-columns data for "+tapClient.tapBaseUrl);
			setForeignKeyColumn(tapClient, selectedTableName, null);
			throw new Exception("ERROR reading foreign keys! Did not read table data: "+tapClient.tapBaseUrl);
		}
	}
	
	protected void foreignKeysbinaryReader(TapClient tapClient, String selectedTableName, SavotBinary binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinaryReader(binaryData.getStream(), fields);
		foreignKeysbinaryReader(tapClient, selectedTableName, dataReader, fields);
	}

	protected void foreignKeysbinaryReader(TapClient tapClient, String selectedTableName, SavotBinary2 binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinary2Reader(binaryData.getStream(), fields);
		foreignKeysbinaryReader(tapClient, selectedTableName, dataReader, fields);
	}
	
	/**
	 * Tap Table Reader for Savot binary resource
	 * @param newServer
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
    protected void foreignKeysbinaryReader(TapClient tapClient, String selectedTableName, SavotDataReader parser, FieldSet fields) throws IOException {
		try {
           ForeignKeyColumn keyColumn = null;
			while (parser.next()) {
				keyColumn = new ForeignKeyColumn();
				String tableName = null;
				for (int j = 0; j < fields.getItemCount(); j++) {
					SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TARGET_TABLE)) {
							tableName = parser.getCellAsString(j);
						} else if (fieldName.equalsIgnoreCase(FROM_TABLE)) {
							keyColumn.setFrom_table(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(TARGET_COLUMN)) {
							keyColumn.setTarget_column(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(FROM_COLUMN)) {
							keyColumn.setFrom_column(parser.getCellAsString(j));
						}
					}
				}
				setForeignKeyColumn(tapClient, tableName, keyColumn);
			}
		} catch (IOException e) {
			Aladin.trace(3, "ERROR in foreignKeysbinaryReader! Did not read keys data for " + tapClient.tapBaseUrl);
			setForeignKeyColumn(tapClient, selectedTableName, null); //so we don't query the server multiple times
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			throw e;
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
	}
    
	public synchronized void setForeignKeyColumn(TapClient tapClient, String tableName, ForeignKeyColumn keyColumn) {
		if (tableName != null) {
			synchronized (tapClient.tablesMetaData) {
				if (tapClient.tablesMetaData.containsKey(tableName)) {
					//conserve columns if already set. but other details are updated
					TapTable tableToUpdate = tapClient.tablesMetaData.get(tableName);
					if (tableToUpdate.foreignKeyColumns == null) {
						tableToUpdate.foreignKeyColumns = new ArrayList<>();
					}
					if (keyColumn != null) {
						tableToUpdate.foreignKeyColumns.add(keyColumn);
					}
				}
			}
		}
	}
	
    
	/**
	 * Method tries to categorize savot resultsResource as either in binary or table format
	 * @param tableIndex
	 * @param resultsResource
	 * @return 2 for binary2, 1 for binary, 0 for votable
	 */
	protected int getType(int tableIndex, SavotResource resultsResource) {
		int type = -1;
// CODE ORIGINAL CHAITRA
//		if (resultsResource.getTables() == null || resultsResource.getTables().getItemAt(tableIndex) == null
//				|| resultsResource.getTables().getItemAt(tableIndex).getData() == null
//				|| (resultsResource.getTables().getItemAt(tableIndex).getData().getTableData() == null
//						&& resultsResource.getTables().getItemAt(tableIndex).getData().getBinary() != null)) {
//			type = 1; // for binary
//		} else if (resultsResource.getTables().getItemAt(tableIndex).getData().getTableData().getTRs() != null) {
//			type = 0; // for table
//		}
		
		// MODIF PF 20 nov 2017
		if (resultsResource.getTables() == null || resultsResource.getTables().getItemAt(tableIndex) == null
		      || resultsResource.getTables().getItemAt(tableIndex).getData() == null
		      || resultsResource.getTables().getItemAt(tableIndex).getData().getTableData() == null ) {
		   if( resultsResource.getTables().getItemAt(tableIndex).getData().getBinary2() != null) type = 2; // for binary2
		   else if( resultsResource.getTables().getItemAt(tableIndex).getData().getBinary() != null) type = 1; // for binary

		} else if (resultsResource.getTables().getItemAt(tableIndex).getData().getTableData().getTRs() != null) {
		   type = 0; // for table
		}

		return type;
	}

	/**
	 * Method populates all column metainfo TAP_SCHEMA.columns from savot resource 
	 * @param serverToLoad
	 * @param resultsResource
	 * @throws Exception 
	 */
	public void populateColumns(TapClient tapClient, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			long startTime = getTimeToLog();
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableColumnReader(tapClient, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
                case 1:
                   binaryColumnReader(tapClient, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
                   break;
                   
                // MODIF PF 20 nov 2017 - prise en compte du binary2
                case 2:
                   binaryColumnReader(tapClient, resultsResource.getData(i).getBinary2(), resultsResource.getFieldSet(i));
                   break;
				default:
					if (Aladin.levelTrace >= 3)	System.err.println("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
					throw new Exception("ERROR in populateColumns! Did not read table column data for "+tapClient.tapBaseUrl);
				}
			}
			long time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("populateColumns parsing: "+time+" time taken : "+(time - startTime));
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
						tableColumns = new Vector<>();
						table.setColumns(tableColumns);
					}
				} else {
					tableColumns = new Vector<>();
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
	
	protected void binaryColumnReader(TapClient tapClient, SavotBinary binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinaryReader(binaryData.getStream(), fields);
		binaryColumnReader(tapClient, dataReader, fields);
	}

	protected void binaryColumnReader(TapClient tapClient, SavotBinary2 binaryData, FieldSet fields) throws IOException {
		SavotDataReader dataReader =  new DataBinary2Reader(binaryData.getStream(), fields);
		binaryColumnReader(tapClient, dataReader, fields);
	}
	
	/**
	 * Method populates column information from savot binary resource 
	 * @param serverToLoad
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
    protected void binaryColumnReader(TapClient tapClient, SavotDataReader parser, FieldSet fields) throws IOException {
		TapTableColumn tableColumn = null; 
		try {
			while (parser.next()) {
				tableColumn = new TapTableColumn();
                for (int j = 0; j < fields.getItemCount(); j++) {
                    SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableColumn.setTable_name( parser.getCellAsString(j));
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
			Aladin.trace(3, "ERROR in binaryColumnReader! Did not read column column data for "+tapClient.tapBaseUrl);
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
		Vector<TapTableColumn> tableColumns = new Vector<>();
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
			tableColumn.setDataType(f);
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
    public static SavotResource getResults(String what, String tapServiceUrl, String file, String path) throws MalformedURLException, Exception {
       return getResults1(what,tapServiceUrl,file,path,true);
    }
    public static SavotResource getResults1(String what, String tapServiceUrl, String file, String path,
          boolean flagRedo) throws MalformedURLException, Exception {
		SavotResource resultsResource = null;
		MyInputStream is = null;
		try {
			URL url = getUrl(tapServiceUrl, file, path);
			Aladin.trace(3, "TapManager.getResults() for: "+url);
			long startTime = getTimeToLog();
			is = Util.openStreamForTapAndDL(url, null, true, 30000);
			long time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println(what+ "getResults got inputstream: "+time+" time taken: "+(time - startTime));
			startTime = getTimeToLog();
			SavotPullParser savotParser = new SavotPullParser(is, SavotPullEngine.FULL, null, false);
			if( savotParser.getTableCount()==0 ) throw new Exception("No table");  // Probably an error
			resultsResource = Util.populateResultsResource(savotParser);
			time = getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println(what+ "getResults parsing: "+time+" time taken : "+(time - startTime));
			// stream is closed downstream when it is not an error scenario
			is.close();
			
		} catch (MalformedURLException me) {
			if( Aladin.levelTrace >= 3 ) me.printStackTrace();
			throw me;
//		} catch (IOException ioe) {
//			Aladin.trace(3, ioe.getMessage());
//			if (is != null) {
//				is.close();
//			}
//			throw ioe;
		} catch (Exception e) {
		   if (is != null)  is.close();

		   // On retente en supprimant le FORMAT explicite VOTABLE TABLEDATA (non supporté par les vieux serveurs TAP)
		   if( flagRedo ) {
		      int i=file.indexOf(FORMAT);
		      if( i>=0 ) {
		         file = file.substring(0,i)+file.substring(i+FORMAT.length());
		         return getResults1(what,tapServiceUrl,file, path,false);
		      }
		   }

			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw e;
		}
		return resultsResource;
	}
	
	public static long getTimeToLog() {
		return System.nanoTime();
	}
	
	
	/**
	 * Method uploads table from PlanCatalog instance
	 * @param planToLoad
	 * @param uploadTableName
	 */
	public synchronized void initialiseUploadFacadeFromAladinPlan(final Plan planToLoad, final String uploadTableName) {
		aladin.executor.execute(new Runnable(){
			@Override
			public void run() {
				String tableName = uploadTableName;
				
				if (uploadTableName == null || uploadTableName.isEmpty()) {
					tableName = uploadFacade.generateUploadTableName(ALADINTABLEPREFIX);
				}
				populateColumnsFromPlan(planToLoad, tableName , uploadFacade.uploadTablesMetaData);
				uploadTablesModel.addElement(tableName);
			}
		});
	}
	
	/**
	 * Convenience method to quickly change table name.
	 * this is a shallow change: tablename at column level is not changed, as it is not needed as of now (for upload table name changes).
	 * This is not necessarily premature optimization, we avoid looping through columns until a time that is deemed necessary.
	 * @param oldTableName
	 * @param tableName
	 * @param tablesMetaData
	 */
	public void changeUploadTableName(String oldTableName, String tableName, Map<String, TapTable> tablesMetaData) {
		TapTable table = null;
		if (tablesMetaData.containsKey(oldTableName)) {
			table = tablesMetaData.get(oldTableName);
			tablesMetaData.remove(oldTableName);
			tablesMetaData.put(tableName, table);
			table.setTable_name(tableName);
			uploadTablesModel.removeElement(oldTableName);
			uploadTablesModel.addElement(tableName);
			uploadTablesModel.setSelectedItem(tableName);
			//edit mthod name add "reference" at the end
			//loop through join constraints..of jeez.. all the servers in the cache
				//if there are join constraints of the name
					// edit name in the constraint..
					// rewrite 
				//if not dont do anything
		}
	}
	
	/**
	 * Populates a single param value expected to be count. 
	 * @param tapServiceUrl
	 * @return count value
	 * @throws Exception
	 */
	public String getFutureResultsVolume(String tapServiceUrl) throws Exception {
		String count = null;
		SavotResource resultsResource = getResults("getCount ", tapServiceUrl, GETTAPSCHEMACOLUMNCOUNT, PATHSYNC);
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
                   
                // MODIF PF 20 nov 2017
                case 2:
                   count = getBinary2SingleParam(index , resultsResource.getData(i).getBinary2(), resultsResource.getFieldSet(index), COUNT);
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
     * Gets the value of the single param expected in the votable
     * @param index
     * @param tableRows
     * @param fieldSet
     * @param paramName - not in use. see comment below //checking COUNT... 
     * @return
     */
    // INSERTION PF nov 2017 - A REPRENDRE AVEC CHAITRA
    protected String getBinary2SingleParam(int index, SavotBinary2 binaryData, FieldSet fieldSet, String paramName) {
        String count = String.valueOf(MAXTAPCOLUMNDOWNLOADVOLUME);//set to max. If we can't read the column number, we won't download all the metadata
        DataBinary2Reader parser = null;
        try {
           parser = new DataBinary2Reader(binaryData.getStream(), fieldSet);
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
            if (Aladin.levelTrace >= 3) e.printStackTrace();
        }
        
        return count;
    }
    
	/**
	 * Method to lazily gets table metadata.
	 * @param newServer 
	 * @param clientToLoad
	 * @param tapServiceUrl
	 * @param tablesMetaData
	 */
	public void updateTableMetadata(final DynamicTapForm newServer, final String tapServiceUrl) {
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TupdateTableMetadata: "+tapServiceUrl);
					currentT.setPriority(Thread.NORM_PRIORITY-2);
					try {
					   SavotResource resultsResource = getResults("gettableInfos",tapServiceUrl, GETTAPSCHEMATABLES, PATHSYNC);
					   populateTables(newServer.tapClient, resultsResource);
					   //update info panel
					   Future<JPanel> infoPanel = createMetaInfoDisplay(tapServiceUrl, newServer.tapClient.tablesMetaData);
					   if (infoPanel != null) {
					      newServer.tapClient.tackleFrameInfoServerUpdate(aladin, newServer, infoPanel);
					   }

					} catch (Exception e) {
					   
					   // PF Jan 2022 - 2e essai en forçant le format pour contourner le bug de SAVOT sur le base64
					   try {
	                       SavotResource resultsResource = getResults("gettableInfos",tapServiceUrl, FORMAT+GETTAPSCHEMATABLES, PATHSYNC);
	                       populateTables(newServer.tapClient, resultsResource);
	                       //update info panel
	                       Future<JPanel> infoPanel = createMetaInfoDisplay(tapServiceUrl, newServer.tapClient.tablesMetaData);
	                       if (infoPanel != null) {
	                          newServer.tapClient.tackleFrameInfoServerUpdate(aladin, newServer, infoPanel);
	                       }
					      
					   } catch( Exception e1 ) {

					   // TODO Auto-generated catch block
					   if (Aladin.levelTrace >=3 ) {//we do not bother here if all table description is not obtained. 
					      //if there is problem obtaining essential metadata then there will be error actions in the main loadTapColumnSchemas thread
					      e1.printStackTrace();
					   }
					   }
					} finally {
					   currentT.setName(oldTName);
					}
				}
			});
		} catch (RejectedExecutionException e) {
			Aladin.trace(3, "RejectedExecutionException. Unable to update table metadata.."+tapServiceUrl);
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
 			result = aladin.executor.submit(new Callable<JPanel>() {
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
 							
 							allRows = new Vector<>();
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
			Aladin.trace(3, "RejectedExecutionException. Unable to create the metainfo display for.."+tapServiceUrl);
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
	 * @param queryString 
	 * @param serverExamples 
	 * @param url 
	 * @param string 
	 * @throws Exception 
	 */
	public void fireSync(final Server server, final String url, final String queryString,
			final Map<String, Object> postParams) throws Exception {
		// TODO Auto-generated method stub
		final int requestNumber = server.newRequestCreation();
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					try {
						if (postParams == null || postParams.isEmpty()) {
							String queryParam = GETRESULTPARAMS + URLEncoder.encode(queryString, UTF8);
							URL syncUrl = getUrl(url, queryParam, PATHSYNC);
//									new URL(String.format(SYNCGETRESULT, url, queryParam));
							currentT.setName("TsubmitSync: " + syncUrl);
							//TODO:: check for the query status before loading: and remove return- covert to execute
							/*
							 * one INFO element with name=\94QUERY_STATUS\94 and value=\94OK\94 or
							value=\94ERROR\94 must be included before the TABLE. If the TABLE does not
							contain the entire query result, one INFO element with value=\94OVERFLOW\94 or
							value=\94ERROR\94 must be included after the table.*/
							
							handleResponse(aladin, syncUrl, null, server.tapClient.tapLabel, server, queryString,
									requestNumber);
							
						} else {//currently this else part is used only for upload.
							URL syncUrl = getUrl(url, null, PATHSYNC);
							currentT.setName("TsubmitSync: " + syncUrl);
							MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
							String boundary = MultiPartPostOutputStream.createBoundary();

							URLConnection urlConn = MultiPartPostOutputStream.createConnection(syncUrl);
							urlConn.setRequestProperty("Accept", "*/*");
							urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
							// set some other request headers...
							urlConn.setRequestProperty("Connection", "Keep-Alive");
							HttpURLConnection http = (HttpURLConnection)urlConn;
							http.setRequestProperty("http.agent", "Aladin/"+Aladin.VERSION);
					        http.setRequestProperty("Accept-Encoding", "gzip");
							
							MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

							// standard request parameters
							out.writeField("REQUEST", "doQuery");
                            out.writeField("LANG", "ADQL");
                            out.writeField("MAXREC", MAXTAPROWS+"");
//							out.writeField("request", "doQuery");
//							out.writeField("lang", "ADQL");
//							out.writeField("version", "1.0");
//							out.writeField("format", "votable");

							/*if (parserObj != null) {
								int limit = parserObj.getSelect().getLimit();
								if (limit > 0) {
									out.writeField("MAXREC", String.valueOf(limit));
								}
							}*/
							
							out.writeField("QUERY", queryString);

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
							handleResponse(aladin, syncUrl, urlConn, server.tapClient.tapLabel, server,
									queryString, requestNumber);
						}

					} catch (MalformedURLException e) {
						if (Aladin.levelTrace >= 3) e.printStackTrace();
						displayWarning(server, requestNumber, e.getMessage());
					} catch (UnknownHostException e) {
						if( Aladin.levelTrace >= 3 ) e.printStackTrace();
						displayWarning(server, requestNumber, "Cannot contact server! Please try again later"+e.getMessage());
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
			displayWarning(server, requestNumber, "Unable to submit: "+queryString+"\n Request overload! Please wait and try again.");
		} 
	}
	
	/**
	 Convenience method
	 * - creates plane for successful response
	 * - shows error otherwise 
	 * @param aladin
	 * @param url
	 * @param planeLabel
	 * @param server
	 * @param query - to display on plane properties
	 * @param requestNumber - for status updates on Ball.java
	 * @throws Exception
	 */
//	public static void handleResponse(Aladin aladin, URL url, String planeLabel, Server server, String query,
//			int requestNumber) throws Exception {
//		MyInputStream is = null;
//		try {
//			is = Util.openStreamForTap(url, null, true, TIMEOUT);
//			if (requestNumber == -1 || server.requestsSent != requestNumber) {
//				server = null;
//			} else {
//				server.disableStatusForAllPlanes();
//			}
//			aladin.calque.newPlanCatalog(is, planeLabel, server, query, requestNumber);
//			// stream is closed downstream when it is not an error scenario
//		} catch (Exception e) {
//			Aladin.trace(3, "Error when getting job!" + e.getMessage());
//			if (is != null) {
//				is.close();
//			}
//			throw e;
//		}
//	}
	
/*	public static void handleResponse1(Aladin aladin, URL url, URLConnection conn, String planeLabel,
			Server server, String query, int requestNumber) throws Exception {
		MyInputStream is = null;
		try {
			Aladin.trace(3, "trying url : "+url);
			is = Util.openStreamForTap(url, conn, true, TIMEOUT);
			if (requestNumber == -1 || server.requestsSent != requestNumber) {
				server = null;
			} else {
				server.disableStatusForAllPlanes();
			}
			aladin.calque.newPlanCatalog(is, planeLabel, server, query, requestNumber);
			// stream is closed downstream when it is not an error scenario
		} catch (Exception e) {
			Aladin.trace(3, "Error when getting job!" + e.getMessage());
			if (is != null) {
				is.close();
			}
			throw e;
		}
	}*/
	
	public static void handleResponse(Aladin aladin, URL url, URLConnection conn, String planeLabel,
			Server server, String query, int requestNumber) throws Exception {
		InputStream is = null;
		try {
			Aladin.trace(3, "trying url : "+url);
			is = Util.openStreamForTapAndDL(url, conn, true, TIMEOUT);
			if (requestNumber == -1 || server.requestsSent != requestNumber) {
				server = null;
			} else {
				server.disableStatusForAllPlanes();
			}
			String institute = null;
			if (server != null) {
				institute = server.institute;
			}
			aladin.calque.createPlan(is, planeLabel, institute, server, url, query, requestNumber);
		} catch (Exception e) {
			Aladin.trace(3, "Error when getting job!" + e.getMessage());
			throw e;
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
			Aladin.error(s, e.getMessage());
		}
		return result;
	}
	
	/**
	 * Method to :
	 * <ol>	<li>Load query</li>
	 * 		<li>Query tap server asynchronously</li>
	 * 		<li>Populate results on Aladin</li>
	 * </ol>
	 * @param server
	 * @param url
	 * @param queryString
	 * @param postParams
	 * @throws Exception
	 */
	public void fireASync(final Server server, final String url, final String queryString,
			final Map<String, Object> postParams) throws Exception {
		final int requestNumber = server.newRequestCreation();
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TsubmitAsync: " + server.tapClient.tapBaseUrl);
					try {
						uwsFacade.showAsyncPanel();
						Map<String, Object> requestParams = new HashMap<>();
						requestParams.put("REQUEST", "doQuery");
						requestParams.put("LANG", "ADQL");
						requestParams.put("MAXREC", MAXTAPROWS+"");
						requestParams.put("QUERY", queryString);
						if (postParams != null && !postParams.isEmpty()) {
							requestParams.putAll(postParams);
						}
						URL requestUrl = TapManager.getUrl(url, null, PATHASYNC); 
						uwsFacade.handleJob(server, server.tapClient.tapLabel, requestUrl, queryString, requestParams, true, requestNumber);
						currentT.setName(oldTName);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						if (Aladin.levelTrace >= 3 )  e.printStackTrace();
						displayWarning(server, requestNumber,  Aladin.getChaine().getString("GENERICERROR"));
					}
				}
			});
		} catch (RejectedExecutionException ex) {
			displayWarning(server, requestNumber, "Unable to submit: "+queryString+"\n Request overload! Please wait and try again.");
		} 
	}
	
	
	public void setRaDecForTapServer(final ServerTap serverTap, final String selectedTableName) {
		try {
			aladin.executor.execute(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Thread currentT = Thread.currentThread();
					final String oldTName = currentT.getName();
					currentT.setName("TsetRaDec: "+serverTap.tapClient.tapBaseUrl);
					
					boolean addTargetPanel = false;
					TapTable selectedTable = serverTap.tapClient.tablesMetaData.get(selectedTableName);
					Vector<TapTableColumn> raColumnModel = new Vector<>();
					raColumnModel.addAll(ServerTap.getPotentialRaOrDecColumns(selectedTable.getColumns()));
					JComboBox raColumn = new JComboBox(raColumnModel);
					raColumn.setRenderer(new CustomListCellRenderer());
					TapTableColumn selectColumn = serverTap.getDefaultRa();
					if (selectColumn != null) {
						raColumn.setSelectedItem(selectColumn);
						selectColumn = null;
					}
					raColumn.setSize(raColumn.getWidth(), Server.HAUT);
					
					Vector<TapTableColumn> decColumnModel = new Vector<>();
					decColumnModel = new Vector<>();
					decColumnModel.addAll(raColumnModel);
					JComboBox decColumn = new JComboBox(decColumnModel);
					decColumn.setRenderer(new CustomListCellRenderer());
					selectColumn = serverTap.getDefaultDec();
					if (selectColumn != null) {
						decColumn.setSelectedItem(selectColumn);
					}
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
						
						TapTableColumn selected = ((TapTableColumn) raColumn.getSelectedItem());
						String selectedColumnName = null;
						if (selectedTable.alias != null) {
							selectedColumnName = selectedTable.alias + DOT_CHAR + selected.getColumn_name();
						} else {
							selectedColumnName = selected.getColumn_name();
						}
						serverTap.setRaColumnName(selectedColumnName);
						
						selected = ((TapTableColumn) decColumn.getSelectedItem());
						selectedColumnName = null;
						if (selectedTable.alias != null) {
							selectedColumnName = selectedTable.alias + DOT_CHAR + selected.getColumn_name();
						} else {
							selectedColumnName = selected.getColumn_name();
						}
						serverTap.setDecColumnName(selectedColumnName);
						
						if (addTargetPanel) {
//							setWhereAddConstraintsGui(columnNames);
//							this.queryComponentsGui.revalidate();
//							this.queryComponentsGui.repaint();
							if (serverTap.target == null) {
								serverTap.targetPanel = new JPanel();
								serverTap.createTargetPanel(serverTap.targetPanel);
							}
							serverTap.targetPanel.setVisible(true);
							serverTap.queryComponentsGui.revalidate();
							serverTap.queryComponentsGui.repaint();
						}
						serverTap.writeQuery();
//						if (serverTap.tapClient.mode == TapClientMode.UPLOAD) {
//							//trigger changes for ones using the table already
//							serverTap.tablesGui.actionPerformed(null);
//						}
					}
					if (serverTap.joinPanel != null) {
						serverTap.joinPanel.updatePositionParams();
					}
					currentT.setName(oldTName);
				}
			});
		} catch (RejectedExecutionException e) {
			displayWarning(serverTap.tapClient, "Request overload! Please wait and try again.");
		}
	}
	
	public boolean	showOnJoinFrame(String label, String selectedTableName, JoinFacade panel) {
		boolean showFirstTimeInfo = false;
		if (this.joinFrame == null) {
			this.joinFrame = new FrameSimple(aladin);
			showFirstTimeInfo = true;
		}
		String displayTitle = "";
		try {
			displayTitle = String.format(JoinFacade.JOINFRAMETITLE, selectedTableName, label);
		} catch (Exception e) {
			// TODO: handle exception
			displayTitle = "Create simple join constraints with "+selectedTableName+" of "+label;
		}
		this.joinFrame.show(panel, displayTitle);
		return showFirstTimeInfo;
		
	}
	
	/**
	 * set current plan in all tap guis by default of what is chosen on plan stack.
	 * or when new plane gets loaded.
	 * @param currentSelectedPlanName
	 */
	public void setCurrentUploadPlane(String currentSelectedPlanName) {
		if (this.uploadFacade != null && currentSelectedPlanName != null && !currentSelectedPlanName.isEmpty()) {
			String valueToSelect = this.uploadFacade.getUploadTableName(currentSelectedPlanName);
			if (valueToSelect != null) {
				uploadTablesModel.setSelectedItem(valueToSelect);
			}
		}
	}
	
	public boolean checkIsUploadablePlaneByLabel(String planLabel) {
		boolean result = false;
		if (this.uploadFacade.uploadTableNameDict.containsKey(planLabel)) {
			result = true;
		}
		return result;
	}
	
	public void closeMyJoinFacade(JoinFacade joinPanel) {
		// TODO Auto-generated method stub
		if (this.joinFrame != null && this.joinFrame.isVisible()) {
			if ((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, joinPanel) == this.joinFrame) {
				this.joinFrame.setVisible(false);
			}
		}
	}
	
	public void clearJoinPanel() {
		// TODO Auto-generated method stub
		if (this.joinFrame != null) {
			if (this.joinFrame.isShowing()) {
				this.joinFrame.setVisible(false);
			}
			this.joinFrame.dispose();
		}
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
		int whichList = this.tapFrameServer.options.getSelectedIndex();
		if (whichList == 0) {
			int index = this.tapFrameServer.preselectedServersTable.getSelectedRow();
			if (index >= 0) {
				this.tapFrameServer.selectedServerLabel = splTapServerLabels.get(index);
			} else {
				this.tapFrameServer.selectedServerLabel = null;
			}
		} else if (whichList == 1) {
			int index = this.tapFrameServer.allServersTable.getSelectedRow();
			if (index >= 0) {
				Vector<String> dataLabel = this.tapFrameServer.allServersDataLabelTable.getDataLabelAt(index);
				this.tapFrameServer.selectedServerLabel = dataLabel;
			} else {
				this.tapFrameServer.selectedServerLabel = null;
			}
		}
//		for (DataLabel datalabel : instantiateTapServers(whichList)) {
//			if (datalabel.gui.isSelected()) {
//				this.loadTapServerList();
//				this.tapFrameServer.selectedServerLabel = datalabel;
//				break;
//			}
//		}
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
	
	public ServerTapExamples getNewServerTapExamplesInstance(TapClient tapClient, boolean isFullServerCapability) {
		// TODO Auto-generated method stub
		ServerTapExamples newServer = new ServerTapExamples(aladin);
		initNewServerTap(newServer, tapClient, isFullServerCapability);
		return newServer;
	}
	
	public void initNewServerTap(DynamicTapForm newServer, TapClient tapClient, boolean isFullServerCapability) {
		// TODO Auto-generated method stub
		newServer.setName(tapClient.tapLabel);
		newServer.tapClient = tapClient;
		newServer.setOpaque(true);
		newServer.isFullServer = isFullServerCapability;
		newServer.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
	}
	
	public ServerTap getNewServerTapInstance(TapClient tapClient, boolean isFullServerCapability) {
		// TODO Auto-generated method stub
		ServerTap newServer = new ServerTap(aladin);//final resort is to create generic form ServerTap
		initNewServerTap(newServer, tapClient, isFullServerCapability);
		return newServer;
	}
	
	public ServerObsTap getNewServerObsTapInstance(TapClient tapClient) {
		// TODO Auto-generated method stub
		ServerObsTap newServer = new ServerObsTap(aladin);
		initNewServerTap(newServer, tapClient, true);
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
	
	public void displayWarning(TapClient tapClient, String message) {
		if (tapClient.mode == TapClientMode.TREEPANEL) {
			Aladin.error(this.tapPanelFromTree, message);
		} else {
			Aladin.error(aladin.dialog, message);
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
	
	/*public static void main(String[] args) {
		DefaultDBTable defaultDBTable = new DefaultDBTable("TAP_SCHEMA.TAP_SCHEMA.TAP_SCHEMA.schemas");
		System.err.println(defaultDBTable.getADQLCatalogName());
		System.err.println(defaultDBTable.getADQLSchemaName());
		System.err.println(defaultDBTable.getADQLName());
		System.err.println(getFullyQualifiedTableName(defaultDBTable));
	}*/
	
	
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
	
	/**
	 * Adds newly loaded plancatalogue into upload options
	 * @param newPlan
	 */
	public void updateAddUploadPlans(Plan newPlan) {
		if (this.uploadFacade != null && this.uploadTablesModel != null) {
			if (/*newPlan.flagOk && */newPlan.pcat != null && newPlan instanceof PlanCatalog && newPlan.pcat.flagVOTable) {
				try {
					this.uploadFacade.allowPlanIntoUploadFacade(newPlan);
					PlanCatalog planCatalog = (PlanCatalog) newPlan;
					this.uploadFacade.updateUploadGuiWithNewUpload(planCatalog);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if(Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.trace(3, "Unable to parse " + newPlan.label + " data for upload");
				}
			}
		}
	}
	
	public void updateDeleteUploadPlans(Plan planInDeletion) {
		if (this.uploadFacade != null) {
			this.uploadFacade.deleteAvailableUploadTable(planInDeletion);
		}
	}
	
	public void reAddPlan(Plan plan) {
		updateDeleteUploadPlans(plan);
		updateAddUploadPlans(plan);
		if (this.joinFrame != null) {
			Aladin.info(this.joinFrame, UPLOADTABLECHANGEWARNING);
		}
	}
	
	public UploadFacade initUploadFrame() {
		if (this.uploadFacade == null) {
			this.uploadFacade = new UploadFacade(this.aladin);
		}
		return this.uploadFacade;
	}
	
	public Map<String, TapTable> initUploadFrameAndGetUploadedTables() {
		initUploadFrame();
		return this.uploadFacade.uploadTablesMetaData;
	}
	
	public Map<String, TapTable> getUploadedTables() {
		Map<String, TapTable> results = null; 
		if (this.uploadFacade != null && this.uploadFacade.uploadTablesMetaData != null) {
			results = this.uploadFacade.uploadTablesMetaData;
		}
		return results;
	}
	
	public ComboBoxModel getUploadClientModel() {
		// TODO Auto-generated method stub
		return uploadTablesModel;
	}

}
