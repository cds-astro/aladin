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

import static cds.aladin.Constants.ADDPOSCONSTRAINT;
import static cds.aladin.Constants.ADDWHERECONSTRAINT;
import static cds.aladin.Constants.CHANGESERVER;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.CIRCLEORSQUARE;
import static cds.aladin.Constants.COMMA_SPACECHAR;
import static cds.aladin.Constants.DISCARDACTION;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.OPEN_SET_RADEC;
import static cds.aladin.Constants.REGEX_TABLENAME_SPECIALCHAR;
import static cds.aladin.Constants.RELOAD;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;
import static cds.aladin.Constants.RETRYACTION;
import static cds.aladin.Constants.SELECTALL;
import static cds.aladin.Constants.SHOWAYNCJOBS;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.SYNC_ASYNC;
import static cds.aladin.Constants.TAPFORM_STATUS_ERROR;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTLOADED;
import static cds.aladin.Constants.TAP_REC_LIMIT;
import static cds.aladin.Constants.TARGETNAN;
import static cds.aladin.Constants.UPLOAD;
import static cds.aladin.Constants.WRITEQUERY;
import static cds.tools.CDSConstants.BOLD;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import cds.aladin.Constants.TapServerMode;
import cds.tools.Util;
import cds.xml.VOSICapabilitiesReader;

public class ServerTap extends Server implements MouseListener{

	private static final long serialVersionUID = 1L;
	public static String LOAD, TAPTABLEJOINTIP, TAPTABLEUPLOADTIP, TARGETERROR, TIPCLICKTOADD, TAPTABLENOUPLOADTIP, GENERICERROR,
			REFRESHQUERYTOOLTIP, CHECKQUERYTOOLTIP, SYNCASYNCTOOLTIP, SHOWASYNCTOOLTIP, DISCARD, DISCARDTIP, RETRY,
			TIPRETRY, CHANGESERVERTOOLTIP, CLIENTINSTR;
	
	protected TapManager tapManager = null;
	
	//metadata
	private String name;
	private String url;
	Map<String, TapTable> tablesMetaData;
	Future<VOSICapabilitiesReader> capabilities;
    public String nodeName = null;//tintin:: TODO::for future
    public static Map<String, DBDatatype> DBDATATYPES = new HashMap();
	List<DefaultDBTable> queryCheckerTables;
	Color primaryColor = Aladin.BLUE;
    Color secondColor = new Color(198,218,239);
    
    //these data are serverTap specific
    protected int formLoadStatus; 
    private String raColumnName;
	private String decColumnName;
	
	//gui
	public Future<JPanel> infoPanel;
	static final int DEFAULT_INFO_TABLE_HEIGHT = 115;
	JComboBox tablesGui;
	JList selectList;
	JCheckBox selectAll;
	JPanel whereClausesPanel;
	List<ColumnConstraint> whereClauses;
	String selectedTableName;
	JComboBox<Integer> limit;
	JComboBox<String> sync_async;
	JComboBox<String> circleOrSquare;
	JPanel queryComponentsGui;
	JPanel targetPanel;
    JFrame setRaDecFrame;
    JLabel info1;
    
	static{
		for (DBDatatype dbDatatype : DBDatatype.values()) {
			DBDATATYPES.put(dbDatatype.name(), dbDatatype);
		}
	}
	
	protected ServerTap(Aladin aladin) {
		this.aladin = aladin;
		formLoadStatus = TAPFORM_STATUS_NOTLOADED;
		createChaine();
		type = CATALOG;
		aladinLabel = "TAP";
//		aladinLogo = "tap.png";
		this.adqlParser = new ADQLParser();
		this.tapManager = TapManager.getInstance(aladin);
	}
	
	protected void createFormDefault() {
		this.createForm(null);
	}
	
	/**
	 * Creation of the tap query panel
	 * @param tablesMetaData
	 * @param tableChoice
	 */
	protected void createForm(String tableChoice) {
		Vector<String> tables = new Vector<String>(this.tablesMetaData.keySet().size());
		tables.addAll(this.tablesMetaData.keySet());
		if (tableChoice == null || !tables.contains(tableChoice)) {
			selectedTableName = tables.get(0);
		} else {
			selectedTableName = tableChoice;
		}
		Vector<TapTableColumn> columnNames = this.tablesMetaData.get(selectedTableName).getColumns();
		if (columnNames == null) {
			if (mode == TapServerMode.UPLOAD) {
				Aladin.warning("Error in uploaded data");
				return;
			}
			try {
				columnNames = tapManager.updateTableColumnSchemas(this, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				return;
			}
			if (columnNames == null) {
				Aladin.warning("Error in updating the metadata for :"+selectedTableName);
				return;
			}
		}
		setQueryChecker();
		
		JButton button;
		type = CATALOG;

		setLayout(new BorderLayout());
		setOpaque(true);
		this.setBackground(this.primaryColor);
		setFont(Aladin.PLAIN);
		
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		
		JPanel containerPanel = new JPanel(new GridBagLayout());
		containerPanel.setBackground(this.primaryColor);
	    
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		
		JPanel titlePanel = new JPanel();
		titlePanel.setBackground(this.primaryColor);
		titlePanel.setAlignmentY(SwingConstants.CENTER);
		if (mode != TapServerMode.UPLOAD) {
			makeTitle(titlePanel, getVisibleLabel());
//			this.aladinLabel = this.name;
		} else {
			makeTitle(titlePanel, "Upload server");
		}
		
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.CENTER;
	    c.gridx = 0;
	    c.weighty = 0.02;
	    c.weightx = 0.99;
		containerPanel.add(titlePanel, c);
		
		JPanel optionsPanel = new JPanel();
		if (this.tapClient != null) {
			optionsPanel = this.tapClient.getOptionsPanel(this);
			if (this.tapClient.serverGlu == null && this.modeChoice != null) {
				this.modeChoice.setVisible(false);
			}
		}
		
		c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 1;
	    c.weightx = 0.01;
	    optionsPanel.setBackground(this.primaryColor);
		containerPanel.add(optionsPanel, c);
		
		// Premiere indication
		info1 = new JLabel(CLIENTINSTR);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
	    c.weighty = 0.02;
	    info1.setHorizontalAlignment(SwingConstants.CENTER);
	    containerPanel.add(info1, c);
	    c.gridy++;
	    
	    
		JPanel tablesPanel = null;
		try {
			tablesPanel = getTablesPanel(tableChoice, tables);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			Aladin.warning(e.getMessage());
			return;
		}
		tablesPanel.setBackground(this.primaryColor);
		tablesPanel.setFont(BOLD);
		c.weighty = 0.02;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0.10;
	    c.fill = GridBagConstraints.NONE;
	    c.insets = new Insets(0, 4, 0, 0);
	    containerPanel.add(tablesPanel, c);		
		
	    this.queryComponentsGui = new JPanel();
		setWhereAddConstraintsGui(columnNames);
		this.queryComponentsGui.setBackground(this.primaryColor);
		this.queryComponentsGui.setFont(BOLD);
		c.weighty = 0.51;
		c.weightx = 0.40;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(this.queryComponentsGui, c);	
		
		JPanel linePanel = getBottomPanel();
		c.weightx = 0.10;
		c.weighty = 0.02;
		c.insets = new Insets(0, -6, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(linePanel, c);
	    
	    
	    JScrollPane scrolley = null;
		tap = new JTextArea(8, 100);//"", 8, 50
		tap.setFont(Aladin.ITALIC);
		tap.setWrapStyleWord(true);
		tap.setLineWrap(true);
		tap.setEditable(true);
		scrolley = new JScrollPane(tap);
		c.weightx = 0.35;
		c.weighty = 0.35;

		c.insets = new Insets(0, 4, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(scrolley, c);

	    this.removeAll();
	    add(containerPanel);
	    
	    this.addMouseListener(this);
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    writeQuery();
	    
	}
	
	
	private String getVisibleLabel() {
		// TODO Auto-generated method stub
		String results = this.name;
		if (this.name.equalsIgnoreCase(this.url)) {
			try {
				results = Util.getDomainNameFromUrl(this.url);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		return results;
	}

	protected void changeTableSelection(String tableChoice) {
		waitCursor();
		Vector<String> tables = new Vector<String>(this.tablesMetaData.keySet().size());
		tables.addAll(this.tablesMetaData.keySet());
		if (tableChoice == null || !tables.contains(tableChoice)) {
			selectedTableName = tables.get(0);
		} else {
			selectedTableName = tableChoice;
		}
		Vector<TapTableColumn> columnNames = this.tablesMetaData.get(selectedTableName).getColumns();
		tablesGui.setToolTipText(tablesMetaData.get(selectedTableName).getDescription());
		if (columnNames == null) {
			if (mode == TapServerMode.UPLOAD) {
				Aladin.warning("Error in uploaded data");
				return;
			}
			try {
				columnNames = tapManager.updateTableColumnSchemas(this, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				String revertTable = tables.get(0);
				if (this.tablesMetaData.get(revertTable).getColumns() != null) {
					JTextComponent tablesGuiEditor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
					TapTableFilterDocument tapTableFilterDocument = (TapTableFilterDocument) tablesGuiEditor
							.getDocument();
					try {
						tapTableFilterDocument.setDefaultTable();//trying to select default table till here
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
				return;
			}
			if (columnNames == null) {
				Aladin.warning("Error in updating the metadata for :"+selectedTableName);
				showLoadingError();
				defaultCursor();
				return;
			}
			updateQueryChecker(this.selectedTableName);
		}
		
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		if(Aladin.levelTrace >= 3) System.out.println("ra and dec: "+(this.raColumnName!=null && this.decColumnName!=null));
		if(Aladin.levelTrace >= 3) System.out.println("and target panel: "+target);
		if  (this.raColumnName != null && this.decColumnName != null) {
			if (Aladin.levelTrace >= 3) System.out.println("target: "+(target));
			if (target == null) {
				createTargetPanel();
			}
			targetPanel.setVisible(true);
		} else {
			if (target != null) {
				targetPanel.setVisible(false);
			}
		}
		
		this.selectList.removeAll();
		this.selectList.setListData(columnNames);
		
		if (this.selectAll != null) {
			this.selectAll.setSelected(true);
		}
		
		resetFields();
		ball.setMode(Ball.UNKNOWN);
//	    aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    writeQuery();
		this.revalidate();
		this.repaint();
		defaultCursor();
	}
	
	/**
	 * Tap client gui in case when it is still loading
	 */
	protected void showloading() {
		this.setBackground(this.primaryColor);
		JLabel planeLabel = new JLabel("loading "+this.name+"...");
		planeLabel.setFont(Aladin.ITALIC);
		add(planeLabel,"Center");
		if (this.tapClient != null && this.tapClient.mode == TapClientMode.DIALOG) {
			JButton button = ServerTap.getChangeServerButton();
			button.addActionListener(this);
			add(button);
		}
		revalidate();
		repaint();
	}
	
	/**
	 * Tap client gui in case of loading error
	 */
	protected void showLoadingError() {
		this.setBackground(this.primaryColor);
		this.removeAll();
		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		JLabel planeLabel = new JLabel("Error: unable to load "+this.name);
		planeLabel.setFont(Aladin.ITALIC);
		add(planeLabel);
		if (this.mode != TapServerMode.UPLOAD) {
			
			if (this.tapClient != null && this.tapClient.mode == TapClientMode.DIALOG) {
				JButton button = ServerTap.getChangeServerButton();
				button.addActionListener(this);
				add(button);
			}
			
			JButton reloadButton = null;
			Image image = Aladin.aladin.getImagette("reload.png");
			if (image == null) {
				reloadButton = new JButton(RETRY);
			} else {
				reloadButton = new JButton(new ImageIcon(image));
			}
			reloadButton.setBorderPainted(false);
			reloadButton.setMargin(new Insets(0, 0, 0, 0));
			reloadButton.setContentAreaFilled(true);
			reloadButton.setActionCommand(RELOAD);
			reloadButton.setToolTipText(TIPRETRY);
			reloadButton.addActionListener(this);
			add(reloadButton);
		}
		
		formLoadStatus = TAPFORM_STATUS_ERROR;
		revalidate();
		repaint();
	}
	
	/**
	 * Creates the table selection panel with tables drop-down, upload button etc..
	 * @param tableChoice
	 * @param tables
	 * @return
	 * @throws BadLocationException 
	 */
	public JPanel getTablesPanel(String tableChoice, Vector<String> tables) throws BadLocationException {
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
		
		tablesGui = new JComboBox(tables);
		tablesGui.setToolTipText(tablesMetaData.get(selectedTableName).getDescription());
		tablesGui.setEditable(true);
		JTextComponent tablesGuiEditor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
		tablesGuiEditor.setDocument(new TapTableFilterDocument(this));
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
		
		if (mode == TapServerMode.UPLOAD) {
			c.weightx = 0.05;
			c.gridx++;
			JButton button = new JButton(DISCARD);
			button.setActionCommand(DISCARDACTION);
			button.addActionListener(tapManager.uploadFrame);
			button.setToolTipText(DISCARDTIP);
			tablesPanel.add(button, c);
		}
		
		JButton button = new JButton("Set ra, dec");
		button.setActionCommand(OPEN_SET_RADEC);
		button.addActionListener(this);
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
		
		return tablesPanel;
	}
	
	/**
	 * Creates where constraints panel. Displays columns, where constraints addition interface(Column and position constraints)
	 * @param columnNames
	 */
	public void setWhereAddConstraintsGui(Vector<TapTableColumn> columnNames) {
		// TODO Auto-generated method stub
		this.queryComponentsGui.removeAll();
		GridBagLayout gridbag = new GridBagLayout();
		this.queryComponentsGui.setLayout(gridbag);
		this.queryComponentsGui.setBackground(this.primaryColor);
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.secondColor);
		JLabel label = new JLabel("Select:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.selectAll = new JCheckBox("All", true);
		this.selectAll.setActionCommand(SELECTALL);
		this.selectAll.addActionListener(this);
		panel.add(selectAll);
		
		if (columnNames != null) {
			this.selectList = new JList(columnNames);
			this.selectList.setSelectionInterval(0, columnNames.size()-1);
			this.selectList.setCellRenderer(new TapTableColumnRenderer());
			this.selectList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					// TODO Auto-generated method stub
					if (selectList.getSelectedValuesList().size() != selectList.getModel().getSize()) {
						selectAll.setSelected(false);
					} else {
						selectAll.setSelected(true);
					}
					writeQuery();
				}
			});
		}
		
		
		JScrollPane scrolley = new JScrollPane(this.selectList);
		this.selectList.setVisibleRowCount(4);
		this.selectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.20;
		c.weighty = 0.02;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, -5, 0, 0);
		this.queryComponentsGui.add(panel, c);
		
		c.gridy = 1;
		c.weighty = 0.98;
		c.gridheight = 2;
		c.insets = new Insets(0, 0, 0, 0);
		this.queryComponentsGui.add(scrolley, c);
		
		c.gridy = 0;
		c.gridx = 1;
		c.weightx = 0.80;
		c.weighty = 0.02;
		c.gridheight = 1;
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.secondColor);
		label = new JLabel("Constraints:");
		label.setFont(BOLD);
		panel.add(label);
		
		JButton button = new JButton("Add new");
		button.setActionCommand(ADDWHERECONSTRAINT);
		button.addActionListener(this);
		panel.add(button);
		
		panel.add(Box.createHorizontalStrut(15));
		
		label = new JLabel("Max rows:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.limit = new JComboBox<Integer>(TAP_REC_LIMIT);
		this.limit.setOpaque(false);
		this.limit.addItemListener(new ItemListener() {
	         public void itemStateChanged(ItemEvent e) {
	        	 writeQuery();
	          }
	       });
		panel.add(this.limit);
		
		this.queryComponentsGui.add(panel, c);
		
		c.gridy = 1;
		c.weighty = 0.98;
		
		targetPanel = new JPanel();
		targetPanel.setBackground(this.primaryColor);
		Aladin.trace(3, "ra and dec at createForm "+(this.raColumnName!=null && this.decColumnName!=null));
		
//		if (this.raColumnName != null && this.decColumnName != null) {
//			createTargetPanel();// TAPALADIN
//		} else {
//			targetPanel.setVisible(false);
//		}
		createTargetPanel();
		if (this.raColumnName == null || this.decColumnName == null) {
			targetPanel.setVisible(false);
		}
		c.weighty = 0.20;
		c.gridheight = 1;
		this.queryComponentsGui.add(targetPanel, c);
		c.gridy = 2;
		c.weighty = 0.78;
		
		this.whereClausesPanel = new JPanel();
		this.whereClausesPanel.setLayout(new BoxLayout(this.whereClausesPanel, BoxLayout.Y_AXIS));
		this.whereClausesPanel.setBackground(this.secondColor);
		this.whereClausesPanel.setName("WHERECLAUSES");
		
		JScrollPane constraintsScroller = new JScrollPane();
		constraintsScroller.setViewportView(this.whereClausesPanel);
		constraintsScroller.getVerticalScrollBar().setUnitIncrement(4);
		
		this.queryComponentsGui.add(constraintsScroller, c);
	}
	
	/**
	 * Lower buttons panel, just above the tap query text area
	 * @return
	 */
	public JPanel getBottomPanel() {
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bottomPanel.setBackground(this.primaryColor);
		JButton button = new JButton("Refresh query");
		button.setToolTipText(REFRESHQUERYTOOLTIP);
		button.setActionCommand("WRITEQUERY");
		button.addActionListener(this);
		bottomPanel.add(button);
		
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
		
		if (mode != TapServerMode.UPLOAD) {
			String uploadTipText = TAPTABLEUPLOADTIP;
			button = new JButton("Upload");
			button.setActionCommand(UPLOAD);
			if (this.capabilities != null) {
				try {
					VOSICapabilitiesReader meta = this.capabilities.get();
					button.setEnabled(meta.isUploadAllowed());
					button.addActionListener(this);
					if (meta.isUploadAllowed() && meta.getUploadHardLimit() > 0L) {
						String tip= String.format("Hard limit =%1$s rows", meta.getUploadHardLimit());
						uploadTipText = uploadTipText.concat(tip);
					} else if (!meta.isUploadAllowed()) {
						uploadTipText = TAPTABLENOUPLOADTIP;
					}
					button.setToolTipText(uploadTipText);
					bottomPanel.add(button);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if( Aladin.levelTrace >= 3 ) e.printStackTrace();//Do nothing, no upload button will be added
				}
			}
		}
		
		return bottomPanel;
	}
	
	/**
	 * Creates the 
	 * @param targetPanel
	 */
	protected void createTargetPanel(){
		this.targetPanel.removeAll();
		GridBagLayout gridbag = new GridBagLayout();
		this.targetPanel.setLayout(gridbag);
		this.targetPanel.setFont(BOLD);
		
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
		this.targetPanel.add(label);

		target = new JTextField(40);
        target.addKeyListener(this);
        target.addActionListener(this);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.80;
		gridbag.setConstraints(target, c);
		this.targetPanel.add(target);

		grab = new JToggleButton("Grab");
		Insets m = grab.getMargin();
        grab.setMargin(new Insets(m.top,2,m.bottom,2));
        grab.setOpaque(false);
		grab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aladin.f.toFront();
				JPanel server = ServerTap.this;
				aladin.grabUtilInstance.grabFrame = (GrabItFrame) SwingUtilities.getRoot(server);
       	   
              /*aladin.dialog.startGrabIt();
              if (aladin.additionalServiceDialog!=null) {
            	  aladin.additionalServiceDialog.startGrabIt();
			}*/
              
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
		this.targetPanel.add(grab);

		String radText=RAD;
		label= new JLabel(addDot(radText));
		label.setFont(Aladin.BOLD);
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.weightx = 0.10;
		gridbag.setConstraints(label, c);
		this.targetPanel.add(label);
		
		radius = new JTextField(50);
		radius.addKeyListener(this);
		radius.addActionListener(this);
		c.gridx = 1;
		c.weightx = 0.80;
		gridbag.setConstraints(radius, c);
		this.targetPanel.add(radius);
		
		this.circleOrSquare = new JComboBox<String>(CIRCLEORSQUARE);
		this.circleOrSquare.setOpaque(false);
		this.circleOrSquare.setName("posConstraintShape");
//		this.circleOrSquare.setActionCommand(POSCONSTRAINTSHAPECHANGED);
		this.circleOrSquare.setSelectedIndex(0);
//		this.circleOrSquare.addActionListener(this);
		c.weightx = 0.05;
		c.gridx = 2;
		gridbag.setConstraints(this.circleOrSquare, c);
		this.targetPanel.add(this.circleOrSquare);
		
		JButton button = new JButton("Add");
		button.setActionCommand(ADDPOSCONSTRAINT);
		button.addActionListener(this);
		button.setToolTipText(TIPCLICKTOADD);
		c.weightx = 0.05;
		c.gridx = 3;
		gridbag.setConstraints(button, c);
		this.targetPanel.add(button);
		
		Util.toolTip(label, RADIUS_EX);
	    Util.toolTip(radius, RADIUS_EX);
	    
		modeCoo=RADEd; //just ra and dec
		modeRad=STRINGd;
		if( coo==null ) {
			coo = new JTextField[2];
	        coo[0] = new JTextField();
	        coo[1] = new JTextField();
		}
		if (rad==null) {
			rad = new JTextField[2];
	        rad[0] = new JTextField();
	        rad[1] = new JTextField();
		}
		this.targetPanel.setVisible(true);
	}

	/**
	 * Method that set the gui of a where contraint
	 * @param whereClause
	 * @param firstGridEle
	 * @param secondGridEle
	 * @param thirdGridEle
	 */
	public void addWhereConstraint(WhereGridConstraint whereConstraintPanel) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.anchor = GridBagConstraints.NORTH;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.gridy = 0;
		
		//Col-- start
				
		//and/or option
		gridBagConstraints.gridx = 0;
		gridBagConstraints.weightx = 0.05;
		gridbag.setConstraints(whereConstraintPanel.andOrOperator, gridBagConstraints);

		//column names drop box or ra query
		gridBagConstraints.gridx = 1;
		gridBagConstraints.weightx = 0.45;
		gridbag.setConstraints(whereConstraintPanel.firstGridComponent, gridBagConstraints);
		
		//where operators or dec query
		gridBagConstraints.gridx = 2;
		gridBagConstraints.weightx = 0.05;
		gridbag.setConstraints(whereConstraintPanel.secondGridComponent, gridBagConstraints);
		
		//value for constraint or radius query
		gridBagConstraints.gridx = 3;
		gridBagConstraints.weightx = 0.42;
		gridbag.setConstraints(whereConstraintPanel.thirdGridComponent , gridBagConstraints);
		//whereClausesPanel.add(constraintValue);
		
		//remove button
		gridBagConstraints.gridx = 4;
		gridBagConstraints.weightx = 0.03;
		whereConstraintPanel.removeButton.addActionListener(this);
		gridbag.setConstraints(whereConstraintPanel.removeButton, gridBagConstraints);
		
		
		// Col-- end
		whereConstraintPanel.setBackground(this.secondColor);
		whereConstraintPanel.addWhereConstraints();
		this.whereClausesPanel.add(whereConstraintPanel, Component.TOP_ALIGNMENT);
		ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
		
//		this.queryComponentsGui.setBounds(XTAB1, addConstraintY, XWIDTH, 250);
		revalidate();
		repaint();
	}
	
	@Override
	protected void showStatusReport() {
		if (aladin.frameInfoServer == null || !aladin.frameInfoServer.isOfTapServerType()
				|| !aladin.frameInfoServer.getServer().equals(this)) {
			if (aladin.frameInfoServer != null) {
				aladin.frameInfoServer.dispose();
			}
			if (this.infoPanel != null) {// new server
				aladin.frameInfoServer = new FrameInfoServer(aladin, this.infoPanel);
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
	
	/**
	 * Method to update the frame info panel.
	 * 	if the old frame is visible the method reloads it.
	 * @param newInfoPanel
	 */
	protected void tackleFrameInfoServerUpdate(Future<JPanel> newInfoPanel) {
		try {
			FrameInfoServer frameInfoServer = null;
			if (this.infoPanel!=null) {
				if (this.infoPanel.isDone()) {
					JPanel infoPanel = this.infoPanel.get();
					frameInfoServer = (FrameInfoServer) SwingUtilities.getRoot(infoPanel);
					if (frameInfoServer!=null) {
						frameInfoServer.setAdditionalComponent(newInfoPanel);
						if (frameInfoServer.isVisible()) {
							frameInfoServer.updateInfoPanel();
							frameInfoServer.revalidate();
							frameInfoServer.repaint();
						} else {
							frameInfoServer.setFlagUpdate(1);
						}
					}
				} else {
					this.infoPanel.cancel(true);
				}
			} else if (aladin.frameInfoServer != null && aladin.frameInfoServer.getServer().equals(this)) {
				//this else part is for a specific case where generic status report is displayed before table meta can be obtained
				frameInfoServer = new FrameInfoServer(aladin, newInfoPanel);
				if (aladin.frameInfoServer.isVisible()) {
					frameInfoServer.updateInfoPanel();
					aladin.frameInfoServer.dispose();
					aladin.frameInfoServer = frameInfoServer;
					frameInfoServer.revalidate();
					frameInfoServer.repaint();
					frameInfoServer.show(this);
				} else {
					frameInfoServer.setFlagUpdate(1);
			}
			}
			this.infoPanel = newInfoPanel;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		}

	}

	/**
	 * Method assembles the query from all the front end components.
	 */
	public void writeQuery() {
		try {
			StringBuffer queryFromGui = new StringBuffer();
			queryFromGui.append("SELECT TOP ").append(this.limit.getSelectedItem());
			if (this.selectAll.isSelected()) {
				queryFromGui.append(" *");
			} else {
				List<TapTableColumn> selectedColumns = (this.selectList.getSelectedValuesList());
				if (selectedColumns == null || selectedColumns.isEmpty()) {
					queryFromGui.append(" *");
				} else {
					queryFromGui.append(SPACESTRING);
					for (TapTableColumn selectedColumn : selectedColumns) {
						queryFromGui.append(selectedColumn.getColumn_name()).append(COMMA_SPACECHAR);
					}
				}
				
			}
			//queryFromGui.append(((List<TapTableColumn>) this.selectList.getSelectedValuesList()).toString().replaceAll("[\\[\\]]", ""))
			queryFromGui = new StringBuffer(queryFromGui.toString().trim().replaceAll(",$", EMPTYSTRING));
			queryFromGui.append(" FROM ")
			.append(getQueryPart(selectedTableName)).append(SPACESTRING);
			
			Component[] whereConstraints = this.whereClausesPanel.getComponents();
			if (this.whereClausesPanel.getComponentCount() > 0) {
				WhereGridConstraint whereConstraint;
				queryFromGui.append("WHERE ");
				for (int i = 0; i < whereConstraints.length; i++) {
					whereConstraint = (WhereGridConstraint) whereConstraints[i];
					queryFromGui.append(whereConstraint.getAdqlString());
				}
			}
			
			tap.setText(queryFromGui.toString());
			ball.setMode(Ball.OK);
		} catch (Exception e) {
			// TODO: handle exception
			Aladin.warning(this, e.getMessage());
            ball.setMode(Ball.NOK);
		}
		
	}
	
	/**
	 * Method only used for cases of table names with special chars.
	 * Adds double quote to the names
	 * @param queryPartInput
	 * @return
	 */
	public static String getQueryPart(String queryPartInput) {
		Pattern regex = Pattern.compile(REGEX_TABLENAME_SPECIALCHAR);
		Matcher matcher = regex.matcher(queryPartInput);
		if (matcher.find()){
			queryPartInput = Glu.doubleQuote(queryPartInput);
		}
		return queryPartInput;
	}
	
	/*@Override
	public ADQLQuery checkQuery() {
		// TODO Auto-generated method stub
		return super.checkQuery();
		if (query == null && unrecognisedParams!=null && !unrecognisedParams.isEmpty()) {
			for (String tableName : unrecognisedParams) {
				updateQueryChecker(tableName);
			}
		}
		return query;
	}*/
	
	@Override
	public ADQLQuery checkQuery()  throws UnresolvedIdentifiersException {
		ADQLQuery query = null;
		try {
			query = super.checkQuery();
		} catch (UnresolvedIdentifiersException uie) {	
			Aladin.trace(3, "Number of errors in the query: "+uie.getNbErrors());
			adql.parser.ParseException ex = null;
			try {
				List<String> tableNames = getTableNamesofNoMetadataInQuery(tap.getText());
				if (tableNames != null && !tableNames.isEmpty()) {
					try {
						tapManager.updateTableColumnSchemas(this, tableNames);
						updateQueryChecker(tableNames);
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
				tapManager.eraseNotification(info1, description);
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
				TapTable meta = this.tablesMetaData.get(tableNameKey);
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
	
	/**
	 * Method sets the database metadata to the parser
	 */
	public void setQueryChecker() {
		if (this.queryCheckerTables == null) {//initialise for the first time
			this.queryCheckerTables = new ArrayList<DefaultDBTable>();
		} else {
			this.queryCheckerTables.clear();
		}
		for (Entry<String, TapTable> metaDataEntry : tablesMetaData.entrySet()) {
			DefaultDBTable parserTable = new DefaultDBTable(metaDataEntry.getKey());
			Vector<TapTableColumn> columnsMeta = tablesMetaData.get(metaDataEntry.getKey()).getColumns();
			updateQueryCheckTableColumns(parserTable, columnsMeta);
			this.queryCheckerTables.add(parserTable);
		}
		QueryChecker checker = new DBChecker(this.queryCheckerTables);
		this.adqlParser.setQueryChecker(checker);
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
		DefaultDBTable table = new DefaultDBTable(tableName);
		DefaultDBTable queryCheckerTable = null;
		
		if (this.queryCheckerTables != null) {
			for (DefaultDBTable defaultDBTable : this.queryCheckerTables) {//Check if table is existing
				if (TapManager.areSameQueryCheckerTables(defaultDBTable, table)) {
					queryCheckerTable = defaultDBTable;
					break;
				}
			}
			
			if (tablesMetaData.containsKey(tableName)) {//Get table metadata
				Vector<TapTableColumn> columns = tablesMetaData.get(tableName).getColumns();
				updateQueryCheckTableColumns(table, columns);
				
				if (mode == TapServerMode.UPLOAD
						|| (queryCheckerTable != null && this.queryCheckerTables.remove(queryCheckerTable))) {
					this.queryCheckerTables.add(table);
					QueryChecker checker = new DBChecker(this.queryCheckerTables);
					this.adqlParser.setQueryChecker(checker);
				}
				
			}
		}
	}
	
	/**
	 * Convenience method to set column to table for parser
	 * @param parserTable
	 * @param columnsMeta
	 */
	private void updateQueryCheckTableColumns(DefaultDBTable parserTable, Vector<TapTableColumn> columnsMeta) {
		if (parserTable != null && columnsMeta != null) {
			for(TapTableColumn tapTableColumn : columnsMeta) {
				DefaultDBColumn columnForParser = new DefaultDBColumn(tapTableColumn.getColumn_name(), parserTable);
				if (tapTableColumn.getDatatype() != null && !tapTableColumn.getDatatype().isEmpty()) {
					int offset = tapTableColumn.getDatatype().indexOf("adql:");
					if (offset != -1 && offset + 5 < tapTableColumn.getDatatype().length()) {
						String datatype = tapTableColumn.getDatatype().substring(offset + 5);
						if (DBDATATYPES.containsKey(datatype)) {
							DBDatatype dbDataType = DBDATATYPES.get(datatype);
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
	protected void clear() {
		if (this.sync_async!=null) {
			this.sync_async.setSelectedIndex(0);
		}
		if (this.limit!=null) {
			this.limit.setSelectedIndex(0);
		}
		if (this.selectAll!=null) {
			this.selectAll.setSelected(false);
		}
		
		boolean resetTargetPanel = false;
		if (this.raColumnName != null && this.decColumnName != null) {
			resetTargetPanel = true;
		}
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		if (resetTargetPanel && (this.raColumnName!=null && this.decColumnName!=null)) {
			resetTargetPanel = false;
		}
		if (resetTargetPanel) {
			Vector<TapTableColumn> columnNames = this.tablesMetaData.get(this.selectedTableName).getColumns();
			setWhereAddConstraintsGui(columnNames);
			this.queryComponentsGui.revalidate();
			this.queryComponentsGui.repaint();
		}
		
		resetColumnSelection();
		resetFields();
		super.clear();
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * Convenience method
	 */
	protected void resetFields() {
		if (this.whereClausesPanel != null) {
			this.whereClausesPanel.removeAll();
		}
		if (this.circleOrSquare != null) {
			this.circleOrSquare.setSelectedIndex(0);
		}
		if (this.tap != null) {
			this.tap.setText(EMPTYSTRING);
		}
		resetColumnSelection();
	};
	
	@Override
	protected void reset() {
		this.createFormDefault();
		if (this.selectAll != null) {
			this.selectAll.setSelected(true);
		}
		resetFields();
		super.reset();
		this.revalidate();
		this.repaint();
	};
	
	/**
	 * Method sets/resets column selection based on selectAll selection
	 * repaint is <b>not</b> called implicitly
	 */
	protected void resetColumnSelection() {
		if (this.selectAll!=null) {
			if (this.selectAll.isSelected()) {
				int start = 0;
				int end = selectList.getModel().getSize() - 1;
				if (end >= 0) {
					selectList.setSelectionInterval(start, end);
				}
			} else {
				selectList.clearSelection();
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
		if (formLoadStatus != TAPFORM_STATUS_LOADED) {
			return;
		}
		boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
  	  	this.submitTapServerRequest(sync, requestParams, this.name, url);
	}
	
	@Override
	public void submit() {
		submit(null);//no request params
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
					this.tapManager.showTapRegistryForm();
				} catch (Exception e) {
					Aladin.warning(this, GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} else if (action.equals(WRITEQUERY)) {
				this.writeQuery();
			} else if (action.equals(ADDWHERECONSTRAINT)) {
				Vector<TapTableColumn> columnMetaData = this.tablesMetaData.get(this.selectedTableName).getColumns();
				WhereGridConstraint columnConstraint = new ColumnConstraint(this, columnMetaData);
				addWhereConstraint(columnConstraint);
				writeQuery();
			} else if (action.equals(REMOVEWHERECONSTRAINT)) {
				JButton button = ((JButton) source);
		    	JPanel thisComponent = (JPanel) button.getParent();
		    	JPanel compUnderWorks = (JPanel) thisComponent.getParent();
		    	compUnderWorks.remove(thisComponent);
		    	ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
		    	writeQuery();
		    	this.whereClausesPanel.revalidate();
		    	this.whereClausesPanel.repaint();
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
					Aladin.warning(this, TARGETERROR);
		            ball.setMode(Ball.NOK);
		            return;
				}
				WhereGridConstraint positionConstraint = new PositionConstraint(this, coo[0].getText(), coo[1].getText(), rad[0].getText(), this.raColumnName, this.decColumnName);
				addWhereConstraint(positionConstraint);
				writeQuery();
			} else if (action.equals(CHECKQUERY)) {
				checkQueryFlagMessage();
			} else if (action.equals(UPLOAD)) {
				//disabled based on capability and if user has not created a table
				if (tapManager.uploadFrame == null) {
					tapManager.uploadFrame = new FrameUploadServer(aladin, this.url);
				}
				tapManager.uploadFrame.show(this);
				
			} else if (action.equals(OPEN_SET_RADEC)) {
	        	this.tapManager.setRaDecForTapServer(this, selectedTableName);
			} else if (action.equals(SHOWAYNCJOBS)) {
				try {
					this.tapManager.showAsyncPanel();
				} catch (Exception e) {
					Aladin.warning(this, GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} /*else if (action.equals(RETRYACTION)) {//tintin what is this rety and reload for 2
				try {
					tapManager.reloadSimpleFramePanelServer(this.name, this.url);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if (Aladin.levelTrace >=3) e.printStackTrace();
					Aladin.warning(this, e.getMessage());
				}
			}*/ else if (action.equals(RELOAD)) {
				try {
					this.tapClient.reload();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if (Aladin.levelTrace >=3) e.printStackTrace();
					Aladin.warning(this, e.getMessage());
				}
			}
		} else if(source instanceof JCheckBox){// check command- SELECTALL
			Aladin.trace(3, "actionperformed for SELECTALL was triggered");
			JCheckBox selectAll = (JCheckBox)source;
			String action = selectAll.getActionCommand();
			if (action.equals(SELECTALL)) {
				resetColumnSelection();
			}
			writeQuery();
			selectList.revalidate();
			selectList.repaint();
		}
	}
	
	public static JButton getChangeServerButton() {
		JButton button = null;
		Image image = Aladin.aladin.getImagette("changeServerOptions.png");
		if (image == null) {
			button = new JButton("Change server");
		} else {
			button = new JButton(new ImageIcon(image));
		}
		button.setBorderPainted(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setContentAreaFilled(true);
		button.setActionCommand(CHANGESERVER);
		button.setToolTipText(CHANGESERVERTOOLTIP);
		return button;
	}
	
	public boolean isNotLoaded() {
		return (formLoadStatus == TAPFORM_STATUS_NOTLOADED);
	}
	protected void createChaine() {
		super.createChaine();
		aladinLabel = Aladin.chaine.getString("TAPFORMNAME");
		description = Aladin.chaine.getString("TAPFORMINFO");
		title = Aladin.chaine.getString("TAPFORMTITLE");
		verboseDescr = Aladin.chaine.getString("TAPFORMDESC");
		CLIENTINSTR  = Aladin.chaine.getString("TAPCLIENTINSTR");
	}
	
	static {
		LOAD = Aladin.chaine.getString("FSLOAD");
		TAPTABLEJOINTIP = Aladin.chaine.getString("TAPTABLEJOINTIP");
		TAPTABLEUPLOADTIP = Aladin.chaine.getString("TAPTABLEUPLOADTIP");
		TARGETERROR = Aladin.chaine.getString("TARGETERROR");
		TIPCLICKTOADD = Aladin.chaine.getString("TIPCLICKTOADD");
		TAPTABLENOUPLOADTIP = Aladin.chaine.getString("TAPTABLENOUPLOADTIP");
		GENERICERROR = Aladin.chaine.getString("GENERICERROR");
		REFRESHQUERYTOOLTIP = Aladin.chaine.getString("REFRESHQUERYTOOLTIP");
		CHECKQUERYTOOLTIP = Aladin.chaine.getString("CHECKQUERYTOOLTIP");
		SYNCASYNCTOOLTIP = Aladin.chaine.getString("SYNCASYNCTOOLTIP");
		SHOWASYNCTOOLTIP = Aladin.chaine.getString("SHOWASYNCTOOLTIP");
		DISCARD = Aladin.chaine.getString("DISCARD");
		DISCARDTIP = Aladin.chaine.getString("DISCARDTIP");
		RETRY = Aladin.chaine.getString("TAPRETRY");
		TIPRETRY = Aladin.chaine.getString("TAPTIPRETRY");
	    CHANGESERVERTOOLTIP = Aladin.chaine.getString("CHANGESERVERTOOLTIP");
	}
	
	public void setData(Map<String, TapTable> tablesMetaData) {
		// TODO Auto-generated method stub
		this.tablesMetaData = tablesMetaData;
	}

	public String getRaColumnName() {
		return raColumnName;
	}

	public void setRaColumnName(String raColumnName) {
		this.raColumnName = raColumnName;
	}

	public String getDecColumnName() {
		return decColumnName;
	}

	public void setDecColumnName(String decColumnName) {
		this.decColumnName = decColumnName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setCapalilities() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		this.requestFocusInWindow();
		this.writeQuery();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
	}
	
	/*Keeping this method just for ref
	 * public void setQueryChecker(String tableName) {
		DefaultDBTable table = new DefaultDBTable(tableName);
		DefaultDBTable queryCheckerTable = null;
		boolean updateQueryChecker = true;
		
		if (this.queryCheckerTables == null) {//initialise for the first time
			this.queryCheckerTables = new ArrayList<DefaultDBTable>();
		} else {
			for (DefaultDBTable defaultDBTable : this.queryCheckerTables) {//Check if table is existing
				if (defaultDBTable.getADQLName() != null && defaultDBTable.getADQLName().equalsIgnoreCase(table.getADQLName())) {
					queryCheckerTable = defaultDBTable;
					break;
				}
			}
		}
		
		if (tablesMetaData.containsKey(tableName)) {//Get table metadata
			Vector<TapTableColumn> columns = tablesMetaData.get(tableName).getColumns();
			if (columns != null) {
				for (TapTableColumn tapTableColumn : columns) {
					DefaultDBColumn columnForParser = null;
					if (queryCheckerTable != null) {
						columnForParser = (DefaultDBColumn) queryCheckerTable.getColumn(tapTableColumn.getColumn_name(), true);//check if queryparser has column info
						if (columnForParser != null) {// if there are columns already. if yes- then no need to update
							updateQueryChecker = false;// or just that columnForParser != null..for all columns
							break;//all other column would be populated
						}
					} 
					columnForParser = new DefaultDBColumn(tapTableColumn.getColumn_name(), table); //if column info is not there then you do the below, populate checker from metainfo of column..
					if (tapTableColumn.getDatatype() != null && !tapTableColumn.getDatatype().isEmpty()) {
						int offset = tapTableColumn.getDatatype().indexOf("adql:");
						if (offset != -1 && offset + 5 < tapTableColumn.getDatatype().length()) {
							String datatype = tapTableColumn.getDatatype().substring(offset+5);
							if (DBDATATYPES.containsKey(datatype)) {
								DBDatatype dbDataType = DBDATATYPES.get(datatype);
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
					table.addColumn(columnForParser);
				}
			}
			
			if (updateQueryChecker) {
				if (queryCheckerTable != null) {
					this.queryCheckerTables.remove(queryCheckerTable);
				}
				this.queryCheckerTables.add(table);
				QueryChecker checker = new DBChecker(this.queryCheckerTables);
				this.adqlParser.setQueryChecker(checker);
			}
		}
		
	}*/

}
