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

import static cds.aladin.Constants.ADDWHERECONSTRAINT;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;
import static cds.aladin.Constants.SERVERJOINTABLESELECTED;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.UPLOADJOINTABLESELECTED;
import static cds.tools.CDSConstants.BOLD;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;

/**
 * Class to manage changes in generic tap client w.r.t adding join constraint(s).
 * @author chaitra
 *
 */
public class JoinFacade extends JPanel implements FilterActionClass, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 797757395359447128L;
	public Aladin aladin;
	public ServerTap serverTap;
	
	public JComboBox serverTablesGui;
	public List<JoinConstraint> constraints;
	public Vector<TapTableColumn> mainColumns1;
	public String mainRaColumnName;
	public String mainDecColumnName;
	JPanel constraintListPanel;
	GridBagConstraints cc = new GridBagConstraints();
	private JRadioButton radio1;
	private JRadioButton radio2;
	private JComboBox uploadTablesGui;
	private JRadioButton positionJoin;
	private JRadioButton foreignRelJoin;
	private JTextField radius;
	private JComboBox targetRefCols;
	private JComboBox targetCols;
	private JComboBox unrelJoinOperators;
	private JComboBox refCols;
	private JLabel info1;
	private String joinTableName;
	private JRadioButton otherJoin;
	private String CLIENTINSTR;
	public String mainTable;
	
	public static String JOINCONTRAINTSTIP, JOINCONSTRAINTASKRADIUS, JOINCONSTRAINTASKRADIUSTIP, JOINCONSTRAINTASKCOL,
			JOINCONSTRAINTASKCOLTIP, JOINCONSTRAINTASKEQCOLTIP, GENERCIERROR_JOIN, ERROR_NOJOINCOLUMNS,
			ERROR_NOJOINRADUIS, JOINFRAMETITLE, ERROR_NOINTERFACEMESSAGE, LOADING_JOININTERFACEMESSAGE, JOINUPLOADEDTABLETIP,
			JOINTABLELABEL, ERROR_NOJOINCOLUMN, UPLOADJOINTABLENAMETOOLTIP, WRITEJOINBUTTONLABEL, JOINRADIUSTOOLTIP, JOINUPLOADTABLELABEL;
	
	protected static final String[] joinOperators = { "=", "<>"};
	
	public JoinFacade() {
		// TODO Auto-generated constructor stub
		info1 = new JLabel();
		constraints = new ArrayList<JoinConstraint>();
		
	}
	
	public JoinFacade(Aladin aladin, ServerTap server) {
		// TODO Auto-generated constructor stub
		this();
		this.aladin = aladin;
		this.serverTap = server;		
		showloading();
		TapManager.getInstance(aladin).initUploadFrame();
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(700, 450);
	}
	
	/**
	 * Creates the first form for loading upload data
	 */
	public void setJoinTableForm(String priTableChoice, String secTableChoice) {
		if (mainTable != this.serverTap.selectedTableName) {
			mainTable = this.serverTap.selectedTableName;
			Vector<String> tables = serverTap.getTableNames();
			TapTable chosenTable = null;
			
			if (priTableChoice == null || !tables.contains(priTableChoice)) {
				chosenTable = this.serverTap.getSuitableJoinTable();
			} else {
				chosenTable = this.serverTap.tapClient.tablesMetaData.get(priTableChoice);
			}
			String selectedTableName = chosenTable.getTable_name();
			
			TapTable mainTableMeta = this.serverTap.tapClient.tablesMetaData.get(mainTable);
			Vector<TapTableColumn> mainColumns = mainTableMeta.getColumns();
			Vector<TapTableColumn> joinColumns = this.serverTap.getColumnsToLoad(selectedTableName, this.serverTap.tapClient.tablesMetaData);
			TapTable serverJoinTable = this.serverTap.tapClient.tablesMetaData.get(selectedTableName);
			
			/*if (this.serverTablesGui != null) {
				mainTableChanged(chosenTable);
				return;
			}*/
			Map<String, TapTable> uploadMeta = this.serverTap.tapClient.tapManager.getUploadedTables();
			
			TapTable chosenTable2 = null;
			String secondaryTable = null;
			if (this.serverTap.tapClient.isUploadAllowed() && uploadMeta != null && !uploadMeta.isEmpty()) {
				if (secTableChoice == null || !uploadMeta.containsKey(secTableChoice)) {
					if (uploadMeta.size() > 1) {
						secondaryTable = uploadMeta.keySet().iterator().next();
					} else {
						secondaryTable = null;
					}
				} else {
					secondaryTable = secTableChoice;
				}
			}
			
			if (secondaryTable != null) {
				chosenTable2 = uploadMeta.get(secondaryTable);
			}
			
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 0.01;
	        c.weightx = 0.10;
	        c.anchor = GridBagConstraints.NORTHWEST;
		    c.fill = GridBagConstraints.NONE;
		    c.insets = new Insets(10, 10, 10, 10);
		    CLIENTINSTR = Aladin.chaine.getString("TAPJOININSTR");
		    this.removeAll();
		    info1 = new JLabel(CLIENTINSTR);
		    this.add(info1, c);	
		    
			JPanel tablesPanel = null;
			try {
				Vector<String> model = new Vector<String>();
				model.addAll(tables);
				serverTablesGui = new JComboBox(model);
				List<JComponent> compToPrefix = new ArrayList<JComponent>();
				ButtonGroup radioGroup1 = new ButtonGroup();
				radio1 = new JRadioButton();
				radio1.setSelected(true);
				radio1.addActionListener(this);
				radio1.setActionCommand(SERVERJOINTABLESELECTED);
				radioGroup1.add(radio1);
				compToPrefix.add(radio1);
				
				tablesPanel = DynamicTapForm.getTablesPanel(null, this, JOINTABLELABEL, serverTablesGui, chosenTable, tables, compToPrefix, true);
				tablesPanel.setFont(BOLD);
				c.weighty = 0.01;
		        c.weightx = 0.10;
		        c.anchor = GridBagConstraints.NORTHWEST;
			    c.fill = GridBagConstraints.NONE;
			    c.insets = new Insets(0, 4, 0, 4);
			    c.gridy++;
			    this.add(tablesPanel, c);	
			    
			    joinTableName = chosenTable.getTable_name();
			    
			    if (this.serverTap.tapClient.isUploadAllowed()) {
					MutableComboBoxModel uploadModel = (MutableComboBoxModel) this.serverTap.tapClient.tapManager.getUploadClientModel();
					
					radio2 = new JRadioButton();
					radioGroup1.add(radio2);
					radio2.setToolTipText(JOINUPLOADEDTABLETIP);
					radio2.setEnabled((uploadModel.getSize() > 0));
					radio2.addActionListener(this);
					radio2.setActionCommand(UPLOADJOINTABLESELECTED);
					
					uploadTablesGui = new JComboBox();
					uploadTablesGui.setModel(uploadModel);
					
					compToPrefix = new ArrayList<JComponent>();
					compToPrefix.add(radio2);
					tablesPanel = DynamicTapForm.getTablesPanel(null, this, JOINUPLOADTABLELABEL, uploadTablesGui, chosenTable2, null, compToPrefix, false);
					UploadTablesRenderer uploadTableRenderer = UploadTablesRenderer.getInstance(this.aladin);
					uploadTablesGui.setRenderer(uploadTableRenderer);
					if (uploadTablesGui.getSelectedItem() != null) {
						String uploadTable = (String) uploadTablesGui.getSelectedItem();
						String planeName = uploadTableRenderer.getUploadPlaneName(uploadTable);
						uploadTablesGui.setToolTipText(UploadTablesRenderer.getToolTip(planeName, uploadTable));
					}
					tablesPanel.setFont(BOLD);
					
					c.weightx = 0.12;
			        c.anchor = GridBagConstraints.NORTHWEST;
				    c.fill = GridBagConstraints.NONE;
				    c.insets = new Insets(0, 4, 0, 4);
				    c.gridy++;
				    this.add(tablesPanel, c);	
				    c.weighty = 0.05;
				}
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				Aladin.error(this, e.getMessage());
				Aladin.trace(3, e.getMessage());
				return;
			}
			
			JPanel joinConstraintsPanel = new JPanel();
			joinConstraintsPanel.setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
			joinConstraintsPanel.setLayout(new BoxLayout(joinConstraintsPanel, BoxLayout.Y_AXIS));
			joinConstraintsPanel.setBorder(BorderFactory.createTitledBorder(JOINCONTRAINTSTIP));
			
			ButtonGroup radioGroup2 = new ButtonGroup();
			JRadioButton gui = new JRadioButton();
			
			gui.setLayout(new GridBagLayout());
			GridBagConstraints guic = new GridBagConstraints();
			guic.anchor = GridBagConstraints.WEST;
			guic.fill = GridBagConstraints.HORIZONTAL;
			guic.insets = new Insets(2, 25, 2, 0);
			guic.gridx = 0;
			guic.gridy = 0;
			guic.weightx = 0.01;
			guic.weighty = 1;
			gui.add(new JLabel(JOINCONSTRAINTASKCOL), guic);
			foreignRelJoin = gui;
			radioGroup2.add(gui);
			guic.gridx++;
			guic.insets = new Insets(2, 5, 2, 0);
			guic.weightx = 0.99;
			Vector<ForeignKeyColumn> joinConditions = getJoinRefConditions(mainTableMeta);
			targetRefCols = new JComboBox(joinConditions);
			targetRefCols.setRenderer(new TapForeignKeysRenderer());
			gui.add(targetRefCols, guic);
			gui.setToolTipText(JOINCONSTRAINTASKEQCOLTIP);
			joinConstraintsPanel.add(gui);
			
			gui = new JRadioButton();
			gui.setLayout(new GridBagLayout());
			guic = new GridBagConstraints();
			guic.insets = new Insets(0, 25, 0, 0);
			guic.gridx = 0;
			guic.gridy = 0;
			guic.anchor = GridBagConstraints.WEST;
			guic.fill = GridBagConstraints.HORIZONTAL;
			guic.weightx = 0.01;
			guic.weighty = 1;
			gui.add(new JLabel(JOINCONSTRAINTASKRADIUS), guic);
			guic.gridx++;
			guic.weightx = 0.99;
			guic.insets = new Insets(0, 5, 0, 0);
			radius = new JTextField(30);
			radius.setToolTipText(JOINRADIUSTOOLTIP);
			gui.add(radius, guic);
			joinConstraintsPanel.add(gui);
			positionJoin = gui;
			radioGroup2.add(gui);
			
			
			gui = new JRadioButton();
			gui.setLayout(new GridBagLayout());
			guic = new GridBagConstraints();
			guic.gridx = 0;
			guic.gridy = 0;
			guic.weightx = 0.1;
			guic.weighty = 1;
			guic.insets = new Insets(2, 25, 2, 0);
			guic.anchor = GridBagConstraints.WEST;
			guic.fill = GridBagConstraints.HORIZONTAL;
			gui.add(new JLabel(JOINCONSTRAINTASKCOL), guic);
			gui.setToolTipText(JOINCONSTRAINTASKCOLTIP);
			otherJoin = gui;
			radioGroup2.add(gui);
			
			guic.gridx++;
			guic.insets = new Insets(2, 5, 2, 0);
			guic.weightx = 0.99;
			Vector<TapTableColumn> model = new Vector<TapTableColumn>();
			model.addAll(mainColumns);
			targetCols = new JComboBox(model);
			targetCols.setRenderer(new CustomListCellRenderer());
			unrelJoinOperators = new JComboBox(joinOperators);
			TapTableColumn selectedTargetCol = (TapTableColumn) targetCols.getSelectedItem();
			Vector<TapTableColumn> sameTypeColumns = getSameTypeColumns(selectedTargetCol, joinColumns);
			if (!model.isEmpty()) {
				refCols = new JComboBox(sameTypeColumns);
			} else {
				refCols = new JComboBox();
			}
			refCols.setRenderer(new CustomListCellRenderer());
			
			JPanel unrelatedJoinConstraintPanel = new JPanel();
			unrelatedJoinConstraintPanel.setLayout(new BoxLayout(unrelatedJoinConstraintPanel, BoxLayout.X_AXIS));
			unrelatedJoinConstraintPanel.add(targetCols);
			unrelatedJoinConstraintPanel.add(unrelJoinOperators);
			unrelatedJoinConstraintPanel.add(refCols);
			gui.add(unrelatedJoinConstraintPanel, guic);
			
			targetCols.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					// TODO Auto-generated method stub
					TapTable secTableMetaData = serverTap.tapClient.tablesMetaData.get(joinTableName);
					if (radio2.isSelected()) {
						Map<String, TapTable> uploadedTables = serverTap.tapClient.tapManager.getUploadedTables();
						if (uploadedTables != null) {
							secTableMetaData = uploadedTables.get(joinTableName);
						}
					}
					TapTableColumn selectedTargetCol = (TapTableColumn) targetCols.getSelectedItem();
					Vector<TapTableColumn> sameTypeColumns = getSameTypeColumns(selectedTargetCol, secTableMetaData.getColumns());
					refCols.removeAllItems();
					if (!sameTypeColumns.isEmpty()) {
						refCols.setEnabled(true);
						DefaultComboBoxModel model = new DefaultComboBoxModel(sameTypeColumns);
						refCols.setModel(model);
					} else {
						refCols.setEnabled(false);
					}
				}
			});
			
			setFormState(joinConditions, serverJoinTable);
			
			joinConstraintsPanel.add(gui);
			
			JButton writeJoinButton = new JButton(WRITEJOINBUTTONLABEL);
			writeJoinButton.setActionCommand(ADDWHERECONSTRAINT);
			joinConstraintsPanel.add(writeJoinButton);
			writeJoinButton.addActionListener(this);
			
			c.gridy++;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.fill = GridBagConstraints.NONE;
			c.insets = new Insets(2, 10, 2, 10);
			this.add(joinConstraintsPanel, c);
			
			this.constraintListPanel = new JPanel();
			this.constraintListPanel.setLayout(new BoxLayout(constraintListPanel, BoxLayout.Y_AXIS));
			this.constraintListPanel.setBorder(BorderFactory.createLineBorder(Aladin.COLOR_BUTTON_BACKGROUND_BORDER_UP));
			JScrollPane constraintsScroller = new JScrollPane(constraintListPanel);
			constraintsScroller.getVerticalScrollBar().setUnitIncrement(5);
			constraintsScroller.setMinimumSize(new Dimension(200, 100));
			
			c.gridy++;
			c.fill = GridBagConstraints.BOTH;
			c.insets = new Insets(2, 10, 10, 10);
			c.weighty = 0.94;
			this.add(constraintsScroller, c);	
			
			if (secondaryTable != null) {
				uploadTablesGui.setSelectedItem(secondaryTable);
			} else {
				uploadTablesGui.setEnabled(false);
			}
		}
	}
	
	/**
	 * Join facade changes when main table selection is changed
	 * @param priTableChoice
	 * @param secTableChoice
	 
	public void mainTableChanged(TapTable chosenTable) {
		String selectedTableName = chosenTable.getTable_name();
		
		TapTable mainTableMeta = this.serverTap.tapClient.tablesMetaData.get(this.serverTap.selectedTableName);
		Vector<TapTableColumn> mainColumnNames = mainTableMeta.getColumns();
		Vector<TapTableColumn> joinColumnNames = this.serverTap.getColumnsToLoad(selectedTableName, this.serverTap.tapClient.tablesMetaData);
		TapTable serverJoinTable = this.serverTap.tapClient.tablesMetaData.get(selectedTableName);
		
		serverTablesGui.setSelectedItem(chosenTable);
		radio1.setSelected(true);
	    joinTableName = chosenTable.getTable_name();
		
		Vector<String> joinConditions = getJoinRefConditions(mainTableMeta);
		DefaultComboBoxModel newModel = new DefaultComboBoxModel(joinConditions);
		targetRefCols.setModel(newModel);
		
		Vector<TapTableColumn> mainColumnNamesModel = new Vector<TapTableColumn>();
		mainColumnNamesModel.addAll(mainColumnNames);
		newModel = new DefaultComboBoxModel(mainColumnNamesModel);
		targetCols.setModel(newModel);
		
		unrelJoinOperators.setSelectedIndex(0);
		TapTableColumn selectedTargetCol = (TapTableColumn) targetCols.getSelectedItem();
		Vector<TapTableColumn> refColsNames = getSameTypeColumns(selectedTargetCol, joinColumnNames);
		newModel = new DefaultComboBoxModel(refColsNames);
		refCols.setModel(newModel);
		
		this.constraintListPanel.removeAll();
		setFormState(joinConditions, serverJoinTable);
		revalidate();
		repaint();
		
	}*/
	
	private void setFormState(Vector<ForeignKeyColumn> joinConditions, TapTable joinTableMeta) {
		// TODO Auto-generated method stub
		boolean allowPositionJoin = false;
		boolean allowRefJoin = false;
		
		if (this.serverTap.getRaColumnName() != null && this.serverTap.getDecColumnName() != null 
				&& (joinTableMeta!= null && joinTableMeta.getRaColumnName(true) != null && joinTableMeta.getDecColumnName(true) != null) ) {
			allowPositionJoin = true;
		} 
		if (joinConditions != null && !joinConditions.isEmpty()) {
			allowRefJoin = true;
		}
		
		if (allowRefJoin) {
			targetRefCols.setEnabled(true);
			foreignRelJoin.setEnabled(true);
			foreignRelJoin.setSelected(true);
		} else {
			targetRefCols.setEnabled(false);
			foreignRelJoin.setSelected(false);
			foreignRelJoin.setEnabled(false);
		}
		
		if (allowPositionJoin) {
			if (!allowRefJoin) {
				positionJoin.setSelected(true);
			}
			radius.setEnabled(true);
			positionJoin.setEnabled(true);
			positionJoin.setToolTipText(JOINCONSTRAINTASKRADIUSTIP);
		} else {
			positionJoin.setSelected(false);
			positionJoin.setEnabled(false);
			radius.setEnabled(false);
			positionJoin.setToolTipText(null);
		}
		
		if (!allowRefJoin && !allowPositionJoin) {
			otherJoin.setSelected(true);
		}
		
		if (refCols.getItemCount() == 0) {
			refCols.setEnabled(false);
		} else {
			refCols.setEnabled(true);
		}
	}

	private Vector<ForeignKeyColumn> getJoinRefConditions(TapTable mainTableMeta) {
		// TODO Auto-generated method stub
		Vector<ForeignKeyColumn> foreignRels = new Vector<ForeignKeyColumn>();
//		StringBuffer joinStatement = null;
//		String priTableNameForQuery = TapTable.getQueryPart(mainTableMeta.getTable_name(), true);
//		String secTableNameForQuery = this.currentConstraint.alias;
		if (mainTableMeta.foreignKeyColumns != null && !mainTableMeta.foreignKeyColumns.isEmpty()) {
			for (ForeignKeyColumn keyColumn : mainTableMeta.foreignKeyColumns) {
				if (joinTableName.equalsIgnoreCase(keyColumn.getFrom_table())) {
//					joinStatement = new StringBuffer();
//					joinStatement/*.append(priTableNameForQuery).append(DOT_CHAR)*/.append(keyColumn.getTarget_column())
//					.append(" = ")/*.append(secTableNameForQuery).append(DOT_CHAR)*/.append(keyColumn.getFrom_column());
					foreignRels.addElement(keyColumn);
				}
			}
		}
		return foreignRels;
	}

	public void addConstraint() throws Exception {
		//check if syntax it right.
		StringBuffer cond = new StringBuffer();
		
		TapTable secTableMetaData = this.serverTap.tapClient.tablesMetaData.get(joinTableName);
		if (radio2.isSelected()) {
			Map<String, TapTable> uploadedTables = this.serverTap.tapClient.tapManager.getUploadedTables();
			if (uploadedTables != null) {
				secTableMetaData = uploadedTables.get(joinTableName);
			}
		}
		
		String priTableNameForQuery = TapTable.getQueryPart(mainTable, true);
		JoinConstraint constraint = new JoinConstraint(this, mainTable, secTableMetaData);
		
		if (foreignRelJoin.isSelected()) {
//			cond.append(targetRefCols.getSelectedItem());
			ForeignKeyColumn forRel = (ForeignKeyColumn) targetRefCols.getSelectedItem();
			cond.append(priTableNameForQuery).append(DOT_CHAR).append(forRel.getTarget_column()).append(SPACESTRING)
			.append(" = ").append(constraint.alias).append(DOT_CHAR).append(forRel.getFrom_column());
		} else if (positionJoin.isSelected()) {
			if (radius.getText().trim().isEmpty()) {
				throw new Exception(ERROR_NOJOINRADUIS);
			}
			String secRaColumnName = null;
			String secDecColumnName = null;
			if (secTableMetaData != null) {
				secRaColumnName = constraint.raColumnName;
				secDecColumnName = constraint.decColumnName;
//				secRaColumnName = TapTable.getQueryPart(secTableMetaData.getRaColumnName(), false); 
//				secDecColumnName = TapTable.getQueryPart(secTableMetaData.getDecColumnName(), false);
			}
			String priRaColumnName = TapTable.getQueryPart(this.serverTap.getRaColumnName(), false);
			String priDecColumnName = TapTable.getQueryPart(this.serverTap.getDecColumnName(), false);
			cond.append("1 =CONTAINS (POINT('ICRS', ")
			.append(priTableNameForQuery).append(DOT_CHAR).append(priRaColumnName).append(", ")
			.append(priTableNameForQuery).append(DOT_CHAR).append(priDecColumnName)
			.append("), CIRCLE('ICRS', ").append(constraint.alias).append(DOT_CHAR)
			.append(secRaColumnName).append(", ").append(constraint.alias).append(DOT_CHAR)
			.append(secDecColumnName).append(", ").append(radius.getText()).append("/3600.))");
		} else {
			if (!refCols.isEnabled()) {
				throw new Exception(ERROR_NOJOINCOLUMN);
			}
			TapTableColumn secColumn = (TapTableColumn) refCols.getSelectedItem();
			String mainColName = ((TapTableColumn) targetCols.getSelectedItem()).getColumn_name();
			cond.append(priTableNameForQuery).append(DOT_CHAR).append(mainColName).append(SPACESTRING)
					.append(unrelJoinOperators.getSelectedItem()).append(constraint.alias).append(DOT_CHAR)
					.append(secColumn.getColumn_name());
			
			/*.append(SPACESTRING).append(constraint.alias).append(DOT_CHAR).append(selectedJoinColName);*/
		}
		
		if (!cond.toString().isEmpty()) {
			constraint.setConstraintAndGui(cond.toString());
			constraints.add(constraint);
			constraint.setMaximumSize(new Dimension(Integer.MAX_VALUE, constraint.getMinimumSize().height));
			constraint.setAlignmentX(Component.LEFT_ALIGNMENT);
			this.constraintListPanel.add(constraint);
		}
	}
	
	@Override
	public void checkSelectionChanged(JComboBox<String> comboBox) {
		if (comboBox.getSelectedItem() != null) {
			String chosen = comboBox.getSelectedItem().toString();
			if (comboBox.equals(this.serverTablesGui) && !chosen.equalsIgnoreCase(joinTableName)) {
				Aladin.trace(3, "Change table selection from within the document");
				joinTableName = chosen;
				if (!radio1.isSelected()) {
					radio1.setSelected(true);
				}
				this.changeTableSelection(joinTableName, true);
			} else if (comboBox.equals(this.uploadTablesGui) && !chosen.equalsIgnoreCase(joinTableName)) {
				Aladin.trace(3, "Change table selection from within the document");
				boolean isServerJoin = false;
				if (this.uploadTablesGui.getItemCount() > 0) {
					this.uploadTablesGui.setEnabled(true);
					if (radio2.isSelected()) {
						joinTableName = chosen;
					} else {
						isServerJoin = true;
					}
					if (!radio2.isEnabled()) {
						radio2.setEnabled(true);
					}
					if (uploadTablesGui.getSelectedItem() != null) {
						String uploadTable = (String) uploadTablesGui.getSelectedItem();
						UploadTablesRenderer uploadTableRenderer = (UploadTablesRenderer) uploadTablesGui.getRenderer();
						String planeName = uploadTableRenderer.getUploadPlaneName(uploadTable);
						uploadTablesGui.setToolTipText(UploadTablesRenderer.getToolTip(planeName, uploadTable));
					}
				} else {
					if (radio2.isSelected()) {
						joinTableName = (String) this.serverTablesGui.getSelectedItem();
						radio1.setSelected(true);
					}
					radio2.setEnabled(false);
					this.uploadTablesGui.setEnabled(false);
					isServerJoin = true;
				}
				this.changeTableSelection(joinTableName, isServerJoin);
			}
		} else if (comboBox.equals(this.uploadTablesGui)) {
			joinTableName = (String) this.serverTablesGui.getSelectedItem();
			radio1.setSelected(true);
			radio2.setEnabled(false);
			this.uploadTablesGui.setEnabled(false);
			this.changeTableSelection(joinTableName, true);
		}
	}

	public void changeTableSelection(String tableChoice, boolean isServerJoin) {
		// TODO Auto-generated method stub
		TapTable mainTableMeta = this.serverTap.tapClient.tablesMetaData.get(mainTable);
		//third radio
		TapTable table = null;
		Vector<TapTableColumn> columnNames = null;
		if (isServerJoin) {
			Map<String, TapTable> tablesMetaData = this.serverTap.tapClient.tablesMetaData;
			columnNames = this.serverTap.getColumnsToLoad(tableChoice, tablesMetaData);
			table = tablesMetaData.get(tableChoice);
		} else {
			Map<String, TapTable> uploadedTables = this.serverTap.tapClient.tapManager.getUploadedTables();
			if (uploadedTables != null && uploadedTables.containsKey(tableChoice)) {
				table = uploadedTables.get(tableChoice);
				columnNames = table.getColumns();
			}
		}
		this.refCols.removeAllItems();
		
		DefaultComboBoxModel model = null;
		if (columnNames != null && !columnNames.isEmpty()) {
			TapTableColumn selectedTargetCol = (TapTableColumn) targetCols.getSelectedItem();
			Vector<TapTableColumn> columns = getSameTypeColumns(selectedTargetCol, columnNames);
			model = new DefaultComboBoxModel(columns);
			this.refCols.setModel(model);
		}
		
		//if foreign key rel exists between both tables
		targetRefCols.removeAllItems();
		Vector<ForeignKeyColumn> joinConditions = null;
		if (isServerJoin) {
			joinConditions = getJoinRefConditions(mainTableMeta);
			if (joinConditions != null && !joinConditions.isEmpty()) {
				model = new DefaultComboBoxModel(joinConditions);
				this.targetRefCols.setModel(model);
			}
		}
		
		setFormState(joinConditions, table);
	}

	@Override
	public Vector<String> getMatches(String mask, JComboBox<String> comboBox) {
		// TODO Auto-generated method stub
		return this.serverTap.getMatches(mask, comboBox);
	}
	
	/**
	 * Tap join client gui in case of loading error
	 * @param string 
	 */
	public void showLoadingError() {
		this.removeAll();
		this.setLayout(new BorderLayout());
		GridBagConstraints c = new GridBagConstraints();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		CLIENTINSTR = ERROR_NOINTERFACEMESSAGE+SPACESTRING+this.serverTap.tapClient.tapLabel;
		info1.setText(CLIENTINSTR);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
	    c.weighty = 0.02;
	    info1.setHorizontalAlignment(SwingConstants.CENTER);
	    containerPanel.add(info1, c);
	    add(containerPanel);
		revalidate();
		repaint();
	}
	
	/**
	 * Tap join client gui in case when it is still loading
	 */
	public void showloading() {
		this.removeAll();
		this.setLayout(new BorderLayout());
		GridBagConstraints c = new GridBagConstraints();
		JPanel containerPanel = new JPanel(new GridBagLayout());
		CLIENTINSTR = LOADING_JOININTERFACEMESSAGE+SPACESTRING+this.serverTap.tapClient.tapLabel+"...";
		info1.setText(CLIENTINSTR);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
	    c.weighty = 0.02;
	    info1.setHorizontalAlignment(SwingConstants.CENTER);
	    containerPanel.add(info1, c);
	    add(containerPanel);
		revalidate();
		repaint();
	}
	
	public void removeConstraint(JoinConstraint constraint) {
		constraints.remove(constraint);
		this.constraintListPanel.remove(constraint);
	}
	
	/**
	 * Method decides which columns of the join table can be allowed to be joined with the target table
	 * If datatype is not specified then no restriction is placed
	 * If specified: it is checked whether columns are numeric or not. 
	 * @param ref
	 * @param availableColumns
	 * @return
	 */
	public Vector<TapTableColumn> getSameTypeColumns(TapTableColumn ref, Vector<TapTableColumn> availableColumns) {
		Vector<TapTableColumn> result = new Vector<TapTableColumn>();
		for (TapTableColumn tapTableColumn : availableColumns) {
			if (ref != null) {
				if (tapTableColumn.getDatatype() == null) {
					result.addElement(tapTableColumn);
				} else if (ref.isNumeric()) {
					if (tapTableColumn.isNumeric()) {
						result.addElement(tapTableColumn);
					}
				} else if (!tapTableColumn.isNumeric()) {
					result.addElement(tapTableColumn);
				}
			} else {
				result.addAll(availableColumns);
				break;
			}
		}
		return result;
	}
	
	/**
	 * updates position join constraint gui in the event of edits on ra/dec by user.
	 */
	public void updatePositionParams() {
		// TODO Auto-generated method stub
		if (radio1.isSelected()) {
			TapTable joinTableMeta = this.serverTap.tapClient.tablesMetaData.get(joinTableName);
			if (this.serverTap.getRaColumnName() != null && this.serverTap.getDecColumnName() != null 
					&& (joinTableMeta!= null && joinTableMeta.getRaColumnName(false) != null && joinTableMeta.getDecColumnName(false) != null) ) {
				positionJoin.setEnabled(true);
			} else {
				positionJoin.setEnabled(false);
			}
			positionJoin.revalidate();
			positionJoin.repaint();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() instanceof JButton) {
			JButton button = (JButton) e.getSource();
			String action = button.getActionCommand();
			if (action.equals(ADDWHERECONSTRAINT)) {
				// check new query.
				try {
					addConstraint();
					this.constraintListPanel.revalidate();
					this.constraintListPanel.repaint();
					// change select and columns of main panel
					this.serverTap.joinConstraintUpdated();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.trace(3, e1.getMessage());
					Aladin.error(this.getRootPane(), e1.getMessage());
				}
				
			} else if (action.equals(REMOVEWHERECONSTRAINT)) {
				JPanel thisComponent = (JPanel) button.getParent();
				if (thisComponent instanceof JoinConstraint) {
					removeConstraint((JoinConstraint) thisComponent);
				}
				this.constraintListPanel.revalidate();
				this.constraintListPanel.repaint();

				// revert changes in main panel
				this.serverTap.joinConstraintUpdated();
				// check new query.
			}
		} else if (e.getSource() instanceof JRadioButton) {
			JRadioButton button = (JRadioButton) e.getSource();
			String action = button.getActionCommand();
			if (action.equals(SERVERJOINTABLESELECTED)) {
				checkSelectionChanged(serverTablesGui);
			} else if (action.equals(UPLOADJOINTABLESELECTED)) {
				checkSelectionChanged(uploadTablesGui);
			}
		}
	}

	public TapTable getTable(String joinTableName) {
		// TODO Auto-generated method stub
		return this.serverTap.tapClient.tablesMetaData.get(joinTableName);
	}
	
	static {
		JOINCONTRAINTSTIP = Aladin.chaine.getString("JOINCONTRAINTSTIP");
		JOINCONSTRAINTASKRADIUS = Aladin.chaine.getString("JOINCONSTRAINTASKRADIUS");
		JOINCONSTRAINTASKRADIUSTIP = Aladin.chaine.getString("JOINCONSTRAINTASKRADIUSTIP");
		JOINCONSTRAINTASKCOL = Aladin.chaine.getString("JOINCONSTRAINTASKCOL");
		JOINCONSTRAINTASKCOLTIP = Aladin.chaine.getString("JOINCONSTRAINTASKCOLTIP");
		JOINCONSTRAINTASKEQCOLTIP = Aladin.chaine.getString("JOINCONSTRAINTASKEQCOLTIP");
		GENERCIERROR_JOIN = Aladin.chaine.getString("GENERCIERROR_JOIN");
		ERROR_NOJOINCOLUMNS = Aladin.chaine.getString("ERROR_NOJOINCOLUMNS");
		ERROR_NOJOINRADUIS = Aladin.chaine.getString("ERROR_NOJOINRADUIS");
		JOINFRAMETITLE = Aladin.chaine.getString("JOINFRAMETITLE");
		ERROR_NOINTERFACEMESSAGE = Aladin.chaine.getString("ERROR_NOINTERFACEMESSAGE");
		LOADING_JOININTERFACEMESSAGE = Aladin.chaine.getString("LOADING_JOININTERFACEMESSAGE");
		JOINUPLOADEDTABLETIP = Aladin.chaine.getString("JOINUPLOADEDTABLETIP");
		JOINTABLELABEL = Aladin.chaine.getString("JOINTABLELABEL");
		ERROR_NOJOINCOLUMN = Aladin.chaine.getString("ERROR_NOJOINCOLUMN");
		UPLOADJOINTABLENAMETOOLTIP = Aladin.chaine.getString("UPLOADJOINTABLENAMETOOLTIP");
		WRITEJOINBUTTONLABEL = Aladin.chaine.getString("WRITEJOINBUTTONLABEL");
		JOINRADIUSTOOLTIP = Aladin.chaine.getString("JOINRADIUSTOOLTIP");
		JOINUPLOADTABLELABEL = Aladin.chaine.getString("JOINUPLOADTABLELABEL");
	}


}
