/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.ACCESSURL;
import static cds.aladin.Constants.ACCESS_ESTSIZE;
import static cds.aladin.Constants.ACCESS_FORMAT;
import static cds.aladin.Constants.ADDPOSCONSTRAINT;
import static cds.aladin.Constants.ADD_DATAPRODUCTTYPE;
import static cds.aladin.Constants.ADD_FREECONSTRAINT;
import static cds.aladin.Constants.ADD_SPATIALCONSTRAINT;
import static cds.aladin.Constants.ADD_SPECTRALCONSTRAINT;
import static cds.aladin.Constants.ADD_TIMECONSTRAINT;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.COMMA_CHAR;
import static cds.aladin.Constants.DATAPRODUCT_TYPE;
import static cds.aladin.Constants.DEC;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.EM_MAX;
import static cds.aladin.Constants.EM_MIN;
import static cds.aladin.Constants.OBSID;
import static cds.aladin.Constants.POSQuery;
import static cds.aladin.Constants.RA;
import static cds.aladin.Constants.SELECTALL;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TAP_REC_LIMIT;
import static cds.aladin.Constants.TAP_REC_LIMIT_UNLIMITED;
import static cds.aladin.Constants.TARGETNAN;
import static cds.aladin.Constants.T_MAX;
import static cds.aladin.Constants.T_MIN;
import static cds.aladin.Constants.WRITEQUERY;
import static cds.tools.CDSConstants.BOLD;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import adql.db.DBChecker;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.parser.ADQLParser;
import adql.parser.QueryChecker;
import adql.parser.TokenMgrError;
import adql.query.ADQLQuery;
import adql.query.ClauseConstraints;
import cds.aladin.Constants.TapClientMode;

/**
 * @author chaitra
 *
 */
public class ServerObsTap extends DynamicTapForm implements ItemListener {
	/**
	 * 
	 */
	
	private static final long serialVersionUID = 8589874284137553948L;
	private static final String[] DATAPRODUCT_TYPES = { "image", "cube", "spectrum", "sed", "timeseries", "visibility",
			"event", "measurements" };
	private static String TIPCLICKTOADD;
	
	public Map<String, String> dataProductTypeParamName = null;
	private static String S_REGION_INCLUDINGPOINT = "s_region containing point";
	String raColumnName = null;
	String decColumnName = null;
	public static String EXPOSURETIME = "Exposure time";
	public static String SPECTRALRESOLUTIONPOWER = "Spectral resolving power";
	public static String SPECTRALRANGE = "Spectral range";
	public static String TIMERANGE = "Time range";
	
	public static String SPATIALRESOLUTION = "Spatial resolution";
	public static String SPECTRALRESOLUTION = "Spectral resolution";
	public static String TIMERESOLUTION = "Temporal resolution";
	public static String FIELDSIZE = "Field size";
	
	private static String RANGEQUERY = " >= ${range1} AND ${range2} <= ";
	
	Map<String, Map<String, String>> spatialFieldValueOptions = new HashMap<String, Map<String, String>>();
	Map<String, Map<String, String>> spectralFieldValueOptions = new HashMap<String, Map<String, String>>();
	Map<String, Map<String, String>> timeFieldValueOptions = new HashMap<String, Map<String, String>>();
	
	public void addOtherParams(String tableName){
		String min = null;
		String max = null;
		String range = null;
		if (spectralFieldValueOptions.get(tableName).containsKey(EM_MIN) && spectralFieldValueOptions.get(tableName).containsKey(EM_MAX)) {
			min = spectralFieldValueOptions.get(tableName).get(EM_MIN);
			max = spectralFieldValueOptions.get(tableName).get(EM_MAX);
			range = min+RANGEQUERY+max;
			spectralFieldValueOptions.get(tableName).put(SPECTRALRANGE, range);
		}
		
		if (timeFieldValueOptions.get(tableName).containsKey(T_MIN) && timeFieldValueOptions.get(tableName).containsKey(T_MAX)) {
			min = timeFieldValueOptions.get(tableName).get(T_MIN);
			max = timeFieldValueOptions.get(tableName).get(T_MAX);
			range = min+RANGEQUERY+max;
			timeFieldValueOptions.get(tableName).put(TIMERANGE, range);
		}
	}
	
	//gui
	JComboBox<String> limit;
	JComboBox selectColumns;
	JComboBox<String> dataProduct_types;
	JComboBox dataProduct_types_andOrOp;
	JComboBox spatial_andOrOp;
	JComboBox spatial_fields;
	JTextField spatial_value; 
	JComboBox spectral_andOrOp;
	JComboBox spectral_fields;
	JTextField spectral_value;
	JComboBox time_andOrOp;
	JComboBox time_fields;
	JTextField time_value;
	JComboBox free_andOrOp;
	JComboBox free_fields;
	JTextField free_value;
	
	public Map<String, List<String>> selectAllOptions = null;
	private JPanel targetPanel;

	public ServerObsTap(Aladin aladin) {
		// TODO Auto-generated constructor stub
		super(aladin);
	}
	
	public void createForm(String tableChoice) {
		CLIENTINSTR = Aladin.chaine.getString("TAPOBSCORECLIENTINSTR");
		for (TapTable table : tapClient.obscoreTables.values()) {
			setObsCore(table);
		}
		QueryChecker checker = new DBChecker(this.tapClient.queryCheckerTables);
		this.adqlParser.setQueryChecker(checker);
		setBasics();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		DynamicTapForm.setTopPanel(this, containerPanel, c, info1, CLIENTINSTR);
		
		JPanel panelScroll = new JPanel();
		panelScroll.setBackground(tapClient.secondColor);
		setConstraintsGui(tableChoice, panelScroll);
//		panelScroll.setPreferredSize(new Dimension(500, 320));
		panelScroll.setPreferredSize(new Dimension(565, 290));
		JScrollPane scrolley = new JScrollPane(panelScroll);
		scrolley.setPreferredSize(new Dimension(565, 400));
		c.weighty = 0.75;
		c.insets = new Insets(0, -6, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
		containerPanel.add(scrolley, c);
		
		JPanel linePanel = getBottomPanel(true);
		c.weightx = 0.10;
		c.weighty = 0.02;
		c.insets = new Insets(0, -6, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridy++;
	    containerPanel.add(linePanel, c);
	    
		tap = new JTextArea(8, 200);//"", 8, 50
		tap.setFont(Aladin.ITALIC);
		tap.setWrapStyleWord(true);
		tap.setLineWrap(true);
		tap.setEditable(true);
		scrolley = new JScrollPane(tap);
		c.weightx = 0.35;
		c.weighty = 0.17;
		c.insets = new Insets(0, 4, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(scrolley, c);

	    this.removeAll();
	    add(containerPanel);
	    
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    writeQuery();
	}
	
	public void setObsCore(TapTable table) {
		String tableName = table.getTable_name();
		Map<String, String> obsCoreColumns = table.obsCoreColumns;
		
		if (obsCoreColumns != null && obsCoreColumns.size() > TapTable.MAXOBSCORECOLSCOUNTED) {//==17
			StringBuffer statement = null;
			Map<String, String> params = new HashMap<String, String>();
			if (this.selectAllOptions == null) {
				this.selectAllOptions = new HashMap<String, List<String>>();
			}
			this.selectAllOptions.put(tableName, new ArrayList<String>());
			this.selectAllOptions.get(tableName).add(" * ");
			if (obsCoreColumns.containsKey(OBSID) && obsCoreColumns.containsKey(RA) && obsCoreColumns.containsKey(DEC)) {
				statement = new StringBuffer(obsCoreColumns.get(OBSID));
				statement.append(COMMA_CHAR).append(obsCoreColumns.get(RA)).append(COMMA_CHAR).append(obsCoreColumns.get(DEC));
				this.selectAllOptions.get(tableName).add(statement.toString());
			}
			if (obsCoreColumns.containsKey(ACCESSURL) && obsCoreColumns.containsKey(ACCESS_FORMAT) && obsCoreColumns.containsKey(ACCESS_ESTSIZE)) {
				statement = new StringBuffer(ACCESSURL);
				statement.append(COMMA_CHAR).append(obsCoreColumns.get(ACCESS_FORMAT)).append(COMMA_CHAR).append(obsCoreColumns.get(ACCESS_ESTSIZE));
				this.selectAllOptions.get(tableName).add(statement.toString());
			}
			if (obsCoreColumns.get(DATAPRODUCT_TYPE) != null) {
				if (this.dataProductTypeParamName == null) {
					this.dataProductTypeParamName = new HashMap<String, String>();
				}
				this.dataProductTypeParamName.put(tableName, obsCoreColumns.get(DATAPRODUCT_TYPE));
			}
			
			if (this.spatialFieldValueOptions == null) {
				this.spatialFieldValueOptions = new HashMap<String, Map<String, String>>();
			}
			params.put(ServerObsTap.FIELDSIZE, "AREA("+obsCoreColumns.get(ServerObsTap.FIELDSIZE)+")");
			params.put(ServerObsTap.SPATIALRESOLUTION, obsCoreColumns.get(ServerObsTap.SPATIALRESOLUTION));
			params.put(RA, obsCoreColumns.get(RA));
			params.put(DEC, obsCoreColumns.get(DEC));
			this.spatialFieldValueOptions.put(tableName, params);
			
			
			if (this.timeFieldValueOptions == null) {
				this.timeFieldValueOptions = new HashMap<String, Map<String, String>>();
			}
			params = new HashMap<String, String>();
			params.put(T_MIN, obsCoreColumns.get(T_MIN));
			params.put(T_MAX, obsCoreColumns.get(T_MAX));
			params.put(ServerObsTap.EXPOSURETIME, obsCoreColumns.get(ServerObsTap.EXPOSURETIME));
			params.put(ServerObsTap.TIMERESOLUTION, obsCoreColumns.get(ServerObsTap.TIMERESOLUTION));
			this.timeFieldValueOptions.put(tableName, params);
			
			if (this.spectralFieldValueOptions == null) {
				this.spectralFieldValueOptions = new HashMap<String, Map<String, String>>();
			}
			params = new HashMap<String, String>();
			params.put(EM_MIN, obsCoreColumns.get(EM_MIN));
			params.put(EM_MAX, obsCoreColumns.get(EM_MAX));
			params.put(ServerObsTap.SPECTRALRESOLUTION, obsCoreColumns.get(ServerObsTap.SPECTRALRESOLUTION));
			params.put(ServerObsTap.SPECTRALRESOLUTIONPOWER, obsCoreColumns.get(ServerObsTap.SPECTRALRESOLUTIONPOWER));
			this.spectralFieldValueOptions.put(tableName, params);
			this.addOtherParams(tableName);
		}
	}
	
	private void setConstraintsGui(String tableChoice, JPanel panelScroll) {
		// TODO Auto-generated method stub
		GridBagLayout g = new GridBagLayout();
		panelScroll.setLayout(g);
		GridBagConstraints c = new GridBagConstraints();
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.tapClient.secondColor);
		JLabel label = new JLabel();
		label.setText("Table:");
		label.setFont(BOLD);
		panel.add(label);
		Vector<String> tables = new Vector<String>(this.tapClient.obscoreTables.keySet().size());
		tables.addAll(this.tapClient.obscoreTables.keySet());
		if (tableChoice == null || !tables.contains(tableChoice)) {
			selectedTableName = tables.get(0);
		} else {
			selectedTableName = tableChoice;
		}
		Map<String, TapTable> tablesMetaData = this.tapClient.tablesMetaData;
		getColumnsToLoad(selectedTableName, tablesMetaData);
		
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		
		tablesGui = new JComboBox(tables);
		tablesGui.setToolTipText(this.tapClient.tablesMetaData.get(selectedTableName).getDescription());
		tablesGui.setEditable(true);
		JTextComponent tablesGuiEditor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
		try {
			List<String> keys = new ArrayList<String>();
			keys.addAll(this.tapClient.obscoreTables.keySet());
			tablesGuiEditor.setDocument(new FilterDocument(this, this.tablesGui, keys, selectedTableName));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			Aladin.warning(e.getMessage());
			showLoadingError();
			return;
		}
		tablesGui.setOpaque(false);
		tablesGui.setName("table");
//		tablesGui.setActionCommand(TABLECHANGED);
		
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.gridwidth = 4;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
//		tablesGui.setAlignmentY(SwingConstants.CENTER);
		panel.add(tablesGui);
		panelScroll.add(panel, c);
		
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.tapClient.secondColor);
		label = new JLabel("Select:");
		label.setFont(BOLD);
		panel.add(label);
		
		selectColumns = new JComboBox(selectAllOptions.get(selectedTableName).toArray());
		selectColumns.setPreferredSize(new Dimension(200, Server.HAUT));
		selectColumns.setActionCommand(SELECTALL);
		selectColumns.addItemListener(this);
		panel.add(selectColumns);
		
		label = new JLabel("Max rows:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.limit = new JComboBox<String>(TAP_REC_LIMIT);
		if (TAPEXDEFAULTMAXROWS != null) {
			if (!TAPEXDEFAULTMAXROWS.isEmpty()) {
				this.limit.setSelectedItem(TAPEXDEFAULTMAXROWS);
			} else {
				this.limit.setSelectedItem(TAP_REC_LIMIT_UNLIMITED);
			}
		}
		this.limit.setPreferredSize(new Dimension(80, Server.HAUT));
		this.limit.setOpaque(false);
		this.limit.addItemListener(this);
		panel.add(this.limit);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		c.gridwidth = 4;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelScroll.add(panel, c);
		
		JButton add = null;
		//Data product type
		if (dataProductTypeParamName.get(selectedTableName) != null) {
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			label = new JLabel();
			label.setText("Dataproduct type:");
			label.setFont(BOLD);
			c.weightx = 0.05;
			panelScroll.add(label, c);
			
			dataProduct_types = new JComboBox<String>(DATAPRODUCT_TYPES);
			c.weightx = 0.87;
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			panelScroll.add(dataProduct_types, c);
			
			c.weightx = 0.04;
			c.gridx++;
			dataProduct_types_andOrOp = new JComboBox(WhereGridConstraint.andOrOptions);
			panelScroll.add(dataProduct_types_andOrOp, c);
			
			c.gridx++;
			c.anchor = GridBagConstraints.WEST;
			add = new JButton("Add");
			add.setToolTipText("Click to add constraint");
			add.setActionCommand(ADD_DATAPRODUCTTYPE);
			add.addActionListener(this);
			c.weightx = 0.04;
			c.fill = GridBagConstraints.NONE;
			panelScroll.add(add, c);
		}
		
		targetPanel = new JPanel();
		targetPanel.setBackground(this.tapClient.secondColor);
		createTargetPanel(targetPanel);
		if (this.raColumnName == null || this.decColumnName == null) {
			targetPanel.setVisible(false);
		}
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		panelScroll.add(this.targetPanel, c);
		
		//Spatial constraints
		if (!spatialFieldValueOptions.get(selectedTableName).isEmpty()) {
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			spatial_fields = new JComboBox(spatialFieldValueOptions.get(selectedTableName).keySet().toArray());
			spatial_fields.setFont(BOLD);
			c.weightx = 0.05;
			panelScroll.add(spatial_fields, c);
			
			spatial_value = new JTextField();
			c.weightx = 0.87;
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			panelScroll.add(spatial_value, c);
			
			c.weightx = 0.04;
			c.gridx++;
			spatial_andOrOp = new JComboBox(WhereGridConstraint.andOrOptions);
			panelScroll.add(spatial_andOrOp, c);
			
			c.gridx++;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.WEST;
			add = new JButton("Add");
			add.setActionCommand(ADD_SPATIALCONSTRAINT);
			add.setToolTipText("Click to add constraint");
			add.addActionListener(this);
			c.weightx = 0.04;
			c.fill = GridBagConstraints.NONE;
			panelScroll.add(add, c);
		}
		
		//Spectral constraints
		if (!spectralFieldValueOptions.get(selectedTableName).isEmpty()) {
			c.gridy++;
			c.gridx = 0;
			spectral_fields = new JComboBox(spectralFieldValueOptions.get(selectedTableName).keySet().toArray());
			spectral_fields.setFont(BOLD);
			c.weightx = 0.05;
			panelScroll.add(spectral_fields, c);
			
			spectral_value = new JTextField();
			c.weightx = 0.87;
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			panelScroll.add(spectral_value, c);
			
			c.weightx = 0.04;
			c.gridx++;
			spectral_andOrOp = new JComboBox(WhereGridConstraint.andOrOptions);
			panelScroll.add(spectral_andOrOp, c);
			
			c.gridwidth = 1;
			c.gridx++;
			c.anchor = GridBagConstraints.WEST;
			add = new JButton("Add");
			add.setActionCommand(ADD_SPECTRALCONSTRAINT);
			add.setToolTipText("Click to add constraint");
			add.addActionListener(this);
			c.weightx = 0.04;
			c.fill = GridBagConstraints.NONE;
			panelScroll.add(add, c);
			
		}
		
		
		//Time constraints
		if (!timeFieldValueOptions.get(selectedTableName).isEmpty()) {
			c.gridy++;
			c.gridx = 0;
			
			time_fields = new JComboBox(timeFieldValueOptions.get(selectedTableName).keySet().toArray());
			time_fields.setFont(BOLD);
			c.weightx = 0.05;
			panelScroll.add(time_fields, c);
			
			time_value = new JTextField();
			c.weightx = 0.87;
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			panelScroll.add(time_value, c);
			
			c.weightx = 0.04;
			c.gridx++;
			time_andOrOp = new JComboBox(WhereGridConstraint.andOrOptions);
			panelScroll.add(time_andOrOp, c);
			
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.WEST;
			add = new JButton("Add");
			add.setActionCommand(ADD_TIMECONSTRAINT);
			add.setToolTipText("Click to add constraint");
			add.addActionListener(this);
			c.weightx = 0.04;
			c.gridx++;
			c.fill = GridBagConstraints.NONE;
			panelScroll.add(add, c);
		}
		
		//Free constraints
		Vector<TapTableColumn> columns = tapClient.obscoreTables.get(selectedTableName).getColumns();
		if (columns != null && !columns.isEmpty()) {
			c.gridy++;
			c.gridx = 0;
			Vector<TapTableColumn> model = new Vector<TapTableColumn>();
			model.addAll(columns);
			free_fields = new JComboBox(model);
			free_fields.setRenderer(new CustomListCellRenderer());
			free_fields.setSize(free_fields.getWidth(), Server.HAUT);
			free_fields.setFont(BOLD);
			c.weightx = 0.05;
			panelScroll.add(free_fields, c);
			
			free_value = new JTextField();
			c.weightx = 0.87;
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			panelScroll.add(free_value, c);
			
			c.weightx = 0.04;
			c.gridx++;
			free_andOrOp = new JComboBox(WhereGridConstraint.andOrOptions);
			panelScroll.add(free_andOrOp, c);
			
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.WEST;
			add = new JButton("Add");
			add.setActionCommand(ADD_FREECONSTRAINT);
			add.setToolTipText("Click to add constraint");
			add.addActionListener(this);
			c.weightx = 0.04;
			c.gridx++;
			c.fill = GridBagConstraints.NONE;
			panelScroll.add(add, c);
		}
		
		
		//Orderby rule
	/*	c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		add = new JButton("+");
		add.setToolTipText("Click to add constraint");
		add.addActionListener(this);
		c.weightx = 0.02;
		c.fill = GridBagConstraints.NONE;
		panelScroll.add(add, c);
		
		c.weightx = 0.05;
		c.gridx++;
		JComboBox andOrOperator = new JComboBox(WhereGridConstraint.andOrOptions);
		panelScroll.add(andOrOperator, c);
		
		label = new JLabel();
		label.setText("Field:");
		label.setFont(BOLD);
		c.weightx = 0.05;
		c.gridx++;
		panelScroll.add(label, c);
		
		JTextField textField = new JTextField();
		c.weightx = 0.88;
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelScroll.add(textField, c);*/
		
	}

	@Override
	void createFormDefault() {
		// TODO Auto-generated method stub
		createForm(null);
	}

	/*@Override
	public void checkTableSelectionChanged(JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		if (comboBox.getSelectedItem() != null
				&& !selectedTableName.equalsIgnoreCase(comboBox.getSelectedItem().toString())) {
			Aladin.trace(3, "Change table selection from within the document");
			tableSelectionChanged(comboBox);
//			changeTableSelection((String) comboBox.getSelectedItem());
		}
	}
	
	@Override
	public void tableSelectionChanged(JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		if (comboBox == this.tablesGui) {
			this.changeTableSelection((String) comboBox.getSelectedItem());
//			createForm((String) comboBox.getSelectedItem());
		}
	}*/
	
	@Override
	public void changeTableSelection(String tableChoice) {
		waitCursor();
		Map<String, TapTable> tablesMetaData = this.tapClient.tablesMetaData;
		Vector<TapTableColumn> columnNames = this.setTableGetColumnsToLoad(tableChoice, tablesMetaData);
		if (columnNames == null) {
			return;
		}
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		if(Aladin.levelTrace >= 3) System.out.println("ra and dec: "+(this.raColumnName!=null && this.decColumnName!=null));
		if(Aladin.levelTrace >= 3) System.out.println("and target panel: "+target);
		if  (this.raColumnName != null && this.decColumnName != null) {
			if (Aladin.levelTrace >= 3) System.out.println("target: "+(target));
			if (target == null) {
				createTargetPanel(targetPanel);
			}
			targetPanel.setVisible(true);
		} else {
			if (target != null) {
				targetPanel.setVisible(false);
			}
		}
		
		this.selectColumns.removeAllItems();
		DefaultComboBoxModel<Object> items = new DefaultComboBoxModel(selectAllOptions.get(selectedTableName).toArray());
		this.selectColumns.setModel(items);
		
		
		resetFields();
		ball.setMode(Ball.UNKNOWN);
//	    aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    writeQuery();
		this.revalidate();
		this.repaint();
		defaultCursor();
	}
	
	@Override
	protected void clear() {
		if (this.sync_async != null) {
			this.sync_async.setSelectedIndex(0);
		}
		resetFields();
		super.clear();
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * Convenience method
	 */
	protected void resetFields() {
		this.selectColumns.setSelectedIndex(0);
		this.limit.setSelectedIndex(0);
		if (this.dataProductTypeParamName.get(this.selectedTableName) != null) {
			this.dataProduct_types_andOrOp.setSelectedIndex(0);
			this.dataProduct_types.setSelectedIndex(0);
		}
		if (!this.spatialFieldValueOptions.get(this.selectedTableName).isEmpty()) {
			this.spatial_andOrOp.setSelectedIndex(0);
			this.spatial_fields.removeAllItems();
			this.spatial_fields.setModel(new DefaultComboBoxModel(spatialFieldValueOptions.get(selectedTableName).keySet().toArray()));
			this.spatial_fields.setSelectedIndex(0);
			this.spatial_value.setText(EMPTYSTRING);
			this.spatial_value.setVisible(true);
		} else {
			this.spatial_value.setVisible(false);
		}
		//Spectral constraints
		if (!this.spectralFieldValueOptions.get(this.selectedTableName).isEmpty()) {
			this.spectral_andOrOp.setSelectedIndex(0);
			this.spectral_fields.removeAllItems();
			this.spectral_fields.setModel(new DefaultComboBoxModel(spectralFieldValueOptions.get(selectedTableName).keySet().toArray()));
			this.spectral_fields.setSelectedIndex(0);
			this.spectral_value.setText(EMPTYSTRING);
		}
		if (!this.timeFieldValueOptions.get(this.selectedTableName).isEmpty()) {
			this.time_andOrOp.setSelectedIndex(0);
			this.time_fields.removeAllItems();
			this.time_fields.setModel(new DefaultComboBoxModel(timeFieldValueOptions.get(selectedTableName).keySet().toArray()));
			this.time_fields.setSelectedIndex(0);
			this.time_value.setText(EMPTYSTRING);
		}
		Vector<TapTableColumn> columns = this.tapClient.obscoreTables.get(selectedTableName).getColumns();
		if (columns != null && !columns.isEmpty() && this.free_andOrOp != null) {
			this.free_andOrOp.setSelectedIndex(0);
			this.free_fields.removeAllItems();
			Vector<TapTableColumn> model = new Vector<TapTableColumn>();
			model.addAll(columns);
			DefaultComboBoxModel combo = new DefaultComboBoxModel<TapTableColumn>(model);
			this.free_fields.setModel(combo);
			free_value.setText(EMPTYSTRING);
		}
		if (this.circleOrSquare != null) {
			this.circleOrSquare.setSelectedIndex(0);
		}
		if (this.tap != null) {
			this.tap.setText(EMPTYSTRING);
		}
	};
	
	@Override
	protected void reset() {
		this.createFormDefault();
		resetFields();
		super.reset();
		this.revalidate();
		this.repaint();
	};

	
	/**
	 * Method assembles the query from all the front end components.
	 */
	public void writeQuery() {
		try {
			StringBuffer queryFromGui = new StringBuffer("SELECT");
			if (!this.limit.getSelectedItem().equals("unlimited")) {
				queryFromGui.append(" TOP ").append(this.limit.getSelectedItem());
			}
			queryFromGui.append(SPACESTRING).append(this.selectColumns.getSelectedItem());
			
			//queryFromGui.append(((List<TapTableColumn>) this.selectList.getSelectedValuesList()).toString().replaceAll("[\\[\\]]", ""))
			queryFromGui = new StringBuffer(queryFromGui.toString().trim().replaceAll(",$", EMPTYSTRING));
			queryFromGui.append(" FROM ")
			.append(TapTable.getQueryPart(selectedTableName)).append(SPACESTRING);
			
			/*Component[] whereConstraints = this.whereClausesPanel.getComponents();
			if (this.whereClausesPanel.getComponentCount() > 0) {
				WhereGridConstraint whereConstraint;
				queryFromGui.append("WHERE ");
				for (int i = 0; i < whereConstraints.length; i++) {
					whereConstraint = (WhereGridConstraint) whereConstraints[i];
					queryFromGui.append(whereConstraint.getAdqlString());
				}
			}*/
			
			tap.setText(queryFromGui.toString());
		} catch (Exception e) {
			// TODO: handle exception
			Aladin.warning(this, e.getMessage());
            ball.setMode(Ball.NOK);
		}
	}
	
	public String getRangeDefaultString(String low, String high) {
		StringBuffer result = new StringBuffer();
		low = TapTable.getQueryPart(low);
		high = TapTable.getQueryPart(high);
		result.append(low).append(" IS NOT NULL AND ")
		.append(high).append(" IS NOT NULL ");
		return result.toString();
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		super.actionPerformed(arg0);
		Object source = arg0.getSource();
		String action = arg0.getActionCommand();
		if (source instanceof JButton) {
			String defaultValue = ColumnConstraint.defaultValue;
			boolean inRange = false;
			if (action.equals(ADD_DATAPRODUCTTYPE)) {
				appendConstraint(dataProduct_types_andOrOp, new String(dataProductTypeParamName.get(selectedTableName)),
						inRange, false, ((String) dataProduct_types.getSelectedItem()), ColumnConstraint.defaultValue);
			} else if (action.equals(ADD_SPATIALCONSTRAINT)) {
				String constraintSelected = (String) spatial_fields.getSelectedItem();
				constraintSelected = spatialFieldValueOptions.get(selectedTableName).get(constraintSelected);
				if (constraintSelected != null) {
					constraintSelected = TapTable.getQueryPart(constraintSelected);
					appendConstraint(spatial_andOrOp, constraintSelected, inRange, true, spatial_value.getText(),
							ColumnConstraint.defaultValue);
				}
			} else if (action.equals(ADD_SPECTRALCONSTRAINT)) {
				try {
					String constraintSelected = (String) spectral_fields.getSelectedItem();
					boolean processToMeters = false;
					if (constraintSelected.equals(SPECTRALRANGE)) {
						inRange = true;
						processToMeters = true;
						defaultValue = getRangeDefaultString(
								spectralFieldValueOptions.get(selectedTableName).get("em_min"),
								spectralFieldValueOptions.get(selectedTableName).get("em_max"));
					} else if (constraintSelected.equals("em_min") || constraintSelected.equals("em_max")) {
						processToMeters = true;
					}
					constraintSelected = spectralFieldValueOptions.get(selectedTableName).get(constraintSelected);
					if (constraintSelected != null) {
						StringBuffer mText;
						String valueInProcess = spectral_value.getText();
						if (processToMeters && !valueInProcess.trim().isEmpty()) {
							mText = processSpectralBand(false, valueInProcess.trim(), null);
							valueInProcess = mText.toString();
						}
						constraintSelected = TapTable.getQueryPart(constraintSelected);
						appendConstraint(spectral_andOrOp, constraintSelected, inRange, true, valueInProcess,
								defaultValue);
					}
				} catch (NumberFormatException e) {
					Aladin.warning(this, "Error! "+e.getMessage());
					ball.setMode(Ball.NOK);
				} catch (Exception e) {
					Aladin.warning(this, "Error! "+e.getMessage());
					ball.setMode(Ball.NOK);
				}
			} else if (action.equals(ADD_TIMECONSTRAINT)) {
				try {
					boolean processToMjd = false;
					String constraintSelected = (String) time_fields.getSelectedItem();
					if (constraintSelected.equals(TIMERANGE)) {
						inRange = true;
						processToMjd = true;
						defaultValue = getRangeDefaultString(
								timeFieldValueOptions.get(selectedTableName).get("t_min"),
								timeFieldValueOptions.get(selectedTableName).get("t_max"));
					} else if (constraintSelected.equals("t_min") || constraintSelected.equals("t_max")) {
						processToMjd = true;
					}
					constraintSelected = timeFieldValueOptions.get(selectedTableName).get(constraintSelected);
					if (constraintSelected != null) {
						StringBuffer mjdText;
						String valueInProcess = time_value.getText();
						if (processToMjd) {
							mjdText = setDateInMJDFormat(false, valueInProcess, null, SPACESTRING);
							if (mjdText != null) {
								valueInProcess = mjdText.toString();
							}
						}
						constraintSelected = TapTable.getQueryPart(constraintSelected);
						appendConstraint(time_andOrOp, constraintSelected, inRange , true, valueInProcess, defaultValue);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Aladin.warning(this, "Error! "+e.getMessage());
	                ball.setMode(Ball.NOK);
	                return;
				}
			} else if (action.equals(ADD_FREECONSTRAINT)) {
				TapTableColumn columnSelected = (TapTableColumn) free_fields.getSelectedItem();
				boolean processAsNumber = true;
				if (columnSelected != null && columnSelected.getColumn_name() != null
						&& !columnSelected.getColumn_name().isEmpty()) {
					String dataType = columnSelected.getDatatype();
					if (dataType != null && dataType.toUpperCase().contains("VARCHAR")) {
						processAsNumber = false;
					}
					String constraintSelected = columnSelected.getColumnNameForQuery();
					appendConstraint(free_andOrOp, constraintSelected, inRange, processAsNumber,
							free_value.getText(), defaultValue);
				}
			} else if (action.equals(ADDPOSCONSTRAINT)) {
				String objet = null;
				if( target != null ) {
			         try {
			        	objet = resolveQueryField();
			            if( objet == null) throw new Exception(UNKNOWNOBJ);
			            ball.setMode(Ball.OK);
			         } catch( Exception e1 ) {
			            Aladin.warning(this, e1.getMessage());
			            ball.setMode(Ball.NOK);
			            return;
			         }
			      }
				if (objet.trim().equals(TARGETNAN)) {//resolve obj for empty field
					Aladin.warning(this, TapClient.TARGETERROR);
		            ball.setMode(Ball.NOK);
		            return;
				}
				StringBuffer whereClause = new StringBuffer();
				String raColumNameForQuery = TapTable.getQueryPart(this.raColumnName);
				String decColumNameForQuery = TapTable.getQueryPart(this.decColumnName);
				whereClause.append(String.format(POSQuery, raColumNameForQuery, decColumNameForQuery,
						coo[0].getText(), coo[1].getText(), rad[0].getText())).append(SPACESTRING);
				appendConstraint(free_andOrOp, null, true, false, null, whereClause.toString());
				
			} else if (action.equals(CHECKQUERY)) {
				checkQueryFlagMessage();//TODO:: goes in dynamic form!
			} else if (action.equals(WRITEQUERY)) {
				this.writeQuery();
			}
		}
	}
	
	/**
	 * Essentially calls checkQuery to know written query validity.
	 * @param arrayList
	 */
	public void appendConstraint(JComboBox andOrOp, String constraint, boolean inRange, boolean processAsNumber,
			String value, String defaultValue) {
		ADQLQuery query = null;
		try {
			query = this.checkQuery();
		} catch (UnresolvedIdentifiersException uie) {
			// TODO Auto-generated catch block
			Iterator<adql.parser.ParseException> it = uie.getErrors();
			adql.parser.ParseException ex = null;
			while (it.hasNext()) {
				ex = it.next();
				highlightQueryError(tap.getHighlighter(), ex);
			}
			try {
				query = checkSyntax();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (query != null) {
			ClauseConstraints whereConstraints = query.getWhere();
			boolean firstConstraint = false;
			StringBuffer queryInput = new StringBuffer(tap.getText().trim());
			if (whereConstraints == null || whereConstraints.size() == 0) {
				queryInput.append(SPACESTRING).append(" WHERE");
				firstConstraint = true;
			}
			if (!firstConstraint) {
				// append the andOr operator
				queryInput.append(SPACESTRING);
				if (andOrOp == null) {
					queryInput.append("AND");
				} else {
					queryInput.append(andOrOp.getSelectedItem());
				}
				queryInput.append(SPACESTRING);
			}
			
			String processedInput = null;
			if (value == null || value.isEmpty()) {// tintin below if condn
				if (inRange) {
					processedInput = defaultValue;
				} else {
					processedInput = defaultValue;//constraint+SPACESTRING+defaultValue;
				}
			} else {
				if (processAsNumber) {
					if (inRange) {
						processedInput = TapClient.getRangeInput(value, constraint);
						if (processedInput == null || processedInput.isEmpty()) {
							Aladin.warning(this, NORANGEERRORMESSAGE);
							this.ball.setMode(Ball.NOK);
							return;
						}
					} else {
						processedInput = TapClient.getRangeInput(value, null);
					}
					
					if (processedInput == null || processedInput.isEmpty()) {
						processedInput = TapClient.isInValidOperatorNumber(this, value, true);
					}
				} else {
					processedInput = TapClient.getStringInput(value, true);
					if (processedInput == null || processedInput.isEmpty()) {
						Aladin.warning(this, value+" is invalid input, please check");
						this.ball.setMode(Ball.NOK);
						return;
					}
				}
			}

			if (processedInput != null && !processedInput.isEmpty()) {
				if (!inRange && constraint != null) {
					processedInput = constraint+SPACESTRING+processedInput;
				}
				queryInput.append(SPACESTRING).append(processedInput).append(SPACESTRING);
				tap.setText(queryInput.toString());
				ball.setMode(Ball.OK);
			}

		}

	}
	
	/**
	 * Method fires sync or async submissions based on what is selected by user
	 * Incase of upload as file is posted, diff sync methods are followed
	 * @param requestParams
	 */
	public void submit(Map<String, Object> requestParams) {
	      //check again
		if (this.sync_async != null &&  this.tap != null) {
			boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
	  	  	this.submitTapServerRequest(sync, requestParams, this.tapClient.tapLabel, this.tapClient.tapBaseUrl, this.tap.getText());
		}
	}
	
	@Override
	public void submit() {
		submit(null);//no request params
	}
	
	/**
	 * Method parses adql query from user using Grégory
	 * Mantelet's (ARI/ZAH) adql parser lib
	 * No tap meta is set. only checking syntax.
	 * @return the adql query
	 * @throws Exception 
	 */
	public ADQLQuery checkSyntax() throws Exception {
		if (tap.getText().isEmpty()) {
			throw new Exception(CHECKQUERY_ISBLANK);
		}
		ADQLQuery query = null;
		ADQLParser syntaxParser = new ADQLParser();
		try {
			query = syntaxParser.parseQuery(tap.getText());
		} catch (UnresolvedIdentifiersException ie) {	
			Aladin.trace(3, "Number of errors in the query:"+ie.getNbErrors());
			throw ie;
		} catch (adql.parser.ParseException pe) {
			throw pe;
		} catch (TokenMgrError e) {
			// TODO: handle exception
			throw e;
		}
		return query;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		writeQuery();
	}
	
	protected void createChaine() {
		super.createChaine();
		description = Aladin.chaine.getString("TAPFORMINFO");
		title = Aladin.chaine.getString("TAPFORMTITLE");
		verboseDescr = loadedServerDescription = Aladin.chaine.getString("TAPOBSFORMDESC");
		TIPCLICKTOADD = Aladin.chaine.getString("TIPCLICKTOADD");
		NORANGEERRORMESSAGE = Aladin.chaine.getString("NORANGEERRORMESSAGE");
	}

}
