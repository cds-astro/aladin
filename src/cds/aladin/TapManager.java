package cds.aladin;

import static cds.aladin.Constants.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import adql.query.ADQLQuery;
import cds.savot.binary.DataBinaryReader;
import cds.savot.model.FieldSet;
import cds.savot.model.SavotBinary;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.MultiPartPostOutputStream;
import cds.tools.TwoColorJTable;
import cds.tools.Util;
import cds.xml.Field;
import cds.xml.VOSICapabilitiesReader;

public class TapManager {
	
	private static TapManager instance = null;
	ExecutorService executor;
	
	public Aladin aladin;
	protected static List<DataLabel> tapServerLabels;
	protected TapFrameServer tapFrameServer=null;
	protected DataLabel selectedServerLabel;
	protected FrameUploadServer uploadFrame;
	
	protected static Map<String, Server> tapServerPanelCache = new HashMap<String, Server>();
	public static final String GETTAPSCHEMACOLUMNCOUNT = "/sync?REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+COUNT(*)+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMACOLUMNS = "/sync?REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.columns";
	public static final String GETTAPSCHEMATABLES = "/sync?REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.tables";
	public static final String GETTAPSCHEMACOLUMN = "/sync?REQUEST=doQuery&LANG=ADQL&QUERY=SELECT+*+FROM+TAP_SCHEMA.columns+where+table_name+=+'%1$s'+";
	public static final String GETTAPCAPABILITIES = "/capabilities";
	
	public static final int MAXTAPCOLUMNDOWNLOADVOLUME = 1000;//limit for the table columns that will be downloaded together. 409 cadc;1000 decided limit
	
	protected List<String> eligibleUploadServers;//URls of servers loaded and allow uploads
	
	public UWSFacade uwsFacade;
	
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
	
	public List<DataLabel> getTapServerList(){
		if (tapServerLabels==null || tapServerLabels.isEmpty()) {
			populateTapServerList();
		}
		return tapServerLabels;
	}
	
	/**
	 * Method to populate all the tap servers in the configuration file into a tap frame server
	 */
	private void populateTapServerList() {
		BufferedReader bufReader = null;
		try {
			tapServerLabels = new ArrayList<DataLabel>();
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
				tapServerLabels.add(new DataLabel(label, url, description));
			}
			
		} catch (Exception e) {
			System.err.println("TapServer.txt not loaded error:"+e);
            e.printStackTrace();
			Aladin.warning("TapServer.txt not loaded !", 1);
		} finally {
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
	 * Loads the tapquery gui for the selected tap server
	 * - either loads cached gui
	 * - creates a new gui
	 * - or just loads the registry incase no tapserver is selected
	 * @return whether the query panel is loaded or not
	 */
	public boolean loadTapServer(){
		//either get exixitng details form the configuration file or call url to get tap server details.
		boolean result = false;
		
		if (this.getSelectedServerLabel() == null) {//not necessary condition. But adding just in case.
			this.showTapRegistryForm();
			result = false;
		} else if (tapServerPanelCache.containsKey(this.selectedServerLabel.getValue())) {
			Server cachedCopy = tapServerPanelCache.get(this.selectedServerLabel.getValue());
			aladin.dialog.findReplaceServer(aladin.dialog.tapServer, cachedCopy);
			aladin.dialog.tapServer = cachedCopy;
			result = true;
		} else {
			ServerTap newServer = new ServerTap(aladin);
			newServer.setName(this.selectedServerLabel.getLabel());
			String url = this.selectedServerLabel.getValue();
			newServer.setUrl(url);
			newServer.showloading();
			tapServerPanelCache.put(url, newServer);
			aladin.dialog.findReplaceServer(aladin.dialog.tapServer, newServer);
			aladin.dialog.tapServer = newServer;
			newServer.capabilities = this.getTapCapabilities(url);
			this.loadTapColumnSchemas(url);
			newServer.setOpaque(true);
			tapFrameServer.setReload(url);
			result = true;
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
		if (server instanceof ServerTap && ((ServerTap)server).dummyInstantiation) {
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
		Future<VOSICapabilitiesReader> capabilities = executor.submit(new Callable<VOSICapabilitiesReader>() {
			@Override
			public VOSICapabilitiesReader call() {
				// TODO Auto-generated method stub
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				VOSICapabilitiesReader vosiCapabilitiesReader = null;
				currentT.setName("TgetCapabilities: "+tapServiceUrl);
				try {
					URL capabilitiesUrl = new URL(tapServiceUrl+GETTAPCAPABILITIES);
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
		return capabilities;
	}
	
	/**
	 * Method to populate tap meta data
	 * @param tapServiceUrl
	 */
	public void loadTapColumnSchemas(final String tapServiceUrl){
		executor.execute(new Runnable(){
			@Override
			public void run() {
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				currentT.setName("TgetMetaInfo: "+tapServiceUrl);
				ServerTap serverToLoad = (ServerTap) tapServerPanelCache.get(tapServiceUrl);
				try {
					String volume = getFutureResultsVolume(tapServiceUrl);
					if (volume != null) {
						serverToLoad.setData(new HashMap<String,TapTable>());
						int count = Integer.parseInt(volume);
						if (count > MAXTAPCOLUMNDOWNLOADVOLUME) {
							//download only table names and first table's columns
							SavotResource resultsResource = getResults(tapServiceUrl+GETTAPSCHEMATABLES);
							populateTables(serverToLoad, resultsResource);
							String defaultTable = serverToLoad.tablesMetaData.keySet().iterator().next();
							String tableNameQueryParam = defaultTable;
							tableNameQueryParam = URLEncoder.encode(tableNameQueryParam, UTF8);
							
							//get single table data and populate it to front end
							String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, defaultTable);
							SavotResource columnResults = getResults(tapServiceUrl+gettablesColumnsQuery);
							populateColumns(serverToLoad, columnResults);
						} else if (count > 0) {
							// download all
							SavotResource resultsResource = getResults(tapServiceUrl+GETTAPSCHEMACOLUMNS);
							populateColumns(serverToLoad, resultsResource);
						} else {
							serverToLoad.showLoadingError();
							Aladin.warning(aladin.dialog, "Error from tap server "+serverToLoad.getName()+" : unable to get metadata !");
							return;
						}
						serverToLoad.createFormDefault(); //default choice is the first table
						serverToLoad.revalidate();
						serverToLoad.repaint();
						serverToLoad.infoPanel = createMetaInfoDisplay(tapServiceUrl, serverToLoad.tablesMetaData);
					} else {
						serverToLoad.showLoadingError();
						Aladin.warning(aladin.dialog, "Error from tap server "+serverToLoad.getName()+" : unable to get metadata !");
					}
				} catch (Exception e) {
					if (Aladin.levelTrace >= 3) 
						e.printStackTrace();
					serverToLoad.showLoadingError();
					Aladin.warning(aladin.dialog, "Error from tap server "+serverToLoad.getName()+" : unable to get metadata !");
				} finally {
					serverToLoad.revalidate();
					serverToLoad.repaint();
					currentT.setName(oldTName);
				}
			}
		});
	}
	
	/**
	 * creates the first form to load the tap servers from the registry file
	 */
	public void showTapRegistryForm() {
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
	 */
	public void showAsyncPanel() {
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
	
	/**
	 * Method updates the tap metadata and calls for a metadata gui update.
	 * @param tableName
	 * @param serverTap
	 * @return
	 * @throws Exception
	 */
	public Vector<TapTableColumn> updateTableColumnSchemas(String tableName, ServerTap serverTap) throws Exception {
		Vector<TapTableColumn> tableColumnMetaData = null;
		String tapServiceUrl = serverTap.getUrl();
		Future<Vector<TapTableColumn>> futureResult = updateTableColumnSchemas(tapServiceUrl, tableName);
		try {
			Aladin.trace(3, futureResult+ " is the serverTap");
			tableColumnMetaData = futureResult.get();
			if (tableColumnMetaData == null) {
				throw new Exception("Error from tap server: unable to get metadata !");
			}
			 //update info panel
			serverTap.tackleFrameInfoServerUpdate(this.createMetaInfoDisplay(tapServiceUrl, serverTap.tablesMetaData));
			Aladin.trace(3,"done updating tap info fo : "+tableName+" server: "+serverTap.getUrl());
		} catch (Exception e) {
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			throw e;
		}
		return tableColumnMetaData;
	}
	
	/**
	 * Method to populate tap meta data of a particular table
	 * @param tapServiceUrl
	 * @return
	 * @throws Exception 
	 */
	public Future<Vector<TapTableColumn>> updateTableColumnSchemas(final String tapServiceUrl, final String tableName) throws Exception {
		final int refThreadPriority = Thread.currentThread().getPriority();
		Future<Vector<TapTableColumn>> tapResult = executor.submit(new  Callable<Vector<TapTableColumn>>(){
			@Override
			public Vector<TapTableColumn> call() throws Exception {
				String tableNamequeryParam = tableName;
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				currentT.setName("Tupdate: "+tableName+", from: "+tapServiceUrl);
				if (currentT.getPriority() < refThreadPriority-1) {//just asking for quicker execution without overstepping a lot
					currentT.setPriority(refThreadPriority-1);//TODO:: tintin i donno check what todo
				}
				
				ServerTap serverToLoad = (ServerTap) tapServerPanelCache.get(tapServiceUrl);
				try {
					tableNamequeryParam = URLEncoder.encode(tableName, UTF8);
					String gettablesColumnsQuery = String.format(GETTAPSCHEMACOLUMN, tableNamequeryParam);
					SavotResource columnResults = getResults(tapServiceUrl+gettablesColumnsQuery);
					populateColumns(serverToLoad, columnResults);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw e;
				} finally {
					currentT.setName(oldTName);
				}
				return serverToLoad.tablesMetaData.get(tableName).getColumns();
			}
		});
		return tapResult;
	}
	
	/**
	 * Makes small notification flags to user. The initial text set is erased after a small time delay.
	 * Usefull to flag user when any gui model updates are made.
	 * @param notificationBar
	 * @param message
	 */
	public void eraseNotification(final JLabel notificationBar) {
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
					notificationBar.setText(EMPTYSTRING);
					notificationBar.revalidate();
					notificationBar.repaint();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					currentT.setName(oldTName);
				}
			}
		});
	}
	
	/**
	 * Method populates loads all tables from TAP_SCHEMA.tables
	 * @param newServer 
	 * @param resultsResource
	 * @throws Exception 
	 */
	public void populateTables(ServerTap newServer, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableReader(newServer, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
				case 1:
					binaryTableReader(newServer, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
					break;
				default:
					System.err.println("ERROR in populateTables! Did not read table data for "+newServer.getUrl());
					throw new Exception("ERROR in populateTables! Did not read table data"+newServer.getUrl());
				}
			}
		}
	}
	
	/**
	 * Tap Table Reader for Savot table Resource
	 * @param newServer
	 * @param tableRows
	 * @param fieldSet
	 */
	protected void tableReader(ServerTap newServer, TRSet tableRows, FieldSet fieldSet) {
		TapTable table = null;
		if (fieldSet != null && tableRows != null) {
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
					synchronized (newServer.tablesMetaData) {
						newServer.tablesMetaData.put(tableName, table);
					}
				}
			}
		}
	}
	
	/**
	 * Tap Table Reader for Savot binary resource
	 * @param newServer
	 * @param binaryData
	 * @param fields
	 * @throws IOException
	 */
	protected void binaryTableReader(ServerTap newServer, SavotBinary binaryData, FieldSet fields) throws IOException {
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
					synchronized (newServer.tablesMetaData) {
						newServer.tablesMetaData.put(tableName, table);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("ERROR in binaryTableReader! Did not read table data for " + newServer.getUrl());
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
	public void populateColumns(ServerTap serverToLoad, SavotResource resultsResource) throws Exception {
		if (resultsResource != null) {
			for (int i = 0; i < resultsResource.getTableCount(); i++) {
				int type = getType(0, resultsResource);
				switch (type) {
				case 0:
					tableColumnReader(serverToLoad, resultsResource.getTRSet(i), resultsResource.getFieldSet(i));
					break;
				case 1:
					binaryColumnReader(serverToLoad, resultsResource.getData(i).getBinary(), resultsResource.getFieldSet(i));
					break;
				default:
					System.err.println("ERROR in populateColumns! Did not read table column data for "+serverToLoad.getUrl());
					throw new Exception("ERROR in populateColumns! Did not read table column data for "+serverToLoad.getUrl());
				}
			}
		}
	}
	
	/**
	 * Method populates column information from savot resource 
	 * @param serverToLoad
	 * @param tableRows
	 * @param fieldSet
	 */
	protected void tableColumnReader(ServerTap serverToLoad, TRSet tableRows, FieldSet fieldSet) {
		TapTable table = null;
		TapTableColumn tableColumn = null;
		Vector<TapTableColumn> tableColumns = null;
		String columnName = null;
		String raColumnName = null;
		String decColumnName = null;
		if (fieldSet != null && tableRows != null) {
			for (int j = 0; j < tableRows.getItemCount(); j++) {
				tableColumn = new TapTableColumn();
				TDSet theTDs = tableRows.getTDSet(j);
				String tableName = null;
				for (int k = 0; k < theTDs.getItemCount(); k++) {
					SavotField field = fieldSet.getItemAt(k);
					String fieldName = field.getName();
					if (fieldName != null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName = theTDs.getContent(k);
							tableColumn.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(COLUMNNAME)) {
							columnName = theTDs.getContent(k);
							tableColumn.setColumn_name(columnName);
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							tableColumn.setDescription(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(UNIT)) {
							tableColumn.setUnit(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(UCD)) {
							String ucd = theTDs.getContent(k);
							tableColumn.setUcd(ucd);
							if (raColumnName == null
									&& (ucd.contains(UCD_RA_PATTERN2) || ucd.equalsIgnoreCase(UCD_RA_PATTERN3))) {
								raColumnName = columnName;
							}
							if (decColumnName == null
									&& (ucd.contains(UCD_DEC_PATTERN2) || ucd.equalsIgnoreCase(UCD_DEC_PATTERN3))) {
								decColumnName = columnName;
							}
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							tableColumn.setUtype(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(DATATYPE)) {
							tableColumn.setDatatype(theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(SIZE)) {
							tableColumn.setSize(field.getDataType(), theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(PRINCIPAL)) {
							tableColumn.setPrincipal(field.getDataType(), theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(INDEXED)) {
							tableColumn.setIndexed(field.getDataType(), theTDs.getContent(k));
						} else if (fieldName.equalsIgnoreCase(STD)) {
							tableColumn.setStandard(field.getDataType(), theTDs.getContent(k));
						}
					}
				}
				if (tableName != null) {
					synchronized (serverToLoad.tablesMetaData) {
						if (serverToLoad.tablesMetaData.containsKey(tableName)) {
							table = serverToLoad.tablesMetaData.get(tableColumn.getTable_name());
							tableColumns = table.getColumns();
							if (tableColumns == null) {
								tableColumns = new Vector<TapTableColumn>();
								table.setColumns(tableColumns);
							}
						} else {
							tableColumns = new Vector<TapTableColumn>();
							table = new TapTable();
							table.setTable_name(tableColumn.getTable_name());
							table.setColumns(tableColumns);
							serverToLoad.tablesMetaData.put(tableColumn.getTable_name(), table);
						}
					}
				}
				if (raColumnName != null) {
					table.setRaColumnName(raColumnName);
					raColumnName = null;
				}
				if (decColumnName != null) {
					table.setDecColumnName(decColumnName);
					decColumnName = null;
				}
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
	protected void binaryColumnReader(ServerTap serverToLoad, SavotBinary binaryData, FieldSet fields) throws IOException {
		TapTable table = null; 
		TapTableColumn tableColumn = null; 
		Vector<TapTableColumn> tableColumns = null;
		String columnName = null;
		String raColumnName = null;
		String decColumnName = null;
		DataBinaryReader parser = null;
		try {
			parser = new DataBinaryReader(binaryData.getStream(), fields);
			while (parser.next()) {
				table = new TapTable();
				tableColumn = new TapTableColumn();
				String tableName = null;
                for (int j = 0; j < fields.getItemCount(); j++) {
                    SavotField field = fields.getItemAt(j);
					String fieldName = field.getName();
					if (fieldName!=null && !fieldName.isEmpty()) {
						if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName= parser.getCellAsString(j);
							tableColumn.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(COLUMNNAME)) {
							columnName = parser.getCellAsString(j);
							tableColumn.setColumn_name(columnName);
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							tableColumn.setDescription(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(UNIT)) {
							tableColumn.setUnit(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(UCD)) {
							String ucd = parser.getCellAsString(j);
							tableColumn.setUcd(ucd);
							if (raColumnName == null && (ucd.startsWith(UCD_RA_PATTERN2)
									|| ucd.equalsIgnoreCase(UCD_RA_PATTERN3))) {
								raColumnName = columnName;
							}
							if (decColumnName == null && (ucd.startsWith(UCD_DEC_PATTERN2)
									|| ucd.equalsIgnoreCase(UCD_DEC_PATTERN3))) {
								decColumnName = columnName;
							}
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							tableColumn.setUtype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(DATATYPE)) {
							tableColumn.setDatatype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(SIZE)) {
							tableColumn.setSize(field.getDataType(), parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(PRINCIPAL)) {
							tableColumn.setPrincipal(field.getDataType(), parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(INDEXED)) {
							tableColumn.setIndexed(field.getDataType(), parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(STD)) {
							tableColumn.setStandard(field.getDataType(), parser.getCellAsString(j));
						}
						
						if (fieldName.equalsIgnoreCase(TABLETYPE)) {
							table.setTable_type(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(SCHEMANAME)) {
							table.setSchema_name(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(TABLENAME)) {
							tableName= parser.getCellAsString(j);
							table.setTable_name(tableName);
						} else if (fieldName.equalsIgnoreCase(UTYPE)) {
							table.setUtype(parser.getCellAsString(j));
						} else if (fieldName.equalsIgnoreCase(DESCRIPTION)) {
							table.setDescription(parser.getCellAsString(j));
						}
					}
                }
                if (tableName!=null) {
					synchronized (serverToLoad.tablesMetaData) {
						if (serverToLoad.tablesMetaData.containsKey(tableName)) {
							table = serverToLoad.tablesMetaData.get(tableColumn.getTable_name());
							tableColumns = table.getColumns();
							if (tableColumns==null) {
								tableColumns = new Vector<TapTableColumn>();
								table.setColumns(tableColumns);
							}
						} else {
							tableColumns = new Vector<TapTableColumn>();
							table = new TapTable();
							table.setTable_name(tableColumn.getTable_name());
							table.setColumns(tableColumns);
							serverToLoad.tablesMetaData.put(tableColumn.getTable_name(), table);
						}
					}
				}
				if (raColumnName!=null && table.getRaColumnName()==null) {// second condition:: set only the first one
					table.setRaColumnName(raColumnName);
					raColumnName = null;
				}
				if (decColumnName!=null && table.getDecColumnName()==null) {
					table.setDecColumnName(decColumnName);
					decColumnName = null;
				}
				tableColumns.add(tableColumn);
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
			System.err.println("ERROR in binaryColumnReader! Did not read column column data for "+serverToLoad.getUrl());
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
			tableColumns.add(tableColumn);
			if (f.isRa()) {
				raName = f.name;
			}
			if (f.isDe()) {
				decName = f.name;
			}
			
		}
		
		table = new TapTable();
		table.setTable_name(tableName);
		table.setColumns(tableColumns);
		table.setRaColumnName(raName);
		table.setDecColumnName(decName);
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
	public static SavotResource getResults(String tapServiceUrl) throws MalformedURLException, Exception {
		SavotResource resultsResource = null;
		try {
			URL url = new URL(tapServiceUrl);// change to just url tintin TODO
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
					tableName = uploadFrame.generateUploadTableName();
				}
				if (uploadFrame.uploadServer.tablesMetaData.isEmpty()) {
					firstUpload = true;
					uploadFrame.uploadServer.showloading();
				}
//				uploadServer.tablesGui.invalidate();
				populateColumnsFromPlan(planToLoad, tableName , uploadFrame.uploadServer.tablesMetaData);
//				uploadFrame.setUploadFileForm();
				if (firstUpload) {
					uploadFrame.createUploadServer();
				}
				if (uploadFrame.uploadServer.isNotLoaded) {
					uploadFrame.uploadServer.createFormDefault();
					uploadFrame.uploadServer.invalidate();
					
				}
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
		SavotResource resultsResource = getResults(tapServiceUrl+GETTAPSCHEMACOLUMNCOUNT);
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
					System.err.println("ERROR in populateTables! Did not read count. url:"+tapServiceUrl);
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
	 * @param paramName
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
			System.err.println("IOError.. Count not known");
			if (Aladin.levelTrace >= 3)	e.printStackTrace();
		}
		
		return count;
	}
	
	/**
	 * Method populates the info panel gui of the server
	 * @param tapServiceUrl
	 * @param tablesMetaData
	 * @return
	 */
	public Future<JPanel> createMetaInfoDisplay(final String tapServiceUrl, final Map<String, TapTable> tablesMetaData){
 		Future<JPanel> result = executor.submit(new Callable<JPanel>() {
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
				bagConstraints.weighty = 1;
				bagConstraints.fill = GridBagConstraints.HORIZONTAL;
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
						description = "Description:" + table.getDescription();
					}
					
					if (columnMetadata != null) {
						tableNameLabel = new JLabel("Table: "+tableName);
						tableNameLabel.setFont(Aladin.BOLD);
						//tableName.setBounds(x, y, width, 10);y+=10;
						bagConstraints.gridy++;
						bagConstraints.insets = new Insets(20, 2, 2, 2);
						gridbag.setConstraints(tableNameLabel, bagConstraints);
						infoPanel.add(tableNameLabel);
						if (!description.isEmpty()) {
							tableDescription = new JLabel(description);
							bagConstraints.gridy++;
							bagConstraints.insets = new Insets(2, 2, 2, 2);
							gridbag.setConstraints(tableDescription, bagConstraints);
							infoPanel.add(tableDescription);
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
						gridbag.setConstraints(scrollPane, bagConstraints);
						infoPanel.add(scrollPane);
					} else {
						recordInfoTables.append("\nTable: "+tableName);
						if (!description.isEmpty()) {
							recordInfoTables.append("\n").append(description);
								
						}
						recordInfoTables.append("\nNo column metadata available.\n");
//						noInfolabel = new JLabel("No column metadata available.");
//						noInfolabel.setFont(Aladin.ITALIC);
//						bagConstraints.gridy++;
//						bagConstraints.insets = new Insets(2, 2, 2, 2);
//						gridbag.setConstraints(noInfolabel, bagConstraints);
//						infoPanel.add(noInfolabel);
					}
				}
				if (recordInfoTables.length()>0) {
					JTextArea infoTablesDisplay = new JTextArea(20, 85);
					infoTablesDisplay.setText(recordInfoTables.toString());
					infoTablesDisplay.setFont(Aladin.COURIER);
					infoTablesDisplay.setBackground(Color.white);
					infoTablesDisplay.setEditable(false);
					scrollPane = new JScrollPane(infoTablesDisplay);
					bagConstraints.gridy++;
					bagConstraints.insets = new Insets(2, 2, 2, 2);
					gridbag.setConstraints(scrollPane, bagConstraints);
					infoPanel.add(scrollPane);
				}
//				FrameInfoServer frameInfoServer = new FrameInfoServer(aladin, infoPanel);
				currentT.setName(oldTName);
				return infoPanel;
			}
		});
		return result;
	}
	
	/**
	 *	Method to handle sync tap query submissions:
	 * <ol>	<li>Load query</li>
	 * 		<li>Query tap server synchronously</li>
	 * 		<li>Populate results(votable) on Aladin</li>
	 * </ol>
	 * if postParams are null then it executes GET, else this method executes a POST
	 * @param url 
	 * @param query
	 * @throws Exception 
	 */
	public void fireSync(final String name, final String url, final ADQLQuery query,
			final Map<String, Object> postParams) throws Exception {
		// TODO Auto-generated method stub
		executor.execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				try {
					if (postParams == null) {
						String queryParam = URLEncoder.encode(query.toADQL(), UTF8);
						URL syncUrl = new URL(String.format(SYNCGETRESULT, url, queryParam));
						currentT.setName("TsubmitSync: " + syncUrl);
						//TODO:: tintin check for the query status before loading: and remove return- covert to execute
						/*
						 * one INFO element with name=”QUERY_STATUS” and value=”OK” or
						value=”ERROR” must be included before the TABLE. If the TABLE does not
						contain the entire query result, one INFO element with value=”OVERFLOW” or
						value=”ERROR” must be included after the table.*/
						aladin.calque.newPlanCatalog(Util.openStream(syncUrl), name);
					} else {//currently this else part is used only for upload.
						URL syncUrl = new URL(url + "/sync");
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

						int limit = query.getSelect().getLimit();
						if (limit > 0) {
							out.writeField("maxrec", String.valueOf(limit));
						}
						out.writeField("query", query.toADQL());

						if (postParams != null) {// this part only for upload as of now
							for (Entry<String, Object> postParam : postParams.entrySet()) {
								if (postParam.getValue() instanceof String) {
									out.writeField(postParam.getKey(), String.valueOf(postParam.getValue()));
								} else if (postParam.getValue() instanceof File) {
									out.writeFile(postParam.getKey(), null, (File) postParam.getValue(), false);
								}
							}
						}
						out.close();
						aladin.calque.newPlanCatalog(new MyInputStream(urlConn.getInputStream()), name);
					}

				} catch (MalformedURLException e) {
					if (Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.warning(aladin.dialog, e.getMessage());
				} catch (IOException e) {
					if( Aladin.levelTrace >= 3 ) e.printStackTrace();
					Aladin.warning(aladin.dialog, e.getMessage());
				} catch (Exception e) {
					if( Aladin.levelTrace >= 3 ) e.printStackTrace();
					Aladin.warning(aladin.dialog, e.getMessage());
				} finally {
					currentT.setName(oldTName);
				}
			}
		});
	}
	
	public String getSyncUrlUnEncoded(String url, String queryParam) {
		return String.format(SYNCGETRESULT, url, queryParam);
	}
	
	/**
	 * Method to :
	 * <ol>	<li>Load query</li>
	 * 		<li>Query tap server asynchronously</li>
	 * 		<li>Populate results on Aladin</li>
	 * </ol>
	 * @param query
	 * @throws Exception 
	 */
	public void fireASync(final String name, final String serverUrl,final ADQLQuery query, final Map<String, Object> postParams) throws Exception {
		// TODO Auto-generated method stub
		executor.execute(new Runnable() {
			@Override
			public void run() {
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				currentT.setName("TsubmitAsync: "+serverUrl);
				showAsyncPanel();
				uwsFacade.handleJob(name, serverUrl, query, postParams);
				currentT.setName(oldTName);
			}
		});
	}
	
	
	public void setRaDecForTapServer(final ServerTap serverTap, final String selectedTableName) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				final Thread currentT = Thread.currentThread();
				final String oldTName = currentT.getName();
				currentT.setName("TsetRaDec: "+serverTap.getUrl());
				
				boolean addTargetPanel = false;
				Vector<TapTableColumn> columns = serverTap.tablesMetaData.get(selectedTableName).getColumns();
				JComboBox raColumn = new JComboBox(columns);
				raColumn.setRenderer(new TapTableColumnRenderer());
				raColumn.setSize(raColumn.getWidth(), Server.HAUT);
				JComboBox decColumn = new JComboBox(columns);
				decColumn.setRenderer(new TapTableColumnRenderer());
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
//						setWhereAddConstraintsGui(columnNames);
//						this.queryComponentsGui.revalidate();
//						this.queryComponentsGui.repaint();
						serverTap.createTargetPanel();
						serverTap.queryComponentsGui.revalidate();
						serverTap.queryComponentsGui.repaint();
					}
					serverTap.writeQuery();
				}
				
				currentT.setName(oldTName);
			}
		});
		
	}
	
	/**
	 * Gentle shut down of all tap threads
	 * plus async job clean up
	 */
	public void cleanUp() {
		this.executor.shutdown();
		this.uwsFacade.deleteAllSetToDeleteJobs();
		tapServerPanelCache.clear();
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
	
	/*Unless cast expcetions are -remove theis
    public <T> Future<T> submit(Callable<T> task) throws Exception {
        return executor.submit((Callable<T>) addOriginInfo(task, submitTrace(), Thread.currentThread().getName()));
    }
    
    private <T> T addOriginInfo(final Callable<T> task, final Exception oriStack, final String oriTName) throws Exception {
    	try {
            return task.call();
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Exception :"+oriTName+e+oriStack);
            throw e;
        }
    }
    
    public <T> void execute(Runnable task) throws Exception {
        executor.execute(addOriginInfo(task, submitTrace(), Thread.currentThread().getName()));
    }
 
    private <T> Runnable addOriginInfo(final Runnable task, final Exception oriStack, final String oriTName) throws Exception {
    	try {
            task.run();
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Exception:"+oriTName+e+oriStack);
            throw e;
        }
		return task;
    }
	
    private Exception submitTrace() {
        return new Exception("TwhereSubmit here");
    }
    */
	
	/**
	 * Removes current selected tap server cached info form the cache
	 */
	public void removeCurrentFromServerCache() {
		setSelectedServerLabel();
		tapServerPanelCache.remove(this.selectedServerLabel.getValue());
	}
	
	/**
	 * Hide the registry form of tap
	 */
	public void hideTapRegistryForm() {
		if (tapFrameServer != null) {
			tapFrameServer.setVisible(false);
		}
	}
	
	public DataLabel getSelectedServerLabel() {
		return selectedServerLabel;
	}
	
	/**
	 * Gets the selected datalabel on click of load
	 */
	public void setSelectedServerLabel() {
//		this.selectedServerLabel = tapServerLabels.get(DataLabel.selectedId);
		this.selectedServerLabel = null;
		for (DataLabel datalabel : getTapServerList()) {
			if (datalabel.gui.isSelected()) {
				this.loadTapServerList();
				this.selectedServerLabel = datalabel;
				break;
			}
		}
	}
	
	public boolean canReload() {
		return canReload(this.selectedServerLabel.getValue());
	}
	
	/**
	 * For tap clients loaded from AlaGlu.dic - we don't reload
	 * to reload from AlaGlu.dic - user has to drag-drop the said glu to Aladin again
	 * Reload button is mainly for the servers whose metadata is loaded from the internet. 
	 * @return
	 */
	public boolean canReload(String cacheId) {
		boolean result = false;
		if (tapServerPanelCache.containsKey(cacheId)
				&& tapServerPanelCache.get(cacheId) instanceof ServerTap) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Adds newly loaded plancatalogue into upload options
	 * @param newPlan
	 */
	public void updateAddUploadPlans(Plan newPlan) {
		if (this.uploadFrame!=null && this.uploadFrame.uploadOptions!=null) {
			if (newPlan.pcat!=null && newPlan.pcat.flagVOTable) {
				this.uploadFrame.uploadOptions.addItem(newPlan.label);
				this.uploadFrame.uploadOptions.setEnabled(true);
				if (this.uploadFrame.isVisible()) {
					this.uploadFrame.infoLabel.setText("New option added to upload file choices!");
					this.eraseNotification(this.uploadFrame.infoLabel);
					this.uploadFrame.uploadOptions.revalidate();
					this.uploadFrame.uploadOptions.repaint();
				}
				
			}
		}
	}
	
	public void updateDeleteUploadPlans(Plan planInDeletion) {
		if (this.uploadFrame!=null && this.uploadFrame.uploadOptions!=null) {
			if (planInDeletion.pcat!=null && planInDeletion.pcat.flagVOTable) {
				this.uploadFrame.uploadOptions.removeItem(planInDeletion.label);
				if (this.uploadFrame.uploadOptions.getItemCount()<=0) {
					this.uploadFrame.uploadOptions.setEnabled(false);
				} else {
					this.uploadFrame.uploadOptions.setEnabled(true);
				}
				if (this.uploadFrame.isVisible()) {
					this.uploadFrame.infoLabel.setText("One option removed from upload file choices!");
					this.eraseNotification(this.uploadFrame.infoLabel);
					this.uploadFrame.uploadOptions.revalidate();
					this.uploadFrame.uploadOptions.repaint();
				}
				
			}
		}
	}

	
}