/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.ADDPOSCONSTRAINT;
import static cds.aladin.Constants.CHANGESERVER;
import static cds.aladin.Constants.CIRCLEORSQUARE;
import static cds.aladin.Constants.DISCARDACTION;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.OPEN_SET_RADEC;
import static cds.aladin.Constants.RADECBUTTON;
import static cds.aladin.Constants.RELOAD;
import static cds.aladin.Constants.SETTINGS;
import static cds.aladin.Constants.SHOWAYNCJOBS;
import static cds.aladin.Constants.SYNC_ASYNC;
import static cds.aladin.Constants.TAPFORM_STATUS_ERROR;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADING;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTLOADED;
import static cds.aladin.Constants.UPLOAD;
import static cds.tools.CDSConstants.BOLD;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import adql.db.DBChecker;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.parser.QueryChecker;
import adql.query.ADQLQuery;
import adql.query.from.ADQLTable;
import cds.aladin.Constants.TapClientMode;
import cds.tools.ConfigurationReader;
import cds.tools.Util;
import cds.xml.VOSICapabilitiesReader;

/**
 * @author chaitra
 *
 */
public abstract class DynamicTapForm extends Server implements FilterActionClass{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8296179835574266057L;
	
	public static String TIPRETRY, TAPTABLEUPLOADTIP, TAPTABLENOUPLOADTIP, REFRESHQUERYTOOLTIP, CHECKQUERYTOOLTIP,
			SYNCASYNCTOOLTIP, SHOWASYNCTOOLTIP, TAPTABLEJOINTIP, DISCARD, DISCARDTIP, SETRADECBUTTONTIP,
			SETTINGSTOOLTIP, TIPCLICKTOADD, TAPEXDEFAULTMAXROWS, NORANGEERRORMESSAGE, TAPERRORSTATUSINFO, 
			TAPLOADINGSTATUSINFO;
	
	public String CLIENTINSTR;

	String selectedTableName;
	String loadedServerDescription;
	public boolean isFullServer = true;
	
	protected int formLoadStatus;
	protected JLabel info1;
	JComboBox tablesGui;
	protected JComboBox<String> sync_async;
	protected JComboBox<String> circleOrSquare;
	
	public DynamicTapForm() {
		// TODO Auto-generated constructor stub
		formLoadStatus = TAPFORM_STATUS_NOTLOADED;
		type = CATALOG;
		aladinLabel = "TAP";
		aladinLogo    = "TAP.png";
		this.adqlParser = new ADQLParser();
		info1 = new JLabel();
	}
	
	public DynamicTapForm(Aladin aladin) {
		// TODO Auto-generated constructor stub
		this();
		this.aladin = aladin;
		createChaine();
	}

	abstract void createFormDefault();
	
	public void setBasics() {	type = Server.CATALOG;
		verboseDescr = loadedServerDescription;
		setLayout(new BorderLayout());
		setOpaque(true);
		setBackground(tapClient.primaryColor);
		setFont(Aladin.PLAIN);
	}
	
	public static void setTopPanel(Server server, JPanel containerPanel, GridBagConstraints c, JLabel info1, String clientInstrucMessage) {
		containerPanel.setBackground(server.tapClient.primaryColor);
		
		JPanel addingPanel = new JPanel(new GridBagLayout());
		GridBagConstraints tc = new GridBagConstraints();
		addingPanel.setBackground(server.tapClient.primaryColor);
		
		tc.gridx = 0;
		tc.weightx = 0.01;
		tc.gridy = 0;
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		if (server.tapClient.mode == TapClientMode.DIALOG) {
			JButton button = TapClient.getChangeServerButton(server);
			addingPanel.add(button, tc);
			tc.gridx++;
		}
		
		if (server.tapClient.mode != TapClientMode.UPLOAD) {
			if (server instanceof DynamicTapForm) {
				JButton reloadButton = TapClient.getReloadButton();
				reloadButton.addActionListener(server);
				addingPanel.add(reloadButton, tc);
				tc.gridx++;
			}
		}
		
		JPanel titlePanel = new JPanel();
		titlePanel.setBackground(server.tapClient.primaryColor);
		titlePanel.setAlignmentY(SwingConstants.CENTER);
		if (server.tapClient.mode != TapClientMode.UPLOAD) {
			server.makeTitle(titlePanel, server.tapClient.getVisibleLabel());
//			this.aladinLabel = this.name;
		} else {
			server.makeTitle(titlePanel, "Upload server");
		}
		tc.weightx = 0.88;
		c.fill = GridBagConstraints.HORIZONTAL;
	    addingPanel.add(titlePanel, tc);
	    tc.gridx++;
//		containerPanel.add(titlePanel, c);
		
		JPanel optionsPanel = server.tapClient.getModes(server);
	    optionsPanel.setBackground(server.tapClient.primaryColor);
	    tc.weightx = 0.10;
	    c.fill = GridBagConstraints.NONE;
	    addingPanel.add(optionsPanel, tc);
	    
	    c.gridx = 0;
	    c.gridy = 0;
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 1;
		containerPanel.add(addingPanel, c);
		
		// Premiere indication
		info1.setText(clientInstrucMessage);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
	    c.weighty = 0.02;
	    info1.setHorizontalAlignment(SwingConstants.CENTER);
	    containerPanel.add(info1, c);
	    c.gridy++;
	}
	
	/**
	 * Creates the table selection panel with tables drop-down, upload button etc..
	 * @param tableChoice
	 * @param tables
	 * @return
	 * @throws BadLocationException 
	 */
	public JPanel getTablesPanel(final JComboBox tablesGui, TapTable chosenTable, boolean showSettings) throws BadLocationException {
    	JPanel tablesPanel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		tablesPanel.setLayout(gridbag);
		tablesPanel.setFont(BOLD);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets = new Insets(1, 3, 1, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.WEST;
		c.weightx = 0.05;
		
		JLabel label = new JLabel();
		label.setText("Table:");
		label.setFont(BOLD);
		tablesPanel.add(label, c);
		
		String tableToolTip = chosenTable.getDescription();
		if (tableToolTip != null && !tableToolTip.isEmpty()) {
			tablesGui.setToolTipText("<html><p width=\"500\">"+tableToolTip+"</p></html>");
		} else {
			tablesGui.setToolTipText(null);
		}
		if (this.tapClient.mode != TapClientMode.UPLOAD) {
			tablesGui.setEditable(true);
			JTextComponent tablesGuiEditor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
			List<String> keys = new ArrayList<String>();
			keys.addAll(this.tapClient.tablesMetaData.keySet());
			FilterDocument document = new FilterDocument(this, tablesGui, keys, chosenTable.getTable_name());
			tablesGuiEditor.setDocument(document);
		} else {
			tablesGui.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					checkSelectionChanged(tablesGui);
				}
			});
		}
		
		tablesGui.setOpaque(false);
		tablesGui.setName("table");
//		tablesGui.setActionCommand(TABLECHANGED);
		tablesGui.setAlignmentY(SwingConstants.CENTER);
		c.insets = new Insets(1, 0, 1, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx++;
		c.weightx = 0.75;
		tablesPanel.add(tablesGui, c);
		
		if (this.tapClient.mode == TapClientMode.UPLOAD) {
			c.weightx = 0.05;
			c.gridx++;
			JButton button = new JButton(DISCARD);
			button.setActionCommand(DISCARDACTION);
			button.addActionListener(this.tapClient.tapManager.uploadFrame);
			button.setToolTipText(DISCARDTIP);
			tablesPanel.add(button, c);
		}
		
		if (this instanceof ServerTap) {
			JButton button = new JButton("Set ra, dec");
			button.setName(RADECBUTTON);
			button.setActionCommand(OPEN_SET_RADEC);
			button.addActionListener(this);
			button.setToolTipText(SETRADECBUTTONTIP);
			c.insets = new Insets(1, 3, 1, 3);
			c.weightx = 0.10;
			c.gridx++;
			tablesPanel.add(button,c);
		
			button = new JButton("Join");
			c.weightx = 0.05;
			c.gridx++;
			tablesPanel.add(button, c);
			button.setEnabled(false);
			button.addActionListener(this);
			button.setToolTipText(TAPTABLEJOINTIP);
		} else if (showSettings) {
			JButton button = new JButton("Settings");
			button.setActionCommand(SETTINGS);
			button.addActionListener(this);
			button.setToolTipText(SETTINGSTOOLTIP);
			c.insets = new Insets(1, 3, 1, 3);
			c.weightx = 0.10;
			c.gridx++;
			tablesPanel.add(button,c);
		}
		
		return tablesPanel;
	}
	
	/**
	 * Creates the 
	 * @param targetPanel
	 */
	protected void createTargetPanel(JPanel targetPanel){
		targetPanel.removeAll();
		GridBagLayout gridbag = new GridBagLayout();
		targetPanel.setLayout(gridbag);
		targetPanel.setFont(BOLD);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.10;
		
		JLabel label;
//		targetLabel = label= new JLabel("");
//        resumeTargetLabel();
		label= new JLabel("Target");
        label.setFont(BOLD);
//        label.setSize(20, HAUT);
		gridbag.setConstraints(label, c);
		targetPanel.add(label);

		this.target = new JTextField(40);
        target.addKeyListener(this);
        target.addActionListener(this);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.80;
		gridbag.setConstraints(target, c);
		targetPanel.add(target);

		this.grab = new JToggleButton("Grab");
		Insets m = grab.getMargin();
        grab.setMargin(new Insets(m.top,2,m.bottom,2));
        grab.setOpaque(false);
		grab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aladin.f.toFront();
				JPanel panel = DynamicTapForm.this;
				aladin.grabUtilInstance.grabFrame = (GrabItFrame) SwingUtilities.getRoot(panel);
           }
        });
        grab.setFont(Aladin.SBOLD);
        Component rootFrame = SwingUtilities.getRoot(this);
        if (rootFrame instanceof GrabItFrame) {
        	updateWidgets((GrabItFrame) rootFrame);
		}
        
        if (this.aladinLabel.equalsIgnoreCase(Constants.DATALINK_CUTOUT_FORMLABEL)) {
        	grab.setEnabled(true);//Default true for datalink forms
		}
        
        c.gridwidth = 1;
		c.weightx = 0.05;
		c.gridx = 3;
		gridbag.setConstraints(grab, c);
		targetPanel.add(grab);

		String radText=RAD;
		label= new JLabel(addDot(radText));
		label.setFont(Aladin.BOLD);
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.weightx = 0.10;
		gridbag.setConstraints(label, c);
		targetPanel.add(label);
		
		radius = new JTextField(50);
		radius.addKeyListener(this);
		radius.addActionListener(this);
		c.gridx = 1;
		c.weightx = 0.80;
		gridbag.setConstraints(radius, c);
		targetPanel.add(radius);
		
		this.circleOrSquare = new JComboBox<String>(CIRCLEORSQUARE);
		circleOrSquare.setOpaque(false);
		circleOrSquare.setName("posConstraintShape");
//		this.circleOrSquare.setActionCommand(POSCONSTRAINTSHAPECHANGED);
		circleOrSquare.setSelectedIndex(0);
//		this.circleOrSquare.addActionListener(this);
		c.gridx = 2;
		
		if (!(this instanceof ServerTapExamples)) {
			c.weightx = 0.05;
			targetPanel.add(circleOrSquare, c);
			JButton button = new JButton("Add");
			button.setActionCommand(ADDPOSCONSTRAINT);
			button.addActionListener(this);
			button.setToolTipText(TIPCLICKTOADD);
			c.weightx = 0.05;
			c.gridx = 3;
			gridbag.setConstraints(button, c);
			targetPanel.add(button);
		} else {
			c.gridwidth = 2;
			c.weightx = 0.10;
			targetPanel.add(circleOrSquare, c);
		}
		
		Util.toolTip(label, RADIUS_EX);
	    Util.toolTip(radius, RADIUS_EX);
	    
		modeCoo = RADEd; // just ra and dec
		modeRad = STRINGd;
		if (coo == null) {
			coo = new JTextField[2];
			coo[0] = new JTextField();
			coo[1] = new JTextField();
		}
		if (rad == null) {
			rad = new JTextField[2];
			rad[0] = new JTextField();
			rad[1] = new JTextField();
		}
		targetPanel.setVisible(true);
	}
	
	/**
	 * Lower buttons panel, just above the tap query text area
	 * @return
	 */
	public JPanel getBottomPanel(boolean addRefreshQuery) {
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bottomPanel.setBackground(this.tapClient.primaryColor);
		JButton button = new JButton("Refresh query");
		if (addRefreshQuery && (this instanceof ServerTap || this instanceof ServerObsTap)) {
			button.setToolTipText(REFRESHQUERYTOOLTIP);
			button.setActionCommand("WRITEQUERY");
			button.addActionListener(this);
			bottomPanel.add(button);
		}
		
		button = new JButton("Check..");
		button.setToolTipText(CHECKQUERYTOOLTIP);
		button.setActionCommand("CHECKQUERY");
		button.addActionListener(this);
		bottomPanel.add(button);
		
		this.sync_async = new JComboBox<String>(SYNC_ASYNC);
		this.sync_async.setOpaque(false);
		if (SYNCASYNCTOOLTIP!=null && !SYNCASYNCTOOLTIP.isEmpty()) {
			SYNCASYNCTOOLTIP = "<html><p width=\"500\">"+SYNCASYNCTOOLTIP+"</p></html>";
			this.sync_async.setToolTipText(SYNCASYNCTOOLTIP);
		}
		bottomPanel.add(this.sync_async);
		
		button = new JButton("Async jobs>>");
		button.setActionCommand(SHOWAYNCJOBS);
		button.setToolTipText(SHOWASYNCTOOLTIP);
		button.addActionListener(this);
		bottomPanel.add(button);
		
		if (this.tapClient.mode != TapClientMode.UPLOAD) {
			bottomPanel.add(getUploadButtonIfAvailable());
		}
		return bottomPanel;
	}
	
	public JButton getUploadButtonIfAvailable() {
		String uploadTipText = TAPTABLEUPLOADTIP;
		JButton button = new JButton("Upload");
		button.setActionCommand(UPLOAD);
		if (this.tapClient.capabilities != null) {
			try {
				VOSICapabilitiesReader meta = this.tapClient.capabilities.get();
				button.setEnabled(meta.isUploadAllowed());
				button.addActionListener(this);
				if (meta.isUploadAllowed() && meta.getUploadHardLimit() > 0L) {
					String tip = String.format("Hard limit =%1$s rows", meta.getUploadHardLimit());
					uploadTipText = uploadTipText.concat(tip);
				} else if (!meta.isUploadAllowed()) {
					uploadTipText = TAPTABLENOUPLOADTIP;
				}
				button.setToolTipText(uploadTipText);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if( Aladin.levelTrace >= 3 ) e.printStackTrace();//Do nothing, no upload button will be added
			}
		}
		return button;
	}
	
	
//	public abstract void tableSelectionChanged(JComboBox<String> comboBox);
	
	public abstract void changeTableSelection(String tableChoice);
	
	@Override
	public void checkSelectionChanged(JComboBox<String> comboBox){
		if (comboBox.getSelectedItem() != null
				&& !selectedTableName.equalsIgnoreCase(comboBox.getSelectedItem().toString())) {
			Aladin.trace(3, "Change table selection from within the document");
			if (comboBox == this.tablesGui) {
				selectedTableName = (String) comboBox.getSelectedItem();
				this.changeTableSelection(selectedTableName);
			}
//			changeTableSelection((String) comboBox.getSelectedItem());
		}
	};
	
	/**
	 * Method sets selectedTableName as per table choice, also checks loads the respective column
	 * @param columnNames
	 * @param tablesMetaData
	 * @return
	 */
	public Vector<TapTableColumn> setTableGetColumnsToLoad(String tableChoice, Map<String, TapTable> tablesMetaData) {
		if (tableChoice == null || !tablesMetaData.keySet().contains(tableChoice)) {
			selectedTableName = tablesMetaData.keySet().iterator().next();
		} else {
			selectedTableName = tableChoice;
		}
		String tableToolTip = tablesMetaData.get(selectedTableName).getDescription();
		if (tableToolTip != null && !tableToolTip.isEmpty()) {
			tablesGui.setToolTipText("<html><p width=\"500\">"+tableToolTip+"</p></html>");
		} else {
			tablesGui.setToolTipText(null);
		}
		Vector<TapTableColumn> columnNames = getColumnsToLoad(selectedTableName, tablesMetaData);
		return columnNames;
	}
	
	public Vector<TapTableColumn> getColumnsToLoad(String tableName, Map<String, TapTable> tablesMetaData) {
		Vector<TapTableColumn> columnNames = tablesMetaData.get(tableName).getColumns();
		if (columnNames == null) {
			if (this.tapClient.mode == TapClientMode.UPLOAD) {
				Aladin.warning("Error in uploaded data");
				return null;
			}
			try {
				List<String> tableNamesToUpdate = new ArrayList<String>();
				tableNamesToUpdate.add(tableName);
				this.tapClient.tapManager.updateTableColumnSchemas(this, tableNamesToUpdate);
				columnNames = tablesMetaData.get(tableName).getColumns();
				if (this instanceof ServerObsTap) {
					((ServerObsTap) this).setObsCore(this.tapClient.obscoreTables.get(tableName));
//					tapClient.parseForObscore(selectedTableName, tablesMetaData.get(selectedTableName));
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				String revertTable = tablesMetaData.keySet().iterator().next();
				if (tablesMetaData.get(revertTable).getColumns() != null) {
					JTextComponent tablesGuiEditor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
					FilterDocument tapTableFilterDocument = (FilterDocument) tablesGuiEditor
							.getDocument();
					try {
						tapTableFilterDocument.setDefault();//trying to select default table till here
						changeTableSelection(revertTable);
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						showLoadingError();
					}
				} else {
					showLoadingError();
				}
				defaultCursor();
				return null;
			}
			if (columnNames == null) {
				Aladin.warning("Error in updating the metadata for :"+selectedTableName);
				showLoadingError();
				defaultCursor();
				return null;
			}
		}
		return columnNames;
	}
	
	@Override
	protected void showStatusReport() {
		if (aladin.frameInfoServer == null || !aladin.frameInfoServer.isOfDynamicTapServerType()
				|| !aladin.frameInfoServer.getServer().equals(this)) {
			if (aladin.frameInfoServer != null) {
				aladin.frameInfoServer.dispose();
			}
			if (this.tapClient.infoPanel != null) {// new server
				aladin.frameInfoServer = new FrameInfoServer(aladin, this.tapClient.infoPanel);
			} else {// incase the table info is not populated or some issues..
				aladin.frameInfoServer = new FrameInfoServer(aladin);
			}
		} else if (aladin.frameInfoServer.isFlagUpdate() == 1) {
			try {
				aladin.frameInfoServer.updateInfoPanel();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if (Aladin.levelTrace >= 3)
					e.printStackTrace();
			}
		}
		aladin.frameInfoServer.show(this);
	}
	
	@Override
	public Vector<String> getMatches(String mask, JComboBox<String> comboBox) {
		Vector<String> matches = new Vector<String>();
		if (/*this.tablesGui ==  comboBox && */mask != null && !mask.isEmpty()) {
			for (String key : this.tapClient.tablesMetaData.keySet()) {
				boolean checkDescription = false;
				TapTable table = this.tapClient.tablesMetaData.get(key);
				if (table != null && table.getDescription() != null && !table.getDescription().isEmpty()) {
					checkDescription = true;
				}
				if (!(Util.indexOfIgnoreCase(key, mask) >= 0
						|| (checkDescription && Util.indexOfIgnoreCase(table.getDescription(), mask) >= 0))) {
					continue;
				}
				matches.add(key);
			}
		}
		return matches;
		
	}
	
	@Override
	public ADQLQuery checkQuery()  throws UnresolvedIdentifiersException {
		ADQLQuery query = null;
		try {
			this.tapClient.updateUploadedTablesToParser(this);
			query = super.checkQuery();
//			DefaultDBTable table = new DefaultDBTable(selectedTableName);
//			System.err.println("getADQLCatalogName "+table.getADQLCatalogName()+"\ngetADQLSchemaName "+table.getADQLSchemaName()+"\ngetADQLName "+table.getADQLName()+"\ntoString "+table.toString());
		} catch (UnresolvedIdentifiersException uie) {	
			Aladin.trace(3, "Number of errors in the query: "+uie.getNbErrors());
			adql.parser.ParseException ex = null;
			try {
				List<String> tableNames = getTableNamesofNoMetadataInQuery(tap.getText());
				if (tableNames != null && !tableNames.isEmpty()) {
					try {
						this.tapClient.tapManager.updateTableColumnSchemas(this, tableNames);
						Aladin.trace(3, "updated metadata for these tables:"+uie.getNbErrors());
					} catch (Exception e) {
						// do nothing. 
					}
					query = this.adqlParser.parseQuery(tap.getText());
				} else {
					throw uie;
				}
			} catch (UnresolvedIdentifiersException uie2) {
				//yeah those are columns then get those table meta data
				//if still there is an issue then you go ahead and highlight
				Iterator<adql.parser.ParseException> it = uie2.getErrors();
				while (it.hasNext()) {
					ex = it.next();
					highlightQueryError(tap.getHighlighter(), ex);
				}
				info1.setText("Are you sure of the highlighted identifiers?");
				this.tapClient.tapManager.eraseNotification(info1, CLIENTINSTR);
				throw uie2;// this is just for showing message
			} catch (ParseException e) {
				//this one should not occur, but anyway error from this is highlighted. so do nothing
			}
		}
		return query;
	}
	
	public List<String> getTableNamesofNoMetadataInQuery(String query) {
		ADQLParser syntaxParser = new ADQLParser();
		List<String> tableNames = null;
		try {
			ADQLQuery adqlQuery = syntaxParser.parseQuery(query);
			tableNames = new ArrayList<String>();
//			DBColumn[] columns = query.getResultingColumns();//match columns with the unresolvedIdentifiers?
			for (ADQLTable adqlTable : adqlQuery.getFrom().getTables()) {
				String tableNameKey = getTableMetaCacheKey(adqlTable.getFullTableName());
				TapTable meta = this.tapClient.tablesMetaData.get(tableNameKey);
				Vector<TapTableColumn> columnNames = null;
				if (meta != null) {
					columnNames = meta.getColumns();
				}
				if (columnNames == null) {
					tableNames.add(tableNameKey);
				}
				
			}
		} catch (Exception ie){
			//don't do anything
		}
		return tableNames;
	}
	
	public String getTableMetaCacheKey(String fullTableName) {
		String result = fullTableName;
		if (fullTableName.contains("\"")) {
			result = fullTableName.replaceAll("\"", EMPTYSTRING);
		}
		return result;
	}
	
	public void updateQueryChecker(List<String> tableNames) {
		// TODO Auto-generated method stub
		for (String tableName : tableNames) {
			updateQueryChecker(tableName);
		}
	}
	
	/**
	 * Updates the adql parser for a table.
	 * @param tableName
	 */
	public void updateQueryChecker(String tableName) {
		boolean isUpload = this.tapClient.mode == TapClientMode.UPLOAD;
		updateQueryChecker(isUpload, tableName, this.tapClient.tablesMetaData, this.tapClient.queryCheckerTables);
	}
	
	/**
	 * Updates the adql parser for a table from the tap metadata param.
	 * @param tableName
	 */
	public void updateQueryChecker(boolean isUploadTable, String tableName, Map<String, TapTable> tablesMetaData, List<DefaultDBTable> queryCheckerTables) {
		DefaultDBTable table = new DefaultDBTable(tableName);
		DefaultDBTable queryCheckerTable = null;
		
		if (queryCheckerTables != null) {
			for (DefaultDBTable defaultDBTable : queryCheckerTables) {//Check if table is existing
				if (TapManager.areSameQueryCheckerTables(defaultDBTable, table)) {
					queryCheckerTable = defaultDBTable;
					break;
				}
			}
			
			if (tablesMetaData.containsKey(tableName)) {//Get table metadata
				Vector<TapTableColumn> columns = tablesMetaData.get(tableName).getColumns();
				updateQueryCheckTableColumns(table, columns);
				
				if (isUploadTable || (queryCheckerTable != null && queryCheckerTables.remove(queryCheckerTable))) {
					queryCheckerTables.add(table);
					QueryChecker checker = new DBChecker(queryCheckerTables);
					this.adqlParser.setQueryChecker(checker);
				}
				
			}
		}
	}
	
	/**
	 * Updates the adql parser for a table from the tap metadata param.
	 * @param tableName
	 */
	public void updateQueryChecker_deleteTable(String tableName, List<DefaultDBTable> queryCheckerTables) {
		DefaultDBTable table = new DefaultDBTable(tableName);
		DefaultDBTable queryCheckerTable = null;
		
		if (queryCheckerTables != null) {
			for (DefaultDBTable defaultDBTable : queryCheckerTables) {//Check if table is existing
				if (TapManager.areSameQueryCheckerTables(defaultDBTable, table)) {
					queryCheckerTable = defaultDBTable;
					break;
				}
			}
			
			if (this.tapClient.mode == TapClientMode.UPLOAD
					|| (queryCheckerTable != null && queryCheckerTables.remove(queryCheckerTable))) {
				QueryChecker checker = new DBChecker(queryCheckerTables);
				this.adqlParser.setQueryChecker(checker);
			}
		}
	}
	
	/**
	 * Convenience method to set column to table for parser
	 * @param parserTable
	 * @param columnsMeta
	 */
	protected void updateQueryCheckTableColumns(DefaultDBTable parserTable, Vector<TapTableColumn> columnsMeta) {
		if (parserTable != null && columnsMeta != null) {
			for(TapTableColumn tapTableColumn : columnsMeta) {
				DefaultDBColumn columnForParser = new DefaultDBColumn(tapTableColumn.getColumn_name(), parserTable);
				if (tapTableColumn.getDatatype() != null && !tapTableColumn.getDatatype().isEmpty()) {
					int offset = tapTableColumn.getDatatype().indexOf("adql:");
					if (offset != -1 && offset + 5 < tapTableColumn.getDatatype().length()) {
						String datatype = tapTableColumn.getDatatype().substring(offset + 5);
						if (TapClient.DBDATATYPES.containsKey(datatype)) {
							DBDatatype dbDataType = TapClient.DBDATATYPES.get(datatype);
							DBType type = null;
							if (tapTableColumn.getSize() > 0) {
								type = new DBType(dbDataType, tapTableColumn.getSize());
							} else {
								type = new DBType(dbDataType);
							}
							columnForParser.setDatatype(type);
						}
					}
				}
				parserTable.addColumn(columnForParser);
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		super.actionPerformed(arg0);
		Object source = arg0.getSource();
		if (source instanceof JButton) {
			String action = ((JButton) source).getActionCommand();
			if (action.equals(CHANGESERVER)) {
				try {
					this.tapClient.tapManager.showTapRegistryForm();
				} catch (Exception e) {
					Aladin.warning(this, TapClient.GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} else if (action.equals(UPLOAD)) {
				//disabled based on capability and if user has not created a table
				this.tapClient.showOnUploadFrame();
				
			} else if (action.equals(SHOWAYNCJOBS)) {
				try {
					this.tapClient.tapManager.showAsyncPanel();
				} catch (Exception e) {
					Aladin.warning(this, TapClient.GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} else if (action.equals(RELOAD)) {
				try {
					this.tapClient.reload(this);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if (Aladin.levelTrace >=3) e.printStackTrace();
					Aladin.warning(this, e.getMessage());
				}
			}
		}
	}
	
	//Not bothering for ServerObscore.. yet!
	public Vector<String> getTableNames() {
		Vector<String> tables = new Vector<String>();
		if (!isFullServer && this.tapClient.nodeTableNames != null) {
			for (String nodeTableName : this.tapClient.nodeTableNames) {
				DefaultDBTable ntable = new DefaultDBTable(nodeTableName);
				if (this.tapClient.tablesMetaData.containsKey(TapManager.getFullyQualifiedTableName(ntable))) {
					tables.add(nodeTableName);
//					tables.add(this.tapClient.tablesMetaData.get(nodeTableName));
				} else if (this.tapClient.tablesMetaData.containsKey(ntable.getADQLName())) {
					tables.add(ntable.getADQLName());
//					tables.addElement(this.tapClient.tablesMetaData.get(nodeTableName));
				}
			}
		} else {
			tables.addAll(this.tapClient.tablesMetaData.keySet());
		}
		return tables;
		
	}
	
	/**
	 * Tap client gui in case of loading error
	 * @param string 
	 */
	public void showLoadingError() {
		this.removeAll();
		
		this.setLayout(new BorderLayout());
		this.setBackground(this.tapClient.primaryColor);
		GridBagConstraints c = new GridBagConstraints();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		CLIENTINSTR = "Error: unable to load "+this.tapClient.tapLabel;
		setTopPanel(this, containerPanel, c, info1, CLIENTINSTR);
		prepNoMetaDataScreen(containerPanel, c);
		ball.setMode(Ball.NOK);
		this.add(containerPanel);
		verboseDescr = TAPERRORSTATUSINFO;
		formLoadStatus = TAPFORM_STATUS_ERROR;
		revalidate();
		repaint();
	}
	
	/**
	 * Tap client gui in case when it is still loading
	 */
	public void showloading() {
		this.removeAll();
		this.formLoadStatus = TAPFORM_STATUS_LOADING;
		
		this.setLayout(new BorderLayout());
		this.setBackground(this.tapClient.primaryColor);
		GridBagConstraints c = new GridBagConstraints();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		CLIENTINSTR = "loading "+this.tapClient.tapLabel+"...";
		setTopPanel(this, containerPanel, c, info1, CLIENTINSTR);
		
		prepNoMetaDataScreen(containerPanel, c);
		ball.setMode(Ball.WAIT);
		this.add(containerPanel);
		verboseDescr = TAPLOADINGSTATUSINFO;
		revalidate();
		repaint();
	}
	
	public void prepNoMetaDataScreen(JPanel containerPanel, GridBagConstraints c) {
		JPanel linePanel = getBottomPanel(false);
		c.gridwidth = 1;
		c.gridx = 0;
		c.weightx = 1;
		c.weighty = 0.02;
		c.insets = new Insets(0, -6, 0, 0);
	    c.fill = GridBagConstraints.NONE;
	    c.gridy++;
	    containerPanel.add(linePanel, c);
	    
		tap = new JTextArea(8, 100);//"", 8, 50
		tap.setFont(Aladin.ITALIC);
		tap.setWrapStyleWord(true);
		tap.setLineWrap(true);
		tap.setEditable(true);
		JScrollPane scrolley = new JScrollPane(tap);
//		c.weightx = 0.35;
		c.weighty = 0.75;
		c.weightx = 1;
		c.insets = new Insets(0, 4, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(scrolley, c);
	}
	
	public boolean isNotLoaded() {
		return (formLoadStatus == TAPFORM_STATUS_NOTLOADED);
	}
	
	public boolean isLoaded(){
		return (formLoadStatus == TAPFORM_STATUS_LOADED);
	}
	
	static {
		TIPRETRY = Aladin.chaine.getString("TAPTIPRETRY");
		TAPTABLEUPLOADTIP = Aladin.chaine.getString("TAPTABLEUPLOADTIP");
		TAPTABLENOUPLOADTIP = Aladin.chaine.getString("TAPTABLENOUPLOADTIP");
		REFRESHQUERYTOOLTIP = Aladin.chaine.getString("REFRESHQUERYTOOLTIP");
		CHECKQUERYTOOLTIP = Aladin.chaine.getString("CHECKQUERYTOOLTIP");
		SYNCASYNCTOOLTIP = Aladin.chaine.getString("SYNCASYNCTOOLTIP");
		SHOWASYNCTOOLTIP = Aladin.chaine.getString("SHOWASYNCTOOLTIP");
		TIPRETRY = Aladin.chaine.getString("TAPTIPRETRY");
		TAPTABLEJOINTIP = Aladin.chaine.getString("TAPTABLEJOINTIP");
		DISCARD = Aladin.chaine.getString("DISCARD");
		DISCARDTIP = Aladin.chaine.getString("DISCARDTIP");
		SETRADECBUTTONTIP = Aladin.chaine.getString("SETRADECBUTTONTIP");
		SETTINGSTOOLTIP = Aladin.chaine.getString("TAPSETTINGSTOOLTIP");
		TIPCLICKTOADD = Aladin.chaine.getString("TIPCLICKTOADD");
		TAPEXDEFAULTMAXROWS = ConfigurationReader.getInstance().getPropertyValue("TAPEXDEFAULTMAXROWS");
		TAPERRORSTATUSINFO = Aladin.chaine.getString("TAPERRORSTATUSINFO");
		TAPLOADINGSTATUSINFO = Aladin.chaine.getString("TAPLOADINGSTATUSINFO");
	}

}
