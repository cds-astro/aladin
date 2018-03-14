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

import static cds.aladin.Constants.ADDPOSCONSTRAINT;
import static cds.aladin.Constants.ADDWHERECONSTRAINT;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.COMMA_SPACECHAR;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.JOIN_TABLE;
import static cds.aladin.Constants.OPEN_SET_RADEC;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;
import static cds.aladin.Constants.SELECTALL;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TAP_REC_LIMIT;
import static cds.aladin.Constants.TAP_REC_LIMIT_UNLIMITED;
import static cds.aladin.Constants.TARGETNAN;
import static cds.aladin.Constants.WRITEQUERY;
import static cds.tools.CDSConstants.BOLD;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import adql.db.DBChecker;
import adql.parser.QueryChecker;
import cds.aladin.Constants.TapClientMode;
import cds.tools.Util;

public class ServerTap extends DynamicTapForm implements MouseListener {

	private static final long serialVersionUID = 1L;
	public static String LOAD, REFRESHQUERYTOOLTIP;
	
    //these data are serverTap specific
    private String raColumnName;
	private String decColumnName;
	
	//gui
	static final int DEFAULT_INFO_TABLE_HEIGHT = 115;
	JList selectList;
	JCheckBox selectAll;
	JPanel whereClausesPanel;
	List<ColumnConstraint> whereClauses;
	JComboBox<String> limit;
	JPanel queryComponentsGui;
	JPanel targetPanel;
    JFrame setRaDecFrame;
    JoinFacade joinPanel;
    
	protected ServerTap(Aladin aladin) {
		super(aladin);
	}
	
	@Override
	public void createFormDefault() {
		this.createForm(null);
	}
	
	/**
	 * Creation of the tap query panel
	 * @param tablesMetaData
	 * @param tableChoice
	 */
	protected void createForm(String tableChoice) {
		CLIENTINSTR  = Aladin.chaine.getString("TAPCLIENTINSTR");
		Map<String, TapTable> tablesMetaData = this.tapClient.tablesMetaData;
		Vector<String> tables = getTableNames();
		TapTable chosenTable = null;
		/*
		if (tableChoice != null) {
			for (TapTable tapTable : tables) {
				DefaultDBTable ntable = new DefaultDBTable(tableChoice);
				if (tablesMetaData.containsKey(TapManager.getFullyQualifiedTableName(ntable))) {
					chosenTable = tapTable;
					break;
				} else if (tablesMetaData.containsKey(ntable.getADQLName())) {
					chosenTable = tapTable;
					break;
				}
			}
		}
		if (chosenTable == null) {
			chosenTable = tables.get(0);
		} 
		selectedTableName = chosenTable.getTable_name();
		*/
		
		if (tableChoice == null || !tables.contains(tableChoice)) {
			chosenTable = tablesMetaData.get(tables.firstElement());
		} else {
			chosenTable = tablesMetaData.get(tableChoice);
		}
		selectedTableName = chosenTable.getTable_name();
		Vector<TapTableColumn> columnNames = getColumnsToLoad(selectedTableName, tablesMetaData);

		QueryChecker checker = new DBChecker(this.tapClient.queryCheckerTables);
		this.adqlParser.setQueryChecker(checker);
		setBasics();
		
		this.raColumnName = chosenTable.getRaColumnName(false);
		this.decColumnName = chosenTable.getDecColumnName(false);
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		
		setTopPanel(containerPanel, c, info1, CLIENTINSTR);
	    
		JPanel tablesPanel = null;
		try {
			if (this.tapClient.mode == TapClientMode.UPLOAD) {
				tablesGui = new JComboBox();
				tablesGui.setModel(this.tapClient.tapManager.getUploadClientModel());
				tablesPanel = getTablesPanel(this.tapClient, this, null, tablesGui, chosenTable, null, null, false);
			} else {
				tablesGui = new JComboBox(tables);
				tablesPanel = getTablesPanel(this.tapClient, this, null, tablesGui, chosenTable, tables, null, false);
			}
			
//			tablesGui.setRenderer(new CustomTableRenderer());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			Aladin.error(this, e.getMessage());
			return;
		}
		tablesPanel.setBackground(this.tapClient.primaryColor);
		tablesPanel.setFont(BOLD);
		c.weighty = 0.02;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0.10;
	    c.fill = GridBagConstraints.NONE;
	    c.insets = new Insets(0, 4, 0, 0);
	    containerPanel.add(tablesPanel, c);		
		
	    this.queryComponentsGui = new JPanel();
		setWhereAddConstraintsGui(columnNames);
		this.queryComponentsGui.setBackground(this.tapClient.primaryColor);
		this.queryComponentsGui.setFont(BOLD);
		c.weighty = 0.51;
		c.weightx = 0.40;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(this.queryComponentsGui, c);	
		
		JPanel linePanel = getBottomPanel(true);
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
	
	public void joinConstraintUpdated() {
		waitCursor();
		tapClient.activateWaitMode(this);
		
		Map<String, TapTable> tablesMetaData = this.tapClient.tablesMetaData;
		TapTable mainTable = tablesMetaData.get(this.selectedTableName);
		
		if (!this.joinPanel.constraints.isEmpty()) {//is first join
			String alias = TapTable.getQueryPart(this.selectedTableName, true);
			mainTable.setAlias(alias);
		} else {
			mainTable.setAlias(null);
		}
		
		Vector<TapTableColumn> displayColumns = null;
		try {
			displayColumns = getSelectedTablesColumns();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			defaultCursor();
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			Aladin.error(this, e.getMessage());
			return;
		}
		
		// check if each where constraints has selected something that aint in the model
		// ya? then delete that thing off.
		// if not just set new model to this new thing : with the previously item preselected item
		if (this.whereClausesPanel.getComponentCount() > 0) {
			ColumnConstraint columnConstraint = null;
			boolean changed = false;
			Component[] whereConstraints = this.whereClausesPanel.getComponents();
			for (Component component : whereConstraints) {
				if (component instanceof ColumnConstraint) {
					columnConstraint = (ColumnConstraint) component;
					TapTableColumn selectedItem = columnConstraint.getSelectedItem();
					if (!displayColumns.contains(selectedItem)) {
						this.whereClausesPanel.remove(columnConstraint);
						changed = true;
					} else {
						columnConstraint.setWhereModel(displayColumns, selectedItem);
					}
				}
			}
			if (changed) {
				ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
			}
		}
		
		
		int[] selectedSelectListColumns = null;
		if (!this.selectAll.isSelected()) {
			selectedSelectListColumns = this.selectList.getSelectedIndices();
		} 
		

//		this.selectList.removeAll();
		DefaultListModel model = new DefaultListModel();
//		Vector<TapTableColumn> model = new Vector<TapTableColumn>();
//		model.addAll(displayColumns);
		for (TapTableColumn displayColumn : displayColumns) {
			model.addElement(displayColumn);
		}
		this.selectList.setModel(model);
		if (selectedSelectListColumns == null) {
			this.selectAll.setSelected(true);
			resetColumnSelection();
		} else {
			this.selectList.setSelectedIndices(selectedSelectListColumns);
		}
		
//		if (this.selectAll != null) {
//			this.selectAll.setSelected(true);
//		}
		
		
//		resetFields();
		ball.setMode(Ball.UNKNOWN);
//	    aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
	    formLoadStatus = TAPFORM_STATUS_LOADED;
		
		writeQuery();
		this.revalidate();
		this.repaint();
		defaultCursor();
		this.info1.setText(CLIENTINSTR);
	}
	
	@Override
	public void changeTableSelection(String tableChoice) {
		waitCursor();
		if (this.modeChoice != null) {
			this.modeChoice.setVisible(true);
		}
		tapClient.activateWaitMode(this);
		Map<String, TapTable> tablesMetaData = this.tapClient.tablesMetaData;
		Vector<TapTableColumn> columnNames = this.setTableGetColumnsToLoad(tableChoice, tablesMetaData);
		if (columnNames == null) {
			return;
		}
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName(false);
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName(false);
		if (Aladin.levelTrace >= 3) System.out.println("ra and dec: "+(this.raColumnName!=null && this.decColumnName!=null));
		if (Aladin.levelTrace >= 3) System.out.println("and target panel: "+target);
		if (this.raColumnName != null && this.decColumnName != null) {
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
		
		this.selectList.removeAll();
		Vector<TapTableColumn> model = new Vector<TapTableColumn>();
		model.addAll(columnNames);
		this.selectList.setListData(model);
		
		if (this.selectAll != null) {
			this.selectAll.setSelected(true);
		}
		
		resetFields();
		changeJoin();
		ball.setMode(Ball.UNKNOWN);
//	    aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    writeQuery();
		this.revalidate();
		this.repaint();
		defaultCursor();
		this.info1.setText(CLIENTINSTR);
	}
	
	private void changeJoin() {
		// TODO Auto-generated method stub
		if (this.joinPanel != null) {
			this.tapClient.tapManager.loadForeignKeyRelationsForSelectedTable(this.tapClient, this.joinPanel, selectedTableName);
		}
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
		this.queryComponentsGui.setBackground(this.tapClient.primaryColor);
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.tapClient.secondColor);
		JLabel label = new JLabel("Select:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.selectAll = new JCheckBox("All", true);
		this.selectAll.setActionCommand(SELECTALL);
		this.selectAll.addActionListener(this);
		panel.add(selectAll);
		
		if (columnNames != null) {
			Vector<TapTableColumn> modelNames = new Vector<TapTableColumn>();
			modelNames.addAll(columnNames);
			this.selectList = new JList(modelNames);
			this.selectList.setSelectionInterval(0, modelNames.size()-1);
			this.selectList.setCellRenderer(new CustomListCellRenderer(this));
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
		panel.setBackground(this.tapClient.secondColor);
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
		
		this.limit = new JComboBox<String>(TAP_REC_LIMIT);
		if (TAPEXDEFAULTMAXROWS != null) {
			if (!TAPEXDEFAULTMAXROWS.isEmpty()) {
				this.limit.setSelectedItem(TAPEXDEFAULTMAXROWS);
			} else {
				this.limit.setSelectedItem(TAP_REC_LIMIT_UNLIMITED);
			}
		}
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
		targetPanel.setBackground(this.tapClient.primaryColor);
		Aladin.trace(3, "ra and dec at createForm "+(this.raColumnName!=null && this.decColumnName!=null));
		
		createTargetPanel(targetPanel);
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
		this.whereClausesPanel.setBackground(this.tapClient.secondColor);
		this.whereClausesPanel.setName("WHERECLAUSES");
		
		JScrollPane constraintsScroller = new JScrollPane();
		constraintsScroller.setViewportView(this.whereClausesPanel);
		constraintsScroller.getVerticalScrollBar().setUnitIncrement(4);
		
		this.queryComponentsGui.add(constraintsScroller, c);
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
		whereConstraintPanel.setBackground(this.tapClient.secondColor);
		whereConstraintPanel.addWhereConstraints();
		whereConstraintPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, whereConstraintPanel.getMinimumSize().height));
		this.whereClausesPanel.add(whereConstraintPanel, Component.TOP_ALIGNMENT);
		ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
		
//		this.queryComponentsGui.setBounds(XTAB1, addConstraintY, XWIDTH, 250);
		revalidate();
		repaint();
	}
	

	/**
	 * Method assembles the query from all the front end components.
	 */
	public void writeQuery() {
		try {
			StringBuffer queryFromGui = new StringBuffer("SELECT");
			if (!this.limit.getSelectedItem().equals("unlimited")) {
				queryFromGui.append(" TOP ").append(this.limit.getSelectedItem());
			}
			
			if (this.selectAll.isSelected()) {
				queryFromGui.append(" *");
			} else {
				List<TapTableColumn> selectedColumns = (this.selectList.getSelectedValuesList());
				if (selectedColumns == null || selectedColumns.isEmpty()) {
					queryFromGui.append(" *");
				} else {
					queryFromGui.append(SPACESTRING);
					for (TapTableColumn selectedColumn : selectedColumns) {
						String alias = this.getRelevantAlias(selectedColumn);
						if (alias != null) {
							queryFromGui.append(alias).append(DOT_CHAR);
						}
						queryFromGui.append(selectedColumn.getColumnNameForQuery()).append(COMMA_SPACECHAR);
					}
				}
			}
			//queryFromGui.append(((List<TapTableColumn>) this.selectList.getSelectedValuesList()).toString().replaceAll("[\\[\\]]", ""))
			queryFromGui = new StringBuffer(queryFromGui.toString().trim().replaceAll(",$", EMPTYSTRING));
			queryFromGui.append(" FROM ")
			.append(TapTable.getQueryPart(selectedTableName, true)).append(SPACESTRING);
			
			if (this.joinPanel != null && this.joinPanel.constraints != null && !this.joinPanel.constraints.isEmpty()) {
				for (JoinConstraint joinConstraint : this.joinPanel.constraints) {
					queryFromGui.append(joinConstraint.getADQLString()).append(SPACESTRING);
				}
			}
			
			if (this.whereClausesPanel.getComponentCount() > 0) {
				WhereGridConstraint whereConstraint;
				Component[] whereConstraints = this.whereClausesPanel.getComponents();
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
			Aladin.error(this, e.getMessage());
			Aladin.trace(3, e.getMessage());
            ball.setMode(Ball.NOK);
		}
		
	}
	
	@Override
	protected void clear() {
		if (this.sync_async != null) {
			this.sync_async.setSelectedIndex(0);
		}
		if (this.limit != null) {
			if (!TAPEXDEFAULTMAXROWS.isEmpty()) {
				this.limit.setSelectedItem(TAPEXDEFAULTMAXROWS);
			} else {
				this.limit.setSelectedIndex(0);
			}
		}
		if (this.selectAll != null) {
			this.selectAll.setSelected(false);
		}
		
		boolean resetTargetPanel = false;
		if (this.raColumnName != null && this.decColumnName != null) {
			resetTargetPanel = true;
		}
		this.raColumnName = this.tapClient.tablesMetaData.get(selectedTableName).getRaColumnName(false);
		this.decColumnName = this.tapClient.tablesMetaData.get(selectedTableName).getDecColumnName(false);
		if (resetTargetPanel && (this.raColumnName != null && this.decColumnName != null)) {
			resetTargetPanel = false;
		}
		if (resetTargetPanel) {
			Vector<TapTableColumn> columnNames = null;
			try {
				columnNames = getSelectedTableColumns();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if (Aladin.levelTrace >= 3) e.printStackTrace();
				Aladin.error(this, e.getMessage());
				return;
			}
			setWhereAddConstraintsGui(columnNames);
			this.queryComponentsGui.revalidate();
			this.queryComponentsGui.repaint();
		}
		
		resetColumnSelection();
		resetFields();
		resetJoin();
		super.clear();
		this.revalidate();
		this.repaint();
	}
	
	public void resetJoin() {
		// TODO Auto-generated method stub
		if (this.joinPanel != null) {
			this.joinPanel.constraints.clear();
			joinConstraintUpdated();
			this.tapClient.tapManager.clearJoinPanel();
			this.joinPanel = null;
		}
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
	
	/*@Override
	public void checkTableSelectionChanged(JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		if (comboBox.getSelectedItem() != null
				&& !selectedTableName.equalsIgnoreCase(comboBox.getSelectedItem().toString())) {
			Aladin.trace(3, "Change table selection from within the document");
			if (comboBox == this.tablesGui) {
				this.changeTableSelection((String) comboBox.getSelectedItem());
			}
//			changeTableSelection((String) comboBox.getSelectedItem());
		}
	}
	
	@Override
	public void tableSelectionChanged(JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		if (comboBox == this.tablesGui) {
			this.changeTableSelection((String) comboBox.getSelectedItem());
		}
	}*/
	
	
	/** Sync TAP query via the script command: get TAP(URL|ID,queryString)
	 * @param target Not used here
	 * @param radius Not used here
	 * @param criteria two parameters: 1) the baseUrl or the ID of the MocServer record, 2) the TAP query
	 * @param label The plane label
	 * @param origin not Used here
	 * @return 0 ok, -1 si erreur
	 */
	protected int createPlane(String target,String radius,String criteria, String label, String origin) {

	   try {
         Tok tok = new Tok(criteria,", ");
         String baseUrl = tok.nextToken();
         String query = tok.nextToken();
          if( !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://") ) {
             if( label==null ) label=baseUrl;
             baseUrl = aladin.directory.resolveServiceUrl("tap",baseUrl);
          }
          ball = new Ball();
          tapClient = new TapClient();
          tapClient.tapLabel=label;
          TapManager tapManager = TapManager.getInstance(aladin);
          tapManager.fireSync(this, baseUrl, query, null);
         
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         return -1;
      }
	   return 0;
	}


	
//	@Override
//	public void submit() {
//		submit(null);//no request params
//	}
	
	/**
	 * Method fires sync or async submissions based on what is selected by user
	 * Incase of upload as file is posted, diff sync methods are followed
	 * @param requestParams
	 */
	@Override
	public void submit() {
	      //check again
		if (this.sync_async != null &&  this.tap != null) {
			boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
	  	  	this.submitTapServerRequest(sync, this.tapClient.tapLabel, this.tapClient.tapBaseUrl, this.tap.getText());

	  	  	// Echo of the equivalent script command
	  	  	aladin.console.printCommand("get TAP("+Tok.quote(this.tapClient.tapLabel)+","+Tok.quote(this.tap.getText())+")");
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		super.actionPerformed(arg0);
		Object source = arg0.getSource();
		if (source instanceof JButton) {
			String action = ((JButton) source).getActionCommand();
			if (action.equals(WRITEQUERY)) {
				this.writeQuery();
			} else if (action.equals(ADDWHERECONSTRAINT)) {
				try {
					Vector<TapTableColumn> columnMetaData = getSelectedTablesColumns();
					WhereGridConstraint columnConstraint = new ColumnConstraint(this, columnMetaData);
					addWhereConstraint(columnConstraint);
					writeQuery();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if (Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.error(this, e.getMessage());
					return;
				}
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
			            Aladin.error(this, e1.getMessage());
			            ball.setMode(Ball.NOK);
			            return;
			         }
			      }
				if (objet.trim().equals(TARGETNAN)) {//resolve obj for empty field
					Aladin.error(this, TapClient.TARGETERROR);
		            ball.setMode(Ball.NOK);
		            return;
				}
				WhereGridConstraint positionConstraint = new PositionConstraint(this, Util.myRound(coo[0].getText(), 5),
						Util.myRound(coo[1].getText(), 5), Util.myRound(rad[0].getText(), 5), this.selectedTableName, this.raColumnName,
						this.decColumnName);
				addWhereConstraint(positionConstraint);
				writeQuery();
			} else if (action.equals(CHECKQUERY)) {
				checkQueryFlagMessage();
			} else if (action.equals(OPEN_SET_RADEC)) {
				this.tapClient.tapManager.setRaDecForTapServer(this, selectedTableName);
			} else if (action.equals(JOIN_TABLE)) {
				if (this.joinPanel == null) {
					this.joinPanel = new JoinFacade(aladin, this);
					
					this.addAncestorListener(new AncestorListener() {
						
						@Override
						public void ancestorRemoved(AncestorEvent event) {
							// TODO Auto-generated method stub
							if (!ServerTap.this.isShowing()) {
								if (ServerTap.this.joinPanel.isShowing()) {
									ServerTap.this.tapClient.tapManager.closeMyJoinFacade(joinPanel);
								}
							}
						}
						
						@Override
						public void ancestorMoved(AncestorEvent event) {
							// TODO Auto-generated method stub
						}
						
						@Override
						public void ancestorAdded(AncestorEvent event) {
							// TODO Auto-generated method stub
							//good idea to show join facade? perhaps not if user has opened another one from dir tree..
						}
					});
				}
				this.tapClient.tapManager.showOnJoinFrame(this.joinPanel);
				this.tapClient.tapManager.loadForeignKeyRelationsForSelectedTable(this.tapClient, this.joinPanel, selectedTableName);
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
	
	public TapTable getSuitableJoinTable() {
		// TODO Auto-generated method stub
		TapTable potentialJoinTable = null;
		TapTable chosenTable = this.tapClient.tablesMetaData.get(selectedTableName);
		if (chosenTable.foreignKeyColumns != null && !chosenTable.foreignKeyColumns.isEmpty()) {
			String potentialJoinTableName = chosenTable.foreignKeyColumns.get(0).getFrom_table();
			potentialJoinTable = this.tapClient.tablesMetaData.get(potentialJoinTableName);
		} else {
			String joinTableName = getNextUnselectedTableName();
			potentialJoinTable = this.tapClient.tablesMetaData.get(joinTableName);
		}
		return potentialJoinTable;
	}
	
	/**
	 * Tables are already sorted to the understanding of metadata. Method tries to choose one which is not the selected table in the main gui
	 * @return
	 */
	public String getNextUnselectedTableName() {
		String notSelectedTopTable = selectedTableName;
		Vector<String> tables = this.getTableNames();
		for (String table : tables) {
			if (selectedTableName.equalsIgnoreCase(table)) {
				continue;
			} else {
				notSelectedTopTable = table;
				break;
			}
		}
		return notSelectedTopTable;
	}
	
	/**
	 * Just the selected table's columns in the main gui
	 * @return
	 */
	public Vector<TapTableColumn> getSelectedTableColumns() {
		// TODO Auto-generated method stub
		return this.tapClient.tablesMetaData.get(selectedTableName).getColumns();
	}
	
	public static Vector<TapTableColumn> getPotentialRaOrDecColumns(Vector<TapTableColumn> allColumns) {
		Vector<TapTableColumn> displayColumns = new Vector<TapTableColumn>();
		for (TapTableColumn tapTableColumn : allColumns) {
			if (tapTableColumn.isNumeric()) {
				displayColumns.add(tapTableColumn);
			}
		}
		return displayColumns;
	}
	
	/**
	 * All the selected tables's columns(main plu join)
	 * @return
	 * @throws Exception 
	 */
	public Vector<TapTableColumn> getSelectedTablesColumns() throws Exception {
		// TODO Auto-generated method stub
		Vector<TapTableColumn> displayColumns = new Vector<TapTableColumn>();
		if (this.joinPanel != null && this.joinPanel.constraints != null && !this.joinPanel.constraints.isEmpty()) {
			displayColumns.addAll(this.getSelectedTableColumns());
//			this.raColumnName = this.joinPanel.mainRaColumnName;
//			this.decColumnName = this.joinPanel.mainDecColumnName;
			
			for (JoinConstraint joinConstraint : this.joinPanel.constraints) {
				Vector<TapTableColumn> columnNames = joinConstraint.columns;
				if (columnNames == null) {
					throw new Exception(JoinFacade.ERROR_NOJOINCOLUMNS);
				}
				displayColumns.addAll(joinConstraint.columns);
			}
		} else {
			displayColumns = this.tapClient.tablesMetaData.get(selectedTableName).getColumns();
//			this.raColumnName = this.tapClient.tablesMetaData.get(selectedTableName).getRaColumnName();
//			this.decColumnName = this.tapClient.tablesMetaData.get(selectedTableName).getDecColumnName();
		}
		return displayColumns;
	}
	
	public String getRelevantAlias(TapTableColumn column) {
		// TODO Auto-generated method stub
		String alias = null;
		TapTable table = this.tapClient.tablesMetaData.get(column.getTable_name());
		if (table != null) {
			alias = table.getAlias();
		} else if (this.joinPanel != null && !this.joinPanel.constraints.isEmpty()) {
			for (JoinConstraint constraint : this.joinPanel.constraints) {
				if (constraint.hasThisColumn(column)) {
					alias = constraint.alias;
					break;
				}
			}
		} 
		return alias;
	}
	
	protected void createChaine() {
		super.createChaine();
		description = Aladin.chaine.getString("TAPFORMINFO");
		title = Aladin.chaine.getString("TAPFORMTITLE");
		verboseDescr  = loadedServerDescription = Aladin.chaine.getString("TAPGENERICFORMDESC");
	}
	
	static {
		LOAD = Aladin.chaine.getString("FSLOAD");
		REFRESHQUERYTOOLTIP = Aladin.chaine.getString("REFRESHQUERYTOOLTIP");
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
	
	
	public TapTableColumn getDefaultRa() {
		// TODO Auto-generated method stub
		return getDefault(this.raColumnName);
	}
	
	public TapTableColumn getDefaultDec() {
		// TODO Auto-generated method stub
		return getDefault(this.decColumnName);
	}
	
	public TapTableColumn getDefault(String columnName) {
		// TODO Auto-generated method stub
		TapTableColumn result = null;
		if (columnName != null) {
			for (TapTableColumn columnMeta : this.tapClient.tablesMetaData.get(selectedTableName).getColumns()) {
				if (columnMeta.getColumn_name().equals(columnName)) {
					result = columnMeta;
					break;
				}
			}
		}
		return result;
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
