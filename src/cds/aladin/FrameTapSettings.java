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

import static cds.aladin.Constants.CHANGESETTINGS;
import static cds.aladin.Constants.DEC;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.RA;
import static cds.aladin.Constants.TAP_EMPTYINPUT;
import static cds.aladin.Constants.TAP_REC_LIMIT;
import static cds.aladin.Constants.TAP_REC_LIMIT_UNLIMITED;
import static cds.aladin.Constants.SPACESTRING;
import static cds.tools.CDSConstants.BOLD;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import cds.aladin.Constants.TapClientMode;
import cds.tools.Util;


/**
 * This class is generic to show servers
 *
 */
public class FrameTapSettings extends JFrame implements ActionListener, GrabItFrame , MouseListener, FilterActionClass {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3541428440636805284L;
	
	Aladin aladin;
	ServerTapExamples serverEx;
	
	public static String CLOSE;
	String priTableName;
	
	TargetPanel targetPanel1;
//	TargetPanel targetPanel2;
	JComboBox raColumn1;
	JComboBox raColumn2;
	JComboBox decColumn1;
	JComboBox decColumn2;
	
	JPanel buttonsPanel;

	public JComboBox<String> limit;

	public JComboBox secondaryTablesGui;

	public String secondaryTable;

	public JComboBox uploadTablesGui;

	protected int tableSelection = 0;

	private JCheckBox enableSecondTable;
	
	protected FrameTapSettings() {
		super();
	}
	
	/**
	 * All you have is only one tapserver frame. 
	 * So we will need the argument during frame construction
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected FrameTapSettings(Aladin aladin) {
		super();
		this.aladin = aladin;
		Aladin.setIcon(this);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		setLocation(Aladin.computeLocation(this));
		setFont(Aladin.PLAIN);
		getRootPane().registerKeyboardAction(new ActionListener() {
	         public void actionPerformed(ActionEvent e) { processAnyClosingActions(); }
	      },
	      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
	      JComponent.WHEN_IN_FOCUSED_WINDOW
	            );
	      getRootPane().registerKeyboardAction(new ActionListener() {
	         public void actionPerformed(ActionEvent e) { processAnyClosingActions(); }
	      },
	      KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
	      JComponent.WHEN_IN_FOCUSED_WINDOW
	            );
	      createChaine();
	}
	
	/** Affichage des infos associées à un serveur */
	protected void show(ServerTapExamples s, String title) {
		try {
			setTitle(title);
			if (this.serverEx == null) {
				createFrame(s);
			} else {
				if (s != this.serverEx || priTableName == null || !serverEx.selectedTableName.equalsIgnoreCase(priTableName)) {
					setRaDecColumn(s, s.selectedTableName, raColumn1, decColumn1);
				}
				
				if (s != this.serverEx || (s.secondaryTable != null && !s.secondaryTable.equalsIgnoreCase(secondaryTable))) {
					Vector<String> tables = new Vector<String>(s.tapClient.tablesMetaData.keySet());
					if (s.secondaryTable  == null || !tables.contains(s.secondaryTable)) {
						if (tables.size() > 1) {
							secondaryTable = tables.get(1);
						} else {
							secondaryTable = null;
						}
					} else {
						secondaryTable = s.secondaryTable;
					}
					DefaultComboBoxModel items = new DefaultComboBoxModel(tables);
					JTextComponent tablesGuiEditor = (JTextComponent) secondaryTablesGui.getEditor().getEditorComponent();
					secondaryTablesGui.setEnabled(false);
					secondaryTablesGui.removeAllItems();
					secondaryTablesGui.setModel(items);
					List<String> keys = new ArrayList<String>();
					keys.addAll(s.tapClient.tablesMetaData.keySet());
					tablesGuiEditor.setDocument(new FilterDocument(this, this.secondaryTablesGui, keys, secondaryTable));
					
					boolean enabled = false;
					if (secondaryTable != null || uploadTablesGui.isEnabled()) {
						enabled = true;
					}
					enableSecondTable.setSelected(enabled);
					pack();
					setRaDecColumn(s, secondaryTable, raColumn2, decColumn2);
				}
				if (s != this.serverEx) {
					if (s.max != null) {
						if (s.max.isEmpty()) {
							this.limit.setSelectedItem(TAP_REC_LIMIT_UNLIMITED);
						} else if (s.max.split(SPACESTRING).length > 1) {
							String maxSelection = s.max.split(SPACESTRING)[1];
							this.limit.setSelectedItem(maxSelection);
						}
					}
				}
			}
			this.serverEx = s;
			this.serverEx.updateWidgets(this);
			priTableName = serverEx.selectedTableName;
			pack();
			setVisible(true);
		} catch (Exception e) {
			// TODO: handle exception
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			Aladin.warning(this, "Cannot open settings "+e.getMessage());
			secondaryTable = null;
			if (isVisible()) {
				setVisible(false);
			}
		}
	}
	
	private void setRaDecColumn(DynamicTapForm s, String tableName, JComboBox raSettingsGui, JComboBox decSettingsGui) throws Exception {
		TapTable table = s.tapClient.tablesMetaData.get(tableName);
		Vector<TapTableColumn> columns = new Vector<TapTableColumn>();
		TapTableColumn dummyColumn = getDummyColumn();
		columns.add(0, dummyColumn);
		if (tableName != null) {
			columns.addAll(getColumnSchemas(s, tableName));
		}
		DefaultComboBoxModel items = new DefaultComboBoxModel(columns);
		raSettingsGui.removeAllItems();
		raSettingsGui.setModel(items);
		TapTableColumn toSelect = dummyColumn;
		if (table != null) {
			toSelect = table.getFlaggedColumn(RA);
		}
		raSettingsGui.setSelectedItem(toSelect);
		toSelect = dummyColumn;
		items = new DefaultComboBoxModel(columns);
		decSettingsGui.removeAllItems();
		decSettingsGui.setModel(items);
		if (table != null) {
			toSelect = table.getFlaggedColumn(DEC);
		}
		decSettingsGui.setSelectedItem(toSelect);
	}
	
	public void refreshTapSettings_Add(String uploadTableName, Map<String, TapTable> tablesMetaData) {
		// TODO Auto-generated method stub
		if (this.serverEx != null && this.serverEx.tapClient.isUploadAllowed()) {
			this.uploadTablesGui.addItem(uploadTableName);
//			this.serverEx.updateQueryChecker(true, uploadTableName, tablesMetaData, serverEx.queryCheckerTables);
			this.uploadTablesGui.setEnabled(true);
			this.uploadTablesGui.revalidate();
			this.uploadTablesGui.repaint();
			if (tableSelection == 2) {
				setSecondTableGui(true);
				pack();
				checkSelectionChanged(uploadTablesGui);
			}
		}
	}
	
	public void refreshTapSettings_Delete(String deletedTableName, Map<String, TapTable> tablesMetaData) {
		// TODO Auto-generated method stub
		if (this.serverEx != null && this.serverEx.tapClient.isUploadAllowed()) {
			boolean enable = false;
			this.uploadTablesGui.setEnabled(false);
			if (tablesMetaData != null && !tablesMetaData.isEmpty()) {
				enable = true;
				this.uploadTablesGui.removeItem(deletedTableName);
//				this.serverEx.updateQueryChecker_deleteTable(deletedTableName, this.serverEx.queryCheckerTables);
			} else {
				this.uploadTablesGui.removeAllItems();
			}
			this.uploadTablesGui.setEnabled(enable);
			if (tableSelection == 2) {
				setSecondTableGui(enable);
				pack();
				checkSelectionChanged(uploadTablesGui);
			}
			
		}
	}

	/**
	 * Creates the first form for loading upload data
	 * @param s 
	 * @param server2 
	 * @throws Exception 
	 */
	public void createFrame(ServerTapExamples s) throws Exception {
//		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		this.getContentPane().removeAll();
		this.getContentPane().setBackground(Aladin.COLOR_MAINPANEL_BACKGROUND);
		this.getContentPane().add(createContents(s), "Center");
		buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton settingsButton = new JButton("Apply changes");
		settingsButton.addActionListener(this);
		settingsButton.setActionCommand(CHANGESETTINGS);
		buttonsPanel.add(settingsButton);

		buttonsPanel.add(settingsButton = new JButton(CLOSE));
		settingsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				setVisible(false);
			}
		});
		
		this.getContentPane().add(buttonsPanel, "South");
		this.getContentPane().revalidate();
		this.getContentPane().repaint();
		this.getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray));
		this.getRootPane().getInsets().set(2, 2, 0, 2);
		this.getRootPane().setSize(500, 500);
	}
	
	public JPanel createContents(ServerTapExamples s) throws Exception {
		JPanel contents = new JPanel();
		contents.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(15, 10, 3, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		
		c.weightx = 0.10;
		JLabel label = new JLabel("Max rows:");
		label.setFont(BOLD);
		contents.add(label, c);
		
		c.gridx = 1;
		c.weightx = 0.90;
		this.limit = new JComboBox<String>(TAP_REC_LIMIT);
		if (s.max != null) {
			if (s.max.isEmpty()) {
				this.limit.setSelectedItem(TAP_REC_LIMIT_UNLIMITED);
			} else if (s.max.split(SPACESTRING).length > 1) {
				String maxSelection = s.max.split(SPACESTRING)[1];
				this.limit.setSelectedItem(maxSelection);
			}
		}
		this.limit.setOpaque(false);
		contents.add(this.limit, c);
		
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 2;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy++;
		c.insets = new Insets(10, 10, 3, 10);
		label = new JLabel("Settings for table 1:");
		label.setFont(Aladin.BOLD);
		contents.add(label, c);
		
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.10;
		
		Vector<TapTableColumn> columnsToSet = new Vector<TapTableColumn>();
		TapTableColumn dummyColumn = getDummyColumn();
		columnsToSet.add(0, dummyColumn);
		columnsToSet.addAll(getColumnSchemas(s, s.selectedTableName));
		raColumn1 = new JComboBox(columnsToSet);
		raColumn1.setRenderer(new CustomListCellRenderer());
		raColumn1.setSize(raColumn1.getWidth(), Server.HAUT);
		TapTable table = s.tapClient.tablesMetaData.get(s.selectedTableName);
		TapTableColumn selectedItem = table.getFlaggedColumn(RA);
		if (selectedItem != null) {
			raColumn1.setSelectedItem(selectedItem);
		} else {
			raColumn1.setSelectedItem(dummyColumn);
		}
		selectedItem = null;
		contents.add(new JLabel("Ra:"), c);
		c.gridx = 1;
		c.weightx = 0.90;
		contents.add(raColumn1, c);
		
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.10;
		decColumn1 = new JComboBox(columnsToSet);
		decColumn1.setRenderer(new CustomListCellRenderer());
		decColumn1.setSize(decColumn1.getWidth(), Server.HAUT);
		selectedItem = table.getFlaggedColumn(DEC);
		if (selectedItem != null) {
			decColumn1.setSelectedItem(selectedItem);
		} else {
			decColumn1.setSelectedItem(dummyColumn);
		}
		selectedItem = null;
		contents.add(new JLabel("Dec:"), c);
		c.gridx = 1;
		c.weightx = 0.90;
		contents.add(decColumn1, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		targetPanel1 = new TargetPanel();
//		createTargetPanel(targetPanel1);
		s.createTargetPanel(targetPanel1);
		targetPanel1.target = s.target;
		targetPanel1.radius = s.radius;
		targetPanel1.grab = s.grab;
		targetPanel1.circleOrSquare = s.circleOrSquare;
		if (this.serverEx == null) {
			aladin.grabUtilInstance.grabs.add(s.grab);
		} else {
			aladin.grabUtilInstance.removeAndAdd(this.serverEx.grab, s.grab);
		}
		s.grab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				serverEx.grab = (JToggleButton) e.getSource();
				serverEx.radius = targetPanel1.radius; 
				serverEx.target = targetPanel1.target;
           }
        });
		String targetdefault = aladin.localisation.getLastPosition();
		if (targetdefault != null) {
			targetPanel1.target.setText(targetdefault);
			targetPanel1.radius.setText("10'");
		}
		contents.add(targetPanel1, c);
		
		
		table = null;
		ButtonGroup secondTableCheckGroup = new ButtonGroup();
		Vector<String> tables = new Vector<String>(s.tapClient.tablesMetaData.keySet().size());
		tables.addAll(s.tapClient.tablesMetaData.keySet());
		if (s.secondaryTable == null || !tables.contains(s.secondaryTable)) {
			if (tables.size() > 2) {
				secondaryTable = tables.get(1);
			} else {
				secondaryTable = null;
			}
		} else {
			secondaryTable = s.secondaryTable;
		}
		columnsToSet = new Vector<TapTableColumn>();
		columnsToSet.add(0, dummyColumn);
		if (secondaryTable != null) {
			try {
				columnsToSet.addAll(getColumnSchemas(s, secondaryTable));
			} catch (Exception e) {
				// for second table errors, we do not provide the gui
				if (Aladin.levelTrace >= 3) {
					e.printStackTrace();
				}
			}
		}
		
		c.insets = new Insets(15, 10, 3, 10);
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1;
		enableSecondTable = new JCheckBox("Enable a second table");
		enableSecondTable.setFont(Aladin.BOLD);
		enableSecondTable.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				boolean enable = false;
				if (e.getStateChange() == ItemEvent.SELECTED) {
					enable = true;
				}
				setSecondAllTableGui(enable);
			}
		});
		contents.add(enableSecondTable, c );
		
		JPanel secondaryTablePanel = new JPanel();
		secondaryTablePanel.setLayout(new BoxLayout(secondaryTablePanel, BoxLayout.X_AXIS));
		JRadioButton addSecondTable = new JRadioButton();
		secondTableCheckGroup.add(addSecondTable);
		addSecondTable.setSelected(true);
		tableSelection = 1;
		secondaryTablesGui = new JComboBox(tables);
		secondaryTablesGui.setEditable(true);
		
		addSecondTable.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (e.getStateChange() == ItemEvent.SELECTED) {
					tableSelection = 1;
					pack();
					checkSelectionChanged(secondaryTablesGui);
				}
				
			}
		});
		secondaryTablePanel.add(addSecondTable);
		
		label = new JLabel();
		label.setText("Table:");
		secondaryTablePanel.add(label);
		
		
		JTextComponent tablesGuiEditor = (JTextComponent) secondaryTablesGui.getEditor().getEditorComponent();
		List<String> keys = new ArrayList<String>();
		keys.addAll(tables);
		try {
			tablesGuiEditor.setDocument(new FilterDocument(this, this.secondaryTablesGui, keys, secondaryTable));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			Aladin.warning(this, e.getMessage());
			setVisible(false);//tintin TODO error handling
			throw e;
		}
		if (secondaryTable == null) {
			secondaryTablesGui.setEnabled(false);
		}
		secondaryTablesGui.setOpaque(false);
		
		secondaryTablePanel.setFont(BOLD);
		secondaryTablePanel.add(secondaryTablesGui);
		
		c.weightx = 0.12;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.insets = new Insets(3, 20, 3, 10);
		contents.add(secondaryTablePanel, c);
		
		c.gridy++;
		c.insets = new Insets(15, 20, 3, 10);
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1;
		label = new JLabel("Or");
		label.setFont(Aladin.BOLD);
		contents.add(label, c );
		
		c.gridy++;
		JPanel uploadTablePanel = new JPanel();
		uploadTablePanel.setLayout(new BoxLayout(uploadTablePanel, BoxLayout.X_AXIS));
		JRadioButton useUploadTable = new JRadioButton();
		secondTableCheckGroup.add(useUploadTable);
		useUploadTable.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (e.getStateChange() == ItemEvent.SELECTED) {
					tableSelection = 2;
					pack();
					checkSelectionChanged(uploadTablesGui);
				}
			}
		});
		uploadTablePanel.add(useUploadTable);	
		
		label = new JLabel();
		label.setText("Uploaded tables:");
		uploadTablePanel.add(label);
		
		Map<String, TapTable> uploadedFiles = getUploadedTables();
		if (uploadedFiles == null || uploadedFiles.isEmpty()) {
			uploadTablesGui = new JComboBox();
			uploadTablesGui.setEnabled(false);
		} else {
			uploadTablesGui = new JComboBox(uploadedFiles.keySet().toArray());
			if (s.tapClient.isUploadAllowed()) {
				this.uploadTablesGui.setEnabled(true);
			}
			/*tablesGuiEditor = (JTextComponent) uploadTablesGui.getEditor().getEditorComponent();
			keys = new ArrayList<String>();
			keys.addAll(uploadedFiles.keySet());
			try {
				tablesGuiEditor.setDocument(new FilterDocument(this, this.uploadTablesGui, keys, null));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				setVisible(false);//tintin TODO error handling
			}*/
			uploadTablesGui.setOpaque(false);
		}
		
		uploadTablePanel.setFont(BOLD);
		uploadTablePanel.add(uploadTablesGui);
		
		uploadTablePanel.add(s.getUploadButtonIfAvailable("Upload:"));
		
		contents.add(uploadTablePanel, c );
		
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1;
		label = new JLabel("Settings for table 2 (only used when table 2 is active):");
		label.setFont(Aladin.BOLD);
		contents.add(label, c );
		
		
		c.insets = new Insets(3, 20, 3, 10);
		raColumn2 = new JComboBox(columnsToSet);
		raColumn2.setRenderer(new CustomListCellRenderer());
		raColumn2.setSize(raColumn2.getWidth(), Server.HAUT);
		
		if (secondaryTable != null) {
			table = s.tapClient.tablesMetaData.get(secondaryTable);
			selectedItem = table.getFlaggedColumn(RA);
		}
		
		if (selectedItem != null) {
			raColumn2.setSelectedItem(selectedItem);
		} else {
			raColumn2.setSelectedItem(dummyColumn);
		}
		selectedItem = null;

		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.10;
		contents.add(new JLabel("Ra:"), c);
		c.gridx = 1;
		c.weightx = 0.90;
		contents.add(raColumn2, c);
		
		decColumn2 = new JComboBox(columnsToSet);
		decColumn2.setRenderer(new CustomListCellRenderer());
		decColumn2.setSize(decColumn2.getWidth(), Server.HAUT);
		if (secondaryTable != null) {
			selectedItem = table.getFlaggedColumn(DEC);
		}
		if (selectedItem != null) {
			decColumn2.setSelectedItem(selectedItem);
		} else {
			decColumn2.setSelectedItem(dummyColumn);
		}
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.10;
		contents.add(new JLabel("Dec:"), c);
		c.gridx = 1;
		c.weightx = 0.90;
		contents.add(decColumn2, c);
		
		/*targetPanel2 = new TargetPanel();
//			createTargetPanel(targetPanel2);
		s.createTargetPanel(targetPanel2);
		targetPanel2.target = s.target;
		targetPanel2.grab = s.grab;
		targetPanel2.radius = s.radius;
		targetPanel2.circleOrSquare = s.circleOrSquare;
		if (this.serverEx == null) {
			aladin.grabUtilInstance.grabs.add(s.grab);
		} else {
			aladin.grabUtilInstance.removeAndAdd(this.serverEx.grab, s.grab);
		}	
		s.grab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				serverEx.grab = (JToggleButton) e.getSource();
				serverEx.radius = targetPanel2.radius; 
				serverEx.target = targetPanel2.target;
		   }
		});
		c.gridx = 0;
		c.gridy++;
		c.weightx = 1;
		c.gridwidth = 2;
		
		targetdefault = aladin.localisation.getLastPosition();
		if (targetdefault != null) {
			targetPanel2.target.setText(targetdefault);
			targetPanel2.radius.setText("5'");
		}
		contents.add(targetPanel2, c);*/
		boolean enabled = false;
		if (secondaryTable != null) {
			enabled = true;
		}
		enableSecondTable.setSelected(enabled);
//		setSecondAllTableGui(enabled); 
		contents.setMinimumSize(new Dimension(400, 480));
		contents.setPreferredSize(new Dimension(400, 480));
		return contents;
	}
	
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (command.equals(CHANGESETTINGS)) {
			try {
				if (this.limit.getSelectedItem().equals(TAP_REC_LIMIT_UNLIMITED)) {
					serverEx.max = EMPTYSTRING;
				} else {
					serverEx.max = (String) this.limit.getSelectedItem();
				}

				TapTable tableMetaData = serverEx.tapClient.tablesMetaData.get(serverEx.selectedTableName);
				TapTableColumn raColumn = ((TapTableColumn) raColumn1.getSelectedItem());
				TapTableColumn decColumn = ((TapTableColumn) decColumn1.getSelectedItem());
				if (raColumn.getUtype().equals(TAP_EMPTYINPUT) || decColumn.getUtype().equals(TAP_EMPTYINPUT)) {
					tableMetaData.setRaColumn(null);
					tableMetaData.setDecColumn(null);
				} else {
					tableMetaData.setRaColumn(raColumn);
					tableMetaData.setDecColumn(decColumn);
				}
				
//				serverEx.changeTargetSettings(targetPanel1, 0);

				if (this.enableSecondTable.isSelected() && tableSelection > 0) {
//					serverEx.changeTargetSettings(targetPanel2, 1);
					if (tableSelection == 1) {
						String tableSelected = (String) this.secondaryTablesGui.getSelectedItem();
						tableMetaData = serverEx.tapClient.tablesMetaData.get(tableSelected);
						raColumn = ((TapTableColumn) raColumn2.getSelectedItem());
						decColumn = ((TapTableColumn) decColumn2.getSelectedItem());
						this.serverEx.getColumnsToLoad(tableSelected, this.serverEx.tapClient.tablesMetaData);
						//do not call getColumnsToLoad with the upload table, 
						//because this is not on upload frame for the tap client to know that this is an uploaded table
						if (tableSelected.equalsIgnoreCase(serverEx.selectedTableName)) {
							Aladin.warning(this.getContentPane(), "Cannot select same table! No second table selected.");
							tableSelected = null;
						}
						serverEx.secondaryTable = tableSelected;
					} else if (tableSelection == 2) {
						if (this.uploadTablesGui.isEnabled()) {
							String tableSelected = (String) this.uploadTablesGui.getSelectedItem();
							tableMetaData = TapManager.getInstance(aladin).uploadFrame.uploadClient.tablesMetaData.get(tableSelected);
							raColumn = ((TapTableColumn) raColumn2.getSelectedItem());
							decColumn = ((TapTableColumn) decColumn2.getSelectedItem());
							serverEx.secondaryTable = tableSelected;
						} else {
							Aladin.warning(this.getContentPane(), "Please upload a table!");
							return;
						}
					}
					if (raColumn == null || decColumn == null || TAP_EMPTYINPUT.equals(raColumn.getUtype())
							|| TAP_EMPTYINPUT.equals(decColumn.getUtype())) {
						tableMetaData.setRaColumn(null);
						tableMetaData.setDecColumn(null);
					} else {
						tableMetaData.setRaColumn(raColumn);
						tableMetaData.setDecColumn(decColumn);
					}
					
				} else {
					serverEx.secondaryTable = null;
				}
				serverEx.changeTableSelection(serverEx.selectedTableName);
//				serverEx.info1.setText("Queries regenerated as per new settings!");
//				TapManager.getInstance(aladin).eraseNotification(serverEx.info1, serverEx.CLIENTINSTR);

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				Aladin.warning(this.serverEx, "Unable to change settings! " + e.getMessage());
				setVisible(false);
				return;
			}
			setVisible(false);
		}
	}
	
	private Map<String, TapTable> getUploadedTables() {
		Map<String, TapTable> results = null; 
		TapManager tapManager = TapManager.getInstance(aladin);
		if (tapManager.uploadFrame != null && tapManager.uploadFrame.uploadClient != null) {
			results = tapManager.uploadFrame.uploadClient.tablesMetaData;
		}
		return results;
	}
	
	public void setSecondAllTableGui(boolean enable) {
		if (secondaryTable != null) {
			this.secondaryTablesGui.setEnabled(enable);
		}
		if (uploadTablesGui.getItemCount() > 0) {
			uploadTablesGui.setEnabled(enable);
		}
		setSecondTableGui(enable);
	}

	public void setSecondTableGui(boolean enable) {
		raColumn2.setEnabled(enable);
		decColumn2.setEnabled(enable);
//		targetPanel2.target.setEnabled(enable);
//		targetPanel2.radius.setEnabled(enable);
//		targetPanel2.grab.setEnabled(enable);
//		targetPanel2.circleOrSquare.setEnabled(enable);
	}
	
	private TapTableColumn getDummyColumn() {
		// TODO Auto-generated method stub
		TapTableColumn dummyColumn = new TapTableColumn();
		dummyColumn.setColumn_name("-");
		dummyColumn.setUtype(TAP_EMPTYINPUT);
		return dummyColumn;
	}

	public Vector<TapTableColumn> getColumnSchemas(DynamicTapForm s, String tableName) throws Exception {
		Map<String, TapTable> tablesMetaData = s.tapClient.tablesMetaData;
		Vector<TapTableColumn> columnNames = tablesMetaData.get(tableName).getColumns();
		if (columnNames == null || columnNames.isEmpty()) {
			if (s.tapClient.mode == TapClientMode.UPLOAD) {
				throw new Exception("Error in uploaded data");
			}
			List<String> tableNamesToUpdate = new ArrayList<String>();
			tableNamesToUpdate.add(tableName);
			TapManager.getInstance(aladin).updateTableColumnSchemas(s, tableNamesToUpdate);
			columnNames = tablesMetaData.get(tableName).getColumns();
			if (columnNames == null) {
				throw new Exception("Error in updating the metadata for :" + tableName);
			}
		}
		return columnNames;
	}
	
	@Override
	public void checkSelectionChanged(JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		String selectedTable = (String) comboBox.getSelectedItem();
		DynamicTapForm s = null;
		if (comboBox.isEnabled()/* && !selectedTable.equalsIgnoreCase(secondaryTable)*/) {
			if (tableSelection == 1 && comboBox == this.secondaryTablesGui) {
				s = this.serverEx;
			} else if (tableSelection == 2 && comboBox == this.uploadTablesGui) {
				if (this.uploadTablesGui.isEnabled()) {
					s = TapManager.getInstance(aladin).uploadFrame.uploadClient.serverTap;
				} else {
					uploadTablesGui.setEnabled(false);
					setSecondAllTableGui(false);
				}
			}
			
			if (s != null) {
				try {
					setRaDecColumn(s, selectedTable, raColumn2, decColumn2);
					raColumn2.revalidate();
					raColumn2.repaint();
					decColumn2.revalidate();
					decColumn2.repaint();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					secondaryTablesGui.setEnabled(false);
					uploadTablesGui.setEnabled(false);
					setSecondAllTableGui(false);
					if (Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.warning(this, "Error: " + e.getMessage());
				}
				
			}
		}
	}
	
	@Override
	public Vector<String> getMatches(String mask, JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		Vector<String> matches = new Vector<String>();
		if (mask != null && !mask.isEmpty()) {
			if (comboBox == this.secondaryTablesGui) {
				matches = this.serverEx.getMatches(mask, comboBox);
			} /*else if (comboBox == this.uploadTablesGui) {
				Set<String> uploadedFiles = getUploadedTables().keySet();
				for (String key : uploadedFiles) {
					if (!(Util.indexOfIgnoreCase(key, mask) >= 0)) {
						continue;
					}
					matches.add(key);
				}
			}*/
		}
		return matches;
	}

	@Override
	public void setGrabItCoord(double x, double y) {
		GrabUtil.setGrabItCoord(aladin, serverEx, x, y);
	}

	@Override
	public void stopGrabIt() {
	    GrabUtil.stopGrabIt(aladin, this, serverEx);
	}

	/**
	    * Retourne true si le bouton grabit du formulaire existe et qu'il est
	    * enfoncé
	    */
	@Override
	public boolean isGrabIt() {
	      return (serverEx.modeCoo != Server.NOMODE
	            && serverEx.grab != null && serverEx.grab.getModel().isSelected());
	   }

	@Override
	public void setGrabItRadius(double x1, double y1, double x2, double y2) {
		GrabUtil.setGrabItRadius(aladin, serverEx, x1, y1, x2, y2);
	}
	
	@Override
	protected void processWindowEvent(WindowEvent e) {
		// TODO Auto-generated method stub
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
			processAnyClosingActions();
		super.processWindowEvent(e);
	}

	protected void processAnyClosingActions() {
		setVisible(false);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		this.getContentPane().requestFocusInWindow();
	}
	
	public void createChaine() {
		CLOSE = Aladin.chaine.getString("CLOSE");
	}

}
